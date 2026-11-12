package com.karen.miruta

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import java.util.concurrent.Executors

/**
 * Revisión en segundo plano de cada oferta (10.48). El pop-up sale al instante con el lector
 * local; después la función `confirmar-oferta` de Supabase revisa la misma lectura. Si no
 * coincide y la revisión pasa los mismos límites del lector, [RutaService] corrige el pop-up
 * sin avisar. No se muestra nada de esto al conductor. Sin sesión, sin internet o sin saldo
 * no pasa nada: se queda la lectura local.
 * Solo se envían líneas cortas con números (montos, min, km, ⚡): nunca direcciones.
 */
object Confirmador {
    private val hilo = Executors.newSingleThreadExecutor()
    /** Lo que tardó la revisión anterior (teléfono → servidor → teléfono); -1 = sin dato. */
    @Volatile private var msRevision = -1L
    private val enCurso = HashSet<String>()
    /** Lectura local → corrección (null = coincidió o no hubo respuesta). Así no se pregunta dos veces. */
    private val revisadas = object : LinkedHashMap<String, Oferta?>(32, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, Oferta?>?) = size > 40
    }

    private val util = Regex("""(min|km|\$|mxn|/km|\dx\b|×|⚡|incluid|adicional|por inicio)""", RegexOption.IGNORE_CASE)

    // ---------- Medición de consumo (10.50) ----------
    // Solo números del día: ofertas con pop-up y correcciones aplicadas. Viajan dentro de la
    // siguiente revisión (sin llamadas extra). Al cambiar de día, el del día anterior se manda
    // hasta que el servidor lo recibe.
    private fun medicion(ctx: Context) = ctx.applicationContext.getSharedPreferences("medicion", Context.MODE_PRIVATE)

    private fun hoy(): String = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        .apply { timeZone = TimeZone.getTimeZone("America/Mexico_City") }.format(Date())

    @Synchronized
    private fun cambiarDia(ctx: Context) {
        val m = medicion(ctx)
        val d = hoy()
        val dia = m.getString("dia", null)
        if (dia == d) return
        val e = m.edit()
        if (dia != null && (m.getInt("ofertas", 0) > 0 || m.getInt("correcciones", 0) > 0)) {
            e.putString("ant_dia", dia).putInt("ant_ofertas", m.getInt("ofertas", 0))
                .putInt("ant_correcciones", m.getInt("correcciones", 0))
        }
        e.putString("dia", d).putInt("ofertas", 0).putInt("correcciones", 0).apply()
    }

    /** Una oferta nueva con pop-up (se cuenta aunque no haya sesión). */
    @Synchronized
    fun contarOferta(ctx: Context) {
        cambiarDia(ctx)
        val m = medicion(ctx)
        m.edit().putInt("ofertas", m.getInt("ofertas", 0) + 1).apply()
    }

    /** La app aplicó una corrección de la revisión. */
    @Synchronized
    fun contarCorreccion(ctx: Context) {
        cambiarDia(ctx)
        val m = medicion(ctx)
        m.edit().putInt("correcciones", m.getInt("correcciones", 0) + 1).apply()
    }

    @Synchronized
    private fun conteos(ctx: Context): Pair<JSONObject, JSONObject?> {
        cambiarDia(ctx)
        val m = medicion(ctx)
        val actual = JSONObject().put("dia", m.getString("dia", null) ?: hoy())
            .put("ofertas", m.getInt("ofertas", 0)).put("correcciones", m.getInt("correcciones", 0))
        val ant = m.getString("ant_dia", null)?.let {
            JSONObject().put("dia", it).put("ofertas", m.getInt("ant_ofertas", 0))
                .put("correcciones", m.getInt("ant_correcciones", 0))
        }
        return actual to ant
    }

    @Synchronized
    private fun anteriorRecibido(ctx: Context, dia: String) {
        val m = medicion(ctx)
        if (m.getString("ant_dia", null) == dia) {
            m.edit().remove("ant_dia").remove("ant_ofertas").remove("ant_correcciones").apply()
        }
    }

    /** Si el servidor guardó un caso (`caso`), manda la captura reciente de esa app para revisarlo. */
    private fun subirCaptura(ctx: Context, app: String, r: JSONObject) {
        val caso = r.optLong("caso", 0L)
        if (caso <= 0L) return
        Guardado.tomarReciente(app)?.let { Cuenta.subirCaptura(ctx, caso, it) }
    }

    private fun firma(o: Oferta) = "${o.tarifa}|${o.km}|${o.min}"

    /** Líneas que se pueden enviar: cortas, con un número y con algo de la oferta. */
    fun filtrar(lineas: List<String>): List<String> = lineas.asSequence()
        .map { it.trim() }
        .filter { it.isNotEmpty() && it.length <= 60 && it.any(Char::isDigit) && util.containsMatchIn(it) &&
            !it.contains("total", ignoreCase = true) }
        .take(30).toList()

    // ---------- Rescate (10.51) ----------
    // Hay una oferta en pantalla (min y km + un monto) pero el lector no la entiende: se piden los
    // datos al revisor y, si pasan Parser.plausible, sale el pop-up. Una vez por pantalla, y con
    // pausa por app para no gastar si no era una oferta (navegación, pantallas raras).
    private val rescates = object : LinkedHashMap<Int, Oferta?>(16, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<Int, Oferta?>?) = size > 20
    }
    private val rescateEnCurso = HashSet<String>()
    private val pausaRescate = HashMap<String, Long>()
    private const val PAUSA_SIN_OFERTA_MS = 60_000L
    private const val ENTRE_RESCATES_MS = 3_000L

    /** Pide el rescate de una oferta que el lector no entendió; [listo] recibe la oferta (en otro hilo). */
    fun rescatar(ctx: Context, app: String, lineas: List<String>, moneda: Moneda, lector: String, listo: (Oferta) -> Unit) {
        if (!Cuenta.conectada(ctx)) return
        val enviar = filtrar(lineas)
        if (enviar.isEmpty()) return
        val clave = enviar.hashCode()
        val ahora = System.currentTimeMillis()
        synchronized(this) {
            if (rescates.containsKey(clave)) { rescates[clave]?.let(listo); return }
            if (app in rescateEnCurso || ahora < (pausaRescate[app] ?: 0L)) return
            rescateEnCurso.add(app)
            pausaRescate[app] = ahora + ENTRE_RESCATES_MS
        }
        val app0 = ctx.applicationContext
        val pant = pantalla(app, null)
        try {
            hilo.execute {
                var oferta: Oferta? = null
                var noEraOferta = false
                try {
                    val (conteo, conteoAnterior) = conteos(app0)
                    val cuerpo = JSONObject()
                        .put("modo", "rescate")
                        .put("lector", lector)
                        .put("equipo", equipo())
                        .put("pantalla", pant ?: JSONObject.NULL)
                    val lote = tomarCoincidencias()
                    if (lote.isNotEmpty()) cuerpo.put("coincidencias", JSONArray(lote))
                        .put("conteo", conteo)
                        .put("conteoAnterior", conteoAnterior ?: JSONObject.NULL)
                        .put("app", app)
                        .put("moneda", moneda.codigo)
                        .put("version", BuildConfig.VERSION_NAME)
                        .put("lineas", JSONArray(enviar))
                    val respuesta = Cuenta.revisarOferta(app0, cuerpo.toString())
                    if (respuesta != null && conteoAnterior != null) anteriorRecibido(app0, conteoAnterior.getString("dia"))
                    val r = JSONObject(respuesta ?: "{}")
                    subirCaptura(app0, app, r)
                    val ia = r.optJSONObject("ia")
                    if (r.optBoolean("ok") && ia != null) {
                        if (!ia.optBoolean("oferta")) noEraOferta = true
                        else {
                            val c = Oferta(
                                tarifa = ia.optDouble("tarifa", 0.0),
                                km = ia.optDouble("km", 0.0),
                                min = ia.optDouble("min", 0.0),
                                recogidaKm = ia.optDouble("recKm", 0.0),
                                recogidaMin = ia.optDouble("recMin", 0.0),
                                multiplicador = ia.optDouble("multi", 0.0).let { if (it > 1.0) it else 0.0 }
                            )
                            if (Parser.plausible(c, lineas, moneda)) oferta = c else noEraOferta = true
                        }
                    }
                } catch (_: Exception) { }
                synchronized(this) {
                    rescateEnCurso.remove(app)
                    rescates[clave] = oferta
                    if (noEraOferta) pausaRescate[app] = System.currentTimeMillis() + PAUSA_SIN_OFERTA_MS
                }
                oferta?.let(listo)
            }
        } catch (_: Exception) {
            synchronized(this) { rescateEnCurso.remove(app) }
        }
    }

    private fun local(o: Oferta) = JSONObject()
        .put("tarifa", o.tarifa).put("km", o.km).put("min", o.min)
        .put("recKm", o.recogidaKm).put("recMin", o.recogidaMin).put("multi", o.multiplicador)

    /** La corrección ya conocida para esta lectura local, si la hubo. */
    @Synchronized
    fun corregida(o: Oferta): Oferta? = revisadas[firma(o)]

    /** Pide la revisión de [o] una sola vez; [listo] se llama (en otro hilo) solo si hay que corregir. */
    /** La lectura del otro lector cuando no coinciden (10.57): la IA revisa las dos juntas. */
    class Otra(val lector: String, val lineas: List<String>, val oferta: Oferta, val ms: Long = -1)

    /** Teléfono y Android (10.58): explican por qué falla un lector en ciertos equipos. */
    private fun equipo(): JSONObject = JSONObject()
        .put("telefono", "${android.os.Build.MANUFACTURER} ${android.os.Build.MODEL}".trim().take(40))
        .put("android", android.os.Build.VERSION.RELEASE ?: "")

    /** Pantalla al momento de la lectura (se toma en el hilo principal, antes de enviar). */
    private fun pantalla(app: String, o: Oferta?): JSONObject? =
        try { RutaService.instancia?.contexto(app, o) } catch (_: Exception) { null }

    // ---------- Coincidencias (10.57) ----------
    // Ofertas donde el texto y la foto leyeron lo mismo: no se pregunta a la IA, solo se anotan.
    // Viajan dentro de la siguiente revisión o, al juntar 10, en un envío propio (sin IA).
    private val coincidencias = ArrayList<JSONObject>()
    private const val COINCIDENCIAS_LOTE = 10
    private const val COINCIDENCIAS_MAX = 50

    fun anotarCoincidencia(ctx: Context, app: String, o: Oferta, msTexto: Long, msFoto: Long) {
        val fila = JSONObject().put("app", app)
            .put("tarifa", o.tarifa).put("km", o.km).put("min", o.min)
            .put("recKm", o.recogidaKm).put("recMin", o.recogidaMin).put("multi", o.multiplicador)
            .put("msTexto", msTexto).put("msFoto", msFoto).put("visto", System.currentTimeMillis())
            .put("pantalla", pantalla(app, o) ?: JSONObject.NULL)
        val lote = synchronized(this) {
            coincidencias.add(fila)
            while (coincidencias.size > COINCIDENCIAS_MAX) coincidencias.removeAt(0)
            if (coincidencias.size < COINCIDENCIAS_LOTE || !Cuenta.conectada(ctx)) null else tomarCoincidencias()
        } ?: return
        val app0 = ctx.applicationContext
        try {
            hilo.execute {
                val cuerpo = JSONObject().put("modo", "coincidencias").put("version", BuildConfig.VERSION_NAME).put("equipo", equipo())
                    .put("coincidencias", JSONArray(lote))
                val r = try { Cuenta.revisarOferta(app0, cuerpo.toString()) } catch (_: Exception) { null }
                if (r == null) devolverCoincidencias(lote)
            }
        } catch (_: Exception) { devolverCoincidencias(lote) }
    }

    @Synchronized
    private fun tomarCoincidencias(): List<JSONObject> {
        val l = ArrayList(coincidencias)
        coincidencias.clear()
        return l
    }

    @Synchronized
    private fun devolverCoincidencias(l: List<JSONObject>) {
        coincidencias.addAll(0, l)
        while (coincidencias.size > COINCIDENCIAS_MAX) coincidencias.removeAt(0)
    }

    fun revisar(ctx: Context, app: String, lineas: List<String>, o: Oferta, moneda: Moneda, msPopup: Long, lector: String, otra: Otra? = null, listo: (Oferta) -> Unit) {
        if (!Cuenta.conectada(ctx)) return
        val enviar = filtrar(lineas)
        if (enviar.isEmpty()) return
        val f = firma(o)
        synchronized(this) {
            if (revisadas.containsKey(f) || !enCurso.add(f)) return
        }
        val app0 = ctx.applicationContext
        val pant = pantalla(app, o)
        try {
            hilo.execute {
                var nueva: Oferta? = null
                try {
                    val t0 = android.os.SystemClock.elapsedRealtime()
                    val (conteo, conteoAnterior) = conteos(app0)
                    val cuerpo = JSONObject()
                        .put("conteo", conteo)
                        .put("conteoAnterior", conteoAnterior ?: JSONObject.NULL)
                        .put("app", app)
                        .put("moneda", moneda.codigo)
                        .put("version", BuildConfig.VERSION_NAME)
                        .put("lineas", JSONArray(enviar))
                        .put("msPopup", msPopup)
                        .put("lector", lector)
                        .put("equipo", equipo())
                        .put("pantalla", pant ?: JSONObject.NULL)
                        .put("msRevisionAnterior", msRevision)
                        .put("local", local(o))
                    if (otra != null) {
                        cuerpo.put("modo", "desacuerdo")
                            .put("lectorOtro", otra.lector)
                            .put("lineasOtro", JSONArray(filtrar(otra.lineas)))
                            .put("localOtro", local(otra.oferta))
                            .put("msFoto", otra.ms)
                    }
                    val lote = tomarCoincidencias()
                    if (lote.isNotEmpty()) cuerpo.put("coincidencias", JSONArray(lote))
                    val respuesta = Cuenta.revisarOferta(app0, cuerpo.toString())
                    if (respuesta == null && lote.isNotEmpty()) devolverCoincidencias(lote)
                    // El servidor respondió (aunque sea sin saldo): ya anotó el conteo del día anterior.
                    if (respuesta != null && conteoAnterior != null) anteriorRecibido(app0, conteoAnterior.getString("dia"))
                    val r = JSONObject(respuesta ?: "{}")
                    subirCaptura(app0, app, r)
                    // Ida y vuelta completa de la revisión, vista desde el teléfono: va en la siguiente.
                    msRevision = android.os.SystemClock.elapsedRealtime() - t0
                    val ia = r.optJSONObject("ia")
                    if (r.optBoolean("ok") && !r.optBoolean("coincide", true) && ia != null && ia.optBoolean("oferta")) {
                        val c = Oferta(
                            tarifa = ia.optDouble("tarifa", 0.0),
                            km = ia.optDouble("km", 0.0),
                            min = ia.optDouble("min", 0.0),
                            recogidaKm = ia.optDouble("recKm", 0.0),
                            recogidaMin = ia.optDouble("recMin", 0.0),
                            multiplicador = ia.optDouble("multi", 0.0).let { if (it > 1.0) it else o.multiplicador },
                            dinamica = o.dinamica
                        )
                        if (Parser.plausible(c, lineas + otra?.lineas.orEmpty(), moneda)) nueva = c
                    }
                } catch (_: Exception) { }
                synchronized(this) {
                    enCurso.remove(f)
                    revisadas[f] = nueva
                }
                nueva?.let(listo)
            }
        } catch (_: Exception) {
            synchronized(this) { enCurso.remove(f) }
        }
    }
}
