package com.karen.miruta

/**
 * Novedades de cada versión (10.34): el aviso sale una sola vez, al abrir la app tras actualizar,
 * y «Cuenta › Novedades» muestra las últimas tres. La primera de la lista es la más reciente:
 * en cada versión nueva se agrega un bloque arriba, en lenguaje de conductor (nada técnico).
 */
object Novedades {
    class Version(val codigo: Int, val nombre: String, val puntos: List<String>, val widgets: Boolean = false, val vistaQr: Boolean = false)

    val LISTA = listOf(
        Version(137, "10.57", listOf(
            "Lectura más eficiente: menos batería cuando la oferta ya está confirmada."
        )),
        Version(136, "10.56", listOf(
            "Al abrir Checa te avisamos cuando hay una versión nueva en Google Play y se instala sin salir de la app."
        )),
        Version(135, "10.55", listOf(
            "Con el teléfono en vertical sale un solo pop-up: el de la oferta que acaba de llegar. Dos a la vez, solo en horizontal."
        )),
        Version(134, "10.54", listOf(
            "El pop-up se quita solo cuando la oferta ya no está en pantalla.",
            "Con varias ofertas a la vista (lista de DiDi) se lee cada una por separado."
        )),
        Version(123, "10.43", listOf(
            "Los widgets ahora se ven con su propia vista previa al elegirlos, con datos de ejemplo."
        )),
        Version(122, "10.42", listOf(
            "Widget nuevo «Mi QR»: un toque muestra un código para que tus pasajeros te escriban por WhatsApp.",
            "Así te piden viajes por fuera de las apps. La primera vez que lo tocas pones tu número de WhatsApp; el nombre es el de tu cuenta de Google y todo queda ligado a ella.",
            "También lo abres en Ajustes › País y seguridad › Mi WhatsApp para pasajeros."
        ), vistaQr = true),
        Version(121, "10.41", listOf(
            "Widgets nuevos para tu pantalla de inicio: un mosaico 4×2 con tu día, descanso, lectura y SOS.",
            "También por separado: Mi día, Descanso y SOS (el de encender y apagar ya lo tenías).",
            "Descanso cuenta tus horas manejando seguidas y te avisa a las 4 h. Una pausa de 10 min lo reinicia.",
            "SOS en un toque desde tu pantalla de inicio."
        ), widgets = true),
        Version(120, "10.40", listOf(
            "Al abrir Uber o DiDi, Checa enciende sola la lectura y avisa «Lista para trabajar».",
            "Al terminar tu jornada, apágala tú: toca «Apagar ahora» o «Detener» en la notificación.",
            "Si pasan 30 minutos sin Uber ni DiDi con la lectura encendida, te sugiere apagarla.",
            "Puedes desactivarlo en Ajustes › «Iniciar sola al abrir Uber o DiDi»."
        )),
        Version(119, "10.39", listOf(
            "Si cambias de teléfono, tu país y tu apariencia (claro u oscuro) regresan con tu cuenta.",
            "Mejores horas también regresa: guardamos solo el resumen por hora, no cada oferta.",
            "Mi día recupera los viajes de esta semana y tu avance de la meta."
        )),
        Version(118, "10.38", listOf(
            "Arreglo: Checa ya no deja de leer después de un rato; si la lectura se traba, se reanuda sola en 10 segundos."
        )),
        Version(117, "10.37", listOf(
            "Arreglo: tras bloquear y desbloquear el teléfono, Checa vuelve a leer las ofertas sola."
        )),
        Version(116, "10.36", listOf(
            "Arreglo: si tu país es México, la moneda ya no se queda en dólares y Checa vuelve a mostrar el pop-up."
        )),
        Version(115, "10.35", listOf(
            "La apariencia ahora viene en Automático: sigue el modo claro u oscuro de tu teléfono."
        )),
        Version(114, "10.34", listOf(
            "Modo claro: en Ajustes › País › Apariencia elige Oscuro, Claro o Automático (sigue a tu teléfono).",
            "Pantallas más compactas: caben más cosas sin bajar tanto.",
            "Colores más fáciles de leer en modo claro.",
            "Novedades: aquí verás lo nuevo de cada versión y lo puedes releer en Cuenta."
        )),
        Version(110, "10.30", listOf(
            "«Compartir mi día» ahora es una tarjeta con el color del semáforo.",
            "Widget nuevo sobre fondo asfalto."
        )),
        Version(109, "10.29", listOf(
            "Avisos de actualización con versión, porcentaje grande y cuenta regresiva.",
            "Paquetes con 2 beneficios y «Ver todo»; Invitar con código y botón «Copiar»."
        ))
    )

    val ultima: Version get() = LISTA.first()
}
