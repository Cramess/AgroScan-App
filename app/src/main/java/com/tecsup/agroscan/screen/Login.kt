package com.tecsup.agroscan.screen

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.Eco
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tecsup.agroscan.data.ValidacionCuenta
import com.tecsup.agroscan.ui.utils.bounceClick
import com.tecsup.agroscan.ui.utils.CurvaSuave
import com.tecsup.agroscan.ui.utils.LocalAnimaciones
import com.tecsup.agroscan.viewmodel.MainViewModel

private val VerdeOscuro = Color(0xFF2E401F)
private val VerdeClaro = Color(0xFFC0E0A0)

/**
 * Inicio de sesión con las cuentas guardadas en el teléfono, y registro de cuentas nuevas
 * de técnico agrónomo con correo corporativo.
 */
@Composable
fun PantallaLogin(viewModel: MainViewModel) {
    var modoRegistro by remember { mutableStateOf(false) }

    var nombre by remember { mutableStateOf("") }
    var correo by remember { mutableStateOf("") }
    var contrasena by remember { mutableStateOf("") }
    var confirmacion by remember { mutableStateOf("") }
    var contrasenaVisible by remember { mutableStateOf(false) }
    var mensajeError by remember { mutableStateOf<String?>(null) }

    // Estado para animación de entrada (si las animaciones están desactivadas, todo aparece de inmediato)
    val animaciones = LocalAnimaciones.current
    var visible by remember { mutableStateOf(!animaciones) }
    LaunchedEffect(Unit) { visible = true }
    val entradaCampo = if (animaciones) expandVertically(tween(350, easing = CurvaSuave)) + fadeIn(tween(250, 100)) else EnterTransition.None
    val salidaCampo = if (animaciones) shrinkVertically(tween(300, easing = CurvaSuave)) + fadeOut(tween(150)) else ExitTransition.None

    val camposCompletos = correo.isNotBlank() && contrasena.isNotBlank() &&
            (!modoRegistro || (nombre.isNotBlank() && confirmacion.isNotBlank()))
    val enviar = {
        mensajeError = if (modoRegistro) {
            viewModel.registrarCuenta(nombre, correo, contrasena, confirmacion)
        } else {
            viewModel.iniciarSesion(correo, contrasena)
        }
    }
    val cambiarModo = {
        modoRegistro = !modoRegistro
        mensajeError = null
        contrasena = ""
        confirmacion = ""
    }

    val avisoCorreo = if (modoRegistro && ValidacionCuenta.esCorreoValido(correo) &&
        !ValidacionCuenta.esCorreoCorporativo(correo)
    ) "Use el correo de su empresa, no uno personal" else null

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .imePadding()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Spacer(modifier = Modifier.height(if (modoRegistro) 16.dp else 48.dp))

        AnimatedVisibility(
            visible = visible,
            enter = fadeIn(tween(800)) + slideInVertically(tween(800)) { -40 }
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(
                    modifier = Modifier
                        .size(if (modoRegistro) 64.dp else 88.dp)
                        .background(VerdeClaro, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Eco, null, tint = VerdeOscuro, modifier = Modifier.size(if (modoRegistro) 36.dp else 48.dp))
                }
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = if (modoRegistro) "Crear cuenta" else "AgroScan",
                    fontSize = 34.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = if (modoRegistro) "Cuenta de técnico agrónomo con el correo de su empresa" else "Inicie sesión para continuar",
                    fontSize = 15.sp,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(modifier = Modifier.height(28.dp))

        AnimatedVisibility(
            visible = visible,
            enter = fadeIn(tween(800, 200)) + slideInVertically(tween(800, 200)) { 40 }
        ) {
            Column {
                AnimatedVisibility(visible = modoRegistro, enter = entradaCampo, exit = salidaCampo) {
                    Column {
                        CampoTexto(nombre, { nombre = it; mensajeError = null }, "Nombre completo", Icons.Default.Person)
                        Spacer(modifier = Modifier.height(14.dp))
                    }
                }

                CampoTexto(
                    valor = correo,
                    alCambiar = { correo = it; mensajeError = null },
                    etiqueta = if (modoRegistro) "Correo corporativo" else "Correo electrónico",
                    icono = if (modoRegistro) Icons.Default.Business else Icons.Default.Email,
                    tipoTeclado = KeyboardType.Email,
                    error = avisoCorreo,
                    ayuda = if (modoRegistro) "Ejemplo: nombre@suempresa.com.pe" else null
                )

                Spacer(modifier = Modifier.height(14.dp))

                CampoTexto(
                    valor = contrasena,
                    alCambiar = { contrasena = it; mensajeError = null },
                    etiqueta = "Contraseña",
                    icono = Icons.Default.Lock,
                    tipoTeclado = KeyboardType.Password,
                    oculto = !contrasenaVisible,
                    alternarVisible = { contrasenaVisible = !contrasenaVisible },
                    ayuda = if (modoRegistro) "Mínimo 6 caracteres" else null,
                    alTerminar = if (!modoRegistro && camposCompletos) enviar else null
                )

                AnimatedVisibility(visible = modoRegistro, enter = entradaCampo, exit = salidaCampo) {
                    Column {
                        Spacer(modifier = Modifier.height(14.dp))
                        CampoTexto(
                            valor = confirmacion,
                            alCambiar = { confirmacion = it; mensajeError = null },
                            etiqueta = "Confirmar contraseña",
                            icono = Icons.Default.Lock,
                            tipoTeclado = KeyboardType.Password,
                            oculto = !contrasenaVisible,
                            error = if (confirmacion.isNotEmpty() && confirmacion != contrasena) "Las contraseñas no coinciden" else null,
                            alTerminar = if (camposCompletos) enviar else null
                        )
                    }
                }
            }
        }

        mensajeError?.let {
            Spacer(modifier = Modifier.height(12.dp))
            Text(it, color = MaterialTheme.colorScheme.error, fontSize = 14.sp, textAlign = TextAlign.Center)
        }

        Spacer(modifier = Modifier.height(22.dp))

        AnimatedVisibility(
            visible = visible,
            enter = fadeIn(tween(800, 400)) + expandVertically(tween(800, 400))
        ) {
            Button(
                onClick = { enviar() },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .bounceClick(),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = VerdeOscuro, contentColor = Color.White),
                enabled = camposCompletos
            ) {
                Text(if (modoRegistro) "Crear cuenta" else "Ingresar", style = MaterialTheme.typography.titleMedium)
            }
        }

        if (!modoRegistro) {
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "La cuenta es necesaria",
                fontSize = 12.sp,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Spacer(modifier = Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = if (modoRegistro) "¿Ya tiene una cuenta?" else "¿No tiene una cuenta?",
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            TextButton(onClick = cambiarModo) {
                Text(
                    text = if (modoRegistro) "Iniciar sesión" else "Crear cuenta",
                    fontWeight = FontWeight.Bold,
                    color = VerdeOscuro
                )
            }
        }
    }
}

