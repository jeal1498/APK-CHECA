package com.karen.miruta

import android.Manifest
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings

/** Ayuda: preguntas frecuentes, diagnóstico de problemas y mensaje de soporte por WhatsApp. */
object Soporte {
    const val WHATSAPP = "529985214490"

    /** Acción que puede ofrecer una respuesta o una revisión. */
    enum class Accion(val texto: String) {
        NINGUNA(""),
        ACCESIBILIDAD("Abrir Accesibilidad"),
        INFO_APP("Abrir información de la app"),
        BATERIA("Abrir ajustes de batería"),
        NOTIFICACIONES("Abrir notificaciones"),
        PAQUETES("Ver mi plan"),
        PAUSAR("Pausar el lector"),
        SOPORTE("Escribir a soporte")
    }

    class Pregunta(val pregunta: String, val respuesta: String, val accion: Accion = Accion.NINGUNA)

    class Revision(val ok: Boolean, val titulo: String, val detalle: String, val accion: Accion = Accion.NINGUNA)

    val preguntas = listOf(
        Pregunta(
            "No me sale el pop-up",
            "Revisa tres cosas: 1) que «Checa – lector de ofertas» esté activo en Accesibilidad, " +
                "2) que hayas tocado el botón verde ▶ de abajo y aceptado «Pantalla completa», y " +
                "3) que tengas Uber o DiDi abierto con una oferta en pantalla. " +
                "Si sigue sin salir, usa «Diagnóstico de problemas».",
            Accion.ACCESIBILIDAD
        ),
        Pregunta(
            "No encuentro «Checa – lector de ofertas» en Accesibilidad",
            "En Samsung no está en la lista principal: entra a «Aplicaciones instaladas» y ahí está. " +
                "En OPPO suele estar en «Aplicaciones descargadas». Tócalo y actívalo.",
            Accion.ACCESIBILIDAD
        ),
        Pregunta(
            "Mi teléfono dice «Controlado por ajustes restringidos»",
            "Pasa en Android 13 o más nuevo con apps instaladas fuera de Play Store. " +
                "Abre la información de la app, toca los tres puntos ⋮ arriba a la derecha y elige " +
                "«Permitir configuración restringida» (si no aparece, intenta activar Checa una vez en Accesibilidad y vuelve). " +
                "Después actívalo en Accesibilidad.",
            Accion.INFO_APP
        ),
        Pregunta(
            "Play Protect bloquea la instalación",
            "Toca «Instalar de todos modos». Si no aparece, apaga un momento «Analizar apps con Play Protect» " +
                "en la app de Play Store, instala Checa y vuelve a encenderlo."
        ),
        Pregunta(
            "La lectura se detuvo sola",
            "Android puede cortar la lectura al bloquear la pantalla, reiniciar el teléfono o ahorrar batería. " +
                "Toca ▶ para iniciarla otra vez. Para que pase menos, quita la restricción de batería a Checa.",
            Accion.BATERIA
        ),
        Pregunta(
            "Mi app del banco no abre o se bloquea",
            "Algunos bancos se bloquean cuando hay una app con Accesibilidad. Pausa el lector, usa tu banco " +
                "y luego vuelve a activar Checa en Accesibilidad.",
            Accion.PAUSAR
        ),
        Pregunta(
            "¿Cómo se calcula la ganancia neta?",
            "Es lo que paga el viaje menos la gasolina del trayecto completo (recogida + viaje). " +
                "La gasolina sale de tus km/l y precio por litro, que puedes cambiar en Ajustes → Gasolina."
        ),
        Pregunta(
            "¿Qué significan los colores?",
            "Verde: cumple tus metas de $/hora y $/km. Amarillo: cumple solo una. Rojo: no cumple ninguna, " +
                "o la recogida o la duración pasan tu máximo. Las metas se cambian en Filtros."
        ),
        Pregunta(
            "Uso pantalla dividida (Uber y DiDi juntos)",
            "Funciona. Pon DiDi a la izquierda y Uber a la derecha (o al revés) e inicia la lectura con " +
                "«Pantalla completa». Checa muestra un pop-up por app y marca con ⭐ la mejor oferta."
        ),
        Pregunta(
            "Una oferta se leyó mal",
            "Ve a Ajustes → Ayuda con la lectura → «Enviar reporte a soporte». Eliges qué ofertas mandar, " +
                "con su foto y lo que leyó Checa, y se envía por WhatsApp para mejorar la lectura.",
            Accion.SOPORTE
        ),
        Pregunta(
            "¿Qué datos guarda Checa?",
            "La lectura de ofertas se hace en tu teléfono, sin internet. En el teléfono quedan tus estadísticas (30 días) " +
                "y, si lo tienes encendido, las ofertas guardadas para ayuda con la lectura (7 días). En tu cuenta de Google " +
                "se guardan tu licencia, tus equipos y tus ajustes. A soporte solo llega lo que tú revisas y mandas. " +
                "Todo el detalle está en Cuenta → Tus datos."
        )
    )

