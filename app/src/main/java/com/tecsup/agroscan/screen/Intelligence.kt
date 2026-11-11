package com.tecsup.agroscan.screen

import android.annotation.SuppressLint
import android.location.Location
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
import androidx.compose.ui.draw.clip
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
import com.tecsup.agroscan.viewmodel.ModoDeteccion
import com.tecsup.agroscan.viewmodel.ResultadoEscaneo
import com.tecsup.agroscan.data.DatosClima
import com.tecsup.agroscan.data.InformacionZona
import com.tecsup.agroscan.ui.utils.bounceClick
import com.tecsup.agroscan.ui.utils.CurvaSuave
import com.tecsup.agroscan.ui.utils.LocalAnimaciones
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.ui.graphics.Brush
import androidx.compose.foundation.border
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import com.tecsup.agroscan.ui.utils.PanelVidrio
import com.tecsup.agroscan.ui.utils.fuenteVidrio
import com.tecsup.agroscan.ui.utils.rememberFondoVidrio
import kotlinx.coroutines.delay
import kotlinx.coroutines.tasks.await
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
    var resultadoEscaneo by remember { mutableStateOf<ResultadoEscaneo?>(null) }
    var zonaAlCapturar by remember { mutableStateOf<InformacionZona?>(null) }
    var modoFlash by remember { mutableIntStateOf(ImageCapture.FLASH_MODE_AUTO) }
    var mostrarConsejo by remember { mutableStateOf(true) }
    var camara by remember { mutableStateOf<Camera?>(null) }

    var zonaSeleccionadaManual by remember { mutableStateOf<InformacionZona?>(null) }
    var modoAutomaticoGPS by remember { mutableStateOf(true) }
    var zonaDetectadaGPS by remember { mutableStateOf<InformacionZona?>(null) }
    var mostrarSelectorZonas by remember { mutableStateOf(false) }

    val capturaImagen = remember { ImageCapture.Builder().build() }

    val vistaPrevia = remember {
        PreviewView(contexto).apply {
            scaleType = PreviewView.ScaleType.FILL_CENTER
            // TextureView en lugar de SurfaceView: así la imagen de la cámara se puede desenfocar
            // en vivo detrás de los paneles de vidrio (un SurfaceView no se puede capturar)
            implementationMode = PreviewView.ImplementationMode.COMPATIBLE
        }
    }
    val casoPreview = remember { Preview.Builder().build() }
    val fondoCamara = rememberFondoVidrio()

    // Al salir de la pestaña se libera la cámara: antes seguía encendida (y gastando GPU y batería)
    // mientras se navegaba por el resto de la app, lo que restaba fluidez a todas las pantallas
    DisposableEffect(Unit) {
        onDispose {
            if (camaraProductor.isDone) {
                try {
                    camaraProductor.get().unbind(casoPreview, capturaImagen)
                } catch (e: Exception) {
                    Log.w("PantallaIA", "No se pudo liberar la cámara", e)
                }
            }
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
                delay(5.seconds)
            }
        }
    }

    LaunchedEffect(viewModel.permisoCamaraConcedido) {
        if (viewModel.permisoCamaraConcedido) {
            try {
                val proveedor = suspendCancellableCoroutine<ProcessCameraProvider> { continuation ->
                    camaraProductor.addListener({
                        continuation.resume(camaraProductor.get())
                    }, ContextCompat.getMainExecutor(contexto))
                }

                casoPreview.surfaceProvider = vistaPrevia.surfaceProvider

                val selector = CameraSelector.DEFAULT_BACK_CAMERA

                proveedor.unbindAll()
                camara = proveedor.bindToLifecycle(
                    cicloVida,
                    selector,
                    casoPreview,
                    capturaImagen
                )
            } catch (e: Exception) {
                Log.e("PantallaIA", "Error inicializando cámara", e)
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        if (viewModel.permisoCamaraConcedido) {
            AndroidView(
                factory = { vistaPrevia },
                modifier = Modifier.fillMaxSize().fuenteVidrio(fondoCamara)
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

        if (viewModel.permisoCamaraConcedido) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                MarcoEscaneo(analizando = analizando)
            }
        }

        Column(modifier = Modifier.fillMaxSize().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            if (viewModel.permisoCamaraConcedido) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    PanelVidrio(
                        fondo = fondoCamara,
                        forma = RoundedCornerShape(20.dp),
                        tinte = Color.Black,
                        opacidadTinte = 0.28f,
                        opacidadSinVidrio = 0.6f,
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
                            modifier = Modifier.background(MaterialTheme.colorScheme.surface)
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
                        PanelVidrio(
                            fondo = fondoCamara, forma = CircleShape, tinte = Color.Black,
                            opacidadTinte = 0.28f, opacidadSinVidrio = 0.5f, modifier = Modifier.bounceClick()
                        ) {
                            IconButton(onClick = { mostrarConsejo = !mostrarConsejo }) {
                                Icon(Icons.Default.Info, "Información", tint = if (mostrarConsejo) Color(0xFFC0E0A0) else Color.White)
                            }
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        PanelVidrio(
                            fondo = fondoCamara, forma = CircleShape, tinte = Color.Black,
                            opacidadTinte = 0.28f, opacidadSinVidrio = 0.5f, modifier = Modifier.bounceClick()
                        ) {
                            IconButton(
                                onClick = {
                                    modoFlash = when(modoFlash) {
                                        ImageCapture.FLASH_MODE_AUTO -> ImageCapture.FLASH_MODE_ON
                                        ImageCapture.FLASH_MODE_ON -> ImageCapture.FLASH_MODE_OFF
                                        else -> ImageCapture.FLASH_MODE_AUTO
                                    }
                                }
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
                }

                Spacer(modifier = Modifier.height(16.dp))

                AnimatedVisibility(
                    visible = mostrarConsejo,
                    enter = fadeIn() + expandVertically(),
                    exit = fadeOut() + shrinkVertically()
                ) {
                    PanelVidrio(
                        fondo = fondoCamara,
                        forma = RoundedCornerShape(16.dp),
                        tinte = Color.Black,
                        opacidadTinte = 0.3f,
                        opacidadSinVidrio = 0.6f,
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

            // Se recuerda el último resultado para que el panel pueda animar su salida
            var ultimoResultado by remember { mutableStateOf<ResultadoEscaneo?>(null) }
            resultadoEscaneo?.let { ultimoResultado = it }
            val animacionesPanel = LocalAnimaciones.current
            AnimatedVisibility(
                visible = resultadoEscaneo != null,
                enter = if (animacionesPanel) {
                    slideInVertically(tween(450, easing = CurvaSuave)) { it / 3 } + fadeIn(tween(300)) +
                            scaleIn(tween(450, easing = CurvaSuave), initialScale = 0.94f)
                } else EnterTransition.None,
                exit = if (animacionesPanel) {
                    slideOutVertically(tween(250)) { it / 4 } + fadeOut(tween(200))
                } else ExitTransition.None
            ) {
                ultimoResultado?.let { escaneo ->
                    PanelResultado(
                        escaneo = escaneo,
                        clima = viewModel.datosClimaReales,
                        zonas = viewModel.zonas,
                        zonaInicial = zonaAlCapturar,
                        alGuardar = { zona -> viewModel.guardarDiagnostico(escaneo, zona) },
                        alCerrar = { resultadoEscaneo = null }
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            if (viewModel.permisoCamaraConcedido) {
                val zonaParaAnalisis = if (modoAutomaticoGPS) zonaDetectadaGPS else zonaSeleccionadaManual

                // Modo de detección: automático (nube con respaldo local), solo nube o solo local
                PanelVidrio(
                    fondo = fondoCamara,
                    forma = RoundedCornerShape(20.dp),
                    tinte = Color.Black,
                    opacidadTinte = 0.28f,
                    opacidadSinVidrio = 0.6f,
                    modifier = Modifier
                        .bounceClick()
                        .clickable(enabled = !analizando) { viewModel.alternarModoDeteccion() }
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                    ) {
                        val iconoModo = when (viewModel.modoDeteccion) {
                            ModoDeteccion.AUTOMATICO -> Icons.Default.Autorenew
                            ModoDeteccion.NUBE -> Icons.Default.Cloud
                            ModoDeteccion.LOCAL -> Icons.Default.PhoneAndroid
                        }
                        Icon(iconoModo, null, tint = Color(0xFFC0E0A0), modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Modelo: ${viewModel.modoDeteccion.etiqueta}",
                            color = Color.White,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Icon(Icons.Default.SwapHoriz, null, tint = Color.White, modifier = Modifier.padding(start = 6.dp).size(18.dp))
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                BotonEscanear(
                    estaAnalizando = analizando,
                    habilitado = true,
                    alClickear = {
                        analizando = true
                        mostrarConsejo = false 
                        resultadoEscaneo = null
                        zonaAlCapturar = zonaParaAnalisis
                        capturarYAnalizar(contexto, capturaImagen, viewModel, modoFlash) { resultado ->
                            resultadoEscaneo = resultado
                            analizando = false
                        }
                    }
                )
            }
            
            Spacer(modifier = Modifier.height(100.dp))
        }
    }
}

/**
 * Recuadro de encuadre: el borde "respira" para invitar a encuadrar la hoja y una línea lo barre al analizar.
 * Los valores animados se leen solo al dibujar (drawBehind / graphicsLayer): antes se leían al componer y
 * toda la pantalla de escaneo se recomponía unas 60 veces por segundo.
 */
@Composable
private fun MarcoEscaneo(analizando: Boolean) {
    val animaciones = LocalAnimaciones.current
    val verdeClaro = Color(0xFFC0E0A0)
    val forma = RoundedCornerShape(32.dp)

    Box(modifier = Modifier.size(280.dp)) {
        if (animaciones) {
            val transicion = rememberInfiniteTransition(label = "escaneo")
            val respiracion = transicion.animateFloat(
                0.35f, 0.8f, infiniteRepeatable(tween(1800, easing = CurvaSuave), RepeatMode.Reverse), label = "respiracion"
            )
            val barrido = transicion.animateFloat(
                0f, 1f, infiniteRepeatable(tween(1300, easing = CurvaSuave), RepeatMode.Reverse), label = "barrido"
            )
            Box(
                Modifier
                    .matchParentSize()
                    .drawBehind {
                        val grosor = 2.dp.toPx()
                        drawRoundRect(
                            color = verdeClaro.copy(alpha = if (analizando) 0.6f else respiracion.value),
                            topLeft = Offset(grosor / 2, grosor / 2),
                            size = Size(size.width - grosor, size.height - grosor),
                            cornerRadius = CornerRadius(32.dp.toPx() - grosor / 2),
                            style = Stroke(grosor)
                        )
                    }
            )
            if (analizando) {
                Box(Modifier.matchParentSize().clip(forma)) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp)
                            .graphicsLayer { translationY = 224.dp.toPx() * barrido.value }
                            .background(
                                Brush.verticalGradient(
                                    listOf(Color.Transparent, verdeClaro.copy(alpha = 0.25f), verdeClaro.copy(alpha = 0.9f))
                                )
                            )
                    )
                }
            }
        } else {
            Box(Modifier.matchParentSize().border(2.dp, verdeClaro.copy(alpha = 0.6f), forma))
            if (analizando) {
                LinearProgressIndicator(
                    color = verdeClaro,
                    modifier = Modifier.fillMaxWidth().align(Alignment.TopCenter).padding(horizontal = 24.dp, vertical = 2.dp)
                )
            }
        }
    }
}

private fun capturarYAnalizar(
    contexto: android.content.Context,
    captura: ImageCapture,
    viewModel: MainViewModel,
    modoFlash: Int,
    alFinalizar: (ResultadoEscaneo) -> Unit
) {
    captura.flashMode = modoFlash
    captura.takePicture(
        ContextCompat.getMainExecutor(contexto),
        object : ImageCapture.OnImageCapturedCallback() {
            override fun onCaptureSuccess(imagen: ImageProxy) {
                // Solo se copian los bytes aquí; la decodificación y rotación se hacen en segundo plano
                val jpeg = proxyABytes(imagen)
                val rotacion = imagen.imageInfo.rotationDegrees
                imagen.close()
                viewModel.analizarImagenPlanta(jpeg, rotacion) { resultado ->
                    alFinalizar(resultado)
                }
            }

            override fun onError(error: ImageCaptureException) {
                Log.e("PantallaIA", "Error captura", error)
                alFinalizar(ResultadoEscaneo.mensaje("Error al capturar imagen"))
            }
        }
    )
}

private fun proxyABytes(image: ImageProxy): ByteArray {
    val buffer = image.planes[0].buffer
    val bytes = ByteArray(buffer.remaining())
    buffer.get(bytes)
    return bytes
}

@Composable
fun PanelResultado(
    escaneo: ResultadoEscaneo,
    clima: DatosClima?,
    zonas: List<InformacionZona>,
    zonaInicial: InformacionZona?,
    alGuardar: (InformacionZona?) -> Unit,
    alCerrar: () -> Unit
) {
    // Si solo hay un campo registrado, se propone ese aunque el GPS no lo haya detectado
    var zonaElegida by remember(escaneo) { mutableStateOf(zonaInicial ?: zonas.singleOrNull()) }
    var guardadoEn by remember(escaneo) { mutableStateOf<String?>(null) }
    var mostrarZonas by remember { mutableStateOf(false) }

    Surface(shape = RoundedCornerShape(28.dp), color = MaterialTheme.colorScheme.surface, modifier = Modifier.fillMaxWidth(), shadowElevation = 8.dp) {
        Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.AutoAwesome, null, tint = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.width(12.dp))
                Text("Detección IA", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = MaterialTheme.colorScheme.onSurface)
                Spacer(modifier = Modifier.weight(1f))
                IconButton(onClick = alCerrar) { Icon(Icons.Default.Close, null) }
            }

            val enfermedad = escaneo.enfermedad
            if (enfermedad == null) {
                Text(escaneo.mensaje.orEmpty(), color = MaterialTheme.colorScheme.onSurfaceVariant)
                return@Column
            }

            val colorEstado = if (enfermedad.sana) Color(0xFF2ECC71) else Color(0xFFE67E22)

            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.size(10.dp).background(colorEstado, CircleShape))
                Spacer(modifier = Modifier.width(10.dp))
                Text(enfermedad.nombre, fontWeight = FontWeight.Bold, fontSize = 17.sp, color = MaterialTheme.colorScheme.onSurface)
            }
            Spacer(modifier = Modifier.height(10.dp))
            Row {
                Text("Confianza", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(modifier = Modifier.weight(1f))
                Text("${escaneo.porcentaje}%", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
            }
            Spacer(modifier = Modifier.height(4.dp))
            LinearProgressIndicator(
                progress = { escaneo.porcentaje / 100f },
                color = colorEstado,
                trackColor = colorEstado.copy(alpha = 0.2f),
                modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp))
            )

            // Condiciones del momento, que se guardan junto con el diagnóstico
            Spacer(modifier = Modifier.height(14.dp))
            if (clima != null) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    DatoClima(Icons.Default.Thermostat, clima.temperatura, Modifier.weight(1f))
                    DatoClima(Icons.Default.WaterDrop, clima.humedad, Modifier.weight(1f))
                    DatoClima(Icons.Default.Air, clima.viento, Modifier.weight(1f))
                }
            } else {
                Text("Clima no disponible: active la ubicación", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }

            Spacer(modifier = Modifier.height(14.dp))
            val guardado = guardadoEn
            if (guardado == null) {
                Box {
                    OutlinedButton(
                        onClick = { mostrarZonas = true },
                        enabled = zonas.isNotEmpty(),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.LocationOn, null, tint = zonaElegida?.color ?: MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = zonaElegida?.nombre ?: "Escaneo rápido (sin campo)",
                            maxLines = 1,
                            modifier = Modifier.weight(1f)
                        )
                        Icon(Icons.Default.ArrowDropDown, null)
                    }
                    DropdownMenu(expanded = mostrarZonas, onDismissRequest = { mostrarZonas = false }) {
                        zonas.forEach { zona ->
                            DropdownMenuItem(
                                text = { Text(zona.nombre) },
                                leadingIcon = { Icon(Icons.Default.LocationOn, null, tint = zona.color) },
                                onClick = {
                                    zonaElegida = zona
                                    mostrarZonas = false
                                }
                            )
                        }
                        HorizontalDivider()
                        DropdownMenuItem(
                            text = { Text("Escaneo rápido (sin campo)") },
                            onClick = {
                                zonaElegida = null
                                mostrarZonas = false
                            }
                        )
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = {
                        alGuardar(zonaElegida)
                        guardadoEn = zonaElegida?.nombre ?: "Escaneo rápido"
                    },
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E401F), contentColor = Color.White),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Save, null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Guardar en historial", fontWeight = FontWeight.SemiBold)
                }
            } else {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.CheckCircle, null, tint = Color(0xFF2ECC71), modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Guardado en $guardado", fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
                }
            }
        }
    }
}

@Composable
private fun DatoClima(icono: androidx.compose.ui.graphics.vector.ImageVector, valor: String, modifier: Modifier = Modifier) {
    Surface(shape = RoundedCornerShape(12.dp), color = MaterialTheme.colorScheme.surfaceVariant, modifier = modifier) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier.padding(vertical = 8.dp, horizontal = 6.dp)
        ) {
            Icon(icono, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text(valor, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
