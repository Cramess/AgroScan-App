package com.tecsup.agroscan

import android.Manifest
import android.os.Bundle
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
import androidx.lifecycle.viewmodel.compose.viewModel
import com.tecsup.agroscan.screen.PantallaPanelControl
import com.tecsup.agroscan.ui.theme.AgroScanTheme
import com.tecsup.agroscan.viewmodel.MainViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val viewModel: MainViewModel = viewModel()
            val context = androidx.compose.ui.platform.LocalContext.current
            val fusedLocationClient = remember { LocationServices.getFusedLocationProviderClient(context) }
            
            // Lanzador para solicitar múltiples permisos
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
                            location?.let {
                                viewModel.obtenerClimaActual(it.latitude, it.longitude)
                            }
                        }
                    } catch (e: SecurityException) {
                        // Manejar excepción
                    }
                }
            }

            // Solicitar permisos al iniciar
            LaunchedEffect(Unit) {
                lanzadorPermisos.launch(
                    arrayOf(
                        Manifest.permission.CAMERA,
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION
                    )
                )
            }

            AgroScanTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    Box(modifier = Modifier.padding(innerPadding)) {
                        PantallaPanelControl(viewModel = viewModel)
                    }
                }
            }
        }
    }
}
