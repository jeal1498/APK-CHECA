package com.karen.miruta

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.widget.RemoteViews

/**
 * Widget (1x1) de la pantalla de inicio: enciende o apaga la lectura de pantalla.
 * Apagar es directo. Encender abre Checa, que pide el permiso de captura (Android lo exige
 * cada vez) y regresa a la app anterior.
 */
class WidgetCheca : AppWidgetProvider() {
    override fun onUpdate(ctx: Context, manager: AppWidgetManager, ids: IntArray) {
        for (id in ids) manager.updateAppWidget(id, vista(ctx))
    }

    companion object {
        /** Repinta todos los widgets (se llama al iniciar o detener la lectura). */
        fun actualizar(ctx: Context) {
            try {
                val m = AppWidgetManager.getInstance(ctx)
                val ids = m.getAppWidgetIds(ComponentName(ctx, WidgetCheca::class.java))
                if (ids.isEmpty()) return
                val v = vista(ctx)
                for (id in ids) m.updateAppWidget(id, v)
            } catch (_: Exception) { }
        }

        private fun vista(ctx: Context): RemoteViews {
            val v = RemoteViews(ctx.packageName, R.layout.widget_checa)
            val flags = PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            if (CapturaService.corriendo) {
                v.setImageViewResource(R.id.icono, R.drawable.widget_foco_on)
                v.setTextColor(R.id.estado, Color.WHITE)
                v.setTextViewText(R.id.estado, "Leyendo")
                v.setOnClickPendingIntent(
                    R.id.raiz,
                    PendingIntent.getService(
                        ctx, 5, Intent(ctx, CapturaService::class.java).setAction(CapturaService.ACCION_DETENER), flags
                    )
                )
            } else {
                v.setImageViewResource(R.id.icono, R.drawable.widget_foco_off)
                v.setTextColor(R.id.estado, Color.parseColor("#E6E6EC"))
                v.setTextViewText(R.id.estado, "Apagado")
                val abrir = Intent(ctx, MainActivity::class.java)
                    .putExtra(MainActivity.EXTRA_AUTO_INICIAR, true)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                v.setOnClickPendingIntent(R.id.raiz, PendingIntent.getActivity(ctx, 6, abrir, flags))
            }
            return v
        }
    }
}
