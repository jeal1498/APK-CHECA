# CLAUDE.md: Checa

Lo vigente para trabajar en Checa. La historia con fecha y el detalle largo viven en `docs/` (índice al final): léelos solo cuando la tarea lo pida. Si algo de `docs/` contradice este archivo, gana este archivo.

## Qué es
**Checa** (antes Luz Verde / Neta / Mi Ruta) es una app Android para conductores de **Uber y DiDi**. Cuando llega una oferta de viaje, lee la pantalla y muestra un **pop-up** con la ganancia neta después de gasolina, **$/hora**, **$/km**, el semáforo (🟢 conviene / 🟡 regular / 🔴 no conviene) y el multiplicador ⚡.
- Escrita **desde cero**. **No** se modificó, descompiló ni copió el APK de «Ruta Rentable» (solo sirve como referencia de qué funciones tener).
- En el material de marca: sin ciudad ni país, sin logos de Uber/DiDi, sin nombres de competencia.
- Usuario: **Eduardo** (Lalo, hombre: tratarlo en masculino; la cuenta de Claude está a nombre de Karen; donde algún documento diga «ella», es él). Usa la app en su teléfono y en una tablet en horizontal con pantalla dividida (DiDi a la izquierda, Uber a la derecha).

## Cómo trabajar con Eduardo
- **Respuestas cortas, en español.** Él escucha el chat con lectura en voz alta: nada de textos largos.
- Cambios **directo a `main`** (tiene permiso) en commits pequeños; sube también la rama de la sesión. Ningún secreto en el repo. No publicar en Play Console sin su autorización.
- Él prueba en su teléfono con el APK del workflow **«Compilar flavor play»** (`play-compilar.yml`, artefacto de la última corrida en `main`), no con el AAB de Play. Cuando haya algo que probar, mándale el link de esa corrida.
- Él prueba en su teléfono y manda capturas. Para fallas de lectura, pídele que active «Guardar pantallas de ofertas» y comparta lo guardado.
- Tras un `/context` (o `/clear`), termina tu respuesta sugiriendo en una línea: «¿Quieres retomar con `/parte`?».
- `rm` con comodín está bloqueado en el entorno: usa `mv`. Tareas de más de 2 min, en segundo plano.

## Estilo de respuesta (i-have-adhd, siempre activo)
Quien lee tiene TDAH. Cada respuesta debe poder ejecutarse:
1. Primero la respuesta o la siguiente acción (comando, ruta o fragmento).
2. Pasos numerados, una acción acotada por paso.
3. Termina con una acción siguiente de menos de 2 minutos.
4. Cierra el tema actual antes de abrir otro.
5. Retoma el avance en cada turno («paso 3 de 5»).
6. Tiempos en unidades concretas, nunca «un poco».
7. Tras un cambio, muestra qué funciona ahora.
8. Errores: lugar, causa y arreglo. Sin dramatismo.
9. Listas de máximo 5 elementos.
10. Sin preámbulo, sin recapitulación, sin cierres.

Excepciones: explica completo si lo piden; confirma antes de acciones destructivas; tras tres arreglos fallidos, detente y nombra la suposición dudosa; si la petición es ambigua, una pregunta corta.

