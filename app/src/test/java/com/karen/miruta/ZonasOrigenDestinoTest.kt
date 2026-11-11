package com.karen.miruta

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/** Bloqueo de zonas por origen y destino (10.61): lector, motor geográfico, recomendación y turnos. */
class ZonasOrigenDestinoTest {
    private val didi = listOf(
        "$134.42", "7 punto(s)", "4.95 • 463 viajes", "Perfil avanzado",
        "3min (835m)", "Residencial Torrecillas 1 - Avenida Cancún,", "Residencial Torrecillas 1, Benito Juárez",
        "35min (11.9km)", "Ligia Arceo Mendoza 13, 13, Benito Juárez", "Aceptar $134.42"
    )
    private val uber = listOf(
        "Uber Priority", "$78.57", "$9.70/km est.", "+$9.75 por inicio de viaje prioritario",
        "A 3 min (1.1 km)", "Calle Josefa Ortiz de Domínguez, Paseos Kabah",
        "Viaje: 15 min (6.9 km)", "c. tepich 506, 5, 77533 Cancún, Quintana", "Roo - IMSS 510", "Aceptar"
    )
    private val uberNuevo = listOf(
        "UberX", "Exclusivo", "$103.13", "50 min · 16.4 km · $6.29/km est.", "6 min · 2.3 km",
        "Andador León Guzmán - Iztacalco", "Identidad verificada", "4.74 (1307)",
        "44 min · 14.1 km", "Masaryk, Col Polanco IV Sección", "Aceptar"
    )
    private val radar = listOf(
        "$108.99", "UberX", "$66.21", "33 min · 10.6 km · $6.25/km est.", "9 min · 3.0 km",
        "Calle Peyote - Iztacalco", "24 min · 7.6 km", "Calle Luis Braile 191", "Me interesa"
    )

    // Cuadro de 1° alrededor de (21.5, -86.5) y otro lejos.
    private val zonas = Zonas.leer("Centro|21.0,-87.0 21.0,-86.0 22.0,-86.0 22.0,-87.0;Hotelera|10.0,10.0 10.0,11.0 11.0,11.0 11.0,10.0")
    private val adentro = Zonas.Geo.Hallado(21.5, -86.5)
    private val enHotelera = Zonas.Geo.Hallado(10.5, 10.5)
    private val afuera = Zonas.Geo.Hallado(30.0, 30.0)
    private val base = Oferta(100.0, 10.0, 20.0, 2.0, 5.0)

    /** Prefs sin SharedPreferences: solo sirve donde la zona decide antes de leer un ajuste. */
    private fun prefsSinDatos(): Prefs {
        val f = Class.forName("sun.misc.Unsafe").getDeclaredField("theUnsafe").apply { isAccessible = true }
        val u = f.get(null)
        return u.javaClass.getMethod("allocateInstance", Class::class.java).invoke(u, Prefs::class.java) as Prefs
    }

    @Before fun limpiar() = Zonas.limpiarCache()

    // ---------------- Lector ----------------

    @Test fun origenYDestinoUber() {
        assertEquals("Calle Josefa Ortiz de Domínguez, Paseos Kabah", Parser.origen(uber))
        assertEquals("c. tepich 506, 5, 77533 Cancún, Quintana Roo - IMSS 510", Parser.destino(uber))
    }

    @Test fun origenYDestinoUberNuevo() {
        assertEquals("Andador León Guzmán - Iztacalco", Parser.origen(uberNuevo))
        assertEquals("Masaryk, Col Polanco IV Sección", Parser.destino(uberNuevo))
    }

    @Test fun origenYDestinoDidiEnVariasLineas() {
        assertEquals("Residencial Torrecillas 1 - Avenida Cancún, Residencial Torrecillas 1, Benito Juárez", Parser.origen(didi))
        assertEquals("Ligia Arceo Mendoza 13, 13, Benito Juárez", Parser.destino(didi))
    }

    @Test fun botonesNoSonDireccion() {
        assertEquals("Calle Peyote - Iztacalco", Parser.origen(radar))
        assertEquals("Calle Luis Braile 191", Parser.destino(radar))
    }

    @Test fun sinDireccionesEsNull() {
        val sinCalles = listOf("$48.73", "$10.15/km est.", "A 5 min (1.6 km)", "Viaje: 8 min (3.2 km)", "Aceptar")
        assertNull(Parser.origen(sinCalles))
        assertNull(Parser.destino(sinCalles))
        // Un solo tramo (el otro tapado): no se sabe si es recogida o viaje.
        val unTramo = listOf("$48.73", "Viaje: 8 min (3.2 km)", "Calle Falsa 123")
        assertNull(Parser.origen(unTramo))
        assertNull(Parser.destino(unTramo))
        assertNull(Parser.origen(emptyList()))
    }

