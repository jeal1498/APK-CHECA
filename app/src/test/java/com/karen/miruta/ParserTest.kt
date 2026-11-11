package com.karen.miruta

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * Pruebas del lector con ofertas de ejemplo (archivos .txt en `app/src/test/resources/ofertas/`).
 * Cada archivo: las líneas tal como las lee el OCR, luego una línea «---» y lo esperado
 * («tarifa=151.34 km=14.9 min=42 recKm=1.7 recMin=5», o «nada» si no es una oferta).
 * Las líneas que empiezan con # son comentarios. Para agregar un caso de «Guardar pantallas
 * de ofertas», copia sus líneas a un archivo nuevo y escribe lo que debió entender.
 */
class ParserTest {
    private fun archivos(): List<File> {
        val url = javaClass.classLoader!!.getResource("ofertas") ?: error("No está la carpeta de ofertas")
        return File(url.toURI()).listFiles { f -> f.extension == "txt" }!!.sortedBy { it.name }
    }

    @Test
    fun hayEjemplos() {
        assertTrue("Faltan ofertas de ejemplo", archivos().size >= 10)
    }

    @Test
    fun ofertasDeEjemplo() {
        val fallas = ArrayList<String>()
        for (f in archivos()) {
            val partes = f.readText().replace("\r", "").split("\n---\n")
            if (partes.size != 2) { fallas.add("${f.name}: falta la línea ---"); continue }
            val lineas = partes[0].lines().filter { it.isNotBlank() && !it.startsWith("#") }
            val esperado = partes[1].trim()
            val o = Parser.analizar(lineas)
            if (esperado == "nada") {
                if (o != null) fallas.add("${f.name}: esperaba nada y leyó $o")
                continue
            }
            if (o == null) { fallas.add("${f.name}: no leyó la oferta"); continue }
            val leido = mapOf(
                "tarifa" to o.tarifa, "km" to o.km, "min" to o.min, "recKm" to o.recogidaKm,
                "recMin" to o.recogidaMin, "multi" to o.multiplicador, "dinamica" to o.dinamica
            )
            for (par in esperado.split(Regex("\\s+"))) {
                val (k, v) = par.split("=")
                val real = leido[k]
                if (real == null) { fallas.add("${f.name}: dato desconocido «$k»"); continue }
                if (Math.abs(real - v.toDouble()) > 0.01) fallas.add("${f.name}: $k esperado $v, leído $real")
            }
        }
        assertTrue(fallas.joinToString("\n"), fallas.isEmpty())
    }

    @Test
    fun revisionPlausible() {
        val l = listOf("\$66.21", "33 min · 10.6 km · \$6.25/km est.", "9 min · 3.0 km", "24 min · 7.6 km")
        assertTrue(Parser.plausible(Oferta(66.21, 10.6, 33.0, 3.0, 9.0), l))
        // Tarifa que no está en pantalla (otra tarjeta del radar) o que no da el $/km del resumen.
        assertFalse(Parser.plausible(Oferta(108.99, 10.6, 33.0, 3.0, 9.0), l))
        assertFalse(Parser.plausible(Oferta(66.21, 21.2, 66.0, 3.0, 9.0), l))
    }

    @Test
    fun rescateSoloConMonto() {
        // Navegación de un viaje: minutos y km sin monto → no se pide rescate.
        assertFalse(Parser.tieneMonto(listOf("Llegada 12:40", "8 min · 3.1 km", "Av. Insurgentes")))
        assertTrue(Parser.tieneMonto(listOf("UberX", "\$103.13", "6 min · 2.3 km")))
        // El pop-up propio («… total») no cuenta como monto de oferta.
        assertFalse(Parser.tieneMonto(listOf("24 min (6.0 km) total")))
    }
}
