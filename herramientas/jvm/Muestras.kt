package com.karen.miruta

// Imprime eventos reales serializados por Traza.kt (para comprobar que el contrato TS y la base los aceptan).
fun main() {
    val o = Oferta(tarifa = 151.34, km = 14.9, min = 42.0, recogidaKm = 1.7, recogidaMin = 5.0, multiplicador = 1.4)
    val lineas = listOf("$151.34", "5min (1.7km)", "37min (13.2km)", "Av. Siempre Viva 742", "⚡1.4x")
    fun ev(r: String, oo: Oferta?, nivel: String?, lector: String = "ocr", sha: String? = null, motivo: String? = null) = Traza.Evento(
        app = "didi", versionApp = "10.81", lector = lector, fotogramaId = if (lector == "texto") null else Traza.nuevoId(),
        tsFotogramaMs = if (lector == "texto") null else 1_763_200_800_000, tsEventoMs = 1_763_200_800_700, resultado = r,
        motivo = motivo, oferta = oo, nivel = nivel, moneda = "MXN", tarjetas = 1, msLectura = 380, msPopup = 610,
        lineasSha256 = Traza.huellaLineas(lineas), lineasRedactadas = Redaccion.redactar(lineas), capturaSha256 = sha,
        telefono = "samsung SM-A155M", android = "14", orientacion = "vertical",
        etapas = listOf(Traza.Etapa("captura", "ok", 25), Traza.Etapa("ocr", "ok", 300), Traza.Etapa("parser", "ok", 3), Traza.Etapa("popup", "ok", 40)))
    val l = listOf(
        ev("oferta", o, "verde"),
        ev("oferta", o, "rojo", sha = Traza.sha256(byteArrayOf(1, 2, 3))),
        ev("oferta", o, "amarillo", lector = "texto"),
        ev("varias", null, null, motivo = "tarjeta_ambigua"),
        ev("abstencion", null, null, motivo = "datos_insuficientes"),
        ev("duplicada", null, null, motivo = "misma_oferta_60s"),
        ev("no_oferta", null, null, lector = "texto"),
    )
    println(l.joinToString(",", "[", "]") { it.aJson() })
}
