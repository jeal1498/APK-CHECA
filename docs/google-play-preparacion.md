# Preparación de Checa para Google Play

Este documento es una guía interna para preparar la primera versión. No sustituye las declaraciones que se deben contestar en Play Console ni garantiza la aprobación de Google.

## Estado y configuración técnica revisada

- Nombre visible: Checa.
- Paquete del flavor Play: `com.jeal1498.checa`.
- SDK objetivo actual: API 36 (Android 16).
- Versión de código revisada antes de estos cambios: 73; versión: 9.3. Antes de subir el primer AAB, confirmar la versión que se generará.
- La versión Play no usa compras de Mercado Pago ni compras integradas de Google Play en esta etapa.
- La beta gratuita está configurada en el código hasta el 31 de enero de 2027 (`Licencia.BETA_HASTA = 20270131`). Antes de esa fecha hay que decidir si se amplía la beta o se prepara otro modelo; después de esa fecha no asumir que el acceso seguirá gratis automáticamente.
- Workflow de AAB existente: `.github/workflows/play-aab-firmado.yml`. Es manual, ejecuta `gradle bundlePlayRelease`, verifica la firma y guarda `checa-play-release.aab` como artefacto por 14 días. No publica en Play Console.
- Workflow de comprobación: `.github/workflows/play-compilar.yml`; compila el flavor Play y revisa paquete/permisos. Se añadió una comprobación de la divulgación de Accesibilidad.

## Borrador de ficha de Google Play

**Nombre:** Checa

**Descripción breve (borrador):** Calcula lo que te queda de las ofertas de Uber y DiDi después de gasolina.

**Descripción completa (borrador):**

Checa ayuda a conductores a evaluar ofertas de viaje antes de decidir.

- Estima cuánto te queda después de gasolina.
- Compara el ingreso estimado por kilómetro y por hora con tus metas.
- Muestra un indicador visual y puede avisarte por voz y vibración.
- Puedes ajustar consumo, precio de gasolina y tus metas.
- La decisión siempre es tuya: Checa no pulsa botones ni acepta o rechaza viajes.

Para leer las ofertas, debes activar manualmente el servicio de Accesibilidad y aceptar el diálogo de captura de pantalla de Android cuando inicies la lectura. El contenido de las ofertas se procesa en tu teléfono. Tu correo de cuenta, la licencia, los equipos vinculados y tus preferencias pueden sincronizarse con los servidores de Checa; consulta el aviso de privacidad para más detalles.

La versión inicial de Google Play será gratuita durante la beta actual y no incluye compras dentro de la app. La beta está configurada hasta el 31 de enero de 2027; cualquier cambio futuro se comunicará en la app y en el sitio web.

Checa es un proyecto independiente y no está afiliado ni respaldado por Uber o DiDi.

**Sitio web:** https://jeal1498.github.io/PWA-Checa-Multimedia/

**Contacto/soporte:** https://wa.me/529985214490

**Categoría sugerida para revisar:** Herramientas. Confirmar la categoría definitiva al crear la ficha.

**Capturas requeridas:** capturas reales de la versión Play en un dispositivo, sin correos, nombres, datos de pasajeros ni información de cuenta. Conviene mostrar la pantalla principal, un ejemplo de oferta calculada, los ajustes de metas y la explicación de permisos. No usar mockups que sugieran funciones que no están implementadas.

**Público objetivo y clasificación de contenido:** contestar los cuestionarios reales en Play Console; no marcar una edad ni una clasificación por suposición. La app está orientada a conductores, no a niños.

## URLs que deben declararse

- Política de privacidad: https://jeal1498.github.io/PWA-Checa-Multimedia/privacidad.html
- Solicitud externa de eliminación de cuenta: https://jeal1498.github.io/PWA-Checa-Multimedia/eliminar-cuenta.html

Antes de presentar la app, abrir ambas URLs en una ventana privada y confirmar que responden públicamente y que el contenido coincide con la versión publicada.

## Borrador para la declaración de Accesibilidad

**¿La app es una herramienta de accesibilidad destinada principalmente a ayudar a personas con discapacidad?** No. Checa es una herramienta para conductores que calcula la rentabilidad de ofertas.

**¿Usa la API de AccessibilityService?** Sí.

**Función principal que necesita la API:** Checa lee el texto visible de las ofertas dentro de Uber y DiDi para reconocer tarifa, distancia y duración. Calcula estimaciones de ganancia después de gasolina, importe por kilómetro e importe por hora, y muestra un aviso superpuesto para ayudar al conductor a decidir.

