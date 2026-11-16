# Seguimiento maestro de Checa

**Última actualización:** 16 de noviembre de 2026 (auditoría de cierre: plan E **BLOQUEADO** en E0; ver `EXPEDIENTE-E0-E7.md` § 0)  
**Repositorio:** `jeal1498/APK-CHECA`  
**Propósito:** mantener el objetivo, el orden de ejecución, las decisiones y la evidencia de cada entrega en un único lugar. Actualizar este archivo cada vez que se revise o cierre una entrega.

## Objetivo principal

Convertir Checa en un lector de ofertas **confiable para tomar decisiones reales**, no simplemente en un lector que detecte más ofertas.

Checa debe:
- Leer la oferta correcta y asociar todos sus datos a la misma tarjeta.
- No inventar ni mezclar tarifa, kilómetros o minutos.
- Evitar duplicados sin descartar ofertas nuevas legítimas.
- Clasificar rentabilidad únicamente cuando los datos sean suficientes y fiables.
- Abstenerse de mostrar un semáforo cuando no pueda identificar inequívocamente la oferta.
- Permitir investigar errores con evidencia y demostrar que cada actualización no introduce regresiones.

**Criterio rector:** priorizar la exactitud, trazabilidad y seguridad de las decisiones por encima del volumen de lecturas o de nuevas funciones.

## Orden maestro obligatorio

El orden de ejecución y aceptación es **E0 → E1 → E2 → E3 → E4 → E5 → E6 → E7**. No se debe saltar una entrega ni declararla terminada por tener código preparado: debe cumplir sus criterios de aceptación y recibir aprobación explícita.

| Entrega | Resultado que debe quedar | Estado al 15-11-2026 |
|---|---|---|
| **E0** | Banco histórico 10.77 respaldado y comprobado. | **BLOQUEADA.** Procedimiento E0.3 (D1–D5 y H1–H8 corregidos; 84 pruebas con servidor simulado). **No existe respaldo real**, ni copias, ni restauración, ni inventario reconciliado (`respaldo-e0/INVENTARIO.md`). Lo ejecuta el propietario. **Las capturas expiran desde el 20-11-2026.** |
| **E1** | Migraciones y esquema de auditoría preparados. | **Abierta.** `auditoria/sql/001` (+ reversión) probado en PostgreSQL 16.15 y 17.12 locales. No aplicado ni validado en Supabase. No avanza hasta aceptar E0. |
| **E2** | Servidor compatible con los nuevos registros. | **Abierta.** `002` + `registrar-lectura` (contrato v1 con lista blanca de líneas). Probado en local; sin Deno ni despliegue. |
| **E3** | Android 10.81 con trazabilidad completa. | **Abierta.** Núcleo probado en JVM (17 pruebas); redacción corregida (ya no salen direcciones). **Sin compilación Android real, sin teléfono, sin aviso de privacidad.** Apagado por defecto. |
| **E4** | Veredictos humanos e IA. | **BLOQUEADA.** Esquema y herramientas probados; **0 etiquetas humanas** (necesita las capturas de E0 y dos revisores). Banco: 0 humanos, 45 circulares. |
| **E5** | Reproducción histórica y pruebas automáticas. | **Abierta.** Reproducción determinista y evaluación ciega re-ejecutadas (319/365 contra etiquetas **heredadas**); 5/5 mutaciones. Sin referencia humana no mide precisión. |
| **E6** | Checa Admin MVP. | **Abierta.** `004` + `admin/` probados en local (cliente simulado). No desplegado. |
| **E7** | Dashboard completo y métricas confiables. | **Abierta.** `005` probado en 16 y 17; métricas recalculadas de forma independiente. Sin datos reales. |

### Condiciones de cierre por entrega

Para cerrar cualquier E:
1. Entregar el diff completo y la lista de archivos afectados.
2. Mostrar pruebas ejecutadas, comandos, entorno y resultados reproducibles.
3. Documentar limitaciones, riesgos y pruebas que falten en dispositivos o servicios reales.
4. Confirmar que no se rompieron comportamientos previamente correctos.
5. Verificar que se respetaron las dependencias de entregas anteriores.
6. Obtener aprobación explícita antes de fusionar, desplegar, publicar o modificar producción.

## Línea complementaria: correcciones del lector L1–L4

L1–L4 **no sustituyen ni reordenan** el plan maestro E0–E7. Son cambios técnicos separados, que deben coordinarse con las entregas correspondientes y publicarse en releases independientes.

