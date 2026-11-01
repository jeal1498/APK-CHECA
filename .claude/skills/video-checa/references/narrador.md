# Modo narrador de IA: pasos técnicos

Para CHECA y su v2 de PERSONAL, sin la cara de Eduardo. Se usa solo HyperFrames con `construir.py`: arma escenas, voz, subtítulos e `index.html` desde dos archivos de texto, sin agentes. (El proceso viejo con agentes costó ~1.7 M de tokens; el 1Volt y el 3D quedaron en el historial de git. `referencia/` guarda cuadros de videos que a Eduardo le gustaron, solo como inspiración visual.)

## 1. Proyecto
Todos los videos usan un **motor compartido**: `video/hyperframes/_base/` (un solo `construir.py`, fuentes, sonidos, pop-up, subtítulos y plantilla de portada). Cada video solo guarda `SCRIPT.md`, `escenas.json`, `portada/portada.json`, `assets/voice/` y `tiempos.json`; lo demás se copia de la base al construir y git lo ignora. Así un arreglo al motor vale para todos los videos y no hay copias que diverjan.
```bash
python3 -I video/scripts/checa.py nuevo <tema> [--desde <proyecto-parecido>]
```
Los proyectos anteriores al 30 oct (`cuanto-vale-km-22`, `como-funciona-17-v2`…) conservan su copia vieja; sirven de ejemplo de `escenas.json`, pero no se construyen con la base.

## 2. SCRIPT.md y escenas.json
- `SCRIPT.md`: una línea de voz por escena, con 4 espacios al inicio. Montos **sin moneda** («110», no «$110»): ElevenLabs leyó «dólares».
- `escenas.json`: una escena por línea, en el mismo orden. La cabecera de `construir.py` explica cada tipo.

| Tipo | Para qué |
|---|---|
| `oferta` | Gancho: tarjeta que se estrella, monto que sube, título y sello `¿$?` (`"d"` cambia el detalle de la tarjeta) |
| `comparar` | Gancho con dos ofertas que se estrellan; la de más pesos se marca como «el peor» |
| `cuenta` | Tarifa ÷ km = $/km en color + ticket de gasolina → te quedan (`w_km`, `w_res`, `w_q`) |
| `meta` | Raya de meta $/km y dos barras (`w_m`, `w_u`, `w_o`) |
| `reloj15` | Cuenta regresiva sobre cuentas sin resolver |
| `lee`, `aviso`, `ticket`, `fichas`, `estados`, `metas`, `horas`, `rutas` | Explicar lo que hace Checa |
| `popup`, `duo`, `dos`, `tachado`, `numero`, `gancho` | Pop-ups, comparador, cifras grandes |
| `cierre` | Pantalla final: `{"tipo": "cierre", "pil": "Comenta CHECA", "link": false}` |

Toda palabra que se pase (`en_*`, `w_*`, `titulo_en`, `sfx`) tiene que existir en su línea de voz; si no, el script se detiene y dice cuál falta. Para un tipo nuevo, agrégalo a `video/hyperframes/_base/construir.py` (y a la tabla `TIPOS`) en vez de escribir HTML a mano, porque el motor borra y regenera `compositions/frames/`.

## 3. Voz y revisión
```bash
ELEVEN_V3=1 python3 -I video/scripts/checa.py construir <tema> --check
```
- Voz Fernando (`dlGxemPxFMTY7iXagmOj`) en **v3 en todas las líneas**: si se mezclan v2 y v3 se oyen dos voces distintas. Si cambia una línea, aparta `assets/voice` con `mv` y vuelve a correr (nunca `rm` con comodín: el entorno lo bloquea).
- Si ElevenLabs responde 401 `quota_exceeded`, avisa a Eduardo (v3 gasta más créditos).
- Los avisos de `text_occluded` e ids de Studio son normales; los errores no.

