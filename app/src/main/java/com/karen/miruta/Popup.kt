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
import android.text.style.MetricAffectingSpan
import android.text.TextPaint
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
     * Pop-up E (10.11), pensado para leerlo de un vistazo manejando: tarjeta oscura con borde del
     * semáforo; arriba la acción (★/2ª delante) con el ⚡, en medio el monto MUY grande, luego
     * `$/h | $/km` (verde si cumple la meta, rojo si no, hueso si el filtro está apagado) y abajo,
     * chico, «24 min (6.0 km) total». La palabra «total» es obligatoria: el lector ignora los
     * textos con «total», así no toma esta línea por un tramo de la oferta.
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
        val linea = Color.argb(110, Color.red(Tema.TENUE), Color.green(Tema.TENUE), Color.blue(Tema.TENUE))
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
                setColor(Tema.TABLERO)
                cornerRadius = e(16f).toFloat()
                // La mejor de dos lleva el borde más grueso.
                setStroke(e(if (marca == MEJOR) 4f else 2.5f).coerceAtLeast(2), color)
            }
            elevation = dp(8).toFloat()
        }
        val col = LinearLayout(ctx).apply { orientation = LinearLayout.VERTICAL }
        marco.addView(col, FrameLayout.LayoutParams(-1, -2))

        // Arriba: la acción y el ⚡ (un solo texto; los lados iguales dejan lugar a la ✕).
        val rayo = when {
            o.multiplicador > 1.0 -> "⚡" + fmtX(o.multiplicador) + "x"
            o.dinamica > 0 -> "⚡+" + mon.monto(o.dinamica, 0)
            else -> null
        }
        val cab = SpannableStringBuilder()
        cab.append(pre + accion)
        cab.setSpan(ForegroundColorSpan(color), 0, cab.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        if (rayo != null) {
            cab.append("  ")
            val r0 = cab.length
            cab.append(rayo)
            cab.setSpan(ForegroundColorSpan(Tema.AMARILLO), r0, cab.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            cab.setSpan(RelativeSizeSpan(0.8f), r0, cab.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            cab.setSpan(FuenteSpan(Fuentes.numero), r0, cab.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        }
        val cabeza = TextView(ctx).apply {
            text = cab
            setTextSize(TypedValue.COMPLEX_UNIT_SP, sp(14f))
            typeface = Fuentes.negrita
            letterSpacing = 0.1f
            gravity = Gravity.CENTER
            includeFontPadding = false
            maxLines = 1
            setHorizontallyScrolling(true)
            setPadding(e(26f), e(8f), e(26f), 0)
        }
        encogerFila(cabeza, 9f)
        col.addView(cabeza, LinearLayout.LayoutParams(-1, -2))

        // En medio: el monto (neto si así está en Ajustes), lo más grande de la tarjeta.
        val conNeta = prefs.mostrarNeta && (prefs.kmPorLitro > 0 || prefs.desgastePorKm > 0)
        val montoTxt = TextView(ctx).apply {
            text = mon.monto(if (conNeta) o.neta(prefs) else o.tarifa, 0)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, sp(46f))
            setTextColor(Tema.HUESO)
            typeface = Fuentes.titulo
            gravity = Gravity.CENTER
            includeFontPadding = false
            maxLines = 1
            setPadding(e(8f), e(2f), e(8f), e(2f))
        }
        encoger(montoTxt, 24f)
        col.addView(montoTxt, LinearLayout.LayoutParams(-1, -2))

        // $/h | $/km, grandes.
        fun tono(cumple: Boolean?) = when (cumple) { true -> Tema.VERDE; false -> Tema.ROJO; null -> Tema.HUESO }
        fun metrica(txt: String, c: Int) = TextView(ctx).apply {
            text = txt
            setTextSize(TypedValue.COMPLEX_UNIT_SP, sp(16.5f))
            setTextColor(c)
            typeface = Fuentes.numero
            gravity = Gravity.CENTER
            includeFontPadding = false
            maxLines = 1
            setPadding(e(2f), 0, e(2f), 0)
        }.also { encoger(it, 9f) }
        val fila = LinearLayout(ctx).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(e(6f), e(5f), e(6f), 0)
        }
        fila.addView(metrica(mon.monto(o.porHora, 0) + "/h",
            tono(if (prefs.filtroHora) o.porHora >= prefs.minPorHora else null)), LinearLayout.LayoutParams(0, -2, 1f))
        fila.addView(View(ctx).apply { setBackgroundColor(linea) },
            LinearLayout.LayoutParams(maxOf(1, dp(1)), e(16f)).apply { leftMargin = e(4f); rightMargin = e(4f) })
        fila.addView(metrica(mon.monto(o.porKm, 1) + "/km",
            tono(if (prefs.filtroKm) o.porKm >= prefs.minPorKm else null)), LinearLayout.LayoutParams(0, -2, 1f))
        col.addView(fila, LinearLayout.LayoutParams(-1, -2))

        // Abajo, chico: tiempo y distancia totales. «total» evita que el lector lo tome por un tramo.
        val recFalla = prefs.filtroRecMax && o.recogidaKm > 0 && o.recogidaKm > prefs.recogidaMaxKm
        val durFalla = prefs.filtroDurMax && o.viajeMin > prefs.duracionMax
        col.addView(TextView(ctx).apply {
            text = fmt(o.min, 0) + " min (" + fmt(o.km, 1) + " km) total"
            setTextSize(TypedValue.COMPLEX_UNIT_SP, sp(11f))
            setTextColor(if (recFalla || durFalla) Tema.ROJO else Tema.TENUE)
            typeface = Fuentes.numero
            gravity = Gravity.CENTER
            includeFontPadding = false
            maxLines = 1
            setPadding(e(6f), e(6f), e(6f), if (recFalla || durFalla) 0 else e(9f))
        }.also { encoger(it, 7f) }, LinearLayout.LayoutParams(-1, -2))

        // Si falla recogida o duración, una línea roja dice cuál (también con «total»).
        val aviso = when {
            recFalla -> "Recogida lejana · " + fmt(o.recogidaKm, 1) + " km total"
            durFalla -> "Viaje largo · " + fmt(o.viajeMin, 0) + " min total"
            else -> null
        }
        if (aviso != null) {
            col.addView(TextView(ctx).apply {
                text = aviso
                setTextSize(TypedValue.COMPLEX_UNIT_SP, sp(10f))
                setTextColor(Tema.ROJO)
                typeface = Fuentes.texto
                gravity = Gravity.CENTER
                includeFontPadding = false
                maxLines = 1
                setPadding(e(6f), e(3f), e(6f), e(9f))
            }, LinearLayout.LayoutParams(-1, -2))
        }

        // ✕ en la esquina, a la altura de la acción (zona de toque de 24 dp escalada).
        val cerrarBtn = ImageView(ctx).apply {
            setImageDrawable(Icono(Icono.CERRAR, Tema.TENUE))
            setPadding(e(6f), e(6f), e(6f), e(6f))
            if (interactivo) setOnClickListener { ocultar() }
        }
        marco.addView(cerrarBtn, FrameLayout.LayoutParams(e(24f), e(24f), Gravity.TOP or Gravity.END).apply {
            topMargin = e(3f); rightMargin = e(4f)
        })

        return marco
    }

    /** Fuente propia para un tramo del texto (TypefaceSpan pide Android 9; la app llega a 8). */
    private class FuenteSpan(private val tf: Typeface) : MetricAffectingSpan() {
        override fun updateDrawState(p: TextPaint) { p.typeface = tf }
        override fun updateMeasureState(p: TextPaint) { p.typeface = tf }
    }

    /** Como [encoger] pero para un texto con tramos de distinto tamaño (mide el renglón ya armado). */
    private fun encogerFila(t: TextView, minimoSp: Float) {
        t.addOnLayoutChangeListener { _, _, _, _, _, _, _, _, _ ->
            val disp = t.width - t.paddingLeft - t.paddingRight
            val l = t.layout
            if (disp <= 0 || l == null || l.lineCount == 0) return@addOnLayoutChangeListener
            val ancho = l.getLineWidth(0)
            if (ancho <= disp) return@addOnLayoutChangeListener
            val minimo = minimoSp * ctx.resources.displayMetrics.scaledDensity
            val nuevo = maxOf(minimo, t.textSize * disp / ancho)
            if (nuevo < t.textSize - 0.5f) t.post { t.setTextSize(TypedValue.COMPLEX_UNIT_PX, nuevo) }
        }
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
        const val ANCHO_BASE = 218

        /** Ancho del pop-up en píxeles según el tamaño elegido (sin el tope de la pantalla). */
        fun anchoPx(c: Context, p: Prefs): Int =
            (ANCHO_BASE * p.escalaPopup / 100f * c.resources.displayMetrics.density).toInt()
    }
}
