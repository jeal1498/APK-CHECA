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
- Él prueba en su teléfono y manda capturas. Para fallas de lectura, pídele que active «Guardar pantallas de ofertas» y comparta lo guardado.
- `rm` con comodín está bloqueado en el entorno: usa `mv`. Tareas de más de 2 min, en segundo plano.

## Videos (TikTok, Reels, Shorts)
- **Frase de arranque: «Lote de videos: …»** (con temas, o «propón los temas»). Al leerla: `git pull origin main` en los dos repos, lee `marketing/plan/plan-ponerse-al-dia.md`, invoca la skill `guion-checa` con la herramienta Skill y arranca el flujo de dos aprobaciones (abajo). Si no trae temas, propón 3–4 con lo que más retiene y espera su elección.
- **Guiones:** siempre con la skill **`guion-checa`**. **Producción** (voz, escenas, portada, copy, calendarios y control de calidad): skill **`video-checa`** con la herramienta `video/scripts/checa.py` y el motor `video/hyperframes/_base/`.
- **Flujo: dos aprobaciones por lote.** (1) Eduardo aprueba juntos los guiones del lote (cada tema con CHECA y su v2 PERSONAL); (2) Claude produce todo sin preguntar, pasa el control de calidad y le manda el lote completo para una sola revisión; se corrige solo lo que él marque. «Publicado» lo marca él.
- **CTA vigente:** «Comenta CHECA y te paso el acceso a la prueba en Google Play». Cuentas: `@checa.driver` (Checa) y `@Eduardoalv9802` (personal).
- **Estado:** el real está en el calendario web (`PWA-Checa-Multimedia/calendario-d4f81c/data.json`); `marketing/plan/calendario.md` lo acompaña y `marketing/plan/plan-ponerse-al-dia.md` tiene el plan. Al empezar: `git pull origin main` en los dos repos.
- **Se sube video todos los días, domingos incluidos** (Eduardo, 31 oct; antes los domingos no se subía).
- El `video.mp4` se commitea **una sola vez**, ya aprobado (cada mp4 commiteado engorda el repo para siempre).

## Entorno y compilación
- No hay Android SDK, Gradle ni Kotlin en el contenedor: **no se puede compilar localmente**. Revisa el Kotlin con cuidado antes de subir; las regex se prueban con `python3 -I`. Hay git, Python 3.12, Node 22, Java 21 (sin `javac`), ffmpeg y Playwright (`NODE_PATH=$(npm root -g)`).
- La red solo permite GitHub, PyPI y npm. No llega a `productionresultssa1.blob.core.windows.net` (artefactos y logs de Actions); el estado de los workflows sí se lee con las herramientas de GitHub MCP (el repo es privado).
- **Dos canales (flavors):**
  - `directo` (`com.karen.miruta`, `MiRuta.apk` en la web): `build.yml` compila en cada push a `main` y publica el release `vN` (`https://github.com/jeal1498/Mi-Ruta/releases/download/vN/MiRuta.apk`). **Ignora** `app/**`, `marketing/**`, `video/**`, `docs/**`, `.claude/**`, `pedidos/**`, `supabase/**` y `CLAUDE.md`, así que un cambio solo de la app no genera release. Firma local por variables `MIRUTA_*`; la llave nunca va a Git. Se **cierra el 15 nov 2026** (ver «Estado actual»).
  - `play` (`com.jeal1498.checa`, Google Play): `play-compilar.yml` (push a `app/**`) compila y verifica; `play-aab-firmado.yml` (manual) genera el AAB firmado con los secretos `PLAY_UPLOAD_*`. Sin auto-actualización, sin Mercado Pago, sin `REQUEST_INSTALL_PACKAGES` ni excepción de batería. `BuildConfig.ES_PLAY` separa el comportamiento.
- Stack: AGP 8.10.1, Kotlin 1.9.24, compileSdk/targetSdk 36, minSdk 26. **UI 100 % programática** (sin XML de layouts, sin AppCompat ni Compose). Dependencias: ML Kit `text-recognition:16.0.1` (OCR en el teléfono) y `androidx.credentials` + `googleid` (login con Google). INTERNET solo para cuenta y licencia; la lectura de ofertas no usa internet.
- `versionCode` / `versionName` en `app/build.gradle.kts` (compartidos por los dos flavors): súbelos en cada versión y nunca bajes de 67. Historial en `docs/historial-versiones.md`.