## 4. Render y revisión
```bash
ELEVEN_V3=1 setsid nohup python3 -I video/scripts/checa.py construir <tema> --render > /tmp/render-<tema>.log 2>&1 &
python3 -I video/scripts/checa.py revisar video/hyperframes/<tema>/renders/video.mp4
```
El render tarda ~2 min (espera a que `renders/video.mp4` deje de crecer). `revisar` imprime duración y formato, falla si no es `yuv420p` High (el teléfono de Eduardo no reproduce otro) y deja una hoja de 10 cuadros para mirarla con Read.

## 5. Oficio (lo que ya funcionó)
- Nada quieto en los primeros 3 s: flash en el cuadro 0, tarjeta con zoom, cifra que sube, cámara que sacude.
- Cada texto entra ~0.25 s antes de su palabra (`pal()` ya lo hace); si entra justo en la palabra se siente atrasado.
- Todo lo importante por encima de y≈1150; los subtítulos van en 1160–1480.
- Pop-ups con «OFERTA», nunca nombres de apps; «Números de ejemplo» donde haya cifras; neta = viaje − gasolina (`validar_popup` lo revisa).
- Sin música; los efectos de sonido oficiales los pone `construir.py` (máx. 3 por escena, volumen 0.3–0.5).
- Recursos de movimiento que le gustan: texto que sube desde una máscara, un elemento que empuja al siguiente, cámara que sigue lo importante, un objeto que se transforma en la siguiente escena y entradas sincronizadas con la palabra.

## 6. Videos de la web
Mismo proceso, con `{"tipo": "cierre", "sin_redes": true}`. Ejemplos: `video/hyperframes/checa-oferta-web/` y `video/tutorial-instalar/README-v2.md`.

## 7. Énfasis de Eduardo (31 oct 2026)
Eduardo marca el énfasis (lo escribe Gémini) en una lista bajo cada línea de voz, **sin cambiar el texto**; si el texto cambia, se avisa y se compara con el aprobado:
```
L3: Nomás estás poniendo de tu bolsa.
  voz: tono cansado y molesto, más lento
  fuerte: "tu bolsa"
  pausa: antes de "cada"
  tono: cansado
  pantalla: "tu bolsa" = rojo | "regales" = golpe
```
- `fuerte` → MAYÚSCULAS en `SCRIPT.md` (salen también en los subtítulos); `pausa` → «...» antes de la palabra; `tono`/`voz` → etiquetas de ElevenLabs v3 al inicio de la línea (`[urgent]`, `[firm]`, `[tired]`, `[angry]`, `[calm]`, `[slowly]`, `[quickly]`; no salen en subtítulos). El volumen de una sola palabra no se fija: se aproxima con mayúsculas y etiquetas. No pongas «CHECA» ni «Checa» en mayúsculas si no hace falta (v3 podría deletrearlo).
- `pantalla` → color (`rojo`, `amarillo`, `verde`) o `golpe` (zoom + sacudida) en esa palabra.
- Si falta una palabra de la lista en la línea, se avisa en vez de adivinar. Regenerar voces gasta créditos de ElevenLabs: cambia solo las líneas necesarias (aparta `assets/voice` con `mv`).

## 8. Gráficos de impacto (1 nov 2026)
Donde hay énfasis, el gráfico tiene que pesar tanto como la voz. Escena `impacto` (y `cierre` con `golpes`): lista `golpes`, cada uno `{"en": palabra, "kind": ...}` que cae en esa palabra con zoom, destello y sacudida y deja su lugar al siguiente.
- `kind`: `texto` (líneas `[texto, color, px]`, `icono`, `sub`, `billetes`, `flecha`, `glow`), `apagon` (negro + ícono de apagado), `logo` (semáforo que se enciende), `captura` (`img` de `assets/img/`, `chip`) y `popup`.
- Cifra gigante en rojo para lo que pierdes, verde para lo que ganas; un golpe por palabra enfatizada, con pocos elementos a la vez. Tamaños seguros: «$10» 560 px, «$9» 620 px, «−$47» 420 px, «CHECA» 320 px.
- Cuando la voz nombra la app, se muestra la app real (capturas de `docs/google-play/graficos/`, sin «Uber» ni «DiDi»; se tapa ese texto). Ejemplo: `video/hyperframes/respuesta-costo-km-32/`.