| Línea | Objetivo | Estado |
|---|---|---|
| **L1** | Corregir lectura Uber usando `tarifa ÷ $/km est.` con validaciones. | **No aprobado para integración.** Revisión cruzada L1.1/L1.2 completada; los +7 casos provienen de etiquetas, no del parser. Etiquetas sin captura original; pruebas Android pendientes. Rama/commits intactos, sin push ni merge. |
| **L2** | Vincular cada imagen al fotograma exacto de su lectura. | No iniciada. Depende de la instrumentación y de la evidencia histórica según el plan técnico. |
| **L3** | Deduplicar sin descartar ofertas nuevas legítimas. | No iniciada. |
| **L4** | Manejar DiDi con varias tarjetas: si no se identifica inequívocamente la tarjeta del botón «Aceptar», mostrar «Varias ofertas detectadas» sin semáforo, alerta, historial ni revisión; nunca mezclar tarjetas. | No iniciada. |

Reglas ya acordadas:
- DiDi con varias tarjetas ambiguas: abstenerse de clasificar y no mezclar datos.
- La corrección Uber debe tener validaciones y regresión por caso; no se publica sin pruebas en Android.
- Una oferta nueva legítima debe generar su propio pop-up; la deduplicación no debe ocultarla.
- No mezclar instrumentación E3 y cambios del parser en un mismo release.

## Estado de trabajos locales reportados por Claude

Estos commits fueron reportados como existentes **solo en el contenedor local de Claude**; no son ramas remotas de GitHub y no deben asumirse disponibles en este repositorio:

| Trabajo local | Rama reportada | Commit(s) reportado(s) | Estado de revisión |
|---|---|---|---|
| E1–E2 | `trabajo/e1-e2-esquema` | `2d46e07` | Pendiente |
| E5 | `trabajo/e5-reproduccion` | `a5b4ac8` | Infraestructura revisada; integración/cierre pendiente |
| L1 | `trabajo/l1-uber-est` | `251a8e6`, `37db333` | Revisión completada; **no aprobado para integración**. Sin cambios durante la revisión; mantener local e intacto. |
| E4 | `trabajo/e4-veredictos` | `652b5eb` | Pendiente |

No subir estas ramas ni abrir PRs hasta recibir autorización expresa y comprobar los efectos de workflows. No fusionar nada a `main` sin autorización.

## Prioridad inmediata: E0.1

El respaldo debe ejecutarse en el equipo del propietario usando su propia llave `service_role`. La llave nunca debe compartirse por chat ni entregarse a Claude.

Artefactos que se entregan para revisión, sin imágenes ni secretos:
- `resumen.json`
- `manifiesto.csv`
- `VERIFICACION.json`

La verificación debe confirmar, como mínimo:
- 738 objetos o más, o el conteo actualizado al momento de ejecutar.
- Los 251 IDs históricos requeridos presentes.
- Hashes íntegros y coincidentes con el manifiesto.
- Conteos de filas iguales o superiores a los de la consulta de referencia, ajustados al momento de ejecución.
- Huella del conjunto comparada con la huella tomada en ese mismo momento.

**E0 no se considera terminado** hasta verificar los artefactos, conservar dos copias cifradas independientes y completar una prueba de restauración en la copia B. La huella anterior `8d2d5cee…` quedó obsoleta; usar la huella vigente del momento del respaldo.

## Seguridad y límites operativos

Salvo autorización explícita y específica del propietario:
- No modificar producción, Supabase, políticas, retención ni credenciales.
- No aplicar migraciones.
- No fusionar a `main`, abrir PR, desplegar ni publicar APK.
- No rotar ni exponer llaves, PEM, keystore o contraseñas.
- No ejecutar purgas ni crear workflows de retención.
- Mantener separados los cambios de instrumentación y los cambios del lector.
- Preparar y probar en local no equivale a desplegar ni a validar en producción.

Antes de que E3 llegue a producción deben estar aprobados el plan de datos, la redacción, la retención de 30 días y la revisión del aviso de privacidad. La purga programada requiere diseño y autorización aparte.

## Registro de progreso y decisiones

Añadir una entrada nueva al principio de esta sección por cada revisión o cambio. Registrar hechos comprobados y separar claramente lo reportado por Claude de lo verificado de forma independiente.

