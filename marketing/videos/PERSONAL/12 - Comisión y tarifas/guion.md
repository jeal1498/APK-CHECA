# Guion · 12 - Comisión y tarifas (PERSONAL, v2)

v1: `marketing/videos/CHECA/12 - Comisión y tarifas/` (publicada 23 oct) · v2: esta carpeta (~38 s con cierre estándar; publica sáb 24 oct). **Estado: guion aprobado; tabla aprobada; portada para aprobar.**

## Auditoría de la v1
- El gancho abre con la comisión (algo que no se controla) y la queja dura 2 cortes antes de llegar a los $100.
- El payoff ($72 vs $84) llega hasta el corte 5 (0:15); el hook debe plantear la contradicción de inmediato.
- «Link del perfil» no aplica a la cuenta personal ni al cierre estándar.

## Voz limpia (sin etiquetas, para ElevenLabs)
Un viaje de $100 pesos no te deja $100 pesos. [Corte] Te cae una oferta de $100 con 14 kilómetros en total, y otra, también de $100, con solo 8. [Corte] Se ven igual de buenas, pero ya con la gasolina descontada, la primera te deja $72 y la segunda $84. [Corte] Son $12 pesos de diferencia por el mismo precio. [Corte] Y si haces 20 viajes al día, son $240 pesos que se quedan en el camino. [Corte] La comisión no la puedes cambiar, pero qué viaje aceptas, eso sí lo decides tú. [Corte] Con la app Checa lo ves antes de aceptar, porque te marca en verde el que conviene. [Corte] Sígueme para más consejos. [Corte] Descarga Checa y checa cuánto te queda de cada viaje.

| Corte | Tiempo | Voz | En pantalla | Técnica |
|---|---|---|---|---|
| 1 | 0:00–0:03 | Un viaje de $100 pesos no te deja $100 pesos. | Billete de $100 que se reduce al restarle comisión y gasolina | [Hook Inicial: contradicción] |
| 2 | 0:03–0:09 | Te cae una oferta de $100 con 14 kilómetros en total, y otra, también de $100, con solo 8. | Dos ofertas de $100; cotas 14 km y 8 km | [Setup] |
| 3 | 0:09–0:15 | Se ven igual de buenas, pero ya con la gasolina descontada, la primera te deja $72 y la segunda $84. | $72 frente a $84 («números de ejemplo») | [Payoff] |
| 4 | 0:15–0:18 | Son $12 pesos de diferencia por el mismo precio. | +$12 | [Énfasis] |
| 5 | 0:18–0:23 | Y si haces 20 viajes al día, son $240 pesos que se quedan en el camino. | Rejilla de 20 puntos que suma $240 | [Re-enganche] |
| 6 | 0:23–0:28 | La comisión no la puedes cambiar, pero qué viaje aceptas, eso sí lo decides tú. | Candado en «Comisión», mano en «Tus viajes» | [Giro] |
| 7 | 0:28–0:32 | Con la app Checa lo ves antes de aceptar, porque te marca en verde el que conviene. | Pop-up ACEPTAR verde y CONSIDERAR amarillo | [Solución] |
| 8 | 0:32–0:34 | Sígueme para más consejos. | Botón «+ Seguir» | [CTA] |
| 9 | 0:34–0:38 | Descarga Checa y checa cuánto te queda de cada viaje. | Cierre estándar | [Cierre] |

**Honestidad:** cifras solo como ejemplo («números de ejemplo» en pantalla); la gasolina depende del km/l y $/l que configure el conductor.

## Animaciones y gráficos HTML por toma (marca v2: asfalto #0C0C10, hueso #F4F4EF; verde/amarillo/rojo solo para decidir)
| Toma / frase | Componente HTML/UI | Efecto/animación CSS | Objetivo visual |
|---|---|---|---|
| 1 · 0:00 «Un viaje de $100 no te deja $100» | Billete «$100» en hueso sobre asfalto | Se parte en tres: pedazos «comisión» y «gasolina» salen volando; queda uno más chico | Contradicción en 1 s |
| 2 · 0:03 «oferta de $100 con 14 km… otra con 8» | Dos tarjetas de oferta «$100» lado a lado + cotas | Entran de lados opuestos; cotas «14 km» y «8 km» se dibujan bajo cada una | Mismo precio, distinta distancia |
| 3 · 0:09 «te deja $72… $84» | Las dos tarjetas con la gasolina restándose; «Números de ejemplo» | $100 baja contando a $72 y a $84; el $84 se enciende verde, el $72 queda hueso | Payoff de la cuenta |
| 4 · 0:15 «$12 de diferencia» | Pastilla «+$12» entre las dos | Entra con curva suave y sin rebote | Énfasis en la cifra |
| 5 · 0:18 «20 viajes… $240» | Rejilla de 20 puntos + contador | Los puntos se llenan de uno en uno; el contador sube a «$240» | Dimensión acumulada |
| 6 · 0:23 «la comisión no… tú decides» | Dos filas: «Comisión» con candado, «Tus viajes» con mano | El candado cierra; la fila «Tus viajes» se enciende | Control del conductor |
| 7 · 0:28 «te marca en verde el que conviene» | Dos pop-ups reales del mismo tamaño: CONSIDERAR (amarillo) y ACEPTAR (verde) | Entran con curva suave; el verde queda al frente | Solución visible |
| 8 · 0:32 «Sígueme» | Pastilla + botón «+ Seguir» | Botón pulsa una vez | CTA claro |
| 9 · 0:34 «Descarga Checa…» | Cierre estándar | Logo, pastilla «Descarga CHECA», @checa.driver, @Eduardoalv9802 y link; pausa 0.45 s | Cierre de marca |
