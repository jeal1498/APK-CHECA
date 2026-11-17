package com.karen.miruta

// Costo en la JVM del contenedor (NO en un teléfono) de armar, redactar y serializar un evento y encolarlo.
fun main() {
    val o = Oferta(tarifa = 151.34, km = 14.9, min = 42.0, recogidaKm = 1.7, recogidaMin = 5.0)
    val lineas = listOf("$151.34", "5min (1.7km)", "37min (13.2km)", "Av. Siempre Viva 742", "⚡1.4x", "Aceptar") + List(14) { "texto $it" }
    class M : AlmacenTraza { var t: String? = null; override fun leer() = t; override fun escribir(texto: String) { t = texto } }
    val cola = ColaTraza(M())
    fun uno() {
        val e = Traza.Evento(app = "didi", versionApp = "10.81", lector = "ocr", fotogramaId = Traza.nuevoId(), tsFotogramaMs = 1L,
            tsEventoMs = 2L, resultado = "oferta", oferta = o, nivel = "verde", lineasSha256 = Traza.huellaLineas(lineas),
            lineasRedactadas = Redaccion.redactar(lineas))
        cola.agregar(e.id, e.aJson())
    }
    repeat(2000) { uno() }            // calentamiento
    val n = 5000; val t0 = System.nanoTime()
    repeat(n) { uno() }
    val us = (System.nanoTime() - t0) / 1000.0 / n
    println("Por evento (armar + redactar + huella + JSON + encolar con cola llena de 500 y reescritura): %.1f µs".format(us))
    val c2 = ColaTraza(M()); val t1 = System.nanoTime()
    repeat(n) {
        val e = Traza.Evento(app = "didi", versionApp = "10.81", lector = "ocr", fotogramaId = Traza.nuevoId(), tsFotogramaMs = 1L,
            tsEventoMs = 2L, resultado = "oferta", oferta = o, nivel = "verde", lineasSha256 = Traza.huellaLineas(lineas),
            lineasRedactadas = Redaccion.redactar(lineas))
        c2.agregar(e.id, e.aJson()); c2.quitar(listOf(e.id))   // con red: la cola se vacía al enviar
    }
    println("Por evento con la cola casi vacía (caso normal con red): %.1f µs".format((System.nanoTime() - t1) / 1000.0 / n))
    println("Tamaño medio de un evento: ${cola.lote(1).first().second.length} bytes; cola: ${cola.tamano()} eventos")
}
