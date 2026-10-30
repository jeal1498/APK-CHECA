package com.karen.miruta

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ColorFilter
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PixelFormat
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.drawable.Drawable

/** Paleta, medidas y tamaños de letra de toda la app (múltiplos de 8 dp). */
object Tema {
    // ---------- colores: marca v2 (PWA-Checa-Multimedia/marca/tokens.json). El color solo decide. ----------
    val ASFALTO = Color.rgb(12, 12, 16)      // #0C0C10 fondo
    val TABLERO = Color.rgb(22, 22, 28)      // #16161C tarjetas
    val HUMO = Color.rgb(58, 58, 69)         // #3A3A45 bordes
    val HUESO = Color.rgb(244, 244, 239)     // #F4F4EF marca, texto y botones
    val FONDO = ASFALTO
    val TARJETA = TABLERO
    val TARJETA2 = Color.rgb(34, 34, 42)     // #22222A controles dentro de tarjetas
    val CAMPO = Color.rgb(30, 30, 37)        // #1E1E25 campos (nombre + dato) dentro de tarjetas
    val BORDE = Color.rgb(42, 42, 51)        // #2A2A33
    val TEXTO = HUESO
    val TEXTO2 = Color.rgb(207, 207, 212)    // #CFCFD4
    val TENUE = Color.rgb(163, 163, 173)     // #A3A3AD gris del texto
    val VERDE = Color.rgb(34, 197, 94)       // #22C55E aceptar
    val VERDE_SUAVE = Color.rgb(14, 42, 26)
    val SOBRE_VERDE = Color.rgb(4, 33, 15)   // #04210F texto o ✓ sobre verde
    val AMARILLO = Color.rgb(250, 204, 21)   // #FACC15 considerar
    val AMARILLO_SUAVE = Color.rgb(46, 39, 8)
    val ROJO = Color.rgb(239, 68, 68)        // #EF4444 rechazar
    val ROJO_SUAVE = Color.rgb(46, 18, 20)
    /** El fundador ya no usa dorado (la marca v2 solo colorea lo que decide): va en hueso. */
    val DORADO = HUESO
    val DIDI = Color.rgb(255, 112, 40)

    fun colorNivel(n: Int) = when (n) {
        Oferta.VERDE -> VERDE
        Oferta.AMARILLO -> AMARILLO
        else -> ROJO
    }

    fun suaveNivel(n: Int) = when (n) {
        Oferta.VERDE -> VERDE_SUAVE
        Oferta.AMARILLO -> AMARILLO_SUAVE
        else -> ROJO_SUAVE
    }

    // ---------- espaciado (dp) ----------
    const val S1 = 8
    const val S2 = 16
    const val S3 = 24
    const val S4 = 32
    const val ENTRE_TARJETAS = 12
    const val R_TARJETA = 24
    const val R_BOTON = 16
    const val ALTO_TOQUE = 48
    const val ICONO = 24

    // ---------- letra (sp) ----------
    const val T_TITULO = 22f
    const val T_SUB = 16f
    const val T_TEXTO = 14f
    const val T_NOTA = 12f
    const val T_NUMERO = 28f
}

/**
 * Letras de la marca v2 (assets/fuentes): Bricolage Grotesque 800 para títulos y números grandes,
 * DM Sans para el texto y JetBrains Mono para datos y etiquetas. El pop-up también las usa desde v65.
 */
object Fuentes {
    var titulo: Typeface = Typeface.DEFAULT_BOLD; private set
    var texto: Typeface = Typeface.DEFAULT; private set
    var media: Typeface = Typeface.DEFAULT; private set
    var negrita: Typeface = Typeface.DEFAULT_BOLD; private set
    /** Datos y etiquetas (ancho fijo: los números no se mueven al cambiar). */
    var numero: Typeface = Typeface.MONOSPACE; private set

    @Volatile private var listas = false