**Límites del comportamiento:** Checa no pulsa botones ni acepta, rechaza o completa viajes por el usuario. El servicio limita su lectura a ventanas identificadas como Uber o DiDi. El contenido se procesa localmente; no se envía a los servidores de Checa ni se comparte con terceros. La captura de pantalla requiere que el usuario inicie la lectura y acepte el diálogo de Android. Si el usuario activa el guardado de diagnósticos, las capturas/textos se conservan localmente hasta 7 días y pueden contener datos visibles en pantalla.

**Divulgación mostrada antes de abrir los ajustes de Accesibilidad:** el texto del diálogo «Permiso para leer ofertas» en la app Play. La acción afirmativa es «Acepto y continuar»; «Ahora no» no activa el servicio.

Google puede pedir una demostración en vídeo del flujo de permiso y de la función principal. Grabarla con la versión Play real, sin datos personales de pasajeros.

## Vídeo de demostración para la declaración de Accesibilidad

Grabarlo en un dispositivo con la versión Play final y datos ficticios. Google indica que el vídeo debe mostrar la apertura de la app, la divulgación completa, el consentimiento, la concesión del permiso, la función principal y también qué pasa si el usuario no consiente. Para este último caso, mostrar «Ahora no» y cómo volver a abrir la divulgación más tarde. Si el texto requiere desplazamiento, desplazarlo lentamente para que se lea completo. No subirlo hasta que se haya probado el flujo real.

## Borrador de información de seguridad de los datos

Revisar estos puntos contra el código y los flujos reales antes de contestar el formulario:

- **Correo electrónico:** se usa para iniciar sesión con Google, identificar la cuenta y consultar licencia/equipos. Se procesa mediante Supabase.
- **Identificadores de cuenta/equipo:** la app sincroniza la licencia y los equipos vinculados con Supabase. Confirmar en el código y en el formulario qué categoría de identificador corresponde.
- **Preferencias de la app:** filtros, metas, consumo y precio de gasolina se sincronizan con Supabase para recuperar ajustes al iniciar sesión en otro equipo.
- **Texto de ofertas e historial de viajes:** se procesa localmente. En la versión Play, el guardado de diagnósticos empieza desactivado y solo se guarda si el usuario lo activa; es local y temporal. No declarar que se recopila por el servidor salvo que una comprobación del código revele lo contrario.
- **Pagos:** la primera versión Play está configurada sin compras. No declarar un flujo de pago dentro de Play que no esté implementado.
- Confirmar en el formulario si algún dato se comparte con proveedores que actúan únicamente como procesadores de servicio, y documentar las medidas de seguridad y la eliminación.
- Completar también la sección de eliminación de datos, con las dos URLs indicadas arriba.

## Servicios en primer plano y captura de pantalla

La app usa un servicio de primer plano de tipo `mediaProjection` para la lectura de pantalla iniciada por el usuario. En Play Console, revisar la declaración de permisos de servicios en primer plano y explicar que la captura se inicia por acción explícita del usuario, que Android muestra su propio diálogo de consentimiento y que el contenido se analiza localmente para reconocer ofertas.

## Lista de comprobación antes de generar/subir el AAB

- [ ] La ejecución más reciente de `play-compilar.yml` termina en éxito tras los cambios de divulgación.
- [ ] Probar en un dispositivo real el diálogo «Permiso para leer ofertas»: aceptar abre Ajustes; «Ahora no» no abre Ajustes ni activa el lector.
- [ ] Probar el diálogo de captura de pantalla de Android y confirmar que cancelar no inicia el servicio.
- [ ] Confirmar que la versión Play funciona gratis durante la beta actual y que no bloquea el lector por falta de compra.
- [ ] Confirmar que privacidad y eliminación de cuenta son accesibles públicamente.
- [ ] Probar la eliminación de cuenta con una cuenta de prueba, sin usar una cuenta real importante.
- [ ] Preparar icono, capturas reales, descripción, clasificación de contenido, público objetivo y credenciales/instrucciones de revisión si Google las pide.
- [ ] Contestar las declaraciones de Accesibilidad, servicios en primer plano, seguridad de datos y eliminación de cuenta en Play Console.
- [ ] Ejecutar el workflow manual `play-aab-firmado.yml`, descargar el artefacto y guardar el AAB y el `mapping.txt` en un lugar seguro.
- [ ] No subir el AAB hasta revisar todos los puntos anteriores.

## Pendientes que requieren prueba real

La compilación correcta no demuestra por sí sola que Google aprobará la política de Accesibilidad ni que la eliminación de cuenta funciona de extremo a extremo. Ambos puntos deben probarse antes de publicar.
