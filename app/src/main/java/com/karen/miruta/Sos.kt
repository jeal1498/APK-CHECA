package com.karen.miruta

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.Locale
import java.util.UUID

/** Contacto de confianza (tabla `contactos_sos`). [telefono] solo dígitos, con lada del país. */
data class ContactoSos(val id: String, val nombre: String, val telefono: String, val relacion: String)

/**
 * SOS: contactos de confianza, ubicación (solo al tocar SOS, en primer plano), WhatsApp y llamada.
 * Los contactos viven en `sos.json` y, con cuenta, en `contactos_sos` (Cuenta los sincroniza).
 */
object Sos {
    private const val ARCHIVO = "sos.json"
    private val principal = Handler(Looper.getMainLooper())

    private fun archivo(ctx: Context) = File(ctx.filesDir, ARCHIVO)

    fun contactos(ctx: Context): List<ContactoSos> = try {
        val f = archivo(ctx)
        if (!f.exists()) emptyList() else desdeJson(JSONArray(f.readText()))
    } catch (_: Exception) { emptyList() }

    fun desdeJson(a: JSONArray): List<ContactoSos> = (0 until a.length()).mapNotNull { i ->
        val o = a.optJSONObject(i) ?: return@mapNotNull null
        val tel = o.optString("telefono").filter { it.isDigit() }
        val nombre = o.optString("nombre").trim()
        if (nombre.isEmpty() || tel.length !in 10..15) null
        else ContactoSos(o.optString("id").ifEmpty { UUID.randomUUID().toString() }, nombre, tel,
            if (o.isNull("relacion")) "" else o.optString("relacion"))
    }

    fun aJson(l: List<ContactoSos>): JSONArray = JSONArray().apply {
        l.forEach {
            put(JSONObject().put("id", it.id).put("nombre", it.nombre).put("telefono", it.telefono)
                .put("relacion", if (it.relacion.isEmpty()) JSONObject.NULL else it.relacion))
        }
    }

    /** Guarda la lista; [local] = cambió en este teléfono y hay que subirla a la cuenta. */
    fun guardar(ctx: Context, l: List<ContactoSos>, local: Boolean = true) {
        try { archivo(ctx).writeText(aJson(l).toString()) } catch (_: Exception) { }
        if (local) Cuenta.contactosCambiaron(ctx)
    }

    fun borrarTodo(ctx: Context) { archivo(ctx).delete() }

    /** Deja solo dígitos; con 10 dígitos y pesos mexicanos le pone la lada 52. null si no sirve. */
    fun normalizarTelefono(t: String, moneda: Moneda): String? {
        var d = t.filter { it.isDigit() }
        if (d.length == 10 && moneda.codigo == "MXN") d = "52$d"
        return d.takeIf { it.length in 11..15 }
    }

    /** Número de emergencias según el país de la moneda. */
    fun emergencias(moneda: Moneda): String = when (moneda.codigo) {
        "COP" -> "123"
        "CLP" -> "133"
        "PEN" -> "105"
        "BOB", "GTQ" -> "110"
        "BRL" -> "190"
        "EUR" -> "112"
        else -> "911"
    }

    fun tienePermiso(ctx: Context) =
        ctx.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
            ctx.checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED

    /**
     * Ubicación actual, una sola vez (sin seguirla). Usa la última conocida si es de hace menos de 2 min;
     * si no, pide una nueva y espera hasta 10 s. null si no hay permiso o no se pudo.
     */
    @SuppressLint("MissingPermission")
    fun ubicacion(ctx: Context, listo: (Location?) -> Unit) {
        if (!tienePermiso(ctx)) { listo(null); return }
        val lm = ctx.getSystemService(Context.LOCATION_SERVICE) as? LocationManager ?: run { listo(null); return }
        val proveedores = listOf(LocationManager.GPS_PROVIDER, LocationManager.NETWORK_PROVIDER, LocationManager.PASSIVE_PROVIDER)
            .filter { try { lm.isProviderEnabled(it) } catch (_: Exception) { false } }
        val ultima = proveedores.mapNotNull { try { lm.getLastKnownLocation(it) } catch (_: Exception) { null } }
            .maxByOrNull { it.time }
        if (ultima != null && System.currentTimeMillis() - ultima.time < 120_000) { listo(ultima); return }
        val vivos = proveedores.filter { it != LocationManager.PASSIVE_PROVIDER }
        if (vivos.isEmpty()) { listo(ultima); return }
        var hecho = false
        val listeners = mutableListOf<LocationListener>()
        fun terminar(l: Location?) {
            if (hecho) return
            hecho = true
            listeners.forEach { try { lm.removeUpdates(it) } catch (_: Exception) { } }
            listo(l ?: ultima)
        }
        for (p in vivos) {
            val ll = object : LocationListener {
                override fun onLocationChanged(location: Location) = terminar(location)
                @Deprecated("API vieja") override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}
                override fun onProviderEnabled(provider: String) {}
                override fun onProviderDisabled(provider: String) {}
            }
            listeners.add(ll)
            try { lm.requestLocationUpdates(p, 0L, 0f, ll, Looper.getMainLooper()) } catch (_: Exception) { }
        }
        principal.postDelayed({ terminar(null) }, 10_000)
    }

    fun mensaje(l: Location?): String {
        val donde = if (l == null) "No pude obtener mi ubicación; llámame."
        else "Mi ubicación: https://maps.google.com/?q=" +
            String.format(Locale.US, "%.6f,%.6f", l.latitude, l.longitude) +
            (if (l.hasAccuracy()) " (±${l.accuracy.toInt()} m)" else "")
        return "🆘 Necesito ayuda, estoy manejando. $donde Enviado desde Checa."
    }

    /** Abre WhatsApp con el mensaje listo para ese contacto. */
    fun whatsapp(ctx: Context, c: ContactoSos, texto: String): Boolean = try {
        ctx.startActivity(Intent(Intent.ACTION_VIEW,
            Uri.parse("https://wa.me/${c.telefono}?text=" + Uri.encode(texto)))
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        true
    } catch (_: Exception) { false }

    /** Abre el marcador con el número (no llama solo: no pide permiso de llamadas). */
    fun llamar(ctx: Context, numero: String): Boolean = try {
        val n = if (numero.length > 5) "+$numero" else numero
        ctx.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:$n")).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        true
    } catch (_: Exception) { false }

}
