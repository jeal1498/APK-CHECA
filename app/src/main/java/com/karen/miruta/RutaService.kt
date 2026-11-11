package com.karen.miruta

import android.accessibilityservice.AccessibilityService
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Rect
import android.os.Build
import android.os.Handler
import android.os.HandlerThread
import android.os.Looper
import android.os.PowerManager
import android.os.SystemClock
import android.view.Display
import java.util.concurrent.Executor
import android.view.WindowManager
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo

/** Zona de la pantalla que ocupa una app (Uber o DiDi). */
data class Zona(val app: String, val rect: Rect, val capa: Int)

class RutaService : AccessibilityService() {
    private lateinit var prefs: Prefs
    private var alertas: Alertas? = null
    private var appActual = ""
    private val handler = Handler(Looper.getMainLooper())
    private var pendiente = false

    /** Un pop-up por app, para que dos ofertas simultáneas no se pisen. */
    private val popups = HashMap<String, Popup>()
    private val activas = LinkedHashMap<String, Pair<Oferta, Rect?>>()
    private val ultimaFirma = HashMap<String, Pair<Oferta, Long>>()
    private val ultimoAviso = HashMap<String, Long>()
    /** Último evento de cada app (para saber cuándo se abrió de nuevo) y último «Lista para trabajar». */
    private val ultimoEventoPorApp = HashMap<String, Long>()
    private val ultimoListo = HashMap<String, Long>()
    private var avisoListo: AvisoListo? = null
    /** Ya se sugirió apagar la lectura en esta tanda sin Uber/DiDi. */
    private var terminasteAvisado = false
    private var inicioLectura = 0L
    /** Última vez que Uber o DiDi mandó un evento (también al salir su oferta encima de otra app). */
    @Volatile private var ultimoEventoApp = 0L

    override fun onServiceConnected() {
        super.onServiceConnected()
        prefs = Prefs(this)
        alertas = Alertas(this, prefs)
        instancia = this
        soloUberYDidi()
        // La lectura quedó encendida: se reanuda sola (tras reiniciar el teléfono o si Android
        // reconectó el servicio), sin volver a pedir nada.
        if (prefs.lecturaActiva && Lectura.porAccesibilidad(this) && Licencia.activa(this) && !Migracion.vencida()) {
            iniciarLectura()
        }
    }

    /**
     * Limita el lector a las apps de Uber y DiDi instaladas, para no tocar otras apps
     * (algunos bancos bloquean los lectores que vigilan todas las apps).
     */
    private fun soloUberYDidi() {
        try {
            val lanzador = android.content.Intent(android.content.Intent.ACTION_MAIN)
                .addCategory(android.content.Intent.CATEGORY_LAUNCHER)
            val pkgs = packageManager.queryIntentActivities(lanzador, 0)
                .map { it.activityInfo.packageName }
                .filter { val p = it.lowercase(); p.contains("uber") || p.contains("didi") }
                .toMutableSet()
            pkgs.addAll(listOf("com.ubercab.driver", "com.didiglobal.driver"))
            val info = serviceInfo ?: return
            info.packageNames = pkgs.toTypedArray()
            serviceInfo = info
        } catch (_: Exception) { }
    }

    /** Apaga el lector (para abrir apps de banco). Se vuelve a encender en Accesibilidad. */
    fun pausar() {
        prefs.lecturaActiva = false
        detenerLectura()
        popups.values.forEach { it.ocultar() }
        disableSelf()
    }

    override fun onUnbind(intent: android.content.Intent?): Boolean {
        pararPorSistema()
        instancia = null
        return super.onUnbind(intent)
    }

    /** Android apagó el servicio con la lectura encendida: Inicio lo avisa en amarillo. */
    private fun pararPorSistema() {
        if (!leyendo) return
        detenerLectura()
        if (::prefs.isInitialized && prefs.lecturaActiva) cortada = true
    }

    // ------------------------------------------------------------------
    // Lectura por captura de accesibilidad (Android 11+)
    // Solo se toma la imagen cuando hace falta: al llegar un evento de Uber o DiDi y,
    // mientras están en pantalla, cada ~1.5 s (0.5 s si hay una oferta a la vista).
    // No hay pantalla virtual copiando la pantalla todo el tiempo ni permiso que caduque.
    // ------------------------------------------------------------------

