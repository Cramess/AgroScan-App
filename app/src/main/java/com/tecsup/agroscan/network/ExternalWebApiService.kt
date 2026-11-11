package com.tecsup.agroscan.network

import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
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
        /**
         * Crea el cliente para la URL de WEB_API_URL (local.properties).
         * Devuelve null si no está configurada o no es una URL http(s) válida.
         */
        fun create(urlBase: String): ExternalWebApiService? {
            val url = urlBase.trim().let { if (it.endsWith("/")) it else "$it/" }
            if (url.toHttpUrlOrNull() == null) return null
            return Retrofit.Builder()
                .baseUrl(url)
                .addConverterFactory(GsonConverterFactory.create())
                .build()
                .create(ExternalWebApiService::class.java)
        }
    }
}
