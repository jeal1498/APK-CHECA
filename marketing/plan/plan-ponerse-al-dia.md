# Plan para ponerse al día con los videos (30 oct 2026)

Para empezar en una **conversación nueva**. Hecho el 30 oct 2026 con lo que hay en el repo; todo lo marcado «confirmar» lo debe confirmar Eduardo antes de actuar.

## 0. Arranque de la conversación nueva
1. **Rama:** todo el trabajo del 29–30 oct está en `ccr-5643065c-aezfnl` (repo `APK-CHECA`), no en `main`. Pregunta a Eduardo si la pasa a `main` (el CLAUDE.md pide `main` directo) y, mientras tanto, `git fetch origin ccr-5643065c-aezfnl && git checkout ccr-5643065c-aezfnl`.
2. Lee `CLAUDE.md` (sección «Cómo trabajar con el usuario»), la skill `videos-checa-camara` (sobre todo «Estado y pendientes» y «Novedades del 30 oct») y `marketing/plan/reglas-v2.md`.
3. Eduardo habla en masculino, quiere respuestas **cortas**; un video a la vez, **una aprobación por etapa** (guion → tabla de animaciones → portada → video → copy), sin adelantar la siguiente.
4. **Pídele confirmar qué publicó desde el 25 oct** (ver §1) y actualiza los dos calendarios antes de planear fechas.

## 1. Dónde está la verdad del estado (hay dos calendarios)
| Fuente | Qué tiene | Estado |
|---|---|---|
| Calendario interno de la web: repo `PWA-Checa-Multimedia`, carpeta `calendario-d4f81c/` (`data.json` con `e` = `pub` / `listo` / `proceso`, y los videos en `v/`) | Es el que Eduardo usa para subir (copy, DM, reproductor, descarga) | **Al día hasta el 25 oct**; última edición «C23 y P23 publicados el 25 oct» |
| `marketing/plan/calendario.md` (este repo) | Tabla de apoyo | **Desfasada**: no marca 12, 14, 15 ni 23 de PERSONAL, ni el 26 |

Regla: tras confirmar con Eduardo, actualizar **ambos** (el `data.json` con commit a `main` del repo de la web, como se hizo el 25 oct).

## 2. Inventario (según los dos calendarios al 30 oct; Eduardo confirma que no ha publicado nada desde el 25 oct)
**Publicados (hasta el 25 oct):** CHECA 02–06, 08, 10, 12, 21, 23 · PERSONAL 05, 06, 07, 08, 12, 19, 23.

**Listos en la web, sin publicar el 25 oct (confirmar si ya se subieron):**
| Cuenta | Video | Dónde está |
|---|---|---|
| CHECA | 14 Miedo a rechazar · 15 Desgaste y renta | `v/C14.mp4`, `v/C15.mp4` |
| PERSONAL | 10 Mejores horas · 14 Miedo a rechazar · 15 Desgaste y renta | `v/P10.mp4`, `v/P14.mp4`, `v/P15.mp4` |

**Hechos en este repo pero «en proceso» en la web (falta subirlos al calendario web y revisar copy):** CHECA 17 Cómo funciona Checa, 19 Checa lee la oferta (los dos con `video.mp4`, `portada.jpg`, `copy.txt`, `dm.txt`).

**Con video pero fuera de calendario:** CHECA 18 Cuánto ganas tú (ya salió en PERSONAL como original) · CHECA 20 Cómo instalar Checa (**descartado el 30 oct**).

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

## 3. Decisiones de Eduardo (30 oct 2026)
1. **Desde el 25 oct no ha publicado nada.** Todo lo «listo» sigue sin subir; el atraso es de 5 días.
2. **CTA nuevo: hecho.** En `copy.txt` y `dm.txt` de CHECA 14, 15, 19 y en el calendario web de C14, C15, C19, P10, P14 y P15, la llamada es «Comenta «CHECA» y te mando el acceso a la prueba en Google Play (solo Android).» y el DM manda a `/registro/` con su `utm_campaign` (modelo del 23). Ojo: **el cierre de los videos ya renderizados no cambia** y dice «Descarga CHECA» con el link de la web; la web ofrece el APK hasta el 15 nov. Si Eduardo quiere, se puede regrabar solo la pantalla final.
   - **CHECA 17 «Cómo funciona Checa»: no subirlo tal cual.** Su voz termina con «pruébala 7 días sin costo» (ya no es la oferta) y no tiene `copy.txt`. Solo se actualizó su `dm.txt`. Rehacer el último corte o descartarlo (pendiente de Eduardo).
3. **Video 20 «Cómo instalar Checa»: descartado.** Explica instalar el APK, que se cierra el 15 nov. La carpeta se conserva sin subirse. Si hace falta, hacer uno nuevo «cómo entrar a la prueba en Google Play».
4. **Borradores 24 y 25 de PERSONAL (Eduardo no los recordaba).** Son **tres versiones de la misma idea** («aceptaste un viaje de 60 que te deja 38») y las tres cierran con la campaña vieja «Busco 50 conductores, 3 meses gratis, comenta YO»: `24 - Viaje de 60 que se queda en 38`, `25 - Checa lee la oferta en 3 segundos` y `25 - Pagar gasolina antes de aceptar` (Remotion). **Recomendación: descartar las tres;** el video 26 ya cuenta esa idea con mejor gancho y el CTA vigente es otro. Pendiente de que Eduardo lo confirme; si no, conservar solo una y renumerarla.
5. **Estadísticas de TikTok: no por ahora.** Se sigue sin horario medido (usar 8 pm) y se retoma la medición del domingo cuando él quiera.
6. **Falta decidir:** aprobar o descartar el guion del 22 «Cuánto vale tu kilómetro» (CHECA y PERSONAL).

## 4. Plan en tres fases
**Fase 1 — Subir lo que ya está hecho (30 oct – 5 nov).** Un video diario por cuenta, solo venta (los de consejo están descartados). Propuesta, a ajustar con la respuesta 1 de §3:
| Día | CHECA | PERSONAL |
|---|---|---|
| vie 30 oct | 14 Miedo a rechazar | 10 Mejores horas |
| sáb 31 oct | 15 Desgaste y renta | 14 Miedo a rechazar |
| dom 1 nov | **26 Dos ofertas** (el más fuerte; antes: su copy y su dm) | 15 Desgaste y renta |
| lun 2 nov | 19 Checa lee la oferta | 26 v2 (crearla antes, ver Fase 2) |
| mar 3 nov | 17 solo si se rehace su cierre; si no, 22 v1 | 22 v2 si se aprueba |
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

## Avance al 30 oct 2026 (noche)
Hecho: copy y DM del 26; calendarios con fechas de la Fase 1; 17 rehecho; 26 v2 hecha; cierre nuevo en C14, C15, C19, P10, P14, P15; 24 y 25 descartados. PERSONAL 17 v2 hecho y en el calendario web (3 nov). Pendiente: subir los videos (calendario de arriba), decidir el guion del 22 y los videos nuevos de urgencia. Detalle en `CLAUDE.md` («Videos — puesta al día»).
Descartado el 30 oct: video «Plan Fundador: primeros 50» (Eduardo). Guion del 22 reescrito y por aprobar.
