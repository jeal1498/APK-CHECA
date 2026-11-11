package com.karen.miruta

import android.content.Context
import android.net.ConnectivityManager
import org.json.JSONArray
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.util.Locale

/**
 * Zonas bloqueadas (10.60): el conductor dibuja en un mapa las zonas a las que no quiere ir
 * y, si el destino de una oferta cae dentro de una, el pop-up sale en rojo.
 *
 * Las zonas se guardan en [Prefs.zonasTexto]: zonas separadas por «;», cada una con puntos
 * «lat,lon» separados por espacios. El destino sale del texto de la oferta ([Parser.destino])
 * y se convierte en coordenadas por internet ([ubicar]); sin internet no se bloquea nada.
 */
object Zonas {
    class Punto(val lat: Double, val lon: Double)
    class Zona(val puntos: List<Punto>)

    fun leer(texto: String): List<Zona> =
        texto.split(";").mapNotNull { z ->
            val pts = z.trim().split(" ").mapNotNull { p ->
                val d = p.split(",")
                val la = d.getOrNull(0)?.toDoubleOrNull()
                val lo = d.getOrNull(1)?.toDoubleOrNull()
                if (la != null && lo != null) Punto(la, lo) else null
            }
            if (pts.size >= 3) Zona(pts) else null
        }

    fun escribir(zonas: List<Zona>): String =
        zonas.joinToString(";") { z ->
            z.puntos.joinToString(" ") { String.format(Locale.US, "%.6f,%.6f", it.lat, it.lon) }
        }

    /** Punto dentro del polígono (rayo hacia la derecha). */
    fun contiene(z: Zona, lat: Double, lon: Double): Boolean {
        var dentro = false
        var j = z.puntos.size - 1
        for (i in z.puntos.indices) {
            val a = z.puntos[i]
            val b = z.puntos[j]
            if ((a.lat > lat) != (b.lat > lat) &&
                lon < (b.lon - a.lon) * (lat - a.lat) / (b.lat - a.lat) + a.lon
            ) dentro = !dentro
            j = i
        }
        return dentro
    }

    /** Número (1, 2…) de la primera zona que contiene el punto; 0 si ninguna. */
    fun cual(zonas: List<Zona>, lat: Double, lon: Double): Int =
        zonas.indexOfFirst { contiene(it, lat, lon) } + 1

    /** Quita puntos del trazo del dedo para que la zona pese poco (máx. [max] puntos). */
    fun simplificar(puntos: List<Punto>, max: Int = 60): List<Punto> {
        if (puntos.size <= max) return puntos
        val paso = puntos.size.toDouble() / max
        return (0 until max).map { puntos[(it * paso).toInt()] }
    }

    // ------------------------------------------------------------------
    // Destino → coordenadas (Nominatim de OpenStreetMap, solo el texto de la dirección)
    // ------------------------------------------------------------------

    private val cache = object : LinkedHashMap<String, DoubleArray?>(64, 0.75f, true) {
        override fun removeEldestEntry(e: MutableMap.MutableEntry<String, DoubleArray?>?) = size > 100
    }
    private var ultimaConsulta = 0L

    /** Direcciones a probar: completa y, si no se halla, sin números sueltos ni repetidos. */
    fun consultas(destino: String): List<String> {
        val base = destino.replace(Regex("\\s+"), " ").trim()
        val sinNumeros = base.split(",", " ").map { it.trim() }.filter { it.isNotEmpty() }
            .filterNot { it.length <= 4 && it.all { c -> c.isDigit() } }
            .distinct().joinToString(" ")
        return listOf(base, sinNumeros).filter { it.isNotEmpty() }.distinct()
    }

    fun hayRed(ctx: Context): Boolean = try {
        val cm = ctx.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        cm.activeNetwork != null
    } catch (_: Exception) { false }

    /** Coordenadas (lat, lon) del destino, o null. Bloquea el hilo: llamar fuera del hilo principal. */
    fun ubicar(ctx: Context, destino: String, zonas: List<Zona>, pais: String): Pair<Double, Double>? {
        val clave = destino.lowercase(Locale.ROOT)
        synchronized(cache) { if (cache.containsKey(clave)) return cache[clave]?.let { it[0] to it[1] } }
        if (!hayRed(ctx)) return null
        // Un recuadro alrededor de tus zonas sesga la búsqueda hacia tu ciudad.
        val pts = zonas.flatMap { it.puntos }
        val margen = 0.4
        val caja = if (pts.isEmpty()) "" else String.format(
            Locale.US, "&viewbox=%.5f,%.5f,%.5f,%.5f",
            pts.minOf { it.lon } - margen, pts.maxOf { it.lat } + margen,
            pts.maxOf { it.lon } + margen, pts.minOf { it.lat } - margen
        )
        var res: Pair<Double, Double>? = null
        for (q in consultas(destino)) {
            res = buscar(q, caja, pais)
            if (res != null) break
        }
        // Solo se recuerda lo hallado: un fallo por red se puede reintentar.
        if (res != null) synchronized(cache) { cache[clave] = doubleArrayOf(res.first, res.second) }
        return res
    }

    private fun buscar(q: String, caja: String, pais: String): Pair<Double, Double>? {
        // Nominatim pide máximo 1 consulta por segundo.
        synchronized(this) {
            val espera = 1100 - (System.currentTimeMillis() - ultimaConsulta)
            if (espera > 0) try { Thread.sleep(espera) } catch (_: InterruptedException) {}
            ultimaConsulta = System.currentTimeMillis()
        }
        return try {
            val url = "https://nominatim.openstreetmap.org/search?format=json&limit=1" +
                "&q=" + URLEncoder.encode(q, "UTF-8") + caja +
                (if (pais.length == 2) "&countrycodes=" + pais.lowercase(Locale.ROOT) else "")
            val c = URL(url).openConnection() as HttpURLConnection
            c.connectTimeout = 4000
            c.readTimeout = 4000
            c.setRequestProperty("User-Agent", "Checa-Android/" + BuildConfig.VERSION_NAME)
            c.setRequestProperty("Accept-Language", "es")
            try {
                if (c.responseCode != 200) return null
                val arr = JSONArray(c.inputStream.bufferedReader().readText())
                if (arr.length() == 0) return null
                val o = arr.getJSONObject(0)
                o.getString("lat").toDouble() to o.getString("lon").toDouble()
            } finally { c.disconnect() }
        } catch (_: Exception) { null }
    }
}