### 2026-11-16 — Auditoría de cierre del plan E: BLOQUEADO en E0
- **Estado anterior:** E0–E7 «implementados y probados en local» (commit `7dc25b9`, pausado para revisión).
- **Trabajo realizado:** recuperación verificada del bundle (commit y árbol exactos); auditoría de E0–E7; 17 defectos corregidos con regresión (H1–H8 en E0; A1–A7 en E1–E7; logs completos). Los más graves: `verificar.py`/`empaquetar` leían o empaquetaban archivos de fuera del respaldo; la copia A nunca se comprobaba; **la redacción de E3 dejaba salir direcciones**; un revisor podía valer por dos. Rama local `auditoria/cierre-plan-e`. Sin push.
- **Evidencia y pruebas:** 233 pruebas únicas OK (84 E0, 89 SQL/Python en PostgreSQL 16.15 **y** 17.12, 34 Node, 17 Kotlin/JVM, 9 Playwright); mutaciones E0 y 5/5 del lector; logs completos en `evidencias/cierre/pruebas/`; auditoría estática de seguridad sin hallazgos críticos en el cambio.
- **Limitaciones / riesgos:** sin respaldo real; sin compilación Android (aceptar licencias del SDK fue denegado); sin Supabase real; 0 etiquetas humanas; hallazgo X1: en producción ya se envían líneas de DiDi con dirección (`Confirmador.filtrar`); un merge a `main` publicaría un release (`auditoria/WORKFLOWS.md`).
- **Decisión del propietario:** pendiente. Lista única de acciones externas en `EXPEDIENTE-E0-E7.md` § 0.5.
- **Siguiente paso autorizado:** ninguno nuevo. Prioridad: el propietario ejecuta E0.
- **Cambios en producción / main / Supabase:** ninguno. No se consultó Supabase en esta auditoría.

### 2026-11-15 — E0.2 a E7: implementados y probados en local (ninguno cerrado)
- **Estado anterior:** E0.1 preparado con D1–D5 detectados; E1–E7 sin código en GitHub (las ramas locales reportadas no existen).
- **Trabajo realizado:** E0.2 (D1–D5 con regresiones); E1 esquema `auditoria`; E2 `registrar-lectura`; E3 trazabilidad 10.81 apagada; E4 veredictos y etiquetado humano; E5 reproducción y evaluación ciega; E6 Admin; E7 métricas. Rama local `claude/checa-e0-e7-complete-z2iimn`, commits `b4fdedf`…`751e7fa` (+ expediente). Sin push.
- **Evidencia y pruebas:** 199 pruebas OK (`herramientas/probar_todo.sh`, `evidencias/`); consulta de solo lectura a Supabase para comparar ids (sin descargar nada).
- **Limitaciones / riesgos:** Postgres 16 (no 17); sin SDK Android ni teléfono; sin etiquetas humanas; banco contaminado y con 45 casos circulares; un merge a `main` dispararía `build.yml` (release directo), `play-compilar.yml` y `supabase.yml`.
- **Decisión del propietario:** pendiente (respaldo E0, push, aplicar migraciones, privacidad de E3, etiquetado humano).
- **Siguiente paso autorizado:** ninguno; entrega a auditoría independiente con `EXPEDIENTE-E0-E7.md`.
- **Cambios en producción / main / Supabase:** ninguno (solo una consulta `select` de ids y conteos).

### 2026-11-15 — E0.1: procedimiento de respaldo preparado y probado localmente (sin respaldo real)
- **Estado anterior:** E0.1 pendiente; se hablaba de «scripts existentes».
- **Hallazgo:** no existe ningún script de respaldo en `main`, en `docs/seguimiento-maestro` ni en el contenedor de esta sesión; las ramas `trabajo/*` citadas en este archivo tampoco existen aquí. No hubo nada que auditar: se escribió el procedimiento desde cero (`respaldo-e0/`). El único método previo documentado (`docs/banco-lector.md` §2: función temporal desplegada con `SERVICE_ROLE`) contradice los límites de E0 y no se usó.
- **Trabajo realizado:** `respaldar.py` (solo lectura, host fijo, sin redirecciones, llave solo por variable de entorno), `verificar.py` (offline), `referencia.sql` (huellas independientes del script), `evidencia_copias.py` (tar determinista, restauración desde la copia B, registro), `verificar_entrega.py` (revisión de los artefactos sanitizados), `README.md` (pasos para el propietario) y 49 pruebas contra un servidor simulado.
- **Evidencia y pruebas:** `python3 -m unittest discover -s pruebas` → 49 OK (Python 3.13, age 1.1.1, gpg 2.4.4). `referencia.sql` ejecutada en PostgreSQL 16 local con tablas simuladas: sus huellas coinciden con las de Python. 7 mutaciones del código (redirecciones, WebP, sha256, ids requeridos, llave anon, paginación, ligado a la copia B): 2 sobrevivieron al principio, se reforzaron esas 2 pruebas y las 7 quedaron detectadas. Detalle en `ENTREGA-E0-REVISION.md`.
- **Limitaciones / riesgos:** nada se probó contra Supabase real (formato de listado, código HTTP de objeto inexistente, eTag, límites del SQL Editor son supuestos); la independencia física de las copias y su conservación son declaraciones del propietario; el contenido de las capturas no se valida; si hay actividad entre el respaldo y la consulta SQL aparecerán advertencias.
- **Decisión del propietario:** pendiente (autorizar push de la rama para que pueda bajar los scripts, y el cuarto artefacto `EVIDENCIA-COPIAS.json`).
- **Siguiente paso autorizado:** ninguno nuevo. El propietario ejecuta el README en su equipo y entrega los artefactos sanitizados. E0 sigue **abierto**.
- **Cambios en producción / main / Supabase:** ninguno. Rama local `trabajo/e0-respaldo`, sin push.

