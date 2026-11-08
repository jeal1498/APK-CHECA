package com.karen.miruta

/**
 * Novedades de cada versión (10.34): el aviso sale una sola vez, al abrir la app tras actualizar,
 * y «Cuenta › Novedades» muestra las últimas tres. La primera de la lista es la más reciente:
 * en cada versión nueva se agrega un bloque arriba, en lenguaje de conductor (nada técnico).
 */
object Novedades {
    class Version(val codigo: Int, val nombre: String, val puntos: List<String>)

    val LISTA = listOf(
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
