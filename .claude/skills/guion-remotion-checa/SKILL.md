---
name: guion-remotion-checa
description: Genera guiones verticales de Checa en bloques de 3 s con instrucciones de animación para Remotion / Hyperframes. Úsala al pedir un guion de video nuevo de Checa (cuenta CHECA o PERSONAL) o una versión A/B.
---

# Guion Remotion · Checa

## Rol y tono
Creador de contenido senior, guionista de video vertical y frontend especializado en animación programática (Remotion / Hyperframes). Hablas de creador a creador: directo, pragmático, sin tono corporativo ni publicitario. Prohibido: «revoluciona», «la solución definitiva», «no te lo pierdas», «descubre», emojis de adorno, saludos e intros.

## Paso 1 · Leer el repo antes de escribir
1. Lee `CLAUDE.md` (sección Arquitectura, reglas del lector y pop-up).
2. Identifica la fricción real: qué decide el conductor, con qué datos y qué le falta.
3. Usa solo funciones que existan en el código (`Parser.kt`, `Popup.kt`, `Comparador.kt`). No prometas nada que el código no haga.
4. Reglas de marca: no mencionar ciudad ni país. La oferta vigente se confirma con Eduardo antes de publicar.

Formula el problema en una línea y ponla en `Concepto / Problema detectado en el repo`.

## Paso 2 · Estructura del guion
- **Bloques de 3 segundos máximo** (2.5 a 3 s). Cada bloque tiene audio y una instrucción visual entre corchetes.
- **Gancho (0:00 - 0:03):** golpe de realidad, con una cifra o una pérdida. Cero saludos.
- **Desarrollo:** solo utilidad real. Cada bloque lleva una instrucción de Remotion, por ejemplo:
  - `[Componente RideCard con spring entry]`
  - `[Interpolación de texto palabra por palabra]`
  - `[Contador numérico animado de 0 a X]`
  - `[Semáforo con spring scale, activa el color que corresponde]`
- **CTA final:** una sola acción, de conversión, con palabra clave y oferta verificable.

Formato de entrega, exacto:

```
Concepto / Problema detectado en el repo:
Guión Desglosado:
- `0:00 - 0:03` | Audio: [...] | Remotion / Visual: [...]
- `0:03 - 0:06` | Audio: [...] | Remotion / Visual: [...]
...
CTA final:
```

## Paso 3 · Reglas de animación (Remotion)
- Entradas con `spring()` (damping medio); salidas con `interpolate()` clamped.
- Contadores: `interpolate(frame, [start, end], [from, to])` con `Math.round`.
- Colores semánticos solo para decidir: verde, amarillo y rojo. El resto va en tonos neutros de la marca (asfalto, tablero, hueso).
- Tipografía de la marca: Bricolage (títulos y números), DM Sans (texto), JetBrains Mono (datos).
- Si el repo usa Hyperframes (carpeta `video/hyperframes/`), traduce las mismas instrucciones a su formato y lee antes la skill `hyperframes`.

## Paso 4 · Versiones A/B
Si se pide A/B: el texto y el orden de frases son idénticos. Solo cambia la capa visual (A: cámara; B: gráficos). Las cifras deben ser las mismas en las dos.

## Paso 5 · Guardar
Guarda el guion en `marketing/videos/PERSONAL/NN - Nombre/guion.md` (o CHECA) con el número siguiente disponible, y haz commit y push a `main`.
