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
        val v = construir(o)
        val lp = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
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

    private fun cambiarTamano(delta: Int) {
        prefs.tamanoTexto = prefs.tamanoTexto + delta
        ultima?.let { mostrar(it) }
    }

    private fun construir(o: Oferta): View {
        val base = prefs.tamanoTexto.toFloat()
        val okKm = o.porKm >= prefs.minPorKm
        val okHora = o.porHora >= prefs.minPorHora

        val (fondo, texto, titulo) = when {
            okKm && okHora -> Triple(Color.rgb(27, 142, 62), Color.WHITE, "CONVIENE")
            okKm || okHora -> Triple(Color.rgb(240, 180, 0), Color.BLACK, "REGULAR")
            else -> Triple(Color.rgb(198, 40, 40), Color.WHITE, "NO CONVIENE")
        }

        val caja = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(18), dp(12), dp(18), dp(10))
            background = GradientDrawable().apply {
                setColor(fondo)
                cornerRadius = dp(18).toFloat()
            }
            elevation = dp(8).toFloat()
        }

        fun linea(s: String, factor: Float, negrita: Boolean): TextView =
            TextView(ctx).apply {
                text = s
                setTextColor(texto)
                setTextSize(TypedValue.COMPLEX_UNIT_SP, base * factor)
                if (negrita) setTypeface(typeface, Typeface.BOLD)
                gravity = Gravity.CENTER_HORIZONTAL
            }

        caja.addView(linea(titulo, 1.0f, true))
        caja.addView(linea("$" + fmt(o.porKm, 2) + " / km", 1.5f, true))
        caja.addView(linea("$" + fmt(o.porHora, 0) + " / hora", 1.5f, true))
        caja.addView(
            linea("$" + fmt(o.tarifa, 2) + "  ·  " + fmt(o.km, 1) + " km  ·  " + fmt(o.min, 0) + " min", 0.8f, false)
        )

        val botones = LinearLayout(ctx).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            setPadding(0, dp(6), 0, 0)
        }
        fun boton(s: String, accion: () -> Unit): TextView =
            linea(s, 0.9f, true).apply {
                setPadding(dp(16), dp(4), dp(16), dp(4))
                setOnClickListener { accion() }
            }
        botones.addView(boton("A−") { cambiarTamano(-2) })
        botones.addView(boton("A+") { cambiarTamano(2) })
        botones.addView(boton("✕") { ocultar() })
        caja.addView(botones)

        return caja
    }

    private fun fmt(v: Double, dec: Int): String = String.format(Locale.US, "%,.${dec}f", v)

    private fun dp(v: Int): Int = (v * ctx.resources.displayMetrics.density).toInt()
}