    @Volatile var leyendo = false
        private set
    private var ocr: LectorOcr? = null
    private var hiloOcr: HandlerThread? = null
    private var handlerOcr: Handler? = null
    private var capturando = false
    private var programada = false
    private var proxima = 0L
    private var ultimaCaptura = 0L
    private var fallosCaptura = 0
    /** Capturas buenas desde que se encendió: si hubo alguna, el teléfono sí deja capturar. */
    private var exitos = 0
    /** Al encender la pantalla o desbloquear: se limpia lo colgado y se lee ya. */
    private val pantallaRx = object : android.content.BroadcastReceiver() {
        override fun onReceive(c: Context?, i: Intent?) {
            if (!leyendo) return
            capturando = false
            ocr?.liberar()
            programar(0)
        }
    }
    /** Última vez que corrió el ciclo de captura (aunque no capturara). */
    private var ultimoCiclo = 0L
    /**
     * Vigilante: la lectura solo avanza cuando la captura y el OCR responden. Si Android pierde
     * una respuesta (sin éxito ni error), nada volvía a programar la captura y la lectura se
     * quedaba parada con la notificación activa. Cada 5 s revisa y, si el ciclo lleva 10 s
     * quieto, limpia lo colgado y vuelve a leer.
     */
    private val vigilante = object : Runnable {
        override fun run() {
            if (!leyendo) return
            if (SystemClock.uptimeMillis() - ultimoCiclo > VIGILANTE_QUIETO) {
                capturando = false
                ocr?.liberar()
                programada = false
                handler.removeCallbacks(capturar)
                programar(0)
            }
            revisarTerminaste()
            handler.postDelayed(this, VIGILANTE_CADA)
        }
    }
    private val capturar = Runnable {
        programada = false
        ultimoCiclo = SystemClock.uptimeMillis()
        if (Build.VERSION.SDK_INT >= 30) tomarCaptura()
    }

    /** Enciende la lectura. false si este Android no puede capturar desde accesibilidad. */
    fun iniciarLectura(): Boolean {
        if (Build.VERSION.SDK_INT < 30 || !::prefs.isInitialized) return false
        prefs.lecturaActiva = true
        if (leyendo) return true
        val h = HandlerThread("checa-ocr").also { it.start() }
        hiloOcr = h
        try {
            registerReceiver(pantallaRx, android.content.IntentFilter().apply {
                addAction(Intent.ACTION_SCREEN_ON); addAction(Intent.ACTION_USER_PRESENT)
            })
        } catch (_: Exception) { }
        handlerOcr = Handler(h.looper)
        ocr = LectorOcr(this, prefs)
        leyendo = true
        cortada = false
        fallosCaptura = 0
        exitos = 0
        inicioLectura = System.currentTimeMillis()
        terminasteAvisado = false
        notificarLectura()
        WidgetCheca.actualizar(this)
        ultimoCiclo = SystemClock.uptimeMillis()
        handler.removeCallbacks(vigilante)
        handler.postDelayed(vigilante, VIGILANTE_CADA)
        programar(0)
        return true
    }

    /** Apaga la lectura por accesibilidad (el servicio sigue para los pop-ups). */
    fun detenerLectura() {
        if (!leyendo) return
        leyendo = false
        handler.removeCallbacks(capturar)
        handler.removeCallbacks(vigilante)
        programada = false
        capturando = false
        try { unregisterReceiver(pantallaRx) } catch (_: Exception) { }
        try { ocr?.cerrar() } catch (_: Exception) { }
        ocr = null
        hiloOcr?.quitSafely()
        hiloOcr = null
        handlerOcr = null
        try { (getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager).cancel(ID_LECTURA) } catch (_: Exception) { }
        cancelarTerminaste()
        avisoListo?.ocultar()
        WidgetCheca.actualizar(this)
    }

