# Data safety de Checa (com.jeal1498.checa, flavor `play`)

**Estado:** borrador del 21 de octubre de 2026, basado en el código (Fase 0) y en la ayuda oficial de Google. Las respuestas finales son responsabilidad de Eduardo. Fuentes: [Data safety](https://support.google.com/googleplay/android-developer/answer/10787469), [eliminación de cuenta](https://support.google.com/googleplay/android-developer/answer/13327111) y [declaración de datos de ML Kit](https://developers.google.com/ml-kit/android-data-disclosure).

## Reglas de Play que aplican (verificadas)
- «Recopilar» es enviar datos fuera del dispositivo, también los que envían SDKs. Lo que solo se procesa en el teléfono no se declara.
- Los datos seudónimos (re-asociables a una persona o dispositivo) sí se declaran.
- El formulario es único por paquete y debe describir la suma de lo que hacen todas las versiones publicadas.
- Si alguna versión exige un dato, se declara «obligatorio».

## Preguntas generales
| Pregunta | Respuesta | Evidencia |
|---|---|---|
| ¿Recopila o comparte datos? | Sí | Cuenta, equipos, ajustes y ML Kit |
| ¿Cifrado en tránsito? | Sí | Sin `http://` en el código; Supabase y Google por HTTPS; ML Kit usa HTTPS según Google |
| ¿Mecanismo para pedir eliminación? | Sí | En la app: Cuenta → Eliminar cuenta. Web: `https://jeal1498.github.io/PWA-Checa-Multimedia/eliminar-cuenta.html` |
| Families / revisión independiente | No / No | App para conductores adultos |

## Datos a declarar (todos «Recopilado»; ninguno «Compartido», ver abajo)
| Categoría → tipo | Qué es y de dónde sale | Propósitos |
|---|---|---|
| Info personal → Correo | Inicio de sesión con Google; llave de `licencias`, `equipos` y `ajustes` | Funcionalidad de la app; Administración de cuenta |
| Info personal → Nombre | El token de Google trae el nombre y Supabase Auth puede guardarlo. **Confirmado el 23 oct 2026 en Supabase → Auth → Users: sí lo guarda (Display name) y también la foto de perfil; se declaran Nombre y Fotos.** | Administración de cuenta |
| Info personal → ID de usuario | UUID de la cuenta en Supabase | Funcionalidad; Administración de cuenta |
| Dispositivo u otros ID | Código de equipo = hash SHA-256 truncado (8 hex) de ANDROID_ID, guardado en `equipos` con marca, modelo y alias. Es seudónimo | Funcionalidad (límite de equipos); Prevención de fraude, seguridad y cumplimiento |
| Dispositivo u otros ID | ML Kit: identificadores por instalación | Analítica |
| Información y rendimiento de la app → Diagnóstico / Otros datos de rendimiento | ML Kit: métricas de rendimiento, tipo de evento, códigos de error, configuración de API, versión de la función, tamaños de entrada y salida; además modelo, SO, paquete y versión | Analítica |
| Actividad en la app → Otras acciones | Ajustes sincronizados (`ajustes.datos`): filtros y umbrales, metas, km/l, precio de gasolina, voz, vibración, volumen, tamaño y posición del pop-up, apps activas | Funcionalidad; Personalización |
| Actividad en la app → Otro contenido generado por el usuario | Alias que el usuario le pone a su equipo | Funcionalidad |

**Obligatorio u opcional:** correo, nombre, ID de usuario, equipo y ajustes son *opcionales* mientras exista «Entrar sin cuenta» (beta gratis) en el flavor play; si se retira, cambiar a *obligatorio*. Lo de ML Kit es obligatorio (el usuario no lo controla).

**Decisión tuya:** la meta diaria (`metaDia`) no la trato como «Otra información financiera», porque Play ejemplifica ese tipo con sueldos o deudas y esto es una meta configurada, no un ingreso real. Si prefieres máxima prudencia, decláralo.

## Por qué «Compartido: No»
- Play no cuenta como compartir las transferencias a un proveedor de servicios que procesa por tu cuenta (Supabase es el backend de Checa).
- Google documenta que ML Kit no transfiere esos datos a terceros. **Confirmar:** si consideras que el envío de ML Kit a Google es compartir, marca «Compartido» en esas categorías.

## Qué NO se declara y por qué
- **Texto, OCR y capturas de las ofertas:** se procesan solo en el teléfono. Con «Guardar pantallas de ofertas» se guardan localmente, máximo 7 días.
- **Apps instaladas:** la app consulta las apps con lanzador solo para ubicar Uber y DiDi, sin enviarlas.
- **Ubicación, contactos, mensajes, fotos, audio, calendario y archivos:** la app no tiene esos permisos.
- **Pagos y compras:** el flavor play no tiene pagos. Revisar al llegar Google Play Billing (Fase 7).
- **Registro de testers y analítica de la página web:** ocurren en la web, no en la app.
- **Reporte a soporte por WhatsApp:** lo inicia el usuario al compartir.

## Preguntas de eliminación de datos
- ¿Permite crear cuenta en la app? Sí. Ruta en la app: Cuenta → Eliminar cuenta (llama a la función `eliminar-cuenta`).
- URL web: `https://jeal1498.github.io/PWA-Checa-Multimedia/eliminar-cuenta.html`
- Se elimina: cuenta de autenticación, licencia, equipos, ajustes e invitaciones.
- Retención en el flavor play: ninguna. Los registros de pago son del canal directo (Mercado Pago).
- La página web debe cumplir lo que pide Play: funcionar, mostrar la vía de solicitud de forma destacada, nombrar la app como en la ficha, permitir pedirlo sin reinstalar, explicar pasos extra y dar un plazo razonable.

## Pendiente antes de enviar el formulario
1. ~~Revisar nombre y avatar en Supabase Auth.~~ Hecho (23 oct 2026): se guardan nombre y foto; declarados en Play Console.
2. Probar `eliminar-cuenta` con una cuenta secundaria.
3. Confirmar en un teléfono si «Entrar sin cuenta» se ve en el flavor play.
4. ~~Agregar correo de contacto y plazo~~ Hecho en `PWA-Checa-Multimedia` (`8f9d5cb`): correo y plazo de 30 días en la página de eliminación y en la política.
5. Actualizar este documento si cambian las llamadas de red o los SDKs.
