package com.karen.miruta

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileOutputStream
import java.util.Calendar
import java.util.Locale

/** Imagen «Mi día / Mi semana» para compartir por WhatsApp u otras apps. */
object Resumen {
    private const val ANCHO = 1080
    private const val ALTO = 1350

    fun compartir(ctx: Context, semana: Boolean) {
        mon = Prefs(ctx).moneda
        val viajes = if (semana) Jornada.deSemana(ctx) else Jornada.deHoy(ctx)
        val t = Jornada.totales(viajes)
        try {
            val dir = File(ctx.cacheDir, "compartir").apply { mkdirs() }
            val f = File(dir, "checa-resumen.png")
            val bmp = dibujar(ctx, t, viajes, semana)
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

    private var mon = Moneda.MXN
    private fun dinero(v: Double) = mon.monto(v, 0)

    private fun texto(t: Totales, semana: Boolean) =
        "Mi ${if (semana) "semana" else "día"} con Checa: ${dinero(t.neta)} netos en ${t.viajes} viajes " +
            "(gasolina ${dinero(t.gasolina)})."

    private val DIAS = listOf("DOM", "LUN", "MAR", "MIÉ", "JUE", "VIE", "SÁB")
    private val MESES = listOf("ENE", "FEB", "MAR", "ABR", "MAY", "JUN", "JUL", "AGO", "SEP", "OCT", "NOV", "DIC")

    private fun fechaDia(c: Calendar) =
        "${DIAS[c.get(Calendar.DAY_OF_WEEK) - 1]} ${c.get(Calendar.DAY_OF_MONTH)} ${MESES[c.get(Calendar.MONTH)]}"

    /** «13–19 OCT» o «27 OCT–2 NOV». */
    private fun rangoSemana(): String {
        val ini = Calendar.getInstance().apply { timeInMillis = Jornada.inicioDeSemana() }
        val fin = (ini.clone() as Calendar).apply { add(Calendar.DAY_OF_MONTH, 6) }
        val a = ini.get(Calendar.DAY_OF_MONTH)
        val b = fin.get(Calendar.DAY_OF_MONTH)
        return if (ini.get(Calendar.MONTH) == fin.get(Calendar.MONTH)) "$a–$b ${MESES[fin.get(Calendar.MONTH)]}"
        else "$a ${MESES[ini.get(Calendar.MONTH)]}–$b ${MESES[fin.get(Calendar.MONTH)]}"
    }

    /**
     * Marca v2 (10.30): tarjeta con borde de semáforo como el pop-up, el neto grande al centro, viajes y neto/km,
     * barra de meta (solo el día) y las ofertas en sus colores.
     */
    private fun dibujar(ctx: Context, t: Totales, viajes: List<Viaje>, semana: Boolean): Bitmap {
        val bmp = Bitmap.createBitmap(ANCHO, ALTO, Bitmap.Config.ARGB_8888)
        val c = Canvas(bmp)
        c.drawColor(Oscuro.ASFALTO)
        val p = Paint(Paint.ANTI_ALIAS_FLAG)
        val x0 = 70f
        val x1 = ANCHO - 70f

        fun texto(s: String, x: Float, y: Float, tf: Typeface, tam: Float, color: Int,
                  espacio: Float = 0f, alinear: Paint.Align = Paint.Align.LEFT) {
            p.style = Paint.Style.FILL
            p.typeface = tf
            p.textSize = tam
            p.color = color
            p.letterSpacing = espacio
            p.textAlign = alinear
            c.drawText(s, x, y, p)
            p.textAlign = Paint.Align.LEFT
            p.letterSpacing = 0f
        }
        fun medir(s: String, tf: Typeface, tam: Float): Float {
            p.typeface = tf
            p.textSize = tam
            p.letterSpacing = 0f
            return p.measureText(s)
        }
        fun caja(l: Float, t: Float, r: Float, b: Float, radio: Float, color: Int) {
            p.style = Paint.Style.FILL
            p.color = color
            c.drawRoundRect(RectF(l, t, r, b), radio, radio, p)
        }

        // Tarjeta con borde de semáforo, como el pop-up: el color solo decide (meta cumplida, a medias o lejos).
        val meta = if (semana) 0.0 else Prefs(ctx).metaDia.toDouble()
        val avance = if (meta > 0) t.neta / meta else -1.0
        val colBorde = when {
            avance < 0 -> Oscuro.HUESO
            avance >= 1.0 -> Tema.VERDE
            avance >= 0.5 -> Oscuro.AMARILLO
            else -> Tema.ROJO
        }
        val etq = when {
            semana -> "SEMANA"
            avance < 0 -> "MI DÍA"
            avance >= 1.0 -> "META CUMPLIDA"
            avance >= 0.5 -> "VAS A MEDIAS"
            else -> "TE FALTA"
        }
        val fecha = if (semana) rangoSemana() else fechaDia(Calendar.getInstance())
        val cx = ANCHO / 2f
        val arriba = if (meta > 0) 190f else 250f
        val abajo = arriba + if (meta > 0) 700f else 560f
        caja(x0, arriba, x1, abajo, 60f, Oscuro.TARJETA)
        p.style = Paint.Style.STROKE
        p.strokeWidth = 8f
        p.color = colBorde
        c.drawRoundRect(RectF(x0, arriba, x1, abajo), 60f, 60f, p)
        p.style = Paint.Style.FILL

        texto("$etq · $fecha", cx, arriba + 110f, Fuentes.numero, 33f, colBorde, 0.12f, Paint.Align.CENTER)
        val neto = dinero(t.neta)
        var tam = 190f
        while (medir(neto, Fuentes.titulo, tam) > x1 - x0 - 80f && tam > 80f) tam -= 10f
        texto(neto, cx, arriba + 320f, Fuentes.titulo, tam, Oscuro.HUESO, -0.03f, Paint.Align.CENTER)
        texto("neto ${if (semana) "de la semana" else "del día"}, después de gasolina", cx, arriba + 390f, Fuentes.texto, 38f,
            Oscuro.TEXTO2, 0f, Paint.Align.CENTER)
        texto("${t.viajes} ${if (t.viajes == 1) "viaje" else "viajes"}  |  ${mon.monto(t.netaPorKm, 2)}/km", cx, arriba + 480f,
            Fuentes.numero, 46f, Oscuro.HUESO, 0f, Paint.Align.CENTER)
        if (meta > 0) {
            val m = x0 + 60f
            val n = x1 - 60f
            caja(m, arriba + 555f, n, arriba + 581f, 13f, Oscuro.TARJETA2)
            val llena = (avance.coerceIn(0.0, 1.0) * (n - m)).toFloat()
            if (llena > 0) caja(m, arriba + 555f, m + maxOf(26f, llena), arriba + 581f, 13f, colBorde)
            texto("${(avance * 100).toInt().coerceIn(0, 999)} % de ${dinero(meta)}", cx, arriba + 640f, Fuentes.numero, 33f,
                Oscuro.TENUE, 0.06f, Paint.Align.CENTER)
        }

        // Ofertas que viste, en sus colores, centradas.
        val inicio = if (semana) Jornada.inicioDeSemana() else Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
        }.timeInMillis
        val ofertas = Historial.ultimosDias(ctx, 8).filter { it.hora >= inicio }
        val filas = listOf(
            Triple(Tema.VERDE, ofertas.count { it.nivel == Oferta.VERDE }, "convenían"),
            Triple(Oscuro.AMARILLO, ofertas.count { it.nivel == Oferta.AMARILLO }, "regulares"),
            Triple(Tema.ROJO, ofertas.count { it.nivel == Oferta.ROJO }, "no")
        )
        val anchoFilas = filas.sumOf { (_, n, e) ->
            (41f + medir(n.toString(), Fuentes.numero, 36f) + 12f + medir(e, Fuentes.texto, 36f)).toDouble()
        }.toFloat() + 43f * (filas.size - 1)
        val yf = abajo + 100f
        var x = cx - anchoFilas / 2f
        filas.forEach { (col, n, e) ->
            p.style = Paint.Style.FILL
            p.color = col
            c.drawCircle(x + 13f, yf - 12f, 13f, p)
            x += 41f
            texto(n.toString(), x, yf, Fuentes.numero, 36f, Oscuro.HUESO)
            x += medir(n.toString(), Fuentes.numero, 36f) + 12f
            texto(e, x, yf, Fuentes.texto, 36f, Oscuro.TEXTO2)
            x += medir(e, Fuentes.texto, 36f) + 43f
        }

        // Pie: semáforo y «Checa» centrados, frase y dirección.
        val wMarca = medir("Checa", Fuentes.titulo, 65f)
        val xm = cx - (50f + 22f + wMarca) / 2f
        ctx.getDrawable(R.drawable.semaforo)?.apply { setBounds(xm.toInt(), 1090, xm.toInt() + 50, 1173); draw(c) }
        texto("Checa", xm + 72f, 1153f, Fuentes.titulo, 65f, Oscuro.HUESO, -0.02f)
        texto("Mira cuánto te queda de cada viaje.", cx, 1236f, Fuentes.texto, 36f, Oscuro.TEXTO2, 0f, Paint.Align.CENTER)
        texto("jeal1498.github.io/PWA-Checa-Multimedia", cx, 1283f, Fuentes.numero, 30f, Oscuro.TENUE, 0f, Paint.Align.CENTER)
        return bmp
    }
}
