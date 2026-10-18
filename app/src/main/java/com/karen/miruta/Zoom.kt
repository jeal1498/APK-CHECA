package com.karen.miruta

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Matrix
import android.graphics.RectF
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.widget.ImageView

/** Foto que se amplía con dos dedos (o doble toque) y se mueve con un dedo. */
class ZoomImagen(ctx: Context) : ImageView(ctx) {
    private val m = Matrix()
    private var escala = 1f

    private val pinza = ScaleGestureDetector(ctx, object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
        override fun onScale(d: ScaleGestureDetector): Boolean {
            val nueva = (escala * d.scaleFactor).coerceIn(1f, 5f)
            val f = nueva / escala
            escala = nueva
            m.postScale(f, f, d.focusX, d.focusY)
            limitar()
            return true
        }
    })

    private val gestos = GestureDetector(ctx, object : GestureDetector.SimpleOnGestureListener() {
        override fun onScroll(e1: MotionEvent?, e2: MotionEvent, dx: Float, dy: Float): Boolean {
            if (escala > 1f) { m.postTranslate(-dx, -dy); limitar() }
            return true
        }

        override fun onDoubleTap(e: MotionEvent): Boolean {
            if (escala > 1f) ajustar() else {
                escala = 2.5f
                m.postScale(2.5f, 2.5f, e.x, e.y)
                limitar()
            }
            return true
        }
    })

    init { scaleType = ScaleType.MATRIX }

    override fun setImageBitmap(bm: Bitmap?) {
        super.setImageBitmap(bm)
        ajustar()
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        ajustar()
    }

    /** La foto completa, centrada, sin ampliar. */
    private fun ajustar() {
        val d = drawable ?: return
        if (width == 0 || height == 0 || d.intrinsicWidth <= 0) return
        val s = minOf(width.toFloat() / d.intrinsicWidth, height.toFloat() / d.intrinsicHeight)
        m.reset()
        m.postScale(s, s)
        m.postTranslate((width - d.intrinsicWidth * s) / 2f, (height - d.intrinsicHeight * s) / 2f)
        escala = 1f
        imageMatrix = m
    }

    /** No deja que la foto se salga de la vista. */
    private fun limitar() {
        val d = drawable ?: return
        val r = RectF(0f, 0f, d.intrinsicWidth.toFloat(), d.intrinsicHeight.toFloat())
        m.mapRect(r)
        val dx = if (r.width() <= width) (width - r.width()) / 2f - r.left
            else if (r.left > 0) -r.left else if (r.right < width) width - r.right else 0f
        val dy = if (r.height() <= height) (height - r.height()) / 2f - r.top
            else if (r.top > 0) -r.top else if (r.bottom < height) height - r.bottom else 0f
        m.postTranslate(dx, dy)
        imageMatrix = m
    }

    override fun onTouchEvent(e: MotionEvent): Boolean {
        parent?.requestDisallowInterceptTouchEvent(escala > 1f || e.pointerCount > 1)
        pinza.onTouchEvent(e)
        gestos.onTouchEvent(e)
        return true
    }
}
