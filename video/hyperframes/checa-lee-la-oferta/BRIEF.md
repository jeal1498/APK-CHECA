---
workflow: product-launch-video
flow: automation
storyboard: no
message: "Checa te dice en un segundo si una oferta de Uber o DiDi de verdad te conviene"
destination: tiktok
aspect: 1080x1920
language: es
audience: conductores de Uber y DiDi
length: 50s
angle: problema → cómo funciona → beneficios → prueba gratis
---

## Intent

Video que explica cómo funciona la app Checa y cómo ayuda al conductor, para TikTok / Reels / Shorts.
Eduardo quiere ver «de lo que es capaz HyperFrames»: seguir el proceso completo de HyperFrames
(captura de la web, preset de diseño, storyboard, escenas por frame, transiciones, captions, mezcla).

## Assets

- assets/popup-referencia.md — cómo se ve el pop-up real de Checa (colores, textos, medidas). Se debe respetar tal cual.

## Customizations

- Voz: **Fernando Martínez** (ElevenLabs `dlGxemPxFMTY7iXagmOj`, velocidad 1.0). Se queda igual que el video anterior.
- Pop-up: el mismo tipo de pop-up de la app (franja del semáforo con ACEPTAR / CONSIDERAR / RECHAZAR, neta grande, «Viaje $x · Gasolina −$y», 3 fichas).
- Guion: verbatim (VO_MODE: verbatim), el corregido con Gemini (`marketing/guiones-checa.md`, «Cómo funciona Checa»).

## Notes

- Mostrar «Números de ejemplo» donde salgan cifras.
- La oferta de Uber/DiDi en pantalla es genérica: no imitar logos ni marcas.
- Sin ciudad ni país. Sin «link en el perfil».
- HeyGen sin sesión: voz y música por ElevenLabs (clave en `ELEVENLABS_API_KEY`).
