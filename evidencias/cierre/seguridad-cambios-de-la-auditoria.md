# Auditoría estática de seguridad

Base `7dc25b95c2ca` → `9d05fd98dc35` · 57 archivos revisados (18 creados, 39 modificados, 0 eliminados).

## Hallazgos críticos
- Ninguno.

## Mensajes de commit con URL de sesión
- Ninguno.

## Dominios externos citados (código, pruebas y documentación)
- http://127.0.0.1 — en: auditoria, respaldo-e0
- http://gqsadclvftkzwrihfipz.supabase.co — en: respaldo-e0
- http://localhost — en: respaldo-e0
- http://x — en: auditoria
- https://age-encryption.org — en: respaldo-e0
- https://cdn.jsdelivr.net — en: admin
- https://claude.ai — en: respaldo-e0
- https://ejemplo.supabase.co — en: respaldo-e0
- https://evil.com — en: respaldo-e0
- https://x.y — en: app, auditoria

## Correos presentes
- correo@x.com — 1 archivo(s)
- juan@correo.com — 2 archivo(s)
- noreply@anthropic.com — 1 archivo(s)

## Cambios en Gradle (dependencias y versión)
- Sin cambios.

## AndroidManifest / ProGuard
- Sin cambios (ningún permiso nuevo).

## Workflows
- Sin cambios en `.github/`.

## Notas (revisadas a mano)
- SQL dinámico auditoria/sql/004_admin.sql:218: execute format('revoke all on function %s from public, anon', fn);
- SQL dinámico auditoria/sql/004_admin.sql:219: execute format('grant execute on function %s to authenticated', fn);
