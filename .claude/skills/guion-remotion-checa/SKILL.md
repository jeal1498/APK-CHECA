---
name: guion-remotion-checa
description: Genera guiones verticales de Checa (TikTok, Reels, Shorts) con la «regla de los 3 segundos» y una instrucción de animación por bloque para Remotion / Hyperframes, partiendo del análisis del código de la app. Úsala al pedir un guion de video nuevo de Checa (cuenta CHECA o PERSONAL), una versión A/B o «un guion con Remotion».
---

# Guion Remotion · Checa

## Rol y tono
Creador de contenido senior, guionista de video vertical y frontend especializado en animación programática (Remotion / Hyperframes). Hablas de creador a creador: directo, pragmático, sin tono corporativo ni publicitario. Prohibido: «revoluciona», «la solución definitiva», «no te lo pierdas», «descubre», «increíble», emojis de adorno, saludos e intros.

## Paso 1 · Analizar el repo antes de escribir
1. Lee `CLAUDE.md` (Arquitectura, reglas del lector, semáforo y pop-up).
2. Abre el código que respalda la idea: `Parser.kt` (`gasolina()`, `neta()`, `porKm`/`porHora` sobre km y min **totales**, `nivel()`, `motivoFiltro()`), `Comparador.kt` (mejor oferta por neta/hora, neta/km o neta total), `Popup.kt` (franja ACEPTAR/CONSIDERAR/RECHAZAR, «$x netos», fichas).
3. Identifica la fricción real del conductor (qué decide, en cuántos segundos, con qué dato le falta) y escríbela en una línea.
4. Usa solo funciones que existan. Calcula los números de ejemplo **con las fórmulas del código** y los ajustes por defecto (12 km/l, $24/l, meta $150/h y $8/km, recogida máx. 4 km) y anótalos en el guion.
5. Marca: sin ciudad ni país, sin logos de Uber/DiDi, «números de ejemplo» en pantalla si la voz no lo dice. La oferta/CTA vigente se confirma con Eduardo.

## Paso 2 · Regla de los 3 segundos
- Cada bloque dura **2.5 a 3 s**, máximo. Nunca un bloque sin cambio visual.
- Una idea por bloque; la voz cabe en ~8 palabras.
- **Gancho (0:00 - 0:03):** fricción o golpe de realidad con una cifra o una pérdida. Cero saludos, cero intros.
- **Desarrollo:** solo utilidad real del código, ritmo rápido.
- **CTA final:** una sola acción de conversión (p. ej. «Comenta CHECA…»), verificable.
- Duración total: 25–40 s.

## Paso 3 · Instrucción de Remotion por bloque (entre corchetes)
Ejemplos de vocabulario:
- `[RideCard con spring entry desde abajo]`
- `[Interpolación de texto palabra por palabra]`
- `[Contador numérico animado $95 → $67 con interpolate + Math.round]`
- `[Tachado SVG que se dibuja con strokeDashoffset]`
- `[Layout split: segunda tarjeta entra con spring, la primera se reduce con scale + translateX]`
- `[Componente Popup «A · Franja» con franja del semáforo y pastilla ★]`
- `[Barras/arcos que crecen con spring]`, `[stagger de 4 frames]`, `[zoom scale 1 → 1.4]`

Reglas: entradas con `spring()` (damping medio), salidas con `interpolate()` clamped; verde/amarillo/rojo solo para decidir, el resto asfalto/tablero/hueso; Bricolage (títulos y números), DM Sans (texto), JetBrains Mono (datos). Si se produce en Hyperframes (`video/hyperframes/`), traduce las mismas instrucciones con el proceso económico de la skill `videos-checa`.

## Formato de entrega (exacto)
```
Concepto / Problema detectado en el repo:
Guión Desglosado:
- `0:00 - 0:03` | Audio: [...] | Remotion / Visual: [...]
- `0:03 - 0:06` | Audio: [...] | Remotion / Visual: [...]
...
CTA final:
```

## Versiones A/B
Mismo texto y mismas cifras; solo cambia la capa visual (A: cámara; B: gráficos).

## Guardar
Guarda en `marketing/videos/CHECA/NN - Nombre/guion.md` (o `PERSONAL/`) con el siguiente número libre, junto con los números de ejemplo y su cálculo. Commit y push. El guion queda «propuesto» hasta que Eduardo lo apruebe (flujo de etapas de `videos-checa`).
