package com.karen.miruta

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.view.View
import android.widget.RemoteViews
import java.util.Locale

/** Mi día 3×1: lo que llevas hoy (neto, viajes) y el avance de tu meta. */
class WidgetDia : AppWidgetProvider() {
    override fun onUpdate(ctx: Context, manager: AppWidgetManager, ids: IntArray) {
        for (id in ids) manager.updateAppWidget(id, Widgets.dia(ctx))
    }
}

/** Descanso 3×1: horas manejando seguidas y aviso a las 4 h. */
class WidgetDescanso : AppWidgetProvider() {
    override fun onUpdate(ctx: Context, manager: AppWidgetManager, ids: IntArray) {
        for (id in ids) manager.updateAppWidget(id, Widgets.descanso(ctx))
    }
}

/** SOS 1×1: un toque abre la pantalla de SOS. */
class WidgetSos : AppWidgetProvider() {
    override fun onUpdate(ctx: Context, manager: AppWidgetManager, ids: IntArray) {
        for (id in ids) manager.updateAppWidget(id, Widgets.sos(ctx))
    }
}

/** Mosaico 4×2: Mi día, Descanso, encender/apagar y SOS en una sola pieza. */
class WidgetMosaico : AppWidgetProvider() {
    override fun onUpdate(ctx: Context, manager: AppWidgetManager, ids: IntArray) {
        for (id in ids) manager.updateAppWidget(id, Widgets.mosaico(ctx))
    }
}

/** Mi QR 1×1: un toque abre el pop-up con el QR de contacto para los pasajeros. */
class WidgetQr : AppWidgetProvider() {
    override fun onUpdate(ctx: Context, manager: AppWidgetManager, ids: IntArray) {
        for (id in ids) manager.updateAppWidget(id, Widgets.qr(ctx))
    }
}

/**
 * Widgets de la pantalla de inicio (10.41). Los datos salen de lo que ya existe: «Mi día»
 * (`Jornada` y la meta), `Manejo` (tiempo seguido) y `Lectura` (encendida o apagada).
 * Las mismas vistas se usan en el aviso de Novedades para que Eduardo las vea antes de agregarlas.
 */
object Widgets {
    private val ASFALTO = Color.rgb(12, 12, 16)
    private val CAMPO = Color.rgb(30, 30, 37)
    private val HUESO = Color.rgb(244, 244, 239)
    private val GRIS = Color.rgb(163, 163, 173)
    private val VERDE = Color.rgb(34, 197, 94)
    private val AMARILLO = Color.rgb(250, 204, 21)

    private var ultimaActualizacion = 0L

    /** Repinta todos los widgets nuevos (viaje registrado, meta, lectura encendida o apagada). */
    fun actualizar(ctx: Context) {
        try {
            val app = ctx.applicationContext
            val m = AppWidgetManager.getInstance(app)
            fun repintar(clase: Class<*>, vista: () -> RemoteViews) {
                val ids = m.getAppWidgetIds(ComponentName(app, clase))
                if (ids.isEmpty()) return
                val v = vista()
                for (id in ids) m.updateAppWidget(id, v)
            }
            repintar(WidgetDia::class.java) { dia(app) }
            repintar(WidgetDescanso::class.java) { descanso(app) }
            repintar(WidgetSos::class.java) { sos(app) }
            repintar(WidgetMosaico::class.java) { mosaico(app) }
        } catch (_: Exception) { }
    }

    /** Como [actualizar], pero como mucho una vez por minuto (el servicio lo llama en cada evento). */
    fun actualizarSiToca(ctx: Context, ahora: Long) {
        if (ahora - ultimaActualizacion < 60_000L) return
        ultimaActualizacion = ahora
        actualizar(ctx)
    }

    /** Pide a la pantalla de inicio que agregue el widget. false si el teléfono no lo permite. */
    fun fijar(ctx: Context, clase: Class<*>): Boolean = try {
        val m = AppWidgetManager.getInstance(ctx)
        m.isRequestPinAppWidgetSupported && m.requestPinAppWidget(ComponentName(ctx, clase), null, null)
    } catch (_: Exception) { false }

