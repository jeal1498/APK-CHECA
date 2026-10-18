# Historial de versiones de Checa

Movido desde `CLAUDE.md` (16 oct 2026) para que no se cargue en cada conversación. Agrega aquí cada versión nueva.

v1 base (accesibilidad) →
v2 OCR →
v3 vibración/sonido + historial →
v4 rediseño oscuro →
v5–v6 pop-up nuevo →
v7 botón circular + semáforo con detalle →
v8–v9 filtros inteligentes (4) →
v10 neta, mejores horas, multiplicador →
v11 pantalla dividida, pop-up por app, ⭐ mejor opción (2.0) →
**v12 rediseño "Neta"**: sistema de medidas, íconos propios, pop-up con franja de semáforo y neta grande, detalles en hojas propias, animaciones (versionName 3.0) →
**v13 nombre "Luz Verde"** (ícono de semáforo) y aviso por voz (3.1) →
**v14 pop-up B2 compacto** (3.2) →
v15 fichas centradas (3.3) →
v16 pantalla dividida sin zonas: separa por mitades (DiDi izq., Uber der.) y usa el título de la ventana (3.4) →
v17 OCR "ll min" →
"11 min" (3.5) →
v18 sin parpadeo: el pop-up no se rehace si no cambia, el OCR ignora lo que tapa el pop-up y no se reemplaza la oferta si los tramos son iguales (3.6) →
v19 recuerda la pantalla dividida 10 min para no volver a leer la pantalla entera cuando un pop-up tapa una mitad (3.7) →
v20 pop-ups sólidos: la mejor con borde dorado grueso, la 2ª con franja apagada (3.8) →
**v21 licencia** por equipo, 7 días de prueba, generador en GitHub (4.0) →
v22 insignia ⭐ Miembro fundador (4.1) →
v23 ofuscación R8 (`isMinifyEnabled`, `app/proguard-rules.pro`) para dificultar quitar la licencia (4.2), probada en su teléfono sin fallas →
**v24 nombre "Checa"** (ella lo eligió, fácil de decir; el ícono de semáforo se queda) (4.3) →
v25 sin encabezado en Inicio (empieza en "HOY"; la insignia ⭐ solo queda en Ajustes) (4.4) →
v26 filtros sin desplegar (monto editable en la misma fila, fichas de recogida/duración siempre visibles), meta por hora = $/km × 20 al editar el km (sigue editable) e Inicio más compacto (4.5) →
v27 filtro Por km primero y Mejores horas como gráfica de barras de $/km por hora (4.6) →
v28 Mejores horas como semáforo por hora de hoy + ventana de la semana (sin `Grafica.kt`) (4.7) →
v29 apps de banco: Nu bloqueó a Checa por usar Accesibilidad fuera de Play Store. `RutaService.soloUberYDidi()` limita `serviceInfo.packageNames` a paquetes instalados con uber/didi (+ `com.ubercab.driver`, `com.didiglobal.driver`; `<queries>` LAUNCHER en el manifiesto) y en Ajustes →
«Apps de banco» hay «Pausar lector» (`disableSelf()`) y «Volver a activar» (4.8). **Probado: con la 4.8 Nu ya funciona** (ella lo confirmó) y los pop-ups siguen bien en tablet (cálculos verificados contra capturas reales) →
v30 ⚡+$ de la dinámica de Uber «+$X incluido» (4.9) →
**v31 licencia por cuenta Google** con Supabase, 2 equipos, revisión diaria y 15 días sin internet (5.0) →
v32 botón «Ya pagué: revisar mi licencia» en Ajustes →
Cuenta (5.1) →
v33 botones de WhatsApp abren el chat de ventas `wa.me/529985214490` (5.2) →
v34 barra de 4 pestañas (Inicio · Filtros · ▶ · Ajustes · Cuenta), botón ▶ siempre verde (sin permiso abre Accesibilidad) y **paquetes** en Cuenta (5.3) →
v35 los paquetes se ocultan con licencia sin vencimiento (5.4) →
v36 pagos con Mercado Pago, sin WhatsApp (5.5) →
v37 sin «Tengo un código de activación» en Cuenta: la tarjeta queda como «Tu plan» (estado + ⭐). Los códigos ya activados siguen valiendo; para regalar licencias usar `correo=` en el pedido (5.6). →
v38 pestaña Cuenta con diseño «A · Credencial» (maqueta https://claude.ai/artifact/J1WDL7uEqyz8sqLD6GnuVc): perfil arriba (inicial con aro verde/dorado, nombre, correo), credencial del plan (dorada para fundador, barra en la prueba), Renovar, equipos como 2 lugares con «Quitar» que pide confirmar y «Cerrar sesión» discreto (5.7). Ella pidió: **el usuario siempre primero**. →
v39 **guía de primer uso** (maqueta https://claude.ai/artifact/2LAgUcZUwZzKk98XqXsNy5): diálogo a pantalla completa con 5 pasos (bienvenida + Google, Accesibilidad que avanza sola al volver con el permiso, metas $/km y $/h con −/+, km/l y $/l, pop-up de ejemplo real y «▶ Empezar a leer»). Solo sale en instalaciones nuevas (sin permiso, sin licencia pagada ni cuenta; `prefs.guiaVista`), reemplaza la bienvenida vieja y se puede ver en Ajustes →
Cómo usarla (5.8). Si otro banco bloquea, usar «Pausar lector»; solución de fondo: publicar en Play Store. La firma de licencias sigue usando el prefijo `LV|`: no cambiarlo o se invalidan las licencias. →
v40 **rediseño 6.0** con la skill UI/UX Pro Max (`.claude/skills/`, maqueta https://claude.ai/artifact/EuhqXjBd2xQnUezzgz9amB): fondo negro OLED, rojo #F43F5E, amarillo #FACC15, dorado #E8B84A, letra Fira Sans + Fira Code para números (`assets/fuentes`, objeto `Fuentes`), Inicio con barra proporcional del semáforo, cuadros de hora con el número adentro, Filtros en tarjetas con −/+ (mantener para avanzar rápido) y sin emojis en textos. **El pop-up no cambió**: usa `TemaPopup` (paleta vieja) (6.0). →
v41 **aviso de actualización** (Inicio + ventana + «Buscar actualización» en Cuenta) y **Comparte Checa** (6.1). Solo avisa desde la 6.1 en adelante: quien tenga la 6.0 debe bajarla una vez de la web. →
v42 **Invita a un amigo**: código por cuenta, link `?ref=` + portapapeles + campo a mano; al primer pago del amigo, 1 mes gratis a los dos (6.2). →
v43 **beta gratis** hasta `Licencia.BETA_HASTA` (31 ene 2027): todos con todo incluido, sin licencia; paquetes ocultos (6.3). →
v44 **Ayuda** en Ajustes (`Soporte.kt`): diagnóstico de problemas, preguntas frecuentes, «Una oferta se leyó mal» y soporte por WhatsApp con datos del equipo; aviso si la lectura se corta sola (6.4). →
v45 **Mi día** (6.5): en Inicio, `Jornada.kt` guarda los viajes que el conductor marca como aceptados («Registrar viaje», `viajes.txt`) para meta del día, gasolina de la semana y resumen para compartir como imagen (`Resumen.kt` + FileProvider); pop-up arrastrable (se guarda en `prefs.posicion`); aviso «Checa no está leyendo» al abrir Uber/DiDi (Android exige permiso de captura cada vez: un toque en el aviso inicia y regresa a la app); pide opinión por WhatsApp tras 7 días y 10 ofertas (máx. 3 veces); aviso de fin de beta 30 y 7 días antes. →
v46 (6.6): el aviso al abrir Uber/DiDi limita por app (3 min), no global; textos de compartir sin «7 días». **Pendiente de decidir:** precio de fundador para quienes usaron la beta (los avisos lo mencionan sin precio; hay que crearlo en `supabase/functions/_shared/planes.ts`, `mp-pago` y `tarjetaPaquete`). →
v47 **precio de fundador** para quien usó la beta: 40 % menos de por vida (1m $59, 6m $239, 12m $539; primer pago antes del 28 feb 2027 y renovar sin pasar 30 días vencido), en las tarjetas de paquetes y en los avisos de fin de beta (6.7).
v48 (6.8): una oferta cerrada (✕ o tiempo) no reaparece mientras siga en pantalla: la comparación tolera variaciones del OCR y el plazo de 60 s se renueva cada vez que se ve (feedback de beta tester: parpadeo). **Pendiente:** leer el listado de ofertas de DiDi (hoy solo lee la oferta nueva). →
v49 (6.9): Diagnóstico encendido por defecto y **fotos de las ofertas** (`Fotos.kt`): con Diagnóstico, guarda en el teléfono la pantalla de cada oferta leída o no (JPEG, últimas 30, máx. 1 cada 4 s) y Ajustes tiene «Compartir fotos de ofertas» y «Borrar fotos». **Pendiente:** avisarlo en Privacidad de checa-web. →
v50 (7.0): **widget** de pantalla de inicio (`WidgetCheca.kt`, 1x1: ícono y «Leyendo» / «Apagado»): verde «Leyendo» (un toque apaga) o oscuro «Apagado» (un toque abre Checa con `EXTRA_AUTO_INICIAR`, que pide el permiso de captura y regresa a la app anterior). Se repinta al iniciar/detener `CapturaService`. →
v51 (7.1): la voz de «Aceptar / Considerar / Rechazar» es la de Fernando (ElevenLabs, 3 mp3 en `res/raw/voz_*.mp3`, `MediaPlayer` en `Alertas.reproducir`); si falla, usa la voz del teléfono (TTS). →
v52 (7.2): se ocultan en Cuenta las tarjetas «Invita a un amigo» y «¿Te invitó alguien?» (el código en `MainActivity` sigue; la tarjeta de Inicio se queda). →
v53 (7.3): **entrar con Google es obligatorio** (pantalla de acceso que tapa la app; `RutaService` no muestra pop-ups sin sesión; la guía de 5 pasos sale después de entrar) y los **ajustes viajan con la cuenta** (tabla `ajustes` en Supabase, `Prefs.exportar/importar`, `Cuenta.sincronizarAjustes`: primera vez en el equipo = se aplican los de la cuenta o se suben los de aquí; después se suben al salir de la app si cambiaron y se bajan en la revisión diaria). La «bienvenida» con «Ahora no» se quitó. →
v54 (7.4): después de entrar con Google sale la pantalla **Permisos de Checa** (Accesibilidad, notificaciones, batería sin restricciones, inicio automático en Xiaomi y aviso de la grabación de pantalla) con botón «Iniciar lectura»; también sale una vez en equipos que ya tenían sesión. Permiso nuevo `REQUEST_IGNORE_BATTERY_OPTIMIZATIONS`. **Solo en la rama `ccr-9fd5942c-owg58l`, sin pasar a main (Eduardo lo pidió).** →
v55 (7.5): la pantalla de permisos es **obligatoria**: Accesibilidad, notificaciones y batería deben estar en «Listo» para continuar (sin «Ahora no», sin cerrar con atrás) y vuelve a salir al abrir la app mientras falte alguno; si ya negó notificaciones, el botón abre los ajustes de la app. Inicio automático (Xiaomi) y grabación de pantalla siguen como recomendación. →
v56 (7.6): **actualizar desde la app**: «Actualizar» descarga `checa-web/descargar/Checa.apk` dentro de Checa (con %), comprueba que sea el APK de Checa y abre el instalador de Android (el usuario confirma; la primera vez pide «Permitir de esta fuente»); si falla, abre la página `instalar/`. Permiso `REQUEST_INSTALL_PACKAGES`. →
v57 (7.7): **gestor de fotos** de Diagnóstico: «Revisar y elegir fotos» (cuadrícula con miniaturas, foto grande, círculo para elegir, «Compartir (n)» solo manda las elegidas, borrar una o varias). Nada se envía solo. **Solo en la rama `ccr-9fd5942c-owg58l`; pendiente el diseño del widget (Eduardo elige entre 2 propuestas) antes de pasar a main.** →
v58 (7.8): widget «Un foco» (propuesta B, sin marco): foco gris «Apagado» / foco verde con palomita «Leyendo»; ya en main junto con el gestor de fotos (7.7). →
v59 (7.9): **Ayuda con la lectura** (rediseño del diagnóstico, maqueta https://claude.ai/artifact/HEJ1sEf2N9ba2Pvia24V13): estado «Leyendo · última oferta hace…», contador, «Enviar reporte a soporte», «Lo guardado» (una fila por oferta con foto y texto, pestañas No leídas/Leídas/Todas, elegir sin recargar), detalle con zoom, reporte con motivo + ofertas + datos del equipo por WhatsApp (borrar lo enviado después, opcional). Por dentro: `Registro.kt` sustituye a `Fotos.kt` y a las capturas de texto; sin repetidas, en segundo plano, recortada a la app, 7 días, no leídas primero; no guarda «no leída» si el pop-up tapa la oferta; apagado no guarda nada; aviso la primera vez; cerrar sesión borra lo guardado y las estadísticas; «Borrar estadísticas» en tarjeta propia «Tus datos». Pendiente: «Tapar datos» (2.ª entrega). **Solo en la rama, sin pasar a main.** →
v60 (8.0): **Cuenta como menú** (maqueta https://claude.ai/artifact/AVNnKkJ44Fw8E13scufVpH): perfil compacto, credencial con «Precio de fundador asegurado · 40 % menos», filas con estado (Equipos, Permisos, Ayuda y soporte, Ayuda con la lectura, Apps de banco, Guía de inicio, Tus datos) que abren su pantalla; Equipos con nombre propio (`equipos.alias`, upsert porque HttpURLConnection no hace PATCH) y quitar con explicación; Ayuda y soporte con revisión automática (+ «Apps en Checa»), preguntas con buscador y WhatsApp; «Cerrar sesión» con confirmación; pie con versión. La pantalla de permisos avisa si el lector se pausó por el banco. Pregunta de privacidad actualizada («¿Qué datos guarda Checa?»). Se quitaron «Cómo usarla» y la tarjeta Permisos. **Solo en la rama, sin pasar a main.** →
v61 (8.1): **marca v2 en la app** (salvo el pop-up) y **app reorganizada** (maqueta https://claude.ai/artifact/XtXzYAKSpStts7U9KagMDG): asfalto/tablero/hueso, Bricolage/DM Sans/JetBrains Mono (TTF en assets, convertidos de checa-web/fonts), botones hueso en píldora, logo semáforo con ✓ en Inicio e ícono nuevo; Inicio con línea de estado y Mi día primero; Filtros → Mis metas (+ Tu auto y Dos ofertas); Ajustes → Lectura (Uber/DiDi, permisos, bancos, ayuda con la lectura); Cuenta solo con lo de la cuenta; arranque de 3 pasos (permisos con «Guardar pantallas» → Tus números → Así se verá); una ventana a la vez. **En la rama, sin pasar a main.** →