### Aprendizajes de la v2 del 32 (1 nov 2026, aprobada por Eduardo: «exactamente como quiero los videos»)
- **Una v2 con el mismo audio** se hace con `checa.py nuevo <tema>-v2 --desde <tema>`, copiando `assets/voice/` y `tiempos.json` (no se regasta voz); solo cambian `escenas.json` y la portada. Si cambias de proyecto, vuelve a correr `construir` para regenerar subtítulos y escenas.
- **El gráfico dice lo que dice la voz, palabra por palabra.** Mi primer intento puso textos que la voz no pronuncia («VIAJES CORTOS», «Pagan menos de lo que gastas», «TU CHAMBA VALE MÁS») y Eduardo lo rechazó: «no son alusivos a lo que dice la voz». Regla: el texto de cada golpe es la palabra o frase exacta que suena en ese instante (`VIAJES` → `MENOS DE` → `$10` → apagón; `COMBUSTIBLE` → `+ DESGASTE` → `CADA KM` → `$9`; `PONIENDO` → `DE TU BOLSA`; `REGALES` → `LA CHAMBA`).
- **Un golpe por palabra con peso**, uno solo en pantalla a la vez; la cifra o el verbo grande y, debajo, un `sub` en mono con la frase de apoyo («EL KILÓMETRO», «YA TE CUESTA», «CASI 9 PESOS»).
- **Íconos solo si significan lo que se dice:** gasolina para combustible, billete/dinero para «poniendo» y «regales», reloj para «chamba». El ícono de ruta (`km`) no dice nada: no lo uses de adorno.
- **Capturas con chips cortos** («MÍNIMA», un chip largo se encima con la captura). Sin «NO» ni etiquetas que la voz no dice. En el bloque de Checa, el orden de la voz manda: logo en «Checa», captura de metas en «tarifa», pop-up en «avisa», «SÍ CONVIENE» en «conviene».
- **Pop-up de ejemplo con cifras que cuadren** (120 − 22 = 98; 11 km → gasolina 22; $10.9/km; $257/h).
- **Portada de la v2 distinta a la v1:** otro título, otra tarjeta y otra cifra escondida que cuadre (62 por 10 km × 9 = 90 → «TE FALTAN 28»).
- **Palabras con acento:** en `en` se escriben igual que en la línea de voz («mínima», no «minima»). Si el script dice «No encuentro…», copia la palabra de la lista que imprime.
- **Revisa la hoja de cuadros antes de mandar** y manda el mp4 solo cuando cada gráfico coincide con la palabra de su subtítulo.

## 9. Híbrido teléfono + golpes (1 nov 2026, CHECA 22; Eduardo: «ahora sí es correcto»)
Estilo que se elige por video: el teléfono dibujado del 30 aparece **solo cuando hace falta** y los golpes del 32 caen arriba. Ejemplo: `video/hyperframes/cuanto-vale-km-22-iphone/`.
- **Flujo:** antes de renderizar, haz una **imagen de cuadros (storyboard)** con la distribución (Playwright sobre un HTML) y que Eduardo la apruebe. Así se evitaron muchas vueltas.
- **Escena `telefono`:** `{"tipo":"telefono","luz":"rojo|verde","sale":true,"telefonos":[{"oferta":{...},"popup":{...},"popup_en":"checa","x":221,"y":430,"k":1.45,"entra_en":"palabra","dim_en":"palabra"}],"golpes":[...]}`. El teléfono sube desde abajo; `sale` lo baja al final (úsalo si la escena siguiente no lleva teléfono). Dos teléfonos en la misma escena: `x` 60 y 580, `y` 760, `k` 1.0.
- **Zonas:** golpes arriba (y 70, h 330; cifra de 230–250 px con `sub`), teléfono en medio (termina antes de y 1740), subtítulos abajo con `opciones.json` en el proyecto: `{"subs_top": 1745, "subs_h": 150}`. Las escenas `impacto` del mismo video usan la zona normal.
- **Oferta de viaje:** formato medido sobre una captura real de Eduardo (etiqueta negra «Viaje», `MXN90.00` grande, ★, ruta con punto y cuadro negros, «Viaje disponible» con barra). **Sin logo ni nombre de app.** Compacta (poco texto, direcciones cortas) y pegada al **fondo de la pantalla del teléfono**, con el mapa arriba. Campos: `precio, rating, a, a_dir, viaje, v_dir`.
- **Pop-up (`popup`) = el de la app Android**, copiado de la captura real (franja ACEPTAR, `$x netos` en blanco, `Viaje $x · Gasolina −$y`, tres fichas con su «meta $x»; verde si cumple). **No** el de marca v2 del motor (`checa-popup.html`). Formato: `{"app":"OFERTA","neta":60,"viaje":90,"gasolina":30,"chips":[["$225/h","meta $150",true],["$6/km","meta $5",true],["24 min","15 km total",false]]}`. La meta de la ficha tiene que ser la que dice la voz.
- **Sincronía:** las palabras de `tiempos.json` son precisas (±0.1 s, sin sesgo; se midió contra la voz). El desfase venía del adelanto de 0.25 s: en cada escena pon `"adelanto": 0.04` para que el golpe, el pop-up y el teléfono caigan justo con la palabra. Sin esa clave queda 0.25 (los videos ya aprobados no cambian).
- **Revisor del motor:** el teléfono y los golpes se encima a propósito; el teléfono lleva `data-layout-ignore`. Si `check` marca `content_overlap`, no lo quites: separa zonas.