## Videos (TikTok, Reels, Shorts)
- **Frase de arranque: «Lote de videos: …»** (con temas, o «propón los temas»). Al leerla: `git pull origin main` en los dos repos, **revisa `video/referencias/` (README y archivos nuevos; si hay una referencia sin fila en el README, léela y úsala como guía de diseño)**, lee `marketing/plan/plan-ponerse-al-dia.md`, invoca la skill `guion-checa` con la herramienta Skill y arranca el flujo de dos aprobaciones (abajo). Si no trae temas, propón 3–4 con lo que más retiene y espera su elección.
- **Guiones:** siempre con la skill **`guion-checa`**. **Producción** (voz, escenas, portada, copy, calendarios y control de calidad): skill **`video-checa`** con la herramienta `video/scripts/checa.py` y el motor `video/hyperframes/_base/`.
- **Flujo: dos aprobaciones por lote.** (1) Eduardo aprueba juntos los guiones del lote (cada tema con CHECA y su v2 PERSONAL); (2) Claude produce todo sin preguntar, pasa el control de calidad y le manda el lote completo para una sola revisión; se corrige solo lo que él marque. «Publicado» lo marca él.
- **CTA vigente:** «Comenta CHECA y te paso el acceso a la prueba en Google Play». Cuentas: `@checa.driver` (Checa) y `@Eduardoalv9802` (personal).
- **Estado:** el real está en el calendario web (`PWA-Checa-Multimedia/calendario-d4f81c/data.json`); `marketing/plan/calendario.md` lo acompaña y `marketing/plan/plan-ponerse-al-dia.md` tiene el plan. Al empezar: `git pull origin main` en los dos repos.
- **Se sube video todos los días, domingos incluidos** (Eduardo, 31 oct; antes los domingos no se subía).
- **Énfasis (Eduardo, 31 oct):** lo escribe Gémini en una lista bajo cada línea de voz (`fuerte`, `pausa`, `tono`, `voz`, `pantalla`) sin cambiar el texto; formato y cómo se aplica en `.claude/skills/video-checa/references/narrador.md` §7. Los gráficos de cada énfasis son golpes visuales que caen en la palabra (escena `impacto`, §8). Estilo híbrido teléfono + golpes (oferta real, pop-up de la app, `adelanto` 0.04): `narrador.md` §9.
- **Siempre que se pida un video nuevo** (lote, tema suelto, rehacer o proponer), antes de escribir o producir: `git pull origin main` y mirar `video/referencias/` por referencias nuevas (`ls` + README). Si hay alguna nueva, decir en una línea qué toma el video de ella.
- **Estilo de video vigente (10 nov):** «claro» (mapa de nodos, pop-up y oferta réplica de la app), `checa.py claro`, detalle en `narrador.md` §00. Los videos de referencia de diseño se guardan en `video/referencias/` (con su README).
- El `video.mp4` se commitea **una sola vez**, ya aprobado (cada mp4 commiteado engorda el repo para siempre).

## Revisión de lecturas (DeepSeek / Haiku)
- **Frase de arranque: «Estado de la revisión»** (opcional: «de hoy», «de la semana», «del mes»). Al leerla, con las herramientas de Supabase (proyecto `gqsadclvftkzwrihfipz`) responde en máximo 5 líneas: revisiones y ofertas del periodo, % que coincidió, correcciones y rescates (`errores_lector`, con su `veredicto` si ya lo hay), tiempos (mediana de `ms_popup` y `ms_ia`), costo real en USD y MXN por conductor (`consumo_conductor_dia`/`_mes`, `margen_planes`) y fallas (`fallas_ia`; `http_402` = sin saldo en DeepSeek). Sin cambiar código.
- **Formato de las métricas (Eduardo, 11 nov):** cuando pida métricas, una tabla con una fila por conductor y estas columnas: Conductor (primeros 8 caracteres del id) · Revisiones · Coincidió (n y %) · Diferencias · Apps · Versión · IA (tiempo medio) · Costo (USD). Excluye a `a23854c9` (es Eduardo) salvo que pida incluirlo. Los tiempos y costos van de `revisiones`; «coincidió» solo significa que la IA leyó lo mismo que el lector local, no que sea correcto.
- La revisión semanal automática corre los lunes a las 7:12 a.m. (CDMX) en la sesión donde se creó; si esa sesión ya no existe, se hace a mano con la frase «Revisión semanal del lector».

