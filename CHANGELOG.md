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
- Ajustes persistentes (DataStore), retención configurable del registro.
- Interfaz Material 3 con tema claro/oscuro y colores dinámicos.
- Sin permiso `INTERNET` (eliminado explícitamente del manifiesto).
- Sección **Acerca de** en Ajustes: Filtro · Neom · versión 1.0.0 (2026), compilación, qué hace, privacidad,
  licencia MIT, requisitos, tecnologías y paquete.
