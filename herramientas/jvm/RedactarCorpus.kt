package com.karen.miruta

import java.io.File

// Auditoría de privacidad de E3: pasa TODAS las líneas de un corpus (banco/ y ofertas/) por Redaccion.redactar
// y escribe, por caso, las líneas que saldrían del teléfono. Uso: RedactarCorpusKt <dir> [<dir>…] > salida.json
fun main(a: Array<String>) {
    val sb = StringBuilder("[")
    var primero = true
    for (d in a) for (f in File(d).listFiles { x -> x.extension == "txt" && x.name != "minimo.txt" }!!.sortedBy { it.name }) {
        val lineas = f.readText(Charsets.UTF_8).replace("\r", "").split("\n---\n")[0].lines().filter { it.isNotBlank() && !it.startsWith("#") }
        if (!primero) sb.append(",")
        primero = false
        sb.append(Traza.Json().s("caso", f.nameWithoutExtension).n("entran", lineas.size).lista("salen", Redaccion.redactar(lineas)).cerrar())
    }
    println(sb.append("]"))
}
