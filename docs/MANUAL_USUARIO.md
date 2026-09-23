# Manual de usuario — Filtro

## ¿Qué hace Filtro?

Cuando te llama un número **oculto** o que **no está en tus contactos**, tu teléfono **no suena**: Filtro rechaza
la llamada en silencio y tu operador la **desvía a otro teléfono** (tu "asistente"), por ejemplo un iPhone con
iOS 26 que contesta, pregunta quién llama y te muestra la transcripción. Filtro guarda un **registro** de todo.

Tus contactos, tus conocidos y quien insiste **siguen timbrando normal**.

---

## 1. Antes de empezar

Necesitas:

- ✅ Teléfono Android 10 o superior con la app **Filtro** instalada.
- ✅ Un **teléfono destino** con otra línea. Recomendado: iPhone con iOS 26.
- ✅ Que tu operador permita el **desvío de llamadas** (casi todos lo permiten). Consulta si te cobra los minutos desviados;
  con planes de minutos ilimitados nacionales normalmente no tiene costo.

### Preparar el iPhone (si es tu destino)

1. *Ajustes → General → Actualización de software* → iOS 26.
2. *Ajustes → General → Idioma y región*: **Español** + tu región.
3. *Ajustes → Apps → Teléfono → Filtrar llamadas desconocidas* → **Preguntar motivo de la llamada**.
4. *Ajustes → Apps → Teléfono* → activa **Buzón de voz en directo**.
5. Verifica que el iPhone **no** tenga desvío hacia tu Android (evita bucles).

---

## 2. Configuración inicial (5 minutos)

### Paso 1 — Completar la configuración pendiente
Abre Filtro. En **Inicio** verás *Configuración pendiente*:

| Ítem | Qué hacer | ¿Para qué? |
|---|---|---|
| Filtro de llamadas | **Asignar** → elige *Filtro* | Permite revisar las llamadas antes de que suenen |
| Contactos | **Permitir** | Reconocer a tus contactos (solo lectura) |
| Registro de llamadas | **Permitir** | Dejar pasar a quienes llamaste hace poco |
| Líneas y marcación | **Permitir** | Elegir la SIM y marcar el código de desvío |
| Notificaciones | **Permitir** | Aviso resumen de llamadas desviadas |
| Número destino | **Configurar** | Ver paso 2 |

### Paso 2 — Número destino
**Ajustes → Destino de la redirección**: escribe el número del teléfono destino (10 dígitos), un nombre
(p. ej. "iPhone") y pulsa **Guardar**. Si tienes doble SIM, en **Línea protegida** elige cuál filtrar.

### Paso 3 — Activar el desvío en el operador
**Inicio → Redirección → Activar → Marcar código.** El teléfono marca `**67*<número>#` y el operador responde en pantalla.

- Si responde error: en Ajustes activa **Anteponer indicativo 57** y repite.
- **Verificar** consulta el estado (`*#67#`). **Desactivar** lo quita (`##67#`).
- Solo se hace una vez (o cuando cambies de destino).

### Paso 4 — Encender la protección
En **Inicio**, activa el interruptor **Protección activa**. ¡Listo! Ya puedes cerrar la app.

---

## 3. Uso diario

- **No necesitas abrir la app.** Funciona sola, también después de reiniciar el teléfono.
- Recibes **una sola notificación** que se actualiza: *"7 llamadas desviadas hoy · Última: número · motivo"*.
  Suena solo la primera vez; las siguientes se suman en silencio.
- El mensaje o transcripción lo ves **en el teléfono destino**.
- **Rutina recomendada (1 minuto al día):** abre **Números → Revisar**, permite a quien reconozcas y pulsa
  **Bloquear insistentes**.

### Pantallas

**Inicio** — estado de la protección, aviso *"N números por revisar"*, redirección, contadores (desviadas hoy / 7 días, permitidas hoy) y actividad reciente.

**Números** — la bandeja para decidir rápido, sin leer el registro completo:

| Pestaña | Qué muestra | Acciones |
|---|---|---|
| **Revisar → Pendientes** | Cada número desconocido desviado **una sola vez**, con la cantidad de intentos y el último; marca *Insistente* desde 3 intentos | ✅ **Permitir** · ⛔ **Bloquear** (con *Deshacer*) · **Bloquear insistentes** · **Marcar todo revisado** |
| **Revisar → Revisados** | Los que ya marcaste como revisados y no han vuelto a llamar | 🔄 **Volver a pendientes** · ✅ **Permitir** · ⛔ **Bloquear** |
| **Permitidos** | Siempre timbran (domicilios, médico, colegio…) | Agregar · quitar |
| **Bloqueados** | Siempre se desvían, **aunque insistan** o estén en contactos | Agregar · quitar |

> ¿Por qué bloquear si los desconocidos ya se desvían? Por la regla *"Dejar pasar si insiste"*: los robots de
> spam suelen remarcar a los pocos minutos y timbrarían. Bloqueados, nunca pasan.
> *Marcar todo revisado* solo limpia la bandeja: los números pasan a **Revisados**, siguen tratándose como
> desconocidos y **vuelven solos a Pendientes si llaman otra vez**.

**Estadísticas** (Inicio → *Estadísticas*) — para responder "¿está bajando el spam?":

| Bloque | Qué muestra |
|---|---|
| Período | 7, 30 o 90 días |
| Tendencia | *"Bajaron 37%"* comparando con el período anterior de igual duración |
| Totales | Desviadas, permitidas y promedio de desviadas al día |
| Llamadas por día | Barras apiladas: desviadas (rojo) sobre permitidas (verde) |
| Motivos del desvío | Cuántas por oculto, desconocido o bloqueado, con porcentaje |
| A qué hora llaman | Volumen por hora del día (útil para el modo No molestar) |
| Los que más insisten | Los 5 números con más intentos |

