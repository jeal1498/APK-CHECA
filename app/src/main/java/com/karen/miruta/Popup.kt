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
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.text.SpannableStringBuilder
import android.text.style.AbsoluteSizeSpan
import android.text.style.StyleSpan
import android.text.Spanned
import android.text.style.ForegroundColorSpan
import android.text.style.RelativeSizeSpan
import android.view.animation.DecelerateInterpolator
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import java.util.Locale

class Popup(private val ctx: Context, private val prefs: Prefs, private val tipoVentana: Int) {
    private val wm = ctx.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    private val handler = Handler(Looper.getMainLooper())
    private var vista: View? = null
    private var ultima: Oferta? = null
    private var firmaVista: String? = null
    private var lpActual: WindowManager.LayoutParams? = null
    private var areaActual: Rect? = null
    private var temporizador = false
    private val cerrar = Runnable { temporizador = false; ocultar() }

    init { Fuentes.iniciar(ctx) }

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
        // Misma oferta, misma marca y misma zona: no se vuelve a pintar (evita el parpadeo).
        val firma = "$o|$app|$marca|$zona|${prefs.posicion}|${prefs.escalaPopup}"
        val actual = vista
        if (actual != null && firma == firmaVista) {
            if (reiniciar || !temporizador) {
                handler.removeCallbacks(cerrar)
                handler.postDelayed(cerrar, prefs.duracionSeg * 1000L)
                temporizador = true
            }
            return
        }
        firmaVista = firma
        val v = construir(o, true, app, marca)

