import com.karen.miruta.Banco
import java.io.File
fun main(a: Array<String>) {
    val r = Banco.calificar(File(a[0]))
    if (a.size > 1) File(a[1]).writeText(r.fallas.joinToString("\n"))
    println("ACIERTOS ${r.aciertos} de ${r.total} (${"%.1f".format(r.porciento)} %)")
}
