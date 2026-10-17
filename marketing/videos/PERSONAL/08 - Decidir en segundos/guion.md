# Guion · 08 - Decidir en segundos (PERSONAL, v2)

## 08 · Decidir en segundos (v2, optimizado para retención)
v1: `marketing/videos/CHECA/08 - Decidir en segundos/` · v2: `marketing/videos/PERSONAL/08 - Decidir en segundos/` (~40 s con cierre estándar; publica mar 20 oct, un día detrás de CHECA)

## Auditoría de la v1
- El gancho («Te cae una oferta y solo tienes unos segundos») describe la situación, pero no duele: no dice qué pierdes.
- Los cortes 2 y 3 enumeran datos y luego «no da tiempo»: dos cortes seguidos de queja sin avance.
- «Menos de un segundo» promete más de lo que la voz de la v1 dijo («en un segundo»); mejor «cuando llega la oferta».
- Nunca se muestra el payoff con la cuenta hecha: el $96 netos aparece solo al final y sin explicar de dónde sale.
- El CTA («link del perfil») no aplica a la cuenta personal ni al cierre estándar.

## Voz limpia (sin etiquetas, para ElevenLabs)
Aceptaste sin pensarlo, y a mitad del viaje ya sabías que perdiste dinero. [Corte] No fue descuido: la oferta dura segundos y nadie alcanza a sacar la cuenta. [Corte] Tarifa, kilómetros, minutos de recogida, y falta lo que se va en gasolina. [Corte] Esa cuenta te la hace la app Checa. [Corte] Lee la pantalla cuando llega la oferta y te dice cuánto te queda neto, ya con la gasolina descontada. [Corte] Y te lo dice en voz alta: Aceptar, Considerar o Rechazar. [Corte] Ves el color o escuchas la palabra, y sigues manejando. [Corte] Decides en segundos, pero con la cuenta ya hecha. [Corte] Pruébala 7 días sin costo. Sígueme para más consejos. [Corte] Descarga Checa y checa cuánto te queda de cada viaje.

| Corte | Tiempo | Voz | En pantalla / sonido | Técnica |
|---|---|---|---|---|
| 1 | 0:00–0:04 | Aceptaste sin pensarlo, y a mitad del viaje ya sabías que perdiste dinero. | Botón «Aceptar» que se toca, luego tarjeta roja «Perdiste dinero» en el mapa; impact-bass | [Hook Inicial: vivencia reconocible, voz + texto + imagen a la vez] |
| 2 | 0:04–0:09 | No fue descuido: la oferta dura segundos y nadie alcanza a sacar la cuenta. | Oferta con círculo de tiempo que se agota | [Micro-hook: quita la culpa y abre la tensión] |
| 3 | 0:09–0:13 | Tarifa, kilómetros, minutos de recogida, y falta lo que se va en gasolina. | Datos en cascada y, al final, aguja de gasolina vacía parpadeando | [Open Loop: falta una pieza de la cuenta] |
| 4 | 0:13–0:16 | Esa cuenta te la hace la app Checa. | Logo cae sobre la oferta; click | [Giro: la solución entra] |
| 5 | 0:16–0:22 | Lee la pantalla cuando llega la oferta y te dice cuánto te queda neto, ya con la gasolina descontada. | Oferta escaneada por línea verde; el neto aparece restando la gasolina («números de ejemplo») | [Payoff del Open Loop + Re-enganche visual] |
| 6 | 0:22–0:26 | Y te lo dice en voz alta: Aceptar, Considerar o Rechazar. | Ondas de audio que cambian a verde, amarillo y rojo con cada palabra | [Cambio de ritmo: tres palabras cortas] |
| 7 | 0:26–0:30 | Ves el color o escuchas la palabra, y sigues manejando. | Pop-up verde ACEPTAR sobre la oferta, pantalla estable | [Pausa dramática: respiro visual] |
| 8 | 0:30–0:33 | Decides en segundos, pero con la cuenta ya hecha. | Contador de tiempo en 0:03 junto a ✓ verde | [Giro emocional: el alivio] |
| 9 | 0:33–0:37 | Pruébala 7 días sin costo. Sígueme para más consejos. | Pastilla «7 días sin costo», botón «+ Seguir» | [CTA específico] |
| 10 | 0:37–0:41 | Descarga Checa y checa cuánto te queda de cada viaje. | Cierre estándar: logo, «Descarga CHECA», @checa.driver, @Eduardoalv9802 y el link | [Cierre estándar: eslogan + cuentas + link] |

## Animaciones y gráficos HTML por toma (marca v2: asfalto #0C0C10, hueso #F4F4EF; verde/amarillo/rojo solo para decidir)
| Toma / frase | Componente HTML/UI | Efecto/animación CSS | Objetivo visual |
|---|---|---|---|
| 1 · 0:00 «Aceptaste sin pensarlo… perdiste dinero» | Botón «Aceptar» + tarjeta roja «Perdiste dinero» | Toque con onda; tarjeta cae con sacudida roja | Dolor reconocible en 1 s |
| 2 · 0:04 «la oferta dura segundos» | Oferta genérica con anillo de tiempo | El anillo se vacía; la oferta se oscurece | Presión de tiempo |
| 3 · 0:09 «falta lo que se va en gasolina» | Fichas Tarifa / Km / Minutos + aguja de gas | Fichas entran en cascada; aguja cae a vacío y parpadea | Falta la pieza clave |
| 4 · 0:13 «la app Checa» | Logo del semáforo con ✓ | Rojo → ámbar → verde → ✓ en 1.2 s | Entra la solución |
| 5 · 0:16 «cuánto te queda neto» | Teléfono con oferta + línea de escaneo + resta de gasolina | Línea verde baja; el neto cuenta de bruto a neto | Muestra de dónde sale la cuenta |
| 6 · 0:22 «Aceptar, Considerar o Rechazar» | Ondas de audio + tres palabras | Cada palabra enciende su color (verde, amarillo, rojo) | Los tres estados, mismo tamaño |
| 7 · 0:26 «ves el color o escuchas» | Pop-up real ACEPTAR sobre la oferta | Entra con curva suave; sin rebote | Se ve el pop-up y la oferta completos |
| 8 · 0:30 «con la cuenta ya hecha» | Reloj en 0:03 + ✓ verde | Ticks del reloj, ✓ se dibuja | Alivio |
| 9 · 0:33 «7 días sin costo… sígueme» | Pastilla + botón «+ Seguir» | Pastilla entra en máscara; botón pulsa | CTA claro |
| 10 · 0:37 «Descarga Checa…» | Cierre estándar | Logo, pastilla «Descarga CHECA», cuentas y link; pausa 0.45 s | Cierre de marca |

**Ajustes de honestidad:** «ya con la gasolina descontada» depende del km/l y $/l que el conductor configure; cifras solo en pantalla y marcadas «números de ejemplo»; sin cifras habladas ni «menos de un segundo»; la prueba de 7 días sin costo es la oferta vigente de los videos de venta.

**Texto de publicación (v2):**
Aceptaste el viaje sin pensarlo y perdiste dinero 😤
En Uber y DiDi la oferta dura segundos y nadie alcanza a sacar la cuenta de la gasolina. Así decido si me conviene el viaje antes de aceptar.
¿Cuántas veces aceptaste a ciegas? 👇
Descarga CHECA y checa cuánto te queda de cada viaje (solo Android).
#choferuber #choferdidi #conductores #gasolina #uberdidi
