---
workflow: product-launch-video
flow: automation
storyboard: no
message: "Checa te dice en un segundo si una oferta de Uber/DiDi te conviene"
destination: TikTok / Reels / Shorts
aspect: "9:16"
language: es
length: "≤ 60 s (sale en 47 s)"
---
## Intent
Explicar cómo funciona Checa y cómo ayuda al conductor. Guion corregido con Gemini (ver `marketing/guiones-checa.md`), voz Fernando Martínez (ElevenLabs, velocidad 1.0), estilo nuevo de HyperFrames (no 1Volt).

## Cómo editar
`python3 -I generar.py` rehace `index.html` desde la voz (`assets/voz/NN.json`) y `estilo.css`; luego `npx hyperframes check` y `npx hyperframes render --quality high --output renders/video.mp4`.
