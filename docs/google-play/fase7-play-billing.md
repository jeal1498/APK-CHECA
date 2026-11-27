# Fase 7: Play Billing (cobros con Google Play)

Estado (27 nov 2026): compra de prueba verificada de punta a punta (10.88, Prueba interna). Producto `checa_acceso`, planes `mensual` ($119), `semestral` ($589), `anual` ($1,059), solo México.

## Piezas
- App: `Compras.kt` (billing 8.0.0) + `Cuenta.verificarCompra`. Los paquetes salen solo si hay planes activos en Play.
- Servidor: `play-verificar` (compra desde la app), `play-notificaciones` (avisos en tiempo real), `_shared/play.ts`, `_shared/fundador.ts`. Tabla `compras_play` (token → cuenta).
- Secretos de Supabase: `GOOGLE_PLAY_SA_B64` (JSON de la cuenta de servicio `checa-verificar`), `LICENCIA_PRIVADA_B64`, `PLAY_RTDN_SECRETO` (cadena larga al azar, para los avisos).
- Pruebas: un mes dura 5 min en compras de prueba; la licencia vence casi de inmediato (normal).

## Plan Fundador (−40 %)
Planes básicos aparte en `checa_acceso`: `mensual-fundador` (71), `semestral-fundador` (353), `anual-fundador` (635), P1M / P6M / P1Y, renovación automática, solo México. La app los usa solo si la cuenta tiene precio de fundador; el servidor lo vuelve a comprobar.

## Avisos en tiempo real (RTDN), una sola vez
1. Google Cloud (proyecto de la cuenta de servicio) › Pub/Sub › Temas › Crear tema `checa-play-rtdn`.
2. En ese tema › Permisos › Agregar principal `google-play-developer-notifications@system.gserviceaccount.com` con rol **Publicador de Pub/Sub**.
3. Pub/Sub › Suscripciones › Crear: tema `checa-play-rtdn`, tipo **Push**, URL `https://gqsadclvftkzwrihfipz.supabase.co/functions/v1/play-notificaciones?k=<PLAY_RTDN_SECRETO>`.
4. Play Console › app Checa › Monetizar con Play › Configuración de monetización › Notificaciones en tiempo real: activar, poner el nombre completo del tema (`projects/<proyecto>/topics/checa-play-rtdn`) y **Enviar notificación de prueba**.
