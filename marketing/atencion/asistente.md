# Asistente de atención (Claude in Chrome)

Contexto para retomar en una conversación nueva. Actualizado el 30 oct 2026; la historia del primer día y sus pendientes viejos están en `docs/atencion-historia.md`.

## Qué es
Un asistente en la extensión **Claude in Chrome** que, una vez al día:
- contesta como Eduardo los comentarios y mensajes de **TikTok** (solo la cuenta abierta en ese momento: @checa.driver o @Eduardoalv9802; no cambia de cuenta) y de **WhatsApp Web** (cuenta de empresa «Checa Netos»);
- aprende el tono de Eduardo a partir de sus respuestas de antes (`tono.md`) y usa respuestas modelo (`respuestas.md`);
- toma los links solo de los `dm.txt` / `copy.txt` de los videos;
- guarda el diario y las métricas en este repo (privado), solo dentro de `marketing/`;
- cada domingo analiza los videos y propone mejoras para los guiones (sin editar la skill).

## Decisiones tomadas
- **Repo:** APK-CHECA (privado; antes Mi-Ruta), no checa-web (público). Edita desde github.com: la línea de comandos de la extensión no tiene acceso a GitHub.
- **Modo borrador** desde el 18 oct: no envía nada sin el OK de Eduardo hasta que él diga «modo automático» (plan: una semana).
- **Nunca contesta solo** (va a «Para Eduardo»): quejas, pagos/licencias/precios/descuentos, fallas técnicas, temas delicados, colaboraciones o prensa, «¿eres bot?», cualquier duda.
- **Multimedia:** ve las imágenes y los videos dentro del chat sin descargarlos (los videos sin sonido). No escucha audios: usa la transcripción de WhatsApp si existe; si no, «Audio de 0:18 sin escuchar» para Eduardo. Nunca guarda multimedia en el repo.
- **Quien comenta «CHECA»:** se le manda el `dm.txt` de ese video (registro con Gmail para la prueba en Google Play). La regla vieja de «yo» y el link del APK ya no se usan (30 oct 2026).
- **Modelo recomendado:** Opus para «Primera vez» y el análisis del domingo; Sonnet para la rutina diaria; Haiku no.
- **wa.me:** Eduardo mencionó usar `wa.me/<número>?text=hola!` cuando alguien deje su número en TikTok. Falta confirmar la regla exacta.

## Archivos
- `marketing/atencion/`: `tono.md`, `respuestas.md`, `README.md`, `diario/AAAA-MM-DD.md` y este archivo.
- `marketing/metricas/`: `mensajes.csv` (por día y canal, con columnas de multimedia), `videos.csv` (por video y día) y `aprendizajes.md` (domingos, aún no existe).

## Prompt
El prompt vigente está en `marketing/atencion/prompt.md`: cópialo completo y pégalo en Claude in Chrome. Si cambia el flujo de los videos (CTA, `dm.txt`, precios), actualízalo ahí y avisa a Eduardo para que lo vuelva a pegar.

## Cómo retomar
En una conversación nueva de Claude Code: «Lee `marketing/atencion/asistente.md` y retomemos el asistente». Antes de revisar, haz `git pull origin main`, porque el asistente sube sus cambios directo a `main`.
