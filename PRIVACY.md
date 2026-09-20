# Política de privacidad — Filtro

*Última actualización: 19 de septiembre de 2026*

**Resumen: Filtro no recopila, no envía y no comparte ningún dato. Todo se queda en tu teléfono.**

## Garantía técnica

La app **no tiene permiso de acceso a Internet** (`android.permission.INTERNET` está eliminado del manifiesto).
Por diseño del sistema Android, sin ese permiso la app **no puede** enviar información fuera del dispositivo.
Cualquiera puede comprobarlo en el código fuente o en *Ajustes → Apps → Filtro → Permisos*.

## Datos que la app usa (solo en el teléfono)

| Dato | Para qué | ¿Se guarda? |
|---|---|---|
| Número de la llamada entrante | Decidir si timbra o se desvía | Sí, en el registro local |
| Contactos (lectura) | Saber si quien llama es un contacto | No, solo se consulta |
| Registro de llamadas (lectura) | Regla "lo llamaste recientemente" | No, solo se consulta |
| Líneas SIM | Elegir la SIM protegida | Solo el nombre/ID de la línea elegida |
| Ajustes, números permitidos/bloqueados/revisados y notas | Funcionamiento de la app | Sí, localmente |

- El registro se borra automáticamente según el período que elijas (1 mes a 1 año) o manualmente.
- Las copias de seguridad automáticas de Android están **desactivadas** (`allowBackup=false`).
- La exportación CSV solo ocurre cuando tú la pides y el archivo va donde tú elijas.
- Al desinstalar la app se elimina todo.

## Terceros

Filtro no integra SDKs de analítica, publicidad, crash reporting ni servicios en la nube.

**Importante:** las llamadas desviadas son atendidas por el **teléfono destino** que tú configures
(p. ej. un iPhone) y viajan por la red de tu **operador**. Su tratamiento de datos se rige por las políticas de
esos terceros, no por Filtro. Si el destino graba o transcribe, informa a quien llama según la ley aplicable
(Colombia: Ley 1581 de 2012).

## Contacto

Dudas o reportes: abre un *issue* en el repositorio del proyecto.
