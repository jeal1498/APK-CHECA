# Accesibilidad, captura de pantalla y video: justificación para Play Console

**Estado:** borrador del 21 de octubre de 2026. Fuentes: [uso de AccessibilityService](https://support.google.com/googleplay/android-developer/answer/10964491) y [servicios en primer plano](https://support.google.com/googleplay/android-developer/answer/13392821). Los textos de la app se verificaron contra el código del flavor `play`.

## Lo que hace la app (verificado en el código)
- `RutaService` **no** declara `isAccessibilityTool`: Checa no es una herramienta de accesibilidad y debe cumplir la divulgación destacada.
- No ejecuta acciones: no hay `performAction`, `dispatchGesture` ni `canPerformGestures`. Solo lee y muestra un pop-up.
- Al conectarse, limita su alcance a los paquetes cuyo nombre contiene «uber» o «didi» (`soloUberYDidi()`).
- `CapturaService` usa MediaProjection y analiza **toda la pantalla** cada 1 s (`INTERVALO_MS = 1000`) con ML Kit en el teléfono. Lo que no es una oferta se descarta, y no hay envío de red.
- Con «Guardar pantallas de ofertas» se guardan texto y recorte, solo si aparece un tramo «N min (X km)», localmente y máximo 7 días.
- Notificación visible mientras corre: «Checa está leyendo la pantalla», con botón «Detener».

## Declaración de la Accessibility API (Play Console → Contenido de la app)
| Pregunta | Respuesta |
|---|---|
| ¿Es una herramienta de accesibilidad? | No (no se declara `isAccessibilityTool`) |
| ¿Por qué usa la API? | Funcionalidad de la app |
| ¿Recopila o comparte datos personales o sensibles con la API? | **No**, porque el contenido leído se procesa solo en el teléfono. Si algún día se envía fuera del dispositivo, hay que cambiar la respuesta y el formulario Data safety |
| Video | Ver guion abajo |

## Divulgación destacada: revisión contra los requisitos de Play
| Requisito de Play | Estado |
|---|---|
| Dentro de la app, no solo en la ficha o la web | Cumple (diálogo en `abrirAccesibilidad()`, solo flavor play) |
| Se muestra en el uso normal, sin navegar por menús | Cumple: aparece al activar el lector desde la guía de permisos |
| Describe qué datos se leen | Cumple: texto visible, tarifa, distancia, tiempo |
| Explica cómo se usan o comparten | Cumple: se procesa en el teléfono; no se envía ni se comparte |
| Consentimiento con acción afirmativa | Cumple: «Acepto y continuar» / «Ahora no» |
| Separada de otras divulgaciones | Cumple: la captura tiene su propio aviso (commit `a7ad302`) |

## Divulgación de captura de pantalla (implementada en `a7ad302`, solo flavor play)
Antes de ese commit, `iniciarCaptura()` pedía el permiso solo con el diálogo del sistema. Como Play exige que la divulgación de accesibilidad sea separada, la captura tiene ahora la suya, con consentimiento afirmativo y guardado en `play_policy`. Aparece antes del aviso de Android, y si la persona toca «Ahora no», no se pide la captura. Texto vigente en la app:

> **Permiso para leer la pantalla**
> Para calcular tus ofertas, Checa usa la captura de pantalla de Android. Mientras la lectura está activa, Checa analiza en este teléfono todo lo que se muestra en la pantalla, no solo Uber o DiDi, para encontrar ofertas de viaje (tarifa, distancia y tiempo). Lo que no es una oferta se descarta. Nada de esto se envía a los servidores de Checa ni se comparte con terceros.
> Si activas «Guardar pantallas de ofertas», los datos se guardan solo en este teléfono hasta 7 días y pueden incluir información personal visible.
> Android te pedirá confirmar la captura y verás una notificación mientras esté activa; puedes detenerla cuando quieras desde esa notificación. Evita mostrar información sensible, como apps de banco, mientras la lectura esté activa.
>
> Botones: **«Acepto y continuar»** / **«Ahora no»**.

## Declaración del servicio en primer plano (tipo `mediaProjection`)
- **Función:** al tocar «Iniciar lectura» y aceptar el aviso de Android, `CapturaService` captura la pantalla cada segundo, reconoce en el teléfono la tarifa, los km y los minutos y muestra un pop-up con $/km, $/hora y ganancia neta.
- **Si el sistema la retrasa:** no empieza la lectura y la persona no recibe el análisis de ofertas hasta iniciarla de nuevo.
- **Si el sistema la interrumpe:** se detiene la lectura y se deja de mostrar el pop-up; la app avisa con una notificación («Checa dejó de leer») para volver a iniciarla.
- **Caso de uso:** el de MediaProjection del listado de Play; si no encaja, descríbelo manualmente.
- **Para el revisor (EN):** *Checa is an independent tool for ride-hailing drivers. When the user taps "Start reading" and accepts the Android screen-capture prompt, a foreground service (mediaProjection) captures the screen once per second and runs on-device text recognition to extract fare, distance and time, then shows an overlay with earnings per km and per hour. Nothing is sent to a server. A persistent notification with a Stop button is shown while it runs.*

## Guion del video 1: divulgación de accesibilidad (flavor play, teléfono real)
Debe incluir los cinco puntos que pide Google:
1. Abrir Checa en el teléfono.
2. Recorrer el flujo hasta el diálogo «Permiso para leer ofertas» y **mostrarlo completo** (si hay que desplazar, hacerlo despacio).
3. Tocar «Acepto y continuar» y activar el servicio en Ajustes de Accesibilidad.
4. Repetir desde cero y tocar «Ahora no»; mostrar que el lector no se activa y cómo volver a ver el diálogo.
5. Mostrar la función principal: una oferta en pantalla y el pop-up de Checa (si no es obvio, narrar o subtitular).

## Guion del video 2: servicio en primer plano
1. Abrir Checa con el lector ya activado.
2. Tocar «Iniciar lectura» y mostrar el aviso «Permiso para leer la pantalla» completo y el diálogo de Android.
3. Mostrar la notificación «Checa está leyendo la pantalla».
4. Mostrar una oferta y el pop-up.
5. Tocar «Detener» y mostrar que la lectura termina.

## Pendiente
1. Grabar los dos videos con la versión final del AAB.
2. Revisar el texto de `servicio_desc` («No envía datos a ningún lado»): es demasiado absoluto, porque la app sí sincroniza cuenta y ajustes. Propuesta: «El contenido que lee se procesa en tu teléfono». Se aplicaría solo al flavor play.
