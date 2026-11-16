# Workflows: disparadores y efectos reales (auditoría de cierre, 16-nov-2026)

Leído de los 7 archivos de `.github/workflows/` en este commit (idénticos a `main` `a7a732f`: `git diff a7a732f -- .github` vacío). Ninguno se ejecutó ni se modificó.

| Workflow | Disparadores | Efecto | Secretos que usa |
|---|---|---|---|
| `build.yml` | push a `main` salvo que **todas** las rutas cambiadas estén en `paths-ignore`; manual | `gradle assembleRelease`; **crea el release `v<run_number>` con `MiRuta.apk`**; llama a `publicar-apk.yml` | `GITHUB_TOKEN` (contents: write); firma `MIRUTA_*`; hereda todos |
| `publicar-apk.yml` | release publicado; manual; llamado por `build.yml` | baja el APK del release y **hace commit y push a `jeal1498/PWA-Checa-Multimedia`** (`descargar/Checa.apk`, `version.json`) → la app del canal directo ofrece la actualización | `CHECA_WEB_TOKEN` |
| `play-compilar.yml` | push a `main` **o PR hacia `main`** que toque `app/**`, `*.gradle.kts`, `gradle.properties` o su yml; manual | `assemblePlayRelease`, `testPlayReleaseUnitTest` (JUnit real: aquí correrían `TrazaTest` y `GuardiaParserTest`), comprobaciones de textos, firma y sube un APK como artefacto | `PLAY_UPLOAD_*` (en PR sin secretos omite la firma) |
| `play-aab-firmado.yml` | solo manual | AAB firmado como artefacto | `PLAY_UPLOAD_*` |
| `supabase.yml` | push a `main` que toque `supabase/**` o su yml; manual | **ejecuta `supabase/esquema.sql` completo en producción** (API de administración), carga secretos y **despliega** `mp-pago`, `mp-webhook`, `eliminar-cuenta`, `confirmar-oferta` | `SUPABASE_ACCESS_TOKEN`, `MP_*`, `LICENCIA_PRIVADA` |
| `licencia.yml` | push a `main` que toque `pedidos/pedido.txt`; manual | genera una licencia y hace push de la respuesta | `SUPABASE_SERVICE_ROLE`, `LICENCIA_PRIVADA` |
| `reporte-web.yml` | push a `main` que toque `reportes/pedir.txt`; **cron diario 12:52 UTC**; manual | consulta Supabase y hace push del reporte | `SUPABASE_ACCESS_TOKEN` |

## Qué pasaría con esta rama
| Acción | Resultado |
|---|---|
| Push de la rama (no es `main`) | **Nada.** Ningún workflow escucha otras ramas |
| PR hacia `main` | **`play-compilar.yml`** (toca `app/**`): compila y prueba; con los secretos del repo firma y sube un APK de prueba como artefacto. No publica nada |
| Merge o push a `main` | **`build.yml` → release del canal directo con la 10.81 + push a la web** (hay rutas fuera de `paths-ignore`: `admin/`, `auditoria/`, `evidencias/`, `herramientas/`, `respaldo-e0/`, `paquete-revision-e0/` y los `.md` de la raíz). **`play-compilar.yml`** (`app/**`). **`supabase.yml`** (por `supabase/functions/registrar-lectura/**`): vuelve a ejecutar `esquema.sql` (sin cambios) y redepliega las 4 funciones nombradas (sin cambios); **no** despliega `registrar-lectura` ni aplica `auditoria/sql/*` |

## Riesgos y cómo evitarlos (decisión del propietario)
1. **Release involuntario del canal directo** (y actualización ofrecida a sus usuarios; ese canal cerró el 15-nov-2026). Opciones: integrar por partes dejando fuera `app/`; o agregar las carpetas nuevas a `paths-ignore` de `build.yml` en un commit previo y revisado; o deshabilitar `build.yml` antes del merge.
2. **Mezcla de instrumentación con el resto:** la rama lleva E0–E7 en una sola línea. Cada etapa es un commit: se puede integrar por `cherry-pick` en ramas separadas (E3 sola, como pide INSTRUCCIONES-SONNET §3 y §7).
3. **`supabase.yml` re-ejecuta todo `esquema.sql`:** es idempotente y no cambió, pero es una ejecución en producción; conviene integrar `supabase/functions/registrar-lectura` aparte y con autorización.
4. La rama `trabajo/e0-respaldo`, ya en GitHub, tiene el mismo riesgo 1 si se fusiona (toca `respaldo-e0/` y `.md` de la raíz).
5. Los pushes que hacen los propios workflows con `GITHUB_TOKEN` no disparan otros workflows; `publicar-apk.yml` usa `CHECA_WEB_TOKEN` sobre **otro** repositorio (sus workflows no se revisaron: fuera de alcance).
