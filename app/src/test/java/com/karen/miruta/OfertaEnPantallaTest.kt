package com.karen.miruta

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** 10.86: quitar el pop-up viejo cuando en su zona se ve otra oferta (caso capturas_ok 1289, 20 nov). */
class OfertaEnPantallaTest {
    // La oferta anterior (#1288): $42.31, 7.9 km, 17 min, recogida 5.5 km en 9 min.
    private val vieja = Oferta(42.31, 7.9, 17.0, 5.5, 9.0)

    @Test fun losMismosTramosSonDeLaOferta() {
        // Se ve su recogida y su viaje.
        assertEquals(true, OfertaEnPantalla.coincide(listOf(5.5 to 9.0, 2.4 to 8.0), vieja))
        // Basta con ver uno (el otro está bajo el pop-up).
        assertEquals(true, OfertaEnPantalla.coincide(listOf(2.4 to 8.0), vieja))
        // También el total (Uber lo pone en un resumen).
        assertEquals(true, OfertaEnPantalla.coincide(listOf(7.9 to 17.0), vieja))
    }

    @Test fun otraOfertaEnLaMismaZona() {
        // Llegó la nueva ($64.90, recogida 1.3 km): sus tramos no son los de la oferta del pop-up.
        assertEquals(false, OfertaEnPantalla.coincide(listOf(1.3 to 4.0, 6.1 to 21.0), vieja))
    }

    @Test fun sinTramosLegiblesNoDecide() {
        assertNull(OfertaEnPantalla.coincide(emptyList(), vieja))
    }

    @Test fun laLecturaOriginalTambienCuenta() {
        // La revisión corrigió los km del pop-up: en pantalla sigue lo que leyó el lector, no se debe quitar.
        val corregida = Oferta(42.31, 8.4, 17.0, 5.5, 9.0)
        val original = Oferta(42.31, 7.9, 17.0, 5.5, 9.0)
        assertEquals(true, OfertaEnPantalla.coincide(listOf(2.4 to 8.0, 5.5 to 9.0), corregida, original))
        assertEquals(false, OfertaEnPantalla.coincide(listOf(1.3 to 4.0), corregida, original))
    }

    @Test fun ofertaDeUnSoloTramoSeCompararConSuTotal() {
        val una = Oferta(44.9, 4.3, 12.0, 0.0, 0.0)
        assertEquals(true, OfertaEnPantalla.coincide(listOf(4.3 to 12.0), una))
        assertEquals(false, OfertaEnPantalla.coincide(listOf(9.1 to 19.0), una))
    }

    @Test fun losTramosSeLeenConLasCorreccionesDelOcr() {
        assertEquals(5.5 to 9.0, Parser.tramoDe("(9 min 5.5 km) Calle Allende 12"))
        assertEquals(2.1 to 5.0, Parser.tramoDe("5 min (2:1 km)"))          // punto leído como dos puntos
        assertEquals(0.972 to 4.0, Parser.tramoDe("4 min (972 m)"))         // metros
        assertEquals(0.78 to 4.0, Parser.tramoDe("74min (780 m)"))          // ícono 7 pegado a los minutos
        assertNull(Parser.tramoDe("Aceptar \$44.90"))
        assertNull(Parser.tramoDe("Pon Tu Precio"))
    }

    @Test fun elPopupViejoYLaOfertaNuevaDelCasoReal() {
        // Lo que ve el OCR en la zona (sin lo que tapa el pop-up): la oferta nueva.
        val visibles = listOf("A 5 min (1.3 km)", "Viaje: 21 min (6.1 km)").mapNotNull { Parser.tramoDe(it) }
        assertFalse("la oferta nueva no es la del pop-up", OfertaEnPantalla.coincide(visibles, vieja) == true)
        assertEquals(false, OfertaEnPantalla.coincide(visibles, vieja))
        assertTrue(OfertaEnPantalla.coincide(visibles, Oferta(64.90, 7.4, 26.0, 1.3, 5.0)) == true)
    }
}
