# CLAUDE.md: Checa (antes Luz Verde / Neta / Mi Ruta)

Contexto para continuar el proyecto en una conversación nueva.

## Qué es
**Checa** (antes "Luz Verde", "Neta" y "Mi Ruta") es una app Android para conductores de **Uber y DiDi** en Cancún. Cuando llega una oferta de viaje, la app lee la pantalla y muestra un **pop-up** con **$/hora**, **$/km**, el semáforo (🟢 conviene / 🟡 regular / 🔴 no conviene), la ganancia neta después de gasolina y el multiplicador ⚡.

- Se escribió **desde cero** como alternativa propia a la app comercial "Ruta Rentable".
- **No** se modificó, descompiló ni copió el APK de Ruta Rentable, y esa regla se mantiene. Ruta Rentable solo sirve como referencia de *qué funciones* tener (formato del pop-up, "filtros inteligentes"). El diseño y el código son propios, con estilo verde y oscuro.
- Desde v21 se **vende** a otros conductores con licencia por equipo (ver *Licencias y venta*).
- Usuaria: Karen. Usa la app en su **teléfono** y en una **tablet en horizontal con pantalla dividida** (DiDi a la izquierda, Uber a la derecha).

## Cómo trabajar con la usuaria
- **Respuestas concisas, en español**, sin textos largos.
- Tras cada cambio: commit, push a `main`, esperar a que GitHub Actions compile y **pasarle el link directo del APK** (`https://github.com/jeal1498/Mi-Ruta/releases/download/vN/MiRuta.apk`).
- Ella prueba en su teléfono y manda **capturas**. Para bugs de lectura, pedirle que active **Diagnóstico** y comparta las "capturas de texto".
- Subir `versionCode` / `versionName` en `app/build.gradle.kts` en cada release.

## Compilación (no hay Android SDK en el entorno local)
- En el contenedor no hay acceso a Maven ni a Google, así que **no se puede compilar localmente**.
- `.github/workflows/build.yml` compila en cada push a `main`: Java 17, Gradle 8.9 (`gradle/actions/setup-gradle`, sin wrapper) y `gradle assembleRelease`. Publica `MiRuta.apk` en un **Release `v<run_number>`**.
- Revisar el estado con `mcp__github__actions_list` (`list_workflow_runs`, `build.yml`) o esperando el tag `vN` con `git ls-remote origin refs/tags/vN`. La API pública ya no sirve (repo privado).
- Firma: `app/miruta.jks` (pass `miruta123`, alias `miruta`). Está en el repo para que las actualizaciones se instalen encima de la versión anterior. El repo ya es privado.
- Stack: AGP 8.5.2, Kotlin 1.9.24, compileSdk/targetSdk 34, minSdk 26. **UI 100% programática** (sin XML de layouts, sin AppCompat ni Compose). Única dependencia: `com.google.mlkit:text-recognition:16.0.1` (OCR en el teléfono, sin internet).
- Como no hay compilador local, hay que **revisar el Kotlin con cuidado** antes de hacer push. Las regex se pueden probar con Python (`python3 -I`).

## Arquitectura (`app/src/main/java/com/karen/miruta/`)
| Archivo | Qué hace |
|---|---|
| `RutaService.kt` | **AccessibilityService**. (1) Muestra los pop-ups como `TYPE_ACCESSIBILITY_OVERLAY`, uno por app. (2) `zonasApps()` devuelve el `Rect` de la ventana de Uber/DiDi (pantalla dividida). (3) Lee texto por accesibilidad, analizando cada ventana por separado. (4) `mostrarOferta(oferta, app, zona)`: evita duplicados por app en 60 s, aplica el `Comparador`, lanza las alertas y guarda el historial. `instancia` estática. |
| `CapturaService.kt` | Foreground service con **MediaProjection** y **ML Kit OCR** cada 1 s. Es el modo principal, porque Uber/DiDi no exponen bien su texto. Agrupa las líneas OCR **por zona de app** (usando `zonasApps()`) para no mezclar ofertas. Si no hay zonas: en horizontal separa por mitades (y lo recuerda 10 min); si no, analiza toda la pantalla. Ignora el texto que tapan los pop-ups. Pone primero el monto con letra más grande (la tarifa). Normaliza `S151` → `$151`. Ajusta la captura si la pantalla rota. |
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
- **Uber**: `$48.73`, `$10.15/km est.`, `A 5 min (1.6 km)`, `Viaje: 8 min (3.2 km)`, `+$9.75 por inicio…`, `+$62.00 incluido` (dinámica ya sumada en la tarifa → `Oferta.dinamica`, se muestra como ⚡+$62 en el pop-up si no hay multiplicador).
- Tarifa = primer texto que sea *solo* un monto. Respaldo: primer monto que no sea extra, /km, /h ni "Aceptar".
- km y min = suma de los tramos `N min (X km|m)`. El primer tramo es la recogida. Se ignoran textos con "total" (el pop-up propio o el de otra app).
- Protección contra el OCR: "m" con decimales o < 30 se trata como **km** (el lector confundía km con m y daba $/km absurdos). Se descarta la oferta si $/km > 80, $/h > 3000 o km < 0.3.
- **No** se suman bonos ("adicionales", "+$… por inicio"). Ruta Rentable tampoco los suma.

