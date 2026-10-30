# Escenas gráficas para videos con Eduardo a cámara

Eduardo habla a cámara; las escenas llenan la pantalla unos segundos para ilustrar una frase y regresan a él sin cortar su voz. Los subtítulos siguen encima. Los números siempre son de ejemplo y cuadran.

## Qué escena entra según lo que dice

| Eduardo dice… | Escena | Ejemplo |
|---|---|---|
| Una oferta de viaje y lo que te queda | `popup` | pop-up rojo, neta $88 de un viaje de $150 |
| «Se veía bueno… pero» | `comparar` | arriba «Se veía bueno · $150 de tarifa», abajo en rojo «Te quedó · $88 netos» |
| Pasos para decidir | `pasos` | «Mira la oferta · Checa lo que te queda · Tú decides» (la última luz es verde) |
| Una cifra que duele o convence | `numero` | $/h, $/km o netos grandes, con el color del semáforo |
| Aceptar, considerar, rechazar | `lista` con `nivel` | tres puntos verde, amarillo y rojo |
| Una regla para recordar | `dato` | «Checa antes de aceptar», con la raya de carretera |

## Reglas

- Una idea por escena, de 3 a 5 s, máximo 12 palabras en pantalla.
- Las escenas suman como máximo el 40 % del video; Eduardo a cámara es lo que da confianza.
- Entran 0.2 s antes de la palabra clave (usa `en` para anclar cada punto a una palabra).
- Ninguna escena coincide en tiempo con el gancho, el rótulo ni la pantalla final.
- El contenido va entre y=250 y y=1120: los subtítulos ocupan y=1160 a 1480.
- El color solo decide: verde = lo que ganas, amarillo = lo que recuerdas, rojo = lo que pierdes. Nunca como decoración.
- El pop-up sale de `video/hyperframes/checa-marca-v2/assets/checa-popup.html` sin rediseñarlo; la neta debe ser viaje − gasolina.
- Sin logos de Uber ni DiDi (`"app": "APP"`), sin ciudad ni país, sin promesas de ganancia.

## Ritmo de un video de 60 s (ejemplo)

| Segundos | Qué pasa |
|---|---|
| 0–3 | Gancho en pantalla con la vivencia del conductor; Eduardo a cámara |
| 3–7 | Rótulo con su nombre |
| 8–12 | `popup` con la oferta |
| 14–20 | Eduardo a cámara |
| 20–25 | `comparar` |
| 28–34 | Eduardo a cámara + `pasos` |
| 38–42 | `numero` |
| 42–52 | Eduardo a cámara, «sígueme» |
| 52–60 | Pantalla final estándar |

Escenas: 18 s de 58 s (31 %).
