package com.karen.miruta

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Fotos de las ofertas leídas (solo con Diagnóstico encendido). Quedan en el teléfono,
 * se conservan las últimas [MAX] y el conductor decide si las comparte.
 */
object Fotos {
    private const val MAX = 30
    private const val ANCHO = 900
    private const val ENTRE_FOTOS_MS = 4000L
    private var ultima = 0L

    private fun carpeta(ctx: Context) = File(ctx.filesDir, "diag").apply { mkdirs() }

    fun lista(ctx: Context): List<File> =
        carpeta(ctx).listFiles { f -> f.name.endsWith(".jpg") }?.sortedByDescending { it.name } ?: emptyList()

    /** Guarda [bmp] (sin recortarlo ni recargarlo). [leida] = Checa entendió la oferta. */
    fun guardar(ctx: Context, bmp: Bitmap, app: String?, leida: Boolean) {
        val ahora = System.currentTimeMillis()
        if (ahora - ultima < ENTRE_FOTOS_MS) return
        ultima = ahora
        try {
            val alto = (bmp.height * (ANCHO.toFloat() / bmp.width)).toInt()
            val chica = Bitmap.createScaledBitmap(bmp, ANCHO, alto, true)
            val fecha = SimpleDateFormat("yyyyMMdd-HHmmss", Locale.US).format(Date(ahora))
            val f = File(carpeta(ctx), "$fecha-${app ?: "pantalla"}-${if (leida) "leida" else "noleida"}.jpg")
            FileOutputStream(f).use { chica.compress(Bitmap.CompressFormat.JPEG, 65, it) }
            if (chica !== bmp) chica.recycle()
            lista(ctx).drop(MAX).forEach { it.delete() }
        } catch (_: Exception) { }
    }

    fun borrar(ctx: Context) = lista(ctx).forEach { it.delete() }

    fun compartir(ctx: Context) {
        val fotos = lista(ctx)
        if (fotos.isEmpty()) return
        try {
            val uris = ArrayList(fotos.map { FileProvider.getUriForFile(ctx, ctx.packageName + ".fileprovider", it) })
            val i = Intent(Intent.ACTION_SEND_MULTIPLE).apply {
                type = "image/jpeg"
                putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            ctx.startActivity(Intent.createChooser(i, "Compartir fotos").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        } catch (_: Exception) { }
    }
}
