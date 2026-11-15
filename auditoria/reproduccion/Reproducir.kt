// E5: corre el lector (Parser.analizar) sobre una carpeta de ENTRADAS (solo líneas, sin respuesta esperada)
// y escribe una predicción por caso en JSONL, en orden de nombre. Determinista: no usa hora ni azar.
// Uso: java -cp reproducir.jar ReproducirKt <entradas_dir> <salida.jsonl>
import com.karen.miruta.Parser
import com.karen.miruta.Traza
import java.io.File

fun main(a: Array<String>) {
    val archivos = File(a[0]).listFiles { f -> f.extension == "txt" }!!.sortedBy { it.name }
    val sb = StringBuilder()
    for (f in archivos) {
        val texto = f.readText(Charsets.UTF_8).replace("\r", "")
        require(!texto.contains("\n---\n")) { "${f.name}: la entrada trae respuesta esperada (no es ciega)" }
        val lineas = texto.lines().filter { it.isNotBlank() && !it.startsWith("#") }
        val o = try { Parser.analizar(lineas) } catch (e: Exception) { null }
        val j = Traza.Json().s("caso", f.nameWithoutExtension).s("resultado", if (o == null) "nada" else "oferta")
        if (o != null) {
            j.d("tarifa", o.tarifa, 2).d("km", o.km, 2).d("min", o.min, 1).d("recKm", o.recogidaKm, 2)
                .d("recMin", o.recogidaMin, 1).d("multi", o.multiplicador, 2).d("dinamica", o.dinamica, 2)
        }
        sb.append(j.cerrar()).append('\n')
    }
    File(a[1]).writeText(sb.toString(), Charsets.UTF_8)
    println("${archivos.size} casos → ${a[1]}")
}