    /** Programa la próxima captura en [retraso] ms, sin bajar del intervalo mínimo. */
    private fun programar(retraso: Long) {
        if (!leyendo) return
        val o = ocr
        val minimo = if (o != null && (o.ofertaEnPantalla || o.reintentoRapido)) INTERVALO_RAPIDO else INTERVALO_MIN
        val ahora = SystemClock.uptimeMillis()
        val t = maxOf(ahora + retraso, ultimaCaptura + minimo)
        if (programada && proxima <= t) return
        handler.removeCallbacks(capturar)
        programada = true
        proxima = t
        handler.postAtTime(capturar, t)
    }

    /** Después de cada captura: rápido con una oferta a la vista, normal con Uber/DiDi, lento sin ellas. */
    private fun siguiente() {
        if (!leyendo) return
        val o = ocr
        programar(when {
            o != null && (o.ofertaEnPantalla || o.reintentoRapido) -> INTERVALO_RAPIDO
            hayQueLeer() -> LATIDO_APP
            else -> LATIDO_SIN_APP
        })
    }

    @android.annotation.TargetApi(30)
    private fun tomarCaptura() {
        if (!leyendo) return
        val lector = ocr ?: return
        // Con la pantalla apagada no hay ofertas que ver (y la captura fallaría).
        val pm = getSystemService(Context.POWER_SERVICE) as PowerManager
        if (!pm.isInteractive) { programar(LATIDO_SIN_APP); return }
        // Al apagar o bloquear la pantalla, una captura u OCR puede quedar colgado sin avisar:
        // pasados 6 s se da por perdido y se sigue leyendo (si no, tras desbloquear no leía nada).
        if ((capturando || lector.ocupado) && SystemClock.uptimeMillis() - ultimaCaptura > 6000L) {
            capturando = false
            lector.liberar()
        }
        if (capturando || lector.ocupado) { programar(ESPERA_OCUPADO); return }
        val h = handlerOcr ?: return
        val ejecutor = Executor { r -> h.post(r) }
        capturando = true
        ultimaCaptura = SystemClock.uptimeMillis()
        try {
            takeScreenshot(Display.DEFAULT_DISPLAY, ejecutor, object : AccessibilityService.TakeScreenshotCallback {
                override fun onSuccess(resultado: AccessibilityService.ScreenshotResult) {
                    // Pase lo que pase (memoria llena, imagen rara), siempre se agenda la siguiente:
                    // antes un error aquí cortaba la cadena y la lectura se quedaba parada.
                    var ok = false
                    try {
                        val buffer = resultado.hardwareBuffer
                        val bmp: Bitmap? = try {
                            Bitmap.wrapHardwareBuffer(buffer, resultado.colorSpace)?.let { hw ->
                                val sw = hw.copy(Bitmap.Config.ARGB_8888, false)
                                hw.recycle()
                                sw
                            }
                        } catch (e: Throwable) { null } finally {
                            try { buffer.close() } catch (_: Throwable) { }
                        }
                        if (bmp != null) {
                            val ancho = bmp.width
                            val alto = bmp.height
                            lector.procesar(LectorOcr.reducir(bmp), ancho, alto)
                            ok = true
                        }
                    } catch (_: Throwable) { }
                    handler.post {
                        capturando = false
                        if (ok) { exitos++; fallosCaptura = 0; siguiente() } else falloCaptura(-1)
                    }
                }

                override fun onFailure(codigo: Int) {
                    handler.post { capturando = false; falloCaptura(codigo) }
                }
            })
        } catch (e: Exception) {
            capturando = false
            falloCaptura(-1)
        }
    }

    private fun falloCaptura(codigo: Int) {
        if (!leyendo) return
        if (codigo == AccessibilityService.ERROR_TAKE_SCREENSHOT_INTERVAL_TIME_SHORT) { programar(INTERVALO_RAPIDO); return }
        fallosCaptura++
        // Si alguna vez funcionó, es un fallo pasajero: se reintenta con calma.
        if (exitos > 0 || fallosCaptura < FALLOS_CAPTURA_MAX) {
            programar(if (fallosCaptura < FALLOS_CAPTURA_MAX) INTERVALO_MIN else LATIDO_SIN_APP)
            return
        }
        // Este teléfono no deja capturar desde accesibilidad: se pasa a la captura de Android.
        prefs.capturaProyeccion = true
        prefs.lecturaActiva = false
        detenerLectura()
        cortada = true
        avisar(
            "Checa dejó de leer",
            "Tu teléfono pide otro permiso para leer la pantalla. Toca para volver a iniciar la lectura.",
            true
        )
    }

