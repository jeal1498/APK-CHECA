package com.karen.miruta

import android.app.Activity
import com.google.android.play.core.review.ReviewManagerFactory

/**
 * Solo canal `play`: pide la reseña con la ventana nativa de Google Play (In-App Review).
 * Solo cuando el conductor ya aceptó 10 viajes con Checa y como máximo una vez cada 60 días
 * (Google además limita cuántas veces la muestra; puede no salir). No lleva datos del conductor.
 */
object Resena {
    private const val MIN_VIAJES = 10
    private const val ESPERA_MS = 60L * 86_400_000L
    private var intentado = false

    fun pedir(a: Activity) {
        if (!BuildConfig.ES_PLAY || intentado) return
        intentado = true
        try {
            val sp = a.getSharedPreferences("resena", Activity.MODE_PRIVATE)
            val ahora = System.currentTimeMillis()
            if (ahora - sp.getLong("ultima", 0L) < ESPERA_MS) return
            if (Jornada.todos(a).size < MIN_VIAJES) return
            val gestor = ReviewManagerFactory.create(a.applicationContext)
            gestor.requestReviewFlow().addOnSuccessListener { info ->
                if (a.isFinishing) return@addOnSuccessListener
                sp.edit().putLong("ultima", ahora).apply()
                gestor.launchReviewFlow(a, info)
            }
        } catch (_: Exception) { }
    }
}