    @Test fun elResumenYNuestroPopupNoCuentanComoTramo() {
        val l = listOf("$90.00", "24 min (6.0 km) total", "Calle Popup 1", "5min (1.7km)", "Origen Real 5", "19min (4.3km)", "Destino Real 9")
        assertEquals("Origen Real 5", Parser.origen(l))
        assertEquals("Destino Real 9", Parser.destino(l))
    }

    @Test fun cadaTarjetaConSusDirecciones() {
        assertFalse(Parser.destino(didi) == Parser.destino(uber))
        assertFalse(Parser.origen(uberNuevo) == Parser.origen(radar))
    }

    @Test fun lasDireccionesNoCambianTarifaKmNiMin() {
        val o = Parser.analizar(uberNuevo)!!
        assertEquals(103.13, o.tarifa, 0.001)
        assertEquals(16.4, o.km, 0.01)
        assertEquals(50.0, o.min, 0.01)
        val d = Parser.analizar(didi)!!
        assertEquals(134.42, d.tarifa, 0.001)
        assertEquals(12.735, d.km, 0.01)
        assertEquals(38.0, d.min, 0.01)
        assertFalse(d.bloqueada)
    }

    // ---------------- Motor geográfico ----------------

    @Test fun puntoDentroFueraYBorde() {
        assertTrue(Zonas.contiene(zonas[0], 21.5, -86.5))
        assertFalse(Zonas.contiene(zonas[0], 23.0, -86.5))
        assertTrue(Zonas.contiene(zonas[0], 21.0, -86.5))   // lado de abajo
        assertTrue(Zonas.contiene(zonas[0], 22.0, -87.0))   // esquina
    }

    @Test fun variosPoligonosYNombres() {
        assertEquals(1, Zonas.cual(zonas, 21.5, -86.5))
        assertEquals(2, Zonas.cual(zonas, 10.5, 10.5))
        assertEquals(0, Zonas.cual(zonas, 0.0, 0.0))
        assertEquals("Centro", Zonas.nombre(zonas, 0))
        assertEquals("Zona 1", Zonas.nombre(Zonas.leer("21.0,-87.0 21.0,-86.0 22.0,-86.0"), 0))
    }

    @Test fun guardarConNombre() {
        val t = Zonas.escribir(zonas)
        assertTrue(t.startsWith("Centro|"))
        assertEquals(t, Zonas.escribir(Zonas.leer(t)))
        assertEquals("Mi zona", Zonas.limpiarNombre("Mi;zona|").replace(Regex("\\s+"), " ").trim())
    }

    @Test fun estados() {
        val o = Zonas.evaluar(Zonas.Tipo.ORIGEN, "Calle 1", zonas) { adentro }
        assertEquals(Zonas.Estado.BLOQUEADA, o.estado)
        assertEquals("Centro", o.zona)
        assertEquals(21.5, o.lat!!, 1e-9)
        assertEquals(Zonas.Estado.LIBRE, Zonas.evaluar(Zonas.Tipo.DESTINO, "Calle 2", zonas) { afuera }.estado)
        assertEquals(Zonas.Estado.SIN_DIRECCION, Zonas.evaluar(Zonas.Tipo.DESTINO, null, zonas) { adentro }.estado)
        assertEquals(Zonas.Estado.SIN_COORDENADAS, Zonas.evaluar(Zonas.Tipo.DESTINO, "X", zonas) { Zonas.Geo.NoHallado }.estado)
        val red = Zonas.evaluar(Zonas.Tipo.DESTINO, "X", zonas) { Zonas.Geo.ErrorRed }
        assertEquals(Zonas.Estado.ERROR_RED, red.estado)
        assertFalse(red.bloqueada)
    }

    @Test fun cacheEvitaConsultasRepetidas() {
        var n = 0
        val buscar = { _: String -> n++; adentro }
        assertTrue(Zonas.ubicarCon("Calle  Uno 5", buscar) is Zonas.Geo.Hallado)
        assertTrue(Zonas.ubicarCon("calle uno 5", buscar) is Zonas.Geo.Hallado)
        assertEquals(1, n)
    }

    @Test fun errorDeRedNoSeGuarda() {
        var n = 0
        assertTrue(Zonas.ubicarCon("Calle Dos", { _ -> n++; Zonas.Geo.ErrorRed }) is Zonas.Geo.ErrorRed)
        assertTrue(Zonas.ubicarCon("Calle Dos", { _ -> n++; adentro }) is Zonas.Geo.Hallado)
        assertEquals(2, n)
    }

    @Test fun noHalladoSeRecuerda30Min() {
        var n = 0
        val nada = { _: String -> n++; Zonas.Geo.NoHallado }
        Zonas.ubicarCon("Lugar 7", nada, ahora = 0L)
        val antes = n
        assertTrue(Zonas.ubicarCon("Lugar 7", nada, ahora = 60_000L) is Zonas.Geo.NoHallado)
        assertEquals(antes, n)
        Zonas.ubicarCon("Lugar 7", nada, ahora = 31 * 60_000L)
        assertTrue(n > antes)
    }

