package com.karen.miruta

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.speech.tts.TextToSpeech
import java.util.Locale

/** Vibración y sonido distintos según el semáforo. */
class Alertas(private val ctx: Context, private val prefs: Prefs) {
    private val handler = Handler(Looper.getMainLooper())

    companion object {
        // Un solo motor de voz para toda la app (tarda en arrancar).
        @Volatile private var tts: TextToSpeech? = null
        @Volatile private var listo = false
        @Volatile private var pendiente: Pair<String, Float>? = null
    }

    fun avisar(nivel: Int) {
        if (prefs.vibrar) vibrar(nivel)
        if (prefs.sonido) {
            if (prefs.voz) decir(nivel) else sonar(nivel)
        }
    }

    private fun decir(nivel: Int) {
        val frase = when (nivel) {
            Oferta.VERDE -> "Aceptar"
            Oferta.AMARILLO -> "Considerar"
            else -> "Rechazar"
        }
        val vol = prefs.volumen / 100f
        val t = tts
        if (t != null && listo) {
            hablar(t, frase, vol)
            return
        }
        pendiente = frase to vol
        if (t == null) {
            tts = TextToSpeech(ctx.applicationContext) init@{ estado ->
                val motor = tts ?: return@init
                if (estado != TextToSpeech.SUCCESS) { tts = null; return@init }
                val mx = motor.setLanguage(Locale("es", "MX"))
                if (mx == TextToSpeech.LANG_MISSING_DATA || mx == TextToSpeech.LANG_NOT_SUPPORTED) {
                    motor.setLanguage(Locale("es"))
                }
                motor.setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ASSISTANCE_NAVIGATION_GUIDANCE)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                        .build()
                )
                listo = true
                pendiente?.let { (f, v) -> hablar(motor, f, v) }
                pendiente = null
            }
        }
    }

    private fun hablar(t: TextToSpeech, frase: String, vol: Float) {
        try {
            val p = android.os.Bundle().apply { putFloat(TextToSpeech.Engine.KEY_PARAM_VOLUME, vol) }
            t.speak(frase, TextToSpeech.QUEUE_FLUSH, p, "aviso")
        } catch (_: Exception) { }
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
