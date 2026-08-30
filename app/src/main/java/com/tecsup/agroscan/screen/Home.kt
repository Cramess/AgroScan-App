package com.tecsup.agroscan.screen

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tecsup.agroscan.viewmodel.MainViewModel
import com.tecsup.agroscan.data.InformacionZona
import com.tecsup.agroscan.data.DatosClima
import com.google.android.gms.maps.model.LatLng
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.MapEventsOverlay
import org.osmdroid.events.MapEventsReceiver
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polygon
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.core.content.ContextCompat
import android.annotation.SuppressLint
import android.app.Application
import com.tecsup.agroscan.ui.theme.AgroScanTheme
import com.tecsup.agroscan.ui.utils.bounceClick
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PantallaPanelControl(
    viewModel: MainViewModel,
    alSolicitarUbicacion: () -> Unit = {},
    alSolicitarCamara: () -> Unit = {}
) {
    var itemSeleccionado by remember { mutableIntStateOf(0) }
    var zonaSeleccionada by remember { mutableStateOf<InformacionZona?>(null) }
    var mostrarHojaDetalle by remember { mutableStateOf(false) }
    var mostrarHojaAgregar by remember { mutableStateOf(false) }
    var mostrarHojaEditar by remember { mutableStateOf(false) }
    val estadoHojaDetalle = rememberModalBottomSheetState()
    val estadoHojaAgregar = rememberModalBottomSheetState()
    val estadoHojaEditar = rememberModalBottomSheetState()

    // Estados para persistir datos del formulario durante el dibujo
    var nombreEnProceso by remember { mutableStateOf("") }
    var cultivoEnProceso by remember { mutableStateOf("") }
    var modoDibujoActivo by remember { mutableStateOf(false) }
    var modoEdicionPoligono by remember { mutableStateOf(false) }

    // Estados para eliminación segura
    var mostrarDialogoEliminar by remember { mutableStateOf(false) }
    var zonaParaEliminar by remember { mutableStateOf<InformacionZona?>(null) }

    val colorFondo = MaterialTheme.colorScheme.background
    val colorTextoNav = MaterialTheme.colorScheme.primary

    Box(modifier = Modifier.fillMaxSize().background(colorFondo)) {
        Column(modifier = Modifier.fillMaxSize()) {
            Box(modifier = Modifier.weight(1f)) {
                AnimatedContent(
                    targetState = itemSeleccionado,
                    transitionSpec = {
                        if (targetState > initialState) {
                            slideInHorizontally(animationSpec = tween(300)) { it } + fadeIn() togetherWith
                                    slideOutHorizontally(animationSpec = tween(300)) { -it } + fadeOut()
                        } else {
                            slideInHorizontally(animationSpec = tween(300)) { -it } + fadeIn() togetherWith
                                    slideOutHorizontally(animationSpec = tween(300)) { it } + fadeOut()
                        }.using(SizeTransform(clip = false))
                    },
                    label = "navTransition"
                ) { targetIndex ->
                    when (targetIndex) {
                        0 -> ContenidoPrincipalPanelControl(
                            zonas = viewModel.zonas,
                            datosClimaReales = viewModel.datosClimaReales,
                            alHacerClicEnZona = {
                                zonaSeleccionada = it
                                mostrarHojaDetalle = true
                            },
                            alHacerClicEnAgregar = { mostrarHojaAgregar = true },
                            alEditarZona = {
                                zonaSeleccionada = it
                                nombreEnProceso = it.nombre
                                cultivoEnProceso = it.cultivo
                                viewModel.puntosEdicion.clear()
                                viewModel.puntosEdicion.addAll(it.vertices)
                                modoEdicionPoligono = true
                                mostrarHojaEditar = true
                            },
                            alEliminarZona = {
                                zonaParaEliminar = it
                                mostrarDialogoEliminar = true
                            }
                        )
                        1 -> PantallaIA(viewModel = viewModel)
                        2 -> PantallaHistorial(viewModel = viewModel)
                        3 -> PantallaUsuario(viewModel = viewModel)
                    }
                }
            }
        }

        // --- BARRA DE NAVEGACIÓN ---
        Surface(
            modifier = Modifier.align(Alignment.BottomCenter).padding(horizontal = 20.dp).navigationBarsPadding().padding(bottom = 12.dp).fillMaxWidth().height(72.dp),
            shape = RoundedCornerShape(36.dp),
            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.98f),
            shadowElevation = 10.dp
        ) {
            Row(modifier = Modifier.fillMaxSize(), horizontalArrangement = Arrangement.SpaceAround, verticalAlignment = Alignment.CenterVertically) {
                val itemsNav = listOf("Inicio", "IA", "Historial", "Perfil")
                val iconos = listOf(Icons.Default.Home, Icons.Default.AutoAwesome, Icons.Default.History, Icons.Default.Person)
                itemsNav.forEachIndexed { index, item ->
                    val estaSeleccionado = itemSeleccionado == index
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .bounceClick()
                            .clickable { itemSeleccionado = index },
                        horizontalAlignment = Alignment.CenterHorizontally, 
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(imageVector = iconos[index], contentDescription = item, tint = if (estaSeleccionado) colorTextoNav else Color(0xFF8E9196), modifier = Modifier.size(if (estaSeleccionado) 28.dp else 24.dp))
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(text = item, fontSize = 11.sp, fontWeight = if (estaSeleccionado) FontWeight.Bold else FontWeight.Medium, color = if (estaSeleccionado) colorTextoNav else Color(0xFF8E9196))
                    }
                }
            }
        }

        if (mostrarHojaDetalle && zonaSeleccionada != null) {
            ModalBottomSheet(
                onDismissRequest = { mostrarHojaDetalle = false },
                sheetState = estadoHojaDetalle,
                shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp)
            ) {
                ContenidoDetalleZona(zonaSeleccionada!!)
            }
        }

        if (mostrarHojaAgregar) {
            ModalBottomSheet(
                onDismissRequest = { mostrarHojaAgregar = false },
                sheetState = estadoHojaAgregar,
                shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp)
            ) {
                FormularioAgregarZona(
                    viewModel = viewModel,
                    nombreInicial = nombreEnProceso,
                    cultivoInicial = cultivoEnProceso,
                    alCambiarDatos = { n, c ->
                        nombreEnProceso = n
                        cultivoEnProceso = c
                    },
                    alDibujarEnMapa = {
                        mostrarHojaAgregar = false
                        modoDibujoActivo = true
                    },
                    alAgregar = { nuevaZona ->
                        viewModel.agregarZona(nuevaZona)
                        nombreEnProceso = ""
                        cultivoEnProceso = ""
                        mostrarHojaAgregar = false
                    }
                )
            }
        }

        if (mostrarHojaEditar && zonaSeleccionada != null) {
            ModalBottomSheet(
                onDismissRequest = { 
                    mostrarHojaEditar = false 
                    modoEdicionPoligono = false
                },
                sheetState = estadoHojaEditar,
                shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp)
            ) {
                FormularioEditarZona(
                    viewModel = viewModel,
                    zona = zonaSeleccionada!!,
                    nombreActual = nombreEnProceso,
                    cultivoActual = cultivoEnProceso,
                    alCambiarDatos = { n, c ->
                        nombreEnProceso = n
                        cultivoEnProceso = c
                    },
                    alDibujarEnMapa = {
                        mostrarHojaEditar = false
                        modoDibujoActivo = true
                    },
                    alActualizar = { zonaActualizada ->
                        viewModel.actualizarZona(zonaSeleccionada!!, zonaActualizada)
                        nombreEnProceso = ""
                        cultivoEnProceso = ""
                        modoEdicionPoligono = false
                        mostrarHojaEditar = false
                    }
                )
            }
        }

        // --- DIÁLOGO DE ELIMINACIÓN SEGURA ---
        if (mostrarDialogoEliminar && zonaParaEliminar != null) {
            DialogoConfirmacionEliminar(
                nombreZona = zonaParaEliminar!!.nombre,
                alConfirmar = {
                    viewModel.eliminarZona(zonaParaEliminar!!)
                    mostrarDialogoEliminar = false
                    zonaParaEliminar = null
                },
                alCancelar = {
                    mostrarDialogoEliminar = false
                    zonaParaEliminar = null
                }
            )
        }

        // --- PANTALLA DE DIBUJO ---
        if (modoDibujoActivo) {
            PantallaDibujoParcela(
                viewModel = viewModel,
                alTerminar = { 
                    modoDibujoActivo = false
                    if (modoEdicionPoligono) mostrarHojaEditar = true else mostrarHojaAgregar = true
                },
                alCancelar = { 
                    if (!modoEdicionPoligono) viewModel.puntosEdicion.clear()
                    modoDibujoActivo = false 
                    if (modoEdicionPoligono) mostrarHojaEditar = true else mostrarHojaAgregar = true
                }
            )
        }
    }
}

