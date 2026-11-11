package com.karen.miruta

/**
 * Varias ofertas en la misma zona (lista de DiDi, ventana flotante, radar): se separan por
 * tarjeta para no sumar los tramos de una con la tarifa de otra (11 nov: $95.21 con tres
 * tramos dio 26.6 km en vez de 16.2).
 *
 * Cada tarjeta empieza en un monto grande (la tarifa) y llega hasta el siguiente. Lo que
 * queda arriba del primer monto es el pedazo de una tarjeta cortada: se descarta. Se toma la
 * primera tarjeta completa (con recogida y viaje). Sin JVM de Android, para poder probarla.
 */
object Tarjetas {
    // «5min (1.7km)» (DiDi, Uber viejo) y «6 min · 2.3 km» (Uber nov 2026).
    val tramoTexto = Regex("""\d\s?min\s*(?:\(|[^\d(\s]{0,3}\s*\d+(?:[.,]\d+)?\s?k?m\b)""", RegexOption.IGNORE_CASE)

    /** Un tramo «N min (» de una oferta; el resumen «… total» (pop-up propio o de otra app) no cuenta. */
    fun esTramo(t: String) = tramoTexto.containsMatchIn(t) && !t.contains("total", ignoreCase = true)

    /** Un renglón del OCR con su posición vertical (px). */
    data class Renglon(val texto: String, val arriba: Int, val abajo: Int) {
        val alto get() = abajo - arriba
        val centro get() = (arriba + abajo) / 2
    }

    /**
     * Índices de los renglones de la primera tarjeta completa, o null si no hace falta separar
     * (dos tramos o menos) o no se puede (sin montos grandes, ninguna tarjeta completa).
     * [renglones] van ordenados de arriba abajo.
     */
    fun primera(renglones: List<Renglon>, moneda: Moneda = Moneda.MXN): List<Int>? {
        val tramo = { r: Renglon -> esTramo(r.texto) }
        if (renglones.count(tramo) <= 2) return null
        val montos = renglones.indices.filter { Parser.esMontoSolo(renglones[it].texto, moneda) }
        val mayor = montos.maxOfOrNull { renglones[it].alto } ?: return null
        if (mayor <= 0) return null
        // La tarifa va en letra grande; los botones de contraoferta («$34.74») son más chicos.
        val anclas = montos.filter { renglones[it].alto * 4 >= mayor * 3 }
        for ((k, a) in anclas.withIndex()) {
            val desde = renglones[a].arriba - renglones[a].alto / 2
            val hasta = anclas.getOrNull(k + 1)?.let { renglones[it].arriba - renglones[it].alto / 2 } ?: Int.MAX_VALUE
            val idx = renglones.indices.filter { renglones[it].centro in desde until hasta }
            if (idx.count { tramo(renglones[it]) } >= 2) return idx
        }
        return null
    }
}
