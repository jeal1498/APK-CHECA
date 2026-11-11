package com.karen.miruta

import android.content.Context
import android.content.SharedPreferences

class Prefs(context: Context) {
    private val sp: SharedPreferences =
        context.applicationContext.getSharedPreferences("cfg", Context.MODE_PRIVATE)

    var minPorKm: Float
        get() = sp.getFloat("minKm", 10f)
        set(v) { sp.edit().putFloat("minKm", v).apply() }

    var minPorHora: Float
        get() = sp.getFloat("minHora", 200f)
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
        listOf(filtroKm, filtroHora, filtroRecMax, filtroDurMax, filtroTarMin).count { it }

    /** Cómo elegir la mejor cuando hay dos ofertas: 0 neta/hora, 1 neta/km, 2 neta total. */
    var criterio: Int
        get() = sp.getInt("criterio", 0)
        set(v) { sp.edit().putInt("criterio", v).apply() }

    /** Código de la última versión cuyas novedades ya se mostraron (10.34). */
    var novedadesVista: Int
        get() = sp.getInt("novedadesVista", 0)
        set(v) { sp.edit().putInt("novedadesVista", v).apply() }

    /** Apariencia de la app (10.32): «auto» (por defecto, sigue al teléfono), «oscuro» o «claro». */
    var tema: String
        get() = sp.getString("tema", Tema.MODO_AUTO) ?: Tema.MODO_AUTO
        set(v) { sp.edit().putString("tema", v).apply() }

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

    /** El conductor ya revisó su rendimiento y su gasolina (o los cambió de los de fábrica, 12 km/l y $24/l). */
    var gasRevisada: Boolean
        get() = sp.getBoolean("gas_revisada", false) || kmPorLitro != 12f || precioLitro != 24f
        set(v) { sp.edit().putBoolean("gas_revisada", v).apply() }

    /** Monto directo de desgaste por km (el de siempre). Solo cuenta si no se llena ningún concepto. */
    var desgasteDirecto: Float
        get() = sp.getFloat("desgKm", 0f)
        set(v) { sp.edit().putFloat("desgKm", v).apply() }

    /**
     * Desgaste del auto por km (llantas, seguro, licencia…). Es la suma de los conceptos de
     * [Desgaste]; si no hay ninguno, el monto directo. 0 = no se descuenta.
     */
    var desgastePorKm: Float
        get() = Desgaste.total(this).let { if (it > 0) it.toFloat() else desgasteDirecto }
        set(v) { desgasteDirecto = v }

    /** Dato numérico suelto (conceptos de desgaste y sus km). */
    fun flotante(clave: String, defecto: Float = 0f): Float = sp.getFloat(clave, defecto)
    fun ponerFlotante(clave: String, v: Float) { sp.edit().putFloat(clave, v).apply() }

    // ---------- Tarifa mínima (10.12): lo menos que se acepta por un viaje ----------
    var filtroTarMin: Boolean
        get() = sp.getBoolean("fTarMin", false)
        set(v) { sp.edit().putBoolean("fTarMin", v).apply() }

    var tarifaMinima: Float
        get() = sp.getFloat("tarMin", 50f)
        set(v) { sp.edit().putFloat("tarMin", v).apply() }

    /** true: cuenta la ganancia neta (después de gasolina y desgaste); false: la tarifa que muestra la app. */
    var tarMinNeta: Boolean
        get() = sp.getBoolean("tarMinNeta", true)
        set(v) { sp.edit().putBoolean("tarMinNeta", v).apply() }

    // ---------- Moneda (la de la cuenta vive en perfil_conductor, compartida con Clientela) ----------
    var moneda: Moneda
        get() = Moneda(
            sp.getString("monCod", null) ?: "MXN",
            sp.getString("monSim", null) ?: "$",
            sp.getBoolean("monDesp", false),
            sp.getString("monMil", null) ?: ",",
            sp.getString("monDec", null) ?: ".",
            sp.getInt("monDig", 0)
        ).normalizada()
        set(v) {
            val m = v.normalizada()
            sp.edit().putString("monCod", m.codigo).putString("monSim", m.simbolo).putBoolean("monDesp", m.despues)
                .putString("monMil", m.miles).putString("monDec", m.decimal).putInt("monDig", m.decimales).apply()
        }

    /** La moneda se cambió en este equipo y falta subirla a la cuenta. */
    /** País elegido (código ISO, «MX»). Vacío = se deduce de la moneda (ver [Pais.actual]). */
    var pais: String
        get() = sp.getString("pais", "") ?: ""
        set(v) { sp.edit().putString("pais", v).apply() }

    var monedaPendiente: Boolean
        get() = sp.getBoolean("monPend", false)
        set(v) { sp.edit().putBoolean("monPend", v).apply() }

