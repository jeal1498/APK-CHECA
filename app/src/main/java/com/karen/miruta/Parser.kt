package com.karen.miruta

data class Oferta(val tarifa: Double, val km: Double, val min: Double) {
    val porKm: Double get() = if (km > 0) tarifa / km else 0.0
    val porHora: Double get() = if (min > 0) tarifa / min * 60.0 else 0.0
}

/**
 * Busca en los textos de la pantalla: una tarifa ($), kilómetros y minutos.
 * Suma los km y minutos que encuentre (ir por el pasajero + viaje).
 */
object Parser {
    private val dinero = Regex("""(?:MXN?\s?)?\$\s?(\d{1,3}(?:,\d{3})+(?:\.\d{1,2})?|\d+(?:\.\d{1,2})?)""")
    private val kms = Regex("""(\d+(?:[.,]\d+)?)\s?km\b""", RegexOption.IGNORE_CASE)
    private val metros = Regex("""(?<![\d.,])(\d+)\s?m\b""")
    private val tiempo = Regex("""(?:(\d+)\s?h(?:rs?|oras?)?\s*)?(\d+)\s?min""", RegexOption.IGNORE_CASE)
    private val ignorar = Regex("""(hoy|semana|ganancia|saldo|billetera|cartera|meta|propina)""", RegexOption.IGNORE_CASE)

    fun analizar(textosEntrada: List<String>): Oferta? {
        // Quita textos que solo agrupan a otros (para no contar doble).
        val textos = textosEntrada.filter { s ->
            textosEntrada.none { t -> t != s && t.length >= 3 && s.contains(t) }
        }

        var tarifa = 0.0
        var km = 0.0
        var min = 0.0

        for (t in textos) {
            if (!ignorar.containsMatchIn(t)) {
                for (m in dinero.findAll(t)) {
                    val v = m.groupValues[1].replace(",", "").toDoubleOrNull() ?: continue
                    if (v > tarifa) tarifa = v
                }
            }
            for (m in kms.findAll(t)) {
                km += m.groupValues[1].replace(",", ".").toDoubleOrNull() ?: 0.0
            }
            for (m in metros.findAll(t)) {
                km += (m.groupValues[1].toDoubleOrNull() ?: 0.0) / 1000.0
            }
            for (m in tiempo.findAll(t)) {
                val h = m.groupValues[1].toDoubleOrNull() ?: 0.0
                val mi = m.groupValues[2].toDoubleOrNull() ?: 0.0
                min += h * 60.0 + mi
            }
        }

        if (tarifa < 10 || tarifa > 20000) return null
        if (km < 0.1 || km > 500) return null
        if (min < 1 || min > 600) return null
        return Oferta(tarifa, km, min)
    }
}