## Entorno y compilación
- No hay Android SDK, Gradle ni Kotlin en el contenedor: **no se puede compilar localmente**. Revisa el Kotlin con cuidado antes de subir; las regex se prueban con `python3 -I`. Hay git, Python 3.12, Node 22, Java 21 (sin `javac`), ffmpeg y Playwright (`NODE_PATH=$(npm root -g)`).
- La red solo permite GitHub, PyPI y npm. No llega a `productionresultssa1.blob.core.windows.net` (artefactos y logs de Actions); el estado de los workflows sí se lee con las herramientas de GitHub MCP (el repo es privado).
- **Dos canales (flavors):**
  - `directo` (`com.karen.miruta`, `MiRuta.apk` en la web): `build.yml` compila en cada push a `main` y publica el release `vN` (`https://github.com/jeal1498/Mi-Ruta/releases/download/vN/MiRuta.apk`). **Ignora** `app/**`, `marketing/**`, `video/**`, `docs/**`, `.claude/**`, `pedidos/**`, `supabase/**` y `CLAUDE.md`, así que un cambio solo de la app no genera release. Firma local por variables `MIRUTA_*`; la llave nunca va a Git. Se **cierra el 15 nov 2026** (ver «Estado actual»).
  - `play` (`com.jeal1498.checa`, Google Play): `play-compilar.yml` (push a `app/**`) compila y verifica; `play-aab-firmado.yml` (manual) genera el AAB firmado con los secretos `PLAY_UPLOAD_*`. Sin auto-actualización, sin Mercado Pago ni `REQUEST_INSTALL_PACKAGES`. Desde 10.9 **sí** pide la excepción de batería (`REQUEST_IGNORE_BATTERY_OPTIMIZATIONS`, decisión de Eduardo, 6 nov); hay que declararla en Play Console. `BuildConfig.ES_PLAY` separa el comportamiento.
- Stack: AGP 8.10.1, Kotlin 1.9.24, compileSdk/targetSdk 36, minSdk 26. **UI 100 % programática** (sin XML de layouts, sin AppCompat ni Compose). Dependencias: ML Kit `text-recognition:16.0.1` (OCR en el teléfono) y `androidx.credentials` + `googleid` (login con Google). INTERNET solo para cuenta y licencia; la lectura de ofertas no usa internet.
- `versionCode` / `versionName` en `app/build.gradle.kts` (compartidos por los dos flavors): súbelos en cada versión y nunca bajes de 67. Historial en `docs/historial-versiones.md`.

