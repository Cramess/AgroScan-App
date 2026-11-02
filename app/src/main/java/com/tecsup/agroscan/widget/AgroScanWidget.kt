package com.tecsup.agroscan.widget

import android.content.Context
import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.LocalSize
import androidx.glance.action.ActionParameters
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.color.ColorProvider
import androidx.glance.layout.*
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import com.tecsup.agroscan.MainActivity
import com.tecsup.agroscan.data.local.AppDatabase
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.concurrent.TimeUnit

private val GlassBgDay = Color(0xF3E8F0E2)
private val GlassBgNight = Color(0xF31E2B14)
private val TitleColorDay = Color(0xFF23331A)
private val TitleColorNight = Color(0xFFE3F0D8)
private val SubColorDay = Color(0xFF4A5C3D)
private val SubColorNight = Color(0xFFCBD8C0)
private val AlertaColor = Color(0xFFE67E22)

const val PREFS_WIDGET = "agroscan_widget_prefs"
const val PREF_CLIMA_TEMPERATURA = "clima_temperatura"
const val PREF_CLIMA_HUMEDAD = "clima_humedad"
const val PREF_CLIMA_ALERTA = "clima_alerta"

private data class UltimoDiagnostico(val nombre: String, val detalle: String, val color: Color)
private data class ProximaCosecha(val zona: String, val dias: Long)

class AgroScanWidget : GlanceAppWidget() {

    override val sizeMode = SizeMode.Exact

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val dao = AppDatabase.getDatabase(context).agroScanDao()
        val zonas = dao.obtenerTodasLasZonas()
        val historial = dao.obtenerTodoElHistorial()
        val prefs = context.getSharedPreferences(PREFS_WIDGET, Context.MODE_PRIVATE)

        val resumenCampo = String.format(
            Locale.getDefault(), "%d %s · %.2f ha",
            zonas.size, if (zonas.size == 1) "zona" else "zonas", zonas.sumOf { it.hectareas }
        )

        // "Registro de Zona" también vive en el historial; solo interesan los diagnósticos reales
        val analisis = historial.filter { it.titulo == "Análisis IA" }
        val porValidar = analisis.count { it.estadoValidacion == "PENDIENTE" }
        val hoy = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(java.util.Date())
        val ultimo = analisis.firstOrNull()?.let {
            val cuando = if (it.fecha == hoy) "hoy ${it.hora}" else it.fecha
            UltimoDiagnostico(it.descripcion, "${it.nombreZona} · $cuando", Color(it.valorColor))
        }

        val proximaCosecha = zonas
            .mapNotNull { zona -> diasHasta(zona.fechaCosecha)?.let { ProximaCosecha(zona.nombre, it) } }
            .filter { it.dias >= 0 }
            .minByOrNull { it.dias }

        val temperatura = prefs.getString(PREF_CLIMA_TEMPERATURA, null)
        val humedad = prefs.getString(PREF_CLIMA_HUMEDAD, null)
        val alertaClima = prefs.getString(PREF_CLIMA_ALERTA, null)

