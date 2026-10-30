# Continuación de auditoría Google Play — 21 de octubre de 2026

## Estado real verificado

- El AAB de Play del commit `d7a1aab26faed2ef65be79d4d896811d7e61c763` pasó el workflow de validación y su firma fue comprobada en la revisión anterior.
- El paquete Play es `com.jeal1498.checa`, versión `9.3`, `versionCode 73`.
- El manifiesto de origen común contiene dos permisos que no deben llegar al flavor Play: `REQUEST_INSTALL_PACKAGES` y `REQUEST_IGNORE_BATTERY_OPTIMIZATIONS`. `app/src/play/AndroidManifest.xml` los elimina mediante manifest merger. La auditoría del AAB previo confirmó que no están en el manifiesto final.
- La función Supabase `eliminar-cuenta` sí está desplegada y activa en el proyecto esperado, versión 3, con verificación JWT habilitada. El archivo desplegado coincide con `supabase/functions/eliminar-cuenta/index.ts`. No se ejecutó ni se cambió el despliegue.
- La verificación de identidad del desarrollador sigue siendo un bloqueo externo para subir el AAB a Play Console.
- Se mantiene intacto el repositorio `jeal1498/PWA-Checa-Multimedia` y el workflow legado `.github/workflows/build.yml`.

## Hallazgos estáticos adicionales

### Consentimiento de Accesibilidad

En `MainActivity.kt`, el flavor Play muestra una divulgación propia antes de abrir Ajustes. Incluye el contenido visible al que accede el lector, procesamiento local, diagnóstico opcional, y aclara que no acepta ni rechaza viajes. La opción negativa cierra la guía sin abrir Ajustes; la afirmativa guarda el consentimiento y abre la configuración del sistema.

**No está verificado en dispositivo** que el diálogo aparezca en todos los caminos de entrada, que se comporte correctamente después de reinstalar/actualizar, ni que la revisión de Google acepte la divulgación. Hace falta probar el flujo real y el consentimiento de MediaProjection.

### Eliminación de cuenta

La interfaz presenta confirmación explícita y avisa de que se borran cuenta, licencia, equipos, ajustes sincronizados, ofertas guardadas y estadísticas locales. El botón invoca el flujo de eliminación y actualiza las vistas tras el éxito.

La función de backend borra registros secuencialmente, trata el correo de pagos sustituyéndolo por `eliminada+<id>@cuentas.invalid`, y elimina al usuario de Auth al final. Ese valor evita conservar el correo original, pero mantiene un identificador estable: no debe describirse como anonimización irreversible sin una decisión de retención y un análisis de vinculación. La secuencia tampoco es transaccional; debe probarse un fallo parcial y su reintento en una cuenta desechable.

**No se realizó una eliminación real**, no se inspeccionaron datos personales y no se usó la cuenta de Eduardo. No se debe ejecutar esta prueba sobre una cuenta real, una licencia en uso o pagos importantes.

## Próximas tareas, por prioridad

1. Esperar a que Google termine la verificación de identidad del desarrollador.
2. Probar en un teléfono el consentimiento de Accesibilidad, aceptación/rechazo de MediaProjection y la lectura de una oferta ficticia.
3. Crear una cuenta desechable sin licencia ni datos importantes y probar eliminación de extremo a extremo; comprobar Auth, tablas relacionadas, pagos retenidos e imposibilidad de iniciar sesión de nuevo.
4. Revisar la política de privacidad y las declaraciones de Seguridad de los datos de Play para que coincidan con la conducta real, incluida la conservación de registros de pago y el diagnóstico local opcional.
5. Verificar compatibilidad de bibliotecas nativas con páginas de memoria de 16 KB y completar las pruebas de regresión del flavor Play.
6. Antes de cada nuevo AAB, incrementar `versionCode` y ejecutar de nuevo el workflow de validación.

## Límites de esta revisión

Esta es una revisión estática de código y artefactos existentes. No sustituye pruebas en dispositivo ni una revisión de cumplimiento de Google Play. No se desplegaron funciones, no se borraron cuentas, no se publicaron versiones y no se modificó el canal web/directo.