@Composable
fun PantallaDibujoParcela(
    viewModel: MainViewModel,
    alTerminar: () -> Unit,
    alCancelar: () -> Unit
) {
    val contexto = LocalContext.current
    val hectareasCalculadas = viewModel.calcularHectareas(viewModel.puntosEdicion)
    val locale = LocalConfiguration.current.locales[0]

    // Configuración necesaria para OSM
    Configuration.getInstance().userAgentValue = contexto.packageName

    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        AndroidView(
            factory = { ctx ->
                MapView(ctx).apply {
                    setTileSource(TileSourceFactory.MAPNIK)
                    setMultiTouchControls(true)
                    controller.setZoom(16.0)
                    controller.setCenter(GeoPoint(-12.046374, -77.042793))

                    val overlayEventos = MapEventsOverlay(object : MapEventsReceiver {
                        override fun singleTapConfirmedHelper(p: GeoPoint): Boolean {
                            viewModel.puntosEdicion.add(LatLng(p.latitude, p.longitude))
                            return true
                        }
                        override fun longPressHelper(p: GeoPoint): Boolean = false
                    })
                    overlays.add(overlayEventos)
                }
            },
            update = { mapView ->
                mapView.overlays.removeAll { it is Marker || it is Polygon }

                if (viewModel.puntosEdicion.isNotEmpty()) {
                    val puntosGeo = viewModel.puntosEdicion.map { GeoPoint(it.latitude, it.longitude) }

                    if (puntosGeo.size >= 3) {
                        val poligono = Polygon(mapView)
                        poligono.points = puntosGeo
                        poligono.fillPaint.color = android.graphics.Color.argb(80, 192, 224, 160)
                        poligono.outlinePaint.color = android.graphics.Color.rgb(46, 64, 31)
                        poligono.outlinePaint.strokeWidth = 3f
                        mapView.overlays.add(poligono)
                    }

                    viewModel.puntosEdicion.forEach { latLng ->
                        val marcador = Marker(mapView)
                        marcador.position = GeoPoint(latLng.latitude, latLng.longitude)
                        marcador.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
                        marcador.icon = ContextCompat.getDrawable(contexto, org.osmdroid.library.R.drawable.marker_default)
                        mapView.overlays.add(marcador)
                    }
                }
                mapView.invalidate()
            },
            modifier = Modifier.fillMaxSize()
        )

        // UI de Control de Dibujo
        Column(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 60.dp)
                .padding(horizontal = 24.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Dibujando Parcela", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                    Text("Toca el mapa para añadir los límites", fontSize = 12.sp, color = Color.Gray)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = String.format(locale, "%.2f ha", hectareasCalculadas),
                        fontSize = 24.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }

        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 40.dp)
                .padding(horizontal = 24.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Button(
                onClick = alCancelar,
                modifier = Modifier.weight(1f).height(56.dp),
                shape = RoundedCornerShape(28.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surface, contentColor = Color.Gray)
            ) {
                Text("Cancelar")
            }

            Button(
                onClick = alTerminar,
                enabled = viewModel.puntosEdicion.size >= 3,
                modifier = Modifier.weight(1f).height(56.dp).bounceClick(),
                shape = RoundedCornerShape(28.dp)
            ) {
                Text("Terminar", fontWeight = FontWeight.Bold)
            }
        }

        if (viewModel.puntosEdicion.isNotEmpty()) {
            FloatingActionButton(
                onClick = { viewModel.puntosEdicion.removeAt(viewModel.puntosEdicion.lastIndex) },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(bottom = 120.dp, end = 24.dp),
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.primary,
                shape = CircleShape
            ) {
                Icon(Icons.AutoMirrored.Filled.Undo, "Deshacer")
            }
        }
    }
}


