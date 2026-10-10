package com.karen.miruta

import android.accessibilityservice.AccessibilityService
import android.os.Handler
import android.os.Looper
import android.view.WindowManager
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo

class RutaService : AccessibilityService() {
    private lateinit var prefs: Prefs
    private var popup: Popup? = null
    private val handler = Handler(Looper.getMainLooper())
    private var pendiente = false
    private var ultimaFirma = ""
    private var ultimaVez = 0L
    private var ultimaCaptura = 0

    override fun onServiceConnected() {
        super.onServiceConnected()
        prefs = Prefs(this)
        popup = Popup(this, prefs, WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY)
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (!::prefs.isInitialized) return
        val pkg = event?.packageName?.toString() ?: return
        if (!esApp(pkg)) return
        if (pendiente) return
        pendiente = true
        handler.postDelayed({
            pendiente = false
            analizar()
        }, 300)
    }

    override fun onInterrupt() {}

    override fun onDestroy() {
        popup?.ocultar()
        super.onDestroy()
    }

    private fun esApp(pkg: String): Boolean {
        val p = pkg.lowercase()
        return (prefs.uber && p.contains("uber")) || (prefs.didi && p.contains("didi"))
    }

    private fun analizar() {
        val textos = LinkedHashSet<String>()
        var paquete = ""
        try {
            val raices = ArrayList<AccessibilityNodeInfo>()
            val ventanas = windows
            if (ventanas.isNullOrEmpty()) {
                rootInActiveWindow?.let { raices.add(it) }
            } else {
                for (w in ventanas) w.root?.let { raices.add(it) }
            }
            for (r in raices) {
                val p = r.packageName?.toString() ?: continue
                if (p == packageName || !esApp(p)) continue
                paquete = p
                recolectar(r, textos, 0)
            }
        } catch (e: Exception) {
            return
        }
        if (textos.isEmpty()) return

        if (prefs.diagnostico) {
            val firma = textos.hashCode()
            if (firma != ultimaCaptura) {
                ultimaCaptura = firma
                prefs.guardarCaptura(paquete + "\n" + textos.joinToString("\n"))
            }
        }

        val oferta = Parser.analizar(textos.toList()) ?: return
        val firma = "${oferta.tarifa}|${oferta.km}|${oferta.min}"
        val ahora = System.currentTimeMillis()
        if (firma == ultimaFirma && ahora - ultimaVez < 60_000) return
        ultimaFirma = firma
        ultimaVez = ahora
        popup?.mostrar(oferta)
    }

    private fun recolectar(n: AccessibilityNodeInfo, salida: MutableSet<String>, nivel: Int) {
        if (nivel > 40) return
        val t = (n.text ?: n.contentDescription)?.toString()?.trim()
        if (!t.isNullOrEmpty()) salida.add(t)
        for (i in 0 until n.childCount) {
            n.getChild(i)?.let { recolectar(it, salida, nivel + 1) }
        }
    }
}
