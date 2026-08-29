package com.tecsup.agroscan.screen

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tecsup.agroscan.viewmodel.MainViewModel
import com.tecsup.agroscan.ui.utils.bounceClick

@Composable
fun PantallaUsuario(viewModel: MainViewModel) {
    val usuario = viewModel.usuarioActual
    val estadoScroll = rememberScrollState()
    val verdeOscuro = Color(0xFF2E401F)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(estadoScroll)
            .padding(horizontal = 24.dp)
    ) {
        Spacer(modifier = Modifier.height(48.dp))

        Text(
            text = "Mi Perfil",
            fontSize = 32.sp,
            fontWeight = FontWeight.Bold,
            color = verdeOscuro
        )

        Spacer(modifier = Modifier.height(32.dp))

        // --- TARJETA DE PERFIL ---
        Surface(
            shape = RoundedCornerShape(32.dp),
            color = MaterialTheme.colorScheme.surface,
            shadowElevation = 2.dp,
            modifier = Modifier.fillMaxWidth().bounceClick()
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                    modifier = Modifier.size(100.dp)
                ) {
                    Icon(
                        Icons.Default.Person,
                        null,
                        modifier = Modifier.padding(20.dp).fillMaxSize(),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(usuario?.nombre ?: "Usuario", fontSize = 22.sp, fontWeight = FontWeight.ExtraBold, color = verdeOscuro)
                Text(usuario?.email ?: "", fontSize = 14.sp, color = Color.Gray)

                Spacer(modifier = Modifier.height(12.dp))

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                ) {
                    Text(
                        text = usuario?.rol ?: "OPERADOR",
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        color = verdeOscuro,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // --- ESTADÍSTICAS ---
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            TarjetaEstadistica("Zonas", viewModel.zonas.size.toString(), Modifier.weight(1f))
            TarjetaEstadistica("Análisis", viewModel.historialAnalisis.size.toString(), Modifier.weight(1f))
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "Cuenta y Seguridad",
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(start = 8.dp, bottom = 12.dp)
        )

        Surface(
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column {
                OpcionPerfil(Icons.Outlined.Edit, "Editar Datos Personales")
                HorizontalDivider(modifier = Modifier.padding(horizontal = 20.dp), color = Color(0xFFF2F4F7))
                OpcionPerfil(Icons.Outlined.Lock, "Cambiar Contraseña")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Surface(
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(20.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Outlined.DarkMode, null, tint = Color.Gray)
                    Spacer(modifier = Modifier.width(16.dp))
                    Text("Modo Oscuro", modifier = Modifier.weight(1f), color = verdeOscuro)
                    Switch(
                        checked = viewModel.modoOscuroHabilitado,
                        onCheckedChange = { viewModel.alternarTema(it) },
                        colors = SwitchDefaults.colors(checkedThumbColor = MaterialTheme.colorScheme.primary)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = { viewModel.cerrarSesion() },
            modifier = Modifier.fillMaxWidth().height(56.dp).bounceClick(),
            shape = RoundedCornerShape(28.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFEBEE))
        ) {
            Icon(Icons.AutoMirrored.Filled.Logout, null, tint = Color(0xFFE57373))
            Spacer(modifier = Modifier.width(12.dp))
            Text("Cerrar Sesión", color = Color(0xFFE57373), fontWeight = FontWeight.Bold)
        }

        Spacer(modifier = Modifier.height(140.dp))
    }
}

@Composable
fun TarjetaEstadistica(etiqueta: String, valor: String, modifier: Modifier) {
    Surface(
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surface,
        modifier = modifier.bounceClick()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(etiqueta, fontSize = 12.sp, color = Color.Gray)
            Text(valor, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
        }
    }
}

@Composable
fun OpcionPerfil(icono: androidx.compose.ui.graphics.vector.ImageVector, titulo: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .bounceClick()
            .clickable { /* Acción futura */ }
            .padding(20.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icono, null, tint = Color.Gray)
        Spacer(modifier = Modifier.width(16.dp))
        Text(titulo, modifier = Modifier.weight(1f), fontSize = 15.sp, color = Color(0xFF2E401F))
        Icon(Icons.Default.ChevronRight, null, tint = Color.LightGray)
    }
}