    // ---------------- Recomendación ----------------

    private fun res(tipo: Zonas.Tipo, g: Zonas.Geo) = Zonas.evaluar(tipo, "Calle", zonas) { g }

    @Test fun origenBloqueadoEsRojo() {
        val o = Zonas.marcar(base, res(Zonas.Tipo.ORIGEN, adentro), res(Zonas.Tipo.DESTINO, afuera))
        assertEquals("Origen en zona bloqueada · Centro", o.motivoZona())
        assertEquals(Oferta.ROJO, o.nivel(prefsSinDatos()))
    }

    @Test fun destinoBloqueadoEsRojo() {
        val o = Zonas.marcar(base, res(Zonas.Tipo.ORIGEN, afuera), res(Zonas.Tipo.DESTINO, enHotelera))
        assertEquals("Destino en zona bloqueada · Hotelera", o.motivoFiltro(prefsSinDatos()))
        assertEquals(Oferta.ROJO, o.nivel(prefsSinDatos()))
    }

    @Test fun ambosBloqueadosEsRojoConLosDos() {
        val o = Zonas.marcar(base, res(Zonas.Tipo.ORIGEN, adentro), res(Zonas.Tipo.DESTINO, enHotelera))
        assertEquals("Origen y destino bloqueados · Centro / Hotelera", o.motivoZona())
        assertEquals(Oferta.ROJO, o.nivel(prefsSinDatos()))
    }

    @Test fun sinBloqueoLaOfertaNoCambia() {
        val libre = Zonas.marcar(base, res(Zonas.Tipo.ORIGEN, afuera), res(Zonas.Tipo.DESTINO, afuera))
        assertEquals(base, libre)
        assertNull(libre.motivoZona())
        // Ubicación desconocida o sin red: nada de falso bloqueo.
        val desconocida = Zonas.marcar(base, res(Zonas.Tipo.ORIGEN, Zonas.Geo.NoHallado), res(Zonas.Tipo.DESTINO, Zonas.Geo.ErrorRed))
        assertEquals(base, desconocida)
    }

    @Test fun lasFormulasNoCambian() {
        val o = Zonas.marcar(base, res(Zonas.Tipo.ORIGEN, adentro), res(Zonas.Tipo.DESTINO, adentro))
        assertEquals(base.porKm, o.porKm, 1e-9)
        assertEquals(base.porHora, o.porHora, 1e-9)
        assertEquals(base, o.sinZonas())
    }

    @Test fun laCorreccionConservaLasZonas() {
        val marcada = Zonas.marcar(base, res(Zonas.Tipo.ORIGEN, adentro), res(Zonas.Tipo.DESTINO, afuera))
        val corregida = Oferta(110.0, 11.0, 22.0, 2.0, 5.0)
        val final = Zonas.conservar(marcada, corregida)
        assertEquals(110.0, final.tarifa, 1e-9)
        assertEquals("Centro", final.zonaOrigen)
        assertEquals("", final.zonaDestino)
        // Sin marcas no se inventa ninguna.
        assertFalse(Zonas.conservar(base, corregida).bloqueada)
    }

    // ---------------- Turnos (resultados que llegan tarde) ----------------

    @Test fun laOfertaATerminaDespuesDeLaB() {
        val t = Zonas.Turnos()
        val a = t.nuevo("Uber")
        val b = t.nuevo("Uber")
        assertFalse(t.vigente("Uber", a))
        assertTrue(t.vigente("Uber", b))
    }

    @Test fun laOfertaDesaparece() {
        val t = Zonas.Turnos()
        val a = t.nuevo("DiDi")
        t.quitar("DiDi")
        assertFalse(t.vigente("DiDi", a))
        assertNull(t.actual("DiDi"))
    }

    @Test fun dosAppsNoSeMezclan() {
        val t = Zonas.Turnos()
        val u = t.nuevo("Uber")
        val d = t.nuevo("DiDi")
        assertTrue(t.vigente("Uber", u))
        assertTrue(t.vigente("DiDi", d))
        assertFalse(t.vigente("DiDi", u))
        assertNotNull(t.actual("Uber"))
    }

    @Test fun laMismaMarcaNoSeAplicaDosVeces() {
        val ro = res(Zonas.Tipo.ORIGEN, adentro)
        val rd = res(Zonas.Tipo.DESTINO, afuera)
        val una = Zonas.marcar(base, ro, rd)
        // RutaService no vuelve a avisar si la marca no cambia nada.
        assertEquals(una, Zonas.marcar(una, ro, rd))
    }
}
