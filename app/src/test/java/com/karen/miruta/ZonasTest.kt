package com.karen.miruta

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Destino leído de la oferta y zonas bloqueadas (10.60). */
class ZonasTest {
    @Test
    fun destinoDidi() {
        val l = listOf(
            "$134.42", "7 punto(s)", "4.95 • 463 viajes", "Perfil avanzado",
            "3min (835m)", "Residencial Torrecillas 1 - Avenida Cancún,", "Residencial Torrecillas 1, Benito Juárez",
            "35min (11.9km)", "Ligia Arceo Mendoza 13, 13, Benito Juárez", "Aceptar $134.42"
        )
        assertEquals("Ligia Arceo Mendoza 13, 13, Benito Juárez", Parser.destino(l))
    }

    @Test
    fun destinoUberEnDosLineas() {
        val l = listOf(
            "Uber Priority", "$78.57", "$9.70/km est.", "+$9.75 por inicio de viaje prioritario",
            "A 3 min (1.1 km)", "Calle Josefa Ortiz de Domínguez, Paseos Kabah",
            "Viaje: 15 min (6.9 km)", "c. tepich 506, 5, 77533 Cancún, Quintana", "Roo - IMSS 510", "Aceptar"
        )
        assertEquals("c. tepich 506, 5, 77533 Cancún, Quintana Roo - IMSS 510", Parser.destino(l))
    }

    @Test
    fun sinDestinoVisible() {
        assertNull(Parser.destino(listOf("$48.73", "A 5 min (1.6 km)", "Viaje: 8 min (3.2 km)", "Aceptar")))
        assertNull(Parser.destino(listOf("$48.73")))
    }

    @Test
    fun zonaContienePuntos() {
        val z = Zonas.leer("21.0,-87.0 21.0,-86.0 22.0,-86.0 22.0,-87.0")
        assertEquals(1, z.size)
        assertTrue(Zonas.contiene(z[0], 21.5, -86.5))
        assertFalse(Zonas.contiene(z[0], 23.0, -86.5))
        assertEquals(1, Zonas.cual(z, 21.5, -86.5))
        assertEquals(0, Zonas.cual(z, 20.0, -86.5))
    }

    @Test
    fun guardarYLeer() {
        val texto = "21.000000,-87.000000 21.000000,-86.000000 22.000000,-86.000000;10.000000,10.000000 10.000000,11.000000 11.000000,11.000000"
        assertEquals(texto, Zonas.escribir(Zonas.leer(texto)))
        assertTrue(Zonas.leer("").isEmpty())
    }

    @Test
    fun consultasSinNumerosSueltos() {
        val q = Zonas.consultas("Ligia Arceo Mendoza 13, 13, Benito Juárez")
        assertEquals(2, q.size)
        assertEquals("Ligia Arceo Mendoza Benito Juárez", q[1])
    }
}