@Composable
fun DialogoConfirmacionEliminar(
    nombreZona: String,
    alConfirmar: () -> Unit,
    alCancelar: () -> Unit
) {
    var textoConfirmacion by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = alCancelar,
        shape = RoundedCornerShape(28.dp),
        containerColor = MaterialTheme.colorScheme.surface,
        title = {
            Text(
                text = "Confirmar Eliminación",
                fontWeight = FontWeight.Bold,
                color = Color(0xFFC81E1E)
            )
        },
        text = {
            Column {
                Text(
                    text = "¿Estás seguro de que deseas eliminar la zona \"$nombreZona\"?",
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "Esta acción no se puede deshacer. Todos los datos y el historial se perderán permanentemente.",
                    fontSize = 13.sp,
                    color = Color.Gray
                )
                Spacer(modifier = Modifier.height(20.dp))
                Text(
                    text = "Escribe \"ELIMINAR\" para continuar:",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = textoConfirmacion,
                    onValueChange = { textoConfirmacion = it },
                    placeholder = { Text("Escribe aquí...") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFFC81E1E),
                        unfocusedBorderColor = Color.LightGray
                    )
                )
            }
        },
        confirmButton = {
            Button(
                onClick = alConfirmar,
                enabled = textoConfirmacion == "ELIMINAR",
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFC81E1E),
                    disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.bounceClick()
            ) {
                Text("Eliminar", color = Color.White, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = alCancelar) {
                Text("Cancelar", color = Color.Gray)
            }
        }
    )
}