    private fun servicioActivo(ctx: Context): Boolean {
        val activos = Settings.Secure.getString(
            ctx.contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        ) ?: return false
        val yo = ComponentName(ctx, RutaService::class.java)
        return activos.split(':').any { ComponentName.unflattenFromString(it) == yo }
    }

    private fun appsInstaladas(ctx: Context): List<String> = try {
        val lanzador = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        val pkgs = ctx.packageManager.queryIntentActivities(lanzador, 0)
            .map { it.activityInfo.packageName }
            .filter { val p = it.lowercase(); p.contains("uber") || p.contains("didi") }
            .toMutableSet()
        listOf("com.ubercab.driver" to "Uber Conductor", "com.didiglobal.driver" to "DiDi Conductor")
            .filter { (p, _) -> try { ctx.packageManager.getPackageInfo(p, 0); true } catch (_: Exception) { p in pkgs } }
            .map { it.second }
    } catch (_: Exception) { emptyList() }

    /** Revisa lo que suele fallar cuando no sale el pop-up. */
    fun diagnosticar(ctx: Context): List<Revision> {
        val r = mutableListOf<Revision>()
        val acc = servicioActivo(ctx)
        r += Revision(
            acc, "Accesibilidad",
            if (acc) "«Checa – lector de ofertas» está activo."
            else "Está apagado. Actívalo para que Checa pueda ver Uber y DiDi.",
            if (acc) Accion.NINGUNA else Accion.ACCESIBILIDAD
        )
        val leyendo = CapturaService.corriendo
        r += Revision(
            leyendo, "Lectura de pantalla",
            if (leyendo) "La lectura está encendida."
            else "No está leyendo. Toca el botón verde ▶ de abajo y elige «Pantalla completa»."
        )
        val est = Licencia.estado(ctx)
        r += Revision(
            est.activa, "Plan",
            when {
                est.beta -> "Beta gratis con todo incluido."
                est.pagada -> "Tu licencia está vigente."
                est.activa -> "Estás en tu prueba gratis."
                else -> "Tu licencia o prueba terminó. Los pop-ups no aparecen."
            },
            if (est.activa) Accion.NINGUNA else Accion.PAQUETES
        )
        if (Build.VERSION.SDK_INT >= 33) {
            val ok = ctx.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
            r += Revision(
                ok, "Notificaciones",
                if (ok) "Permitidas: verás el aviso de «Checa está leyendo»."
                else "Apagadas. Sin ellas Android puede cortar la lectura sin avisarte.",
                if (ok) Accion.NINGUNA else Accion.NOTIFICACIONES
            )
        }
        val pm = ctx.getSystemService(Context.POWER_SERVICE) as PowerManager
        val libre = pm.isIgnoringBatteryOptimizations(ctx.packageName)
        r += Revision(
            libre, "Batería",
            if (libre) "Sin restricción de batería."
            else "El ahorro de batería puede detener la lectura. Pon Checa en «Sin restricciones».",
            if (libre) Accion.NINGUNA else Accion.BATERIA
        )
        val prefs = Prefs(ctx)
        val activas = prefs.uber || prefs.didi
        r += Revision(
            activas, "Apps en Checa",
            if (activas) "Lee: " + listOfNotNull(if (prefs.uber) "Uber" else null, if (prefs.didi) "DiDi" else null).joinToString(" y ") + "."
            else "Uber y DiDi están apagadas en Ajustes → Apps. Enciende al menos una."
        )
        val apps = appsInstaladas(ctx)
        r += Revision(
            apps.isNotEmpty(), "Uber / DiDi",
            if (apps.isNotEmpty()) "Encontré: ${apps.joinToString(", ")}."
            else "No encontré la app de Uber ni de DiDi Conductor en este equipo."
        )
        return r
    }