## Estado actual (30 oct 2026)
- **Versión:** `main` trae `versionCode` 143 / **10.63** (11 nov: rediseño de Zonas bloqueadas con lista, nombres y contador; 10.62: búsqueda de zonas limitada al recuadro y registro «Zonas» en Guardar pantallas; 10.61: zonas bloqueadas por origen y destino, con nombre, `ZonasOrigenDestinoTest`, sin probar en teléfono; 10.60: zonas bloqueadas con mapa, `ZonasActivity.kt`/`Zonas.kt`, sin probar; 10.59: borde a borde Android 15; teléfono, Android, orientación, zonas, tarjetas y color en cada revisión; Seguridad de los datos por actualizar; cascada de lectores: texto primero, la foto confirma sin IA (`coincidencias`) o la IA revisa las dos (`desacuerdo`, `acerto`), rescate solo desde la foto; aviso de versión nueva de Play al abrir, `ActualizacionPlay.kt`; lector de texto solo con lo visible, un pop-up en vertical (dos solo en horizontal), `lector` en `revisiones`; el pop-up se quita cuando la oferta deja de verse y varias tarjetas de DiDi se leen por separado, `Tarjetas.kt`; la guía insiste en poner la gasolina real; revisión y rescate también sin zonas de ventana; rescate de ofertas que el lector no entiende; 10.49–10.50: métricas y consumo; revisión de lecturas en segundo plano, `Confirmador.kt`; 10.46–10.47: lector y detector con el resumen «N min · X km · $Y/km» de Uber; 8 nov: widget «Mi QR» 1×1 con QR al WhatsApp del conductor para pasajeros (`QrActivity.kt`, zxing core); 10.41: 8 nov: widgets nuevos (mosaico 4×2, Mi día, Descanso y SOS; `Widgets.kt`, `Manejo.kt`) y Novedades con vista previa; 10.40: 8 nov: inicio automático al abrir Uber/DiDi con aviso «Lista para trabajar», apagado manual y aviso «¿Terminaste?» a los 30 min (`AvisoListo.kt`); 10.39: 8 nov: país, apariencia, resumen de Mejores horas (`horas_checa`) y viajes de la semana viajan con la cuenta; Seguridad de los datos por actualizar según `docs/google-play/data-safety.md`; 10.38: vigilante que reanuda la lectura; 10.34: 6 nov: aviso y botón de Novedades (`Novedades.kt`: agrega un bloque en cada versión); 10.33: 6 nov: modo claro pulido (ámbar, bordes); 10.32: 6 nov: modo claro en Ajustes › País › Apariencia; 10.31: 6 nov: interfaz compacta: tarjetas 12 dp, 8 dp entre ellas, radio 16, filas de 48 dp, botón de 44 dp; 10.30: 6 nov: imagen de «Compartir mi día» como tarjeta con borde de semáforo y widget sobre azulejo asfalto; 10.29: avisos de actualización y migración con versión, % grande y cuenta regresiva; 10.28: Paquetes con 2 beneficios y «Ver todo», Invitar con código y «Copiar»; 10.27: Equipos primero «Este equipo», Ayuda y soporte con franja y contacto fijo abajo, Tus datos con plazos; 10.26: Ayuda con la lectura con franja de estado y detalle con texto plegable; 10.25: País con tu país arriba y Apps que lee con una tarjeta por app; 10.24: Desgaste rápido/detallado y Tarifa mínima con ejemplo en vivo; 10.23: SOS con el 911 primero y formulario propio para agregar contacto; 10.22: Registrar viaje con total y Meta del día con atajos; 10.21: Mejores horas de la semana en siete tiras; 10.20: «¿Algo falla?» con titular y un solo botón; 10.19: permisos como tarjetas con estado y vista previa en «Tus números» de la guía; 10.18: Cuenta en tres grupos con el plan como etiqueta; 10.17: rediseño de Inicio, Mis metas y Ajustes; 10.16: equipos sin límite, también en Supabase; 10.15: orden de pantallas: «Mostrar lo que te queda» en Ajustes › Pop-up, País y Contactos SOS en Ajustes › País y seguridad, «Meta del día»; 10.14: guía de permisos de 4 pasos, botón que dice lo que sigue e interruptor en «Guardar pantallas»; 10.13: pop-up más legible: ✕ de 48 dp, ✓/✕ en métricas, avisos y acción más grandes; 10.12: desgaste por conceptos y tarifa mínima en Tu auto; 10.11: pop-up E con monto grande y «min (km) total», encabezado fijo con botón SOS en todas las pestañas; 10.9: en Android 11+ la lectura captura con el servicio de accesibilidad, `takeScreenshot()`, solo cuando hay eventos de Uber/DiDi, sin aviso de captura y se reanuda sola; `CapturaService` queda de respaldo; ver `docs/historial-versiones.md`; 10.8: pop-up C y País; 5 nov: lectura más rápida y con menos batería: imagen reducida a 1600 px, filtro de cambios conservador, reintento rápido si no se entiende una oferta y salto «sin Uber/DiDi» de 4 a 1 lectura; antes 10.6: metas por defecto $10/km y $200/h, ⚡ ya no tapa el monto, guía con el pop-up a su tamaño y «Entrar sin iniciar la lectura», sin «Apps de banco» en Ajustes; desgaste por km en Mis metas › Tu auto, que se resta en la neta del pop-up; OCR se salta sin Uber/DiDi en pantalla, captura de pantalla completa en Android 14+ e historial en otro hilo; 4 nov: pop-up V2 de 175 dp con deslizador de tamaño y Clientela en pausa, en prueba; antes moneda, botón Clientela, «Mi día» unificado y SOS con ubicación al tocar; Seguridad de los datos por actualizar en Play Console según `docs/google-play/data-safety.md`). Último release del canal directo: **v80 (9.5)** (pendiente el de la 9.6). Play: **AAB 138 (10.58) enviado a revisión en Prueba cerrada «Alpha»** (11 nov); antes AAB 133 (10.53), ya aprobado (Seguridad de los datos con «Estadísticas» en Otras acciones); antes AAB 74 (9.4) en Prueba interna y Alpha (México).
- **Migración al canal Play (30 oct):** la 9.5 ya tiene release directo (v80) y la web manda a `/registro/`. La web cambió de nombre a `PWA-Checa-Multimedia`: la 9.6 corrige las direcciones dentro de la app y **necesita un release directo nuevo** (Eduardo lo lanza con «Run workflow» en `build.yml`; yo no tengo ese permiso).
- **Google Play:** falta juntar **12 testers dentro 14 días seguidos** para pedir producción. `/registro/` (PWA-Checa-Multimedia) guarda nombre y Gmail en la tabla `testers` de Supabase y manda a la comunidad de WhatsApp; Eduardo pasa los correos a mano a la lista «Testers» de Play Console. El registro cierra al llegar a 50 o el 15 nov 2026.
- **Fase 7 (Play Billing)** pendiente: debe estar lista antes del **1 feb 2027**, cuando termina la beta gratis (`Licencia.BETA_HASTA` = 31 ene 2027). Necesita su «adelante».
- Detalle e historia: `docs/google-play/bitacora.md`.

