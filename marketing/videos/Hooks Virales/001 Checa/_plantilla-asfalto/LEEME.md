# Plantilla «Asfalto» · videos de Hooks Virales CHECA

**Todos los videos de `001 Checa` se hacen con este estilo** (aprobado por Eduardo el 13 nov 2026). Primer video: `001 …` (ejemplo completo).

## Cómo empezar un video nuevo
1. Copia la carpeta `produccion/` de aquí dentro de la carpeta del video (`001 Checa/NNN …/produccion/`; misma profundidad, así las fuentes cargan).
2. En `estimar_tiempos.py` pon las frases del guion y córrelo (`python3 -I estimar_tiempos.py`).
3. En `pagina.html` reescribe solo los bloques `// ── 1 ·` … `// ── 6 ·` (una acción por frase). No toques el fondo, los subtítulos ni el CSS.
4. `NODE_PATH=$(npm root -g) node render.js hoja` → revisa `renders/storyboard.png`; `node render.js video` → borrador para Eduardo.
5. Con su grabación: tiempos reales en `palabras.json` (Whisper), `node render.js video llave` y `bash armar.sh <clip> [0–1]`.

## Qué define el estilo (no cambia de un video a otro)
- **Pantalla:** Eduardo a cámara en el 45 % de arriba (1080×864), centrado y con los ojos en el tercio superior (y=288); gráficos centrados en el 55 % de abajo (zona 808×480 en 136, 916).
- **Identidad de Checa:** asfalto `#0C0C10`, tablero `#16161C`, hueso `#F4F4EF`; verde, amarillo y rojo solo para decidir; Bricolage Grotesque 800, DM Sans y JetBrains Mono.
- **Fondo:** la calle de noche vista desde el auto: luces del semáforo desenfocadas alrededor de los gráficos y, abajo, la carretera con la raya punteada amarilla que avanza.
- **Gráficos:** pop-up igual al de la app (`Popup.kt`), con borde y brillo del color del veredicto; oferta de ejemplo en tarjeta blanca; datos en fichas tablero con borde amarillo; semáforo de Checa con ✓ en el cierre.
- **Cifras:** siempre con las fórmulas de `Parser.kt` (metas $200/h y $10/km, gasolina 12 km/l a $24) y el letrero «NÚMEROS DE EJEMPLO» mientras estén en pantalla.
- **Subtítulos estilo B:** mayúsculas con contorno negro sobre la frontera, sin caja; solo se destaca la palabra que suena, en el momento exacto; la raya amarilla se abre para dejarlos pasar.
- **Reglas de animación:** una acción clara por bloque; textos de máx. 4 palabras; analogías literales; escala para proporciones; los elementos se transforman de un bloque al siguiente; cada bloque cierra con una imagen clara; nada vacío esperando su texto; todo cae en su palabra.
- **Marca:** sin logos ni nombres de apps de viajes, sin ciudad ni país, sin promesas de ganancia.
