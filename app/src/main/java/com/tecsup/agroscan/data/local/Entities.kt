package com.tecsup.agroscan.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

import androidx.compose.ui.graphics.Color
import com.google.android.gms.maps.model.LatLng
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.tecsup.agroscan.data.InformacionZona
import com.tecsup.agroscan.data.ResultadoAnalisis

@Entity(tableName = "zonas")
data class ZonaEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val nombre: String,
    val cultivo: String,
    val colorArgb: Int,
    val diasParaCosecha: Int,
    val latitud: Double,
    val longitud: Double,
    val hectareas: Double,
    val fechaSiembra: String,
    val observaciones: String,
    val estadoCultivo: String,
    val uriFoto: String?,
    val verticesJson: String // Serializado para Room
)

fun ZonaEntity.toDomain(): InformacionZona {
    val listType = object : TypeToken<List<LatLng>>() {}.type
    val vertices: List<LatLng> = Gson().fromJson(verticesJson, listType)
    return InformacionZona(
        nombre = nombre,
        cultivo = cultivo,
        color = Color(colorArgb),
        diasParaCosecha = diasParaCosecha,
        ubicacion = LatLng(latitud, longitud),
        hectareas = hectareas,
        fechaSiembra = fechaSiembra,
        observaciones = observaciones,
        estadoCultivo = estadoCultivo,
        uriFoto = uriFoto,
        vertices = vertices
    )
}

fun InformacionZona.toEntity(): ZonaEntity {
    return ZonaEntity(
        nombre = nombre,
        cultivo = cultivo,
        colorArgb = color.hashCode(), // Simple conversion
        diasParaCosecha = diasParaCosecha,
        latitud = ubicacion.latitude,
        longitud = ubicacion.longitude,
        hectareas = hectareas,
        fechaSiembra = fechaSiembra,
        observaciones = observaciones,
        estadoCultivo = estadoCultivo,
        uriFoto = uriFoto,
        verticesJson = Gson().toJson(vertices)
    )
}

@Entity(tableName = "historial")
data class HistorialEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
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
    val radiacionUV: String,
    val resumen: String
)

fun HistorialEntity.toDomain(): ResultadoAnalisis {
    return ResultadoAnalisis(
        titulo = titulo,
        nombrePlanta = nombrePlanta,
        nombreZona = nombreZona,
        descripcion = descripcion,
        valorColor = valorColor,
        fecha = fecha,
        hora = hora,
        ubicacion = ubicacion,
        temperatura = temperatura,
        humedad = humedad,
        radiacionUV = radiacionUV,
        resumen = resumen
    )
}

fun ResultadoAnalisis.toEntity(): HistorialEntity {
    return HistorialEntity(
        titulo = titulo,
        nombrePlanta = nombrePlanta,
        nombreZona = nombreZona,
        descripcion = descripcion,
        valorColor = valorColor,
        fecha = fecha,
        hora = hora,
        ubicacion = ubicacion,
        temperatura = temperatura,
        humedad = humedad,
        radiacionUV = radiacionUV,
        resumen = resumen
    )
}