## Arquitectura (`app/src/main/java/com/karen/miruta/`)
| Archivo | Qué hace |
|---|---|
| `RutaService.kt` | AccessibilityService: pop-ups como `TYPE_ACCESSIBILITY_OVERLAY` (uno por app), `zonasApps()` (ventana de Uber/DiDi en pantalla dividida), lectura por accesibilidad y `mostrarOferta()` (sin duplicados en 60 s, `Comparador`, alertas, historial). |
| `LectorOcr.kt` / `Lectura.kt` | OCR común de las dos capturas (agrupa por zona, filtro de cambios, reintento rápido) / estado común de la lectura y «Detener». En Android 11+ `RutaService` captura con `takeScreenshot()`. |
| `CapturaService.kt` | Respaldo (Android ≤ 10 o si falla la de accesibilidad): MediaProjection + ML Kit OCR cada 1 s (imagen reducida a 1600 px, filtro de cambios y reintento a 0.5 s si ve una oferta que no entiende); agrupa líneas por zona de app, ignora el texto que tapan los pop-ups, pone primero el monto más grande y normaliza `S151` → `$151`. |
| `Parser.kt` | `Oferta` (tarifa, km, min, recogidaKm, recogidaMin, multiplicador) y `nivel()`, `motivoFiltro()`, `gasolina()` (km totales ÷ km/l × $/l), `desgaste()` (km totales × $/km, `desgKm`, 0 por defecto), `neta()` (tarifa − gasolina − desgaste); `porKm` y `porHora` sobre km y min **totales**. |
| `Popup.kt` | Pop-up E (10.11): tarjeta oscura con borde del semáforo (218 dp al 100 %); arriba ACEPTAR/CONSIDERAR/RECHAZAR (★/2ª delante) y ⚡; monto neto de 46 sp al centro; `$/h \| $/km` (verde/rojo según la meta); abajo `24 min (6.0 km) total` (**«total» obligatorio**: el lector ignora esos textos) y, si falla recogida o duración, una línea roja. ✕ en la esquina. Tamaño 70–160 %. |
| `Pais.kt` | País (código ISO en `Prefs.pais`): define la moneda y el número del SOS. Se elige en la guía (paso «¿En qué país manejas?», con el del teléfono detectado) y en Cuenta › País; vacío = se deduce de la moneda. |
| `Desgaste.kt` | Desgaste por conceptos (por km: llantas, aceite, frenos, servicio, batería; al año: seguro, licencia, tenencia, verificación, datos, lavado, depreciación, mensualidad ÷ km al año). Suma = `Prefs.desgastePorKm`; sin conceptos se usa el monto directo. UI: Mis metas › Tu auto › Desgaste. |
| `Confirmador.kt` | Revisión en segundo plano (10.48, invisible para el conductor): manda las líneas con números a `supabase/functions/confirmar-oferta` (revisor según el secreto `REVISOR`: por ahora **DeepSeek `deepseek-flash`** con `DEEPSEEK_API_KEY`, por falta de saldo para Haiku; `anthropic` = Claude Haiku 5.5 con `ANTHROPIC_API_KEY`; precios en `precios_modelo`); si no coincide y pasa `Parser.plausible`, corrige el pop-up. **Rescate** (10.51): si el lector no entiende una oferta visible (tramo + monto), el revisor da los datos y sale el pop-up. Diferencias en `errores_lector` (revisión semanal los lunes), fallas en `fallas_ia`. **No publicar que se usa IA** (Eduardo, 11 nov). |
| `ZonasActivity.kt` / `Zonas.kt` | Zonas bloqueadas (10.60): mapa osmdroid donde el conductor dibuja zonas; `Parser.destino()` lee la dirección del destino, `Zonas.ubicar` la convierte en coordenadas (Nominatim, necesita internet) y `Parser.origen()` la del origen; `Zonas.evaluar` da un `Resultado` con estado y `RutaService.revisarZona` pone el pop-up en rojo con el motivo si alguno cae dentro (`Oferta.zonaOrigen`/`zonaDestino`, `Zonas.Turnos` evita aplicar resultados viejos). |
| `Comparador.kt` | Con 2 ofertas gana el mejor nivel; si empatan, el mejor puntaje según `prefs.criterio` (neta/hora por defecto, neta/km o neta total). |
| `Prefs.kt` | SharedPreferences `cfg`: filtros, gasolina (12 km/l y $24/l por defecto), pop-up, avisos, apps, diagnóstico y criterio. |
| `Historial.kt` / `Jornada.kt` | Ofertas de ~30 días (`historial.txt`) / «Mi día»: viajes aceptados, meta del día y gasolina de la semana. |
| `Alertas.kt` | Vibración por color y voz TTS es-MX («Aceptar / Considerar / Rechazar») o tonos. |
| `Tema.kt` | Marca v2: asfalto #0C0C10, tablero #16161C, hueso #F4F4EF; verde/amarillo/rojo solo para decidir; Bricolage 800, DM Sans, JetBrains Mono. |
| `Cuenta.kt` | Login con Google (Credential Manager → Supabase), licencia y ajustes en Supabase, «Entrar sin cuenta (beta)» hasta `BETA_HASTA`, «Eliminar cuenta» (solo `play`). |
| `Licencia.kt` | Licencia sin internet (código de equipo firmado ECDSA), combinada con la de cuenta y la beta. |
| `Migracion.kt` | Solo `directo`: aviso con cuenta regresiva y bloqueo el 15 nov 2026 para pasar a Play. |
| `ActualizacionPlay.kt` | Solo `play`: aviso de versión nueva con In-App Updates de Google Play al abrir la app. |
| `Actualizacion.kt` | Solo `directo`: busca versión nueva en `PWA-Checa-Multimedia/descargar/version.json`, avisa e instala. |
| `Widgets.kt` / `Manejo.kt` / `QrActivity.kt` | Widgets de inicio (mosaico 4×2, Mi día, Descanso, SOS, Mi QR → `QrActivity` con el QR al WhatsApp del conductor; el encender 1×1 sigue en `WidgetCheca.kt`) y tiempo manejando seguido para Descanso. |
| `Resumen.kt`, `Soporte.kt`, `Guardado.kt`, `MainActivity.kt` | Imagen del día para compartir, ayuda y soporte, pantallas guardadas para diagnóstico, y la UI (encabezado fijo con marca y botón SOS · Inicio · Mis metas · botón central · Ajustes · Cuenta). |