        provideContent {
            GlanceTheme {
                // En tamaños pequeños se muestra solo lo esencial
                val compacto = LocalSize.current.height < 170.dp

                Box(
                    modifier = GlanceModifier
                        .fillMaxSize()
                        .background(ColorProvider(day = GlassBgDay, night = GlassBgNight))
                        .cornerRadius(28.dp)
                        .padding(14.dp)
                        .clickable(actionRunCallback<AbrirCamaraAction>())
                ) {
                    Column(modifier = GlanceModifier.fillMaxSize()) {
                        Row(
                            modifier = GlanceModifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "AgroScan",
                                style = TextStyle(
                                    color = ColorProvider(day = TitleColorDay, night = TitleColorNight),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp
                                ),
                                modifier = GlanceModifier.defaultWeight()
                            )
                            if (temperatura != null) {
                                TextoSecundario(
                                    texto = listOfNotNull(temperatura, humedad?.let { "HR $it" }).joinToString(" · "),
                                    negrita = true
                                )
                            }
                        }

                        TextoSecundario(texto = resumenCampo, tamano = 11)

                        if (alertaClima != null) {
                            Spacer(modifier = GlanceModifier.height(6.dp))
                            Fila(colorPunto = AlertaColor, texto = "⚠ $alertaClima", negrita = true)
                        }

                        Spacer(modifier = GlanceModifier.height(8.dp))

                        if (ultimo != null) {
                            Fila(colorPunto = ultimo.color, texto = ultimo.nombre, negrita = true, titulo = true)
                            Row {
                                Spacer(modifier = GlanceModifier.width(16.dp))
                                TextoSecundario(texto = ultimo.detalle, tamano = 11)
                            }
                        } else {
                            Fila(colorPunto = Color(0xFF9E9E9E), texto = "Sin diagnósticos aún")
                        }

                        if (!compacto) {
                            if (porValidar > 0) {
                                Spacer(modifier = GlanceModifier.height(6.dp))
                                Fila(
                                    colorPunto = Color(0xFFFFC107),
                                    texto = "$porValidar análisis por validar"
                                )
                            }
                            if (proximaCosecha != null) {
                                Spacer(modifier = GlanceModifier.height(6.dp))
                                val cuando = when (proximaCosecha.dias) {
                                    0L -> "hoy"
                                    1L -> "mañana"
                                    else -> "en ${proximaCosecha.dias} días"
                                }
                                Fila(colorPunto = Color(0xFF29B6F6), texto = "Cosecha ${proximaCosecha.zona}: $cuando")
                            }
                        }

                        Spacer(modifier = GlanceModifier.defaultWeight())

                        Box(
                            modifier = GlanceModifier
                                .fillMaxWidth()
                                .background(ColorProvider(day = Color(0xFF2E401F), night = Color(0xFFC0E0A0)))
                                .cornerRadius(20.dp)
                                .padding(vertical = 9.dp, horizontal = 12.dp)
                                .clickable(actionRunCallback<AbrirCamaraAction>()),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Escanear hoja",
                                style = TextStyle(
                                    color = ColorProvider(day = Color.White, night = Color(0xFF2E401F)),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp
                                )
                            )
                        }
                    }
                }
            }
        }
    }

    private fun diasHasta(fecha: String): Long? {
        if (fecha.isBlank()) return null
        return try {
            val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).apply { isLenient = false }
            val objetivo = sdf.parse(fecha) ?: return null
            val hoy = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
            }
            TimeUnit.MILLISECONDS.toDays(objetivo.time - hoy.timeInMillis)
        } catch (e: Exception) {
            null
        }
    }
}

class AbrirCamaraAction : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        val prefs = context.getSharedPreferences(PREFS_WIDGET, Context.MODE_PRIVATE)
        prefs.edit().putBoolean("abrir_camara", true).apply()

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("ABRIR_CAMARA", true)
        }
        context.startActivity(intent)
    }
}

@Composable
private fun Fila(colorPunto: Color, texto: String, negrita: Boolean = false, titulo: Boolean = false) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = GlanceModifier
                .size(8.dp)
                .background(ColorProvider(day = colorPunto, night = colorPunto))
                .cornerRadius(4.dp)
        ) {}
        Spacer(modifier = GlanceModifier.width(8.dp))
        Text(
            text = texto,
            maxLines = 1,
            style = TextStyle(
                color = if (titulo) ColorProvider(day = TitleColorDay, night = TitleColorNight)
                        else ColorProvider(day = SubColorDay, night = SubColorNight),
                fontWeight = if (negrita) FontWeight.Bold else FontWeight.Medium,
                fontSize = if (titulo) 14.sp else 12.sp
            )
        )
    }
}

@Composable
private fun TextoSecundario(texto: String, tamano: Int = 12, negrita: Boolean = false) {
    Text(
        text = texto,
        maxLines = 1,
        style = TextStyle(
            color = ColorProvider(day = SubColorDay, night = SubColorNight),
            fontWeight = if (negrita) FontWeight.Bold else FontWeight.Medium,
            fontSize = tamano.sp
        )
    )
}

class AgroScanWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = AgroScanWidget()
}
