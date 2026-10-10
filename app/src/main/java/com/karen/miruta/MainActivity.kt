package com.karen.miruta

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.RippleDrawable
import android.media.projection.MediaProjectionManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.text.Editable
import android.text.InputType
import android.text.TextWatcher
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.SeekBar
import android.widget.Switch
import android.widget.TextView
import android.widget.Toast
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Calendar
import java.util.Locale

class MainActivity : Activity() {

    // ---------- colores (tema oscuro) ----------
    private val cFondo = Color.rgb(15, 17, 21)
    private val cTarjeta = Color.rgb(26, 29, 35)
    private val cTarjeta2 = Color.rgb(37, 41, 49)
    private val cTexto = Color.rgb(242, 244, 247)
    private val cTenue = Color.rgb(154, 163, 175)
    private val cVerde = Color.rgb(34, 197, 94)
    private val cAmarillo = Color.rgb(240, 180, 0)
    private val cRojo = Color.rgb(239, 68, 68)

    private lateinit var prefs: Prefs
    private var popupPrueba: Popup? = null
    private val handler = Handler(Looper.getMainLooper())

    private lateinit var contenido: FrameLayout
    private val pestanas = ArrayList<View>()
    private val botonesNav = ArrayList<TextView>()

    // Barra
    private lateinit var botonCircular: TextView
    private lateinit var etiquetaCircular: TextView

    // Inicio
    private lateinit var tarjetaPermiso: View
    private lateinit var mejoresHorasTxt: TextView
    private lateinit var statVerde: TextView
    private lateinit var statAmarillo: TextView
    private lateinit var statRojo: TextView