@Composable
fun ContenidoPrincipalPanelControl(
    zonas: List<InformacionZona>,
    datosClimaReales: DatosClima? = null,
    alHacerClicEnZona: (InformacionZona) -> Unit,
    alHacerClicEnAgregar: () -> Unit,
    alEditarZona: (InformacionZona) -> Unit,
    alEliminarZona: (InformacionZona) -> Unit
) {
    val totalHectareas = zonas.sumOf { it.hectareas }
    val proximaZonaCosecha = zonas.filter { it.diasParaCosecha <= 30 }.minByOrNull { it.diasParaCosecha }
    val colorTexto = MaterialTheme.colorScheme.onBackground
    val locale = LocalConfiguration.current.locales[0]

    Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 24.dp)) {
        Spacer(modifier = Modifier.height(32.dp))
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = "Monitoreo de Cultivos", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = colorTexto)
                Text(
                    text = "${String.format(locale, "%.2f", totalHectareas)} hectáreas en monitoreo activo", 
                    fontSize = 16.sp, 
                    color = colorTexto.copy(alpha = 0.7f)
                )
            }
            
            if (datosClimaReales != null) {
                ClimaHeaderCompacto(datosClimaReales)
            } else {
                ClimaHeaderCargando()
            }
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        proximaZonaCosecha?.let { zona ->
            Spacer(modifier = Modifier.height(16.dp))
            TarjetaAlerta(titulo = "Cosecha Próxima", subtitulo = "${zona.nombre} lista en ${zona.diasParaCosecha} días", icono = Icons.Default.WarningAmber, colorContenedor = Color(0xFFFFF4E5).copy(alpha = 0.9f), colorContenido = Color(0xFF855300))
        }

        Spacer(modifier = Modifier.height(32.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(text = "Mapa de Zonas", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = colorTexto)
                Spacer(modifier = Modifier.width(8.dp))
                IconButton(onClick = alHacerClicEnAgregar, modifier = Modifier.bounceClick()) {
                    Icon(Icons.Default.AddCircle, contentDescription = "Agregar Zona", tint = colorTexto, modifier = Modifier.size(28.dp))
                }
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
        CuadriculaZonas(zonas, alHacerClicEnZona, alEditarZona, alEliminarZona)
        Spacer(modifier = Modifier.height(140.dp))
    }
}

@Composable
fun ClimaHeaderCargando() {
    val verdeOscuro = Color(0xFF2E401F)
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = Color(0xFFC0E0A0).copy(alpha = 0.1f),
        modifier = Modifier.bounceClick()
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Column(horizontalAlignment = Alignment.End) {
                Text(text = "--°C", fontWeight = FontWeight.ExtraBold, fontSize = 18.sp, color = verdeOscuro.copy(alpha = 0.3f))
                Text(text = "--%", fontSize = 11.sp, color = verdeOscuro.copy(alpha = 0.2f))
            }
            Surface(shape = CircleShape, color = Color.Gray.copy(alpha = 0.1f), modifier = Modifier.size(36.dp)) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.Cloud, null, tint = Color.Gray.copy(alpha = 0.3f), modifier = Modifier.size(20.dp))
                }
            }
        }
    }
}

