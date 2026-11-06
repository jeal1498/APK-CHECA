package com.karen.miruta

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build

/**
 * Estado de la lectura, sea cual sea la forma de capturar:
 * - Android 11+: el servicio de accesibilidad toma la imagen solo cuando hace falta
 *   (sin permiso de captura cada vez, sin pantalla virtual y sigue al bloquear la pantalla).
 * - Respaldo: [CapturaService] con la captura de pantalla de Android.
 */
object Lectura {
    /** true si en este teléfono se lee con el servicio de accesibilidad. */
    fun porAccesibilidad(ctx: Context): Boolean =
        Build.VERSION.SDK_INT >= 30 && !Prefs(ctx).capturaProyeccion

    val corriendo: Boolean
        get() = CapturaService.corriendo || RutaService.instancia?.leyendo == true

    /** Android cortó la lectura sin que el conductor la detuviera. */
    val cortada: Boolean
        get() = !corriendo && (CapturaService.cortada || RutaService.cortada)

    /** Apaga la lectura (las dos formas). */
    fun detener(ctx: Context) {
        Prefs(ctx).lecturaActiva = false
        RutaService.instancia?.detenerLectura()
        if (CapturaService.corriendo) {
            try {
                ctx.startService(Intent(ctx, CapturaService::class.java).setAction(CapturaService.ACCION_DETENER))
            } catch (_: Exception) { }
        }
        WidgetCheca.actualizar(ctx)
    }
}

/** «Detener» de la notificación y del widget (una difusión no tiene límites en segundo plano). */
class DetenerLecturaReceiver : BroadcastReceiver() {
    override fun onReceive(ctx: Context, intent: Intent?) = Lectura.detener(ctx)

    companion object {
        const val ACCION = "com.karen.miruta.DETENER_LECTURA"
    }
}
