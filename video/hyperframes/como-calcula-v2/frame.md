---
version: alpha
name: Checa v2 — Frame (video)
description: >
  Capa de video de la marca Checa v2 (17 oct 2026). Fuente única de la marca: repo jeal1498/checa-web,
  carpeta marca/ (skill marca-checa). Fondo asfalto, texto hueso, y el color solo decide.
unit: el cuadro — 1080×1920 (9:16) principal; 1920×1080 y 1080×1080 solo si se piden
principle: una idea por cuadro · el color significa algo · todo redondeado

colors:
  asfalto: "#0C0C10"
  tablero: "#16161C"
  humo: "#3A3A45"
  hueso: "#F4F4EF"
  gris-texto: "#A3A3AD"
  aceptar: "#22C55E"
  considerar: "#FACC15"
  rechazar: "#EF4444"
  texto-sobre-aceptar: "#04210F"
  texto-sobre-considerar: "#2B2200"
  texto-sobre-rechazar: "#1F0000"

typography:
  numero-heroe: { fontFamily: "Bricolage Grotesque", px: 150, weight: 800, lineHeight: 1.0, tracking: "-0.04em" }
  titulo:       { fontFamily: "Bricolage Grotesque", px: 96, weight: 800, lineHeight: 0.95, tracking: "-0.045em" }
  subtitulo:    { fontFamily: "Bricolage Grotesque", px: 56, weight: 800, lineHeight: 1.05, tracking: "-0.03em" }
  texto:        { fontFamily: "DM Sans", px: 40, weight: 400, lineHeight: 1.45 }
  etiqueta:     { fontFamily: "JetBrains Mono", px: 28, weight: 700, tracking: "0.12em", upper: true }
  dato:         { fontFamily: "JetBrains Mono", px: 46, weight: 700 }
  subtitulo-video: { fontFamily: "Bricolage Grotesque", px: 60, weight: 800, lineHeight: 1.08, tracking: "-0.03em" }

shapes:
  radio-tarjeta: 44px
  radio-ficha: 28px
  radio-pildora: 999px

motion:
  curva: "cubic-bezier(0.2, 0.8, 0.2, 1)"   # GSAP: "expo.out" o power4.out; sin rebote
  palabra-subtitulo: 0.15s
  popup-entra: 0.2s
  numero-sube: 0.6s
  cambio-de-toma: 3s
  logo: "rojo 0 s → ámbar 0.3 s → verde 0.6 s → ✓ 0.9 s → nombre 1.2 s"
---

# Checa v2 — Frame

## Qué es
Un video de Checa es un **ticket de información sobre asfalto**: fondo `asfalto`, un número o una pregunta
enorme en `hueso`, y color solo cuando algo se decide. Redondeado (el cuerpo del semáforo), sin neones, sin
degradados de colores, sin emojis. Una idea por cuadro; 45 % del cuadro vacío en los declarativos.

## Color = decisión
- **Verde `aceptar`:** lo que ganas, lo que conviene. **Amarillo `considerar`:** la palabra a recordar, la raya de la calle. **Rojo `rechazar`:** lo que pierdes (gasolina, km sin pago).
- **Hueso** es todo lo demás (texto, botones, marca). Un cuadro nunca usa los tres colores para adornar.
- Texto sobre verde/amarillo/rojo: siempre el oscuro de la lista, nunca blanco.

## Cuadro 1080×1920
- **Zonas de la app de video:** nada importante en el 12 % de arriba (y < 230 px) ni en el 22 % de abajo (y > 1498 px), salvo la **pantalla final**, que ocupa abajo pero con todo por encima de y ≈ 1450.
- **Banda de subtítulos:** `top: 1160px`, alto 320 (termina en 1480). El contenido del cuadro va **por encima de y = 1140** o con hueco claro; no tapar la banda.
- Márgenes laterales 60 px. Un solo elemento dominante, 3–6× más grande que el resto.

## Componentes
- **Logo** (`assets/logo-checa.svg`): semáforo con luz verde encendida y ✓. Entra con la animación de `motion.logo`. Mínimo 24 px de alto; no estirar, girar ni cambiar las luces.
- **Pop-up** (`assets/checa-popup.html`): se copia tal cual; los tres niveles miden **exactamente lo mismo**. Ningún texto es solo un monto; «números de ejemplo» donde haya cifras; las cifras cuadran (neta = viaje − gasolina, gasolina ≈ $2/km); oferta genérica sin logos de Uber/DiDi.
- **Subtítulos** (`compositions/captions.html`): estilo A de la marca. Máx. 2 líneas y 5 palabras por pantalla; palabra activa en bloque amarillo con texto oscuro; ya dicha en hueso; por venir en gris claro; sombra oscura, sin placa. Para cifras: clase `cap-gana` (verde) o `cap-pierde` (rojo). Entrada palabra por palabra, 150 ms, sin rebote.
- **Raya de carretera:** línea punteada amarilla `stroke-dasharray: 2 14`, siempre curva, con un punto verde; sirve de separador, de camino que se dibuja o de cierre.
- **Íconos de línea:** trazo 2.2, redondeado, `hueso`. Sin rellenos de color salvo el semáforo.
- **Sello:** círculo con borde del color del semáforo y su palabra, inclinado −10° a 8°.
- **Pantalla final** (≥ 2 s tras la voz): logo + «Descarga Checa» + «solo Android» + `@checa.driver` («síguenos en tiktok») + `@Eduardoalv9802` («creador de checa») + `jeal1498.github.io/checa-web`. Todo por encima de y ≈ 1450.

## Movimiento
Curva rápida que frena suave, sin rebote. Cambio de toma cada 3 s. Una transición por objeto (la tarjeta se vuelve el teléfono, el número se vuelve la barra del semáforo, el logo se arma desde las tres luces). Con «reducir movimiento»: solo cortes.

## Qué NO hacer
- Neones rosa/azul, fondo `#0D0D0D`, Fira Sans/Fira Code, degradados verdes, placa negra con borde verde en los subtítulos (todo eso era la marca anterior).
- Esquinas rectas, sombras largas de color, más de un número protagonista, texto de más de 5 palabras en pantalla.
- Logos o colores de Uber/DiDi, ciudad o país, nombres de competencia, promesas de ganancia.
