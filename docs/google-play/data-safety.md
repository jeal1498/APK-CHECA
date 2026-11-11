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

## Cambios por el puente con Clientela y SOS (4 nov 2026, versiones 9.7–10.0)
Agregar en Play Console → Contenido de la app → Seguridad de los datos (todo «Recopilado», «Compartido: No», opcional, cifrado en tránsito, se puede borrar):
| Categoría → tipo | Qué es | Propósitos |
|---|---|---|
| Info financiera → Otra información financiera | Viajes aceptados en `viajes_checa`: tarifa, gasolina y neto (ganancias del conductor) | Funcionalidad |
| Actividad en la app → Otras acciones | Fecha, hora, app (Uber/DiDi) y km de cada viaje aceptado; moneda y gasolina por km en `perfil_conductor` | Funcionalidad; Personalización |
| Info personal → Teléfono | WhatsApp del conductor para su QR (10.42): `contactoTel` dentro de `ajustes.datos`, ligado a su cuenta de Google; lo usa el widget «Mi QR» (el nombre del QR es el de su cuenta de Google, que ya se declara) | Funcionalidad |
| Info personal → Teléfono | Contactos SOS (`contactos_sos`): nombre y teléfono de terceros que agrega el conductor | Funcionalidad |
| Info personal → Nombre | Nombre de los contactos SOS (ya estaba declarado por la cuenta: agregar el propósito si cambia) | Funcionalidad |
| Info personal → Otra información | Relación del contacto SOS («mamá», etc.) | Funcionalidad |
| Info personal → Teléfono y Nombre (si Play lo pide por Clientela) | `clientes` y `viajes_privados` los escribe **Clientela**, no Checa; Checa solo lee de `viajes_privados` fecha, hora, precio y km | Funcionalidad |

**Ubicación:** la app ahora tiene `ACCESS_FINE_LOCATION`/`COARSE` (solo SOS, primer plano). Como **no sale del teléfono hacia nuestros servidores** (la pone el usuario en un mensaje de WhatsApp que él envía), según la regla de «recopilar» **no se declara** como recopilada; sí debe quedar explicada en Privacidad (hecho). Si Play la marca al revisar, declarar «Ubicación precisa · Recopilada · Funcionalidad · opcional». No requiere la declaración de ubicación en segundo plano.

**Ficha y revisión:** el permiso de ubicación aparecerá en la ficha («Ubicación»). Si Play pide justificación: «Botón SOS: al tocar Pedir ayuda, se lee la ubicación una vez para enviarla por WhatsApp a un contacto de confianza; sin uso en segundo plano».

**Eliminación:** al borrar el usuario de Auth se borran en cascada `perfil_conductor`, `clientes`, `viajes_privados`, `contactos_sos` y `viajes_checa` (ya en `eliminar-cuenta.html` y Privacidad).

Antes de subir el AAB con la 10.0 (Eduardo, sin publicar sin su OK): actualizar este formulario.

## Cambios de la 10.39 (8 nov 2026)
Agregar en Play Console → Seguridad de los datos (todo «Recopilado», «Compartido: No», opcional, cifrado en tránsito, se puede borrar):
| Categoría → tipo | Qué es | Propósitos |
|---|---|---|
| Actividad en la app → Otras acciones | `horas_checa`: ofertas, tarifa total y km por día de la semana y hora (30 días), sin cada oferta; país y apariencia dentro de `ajustes` | Funcionalidad; Personalización |
| Info financiera → Otra información financiera | Ya declarada por los viajes aceptados; ahora también se bajan a otro equipo | Funcionalidad |

**Eliminación:** `horas_checa` se borra en cascada con el usuario de Auth (agregarla en `eliminar-cuenta.html` y Privacidad).

## Cambios de la 10.48 (11 nov 2026): revisión de lecturas
Con sesión, la app envía a la función `confirmar-oferta` de Supabase **solo las líneas con números** de cada oferta (monto, minutos, km, multiplicador; nunca direcciones ni nombres) para verificar la lectura. Supabase la pasa a un proveedor de procesamiento que trabaja por cuenta de Checa (no es «compartir» según Play). Si no coincide, el caso queda en `errores_lector` para corregir el lector. Agregar en Play Console → Seguridad de los datos («Recopilado», «Compartido: No», cifrado en tránsito, se puede borrar):
| Categoría → tipo | Qué es | Propósitos |
|---|---|---|
| Actividad en la app → Otras acciones | Texto de la oferta (monto, minutos, km) para verificar la lectura; las diferencias se guardan en `errores_lector` | Funcionalidad; Analítica |
| Información y rendimiento de la app → Otros datos de rendimiento | Versión de la app y lectura local de la oferta, para corregir errores del lector | Analítica |

**Texto sugerido para Privacidad (sin mencionar la tecnología):** «Para verificar que la lectura de cada oferta sea correcta, Checa envía a nuestro servidor el monto, los minutos y los kilómetros de la oferta (nunca direcciones ni datos del pasajero). Si la verificación no coincide, guardamos ese caso para mejorar el lector.»

**Eliminación:** `errores_lector` y `uso_confirmar` se borran en cascada con el usuario de Auth. El punto «Texto, OCR y capturas» de abajo ahora es solo para lo que no son estas líneas.

## Cambios de la 10.57–10.58 (11 nov 2026): cascada de lectores y contexto de lectura
La revisión ahora también envía: el modelo del teléfono y la versión de Android, la orientación de la pantalla, si Android informó las ventanas de cada app, cuántas ofertas había a la vista, el color del semáforo y los tiempos de cada lector. Las ofertas donde los dos lectores coinciden se guardan en `coincidencias` (monto, km, min). Ajustar en Play Console → Seguridad de los datos:
| Categoría → tipo | Qué es | Propósitos |
|---|---|---|
| Información y rendimiento de la app → Otros datos de rendimiento | Modelo del teléfono, versión de Android, orientación y tiempos de lectura, para corregir el lector en cada equipo | Analítica |

**Eliminación:** `coincidencias` se borra en cascada con el usuario de Auth.

## Qué NO se declara y por qué
- **Capturas y el resto del texto de las ofertas:** se procesan solo en el teléfono (las líneas con números van a la revisión de la 10.48). Con «Guardar pantallas de ofertas» se guardan localmente, máximo 7 días.
- **Apps instaladas:** la app consulta las apps con lanzador solo para ubicar Uber y DiDi, sin enviarlas.
- **Contactos del teléfono, mensajes, fotos, audio, calendario y archivos:** la app no tiene esos permisos. (Ubicación: ver la sección del 4 nov.)
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

## Cambios de la 10.60 (11 nov 2026): zonas bloqueadas
- **Ubicación aproximada y precisa:** solo para centrar el mapa de zonas en primer plano (ya se declaraba por el SOS). No se guarda ni se envía.
- **Direcciones:** el texto del destino de una oferta (calle y colonia) se envía a Nominatim (OpenStreetMap) para convertirlo en coordenadas, solo si el conductor dibujó zonas. No incluye cuenta ni identificador. Revisar si cuenta como «Ubicación aproximada: recopilada/compartida» y mencionarlo en la política de privacidad.
- Los mosaicos del mapa se bajan de OpenStreetMap (internet).
