# Auditoría estática de seguridad

Base `a7a732fb3fc8` → `d5548f569f8a` · 181 archivos revisados (171 creados, 10 modificados, 0 eliminados).

## Hallazgos críticos
- Ninguno.

## Mensajes de commit con URL de sesión
- 5abadea75dd1, e6a05ec0b00e (ya publicados antes de esta auditoría; no se reescribe historial)

## Dominios externos citados (código, pruebas y documentación)
- http://127.0.0.1 — en: auditoria, evidencias, paquete-revision-e0, respaldo-e0
- http://gqsadclvftkzwrihfipz.supabase.co — en: evidencias, paquete-revision-e0, respaldo-e0
- http://localhost — en: evidencias, paquete-revision-e0, respaldo-e0
- http://x — en: auditoria, evidencias
- https://age-encryption.org — en: evidencias, paquete-revision-e0, respaldo-e0
- https://cdn.jsdelivr.net — en: admin, evidencias
- https://claude.ai — en: app, evidencias, respaldo-e0
- https://ejemplo.supabase.co — en: evidencias, paquete-revision-e0, respaldo-e0
- https://evil.com — en: evidencias, paquete-revision-e0, respaldo-e0
- https://filippo.io — en: evidencias, paquete-revision-e0
- https://gqsadclvftkzwrihfipz.supabase.co — en: ENTREGA-E0-REVISION.md, admin, app, evidencias, paquete-revision-e0
- https://jeal1498.github.io — en: app, evidencias
- https://wa.me — en: app, docs, evidencias
- https://x.y — en: app, auditoria, evidencias

## Correos presentes
- correo@x.com — 4 archivo(s)
- juan@correo.com — 3 archivo(s)
- noreply@anthropic.com — 2 archivo(s)
- this@MainActivity.handler — 2 archivo(s)

## Cambios en Gradle (dependencias y versión)
    -        versionCode = 160
    -        versionName = "10.80"
    +        versionCode = 161
    +        versionName = "10.81"

## AndroidManifest / ProGuard
- Sin cambios (ningún permiso nuevo).

## Workflows
- .github/workflows/build.yml | 2 +-
 1 file changed, 1 insertion(+), 1 deletion(-)

## Notas (revisadas a mano)
- llave anon de Supabase (pública por diseño) en app/src/main/java/com/karen/miruta/Cuenta.kt — ya estaba en la base
- SQL dinámico auditoria/sql/004_admin.sql:218: execute format('revoke all on function %s from public, anon', fn);
- SQL dinámico auditoria/sql/004_admin.sql:219: execute format('grant execute on function %s to authenticated', fn);
