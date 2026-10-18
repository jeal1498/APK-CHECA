package com.karen.miruta

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.job.JobInfo
import android.app.job.JobParameters
import android.app.job.JobScheduler
import android.app.job.JobService
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileOutputStream
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
    private const val APK = WEB + "descargar/Checa.apk"
    private const val CADA = 6 * 60 * 60 * 1000L
    /** La ventana de aviso vuelve a salir al abrir Checa, como mucho cada 30 min, hasta actualizar. */
    private const val REPETIR_AVISO = 30 * 60 * 1000L
    private const val CANAL = "actualizaciones"
    private const val ID_NOTIFICACION = 7
    private const val ID_TRABAJO = 7

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

    /** La ventana sale cada vez que se abre Checa (como mucho cada 30 min) hasta actualizar; la tarjeta de Inicio se queda. */
    fun avisoToca(ctx: Context, v: String): Boolean {
        val s = sp(ctx)
        return s.getString("avisada", null) != v || System.currentTimeMillis() - s.getLong("avisadaHora", 0L) >= REPETIR_AVISO
    }

    fun marcarAviso(ctx: Context, v: String) =
        sp(ctx).edit().putString("avisada", v).putLong("avisadaHora", System.currentTimeMillis()).apply()

    // ------------------------------------------------------------------
    // Aviso con Checa cerrada: revisión en segundo plano y notificación (una por versión)
    // ------------------------------------------------------------------

    /** Programa la revisión cada 6 h con internet (sigue después de reiniciar el teléfono). */
    fun programar(ctx: Context) {
        try {
            val js = ctx.getSystemService(JobScheduler::class.java) ?: return
            if (js.getPendingJob(ID_TRABAJO) != null) return
            js.schedule(
                JobInfo.Builder(ID_TRABAJO, ComponentName(ctx, ActualizacionJob::class.java))
                    .setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY)
                    .setPeriodic(CADA)
                    .setPersisted(true)
                    .build()
            )
        } catch (_: Exception) { }
    }

    /** Notificación «Hay una versión nueva»; tocarla abre Checa con la ventana para actualizar. */
    fun notificar(ctx: Context, v: String) {
        val s = sp(ctx)
        if (s.getString("notificada", null) == v) return
        if (Build.VERSION.SDK_INT >= 33 &&
            ctx.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) return
        try {
            val nm = ctx.getSystemService(NotificationManager::class.java) ?: return
            nm.createNotificationChannel(NotificationChannel(CANAL, "Versiones nuevas", NotificationManager.IMPORTANCE_DEFAULT))
            val abrir = PendingIntent.getActivity(
                ctx, ID_NOTIFICACION,
                Intent(ctx, MainActivity::class.java).putExtra(MainActivity.EXTRA_ACTUALIZAR, true)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP),
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )
            nm.notify(
                ID_NOTIFICACION,
                Notification.Builder(ctx, CANAL)
                    .setContentTitle("Hay una versión nueva de Checa")
                    .setContentText("Toca para actualizar a la $v. No pierdes tu cuenta ni tus ajustes.")
                    .setSmallIcon(R.drawable.ic_notif)
                    .setContentIntent(abrir)
                    .setAutoCancel(true)
                    .build()
            )
            s.edit().putString("notificada", v).apply()
        } catch (_: Exception) { }
    }

    fun quitarNotificacion(ctx: Context) {
        try { ctx.getSystemService(NotificationManager::class.java)?.cancel(ID_NOTIFICACION) } catch (_: Exception) { }
    }

    fun abrirDescarga(ctx: Context) {
        try {
            ctx.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(URL_ACTUALIZAR)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        } catch (_: Exception) { }
    }

    // ------------------------------------------------------------------
    // Descarga e instalación dentro de la app (el usuario confirma en la ventana de Android)
    // ------------------------------------------------------------------

    /** Android pide un permiso, una sola vez, para que Checa pueda abrir el instalador. */
    fun puedeInstalar(ctx: Context) = Build.VERSION.SDK_INT < 26 || ctx.packageManager.canRequestPackageInstalls()

    fun pedirPermisoInstalar(ctx: Context) {
        try {
            ctx.startActivity(
                Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, Uri.parse("package:" + ctx.packageName))
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
        } catch (_: Exception) { }
    }

    /** Baja el APK de la web. [progreso] recibe 0–100 (o -1 si no se sabe); [listo], el archivo o null si falló. */
    fun descargar(ctx: Context, progreso: (Int) -> Unit, listo: (File?) -> Unit) {
        val app = ctx.applicationContext
        Thread {
            var archivo: File? = null
            try {
                val dir = File(app.cacheDir, "actualizacion").apply { mkdirs(); listFiles()?.forEach { it.delete() } }
                val f = File(dir, "Checa.apk")
                val con = URL(APK + "?t=" + System.currentTimeMillis()).openConnection() as HttpURLConnection
                try {
                    con.connectTimeout = 15_000
                    con.readTimeout = 30_000
                    con.useCaches = false
                    if (con.responseCode == 200) {
                        val total = con.contentLengthLong
                        var hecho = 0L
                        var ultimo = -2
                        con.inputStream.use { ent ->
                            FileOutputStream(f).use { sal ->
                                val buf = ByteArray(32 * 1024)
                                while (true) {
                                    val n = ent.read(buf)
                                    if (n < 0) break
                                    sal.write(buf, 0, n)
                                    hecho += n
                                    val pct = if (total > 0) (hecho * 100 / total).toInt() else -1
                                    if (pct != ultimo) { ultimo = pct; principal.post { progreso(pct) } }
                                }
                            }
                        }
                        // Que sea de verdad el APK de Checa antes de abrir el instalador.
                        val info = app.packageManager.getPackageArchiveInfo(f.path, 0)
                        if (info != null && info.packageName == app.packageName && f.length() > 1_000_000) archivo = f
                    }
                } finally { con.disconnect() }
            } catch (_: Exception) { }
            principal.post { listo(archivo) }
        }.start()
    }

    /** Abre el instalador de Android con el APK descargado. */
    fun instalar(ctx: Context, apk: File): Boolean = try {
        val uri = FileProvider.getUriForFile(ctx, ctx.packageName + ".fileprovider", apk)
        ctx.startActivity(
            Intent(Intent.ACTION_VIEW)
                .setDataAndType(uri, "application/vnd.android.package-archive")
                .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
        )
        true
    } catch (_: Exception) { false }

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

    /** Mensaje para compartir; con [codigo], el link lleva ?ref= y el amigo gana el mes extra al pagar. */
    fun textoCompartir(codigo: String?): String {
        val link = WEB + "?" + (if (codigo != null) "ref=$codigo&" else "") + "utm_source=app&utm_campaign=compartir"
        val base = "Te recomiendo Checa: en cada oferta de Uber o DiDi te dice cuánto te queda después de gasolina " +
            "y si conviene aceptarla. Es gratis durante la beta (Android): $link"
        return if (codigo == null) base
        else "$base\n\nUsa mi código $codigo: si pagas un paquete, los dos ganamos 1 mes gratis."
    }

    fun compartir(ctx: Context, codigo: String?) {
        val i = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, "Checa")
            putExtra(Intent.EXTRA_TEXT, textoCompartir(codigo))
        }
        try {
            ctx.startActivity(Intent.createChooser(i, "Compartir Checa").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        } catch (_: Exception) { }
    }
}

/** Revisión de versión nueva en segundo plano (la programa [Actualizacion.programar]). */
class ActualizacionJob : JobService() {
    override fun onStartJob(params: JobParameters?): Boolean {
        Actualizacion.revisar(applicationContext, forzar = true) { v, _ ->
            if (v != null) Actualizacion.notificar(applicationContext, v)
            jobFinished(params, false)
        }
        return true
    }

    override fun onStopJob(params: JobParameters?): Boolean = true
}