### Semáforo y filtros inteligentes (solo 4, como pidió la usuaria)
- ⏱️ **Por hora** (campo de texto, ON por defecto, $150) y 📈 **Por km** (campo de texto, ON, $8). 🟢 si cumple todos los activos, 🟡 si cumple alguno, 🔴 si no cumple ninguno.
- 📍 **Recogida máx.** (opciones 2/3/4/5 km) y ⌛ **Duración máx.** del viaje sin recogida (15/20/30 min). Si fallan, la oferta va a 🔴 con su motivo. Se toca la opción para activarla y otra vez para apagarla.
- Ella pidió **quitar** recogida mín. y duración mín.

## Pantallas
- **Inicio** (sin encabezado desde v25): aviso de permiso (solo si falta), tarjetas 🟢/🟡/🔴 del día que se pueden tocar para ver el detalle de cada grupo, **Mejores horas** (solo hoy: cajas «Ahora» y «Lo mejor que viene», 24 cuadros de 1 h deslizables que empiezan en la hora actual, color semáforo según la meta por km: verde ≥ meta, amarillo ≥ 85 %, rojo abajo, gris < 3 ofertas; «Ver toda la semana ›» abre ventana con mejor/peor día, 3 mejores momentos, tabla 7×24 y detalle al tocar: $/km, ofertas, % verdes, Uber vs DiDi). Se diseñó primero como maqueta en un artifact y **Filtros inteligentes** (sin desplegar: cada fila con su monto o fichas y ON/OFF).
- **Ajustes**: Pop-up (vista previa en vivo, tamaño, tiempo, posición), Gasolina (mostrar neta, km/l = 12 y $/l = 24 por defecto; su auto es un Cavalier 2020), Dos ofertas a la vez (criterio), Avisos, Apps (Uber/DiDi), Cómo usarla, Permisos, Diagnóstico y versión.
- Ella pidió **quitar**: los KPI (ofertas, promedios, mejor), el botón "Ver ofertas de hoy", la tarjeta de estado de la lectura y los botones A−/A+ del pop-up.

## Instalación en el teléfono (problemas que ya salieron)
- Play Protect bloquea la instalación porque la app usa Accesibilidad. Se resuelve apagando "Analizar apps" un momento o con "Instalar de todos modos".
- Android 13+: "Controlado por ajustes restringidos". Se resuelve en Info de la app → ⋮ → **Permitir configuración restringida** y luego activando "Checa – lector de ofertas" en Accesibilidad.
- Apps de banco: antes de v29 Nu bloqueaba a Checa por usar Accesibilidad. Desde la 4.8 (lector limitado a Uber/DiDi) Nu funciona. Si otro banco bloquea: Ajustes → Apps de banco → Pausar lector. **No desinstalar**: se pierde la activación (el código de equipo no cambia, pero hay que generar otra activación).
- Lectura de pantalla: elegir **Pantalla completa** → Empezar. Después de reiniciar el teléfono hay que volver a iniciarla.

## Licencias y venta (v21+)
- `Licencia.kt`: sin internet. Código de equipo = SHA-256 de ANDROID_ID (8 hex, `A1B2-C3D4`). Activación `AAAAMMDD[F]-firma` (ECDSA P-256 de `LV|EQUIPO|AAAAMMDD[|F]`). `99991231` = sin vencimiento, `F` = ⭐ Miembro fundador. 7 días de prueba. Sin licencia, `RutaService.mostrarOferta` no muestra nada.
- La llave privada solo está en el secreto `LICENCIA_PRIVADA` (no se puede leer). La pública está en `Licencia.kt`.
- **Para generar un código cuando ella lo pida** (nombre, WhatsApp, equipo, Fundador/Normal, días; 0 = sin vencimiento). Ella **autorizó** este método. La app de Claude no puede disparar Actions (403), así que se hace con un push:
  1. Crear `pedidos/pedido.txt` con líneas `nombre=…`, `whatsapp=…`, `equipo=A1B2-C3D4`, `tipo=Fundador|Normal`, `dias=30`. Commit (sin `[skip ci]`) y `git push origin HEAD:main`.
  2. El workflow `licencia.yml` firma, anota en `clientes.md`, borra el pedido y escribe `pedidos/respuesta.txt` (`codigo=…`). Tarda ~30 s.
  3. `git pull origin main` hasta que la respuesta sea la nueva y pasarle el `codigo` completo.
  - Ese push no compila APK (`build.yml` ignora `pedidos/**`, `clientes.md` y `CLAUDE.md`).
- Insignia dorada **⭐ Miembro fundador** en Inicio y Ajustes si la licencia trae `F`. Karen tiene licencia de fundadora sin vencimiento en su celular (05E7-8598) y su tablet (4817-F84C). **No** usar esos equipos para pruebas.
- Probado de punta a punta desde una conversación nueva (pedido → código → activado en la tablet).
- Precios: Ruta Rentable cobra $129/mes. Propuesta: $99/mes o $899/año; fundadores (primeros 50) $49/mes de por vida.
- El repo ya es **privado** (la API pública da 404; usar las herramientas de GitHub MCP para revisar Actions).

