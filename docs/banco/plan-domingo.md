# Plan ejecutable del domingo (versión semanal del lector)

Lo dispara un recordatorio automático cada domingo (14:23, hora de CDMX) en una sesión nueva. Reglas de fondo en `CLAUDE.md` («Plan de mejora semanal»). Eduardo revisa 100 ofertas al día de lunes a sábado; el domingo se mide, se corrige y sale la versión. Si un paso no se puede cumplir, detente y dile a Eduardo qué falta antes de seguir.

## Reglas que no se rompen
- Respuestas cortas, en español (Eduardo escucha el chat). La verdad es su revisión de Admin; **ninguna IA juzga si una lectura es correcta** y no se activa ni se gasta IA.
- **No cambies el lector sin su «adelante»** a la propuesta del paso 5. No modifiques el banco ni las verificaciones de `auditoria`.
- **No publiques en Play Console ni armes el AAB sin su autorización.** Nunca secretos en el repo.
- Un fallo grave (la app se cierra, no abre, no lee nada) no espera al domingo: dilo y arréglalo.

## Paso 0 · Preparar (2 min)
1. `cd` al repo, `git pull origin main`. Lee `CLAUDE.md` y este archivo.
2. Gradle y el SDK: si no hay `~/android-sdk`, instálalo como dice «Entorno y compilación» de `CLAUDE.md`.
3. Dile a Eduardo en una línea que empezó el domingo de versión.

## Paso 1 · Datos de la semana (Supabase, proyecto `gqsadclvftkzwrihfipz`, solo lectura)
Versión vigente = la última de `app/build.gradle.kts` (`versionName`). Ventana = últimos 7 días.

```sql
-- Normales revisadas por versión (vigente por captura) y resultado, con denominador
select c.version, count(*) revisadas,
  count(*) filter (where r.resultado='correcta') correctas,
  count(*) filter (where r.resultado='valores_mal') valores_mal,
  count(*) filter (where r.resultado='falso_positivo' and r.motivo<>'ilegible') falsos_positivos,
  count(*) filter (where r.resultado='falso_positivo' and r.motivo='ilegible') ilegibles
from auditoria.revision_ofertas_vigente r join public.capturas_ok c on c.id=r.captura_ok_id
where r.creado >= now() - interval '7 days' group by 1 order by 1;

-- Dudosas con veredicto humano
select e.version, v.clase, count(*) from auditoria.veredicto_vigente v
join public.errores_lector e on e.id=v.error_lector_id
where v.estado='referencia_humana' and v.creado >= now() - interval '7 days' group by 1,2 order by 1,2;
```
Evaluables = revisadas − ilegibles. Meta de muestra: unas 600 en la semana; con menos de 30 por grupo avisa «muestra insuficiente» y no des el %.

## Paso 2 · Integridad (si algo falla, repórtalo; no lo arregles en los datos)
```sql
-- ¿Las capturas están alineadas? (la imagen viaja con la petición = fotograma exacto)
select r2.imagenes, (c.ia->>'sin_ia') is not null as sin_ia, count(*)
from auditoria.revision_ofertas_vigente v join public.capturas_ok c on c.id=v.captura_ok_id
left join public.revisiones r2 on r2.fotograma_id=c.fotograma_id
where v.creado >= now() - interval '7 days' group by 1,2 order by 1,2;
```
Desde la 10.86 la captura que sube el teléfono es la del fotograma de la lectura. Si siguen apareciendo casos desfasados (el pop-up o la oferta de la captura no corresponden a las líneas), clasifícalos `DESFASE_TEMPORAL_CAPTURA_NO_EVALUABLE` y no los cuentes como errores del Parser.

