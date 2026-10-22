# Resultado de comprobación de despliegue: eliminación de cuenta

**Fecha:** 21 de octubre de 2026  
**Proyecto Supabase:** `gqsadclvftkzwrihfipz`  
**Función:** `eliminar-cuenta`

## Confirmación de solo lectura

La consulta al panel de Supabase confirmó que la función existe y está activa:

- Estado: `ACTIVE`
- Versión desplegada: `3`
- Verificación JWT del gateway: activada (`verify_jwt: true`)
- El código desplegado recuperado del panel coincide en contenido con el archivo `supabase/functions/eliminar-cuenta/index.ts` revisado en `main`.

No se invocó la función, no se borraron datos y no se modificaron secretos ni configuración.

## Qué confirma y qué no

Esto confirma el despliegue de la función, **no** su funcionamiento de extremo a extremo. Siguen pendientes la prueba con una cuenta de prueba y la comprobación de que todas las filas se eliminan o se tratan según la política de retención.

## Riesgo que debe resolverse antes de declarar la función terminada

El código cambia el correo de los pagos a `eliminada+<id de usuario>@cuentas.invalid`. El correo real deja de estar guardado, pero el identificador estable del usuario permanece dentro del valor. No debe describirse automáticamente como anonimización irreversible; puede ser seudonimización. Revisar la política de privacidad y decidir el tratamiento correcto de los registros de pago antes de afirmar que la cuenta quedó completamente anonimizada.

## Siguiente paso seguro

Probar la eliminación con una cuenta creada exclusivamente para pruebas. No utilizar la cuenta real de Eduardo ni una cuenta que tenga una licencia o pagos importantes. Antes de probar, confirmar cómo se observará el resultado en Supabase y conservar los registros de pago de prueba que se necesiten para comprobar la retención.
