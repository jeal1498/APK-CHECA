# Auditoría estática de seguridad

Base `a7a732fb3fc8` → `9d05fd98dc35` · 163 archivos revisados (154 creados, 9 modificados, 0 eliminados).

## Hallazgos críticos
- Ninguno.

## Mensajes de commit con URL de sesión
- 5abadea75dd1, e6a05ec0b00e (ya publicados antes de esta auditoría; no se reescribe historial)

## Dominios externos citados (código, pruebas y documentación)
- http://127.0.0.1 — en: auditoria, paquete-revision-e0, respaldo-e0
- http://gqsadclvftkzwrihfipz.supabase.co — en: paquete-revision-e0, respaldo-e0
- http://localhost — en: paquete-revision-e0, respaldo-e0
- http://x — en: auditoria
- https://age-encryption.org — en: paquete-revision-e0, respaldo-e0
- https://cdn.jsdelivr.net — en: admin
- https://claude.ai — en: app, respaldo-e0
- https://ejemplo.supabase.co — en: paquete-revision-e0, respaldo-e0
- https://evil.com — en: paquete-revision-e0, respaldo-e0
- https://filippo.io — en: evidencias, paquete-revision-e0
- https://gqsadclvftkzwrihfipz.supabase.co — en: ENTREGA-E0-REVISION.md, admin, app, paquete-revision-e0
- https://jeal1498.github.io — en: app
- https://wa.me — en: app, docs
- https://x.y — en: app, auditoria

## Correos presentes
- correo@x.com — 3 archivo(s)
- juan@correo.com — 2 archivo(s)
- noreply@anthropic.com — 1 archivo(s)
- this@MainActivity.handler — 1 archivo(s)

## Cambios en Gradle (dependencias y versión)
    -        versionCode = 160
    -        versionName = "10.80"
    +        versionCode = 161
    +        versionName = "10.81"

## AndroidManifest / ProGuard
- Sin cambios (ningún permiso nuevo).

## Workflows
- Sin cambios en `.github/`.

## Notas (revisadas a mano)
- llave anon de Supabase (pública por diseño) en app/src/main/java/com/karen/miruta/Cuenta.kt — ya estaba en la base
- SQL dinámico auditoria/sql/004_admin.sql:218: execute format('revoke all on function %s from public, anon', fn);
- SQL dinámico auditoria/sql/004_admin.sql:219: execute format('grant execute on function %s to authenticated', fn);
