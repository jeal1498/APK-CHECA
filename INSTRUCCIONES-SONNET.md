# Instrucciones operativas para Claude Sonnet — Proyecto Checa

**Repositorio:** `jeal1498/APK-CHECA`  
**Documento rector:** `SEGUIMIENTO-MAESTRO.md`  
**Orden obligatorio:** E0 → E1 → E2 → E3 → E4 → E5 → E6 → E7  
**Objetivo:** construir un lector de ofertas confiable para decisiones reales de conductores.

Este documento define cómo debe trabajar Sonnet, qué debe entregar y cuándo debe detenerse. Si alguna instrucción de este archivo contradice una instrucción posterior explícita del propietario, prevalece la instrucción posterior únicamente para el alcance que autorice. En todo lo demás, estas reglas siguen vigentes.

---

## 1. Misión y criterio de éxito

La misión no es aumentar el número de ofertas leídas, desarrollar un dashboard por sí mismo ni acumular métricas de coincidencia. La misión es que Checa interprete correctamente **la oferta correcta**, con datos que pertenecen a esa misma oferta, y que se abstenga de clasificar cuando no exista evidencia suficiente.

El resultado final debe permitir demostrar, con evidencia reproducible, que:
- Se identificó la tarjeta/oferta correcta.
- Tarifa, kilómetros y minutos pertenecen a esa misma oferta.
- Los datos no se inventaron ni se mezclaron entre tarjetas.
- Los duplicados se controlan sin ocultar ofertas nuevas legítimas.
- El semáforo se calcula solo con datos válidos y suficientes.
- Cuando hay ambigüedad, Checa se abstiene de clasificar.
- Los errores pueden reconstruirse e investigarse con trazabilidad.
- Las métricas distinguen precisión real, cobertura, abstenciones, falsos positivos, falsos negativos y casos sin veredicto.
- Cada versión demuestra que no introduce regresiones conocidas.

**No presentar coincidencia entre dos lectores/IA como exactitud real.** La verdad de referencia debe provenir de evidencia independiente y, cuando corresponda, verificación humana sobre la captura original.

## 2. Orden de ejecución: no negociable

Trabaja y solicita aceptación en este orden exacto:

1. **E0 — Banco histórico 10.77 respaldado y comprobado.**
2. **E1 — Migraciones y esquema de auditoría preparados.**
3. **E2 — Servidor compatible con los nuevos registros.**
4. **E3 — Android 10.81 con trazabilidad completa.**
5. **E4 — Veredictos humanos e IA.**
6. **E5 — Reproducción histórica y pruebas automáticas.**
7. **E6 — Checa Admin MVP.**
8. **E7 — Dashboard completo y métricas confiables.**

No cambies el orden, no saltes una etapa y no declares una entrega terminada porque el código compile o porque exista una rama. Cada entrega requiere evidencia, revisión independiente de ChatGPT y aprobación explícita del propietario.

### Regla de parada por entrega

Trabaja únicamente en la entrega actualmente autorizada. Al terminar:
1. Detén cambios y pruebas que puedan modificar archivos/datos.
2. Prepara el paquete de revisión descrito en la sección 4.
3. Actualiza `SEGUIMIENTO-MAESTRO.md` con hechos, pruebas, limitaciones y estado; no marques la entrega como aprobada por tu cuenta.
4. Informa a ChatGPT/propietario y espera su revisión.
5. No empieces la siguiente entrega hasta recibir autorización explícita.

Se puede hacer análisis de dependencias de etapas futuras sin implementarlas. No se permite adelantar código, migraciones, integraciones ni despliegues de etapas posteriores.

## 3. Límites permanentes de seguridad y autorización

Sin autorización explícita, específica y vigente del propietario, está prohibido:
- Ejecutar cambios en producción o en Supabase.
- Aplicar migraciones, cambiar RLS, políticas, configuración, retención o Storage.
- Fusionar a `main`, abrir PR, hacer push de ramas, publicar releases o APK.
- Desplegar funciones, servidor, Android, web, Admin o dashboard.
- Rotar, crear, revelar o copiar credenciales, llaves, PEM, keystore o contraseñas.
- Ejecutar borrados, purgas, restauraciones sobre datos originales o tareas destructivas.
- Crear o activar workflows, cron jobs o automatizaciones.
- Cambiar licencias, firmas, pagos o mecanismo de distribución.
- Mezclar en un mismo release instrumentación/trazabilidad y cambios del parser.