Pantallas, instalación en teléfonos (Play Protect, ajustes restringidos, Samsung/OPPO) y licencias en detalle: `docs/app-pantallas.md`.

### Reglas del lector (Parser), calibradas con capturas reales
- **DiDi**: `$151.34` grande, `5min (1.7km)` / `37min (13.2km)` / `2min (720m)`. Botones de contraoferta `$163.34`… que van **después** de la tarifa. `$9.00 adicionales`, `⚡1.4x`. El mapa de calor muestra `1.8X-2.4X`, que **se ignora** porque es un rango.
- **Uber**: `$48.73`, `$10.15/km est.`, `A 5 min (1.6 km)`, `Viaje: 8 min (3.2 km)`, `+$9.75 por inicio…`, `+$62.00 incluido` (dinámica ya sumada en la tarifa → `Oferta.dinamica`, se muestra como ⚡+$62 en el pop-up si no hay multiplicador).
- Tarifa = primer texto que sea *solo* un monto. Respaldo: primer monto que no sea extra, /km, /h ni "Aceptar".
- km y min = suma de los tramos `N min (X km|m)`. El primer tramo es la recogida. Se ignoran textos con "total" (el pop-up propio o el de otra app).
- Protección contra el OCR: "m" con decimales o < 30 se trata como **km**. Se descarta la oferta si $/km > 80, $/h > 3000 o km < 0.3.
- **No** se suman bonos ("adicionales", "+$… por inicio").
- **Pruebas del lector:** `app/src/test/resources/ofertas/*.txt` (líneas del OCR, `---` y lo esperado) y `ParserTest.kt`; corren en `play-compilar.yml`. Cada falla de lectura que mande Eduardo se agrega como caso nuevo. Antes de subir, simula con `python3 -I` si hace falta.
- Pop-up: el monto grande sí va solo (el OCR ignora lo que tapan los pop-ups, `rectsPopups()`); ningún texto del pop-up puede parecer un tramo `N min (X km)`, por eso su línea de abajo siempre lleva «total» (`24 min (6.0 km) total`, el lector ignora esos textos) y los avisos dicen «Recogida lejana · 2.5 km total».

