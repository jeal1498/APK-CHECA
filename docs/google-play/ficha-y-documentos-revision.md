# Google Play — ficha y documentos de revisión de Checa

**Estado al 21 de octubre de 2026:** borradores preparados; esperar la revisión de documentos por parte de Google antes de dar esta fase por aprobada o avanzar a publicación. Este archivo documenta el trabajo de ficha; no cambia el código ni la distribución web.

## Decisiones confirmadas
- Nombre: **Checa: Rentabilidad de viajes**
- Lanzamiento inicial gratuito; monetización posterior.
- Público: conductores de Uber y DiDi.
- No presentar Checa como herramienta de accesibilidad.
- La distribución web/directa debe permanecer intacta; las decisiones exclusivas de Play se aplican al flavor `play`.

## Borrador de ficha de Google Play

### Título
Checa: Rentabilidad de viajes

### Descripción breve
Analiza ofertas de viaje por kilómetro, hora y ganancia neta estimada.

### Descripción completa
Checa te ayuda a evaluar ofertas de viaje antes de decidir si te convienen.

Consulta de un vistazo indicadores estimados como:
- Ganancia por hora y por kilómetro.
- Un semáforo visual para comparar la oferta con tus metas.
- Ganancia neta estimada después del costo de gasolina.
- Información resumida para tomar una decisión con más contexto.

Diseñada para conductores que trabajan con aplicaciones como Uber y DiDi. Checa es una herramienta independiente y no está afiliada, patrocinada ni respaldada por Uber, DiDi u otras plataformas mencionadas.

Los cálculos son estimaciones y dependen de los datos configurados, el consumo del vehículo y el precio de combustible. No garantizan ganancias ni sustituyen tu propio criterio. Usa la app y toma decisiones únicamente cuando sea seguro; nunca interactúes con el teléfono mientras conduces.

La app se ofrecerá gratis durante el lanzamiento. Las condiciones futuras de cualquier plan o función de pago se comunicarán claramente dentro de la aplicación y en Google Play antes de que se apliquen.

## Borrador de divulgación destacada de Accesibilidad

> **Por qué Checa necesita este permiso**
>
> Checa utiliza el servicio de Accesibilidad de Android para detectar información visible de las ofertas de viaje y mostrar un resumen de rentabilidad. El contenido de la pantalla puede incluir información de la aplicación que estés usando. Este acceso se utiliza para la función de análisis de ofertas de Checa; no es una herramienta destinada a ayudar a personas con discapacidad a utilizar el dispositivo.
>
> Si no concedes este permiso, las funciones que dependen de la lectura de ofertas no estarán disponibles.

Botones propuestos: **«Aceptar y continuar»** y **«No aceptar»**. La divulgación debe mostrarse dentro de la app antes de abrir la configuración del permiso, y el usuario debe dar consentimiento afirmativo.

**Importante:** este texto es un borrador de producto, no una declaración técnica verificada. Antes de enviarlo definitivamente, comprobar en el código y en el comportamiento real qué datos se leen, procesan, almacenan o transmiten; ajustar el texto si hace falta y completar la declaración de uso de Accessibility API en Play Console. No afirmar que los datos nunca se guardan, transmiten o comparten sin verificarlo.

## Pendiente antes de publicar
- [ ] Esperar la revisión de documentos de Google y registrar exactamente sus observaciones.
- [ ] Verificar la divulgación de Accesibilidad contra el comportamiento real de la app.
- [ ] Completar la declaración de uso de Accessibility API en Play Console.
- [ ] Confirmar correo de soporte y URL pública de política de privacidad.
- [ ] Preparar/verificar icono de 512 × 512, gráfico de funciones de 1024 × 500 y capturas reales de la versión Play.
- [ ] Completar categoría, etiquetas, público objetivo y clasificación de contenido.
- [ ] Completar Seguridad de los datos según el flujo real de datos y servicios conectados.
- [ ] Desplegar y probar el flujo de eliminación de cuenta y su URL pública antes de publicar.
- [ ] Confirmar que el AAB de Play esté firmado y que la prueba cerrada cumpla los requisitos de Play Console.
- [ ] No publicar ni avanzar a la siguiente fase sin revisar el resultado de Google.

## Regla de espera
Hasta recibir respuesta de Google, tratar la ficha y los textos como **borradores pendientes de revisión**. No cambiar el sitio web ni la distribución directa para resolver requisitos exclusivos de Play. Cuando llegue la respuesta, añadir aquí la fecha, el texto del problema y la corrección acordada.