Reglas adicionales:
- Nunca solicites ni recibas la llave `service_role`. Solo el propietario la usa en su equipo.
- Nunca incluyas secretos, tokens, datos personales, capturas privadas ni credenciales en commits, logs, reportes o mensajes.
- Usa datos sintéticos para pruebas de esquema y servidor.
- Los archivos temporales, mutaciones y experimentos deben hacerse en copias aisladas, nunca sobre el único original.
- No afirmes que algo está probado en Supabase/Android/producción si solo se probó en mocks, Postgres local, JVM o emulador.
- No infieras que una rama existe en GitHub porque exista en tu contenedor local.
- No hagas push para “respaldar” sin permiso. Antes de cualquier push autorizado, inspecciona los workflows y explica exactamente cuáles podrían dispararse.
- Si detectas un secreto o exposición, detente, no lo reproduzcas y reporta el tipo de hallazgo y su ubicación de forma segura.

## 4. Paquete obligatorio de revisión para cada entrega

Cada entrega debe producir un informe `ENTREGA-EXX-REVISION.md` (o nombre equivalente acordado) con:

1. **Alcance:** qué se autorizó y qué queda expresamente fuera.
2. **Estado:** preparado, probado localmente, probado en entorno integrado, probado en dispositivo/servicio real, pendiente de revisión o bloqueado. No usar “listo” sin calificador.
3. **Archivos:** lista completa de archivos creados, modificados y eliminados.
4. **Diff:** diff completo o referencia accesible a commit/patch exacto; resumen no sustituye el diff.
5. **Commits y ramas:** nombre de rama, SHA completo, base exacta y confirmación de si es local o remota.
6. **Pruebas:** comando exacto, entorno/versiones, resultado, conteos y logs relevantes sin secretos.
7. **Regresión:** casos que antes pasaban y ahora fallan; casos que antes fallaban y ahora pasan; cambios de etiquetas esperadas; falsos positivos/negativos y abstenciones.
8. **Seguridad/privacidad:** permisos, RLS, datos sensibles, retención, redacción y superficies de exposición que se revisaron.
9. **Limitaciones:** qué no se probó, qué depende del propietario, qué requiere dispositivo o servicio real.
10. **Riesgos y reversión:** impacto potencial, cómo revertir de forma segura y qué no debe revertirse.
11. **Puerta de aceptación:** lista de criterios cumplidos/no cumplidos y decisión solicitada.

No presentes únicamente una narración. Incluye evidencia reproducible y resultados exactos. Separa claramente:
- hecho verificado por ejecución;
- resultado reportado por otra persona/herramienta;
- inferencia;
- supuesto aún no probado.

ChatGPT revisará el diff, el diseño, las pruebas y los riesgos de forma independiente. Si el material no está disponible en GitHub, entrega el patch/diff exacto y los archivos relevantes de forma que pueda revisarse; no declares que ChatGPT ya lo verificó.

## 5. Definición de cada entrega

### E0 — Banco histórico 10.77 respaldado y comprobado

**Resultado:** rescatar las capturas y datos históricos necesarios antes de que empiece la eliminación automática, y demostrar integridad y restaurabilidad.

Responsabilidades de Sonnet:
- Mantener y documentar el procedimiento y los scripts de respaldo/verificación.
- Auditar los scripts en local y con servidor simulado; explicar qué validan y qué no.
- Dar al propietario instrucciones paso a paso para ejecutar el respaldo en su propio equipo.
- No ejecutar el respaldo real ni pedir la llave `service_role`.
- Recibir únicamente los artefactos sanitizados que el propietario decida compartir: `resumen.json`, `manifiesto.csv` y `VERIFICACION.json`. No solicitar las imágenes ni secretos.
- Verificar conteos, IDs requeridos, hashes, huella y consistencia de filas contra la consulta de referencia tomada en el mismo momento.
- Confirmar que existen dos copias cifradas independientes y que se hizo una restauración de prueba desde la copia B, sin alterar el original.
- Documentar discrepancias y detener el cierre si falta cualquier comprobación.

