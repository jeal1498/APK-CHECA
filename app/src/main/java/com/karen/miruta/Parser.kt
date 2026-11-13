package com.karen.miruta

data class Oferta(
    val tarifa: Double,
    val km: Double,
    val min: Double,
    val recogidaKm: Double = 0.0,
    val recogidaMin: Double = 0.0,
    val multiplicador: Double = 0.0,
    /** Dinámica de Uber en pesos ("+$62.00 incluido"), ya incluida en la tarifa. */
    val dinamica: Double = 0.0,
    /** Nombre de la zona bloqueada donde cae el origen (recogida); vacío si no cae en ninguna. */
    val zonaOrigen: String = "",
    /** Nombre de la zona bloqueada donde cae el destino; vacío si no cae en ninguna. */
    val zonaDestino: String = ""
) {
    /** true si el origen o el destino cayeron en una zona bloqueada. */
    val bloqueada: Boolean get() = zonaOrigen.isNotEmpty() || zonaDestino.isNotEmpty()

    /** La misma oferta sin las marcas de zona (para comparar con la lectura original). */
    fun sinZonas(): Oferta = if (bloqueada) copy(zonaOrigen = "", zonaDestino = "") else this

    /** Motivo del bloqueo por zona, corto para leerlo manejando; null si no hay. */
    fun motivoZona(): String? = when {
        zonaOrigen.isNotEmpty() && zonaDestino.isNotEmpty() ->
            if (zonaOrigen == zonaDestino) "Origen y destino bloqueados · $zonaOrigen"
            else "Origen y destino bloqueados · $zonaOrigen / $zonaDestino"
        zonaOrigen.isNotEmpty() -> "Origen en zona bloqueada · $zonaOrigen"
        zonaDestino.isNotEmpty() -> "Destino en zona bloqueada · $zonaDestino"
        else -> null
    }

    val porKm: Double get() = if (km > 0) tarifa / km else 0.0
    val porHora: Double get() = if (min > 0) tarifa / min * 60.0 else 0.0
    val viajeKm: Double get() = km - recogidaKm
    val viajeMin: Double get() = min - recogidaMin

    /** Costo de gasolina de todo el recorrido (recogida + viaje). */
    fun gasolina(p: Prefs): Double =
        if (p.kmPorLitro > 0) km / p.kmPorLitro * p.precioLitro else 0.0

    /** Desgaste del auto en todo el recorrido (recogida + viaje). */
    fun desgaste(p: Prefs): Double = km * p.desgastePorKm

    /** Lo que te queda: tarifa menos gasolina y desgaste. */
    fun neta(p: Prefs): Double = tarifa - gasolina(p) - desgaste(p)

    /** Lo que se compara con la tarifa mínima: la neta (después de gasolina y desgaste) o la tarifa de la app. */
    fun baseTarifaMinima(p: Prefs): Double = if (p.tarMinNeta) neta(p) else tarifa

    /** Si la oferta no pasa los filtros de recogida, duración o tarifa mínima, dice por qué. */
    fun motivoFiltro(p: Prefs): String? {
        val f1 = { v: Double -> String.format(java.util.Locale.US, "%.1f", v) }
        val f0 = { v: Double -> String.format(java.util.Locale.US, "%.0f", v) }
        return when {
            bloqueada -> motivoZona()
            p.filtroRecMax && recogidaKm > 0 && recogidaKm > p.recogidaMaxKm ->
                "Recogida lejana: ${f1(recogidaKm)} km (máx ${f1(p.recogidaMaxKm.toDouble())})"
            p.filtroDurMax && viajeMin > p.duracionMax ->
                "Viaje largo: ${viajeMin.toInt()} min (máx ${p.duracionMax})"
            p.filtroTarMin && p.tarifaMinima > 0 && baseTarifaMinima(p) < p.tarifaMinima ->
                "Paga poco: ${f0(baseTarifaMinima(p))} (mín ${f0(p.tarifaMinima.toDouble())})"
            else -> null
        }
    }

    /** 2 = conviene, 1 = regular, 0 = no conviene. */
    fun nivel(prefs: Prefs): Int {
        if (motivoFiltro(prefs) != null) return ROJO
        // Solo cuentan los filtros de $/km y $/h que estén encendidos.
        val activos = listOfNotNull(
            if (prefs.filtroKm) porKm >= prefs.minPorKm else null,
            if (prefs.filtroHora) porHora >= prefs.minPorHora else null
        )
        val cumplen = activos.count { it }
        return when {
            cumplen == activos.size -> VERDE
            cumplen > 0 -> AMARILLO
            else -> ROJO
        }
    }

    companion object {
        const val VERDE = 2
        const val AMARILLO = 1
        const val ROJO = 0
    }
}

