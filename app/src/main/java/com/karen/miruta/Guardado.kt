package com.karen.miruta

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.concurrent.Executors

/**
 * «Lo guardado»: cada oferta que Checa vio (con «Guardar pantallas de ofertas» encendido) queda como un
 * registro con su foto (solo la zona de la app) y el texto que leyó. Solo en el teléfono, se borran solos a
 * los [DIAS] días y las que no se pudieron leer se conservan antes que las leídas. Nada se envía solo.
 */
object Guardado {
    private const val DIAS = 7
    private const val MAX_LEIDAS = 20
    private const val MAX_NO_LEIDAS = 40
    private const val ANCHO = 900
    /** La misma oferta en pantalla no se vuelve a guardar en este tiempo. */
    private const val MISMA_LEIDA_MS = 120_000L
    private const val MISMA_NO_LEIDA_MS = 20_000L
    private const val DIA = 86_400_000L

    /** La foto se comprime fuera del hilo principal para no dar tirones al pop-up. */
    private val hilo = Executors.newSingleThreadExecutor()
    private val ultimas = HashMap<String, Pair<String, Long>>()

    class Item(val base: String, val foto: File?, val texto: File, val hora: Long, val app: String, val leida: Boolean) {
        fun lineas(): String = try { texto.readText() } catch (_: Exception) { "" }

        /** «DiDi · hoy 8:12 pm» */
        fun titulo(): String = "$app · " + cuando(hora)
    }

    private fun carpeta(ctx: Context) = File(ctx.filesDir, "diag").apply { mkdirs() }

    fun lista(ctx: Context): List<Item> {
        val dir = carpeta(ctx)
        val formato = SimpleDateFormat("yyyyMMdd-HHmmss", Locale.US)
        return (dir.listFiles { f -> f.name.endsWith(".txt") } ?: emptyArray()).mapNotNull { t ->
            val base = t.name.removeSuffix(".txt")
            val partes = base.split("-")
            if (partes.size < 4) return@mapNotNull null
            val hora = try { formato.parse(partes[0] + "-" + partes[1])?.time } catch (_: Exception) { null } ?: return@mapNotNull null
            val foto = File(dir, "$base.jpg").takeIf { it.exists() }
            Item(base, foto, t, hora, partes[2], partes[3] == "leida")
        }.sortedByDescending { it.base }
    }

    fun bytes(ctx: Context): Long = carpeta(ctx).listFiles()?.sumOf { it.length() } ?: 0L

    private fun nombreApp(app: String?) = (app ?: "Pantalla").replace("-", "")

    /** true si es una oferta distinta a la última guardada de esa app (la misma en pantalla no se repite). */
    fun esNueva(app: String?, leida: Boolean, firma: String): Boolean {
        val clave = nombreApp(app)
        val ahora = System.currentTimeMillis()
        synchronized(ultimas) {
            val previa = ultimas[clave]
            val espera = if (leida) MISMA_LEIDA_MS else MISMA_NO_LEIDA_MS
            if (previa != null && previa.first == firma && ahora - previa.second < espera) return false
            ultimas[clave] = firma to ahora
        }
        return true
    }

    /**
     * Guarda una oferta vista (antes, [esNueva]). [recorte] es una copia propia de la zona de la app
     * (o null si no hay foto); se comprime y recicla en segundo plano.
     */
    fun guardar(ctx: Context, app: String?, leida: Boolean, texto: String, recorte: Bitmap?) {
        val app2 = nombreApp(app)
        val ahora = System.currentTimeMillis()
        val appCtx = ctx.applicationContext
        hilo.execute {
            try {
                val dir = carpeta(appCtx)
                val base = SimpleDateFormat("yyyyMMdd-HHmmss", Locale.US).format(Date(ahora)) +
                    "-$app2-" + if (leida) "leida" else "noleida"
                if (recorte != null) {
                    FileOutputStream(File(dir, "$base.jpg")).use { recorte.compress(Bitmap.CompressFormat.JPEG, 70, it) }
                    recorte.recycle()
                }
                File(dir, "$base.txt").writeText(texto)
                podar(appCtx)
            } catch (_: Exception) { }
        }
    }

    /** Recorta [bmp] a [zona] (coordenadas de la imagen) y lo achica: devuelve siempre una copia propia. */
    fun recortar(bmp: Bitmap, zona: android.graphics.Rect?): Bitmap? = try {
        val r = android.graphics.Rect(0, 0, bmp.width, bmp.height)
        if (zona != null && !r.intersect(zona)) r.set(0, 0, bmp.width, bmp.height)
        val c = Bitmap.createBitmap(bmp, r.left, r.top, r.width(), r.height())
        val ancho = minOf(ANCHO, c.width)
        var s = Bitmap.createScaledBitmap(c, ancho, (c.height.toLong() * ancho / c.width).toInt().coerceAtLeast(1), true)
        if (s === bmp) s = bmp.copy(Bitmap.Config.ARGB_8888, false)
        if (c !== bmp && c !== s) c.recycle()
        s
    } catch (_: Exception) { null }

