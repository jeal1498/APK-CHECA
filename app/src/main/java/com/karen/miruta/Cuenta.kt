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

    /** [nombre] = modelo del teléfono; [alias] = nombre que le puso el conductor («Mi tablet»). */
    data class Equipo(val id: String, val nombre: String, val ultimoUso: String, val alias: String = "") {
        fun visible(): String = alias.ifEmpty { nombre.ifEmpty { id } }
    }

    private fun texto(o: JSONObject, k: String): String = o.optString(k).takeUnless { it == "null" } ?: ""

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
    /** Entró con «Entrar sin cuenta»: solo vale mientras dure la beta gratis. */
    fun sinCuenta(ctx: Context) =
        !conectada(ctx) && sp(ctx).getBoolean("sin_cuenta", false) && Licencia.estado(ctx).beta
    /** Puede usar Checa: con sesión de Google o sin cuenta durante la beta. Nadie, al vencer [Migracion]. */
    fun puedeUsar(ctx: Context) = !Migracion.vencida() && (conectada(ctx) || sinCuenta(ctx))
    fun entrarSinCuenta(ctx: Context) = sp(ctx).edit().putBoolean("sin_cuenta", true).apply()
    fun correo(ctx: Context): String = sp(ctx).getString("correo", null) ?: ""
    fun nombre(ctx: Context): String = sp(ctx).getString("nombre", null) ?: ""
    fun equipoQuitado(ctx: Context) = sp(ctx).getBoolean("quitado", false)
    fun tieneLicenciaGuardada(ctx: Context) = sp(ctx).getString("lic_firma", null) != null
    /** Precio de fundador (40 % menos): la cuenta tiene uno de los 50 lugares del Plan Fundador (lo decide mp-pago). */
    fun precioBeta(ctx: Context) = sp(ctx).getBoolean("precio_beta", false)

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
            Equipo(texto(o, "equipo"), texto(o, "nombre"), texto(o, "ultimo_uso"), texto(o, "alias"))
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
                    if (e is GetCredentialCancellationException) {
                        listo(Resultado.Error(""))
                        return
                    }
                    // El detalle técnico va solo al registro del sistema (logcat); la persona ve un mensaje amable.
                    android.util.Log.e("ChecaGoogleLogin", "Credential Manager falló: ${e.javaClass.name}", e)
                    listo(Resultado.Error("No se pudo abrir Google. Revisa tu internet e inténtalo otra vez."))
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
            // Otra persona puede entrar después en este equipo: se borra lo guardado de esta cuenta.
            Guardado.borrarTodo(app)
            Historial.borrar(app)
            Prefs(app).borrarDiagnostico()
            Licencia.limpiarCache()
            principal.post { listo() }
        }.start()
    }

    /**
     * Solicita al backend la eliminación de la cuenta autenticada y sus datos asociados.
     * Los registros de pago se conservan anonimizados en el servidor.
     */
    fun eliminarCuenta(ctx: Context, listo: (Resultado) -> Unit) {
        val app = ctx.applicationContext
        Thread {
            val resultado = try {
                if (!conectada(app)) throw HttpError(401, "sin sesión")
                pedir("POST", "/functions/v1/eliminar-cuenta", token(app), JSONObject().toString())
                val bienvenida = sp(app).getBoolean("bienvenida", false)
                sp(app).edit().clear().putBoolean("bienvenida", bienvenida).commit()
                Guardado.borrarTodo(app)
                Historial.borrar(app)
                Prefs(app).borrarDiagnostico()
                Licencia.limpiarCache()
                Resultado.Ok
            } catch (e: HttpError) {
                Resultado.Error(
                    if (e.codigo == 401) "Tu sesión venció. Vuelve a entrar con Google e inténtalo otra vez."
                    else "No se pudo completar la eliminación (${e.codigo}). Inténtalo de nuevo o contacta soporte."
                )
            } catch (_: Exception) {
                Resultado.Error("No se pudo completar la eliminación. Revisa tu conexión e inténtalo de nuevo.")
            }
            principal.post { listo(resultado) }
        }.start()
    }

    /**
     * Pide a Supabase (función mp-pago) el link de pago de Mercado Pago para [plan] (1m, 6m o 12m).
     * [listo] recibe el link, o null y un mensaje si falló.
     */
    fun crearPago(ctx: Context, plan: String, listo: (String?, String) -> Unit) {
        if (BuildConfig.ES_PLAY) {
            listo(null, "Las compras de Google Play aún no están disponibles en esta versión.")
            return
        }
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
                .remove("precio_beta").remove("ajustes_hash")
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
        val lic = JSONArray(pedir("GET", "/rest/v1/licencias?select=vence,fundador,firma,max_equipos,premios", t, null))
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
                .putInt("premios", o.optInt("premios", 0))
            // Primera revisión de esta cuenta en el teléfono: no avisar premios viejos.
            if (!sp(ctx).contains("premios_vistos")) e.putInt("premios_vistos", o.optInt("premios", 0))
        } else {
            e.remove("lic_vence").remove("lic_f").remove("lic_eq").remove("lic_firma")
            if (!sp(ctx).contains("premios_vistos")) e.putInt("premios_vistos", 0)
        }
        e.commit()
        sincronizarInvitacion(ctx, t)
        sincronizarPrecio(ctx, t)
        try { sincronizarAjustes(ctx, t) } catch (_: Exception) { }
        return Resultado.Ok
    }

    // ------------------------------------------------------------------
    // Ajustes en la cuenta (filtros, gasolina, pop-up, avisos…)
    // ------------------------------------------------------------------

    /** Texto estable de los ajustes, para saber si algo cambió. */
    private fun canon(o: JSONObject): String =
        o.keys().asSequence().sorted().joinToString(",") { k ->
            val v = o.get(k)
            k + "=" + if (v is Number) String.format(Locale.US, "%.4f", v.toDouble()) else v.toString()
        }

    private fun subirAjustes(ctx: Context, t: String, datos: JSONObject) {
        val fila = JSONObject().put("correo", correo(ctx)).put("datos", datos).put("actualizado", isoAhora())
        pedir("POST", "/rest/v1/ajustes", t, JSONArray().put(fila).toString(),
            "resolution=merge-duplicates,return=minimal")
        sp(ctx).edit().putString("ajustes_hash", canon(datos)).commit()
    }

    /**
     * Primera vez en este equipo: se aplican los de la cuenta (o, si no hay, se suben los de aquí).
     * Después: si cambiaron aquí se suben; si no, se aplican los de la cuenta por si cambiaron en otro equipo.
     */
    private fun sincronizarAjustes(ctx: Context, t: String) {
        val prefs = Prefs(ctx)
        val local = prefs.exportar()
        val guardado = sp(ctx).getString("ajustes_hash", null)
        val fila = JSONArray(pedir("GET", "/rest/v1/ajustes?select=datos", t, null))
        if (fila.length() == 0) { subirAjustes(ctx, t, local); return }
        val remoto = fila.getJSONObject(0).getJSONObject("datos")
        val l = canon(local)
        when {
            guardado != null && l != guardado -> subirAjustes(ctx, t, local)
            guardado == null || canon(remoto) != l -> {
                prefs.importar(remoto)
                sp(ctx).edit().putString("ajustes_hash", canon(prefs.exportar())).putBoolean("ajustes_nuevos", true).commit()
            }
        }
    }

    /** Sube los ajustes si cambiaron desde la última vez (se llama al salir de la app). */
    fun subirAjustesSiCambio(ctx: Context) {
        if (!conectada(ctx)) return
        val app = ctx.applicationContext
        Thread {
            try {
                val local = Prefs(app).exportar()
                if (canon(local) != sp(app).getString("ajustes_hash", null)) subirAjustes(app, token(app), local)
            } catch (_: Exception) { }
        }.start()
    }

    /** true una sola vez si se aplicaron ajustes de la cuenta (la pantalla debe repintarse). */
    fun ajustesAplicados(ctx: Context): Boolean {
        val s = sp(ctx)
        if (!s.getBoolean("ajustes_nuevos", false)) return false
        s.edit().remove("ajustes_nuevos").commit()
        return true
    }

    // ------------------------------------------------------------------
    // Invita a un amigo
    // ------------------------------------------------------------------

    private val FORMATO_CODIGO = Regex("^[A-Z0-9]{4}-?[A-Z0-9]{3}$")

    /** Deja el código como ABCD-EF2, o null si no tiene ese formato. */
    fun normalizarCodigo(c: String): String? {
        val l = c.uppercase(Locale.ROOT).filter { it.isLetterOrDigit() }
        if (!FORMATO_CODIGO.matches(l)) return null
        return l.take(4) + "-" + l.drop(4)
    }

    /** Código de esta cuenta para compartir (null si aún no se ha descargado). */
    fun miCodigo(ctx: Context): String? = if (conectada(ctx)) sp(ctx).getString("mi_codigo", null) else null

    /** Código del amigo ya registrado en la cuenta. */
    fun invitacion(ctx: Context): String? = sp(ctx).getString("invitacion", null)

    /** Código guardado que se aplicará al entrar con Google. */
    fun invitacionPendiente(ctx: Context): String? = sp(ctx).getString("inv_pendiente", null)

    /** Meses ganados por invitaciones que la app todavía no ha avisado. */
    fun premiosNuevos(ctx: Context): Int = sp(ctx).getInt("premios", 0) - sp(ctx).getInt("premios_vistos", 0)

    fun marcarPremiosVistos(ctx: Context) =
        sp(ctx).edit().putInt("premios_vistos", sp(ctx).getInt("premios", 0)).apply()

    /**
     * Guarda el código de un amigo. Con sesión lo registra ya; sin sesión queda pendiente.
     * [listo] recibe un mensaje para mostrar y si quedó bien.
     */
    fun usarCodigo(ctx: Context, codigo: String, listo: (String, Boolean) -> Unit) {
        val app = ctx.applicationContext
        val c = normalizarCodigo(codigo)
        if (c == null) { listo("Ese código no es válido. Tiene 7 letras o números, como ABCD-EF2.", false); return }
        if (!conectada(app)) {
            sp(app).edit().putString("inv_pendiente", c).apply()
            listo("Código $c guardado. Se aplica cuando entres con Google.", true)
            return
        }
        Thread {
            val (msj, ok) = try {
                aplicarCodigo(app, token(app), c)
            } catch (_: Exception) {
                sp(app).edit().putString("inv_pendiente", c).apply()
                "Sin conexión. Guardamos el código y lo aplicamos cuando tengas internet." to true
            }
            principal.post { listo(msj, ok) }
        }.start()
    }

    private fun aplicarCodigo(ctx: Context, t: String, c: String): Pair<String, Boolean> {
        val r = JSONObject("{\"r\":" + pedir("POST", "/rest/v1/rpc/usar_codigo", t, JSONObject().put("c", c).toString()) + "}")
            .optString("r")
        val e = sp(ctx).edit().remove("inv_pendiente")
        val res = when (r) {
            "ok" -> { e.putString("invitacion", c); "¡Listo! Cuando pagues tu primer paquete, tú y tu amigo ganan 1 mes gratis." to true }
            "ya_tienes" -> "Tu cuenta ya tiene el código de un amigo." to true
            "propio" -> "Ese es tu propio código. Compártelo con un amigo." to false
            "ya_pagaste" -> "El código de amigo solo vale antes de tu primer pago." to false
            else -> "No encontramos ese código. Revísalo con tu amigo." to false
        }
        e.apply()
        return res
    }

    /** Pregunta a mp-pago si la cuenta tiene precio de fundador (si falla, se queda lo último guardado). */
    private fun sincronizarPrecio(ctx: Context, t: String) {
        try {
            val r = JSONObject(pedir("POST", "/functions/v1/mp-pago", t, "{\"consulta\":true}"))
            sp(ctx).edit().putBoolean("precio_beta", r.optBoolean("beta", false)).apply()
        } catch (_: Exception) { }
    }

    /** Descarga el código propio, aplica uno pendiente y recuerda con qué código entró la cuenta. */
    private fun sincronizarInvitacion(ctx: Context, t: String) {
        try {
            if (sp(ctx).getString("mi_codigo", null) == null) {
                val c = JSONObject("{\"r\":" + pedir("POST", "/rest/v1/rpc/mi_codigo", t, "{}") + "}").optString("r")
                if (c.isNotEmpty() && c != "null") sp(ctx).edit().putString("mi_codigo", c).apply()
            }
            val pendiente = invitacionPendiente(ctx)
            if (pendiente != null && invitacion(ctx) == null) {
                aplicarCodigo(ctx, t, pendiente)
            }
            if (invitacion(ctx) == null) {
                val c = JSONObject("{\"r\":" + pedir("POST", "/rest/v1/rpc/mi_invitacion", t, "{}") + "}").optString("r")
                if (c.isNotEmpty() && c != "null") sp(ctx).edit().putString("invitacion", c).apply()
            }
        } catch (_: Exception) { }
    }

    private fun leerEquipos(ctx: Context, t: String): List<Equipo> {
        // Si el esquema aún no tiene la columna «alias», se leen sin ella.
        val txt = try {
            pedir("GET", "/rest/v1/equipos?select=equipo,nombre,ultimo_uso,alias&order=ultimo_uso.desc", t, null)
        } catch (_: HttpError) {
            pedir("GET", "/rest/v1/equipos?select=equipo,nombre,ultimo_uso&order=ultimo_uso.desc", t, null)
        }
        val a = JSONArray(txt)
        return (0 until a.length()).map { i ->
            val o = a.getJSONObject(i)
            Equipo(texto(o, "equipo"), texto(o, "nombre"), texto(o, "ultimo_uso"), texto(o, "alias"))
        }
    }

    private fun equiposJson(l: List<Equipo>): String {
        val a = JSONArray()
        l.forEach {
            a.put(JSONObject().put("equipo", it.id).put("nombre", it.nombre).put("ultimo_uso", it.ultimoUso).put("alias", it.alias))
        }
        return a.toString()
    }

    /** Le pone un nombre propio a un equipo de la cuenta («Mi tablet»). Vacío = vuelve al modelo. */
    fun renombrarEquipo(ctx: Context, id: String, alias: String, listo: (Resultado) -> Unit) =
        enSegundoPlano(ctx, listo) {
            // HttpURLConnection no admite PATCH: se usa el mismo «upsert» que registra el equipo.
            val valor: Any = alias.trim().take(30).ifEmpty { JSONObject.NULL }
            val fila = JSONObject().put("correo", correo(ctx)).put("equipo", id).put("alias", valor)
            pedir("POST", "/rest/v1/equipos", token(ctx), JSONArray().put(fila).toString(),
                "resolution=merge-duplicates,return=minimal")
            sincronizar(ctx)
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
