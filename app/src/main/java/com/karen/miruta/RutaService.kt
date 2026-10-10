package com.karen.miruta

import android.accessibilityservice.AccessibilityService
import android.graphics.Rect
import android.os.Handler
import android.os.Looper
import android.view.WindowManager
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo

/** Zona de la pantalla que ocupa una app (Uber o DiDi). */
data class Zona(val app: String, val rect: Rect, val capa: Int)

class RutaService : AccessibilityService() {
    private lateinit var prefs: Prefs
    private var alertas: Alertas? = null
    private var appActual = ""
    private val handler = Handler(Looper.getMainLooper())
    private var pendiente = false
    private var ultimaCaptura = 0

    /** Un pop-up por app, para que dos ofertas simultáneas no se pisen. */
    private val popups = HashMap<String, Popup>()
    private val activas = LinkedHashMap<String, Pair<Oferta, Rect?>>()
    private val ultimaFirma = HashMap<String, Pair<String, Long>>()

    override fun onServiceConnected() {
        super.onServiceConnected()
        prefs = Prefs(this)
        alertas = Alertas(this, prefs)
        instancia = this
    }

    override fun onUnbind(intent: android.content.Intent?): Boolean {
        instancia = null
        return super.onUnbind(intent)
    }

    // ------------------------------------------------------------------
    // Pop-ups y comparación
    // ------------------------------------------------------------------

    /** Compatibilidad: oferta sin app/zona conocida. */
    fun mostrarOferta(oferta: Oferta) = mostrarOferta(oferta, appActual, null)

    /** Muestra la oferta de [app] sobre su [zona] (sin repetir la misma en 60 s). */
    fun mostrarOferta(oferta: Oferta, app: String, zona: Rect?) {
        if (!Licencia.activa(this)) return
        val clave = app.ifEmpty { "?" }
        // Con el pop-up encima, el lector ve la oferta tapada y lee otros montos.
        // Si los tramos son los mismos (o se perdió uno), es la misma oferta: no se toca.
        activas[clave]?.first?.let { a ->
            val mismosTramos = Math.abs(a.km - oferta.km) < 0.05 && Math.abs(a.min - oferta.min) < 0.5
            val tramoTapado = a.recogidaMin > 0 && oferta.recogidaMin == 0.0
            if (mismosTramos || tramoTapado) return
        }
        val firma = "${oferta.tarifa}|${oferta.km}|${oferta.min}"
        val ahora = System.currentTimeMillis()
        ultimaFirma[clave]?.let { (f, t) -> if (f == firma && ahora - t < 60_000) return }
        ultimaFirma[clave] = firma to ahora

        popups.getOrPut(clave) {
            Popup(this, prefs, WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY).also { p ->
                p.alCerrar = {
                    activas.remove(clave)
                    redibujar(null)
                }
            }
        }
        activas[clave] = oferta to zona
        redibujar(clave)

        val nivel = oferta.nivel(prefs)
        alertas?.avisar(nivel)
        Historial.registrar(this, oferta, app, nivel)
    }

    /** Zonas que tapan los pop-ups visibles. */
    fun rectsPopups(): List<Rect> = popups.values.mapNotNull { it.rect() }

    /** Vuelve a pintar los pop-ups visibles con su marca (mejor / 2ª opción). */
    private fun redibujar(nueva: String?) {
        val marcas = Comparador.marcas(activas.mapValues { it.value.first }, prefs)
        for ((app, par) in activas) {
            popups[app]?.mostrar(par.first, app, par.second, marcas[app] ?: Popup.SIN_MARCA, app == nueva)
        }
    }

    // ------------------------------------------------------------------
    // Zonas de pantalla por app (pantalla dividida, ventanas flotantes)
    // ------------------------------------------------------------------

    fun nombreApp(pkg: String): String? {
        val p = pkg.lowercase()
        return when {
            prefs.uber && p.contains("uber") -> "Uber"
            prefs.didi && p.contains("didi") -> "DiDi"
            else -> null
        }
    }

    /** Ventanas visibles de Uber/DiDi, de la más alta (encima) a la más baja. */
    fun zonasApps(): List<Zona> {
        if (!::prefs.isInitialized) return emptyList()
        val out = ArrayList<Zona>()
        try {
            for (w in windows) {
                // En pantalla dividida algunas tablets no dan el contenido de la
                // ventana sin foco: entonces se usa su título ("Uber Driver", "DiDi…").
                val pkg = w.root?.packageName?.toString() ?: w.title?.toString() ?: continue
                val app = nombreApp(pkg) ?: continue
                val r = Rect()
                w.getBoundsInScreen(r)
                if (!r.isEmpty) out.add(Zona(app, r, w.layer))
            }
        } catch (_: Exception) { }
        return out.sortedByDescending { it.capa }
    }

    // ------------------------------------------------------------------
    // Lectura por accesibilidad (cuando la app deja leer su texto)
    // ------------------------------------------------------------------

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (!::prefs.isInitialized) return
        val pkg = event?.packageName?.toString() ?: return
        if (event.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            val p = pkg.lowercase()
            when {
                p.contains("uber") -> appActual = "Uber"
                p.contains("didi") -> appActual = "DiDi"
                p != packageName && !p.contains("systemui") -> appActual = "Otra"
            }
        }
        if (nombreApp(pkg) == null) return
        if (pendiente) return
        pendiente = true
        handler.postDelayed({
            pendiente = false
            analizar()
        }, 300)
    }

    override fun onInterrupt() {}

    override fun onDestroy() {
        instancia = null
        popups.values.forEach { it.ocultar() }
        super.onDestroy()
    }

    /** Analiza cada ventana de Uber/DiDi POR SEPARADO para no mezclar ofertas. */
    private fun analizar() {
        try {
            val ventanas = windows ?: return
            for (w in ventanas) {
                val raiz = w.root ?: continue
                val pkg = raiz.packageName?.toString() ?: continue
                if (pkg == packageName) continue
                val app = nombreApp(pkg) ?: continue
                val textos = LinkedHashSet<String>()
                recolectar(raiz, textos, 0)
                if (textos.isEmpty()) continue
                if (prefs.diagnostico) {
                    val firma = textos.hashCode()
                    if (firma != ultimaCaptura) {
                        ultimaCaptura = firma
                        prefs.guardarCaptura("[$app · accesibilidad]\n" + textos.joinToString("\n"))
                    }
                }
                val oferta = Parser.analizar(textos.toList()) ?: continue
                val r = Rect()
                w.getBoundsInScreen(r)
                mostrarOferta(oferta, app, r)
            }
        } catch (_: Exception) { }
    }

    private fun recolectar(n: AccessibilityNodeInfo, salida: MutableSet<String>, nivel: Int) {
        if (nivel > 40) return
        val t = (n.text ?: n.contentDescription)?.toString()?.trim()
        if (!t.isNullOrEmpty()) salida.add(t)
        for (i in 0 until n.childCount) {
            n.getChild(i)?.let { recolectar(it, salida, nivel + 1) }
        }
    }

    companion object {
        @Volatile
        var instancia: RutaService? = null
    }
}
