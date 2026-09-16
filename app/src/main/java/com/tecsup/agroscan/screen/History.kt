package com.tecsup.agroscan.screen

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tecsup.agroscan.viewmodel.MainViewModel
import com.tecsup.agroscan.data.ResultadoAnalisis
import com.tecsup.agroscan.ui.utils.bounceClick

@Composable
fun PantallaHistorial(viewModel: MainViewModel) {
    var consultaBusqueda by remember { mutableStateOf("") }
    val zonasExpandidas = remember { mutableStateMapOf<String, Boolean>() }

    var analisisParaValidar by remember { mutableStateOf<ResultadoAnalisis?>(null) }

    val historialFiltrado = remember(consultaBusqueda, viewModel.historialAnalisis.size) {
        if (consultaBusqueda.isEmpty()) {
            viewModel.historialAnalisis
        } else {
            viewModel.historialAnalisis.filter {
                it.nombrePlanta.contains(consultaBusqueda, ignoreCase = true) ||
                it.descripcion.contains(consultaBusqueda, ignoreCase = true) ||
                it.nombreZona.contains(consultaBusqueda, ignoreCase = true)
            }
        }
    }

    val historialAgrupado = historialFiltrado.groupBy { it.nombreZona }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 24.dp)
    ) {
        Spacer(modifier = Modifier.height(48.dp))

        Text(
            text = "Historial",
            fontSize = 32.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = consultaBusqueda,
            onValueChange = { consultaBusqueda = it },
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("Buscar análisis...") },
            leadingIcon = { Icon(Icons.Default.Search, null) },
            shape = RoundedCornerShape(24.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = MaterialTheme.colorScheme.surface,
                unfocusedContainerColor = MaterialTheme.colorScheme.surface
            ),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(24.dp))

        if (historialAgrupado.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    text = if (consultaBusqueda.isEmpty()) "No hay análisis registrados" else "Sin resultados",
                    color = Color.Gray
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 140.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                historialAgrupado.forEach { (zona, analisisEnZona) ->
                    item {
                        val estaExpandido = zonasExpandidas[zona] ?: false
                        TarjetaZonaHistorial(
                            nombreZona = zona,
                            cantidadAnalisis = analisisEnZona.size,
                            estaExpandido = estaExpandido,
                            onClick = { zonasExpandidas[zona] = !estaExpandido }
                        )
                    }
                    
                    item {
                        AnimatedVisibility(
                            visible = zonasExpandidas[zona] ?: false,
                            enter = expandVertically(animationSpec = tween(400)) + fadeIn(),
                            exit = shrinkVertically(animationSpec = tween(400)) + fadeOut()
                        ) {
                            Column(
                                modifier = Modifier.padding(start = 8.dp, top = 8.dp, bottom = 16.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                analisisEnZona.forEach { analisis ->
                                    TarjetaHistorialCompacta(
                                        analisis = analisis,
                                        alEditarValidacion = { analisisParaValidar = analisis }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // --- DIÁLOGO DE VALIDACIÓN HÍBRIDA (HUMANO EN EL BUCLE) ---
        analisisParaValidar?.let { analisis ->
            DialogoValidacionHumana(
                analisis = analisis,
                alGuardar = { estado, obs, esNutricional ->
                    viewModel.actualizarValidacionAnalisis(analisis, estado, obs, esNutricional)
                    analisisParaValidar = null
                },
                alCancelar = { analisisParaValidar = null }
            )
        }
    }
}

@Composable
fun DialogoValidacionHumana(
    analisis: ResultadoAnalisis,
    alGuardar: (String, String, Boolean) -> Unit,
    alCancelar: () -> Unit
) {
    var estadoSeleccionado by remember { mutableStateOf(analisis.estadoValidacion) }
    var observacion by remember { mutableStateOf(analisis.observacionTecnico) }
    var esNutricional by remember { mutableStateOf(analisis.esDeficienciaNutricional) }

    AlertDialog(
        onDismissRequest = alCancelar,
        shape = RoundedCornerShape(24.dp),
        title = { Text("Validación Técnica / Agrónomo", fontWeight = FontWeight.Bold, fontSize = 18.sp) },
        text = {
            Column {
                Text("Planta: ${analisis.nombrePlanta} • Zona: ${analisis.nombreZona}", fontSize = 13.sp, color = Color.Gray)
                Text("Diagnóstico IA: ${analisis.descripcion}", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                Spacer(modifier = Modifier.height(16.dp))

                Text("Estado de Validación:", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(6.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    FilterChip(
                        selected = estadoSeleccionado == "VALIDADO_OK",
                        onClick = { estadoSeleccionado = "VALIDADO_OK" },
                        label = { Text("Validado OK", fontSize = 11.sp) }
                    )
                    FilterChip(
                        selected = estadoSeleccionado == "CORREGIDO_AGRONOMO",
                        onClick = { estadoSeleccionado = "CORREGIDO_AGRONOMO" },
                        label = { Text("Corregido", fontSize = 11.sp) }
                    )
                    FilterChip(
                        selected = estadoSeleccionado == "PENDIENTE",
                        onClick = { estadoSeleccionado = "PENDIENTE" },
                        label = { Text("Pendiente", fontSize = 11.sp) }
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = esNutricional, onCheckedChange = { esNutricional = it })
                    Text("Es deficiencia nutricional (NPK)", fontSize = 12.sp)
                }

                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = observacion,
                    onValueChange = { observacion = it },
                    label = { Text("Observación Técnica") },
                    placeholder = { Text("Escriba notas o acciones correctivas...") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )
            }
        },
        confirmButton = {
            Button(onClick = { alGuardar(estadoSeleccionado, observacion, esNutricional) }) {
                Text("Guardar Validación")
            }
        },
        dismissButton = {
            TextButton(onClick = alCancelar) { Text("Cancelar") }
        }
    )
}

@Composable
fun TarjetaZonaHistorial(
    nombreZona: String, 
    cantidadAnalisis: Int, 
    estaExpandido: Boolean, 
    onClick: () -> Unit
) {
    val colorTexto = MaterialTheme.colorScheme.onSurface
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 1.dp,
        modifier = Modifier
            .fillMaxWidth()
            .bounceClick()
            .clickable { onClick() }
    ) {
        Row(
            modifier = Modifier.padding(20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.LocationOn, 
                    contentDescription = null, 
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text(text = nombreZona, fontWeight = FontWeight.Bold, fontSize = 18.sp, color = colorTexto)
                    Text(text = "$cantidadAnalisis análisis realizados", fontSize = 13.sp, color = colorTexto.copy(alpha = 0.6f))
                }
            }
            Icon(
                imageVector = if (estaExpandido) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                contentDescription = null,
                tint = colorTexto.copy(alpha = 0.5f)
            )
        }
    }
}

@Composable
fun TarjetaHistorialCompacta(analisis: ResultadoAnalisis, alEditarValidacion: () -> Unit = {}) {
    val colorIndicador = Color(analisis.valorColor)
    val colorTexto = MaterialTheme.colorScheme.onSurface
    
    val (badgeTexto, badgeColor) = when(analisis.estadoValidacion) {
        "VALIDADO_OK" -> "Validado OK" to Color(0xFF2ECC71)
        "CORREGIDO_AGRONOMO" -> "Corregido Agrónomo" to Color(0xFFE67E22)
        else -> "Pendiente Validación" to Color(0xFFF1C40F)
    }

    Surface(
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.1f)),
        modifier = Modifier.fillMaxWidth().bounceClick().clickable { alEditarValidacion() }
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .background(colorIndicador, CircleShape)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = analisis.nombrePlanta, 
                    fontWeight = FontWeight.Bold, 
                    fontSize = 16.sp, 
                    color = colorTexto,
                    modifier = Modifier.weight(1f)
                )
                
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = badgeColor.copy(alpha = 0.15f)
                ) {
                    Text(
                        text = badgeTexto,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = badgeColor,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                ItemTecnicoHistorial(Icons.Default.DeviceThermostat, analisis.temperatura)
                ItemTecnicoHistorial(Icons.Default.WaterDrop, analisis.humedad)
                ItemTecnicoHistorial(Icons.Default.WbSunny, analisis.radiacionUV)
                ItemTecnicoHistorial(Icons.Default.CheckCircle, analisis.descripcion, colorIndicador)
            }

            if (analisis.observacionTecnico.isNotBlank() || analisis.esDeficienciaNutricional) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Nota Técnico: ${analisis.observacionTecnico}${if(analisis.esDeficienciaNutricional) " [Deficiencia Nutricional NPK]" else ""}",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

@Composable
fun ItemTecnicoHistorial(icono: androidx.compose.ui.graphics.vector.ImageVector, texto: String, colorIcono: Color = MaterialTheme.colorScheme.onSurfaceVariant) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icono, null, modifier = Modifier.size(14.dp), tint = colorIcono)
        Spacer(modifier = Modifier.width(4.dp))
        Text(text = texto, fontSize = 11.sp, color = colorIcono, fontWeight = if (colorIcono == MaterialTheme.colorScheme.onSurfaceVariant) FontWeight.Normal else FontWeight.Bold)
    }
}
