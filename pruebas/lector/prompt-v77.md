# Revisión del lunes: Haiku 5.5 lee TODAS las capturas de la v10.77

Tarea: leer todas las capturas de la versión 10.77 del bucket `capturas` de Supabase (proyecto `gqsadclvftkzwrihfipz`) y guardar lo que se ve en cada una. Nada más: no compares con otras lecturas ni corrijas código.

## Pasos
1. Lista (solo lectura): `select o.name, e.id from storage.objects o join public.errores_lector e on e.id::text = split_part(o.name,'.',1) where o.bucket_id = 'capturas' and e.version = '10.77' order by e.id;`
2. Baja las imágenes con la función temporal `tmp-capturas-9f3k` (`docs/banco-lector.md` §2: clave aleatoria nueva en el encabezado, limitada a esos ids) y conviértelas a PNG con ffmpeg en tu directorio temporal. **Las imágenes NO van al repo.**
3. Lee las imágenes en lotes de 30. Después de cada lote guarda lo avanzado en `pruebas/lector/v77/resultados_haiku_v77.json` (así no se pierde nada si se corta).
4. Al terminar: desactiva la función (stub 410), borra las imágenes de tu directorio temporal, haz `git pull`, un solo commit con el archivo y push directo a `main`. No borres nada de Supabase.

## Qué anotar por imagen
```json
{"id": 947, "estado": "vigente|vencida|aceptada|sin_oferta", "es_oferta": true, "tarifa": 63.42, "km": 11.8, "minutos": 26, "confianza": "alta|media|baja", "nota": "solo si algo no se ve claro"}
```
Reglas:
- `estado`: `vigente` = oferta activa que se puede aceptar; `vencida` = dice «La solicitud de viaje venció» o «La solicitud ya no está disponible» (el texto apagado se ve, pero no cuenta); `aceptada` = «Viaje aceptado»; `sin_oferta` = mapa, navegación, otra pantalla.
- `es_oferta` es `true` solo si `estado` es `vigente`. En las demás, `tarifa`, `km` y `minutos` van en `null`.
- `km` y `minutos` son la suma de los dos tramos de la tarjeta de Uber/DiDi, no del pop-up de Checa. Si solo se ve una parte, pon lo que se ve y baja la confianza.
- Lee solo la imagen. No uses las columnas `local` ni `ia` de `errores_lector`.
- Números como número, no como texto. Si un dato no se ve, `null`; no adivines.
- Al final di cuántas leíste, cuántas por estado y cuántas fallaron.