/**
 * Lee la oferta a partir de los textos de pantalla (en orden de aparición).
 *
 * Formatos vistos:
 *  DiDi:  "$151.34" · "5min (1.7km)" · "37min (13.2km)" · "2min (720m)"
 *         botones de contraoferta "$163.34" (van DESPUÉS de la tarifa)
 *         "$9.00 adicionales", "Aceptar $151.34"
 *  Uber:  "$48.73" · "$10.15/km est." · "A 5 min (1.6 km)" · "Viaje: 8 min (3.2 km)"
 *         "+$9.75 por inicio de viaje prioritario" · "+$62.00 incluido" (dinámica, ya va en la tarifa)
 *
 * Tarifa = primer texto que sea SOLO un monto. Km y minutos = suma de
 * los tramos "N min (X km)" (ir por el pasajero + viaje).
 */
object Parser {
    /** Regex de montos para una moneda. Con pesos mexicanos son las de siempre. */
    class Reglas(val moneda: Moneda) {
        private val legado = moneda.codigo == "MXN" && moneda.simbolo == "$" && !moneda.despues &&
            moneda.miles == "," && moneda.decimal == "."
        private val num: String
        private val pre: String
        private val sim: String
        val montoSolo: Regex
        val monto: Regex
        val incluido: Regex
        /** Solo el monto, sin nada más (la tarifa grande, para ponerla primero). */
        val montoGrande: Regex
        /** DiDi: «Aceptar $94.62» (el botón trae la tarifa). */
        val aceptarMonto: Regex
        /** DiDi: «$94.62 1.7x» (la tarifa con su multiplicador). */
        val montoMulti: Regex
        /** Línea del pop-up propio («X $155/h | $9.9/km») que el OCR alcanzó a leer. */
        val lineaPopup: Regex

        init {
            val d = sep(moneda.decimal)
            val alt = """\d+(?:$d\d{1,2})?"""
            num = if (moneda.miles.isEmpty()) "($alt)"
                else """(\d{1,3}(?:${sep(moneda.miles)}\d{3})+(?:$d\d{1,2})?|$alt)"""
            sim = moneda.simbolos().joinToString("|") { Regex.escape(it) }
            pre = if (legado) """(?:\$|MXN\s?)""" else """(?:${Regex.escape(moneda.codigo)}\s?)?(?:$sim)"""
            val cola = """\s*(?:[⚡x×\d.\s]*)$"""
            if (legado || !moneda.despues) {
                val preSolo = if (legado) """(?:(?:MXN?\s?)?\$|MXN\s?)""" else pre
                montoSolo = Regex("""^\s*$preSolo\s?$num$cola""")
                monto = Regex("""(?<!\+)$pre\s?$num(?!\s?/)""")
                incluido = Regex("""\+?\s?$pre\s?$num\s*incluid""", RegexOption.IGNORE_CASE)
                montoGrande = if (legado) Regex("""^(?:\$|MXN\s?)\s?[\d,]+(?:\.\d{1,2})?$""")
                    else Regex("""^\s*$pre\s?$num\s*$""")
            } else {
                // «12,50 €»; también se acepta «€12,50».
                montoSolo = Regex("""^\s*(?:$num\s?(?:$sim)|$pre\s?$num)$cola""")
                monto = Regex("""(?<![+\d.,])$num\s?(?:$sim)(?!\s?/)|(?<!\+)$pre\s?$num(?!\s?/)""")
                incluido = Regex("""\+?\s?(?:$num\s?(?:$sim)|$pre\s?$num)\s*incluid""", RegexOption.IGNORE_CASE)
                montoGrande = Regex("""^\s*(?:$num\s?(?:$sim)|$pre\s?$num)\s*$""")
            }
            aceptarMonto = Regex("""^\s*aceptar\s*$pre\s?$num""", RegexOption.IGNORE_CASE)
            montoMulti = Regex("""^\s*$pre\s?$num\s+\S?\s?[1-4](?:[.,]\d{1,2})?\s?[x×X]\s*$""")
            lineaPopup = Regex("""(?:$sim)\s?\d+(?:[.,]\d)?\s?/\s?h""", RegexOption.IGNORE_CASE)
        }

