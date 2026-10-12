package com.karen.miruta

import android.app.Activity
import android.content.Context
import android.os.Build
import android.os.CancellationSignal
import android.os.Handler
import android.os.Looper
import androidx.credentials.CredentialManager
import androidx.credentials.CredentialManagerCallback
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.GetCredentialResponse
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest
import java.security.SecureRandom
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import java.util.concurrent.Executor

/**
 * Cuenta Google + licencia guardada en Supabase (tablas `licencias` y `equipos`).
 * La licencia viene firmada (ECDSA de "LV|<correo>|<fecha>[|F][|B]") y se guarda en el teléfono:
 * funciona sin internet, pero se revisa una vez al día y deja de valer tras 15 días sin revisión.
 */
object Cuenta {
    private const val URL_BASE = "https://gqsadclvftkzwrihfipz.supabase.co"
    private const val ANON =
        "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6Imdxc2FkY2x2ZnRrendyaWhmaXB6Iiwicm9sZSI6ImFub24iLCJpYXQiOjE3OTE3NDY5NTEsImV4cCI6MjEwNzMyMjk1MX0.yLcJBXuE8kz_PXiUpCL_7ArDoINhce6NbbsBo2NMQTc"
    private const val WEB_ID = "280542529955-bk79hqpgjs2ddgsfllaol0cdeu60i4nt.apps.googleusercontent.com"
    private const val DIA = 86_400_000L
    const val DIAS_SIN_RED = 15
    const val MAX_EQUIPOS = 2

    /** Equipos que permite la licencia de la cuenta (1 en el plan de 1 mes). */
    fun maxEquipos(ctx: Context): Int = sp(ctx).getInt("lic_eq", MAX_EQUIPOS)

    data class Equipo(val id: String, val nombre: String, val ultimoUso: String)

    sealed class Resultado {
        object Ok : Resultado()
        class Limite(val equipos: List<Equipo>) : Resultado()
        class Error(val mensaje: String) : Resultado()
    }

    private class HttpError(val codigo: Int, val cuerpo: String) : Exception("Error $codigo")

    private val principal = Handler(Looper.getMainLooper())
    @Volatile private var revisando = false

    private fun sp(ctx: Context) = ctx.applicationContext.getSharedPreferences("cuenta", Context.MODE_PRIVATE)

    fun conectada(ctx: Context) = sp(ctx).getString("correo", null) != null
    fun correo(ctx: Context): String = sp(ctx).getString("correo", null) ?: ""
    fun nombre(ctx: Context): String = sp(ctx).getString("nombre", null) ?: ""
    fun equipoQuitado(ctx: Context) = sp(ctx).getBoolean("quitado", false)
    fun tieneLicenciaGuardada(ctx: Context) = sp(ctx).getString("lic_firma", null) != null

    fun bienvenidaVista(ctx: Context) = sp(ctx).getBoolean("bienvenida", false)

    fun marcarBienvenida(ctx: Context) = sp(ctx).edit().putBoolean("bienvenida", true).apply()

    /** Días desde la última revisión en línea. */
    fun diasSinRevisar(ctx: Context): Int {
        val rev = sp(ctx).getLong("revision", 0L)
        if (rev == 0L) return Int.MAX_VALUE
        return ((Licencia.ahora(ctx) - rev) / DIA).toInt()
    }

    /** Hay licencia de cuenta pero pasaron más de 15 días sin revisarla. */
    fun necesitaRevision(ctx: Context) =
        conectada(ctx) && tieneLicenciaGuardada(ctx) && diasSinRevisar(ctx) > DIAS_SIN_RED

    /** Licencia de la cuenta guardada en el teléfono, o null si no sirve en este equipo. */
    fun licencia(ctx: Context): Licencia.Codigo? {
        val s = sp(ctx)
        val correo = s.getString("correo", null) ?: return null
        if (s.getBoolean("quitado", false)) return null
        val firma = s.getString("lic_firma", null) ?: return null
        if (diasSinRevisar(ctx) > DIAS_SIN_RED) return null
        val vence = s.getInt("lic_vence", 0)
        val f = s.getBoolean("lic_f", false)
        val b = s.getInt("lic_eq", MAX_EQUIPOS) == 1
        val texto = "LV|$correo|$vence" + (if (f) "|F" else "") + (if (b) "|B" else "")
        return if (Licencia.verificar(texto, firma)) Licencia.Codigo(vence, f, b) else null
    }

