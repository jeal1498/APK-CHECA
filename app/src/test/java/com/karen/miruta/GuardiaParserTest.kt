package com.karen.miruta

import org.junit.Assert.assertEquals
import org.junit.Test
import java.io.File
import java.security.MessageDigest

/** E3: en el release de trazabilidad el lector no cambia (huellas en herramientas/parser.sha256). */
class GuardiaParserTest {
    @Test fun lectorSinCambios() {
        val raiz = listOf(File(".."), File(".")).first { File(it, "herramientas/parser.sha256").isFile }
        for (l in File(raiz, "herramientas/parser.sha256").readLines().filter { it.isNotBlank() }) {
            val (h, ruta) = l.trim().split(Regex("\\s+"))
            val real = MessageDigest.getInstance("SHA-256").digest(File(raiz, ruta).readBytes()).joinToString("") { "%02x".format(it) }
            assertEquals("cambió $ruta (los cambios del lector van en un release aparte)", h, real)
        }
    }
}
