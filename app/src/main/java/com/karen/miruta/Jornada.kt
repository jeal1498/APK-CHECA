package com.karen.miruta

import android.content.Context
import java.io.File
import java.util.Calendar

/** Un viaje que el conductor marcó como aceptado. [hora] es la misma de la oferta del historial. */
data class Viaje(val hora: Long, val app: String, val tarifa: Double, val km: Double, val gasolina: Double) {
    val neta: Double get() = tarifa - gasolina
}

data class Totales(val viajes: Int, val tarifa: Double, val gasolina: Double, val km: Double) {
    val neta: Double get() = tarifa - gasolina
    val netaPorKm: Double get() = if (km > 0) neta / km else 0.0
}

/**
 * Viajes aceptados (archivo viajes.txt, solo en el teléfono). Sirven para la meta del día,
 * la gasolina de la semana y el resumen para compartir. Línea: hora|app|tarifa|km|gasolina.
 */
object Jornada {
    private const val ARCHIVO = "viajes.txt"
    private const val DIAS_GUARDADOS = 60

    private fun archivo(ctx: Context) = File(ctx.filesDir, ARCHIVO)

    fun todos(ctx: Context): List<Viaje> {
        val f = archivo(ctx)
        if (!f.exists()) return emptyList()
        return try {
            f.readLines().mapNotNull { l ->
                val p = l.split("|")
                try {
                    if (p.size < 5) null
                    else Viaje(p[0].toLong(), p[1], p[2].toDouble(), p[3].toDouble(), p[4].toDouble())
                } catch (_: Exception) { null }
            }
        } catch (_: Exception) { emptyList() }
    }

    fun registrado(ctx: Context, hora: Long): Boolean = todos(ctx).any { it.hora == hora }

    /** Marca la oferta como aceptada; la gasolina se calcula con los km/l y precio de ahora. */
    fun registrar(ctx: Context, r: Registro, prefs: Prefs) {
        if (registrado(ctx, r.hora)) return
        val gas = if (prefs.kmPorLitro > 0) r.km / prefs.kmPorLitro * prefs.precioLitro else 0.0
        try {
            archivo(ctx).appendText("${r.hora}|${r.app.replace("|", "")}|${r.tarifa}|${r.km}|$gas\n")
            podar(ctx)
        } catch (_: Exception) { }
    }

    fun quitar(ctx: Context, hora: Long) {
        val quedan = todos(ctx).filter { it.hora != hora }
        try { escribir(ctx, quedan) } catch (_: Exception) { }
    }

    private fun escribir(ctx: Context, lista: List<Viaje>) {
        archivo(ctx).writeText(lista.joinToString("") {
            "${it.hora}|${it.app}|${it.tarifa}|${it.km}|${it.gasolina}\n"
        })
    }

    private fun podar(ctx: Context) {
        val f = archivo(ctx)
        if (f.length() < 100_000) return
        val limite = System.currentTimeMillis() - DIAS_GUARDADOS * 86_400_000L
        escribir(ctx, todos(ctx).filter { it.hora >= limite })
    }

    private fun inicioDeHoy(): Long = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
    }.timeInMillis

    /** Lunes de esta semana a las 00:00. */
    private fun inicioDeSemana(): Long = Calendar.getInstance().apply {
        firstDayOfWeek = Calendar.MONDAY
        set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
        set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
    }.timeInMillis

    fun deHoy(ctx: Context): List<Viaje> = todos(ctx).filter { it.hora >= inicioDeHoy() }

    fun deSemana(ctx: Context): List<Viaje> = todos(ctx).filter { it.hora >= inicioDeSemana() }

    fun totales(lista: List<Viaje>) = Totales(
        lista.size, lista.sumOf { it.tarifa }, lista.sumOf { it.gasolina }, lista.sumOf { it.km }
    )
}
