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
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.SeekBar
import android.widget.Switch
import android.widget.TextView
import android.widget.Toast
import java.text.SimpleDateFormat
import java.util.Date
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

    // Inicio
    private lateinit var estadoTitulo: TextView
    private lateinit var estadoSub: TextView
    private lateinit var estadoPunto: View
    private lateinit var botonLectura: TextView
    private lateinit var tarjetaPermiso: View
    private lateinit var statOfertas: TextView
    private lateinit var statKm: TextView
    private lateinit var statHora: TextView
    private lateinit var statMejor: TextView
    private lateinit var statVerde: TextView
    private lateinit var statAmarillo: TextView
    private lateinit var statRojo: TextView

    // Ajustes
    private lateinit var vistaPrevia: FrameLayout

    // Ayuda
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
        pestanas.add(pantallaAyuda())
        pestanas.forEach { contenido.addView(it, FrameLayout.LayoutParams(-1, -1)) }

        val raiz = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(cFondo)
            addView(contenido, LinearLayout.LayoutParams(-1, 0, 1f))
            addView(barraNavegacion(), LinearLayout.LayoutParams(-1, -2))
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
    // Navegación
    // =====================================================================

    private fun barraNavegacion(): View {
        val barra = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setBackgroundColor(cTarjeta)
            setPadding(0, dp(6), 0, dp(6))
        }
        listOf("🏠\nInicio", "⚙️\nAjustes", "❔\nAyuda").forEachIndexed { i, s ->
            val b = TextView(this).apply {
                text = s
                gravity = Gravity.CENTER
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f)
                setPadding(0, dp(6), 0, dp(6))
                background = ripple(Color.TRANSPARENT, 14)
                setOnClickListener { mostrarPestana(i) }
            }
            botonesNav.add(b)
            barra.addView(b, LinearLayout.LayoutParams(0, -2, 1f).apply { setMargins(dp(6), 0, dp(6), 0) })
        }
        return barra
    }

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
    // Pestaña 1: Inicio
    // =====================================================================

    private fun pantallaInicio(): View {
        val col = columna()
        col.addView(texto("Mi Ruta", 30f, cTexto, true))
        col.addView(texto("Tu calculadora de ofertas para Uber y DiDi", 14f, cTenue, false).apply {
            setPadding(0, 0, 0, dp(16))
        })

        // --- Estado ---
        val est = tarjeta()
        val filaEstado = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        estadoPunto = View(this)
        filaEstado.addView(estadoPunto, LinearLayout.LayoutParams(dp(14), dp(14)).apply { rightMargin = dp(12) })
        val textosEstado = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        estadoTitulo = texto("", 20f, cTexto, true)
        estadoSub = texto("", 13f, cTenue, false)
        textosEstado.addView(estadoTitulo)
        textosEstado.addView(estadoSub)
        filaEstado.addView(textosEstado, LinearLayout.LayoutParams(0, -2, 1f))
        est.addView(filaEstado)
        botonLectura = boton("", cVerde, Color.BLACK) { alternarLectura() }
        est.addView(botonLectura, margenArriba(14))
        col.addView(est, margenTarjeta())

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

        // --- Resumen de hoy ---
        col.addView(etiqueta("HOY"))
        statOfertas = valorStat()
        statKm = valorStat()
        statHora = valorStat()
        statMejor = valorStat()
        col.addView(filaStats(statTarjeta("Ofertas", statOfertas), statTarjeta("Promedio $/km", statKm)))
        col.addView(filaStats(statTarjeta("Promedio $/h", statHora), statTarjeta("Mejor $/km", statMejor)))

        val semaforo = tarjeta()
        val filaSem = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        statVerde = valorStat().apply { setTextColor(cVerde) }
        statAmarillo = valorStat().apply { setTextColor(cAmarillo) }
        statRojo = valorStat().apply { setTextColor(cRojo) }
        listOf(statVerde to "Convienen", statAmarillo to "Regulares", statRojo to "No convienen").forEach { (v, l) ->
            val c = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                gravity = Gravity.CENTER_HORIZONTAL
                addView(v.apply { gravity = Gravity.CENTER })
                addView(texto(l, 12f, cTenue, false).apply { gravity = Gravity.CENTER })
            }
            filaSem.addView(c, LinearLayout.LayoutParams(0, -2, 1f))
        }
        semaforo.addView(filaSem)
        col.addView(semaforo, margenTarjeta())

        col.addView(boton("📋  Ver ofertas de hoy", cTarjeta2, cTexto) { verHistorial() })
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
        when {
            !acc -> {
                estadoTitulo.text = "Falta activar el servicio"
                estadoSub.text = "Sigue el paso de abajo"
                punto(cAmarillo)
            }
            leyendo -> {
                estadoTitulo.text = "Leyendo ofertas"
                estadoSub.text = "Abre Uber o DiDi y espera una oferta"
                punto(cVerde)
            }
            else -> {
                estadoTitulo.text = "Lectura apagada"
                estadoSub.text = "Toca Iniciar antes de conectarte"
                punto(cTenue)
            }
        }
        if (leyendo) {
            botonLectura.text = "■  Detener lectura"
            botonLectura.background = ripple(cTarjeta2, 14)
            botonLectura.setTextColor(cRojo)
        } else {
            botonLectura.text = "▶  Iniciar lectura"
            botonLectura.background = ripple(if (acc) cVerde else cTarjeta2, 14)
            botonLectura.setTextColor(if (acc) Color.BLACK else cTenue)
        }
        if (::ayudaPermiso.isInitialized) {
            ayudaPermiso.text = if (acc) "✅ Servicio de Accesibilidad activado" else "⚠️ Servicio de Accesibilidad desactivado"
            ayudaPermiso.setTextColor(if (acc) cVerde else cAmarillo)
        }
    }

    private fun punto(color: Int) {
        estadoPunto.background = GradientDrawable().apply {
            shape = GradientDrawable.OVAL
            setColor(color)
        }
    }

    private fun actualizarResumen() {
        if (!::statOfertas.isInitialized) return
        val regs = Historial.deHoy(this)
        val km = regs.sumOf { it.km }
        val min = regs.sumOf { it.min }
        val tarifa = regs.sumOf { it.tarifa }
        statOfertas.text = regs.size.toString()
        statKm.text = if (km > 0) dinero(tarifa / km) else "—"
        statHora.text = if (min > 0) dinero(tarifa / min * 60, 0) else "—"
        statMejor.text = regs.maxByOrNull { it.porKm }?.let { dinero(it.porKm) } ?: "—"
        statVerde.text = regs.count { it.nivel == Oferta.VERDE }.toString()
        statAmarillo.text = regs.count { it.nivel == Oferta.AMARILLO }.toString()
        statRojo.text = regs.count { it.nivel == Oferta.ROJO }.toString()
    }

    private fun verHistorial() {
        val regs = Historial.deHoy(this)
        val fmtHora = SimpleDateFormat("HH:mm", Locale.getDefault())
        val lista = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(8), dp(16), dp(8))
        }
        if (regs.isEmpty()) {
            lista.addView(texto("Aún no hay ofertas hoy.", 15f, cTenue, false))
        }
        regs.asReversed().forEach { r ->
            val color = when (r.nivel) { Oferta.VERDE -> cVerde; Oferta.AMARILLO -> cAmarillo; else -> cRojo }
            val fila = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                setPadding(dp(12), dp(10), dp(12), dp(10))
                background = redondo(cTarjeta2, 12)
            }
            fila.addView(View(this).apply {
                background = GradientDrawable().apply { shape = GradientDrawable.OVAL; setColor(color) }
            }, LinearLayout.LayoutParams(dp(10), dp(10)).apply { rightMargin = dp(12) })
            val info = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
            info.addView(texto(
                "${dinero(r.tarifa)}   ·   ${dinero(r.porKm)}/km   ·   ${dinero(r.porHora, 0)}/h",
                15f, cTexto, true
            ))
            info.addView(texto(
                "${fmtHora.format(Date(r.hora))}  ·  ${r.app.ifEmpty { "—" }}  ·  " +
                    "${String.format(Locale.US, "%.1f", r.km)} km  ·  ${r.min.toInt()} min",
                12f, cTenue, false
            ))
            fila.addView(info, LinearLayout.LayoutParams(0, -2, 1f))
            lista.addView(fila, margenArriba(8))
        }
        AlertDialog.Builder(this)
            .setTitle("Ofertas de hoy (${regs.size})")
            .setView(ScrollView(this).apply { addView(lista) })
            .setPositiveButton("Cerrar", null)
            .setNeutralButton("Borrar todo") { _, _ ->
                AlertDialog.Builder(this)
                    .setMessage("¿Borrar todo el historial?")
                    .setPositiveButton("Borrar") { _, _ -> Historial.borrar(this); actualizarResumen() }
                    .setNegativeButton("Cancelar", null)
                    .show()
            }
            .show()
    }

    // =====================================================================
    // Pestaña 2: Ajustes
    // =====================================================================

    private fun pantallaAjustes(): View {
        val col = columna()
        col.addView(texto("Ajustes", 30f, cTexto, true).apply { setPadding(0, 0, 0, dp(12)) })

        // --- Mínimos ---
        col.addView(etiqueta("¿CUÁNDO CONVIENE?"))
        val min = tarjeta()
        min.addView(paso("Mínimo por km", prefs.minPorKm, 0.5f, "$", "/km") {
            prefs.minPorKm = it; refrescarVistaPrevia()
        })
        min.addView(separador())
        min.addView(paso("Mínimo por hora", prefs.minPorHora, 10f, "$", "/h") {
            prefs.minPorHora = it; refrescarVistaPrevia()
        })
        min.addView(texto("🟢 cumple los dos   🟡 cumple uno   🔴 ninguno", 13f, cTenue, false).apply {
            setPadding(0, dp(10), 0, 0)
        })
        col.addView(min, margenTarjeta())

        // --- Pop-up ---
        col.addView(etiqueta("POP-UP"))
        val pop = tarjeta()
        vistaPrevia = FrameLayout(this).apply {
            background = redondo(Color.rgb(48, 54, 64), 14)
            setPadding(dp(12), dp(16), dp(12), dp(16))
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

        return scroll(col)
    }

    private fun ofertaEjemplo() = Oferta(85.0, 7.2, 24.0)

    private fun refrescarVistaPrevia() {
        if (!::vistaPrevia.isInitialized) return
        vistaPrevia.removeAllViews()
        val v = Popup(this, prefs, WindowManager.LayoutParams.TYPE_APPLICATION).construir(ofertaEjemplo(), false)
        vistaPrevia.addView(v, FrameLayout.LayoutParams(-2, -2, Gravity.CENTER))
    }

    // =====================================================================
    // Pestaña 3: Ayuda
    // =====================================================================

    private fun pantallaAyuda(): View {
        val col = columna()
        col.addView(texto("Ayuda", 30f, cTexto, true).apply { setPadding(0, 0, 0, dp(12)) })

        col.addView(etiqueta("CÓMO USARLA"))
        val pasos = tarjeta()
        listOf(
            "Activa el servicio de Accesibilidad (una sola vez).",
            "En Inicio toca \"Iniciar lectura\" → elige \"Pantalla completa\" → \"Empezar\".",
            "Abre Uber o DiDi. Cuando llegue una oferta verás el pop-up con el semáforo.",
            "Al terminar, toca \"Detener\" aquí o en la notificación.",
            "Después de reiniciar el teléfono hay que volver a tocar \"Iniciar\"."
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

        col.addView(etiqueta("PERMISOS"))
        val perm = tarjeta()
        ayudaPermiso = texto("", 15f, cVerde, true)
        perm.addView(ayudaPermiso)
        perm.addView(boton("Abrir Accesibilidad", cTarjeta2, cTexto) { abrirAccesibilidad() }, margenArriba(10))
        perm.addView(boton("Información de la app", cTarjeta2, cTexto) { abrirInfoApp() }, margenArriba(8))
        col.addView(perm, margenTarjeta())

        col.addView(etiqueta("DIAGNÓSTICO"))
        val diag = tarjeta()
        diag.addView(interruptor(
            "Guardar texto de pantalla",
            "Si una oferta se lee mal, actívalo y comparte las capturas", prefs.diagnostico
        ) { prefs.diagnostico = it })
        diag.addView(boton("Ver / compartir capturas", cTarjeta2, cTexto) { verCapturas() }, margenArriba(10))
        col.addView(diag, margenTarjeta())

        val version = try { packageManager.getPackageInfo(packageName, 0).versionName } catch (_: Exception) { "" }
        col.addView(texto("Mi Ruta $version · uso personal", 12f, cTenue, false).apply {
            gravity = Gravity.CENTER
            setPadding(0, dp(8), 0, dp(8))
        })
        return scroll(col)
    }

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
        titulo: String, valor: Float, incremento: Float, pre: String, suf: String, cambio: (Float) -> Unit
    ): View {
        var actual = valor
        val fila = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        fila.addView(texto(titulo, 16f, cTexto, true), LinearLayout.LayoutParams(0, -2, 1f))
        val muestra = texto("", 18f, cVerde, true).apply { gravity = Gravity.CENTER }
        fun pintar() {
            muestra.text = pre + (if (actual % 1f == 0f) actual.toInt().toString() else String.format(Locale.US, "%.1f", actual)) + suf
        }
        pintar()
        fun circulo(s: String, delta: Float) = texto(s, 22f, cTexto, true).apply {
            gravity = Gravity.CENTER
            background = ripple(cTarjeta2, 22)
            isClickable = true
            setOnClickListener {
                actual = (actual + delta).coerceAtLeast(0f)
                pintar()
                cambio(actual)
            }
        }
        fila.addView(circulo("−", -incremento), LinearLayout.LayoutParams(dp(44), dp(44)))
        fila.addView(muestra, LinearLayout.LayoutParams(dp(96), -2))
        fila.addView(circulo("+", incremento), LinearLayout.LayoutParams(dp(44), dp(44)))
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
