package com.tecsup.agroscan.data

import androidx.compose.ui.graphics.Color
import com.google.android.gms.maps.model.LatLng

data class InformacionZona(
    val nombre: String,
    val cultivo: String,
    val color: Color,
    val diasParaCosecha: Int,
    val ubicacion: LatLng,
    val hectareas: Double,
    val fechaSiembra: String = "",
    val observaciones: String = "",
    val estadoCultivo: String = "Pendiente",
    val uriFoto: String? = null,
    val vertices: List<LatLng> = emptyList(), // Nuevos datos para el polígono
    val tipoSuelo: String = "Franco-Arcilloso",
    val phSuelo: Double = 6.8,
    val nivelNitrogeno: String = "Medio",
    val nivelFosforo: String = "Medio",
    val nivelPotasio: String = "Medio"
)

data class DatosClima(
    val ubicacion: String,
    val temperatura: String,
    val humedad: String,
    val viento: String,
    val uv: String,
    val lluvia: String
)

data class ResultadoAnalisis(
    val titulo: String,
    val nombrePlanta: String,
    val nombreZona: String,
    val descripcion: String,
    val valorColor: Long,
    val fecha: String,
    val hora: String,
    val ubicacion: String,
    val temperatura: String,
    val humedad: String,
    val radiacionUV: String, // Nuevo campo
    val resumen: String,
    val estadoValidacion: String = "PENDIENTE", // PENDIENTE, VALIDADO_OK, CORREGIDO_AGRONOMO
    val observacionTecnico: String = "",
    val esDeficienciaNutricional: Boolean = false
)

data class Usuario(
    val nombre: String,
    val email: String,
    val rol: String
)
