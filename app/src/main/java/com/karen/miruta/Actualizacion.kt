package com.karen.miruta

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Handler
import android.os.Looper
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

/**
 * Avisa cuando hay una versión nueva en la web. Lee descargar/version.json de checa-web
 * (lo escribe publicar-apk.yml en cada compilación) y lo compara con la versión instalada.
 */
object Actualizacion {
    private const val WEB = "https://jeal1498.github.io/checa-web/"
    private const val VERSION_JSON = WEB + "descargar/version.json"
    /** Página que baja el APK y explica cómo instalarlo encima. */
    const val URL_ACTUALIZAR = WEB + "instalar/?desde=app"
    private const val CADA = 6 * 60 * 60 * 1000L

    private val principal = Handler(Looper.getMainLooper())
    @Volatile private var revisando = false

    private fun sp(ctx: Context) = ctx.getSharedPreferences("actualizacion", Context.MODE_PRIVATE)

    fun instalada(ctx: Context): String = try {
        ctx.packageManager.getPackageInfo(ctx.packageName, 0).versionName ?: ""
    } catch (_: Exception) { "" }

    /** Versión nueva ya conocida (sin internet), o null si la instalada está al día. */
    fun pendiente(ctx: Context): String? {
        val v = sp(ctx).getString("nueva", null) ?: return null
        return if (esMayor(v, instalada(ctx))) v else null
    }

    /**
     * Revisa la web como mucho cada 6 h (o ya si [forzar]). [listo] recibe la versión nueva (o null)
     * y si se pudo consultar la web en esta llamada (false sin internet o si no tocaba revisar).
     */
    fun revisar(ctx: Context, forzar: Boolean = false, listo: (String?, Boolean) -> Unit) {
        val app = ctx.applicationContext
        val ultima = sp(app).getLong("revision", 0L)
        if (revisando || (!forzar && System.currentTimeMillis() - ultima < CADA)) {
            listo(pendiente(app), false); return
        }
        revisando = true
        Thread {
            var ok = false
            try {
                val con = URL(VERSION_JSON + "?t=" + System.currentTimeMillis()).openConnection() as HttpURLConnection
                try {
                    con.connectTimeout = 10_000
                    con.readTimeout = 10_000
                    con.useCaches = false
                    if (con.responseCode == 200) {
                        val v = JSONObject(con.inputStream.bufferedReader().use { it.readText() }).optString("version")
                        if (v.isNotEmpty()) {
                            sp(app).edit().putString("nueva", v).putLong("revision", System.currentTimeMillis()).apply()
                            ok = true
                        }
                    }
                } finally { con.disconnect() }
            } catch (_: Exception) { }
            revisando = false
            principal.post { listo(pendiente(app), ok) }
        }.start()
    }

    /** La ventana de aviso sale una sola vez por versión; la tarjeta de Inicio se queda hasta actualizar. */
    fun avisoMostrado(ctx: Context, v: String) = sp(ctx).getString("avisada", null) == v
    fun marcarAviso(ctx: Context, v: String) = sp(ctx).edit().putString("avisada", v).apply()

    fun abrirDescarga(ctx: Context) {
        try {
            ctx.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(URL_ACTUALIZAR)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        } catch (_: Exception) { }
    }

    /** Compara "6.10" > "6.9" por partes numéricas. */
    fun esMayor(a: String, b: String): Boolean {
        val x = a.split('.').map { it.filter { c -> c.isDigit() }.toIntOrNull() ?: 0 }
        val y = b.split('.').map { it.filter { c -> c.isDigit() }.toIntOrNull() ?: 0 }
        for (i in 0 until maxOf(x.size, y.size)) {
            val d = x.getOrElse(i) { 0 } - y.getOrElse(i) { 0 }
            if (d != 0) return d > 0
        }
        return false
    }

    // ------------------------------------------------------------------
    // Compartir
    // ------------------------------------------------------------------

    private const val TEXTO_COMPARTIR =
        "Te recomiendo Checa: en cada oferta de Uber o DiDi te dice cuánto te queda después de gasolina " +
            "y si conviene aceptarla. Pruébala 7 días gratis (Android): " +
            WEB + "?utm_source=app&utm_campaign=compartir"

    fun compartir(ctx: Context) {
        val i = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, "Checa")
            putExtra(Intent.EXTRA_TEXT, TEXTO_COMPARTIR)
        }
        try {
            ctx.startActivity(Intent.createChooser(i, "Compartir Checa").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        } catch (_: Exception) { }
    }
}