        /** El número del primer grupo que se llenó, ya como Double. */
        fun valor(m: MatchResult): Double? {
            val s = m.groupValues.drop(1).firstOrNull { it.isNotEmpty() } ?: return null
            val sinMiles = if (moneda.miles.isEmpty()) s else s.replace(moneda.miles, "")
            return sinMiles.replace(moneda.decimal, ".").toDoubleOrNull()
        }

        private fun sep(c: String) = if (c == ".") "\\." else c
    }

    @Volatile private var reglas = Reglas(Moneda.MXN)
    private fun reglasDe(m: Moneda): Reglas {
        val r = reglas
        if (r.moneda == m) return r
        return Reglas(m).also { reglas = it }
    }

    /** true si el texto es solo un monto (la tarifa grande del OCR). */
    fun esMontoSolo(t: String, m: Moneda = Moneda.MXN): Boolean = reglasDe(m).montoGrande.matches(t)

    private val noTarifa = Regex("""(adicional|/km|/h|aceptar|considerar|rechazar|por inicio|propina|ganancia|hoy|semana|saldo|quedan|gasolina|netos|meta\s)""", RegexOption.IGNORE_CASE)

    // "5min (1.7km)", "A 5 min (1.6 km)", "Viaje: 8 min (3.2 km)", "2min (720m)", "1 h 5 min (40 km)"
    // Uber nuevo (nov 2026): "8 min • 2.2 km", "1 h 20 min • 32.9 km" (sin paréntesis, con viñeta).
    private val tramo = Regex(
        """(?:(\d+)\s?h(?:rs?|oras?)?\s*)?(\d+)\s?min\s*(?:\(\s*(\d+(?:[.,]\d+)?)\s?(km|m)\s*\)|[^\d(\s]{0,3}\s*(\d+(?:[.,]\d+)?)\s?(km|m)\b)""",
        RegexOption.IGNORE_CASE
    )

    // "$6.29/km est." (Uber nuevo): precio por km del resumen.
    private val porKmTexto = Regex("""(\d+(?:[.,]\d{1,2})?)\s?/\s?km""", RegexOption.IGNORE_CASE)

    // "⚡1.4x", "1.2x", "1,5 x" (tarifa dinámica). No confunde calificaciones como "4.98".
    private val multi = Regex("""(?<![.,$])([1-4](?:[.,]\d{1,2})?)\s?[x×X](?![a-zA-Z0-9])""")

    // Uber: "+$62.00 incluido" = dinámica ya sumada en la tarifa (solo se muestra).
    private fun buscarDinamica(textos: List<String>, r: Reglas): Double {
        for (t in textos) {
            val m = r.incluido.find(t) ?: continue
            val v = r.valor(m) ?: continue
            if (v > 0) return v
        }
        return 0.0
    }

    private fun buscarMultiplicador(textos: List<String>): Double {
        val rango = Regex("""\d\s?[xX×]\s?[-–]""")
        for (t in textos) {
            // "1.8X-2.4X" son zonas del mapa de calor, no la tarifa de la oferta.
            if (rango.containsMatchIn(t)) continue
            val m = multi.find(t) ?: continue
            val v = m.groupValues[1].replace(",", ".").toDoubleOrNull() ?: continue
            if (v > 1.0 && v <= 4.0) return v
        }
        return 0.0
    }

