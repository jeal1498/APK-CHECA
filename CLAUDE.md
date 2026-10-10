# CLAUDE.md: Luz Verde (antes Neta / Mi Ruta)

Contexto para continuar el proyecto en una conversación nueva.

## Qué es
**Luz Verde** (antes "Neta" y "Mi Ruta") es una app Android **personal** (sin fines comerciales) para conductores de **Uber y DiDi** en Cancún. Cuando llega una oferta de viaje, la app lee la pantalla y muestra un **pop-up** con **$/hora**, **$/km**, el semáforo (🟢 conviene / 🟡 regular / 🔴 no conviene), la ganancia neta después de gasolina y el multiplicador ⚡.

- Se escribió **desde cero** como alternativa propia a la app comercial "Ruta Rentable".
- **No** se modificó, descompiló ni copió el APK de Ruta Rentable, y esa regla se mantiene. Ruta Rentable solo sirve como referencia de *qué funciones* tener (formato del pop-up, "filtros inteligentes"). El diseño y el código son propios, con estilo verde y oscuro.
- Usuaria: Karen. Usa la app en su **teléfono** y en una **tablet en horizontal con pantalla dividida** (DiDi a la izquierda, Uber a la derecha).

## Cómo trabajar con la usuaria
- **Respuestas concisas, en español**, sin textos largos.
- Tras cada cambio: commit, push a `main`, esperar a que GitHub Actions compile y **pasarle el link directo del APK** (`https://github.com/jeal1498/Mi-Ruta/releases/download/vN/MiRuta.apk`).
- Ella prueba en su teléfono y manda **capturas**. Para bugs de lectura, pedirle que active **Diagnóstico** y comparta las "capturas de texto".
- Subir `versionCode` / `versionName` en `app/build.gradle.kts` en cada release.

## Compilación (no hay Android SDK en el entorno local)
- En el contenedor no hay acceso a Maven ni a Google, así que **no se puede compilar localmente**.
- `.github/workflows/build.yml` compila en cada push a `main`: Java 17, Gradle 8.9 (`gradle/actions/setup-gradle`, sin wrapper) y `gradle assembleRelease`. Publica `MiRuta.apk` en un **Release `v<run_number>`**.
- Revisar el estado con `https://api.github.com/repos/jeal1498/Mi-Ruta/actions/runs?per_page=1`.
- Firma: `app/miruta.jks` (pass `miruta123`, alias `miruta`). Está en el repo para que las actualizaciones se instalen encima de la versión anterior. ⚠️ **El repo es público**: se le recomendó hacerlo privado.
- Stack: AGP 8.5.2, Kotlin 1.9.24, compileSdk/targetSdk 34, minSdk 26. **UI 100% programática** (sin XML de layouts, sin AppCompat ni Compose). Única dependencia: `com.google.mlkit:text-recognition:16.0.1` (OCR en el teléfono, sin internet).
- Como no hay compilador local, hay que **revisar el Kotlin con cuidado** antes de hacer push. Las regex se pueden probar con Python (`python3 -I`).

