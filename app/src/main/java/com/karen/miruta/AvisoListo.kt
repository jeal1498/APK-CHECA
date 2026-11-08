package com.karen.miruta

import android.content.Context
import android.graphics.PixelFormat
import android.graphics.Rect
import android.graphics.drawable.GradientDrawable
import android.os.Handler
import android.os.Looper
import android.util.DisplayMetrics
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.view.animation.LinearInterpolator
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView

/**
 * Aviso «Lista para trabajar» (10.40): sale al abrir Uber o DiDi cuando Checa enciende sola la
 * lectura. Dice que ya lee las ofertas y sugiere apagarla al terminar la jornada. Se cierra solo
 * en 8 s; no bloquea los toques fuera de la tarjeta (una oferta que llegue sigue tocable).
 * Paleta oscura fija de la marca, igual que el pop-up de ofertas.
 */
class AvisoListo(private val ctx: Context, private val tipoVentana: Int) {
    private val wm = ctx.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    private val handler = Handler(Looper.getMainLooper())
    private var vista: View? = null
    private val cerrar = Runnable { ocultar() }

    init { Fuentes.iniciar(ctx) }

    /**
     * Muestra el aviso de [app] sobre su [zona] (la ventana de la app en pantalla dividida;
     * null = pantalla completa). [alApagar] se llama si el conductor toca «Apagar ahora».
     */
    fun mostrar(app: String, zona: Rect?, alApagar: () -> Unit) {
        ocultar()
        val real = DisplayMetrics()
        @Suppress("DEPRECATION")
        wm.defaultDisplay.getRealMetrics(real)
        val area = zona ?: Rect(0, 0, real.widthPixels, real.heightPixels)
        val ancho = minOf((area.width() * 0.94f).toInt(), dp(360))
        val v = construir(app, alApagar)
        val lp = WindowManager.LayoutParams(
            ancho,
            WindowManager.LayoutParams.WRAP_CONTENT,
            tipoVentana,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        )
        lp.gravity = Gravity.TOP or Gravity.LEFT
        lp.x = area.left + (area.width() - ancho) / 2
        lp.y = area.top + dp(56)
        try {
            wm.addView(v, lp)
            vista = v
            v.alpha = 0f
            v.translationY = -dp(12).toFloat()
            v.animate().alpha(1f).translationY(0f).setDuration(220).start()
            // Barra de tiempo: se vacía en SEGUNDOS s.
            v.findViewWithTag<View>(TAG_BARRA)?.let { b ->
                b.pivotX = 0f
                b.animate().scaleX(0f).setDuration(SEGUNDOS * 1000L)
                    .setInterpolator(LinearInterpolator()).start()
            }
            handler.postDelayed(cerrar, SEGUNDOS * 1000L)
        } catch (_: Exception) {
            vista = null
        }
    }

    fun ocultar() {
        handler.removeCallbacks(cerrar)
        vista?.let { try { wm.removeView(it) } catch (_: Exception) { } }
        vista = null
    }

    private fun construir(app: String, alApagar: () -> Unit): View {
        val caja = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(14), dp(14), dp(14), dp(12))
            background = GradientDrawable().apply {
                setColor(Oscuro.TABLERO)
                cornerRadius = dp(16).toFloat()
                setStroke(dp(2), Oscuro.VERDE)
            }
            elevation = dp(8).toFloat()
        }

        // Encabezado: ✓ verde + título.
        val cab = LinearLayout(ctx).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        val ok = TextView(ctx).apply {
            text = "✓"
            gravity = Gravity.CENTER
            setTextColor(Oscuro.SOBRE_VERDE)
            typeface = Fuentes.titulo
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 15f)
            background = GradientDrawable().apply { shape = GradientDrawable.OVAL; setColor(Oscuro.VERDE) }
        }
        cab.addView(ok, LinearLayout.LayoutParams(dp(26), dp(26)))
        val titulo = TextView(ctx).apply {
            text = "Lista para trabajar"
            setTextColor(Oscuro.HUESO)
            typeface = Fuentes.titulo
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 19f)
        }
        cab.addView(titulo, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply { leftMargin = dp(10) })
        caja.addView(cab)

        caja.addView(texto("Checa leerá tus ofertas de $app.", Oscuro.TEXTO2), margenArriba(10))
        caja.addView(texto("🔋 Cuando termines tu jornada, apágala para cuidar la batería.", Oscuro.TEXTO2), margenArriba(8))

        // Botones.
        val fila = LinearLayout(ctx).apply { orientation = LinearLayout.HORIZONTAL }
        val apagar = boton("Apagar ahora", false) { ocultar(); alApagar() }
        val entendido = boton("Entendido", true) { ocultar() }
        fila.addView(apagar, LinearLayout.LayoutParams(0, dp(44), 1f).apply { rightMargin = dp(4) })
        fila.addView(entendido, LinearLayout.LayoutParams(0, dp(44), 1f).apply { leftMargin = dp(4) })
        caja.addView(fila, margenArriba(12))

        // Barra de tiempo.
        val pista = FrameLayout(ctx).apply {
            background = GradientDrawable().apply { setColor(Oscuro.BORDE); cornerRadius = dp(2).toFloat() }
        }
        val barra = View(ctx).apply {
            tag = TAG_BARRA
            background = GradientDrawable().apply { setColor(Oscuro.VERDE); cornerRadius = dp(2).toFloat() }
        }
        pista.addView(barra, FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT))
        caja.addView(pista, margenArriba(10).apply { height = dp(3) })
        return caja
    }

    private fun texto(t: String, color: Int) = TextView(ctx).apply {
        text = t
        setTextColor(color)
        typeface = Fuentes.texto
        setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f)
        setLineSpacing(0f, 1.1f)
    }

    private fun boton(t: String, lleno: Boolean, clic: () -> Unit) = TextView(ctx).apply {
        text = t
        gravity = Gravity.CENTER
        typeface = Fuentes.negrita
        setTextSize(TypedValue.COMPLEX_UNIT_SP, 15f)
        setTextColor(if (lleno) Oscuro.SOBRE_VERDE else Oscuro.HUESO)
        background = GradientDrawable().apply {
            cornerRadius = dp(12).toFloat()
            if (lleno) setColor(Oscuro.VERDE) else { setColor(Oscuro.TABLERO); setStroke(dp(1), Oscuro.HUMO) }
        }
        isClickable = true
        setOnClickListener { clic() }
    }

    private fun margenArriba(valor: Int) = LinearLayout.LayoutParams(
        LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT
    ).apply { topMargin = dp(valor) }

    private fun dp(v: Int): Int = (v * ctx.resources.displayMetrics.density).toInt()

    companion object {
        private const val SEGUNDOS = 8
        private const val TAG_BARRA = "barraListo"
    }
}