    /** Notificación fija mientras se lee, con «Detener». */
    private fun notificarLectura() {
        try {
            val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            nm.createNotificationChannel(
                NotificationChannel("lectura", "Lectura de pantalla", NotificationManager.IMPORTANCE_LOW)
            )
            nm.cancel(3) // aviso «Checa no está leyendo»
            val detenerPi = PendingIntent.getBroadcast(
                this, 6, Intent(this, DetenerLecturaReceiver::class.java).setAction(DetenerLecturaReceiver.ACCION),
                PendingIntent.FLAG_IMMUTABLE
            )
            val abrirPi = PendingIntent.getActivity(
                this, 7, Intent(this, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE
            )
            nm.notify(
                ID_LECTURA,
                Notification.Builder(this, "lectura")
                    .setContentTitle("Checa está leyendo la pantalla")
                    .setContentText("Toca \"Detener\" para apagarlo")
                    .setSmallIcon(R.drawable.ic_notif)
                    .setContentIntent(abrirPi)
                    .addAction(Notification.Action.Builder(null, "Detener", detenerPi).build())
                    .setOngoing(true)
                    .build()
            )
        } catch (_: Exception) { }
    }

    // ------------------------------------------------------------------
    // Pop-ups y comparación
    // ------------------------------------------------------------------

    /** Compatibilidad: oferta sin app/zona conocida. */
    fun mostrarOferta(oferta: Oferta) = mostrarOferta(oferta, appActual, null)

    /** Muestra la oferta de [app] sobre su [zona] (sin repetir la misma en 60 s). */
    fun mostrarOferta(leida: Oferta, app: String, zona: Rect?, lineas: List<String>? = null) {
        if (!Licencia.activa(this) || !Cuenta.puedeUsar(this)) return
        // Si la revisión en segundo plano ya corrigió esta misma lectura, se usa la corregida.
        val oferta = Confirmador.corregida(leida) ?: leida
        prefs.ultimaOferta = System.currentTimeMillis()
        val clave = app.ifEmpty { "?" }
        // Con el pop-up encima, el lector ve la oferta tapada y lee otros montos.
        // Si los tramos son los mismos (o se perdió uno), es la misma oferta: no se toca.
        activas[clave]?.first?.let { a ->
            val mismosTramos = Math.abs(a.km - oferta.km) < 0.05 && Math.abs(a.min - oferta.min) < 0.5
            // Un solo tramo visible (el otro quedó bajo el pop-up): es la misma oferta solo si ese
            // tramo coincide con la recogida o con el viaje de la activa. Si no, es una oferta NUEVA
            // que llegó con el pop-up anterior encima (antes se descartaba).
            fun cerca(km: Double, min: Double) = Math.abs(km - oferta.km) < 0.15 && Math.abs(min - oferta.min) < 1.1
            val tramoTapado = a.recogidaMin > 0 && oferta.recogidaMin == 0.0 &&
                (cerca(a.viajeKm, a.viajeMin) || cerca(a.recogidaKm, a.recogidaMin))
            if (mismosTramos || tramoTapado) return
        }
        // La misma oferta (con pequeñas variaciones del OCR) no vuelve a salir mientras siga en
        // pantalla: cada vez que se ve de nuevo se renueva el plazo de 60 s. Así, al cerrar el
        // pop-up con la ✕ o por tiempo, no reaparece (parpadeo).
        val ahora = System.currentTimeMillis()
        ultimaFirma[clave]?.let { (o, t) ->
            if (ahora - t < 60_000 && parecida(o, oferta)) { ultimaFirma[clave] = o to ahora; return }
        }
        ultimaFirma[clave] = oferta to ahora

        popups.getOrPut(clave) {
            Popup(this, prefs, WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY).also { p ->
                p.alCerrar = {
                    activas.remove(clave)
                    redibujar(null)
                }
            }
        }
        // Plan de 1 mes: un pop-up a la vez (Uber y DiDi juntos es del plan de 6 meses).
        if (Licencia.basico(this)) {
            activas.keys.filter { it != clave }.forEach { otra ->
                activas.remove(otra)
                popups[otra]?.ocultar()
            }
        }
        activas[clave] = oferta to zona
        redibujar(clave)

        val nivel = oferta.nivel(prefs)
        alertas?.avisar(nivel)
        Historial.registrar(this, oferta, app, nivel)
        if (lineas != null && oferta === leida) {
            Confirmador.revisar(this, app, lineas, leida, prefs.moneda) { nueva ->
                handler.post { aplicarCorreccion(clave, app, leida, nueva) }
            }
        }
    }

    /** La revisión en segundo plano no coincidió: el pop-up se corrige sin anunciarlo. */
    private fun aplicarCorreccion(clave: String, app: String, leida: Oferta, nueva: Oferta) {
        val par = activas[clave] ?: return
        if (par.first !== leida) return
        activas[clave] = nueva to par.second
        ultimaFirma[clave] = nueva to System.currentTimeMillis()
        redibujar(null)
        val antes = leida.nivel(prefs)
        val ahora = nueva.nivel(prefs)
        // Solo si cambia el color suena la alerta normal, como con cualquier oferta.
        if (ahora != antes) alertas?.avisar(ahora)
        Historial.corregirUltima(this, app, nueva, ahora)
    }

    private fun parecida(a: Oferta, b: Oferta) =
        Math.abs(a.km - b.km) < 0.15 && Math.abs(a.min - b.min) < 1.1 && Math.abs(a.tarifa - b.tarifa) < 1.0

    /** true si el pop-up de [app] está en pantalla (entonces tapa la oferta y el lector no la ve completa). */
    fun popupVisible(app: String?): Boolean = popups[app ?: "?"]?.rect() != null

    /** Zonas que tapan los pop-ups visibles. */
    fun rectsPopups(): List<Rect> = popups.values.mapNotNull { it.rect() }

    /** Vuelve a pintar los pop-ups visibles con su marca (mejor / 2ª opción). */
    private fun redibujar(nueva: String?) {
        val marcas = if (Licencia.basico(this)) emptyMap<String, Int>()
            else Comparador.marcas(activas.mapValues { it.value.first }, prefs)
        for ((app, par) in activas) {
            popups[app]?.mostrar(par.first, app, par.second, marcas[app] ?: Popup.SIN_MARCA, app == nueva)
        }
    }

    // ------------------------------------------------------------------
    // Zonas de pantalla por app (pantalla dividida, ventanas flotantes)
    // ------------------------------------------------------------------

    fun nombreApp(pkg: String): String? {
        val p = pkg.lowercase()
        return when {
            prefs.uber && p.contains("uber") -> "Uber"
            prefs.didi && p.contains("didi") -> "DiDi"
            else -> null
        }
    }

    /** Ventanas visibles de Uber/DiDi, de la más alta (encima) a la más baja. */
    fun zonasApps(): List<Zona> {
        if (!::prefs.isInitialized) return emptyList()
        val out = ArrayList<Zona>()
        try {
            for (w in windows) {
                // En pantalla dividida algunas tablets no dan el contenido de la
                // ventana sin foco: entonces se usa su título ("Uber Driver", "DiDi…").
                val pkg = w.root?.packageName?.toString() ?: w.title?.toString() ?: continue
                val app = nombreApp(pkg) ?: continue
                val r = Rect()
                w.getBoundsInScreen(r)
                if (!r.isEmpty) out.add(Zona(app, r, w.layer))
            }
        } catch (_: Exception) { }
        return out.sortedByDescending { it.capa }
    }

    /**
     * Para ahorrar batería: false solo si es seguro que no hay Uber ni DiDi en pantalla
     * (ninguna ventana suya, aunque sea una oferta encima de WhatsApp, y ningún evento
     * suyo en 30 s). Ante cualquier duda devuelve true y se lee como siempre.
     */
    fun hayQueLeer(): Boolean {
        if (!::prefs.isInitialized) return true
        if (System.currentTimeMillis() - ultimoEventoApp < 30_000L) return true
        return try {
            val ventanas = windows ?: return true
            var conocidas = 0
            for (w in ventanas) {
                val pkg = w.root?.packageName?.toString()
                if (pkg != null) conocidas++
                val nombre = ((pkg ?: "") + " " + (w.title?.toString() ?: "")).lowercase()
                if (nombre.contains("uber") || nombre.contains("didi")) return true
            }
            // Si no se pudo ver ninguna ventana, no sabemos qué hay: se lee.
            conocidas == 0
        } catch (_: Exception) { true }
    }

    // ------------------------------------------------------------------
    // Lectura por accesibilidad (cuando la app deja leer su texto)
    // ------------------------------------------------------------------

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (!::prefs.isInitialized) return
        val pkg = event?.packageName?.toString() ?: return
        if (event.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            val p = pkg.lowercase()
            when {
                p.contains("uber") -> appActual = "Uber"
                p.contains("didi") -> appActual = "DiDi"
                p != packageName && !p.contains("systemui") -> appActual = "Otra"
            }
        }
        val nombre = nombreApp(pkg) ?: return
        val ahora = System.currentTimeMillis()
        ultimoEventoApp = ahora
        // Volvió Uber/DiDi tras un rato fuera: Checa se enciende sola y avisa (10.40).
        val previo = ultimoEventoPorApp[nombre] ?: 0L
        val hueco = ahora - previo > REABRIO_MS
        val abierta = !hueco || event.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED || esVentanaActiva(nombre)
        // Sin pruebas de que la app está al frente se deja el reloj quieto: el próximo evento reintenta.
        if (abierta) ultimoEventoPorApp[nombre] = ahora
        if (hueco && abierta) abrirApp(nombre)
        // Tiempo manejando seguido (widget Descanso): a las 4 h avisa una vez.
        if (leyendo) {
            if (Manejo.latido(this, ahora)) avisarDescanso()
            Widgets.actualizarSiToca(this, ahora)
        }
        // Hay actividad de Uber/DiDi: el aviso «¿Terminaste?» vuelve a empezar.
        if (terminasteAvisado) { terminasteAvisado = false; cancelarTerminaste() }
        // Algo cambió en Uber o DiDi: se mira la pantalla ya (respetando el intervalo mínimo).
        if (leyendo) programar(0)
        if (pendiente) return
        pendiente = true
        handler.postDelayed({
            pendiente = false
            analizar()
        }, 300)
    }

