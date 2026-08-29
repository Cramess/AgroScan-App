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
            color = Color(0xFF2E401F)
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
                                    TarjetaHistorialCompacta(analisis)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun TarjetaZonaHistorial(
    nombreZona: String, 
    cantidadAnalisis: Int, 
    estaExpandido: Boolean, 
    onClick: () -> Unit
) {
    val verdeOscuro = Color(0xFF2E401F)
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
                    Text(text = nombreZona, fontWeight = FontWeight.Bold, fontSize = 18.sp, color = verdeOscuro)
                    Text(text = "$cantidadAnalisis análisis realizados", fontSize = 13.sp, color = Color.Gray)
                }
            }
            Icon(
                imageVector = if (estaExpandido) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                contentDescription = null,
                tint = Color.Gray
            )
        }
    }
}

@Composable
fun TarjetaHistorialCompacta(analisis: ResultadoAnalisis) {
    val colorIndicador = Color(analisis.valorColor)
    val verdeOscuro = Color(0xFF2E401F)
    
    Surface(
        shape = RoundedCornerShape(24.dp),
        color = Color(0xFFF9FAFB),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE5E7EB)),
        modifier = Modifier.fillMaxWidth().bounceClick()
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
                    color = verdeOscuro,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = "${analisis.fecha} • ${analisis.hora}", 
                    fontSize = 11.sp, 
                    color = Color.Gray
                )
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
        }
    }
}

@Composable
fun ItemTecnicoHistorial(icono: androidx.compose.ui.graphics.vector.ImageVector, texto: String, colorIcono: Color = Color.Gray) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icono, null, modifier = Modifier.size(14.dp), tint = colorIcono)
        Spacer(modifier = Modifier.width(4.dp))
        Text(text = texto, fontSize = 11.sp, color = if (colorIcono == Color.Gray) Color.Gray else colorIcono, fontWeight = if (colorIcono == Color.Gray) FontWeight.Normal else FontWeight.Bold)
    }
}
