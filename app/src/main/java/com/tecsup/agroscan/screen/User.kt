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
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import com.tecsup.agroscan.viewmodel.MainViewModel
import com.tecsup.agroscan.ui.utils.bounceClick
import com.tecsup.agroscan.ui.utils.aparicion
import com.tecsup.agroscan.ui.utils.NumeroAnimado
import com.tecsup.agroscan.ui.utils.vidrioSoportado

@Composable
fun PantallaUsuario(viewModel: MainViewModel) {
    val usuario = viewModel.usuarioActual
    val estadoScroll = rememberScrollState()
    val verdeOscuro = MaterialTheme.colorScheme.onBackground

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

        Surface(
            shape = RoundedCornerShape(32.dp),
            color = MaterialTheme.colorScheme.surface,
            shadowElevation = 2.dp,
            modifier = Modifier.fillMaxWidth().aparicion(0).bounceClick()
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

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            TarjetaEstadistica("Zonas", viewModel.zonas.size, Modifier.weight(1f).aparicion(1))
            TarjetaEstadistica("Análisis", viewModel.historialAnalisis.size, Modifier.weight(1f).aparicion(2))
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "Cuenta y Seguridad",
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(start = 8.dp, bottom = 12.dp)
        )

        val context = LocalContext.current
        var dialogo by remember { mutableStateOf<DialogoPerfil?>(null) }

        Surface(
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column {
                OpcionPerfil(Icons.Outlined.Edit, "Editar Datos Personales") { dialogo = DialogoPerfil.DATOS }
                HorizontalDivider(modifier = Modifier.padding(horizontal = 20.dp), color = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))
                OpcionPerfil(Icons.Outlined.Lock, "Cambiar Contraseña") { dialogo = DialogoPerfil.CONTRASENA }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "Soporte",
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
            OpcionPerfil(Icons.Outlined.Email, "Contactar al administrador") { dialogo = DialogoPerfil.CONTACTO }
        }

        when (dialogo) {
            DialogoPerfil.DATOS -> DialogoEditarDatos(
                nombreInicial = usuario?.nombre.orEmpty(),
                correoInicial = usuario?.email.orEmpty(),
                alGuardar = { nombre, correo -> viewModel.actualizarDatos(nombre, correo) },
                alTerminar = { guardado ->
                    dialogo = null
                    if (guardado) Toast.makeText(context, "Datos actualizados", Toast.LENGTH_SHORT).show()
                }
            )
            DialogoPerfil.CONTRASENA -> DialogoCambiarContrasena(
                alGuardar = { actual, nueva, confirmacion -> viewModel.cambiarContrasena(actual, nueva, confirmacion) },
                alTerminar = { guardado ->
                    dialogo = null
                    if (guardado) Toast.makeText(context, "Contraseña actualizada", Toast.LENGTH_SHORT).show()
                }
            )
            DialogoPerfil.CONTACTO -> DialogoContactarAdministrador(
                correoInicial = viewModel.correoAdministrador(),
                alEnviar = { correoAdmin, tipo, mensaje ->
                    viewModel.guardarCorreoAdministrador(correoAdmin) ?: run {
                        val asunto = "AgroScan · $tipo de ${usuario?.nombre.orEmpty()}"
                        val cuerpo = "$mensaje\n\n--\n${usuario?.nombre.orEmpty()}\n${usuario?.email.orEmpty()}\n" +
                                "Técnico agrónomo · enviado desde AgroScan"
                        val uri = Uri.parse("mailto:${correoAdmin.trim()}?subject=${Uri.encode(asunto)}&body=${Uri.encode(cuerpo)}")
                        try {
                            context.startActivity(Intent(Intent.ACTION_SENDTO, uri))
                            dialogo = null
                            null
                        } catch (_: Exception) {
                            "No hay una aplicación de correo instalada"
                        }
                    }
                },
                alCancelar = { dialogo = null }
            )
            null -> Unit
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
                        colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = MaterialTheme.colorScheme.primary)
                    )
                }
                HorizontalDivider(modifier = Modifier.padding(horizontal = 20.dp), color = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))
                Row(
                    modifier = Modifier.fillMaxWidth().padding(20.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Outlined.AutoAwesome, null, tint = Color.Gray)
                    Spacer(modifier = Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Animaciones", color = verdeOscuro)
                        Text(
                            "Transiciones y efectos de la interfaz",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = viewModel.animacionesHabilitadas,
                        onCheckedChange = { viewModel.alternarAnimaciones(it) },
                        colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = MaterialTheme.colorScheme.primary)
                    )
                }
                HorizontalDivider(modifier = Modifier.padding(horizontal = 20.dp), color = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))
                Row(
                    modifier = Modifier.fillMaxWidth().padding(20.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Outlined.BlurOn, null, tint = Color.Gray)
                    Spacer(modifier = Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Efecto esmerilado", color = verdeOscuro)
                        Text(
                            if (vidrioSoportado) "Vidrio desenfocado en vivo en barras y botones"
                            else "Requiere Android 12 o superior",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = viewModel.vidrioHabilitado && vidrioSoportado,
                        enabled = vidrioSoportado,
                        onCheckedChange = { viewModel.alternarVidrio(it) },
                        colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = MaterialTheme.colorScheme.primary)
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

private enum class DialogoPerfil { DATOS, CONTRASENA, CONTACTO }

@Composable
private fun DialogoEditarDatos(
    nombreInicial: String,
    correoInicial: String,
    alGuardar: (String, String) -> String?,
    alTerminar: (Boolean) -> Unit
) {
    var nombre by remember { mutableStateOf(nombreInicial) }
    var correo by remember { mutableStateOf(correoInicial) }
    var error by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = { alTerminar(false) },
        title = { Text("Editar datos personales") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                CampoDialogo(nombre, { nombre = it; error = null }, "Nombre completo")
                CampoDialogo(correo, { correo = it; error = null }, "Correo corporativo", teclado = KeyboardType.Email)
                error?.let { Text(it, color = MaterialTheme.colorScheme.error, fontSize = 13.sp) }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                error = alGuardar(nombre, correo)
                if (error == null) alTerminar(true)
            }) { Text("Guardar") }
        },
        dismissButton = { TextButton(onClick = { alTerminar(false) }) { Text("Cancelar") } }
    )
}

