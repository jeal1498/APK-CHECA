# Prompt para Opus: arreglar la Referencia 02 de PERSONAL (sesión nueva)

Copia todo lo que está debajo de la línea y pégalo en una sesión nueva con Opus (repo `jeal1498/APK-CHECA`).

---

Eres Opus trabajando con Eduardo (Lalo, hombre; respuestas cortas, en español; escucha el chat con voz, nada de textos largos). Repo `jeal1498/APK-CHECA`: lee `CLAUDE.md` (sección «Cómo trabajar con Eduardo» y «Videos») y `marketing/videos/PERSONAL/Editor IA/LEEME.md` e `Identidad/IDENTIDAD.md`. Trabaja en la rama de la sesión y sube también a `main` en commits pequeños. No hagas PR.

## Problema (lo único que importa)
La composición `EA-Referencia-02` (`video/src/EA/Referencia02.tsx`, 26 s, 1080×1920, 30 fps) **se ve mal en el reproductor de Eduardo: interferencia, se traba o se rompe**, aunque yo (otra sesión) no logré reproducirlo. Ya probé y NO bastó: (1) mp4 directo de Remotion (`yuvj420p`, ~460 kbps, GOP de 250); (2) PNG en secuencia + ffmpeg `libx264 -crf 14 -g 30 -pix_fmt yuv420p` con matriz bt709 (`ref02c.mp4`). Esa versión salió limpia en mis revisiones de fotogramas, así que **la causa real sigue sin identificar**. Tu trabajo es encontrarla y arreglarla, no repetir lo mismo.

## Paso 0 (una pregunta corta a Eduardo, antes de nada)
Pregunta en una línea: ¿en qué aparato y reproductor lo ves (teléfono/iPhone/Android, galería, WhatsApp, CapCut, navegador) y en qué segundo falla? Sigue con el diagnóstico mientras responde.

## Hipótesis a descartar con evidencia (no a adivinar)
1. **Costo de render por fotograma:** 80 `div` con transform + `filter: blur` sobre un contenedor de 1080×1920 + sombras. En Remotion no afecta la reproducción, pero sí si el render se hizo con tiempo agotado o fotogramas cortados. Revisa el log por `timeout`/`delayRender`/fotogramas repetidos y renderiza con `--concurrency=1` para comparar contra el normal.
2. **Fotogramas idénticos o saltados:** mide la diferencia entre fotogramas consecutivos (numpy sobre el video en gris a 270 px) y busca picos y repeticiones dentro de animaciones (no en pausas).
3. **Marcas de tiempo (VFR/pts):** `ffprobe -show_frames` y comprueba que `pts` avance de 1/30 s sin huecos; fuerza CFR (`-r 30 -fps_mode cfr`).
4. **Compatibilidad del códec:** prueba `-profile:v high -level 4.0`, y una variante `-profile:v main -level 4.0 -bf 0` y otra con `-tune animation`. Los reproductores móviles fallan con B-frames/CRF bajo en contenido plano de alto contraste.
5. **Color:** ya se corrigió (usar `scale=in_range=pc:out_range=tv:out_color_matrix=bt709,format=yuv420p`); no lo quites.
6. **Animación CSS que Remotion no controla:** cualquier `transition`, `animation` o `will-change` en `video/src/EA/**` rompe el determinismo. Búscalos (`grep`) y quítalos.
7. **Diseño:** cuadros que se solapan al aplastarse (`escalaY`), bordes de 3 px sobre fondo marfil y texto muy fino se ven como «interferencia» al comprimir. Si el aliasing es el culpable, sube el grosor, evita bordes de 1–2 px y no uses opacidades intermedias en elementos de línea fina.

Entrega la causa con evidencia (una línea por hipótesis: confirmada o descartada y cómo lo comprobaste).

## Qué debe hacer el video (no cambiar la dirección, ya aprobada)
Estilo EA editorial: marfil `#F1EEE6`, negro `#151515`, naranja `#F47732`, Montserrat 800 / Roboto, tema en `video/src/EA/theme.ts`. Zona de cámara arriba (0–800 px, recuadro gris que luego se reemplaza por el bruto), gráficos abajo, subtítulos en y=1388. **Máximo 4 palabras en pantalla por escena** (sin contar subtítulos). Una sola pieza que se transforma (80 cuadros, 1 cuadro = $1; 16×5):

