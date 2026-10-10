package com.karen.miruta

data class Oferta(val tarifa: Double, val km: Double, val min: Double) {
    val porKm: Double get() = if (km > 0) tarifa / km else 0.0
    val porHora: Double get() = if (min > 0) tarifa / min * 60.0 else 0.0
}

/**
 * Lee la oferta a partir de los textos de pantalla (en orden de aparición).
 *
 * Formatos vistos:
 *  DiDi:  "$151.34" · "5min (1.7km)" · "37min (13.2km)" · "2min (720m)"
 *         botones de contraoferta "$163.34" (van DESPUÉS de la tarifa)
 *         "$9.00 adicionales", "Aceptar $151.34"
 *  Uber:  "$48.73" · "$10.15/km est." · "A 5 min (1.6 km)" · "Viaje: 8 min (3.2 km)"
 *         "+$9.75 por inicio de viaje prioritario"
 *
 * Tarifa = primer texto que sea SOLO un monto. Km y minutos = suma de
 * los tramos "N min (X km)" (ir por el pasajero + viaje).
 */
object Parser {
    private const val NUM = """(\d{1,3}(?:,\d{3})+(?:\.\d{1,2})?|\d+(?:\.\d{1,2})?)"""
    private val montoSolo = Regex("""^\s*(?:MXN?\s?)?\$\s?$NUM\s*(?:[⚡x×\d.\s]*)$""")
    private val monto = Regex("""(?<!\+)\$\s?$NUM(?!\s?/)""")
    private val noTarifa = Regex("""(adicional|/km|/h|aceptar|por inicio|propina|ganancia|hoy|semana|saldo)""", RegexOption.IGNORE_CASE)

    // "5min (1.7km)", "A 5 min (1.6 km)", "Viaje: 8 min (3.2 km)", "2min (720m)", "1 h 5 min (40 km)"
    private val tramo = Regex(
        """(?:(\d+)\s?h(?:rs?|oras?)?\s*)?(\d+)\s?min\s*\(\s*(\d+(?:[.,]\d+)?)\s?(km|m)\s*\)""",
        RegexOption.IGNORE_CASE
    )

    fun analizar(textos: List<String>): Oferta? {
        val tarifa = buscarTarifa(textos) ?: return null

        var km = 0.0
        var min = 0.0
        var tramos = 0
        val vistos = HashSet<String>()
        for (t in textos) {
            for (m in tramo.findAll(t)) {
                // Evita contar dos veces el mismo tramo si un contenedor repite el texto.
                if (!vistos.add(m.value.replace(" ", "").lowercase())) continue
                val h = m.groupValues[1].toDoubleOrNull() ?: 0.0
                val mi = m.groupValues[2].toDoubleOrNull() ?: 0.0
                val d = m.groupValues[3].replace(",", ".").toDoubleOrNull() ?: 0.0
                min += h * 60.0 + mi
                km += if (m.groupValues[4].equals("m", ignoreCase = true)) d / 1000.0 else d
                tramos++
            }
        }

        // Una oferta siempre trae 2 tramos (recogida + viaje); con 1 aceptamos también.
        if (tramos == 0) return null
        if (tarifa < 10 || tarifa > 20000) return null
        if (km < 0.1 || km > 500) return null
        if (min < 1 || min > 600) return null
        return Oferta(tarifa, km, min)
    }

    private fun aNumero(s: String): Double? = s.replace(",", "").toDoubleOrNull()

    private fun buscarTarifa(textos: List<String>): Double? {
        // 1) Primer texto que es solo un monto (la tarifa grande va antes que los botones).
        for (t in textos) {
            val m = montoSolo.find(t) ?: continue
            return aNumero(m.groupValues[1])
        }
        // 2) Respaldo: primer monto dentro de un texto que no sea extra/bono/precio por km.
        for (t in textos) {
            if (noTarifa.containsMatchIn(t)) continue
            val m = monto.find(t) ?: continue
            return aNumero(m.groupValues[1])
        }
        return null
    }
}