## Arquitectura (`app/src/main/java/com/karen/miruta/`)
| Archivo | Qué hace |
|---|---|
| `RutaService.kt` | **AccessibilityService**. (1) Muestra los pop-ups como `TYPE_ACCESSIBILITY_OVERLAY`, uno por app. (2) `zonasApps()` devuelve el `Rect` de la ventana de Uber/DiDi (pantalla dividida). (3) Lee texto por accesibilidad, analizando cada ventana por separado. (4) `mostrarOferta(oferta, app, zona)`: evita duplicados por app en 60 s, aplica el `Comparador`, lanza las alertas y guarda el historial. `instancia` estática. |
| `CapturaService.kt` | Foreground service con **MediaProjection** y **ML Kit OCR** cada 1 s. Es el modo principal, porque Uber/DiDi no exponen bien su texto. Agrupa las líneas OCR **por zona de app** (usando `zonasApps()`) para no mezclar ofertas. Si no hay zonas, por ejemplo al probar con una captura en la galería, analiza toda la pantalla. Pone primero el monto con letra más grande (la tarifa). Normaliza `S151` → `$151`. Ajusta la captura si la pantalla rota. |
| `Parser.kt` | `Oferta` (tarifa, km, min, recogidaKm, recogidaMin, multiplicador). Funciones: `nivel()`, `motivoFiltro()`, `gasolina()`, `neta()`. `Parser.analizar(lineas)`. |
| `Popup.kt` | Pop-up "B2 compacto" (v14): franja del color del semáforo con la acción centrada (ACEPTAR / CONSIDERAR / RECHAZAR, sin símbolos), app/⭐ a la izquierda y ⚡ + ✕ a la derecha. Neta grande al centro (`$x netos`) y abajo `Viaje $x · Gasolina −$y`. Fila de 3 fichas con texto centrado: $/h y $/km (verde si cumple la meta, rojo con borde si no, blanco si el filtro está apagado) y tiempo/km total; si falla recogida o duración, la 3ª ficha se vuelve roja con ese dato. Los números de las fichas se achican si no caben. `construir()` también genera la vista previa en Ajustes. Se posiciona sobre la zona de su app. |
| `Comparador.kt` | Con 2 ofertas visibles, gana la de mejor nivel y, si empatan, la de mejor puntaje según `prefs.criterio`: 0 neta/hora (default), 1 neta/km, 2 neta total. |
| `Prefs.kt` | SharedPreferences `cfg`: filtros, gasolina, pop-up, avisos, apps, diagnóstico y criterio. |
| `Historial.kt` | `filesDir/historial.txt`, una línea `hora|app|tarifa|km|min|nivel|recKm|recMin|multi`. Se conservan ~30 días. `deHoy()` y `ultimosDias()` filtran lecturas imposibles. |
| `Alertas.kt` | Vibración (1 corta 🟢, 2 cortas 🟡, 1 larga 🔴). Sonido: voz con `TextToSpeech` (es-MX) que dice «Aceptar» / «Considerar» / «Rechazar» (`prefs.voz`, ON por defecto) o tonos con `ToneGenerator`. Un solo motor TTS compartido (companion). |
| `Tema.kt` | Paleta (fondo #0E1110, tarjeta #181C1A, verde #22C55E), medidas en múltiplos de 8 dp (márgenes 16, entre tarjetas 12, radios 16/12, toque mín. 48), letra 22/16/14/12 sp (+28 números) y `Icono`: íconos lineales dibujados con Canvas (sin emojis en la UI). |
| `MainActivity.kt` | UI con tema oscuro. Barra inferior: 🏠 Inicio · **botón circular central** (▶ verde iniciar / ■ rojo detener) · ⚙️ Ajustes. |

### Reglas del lector (Parser), calibradas con capturas reales
- **DiDi**: `$151.34` grande, `5min (1.7km)` / `37min (13.2km)` / `2min (720m)`. Botones de contraoferta `$163.34`… que van **después** de la tarifa. `$9.00 adicionales`, `⚡1.4x`. El mapa de calor muestra `1.8X-2.4X`, que **se ignora** porque es un rango.
- **Uber**: `$48.73`, `$10.15/km est.`, `A 5 min (1.6 km)`, `Viaje: 8 min (3.2 km)`, `+$9.75 por inicio…`.
- Tarifa = primer texto que sea *solo* un monto. Respaldo: primer monto que no sea extra, /km, /h ni "Aceptar".
- km y min = suma de los tramos `N min (X km|m)`. El primer tramo es la recogida. Se ignoran textos con "total" (el pop-up propio o el de otra app).
- Protección contra el OCR: "m" con decimales o < 30 se trata como **km** (el lector confundía km con m y daba $/km absurdos). Se descarta la oferta si $/km > 80, $/h > 3000 o km < 0.3.
- **No** se suman bonos ("adicionales", "+$… por inicio"). Ruta Rentable tampoco los suma.

### Semáforo y filtros inteligentes (solo 4, como pidió la usuaria)
- ⏱️ **Por hora** (campo de texto, ON por defecto, $150) y 📈 **Por km** (campo de texto, ON, $8). 🟢 si cumple todos los activos, 🟡 si cumple alguno, 🔴 si no cumple ninguno.
- 📍 **Recogida máx.** (opciones 2/3/4/5 km) y ⌛ **Duración máx.** del viaje sin recogida (15/20/30 min). Si fallan, la oferta va a 🔴 con su motivo. Se toca la opción para activarla y otra vez para apagarla.
- Ella pidió **quitar** recogida mín. y duración mín.

## Pantallas
- **Inicio**: aviso de permiso (solo si falta), tarjetas 🟢/🟡/🔴 del día que se pueden tocar para ver el detalle de cada grupo, **Mejores horas** (top 3 por $/km de los últimos 30 días, con desglose por hora y por día) y **Filtros inteligentes** (tarjetas desplegables con ON/OFF).
- **Ajustes**: Pop-up (vista previa en vivo, tamaño, tiempo, posición), Gasolina (mostrar neta, km/l = 12 y $/l = 24 por defecto; su auto es un Cavalier 2020), Dos ofertas a la vez (criterio), Avisos, Apps (Uber/DiDi), Cómo usarla, Permisos, Diagnóstico y versión.
- Ella pidió **quitar**: los KPI (ofertas, promedios, mejor), el botón "Ver ofertas de hoy", la tarjeta de estado de la lectura y los botones A−/A+ del pop-up.

## Instalación en el teléfono (problemas que ya salieron)
- Play Protect bloquea la instalación porque la app usa Accesibilidad. Se resuelve apagando "Analizar apps" un momento o con "Instalar de todos modos".
- Android 13+: "Controlado por ajustes restringidos". Se resuelve en Info de la app → ⋮ → **Permitir configuración restringida** y luego activando "Luz Verde – lector de ofertas" en Accesibilidad.
- Lectura de pantalla: elegir **Pantalla completa** → Empezar. Después de reiniciar el teléfono hay que volver a iniciarla.

## Ideas propuestas y pendientes
- **No** integrar el bloqueo de zonas por palabras de la dirección: se explicó y ella decidió no hacerlo.
- Aún no se han hecho: arranque automático al abrir Uber/DiDi, meta del día, pop-up arrastrable, respaldo o exportación del historial.
- Falta probar en la calle la versión v11, con pantalla dividida y ⭐.

## Historial de versiones
v1 base (accesibilidad) → v2 OCR → v3 vibración/sonido + historial → v4 rediseño oscuro → v5–v6 pop-up nuevo → v7 botón circular + semáforo con detalle → v8–v9 filtros inteligentes (4) → v10 neta, mejores horas, multiplicador → v11 pantalla dividida, pop-up por app, ⭐ mejor opción (2.0) → **v12 rediseño "Neta"**: sistema de medidas, íconos propios, pop-up con franja de semáforo y neta grande, detalles en hojas propias, animaciones (versionName 3.0) → **v13 nombre "Luz Verde"** (ícono de semáforo) y aviso por voz (3.1) → **v14 pop-up B2 compacto** (3.2) → v15 fichas centradas (3.3) → v16 pantalla dividida sin zonas: separa por mitades (DiDi izq., Uber der.) y usa el título de la ventana (3.4) → v17 OCR "ll min" → "11 min" (3.5).
- El paquete sigue siendo `com.karen.miruta` y el APK `MiRuta.apk` para que se actualice encima.
- Pop-up: ningún texto puede ser *solo un monto* (el OCR lo tomaría como tarifa); por eso "Te quedan $x", "$x/h", "$x/km" y "… km total".
