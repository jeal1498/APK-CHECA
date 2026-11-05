package com.karen.miruta

import android.app.Activity
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.Bitmap
import android.graphics.PixelFormat
import android.graphics.Rect
import android.util.DisplayMetrics
import android.view.WindowManager
import com.google.mlkit.vision.text.Text
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.Image
import android.media.ImageReader
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.Handler
import android.os.HandlerThread
import android.os.IBinder
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.TextRecognizer
import com.google.mlkit.vision.text.latin.TextRecognizerOptions

/**
 * Modo "lectura de pantalla": toma una imagen de la pantalla cada ~1 s,
 * reconoce el texto en el teléfono (sin internet) y busca una oferta.
 */
class CapturaService : Service() {
    private var proyeccion: MediaProjection? = null
    private var display: VirtualDisplay? = null
    private var lector: ImageReader? = null
    private var hilo: HandlerThread? = null
    private var handler: Handler? = null
    private var ocr: TextRecognizer? = null
    private lateinit var prefs: Prefs
    @Volatile private var procesando = false
    private var ultimaGuardada = 0
    private var capAncho = 0
    private var capAlto = 0
    private var divididaHasta = 0L
    /** Lecturas seguidas que se saltaron por no haber Uber ni DiDi en pantalla. */
    private var saltadas = 0
    /** Imagen que se está analizando y su escala, para recortar la foto de «Lo guardado». */
    private var bmpActual: Bitmap? = null
    private var escX = 1f
    private var escY = 1f

    /** Muestras de brillo de la última imagen a la que se le hizo OCR (filtro de cambios). */
    @Volatile private var huellaOcr: IntArray? = null
    /** Lecturas seguidas que se saltó el filtro de cambios por ser casi idénticas. */
    private var saltosGate = 0
    /** La última lectura vio un tramo «N min (» (hay una oferta en pantalla). */
    @Volatile private var ofertaEnPantalla = false
    /** Lecturas seguidas con tramo a la vista pero sin entender la oferta (se reintenta rápido). */
    @Volatile private var fallos = 0
    @Volatile private var leyoPasada = false

    private val ciclo = object : Runnable {
        override fun run() {
            leerPantalla()
            // Si se ve una oferta pero no se pudo leer, reintenta pronto (unas cuantas veces):
            // suele ser un cuadro a medias de la animación o un OCR dudoso.
            val rapido = fallos in 1..FALLOS_MAX
            handler?.postDelayed(this, if (rapido) REINTENTO_MS else INTERVALO_MS)
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACCION_DETENER) {
            detener()
            return START_NOT_STICKY
        }
        prefs = Prefs(this)
        iniciarPrimerPlano()

        val codigo = intent?.getIntExtra(EXTRA_CODIGO, Activity.RESULT_CANCELED) ?: Activity.RESULT_CANCELED
        val datos: Intent? = if (Build.VERSION.SDK_INT >= 33) {
            intent?.getParcelableExtra(EXTRA_DATOS, Intent::class.java)
        } else {
            @Suppress("DEPRECATION") intent?.getParcelableExtra(EXTRA_DATOS)
        }
        if (codigo != Activity.RESULT_OK || datos == null || proyeccion != null) {
            if (proyeccion == null) detener()
            return START_NOT_STICKY
        }

        val mpm = getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        val p = mpm.getMediaProjection(codigo, datos) ?: run { detener(); return START_NOT_STICKY }
        proyeccion = p

        hilo = HandlerThread("miruta-ocr").also { it.start() }
        handler = Handler(hilo!!.looper)
        ocr = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

        p.registerCallback(object : MediaProjection.Callback() {
            override fun onStop() { detener(avisar = true) }
        }, handler)

        val dm = medidasReales()
        capAncho = dm.widthPixels
        capAlto = dm.heightPixels
        val r = ImageReader.newInstance(capAncho, capAlto, PixelFormat.RGBA_8888, 2)
        lector = r
        display = p.createVirtualDisplay(
            "miruta", capAncho, capAlto, dm.densityDpi,
            DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
            r.surface, null, handler
        )
        corriendo = true
        cortada = false
        WidgetCheca.actualizar(this)
        (getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager).apply {
            cancel(ID_AVISO)
            cancel(3) // aviso «Checa no está leyendo» de RutaService
        }
        handler?.postDelayed(ciclo, INTERVALO_MS)
        return START_NOT_STICKY
    }

