package com.karen.miruta

import android.app.Activity
import android.app.AlertDialog
import com.google.android.play.core.appupdate.AppUpdateManager
import com.google.android.play.core.appupdate.AppUpdateManagerFactory
import com.google.android.play.core.appupdate.AppUpdateOptions
import com.google.android.play.core.install.InstallStateUpdatedListener
import com.google.android.play.core.install.model.AppUpdateType
import com.google.android.play.core.install.model.InstallStatus
import com.google.android.play.core.install.model.UpdateAvailability

/**
 * Solo canal `play` (10.56): al abrir Checa, si Google Play tiene una versión nueva, sale la
 * ventana de Play «Actualización disponible». Se descarga en segundo plano y, al terminar,
 * un aviso propio ofrece reiniciar para instalarla. Solo funciona con la app instalada desde
 * Play (con el APK del workflow Play dice que no hay nada).
 */
object ActualizacionPlay {
    private const val CODIGO = 4107
    /** Una sola ventana de Play por vez que se abre la app (no en cada regreso a la pantalla). */
    private var preguntado = false
    private var gestor: AppUpdateManager? = null
    private var escucha: InstallStateUpdatedListener? = null

    fun revisar(a: Activity) {
        if (!BuildConfig.ES_PLAY) return
        val g = gestor ?: try { AppUpdateManagerFactory.create(a.applicationContext) } catch (_: Exception) { null } ?: return
        gestor = g
        try {
            g.appUpdateInfo.addOnSuccessListener { info ->
                if (a.isFinishing) return@addOnSuccessListener
                when {
                    // Ya se descargó (la app estaba cerrada o en segundo plano): ofrecer reiniciar.
                    info.installStatus() == InstallStatus.DOWNLOADED -> listaParaInstalar(a, g)
                    !preguntado && info.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE &&
                        info.isUpdateTypeAllowed(AppUpdateType.FLEXIBLE) -> {
                        preguntado = true
                        escuchar(a, g)
                        @Suppress("DEPRECATION")
                        g.startUpdateFlowForResult(info, a, AppUpdateOptions.newBuilder(AppUpdateType.FLEXIBLE).build(), CODIGO)
                    }
                }
            }
        } catch (_: Exception) { }
    }

    private fun escuchar(a: Activity, g: AppUpdateManager) {
        if (escucha != null) return
        val e = InstallStateUpdatedListener { estado ->
            if (estado.installStatus() == InstallStatus.DOWNLOADED) {
                escucha?.let { g.unregisterListener(it) }
                escucha = null
                if (!a.isFinishing) listaParaInstalar(a, g)
            }
        }
        escucha = e
        g.registerListener(e)
    }

    private fun listaParaInstalar(a: Activity, g: AppUpdateManager) {
        try {
            AlertDialog.Builder(a)
                .setTitle("Actualización lista")
                .setMessage("La versión nueva de Checa ya se descargó. Reinicia para usarla (tarda unos segundos).")
                .setPositiveButton("Reiniciar") { _, _ -> try { g.completeUpdate() } catch (_: Exception) { } }
                .setNegativeButton("Después", null)
                .show()
        } catch (_: Exception) { }
    }
}