    /** true si la ventana activa es de [app] (para distinguir «se abrió» de «actualizó en segundo plano»). */
    private fun esVentanaActiva(app: String): Boolean = try {
        rootInActiveWindow?.packageName?.toString()?.let { nombreApp(it) } == app
    } catch (_: Exception) { false }

    /**
     * Se abrió [app] (10.40). En Android 11+ enciende la lectura sola, sin permiso de captura, y
     * muestra «Lista para trabajar» una vez por app cada 4 h. Si el conductor la apagó a mano
     * no se reenciende hasta que vuelva a abrir Uber o DiDi. En Android ≤ 10 la captura pide
     * permiso cada vez: queda el aviso con notificación.
     */
    private fun abrirApp(app: String) {
        try {
            if (!Licencia.activa(this) || !Cuenta.puedeUsar(this) || Migracion.vencida()) return
            if (!prefs.inicioAutomatico || !Lectura.porAccesibilidad(this)) { avisarApertura(app); return }
            if (!leyendo && !iniciarLectura()) { avisarApertura(app); return }
            val ahora = System.currentTimeMillis()
            if (ahora - (ultimoListo[app] ?: 0L) < LISTO_CADA_MS) return
            ultimoListo[app] = ahora
            val zona = zonasApps().firstOrNull { it.app == app }?.rect
            val a = avisoListo ?: AvisoListo(this, WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY).also { avisoListo = it }
            a.mostrar(app, zona) { Lectura.detener(this) }
        } catch (_: Exception) { }
    }

