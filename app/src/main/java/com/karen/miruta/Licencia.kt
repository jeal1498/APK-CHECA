package com.karen.miruta

import android.content.Context
import android.provider.Settings
import android.util.Base64
import java.security.KeyFactory
import java.security.MessageDigest
import java.security.Signature
import java.security.spec.X509EncodedKeySpec
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Licencia sin internet. Cada equipo tiene un código (8 letras/números).
 * El código de activación es "AAAAMMDD-firma": la fecha de vencimiento y una firma
 * ECDSA de "LV|<equipo>|<fecha>" hecha con la llave privada (solo la tiene Karen,
 * en el secreto LICENCIA_PRIVADA del workflow "Generar licencia").
 */
object Licencia {
    private const val PUBLICA =
        "MFkwEwYHKoZIzj0CAQYIKoZIzj0DAQcDQgAEUHR2OkZBiVYP2xodleS7qMig+4Yga4+hpFUjOLP805Gh4P46PmFHJnyAqwGq+O5CKOXVuHdTPNr4V6cXIetR/Q=="
    const val DIAS_PRUEBA = 7
    const val PERMANENTE = 99991231

    /** pagada: con código válido; si no, está en prueba (o vencida si diasRestantes = 0). */
    data class Estado(val activa: Boolean, val pagada: Boolean, val diasRestantes: Int, val vence: Int)

    private var cache: Pair<Long, Estado>? = null

    private fun sp(ctx: Context) = ctx.applicationContext.getSharedPreferences("lic", Context.MODE_PRIVATE)

    /** Código del equipo para mostrar, p. ej. "A1B2-C3D4". */
    fun codigoEquipo(ctx: Context): String {
        val c = equipo(ctx)
        return c.substring(0, 4) + "-" + c.substring(4)
    }

    private fun equipo(ctx: Context): String {
        val id = Settings.Secure.getString(ctx.contentResolver, Settings.Secure.ANDROID_ID) ?: "sin-id"
        val h = MessageDigest.getInstance("SHA-256").digest("LV:$id".toByteArray())
        return h.take(4).joinToString("") { "%02X".format(it.toInt() and 0xFF) }
    }

    /** Hoy como AAAAMMDD, sin dejar que el reloj se regrese a una fecha ya vista. */
    private fun hoy(ctx: Context): Int {
        val s = sp(ctx)
        val ahora = maxOf(System.currentTimeMillis(), s.getLong("visto", 0L))
        s.edit().putLong("visto", ahora).apply()
        return SimpleDateFormat("yyyyMMdd", Locale.US).format(Date(ahora)).toInt()
    }

    fun estado(ctx: Context): Estado {
        val t = System.currentTimeMillis()
        cache?.let { (cuando, e) -> if (t - cuando in 0..60_000) return e }
        val s = sp(ctx)
        val hoy = hoy(ctx)
        val vence = s.getString("codigo", null)?.let { venceDe(ctx, it) } ?: 0
        val e = if (vence >= hoy) {
            Estado(true, true, 0, vence)
        } else {
            var inicio = s.getLong("prueba", 0L)
            if (inicio == 0L) {
                inicio = t
                s.edit().putLong("prueba", inicio).apply()
            }
            val usados = ((maxOf(t, s.getLong("visto", t)) - inicio) / 86_400_000L).toInt()
            val quedan = (DIAS_PRUEBA - usados).coerceIn(0, DIAS_PRUEBA)
            Estado(quedan > 0, false, quedan, vence)
        }
        cache = t to e
        return e
    }

    fun activa(ctx: Context): Boolean = estado(ctx).activa

    /** Guarda el código si es válido para este equipo. Devuelve la fecha de vencimiento o null. */
    fun activar(ctx: Context, codigo: String): Int? {
        val limpio = codigo.replace(Regex("\\s"), "")
        val vence = venceDe(ctx, limpio) ?: return null
        sp(ctx).edit().putString("codigo", limpio).apply()
        cache = null
        return vence
    }

    private fun venceDe(ctx: Context, codigo: String): Int? = try {
        val partes = codigo.split("-", limit = 2)
        val fecha = partes[0]
        require(fecha.length == 8)
        val firma = Base64.decode(partes[1], Base64.URL_SAFE or Base64.NO_PADDING or Base64.NO_WRAP)
        val llave = KeyFactory.getInstance("EC")
            .generatePublic(X509EncodedKeySpec(Base64.decode(PUBLICA, Base64.DEFAULT)))
        val v = Signature.getInstance("SHA256withECDSA")
        v.initVerify(llave)
        v.update("LV|${equipo(ctx)}|$fecha".toByteArray())
        if (v.verify(firma)) fecha.toInt() else null
    } catch (_: Exception) {
        null
    }

    fun fechaBonita(aaaammdd: Int): String {
        if (aaaammdd >= PERMANENTE) return "sin vencimiento"
        val d = SimpleDateFormat("yyyyMMdd", Locale.US).parse(aaaammdd.toString()) ?: return ""
        return SimpleDateFormat("d MMM yyyy", Locale("es", "MX")).format(d)
    }
}