    private fun medidasReales(): DisplayMetrics {
        val dm = DisplayMetrics()
        @Suppress("DEPRECATION")
        (getSystemService(Context.WINDOW_SERVICE) as WindowManager).defaultDisplay.getRealMetrics(dm)
        return dm
    }

    /** Si la pantalla giró (vertical ↔ horizontal), ajusta la captura al nuevo tamaño. */
    private fun revisarRotacion() {
        val dm = medidasReales()
        if (dm.widthPixels == capAncho && dm.heightPixels == capAlto) return
        val d = display ?: return
        try {
            capAncho = dm.widthPixels
            capAlto = dm.heightPixels
            huellaOcr = null
            val nuevo = ImageReader.newInstance(capAncho, capAlto, PixelFormat.RGBA_8888, 2)
            d.resize(capAncho, capAlto, dm.densityDpi)
            d.surface = nuevo.surface
            lector?.close()
            lector = nuevo
        } catch (_: Exception) { }
    }

    private fun leerPantalla() {
        if (procesando) return
        revisarRotacion()
        val r = lector ?: return
        val img = try { r.acquireLatestImage() } catch (e: Exception) { null } ?: return
        // Sin Uber ni DiDi en pantalla no se hace el OCR (ahorra batería). Por si Android no
        // reporta alguna ventana, igual se lee 1 de cada 5 segundos.
        if (RutaService.instancia?.hayQueLeer() == false && saltadas < SALTAR_MAX) {
            saltadas++
            img.close()
            return
        }
        saltadas = 0
        // Filtro de cambios (conservador): solo se salta el OCR si NO hay oferta a la vista,
        // no hay lecturas fallidas pendientes y la pantalla es casi igual a la última leída.
        // Con una oferta en pantalla siempre se lee (los cambios pueden ser solo unos dígitos).
        // Como máximo se saltan GATE_MAX lecturas seguidas.
        val hu = huella(img)
        val previa = huellaOcr
        if (hu != null && previa != null && previa.size == hu.size && !ofertaEnPantalla &&
            fallos == 0 && saltosGate < GATE_MAX && parecidas(previa, hu)
        ) {
            saltosGate++
            img.close()
            return
        }
        saltosGate = 0
        huellaOcr = hu
        val bmp: Bitmap
        try {
            val plano = img.planes[0]
            val pixStride = plano.pixelStride
            val rowStride = plano.rowStride
            val anchoBuffer = rowStride / pixStride
            val completo = Bitmap.createBitmap(anchoBuffer, img.height, Bitmap.Config.ARGB_8888)
            completo.copyPixelsFromBuffer(plano.buffer)
            val entero = if (anchoBuffer != img.width) {
                Bitmap.createBitmap(completo, 0, 0, img.width, img.height).also { completo.recycle() }
            } else completo
            // El OCR cuesta según los píxeles: se reduce el lado mayor (ya se escala al volver a coordenadas de pantalla).
            bmp = reducir(entero)
        } catch (e: Exception) {
            return
        } finally {
            img.close()
        }

        procesando = true
        val cliente = ocr ?: run { procesando = false; return }
        cliente.process(InputImage.fromBitmap(bmp, 0))
            .addOnSuccessListener { resultado ->
                val escalaX = capAncho.toFloat() / bmp.width
                val escalaY = capAlto.toFloat() / bmp.height
                // Lo que está debajo de nuestros pop-ups no se lee (son nuestros números).
                val tapado = RutaService.instancia?.rectsPopups().orEmpty()
                val todas = resultado.textBlocks.flatMap { it.lines }.filter { l ->
                    val b = l.boundingBox ?: return@filter true
                    val x = (b.exactCenterX() * escalaX).toInt()
                    val y = (b.exactCenterY() * escalaY).toInt()
                    tapado.none { it.contains(x, y) }
                }
                bmpActual = bmp
                escX = escalaX
                escY = escalaY
                leyoPasada = false
                val zonas = RutaService.instancia?.zonasApps().orEmpty()
                if (zonas.isEmpty()) {
                    // No sabemos dónde está cada app. En horizontal con dos ofertas
                    // (pantalla dividida) se separa por mitades para no mezclarlas.
                    if (!procesarMitades(todas, escalaX)) procesarGrupo(todas, null, null)
                } else {
                    // Pantalla dividida: cada línea pertenece a la app cuya ventana la contiene.
                    val grupos = LinkedHashMap<Zona, MutableList<Text.Line>>()
                    for (l in todas) {
                        val b = l.boundingBox ?: continue
                        val x = (b.exactCenterX() * escalaX).toInt()
                        val y = (b.exactCenterY() * escalaY).toInt()
                        val z = zonas.firstOrNull { it.rect.contains(x, y) } ?: continue
                        grupos.getOrPut(z) { ArrayList() }.add(l)
                    }
                    for ((z, ls) in grupos) procesarGrupo(ls, z.app, z.rect)
                }
                // ¿Hay una oferta a la vista (tramo «N min (»)? Si sí pero no se entendió, se reintenta rápido.
                val hayTramo = todas.any { tramoTexto.containsMatchIn(it.text) }
                ofertaEnPantalla = hayTramo
                fallos = if (hayTramo && !leyoPasada) minOf(fallos + 1, 99) else 0
            }
            .addOnCompleteListener {
                bmpActual = null
                bmp.recycle()
                procesando = false
            }
    }

