package com.karen.miruta

import android.content.Context
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PixelFormat
import android.graphics.Rect
import android.util.DisplayMetrics
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Handler
import android.os.Looper
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.text.SpannableString
import android.text.Spanned
import android.text.style.ForegroundColorSpan
import android.text.style.RelativeSizeSpan
import android.view.animation.DecelerateInterpolator
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import java.util.Locale

class Popup(private val ctx: Context, private val prefs: Prefs, private val tipoVentana: Int) {
    private val wm = ctx.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    private val handler = Handler(Looper.getMainLooper())
    private var vista: View? = null
    private var ultima: Oferta? = null
    private var temporizador = false
    private val cerrar = Runnable { temporizador = false; ocultar() }

    /** Se llama cuando el pop-up se cierra (✕ o tiempo agotado). */
    var alCerrar: (() -> Unit)? = null

    /**
     * Muestra la oferta. [zona] es la parte de la pantalla de la app (pantalla dividida);
     * el pop-up se centra sobre ella. [reiniciar] = false conserva el tiempo restante.
     */
    fun mostrar(
        o: Oferta, app: String? = null, zona: Rect? = null,
        marca: Int = SIN_MARCA, reiniciar: Boolean = true
    ) {
        ultima = o
        val nuevo = vista == null
        quitarVista()
        val v = construir(o, true, app, marca)

        val real = DisplayMetrics()
        @Suppress("DEPRECATION")
        wm.defaultDisplay.getRealMetrics(real)
        val area = zona ?: Rect(0, 0, real.widthPixels, real.heightPixels)
        val ancho = minOf((area.width() * 0.94f).toInt(), dp(360))
        val lp = WindowManager.LayoutParams(
            ancho,
            WindowManager.LayoutParams.WRAP_CONTENT,
            tipoVentana,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        )
        lp.gravity = Gravity.TOP or Gravity.LEFT
        lp.x = area.left + (area.width() - ancho) / 2
        lp.y = area.top + area.height() * prefs.posicion / 100
        try {
            wm.addView(v, lp)
            vista = v
            if (nuevo) {
                val destino = v.alpha
                v.alpha = 0f
                v.scaleX = 0.94f
                v.scaleY = 0.94f
                v.translationY = -dp(8).toFloat()
                v.animate().alpha(destino).scaleX(1f).scaleY(1f).translationY(0f)
                    .setDuration(180).setInterpolator(DecelerateInterpolator()).start()
            }
        } catch (e: Exception) {
            vista = null
        }
        if (reiniciar || !temporizador) {
            handler.removeCallbacks(cerrar)
            handler.postDelayed(cerrar, prefs.duracionSeg * 1000L)
            temporizador = true
        }
    }

    fun ocultar() {
        handler.removeCallbacks(cerrar)
        temporizador = false
        val estaba = vista != null
        quitarVista()
        if (estaba) alCerrar?.invoke()
    }

    private fun quitarVista() {
        vista?.let { try { wm.removeView(it) } catch (_: Exception) { } }
        vista = null
    }

    /** También se usa para la vista previa en Ajustes (sin acción en la ✕). */
    fun construir(o: Oferta, interactivo: Boolean, app: String? = null, marca: Int = SIN_MARCA): View {
        val base = prefs.tamanoTexto.toFloat()
        val nivel = o.nivel(prefs)
        val acento = Tema.colorNivel(nivel)
        val tinta = when (nivel) {
            Oferta.VERDE -> TINTA_VERDE
            Oferta.AMARILLO -> TINTA_AMARILLO
            else -> TINTA_ROJO
        }
        val accion = when (nivel) {
            Oferta.VERDE -> "ACEPTAR"
            Oferta.AMARILLO -> "CONSIDERAR"
            else -> "RECHAZAR"
        }

        // Ojo: ningún texto del pop-up puede ser "solo un monto", para que el lector
        // no lo confunda con la tarifa de una oferta cuando queda encima de la app.
        fun txt(s: CharSequence, factor: Float, color: Int, negrita: Boolean) = TextView(ctx).apply {
            text = s
            setTextColor(color)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, base * factor)
            if (negrita) setTypeface(Typeface.DEFAULT_BOLD)
            includeFontPadding = false
        }

