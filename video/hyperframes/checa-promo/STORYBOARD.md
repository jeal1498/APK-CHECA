---
format: 1080x1920
duration: 50s
message: "Checa te dice en un segundo si una oferta de Uber o DiDi de verdad te conviene"
arc: PAS con Demo Loop — duda → dolor → Checa lee → pop-up → por hora/km → neta → semáforo → metas → recogida → la mejor → horas → resultado → prueba gratis
audience: conductores de Uber y DiDi
mode: autonomous
music: none
---

## Video direction

- **palette system** (from `frame.md`, brand-remixed broadside): canvas `cream` #000000 (OLED black) is the default ground; text `ink-black` #F1F5F2; the ONE accent is `fire-orange` #22C55E (Checa green); `cream-muted` #FACC15 = yellow (doubt / CONSIDERAR); `cream-hint` #AFB7B2 = secondary text. Brand extras from the capture: red #F43F5E for pain/RECHAZAR outside the pop-up, gold #E8B84A only for "the best" (frame 10). Registers: black ground everywhere except **Frame 2 = light register** (#F1F5F2 ground, black type) and **Frame 12 = green register** (#22C55E ground, #04120A type). One register per frame.
- **the pop-up is sacred**: every frame that shows it copies `assets/checa-popup.html` verbatim (its own classes and palette #181C1A / #22C55E / #F5B70A / #EF4444); only numbers, level class and size (`--u`) change. Never restyle it into broadside atoms. Its texts are never a lone amount ("$118 netos", "$321/h", "$9.4/km", "16 km total").
- **type**: display = Fira Sans 700–800, lowercase, negative tracking (broadside's massive-lowercase primitive) for statement words; numbers and amounts = Fira Code 700 (`tabular-nums`); chrome/kickers = Fira Code uppercase 0.14em in green with the 36×2 green rule stub. Spanish accents must render (¿ ¡ á é í ó ú ñ).
- **chrome**: every frame except 1, 12, 13 carries a tiny top kicker (Fira Code label, green) naming the step — e.g. `01 / LA DUDA`. "Números de ejemplo" appears as a small `cream-hint` label wherever a $ figure shows (frames 2, 4–10).
- **motion grammar**: smooth long-tail `power3` settles; NO bounce/elastic (the lone overshoots allowed: comparison-split badges in frames 5 and 10). Reveal model = VO-paced: each piece enters when Fernando says it (word times are in the frame's audio), nothing front-loaded; holds are still, subtle jitter at most. Internal seams are velocity-matched cuts (`cut-catalog.md`).
- **rhythm / held frames**: high-energy reveals in 1–9; **Frame 6 and Frame 11 hold** a beat of stillness after their payoff (breathers before 7 and before the 12–13 close); Frame 12 is the loudest kinetic slam; Frame 13 ends on a held lockup with the only real exit (fade to black at the very end).
- **layout**: 1080×1920 vertical; all content in the top ~83% (captions live in the bottom band, y > ~1590). Primary visual ≥ 40% of the canvas, ≥ 3 depth layers (ground + soft green/colored radial glow + subject). Kicker near the top safe area (y ≈ 200–260), never under TikTok's right-rail icons for key info (keep key numbers within x 60–900).
- **negative list**: no generic decorative shapes standing in for the product, no fake Uber/DiDi logos (the ride offer is a neutral white card "NUEVA SOLICITUD"), no city or country, no purple/blue AI gradients, no lazy breathing, no slow back-half pan, no everything-enters-at-once slideshow, no infinite loops.

## Frame 1 — ¿Te conviene?

- scene: Una oferta genérica de viaje entra y un «?» gigante la tacha de duda
- voiceover: "¿Aceptas viajes sin saber si de verdad te conviene?"
- duration: 2.952s
- transition_in: cut
- status: outline
- src: compositions/frames/01-duda.html
- type: hook
- persuasion: Pain validation (pregunta retórica directa al conductor)
- beat: anxiety + curiosity
- asset_candidates:
- blueprint: kinetic-type-beats (Adapt)
- focal: (typography)
- roles: offer card = supporting · question words = hero
- sfx: whoosh-short, pop

Adapt: keep the escalating multi-beat statement + spring-pop payoff; one beat is a non-text element (a neutral ride-offer card).
Scene 1 (0.0–0.9s): black ground; "¿aceptas" slams in huge (display, lowercase, white) upper third — kinetic beat-slam (`kinetic-beat-slam`).
Scene 2 (0.9–1.8s): on "viajes / sin saber", a neutral white offer card (NUEVA SOLICITUD · $150.00 · "A 6 min (3 km)" · "Viaje: 22 min (13 km)" · Aceptar button) rises into the middle, tilted −4°, layered under the word; "sin saber" swaps in place below "¿aceptas" by hard cut. Centered, ~55% of frame, 3 depth layers.
Scene 3 (1.8–end): on "conviene", a giant yellow #FACC15 "?" spring-pops over the card's corner (`spring-pop-entrance`, the one playful overshoot) and the card dims slightly; hold.

narrativeRole: abre en la duda diaria del conductor ante cada oferta.
keyMessage: aceptas a ciegas.

## Frame 2 — $150… ¿cuánto queda?

- scene: «$150» cuenta hacia arriba y se desarma en «¿$?»; abajo «Números de ejemplo»
- voiceover: "Te llega una oferta de 150 pesos... pero ¿cuánto te queda limpio en la bolsa?"
- duration: 4.676s
- transition_in: crossfade
- status: outline
- src: compositions/frames/02-cuanto.html
- type: pain_point
- persuasion: Negative contrast (lo que cobras vs lo que te queda)
- beat: skepticism
- asset_candidates:
- blueprint: dataviz-countup (Adapt)
- focal: (typography)
- roles: amount = hero · "¿cuánto te queda?" = hero 2
- sfx: key-press, glitch-1

Adapt: keep the value-scaled count-up and the push-THROUGH; one stat, then it breaks.
Scene 1 (0.0–1.3s): LIGHT register (#F1F5F2 ground). Small mono kicker "02 / LA OFERTA" top. Big Fira Code "$0" sits center.
Scene 2 (1.3–2.5s): on "150", value-scaled counter $0→$150 grows as it climbs (`counting-dynamic-scale`); label "Números de ejemplo" fades in under it. Centered, ~60% width.
Scene 3 (2.5–3.9s): on "pero ¿cuánto", camera pushes THROUGH the "0" of $150 (`multi-phase-camera` push phase, with a quick motion-blur streak) and lands on a black lowercase "¿cuánto te queda" stacked line, words revealed one by one.
Scene 4 (3.9–end): on "bolsa", a red #F43F5E "¿$?" stamps below; hold still.

narrativeRole: convierte la duda en un número concreto que no cuadra.
keyMessage: la tarifa no es lo que te queda.

## Frame 3 — Checa lee la oferta

- scene: Un teléfono con la oferta; una línea de escaneo verde la recorre y los datos se iluminan (tarifa, minutos, km)
- voiceover: "Checa lee la oferta en cuanto cae a tu pantalla..."
- duration: 2.952s
- transition_in: zoom-through
- status: outline
- src: compositions/frames/03-lee.html
- type: product_intro
- persuasion: Show-don't-tell proof
- beat: curiosity → clarity
- asset_candidates: assets/logo-7eb355dc.svg — logo semáforo de Checa
- blueprint: device-surface-showcase (Adapt)
- focal: (reconstructed phone)
- roles: phone = hero · offer card = inside the phone · scan beam = supporting · logo = supporting (small, top)
- sfx: typing

Adapt: keep the device held as hero with its screen advancing through a real flow (cursorless stepwise); the "flow" is the scan.
Scene 1 (0.0–0.6s): black ground with soft green radial glow; a dark phone frame (rounded, #111413 body, #262B28 bezel) holding the neutral offer card rises in center (power3). Small Checa logo mark + "checa" wordmark in a pill above it.
Scene 2 (0.6–2.0s): on "lee la oferta", a green scan beam sweeps top→bottom across the screen (`svg-path-draw`-style line + glow); as it passes, three fields get green outline boxes in order: "$150.00", "6 min · 3 km", "22 min · 13 km" (`css-marker-patterns` box, sequential).
Scene 3 (2.0–end): on "pantalla", the three captured values fly out of the phone as small green Fira Code chips to the right side, stacking; hold.

narrativeRole: presenta el producto haciendo su trabajo.
keyMessage: Checa lee la oferta sola.

## Frame 4 — El aviso flotante

- scene: El pop-up REAL de Checa cae encima de la oferta (verde, ACEPTAR, $118 netos)
- voiceover: "y de inmediato te muestra un aviso flotante."
- duration: 2.691s
- transition_in: cut
- status: outline
- src: compositions/frames/04-aviso.html
- type: feature_showcase
- persuasion: Show-don't-tell proof
- beat: relief
- asset_candidates: assets/checa-popup.html — pop-up real de Checa
- blueprint: device-surface-showcase (Adapt)
- focal: assets/checa-popup.html
- roles: checa-popup = cutout (hero, foreground) · phone with offer = background (dim ~40%)
- sfx: pop

Adapt: same held phone as frame 3; the next screen state is the overlay appearing.
Scene 1 (0.0–0.6s): the phone with the offer card holds center (continuity from frame 3), slightly dimmed.
Scene 2 (0.6–1.6s): on "muestra", the REAL Checa pop-up (green, ACEPTAR, UBER pill, "$118 netos", "Viaje $150 · Gasolina −$32", fichas $321/h · $9.4/km · 28 min / 16 km total) drops from above onto the top half of the phone with a smooth settle (`spring-pop-entrance` smooth register) and a soft shadow; the phone pushes back (scale down + dim) for depth.
Scene 3 (1.6–end): on "aviso flotante", the pop-up's green band gets a brief glow bloom (`ambient-glow-bloom`); label "Números de ejemplo" under the phone; hold.

narrativeRole: el momento «ajá»: la respuesta aparece sobre la oferta.
keyMessage: un pop-up te dice todo encima de Uber/DiDi.

## Frame 5 — Por hora y por kilómetro

- scene: Las dos fichas del pop-up ($321/h y $9.4/km) se separan y se abren en espejo, cada una con su pastilla
- voiceover: "Te da lo que ganas por hora y por kilómetro."
- duration: 2.534s
- transition_in: push-slide UP
- status: outline
- src: compositions/frames/05-hora-km.html
- type: feature_showcase
- persuasion: Feature-to-benefit translation
- beat: clarity
- asset_candidates: assets/checa-popup.html — fichas del pop-up
- blueprint: comparison-split (Reproduce)
- focal: assets/checa-popup.html
- roles: two fichas (from the pop-up) = hero cards
- sfx: whoosh

Scene 1 (0.0–0.8s): kicker "03 / LO QUE GANAS"; the pop-up from frame 4 sits small at the top (u≈0.6), its two green fichas highlighted.
Scene 2 (0.8–1.6s): on "por hora", the $321/h ficha detaches and flies down-left into a big card (Fira Code number, label "por hora") with mirrored book-open tilt (`split-tilt-cards`).
Scene 3 (1.6–2.3s): on "kilómetro", the $9.4/km ficha does the same to the right; both hold side by side.
Scene 4 (2.3–end): inner-edge pill badges pop: "meta $150/h ✓" and "meta $8/km ✓" (green, the lone overshoot `spring-pop-entrance`); "Números de ejemplo" small below; hold.

narrativeRole: las dos medidas que importan, lado a lado.
keyMessage: sabes cuánto ganas por hora y por km.

## Frame 6 — Ganancia neta

- scene: Ticket: Viaje $150, Gasolina −$32 tacha, y la neta cuenta hacia abajo hasta $118 netos
- voiceover: "Con tu ganancia neta, ya descontada la gasolina."
- duration: 3.109s
- transition_in: crossfade
- status: outline
- src: compositions/frames/06-neta.html
- type: benefit_highlight
- persuasion: Value framing (lo que de verdad te llevas)
- beat: clarity + control
- asset_candidates:
- blueprint: dataviz-countup (Adapt)
- focal: (receipt)
- roles: receipt = hero
- sfx: key-press, chime

Adapt: keep the count as the hero; it counts DOWN from the fare to the net.
Scene 1 (0.0–0.8s): kicker "04 / LO QUE TE QUEDA"; a dark receipt card (#111413, hairline #262B28) with row "Viaje  $150" enters (power3 rise).
Scene 2 (0.8–2.1s): on "neta", a big green Fira Code "$150 netos" sits below the rule; on "descontada… gasolina", the row "Gasolina  −$32" slides in red and the big number counts down $150→$118 (`counting-dynamic-scale`, shrinking slightly as it drops).
Scene 3 (2.1–end): HELD breather — "$118 netos" glows once (`ambient-glow-bloom`) and stays still; "Números de ejemplo" under the card.

narrativeRole: responde la pregunta del frame 2.
keyMessage: ves lo que te queda limpio.

## Frame 7 — El semáforo

- scene: El pop-up fijo al centro; solo su franja cambia de color al ritmo de la voz: verde ACEPTAR → amarillo CONSIDERAR → rojo RECHAZAR
- voiceover: "El semáforo te dice qué hacer: verde, acepta; amarillo, piénsalo; rojo, recházalo."
- duration: 5.616s
- transition_in: zoom-through
- status: outline
- src: compositions/frames/07-semaforo.html
- type: feature_showcase
- persuasion: Rule of three
- beat: confidence
- asset_candidates: assets/checa-popup.html — pop-up en sus 3 niveles
- blueprint: fixed-anchor-cycle (Adapt)
- focal: assets/checa-popup.html
- roles: checa-popup = pinned anchor (hero)
- sfx: click-soft

Adapt: sub-shape A — the pop-up is the pinned anchor and only its band (level) cycles by hard cut; the cycle is also mirrored by a vertical 3-light traffic signal beside it.
Scene 1 (0.0–2.1s): kicker "05 / EL SEMÁFORO"; on "semáforo", a tall traffic-light capsule (like the logo: #111413 body, three dim lights) assembles at the left; the pop-up (u≈0.78) pins to its right, band still grey/neutral.
Scene 2 (2.1–2.7s): on "verde", the green light ignites and the pop-up band hard-cuts to green ACEPTAR (`discrete-text-sequence`).
Scene 3 (2.7–4.0s): on "amarillo", yellow light ignites (green dims), band hard-cuts to #F5B70A CONSIDERAR, numbers swap to a mid offer ($84 netos, fichas $268/h ok · $7.1/km mal).
Scene 4 (4.0–end): on "rojo", red light ignites, band hard-cuts to #EF4444 RECHAZAR, numbers swap ($41 netos, $150/h mal · $4.1/km mal); hold. The pop-up never moves.

narrativeRole: la decisión en un vistazo.
keyMessage: verde, amarillo, rojo: decides en un segundo.

## Frame 8 — Tus metas

- scene: Panel de filtros (Meta por km, Meta por hora con − / +); al subir la meta, el pop-up de al lado cambia en vivo de ACEPTAR a CONSIDERAR
- voiceover: "Tú pones tus metas de cuánto quieres ganar."
- duration: 2.325s
- transition_in: push-slide LEFT
- status: outline
- src: compositions/frames/08-metas.html
- type: feature_showcase
- persuasion: Status seeking / control (tú mandas)
- beat: control
- asset_candidates: assets/checa-popup.html — pop-up que reacciona a la meta
- blueprint: panel-edit-live-sync (Reproduce)
- focal: assets/checa-popup.html
- roles: goals panel = control · checa-popup = bound target
- sfx: click

Scene 1 (0.0–0.6s): kicker "06 / TUS METAS"; upper half: a dark settings card "Meta por km  $8" with − / + buttons (green +), Fira Code value; lower half: the pop-up (u≈0.75) showing $9.4/km ok, ACEPTAR.
Scene 2 (0.6–1.5s): on "metas", the + button presses (`press-release-spring`) and the value ticks $8→$9→$10 (`discrete-text-sequence`); live-sync: in the same beat the pop-up's $9.4/km ficha flips to red "mal" and its band hard-cuts to CONSIDERAR (yellow).
Scene 3 (1.5–end): on "ganar", a second row "Meta por hora $150" appears under the first; hold with both coupled surfaces visible.

narrativeRole: el conductor pone las reglas.
keyMessage: Checa juzga con tus metas.

## Frame 9 — Recogida lejos

- scene: Pop-up rojo RECHAZAR; la ficha «6 km recogida» late en rojo con un mapa de recogida lejana detrás
- voiceover: "Y si la recogida queda muy lejos, te avisa de inmediato."
- duration: 3.239s
- transition_in: push-slide LEFT
- status: outline
- src: compositions/frames/09-recogida.html
- type: feature_showcase
- persuasion: Loss aversion (kilómetros que nadie paga)
- beat: relief
- asset_candidates: assets/checa-popup.html — pop-up rojo con ficha de recogida
- blueprint: device-surface-showcase (Adapt)
- focal: assets/checa-popup.html
- roles: checa-popup (red) = hero · mini route map = background (dim ~40%)
- sfx: error

Adapt: the surface is the pop-up itself; the flow step is the pickup check failing.
Scene 1 (0.0–1.2s): kicker "07 / RECOGIDA"; a minimal line-art route map draws itself behind (`svg-path-draw`): a car dot far away from a pin, dashed path labeled "6 km" in red — "recogida" cue.
Scene 2 (1.2–2.0s): on "lejos", the red pop-up (RECHAZAR, "$71 netos", "Viaje $120 · Gasolina −$49", fichas $212/h ok · $4.9/km mal · "6 km recogida" mal) drops over the map (smooth settle).
Scene 3 (2.0–end): on "avisa", the "6 km recogida" ficha gets a red marker circle (`css-marker-patterns`) and one short shake (finite, 3 cycles max, no yoyo/repeat); hold.

narrativeRole: protege al conductor de la trampa más común.
keyMessage: no más recogidas lejanas gratis.

## Frame 10 — La mejor de dos

- scene: Dos pop-ups (Uber y DiDi) entran desde lados opuestos; la mejor recibe borde dorado y estrella, la otra se apaga
- voiceover: "Si caen dos ofertas, te marca la mejor."
- duration: 2.325s
- transition_in: crossfade
- status: outline
- src: compositions/frames/10-mejor.html
- type: feature_showcase
- persuasion: Show-don't-tell proof
- beat: confidence
- asset_candidates: assets/checa-popup.html — variantes cp-oro y cp-apagada
- blueprint: comparison-split (Adapt)
- focal: assets/checa-popup.html
- roles: two pop-ups = hero cards (Uber wins)
- sfx: whoosh-short, sparkle

Adapt: the two cards are two full pop-ups stacked (vertical canvas): top from the left wing, bottom from the right wing, mirrored tilts; the badge is the gold "MEJOR" pill.
Scene 1 (0.0–1.1s): kicker "08 / DOS OFERTAS"; on "dos ofertas", pop-up A (UBER, verde ACEPTAR, $118 netos) enters from the left tilted, pop-up B (DIDI, amarillo CONSIDERAR, $84 netos) from the right ~0.2s later (`split-tilt-cards`), both u≈0.72, stacked top/bottom.
Scene 2 (1.1–1.6s): on "marca", B dims (cp-apagada, opacity ~0.5, slight scale-down).
Scene 3 (1.6–end): on "mejor", A switches to cp-oro (thick gold border + star) and a gold pill "★ LA MEJOR" pops at its inner edge (the lone overshoot); hold.

narrativeRole: pantalla dividida resuelta.
keyMessage: con dos ofertas, Checa elige la mejor.

## Frame 11 — Mejores horas

- scene: Cuadrícula de horas que se arma en cascada con colores de semáforo; las mejores (17–19 h) se encienden
- voiceover: "También te enseña a qué horas pagan mejor."
- duration: 2.351s
- transition_in: zoom-through
- status: outline
- src: compositions/frames/11-horas.html
- type: benefit_highlight
- persuasion: Future pacing (planea tu turno)
- beat: aspiration
- asset_candidates:
- blueprint: grid-card-assemble (Reproduce)
- focal: (hour grid)
- roles: hour tiles = hero
- sfx: typing

Scene 1 (0.0–0.5s): kicker "09 / MEJORES HORAS"; empty grid outline 4×4.
Scene 2 (0.5–1.6s): on "enseña… horas", 16 hour tiles (6 to 21 h, Fira Code numbers) cascade in with a stagger in reading order (`grid-card-assemble` stagger), colored green / yellow / red (traffic-light colors; most green at 8–9, 13, 17–19).
Scene 3 (1.6–end): on "pagan mejor", the 17, 18, 19 tiles light up (glow + slight lift) and a line "lo mejor que viene: 17–19 h" types below (`discrete-text-sequence`); HELD breather after.

narrativeRole: de una oferta al día entero.
keyMessage: sabes cuándo salir a manejar.

## Frame 12 — Más dinero

- scene: «Menos viajes malos» se tacha y «más dinero en tu cuenta» explota en verde
- voiceover: "Menos viajes malos, más dinero en tu cuenta."
- duration: 2.586s
- transition_in: crossfade
- status: outline
- src: compositions/frames/12-resultado.html
- type: benefit_highlight
- persuasion: Value stacking
- beat: triumph
- asset_candidates:
- blueprint: kinetic-type-beats (Reproduce)
- focal: (typography)
- roles: words = hero
- sfx: impact-bass-1

Scene 1 (0.0–1.0s): GREEN register (#22C55E ground, #04120A type). "menos viajes malos" slams in lowercase display, three words per-beat (`kinetic-beat-slam`).
Scene 2 (1.0–1.8s): on "más", a hand-drawn strike scribbles through "viajes malos", and "más dinero" slams in huge below it.
Scene 3 (1.8–end): on "cuenta", "en tu cuenta" lands smaller; a tiny upward arrow ticks; hold.

narrativeRole: el resultado en una frase.
keyMessage: ganas más.

## Frame 13 — Pruébala 7 días

- scene: El semáforo del logo se arma pieza por pieza, se enciende el verde, aparece «Checa» y la pastilla «Pruébala 7 días sin costo»
- voiceover: "Pide Checa y pruébala 7 días sin costo."
- duration: 2.821s
- transition_in: blur-crossfade
- status: outline
- src: compositions/frames/13-cta.html
- type: cta
- persuasion: Risk reversal
- beat: urgency-to-act
- asset_candidates: assets/logo-7eb355dc.svg — logo semáforo de Checa
- blueprint: logo-assemble-lockup (Adapt)
- focal: assets/logo-7eb355dc.svg
- roles: logo-7eb355dc = hero mark
- sfx: riser, chime

Adapt: parts-assembly variant + final CTA pill instead of a URL push-through.
Scene 1 (0.0–0.9s): black ground with a green radial bloom; the traffic-light capsule from the logo assembles: body draws (`svg-path-draw`), then red, yellow, green lights pop in sequence on a smooth settle.
Scene 2 (0.9–1.6s): on "Checa", the green light ignites with glow and the wordmark "checa" (Fira Sans 700, lowercase, white) slides out from behind the mark to complete the lockup.
Scene 3 (1.6–end): on "pruébala 7 días", a green pill "Pruébala 7 días sin costo" rises under the lockup (power3); hold; the final 0.4s fades to black (the video's only exit).

narrativeRole: cierre y acción.
keyMessage: pruébala gratis 7 días.
