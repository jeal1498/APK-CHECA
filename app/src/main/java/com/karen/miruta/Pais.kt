package com.karen.miruta

import java.util.Locale

/**
 * País donde maneja el conductor: define la moneda (cómo se leen y muestran los montos)
 * y el número de emergencias del SOS. Se guarda en `Prefs.pais` (código ISO, «MX»);
 * si está vacío se deduce de la moneda guardada y, para el dólar, del idioma del teléfono.
 */
data class Pais(val codigo: String, val nombre: String, val moneda: Moneda, val sos: String) {
    companion object {
        private fun mon(c: String) = Moneda.LISTA.first { it.first.codigo == c }.first

        val LISTA: List<Pais> = listOf(
            Pais("MX", "México", mon("MXN"), "911"),
            Pais("CO", "Colombia", mon("COP"), "123"),
            Pais("AR", "Argentina", mon("ARS"), "911"),
            Pais("CL", "Chile", mon("CLP"), "133"),
            Pais("PE", "Perú", mon("PEN"), "105"),
            Pais("EC", "Ecuador", mon("USD"), "911"),
            Pais("PA", "Panamá", mon("USD"), "911"),
            Pais("SV", "El Salvador", mon("USD"), "911"),
            Pais("GT", "Guatemala", mon("GTQ"), "110"),
            Pais("CR", "Costa Rica", mon("CRC"), "911"),
            Pais("DO", "Rep. Dominicana", mon("DOP"), "911"),
            Pais("UY", "Uruguay", mon("UYU"), "911"),
            Pais("BO", "Bolivia", mon("BOB"), "110"),
            Pais("BR", "Brasil", mon("BRL"), "190"),
            Pais("US", "Estados Unidos", mon("USD"), "911"),
            Pais("ES", "España", mon("EUR"), "112")
        )

        fun de(codigo: String): Pais? = LISTA.firstOrNull { it.codigo == codigo }

        /** El país del teléfono (idioma/región), si está en la lista; si no, México. */
        fun detectado(): Pais = de(Locale.getDefault().country.uppercase(Locale.US)) ?: LISTA.first()

        /** El elegido; si no hay, el que corresponde a la moneda guardada. */
        fun actual(p: Prefs): Pais {
            de(p.pais)?.let { return it }
            val m = p.moneda.codigo
            val tel = detectado()
            if (tel.moneda.codigo == m) return tel
            return LISTA.firstOrNull { it.moneda.codigo == m } ?: LISTA.first()
        }
    }
}
