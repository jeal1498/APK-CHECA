package com.karen.miruta

import android.content.Context
import java.io.File
import java.util.Calendar
import java.util.Locale

/** Resumen de «Mejores horas» de una celda día × hora: ofertas, tarifa total y km totales (30 días). */
data class CeldaHora(val dia: Int, val hora: Int, val n: Int, val tarifa: Double, val km: Double)

/**
 * Resumen por hora que viaja con la cuenta (10.39): solo conteo, tarifa y km por día y hora, no las ofertas.
 * En un equipo nuevo rellena «Mejores horas» mientras el historial local está vacío. Día 0 = lunes.
 * Archivo horas_nube.txt, línea: dia|hora|n|tarifa|km (lo último bajado de la cuenta).
 */
object Horas {
    private const val ARCHIVO = "horas_nube.txt"

    private fun archivo(ctx: Context) = File(ctx.filesDir, ARCHIVO)

    private fun diaLunes(c: Calendar) = (c.get(Calendar.DAY_OF_WEEK) + 5) % 7

    /** Celdas con ofertas del historial local de los últimos 30 días. */
    fun locales(ctx: Context): List<CeldaHora> = deRegistros(Historial.ultimosDias(ctx, 30))

    fun deRegistros(regs: List<Registro>): List<CeldaHora> {
        val cal = Calendar.getInstance()
        return regs.groupBy { cal.timeInMillis = it.hora; diaLunes(cal) * 24 + cal.get(Calendar.HOUR_OF_DAY) }
            .map { (k, l) -> CeldaHora(k / 24, k % 24, l.size, l.sumOf { it.tarifa }, l.sumOf { it.km }) }
            .sortedBy { it.dia * 24 + it.hora }
    }

    /** Texto estable de las celdas, para saber si algo cambió desde la última subida. */
    fun canon(l: List<CeldaHora>): String = l.joinToString(";") {
        "${it.dia}.${it.hora}.${it.n}.${String.format(Locale.US, "%.2f.%.2f", it.tarifa, it.km)}"
    }

    fun deNube(ctx: Context): List<CeldaHora> {
        val f = archivo(ctx)
        if (!f.exists()) return emptyList()
        return try {
            f.readLines().mapNotNull { l ->
                val p = l.split("|")
                try {
                    if (p.size < 5) null else CeldaHora(p[0].toInt(), p[1].toInt(), p[2].toInt(), p[3].toDouble(), p[4].toDouble())
                } catch (_: Exception) { null }
            }
        } catch (_: Exception) { emptyList() }
    }

    fun guardarNube(ctx: Context, l: List<CeldaHora>) {
        try {
            archivo(ctx).writeText(l.joinToString("") { "${it.dia}|${it.hora}|${it.n}|${it.tarifa}|${it.km}\n" })
        } catch (_: Exception) { }
    }

    fun borrar(ctx: Context) { archivo(ctx).delete() }

    /** Celdas donde la cuenta tiene más ofertas que este equipo. */
    fun masLlenasEnLaNube(ctx: Context): List<CeldaHora> {
        val loc = locales(ctx).associateBy { it.dia * 24 + it.hora }
        return deNube(ctx).filter { it.n > (loc[it.dia * 24 + it.hora]?.n ?: 0) }
    }

    /**
     * Ofertas de relleno para «Mejores horas»: en cada celda donde la cuenta sabe más que este equipo,
     * n copias del promedio (misma tarifa ÷ km). Solo para pintar; nunca se guardan en el historial.
     */
    private fun relleno(extra: List<CeldaHora>): List<Registro> {
        val out = ArrayList<Registro>()
        for (c in extra) {
            if (c.n <= 0 || c.km <= 0) continue
            val cal = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, c.hora); set(Calendar.MINUTE, 30); set(Calendar.SECOND, 0)
                for (i in 0 until 7) { if (diaLunes(this) == c.dia) break; add(Calendar.DAY_OF_MONTH, -1) }
            }
            if (cal.timeInMillis > System.currentTimeMillis()) cal.add(Calendar.DAY_OF_MONTH, -7)
            val t = c.tarifa / c.n
            val k = c.km / c.n
            repeat(c.n) { out.add(Registro(cal.timeInMillis, "nube", t, k, 0.0, 0)) }
        }
        return out
    }

    /** Historial local de 30 días con las celdas más llenas de la cuenta en lugar de las locales. */
    fun mezclar(ctx: Context): List<Registro> {
        val locales = Historial.ultimosDias(ctx, 30)
        val extra = masLlenasEnLaNube(ctx)
        if (extra.isEmpty()) return locales
        val cal = Calendar.getInstance()
        val reemplazadas = extra.map { it.dia * 24 + it.hora }.toSet()
        val propias = locales.filter {
            cal.timeInMillis = it.hora
            (diaLunes(cal) * 24 + cal.get(Calendar.HOUR_OF_DAY)) !in reemplazadas
        }
        return propias + relleno(extra)
    }
}
