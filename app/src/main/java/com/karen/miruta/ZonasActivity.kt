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
import android.graphics.drawable.GradientDrawable
import android.location.LocationManager
import android.os.Bundle
import android.view.Gravity
import android.view.MotionEvent
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.CustomZoomButtonsController
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Overlay
import org.osmdroid.views.overlay.Polygon
import java.io.File

/**
 * Mapa de zonas bloqueadas (10.60). «+» activa el dibujo: se traza la zona con el dedo y se
 * cierra sola. Tocar una zona roja pregunta si se borra. Las zonas se guardan en [Prefs.zonasTexto].
 */
class ZonasActivity : Activity() {
    private lateinit var prefs: Prefs
    private lateinit var mapa: MapView
    private lateinit var contador: TextView
    private lateinit var aviso: TextView
    private lateinit var boton: TextView
    private lateinit var interruptor: TextView
    private var zonas = ArrayList<Zonas.Zona>()
    private var dibujando = false
    private val trazo = ArrayList<GeoPoint>()
    private var arriba = 0
    private var abajo = 0

    private val rojo = Color.rgb(239, 68, 68)
    private val asfalto = Color.rgb(12, 12, 16)
    private val hueso = Color.rgb(244, 244, 239)

    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()

    private fun fondo(color: Int, radio: Int, borde: Int = 0) = GradientDrawable().apply {
        setColor(color)
        cornerRadius = dp(radio).toFloat()
        if (borde != 0) setStroke(dp(2), borde)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        prefs = Prefs(this)
        zonas = ArrayList(Zonas.leer(prefs.zonasTexto))

        WindowCompat.setDecorFitsSystemWindows(window, false)
        WindowCompat.getInsetsController(window, window.decorView).isAppearanceLightStatusBars = false

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

        // Cabecera: volver, título y número de zonas.
        val cab = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setBackgroundColor(asfalto)
            setPadding(dp(8), dp(8), dp(16), dp(8))
        }
        cab.addView(TextView(this).apply {
            text = "←"; textSize = 24f; setTextColor(hueso); gravity = Gravity.CENTER
            setOnClickListener { finish() }
        }, LinearLayout.LayoutParams(dp(48), dp(48)))
        cab.addView(TextView(this).apply {
            text = "Zonas bloqueadas"; textSize = 20f; setTextColor(hueso)
            setTypeface(typeface, android.graphics.Typeface.BOLD)
        }, LinearLayout.LayoutParams(0, -2, 1f))
        contador = TextView(this).apply { textSize = 14f; setTextColor(Color.rgb(154, 154, 164)) }
        cab.addView(contador)

        aviso = TextView(this).apply {
            textSize = 15f; setTextColor(hueso); gravity = Gravity.CENTER
            background = fondo(asfalto, 16, Color.rgb(42, 42, 51))
            setPadding(dp(16), dp(12), dp(16), dp(12))
        }
        boton = TextView(this).apply {
            textSize = 30f; setTextColor(Color.WHITE); gravity = Gravity.CENTER
            background = fondo(rojo, 32)
            setOnClickListener { alternarDibujo() }
        }
        interruptor = TextView(this).apply {
            textSize = 14f; setTextColor(hueso); gravity = Gravity.CENTER
            background = fondo(asfalto, 24, Color.rgb(42, 42, 51))
            setPadding(dp(16), 0, dp(16), 0)
            setOnClickListener {
                prefs.zonasOn = !prefs.zonasOn
                pintarControles()
            }
        }

        val raiz = FrameLayout(this).apply { setBackgroundColor(asfalto) }
        raiz.addView(mapa, FrameLayout.LayoutParams(-1, -1))
        raiz.addView(cab, FrameLayout.LayoutParams(-1, -2, Gravity.TOP))
        raiz.addView(aviso, FrameLayout.LayoutParams(-1, -2, Gravity.BOTTOM))
        raiz.addView(interruptor, FrameLayout.LayoutParams(-2, dp(48), Gravity.BOTTOM or Gravity.START))
        raiz.addView(boton, FrameLayout.LayoutParams(dp(64), dp(64), Gravity.BOTTOM or Gravity.END))
        setContentView(raiz)

        // Borde a borde: el mapa llena la pantalla y los controles quedan dentro de las barras.
        ViewCompat.setOnApplyWindowInsetsListener(raiz) { _, insets ->
            val b = insets.getInsets(WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout())
            arriba = b.top; abajo = b.bottom
            cab.setPadding(dp(8) + b.left, dp(8) + b.top, dp(16) + b.right, dp(8))
            (aviso.layoutParams as FrameLayout.LayoutParams).apply {
                setMargins(dp(12) + b.left, 0, dp(12) + b.right, dp(96) + b.bottom)
            }
            (interruptor.layoutParams as FrameLayout.LayoutParams).apply {
                setMargins(dp(12) + b.left, 0, 0, dp(16) + b.bottom)
            }
            (boton.layoutParams as FrameLayout.LayoutParams).apply {
                setMargins(0, 0, dp(16) + b.right, dp(16) + b.bottom)
            }
            insets
        }
        ViewCompat.requestApplyInsets(raiz)

        centrar()
        pintarMapa()
        pintarControles()
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
                if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                    requestPermissions(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION), 7)
                }
            }
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 7) {
            ubicacion()?.let { mapa.controller.setZoom(13.0); mapa.controller.setCenter(it); pintarMapa() }
        }
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

    private fun pintarControles() {
        contador.text = if (zonas.size == 1) "1 zona" else "${zonas.size} zonas"
        boton.text = if (dibujando) "✕" else "+"
        aviso.text = when {
            dibujando -> "Dibuja la zona con el dedo. Al soltarlo se cierra sola."
            zonas.isEmpty() -> "Toca + y dibuja la zona a la que no quieres ir."
            else -> "Toca una zona roja para borrarla."
        }
        interruptor.text = if (prefs.zonasOn) "Bloqueo: sí" else "Bloqueo: no"
    }

    /** Vuelve a poner en el mapa las zonas, tu punto y la capa que recibe el dibujo. */
    private fun pintarMapa() {
        mapa.overlays.clear()
        zonas.forEachIndexed { i, z ->
            val poli = Polygon(mapa).apply {
                points = z.puntos.map { GeoPoint(it.lat, it.lon) }
                fillPaint.color = Color.argb(90, 239, 68, 68)
                outlinePaint.color = rojo
                outlinePaint.strokeWidth = 5f
                infoWindow = null
                setOnClickListener(Polygon.OnClickListener { _, _, _ -> preguntarBorrar(i); true })
            }
            mapa.overlays.add(poli)
        }
        ubicacion()?.let { mapa.overlays.add(PuntoAzul(it)) }
        mapa.overlays.add(Capa())
        mapa.invalidate()
    }

    private fun preguntarBorrar(i: Int) {
        if (dibujando) return
        AlertDialog.Builder(this)
            .setTitle("¿Borrar la zona ${i + 1}?")
            .setMessage("Las ofertas que vayan ahí dejarán de salir en rojo por esta zona.")
            .setPositiveButton("Borrar") { _, _ ->
                if (i < zonas.size) { zonas.removeAt(i); guardar() }
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
        zonas.add(Zonas.Zona(Zonas.simplificar(pts)))
        guardar()
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
