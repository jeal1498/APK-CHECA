# 20 · Cómo instalar Checa (tutorial)

Tutorial de instalación para cualquier conductor (fácil de seguir para un chavo o un señor). Aprobado por Eduardo el 16 oct 2026.

- **Base:** grabación de pantalla de Eduardo (6:07, Google Drive `1_konXN6bdc1hyDYrOt2tt0kqwunm8ycR`, «Paso a pasó para instalar CHECA.mp4»).
- **Formato:** capturas fijas de los momentos exactos del video, zoom suave al botón, la pantalla se oscurece y se ilumina lo que hay que tocar, dedo que «toca» cuando la voz dice la acción, 14 pasos numerados arriba, subtítulos de 3 palabras y cierre estándar.
- **Voz:** Fernando Martínez (ElevenLabs), velocidad 0.93, una línea por toma (53). Duración 4:04.
- **Privacidad:** se difuminan las cuentas de Google, el nombre y el correo de Eduardo y el nombre del paquete en la portada.
- **Código:** `video/tutorial-instalar/` (`build.py` con las capturas, zoom y señalamientos por línea; `lineas.txt` y `voz/`). Necesita los cuadros del video original a 4 fps en `../f4` (`ffmpeg -i video.mp4 -vf fps=4 -q:v 3 f4/%05d.jpg`); `python3 -I build.py audio`, `fotos` (hoja de revisión) y sin argumentos para el render.

## Auditoría de la versión base (grabación de Eduardo)
- Dura 6 min con esperas largas (descargas, pantallas de carga) y titubeos («perdón, perdón», litros/kilómetros).
- Su voz va antes o después de los toques: en la primera propuesta eso desfasaba voz y pantalla.
- No señala dónde tocar; en un celular el botón se ve chico.
- Muestra datos personales (cuentas de Google, correo, licencia de fundador).
- Hay pasos que no explica (el aviso «Se bloqueó la app», la foto de perfil en Play Store, «Abrir», «Aceptar»).

## Guion (voz limpia)