    // Mapa de calor de DiDi («1.1X-1.4X»): con dos o más rangos y un solo tramo es la pantalla de inicio.
    private val rango = Regex("""\d\s?[xX×]\s?[-–]""")
    // «El pasajero canceló»: aviso, no oferta.
    private val cancelo = Regex("""cancel[oó]\b""", RegexOption.IGNORE_CASE)
    // Uber «$9.65/km est.» en su propia línea: km totales ≈ tarifa ÷ $/km.
    private val kmEstimado = Regex("""(\d{1,2}[.,]\d{2})\s?/\s?km""", RegexOption.IGNORE_CASE)

    private val noDireccion = Regex(
        """(\$|aceptar|considerar|rechazar|adicional|por inicio|incluid|calificaci|viajes|perfil|verificad|exclusivo|priority|/km|/h|me interesa|contraofert|ofrecer|ignorar|omitir|cancelar|ver m[aá]s|punto\(s\)|parada|pasajer|efectivo|tarjeta)""",
        RegexOption.IGNORE_CASE
    )

    /** Índices de los tramos «N min (X km)» de la oferta, sin resúmenes («total», «/km»). */
    private fun indicesTramos(textos: List<String>): List<Int> =
        textos.indices.filter { i ->
            val t = textos[i]
            !t.contains("total", ignoreCase = true) && !porKmTexto.containsMatchIn(t) &&
                tramo.containsMatchIn(corregirMinutos(t))
        }

    /** Lo que sigue al tramo en su misma línea (DiDi: «(6 min 1.3 km) Ferretería…, Benito Juárez»). */
    private fun restoDelTramo(t: String): String {
        val c = corregirMinutos(t)
        val m = tramo.find(c) ?: return ""
        return c.substring(m.range.last + 1).trimStart(')', ' ', '·', '•', '-', ',', ':', '\u00A0').trim()
    }

    /**
     * La dirección del tramo [i]: lo que va en su misma línea (si hay) y 1–2 líneas debajo
     * (la dirección larga se parte en dos); se corta en el siguiente tramo, botón o dato.
     */
    private fun direccionTras(textos: List<String>, i: Int): String? {
        val partes = ArrayList<String>()
        val misma = restoDelTramo(textos[i])
        if (misma.length >= 4 && misma.count { it.isLetter() } >= 3 && !noDireccion.containsMatchIn(misma)) partes.add(misma)
        val max = if (partes.isEmpty()) 2 else 3
        for (t in textos.drop(i + 1)) {
            val s = t.trim()
            if (partes.size >= max || s.length < 4) break
            if (s.count { it.isLetter() } < 3 || noDireccion.containsMatchIn(s) || tramo.containsMatchIn(corregirMinutos(s))) break
            partes.add(s)
        }
        return partes.joinToString(" ").ifBlank { null }
    }

    /**
     * Dirección del origen (donde se recoge al pasajero): las 1–2 líneas debajo del PRIMER tramo.
     * Uber: «A 3 min (1.1 km)» / «6 min · 2.3 km» y debajo la calle; DiDi: «3min (835m)» y debajo.
     * Solo con dos tramos a la vista (recogida y viaje): con uno solo no se sabe cuál es (null).
     */
    fun origen(textos: List<String>): String? {
        val t = indicesTramos(textos)
        if (t.size < 2) return null
        return direccionTras(textos, t.first())
    }

    /**
     * Dirección del destino: las 1–2 líneas que siguen al ÚLTIMO tramo «N min (X km)»
     * (Uber: «Viaje: 15 min (6.9 km)» y debajo la dirección; DiDi: el segundo tramo y debajo la dirección).
     * Solo con dos tramos a la vista; null si no se ve (entonces no se bloquea nada).
     */
    fun destino(textos: List<String>): String? {
        val t = indicesTramos(textos)
        if (t.size < 2) return null
        return direccionTras(textos, t.last())
    }

