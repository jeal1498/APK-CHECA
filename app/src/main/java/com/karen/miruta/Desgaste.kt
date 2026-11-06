package com.karen.miruta

/**
 * Desgaste del auto por conceptos (10.12). Cada concepto es opcional (vacío = 0):
 * - por km: «costo» entre «cada cuántos km» (llantas $6,000 cada 50,000 km = $0.12/km);
 * - al año: lo que cuesta al año entre los km que se hacen al año (seguro $9,600 / 40,000 km = $0.24/km).
 * La suma es el desgaste por km que se resta en la ganancia neta. Si no se llena ningún concepto,
 * se usa el monto directo de siempre ([Prefs.desgasteDirecto]).
 */
object Desgaste {
    class Concepto(val id: String, val nombre: String, val porKm: Boolean, val enMes: Boolean = false)

    val POR_KM = listOf(
        Concepto("lla", "Llantas", true),
        Concepto("ace", "Aceite y filtros", true),
        Concepto("fre", "Frenos", true),
        Concepto("ser", "Servicio y reparaciones", true),
        Concepto("bat", "Batería", true)
    )

    /** [enMes]: se captura por defecto «al mes» (se guarda siempre al año). */
    val AL_ANIO = listOf(
        Concepto("seg", "Seguro del auto", false),
        Concepto("lic", "Licencia y tarjeta", false),
        Concepto("ten", "Tenencia y refrendo", false),
        Concepto("ver", "Verificación", false),
        Concepto("dat", "Datos del celular", false, true),
        Concepto("lav", "Lavado", false, true),
        Concepto("dep", "Depreciación del auto", false),
        Concepto("men", "Mensualidad del auto", false, true)
    )

    const val KM_ANIO_DEF = 40000f
    const val CLAVE_KM_ANIO = "kmAnio"

    fun claveCosto(c: Concepto) = "d_${c.id}_c"
    fun claveCada(c: Concepto) = "d_${c.id}_k"
    fun claveAnual(c: Concepto) = "d_${c.id}_a"

    /** Claves que viajan con la cuenta. */
    val CLAVES: List<String> =
        POR_KM.flatMap { listOf(claveCosto(it), claveCada(it)) } + AL_ANIO.map { claveAnual(it) } + CLAVE_KM_ANIO

    fun kmAnio(p: Prefs): Float = p.flotante(CLAVE_KM_ANIO, KM_ANIO_DEF).let { if (it > 0) it else KM_ANIO_DEF }

    /** $/km de un concepto (0 si falta algún dato). */
    fun costoKm(p: Prefs, c: Concepto): Double {
        if (c.porKm) {
            val costo = p.flotante(claveCosto(c))
            val cada = p.flotante(claveCada(c))
            return if (costo > 0 && cada > 0) costo.toDouble() / cada else 0.0
        }
        val anual = p.flotante(claveAnual(c))
        return if (anual > 0) anual.toDouble() / kmAnio(p) else 0.0
    }

    fun subtotalKm(p: Prefs): Double = POR_KM.sumOf { costoKm(p, it) }
    fun subtotalAnio(p: Prefs): Double = AL_ANIO.sumOf { costoKm(p, it) }

    /** Suma de todos los conceptos, en $/km. */
    fun total(p: Prefs): Double = subtotalKm(p) + subtotalAnio(p)
}
