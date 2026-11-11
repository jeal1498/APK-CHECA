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

    private fun firma(o: Oferta) = "${o.tarifa}|${o.km}|${o.min}"

    /** Líneas que se pueden enviar: cortas, con un número y con algo de la oferta. */
    fun filtrar(lineas: List<String>): List<String> = lineas.asSequence()
        .map { it.trim() }
        .filter { it.isNotEmpty() && it.length <= 60 && it.any(Char::isDigit) && util.containsMatchIn(it) &&
            !it.contains("total", ignoreCase = true) }
        .take(30).toList()

    /** La corrección ya conocida para esta lectura local, si la hubo. */
    @Synchronized
    fun corregida(o: Oferta): Oferta? = revisadas[firma(o)]

    /** Pide la revisión de [o] una sola vez; [listo] se llama (en otro hilo) solo si hay que corregir. */
    fun revisar(ctx: Context, app: String, lineas: List<String>, o: Oferta, moneda: Moneda, msPopup: Long, listo: (Oferta) -> Unit) {
        if (!Cuenta.conectada(ctx)) return
        val enviar = filtrar(lineas)
        if (enviar.isEmpty()) return
        val f = firma(o)
        synchronized(this) {
            if (revisadas.containsKey(f) || !enCurso.add(f)) return
        }
        val app0 = ctx.applicationContext
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
                        .put("msRevisionAnterior", msRevision)
                        .put("local", JSONObject()
                            .put("tarifa", o.tarifa).put("km", o.km).put("min", o.min)
                            .put("recKm", o.recogidaKm).put("recMin", o.recogidaMin).put("multi", o.multiplicador))
                    val respuesta = Cuenta.revisarOferta(app0, cuerpo.toString())
                    // El servidor respondió (aunque sea sin saldo): ya anotó el conteo del día anterior.
                    if (respuesta != null && conteoAnterior != null) anteriorRecibido(app0, conteoAnterior.getString("dia"))
                    val r = JSONObject(respuesta ?: "{}")
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
                        if (Parser.plausible(c, lineas, moneda)) nueva = c
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
