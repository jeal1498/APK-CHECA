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
import android.view.ViewOutlineProvider
import android.view.WindowManager
import android.view.animation.DecelerateInterpolator
import android.view.animation.LinearInterpolator
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
    private var abierto = false              // 10.86: el compacto de esta oferta ya se abrió
    private var ultApp: String? = null
    private var ultZona: Rect? = null
    private var ultMarca = SIN_MARCA
    private var barraRelleno: View? = null   // la barra de tiempo que se vacía (solo el pop-up real)
    private var finPlazo = 0L                // uptimeMillis en que se cierra solo
    private val cerrar = Runnable { temporizador = false; ocultar() }

    init { Fuentes.iniciar(ctx) }

    /** Se llama cuando el pop-up se cierra (✕ o tiempo agotado). */
    var alCerrar: (() -> Unit)? = null

    /**
     * «Leyó mal» (10.77): el conductor avisa que este pop-up no coincide con la oferta. Devuelve
     * true si el reporte se pudo mandar. Sin esto (vista previa, sin sesión) el botón no sale.
     */
    var alReportar: (() -> Boolean)? = null
    /** La oferta que ya se reportó: su pop-up muestra ✓ y no se manda dos veces. */
    private var reportada: Oferta? = null

    /**
     * Muestra la oferta. [zona] es la parte de la pantalla de la app (pantalla dividida);
     * el pop-up se centra sobre ella. [reiniciar] = false conserva el tiempo restante.
     */
    fun mostrar(
        o: Oferta, app: String? = null, zona: Rect? = null,
        marca: Int = SIN_MARCA, reiniciar: Boolean = true
    ) {
        if (ultima != null && ultima != o) abierto = false
        ultima = o
        ultApp = app; ultZona = zona; ultMarca = marca
        val compacto = compactoActivo()
        // Misma oferta, misma marca y misma zona: no se vuelve a pintar (evita el parpadeo).
        val firma = "$o|$app|$marca|$zona|$compacto|${prefs.posicion}|${prefs.escalaPopup}"
        val actual = vista
        if (actual != null && firma == firmaVista) {
            if (reiniciar || !temporizador) arrancarTemporizador()
            return
        }
        firmaVista = firma
        val v = construir(o, true, app, marca)

        val real = DisplayMetrics()
        @Suppress("DEPRECATION")
        wm.defaultDisplay.getRealMetrics(real)
        val area = zona ?: Rect(0, 0, real.widthPixels, real.heightPixels)
        val ancho = minOf((area.width() * 0.94f).toInt(), if (compacto) anchoCompactoPx(ctx, prefs) else anchoPx(ctx, prefs))
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
        if (reiniciar || !temporizador) arrancarTemporizador() else animarBarra()
    }

    /** Plazo del pop-up: se cierra solo a los `duracionSeg`; la barra de abajo se vacía en ese tiempo. */
    private fun arrancarTemporizador() {
        handler.removeCallbacks(cerrar)
        handler.postDelayed(cerrar, prefs.duracionSeg * 1000L)
        temporizador = true
        finPlazo = android.os.SystemClock.uptimeMillis() + prefs.duracionSeg * 1000L
        animarBarra()
    }

    /** La barra de abajo se vacía desde lo que queda del plazo hasta 0 (si la vista se rehízo, sigue donde iba). */
    private fun animarBarra() {
        val b = barraRelleno ?: return
        val total = (prefs.duracionSeg * 1000L).coerceAtLeast(1L)
        val resta = (finPlazo - android.os.SystemClock.uptimeMillis()).coerceIn(0L, total)
        b.animate().cancel()
        b.pivotX = 0f
        b.scaleX = resta.toFloat() / total
        b.animate().scaleX(0f).setDuration(resta).setInterpolator(LinearInterpolator()).start()
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
                    arrancarTemporizador()
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
        // La captura que se está leyendo pudo tomarse con el pop-up aún puesto: su zona se
        // sigue tapando un momento ([rectTapado]) para que el lector no lea nuestros números.
        rectCerrado = rect()
        cerradoEn = android.os.SystemClock.uptimeMillis()
        barraRelleno?.animate()?.cancel()
        barraRelleno = null
        abierto = false
        vista?.let { try { wm.removeView(it) } catch (_: Exception) { } }
        vista = null
        firmaVista = null
        lpActual = null
        reportada = null
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
        val r = Rect(lp.x, lp.y, lp.x + lp.width, lp.y + alto)
        // Donde Android lo puso de verdad (barras o recorte de cámara pueden moverlo).
        if (v.width > 0 && v.height > 0) try {
            val p = IntArray(2)
            v.getLocationOnScreen(p)
            r.union(p[0], p[1], p[0] + v.width, p[1] + v.height)
        } catch (_: Exception) { }
        return r
    }

    private var rectCerrado: Rect? = null
    private var cerradoEn = 0L

    /** Como [rect], pero también la zona de un pop-up recién quitado (2 s): la foto en curso aún lo trae. */
    fun rectTapado(): Rect? = rect()
        ?: rectCerrado?.takeIf { android.os.SystemClock.uptimeMillis() - cerradoEn < 2000L }

    /**
     * Pop-up 10.86 (diseño v9 de Eduardo, 20 nov, `docs/propuestas/popup-v9.png`). También se usa para la vista previa
     * en Ajustes (sin acciones). Completo horizontal: a la izquierda un bloque del color del semáforo con la decisión y el
     * monto grandes (texto oscuro en los tres colores); a la derecha `$/h` y `$/km` grandes con ✓/✕ y, pegados, los
     * minutos y los km. Los avisos son píldoras rojas (recogida y viaje lado a lado; tarifa mínima o zona, a todo el ancho).
     * Abajo, chico: «Leyó mal», la app con su tarifa y una barra de tiempo que se vacía. Con «Pop-up compacto» en Ajustes
     * sale una píldora (color, monto y `$/h`) que se toca para abrir el completo. Todo se escala con prefs.escalaPopup
     * (70–160 %) y las medidas repartidas en pesos, para que quepa en pantallas angostas.
     * Sin la línea «N min (X km) total»: los números sueltos «18 min» y «6.5 km» no forman un tramo, y el OCR además ignora
     * lo que tapa el pop-up (se prueba en `PopupLectorTest`).
     */
    fun construir(o: Oferta, interactivo: Boolean, app: String? = null, marca: Int = SIN_MARCA): View {
        val d = Datos(o, app, marca)
        return if (compactoActivo()) construirCompacto(d, interactivo) else construirCompleto(d, interactivo)
    }

    private fun compactoActivo() = prefs.popupCompacto && !abierto

    /** Lo que se muestra de una oferta, ya calculado (lo usan el completo y el compacto). */
    private inner class Datos(val o: Oferta, val app: String?, val marca: Int) {
        val nivel = o.nivel(prefs)
        val segunda = marca == SEGUNDA
        val acento = Oscuro.colorNivel(nivel)
        // 2ª opción (dos ofertas a la vez): el color apagado para que la mejor resalte.
        val color = if (segunda) mezclar(acento, Oscuro.TABLERO, 0.45f) else acento
        val sobre = when (nivel) { Oferta.VERDE -> COLOR_SOBRE_VERDE; Oferta.AMARILLO -> COLOR_SOBRE_AMARILLO; else -> COLOR_SOBRE_ROJO }
        val accion = (when { marca == MEJOR -> "★ "; segunda -> "2ª "; else -> "" }) +
            when (nivel) { Oferta.VERDE -> "ACEPTAR"; Oferta.AMARILLO -> "CONSIDERAR"; else -> "RECHAZAR" }
        val mon = prefs.moneda
        private val conNeta = prefs.mostrarNeta && (prefs.kmPorLitro > 0 || prefs.desgastePorKm > 0)
        val monto = mon.monto(if (conNeta) o.neta(prefs) else o.tarifa, 0)
        val cumpleHora: Boolean? = if (prefs.filtroHora) o.porHora >= prefs.minPorHora else null
        val cumpleKm: Boolean? = if (prefs.filtroKm) o.porKm >= prefs.minPorKm else null
        val recFalla = prefs.filtroRecMax && o.recogidaKm > 0 && o.recogidaKm > prefs.recogidaMaxKm
        val durFalla = prefs.filtroDurMax && o.viajeMin > prefs.duracionMax
        private val baseMin = o.baseTarifaMinima(prefs)
        private val tarFalla = prefs.filtroTarMin && prefs.tarifaMinima > 0 && baseMin < prefs.tarifaMinima
        /** Tercera píldora (a todo el ancho): la zona bloqueada manda sobre la tarifa mínima. */
        val extra: String? = when {
            o.bloqueada -> o.motivoZona()
            tarFalla -> "⚠ Menos de tu mínimo · " + mon.monto(baseMin, 0)
            else -> null
        }
        val nAvisos = (if (recFalla) 1 else 0) + (if (durFalla) 1 else 0) + (if (extra != null) 1 else 0)
    }

    private fun tono(cumple: Boolean?) = when (cumple) { true -> Tema.VERDE; false -> COLOR_NO; null -> Oscuro.HUESO }
    private fun marca(cumple: Boolean?) = when (cumple) { true -> "✓ "; false -> "✕ "; null -> "" }

    private fun redondo(relleno: Int, radio: Float, borde: Int = 0, grosor: Int = 0) = GradientDrawable().apply {
        setColor(relleno)
        cornerRadius = radio
        if (grosor > 0) setStroke(grosor, borde)
    }

    private fun texto(t: String, tam: Float, color: Int, tf: Typeface, gravedad: Int = Gravity.CENTER): TextView = TextView(ctx).apply {
        text = t
        setTextSize(TypedValue.COMPLEX_UNIT_SP, tam)
        setTextColor(color)
        typeface = tf
        gravity = gravedad
        includeFontPadding = false
        maxLines = 1
    }

    /** Barra de tiempo (abajo): se vacía mientras corre el plazo del pop-up. La vista previa la deja fija. */
    private fun barraDeTiempo(color: Int, alto: Int, interactivo: Boolean): View {
        val caja = FrameLayout(ctx).apply { setBackgroundColor(Color.argb(26, 255, 255, 255)) }
        val relleno = View(ctx).apply { setBackgroundColor(color) }
        caja.addView(relleno, FrameLayout.LayoutParams(-1, alto))
        if (interactivo) {
            barraRelleno = relleno
            relleno.pivotX = 0f
        } else {
            relleno.pivotX = 0f
            relleno.scaleX = 0.62f
        }
        return caja
    }

    private fun construirCompleto(d: Datos, interactivo: Boolean): View {
        val esc = prefs.escalaPopup / 100f
        fun e(v: Float): Int = (v * esc * ctx.resources.displayMetrics.density).toInt()
        fun sp(v: Float) = v * esc
        val o = d.o

        val marco = FrameLayout(ctx).apply {
            background = redondo(Oscuro.ASFALTO, e(16f).toFloat(), d.color, e(if (d.marca == MEJOR) 4f else 2f).coerceAtLeast(2))
            // El bloque de color no se sale de las esquinas redondeadas.
            clipToOutline = true
            outlineProvider = ViewOutlineProvider.BACKGROUND
            elevation = dp(8).toFloat()
        }
        val col = LinearLayout(ctx).apply { orientation = LinearLayout.VERTICAL }
        marco.addView(col, FrameLayout.LayoutParams(-1, -2))

        // Cuerpo: bloque de color (decisión y monto) + $/h y $/km con sus minutos y km. Repartido en pesos 156 : 228.
        val cuerpo = LinearLayout(ctx).apply { orientation = LinearLayout.HORIZONTAL; minimumHeight = e(100f) }
        val bloque = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setBackgroundColor(d.color)
            setPadding(e(6f), e(10f), e(6f), e(10f))
        }
        bloque.addView(texto(d.accion, sp(14f), d.sobre, Fuentes.negrita).apply { letterSpacing = 0.06f }.also { encoger(it, 8f) },
            LinearLayout.LayoutParams(-1, -2))
        bloque.addView(texto(d.monto, sp(54f), d.sobre, Fuentes.titulo).apply { letterSpacing = -0.03f }.also { encoger(it, 24f) },
            LinearLayout.LayoutParams(-1, -2))
        cuerpo.addView(bloque, LinearLayout.LayoutParams(0, -1, 156f))

        val der = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(e(10f), e(10f), e(12f), e(8f))
        }
        fun fila(metrica: String, c: Int, dato: String): LinearLayout = LinearLayout(ctx).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            addView(texto(metrica, sp(22f), c, Fuentes.numero, Gravity.START or Gravity.CENTER_VERTICAL).also { encoger(it, 11f) },
                LinearLayout.LayoutParams(0, -2, 1f))
            addView(texto(dato, sp(13f), Oscuro.HUESO, Fuentes.numero, Gravity.END or Gravity.CENTER_VERTICAL).also { encoger(it, 8f) }
                .apply { alpha = 0.92f; setPadding(e(6f), 0, 0, 0) }, LinearLayout.LayoutParams(-2, -2))
        }
        der.addView(fila(marca(d.cumpleHora) + d.mon.monto(o.porHora, 0) + "/h", tono(d.cumpleHora), fmt(o.min, 0) + " min"),
            LinearLayout.LayoutParams(-1, -2))
        der.addView(fila(marca(d.cumpleKm) + d.mon.monto(o.porKm, 1) + "/km", tono(d.cumpleKm), fmt(o.km, 1) + " km"),
            LinearLayout.LayoutParams(-1, -2).apply { topMargin = e(2f) })
        cuerpo.addView(der, LinearLayout.LayoutParams(0, -1, 228f))
        col.addView(cuerpo, LinearLayout.LayoutParams(-1, -2))

        // Avisos: píldoras rojas. Recogida y viaje lado a lado; la tercera (tarifa mínima o zona) a todo el ancho.
        if (d.nAvisos > 0) {
            val avisos = LinearLayout(ctx).apply {
                orientation = LinearLayout.VERTICAL
                setBackgroundColor(Color.argb(24, 239, 68, 68))
                setPadding(e(10f), e(7f), e(10f), e(7f))
            }
            avisos.addView(View(ctx).apply { setBackgroundColor(Color.argb(102, 239, 68, 68)) }, LinearLayout.LayoutParams(-1, 1)
                .apply { topMargin = -e(7f); bottomMargin = e(6f) })
            fun pildora(t: String): TextView = texto(t, sp(14f), COLOR_AVISO, Fuentes.numero).apply {
                background = redondo(Color.argb(51, 239, 68, 68), e(40f).toFloat(), Tema.ROJO, maxOf(1, e(1.5f)))
                setPadding(e(11f), e(4f), e(11f), e(4f))
            }.also { encoger(it, 10f) }
            val unidas = ArrayList<String>()
            if (d.recFalla) unidas.add("📍 Recogida " + fmt(o.recogidaKm, 1) + " km")
            if (d.durFalla) unidas.add("⏱ Viaje " + fmt(o.viajeMin, 0) + " min")
            if (unidas.isNotEmpty()) {
                val f1 = LinearLayout(ctx).apply { orientation = LinearLayout.HORIZONTAL }
                unidas.forEachIndexed { i, t ->
                    f1.addView(pildora(t), LinearLayout.LayoutParams(0, -2, 1f).apply { if (i > 0) leftMargin = e(8f) })
                }
                avisos.addView(f1, LinearLayout.LayoutParams(-1, -2))
            }
            d.extra?.let { t ->
                avisos.addView(pildora(t), LinearLayout.LayoutParams(-1, -2).apply { if (unidas.isNotEmpty()) topMargin = e(8f) })
            }
            col.addView(avisos, LinearLayout.LayoutParams(-1, -2))
        }

        // Abajo, chico y gris: «Leyó mal», la app con su tarifa y la barra de tiempo.
        val meta = LinearLayout(ctx).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setBackgroundColor(Color.argb(18, 255, 255, 255))
            setPadding(e(10f), e(4f), e(10f), e(5f))
        }
        val reportar = alReportar
        if (interactivo && reportar != null && Cuenta.conectada(ctx)) {
            val malBtn = texto(if (reportada == o) "✓ Enviado" else "⚠ Leyó mal", maxOf(sp(11f), 9f),
                if (reportada == o) Tema.VERDE else Color.rgb(185, 185, 194), Fuentes.texto).apply {
                background = redondo(Color.TRANSPARENT, e(40f).toFloat(), Color.argb(34, 255, 255, 255), maxOf(1, e(1f)))
                setPadding(e(9f), e(5f), e(9f), e(5f))
                contentDescription = "Leyó mal: reportar esta lectura"
                setOnClickListener {
                    if (reportada == o) return@setOnClickListener
                    val enviado = try { reportar() } catch (_: Exception) { false }
                    if (enviado) {
                        reportada = o
                        text = "✓ Enviado"
                        setTextColor(Tema.VERDE)
                    }
                    try {
                        android.widget.Toast.makeText(
                            ctx.applicationContext,
                            if (enviado) "Reporte enviado. Gracias: lo revisamos." else "No se pudo enviar el reporte.",
                            android.widget.Toast.LENGTH_SHORT
                        ).show()
                    } catch (_: Exception) { }
                }
            }
            meta.addView(malBtn, LinearLayout.LayoutParams(-2, -2))
        }
        val nombreApp = when (d.app?.lowercase(Locale.ROOT)) { "uber" -> "Uber"; "didi" -> "DiDi"; else -> null }
        meta.addView(texto((if (nombreApp != null) "$nombreApp · " else "") + d.mon.monto(o.tarifa, 2), maxOf(sp(11f), 9f), Oscuro.TENUE,
            Fuentes.texto, Gravity.END or Gravity.CENTER_VERTICAL), LinearLayout.LayoutParams(0, -2, 1f))
        col.addView(meta, LinearLayout.LayoutParams(-1, -2))
        col.addView(barraDeTiempo(d.color, e(5f).coerceAtLeast(3), interactivo), LinearLayout.LayoutParams(-1, e(5f).coerceAtLeast(3)))

        // ✕ en la esquina de arriba a la derecha (zona de toque de 48 dp fija, no se encoge con el tamaño).
        val cerrarBtn = ImageView(ctx).apply {
            setImageDrawable(Icono(Icono.CERRAR, Oscuro.TENUE))
            val lado = dp(48)
            val chico = dp(6)
            val resto = maxOf(0, lado - e(12f) - chico)
            setPadding(resto, chico, chico, resto)
            if (interactivo) setOnClickListener { ocultar() }
        }
        marco.addView(cerrarBtn, FrameLayout.LayoutParams(dp(48), dp(48), Gravity.TOP or Gravity.END).apply {
            topMargin = dp(2); rightMargin = dp(2)
        })
        return marco
    }

    /** Píldora de 340×56 dp: punto del color, decisión, monto y `$/h`; con «⚠ N» si hay avisos. Se toca para abrir el completo. */
    private fun construirCompacto(d: Datos, interactivo: Boolean): View {
        val esc = prefs.escalaPopup / 100f
        fun e(v: Float): Int = (v * esc * ctx.resources.displayMetrics.density).toInt()
        fun sp(v: Float) = v * esc
        val marco = FrameLayout(ctx).apply {
            background = redondo(Oscuro.ASFALTO, e(40f).toFloat(), d.color, e(3f).coerceAtLeast(2))
            clipToOutline = true
            outlineProvider = ViewOutlineProvider.BACKGROUND
            elevation = dp(8).toFloat()
            minimumHeight = e(56f)
        }
        val fila = LinearLayout(ctx).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            setPadding(e(14f), e(8f), e(14f), e(8f))
        }
        fila.addView(View(ctx).apply { background = redondo(d.color, e(8f).toFloat()) }, LinearLayout.LayoutParams(e(13f), e(13f)))
        fila.addView(texto(d.accion, sp(12f), d.color, Fuentes.negrita).apply { letterSpacing = 0.04f; setPadding(e(9f), 0, e(9f), 0) },
            LinearLayout.LayoutParams(-2, -2))
        fila.addView(texto(d.monto, sp(28f), Oscuro.HUESO, Fuentes.titulo), LinearLayout.LayoutParams(-2, -2))
        fila.addView(texto(marca(d.cumpleHora) + d.mon.monto(d.o.porHora, 0) + "/h", sp(16f), tono(d.cumpleHora), Fuentes.numero)
            .apply { setPadding(e(9f), 0, 0, 0) }, LinearLayout.LayoutParams(-2, -2))
        if (d.nAvisos > 0) {
            fila.addView(texto("⚠ " + d.nAvisos, sp(12f), Color.WHITE, Fuentes.numero).apply {
                background = redondo(Tema.ROJO, e(40f).toFloat())
                setPadding(e(8f), e(2f), e(8f), e(2f))
            }, LinearLayout.LayoutParams(-2, -2).apply { leftMargin = e(8f) })
        }
        marco.addView(fila, FrameLayout.LayoutParams(-1, -2, Gravity.CENTER))
        marco.addView(barraDeTiempo(d.color, e(4f).coerceAtLeast(3), interactivo),
            FrameLayout.LayoutParams(-1, e(4f).coerceAtLeast(3), Gravity.BOTTOM))
        if (interactivo) marco.setOnClickListener { abrirCompleto() }
        return marco
    }

    /** Un toque en el compacto: ese pop-up pasa a completo (con su plazo renovado). */
    private fun abrirCompleto() {
        val o = ultima ?: return
        abierto = true
        mostrar(o, ultApp, ultZona, ultMarca, true)
    }

    /** Achica el texto (sin bajar de [minimoSp]) si no cabe en una línea dentro de su espacio. */
    private fun encoger(t: TextView, minimoSp: Float = 10f) {
        t.addOnLayoutChangeListener { _, _, _, _, _, _, _, _, _ ->
            val disp = t.width - t.paddingLeft - t.paddingRight
            if (disp <= 0) return@addOnLayoutChangeListener
            val p = Paint(t.paint)
            val texto = t.text.toString()
            val minimo = minimoSp * escalaFuente()
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

    private fun escalaFuente(): Float =
        ctx.resources.displayMetrics.density * ctx.resources.configuration.fontScale

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

        /** Ancho del pop-up completo al 100 %, en dp (diseño v9, 10.86); se multiplica por el tamaño elegido en Ajustes. */
        const val ANCHO_BASE = 384
        /** Ancho del pop-up compacto al 100 %, en dp. */
        const val ANCHO_COMPACTO = 340

        // Texto sobre el bloque de color (el mismo oscuro en los tres colores) y rojos del diseño v9.
        private val COLOR_SOBRE_VERDE = Color.rgb(6, 33, 15)
        private val COLOR_SOBRE_AMARILLO = Color.rgb(27, 21, 0)
        private val COLOR_SOBRE_ROJO = Color.rgb(27, 0, 0)
        private val COLOR_NO = Color.rgb(255, 107, 107)
        private val COLOR_AVISO = Color.rgb(255, 208, 208)

        fun anchoCompactoPx(c: Context, p: Prefs): Int =
            (ANCHO_COMPACTO * p.escalaPopup / 100f * c.resources.displayMetrics.density).toInt()

        /** Ancho del pop-up en píxeles según el tamaño elegido (sin el tope de la pantalla). */
        fun anchoPx(c: Context, p: Prefs): Int =
            (ANCHO_BASE * p.escalaPopup / 100f * c.resources.displayMetrics.density).toInt()
    }
}
