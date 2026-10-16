# Checa

App Android para conductores de Uber y DiDi: lee cada oferta y muestra un pop-up con **$/km**, **$/hora**, la ganancia neta después de gasolina y un semáforo (conviene / regular / no conviene).

## Qué hay en este repo
| Carpeta | Para qué |
|---|---|
| `app/` | Código de la app (Kotlin, UI programática). Cada push a `main` compila el APK y lo publica en **Releases**. |
| `supabase/` | Cuentas, licencias y cobros con Mercado Pago (funciones y esquema). |
| `.github/workflows/` | Compilar APK, generar licencias, reporte diario, subir a Supabase, publicar el APK en la web. |
| `pedidos/`, `clientes.md` | Generador de licencias (lo usa `licencia.yml`). |
| `reportes/` | Reporte diario de la web y la app (lo escribe `reporte-web.yml`). |
| `marketing/` | Videos listos (`CHECA/`, `PERSONAL/`), guiones, calendario, web, marca. |
| `video/` | Proyectos para hacer los videos (Remotion y HyperFrames). |
| `docs/` | Documentación que no hace falta cargar siempre (p. ej. historial de versiones). |
| `.claude/skills/` | Skills que usa Claude; las que no se usan están en `.claude/skills-archivo/`. |

Contexto completo para trabajar con Claude: `CLAUDE.md`.
