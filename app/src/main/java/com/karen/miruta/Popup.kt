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
        val estado = when (nivel) {
            Oferta.VERDE -> "CONVIENE"
            Oferta.AMARILLO -> "REGULAR"
            else -> "NO CONVIENE"
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
            setPadding(dp(8), dp(4), dp(8), dp(4))
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

        // Franja del semáforo
        marco.addView(View(ctx).apply { setBackgroundColor(acento) }, LinearLayout.LayoutParams(-1, dp(4)))

        val col = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(Tema.S2), dp(12), dp(Tema.S2), dp(Tema.S2))
        }
        marco.addView(col, LinearLayout.LayoutParams(-1, -2))

        // Fila 1: estado · app · marca · ✕
        val fila1 = LinearLayout(ctx).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        val sp = SpannableString("●  $estado")
        sp.setSpan(ForegroundColorSpan(acento), 0, 1, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        fila1.addView(pastilla("", Tema.suaveNivel(nivel), acento).apply { text = sp })
        if (!app.isNullOrEmpty()) {
            fila1.addView(pastilla(app.uppercase(), if (app == "DiDi") Tema.DIDI else Tema.TARJETA2, Color.WHITE),
                LinearLayout.LayoutParams(-2, -2).apply { leftMargin = dp(Tema.S1) })
        }
        if (marca != SIN_MARCA) {
            val esMejor = marca == MEJOR
            fila1.addView(pastilla(if (esMejor) "⭐ MEJOR" else "2ª opción",
                if (esMejor) Color.rgb(255, 214, 10) else Tema.TARJETA2,
                if (esMejor) Color.BLACK else Tema.TEXTO2),
                LinearLayout.LayoutParams(-2, -2).apply { leftMargin = dp(Tema.S1) })
        }
        fila1.addView(View(ctx), LinearLayout.LayoutParams(0, 1, 1f))
        val cerrarBtn = ImageView(ctx).apply {
            setImageDrawable(Icono(Icono.CERRAR, Tema.TENUE))
            setPadding(dp(6), dp(6), dp(6), dp(6))
            if (interactivo) setOnClickListener { ocultar() }
        }
        fila1.addView(cerrarBtn, LinearLayout.LayoutParams(dp(32), dp(32)))
        col.addView(fila1)

        // Fila 2: monto grande (neta o tarifa) y multiplicador
        val conNeta = prefs.mostrarNeta && prefs.kmPorLitro > 0
        val fila2 = LinearLayout(ctx).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(0, dp(12), 0, 0)
        }
        val etiqueta = if (conNeta) "Te quedan" else "Tarifa"
        val valor = "$" + fmt(if (conNeta) o.neta(prefs) else o.tarifa, 2)
        val grande = SpannableString("$etiqueta  $valor")
        grande.setSpan(RelativeSizeSpan(0.42f), 0, etiqueta.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        grande.setSpan(ForegroundColorSpan(Tema.TEXTO2), 0, etiqueta.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        fila2.addView(txt(grande, 1.9f, Tema.TEXTO, true), LinearLayout.LayoutParams(0, -2, 1f))
        if (o.multiplicador > 1.0) {
            fila2.addView(pastilla("⚡" + fmtX(o.multiplicador) + "x", Tema.AMARILLO_SUAVE, Tema.AMARILLO))
        }
        col.addView(fila2)
        if (conNeta) {
            col.addView(txt(
                "Tarifa $" + fmt(o.tarifa, 2) + "  ·  Gasolina −$" + fmt(o.gasolina(prefs), 2),
                0.7f, Tema.TENUE, false
            ).apply { setPadding(0, dp(6), 0, 0) })
        }

        // Fila 3: $/h y $/km
        val metricas = LinearLayout(ctx).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(0, dp(12), 0, dp(12))
            background = GradientDrawable().apply {
                cornerRadius = dp(Tema.R_BOTON).toFloat()
                setColor(Tema.TARJETA)
            }
        }
        fun metrica(v: String, etq: String) = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            addView(txt(v, 1.25f, acento, true).apply { gravity = Gravity.CENTER })
            addView(txt(etq, 0.6f, Tema.TENUE, false).apply {
                gravity = Gravity.CENTER
                setPadding(0, dp(4), 0, 0)
            })
        }
        metricas.addView(metrica("$" + fmt(o.porHora, 0) + "/h", "por hora"), LinearLayout.LayoutParams(0, -2, 1f))
        metricas.addView(View(ctx).apply { setBackgroundColor(Tema.BORDE) }, LinearLayout.LayoutParams(dp(1), dp(32)))
        metricas.addView(metrica("$" + fmt(o.porKm, 2) + "/km", "por km"), LinearLayout.LayoutParams(0, -2, 1f))
        col.addView(metricas, LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(12) })

        // Fila 4: total del viaje (la palabra "total" evita que el lector sume estos km)
        col.addView(txt(fmt(o.min, 0) + " min  ·  " + fmt(o.km, 1) + " km total", 0.7f, Tema.TEXTO2, false).apply {
            gravity = Gravity.CENTER
            setPadding(0, dp(12), 0, 0)
        }, LinearLayout.LayoutParams(-1, -2))

        o.motivoFiltro(prefs)?.let { motivo ->
            val aviso = LinearLayout(ctx).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                setPadding(dp(12), dp(8), dp(12), dp(8))
                background = GradientDrawable().apply {
                    cornerRadius = dp(Tema.R_BOTON).toFloat()
                    setColor(Tema.ROJO_SUAVE)
                }
            }
            val lado = (base * 0.75f * ctx.resources.displayMetrics.scaledDensity).toInt()
            aviso.addView(ImageView(ctx).apply { setImageDrawable(Icono(Icono.ALERTA, Tema.ROJO)) },
                LinearLayout.LayoutParams(lado, lado).apply { rightMargin = dp(Tema.S1) })
            aviso.addView(txt(motivo, 0.7f, Tema.ROJO, true), LinearLayout.LayoutParams(0, -2, 1f))
            col.addView(aviso, LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(12) })
        }

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
        private val FONDO = Color.rgb(18, 22, 20)
    }
}
