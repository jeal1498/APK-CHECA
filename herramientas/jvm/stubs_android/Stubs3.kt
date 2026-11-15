package com.karen.miruta
import android.content.Context
object BuildConfig { const val VERSION_NAME = "10.81" }
class Prefs(c: Context? = null) {
    var trazabilidad = false
    val kmPorLitro = 12.0; val precioLitro = 24.0; val desgastePorKm = 0.0; val tarMinNeta = true
    val filtroRecMax = false; val recogidaMaxKm = 3; val filtroDurMax = false; val duracionMax = 30
    val filtroTarMin = false; val tarifaMinima = 50; val filtroKm = true; val minPorKm = 10
    val filtroHora = true; val minPorHora = 200
}
object Cuenta { fun conectada(c: Context) = false; fun enviarTrazas(c: Context, s: String): Pair<Int, String>? = null }
