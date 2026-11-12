package com.karen.miruta

import android.content.Context
import android.location.Geocoder
import android.net.ConnectivityManager
import org.json.JSONArray
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.util.Locale

/**
 * Zonas bloqueadas (10.60–10.61): el conductor dibuja en un mapa las zonas a las que no quiere ir
 * y, si el origen o el destino de una oferta cae dentro de una, el pop-up sale en rojo.
 *
 * Las zonas se guardan en [Prefs.zonasTexto]: zonas separadas por «;», cada una con puntos
 * «lat,lon» separados por espacios y, opcional, su nombre delante con «|» («Centro|21.1,-86.8 …»).
 * El origen y el destino salen del texto de la oferta ([Parser.origen], [Parser.destino]) y se
 * convierten en coordenadas por internet ([ubicar]), con caché; sin internet ni caché no se bloquea nada.
 * Solo sale del teléfono el texto de la dirección: ni capturas ni datos de la oferta.
 */
object Zonas {
    class Punto(val lat: Double, val lon: Double)
    class Zona(val puntos: List<Punto>, val nombre: String = "")

    /** Cuál punto de la oferta se evalúa. */
    enum class Tipo { ORIGEN, DESTINO }

    /** Estado de la evaluación: «fuera de zona» no es lo mismo que «no se pudo comprobar». */
    enum class Estado { BLOQUEADA, LIBRE, SIN_DIRECCION, SIN_COORDENADAS, ERROR_RED }

    /** Resultado de evaluar un punto. Solo [Estado.BLOQUEADA] pone el pop-up en rojo. */
    data class Resultado(
        val tipo: Tipo,
        val estado: Estado,
        val direccion: String? = null,
        /** Nombre de la zona que lo contiene (solo con [Estado.BLOQUEADA]). */
        val zona: String = "",
        val lat: Double? = null,
        val lon: Double? = null
    ) {
        val bloqueada: Boolean get() = estado == Estado.BLOQUEADA
    }

    /** Respuesta del geocodificador para una dirección. */
    sealed class Geo {
        class Hallado(val lat: Double, val lon: Double) : Geo()
        object NoHallado : Geo()
        object ErrorRed : Geo()
    }

    /** Nombre con el que se muestra la zona [i] (0, 1…): el suyo o «Zona N». */
    fun nombre(zonas: List<Zona>, i: Int): String =
        zonas.getOrNull(i)?.nombre?.ifBlank { null } ?: "Zona ${i + 1}"

    /** Quita del nombre lo que rompe el formato guardado. */
    fun limpiarNombre(n: String): String = n.replace(Regex("[;|\\n\\r]"), " ").trim().take(30)

    fun leer(texto: String): List<Zona> =
        texto.split(";").mapNotNull { z0 ->
            val nombre = if (z0.contains("|")) z0.substringBefore("|").trim() else ""
            val z = z0.substringAfter("|")
            val pts = z.trim().split(" ").mapNotNull { p ->
                val d = p.split(",")
                val la = d.getOrNull(0)?.toDoubleOrNull()
                val lo = d.getOrNull(1)?.toDoubleOrNull()
                if (la != null && lo != null) Punto(la, lo) else null
            }
            if (pts.size >= 3) Zona(pts, nombre) else null
        }

    fun escribir(zonas: List<Zona>): String =
        zonas.joinToString(";") { z ->
            val n = limpiarNombre(z.nombre)
            (if (n.isEmpty()) "" else "$n|") +
                z.puntos.joinToString(" ") { String.format(Locale.US, "%.6f,%.6f", it.lat, it.lon) }
        }

