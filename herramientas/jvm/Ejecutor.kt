// Corre los métodos @org.junit.Test de las clases indicadas. Sale con 1 si alguno falla.
fun main(args: Array<String>) {
    var ok = 0; var mal = 0
    for (nombre in args) {
        val c = Class.forName(nombre)
        for (m in c.methods.filter { it.isAnnotationPresent(org.junit.Test::class.java) }.sortedBy { it.name }) {
            try { m.invoke(c.getDeclaredConstructor().newInstance()); ok++; println("ok   ${c.simpleName}.${m.name}") }
            catch (e: java.lang.reflect.InvocationTargetException) { mal++; println("FALLA ${c.simpleName}.${m.name}: ${e.targetException}") }
        }
    }
    println("$ok ok, $mal fallan")
    if (mal > 0) kotlin.system.exitProcess(1)
}
