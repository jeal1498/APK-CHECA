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

    /** Ya se le mostró la pantalla de permisos en este equipo. */
    var permisosVistos: Boolean
        get() = sp.getBoolean("permisosVistos", false)
        set(v) { sp.edit().putBoolean("permisosVistos", v).apply() }

    /** Ya vio (u omitió) la guía de primer uso. */
    var guiaVista: Boolean
        get() = sp.getBoolean("guiaVista", false)
        set(v) { sp.edit().putBoolean("guiaVista", v).apply() }

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
        get() = sp.getFloat("recMaxV", 2f).coerceAtMost(4f)  // 5 km ya no es opción (v62)
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
        listOf(filtroKm, filtroHora, filtroRecMax, filtroDurMax).count { it }

    /** Cómo elegir la mejor cuando hay dos ofertas: 0 neta/hora, 1 neta/km, 2 neta total. */
    var criterio: Int
        get() = sp.getInt("criterio", 0)
        set(v) { sp.edit().putInt("criterio", v).apply() }

    // ---------- Gasolina ----------
    var mostrarNeta: Boolean
        get() = sp.getBoolean("neta", true)
        set(v) { sp.edit().putBoolean("neta", v).apply() }

    var kmPorLitro: Float
        get() = sp.getFloat("kml", 12f)
        set(v) { sp.edit().putFloat("kml", v).apply() }

    var precioLitro: Float
        get() = sp.getFloat("precioL", 24f)
        set(v) { sp.edit().putFloat("precioL", v).apply() }

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

    /** true = dice "Aceptar / Considerar / Rechazar"; false = tonos. */
    var voz: Boolean
        get() = sp.getBoolean("voz", true)
        set(v) { sp.edit().putBoolean("voz", v).apply() }

    /** Volumen del sonido, 10–100. */
    var volumen: Int
        get() = sp.getInt("vol", 80)
        set(v) { sp.edit().putInt("vol", v).apply() }

    var diagnostico: Boolean
        get() = sp.getBoolean("diag", true)
        set(v) { sp.edit().putBoolean("diag", v).apply() }

    /** Ya vio el aviso de qué guarda «Guardar pantallas de ofertas». */
    var diagAvisoVisto: Boolean
        get() = sp.getBoolean("diagAviso", false)
        set(v) { sp.edit().putBoolean("diagAviso", v).apply() }

    /** Se pausó el lector desde «Apps de banco» (la pantalla de permisos lo explica al volver). */
    var lectorPausado: Boolean
        get() = sp.getBoolean("lectorPausado", false)
        set(v) { sp.edit().putBoolean("lectorPausado", v).apply() }

    /** Cuándo salió el último pop-up (para «Leyendo · última oferta hace 2 min»). */
    var ultimaOferta: Long
        get() = sp.getLong("ultOferta", 0L)
        set(v) { sp.edit().putLong("ultOferta", v).apply() }

    /** Meta de ganancia neta del día en pesos (0 = sin meta). */
    var metaDia: Float
        get() = sp.getFloat("metaDia", 0f)
        set(v) { sp.edit().putFloat("metaDia", v).apply() }

    /** Avisar con una notificación al abrir Uber o DiDi si la lectura está apagada. */
    var avisarApertura: Boolean
        get() = sp.getBoolean("avisoApertura", true)
        set(v) { sp.edit().putBoolean("avisoApertura", v).apply() }

    /** Cuándo se avisó por última vez al abrir Uber/DiDi (para no repetirlo cada rato). */
    var ultimoAvisoApertura: Long
        get() = sp.getLong("ultAvisoApertura", 0L)
        set(v) { sp.edit().putLong("ultAvisoApertura", v).apply() }

    /** Pedir opinión: cuándo volver a preguntar, cuántas veces se ha preguntado y si ya la dio. */
    var resenaProxima: Long
        get() = sp.getLong("resenaProx", 0L)
        set(v) { sp.edit().putLong("resenaProx", v).apply() }

    var resenaPedida: Int
        get() = sp.getInt("resenaPedida", 0)
        set(v) { sp.edit().putInt("resenaPedida", v).apply() }

    var resenaHecha: Boolean
        get() = sp.getBoolean("resenaHecha", false)
        set(v) { sp.edit().putBoolean("resenaHecha", v).apply() }

    /** Último aviso de fin de beta mostrado en ventana: 30 o 7 (días que quedaban). */
    var avisoFinBeta: Int
        get() = sp.getInt("avisoFinBeta", 0)
        set(v) { sp.edit().putInt("avisoFinBeta", v).apply() }

    /** Último texto leído de una oferta (solo con «Guardar pantallas de ofertas»). */
    var ultimaLectura: String
        get() = sp.getString("ultima", "") ?: ""
        set(v) { sp.edit().putString("ultima", v).apply() }

    /** Borra lo leído que queda en ajustes (al borrar todo lo guardado o cerrar sesión). */
    fun borrarDiagnostico() { sp.edit().remove("caps").remove("ultima").remove("ultOferta").apply() }

    /** Ajustes que viajan con la cuenta (los demás son de este equipo: guía, capturas, avisos). */
    fun exportar(): org.json.JSONObject {
        val o = org.json.JSONObject()
        for (k in AJUSTES_FLOAT) if (sp.contains(k)) o.put(k, sp.getFloat(k, 0f).toDouble())
        for (k in AJUSTES_INT) if (sp.contains(k)) o.put(k, sp.getInt(k, 0))
        for (k in AJUSTES_BOOL) if (sp.contains(k)) o.put(k, sp.getBoolean(k, false))
        return o
    }

    /** Aplica los ajustes guardados en la cuenta. */
    fun importar(o: org.json.JSONObject) {
        val e = sp.edit()
        for (k in AJUSTES_FLOAT) if (o.has(k)) e.putFloat(k, o.getDouble(k).toFloat())
        for (k in AJUSTES_INT) if (o.has(k)) e.putInt(k, o.getInt(k))
        for (k in AJUSTES_BOOL) if (o.has(k)) e.putBoolean(k, o.getBoolean(k))
        e.commit()
    }

    companion object {
        private val AJUSTES_FLOAT = listOf("minKm", "minHora", "recMaxV", "recMinV", "kml", "precioL", "metaDia")
        private val AJUSTES_INT = listOf("durMaxV", "durMinV", "criterio", "tam", "dur", "pos", "vol")
        private val AJUSTES_BOOL = listOf(
            "fKm", "fHora", "fRecMax", "fRecMin", "fDurMax", "fDurMin", "neta", "uber", "didi",
            "vib", "son", "voz", "avisoApertura"
        )
        const val SEP = "\n\n==========\n\n"
        const val TAM_MIN = 10
        const val TAM_MAX = 40
    }
}
