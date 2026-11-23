package com.karen.miruta

import java.util.Locale

/**
 * Diagnóstico OCR → Parser → resultado de UNA lectura (10.86). Kotlin puro, para probarlo en la JVM.
 *
 * Solo describe lo que ya pasó; no cambia ninguna lectura. Se arma ÚNICAMENTE cuando «Guardar pantallas de
 * ofertas» está encendido y la pantalla es nueva (misma puerta que la foto guardada), y su texto va dentro del
 * archivo .txt que ya se guarda junto a la captura, en el teléfono. No se manda a ningún servidor (la traza E3 sigue
 * mandando solo las líneas redactadas). Las líneas del OCR llevan su posición para ver el orden y las tarjetas.
 *
 * Por qué existe: las líneas que se guardaban para revisar (Confirmador.filtrar) son una copia más limpia que la
 * que recibe el Parser, y la captura puede ser posterior a la lectura; con esto se ve lo que de verdad entró.
 */
object DiagnosticoLectura {
    /** Una línea del OCR con su caja en la imagen (px). */
    class LineaOcr(val texto: String, val arriba: Int, val abajo: Int, val izq: Int, val der: Int)

    private const val MAX_LINEAS = 60
    private const val MAX_TEXTO = 120
    private const val MAX_PERDIDAS = 20

    private fun cortar(t: String): String {
        val u = t.replace('\n', ' ').trim()
        return if (u.length <= MAX_TEXTO) u else u.take(MAX_TEXTO) + "…"
    }

    private fun num(v: Double, dec: Int) = String.format(Locale.US, "%.${dec}f", v)

    /**
     * @param ocr todas las líneas que dio el OCR en la zona, en el orden en que se ordenaron (arriba→abajo, izquierda→derecha).
     * @param tarifaPrimero la línea que el lector subió al principio (la de monto más grande), si hubo.
     * @param tarjetasConservadas cuántas de las líneas dejó la separación por geometría (`Tarjetas.primera`); igual a
     *        `ocr.size` si no separó nada.
     * @param alParser las líneas exactas que recibió `Parser.analizar`.
     * @param alRevisor lo que de ellas pasaría a la revisión (`Confirmador.filtrar`): lo demás NO se guarda ni se manda.
     * @param eleccion `Parser.eleccionTarjeta()`: «sin_separar», «elegida» o «ambigua».
     * @param rechazo `Parser.motivoRechazo()`: por qué no hubo oferta (null si la hubo).
     */
    fun armar(
        fotograma: String?, ocr: List<LineaOcr>, tarifaPrimero: String?, tarjetasConservadas: Int,
        alParser: List<String>, alRevisor: List<String>, oferta: Oferta?, eleccion: String?, rechazo: String?,
        msOcr: Long, msParser: Long,
    ): String {
        val sb = StringBuilder("=== Diagnóstico por etapas (solo en este teléfono) ===\n")
        if (fotograma != null) sb.append("fotograma: ").append(fotograma).append('\n')

        sb.append("\n[1 OCR] ").append(ocr.size).append(" líneas, ").append(msOcr).append(" ms desde la captura\n")
        for ((i, l) in ocr.take(MAX_LINEAS).withIndex()) {
            sb.append("  ").append((i + 1).toString().padStart(2, '0')).append(" y=").append(l.arriba).append('-').append(l.abajo)
                .append(" x=").append(l.izq).append('-').append(l.der).append("  «").append(cortar(l.texto)).append("»\n")
        }
        if (ocr.size > MAX_LINEAS) sb.append("  … ").append(ocr.size - MAX_LINEAS).append(" líneas más\n")
        if (tarifaPrimero != null) sb.append("  subida al principio (monto más grande): «").append(cortar(tarifaPrimero)).append("»\n")
        if (tarjetasConservadas < ocr.size) {
            sb.append("  separación por tarjetas (geometría): se quedaron ").append(tarjetasConservadas).append(" de ").append(ocr.size).append('\n')
        }

        val perdidas = alParser.size - alRevisor.size
        sb.append("\n[2 PARSER] recibió ").append(alParser.size).append(" líneas, ").append(msParser).append(" ms\n")
        if (perdidas > 0) {
            sb.append("  ").append(perdidas).append(" no pasarían a la revisión (más de 60 caracteres, sin dígitos o con «total»; máx. 30):\n")
            val enviadas = HashMap<String, Int>()
            alRevisor.forEach { enviadas[it.trim()] = (enviadas[it.trim()] ?: 0) + 1 }
            var mostradas = 0
            for (l in alParser) {
                val k = l.trim()
                val n = enviadas[k] ?: 0
                if (n > 0) { enviadas[k] = n - 1; continue }
                if (mostradas < MAX_PERDIDAS) sb.append("    -  «").append(cortar(l)).append("»\n")
                mostradas++
            }
            if (mostradas > MAX_PERDIDAS) sb.append("    … ").append(mostradas - MAX_PERDIDAS).append(" más\n")
        }
        sb.append("  selección de tarjeta: ").append(eleccion ?: "—").append('\n')
        if (oferta != null) {
            sb.append("  leyó: tarifa=").append(num(oferta.tarifa, 2)).append(" km=").append(num(oferta.km, 2))
                .append(" min=").append(num(oferta.min, 0))
            if (oferta.recogidaKm > 0 || oferta.recogidaMin > 0) {
                sb.append(" recogida=").append(num(oferta.recogidaKm, 2)).append(" km/").append(num(oferta.recogidaMin, 0)).append(" min")
            }
            if (oferta.multiplicador > 0) sb.append(" multiplicador=").append(num(oferta.multiplicador, 2))
            sb.append('\n')
        } else {
            sb.append("  no leyó oferta: ").append(rechazo ?: "motivo no registrado").append('\n')
        }

        sb.append("\n[3 RESULTADO] ").append(if (oferta != null) "oferta enviada al pop-up" else "sin oferta (no se muestra pop-up)").append('\n')
        return sb.toString()
    }
}
