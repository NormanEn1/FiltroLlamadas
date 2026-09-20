# Cómo contribuir

¡Gracias por tu interés! Toda ayuda es bienvenida: reportes, traducciones, pruebas con otros operadores y código.

## Principios del proyecto (no negociables)

1. **Sin permiso `INTERNET`** ni SDKs de terceros que envíen datos.
2. **Ante la duda, la llamada timbra**: ningún cambio puede hacer que se pierda una llamada legítima.
3. Sin root ni APIs de sistema.

## Reportar un problema

Usa las plantillas de *Issues*. Indica modelo, versión de Android, **operador y país** (el desvío depende de él)
y, si aplica, el motivo mostrado en el Registro.

## Enviar cambios

1. Haz *fork* y crea una rama: `feature/descripcion` o `fix/descripcion`.
2. Sigue el estilo existente (Kotlin oficial, Compose, textos de UI en español).
3. Verifica que compila: `./gradlew assembleDebug lint`.
4. Si cambias reglas o datos, actualiza `docs/ARQUITECTURA.md` y `CHANGELOG.md`.
5. Abre un *Pull Request* describiendo qué cambia y cómo lo probaste (emulador y/o dispositivo real).

## Ideas bienvenidas

- Pruebas unitarias del motor de reglas.
- Traducción al inglés (`values-en/strings.xml` y textos de UI).
- Informes de compatibilidad por operador (códigos de desvío, costos, si conserva el número original).
