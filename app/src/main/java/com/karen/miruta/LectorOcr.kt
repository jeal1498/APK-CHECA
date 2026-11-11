package com.karen.miruta

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Rect
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.Text
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions

/**
 * Lee ofertas de una imagen de la pantalla con el OCR del teléfono (sin internet).
 * Lo usan las dos formas de capturar: el servicio de accesibilidad (Android 11+) y
 * la captura de pantalla de Android ([CapturaService], respaldo).
 */
class LectorOcr(private val ctx: Context, private val prefs: Prefs) {
    private val ocr = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

    /** Hay un OCR en curso: la imagen nueva se descarta. */
    @Volatile var ocupado = false
    /** Momento de la captura que se está leyendo (para medir cuánto tarda el pop-up). */
    @Volatile private var inicioCaptura = 0L
        private set
    /** La última lectura vio un tramo «N min (» (hay una oferta en pantalla). */
    @Volatile var ofertaEnPantalla = false
        private set
    /** Lecturas seguidas con tramo a la vista pero sin entender la oferta. */
    @Volatile var fallos = 0
        private set
    /** Conviene leer pronto: hay una oferta a la vista que no se pudo leer. */
    val reintentoRapido: Boolean get() = fallos in 1..FALLOS_MAX

    @Volatile private var cerrado = false
    /** Número de la lectura en curso. */
    @Volatile private var turnoOcr = 0
    private var leyoPasada = false
    private var ultimaGuardada = 0
    private var capAncho = 0
    private var capAlto = 0
    private var divididaHasta = 0L
    private var bmpActual: Bitmap? = null
    private var escX = 1f
    private var escY = 1f
    /** Muestras de brillo de la última imagen a la que se le hizo OCR (filtro de cambios). */
    private var huellaOcr: IntArray? = null
    private var saltosGate = 0

    /**
     * Analiza [bmp] (ya reducida; el lector la recicla). [ancho] y [alto] son el tamaño real
     * de la pantalla, para volver a sus coordenadas. Devuelve false si estaba ocupado.
     */
    fun procesar(bmp: Bitmap, ancho: Int, alto: Int): Boolean {
        if (ocupado || cerrado) { bmp.recycle(); return false }
        if (ancho != capAncho || alto != capAlto) {
            capAncho = ancho
            capAlto = alto
            huellaOcr = null
        }
        // Filtro de cambios (conservador): solo se salta el OCR si NO hay oferta a la vista,
        // no hay lecturas fallidas pendientes y la pantalla es casi igual a la última leída.
        val hu = huella(bmp)
        val previa = huellaOcr
        if (hu != null && previa != null && !ofertaEnPantalla && fallos == 0 &&
            saltosGate < GATE_MAX && parecidas(previa, hu)
        ) {
            saltosGate++
            bmp.recycle()
            return true
        }
        saltosGate = 0
        huellaOcr = hu
        ocupado = true
        inicioCaptura = android.os.SystemClock.elapsedRealtime()
        val turno = ++turnoOcr
        try {
            ocr.process(InputImage.fromBitmap(bmp, 0))
                .addOnSuccessListener { resultado ->
                    // Un error al analizar no debe dejar el lector trabado.
                    try { if (!cerrado) analizar(resultado, bmp) } catch (_: Throwable) { }
                }
                .addOnCompleteListener {
                    bmpActual = null
                    try { bmp.recycle() } catch (_: Throwable) { }
                    // Una lectura vieja (ya dada por perdida) no libera a la que está en curso.
                    if (turno == turnoOcr) ocupado = false
                }
        } catch (e: Throwable) {
            bmp.recycle()
            ocupado = false
        }
        return true
    }

    /** Da por perdida una lectura colgada (ML Kit no respondió) para poder seguir. */
    fun liberar() { turnoOcr++; ocupado = false }

    fun cerrar() {
        cerrado = true
        try { ocr.close() } catch (_: Exception) { }
    }

    private fun analizar(resultado: Text, bmp: Bitmap) {
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
        val hayTramo = todas.any { esTramo(it.text) }
        ofertaEnPantalla = hayTramo
        fallos = if (hayTramo && !leyoPasada) minOf(fallos + 1, 99) else 0
    }

