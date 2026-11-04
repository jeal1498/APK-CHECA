package com.karen.miruta

data class Oferta(
    val tarifa: Double,
    val km: Double,
    val min: Double,
    val recogidaKm: Double = 0.0,
    val recogidaMin: Double = 0.0,
    val multiplicador: Double = 0.0,
    /** Dinámica de Uber en pesos ("+$62.00 incluido"), ya incluida en la tarifa. */
    val dinamica: Double = 0.0
) {
    val porKm: Double get() = if (km > 0) tarifa / km else 0.0
    val porHora: Double get() = if (min > 0) tarifa / min * 60.0 else 0.0
    val viajeKm: Double get() = km - recogidaKm
    val viajeMin: Double get() = min - recogidaMin

    /** Costo de gasolina de todo el recorrido (recogida + viaje). */
    fun gasolina(p: Prefs): Double =
        if (p.kmPorLitro > 0) km / p.kmPorLitro * p.precioLitro else 0.0

    fun neta(p: Prefs): Double = tarifa - gasolina(p)

    /** Si la oferta no pasa los filtros de recogida o duración, dice por qué. */
    fun motivoFiltro(p: Prefs): String? {
        val f1 = { v: Double -> String.format(java.util.Locale.US, "%.1f", v) }
        return when {
            p.filtroRecMax && recogidaKm > 0 && recogidaKm > p.recogidaMaxKm ->
                "Recogida lejana: ${f1(recogidaKm)} km (máx ${f1(p.recogidaMaxKm.toDouble())})"
            p.filtroDurMax && viajeMin > p.duracionMax ->
                "Viaje largo: ${viajeMin.toInt()} min (máx ${p.duracionMax})"
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

        init {
            val d = sep(moneda.decimal)
            val alt = """\d+(?:$d\d{1,2})?"""
            num = if (moneda.miles.isEmpty()) "($alt)"
                else """(\d{1,3}(?:${sep(moneda.miles)}\d{3})+(?:$d\d{1,2})?|$alt)"""
            sim = moneda.simbolos().joinToString("|") { Regex.escape(it) }
            pre = if (legado) """\$""" else """(?:${Regex.escape(moneda.codigo)}\s?)?(?:$sim)"""
            val cola = """\s*(?:[⚡x×\d.\s]*)$"""
            if (legado || !moneda.despues) {
                val preSolo = if (legado) """(?:MXN?\s?)?\$""" else pre
                montoSolo = Regex("""^\s*$preSolo\s?$num$cola""")
                monto = Regex("""(?<!\+)$pre\s?$num(?!\s?/)""")
                incluido = Regex("""\+?\s?$pre\s?$num\s*incluid""", RegexOption.IGNORE_CASE)
                montoGrande = if (legado) Regex("""^\$\s?[\d,]+(?:\.\d{1,2})?$""")
                    else Regex("""^\s*$pre\s?$num\s*$""")
            } else {
                // «12,50 €»; también se acepta «€12,50».
                montoSolo = Regex("""^\s*(?:$num\s?(?:$sim)|$pre\s?$num)$cola""")
                monto = Regex("""(?<![+\d.,])$num\s?(?:$sim)(?!\s?/)|(?<!\+)$pre\s?$num(?!\s?/)""")
                incluido = Regex("""\+?\s?(?:$num\s?(?:$sim)|$pre\s?$num)\s*incluid""", RegexOption.IGNORE_CASE)
                montoGrande = Regex("""^\s*(?:$num\s?(?:$sim)|$pre\s?$num)\s*$""")
            }
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
    private val tramo = Regex(
        """(?:(\d+)\s?h(?:rs?|oras?)?\s*)?(\d+)\s?min\s*\(\s*(\d+(?:[.,]\d+)?)\s?(km|m)\s*\)""",
        RegexOption.IGNORE_CASE
    )

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

    fun analizar(textos: List<String>, moneda: Moneda = Moneda.MXN): Oferta? {
        val r = reglasDe(moneda)
        val tarifa = buscarTarifa(textos, r) ?: return null

        var km = 0.0
        var min = 0.0
        var tramos = 0
        var recKm = 0.0
        var recMin = 0.0
        val vistos = HashSet<String>()
        for (t in textos) {
            // "42 min (14.9 km) total" es un resumen (nuestro pop-up u otra app): no sumar.
            if (t.contains("total", ignoreCase = true)) continue
            for (m in tramo.findAll(corregirMinutos(t))) {
                // Evita contar dos veces el mismo tramo si un contenedor repite el texto.
                if (!vistos.add(m.value.replace(" ", "").lowercase())) continue
                val h = m.groupValues[1].toDoubleOrNull() ?: 0.0
                val mi = m.groupValues[2].toDoubleOrNull() ?: 0.0
                val numero = m.groupValues[3]
                val d = numero.replace(",", ".").toDoubleOrNull() ?: 0.0
                // Metros reales siempre son enteros ≥ 30 ("720m"). "2.4m" o "1m" son km mal leídos.
                val esMetros = m.groupValues[4].equals("m", ignoreCase = true) &&
                    !numero.contains('.') && !numero.contains(',') && d >= 30
                val tramoKm = if (esMetros) d / 1000.0 else d
                val tramoMin = h * 60.0 + mi
                if (tramos == 0) { recKm = tramoKm; recMin = tramoMin }
                min += tramoMin
                km += tramoKm
                tramos++
            }
        }

        // Una oferta siempre trae 2 tramos (recogida + viaje); con 1 aceptamos también.
        if (tramos == 0) return null
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

    // El OCR lee "11 min" como "ll min" o "1l min": esas l/I/| junto a "min" son unos.
    private val minutosOcr = Regex("""(?<![A-Za-z0-9])[0-9lI|]*[lI|][0-9lI|]*(?=\s?min)""")
    private fun corregirMinutos(t: String): String =
        minutosOcr.replace(t) { it.value.replace(Regex("[lI|]"), "1") }

    private fun buscarTarifa(textos: List<String>, r: Reglas): Double? {
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
