package com.karen.miruta

import android.app.Activity
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.text.InputType
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel

/**
 * Pop-up con el QR de contacto (10.42): el pasajero lo escanea con la cámara de su teléfono y
 * «Agregar contacto» le guarda el nombre y el número del conductor, para pedirle viajes por fuera
 * de las apps. Se abre desde el widget «Mi QR» (ventana translúcida sobre la pantalla de inicio)
 * y desde Ajustes. La primera vez pide nombre y número. Pantalla al máximo brillo para escanear.
 */
class QrActivity : Activity() {
    private lateinit var prefs: Prefs
    private lateinit var raiz: FrameLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        prefs = Prefs(this)
        Fuentes.iniciar(this)
        window.attributes = window.attributes.apply { screenBrightness = 1f }
        raiz = FrameLayout(this).apply {
            setBackgroundColor(Color.argb(170, 0, 0, 0))
            setOnClickListener { finish() }
        }
        setContentView(raiz)
        if (prefs.contactoTel.isEmpty() || prefs.contactoNombre.isEmpty()) formulario(false) else mostrarQr()
    }

    // ------------------------------------------------------------------
    // QR
    // ------------------------------------------------------------------

    private fun mostrarQr() {
        val nombre = prefs.contactoNombre
        val tel = prefs.contactoTel
        val tarjeta = tarjeta()
        tarjeta.addView(titulo("Guárdame como contacto"))
        tarjeta.addView(nota("Escanea con la cámara de tu celular y toca «Agregar contacto»."), margen(6))

        val img = ImageView(this).apply {
            scaleType = ImageView.ScaleType.FIT_CENTER
            setPadding(dp(12), dp(12), dp(12), dp(12))
            background = GradientDrawable().apply { setColor(Color.WHITE); cornerRadius = dp(16).toFloat() }
            contentDescription = "Código QR de contacto de $nombre"
        }
        val bmp = crearQr(vcard(nombre, tel))
        if (bmp != null) img.setImageBitmap(bmp)
        tarjeta.addView(img, LinearLayout.LayoutParams(dp(248), dp(248)).apply { topMargin = dp(14); gravity = Gravity.CENTER_HORIZONTAL })

        tarjeta.addView(TextView(this).apply {
            text = nombre
            setTextColor(Oscuro.HUESO)
            typeface = Fuentes.titulo
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 20f)
            gravity = Gravity.CENTER
        }, margen(12))
        tarjeta.addView(TextView(this).apply {
            text = "+$tel"
            setTextColor(Oscuro.TENUE)
            typeface = Fuentes.numero
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f)
            gravity = Gravity.CENTER
        }, margen(2))

        val fila = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        fila.addView(boton("Editar", false) { formulario(true) }, LinearLayout.LayoutParams(0, dp(48), 1f).apply { rightMargin = dp(4) })
        fila.addView(boton("Cerrar", true) { finish() }, LinearLayout.LayoutParams(0, dp(48), 1f).apply { leftMargin = dp(4) })
        tarjeta.addView(fila, margen(14))
        poner(tarjeta)
    }

    /** Tarjeta de contacto vCard 3.0: la cámara del pasajero ofrece guardarla tal cual. */
    private fun vcard(nombre: String, tel: String): String {
        fun esc(s: String) = s.replace("\\", "\\\\").replace(";", "\\;").replace(",", "\\,").replace("\n", " ")
        return "BEGIN:VCARD\r\nVERSION:3.0\r\nN:;${esc(nombre)};;;\r\nFN:${esc(nombre)}\r\nTEL;TYPE=CELL:+$tel\r\nEND:VCARD\r\n"
    }

    private fun crearQr(texto: String): Bitmap? = try {
        val hints = mapOf(
            EncodeHintType.CHARACTER_SET to "UTF-8",
            EncodeHintType.MARGIN to 0,
            EncodeHintType.ERROR_CORRECTION_LEVEL to ErrorCorrectionLevel.M
        )
        val m = QRCodeWriter().encode(texto, BarcodeFormat.QR_CODE, 0, 0, hints)
        val n = m.width
        val esc = (720 / n).coerceAtLeast(1)
        val lado = n * esc
        val px = IntArray(lado * lado)
        for (y in 0 until lado) {
            val fila = (y / esc)
            for (x in 0 until lado) {
                px[y * lado + x] = if (m.get(x / esc, fila)) Color.BLACK else Color.WHITE
            }
        }
        Bitmap.createBitmap(px, lado, lado, Bitmap.Config.ARGB_8888)
    } catch (e: Exception) { null }

    // ------------------------------------------------------------------
    // Formulario (primera vez y «Editar»)
    // ------------------------------------------------------------------

    private fun formulario(editando: Boolean) {
        val tarjeta = tarjeta()
        tarjeta.addView(titulo(if (editando) "Tu contacto" else "Crea tu QR de contacto"))
        tarjeta.addView(nota("Tus pasajeros te guardan con este nombre y número para pedirte viajes por fuera de las apps."), margen(6))

        fun campo(pista: String, tipo: Int, valor: String) = EditText(this).apply {
            hint = pista
            setHintTextColor(Oscuro.TENUE)
            setTextColor(Oscuro.HUESO)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 16f)
            setSingleLine()
            inputType = tipo
            setText(valor)
            background = GradientDrawable().apply { setColor(Oscuro.CAMPO); cornerRadius = dp(14).toFloat() }
            setPadding(dp(14), 0, dp(14), 0)
        }
        val nombre = campo("Cómo te guardarán (ej. Eduardo chofer)",
            InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_CAP_WORDS, prefs.contactoNombre)
        val tel = campo(if (prefs.moneda.codigo == "MXN") "Tu número, 10 dígitos" else "Tu número con lada del país",
            InputType.TYPE_CLASS_PHONE, prefs.contactoTel.let { if (prefs.moneda.codigo == "MXN" && it.startsWith("52") && it.length == 12) it.substring(2) else it })
        tarjeta.addView(nombre, LinearLayout.LayoutParams(-1, dp(48)).apply { topMargin = dp(14) })
        tarjeta.addView(tel, LinearLayout.LayoutParams(-1, dp(48)).apply { topMargin = dp(8) })
        val error = TextView(this).apply {
            setTextColor(Oscuro.AMARILLO)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
            typeface = Fuentes.texto
        }
        tarjeta.addView(error, margen(6))

        val fila = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        fila.addView(boton(if (editando) "Cancelar" else "Cerrar", false) {
            if (editando) mostrarQr() else finish()
        }, LinearLayout.LayoutParams(0, dp(48), 1f).apply { rightMargin = dp(4) })
        fila.addView(boton("Guardar", true) {
            val n = nombre.text.toString().trim()
            val t = Sos.normalizarTelefono(tel.text.toString(), prefs.moneda)
            when {
                n.isEmpty() -> { error.text = "Escribe tu nombre." }
                t == null -> {
                    error.text = if (prefs.moneda.codigo == "MXN") "El número lleva 10 dígitos." else "Revisa el número (con lada del país)."
                }
                else -> {
                    prefs.contactoNombre = n
                    prefs.contactoTel = t
                    mostrarQr()
                }
            }
        }, LinearLayout.LayoutParams(0, dp(48), 1f).apply { leftMargin = dp(4) })
        tarjeta.addView(fila, margen(12))
        poner(tarjeta)
    }

    // ------------------------------------------------------------------
    // Piezas
    // ------------------------------------------------------------------

    private fun tarjeta(): LinearLayout = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        setPadding(dp(20), dp(20), dp(20), dp(18))
        background = GradientDrawable().apply {
            setColor(Oscuro.TABLERO)
            cornerRadius = dp(24).toFloat()
            setStroke(dp(1), Oscuro.BORDE)
        }
        isClickable = true // un toque dentro de la tarjeta no la cierra
    }

    private fun poner(tarjeta: View) {
        raiz.removeAllViews()
        val ancho = minOf((resources.displayMetrics.widthPixels * 0.92f).toInt(), dp(340))
        raiz.addView(tarjeta, FrameLayout.LayoutParams(ancho, FrameLayout.LayoutParams.WRAP_CONTENT, Gravity.CENTER))
    }

    private fun titulo(t: String) = TextView(this).apply {
        text = t
        setTextColor(Oscuro.HUESO)
        typeface = Fuentes.titulo
        setTextSize(TypedValue.COMPLEX_UNIT_SP, 22f)
        gravity = Gravity.CENTER
    }

    private fun nota(t: String) = TextView(this).apply {
        text = t
        setTextColor(Oscuro.TENUE)
        typeface = Fuentes.texto
        setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f)
        gravity = Gravity.CENTER
        setLineSpacing(0f, 1.15f)
    }

    private fun boton(t: String, lleno: Boolean, clic: () -> Unit) = TextView(this).apply {
        text = t
        gravity = Gravity.CENTER
        typeface = Fuentes.negrita
        setTextSize(TypedValue.COMPLEX_UNIT_SP, 15f)
        setTextColor(if (lleno) Oscuro.ASFALTO else Oscuro.HUESO)
        background = GradientDrawable().apply {
            cornerRadius = dp(14).toFloat()
            if (lleno) setColor(Oscuro.HUESO) else { setColor(Oscuro.TABLERO); setStroke(dp(1), Oscuro.HUMO) }
        }
        isClickable = true
        setOnClickListener { clic() }
    }

    private fun margen(arriba: Int) = LinearLayout.LayoutParams(
        LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT
    ).apply { topMargin = dp(arriba) }

    private fun dp(v: Int): Int = (v * resources.displayMetrics.density).toInt()
}
