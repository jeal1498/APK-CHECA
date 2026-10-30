# Página web como funnel · Paso 7: medición

**Herramienta:** Supabase (tabla `web_eventos`), sin cookies ni datos personales. La web solo puede anotar; nadie de fuera puede leer.
**Reporte:** cambia la fecha en `reportes/pedir.txt` (y `dias=` si quieres otro periodo), push a `main`, y en ~30 s queda `reportes/web.md`. O pídeselo a Claude: «dame el reporte de la web».

## Qué se anota
| Evento | Cuándo | Para decidir |
|---|---|---|
| `visita` | Abre cualquier página | ¿Llega gente? ¿De dónde? |
| `clic_descargar` | Toca un botón de descargar (con `desde`: hero, demo, estratega, fijo…) | ¿Qué sección convence? |
| `descarga` | En Android, empieza la descarga en `/instalar/` | **Conversión principal** |
| `descarga_instalar` | Toca «Descargar» o «Descargar otra vez» en `/instalar/` | ¿Falla la descarga automática? |
| `whatsapp_ayuda` | Pide ayuda para instalar | ¿Instalar es muy difícil? |
| `beta`, `beta_whatsapp` | Toca la banda de beta / pide su lugar | ¿Funciona la beta? |
| `video_play` | Reproduce el video | ¿El video ayuda? |

Cada evento lleva: página, dispositivo (android, iphone, pc), fuente y campaña.

## Links para publicar (ponlos tal cual)
La fuente sale de `?utm_source=` (o `?ref=`). Sin ella, se usa el sitio de donde llegó, y si no hay, «directo».

| Dónde | Link |
|---|---|
| Perfil de TikTok | `https://jeal1498.github.io/PWA-Checa-Multimedia/?utm_source=tiktok` |
| Perfil de Instagram | `https://jeal1498.github.io/PWA-Checa-Multimedia/?utm_source=instagram` |
| Página de Facebook | `https://jeal1498.github.io/PWA-Checa-Multimedia/?utm_source=facebook` |
| YouTube | `https://jeal1498.github.io/PWA-Checa-Multimedia/?utm_source=youtube` |
| Grupos de WhatsApp | `https://jeal1498.github.io/PWA-Checa-Multimedia/?utm_source=whatsapp&utm_campaign=grupos` |
| Grupos de Facebook | `https://jeal1498.github.io/PWA-Checa-Multimedia/?utm_source=facebook&utm_campaign=grupos` |
| Campaña beta | `https://jeal1498.github.io/PWA-Checa-Multimedia/beta/?utm_source=<red>&utm_campaign=beta` |
| Un video en especial | agrega `&utm_campaign=<tema>` (p. ej. `gasolina-cara`) |

## Qué mirar cada semana
1. **Visitas → descargas** (meta inicial: 15 % o más en Android).
2. **Fuente con más descargas**: ahí poner más esfuerzo.
3. **iPhone vs Android**: si muchas visitas son iPhone, avisar «solo Android» en los videos.
4. **WhatsApp de ayuda / descargas**: si pasa de 20 %, mejorar `/instalar/` con capturas.
5. Con ~1,000 visitas al mes, empezar las pruebas A/B de `05-cro.md`.

Nota: el 13 oct hay 1 visita de prueba (fuente «prueba»); no cuenta.