    /**
     * Pantalla horizontal sin zonas conocidas: si las dos mitades tienen un tramo
     * "N min (X km)", es pantalla dividida y cada mitad es una oferta distinta.
     * Se recuerda 10 min para seguir separando aunque un pop-up tape una mitad
     * (si no, se leía la pantalla entera, se mezclaban ofertas y parpadeaba).
     * Devuelve false si no aplica.
     */
    private fun procesarMitades(todas: List<Text.Line>, escalaX: Float): Boolean {
        if (capAncho <= capAlto) return false
        val mitad = capAncho / 2
        val (izq, der) = todas.filter { it.boundingBox != null }
            .partition { (it.boundingBox!!.exactCenterX() * escalaX) < mitad }
        val hayIzq = izq.any { tramoTexto.containsMatchIn(it.text) }
        val hayDer = der.any { tramoTexto.containsMatchIn(it.text) }
        val ahora = System.currentTimeMillis()
        if (hayIzq && hayDer) divididaHasta = ahora + 10 * 60_000L
        if (ahora > divididaHasta) return false
        // Por defecto DiDi a la izquierda y Uber a la derecha; el texto manda si lo dice.
        val appIzq = detectarApp(izq) ?: "DiDi"
        var appDer = detectarApp(der) ?: "Uber"
        if (appDer == appIzq) appDer = if (appIzq == "DiDi") "Uber" else "DiDi"
        if (hayIzq) procesarGrupo(izq, appIzq, Rect(0, 0, mitad, capAlto))
        if (hayDer) procesarGrupo(der, appDer, Rect(mitad, 0, capAncho, capAlto))
        return true
    }

    private fun detectarApp(ls: List<Text.Line>): String? {
        val t = ls.joinToString(" ") { it.text }.lowercase()
        return when {
            t.contains("uber") || t.contains("me interesa") || t.contains("/km (est") -> "Uber"
            t.contains("didi") || t.contains("pon tu precio") || t.contains("aceptar $") -> "DiDi"
            else -> null
        }
    }

    /** Corrige confusiones típicas del OCR: "S151.34" → "$151.34" (solo en monedas con «$»). */
    private fun normalizar(s: String, conPeso: Boolean): String =
        if (conPeso) s.trim().replace(Regex("""^[S§]\s?(?=\d)"""), "\\$") else s.trim()

