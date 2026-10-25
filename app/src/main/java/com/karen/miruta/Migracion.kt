package com.karen.miruta

import android.content.Context
import android.content.Intent
import android.net.Uri

/**
 * Cierre del canal directo (APK de la web): cuenta regresiva y bloqueo total al llegar a [LIMITE_MS].
 * Los usuarios pasan a Checa de Google Play (com.jeal1498.checa) por el registro de testers.
 * El canal play nunca se bloquea ni muestra el aviso.
 */
object Migracion {
    /** 15 nov 2026 23:59 Cancún (UTC-5) = 16 nov 05:00 UTC; es el mismo cierre de /registro/. */
    const val LIMITE_MS = 1794805200000L
    const val URL_REGISTRO = "https://jeal1498.github.io/checa-web/registro/"
    /** La ventana de aviso sale al abrir Checa, como mucho cada 30 min. */
    private const val REPETIR_AVISO = 30 * 60 * 1000L

    private fun sp(ctx: Context) = ctx.applicationContext.getSharedPreferences("migracion", Context.MODE_PRIVATE)

    fun aplica() = !BuildConfig.ES_PLAY

    fun vencida() = aplica() && System.currentTimeMillis() >= LIMITE_MS

    /** Texto de la cuenta regresiva: «12 días 4 h», «5 h 30 min» o «40 min». */
    fun restante(): String {
        val min = maxOf(0L, (LIMITE_MS - System.currentTimeMillis()) / 60_000L)
        val d = min / 1440
        val h = (min % 1440) / 60
        val m = min % 60
        return when {
            d > 0 -> "$d ${if (d == 1L) "día" else "días"} $h h"
            h > 0 -> "$h h $m min"
            else -> "${maxOf(1L, m)} min"
        }
    }

    fun avisoToca(ctx: Context) = System.currentTimeMillis() - sp(ctx).getLong("avisoHora", 0L) >= REPETIR_AVISO

    fun marcarAviso(ctx: Context) = sp(ctx).edit().putLong("avisoHora", System.currentTimeMillis()).apply()

    fun abrirRegistro(ctx: Context) {
        try {
            ctx.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(URL_REGISTRO)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        } catch (_: Exception) { }
    }
}
