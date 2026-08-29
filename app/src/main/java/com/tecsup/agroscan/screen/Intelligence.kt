package com.tecsup.agroscan.screen

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.location.Location
import android.util.Base64
import android.util.Log
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.model.LatLng
import com.tecsup.agroscan.viewmodel.MainViewModel
import com.tecsup.agroscan.data.InformacionZona
import com.tecsup.agroscan.ui.utils.bounceClick
import kotlinx.coroutines.delay
import kotlinx.coroutines.tasks.await
import java.io.ByteArrayOutputStream
import kotlin.coroutines.resume
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.time.Duration.Companion.seconds

@SuppressLint("MissingPermission")
@Composable
fun PantallaIA(viewModel: MainViewModel) {
    val contexto = LocalContext.current
    val cicloVida = LocalLifecycleOwner.current
    val camaraProductor = remember { ProcessCameraProvider.getInstance(contexto) }
    val locationClient = remember { LocationServices.getFusedLocationProviderClient(contexto) }

    var analizando by remember { mutableStateOf(false) }
    var textoResultado by remember { mutableStateOf<String?>(null) }
    var modoFlash by remember { mutableIntStateOf(ImageCapture.FLASH_MODE_AUTO) }
    var mostrarConsejo by remember { mutableStateOf(true) }
    var camara by remember { mutableStateOf<Camera?>(null) }

    // Estados para el selector de zona
    var zonaSeleccionadaManual by remember { mutableStateOf<InformacionZona?>(null) }
    var modoAutomaticoGPS by remember { mutableStateOf(true) }
    var zonaDetectadaGPS by remember { mutableStateOf<InformacionZona?>(null) }
    var mostrarSelectorZonas by remember { mutableStateOf(false) }

    val capturaImagen = remember { ImageCapture.Builder().build() }

    val vistaPrevia = remember {
        PreviewView(contexto).apply {
            scaleType = PreviewView.ScaleType.FILL_CENTER
        }
    }

    // Lógica para detectar zona por GPS periódicamente
    LaunchedEffect(modoAutomaticoGPS, viewModel.permisoUbicacionConcedido) {
        if (modoAutomaticoGPS && viewModel.permisoUbicacionConcedido) {
            while (true) {
                try {
                    val location: Location? = locationClient.lastLocation.await()
                    if (location != null) {
                        zonaDetectadaGPS = viewModel.detectarZonaPorUbicacion(LatLng(location.latitude, location.longitude))
                    }
                } catch (e: Exception) {
                    Log.e("PantallaIA", "Error obteniendo ubicación", e)
                }
                delay(5.seconds) // Actualizar cada 5 segundos
            }
        }
    }

    // Inicializar cámara
    LaunchedEffect(viewModel.permisoCamaraConcedido) {
        if (viewModel.permisoCamaraConcedido) {
            try {
                val proveedor = suspendCancellableCoroutine<ProcessCameraProvider> { continuation ->
                    camaraProductor.addListener({
                        continuation.resume(camaraProductor.get())
                    }, ContextCompat.getMainExecutor(contexto))
                }

                val preview = Preview.Builder().build().also {
                    it.surfaceProvider = vistaPrevia.surfaceProvider
                }

                val selector = CameraSelector.DEFAULT_BACK_CAMERA
                
                proveedor.unbindAll()
                camara = proveedor.bindToLifecycle(
                    cicloVida,
                    selector,
                    preview,
                    capturaImagen
                )
            } catch (e: Exception) {
                Log.e("PantallaIA", "Error inicializando cámara", e)
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        if (viewModel.permisoCamaraConcedido) {
            AndroidView(
                factory = { vistaPrevia },
                modifier = Modifier.fillMaxSize()
            )
        } else {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.CameraAlt, null, tint = Color.Gray, modifier = Modifier.size(64.dp))
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Acceso a la cámara denegado", color = Color.White, fontWeight = FontWeight.Bold)
                    Text("Habilite los permisos en la configuración del dispositivo", color = Color.Gray, fontSize = 12.sp)
                }
            }
        }

        // --- INTERFAZ DE USUARIO (OVERLAY) ---
        if (viewModel.permisoCamaraConcedido) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Surface(
                    modifier = Modifier.size(280.dp),
                    color = Color.Transparent,
                    shape = RoundedCornerShape(32.dp),
                    border = androidx.compose.foundation.BorderStroke(2.dp, Color(0xFFC0E0A0).copy(alpha = 0.5f))
                ) {
                    if (analizando) {
                        Box(modifier = Modifier.fillMaxSize()) {
                            LinearProgressIndicator(
                                color = Color(0xFFC0E0A0),
                                modifier = Modifier.fillMaxWidth().align(Alignment.TopCenter)
                            )
                        }
                    }
                }
            }
        }

        Column(modifier = Modifier.fillMaxSize().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            if (viewModel.permisoCamaraConcedido) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Selector de Zona
                    Surface(
                        color = Color.Black.copy(alpha = 0.6f),
                        shape = RoundedCornerShape(20.dp),
                        modifier = Modifier
                            .bounceClick()
                            .clickable { mostrarSelectorZonas = true }
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                        ) {
                            Icon(
                                imageVector = if (modoAutomaticoGPS) Icons.Default.GpsFixed else Icons.Default.Map,
                                contentDescription = null,
                                tint = Color(0xFFC0E0A0),
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (modoAutomaticoGPS) {
                                    zonaDetectadaGPS?.nombre ?: "Buscando Zona..."
                                } else {
                                    zonaSeleccionadaManual?.nombre ?: "Seleccionar Zona"
                                },
                                color = Color.White,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Icon(Icons.Default.ArrowDropDown, null, tint = Color.White)
                        }

                        DropdownMenu(
                            expanded = mostrarSelectorZonas,
                            onDismissRequest = { mostrarSelectorZonas = false },
                            modifier = Modifier.background(Color.White)
                        ) {
                            DropdownMenuItem(
                                text = { Text("Automático (GPS)", color = if(modoAutomaticoGPS) Color(0xFF2E401F) else Color.Unspecified) },
                                leadingIcon = { Icon(Icons.Default.GpsFixed, null) },
                                onClick = {
                                    modoAutomaticoGPS = true
                                    mostrarSelectorZonas = false
                                }
                            )
                            HorizontalDivider()
                            viewModel.zonas.forEach { zona ->
                                DropdownMenuItem(
                                    text = { Text(zona.nombre) },
                                    leadingIcon = { Icon(Icons.Default.LocationOn, null, tint = zona.color) },
                                    onClick = {
                                        modoAutomaticoGPS = false
                                        zonaSeleccionadaManual = zona
                                        mostrarSelectorZonas = false
                                    }
                                )
                            }
                        }
                    }

                    Row {
                        IconButton(
                            onClick = { mostrarConsejo = !mostrarConsejo },
                            modifier = Modifier.background(Color.Black.copy(alpha = 0.5f), CircleShape).bounceClick()
                        ) {
                            Icon(Icons.Default.Info, "Información", tint = if (mostrarConsejo) Color(0xFFC0E0A0) else Color.White)
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        IconButton(
                            onClick = {
                                modoFlash = when(modoFlash) {
                                    ImageCapture.FLASH_MODE_AUTO -> ImageCapture.FLASH_MODE_ON
                                    ImageCapture.FLASH_MODE_ON -> ImageCapture.FLASH_MODE_OFF
                                    else -> ImageCapture.FLASH_MODE_AUTO
                                }
                            },
                            modifier = Modifier.background(Color.Black.copy(alpha = 0.5f), CircleShape).bounceClick()
                        ) {
                            val iconoFlash = when(modoFlash) {
                                ImageCapture.FLASH_MODE_AUTO -> Icons.Default.FlashAuto
                                ImageCapture.FLASH_MODE_ON -> Icons.Default.FlashOn
                                else -> Icons.Default.FlashOff
                            }
                            Icon(iconoFlash, "Flash", tint = if (modoFlash != ImageCapture.FLASH_MODE_OFF) Color.Yellow else Color.White)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                AnimatedVisibility(
                    visible = mostrarConsejo,
                    enter = fadeIn() + expandVertically(),
                    exit = fadeOut() + shrinkVertically()
                ) {
                    Surface(
                        color = Color.Black.copy(alpha = 0.6f),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.padding(horizontal = 8.dp)
                    ) {
                        Text(
                            text = "Para obtener mejores resultados, tome la fotografía en un lugar claro o con buena luz.",
                            color = Color.White,
                            fontSize = 13.sp,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(16.dp),
                            lineHeight = 18.sp
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.weight(1f))

            if (textoResultado != null) {
                PanelResultado(textoResultado!!, alCerrar = { textoResultado = null })
            }

            Spacer(modifier = Modifier.height(24.dp))

            if (viewModel.permisoCamaraConcedido) {
                val zonaParaAnalisis = if (modoAutomaticoGPS) zonaDetectadaGPS else zonaSeleccionadaManual
                
                BotonEscanear(
                    estaAnalizando = analizando,
                    habilitado = true,
                    alClickear = {
                        analizando = true
                        mostrarConsejo = false 
                        capturarYAnalizar(contexto, capturaImagen, viewModel, modoFlash, zonaParaAnalisis) { resultado ->
                            textoResultado = resultado
                            analizando = false
                        }
                    }
                )
            }
            
            Spacer(modifier = Modifier.height(100.dp))
        }
    }
}

private fun capturarYAnalizar(
    contexto: android.content.Context,
    captura: ImageCapture,
    viewModel: MainViewModel,
    modoFlash: Int,
    zona: InformacionZona?,
    alFinalizar: (String) -> Unit
) {
    captura.flashMode = modoFlash
    captura.takePicture(
        ContextCompat.getMainExecutor(contexto),
        object : ImageCapture.OnImageCapturedCallback() {
            override fun onCaptureSuccess(imagen: ImageProxy) {
                val bitmap = proxyABitmap(imagen)
                val base64 = bitmapABase64(bitmap)
                viewModel.analizarImagenPlanta(base64, zona) { resultado ->
                    alFinalizar(resultado)
                }
                imagen.close()
            }

            override fun onError(error: ImageCaptureException) {
                Log.e("PantallaIA", "Error captura", error)
                alFinalizar("Error al capturar imagen")
            }
        }
    )
}

private fun proxyABitmap(image: ImageProxy): Bitmap {
    val buffer = image.planes[0].buffer
    val bytes = ByteArray(buffer.remaining())
    buffer.get(bytes)
    return BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
}

private fun bitmapABase64(bitmap: Bitmap): String {
    val stream = ByteArrayOutputStream()
    bitmap.compress(Bitmap.CompressFormat.JPEG, 70, stream)
    return Base64.encodeToString(stream.toByteArray(), Base64.DEFAULT)
}

@Composable
fun PanelResultado(resultado: String, alCerrar: () -> Unit) {
    Surface(shape = RoundedCornerShape(28.dp), color = Color.White, modifier = Modifier.fillMaxWidth(), shadowElevation = 8.dp) {
        Column(modifier = Modifier.padding(24.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.AutoAwesome, null, tint = Color(0xFFC0E0A0))
                Spacer(modifier = Modifier.width(12.dp))
                Text("Detección IA", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = Color(0xFF2E401F))
                Spacer(modifier = Modifier.weight(1f))
                IconButton(onClick = alCerrar) { Icon(Icons.Default.Close, null) }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(resultado, color = Color.DarkGray)
        }
    }
}

@Composable
fun BotonEscanear(estaAnalizando: Boolean, habilitado: Boolean, alClickear: () -> Unit) {
    Button(
        onClick = alClickear,
        enabled = habilitado && !estaAnalizando,
        modifier = Modifier.size(80.dp).bounceClick(),
        shape = CircleShape,
        colors = ButtonDefaults.buttonColors(
            containerColor = if (habilitado) Color(0xFFC0E0A0) else Color.Gray.copy(alpha = 0.5f),
            contentColor = Color(0xFF2E401F)
        ),
        contentPadding = PaddingValues(0.dp)
    ) {
        if (estaAnalizando) CircularProgressIndicator(color = Color(0xFF2E401F))
        else Icon(Icons.Default.Camera, null, modifier = Modifier.size(36.dp))
    }
}
