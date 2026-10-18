---
name: marca-checa
description: Identidad de marca de Checa (v2, 17 oct 2026): logo del semáforo con ✓, paleta asfalto + hueso con semáforo aceptar/considerar/rechazar, tipografías, voz y tono, recursos gráficos, plantillas, subtítulos, movimiento y avisos legales. Úsala SIEMPRE que haya que diseñar, escribir o revisar algo con la marca de Checa: logo, colores, portadas, videos, web, app, pop-up, redes, textos de venta o stickers. La fuente única vive en el repo jeal1498/checa-web, carpeta marca/; no guardar copias en este repo.
---

# Marca Checa (v2)

## Dónde está la marca
**Fuente única: repo `jeal1498/checa-web`, carpeta `marca/`** (publicada en https://jeal1498.github.io/checa-web/marca/).
Contiene: `semaforo*.svg`, `logo-*.svg`, `icono-app.svg`, `tokens.json`, `tokens.css`, `colors-android.xml` y un `README.md`.

Antes de diseñar o escribir:
1. Si `/home/user/checa-web` no existe, clónalo: `git clone --depth 1 https://github.com/jeal1498/checa-web /home/user/checa-web` (si no hay acceso, `add_repo` de `jeal1498/checa-web`; para escribir, con acceso `push`).
2. Lee `marca/README.md` y `marca/tokens.json`; usa esos valores, no los de memoria ni los de este resumen si difieren.
3. **Nunca dupliques la marca en este repo.** Si cambia, se edita en `checa-web/marca/` y se avisa.

## Resumen (por si no hay acceso; manda `checa-web`)
- **Logo:** semáforo oscuro con luz roja y ámbar apagadas y la **luz verde encendida con una ✓**. Más el nombre «Checa» (C mayúscula). Versiones: oscuro, claro, una tinta negra, una tinta blanca. El semáforo ya es conocido: **no se cambia**. Espacio libre = ancho de la luz verde; mínimo 32 px (completo) y 24 px (solo semáforo); nunca estirar, girar ni cambiar las luces.
- **Colores:** asfalto `#0C0C10` (fondo), tablero `#16161C` (tarjetas), hueso `#F4F4EF` (marca, botones), humo `#3A3A45`. **El color solo decide:** aceptar `#22C55E` (= verde del logo), considerar `#FACC15` (amarillo de la raya de la calle), rechazar `#EF4444`. Texto sobre ellos: `#04210F`, `#2B2200`, `#1F0000`.
- **Tipografía:** Bricolage Grotesque 800 (títulos y números), DM Sans (texto), JetBrains Mono (datos y etiquetas). Respaldo: Arial Black, Arial, Courier.
- **Voz:** directa, honesta, rápida, de compañero. Tagline: «Checa cuánto te queda». Palabras: lana, te queda, netos, por km, por hora, recogida. Nunca: promesas de ganancia, tecnicismos (OCR, algoritmo), ciudad o país, nombres de competencia.
- **Pop-up:** los tres estados miden **exactamente lo mismo** (alto, ancho, fichas); solo cambian color y datos. En pantalla dividida, también iguales; la mejor lleva borde hueso y etiqueta «Mejor». Ningún texto es solo un monto.
- **Recursos:** raya de carretera punteada amarilla con una luz verde, íconos de línea (trazo 2.2), tres puntos del semáforo, sello inclinado, todo redondeado.
- **Subtítulos de video:** máx. 2 líneas y 5 palabras; tercio medio-bajo (evitar 12 % arriba y 22 % abajo); sombra oscura; palabra por palabra a 150 ms. Verde = lo que ganas, amarillo = palabra a recordar, rojo = lo que pierdes, hueso = lo demás.
- **Movimiento:** logo rojo → ámbar → verde → ✓ en 1.2 s; curva `cubic-bezier(.2,.8,.2,1)`, sin rebote; con «reducir movimiento», solo cortes.
- **Uber y DiDi:** sin sus logos ni colores; pantallas genéricas. Aviso: «Checa es independiente y no está afiliada a Uber ni a DiDi». Números de ejemplo siempre marcados.
- **Pantalla final de videos:** logo + «Descarga Checa» + `@checa.driver` + `@Eduardoalv9802` + `jeal1498.github.io/checa-web` (solo Android). En los videos que van **dentro de la web** el cierre va sin cuentas ni link (`"sin_redes": true`), porque la web no tiene salidas.

## Estado
- Aprobada por Eduardo el 17 oct 2026. **Ya se aplica a la web** (17 oct 2026, `checa-web`) **y a la app Android** (v61, 18 oct 2026: `Tema.kt` con asfalto/tablero/hueso, letras en `assets/fuentes/` —Bricolage 800, DM Sans y JetBrains Mono convertidas de los woff2 de `checa-web/fonts/`—, botones hueso en píldora, logo `res/drawable/semaforo.xml` e ícono `ic_launcher_frente.xml`). **Pop-up con la marca v2 desde v65** (Eduardo eligió la propuesta A «Franja», maqueta https://claude.ai/artifact/YD5aQPLwyMABZ11dWVmAuh): mismo acomodo de siempre con fondo tablero, letras de la marca y fichas pintadas. **Videos:** plantilla en marca v2, `video/hyperframes/checa-marca-v2/` (la vieja `checa-promo` queda como legado).
- Pendientes (PNG del ícono ya hechos en `checa-web/marca/png/`): láminas exportadas a `checa-web/marca/laminas/`, fotografía y video real.
