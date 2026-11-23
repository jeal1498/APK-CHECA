package com.karen.miruta

import java.security.MessageDigest
import java.util.Locale
import java.util.UUID

/**
 * E3 (10.81) — Núcleo de la trazabilidad de lecturas, en Kotlin puro (sin Android) para probarlo en la JVM.
 * No cambia el lector: solo describe lo que ya pasó (fotograma, resultado, motivo, tiempos) y lo encola.
 * Contrato con el servidor: supabase/functions/registrar-lectura/contrato.ts (v1) y auditoria/README.md.
 * El envío y el almacenamiento en el teléfono están en TrazaAndroid.kt. Encendido por defecto (Prefs.trazabilidad).
 */
object Traza {
    const val CONTRATO = 1
    /** Versión del lector que produjo el resultado (se sube a mano cuando cambie Parser/Tarjetas/Moneda). */
    const val VERSION_PARSER = "10.86"

    val RESULTADOS = setOf("oferta", "no_oferta", "duplicada", "abstencion", "varias", "descartada", "timeout", "error")
    val ETAPAS = setOf("captura", "ocr", "texto", "tarjetas", "parser", "dedupe", "popup", "revision", "envio")
    val ESTADOS = setOf("ok", "fallo", "omitida", "timeout")
    private val MOTIVO = Regex("^[a-z0-9_]{1,64}$")

    fun nuevoId(): String = UUID.randomUUID().toString()

    fun sha256(b: ByteArray): String =
        MessageDigest.getInstance("SHA-256").digest(b).joinToString("") { "%02x".format(it) }

    /** Huella de TODAS las líneas leídas (no se mandan): permite saber si dos eventos vieron lo mismo. */
    fun huellaLineas(lineas: List<String>): String = sha256(lineas.joinToString("\n").toByteArray(Charsets.UTF_8))

    fun appCorta(app: String?): String = when (app?.lowercase(Locale.ROOT)?.replace("-", "")) {
        "uber" -> "uber"
        "didi" -> "didi"
        else -> "otra"
    }

    data class Etapa(val etapa: String, val estado: String, val ms: Long? = null, val motivo: String? = null) {
        init {
            require(etapa in ETAPAS) { "etapa" }
            require(estado in ESTADOS) { "estado" }
            require(motivo == null || MOTIVO.matches(motivo)) { "motivo" }
        }
    }

    /** Un evento de lectura. Las comprobaciones de coherencia son las mismas que impone el servidor y la base. */
    data class Evento(
        val id: String = nuevoId(),
        val app: String,
        val versionApp: String,
        val lector: String,
        val fotogramaId: String?,
        val tsFotogramaMs: Long?,
        val tsEventoMs: Long,
        val resultado: String,
        val motivo: String? = null,
        val oferta: Oferta? = null,
        val nivel: String? = null,
        val moneda: String? = null,
        val tarjetas: Int? = null,
        val msLectura: Long? = null,
        val msPopup: Long? = null,
        val lineasSha256: String? = null,
        val lineasRedactadas: List<String> = emptyList(),
        val capturaSha256: String? = null,
        val telefono: String? = null,
        val android: String? = null,
        val orientacion: String? = null,
        val etapas: List<Etapa> = emptyList(),
    ) {
        init {
            require(app in setOf("uber", "didi", "otra")) { "app" }
            require(lector in setOf("texto", "ocr", "ia")) { "lector" }
            require(resultado in RESULTADOS) { "resultado" }
            require(motivo == null || MOTIVO.matches(motivo)) { "motivo" }
            require(lector != "ocr" || fotogramaId != null) { "ocr_sin_fotograma" }
            if (resultado == "oferta") {
                require(oferta != null && oferta.tarifa > 0 && oferta.km > 0 && oferta.min > 0 && nivel != null) { "oferta_incompleta" }
            } else {
                require(nivel == null) { "semaforo_sin_oferta" }
                require(oferta == null) { "datos_sin_oferta" }  // «varias», «abstencion»…: nunca datos de otra tarjeta
            }
            require(nivel == null || nivel in setOf("verde", "amarillo", "rojo")) { "nivel" }
            require(capturaSha256 == null || fotogramaId != null) { "captura_sin_fotograma" }
            require(lineasRedactadas.size <= 30 && lineasRedactadas.all { it.length <= 60 }) { "lineas" }
            require(etapas.size <= 32) { "etapas" }
        }

        fun aJson(): String {
            val j = Json()
            j.s("id", id).n("contrato", CONTRATO).s("app", app).s("version_app", versionApp)
                .s("version_parser", VERSION_PARSER).s("lector", lector).s("fotograma_id", fotogramaId)
                .s("captura_sha256", capturaSha256).s("captura_vinculo", if (capturaSha256 != null) "exacto" else "sin_captura")
                .s("ts_fotograma", tsFotogramaMs?.let(::iso)).s("ts_evento", iso(tsEventoMs))
                .s("resultado", resultado).s("motivo", motivo)
            oferta?.let {
                j.d("tarifa", it.tarifa, 2).d("km", it.km, 2).d("min", it.min, 1)
                if (it.recogidaKm > 0) j.d("rec_km", it.recogidaKm, 2)
                if (it.recogidaMin > 0) j.d("rec_min", it.recogidaMin, 1)
                if (it.multiplicador >= 1.0 && it.multiplicador < 20.0) j.d("multi", it.multiplicador, 2)
            }
            j.s("moneda", moneda).s("nivel", nivel).n("tarjetas", tarjetas?.toLong())
                .n("ms_lectura", msLectura?.coerceIn(0, 600_000)).n("ms_popup", msPopup?.coerceIn(0, 600_000))
                .s("lineas_sha256", lineasSha256).lista("lineas_redactadas", lineasRedactadas)
                .s("telefono", telefono?.take(64)).s("android", android?.take(8)).s("orientacion", orientacion)
            j.objetos("etapas", etapas.map { e ->
                Json().s("etapa", e.etapa).s("estado", e.estado).n("ms", e.ms?.coerceIn(0, 600_000)).s("motivo", e.motivo)
            })
            return j.cerrar()
        }
    }

