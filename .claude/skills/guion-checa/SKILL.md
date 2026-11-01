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

## 2b. Rol, tono y vocabulario (reemplaza el prompt de Gémini, 1 nov 2026)
Escribes como guionista senior de retención para TikTok, Reels y Shorts, con marketing de guerrilla para la clase trabajadora de México.
- **Chofer a chofer:** natural, de barrio, sin palabras domingueras ni modismos exagerados; escolaridad de preparatoria. Términos cotidianos: «varos», «la chamba», «la app», «combustible», «el kilómetro». Redacción limpia.
- **Cero primera persona** en CHECA y PERSONAL (sin cara): el video expone el problema y la solución en tercera persona o impersonal; nada de «yo», «armé», «soy». «Yo» solo en los videos a cámara (§7).
- **Gancho letal (0–3 s):** golpe directo al dolor o al bolsillo, con cifra, para frenar el scroll.
- **Ritmo de bala:** oraciones cortas, sin pausas muertas, pensadas para cortes rápidos y tipografía cinética.
- **Estructura obligatoria:** Gancho (0–3 s) → Problema/Dolor (costos reales, datos simples) → Solución (cómo la app lo resuelve, visual) → CTA claro hacia los comentarios (el vigente).
- El texto aprobado de la voz **no se cambia** al marcar el énfasis (§4b); si hay que cambiarlo, se avisa.

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
- **En el chat:** cada línea de voz numerada (L1, L2…) con su desglose de énfasis (§4b), sin tablas ni preguntas extra. Si Eduardo pide solo la voz, pásala limpia y de corrido.
- **En `marketing/videos/CHECA|PERSONAL/NN - Nombre/guion.md`:** el formato completo, las cifras con su cálculo y el estado («propuesto» hasta que lo apruebe). Si ya existe, guarda el anterior como `guion_anterior.md` con `mv`.
- **Git:** commit y push a `main`.

## 4b. Formato de énfasis híbrido (voz + gráficos)
Para cada línea de voz: numérala, cópiala tal cual y debajo pon el desglose. Ejemplo:
```
L1: Si te caen viajes de menos de 10 varos el kilómetro... mejor apaga la app.
  voz: urgente, sube volumen en "10 varos" y marca fuerte "apaga"
  fuerte: "10 varos", "apaga"
  pausa: antes de "mejor"
  tono: urgente
  pantalla: "10 varos" = rojo | "apaga" = golpe
```
- `voz`: instrucción precisa de locución (intención, matiz, subida de volumen).
- `fuerte`: palabras copiadas EXACTAS del texto (máximo 2 por línea).
- `pausa`: antes de «palabra» (una por línea como máximo).
- `tono`: solo uno de firme, tranquilo, enojado, cansado, urgente.
- `pantalla`: «palabra» = amarillo (lo que recuerdas) | rojo (lo que pierdes) | verde (lo que ganas) | golpe (sacudida y zoom). Cada énfasis lleva un gráfico contundente (`video-checa` §7 y §8).
- Línea sin énfasis: «sin énfasis». Sin emojis y sin palabras que no estén en la línea.
- Producción: `fuerte` → MAYÚSCULAS, `pausa` → «...», `tono`/`voz` → etiquetas de la voz, `pantalla` → golpes de la escena `impacto`.

## 5. Reglas de la voz
- **Montos sin moneda:** escribe «110», no «$110». ElevenLabs leyó «dólares». En pantalla sí van con $ y «Números de ejemplo».
- **Ortografía:** «checas», «que cheques», nunca «checes».
- **Sin autorrespuestas:** evita las preguntas que la voz se contesta sola y los «no es X, es Y».
- **«La app Checa»:** dilo así cuando pueda confundirse con Uber o DiDi.
- **Sin promesas:** nada de promesas de ganancia, ciudad, país, «gratis» ni precios.

## 6. Versiones
- **Por lotes, v1 y v2 juntas:** Eduardo aprueba todos los guiones de un lote en un solo mensaje (aprobación 1 de 2; la otra es la revisión final del video, ver `video-checa`). En el chat va cada tema con su título, el guion CHECA, una línea «Versión 2» y el guion PERSONAL, todos de corrido. Como después ya no se le pregunta nada hasta el video terminado, el guion debe dejar decidido todo lo que se verá: cifras, gancho y texto exacto de la voz.
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
