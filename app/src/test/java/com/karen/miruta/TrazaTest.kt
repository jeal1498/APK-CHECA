package com.karen.miruta

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** E3 (10.81): núcleo de trazabilidad. Corre en play-compilar.yml (JUnit) y en el contenedor (herramientas/jvm/probar.sh). */
class TrazaTest {
    private class Memoria(var texto: String? = null) : AlmacenTraza {
        var escrituras = 0
        override fun leer() = texto
        override fun escribir(texto: String) { this.texto = texto; escrituras++ }
    }

    private fun falla(f: () -> Unit): Boolean = try { f(); false } catch (_: IllegalArgumentException) { true }

    private fun oferta() = Oferta(tarifa = 120.5, km = 10.2, min = 25.0, recogidaKm = 1.5, recogidaMin = 5.0, multiplicador = 1.4)

    private fun evento(resultado: String = "oferta", o: Oferta? = oferta(), nivel: String? = "verde", lector: String = "ocr",
                       fot: String? = Traza.nuevoId(), sha: String? = null) = Traza.Evento(
        app = "didi", versionApp = "10.81", lector = lector, fotogramaId = fot, tsFotogramaMs = 1_763_200_800_000,
        tsEventoMs = 1_763_200_801_000, resultado = resultado, oferta = o, nivel = nivel, moneda = "MXN", tarjetas = 1,
        msLectura = 420, msPopup = 650, lineasSha256 = Traza.huellaLineas(listOf("$120.50")), lineasRedactadas = listOf("$120.50"),
        capturaSha256 = sha, etapas = listOf(Traza.Etapa("captura", "ok", 30), Traza.Etapa("parser", "ok", 2)))

    @Test fun etapasOcrYParserLlevanEstadoMsYMotivo() {
        val e = Traza.Evento(
            app = "didi", versionApp = "10.86", lector = "ocr", fotogramaId = Traza.nuevoId(), tsFotogramaMs = 1_763_200_800_000,
            tsEventoMs = 1_763_200_801_000, resultado = "oferta", oferta = oferta(), nivel = "verde", moneda = "MXN",
            etapas = listOf(Traza.Etapa("ocr", "ok", 410), Traza.Etapa("parser", "ok", 3, "elegida")))
        val j = e.aJson()
        assertTrue(j, j.contains("\"etapas\":[{\"etapa\":\"ocr\",\"estado\":\"ok\",\"ms\":410},{\"etapa\":\"parser\",\"estado\":\"ok\",\"ms\":3,\"motivo\":\"elegida\"}]"))
        // Los motivos que deja el Parser cumplen la gramática del servidor (a-z, 0-9 y «_»).
        for (m in listOf("sin_separar", "elegida", "ambigua")) Traza.Etapa("parser", "ok", 1, m)
    }

    @Test fun ofertaValidaSerializa() {
        val j = evento().aJson()
        assertTrue(j, j.startsWith("{\"id\":\"") && j.endsWith("}"))
        for (p in listOf("\"contrato\":1", "\"app\":\"didi\"", "\"resultado\":\"oferta\"", "\"tarifa\":120.5", "\"km\":10.2",
                         "\"min\":25", "\"multi\":1.4", "\"nivel\":\"verde\"", "\"captura_vinculo\":\"sin_captura\"",
                         "\"version_parser\":\"${Traza.VERSION_PARSER}\"", "\"ts_evento\":\"2025-11-15T10:00:01.000Z\"")) {
            assertTrue("falta $p en $j", j.contains(p))
        }
        assertFalse("no debe haber nulos", j.contains("null"))
    }

    @Test fun coherenciaComoElServidor() {
        assertTrue("oferta sin semáforo", falla { evento(nivel = null) })
        assertTrue("semáforo sin oferta", falla { evento(resultado = "no_oferta", o = null) })
        assertTrue("varias con datos de una tarjeta", falla { evento(resultado = "varias", nivel = null) })
        assertTrue("ocr sin fotograma", falla { evento(fot = null) })
        assertTrue("captura sin fotograma", falla { evento(lector = "texto", fot = null, sha = "a".repeat(64)) })
        assertTrue("km 0", falla { evento(o = oferta().copy(km = 0.0)) })
        assertTrue("resultado raro", falla { evento(resultado = "quizas", o = null, nivel = null) })
        assertFalse("varias sin datos sí", falla { evento(resultado = "varias", o = null, nivel = null) })
        assertFalse("abstención sí", falla { evento(resultado = "abstencion", o = null, nivel = null) })
    }

