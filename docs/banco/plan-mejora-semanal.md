# Plan de mejora semanal del lector (23 nov 2026)

Decidido con Eduardo: **100 revisiones diarias**, **versión cada domingo** y **sin bloqueo de actualizaciones**.

## Calendario de la semana
| Día | Qué pasa | Quién |
|---|---|---|
| Lunes a sábado | 100 revisiones al día en Admin › Hoy: verdes y amarillos primero, luego rojos y errores; ilegibles aparte | Eduardo |
| Lunes 7:12 a.m. | Revisión semanal automática (rutina existente) | Claude |
| Sábado | Último día de revisiones de la semana | Eduardo |
| Domingo 14:23 (CDMX) | Recordatorio automático: corte (medir, agrupar, proponer 1 a 3 arreglos), «adelante» de Eduardo, corregir, medir antes y después, subir versión y mandar el link del APK. Guion: `plan-domingo.md` | Claude, con el «adelante» de Eduardo |
| Semana siguiente | Eduardo prueba la versión con Uber y DiDi y sigue revisando | Eduardo |

## Qué mide el corte del domingo
1. **Lectura completa** (detección, tarifa, km y minutos correctos a la vez) de las normales de la versión vigente, con denominador. Base: 89.9 % (408 de 454) en la 10.85.
2. **Falsos positivos** (pop-up donde no había oferta o de otra tarjeta). Meta: menos de 1 %.
3. Por **color**, **app**, **tarjetas** y **orientación**.
4. **Etapa que falla**: OCR, Parser o pop-up, con el diagnóstico del `.txt` y las etapas de la traza (Admin › Hoy).
5. **Ofertas no detectadas** (motivo de rechazo del Parser) y reportes «Leyó mal».
Las muestras chicas se avisan (menos de 30 por grupo). Los casos de la versión 10.85 y anteriores con desfase siguen aparte (`DESFASE_TEMPORAL_CAPTURA_NO_EVALUABLE`).

## Regla para corregir
- Una causa entra a la versión solo con **3 o más casos reproducibles** con líneas reales (diagnóstico) o captura alineada con la lectura (desde la 10.86 la captura es la del fotograma).
- Prioridad: falsos positivos, ofertas no detectadas, otra tarjeta, km, minutos, tarifa, menores.
- Cada arreglo: `docs/banco/calificar.sh` antes y después, sin regresiones, caso nuevo en `app/src/test/resources/ofertas/`, `herramientas/parser.sha256`, `GuardiaParserTest` y JUnit con Gradle.
- No se modifica el banco para mejorar el número. Una respuesta esperada mal se corrige solo con autorización de Eduardo y un comentario en el archivo.
- Si el domingo no hay una causa demostrada, no sale versión de lector ese domingo.

## Salida de versión (domingo, `plan-domingo.md` pasos 6 y 7)
1. Subir `versionCode` y `versionName`; Novedades en lenguaje de conductor; `docs/historial-versiones.md`.
2. Commit y push a `main` (y a la rama de la sesión). «Compilar flavor play» da el APK.
3. El AAB para Play solo con la autorización de Eduardo; «Seguridad de los datos» de Play al día antes de publicar.
4. Fallo grave (la app se cierra, no abre, no lee nada): versión inmediata fuera de calendario.

## Pendientes que alimenta el plan
N829/N853/N854 (pantallas que no son oferta), decimal perdido de Uber, D2386, multiplicadores mal leídos, lo que salga de la prueba del pop-up nuevo en teléfono y tablet, y que el pop-up viejo se quite sin quitar uno bueno.
