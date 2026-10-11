package com.karen.miruta

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.view.View
import java.util.Locale

/** Gráfica de barras de $/km por hora del día (0–23). Las 3 mejores en verde. */
class Grafica(ctx: Context) : View(ctx) {
    private var valores = DoubleArray(24)
    private var mejores = emptySet<Int>()
    private val d = resources.displayMetrics.density
    private val barra = Paint(Paint.ANTI_ALIAS_FLAG)
    private val letra = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textAlign = Paint.Align.CENTER
        textSize = 11 * resources.displayMetrics.scaledDensity
    }
    private val r = RectF()

    fun poner(v: DoubleArray, top: Set<Int>) {
        valores = v
        mejores = top
        invalidate()
    }

    override fun onDraw(c: Canvas) {
        val ancho = width.toFloat()
        val abajo = height - 18 * d          // espacio para las horas
        val arriba = 18 * d                  // espacio para el valor de la mejor
        val max = valores.maxOrNull()?.takeIf { it > 0 } ?: 1.0
        val paso = ancho / 24f
        val hueco = paso * 0.22f
        val mejor = mejores.maxByOrNull { valores[it] }
        for (h in 0 until 24) {
            val v = valores[h]
            val alto = if (v > 0) ((abajo - arriba) * (v / max)).toFloat().coerceAtLeast(3 * d) else 2 * d
            r.set(h * paso + hueco, abajo - alto, (h + 1) * paso - hueco, abajo)
            barra.color = when {
                h in mejores -> Tema.VERDE
                v > 0 -> Color.rgb(70, 82, 76)
                else -> Tema.BORDE
            }
            c.drawRoundRect(r, 3 * d, 3 * d, barra)
            if (h == mejor) {
                letra.color = Tema.VERDE
                letra.isFakeBoldText = true
                c.drawText("$" + String.format(Locale.US, "%.2f", v), r.centerX().coerceIn(20 * d, ancho - 20 * d), r.top - 5 * d, letra)
            }
        }
        letra.color = Tema.TENUE
        letra.isFakeBoldText = false
        for (h in listOf(0, 6, 12, 18)) c.drawText("${h}h", h * paso + paso / 2, height - 4 * d, letra)
        c.drawText("23h", 23 * paso + paso / 2 - 4 * d, height - 4 * d, letra)
    }
}