## Estado actual (30 oct 2026)
- **Versión:** `main` trae `versionCode` 76 / **9.6**. Último release del canal directo: **v80 (9.5)** (pendiente el de la 9.6). Play: AAB 74 (9.4) en Prueba interna y **Prueba cerrada «Alpha»** (México), ya disponible para testers.
- **Migración al canal Play (30 oct):** la 9.5 ya tiene release directo (v80) y la web manda a `/registro/`. La web cambió de nombre a `PWA-Checa-Multimedia`: la 9.6 corrige las direcciones dentro de la app y **necesita un release directo nuevo** (Eduardo lo lanza con «Run workflow» en `build.yml`; yo no tengo ese permiso).
- **Google Play:** falta juntar **12 testers dentro 14 días seguidos** para pedir producción. `/registro/` (PWA-Checa-Multimedia) guarda nombre y Gmail en la tabla `testers` de Supabase y manda a la comunidad de WhatsApp; Eduardo pasa los correos a mano a la lista «Testers» de Play Console. El registro cierra al llegar a 50 o el 15 nov 2026.
- **Fase 7 (Play Billing)** pendiente: debe estar lista antes del **1 feb 2027**, cuando termina la beta gratis (`Licencia.BETA_HASTA` = 31 ene 2027). Necesita su «adelante».
- Detalle e historia: `docs/google-play/bitacora.md`.

## Arquitectura (`app/src/main/java/com/karen/miruta/`)
| Archivo | Qué hace |
|---|---|
| `RutaService.kt` | AccessibilityService: pop-ups como `TYPE_ACCESSIBILITY_OVERLAY` (uno por app), `zonasApps()` (ventana de Uber/DiDi en pantalla dividida), lectura por accesibilidad y `mostrarOferta()` (sin duplicados en 60 s, `Comparador`, alertas, historial). |
| `CapturaService.kt` | Modo principal: MediaProjection + ML Kit OCR cada 1 s; agrupa líneas por zona de app, ignora el texto que tapan los pop-ups, pone primero el monto más grande y normaliza `S151` → `$151`. |
| `Parser.kt` | `Oferta` (tarifa, km, min, recogidaKm, recogidaMin, multiplicador) y `nivel()`, `motivoFiltro()`, `gasolina()` (km totales ÷ km/l × $/l), `neta()` (tarifa − gasolina); `porKm` y `porHora` sobre km y min **totales**. |
| `Popup.kt` | Pop-up «A · Franja» (marca v2): franja ACEPTAR/CONSIDERAR/RECHAZAR, «$x netos», `Viaje $x · Gasolina −$y`, fichas $/h, $/km y tiempo/km total; ★ y borde hueso en la mejor de dos. |
| `Comparador.kt` | Con 2 ofertas gana el mejor nivel; si empatan, el mejor puntaje según `prefs.criterio` (neta/hora por defecto, neta/km o neta total). |
| `Prefs.kt` | SharedPreferences `cfg`: filtros, gasolina (12 km/l y $24/l por defecto), pop-up, avisos, apps, diagnóstico y criterio. |
| `Historial.kt` / `Jornada.kt` | Ofertas de ~30 días (`historial.txt`) / «Mi día»: viajes aceptados, meta del día y gasolina de la semana. |
| `Alertas.kt` | Vibración por color y voz TTS es-MX («Aceptar / Considerar / Rechazar») o tonos. |
| `Tema.kt` | Marca v2: asfalto #0C0C10, tablero #16161C, hueso #F4F4EF; verde/amarillo/rojo solo para decidir; Bricolage 800, DM Sans, JetBrains Mono. |
| `Cuenta.kt` | Login con Google (Credential Manager → Supabase), licencia y ajustes en Supabase, «Entrar sin cuenta (beta)» hasta `BETA_HASTA`, «Eliminar cuenta» (solo `play`). |
| `Licencia.kt` | Licencia sin internet (código de equipo firmado ECDSA), combinada con la de cuenta y la beta. |
| `Migracion.kt` | Solo `directo`: aviso con cuenta regresiva y bloqueo el 15 nov 2026 para pasar a Play. |
| `Actualizacion.kt` | Solo `directo`: busca versión nueva en `PWA-Checa-Multimedia/descargar/version.json`, avisa e instala. |
| `Resumen.kt`, `Soporte.kt`, `Guardado.kt`, `MainActivity.kt` | Imagen del día para compartir, ayuda y soporte, pantallas guardadas para diagnóstico, y la UI (Inicio · Mis metas · botón central · Ajustes · Cuenta). |

Pantallas, instalación en teléfonos (Play Protect, ajustes restringidos, Samsung/OPPO) y licencias en detalle: `docs/app-pantallas.md`.