@Composable
private fun DialogoCambiarContrasena(
    alGuardar: (String, String, String) -> String?,
    alTerminar: (Boolean) -> Unit
) {
    var actual by remember { mutableStateOf("") }
    var nueva by remember { mutableStateOf("") }
    var confirmacion by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = { alTerminar(false) },
        title = { Text("Cambiar contraseña") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                CampoDialogo(actual, { actual = it; error = null }, "Contraseña actual", teclado = KeyboardType.Password)
                CampoDialogo(nueva, { nueva = it; error = null }, "Nueva contraseña (mínimo 6)", teclado = KeyboardType.Password)
                CampoDialogo(confirmacion, { confirmacion = it; error = null }, "Confirmar nueva contraseña", teclado = KeyboardType.Password)
                error?.let { Text(it, color = MaterialTheme.colorScheme.error, fontSize = 13.sp) }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    error = alGuardar(actual, nueva, confirmacion)
                    if (error == null) alTerminar(true)
                },
                enabled = actual.isNotEmpty() && nueva.isNotEmpty() && confirmacion.isNotEmpty()
            ) { Text("Guardar") }
        },
        dismissButton = { TextButton(onClick = { alTerminar(false) }) { Text("Cancelar") } }
    )
}

@Composable
private fun DialogoContactarAdministrador(
    correoInicial: String,
    alEnviar: (String, String, String) -> String?,
    alCancelar: () -> Unit
) {
    var correo by remember { mutableStateOf(correoInicial) }
    var tipo by remember { mutableStateOf("Consulta") }
    var mensaje by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = alCancelar,
        title = { Text("Contactar al administrador") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    "Se abrirá su aplicación de correo con el mensaje listo para enviar al gerente o administrador de la empresa.",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                CampoDialogo(correo, { correo = it; error = null }, "Correo del administrador", teclado = KeyboardType.Email)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("Consulta", "Observación").forEach { opcion ->
                        FilterChip(selected = tipo == opcion, onClick = { tipo = opcion }, label = { Text(opcion) })
                    }
                }
                OutlinedTextField(
                    value = mensaje,
                    onValueChange = { mensaje = it; error = null },
                    label = { Text("Mensaje") },
                    minLines = 4,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                )
                error?.let { Text(it, color = MaterialTheme.colorScheme.error, fontSize = 13.sp) }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { error = alEnviar(correo, tipo, mensaje.trim()) },
                enabled = correo.isNotBlank() && mensaje.isNotBlank()
            ) { Text("Abrir correo") }
        },
        dismissButton = { TextButton(onClick = alCancelar) { Text("Cancelar") } }
    )
}

@Composable
private fun CampoDialogo(
    valor: String,
    alCambiar: (String) -> Unit,
    etiqueta: String,
    teclado: KeyboardType = KeyboardType.Text
) {
    OutlinedTextField(
        value = valor,
        onValueChange = alCambiar,
        label = { Text(etiqueta) },
        singleLine = true,
        shape = RoundedCornerShape(14.dp),
        visualTransformation = if (teclado == KeyboardType.Password) PasswordVisualTransformation() else VisualTransformation.None,
        keyboardOptions = KeyboardOptions(keyboardType = teclado),
        modifier = Modifier.fillMaxWidth()
    )
}

@Composable
fun TarjetaEstadistica(etiqueta: String, valor: Int, modifier: Modifier) {
    Surface(
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surface,
        modifier = modifier.bounceClick()
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(etiqueta, fontSize = 12.sp, color = Color.Gray)
            NumeroAnimado(valor, MaterialTheme.colorScheme.primary, 20.sp)
        }
    }
}

@Composable
fun OpcionPerfil(icono: ImageVector, titulo: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .bounceClick()
            .clickable { onClick() }
            .padding(20.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icono, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(modifier = Modifier.width(16.dp))
        Text(titulo, modifier = Modifier.weight(1f), fontSize = 15.sp, color = MaterialTheme.colorScheme.onSurface)
        Icon(Icons.Default.ChevronRight, null, tint = MaterialTheme.colorScheme.outline)
    }
}