    fun resumen(ctx: Context): String {
        val est = Licencia.estado(ctx)
        val plan = when {
            est.beta && !est.pagada -> "beta gratis"
            est.pagada -> "licencia"
            est.activa -> "prueba"
            else -> "sin licencia"
        }
        val ram = try {
            val mi = android.app.ActivityManager.MemoryInfo()
            (ctx.getSystemService(Context.ACTIVITY_SERVICE) as android.app.ActivityManager).getMemoryInfo(mi)
            String.format(java.util.Locale.US, "%.1f GB", mi.totalMem / 1_073_741_824.0)
        } catch (_: Exception) { "?" }
        val notif = Build.VERSION.SDK_INT < 33 ||
            ctx.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
        val bateria = (ctx.getSystemService(Context.POWER_SERVICE) as PowerManager).isIgnoringBatteryOptimizations(ctx.packageName)
        val ultima = Prefs(ctx).ultimaOferta
        return "Checa ${Actualizacion.instalada(ctx)} · Android ${Build.VERSION.RELEASE} · ${Build.MANUFACTURER} ${Build.MODEL} · RAM $ram\n" +
            "Cuenta: ${Cuenta.correo(ctx).ifEmpty { "sin sesión" }} · Plan: $plan\n" +
            "Accesibilidad: ${if (servicioActivo(ctx)) "sí" else "no"} · Lectura: ${if (CapturaService.corriendo) "sí" else "no"} · " +
            "Notificaciones: ${if (notif) "sí" else "no"} · Batería sin restricción: ${if (bateria) "sí" else "no"}\n" +
            "Última oferta: ${if (ultima == 0L) "ninguna" else Guardado.cuando(ultima)}"
    }

    /** Abre el chat con el borrador de una opinión sobre Checa (con permiso para publicarla o no). */
    fun opinion(ctx: Context) {
        val texto = "Hola, te cuento cómo me ha ido con Checa:\n\n\n\n" +
            "¿Puedes usar mi opinión en tu página? (sí/no): \n\n— Datos de mi equipo —\n" + resumen(ctx)
        try {
            ctx.startActivity(
                Intent(Intent.ACTION_VIEW, Uri.parse("https://wa.me/$WHATSAPP?text=" + Uri.encode(texto)))
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
        } catch (_: Exception) {
            val i = Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, texto)
            ctx.startActivity(Intent.createChooser(i, "Enviar opinión").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        }
    }

    /** Abre el chat de soporte con el mensaje ya armado. */
    fun escribir(ctx: Context, motivo: String, extra: String? = null) {
        val texto = buildString {
            append("Hola, necesito ayuda con Checa: ").append(motivo).append("\n\n")
            if (!extra.isNullOrBlank()) append(extra).append("\n\n")
            append("— Datos de mi equipo —\n").append(resumen(ctx))
        }
        try {
            ctx.startActivity(
                Intent(Intent.ACTION_VIEW, Uri.parse("https://wa.me/$WHATSAPP?text=" + Uri.encode(texto)))
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
        } catch (_: Exception) {
            val i = Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, texto)
            ctx.startActivity(Intent.createChooser(i, "Enviar a soporte").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        }
    }
}
