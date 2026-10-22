# Ficha de Google Play: versión 2 (borrador)

**Estado:** borrador del 21 de octubre de 2026, para reemplazar el texto de `ficha-y-documentos-revision.md`. Límites verificados en [Play Console Help](https://support.google.com/googleplay/android-developer/answer/13393723): título ≤ 30, descripción corta ≤ 80, completa ≤ 4000 caracteres; sin textos de ranking («#1», «mejor app»); la corta no se repite en la completa. La documentación de Google también indica que el uso de la API de Accesibilidad debe documentarse en la ficha, por eso hay una sección de permisos.

## Datos
- **Título:** Checa: Rentabilidad de viajes
- **Descripción corta:** Calcula cuánto te deja cada oferta de viaje: por km, por hora y ganancia neta.
- **Política de privacidad:** https://jeal1498.github.io/checa-web/privacidad.html
- **Eliminación de cuenta:** https://jeal1498.github.io/checa-web/eliminar-cuenta.html
- **Correo de contacto:** lalolopezxd@gmail.com

## Descripción completa
Checa te ayuda a decidir si una oferta de viaje te conviene antes de aceptarla.

Cuando aparece una oferta, Checa muestra un pop-up con:
• Ganancia por kilómetro y por hora.
• Ganancia neta estimada, descontando la gasolina según tu rendimiento (km/l) y el precio por litro que configures.
• Un semáforo que compara la oferta con tus propias metas.
• Aviso por voz o vibración, si lo activas.

CÓMO FUNCIONA
Activas el lector una vez. Checa lee en tu teléfono la tarifa, la distancia y el tiempo que aparecen en pantalla y calcula los resultados. No toca botones ni acepta o rechaza viajes por ti: la decisión siempre es tuya.

PERMISOS QUE PIDE Y POR QUÉ
• Servicio de Accesibilidad de Android: para leer el texto visible de las ofertas en Uber y DiDi. Checa no es una herramienta de accesibilidad; usa este permiso solo para esta función. Antes de pedirlo, la app te muestra un aviso y necesita tu consentimiento.
• Captura de pantalla: para reconocer en tu teléfono el texto de la oferta. Mientras la lectura está activa, Checa analiza lo que se muestra en pantalla y descarta lo que no es una oferta. Android te pide confirmar y verás una notificación mientras esté activa.
• Notificaciones: para avisarte que la lectura está activa.

TU PRIVACIDAD
El contenido de la pantalla se procesa en tu teléfono y no se envía a los servidores de Checa. Inicias sesión con Google; guardamos tu correo, tus ajustes y los equipos vinculados para sincronizar tu cuenta. Puedes eliminar tu cuenta desde la app (Cuenta → Eliminar cuenta) o desde la web. El componente de reconocimiento de texto de Google (ML Kit) puede enviar a Google datos técnicos de diagnóstico. Lee el aviso de privacidad completo en el enlace de esta ficha.

IMPORTANTE
• Checa es una herramienta independiente. No está afiliada, patrocinada ni respaldada por Uber, DiDi ni otras plataformas; sus nombres pertenecen a sus propietarios y se mencionan solo para indicar compatibilidad.
• Los cálculos son estimaciones y dependen de los datos que configures, el consumo de tu vehículo y el precio del combustible. No garantizan ganancias ni sustituyen tu criterio.
• Nunca uses el teléfono mientras conduces. Revisa las ofertas solo cuando sea seguro.

Checa se ofrece gratis durante el lanzamiento. Si más adelante hay planes de pago, las condiciones se comunicarán dentro de la app y en Google Play antes de aplicarse.

## Notas de la versión (≤ 500)
Primera versión en Google Play.

## Recursos gráficos (preparar y verificar medidas al subirlos)
- [ ] Icono 512 × 512 px (a partir de `marca/icono-app.svg` de checa-web)
- [ ] Gráfico de funciones 1024 × 500 px
- [ ] Capturas de teléfono reales del flavor play
- Sin logos ni pantallas de Uber o DiDi: usar pantallas propias de Checa (pop-up sobre fondo neutro, «Lo guardado», ajustes). Es una recomendación; falta verificarla contra la política de metadatos.

## Pendiente
1. Categoría, etiquetas y clasificación de contenido (Fase 6, en Play Console).
2. Revisar el texto cuando se graben los videos con el AAB final.
