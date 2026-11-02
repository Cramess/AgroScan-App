package com.tecsup.agroscan.Services

data class AlertaAgroclimatica(
    val titulo: String,
    val patogenoAsociado: String,
    val descripcion: String,
    val recomendaciones: String
)

object WeatherAlertEngine {
    fun evaluarRiesgo(temperatura: Double, humedad: Int): List<AlertaAgroclimatica> {
        val alertas = mutableListOf<AlertaAgroclimatica>()
        if (temperatura > 26.0 && humedad > 75) {
            alertas.add(
                AlertaAgroclimatica(
                    titulo = "Alto Riesgo de Roya / Mildiu",
                    patogenoAsociado = "Hongos fitopatógenos",
                    descripcion = "Temperatura y humedad elevadas favorecen la proliferación de esporas.",
                    recomendaciones = "Realizar aplicaciones preventivas de fungicida sistémico y revisar ventilación en follaje."
                )
            )
        }
        if (temperatura < 12.0) {
            alertas.add(
                AlertaAgroclimatica(
                    titulo = "Alerta de Helada / Estrés Térmico",
                    patogenoAsociado = "Bajas temperaturas",
                    descripcion = "Temperatura por debajo del umbral óptimo para el cultivo.",
                    recomendaciones = "Considerar riego nocturno o cubiertas protectoras si aplica."
                )
            )
        }
        return alertas
    }
}
