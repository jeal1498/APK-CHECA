package com.karen.miruta

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Imagen «Mi día / Mi semana» para compartir por WhatsApp u otras apps. */
object Resumen {
    private const val ANCHO = 1080
    private const val ALTO = 1350

    fun compartir(ctx: Context, semana: Boolean) {
        val t = Jornada.totales(if (semana) Jornada.deSemana(ctx) else Jornada.deHoy(ctx))
        try {
            val dir = File(ctx.cacheDir, "compartir").apply { mkdirs() }
            val f = File(dir, "checa-resumen.png")
            val bmp = dibujar(t, semana)
            FileOutputStream(f).use { bmp.compress(Bitmap.CompressFormat.PNG, 100, it) }
            val uri = FileProvider.getUriForFile(ctx, ctx.packageName + ".fileprovider", f)
            val i = Intent(Intent.ACTION_SEND).apply {
                type = "image/png"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_TEXT, Actualizacion.textoCompartir(Cuenta.miCodigo(ctx)))
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            ctx.startActivity(Intent.createChooser(i, "Compartir resumen").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        } catch (_: Exception) {
            val i = Intent(Intent.ACTION_SEND).setType("text/plain")
                .putExtra(Intent.EXTRA_TEXT, texto(t, semana) + "\n\n" + Actualizacion.textoCompartir(Cuenta.miCodigo(ctx)))
            ctx.startActivity(Intent.createChooser(i, "Compartir resumen").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        }
    }

    private fun dinero(v: Double) = "$" + String.format(Locale.US, "%,.0f", v)

    private fun texto(t: Totales, semana: Boolean) =
        "Mi ${if (semana) "semana" else "día"} con Checa: ${dinero(t.neta)} netos en ${t.viajes} viajes " +
            "(gasolina ${dinero(t.gasolina)})."

    private fun dibujar(t: Totales, semana: Boolean): Bitmap {
        val bmp = Bitmap.createBitmap(ANCHO, ALTO, Bitmap.Config.ARGB_8888)
        val c = Canvas(bmp)
        c.drawColor(Tema.FONDO)
        val p = Paint(Paint.ANTI_ALIAS_FLAG)

        // Marca arriba
        p.typeface = Fuentes.negrita
        p.color = Tema.TENUE
        p.textSize = 40f
        p.letterSpacing = 0.14f
        c.drawText("CHECA", 72f, 120f, p)
        p.letterSpacing = 0f
        listOf(Tema.VERDE, Tema.AMARILLO, Tema.ROJO).forEachIndexed { i, col ->
            p.color = col
            c.drawCircle(ANCHO - 72f - 20f - i * 44f, 104f, 14f, p)
        }

        // Etiqueta
        val fecha = SimpleDateFormat("d 'de' MMMM", Locale("es", "MX")).format(Date()).uppercase(Locale("es", "MX"))
        p.typeface = Fuentes.media
        p.color = Tema.TENUE
        p.textSize = 40f
        c.drawText((if (semana) "MI SEMANA" else "MI DÍA") + " · " + fecha, 72f, 260f, p)

        // Neta grande
        p.typeface = Fuentes.numero
        p.color = Tema.VERDE
        p.textSize = 200f
        c.drawText(dinero(t.neta), 72f, 470f, p)
        p.typeface = Fuentes.texto
        p.color = Tema.TEXTO
        p.textSize = 48f
        c.drawText("netos después de gasolina", 72f, 550f, p)

        // Tres datos
        val datos = listOf(
            "VIAJES" to t.viajes.toString(),
            "GASOLINA" to dinero(t.gasolina),
            "NETO POR KM" to ("$" + String.format(Locale.US, "%.1f", t.netaPorKm))
        )
        val ancho = (ANCHO - 144f - 2 * 24f) / 3f
        datos.forEachIndexed { i, (etq, valor) ->
            val x = 72f + i * (ancho + 24f)
            p.color = Tema.TARJETA2
            p.style = Paint.Style.FILL
            c.drawRoundRect(RectF(x, 680f, x + ancho, 930f), 32f, 32f, p)
            p.typeface = Fuentes.media
            p.color = Tema.TENUE
            p.textSize = 30f
            c.drawText(etq, x + 28f, 744f, p)
            p.typeface = Fuentes.numero
            p.color = Tema.TEXTO
            p.textSize = if (valor.length > 6) 52f else 64f
            c.drawText(valor, x + 28f, 850f, p)
        }

        // Pie
        p.typeface = Fuentes.texto
        p.color = Tema.TEXTO2
        p.textSize = 40f
        c.drawText("Checa: te dice si cada oferta de Uber o DiDi conviene.", 72f, 1170f, p)
        p.color = Tema.VERDE
        p.typeface = Fuentes.media
        c.drawText("Pruébala 7 días gratis · jeal1498.github.io/checa-web", 72f, 1240f, p)
        return bmp
    }
}
