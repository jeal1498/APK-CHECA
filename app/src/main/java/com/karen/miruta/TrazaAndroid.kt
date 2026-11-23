package com.karen.miruta

import android.content.Context
import java.io.File
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

/**
 * E3 (10.81) — Trazabilidad en el teléfono: guarda en una cola acotada qué pasó con cada lectura
 * (fotograma, resultado, motivo, tiempos) y la manda a `registrar-lectura` en segundo plano.
 *
 * Reglas: ENCENDIDA por defecto (Prefs.trazabilidad) y solo con cuenta; nunca lanza excepciones al lector;
 * todo el trabajo pesado (huellas, archivo, red) en un hilo aparte; no cambia lo que se muestra ni el
 * lector (Parser/Tarjetas/Moneda: guardia en herramientas/guardia_parser.py).
 *
 * Captura exacta: LectorOcr fija [fotograma] justo antes de procesar ESA imagen (hilo principal, mismo
 * callback que llega a mostrarOferta); Guardado avisa el sha256 del WebP de ese mismo fotograma con
 * [anotarCaptura]. El evento espera hasta [ESPERA_CAPTURA_MS] su captura; si no llega, va «sin_captura».
 * Así una imagen nunca se asocia a una lectura por «la más reciente».
 */
object TrazaAndroid {
    private const val ESPERA_CAPTURA_MS = 3_000L
    private const val ESPERA_TRAS_SIN_SESION_MS = 15 * 60_000L

    private val hilo = Executors.newSingleThreadScheduledExecutor { r -> Thread(r, "traza").apply { isDaemon = true } }
    @Volatile private var cola: ColaTraza? = null
    private val pendientes = LinkedHashMap<String, Traza.Evento>()   // por fotograma, mientras llega su captura
    private var intento = 0
    private var programado = false
    private var pausaHasta = 0L
    @Volatile var errores = 0L
        private set

    /** Fotograma que está leyendo el OCR ahora mismo (id, hora). Lo pone y lo quita LectorOcr. */
    @Volatile var fotograma: Pair<String, Long>? = null

    /** 10.86: etapas de la lectura que se está informando ahora (OCR y Parser: estado, ms y motivo). Lo pone y lo quita LectorOcr. */
    @Volatile var etapas: List<Traza.Etapa> = emptyList()

    fun activa(ctx: Context): Boolean = try { Prefs(ctx).trazabilidad && Cuenta.conectada(ctx) } catch (_: Throwable) { false }

    private fun cola(ctx: Context): ColaTraza = cola ?: synchronized(this) {
        cola ?: ColaTraza(Archivo(File(ctx.applicationContext.filesDir, "traza.txt"))).also { cola = it }
    }

    private class Archivo(private val f: File) : AlmacenTraza {
        override fun leer(): String? = if (f.exists()) f.readText() else null
        override fun escribir(texto: String) {
            val tmp = File(f.parentFile, f.name + ".tmp")
            tmp.writeText(texto)
            if (!tmp.renameTo(f)) { f.delete(); tmp.renameTo(f) }
        }
    }

    /** Anota un resultado. Barato en el hilo que llama: solo copia datos; lo demás va al hilo «traza». */
    fun anotar(
        ctx: Context, app: String, lector: String, fot: Pair<String, Long>?, resultado: String, motivo: String?,
        oferta: Oferta?, nivel: String?, moneda: String?, lineas: List<String>, msLectura: Long, orientacion: String,
        etapas: List<Traza.Etapa> = emptyList(),
    ) {
        val appCtx = ctx.applicationContext
        val ahora = System.currentTimeMillis()
        val copia = ArrayList(lineas)
        val copiaEtapas = ArrayList(etapas)
        try {
            hilo.execute {
                try {
                    val e = Traza.Evento(
                        app = Traza.appCorta(app), versionApp = BuildConfig.VERSION_NAME, lector = lector,
                        fotogramaId = fot?.first, tsFotogramaMs = fot?.second, tsEventoMs = ahora, resultado = resultado,
                        motivo = motivo, oferta = if (resultado == "oferta") oferta else null, nivel = if (resultado == "oferta") nivel else null,
                        moneda = moneda, tarjetas = null, msLectura = msLectura.takeIf { it >= 0 },
                        lineasSha256 = if (copia.isEmpty()) null else Traza.huellaLineas(copia),
                        lineasRedactadas = Redaccion.redactar(copia),
                        telefono = "${android.os.Build.MANUFACTURER} ${android.os.Build.MODEL}".trim().take(40),
                        android = android.os.Build.VERSION.RELEASE?.take(8), orientacion = orientacion,
                        etapas = copiaEtapas,
                    )
                    val f = e.fotogramaId
                    if (f != null && resultado == "oferta") {
                        pendientes[f] = e
                        hilo.schedule(Runnable { soltar(appCtx, f) }, ESPERA_CAPTURA_MS, TimeUnit.MILLISECONDS)
                    } else encolar(appCtx, e)
                } catch (_: Throwable) { errores++ }
            }
        } catch (_: Throwable) { errores++ }
    }

    /** Sha256 del WebP de [fot] (el mismo arreglo de bytes que se sube si hay caso). */
    fun anotarCaptura(fot: String?, bytes: ByteArray) {
        if (fot == null) return
        try {
            hilo.execute {
                try { pendientes[fot]?.let { pendientes[fot] = it.copy(capturaSha256 = Traza.sha256(bytes)) } } catch (_: Throwable) { errores++ }
            }
        } catch (_: Throwable) { errores++ }
    }

    private fun soltar(ctx: Context, fot: String) {
        pendientes.remove(fot)?.let { encolar(ctx, it) }
    }

    private fun encolar(ctx: Context, e: Traza.Evento) {
        cola(ctx).agregar(e.id, e.aJson())
        programar(ctx, 5_000L)
    }

    private fun programar(ctx: Context, demora: Long) {
        if (programado || System.currentTimeMillis() < pausaHasta) return
        programado = true
        hilo.schedule(Runnable { programado = false; enviar(ctx) }, demora, TimeUnit.MILLISECONDS)
    }

    private fun enviar(ctx: Context) {
        try {
            if (!activa(ctx)) return
            val c = cola(ctx)
            repeat(5) {   // hasta 5 lotes seguidos; luego espera al siguiente evento
                val lote = c.lote()
                if (lote.isEmpty()) return
                val r = Cuenta.enviarTrazas(ctx, Traza.lote(lote.map { it.second }))
                when (val a = RespuestaTraza.decidir(r?.first, r?.second, lote.map { it.first })) {
                    is RespuestaTraza.Accion.Quitar -> { c.quitar(a.ids); intento = 0; if (a.ids.isEmpty()) return }
                    is RespuestaTraza.Accion.Descartar -> { c.quitar(a.ids); intento = 0 }
                    RespuestaTraza.Accion.Esperar -> { pausaHasta = System.currentTimeMillis() + ESPERA_TRAS_SIN_SESION_MS; return }
                    RespuestaTraza.Accion.Reintentar -> {
                        intento++
                        programar(ctx, Reintento.espera(intento, Math.random()))
                        return
                    }
                }
            }
        } catch (_: Throwable) { errores++ }
    }
}