    // Ajustes
    private lateinit var vistaPrevia: FrameLayout
    private lateinit var ayudaPermiso: TextView

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
            addView(barraNavegacion(), LinearLayout.LayoutParams(-1, dp(96)))
        }
        setContentView(raiz)
        mostrarPestana(savedInstanceState?.getInt("tab") ?: 0)
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
            setBackgroundColor(cTarjeta)
        }
        fun pestana(s: String, i: Int) = TextView(this).apply {
            text = s
            gravity = Gravity.CENTER
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f)
            setPadding(0, dp(8), 0, dp(8))
            background = ripple(Color.TRANSPARENT, 14)
            setOnClickListener { mostrarPestana(i) }
        }.also { botonesNav.add(it) }

        barra.addView(pestana("🏠\nInicio", 0), LinearLayout.LayoutParams(0, -2, 1f))
        etiquetaCircular = texto("", 12f, cTenue, true).apply {
            gravity = Gravity.CENTER or Gravity.BOTTOM
            setPadding(0, 0, 0, dp(8))
        }
        barra.addView(etiquetaCircular, LinearLayout.LayoutParams(dp(110), -1))
        barra.addView(pestana("⚙️\nAjustes", 1), LinearLayout.LayoutParams(0, -2, 1f))
        marco.addView(barra, FrameLayout.LayoutParams(-1, dp(72), Gravity.BOTTOM))

        botonCircular = TextView(this).apply {
            gravity = Gravity.CENTER
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 26f)
            setTypeface(typeface, Typeface.BOLD)
            elevation = dp(8).toFloat()
            isClickable = true
            setOnClickListener { alternarLectura() }
        }
        marco.addView(botonCircular, FrameLayout.LayoutParams(dp(68), dp(68), Gravity.TOP or Gravity.CENTER_HORIZONTAL))
        return marco
    }

    private fun circulo(color: Int) = RippleDrawable(
        ColorStateList.valueOf(Color.argb(70, 255, 255, 255)),
        GradientDrawable().apply {
            shape = GradientDrawable.OVAL
            setColor(color)
            setStroke(dp(5), cFondo)
        },
        null
    )

    private fun mostrarPestana(i: Int) {
        pestanas.forEachIndexed { j, v -> v.visibility = if (j == i) View.VISIBLE else View.GONE }
        botonesNav.forEachIndexed { j, b ->
            b.setTextColor(if (j == i) cVerde else cTenue)
            b.setTypeface(null, if (j == i) Typeface.BOLD else Typeface.NORMAL)
        }
        if (i == 0) actualizarResumen()
        if (i == 1) refrescarVistaPrevia()
    }

    // =====================================================================
    // Pestaña Inicio
    // =====================================================================

    private fun pantallaInicio(): View {
        val col = columna()
        col.addView(texto("Mi Ruta", 30f, cTexto, true))
        col.addView(texto("Tu calculadora de ofertas para Uber y DiDi", 14f, cTenue, false).apply {
            setPadding(0, 0, 0, dp(16))
        })

        // --- Permiso pendiente (solo si falta) ---
        val perm = tarjeta(Color.rgb(58, 42, 12))
        perm.addView(texto("⚠️  Falta un permiso", 17f, cAmarillo, true))
        perm.addView(texto(
            "Activa \"Mi Ruta – lector de ofertas\" en Accesibilidad. Si aparece bloqueado, " +
                "abre la información de la app → menú ⋮ → \"Permitir configuración restringida\".",
            14f, cTexto, false
        ).apply { setPadding(0, dp(6), 0, dp(10)) })
        perm.addView(boton("Abrir Accesibilidad", cAmarillo, Color.BLACK) { abrirAccesibilidad() })
        perm.addView(boton("Información de la app", cTarjeta2, cTexto) { abrirInfoApp() }, margenArriba(8))
        tarjetaPermiso = perm
        col.addView(perm, margenTarjeta())

        // --- Semáforo del día (toca para ver el detalle) ---
        col.addView(etiqueta("OFERTAS DE HOY · TOCA PARA VER"))
        val filaSem = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        statVerde = texto("0", 30f, cVerde, true)
        statAmarillo = texto("0", 30f, cAmarillo, true)
        statRojo = texto("0", 30f, cRojo, true)
        listOf(
            Triple(statVerde, "Convienen", Oferta.VERDE),
            Triple(statAmarillo, "Regulares", Oferta.AMARILLO),
            Triple(statRojo, "No convienen", Oferta.ROJO)
        ).forEachIndexed { i, (v, l, n) ->
            val c = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                gravity = Gravity.CENTER_HORIZONTAL
                setPadding(dp(6), dp(16), dp(6), dp(14))
                background = ripple(cTarjeta, 18)
                isClickable = true
                setOnClickListener { verNivel(n) }
                addView(v.apply { gravity = Gravity.CENTER })
                addView(texto(l, 13f, cTenue, false).apply { gravity = Gravity.CENTER })
                addView(texto("Ver ›", 12f, cTenue, true).apply {
                    gravity = Gravity.CENTER
                    setPadding(0, dp(6), 0, 0)
                })
            }
            filaSem.addView(c, LinearLayout.LayoutParams(0, -2, 1f).apply {
                if (i > 0) leftMargin = dp(8)
            })
        }
        col.addView(filaSem, margenTarjeta())

        // --- Mejores horas ---
        col.addView(etiqueta("MEJORES HORAS · ÚLTIMOS 30 DÍAS"))
        val mh = tarjeta().apply {
            background = ripple(cTarjeta, 18)
            isClickable = true
            setOnClickListener { verMejoresHoras() }
        }
        mejoresHorasTxt = texto("", 15f, cTexto, false)
        mh.addView(mejoresHorasTxt)
        mh.addView(texto("Ver por hora y por día ›", 13f, cVerde, true).apply { setPadding(0, dp(10), 0, 0) })
        col.addView(mh, margenTarjeta())

        // --- Filtros inteligentes ---
        col.addView(tarjetaFiltros(), margenTarjeta())

        return scroll(col)
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
        if (leyendo) {
            botonCircular.text = "■"
            botonCircular.setTextColor(Color.WHITE)
            botonCircular.background = circulo(cRojo)
            etiquetaCircular.text = "Detener"
            etiquetaCircular.setTextColor(cRojo)
        } else {
            botonCircular.text = "▶"
            botonCircular.setTextColor(if (acc) Color.BLACK else cTenue)
            botonCircular.background = circulo(if (acc) cVerde else cTarjeta2)
            etiquetaCircular.text = "Iniciar"
            etiquetaCircular.setTextColor(if (acc) cVerde else cTenue)
        }
        if (::ayudaPermiso.isInitialized) {
            ayudaPermiso.text = if (acc) "✅ Servicio de Accesibilidad activado" else "⚠️ Servicio de Accesibilidad desactivado"
            ayudaPermiso.setTextColor(if (acc) cVerde else cAmarillo)
        }
    }

    private fun actualizarResumen() {
        if (!::statVerde.isInitialized) return
        mejoresHorasTxt.text = resumenMejoresHoras()
        val regs = Historial.deHoy(this)
        statVerde.text = regs.count { it.nivel == Oferta.VERDE }.toString()
        statAmarillo.text = regs.count { it.nivel == Oferta.AMARILLO }.toString()
        statRojo.text = regs.count { it.nivel == Oferta.ROJO }.toString()
    }

    private fun verNivel(nivel: Int) {
        val regs = Historial.deHoy(this).filter { it.nivel == nivel }
        val (color, titulo) = when (nivel) {
            Oferta.VERDE -> cVerde to "🟢 Convenían"
            Oferta.AMARILLO -> cAmarillo to "🟡 Regulares"
            else -> cRojo to "🔴 No convenían"
        }
        val fmtHora = SimpleDateFormat("HH:mm", Locale.getDefault())
        val lista = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(8), dp(16), dp(8))
        }
        if (regs.isEmpty()) {
            lista.addView(texto("No hay ofertas en este grupo hoy.", 15f, cTenue, false))
        } else {
            val km = regs.sumOf { it.km }
            val min = regs.sumOf { it.min }
            val tarifa = regs.sumOf { it.tarifa }
            lista.addView(texto(
                "Promedio: ${dinero(tarifa / km)}/km · ${dinero(tarifa / min * 60, 0)}/h",
                14f, color, true
            ).apply { setPadding(dp(4), 0, 0, dp(4)) })
        }
        regs.asReversed().forEach { r ->
            val fila = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(dp(14), dp(10), dp(14), dp(10))
                background = redondo(cTarjeta2, 12)
            }
            val arriba = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
            arriba.addView(texto(dinero(r.tarifa), 17f, cTexto, true), LinearLayout.LayoutParams(0, -2, 1f))
            arriba.addView(texto("${fmtHora.format(Date(r.hora))} · ${r.app.ifEmpty { "—" }}", 13f, cTenue, false))
            fila.addView(arriba)
            fila.addView(texto("${dinero(r.porKm)}/km   ·   ${dinero(r.porHora, 0)}/h", 15f, color, true).apply {
                setPadding(0, dp(2), 0, dp(2))
            })
            val detalle = if (r.recogidaKm > 0)
                "Recogida ${num1(r.recogidaKm)} km · Viaje ${num1(r.km - r.recogidaKm)} km, ${(r.min - r.recogidaMin).toInt()} min"
            else
                "${num1(r.km)} km · ${r.min.toInt()} min en total"
            fila.addView(texto(detalle, 12f, cTenue, false))
            val extra = buildList {
                if (prefs.mostrarNeta && prefs.kmPorLitro > 0) {
                    val gas = r.km / prefs.kmPorLitro * prefs.precioLitro
                    add("⛽ Te quedaban ${dinero(r.tarifa - gas)}")
                }
                if (r.multiplicador > 1.0) add("⚡ ${String.format(Locale.US, "%.1f", r.multiplicador)}x")
            }
            if (extra.isNotEmpty()) fila.addView(texto(extra.joinToString("   ·   "), 12f, cTexto, false).apply {
                setPadding(0, dp(2), 0, 0)
            })
            lista.addView(fila, margenArriba(8))
        }
        AlertDialog.Builder(this)
            .setTitle("$titulo (${regs.size})")
            .setView(ScrollView(this).apply { addView(lista) })
            .setPositiveButton("Cerrar", null)
            .show()
    }

    // =====================================================================
    // Filtros inteligentes
    // =====================================================================

    private val repintarFiltros = ArrayList<() -> Unit>()

    private fun tarjetaFiltros(): View {
        val cont = tarjeta().apply { setPadding(dp(14), dp(14), dp(14), dp(6)) }

        // Encabezado: ícono · título · "N activos" · ⌃
        val cab = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(2), 0, dp(2), dp(10))
        }
        cab.addView(icono("🎛️", true), LinearLayout.LayoutParams(dp(44), dp(44)).apply { rightMargin = dp(12) })
        cab.addView(texto("Filtros inteligentes", 19f, cTexto, true), LinearLayout.LayoutParams(0, -2, 1f))
        val insignia = texto("", 13f, cVerde, true).apply {
            setPadding(dp(12), dp(5), dp(12), dp(5))
            background = GradientDrawable().apply {
                cornerRadius = dp(50).toFloat()
                setColor(Color.argb(40, 34, 197, 94))
                setStroke(dp(1), cVerde)
            }
        }
        cab.addView(insignia)
        val flecha = texto("⌃", 20f, cTenue, true).apply { setPadding(dp(14), 0, dp(4), 0) }
        cab.addView(flecha)
        cont.addView(cab)

        val lista = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        cont.addView(lista)
        cab.setOnClickListener {
            val abrir = lista.visibility != View.VISIBLE
            lista.visibility = if (abrir) View.VISIBLE else View.GONE
            flecha.text = if (abrir) "⌃" else "⌄"
        }

        fun actualizarInsignia() { insignia.text = "${prefs.filtrosActivos()} activos" }
        repintarFiltros.add { actualizarInsignia() }
        actualizarInsignia()

        lista.addView(filtro(
            "⏱️", "Por hora", "Valor mínimo por hora", null, "$", "", 10f,
            { prefs.filtroHora }, { prefs.filtroHora = it },
            { prefs.minPorHora }, { prefs.minPorHora = it }
        ))
        lista.addView(filtro(
            "📈", "Por km", "Valor mínimo por kilómetro", null, "$", "", 0.5f,
            { prefs.filtroKm }, { prefs.filtroKm = it },
            { prefs.minPorKm }, { prefs.minPorKm = it }
        ))
        lista.addView(filtro(
            "📍", "Recogida máx.", "Distancia máxima de recogida", listOf(2f, 3f, 4f, 5f), "", "km", 0.5f,
            { prefs.filtroRecMax }, { prefs.filtroRecMax = it },
            { prefs.recogidaMaxKm }, { prefs.recogidaMaxKm = it }
        ))
        lista.addView(filtro(
            "⌛", "Duración máx.", "Tiempo máximo de viaje", listOf(15f, 20f, 30f), "", "min", 5f,
            { prefs.filtroDurMax }, { prefs.filtroDurMax = it },
            { prefs.duracionMax.toFloat() }, { prefs.duracionMax = it.toInt() }
        ))
        return cont
    }

    private fun icono(emoji: String, activo: Boolean) = texto(emoji, 20f, cTexto, false).apply {
        gravity = Gravity.CENTER
        background = GradientDrawable().apply {
            shape = GradientDrawable.OVAL
            setColor(if (activo) Color.rgb(20, 61, 38) else cTarjeta2)
        }
    }

    private fun filtro(
        emoji: String, titulo: String, descripcion: String,
        valores: List<Float>?, pre: String, unidad: String, incremento: Float,
        leerOn: () -> Boolean, ponerOn: (Boolean) -> Unit,
        leerV: () -> Float, ponerV: (Float) -> Unit
    ): View {
        val caja = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        val fmt = { v: Float -> pre + num(v) + (if (unidad.isEmpty()) "" else " $unidad") }

        val cabecera = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(12), dp(12), dp(10), dp(12))
        }
        val ico = icono(emoji, true)
        cabecera.addView(ico, LinearLayout.LayoutParams(dp(48), dp(48)).apply { rightMargin = dp(14) })
        val textos = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        textos.addView(texto(titulo, 15f, cTexto, false))
        val valorTxt = texto("", 18f, cVerde, true)
        textos.addView(valorTxt)
        cabecera.addView(textos, LinearLayout.LayoutParams(0, -2, 1f))
        val pastilla = texto("", 13f, cVerde, true).apply {
            gravity = Gravity.CENTER
            setPadding(dp(12), dp(7), dp(12), dp(7))
        }
        cabecera.addView(pastilla, LinearLayout.LayoutParams(dp(58), -2))
        val flecha = texto("⌄", 20f, cTenue, true).apply { setPadding(dp(16), 0, dp(4), 0) }
        cabecera.addView(flecha)
        caja.addView(cabecera)

        // Panel desplegable
        val panel = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(14), dp(12), dp(14), dp(14))
            visibility = View.GONE
        }
        val chips = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        val chipViews = ArrayList<Pair<Float, LinearLayout>>()
        lateinit var pintar: () -> Unit

        if (valores != null) {
            panel.addView(texto("$descripcion — toca para activar/desactivar", 13f, cTenue, false))
            panel.addView(chips, margenArriba(12))
            valores.forEachIndexed { i, v ->
                val chip = LinearLayout(this).apply {
                    orientation = LinearLayout.VERTICAL
                    gravity = Gravity.CENTER
                    setPadding(0, dp(12), 0, dp(12))
                    isClickable = true
                    addView(texto(pre + num(v), 20f, cTexto, true).apply { gravity = Gravity.CENTER })
                    if (unidad.isNotEmpty()) addView(texto(unidad, 12f, cTenue, false).apply { gravity = Gravity.CENTER })
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
                chips.addView(chip, LinearLayout.LayoutParams(0, -2, 1f).apply { if (i > 0) leftMargin = dp(8) })
            }
        } else {
            panel.addView(texto(descripcion, 14f, cVerde, false))
            val campo = EditText(this).apply {
                setText(num(leerV()))
                setTextColor(cTexto)
                setHintTextColor(cTenue)
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 18f)
                inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL
                setPadding(dp(18), dp(16), dp(18), dp(16))
                background = GradientDrawable().apply {
                    cornerRadius = dp(14).toFloat()
                    setColor(Color.rgb(15, 17, 21))
                    setStroke(dp(1), Color.rgb(60, 66, 76))
                }
                addTextChangedListener(object : TextWatcher {
                    override fun beforeTextChanged(a: CharSequence?, b: Int, c: Int, d: Int) {}
                    override fun onTextChanged(a: CharSequence?, b: Int, c: Int, d: Int) {}
                    override fun afterTextChanged(e: Editable?) {
                        val v = e?.toString()?.replace(",", ".")?.toFloatOrNull() ?: return
                        ponerV(v)
                        pintar()
                    }
                })
            }
            panel.addView(campo, margenArriba(10))
        }

        caja.addView(panel)

        pintar = {
            val on = leerOn()
            val abierto = panel.visibility == View.VISIBLE
            val v = leerV()
            valorTxt.text = if (on) fmt(v) else "Desactivado"
            valorTxt.setTextColor(if (on) cVerde else cTenue)
            pastilla.text = if (on) "ON" else "OFF"
            pastilla.setTextColor(if (on) cVerde else cTenue)
            pastilla.background = GradientDrawable().apply {
                cornerRadius = dp(10).toFloat()
                setColor(if (on) Color.argb(40, 34, 197, 94) else cTarjeta2)
                setStroke(dp(1), if (on) Color.rgb(22, 128, 61) else Color.rgb(70, 76, 86))
            }
            ico.background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(if (on) Color.rgb(20, 61, 38) else cTarjeta2)
            }
            ico.alpha = if (on) 1f else 0.5f
            flecha.text = if (abierto) "⌃" else "⌄"
            flecha.setTextColor(if (abierto) cVerde else cTenue)
            val borde = when {
                abierto -> cVerde
                on -> Color.rgb(22, 90, 52)
                else -> Color.rgb(50, 55, 64)
            }
            caja.background = GradientDrawable().apply {
                cornerRadius = dp(18).toFloat()
                setColor(Color.rgb(20, 23, 29))
                setStroke(dp(if (abierto) 2 else 1), borde)
            }
            chipViews.forEach { (cv, chip) ->
                val sel = on && cv == v
                chip.background = GradientDrawable().apply {
                    cornerRadius = dp(14).toFloat()
                    setColor(if (sel) Color.rgb(20, 61, 38) else Color.rgb(15, 17, 21))
                    setStroke(dp(if (sel) 2 else 1), if (sel) cVerde else Color.rgb(60, 66, 76))
                }
                (chip.getChildAt(0) as TextView).setTextColor(if (sel) cVerde else cTexto)
            }
            repintarFiltros.forEach { it() }
        }

        cabecera.background = ripple(Color.TRANSPARENT, 18)
        cabecera.setOnClickListener {
            panel.visibility = if (panel.visibility == View.VISIBLE) View.GONE else View.VISIBLE
            pintar()
        }
        pastilla.setOnClickListener {
            ponerOn(!leerOn())
            pintar()
        }
        pintar()

        val envoltura = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        envoltura.addView(caja, LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = dp(10) })
        return envoltura
    }

    // =====================================================================
    // Mejores horas
    // =====================================================================

    private data class Grupo(val etiqueta: String, val regs: List<Registro>) {
        val porKm: Double get() { val km = regs.sumOf { it.km }; return if (km > 0) regs.sumOf { it.tarifa } / km else 0.0 }
        val porHora: Double get() { val m = regs.sumOf { it.min }; return if (m > 0) regs.sumOf { it.tarifa } / m * 60 else 0.0 }
        val pctVerde: Int get() = if (regs.isEmpty()) 0 else regs.count { it.nivel == Oferta.VERDE } * 100 / regs.size
    }

    private fun porHoraDelDia(regs: List<Registro>): List<Grupo> {
        val cal = Calendar.getInstance()
        return regs.groupBy { cal.timeInMillis = it.hora; cal.get(Calendar.HOUR_OF_DAY) }
            .toSortedMap()
            .map { (h, l) -> Grupo(String.format(Locale.US, "%02d:00–%02d:00", h, (h + 1) % 24), l) }
    }

    private fun porDiaSemana(regs: List<Registro>): List<Grupo> {
        val nombres = mapOf(
            Calendar.MONDAY to "Lunes", Calendar.TUESDAY to "Martes", Calendar.WEDNESDAY to "Miércoles",
            Calendar.THURSDAY to "Jueves", Calendar.FRIDAY to "Viernes", Calendar.SATURDAY to "Sábado",
            Calendar.SUNDAY to "Domingo"
        )
        val orden = listOf(Calendar.MONDAY, Calendar.TUESDAY, Calendar.WEDNESDAY, Calendar.THURSDAY,
            Calendar.FRIDAY, Calendar.SATURDAY, Calendar.SUNDAY)
        val cal = Calendar.getInstance()
        val g = regs.groupBy { cal.timeInMillis = it.hora; cal.get(Calendar.DAY_OF_WEEK) }
        return orden.filter { g.containsKey(it) }.map { Grupo(nombres[it]!!, g[it]!!) }
    }

    private fun resumenMejoresHoras(): String {
        val regs = Historial.ultimosDias(this, 30)
        if (regs.size < 10) return "Necesito al menos 10 ofertas para calcularlas (llevas ${regs.size})."
        val top = porHoraDelDia(regs).filter { it.regs.size >= 3 }.sortedByDescending { it.porKm }.take(3)
        if (top.isEmpty()) return "Aún no hay suficientes ofertas por hora."
        return top.mapIndexed { i, g ->
            "${listOf("🥇", "🥈", "🥉")[i]}  ${g.etiqueta}   ${dinero(g.porKm)}/km"
        }.joinToString("\n")
    }

    private fun verMejoresHoras() {
        val regs = Historial.ultimosDias(this, 30)
        val lista = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(8), dp(16), dp(8))
        }
        if (regs.isEmpty()) {
            lista.addView(texto("Aún no hay ofertas guardadas.", 15f, cTenue, false))
        } else {
            lista.addView(texto("Promedio de $/km de las ofertas que te llegaron, y cuántas convenían.", 13f, cTenue, false))
            seccionGrupos(lista, "POR HORA", porHoraDelDia(regs))
            seccionGrupos(lista, "POR DÍA", porDiaSemana(regs))
        }
        AlertDialog.Builder(this)
            .setTitle("Mejores horas (${regs.size} ofertas)")
            .setView(ScrollView(this).apply { addView(lista) })
            .setPositiveButton("Cerrar", null)
            .show()
    }

    private fun seccionGrupos(lista: LinearLayout, titulo: String, grupos: List<Grupo>) {
        lista.addView(etiqueta(titulo), margenArriba(14))
        val max = grupos.maxOfOrNull { it.porKm } ?: 1.0
        val mejor = grupos.filter { it.regs.size >= 3 }.maxByOrNull { it.porKm }
        grupos.forEach { g ->
            val fila = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(dp(12), dp(8), dp(12), dp(8))
                background = redondo(if (g === mejor) Color.rgb(20, 61, 38) else cTarjeta2, 12)
            }
            val arriba = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
            arriba.addView(texto(g.etiqueta, 14f, cTexto, true), LinearLayout.LayoutParams(0, -2, 1f))
            arriba.addView(texto("${dinero(g.porKm)}/km", 14f, cVerde, true))
            fila.addView(arriba)
            // Barra proporcional
            val barra = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
            val peso = (g.porKm / max).toFloat().coerceIn(0.03f, 1f)
            barra.addView(View(this).apply { background = redondo(cVerde, 4) }, LinearLayout.LayoutParams(0, dp(6), peso))
            barra.addView(View(this), LinearLayout.LayoutParams(0, dp(6), 1f - peso + 0.0001f))
            fila.addView(barra, margenArriba(6))
            fila.addView(texto(
                "${g.regs.size} ofertas · ${g.pctVerde}% 🟢 · ${dinero(g.porHora, 0)}/h",
                12f, cTenue, false
            ).apply { setPadding(0, dp(4), 0, 0) })
            lista.addView(fila, margenArriba(6))
        }
    }

    private fun campoNumero(titulo: String, sub: String, valor: Float, sufijo: String, cambio: (Float) -> Unit): View {
        val fila = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        val textos = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        textos.addView(texto(titulo, 16f, cTexto, true))
        textos.addView(texto(sub, 12f, cTenue, false))
        fila.addView(textos, LinearLayout.LayoutParams(0, -2, 1f))
        val campo = EditText(this).apply {
            setText(num(valor))
            setTextColor(cVerde)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 17f)
            setTypeface(typeface, Typeface.BOLD)
            gravity = Gravity.CENTER
            inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL
            setPadding(dp(8), dp(10), dp(8), dp(10))
            background = GradientDrawable().apply {
                cornerRadius = dp(12).toFloat()
                setColor(Color.rgb(15, 17, 21))
                setStroke(dp(1), Color.rgb(60, 66, 76))
            }
            addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(a: CharSequence?, b: Int, c: Int, d: Int) {}
                override fun onTextChanged(a: CharSequence?, b: Int, c: Int, d: Int) {}
                override fun afterTextChanged(e: Editable?) {
                    val v = e?.toString()?.replace(",", ".")?.toFloatOrNull() ?: return
                    if (v > 0) cambio(v)
                }
            })
        }
        fila.addView(campo, LinearLayout.LayoutParams(dp(84), -2))
        fila.addView(texto(sufijo, 13f, cTenue, false).apply { setPadding(dp(8), 0, 0, 0) })
        return fila
    }

    // =====================================================================
    // Pestaña Ajustes (incluye la ayuda)
    // =====================================================================

    private fun pantallaAjustes(): View {
        val col = columna()
        col.addView(texto("Ajustes", 30f, cTexto, true).apply { setPadding(0, 0, 0, dp(12)) })

        // --- Pop-up ---
        col.addView(etiqueta("POP-UP"))
        val pop = tarjeta()
        vistaPrevia = FrameLayout(this).apply {
            background = redondo(Color.rgb(48, 54, 64), 14)
            setPadding(dp(10), dp(16), dp(10), dp(16))
        }
        pop.addView(vistaPrevia)
        pop.addView(deslizador("Tamaño del texto", Prefs.TAM_MIN, Prefs.TAM_MAX, prefs.tamanoTexto, "") {
            prefs.tamanoTexto = it; refrescarVistaPrevia()
        })
        pop.addView(deslizador("Tiempo en pantalla", 3, 60, prefs.duracionSeg, " s") { prefs.duracionSeg = it })
        pop.addView(deslizador("Distancia desde arriba", 0, 80, prefs.posicion, " %") { prefs.posicion = it })
        pop.addView(boton("Probar en pantalla", cTarjeta2, cTexto) {
            val o = ofertaEjemplo()
            popupPrueba?.mostrar(o)
            Alertas(this, prefs).avisar(o.nivel(prefs))
        }, margenArriba(12))
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

        // --- Avisos ---
        col.addView(etiqueta("AVISOS"))
        val av = tarjeta()
        av.addView(interruptor("Vibrar", "1 corta 🟢 · 2 cortas 🟡 · 1 larga 🔴", prefs.vibrar) { prefs.vibrar = it })
        av.addView(separador())
        av.addView(interruptor("Sonido", "Un tono distinto para cada color", prefs.sonido) { prefs.sonido = it })
        av.addView(deslizador("Volumen", 10, 100, prefs.volumen, " %") { prefs.volumen = it })
        val pruebas = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        val alertas = Alertas(this, prefs)
        listOf("🟢" to Oferta.VERDE, "🟡" to Oferta.AMARILLO, "🔴" to Oferta.ROJO).forEachIndexed { i, (e, n) ->
            pruebas.addView(boton("Probar $e", cTarjeta2, cTexto) { alertas.avisar(n) },
                LinearLayout.LayoutParams(0, -2, 1f).apply { if (i > 0) leftMargin = dp(8) })
        }
        av.addView(pruebas, margenArriba(12))
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
            val fila = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; setPadding(0, dp(6), 0, dp(6)) }
            fila.addView(texto("${i + 1}", 15f, Color.BLACK, true).apply {
                gravity = Gravity.CENTER
                background = GradientDrawable().apply { shape = GradientDrawable.OVAL; setColor(cVerde) }
            }, LinearLayout.LayoutParams(dp(26), dp(26)).apply { rightMargin = dp(12) })
            fila.addView(texto(s, 14f, cTexto, false), LinearLayout.LayoutParams(0, -2, 1f))
            pasos.addView(fila)
        }
        col.addView(pasos, margenTarjeta())

        // --- Permisos ---
        col.addView(etiqueta("PERMISOS"))
        val perm = tarjeta()
        ayudaPermiso = texto("", 15f, cVerde, true)
        perm.addView(ayudaPermiso)
        perm.addView(boton("Abrir Accesibilidad", cTarjeta2, cTexto) { abrirAccesibilidad() }, margenArriba(10))
        perm.addView(boton("Información de la app", cTarjeta2, cTexto) { abrirInfoApp() }, margenArriba(8))
        col.addView(perm, margenTarjeta())

        // --- Diagnóstico ---
        col.addView(etiqueta("DIAGNÓSTICO"))
        val diag = tarjeta()
        diag.addView(interruptor(
            "Guardar texto de pantalla",
            "Si una oferta se lee mal, actívalo y comparte las capturas", prefs.diagnostico
        ) { prefs.diagnostico = it })
        diag.addView(boton("Ver / compartir capturas", cTarjeta2, cTexto) { verCapturas() }, margenArriba(10))
        diag.addView(boton("Borrar historial de ofertas", cTarjeta2, cRojo) {
            AlertDialog.Builder(this)
                .setMessage("¿Borrar todo el historial de ofertas?")
                .setPositiveButton("Borrar") { _, _ -> Historial.borrar(this); actualizarResumen() }
                .setNegativeButton("Cancelar", null)
                .show()
        }, margenArriba(8))
        col.addView(diag, margenTarjeta())

        val version = try { packageManager.getPackageInfo(packageName, 0).versionName } catch (_: Exception) { "" }
        col.addView(texto("Mi Ruta $version · uso personal", 12f, cTenue, false).apply {
            gravity = Gravity.CENTER
            setPadding(0, dp(8), 0, dp(8))
        })
        return scroll(col)
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
        val vista = texto(caps, 12f, cTexto, false).apply {
            setTextIsSelectable(true)
            setPadding(dp(16), dp(12), dp(16), dp(12))
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

    private fun redondo(color: Int, radio: Int) = GradientDrawable().apply {
        setColor(color)
        cornerRadius = dp(radio).toFloat()
    }

    private fun ripple(color: Int, radio: Int) =
        RippleDrawable(ColorStateList.valueOf(Color.argb(60, 255, 255, 255)), redondo(color, radio), redondo(Color.WHITE, radio))

    private fun columna() = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        setPadding(dp(18), dp(28), dp(18), dp(24))
    }

    private fun scroll(v: View) = ScrollView(this).apply {
        isFillViewport = true
        setBackgroundColor(cFondo)
        addView(v)
    }

    private fun tarjeta(color: Int = cTarjeta) = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        background = redondo(color, 18)
        setPadding(dp(16), dp(16), dp(16), dp(16))
    }

    private fun margenTarjeta() = LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = dp(14) }

    private fun margenArriba(v: Int) = LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(v) }

    private fun texto(s: String, tam: Float, color: Int, negrita: Boolean) = TextView(this).apply {
        text = s
        setTextColor(color)
        setTextSize(TypedValue.COMPLEX_UNIT_SP, tam)
        if (negrita) setTypeface(typeface, Typeface.BOLD)
    }

    private fun etiqueta(s: String) = texto(s, 12f, cTenue, true).apply {
        letterSpacing = 0.08f
        setPadding(dp(4), dp(6), 0, dp(8))
    }

    private fun boton(s: String, fondo: Int, colorTexto: Int, accion: () -> Unit) = TextView(this).apply {
        text = s
        gravity = Gravity.CENTER
        setTextColor(colorTexto)
        setTextSize(TypedValue.COMPLEX_UNIT_SP, 16f)
        setTypeface(typeface, Typeface.BOLD)
        setPadding(dp(12), dp(14), dp(12), dp(14))
        background = ripple(fondo, 14)
        isClickable = true
        setOnClickListener { accion() }
    }

    private fun separador() = View(this).apply {
        setBackgroundColor(cTarjeta2)
        layoutParams = LinearLayout.LayoutParams(-1, dp(1)).apply { topMargin = dp(10); bottomMargin = dp(10) }
    }

    private fun valorStat() = texto("—", 24f, cTexto, true)

    private fun statTarjeta(titulo: String, valor: TextView): View = tarjeta().apply {
        addView(texto(titulo, 12f, cTenue, false))
        addView(valor)
    }

    private fun filaStats(a: View, b: View) = LinearLayout(this).apply {
        orientation = LinearLayout.HORIZONTAL
        addView(a, LinearLayout.LayoutParams(0, -2, 1f).apply { rightMargin = dp(7) })
        addView(b, LinearLayout.LayoutParams(0, -2, 1f).apply { leftMargin = dp(7) })
        layoutParams = margenTarjeta()
    }

    private fun interruptor(titulo: String, sub: String?, valor: Boolean, cambio: (Boolean) -> Unit): View {
        val fila = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        val textos = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        textos.addView(texto(titulo, 16f, cTexto, true))
        if (sub != null) textos.addView(texto(sub, 12f, cTenue, false))
        fila.addView(textos, LinearLayout.LayoutParams(0, -2, 1f))
        val sw = Switch(this).apply {
            isChecked = valor
            val estados = arrayOf(intArrayOf(android.R.attr.state_checked), intArrayOf())
            thumbTintList = ColorStateList(estados, intArrayOf(cVerde, Color.rgb(200, 200, 200)))
            trackTintList = ColorStateList(estados, intArrayOf(Color.argb(120, 34, 197, 94), Color.rgb(80, 86, 96)))
            setOnCheckedChangeListener { _, b -> cambio(b) }
        }
        fila.addView(sw)
        fila.setOnClickListener { sw.toggle() }
        return fila
    }

    private fun paso(
        titulo: String, sub: String?, valor: Float, incremento: Float,
        formato: (Float) -> String, cambio: (Float) -> Unit
    ): View {
        var actual = valor
        val fila = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        val textos = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        textos.addView(texto(titulo, 16f, cTexto, true))
        if (sub != null) textos.addView(texto(sub, 12f, cTenue, false))
        fila.addView(textos, LinearLayout.LayoutParams(0, -2, 1f))
        val muestra = texto("", 16f, cVerde, true).apply { gravity = Gravity.CENTER }
        fun pintar() {
            muestra.text = formato(actual)
            muestra.setTextColor(if (formato(actual).startsWith("Sin")) cTenue else cVerde)
        }
        pintar()
        fun masMenos(s: String, delta: Float) = texto(s, 22f, cTexto, true).apply {
            gravity = Gravity.CENTER
            background = ripple(cTarjeta2, 22)
            isClickable = true
            setOnClickListener {
                actual = (actual + delta).coerceAtLeast(0f)
                pintar()
                cambio(actual)
            }
        }
        fila.addView(masMenos("−", -incremento), LinearLayout.LayoutParams(dp(42), dp(42)))
        fila.addView(muestra, LinearLayout.LayoutParams(dp(92), -2))
        fila.addView(masMenos("+", incremento), LinearLayout.LayoutParams(dp(42), dp(42)))
        return fila
    }

    private fun deslizador(
        titulo: String, min: Int, max: Int, valor: Int, unidad: String, cambio: (Int) -> Unit
    ): View {
        val caja = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(0, dp(12), 0, 0)
        }
        val cabecera = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        cabecera.addView(texto(titulo, 14f, cTexto, false), LinearLayout.LayoutParams(0, -2, 1f))
        val valorTxt = texto("$valor$unidad", 14f, cVerde, true)
        cabecera.addView(valorTxt)
        val barra = SeekBar(this).apply {
            this.max = max - min
            progress = (valor - min).coerceIn(0, max - min)
            progressTintList = ColorStateList.valueOf(cVerde)
            thumbTintList = ColorStateList.valueOf(cVerde)
            progressBackgroundTintList = ColorStateList.valueOf(Color.rgb(80, 86, 96))
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
