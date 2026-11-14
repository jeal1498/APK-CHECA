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
            Horas.borrar(app)
            Jornada.borrarPrivados(app)
            Sos.borrarTodo(app)
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
                Horas.borrar(app)
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
                .remove("precio_beta").remove("ajustes_hash").remove("perfil_hash")
                .remove("vc_subidos").remove("vc_borrar").remove("capt_ok")
            Jornada.borrarPrivados(ctx)
            // Los contactos que se pusieron sin cuenta se quedan y se suben; los de otra cuenta, no.
            if (s.getString("correo", null) != null) { e.remove("sos_pend"); Sos.borrarTodo(ctx) }
        }
        e.putString("correo", correo)
            .putString("acceso", j.getString("access_token"))
            .putString("refresco", j.getString("refresh_token"))
            .putLong("expira", System.currentTimeMillis() + j.optLong("expires_in", 3600) * 1000)
        if (!nombre.isNullOrBlank()) e.putString("nombre", nombre)
        e.commit()
    }

    /** Revisión de una oferta en segundo plano ([Confirmador]). null si no hay sesión o falla. */
    fun revisarOferta(ctx: Context, cuerpo: String): String? = try {
        if (sp(ctx).getString("refresco", null) == null) null
        else pedir("POST", "/functions/v1/confirmar-oferta", token(ctx), cuerpo)
    } catch (_: Exception) { null }

    /** Sube la captura de un caso dudoso (10.72) al almacén privado «capturas»; se borra a los 7 días. */
    fun subirCaptura(ctx: Context, caso: Long, bytes: ByteArray) {
        // Si lo apagó después de tomar la captura, ya no se manda.
        if (!Prefs(ctx).diagnostico) return
        subirImagen(ctx, "$caso.webp", bytes)
    }

    /** Tope de capturas de ofertas normales por conductor, en total (Eduardo, 14 nov). */
    const val MAX_CAPTURAS_OK = 100

    /**
     * 10.78: sube la captura de una oferta normal (el pop-up salió verde, amarillo o rojo y la revisión
     * coincidió), como `ok-<color>-<tarifa>-<km>-<min>-<hora>.webp`. Solo si el conductor vio el aviso y
     * sin pasar de [MAX_CAPTURAS_OK] por cuenta (el servidor también lo limita). Se borra a los 7 días.
     */
    @Synchronized
    fun subirCapturaOk(ctx: Context, nivel: String, o: Oferta, bytes: ByteArray) {
        try {
            val p = Prefs(ctx)
            if (!p.diagnostico || !p.diagEnvioVisto) return
            val s = sp(ctx)
            val subidas = s.getInt("capt_ok", 0)
            if (subidas >= MAX_CAPTURAS_OK) return
            val color = if (nivel == "verde" || nivel == "amarillo" || nivel == "rojo") nivel else "sin"
            val nombre = "ok-$color-" + String.format(Locale.US, "%.2f-%.1f-%.0f", o.tarifa, o.km, o.min) +
                "-" + System.currentTimeMillis() + ".webp"
            if (subirImagen(ctx, nombre, bytes)) s.edit().putInt("capt_ok", subidas + 1).apply()
        } catch (_: Exception) { }
    }

    /** POST de una imagen WebP al almacén «capturas»; true si el servidor la aceptó. */
    private fun subirImagen(ctx: Context, nombre: String, bytes: ByteArray): Boolean {
        try {
            if (sp(ctx).getString("refresco", null) == null) return false
            val con = URL(URL_BASE + "/storage/v1/object/capturas/$nombre").openConnection() as HttpURLConnection
            try {
                con.requestMethod = "POST"
                con.connectTimeout = 15_000
                con.readTimeout = 20_000
                con.setRequestProperty("apikey", ANON)
                con.setRequestProperty("Authorization", "Bearer " + token(ctx))
                con.setRequestProperty("Content-Type", "image/webp")
                con.doOutput = true
                con.setFixedLengthStreamingMode(bytes.size)
                con.outputStream.use { it.write(bytes) }
                return con.responseCode in 200..299
            } finally { con.disconnect() }
        } catch (_: Exception) { return false }
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
        try { sincronizarPerfil(ctx, t) } catch (_: Exception) { }
        try { sincronizarViajes(ctx, t) } catch (_: Exception) { }
        try { bajarViajes(ctx, t) } catch (_: Exception) { }
        try { sincronizarHoras(ctx, t) } catch (_: Exception) { }
        if (Jornada.CLIENTELA_ACTIVA) try { bajarPrivados(ctx, t) } catch (_: Exception) { }
        try { sincronizarSos(ctx, t) } catch (_: Exception) { }
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
            try {
                if (canonPerfil(Prefs(app)) != sp(app).getString("perfil_hash", null)) sincronizarPerfil(app, token(app))
            } catch (_: Exception) { }
            try { sincronizarHoras(app, token(app)) } catch (_: Exception) { }
        }.start()
    }

    // ------------------------------------------------------------------
    // Perfil del conductor (compartido con Clientela): moneda y gasolina por km
    // ------------------------------------------------------------------

    /** Costo de gasolina por km (precio por litro ÷ km por litro), o null si falta un dato. */
    private fun gasolinaKm(p: Prefs): Double? =
        if (p.kmPorLitro > 0 && p.precioLitro > 0)
            Math.round(p.precioLitro.toDouble() / p.kmPorLitro * 10_000) / 10_000.0
        else null

    private fun canonPerfil(p: Prefs): String = p.moneda.toString() + "|" + gasolinaKm(p)

    /**
     * La moneda la manda la cuenta, salvo que se haya cambiado aquí y falte subirla.
     * La gasolina por km la escribe Checa (Clientela la lee para su cotizador).
     */
    private fun sincronizarPerfil(ctx: Context, t: String) {
        val p = Prefs(ctx)
        val filas = JSONArray(pedir("GET",
            "/rest/v1/perfil_conductor?select=moneda,simbolo,simbolo_despues,sep_miles,sep_decimal,decimales,gasolina_km",
            t, null))
        val remoto = if (filas.length() > 0) filas.getJSONObject(0) else null
        val sube = JSONObject()
        // El país elegido manda: si su moneda no es la guardada (la cuenta la había dejado en otra), se corrige y se sube.
        if (!p.monedaPendiente) {
            Pais.de(p.pais)?.let { pa ->
                if (pa.moneda.codigo != p.moneda.codigo || (remoto != null && pa.moneda.codigo != remoto.optString("moneda", "MXN"))) {
                    p.moneda = pa.moneda
                    p.monedaPendiente = true
                }
            }
        }
        val pendiente = p.monedaPendiente
        if (pendiente) {
            val m = p.moneda
            sube.put("moneda", m.codigo).put("simbolo", m.simbolo).put("simbolo_despues", m.despues)
                .put("sep_miles", m.miles).put("sep_decimal", m.decimal).put("decimales", m.decimales)
        } else if (remoto != null) {
            val m = Moneda(
                remoto.optString("moneda", "MXN"), remoto.optString("simbolo", "$"),
                remoto.optBoolean("simbolo_despues", false), remoto.optString("sep_miles", ","),
                remoto.optString("sep_decimal", "."), remoto.optInt("decimales", 0)
            ).normalizada()
            if (m != p.moneda) {
                p.moneda = m
                sp(ctx).edit().putBoolean("ajustes_nuevos", true).commit()
            }
        }
        val gk = gasolinaKm(p)
        val gkRemoto = if (remoto == null || remoto.isNull("gasolina_km")) null else remoto.optDouble("gasolina_km")
        if (Jornada.CLIENTELA_ACTIVA && gk != null && (gkRemoto == null || Math.abs(gk - gkRemoto) > 0.00005)) sube.put("gasolina_km", gk)
        if (sube.length() > 0) {
            pedir("POST", "/rest/v1/perfil_conductor?on_conflict=user_id", t, JSONArray().put(sube).toString(),
                "resolution=merge-duplicates,return=minimal")
            if (pendiente) p.monedaPendiente = false
        }
        sp(ctx).edit().putString("perfil_hash", canonPerfil(p)).commit()
    }

    /** Elige la moneda en este equipo y la sube a la cuenta (la ve también Clientela). */
    fun cambiarMoneda(ctx: Context, m: Moneda) {
        val p = Prefs(ctx)
        p.moneda = m
        p.monedaPendiente = true
        if (!conectada(ctx)) return
        val app = ctx.applicationContext
        Thread { try { sincronizarPerfil(app, token(app)) } catch (_: Exception) { } }.start()
    }

    // ------------------------------------------------------------------
    // «Mi día» unificado: viajes_checa (sube) y viajes_privados de Clientela (baja)
    // ------------------------------------------------------------------

    /** Id fijo por viaje (la hora de la oferta), para que subirlo dos veces no lo duplique. */
    private fun idViaje(hora: Long): String =
        java.util.UUID.nameUUIDFromBytes("checa-$hora".toByteArray()).toString()

    private fun isoDe(ms: Long): String = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US)
        .apply { timeZone = TimeZone.getTimeZone("UTC") }.format(Date(ms))

    private fun conjunto(ctx: Context, k: String): MutableSet<String> =
        HashSet(sp(ctx).getStringSet(k, emptySet()) ?: emptySet())

    /** Sube los viajes aceptados que falten y borra los que se quitaron. */
    @Synchronized
    private fun sincronizarViajes(ctx: Context, t: String) {
        val subidos = conjunto(ctx, "vc_subidos")
        val borrar = conjunto(ctx, "vc_borrar")
        for (h in borrar.toList()) {
            val ms = h.toLongOrNull()
            if (ms != null) pedir("DELETE", "/rest/v1/viajes_checa?id=eq.${idViaje(ms)}", t, null)
            borrar.remove(h)
            subidos.remove(h)
        }
        val locales = Jornada.todos(ctx)
        val nuevos = locales.filter { it.hora.toString() !in subidos }
        if (nuevos.isNotEmpty()) {
            val filas = JSONArray()
            for (v in nuevos) {
                val app = v.app.lowercase(Locale.ROOT).let { if (it == "uber" || it == "didi") it else "otra" }
                filas.put(JSONObject().put("id", idViaje(v.hora)).put("app", app).put("cuando", isoDe(v.hora))
                    .put("tarifa", v.tarifa).put("km", v.km).put("gasolina", v.gasolina).put("neto", v.neta))
            }
            pedir("POST", "/rest/v1/viajes_checa?on_conflict=user_id,id", t, filas.toString(),
                "resolution=merge-duplicates,return=minimal")
            nuevos.forEach { subidos.add(it.hora.toString()) }
        }
        subidos.retainAll(locales.map { it.hora.toString() }.toSet())
        sp(ctx).edit().putStringSet("vc_subidos", subidos).putStringSet("vc_borrar", borrar).commit()
    }

    /**
     * Viajes aceptados de otros equipos (o de antes de cambiar de teléfono): baja los de la última semana
     * que no estén aquí, para reconstruir «Mi día» y la meta. Se marcan como subidos para no repetirlos.
     */
    @Synchronized
    private fun bajarViajes(ctx: Context, t: String) {
        val desde = isoDe(Jornada.inicioDeSemana())
        val r = JSONArray(pedir("GET",
            "/rest/v1/viajes_checa?select=id,app,cuando,tarifa,km,gasolina&cuando=gte.$desde&order=cuando.desc&limit=500", t, null))
        if (r.length() == 0) return
        val locales = Jornada.todos(ctx)
        val ids = locales.map { idViaje(it.hora) }.toSet()
        val borrar = conjunto(ctx, "vc_borrar")
        val subidos = conjunto(ctx, "vc_subidos")
        val fmt = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US).apply { timeZone = TimeZone.getTimeZone("UTC") }
        val nuevos = ArrayList<Viaje>()
        for (i in 0 until r.length()) {
            val o = r.getJSONObject(i)
            val id = o.optString("id")
            if (id in ids || id.isEmpty()) continue
            val base = try { fmt.parse(o.optString("cuando").take(19))?.time } catch (_: Exception) { null } ?: continue
            // La hora original tiene milisegundos y el id sale de ella: se recupera para que borrar siga funcionando.
            val hora = (base until base + 1000).firstOrNull { idViaje(it) == id } ?: base
            if (hora.toString() in borrar) continue
            val app = when (o.optString("app")) { "uber" -> "Uber"; "didi" -> "DiDi"; else -> "Otra" }
            val tarifa = o.optDouble("tarifa", 0.0)
            if (tarifa <= 0) continue
            nuevos.add(Viaje(hora, app, tarifa, o.optDouble("km", 0.0), o.optDouble("gasolina", 0.0)))
        }
        if (nuevos.isEmpty()) return
        Jornada.agregar(ctx, nuevos)
        nuevos.forEach { subidos.add(it.hora.toString()) }
        sp(ctx).edit().putStringSet("vc_subidos", subidos).putBoolean("ajustes_nuevos", true).commit()
    }

    // ------------------------------------------------------------------
    // «Mejores horas» en la cuenta: solo el resumen por día y hora (horas_checa)
    // ------------------------------------------------------------------

    /**
     * Baja el resumen de la cuenta (para equipos nuevos) y sube las celdas donde este equipo
     * sabe más (más ofertas) que la cuenta. Solo conteo, tarifa y km por día y hora.
     */
    @Synchronized
    private fun sincronizarHoras(ctx: Context, t: String) {
        val desde = isoDe(System.currentTimeMillis() - 30 * 86_400_000L)
        val r = JSONArray(pedir("GET",
            "/rest/v1/horas_checa?select=dia,hora,n,tarifa,km&actualizado=gte.$desde", t, null))
        var nube = (0 until r.length()).map {
            val o = r.getJSONObject(it)
            CeldaHora(o.getInt("dia"), o.getInt("hora"), o.getInt("n"), o.getDouble("tarifa"), o.getDouble("km"))
        }
        val locales = Horas.locales(ctx)
        val canonLocal = Horas.canon(locales)
        if (canonLocal != sp(ctx).getString("horas_hash", null)) {
            val enNube = nube.associateBy { it.dia * 24 + it.hora }
            val suben = locales.filter { it.n > (enNube[it.dia * 24 + it.hora]?.n ?: 0) && it.n >= 3 }
            if (suben.isNotEmpty()) {
                val filas = JSONArray()
                for (c in suben) filas.put(JSONObject().put("dia", c.dia).put("hora", c.hora).put("n", c.n)
                    .put("tarifa", Math.round(c.tarifa * 100) / 100.0).put("km", Math.round(c.km * 100) / 100.0))
                pedir("POST", "/rest/v1/horas_checa?on_conflict=user_id,dia,hora", t, filas.toString(),
                    "resolution=merge-duplicates,return=minimal")
                val subidas = suben.associateBy { it.dia * 24 + it.hora }
                nube = nube.filter { (it.dia * 24 + it.hora) !in subidas } + suben
            }
            sp(ctx).edit().putString("horas_hash", canonLocal).commit()
        }
        Horas.guardarNube(ctx, nube)
    }

    /** «Borrar estadísticas»: también se borra el resumen de la cuenta, o regresaría solo. */
    fun borrarHoras(ctx: Context) {
        Horas.borrar(ctx)
        sp(ctx).edit().remove("horas_hash").commit()
        if (!conectada(ctx)) return
        val app = ctx.applicationContext
        Thread { try { pedir("DELETE", "/rest/v1/horas_checa?dia=gte.0", token(app), null) } catch (_: Exception) { } }.start()
    }

    /** Baja los viajes privados hechos de esta semana (Clientela) para sumarlos a «Mi día». */
    private fun bajarPrivados(ctx: Context, t: String) {
        val desde = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date(Jornada.inicioDeSemana()))
        val r = pedir("GET", "/rest/v1/viajes_privados?select=fecha,hora,precio,km:datos-%3E%3Ekm" +
            "&borrado=is.false&estado=eq.hecho&fecha=gte.$desde", t, null)
        Jornada.guardarPrivados(ctx, JSONArray(r).toString())
    }

    /**
     * Contactos SOS: si cambiaron aquí, se suben (y se borran en la cuenta los que se quitaron);
     * si no, se toman los de la cuenta.
     */
    @Synchronized
    private fun sincronizarSos(ctx: Context, t: String) {
        val remotos = Sos.desdeJson(JSONArray(pedir("GET", "/rest/v1/contactos_sos?select=id,nombre,telefono,relacion", t, null)))
        if (!sp(ctx).getBoolean("sos_pend", false)) {
            Sos.guardar(ctx, remotos, local = false)
            return
        }
        val locales = Sos.contactos(ctx)
        val ids = locales.map { it.id }.toSet()
        for (r in remotos) if (r.id !in ids) pedir("DELETE", "/rest/v1/contactos_sos?id=eq.${r.id}", t, null)
        if (locales.isNotEmpty()) {
            pedir("POST", "/rest/v1/contactos_sos?on_conflict=user_id,id", t, Sos.aJson(locales).toString(),
                "resolution=merge-duplicates,return=minimal")
        }
        sp(ctx).edit().putBoolean("sos_pend", false).commit()
    }

    /** Cambiaron los contactos SOS en este teléfono: se suben si hay cuenta. */
    fun contactosCambiaron(ctx: Context) {
        sp(ctx).edit().putBoolean("sos_pend", true).commit()
        if (!conectada(ctx)) return
        val app = ctx.applicationContext
        Thread { try { sincronizarSos(app, token(app)) } catch (_: Exception) { } }.start()
    }

    /** Tras registrar un viaje: lo sube si hay cuenta (sin esperar). */
    fun subirViajesLuego(ctx: Context) {
        if (!conectada(ctx)) return
        val app = ctx.applicationContext
        Thread { try { sincronizarViajes(app, token(app)) } catch (_: Exception) { } }.start()
    }

    /** Tras quitar un viaje: si ya estaba en la cuenta, se borra allá también. */
    fun viajeQuitado(ctx: Context, hora: Long) {
        val h = hora.toString()
        if (h !in conjunto(ctx, "vc_subidos")) return
        val borrar = conjunto(ctx, "vc_borrar").apply { add(h) }
        sp(ctx).edit().putStringSet("vc_borrar", borrar).commit()
        subirViajesLuego(ctx)
    }

    /** Vuelve a bajar los viajes de Clientela (al abrir Inicio), sin esperar. */
    fun actualizarPrivados(ctx: Context, listo: () -> Unit) {
        if (!Jornada.CLIENTELA_ACTIVA || !conectada(ctx)) return
        val ahora = System.currentTimeMillis()
        if (ahora - sp(ctx).getLong("priv_ms", 0L) < 120_000) return
        sp(ctx).edit().putLong("priv_ms", ahora).apply()
        val app = ctx.applicationContext
        Thread {
            try {
                bajarPrivados(app, token(app))
                principal.post { listo() }
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
    fun premios(ctx: Context): Int = sp(ctx).getInt("premios", 0)

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
