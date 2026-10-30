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