    fun analizar(textos: List<String>, moneda: Moneda = Moneda.MXN): Oferta? {
        val r = reglasDe(moneda)
        var tarifa = buscarTarifa(textos, r) ?: return null
        if (textos.any { cancelo.containsMatchIn(it) }) return null

        // Tramos en orden: (km, min, ¿es resumen?).
        val lista = ArrayList<Triple<Double, Double, Boolean>>()
        var resPorKm = 0.0
        val vistos = HashSet<String>()
        for (t in textos) {
            // "42 min (14.9 km) total" es un resumen (nuestro pop-up u otra app): no sumar.
            if (t.contains("total", ignoreCase = true)) continue
            // Uber nuevo: "50 min • 16.4 km • $6.29/km est." es el resumen del viaje completo.
            val porKm = porKmTexto.find(t)?.groupValues?.get(1)?.replace(",", ".")?.toDoubleOrNull()
            for (m in tramo.findAll(corregirMinutos(t))) {
                // Evita contar dos veces el mismo tramo si un contenedor repite el texto.
                if (!vistos.add(m.value.replace(" ", "").lowercase())) continue
                val h = m.groupValues[1].toDoubleOrNull() ?: 0.0
                val mi = m.groupValues[2].toDoubleOrNull() ?: 0.0
                val numero = m.groupValues[3].ifEmpty { m.groupValues[5] }
                val unidad = m.groupValues[4].ifEmpty { m.groupValues[6] }
                val d = numero.replace(",", ".").toDoubleOrNull() ?: 0.0
                // Metros reales siempre son enteros ≥ 30 ("720m"). "2.4m" o "1m" son km mal leídos.
                val esMetros = unidad.equals("m", ignoreCase = true) &&
                    !numero.contains('.') && !numero.contains(',') && d >= 30
                val tramoKm = if (esMetros) d / 1000.0 else d
                val tramoMin = h * 60.0 + mi
                val resumen = porKm != null
                if (resumen && porKm != null) resPorKm = porKm
                lista.add(Triple(tramoKm, tramoMin, resumen))
            }
        }
        // Sin «/km» a la vista: un tramo que vale la suma de los demás también es el resumen.
        if (lista.count { !it.third } >= 3) {
            val reales = lista.filter { !it.third }
            val sk = reales.sumOf { it.first }
            val sm = reales.sumOf { it.second }
            val i = lista.indexOfFirst { !it.third &&
                Math.abs(it.first - (sk - it.first)) <= 0.2 && Math.abs(it.second - (sm - it.second)) <= 2.0 }
            if (i >= 0) lista[i] = lista[i].copy(third = true)
        }
        val resumen = lista.firstOrNull { it.third }
        val tramosReales = lista.filter { !it.third }.ifEmpty { listOfNotNull(resumen) }
        val tramos = tramosReales.size
        var km = tramosReales.sumOf { it.first }
        var min = tramosReales.sumOf { it.second }
        var recKm = tramosReales.firstOrNull()?.first ?: 0.0
        var recMin = tramosReales.firstOrNull()?.second ?: 0.0
        // Si solo se ve un tramo pero hay resumen, el resumen es el total.
        if (resumen != null && tramos < 2) { km = resumen.first; min = resumen.second }
        // Tarifa ≈ $/km del resumen × km del resumen: así no se toma el monto de otra tarjeta (radar).
        if (resumen != null && resPorKm > 0) {
            val esperada = resPorKm * resumen.first
            val cerca = textos.mapNotNull { t -> r.montoSolo.find(t)?.let { r.valor(it) } }
                .filter { Math.abs(it - esperada) <= esperada * 0.06 + 0.5 }
                .minByOrNull { Math.abs(it - esperada) }
            if (cerca != null) tarifa = cerca
        }

        // Una oferta siempre trae 2 tramos (recogida + viaje); con 1 aceptamos también.
        if (tramos == 0) return null
        if (esNavegacion(textos, moneda)) return null
        if (tramos < 2 && textos.count { rango.containsMatchIn(it) } >= 2 &&
            textos.none { r.aceptarMonto.containsMatchIn(it) }) return null
        // Uber: si los km no dan el «$/km est.», el OCR perdió un punto («70 km» = 7.0): manda el $/km.
        if (resumen == null && tramos >= 2) {
            val linea = textos.firstOrNull { t -> kmEstimado.containsMatchIn(t) && !tramo.containsMatchIn(corregirMinutos(t)) }
            val p = linea?.let { kmEstimado.find(it)?.groupValues?.get(1)?.replace(",", ".")?.toDoubleOrNull() }
            if (p != null && p >= 2 && p <= 80) {
                val est = tarifa / p
                if (Math.abs(km - est) > Math.max(0.3, est * 0.08) && recKm < est) km = est
            }
        }
        // Límites en pesos mexicanos; en otra moneda se convierten (aproximado) con más holgura.
        val f = moneda.factor
        val holgura = if (moneda.codigo == "MXN") 1.0 else 2.0
        if (tarifa <= 0) return null
        if (f != null && (tarifa * f < 10 / holgura || tarifa * f > 20000 * holgura)) return null
        if (km < 0.3 || km > 500) return null
        if (min < 1 || min > 600) return null
        // Con un solo tramo no sabemos cuál es la recogida.
        if (tramos < 2) { recKm = 0.0; recMin = 0.0 }
        val dinamica = buscarDinamica(textos, r).let { if (it < tarifa) it else 0.0 }
        val o = Oferta(tarifa, km, min, recKm, recMin, buscarMultiplicador(textos), dinamica)
        // Descarta lecturas imposibles.
        if (f != null && (o.porKm * f > 80 * holgura || o.porHora * f > 3000 * holgura)) return null
        return o
    }