### Reglas del lector (Parser), calibradas con capturas reales
- **DiDi**: `$151.34` grande, `5min (1.7km)` / `37min (13.2km)` / `2min (720m)`. Botones de contraoferta `$163.34`… que van **después** de la tarifa. `$9.00 adicionales`, `⚡1.4x`. El mapa de calor muestra `1.8X-2.4X`, que **se ignora** porque es un rango.
- **Uber**: `$48.73`, `$10.15/km est.`, `A 5 min (1.6 km)`, `Viaje: 8 min (3.2 km)`, `+$9.75 por inicio…`, `+$62.00 incluido` (dinámica ya sumada en la tarifa → `Oferta.dinamica`, se muestra como ⚡+$62 en el pop-up si no hay multiplicador).
- Tarifa = primer texto que sea *solo* un monto. Respaldo: primer monto que no sea extra, /km, /h ni "Aceptar".
- km y min = suma de los tramos `N min (X km|m)`. El primer tramo es la recogida. Se ignoran textos con "total" (el pop-up propio o el de otra app).
- Protección contra el OCR: "m" con decimales o < 30 se trata como **km**. Se descarta la oferta si $/km > 80, $/h > 3000 o km < 0.3.
- **No** se suman bonos ("adicionales", "+$… por inicio").
- Pop-up: ningún texto puede ser *solo un monto* (el OCR lo tomaría como tarifa); por eso "Te quedan $x", "$x/h", "$x/km" y "… km total".

### Semáforo y filtros (solo 4)
- **Por hora** ($150 por defecto) y **Por km** ($8): 🟢 si cumple todos los activos, 🟡 si cumple alguno, 🔴 si ninguno.
- **Recogida máx.** (2/3/4 km) y **Duración máx.** del viaje sin recogida (15/20/30 min): si fallan, la oferta va a 🔴 con su motivo.

## Licencias y precios (vigente)
- Beta gratis para todos hasta el **31 ene 2027**. Después: **1 mes $119 · 6 meses $589 · 1 año $1,059** (MXN).
- **Plan Fundador −40 % de por vida** ($71 / $353 / $635) solo para los **primeros 50 correos de la tabla `testers`** por orden de registro, si entran con Google (`reclamar_fundador` consulta esa posición).
- Canal `directo`: cobro con Mercado Pago (`supabase/functions/mp-pago` y `mp-webhook`; planes en `_shared/planes.ts`). Canal `play`: Play Billing (Fase 7, pendiente).
- **Regalar una licencia:** `pedidos/pedido.txt` con `nombre=`, `correo=` (preferido) o `equipo=`, `tipo=Fundador|Normal` y `dias=` o `plan=`; push a `main` y `licencia.yml` responde en `pedidos/respuesta.txt` (~30 s). Eduardo autorizó este método. La llave privada solo está en los secretos de GitHub y en su Drive.
- Supabase: proyecto `gqsadclvftkzwrihfipz`; esquema en `supabase/esquema.sql`, que despliega `supabase.yml`.
- Detalle (paquetes, Mercado Pago, invitaciones, historia de precios): `docs/web-y-ventas.md`.

## Marca
- Vive en el repo **`jeal1498/PWA-Checa-Multimedia`**, carpeta `marca/` (skill `marca-checa`); no guardar copias aquí. Asfalto `#0C0C10`, hueso `#F4F4EF`; el color solo decide (aceptar `#22C55E`, considerar `#FACC15`, rechazar `#EF4444`); Bricolage Grotesque 800, DM Sans, JetBrains Mono.
- La web es `https://jeal1498.github.io/PWA-Checa-Multimedia/` (repo `PWA-Checa-Multimedia`; cambios directo a `main`). Los legales los debe revisar un abogado.

## Índice de `docs/` (léelos solo si la tarea lo pide)
| Si vas a… | Lee |
|---|---|
| Trabajar en Google Play (fases, firmas, huellas SHA-1, Play Console, testers) | `docs/google-play/bitacora.md` y `docs/google-play/*.md` |
| Tocar pantallas de la app, instalación o licencias | `docs/app-pantallas.md` |
| Tocar precios, Mercado Pago, invitaciones, la web o el marketing | `docs/web-y-ventas.md` |
| Saber qué cambió en cada versión | `docs/historial-versiones.md` |
| Ver la historia de los videos | `docs/videos-historia.md` y `marketing/plan/` |
| Atención en TikTok y WhatsApp | `marketing/atencion/asistente.md` y `prompt.md` |
| Videos: proyectos viejos, calendario publicado y skills quitadas | `docs/videos-calendario-historia.md`, `docs/skills-borradas.md` |
