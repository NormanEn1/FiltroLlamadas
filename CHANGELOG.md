# Changelog

Formato basado en [Keep a Changelog](https://keepachangelog.com/es-ES/1.1.0/) y [SemVer](https://semver.org/lang/es/).

## [1.0.0] — 2026-09-19

### Añadido
- Filtrado de llamadas con `CallScreeningService`: rechazo silencioso de números ocultos, desconocidos o bloqueados.
- Reglas: contactos, permitidos, "lo llamaste en N días", "insiste en N minutos", emergencias y códigos cortos siempre permitidos.
- Activación / verificación / desactivación del desvío del operador (`**67*`, `*#67#`, `##67#`) por la SIM elegida.
- Soporte doble SIM (proteger todas o una línea).
- Registro local por día con búsqueda, filtros, notas, acciones rápidas y exportación CSV.
- Bandeja **Números → Revisar**: desconocidos desviados agrupados por número con conteo de intentos,
  Permitir/Bloquear con un toque y *Deshacer*, "Bloquear insistentes" (≥ 3 intentos) y "Marcar todo revisado".
  Filtro **Pendientes / Revisados**: los revisados siguen visibles, se pueden devolver a pendientes y reaparecen
  solos si vuelven a llamar.
  Contador en la barra inferior y aviso en Inicio.
- Listas Permitidos (blanca) y Bloqueados (negra).
- Notificación resumen única con el total del día (suena solo la primera vez).
- Pantalla **Estadísticas** (desde Inicio → "Estadísticas"): períodos de 7/30/90 días, comparación con el período
  anterior (¿bajó el spam?), barras apiladas por día, motivos del desvío, franja horaria y los 5 números que más
  insisten. Gráficas dibujadas con Compose Canvas, sin librerías externas.
- Ajustes persistentes (DataStore), retención configurable del registro.
- Interfaz Material 3 con tema claro/oscuro y colores dinámicos.
- Muestra el **nombre del contacto** (agenda del teléfono) en Registro, en el detalle, en las listas, en la
  notificación y en el CSV, en lugar del motivo "En contactos". Se resuelve también para llamadas ya registradas.
- Números internacionales con formato `+<país>…`; `+57` se muestra como número local.
- Sin permiso `INTERNET` (eliminado explícitamente del manifiesto).
- Sección **Acerca de** en Ajustes: Filtro · Neom · versión 1.0.0 (2026), compilación, qué hace, privacidad,
  licencia MIT, requisitos, tecnologías, paquete y enlaces al repositorio y a los issues.