        fun pastilla(s: String, fondo: Int, color: Int) = txt(s, 0.6f, color, true).apply {
            gravity = Gravity.CENTER
            setPadding(dp(8), dp(3), dp(8), dp(3))
            background = GradientDrawable().apply {
                cornerRadius = dp(50).toFloat()
                setColor(fondo)
            }
        }

        val marco = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            background = GradientDrawable().apply {
                setColor(FONDO)
                cornerRadius = dp(Tema.R_TARJETA).toFloat()
                setStroke(dp(if (marca == MEJOR) 2 else 1), if (marca == MEJOR) acento else Tema.BORDE)
            }
            clipToOutline = true
            elevation = dp(8).toFloat()
        }
        if (marca == SEGUNDA) marco.alpha = 0.72f

        // Franja del semáforo: [app · marca]  ACCIÓN  [⚡ · ✕]. Los lados pesan igual
        // para que la acción quede centrada.
        val franja = LinearLayout(ctx).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setBackgroundColor(acento)
            setPadding(dp(Tema.S1), dp(4), dp(4), dp(4))
        }
        val izq = LinearLayout(ctx).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL or Gravity.START
        }
        if (!app.isNullOrEmpty()) {
            izq.addView(pastilla(app.uppercase(), if (app == "DiDi") Tema.DIDI else Tema.TARJETA2, Color.WHITE))
        }
        if (marca != SIN_MARCA) {
            val esMejor = marca == MEJOR
            izq.addView(pastilla(if (esMejor) "⭐ MEJOR" else "2ª",
                if (esMejor) Color.rgb(255, 214, 10) else Tema.TARJETA2,
                if (esMejor) Color.BLACK else Tema.TEXTO2),
                LinearLayout.LayoutParams(-2, -2).apply { leftMargin = dp(4) })
        }
        franja.addView(izq, LinearLayout.LayoutParams(0, -2, 1f))
        franja.addView(txt(accion, 0.8f, tinta, true).apply {
            letterSpacing = 0.1f
            gravity = Gravity.CENTER
            setPadding(dp(Tema.S1), 0, dp(Tema.S1), 0)
        })
        val der = LinearLayout(ctx).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL or Gravity.END
        }
        if (o.multiplicador > 1.0) {
            der.addView(pastilla("⚡" + fmtX(o.multiplicador) + "x", tinta, Tema.AMARILLO))
        }
        val cerrarBtn = ImageView(ctx).apply {
            setImageDrawable(Icono(Icono.CERRAR, tinta))
            setPadding(dp(6), dp(6), dp(6), dp(6))
            if (interactivo) setOnClickListener { ocultar() }
        }
        der.addView(cerrarBtn, LinearLayout.LayoutParams(dp(32), dp(32)).apply { leftMargin = dp(4) })
        franja.addView(der, LinearLayout.LayoutParams(0, -2, 1f))
        marco.addView(franja, LinearLayout.LayoutParams(-1, -2))

        val col = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(12), dp(12), dp(12), dp(12))
        }
        marco.addView(col, LinearLayout.LayoutParams(-1, -2))

        // Neta al centro y, abajo, viaje y gasolina
        val conNeta = prefs.mostrarNeta && prefs.kmPorLitro > 0
        val valor = "$" + fmt(if (conNeta) o.neta(prefs) else o.tarifa, 2)
        val etiqueta = if (conNeta) "netos" else "tarifa"
        val grande = SpannableString("$valor  $etiqueta")
        grande.setSpan(RelativeSizeSpan(0.4f), valor.length, grande.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        grande.setSpan(ForegroundColorSpan(Tema.TEXTO2), valor.length, grande.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        col.addView(txt(grande, 2.0f, Tema.TEXTO, true).apply { gravity = Gravity.CENTER },
            LinearLayout.LayoutParams(-1, -2))
        if (conNeta) {
            col.addView(txt(
                "Viaje $" + fmt(o.tarifa, 2) + "  ·  Gasolina −$" + fmt(o.gasolina(prefs), 2),
                0.7f, Tema.TEXTO2, false
            ).apply {
                gravity = Gravity.CENTER
                setPadding(0, dp(6), 0, 0)
            }, LinearLayout.LayoutParams(-1, -2))
        }

        // Fichas: $/h · $/km · tiempo (o el filtro de recogida/duración que falló).
        // Verde si cumple la meta, rojo con borde si no; blanco si el filtro está apagado.
        val fichas = LinearLayout(ctx).apply { orientation = LinearLayout.HORIZONTAL }
        fun ficha(v: String, etq: String, cumple: Boolean?) = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(dp(4), dp(Tema.S1), dp(4), dp(Tema.S1))
            background = GradientDrawable().apply {
                cornerRadius = dp(10).toFloat()
                if (cumple == false) {
                    setColor(Tema.ROJO_SUAVE)
                    setStroke(dp(1), Tema.ROJO)
                } else setColor(Tema.TARJETA)
            }
            val color = when (cumple) { true -> Tema.VERDE; false -> Tema.ROJO; null -> Tema.TEXTO }
            addView(txt(v, 0.9f, color, true).apply { maxLines = 1; gravity = Gravity.CENTER; encoger(this) },
                LinearLayout.LayoutParams(-1, -2))
            addView(txt(etq, 0.6f, Tema.TENUE, false).apply {
                maxLines = 1
                gravity = Gravity.CENTER
                setPadding(0, dp(4), 0, 0)
            }, LinearLayout.LayoutParams(-1, -2))
        }
        val recFalla = prefs.filtroRecMax && o.recogidaKm > 0 && o.recogidaKm > prefs.recogidaMaxKm
        val durFalla = prefs.filtroDurMax && o.viajeMin > prefs.duracionMax
        val lista = listOf(
            ficha("$" + fmt(o.porHora, 0) + "/h",
                if (prefs.filtroHora) "meta $" + fmt(prefs.minPorHora.toDouble(), 0) else "por hora",
                if (prefs.filtroHora) o.porHora >= prefs.minPorHora else null),
            ficha("$" + fmt(o.porKm, 2) + "/km",
                if (prefs.filtroKm) "meta $" + fmtMin(prefs.minPorKm) else "por km",
                if (prefs.filtroKm) o.porKm >= prefs.minPorKm else null),
            when {
                recFalla -> ficha(fmt(o.recogidaKm, 1) + " km", "recogida máx. " + fmtMin(prefs.recogidaMaxKm), false)
                durFalla -> ficha(fmt(o.viajeMin, 0) + " min", "viaje máx. " + prefs.duracionMax, false)
                // La palabra "total" evita que el lector sume estos km
                else -> ficha(fmt(o.min, 0) + " min", fmt(o.km, 1) + " km total", null)
            }
        )
        lista.forEachIndexed { i, f ->
            fichas.addView(f, LinearLayout.LayoutParams(0, -1, 1f).apply { if (i > 0) leftMargin = dp(6) })
        }
        col.addView(fichas, LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(12) })

        return marco
    }

    /** Achica el texto (sin bajar de 10 sp) si no cabe en una línea dentro de su ficha. */
    private fun encoger(t: TextView) {
        t.addOnLayoutChangeListener { _, _, _, _, _, _, _, _, _ ->
            val disp = t.width - t.paddingLeft - t.paddingRight
            if (disp <= 0) return@addOnLayoutChangeListener
            val p = Paint(t.paint)
            val texto = t.text.toString()
            val minimo = 10f * ctx.resources.displayMetrics.scaledDensity
            var tam = p.textSize
            while (tam > minimo && p.measureText(texto) > disp) {
                tam -= 1f
                p.textSize = tam
            }
            if (tam < t.textSize) t.post { t.setTextSize(TypedValue.COMPLEX_UNIT_PX, tam) }
        }
    }

    private fun fmtMin(v: Float): String =
        if (v % 1f == 0f) v.toInt().toString() else String.format(Locale.US, "%.1f", v)

    private fun fmtX(v: Double): String =
        if (v * 10 % 1.0 == 0.0) String.format(Locale.US, "%.1f", v) else String.format(Locale.US, "%.2f", v)

    private fun fmt(v: Double, dec: Int): String = String.format(Locale.US, "%,.${dec}f", v)

    private fun dp(v: Int): Int = (v * ctx.resources.displayMetrics.density).toInt()

    companion object {
        const val SIN_MARCA = 0
        const val MEJOR = 1
        const val SEGUNDA = 2
        private val FONDO = Color.rgb(18, 22, 20)
        // Texto oscuro sobre la franja de color
        private val TINTA_VERDE = Color.rgb(6, 33, 15)
        private val TINTA_AMARILLO = Color.rgb(43, 30, 2)
        private val TINTA_ROJO = Color.rgb(42, 7, 7)
    }
}
