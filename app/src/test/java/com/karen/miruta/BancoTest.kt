package com.karen.miruta

import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * Califica el lector contra el banco de ofertas reales ([Banco]). No exige 100 %: exige no
 * bajar de `banco/minimo.txt`. Cuando un arreglo sube los aciertos, se sube ese número y se
 * anota la versión en `docs/banco-lector.md`.
 */
class BancoTest {
    @Test
    fun elLectorNoEmpeora() {
        val carpeta = File(javaClass.classLoader!!.getResource("banco")!!.toURI())
        val minimo = File(carpeta, "minimo.txt").readText().trim().toInt()
        val r = Banco.calificar(carpeta)
        println("BANCO DEL LECTOR: ${r.aciertos} de ${r.total} (${"%.1f".format(r.porciento)} %), mínimo $minimo")
        assertTrue(
            "El lector bajó en el banco: ${r.aciertos} de ${r.total} (mínimo $minimo).\n" + r.fallas.joinToString("\n"),
            r.aciertos >= minimo
        )
    }
}
