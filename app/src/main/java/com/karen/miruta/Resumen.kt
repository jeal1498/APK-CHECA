package com.karen.miruta

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
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

    private fun dinero(v: Double) = "$" + String.format(Locale.US, "%,.0f", v)

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

    /** Día de la semana empezando en lunes (0) hasta domingo (6). */
    private fun diaLunes(ms: Long): Int {
        val c = Calendar.getInstance().apply { timeInMillis = ms }
        return (c.get(Calendar.DAY_OF_WEEK) + 5) % 7
    }

    /**
     * Marca v2 (maqueta https://claude.ai/artifact/79ZZqHqDcH6M5gBZfFca3r): asfalto y hueso, lo que te quedó en grande,
     * meta (día) o una barra por día (semana), tres datos, las ofertas en sus colores y la raya de carretera.
     */
    private fun dibujar(ctx: Context, t: Totales, viajes: List<Viaje>, semana: Boolean): Bitmap {
        val bmp = Bitmap.createBitmap(ANCHO, ALTO, Bitmap.Config.ARGB_8888)
        val c = Canvas(bmp)
        c.drawColor(Tema.ASFALTO)
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

        // Encabezado: semáforo, «Checa» y la fecha.
        ctx.getDrawable(R.drawable.semaforo)?.apply { setBounds(70, 65, 120, 148); draw(c) }
        texto("Checa", 144f, 128f, Fuentes.titulo, 65f, Tema.HUESO, -0.02f)
        texto(if (semana) rangoSemana() else fechaDia(Calendar.getInstance()), x1, 122f, Fuentes.numero, 35f, Tema.TENUE,
            0.08f, Paint.Align.RIGHT)

        // Lo que te quedó.
        texto(if (semana) "MI SEMANA" else "MI DÍA", x0, 229f, Fuentes.numero, 33f, Tema.TENUE, 0.14f)
        texto(dinero(t.neta), x0 - 4f, 368f, Fuentes.titulo, 157f, Tema.HUESO, -0.03f)
        texto("te quedaron después de gasolina", x0, 424f, Fuentes.texto, 41f, Tema.TEXTO2)
        var y = 424f

        if (semana) {
            // Una barra por día (lunes a domingo); el mejor día en hueso con su cifra.
            val netas = DoubleArray(7)
            viajes.forEach { netas[diaLunes(it.hora)] += it.neta }
            val max = netas.maxOrNull() ?: 0.0
            val mejor = if (max > 0) netas.indexOfFirst { it == max } else -1
            val hoy = diaLunes(System.currentTimeMillis())
            val alto = 173f
            val base = y + 43f + alto
            val gap = 26f
            val ancho = (x1 - x0 - 6 * gap) / 7
            for (i in 0..6) {
                val x = x0 + i * (ancho + gap)
                val h = if (max > 0) (netas[i].coerceAtLeast(0.0) / max * alto * 0.82).toFloat() else 0f
                val hh = h.coerceAtLeast(6f)
                p.style = Paint.Style.FILL
                p.color = if (i == mejor) Tema.HUESO else Tema.HUMO
                val r = minOf(12f, hh)
                c.drawPath(Path().apply {
                    addRoundRect(RectF(x, base - hh, x + ancho, base), floatArrayOf(r, r, r, r, 0f, 0f, 0f, 0f), Path.Direction.CW)
                }, p)
                if (i == mejor) texto(dinero(netas[i]), x + ancho / 2, base - hh - 14f, Fuentes.numero, 30f, Tema.HUESO,
                    alinear = Paint.Align.CENTER)
                texto(listOf("L", "M", "M", "J", "V", "S", "D")[i], x + ancho / 2, base + 66f, Fuentes.numero, 30f,
                    if (i == hoy) Tema.HUESO else Tema.TENUE, alinear = Paint.Align.CENTER)
            }
            caja(x0, base + 16f, x1, base + 18f, 0f, Tema.HUMO)
            y = base + 66f
        } else {
            val meta = Prefs(ctx).metaDia.toDouble()
            if (meta > 0) {
                val avance = (t.neta / meta).coerceIn(0.0, 1.0)
                texto("META ${dinero(meta)}", x0, y + 76f, Fuentes.numero, 33f, Tema.TENUE, 0.08f)
                texto("${(t.neta / meta * 100).toInt().coerceIn(0, 999)} %", x1, y + 76f, Fuentes.numero, 33f, Tema.TENUE,
                    0.08f, Paint.Align.RIGHT)
                caja(x0, y + 93f, x1, y + 119f, 13f, Tema.TARJETA2)
                if (avance > 0) caja(x0, y + 93f, x0 + maxOf(26f, ((x1 - x0) * avance).toFloat()), y + 119f, 13f, Tema.VERDE)
                y += 119f
            }
        }

        // Tres datos.
        val datos = listOf(
            "VIAJES" to t.viajes.toString(),
            "GASOLINA" to "−" + dinero(t.gasolina),
            "NETO POR KM" to ("$" + String.format(Locale.US, "%.2f", t.netaPorKm))
        )
        val top = y + 46f
        val w = (x1 - x0 - 48f) / 3f
        datos.forEachIndexed { i, (etq, valor) ->
            val x = x0 + i * (w + 24f)
            caja(x, top, x + w, top + 160f, 39f, Tema.TARJETA)
            texto(etq, x + 30f, top + 52f, Fuentes.numero, 25f, Tema.TENUE, 0.1f)
            texto(valor, x + 30f, top + 122f, Fuentes.numero, if (valor.length > 7) 40f else 48f, Tema.HUESO)
        }
        y = top + 160f

        // Ofertas que viste, en sus colores (el color solo para decidir).
        val inicio = if (semana) Jornada.inicioDeSemana() else Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
        }.timeInMillis
        val ofertas = Historial.ultimosDias(ctx, 8).filter { it.hora >= inicio }
        val base = y + 75f
        var x = x0
        listOf(
            Triple(Tema.VERDE, ofertas.count { it.nivel == Oferta.VERDE }, "convenían"),
            Triple(Tema.AMARILLO, ofertas.count { it.nivel == Oferta.AMARILLO }, "regulares"),
            Triple(Tema.ROJO, ofertas.count { it.nivel == Oferta.ROJO }, "no")
        ).forEach { (col, n, etq) ->
            p.style = Paint.Style.FILL
            p.color = col
            c.drawCircle(x + 13f, base - 12f, 13f, p)
            x += 41f
            texto(n.toString(), x, base, Fuentes.numero, 36f, Tema.HUESO)
            x += medir(n.toString(), Fuentes.numero, 36f) + 12f
            texto(etq, x, base, Fuentes.texto, 36f, Tema.TEXTO2)
            x += medir(etq, Fuentes.texto, 36f) + 43f
        }

        // Pie: raya de carretera, frase y dirección.
        var xr = x0
        while (xr < x1 - 50f) {
            caja(xr, 1172f, minOf(xr + 65f, x1 - 40f), 1187f, 7.5f, Tema.AMARILLO)
            xr += 108f
        }
        p.color = Tema.VERDE
        c.drawCircle(x1 - 20f, 1179.5f, 20f, p)
        texto("Checa: mira cuánto te queda de cada viaje.", x0, 1238f, Fuentes.texto, 36f, Tema.TEXTO2)
        texto("jeal1498.github.io/PWA-Checa-Multimedia", x0, 1283f, Fuentes.numero, 33f, Tema.TENUE)
        return bmp
    }
}
