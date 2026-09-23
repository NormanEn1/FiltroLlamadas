# Manual técnico — Filtro

## 1. Stack

| Elemento | Versión |
|---|---|
| Lenguaje | Kotlin 2.2.0 (JVM target 17) |
| UI | Jetpack Compose (BOM 2025.06.00) + Material 3 |
| Persistencia | Room 2.7.1 (KSP 2.2.0-2.0.2) · DataStore Preferences 1.1.7 |
| Navegación | Navigation Compose 2.9.0 |
| Build | Android Gradle Plugin 8.11.1 · Gradle 8.14.5 (wrapper) |
| SDK | `minSdk 29` (Android 10) · `targetSdk/compileSdk 36` (Android 16) |
| IDE | Android Studio Quail o superior (JDK incluido, JBR 21) |

Las versiones están centralizadas en `gradle/libs.versions.toml`.

## 2. Entorno de desarrollo

1. Instalar Android Studio (canal estable).
2. SDK Manager: **Android 16 (API 36)**, Build-Tools, Platform-Tools. (Gradle instala lo faltante al compilar.)
3. Abrir la carpeta del proyecto y sincronizar Gradle.
4. `local.properties` lo crea Studio con `sdk.dir`; **no se versiona**.

## 3. Compilación

```bash
./gradlew assembleDebug          # APK de depuración
./gradlew assembleRelease        # APK de release (requiere firma, ver §8)
./gradlew lint                   # análisis estático
```

Salida: `app/build/outputs/apk/<tipo>/`.

> **Windows + sandbox/antivirus:** si Gradle falla con `Unable to establish loopback connection`,
> define un TEMP simple: `set TEMP=C:\tmp` y `set TMP=C:\tmp` antes de compilar.

## 4. Estructura del código

```
app/src/main/
├─ AndroidManifest.xml            permisos, INTERNET eliminado, servicio de filtrado
├─ res/                           tema base, íconos vectoriales (adaptive + monochrome)
└─ java/com/norman/filtrollamadas/
   ├─ FiltroApp.kt                Application: crea AppContainer, canal de notificación, purga
   ├─ data/
   │  ├─ AppContainer.kt          DI manual + appScope (SupervisorJob + IO)
   │  ├─ CsvExporter.kt
   │  ├─ db/  Entities.kt · Daos.kt · AppDatabase.kt
   │  └─ settings/Settings.kt     AppSettings + SettingsRepository (DataStore)
   ├─ domain/
   │  ├─ Model.kt                 Decision, Reason, Verdict, ListType, PhoneNumbers
   │  └─ ScreeningEngine.kt       reglas
   ├─ screening/FiltroScreeningService.kt
   ├─ telephony/Telefonia.kt      SIMs y MMI (**67*, *#67#, ##67#)
   ├─ notify/Notifier.kt
   └─ ui/
      ├─ MainActivity.kt          edge-to-edge, tema según ajustes
      ├─ FiltroRoot.kt            Scaffold + NavigationBar + NavHost
      ├─ UiSupport.kt             containerViewModel, SetupStatus, formatos de fecha
      ├─ theme/Theme.kt           esquemas de color claro/oscuro/dinámico
      ├─ components/Components.kt ScreenScaffold, SectionCard, SwitchRow, ChoiceSegments, CallRow…
      ├─ home/HomeScreen.kt        Inicio (estado, pendientes, redirección, estadísticas)
      ├─ lists/ListsScreen.kt      NumbersScreen: bandeja Revisar + Permitidos + Bloqueados
      ├─ log/LogScreen.kt          Registro cronológico, detalle, exportar
      ├─ stats/StatsScreen.kt      Estadísticas: agregados en el ViewModel y gráficas en Canvas
      └─ settings/SettingsScreen.kt
         (cada archivo contiene su pantalla y su ViewModel)
```

Detalle de diseño y diagramas: [ARQUITECTURA.md](ARQUITECTURA.md).

## 5. Puntos clave de implementación

