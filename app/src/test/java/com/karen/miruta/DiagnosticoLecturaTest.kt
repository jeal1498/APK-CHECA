package com.karen.miruta

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** 10.86: diagnóstico OCR → Parser → resultado (texto local) y motivos que deja el Parser. */
class DiagnosticoLecturaTest {
    private fun l(t: String, arriba: Int) = DiagnosticoLectura.LineaOcr(t, arriba, arriba + 30, 40, 300)

    @Test fun lasTresEtapasYLasPosiciones() {
        val ocr = listOf(l("\$29.71 Ox1.2", 120), l("(10 min 2.1 km) Cacama 25, El Tenayo", 200), l("(11 min 2.7 km) Cacama 27", 320))
        val o = Oferta(29.71, 4.8, 21.0, 2.1, 10.0, 1.2)
        val t = DiagnosticoLectura.armar("f-1", ocr, "\$29.71 Ox1.2", 3, ocr.map { it.texto }, ocr.map { it.texto }, o, "elegida", null, 540, 2)
        assertTrue(t.contains("fotograma: f-1"))
        assertTrue(t.contains("[1 OCR] 3 líneas, 540 ms"))
        assertTrue(t.contains("01 y=120-150 x=40-300  «\$29.71 Ox1.2»"))
        assertTrue(t.contains("[2 PARSER] recibió 3 líneas, 2 ms"))
        assertTrue(t.contains("selección de tarjeta: elegida"))
        assertTrue(t.contains("leyó: tarifa=29.71 km=4.80 min=21 recogida=2.10 km/10 min multiplicador=1.20"))
        assertTrue(t.contains("[3 RESULTADO] oferta enviada al pop-up"))
        assertFalse("sin separación por geometría no debe avisar", t.contains("separación por tarjetas"))
    }

    @Test fun sinOfertaDiceElMotivo() {
        val t = DiagnosticoLectura.armar(null, listOf(l("\$33.42", 10)), null, 1, listOf("\$33.42"), listOf("\$33.42"), null, "sin_separar", "un_tramo_sin_boton", 10, 1)
        assertTrue(t.contains("no leyó oferta: un_tramo_sin_boton"))
        assertTrue(t.contains("sin oferta (no se muestra pop-up)"))
        assertFalse(t.contains("fotograma:"))
    }

    @Test fun muestraLasLineasQueLaRevisionNoVe() {
        val larga = "(12 min 4.3 km) " + "Avenida muy larga de nombre interminable ".repeat(3)
        val sin = "Pon Tu Precio"
        val alParser = listOf("\$44.90", larga, sin, "Aceptar \$44.90")
        val alRevisor = listOf("\$44.90", "Aceptar \$44.90")        // lo que dejaría Confirmador.filtrar
        val t = DiagnosticoLectura.armar("f", alParser.map { l(it, 5) }, null, 4, alParser, alRevisor, null, null, "sin_tramos", 1, 1)
        assertTrue(t.contains("2 no pasarían a la revisión"))
        assertTrue(t.contains("«Pon Tu Precio»"))
        assertTrue(t.contains("(12 min 4.3 km) Avenida muy larga"))
    }

    @Test fun avisaCuandoLaGeometriaQuitoLineas() {
        val ocr = (1..6).map { l("\$1$it.00", it * 10) }
        val t = DiagnosticoLectura.armar("f", ocr, null, 4, ocr.map { it.texto }, emptyList(), null, "sin_separar", "sin_tramos", 1, 1)
        assertTrue(t.contains("se quedaron 4 de 6"))
    }

    @Test fun limitaElTamano() {
        val ocr = (1..100).map { l("línea $it " + "x".repeat(300), it) }
        val t = DiagnosticoLectura.armar("f", ocr, null, 100, ocr.map { it.texto }, emptyList(), null, null, null, 1, 1)
        assertTrue(t.contains("… 40 líneas más"))
        assertTrue("el texto debe quedar acotado", t.length < 20_000)
        assertTrue(t.contains("no leyó oferta: motivo no registrado"))
    }

    // ---- motivos que deja el Parser (no cambian el resultado) ----

    @Test fun motivosDelParser() {
        assertNull(Parser.analizar(listOf("\$33.42", "-\$1.14", "(8 min 2.6 km) Las Hijas de la Tostada")))
        assertEquals("un_tramo_sin_boton", Parser.motivoRechazo())
        assertNull(Parser.analizar(listOf("\$42.23", "1 min más lenta", "65min (1.8 km)", "|5min (2.3 km)")))
        assertEquals("ruta_alternativa", Parser.motivoRechazo())
        assertNull(Parser.analizar(listOf("Hola", "Mundo")))
        assertEquals("sin_tarifa", Parser.motivoRechazo())
        assertNull(Parser.analizar(listOf("\$40.00", "Aceptar \$40.00")))
        assertEquals("sin_tramos", Parser.motivoRechazo())
    }

    @Test fun elMotivoSeBorraConUnaLecturaBuena() {
        assertNull(Parser.analizar(listOf("Hola")))
        assertNotNull(Parser.motivoRechazo())
        val o = Parser.analizar(listOf("\$44.90", "(12 min 4.3 km) Plaza", "Aceptar \$44.90", "\$48.90 \$49.90 \$51.90"))
        assertNotNull(o)
        assertNull("una lectura buena no deja motivo de rechazo", Parser.motivoRechazo())
    }

    @Test fun eleccionDeTarjetaQuedaRegistrada() {
        // Tres tarjetas: la de arriba cortada (solo su botón) y la completa de $29.71.
        val varias = listOf(
            "\$162.97", "Aceptar \$105.34", "\$113.34", "\$118.34", "\$122.34", "Pon Tu Precio", "\$29.71 +", "4.81 • 107 viajes",
            "(10 min 2.1 km) Cacama 25, El Tenayo", "Centro, Tlalnepantla de Baz, Ciudad de", "México", "1 parada(s)",
            "(11 min 2.7 km) Cacama 27, El Tenayo", "Sur, Tlalnepantla de Baz, Ciudad de", "México", "Aceptar \$29.71",
            "\$30.71", "\$31.71", "\$32.71"
        )
        assertEquals(29.71, Parser.analizar(varias)!!.tarifa, 0.001)
        assertEquals("elegida", Parser.eleccionTarjeta())
        // Dos tarjetas lado a lado (líneas intercaladas): no se lee nada y queda dicho por qué.
        val ladoALado = listOf(
            "\$47.50 Ox1.2", "\$62.10 Ox1.4", "(6 min 1.5 km) Calle Allende 12", "(8 min 2.0 km) Calle Hidalgo 90",
            "(12 min 4.0 km) Avenida Juárez 440,", "(15 min 6.1 km) Calzada del Valle 12,", "Aceptar \$47.50", "Aceptar \$62.10"
        )
        assertNull(Parser.analizar(ladoALado))
        assertEquals("ambigua", Parser.eleccionTarjeta())
        assertEquals("tarjetas_ambiguas", Parser.motivoRechazo())
        // Una sola tarjeta: no se separa.
        Parser.analizar(listOf("\$44.90", "(12 min 4.3 km) Plaza", "Aceptar \$44.90"))
        assertEquals("sin_separar", Parser.eleccionTarjeta())
    }
}