    fun iso(ms: Long): String {
        val f = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US)
        f.timeZone = java.util.TimeZone.getTimeZone("UTC")
        return f.format(java.util.Date(ms))
    }

    /** Lote del contrato v1 (1 a 50 eventos ya serializados). */
    fun lote(eventos: List<String>): String {
        require(eventos.size in 1..50)
        return "{\"contrato\":$CONTRATO,\"eventos\":[" + eventos.joinToString(",") + "]}"
    }

    /** JSON mínimo y determinista (la JVM de pruebas no tiene org.json). Omite los nulos. */
    class Json {
        private val sb = StringBuilder("{")
        private fun clave(k: String) { if (sb.length > 1) sb.append(','); sb.append(cadena(k)).append(':') }
        fun s(k: String, v: String?) = apply { if (v != null) { clave(k); sb.append(cadena(v)) } }
        fun n(k: String, v: Long?) = apply { if (v != null) { clave(k); sb.append(v) } }
        fun n(k: String, v: Int) = apply { clave(k); sb.append(v) }
        fun d(k: String, v: Double, dec: Int) = apply {
            if (v.isFinite()) { clave(k); sb.append(String.format(Locale.US, "%.${dec}f", v).trimEnd('0').trimEnd('.')) }
        }
        fun lista(k: String, v: List<String>) = apply { clave(k); sb.append(v.joinToString(",", "[", "]") { cadena(it) }) }
        fun objetos(k: String, v: List<Json>) = apply { clave(k); sb.append(v.joinToString(",", "[", "]") { it.cerrar() }) }
        fun cerrar(): String = sb.toString() + "}"

        companion object {
            fun cadena(t: String): String {
                val b = StringBuilder("\"")
                for (c in t) when {
                    c == '"' -> b.append("\\\"")
                    c == '\\' -> b.append("\\\\")
                    c == '\n' -> b.append("\\n")
                    c == '\r' -> b.append("\\r")
                    c == '\t' -> b.append("\\t")
                    c < ' ' || c == ' ' || c == ' ' -> b.append(String.format(Locale.US, "\\u%04x", c.code))
                    else -> b.append(c)
                }
                return b.append('"').toString()
            }
        }
    }
}

/**
 * Redacción antes de guardar o mandar, por LISTA BLANCA: de cada línea solo sobreviven las piezas con forma de
 * dato de la oferta (montos, número + min/km/m/h, multiplicador, «/km», y unas pocas palabras fijas). Todo lo
 * demás se quita, también dentro de la línea: en DiDi la dirección viene pegada al tramo
 * («(4 min 1.4 km) Calle 46 612,» → «( 4 min 1.4 km )»). Los números sueltos (número de casa) tampoco pasan.
 * La misma gramática la exige el servidor (contrato.ts, LINEA_REDACTADA): si cambia aquí, cambia allá.
 */
object Redaccion {
    const val PALABRAS = "min|km|m|h|est|viaje|a|de|por|inicio|incluido|adicionales|adicional|aceptar|total|tarifa"
    /** Piezas permitidas, en orden de prioridad. */
    // Límites de palabra explícitos con \p{L}: «\b» de Java no cuenta ñ ni acentos como letra («Cabaña» dejaba «a»).
    private const val FIN = """(?![\p{L}\d])"""
    const val PIEZA = """[+-]?[$€£] ?\d[\d.,]{0,11}|/ ?(?:km|h)""" + FIN + """|(?<![\p{L}\d.,])\d{1,5}(?:[.,]\d{1,3})? ?(?:min|km|m|h|x)""" + FIN +
        """|[()⚡·:+]|(?<![\p{L}\d])(?:""" + PALABRAS + """)""" + FIN
    private val pieza = Regex(PIEZA, RegexOption.IGNORE_CASE)
    private val dato = Regex("""[$€£] ?\d|\d ?(?:min|km|m|h|x)""" + FIN, RegexOption.IGNORE_CASE)

