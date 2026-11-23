package com.karen.miruta

/**
 * 10.86: las últimas capturas ya comprimidas, buscadas por el fotograma que leyó el OCR (Kotlin puro, se prueba en la JVM).
 *
 * Antes el teléfono subía «la captura más reciente de esa app» cuando el servidor respondía, segundos después: podía ser de
 * otra oferta o traer ya el pop-up nuevo (los 18 casos de desfase de la auditoría de la 10.85 salieron todos de ahí). Ahora
 * se sube solo la del fotograma de esa lectura; si ya no está (la reemplazaron las siguientes) no se sube nada, que es mejor
 * que subir una de otro momento.
 */
class CapturasPorFotograma(private val maximo: Int = 6, private val vigenteMs: Long = 30_000L) {
    private val mapa = LinkedHashMap<String, Pair<Long, ByteArray>>()

    @Synchronized fun guardar(fotograma: String, bytes: ByteArray, ahoraMs: Long) {
        mapa.remove(fotograma)
        mapa[fotograma] = ahoraMs to bytes
        while (mapa.size > maximo) mapa.remove(mapa.keys.first())
    }

    /** Toma (y olvida) la captura de [fotograma]; null si no hay o ya es vieja. Nunca devuelve la de otro fotograma. */
    @Synchronized fun tomar(fotograma: String?, ahoraMs: Long): ByteArray? {
        if (fotograma == null) return null
        val p = mapa.remove(fotograma) ?: return null
        return if (ahoraMs - p.first <= vigenteMs) p.second else null
    }

    @Synchronized fun vaciar() = mapa.clear()
    @Synchronized fun tamano() = mapa.size
}