    /** Con la lectura encendida y 30 min sin Uber/DiDi, sugiere apagarla (una vez por tanda). */
    private fun revisarTerminaste() {
        if (terminasteAvisado || !leyendo) return
        val ref = maxOf(ultimoEventoApp, inicioLectura)
        if (System.currentTimeMillis() - ref < TERMINASTE_MS) return
        terminasteAvisado = true
        try {
            val nm = getSystemService(NotificationManager::class.java)
            nm.createNotificationChannel(NotificationChannel("avisos", "Avisos de Checa", NotificationManager.IMPORTANCE_HIGH))
            val detenerPi = PendingIntent.getBroadcast(
                this, 6, Intent(this, DetenerLecturaReceiver::class.java).setAction(DetenerLecturaReceiver.ACCION),
                PendingIntent.FLAG_IMMUTABLE
            )
            nm.notify(
                ID_TERMINASTE,
                Notification.Builder(this, "avisos")
                    .setContentTitle("¿Terminaste?")
                    .setContentText("Apaga Checa para no gastar batería de más.")
                    .setSmallIcon(R.drawable.ic_notif)
                    .setAutoCancel(true)
                    .addAction(Notification.Action.Builder(null, "Apagar", detenerPi).build())
                    .build()
            )
        } catch (_: Exception) { }
    }

