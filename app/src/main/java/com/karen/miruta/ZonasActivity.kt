package com.karen.miruta

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.drawable.GradientDrawable
import android.location.LocationManager
import android.os.Bundle
import android.text.TextUtils
import android.util.TypedValue
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.BoundingBox
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.CustomZoomButtonsController
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Overlay
import org.osmdroid.views.overlay.Polygon
import java.io.File

/**
 * Zonas bloqueadas (10.60; rediseño 10.63). El mapa llena la pantalla; arriba, el título y el
 * interruptor del bloqueo; abajo, un panel con la lista de zonas (nombre, ofertas que bloqueó
 * en 30 días, renombrar y borrar) y el botón «Dibujar zona». En modo dibujo se traza la zona
 * con el dedo y se cierra sola. Las zonas se guardan en [Prefs.zonasTexto].
 */
class ZonasActivity : Activity() {
    private lateinit var prefs: Prefs
    private lateinit var mapa: MapView
    private lateinit var interruptor: TextView
    private lateinit var titulo: TextView
    private lateinit var nota: TextView
    private lateinit var lista: LinearLayout
    private lateinit var scroll: ScrollView
    private lateinit var principal: TextView
    private var zonas = ArrayList<Zonas.Zona>()
    /** Ofertas bloqueadas por zona en 30 días (por nombre). */
    private var bloqueos: Map<String, Int> = emptyMap()
    private var dibujando = false
    private val trazo = ArrayList<GeoPoint>()

    private val rojo = Tema.ROJO
    private val fondoC get() = Tema.ASFALTO
    private val tarjeta get() = Tema.TABLERO
    private val texto get() = Tema.TEXTO
    private val tenue get() = Tema.TENUE
    private val borde get() = Tema.BORDE

    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()

    private fun fondo(color: Int, radio: Int, bordeC: Int = 0, arribaSolo: Boolean = false) = GradientDrawable().apply {
        setColor(color)
        val r = dp(radio).toFloat()
        if (arribaSolo) cornerRadii = floatArrayOf(r, r, r, r, 0f, 0f, 0f, 0f) else cornerRadius = r
        if (bordeC != 0) setStroke(dp(1), bordeC)
    }

    private fun txt(t: String, sp: Float, color: Int, tf: android.graphics.Typeface = Fuentes.texto) = TextView(this).apply {
        text = t
        setTextSize(TypedValue.COMPLEX_UNIT_SP, sp)
        setTextColor(color)
        typeface = tf
        includeFontPadding = false
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        prefs = Prefs(this)
        Fuentes.iniciar(this)
        zonas = ArrayList(Zonas.leer(prefs.zonasTexto))

        WindowCompat.setDecorFitsSystemWindows(window, false)
        WindowCompat.getInsetsController(window, window.decorView).isAppearanceLightStatusBars = true

        // OpenStreetMap pide que cada app se identifique; el caché va en la carpeta privada (sin permisos de archivos).
        Configuration.getInstance().apply {
            load(this@ZonasActivity, getSharedPreferences("osmdroid", Context.MODE_PRIVATE))
            userAgentValue = packageName
            osmdroidBasePath = File(cacheDir, "osmdroid")
            osmdroidTileCache = File(cacheDir, "osmdroid/tiles")
        }

        mapa = MapView(this).apply {
            setTileSource(TileSourceFactory.MAPNIK)
            setMultiTouchControls(true)
            zoomController.setVisibility(CustomZoomButtonsController.Visibility.NEVER)
            minZoomLevel = 4.0
        }

        // Arriba: tarjeta flotante con volver, título e interruptor del bloqueo.
        val cab = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            background = fondo(tarjeta, Tema.R_TARJETA, borde)
            elevation = dp(4).toFloat()
            setPadding(dp(4), dp(4), dp(8), dp(4))
        }
        cab.addView(txt("←", 22f, texto).apply {
            gravity = Gravity.CENTER
            setOnClickListener { if (dibujando) alternarDibujo() else finish() }
        }, LinearLayout.LayoutParams(dp(48), dp(48)))
        cab.addView(txt("Zonas bloqueadas", 18f, texto, Fuentes.titulo).apply {
            maxLines = 1; ellipsize = TextUtils.TruncateAt.END
        }, LinearLayout.LayoutParams(0, -2, 1f))
        interruptor = txt("", 13f, texto, Fuentes.media).apply {
            gravity = Gravity.CENTER
            setPadding(dp(12), 0, dp(12), 0)
            setOnClickListener {
                prefs.zonasOn = !prefs.zonasOn
                pintarMapa()
                pintarControles()
            }
        }
        cab.addView(interruptor, LinearLayout.LayoutParams(-2, dp(36)))