    fun equipos(ctx: Context): List<Equipo> = try {
        val a = JSONArray(sp(ctx).getString("equipos", "[]"))
        (0 until a.length()).map { i ->
            val o = a.getJSONObject(i)
            Equipo(o.optString("equipo"), o.optString("nombre"), o.optString("ultimo_uso"))
        }
    } catch (_: Exception) { emptyList() }

    fun nombreEsteEquipo(): String {
        val marca = Build.MANUFACTURER.replaceFirstChar { it.uppercase() }
        return if (Build.MODEL.startsWith(Build.MANUFACTURER, ignoreCase = true)) Build.MODEL else "$marca ${Build.MODEL}"
    }

    // ------------------------------------------------------------------
    // Iniciar sesión con Google
    // ------------------------------------------------------------------

    /**
     * Muestra la ventana de Google, entra a Supabase y busca la licencia.
     * [buscando] se llama cuando ya se eligió la cuenta (para mostrar "Buscando tu licencia…").
     */
    fun iniciarSesion(act: Activity, buscando: () -> Unit, listo: (Resultado) -> Unit) {
        val crudo = ByteArray(16).also { SecureRandom().nextBytes(it) }.joinToString("") { "%02x".format(it) }
        val hash = MessageDigest.getInstance("SHA-256").digest(crudo.toByteArray())
            .joinToString("") { "%02x".format(it) }
        val opcion = GetGoogleIdOption.Builder()
            .setFilterByAuthorizedAccounts(false)
            .setServerClientId(WEB_ID)
            .setAutoSelectEnabled(false)
            .setNonce(hash)
            .build()
        val pedido = GetCredentialRequest.Builder().addCredentialOption(opcion).build()
        val ejecutor = Executor { principal.post(it) }
        CredentialManager.create(act).getCredentialAsync(act, pedido, CancellationSignal(), ejecutor,
            object : CredentialManagerCallback<GetCredentialResponse, GetCredentialException> {
                override fun onResult(result: GetCredentialResponse) {
                    val c = result.credential
                    if (c !is CustomCredential || c.type != GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                        listo(Resultado.Error("No se pudo leer tu cuenta de Google."))
                        return
                    }
                    val g = try {
                        GoogleIdTokenCredential.createFrom(c.data)
                    } catch (_: Exception) {
                        listo(Resultado.Error("No se pudo leer tu cuenta de Google."))
                        return
                    }
                    val nombre = g.givenName ?: g.displayName ?: ""
                    buscando()
                    enSegundoPlano(act, listo) {
                        val cuerpo = JSONObject()
                            .put("provider", "google")
                            .put("id_token", g.idToken)
                            .put("nonce", crudo)
                        val r = pedir("POST", "/auth/v1/token?grant_type=id_token", null, cuerpo.toString())
                        guardarSesion(act, JSONObject(r), nombre)
                        sincronizar(act)
                    }
                }

                override fun onError(e: GetCredentialException) {
                    listo(Resultado.Error(
                        if (e is GetCredentialCancellationException) ""
                        else "No se pudo abrir Google. Revisa tu internet e inténtalo otra vez."
                    ))
                }
            })
    }

    /** Revisión diaria (si hay sesión y ya pasó un día). Sin internet no hace nada. */
    fun revisarSiToca(ctx: Context, listo: (Resultado) -> Unit) {
        if (!conectada(ctx) || revisando) return
        val rev = sp(ctx).getLong("revision", 0L)
        if (Licencia.ahora(ctx) - rev < DIA && !equipoQuitado(ctx)) return
        revisando = true
        enSegundoPlano(ctx, { r -> revisando = false; listo(r) }) { sincronizar(ctx) }
    }

    /** Vuelve a registrar este equipo y descarga la licencia (por ejemplo tras quitar otro equipo). */
    fun revisarAhora(ctx: Context, listo: (Resultado) -> Unit) =
        enSegundoPlano(ctx, listo) { sincronizar(ctx) }