Criterios de aceptación:
- Respaldo íntegro de todos los objetos requeridos del banco histórico 10.77.
- Los IDs requeridos están presentes; el número esperado se compara con la lista de IDs vigente y no con un conteo obsoleto.
- Manifiesto y hashes verifican correctamente.
- Los conteos de tablas son iguales o superiores a los valores de referencia ajustados a la hora de la extracción; explicar cualquier diferencia.
- Huella del conjunto corresponde a la consulta del momento de respaldo.
- Dos copias cifradas independientes bajo control del propietario.
- Restauración de prueba satisfactoria desde copia B.
- El propietario confirma que conserva las copias.

**No cerrar E0** por tener scripts preparados, pruebas simuladas aprobadas o un manifiesto generado. E0 solo se cierra después de verificar respaldo real, copias cifradas y restauración.

### E1 — Migraciones y esquema de auditoría preparados

**Resultado:** diseñar y preparar el esquema necesario para guardar eventos de lectura y auditoría sin romper datos ni permisos existentes.

Responsabilidades:
- Revisar el esquema actual y documentar las tablas, relaciones, índices, restricciones y políticas existentes antes de proponer cambios.
- Preparar migraciones pequeñas, idempotentes y ordenadas; no aplicarlas.
- Definir claves, relaciones, estados, timestamps, expiración y trazabilidad.
- Definir RLS y privilegios mínimos. Probar explícitamente que roles públicos/no autenticados no acceden a datos privados.
- Definir qué datos se guardan, cuáles se redactan y por qué; minimizar datos personales y evitar almacenar contenido innecesario.
- Definir retención y purga, pero no programar ni ejecutar purgas sin aprobación.
- Probar migraciones repetidas, integridad, restricciones, cascadas, índices, vistas y compatibilidad con datos previos en un entorno local aislado.
- Documentar compatibilidad real pendiente: Supabase usa PostgreSQL 17; una prueba en PostgreSQL 16 no equivale a validación en el destino.
- Revisar especialmente workflows: cualquier cambio bajo `supabase/**` puede activar automatizaciones al llegar a `main`. No hacer push/merge.

Criterios de aceptación:
- Esquema documentado y consistente con los eventos que Android/servidor realmente producirán.
- Migraciones idempotentes y reversión documentada.
- Sin pérdida ni modificación involuntaria de datos existentes en pruebas.
- RLS/permisos probados con roles representativos.
- Pruebas de privacidad y retención aprobadas.
- No se aplicó nada en Supabase real.

### E2 — Servidor compatible con los nuevos registros

**Resultado:** preparar la función/servidor para recibir los nuevos registros, manteniendo compatibilidad con la versión antigua de Android.

Responsabilidades:
- Definir y documentar contrato de entrada/salida, campos requeridos/opcionales, tipos, límites y versiones.
- Validar datos en servidor; nunca confiar solo en validación Android.
- Mantener compatibilidad con payloads de versiones anteriores durante la transición.
- Manejar campos faltantes, inválidos, duplicados, payloads grandes, reintentos, timeouts y errores sin corromper registros.
- Establecer autenticación/autorización y privilegio mínimo; no exponer service-role al cliente.
- Hacer escrituras idempotentes cuando corresponda y evitar duplicados accidentales.
- Añadir pruebas unitarias y de integración con stubs/simulaciones; documentar qué no se ha probado contra Supabase real.
- Preparar la integración con el entrypoint real, pero no desplegar ni cambiar funciones en producción.

Criterios de aceptación:
- Contrato versionado, documentado y validado.
- Payload antiguo aceptado sin regresiones.
- Payload nuevo almacenado correctamente en entorno de prueba.
- Casos inválidos rechazados de forma segura.
- Errores y reintentos no duplican ni pierden registros.
- Pruebas reproducibles y sin secretos.
- Ningún despliegue a producción.

### E3 — Android 10.81 con trazabilidad completa

**Resultado:** incorporar instrumentación en Android para reconstruir qué ocurrió en cada lectura y asociarla a la evidencia correcta, sin cambiar la lógica del parser en este release.

