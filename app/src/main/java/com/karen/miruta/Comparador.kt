package com.karen.miruta

/** Decide cuál oferta conviene más cuando hay varias en pantalla. */
object Comparador {
    const val NETA_HORA = 0
    const val NETA_KM = 1
    const val NETA_TOTAL = 2

    fun puntaje(o: Oferta, p: Prefs): Double {
        val neta = o.neta(p)
        return when (p.criterio) {
            NETA_KM -> if (o.km > 0) neta / o.km else 0.0
            NETA_TOTAL -> neta
            else -> if (o.min > 0) neta / o.min * 60.0 else 0.0
        }
    }

    /** Devuelve la marca de cada app: MEJOR para la ganadora, SEGUNDA para las demás. */
    fun marcas(ofertas: Map<String, Oferta>, p: Prefs): Map<String, Int> {
        if (ofertas.size < 2) return emptyMap()
        val orden = ofertas.entries.sortedWith(
            compareByDescending<Map.Entry<String, Oferta>> { it.value.nivel(p) }
                .thenByDescending { puntaje(it.value, p) }
        )
        return orden.mapIndexed { i, e -> e.key to (if (i == 0) Popup.MEJOR else Popup.SEGUNDA) }.toMap()
    }
}