    fun quitarEquipo(ctx: Context, id: String, listo: (Resultado) -> Unit) =
        enSegundoPlano(ctx, listo) {
            pedir("DELETE", "/rest/v1/equipos?equipo=eq.$id", token(ctx), null)
            sincronizar(ctx)
        }

    /** Cierra la sesión y libera este equipo en la cuenta (si hay internet). */
    fun cerrarSesion(ctx: Context, listo: () -> Unit) {
        val app = ctx.applicationContext
        val id = Licencia.codigoEquipo(app)
        Thread {
            try { pedir("DELETE", "/rest/v1/equipos?equipo=eq.$id", token(app), null) } catch (_: Exception) { }
            val bienvenida = sp(app).getBoolean("bienvenida", false)
            sp(app).edit().clear().putBoolean("bienvenida", bienvenida).commit()
            Licencia.limpiarCache()
            principal.post { listo() }
        }.start()
    }

    /**
     * Pide a Supabase (función mp-pago) el link de pago de Mercado Pago para [plan] (1m, 6m o 12m).
     * [listo] recibe el link, o null y un mensaje si falló.
     */
    fun crearPago(ctx: Context, plan: String, listo: (String?, String) -> Unit) {
        val app = ctx.applicationContext
        Thread {
            var url: String? = null
            var error = ""
            try {
                val r = pedir("POST", "/functions/v1/mp-pago", token(app), JSONObject().put("plan", plan).toString())
                url = JSONObject(r).optString("url").ifEmpty { null }
                if (url == null) error = "No se pudo abrir Mercado Pago. Inténtalo más tarde."
            } catch (e: HttpError) {
                error = if (e.codigo == 401) "Vuelve a entrar con tu cuenta de Google."
                    else "No se pudo abrir Mercado Pago (${e.codigo}). Inténtalo más tarde."
            } catch (_: Exception) {
                error = "Sin conexión. Inténtalo cuando tengas internet."
            }
            principal.post { listo(url, error) }
        }.start()
    }

    // ------------------------------------------------------------------
    // Supabase
    // ------------------------------------------------------------------

    private fun enSegundoPlano(ctx: Context, listo: (Resultado) -> Unit, trabajo: () -> Resultado) {
        Thread {
            val r = try {
                trabajo()
            } catch (e: HttpError) {
                Resultado.Error("No se pudo revisar tu cuenta (${e.codigo}). Inténtalo más tarde.")
            } catch (_: Exception) {
                Resultado.Error("Sin conexión. Inténtalo cuando tengas internet.")
            }
            Licencia.limpiarCache()
            principal.post { listo(r) }
        }.start()
    }

    private fun guardarSesion(ctx: Context, j: JSONObject, nombre: String?) {
        val correo = j.getJSONObject("user").getString("email").lowercase(Locale.ROOT)
        val s = sp(ctx)
        val e = s.edit()
        if (s.getString("correo", null) != correo) {
            e.remove("lic_firma").remove("lic_vence").remove("lic_f").remove("lic_eq").remove("equipos").remove("quitado")
        }
        e.putString("correo", correo)
            .putString("acceso", j.getString("access_token"))
            .putString("refresco", j.getString("refresh_token"))
            .putLong("expira", System.currentTimeMillis() + j.optLong("expires_in", 3600) * 1000)
        if (!nombre.isNullOrBlank()) e.putString("nombre", nombre)
        e.commit()
    }

    /** Token vigente; si ya venció, lo renueva. */
    private fun token(ctx: Context): String {
        val s = sp(ctx)
        val acceso = s.getString("acceso", null)
        if (acceso != null && s.getLong("expira", 0L) - System.currentTimeMillis() > 60_000) return acceso
        val refresco = s.getString("refresco", null) ?: throw HttpError(401, "sin sesión")
        val r = pedir("POST", "/auth/v1/token?grant_type=refresh_token", null,
            JSONObject().put("refresh_token", refresco).toString())
        guardarSesion(ctx, JSONObject(r), null)
        return sp(ctx).getString("acceso", null) ?: throw HttpError(401, "sin sesión")
    }