    /** Tamaño del pop-up en %: escala todo (ancho, letra y márgenes). 100 = el de siempre. */
    var escalaPopup: Int
        get() = sp.getInt("escPop", 100).coerceIn(ESC_MIN, ESC_MAX)
        set(v) { sp.edit().putInt("escPop", v.coerceIn(ESC_MIN, ESC_MAX)).apply() }

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
        get() = sp.getBoolean("son", true)
        set(v) { sp.edit().putBoolean("son", v).apply() }

    /** true = dice "Aceptar / Considerar / Rechazar"; false = tonos. */
    var voz: Boolean
        get() = sp.getBoolean("voz", true)
        set(v) { sp.edit().putBoolean("voz", v).apply() }

    /** Volumen del sonido, 10–100. */
    var volumen: Int
        get() = sp.getInt("vol", 100)
        set(v) { sp.edit().putInt("vol", v).apply() }

    var diagnostico: Boolean
        // En Google Play el guardado de capturas/texto es opcional y empieza apagado.
        // El canal directo conserva su comportamiento actual.
        get() = sp.getBoolean("diag", !BuildConfig.ES_PLAY)
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
    /** El conductor dejó la lectura encendida (por accesibilidad se reanuda sola al reconectar el servicio). */
    var lecturaActiva: Boolean
        get() = sp.getBoolean("lecturaActiva", false)
        set(v) { sp.edit().putBoolean("lecturaActiva", v).apply() }

    /** La captura por accesibilidad falló en este teléfono: se usa la captura de pantalla de Android. */
    var capturaProyeccion: Boolean
        get() = sp.getBoolean("capturaProyeccion", false)
        set(v) { sp.edit().putBoolean("capturaProyeccion", v).apply() }

    var avisarApertura: Boolean
        get() = sp.getBoolean("avisoApertura", true)
        set(v) { sp.edit().putBoolean("avisoApertura", v).apply() }

    /** 10.40: al abrir Uber o DiDi, Checa enciende sola la lectura y avisa «Lista para trabajar». */
    var inicioAutomatico: Boolean
        get() = sp.getBoolean("inicioAuto", true)
        set(v) { sp.edit().putBoolean("inicioAuto", v).apply() }

    /** Contacto del conductor para el QR de pasajeros (10.42): nombre y número (solo dígitos, con lada). */
    var contactoNombre: String
        get() = sp.getString("contactoNombre", "") ?: ""
        set(v) { sp.edit().putString("contactoNombre", v).apply() }
    var contactoTel: String
        get() = sp.getString("contactoTel", "") ?: ""
        set(v) { sp.edit().putString("contactoTel", v).apply() }

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
        // Textos (10.39): apariencia y país. Vacío = sin elegir, no se sube.
        for (k in AJUSTES_TEXTO) sp.getString(k, "")?.takeIf { it.isNotEmpty() }?.let { o.put(k, it) }
        return o
    }

    /** Aplica los ajustes guardados en la cuenta. */
    fun importar(o: org.json.JSONObject) {
        val e = sp.edit()
        for (k in AJUSTES_FLOAT) if (o.has(k)) e.putFloat(k, o.getDouble(k).toFloat())
        for (k in AJUSTES_INT) if (o.has(k)) e.putInt(k, o.getInt(k))
        for (k in AJUSTES_BOOL) if (o.has(k)) e.putBoolean(k, o.getBoolean(k))
        // Solo valores conocidos: una cuenta con datos raros no debe romper la apariencia ni el país.
        o.optString("tema").takeIf { it in listOf(Tema.MODO_AUTO, Tema.MODO_CLARO, Tema.MODO_OSCURO) }?.let { e.putString("tema", it) }
        o.optString("pais").takeIf { Pais.de(it) != null }?.let { e.putString("pais", it) }
        // WhatsApp del conductor para el QR (10.42): viaja con la cuenta de Google (el nombre sale de la cuenta).
        o.optString("contactoTel").takeIf { it.length in 11..15 && it.all { c -> c.isDigit() } }?.let { e.putString("contactoTel", it) }
        e.commit()
    }

    companion object {
        private val AJUSTES_FLOAT = listOf("minKm", "minHora", "recMaxV", "recMinV", "kml", "precioL", "metaDia", "desgKm", "tarMin") +
            Desgaste.CLAVES
        private val AJUSTES_TEXTO = listOf("tema", "pais", "contactoTel")
        private val AJUSTES_INT = listOf("durMaxV", "durMinV", "criterio", "tam", "dur", "pos", "vol", "escPop")
        private val AJUSTES_BOOL = listOf(
            "fKm", "fHora", "fRecMax", "fRecMin", "fDurMax", "fDurMin", "fTarMin", "tarMinNeta", "neta", "uber", "didi",
            "vib", "son", "voz", "avisoApertura"
        )
        const val SEP = "\n\n==========\n\n"
        const val ESC_MIN = 70
        const val ESC_MAX = 160
    }
}
