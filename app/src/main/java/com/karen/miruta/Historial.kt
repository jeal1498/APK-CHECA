package com.karen.miruta

import android.content.Context
import java.io.File
import java.util.Calendar

data class Registro(
    val hora: Long, val app: String, val tarifa: Double,
    val km: Double, val min: Double, val nivel: Int,
    val recogidaKm: Double = 0.0, val recogidaMin: Double = 0.0,
    val multiplicador: Double = 0.0
) {
    val porKm: Double get() = if (km > 0) tarifa / km else 0.0
    val porHora: Double get() = if (min > 0) tarifa / min * 60.0 else 0.0
}

/** Historial de ofertas en un archivo de texto dentro de la app (solo en el teléfono). */
object Historial {
    private const val ARCHIVO = "historial.txt"
    private const val DIAS_GUARDADOS = 30

    private fun archivo(ctx: Context) = File(ctx.filesDir, ARCHIVO)

    fun registrar(ctx: Context, o: Oferta, app: String, nivel: Int) {
        try {
            val linea = listOf(
                System.currentTimeMillis(), app.replace("|", ""),
                o.tarifa, o.km, o.min, nivel, o.recogidaKm, o.recogidaMin, o.multiplicador
            ).joinToString("|")
            archivo(ctx).appendText(linea + "\n")
            podarSiHaceFalta(ctx)
        } catch (_: Exception) { }
    }

    fun todos(ctx: Context): List<Registro> {
        val f = archivo(ctx)
        if (!f.exists()) return emptyList()
        return try {
            f.readLines().mapNotNull { l ->
                val p = l.split("|")
                if (p.size < 6) null else try {
                    Registro(
                        p[0].toLong(), p[1], p[2].toDouble(), p[3].toDouble(), p[4].toDouble(), p[5].toInt(),
                        p.getOrNull(6)?.toDoubleOrNull() ?: 0.0, p.getOrNull(7)?.toDoubleOrNull() ?: 0.0,
                        p.getOrNull(8)?.toDoubleOrNull() ?: 0.0
                    )
                } catch (_: Exception) { null }
            }
        } catch (_: Exception) { emptyList() }
    }

    fun deHoy(ctx: Context): List<Registro> {
        val inicio = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
        }.timeInMillis
        // Ignora lecturas imposibles que hayan quedado de versiones anteriores.
        return todos(ctx).filter { it.hora >= inicio && it.km >= 0.3 && it.porKm <= 80 }
    }

    /** Ofertas válidas de los últimos [dias] días (para "Mejores horas"). */
    fun ultimosDias(ctx: Context, dias: Int): List<Registro> {
        val limite = System.currentTimeMillis() - dias * 86_400_000L
        return todos(ctx).filter { it.hora >= limite && it.km >= 0.3 && it.porKm <= 80 }
    }

    fun borrar(ctx: Context) { archivo(ctx).delete() }

    private fun podarSiHaceFalta(ctx: Context) {
        val f = archivo(ctx)
        if (f.length() < 200_000) return
        val limite = System.currentTimeMillis() - DIAS_GUARDADOS * 86_400_000L
        val quedan = todos(ctx).filter { it.hora >= limite }
        f.writeText(quedan.joinToString("") {
            "${it.hora}|${it.app}|${it.tarifa}|${it.km}|${it.min}|${it.nivel}|${it.recogidaKm}|${it.recogidaMin}|${it.multiplicador}\n"
        })
    }
}