        val real = DisplayMetrics()
        @Suppress("DEPRECATION")
        wm.defaultDisplay.getRealMetrics(real)
        val area = zona ?: Rect(0, 0, real.widthPixels, real.heightPixels)
        val ancho = minOf((area.width() * 0.94f).toInt(), anchoPx(ctx, prefs))
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
        lpActual = lp
        areaActual = area
        if (actual is FrameLayout) {
            // Ya hay ventana: se cambia el contenido sin quitarla (sin parpadeo).
            actual.removeAllViews()
            actual.addView(v, ajuste())
            try { wm.updateViewLayout(actual, lp) } catch (_: Exception) { }
            corregirPosicion(actual)
        } else {
            val caja = CajaArrastrable(ctx).apply { addView(v, ajuste()) }
            try {
                wm.addView(caja, lp)
                vista = caja
                corregirPosicion(caja)
                caja.alpha = 0f
                caja.scaleX = 0.94f
                caja.scaleY = 0.94f
                caja.translationY = -dp(8).toFloat()
                caja.animate().alpha(1f).scaleX(1f).scaleY(1f).translationY(0f)
                    .setDuration(180).setInterpolator(DecelerateInterpolator()).start()
            } catch (e: Exception) {
                vista = null
                firmaVista = null
            }
        }
        if (reiniciar || !temporizador) {
            handler.removeCallbacks(cerrar)
            handler.postDelayed(cerrar, prefs.duracionSeg * 1000L)
            temporizador = true
        }
    }

    /**
     * Ventana del pop-up: se arrastra con el dedo hacia arriba o abajo y la posición
     * se guarda (la misma que el deslizador de Ajustes). Los toques en la ✕ siguen igual.
     */
    private inner class CajaArrastrable(c: Context) : FrameLayout(c) {
        private var inicioY = 0f
        private var inicioLpY = 0
        private var arrastrando = false

        override fun onInterceptTouchEvent(e: MotionEvent): Boolean {
            when (e.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    inicioY = e.rawY
                    inicioLpY = lpActual?.y ?: 0
                    arrastrando = false
                }
                MotionEvent.ACTION_MOVE -> if (!arrastrando && Math.abs(e.rawY - inicioY) > dp(10)) {
                    arrastrando = true
                    handler.removeCallbacks(cerrar)
                }
            }
            return arrastrando
        }

        override fun onTouchEvent(e: MotionEvent): Boolean {
            when (e.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    inicioY = e.rawY
                    inicioLpY = lpActual?.y ?: 0
                }
                MotionEvent.ACTION_MOVE -> {
                    val lp = lpActual ?: return true
                    val area = areaActual ?: return true
                    arrastrando = true
                    val alto = height.takeIf { it > 0 } ?: dp(200)
                    val maxY = maxOf(area.top, area.bottom - alto)
                    lp.y = (inicioLpY + (e.rawY - inicioY)).toInt().coerceIn(area.top, maxY)
                    try { wm.updateViewLayout(this, lp) } catch (_: Exception) { }
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    if (arrastrando) guardarPosicion()
                    arrastrando = false
                    handler.removeCallbacks(cerrar)
                    handler.postDelayed(cerrar, prefs.duracionSeg * 1000L)
                    temporizador = true
                }
            }
            return true
        }
    }

    /** Guarda dónde quedó el pop-up como % de la altura de su zona. */
    private fun guardarPosicion() {
        val lp = lpActual ?: return
        val area = areaActual ?: return
        if (area.height() <= 0) return
        val alto = vista?.height?.takeIf { it > 0 } ?: dp(200)
        val maxPct = (((area.height() - alto).coerceAtLeast(0) * 100) / area.height()).coerceIn(0, 80)
        val pct = ((lp.y - area.top) * 100 / area.height()).coerceIn(0, maxPct)
        prefs.posicion = pct
        // La firma termina con la posición: se actualiza para no rehacer el pop-up por moverlo.
        firmaVista = firmaVista?.replaceAfterLast('|', pct.toString())
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
        firmaVista = null
        lpActual = null
    }

    /** Corrige la posición cuando Android ya midió el alto real del pop-up. */
    private fun corregirPosicion(v: View) {
        v.post {
            val lp = lpActual ?: return@post
            val area = areaActual ?: return@post
            if (area.height() <= 0) return@post
            val alto = v.height.takeIf { it > 0 } ?: v.measuredHeight
            if (alto <= 0) return@post
            val maxY = maxOf(area.top, area.bottom - alto)
            val y = lp.y.coerceIn(area.top, maxY)
            if (y != lp.y) {
                lp.y = y
                try { wm.updateViewLayout(v, lp) } catch (_: Exception) { }
            }
        }
    }

    private fun ajuste() = FrameLayout.LayoutParams(
        FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.WRAP_CONTENT
    )

    /** Parte de la pantalla que tapa el pop-up (para que el lector no la lea). */
    fun rect(): Rect? {
        val v = vista ?: return null
        val lp = lpActual ?: return null
        val alto = if (v.height > 0) v.height else dp(200)
        return Rect(lp.x, lp.y, lp.x + lp.width, lp.y + alto)
    }

    /**
     * También se usa para la vista previa en Ajustes (sin acción en la ✕).
     * Pop-up C (10.8): arriba la acción con su punto de color (★/2ª delante) y el ⚡ a la derecha;
     * en medio el monto neto grande y centrado; abajo tres columnas iguales: $/h, $/km y minutos
     * (verde si cumple la meta, rojo si no, hueso si el filtro está apagado).
     * Todo se escala con prefs.escalaPopup (70–160 %).
     */
    fun construir(o: Oferta, interactivo: Boolean, app: String? = null, marca: Int = SIN_MARCA): View {
        val esc = prefs.escalaPopup / 100f
        fun e(v: Float): Int = (v * esc * ctx.resources.displayMetrics.density).toInt()
        fun sp(v: Float) = v * esc
        val nivel = o.nivel(prefs)
        val segunda = marca == SEGUNDA
        val acento = Tema.colorNivel(nivel)
        // 2ª opción (dos ofertas a la vez): el color apagado para que la mejor resalte.
        val color = if (segunda) mezclar(acento, Tema.TABLERO, 0.45f) else acento
        val fondo = mezclar(Tema.ASFALTO, color, 0.14f)
        val linea = Color.argb(77, Color.red(color), Color.green(color), Color.blue(color))
        val accion = when (nivel) {
            Oferta.VERDE -> "ACEPTAR"
            Oferta.AMARILLO -> "CONSIDERAR"
            else -> "RECHAZAR"
        }
        val pre = when {
            marca == MEJOR -> "★ "
            segunda -> "2ª "
            else -> ""
        }
        val mon = prefs.moneda

        val marco = FrameLayout(ctx).apply {
            background = GradientDrawable().apply {
                setColor(fondo)
                cornerRadius = e(14f).toFloat()
                // La mejor de dos lleva el borde más grueso.
                setStroke(e(if (marca == MEJOR) 3.5f else 2f).coerceAtLeast(2), color)
            }
            elevation = dp(8).toFloat()
        }
        val col = LinearLayout(ctx).apply { orientation = LinearLayout.VERTICAL }
        marco.addView(col, FrameLayout.LayoutParams(-1, -2))

        // Arriba: punto de color + acción, y el ⚡ a la derecha (deja lugar a la ✕).
        val cabeza = LinearLayout(ctx).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(e(10f), e(7f), e(28f), 0)
        }
        cabeza.addView(View(ctx).apply {
            background = GradientDrawable().apply { shape = GradientDrawable.OVAL; setColor(color) }
        }, LinearLayout.LayoutParams(e(7f), e(7f)).apply { rightMargin = e(6f) })
        cabeza.addView(TextView(ctx).apply {
            text = pre + accion
            setTextSize(TypedValue.COMPLEX_UNIT_SP, sp(9.5f))
            setTextColor(color)
            typeface = Fuentes.negrita
            letterSpacing = 0.08f
            includeFontPadding = false
            maxLines = 1
        }, LinearLayout.LayoutParams(0, -2, 1f))
        val rayo = when {
            o.multiplicador > 1.0 -> "⚡" + fmtX(o.multiplicador) + "x"
            o.dinamica > 0 -> "⚡+" + mon.monto(o.dinamica, 0)
            else -> null
        }
        if (rayo != null) {
            cabeza.addView(TextView(ctx).apply {
                text = rayo
                setTextSize(TypedValue.COMPLEX_UNIT_SP, sp(9f))
                setTextColor(Tema.AMARILLO)
                typeface = Fuentes.numero
                includeFontPadding = false
                maxLines = 1
            })
        }
        col.addView(cabeza, LinearLayout.LayoutParams(-1, -2))

        // En medio: el monto (neto si así está en Ajustes), grande y centrado.
        val conNeta = prefs.mostrarNeta && (prefs.kmPorLitro > 0 || prefs.desgastePorKm > 0)
        val montoTxt = TextView(ctx).apply {
            text = mon.monto(if (conNeta) o.neta(prefs) else o.tarifa, 0)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, sp(34f))
            setTextColor(Tema.HUESO)
            typeface = Fuentes.titulo
            gravity = Gravity.CENTER
            includeFontPadding = false
            maxLines = 1
            setPadding(e(8f), e(4f), e(8f), e(7f))
        }
        encoger(montoTxt, 18f)
        col.addView(montoTxt, LinearLayout.LayoutParams(-1, -2))

        // Abajo: tres columnas iguales separadas por líneas finas.
        col.addView(View(ctx).apply { setBackgroundColor(linea) }, LinearLayout.LayoutParams(-1, maxOf(1, dp(1))))
        fun tono(cumple: Boolean?) = when (cumple) { true -> Tema.VERDE; false -> Tema.ROJO; null -> Tema.HUESO }
        val recFalla = prefs.filtroRecMax && o.recogidaKm > 0 && o.recogidaKm > prefs.recogidaMaxKm
        val durFalla = prefs.filtroDurMax && o.viajeMin > prefs.duracionMax
        val celdas = listOf(
            Triple(fmt(o.porHora, 0), "por hora", tono(if (prefs.filtroHora) o.porHora >= prefs.minPorHora else null)),
            Triple(fmt(o.porKm, 1), "por km", tono(if (prefs.filtroKm) o.porKm >= prefs.minPorKm else null)),
            when {
                // «recogida» evita que el lector tome este dato como un tramo de la oferta.
                recFalla -> Triple(fmt(o.recogidaKm, 1), "km recogida", Tema.ROJO)
                durFalla -> Triple(fmt(o.viajeMin, 0), "min de viaje", Tema.ROJO)
                else -> Triple(fmt(o.min, 0), "minutos", Tema.HUESO)
            }
        )
        val pie = LinearLayout(ctx).apply { orientation = LinearLayout.HORIZONTAL }
        celdas.forEachIndexed { k, (n, etq, c) ->
            if (k > 0) pie.addView(View(ctx).apply { setBackgroundColor(linea) }, LinearLayout.LayoutParams(maxOf(1, dp(1)), -1))
            val celda = LinearLayout(ctx).apply {
                orientation = LinearLayout.VERTICAL
                gravity = Gravity.CENTER_HORIZONTAL
                setPadding(e(2f), e(5f), e(2f), e(6f))
            }
            celda.addView(TextView(ctx).apply {
                text = n
                setTextSize(TypedValue.COMPLEX_UNIT_SP, sp(12.5f))
                setTextColor(c)
                typeface = Fuentes.numero
                gravity = Gravity.CENTER
                includeFontPadding = false
                maxLines = 1
            }.also { encoger(it, 8f) }, LinearLayout.LayoutParams(-1, -2))
            celda.addView(TextView(ctx).apply {
                text = etq
                setTextSize(TypedValue.COMPLEX_UNIT_SP, sp(8.5f))
                setTextColor(Tema.TENUE)
                typeface = Fuentes.texto
                gravity = Gravity.CENTER
                includeFontPadding = false
                maxLines = 1
            }, LinearLayout.LayoutParams(-1, -2).apply { topMargin = e(2f) })
            pie.addView(celda, LinearLayout.LayoutParams(0, -2, 1f))
        }
        col.addView(pie, LinearLayout.LayoutParams(-1, -2))

        // ✕ en la esquina (zona de toque de 24 dp escalada).
        val cerrarBtn = ImageView(ctx).apply {
            setImageDrawable(Icono(Icono.CERRAR, Tema.TENUE))
            setPadding(e(6f), e(6f), e(6f), e(6f))
            if (interactivo) setOnClickListener { ocultar() }
        }
        marco.addView(cerrarBtn, FrameLayout.LayoutParams(e(24f), e(24f), Gravity.TOP or Gravity.END).apply {
            topMargin = e(1f); rightMargin = e(3f)
        })

        return marco
    }

    /** Achica el texto (sin bajar de [minimoSp]) si no cabe en una línea dentro de su espacio. */
    private fun encoger(t: TextView, minimoSp: Float = 10f) {
        t.addOnLayoutChangeListener { _, _, _, _, _, _, _, _, _ ->
            val disp = t.width - t.paddingLeft - t.paddingRight
            if (disp <= 0) return@addOnLayoutChangeListener
            val p = Paint(t.paint)
            val texto = t.text.toString()
            val minimo = minimoSp * ctx.resources.displayMetrics.scaledDensity
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

        /** Mezcla [a] con [b]: f = 0 da [a], f = 1 da [b]. */
        private fun mezclar(a: Int, b: Int, f: Float): Int = Color.rgb(
            (Color.red(a) * (1 - f) + Color.red(b) * f).toInt(),
            (Color.green(a) * (1 - f) + Color.green(b) * f).toInt(),
            (Color.blue(a) * (1 - f) + Color.blue(b) * f).toInt()
        )

        /** Ancho del pop-up al 100 %, en dp; se multiplica por el tamaño elegido en Ajustes. */
        const val ANCHO_BASE = 175

        /** Ancho del pop-up en píxeles según el tamaño elegido (sin el tope de la pantalla). */
        fun anchoPx(c: Context, p: Prefs): Int =
            (ANCHO_BASE * p.escalaPopup / 100f * c.resources.displayMetrics.density).toInt()
    }
}