| Escena | Segundos | Acción literal | Texto |
|---|---|---|---|
| 1 | 0–2.6 | Tarjeta de $80 entra; línea naranja la parte en «no siempre» (1.4 s) | `$80` |
| 2 | 2.6–6.2 | Se deshace en 80 cuadros que se cuentan hasta 80 en «cuenta» (5.9 s) | `1 cuadro = $1` |
| 3 | 6.2–12 | Tira de 14 segmentos bajo la rejilla desenfocada; 28 cuadros pasan a naranja hasta «$28» (11.6 s) | `14 km`, `$28` |
| 4 | 12–14.4 | Los 28 se aplastan en una barra del 35 % del ancho de la rejilla | `35 %` |
| 5 | 14.4–16.8 | Los 52 restantes se contraen en una tarjeta `$52` (escala 52/80); fantasma de `$80` borroso y tachado | `$52` |
| 6 | 16.8–21.6 | La tarjeta crece; un interruptor pasa de ✕ a ✓ en «decisión» (20.9 s) | `¿Sí o no?` |
| 7 | 21.6–26 | La tarjeta se vuelve globo de comentario y entra el logo EA. en «comentarios» (24.4 s) | `¿La haces?` |

Cuenta: 14 km ÷ 12 km/l × $24 = $28; 80 − 28 = 52; 28 ÷ 80 = 35 %. Subtítulos (ya escritos en `Referencia02.tsx`, sin R fuerte al inicio ni RR). Cada escena debe terminar en una composición limpia antes de cortar.

## Reglas de implementación
- Remotion con `useCurrentFrame()`/`interpolate()`; todo determinista. Componentes reutilizables en `video/src/EA/components/` (Unidades, Tarjeta, Interruptor, etc.). No instales dependencias sin justificarlo.
- Entorno: `cd video && npm i` (la carpeta `node_modules` no viene). Fuentes ya en `video/public/ea/`. Chromium: `--browser-executable=/opt/pw-browsers/chromium --chrome-mode=chrome-for-testing`. Hay `ffmpeg` 6.1 y Node 22.
- Si una técnica (blur, escala por eje, 80 transforms) es el problema, sustitúyela por otra más simple que dé el mismo efecto (por ejemplo, un `<canvas>` o SVG único para la rejilla en vez de 80 `div`, o precalcular posiciones).

## Exportación obligatoria
```bash
cd video
npx remotion render src/EA/index.ts EA-Referencia-02 /ruta/seq --sequence --image-format=png
ffmpeg -framerate 30 -i /ruta/seq/element-%03d.png \
  -vf "scale=in_range=pc:out_range=tv:out_color_matrix=bt709,format=yuv420p" \
  -c:v libx264 -preset slow -crf 14 -g 30 -keyint_min 30 -r 30 -fps_mode cfr \
  -colorspace bt709 -color_primaries bt709 -color_trc bt709 -color_range tv \
  -movflags +faststart salida.mp4
```
(No uses el mp4 directo de Remotion: sale `yuvj420p` y a ~450 kbps.)

## Verificación antes de entregar (con números, no a ojo)
1. `ffprobe`: `pix_fmt=yuv420p`, 780 fotogramas, CFR 30, un fotograma clave por segundo, `pts` sin huecos.
2. Diferencia entre fotogramas consecutivos: sin picos fuera de las transiciones previstas y sin fotogramas idénticos dentro de animaciones.
3. Color: el naranja decodificado debe quedar a ±3 de `244,119,50` (compara un píxel del PNG con el del mp4).
4. Mira los fotogramas de cada transición (escenas 1→2, 3→4, 4→5, 6→7) ampliados al 200 % y confirma que no hay bordes dobles, líneas residuales, cuadros estirados ni texto encimado.
5. Entrega dos variantes (alto: el comando de arriba; y compatible: `-profile:v main -level 4.0 -bf 0`) y pídele a Eduardo que diga cuál se ve bien.

## Entrega
Envía los mp4 con `SendUserFile`, commit y push (rama de la sesión y `main`) del código, y deja la causa encontrada en `Editor IA/LEEME.md` (sección «Cómo exportar sin interferencia»). No subas el mp4 a Git hasta que Eduardo diga «aprobado». Termina con una acción de menos de 2 min para él.
