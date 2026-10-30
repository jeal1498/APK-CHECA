# Plan para ponerse al día con los videos (30 oct 2026)

Para empezar en una **conversación nueva**. Hecho el 30 oct 2026 con lo que hay en el repo; todo lo marcado «confirmar» lo debe confirmar Eduardo antes de actuar.

## 0. Arranque de la conversación nueva
1. **Rama:** todo el trabajo del 29–30 oct está en `ccr-5643065c-aezfnl` (repo `APK-CHECA`), no en `main`. Pregunta a Eduardo si la pasa a `main` (el CLAUDE.md pide `main` directo) y, mientras tanto, `git fetch origin ccr-5643065c-aezfnl && git checkout ccr-5643065c-aezfnl`.
2. Lee `CLAUDE.md` (sección «Cómo trabajar con el usuario»), la skill `videos-checa` (sobre todo «Estado y pendientes» y «Novedades del 30 oct») y `marketing/plan/reglas-v2.md`.
3. Eduardo habla en masculino, quiere respuestas **cortas**; un video a la vez, **una aprobación por etapa** (guion → tabla de animaciones → portada → video → copy), sin adelantar la siguiente.
4. **Pídele confirmar qué publicó desde el 25 oct** (ver §1) y actualiza los dos calendarios antes de planear fechas.

## 1. Dónde está la verdad del estado (hay dos calendarios)
| Fuente | Qué tiene | Estado |
|---|---|---|
| Calendario interno de la web: repo `PWA-Checa-Multimedia`, carpeta `calendario-d4f81c/` (`data.json` con `e` = `pub` / `listo` / `proceso`, y los videos en `v/`) | Es el que Eduardo usa para subir (copy, DM, reproductor, descarga) | **Al día hasta el 25 oct**; última edición «C23 y P23 publicados el 25 oct» |
| `marketing/plan/calendario.md` (este repo) | Tabla de apoyo | **Desfasada**: no marca 12, 14, 15 ni 23 de PERSONAL, ni el 26 |

Regla: tras confirmar con Eduardo, actualizar **ambos** (el `data.json` con commit a `main` del repo de la web, como se hizo el 25 oct).

## 2. Inventario (según los dos calendarios al 30 oct)
**Publicados (hasta el 25 oct):** CHECA 02–06, 08, 10, 12, 21, 23 · PERSONAL 05, 06, 07, 08, 12, 19, 23.

**Listos en la web, sin publicar el 25 oct (confirmar si ya se subieron):**
| Cuenta | Video | Dónde está |
|---|---|---|
| CHECA | 14 Miedo a rechazar · 15 Desgaste y renta | `v/C14.mp4`, `v/C15.mp4` |
| PERSONAL | 10 Mejores horas · 14 Miedo a rechazar · 15 Desgaste y renta | `v/P10.mp4`, `v/P14.mp4`, `v/P15.mp4` |

**Hechos en este repo pero «en proceso» en la web (falta subirlos al calendario web y revisar copy):** CHECA 17 Cómo funciona Checa, 19 Checa lee la oferta (los dos con `video.mp4`, `portada.jpg`, `copy.txt`, `dm.txt`).

**Con video pero fuera de calendario:** CHECA 18 Cuánto ganas tú (ya salió en PERSONAL como original) · CHECA 20 Cómo instalar Checa (tutorial del APK).

**Con solo guion o borrador:**
| Video | Estado |
|---|---|
| 22 Cuánto vale tu kilómetro (CHECA y PERSONAL) | guion reescrito el 19 oct, **sin aprobar** |
| PERSONAL 24 «Viaje de 60 que se queda en 38» | borrador del 28 oct (no existe versión CHECA) |
| PERSONAL 25 «Checa lee la oferta en 3 segundos» | borrador del 29 oct (skill `guion-vertical-checa`) |
| PERSONAL 25 «Pagar gasolina antes de aceptar» | borrador Remotion del 29 oct. **Dos videos con el número 25: renumerar uno** |

**En curso, el más avanzado — CHECA 26 «Dos ofertas, misma neta» (30 oct):**
- Hecho y aprobado por Eduardo: guion, voz con Audio Tags v3, video (31.5 s) y portada «No son $95» como primer cuadro. `video.mp4` y `portada.jpg` ya están en `marketing/videos/CHECA/26 - Dos ofertas misma neta/`.
- Falta: `copy.txt` y `dm.txt`, su fila en los dos calendarios, y la **v2 PERSONAL** (propuesta de Claude: abrir con la oferta de $82 que «parece peor» y revelar al final que la de $95 deja menos; otra voz, otras escenas, mismo número 26).
- Proyecto: `video/hyperframes/dos-ofertas-misma-neta/` (ver «Novedades del 30 oct» en la skill).