    /**
     * Pantalla horizontal sin zonas conocidas: si las dos mitades tienen un tramo
     * "N min (X km)", es pantalla dividida y cada mitad es una oferta distinta.
     * Se recuerda 10 min para seguir separando aunque un pop-up tape una mitad.
     * Devuelve false si no aplica.
     */
    private fun procesarMitades(todas: List<Text.Line>, escalaX: Float): Boolean {
        if (capAncho <= capAlto) return false
        val mitad = capAncho / 2
        val (izq, der) = todas.filter { it.boundingBox != null }
            .partition { (it.boundingBox!!.exactCenterX() * escalaX) < mitad }
        val hayIzq = izq.any { esTramo(it.text) }
        val hayDer = der.any { esTramo(it.text) }
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
                Guardado.guardar(ctx, app, oferta != null, texto, recorte)
            }
        }
        if (oferta == null) return
        leyoPasada = true
        val servicio = RutaService.instancia ?: return
        val msLectura = android.os.SystemClock.elapsedRealtime() - inicioCaptura
        if (app != null) servicio.mostrarOferta(oferta, app, zona, lineas, msLectura) else servicio.mostrarOferta(oferta)
    }

    /** Brillo de una rejilla de puntos de la imagen. null si falla. */
    private fun huella(b: Bitmap): IntArray? = try {
        val out = IntArray(HU_COLS * HU_FILAS)
        val ancho = b.width
        val alto = b.height
        var i = 0
        for (fy in 0 until HU_FILAS) {
            val y = (fy * alto) / HU_FILAS + alto / (2 * HU_FILAS)
            for (fx in 0 until HU_COLS) {
                val x = (fx * ancho) / HU_COLS + ancho / (2 * HU_COLS)
                val c = b.getPixel(x, y)
                out[i++] = (((c shr 16) and 0xFF) * 30 + ((c shr 8) and 0xFF) * 59 + (c and 0xFF) * 11) / 100
            }
        }
        out
    } catch (e: Exception) { null }

    /** true si casi ningún punto cambió de brillo (menos de GATE_PORC %). */
    private fun parecidas(a: IntArray, b: IntArray): Boolean {
        if (a.size != b.size) return false
        var dif = 0
        for (k in a.indices) if (Math.abs(a[k] - b[k]) > GATE_DIF) dif++
        return dif * 100 < a.size * GATE_PORC
    }

    companion object {
        /** Lado mayor máximo (px) de la imagen que va al OCR. */
        const val LADO_MAX = 1600
        private const val FALLOS_MAX = 6
        private const val HU_COLS = 64
        private const val HU_FILAS = 112
        /** Un punto cuenta como «cambió» si su brillo (0–255) varía más de esto… */
        private const val GATE_DIF = 24
        /** …y la pantalla es «casi igual» si cambió menos de este % de puntos. */
        private const val GATE_PORC = 2
        private const val GATE_MAX = 3
        // «5min (1.7km)» (DiDi, Uber viejo) y «6 min · 2.3 km» (Uber nov 2026).
        val tramoTexto = Regex("""\d\s?min\s*(?:\(|[^\d(\s]{0,3}\s*\d+(?:[.,]\d+)?\s?k?m\b)""", RegexOption.IGNORE_CASE)

        /** Un tramo «N min (» de una oferta; el resumen «… total» (pop-up propio o de otra app) no cuenta. */
        fun esTramo(t: String) = tramoTexto.containsMatchIn(t) && !t.contains("total", ignoreCase = true)

        /** Factor para que el lado mayor quede en [LADO_MAX] o menos (1 si ya cabe). */
        fun factor(ancho: Int, alto: Int): Float {
            val mayor = maxOf(ancho, alto)
            return if (mayor <= LADO_MAX) 1f else LADO_MAX.toFloat() / mayor
        }

        /** Reduce la imagen para que el OCR sea más rápido y gaste menos batería. */
        fun reducir(b: Bitmap): Bitmap {
            val f = factor(b.width, b.height)
            if (f >= 1f) return b
            val r = Bitmap.createScaledBitmap(
                b, maxOf(1, (b.width * f).toInt()), maxOf(1, (b.height * f).toInt()), true
            )
            if (r !== b) b.recycle()
            return r
        }
    }
}