### Semáforo y filtros (4 + tarifa mínima opcional)
- **Por hora** ($200 por defecto) y **Por km** ($10): 🟢 si cumple todos los activos, 🟡 si cumple alguno, 🔴 si ninguno.
- **Recogida máx.** (2/3/4 km) y **Duración máx.** del viaje sin recogida (15/20/30 min): si fallan, la oferta va a 🔴 con su motivo.
- **Tarifa mínima** (10.12, apagada por defecto, $50): si la ganancia neta (o la tarifa de la app, a elección) queda por debajo, 🔴 («Paga poco»). Está en Mis metas › Tu auto.

## Licencias y precios (vigente)
- Beta gratis para todos hasta el **31 ene 2027**. Después: **1 mes $119 · 6 meses $589 · 1 año $1,059** (MXN).
- **Plan Fundador −40 % de por vida** ($71 / $353 / $635) solo para los **primeros 50 correos de la tabla `testers`** por orden de registro, si entran con Google (`reclamar_fundador` consulta esa posición).
- Canal `directo`: cobro con Mercado Pago (`supabase/functions/mp-pago` y `mp-webhook`; planes en `_shared/planes.ts`). Canal `play`: Play Billing (Fase 7, pendiente).
- **Regalar una licencia:** `pedidos/pedido.txt` con `nombre=`, `correo=` (preferido) o `equipo=`, `tipo=Fundador|Normal` y `dias=` o `plan=`; push a `main` y `licencia.yml` responde en `pedidos/respuesta.txt` (~30 s). Eduardo autorizó este método. La llave privada solo está en los secretos de GitHub y en su Drive.
- Supabase: proyecto `gqsadclvftkzwrihfipz`; esquema en `supabase/esquema.sql`, que despliega `supabase.yml`.
- Detalle (paquetes, Mercado Pago, invitaciones, historia de precios): `docs/web-y-ventas.md`.

## Marca
- **Posicionamiento (nov 2026):** vendemos «tranquilidad económica a través del control», no funciones. Eslogan: «Tu trabajo vale. Asegúrate de que cada viaje también.» Planes: Básico $119 · **Estratega $589 (el que se impulsa)** · Pro $1,059. Mensajes, tono y regla «transformación, no funciones»: `marketing/plan/identidad-comercial.md`.
- Vive en el repo **`jeal1498/PWA-Checa-Multimedia`**, carpeta `marca/` (skill `marca-checa`); no guardar copias aquí. Asfalto `#0C0C10`, hueso `#F4F4EF`; el color solo decide (aceptar `#22C55E`, considerar `#FACC15`, rechazar `#EF4444`); Bricolage Grotesque 800, DM Sans, JetBrains Mono.
- La web es `https://jeal1498.github.io/PWA-Checa-Multimedia/` (repo `PWA-Checa-Multimedia`; cambios directo a `main`). Los legales los debe revisar un abogado.

## Índice de `docs/` (léelos solo si la tarea lo pide)
| Si vas a… | Lee |
|---|---|
| Trabajar en Google Play (fases, firmas, huellas SHA-1, Play Console, testers) | `docs/google-play/bitacora.md` y `docs/google-play/*.md` |
| Tocar pantallas de la app, instalación o licencias | `docs/app-pantallas.md` |
| Tocar precios, Mercado Pago, invitaciones, la web o el marketing | `docs/web-y-ventas.md` |
| Saber qué cambió en cada versión | `docs/historial-versiones.md` |
| Mensajes, tono y posicionamiento | `marketing/plan/identidad-comercial.md` |
| Ver la historia de los videos | `docs/videos-historia.md` y `marketing/plan/` |
| Atención en TikTok y WhatsApp | `marketing/atencion/asistente.md` y `prompt.md` |
| Videos: proyectos viejos, calendario publicado y skills quitadas | `docs/videos-calendario-historia.md`, `docs/skills-borradas.md` |