## Paso 3 · Métricas (siempre con su denominador: `90 % (90 de 100 lecturas)`)
Compara la versión vigente contra la anterior (la 10.85 tuvo 89.9 % = 408 de 454 en lectura completa).
```sql
select c.nivel, c.app, count(*) n, count(*) filter (where r.resultado='correcta') correctas,
  count(*) filter (where r.resultado='falso_positivo' and r.motivo<>'ilegible') fp
from auditoria.revision_ofertas_vigente r join public.capturas_ok c on c.id=r.captura_ok_id
where c.version = '<versionName>' and not (r.resultado='falso_positivo' and r.motivo='ilegible')
group by rollup(c.nivel, c.app) order by 1,2;
```
Reporta: lectura completa, falsos positivos, por color, por app, por tarjetas (`c.tarjetas`) y por orientación, y las etapas (Admin › Hoy: `ocr` y `parser`, tiempo y motivo). Metas: lectura completa ≥ 93 %, falsos positivos < 1 %.

## Paso 4 · Agrupar errores
1. Lista los errores (`valores_mal`, `falso_positivo`, dudosas `oferta` con lectura distinta): líneas (`capturas_ok.lineas` / `errores_lector.lineas`), `local`, y lo que Eduardo dio (`tarifa`, `km`, `min`).
2. Reproduce cada uno con el Parser actual (`docs/banco/calificar.sh` compila `Parser.kt`; escribe un arnés pequeño en el directorio temporal de la sesión, nunca en el repo). Las líneas guardadas son más limpias que las del Parser: para un error de tarjetas usa las líneas completas del diagnóstico que Eduardo comparta o reconstruidas de la captura.
3. Agrupa por causa, de más a menos frecuente, con gravedad, 2 o 3 IDs de ejemplo (N = normal, D = dudosa) y causa probable (separa evidencia de hipótesis).
4. Prioridad: falsos positivos → ofertas no detectadas → otra tarjeta o monto ajeno → km → min → tarifa → menores.
5. **Solo entra a la versión una causa con 3 o más casos reproducibles.** Lo demás queda «sin evidencia».

## Paso 5 · Propuesta y «adelante»
Presenta a Eduardo una tabla corta: causa · casos · solución propuesta · riesgo. **Espera su «adelante».** Si no hay ninguna causa demostrada, dilo: ese domingo no sale versión de lector.

## Paso 6 · Implementar lo aprobado
1. `docs/banco/calificar.sh` ANTES; guarda el resultado y la lista de fallas.
2. Cambios pequeños en `Parser.kt` (o donde sea), solo lo necesario. No toques Uber salvo que la causa lo exija y Eduardo lo haya aprobado.
3. Casos nuevos en `app/src/test/resources/ofertas/` (líneas reales o completas, no las filtradas).
4. `calificar.sh` DESPUÉS; compara. Reporta cada caso que empeore, aunque sea uno. Si el piso del banco sube, actualiza `app/src/test/resources/banco/minimo.txt`.
5. `herramientas/parser.sha256`: `sha256sum` del archivo cambiado y pégalo.
6. `ANDROID_HOME=$HOME/android-sdk gradle :app:cleanTestPlayDebugUnitTest :app:testPlayDebugUnitTest :app:assemblePlayDebug :app:compileDirectoDebugKotlin`: todo verde.

## Paso 7 · Versión
1. `app/build.gradle.kts`: `versionCode` + 1 y `versionName` (10.86 → 10.87 …); `Traza.VERSION_PARSER` igual.
2. `Novedades.kt`: bloque arriba, en lenguaje de conductor (nada técnico, nada de IA).
3. `docs/historial-versiones.md` (entrada nueva) y `CLAUDE.md` (versión, banco, qué quedó sin corregir).
4. Commits pequeños a `main` y a la rama de la sesión (`git push origin HEAD:main`; `git push -u origin HEAD`). Cierra cada commit con las líneas de atribución de la sesión.
5. Busca la corrida de «Compilar flavor play» (`play-compilar.yml`) del último commit y mándale a Eduardo el link cuando esté en verde. Si falla, arréglalo antes de avisar.

## Paso 8 · Informe (máximo 5 líneas)
Métricas de la semana contra la anterior (con denominadores) · causas corregidas y con qué casos · lo que quedó sin evidencia · link del APK · qué necesitas de Eduardo (pruebas en teléfono, revisiones que faltan).
