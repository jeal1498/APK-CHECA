package com.karen.miruta

import android.Manifest
import android.animation.LayoutTransition
import android.app.Activity
import android.app.AlertDialog
import android.app.Dialog
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.InsetDrawable
import android.graphics.drawable.LayerDrawable
import android.graphics.drawable.RippleDrawable
import android.media.projection.MediaProjectionManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.text.Editable
import android.text.InputType
import android.text.TextWatcher
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.view.WindowManager
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.HorizontalScrollView
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.SeekBar
import android.widget.Switch
import android.widget.TextView
import android.widget.Toast
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class MainActivity : Activity() {

    // ---------- colores (ver Tema) ----------
    private val cFondo = Tema.FONDO
    private val cTarjeta = Tema.TARJETA
    private val cTarjeta2 = Tema.TARJETA2
    private val cTexto = Tema.TEXTO
    private val cTexto2 = Tema.TEXTO2
    private val cTenue = Tema.TENUE
    private val cVerde = Tema.VERDE
    private val cAmarillo = Tema.AMARILLO
    private val cRojo = Tema.ROJO
    private val DORADO = Color.rgb(255, 214, 10)

    private val fuenteMedia: Typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)

    private lateinit var prefs: Prefs
    private var popupPrueba: Popup? = null
    private val handler = Handler(Looper.getMainLooper())

    private lateinit var contenido: FrameLayout
    private val pestanas = ArrayList<View>()
    private val botonesNav = ArrayList<Triple<Icono, TextView, Int>>()

    // Barra
    private lateinit var botonCircular: ImageView
    private lateinit var iconoCircular: Icono
    private lateinit var etiquetaCircular: TextView

    // Inicio
    private lateinit var tarjetaPermiso: View
    private lateinit var tarjetaLicencia: View
    private lateinit var licTitulo: TextView
    private lateinit var licTexto: TextView
    private lateinit var mejoresHorasLista: LinearLayout
    private lateinit var statVerde: TextView
    private lateinit var statAmarillo: TextView
    private lateinit var statRojo: TextView

    // Ajustes
    private lateinit var vistaPrevia: FrameLayout
    private lateinit var ayudaPermiso: TextView
    private lateinit var ayudaPermisoIcono: Icono
    private lateinit var licAjustes: TextView
    private val insignias = ArrayList<TextView>()

    private val refresco = object : Runnable {
        override fun run() {
            actualizarEstado()
            handler.postDelayed(this, 1500)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        prefs = Prefs(this)
        popupPrueba = Popup(this, prefs, WindowManager.LayoutParams.TYPE_APPLICATION)
        window.statusBarColor = cFondo
        window.navigationBarColor = cTarjeta

        contenido = FrameLayout(this)
        pestanas.add(pantallaInicio())
        pestanas.add(pantallaAjustes())
        pestanas.forEach { contenido.addView(it, FrameLayout.LayoutParams(-1, -1)) }

        val raiz = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(cFondo)
            clipChildren = false
            addView(contenido, LinearLayout.LayoutParams(-1, 0, 1f))
            addView(barraNavegacion(), LinearLayout.LayoutParams(-1, dp(88)))
        }
        setContentView(raiz)
        mostrarPestana(savedInstanceState?.getInt("tab") ?: 0, false)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putInt("tab", pestanas.indexOfFirst { it.visibility == View.VISIBLE }.coerceAtLeast(0))
    }

    override fun onResume() {
        super.onResume()
        actualizarEstado()
        actualizarResumen()
        handler.post(refresco)
    }

    override fun onPause() {
        handler.removeCallbacks(refresco)
        popupPrueba?.ocultar()
        super.onPause()
    }

    // =====================================================================
    // Barra inferior con botón circular al centro
    // =====================================================================

    private fun barraNavegacion(): View {
        val marco = FrameLayout(this).apply {
            clipChildren = false
            clipToPadding = false
        }
        val barra = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            background = GradientDrawable().apply {
                setColor(cTarjeta)
                cornerRadii = floatArrayOf(
                    dp(Tema.R_TARJETA).toFloat(), dp(Tema.R_TARJETA).toFloat(),
                    dp(Tema.R_TARJETA).toFloat(), dp(Tema.R_TARJETA).toFloat(), 0f, 0f, 0f, 0f
                )
            }
        }
        fun pestana(nombre: String, tipo: Int, i: Int): View {
            val ico = Icono(tipo, cTenue)
            val etq = texto(nombre, Tema.T_NOTA, cTenue, false).apply { gravity = Gravity.CENTER }
            botonesNav.add(Triple(ico, etq, i))
            return LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                gravity = Gravity.CENTER
                background = ripple(Color.TRANSPARENT, Tema.R_BOTON)
                isClickable = true
                setOnClickListener { mostrarPestana(i) }
                addView(ImageView(this@MainActivity).apply { setImageDrawable(ico) },
                    LinearLayout.LayoutParams(dp(Tema.ICONO), dp(Tema.ICONO)))
                addView(etq, LinearLayout.LayoutParams(-2, -2).apply { topMargin = dp(4) })
            }
        }

        barra.addView(pestana("Inicio", Icono.INICIO, 0), LinearLayout.LayoutParams(0, -1, 1f))
        etiquetaCircular = texto("", Tema.T_NOTA, cTenue, true).apply {
            gravity = Gravity.CENTER_HORIZONTAL or Gravity.BOTTOM
            setPadding(0, 0, 0, dp(10))
        }
        barra.addView(etiquetaCircular, LinearLayout.LayoutParams(dp(112), -1))
        barra.addView(pestana("Ajustes", Icono.AJUSTES, 1), LinearLayout.LayoutParams(0, -1, 1f))
        marco.addView(barra, FrameLayout.LayoutParams(-1, dp(64), Gravity.BOTTOM))

        iconoCircular = Icono(Icono.PLAY, Color.BLACK)
        botonCircular = ImageView(this).apply {
            setImageDrawable(iconoCircular)
            setPadding(dp(20), dp(20), dp(20), dp(20))
            elevation = dp(8).toFloat()
            isClickable = true
            setOnClickListener {
                animate().scaleX(0.9f).scaleY(0.9f).setDuration(80).withEndAction {
                    animate().scaleX(1f).scaleY(1f).setDuration(120).start()
                }.start()
                alternarLectura()
            }
        }
        marco.addView(botonCircular, FrameLayout.LayoutParams(dp(64), dp(64), Gravity.TOP or Gravity.CENTER_HORIZONTAL))
        return marco
    }

    private fun circulo(color: Int) = RippleDrawable(
        ColorStateList.valueOf(Color.argb(70, 255, 255, 255)),
        GradientDrawable().apply {
            shape = GradientDrawable.OVAL
            setColor(color)
            setStroke(dp(4), cFondo)
        },
        null
    )

    private fun mostrarPestana(i: Int, animar: Boolean = true) {
        pestanas.forEachIndexed { j, v ->
            if (j == i) {
                if (animar && v.visibility != View.VISIBLE) {
                    v.alpha = 0f
                    v.animate().alpha(1f).setDuration(160).start()
                }
                v.visibility = View.VISIBLE
            } else {
                v.visibility = View.GONE
            }
        }
        botonesNav.forEach { (ico, etq, j) ->
            val sel = j == i
            ico.color = if (sel) cVerde else cTenue
            etq.setTextColor(if (sel) cVerde else cTenue)
            etq.typeface = if (sel) fuenteMedia else Typeface.DEFAULT
        }
        if (i == 0) actualizarResumen()
        if (i == 1) refrescarVistaPrevia()
    }

    // =====================================================================
    // Pestaña Inicio
    // =====================================================================

    private fun pantallaInicio(): View {
        val col = columna()

        // --- Permiso pendiente (solo si falta) ---
        val perm = tarjeta(Tema.AMARILLO_SUAVE)
        val permCab = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        permCab.addView(icono(Icono.ALERTA, cAmarillo, Tema.ICONO), LinearLayout.LayoutParams(dp(Tema.ICONO), dp(Tema.ICONO)).apply {
            rightMargin = dp(Tema.S1)
        })
        permCab.addView(texto("Falta un permiso", Tema.T_SUB, cAmarillo, true))
        perm.addView(permCab)
        perm.addView(texto(
            "Activa \"Checa – lector de ofertas\" en Accesibilidad. Si aparece bloqueado, " +
                "abre la información de la app → menú ⋮ → \"Permitir configuración restringida\".",
            Tema.T_TEXTO, cTexto, false
        ).apply { setLineSpacing(0f, 1.2f) }, margenArriba(Tema.S1))
        perm.addView(boton("Abrir Accesibilidad", cAmarillo, Color.BLACK) { abrirAccesibilidad() }, margenArriba(Tema.S2))
        perm.addView(boton("Información de la app", Color.argb(40, 255, 255, 255), cTexto) { abrirInfoApp() }, margenArriba(Tema.S1))
        tarjetaPermiso = perm
        col.addView(perm, margenTarjeta())

        // --- Licencia (solo en prueba o vencida) ---
        val lic = tarjeta(Tema.VERDE_SUAVE)
        licTitulo = texto("", Tema.T_SUB, cVerde, true)
        lic.addView(licTitulo)
        licTexto = texto("", Tema.T_TEXTO, cTexto, false).apply { setLineSpacing(0f, 1.2f) }
        lic.addView(licTexto, margenArriba(Tema.S1))
        lic.addView(boton("Activar licencia", cVerde, Color.BLACK) { verLicencia() }, margenArriba(Tema.S2))
        tarjetaLicencia = lic
        col.addView(lic, margenTarjeta())

        // --- Semáforo del día (toca para ver el detalle) ---
        col.addView(etiqueta("HOY · TOCA PARA VER EL DETALLE"))
        val filaSem = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        statVerde = texto("0", Tema.T_NUMERO, cVerde, true)
        statAmarillo = texto("0", Tema.T_NUMERO, cAmarillo, true)
        statRojo = texto("0", Tema.T_NUMERO, cRojo, true)
        listOf(
            Triple(statVerde, "Convienen", Oferta.VERDE),
            Triple(statAmarillo, "Regulares", Oferta.AMARILLO),
            Triple(statRojo, "No convienen", Oferta.ROJO)
        ).forEachIndexed { i, (v, l, n) ->
            val c = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                gravity = Gravity.CENTER_HORIZONTAL
                setPadding(dp(Tema.S1), dp(Tema.S2), dp(Tema.S1), dp(Tema.S2))
                background = ripple(cTarjeta, Tema.R_TARJETA)
                isClickable = true
                setOnClickListener { verNivel(n) }
                addView(View(this@MainActivity).apply { background = punto(Tema.colorNivel(n)) },
                    LinearLayout.LayoutParams(dp(8), dp(8)).apply { bottomMargin = dp(Tema.S1) })
                addView(v.apply { gravity = Gravity.CENTER })
                addView(texto(l, Tema.T_NOTA, cTexto2, false).apply { gravity = Gravity.CENTER },
                    LinearLayout.LayoutParams(-2, -2).apply { topMargin = dp(4) })
            }
            filaSem.addView(c, LinearLayout.LayoutParams(0, -2, 1f).apply {
                if (i > 0) leftMargin = dp(Tema.S1)
            })
        }
        col.addView(filaSem, margenTarjeta())

        // --- Mejores horas ---
        val mh = tarjeta()
        mh.addView(cabeceraTarjeta(Icono.RELOJ, "Mejores horas", "Hoy, hora por hora · últimos 30 días", false).first)
        mejoresHorasLista = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        mh.addView(mejoresHorasLista, margenArriba(12))
        col.addView(mh, margenTarjeta())

        // --- Filtros inteligentes ---
        col.addView(tarjetaFiltros(), margenTarjeta())

        return scroll(col)
    }

    /** Ícono en círculo + título + subtítulo (+ flecha). Devuelve la fila y la flecha. */
    private fun cabeceraTarjeta(tipo: Int, titulo: String, sub: String?, flechaDerecha: Boolean): Pair<LinearLayout, ImageView?> {
        val cab = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        cab.addView(iconoCirculo(tipo, true), LinearLayout.LayoutParams(dp(40), dp(40)).apply { rightMargin = dp(12) })
        val tx = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        tx.addView(texto(titulo, Tema.T_SUB, cTexto, true))
        if (sub != null) tx.addView(texto(sub, Tema.T_NOTA, cTenue, false), LinearLayout.LayoutParams(-2, -2).apply { topMargin = dp(2) })
        cab.addView(tx, LinearLayout.LayoutParams(0, -2, 1f))
        var flecha: ImageView? = null
        if (flechaDerecha) {
            flecha = icono(Icono.FLECHA, cTenue, 20).apply { rotation = -90f }
            cab.addView(flecha, LinearLayout.LayoutParams(dp(20), dp(20)))
        }
        return cab to flecha
    }

    private fun alternarLectura() {
        if (CapturaService.corriendo) {
            startService(Intent(this, CapturaService::class.java).setAction(CapturaService.ACCION_DETENER))
            handler.postDelayed({ actualizarEstado() }, 500)
        } else {
            iniciarCaptura()
        }
    }

    private fun actualizarEstado() {
        val acc = servicioActivo()
        val leyendo = CapturaService.corriendo
        tarjetaPermiso.visibility = if (acc) View.GONE else View.VISIBLE
        pintarLicencia()
        if (leyendo) {
            botonCircular.setImageDrawable(Icono(Icono.STOP, Color.WHITE).also { iconoCircular = it })
            botonCircular.background = circulo(cRojo)
            etiquetaCircular.text = "Detener"
            etiquetaCircular.setTextColor(cRojo)
        } else {
            val color = if (acc) Color.BLACK else cTenue
            botonCircular.setImageDrawable(Icono(Icono.PLAY, color).also { iconoCircular = it })
            botonCircular.background = circulo(if (acc) cVerde else cTarjeta2)
            etiquetaCircular.text = "Iniciar"
            etiquetaCircular.setTextColor(if (acc) cVerde else cTenue)
        }
        if (::ayudaPermiso.isInitialized) {
            ayudaPermiso.text = if (acc) "Servicio de Accesibilidad activado" else "Servicio de Accesibilidad desactivado"
            ayudaPermiso.setTextColor(if (acc) cVerde else cAmarillo)
            ayudaPermiso.setCompoundDrawablesRelative(
                (if (acc) Icono(Icono.OK, cVerde) else Icono(Icono.ALERTA, cAmarillo)).apply {
                    setBounds(0, 0, dp(20), dp(20))
                }, null, null, null
            )
        }
    }

    private fun actualizarResumen() {
        if (!::statVerde.isInitialized) return
        pintarMejoresHoras()
        val regs = Historial.deHoy(this)
        statVerde.text = regs.count { it.nivel == Oferta.VERDE }.toString()
        statAmarillo.text = regs.count { it.nivel == Oferta.AMARILLO }.toString()
        statRojo.text = regs.count { it.nivel == Oferta.ROJO }.toString()
    }

    private fun verNivel(nivel: Int) {
        val regs = Historial.deHoy(this).filter { it.nivel == nivel }
        val color = Tema.colorNivel(nivel)
        val titulo = when (nivel) {
            Oferta.VERDE -> "Convenían"
            Oferta.AMARILLO -> "Regulares"
            else -> "No convenían"
        }
        val fmtHora = SimpleDateFormat("HH:mm", Locale.getDefault())
        val lista = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        if (regs.isEmpty()) {
            lista.addView(texto("No hay ofertas en este grupo hoy.", Tema.T_TEXTO, cTenue, false))
        } else {
            val km = regs.sumOf { it.km }
            val min = regs.sumOf { it.min }
            val tarifa = regs.sumOf { it.tarifa }
            lista.addView(texto(
                "Promedio: ${dinero(tarifa / km)}/km · ${dinero(tarifa / min * 60, 0)}/h",
                Tema.T_TEXTO, color, true
            ))
        }
        regs.asReversed().forEach { r ->
            val fila = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(dp(Tema.S2), dp(12), dp(Tema.S2), dp(12))
                background = redondo(cTarjeta, Tema.R_BOTON)
            }
            val arriba = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
            }
            arriba.addView(texto(dinero(r.tarifa), Tema.T_SUB, cTexto, true), LinearLayout.LayoutParams(0, -2, 1f))
            arriba.addView(texto("${fmtHora.format(Date(r.hora))} · ${r.app.ifEmpty { "—" }}", Tema.T_NOTA, cTenue, false))
            fila.addView(arriba)
            fila.addView(texto("${dinero(r.porKm)}/km  ·  ${dinero(r.porHora, 0)}/h", Tema.T_TEXTO, color, true),
                margenArriba(4))
            val detalle = if (r.recogidaKm > 0)
                "Recogida ${num1(r.recogidaKm)} km · Viaje ${num1(r.km - r.recogidaKm)} km, ${(r.min - r.recogidaMin).toInt()} min"
            else
                "${num1(r.km)} km · ${r.min.toInt()} min en total"
            fila.addView(texto(detalle, Tema.T_NOTA, cTexto2, false), margenArriba(4))
            val extra = buildList {
                if (prefs.mostrarNeta && prefs.kmPorLitro > 0) {
                    val gas = r.km / prefs.kmPorLitro * prefs.precioLitro
                    add("Te quedaban ${dinero(r.tarifa - gas)}")
                }
                if (r.multiplicador > 1.0) add("⚡ ${String.format(Locale.US, "%.1f", r.multiplicador)}x")
            }
            if (extra.isNotEmpty()) fila.addView(texto(extra.joinToString("  ·  "), Tema.T_NOTA, cTexto, false), margenArriba(4))
            lista.addView(fila, margenArriba(Tema.S1))
        }
        hoja("$titulo (${regs.size})", color, lista)
    }

    // =====================================================================
    // Filtros inteligentes
    // =====================================================================

    private val repintarFiltros = ArrayList<() -> Unit>()

    private fun tarjetaFiltros(): View {
        val cont = tarjeta()

        val (cab, _) = cabeceraTarjeta(Icono.FILTROS, "Filtros inteligentes", null, false)
        val insignia = texto("", Tema.T_NOTA, cVerde, true).apply {
            setPadding(dp(12), dp(4), dp(12), dp(4))
            background = redondo(Tema.VERDE_SUAVE, 50)
        }
        cab.addView(insignia)
        cont.addView(cab)

        fun actualizarInsignia() { insignia.text = "${prefs.filtrosActivos()} activos" }
        repintarFiltros.add { actualizarInsignia() }
        actualizarInsignia()

        // La meta por hora se calcula sola: $/km × 20 (sigue siendo editable).
        var campoHora: EditText? = null
        cont.addView(filtro(
            Icono.TENDENCIA, "Por km", null, "$", "",
            { prefs.filtroKm }, { prefs.filtroKm = it },
            { prefs.minPorKm }, { v ->
                prefs.minPorKm = v
                campoHora?.setText(num(v * 20f))
                pintarMejoresHoras(false)
            }
        ), margenArriba(Tema.S1))
        cont.addView(separador())
        cont.addView(filtro(
            Icono.RELOJ, "Por hora", null, "$", "",
            { prefs.filtroHora }, { prefs.filtroHora = it },
            { prefs.minPorHora }, { prefs.minPorHora = it }
        ) { campoHora = it })
        cont.addView(separador())
        cont.addView(filtro(
            Icono.PIN, "Recogida máx.", listOf(2f, 3f, 4f, 5f), "", "km",
            { prefs.filtroRecMax }, { prefs.filtroRecMax = it },
            { prefs.recogidaMaxKm }, { prefs.recogidaMaxKm = it }
        ))
        cont.addView(separador())
        cont.addView(filtro(
            Icono.DURACION, "Duración máx.", listOf(15f, 20f, 30f), "", "min",
            { prefs.filtroDurMax }, { prefs.filtroDurMax = it },
            { prefs.duracionMax.toFloat() }, { prefs.duracionMax = it.toInt() }
        ))
        return cont
    }

    /** Fila de filtro sin desplegar: monto editable (o fichas de opciones) + interruptor. */
    private fun filtro(
        tipo: Int, titulo: String,
        valores: List<Float>?, pre: String, unidad: String,
        leerOn: () -> Boolean, ponerOn: (Boolean) -> Unit,
        leerV: () -> Float, ponerV: (Float) -> Unit,
        alCrearCampo: (EditText) -> Unit = {}
    ): View {
        val caja = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }

        val fila = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            minimumHeight = dp(Tema.ALTO_TOQUE)
        }
        val ico = iconoCirculo(tipo, true)
        fila.addView(ico, LinearLayout.LayoutParams(dp(36), dp(36)).apply { rightMargin = dp(12) })
        fila.addView(texto(titulo, Tema.T_TEXTO, cTexto, false), LinearLayout.LayoutParams(0, -2, 1f))

        var ed: EditText? = null
        if (valores == null) {
            fila.addView(texto(pre, Tema.T_SUB, cTenue, true), LinearLayout.LayoutParams(-2, -2).apply { rightMargin = dp(4) })
            val e = campo(num(leerV())) { v -> if (v > 0) ponerV(v) }.apply {
                setPadding(dp(Tema.S1), dp(4), dp(Tema.S1), dp(4))
                minimumHeight = dp(40)
                setTextColor(cVerde)
            }
            ed = e
            alCrearCampo(e)
            fila.addView(e, LinearLayout.LayoutParams(dp(88), dp(40)).apply { rightMargin = dp(Tema.S1) })
        }
        val sw = interruptorSolo(leerOn())
        fila.addView(sw)
        caja.addView(fila)

        val chipViews = ArrayList<Pair<Float, TextView>>()
        lateinit var pintar: () -> Unit
        if (valores != null) {
            val chips = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
            valores.forEachIndexed { i, v ->
                val chip = texto(pre + num(v) + " " + unidad, Tema.T_TEXTO, cTexto, true).apply {
                    gravity = Gravity.CENTER
                    minimumHeight = dp(40)
                    isClickable = true
                    setOnClickListener {
                        if (leerOn() && leerV() == v) {
                            ponerOn(false)
                        } else {
                            ponerV(v)
                            ponerOn(true)
                        }
                        pintar()
                    }
                }
                chipViews.add(v to chip)
                chips.addView(chip, LinearLayout.LayoutParams(0, dp(40), 1f).apply { if (i > 0) leftMargin = dp(Tema.S1) })
            }
            caja.addView(chips, LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(4); bottomMargin = dp(4) })
        }

        var cambiando = false
        pintar = {
            val on = leerOn()
            val v = leerV()
            if (sw.isChecked != on) { cambiando = true; sw.isChecked = on; cambiando = false }
            ico.background = punto(if (on) Tema.VERDE_SUAVE else cTarjeta2)
            ico.alpha = if (on) 1f else 0.5f
            ed?.alpha = if (on) 1f else 0.4f
            chipViews.forEach { (cv, chip) ->
                val sel = on && cv == v
                chip.background = GradientDrawable().apply {
                    cornerRadius = dp(Tema.R_BOTON).toFloat()
                    setColor(if (sel) Tema.VERDE_SUAVE else cFondo)
                    setStroke(dp(if (sel) 2 else 1), if (sel) cVerde else Tema.BORDE)
                }
                chip.setTextColor(if (sel) cVerde else cTexto)
            }
            repintarFiltros.forEach { it() }
        }

        sw.setOnCheckedChangeListener { _, b ->
            if (!cambiando) { ponerOn(b); pintar() }
        }
        pintar()
        return caja
    }

    // =====================================================================
    // Mejores horas
    // =====================================================================

    private var mhRegs: List<Registro> = emptyList()
    private val nombresDia = listOf("Lunes", "Martes", "Miércoles", "Jueves", "Viernes", "Sábado", "Domingo")

    /** Día de la semana empezando en lunes (0) hasta domingo (6). */
    private fun diaLunes(c: Calendar) = (c.get(Calendar.DAY_OF_WEEK) + 5) % 7

    /** Ofertas agrupadas por [día][hora]. */
    private fun celdas(regs: List<Registro>): Array<Array<ArrayList<Registro>>> {
        val m = Array(7) { Array(24) { ArrayList<Registro>() } }
        val cal = Calendar.getInstance()
        regs.forEach { cal.timeInMillis = it.hora; m[diaLunes(cal)][cal.get(Calendar.HOUR_OF_DAY)].add(it) }
        return m
    }

    /** $/km promedio (total cobrado ÷ km); null si hay menos de 3 ofertas. */
    private fun porKmDe(l: List<Registro>): Double? {
        if (l.size < 3) return null
        val km = l.sumOf { it.km }
        return if (km > 0) l.sumOf { it.tarifa } / km else null
    }

    private fun colorKm(v: Double?): Int {
        val meta = prefs.minPorKm.toDouble()
        return when {
            v == null -> Tema.BORDE
            v >= meta -> cVerde
            v >= meta * 0.85 -> cAmarillo
            else -> cRojo
        }
    }

    private fun estadoKm(v: Double?): String {
        val meta = prefs.minPorKm.toDouble()
        return when {
            v == null -> "Sin datos"
            v >= meta -> "Conviene"
            v >= meta * 0.85 -> "Casi"
            else -> "Abajo"
        }
    }

    private fun hh(h: Int) = String.format(Locale.US, "%02d:00", h)
    private fun rango(h: Int) = hh(h) + "–" + hh((h + 1) % 24)

    private fun cuadro(color: Int, marcado: Boolean, radio: Int) = GradientDrawable().apply {
        setColor(color)
        cornerRadius = dp(radio).toFloat()
        if (marcado) setStroke(dp(2), cTexto)
    }

    private fun cajaResumen(titulo: String, color: Int?, valor: String, sub: String): View {
        val c = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = redondo(cTarjeta2, Tema.R_BOTON)
            setPadding(dp(12), dp(10), dp(12), dp(10))
        }
        c.addView(texto(titulo, 11f, cTenue, true).apply { letterSpacing = 0.06f })
        val fila = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        if (color != null) fila.addView(View(this).apply { background = punto(color) },
            LinearLayout.LayoutParams(dp(8), dp(8)).apply { rightMargin = dp(6) })
        fila.addView(texto(valor, Tema.T_TEXTO, cTexto, true))
        c.addView(fila, margenArriba(4))
        c.addView(texto(sub, Tema.T_NOTA, cTexto2, false), margenArriba(2))
        return c
    }

    private fun leyenda(): View {
        val fila = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL }
        listOf(cVerde to "Conviene", cAmarillo to "Casi", cRojo to "Abajo", Tema.BORDE to "Sin datos").forEachIndexed { i, (c, t) ->
            fila.addView(View(this).apply { background = redondo(c, 3) }, LinearLayout.LayoutParams(dp(10), dp(10)).apply {
                if (i > 0) leftMargin = dp(12)
                rightMargin = dp(4)
            })
            fila.addView(texto(t, Tema.T_NOTA, cTexto2, false))
        }
        return fila
    }

    /** Tarjeta de Inicio: solo el día de hoy, hora por hora, con colores del semáforo según la meta por km. */
    private fun pintarMejoresHoras(recargar: Boolean = true) {
        if (!::mejoresHorasLista.isInitialized) return
        mejoresHorasLista.removeAllViews()
        if (recargar) mhRegs = Historial.ultimosDias(this, 30)
        val m = celdas(mhRegs)
        val cal = Calendar.getInstance()
        val hoy = diaLunes(cal)
        val ahora = cal.get(Calendar.HOUR_OF_DAY)
        val f = Array(24) { porKmDe(m[hoy][it]) }

        // Ahora / Lo mejor que viene
        val cajas = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        val va = f[ahora]
        cajas.addView(cajaResumen("AHORA", colorKm(va), estadoKm(va),
            if (va == null) "Aún sin ofertas" else "${dinero(va)}/km"), LinearLayout.LayoutParams(0, -1, 1f))
        var mejor = -1
        for (h in ahora + 1 until 24) {
            val v = f[h] ?: continue
            if (mejor < 0 || v > (f[mejor] ?: 0.0)) mejor = h
        }
        val vm = if (mejor >= 0) f[mejor] else null
        val caja2 = if (vm == null) cajaResumen("LO MEJOR QUE VIENE", null, "Sin datos", "Aún no hay suficientes ofertas")
            else cajaResumen("LO MEJOR QUE VIENE", colorKm(vm), hh(mejor), "${dinero(vm)}/km · en ${mejor - ahora} h")
        cajas.addView(caja2, LinearLayout.LayoutParams(0, -1, 1f).apply { leftMargin = dp(Tema.S1) })
        mejoresHorasLista.addView(cajas)

        // Las 24 horas de hoy (deslizable)
        val detalle = texto("", Tema.T_NOTA, cTexto2, false).apply { visibility = View.GONE }
        val tira = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        var celdaAhora: View? = null
        for (h in 0 until 24) {
            val v = f[h]
            val celda = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                gravity = Gravity.CENTER_HORIZONTAL
            }
            celda.addView(View(this).apply {
                background = cuadro(colorKm(v), h == ahora, 6)
                isClickable = true
                setOnClickListener {
                    detalle.visibility = View.VISIBLE
                    detalle.text = if (v == null) "${rango(h)}: aún no hay suficientes ofertas."
                        else "${rango(h)}: ${dinero(v)}/km (tu meta: ${dinero(prefs.minPorKm.toDouble())}/km)"
                }
            }, LinearLayout.LayoutParams(dp(28), dp(28)))
            celda.addView(texto(if (h == ahora) "Ahora" else "$h", 10f, if (h == ahora) cTexto else cTenue, h == ahora).apply {
                gravity = Gravity.CENTER
            }, LinearLayout.LayoutParams(-2, -2).apply { topMargin = dp(3) })
            if (h == ahora) celdaAhora = celda
            tira.addView(celda, LinearLayout.LayoutParams(-2, -2).apply { if (h > 0) leftMargin = dp(4) })
        }
        val hsv = HorizontalScrollView(this).apply {
            isHorizontalScrollBarEnabled = false
            isHorizontalFadingEdgeEnabled = true
            setFadingEdgeLength(dp(32))
            addView(tira)
        }
        mejoresHorasLista.addView(hsv, margenArriba(12))
        val ca = celdaAhora
        hsv.post { if (ca != null) hsv.scrollTo(ca.left, 0) }

        mejoresHorasLista.addView(leyenda(), margenArriba(12))
        mejoresHorasLista.addView(detalle, margenArriba(Tema.S1))
        if (mhRegs.size < 10) {
            mejoresHorasLista.addView(texto("Llevas ${mhRegs.size} ofertas guardadas. Los colores se llenan conforme uses la app.",
                Tema.T_NOTA, cTenue, false), margenArriba(Tema.S1))
        }
        mejoresHorasLista.addView(texto("Ver toda la semana ›", Tema.T_NOTA, cVerde, true).apply {
            gravity = Gravity.END or Gravity.CENTER_VERTICAL
            minHeight = dp(40)
            setPadding(dp(Tema.S1), 0, dp(4), 0)
            isClickable = true
            setOnClickListener { verSemana() }
        }, LinearLayout.LayoutParams(-2, -2).apply { gravity = Gravity.END; topMargin = 4 })
    }

    /** Ventana con toda la semana: mejor día, los 3 mejores momentos, tabla día × hora y detalle al tocar. */
    private fun verSemana() {
        val m = celdas(mhRegs)
        val cal = Calendar.getInstance()
        val hoy = diaLunes(cal)
        val ahora = cal.get(Calendar.HOUR_OF_DAY)
        val v = Array(7) { d -> Array(24) { h -> porKmDe(m[d][h]) } }
        val cuerpo = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }

        // Mejor día y día más bajo
        val prom = (0 until 7).map { d -> porKmDe(m[d].flatMap { it }) }
        val conDatos = (0 until 7).filter { prom[it] != null }
        if (conDatos.isNotEmpty()) {
            val md = conDatos.maxBy { prom[it]!! }
            val pd = conDatos.minBy { prom[it]!! }
            val caja = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                background = redondo(Tema.VERDE_SUAVE, Tema.R_BOTON)
                setPadding(dp(12), dp(10), dp(12), dp(10))
            }
            caja.addView(texto("Tu mejor día: ${nombresDia[md]} · ${dinero(prom[md]!!)}/km", Tema.T_TEXTO, cVerde, true))
            if (pd != md) caja.addView(texto("El más bajo: ${nombresDia[pd].lowercase()} (${dinero(prom[pd]!!)}/km), buen día para descansar.",
                Tema.T_NOTA, cTexto2, false), margenArriba(2))
            cuerpo.addView(caja)
        }

        // Los 3 mejores momentos
        data class Momento(val d: Int, val h: Int, val v: Double)
        val top = ArrayList<Momento>()
        for (d in 0 until 7) for (h in 0 until 24) v[d][h]?.let { top.add(Momento(d, h, it)) }
        top.sortByDescending { it.v }
        if (top.isNotEmpty()) {
            cuerpo.addView(etiqueta("LOS 3 MEJORES MOMENTOS"))
            top.take(3).forEachIndexed { i, x ->
                val fila = LinearLayout(this).apply {
                    orientation = LinearLayout.HORIZONTAL
                    gravity = Gravity.CENTER_VERTICAL
                    minimumHeight = dp(32)
                }
                fila.addView(texto("${i + 1}", Tema.T_NOTA, if (i == 0) Color.BLACK else cTexto, true).apply {
                    gravity = Gravity.CENTER
                    background = punto(if (i == 0) cVerde else cTarjeta2)
                }, LinearLayout.LayoutParams(dp(22), dp(22)).apply { rightMargin = dp(10) })
                fila.addView(texto("${nombresDia[x.d]} ${rango(x.h)}", Tema.T_TEXTO, cTexto, false), LinearLayout.LayoutParams(0, -2, 1f))
                fila.addView(texto("${dinero(x.v)}/km", Tema.T_TEXTO, cVerde, true))
                cuerpo.addView(fila)
            }
        }

        // Tabla día × hora (los días quedan fijos a la izquierda)
        cuerpo.addView(etiqueta("$/KM POR HORA · ÚLTIMOS 30 DÍAS"))
        val ficha = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = redondo(cTarjeta2, Tema.R_BOTON)
            setPadding(dp(12), dp(10), dp(12), dp(10))
        }
        ficha.addView(texto("Toca un cuadro para ver el detalle de esa hora.", Tema.T_NOTA, cTexto2, false))

        val lado = 24; val hueco = 3
        val tabla = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        val nombres = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        nombres.addView(View(this), LinearLayout.LayoutParams(dp(36), dp(16)))
        val rejilla = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        val horas = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        for (h in 0 until 24) horas.addView(texto("$h", 10f, if (h == ahora) cTexto else cTenue, h == ahora).apply {
            gravity = Gravity.CENTER
        }, LinearLayout.LayoutParams(dp(lado), dp(16)).apply { if (h > 0) leftMargin = dp(hueco) })
        rejilla.addView(horas)
        var seleccion: Pair<View, Int>? = null
        for (d in 0 until 7) {
            nombres.addView(texto(nombresDia[d].take(3), Tema.T_NOTA, if (d == hoy) cVerde else cTexto2, d == hoy).apply {
                gravity = Gravity.CENTER_VERTICAL
            }, LinearLayout.LayoutParams(dp(36), dp(lado)).apply { topMargin = dp(hueco) })
            val fila = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
            for (h in 0 until 24) {
                val valor = v[d][h]
                val col = colorKm(valor)
                fila.addView(View(this).apply {
                    background = cuadro(col, false, 5)
                    isClickable = true
                    setOnClickListener { vista ->
                        seleccion?.let { (vv, c) -> vv.background = cuadro(c, false, 5) }
                        vista.background = cuadro(col, true, 5)
                        seleccion = vista to col
                        llenarFicha(ficha, d, h, m[d][h], valor)
                    }
                }, LinearLayout.LayoutParams(dp(lado), dp(lado)).apply { if (h > 0) leftMargin = dp(hueco) })
            }
            rejilla.addView(fila, LinearLayout.LayoutParams(-2, -2).apply { topMargin = dp(hueco) })
        }
        val hsv = HorizontalScrollView(this).apply {
            isHorizontalScrollBarEnabled = false
            isHorizontalFadingEdgeEnabled = true
            setFadingEdgeLength(dp(24))
            addView(rejilla)
        }
        tabla.addView(nombres)
        tabla.addView(hsv, LinearLayout.LayoutParams(0, -2, 1f))
        cuerpo.addView(tabla)
        cuerpo.addView(leyenda(), margenArriba(12))
        cuerpo.addView(ficha, margenArriba(12))

        hoja("Toda la semana", null, cuerpo)
        hsv.post { hsv.scrollTo(maxOf(0, (ahora - 2) * dp(lado + hueco)), 0) }
    }

    private fun llenarFicha(ficha: LinearLayout, d: Int, h: Int, regs: List<Registro>, valor: Double?) {
        ficha.removeAllViews()
        ficha.addView(texto("${nombresDia[d]} ${rango(h)}", Tema.T_TEXTO, cTexto, true))
        if (valor == null) {
            ficha.addView(texto("Aún no hay suficientes ofertas a esta hora (${regs.size} de 3).", Tema.T_NOTA, cTexto2, false), margenArriba(4))
            return
        }
        val pct = regs.count { it.nivel == Oferta.VERDE } * 100 / regs.size
        val fila = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        listOf(
            "Pago por km" to "${dinero(valor)}",
            "Ofertas" to "${regs.size}",
            "Convenían" to "$pct%",
            "Tu meta" to dinero(prefs.minPorKm.toDouble())
        ).forEachIndexed { i, (t, x) ->
            val c = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
            c.addView(texto(t, 11f, cTenue, false))
            c.addView(texto(x, Tema.T_TEXTO, if (i == 0) colorKm(valor) else cTexto, true))
            fila.addView(c, LinearLayout.LayoutParams(0, -2, 1f))
        }
        ficha.addView(fila, margenArriba(Tema.S1))
        // Uber vs DiDi (solo apps con ofertas a esa hora)
        val porApp = regs.groupBy { it.app }.mapValues { (_, l) ->
            val km = l.sumOf { it.km }; if (km > 0) l.sumOf { it.tarifa } / km else 0.0
        }.filterValues { it > 0 }
        if (porApp.size >= 2) {
            val orden = porApp.entries.sortedByDescending { it.value }
            ficha.addView(texto("${orden[0].key} pagó mejor: ${dinero(orden[0].value)} vs ${dinero(orden[1].value)}/km en ${orden[1].key}.",
                Tema.T_NOTA, cTexto2, false), margenArriba(Tema.S1))
        } else if (porApp.size == 1) {
            val e = porApp.entries.first()
            ficha.addView(texto("Solo hubo ofertas de ${e.key} (${dinero(e.value)}/km).", Tema.T_NOTA, cTexto2, false), margenArriba(Tema.S1))
        }
    }

    private fun campoNumero(titulo: String, sub: String, valor: Float, sufijo: String, cambio: (Float) -> Unit): View {
        val fila = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            minimumHeight = dp(Tema.ALTO_TOQUE)
        }
        fila.addView(textosFila(titulo, sub), LinearLayout.LayoutParams(0, -2, 1f))
        fila.addView(campo(num(valor)) { v -> if (v > 0) cambio(v) }, LinearLayout.LayoutParams(dp(88), dp(Tema.ALTO_TOQUE)))
        fila.addView(texto(sufijo, Tema.T_NOTA, cTenue, false), LinearLayout.LayoutParams(dp(40), -2).apply { leftMargin = dp(Tema.S1) })
        return fila
    }

    private fun campo(valor: String, cambio: (Float) -> Unit) = EditText(this).apply {
        setText(valor)
        setTextColor(cTexto)
        setTextSize(TypedValue.COMPLEX_UNIT_SP, Tema.T_SUB)
        typeface = fuenteMedia
        gravity = Gravity.CENTER
        minimumHeight = dp(Tema.ALTO_TOQUE)
        inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL
        setPadding(dp(Tema.S2), dp(12), dp(Tema.S2), dp(12))
        background = GradientDrawable().apply {
            cornerRadius = dp(Tema.R_BOTON).toFloat()
            setColor(cFondo)
            setStroke(dp(1), Tema.BORDE)
        }
        setOnFocusChangeListener { v, f ->
            v.background = GradientDrawable().apply {
                cornerRadius = dp(Tema.R_BOTON).toFloat()
                setColor(cFondo)
                setStroke(dp(if (f) 2 else 1), if (f) cVerde else Tema.BORDE)
            }
        }
        addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(a: CharSequence?, b: Int, c: Int, d: Int) {}
            override fun onTextChanged(a: CharSequence?, b: Int, c: Int, d: Int) {}
            override fun afterTextChanged(e: Editable?) {
                val v = e?.toString()?.replace(",", ".")?.toFloatOrNull() ?: return
                cambio(v)
            }
        })
    }

    // =====================================================================
    // Pestaña Ajustes (incluye la ayuda)
    // =====================================================================

    private fun pantallaAjustes(): View {
        val col = columna()
        col.addView(texto("Ajustes", Tema.T_TITULO, cTexto, true), LinearLayout.LayoutParams(-1, -2).apply {
            bottomMargin = dp(Tema.S1)
        })

        // --- Licencia ---
        col.addView(etiqueta("LICENCIA"))
        val licA = tarjeta()
        licAjustes = texto("", Tema.T_TEXTO, cTexto, true)
        licA.addView(licAjustes)
        licA.addView(insigniaFundador(), LinearLayout.LayoutParams(-2, -2).apply { topMargin = dp(Tema.S1) })
        licA.addView(texto("Código de este equipo: ${Licencia.codigoEquipo(this)}", Tema.T_NOTA, cTenue, false), margenArriba(4))
        licA.addView(boton("Activar o renovar", cTarjeta2, cTexto) { verLicencia() }, margenArriba(Tema.S2))
        col.addView(licA, margenTarjeta())

        // --- Pop-up ---
        col.addView(etiqueta("POP-UP"))
        val pop = tarjeta()
        vistaPrevia = FrameLayout(this).apply {
            background = redondo(cFondo, Tema.R_BOTON)
            setPadding(dp(Tema.S2), dp(Tema.S2), dp(Tema.S2), dp(Tema.S2))
        }
        pop.addView(vistaPrevia)
        pop.addView(deslizador("Tamaño del texto", Prefs.TAM_MIN, Prefs.TAM_MAX, prefs.tamanoTexto, "") {
            prefs.tamanoTexto = it; refrescarVistaPrevia()
        }, margenArriba(Tema.S1))
        pop.addView(deslizador("Tiempo en pantalla", 3, 60, prefs.duracionSeg, " s") { prefs.duracionSeg = it })
        pop.addView(deslizador("Distancia desde arriba", 0, 80, prefs.posicion, " %") { prefs.posicion = it })
        pop.addView(boton("Probar en pantalla", cVerde, Color.BLACK) {
            val o = ofertaEjemplo()
            popupPrueba?.mostrar(o)
            Alertas(this, prefs).avisar(o.nivel(prefs))
        }, margenArriba(Tema.S2))
        col.addView(pop, margenTarjeta())

        // --- Gasolina ---
        col.addView(etiqueta("GASOLINA"))
        val gas = tarjeta()
        gas.addView(interruptor("Mostrar ganancia neta", "Descuenta la gasolina de recogida + viaje en el pop-up", prefs.mostrarNeta) {
            prefs.mostrarNeta = it
            refrescarVistaPrevia()
        })
        gas.addView(separador())
        gas.addView(campoNumero("Rendimiento", "Kilómetros por litro de tu auto", prefs.kmPorLitro, "km/l") {
            prefs.kmPorLitro = it
            refrescarVistaPrevia()
        })
        gas.addView(separador())
        gas.addView(campoNumero("Precio de la gasolina", "Por litro", prefs.precioLitro, "$/l") {
            prefs.precioLitro = it
            refrescarVistaPrevia()
        })
        col.addView(gas, margenTarjeta())

        // --- Comparar ofertas ---
        col.addView(etiqueta("DOS OFERTAS A LA VEZ"))
        val cmp = tarjeta()
        cmp.addView(texto(
            "Si llegan ofertas de Uber y DiDi al mismo tiempo, la mejor se marca con ⭐. " +
                "Primero gana la de mejor color; si empatan, decide este criterio:",
            Tema.T_NOTA, cTenue, false
        ).apply { setLineSpacing(0f, 1.2f) })
        val opciones = listOf(
            Triple(Comparador.NETA_HORA, "Ganancia neta por hora", "Recomendado: tarifa, tiempo y gasolina juntos"),
            Triple(Comparador.NETA_KM, "Ganancia neta por km", "Cuida más el desgaste del auto"),
            Triple(Comparador.NETA_TOTAL, "Ganancia neta total", "La que deja más dinero, aunque tarde más")
        )
        val filasOpc = ArrayList<Pair<Int, View>>()
        fun pintarOpc() = filasOpc.forEach { (v, marca) -> marca.background = radio(prefs.criterio == v) }
        opciones.forEach { (v, t, sub) ->
            val fila = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                minimumHeight = dp(56)
                background = ripple(Color.TRANSPARENT, Tema.R_BOTON)
                isClickable = true
            }
            val marca = View(this)
            fila.addView(marca, LinearLayout.LayoutParams(dp(20), dp(20)).apply { rightMargin = dp(Tema.S2) })
            fila.addView(textosFila(t, sub), LinearLayout.LayoutParams(0, -2, 1f))
            fila.setOnClickListener { prefs.criterio = v; pintarOpc() }
            filasOpc.add(v to marca)
            cmp.addView(fila, margenArriba(4))
        }
        pintarOpc()
        col.addView(cmp, margenTarjeta())

        // --- Avisos ---
        col.addView(etiqueta("AVISOS"))
        val av = tarjeta()
        av.addView(interruptor("Vibrar", "1 corta 🟢 · 2 cortas 🟡 · 1 larga 🔴", prefs.vibrar) { prefs.vibrar = it })
        av.addView(separador())
        av.addView(interruptor("Sonido", "Avisa con sonido cada oferta", prefs.sonido) { prefs.sonido = it })
        av.addView(separador())
        av.addView(interruptor("Decirlo con voz", "«Aceptar» 🟢 · «Considerar» 🟡 · «Rechazar» 🔴 (apagado: tonos)", prefs.voz) { prefs.voz = it })
        av.addView(deslizador("Volumen", 10, 100, prefs.volumen, " %") { prefs.volumen = it })
        val pruebas = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        val alertas = Alertas(this, prefs)
        listOf(Oferta.VERDE, Oferta.AMARILLO, Oferta.ROJO).forEachIndexed { i, n ->
            val b = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER
                minimumHeight = dp(Tema.ALTO_TOQUE)
                background = ripple(cTarjeta2, Tema.R_BOTON)
                isClickable = true
                setOnClickListener { alertas.avisar(n) }
                addView(View(this@MainActivity).apply { background = punto(Tema.colorNivel(n)) },
                    LinearLayout.LayoutParams(dp(10), dp(10)).apply { rightMargin = dp(Tema.S1) })
                addView(texto("Probar", Tema.T_TEXTO, cTexto, true).apply { typeface = fuenteMedia })
            }
            pruebas.addView(b, LinearLayout.LayoutParams(0, -2, 1f).apply { if (i > 0) leftMargin = dp(Tema.S1) })
        }
        av.addView(pruebas, margenArriba(Tema.S2))
        col.addView(av, margenTarjeta())

        // --- Apps ---
        col.addView(etiqueta("APPS"))
        val apps = tarjeta()
        apps.addView(interruptor("Uber Driver", null, prefs.uber) { prefs.uber = it })
        apps.addView(separador())
        apps.addView(interruptor("DiDi Conductor", null, prefs.didi) { prefs.didi = it })
        col.addView(apps, margenTarjeta())

        // --- Cómo usarla ---
        col.addView(etiqueta("CÓMO USARLA"))
        val pasos = tarjeta()
        listOf(
            "Activa el servicio de Accesibilidad (una sola vez).",
            "Toca el botón verde de abajo → elige \"Pantalla completa\" → \"Empezar\".",
            "Abre Uber o DiDi. Cuando llegue una oferta verás el pop-up con el semáforo.",
            "Al terminar, toca el botón rojo o \"Detener\" en la notificación.",
            "Después de reiniciar el teléfono hay que volver a iniciar la lectura."
        ).forEachIndexed { i, s ->
            val fila = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
            fila.addView(texto("${i + 1}", Tema.T_NOTA, cVerde, true).apply {
                gravity = Gravity.CENTER
                background = punto(Tema.VERDE_SUAVE)
            }, LinearLayout.LayoutParams(dp(24), dp(24)).apply { rightMargin = dp(12) })
            fila.addView(texto(s, Tema.T_TEXTO, cTexto, false).apply { setLineSpacing(0f, 1.2f) },
                LinearLayout.LayoutParams(0, -2, 1f).apply { topMargin = dp(2) })
            pasos.addView(fila, if (i > 0) margenArriba(12) else LinearLayout.LayoutParams(-1, -2))
        }
        col.addView(pasos, margenTarjeta())

        // --- Permisos ---
        col.addView(etiqueta("PERMISOS"))
        val perm = tarjeta()
        ayudaPermiso = texto("", Tema.T_TEXTO, cVerde, true).apply {
            gravity = Gravity.CENTER_VERTICAL
            compoundDrawablePadding = dp(Tema.S1)
        }
        perm.addView(ayudaPermiso)
        perm.addView(boton("Abrir Accesibilidad", cTarjeta2, cTexto) { abrirAccesibilidad() }, margenArriba(Tema.S2))
        perm.addView(boton("Información de la app", cTarjeta2, cTexto) { abrirInfoApp() }, margenArriba(Tema.S1))
        col.addView(perm, margenTarjeta())

        // --- Diagnóstico ---
        col.addView(etiqueta("DIAGNÓSTICO"))
        val diag = tarjeta()
        diag.addView(interruptor(
            "Guardar texto de pantalla",
            "Si una oferta se lee mal, actívalo y comparte las capturas", prefs.diagnostico
        ) { prefs.diagnostico = it })
        diag.addView(boton("Ver / compartir capturas", cTarjeta2, cTexto) { verCapturas() }, margenArriba(Tema.S2))
        diag.addView(boton("Borrar historial de ofertas", Tema.ROJO_SUAVE, cRojo) {
            AlertDialog.Builder(this)
                .setMessage("¿Borrar todo el historial de ofertas?")
                .setPositiveButton("Borrar") { _, _ -> Historial.borrar(this); actualizarResumen() }
                .setNegativeButton("Cancelar", null)
                .show()
        }, margenArriba(Tema.S1))
        col.addView(diag, margenTarjeta())

        val version = try { packageManager.getPackageInfo(packageName, 0).versionName } catch (_: Exception) { "" }
        col.addView(texto("Checa $version", Tema.T_NOTA, cTenue, false).apply {
            gravity = Gravity.CENTER
            setPadding(0, dp(Tema.S1), 0, dp(Tema.S1))
        }, LinearLayout.LayoutParams(-1, -2))
        return scroll(col)
    }

    // =====================================================================
    // Licencia
    // =====================================================================

    private fun pintarLicencia() {
        val e = Licencia.estado(this)
        tarjetaLicencia.visibility = if (e.pagada) View.GONE else View.VISIBLE
        if (e.activa && !e.pagada) {
            licTitulo.text = "Prueba gratis: ${e.diasRestantes} ${if (e.diasRestantes == 1) "día" else "días"}"
            licTitulo.setTextColor(cVerde)
            licTexto.text = "Úsala en tu turno. Para seguir después de la prueba, activa tu licencia."
        } else if (!e.activa) {
            licTitulo.text = "Tu licencia terminó"
            licTitulo.setTextColor(cAmarillo)
            licTexto.text = "Activa o renueva tu licencia para volver a ver los pop-ups."
        }
        insignias.forEach { it.visibility = if (e.fundador) View.VISIBLE else View.GONE }
        if (::licAjustes.isInitialized) {
            licAjustes.text = when {
                e.pagada -> "Activa · ${if (e.vence >= Licencia.PERMANENTE) "sin vencimiento" else "vence el " + Licencia.fechaBonita(e.vence)}"
                e.activa -> "Prueba gratis · quedan ${e.diasRestantes} días"
                else -> "Sin licencia"
            }
            licAjustes.setTextColor(if (e.activa) cVerde else cAmarillo)
        }
    }

    /** Etiqueta dorada "⭐ Miembro fundador" (solo visible con licencia de fundador). */
    private fun insigniaFundador() = texto("⭐ Miembro fundador", Tema.T_NOTA, DORADO, true).apply {
        setPadding(dp(10), dp(4), dp(10), dp(4))
        background = GradientDrawable().apply {
            cornerRadius = dp(Tema.R_BOTON).toFloat()
            setColor(Color.argb(30, 255, 214, 10))
            setStroke(dp(1), DORADO)
        }
        visibility = View.GONE
        insignias.add(this)
    }

    private fun verLicencia() {
        val codigo = Licencia.codigoEquipo(this)
        val cuerpo = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        cuerpo.addView(texto("1. Envía el código de este equipo para pagar tu licencia:", Tema.T_TEXTO, cTexto2, false)
            .apply { setLineSpacing(0f, 1.2f) })
        cuerpo.addView(texto(codigo, Tema.T_NUMERO, cVerde, true).apply {
            gravity = Gravity.CENTER
            setTextIsSelectable(true)
        }, margenArriba(Tema.S1))
        cuerpo.addView(boton("Enviar mi código", cTarjeta2, cTexto) {
            val i = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, "Hola, quiero activar Checa. Código de mi equipo: $codigo")
            }
            startActivity(Intent.createChooser(i, "Enviar código"))
        }, margenArriba(Tema.S1))
        cuerpo.addView(texto("2. Pega aquí el código de activación que te enviaron:", Tema.T_TEXTO, cTexto2, false)
            .apply { setLineSpacing(0f, 1.2f) }, margenArriba(Tema.S3))
        val campoCodigo = EditText(this).apply {
            setTextColor(cTexto)
            setHintTextColor(cTenue)
            hint = "Código de activación"
            setTextSize(TypedValue.COMPLEX_UNIT_SP, Tema.T_NOTA)
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS or
                InputType.TYPE_TEXT_FLAG_MULTI_LINE
            minimumHeight = dp(Tema.ALTO_TOQUE)
            setPadding(dp(Tema.S2), dp(12), dp(Tema.S2), dp(12))
            background = GradientDrawable().apply {
                cornerRadius = dp(Tema.R_BOTON).toFloat()
                setColor(cTarjeta)
                setStroke(dp(1), Tema.BORDE)
            }
        }
        cuerpo.addView(campoCodigo, margenArriba(Tema.S1))
        cuerpo.addView(boton("Activar", cVerde, Color.BLACK) {
            val c = Licencia.activar(this, campoCodigo.text.toString())
            if (c == null) {
                Toast.makeText(this, "Código no válido para este equipo", Toast.LENGTH_LONG).show()
            } else if (!Licencia.estado(this).pagada) {
                Toast.makeText(this, "Ese código ya venció", Toast.LENGTH_LONG).show()
            } else {
                val msg = (if (c.vence >= Licencia.PERMANENTE) "Licencia activada sin vencimiento"
                    else "Licencia activada hasta el ${Licencia.fechaBonita(c.vence)}") +
                    (if (c.fundador) "\n⭐ ¡Gracias por ser miembro fundador!" else "")
                Toast.makeText(this, msg, Toast.LENGTH_LONG).show()
                pintarLicencia()
            }
        }, margenArriba(Tema.S1))
        hoja("Licencia", cVerde, cuerpo)
    }

    private fun ofertaEjemplo() = Oferta(85.0, 7.2, 24.0, 1.5, 5.0, 1.2)

    private fun refrescarVistaPrevia() {
        if (!::vistaPrevia.isInitialized) return
        vistaPrevia.removeAllViews()
        val v = Popup(this, prefs, WindowManager.LayoutParams.TYPE_APPLICATION).construir(ofertaEjemplo(), false)
        vistaPrevia.addView(v, FrameLayout.LayoutParams(-1, -2, Gravity.CENTER))
    }

    private fun num(v: Float): String =
        if (v % 1f == 0f) v.toInt().toString() else String.format(Locale.US, "%.1f", v)

    private fun num1(v: Double): String = String.format(Locale.US, "%.1f", v)

    private fun verCapturas() {
        val caps = prefs.capturas().ifBlank { "Aún no hay capturas." }
        val vista = texto(caps, Tema.T_NOTA, cTexto, false).apply {
            setTextIsSelectable(true)
            setPadding(dp(Tema.S2), dp(12), dp(Tema.S2), dp(12))
        }
        AlertDialog.Builder(this)
            .setTitle("Capturas")
            .setView(ScrollView(this).apply { addView(vista) })
            .setPositiveButton("Compartir") { _, _ ->
                val i = Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_TEXT, caps)
                }
                startActivity(Intent.createChooser(i, "Compartir capturas"))
            }
            .setNeutralButton("Borrar") { _, _ -> prefs.borrarCapturas() }
            .setNegativeButton("Cerrar", null)
            .show()
    }

    // =====================================================================
    // Permisos y lectura de pantalla
    // =====================================================================

    private fun abrirAccesibilidad() = startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))

    private fun abrirInfoApp() = startActivity(
        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:$packageName"))
    )

    private fun servicioActivo(): Boolean {
        val activos = Settings.Secure.getString(
            contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        ) ?: return false
        val yo = ComponentName(this, RutaService::class.java)
        return activos.split(':').any { ComponentName.unflattenFromString(it) == yo }
    }

    private fun iniciarCaptura() {
        if (!Licencia.activa(this)) {
            verLicencia()
            return
        }
        if (!servicioActivo()) {
            Toast.makeText(this, "Primero activa el servicio de Accesibilidad", Toast.LENGTH_LONG).show()
            return
        }
        if (Build.VERSION.SDK_INT >= 33 &&
            checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 7)
        }
        val mpm = getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        @Suppress("DEPRECATION")
        startActivityForResult(mpm.createScreenCaptureIntent(), REQ_CAPTURA)
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        @Suppress("DEPRECATION")
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode != REQ_CAPTURA) return
        if (resultCode != RESULT_OK || data == null) {
            Toast.makeText(this, "No se dio permiso de grabar pantalla", Toast.LENGTH_SHORT).show()
            return
        }
        val i = Intent(this, CapturaService::class.java)
            .putExtra(CapturaService.EXTRA_CODIGO, resultCode)
            .putExtra(CapturaService.EXTRA_DATOS, data)
        startForegroundService(i)
        handler.postDelayed({ actualizarEstado() }, 800)
    }

    // =====================================================================
    // Componentes visuales
    // =====================================================================

    /** Ventana de detalle con el mismo estilo de la app. */
    private fun hoja(titulo: String, color: Int?, cuerpo: LinearLayout) {
        val d = Dialog(this)
        d.requestWindowFeature(Window.FEATURE_NO_TITLE)
        val raiz = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(Tema.S2), dp(Tema.S2), dp(Tema.S2), dp(Tema.S2))
        }
        val cab = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        if (color != null) {
            cab.addView(View(this).apply { background = punto(color) },
                LinearLayout.LayoutParams(dp(10), dp(10)).apply { rightMargin = dp(Tema.S1) })
        }
        cab.addView(texto(titulo, Tema.T_SUB, cTexto, true), LinearLayout.LayoutParams(0, -2, 1f))
        cab.addView(icono(Icono.CERRAR, cTenue, 20).apply {
            setPadding(dp(10), dp(10), dp(10), dp(10))
            background = ripple(Color.TRANSPARENT, 20)
            isClickable = true
            setOnClickListener { d.dismiss() }
        }, LinearLayout.LayoutParams(dp(40), dp(40)))
        raiz.addView(cab)
        raiz.addView(ScrollView(this).apply {
            isVerticalScrollBarEnabled = false
            addView(cuerpo)
        }, margenArriba(Tema.S1))
        d.setContentView(raiz)
        d.window?.setBackgroundDrawable(GradientDrawable().apply {
            setColor(cFondo)
            cornerRadius = dp(Tema.R_TARJETA).toFloat()
            setStroke(dp(1), Tema.BORDE)
        })
        d.show()
        val ancho = minOf((resources.displayMetrics.widthPixels * 0.92f).toInt(), dp(520))
        d.window?.setLayout(ancho, ViewGroup.LayoutParams.WRAP_CONTENT)
    }

    private fun redondo(color: Int, radio: Int) = GradientDrawable().apply {
        setColor(color)
        cornerRadius = dp(radio).toFloat()
    }

    private fun punto(color: Int) = GradientDrawable().apply {
        shape = GradientDrawable.OVAL
        setColor(color)
    }

    private fun radio(sel: Boolean): Drawable {
        val aro = GradientDrawable().apply {
            shape = GradientDrawable.OVAL
            setColor(Color.TRANSPARENT)
            setStroke(dp(2), if (sel) cVerde else cTenue)
        }
        if (!sel) return aro
        return LayerDrawable(arrayOf<Drawable>(aro, InsetDrawable(punto(cVerde), dp(5))))
    }

    private fun ripple(color: Int, radio: Int) =
        RippleDrawable(ColorStateList.valueOf(Color.argb(40, 255, 255, 255)), redondo(color, radio), redondo(Color.WHITE, radio))

    private fun animarCambios(v: ViewGroup) {
        v.layoutTransition = LayoutTransition().apply {
            enableTransitionType(LayoutTransition.CHANGING)
            setDuration(200)
        }
    }

    private fun columna() = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        setPadding(dp(Tema.S2), dp(Tema.S1), dp(Tema.S2), dp(Tema.S4))
        animarCambios(this)
    }

    private fun scroll(v: View) = ScrollView(this).apply {
        isFillViewport = true
        isVerticalScrollBarEnabled = false
        setBackgroundColor(cFondo)
        addView(v)
    }

    private fun tarjeta(color: Int = cTarjeta) = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        background = redondo(color, Tema.R_TARJETA)
        setPadding(dp(Tema.S2), dp(Tema.S2), dp(Tema.S2), dp(Tema.S2))
    }

    private fun margenTarjeta() = LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = dp(Tema.ENTRE_TARJETAS) }

    private fun margenArriba(v: Int) = LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(v) }

    private fun texto(s: String, tam: Float, color: Int, negrita: Boolean) = TextView(this).apply {
        text = s
        setTextColor(color)
        setTextSize(TypedValue.COMPLEX_UNIT_SP, tam)
        if (negrita) setTypeface(typeface, Typeface.BOLD)
    }

    private fun icono(tipo: Int, color: Int, @Suppress("UNUSED_PARAMETER") lado: Int) = ImageView(this).apply {
        setImageDrawable(Icono(tipo, color))
    }

    private fun iconoCirculo(tipo: Int, activo: Boolean) = ImageView(this).apply {
        setImageDrawable(Icono(tipo, cVerde))
        setPadding(dp(10), dp(10), dp(10), dp(10))
        background = punto(if (activo) Tema.VERDE_SUAVE else cTarjeta2)
    }

    private fun etiqueta(s: String) = texto(s, Tema.T_NOTA, cTenue, true).apply {
        letterSpacing = 0.08f
        setPadding(dp(4), dp(Tema.S2), 0, dp(Tema.S1))
    }

    private fun boton(s: String, fondo: Int, colorTexto: Int, accion: () -> Unit) = TextView(this).apply {
        text = s
        gravity = Gravity.CENTER
        setTextColor(colorTexto)
        setTextSize(TypedValue.COMPLEX_UNIT_SP, Tema.T_TEXTO)
        typeface = fuenteMedia
        minHeight = dp(Tema.ALTO_TOQUE)
        setPadding(dp(Tema.S2), dp(12), dp(Tema.S2), dp(12))
        background = ripple(fondo, Tema.R_BOTON)
        isClickable = true
        setOnClickListener { accion() }
    }

    private fun separador() = View(this).apply {
        setBackgroundColor(Tema.BORDE)
        layoutParams = LinearLayout.LayoutParams(-1, dp(1)).apply { topMargin = dp(Tema.S1); bottomMargin = dp(Tema.S1) }
    }

    private fun textosFila(titulo: String, sub: String?): View {
        val textos = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        textos.addView(texto(titulo, Tema.T_SUB, cTexto, false).apply { typeface = fuenteMedia })
        if (sub != null) textos.addView(texto(sub, Tema.T_NOTA, cTenue, false), LinearLayout.LayoutParams(-2, -2).apply { topMargin = dp(2) })
        return textos
    }

    private fun interruptorSolo(valor: Boolean) = Switch(this).apply {
        isChecked = valor
        val estados = arrayOf(intArrayOf(android.R.attr.state_checked), intArrayOf())
        thumbTintList = ColorStateList(estados, intArrayOf(cVerde, Color.rgb(200, 205, 202)))
        trackTintList = ColorStateList(estados, intArrayOf(Color.argb(110, 34, 197, 94), Tema.BORDE))
    }

    private fun interruptor(titulo: String, sub: String?, valor: Boolean, cambio: (Boolean) -> Unit): View {
        val fila = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            minimumHeight = dp(Tema.ALTO_TOQUE)
        }
        fila.addView(textosFila(titulo, sub), LinearLayout.LayoutParams(0, -2, 1f).apply { rightMargin = dp(Tema.S1) })
        val sw = interruptorSolo(valor).apply { setOnCheckedChangeListener { _, b -> cambio(b) } }
        fila.addView(sw)
        fila.setOnClickListener { sw.toggle() }
        return fila
    }

    private fun deslizador(
        titulo: String, min: Int, max: Int, valor: Int, unidad: String, cambio: (Int) -> Unit
    ): View {
        val caja = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(0, dp(Tema.S2), 0, 0)
        }
        val cabecera = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        cabecera.addView(texto(titulo, Tema.T_TEXTO, cTexto2, false), LinearLayout.LayoutParams(0, -2, 1f))
        val valorTxt = texto("$valor$unidad", Tema.T_TEXTO, cVerde, true)
        cabecera.addView(valorTxt)
        val barra = SeekBar(this).apply {
            this.max = max - min
            progress = (valor - min).coerceIn(0, max - min)
            progressTintList = ColorStateList.valueOf(cVerde)
            thumbTintList = ColorStateList.valueOf(cVerde)
            progressBackgroundTintList = ColorStateList.valueOf(Tema.BORDE)
            setPadding(dp(4), dp(12), dp(4), dp(12))
            setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(sb: SeekBar?, p: Int, deUsuario: Boolean) {
                    val v = p + min
                    valorTxt.text = "$v$unidad"
                    if (deUsuario) cambio(v)
                }
                override fun onStartTrackingTouch(sb: SeekBar?) {}
                override fun onStopTrackingTouch(sb: SeekBar?) {}
            })
        }
        caja.addView(cabecera)
        caja.addView(barra, LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(4) })
        return caja
    }

    private fun dinero(v: Double, dec: Int = 2) = "$" + String.format(Locale.US, "%,.${dec}f", v)

    private fun dp(v: Int): Int = (v * resources.displayMetrics.density).toInt()

    companion object {
        private const val REQ_CAPTURA = 42
    }
}
