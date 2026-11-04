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
import android.text.SpannableString
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
     * Pop-up compacto (v10.1): borde del color del semáforo, «ACCIÓN: $neta» con ✕,
     * y abajo $/h · $/km · min, cada dato verde si cumple su meta y rojo si no.
     * Todo se escala con prefs.escalaPopup (70–160 %).
     */
    fun construir(o: Oferta, interactivo: Boolean, app: String? = null, marca: Int = SIN_MARCA): View {
        val esc = prefs.escalaPopup / 100f
        fun e(v: Float): Int = (v * esc * ctx.resources.displayMetrics.density).toInt()
        val nivel = o.nivel(prefs)
        val segunda = marca == SEGUNDA
        val acento = Tema.colorNivel(nivel)
        // 2ª opción (dos ofertas a la vez): el color apagado para que la mejor resalte.
        val color = if (segunda) mezclar(acento, Tema.TABLERO, 0.45f) else acento
        val accion = when (nivel) {
            Oferta.VERDE -> "ACEPTAR"
            Oferta.AMARILLO -> "CONSIDERAR"
            else -> "RECHAZAR"
        }

        // Ojo: ningún texto del pop-up puede ser "solo un monto", para que el lector
        // no lo confunda con la tarifa de una oferta cuando queda encima de la app.
        fun txt(s: CharSequence, sp: Float, c: Int, tf: Typeface) = TextView(ctx).apply {
            text = s
            setTextColor(c)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, sp * esc)
            typeface = tf
            includeFontPadding = false
            maxLines = 1
        }

        val marco = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            background = GradientDrawable().apply {
                setColor(Tema.TABLERO)
                cornerRadius = e(14f).toFloat()
                // La mejor de dos lleva el borde más grueso.
                setStroke(e(if (marca == MEJOR) 3.5f else 2.5f).coerceAtLeast(2), color)
            }
            setPadding(e(10f), e(7f), e(6f), e(8f))
            elevation = dp(8).toFloat()
        }

        // Línea 1: [★] ACCIÓN: $neta [⚡] · ✕. Un espacio igual al de la ✕ a la izquierda la deja centrada.
        val mon = prefs.moneda
        val conNeta = prefs.mostrarNeta && prefs.kmPorLitro > 0
        val valor = mon.monto(if (conNeta) o.neta(prefs) else o.tarifa, 0)
        val rayo = when {
            o.multiplicador > 1.0 -> "⚡" + fmtX(o.multiplicador) + "x"
            o.dinamica > 0 -> "⚡+" + mon.monto(o.dinamica, 0)
            else -> null
        }
        val pre = when {
            marca == MEJOR -> "★ "
            segunda -> "2ª "
            else -> ""
        }
        val cuerpo = "$accion "
        val sb = SpannableString(pre + cuerpo + valor + (if (rayo != null) "  $rayo" else ""))
        var i = 0
        if (pre.isNotEmpty()) {
            sb.setSpan(RelativeSizeSpan(0.75f), 0, pre.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            sb.setSpan(ForegroundColorSpan(Tema.HUESO), 0, pre.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            i = pre.length
        }
        sb.setSpan(ForegroundColorSpan(color), i, i + cuerpo.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        i += cuerpo.length
        sb.setSpan(ForegroundColorSpan(Tema.HUESO), i, i + valor.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        i += valor.length
        if (rayo != null) {
            sb.setSpan(RelativeSizeSpan(0.65f), i, sb.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            sb.setSpan(ForegroundColorSpan(Tema.AMARILLO), i, sb.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        }
        val fila1 = LinearLayout(ctx).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        fila1.addView(txt(sb, 15f, Tema.HUESO, Fuentes.titulo).apply {
            gravity = Gravity.START
            encoger(this, 9f)
        }, LinearLayout.LayoutParams(0, -2, 1f))
        // ✕ con zona de toque amplia.
        val cerrarBtn = ImageView(ctx).apply {
            setImageDrawable(Icono(Icono.CERRAR, Tema.HUESO))
            setPadding(e(6f), e(6f), e(6f), e(6f))
            if (interactivo) setOnClickListener { ocultar() }
        }
        fila1.addView(cerrarBtn, LinearLayout.LayoutParams(e(24f), e(24f)))
        marco.addView(fila1, LinearLayout.LayoutParams(-1, -2))

        // Línea 2: $/h | $/km | min. Verde si cumple la meta, rojo si no, hueso si el filtro está apagado.
        fun tono(cumple: Boolean?) = when (cumple) { true -> Tema.VERDE; false -> Tema.ROJO; null -> Tema.HUESO }
        val recFalla = prefs.filtroRecMax && o.recogidaKm > 0 && o.recogidaKm > prefs.recogidaMaxKm
        val durFalla = prefs.filtroDurMax && o.viajeMin > prefs.duracionMax
        val datos = listOf(
            mon.tasa(o.porHora, 0, "/h") to tono(if (prefs.filtroHora) o.porHora >= prefs.minPorHora else null),
            mon.tasa(o.porKm, 1, "/km") to tono(if (prefs.filtroKm) o.porKm >= prefs.minPorKm else null),
            when {
                // "rec." y "total" evitan que el lector tome estos datos como un tramo de la oferta.
                recFalla -> (fmt(o.recogidaKm, 1) + " km rec.") to Tema.ROJO
                durFalla -> (fmt(o.viajeMin, 0) + "m") to Tema.ROJO
                else -> (fmt(o.min, 0) + "m") to Tema.HUESO
            }
        )
        val sep = " · "
        val linea = StringBuilder()
        val tramos = ArrayList<Triple<Int, Int, Int>>()
        datos.forEachIndexed { k, (t, c) ->
            if (k > 0) linea.append(sep)
            tramos.add(Triple(linea.length, linea.length + t.length, c))
            linea.append(t)
        }
        val sb2 = SpannableString(linea.toString())
        for ((a, b, c) in tramos) sb2.setSpan(ForegroundColorSpan(c), a, b, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        var p = 0
        while (true) {
            val k = linea.indexOf(sep, p)
            if (k < 0) break
            sb2.setSpan(ForegroundColorSpan(Tema.TENUE), k, k + sep.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            p = k + sep.length
        }
        marco.addView(txt(sb2, 12f, Tema.HUESO, Fuentes.numero).apply {
            gravity = Gravity.START
            encoger(this, 8f)
        }, LinearLayout.LayoutParams(-1, -2).apply { topMargin = e(3f) })

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
        const val ANCHO_BASE = 190

        /** Ancho del pop-up en píxeles según el tamaño elegido (sin el tope de la pantalla). */
        fun anchoPx(c: Context, p: Prefs): Int =
            (ANCHO_BASE * p.escalaPopup / 100f * c.resources.displayMetrics.density).toInt()
    }
}