    private fun procesarGrupo(grupo: List<Text.Line>, app: String?, zona: Rect?) {
        val moneda = prefs.moneda
        val conPeso = moneda.simbolos().contains("$")
        val ordenadas = grupo.sortedWith(compareBy({ it.boundingBox?.top ?: 0 }, { it.boundingBox?.left ?: 0 }))
        // La tarifa es el monto con letra más grande: se pone primero.
        val tarifa = ordenadas
            .filter { Parser.esMontoSolo(normalizar(it.text, conPeso), moneda) }
            .maxByOrNull { it.boundingBox?.height() ?: 0 }
        val lineas = (listOfNotNull(tarifa) + ordenadas.filter { it !== tarifa }).map { normalizar(it.text, conPeso) }

        val oferta = Parser.analizar(lineas, moneda)
        // «Guardar pantallas de ofertas»: texto y foto (solo la zona de la app) de cada oferta distinta.
        if (prefs.diagnostico && lineas.any { tramoTexto.containsMatchIn(it) }) {
            val texto = lineas.take(40).joinToString("\n") + "\n\nCheca entendió: " +
                (oferta?.let { "tarifa \$${it.tarifa}, ${it.km} km, ${it.min} min" } ?: "nada (no reconoció la oferta)")
            if (lineas.hashCode() != ultimaGuardada) {
                ultimaGuardada = lineas.hashCode()
                prefs.ultimaLectura = "[${app ?: "pantalla"}]\n" + texto
            }
            val firma = oferta?.let { "${it.tarifa}|${it.km}|${it.min}" } ?: "?"
            // Con el pop-up encima la oferta se ve tapada: eso no es una lectura fallida.
            val tapada = oferta == null && RutaService.instancia?.popupVisible(app) == true
            if (!tapada && Guardado.esNueva(app, oferta != null, firma)) {
                val b = bmpActual
                val recorte = if (b == null) null else Guardado.recortar(b, zona?.let {
                    Rect((it.left / escX).toInt(), (it.top / escY).toInt(), (it.right / escX).toInt(), (it.bottom / escY).toInt())
                })
                Guardado.guardar(this, app, oferta != null, texto, recorte)
            }
        }
        if (oferta == null) return
        leyoPasada = true
        val servicio = RutaService.instancia ?: return
        if (app != null) servicio.mostrarOferta(oferta, app, zona) else servicio.mostrarOferta(oferta)
    }

    /** Reduce la imagen para que el OCR sea más rápido y gaste menos batería. */
    private fun reducir(b: Bitmap): Bitmap {
        val mayor = maxOf(b.width, b.height)
        if (mayor <= LADO_MAX) return b
        val f = LADO_MAX.toFloat() / mayor
        val r = Bitmap.createScaledBitmap(
            b, maxOf(1, (b.width * f).toInt()), maxOf(1, (b.height * f).toInt()), true
        )
        if (r !== b) b.recycle()
        return r
    }

    /** Brillo de una rejilla de puntos de la imagen (barato: no crea bitmaps). null si falla. */
    private fun huella(img: Image): IntArray? {
        return try {
            val plano = img.planes[0]
            val buf = plano.buffer
            val ps = plano.pixelStride
            val rs = plano.rowStride
            val ancho = img.width
            val alto = img.height
            val out = IntArray(HU_COLS * HU_FILAS)
            var i = 0
            for (fy in 0 until HU_FILAS) {
                val y = (fy * alto) / HU_FILAS + alto / (2 * HU_FILAS)
                for (fx in 0 until HU_COLS) {
                    val x = (fx * ancho) / HU_COLS + ancho / (2 * HU_COLS)
                    val o = y * rs + x * ps
                    val r = buf.get(o).toInt() and 0xFF
                    val g = buf.get(o + 1).toInt() and 0xFF
                    val b = buf.get(o + 2).toInt() and 0xFF
                    out[i++] = (r * 30 + g * 59 + b * 11) / 100
                }
            }
            out
        } catch (e: Exception) { null }
    }

    /** true si casi ningún punto cambió de brillo (menos de GATE_PORC %). */
    private fun parecidas(a: IntArray, b: IntArray): Boolean {
        var dif = 0
        for (k in a.indices) if (Math.abs(a[k] - b[k]) > GATE_DIF) dif++
        return dif * 100 < a.size * GATE_PORC
    }

