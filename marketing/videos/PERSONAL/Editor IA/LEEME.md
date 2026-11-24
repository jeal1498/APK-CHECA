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
3. FFmpeg corta silencios; Whisper subtitula y da el tiempo de cada palabra; HyperFrames hace los gráficos con la identidad de la cuenta.
4. Borrador para Eduardo; se corrige solo lo que marque.
5. Aprobado → `Exportados/`.

Frase de arranque: «Hook personal: …» (con el bruto o el tema).
Limites: yt-dlp no llega (la red solo permite GitHub, PyPI y npm): las referencias las pasa Eduardo como mp4 o captura.