    /** 4 h seguidas con Uber/DiDi: sugiere descansar 10 min. */
    private fun avisarDescanso() {
        try {
            val nm = getSystemService(NotificationManager::class.java)
            nm.createNotificationChannel(NotificationChannel("avisos", "Avisos de Checa", NotificationManager.IMPORTANCE_HIGH))
            nm.notify(
                ID_DESCANSO,
                Notification.Builder(this, "avisos")
                    .setContentTitle("Llevas 4 h manejando")
                    .setContentText("Descansa 10 min antes de seguir.")
                    .setSmallIcon(R.drawable.ic_notif)
                    .setAutoCancel(true)
                    .setTimeoutAfter(30 * 60_000L)
                    .build()
            )
        } catch (_: Exception) { }
    }

    private fun cancelarTerminaste() {
        try { getSystemService(NotificationManager::class.java).cancel(ID_TERMINASTE) } catch (_: Exception) { }
    }

    /**
     * Al abrir Uber o DiDi con la lectura apagada (Android ≤ 10 o con el inicio automático
     * apagado), avisa con una notificación: un toque en el aviso inicia la lectura.
     */
    private fun avisarApertura(app: String) {
        try {
            if (!prefs.avisarApertura || Lectura.corriendo || !Licencia.activa(this) || Migracion.vencida()) return
            // El límite es por app: abrir DiDi no debe callar el aviso de Uber.
            val ahora = System.currentTimeMillis()
            if (ahora - (ultimoAviso[app] ?: 0L) < 3 * 60_000L) return
            ultimoAviso[app] = ahora
            avisar("Checa no está leyendo", "Toca para iniciar la lectura y ver el pop-up de cada oferta.", false)
        } catch (_: Exception) { }
    }

    /** Aviso que abre Checa e inicia la lectura con un toque. */
    private fun avisar(titulo: String, texto: String, fijo: Boolean) {
        try {
            val nm = getSystemService(android.content.Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
            nm.createNotificationChannel(
                android.app.NotificationChannel("avisos", "Avisos de Checa", android.app.NotificationManager.IMPORTANCE_HIGH)
            )
            val abrir = android.content.Intent(this, MainActivity::class.java)
                .putExtra(MainActivity.EXTRA_AUTO_INICIAR, true)
                .addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK or android.content.Intent.FLAG_ACTIVITY_SINGLE_TOP)
            val pi = android.app.PendingIntent.getActivity(
                this, 4, abrir, android.app.PendingIntent.FLAG_IMMUTABLE or android.app.PendingIntent.FLAG_UPDATE_CURRENT
            )
            nm.notify(
                3,
                android.app.Notification.Builder(this, "avisos")
                    .setContentTitle(titulo)
                    .setContentText(texto)
                    .setSmallIcon(R.drawable.ic_notif)
                    .setContentIntent(pi)
                    .setAutoCancel(true)
                    .apply { if (!fijo) setTimeoutAfter(2 * 60_000L) }
                    .build()
            )
        } catch (_: Exception) { }
    }