Responsabilidades:
- Diseñar el identificador único de evento/lectura y correlacionarlo con captura/fotograma, app de origen, versión, resultado del parser y revisión.
- Asegurar que la imagen corresponda al instante exacto de esa lectura; no asociarla por “última captura reciente” ni por ventanas temporales ambiguas.
- Registrar etapas y motivos de éxito, fallo, descarte, timeout, duplicado y abstención sin capturar más datos personales de los necesarios.
- Añadir redacción antes de transmitir/guardar; minimizar datos sensibles y limitar accesos.
- Gestionar cola offline, reintentos, límites, conectividad, tamaño de imagen y fallos de envío sin afectar el funcionamiento principal.
- Mantener el parser y la clasificación funcionalmente iguales a la versión base. Añadir una prueba/guardia de CI que detecte cambios no autorizados en archivos del parser acordados.
- Medir latencia y consumo; comprobar que la instrumentación no perjudica la experiencia de conducción.
- Preparar pruebas en dispositivos reales y matriz de versiones/fabricantes.
- Obtener revisión del aviso de privacidad y aprobación formal del plan de datos antes de cualquier despliegue.
- Diseñar la retención de 30 días. No crear workflow de purga ni activar pg_cron sin autorización expresa.

Criterios de aceptación:
- Cada evento de lectura se puede correlacionar inequívocamente con su captura exacta.
- Trazabilidad completa de resultados y motivos.
- Sin datos personales innecesarios y con redacción verificada.
- El comportamiento del parser no cambió en E3.
- Pruebas de cola, reintentos, offline, límites, privacidad, latencia y consumo aprobadas.
- Pruebas en teléfonos reales documentadas.
- Aviso de privacidad y tratamiento de datos aprobados antes de producción.
- No desplegar hasta autorización expresa.

### E4 — Veredictos humanos e IA

**Resultado:** establecer una fuente de verdad auditable que diferencie revisión humana, revisión IA, desacuerdo e incertidumbre.

Responsabilidades:
- Definir un modelo de veredicto con autor/fuente, fecha, versión del revisor, evidencia y estado.
- Mantener separado el resultado del parser de los veredictos humanos y de IA.
- Establecer precedencia explícita: el veredicto humano validado prevalece sobre el de IA; los conflictos y casos dudosos se conservan como tales, no se fuerzan a una clase.
- Migrar registros históricos de forma reversible y trazable, identificando claramente que son importados/traducidos, no revisados nuevamente.
- No convertir automáticamente las etiquetas heredadas en verdad de referencia.
- Crear guía de etiquetado con ejemplos, criterios, doble revisión y resolución de desacuerdos.
- Medir acuerdo entre revisores por separado de exactitud contra casos verificados.
- Diseñar calibración con una muestra humana suficiente y representativa; documentar selección y sesgo.
- No enviar capturas ni datos sensibles a proveedores IA sin aprobación de privacidad y política de tratamiento.

Criterios de aceptación:
- Cada veredicto conserva origen y trazabilidad.
- Humano validado prevalece sobre IA; incertidumbre no se oculta.
- Migración idempotente y reversible; tabla original intacta.
- Casos históricos no revisados se identifican como tales.
- Guía y pruebas de etiquetado aprobadas.
- Métricas separan acuerdo, exactitud, cobertura y abstenciones.

### E5 — Reproducción histórica y pruebas automáticas

**Resultado:** reproducir casos históricos de forma determinista y detectar regresiones por caso, sin confundir un banco sesgado con precisión real.

Responsabilidades:
- Crear un corredor reproducible que ejecute la versión base y la candidata sobre los mismos casos y entorno.
- Mantener casos positivos, negativos, ambiguos, multi-tarjeta, errores OCR, monedas, decimales, separadores y distintas versiones.
- Congelar etiquetas de referencia con historial, origen y evidencia independiente. Cambiar una etiqueta requiere revisión y justificación explícitas.
- Evitar que una prueba pase solo porque se cambió la respuesta esperada.
- Medir por separado tarifa, km, minutos, identificación de tarjeta, detección, duplicados, clasificación, abstención y latencia.
- Probar mutaciones para demostrar que las pruebas fallan cuando se introduce un error conocido.
- Ejecutar pruebas unitarias/instrumentadas Android con las versiones reales de toolchain.
- Mantener datasets de entrenamiento separados de datasets de evaluación ciega.
- Informar métricas por app (Uber/DiDi), versión, fabricante y condiciones, con intervalos y tamaño de muestra cuando sea apropiado.
- Documentar casos no medibles y sesgos de selección.