    /**
     * La barra de navegación de un viaje en curso («12 min · 2.7 km», 13 nov: Jonny, 10.72): un
     * solo tramo con viñeta, sin paréntesis, sin «$/km est.» ni «Aceptar $X». Una oferta real trae
     * recogida y viaje (y Uber su «$/km est.»). El OCR leía la batería como monto («5G .!$ 45»).
     */
    fun esNavegacion(textos: List<String>, moneda: Moneda = Moneda.MXN): Boolean {
        val r = reglasDe(moneda)
        if (textos.any { porKmTexto.containsMatchIn(it) || r.aceptarMonto.containsMatchIn(it) }) return false
        val claves = HashSet<String>()
        var conParentesis = false
        for (t in textos) {
            if (t.contains("total", ignoreCase = true)) continue
            val c = corregirMinutos(t)
            for (m in tramo.findAll(c)) {
                if (m.groupValues[3].isNotEmpty() || c.substring(0, m.range.first).trimEnd().endsWith("(")) conParentesis = true
                val numero = m.groupValues[3].ifEmpty { m.groupValues[5] }.replace(",", ".")
                val unidad = m.groupValues[4].ifEmpty { m.groupValues[6] }.lowercase()
                claves.add("${m.groupValues[1]}|${m.groupValues[2]}|$numero|$unidad")
            }
        }
        return claves.size == 1 && !conParentesis
    }

    /** true si algún texto trae un monto (sin montos no es una oferta: p. ej. la navegación de un viaje). */
    fun tieneMonto(textos: List<String>, moneda: Moneda = Moneda.MXN): Boolean {
        val r = reglasDe(moneda)
        return textos.any { t -> !t.contains("total", ignoreCase = true) && (r.montoSolo.containsMatchIn(t) || r.monto.containsMatchIn(t)) }
    }

