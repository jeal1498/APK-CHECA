package com.karen.miruta

import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * 10.86: el pop-up nuevo ya no lleva la línea «N min (X km) total»; ahora muestra «18 min» y «6.5 km» sueltos, «Recogida 5.5 km»,
 * «Viaje 42 min», la app con su tarifa y «Leyó mal». Si el OCR llegara a leerlos (el lector ya ignora lo que tapa el pop-up),
 * no deben cambiar ninguna lectura: se agregan a TODOS los casos de `ofertas/` y `banco/` y el resultado debe ser el mismo.
 */
class PopupLectorTest {
    private val textosDelPopup = listOf(
        "RECHAZAR", "\$41", "✕ \$98/h", "18 min", "✕ \$5.4/km", "7.9 km",
        "📍 Recogida 5.5 km", "⏱ Viaje 42 min", "⚠ Menos de tu mínimo · \$22", "⚠ Leyó mal", "Uber · \$42.31"
    )
    private val compacto = listOf("● ACEPTAR", "\$112", "✓ \$240/h", "⚠ 3")

    private fun casos(carpeta: String): List<Pair<String, List<String>>> {
        val url = javaClass.classLoader!!.getResource(carpeta) ?: error("No está la carpeta $carpeta")
        return File(url.toURI()).listFiles { f -> f.extension == "txt" && f.name != "minimo.txt" }!!.sortedBy { it.name }.map { f ->
            val partes = f.readText().replace("\r", "").split("\n---\n")
            f.name to partes[0].lines().filter { it.isNotBlank() && !it.startsWith("#") }
        }
    }

    // El pop-up de la 10.85 (con la línea «total» y el aviso en una línea): sirve de referencia.
    private val textosDelPopupViejo = listOf(
        "RECHAZAR", "\$41", "✕ \$98/h", "✕ \$5.4/km", "24 min (6.0 km) total", "Recogida lejana · 2.5 km total"
    )

    /** Casos cuya lectura cambia al agregar [popup] (al principio o al final de las líneas). */
    private fun cambios(popup: List<String>): List<Triple<String, Oferta?, Oferta?>> {
        val r = ArrayList<Triple<String, Oferta?, Oferta?>>()
        for (carpeta in listOf("ofertas", "banco")) for ((nombre, lineas) in casos(carpeta)) {
            val base = Parser.analizar(lineas)
            for ((donde, conPopup) in listOf("principio" to popup + lineas, "final" to lineas + popup)) {
                val o = Parser.analizar(conPopup)
                if (o != base) r.add(Triple("$carpeta/$nombre ($donde)", base, o))
            }
        }
        return r
    }

    /**
     * Peor caso (el OCR NO ignora el texto del pop-up; en la práctica sí lo ignora, `rectsPopups()`): agregar el texto del
     * pop-up a una oferta puede cambiar su lectura (p. ej. su monto sin centavos pasa a ser tarifa si el lector no la
     * entendía). Eso ya pasaba con el pop-up de la 10.85. Lo que se exige aquí es que el pop-up NUEVO, completo o compacto,
     * no agregue ningún caso (ni de lecturas buenas ni de nulas) a los que ya confundía el viejo.
     */
    @Test fun elPopupNuevoNoAgregaConfusionesNuevas() {
        val viejo = cambios(textosDelPopupViejo).map { it.first }.toSet()
        val nuevas = (cambios(textosDelPopup) + cambios(compacto)).map { it.first }.filter { it !in viejo }
        assertTrue("Casos que confunde el pop-up nuevo y no el de la 10.85:\n" + nuevas.joinToString("\n"), nuevas.isEmpty())
    }
}