Hola. Te voy a enseñar a instalar Checa en tu celular Android, paso por paso. Ve a tu ritmo, y si te pierdes, pausa el video. Paso uno. Entra a la página oficial de Checa y toca el botón verde que dice Descargar. La descarga empieza sola. Arriba te sale un aviso que dice Descargando archivo. Espera a que termine. Paso dos. Cuando termine, toca los tres puntitos de arriba, y luego Descargas. Toca el archivo de Checa. Luego toca Instalar. Si te sale que la app se bloqueó, no te asustes. Es normal. Toca Entendido. Después toca Listo. Esto pasa porque Checa todavía no está en la Play Store. Paso tres. Hay que apagar Play Protect. Abre la Play Store y toca tu foto, arriba a la derecha. Toca Play Protect. Arriba a la derecha, toca el engranaje. Apaga la primera opción. Si te pregunta, toca Desactivar, y confirma con tu huella. Listo, las dos opciones quedan apagadas. Paso cuatro. Regresa a Descargas y toca otra vez el archivo de Checa. Toca Instalar y espera a que termine. Ahora toca Abrir. Paso cinco. Toca Continuar con Google. Elige tu cuenta. Espera a que cargue, y toca Empezar. Paso seis. Checa necesita un permiso para leer las ofertas en tu pantalla. Toca Abrir Accesibilidad. Baja hasta el final de la lista y toca Checa, lector de ofertas. Toca el interruptor. Te va a salir que se le negó el acceso. Es normal. Toca Cerrar. Paso siete. Sal a la pantalla de inicio y deja presionado el ícono de Checa. Toca Información de la app. Arriba, toca Eliminar restricción. Toca Eliminar, y confirma con tu código o tu huella. Paso ocho. Regresa a Checa, lector de ofertas, y toca el interruptor. Toca Aceptar. Listo, ya está activo. Checa solo lee la pantalla de Uber y de DiDi. Nada más. Paso nueve. Regresa a Checa y elige cuánto quieres ganar como mínimo, por kilómetro y por hora. Con los botones de más y menos cambias la cantidad. Tú pones tu meta. Toca Siguiente. Paso diez. Pon cuántos kilómetros rinde tu auto con un litro, y cuánto pagas por litro de gasolina. Así Checa sabe cuánto te cuesta cada kilómetro. Toca Siguiente. Así se verá el aviso sobre cada oferta. Toca Empezar a leer ofertas. Paso once. Este es el más importante. Android te pregunta si Checa puede grabar tu pantalla. Elige Toda la pantalla. Te lo va a pedir cada vez que actives Checa, y sin este permiso no funciona. Revisa que esté marcada Toda la pantalla, y toca Permitir. Si te pide notificaciones, toca Permitir también. Y listo. Checa ya está leyendo tus ofertas. Paso doce. En Ajustes, toca Probar en pantalla para ver el aviso en vivo. Más abajo, en Avisos, activa Decirlo con voz. Checa te dirá en voz alta: Aceptar, Considerar o Rechazar. Aquí eliges qué apps quieres que lea: Uber, DiDi, o las dos. Paso trece. En Filtros, elige hasta cuántos kilómetros manejas para recoger al pasajero. Y cuánto tiempo máximo quieres que dure el viaje. En Cuenta ves tu usuario y los celulares donde usas Checa. Paso catorce. En Inicio, toca Ver toda la semana. Cada hora se pinta de un color: verde si conviene, amarillo si más o menos, y rojo si es mejor no salir. Ahorita está vacío porque la app es nueva. Se va llenando con tus viajes. Cuando termines de trabajar, toca Detener. Y eso es todo. Si te atoras en algún paso, en Ajustes hay una sección de Ayuda. Descarga Checa y checa cuánto te queda de cada viaje.

## Pasos
| Paso | Qué se ve |
|---|---|
| Antes de empezar | Página oficial |
| 1 Descarga la app | Botón verde «Descargar» y aviso «Descargando archivo» |
| 2 Instálala | ⋮ → Descargas → Checa.apk → Instalar → «Se bloqueó la app» → Entendido → Listo |
| 3 Apaga Play Protect | Play Store → tu foto → Play Protect → engranaje → apagar → Desactivar → huella |
| 4 Instala otra vez | Checa.apk → Instalar → Abrir |
| 5 Entra con Google | Continuar con Google → tu cuenta → Empezar |
| 6 Da el permiso | Abrir Accesibilidad → Checa, lector de ofertas → interruptor → «se negó el acceso» → Cerrar |
| 7 Quita la restricción | Ícono presionado → Información de la app → Eliminar restricción → Eliminar → código o huella |
| 8 Activa el lector | Interruptor → Aceptar → activo (solo lee Uber y DiDi) |
| 9 Tus metas | Por kilómetro y por hora, botones más/menos → Siguiente |
| 10 Tu auto | Km por litro y precio por litro → costo por km → Siguiente → así se verá el aviso → Empezar a leer ofertas |
| 11 Grabar pantalla | Toda la pantalla → Permitir → notificaciones Permitir |
| 12 Ajustes | Probar en pantalla, Decirlo con voz, apps (Uber/DiDi) |
| 13 Filtros y cuenta | Recogida máxima, duración máxima, tus celulares |
| 14 Mejores horas | Ver toda la semana, colores por hora |
| Listo | Detener · sección de Ayuda en Ajustes · cierre estándar |

## Ajustes de honestidad
Sin duración prometida («cinco minutos» se quitó), sin precios ni prueba gratis en la voz; las metas son ejemplos («Tú pones tu meta»). En las capturas se ven «Descargar gratis · 7 días» y «Miembro fundador» porque así salen en la grabación.

## Texto de publicación
En `copy.txt`.