    /**
     * Revisa una lectura que llegó de fuera ([Confirmador]) con los mismos límites del lector.
     * Además, la tarifa tiene que estar escrita en [textos] y, si Uber muestra «$Y/km est.»,
     * debe dar ese $/km sobre los km totales.
     */
    fun plausible(o: Oferta, textos: List<String>, moneda: Moneda = Moneda.MXN): Boolean {
        val f = moneda.factor
        val holgura = if (moneda.codigo == "MXN") 1.0 else 2.0
        if (o.tarifa <= 0 || o.km < 0.3 || o.km > 500 || o.min < 1 || o.min > 600) return false
        if (o.recogidaKm > o.km || o.recogidaMin > o.min) return false
        if (f != null && (o.tarifa * f < 10 / holgura || o.tarifa * f > 20000 * holgura)) return false
        if (f != null && (o.porKm * f > 80 * holgura || o.porHora * f > 3000 * holgura)) return false
        val digitos = textos.joinToString(" ") { t -> t.filter { it.isDigit() || it == ' ' } }
        val cent = Math.round(o.tarifa * 100)
        val conDec = cent.toString()
        val sinDec = (cent / 100).toString()
        if (!digitos.contains(conDec) && !(cent % 100 == 0L && Regex("""(?<!\d)$sinDec(?!\d)""").containsMatchIn(digitos))) return false
        for (t in textos) {
            if (tramo.find(corregirMinutos(t)) == null) continue
            val p = porKmTexto.find(t)?.groupValues?.get(1)?.replace(",", ".")?.toDoubleOrNull() ?: continue
            if (Math.abs(o.porKm - p) > p * 0.06 + 0.1) return false
        }
        return true
    }

    // El OCR lee "11 min" como "ll min" o "1l min": esas l/I/| junto a "min" son unos.
    // Con letra gruesa también confunde 8→B, 0→O, 5→S ("B min • 2.2 km") y «1 h» → «Ih».
    // «1hl2 min»: también después de la «h» de las horas.
    private val minutosOcr = Regex("""(?<![A-Za-gi-z0-9])[0-9lI|BOoS]*[lI|BOoS][0-9lI|BOoS]*(?=\s?min)""")
    private val horasOcr = Regex("""(?<![A-Za-z0-9])[lI|](?=\s?h(?:rs?|oras?)?\s*\d+\s?min)""")
    // Uber nuevo (12 nov): «6 min l.9 km», «minl.2 km» (l por 1) y «15.4 kmn», «4.7 krn».
    private val unoKm = Regex("""(?:(?<=min)|(?<![A-Za-z]))[lI|](?=[\d.,lI|]*\s?(?:km|m)\b)""")
    private val kmOcr = Regex("""(?<=\d)(\s?)(?:krn|knm|kmn|kon)\b""", RegexOption.IGNORE_CASE)
    private fun corregirMinutos(t: String): String {
        val c = horasOcr.replace(minutosOcr.replace(t) {
            it.value.replace(Regex("[lI|]"), "1").replace('B', '8').replace(Regex("[Oo]"), "0").replace('S', '5')
        }, "1")
        return unoKm.replace(kmOcr.replace(c, "\$1km"), "1")
    }

    private fun buscarTarifa(textos: List<String>, r: Reglas): Double? {
        // 0) DiDi (12 nov): «Aceptar $X» y luego «$X 1.3x» mandan; las contraofertas o el
        //    monto del pop-up propio pueden quedar arriba en el OCR.
        for (t in textos) r.aceptarMonto.find(t)?.let { m -> r.valor(m)?.let { return it } }
        for (t in textos) r.montoMulti.find(t)?.let { m -> r.valor(m)?.let { return it } }
        // Si se ve la línea «$/h» del pop-up propio, su monto (entero, sin centavos) no es la tarifa.
        if (!r.moneda.sinCentavos && textos.any { r.lineaPopup.containsMatchIn(it) }) {
            for (t in textos) {
                val m = r.montoSolo.find(t) ?: continue
                val s = m.groupValues.drop(1).firstOrNull { it.isNotEmpty() } ?: continue
                if (!s.contains(r.moneda.decimal)) continue
                return r.valor(m)
            }
        }
        // 1) Primer texto que es solo un monto (la tarifa grande va antes que los botones).
        for (t in textos) {
            val m = r.montoSolo.find(t) ?: continue
            return r.valor(m)
        }
        // 2) Respaldo: primer monto dentro de un texto que no sea extra/bono/precio por km.
        for (t in textos) {
            if (noTarifa.containsMatchIn(t)) continue
            val m = r.monto.find(t) ?: continue
            return r.valor(m)
        }
        return null
    }
}