    /** Carga las fuentes una sola vez (la app o el pop-up, lo que llegue primero). */
    fun iniciar(c: Context) {
        if (listas) return
        listas = true
        val a = c.assets
        fun variable(archivo: String, peso: Int): Typeface? = try {
            Typeface.Builder(a, "fuentes/$archivo").setFontVariationSettings("'wght' $peso").build()
        } catch (_: Exception) { null }
        try { titulo = Typeface.createFromAsset(a, "fuentes/Bricolage-800.ttf") } catch (_: Exception) {}
        variable("DMSans.ttf", 400)?.let { texto = it }
        variable("DMSans.ttf", 500)?.let { media = it }
        variable("DMSans.ttf", 700)?.let { negrita = it }
        variable("JetBrainsMono.ttf", 700)?.let { numero = it }
    }
}

/** Íconos lineales dibujados a mano en una cuadrícula de 24×24, todos del mismo grosor. */
class Icono(private val tipo: Int, color: Int) : Drawable() {
    private val trazo = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 2f
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
    }
    private val relleno = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        strokeJoin = Paint.Join.ROUND
    }
    private val p = Path()

    var color: Int = color
        set(v) { field = v; trazo.color = v; relleno.color = v; invalidateSelf() }

    init { this.color = color }

    override fun draw(c: Canvas) {
        val b = bounds
        val s = minOf(b.width(), b.height()) / 24f
        if (s <= 0f) return
        c.save()
        c.translate(b.left + (b.width() - 24f * s) / 2f, b.top + (b.height() - 24f * s) / 2f)
        c.scale(s, s)
        p.reset()
        when (tipo) {
            INICIO -> {
                p.moveTo(3f, 11f); p.lineTo(12f, 3.5f); p.lineTo(21f, 11f)
                p.moveTo(5.5f, 9.5f); p.lineTo(5.5f, 20f); p.lineTo(18.5f, 20f); p.lineTo(18.5f, 9.5f)
                p.moveTo(10f, 20f); p.lineTo(10f, 14.5f); p.lineTo(14f, 14.5f); p.lineTo(14f, 20f)
                c.drawPath(p, trazo)
            }
            AJUSTES -> {
                c.drawCircle(12f, 12f, 6.5f, trazo)
                c.drawCircle(12f, 12f, 2.5f, trazo)
                val diente = Paint(trazo).apply { strokeWidth = 3f }
                for (i in 0 until 8) {
                    val a = Math.toRadians(i * 45.0)
                    val cx = Math.cos(a).toFloat()
                    val sy = Math.sin(a).toFloat()
                    c.drawLine(12f + cx * 7.5f, 12f + sy * 7.5f, 12f + cx * 9.5f, 12f + sy * 9.5f, diente)
                }
            }
            RELOJ -> {
                c.drawCircle(12f, 12f, 9f, trazo)
                p.moveTo(12f, 7f); p.lineTo(12f, 12f); p.lineTo(15.5f, 14f)
                c.drawPath(p, trazo)
            }
            TENDENCIA -> {
                p.moveTo(3f, 17f); p.lineTo(9f, 11f); p.lineTo(13f, 15f); p.lineTo(21f, 7f)
                p.moveTo(15f, 7f); p.lineTo(21f, 7f); p.lineTo(21f, 13f)
                c.drawPath(p, trazo)
            }
            PIN -> {
                p.arcTo(RectF(5f, 2.5f, 19f, 16.5f), 150f, 240f, true)
                p.lineTo(12f, 21.5f)
                p.close()
                c.drawPath(p, trazo)
                c.drawCircle(12f, 9.5f, 2.5f, trazo)
            }
            DURACION -> {
                p.moveTo(6f, 3f); p.lineTo(18f, 3f)
                p.moveTo(6f, 21f); p.lineTo(18f, 21f)
                p.moveTo(7.5f, 3f); p.lineTo(7.5f, 6.5f); p.lineTo(12f, 12f); p.lineTo(7.5f, 17.5f); p.lineTo(7.5f, 21f)
                p.moveTo(16.5f, 3f); p.lineTo(16.5f, 6.5f); p.lineTo(12f, 12f); p.lineTo(16.5f, 17.5f); p.lineTo(16.5f, 21f)
                c.drawPath(p, trazo)
            }
            FILTROS -> {
                c.drawLine(4f, 7f, 20f, 7f, trazo)
                c.drawLine(4f, 12f, 20f, 12f, trazo)
                c.drawLine(4f, 17f, 20f, 17f, trazo)
                c.drawCircle(15f, 7f, 2.4f, relleno)
                c.drawCircle(8f, 12f, 2.4f, relleno)
                c.drawCircle(13f, 17f, 2.4f, relleno)
            }
            PLAY -> {
                p.moveTo(8f, 5f); p.lineTo(19f, 12f); p.lineTo(8f, 19f); p.close()
                c.drawPath(p, relleno)
                c.drawPath(p, trazo)
            }
            STOP -> c.drawRoundRect(RectF(6.5f, 6.5f, 17.5f, 17.5f), 2f, 2f, relleno)
            CERRAR -> {
                c.drawLine(6f, 6f, 18f, 18f, trazo)
                c.drawLine(18f, 6f, 6f, 18f, trazo)
            }
            FLECHA -> {
                p.moveTo(6f, 9f); p.lineTo(12f, 15f); p.lineTo(18f, 9f)
                c.drawPath(p, trazo)
            }
            ALERTA -> {
                p.moveTo(12f, 3.5f); p.lineTo(21.5f, 20f); p.lineTo(2.5f, 20f); p.close()
                c.drawPath(p, trazo)
                c.drawLine(12f, 9.5f, 12f, 14f, trazo)
                c.drawCircle(12f, 17f, 1.2f, relleno)
            }
            OK -> {
                c.drawCircle(12f, 12f, 9f, trazo)
                p.moveTo(8f, 12.5f); p.lineTo(11f, 15.5f); p.lineTo(16.5f, 9.5f)
                c.drawPath(p, trazo)
            }
            CUENTA -> {
                c.drawCircle(12f, 8f, 4f, trazo)
                p.moveTo(4f, 20.5f)
                p.cubicTo(4.5f, 15.5f, 8f, 13.5f, 12f, 13.5f)
                p.cubicTo(16f, 13.5f, 19.5f, 15.5f, 20f, 20.5f)
                c.drawPath(p, trazo)
            }
            COMPARTIR -> {
                c.drawCircle(18f, 5.5f, 2.8f, trazo)
                c.drawCircle(6f, 12f, 2.8f, trazo)
                c.drawCircle(18f, 18.5f, 2.8f, trazo)
                c.drawLine(8.5f, 10.7f, 15.5f, 6.8f, trazo)
                c.drawLine(8.5f, 13.3f, 15.5f, 17.2f, trazo)
            }
            DESCARGA -> {
                p.moveTo(12f, 3.5f); p.lineTo(12f, 15f)
                p.moveTo(7f, 10.5f); p.lineTo(12f, 15.5f); p.lineTo(17f, 10.5f)
                p.moveTo(4f, 20f); p.lineTo(20f, 20f)
                c.drawPath(p, trazo)
            }
            CELULAR -> {
                c.drawRoundRect(android.graphics.RectF(6.5f, 2.5f, 17.5f, 21.5f), 2.5f, 2.5f, trazo)
                c.drawLine(10.5f, 18f, 13.5f, 18f, trazo)
            }
        }
        c.restore()
    }

    override fun setAlpha(alpha: Int) { trazo.alpha = alpha; relleno.alpha = alpha; invalidateSelf() }

    override fun setColorFilter(cf: ColorFilter?) { trazo.colorFilter = cf; relleno.colorFilter = cf; invalidateSelf() }

    @Deprecated("Deprecated in Java")
    override fun getOpacity(): Int = PixelFormat.TRANSLUCENT

    companion object {
        const val INICIO = 1
        const val AJUSTES = 2
        const val RELOJ = 3
        const val TENDENCIA = 4
        const val PIN = 5
        const val DURACION = 6
        const val FILTROS = 7
        const val PLAY = 8
        const val STOP = 9
        const val CERRAR = 10
        const val FLECHA = 11
        const val ALERTA = 12
        const val OK = 13
        const val CUENTA = 14
        const val COMPARTIR = 15
        const val DESCARGA = 16
        const val CELULAR = 17
    }
}