@Composable
fun ClimaHeaderCompacto(datos: DatosClima) {
    val colorTexto = MaterialTheme.colorScheme.onSurface
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
        modifier = Modifier.bounceClick()
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = datos.temperatura, 
                    fontWeight = FontWeight.ExtraBold, 
                    fontSize = 18.sp, 
                    color = colorTexto
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.WaterDrop, null, tint = colorTexto.copy(alpha = 0.5f), modifier = Modifier.size(10.dp))
                    Text(
                        text = datos.humedad, 
                        fontSize = 11.sp, 
                        color = colorTexto.copy(alpha = 0.6f),
                        fontWeight = FontWeight.Medium
                    )
                }
            }
            Surface(
                shape = CircleShape,
                color = Color(0xFFFFB300).copy(alpha = 0.1f),
                modifier = Modifier.size(36.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        Icons.Default.WbSunny,
                        contentDescription = null,
                        tint = Color(0xFFFFB300),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun TarjetaAlerta(titulo: String, subtitulo: String, icono: ImageVector, colorContenedor: Color, colorContenido: Color) {
    Surface(shape = RoundedCornerShape(24.dp), color = colorContenedor, modifier = Modifier.fillMaxWidth().bounceClick()) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icono, contentDescription = null, tint = colorContenido, modifier = Modifier.size(28.dp))
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(text = titulo, fontWeight = FontWeight.Bold, color = colorContenido, fontSize = 15.sp)
                Text(text = subtitulo, color = colorContenido.copy(alpha = 0.8f), fontSize = 13.sp)
            }
        }
    }
}

@Composable
fun CuadriculaZonas(zonas: List<InformacionZona>, alHacerClicEnZona: (InformacionZona) -> Unit, alEditar: (InformacionZona) -> Unit, alEliminar: (InformacionZona) -> Unit) {
    Column {
        zonas.chunked(2).forEach { par ->
            Row(modifier = Modifier.fillMaxWidth()) {
                par.forEach { zona ->
                    Box(modifier = Modifier.weight(1f)) {
                        TarjetaZona(zona, alHacerClic = { alHacerClicEnZona(zona) }, alEditar = { alEditar(zona) }, alEliminar = { alEliminar(zona) })
                    }
                }
                if (par.size == 1) Spacer(modifier = Modifier.weight(1f))
            }
            Spacer(modifier = Modifier.height(12.dp))
        }
    }
}