    // ------------------------------------------------------------------
    // Vistas completas
    // ------------------------------------------------------------------

    fun dia(ctx: Context): RemoteViews {
        val v = RemoteViews(ctx.packageName, R.layout.widget_dia)
        pintarDia(ctx, v)
        v.setOnClickPendingIntent(R.id.w_dia, abrirApp(ctx))
        return v
    }

    fun descanso(ctx: Context): RemoteViews {
        val v = RemoteViews(ctx.packageName, R.layout.widget_descanso)
        pintarDescanso(ctx, v, true)
        v.setOnClickPendingIntent(R.id.w_descanso, abrirApp(ctx))
        return v
    }

    fun sos(ctx: Context): RemoteViews {
        val v = RemoteViews(ctx.packageName, R.layout.widget_sos)
        pintarSos(ctx, v)
        v.setOnClickPendingIntent(R.id.w_sos, abrirSos(ctx))
        return v
    }

    fun qr(ctx: Context): RemoteViews {
        val v = RemoteViews(ctx.packageName, R.layout.widget_qr)
        v.setOnClickPendingIntent(
            R.id.w_qr,
            PendingIntent.getActivity(
                ctx, 12, Intent(ctx, QrActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )
        )
        return v
    }

    fun mosaico(ctx: Context): RemoteViews {
        val v = RemoteViews(ctx.packageName, R.layout.widget_mosaico)
        pintarDia(ctx, v)
        pintarDescanso(ctx, v, false)
        pintarSos(ctx, v)
        pintarEncender(ctx, v)
        v.setOnClickPendingIntent(R.id.dia_sec, abrirApp(ctx))
        v.setOnClickPendingIntent(R.id.des_sec, abrirApp(ctx))
        v.setOnClickPendingIntent(R.id.mz_sos, abrirSos(ctx))
        return v
    }

    // ------------------------------------------------------------------
    // Secciones (sirven para el widget suelto y para el mosaico)
    // ------------------------------------------------------------------

    private fun pintarDia(ctx: Context, v: RemoteViews) {
        val prefs = Prefs(ctx)
        val hoy = Jornada.totales(Jornada.deHoy(ctx))
        val meta = prefs.metaDia.toDouble()
        val mon = prefs.moneda
        v.setTextViewText(R.id.dia_neta, mon.monto(hoy.neta, 0))
        v.setTextViewText(R.id.dia_viajes, "${hoy.viajes} ${if (hoy.viajes == 1) "viaje" else "viajes"}")
        if (meta > 0) {
            val pct = ((hoy.neta / meta) * 100).toInt().coerceIn(0, 100)
            v.setProgressBar(R.id.dia_barra, 100, pct, false)
            v.setViewVisibility(R.id.dia_barra, View.VISIBLE)
            v.setTextViewText(R.id.dia_meta, "$pct % de ${mon.monto(meta, 0)}")
        } else {
            v.setViewVisibility(R.id.dia_barra, View.INVISIBLE)
            v.setTextViewText(R.id.dia_meta, "Sin meta")
        }
    }

    /** [tarjeta]: widget suelto (la tarjeta cambia de borde); false: zona del mosaico (cambia el fondo). */
    private fun pintarDescanso(ctx: Context, v: RemoteViews, tarjeta: Boolean) {
        val min = Manejo.minutos(ctx)
        val aviso = min >= Manejo.LIMITE_MIN
        val fr = (min / Manejo.LIMITE_MIN.toFloat()).coerceIn(0f, 1f)
        v.setImageViewBitmap(R.id.des_anillo, anillo(ctx, fr, aviso, if (aviso) "!" else "${Math.round(fr * 100)}%"))
        v.setTextViewText(R.id.des_tiempo, if (aviso) "Descansa" else String.format(Locale.US, "%d h %02d", min / 60, min % 60))
        v.setTextViewText(R.id.des_sub, if (aviso) "Para 10 min" else "Aviso a las 4 h")
        v.setTextColor(R.id.des_tiempo, if (aviso) AMARILLO else HUESO)
        v.setTextColor(R.id.des_sub, if (aviso) AMARILLO else GRIS)
        if (tarjeta) {
            v.setInt(R.id.w_descanso, "setBackgroundResource", if (aviso) R.drawable.widget_tarjeta_aviso else R.drawable.widget_tarjeta)
        } else {
            v.setInt(R.id.des_sec, "setBackgroundResource", if (aviso) R.drawable.widget_zona_aviso else 0)
        }
    }

    private fun pintarSos(ctx: Context, v: RemoteViews) {
        v.setTextViewText(R.id.sos_num, Pais.actual(Prefs(ctx)).sos)
    }

    /** El mismo encender/apagar de `WidgetCheca`, dentro del mosaico. */
    private fun pintarEncender(ctx: Context, v: RemoteViews) {
        val flags = PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        if (Lectura.corriendo) {
            v.setImageViewResource(R.id.tg_icono, R.drawable.widget_foco_on)
            v.setTextColor(R.id.tg_estado, Color.WHITE)
            v.setTextViewText(R.id.tg_estado, "Leyendo")
            v.setOnClickPendingIntent(
                R.id.mz_tog,
                PendingIntent.getBroadcast(
                    ctx, 5, Intent(ctx, DetenerLecturaReceiver::class.java).setAction(DetenerLecturaReceiver.ACCION), flags
                )
            )
        } else {
            v.setImageViewResource(R.id.tg_icono, R.drawable.widget_foco_off)
            v.setTextColor(R.id.tg_estado, Color.parseColor("#E6E6EC"))
            v.setTextViewText(R.id.tg_estado, "Apagado")
            val abrir = Intent(ctx, MainActivity::class.java)
                .putExtra(MainActivity.EXTRA_AUTO_INICIAR, true)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            v.setOnClickPendingIntent(R.id.mz_tog, PendingIntent.getActivity(ctx, 6, abrir, flags))
        }
    }

    // ------------------------------------------------------------------
    // Anillo de progreso (RemoteViews no dibuja arcos: se pinta una imagen)
    // ------------------------------------------------------------------

    private fun anillo(ctx: Context, fraccion: Float, aviso: Boolean, texto: String): Bitmap {
        Fuentes.iniciar(ctx)
        val d = ctx.resources.displayMetrics.density
        val lado = (52 * d).toInt().coerceAtLeast(24)
        val bmp = Bitmap.createBitmap(lado, lado, Bitmap.Config.ARGB_8888)
        val c = Canvas(bmp)
        val trazo = lado * 0.11f
        val caja = RectF(trazo / 2f, trazo / 2f, lado - trazo / 2f, lado - trazo / 2f)
        val p = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = trazo
            strokeCap = Paint.Cap.ROUND
        }
        p.color = CAMPO
        c.drawArc(caja, 0f, 360f, false, p)
        if (fraccion > 0f) {
            p.color = if (aviso) AMARILLO else VERDE
            c.drawArc(caja, -90f, 360f * fraccion, false, p)
        }
        val t = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = HUESO
            textAlign = Paint.Align.CENTER
            typeface = Fuentes.numero
            textSize = lado * 0.24f
        }
        c.drawText(texto, lado / 2f, lado / 2f - (t.ascent() + t.descent()) / 2f, t)
        return bmp
    }

    // ------------------------------------------------------------------
    // Toques
    // ------------------------------------------------------------------

    private fun abrirApp(ctx: Context): PendingIntent = PendingIntent.getActivity(
        ctx, 10,
        Intent(ctx, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP),
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
    )

    private fun abrirSos(ctx: Context): PendingIntent = PendingIntent.getActivity(
        ctx, 11,
        Intent(ctx, MainActivity::class.java).putExtra(MainActivity.EXTRA_SOS, true)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP),
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
    )
}