    /** Registra este equipo, descarga la licencia y la lista de equipos. */
    private fun sincronizar(ctx: Context): Resultado {
        val t = token(ctx)
        val correo = correo(ctx)
        val fila = JSONObject()
            .put("correo", correo)
            .put("equipo", Licencia.codigoEquipo(ctx))
            .put("nombre", nombreEsteEquipo())
            .put("ultimo_uso", isoAhora())
        try {
            pedir("POST", "/rest/v1/equipos", t, JSONArray().put(fila).toString(),
                "resolution=merge-duplicates,return=minimal")
        } catch (e: HttpError) {
            if (!e.cuerpo.contains("limite_equipos")) throw e
            val lista = leerEquipos(ctx, t)
            sp(ctx).edit().putBoolean("quitado", true).putString("equipos", equiposJson(lista)).commit()
            return Resultado.Limite(lista)
        }
        val lic = JSONArray(pedir("GET", "/rest/v1/licencias?select=vence,fundador,firma,max_equipos", t, null))
        val lista = leerEquipos(ctx, t)
        val e = sp(ctx).edit()
            .putBoolean("quitado", false)
            .putString("equipos", equiposJson(lista))
            .putLong("revision", System.currentTimeMillis())
        if (lic.length() > 0) {
            val o = lic.getJSONObject(0)
            e.putInt("lic_vence", o.getInt("vence"))
                .putBoolean("lic_f", o.optBoolean("fundador", false))
                .putInt("lic_eq", o.optInt("max_equipos", MAX_EQUIPOS))
                .putString("lic_firma", o.getString("firma"))
        } else {
            e.remove("lic_vence").remove("lic_f").remove("lic_eq").remove("lic_firma")
        }
        e.commit()
        return Resultado.Ok
    }

    private fun leerEquipos(ctx: Context, t: String): List<Equipo> {
        val a = JSONArray(pedir("GET", "/rest/v1/equipos?select=equipo,nombre,ultimo_uso&order=ultimo_uso.desc", t, null))
        return (0 until a.length()).map { i ->
            val o = a.getJSONObject(i)
            Equipo(o.optString("equipo"), o.optString("nombre"), o.optString("ultimo_uso"))
        }
    }

    private fun equiposJson(l: List<Equipo>): String {
        val a = JSONArray()
        l.forEach { a.put(JSONObject().put("equipo", it.id).put("nombre", it.nombre).put("ultimo_uso", it.ultimoUso)) }
        return a.toString()
    }

    private fun isoAhora(): String = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US)
        .apply { timeZone = TimeZone.getTimeZone("UTC") }.format(Date())

    private fun pedir(metodo: String, ruta: String, token: String?, cuerpo: String?, prefer: String? = null): String {
        val con = URL(URL_BASE + ruta).openConnection() as HttpURLConnection
        try {
            con.requestMethod = metodo
            con.connectTimeout = 15_000
            con.readTimeout = 15_000
            con.setRequestProperty("apikey", ANON)
            con.setRequestProperty("Authorization", "Bearer " + (token ?: ANON))
            con.setRequestProperty("Content-Type", "application/json")
            if (prefer != null) con.setRequestProperty("Prefer", prefer)
            if (cuerpo != null) {
                con.doOutput = true
                con.outputStream.use { it.write(cuerpo.toByteArray()) }
            }
            val codigo = con.responseCode
            val flujo = if (codigo in 200..299) con.inputStream else con.errorStream
            val txt = flujo?.bufferedReader()?.use { it.readText() } ?: ""
            if (codigo !in 200..299) throw HttpError(codigo, txt)
            return txt
        } finally {
            con.disconnect()
        }
    }

    /** Texto corto para "Último uso" a partir de la fecha ISO de Supabase. */
    fun haceCuanto(iso: String): String {
        val f = try {
            SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US)
                .apply { timeZone = TimeZone.getTimeZone("UTC") }.parse(iso.take(19))
        } catch (_: Exception) { null } ?: return ""
        val dias = ((System.currentTimeMillis() - f.time) / DIA).toInt()
        return when {
            dias <= 0 -> "hoy"
            dias == 1 -> "ayer"
            else -> "hace $dias días"
        }
    }
}
