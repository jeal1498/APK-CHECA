# Asistente de atención: historia (18 oct 2026)

> Movido desde `marketing/atencion/asistente.md` el 30 oct 2026.

## Primer día (18 oct 2026)
- Cuenta revisada: @checa.driver + WhatsApp. Llegaron 5 en TikTok y 5 en WhatsApp, y se contestaron 6 hilos (todos aprobados por Eduardo).
- Para Eduardo: audio de 0:18 sin escuchar (chat ·2691) y video de 0:55 sin ver (chat ·7360, el que pidió el APK por chat).
- Videos: 04 Recogidas lejanas 980 vistas / 29 me gusta · 03 Seguridad de noche 673 / 27 · 05 Cansancio 576 · 21 Beta testers corto 546 en un día (trae los «yo») · 02 Trabajar sin lana 499 · Ayuda Android 473 (5 comentarios). Es muy pronto para sacar conclusiones; los de dolor (recogidas, seguridad) van arriba.
- Problemas: TikTok pidió captcha de puzle (no se resuelve; se reintentó). `videos.csv` quedó con «n/d» en tiempo visto, % completo, compartidos, guardados y seguidores (solo salen en TikTok Studio, video por video).

### Revisión de Claude Code (18 oct)
- Bien: tono, escalamiento de audio/video, no duplicó al que Eduardo ya contestó, detectó el «35 %» y los links viejos a `/beta/`.
- Corregido en el repo: quitó los teléfonos casi completos del diario (ahora «chat ·7360») y la respuesta de «yo» en `respuestas.md` ya no pide Gmail.
- La respuesta de precio a @danieltapiacisner4 la aprobó Eduardo, así que está bien; en modo automático el precio sigue yendo a Eduardo.

## Pendientes
**De Eduardo**
- Escuchar el audio (·2691) y ver el video (·7360) de WhatsApp.
- Editar su respuesta a @jaredtavera56 («el ahorro y las ganancias te van a pagar la anualidad» es promesa de ganancia).
- Pegar en TikTok la presentación corregida de `respuestas.md` como respuesta guardada, y quitar de sus plantillas los links a `…/PWA-Checa-Multimedia/beta/`.
- Decidir si la respuesta de precio aprobada (gratis hasta el 31 ene, después los precios de la página, descuento de fundador) queda como respuesta modelo para el modo automático.
- Confirmar la regla de `wa.me`.
- Revisar la sección de @Eduardoalv9802 en `tono.md` la primera vez que el asistente la llene.

**De Claude Code (próxima conversación)**
- El video «Ayuda Android» no tiene carpeta, NN ni `dm.txt`: darle número en `calendario.md` y un `dm.txt` con su UTM para poder medirlo.
- Revisar el diario de cada día cuando Eduardo lo pida (`git pull origin main`, luego `marketing/atencion/diario/` y `marketing/metricas/`).
- El primer domingo, revisar `aprendizajes.md` antes de usarlo en guiones.

