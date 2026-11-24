# Editor IA · PERSONAL

Flujo de edición de la cuenta PERSONAL (basado en el método del video de referencia de PERSONAL).

| Carpeta | Qué va |
|---|---|
| `Brutos/` | Tus grabaciones sin editar (mp4). **No se suben a Git** (pesan; solo se pasan por el chat o se dejan aquí en local). |
| `Referencias/` | De 3 a 5 videos (mp4) y capturas que te encantan + README con qué tomamos. **Inspírate, no copies.** |
| `Exportados/` | Videos terminados y aprobados. El mp4 se commitea **una sola vez**, ya aprobado. |

## Flujo
1. Eduardo graba y suelta el bruto en `Brutos/` (o lo manda por el chat).
2. Claude revisa `Referencias/` y dice en una línea qué toma el video de ellas.
3. FFmpeg corta silencios; Whisper subtitula y da el tiempo de cada palabra; los gráficos salen de la biblioteca EA (Remotion, `video/src/EA/`) y se montan sobre tu video con FFmpeg.
4. Borrador para Eduardo; se corrige solo lo que marque.
5. Aprobado → `Exportados/`.

Frase de arranque: «Hook personal: …» (con el bruto o el tema).
Limites: yt-dlp no llega (la red solo permite GitHub, PyPI y npm): las referencias las pasa Eduardo como mp4 o captura.


## Identidad
La identidad de PERSONAL (reemplaza PS2) está en `Identidad/IDENTIDAD.md`. Léela antes de escribir un guion o producir un video.

## Biblioteca EA (aprobada 24 nov 2026)
Código: `video/src/EA/` (tema en `theme.ts`, componentes en `components/`). Cada componente recibe sus textos y cifras por props; no se programa uno nuevo por video.

| Componente | Para qué |
|---|---|
| `Subtitles` | Subtítulos por bloques, palabra clave en naranja |
| `Headline` | Titular que se revela |
| `Popup` (s/m/l) | Tarjeta informativa |
| `StatCard` | Cifra con contador (dinero, %, cantidad) |
| `Comparison` | Dos cifras enfrentadas |
| `Chart` | Barras horizontales, verticales o progreso |
| `LowerThird` | Identificador (persona, app, concepto) |
| `Arrow`, `Circle`, `Box`, `Underline`, `Sello` | Anotaciones animadas |
| `Logo` | EA. compacta u horizontal |
| `Transition` | Barrido negro y naranja |

Ver todo en movimiento: `cd video && npx remotion render src/EA/index.ts EA-Showcase` (24 s). Muestra de 6 s: `EA-Muestra`. Estudio: `npx remotion studio src/EA/index.ts`.
Zonas seguras 1080×1920: 200 px arriba, 380 abajo, 60 a los lados.
