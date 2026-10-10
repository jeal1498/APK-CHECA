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
    private var ultimaCaptura = 0
    private var capAncho = 0
    private var capAlto = 0

    private val ciclo = object : Runnable {
        override fun run() {
            leerPantalla()
            handler?.postDelayed(this, INTERVALO_MS)
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
            override fun onStop() { detener() }
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
        val bmp: Bitmap
        try {
            val plano = img.planes[0]
            val pixStride = plano.pixelStride
            val rowStride = plano.rowStride
            val anchoBuffer = rowStride / pixStride
            val completo = Bitmap.createBitmap(anchoBuffer, img.height, Bitmap.Config.ARGB_8888)
            completo.copyPixelsFromBuffer(plano.buffer)
            bmp = if (anchoBuffer != img.width) {
                Bitmap.createBitmap(completo, 0, 0, img.width, img.height).also { completo.recycle() }
            } else completo
        } catch (e: Exception) {
            return
        } finally {
            img.close()
        }

        procesando = true
        val cliente = ocr ?: run { procesando = false; return }
        cliente.process(InputImage.fromBitmap(bmp, 0))
            .addOnSuccessListener { resultado ->
                val todas = resultado.textBlocks.flatMap { it.lines }
                val escalaX = capAncho.toFloat() / bmp.width
                val escalaY = capAlto.toFloat() / bmp.height
                val zonas = RutaService.instancia?.zonasApps().orEmpty()
                if (zonas.isEmpty()) {
                    // No sabemos qué app es (p. ej. probando con una captura en la galería).
                    procesarGrupo(todas, null, null)
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
            }
            .addOnCompleteListener {
                bmp.recycle()
                procesando = false
            }
    }

    /** Corrige confusiones típicas del OCR: "S151.34" → "$151.34". */
    private fun normalizar(s: String): String =
        s.trim().replace(Regex("""^[S§]\s?(?=\d)"""), "\\$")

    private fun procesarGrupo(grupo: List<Text.Line>, app: String?, zona: Rect?) {
        val ordenadas = grupo.sortedWith(compareBy({ it.boundingBox?.top ?: 0 }, { it.boundingBox?.left ?: 0 }))
        // La tarifa es el monto con letra más grande: se pone primero.
        val tarifa = ordenadas
            .filter { soloMonto.matches(normalizar(it.text)) }
            .maxByOrNull { it.boundingBox?.height() ?: 0 }
        val lineas = (listOfNotNull(tarifa) + ordenadas.filter { it !== tarifa }).map { normalizar(it.text) }

        if (prefs.diagnostico && lineas.any { it.contains("min", ignoreCase = true) }) {
            val firma = lineas.hashCode()
            if (firma != ultimaCaptura) {
                ultimaCaptura = firma
                prefs.guardarCaptura("[${app ?: "pantalla"} · lectura de pantalla]\n" + lineas.joinToString("\n"))
            }
        }
        val oferta = Parser.analizar(lineas) ?: return
        val servicio = RutaService.instancia ?: return
        if (app != null) servicio.mostrarOferta(oferta, app, zona) else servicio.mostrarOferta(oferta)
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
            .setContentTitle("Neta está leyendo la pantalla")
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

    private fun detener() {
        corriendo = false
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
        stopSelf()
    }

    override fun onDestroy() {
        if (proyeccion != null) detener()
        corriendo = false
        super.onDestroy()
    }

    companion object {
        const val EXTRA_CODIGO = "codigo"
        const val EXTRA_DATOS = "datos"
        const val ACCION_DETENER = "detener"
        private const val CANAL = "lectura"
        private const val INTERVALO_MS = 1000L
        private val soloMonto = Regex("""^\$\s?[\d,]+(?:\.\d{1,2})?$""")
        @Volatile var corriendo = false
    }
}