        // Botón redondo para volver a tu ubicación.
        val yo = txt("◎", 22f, texto).apply {
            gravity = Gravity.CENTER
            background = fondo(tarjeta, 24, borde)
            elevation = dp(4).toFloat()
            contentDescription = "Ir a mi ubicación"
            setOnClickListener { irAMiUbicacion() }
        }

        // Abajo: panel con la lista de zonas y el botón principal.
        val panel = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = fondo(tarjeta, 20, borde, arribaSolo = true)
            elevation = dp(8).toFloat()
            setPadding(dp(16), dp(10), dp(16), dp(16))
        }
        panel.addView(View(this).apply { background = fondo(borde, 2) },
            LinearLayout.LayoutParams(dp(36), dp(4)).apply { gravity = Gravity.CENTER_HORIZONTAL; bottomMargin = dp(12) })
        titulo = txt("", Tema.T_SUB, texto, Fuentes.titulo)
        panel.addView(titulo)
        nota = txt("", Tema.T_NOTA, tenue).apply { setLineSpacing(0f, 1.15f) }
        panel.addView(nota, LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(4) })
        lista = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        scroll = ScrollView(this).apply { addView(lista); isVerticalScrollBarEnabled = false }
        panel.addView(scroll, LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(8) })
        principal = txt("", 16f, fondoC, Fuentes.negrita).apply {
            gravity = Gravity.CENTER
            setOnClickListener { alternarDibujo() }
        }
        panel.addView(principal, LinearLayout.LayoutParams(-1, dp(52)).apply { topMargin = dp(12) })

        val raiz = FrameLayout(this).apply { setBackgroundColor(fondoC) }
        raiz.addView(mapa, FrameLayout.LayoutParams(-1, -1))
        raiz.addView(cab, FrameLayout.LayoutParams(-1, -2, Gravity.TOP))
        raiz.addView(yo, FrameLayout.LayoutParams(dp(48), dp(48), Gravity.END or Gravity.BOTTOM))
        raiz.addView(panel, FrameLayout.LayoutParams(-1, -2, Gravity.BOTTOM))
        setContentView(raiz)

        // Borde a borde: el mapa llena la pantalla y los controles quedan dentro de las barras.
        ViewCompat.setOnApplyWindowInsetsListener(raiz) { _, insets ->
            val b = insets.getInsets(WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout())
            (cab.layoutParams as FrameLayout.LayoutParams).setMargins(dp(12) + b.left, dp(8) + b.top, dp(12) + b.right, 0)
            panel.setPadding(dp(16) + b.left, dp(10), dp(16) + b.right, dp(16) + b.bottom)
            cab.requestLayout()
            insets
        }
        // El botón de ubicación va justo encima del panel, y la lista no pasa del 35 % de la pantalla.
        panel.addOnLayoutChangeListener { _, _, top, _, bottom, _, _, _, _ ->
            val lp = yo.layoutParams as FrameLayout.LayoutParams
            val m = (bottom - top) + dp(12)
            if (lp.bottomMargin != m) { lp.setMargins(0, 0, dp(16), m); yo.layoutParams = lp }
        }
        scroll.layoutParams.height = -2
        ViewCompat.requestApplyInsets(raiz)

        centrar()
        pintarMapa()
        pintarControles()
        contarBloqueos()
    }

    override fun onResume() { super.onResume(); mapa.onResume() }
    override fun onPause() { super.onPause(); mapa.onPause() }
    override fun onDestroy() { super.onDestroy(); mapa.onDetach() }

    // ------------------------------------------------------------------

    private fun ubicacion(): GeoPoint? {
        val ok = checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
            checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
        if (!ok) return null
        return try {
            val lm = getSystemService(Context.LOCATION_SERVICE) as LocationManager
            lm.getProviders(true).mapNotNull { lm.getLastKnownLocation(it) }
                .maxByOrNull { it.time }?.let { GeoPoint(it.latitude, it.longitude) }
        } catch (_: SecurityException) { null }
    }

    /** Centra en tu ubicación; si no hay, en tus zonas; si no, pide el permiso una vez. */
    private fun centrar() {
        val yo = ubicacion()
        val primera = zonas.firstOrNull()?.puntos?.firstOrNull()
        when {
            yo != null -> { mapa.controller.setZoom(13.0); mapa.controller.setCenter(yo) }
            primera != null -> { mapa.controller.setZoom(13.0); mapa.controller.setCenter(GeoPoint(primera.lat, primera.lon)) }
            else -> {
                mapa.controller.setZoom(5.0)
                mapa.controller.setCenter(GeoPoint(23.6, -102.5))
                pedirUbicacion()
            }
        }
    }

    private fun pedirUbicacion() {
        if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION), 7)
        }
    }

    private fun irAMiUbicacion() {
        val yo = ubicacion()
        if (yo == null) { pedirUbicacion(); return }
        mapa.controller.animateTo(yo, maxOf(mapa.zoomLevelDouble, 14.0), 400L)
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 7) {
            ubicacion()?.let { mapa.controller.setZoom(13.0); mapa.controller.setCenter(it); pintarMapa() }
        }
    }

    /** Cuenta en otro hilo las ofertas que cada zona puso en rojo en los últimos 30 días. */
    private fun contarBloqueos() {
        val app = applicationContext
        Thread {
            val cuenta = HashMap<String, Int>()
            try {
                for (r in Historial.ultimosDias(app, 30)) {
                    for (n in setOf(r.zonaOrigen, r.zonaDestino)) if (n.isNotEmpty()) cuenta[n] = (cuenta[n] ?: 0) + 1
                }
            } catch (_: Exception) { }
            runOnUiThread { if (!isFinishing) { bloqueos = cuenta; pintarControles() } }
        }.start()
    }

    private fun guardar() {
        prefs.zonasTexto = Zonas.escribir(zonas)
        pintarMapa()
        pintarControles()
    }

    private fun alternarDibujo() {
        dibujando = !dibujando
        trazo.clear()
        pintarControles()
        mapa.invalidate()
    }

    /** Panel e interruptor según el modo (dibujo o lista) y si el bloqueo está activo. */
    private fun pintarControles() {
        val activo = prefs.zonasOn
        interruptor.text = if (activo) "● Activo" else "○ Apagado"
        interruptor.setTextColor(if (activo) Tema.VERDE else tenue)
        interruptor.background = fondo(if (activo) Tema.VERDE_SUAVE else Tema.CAMPO, 18)

        lista.removeAllViews()
        if (dibujando) {
            titulo.text = "Dibuja la zona"
            nota.text = "Con un dedo, rodea la zona a la que no quieres ir. Al soltar se cierra sola. Para mover el mapa, cancela."
            scroll.visibility = View.GONE
            principal.text = "Cancelar"
            principal.setTextColor(texto)
            principal.background = fondo(Tema.CAMPO, Tema.R_BOTON, borde)
            return
        }
        scroll.visibility = View.VISIBLE
        titulo.text = when (zonas.size) { 0 -> "Sin zonas todavía"; 1 -> "Tu zona"; else -> "Tus ${zonas.size} zonas" }
        nota.text = when {
            !activo -> "El bloqueo está apagado: las ofertas no se revisan contra tus zonas."
            zonas.isEmpty() -> "Si la recogida o el destino de una oferta cae en una zona, el pop-up sale en rojo y dice cuál. Necesita internet."
            else -> "Si la recogida o el destino cae aquí, el pop-up sale en rojo. Toca una zona para verla."
        }
        zonas.forEachIndexed { i, _ -> lista.addView(fila(i)) }
        // Máximo ~3 filas a la vista; el resto se desliza.
        scroll.layoutParams.height = if (zonas.size > 3) dp(64) * 3 else -2
        scroll.requestLayout()
        principal.text = "+  Dibujar zona"
        principal.setTextColor(fondoC)
        principal.background = fondo(texto, Tema.R_BOTON)
    }

    /** Una fila de la lista: color, nombre, ofertas bloqueadas, renombrar y borrar. */
    private fun fila(i: Int): View {
        val nombre = Zonas.nombre(zonas, i)
        val f = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            minimumHeight = dp(56)
            background = fondo(Tema.CAMPO, 12)
            setPadding(dp(12), dp(6), dp(4), dp(6))
            setOnClickListener { encuadrar(i) }
        }
        f.addView(View(this).apply { background = fondo(if (prefs.zonasOn) rojo else tenue, 4) },
            LinearLayout.LayoutParams(dp(14), dp(14)))
        val col = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(12), 0, dp(8), 0) }
        col.addView(txt(nombre, Tema.T_TEXTO + 1, texto, Fuentes.media).apply {
            maxLines = 1; ellipsize = TextUtils.TruncateAt.END
        })
        val n = bloqueos[nombre] ?: 0
        col.addView(txt(
            when (n) { 0 -> "Sin ofertas bloqueadas en 30 días"; 1 -> "1 oferta bloqueada en 30 días"; else -> "$n ofertas bloqueadas en 30 días" },
            Tema.T_NOTA, tenue
        ), LinearLayout.LayoutParams(-2, -2).apply { topMargin = dp(3) })
        f.addView(col, LinearLayout.LayoutParams(0, -2, 1f))
        f.addView(txt("✎", 18f, texto).apply {
            gravity = Gravity.CENTER
            contentDescription = "Cambiar nombre"
            setOnClickListener { pedirNombre(nombre) { nuevo -> renombrar(i, nuevo) } }
        }, LinearLayout.LayoutParams(dp(44), dp(44)))
        f.addView(txt("🗑", 17f, rojo).apply {
            gravity = Gravity.CENTER
            contentDescription = "Borrar zona"
            setOnClickListener { preguntarBorrar(i) }
        }, LinearLayout.LayoutParams(dp(44), dp(44)))
        return FrameLayout(this).apply {
            setPadding(0, 0, 0, dp(Tema.ENTRE_TARJETAS))
            addView(f, FrameLayout.LayoutParams(-1, -2))
        }
    }

    /** Lleva el mapa a la zona [i], con margen para la cabecera y el panel. */
    private fun encuadrar(i: Int) {
        val z = zonas.getOrNull(i) ?: return
        val caja = BoundingBox.fromGeoPoints(z.puntos.map { GeoPoint(it.lat, it.lon) })
        mapa.post { try { mapa.zoomToBoundingBox(caja.increaseByScale(1.6f), true, dp(48)) } catch (_: Exception) { } }
    }

    private fun renombrar(i: Int, nuevo: String) {
        val z = zonas.getOrNull(i) ?: return
        zonas[i] = Zonas.Zona(z.puntos, Zonas.limpiarNombre(nuevo).ifEmpty { Zonas.nombre(zonas, i) })
        guardar()
    }

    /** Diálogo para el nombre de la zona; vacío = el sugerido. */
    private fun pedirNombre(actual: String, listo: (String) -> Unit) {
        val campo = EditText(this).apply {
            setText(actual)
            setSelection(text.length)
            setSingleLine()
            filters = arrayOf(android.text.InputFilter.LengthFilter(30))
        }
        val marco = FrameLayout(this).apply { setPadding(dp(20), dp(8), dp(20), 0); addView(campo) }
        AlertDialog.Builder(this)
            .setTitle("Nombre de la zona")
            .setMessage("Sale en el pop-up: «Destino en zona bloqueada · nombre».")
            .setView(marco)
            .setPositiveButton("Guardar") { _, _ -> listo(campo.text.toString().ifBlank { actual }) }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    /** Vuelve a poner en el mapa las zonas (con su nombre), tu punto y la capa que recibe el dibujo. */
    private fun pintarMapa() {
        mapa.overlays.clear()
        val activo = prefs.zonasOn
        val color = if (activo) rojo else Color.rgb(140, 140, 150)
        zonas.forEachIndexed { i, z ->
            val poli = Polygon(mapa).apply {
                points = z.puntos.map { GeoPoint(it.lat, it.lon) }
                fillPaint.color = Color.argb(if (activo) 80 else 45, Color.red(color), Color.green(color), Color.blue(color))
                outlinePaint.color = color
                outlinePaint.strokeWidth = dp(2).toFloat()
                infoWindow = null
                setOnClickListener(Polygon.OnClickListener { _, _, _ -> if (!dibujando) preguntarBorrar(i); true })
            }
            mapa.overlays.add(poli)
        }
        mapa.overlays.add(Etiquetas())
        ubicacion()?.let { mapa.overlays.add(PuntoAzul(it)) }
        mapa.overlays.add(Capa())
        mapa.invalidate()
    }

    private fun preguntarBorrar(i: Int) {
        if (dibujando) return
        AlertDialog.Builder(this)
            .setTitle("¿Borrar «${Zonas.nombre(zonas, i)}»?")
            .setMessage("Las ofertas que vayan ahí dejarán de salir en rojo por esta zona.")
            .setPositiveButton("Borrar") { _, _ ->
                if (i < zonas.size) {
                    // Las zonas sin nombre guardan el que tenían, así las demás no cambian de número.
                    val conNombre = zonas.mapIndexed { j, z -> Zonas.Zona(z.puntos, Zonas.nombre(zonas, j)) }
                    zonas = ArrayList(conNombre)
                    zonas.removeAt(i)
                    guardar()
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun terminarTrazo() {
        val pts = trazo.map { Zonas.Punto(it.latitude, it.longitude) }
        trazo.clear()
        dibujando = false
        if (pts.size < 8) {
            Toast.makeText(this, "Dibuja una zona más grande", Toast.LENGTH_SHORT).show()
            pintarControles()
            mapa.invalidate()
            return
        }
        val puntos = Zonas.simplificar(pts)
        // El nombre sale en el pop-up («Destino en zona bloqueada · Centro»). Vacío = «Zona N».
        var n = zonas.size + 1
        while (zonas.indices.any { Zonas.nombre(zonas, it) == "Zona $n" }) n++
        val sugerido = "Zona $n"
        val campo = EditText(this).apply {
            hint = sugerido
            setSingleLine()
            filters = arrayOf(android.text.InputFilter.LengthFilter(30))
        }
        val marco = FrameLayout(this).apply { setPadding(dp(20), dp(8), dp(20), 0); addView(campo) }
        fun agregar(nombre: String) {
            zonas.add(Zonas.Zona(puntos, Zonas.limpiarNombre(nombre).ifEmpty { sugerido }))
            guardar()
        }
        AlertDialog.Builder(this)
            .setTitle("¿Cómo se llama esta zona?")
            .setMessage("Sale en el pop-up cuando una oferta pasa por aquí.")
            .setView(marco)
            .setPositiveButton("Guardar") { _, _ -> agregar(campo.text.toString()) }
            .setNegativeButton("Sin nombre") { _, _ -> agregar("") }
            .setCancelable(false)
            .show()
        pintarControles()
        mapa.invalidate()
    }

    // ------------------------------------------------------------------

    private inner class PuntoAzul(val p: GeoPoint) : Overlay() {
        private val azul = Paint().apply { color = Color.rgb(59, 130, 246); isAntiAlias = true }
        private val blanco = Paint().apply { color = Color.WHITE; isAntiAlias = true }
        override fun draw(c: Canvas, v: MapView, shadow: Boolean) {
            if (shadow) return
            val pt = v.projection.toPixels(p, null)
            c.drawCircle(pt.x.toFloat(), pt.y.toFloat(), dp(9).toFloat(), blanco)
            c.drawCircle(pt.x.toFloat(), pt.y.toFloat(), dp(6).toFloat(), azul)
        }
    }

    /** Nombre de cada zona en su centro, en una etiqueta oscura (se esconde si la zona se ve muy chica). */
    private inner class Etiquetas : Overlay() {
        private val letra = Paint().apply {
            color = Oscuro.HUESO; isAntiAlias = true; textSize = dp(12).toFloat(); typeface = Fuentes.media
        }
        private val caja = Paint().apply { color = Color.argb(220, 12, 12, 16); isAntiAlias = true }

        override fun draw(c: Canvas, v: MapView, shadow: Boolean) {
            if (shadow) return
            zonas.forEachIndexed { i, z ->
                val pts = z.puntos.map { v.projection.toPixels(GeoPoint(it.lat, it.lon), null) }
                val ancho = pts.maxOf { it.x } - pts.minOf { it.x }
                if (ancho < dp(56)) return@forEachIndexed
                val cx = pts.sumOf { it.x }.toFloat() / pts.size
                val cy = pts.sumOf { it.y }.toFloat() / pts.size
                val t = Zonas.nombre(zonas, i)
                val w = letra.measureText(t)
                val r = RectF(cx - w / 2 - dp(8), cy - dp(12), cx + w / 2 + dp(8), cy + dp(12))
                c.drawRoundRect(r, dp(12).toFloat(), dp(12).toFloat(), caja)
                c.drawText(t, cx - w / 2, cy + letra.textSize / 3, letra)
            }
        }
    }

    /** Capa de arriba: en modo dibujo se queda con el dedo y pinta el trazo; si no, deja pasar el mapa. */
    private inner class Capa : Overlay() {
        private val linea = Paint().apply {
            color = rojo; style = Paint.Style.STROKE; strokeWidth = dp(4).toFloat(); isAntiAlias = true
        }

        override fun draw(c: Canvas, v: MapView, shadow: Boolean) {
            if (shadow || trazo.size < 2) return
            val ruta = Path()
            trazo.forEachIndexed { i, g ->
                val pt = v.projection.toPixels(g, null)
                if (i == 0) ruta.moveTo(pt.x.toFloat(), pt.y.toFloat()) else ruta.lineTo(pt.x.toFloat(), pt.y.toFloat())
            }
            c.drawPath(ruta, linea)
        }

        override fun onTouchEvent(e: MotionEvent, mapView: MapView): Boolean {
            if (!dibujando) return false
            val g = mapView.projection.fromPixels(e.x.toInt(), e.y.toInt())
            when (e.actionMasked) {
                MotionEvent.ACTION_DOWN -> { trazo.clear(); trazo.add(GeoPoint(g.latitude, g.longitude)) }
                MotionEvent.ACTION_MOVE -> trazo.add(GeoPoint(g.latitude, g.longitude))
                MotionEvent.ACTION_UP -> { trazo.add(GeoPoint(g.latitude, g.longitude)); terminarTrazo(); return true }
                MotionEvent.ACTION_CANCEL -> { trazo.clear() }
            }
            mapView.invalidate()
            return true
        }
    }
}
