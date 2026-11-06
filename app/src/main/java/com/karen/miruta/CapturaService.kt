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
import android.util.DisplayMetrics
import android.view.WindowManager
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.ImageReader
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.Handler
import android.os.HandlerThread
import android.os.IBinder

/**
 * Respaldo de la lectura (Android 10 o menos, o si la captura por accesibilidad falla):
 * usa la captura de pantalla de Android, toma una imagen cada ~1 s y la pasa a [LectorOcr].
 * La pantalla virtual se crea ya reducida (lado mayor ≤ 1600 px): Android la escala sin
 * costo extra y cada imagen pesa ~4 veces menos que a resolución completa.
 */
class CapturaService : Service() {
    private var proyeccion: MediaProjection? = null
    private var display: VirtualDisplay? = null
    private var lector: ImageReader? = null
    private var hilo: HandlerThread? = null
    private var handler: Handler? = null
    private var ocr: LectorOcr? = null
    private lateinit var prefs: Prefs
    /** Tamaño real de la pantalla y de la pantalla virtual (reducida). */
    private var capAncho = 0
    private var capAlto = 0
    private var virAncho = 0
    private var virAlto = 0
    /** Lecturas seguidas que se saltaron por no haber Uber ni DiDi en pantalla. */
    private var saltadas = 0

    private val ciclo = object : Runnable {
        override fun run() {
            leerPantalla()
            // Si se ve una oferta pero no se pudo leer, reintenta pronto (unas cuantas veces):
            // suele ser un cuadro a medias de la animación o un OCR dudoso.
            val rapido = ocr?.reintentoRapido == true
            handler?.postDelayed(this, if (rapido) REINTENTO_MS else INTERVALO_MS)
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACCION_DETENER) {
            // También apaga la lectura por accesibilidad (botón «Detener» de la notificación o del widget).
            RutaService.instancia?.detenerLectura()
            Prefs(this).lecturaActiva = false
            detener()
            return START_NOT_STICKY
        }
        prefs = Prefs(this)
        // Android 14+ no deja reusar el permiso de captura: ahí no se pide que Android reinicie el servicio.
        val reinicio = if (Build.VERSION.SDK_INT < 34) START_REDELIVER_INTENT else START_NOT_STICKY
        try {
            iniciarPrimerPlano()
        } catch (e: Exception) {
            // Android 14+ rechaza el primer plano sin un permiso de captura vigente.
            detener(avisar = true)
            return START_NOT_STICKY
        }

        val codigo = intent?.getIntExtra(EXTRA_CODIGO, Activity.RESULT_CANCELED) ?: Activity.RESULT_CANCELED
        val datos: Intent? = if (Build.VERSION.SDK_INT >= 33) {
            intent?.getParcelableExtra(EXTRA_DATOS, Intent::class.java)
        } else {
            @Suppress("DEPRECATION") intent?.getParcelableExtra(EXTRA_DATOS)
        }
        if (codigo != Activity.RESULT_OK || datos == null || proyeccion != null) {
            if (proyeccion == null) detener()
            return if (proyeccion != null) reinicio else START_NOT_STICKY
        }

        val mpm = getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        val p = try { mpm.getMediaProjection(codigo, datos) } catch (e: Exception) { null }
            ?: run { detener(avisar = true); return START_NOT_STICKY }
        proyeccion = p

        hilo = HandlerThread("miruta-ocr").also { it.start() }
        handler = Handler(hilo!!.looper)
        ocr = LectorOcr(this, prefs)

        p.registerCallback(object : MediaProjection.Callback() {
            override fun onStop() { detener(avisar = true) }
        }, handler)

        val dm = medidasReales()
        medir(dm)
        val r = ImageReader.newInstance(virAncho, virAlto, PixelFormat.RGBA_8888, 2)
        lector = r
        display = try {
            p.createVirtualDisplay(
                "miruta", virAncho, virAlto, densidad(dm),
                DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
                r.surface, null, handler
            )
        } catch (e: Exception) { null }
        if (display == null) { detener(avisar = true); return START_NOT_STICKY }
        corriendo = true
        cortada = false
        WidgetCheca.actualizar(this)
        (getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager).apply {
            cancel(ID_AVISO)
            cancel(3) // aviso «Checa no está leyendo» de RutaService
        }
        handler?.postDelayed(ciclo, INTERVALO_MS)
        return reinicio
    }

    private fun medidasReales(): DisplayMetrics {
        val dm = DisplayMetrics()
        @Suppress("DEPRECATION")
        (getSystemService(Context.WINDOW_SERVICE) as WindowManager).defaultDisplay.getRealMetrics(dm)
        return dm
    }

    private fun medir(dm: DisplayMetrics) {
        capAncho = dm.widthPixels
        capAlto = dm.heightPixels
        val f = LectorOcr.factor(capAncho, capAlto)
        virAncho = maxOf(1, (capAncho * f).toInt())
        virAlto = maxOf(1, (capAlto * f).toInt())
    }

    private fun densidad(dm: DisplayMetrics): Int =
        maxOf(1, (dm.densityDpi * LectorOcr.factor(dm.widthPixels, dm.heightPixels)).toInt())

    /** Si la pantalla giró (vertical ↔ horizontal), ajusta la captura al nuevo tamaño. */
    private fun revisarRotacion() {
        val dm = medidasReales()
        if (dm.widthPixels == capAncho && dm.heightPixels == capAlto) return
        val d = display ?: return
        try {
            medir(dm)
            val nuevo = ImageReader.newInstance(virAncho, virAlto, PixelFormat.RGBA_8888, 2)
            d.resize(virAncho, virAlto, densidad(dm))
            d.surface = nuevo.surface
            lector?.close()
            lector = nuevo
        } catch (_: Exception) { }
    }

    private fun leerPantalla() {
        val lectorOcr = ocr ?: return
        if (lectorOcr.ocupado) return
        revisarRotacion()
        val r = lector ?: return
        val img = try { r.acquireLatestImage() } catch (e: Exception) { null } ?: return
        // Sin Uber ni DiDi en pantalla no se hace el OCR (ahorra batería). Por si Android no
        // reporta alguna ventana, igual se lee 1 de cada 2 segundos.
        if (RutaService.instancia?.hayQueLeer() == false && saltadas < SALTAR_MAX) {
            saltadas++
            img.close()
            return
        }
        saltadas = 0
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
            bmp = LectorOcr.reducir(entero)
        } catch (e: Exception) {
            return
        } finally {
            img.close()
        }
        lectorOcr.procesar(bmp, capAncho, capAlto)
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
        try { ocr?.cerrar() } catch (_: Exception) { }
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
        // Antes 4: hasta 4 s sin mirar. El filtro de cambios ya cuida la batería.
        private const val SALTAR_MAX = 1
        @Volatile var corriendo = false
        /** Android cortó la lectura sin que el conductor la detuviera (Inicio lo avisa en amarillo). */
        @Volatile var cortada = false
    }
}
