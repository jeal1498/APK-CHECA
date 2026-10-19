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
import android.text.TextUtils
import android.text.TextWatcher
import android.util.DisplayMetrics
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
    private val DORADO = Tema.DORADO
    private val cSinDatos = Color.rgb(42, 42, 51)

    private val fuenteMedia: Typeface get() = Fuentes.media

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
    private lateinit var botonPaquetesInicio: TextView
    private lateinit var tarjetaActualizacion: View
    private lateinit var actTexto: TextView
    private lateinit var licTitulo: TextView
    private lateinit var licTexto: TextView
    private lateinit var mejoresHorasLista: LinearLayout
    private lateinit var statVerde: TextView
    private lateinit var statAmarillo: TextView
    private lateinit var statRojo: TextView
    private lateinit var hoyEtiqueta: TextView
    private lateinit var jornadaCaja: LinearLayout

    // Ajustes
    private lateinit var vistaPrevia: FrameLayout
    private lateinit var ayudaPermiso: TextView
    private lateinit var ayudaPermisoIcono: Icono
    private lateinit var cuentaCaja: LinearLayout
    private lateinit var cuentaScroll: ScrollView
    private lateinit var paquetesVista: View
    private var botonBuscarAct: TextView? = null
    private val cajasCompartir = ArrayList<LinearLayout>()
    private var cajaInvitado: LinearLayout? = null
    private var dialogoAct: Dialog? = null
    private var dialogoAviso: Dialog? = null
    private val insignias = ArrayList<TextView>()
    private var notaComparador: TextView? = null
    /** Se abrió Mercado Pago: al volver a la app se busca la licencia. */
    private var esperandoPago = false
    /** Se abrió desde el aviso «Checa no está leyendo»: iniciar la lectura y volver a Uber/DiDi. */
    private var autoIniciar = false
    private var abrirActualizacion = false
    private var desdeAviso = false
    private var volverAtras = false
    /** Pantalla de acceso: tapa la app hasta que se entra con Google. */
    private var acceso: View? = null

    private val refresco = object : Runnable {
        override fun run() {
            actualizarEstado()
            handler.postDelayed(this, 1500)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Fuentes.iniciar(this)
        prefs = Prefs(this)
        popupPrueba = Popup(this, prefs, WindowManager.LayoutParams.TYPE_APPLICATION)
        window.statusBarColor = cFondo
        window.navigationBarColor = cTarjeta

        contenido = FrameLayout(this)
        pestanas.add(pantallaInicio())
        pestanas.add(pantallaFiltros())
        pestanas.add(pantallaAjustes())
        pestanas.add(pantallaCuenta())
        pestanas.forEach { contenido.addView(it, FrameLayout.LayoutParams(-1, -1)) }

        val raiz = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(cFondo)
            clipChildren = false
            addView(contenido, LinearLayout.LayoutParams(-1, 0, 1f))
            // Barra de 56 dp + 32 dp arriba para la mitad del botón que sale de ella.
            addView(barraNavegacion(), LinearLayout.LayoutParams(-1, dp(88)))
        }
        acceso = pantallaAcceso()
        setContentView(FrameLayout(this).apply {
            addView(raiz, FrameLayout.LayoutParams(-1, -1))
            addView(acceso, FrameLayout.LayoutParams(-1, -1))
        })
        actualizarAcceso()
        mostrarPestana(savedInstanceState?.getInt("tab") ?: 0, false)
        autoIniciar = savedInstanceState == null && intent?.getBooleanExtra(EXTRA_AUTO_INICIAR, false) == true
        abrirActualizacion = savedInstanceState == null && intent?.getBooleanExtra(EXTRA_ACTUALIZAR, false) == true
        Actualizacion.programar(this)
        if (savedInstanceState == null && !autoIniciar && Cuenta.puedeUsar(this)) {
            if (faltanPermisos()) mostrarPermisos { if (!prefs.guiaVista) mostrarGuia() } else guiaSiHaceFalta()
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        if (intent.getBooleanExtra(EXTRA_AUTO_INICIAR, false)) autoIniciar = true
        if (intent.getBooleanExtra(EXTRA_ACTUALIZAR, false)) abrirActualizacion = true
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putInt("tab", pestanas.indexOfFirst { it.visibility == View.VISIBLE }.coerceAtLeast(0))
    }

    override fun onResume() {
        super.onResume()
        actualizarAcceso()
        if (pidioPermisoInstalar) {
            pidioPermisoInstalar = false
            if (Actualizacion.puedeInstalar(this)) actualizarApp()
        }
        if (Cuenta.ajustesAplicados(this)) refrescarFiltrosYAjustes()
        // Sin los 3 permisos Checa no funciona bien: la pantalla vuelve a salir hasta completarlos.
        if (dialogoPermisos?.isShowing == true) pintarPermisos()
        else if (Cuenta.puedeUsar(this) && !autoIniciar && faltanPermisos()) mostrarPermisos()
        else if (Cuenta.puedeUsar(this) && prefs.diagnostico && !prefs.diagAvisoVisto && !hayVentana())
            handler.postDelayed({ if (!isFinishing && !prefs.diagAvisoVisto && !hayVentana()) avisoDiagnostico() }, 1200)
        Guardado.borrarPendientes(this)
        Guardado.podar(this)
        pintarDiag()
        pintarAyudaAbierta?.invoke()
        pintarFallaAbierta?.invoke()
        pintarCuenta()
        pintarFilasLectura()
        actualizarEstado()
        if (autoIniciar) {
            autoIniciar = false
            if (!CapturaService.corriendo) {
                desdeAviso = true
                iniciarCaptura()
            }
        }
        // Volvió de Accesibilidad con el permiso activado: la guía pasa sola al paso de metas.
        if (guia?.isShowing == true && guiaPaso == 1) {
            if (servicioActivo()) handler.postDelayed({ if (guiaPaso == 1) pintarGuia(2) }, 600) else pintarGuia(1)
        }
        actualizarResumen()
        handler.post(refresco)
        // Espera un momento para que, si hay una versión nueva, ese aviso salga primero.
        handler.postDelayed({ avisosEnVentana() }, 1500)
        Actualizacion.revisar(this) { v, _ -> pintarActualizacion(v, true) }
        // Vino de la notificación «Hay una versión nueva»: la ventana sale aunque haya otra abierta.
        if (abrirActualizacion) {
            abrirActualizacion = false
            Actualizacion.pendiente(this)?.let { v ->
                if (dialogoAct?.isShowing != true) { Actualizacion.marcarAviso(this, v); dialogoActualizacion(v) }
            }
        }
        if (esperandoPago && Cuenta.conectada(this)) {
            Cuenta.revisarAhora(this) { r ->
                if (r is Cuenta.Resultado.Ok && Cuenta.licencia(this) == null) {
                    pintarLicencia()
                    pintarCuenta()
                    Toast.makeText(this, "Si ya pagaste, tu licencia aparece en unos minutos. Toca «Ya pagué» en Cuenta.",
                        Toast.LENGTH_LONG).show()
                } else {
                    esperandoPago = false
                    mostrarResultadoCuenta(r)
                }
            }
            return
        }
        Cuenta.revisarSiToca(this) { r ->
            if (Cuenta.ajustesAplicados(this)) refrescarFiltrosYAjustes()
            pintarLicencia()
            pintarCuenta()
            if (r is Cuenta.Resultado.Limite) dialogoLimite(r.equipos)
        }
    }

    override fun onPause() {
        handler.removeCallbacks(refresco)
        popupPrueba?.ocultar()
        Cuenta.subirAjustesSiCambio(this)
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
            background = LayerDrawable(arrayOf<Drawable>(
                redondo(Tema.BORDE, 0),
                InsetDrawable(redondo(cTarjeta, 0), 0, dp(1), 0, 0)
            ))
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
        barra.addView(pestana("Mis metas", Icono.FILTROS, 1), LinearLayout.LayoutParams(0, -1, 1f))
        etiquetaCircular = texto("", Tema.T_NOTA, cTenue, true).apply {
            gravity = Gravity.CENTER_HORIZONTAL or Gravity.BOTTOM
            setPadding(0, 0, 0, dp(6))
        }
        barra.addView(etiquetaCircular, LinearLayout.LayoutParams(dp(88), -1))
        barra.addView(pestana("Ajustes", Icono.AJUSTES, 2), LinearLayout.LayoutParams(0, -1, 1f))
        barra.addView(pestana("Cuenta", Icono.CUENTA, 3), LinearLayout.LayoutParams(0, -1, 1f))
        marco.addView(barra, FrameLayout.LayoutParams(-1, dp(56), Gravity.BOTTOM))

        iconoCircular = Icono(Icono.PLAY, Tema.ASFALTO)
        botonCircular = ImageView(this).apply {
            setImageDrawable(iconoCircular)
            setPadding(dp(20), dp(20), dp(20), dp(20))
            isClickable = true
            setOnClickListener {
                animate().scaleX(0.9f).scaleY(0.9f).setDuration(80).withEndAction {
                    animate().scaleX(1f).scaleY(1f).setDuration(120).start()
                }.start()
                alternarLectura()
            }
        }
        // Centro del botón sobre la orilla de la barra: la mitad queda afuera (sin aro ni sombra).
        marco.addView(botonCircular, FrameLayout.LayoutParams(dp(64), dp(64), Gravity.TOP or Gravity.CENTER_HORIZONTAL))
        return marco
    }

    private fun circulo(color: Int) = RippleDrawable(
        ColorStateList.valueOf(Color.argb(70, 255, 255, 255)),
        GradientDrawable().apply {
            shape = GradientDrawable.OVAL
            setColor(color)
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
            ico.color = if (sel) Tema.HUESO else cTenue
            etq.setTextColor(if (sel) Tema.HUESO else cTenue)
            etq.typeface = if (sel) fuenteMedia else Fuentes.texto
        }
        if (i == 0) actualizarResumen()
        if (i == 2) refrescarVistaPrevia()
    }

    // =====================================================================
    // Pestaña Inicio
    // =====================================================================

    private fun pantallaInicio(): View {
        val col = columna()

        // --- Marca y estado de la lectura ---
        val marca = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL }
        marca.addView(ImageView(this).apply { setImageResource(R.drawable.semaforo) },
            LinearLayout.LayoutParams(dp(15), dp(25)).apply { rightMargin = dp(10) })
        marca.addView(texto("Checa", 24f, cTexto, true))
        col.addView(marca, LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = dp(12); topMargin = dp(4) })
        val estado = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(14), dp(12), dp(12), dp(12))
            minimumHeight = dp(56)
            isClickable = true
        }
        val ePunto = View(this)
        estado.addView(ePunto, LinearLayout.LayoutParams(dp(10), dp(10)).apply { rightMargin = dp(12) })
        val eTextos = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        val eTitulo = texto("", Tema.T_TEXTO, cTexto, true)
        val eSub = texto("", Tema.T_NOTA, cTenue, false)
        eTextos.addView(eTitulo)
        eTextos.addView(eSub, LinearLayout.LayoutParams(-2, -2).apply { topMargin = dp(2) })
        estado.addView(eTextos, LinearLayout.LayoutParams(0, -2, 1f))
        val eChip = texto("", Tema.T_NOTA, Tema.ASFALTO, true).apply {
            setPadding(dp(12), dp(6), dp(12), dp(6))
            background = redondo(Tema.HUESO, 50)
        }
        estado.addView(eChip)
        estadoInicio = EstadoInicio(estado, ePunto, eTitulo, eSub, eChip)
        col.addView(estado, margenTarjeta())
        // Los permisos ya son obligatorios: la tarjeta «Falta un permiso» ya no se muestra.
        tarjetaPermiso = View(this)

        // --- Versión nueva (solo si hay) ---
        val act = tarjeta()
        act.addView(cabeceraTarjeta(Icono.DESCARGA, "Hay una versión nueva", null, false).first)
        actTexto = texto("", Tema.T_TEXTO, cTexto, false).apply { setLineSpacing(0f, 1.2f) }
        act.addView(actTexto, margenArriba(Tema.S1))
        act.addView(boton("Actualizar ahora", cVerde, Color.BLACK) { actualizarApp() }, margenArriba(Tema.S2))
        act.visibility = View.GONE
        tarjetaActualizacion = act
        col.addView(act, margenTarjeta())

        // --- Licencia (solo en prueba o vencida) ---
        val lic = tarjeta()
        licTitulo = texto("", Tema.T_SUB, cTexto, true)
        lic.addView(licTitulo)
        licTexto = texto("", Tema.T_TEXTO, cTexto, false).apply { setLineSpacing(0f, 1.2f) }
        lic.addView(licTexto, margenArriba(Tema.S1))
        botonPaquetesInicio = boton("Ver paquetes", cVerde, Color.BLACK) { irAPaquetes() }
        lic.addView(botonPaquetesInicio, margenArriba(Tema.S2))
        tarjetaLicencia = lic
        col.addView(lic, margenTarjeta())

        // --- Mi día: lo que te queda hoy contra tu meta ---
        col.addView(etiqueta("MI DÍA"))
        jornadaCaja = tarjeta()
        col.addView(jornadaCaja, margenTarjeta())
        pintarJornada()

        // --- Ofertas de hoy: tres cuadros con el conteo de cada color (toca para ver el detalle) ---
        hoyEtiqueta = etiqueta("OFERTAS DE HOY")
        col.addView(hoyEtiqueta)
        val filaSem = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        statVerde = texto("0", 26f, cVerde, true)
        statAmarillo = texto("0", 26f, cAmarillo, true)
        statRojo = texto("0", 26f, cRojo, true)
        listOf(
            Triple(statVerde, "conviene", Oferta.VERDE),
            Triple(statAmarillo, "regular", Oferta.AMARILLO),
            Triple(statRojo, "no conviene", Oferta.ROJO)
        ).forEachIndexed { i, (v, l, n) ->
            val c = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                gravity = Gravity.CENTER_HORIZONTAL
                setPadding(dp(6), dp(12), dp(6), dp(12))
                background = ripple(cTarjeta, 18)
                isClickable = true
                setOnClickListener { verNivel(n) }
                addView(v.apply { gravity = Gravity.CENTER })
                addView(texto(l, Tema.T_NOTA, cTenue, false).apply { gravity = Gravity.CENTER },
                    LinearLayout.LayoutParams(-2, -2).apply { topMargin = dp(2) })
            }
            filaSem.addView(c, LinearLayout.LayoutParams(0, -2, 1f).apply {
                if (i > 0) leftMargin = dp(Tema.S1)
            })
        }
        col.addView(filaSem, margenTarjeta())

        // --- Mejores horas: las próximas horas en una tira y lo mejor que viene ---
        val mh = tarjeta().apply {
            background = ripple(cTarjeta, Tema.R_TARJETA)
            isClickable = true
            setOnClickListener { if (Licencia.basico(this@MainActivity)) irAPaquetes() else verSemana() }
        }
        val mhCab = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL }
        mhCab.addView(texto("Mejores horas", Tema.T_SUB, cTexto, true), LinearLayout.LayoutParams(0, -2, 1f))
        mhCab.addView(texto("›", 20f, cTenue, false))
        mh.addView(mhCab)
        mejoresHorasLista = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        mh.addView(mejoresHorasLista, margenArriba(10))
        col.addView(mh, margenTarjeta())

        col.addView(tarjetaCompartir(), margenTarjeta())

        return scroll(col)
    }

    // =====================================================================
    // Pestaña Filtros
    // =====================================================================

    private fun pantallaFiltros(): View {
        val col = columna()
        col.addView(texto("Mis metas", 26f, cTexto, true), LinearLayout.LayoutParams(-1, -2).apply {
            bottomMargin = dp(Tema.S1)
        })
        col.addView(texto(
            "Lo que decide si una oferta es verde, amarilla o roja. Verde si cumple todas tus metas activas, " +
                "amarilla si cumple alguna y roja si no cumple ninguna o falla la recogida o la duración.",
            Tema.T_NOTA, cTenue, false
        ).apply { setLineSpacing(0f, 1.2f) }, LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = dp(Tema.S2) })
        tarjetaFiltros(col)

        // --- Tu auto ---
        val gas = tarjeta()
        gas.addView(etiquetaTarjeta("TU AUTO"))
        gas.addView(campoDato("Rendimiento", prefs.kmPorLitro, "", " km/l") {
            prefs.kmPorLitro = it
            refrescarVistaPrevia()
        }, margenArriba(Tema.S1))
        gas.addView(campoDato("Gasolina", prefs.precioLitro, "$", "/l") {
            prefs.precioLitro = it
            refrescarVistaPrevia()
        }, margenArriba(Tema.S1))
        gas.addView(interruptor("Mostrar lo que te queda", "Descuenta la gasolina en el pop-up", prefs.mostrarNeta) {
            prefs.mostrarNeta = it
            refrescarVistaPrevia()
        }, margenArriba(4))
        col.addView(gas, margenTarjeta())

        // --- Dos ofertas a la vez ---
        val cmp = tarjeta()
        cmp.addView(etiquetaTarjeta("DOS OFERTAS A LA VEZ"))
        notaComparador = texto("Disponible en los paquetes de 6 meses y 1 año. Con el de 1 mes se muestra una oferta a la vez.",
            Tema.T_NOTA, cAmarillo, false).apply { setLineSpacing(0f, 1.2f) }
        cmp.addView(notaComparador, margenArriba(Tema.S1))
        cmp.addView(texto(
            "Si llegan ofertas de Uber y DiDi al mismo tiempo, la mejor se marca con ⭐. " +
                "Primero gana la de mejor color; si empatan, gana la que deja:",
            Tema.T_NOTA, cTenue, false
        ).apply { setLineSpacing(0f, 1.2f) }, margenArriba(Tema.S1))
        val criterios = listOf(Comparador.NETA_HORA, Comparador.NETA_KM, Comparador.NETA_TOTAL)
        cmp.addView(pildoras(listOf("Más por hora", "Más por km", "Más en total"), { criterios.indexOf(prefs.criterio) }) { i ->
            prefs.criterio = criterios[i]
        }, margenArriba(12))
        col.addView(cmp, margenTarjeta())

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

    private fun pausarLector() {
        val srv = RutaService.instancia
        if (srv == null) {
            Toast.makeText(this, "El lector ya está apagado.", Toast.LENGTH_SHORT).show()
            return
        }
        if (CapturaService.corriendo) {
            startService(Intent(this, CapturaService::class.java).setAction(CapturaService.ACCION_DETENER))
        }
        srv.pausar()
        prefs.lectorPausado = true
        Toast.makeText(this, "Lector pausado. Ya puedes abrir tu banco.", Toast.LENGTH_LONG).show()
        handler.postDelayed({ actualizarEstado() }, 600)
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
        pintarLicencia()
        pintarEstadoInicio()
        if (leyendo) {
            botonCircular.setImageDrawable(Icono(Icono.STOP, Color.WHITE).also { iconoCircular = it })
            botonCircular.background = circulo(cRojo)
            etiquetaCircular.text = "Detener"
            etiquetaCircular.setTextColor(cRojo)
        } else {
            botonCircular.setImageDrawable(Icono(Icono.PLAY, Tema.ASFALTO).also { iconoCircular = it })
            botonCircular.background = circulo(Tema.HUESO)
            etiquetaCircular.text = "Iniciar"
            etiquetaCircular.setTextColor(Tema.HUESO)
        }
        pintarEstadoLectura()
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
        pintarJornada()
        val regs = Historial.deHoy(this)
        statVerde.text = regs.count { it.nivel == Oferta.VERDE }.toString()
        statAmarillo.text = regs.count { it.nivel == Oferta.AMARILLO }.toString()
        statRojo.text = regs.count { it.nivel == Oferta.ROJO }.toString()
        hoyEtiqueta.text = if (regs.isEmpty()) "OFERTAS DE HOY · AÚN NINGUNA" else "OFERTAS DE HOY · ${regs.size}"
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

    /** Una sola tarjeta «Qué te conviene» (maqueta v2): metas por km y por hora, y píldoras de recogida y duración. */
    private fun tarjetaFiltros(col: LinearLayout) {
        val t = tarjeta()
        t.addView(etiquetaTarjeta("QUÉ TE CONVIENE"))
        // La meta por hora se calcula sola: $/km × 20 (sigue siendo editable).
        var pintarHora: () -> Unit = {}
        t.addView(campoMeta(
            "Por km", "$", "/km", 0.5f,
            { prefs.filtroKm }, { prefs.filtroKm = it },
            { prefs.minPorKm }, { v ->
                prefs.minPorKm = v
                prefs.minPorHora = v * 20f
                pintarHora()
                pintarMejoresHoras(false)
            }
        ), margenArriba(Tema.S1))
        t.addView(campoMeta(
            "Por hora", "$", "/h", 10f,
            { prefs.filtroHora }, { prefs.filtroHora = it },
            { prefs.minPorHora }, { prefs.minPorHora = it }
        ) { pintarHora = it }, margenArriba(Tema.S1))

        // Tocar la opción elegida otra vez apaga el filtro.
        t.addView(texto("Recogida máxima", Tema.T_TEXTO, cTexto2, false), margenArriba(Tema.S2))
        val rec = listOf(2f, 3f, 4f)
        t.addView(pildoras(rec.map { num(it) + " km" }, { if (prefs.filtroRecMax) rec.indexOf(prefs.recogidaMaxKm) else -1 }) { i ->
            if (prefs.filtroRecMax && prefs.recogidaMaxKm == rec[i]) {
                prefs.filtroRecMax = false
            } else {
                prefs.recogidaMaxKm = rec[i]
                prefs.filtroRecMax = true
            }
        }, margenArriba(Tema.S1))
        t.addView(texto("Duración máxima", Tema.T_TEXTO, cTexto2, false), margenArriba(Tema.S2))
        val dur = listOf(15, 20, 30)
        t.addView(pildoras(dur.map { "$it min" }, { if (prefs.filtroDurMax) dur.indexOf(prefs.duracionMax) else -1 }) { i ->
            if (prefs.filtroDurMax && prefs.duracionMax == dur[i]) {
                prefs.filtroDurMax = false
            } else {
                prefs.duracionMax = dur[i]
                prefs.filtroDurMax = true
            }
        }, margenArriba(Tema.S1))
        t.addView(texto("Toca otra vez la opción elegida para apagarla.", Tema.T_NOTA, cTenue, false), margenArriba(Tema.S1))
        col.addView(t, margenTarjeta())
    }

    /** Valor dentro de un campo: nombre, − valor + (mantener presionado para avanzar rápido) e interruptor si [leerOn] no es null. */
    private fun campoMeta(
        titulo: String, pre: String, unidad: String, paso: Float,
        leerOn: (() -> Boolean)?, ponerOn: ((Boolean) -> Unit)?,
        leerV: () -> Float, ponerV: (Float) -> Unit,
        min: Float = paso, max: Float = Float.MAX_VALUE,
        alCrearPintor: (() -> Unit) -> Unit = {}
    ): View {
        val fila = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(12), dp(2), dp(4), dp(2))
            minimumHeight = dp(Tema.ALTO_TOQUE)
            background = redondo(Tema.CAMPO, 14)
        }
        fila.addView(texto(titulo, Tema.T_TEXTO, cTexto2, false).apply { maxLines = 1 }, LinearLayout.LayoutParams(0, -2, 1f))
        val vt = texto("", Tema.T_TEXTO, cTexto, true).apply {
            typeface = Fuentes.numero
            gravity = Gravity.CENTER
            maxLines = 1
        }
        lateinit var pintar: () -> Unit
        fun cambiar(signo: Int) {
            val nuevo = Math.round((leerV() + signo * paso) * 100f) / 100f
            if (nuevo in min..max) { ponerV(nuevo); pintar() }
        }
        val teclas = ArrayList<View>()
        fun tecla(sim: String, signo: Int) = texto(sim, 20f, cTexto, false).apply {
            typeface = Fuentes.numero
            gravity = Gravity.CENTER
            background = ripple(Color.TRANSPARENT, 20)
            isClickable = true
            setOnClickListener { cambiar(signo) }
            val repetir = object : Runnable {
                override fun run() { cambiar(signo); this@MainActivity.handler.postDelayed(this, 90) }
            }
            setOnLongClickListener { this@MainActivity.handler.post(repetir); true }
            setOnTouchListener { _, ev ->
                if (ev.action == android.view.MotionEvent.ACTION_UP || ev.action == android.view.MotionEvent.ACTION_CANCEL) {
                    this@MainActivity.handler.removeCallbacks(repetir)
                }
                false
            }
            teclas.add(this)
        }
        fila.addView(tecla("−", -1), LinearLayout.LayoutParams(dp(36), dp(40)))
        fila.addView(vt, LinearLayout.LayoutParams(dp(76), -2))
        fila.addView(tecla("+", 1), LinearLayout.LayoutParams(dp(36), dp(40)))
        val sw = if (leerOn != null) interruptorSolo(leerOn()) else null
        if (sw != null) fila.addView(sw, LinearLayout.LayoutParams(-2, -2).apply { leftMargin = dp(4) })

        var cambiando = false
        pintar = {
            val on = leerOn?.invoke() ?: true
            if (sw != null && sw.isChecked != on) { cambiando = true; sw.isChecked = on; cambiando = false }
            vt.text = pre + num(leerV()) + unidad
            vt.setTextColor(if (on) cTexto else cTenue)
            teclas.forEach { it.alpha = if (on) 1f else 0.5f }
        }
        alCrearPintor(pintar)
        sw?.setOnCheckedChangeListener { _, b ->
            if (!cambiando) { ponerOn?.invoke(b); pintar() }
        }
        pintar()
        return fila
    }

    /** Píldoras del mismo ancho, repartidas a lo ancho (marca v2): la elegida en hueso. [elegida] da el índice o −1. */
    private fun pildoras(textos: List<String>, elegida: () -> Int, tocar: (Int) -> Unit): LinearLayout {
        val fila = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        val vistas = ArrayList<TextView>()
        fun pintar() {
            val e = elegida()
            vistas.forEachIndexed { i, v ->
                val sel = i == e
                v.background = ripple(if (sel) Tema.HUESO else cTarjeta2, 50)
                v.setTextColor(if (sel) Tema.ASFALTO else cTexto)
            }
        }
        textos.forEachIndexed { i, s ->
            val v = texto(s, 13f, cTexto, true).apply {
                typeface = Fuentes.numero
                gravity = Gravity.CENTER
                maxLines = 1
                setPadding(dp(6), 0, dp(6), 0)
                setAutoSizeTextTypeUniformWithConfiguration(9, 13, 1, TypedValue.COMPLEX_UNIT_SP)
                isClickable = true
                setOnClickListener { tocar(i); pintar() }
            }
            vistas.add(v)
            fila.addView(v, LinearLayout.LayoutParams(0, dp(40), 1f).apply { if (i > 0) leftMargin = dp(Tema.S1) })
        }
        pintar()
        return fila
    }

    /** Campo de la marca v2: nombre a la izquierda y el dato editable a la derecha. */
    private fun campoDato(titulo: String, valor: Float, pre: String, suf: String, cambio: (Float) -> Unit): View {
        val fila = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(12), 0, dp(12), 0)
            minimumHeight = dp(Tema.ALTO_TOQUE)
            background = redondo(Tema.CAMPO, 14)
            isClickable = true
        }
        fila.addView(texto(titulo, Tema.T_TEXTO, cTexto2, false), LinearLayout.LayoutParams(0, -2, 1f))
        fun dato(s: String) = texto(s, Tema.T_TEXTO, cTexto, true).apply { typeface = Fuentes.numero }
        if (pre.isNotEmpty()) fila.addView(dato(pre))
        val et = EditText(this).apply {
            setText(num(valor))
            setTextColor(cTexto)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, Tema.T_TEXTO)
            typeface = Fuentes.numero
            gravity = Gravity.END or Gravity.CENTER_VERTICAL
            background = null
            setPadding(dp(2), dp(10), dp(2), dp(10))
            minWidth = dp(24)
            setSingleLine()
            inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL
            addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(a: CharSequence?, b: Int, c: Int, d: Int) {}
                override fun onTextChanged(a: CharSequence?, b: Int, c: Int, d: Int) {}
                override fun afterTextChanged(e: Editable?) {
                    val v = e?.toString()?.replace(",", ".")?.toFloatOrNull() ?: return
                    if (v > 0) cambio(v)
                }
            })
        }
        fila.addView(et, LinearLayout.LayoutParams(-2, -2))
        fila.addView(dato(suf))
        fila.setOnClickListener {
            et.requestFocus()
            et.setSelection(et.text.length)
            (getSystemService(android.content.Context.INPUT_METHOD_SERVICE) as android.view.inputmethod.InputMethodManager).showSoftInput(et, 0)
        }
        return fila
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
            v == null -> cSinDatos
            v >= meta -> cVerde
            v >= meta * 0.85 -> cAmarillo
            else -> cRojo
        }
    }

    private fun hh(h: Int) = String.format(Locale.US, "%02d:00", h)
    private fun rango(h: Int) = hh(h) + "–" + hh((h + 1) % 24)

    private fun cuadro(color: Int, marcado: Boolean, radio: Int) = GradientDrawable().apply {
        setColor(color)
        cornerRadius = dp(radio).toFloat()
        if (marcado) setStroke(dp(2), cTexto)
    }

    private fun leyenda(): View {
        val fila = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL }
        listOf(cVerde to "Conviene", cAmarillo to "Casi", cRojo to "Abajo", cSinDatos to "Sin datos").forEachIndexed { i, (c, t) ->
            fila.addView(View(this).apply { background = redondo(c, 3) }, LinearLayout.LayoutParams(dp(10), dp(10)).apply {
                if (i > 0) leftMargin = dp(12)
                rightMargin = dp(4)
            })
            fila.addView(texto(t, Tema.T_NOTA, cTexto2, false))
        }
        return fila
    }

    /** «7 pm», «12 pm», «9 am». */
    private fun horaCorta(h: Int): String {
        val x = h % 24
        val n = if (x % 12 == 0) 12 else x % 12
        return "$n ${if (x < 12) "am" else "pm"}"
    }

    /** «7 a 8 pm» o «11 am a 12 pm». */
    private fun rangoCorto(h: Int): String {
        val a = horaCorta(h)
        val b = horaCorta(h + 1)
        return if (a.takeLast(2) == b.takeLast(2)) a.dropLast(3) + " a " + b else "$a a $b"
    }

    /** Tarjeta de Inicio: las próximas 7 horas en una tira (color según la meta por km) y lo mejor que viene. */
    private fun pintarMejoresHoras(recargar: Boolean = true) {
        if (!::mejoresHorasLista.isInitialized) return
        mejoresHorasLista.removeAllViews()
        if (recargar) mhRegs = Historial.ultimosDias(this, 30)
        val m = celdas(mhRegs)
        val cal = Calendar.getInstance()
        val hoy = diaLunes(cal)
        val ahora = cal.get(Calendar.HOUR_OF_DAY)
        val f = Array(24) { porKmDe(m[hoy][it]) }

        // Tira: desde la hora actual, 7 horas (después de medianoche, con los datos de mañana).
        val detalle = texto("", Tema.T_NOTA, cTexto2, false).apply { visibility = View.GONE }
        val tira = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        for (i in 0 until 7) {
            val h = (ahora + i) % 24
            val v = porKmDe(m[if (ahora + i < 24) hoy else (hoy + 1) % 7][h])
            val celda = numero("$h", 10f, if (v == null) cTenue else Color.argb(200, 0, 0, 0)).apply {
                gravity = Gravity.CENTER
                background = cuadro(colorKm(v), false, 6)
                isClickable = true
                setOnClickListener {
                    detalle.visibility = View.VISIBLE
                    detalle.text = (if (i == 0) "Ahora, " else "") + rangoCorto(h) + ": " +
                        if (v == null) "aún no hay suficientes ofertas." else "${dinero(v)}/km (tu meta: ${dinero(prefs.minPorKm.toDouble())}/km)"
                }
            }
            tira.addView(celda, LinearLayout.LayoutParams(0, dp(28), 1f).apply { if (i > 0) leftMargin = dp(4) })
        }
        mejoresHorasLista.addView(tira)
        mejoresHorasLista.addView(detalle, margenArriba(Tema.S1))

        // Lo mejor que viene (hoy)
        var mejor = -1
        for (h in ahora + 1 until 24) {
            val v = f[h] ?: continue
            if (mejor < 0 || v > (f[mejor] ?: 0.0)) mejor = h
        }
        val linea = when {
            Licencia.basico(this) -> "Lo mejor que viene: plan 6 meses ›"
            mhRegs.size < 10 -> "Llevas ${mhRegs.size} ofertas. Los colores se llenan conforme uses la app."
            mejor < 0 -> "Lo mejor que viene: aún sin datos para hoy"
            else -> "Lo mejor que viene: ${rangoCorto(mejor)} · ${dinero(f[mejor]!!)}/km"
        }
        mejoresHorasLista.addView(texto(linea, Tema.T_NOTA, cTenue, false), margenArriba(Tema.S1))
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
                background = redondo(cTarjeta2, Tema.R_BOTON)
                setPadding(dp(12), dp(10), dp(12), dp(10))
            }
            caja.addView(texto("Tu mejor día: ${nombresDia[md]} · ${dinero(prom[md]!!)}/km", Tema.T_TEXTO, cTexto, true))
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
                    background = punto(if (i == 0) Tema.HUESO else cTarjeta2)
                }, LinearLayout.LayoutParams(dp(22), dp(22)).apply { rightMargin = dp(10) })
                fila.addView(texto("${nombresDia[x.d]} ${rango(x.h)}", Tema.T_TEXTO, cTexto, false), LinearLayout.LayoutParams(0, -2, 1f))
                fila.addView(texto("${dinero(x.v)}/km", Tema.T_TEXTO, cTexto, true))
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
            nombres.addView(texto(nombresDia[d].take(3), Tema.T_NOTA, if (d == hoy) Tema.HUESO else cTexto2, d == hoy).apply {
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
                setStroke(dp(if (f) 2 else 1), if (f) Tema.HUESO else Tema.BORDE)
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
        col.addView(texto("Ajustes", 26f, cTexto, true), LinearLayout.LayoutParams(-1, -2).apply {
            bottomMargin = dp(Tema.S1)
        })

        // --- Pop-up ---
        col.addView(etiqueta("POP-UP"))
        // Vista previa a lo ancho de la pantalla (fuera de la tarjeta) para que mida igual que en «Probar en pantalla».
        val previa = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            setPadding(0, dp(Tema.S1), 0, dp(Tema.S1))
        }
        vistaPrevia = FrameLayout(this)
        previa.addView(vistaPrevia, LinearLayout.LayoutParams(-1, -2))
        previa.addView(texto("VISTA PREVIA · TAMAÑO REAL · NÚMEROS DE EJEMPLO", 10f, cTenue, false).apply {
            typeface = Fuentes.numero
            letterSpacing = 0.08f
            gravity = Gravity.CENTER
        }, margenArriba(10))
        col.addView(previa, LinearLayout.LayoutParams(-1, -2).apply {
            leftMargin = -dp(Tema.S2)
            rightMargin = -dp(Tema.S2)
            bottomMargin = dp(Tema.S1)
        })
        val pop = tarjeta()

        // Tamaño del pop-up: cambia el ancho; lo de adentro queda igual.
        val anchos = listOf(280, 320, 360)
        val cabAncho = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL }
        cabAncho.addView(texto("Tamaño del pop-up", Tema.T_TEXTO, cTexto2, false), LinearLayout.LayoutParams(0, -2, 1f))
        val notaAncho = texto("", 11f, cTenue, false).apply { typeface = Fuentes.numero }
        cabAncho.addView(notaAncho)
        fun pintarAncho() { notaAncho.text = "${prefs.anchoPopup} dp" + if (prefs.anchoPopup == 360) " · como antes" else "" }
        pintarAncho()
        pop.addView(cabAncho)
        pop.addView(pildoras(listOf("Chico", "Mediano", "Grande"), { anchos.indexOf(prefs.anchoPopup) }) { i ->
            prefs.anchoPopup = anchos[i]
            pintarAncho()
            refrescarVistaPrevia()
        }, margenArriba(Tema.S1))
        pop.addView(campoMeta("Tamaño de la letra", "", "", 1f, null, null,
            { prefs.tamanoTexto.toFloat() }, { prefs.tamanoTexto = it.toInt(); refrescarVistaPrevia() },
            Prefs.TAM_MIN.toFloat(), Prefs.TAM_MAX.toFloat()), margenArriba(12))
        pop.addView(campoMeta("Tiempo en pantalla", "", " s", 1f, null, null,
            { prefs.duracionSeg.toFloat() }, { prefs.duracionSeg = it.toInt() }, 3f, 60f), margenArriba(Tema.S1))
        pop.addView(campoMeta("Altura en la pantalla", "", " %", 1f, null, null,
            { prefs.posicion.toFloat() }, { prefs.posicion = it.toInt() }, 0f, 80f), margenArriba(Tema.S1))
        pop.addView(texto("También lo puedes arrastrar con el dedo cuando sale en Uber o DiDi.", Tema.T_NOTA, cTenue, false)
            .apply { setLineSpacing(0f, 1.2f) }, margenArriba(Tema.S1))
        pop.addView(botonContorno("Probar en pantalla") {
            val o = ofertaEjemplo()
            popupPrueba?.mostrar(o)
            Alertas(this, prefs).avisar(o.nivel(prefs))
        }, margenArriba(Tema.S2))
        col.addView(pop, margenTarjeta())

        // --- Avisos: filas agrupadas ---
        col.addView(etiqueta("AVISOS"))
        val av = grupoFilas()
        filaInterruptor(av, "Voz", "«Aceptar», «Considerar», «Rechazar» · apagada: tonos", prefs.voz) { prefs.voz = it }
        filaInterruptor(av, "Sonido", "Avisa con sonido cada oferta", prefs.sonido) { prefs.sonido = it }
        filaInterruptor(av, "Vibrar", "1 corta: conviene · 2 cortas: regular · 1 larga: no", prefs.vibrar) { prefs.vibrar = it }
        filaInterruptor(av, "Avisarme al abrir Uber o DiDi", "Si la lectura está apagada, te ofrece iniciarla", prefs.avisarApertura) {
            prefs.avisarApertura = it
        }
        val vol = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(14), 0, dp(14), dp(12))
        }
        vol.addView(deslizador("Volumen", 10, 100, prefs.volumen, " %") { prefs.volumen = it })
        val pruebas = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        val alertas = Alertas(this, prefs)
        listOf(Oferta.VERDE, Oferta.AMARILLO, Oferta.ROJO).forEachIndexed { i, n ->
            val b = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER
                minimumHeight = dp(40)
                background = ripple(cTarjeta2, 50)
                isClickable = true
                setOnClickListener { alertas.avisar(n) }
                addView(View(this@MainActivity).apply { background = punto(Tema.colorNivel(n)) },
                    LinearLayout.LayoutParams(dp(10), dp(10)).apply { rightMargin = dp(Tema.S1) })
                addView(texto("Probar", Tema.T_TEXTO, cTexto, true).apply { typeface = fuenteMedia })
            }
            pruebas.addView(b, LinearLayout.LayoutParams(0, -2, 1f).apply { if (i > 0) leftMargin = dp(Tema.S1) })
        }
        vol.addView(pruebas, margenArriba(Tema.S1))
        av.addView(View(this).apply { setBackgroundColor(Tema.BORDE) }, LinearLayout.LayoutParams(-1, dp(1)))
        av.addView(vol)
        col.addView(av, margenTarjeta())

        // --- Lectura: qué lee Checa y lo que necesita para leer ---
        col.addView(etiqueta("LECTURA"))
        val lectura = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        cajaLecturaAjustes = lectura
        pintarFilasLectura()
        col.addView(lectura, margenTarjeta())

        return scroll(col)
    }

    // =====================================================================
    // Pestaña Cuenta: cuenta, licencia, paquetes, ayuda y soporte
    // =====================================================================

    private fun pantallaCuenta(): View {
        val col = columna()

        // --- Perfil, credencial del plan y menú de filas ---
        cuentaCaja = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        col.addView(cuentaCaja, margenTarjeta())

        // --- Paquetes ---
        val paq = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        paq.addView(etiqueta("PAQUETES"))
        paq.addView(texto(
            "Paga con tarjeta, OXXO o saldo de Mercado Pago. Tu licencia se activa sola en tu cuenta; " +
                "si ya tienes una, los días se suman.",
            Tema.T_NOTA, cTenue, false
        ).apply { setLineSpacing(0f, 1.2f) }, LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = dp(Tema.S1) })
        paq.addView(tarjetaPaquete(
            "1m", "1 MES", "Plan Básico", "$99", "$59", "Para probar todo en tu día a día.",
            listOf(
                "Pop-up con semáforo, ganancia neta, $/\u2060hora, $/\u2060km y dinámica ⚡",
                "Filtros inteligentes y aviso por voz o vibración",
                "Mejores horas de hoy",
                "1 equipo",
                "Soporte por WhatsApp"
            ), false
        ), margenTarjeta())
        paq.addView(tarjetaPaquete(
            "6m", "6 MESES", "Plan Estratega", "$399", "$239", "Equivale a $66.50 al mes · Ahorras 33 %",
            listOf(
                "Todo lo del Plan Básico, más:",
                "Uber y DiDi al mismo tiempo: Checa marca cuál conviene más",
                "Mejores horas completo: «lo mejor que viene» y vista de la semana",
                "2 equipos (teléfono + tablet)",
                "Soporte prioritario por WhatsApp"
            ), true
        ), margenTarjeta())
        paq.addView(tarjetaPaquete(
            "12m", "1 AÑO", "Plan Pro", "$899", "$539", "Equivale a $74.90 al mes",
            listOf(
                "Todo lo del Plan Estratega",
                "2 equipos (teléfono + tablet)",
                "Soporte VIP por WhatsApp"
            ), false
        ), margenTarjeta())
        paquetesVista = paq
        col.addView(paq)

        // --- Versión y cerrar sesión ---
        val pie = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        cuentaPie = pie
        col.addView(pie, margenArriba(Tema.S1))
        pintarCuenta()
        return scroll(col).also { cuentaScroll = it }
    }

    // =====================================================================
    // Avisos en ventana: fin de la beta y opinión
    // =====================================================================

    private fun avisosEnVentana() {
        if (isFinishing || isDestroyed) return
        if (hayVentana()) return
        val e = Licencia.estado(this)
        if (e.beta && !e.pagada) {
            val umbral = when {
                e.diasRestantes <= 7 -> 7
                e.diasRestantes <= DIAS_AVISO_FIN_BETA -> DIAS_AVISO_FIN_BETA
                else -> 0
            }
            if (umbral > 0 && (prefs.avisoFinBeta == 0 || prefs.avisoFinBeta > umbral)) {
                prefs.avisoFinBeta = umbral
                dialogoFinBeta(e.diasRestantes)
                return
            }
        }
        pedirOpinionSiToca()
    }

    private fun dialogoFinBeta(dias: Int) {
        val cuerpo = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        cuerpo.addView(texto(
            "La beta gratis termina el ${Licencia.fechaBonita(Licencia.BETA_HASTA)} (quedan $dias ${if (dias == 1) "día" else "días"}). " +
                "Después, Checa se paga con paquetes desde $99 al mes.",
            Tema.T_TEXTO, cTexto2, false
        ).apply { setLineSpacing(0f, 1.2f) })
        cuerpo.addView(texto(
            "Como la probaste en la beta, tendrás precio de fundador: 40 % menos de por vida " +
                "(paga antes del 28 de febrero y renueva a tiempo). " +
                if (Cuenta.conectada(this)) "Tu cuenta ya está registrada." else "Entra con tu cuenta de Google para asegurarlo.",
            Tema.T_TEXTO, cTexto, true
        ).apply { setLineSpacing(0f, 1.2f) }, margenArriba(Tema.S1))
        if (!Cuenta.conectada(this)) {
            cuerpo.addView(boton("Entrar con Google", cVerde, Color.BLACK) {
                dialogoAviso?.dismiss()
                entrarConGoogle()
            }, margenArriba(Tema.S2))
        }
        cuerpo.addView(boton("Entendido", cTarjeta2, cTexto) { dialogoAviso?.dismiss() }, margenArriba(Tema.S1))
        dialogoAviso = hoja("Se acerca el fin de la beta", cAmarillo, cuerpo)
    }

    /** Pide la opinión una vez por semana (máx. 3) a quien ya lleva 7 días y varias ofertas leídas. */
    private fun pedirOpinionSiToca() {
        if (prefs.resenaHecha || prefs.resenaPedida >= 3) return
        val ahora = System.currentTimeMillis()
        if (ahora < prefs.resenaProxima) return
        val instalada = try { packageManager.getPackageInfo(packageName, 0).firstInstallTime } catch (_: Exception) { ahora }
        if (ahora - instalada < DIAS_PARA_OPINION * 86_400_000L) return
        if (Historial.todos(this).size < OFERTAS_PARA_OPINION) return
        prefs.resenaPedida += 1
        prefs.resenaProxima = ahora + 7 * 86_400_000L
        val cuerpo = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        cuerpo.addView(texto(
            "Ya llevas una semana con Checa. Tu opinión ayuda a otros conductores a decidir si les sirve.",
            Tema.T_TEXTO, cTexto2, false
        ).apply { setLineSpacing(0f, 1.2f) })
        cuerpo.addView(boton("Sí, me sirve: contar mi opinión", cVerde, Color.BLACK) {
            prefs.resenaHecha = true
            dialogoAviso?.dismiss()
            Soporte.opinion(this)
        }, margenArriba(Tema.S2))
        cuerpo.addView(boton("Tengo un problema", cTarjeta2, cTexto) {
            dialogoAviso?.dismiss()
            mostrarAyuda()
        }, margenArriba(Tema.S1))
        cuerpo.addView(boton("Después", cTarjeta2, cTexto) { dialogoAviso?.dismiss() }, margenArriba(Tema.S1))
        dialogoAviso = hoja("¿Te está sirviendo Checa?", null, cuerpo)
    }

    // =====================================================================
    // Mi día: viajes aceptados, meta y resumen
    // =====================================================================

    private fun gasolinaDe(km: Double): Double =
        if (prefs.kmPorLitro > 0) km / prefs.kmPorLitro * prefs.precioLitro else 0.0

    private fun pintarJornada() {
        if (!::jornadaCaja.isInitialized) return
        val c = jornadaCaja
        c.removeAllViews()
        val hoy = Jornada.totales(Jornada.deHoy(this))
        val sem = Jornada.totales(Jornada.deSemana(this))
        val meta = prefs.metaDia.toDouble()
        // Lo que te queda hoy en grande y, al lado, contra qué meta.
        val cifra = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.BOTTOM }
        cifra.addView(texto(dinero(hoy.neta, 0), 32f, cTexto, true).apply { letterSpacing = -0.03f })
        cifra.addView(
            texto(if (meta > 0) "netos de ${dinero(meta, 0)}" else "netos hoy", Tema.T_TEXTO, cTenue, false),
            LinearLayout.LayoutParams(-2, -2).apply { leftMargin = dp(Tema.S1); bottomMargin = dp(4) }
        )
        c.addView(cifra)
        if (meta > 0) {
            val avance = (hoy.neta / meta).coerceIn(0.0, 1.0)
            val barra = FrameLayout(this).apply { background = redondo(cTarjeta2, 4) }
            val lleno = View(this).apply { background = redondo(cVerde, 4) }
            barra.addView(lleno, FrameLayout.LayoutParams(0, -1))
            barra.post { lleno.layoutParams = FrameLayout.LayoutParams((barra.width * avance).toInt(), -1) }
            c.addView(barra, LinearLayout.LayoutParams(-1, dp(8)).apply { topMargin = dp(10) })
        }
        c.addView(texto(
            when {
                hoy.viajes == 0 -> "Marca los viajes que aceptas para llevar tu cuenta"
                else -> "${hoy.viajes} ${if (hoy.viajes == 1) "viaje" else "viajes"} · gasolina −${dinero(hoy.gasolina, 0)}" +
                    if (meta > 0 && hoy.neta >= meta) " · ¡meta cumplida!" else ""
            },
            Tema.T_NOTA, cTenue, false
        ), margenArriba(10))
        if (sem.viajes > hoy.viajes) {
            c.addView(texto("Esta semana: ${dinero(sem.neta, 0)} netos · ${sem.viajes} viajes", Tema.T_NOTA, cTenue, false),
                margenArriba(2))
        }
        c.addView(dosBotones(
            boton("Registrar viaje", cVerde, Color.BLACK) { dialogoRegistrarViaje() },
            botonContorno(if (meta > 0) "Cambiar meta" else "Poner meta") { dialogoMeta() }
        ), margenArriba(Tema.S2))
        val compartir = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER }
        listOf("Compartir mi día" to false, "Compartir mi semana" to true).forEachIndexed { i, (s, semana) ->
            compartir.addView(texto(s, Tema.T_NOTA, cTenue, false).apply {
                gravity = Gravity.CENTER
                minHeight = dp(40)
                setPadding(dp(Tema.S1), 0, dp(Tema.S1), 0)
                paintFlags = paintFlags or android.graphics.Paint.UNDERLINE_TEXT_FLAG
                isClickable = true
                setOnClickListener { Resumen.compartir(this@MainActivity, semana) }
            }, LinearLayout.LayoutParams(-2, -2).apply { if (i > 0) leftMargin = dp(Tema.S1) })
        }
        c.addView(compartir, margenArriba(4))
    }

    private fun dosBotones(a: View, b: View) = LinearLayout(this).apply {
        orientation = LinearLayout.HORIZONTAL
        addView(a, LinearLayout.LayoutParams(0, -2, 1f))
        addView(b, LinearLayout.LayoutParams(0, -2, 1f).apply { leftMargin = dp(Tema.S1) })
    }

    /** Lista las últimas ofertas de hoy para marcar cuáles se aceptaron. */
    private fun dialogoRegistrarViaje() {
        val regs = Historial.deHoy(this).sortedByDescending { it.hora }.take(10)
        val cuerpo = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        if (regs.isEmpty()) {
            cuerpo.addView(texto(
                "Aún no hay ofertas hoy. Cuando te lleguen, aquí marcas las que aceptaste.",
                Tema.T_TEXTO, cTexto2, false
            ).apply { setLineSpacing(0f, 1.2f) })
            hoja("¿Cuáles aceptaste?", null, cuerpo)
            return
        }
        cuerpo.addView(texto("Toca las ofertas que aceptaste. Sirven para tu meta, tu gasolina y tu resumen.",
            Tema.T_NOTA, cTenue, false).apply { setLineSpacing(0f, 1.2f) })
        val horaFmt = SimpleDateFormat("HH:mm", Locale.US)
        regs.forEach { r ->
            val marca = icono(Icono.OK, Tema.HUESO, 20).apply {
                visibility = if (Jornada.registrado(this@MainActivity, r.hora)) View.VISIBLE else View.INVISIBLE
            }
            val fila = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                background = ripple(cTarjeta, Tema.R_BOTON)
                setPadding(dp(Tema.S2), dp(12), dp(Tema.S2), dp(12))
                isClickable = true
            }
            val neta = r.tarifa - gasolinaDe(r.km)
            val textos = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
            textos.addView(texto("${horaFmt.format(Date(r.hora))} · ${r.app}", Tema.T_TEXTO, cTexto, true))
            textos.addView(
                texto("${dinero(r.tarifa, 0)} · ${num1(r.km)} km · neta ${dinero(neta, 0)}", Tema.T_NOTA, cTexto2, false),
                LinearLayout.LayoutParams(-2, -2).apply { topMargin = dp(2) }
            )
            fila.addView(textos, LinearLayout.LayoutParams(0, -2, 1f))
            fila.addView(marca, LinearLayout.LayoutParams(dp(20), dp(20)))
            fila.setOnClickListener {
                if (Jornada.registrado(this, r.hora)) {
                    Jornada.quitar(this, r.hora)
                    marca.visibility = View.INVISIBLE
                } else {
                    Jornada.registrar(this, r, prefs)
                    marca.visibility = View.VISIBLE
                }
                pintarJornada()
            }
            cuerpo.addView(fila, margenArriba(Tema.S1))
        }
        hoja("¿Cuáles aceptaste?", null, cuerpo)
    }

    private fun dialogoMeta() {
        val cuerpo = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        cuerpo.addView(texto("¿Cuánto quieres ganar neto hoy, después de gasolina?", Tema.T_TEXTO, cTexto2, false)
            .apply { setLineSpacing(0f, 1.2f) })
        var nueva = prefs.metaDia
        cuerpo.addView(
            campo(if (prefs.metaDia > 0) num(prefs.metaDia) else "") { v -> nueva = v },
            LinearLayout.LayoutParams(-1, dp(Tema.ALTO_TOQUE)).apply { topMargin = dp(Tema.S2) }
        )
        var dialogo: Dialog? = null
        cuerpo.addView(boton("Guardar meta", cVerde, Color.BLACK) {
            prefs.metaDia = nueva
            pintarJornada()
            dialogo?.dismiss()
        }, margenArriba(Tema.S2))
        if (prefs.metaDia > 0) {
            cuerpo.addView(boton("Quitar meta", cTarjeta2, cTexto) {
                prefs.metaDia = 0f
                pintarJornada()
                dialogo?.dismiss()
            }, margenArriba(Tema.S1))
        }
        dialogo = hoja("Meta del día", null, cuerpo)
    }

    // =====================================================================
    // Ayuda: preguntas frecuentes, diagnóstico y soporte
    // =====================================================================

    private fun hacerAccion(a: Soporte.Accion) {
        try {
            when (a) {
                Soporte.Accion.ACCESIBILIDAD -> abrirAccesibilidad()
                Soporte.Accion.INFO_APP -> abrirInfoApp()
                Soporte.Accion.BATERIA -> startActivity(Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS))
                Soporte.Accion.NOTIFICACIONES -> startActivity(
                    Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE, packageName)
                )
                Soporte.Accion.PAQUETES -> irAPaquetes()
                Soporte.Accion.PAUSAR -> pausarLector()
                Soporte.Accion.SOPORTE -> Soporte.escribir(this, "tengo una duda")
                Soporte.Accion.NINGUNA -> {}
            }
        } catch (_: Exception) {
            abrirInfoApp()
        }
    }

    // =====================================================================
    // Pestaña Cuenta como menú (maqueta https://claude.ai/artifact/AVNnKkJ44Fw8E13scufVpH)
    // =====================================================================

    private var cuentaPie: LinearLayout? = null
    private var firmaCuenta = ""
    /** Repinta «Ayuda y soporte» si está abierta (al volver de Ajustes del teléfono). */
    private var pintarAyudaAbierta: (() -> Unit)? = null

    private fun permisosFaltantes(): Int =
        listOf(servicioActivo(), permisoNotificaciones(), permisoBateria()).count { !it }

    /** Grupo de filas con fondo de tarjeta y líneas entre ellas. */
    private fun grupoFilas() = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        background = redondo(cTarjeta, Tema.R_TARJETA)
    }

    private fun filaMenu(
        grupo: LinearLayout, tipoIcono: Int, titulo: String, sub: String, colorSub: Int = cTenue, accion: () -> Unit
    ) {
        if (grupo.childCount > 0) grupo.addView(View(this).apply { setBackgroundColor(Tema.BORDE) },
            LinearLayout.LayoutParams(-1, dp(1)).apply { leftMargin = dp(56) })
        val f = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            minimumHeight = dp(60)
            setPadding(dp(12), dp(10), dp(12), dp(10))
            background = ripple(Color.TRANSPARENT, Tema.R_TARJETA)
            isClickable = true
            setOnClickListener { accion() }
        }
        val caja = FrameLayout(this).apply { background = redondo(cTarjeta2, 10) }
        caja.addView(icono(tipoIcono, Tema.HUESO, 18), FrameLayout.LayoutParams(dp(18), dp(18), Gravity.CENTER))
        f.addView(caja, LinearLayout.LayoutParams(dp(32), dp(32)).apply { rightMargin = dp(12) })
        val textos = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        textos.addView(texto(titulo, Tema.T_TEXTO, cTexto, true))
        textos.addView(texto(sub, Tema.T_NOTA, colorSub, false).apply { maxLines = 1; ellipsize = TextUtils.TruncateAt.END })
        f.addView(textos, LinearLayout.LayoutParams(0, -2, 1f))
        f.addView(texto("›", 20f, cTenue, false), LinearLayout.LayoutParams(-2, -2).apply { leftMargin = dp(8) })
        grupo.addView(f)
    }

    /** Fila con nombre, descripción e interruptor dentro de un grupo (marca v2). */
    private fun filaInterruptor(grupo: LinearLayout, titulo: String, sub: String?, valor: Boolean, cambio: (Boolean) -> Unit) {
        if (grupo.childCount > 0) grupo.addView(View(this).apply { setBackgroundColor(Tema.BORDE) },
            LinearLayout.LayoutParams(-1, dp(1)))
        val f = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            minimumHeight = dp(56)
            setPadding(dp(14), dp(8), dp(10), dp(8))
            background = ripple(Color.TRANSPARENT, Tema.R_TARJETA)
            isClickable = true
        }
        val textos = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        textos.addView(texto(titulo, Tema.T_TEXTO, cTexto, true))
        if (sub != null) textos.addView(texto(sub, Tema.T_NOTA, cTenue, false).apply { setLineSpacing(0f, 1.15f) }, margenArriba(2))
        f.addView(textos, LinearLayout.LayoutParams(0, -2, 1f).apply { rightMargin = dp(Tema.S1) })
        val sw = interruptorSolo(valor).apply { setOnCheckedChangeListener { _, b -> cambio(b) } }
        f.addView(sw)
        f.setOnClickListener { sw.toggle() }
        grupo.addView(f)
    }

    /** Qué apps lee Checa: Uber, DiDi o las dos. */
    private fun mostrarAppsLee() {
        val cuerpo = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        cuerpo.addView(texto("Apaga la que no uses para que Checa no la revise.", Tema.T_NOTA, cTenue, false)
            .apply { setLineSpacing(0f, 1.2f) })
        val g = grupoFilas().apply { background = redondo(cTarjeta2, Tema.R_TARJETA) }
        filaInterruptor(g, "Uber Driver", "Leer sus ofertas", prefs.uber) { prefs.uber = it }
        filaInterruptor(g, "DiDi Conductor", "Leer sus ofertas", prefs.didi) { prefs.didi = it }
        cuerpo.addView(g, margenArriba(Tema.S2))
        hoja("Apps que lee", null, cuerpo).setOnDismissListener { pintarFilasLectura() }
    }

    private fun nombreAppsLee() = when {
        prefs.uber && prefs.didi -> "Uber y DiDi"
        prefs.uber -> "Solo Uber"
        prefs.didi -> "Solo DiDi"
        else -> "Ninguna: enciende al menos una"
    }

    /** Filas de la pestaña Cuenta, con el estado de cada cosa a la vista. */
    private fun filasCuenta(): View {
        val g = grupoFilas()
        val equipos = Cuenta.equipos(this)
        filaMenu(g, Icono.CUENTA, "Equipos",
            "${equipos.size} de ${Cuenta.maxEquipos(this)}" +
                if (equipos.isEmpty()) "" else " · " + equipos.joinToString(", ") { it.visible() }) { mostrarEquipos() }
        filaMenu(g, Icono.ALERTA, "Ayuda y soporte", "Preguntas frecuentes y WhatsApp") { mostrarAyuda() }
        filaMenu(g, Icono.PLAY, "Guía de inicio", "Cómo usar Checa paso a paso") { mostrarGuia() }
        filaMenu(g, Icono.RELOJ, "Tus datos", "Qué guarda Checa y estadísticas") { mostrarTusDatos() }
        return g
    }

    // =====================================================================
    // Ajustes → Lectura (permisos, bancos y ayuda con la lectura) y estado en Inicio
    // =====================================================================

    private var cajaLecturaAjustes: LinearLayout? = null

    private fun pintarFilasLectura() {
        val caja = cajaLecturaAjustes ?: return
        caja.removeAllViews()
        val g = grupoFilas()
        val faltan = permisosFaltantes()
        filaMenu(g, Icono.CELULAR, "Apps que lee", nombreAppsLee(),
            if (prefs.uber || prefs.didi) cTenue else cAmarillo) { mostrarAppsLee() }
        filaMenu(g, Icono.OK, "Permisos", if (faltan == 0) "Todo listo" else "Falta $faltan",
            if (faltan == 0) cVerde else cAmarillo) { mostrarPermisos() }
        filaMenu(g, Icono.PIN, "Apps de banco", "Pausa la lectura para usar tu banco") { mostrarBancos() }
        val guardadas = Guardado.lista(this)
        val noLeidas = guardadas.count { !it.leida }
        filaMenu(g, Icono.FILTROS, "Ayuda con la lectura",
            when {
                !prefs.diagnostico -> "Guardar pantallas está apagado"
                guardadas.isEmpty() -> "Aún no hay ofertas guardadas"
                noLeidas > 0 -> "${guardadas.size} guardadas · $noLeidas no ${if (noLeidas == 1) "leída" else "leídas"}"
                else -> "${guardadas.size} ofertas guardadas"
            },
            if (noLeidas > 0) cAmarillo else cTenue) { mostrarAyudaLectura() }
        caja.addView(g)
    }

    private class EstadoInicio(val caja: LinearLayout, val punto: View, val titulo: TextView, val sub: TextView, val chip: TextView)
    private var estadoInicio: EstadoInicio? = null
    private var firmaEstadoInicio = ""

    /** Línea de Inicio: si Checa está leyendo y, si algo falla, a un toque de arreglarlo. */
    private fun pintarEstadoInicio() {
        val e = estadoInicio ?: return
        val leyendo = CapturaService.corriendo
        val faltan = permisosFaltantes()
        val ultima = prefs.ultimaOferta
        val (color, titulo, sub, chip) = when {
            faltan > 0 -> listOf(cAmarillo, "Falta un permiso", "Sin él Checa no puede leer tus ofertas", "Revisar")
            !leyendo && CapturaService.cortada -> listOf(cAmarillo, "La lectura se detuvo", "Android la cortó. Toca para revisar", "Revisar")
            !leyendo -> listOf(cTenue, "Lectura apagada", "Inicia la lectura antes de abrir Uber o DiDi", "Iniciar")
            ultima == 0L -> listOf(cVerde, "Leyendo", "Aún sin ofertas · ¿algo falla? Toca aquí", "")
            else -> listOf(cVerde, "Leyendo", "Última oferta ${hace(ultima)}", "")
        }
        val firma = "$titulo|$sub|$chip"
        if (firma == firmaEstadoInicio) return
        firmaEstadoInicio = firma
        val c = color as Int
        e.punto.background = punto(c)
        e.titulo.text = titulo as String
        e.sub.text = sub as String
        e.chip.text = chip as String
        e.chip.visibility = if ((chip as String).isEmpty()) View.GONE else View.VISIBLE
        e.caja.background = GradientDrawable().apply {
            setColor(cTarjeta)
            cornerRadius = dp(Tema.R_TARJETA).toFloat()
            setStroke(dp(1), if (c == cTenue) Tema.HUMO else Color.argb(150, Color.red(c), Color.green(c), Color.blue(c)))
        }
        e.caja.setOnClickListener {
            if (permisosFaltantes() == 0 && !CapturaService.corriendo && !CapturaService.cortada) iniciarCaptura()
            else mostrarAlgoFalla()
        }
    }

    /** Pie de Cuenta: versión (con actualizar) y «Cerrar sesión», que pide confirmar. */
    private fun pintarCuentaPie() {
        val pie = cuentaPie ?: return
        pie.removeAllViews()
        val nueva = Actualizacion.pendiente(this)
        pie.addView(texto(
            "Checa ${Actualizacion.instalada(this)} · " + if (nueva == null) "al día" else "hay versión nueva",
            Tema.T_NOTA, if (nueva == null) cTenue else Tema.HUESO, false
        ).apply { gravity = Gravity.CENTER })
        botonBuscarAct = texto(if (nueva == null) "Buscar actualización" else "Actualizar a la $nueva", Tema.T_NOTA,
            if (nueva == null) cTexto2 else Tema.HUESO, true).apply {
            gravity = Gravity.CENTER
            minHeight = dp(Tema.ALTO_TOQUE)
            isClickable = true
            setOnClickListener { buscarActualizacion() }
        }
        pie.addView(botonBuscarAct)
        if (Cuenta.conectada(this)) {
            pie.addView(texto("Cerrar sesión", Tema.T_TEXTO, cTenue, false).apply {
                gravity = Gravity.CENTER
                minHeight = dp(Tema.ALTO_TOQUE)
                typeface = fuenteMedia
                paintFlags = paintFlags or android.graphics.Paint.UNDERLINE_TEXT_FLAG
                isClickable = true
                setOnClickListener { confirmarCerrarSesion() }
            })
        }
    }

    private fun confirmarCerrarSesion() {
        val cuerpo = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        listOf(
            "Este teléfono deja de mostrar pop-ups y se libera un lugar de tus equipos.",
            "Se borran de aquí las ofertas guardadas y las estadísticas.",
            "Tu licencia y tus ajustes siguen en tu cuenta: al volver a entrar, regresan."
        ).forEachIndexed { i, s ->
            val f = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
            f.addView(View(this).apply { background = punto(Tema.HUESO) },
                LinearLayout.LayoutParams(dp(6), dp(6)).apply { topMargin = dp(8); rightMargin = dp(10) })
            f.addView(texto(s, Tema.T_TEXTO, cTexto2, false).apply { setLineSpacing(0f, 1.2f) }, LinearLayout.LayoutParams(0, -2, 1f))
            cuerpo.addView(f, if (i > 0) margenArriba(Tema.S1) else LinearLayout.LayoutParams(-1, -2))
        }
        var d: Dialog? = null
        cuerpo.addView(boton("Cerrar sesión", Tema.ROJO_SUAVE, cRojo) {
            d?.dismiss()
            Toast.makeText(this, "Cerrando sesión…", Toast.LENGTH_SHORT).show()
            Cuenta.cerrarSesion(this) { pintarLicencia(); pintarCuenta(); actualizarAcceso() }
        }, margenArriba(Tema.S2))
        cuerpo.addView(boton("Cancelar", cTarjeta2, cTexto) { d?.dismiss() }, margenArriba(Tema.S1))
        d = hoja("¿Cerrar sesión en este teléfono?", null, cuerpo)
    }

    /** Equipos de la cuenta: nombre propio, último uso y qué pasa al quitar uno. */
    private fun mostrarEquipos() {
        val (d, cuerpo, _) = ventanaCompleta("Equipos")
        val col = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), 0, dp(16), dp(Tema.S2))
        }
        cuerpo.addView(ScrollView(this).apply { isVerticalScrollBarEnabled = false; addView(col) }, LinearLayout.LayoutParams(-1, -1))
        fun pintar() {
            col.removeAllViews()
            val max = Cuenta.maxEquipos(this)
            col.addView(texto(
                "Tu plan permite $max ${if (max == 1) "equipo" else "equipos"}. Si quitas uno, deja de mostrar pop-ups " +
                    "hasta que vuelvas a entrar con tu cuenta en él.",
                Tema.T_TEXTO, cTexto2, false
            ).apply { setLineSpacing(0f, 1.2f) })
            val lista = Cuenta.equipos(this)
            lista.forEach { eq ->
                val esEste = eq.id == Licencia.codigoEquipo(this)
                val t = tarjeta()
                val cab = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL }
                val tx = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
                tx.addView(texto(if (eq.alias.isEmpty() && esEste) "Este teléfono" else eq.visible(), Tema.T_SUB, cTexto, true))
                tx.addView(texto("${eq.nombre.ifEmpty { eq.id }} · último uso: ${Cuenta.haceCuanto(eq.ultimoUso)}",
                    Tema.T_NOTA, cTenue, false), margenArriba(2))
                cab.addView(tx, LinearLayout.LayoutParams(0, -2, 1f))
                if (esEste) cab.addView(chipDiag("Este equipo", Tema.HUESO, Tema.ASFALTO))
                t.addView(cab)
                val botones = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
                botones.addView(boton("Cambiar nombre", cTarjeta2, cTexto) {
                    val campo = EditText(this).apply {
                        setText(eq.alias)
                        hint = eq.nombre
                        setSingleLine()
                        inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_CAP_SENTENCES
                    }
                    AlertDialog.Builder(this)
                        .setTitle("Nombre del equipo")
                        .setView(FrameLayout(this).apply { setPadding(dp(20), dp(8), dp(20), 0); addView(campo) })
                        .setPositiveButton("Guardar") { _, _ ->
                            Cuenta.renombrarEquipo(this, eq.id, campo.text.toString()) { r ->
                                if (r is Cuenta.Resultado.Error) Toast.makeText(this, r.mensaje, Toast.LENGTH_LONG).show()
                                pintar()
                                pintarCuenta()
                            }
                        }
                        .setNegativeButton("Cancelar", null)
                        .show()
                }, LinearLayout.LayoutParams(0, dp(44), 1f))
                if (!esEste) {
                    botones.addView(boton("Quitar", Tema.ROJO_SUAVE, cRojo) {
                        val c = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
                        c.addView(texto(
                            "Dejará de mostrar pop-ups. Para usarlo otra vez, entra con tu cuenta en él. " +
                                "Tu licencia y tus ajustes no cambian.",
                            Tema.T_TEXTO, cTexto2, false
                        ).apply { setLineSpacing(0f, 1.2f) })
                        var h: Dialog? = null
                        c.addView(boton("Quitar ${eq.visible()}", Tema.ROJO_SUAVE, cRojo) {
                            h?.dismiss()
                            Toast.makeText(this, "Quitando ${eq.visible()}…", Toast.LENGTH_SHORT).show()
                            Cuenta.quitarEquipo(this, eq.id) { r ->
                                pintar()
                                mostrarResultadoCuenta(r)
                            }
                        }, margenArriba(Tema.S2))
                        c.addView(boton("Cancelar", cTarjeta2, cTexto) { h?.dismiss() }, margenArriba(Tema.S1))
                        h = hoja("¿Quitar ${eq.visible()}?", null, c)
                    }, LinearLayout.LayoutParams(0, dp(44), 1f).apply { leftMargin = dp(8) })
                }
                t.addView(botones, margenArriba(Tema.S1))
                col.addView(t, margenArriba(Tema.ENTRE_TARJETAS))
            }
            repeat(maxOf(0, max - lista.size)) {
                col.addView(lugarLibre(), margenArriba(Tema.ENTRE_TARJETAS))
            }
        }
        pintar()
        d.setOnDismissListener { pintarCuenta() }
        d.show()
    }

    private fun sinAcentos(s: String) =
        java.text.Normalizer.normalize(s.lowercase(), java.text.Normalizer.Form.NFD).replace(Regex("\\p{Mn}+"), "")

    /** Una pregunta frecuente que se abre y se cierra. */
    private fun itemPregunta(p: Soporte.Pregunta): View {
        val caja = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = redondo(cTarjeta, Tema.R_BOTON)
            setPadding(dp(Tema.S2), dp(12), dp(Tema.S2), dp(12))
        }
        val cabecera = texto("+  ${p.pregunta}", Tema.T_TEXTO, cTexto, true)
        val respuesta = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; visibility = View.GONE }
        respuesta.addView(texto(p.respuesta, Tema.T_TEXTO, cTexto2, false).apply { setLineSpacing(0f, 1.2f) })
        if (p.accion != Soporte.Accion.NINGUNA) {
            respuesta.addView(boton(p.accion.texto, cTarjeta2, cTexto) { hacerAccion(p.accion) }, margenArriba(Tema.S1))
        }
        caja.addView(cabecera)
        caja.addView(respuesta, margenArriba(Tema.S1))
        caja.setOnClickListener {
            val abrir = respuesta.visibility != View.VISIBLE
            respuesta.visibility = if (abrir) View.VISIBLE else View.GONE
            cabecera.text = (if (abrir) "−  " else "+  ") + p.pregunta
        }
        return caja
    }

    /** Ayuda y soporte: revisión automática, preguntas con buscador y WhatsApp. */
    private fun mostrarAyuda() {
        val (d, cuerpo, _) = ventanaCompleta("Ayuda y soporte")
        val col = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), 0, dp(16), dp(Tema.S2))
        }
        cuerpo.addView(ScrollView(this).apply { isVerticalScrollBarEnabled = false; addView(col) }, LinearLayout.LayoutParams(-1, -1))

        val revision = tarjeta()
        col.addView(revision)
        fun pintarRevision() {
            revision.removeAllViews()
            val revisiones = Soporte.diagnosticar(this)
            val fallas = revisiones.count { !it.ok }
            revision.addView(texto("¿Algo falla?", Tema.T_SUB, cTexto, true))
            revision.addView(texto(
                if (fallas == 0) "Todo se ve bien. Si aun así no sale el pop-up, abre Uber o DiDi con una oferta en pantalla."
                else if (fallas == 1) "Hay 1 cosa por arreglar:" else "Hay $fallas cosas por arreglar:",
                Tema.T_NOTA, if (fallas == 0) cVerde else cAmarillo, false
            ).apply { setLineSpacing(0f, 1.2f) }, margenArriba(4))
            revisiones.forEach { r ->
                val f = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
                f.addView(icono(if (r.ok) Icono.OK else Icono.ALERTA, if (r.ok) cVerde else cAmarillo, 18),
                    LinearLayout.LayoutParams(dp(18), dp(18)).apply { rightMargin = dp(10); topMargin = dp(1) })
                val tx = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
                tx.addView(texto(r.titulo, Tema.T_TEXTO, cTexto, !r.ok))
                if (!r.ok) {
                    tx.addView(texto(r.detalle, Tema.T_NOTA, cTexto2, false).apply { setLineSpacing(0f, 1.2f) }, margenArriba(2))
                    if (r.accion != Soporte.Accion.NINGUNA) {
                        tx.addView(boton(r.accion.texto, cVerde, Color.BLACK) { hacerAccion(r.accion) }, margenArriba(Tema.S1))
                    }
                }
                f.addView(tx, LinearLayout.LayoutParams(0, -2, 1f))
                revision.addView(f, margenArriba(Tema.S1))
            }
            if (servicioActivo() && !CapturaService.corriendo) {
                revision.addView(boton("▶  Iniciar lectura", cVerde, Color.BLACK) {
                    d.dismiss()
                    iniciarCaptura()
                }, margenArriba(Tema.S2))
            }
        }
        pintarRevision()
        pintarAyudaAbierta = { pintarRevision() }

        col.addView(etiqueta("PREGUNTAS FRECUENTES"), margenArriba(Tema.S2))
        val buscar = EditText(this).apply {
            hint = "Busca: pop-up, banco, batería…"
            setHintTextColor(cTenue)
            setTextColor(cTexto)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, Tema.T_TEXTO)
            background = redondo(cTarjeta2, 12)
            setPadding(dp(12), dp(10), dp(12), dp(10))
            setSingleLine()
        }
        col.addView(buscar)
        val faqs = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        col.addView(faqs)
        fun pintarFaq(q: String) {
            faqs.removeAllViews()
            val clave = sinAcentos(q.trim())
            val lista = Soporte.preguntas.filter { clave.isEmpty() || sinAcentos(it.pregunta + " " + it.respuesta).contains(clave) }
            if (lista.isEmpty()) faqs.addView(texto("No encontré nada. Escríbenos por WhatsApp.", Tema.T_NOTA, cTenue, false),
                margenArriba(Tema.S1))
            lista.forEach { faqs.addView(itemPregunta(it), margenArriba(Tema.S1)) }
        }
        pintarFaq("")
        buscar.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) {}
            override fun onTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) {}
            override fun afterTextChanged(s: Editable?) { pintarFaq(s?.toString() ?: "") }
        })

        col.addView(boton("Reportar una oferta mal leída", cTarjeta2, cTexto) { reportarLecturaMala() }, margenArriba(Tema.S2))
        col.addView(boton("Escribir a soporte por WhatsApp", cTarjeta2, cTexto) {
            Soporte.escribir(this, "tengo una duda")
        }, margenArriba(Tema.S1))
        d.setOnDismissListener { pintarAyudaAbierta = null }
        d.show()
    }

    /** Ayuda con la lectura en su propia pantalla (la tarjeta que pinta [pintarDiag]). */
    private fun mostrarAyudaLectura() {
        val (d, cuerpo, _) = ventanaCompleta("Ayuda con la lectura")
        val col = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), 0, dp(16), dp(Tema.S2))
        }
        val caja = tarjeta()
        col.addView(caja)
        cuerpo.addView(ScrollView(this).apply { isVerticalScrollBarEnabled = false; addView(col) }, LinearLayout.LayoutParams(-1, -1))
        cajaDiag = caja
        pintarDiag()
        d.setOnDismissListener {
            if (cajaDiag === caja) { cajaDiag = null; estadoLecturaTxt = null }
            pintarCuenta()
        }
        d.show()
    }

    private fun mostrarBancos() {
        val cuerpo = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        cuerpo.addView(texto(
            "Algunos bancos (Nu, BBVA…) no abren mientras el lector está encendido. Páusalo, usa tu banco y " +
                "al volver a Checa te pedirá activarlo otra vez.",
            Tema.T_TEXTO, cTexto2, false
        ).apply { setLineSpacing(0f, 1.2f) })
        var d: Dialog? = null
        cuerpo.addView(boton("Pausar lector para usar mi banco", cVerde, Color.BLACK) {
            d?.dismiss()
            pausarLector()
        }, margenArriba(Tema.S2))
        cuerpo.addView(boton("Volver a activar el lector", cTarjeta2, cTexto) {
            d?.dismiss()
            abrirAccesibilidad()
        }, margenArriba(Tema.S1))
        d = hoja("Apps de banco", null, cuerpo)
    }

    /** Qué guarda Checa, dónde y por cuánto tiempo; y borrar las estadísticas. */
    private fun mostrarTusDatos() {
        val (d, cuerpo, _) = ventanaCompleta("Tus datos")
        val col = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), 0, dp(16), dp(Tema.S2))
        }
        cuerpo.addView(ScrollView(this).apply { isVerticalScrollBarEnabled = false; addView(col) }, LinearLayout.LayoutParams(-1, -1))
        fun bloque(titulo: String, puntos: List<String>) {
            val t = tarjeta()
            t.addView(texto(titulo, Tema.T_SUB, cTexto, true))
            puntos.forEach { s ->
                val f = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
                f.addView(View(this).apply { background = punto(Tema.HUESO) },
                    LinearLayout.LayoutParams(dp(6), dp(6)).apply { topMargin = dp(8); rightMargin = dp(10) })
                f.addView(texto(s, Tema.T_TEXTO, cTexto2, false).apply { setLineSpacing(0f, 1.2f) }, LinearLayout.LayoutParams(0, -2, 1f))
                t.addView(f, margenArriba(Tema.S1))
            }
            col.addView(t, margenArriba(Tema.ENTRE_TARJETAS))
        }
        bloque("En este teléfono", listOf(
            "Estadísticas de ofertas (Mejores horas y semana): los últimos 30 días.",
            "Ofertas guardadas para «Ayuda con la lectura»: 7 días, solo si está encendido.",
            "Mi día: los viajes que marcas."
        ))
        bloque("En tu cuenta de Google", listOf(
            "Tu licencia y tus equipos.",
            "Tus ajustes (filtros, gasolina, pop-up), para que regresen si cambias de teléfono."
        ))
        bloque("A soporte", listOf("Nada se envía solo. Solo lo que tú revisas y mandas por WhatsApp."))
        col.addView(boton("Borrar estadísticas", Tema.ROJO_SUAVE, cRojo) {
            AlertDialog.Builder(this)
                .setMessage("Se borran Mejores horas, la semana y las ofertas del día. Tus ajustes, tu cuenta y Mi día no cambian. ¿Borrar?")
                .setPositiveButton("Borrar") { _, _ -> Historial.borrar(this); actualizarResumen() }
                .setNegativeButton("Cancelar", null)
                .show()
        }, margenArriba(Tema.S2))
        d.show()
    }

    /** «Una oferta se leyó mal»: abre el reporte con la última oferta guardada. */
    private fun reportarLecturaMala() {
        if (!prefs.diagnostico) {
            Toast.makeText(this, "Enciende «Guardar pantallas de ofertas» en Ayuda con la lectura y espera la siguiente oferta.",
                Toast.LENGTH_LONG).show()
            return
        }
        val ultima = Guardado.lista(this).take(1)
        if (ultima.isEmpty()) {
            Toast.makeText(this, "Aún no hay ofertas guardadas. Cuando te llegue una, vuelve aquí.", Toast.LENGTH_LONG).show()
            return
        }
        reporteMotivo = 0
        mostrarReporte(ultima)
    }

    // =====================================================================
    // Actualizaciones y compartir
    // =====================================================================

    /** Muestra u oculta la tarjeta de Inicio; la ventana sale cada vez que se abre Checa (máx. cada 30 min) hasta actualizar. */
    private fun pintarActualizacion(v: String?, avisar: Boolean) {
        if (!::tarjetaActualizacion.isInitialized) return
        tarjetaActualizacion.visibility = if (v == null) View.GONE else View.VISIBLE
        botonBuscarAct?.text = if (v == null) "Buscar actualización" else "Actualizar a la $v"
        if (v == null) { Actualizacion.quitarNotificacion(this); return }
        actTexto.text = "Checa $v ya está lista (tienes la ${Actualizacion.instalada(this)}). " +
            "Se instala encima: no pierdes tu cuenta, ajustes ni historial."
        if (avisar && !hayVentana() && Actualizacion.avisoToca(this, v)) {
            Actualizacion.marcarAviso(this, v)
            dialogoActualizacion(v)
        }
    }

    private fun dialogoActualizacion(v: String) {
        val cuerpo = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        cuerpo.addView(texto(
            "Ya está lista la versión $v de Checa. Toca «Actualizar»: se descarga aquí mismo y Android te pide confirmar la instalación.",
            Tema.T_TEXTO, cTexto2, false
        ).apply { setLineSpacing(0f, 1.2f) })
        cuerpo.addView(texto("No pierdes tu cuenta, ajustes ni historial.", Tema.T_NOTA, cTenue, false), margenArriba(Tema.S1))
        cuerpo.addView(boton("Actualizar", cVerde, Color.BLACK) {
            dialogoAct?.dismiss()
            actualizarApp()
        }, margenArriba(Tema.S2))
        cuerpo.addView(boton("Después", cTarjeta2, cTexto) { dialogoAct?.dismiss() }, margenArriba(Tema.S1))
        dialogoAct = hoja("Nueva versión", null, cuerpo)
    }

    private var descargandoApp = false
    private var pidioPermisoInstalar = false

    /** Descarga la versión nueva dentro de la app y abre el instalador de Android (el usuario confirma). */
    private fun actualizarApp() {
        if (descargandoApp) return
        if (!Actualizacion.puedeInstalar(this)) {
            pidioPermisoInstalar = true
            Toast.makeText(this, "Activa «Permitir de esta fuente» para Checa y regresa.", Toast.LENGTH_LONG).show()
            Actualizacion.pedirPermisoInstalar(this)
            return
        }
        descargandoApp = true
        val cuerpo = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        val avance = texto("Descargando… 0 %", Tema.T_TEXTO, cTexto2, false)
        cuerpo.addView(avance)
        cuerpo.addView(texto("No cierres la app. Al terminar, Android te pedirá tocar «Instalar».", Tema.T_NOTA, cTenue, false)
            .apply { setLineSpacing(0f, 1.2f) }, margenArriba(Tema.S1))
        val d = hoja("Actualizando Checa", null, cuerpo)
        d.setCancelable(false)
        Actualizacion.descargar(this, { pct ->
            avance.text = if (pct >= 0) "Descargando… $pct %" else "Descargando…"
        }) { apk ->
            descargandoApp = false
            d.dismiss()
            if (apk == null || !Actualizacion.instalar(this, apk)) {
                Toast.makeText(this, "No se pudo descargar aquí. Te abro la página para bajarla.", Toast.LENGTH_LONG).show()
                Actualizacion.abrirDescarga(this)
            }
        }
    }

    private fun buscarActualizacion() {
        Actualizacion.pendiente(this)?.let { actualizarApp(); return }
        botonBuscarAct?.text = "Buscando…"
        Actualizacion.revisar(this, forzar = true) { v, ok ->
            pintarActualizacion(v, false)
            when {
                v != null -> dialogoActualizacion(v)
                ok -> Toast.makeText(this, "Ya tienes la versión más nueva.", Toast.LENGTH_SHORT).show()
                else -> Toast.makeText(this, "Sin conexión. Inténtalo cuando tengas internet.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    /** Tarjeta «Invita a un amigo» (Inicio y Cuenta); su contenido lo pinta [pintarInvitar]. */
    private fun tarjetaCompartir(): View {
        val t = tarjeta()
        cajasCompartir.add(t)
        pintarInvitar()
        return t
    }

    private fun pintarInvitar() {
        val codigo = Cuenta.miCodigo(this)
        cajasCompartir.forEach { t ->
            t.removeAllViews()
            t.addView(cabeceraTarjeta(Icono.COMPARTIR, "Invita a un amigo",
                if (codigo != null) "Si paga cualquier paquete, los dos ganan 1 mes gratis"
                else "¿Un compa también maneja? Pásale la app", false).first)
            if (codigo != null) {
                val fila = LinearLayout(this).apply {
                    orientation = LinearLayout.HORIZONTAL
                    gravity = Gravity.CENTER_VERTICAL
                    background = redondo(cTarjeta2, Tema.R_BOTON)
                    setPadding(dp(Tema.S2), dp(12), dp(Tema.S2), dp(12))
                }
                fila.addView(texto("Tu código", Tema.T_NOTA, cTenue, false), LinearLayout.LayoutParams(0, -2, 1f))
                fila.addView(numero(codigo, Tema.T_SUB, cTexto))
                t.addView(fila, margenArriba(Tema.S2))
            } else if (!Cuenta.conectada(this)) {
                t.addView(texto("Entra con Google para tener tu código y ganar meses gratis.", Tema.T_NOTA, cTenue, false)
                    .apply { setLineSpacing(0f, 1.2f) }, margenArriba(Tema.S1))
            }
            t.addView(boton("Compartir con un amigo", cVerde, Color.BLACK) { Actualizacion.compartir(this, Cuenta.miCodigo(this)) },
                margenArriba(Tema.S2))
        }
        pintarInvitado()
    }

    /** «¿Te invitó alguien?»: solo antes del primer pago y si la cuenta aún no tiene código de amigo. */
    private fun pintarInvitado() {
        val caja = cajaInvitado ?: return
        caja.removeAllViews()
        val usado = Cuenta.invitacion(this)
        if (usado != null || Licencia.estado(this).pagada) return
        val t = tarjeta()
        t.addView(texto("¿Te invitó alguien?", Tema.T_SUB, cTexto, true))
        val pendiente = Cuenta.invitacionPendiente(this)
        if (pendiente != null) {
            t.addView(texto("Código $pendiente guardado. Se aplica cuando entres con Google; al pagar tu primer paquete, tú y tu amigo ganan 1 mes gratis.",
                Tema.T_TEXTO, cTexto2, false).apply { setLineSpacing(0f, 1.2f) }, margenArriba(Tema.S1))
        } else {
            t.addView(texto("Escribe el código de tu amigo. Al pagar tu primer paquete, los dos ganan 1 mes gratis.",
                Tema.T_TEXTO, cTexto2, false).apply { setLineSpacing(0f, 1.2f) }, margenArriba(Tema.S1))
            val campo = EditText(this).apply {
                hint = "ABCD-EF2"
                setHintTextColor(cTenue)
                setTextColor(cTexto)
                setTextSize(TypedValue.COMPLEX_UNIT_SP, Tema.T_SUB)
                typeface = Fuentes.numero
                isSingleLine = true
                inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_CAP_CHARACTERS
                background = redondo(cTarjeta2, Tema.R_BOTON)
                setPadding(dp(Tema.S2), dp(12), dp(Tema.S2), dp(12))
                minHeight = dp(Tema.ALTO_TOQUE)
            }
            t.addView(campo, margenArriba(Tema.S2))
            t.addView(boton("Aplicar código", cTarjeta2, cTexto) {
                Cuenta.usarCodigo(this, campo.text.toString()) { msj, _ ->
                    Toast.makeText(this, msj, Toast.LENGTH_LONG).show()
                    pintarInvitar()
                }
            }, margenArriba(Tema.S1))
        }
        caja.addView(t, margenTarjeta())
    }

    /** Ventana «¡Ganaste 1 mes!» cuando suben los premios de la cuenta. */
    private fun avisarPremios() {
        val n = Cuenta.premiosNuevos(this)
        if (n <= 0 || guia?.isShowing == true || dialogoCuenta?.isShowing == true) return
        Cuenta.marcarPremiosVistos(this)
        val cuerpo = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        cuerpo.addView(texto(
            (if (n == 1) "Ganaste 1 mes gratis" else "Ganaste $n meses gratis") +
                " con «Invita a un amigo». Ya se sumó a tu licencia.",
            Tema.T_TEXTO, cTexto2, false
        ).apply { setLineSpacing(0f, 1.2f) })
        cuerpo.addView(boton("Invitar a otro amigo", cVerde, Color.BLACK) {
            dialogoCuenta?.dismiss()
            Actualizacion.compartir(this, Cuenta.miCodigo(this))
        }, margenArriba(Tema.S2))
        dialogoCuenta = hoja("¡Ganaste ${if (n == 1) "1 mes" else "$n meses"}!", DORADO, cuerpo)
    }

    /** Si llegó desde el link de un amigo, la web dejó «CHECA-REF:CODIGO» en el portapapeles. */
    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (!hasFocus) return
        val sp = getSharedPreferences("invitar", Context.MODE_PRIVATE)
        val veces = sp.getInt("portapapeles", 0)
        if (veces >= 3 || Cuenta.invitacion(this) != null || Cuenta.invitacionPendiente(this) != null ||
            Licencia.estado(this).pagada) return
        sp.edit().putInt("portapapeles", veces + 1).apply()
        val clip = try {
            (getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager)
                .primaryClip?.getItemAt(0)?.coerceToText(this)?.toString()
        } catch (_: Exception) { null } ?: return
        val c = Regex("CHECA-REF:([A-Za-z0-9-]{7,8})").find(clip)?.groupValues?.get(1) ?: return
        if (Cuenta.normalizarCodigo(c) == null || Cuenta.normalizarCodigo(c) == Cuenta.miCodigo(this)) return
        sp.edit().putInt("portapapeles", 3).apply()
        Cuenta.usarCodigo(this, c) { msj, ok ->
            if (ok) Toast.makeText(this, "Te invitó un amigo. $msj", Toast.LENGTH_LONG).show()
            pintarInvitar()
        }
    }

    /** Lleva a la pestaña Cuenta, a la parte de los paquetes. */
    private fun irAPaquetes() {
        dialogoCuenta?.dismiss()
        mostrarPestana(3)
        cuentaScroll.post { cuentaScroll.smoothScrollTo(0, paquetesVista.top) }
    }

    private fun tarjetaPaquete(
        plan: String, duracion: String, nombre: String, precio: String, precioFundador: String, sub: String,
        beneficios: List<String>, destacado: Boolean
    ): View {
        val t = tarjeta()
        if (destacado) t.background = GradientDrawable().apply {
            cornerRadius = dp(Tema.R_TARJETA).toFloat()
            setColor(cTarjeta)
            setStroke(dp(2), Tema.HUESO)
        }
        val cab = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        cab.addView(texto("$duracion · $nombre", Tema.T_NOTA, if (destacado) Tema.HUESO else cTexto2, true).apply {
            letterSpacing = 0.04f
        }, LinearLayout.LayoutParams(0, -2, 1f))
        if (destacado) cab.addView(texto("Más popular", Tema.T_NOTA, Color.BLACK, true).apply {
            setPadding(dp(10), dp(3), dp(10), dp(3))
            background = redondo(Tema.HUESO, 50)
        })
        t.addView(cab)
        val vPrecio = texto("$precio MXN", Tema.T_NUMERO, cTexto, true)
        val vSub = texto(sub, Tema.T_NOTA, if (destacado) Tema.HUESO else cTenue, destacado)
        t.addView(vPrecio, margenArriba(4))
        t.addView(vSub, margenArriba(2))
        // Precio de fundador (40 % menos) si la cuenta usó la beta; el cobro real lo decide mp-pago.
        val pintar = {
            val f = Cuenta.precioBeta(this)
            vPrecio.text = if (f) "$precioFundador MXN" else "$precio MXN"
            vSub.text = if (f) "Precio de fundador: 40 % menos · antes $precio" else sub
            vSub.setTextColor(if (f) DORADO else if (destacado) Tema.HUESO else cTenue)
        }
        pintarPrecios.add(pintar)
        pintar()
        beneficios.forEach { b ->
            val fila = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
            fila.addView(icono(Icono.OK, if (destacado) Tema.HUESO else cTexto2, 16), LinearLayout.LayoutParams(dp(16), dp(16)).apply {
                rightMargin = dp(Tema.S1)
                topMargin = dp(2)
            })
            fila.addView(texto(b, Tema.T_TEXTO, cTexto, false).apply { setLineSpacing(0f, 1.15f) },
                LinearLayout.LayoutParams(0, -2, 1f))
            t.addView(fila, margenArriba(Tema.S1))
        }
        t.addView(boton("Pagar con Mercado Pago", if (destacado) cVerde else cTarjeta2, if (destacado) Color.BLACK else cTexto) {
            pagarPaquete(plan)
        }, margenArriba(Tema.S2))
        return t
    }

    /** Abre Mercado Pago con el paquete; la licencia se activa sola en la cuenta Google. */
    private fun pagarPaquete(plan: String) {
        if (!Cuenta.conectada(this)) {
            Toast.makeText(this, "Primero entra con tu cuenta de Google: ahí se guarda tu licencia", Toast.LENGTH_LONG).show()
            entrarConGoogle()
            return
        }
        Toast.makeText(this, "Abriendo Mercado Pago…", Toast.LENGTH_SHORT).show()
        Cuenta.crearPago(this, plan) { url, error ->
            if (url == null) {
                Toast.makeText(this, error, Toast.LENGTH_LONG).show()
                return@crearPago
            }
            try {
                startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                esperandoPago = true
            } catch (_: Exception) {
                Toast.makeText(this, "No hay navegador para abrir Mercado Pago", Toast.LENGTH_LONG).show()
            }
        }
    }

    // =====================================================================
    // Licencia
    // =====================================================================

    /** Repinta el precio de cada paquete (normal o de fundador). */
    private val pintarPrecios = mutableListOf<() -> Unit>()

    private fun pintarLicencia() {
        val e = Licencia.estado(this)
        val finBeta = e.beta && !e.pagada && e.diasRestantes <= DIAS_AVISO_FIN_BETA
        // En Inicio solo cuando importa: prueba, licencia vencida o último mes de la beta (el plan vive en Cuenta).
        tarjetaLicencia.visibility = if (finBeta || (!e.beta && !e.pagada)) View.VISIBLE else View.GONE
        if (finBeta) {
            licTitulo.text = "La beta gratis termina en ${e.diasRestantes} ${if (e.diasRestantes == 1) "día" else "días"}"
            licTitulo.setTextColor(cAmarillo)
            licTexto.text = "El ${Licencia.fechaBonita(Licencia.BETA_HASTA)} Checa empieza a cobrar. " +
                "Quienes la probaron en la beta tendrán precio de fundador: 40 % menos de por vida." +
                (if (Cuenta.conectada(this)) " Tu cuenta ya está registrada." else " Entra con tu cuenta de Google para asegurarlo.")
        } else if (e.beta && !e.pagada) {
            licTitulo.text = "Beta gratis · todo incluido"
            licTitulo.setTextColor(cTexto)
            licTexto.text = "Usa Checa sin costo hasta el ${Licencia.fechaBonita(Licencia.BETA_HASTA)}. Gracias por probarla."
        } else if (e.activa && !e.pagada) {
            licTitulo.text = "Prueba gratis: ${e.diasRestantes} ${if (e.diasRestantes == 1) "día" else "días"}"
            licTitulo.setTextColor(cTexto)
            licTexto.text = "Úsala en tu turno. Para seguir después de la prueba, activa tu licencia."
        } else if (!e.activa && Cuenta.necesitaRevision(this)) {
            licTitulo.text = "Revisa tu licencia"
            licTitulo.setTextColor(cAmarillo)
            licTexto.text = "Llevas más de ${Cuenta.DIAS_SIN_RED} días sin internet. Abre Checa con internet para seguir."
        } else if (!e.activa) {
            licTitulo.text = "Tu licencia terminó"
            licTitulo.setTextColor(cAmarillo)
            licTexto.text = "Renueva tu paquete para volver a ver los pop-ups."
        }
        insignias.forEach { it.visibility = if (e.fundador) View.VISIBLE else View.GONE }
        pintarPrecios.forEach { it() }
        notaComparador?.visibility = if (e.basico) View.VISIBLE else View.GONE
        // Con licencia sin vencimiento (fundadores) no hay nada que comprar.
        if (::paquetesVista.isInitialized) {
            paquetesVista.visibility = if (e.beta || (e.pagada && e.vence >= Licencia.PERMANENTE)) View.GONE else View.VISIBLE
        }
        // En la beta no hay nada que comprar: se oculta el botón de paquetes de Inicio.
        if (e.beta) {
            botonPaquetesInicio.text = "Entrar con Google"
            botonPaquetesInicio.setOnClickListener { entrarConGoogle() }
            botonPaquetesInicio.visibility = if (finBeta && !Cuenta.conectada(this)) View.VISIBLE else View.GONE
        } else {
            botonPaquetesInicio.text = "Ver paquetes"
            botonPaquetesInicio.setOnClickListener { irAPaquetes() }
            botonPaquetesInicio.visibility = View.VISIBLE
        }
        pintarCuenta()
    }

    /** Etiqueta dorada "★ Miembro fundador" (solo visible con licencia de fundador). */
    private fun insigniaFundador() = texto("★ Miembro fundador", Tema.T_NOTA, DORADO, true).apply {
        setPadding(dp(10), dp(4), dp(10), dp(4))
        background = GradientDrawable().apply {
            cornerRadius = dp(Tema.R_BOTON).toFloat()
            setColor(Color.argb(30, Color.red(DORADO), Color.green(DORADO), Color.blue(DORADO)))
            setStroke(dp(1), DORADO)
        }
        visibility = View.GONE
        insignias.add(this)
    }

    // =====================================================================
    // Cuenta Google (licencia ligada a la cuenta, hasta 2 equipos)
    // =====================================================================

    // =====================================================================
    // Guía de primer uso (maqueta https://claude.ai/artifact/2LAgUcZUwZzKk98XqXsNy5)
    // =====================================================================

    private var guia: Dialog? = null
    private var guiaPaso = 0
    private lateinit var guiaCuerpo: LinearLayout
    private lateinit var guiaPie: LinearLayout
    private lateinit var guiaProgreso: LinearLayout
    private lateinit var guiaOmitir: TextView
    private var guiaHoraTocada = false
    private var guiaCambio = false

    /** Solo en instalaciones nuevas: sin permiso, sin licencia pagada y sin cuenta. */
    private fun guiaSiHaceFalta(): Boolean {
        if (prefs.guiaVista) return false
        if (servicioActivo() || Licencia.estado(this).pagada || Cuenta.conectada(this)) {
            prefs.guiaVista = true
            return false
        }
        mostrarGuia()
        return true
    }

    private fun mostrarGuia() {
        guia?.dismiss()
        guiaHoraTocada = false
        guiaCambio = false
        val d = Dialog(this, android.R.style.Theme_DeviceDefault_NoActionBar)
        val raiz = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(cFondo)
        }
        val arriba = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(20), dp(20), dp(12), 0)
        }
        guiaProgreso = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        arriba.addView(guiaProgreso, LinearLayout.LayoutParams(0, dp(4), 1f))
        guiaOmitir = texto("Omitir", Tema.T_TEXTO, cTenue, false).apply {
            typeface = fuenteMedia
            gravity = Gravity.CENTER
            minHeight = dp(Tema.ALTO_TOQUE)
            setPadding(dp(12), 0, dp(8), 0)
            isClickable = true
            setOnClickListener { guia?.dismiss() }
        }
        arriba.addView(guiaOmitir)
        raiz.addView(arriba)
        guiaCuerpo = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(Tema.S3), dp(20), dp(Tema.S2))
        }
        raiz.addView(ScrollView(this).apply {
            isVerticalScrollBarEnabled = false
            addView(guiaCuerpo)
        }, LinearLayout.LayoutParams(-1, 0, 1f))
        guiaPie = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(Tema.S2), dp(20), dp(Tema.S3))
        }
        raiz.addView(guiaPie)
        d.setContentView(raiz)
        d.window?.statusBarColor = cFondo
        d.window?.navigationBarColor = cFondo
        d.setOnDismissListener {
            prefs.guiaVista = true
            Cuenta.marcarBienvenida(this)
            if (guiaCambio) refrescarFiltrosYAjustes()
            guia = null
        }
        guia = d
        pintarGuia(2)
        d.show()
    }

    private fun pintarGuia(paso: Int) {
        if (guia == null) return
        guiaPaso = paso
        guiaProgreso.removeAllViews()
        // Avance del arranque de 3 pasos: 1 permisos (ya hecho), 2 tus números, 3 así se verá.
        for (i in 0..2) {
            val lleno = i == 0 || (i == 1 && paso >= 2) || (i == 2 && paso >= 4)
            guiaProgreso.addView(View(this).apply { background = redondo(if (lleno) Tema.HUESO else cTarjeta2, 2) },
                LinearLayout.LayoutParams(0, -1, 1f).apply { if (i > 0) leftMargin = dp(6) })
        }
        guiaOmitir.visibility = if (paso < 4) View.VISIBLE else View.INVISIBLE
        guiaCuerpo.removeAllViews()
        guiaPie.removeAllViews()
        val c = guiaCuerpo
        val atras = texto("Atrás", Tema.T_TEXTO, cTenue, false).apply {
            typeface = fuenteMedia
            gravity = Gravity.CENTER
            minHeight = dp(Tema.ALTO_TOQUE)
            isClickable = true
            setOnClickListener { pintarGuia(if (guiaPaso >= 4) 2 else guiaPaso - 1) }
        }
        fun titulo(s: String) = texto(s, 28f, cTexto, true).apply { setLineSpacing(0f, 1.1f) }
        fun parrafo(s: String) = texto(s, Tema.T_SUB, cTexto2, false).apply { setLineSpacing(0f, 1.3f) }
        fun pasoDe(n: Int) = etiqueta("PASO $n DE 3").apply { setPadding(0, 0, 0, dp(4)) }
        fun principal(s: String, accion: () -> Unit) = guiaPie.addView(boton(s, cVerde, Color.BLACK, accion).apply {
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 15f)
        }, LinearLayout.LayoutParams(-1, dp(52)))

        when (paso) {
            0 -> {
                c.addView(ImageView(this).apply { setImageResource(R.mipmap.ic_launcher) },
                    LinearLayout.LayoutParams(dp(88), dp(88)).apply { gravity = Gravity.CENTER_HORIZONTAL; topMargin = dp(Tema.S2) })
                c.addView(titulo("Lo que de verdad te deja cada viaje").apply { gravity = Gravity.CENTER }, margenArriba(Tema.S3))
                c.addView(parrafo("Cuando te llega una oferta en Uber o DiDi, Checa te dice en un segundo si conviene.")
                    .apply { gravity = Gravity.CENTER }, margenArriba(Tema.S2))
                val t = tarjeta()
                listOf(
                    cVerde to "Ganancia neta después de gasolina",
                    cAmarillo to "\$/km y \$/hora contra tus metas",
                    cRojo to "Te lo dice en voz alta: Aceptar, Considerar o Rechazar"
                ).forEachIndexed { i, (col, s) ->
                    val f = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
                    f.addView(View(this).apply { background = punto(col) },
                        LinearLayout.LayoutParams(dp(10), dp(10)).apply { topMargin = dp(6); rightMargin = dp(12) })
                    f.addView(texto(s, Tema.T_SUB, cTexto2, false), LinearLayout.LayoutParams(0, -2, 1f))
                    t.addView(f, if (i > 0) margenArriba(12) else LinearLayout.LayoutParams(-1, -2))
                }
                c.addView(t, margenArriba(Tema.S3))
                if (!Licencia.estado(this).pagada) {
                    val aviso = if (Licencia.estado(this).beta) "Checa es gratis con todo incluido durante la beta."
                        else "Tienes ${Licencia.DIAS_PRUEBA} días gratis con todo incluido."
                    c.addView(texto(aviso, Tema.T_TEXTO, cTenue, false)
                        .apply { gravity = Gravity.CENTER }, margenArriba(Tema.S2))
                }
                principal("Empezar") { pintarGuia(1) }
                if (!Cuenta.conectada(this)) {
                    guiaPie.addView(boton("Continuar con Google", Color.WHITE, Color.BLACK) {
                        Cuenta.marcarBienvenida(this)
                        pintarGuia(1)
                        entrarConGoogle()
                    }.apply { setTextSize(TypedValue.COMPLEX_UNIT_SP, 15f) },
                        LinearLayout.LayoutParams(-1, dp(52)).apply { topMargin = dp(Tema.S1) })
                }
            }
            1 -> {
                val listo = servicioActivo()
                c.addView(pasoDe(1))
                c.addView(titulo("Activa el lector"), margenArriba(Tema.S1))
                c.addView(parrafo("Checa necesita permiso para ver la oferta en tu pantalla. Solo lee Uber y DiDi; no toca nada ni acepta viajes por ti."),
                    margenArriba(Tema.S2))
                // Android 13+: los ajustes restringidos se permiten ANTES de activar la Accesibilidad.
                if (Build.VERSION.SDK_INT >= 33 && !listo && ajustesRestringidosPermitidos() != true) {
                    val r = tarjeta()
                    r.addView(texto("Antes: permite los ajustes restringidos", Tema.T_TEXTO, cTexto, false).apply { typeface = fuenteMedia })
                    r.addView(texto(
                        "Si no, al activar el lector Android dice «A la app se le negó el acceso». " +
                            "En Información de la app toca ⋮ (arriba a la derecha) → «Permitir configuración restringida». " +
                            "Si la opción no aparece, toca «Abrir Accesibilidad», intenta activar el lector una vez y vuelve aquí.",
                        Tema.T_NOTA, cTenue, false
                    ).apply { setLineSpacing(0f, 1.3f) }, margenArriba(Tema.S1))
                    r.addView(boton("Información de la app", cTarjeta2, cTexto) { abrirInfoApp() }, margenArriba(12))
                    c.addView(r, margenArriba(Tema.S2))
                }
                val t = tarjeta()
                val fila = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL }
                fila.addView(texto("Checa – lector de ofertas", Tema.T_SUB, cTexto, false).apply { typeface = fuenteMedia },
                    LinearLayout.LayoutParams(0, -2, 1f))
                fila.addView(texto(if (listo) "Listo" else "Falta", Tema.T_NOTA, if (listo) cVerde else cAmarillo, true).apply {
                    setPadding(dp(10), dp(4), dp(10), dp(4))
                    background = redondo(if (listo) Tema.VERDE_SUAVE else Tema.AMARILLO_SUAVE, 50)
                })
                t.addView(fila)
                listOf("Toca «Abrir Accesibilidad»", "Busca «Checa – lector de ofertas». En Samsung está dentro de «Aplicaciones instaladas»; en OPPO, en «Aplicaciones descargadas»", "Actívalo y regresa aquí")
                    .forEachIndexed { i, s ->
                        val f = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL }
                        f.addView(texto("${i + 1}", Tema.T_NOTA, cTexto, true).apply {
                            gravity = Gravity.CENTER
                            background = punto(cTarjeta2)
                        }, LinearLayout.LayoutParams(dp(28), dp(28)).apply { rightMargin = dp(12) })
                        f.addView(texto(s, Tema.T_TEXTO, cTexto2, false), LinearLayout.LayoutParams(0, -2, 1f))
                        t.addView(f, margenArriba(12))
                    }
                c.addView(t, margenArriba(Tema.ENTRE_TARJETAS))
                if (listo) principal("Siguiente") { pintarGuia(2) } else principal("Abrir Accesibilidad") { abrirAccesibilidad() }
                guiaPie.addView(atras, margenArriba(Tema.S1))
            }
            2 -> {
                c.addView(pasoDe(2))
                c.addView(titulo("Tus números"), margenArriba(Tema.S1))
                c.addView(parrafo("Con esto Checa pinta cada oferta de verde, amarillo o rojo. Lo cambias cuando quieras en Mis metas."),
                    margenArriba(Tema.S2))
                c.addView(campoGuia("Por kilómetro", { "\$" + num(prefs.minPorKm) + "/km" }) { signo ->
                    prefs.minPorKm = (prefs.minPorKm + 0.5f * signo).coerceIn(1f, 50f)
                    if (!guiaHoraTocada) prefs.minPorHora = Math.round(prefs.minPorKm * 20).toFloat()
                    pintarGuia(2)
                }, margenArriba(Tema.S2))
                c.addView(campoGuia("Por hora", { "\$" + num(prefs.minPorHora) + "/h" }) { signo ->
                    guiaHoraTocada = true
                    prefs.minPorHora = (prefs.minPorHora + 10f * signo).coerceIn(10f, 1000f)
                }, margenArriba(Tema.ENTRE_TARJETAS))
                c.addView(texto("Si no sabes, deja \$8/km y ajústalo después de unos días.", Tema.T_NOTA, cTenue, false),
                    margenArriba(Tema.S1))
                // Tu auto, en la misma pantalla: para restar la gasolina y mostrar lo que te queda.
                c.addView(etiqueta("TU AUTO"), margenArriba(Tema.S1))
                val costo = texto("", 22f, cTexto, true)
                fun pintarCosto() { costo.text = "\$" + num1((prefs.precioLitro / prefs.kmPorLitro).toDouble()) }
                c.addView(campoGuia("Rendimiento", { num(prefs.kmPorLitro) + " km/l" }) { signo ->
                    prefs.kmPorLitro = (prefs.kmPorLitro + signo).coerceIn(4f, 40f)
                    pintarCosto()
                }, margenArriba(Tema.S2))
                c.addView(campoGuia("Precio de la gasolina", { "\$" + num(prefs.precioLitro) + "/l" }) { signo ->
                    prefs.precioLitro = (prefs.precioLitro + 0.5f * signo).coerceIn(5f, 60f)
                    pintarCosto()
                }, margenArriba(Tema.ENTRE_TARJETAS))
                val t = tarjeta()
                val f = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL }
                f.addView(texto("Te cuesta cada km", Tema.T_SUB, cTexto2, false), LinearLayout.LayoutParams(0, -2, 1f))
                f.addView(costo)
                t.addView(f)
                pintarCosto()
                c.addView(t, margenArriba(Tema.ENTRE_TARJETAS))
                principal("Siguiente") { pintarGuia(4) }
            }
            else -> {
                val o = ofertaEjemplo()
                val accion = when (o.nivel(prefs)) {
                    Oferta.VERDE -> "Aceptar"
                    Oferta.AMARILLO -> "Considerar"
                    else -> "Rechazar"
                }
                c.addView(pasoDe(3))
                c.addView(titulo("Así se verá"), margenArriba(Tema.S1))
                c.addView(parrafo("Cuando llegue una oferta, este aviso sale encima de Uber o DiDi y te dice «$accion» en voz alta."),
                    margenArriba(Tema.S2))
                c.addView(Popup(this, prefs, WindowManager.LayoutParams.TYPE_APPLICATION).construir(o, false),
                    margenArriba(Tema.S2))
                c.addView(texto("Oferta de ejemplo con tus metas. Si regresas y cambias tus metas, cambia el color.",
                    Tema.T_NOTA, cTenue, false).apply { setLineSpacing(0f, 1.3f) }, margenArriba(Tema.S1))
                val t = tarjeta()
                t.addView(texto("Al tocar Iniciar lectura", Tema.T_TEXTO, cTexto, false).apply { typeface = fuenteMedia })
                t.addView(texto(
                    "Android te pregunta qué compartir: elige «Pantalla completa» → Empezar. Si reinicias el teléfono, vuelve a tocar ▶.",
                    Tema.T_NOTA, cTenue, false
                ).apply { setLineSpacing(0f, 1.3f) }, margenArriba(Tema.S1))
                c.addView(t, margenArriba(Tema.S2))
                principal("▶  Iniciar lectura") {
                    guia?.dismiss()
                    iniciarCaptura()
                }
                guiaPie.addView(atras, margenArriba(Tema.S1))
            }
        }
    }

    /** Campo con − y + para la guía. [cambio] recibe -1 o +1 y guarda en prefs. */
    private fun campoGuia(etq: String, valor: () -> String, cambio: (Int) -> Unit): View {
        val t = tarjeta()
        t.addView(texto(etq, Tema.T_TEXTO, cTexto2, false))
        val f = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL }
        val v = texto(valor(), 30f, cTexto, true).apply { gravity = Gravity.CENTER }
        fun tecla(s: String, signo: Int) = texto(s, 22f, cTexto, false).apply {
            gravity = Gravity.CENTER
            background = ripple(cTarjeta2, Tema.R_BOTON)
            isClickable = true
            setOnClickListener {
                guiaCambio = true
                cambio(signo)
                v.text = valor()
            }
        }
        f.addView(tecla("−", -1), LinearLayout.LayoutParams(dp(48), dp(48)))
        f.addView(v, LinearLayout.LayoutParams(0, -2, 1f))
        f.addView(tecla("+", 1), LinearLayout.LayoutParams(dp(48), dp(48)))
        t.addView(f, margenArriba(12))
        return t
    }

    /** Vuelve a armar Filtros y Ajustes para que muestren lo que se eligió en la guía. */
    private fun refrescarFiltrosYAjustes() {
        val actual = pestanas.indexOfFirst { it.visibility == View.VISIBLE }.coerceAtLeast(0)
        listOf(1 to { pantallaFiltros() }, 2 to { pantallaAjustes() }).forEach { (i, crear) ->
            contenido.removeView(pestanas[i])
            val nueva = crear()
            pestanas[i] = nueva
            contenido.addView(nueva, i, FrameLayout.LayoutParams(-1, -1))
        }
        mostrarPestana(actual, false)
        actualizarEstado()
    }

    private var dialogoCuenta: Dialog? = null

    /** Entrar con Google es obligatorio: esta pantalla tapa la app hasta que haya sesión. */
    private fun pantallaAcceso(): View {
        val col = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            setPadding(dp(Tema.S3), dp(Tema.S3), dp(Tema.S3), dp(Tema.S3))
        }
        col.addView(ImageView(this).apply { setImageResource(R.mipmap.ic_launcher) },
            LinearLayout.LayoutParams(dp(88), dp(88)).apply { gravity = Gravity.CENTER_HORIZONTAL })
        col.addView(texto("Checa", Tema.T_TITULO, cTexto, true).apply { gravity = Gravity.CENTER }, margenArriba(Tema.S2))
        col.addView(texto("Lo que de verdad te deja cada viaje.", Tema.T_TEXTO, cTexto2, false).apply {
            gravity = Gravity.CENTER
        }, margenArriba(4))
        col.addView(boton("Continuar con Google", Color.WHITE, Color.BLACK) { entrarConGoogle() }, margenArriba(Tema.S4))
        if (Licencia.estado(this).beta) {
            col.addView(boton("Entrar sin cuenta (beta)", cTarjeta2, cTexto) { entrarSinCuenta() }, margenArriba(Tema.S1))
        }
        col.addView(texto(
            "Entra con tu cuenta para activar Checa. Tu licencia y tus ajustes (filtros, gasolina, pop-up) " +
                "se guardan en ella: si cambias de celular o reinstalas, solo vuelves a entrar.",
            Tema.T_NOTA, cTenue, false
        ).apply { gravity = Gravity.CENTER; setLineSpacing(0f, 1.2f) }, margenArriba(Tema.S2))
        return ScrollView(this).apply {
            setBackgroundColor(cFondo)
            isClickable = true
            isFillViewport = true
            addView(FrameLayout(this@MainActivity).apply {
                addView(col, FrameLayout.LayoutParams(minOf(resources.displayMetrics.widthPixels, dp(480)), -2, Gravity.CENTER))
            }, ViewGroup.LayoutParams(-1, -1))
        }
    }

    // =====================================================================
    // Permisos: se piden todos juntos después de entrar con Google
    // =====================================================================

    private var dialogoPermisos: Dialog? = null
    private var permisosAlTerminar: (() -> Unit)? = null
    private lateinit var permisosCuerpo: LinearLayout
    private lateinit var permisosPie: LinearLayout

    private fun permisoNotificaciones() = Build.VERSION.SDK_INT < 33 ||
        checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED

    private fun permisoBateria() =
        (getSystemService(Context.POWER_SERVICE) as android.os.PowerManager).isIgnoringBatteryOptimizations(packageName)

    private fun esXiaomi() = Build.MANUFACTURER.lowercase().let { it.contains("xiaomi") || it.contains("redmi") || it.contains("poco") }

    private fun faltanPermisos() = !servicioActivo() || !permisoNotificaciones() || !permisoBateria()

    private var pidioNotificaciones = false

    private fun mostrarPermisos(alTerminar: (() -> Unit)? = null) {
        if (dialogoPermisos?.isShowing == true) {
            if (alTerminar != null) permisosAlTerminar = alTerminar
            pintarPermisos()
            return
        }
        permisosAlTerminar = alTerminar
        val d = Dialog(this, android.R.style.Theme_DeviceDefault_NoActionBar)
        val raiz = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(cFondo)
        }
        permisosCuerpo = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(Tema.S3), dp(20), dp(Tema.S2))
        }
        raiz.addView(ScrollView(this).apply {
            isVerticalScrollBarEnabled = false
            addView(permisosCuerpo)
        }, LinearLayout.LayoutParams(-1, 0, 1f))
        permisosPie = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(Tema.S2), dp(20), dp(Tema.S3))
        }
        raiz.addView(permisosPie)
        d.setContentView(raiz)
        d.setCancelable(false)
        d.window?.statusBarColor = cFondo
        d.window?.navigationBarColor = cFondo
        d.setOnDismissListener {
            prefs.permisosVistos = true
            dialogoPermisos = null
            val seguir = permisosAlTerminar
            permisosAlTerminar = null
            seguir?.invoke()
        }
        dialogoPermisos = d
        d.show()
        pintarPermisos()
    }

    private fun pintarPermisos() {
        if (dialogoPermisos == null) return
        permisosCuerpo.removeAllViews()
        permisosPie.removeAllViews()
        val c = permisosCuerpo
        val arranque = !prefs.guiaVista
        if (arranque) {
            val avance = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
            for (i in 0..2) avance.addView(View(this).apply { background = redondo(if (i == 0) Tema.HUESO else cTarjeta2, 2) },
                LinearLayout.LayoutParams(0, dp(4), 1f).apply { if (i > 0) leftMargin = dp(6) })
            c.addView(avance, LinearLayout.LayoutParams(-1, dp(4)).apply { bottomMargin = dp(Tema.S2) })
            c.addView(etiqueta("PASO 1 DE 3").apply { setPadding(0, 0, 0, dp(4)) })
        }
        // El aviso de «Guardar pantallas» va aquí como una casilla más (no sale aparte).
        prefs.diagAvisoVisto = true
        c.addView(texto(if (arranque) "Para leer tus ofertas" else "Permisos de Checa", 28f, cTexto, true))
        c.addView(texto("Solo lee Uber y DiDi. No toca nada ni acepta viajes por ti.", Tema.T_SUB, cTexto2, false)
            .apply { setLineSpacing(0f, 1.3f) }, margenArriba(Tema.S1))

        val acc = servicioActivo()
        if (acc) prefs.lectorPausado = false
        if (!acc && prefs.lectorPausado) {
            c.addView(texto("Pausaste el lector para usar tu banco. Actívalo otra vez para seguir viendo los pop-ups.",
                Tema.T_TEXTO, cAmarillo, true).apply {
                setLineSpacing(0f, 1.2f)
                background = redondo(Tema.AMARILLO_SUAVE, 12)
                setPadding(dp(12), dp(10), dp(12), dp(10))
            }, margenArriba(Tema.S2))
        }

        // Casilla verde = listo; contorno amarillo = falta (se toca la fila para activarlo).
        val g = grupoFilas()
        if (Build.VERSION.SDK_INT >= 33 && !acc) {
            val permitido = ajustesRestringidosPermitidos() == true
            filaCasilla(g, "Permitir ajustes restringidos",
                if (permitido) "Ya está permitido" else "Hazlo primero si Android bloquea el permiso de abajo · toca para abrir",
                if (permitido) true else null) {
                abrirInfoApp()
            }
        }
        filaCasilla(g, "Leer Uber y DiDi", if (acc) "Permiso de Accesibilidad" else "Falta · toca para activarlo", acc) {
            abrirAccesibilidad()
        }
        if (Build.VERSION.SDK_INT >= 33) {
            val ok = permisoNotificaciones()
            filaCasilla(g, "Notificaciones", if (ok) "Para avisarte si la lectura se corta" else "Falta · toca para permitirlas", ok) {
                pedirNotificaciones()
            }
        }
        val bat = permisoBateria()
        filaCasilla(g, "Batería sin restricción", if (bat) "Para que Android no corte la lectura" else "Falta · toca para activarla", bat) {
            pedirBateria()
        }
        if (esXiaomi()) {
            filaCasilla(g, "Inicio automático", "Recomendado en Xiaomi · toca para abrir", null) {
                try {
                    startActivity(Intent().setClassName("com.miui.securitycenter",
                        "com.miui.permcenter.autostart.AutoStartManagementActivity"))
                } catch (_: Exception) { abrirInfoApp() }
            }
        }
        filaCasilla(g, "Guardar pantallas de ofertas", "Para revisar las que se lean mal · 7 días", prefs.diagnostico, true) {
            prefs.diagnostico = !prefs.diagnostico
            pintarPermisos()
        }
        c.addView(g, margenArriba(Tema.S2))

        if (!acc) {
            c.addView(texto(
                "Busca «Checa – lector de ofertas» y actívalo. En Samsung está dentro de «Aplicaciones instaladas»; " +
                    "en OPPO suele estar en «Aplicaciones descargadas». Si Android dice «A la app se le negó el acceso» o «Controlado por ajustes restringidos», " +
                    "abre Información de la app, toca ⋮ → «Permitir configuración restringida» (a veces solo aparece después " +
                    "de intentar activarlo una vez) y vuelve a intentar.",
                Tema.T_NOTA, cTenue, false
            ).apply { setLineSpacing(0f, 1.3f) }, margenArriba(Tema.S2))
            c.addView(botonContorno("Información de la app") { abrirInfoApp() }, margenArriba(Tema.S1))
        }
        c.addView(texto(
            (if (arranque) "" else "Al iniciar la lectura, Android pregunta qué compartir: elige «Pantalla completa» y luego Empezar. ") +
                "Las pantallas guardadas se quedan solo en tu teléfono; pueden salir datos del pasajero, " +
                "por eso nada se envía sin que tú lo revises.",
            Tema.T_NOTA, cTenue, false
        ).apply { setLineSpacing(0f, 1.3f) }, margenArriba(Tema.S2))

        val completo = acc && permisoNotificaciones() && permisoBateria()
        if (completo && arranque) {
            permisosPie.addView(boton("Siguiente", cVerde, Color.BLACK) {
                dialogoPermisos?.dismiss()
            }.apply { setTextSize(TypedValue.COMPLEX_UNIT_SP, 15f) }, LinearLayout.LayoutParams(-1, dp(52)))
        } else if (completo) {
            permisosPie.addView(boton("▶  Iniciar lectura", cVerde, Color.BLACK) {
                permisosAlTerminar = null
                dialogoPermisos?.dismiss()
                iniciarCaptura()
            }.apply { setTextSize(TypedValue.COMPLEX_UNIT_SP, 15f) }, LinearLayout.LayoutParams(-1, dp(52)))
            permisosPie.addView(texto("Continuar sin iniciar", Tema.T_TEXTO, cTenue, false).apply {
                typeface = fuenteMedia
                gravity = Gravity.CENTER
                minHeight = dp(Tema.ALTO_TOQUE)
                isClickable = true
                setOnClickListener { dialogoPermisos?.dismiss() }
            }, margenArriba(Tema.S1))
        } else {
            permisosPie.addView(boton("Completa los permisos para seguir", Tema.CAMPO, cTenue) { }
                .apply { setTextSize(TypedValue.COMPLEX_UNIT_SP, 15f) }, LinearLayout.LayoutParams(-1, dp(52)))
        }
    }

    /** Fila del arranque con casilla: verde con ✓ si está listo, contorno amarillo si falta (humo si es opcional). */
    private fun filaCasilla(
        grupo: LinearLayout, titulo: String, sub: String, listo: Boolean?, opcional: Boolean = false, accion: () -> Unit
    ) {
        if (grupo.childCount > 0) grupo.addView(View(this).apply { setBackgroundColor(Tema.BORDE) },
            LinearLayout.LayoutParams(-1, dp(1)))
        val falta = listo == false && !opcional
        val f = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            minimumHeight = dp(60)
            setPadding(dp(14), dp(10), dp(14), dp(10))
            background = ripple(Color.TRANSPARENT, Tema.R_TARJETA)
            isClickable = true
            setOnClickListener { if (listo != true || opcional) accion() }
        }
        f.addView(texto(if (listo == true) "✓" else "", 13f, Tema.SOBRE_VERDE, true).apply {
            gravity = Gravity.CENTER
            background = if (listo == true) redondo(cVerde, 6) else GradientDrawable().apply {
                cornerRadius = dp(6).toFloat()
                setColor(Color.TRANSPARENT)
                setStroke(dp(2), if (falta) cAmarillo else Tema.HUMO)
            }
        }, LinearLayout.LayoutParams(dp(22), dp(22)).apply { rightMargin = dp(14) })
        val textos = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        textos.addView(texto(titulo, Tema.T_TEXTO, cTexto, true))
        textos.addView(texto(sub, Tema.T_NOTA, if (falta) cAmarillo else cTenue, false), margenArriba(2))
        f.addView(textos, LinearLayout.LayoutParams(0, -2, 1f))
        grupo.addView(f)
    }

    private fun pedirNotificaciones() {
        if (!pidioNotificaciones) {
            pidioNotificaciones = true
            requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 7)
        } else {
            // Si ya las negó, Android no vuelve a preguntar: se activan desde los ajustes de la app.
            try {
                startActivity(Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE, packageName))
            } catch (_: Exception) { abrirInfoApp() }
        }
    }

    private fun pedirBateria() {
        try {
            startActivity(Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS, Uri.parse("package:$packageName")))
        } catch (_: Exception) {
            try { startActivity(Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)) } catch (_: Exception) { abrirInfoApp() }
        }
    }

    private var dialogoFalla: Dialog? = null
    /** Repinta «¿Algo falla?» si está abierta (al volver de Ajustes del teléfono). */
    private var pintarFallaAbierta: (() -> Unit)? = null

    /** Hoja que sube desde abajo (marca v2): asa, título y contenido. */
    private fun hojaAbajo(titulo: String, cuerpo: LinearLayout): Dialog {
        val d = Dialog(this)
        d.requestWindowFeature(Window.FEATURE_NO_TITLE)
        val raiz = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(10), dp(20), dp(Tema.S3))
        }
        raiz.addView(View(this).apply { background = redondo(Tema.HUMO, 2) },
            LinearLayout.LayoutParams(dp(36), dp(4)).apply { gravity = Gravity.CENTER_HORIZONTAL; bottomMargin = dp(Tema.S2) })
        raiz.addView(texto(titulo, Tema.T_TITULO, cTexto, true))
        raiz.addView(ScrollView(this).apply {
            isVerticalScrollBarEnabled = false
            addView(cuerpo)
        }, margenArriba(4))
        d.setContentView(raiz)
        d.setCanceledOnTouchOutside(true)
        d.window?.let { w ->
            w.setBackgroundDrawable(GradientDrawable().apply {
                setColor(cTarjeta)
                val r = dp(28).toFloat()
                cornerRadii = floatArrayOf(r, r, r, r, 0f, 0f, 0f, 0f)
            })
            w.setGravity(Gravity.BOTTOM)
            w.attributes = w.attributes.apply { windowAnimations = android.R.style.Animation_InputMethod }
        }
        d.show()
        d.window?.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        return d
    }

    /** Una revisión de «¿Algo falla?»: ✓ verde si está bien, ! amarillo si no. */
    private fun filaRevision(ok: Boolean, s: String): View {
        val f = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL }
        val col = if (ok) cVerde else cAmarillo
        f.addView(texto(if (ok) "✓" else "!", 11f, col, true).apply {
            gravity = Gravity.CENTER
            background = punto(Color.argb(46, Color.red(col), Color.green(col), Color.blue(col)))
        }, LinearLayout.LayoutParams(dp(20), dp(20)).apply { rightMargin = dp(10) })
        f.addView(texto(s, Tema.T_TEXTO, cTexto, false), LinearLayout.LayoutParams(0, -2, 1f))
        return f
    }

    /** «¿Algo falla?» desde la línea de estado de Inicio: qué está bien, qué falta y el botón para arreglar cada cosa. */
    private fun mostrarAlgoFalla() {
        dialogoFalla?.dismiss()
        val cuerpo = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        val d = hojaAbajo("¿Algo falla?", cuerpo)
        dialogoFalla = d
        fun pintar() {
            cuerpo.removeAllViews()
            val acc = servicioActivo()
            val leyendo = CapturaService.corriendo
            val revisiones = ArrayList<Pair<Boolean, String>>()
            val acciones = ArrayList<Pair<String, () -> Unit>>()
            fun accion(s: String, a: () -> Unit) { acciones.add(s to a) }
            revisiones.add(acc to if (acc) "Permiso para leer Uber y DiDi" else "Falta el permiso para leer Uber y DiDi")
            if (!acc) accion("Dar el permiso") { d.dismiss(); mostrarPermisos() }
            revisiones.add(leyendo to when {
                leyendo && prefs.ultimaOferta > 0 -> "Leyendo · última oferta ${hace(prefs.ultimaOferta)}"
                leyendo -> "Leyendo · aún sin ofertas"
                CapturaService.cortada -> "Lectura detenida: Android la cortó"
                else -> "La lectura está apagada"
            })
            if (Build.VERSION.SDK_INT >= 33) {
                val ok = permisoNotificaciones()
                revisiones.add(ok to if (ok) "Notificaciones permitidas" else "Notificaciones apagadas")
                if (!ok) accion("Permitir notificaciones") { pedirNotificaciones() }
            }
            val bat = permisoBateria()
            revisiones.add(bat to if (bat) "Batería sin restricción" else "Batería con restricción")
            if (!bat) accion("Quitar restricción de batería") { pedirBateria() }
            if (!prefs.uber && !prefs.didi) {
                revisiones.add(false to "Uber y DiDi están apagadas")
                accion("Elegir apps que lee") { mostrarAppsLee() }
            }
            if (!Licencia.estado(this).activa) {
                revisiones.add(false to "Tu plan terminó: no salen pop-ups")
                accion("Ver paquetes") { d.dismiss(); irAPaquetes() }
            }
            revisiones.forEach { (ok, s) -> cuerpo.addView(filaRevision(ok, s), margenArriba(Tema.S1)) }
            if (revisiones.all { it.first }) {
                cuerpo.addView(texto(
                    "Todo se ve bien. Si no sale el pop-up, abre Uber o DiDi con una oferta en pantalla.",
                    Tema.T_NOTA, cTenue, false
                ).apply { setLineSpacing(0f, 1.2f) }, margenArriba(Tema.S2))
            }
            // El primer arreglo va como botón principal; los demás, con contorno.
            var primero = true
            if (acc && !leyendo) {
                cuerpo.addView(boton("▶  Iniciar lectura", cVerde, Color.BLACK) { d.dismiss(); iniciarCaptura() }, margenArriba(Tema.S2))
                primero = false
            }
            acciones.forEach { (s, a) ->
                cuerpo.addView(if (primero) boton(s, cVerde, Color.BLACK, a) else botonContorno(s, a),
                    margenArriba(if (primero) Tema.S2 else Tema.S1))
                primero = false
            }
            cuerpo.addView(texto("Más ayuda y preguntas frecuentes", Tema.T_NOTA, cTenue, false).apply {
                gravity = Gravity.CENTER
                minHeight = dp(Tema.ALTO_TOQUE)
                paintFlags = paintFlags or android.graphics.Paint.UNDERLINE_TEXT_FLAG
                isClickable = true
                setOnClickListener { d.dismiss(); mostrarAyuda() }
            }, margenArriba(Tema.S1))
        }
        pintar()
        pintarFallaAbierta = { pintar() }
        d.setOnDismissListener {
            if (dialogoFalla === d) { dialogoFalla = null; pintarFallaAbierta = null }
        }
    }

    /** Una sola ventana a la vez: permisos, guía, cuenta, versión nueva, beta, opinión o aviso de guardar pantallas. */
    private fun hayVentana(): Boolean =
        dialogoPermisos != null || guia?.isShowing == true || dialogoCuenta?.isShowing == true ||
            dialogoAct?.isShowing == true || dialogoAviso?.isShowing == true || dialogoDiagAviso?.isShowing == true ||
            dialogoFalla?.isShowing == true

    private fun actualizarAcceso() {
        acceso?.visibility = if (Cuenta.puedeUsar(this)) View.GONE else View.VISIBLE
    }

    /** Beta gratis: usar Checa sin cuenta. Después de la beta hay que entrar con Google o usar un código. */
    private fun entrarSinCuenta() {
        Cuenta.entrarSinCuenta(this)
        actualizarAcceso()
        pintarLicencia()
        pintarCuenta()
        // Igual que después de entrar con Google: permisos y, si es la primera vez, la guía.
        val seguir: () -> Unit = { if (!prefs.guiaVista) mostrarGuia() }
        if (faltanPermisos()) mostrarPermisos(seguir) else seguir()
    }

    private fun entrarConGoogle() {
        var espera: Dialog? = null
        Cuenta.iniciarSesion(this, buscando = {
            val c = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
            c.addView(texto("Buscando tu licencia…", Tema.T_TEXTO, cTexto2, false))
            espera = hoja("Un momento", null, c)
        }) { r ->
            espera?.dismiss()
            mostrarResultadoCuenta(r)
        }
    }

    private fun mostrarResultadoCuenta(r: Cuenta.Resultado) {
        actualizarAcceso()
        if (Cuenta.ajustesAplicados(this)) refrescarFiltrosYAjustes()
        pintarLicencia()
        pintarCuenta()
        dialogoCuenta?.dismiss()
        when (r) {
            is Cuenta.Resultado.Error -> if (r.mensaje.isNotEmpty()) Toast.makeText(this, r.mensaje, Toast.LENGTH_LONG).show()
            is Cuenta.Resultado.Limite -> dialogoLimite(r.equipos)
            is Cuenta.Resultado.Ok -> {
                // Después de entrar: permisos (paso 1 de 3); si es la primera vez, sigue la guía (pasos 2 y 3).
                val seguir = { if (!prefs.guiaVista) mostrarGuia() else dialogoBienvenidaCuenta() }
                if (faltanPermisos()) mostrarPermisos(seguir) else seguir()
            }
        }
    }

    /** Después de entrar: licencia encontrada o cuenta nueva en prueba. */
    private fun dialogoBienvenidaCuenta() {
        val e = Licencia.estado(this)
        val nombre = Cuenta.nombre(this)
        val cuerpo = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        if (Cuenta.licencia(this) != null) {
            cuerpo.addView(texto("Encontramos tu licencia en ${Cuenta.correo(this)}.", Tema.T_TEXTO, cTexto2, false)
                .apply { setLineSpacing(0f, 1.2f) })
            if (e.fundador) cuerpo.addView(insigniaFundador().apply { visibility = View.VISIBLE },
                LinearLayout.LayoutParams(-2, -2).apply { topMargin = dp(Tema.S1) })
            cuerpo.addView(texto(
                if (e.vence >= Licencia.PERMANENTE) "Sin vencimiento" else "Vence el ${Licencia.fechaBonita(e.vence)}",
                Tema.T_SUB, cTexto, true
            ), margenArriba(Tema.S1))
            cuerpo.addView(listaEquipos(false), margenArriba(Tema.S2))
            cuerpo.addView(boton("Empezar", cVerde, Color.BLACK) { dialogoCuenta?.dismiss() }, margenArriba(Tema.S2))
            dialogoCuenta = hoja(if (nombre.isEmpty()) "¡Listo!" else "¡Listo, $nombre!", null, cuerpo)
        } else {
            cuerpo.addView(texto("Tu cuenta ${Cuenta.correo(this)} todavía no tiene licencia.", Tema.T_TEXTO, cTexto2, false)
                .apply { setLineSpacing(0f, 1.2f) })
            cuerpo.addView(texto(
                if (e.beta && !e.pagada) "Beta gratis: todo incluido" else
                if (e.activa && !e.pagada) "Prueba gratis: quedan ${e.diasRestantes} días" else
                    if (e.pagada) "Este equipo ya tiene licencia con código" else "Tu prueba gratis terminó",
                Tema.T_SUB, if (e.activa) cTexto else cAmarillo, true
            ), margenArriba(Tema.S1))
            cuerpo.addView(texto(
                "Elige tu paquete y paga con Mercado Pago. Se activa solo en esta cuenta; no tienes que escribir ningún código.",
                Tema.T_NOTA, cTenue, false
            ).apply { setLineSpacing(0f, 1.2f) }, margenArriba(Tema.S1))
            cuerpo.addView(boton("Ver paquetes", cTarjeta2, cTexto) { irAPaquetes() }, margenArriba(Tema.S2))
            cuerpo.addView(boton("Empezar", cVerde, Color.BLACK) { dialogoCuenta?.dismiss() }, margenArriba(Tema.S1))
            dialogoCuenta = hoja(if (nombre.isEmpty()) "Bienvenido" else "Bienvenido, $nombre", null, cuerpo)
        }
    }

    /** Ya hay 2 equipos en la cuenta: elegir cuál quitar para activar este. */
    private fun dialogoLimite(equipos: List<Cuenta.Equipo>) {
        val cuerpo = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        cuerpo.addView(texto(
            Cuenta.maxEquipos(this).let { n ->
                if (n == 1) "Tu paquete de 1 mes permite 1 equipo. Quita el otro para activar este, o cambia al de 6 meses para usar 2."
                else "Tu licencia permite $n equipos. Quita uno para activar este."
            },
            Tema.T_TEXTO, cTexto2, false
        ).apply { setLineSpacing(0f, 1.2f) })
        equipos.forEach { eq ->
            cuerpo.addView(filaEquipo(eq, mostrarQuitar = true), margenArriba(Tema.S1))
        }
        cuerpo.addView(texto(
            "El equipo que quites deja de mostrar pop-ups hasta que vuelvas a entrar con tu cuenta en él.",
            Tema.T_NOTA, cAmarillo, false
        ).apply { setLineSpacing(0f, 1.2f) }, margenArriba(Tema.S2))
        dialogoCuenta = hoja("Ya usas Checa en ${equipos.size} equipos", cAmarillo, cuerpo)
    }

    private fun listaEquipos(conQuitar: Boolean): View {
        val caja = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        val lista = Cuenta.equipos(this)
        caja.addView(etiqueta("EQUIPOS · ${lista.size} DE ${Cuenta.maxEquipos(this)}"))
        lista.forEach { eq -> caja.addView(filaEquipo(eq, conQuitar), margenArriba(4)) }
        return caja
    }

    private fun filaEquipo(eq: Cuenta.Equipo, mostrarQuitar: Boolean): View {
        val esEste = eq.id == Licencia.codigoEquipo(this)
        val fila = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(12), dp(10), dp(12), dp(10))
            background = redondo(cTarjeta2, Tema.R_BOTON)
        }
        val textos = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        textos.addView(texto(if (esEste) "Este equipo" else eq.nombre.ifEmpty { eq.id }, Tema.T_TEXTO, cTexto, true))
        val uso = Cuenta.haceCuanto(eq.ultimoUso)
        textos.addView(texto((if (esEste) eq.nombre + " · " else "") + "Último uso: " + uso, Tema.T_NOTA, cTenue, false))
        fila.addView(textos, LinearLayout.LayoutParams(0, -2, 1f))
        if (esEste) {
            fila.addView(texto("AQUÍ", Tema.T_NOTA, Tema.ASFALTO, true).apply {
                setPadding(dp(8), dp(3), dp(8), dp(3))
                background = redondo(Tema.HUESO, 50)
            })
        } else if (mostrarQuitar) {
            fila.addView(texto("Quitar", Tema.T_NOTA, cRojo, true).apply {
                setPadding(dp(12), dp(8), dp(12), dp(8))
                background = GradientDrawable().apply {
                    cornerRadius = dp(10).toFloat()
                    setStroke(dp(1), Tema.BORDE)
                }
                isClickable = true
                setOnClickListener {
                    Toast.makeText(this@MainActivity, "Quitando ${eq.nombre}…", Toast.LENGTH_SHORT).show()
                    Cuenta.quitarEquipo(this@MainActivity, eq.id) { r -> mostrarResultadoCuenta(r) }
                }
            })
        }
        return fila
    }

    /** Pestaña Cuenta: perfil arriba, credencial del plan, equipos y «Cerrar sesión» discreto. */
    private fun pintarCuenta() {
        if (!::cuentaCaja.isInitialized) return
        pintarInvitar()
        avisarPremios()
        val e = Licencia.estado(this)
        // Se llama cada 1.5 s (con el estado de la lectura): solo se vuelve a armar si algo cambió.
        val guardadas = Guardado.lista(this)
        val firma = listOf(
            e, Cuenta.conectada(this), Cuenta.nombre(this), Cuenta.correo(this), Cuenta.equipos(this), Cuenta.maxEquipos(this),
            Cuenta.equipoQuitado(this), Cuenta.necesitaRevision(this), Cuenta.licencia(this) == null, Cuenta.precioBeta(this),
            permisosFaltantes(), prefs.diagnostico, guardadas.size, guardadas.count { !it.leida }, Actualizacion.pendiente(this)
        ).joinToString("|")
        if (firma == firmaCuenta && cuentaCaja.childCount > 0) return
        firmaCuenta = firma
        cuentaCaja.removeAllViews()
        if (!Cuenta.conectada(this)) {
            val t = tarjeta()
            t.addView(texto("Entra con tu cuenta", Tema.T_SUB, cTexto, true))
            t.addView(texto(
                "Entra con tu cuenta de Google para guardar tu licencia. Si reinstalas o cambias de celular, solo vuelves a entrar.",
                Tema.T_TEXTO, cTexto2, false
            ).apply { setLineSpacing(0f, 1.2f) }, margenArriba(Tema.S1))
            t.addView(boton("Continuar con Google", Color.WHITE, Color.BLACK) { entrarConGoogle() }, margenArriba(Tema.S2))
            cuentaCaja.addView(t)
            cuentaCaja.addView(credencial(e), margenArriba(Tema.ENTRE_TARJETAS))
            pintarCuentaPie()
            return
        }
        val nombre = Cuenta.nombre(this)
        cuentaCaja.addView(perfil(nombre, Cuenta.correo(this)))
        cuentaCaja.addView(credencial(e), margenArriba(Tema.S1))
        when {
            Cuenta.equipoQuitado(this) -> {
                cuentaCaja.addView(texto("Este equipo fue quitado de tu cuenta.", Tema.T_TEXTO, cAmarillo, true), margenArriba(Tema.S2))
                cuentaCaja.addView(boton("Activar este equipo", cVerde, Color.BLACK) {
                    Cuenta.revisarAhora(this) { r -> mostrarResultadoCuenta(r) }
                }, margenArriba(Tema.S1))
            }
            Cuenta.necesitaRevision(this) -> cuentaCaja.addView(texto(
                "Llevas más de ${Cuenta.DIAS_SIN_RED} días sin revisar tu licencia. Abre Checa con internet.",
                Tema.T_TEXTO, cAmarillo, false
            ).apply { setLineSpacing(0f, 1.2f) }, margenArriba(Tema.S2))
            e.beta -> {}
            Cuenta.licencia(this) == null -> {
                cuentaCaja.addView(boton("Ver paquetes", cVerde, Color.BLACK) { irAPaquetes() }, margenArriba(Tema.S2))
                cuentaCaja.addView(boton("Ya pagué: revisar mi licencia", cTarjeta2, cTexto) {
                    Toast.makeText(this, "Buscando tu licencia…", Toast.LENGTH_SHORT).show()
                    Cuenta.revisarAhora(this) { r -> mostrarResultadoCuenta(r) }
                }, margenArriba(Tema.S1))
            }
            e.pagada && e.vence < Licencia.PERMANENTE ->
                cuentaCaja.addView(boton("Renovar · se suman los días", cVerde, Color.BLACK) { irAPaquetes() }, margenArriba(Tema.S2))
        }
        cuentaCaja.addView(filasCuenta(), margenArriba(Tema.S2))
        pintarCuentaPie()
    }

    /** Foto con la inicial, nombre y correo en una fila. */
    private fun perfil(nombre: String, correo: String): View {
        val fila = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(4), dp(Tema.S1), dp(4), 0)
        }
        val inicial = (nombre.ifEmpty { correo }.trim().firstOrNull() ?: '?').uppercaseChar().toString()
        val foto = texto(inicial, 22f, Tema.ASFALTO, true).apply {
            gravity = Gravity.CENTER
            background = GradientDrawable().apply { shape = GradientDrawable.OVAL; setColor(Tema.HUESO) }
        }
        fila.addView(foto, LinearLayout.LayoutParams(dp(50), dp(50)).apply { rightMargin = dp(14) })
        val tx = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        tx.addView(texto(nombre.ifEmpty { "Tu cuenta" }, Tema.T_SUB, cTexto, true).apply { maxLines = 1; ellipsize = TextUtils.TruncateAt.END })
        tx.addView(texto(correo, Tema.T_NOTA, cTenue, false).apply { maxLines = 1; ellipsize = TextUtils.TruncateAt.END },
            margenArriba(2))
        fila.addView(tx, LinearLayout.LayoutParams(0, -2, 1f))
        return fila
    }

    /** Tarjeta tipo membresía con el plan: dorada para fundadores. */
    private fun credencial(e: Licencia.Estado): View {
        val sinFin = e.pagada && e.vence >= Licencia.PERMANENTE
        val c = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(20), dp(20), dp(20))
            background = GradientDrawable(
                GradientDrawable.Orientation.TL_BR,
                intArrayOf(Tema.TARJETA, Tema.TARJETA)
            ).apply {
                cornerRadius = dp(20).toFloat()
                setStroke(dp(1), Tema.HUMO)
            }
        }
        val marca = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL }
        marca.addView(texto("TU PLAN", 10.5f, cTenue, true).apply {
            typeface = Fuentes.numero
            letterSpacing = 0.14f
        }, LinearLayout.LayoutParams(0, -2, 1f))
        marca.addView(ImageView(this).apply { setImageResource(R.drawable.semaforo) }, LinearLayout.LayoutParams(dp(12), dp(20)))
        c.addView(marca)
        val titulo = when {
            e.fundador -> "★ Miembro fundador"
            e.pagada && e.basico -> "Plan Básico"
            e.pagada -> "Licencia activa"
            e.beta -> "Beta gratis"
            e.activa -> "Prueba gratis"
            else -> "Sin licencia"
        }
        val colorTitulo = when {
            e.fundador -> DORADO
            e.activa -> cTexto
            else -> cAmarillo
        }
        c.addView(texto(titulo, 26f, colorTitulo, true), margenArriba(Tema.S2))
        val quedan = if (e.pagada && !sinFin) diasHasta(e.vence) else e.diasRestantes
        val sub = when {
            sinFin -> "Activa · sin vencimiento"
            e.pagada -> "Vence el ${Licencia.fechaBonita(e.vence)} · ${quedanDias(quedan)}"
            e.beta -> "Todo incluido gratis hasta el ${Licencia.fechaBonita(Licencia.BETA_HASTA)}"
            e.activa -> "Todo incluido · ${quedanDias(quedan)}"
            else -> "Elige un paquete para volver a ver los pop-ups"
        }
        c.addView(texto(sub, Tema.T_TEXTO, cTexto2, false), margenArriba(4))
        if (!e.pagada && e.activa && !e.beta) {
            // Barra de la prueba: lo que queda de los 7 días.
            val barra = FrameLayout(this).apply { background = redondo(cTarjeta2, 3) }
            val lleno = View(this).apply { background = redondo(cAmarillo, 3) }
            barra.addView(lleno, FrameLayout.LayoutParams(0, -1))
            barra.post {
                lleno.layoutParams = FrameLayout.LayoutParams(barra.width * quedan / Licencia.DIAS_PRUEBA, -1)
            }
            c.addView(barra, LinearLayout.LayoutParams(-1, dp(6)).apply { topMargin = dp(Tema.S2) })
        }
        // En la beta los paquetes están ocultos: aquí se ve que ya tiene asegurado el precio de fundador.
        if (Cuenta.precioBeta(this) && !sinFin) {
            // Sello inclinado de la marca v2.
            c.addView(texto("PRECIO DE FUNDADOR · −40 %", 10.5f, Tema.HUESO, true).apply {
                typeface = Fuentes.numero
                letterSpacing = 0.06f
                setPadding(dp(10), dp(5), dp(10), dp(5))
                rotation = -4f
                background = GradientDrawable().apply {
                    cornerRadius = dp(10).toFloat()
                    setStroke(dp(2), Tema.HUESO)
                }
            }, LinearLayout.LayoutParams(-2, -2).apply { topMargin = dp(Tema.S2); leftMargin = dp(4) })
        }
        return c
    }

    private fun quedanDias(n: Int) = if (n == 1) "queda 1 día" else "quedan $n días"

    /** Días desde hoy hasta una fecha AAAAMMDD (0 si ya pasó). */
    private fun diasHasta(aaaammdd: Int): Int = try {
        val f = SimpleDateFormat("yyyyMMdd", Locale.US).parse(aaaammdd.toString())!!.time
        val hoy = SimpleDateFormat("yyyyMMdd", Locale.US).let { it.parse(it.format(Date()))!!.time }
        maxOf(0, ((f - hoy) / 86_400_000L).toInt() + 1)
    } catch (_: Exception) { 0 }

    /** Un lugar de equipo: ícono, nombre, último uso y «Aquí» o «Quitar» (pide confirmar). */
    private fun lugarEquipo(eq: Cuenta.Equipo): View {
        val esEste = eq.id == Licencia.codigoEquipo(this)
        val caja = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(12), dp(12), dp(12), dp(12))
            background = redondo(cTarjeta2, Tema.R_BOTON)
        }
        val arriba = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL }
        arriba.addView(texto(if (esEste) "Este equipo" else "Otro equipo", Tema.T_NOTA, cTenue, false),
            LinearLayout.LayoutParams(0, -2, 1f))
        if (esEste) {
            arriba.addView(texto("AQUÍ", Tema.T_NOTA, Tema.ASFALTO, true).apply {
                setPadding(dp(8), dp(3), dp(8), dp(3))
                background = redondo(Tema.HUESO, 50)
            })
        } else {
            arriba.addView(texto("Quitar", Tema.T_NOTA, cRojo, true).apply {
                setPadding(dp(10), dp(6), dp(10), dp(6))
                background = GradientDrawable().apply {
                    cornerRadius = dp(10).toFloat()
                    setStroke(dp(1), Tema.BORDE)
                }
                isClickable = true
                var confirmar = false
                setOnClickListener {
                    if (!confirmar) {
                        confirmar = true
                        text = "¿Seguro?"
                        setTextColor(Color.WHITE)
                        background = redondo(cRojo, 10)
                    } else {
                        Toast.makeText(this@MainActivity, "Quitando ${eq.nombre}…", Toast.LENGTH_SHORT).show()
                        Cuenta.quitarEquipo(this@MainActivity, eq.id) { r -> mostrarResultadoCuenta(r) }
                    }
                }
            })
        }
        caja.addView(arriba)
        caja.addView(texto(eq.nombre.ifEmpty { eq.id }, Tema.T_TEXTO, cTexto, true).apply {
            maxLines = 1
            ellipsize = TextUtils.TruncateAt.END
        }, margenArriba(Tema.S1))
        caja.addView(texto("Último uso: " + Cuenta.haceCuanto(eq.ultimoUso), Tema.T_NOTA, cTenue, false), margenArriba(2))
        return caja
    }

    private fun lugarLibre() = texto("Libre", Tema.T_NOTA, cTenue, false).apply {
        gravity = Gravity.CENTER
        minHeight = dp(88)
        background = GradientDrawable().apply {
            cornerRadius = dp(Tema.R_BOTON).toFloat()
            setStroke(dp(1), Tema.BORDE, dp(6).toFloat(), dp(4).toFloat())
        }
    }

    private fun ofertaEjemplo() = Oferta(85.0, 7.2, 24.0, 1.5, 5.0, 1.2)

    private fun refrescarVistaPrevia() {
        if (!::vistaPrevia.isInitialized) return
        vistaPrevia.removeAllViews()
        val v = Popup(this, prefs, WindowManager.LayoutParams.TYPE_APPLICATION).construir(ofertaEjemplo(), false)
        // El mismo ancho que usa Popup.mostrar sobre la pantalla completa: 94 % de la pantalla, máximo el tamaño elegido.
        val real = DisplayMetrics()
        @Suppress("DEPRECATION")
        windowManager.defaultDisplay.getRealMetrics(real)
        val ancho = minOf((real.widthPixels * 0.94f).toInt(), dp(prefs.anchoPopup))
        vistaPrevia.addView(v, FrameLayout.LayoutParams(ancho, -2, Gravity.CENTER_HORIZONTAL))
    }

    private fun num(v: Float): String =
        if (v % 1f == 0f) v.toInt().toString() else String.format(Locale.US, "%.1f", v)

    private fun num1(v: Double): String = String.format(Locale.US, "%.1f", v)

    // =====================================================================
    // Ayuda con la lectura: lo guardado de cada oferta y el reporte a soporte
    // =====================================================================

    private var cajaDiag: LinearLayout? = null
    private var estadoLecturaTxt: TextView? = null
    private var estadoLecturaPunto: View? = null
    private var dialogoDiagAviso: Dialog? = null
    /** Lo que el conductor escribió en el reporte (se conserva si va a elegir ofertas y regresa). */
    private var reporteMotivo = 0
    private var reporteTexto = ""

    private val motivosReporte = listOf("Leyó mal la oferta", "No salió el pop-up", "Parpadea o se queda", "Otro")

    private fun chipDiag(s: String, fondo: Int, color: Int) = texto(s, Tema.T_NOTA, color, true).apply {
        setPadding(dp(10), dp(4), dp(10), dp(4))
        background = redondo(fondo, 50)
    }

    private fun hace(ms: Long): String {
        val min = ((System.currentTimeMillis() - ms) / 60_000L).toInt()
        return when {
            min < 1 -> "hace un momento"
            min < 60 -> "hace $min min"
            min < 24 * 60 -> "hace ${min / 60} h"
            else -> Guardado.cuando(ms)
        }
    }

    /** «Leyendo · última oferta hace 2 min» (se repinta con [actualizarEstado]). */
    private fun pintarEstadoLectura() {
        val t = estadoLecturaTxt ?: return
        val leyendo = CapturaService.corriendo
        val ultima = prefs.ultimaOferta
        t.text = when {
            !leyendo -> "Lectura apagada · toca ▶ para iniciar"
            ultima == 0L -> "Leyendo · aún sin ofertas"
            else -> "Leyendo · última oferta ${hace(ultima)}"
        }
        estadoLecturaPunto?.background = punto(if (leyendo) cVerde else cTenue)
    }

    private fun pintarDiag() {
        val c = cajaDiag ?: return
        c.removeAllViews()
        val estado = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            background = redondo(cTarjeta2, 12)
            setPadding(dp(12), dp(10), dp(12), dp(10))
        }
        val p = View(this)
        estado.addView(p, LinearLayout.LayoutParams(dp(8), dp(8)).apply { rightMargin = dp(10) })
        val t = texto("", Tema.T_NOTA, cTexto2, false)
        estado.addView(t, LinearLayout.LayoutParams(0, -2, 1f))
        estadoLecturaTxt = t
        estadoLecturaPunto = p
        pintarEstadoLectura()
        c.addView(estado)

        c.addView(interruptor(
            "Guardar pantallas de ofertas",
            "Para revisar las que se lean mal. Solo en tu teléfono y se borran solas a los 7 días.",
            prefs.diagnostico
        ) { on ->
            prefs.diagnostico = on
            if (on && !prefs.diagAvisoVisto) avisoDiagnostico()
            handler.post { pintarDiag() }
        }, margenArriba(Tema.S1))

        val items = Guardado.lista(this)
        if (items.isEmpty()) {
            c.addView(texto(
                if (prefs.diagnostico) "Aún no hay ofertas guardadas. Se guardan solas mientras Checa lee."
                else "Está apagado: no se guarda nada.",
                Tema.T_NOTA, cTenue, false
            ).apply { setLineSpacing(0f, 1.2f) }, margenArriba(4))
        } else {
            val noLeidas = items.count { !it.leida }
            val chips = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
            chips.addView(chipDiag("${items.size} ${if (items.size == 1) "oferta" else "ofertas"}", cTarjeta2, cTexto2))
            if (noLeidas > 0) chips.addView(chipDiag("$noLeidas no ${if (noLeidas == 1) "leída" else "leídas"}", Tema.AMARILLO_SUAVE, cAmarillo),
                LinearLayout.LayoutParams(-2, -2).apply { leftMargin = dp(6) })
            chips.addView(chipDiag(String.format(Locale.US, "%.1f MB", Guardado.bytes(this) / 1_048_576.0), cTarjeta2, cTexto2),
                LinearLayout.LayoutParams(-2, -2).apply { leftMargin = dp(6) })
            c.addView(chips, margenArriba(4))
        }
        c.addView(boton("Enviar reporte a soporte", cVerde, Color.BLACK) {
            // Por defecto van las de hoy que no se pudieron leer; el conductor puede cambiarlas.
            val hoy = items.filter { !it.leida && System.currentTimeMillis() - it.hora < 86_400_000L }.take(5)
            mostrarReporte(hoy)
        }, margenArriba(Tema.S2))
        c.addView(boton("Ver lo guardado", cTarjeta2, cTexto) { mostrarGuardado() }, margenArriba(Tema.S1))
        if (items.isNotEmpty()) {
            c.addView(texto("Borrar todo lo guardado", Tema.T_NOTA, cTenue, false).apply {
                gravity = Gravity.CENTER
                minHeight = dp(Tema.ALTO_TOQUE)
                paintFlags = paintFlags or android.graphics.Paint.UNDERLINE_TEXT_FLAG
                isClickable = true
                setOnClickListener {
                    AlertDialog.Builder(this@MainActivity)
                        .setMessage("¿Borrar las ${items.size} ofertas guardadas, con sus fotos y textos? No se puede deshacer.")
                        .setPositiveButton("Borrar") { _, _ ->
                            Guardado.borrarTodo(this@MainActivity)
                            prefs.borrarDiagnostico()
                            pintarDiag()
                        }
                        .setNegativeButton("Cancelar", null)
                        .show()
                }
            }, margenArriba(4))
        }
    }

    /** Explica qué se guarda. Sale una vez (viene encendido) y al encenderlo a mano. */
    private fun avisoDiagnostico() {
        if (dialogoDiagAviso?.isShowing == true) return
        val cuerpo = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        listOf(
            "Solo cuando hay una oferta de Uber o DiDi, y solo la parte de esa app.",
            "Se quedan en tu teléfono y se borran solas a los 7 días.",
            "Pueden salir el nombre del pasajero o direcciones. Antes de enviar algo, tú lo revisas.",
            "Nada se envía solo."
        ).forEachIndexed { i, s ->
            val f = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
            f.addView(View(this).apply { background = punto(Tema.HUESO) },
                LinearLayout.LayoutParams(dp(6), dp(6)).apply { topMargin = dp(8); rightMargin = dp(10) })
            f.addView(texto(s, Tema.T_TEXTO, cTexto2, false).apply { setLineSpacing(0f, 1.2f) }, LinearLayout.LayoutParams(0, -2, 1f))
            cuerpo.addView(f, if (i > 0) margenArriba(Tema.S1) else LinearLayout.LayoutParams(-1, -2))
        }
        cuerpo.addView(boton("Sí, guardar", cVerde, Color.BLACK) {
            prefs.diagAvisoVisto = true
            prefs.diagnostico = true
            dialogoDiagAviso?.dismiss()
            pintarDiag()
        }, margenArriba(Tema.S2))
        cuerpo.addView(boton("No guardar", cTarjeta2, cTexto) {
            prefs.diagAvisoVisto = true
            prefs.diagnostico = false
            dialogoDiagAviso?.dismiss()
            pintarDiag()
        }, margenArriba(Tema.S1))
        dialogoDiagAviso = hoja("Checa guarda las pantallas de tus ofertas", null, cuerpo).apply { setCancelable(false) }
    }

    /** Ventana de pantalla completa con encabezado (título + cerrar), cuerpo y pie. */
    private fun ventanaCompleta(titulo: String): Triple<Dialog, LinearLayout, LinearLayout> {
        val d = Dialog(this, android.R.style.Theme_DeviceDefault_NoActionBar)
        val raiz = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setBackgroundColor(cFondo) }
        val cab = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(16), dp(Tema.S2), dp(8), dp(Tema.S1))
        }
        cab.addView(texto(titulo, Tema.T_SUB, cTexto, true).apply { maxLines = 1; ellipsize = TextUtils.TruncateAt.END },
            LinearLayout.LayoutParams(0, -2, 1f))
        cab.addView(icono(Icono.CERRAR, cTenue, 20).apply {
            setPadding(dp(12), dp(12), dp(12), dp(12))
            background = ripple(Color.TRANSPARENT, 24)
            isClickable = true
            setOnClickListener { d.dismiss() }
        }, LinearLayout.LayoutParams(dp(48), dp(48)))
        raiz.addView(cab)
        val cuerpo = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        raiz.addView(cuerpo, LinearLayout.LayoutParams(-1, 0, 1f))
        val pie = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(Tema.S1), dp(16), dp(Tema.S2))
        }
        raiz.addView(pie)
        d.setContentView(raiz)
        d.window?.statusBarColor = cFondo
        d.window?.navigationBarColor = cFondo
        return Triple(d, cuerpo, pie)
    }

    private fun chipLectura(it: Guardado.Item) =
        if (it.leida) chipDiag("Leída", Tema.VERDE_SUAVE, cVerde) else chipDiag("No la reconoció", Tema.AMARILLO_SUAVE, cAmarillo)

    /**
     * «Lo guardado»: una fila por oferta (foto, texto, hora y si se leyó). Las no leídas primero.
     * Elegir solo cambia la marca de esa fila; la lista no se vuelve a armar.
     */
    private fun mostrarGuardado(preseleccion: Collection<String> = emptyList(), alEnviar: ((List<Guardado.Item>) -> Unit)? = null) {
        val (d, cuerpo, pie) = ventanaCompleta("Lo guardado")
        val elegidos = LinkedHashSet<String>(preseleccion)
        val cargador = java.util.concurrent.Executors.newFixedThreadPool(2)
        var todos = Guardado.lista(this)
        var filtro = if (todos.any { !it.leida }) 0 else 2

        val seg = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            background = redondo(cTarjeta, 12)
            setPadding(dp(3), dp(3), dp(3), dp(3))
        }
        cuerpo.addView(seg, LinearLayout.LayoutParams(-1, -2).apply { setMargins(dp(16), 0, dp(16), dp(Tema.S1)) })
        val lista = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), 0, dp(16), dp(Tema.S2))
        }
        cuerpo.addView(ScrollView(this).apply { isVerticalScrollBarEnabled = false; addView(lista) }, LinearLayout.LayoutParams(-1, 0, 1f))

        val barra = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            background = redondo(cTarjeta, 16)
            setPadding(dp(12), dp(8), dp(8), dp(8))
        }
        val cuenta = texto("", Tema.T_TEXTO, cTexto2, true)
        barra.addView(cuenta, LinearLayout.LayoutParams(0, -2, 1f))
        lateinit var pintar: () -> Unit
        barra.addView(boton("Borrar", Tema.ROJO_SUAVE, cRojo) {
            val sel = todos.filter { it.base in elegidos }
            AlertDialog.Builder(this)
                .setMessage("¿Borrar ${sel.size} ${if (sel.size == 1) "oferta" else "ofertas"} con su foto y texto? No se puede deshacer.")
                .setPositiveButton("Borrar") { _, _ ->
                    Guardado.borrar(sel)
                    elegidos.clear()
                    todos = Guardado.lista(this)
                    pintar()
                }
                .setNegativeButton("Cancelar", null)
                .show()
        }.apply { setPadding(dp(16), 0, dp(16), 0) }, LinearLayout.LayoutParams(-2, dp(44)).apply { rightMargin = dp(8) })
        barra.addView(boton("Enviar", cVerde, Color.BLACK) {
            val sel = todos.filter { it.base in elegidos }
            d.dismiss()
            if (alEnviar != null) alEnviar(sel) else mostrarReporte(sel)
        }.apply { setPadding(dp(20), 0, dp(20), 0) }, LinearLayout.LayoutParams(-2, dp(44)))
        pie.addView(barra)

        fun pintarBarra() {
            val n = elegidos.size
            barra.visibility = if (n > 0) View.VISIBLE else View.GONE
            cuenta.text = if (n == 1) "1 elegida" else "$n elegidas"
        }

        fun marcar(fila: LinearLayout, check: TextView, sel: Boolean) {
            fila.background = GradientDrawable().apply {
                setColor(cTarjeta)
                cornerRadius = dp(14).toFloat()
                if (sel) setStroke(dp(2), Tema.HUESO)
            }
            check.text = if (sel) "✓" else ""
            check.background = if (sel) punto(Tema.HUESO) else GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setStroke(dp(2), cTenue)
            }
        }

        fun fila(it: Guardado.Item): View {
            val f = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                setPadding(dp(10), dp(10), dp(10), dp(10))
            }
            val check = texto("", Tema.T_NOTA, Color.BLACK, true).apply { gravity = Gravity.CENTER }
            f.addView(check, LinearLayout.LayoutParams(dp(24), dp(24)).apply { rightMargin = dp(10); topMargin = dp(2) })
            val col = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
            col.addView(texto(it.titulo(), Tema.T_TEXTO, cTexto, true))
            col.addView(chipLectura(it), LinearLayout.LayoutParams(-2, -2).apply { topMargin = dp(4) })
            val resumen = it.lineas().substringBefore("\n\nCheca entendió").lines().filter { l -> l.isNotBlank() }.take(4)
                .joinToString(" · ")
            col.addView(texto(resumen, Tema.T_NOTA, cTexto2, false).apply {
                typeface = Typeface.MONOSPACE
                maxLines = 2
                ellipsize = TextUtils.TruncateAt.END
            }, margenArriba(6))
            f.addView(col, LinearLayout.LayoutParams(0, -2, 1f))
            val img = ImageView(this).apply {
                scaleType = ImageView.ScaleType.FIT_CENTER
                background = redondo(cTarjeta2, 8)
            }
            f.addView(img, LinearLayout.LayoutParams(dp(54), dp(96)).apply { leftMargin = dp(10) })
            it.foto?.let { foto ->
                cargador.execute {
                    val bm = Guardado.miniatura(foto, dp(54))
                    img.post { if (d.isShowing) img.setImageBitmap(bm) }
                }
            } ?: img.setVisibility(View.GONE)
            marcar(f, check, it.base in elegidos)
            check.isClickable = true
            check.setOnClickListener { _ ->
                if (!elegidos.remove(it.base)) elegidos.add(it.base)
                marcar(f, check, it.base in elegidos)
                pintarBarra()
            }
            f.isClickable = true
            f.setOnClickListener { _ ->
                mostrarDetalle(it, it.base in elegidos, alElegir = { sel ->
                    if (sel) elegidos.add(it.base) else elegidos.remove(it.base)
                    marcar(f, check, sel)
                    pintarBarra()
                }, alBorrar = {
                    elegidos.remove(it.base)
                    todos = Guardado.lista(this)
                    pintar()
                })
            }
            return f
        }

        pintar = {
            seg.removeAllViews()
            val nNo = todos.count { !it.leida }
            listOf("No leídas · $nNo", "Leídas · ${todos.size - nNo}", "Todas").forEachIndexed { i, s ->
                seg.addView(texto(s, Tema.T_NOTA, if (i == filtro) cTexto else cTenue, true).apply {
                    gravity = Gravity.CENTER
                    minHeight = dp(36)
                    background = if (i == filtro) redondo(cTarjeta2, 9) else null
                    isClickable = true
                    setOnClickListener { filtro = i; pintar() }
                }, LinearLayout.LayoutParams(0, -2, 1f))
            }
            lista.removeAllViews()
            val visibles = when (filtro) {
                0 -> todos.filter { !it.leida }
                1 -> todos.filter { it.leida }
                else -> todos
            }
            if (visibles.isEmpty()) {
                lista.addView(texto(
                    when {
                        todos.isEmpty() -> "Aún no hay ofertas guardadas. Se guardan solas mientras Checa lee, si «Guardar pantallas de ofertas» está encendido."
                        filtro == 0 -> "Ninguna oferta sin leer. Checa entendió todas las que vio."
                        else -> "No hay ofertas aquí."
                    },
                    Tema.T_TEXTO, cTenue, false
                ).apply { setLineSpacing(0f, 1.3f) }, margenArriba(Tema.S2))
            }
            visibles.forEachIndexed { i, x -> lista.addView(fila(x), if (i > 0) margenArriba(8) else LinearLayout.LayoutParams(-1, -2)) }
            elegidos.retainAll(todos.map { it.base }.toSet())
            pintarBarra()
        }
        pintar()
        d.setOnDismissListener {
            cargador.shutdown()
            pintarDiag()
        }
        d.show()
    }

    /** Una oferta: foto completa con zoom y el texto que leyó Checa. */
    private fun mostrarDetalle(
        it: Guardado.Item, elegida: Boolean,
        alElegir: ((Boolean) -> Unit)? = null, alBorrar: (() -> Unit)? = null
    ) {
        val (d, cuerpo, pie) = ventanaCompleta(it.titulo())
        if (it.foto != null) {
            val img = ZoomImagen(this).apply { background = redondo(cTarjeta, 12) }
            cuerpo.addView(img, LinearLayout.LayoutParams(-1, 0, 1f).apply { setMargins(dp(16), 0, dp(16), 0) })
            cuerpo.addView(texto("Pellizca o toca dos veces para ampliar", Tema.T_NOTA, cTenue, false).apply {
                gravity = Gravity.CENTER
            }, LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(6) })
            val foto = it.foto
            Thread {
                val bm = Guardado.miniatura(foto, 1200)
                img.post { if (d.isShowing) img.setImageBitmap(bm) }
            }.start()
        }
        val tarjetaTexto = tarjeta()
        val cab = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL }
        cab.addView(texto("Lo que leyó Checa", Tema.T_TEXTO, cTexto, true), LinearLayout.LayoutParams(0, -2, 1f))
        cab.addView(chipLectura(it))
        tarjetaTexto.addView(cab)
        tarjetaTexto.addView(texto(it.lineas(), Tema.T_NOTA, cTexto2, false).apply {
            typeface = Typeface.MONOSPACE
            setTextIsSelectable(true)
        }, margenArriba(Tema.S1))
        cuerpo.addView(ScrollView(this).apply { isVerticalScrollBarEnabled = false; addView(tarjetaTexto) },
            LinearLayout.LayoutParams(-1, if (it.foto != null) dp(170) else 0, if (it.foto != null) 0f else 1f).apply {
                setMargins(dp(16), dp(Tema.S1), dp(16), 0)
            })

        val fila = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        fila.addView(boton("Borrar", Tema.ROJO_SUAVE, cRojo) {
            AlertDialog.Builder(this)
                .setMessage("¿Borrar esta oferta con su foto y texto?")
                .setPositiveButton("Borrar") { _, _ ->
                    Guardado.borrar(it)
                    d.dismiss()
                    alBorrar?.invoke() ?: pintarDiag()
                }
                .setNegativeButton("Cancelar", null)
                .show()
        }, LinearLayout.LayoutParams(0, dp(48), 1f).apply { rightMargin = dp(8) })
        if (alElegir != null) {
            fila.addView(boton(if (elegida) "Quitar del reporte" else "Elegir para el reporte", if (elegida) cTarjeta2 else cVerde,
                if (elegida) cTexto else Color.BLACK) {
                alElegir(!elegida)
                d.dismiss()
            }, LinearLayout.LayoutParams(0, dp(48), 1.6f))
        } else {
            fila.addView(boton("Enviar a soporte", cVerde, Color.BLACK) {
                d.dismiss()
                mostrarReporte(listOf(it))
            }, LinearLayout.LayoutParams(0, dp(48), 1.6f))
        }
        pie.addView(fila)
        d.show()
    }

    /** Reporte a soporte: motivo, ofertas elegidas y datos del equipo en un solo paso. */
    private fun mostrarReporte(items: List<Guardado.Item>) {
        val (d, cuerpo, pie) = ventanaCompleta("Reporte a soporte")
        val col = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), 0, dp(16), dp(Tema.S2))
        }
        cuerpo.addView(ScrollView(this).apply { isVerticalScrollBarEnabled = false; addView(col) }, LinearLayout.LayoutParams(-1, -1))

        val que = tarjeta()
        que.addView(texto("¿Qué pasó?", Tema.T_TEXTO, cTexto, true))
        val chips = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        fun pintarMotivos() {
            chips.removeAllViews()
            motivosReporte.chunked(2).forEachIndexed { r, par ->
                val f = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
                par.forEachIndexed { k, s ->
                    val i = r * 2 + k
                    val sel = i == reporteMotivo
                    f.addView(texto(s, Tema.T_NOTA, if (sel) Tema.ASFALTO else cTexto2, true).apply {
                        gravity = Gravity.CENTER
                        minHeight = dp(40)
                        background = redondo(if (sel) Tema.HUESO else cTarjeta2, 50)
                        isClickable = true
                        setOnClickListener { reporteMotivo = i; pintarMotivos() }
                    }, LinearLayout.LayoutParams(0, -2, 1f).apply { if (k > 0) leftMargin = dp(8) })
                }
                chips.addView(f, if (r > 0) margenArriba(8) else LinearLayout.LayoutParams(-1, -2))
            }
        }
        pintarMotivos()
        que.addView(chips, margenArriba(Tema.S1))
        val campo = EditText(this).apply {
            setText(reporteTexto)
            hint = "Cuéntalo con tus palabras (opcional)"
            setHintTextColor(cTenue)
            setTextColor(cTexto)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, Tema.T_TEXTO)
            background = redondo(cTarjeta2, 12)
            setPadding(dp(12), dp(10), dp(12), dp(10))
            minLines = 2
            gravity = Gravity.TOP
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_MULTI_LINE or InputType.TYPE_TEXT_FLAG_CAP_SENTENCES
        }
        que.addView(campo, margenArriba(Tema.S1))
        col.addView(que)

        val env = tarjeta()
        env.addView(texto("Se va a enviar", Tema.T_TEXTO, cTexto, true))
        fun dato(k: String, v: String) {
            val f = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
            f.addView(texto(k, Tema.T_NOTA, cTexto2, false), LinearLayout.LayoutParams(0, -2, 1f))
            f.addView(texto(v, Tema.T_NOTA, cTexto, true).apply { gravity = Gravity.END },
                LinearLayout.LayoutParams(0, -2, 1.4f))
            env.addView(f, margenArriba(6))
        }
        val conFoto = items.count { it.foto != null }
        dato("Ofertas elegidas", when {
            items.isEmpty() -> "Ninguna"
            conFoto > 0 -> "${items.size} (foto y texto)"
            else -> "${items.size} (texto)"
        })
        dato("Tu equipo", "${Build.MANUFACTURER} ${Build.MODEL} · Android ${Build.VERSION.RELEASE}")
        dato("Checa", "${Actualizacion.instalada(this)} · lector ${if (servicioActivo()) "activo" else "apagado"}")
        dato("Cuenta", Cuenta.correo(this).ifEmpty { "sin sesión" })
        env.addView(boton(if (items.isEmpty()) "Elegir ofertas" else "Cambiar ofertas", cTarjeta2, cTexto) {
            reporteTexto = campo.text.toString()
            d.dismiss()
            mostrarGuardado(items.map { it.base }) { sel -> mostrarReporte(sel) }
        }, margenArriba(Tema.S1))
        var borrarDespues = false
        if (items.isNotEmpty()) {
            env.addView(interruptor("Borrar lo enviado al terminar", null, false) { borrarDespues = it }, margenArriba(4))
        }
        col.addView(env, margenArriba(Tema.ENTRE_TARJETAS))

        pie.addView(boton("Enviar por WhatsApp", cVerde, Color.BLACK) {
            reporteTexto = campo.text.toString()
            val msj = buildString {
                append("Hola, necesito ayuda con Checa: ").append(motivosReporte[reporteMotivo].lowercase()).append("\n")
                if (reporteTexto.isNotBlank()) append(reporteTexto.trim()).append("\n")
                if (items.isNotEmpty()) {
                    append("\n— Ofertas —\n")
                    items.forEachIndexed { i, o ->
                        append("${i + 1}) ${o.titulo()} · ${if (o.leida) "leída" else "no la reconoció"}\n")
                        append(o.lineas().lines().filter { l -> l.isNotBlank() }.take(15).joinToString("\n")).append("\n\n")
                    }
                }
                append("\n— Datos de mi equipo —\n").append(Soporte.resumen(this@MainActivity))
            }
            Guardado.enviar(this, items, msj, Soporte.WHATSAPP)
            if (borrarDespues) Guardado.borrarDespues(this, items)
            reporteTexto = ""
            reporteMotivo = 0
            d.dismiss()
        }, LinearLayout.LayoutParams(-1, dp(52)))
        pie.addView(texto(
            if (conFoto > 0) "Se abre WhatsApp con todo listo. Elige el chat de soporte de Checa."
            else "Se abre el chat de soporte de Checa con el mensaje listo.",
            Tema.T_NOTA, cTenue, false
        ).apply { gravity = Gravity.CENTER }, margenArriba(Tema.S1))
        d.setOnDismissListener { pintarDiag() }
        d.show()
    }

    // =====================================================================
    // Permisos y lectura de pantalla
    // =====================================================================

    /** Dónde está el lector dentro de Accesibilidad: en Samsung y OPPO no está en la lista principal. */
    private fun pistaAccesibilidad(): String =
        if (Build.MANUFACTURER.lowercase().contains("samsung"))
            "Entra a «Aplicaciones instaladas» y activa «Checa – lector de ofertas»"
        else
            "Busca «Checa – lector de ofertas» (puede estar en «Aplicaciones descargadas» o «instaladas») y actívalo"

    private fun abrirAccesibilidad() {
        Toast.makeText(this, pistaAccesibilidad(), Toast.LENGTH_LONG).show()
        // Si Ajustes lo entiende (Android estándar), baja hasta «Checa» y lo resalta; si no, lo ignora.
        val yo = ComponentName(this, RutaService::class.java).flattenToString()
        val abrir = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
            .putExtra(":settings:fragment_args_key", yo)
            .putExtra(":settings:show_fragment_args", Bundle().apply { putString(":settings:fragment_args_key", yo) })
        try {
            startActivity(abrir)
        } catch (_: Exception) {
            try { startActivity(Intent(Settings.ACTION_SETTINGS)) } catch (_: Exception) { }
        }
    }

    /** true = ya se permitieron los ajustes restringidos (o no aplica), false = siguen bloqueados, null = este Android no deja saberlo. */
    private fun ajustesRestringidosPermitidos(): Boolean? {
        if (Build.VERSION.SDK_INT < 33) return true
        return try {
            val ops = getSystemService(Context.APP_OPS_SERVICE) as android.app.AppOpsManager
            ops.unsafeCheckOpNoThrow("android:access_restricted_settings", android.os.Process.myUid(), packageName) ==
                android.app.AppOpsManager.MODE_ALLOWED
        } catch (_: Exception) { null }
    }

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
        val volver = desdeAviso
        desdeAviso = false
        if (!Licencia.activa(this)) {
            Toast.makeText(this, "Elige un paquete para seguir usando Checa", Toast.LENGTH_LONG).show()
            irAPaquetes()
            return
        }
        if (!servicioActivo()) {
            Toast.makeText(this, "Primero activa \"Checa – lector de ofertas\" en Accesibilidad", Toast.LENGTH_LONG).show()
            abrirAccesibilidad()
            return
        }
        if (Build.VERSION.SDK_INT >= 33 &&
            checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 7)
        }
        volverAtras = volver
        val mpm = getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        @Suppress("DEPRECATION")
        startActivityForResult(mpm.createScreenCaptureIntent(), REQ_CAPTURA)
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        @Suppress("DEPRECATION")
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode != REQ_CAPTURA) return
        val volver = volverAtras
        volverAtras = false
        if (resultCode != RESULT_OK || data == null) {
            Toast.makeText(this, "No se dio permiso de grabar pantalla", Toast.LENGTH_SHORT).show()
            return
        }
        val i = Intent(this, CapturaService::class.java)
            .putExtra(CapturaService.EXTRA_CODIGO, resultCode)
            .putExtra(CapturaService.EXTRA_DATOS, data)
        startForegroundService(i)
        handler.postDelayed({ actualizarEstado() }, 800)
        // Si vino del aviso al abrir Uber/DiDi, regresa a la app de manejo.
        if (volver) moveTaskToBack(true)
    }

    // =====================================================================
    // Componentes visuales
    // =====================================================================

    /** Ventana de detalle con el mismo estilo de la app. */
    private fun hoja(titulo: String, color: Int?, cuerpo: LinearLayout): Dialog {
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
        return d
    }

    private fun redondo(color: Int, radio: Int) = GradientDrawable().apply {
        setColor(color)
        cornerRadius = dp(radio).toFloat()
    }

    private fun punto(color: Int) = GradientDrawable().apply {
        shape = GradientDrawable.OVAL
        setColor(color)
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
        // Marca v2: títulos grandes en Bricolage, el resto en DM Sans.
        typeface = if (tam >= Tema.T_TITULO) Fuentes.titulo else if (negrita) Fuentes.negrita else Fuentes.texto
        if (tam >= Tema.T_TITULO) letterSpacing = -0.02f
    }

    /** Número: grande en Bricolage; chico, de ancho fijo (JetBrains Mono) para que no se mueva al cambiar. */
    private fun numero(s: String, tam: Float, color: Int) = texto(s, tam, color, false).apply {
        typeface = if (tam >= 20f) Fuentes.titulo else Fuentes.numero
    }

    private fun icono(tipo: Int, color: Int, @Suppress("UNUSED_PARAMETER") lado: Int) = ImageView(this).apply {
        setImageDrawable(Icono(tipo, color))
    }

    private fun iconoCirculo(tipo: Int, @Suppress("UNUSED_PARAMETER") activo: Boolean) = ImageView(this).apply {
        setImageDrawable(Icono(tipo, Tema.HUESO))
        setPadding(dp(10), dp(10), dp(10), dp(10))
        background = punto(cTarjeta2)
    }

    private fun etiqueta(s: String) = texto(s, 11f, cTenue, true).apply {
        typeface = Fuentes.numero
        letterSpacing = 0.12f
        setPadding(dp(4), dp(Tema.S2), 0, dp(Tema.S1))
    }

    /** Etiqueta dentro de una tarjeta (sin el margen de las de afuera). */
    private fun etiquetaTarjeta(s: String) = etiqueta(s).apply { setPadding(0, 0, 0, 0) }

    private fun boton(s: String, fondo: Int, colorTexto: Int, accion: () -> Unit) = TextView(this).apply {
        // Marca v2: el botón principal es hueso con texto asfalto, en píldora (el verde queda para decidir).
        val principal = fondo == Tema.VERDE
        text = s
        gravity = Gravity.CENTER
        setTextColor(if (principal) Tema.ASFALTO else colorTexto)
        setTextSize(TypedValue.COMPLEX_UNIT_SP, Tema.T_TEXTO)
        typeface = fuenteMedia
        minHeight = dp(Tema.ALTO_TOQUE)
        setPadding(dp(Tema.S2), dp(12), dp(Tema.S2), dp(12))
        background = ripple(if (principal) Tema.HUESO else fondo, 28)
        isClickable = true
        setOnClickListener { accion() }
    }

    /** Botón secundario de la marca v2: transparente con contorno humo y texto hueso. */
    private fun botonContorno(s: String, accion: () -> Unit) = boton(s, Color.TRANSPARENT, cTexto, accion).apply {
        background = RippleDrawable(
            ColorStateList.valueOf(Color.argb(40, 255, 255, 255)),
            GradientDrawable().apply {
                cornerRadius = dp(28).toFloat()
                setColor(Color.TRANSPARENT)
                setStroke(dp(1) + dp(1) / 2, Tema.HUMO)
            },
            redondo(Color.WHITE, 28)
        )
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
        thumbTintList = ColorStateList(estados, intArrayOf(Tema.HUESO, Tema.TENUE))
        trackTintList = ColorStateList(estados, intArrayOf(Color.argb(150, 244, 244, 239), Tema.HUMO))
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
        val valorTxt = texto("$valor$unidad", Tema.T_TEXTO, cTexto, true)
        cabecera.addView(valorTxt)
        val barra = SeekBar(this).apply {
            this.max = max - min
            progress = (valor - min).coerceIn(0, max - min)
            progressTintList = ColorStateList.valueOf(Tema.HUESO)
            thumbTintList = ColorStateList.valueOf(Tema.HUESO)
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
        /** La notificación «Checa no está leyendo» abre la app pidiendo iniciar la lectura. */
        const val EXTRA_AUTO_INICIAR = "auto_iniciar"
        /** La notificación «Hay una versión nueva» abre la app con la ventana para actualizar. */
        const val EXTRA_ACTUALIZAR = "actualizar"
        /** Desde cuántos días antes de que acabe la beta se avisa del cobro. */
        private const val DIAS_AVISO_FIN_BETA = 30
        /** Días de uso y ofertas leídas antes de pedir la opinión. */
        private const val DIAS_PARA_OPINION = 7
        private const val OFERTAS_PARA_OPINION = 10
    }
}
