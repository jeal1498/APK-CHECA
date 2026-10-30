---
name: guion-checa
description: Escribe los guiones de video vertical de Checa para TikTok, Reels y Shorts, tanto de la cuenta CHECA como de su v2 PERSONAL y de los videos donde Eduardo sale a cámara. Trabaja en bloques de 2.5 a 3 s con una instrucción de animación por bloque, a partir del código real de la app (Parser, Comparador, Popup), con cifras calculadas con sus fórmulas y siguiendo las reglas que Eduardo ya fijó (gancho con golpe, montos sin moneda, CTA vigente). Úsala siempre que Eduardo pida un guion, un gancho, una v2, una versión A/B, cambiar el texto de la voz o «rehacer» un video de Checa, aunque no nombre la skill. La producción (voz, render, portada, copy) va después, con `video-checa`.
---

# Guion Checa

Escribe guiones que retienen a un conductor de Uber o DiDi en los primeros segundos y le muestran algo que Checa de verdad hace. El tono es de creador a creador: directo, sin tono de anuncio. Evita «revoluciona», «la solución definitiva», «no te lo pierdas», «descubre» e «increíble», además de emojis, saludos e intros. Esas fórmulas suenan a publicidad y el conductor se va.

## 1. Lee el código antes de escribir
Las cifras y las funciones tienen que ser reales. Si no, un conductor lo nota y se pierde la confianza.
1. En `CLAUDE.md`, lee Arquitectura, las reglas del lector y los filtros.
2. Abre lo que respalda la idea:
   - `Parser.kt`: `gasolina()` = km totales ÷ km por litro × $/litro, `neta()` = tarifa − gasolina, `porKm` y `porHora` sobre km y min **totales** (recogida + viaje), `nivel()` y `motivoFiltro()`.
   - `Comparador.kt`: la mejor de dos ofertas.
   - `Popup.kt`: franja ACEPTAR/CONSIDERAR/RECHAZAR, «$x netos» y fichas.
3. Escribe en una línea la fricción real del conductor: qué decide, en cuánto tiempo y qué dato le falta.
4. Calcula los números de ejemplo con esas fórmulas y los ajustes por defecto: 12 km/l, $24/l, meta $150/h y $8/km, recogida máx. 4 km. Anota el cálculo en el `guion.md`.

## 2. Estructura: la regla de los 3 segundos
- Cada bloque dura 2.5 a 3 s, con una idea y unas 8 palabras de voz. Siempre algo cambia en pantalla, porque una pantalla quieta es donde se va la gente.
- **Gancho (0:00 - 0:03):** golpe de realidad con una cifra o una pérdida, con movimiento desde el cuadro 0. A Eduardo le parecieron «muy flojas» las entradas suaves. Le funcionó esto: flash, tarjeta que se estrella con zoom, monto que sube, cámara que sacude la cifra y título grande con sello. Sin `[pause]` en el gancho, porque deja silencio con la pantalla quieta.
- **Desarrollo:** solo utilidad real del código, con ritmo rápido.
- **CTA final:** una sola acción. Hoy es «Comenta CHECA y te paso el acceso a la prueba en Google Play».
- **Duración:** 25 a 40 s.

## 3. Instrucción visual por bloque
Describe en corchetes qué se ve. Ejemplos de vocabulario:
- `[Tarjeta de oferta se estrella con zoom; contador 0 → 110]`
- `[Cuenta 110 ÷ 28 km = 3.93/km en rojo]`
- `[Raya de meta que se dibuja; barra verde sube sobre ella]`
- `[Pop-up «A · Franja» cae con spring]`
- `[Layout split: la 2ª tarjeta entra y la 1ª se reduce]`
- `[stagger de 4 frames]`

El color solo decide (verde, amarillo, rojo) y el resto va en asfalto, tablero y hueso. Los pop-ups dicen «OFERTA», nunca el nombre de una app. Todo lo importante va por encima de y≈1150. Los tipos de escena que ya existen están en `video-checa` (`references/narrador.md`), así que úsalos para que el guion se pueda producir sin código nuevo.

## 4. Formato de entrega (exacto)
```
Concepto / Problema detectado en el repo:
Guión Desglosado:
- `0:00 - 0:03` | Audio: [...] | Remotion / Visual: [...]
- `0:03 - 0:06` | Audio: [...] | Remotion / Visual: [...]
...
CTA final:
```
- **En el chat:** solo el texto de la voz, limpio y de corrido, sin tablas, etiquetas ni preguntas extra. Eduardo lo escucha con lectura en voz alta.
- **En `marketing/videos/CHECA|PERSONAL/NN - Nombre/guion.md`:** el formato completo, las cifras con su cálculo y el estado («propuesto» hasta que lo apruebe). Si ya existe, guarda el anterior como `guion_anterior.md` con `mv`.
- **Git:** commit y push a `main`.

## 5. Reglas de la voz
- **Montos sin moneda:** escribe «110», no «$110». ElevenLabs leyó «dólares». En pantalla sí van con $ y «Números de ejemplo».
- **Ortografía:** «checas», «que cheques», nunca «checes».
- **Sin autorrespuestas:** evita las preguntas que la voz se contesta sola y los «no es X, es Y».
- **«La app Checa»:** dilo así cuando pueda confundirse con Uber o DiDi.
- **Sin promesas:** nada de promesas de ganancia, ciudad, país, «gratis» ni precios.

## 6. Versiones
- **v2 PERSONAL:** usa las mismas cifras que la v1, con otro gancho, otras palabras y otro orden. Por ejemplo, la v1 abre con la comparación y la v2 con lo que te queda. Lleva el mismo número de carpeta. Reglas en `marketing/plan/reglas-v2.md`.
- **A/B:** el mismo texto y las mismas cifras; solo cambia la capa visual (A con cámara, B con gráficos).

## 7. Si lo dice Eduardo a cámara
Aprendido de su corrección en el video 23:
- **Mexicano coloquial, sin lenguaje de anuncio:** «a la mera hora», «no te agüites», «fletarme», «en corto», «ahorita». Evita «aquí viene lo bueno» y «apártalo ya».
- **Consuelo antes que solución:** primero reconoce el golpe y después ofrece la salida.
- **Identidad, no credencial:** «papá, esposo y conductor», «salgo a la calle a fletarme».
- **Trato entre iguales:** pide ayuda y ofrece algo a cambio, sin escasez ni presión.
- **Un solo número en la voz:** los porcentajes y las condiciones van en pantalla o en el copy.
- **Ritmo hablado:** frases largas con comas, de un tirón.
- **Abre con la pérdida en palabras crudas:** si lleva groserías, avísale del efecto en el alcance y ofrécele una opción suave, pero decide él.
- **Cuida los absolutos:** di «casi nadie» en lugar de «nadie».
