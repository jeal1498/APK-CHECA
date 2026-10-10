package com.karen.miruta

import android.content.Context
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager

/** Vibración y sonido distintos según el semáforo. */
class Alertas(private val ctx: Context, private val prefs: Prefs) {
    private val handler = Handler(Looper.getMainLooper())

    fun avisar(nivel: Int) {
        if (prefs.vibrar) vibrar(nivel)
        if (prefs.sonido) sonar(nivel)
    }

    private fun vibrar(nivel: Int) {
        // Conviene: 1 toque corto · Regular: 2 toques · No conviene: 1 largo
        val patron = when (nivel) {
            Oferta.VERDE -> longArrayOf(0, 120)
            Oferta.AMARILLO -> longArrayOf(0, 120, 120, 120)
            else -> longArrayOf(0, 600)
        }
        try {
            val v: Vibrator = if (Build.VERSION.SDK_INT >= 31) {
                (ctx.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager).defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                ctx.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
            }
            v.vibrate(VibrationEffect.createWaveform(patron, -1))
        } catch (_: Exception) { }
    }

    private fun sonar(nivel: Int) {
        val (tono, ms) = when (nivel) {
            Oferta.VERDE -> ToneGenerator.TONE_PROP_ACK to 300
            Oferta.AMARILLO -> ToneGenerator.TONE_PROP_BEEP2 to 400
            else -> ToneGenerator.TONE_SUP_ERROR to 600
        }
        try {
            val tg = ToneGenerator(AudioManager.STREAM_NOTIFICATION, prefs.volumen)
            tg.startTone(tono, ms)
            handler.postDelayed({ tg.release() }, ms + 300L)
        } catch (_: Exception) { }
    }
}
