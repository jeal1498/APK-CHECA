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

    @Test
    fun dosTarjetasDeDidi() {
        // 11 nov (10.53): ventana flotante de DiDi con el final de una tarjeta arriba, una completa
        // y el monto de otra abajo. Antes sumó los tres tramos (26.6 km).
        val r = listOf(
            Tarjetas.Renglon("O (14 min 7.8 km) Jardín de Niños Madre", 100, 130),
            Tarjetas.Renglon("\$95.21", 200, 260),
            Tarjetas.Renglon("(9 min 2.6 km) Carlos Hank González 9,", 290, 320),
            Tarjetas.Renglon("(42 min 16.2 km) Alcaldia Gustavo A.", 340, 370),
            Tarjetas.Renglon("Aceptar \$95.21", 400, 430),
            Tarjetas.Renglon("\$97.21", 400, 430),
            Tarjetas.Renglon("\$85.15", 500, 560)
        )
        val idx = Tarjetas.primera(r) ?: error("No separó las tarjetas")
        val o = Parser.analizar(idx.map { r[it].texto }) ?: error("No leyó la tarjeta")
        assertTrue("km ${o.km}", Math.abs(o.km - 18.8) < 0.01 && Math.abs(o.tarifa - 95.21) < 0.01)
        // Una sola oferta (dos tramos): no se toca.
        assertTrue(Tarjetas.primera(r.subList(1, 5)) == null)
    }
}