@Composable
fun TarjetaZona(zona: InformacionZona, alHacerClic: () -> Unit, alEditar: () -> Unit, alEliminar: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(28.dp), 
        color = MaterialTheme.colorScheme.surface, 
        modifier = Modifier.padding(6.dp).fillMaxWidth().height(200.dp).bounceClick().clickable { alHacerClic() }, 
        shadowElevation = 2.dp
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Row(modifier = Modifier.fillMaxWidth().padding(12.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                IconButton(onClick = alEditar, modifier = Modifier.bounceClick()) { Icon(Icons.Default.Edit, contentDescription = null, tint = Color.Gray) }
                IconButton(onClick = alEliminar, modifier = Modifier.bounceClick()) { Icon(Icons.Default.Delete, contentDescription = null, tint = Color.Red.copy(alpha = 0.6f)) }
            }
            Column(modifier = Modifier.fillMaxSize().padding(top = 40.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                Surface(shape = CircleShape, color = zona.color.copy(alpha = 0.2f), modifier = Modifier.size(56.dp)) {
                    Box(contentAlignment = Alignment.Center) { Icon(Icons.Default.LocationOn, contentDescription = null, tint = zona.color) }
                }
                Spacer(modifier = Modifier.height(12.dp))
                Text(text = zona.nombre, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = MaterialTheme.colorScheme.onSurface)
                Text(text = zona.cultivo, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
fun FormularioAgregarZona(
    viewModel: MainViewModel,
    nombreInicial: String,
    cultivoInicial: String,
    alCambiarDatos: (String, String) -> Unit,
    alDibujarEnMapa: () -> Unit,
    alAgregar: (InformacionZona) -> Unit
) {
    var nombre by remember(nombreInicial) { mutableStateOf(nombreInicial) }
    var cultivo by remember(cultivoInicial) { mutableStateOf(cultivoInicial) }
    val locale = LocalConfiguration.current.locales[0]
    
    val hectareasCalculadas = if (viewModel.puntosEdicion.isNotEmpty()) 
        String.format(locale, "%.2f", viewModel.calcularHectareas(viewModel.puntosEdicion)) 
    else ""

    Column(modifier = Modifier.fillMaxWidth().padding(24.dp).verticalScroll(rememberScrollState())) {
        Text("Nueva Zona de Cultivo", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
        Spacer(modifier = Modifier.height(20.dp))
        
        OutlinedTextField(
            value = nombre, 
            onValueChange = { 
                nombre = it
                alCambiarDatos(it, cultivo)
            }, 
            label = { Text("Nombre") }, 
            modifier = Modifier.fillMaxWidth(), 
            shape = RoundedCornerShape(16.dp)
        )
        Spacer(modifier = Modifier.height(12.dp))
        
        OutlinedTextField(
            value = cultivo, 
            onValueChange = { 
                cultivo = it
                alCambiarDatos(nombre, it)
            }, 
            label = { Text("Cultivo") }, 
            modifier = Modifier.fillMaxWidth(), 
            shape = RoundedCornerShape(16.dp)
        )
        Spacer(modifier = Modifier.height(12.dp))

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedTextField(
                value = hectareasCalculadas, 
                onValueChange = { }, 
                readOnly = true,
                label = { Text("Hectáreas") }, 
                modifier = Modifier.weight(1f), 
                shape = RoundedCornerShape(16.dp)
            )
            
            Button(
                onClick = alDibujarEnMapa,
                modifier = Modifier.weight(1.2f).height(56.dp).bounceClick(),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                    contentColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Icon(Icons.Default.Map, null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(if(viewModel.puntosEdicion.isEmpty()) "Dibujar" else "Redibujar", fontSize = 12.sp)
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
        Button(
            onClick = { 
                alAgregar(InformacionZona(
                    nombre = nombre, 
                    cultivo = cultivo, 
                    color = Color(0xFFC0E0A0), 
                    diasParaCosecha = 30, 
                    ubicacion = if (viewModel.puntosEdicion.isNotEmpty()) viewModel.puntosEdicion.first() else LatLng(-12.0, -77.0), 
                    hectareas = hectareasCalculadas.replace(",", ".").toDoubleOrNull() ?: 0.0,
                    vertices = viewModel.puntosEdicion.toList()
                )) 
                viewModel.puntosEdicion.clear()
            },
            enabled = nombre.isNotBlank() && cultivo.isNotBlank() && viewModel.puntosEdicion.size >= 3,
            modifier = Modifier.fillMaxWidth().height(56.dp).bounceClick(),
            shape = RoundedCornerShape(28.dp)
        ) { Text("Guardar Zona") }
    }
}

@Composable
fun FormularioEditarZona(
    viewModel: MainViewModel,
    zona: InformacionZona, 
    nombreActual: String,
    cultivoActual: String,
    alCambiarDatos: (String, String) -> Unit,
    alDibujarEnMapa: () -> Unit,
    alActualizar: (InformacionZona) -> Unit
) {
    var nombre by remember(nombreActual) { mutableStateOf(nombreActual) }
    var cultivo by remember(cultivoActual) { mutableStateOf(cultivoActual) }
    val locale = LocalConfiguration.current.locales[0]
    
    val hectareasCalculadas = if (viewModel.puntosEdicion.isNotEmpty()) 
        String.format(locale, "%.2f", viewModel.calcularHectareas(viewModel.puntosEdicion)) 
    else String.format(locale, "%.2f", zona.hectareas)

    Column(modifier = Modifier.fillMaxWidth().padding(24.dp).verticalScroll(rememberScrollState())) {
        Text("Editar Zona", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
        Spacer(modifier = Modifier.height(20.dp))
        
        OutlinedTextField(
            value = nombre, 
            onValueChange = { 
                nombre = it
                alCambiarDatos(it, cultivo)
            }, 
            label = { Text("Nombre") }, 
            modifier = Modifier.fillMaxWidth(), 
            shape = RoundedCornerShape(16.dp)
        )
        Spacer(modifier = Modifier.height(12.dp))
        
        OutlinedTextField(
            value = cultivo, 
            onValueChange = { 
                cultivo = it
                alCambiarDatos(nombre, it)
            }, 
            label = { Text("Cultivo") }, 
            modifier = Modifier.fillMaxWidth(), 
            shape = RoundedCornerShape(16.dp)
        )
        Spacer(modifier = Modifier.height(12.dp))

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedTextField(
                value = hectareasCalculadas, 
                onValueChange = { }, 
                readOnly = true,
                label = { Text("Hectáreas") }, 
                modifier = Modifier.weight(1f), 
                shape = RoundedCornerShape(16.dp)
            )
            
            Button(
                onClick = alDibujarEnMapa,
                modifier = Modifier.weight(1.2f).height(56.dp).bounceClick(),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                    contentColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Icon(Icons.Default.Map, null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Redibujar", fontSize = 12.sp)
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
        Button(
            onClick = { 
                alActualizar(zona.copy(
                    nombre = nombre, 
                    cultivo = cultivo, 
                    hectareas = hectareasCalculadas.replace(",", ".").toDoubleOrNull() ?: zona.hectareas,
                    vertices = viewModel.puntosEdicion.toList(),
                    ubicacion = if (viewModel.puntosEdicion.isNotEmpty()) viewModel.puntosEdicion.first() else zona.ubicacion
                )) 
                viewModel.puntosEdicion.clear()
            },
            enabled = nombre.isNotBlank() && cultivo.isNotBlank(),
            modifier = Modifier.fillMaxWidth().height(56.dp).bounceClick(),
            shape = RoundedCornerShape(28.dp)
        ) { Text("Actualizar") }
    }
}

@Composable
fun ContenidoDetalleZona(zona: InformacionZona) {
    val contexto = LocalContext.current
    Configuration.getInstance().userAgentValue = contexto.packageName

    Column(modifier = Modifier.fillMaxWidth().padding(24.dp).verticalScroll(rememberScrollState())) {
        Text(text = zona.nombre, fontSize = 24.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
        Text(text = "Cultivo: ${zona.cultivo}", fontSize = 16.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(modifier = Modifier.height(24.dp))
        
        Surface(modifier = Modifier.fillMaxWidth().height(220.dp), shape = RoundedCornerShape(24.dp), color = Color.LightGray) {
            AndroidView(
                factory = { ctx ->
                    MapView(ctx).apply {
                        setTileSource(TileSourceFactory.MAPNIK)
                        controller.setZoom(16.0)
                        controller.setCenter(GeoPoint(zona.ubicacion.latitude, zona.ubicacion.longitude))

                        // Si hay vértices guardados, dibujar el polígono real
                        if (zona.vertices.size >= 3) {
                            val poligono = Polygon(this)
                            poligono.points = zona.vertices.map { GeoPoint(it.latitude, it.longitude) }
                            poligono.fillPaint.color = android.graphics.Color.argb(80, 192, 224, 160)
                            poligono.outlinePaint.color = android.graphics.Color.rgb(46, 64, 31)
                            poligono.outlinePaint.strokeWidth = 3f
                            overlays.add(poligono)
                        } else {
                            // Si no hay vértices (legacy), mostrar marcador
                            val marcador = Marker(this)
                            marcador.position = GeoPoint(zona.ubicacion.latitude, zona.ubicacion.longitude)
                            marcador.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                            overlays.add(marcador)
                        }
                    }
                },
                modifier = Modifier.fillMaxSize()
            )
        }
        Spacer(modifier = Modifier.height(24.dp))
        val locale = LocalConfiguration.current.locales[0]
        Text(text = "Superficie: ${String.format(locale, "%.2f", zona.hectareas)} ha", fontSize = 18.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
        Text(text = "Días para cosecha: ${zona.diasParaCosecha}", fontSize = 16.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@SuppressLint("ViewModelConstructorInComposable")
@Preview(showBackground = true)
@Composable
fun VistaPreviaPantallaPanelControl() {
    val context = LocalContext.current
    val viewModel = MainViewModel(context.applicationContext as Application)
    AgroScanTheme {
        PantallaPanelControl(viewModel = viewModel)
    }
}
