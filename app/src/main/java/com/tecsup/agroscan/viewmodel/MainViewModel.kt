package com.tecsup.agroscan.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import android.app.Application
import com.tecsup.agroscan.data.local.AppDatabase
import com.tecsup.agroscan.data.local.toDomain
import com.tecsup.agroscan.data.local.toEntity
import com.tecsup.agroscan.Services.WeatherApiService
import androidx.lifecycle.viewModelScope
import com.google.android.gms.maps.model.LatLng
import com.tecsup.agroscan.data.ResultadoAnalisis
import com.tecsup.agroscan.data.DatosClima
import com.tecsup.agroscan.data.InformacionZona
import com.tecsup.agroscan.data.Usuario
import com.tecsup.agroscan.network.RoboflowApiService
import com.google.maps.android.PolyUtil
import com.google.maps.android.SphericalUtil
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody
import java.text.SimpleDateFormat
import java.util.*

class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val database = AppDatabase.getDatabase(application)
    private val agroScanDao = database.agroScanDao()
    private val weatherService = WeatherApiService.create()

    var permisoUbicacionConcedido by mutableStateOf(false)
    var permisoCamaraConcedido by mutableStateOf(false)
    var ciudadUsuario by mutableStateOf("Lima, Perú")

    // Estado para el dibujo de parcelas
    var puntosEdicion = mutableStateListOf<LatLng>()
    
    fun calcularHectareas(puntos: List<LatLng>): Double {
        if (puntos.size < 3) return 0.0
        val areaMetros = SphericalUtil.computeArea(puntos)
        return areaMetros / 10000.0
    }

    /**
     * Detecta si una ubicación está dentro de los límites de alguna zona guardada.
     */
    fun detectarZonaPorUbicacion(ubicacion: LatLng): InformacionZona? {
        return zonas.find { zona ->
            zona.vertices.size >= 3 && PolyUtil.containsLocation(ubicacion, zona.vertices, true)
        }
    }

    fun actualizarEstadoCamara(concedido: Boolean) {
        permisoCamaraConcedido = concedido
    }

    fun actualizarEstadoUbicacion(concedido: Boolean) {
        permisoUbicacionConcedido = concedido
    }
    
    // Perfil de Usuario
    var usuarioActual by mutableStateOf<Usuario?>(
        Usuario("Cristian", "cristian@agroscan.com", "ADMINISTRADOR")
    )
    
    // Configuración de Tema
    var modoOscuroHabilitado by mutableStateOf(false)

    val zonas = mutableStateListOf<InformacionZona>()

    var datosClimaReales by mutableStateOf<DatosClima?>(null)

    val historialAnalisis = mutableStateListOf<ResultadoAnalisis>()

    init {
        cargarDatosDesdeRoom()
    }

    private fun cargarDatosDesdeRoom() {
        viewModelScope.launch {
            val zonasLocal = agroScanDao.obtenerTodasLasZonas()
            zonas.clear()
            zonas.addAll(zonasLocal.map { it.toDomain() })

            val historialLocal = agroScanDao.obtenerTodoElHistorial()
            historialAnalisis.clear()
            historialAnalisis.addAll(historialLocal.map { it.toDomain() })
        }
    }

    fun obtenerClimaActual(lat: Double, lon: Double) {
        android.util.Log.d("MainViewModel", "Obteniendo clima para: $lat, $lon")
        viewModelScope.launch {
            try {
                val response = weatherService.getClima(lat, lon, "0ba1ef3472a03fcf2f7edbc8b5b8e59c")
                android.util.Log.d("MainViewModel", "Respuesta clima: ${response.name}, ${response.main.temp}")
                datosClimaReales = DatosClima(
                    ubicacion = response.name,
                    temperatura = "${response.main.temp.toInt()}°C",
                    humedad = "${response.main.humidity}%",
                    viento = "${response.wind.speed}km/h",
                    uv = "UV --", // La API gratuita no siempre da UV
                    lluvia = "0.0mm"
                )
                ciudadUsuario = response.name
            } catch (e: Exception) {
                android.util.Log.e("MainViewModel", "Error al obtener clima: ${e.message}")
            }
        }
    }

    private val apiService = RoboflowApiService.create()

    fun agregarZona(zona: InformacionZona) {
        viewModelScope.launch {
            agroScanDao.insertarZona(zona.toEntity())
            zonas.add(zona)
            
            // Crear entrada inicial en el historial
            val sdfFecha = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
            val sdfHora = SimpleDateFormat("HH:mm", Locale.getDefault())
            val ahora = Date()
            
            val resultado = ResultadoAnalisis(
                titulo = "Registro de Zona",
                nombrePlanta = zona.cultivo,
                nombreZona = zona.nombre,
                descripcion = "Zona recién añadida",
                valorColor = 0xFF9E9E9E, // Gris neutro
                fecha = sdfFecha.format(ahora),
                hora = sdfHora.format(ahora),
                ubicacion = ciudadUsuario,
                temperatura = datosClimaReales?.temperatura ?: "--",
                humedad = datosClimaReales?.humedad ?: "--",
                radiacionUV = datosClimaReales?.uv ?: "--",
                resumen = "Sin cambios detectados. La zona ha sido registrada exitosamente en el sistema."
            )
            
            agroScanDao.insertarHistorial(resultado.toEntity())
            historialAnalisis.add(0, resultado)
        }
    }

    fun eliminarZona(zona: InformacionZona) {
        viewModelScope.launch {
            val zonasLocal = agroScanDao.obtenerTodasLasZonas()
            val entity = zonasLocal.find { it.nombre == zona.nombre }
            if (entity != null) {
                agroScanDao.eliminarZona(entity)
                agroScanDao.eliminarHistorialPorZona(zona.nombre)
                zonas.remove(zona)
                historialAnalisis.removeAll { it.nombreZona == zona.nombre }
            }
        }
    }

    fun actualizarZona(zonaAntigua: InformacionZona, zonaNueva: InformacionZona) {
        viewModelScope.launch {
            val zonasLocal = agroScanDao.obtenerTodasLasZonas()
            val entity = zonasLocal.find { it.nombre == zonaAntigua.nombre }
            if (entity != null) {
                // Actualizamos la entidad con los nuevos datos pero conservando el mismo ID
                val nuevaEntity = zonaNueva.toEntity().copy(id = entity.id)
                agroScanDao.insertarZona(nuevaEntity)
                
                // Actualizamos la lista en memoria
                val indice = zonas.indexOf(zonaAntigua)
                if (indice != -1) {
                    zonas[indice] = zonaNueva
                }
            }
        }
    }

    fun alternarTema(habilitado: Boolean) {
        modoOscuroHabilitado = habilitado
    }

    fun cerrarSesion() {
        usuarioActual = null
    }

    fun analizarImagenPlanta(
        base64Image: String, 
        zonaSeleccionada: InformacionZona? = null,
        onResult: (String) -> Unit
    ) {
        viewModelScope.launch {
            try {
                val mediaType = "text/plain".toMediaTypeOrNull()
                val requestBody = base64Image.toRequestBody(mediaType)
                
                val response = apiService.detectDisease(
                    project = "deteccion-de-enfermedades-sbaoa",
                    version = "19",
                    authHeader = "Bearer 0eETUshIouQdAiHdxL83",
                    imageBase64 = requestBody
                )

                if (response.predictions.isNotEmpty()) {
                    val mejorPrediccion = response.predictions.maxByOrNull { it.confidence }
                    val porcentaje = ((mejorPrediccion?.confidence ?: 0.0) * 100).toInt()
                    val resultadoText = "Detectado: ${mejorPrediccion?.className} ($porcentaje%)"
                    onResult(resultadoText)
                    
                    val resultado = ResultadoAnalisis(
                        titulo = "Análisis IA",
                        nombrePlanta = zonaSeleccionada?.cultivo ?: "Planta Detectada",
                        nombreZona = zonaSeleccionada?.nombre ?: "Escaneo Rápido",
                        descripcion = mejorPrediccion?.className ?: "Desconocido",
                        valorColor = 0xFF2ECC71,
                        fecha = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date()),
                        hora = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date()),
                        ubicacion = ciudadUsuario,
                        temperatura = datosClimaReales?.temperatura ?: "--",
                        humedad = datosClimaReales?.humedad ?: "--",
                        radiacionUV = datosClimaReales?.uv ?: "--",
                        resumen = "Análisis automático realizado vía cámara."
                    )
                    
                    agroScanDao.insertarHistorial(resultado.toEntity())
                    historialAnalisis.add(0, resultado)
                } else {
                    onResult("No se detectaron problemas en la imagen.")
                }
            } catch (e: Exception) {
                onResult("Error: ${e.message}")
            }
        }
    }
}
