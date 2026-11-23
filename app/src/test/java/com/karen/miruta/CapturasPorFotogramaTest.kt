package com.karen.miruta

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** 10.86: la captura que sube el teléfono es la del fotograma de la lectura, nunca la «más reciente». */
class CapturasPorFotogramaTest {
    private fun b(n: Int) = ByteArray(4) { n.toByte() }

    @Test fun devuelveLaDeEseFotogramaAunqueHayaOtrasMasNuevas() {
        val c = CapturasPorFotograma()
        c.guardar("f1", b(1), 1_000)
        c.guardar("f2", b(2), 2_000)
        c.guardar("f3", b(3), 3_000)
        assertArrayEquals(b(1), c.tomar("f1", 4_000))   // la lectura f1 sube SU captura, no la de f3
        assertArrayEquals(b(3), c.tomar("f3", 4_000))
    }

    @Test fun sinFotogramaOConOtroNoSubeNada() {
        val c = CapturasPorFotograma()
        c.guardar("f1", b(1), 1_000)
        assertNull(c.tomar(null, 1_500))
        assertNull(c.tomar("otro", 1_500))
        assertEquals("lo que no se pidió se queda", 1, c.tamano())
    }

    @Test fun seTomaUnaSolaVez() {
        val c = CapturasPorFotograma()
        c.guardar("f1", b(1), 1_000)
        assertArrayEquals(b(1), c.tomar("f1", 1_100))
        assertNull(c.tomar("f1", 1_200))
    }

    @Test fun caducaALos30Segundos() {
        val c = CapturasPorFotograma()
        c.guardar("f1", b(1), 1_000)
        assertNull(c.tomar("f1", 1_000 + 30_001))
    }

    @Test fun guardaSoloLasUltimas() {
        val c = CapturasPorFotograma(maximo = 3)
        for (i in 1..5) c.guardar("f$i", b(i), i * 1_000L)
        assertEquals(3, c.tamano())
        assertNull("la más vieja se descarta", c.tomar("f1", 6_000))
        assertArrayEquals(b(5), c.tomar("f5", 6_000))
    }

    @Test fun vaciarLimpiaTodo() {
        val c = CapturasPorFotograma()
        c.guardar("f1", b(1), 1_000)
        c.vaciar()
        assertEquals(0, c.tamano())
    }
}
