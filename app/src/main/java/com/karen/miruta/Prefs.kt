package com.karen.miruta

import android.content.Context
import android.content.SharedPreferences

class Prefs(context: Context) {
    private val sp: SharedPreferences =
        context.applicationContext.getSharedPreferences("cfg", Context.MODE_PRIVATE)

    var minPorKm: Float
        get() = sp.getFloat("minKm", 8f)
        set(v) { sp.edit().putFloat("minKm", v).apply() }

    var minPorHora: Float
        get() = sp.getFloat("minHora", 150f)
        set(v) { sp.edit().putFloat("minHora", v).apply() }

    // ---------- Filtros inteligentes: encendido + valor ----------
    private fun b(k: String, d: Boolean) = sp.getBoolean(k, d)
    private fun setB(k: String, v: Boolean) { sp.edit().putBoolean(k, v).apply() }

    var filtroKm: Boolean
        get() = b("fKm", true)
        set(v) = setB("fKm", v)

    var filtroHora: Boolean
        get() = b("fHora", true)
        set(v) = setB("fHora", v)

    var filtroRecMax: Boolean
        get() = b("fRecMax", false)
        set(v) = setB("fRecMax", v)

    var filtroRecMin: Boolean
        get() = b("fRecMin", false)
        set(v) = setB("fRecMin", v)

    var filtroDurMax: Boolean
        get() = b("fDurMax", false)
        set(v) = setB("fDurMax", v)

    var filtroDurMin: Boolean
        get() = b("fDurMin", false)
        set(v) = setB("fDurMin", v)


    /** Recogida en km. */
    var recogidaMaxKm: Float
        get() = sp.getFloat("recMaxV", 2f)
        set(v) { sp.edit().putFloat("recMaxV", v).apply() }

    var recogidaMinKm: Float
        get() = sp.getFloat("recMinV", 1f)
        set(v) { sp.edit().putFloat("recMinV", v).apply() }

    /** Duración del viaje (sin la recogida) en minutos. */
    var duracionMax: Int
        get() = sp.getInt("durMaxV", 30)
        set(v) { sp.edit().putInt("durMaxV", v).apply() }

    var duracionMin: Int
        get() = sp.getInt("durMinV", 10)
        set(v) { sp.edit().putInt("durMinV", v).apply() }

    fun filtrosActivos(): Int =
        listOf(filtroKm, filtroHora, filtroRecMax, filtroRecMin, filtroDurMax, filtroDurMin).count { it }

    /** Tamaño base del texto del pop-up, en sp. */
    var tamanoTexto: Int
        get() = sp.getInt("tam", 18)
        set(v) { sp.edit().putInt("tam", v.coerceIn(TAM_MIN, TAM_MAX)).apply() }

    var duracionSeg: Int
        get() = sp.getInt("dur", 12)
        set(v) { sp.edit().putInt("dur", v).apply() }

    /** Posición vertical del pop-up, en % de la altura de pantalla. */
    var posicion: Int
        get() = sp.getInt("pos", 8)
        set(v) { sp.edit().putInt("pos", v).apply() }

    var uber: Boolean
        get() = sp.getBoolean("uber", true)
        set(v) { sp.edit().putBoolean("uber", v).apply() }

    var didi: Boolean
        get() = sp.getBoolean("didi", true)
        set(v) { sp.edit().putBoolean("didi", v).apply() }

    var vibrar: Boolean
        get() = sp.getBoolean("vib", true)
        set(v) { sp.edit().putBoolean("vib", v).apply() }

    var sonido: Boolean
        get() = sp.getBoolean("son", false)
        set(v) { sp.edit().putBoolean("son", v).apply() }

    /** Volumen del sonido, 10–100. */
    var volumen: Int
        get() = sp.getInt("vol", 80)
        set(v) { sp.edit().putInt("vol", v).apply() }

    var diagnostico: Boolean
        get() = sp.getBoolean("diag", false)
        set(v) { sp.edit().putBoolean("diag", v).apply() }

    fun guardarCaptura(texto: String) {
        val previas = capturas().split(SEP).filter { it.isNotBlank() }
        val nuevas = (listOf(texto) + previas).take(8)
        sp.edit().putString("caps", nuevas.joinToString(SEP)).apply()
    }

    fun capturas(): String = sp.getString("caps", "") ?: ""

    fun borrarCapturas() { sp.edit().remove("caps").apply() }

    companion object {
        const val SEP = "\n\n==========\n\n"
        const val TAM_MIN = 10
        const val TAM_MAX = 40
    }
}
