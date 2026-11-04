package com.karen.miruta

import kotlin.math.abs
import kotlin.math.roundToLong

/**
 * Moneda y formato de los montos. Es la misma de Clientela: se guarda en `perfil_conductor`
 * (moneda, simbolo, simbolo_despues, sep_miles, sep_decimal, decimales). Por defecto, pesos mexicanos.
 */
data class Moneda(
    val codigo: String = "MXN",
    val simbolo: String = "$",
    /** «12,50 €» en vez de «€12,50». */
    val despues: Boolean = false,
    val miles: String = ",",
    val decimal: String = ".",
    /** Decimales de la moneda en Clientela (0 o 2). Checa los usa solo para saber si hay centavos. */
    val decimales: Int = 0
) {
    /** Pesos mexicanos por unidad, aproximado: solo para las protecciones del lector. null = no se sabe. */
    val factor: Double? get() = FACTORES[codigo]

    /** Monedas de unidad chica (COP, ARS, CLP…): nunca se muestran centavos. */
    val sinCentavos: Boolean get() = (factor ?: 1.0) < 0.1

    /** Número con los separadores de esta moneda («1.234,50»). */
    fun numero(v: Double, dec: Int): String {
        val d = if (sinCentavos) 0 else dec.coerceIn(0, 2)
        var escala = 1L
        repeat(d) { escala *= 10 }
        val total = (abs(v) * escala).roundToLong()
        val ent = (total / escala).toString()
        val frac = (total % escala).toString().padStart(d, '0')
        val conMiles = ent.reversed().chunked(3).joinToString(miles).reversed()
        return if (d > 0) conMiles + decimal + frac else conMiles
    }

    /** Monto con símbolo: «$151.34», «12,50 €», «-$20». */
    fun monto(v: Double, dec: Int = 2): String {
        val n = numero(v, dec)
        val neg = v < 0 && n.any { it in '1'..'9' }
        return (if (neg) "-" else "") + if (despues) "$n $simbolo" else "$simbolo$n"
    }

    /** «$12.50/km», «150 €/h». */
    fun tasa(v: Double, dec: Int, unidad: String): String = monto(v, dec) + unidad

    /** Para el lector: los símbolos que pueden ir junto a un monto (el elegido, «$» si lo lleva y el código). */
    fun simbolos(): List<String> =
        listOfNotNull(simbolo, if (simbolo.contains('$')) "$" else null, codigo)
            .distinct().sortedByDescending { it.length }

    fun normalizada(): Moneda {
        val c = if (Regex("^[A-Z]{3}$").matches(codigo)) codigo else "MXN"
        val s = simbolo.trim().take(4).ifEmpty { "$" }
        val dec = if (decimal == ",") "," else "."
        var mil = if (miles in listOf(",", ".", " ", "'", "")) miles else ","
        if (mil == dec) mil = if (dec == ",") "." else ","
        return Moneda(c, s, despues, mil, dec, if (decimales == 2) 2 else 0)
    }

    companion object {
        val MXN = Moneda()

        /** Igual que `MONEDAS` de Clientela (js/moneda.js): código, nombre y formato. */
        val LISTA: List<Pair<Moneda, String>> = listOf(
            Moneda("MXN", "$", false, ",", ".", 0) to "Peso mexicano",
            Moneda("USD", "$", false, ",", ".", 2) to "Dólar",
            Moneda("COP", "$", false, ".", ",", 0) to "Peso colombiano",
            Moneda("ARS", "$", false, ".", ",", 0) to "Peso argentino",
            Moneda("CLP", "$", false, ".", ",", 0) to "Peso chileno",
            Moneda("UYU", "\$U", false, ".", ",", 0) to "Peso uruguayo",
            Moneda("PEN", "S/", false, ",", ".", 2) to "Sol peruano",
            Moneda("BOB", "Bs", false, ".", ",", 2) to "Boliviano",
            Moneda("GTQ", "Q", false, ",", ".", 2) to "Quetzal",
            Moneda("CRC", "₡", false, ".", ",", 0) to "Colón costarricense",
            Moneda("DOP", "RD$", false, ",", ".", 2) to "Peso dominicano",
            Moneda("BRL", "R$", false, ".", ",", 2) to "Real",
            Moneda("EUR", "€", true, ".", ",", 2) to "Euro"
        )

        /** Aproximado (nov 2026). Solo sirve para descartar lecturas imposibles, nunca para mostrar. */
        private val FACTORES = mapOf(
            "MXN" to 1.0, "USD" to 18.5, "COP" to 0.0047, "ARS" to 0.016, "CLP" to 0.019,
            "UYU" to 0.46, "PEN" to 5.4, "BOB" to 2.7, "GTQ" to 2.4, "CRC" to 0.036,
            "DOP" to 0.30, "BRL" to 3.4, "EUR" to 21.0
        )
    }
}
