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
    /** Turno de la oferta de cada pop-up: un resultado de zonas solo se aplica a la oferta que lo pidió. */
    private val turnos = Zonas.Turnos()
    /** Dónde se vieron por última vez los tramos de cada oferta activa, y lecturas seguidas sin verlos. */
    private val tramosVistos = HashMap<String, List<Rect>>()
    private val sinVer = HashMap<String, Int>()

    // ---------- Cascada de lectores (10.57) ----------
    /** Pop-up que sacó el lector de texto y espera la foto para confirmarse (sin IA si coinciden). */
    private class Espera(val app: String, val mostrada: Oferta, val leida: Oferta, val lineas: List<String>, val ms: Long, val t: Long) {
        var vencer: Runnable? = null
    }
    private val esperas = HashMap<String, Espera>()
    /** Pop-ups cuya oferta confirmaron los dos lectores (la foto puede descansar). */
    private val confirmadasTexto = HashSet<String>()
    /** Última vez que el lector de texto vio una oferta de cada app. */
    private val vistoTexto = HashMap<String, Long>()
    /** Ofertas seguidas que el lector de texto no vio, por app, y hasta cuándo se deja de usar. */
    private val fallosTexto = HashMap<String, Int>()
    private val textoApagadoHasta = HashMap<String, Long>()
    /** Tarjetas de oferta a la vista en la última lectura de cada app (10.58, para la revisión). */
    private val tarjetasVistas = HashMap<String, Int>()
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
        if (textoCubre()) { programar(INTERVALO_MIN); return }
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

    /** App de una oferta leída sin zonas: la que se ve en el texto, o la última Uber/DiDi abierta. */
    fun appEnPantalla(delTexto: String?): String =
        delTexto ?: appActual.takeIf { it == "Uber" || it == "DiDi" } ?: appActual

    /** Compatibilidad: oferta sin app/zona conocida. */
    fun mostrarOferta(oferta: Oferta) = mostrarOferta(oferta, appActual, null)

    /** Muestra la oferta de [app] sobre su [zona] (sin repetir la misma en 60 s). */
    fun mostrarOferta(leida: Oferta, app: String, zona: Rect?, lineas: List<String>? = null, msLectura: Long = -1, lector: String = LECTOR_OCR) {
        if (!Licencia.activa(this) || !Cuenta.puedeUsar(this)) return
        val clave = app.ifEmpty { "?" }
        if (lector == LECTOR_TEXTO) { vistoTexto[clave] = System.currentTimeMillis(); textoAcerto(clave) }
        // La foto vuelve a leer la oferta que ya sacó el texto: confirma (sin IA) o se revisan las dos.
        if (lineas != null && lector == LECTOR_OCR && confirmarConFoto(clave, leida, lineas, msLectura)) return
        // Si la revisión en segundo plano ya corrigió esta misma lectura, se usa la corregida.
        val oferta = Confirmador.corregida(leida) ?: leida
        prefs.ultimaOferta = System.currentTimeMillis()
        // La misma oferta que ya está en el pop-up de la otra app (la foto, sin zonas, puede
        // confundir de qué app es): no sale otro pop-up.
        if (activas.any { (k, par) -> k != clave && parecida(par.first, oferta) }) return
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
                    turnos.quitar(clave)
                    tramosVistos.remove(clave)
                    sinVer.remove(clave)
                    confirmadasTexto.remove(clave)
                    esperas.remove(clave)?.vencer?.let { handler.removeCallbacks(it) }
                    redibujar(null)
                }
            }
        }
        // Plan de 1 mes: un pop-up a la vez (Uber y DiDi juntos es del plan de 6 meses).
        // En vertical también uno solo: el de la oferta que acaba de llegar tapa a los anteriores
        // (Eduardo, 11 nov). Dos a la vez solo en horizontal (pantalla dividida).
        if (Licencia.basico(this) || !horizontal()) {
            activas.keys.filter { it != clave }.forEach { otra ->
                activas.remove(otra)
                popups[otra]?.ocultar()
            }
        }
        activas[clave] = oferta to zona
        val turno = turnos.nuevo(clave)
        tramosVistos.remove(clave)
        sinVer.remove(clave)
        redibujar(clave)

        val nivel = oferta.nivel(prefs)
        alertas?.avisar(nivel)
        Historial.registrar(this, oferta, app, nivel)
        Confirmador.contarOferta(this)
        if (lineas != null) revisarZona(clave, turno, lineas)
        confirmadasTexto.remove(clave)
        esperas.remove(clave)?.vencer?.let { handler.removeCallbacks(it) }
        // El lector de texto no vio esta oferta (la sacó la foto): cuenta para dejar de usarlo en esta app.
        if (lector == LECTOR_OCR && lineas != null) textoFallo(clave)
        if (lineas != null && oferta === leida) {
            // Desde la captura (o el evento) hasta el pop-up en pantalla.
            val msPopup = if (msLectura >= 0) msLectura else -1
            if (lector == LECTOR_TEXTO && leyendo) {
                // Se espera a la foto (≈ 0.5–1.5 s): si coinciden no se pregunta a la IA.
                val e = Espera(app, oferta, leida, lineas, msPopup, System.currentTimeMillis())
                val r = Runnable {
                    if (esperas[clave] === e) {
                        esperas.remove(clave)
                        revisarUna(clave, e.app, e.lineas, e.leida, e.ms, LECTOR_TEXTO)
                    }
                }
                e.vencer = r
                esperas[clave] = e
                handler.postDelayed(r, ESPERA_FOTO_MS)
            } else {
                revisarUna(clave, app, lineas, leida, msPopup, lector)
            }
        }
    }

    private fun revisarUna(clave: String, app: String, lineas: List<String>, leida: Oferta, msPopup: Long, lector: String) {
        Confirmador.revisar(this, app, lineas, leida, prefs.moneda, msPopup, lector) { nueva ->
            handler.post { aplicarCorreccion(clave, app, leida, nueva) }
        }
    }

    /**
     * La foto leyó una oferta mientras un pop-up del texto espera confirmación. Si es la misma,
     * queda confirmada (se anota en `coincidencias`, sin IA). Si no, la IA revisa las dos
     * lecturas juntas y corrige el pop-up si hace falta. true = esta lectura ya se usó.
     */
    private fun confirmarConFoto(app: String, foto: Oferta, lineas: List<String>, msFoto: Long): Boolean {
        val ahora = System.currentTimeMillis()
        fun vigente(k: String, w: Espera) = ahora - w.t <= ESPERA_FOTO_MS && esMostrada(activas[k]?.first, w.mostrada)
        // La espera de la misma app; si no, la de otra app con la misma oferta (la foto sin
        // zonas puede confundir la app). Otra oferta de otra app sigue su camino normal.
        val (clave, e) = esperas[app]?.takeIf { vigente(app, it) }?.let { app to it }
            ?: esperas.entries.firstOrNull { (k, w) -> vigente(k, w) && parecida(w.leida, foto) }?.toPair()
            ?: return false
        val a = e.leida
        // Con el pop-up encima la foto puede ver la oferta incompleta (un tramo tapado): se espera otra.
        fun cerca(km: Double, min: Double) = Math.abs(km - foto.km) < 0.15 && Math.abs(min - foto.min) < 1.1
        if (a.recogidaMin > 0 && foto.recogidaMin == 0.0 && (cerca(a.viajeKm, a.viajeMin) || cerca(a.recogidaKm, a.recogidaMin))) return true
        esperas.remove(clave)
        e.vencer?.let { handler.removeCallbacks(it) }
        if (parecida(a, foto)) {
            confirmadasTexto.add(clave)
            Confirmador.anotarCoincidencia(this, e.app, a, e.ms, if (msFoto >= 0) msFoto else -1)
        } else {
            Confirmador.revisar(this, e.app, e.lineas, a, prefs.moneda, e.ms, LECTOR_TEXTO,
                Confirmador.Otra(LECTOR_OCR, lineas, foto, msFoto)) { nueva ->
                handler.post { aplicarCorreccion(clave, e.app, e.mostrada, nueva) }
            }
        }
        return true
    }

    /** Cuenta las tarjetas de oferta en [lineas] (todas las de la zona, antes de separarlas). */
    fun anotarTarjetas(app: String, lineas: List<String>) {
        // El resumen de Uber («33 min · 10.6 km · $6.25/km est.») no es un tramo de otra tarjeta.
        val tramos = lineas.count { LectorOcr.esTramo(it) && !it.contains("/km") }
        if (tramos > 0) tarjetasVistas[app.ifEmpty { "?" }] = maxOf(1, (tramos + 1) / 2)
    }

    /** Cómo estaba la pantalla al leer [o] (10.58): orientación, zonas, tarjetas y color. Sin datos personales. */
    fun contexto(app: String, o: Oferta?): org.json.JSONObject {
        val nivel: Any = if (o == null || !::prefs.isInitialized) org.json.JSONObject.NULL else when (o.nivel(prefs)) {
            Oferta.VERDE -> "verde"
            Oferta.AMARILLO -> "amarillo"
            else -> "rojo"
        }
        return org.json.JSONObject()
            .put("orientacion", if (horizontal()) "horizontal" else "vertical")
            .put("zonas", try { zonasApps().isNotEmpty() } catch (_: Exception) { false })
            .put("tarjetas", tarjetasVistas[app.ifEmpty { "?" }] ?: 1)
            .put("nivel", nivel)
    }

    private fun textoAcerto(app: String) {
        fallosTexto[app] = 0
    }

    private fun textoFallo(app: String) {
        val n = (fallosTexto[app] ?: 0) + 1
        fallosTexto[app] = n
        if (n >= FALLOS_TEXTO_MAX) {
            // En este teléfono el texto de esta app no sirve: 30 min solo con la foto.
            textoApagadoHasta[app] = System.currentTimeMillis() + TEXTO_PAUSA_MS
            fallosTexto[app] = 0
        }
    }

    private fun textoApagado(app: String) = System.currentTimeMillis() < (textoApagadoHasta[app] ?: 0L)

    /**
     * Las ofertas en pantalla ya las confirmaron los dos lectores y el texto las sigue viendo:
     * la foto descansa (menos batería). Si el texto deja de verlas, la foto vuelve en ≤ 1.5 s.
     */
    private fun textoCubre(): Boolean {
        if (activas.isEmpty()) return false
        // Solo si el texto ya leyó la última oferta de cada app (si no, la foto podría perder una).
        if (APPS.any { textoApagado(it) || fallosTexto[it] != 0 }) return false
        val ahora = System.currentTimeMillis()
        return activas.keys.all { it in confirmadasTexto && ahora - (vistoTexto[it] ?: 0L) < TEXTO_FRESCO_MS }
    }

    /**
     * El lector no entendió una oferta que sí está en pantalla: se piden los datos al revisor
     * y, si pasan las revisiones del teléfono, sale el pop-up normal (1–2 s después).
     */
    fun rescatar(app: String, zona: Rect?, lineas: List<String>, lector: String = LECTOR_OCR) {
        if (!::prefs.isInitialized || !Licencia.activa(this) || !Cuenta.puedeUsar(this)) return
        // Con el pop-up encima la oferta se ve tapada: no es una lectura fallida.
        if (popupVisible(app)) return
        if (lineas.none { LectorOcr.esTramo(it) } || !Parser.tieneMonto(lineas, prefs.moneda)) return
        Confirmador.rescatar(this, app, lineas, prefs.moneda, lector) { oferta ->
            handler.post {
                mostrarOferta(oferta, app, zona)
                // El rescate no trae líneas al pop-up: las zonas se revisan aquí, si salió esta oferta.
                val clave = app.ifEmpty { "?" }
                if (activas[clave]?.first === oferta) turnos.actual(clave)?.let { revisarZona(clave, it, lineas) }
            }
        }
    }

    /**
     * Zonas bloqueadas: si el origen o el destino de la oferta cae en una zona dibujada por el
     * conductor, el pop-up pasa a rojo con el motivo. El pop-up económico ya salió: esto corre
     * en [Zonas.hilo] y solo corrige después. Necesita internet o la caché para ubicar las
     * direcciones; sin ellas (o si no se ve la dirección) no se bloquea nada.
     */
    private fun revisarZona(clave: String, turno: Long, lineas: List<String>) {
        if (!prefs.zonasOn) return
        val zonas = Zonas.leer(prefs.zonasTexto)
        if (zonas.isEmpty()) return
        val origen = Parser.origen(lineas)
        val destino = Parser.destino(lineas)
        if (origen == null && destino == null) return
        val pais = Pais.actual(prefs).codigo
        val ctx = applicationContext
        try {
            Zonas.hilo.execute {
                val geo = { d: String -> Zonas.ubicar(ctx, d, zonas, pais) }
                val ro = Zonas.evaluar(Zonas.Tipo.ORIGEN, origen, zonas, geo)
                val rd = Zonas.evaluar(Zonas.Tipo.DESTINO, destino, zonas, geo)
                if (ro.bloqueada || rd.bloqueada) handler.post { aplicarZonas(clave, turno, ro, rd) }
                // «Guardar pantallas»: qué dirección se leyó y dónde quedó, para revisar fallas.
                if (prefs.diagnostico) Guardado.guardar(ctx, "Zonas", ro.bloqueada || rd.bloqueada,
                    Zonas.informe(ro) + "\n" + Zonas.informe(rd) + "\n\n" + lineas.take(40).joinToString("\n"), null)
            }
        } catch (_: java.util.concurrent.RejectedExecutionException) { }
    }

    /** Pone las marcas de zona en la oferta del pop-up, si sigue siendo la misma que las pidió. Una sola alerta. */
    private fun aplicarZonas(clave: String, turno: Long, ro: Zonas.Resultado, rd: Zonas.Resultado) {
        if (!turnos.vigente(clave, turno)) return
        val par = activas[clave] ?: return
        val marcada = Zonas.marcar(par.first, ro, rd)
        if (marcada == par.first) return
        val antes = par.first.nivel(prefs)
        activas[clave] = marcada to par.second
        ultimaFirma[clave] = marcada to System.currentTimeMillis()
        redibujar(null)
        if (antes != Oferta.ROJO) alertas?.avisar(Oferta.ROJO)
        Historial.corregirUltima(this, clave, marcada, Oferta.ROJO)
    }

    /** La revisión en segundo plano no coincidió: el pop-up se corrige sin anunciarlo. */
    private fun aplicarCorreccion(clave: String, app: String, leida: Oferta, nueva0: Oferta) {
        val par = activas[clave] ?: return
        if (!esMostrada(par.first, leida)) return
        // Las zonas bloqueadas que ya se marcaron no se pierden con la corrección.
        val nueva = Zonas.conservar(par.first, nueva0)
        activas[clave] = nueva to par.second
        ultimaFirma[clave] = nueva to System.currentTimeMillis()
        redibujar(null)
        // Contra lo que se ve (con su marca de zona): si ya era rojo por zona no suena otra vez.
        val antes = par.first.nivel(prefs)
        val ahora = nueva.nivel(prefs)
        // Solo si cambia el color suena la alerta normal, como con cualquier oferta.
        if (ahora != antes) alertas?.avisar(ahora)
        Historial.corregirUltima(this, app, nueva, ahora)
        Confirmador.contarCorreccion(this)
    }

    /** [actual] es la oferta [mostrada], con o sin las marcas de zona bloqueada que se le pudieron agregar. */
    private fun esMostrada(actual: Oferta?, mostrada: Oferta) =
        actual === mostrada || (actual != null && actual.bloqueada && actual.sinZonas() == mostrada)

    private fun parecida(a: Oferta, b: Oferta) =
        Math.abs(a.km - b.km) < 0.15 && Math.abs(a.min - b.min) < 1.1 && Math.abs(a.tarifa - b.tarifa) < 1.0

    /**
     * Cada lectura del OCR dice dónde hay tramos «N min (X km)» a la vista ([tramos], en
     * coordenadas de pantalla, sin lo que tapan los pop-ups). Si la oferta de un pop-up ya no
     * está en dos lecturas seguidas (la tomó otro, se venció o se aceptó), el pop-up se quita.
     * Si el pop-up tapa donde estaba la oferta no se sabe si sigue: lo cierra su tiempo.
     */
    fun revisarSalida(tramos: List<Rect>) {
        if (activas.isEmpty()) return
        val tapado = rectsPopups()
        for ((clave, par) in activas.entries.toList()) {
            val zona = par.second
            val suyos = tramos.filter { zona == null || zona.contains(it.centerX(), it.centerY()) }
            if (suyos.isNotEmpty()) { tramosVistos[clave] = suyos; sinVer[clave] = 0; continue }
            val antes = tramosVistos[clave] ?: continue
            if (antes.all { r -> tapado.any { it.contains(r.centerX(), r.centerY()) } }) continue
            val n = (sinVer[clave] ?: 0) + 1
            sinVer[clave] = n
            if (n >= LECTURAS_SIN_OFERTA) popups[clave]?.ocultar()
        }
    }

    private fun horizontal() =
        resources.configuration.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE

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
        val inicio = android.os.SystemClock.elapsedRealtime()
        try {
            val ventanas = windows ?: return
            for (w in ventanas) {
                val raiz = w.root ?: continue
                val pkg = raiz.packageName?.toString() ?: continue
                if (pkg == packageName) continue
                val app = nombreApp(pkg) ?: continue
                // En este teléfono el texto de esta app no sirvió en las últimas ofertas: solo foto.
                if (textoApagado(app) && !prefs.diagnostico) continue
                // Antes de recorrer todo el árbol (caro: Uber cambia el mapa sin parar), una
                // búsqueda del sistema dice si hay algún «min» a la vista. Sin él no hay oferta.
                val conMin = try { raiz.findAccessibilityNodeInfosByText("min") } catch (e: Exception) { null }
                if (conMin != null && conMin.isEmpty() && !prefs.diagnostico) continue
                // Solo lo que se ve (10.55), con su posición para separar varias tarjetas.
                val renglones = ArrayList<Tarjetas.Renglon>()
                recolectar(raiz, renglones, HashSet(), 0)
                if (renglones.isEmpty()) continue
                val porAltura = renglones.indices.sortedBy { renglones[it].arriba }
                val elegidos = Tarjetas.primera(porAltura.map { renglones[it] }, prefs.moneda)
                    ?.map { porAltura[it] }?.toHashSet()
                val textos = renglones.indices.filter { elegidos == null || it in elegidos }.map { renglones[it].texto }
                anotarTarjetas(app, renglones.map { it.texto })
                val leida = Parser.analizar(textos.toList(), prefs.moneda)
                if (prefs.diagnostico && textos.any { TRAMO.containsMatchIn(it) }) {
                    val firma = leida?.let { "${it.tarifa}|${it.km}|${it.min}" } ?: "?"
                    if (Guardado.esNueva(app, leida != null, firma)) {
                        Guardado.guardar(this, app, leida != null, textos.take(40).joinToString("\n") +
                            "\n\nCheca entendió (accesibilidad): " +
                            (leida?.let { "tarifa \$${it.tarifa}, ${it.km} km, ${it.min} min" } ?: "nada (no reconoció la oferta)"), null)
                    }
                }
                val r = Rect()
                w.getBoundsInScreen(r)
                // Si el texto no la entiende, no se pide rescate: la foto la lee en ≤ 1.5 s y, si
                // tampoco la entiende, ella pide el rescate (10.57).
                if (leida == null) continue
                val oferta = leida
                mostrarOferta(oferta, app, r, textos, android.os.SystemClock.elapsedRealtime() - inicio, LECTOR_TEXTO)
            }
        } catch (_: Exception) { }
    }

    private fun recolectar(n: AccessibilityNodeInfo, salida: MutableList<Tarjetas.Renglon>, vistos: MutableSet<String>, nivel: Int) {
        if (nivel > 40) return
        // Uber y DiDi guardan en el árbol tarjetas ocultas o fuera de pantalla: no cuentan.
        if (!n.isVisibleToUser) return
        val t = (n.text ?: n.contentDescription)?.toString()?.trim()
        if (!t.isNullOrEmpty() && vistos.add(t)) {
            val r = Rect()
            n.getBoundsInScreen(r)
            salida.add(Tarjetas.Renglon(t, r.top, r.bottom))
        }
        for (i in 0 until n.childCount) {
            n.getChild(i)?.let { recolectar(it, salida, vistos, nivel + 1) }
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
        /** Lecturas seguidas sin ver la oferta para quitar su pop-up. */
        private const val LECTURAS_SIN_OFERTA = 2
        /** De qué lector vino una oferta (se guarda en la revisión para comparar). */
        const val LECTOR_OCR = "ocr"
        const val LECTOR_TEXTO = "texto"
        private val APPS = listOf("Uber", "DiDi")
        /** Lo que espera un pop-up del texto a que la foto lo confirme. */
        private const val ESPERA_FOTO_MS = 2500L
        /** Ofertas seguidas que el texto no ve antes de usar solo la foto en esa app… */
        private const val FALLOS_TEXTO_MAX = 5
        /** …durante este tiempo. */
        private const val TEXTO_PAUSA_MS = 30 * 60_000L
        /** El texto vio la oferta hace menos de esto: la foto puede descansar. */
        private const val TEXTO_FRESCO_MS = 1500L
        private const val VIGILANTE_QUIETO = 10_000L
    }
}