**Registro** — todas las llamadas revisadas, agrupadas por día. Si el número está en tu agenda se muestra el
**nombre del contacto** y debajo el número.
- Busca por número, motivo o nota. Filtra *Todas / Desviadas / Permitidas*.
- Toca una llamada para: añadir **nota**, **llamar**, **copiar**, **permitir**, **bloquear**, **eliminar**.
- ⬇️ exporta todo a CSV (se abre en Excel). 🧹 borra el registro.

**Ajustes**

| Opción | Efecto |
|---|---|
| Desviar números ocultos | Llamadas "privadas" van al destino |
| Desviar números desconocidos | Números fuera de tus contactos van al destino |
| Dejar pasar a quienes llamaste | Si llamaste a ese número en los últimos 7/15/30/90 días, timbra |
| Dejar pasar si insiste | Si el mismo número vuelve a llamar en 2–10 min, timbra (útil en urgencias) |
| Avisar cada desvío | Notificación resumen del día |
| Conservar el registro | 1 mes / 3 / 6 meses / 1 año; lo anterior se borra solo |
| Tema / colores dinámicos | Apariencia |
| Acerca de | Nombre y versión de la app, autor, licencia, requisitos y paquete |

---

## 4. Prueba recomendada

1. Pide a alguien **que no esté en tus contactos** que te llame.
2. Tu Android **no debe sonar** y debe llegar la notificación de desvío.
3. El teléfono destino debe recibir la llamada **mostrando el número de quien llamó**.
4. Si esa persona vuelve a llamar antes de 3 minutos, esta vez **debe sonar** en tu Android.
5. Revisa ambas llamadas en **Registro**, y el número en **Números → Revisar** (con 1 intento).
6. Pulsa **⛔ Bloquear** y pide que llame otra vez: ahora **no debe sonar**, ni siquiera insistiendo.
   Luego pásalo a **Permitidos** (o quítalo de Bloqueados) para dejarlo como estaba.

---

## 5. Preguntas frecuentes

**¿Filtro lee o envía mis contactos a algún lado?**
Los lee **solo en el teléfono** para comparar números. La app **no tiene permiso de Internet**: no puede enviar nada.

**¿Gasta batería?**
Prácticamente nada. Android la despierta solo cuando entra una llamada, durante milisegundos.

**¿Tengo que revisar la bandeja "Números" todos los días?**
No es obligatorio: sin revisar, los desconocidos igual se desvían. Revisarla sirve para dos cosas: dejar pasar a
alguien legítimo (*Permitir*) y cortar a los robots que remarcan (*Bloquear*).

**¿Qué diferencia hay entre "Bloquear" y "Marcar todo revisado"?**
*Bloquear* hace que el número **nunca** timbre. *Marcar revisado* solo lo pasa a **Revisar → Revisados**; sigue
tratándose como desconocido (se desvía, pero timbraría si insiste) y regresa a Pendientes si vuelve a llamar.

**Marqué todo como revisado y "desaparecieron".**
Están en **Números → Revisar → Revisados**. Desde ahí puedes devolverlos a pendientes (🔄), permitirlos o bloquearlos.
Todas sus llamadas siguen además en **Registro**.

**Me equivoqué al permitir o bloquear.**
Pulsa *Deshacer* en el aviso inferior, o ve a *Permitidos* / *Bloqueados* y quítalo con 🗑️.

**¿Qué pasa si el teléfono está apagado o sin señal?**
Filtro no actúa; las llamadas van al buzón del operador como siempre.

**¿Qué pasa si rechazo yo mismo la llamada de un contacto?**
También se desvía al destino (el desvío es "por ocupado"). Si no te conviene, desactiva el desvío.

**¿Y las llamadas de emergencia o códigos cortos?**
Nunca se desvían.

**Instalé otra app anti-spam y Filtro dejó de funcionar.**
Android solo permite **un** filtro de llamadas. En Inicio vuelve a **Asignar** el rol a Filtro.

**¿Cuesta algo?**
La app es gratuita. El único costo posible es el del operador por los minutos desviados.

---

## 6. Solución de problemas

| Síntoma | Causa probable | Solución |
|---|---|---|
| Un desconocido sí timbra | Protección apagada o rol no asignado | Inicio: sin pendientes + interruptor activo |
| Nadie timbra y la llamada no llega al destino | Desvío no activo en el operador | Inicio → **Verificar**; vuelve a **Activar** |
| El operador responde error al activar | Formato del número | Ajustes → *Anteponer indicativo 57* |
| El destino muestra el número de tu Android | El operador no conserva el número original | Consulta al operador; el filtrado sigue funcionando |
| Un conocido se desvía | No está en contactos o está bloqueado | Números → Revisar → **Permitir**, o guárdalo en contactos |
| Un spam timbró | Remarcó enseguida (regla "insiste") | Números → **Bloquear** (o *Bloquear insistentes*) |
| No llegan notificaciones | Permiso o ajuste desactivado | Ajustes → *Avisar cada desvío* + permiso de notificaciones |
| Filtro dejó de actuar tras "Forzar detención" | Android la detuvo | Abre la app una vez |

## 7. Desinstalar

1. **Inicio → Redirección → Desactivar** (o marca `##67#`) para quitar el desvío del operador.
2. Desinstala la app normalmente. El registro se borra con ella.