    /** Punto dentro del polígono (rayo hacia la derecha). Un punto sobre el borde cuenta como dentro. */
    fun contiene(z: Zona, lat: Double, lon: Double): Boolean {
        if (enBorde(z, lat, lon)) return true
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

    /** true si el punto cae sobre un lado del polígono (con tolerancia de ~1 cm). */
    private fun enBorde(z: Zona, lat: Double, lon: Double): Boolean {
        val eps = 1e-7
        var j = z.puntos.size - 1
        for (i in z.puntos.indices) {
            val a = z.puntos[i]
            val b = z.puntos[j]
            j = i
            if (lat < minOf(a.lat, b.lat) - eps || lat > maxOf(a.lat, b.lat) + eps) continue
            if (lon < minOf(a.lon, b.lon) - eps || lon > maxOf(a.lon, b.lon) + eps) continue
            val cruz = (b.lat - a.lat) * (lon - a.lon) - (b.lon - a.lon) * (lat - a.lat)
            val largo = Math.hypot(b.lat - a.lat, b.lon - a.lon)
            if (largo == 0.0 || Math.abs(cruz) / largo <= eps) return true
        }
        return false
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
    // Evaluación de origen y destino
    // ------------------------------------------------------------------

    /**
     * Evalúa un punto de la oferta: dirección → coordenadas ([geo]) → ¿en qué zona cae?
     * Sin dirección, sin coordenadas o con error de red NO se bloquea (se prefiere no avisar
     * a avisar mal).
     */
    fun evaluar(tipo: Tipo, direccion: String?, zonas: List<Zona>, geo: (String) -> Geo): Resultado {
        if (direccion.isNullOrBlank()) return Resultado(tipo, Estado.SIN_DIRECCION)
        return when (val g = geo(direccion)) {
            is Geo.ErrorRed -> Resultado(tipo, Estado.ERROR_RED, direccion)
            is Geo.NoHallado -> Resultado(tipo, Estado.SIN_COORDENADAS, direccion)
            is Geo.Hallado -> {
                val n = cual(zonas, g.lat, g.lon)
                if (n == 0) Resultado(tipo, Estado.LIBRE, direccion, "", g.lat, g.lon)
                else Resultado(tipo, Estado.BLOQUEADA, direccion, nombre(zonas, n - 1), g.lat, g.lon)
            }
        }
    }

    /** Una línea para el diagnóstico: «Destino: Calle X → BLOQUEADA (21.1, -86.8) Centro». */
    fun informe(r: Resultado): String =
        (if (r.tipo == Tipo.ORIGEN) "Origen: " else "Destino: ") + (r.direccion ?: "(no se vio)") +
            " → " + r.estado.name +
            (if (r.lat != null && r.lon != null) String.format(Locale.US, " (%.5f, %.5f)", r.lat, r.lon) else "") +
            (if (r.zona.isNotEmpty()) " " + r.zona else "")

    /** Oferta con la marca de un punto, si está bloqueado (las que ya tenía se conservan). */
    fun marcar(o: Oferta, r: Resultado): Oferta = when {
        !r.bloqueada -> o
        r.tipo == Tipo.ORIGEN -> o.copy(zonaOrigen = r.zona)
        else -> o.copy(zonaDestino = r.zona)
    }

    /** Oferta con las marcas de los puntos bloqueados (las que ya tenía se conservan). */
    fun marcar(o: Oferta, origen: Resultado, destino: Resultado): Oferta = marcar(marcar(o, origen), destino)

    /** La corrección [nueva] del revisor conserva las marcas de zona de la oferta [anterior] del mismo pop-up. */
    fun conservar(anterior: Oferta, nueva: Oferta): Oferta =
        if (!anterior.bloqueada || nueva.bloqueada) nueva
        else nueva.copy(zonaOrigen = anterior.zonaOrigen, zonaDestino = anterior.zonaDestino)

    /**
     * Turno de la oferta que muestra cada pop-up (por app). Cada oferta nueva recibe un turno
     * nuevo; un resultado que llega tarde con un turno viejo se descarta. Se consulta también
     * desde los hilos de búsqueda (para no buscar direcciones de ofertas que ya se fueron).
     */
    class Turnos {
        private val ids = HashMap<String, Long>()
        private var ultimo = 0L
        @Synchronized fun nuevo(clave: String): Long { ultimo++; ids[clave] = ultimo; return ultimo }
        @Synchronized fun actual(clave: String): Long? = ids[clave]
        @Synchronized fun vigente(clave: String, turno: Long): Boolean = ids[clave] == turno
        @Synchronized fun quitar(clave: String) { ids.remove(clave) }
    }

    /**
     * Hilos para ubicar direcciones fuera del hilo principal: origen y destino se buscan a la vez
     * (10.65). Nominatim sigue con su límite de 1 consulta por segundo ([buscar]).
     */
    val hilo: java.util.concurrent.ExecutorService = java.util.concurrent.Executors.newFixedThreadPool(3)

    /** Hilo aparte para el buscador del teléfono, que puede colgarse: se le da un tiempo máximo. */
    private val telefono = java.util.concurrent.Executors.newCachedThreadPool()

    // ------------------------------------------------------------------
    // Dirección → coordenadas (Nominatim de OpenStreetMap, solo el texto de la dirección)
    // ------------------------------------------------------------------

    /** Entrada de caché: coordenadas, o null si Nominatim no halló la dirección (con su hora). */
    private class Entrada(val punto: DoubleArray?, val t: Long)

    private val cache = object : LinkedHashMap<String, Entrada>(64, 0.75f, true) {
        override fun removeEldestEntry(e: MutableMap.MutableEntry<String, Entrada>?) = size > 200
    }
    private var ultimaConsulta = 0L

    /** Una dirección que no se halló se vuelve a buscar hasta pasados 30 min. */
    private const val NO_HALLADO_MS = 30 * 60_000L

    /** Solo para pruebas. */
    internal fun limpiarCache() = synchronized(cache) { cache.clear(); cargada = true }

    // Lo hallado también se guarda en el teléfono (10.65): tus lugares de siempre salen al instante
    // aunque cierres la app. Solo coordenadas de direcciones de ofertas, nunca sale del teléfono.
    private const val ARCHIVO_CACHE = "zonas_direcciones.txt"
    private const val MAX_GUARDADAS = 500
    @Volatile private var cargada = false

    private fun cargarCache(ctx: Context) {
        if (cargada) return
        synchronized(cache) {
            if (cargada) return
            cargada = true
            try {
                val f = java.io.File(ctx.applicationContext.filesDir, ARCHIVO_CACHE)
                if (!f.exists()) return
                val lineas = f.readLines().takeLast(MAX_GUARDADAS)
                for (l in lineas) {
                    val p = l.split("|", limit = 3)
                    val la = p.getOrNull(0)?.toDoubleOrNull() ?: continue
                    val lo = p.getOrNull(1)?.toDoubleOrNull() ?: continue
                    val k = p.getOrNull(2)?.takeIf { it.isNotBlank() } ?: continue
                    cache[k] = Entrada(doubleArrayOf(la, lo), 0L)
                }
                // Se poda de vez en cuando para que el archivo no crezca sin fin.
                if (lineas.size >= MAX_GUARDADAS) f.writeText(lineas.joinToString("\n", postfix = "\n"))
            } catch (_: Exception) { }
        }
    }

    private fun guardarEnArchivo(ctx: Context, clave: String, lat: Double, lon: Double) {
        try {
            java.io.File(ctx.applicationContext.filesDir, ARCHIVO_CACHE)
                .appendText(String.format(Locale.US, "%.6f|%.6f|%s\n", lat, lon, clave.replace("\n", " ")))
        } catch (_: Exception) { }
    }

    private fun normalizar(direccion: String) = direccion.replace(Regex("\\s+"), " ").trim().lowercase(Locale.ROOT)

    private val tipoVia = Regex("""^(?:(?:calle|c\.|av\.?|avenida|privada|priv\.|calzada|blvd\.?|boulevard|andador|retorno|cerrada)\s+)+""", RegexOption.IGNORE_CASE)

    /**
     * Direcciones a probar, de la más precisa a la menos: completa; sin números sueltos ni
     * repetidos; solo la calle (antes de la primera coma, sin números); y la calle sin «Calle»,
     * «Privada», «Av.»… (OSM a veces la tiene sin el tipo). La búsqueda queda limitada al
     * recuadro de tus zonas, así una calle de otra ciudad no se confunde.
     */
    fun consultas(destino: String): List<String> {
        val base = destino.replace(Regex("\\s+"), " ").trim()
        fun sinNum(t: String) = t.split(",", " ").map { it.trim() }.filter { it.isNotEmpty() }
            .filterNot { it.length <= 4 && it.all { c -> c.isDigit() } }
            .distinct().joinToString(" ")
        val primero = base.substringBefore(",")
        val resto = if (base.contains(",")) base.substringAfter(",").trim() else ""
        // DiDi junta negocio y calle: «Ferretería Peryca - Avenida Francisco I. Madero, …». Cada
        // parte (de 2 palabras o más) se prueba con el resto de la dirección (colonia y ciudad).
        val partes = if (primero.contains(" - ")) primero.split(" - ").map { it.trim() }
            .filter { it.split(" ").size >= 2 }.reversed() else emptyList()
        val conResto = partes.map { if (resto.isEmpty()) it else "$it, $resto" }
        val calle = sinNum(primero)
        val sinTipo = calle.replace(tipoVia, "").trim()
        return (listOf(base) + conResto + listOf(sinNum(base), calle, sinTipo))
            // Nada que sea solo «Calle» o «Av.»: eso no ubica nada.
            .filter { it.length >= 5 && it.count { c -> c.isLetter() } >= 4 && "$it ".replace(tipoVia, "").isNotBlank() }
            .distinct().take(5)
    }

    fun hayRed(ctx: Context): Boolean = try {
        val cm = ctx.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        cm.activeNetwork != null
    } catch (_: Exception) { false }

    /**
     * Coordenadas de [direccion] con caché: lo hallado se recuerda; lo no hallado, 30 min; un
     * error de red no se recuerda (se puede reintentar). [buscar] hace la consulta real.
     */
    internal fun ubicarCon(
        direccion: String, buscar: (String) -> Geo, ahora: Long = System.currentTimeMillis(),
        recordarNoHallado: Boolean = true, alHallar: ((String, Double, Double) -> Unit)? = null
    ): Geo {
        val clave = normalizar(direccion)
        synchronized(cache) {
            cache[clave]?.let { e ->
                val p = e.punto
                if (p != null) return Geo.Hallado(p[0], p[1])
                if (ahora - e.t < NO_HALLADO_MS) return Geo.NoHallado
            }
        }
        for (q in consultas(direccion)) {
            when (val r = buscar(q)) {
                is Geo.Hallado -> {
                    synchronized(cache) { cache[clave] = Entrada(doubleArrayOf(r.lat, r.lon), ahora) }
                    alHallar?.invoke(clave, r.lat, r.lon)
                    return r
                }
                is Geo.ErrorRed -> return r
                is Geo.NoHallado -> {}
            }
        }
        if (recordarNoHallado) synchronized(cache) { cache[clave] = Entrada(null, ahora) }
        return Geo.NoHallado
    }

    /** Recuadro de tus zonas con 0.4° de margen: [latMín, lonMín, latMáx, lonMáx], o null sin zonas. */
    internal fun recuadro(zonas: List<Zona>): DoubleArray? {
        val pts = zonas.flatMap { it.puntos }
        if (pts.isEmpty()) return null
        val m = 0.4
        return doubleArrayOf(pts.minOf { it.lat } - m, pts.minOf { it.lon } - m, pts.maxOf { it.lat } + m, pts.maxOf { it.lon } + m)
    }

    /**
     * Coordenadas de [direccion]. Bloquea el hilo: llamar desde [hilo], nunca del principal.
     * Orden (10.65): caché (memoria y archivo) → buscador del teléfono (≈ 0.5 s, sin límite por
     * segundo) → Nominatim de respaldo. Las dos búsquedas se limitan al recuadro de tus zonas:
     * lo que cae fuera no puede estar en una zona. [sigue] = false corta la búsqueda (la oferta ya se fue).
     */
    fun ubicar(ctx: Context, direccion: String, zonas: List<Zona>, pais: String, sigue: () -> Boolean = { true }): Geo {
        cargarCache(ctx)
        val caja = recuadro(zonas)
        val guardar = { k: String, la: Double, lo: Double -> guardarEnArchivo(ctx, k, la, lo) }
        val delTelefono = ubicarCon(direccion, { q ->
            if (!sigue()) Geo.ErrorRed else buscarTelefono(ctx, q, caja, pais)
        }, recordarNoHallado = false, alHallar = guardar)
        if (delTelefono is Geo.Hallado || !sigue()) return delTelefono
        // bounded=1: solo resultados dentro del recuadro, así «Benito Juárez» no se va a la CDMX.
        val cajaTexto = if (caja == null) "" else String.format(
            Locale.US, "&bounded=1&viewbox=%.5f,%.5f,%.5f,%.5f", caja[1], caja[2], caja[3], caja[0]
        )
        return ubicarCon(direccion, { q ->
            if (!sigue() || !hayRed(ctx)) Geo.ErrorRed else buscar(q, cajaTexto, pais)
        }, alHallar = guardar)
    }

    /** Buscador de direcciones del teléfono (Android `Geocoder`, de Google Play en casi todos). Máx. 2.5 s. */
    @Suppress("DEPRECATION")
    private fun buscarTelefono(ctx: Context, q: String, caja: DoubleArray?, pais: String): Geo {
        if (!Geocoder.isPresent()) return Geo.ErrorRed
        val tarea = try {
            telefono.submit(java.util.concurrent.Callable {
                val g = Geocoder(ctx.applicationContext, Locale("es", if (pais.length == 2) pais.uppercase(Locale.ROOT) else "MX"))
                if (caja != null) g.getFromLocationName(q, 1, caja[0], caja[1], caja[2], caja[3])
                else g.getFromLocationName(q, 1)
            })
        } catch (_: Exception) { return Geo.ErrorRed }
        return try {
            val a = tarea.get(2500, java.util.concurrent.TimeUnit.MILLISECONDS)?.firstOrNull() ?: return Geo.NoHallado
            if (!a.hasLatitude() || !a.hasLongitude()) return Geo.NoHallado
            // Por si el teléfono ignora el recuadro: lo de fuera no cuenta.
            if (caja != null && (a.latitude < caja[0] || a.latitude > caja[2] || a.longitude < caja[1] || a.longitude > caja[3])) {
                return Geo.NoHallado
            }
            Geo.Hallado(a.latitude, a.longitude)
        } catch (_: java.util.concurrent.TimeoutException) {
            tarea.cancel(true); Geo.ErrorRed
        } catch (_: Exception) { Geo.ErrorRed }
    }

    private fun buscar(q: String, caja: String, pais: String): Geo {
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
                if (c.responseCode != 200) return Geo.ErrorRed
                val arr = JSONArray(c.inputStream.bufferedReader().readText())
                if (arr.length() == 0) return Geo.NoHallado
                val o = arr.getJSONObject(0)
                Geo.Hallado(o.getString("lat").toDouble(), o.getString("lon").toDouble())
            } finally { c.disconnect() }
        } catch (_: Exception) { Geo.ErrorRed }
    }
}
