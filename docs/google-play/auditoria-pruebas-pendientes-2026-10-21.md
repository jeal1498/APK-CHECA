# Auditoría previa de pruebas: Accesibilidad y eliminación de cuenta

**Fecha:** 21 de octubre de 2026  
**Alcance:** revisión estática de archivos del repositorio Android `jeal1498/Mi-Ruta`. No se ejecutó la app en un dispositivo, no se llamó a la función de eliminación, no se accedió a secretos y no se modificó el repositorio web.

## 1. Accesibilidad y captura de pantalla

### Verificado en código
- El manifiesto principal declara el servicio `RutaService` con `BIND_ACCESSIBILITY_SERVICE`.
- `CapturaService` usa MediaProjection y ML Kit para reconocer texto localmente.
- El manifiesto del flavor Play elimina `REQUEST_INSTALL_PACKAGES` y `REQUEST_IGNORE_BATTERY_OPTIMIZATIONS`.
- La guía de preparación especifica el flujo esperado de divulgación: «Permiso para leer ofertas», «Acepto y continuar» y «Ahora no», y aclara que Checa no acepta ni rechaza viajes por el usuario.

### Pendiente de probar en el teléfono
1. Instalar una compilación del flavor Play disponible para pruebas; no usar la versión web/directa para dar por verificado el comportamiento de Play.
2. Abrir la app sin el permiso de Accesibilidad y comprobar que la divulgación aparece antes de enviar al usuario a Ajustes.
3. Tocar «Ahora no»: no debe abrir Ajustes ni activar el lector.
4. Volver a iniciar el flujo, aceptar y comprobar que abre la pantalla de Ajustes de Accesibilidad.
5. Iniciar la lectura y aceptar el diálogo del sistema para compartir/capturar pantalla.
6. Repetir y cancelar el diálogo del sistema: la captura no debe empezar.
7. Probar una oferta ficticia o una pantalla de prueba sin datos de clientes; confirmar que aparece el cálculo y que Checa no pulsa botones de Uber/DiDi.
8. Si se prepara el video de demostración para Google, grabar el flujo completo, incluyendo el rechazo del consentimiento y cómo volver a abrirlo. Ocultar correos, nombres y datos de terceros.

**Resultado:** revisión estática solamente. La aceptación de los diálogos, el comportamiento en dispositivo y la política de Google no están confirmados.

## 2. Eliminación de cuenta

### Verificado en código
- Existe `supabase/functions/eliminar-cuenta/index.ts`.
- Requiere una solicitud POST con token Bearer; valida la sesión contra Supabase Auth y obtiene la identidad del token, no de un correo enviado por el cliente.
- Elimina filas asociadas al correo de `equipos`, `ajustes`, `codigos`, `invitaciones`, `licencias` y `testers`; intenta anonimizar el correo en `pagos` y borrar al usuario de Supabase Auth al final.
- Los errores no exponen token, correo ni cuerpo de error al cliente. Si una operación falla, responde con error genérico y permite reintentar.
- El esquema `supabase/esquema.sql` incluye las tablas mencionadas.

### Riesgos y comprobaciones pendientes
- **No está confirmado que la función esté desplegada** en el proyecto Supabase correcto ni que tenga configuradas sus variables de entorno.
- La ejecución es secuencial, no transaccional: un fallo intermedio puede dejar una eliminación parcial. El mensaje genérico indica reintentar, pero debe probarse la recuperación.
- El valor guardado en `pagos.correo` es `eliminada+<id de usuario>@cuentas.invalid`. Aunque ya no contiene el correo real, conserva un identificador estable que puede vincular los pagos con una cuenta eliminada. Antes de afirmar que los datos quedan «anonimizados», revisar si esto debe describirse como seudonimización o cambiarse con una decisión explícita sobre retención contable.
- La eliminación de un usuario en Supabase Auth no necesariamente invalida de inmediato todos los JWT emitidos anteriormente; confirmar la expiración y el comportamiento de acceso después de borrar la cuenta.
- Debe probarse el flujo completo con una cuenta creada exclusivamente para pruebas y comprobar que el usuario ya no puede iniciar sesión, que los datos de cuenta se eliminaron y que los pagos conservados ya no permiten identificar al usuario.
- No probar con la cuenta real de Eduardo ni con datos de un cliente.

### Prueba manual propuesta
1. Confirmar primero, desde Supabase Dashboard, que `eliminar-cuenta` está desplegada en el proyecto esperado y que las variables de entorno necesarias están configuradas; nunca copiar secretos al chat ni a Git.
2. Crear una cuenta de prueba sin licencia ni datos importantes.
3. Iniciar sesión en la app Play y usar «Eliminar cuenta» con confirmación explícita.
4. Verificar la respuesta de éxito y comprobar en Supabase que el usuario de Auth y las filas de esa cuenta desaparecieron, mientras el registro de pago de prueba (si existe) queda con el tratamiento de retención previsto.
5. Intentar iniciar sesión otra vez y revisar que la interfaz muestra un resultado coherente.
6. Repetir con una interrupción controlada solo en un entorno de pruebas, si se puede hacer sin tocar datos reales, para comprobar la recuperación de una eliminación parcial.

**Resultado:** código revisado, función no ejecutada ni desplegada por esta auditoría; funcionamiento de extremo a extremo pendiente.

## 3. Protección del canal web/directo

- No se modificó `jeal1498/PWA-Checa-Multimedia`.
- No se modificó `.github/workflows/build.yml`.
- No se desplegó ninguna función, no se borraron cuentas y no se publicaron versiones.

## Siguiente acción recomendada

Antes de tocar código, confirmar en el Dashboard de Supabase que la función `eliminar-cuenta` está desplegada en el proyecto correcto. Si no está desplegada, preparar el despliegue como una acción separada y explícitamente autorizada; después probar con una cuenta de prueba. En paralelo, realizar en el teléfono la lista de pruebas de Accesibilidad cuando exista una compilación Play instalable.
