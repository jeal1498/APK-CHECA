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
        val firma = "$o|$app|$marca|$zona|${prefs.posicion}|${prefs.anchoPopup}"
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
        val ancho = minOf((area.width() * 0.94f).toInt(), dp(prefs.anchoPopup))
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
     * Marca v2, propuesta A «Franja» (https://claude.ai/artifact/YD5aQPLwyMABZ11dWVmAuh): franja del color con la
     * acción, lo que te queda en grande, viaje y gasolina (con ⚡) y tres fichas que se pintan si cumplen o no la meta.
     */
    fun construir(o: Oferta, interactivo: Boolean, app: String? = null, marca: Int = SIN_MARCA): View {
        val base = prefs.tamanoTexto.toFloat()
        val nivel = o.nivel(prefs)
        val acento = Tema.colorNivel(nivel)
        // 2ª opción: sólida, pero con la franja apagada para que la mejor resalte.
        val segunda = marca == SEGUNDA
        val franjaColor = if (segunda) mezclar(acento, Tema.TABLERO, 0.45f) else acento
        val tinta = if (segunda) Tema.HUESO else when (nivel) {
            Oferta.VERDE -> Tema.SOBRE_VERDE
            Oferta.AMARILLO -> SOBRE_AMARILLO
            else -> SOBRE_ROJO
        }
        val accion = when (nivel) {
            Oferta.VERDE -> "ACEPTAR"
            Oferta.AMARILLO -> "CONSIDERAR"
            else -> "RECHAZAR"
        }

        // Ojo: ningún texto del pop-up puede ser "solo un monto", para que el lector
        // no lo confunda con la tarifa de una oferta cuando queda encima de la app.
        fun txt(s: CharSequence, factor: Float, color: Int, tf: Typeface) = TextView(ctx).apply {
            text = s
            setTextColor(color)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, base * factor)
            typeface = tf
            includeFontPadding = false
        }

        fun pastilla(s: String, fondo: Int, color: Int) = txt(s, 0.6f, color, Fuentes.numero).apply {
            gravity = Gravity.CENTER
            letterSpacing = 0.06f
            maxLines = 1
            setPadding(dp(7), dp(4), dp(7), dp(4))
            background = GradientDrawable().apply {
                cornerRadius = dp(8).toFloat()
                setColor(fondo)
            }
        }

        val marco = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            background = GradientDrawable().apply {
                setColor(Tema.TABLERO)
                cornerRadius = dp(22).toFloat()
                // La mejor (dos ofertas a la vez) lleva un borde hueso grueso.
                setStroke(dp(if (marca == MEJOR) 3 else 1), if (marca == MEJOR) Tema.HUESO else Tema.BORDE)
            }
            clipToOutline = true
            elevation = dp(8).toFloat()
        }

        // Franja del semáforo: [app]  ACCIÓN  [✕]. Los lados pesan igual para que la acción quede centrada.
        val franja = LinearLayout(ctx).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setBackgroundColor(franjaColor)
            setPadding(dp(12), dp(1), dp(2), dp(1))
        }
        val izq = LinearLayout(ctx).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL or Gravity.START
        }
        val esMejor = marca == MEJOR
        if (!app.isNullOrEmpty()) {
            izq.addView(pastilla((if (esMejor) "★ " else "") + app.uppercase(),
                if (esMejor) Tema.ASFALTO else Color.argb(41, 12, 12, 16),
                if (esMejor) Tema.HUESO else tinta))
        } else if (esMejor) {
            izq.addView(pastilla("★ MEJOR", Tema.ASFALTO, Tema.HUESO))
        }
        if (segunda) {
            izq.addView(pastilla("2ª", Color.argb(41, 12, 12, 16), tinta),
                LinearLayout.LayoutParams(-2, -2).apply { leftMargin = dp(4) })
        }
        franja.addView(izq, LinearLayout.LayoutParams(0, -2, 1f))
        franja.addView(txt(accion, 1.1f, tinta, Fuentes.titulo).apply {
            letterSpacing = 0.06f
            gravity = Gravity.CENTER
            maxLines = 1
            setPadding(dp(6), 0, dp(6), 0)
        })
        val der = LinearLayout(ctx).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL or Gravity.END
        }
        // ✕ con zona de toque de 44 dp.
        val cerrarBtn = ImageView(ctx).apply {
            setImageDrawable(Icono(Icono.CERRAR, tinta))
            setPadding(dp(13), dp(13), dp(13), dp(13))
            if (interactivo) setOnClickListener { ocultar() }
        }
        der.addView(cerrarBtn, LinearLayout.LayoutParams(dp(44), dp(44)))
        franja.addView(der, LinearLayout.LayoutParams(0, -2, 1f))
        marco.addView(franja, LinearLayout.LayoutParams(-1, -2))

        val col = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(14), dp(10), dp(14), dp(14))
        }
        marco.addView(col, LinearLayout.LayoutParams(-1, -2))

        // Lo que te queda en grande, con «netos» en la misma línea (nunca un monto solo).
        val conNeta = prefs.mostrarNeta && prefs.kmPorLitro > 0
        val valor = "$" + fmt(if (conNeta) o.neta(prefs) else o.tarifa, 2)
        val etiqueta = if (conNeta) "netos" else "tarifa"
        val grande = SpannableString("$valor  $etiqueta")
        grande.setSpan(RelativeSizeSpan(0.34f), valor.length, grande.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        grande.setSpan(ForegroundColorSpan(Tema.TENUE), valor.length, grande.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        col.addView(txt(grande, 2.5f, Tema.HUESO, Fuentes.titulo).apply {
            gravity = Gravity.CENTER
            maxLines = 1
            letterSpacing = -0.03f
            encoger(this, 16f)
        }, LinearLayout.LayoutParams(-1, -2))

        // Viaje · gasolina y la dinámica ⚡ en la misma línea.
        val linea = LinearLayout(ctx).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
        }
        if (conNeta) {
            linea.addView(txt(
                "Viaje $" + fmt(o.tarifa, 2) + " · Gasolina −$" + fmt(o.gasolina(prefs), 2),
                0.72f, Tema.TENUE, Fuentes.texto
            ).apply { maxLines = 1 })
        }
        val rayo = when {
            o.multiplicador > 1.0 -> "⚡" + fmtX(o.multiplicador) + "x"
            o.dinamica > 0 -> "⚡+$" + String.format(Locale.US, "%.0f", o.dinamica)
            else -> null
        }
        if (rayo != null) {
            linea.addView(pastilla(rayo, Tema.ASFALTO, Tema.AMARILLO),
                LinearLayout.LayoutParams(-2, -2).apply { if (linea.childCount > 0) leftMargin = dp(8) })
        }
        if (linea.childCount > 0) col.addView(linea, LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(8) })

        // Fichas: $/h · $/km · tiempo (o el filtro de recogida/duración que falló).
        // Se pinta toda la ficha: verde si cumple la meta, roja si no; neutra si el filtro está apagado.
        val fichas = LinearLayout(ctx).apply { orientation = LinearLayout.HORIZONTAL }
        fun ficha(v: String, etq: String, cumple: Boolean?) = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(dp(4), dp(7), dp(4), dp(7))
            background = GradientDrawable().apply {
                cornerRadius = dp(12).toFloat()
                when (cumple) {
                    true -> { setColor(FICHA_SI); setStroke(dp(1) + dp(1) / 2, Tema.VERDE) }
                    false -> { setColor(FICHA_NO); setStroke(dp(1) + dp(1) / 2, Tema.ROJO) }
                    null -> setColor(Tema.CAMPO)
                }
            }
            val color = when (cumple) { true -> Tema.VERDE; false -> Tema.ROJO; null -> Tema.HUESO }
            addView(txt(v, 0.85f, color, Fuentes.numero).apply { maxLines = 1; gravity = Gravity.CENTER; encoger(this) },
                LinearLayout.LayoutParams(-1, -2))
            addView(txt(etq, 0.6f, Tema.TENUE, Fuentes.texto).apply {
                maxLines = 1
                gravity = Gravity.CENTER
                setPadding(0, dp(3), 0, 0)
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
        col.addView(fichas, LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(10) })

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
        // Texto oscuro sobre la franja amarilla y roja (marca v2; el del verde es Tema.SOBRE_VERDE)
        private val SOBRE_AMARILLO = Color.rgb(43, 34, 0)
        private val SOBRE_ROJO = Color.rgb(31, 0, 0)
        // Fichas que cumplen o no la meta
        private val FICHA_SI = Color.rgb(18, 48, 31)
        private val FICHA_NO = Color.rgb(58, 20, 20)
    }
}
