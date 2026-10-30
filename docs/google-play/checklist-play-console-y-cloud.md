# Checklist para Eduardo: Play Console, Google Cloud y testers

**Estado:** borrador del 21 de octubre de 2026 (Fase 6). Los requisitos de abajo se verificaron en la ayuda de Google; las rutas de menú pueden cambiar, así que cuando un paso diga «menú» busca el nombre equivalente. Fuentes: [pruebas para cuentas personales nuevas](https://support.google.com/googleplay/android-developer/answer/14151465), [verificación de desarrolladores Android](https://developer.android.com/developer-verification/guides), [registro desde Play Console](https://developer.android.com/developer-verification/guides/google-play-console), [estados de publicación de OAuth](https://developers.google.com/identity/protocols/oauth2/production-readiness/overview).

## Orden recomendado (ruta crítica)
1. Terminar la verificación de identidad de Google (hoy bloquea subir el AAB).
2. Crear la app y subir el AAB a **Prueba interna** (sin requisitos).
3. Configurar Google Cloud con los dos SHA-1.
4. Completar el contenido de la app y pasar a **Prueba cerrada**.
5. Reclutar 15 a 20 testers: **los 14 días corren desde que 12 están dentro**.
6. Solicitar acceso a producción y esperar la revisión.

Tiempo estimado: unas 3 semanas desde que haya 12 testers dentro (14 días de prueba, la solicitud y la revisión, que según Google suele tardar 7 días o menos).

## 1. Cuenta de Play
- [ ] Completar la verificación de identidad y de dispositivo que pida Play Console (ver `estado-lanzamiento-2026-10-21.md`).
- [ ] Confirmar que el correo de contacto de la cuenta es el que aparece en la ficha: `lalolopezxd@gmail.com`.

## 2. Crear la app y subir el AAB
- [ ] Crear app: nombre «Checa: Rentabilidad de viajes», idioma español (Latinoamérica), tipo **App**, **Gratis**.
- [ ] Bajar el AAB: GitHub → Mi-Ruta → Actions → «Preparar AAB Google Play» → corrida más reciente → artefacto `checa-play-release-N` (zip con el `.aab` y `mapping.txt`). Vigencia: 14 días; si caduca, se regenera con «Run workflow».
- [ ] Subir el `.aab` a **Prueba interna**. El `applicationId` queda fijado en `com.jeal1498.checa` desde esa primera subida.
- [ ] Aceptar **Play App Signing** (llave de subida = la de `checa-upload`).
- [ ] Subir `mapping.txt` como archivo de desofuscación de esa versión.
- [ ] Anotar el `versionCode` (hoy 73): cada subida nueva debe usar uno mayor.

## 3. Google Cloud (login con Google)
- [ ] Menú APIs y servicios → Credenciales → crear **ID de cliente OAuth tipo Android** para `com.jeal1498.checa` con el SHA-1 de la llave de subida:
  `C2:72:0C:07:CB:96:03:4D:2F:4D:0E:E9:B8:9E:B3:EF:5F:9B:BB:0A`
- [ ] En Play Console → Integridad de la app → **Firma de apps de Play**, copiar el SHA-1 del *certificado de firma de la app* y registrarlo igual. Si la consola no permite dos huellas en el mismo cliente, crea un cliente Android por huella (mismo paquete).
- [ ] No hace falta tocar Supabase si la app sigue usando el mismo ID de cliente web como `serverClientId`.
- [ ] Pantalla de consentimiento OAuth: **publicarla («En producción»)**. Con solo los permisos básicos (correo, perfil, openid) no requiere verificación, y estando publicada cualquier cuenta de Google puede entrar. En modo Pruebas, Google limita a 100 usuarios de prueba y avisa en pantalla.
- [ ] Evita poner logo o nombre de marca en esa pantalla si no quieres pasar por verificación de marca.

## 4. Contenido de la app (Play Console → Política y programas → Contenido de la app)
- [ ] **Política de privacidad:** `https://jeal1498.github.io/PWA-Checa-Multimedia/privacidad.html`
- [ ] **Acceso a la app:** indicar que requiere inicio de sesión y dar credenciales de revisión: crea una cuenta de Google solo para el revisor (sin verificación en dos pasos) y explica también la opción «Entrar sin cuenta» si aparece en la versión de Play. Google pide credenciales válidas para probar todas las funciones.
- [ ] **Anuncios:** No.
- [ ] **Clasificación de contenido:** responder el cuestionario con la realidad de la app.
- [ ] **Público objetivo:** solo adultos (conductores).
- [ ] **Seguridad de los datos:** copiar de `data-safety.md` (antes: verificar el nombre en Supabase Auth).
- [ ] **Eliminación de cuenta:** URL `https://jeal1498.github.io/PWA-Checa-Multimedia/eliminar-cuenta.html` y ruta en la app (Cuenta → Eliminar cuenta).
- [ ] **API de Accesibilidad:** declaración y video 1 de `accesibilidad-y-captura.md`.
- [ ] **Servicio en primer plano (mediaProjection):** descripción, impacto y video 2 del mismo documento.
- [ ] **ID de publicidad:** responder según la pestaña de permisos que muestre Play Console para el AAB; la app no muestra anuncios.
- [ ] Revisar que Play Console no muestre advertencias de permisos restringidos (el flavor play no declara `REQUEST_INSTALL_PACKAGES` ni `REQUEST_IGNORE_BATTERY_OPTIMIZATIONS`).
- [ ] Grabar los dos videos con la versión del AAB #5 o posterior (ya incluye el aviso de captura) y subirlos como enlace (por ejemplo, Drive o YouTube sin listar).

## 5. Ficha de la tienda
- [ ] Textos de `ficha-play.md` (título 29, corta 78, completa 2379 caracteres).
- [ ] Icono 512 × 512, gráfico de funciones 1024 × 500 y capturas reales del flavor play (sin logos de Uber o DiDi).
- [ ] Categoría, etiquetas y correo de contacto.

## 6. Prueba cerrada y testers
Reglas de Google para cuentas personales nuevas:
- Prueba cerrada con **mínimo 12 testers dentro, de forma continua durante 14 días**, antes de poder pedir producción.
- Quien sale antes de 14 días no cuenta; si sale y vuelve, los 14 días deben ser consecutivos.
- La solicitud de producción incluye tres partes: sobre la prueba (dificultad para reclutar, si usaron todas las funciones, resumen del feedback), sobre la app y sobre la preparación para producción.
- Google puede pedir seguir probando si hay menos de 12 testers o poca participación.
- Se puede subir una versión nueva durante la prueba.

Cómo hacerlo:
- [ ] Crear la pista de **Prueba cerrada**, subir el AAB y crear la lista de testers por correo (o un grupo de Google).
- [ ] Reclutar **15 a 20** personas con teléfono Android y cuenta de Google: conductores de tu lista de clientes y de la beta de la web (formulario de registro), más conocidos. Margen para las bajas.
- [ ] Enviarles el enlace de aceptación y estas instrucciones: «Acepta la prueba, instala Checa y **no salgas de la prueba durante 14 días**. Úsala al menos unos minutos cada día y manda tu opinión por este grupo de WhatsApp».
- [ ] Crear un grupo de WhatsApp para dudas, avisos y comentarios. Anota el feedback: lo necesitas para la solicitud.
- [ ] Pedir tareas concretas a lo largo de los 14 días (activar el lector, ajustar metas, guardar una oferta, eliminar la cuenta de prueba) para poder responder que usaron todas las funciones.
- [ ] Cada día 1 a 3 mensajes cortos de seguimiento, y revisar en Play Console que el conteo siga en 12 o más.

## 7. Registro de paquetes (verificación de desarrolladores Android)
Según Google, la verificación empieza a aplicarse el 30 de septiembre de 2026 en Brasil, Indonesia, Singapur y Tailandia, y se extiende al resto del mundo en 2027. México no está en la primera fase, pero conviene registrar antes.
- [ ] `com.jeal1498.checa` (Play): Google lo registra solo al crear la app en Play Console. Revisa la página de inicio de Play Console para confirmarlo.
- [ ] `com.karen.miruta` (canal directo): como distribuyes dentro y fuera de Play, registra este paquete desde Play Console. Pide demostrar que eres dueño de la llave: usa `MiRuta.apk` del último release, firmado con `miruta.jks`.
- [ ] Verifica el estado con la guía «Check registration status» de Android Developers.

## 8. Antes de pedir producción
- [ ] Revisar el informe previo al lanzamiento de Play Console y corregir fallas.
- [ ] Confirmar que las 3 pruebas pendientes de `auditoria-pruebas-pendientes-2026-10-21.md` están hechas.
- [ ] Revisión legal de la política de privacidad, del plazo de 30 días y de los avisos.
- [ ] Recordar la **Fase 7**: Google Play Billing antes del 1 de febrero de 2027.