    /** La línea reducida a sus piezas permitidas, o null si no queda ningún dato. */
    fun redactarLinea(l: String): String? {
        val sb = StringBuilder()
        for (m in pieza.findAll(l.trim().take(200))) {
            val p = m.value.trim()
            // Kotlin iguala «ı» (sin punto) con «i» al ignorar mayúsculas y el servidor no: fuera letras no ASCII.
            if (p.any { it.isLetter() && it !in 'a'..'z' && it !in 'A'..'Z' }) continue
            if (sb.length + p.length + 1 > 60) break   // nunca se corta una pieza a la mitad
            if (sb.isNotEmpty()) sb.append(' ')
            sb.append(p)
        }
        val t = sb.toString()
        return if (t.isNotEmpty() && dato.containsMatchIn(t)) t else null
    }

    fun redactar(lineas: List<String>): List<String> = lineas.asSequence().mapNotNull(::redactarLinea).take(30).toList()
}

/** Espera creciente con un poco de azar (sin martillar al servidor). [azar] en 0..1 (inyectable en pruebas). */
object Reintento {
    const val BASE_MS = 30_000L
    const val MAX_MS = 30 * 60_000L

    fun espera(intento: Int, azar: Double): Long {
        val e = (BASE_MS shl intento.coerceIn(0, 10)).coerceAtMost(MAX_MS)
        return (e * (0.5 + 0.5 * azar.coerceIn(0.0, 1.0))).toLong()
    }
}

/** Dónde vive la cola (archivo en el teléfono, memoria en pruebas). */
interface AlmacenTraza {
    fun leer(): String?
    fun escribir(texto: String)
}

/**
 * Cola acotada y persistente de eventos ya serializados. Si se llena, descarta los MÁS VIEJOS y lo cuenta
 * (nunca bloquea la lectura). Formato: una línea por evento «id<TAB>json».
 */
class ColaTraza(private val almacen: AlmacenTraza, private val maxEventos: Int = 500, private val maxBytes: Int = 512 * 1024) {
    private val eventos = LinkedHashMap<String, String>()
    var descartados = 0L
        private set

    init {
        try {
            almacen.leer()?.lineSequence()?.forEach { l ->
                val i = l.indexOf('\t')
                if (i == 36 && l.length > 38 && l.endsWith("}")) eventos[l.substring(0, i)] = l.substring(i + 1)
            }
        } catch (_: Exception) { eventos.clear() }  // archivo dañado: se empieza de cero, la app sigue
        recortar()
    }

    @Synchronized fun tamano() = eventos.size

    @Synchronized fun agregar(id: String, json: String): Boolean {
        if (id.length != 36 || json.length > maxBytes / 4 || json.contains('\n')) { descartados++; return false }
        eventos[id] = json
        recortar()
        guardar()
        return eventos.containsKey(id)
    }

    /** Los más viejos primero, hasta [n] eventos y ~48 KB (el servidor acepta 64 KB). */
    @Synchronized fun lote(n: Int = 50): List<Pair<String, String>> {
        val r = ArrayList<Pair<String, String>>()
        var bytes = 0
        for ((k, v) in eventos) {
            if (r.size >= n || bytes + v.length > 48 * 1024) break
            r.add(k to v); bytes += v.length + 1
        }
        return r
    }

    @Synchronized fun quitar(ids: Collection<String>) {
        if (ids.isEmpty()) return
        ids.forEach { eventos.remove(it) }
        guardar()
    }

    private fun recortar() {
        var total = eventos.values.sumOf { it.length + 38 }
        val it = eventos.entries.iterator()
        while (it.hasNext() && (eventos.size > maxEventos || total > maxBytes)) {
            total -= it.next().value.length + 38
            it.remove(); descartados++
        }
    }

    private fun guardar() {
        try { almacen.escribir(eventos.entries.joinToString("") { "${it.key}\t${it.value}\n" }) } catch (_: Exception) { }
    }
}

/** Qué hacer con un lote según la respuesta del servidor (ver la tabla de auditoria/README.md). */
object RespuestaTraza {
    sealed class Accion {
        /** Quitar estos ids de la cola (guardado, repetido, conflicto o inválido: definitivos). */
        data class Quitar(val ids: Set<String>) : Accion()
        object Reintentar : Accion()
        object Esperar : Accion()       // sin sesión o función apagada: conservar sin reintentar en bucle
        data class Descartar(val ids: Set<String>) : Accion()  // forma del lote inválida (no se arregla reintentando)
    }

    private val idRx = Regex("\"id\"\\s*:\\s*\"([0-9a-f-]{36})\"")

    fun decidir(codigo: Int?, cuerpo: String?, lote: Collection<String>): Accion = when {
        codigo == null -> Accion.Reintentar                         // sin red
        codigo == 200 -> {
            val vistos = idRx.findAll(cuerpo.orEmpty()).map { it.groupValues[1] }.toSet()
            if (cuerpo?.contains("\"ok\":true") == true) Accion.Quitar(vistos intersect lote.toSet()) else Accion.Reintentar
        }
        codigo == 400 || codigo == 413 -> Accion.Descartar(lote.toSet())
        codigo == 401 || codigo == 403 -> Accion.Esperar
        codigo == 503 && cuerpo?.contains("\"reintentar\":false") == true -> Accion.Esperar
        else -> Accion.Reintentar
    }
}