    @Test fun capturaExactaMarcaVinculo() {
        val j = evento(sha = Traza.sha256("x".toByteArray())).aJson()
        assertTrue(j.contains("\"captura_vinculo\":\"exacto\""))
        assertTrue(j.contains("\"captura_sha256\":\"2d711642b726b04401627ca9fbac32f5c8530fb1903cc4db02258717921a4881\""))
    }

    @Test fun jsonEscapaTexto() {
        assertEquals("\"a\\\"b\\\\c\\nd\\u0001\"", Traza.Json.cadena("a\"b\\c\nd\u0001"))
    }

    @Test fun redaccionSoloDatosDeOferta() {
        val casos = listOf(
            "$151.34" to "$151.34", "5min (1.7km)" to "5min ( 1.7km )", "37min (13.2km)" to "37min ( 13.2km )",
            "⚡1.4x" to "⚡ 1.4x", "$10.15/km est." to "$10.15 /km est", "+$9.75 por inicio" to "+$9.75 por inicio",
            "A 5 min (1.6 km)" to "A 5 min ( 1.6 km )", "Viaje: 8 min (3.2 km)" to "Viaje : 8 min ( 3.2 km )",
            // DiDi: la dirección viene en la misma línea que el tramo (hallazgo de la auditoría de cierre).
            "(4 min 1.4 km) Ana Spa," to "( 4 min 1.4 km )",
            "o (6 min 1.8 km) Calle 46 612," to "( 6 min 1.8 km )",
            "(9 min 3 km) Cafe La Cabaña La Venta, Oropeza, Centro" to "( 9 min 3 km )",
            "0.9 km Heroles" to "0.9 km",
            "(9 mın 2.3 Km)" to "( 2.3 Km )",   // «ı» sin punto del OCR: esa pieza no pasa (el servidor no la aceptaría)
            "Tel 55 1234 5678 $50" to "$50", "juan@correo.com $10" to "$10",
            "Av. Insurgentes Sur 1602, Col. Crédito Constructor" to null, "Calle 46 612," to null,
            "Juan Pérez ★4.92" to null, "https://x.y/z" to null, "Aceptar" to null, "" to null, "5 de Mayo 233" to null)
        for ((entrada, esperado) in casos) assertEquals(entrada, esperado, Redaccion.redactarLinea(entrada))
        assertEquals(30, Redaccion.redactar(List(40) { "$$it.00" }).size)
        assertTrue(Redaccion.redactarLinea("$" + "1".repeat(70))!!.length <= 60)
    }

    @Test fun redaccionNoDejaPalabrasNiNumerosSueltos() {
        val permitidas = Redaccion.PALABRAS.split("|").toSet() + "x"
        val sucias = listOf("(11 min 5.1 km) Antojitos Doña Mary 24", "(20 min 5.5 km) Juan Aldama 4, Nueva", "$65 adicionales por realizar 13 solicitud ...",
            "3 min EO 0.9 km", "lh8 min 21.7 km", "R Haz 4 viajes para obtener $65 adicionales", "(16 min 8.9 km) ISSSTE Hospital General")
        for (l in sucias) {
            val r = Redaccion.redactarLinea(l) ?: continue
            for (w in Regex("[A-Za-zÁÉÍÓÚÑáéíóúñ]+").findAll(r)) assertTrue("«${w.value}» en «$r»", w.value.lowercase() in permitidas)
            val sinPiezas = r.replace(Regex("""[+-]?[$€£] ?\d[\d.,]*|\d+(?:[.,]\d+)? ?(?:min|km|m|h|x)""", RegexOption.IGNORE_CASE)) { "" }
            assertFalse("número suelto en «$r»", sinPiezas.any(Char::isDigit))
        }
    }

    @Test fun multiplicadorFueraDeRangoNoSeManda() {
        assertFalse(evento(o = oferta().copy(multiplicador = 25.0)).aJson().contains("\"multi\""))
        assertTrue(evento(o = oferta().copy(multiplicador = 19.9)).aJson().contains("\"multi\":19.9"))
    }

    @Test fun huellaDeLineasEsEstable() {
        assertEquals(Traza.huellaLineas(listOf("a", "b")), Traza.huellaLineas(listOf("a", "b")))
        assertFalse(Traza.huellaLineas(listOf("a", "b")) == Traza.huellaLineas(listOf("ab")))
    }

