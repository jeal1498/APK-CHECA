package com.karen.miruta

import android.content.Context
import android.graphics.Color
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
import android.widget.FrameLayout
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
        quitarVista()
        val v = construir(o, true, app, marca)

        val real = DisplayMetrics()
        @Suppress("DEPRECATION")
        wm.defaultDisplay.getRealMetrics(real)
        val area = zona ?: Rect(0, 0, real.widthPixels, real.heightPixels)
        val ancho = minOf((area.width() * 0.92f).toInt(), dp(440))
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
        val acento = when (nivel) {
            Oferta.VERDE -> VERDE
            Oferta.AMARILLO -> AMARILLO
            else -> ROJO
        }
        val estado = when (nivel) {
            Oferta.VERDE -> "CONVIENE"
            Oferta.AMARILLO -> "REGULAR"
            else -> "NO CONVIENE"
        }

        fun txt(s: CharSequence, factor: Float, color: Int, negrita: Boolean) = TextView(ctx).apply {
            text = s
            setTextColor(color)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, base * factor)
            if (negrita) setTypeface(Typeface.DEFAULT_BOLD)
            gravity = Gravity.CENTER
        }

        val marco = FrameLayout(ctx).apply {
            background = GradientDrawable().apply {
                setColor(FONDO)
                cornerRadius = dp(24).toFloat()
                setStroke(dp(if (marca == MEJOR) 6 else 4), acento)
            }
            elevation = dp(10).toFloat()
        }

        val col = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            setPadding(dp(20), dp(14), dp(20), dp(14))
        }

        if (marca == SEGUNDA) marco.alpha = 0.72f

        // Línea 0: app y marca (solo si aplica)
        if (app != null && app.isNotEmpty() || marca != SIN_MARCA) {
            val fila0 = LinearLayout(ctx).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER
                setPadding(0, 0, 0, dp(6))
            }
            if (!app.isNullOrEmpty()) {
                fila0.addView(txt(app.uppercase(), 0.6f, Color.WHITE, true).apply {
                    setPadding(dp(10), dp(3), dp(10), dp(3))
                    background = GradientDrawable().apply {
                        cornerRadius = dp(50).toFloat()
                        setColor(if (app == "DiDi") Color.rgb(255, 112, 40) else Color.rgb(60, 66, 76))
                    }
                })
            }
            if (marca != SIN_MARCA) {
                val esMejor = marca == MEJOR
                fila0.addView(txt(if (esMejor) "⭐ MEJOR OPCIÓN" else "2ª opción", 0.6f,
                    if (esMejor) Color.BLACK else TENUE, true).apply {
                    setPadding(dp(10), dp(3), dp(10), dp(3))
                    background = GradientDrawable().apply {
                        cornerRadius = dp(50).toFloat()
                        setColor(if (esMejor) Color.rgb(255, 214, 10) else Color.rgb(45, 50, 58))
                    }
                }, LinearLayout.LayoutParams(-2, -2).apply { leftMargin = dp(8) })
            }
            col.addView(fila0)
        }

        // Línea 1: estado y tarifa
        val l1 = "$estado: $" + fmt(o.tarifa, 2)
        if (o.multiplicador > 1.0) {
            val dinamica = "   ⚡" + fmtX(o.multiplicador) + "x"
            val sp1 = SpannableString(l1 + dinamica)
            sp1.setSpan(ForegroundColorSpan(AMARILLO), l1.length, sp1.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            col.addView(txt(sp1, 1.15f, acento, true))
        } else {
            col.addView(txt(l1, 1.15f, acento, true))
        }

        // Línea 2: $/h | $/km
        val separador = "   |   "
        val linea2 = SpannableString("$" + fmt(o.porHora, 2) + " /h" + separador + "$" + fmt(o.porKm, 2) + " /km")
        val ini = linea2.indexOf(separador)
        linea2.setSpan(ForegroundColorSpan(LINEA), ini, ini + separador.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        col.addView(txt(linea2, 1.45f, acento, true).apply { setPadding(0, dp(8), 0, dp(6)) })

        // Línea 3: total
        col.addView(txt(fmt(o.min, 0) + " min (" + fmt(o.km, 1) + " km) total", 0.8f, TENUE, false))
        if (prefs.mostrarNeta && prefs.kmPorLitro > 0) {
            col.addView(txt(
                "⛽ −$" + fmt(o.gasolina(prefs), 2) + "   ·   Te quedan $" + fmt(o.neta(prefs), 2),
                0.85f, Color.WHITE, true
            ).apply { setPadding(0, dp(4), 0, 0) })
        }
        o.motivoFiltro(prefs)?.let { col.addView(txt("⚠ $it", 0.8f, ROJO, true).apply { setPadding(0, dp(6), 0, 0) }) }

        marco.addView(col, FrameLayout.LayoutParams(-1, -2))

        // ✕ arriba a la derecha
        val cerrarBtn = txt("✕", 0.8f, TENUE, true).apply {
            setPadding(dp(14), dp(10), dp(16), dp(10))
            if (interactivo) setOnClickListener { ocultar() }
        }
        marco.addView(cerrarBtn, FrameLayout.LayoutParams(-2, -2, Gravity.TOP or Gravity.END))

        return marco
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
        private val FONDO = Color.rgb(12, 14, 18)
        private val VERDE = Color.rgb(34, 197, 94)
        private val VERDE_OSCURO = Color.rgb(22, 128, 61)
        private val AMARILLO = Color.rgb(240, 180, 0)
        private val ROJO = Color.rgb(239, 68, 68)
        private val TENUE = Color.rgb(154, 163, 175)
        private val LINEA = Color.rgb(75, 82, 94)
    }
}