## Ideas propuestas y pendientes
- **EN CURSO: licencia ligada a cuenta Google con Supabase** (ella eligió Supabase porque ya lo conoce). Plan: login con Google (Credential Manager → ID token → `POST /auth/v1/token?grant_type=id_token` de Supabase, sin SDK); tabla `licencias` (user_id/email, vence, fundador, hasta **2 equipos** por cuenta) con RLS de solo lectura para el usuario; la app guarda la activación firmada (ECDSA, igual que hoy) y **sigue funcionando sin internet** después de activar. Ella debe: crear proyecto Supabase (pasar URL + anon key), en Google Cloud crear cliente OAuth **Android** (`com.karen.miruta`, SHA-1 `9D:2C:6B:15:1E:37:DC:88:84:35:CF:9A:6C:D1:5C:7D:0D:7F:C3:FE`) y cliente **Web** (pasar su ID), y activar Google en Supabase → Authentication → Providers.
- **No** integrar el bloqueo de zonas por palabras de la dirección: se explicó y ella decidió no hacerlo.
- Aún no se han hecho: arranque automático al abrir Uber/DiDi, meta del día, pop-up arrastrable, respaldo o exportación del historial.
- Pantalla dividida en tablet probada y correcta en v20 (dos pop-ups sólidos, ⭐ con borde dorado, sin mezclar ofertas). Si vuelve el parpadeo, pedir Diagnóstico.

## Historial de versiones
v1 base (accesibilidad) → v2 OCR → v3 vibración/sonido + historial → v4 rediseño oscuro → v5–v6 pop-up nuevo → v7 botón circular + semáforo con detalle → v8–v9 filtros inteligentes (4) → v10 neta, mejores horas, multiplicador → v11 pantalla dividida, pop-up por app, ⭐ mejor opción (2.0) → **v12 rediseño "Neta"**: sistema de medidas, íconos propios, pop-up con franja de semáforo y neta grande, detalles en hojas propias, animaciones (versionName 3.0) → **v13 nombre "Luz Verde"** (ícono de semáforo) y aviso por voz (3.1) → **v14 pop-up B2 compacto** (3.2) → v15 fichas centradas (3.3) → v16 pantalla dividida sin zonas: separa por mitades (DiDi izq., Uber der.) y usa el título de la ventana (3.4) → v17 OCR "ll min" → "11 min" (3.5) → v18 sin parpadeo: el pop-up no se rehace si no cambia, el OCR ignora lo que tapa el pop-up y no se reemplaza la oferta si los tramos son iguales (3.6) → v19 recuerda la pantalla dividida 10 min para no volver a leer la pantalla entera cuando un pop-up tapa una mitad (3.7) → v20 pop-ups sólidos: la mejor con borde dorado grueso, la 2ª con franja apagada (3.8) → **v21 licencia** por equipo, 7 días de prueba, generador en GitHub (4.0) → v22 insignia ⭐ Miembro fundador (4.1) → v23 ofuscación R8 (`isMinifyEnabled`, `app/proguard-rules.pro`) para dificultar quitar la licencia (4.2), probada en su teléfono sin fallas → **v24 nombre "Checa"** (ella lo eligió, fácil de decir; el ícono de semáforo se queda) (4.3) → v25 sin encabezado en Inicio (empieza en "HOY"; la insignia ⭐ solo queda en Ajustes) (4.4) → v26 filtros sin desplegar (monto editable en la misma fila, fichas de recogida/duración siempre visibles), meta por hora = $/km × 20 al editar el km (sigue editable) e Inicio más compacto (4.5) → v27 filtro Por km primero y Mejores horas como gráfica de barras de $/km por hora (4.6) → v28 Mejores horas como semáforo por hora de hoy + ventana de la semana (sin `Grafica.kt`) (4.7) → v29 apps de banco: Nu bloqueó a Checa por usar Accesibilidad fuera de Play Store. `RutaService.soloUberYDidi()` limita `serviceInfo.packageNames` a paquetes instalados con uber/didi (+ `com.ubercab.driver`, `com.didiglobal.driver`; `<queries>` LAUNCHER en el manifiesto) y en Ajustes → «Apps de banco» hay «Pausar lector» (`disableSelf()`) y «Volver a activar» (4.8). **Probado: con la 4.8 Nu ya funciona** (ella lo confirmó) y los pop-ups siguen bien en tablet (cálculos verificados contra capturas reales) → v30 ⚡+$ de la dinámica de Uber «+$X incluido» (4.9). Si otro banco bloquea, usar «Pausar lector»; solución de fondo: publicar en Play Store. La firma de licencias sigue usando el prefijo `LV|`: no cambiarlo o se invalidan las licencias.
- El paquete sigue siendo `com.karen.miruta` y el APK `MiRuta.apk` para que se actualice encima.
- Pop-up: ningún texto puede ser *solo un monto* (el OCR lo tomaría como tarifa); por eso "Te quedan $x", "$x/h", "$x/km" y "… km total".
