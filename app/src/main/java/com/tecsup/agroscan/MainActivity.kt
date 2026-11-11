package com.tecsup.agroscan

import android.Manifest
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import com.tecsup.agroscan.widget.PREFS_WIDGET
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.runtime.CompositionLocalProvider
import com.tecsup.agroscan.ui.utils.CurvaSuave
import com.tecsup.agroscan.ui.utils.LocalAnimaciones
import com.tecsup.agroscan.ui.utils.LocalVidrio
import com.tecsup.agroscan.screen.PantallaLogin
import com.tecsup.agroscan.screen.PantallaPanelControl
import com.tecsup.agroscan.ui.theme.AgroScanTheme
import com.tecsup.agroscan.viewmodel.MainViewModel

class MainActivity : ComponentActivity() {
    // Aumenta cada vez que el widget pide abrir la cámara, también con la app ya abierta
    private var solicitudCamara by mutableIntStateOf(0)

    /** Lee (y consume) la petición de abrir la cámara que deja el widget en el intent o en sus preferencias. */
    private fun consumirSolicitudCamara(intent: Intent?): Boolean {
        val prefs = getSharedPreferences(PREFS_WIDGET, Context.MODE_PRIVATE)
        val desdePrefs = prefs.getBoolean("abrir_camara", false)
        if (desdePrefs) prefs.edit().putBoolean("abrir_camara", false).apply()
        val desdeIntent = intent?.getBooleanExtra("ABRIR_CAMARA", false) == true
        intent?.removeExtra("ABRIR_CAMARA")
        return desdeIntent || desdePrefs
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        if (consumirSolicitudCamara(intent)) solicitudCamara++
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        // Solo en el primer arranque: al recrear la actividad (rotación) no se vuelve a saltar a la cámara
        if (savedInstanceState == null && consumirSolicitudCamara(intent)) solicitudCamara = 1
        val tabInicial = if (solicitudCamara > 0) 1 else 0
        setContent {
            val viewModel: MainViewModel = viewModel()
            val context = androidx.compose.ui.platform.LocalContext.current
            val fusedLocationClient = remember { LocationServices.getFusedLocationProviderClient(context) }

            val lanzadorPermisos = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.RequestMultiplePermissions()
            ) { resultados ->
                val camaraOk = resultados[Manifest.permission.CAMERA] ?: false
                val ubicacionOk = resultados[Manifest.permission.ACCESS_FINE_LOCATION] ?: false ||
                                 resultados[Manifest.permission.ACCESS_COARSE_LOCATION] ?: false
                
                viewModel.actualizarEstadoCamara(camaraOk)
                viewModel.actualizarEstadoUbicacion(ubicacionOk)

                if (ubicacionOk) {
                    try {
                        fusedLocationClient.lastLocation.addOnSuccessListener { location: android.location.Location? ->
                            if (location != null) {
                                viewModel.obtenerClimaActual(location.latitude, location.longitude)
                            } else {
                                // Si la última ubicación es nula, solicitar una nueva
                                fusedLocationClient.getCurrentLocation(
                                    Priority.PRIORITY_HIGH_ACCURACY,
                                    CancellationTokenSource().token
                                ).addOnSuccessListener { freshLocation ->
                                    freshLocation?.let {
                                        viewModel.obtenerClimaActual(it.latitude, it.longitude)
                                    }
                                }
                            }
                        }
                    } catch (e: SecurityException) {
                        android.util.Log.e("MainActivity", "Error de permisos: ${e.message}")
                    }
                }
            }

            LaunchedEffect(Unit) {
                lanzadorPermisos.launch(
                    arrayOf(
                        Manifest.permission.CAMERA,
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION
                    )
                )
            }

            AgroScanTheme(darkTheme = viewModel.modoOscuroHabilitado) {
                CompositionLocalProvider(
                    LocalAnimaciones provides viewModel.animacionesHabilitadas,
                    LocalVidrio provides viewModel.vidrioHabilitado
                ) {
                    Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                        Box(modifier = Modifier.padding(innerPadding)) {
                            // Sin sesión (primera vez o tras cerrar sesión) se muestra el login
                            AnimatedContent(
                                targetState = viewModel.usuarioActual == null,
                                transitionSpec = {
                                    if (viewModel.animacionesHabilitadas) {
                                        (fadeIn(tween(450, 100, CurvaSuave)) + scaleIn(tween(450, 100, CurvaSuave), initialScale = 0.96f))
                                            .togetherWith(fadeOut(tween(200)))
                                    } else {
                                        EnterTransition.None togetherWith ExitTransition.None
                                    }
                                },
                                label = "sesion"
                            ) { sinSesion ->
                                if (sinSesion) {
                                    PantallaLogin(viewModel = viewModel)
                                } else {
                                    PantallaPanelControl(
                                        viewModel = viewModel,
                                        initialTab = tabInicial,
                                        solicitudCamara = solicitudCamara
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
