package com.karen.miruta

import java.io.File

/**
 * Banco de ofertas reales con respuesta confirmada (`app/src/test/resources/banco/`). Sirve
 * para medir el lector: cada versión se califica contra los mismos casos y sale un número
 * («acierta 152 de 222»). A diferencia de `ofertas/`, aquí hay casos que el lector todavía
 * falla: el banco no rompe la compilación, solo no puede bajar del mínimo de `minimo.txt`.
 *
 * Formato igual que `ofertas/`: líneas del lector, «---» y lo esperado
 * («tarifa=76.02 km=16.5 min=40» o «nada»). Los comentarios (#) dicen de dónde salió el caso.
 */
object Banco {
    class Caso(val nombre: String, val lineas: List<String>, val esperado: String)
    class Resultado(val total: Int, val fallas: List<String>) {
        val aciertos get() = total - fallas.size
        val porciento get() = if (total == 0) 0.0 else aciertos * 100.0 / total
    }

    /** Tolerancias: las mismas con las que el servidor decide si dos lecturas «coinciden». */
    private val tolerancia = mapOf("tarifa" to 0.99, "km" to 0.15, "min" to 1.1, "recKm" to 0.15, "recMin" to 1.1,
        "multi" to 0.01, "dinamica" to 0.01)

    fun leer(carpeta: File): List<Caso> =
        carpeta.listFiles { f -> f.extension == "txt" && f.name != "minimo.txt" }!!.sortedBy { it.name }.map { f ->
            val partes = f.readText().replace("\r", "").split("\n---\n")
            require(partes.size == 2) { "${f.name}: falta la línea ---" }
            Caso(f.name, partes[0].lines().filter { it.isNotBlank() && !it.startsWith("#") }, partes[1].trim())
        }

    /** Por qué falla el caso, o null si el lector acertó. */
    fun falla(c: Caso): String? {
        val o = Parser.analizar(c.lineas)
        if (c.esperado == "nada") return if (o == null) null else "esperaba nada y leyó ${o.tarifa}, ${o.km} km, ${o.min} min"
        if (o == null) return "no leyó la oferta"
        val leido = mapOf("tarifa" to o.tarifa, "km" to o.km, "min" to o.min, "recKm" to o.recogidaKm, "recMin" to o.recogidaMin,
            "multi" to o.multiplicador, "dinamica" to o.dinamica)
        for (par in c.esperado.split(Regex("\\s+"))) {
            val (k, v) = par.split("=")
            val real = leido[k] ?: return "dato desconocido «$k»"
            if (Math.abs(real - v.toDouble()) > tolerancia.getValue(k)) return "$k esperado $v, leído $real"
        }
        return null
    }

    fun calificar(carpeta: File): Resultado {
        val casos = leer(carpeta)
        return Resultado(casos.size, casos.mapNotNull { c -> falla(c)?.let { "${c.nombre}: $it" } })
    }
}
