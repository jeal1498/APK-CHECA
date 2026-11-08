package com.karen.miruta

import android.content.Context

/**
 * Tiempo manejando seguido (10.41), para el widget Descanso. Cada evento de Uber/DiDi con la
 * lectura encendida suma el rato desde el anterior; una pausa de 10 min o más cuenta como
 * descanso y la racha vuelve a cero. A las 4 h seguidas avisa una vez.
 */
object Manejo {
    const val LIMITE_MIN = 240
    private const val REPOSO_MS = 10 * 60_000L
    private const val GUARDAR_CADA_MS = 30_000L

    private var cargado = false
    private var racha = 0L
    private var ultimo = 0L
    private var avisado = false
    private var guardado = 0L

    private fun sp(ctx: Context) = ctx.applicationContext.getSharedPreferences("manejo", Context.MODE_PRIVATE)

    private fun cargar(ctx: Context) {
        if (cargado) return
        val p = sp(ctx)
        racha = p.getLong("racha", 0L)
        ultimo = p.getLong("ultimo", 0L)
        avisado = p.getBoolean("avisado", false)
        cargado = true
    }

    private fun guardar(ctx: Context, ahora: Long) {
        guardado = ahora
        sp(ctx).edit().putLong("racha", racha).putLong("ultimo", ultimo).putBoolean("avisado", avisado).apply()
    }

    /** Hubo actividad de Uber/DiDi en [ahora]. true = justo se llegó a las 4 h seguidas (avisar una vez). */
    @Synchronized
    fun latido(ctx: Context, ahora: Long): Boolean {
        cargar(ctx)
        val hueco = ahora - ultimo
        if (ultimo == 0L || hueco > REPOSO_MS || hueco < 0L) {
            racha = 0L
            avisado = false
        } else {
            racha += hueco
        }
        ultimo = ahora
        val toca = racha >= LIMITE_MIN * 60_000L && !avisado
        if (toca) avisado = true
        if (toca || ahora - guardado >= GUARDAR_CADA_MS) guardar(ctx, ahora)
        return toca
    }

    /** Minutos manejando seguidos; 0 si ya pasaron 10 min sin Uber/DiDi (ya descansó). */
    @Synchronized
    fun minutos(ctx: Context): Int {
        cargar(ctx)
        if (ultimo == 0L || System.currentTimeMillis() - ultimo > REPOSO_MS) return 0
        return (racha / 60_000L).toInt()
    }
}