    private fun iniciarPrimerPlano() {
        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.createNotificationChannel(
            NotificationChannel(CANAL, "Lectura de pantalla", NotificationManager.IMPORTANCE_LOW)
        )
        val detenerPi = PendingIntent.getService(
            this, 1, Intent(this, CapturaService::class.java).setAction(ACCION_DETENER),
            PendingIntent.FLAG_IMMUTABLE
        )
        val abrirPi = PendingIntent.getActivity(
            this, 2, Intent(this, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE
        )
        val n = Notification.Builder(this, CANAL)
            .setContentTitle("Checa está leyendo la pantalla")
            .setContentText("Toca \"Detener\" para apagarlo")
            .setSmallIcon(R.drawable.ic_notif)
            .setContentIntent(abrirPi)
            .addAction(Notification.Action.Builder(null, "Detener", detenerPi).build())
            .setOngoing(true)
            .build()
        if (Build.VERSION.SDK_INT >= 29) {
            startForeground(1, n, ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION)
        } else {
            startForeground(1, n)
        }
    }

    /** Avisa que la lectura se cortó sola (no la detuvo el usuario) para que la reactive. */
    private fun avisarDetenida() {
        try {
            val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            nm.createNotificationChannel(
                NotificationChannel(CANAL_AVISO, "Avisos de Checa", NotificationManager.IMPORTANCE_HIGH)
            )
            val abrirPi = PendingIntent.getActivity(
                this, 3, Intent(this, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE
            )
            nm.notify(
                ID_AVISO,
                Notification.Builder(this, CANAL_AVISO)
                    .setContentTitle("Checa dejó de leer")
                    .setContentText("Ya no verás pop-ups. Toca para volver a iniciar la lectura.")
                    .setSmallIcon(R.drawable.ic_notif)
                    .setContentIntent(abrirPi)
                    .setAutoCancel(true)
                    .build()
            )
        } catch (_: Exception) { }
    }

    private fun detener(avisar: Boolean = false) {
        val estabaLeyendo = corriendo
        corriendo = false
        WidgetCheca.actualizar(this)
        handler?.removeCallbacksAndMessages(null)
        try { display?.release() } catch (_: Exception) { }
        try { lector?.close() } catch (_: Exception) { }
        val p = proyeccion
        proyeccion = null
        try { p?.stop() } catch (_: Exception) { }
        try { ocr?.close() } catch (_: Exception) { }
        ocr = null
        hilo?.quitSafely()
        hilo = null
        handler = null
        display = null
        lector = null
        stopForeground(STOP_FOREGROUND_REMOVE)
        if (avisar && estabaLeyendo) { cortada = true; avisarDetenida() }
        stopSelf()
    }

    override fun onDestroy() {
        if (proyeccion != null) detener()
        corriendo = false
        WidgetCheca.actualizar(this)
        super.onDestroy()
    }

    companion object {
        const val EXTRA_CODIGO = "codigo"
        const val EXTRA_DATOS = "datos"
        const val ACCION_DETENER = "detener"
        private const val CANAL = "lectura"
        private const val CANAL_AVISO = "avisos"
        private const val ID_AVISO = 2
        private const val INTERVALO_MS = 1000L
        /** Cadencia cuando hay una oferta a la vista que no se pudo leer. */
        private const val REINTENTO_MS = 500L
        private const val FALLOS_MAX = 6
        // Antes 4: hasta 4 s sin mirar. El filtro de cambios ya cuida la batería.
        private const val SALTAR_MAX = 1
        /** Lado mayor máximo (px) de la imagen que va al OCR. */
        private const val LADO_MAX = 1600
        private const val HU_COLS = 64
        private const val HU_FILAS = 112
        /** Un punto cuenta como «cambió» si su brillo (0–255) varía más de esto… */
        private const val GATE_DIF = 24
        /** …y la pantalla es «casi igual» si cambió menos de este % de puntos. */
        private const val GATE_PORC = 2
        private const val GATE_MAX = 3
        private val tramoTexto = Regex("""\d\s?min\s*\(""", RegexOption.IGNORE_CASE)
        @Volatile var corriendo = false
        /** Android cortó la lectura sin que el conductor la detuviera (Inicio lo avisa en amarillo). */
        @Volatile var cortada = false
    }
}
