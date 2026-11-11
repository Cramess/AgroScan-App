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
import org.osmdroid.views.overlay.Polyline
import org.osmdroid.views.CustomZoomButtonsController
import org.osmdroid.tileprovider.tilesource.OnlineTileSourceBase
import org.osmdroid.util.BoundingBox
import org.osmdroid.util.MapTileIndex
import com.google.android.gms.location.LocationServices
import com.google.maps.android.SphericalUtil
import com.tecsup.agroscan.data.GeometriaParcela
import androidx.compose.runtime.saveable.rememberSaveable
import android.Manifest
import android.content.pm.PackageManager
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
import com.tecsup.agroscan.ui.utils.aparicion
import com.tecsup.agroscan.ui.utils.especAnimacion
import com.tecsup.agroscan.ui.utils.CurvaSuave
import com.tecsup.agroscan.ui.utils.LocalAnimaciones
import com.tecsup.agroscan.ui.utils.FondoVidrio
import com.tecsup.agroscan.ui.utils.PanelVidrio
import com.tecsup.agroscan.ui.utils.fuenteVidrio
import com.tecsup.agroscan.ui.utils.rememberFondoVidrio
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.ui.draw.clip
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PantallaPanelControl(
    viewModel: MainViewModel,
    initialTab: Int = 0,
    solicitudCamara: Int = 0,   // Cambia cada vez que el widget pide abrir la cámara con la app ya abierta
    alSolicitarUbicacion: () -> Unit = {},
    alSolicitarCamara: () -> Unit = {}
) {
    var itemSeleccionado by remember { mutableIntStateOf(initialTab) }
    LaunchedEffect(solicitudCamara) {
        if (solicitudCamara > 0) itemSeleccionado = 1
    }
    val contextoPanel = androidx.compose.ui.platform.LocalContext.current
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

    var mostrarDialogoEliminar by remember { mutableStateOf(false) }
    var zonaParaEliminar by remember { mutableStateOf<InformacionZona?>(null) }

    val colorFondo = MaterialTheme.colorScheme.background
    val colorTextoNav = MaterialTheme.colorScheme.primary
    val animaciones = LocalAnimaciones.current

    // Lo que se ve (desenfocado) a través de la barra de navegación de vidrio
    val fondoApp = rememberFondoVidrio()

    Box(modifier = Modifier.fillMaxSize().background(colorFondo)) {
        // fuenteVidrio va antes que background para que el fondo también quede dentro de la capa capturada
        Column(modifier = Modifier.fillMaxSize().fuenteVidrio(fondoApp).background(colorFondo)) {
            Box(modifier = Modifier.weight(1f)) {
                AnimatedContent(
                    targetState = itemSeleccionado,
                    transitionSpec = {
                        if (!animaciones) {
                            EnterTransition.None togetherWith ExitTransition.None
                        } else {
                            // Desplazamiento corto + fundido: se percibe la dirección sin mover la pantalla entera
                            val direccion = if (targetState > initialState) 1 else -1
                            (fadeIn(tween(320, 80, CurvaSuave)) +
                                    slideInHorizontally(tween(380, easing = CurvaSuave)) { direccion * it / 6 } +
                                    scaleIn(tween(380, easing = CurvaSuave), initialScale = 0.98f)) togetherWith
                                    (fadeOut(tween(160)) +
                                            slideOutHorizontally(tween(260, easing = CurvaSuave)) { -direccion * it / 10 })
                        }.using(SizeTransform(clip = false))
                    },
                    label = "navTransition"
                ) { targetIndex ->
                    when (targetIndex) {
                        0 -> ContenidoPrincipalPanelControl(
                            viewModel = viewModel,
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

        PanelVidrio(
            fondo = fondoApp,
            modifier = Modifier.align(Alignment.BottomCenter).padding(horizontal = 20.dp).navigationBarsPadding().padding(bottom = 12.dp).fillMaxWidth().height(72.dp),
            forma = RoundedCornerShape(36.dp),
            opacidadTinte = 0.62f,
            opacidadSinVidrio = 0.98f
        ) {
            Row(modifier = Modifier.fillMaxSize(), horizontalArrangement = Arrangement.SpaceAround, verticalAlignment = Alignment.CenterVertically) {
                val itemsNav = listOf("Inicio", "Escaneo", "Historial", "Perfil")
                val iconos = listOf(Icons.Default.Home, Icons.Default.CameraAlt, Icons.Default.History, Icons.Default.Person)
                itemsNav.forEachIndexed { index, item ->
                    val estaSeleccionado = itemSeleccionado == index
                    // Píldora que se ilumina detrás de la pestaña activa
                    val colorPildora by animateColorAsState(
                        if (estaSeleccionado) colorTextoNav.copy(alpha = 0.12f) else Color.Transparent,
                        especAnimacion(300), label = "pildoraNav"
                    )
                    val colorItem by animateColorAsState(
                        if (estaSeleccionado) colorTextoNav else Color(0xFF8E9196),
                        especAnimacion(300), label = "colorNav"
                    )
                    val tamanoIcono by animateDpAsState(
                        if (estaSeleccionado) 26.dp else 23.dp, especAnimacion(300), label = "iconoNav"
                    )
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 4.dp)
                            .clip(RoundedCornerShape(24.dp))
                            .background(colorPildora)
                            .bounceClick()
                            .clickable { itemSeleccionado = index }
                            .padding(vertical = 6.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(imageVector = iconos[index], contentDescription = item, tint = colorItem, modifier = Modifier.size(tamanoIcono))
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(text = item, fontSize = 11.sp, fontWeight = if (estaSeleccionado) FontWeight.Bold else FontWeight.Medium, color = colorItem)
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
                        val error = viewModel.agregarZona(nuevaZona)
                        if (error != null) {
                            android.widget.Toast.makeText(contextoPanel, error, android.widget.Toast.LENGTH_LONG).show()
                        } else {
                            nombreEnProceso = ""
                            cultivoEnProceso = ""
                            mostrarHojaAgregar = false
                        }
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
                        val error = viewModel.actualizarZona(zonaSeleccionada!!, zonaActualizada)
                        if (error != null) {
                            android.widget.Toast.makeText(contextoPanel, error, android.widget.Toast.LENGTH_LONG).show()
                        } else {
                            nombreEnProceso = ""
                            cultivoEnProceso = ""
                            modoEdicionPoligono = false
                            mostrarHojaEditar = false
                        }
                    }
                )
            }
        }

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

/** Imágenes satelitales de Esri World Imagery (sus teselas usan el orden z/y/x). */
private object EsriSatelite : OnlineTileSourceBase(
    "EsriWorldImagery", 0, 19, 256, "",
    arrayOf("https://server.arcgisonline.com/ArcGIS/rest/services/World_Imagery/MapServer/tile/")
) {
    override fun getTileURLString(pMapTileIndex: Long): String =
        baseUrl + MapTileIndex.getZoom(pMapTileIndex) + "/" +
                MapTileIndex.getY(pMapTileIndex) + "/" + MapTileIndex.getX(pMapTileIndex)
}

private fun iconoVertice(contexto: android.content.Context, numero: Int, alerta: Boolean): android.graphics.Bitmap {
    val densidad = contexto.resources.displayMetrics.density
    val lado = (30 * densidad).toInt()
    val bitmap = android.graphics.Bitmap.createBitmap(lado, lado, android.graphics.Bitmap.Config.ARGB_8888)
    val lienzo = android.graphics.Canvas(bitmap)
    val pincel = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG)
    val color = if (alerta) android.graphics.Color.rgb(198, 40, 40) else android.graphics.Color.rgb(46, 64, 31)
    val radio = lado / 2f
    pincel.color = android.graphics.Color.WHITE
    lienzo.drawCircle(radio, radio, radio - densidad, pincel)
    pincel.style = android.graphics.Paint.Style.STROKE
    pincel.strokeWidth = 3 * densidad
    pincel.color = color
    lienzo.drawCircle(radio, radio, radio - 2.5f * densidad, pincel)
    pincel.style = android.graphics.Paint.Style.FILL
    pincel.textSize = 13 * densidad
    pincel.typeface = android.graphics.Typeface.DEFAULT_BOLD
    pincel.textAlign = android.graphics.Paint.Align.CENTER
    lienzo.drawText(numero.toString(), radio, radio - (pincel.descent() + pincel.ascent()) / 2, pincel)
    return bitmap
}

@SuppressLint("MissingPermission")
private fun centrarEnUbicacion(contexto: android.content.Context, mapa: MapView) {
    val conPermiso = listOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)
        .any { ContextCompat.checkSelfPermission(contexto, it) == PackageManager.PERMISSION_GRANTED }
    if (!conPermiso) {
        android.widget.Toast.makeText(contexto, "Active el permiso de ubicación", android.widget.Toast.LENGTH_SHORT).show()
        return
    }
    LocationServices.getFusedLocationProviderClient(contexto).lastLocation.addOnSuccessListener { ubicacion ->
        if (ubicacion != null) {
            mapa.controller.animateTo(GeoPoint(ubicacion.latitude, ubicacion.longitude), 18.0, 700L)
        } else {
            android.widget.Toast.makeText(contexto, "Ubicación aún no disponible", android.widget.Toast.LENGTH_SHORT).show()
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
    val locale = LocalConfiguration.current.locales[0]
    val puntos = viewModel.puntosEdicion
    val geo = puntos.map { GeometriaParcela.Punto(it.latitude, it.longitude) }
    val hectareasCalculadas = viewModel.calcularHectareas(puntos)
    val perimetro = when {
        puntos.size >= 3 -> SphericalUtil.computeLength(puntos + puntos.first())
        puntos.size == 2 -> SphericalUtil.computeLength(puntos)
        else -> 0.0
    }
    val bordesCruzados = puntos.size >= 4 && !GeometriaParcela.esSimple(geo)

    var satelite by rememberSaveable { mutableStateOf(true) }
    var mapa by remember { mutableStateOf<MapView?>(null) }
    val fondoMapa = rememberFondoVidrio()
    // Los iconos numerados de los vértices se dibujan una sola vez; antes se creaba un bitmap nuevo
    // por vértice en cada actualización del mapa
    val cacheIconos = remember { HashMap<Pair<Int, Boolean>, android.graphics.Bitmap>() }
    // Puntos en el orden en que se tocaron, para que "deshacer" quite el último aunque se haya insertado en medio
    val pilaDeshacer = remember { mutableStateListOf<LatLng>() }

    // Configuración necesaria para OSM
    Configuration.getInstance().userAgentValue = contexto.packageName

    // Al abrir: encuadrar la parcela que se edita o centrar en la ubicación del técnico
    LaunchedEffect(mapa) {
        val m = mapa ?: return@LaunchedEffect
        when {
            puntos.size >= 2 -> {
                val caja = BoundingBox.fromGeoPoints(puntos.map { GeoPoint(it.latitude, it.longitude) })
                m.post { m.zoomToBoundingBox(caja.increaseByScale(1.6f), false) }
            }
            puntos.size == 1 -> m.controller.setCenter(GeoPoint(puntos[0].latitude, puntos[0].longitude))
            else -> centrarEnUbicacion(contexto, m)
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        AndroidView(
            factory = { ctx ->
                MapView(ctx).apply {
                    setMultiTouchControls(true)
                    zoomController.setVisibility(CustomZoomButtonsController.Visibility.NEVER)
                    controller.setZoom(17.0)
                    controller.setCenter(GeoPoint(-12.046374, -77.042793))

                    val overlayEventos = MapEventsOverlay(object : MapEventsReceiver {
                        override fun singleTapConfirmedHelper(p: GeoPoint): Boolean {
                            val nuevo = LatLng(p.latitude, p.longitude)
                            val actuales = viewModel.puntosEdicion.map { GeometriaParcela.Punto(it.latitude, it.longitude) }
                            val indice = GeometriaParcela.indiceDeInsercion(actuales, GeometriaParcela.Punto(p.latitude, p.longitude))
                            viewModel.puntosEdicion.add(indice, nuevo)
                            pilaDeshacer.add(nuevo)
                            return true
                        }
                        override fun longPressHelper(p: GeoPoint): Boolean = false
                    })
                    overlays.add(overlayEventos)
                    mapa = this
                }
            },
            update = { mapView ->
                val fuente = if (satelite) EsriSatelite else TileSourceFactory.MAPNIK
                if (mapView.tileProvider.tileSource != fuente) mapView.setTileSource(fuente)

                mapView.overlays.removeAll { it is Marker || it is Polygon || it is Polyline }
                val puntosGeo = puntos.map { GeoPoint(it.latitude, it.longitude) }
                val colorBorde = if (bordesCruzados) android.graphics.Color.rgb(198, 40, 40) else android.graphics.Color.rgb(192, 224, 160)

                if (puntosGeo.size >= 3) {
                    val poligono = Polygon(mapView)
                    poligono.points = puntosGeo
                    poligono.fillPaint.color = if (bordesCruzados) android.graphics.Color.argb(70, 198, 40, 40)
                    else android.graphics.Color.argb(90, 192, 224, 160)
                    poligono.outlinePaint.color = colorBorde
                    poligono.outlinePaint.strokeWidth = 4f * contexto.resources.displayMetrics.density
                    poligono.setOnClickListener { _, _, _ -> false }
                    mapView.overlays.add(poligono)
                } else if (puntosGeo.size == 2) {
                    val linea = Polyline(mapView)
                    linea.setPoints(puntosGeo)
                    linea.outlinePaint.color = colorBorde
                    linea.outlinePaint.strokeWidth = 4f * contexto.resources.displayMetrics.density
                    mapView.overlays.add(linea)
                }

                puntos.forEachIndexed { i, latLng ->
                    val marcador = Marker(mapView)
                    marcador.position = GeoPoint(latLng.latitude, latLng.longitude)
                    marcador.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
                    val bitmapIcono = cacheIconos.getOrPut((i + 1) to bordesCruzados) {
                        iconoVertice(contexto, i + 1, bordesCruzados)
                    }
                    marcador.icon = android.graphics.drawable.BitmapDrawable(contexto.resources, bitmapIcono)
                    marcador.setInfoWindow(null)
                    // Mantener presionado un punto para moverlo
                    marcador.isDraggable = true
                    marcador.setOnMarkerDragListener(object : Marker.OnMarkerDragListener {
                        override fun onMarkerDrag(m: Marker) {}
                        override fun onMarkerDragStart(m: Marker) {}
                        override fun onMarkerDragEnd(m: Marker) {
                            if (i < viewModel.puntosEdicion.size) {
                                viewModel.puntosEdicion[i] = LatLng(m.position.latitude, m.position.longitude)
                            }
                        }
                    })
                    marcador.setOnMarkerClickListener { _, _ -> true }
                    mapView.overlays.add(marcador)
                }
                mapView.invalidate()
            },
            modifier = Modifier.fillMaxSize().fuenteVidrio(fondoMapa)
        )

        PanelVidrio(
            fondo = fondoMapa,
            forma = RoundedCornerShape(24.dp),
            opacidadTinte = 0.6f,
            opacidadSinVidrio = 0.96f,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .statusBarsPadding()
                .padding(top = 12.dp)
                .padding(horizontal = 16.dp)
                .fillMaxWidth()
                .aparicion()
        ) {
            Column(modifier = Modifier.padding(horizontal = 18.dp, vertical = 14.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Dibujar parcela", fontWeight = FontWeight.Bold, fontSize = 17.sp,
                        color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.weight(1f))
                    if (puntos.isNotEmpty()) {
                        TextButton(onClick = { puntos.clear(); pilaDeshacer.clear() }, contentPadding = PaddingValues(horizontal = 8.dp)) {
                            Text("Limpiar", fontSize = 13.sp)
                        }
                    }
                }
                Text(
                    text = when {
                        puntos.isEmpty() -> "Toca el mapa para marcar la primera esquina"
                        puntos.size < 3 -> "Marca ${3 - puntos.size} esquina${if (puntos.size == 2) "" else "s"} más"
                        else -> "Marca las esquinas en cualquier orden · mantén presionado un punto para moverlo"
                    },
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(10.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    MedidaDibujo(String.format(locale, "%.2f", hectareasCalculadas), "hectáreas", destacada = true)
                    MedidaDibujo(
                        if (perimetro >= 1000) String.format(locale, "%.2f km", perimetro / 1000) else String.format(locale, "%.0f m", perimetro),
                        "perímetro"
                    )
                    MedidaDibujo(puntos.size.toString(), "vértices")
                }
                AnimatedVisibility(visible = bordesCruzados) {
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = MaterialTheme.colorScheme.errorContainer,
                        modifier = Modifier.padding(top = 10.dp).fillMaxWidth()
                    ) {
                        Row(modifier = Modifier.padding(start = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.WarningAmber, null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Los bordes se cruzan", fontSize = 13.sp, color = MaterialTheme.colorScheme.onErrorContainer,
                                modifier = Modifier.weight(1f))
                            TextButton(onClick = {
                                val ordenados = GeometriaParcela.ordenarPorAngulo(geo).map { LatLng(it.lat, it.lng) }
                                puntos.clear()
                                puntos.addAll(ordenados)
                                pilaDeshacer.clear()
                            }) { Text("Ordenar", fontWeight = FontWeight.Bold) }
                        }
                    }
                }
            }
        }

        Column(
            modifier = Modifier.align(Alignment.CenterEnd).padding(end = 16.dp, top = 80.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            BotonMapa(fondoMapa, if (satelite) Icons.Default.Map else Icons.Default.Satellite, if (satelite) "Ver mapa" else "Ver satélite") {
                satelite = !satelite
            }
            BotonMapa(fondoMapa, Icons.Default.MyLocation, "Mi ubicación") { mapa?.let { centrarEnUbicacion(contexto, it) } }
            BotonMapa(fondoMapa, Icons.Default.Add, "Acercar") { mapa?.controller?.zoomIn() }
            BotonMapa(fondoMapa, Icons.Default.Remove, "Alejar") { mapa?.controller?.zoomOut() }
            AnimatedVisibility(visible = puntos.isNotEmpty()) {
                BotonMapa(fondoMapa, Icons.AutoMirrored.Filled.Undo, "Deshacer") {
                    val ultimo = pilaDeshacer.removeLastOrNull()
                    if (ultimo != null && puntos.remove(ultimo)) return@BotonMapa
                    if (puntos.isNotEmpty()) puntos.removeAt(puntos.lastIndex)
                }
            }
        }

        // Atribución obligatoria de las teselas
        Text(
            text = if (satelite) "© Esri, Maxar, Earthstar Geographics" else "© OpenStreetMap",
            fontSize = 10.sp,
            color = Color.White,
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(start = 16.dp, bottom = 112.dp)
                .background(Color.Black.copy(alpha = 0.45f), RoundedCornerShape(6.dp))
                .padding(horizontal = 6.dp, vertical = 2.dp)
        )

        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(bottom = 24.dp)
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
                enabled = puntos.size >= 3 && !bordesCruzados,
                modifier = Modifier.weight(1f).height(56.dp).bounceClick(),
                shape = RoundedCornerShape(28.dp)
            ) {
                Icon(Icons.Default.Check, null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Terminar", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun MedidaDibujo(valor: String, etiqueta: String, destacada: Boolean = false) {
    Column {
        Text(
            valor,
            fontSize = if (destacada) 24.sp else 18.sp,
            fontWeight = FontWeight.ExtraBold,
            color = if (destacada) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
        )
        Text(etiqueta, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun BotonMapa(fondo: FondoVidrio, icono: ImageVector, descripcion: String, alPulsar: () -> Unit) {
    PanelVidrio(
        fondo = fondo,
        forma = CircleShape,
        opacidadTinte = 0.6f,
        opacidadSinVidrio = 1f,
        alineacion = Alignment.Center,
        modifier = Modifier.size(44.dp).bounceClick().clip(CircleShape).clickable(onClick = alPulsar)
    ) {
        Icon(icono, descripcion, tint = MaterialTheme.colorScheme.primary)
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
    viewModel: MainViewModel,
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
    val contexto = LocalContext.current

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
        
        if (viewModel.alertasClimaticasActivas.isNotEmpty()) {
            Spacer(modifier = Modifier.height(16.dp))
            Text("Alertas Agroclimáticas Activas", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error)
            Spacer(modifier = Modifier.height(8.dp))
            viewModel.alertasClimaticasActivas.forEachIndexed { i, alerta ->
                Box(Modifier.aparicion(i)) { TarjetaAlerta(
                    titulo = "${alerta.titulo} (${alerta.patogenoAsociado})",
                    subtitulo = "${alerta.descripcion}\nRecomendación: ${alerta.recomendaciones}",
                    icono = Icons.Default.Thunderstorm,
                    colorContenedor = Color(0xFFFDE8E8),
                    colorContenido = Color(0xFF9B1C1C)
                ) }
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
        
        Spacer(modifier = Modifier.height(12.dp))
        OutlinedButton(
            onClick = {
                viewModel.sincronizarConPlataformaWeb { exito, msg ->
                    android.widget.Toast.makeText(contexto, msg, android.widget.Toast.LENGTH_LONG).show()
                }
            },
            modifier = Modifier.fillMaxWidth().height(48.dp).bounceClick(),
            shape = RoundedCornerShape(24.dp),
            enabled = !viewModel.estaSincronizandoWeb
        ) {
            Icon(Icons.Default.CloudUpload, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text(if (viewModel.estaSincronizandoWeb) "Conectando con la App Web..." else "Comprobar conexión con App Web", fontSize = 13.sp)
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
        zonas.chunked(2).forEachIndexed { fila, par ->
            Row(modifier = Modifier.fillMaxWidth()) {
                par.forEachIndexed { columna, zona ->
                    Box(modifier = Modifier.weight(1f).aparicion(fila * 2 + columna)) {
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

fun estimarDiasCosecha(cultivo: String): Int {
    val text = cultivo.lowercase()
    return when {
        text.contains("papa") -> 120
        text.contains("maíz") || text.contains("maiz") -> 140
        text.contains("café") || text.contains("cafe") -> 270
        text.contains("quinua") -> 150
        text.contains("arroz") -> 120
        text.contains("tomate") -> 90
        text.contains("cacao") -> 180
        else -> 90
    }
}

fun calcularFechaCosecha(fechaSiembraStr: String, dias: Int, locale: Locale): String {
    return try {
        val sdf = SimpleDateFormat("dd/MM/yyyy", locale)
        val date = sdf.parse(fechaSiembraStr) ?: Date()
        val calendar = Calendar.getInstance().apply {
            time = date
            add(Calendar.DAY_OF_YEAR, dias)
        }
        sdf.format(calendar.time)
    } catch (_: Exception) {
        val calendar = Calendar.getInstance().apply {
            add(Calendar.DAY_OF_YEAR, dias)
        }
        SimpleDateFormat("dd/MM/yyyy", locale).format(calendar.time)
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
    var tipoSuelo by remember { mutableStateOf("Franco-Arcilloso") }
    var phSuelo by remember { mutableStateOf("6.8") }
    var nivelN by remember { mutableStateOf("Medio") }
    var nivelP by remember { mutableStateOf("Medio") }
    var nivelK by remember { mutableStateOf("Medio") }
    
    val locale = LocalConfiguration.current.locales[0]
    val fechaActual = remember { SimpleDateFormat("dd/MM/yyyy", locale).format(Date()) }
    var fechaSiembra by remember { mutableStateOf(fechaActual) }

    val diasEstimados = estimarDiasCosecha(cultivo)
    val fechaCosechaCalculada = remember(fechaSiembra, diasEstimados) {
        calcularFechaCosecha(fechaSiembra, diasEstimados, locale)
    }
    var fechaCosecha by remember(fechaCosechaCalculada) { mutableStateOf(fechaCosechaCalculada) }
    
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

        OutlinedTextField(
            value = fechaSiembra,
            onValueChange = { fechaSiembra = it },
            label = { Text("Fecha de Siembra (dd/MM/yyyy)") },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            singleLine = true
        )
        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = fechaCosecha,
            onValueChange = { fechaCosecha = it },
            label = { Text("Fecha Estimada de Cosecha (dd/MM/yyyy)") },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            singleLine = true
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

        Spacer(modifier = Modifier.height(16.dp))
        Text("Caracterización de Suelo y Nutrientes (NPK)", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
        Spacer(modifier = Modifier.height(8.dp))

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedTextField(
                value = tipoSuelo,
                onValueChange = { tipoSuelo = it },
                label = { Text("Tipo Suelo") },
                modifier = Modifier.weight(1.5f),
                shape = RoundedCornerShape(16.dp)
            )
            OutlinedTextField(
                value = phSuelo,
                onValueChange = { phSuelo = it },
                label = { Text("pH") },
                modifier = Modifier.weight(0.8f),
                shape = RoundedCornerShape(16.dp)
            )
        }

        Spacer(modifier = Modifier.height(10.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(value = nivelN, onValueChange = { nivelN = it }, label = { Text("Nitrógeno (N)") }, modifier = Modifier.weight(1f), shape = RoundedCornerShape(12.dp))
            OutlinedTextField(value = nivelP, onValueChange = { nivelP = it }, label = { Text("Fósforo (P)") }, modifier = Modifier.weight(1f), shape = RoundedCornerShape(12.dp))
            OutlinedTextField(value = nivelK, onValueChange = { nivelK = it }, label = { Text("Potasio (K)") }, modifier = Modifier.weight(1f), shape = RoundedCornerShape(12.dp))
        }

        Spacer(modifier = Modifier.height(24.dp))
        Button(
            onClick = { 
                alAgregar(InformacionZona(
                    nombre = nombre, 
                    cultivo = cultivo, 
                    color = Color(0xFFC0E0A0), 
                    diasParaCosecha = diasEstimados, 
                    ubicacion = if (viewModel.puntosEdicion.isNotEmpty()) viewModel.puntosEdicion.first() else LatLng(-12.0, -77.0), 
                    hectareas = hectareasCalculadas.replace(",", ".").toDoubleOrNull() ?: 0.0,
                    vertices = viewModel.puntosEdicion.toList(),
                    tipoSuelo = tipoSuelo,
                    phSuelo = phSuelo.toDoubleOrNull() ?: 6.8,
                    nivelNitrogeno = nivelN,
                    nivelFosforo = nivelP,
                    nivelPotasio = nivelK,
                    fechaSiembra = fechaSiembra,
                    fechaCosecha = fechaCosecha
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
    var fechaSiembra by remember(zona.fechaSiembra) { mutableStateOf(zona.fechaSiembra.ifBlank { SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date()) }) }
    val locale = LocalConfiguration.current.locales[0]

    val diasEstimados = estimarDiasCosecha(cultivo)
    val fechaCosechaCalculada = remember(fechaSiembra, diasEstimados) {
        calcularFechaCosecha(fechaSiembra, diasEstimados, locale)
    }
    var fechaCosecha by remember(zona.fechaCosecha, fechaCosechaCalculada) { 
        mutableStateOf(zona.fechaCosecha.ifBlank { fechaCosechaCalculada }) 
    }
    
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

        OutlinedTextField(
            value = fechaSiembra,
            onValueChange = { fechaSiembra = it },
            label = { Text("Fecha de Siembra (dd/MM/yyyy)") },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            singleLine = true
        )
        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = fechaCosecha,
            onValueChange = { fechaCosecha = it },
            label = { Text("Fecha Estimada de Cosecha (dd/MM/yyyy)") },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            singleLine = true
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
                    diasParaCosecha = diasEstimados,
                    hectareas = hectareasCalculadas.replace(",", ".").toDoubleOrNull() ?: zona.hectareas,
                    vertices = viewModel.puntosEdicion.toList(),
                    ubicacion = if (viewModel.puntosEdicion.isNotEmpty()) viewModel.puntosEdicion.first() else zona.ubicacion,
                    fechaSiembra = fechaSiembra,
                    fechaCosecha = fechaCosecha
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
        if (zona.fechaSiembra.isNotBlank()) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = "Siembra: ${zona.fechaSiembra}", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        if (zona.fechaCosecha.isNotBlank()) {
            Spacer(modifier = Modifier.height(2.dp))
            Text(text = "Cosecha estimada: ${zona.fechaCosecha} (${zona.diasParaCosecha} días)", fontSize = 14.sp, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold)
        }
        Spacer(modifier = Modifier.height(24.dp))
        
        Surface(modifier = Modifier.fillMaxWidth().height(220.dp), shape = RoundedCornerShape(24.dp), color = Color.LightGray) {
            AndroidView(
                factory = { ctx ->
                    MapView(ctx).apply {
                        setTileSource(TileSourceFactory.MAPNIK)
                        controller.setZoom(16.0)
                        controller.setCenter(GeoPoint(zona.ubicacion.latitude, zona.ubicacion.longitude))

                        if (zona.vertices.size >= 3) {
                            val poligono = Polygon(this)
                            poligono.points = zona.vertices.map { GeoPoint(it.latitude, it.longitude) }
                            poligono.fillPaint.color = android.graphics.Color.argb(80, 192, 224, 160)
                            poligono.outlinePaint.color = android.graphics.Color.rgb(46, 64, 31)
                            poligono.outlinePaint.strokeWidth = 3f
                            overlays.add(poligono)
                        } else {
                            // Zonas creadas antes del dibujo de polígonos solo tienen un punto
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

        Spacer(modifier = Modifier.height(16.dp))
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Caracterización de Suelo & NPK", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.height(8.dp))
                Text("• Tipo de Suelo: ${zona.tipoSuelo}", fontSize = 13.sp)
                Text("• pH del Suelo: ${zona.phSuelo}", fontSize = 13.sp)
                Text("• Nitrógeno (N): ${zona.nivelNitrogeno} | Fósforo (P): ${zona.nivelFosforo} | Potasio (K): ${zona.nivelPotasio}", fontSize = 13.sp)
            }
        }
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