### 5.1 Servicio de filtrado
- Declarado con `android:permission="android.permission.BIND_SCREENING_SERVICE"` y la acción
  `android.telecom.CallScreeningService`.
- Solo se enlaza si la app tiene el rol `ROLE_CALL_SCREENING` (se pide con `RoleManager.createRequestRoleIntent`).
- `onScreenCall` lanza la evaluación en `appScope` con `withTimeout(3_500)`; ante excepción → **permitir**.
- Desvío = `setDisallowCall(true)` + `setRejectCall(true)` + `setSkipNotification(true)`; se conserva en el registro
  del sistema (`setSkipCallLog(false)`).
- Oculto = `handle` vacío o `handlePresentation != PRESENTATION_ALLOWED`.
- Las llamadas con el filtro apagado **no** se registran.

### 5.2 Códigos de desvío (MMI)
`TelecomManager.placeCall(Uri.fromParts("tel", code, null), extras)`; si hay SIM elegida se envía
`EXTRA_PHONE_ACCOUNT_HANDLE`. Android no expone a apps normales el estado del desvío
(`getCallForwarding` es `@SystemApi`), por eso se guarda la **última acción** (`forwardingState/Target/At`).

### 5.3 Estado de permisos
`rememberSetupStatus()` recalcula en cada `ON_RESUME` (`LifecycleResumeEffect`), así la UI se actualiza al volver
de los diálogos de permisos o del selector de rol.

### 5.4 Bandeja "Números"
- `CallDao.observePending(DESVIADA)`: `llamadas LEFT JOIN listas`, agrupado por `numberKey`; incluye números sin
  lista o `REVISADO` con `MAX(timestamp) > createdAt` (volvieron a llamar) → `PendingNumber`.
  `observePendingCount` (misma condición) alimenta el contador de la barra inferior y la tarjeta de Inicio.
- `CallDao.observeReviewed(DESVIADA)`: `listas` `REVISADO` sin llamadas posteriores a `createdAt`.
- Clasificar = `ListDao.upsertAll` con `BLANCA`, `NEGRA` o `REVISADO`. *Deshacer* = `deleteAll(keys)` y, en la misma
  corrutina, restaurar como `REVISADO` los que venían de Revisados. *Volver a pendientes* = `deleteAll([key])`.
- Umbral de "insistente": constante `INSISTENT_ATTEMPTS = 3` en `NumbersScreen`.

### 5.5 Nombre del contacto
`ScreeningEngine.contactName()` resuelve `PhoneLookup.DISPLAY_NAME` en la misma consulta con la que decide si es
contacto (sin costo extra) y viaja en `Verdict.contactName` hasta `CallEntry.contactName` (BD v2). La interfaz
muestra el nombre como título y el número como subtítulo; el CSV incluye la columna `contacto`.

En la interfaz, `rememberContactName(raw, stored)` usa el nombre guardado y, si no hay (registros anteriores a la
v2 o entradas de listas), lo resuelve con `ContactNames` (caché en memoria, invalidada al volver a la app).

### 5.6 Estadísticas
`StatsViewModel` combina `observeAll()` con el período elegido y calcula en memoria: serie diaria (relleno de días
sin llamadas), totales, comparación con el período anterior de igual duración, motivos, histograma por hora y top
de números. Las gráficas son `Canvas` de Compose (`drawRoundRect`), sin dependencias externas. La pantalla está
fuera de la barra inferior: se abre desde Inicio y vuelve con la flecha (`ScreenScaffold(onBack = …)`).

### 5.7 Notificación resumen
`Notifier.diverted` usa un id fijo (`1001`), `setNumber(total del día)` y `setOnlyAlertOnce(true)`: la primera
llamada del día suena, las siguientes actualizan el texto en silencio.

### 5.8 Garantía sin red
```xml
<uses-permission android:name="android.permission.INTERNET" tools:node="remove" />
<uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" tools:node="remove" />
```
Verificación del APK final:
```bash
aapt2 dump permissions app/build/outputs/apk/debug/app-debug.apk   # no debe listar INTERNET
```