### 2026-11-15 — Revisión cruzada L1.1/L1.2: no aprobado para integración
- Sonnet reportó cuatro combinaciones sobre el banco de 316 casos: main/main = 277/316; main/etiquetas L1 = 284/316; parser L1/etiquetas L1 = 284/316; parser L1/etiquetas main = 277/316.
- La diferencia de +7 se atribuye a L1.1 (cambios de etiquetas). L1.2 (Parser.kt) no cambió el resultado en esos 316 casos. Esto no demuestra que el parser sea correcto en Android ni que las etiquetas nuevas sean verdad de referencia.
- Se reportaron 11 etiquetas modificadas; ninguna cuenta con captura original. La consistencia aritmética tarifa ÷ estimado es evidencia auxiliar, no sustituto de la verificación visual independiente.
- Sonnet reportó cero regresiones entre los 277 casos previamente acertados y dos mejoras adicionales en una muestra de 20 casos extra. Persisten cuatro casos sin lectura.
- Riesgos pendientes: interpretación de miles (por ejemplo, `$1,234/km est.` puede convertirse en 2.34), OCR real, NBSP, confusión de caracteres, punto decimal perdido, latencia, ofertas superiores a 10 km y DiDi en pantalla dividida. No se completaron pruebas en teléfono.
- La revisión se realizó sin modificar código ni ramas. Los commits locales reportados siguen siendo `251a8e6` y `37db333`; no se verificó que estén en GitHub.
- **Decisión:** L1 no aprobado para integración, push, PR, merge ni publicación. Conservar rama y commits intactos. No continuar corrigiendo L1 ni reetiquetar casos sin autorización específica. L1 permanece experimental y separado del orden maestro E0–E7.
- **Prioridad vigente:** E0.1. No adelantar E1–E7 ni L1; esperar los artefactos sanitizados del respaldo para revisión.

### 2026-11-15 — Se fija el orden maestro y el estado de E5
- Se confirma como orden obligatorio: E0 → E1 → E2 → E3 → E4 → E5 → E6 → E7.
- E0.1 sigue pendiente; el respaldo y su restauración son la prioridad inmediata.
- E5 fue revisado: el ajuste de U+00A0 está en el lector del banco, no en el parser de producción. La medición reportada pasó de 271/316 a 277/316; seis casos adicionales cuadran aritméticamente y cero casos previamente acertados fallaron en esa comparación.
- Una prueba de mutación en una copia temporal hizo fallar `--pasan` con código 1, lo que demuestra que detecta esa regresión concreta.
- Limitaciones de E5: no se ejecutó JUnit/Gradle, no se verificó en Android real y las etiquetas todavía pueden contener sesgo o circularidad. Por ello E5 no acredita 277 aciertos reales en producción.
- Se autorizó continuar con la revisión de L1, sin modificar código durante la revisión y sin subir ramas.
- E1/E2 y E4 están preparados según el informe de Claude, pero no revisados ni aprobados.

---

## Plantilla para futuras actualizaciones

### AAAA-MM-DD — Entrega / decisión
- **Estado anterior:**
- **Trabajo realizado:**
- **Evidencia y pruebas:**
- **Limitaciones / riesgos:**
- **Decisión del propietario:**
- **Siguiente paso autorizado:**
- **Cambios en producción / main / Supabase:** ninguno, salvo que se documente expresamente lo contrario.