## 10. Coherencia y ritmo (1 nov 2026, C19 «A3», aprobado por Eduardo)
Lo que salió de las auditorías visuales del C19 (`calidad.md` §3). Aplícalo **desde el primer render**, no esperes a que la auditoría lo marque.
- **Cada cosa entra en su palabra, nunca antes.** El color del pop-up (rojo, verde, amarillo) cambia en la palabra «Rojo», «Verde», «considerar»…; antes va neutro. Cifras y fichas aparecen en la palabra que las dice, con su valor final (nada de conteos que muestren números a medias como «$51/h»).
- **Sin etiquetas falsas:** si el número todavía no es el neto, no lleva «netos» (usa «viaje» y cambia a «netos» en «limpio»). Mientras no se dice la cifra, muestra `···`.
- **Lo que la voz nombra sigue en pantalla:** si dice «esa oferta», la tarjeta de la oferta queda visible junto al pop-up (tarjeta compacta arriba, con su botón «Aceptar»).
- **Un gráfico por idea que no tiene imagen:** «Lee la oferta» → barrido de escaneo sobre la tarjeta; «ni miras la pantalla» → ojo tachado; «en voz alta» → altavoz (entra en «voz», no antes); «no acepta por ti» → cursor que se frena sobre «Aceptar» con 🚫; «la app Checa» → logo del semáforo; «Google Play» → chip de Google Play; «te cae la oferta» → la tarjeta cae con vibración.
- **Hook:** el cuadro 0 ya trae el texto del gancho y la cifra grande, al 100 % (sin fundido ni destello en t=0). El texto del gancho conecta con lo que dice la voz («Aceptas un viaje…» → «¿LO ACEPTAS?»).
- **Ritmo:** cámara distinta por escena (zoom, giro leve, corte lateral, zoom o giro alternados), lavado de color a pantalla completa en «Rojo»/«Verde» y sacudida en lo que pierde. Ningún tramo de más de ~1.5 s sin algo nuevo en su palabra.
- **Legibilidad:** golpes que quepan en 1080 px (130 px hasta 9 letras, 108 px hasta 14, 96 px más largo); subtítulos con su parte de arriba en y≈1250 (fuera de la franja baja de TikTok); textos chicos de 34 px mínimo; nada de elementos semitransparentes «fantasma» detrás del pop-up.
- **Anclajes:** barra de progreso verde de 8 px arriba; botón «Comenta CHECA» con pulso; el botón entra en «CHECA», no después.
- **Cierre y loop:** máximo ~1 s después de la última palabra. En los últimos 0.5 s el cierre se disuelve y el **último cuadro es idéntico al primero** (compáralos: diferencia 0), con un whoosh que enlaza con el golpe del inicio.
