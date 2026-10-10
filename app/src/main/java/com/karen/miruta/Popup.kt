package com.karen.miruta

import android.content.Context
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Handler
import android.os.Looper
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.widget.LinearLayout
import android.widget.TextView
import java.util.Locale

class Popup(private val ctx: Context, private val prefs: Prefs, private val tipoVentana: Int) {
    private val wm = ctx.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    private val handler = Handler(Looper.getMainLooper())
    private var vista: View? = null
    private var ultima: Oferta? = null
    private val cerrar = Runnable { ocultar() }

    fun mostrar(o: Oferta) {
        ultima = o
        quitarVista()
        val v = construir(o, true)
        val ancho = minOf((ctx.resources.displayMetrics.widthPixels * 0.92f).toInt(), dp(440))
        val lp = WindowManager.LayoutParams(
            ancho,
            WindowManager.LayoutParams.WRAP_CONTENT,
            tipoVentana,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        )
        lp.gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
        lp.y = ctx.resources.displayMetrics.heightPixels * prefs.posicion / 100
        try {
            wm.addView(v, lp)
            vista = v
        } catch (e: Exception) {
            vista = null
        }
        handler.removeCallbacks(cerrar)
        handler.postDelayed(cerrar, prefs.duracionSeg * 1000L)
    }

    fun ocultar() {
        handler.removeCallbacks(cerrar)
        quitarVista()
    }

    private fun quitarVista() {
        vista?.let { try { wm.removeView(it) } catch (_: Exception) { } }
        vista = null
    }

    /** También se usa para la vista previa en Ajustes (sin acción en la ✕). */
    fun construir(o: Oferta, interactivo: Boolean): View {
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

        fun txt(s: String, factor: Float, color: Int, negrita: Boolean) = TextView(ctx).apply {
            text = s
            setTextColor(color)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, base * factor)
            if (negrita) setTypeface(Typeface.DEFAULT_BOLD)
            includeFontPadding = false
        }

        val caja = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(12), dp(10), dp(14))
            background = GradientDrawable().apply {
                setColor(FONDO)
                cornerRadius = dp(22).toFloat()
                setStroke(dp(3), acento)
            }
            elevation = dp(10).toFloat()
        }

        // --- Encabezado: estado · tarifa · ✕ ---
        val cabecera = LinearLayout(ctx).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        val insignia = txt("● $estado", 0.72f, if (nivel == Oferta.AMARILLO) Color.BLACK else Color.WHITE, true).apply {
            setPadding(dp(10), dp(5), dp(10), dp(5))
            background = GradientDrawable().apply {
                setColor(if (nivel == Oferta.VERDE) VERDE_OSCURO else acento)
                cornerRadius = dp(50).toFloat()
            }
        }
        cabecera.addView(insignia)
        cabecera.addView(View(ctx), LinearLayout.LayoutParams(0, 1, 1f))
        cabecera.addView(txt("$" + fmt(o.tarifa, 2), 1.15f, Color.WHITE, true))
        val cerrarBtn = txt("✕", 0.95f, TENUE, true).apply {
            gravity = Gravity.CENTER
            setPadding(dp(12), dp(4), dp(6), dp(4))
            if (interactivo) setOnClickListener { ocultar() }
        }
        cabecera.addView(cerrarBtn)
        caja.addView(cabecera)

        // --- Métricas: cada una en verde o rojo según su mínimo ---
        val metricas = LinearLayout(ctx).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(0, dp(12), dp(6), dp(10))
        }
        fun metrica(valor: String, etiqueta: String, ok: Boolean): View =
            LinearLayout(ctx).apply {
                orientation = LinearLayout.VERTICAL
                gravity = Gravity.CENTER_HORIZONTAL
                addView(txt(valor, 1.7f, if (ok) VERDE else ROJO, true))
                addView(txt(etiqueta, 0.62f, TENUE, false).apply { setPadding(0, dp(4), 0, 0) })
            }
        metricas.addView(
            metrica("$" + fmt(o.porKm, 2), "por km · mín $" + fmtMin(prefs.minPorKm), o.porKm >= prefs.minPorKm),
            LinearLayout.LayoutParams(0, -2, 1f)
        )
        metricas.addView(View(ctx).apply { setBackgroundColor(LINEA) }, LinearLayout.LayoutParams(dp(1), -1))
        metricas.addView(
            metrica("$" + fmt(o.porHora, 0), "por hora · mín $" + fmtMin(prefs.minPorHora), o.porHora >= prefs.minPorHora),
            LinearLayout.LayoutParams(0, -2, 1f)
        )
        caja.addView(metricas)

        // --- Pie ---
        caja.addView(View(ctx).apply { setBackgroundColor(LINEA) },
            LinearLayout.LayoutParams(-1, dp(1)).apply { rightMargin = dp(6) })
        caja.addView(txt(
            fmt(o.min, 0) + " min  ·  " + fmt(o.km, 1) + " km  ·  total del viaje",
            0.65f, TENUE, false
        ).apply {
            gravity = Gravity.CENTER
            setPadding(0, dp(10), dp(6), 0)
        }, LinearLayout.LayoutParams(-1, -2))

        return caja
    }

    private fun fmtMin(v: Float): String =
        if (v % 1f == 0f) v.toInt().toString() else String.format(Locale.US, "%.1f", v)

    private fun fmt(v: Double, dec: Int): String = String.format(Locale.US, "%,.${dec}f", v)

    private fun dp(v: Int): Int = (v * ctx.resources.displayMetrics.density).toInt()

    companion object {
        private val FONDO = Color.rgb(20, 23, 28)
        private val VERDE = Color.rgb(34, 197, 94)
        private val VERDE_OSCURO = Color.rgb(22, 128, 61)
        private val AMARILLO = Color.rgb(240, 180, 0)
        private val ROJO = Color.rgb(239, 68, 68)
        private val TENUE = Color.rgb(154, 163, 175)
        private val LINEA = Color.rgb(45, 50, 58)
    }
}
