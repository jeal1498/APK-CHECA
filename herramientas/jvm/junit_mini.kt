// Sustituto MÍNIMO de JUnit 4 para correr pruebas del app/src/test en el contenedor (sin Gradle ni Maven).
// Solo implementa lo que usan las pruebas de Traza y Guardia. En CI (play-compilar.yml) corre JUnit real.
package org.junit

@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.FUNCTION)
annotation class Test

object Assert {
    @JvmStatic fun fail(m: String?): Nothing = throw AssertionError(m)
    @JvmStatic fun assertTrue(m: String?, c: Boolean) { if (!c) fail(m) }
    @JvmStatic fun assertTrue(c: Boolean) = assertTrue("se esperaba true", c)
    @JvmStatic fun assertFalse(m: String?, c: Boolean) { if (c) fail(m) }
    @JvmStatic fun assertFalse(c: Boolean) = assertFalse("se esperaba false", c)
    @JvmStatic fun assertEquals(m: String?, e: Any?, a: Any?) { if (e != a) fail("${m ?: ""} esperado <$e> pero fue <$a>") }
    @JvmStatic fun assertEquals(e: Any?, a: Any?) = assertEquals(null, e, a)
    @JvmStatic fun assertNull(a: Any?) { if (a != null) fail("se esperaba null y fue <$a>") }
    @JvmStatic fun assertNotNull(a: Any?) { if (a == null) fail("se esperaba no null") }
}
