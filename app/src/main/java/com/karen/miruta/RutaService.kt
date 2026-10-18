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

    /** Un pop-up por app, para que dos ofertas simultáneas no se pisen. */
    private val popups = HashMap<String, Popup>()
    private val activas = LinkedHashMap<String, Pair<Oferta, Rect?>>()
    private val ultimaFirma = HashMap<String, Pair<Oferta, Long>>()
    private val ultimoAviso = HashMap<String, Long>()

    override fun onServiceConnected() {
        super.onServiceConnected()
        prefs = Prefs(this)
        alertas = Alertas(this, prefs)
        instancia = this
        soloUberYDidi()
    }

    /**
     * Limita el lector a las apps de Uber y DiDi instaladas, para no tocar otras apps
     * (algunos bancos bloquean los lectores que vigilan todas las apps).
     */
    private fun soloUberYDidi() {
        try {
            val lanzador = android.content.Intent(android.content.Intent.ACTION_MAIN)
                .addCategory(android.content.Intent.CATEGORY_LAUNCHER)
            val pkgs = packageManager.queryIntentActivities(lanzador, 0)
                .map { it.activityInfo.packageName }
                .filter { val p = it.lowercase(); p.contains("uber") || p.contains("didi") }
                .toMutableSet()
            pkgs.addAll(listOf("com.ubercab.driver", "com.didiglobal.driver"))
            val info = serviceInfo ?: return
            info.packageNames = pkgs.toTypedArray()
            serviceInfo = info
        } catch (_: Exception) { }
    }

    /** Apaga el lector (para abrir apps de banco). Se vuelve a encender en Accesibilidad. */
    fun pausar() {
        popups.values.forEach { it.ocultar() }
        disableSelf()
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
        if (!Licencia.activa(this) || !Cuenta.conectada(this)) return
        prefs.ultimaOferta = System.currentTimeMillis()
        val clave = app.ifEmpty { "?" }
        // Con el pop-up encima, el lector ve la oferta tapada y lee otros montos.
        // Si los tramos son los mismos (o se perdió uno), es la misma oferta: no se toca.
        activas[clave]?.first?.let { a ->
            val mismosTramos = Math.abs(a.km - oferta.km) < 0.05 && Math.abs(a.min - oferta.min) < 0.5
            val tramoTapado = a.recogidaMin > 0 && oferta.recogidaMin == 0.0
            if (mismosTramos || tramoTapado) return
        }
        // La misma oferta (con pequeñas variaciones del OCR) no vuelve a salir mientras siga en
        // pantalla: cada vez que se ve de nuevo se renueva el plazo de 60 s. Así, al cerrar el
        // pop-up con la ✕ o por tiempo, no reaparece (parpadeo).
        val ahora = System.currentTimeMillis()
        ultimaFirma[clave]?.let { (o, t) ->
            if (ahora - t < 60_000 && parecida(o, oferta)) { ultimaFirma[clave] = o to ahora; return }
        }
        ultimaFirma[clave] = oferta to ahora

        popups.getOrPut(clave) {
            Popup(this, prefs, WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY).also { p ->
                p.alCerrar = {
                    activas.remove(clave)
                    redibujar(null)
                }
            }
        }
        // Plan de 1 mes: un pop-up a la vez (Uber y DiDi juntos es del plan de 6 meses).
        if (Licencia.basico(this)) {
            activas.keys.filter { it != clave }.forEach { otra ->
                activas.remove(otra)
                popups[otra]?.ocultar()
            }
        }
        activas[clave] = oferta to zona
        redibujar(clave)

        val nivel = oferta.nivel(prefs)
        alertas?.avisar(nivel)
        Historial.registrar(this, oferta, app, nivel)
    }

    private fun parecida(a: Oferta, b: Oferta) =
        Math.abs(a.km - b.km) < 0.15 && Math.abs(a.min - b.min) < 1.1 && Math.abs(a.tarifa - b.tarifa) < 1.0

    /** true si el pop-up de [app] está en pantalla (entonces tapa la oferta y el lector no la ve completa). */
    fun popupVisible(app: String?): Boolean = popups[app ?: "?"]?.rect() != null

    /** Zonas que tapan los pop-ups visibles. */
    fun rectsPopups(): List<Rect> = popups.values.mapNotNull { it.rect() }

    /** Vuelve a pintar los pop-ups visibles con su marca (mejor / 2ª opción). */
    private fun redibujar(nueva: String?) {
        val marcas = if (Licencia.basico(this)) emptyMap<String, Int>()
            else Comparador.marcas(activas.mapValues { it.value.first }, prefs)
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
        if (event.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) avisarApertura(nombreApp(pkg) ?: "?")
        if (pendiente) return
        pendiente = true
        handler.postDelayed({
            pendiente = false
            analizar()
        }, 300)
    }

    /**
     * Al abrir Uber o DiDi con la lectura apagada, avisa con una notificación. Android pide
     * permiso de captura cada vez, así que no se puede iniciar sola: un toque en el aviso lo hace.
     */
    private fun avisarApertura(app: String) {
        try {
            if (!prefs.avisarApertura || CapturaService.corriendo || !Licencia.activa(this)) return
            // El límite es por app: abrir DiDi no debe callar el aviso de Uber.
            val ahora = System.currentTimeMillis()
            if (ahora - (ultimoAviso[app] ?: 0L) < 3 * 60_000L) return
            ultimoAviso[app] = ahora
            val nm = getSystemService(android.content.Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
            nm.createNotificationChannel(
                android.app.NotificationChannel("avisos", "Avisos de Checa", android.app.NotificationManager.IMPORTANCE_HIGH)
            )
            val abrir = android.content.Intent(this, MainActivity::class.java)
                .putExtra(MainActivity.EXTRA_AUTO_INICIAR, true)
                .addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK or android.content.Intent.FLAG_ACTIVITY_SINGLE_TOP)
            val pi = android.app.PendingIntent.getActivity(
                this, 4, abrir, android.app.PendingIntent.FLAG_IMMUTABLE or android.app.PendingIntent.FLAG_UPDATE_CURRENT
            )
            nm.notify(
                3,
                android.app.Notification.Builder(this, "avisos")
                    .setContentTitle("Checa no está leyendo")
                    .setContentText("Toca para iniciar la lectura y ver el pop-up de cada oferta.")
                    .setSmallIcon(R.drawable.ic_notif)
                    .setContentIntent(pi)
                    .setAutoCancel(true)
                    .setTimeoutAfter(2 * 60_000L)
                    .build()
            )
        } catch (_: Exception) { }
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
                val leida = Parser.analizar(textos.toList())
                if (prefs.diagnostico && textos.any { TRAMO.containsMatchIn(it) }) {
                    val firma = leida?.let { "${it.tarifa}|${it.km}|${it.min}" } ?: "?"
                    if (Guardado.esNueva(app, leida != null, firma)) {
                        Guardado.guardar(this, app, leida != null, textos.take(40).joinToString("\n") +
                            "\n\nCheca entendió (accesibilidad): " +
                            (leida?.let { "tarifa \$${it.tarifa}, ${it.km} km, ${it.min} min" } ?: "nada (no reconoció la oferta)"), null)
                    }
                }
                val oferta = leida ?: continue
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
        /** Un tramo «N min (X km)»: la pantalla tiene una oferta. */
        private val TRAMO = Regex("""\d\s?min\s*\(""", RegexOption.IGNORE_CASE)
        @Volatile
        var instancia: RutaService? = null
    }
}