@Composable
private fun CampoTexto(
    valor: String,
    alCambiar: (String) -> Unit,
    etiqueta: String,
    icono: ImageVector,
    tipoTeclado: KeyboardType = KeyboardType.Text,
    oculto: Boolean = false,
    alternarVisible: (() -> Unit)? = null,
    error: String? = null,
    ayuda: String? = null,
    alTerminar: (() -> Unit)? = null
) {
    val esContrasena = tipoTeclado == KeyboardType.Password
    OutlinedTextField(
        value = valor,
        onValueChange = alCambiar,
        label = { Text(etiqueta) },
        leadingIcon = { Icon(icono, contentDescription = null) },
        trailingIcon = alternarVisible?.let { alternar ->
            {
                IconButton(onClick = alternar) {
                    Icon(
                        imageVector = if (oculto) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                        contentDescription = if (oculto) "Mostrar contraseña" else "Ocultar contraseña"
                    )
                }
            }
        },
        isError = error != null,
        supportingText = (error ?: ayuda)?.let { texto -> { Text(texto) } },
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        visualTransformation = if (esContrasena && oculto) PasswordVisualTransformation() else VisualTransformation.None,
        keyboardOptions = KeyboardOptions(
            keyboardType = tipoTeclado,
            imeAction = if (alTerminar != null) ImeAction.Done else ImeAction.Next
        ),
        keyboardActions = KeyboardActions(onDone = { alTerminar?.invoke() }),
        singleLine = true
    )
}
