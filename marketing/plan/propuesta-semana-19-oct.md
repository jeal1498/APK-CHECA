# Propuesta de cambios para los videos de la semana del 19 oct 2026

Hecha el 19 oct 2026 a partir de las estadísticas de TikTok Studio de las dos cuentas (capturas que mandó Eduardo; los números quedaron en `marketing/metricas/videos.csv`). **Es una propuesta: nada de esto está aplicado todavía.** Muestra chica (8 videos, 5–6 días de vida), así que son tendencias, no reglas.

## Qué dicen los datos

| Cuenta | Video | Vistas | Tiempo prom. | Completo | Comentarios / compartidos / guardados |
|---|---|---|---|---|---|
| Personal | Busco 50 conductores (13 oct, 42.6 s) | 2,602 | 16.4 s (38 %) | 11.8 % | 101 / 12 / 7 |
| Personal | Lo que la app no te dice (14 oct, 40.6 s) | 1,938 | 19.1 s (47 %) | 18.1 % | 57 / 4 / 12 |
| Personal | El hábito #4 no se negocia (16 oct, 34 s) | 169 | 10.8 s (32 %) | 9.1 % | 2 / 2 / 0 |
| CHECA | 04 · 6 km que nadie te paga (15 oct, 31.5 s) | 996 | 13.4 s (43 %) | 9.7 % | 2 / 0 / 2 |
| CHECA | 02 · 12 horas y la lana no aparece (13 oct, 36.5 s) | 499 | 10.9 s (30 %) | 8.9 % | 0 / 1 / 1 |
| CHECA | 05 · 12 horas sentado (16 oct, 35.1 s) | 587 | 8.2 s (23 %) | 3.6 % | 0 / 1 / 3 |
| CHECA | 03 · 5 consejos para manejar de noche (14 oct, 41.8 s) | 676 | 7.1 s (17 %) | 2.8 % | 0 / 2 / 0 |
| CHECA | Ayuda Android (16 oct, 4:04 min) | 715 | 14.2 s (6 %) | 1.3 % | 5 / 8 / 2 |

Cuenta CHECA, 7 días (11–17 oct): 3.2K vistas, 40 visitas al perfil, 104 me gusta, 5 comentarios, 11 compartidos, 11 seguidores nuevos. Cuenta personal, 7 días: 4.4K vistas, 62 visitas al perfil, 59 me gusta, 150 comentarios, 13 compartidos, 11 seguidores netos.

Lecciones:
1. **La mayoría se va a los 2 s en todos los videos**: el primer cuadro y la primera frase deciden.
2. **Ganan los pesos y la comparación.** Retienen mejor «6 km que nadie te paga», «12 horas y la lana no aparece» y, en la cuenta personal, los de la pantalla de la oferta. Los «4 tips» generales de salud y seguridad retienen peor (17–23 %).
3. **Comentar funciona si se pide**: el «comenta 70» de «Busco 50» sacó 101 comentarios; los videos de CHECA casi no tienen.
4. **El impulso dura un día**: casi todo el alcance llega el primer día y por «Para ti» (87–97 %). Casi nada viene del perfil.
5. **Búsquedas en TikTok**: en CHECA el 63 % son de la marca («uber checa», «Checa», «Checa.driver»). En los de la cuenta personal: «app para calcular viajes DiDi», «mejores ganancias en Uber», «cuánto vale el kilómetro Uber o DiDi», «cómo saber tus ganancias de DiDi», «app que te diga si te conviene un viaje», y dudas sueltas («si le das un cinturonazo, el pasaje…», «qué pasa si me dan en efectivo…»).
6. «Ayuda Android» dura más de 4 minutos: casi nadie lo termina, pero tiene los mejores compartidos por vista. Es video de ayuda, no de alcance; no compararlo con los demás.
7. Horario: se publicó a horas distintas (8:57, 10:49, 13:01, 15:23, 17:43) y no hay base para elegir una. El calendario sugiere 8 pm sin datos.

## Cambios propuestos (sin re-renderizar los videos)

**1. Primer cuadro con el número desde el segundo 0.** La portada se usa como primer cuadro (`video/scripts/portada_primer_cuadro.sh`), así que cambiarla es barato. Cada portada necesita la aprobación de Eduardo (flujo por etapas).
- 06 Gasolina cara: «$90 de oferta, $30 se van en gasolina».
- 10 Mejores horas: «$6 por km a las 3 pm. $11 a las 7».
- 12 Comisión y tarifas: «Dos ofertas de $100. Una te deja $12 más» (el gancho más fuerte de la semana).
- 08 Decidir en segundos y 14 Miedo a rechazar: cifra de ejemplo (con «números de ejemplo») en lugar de frases generales.
- Consejos 07, 09, 11, 13: no abrir con «4 tips», sino con lo que cuesta no hacerlo. Ejemplos: «Cuenta desactivada = $0 desde mañana», «Ganas $900 hoy, $150 mañana». Cifras siempre rotuladas como ejemplo; no inventar datos de multas ni de leyes.

**2. Texto de la publicación (`copy.txt`, sin tocar el video).**
- Videos de venta: cerrar con «Comenta CHECA y te paso el link» (ya existe el envío por mensaje directo con `dm.txt` y su UTM para medir descargas) en lugar de «Descarga CHECA…».
- Segunda línea con lo que la gente sí busca: «app para calcular viajes DiDi», «cuánto vale el kilómetro Uber o DiDi», «cómo saber tus ganancias de DiDi».

**3. Horario.** Alternar 1 pm y 8 pm durante la semana y anotar la hora de cada publicación en `videos.csv` (hoy el archivo no tiene columna de hora; agregarla o ponerla en el nombre).

**4. Videos nuevos a partir de las búsquedas** (el slot del 13 «Multas y papeles» es el candidato a ceder, por ser el más genérico y sin demanda de búsqueda vista):
- «¿Cuánto vale el kilómetro?» y «Cómo saber tus ganancias netas».
- Uno corto (~30 s) sobre Play Protect bloqueando la instalación, porque «Ayuda Android» tuvo muchos compartidos pero es muy largo.

**Cuenta personal:** repetir el estilo que funcionó (pantalla de la oferta, número claro y llamada a comentar con una palabra) y no usar «hábitos» genéricos por ahora (169 vistas).

## Calendario de la semana (el vigente en `calendario.md`)
lun 19 · 08 Decidir en segundos · mar 20 · 09 Cuenta desactivada · mié 21 · 10 Mejores horas · jue 22 · 11 Ingreso inestable · vie 23 · 12 Comisión y tarifas · sáb 24 · 13 Multas y papeles · dom 25 · 14 Miedo a rechazar.

## Pendiente para la próxima conversación
- **Preguntar a Eduardo** si los videos 06 y 07 ya se publicaron (el calendario los muestra sin marcar aunque su fecha ya pasó).
- Pedir las estadísticas que faltan: «21 · Beta testers corto» (en el repo, 546 vistas el 18 oct) y cualquier video publicado después del 16 oct, de las dos cuentas.
- Que Eduardo elija qué cambios aplicar. Los de texto (`copy.txt`) y el calendario se pueden aplicar directo; las portadas, una por una con su aprobación.
- Después de aplicar, volver a medir en 3–4 días con las mismas capturas de TikTok Studio (Estadísticas → Contenido → el video: vistas, tiempo promedio, % completo, retención, fuentes de tráfico y búsquedas) y comparar con la tabla de arriba.
