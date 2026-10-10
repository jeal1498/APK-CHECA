package com.karen.miruta

import android.app.Activity
import android.app.AlertDialog
import android.content.ComponentName
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.text.InputType
import android.util.TypedValue
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.SeekBar
import android.widget.Switch
import android.widget.TextView
import android.widget.Toast

class MainActivity : Activity() {
    private lateinit var prefs: Prefs
    private lateinit var estado: TextView
    private var popupPrueba: Popup? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        prefs = Prefs(this)
        popupPrueba = Popup(this, prefs, WindowManager.LayoutParams.TYPE_APPLICATION)

        val col = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(24), dp(20), dp(40))
        }

        col.addView(texto("Mi Ruta", 28f, true))
        col.addView(texto("Pop-up de $/km y $/hora para Uber y DiDi", 15f, false))

        // --- Activación ---
        col.addView(seccion("1. Activar"))
        estado = texto("", 16f, true)
        col.addView(estado)
        col.addView(boton("Abrir ajustes de Accesibilidad") {
            startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
        })
        col.addView(texto(
            "Busca \"Mi Ruta – lector de ofertas\" y actívalo.\n" +
            "Si aparece bloqueado (Android 13+): toca el botón de abajo → menú ⋮ → " +
            "\"Permitir configuración restringida\" y vuelve a intentarlo.", 13f, false))
        col.addView(boton("Abrir información de la app") {
            startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                Uri.parse("package:$packageName")))
        })

        // --- Umbrales ---
        col.addView(seccion("2. ¿Cuándo conviene?"))
        val campoKm = numero("Mínimo $ por km", prefs.minPorKm)
        val campoHora = numero("Mínimo $ por hora", prefs.minPorHora)
        col.addView(texto("Mínimo $ por km", 14f, false)); col.addView(campoKm)
        col.addView(texto("Mínimo $ por hora", 14f, false)); col.addView(campoHora)
        col.addView(texto("Verde: cumple ambos · Amarillo: cumple uno · Rojo: ninguno", 13f, false))
        col.addView(boton("Guardar mínimos") {
            campoKm.text.toString().replace(",", ".").toFloatOrNull()?.let { prefs.minPorKm = it }
            campoHora.text.toString().replace(",", ".").toFloatOrNull()?.let { prefs.minPorHora = it }
            Toast.makeText(this, "Guardado", Toast.LENGTH_SHORT).show()
        })

        // --- Pop-up ---
        col.addView(seccion("3. Pop-up"))
        col.addView(deslizador("Tamaño del texto", Prefs.TAM_MIN, Prefs.TAM_MAX, prefs.tamanoTexto, "") {
            prefs.tamanoTexto = it
        })
        col.addView(deslizador("Tiempo en pantalla", 3, 60, prefs.duracionSeg, " s") {
            prefs.duracionSeg = it
        })
        col.addView(deslizador("Posición (desde arriba)", 0, 80, prefs.posicion, " %") {
            prefs.posicion = it
        })
        col.addView(texto("También puedes usar A− / A+ dentro del pop-up.", 13f, false))
        col.addView(boton("Probar pop-up") {
            popupPrueba?.mostrar(Oferta(85.0, 7.2, 24.0))
        })

        // --- Apps ---
        col.addView(seccion("4. Apps"))
        col.addView(interruptor("Uber Driver", prefs.uber) { prefs.uber = it })
        col.addView(interruptor("DiDi Conductor", prefs.didi) { prefs.didi = it })

        // --- Diagnóstico ---
        col.addView(seccion("5. Diagnóstico"))
        col.addView(texto(
            "Si el pop-up no sale o lee mal los datos, activa esto, recibe una oferta " +
            "y luego comparte las capturas de texto para ajustar la app.", 13f, false))
        col.addView(interruptor("Guardar texto de pantalla", prefs.diagnostico) { prefs.diagnostico = it })
        col.addView(boton("Ver / compartir capturas") { verCapturas() })

        setContentView(ScrollView(this).apply {
            setBackgroundColor(Color.WHITE)
            addView(col)
        })
    }

    override fun onResume() {
        super.onResume()
        if (servicioActivo()) {
            estado.text = "✅ Servicio activado"
            estado.setTextColor(Color.rgb(27, 142, 62))
        } else {
            estado.text = "⚠️ Servicio desactivado"
            estado.setTextColor(Color.rgb(198, 40, 40))
        }
    }

    override fun onPause() {
        popupPrueba?.ocultar()
        super.onPause()
    }

    private fun servicioActivo(): Boolean {
        val activos = Settings.Secure.getString(
            contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES) ?: return false
        val yo = ComponentName(this, RutaService::class.java)
        return activos.split(':').any {
            ComponentName.unflattenFromString(it) == yo
        }
    }

    private fun verCapturas() {
        val caps = prefs.capturas().ifBlank { "Aún no hay capturas." }
        val vista = TextView(this).apply {
            text = caps
            setTextIsSelectable(true)
            setPadding(dp(16), dp(12), dp(16), dp(12))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
        }
        AlertDialog.Builder(this)
            .setTitle("Capturas")
            .setView(ScrollView(this).apply { addView(vista) })
            .setPositiveButton("Compartir") { _, _ ->
                val i = Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_TEXT, caps)
                }
                startActivity(Intent.createChooser(i, "Compartir capturas"))
            }
            .setNeutralButton("Borrar") { _, _ -> prefs.borrarCapturas() }
            .setNegativeButton("Cerrar", null)
            .show()
    }

    // ---------- helpers de UI ----------

    private fun texto(s: String, tam: Float, negrita: Boolean): TextView =
        TextView(this).apply {
            text = s
            setTextColor(Color.rgb(30, 30, 30))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, tam)
            if (negrita) setTypeface(typeface, Typeface.BOLD)
            setPadding(0, dp(4), 0, dp(4))
        }

    private fun seccion(s: String): TextView =
        texto(s, 19f, true).apply { setPadding(0, dp(24), 0, dp(6)) }

    private fun boton(s: String, accion: () -> Unit): Button =
        Button(this).apply {
            text = s
            isAllCaps = false
            setOnClickListener { accion() }
        }

    private fun numero(pista: String, valor: Float): EditText =
        EditText(this).apply {
            hint = pista
            inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL
            setText(if (valor % 1f == 0f) valor.toInt().toString() else valor.toString())
        }

    private fun interruptor(s: String, valor: Boolean, cambio: (Boolean) -> Unit): Switch =
        Switch(this).apply {
            text = s
            textSize = 16f
            isChecked = valor
            setPadding(0, dp(8), 0, dp(8))
            setOnCheckedChangeListener { _, b -> cambio(b) }
        }

    private fun deslizador(
        titulo: String, min: Int, max: Int, valor: Int, unidad: String, cambio: (Int) -> Unit
    ): View {
        val caja = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        val etiqueta = texto("$titulo: $valor$unidad", 14f, false)
        val barra = SeekBar(this).apply {
            this.max = max - min
            progress = (valor - min).coerceIn(0, max - min)
            setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(sb: SeekBar?, p: Int, deUsuario: Boolean) {
                    val v = p + min
                    etiqueta.text = "$titulo: $v$unidad"
                    if (deUsuario) cambio(v)
                }
                override fun onStartTrackingTouch(sb: SeekBar?) {}
                override fun onStopTrackingTouch(sb: SeekBar?) {}
            })
        }
        caja.addView(etiqueta)
        caja.addView(barra)
        return caja
    }

    private fun dp(v: Int): Int = (v * resources.displayMetrics.density).toInt()
}
