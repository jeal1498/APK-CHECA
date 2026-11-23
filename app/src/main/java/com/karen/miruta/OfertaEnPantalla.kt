package com.karen.miruta

/**
 * 10.86: ¿los tramos que se ven en la zona de un pop-up son de SU oferta? Kotlin puro (se prueba en la JVM).
 *
 * Caso real (20 nov, capturas_ok 1289, Uber): el pop-up de la oferta anterior seguía ~15 s encima cuando llegó la
 * nueva en la misma zona. `RutaService.revisarSalida` solo lo quitaba si NO había tramos en su zona; con la oferta
 * nueva sí los había y el pop-up viejo se quedaba. Ahora, si hay tramos legibles y ninguno es de la oferta del
 * pop-up (ni de la lectura original, por si la revisión la corrigió), cuenta como «ya no está».
 */
object OfertaEnPantalla {
    private const val TOL_KM = 0.15
    private const val TOL_MIN = 1.1

    /** Tramos (km, min) de una oferta: recogida, viaje y el total (Uber también pone un resumen con el total). */
    private fun tramosDe(o: Oferta): List<Pair<Double, Double>> {
        val l = ArrayList<Pair<Double, Double>>()
        if (o.recogidaMin > 0) {
            l.add(o.recogidaKm to o.recogidaMin)
            if (o.viajeMin > 0) l.add(o.viajeKm to o.viajeMin)
        }
        l.add(o.km to o.min)
        return l
    }

    /**
     * true: algún tramo visible es de la oferta. false: hay tramos legibles y NINGUNO lo es (es otra oferta).
     * null: no hay tramos que comparar.
     */
    fun coincide(visibles: List<Pair<Double, Double>>, mostrada: Oferta, original: Oferta? = null): Boolean? {
        if (visibles.isEmpty()) return null
        val suyos = tramosDe(mostrada) + (original?.let { tramosDe(it) } ?: emptyList())
        return visibles.any { v -> suyos.any { s -> Math.abs(v.first - s.first) < TOL_KM && Math.abs(v.second - s.second) < TOL_MIN } }
    }
}
