# Auditoría estática de seguridad

Base `7dc25b95c2ca` → `779d0923f980` · 77 archivos revisados (35 creados, 42 modificados, 0 eliminados).

## Hallazgos críticos
- Ninguno.

## Mensajes de commit con URL de sesión
- Ninguno.

## Dominios externos citados (código, pruebas y documentación)
- http://127.0.0.1 — en: auditoria, evidencias, respaldo-e0
- http://gqsadclvftkzwrihfipz.supabase.co — en: evidencias, respaldo-e0
- http://localhost — en: evidencias, respaldo-e0
- http://x — en: auditoria, evidencias
- https://age-encryption.org — en: evidencias, respaldo-e0
- https://cdn.jsdelivr.net — en: admin, evidencias
- https://claude.ai — en: evidencias, respaldo-e0
- https://ejemplo.supabase.co — en: evidencias, respaldo-e0
- https://evil.com — en: evidencias, respaldo-e0
- https://filippo.io — en: evidencias
- https://gqsadclvftkzwrihfipz.supabase.co — en: evidencias
- https://jeal1498.github.io — en: evidencias
- https://wa.me — en: evidencias
- https://x.y — en: app, auditoria, evidencias

## Correos presentes
- correo@x.com — 2 archivo(s)
- juan@correo.com — 3 archivo(s)
- noreply@anthropic.com — 2 archivo(s)
- this@MainActivity.handler — 1 archivo(s)

## Cambios en Gradle (dependencias y versión)
- Sin cambios.

## AndroidManifest / ProGuard
- Sin cambios (ningún permiso nuevo).

## Workflows
- .github/workflows/build.yml | 2 +-
 1 file changed, 1 insertion(+), 1 deletion(-)

## Notas (revisadas a mano)
- SQL dinámico auditoria/sql/004_admin.sql:218: execute format('revoke all on function %s from public, anon', fn);
- SQL dinámico auditoria/sql/004_admin.sql:219: execute format('grant execute on function %s to authenticated', fn);
