package com.tecsup.agroscan.network

import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET

data class ZonaSyncPayload(
    val nombre: String,
    val cultivo: String,
    val hectareas: Double
)

data class AnalisisSyncPayload(
    val titulo: String,
    val descripcion: String,
    val fecha: String
)

interface ExternalWebApiService {
    @GET("health")
    suspend fun verificarConexion(): Response<Map<String, Any>>

    companion object {
        private const val BASE_URL = "https://tu-api-railway.up.railway.app/"

        fun create(): ExternalWebApiService {
            return Retrofit.Builder()
                .baseUrl(BASE_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .build()
                .create(ExternalWebApiService::class.java)
        }
    }
}
