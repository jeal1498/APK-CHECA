# Prueba del lector: mismo prompt para Haiku 5.5 y Sonnet 5.5

Tarea: leer 30 capturas de pop-ups de ofertas de viaje del bucket `capturas` de Supabase (proyecto `gqsadclvftkzwrihfipz`) y guardar lo que se ve en cada una.

## Pasos
1. Lista los objetos del bucket `capturas` (tabla `storage.objects`, solo lectura), ordenados por `created_at` y luego por `name`. Toma las primeras 30.
2. Para cada imagen, léela y responde SOLO lo que se ve en pantalla. No adivines: si un dato no se ve, pon `null`.
3. Anota el tiempo aproximado por imagen, en segundos.
4. Guarda el resultado en `pruebas/lector/resultados_<modelo>.json` (`haiku` o `sonnet`). Luego haz `git pull`, un solo commit con ese archivo y push directo a `main`.

## Formato (una entrada por imagen)
```json
{
  "modelo": "<modelo>-5.5",
  "resultados": [
    {
      "archivo": "<name del objeto>",
      "es_oferta": true,
      "tarifa": 85.5,
      "moneda": "MXN",
      "km": 4.2,
      "minutos": 12,
      "confianza": "alta|media|baja",
      "nota": "solo si algo no se ve claro",
      "segundos": 3
    }
  ]
}
```

## Reglas
- `es_oferta` es `false` si la pantalla no muestra una oferta de viaje (navegación, mapa, otra app). Entonces `tarifa`, `km` y `minutos` van en `null`.
- Números como número, no como texto.
- No uses datos de otras tablas ni de lecturas anteriores: lee solo la imagen.
- Al final di cuántas imágenes leíste y cuántas fallaron.