    override fun onInterrupt() {}

    override fun onDestroy() {
        pararPorSistema()
        instancia = null
        popups.values.forEach { it.ocultar() }
        avisoListo?.ocultar()
        super.onDestroy()
    }

    /** Analiza cada ventana de Uber/DiDi POR SEPARADO para no mezclar ofertas. */
    private fun analizar() {
        try {
            val ventanas = windows ?: return
            for (w in ventanas) {
                val raiz = w.root ?: continue
                val pkg = raiz.packageName?.toString() ?: continue
                if (pkg == packageName) continue
                val app = nombreApp(pkg) ?: continue
                // Antes de recorrer todo el árbol (caro: Uber cambia el mapa sin parar), una
                // búsqueda del sistema dice si hay algún «min» a la vista. Sin él no hay oferta.
                val conMin = try { raiz.findAccessibilityNodeInfosByText("min") } catch (e: Exception) { null }
                if (conMin != null && conMin.isEmpty() && !prefs.diagnostico) continue
                val textos = LinkedHashSet<String>()
                recolectar(raiz, textos, 0)
                if (textos.isEmpty()) continue
                val leida = Parser.analizar(textos.toList(), prefs.moneda)
                if (prefs.diagnostico && textos.any { TRAMO.containsMatchIn(it) }) {
                    val firma = leida?.let { "${it.tarifa}|${it.km}|${it.min}" } ?: "?"
                    if (Guardado.esNueva(app, leida != null, firma)) {
                        Guardado.guardar(this, app, leida != null, textos.take(40).joinToString("\n") +
                            "\n\nCheca entendió (accesibilidad): " +
                            (leida?.let { "tarifa \$${it.tarifa}, ${it.km} km, ${it.min} min" } ?: "nada (no reconoció la oferta)"), null)
                    }
                }
                val oferta = leida ?: continue
                val r = Rect()
                w.getBoundsInScreen(r)
                mostrarOferta(oferta, app, r, textos.toList())
            }
        } catch (_: Exception) { }
    }

    private fun recolectar(n: AccessibilityNodeInfo, salida: MutableSet<String>, nivel: Int) {
        if (nivel > 40) return
        val t = (n.text ?: n.contentDescription)?.toString()?.trim()
        if (!t.isNullOrEmpty()) salida.add(t)
        for (i in 0 until n.childCount) {
            n.getChild(i)?.let { recolectar(it, salida, nivel + 1) }
        }
    }

    companion object {
        /** Un tramo «N min (X km)»: la pantalla tiene una oferta. */
        private val TRAMO = LectorOcr.tramoTexto
        @Volatile
        var instancia: RutaService? = null
        /** Android apagó el servicio (o la captura falló) con la lectura encendida. */
        @Volatile
        var cortada = false
        private const val ID_LECTURA = 5
        private const val ID_TERMINASTE = 8
        private const val ID_DESCANSO = 9
        /** Sin eventos de la app más de 2 min = se cerró y volvió a abrirla. */
        private const val REABRIO_MS = 2 * 60_000L
        /** «Lista para trabajar» sale una vez por app cada 4 h. */
        private const val LISTO_CADA_MS = 4 * 3_600_000L
        /** Sin Uber/DiDi 30 min con la lectura encendida: «¿Terminaste?». */
        private const val TERMINASTE_MS = 30 * 60_000L
        /** Mínimo entre capturas (Android permite una cada 333 ms). */
        private const val INTERVALO_MIN = 1000L
        private const val INTERVALO_RAPIDO = 500L
        private const val LATIDO_APP = 1500L
        private const val LATIDO_SIN_APP = 5000L
        private const val ESPERA_OCUPADO = 150L
        private const val FALLOS_CAPTURA_MAX = 5
        private const val VIGILANTE_CADA = 5000L
        private const val VIGILANTE_QUIETO = 10_000L
    }
}