Criterios de aceptación:
- Reproducción determinista con comandos documentados.
- Regresión por caso que falla ante mutaciones relevantes.
- Ningún caso protegido se rompe sin explicación y aprobación.
- Etiquetas versionadas, justificadas y auditables.
- Evaluación ciega y muestra humana independiente.
- Resultados de banco no se presentan como precisión de producción sin validación representativa.

### E6 — Checa Admin MVP

**Resultado:** una herramienta administrativa mínima para inspeccionar el estado de la lectura y resolver problemas, basada en los registros y veredictos ya validados.

Alcance inicial:
- **Inicio:** salud del sistema, volumen, estado de ingesta, colas/errores operativos y alertas de calidad.
- **Errores:** listado filtrable de errores de lectura, estado de revisión, versión/app y acceso controlado a evidencia cuando esté permitido.
- **Ofertas:** listado de ofertas con datos parseados, origen, trazabilidad, estado de veredicto y posibilidad de comparar resultado del parser con referencia autorizada.

Responsabilidades:
- Definir roles y permisos administrativos de mínimo privilegio.
- Aplicar filtros por fecha, app, versión, estado, tipo de error y veredicto.
- Proteger capturas y datos personales; registrar accesos administrativos.
- Evitar mostrar secretos o datos que no sean necesarios.
- Diseñar paginación, límites y consultas eficientes.
- No incluir acciones destructivas ni edición de veredictos sin flujo de auditoría y autorización.
- Probar control de acceso, aislamiento entre usuarios, manejo de errores y rendimiento.

Criterios de aceptación:
- Las tres vistas MVP funcionan con datos reales de prueba.
- Acceso restringido y auditado.
- Los datos mostrados coinciden con las fuentes y filtros.
- No hay exposición indebida de capturas ni datos personales.
- Sin operaciones destructivas no autorizadas.

### E7 — Dashboard completo y métricas confiables

**Resultado:** panel de calidad y operación que represente fielmente la confiabilidad de Checa y permita detectar dónde falla.

Responsabilidades:
- Definir cada métrica con fórmula, población, exclusiones, fuente y ventana temporal.
- Distinguir volumen detectado de cobertura real; no inferir ofertas omitidas si no hay un denominador observable.
- Separar precisión, recall/cobertura cuando sea medible, falsos positivos, falsos negativos, abstenciones, duplicados, errores OCR, errores de tarjeta y casos sin veredicto.
- Mostrar segmentación por Uber/DiDi, versión, dispositivo/fabricante y tipo de oferta cuando haya muestra suficiente.
- Mostrar tamaño de muestra y advertir cuando los datos sean insuficientes o sesgados.
- Separar resultados históricos, muestra etiquetada, evaluación ciega y telemetría de producción.
- Evitar promedios que oculten fallos graves en un segmento.
- Incluir tendencias temporales y comparaciones entre releases sin atribuir causalidad sin evidencia.
- Documentar latencia, errores de ingesta, retención y salud del sistema.
- Probar que cada indicador se puede recalcular desde datos fuente.

Criterios de aceptación:
- Definiciones y consultas de métricas auditables.
- Reconciliación entre dashboard y datos fuente.
- No se confunde acuerdo IA/lector con precisión.
- No se muestran métricas de cobertura/recall sin denominador válido.
- Segmentos con muestra pequeña se marcan como insuficientes.
- Acceso y privacidad aprobados.

## 6. Línea complementaria L1–L4

L1–L4 son correcciones del lector y **no cambian el orden maestro E0–E7**. No las uses para saltarte etapas ni mezcles sus cambios con releases de instrumentación.

- **L1 Uber:** evaluar la regla `tarifa ÷ $/km est.` solo cuando el dato exista, sea numérico, positivo, finito y pertenezca inequívocamente a la misma tarjeta. Validar redondeo, separadores, moneda, rangos plausibles y contradicciones con otros datos. No usarla si hay ambigüedad. Requiere regresión por caso y pruebas en Android antes de publicar.
- **L2 Captura exacta:** ligar la imagen al evento/fotograma exacto, no a “la última imagen reciente”.
- **L3 Deduplicación:** deduplicar la misma oferta persistente, pero mostrar una oferta nueva si cambian valores relevantes, reaparece después de salir de pantalla o hay dos apps/ofertas simultáneas. No ocultar ofertas legítimas.
- **L4 DiDi multi-tarjeta:** si no se identifica inequívocamente qué tarjeta corresponde al botón «Aceptar», mostrar «Varias ofertas detectadas» sin semáforo, alerta, historial ni revisión. Nunca combinar datos de tarjetas distintas. El banco debe poder representar el resultado esperado `varias`.

