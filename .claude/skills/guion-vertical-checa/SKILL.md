---
name: guion-vertical-checa
description: >
  Escribe guiones de video vertical (TikTok, Reels, Shorts) para Checa en bloques de 3 segundos con instrucción de edición por bloque. Usar cuando Eduardo pida un guion nuevo, una versión A/B o una variante de un video de Checa.
---

# Guion vertical de Checa (regla de los 3 segundos)

## Tono
Creador a creador. Directo, sin saludos, sin intros, sin frases de marketing ("la solución definitiva", "revoluciona tu ganancia"). Habla como conductor que le explica a otro conductor.

## Paso 1: problema real del repo
Antes de escribir, ubica la fricción que la app resuelve. Fuente: `CLAUDE.md` (sección Arquitectura).
- Durante una oferta, el conductor tiene segundos para decidir.
- La app de Uber o DiDi muestra el monto bruto. No resta gasolina ni desgaste, ni cuenta el tiempo real de recogida.
- Checa lee la pantalla de la oferta (`CapturaService`, `RutaService`) y muestra un pop-up con: neta después de gasolina, $/hora, $/km y semáforo (verde, amarillo, rojo).
- Sin licencia, Checa no muestra pop-ups. Durante la beta, todos tienen todo.

Escribe el problema en una línea en el campo «Concepto / Problema detectado en el repo». No inventes funciones que no estén en el código.

## Paso 2: estructura
- **Gancho (0:00 - 0:03):** fricción directa o golpe de realidad. Cero saludos.
- **Desarrollo:** solo utilidad real de la app. Cada bloque de ~3 s lleva `Audio` y `Visual (Edición obligatoria)` entre corchetes.
- **CTA final:** una sola acción, con palabra para comentar.

### Regla de los 3 segundos
Cada bloque cambia algo en pantalla cada 2.5 a 3 segundos máximo: zoom, texto palabra por palabra, overlay, gráfico o corte. Si un bloque no tiene cambio visual, no entra.

## Reglas de contenido
- No mencionar ciudad ni país en el material de marca.
- No nombrar apps de la competencia en el material de marca (Ruta Rentable, etc.). En el guion se puede decir "Uber o DiDi".
- Cifras de ejemplo: marcarlas como ejemplo en las notas, y pedir que se cambien por un viaje real.
- Promesa de oferta: confirmar vigencia con Eduardo antes de publicar. La beta gratis dura hasta el 31 ene 2027 (`Licencia.BETA_HASTA`).
- Siempre con el CTA de comentario con palabra (`YO`, `CHECA`, etc.) y el premio concreto si lo hay.

## Formato de entrega
```
- Concepto / Problema detectado en el repo:
- Guión Desglosado:
  - `0:00 - 0:03` | Audio: [...] | Visual (Edición obligatoria): [...]
  - `0:03 - 0:06` | Audio: [...] | Visual (Edición obligatoria): [...]
  - ...
- CTA final:
```

## Pruebas A/B
Si Eduardo pide dos versiones del mismo guion (A con cara, B con gráficos), el texto y el orden de frases son idénticos. Solo cambia la columna Visual. La duración debe ser casi igual.

## Guardar
Cada guion aprobado se guarda en `marketing/videos/PERSONAL/NN - Nombre/guion.md` (o `CHECA/` si es de la cuenta de Checa), con el mismo formato de `guion.md` existente. Los números de carpeta siguen la secuencia de `marketing/plan/calendario.md`.
