# Product

<!-- impeccable:product-schema 1 -->

## Platform

android

## Stack
App existente: Kotlin, UI 100 % programática (sin XML, AppCompat ni Compose). No cambiar.

## Users
Conductores de Uber y DiDi (México). Usuario principal: Eduardo, en teléfono y en tablet horizontal con pantalla dividida (DiDi a la izquierda, Uber a la derecha). Deciden en segundos, manejando, si aceptan una oferta.

## Product Purpose
Lee la oferta en pantalla y muestra un pop-up con ganancia neta tras gasolina y desgaste, $/hora, $/km, semáforo (aceptar / considerar / rechazar) y multiplicador. Éxito: decidir bien y rápido sin distraerse.

## Positioning
Escrita desde cero; lectura en el teléfono (OCR/accesibilidad) sin internet. Calcula la neta real con los costos del propio auto, no solo la tarifa de la app.

## Operating Context
Se usa conduciendo, sobre las apps de Uber/DiDi, como overlay de accesibilidad. Canales: `directo` (cierra 15 nov 2026) y `play` (Google Play). Beta gratis hasta 31 ene 2027.

## Capabilities and Constraints
Pop-up E (tarjeta oscura, monto neto grande, tamaño 70–160 %); metas por hora y km; filtros de recogida y duración; Mis metas, Mi día, SOS, cuenta con Google. El texto del pop-up nunca debe parecer un tramo "N min (X km)" (lleva "total"). Sin compilación local.

## Brand Commitments
Marca v2 en `jeal1498/PWA-Checa-Multimedia` (skill marca-checa): asfalto #0C0C10, tablero #16161C, hueso #F4F4EF; verde #22C55E, amarillo #FACC15, rojo #EF4444 solo para decidir; Bricolage Grotesque 800, DM Sans, JetBrains Mono. Sin ciudad ni país, sin logos de Uber/DiDi ni competencia en material de marca. Textos en español, tono corto.

## Evidence on Hand
Capturas reales de ofertas en `app/src/test/resources/ofertas/`. No hay testimonios ni métricas de uso documentadas; no inventarlas.

## Product Principles
1. El color solo decide: verde/amarillo/rojo nunca decoran.
2. Legible de un vistazo, con el coche en movimiento.
3. Mínimo texto, en español claro.
4. La neta honesta manda sobre la tarifa bonita.
5. Pedir permisos con explicación simple, un paso a la vez.

## Accessibility & Inclusion
Prioridad declarada: lectura al instante (contraste y tamaño del pop-up) y onboarding/permisos claros (accesibilidad, batería, país).