    /** Más de [DIAS] días se borra; si hay demasiadas, se van primero las leídas más viejas. */
    fun podar(ctx: Context) {
        val limite = System.currentTimeMillis() - DIAS * DIA
        val todos = lista(ctx)
        todos.filter { it.hora < limite }.forEach { borrar(it) }
        val vigentes = todos.filter { it.hora >= limite }
        vigentes.filter { it.leida }.drop(MAX_LEIDAS).forEach { borrar(it) }
        vigentes.filter { !it.leida }.drop(MAX_NO_LEIDAS).forEach { borrar(it) }
    }

    fun borrar(item: Item) {
        item.foto?.delete()
        item.texto.delete()
    }

    fun borrar(items: Collection<Item>) = items.forEach { borrar(it) }

    fun borrarTodo(ctx: Context) {
        carpeta(ctx).listFiles()?.forEach { it.delete() }
        synchronized(ultimas) { ultimas.clear() }
        sp(ctx).edit().clear().apply()
    }

    /** Miniatura liviana (o la foto casi completa si [lado] es grande). */
    fun miniatura(f: File, lado: Int): Bitmap? = try {
        val o = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(f.path, o)
        var m = 1
        while (o.outWidth / (m * 2) >= lado) m *= 2
        BitmapFactory.decodeFile(f.path, BitmapFactory.Options().apply { inSampleSize = m })
    } catch (_: Exception) { null }

    // ------------------------------------------------------------------
    // Envío a soporte
    // ------------------------------------------------------------------

    /**
     * Abre WhatsApp con el mensaje y las fotos elegidas. Con fotos, WhatsApp no deja abrir un chat
     * concreto: el conductor elige el chat de soporte. Sin fotos, se abre directo el chat de soporte.
     */
    fun enviar(ctx: Context, items: List<Item>, mensaje: String, numero: String) {
        val fotos = items.mapNotNull { it.foto }
        try {
            if (fotos.isEmpty()) {
                ctx.startActivity(
                    Intent(Intent.ACTION_VIEW, Uri.parse("https://wa.me/$numero?text=" + Uri.encode(mensaje)))
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                )
                return
            }
            val uris = ArrayList(fotos.map { FileProvider.getUriForFile(ctx, ctx.packageName + ".fileprovider", it) })
            val i = Intent(Intent.ACTION_SEND_MULTIPLE).apply {
                type = "image/jpeg"
                putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris)
                putExtra(Intent.EXTRA_TEXT, mensaje)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            val wa = listOf("com.whatsapp", "com.whatsapp.w4b").firstOrNull { p ->
                try { ctx.packageManager.getPackageInfo(p, 0); true } catch (_: Exception) { false }
            }
            if (wa != null) ctx.startActivity(Intent(i).setPackage(wa))
            else ctx.startActivity(Intent.createChooser(i, "Enviar a soporte").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        } catch (_: Exception) { }
    }

    private fun sp(ctx: Context) = ctx.applicationContext.getSharedPreferences("registro", Context.MODE_PRIVATE)

    /**
     * «Borrar lo enviado al terminar»: WhatsApp lee las fotos después de abrirse, así que no se borran
     * al momento sino la siguiente vez que se abre Checa, pasados unos minutos.
     */
    fun borrarDespues(ctx: Context, items: List<Item>) {
        val previas = sp(ctx).getStringSet("pendientes", emptySet()) ?: emptySet()
        sp(ctx).edit()
            .putStringSet("pendientes", previas + items.map { it.base })
            .putLong("pendientes_desde", System.currentTimeMillis())
            .apply()
    }

    fun borrarPendientes(ctx: Context) {
        val s = sp(ctx)
        val desde = s.getLong("pendientes_desde", 0L)
        if (desde == 0L || System.currentTimeMillis() - desde < 10 * 60_000L) return
        val bases = s.getStringSet("pendientes", emptySet()) ?: emptySet()
        lista(ctx).filter { it.base in bases }.forEach { borrar(it) }
        s.edit().remove("pendientes").remove("pendientes_desde").apply()
    }

    // ------------------------------------------------------------------

    /** «hoy 8:12 pm», «ayer 10:40 pm» o «16 oct 7:55 pm». */
    fun cuando(ms: Long): String {
        val hoy = Calendar.getInstance()
        val c = Calendar.getInstance().apply { timeInMillis = ms }
        val hora = SimpleDateFormat("h:mm a", Locale("es", "MX")).format(Date(ms))
            .replace("a. m.", "am").replace("p. m.", "pm").replace(" a.m.", " am").replace(" p.m.", " pm")
        val dias = ((inicioDia(hoy) - inicioDia(c)) / DIA).toInt()
        return when (dias) {
            0 -> "hoy $hora"
            1 -> "ayer $hora"
            else -> SimpleDateFormat("d MMM", Locale("es", "MX")).format(Date(ms)).replace(".", "") + " $hora"
        }
    }

    private fun inicioDia(c: Calendar): Long = (c.clone() as Calendar).apply {
        set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
    }.timeInMillis
}