## 3. Decisiones que necesita de Eduardo (antes de grabar o subir)
1. **¿Qué se publicó entre el 25 y el 30 oct?** (para marcar ✅ y no repetir).
2. **CTA vigente de los copys viejos.** Los `copy.txt` y `dm.txt` de 06–19 dicen «7 días gratis», «Descarga CHECA» y mandan al APK de `checa-web`. Hoy: el APK se cierra el **15 nov** (`Migracion.LIMITE_MS`), la app está en prueba cerrada de Google Play y el acceso es por `/registro/` (cierra a los 50 o el 15 nov; Plan Fundador = primeros 50). Propuesta: antes de subir cada video listo, cambiar la llamada a «Comenta CHECA y te mando el acceso» y el `dm.txt` al link de `/registro/` con su `utm_campaign` (modelo: carpeta 23). Confirmar redacción.
3. **Video 20 «Cómo instalar Checa»:** explica instalar el APK, que se cierra el 15 nov. Propuesta: no subirlo; si hace falta, hacer uno nuevo «cómo entrar a la prueba en Google Play».
4. **Borradores 24 y 25 (PERSONAL):** ¿se conservan? ¿cuál conserva el 25? ¿se hace su versión CHECA primero (regla: la v1 es de CHECA y la v2 de PERSONAL)?
5. **Aprobar el guion del 22** (o descartarlo; su dato «cuánto vale tu kilómetro» coincide con una búsqueda real de TikTok).
6. **Horario:** el calendario sugiere 8 pm, sin datos que lo apoyen. Pedir las estadísticas de TikTok Studio de los últimos videos (la última captura es del 19 oct, `marketing/metricas/videos.csv`) para actualizar `propuesta-semana-19-oct.md` y escoger hora.

## 4. Plan en tres fases
**Fase 1 — Subir lo que ya está hecho (30 oct – 5 nov).** Un video diario por cuenta, solo venta (los de consejo están descartados). Propuesta, a ajustar con la respuesta 1 de §3:
| Día | CHECA | PERSONAL |
|---|---|---|
| vie 30 oct | 14 Miedo a rechazar | 10 Mejores horas |
| sáb 31 oct | 15 Desgaste y renta | 14 Miedo a rechazar |
| dom 1 nov | **26 Dos ofertas** (el más fuerte; antes: su copy y su dm) | 15 Desgaste y renta |
| lun 2 nov | 19 Checa lee la oferta | 26 v2 (crearla antes, ver Fase 2) |
| mar 3 nov | 17 Cómo funciona Checa | por definir (22 v2 si se aprueba) |
Antes de cada subida: revisar el `copy.txt` (CTA de §3.2), pasar el video al calendario web y marcar ✅ con la fecha.

**Fase 2 — Crear lo que falta (en paralelo, un video a la vez).**
1. 26: `copy.txt`, `dm.txt`, filas en calendarios (≈15 min).
2. 26 v2 PERSONAL (flujo completo por etapas; reutilizar `construir.py` del proyecto 26).
3. 22 v1 y v2 si se aprueba el guion.
4. 24/25 según la decisión de §3.4.
5. **Videos nuevos por urgencia (proponer a Eduardo, no hacer sin su aprobación):** «Checa cambia de app» (los usuarios del APK deben pasar a Play antes del 15 nov, con la cuenta Google se conservan ajustes y licencia) y «Plan Fundador: primeros 50» (urgencia real, el contador de `/registro/`). Ambos mandan a `/registro/` con su `utm_campaign`.

**Fase 3 — Ritmo estable (desde el 6 nov).** Un video por cuenta al día mientras haya material; si no, 3–4 por semana. Cada domingo: Eduardo pasa vistas, % completo y comentarios «CHECA»; Claude llena visitas y descargas con `reportes/web.md` (tabla «Medición semanal» de `calendario.md`) y repite los temas que retienen (comparaciones con pesos; ver `propuesta-semana-19-oct.md`).

## 5. Reglas de oficio que no hay que olvidar
- Gancho con movimiento desde el cuadro 0 (Eduardo: los primeros 3 s «dan pereza» si empiezan quietos). Cifras de ejemplo rotuladas «Números de ejemplo»; neta = viaje − gasolina; cifras iguales en voz, escenas y portada.
- `video.mp4` se commitea **una sola vez**, aprobado y con la portada como primer cuadro (`video/scripts/portada_primer_cuadro.sh`). Las pruebas se mandan con `SendUserFile` desde `renders/` (ignorado en git).
- Sin ciudad ni país, sin logos ni nombres de Uber/DiDi, ortografía «checas / cheques».
- Un `rm` con comodín fue bloqueado por el entorno: para apartar voces viejas usar `mv`.
- Si ElevenLabs responde 401 `quota_exceeded`, avisar a Eduardo (v3 gasta más créditos que v2).