Cada L debe tener rama/release independiente, criterios de aceptación, pruebas y aprobación específica. No avanzar estas líneas salvo autorización del propietario. El trabajo L1 que ya exista debe revisarse como trabajo preparado, no integrarse automáticamente.

## 7. Gestión de ramas, commits y workflows

- Una rama local en el contenedor de Sonnet no equivale a una rama remota ni a un respaldo persistente.
- Antes de crear o modificar ramas, confirmar base y estado limpio; no sobrescribir commits ajenos.
- Mantener una rama por entrega y commits pequeños, descriptivos y revisables.
- No incluir archivos temporales, capturas privadas, secretos ni datasets no autorizados.
- Antes de un push autorizado, inspeccionar todos los workflows y explicar qué eventos/rutas los activan.
- No abrir PR ni fusionar a `main` sin autorización. Un PR puede activar workflows aunque un push a rama no lo haga.
- No ejecutar workflows manuales, releases ni despliegues.
- Si el entorno local puede perderse, advertirlo de inmediato y pedir autorización para un método de respaldo seguro; no subir por iniciativa propia.
- No reescribir historial, forzar pushes ni eliminar ramas.

## 8. Actualización obligatoria del registro

Después de cada trabajo autorizado, actualizar `SEGUIMIENTO-MAESTRO.md` con:
- fecha;
- entrega y subentrega;
- estado exacto (no iniciado / en preparación / probado localmente / revisión pendiente / aprobado / bloqueado / desplegado con autorización);
- archivos, rama y SHA;
- pruebas y resultados;
- riesgos y limitaciones;
- decisiones explícitas del propietario;
- siguiente paso autorizado.

Nunca marques “aprobado”, “cerrado” o “desplegado” si solo lo has preparado o probado localmente. Distingue lo reportado de lo verificado por ChatGPT/propietario.

## 9. Handoff obligatorio a ChatGPT

Al concluir cada entrega, entrega a ChatGPT:
1. El informe de revisión completo.
2. El diff exacto y todos los archivos relevantes.
3. SHA completo de los commits y base de comparación.
4. Comandos de prueba y resultados.
5. Evidencia de criterios cumplidos y pendientes.
6. Riesgos, limitaciones y decisiones que requieren al propietario.

**Después detente.** ChatGPT se encargará de revisar el trabajo de forma independiente y recomendará aprobar, pedir cambios o rechazar. Sonnet no debe autoaprobarse ni comenzar la siguiente entrega hasta recibir la decisión del propietario después de esa revisión.

Si ChatGPT solicita cambios, trabaja solo en esos cambios dentro del alcance autorizado, repite las pruebas afectadas y vuelve a entregar el diff. No ocultes resultados fallidos ni reduzcas pruebas para conseguir un resultado verde.

## 10. Estado inicial conocido al crear estas instrucciones

La información siguiente es el punto de partida reportado al 15 de noviembre de 2026; debe actualizarse con evidencia nueva y no tratarse como estado actual sin verificar:

- E0.1 no se había ejecutado. El propietario debía realizar el respaldo con su llave local y compartir solo los tres artefactos sanitizados indicados en E0.
- Claude reportó ramas locales preparadas para E1/E2, E4, E5 y L1; no estaban subidas a GitHub.
- E5 fue revisado como infraestructura de regresión: la normalización U+00A0 en el lector del banco cambió la medición reportada de 271/316 a 277/316, con seis casos adicionales y cero regresiones en esa comparación. No se ejecutó JUnit/Gradle ni se verificó Android real. Esto no demuestra 277 aciertos reales de producción.
- L1 estaba pendiente de revisión; no se debe modificar ni integrar sin autorización.
- No se debe asumir que algún cambio llegó a `main`, Supabase, producción o APK.

---

## Regla final

**Un solo objetivo, un solo orden y una entrega verificable a la vez: E0 → E1 → E2 → E3 → E4 → E5 → E6 → E7.**

No optimices por velocidad de entrega. Optimiza por evidencia, seguridad, trazabilidad y confiabilidad real de Checa.