## 6. Cómo extender

| Quiero… | Dónde |
|---|---|
| Nueva regla | Añadir valor a `Reason` (Model.kt) y evaluarla en `ScreeningEngine.evaluate` en el orden correcto |
| Nuevo ajuste | Campo en `AppSettings` + clave en `SettingsRepository.K` + `read`/`write` + control en `SettingsScreen` |
| Cambiar esquema de BD | Incrementar `version` en `AppDatabase` y añadir `Migration` (v2 = `MIGRATION_1_2`, columna `contactName`) |
| Otro código de desvío (p. ej. `**62*`) | `Telefonia` + acción en `ForwardAction` (HomeScreen) |
| Cambiar el umbral de "insistente" | `INSISTENT_ATTEMPTS` en `ui/lists/ListsScreen.kt` (candidato a ajuste configurable) |
| Nuevo tipo de lista | Valor en `ListType`; si debe influir en la decisión, evaluarlo en `ScreeningEngine` |

## 7. Pruebas

**Emulador (API 36):** *Extended controls → Phone → Call device* simula llamadas entrantes (número conocido/desconocido)
para validar reglas, registro y notificaciones. El desvío del operador no aplica en emulador.

**Dispositivo real — matriz mínima:**

| Caso | Esperado |
|---|---|
| Contacto | Timbra · `CONTACTO` |
| Desconocido | No timbra · notificación · llega al destino · `DESCONOCIDO` |
| Mismo desconocido < N min | Timbra · `RELLAMADA` |
| Oculto (`#31#` desde el tercer teléfono) | Desvía · `OCULTO` |
| Número permitido / bloqueado | Timbra / desvía (bloqueado desvía aunque insista) |
| Desconocido desviado 3 veces | Aparece una vez en *Revisar* con "Insistente"; *Bloquear insistentes* lo mueve a Bloqueados |
| Permitir desde *Revisar* + *Deshacer* | Vuelve a la bandeja |
| *Marcar todo revisado* | Pendientes queda vacío; todos aparecen en *Revisados* |
| Número revisado vuelve a llamar | Sale de *Revisados* y reaparece en *Pendientes* con el conteo total |
| 🔄 en *Revisados* | El número vuelve a *Pendientes* |
| 5 desvíos seguidos | Una sola notificación "5 llamadas desviadas hoy", suena solo la primera |
| Filtro apagado | Timbra · sin registro |
| App cerrada + reinicio | Sigue filtrando |

**Pendiente:** pruebas unitarias de `ScreeningEngine` (inyectar DAOs falsos y un `SettingsRepository` en memoria).

**Depuración:** `adb logcat | grep -i -E "Telecom|CallScreening|filtrollamadas"`.

## 8. Publicar una versión

1. Subir `versionCode` y `versionName` en `app/build.gradle.kts`; registrar en `CHANGELOG.md`.
2. Crear keystore (una sola vez, **guárdalo fuera del repositorio**):
   ```bash
   keytool -genkeypair -v -keystore filtro-release.jks -alias filtro -keyalg RSA -keysize 4096 -validity 10000
   ```
3. Copiar `keystore.properties.ejemplo` a `keystore.properties` (ya está en `.gitignore`) con la ruta del
   `.jks`, el alias y las contraseñas. Luego:
   ```bash
   ./gradlew assembleRelease
   ```
   Sin ese archivo, `assembleRelease` genera un APK **sin firmar** (Android no lo instala).
   Alternativa gráfica: *Build → Generate Signed App Bundle / APK → APK → release*.
4. Crear un *Release* en GitHub con la etiqueta `vX.Y.Z` y adjuntar el APK firmado y su SHA-256:
   ```bash
   sha256sum app-release.apk
   ```
5. (Opcional) Publicar en F-Droid: la app ya cumple sus requisitos (código abierto, sin servicios propietarios).

## 9. CI

`.github/workflows/build.yml` compila el APK de depuración en cada *push* / *pull request* y lo adjunta como artefacto.