    @Test fun colaPersisteYSobreviveReinicio() {
        val m = Memoria()
        val c = ColaTraza(m)
        val e1 = evento(); val e2 = evento()
        c.agregar(e1.id, e1.aJson()); c.agregar(e2.id, e2.aJson())
        val c2 = ColaTraza(m)  // «reinicio del teléfono»
        assertEquals(2, c2.tamano())
        assertEquals(listOf(e1.id, e2.id), c2.lote().map { it.first })
        c2.quitar(listOf(e1.id))
        assertEquals(listOf(e2.id), ColaTraza(m).lote().map { it.first })
    }

    @Test fun colaAcotadaDescartaLosMasViejos() {
        val c = ColaTraza(Memoria(), maxEventos = 3)
        val ids = (1..5).map { evento().also { e -> c.agregar(e.id, e.aJson()) }.id }
        assertEquals(3, c.tamano())
        assertEquals(2L, c.descartados)
        assertEquals(ids.takeLast(3), c.lote().map { it.first })
    }

    @Test fun colaToleraArchivoDanado() {
        val c = ColaTraza(Memoria("basura\n\u0000\u0000\n{\"a\":1}\nxxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx\tsin-json\n"))
        assertEquals(0, c.tamano())
        val e = evento()
        assertTrue(c.agregar(e.id, e.aJson()))
    }

    @Test fun colaRechazaEventoRaro() {
        val c = ColaTraza(Memoria())
        assertFalse(c.agregar("corto", "{}"))
        assertFalse(c.agregar(Traza.nuevoId(), "{\n}"))
        assertEquals(2L, c.descartados)
    }

    @Test fun loteRespetaTopes() {
        val c = ColaTraza(Memoria(), maxEventos = 200)
        repeat(120) { val e = evento(); c.agregar(e.id, e.aJson()) }
        val l = c.lote()
        assertEquals(50, l.size)
        assertTrue(Traza.lote(l.map { it.second }).length < 64 * 1024)
        assertTrue(falla { Traza.lote(emptyList()) })
    }

    @Test fun reintentoCreceYTieneTope() {
        assertEquals(15_000L, Reintento.espera(0, 0.0))
        assertEquals(30_000L, Reintento.espera(0, 1.0))
        assertEquals(60_000L, Reintento.espera(1, 1.0))
        assertEquals(Reintento.MAX_MS, Reintento.espera(50, 1.0))
        assertTrue(Reintento.espera(3, 0.5) in 180_000L..240_000L)
    }

    @Test fun respuestasDelServidor() {
        val a = "11111111-1111-4111-8111-111111111111"; val b = "22222222-2222-4222-8222-222222222222"
        val ok = "{\"ok\":true,\"resultados\":[{\"id\":\"$a\",\"estado\":\"guardado\"},{\"id\":\"$b\",\"estado\":\"invalido\",\"error\":\"x\"}]}"
        assertEquals(RespuestaTraza.Accion.Quitar(setOf(a, b)), RespuestaTraza.decidir(200, ok, listOf(a, b)))
        // Un id que no viene en la respuesta se queda en la cola.
        val parcial = "{\"ok\":true,\"resultados\":[{\"id\":\"$a\",\"estado\":\"repetido\"}]}"
        assertEquals(RespuestaTraza.Accion.Quitar(setOf(a)), RespuestaTraza.decidir(200, parcial, listOf(a, b)))
        assertEquals(RespuestaTraza.Accion.Reintentar, RespuestaTraza.decidir(null, null, listOf(a)))
        assertEquals(RespuestaTraza.Accion.Reintentar, RespuestaTraza.decidir(503, "{\"reintentar\":true}", listOf(a)))
        assertEquals(RespuestaTraza.Accion.Esperar, RespuestaTraza.decidir(503, "{\"ok\":false,\"reintentar\":false}", listOf(a)))
        assertEquals(RespuestaTraza.Accion.Esperar, RespuestaTraza.decidir(401, "", listOf(a)))
        assertEquals(RespuestaTraza.Accion.Descartar(setOf(a)), RespuestaTraza.decidir(400, "", listOf(a)))
        assertEquals(RespuestaTraza.Accion.Reintentar, RespuestaTraza.decidir(200, "{\"ok\":false}", listOf(a)))
    }

    @Test fun appCorta() {
        assertEquals("uber", Traza.appCorta("Uber")); assertEquals("didi", Traza.appCorta("Di-Di")); assertEquals("otra", Traza.appCorta(null))
    }
}
