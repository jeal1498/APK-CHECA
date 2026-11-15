# Seguimiento maestro de Checa

**Última actualización:** 15 de noviembre de 2026  
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
| **E0** | Banco histórico 10.77 respaldado y comprobado. | **Pendiente y urgente.** E0.1 aún no se ejecuta; falta respaldo local, verificación, dos copias cifradas y prueba de restauración de la copia B. |
| **E1** | Migraciones y esquema de auditoría preparados. | Preparación reportada en rama local de Claude; pendiente de revisión y aceptación. No está en GitHub ni aplicada. |
| **E2** | Servidor compatible con los nuevos registros. | Contrato TypeScript y pruebas preparados en la misma rama local de E1; aún no conectado a `index.ts`, pendiente de revisión. |
| **E3** | Android 10.81 con trazabilidad completa. | No iniciado. Requiere definir y aprobar tratamiento de datos, redacción, retención y revisión del aviso de privacidad antes de producción. |
| **E4** | Veredictos humanos e IA. | Preparación reportada en rama local; pendiente de revisión. La traducción de 309 veredictos no equivale a validación caso por caso. |
| **E5** | Reproducción histórica y pruebas automáticas. | Infraestructura revisada y aprobada como mecanismo de regresión, no como prueba de precisión real en Android. La integración y aceptación formal dentro del orden maestro siguen pendientes hasta completar E3 y E4. |
| **E6** | Checa Admin MVP. | No iniciado. |
| **E7** | Dashboard completo y métricas confiables. | No iniciado. |

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
| **L1** | Corregir lectura Uber usando `tarifa ÷ $/km est.` con validaciones. | Preparada en rama local; revisión pendiente. No fusionada ni publicada. |
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
| L1 | `trabajo/l1-uber-est` | `251a8e6`, `37db333` | Pendiente |
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
