package com.tecsup.agroscan.network

import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.RequestBody
import okhttp3.Response
import retrofit2.Retrofit
import java.io.IOException
import java.net.UnknownHostException
import java.util.concurrent.TimeUnit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Path

interface RoboflowApiService {
    @POST("{project}/{version}")
    suspend fun detectDisease(
        @Path("project") project: String,
        @Path("version") version: String,
        @Header("Authorization") authHeader: String,
        @Body imageBase64: RequestBody
    ): RoboflowResponse

    companion object {
        private const val BASE_URL = "https://serverless.roboflow.com/"

        fun create(): RoboflowApiService {
            val cliente = OkHttpClient.Builder()
                .connectTimeout(10, TimeUnit.SECONDS)
                .writeTimeout(20, TimeUnit.SECONDS)
                .readTimeout(20, TimeUnit.SECONDS)
                .addInterceptor(ReintentoInterceptor(maxReintentos = 2))
                .build()

            return Retrofit.Builder()
                .baseUrl(BASE_URL)
                .client(cliente)
                .addConverterFactory(GsonConverterFactory.create())
                .build()
                .create(RoboflowApiService::class.java)
        }
    }
}

/**
 * Reintenta fallos transitorios (timeouts, 429 y 5xx) con espera creciente.
 * Sin red (UnknownHostException) no reintenta, para pasar enseguida al modelo local.
 */
private class ReintentoInterceptor(private val maxReintentos: Int) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        var intento = 0
        while (true) {
            try {
                val respuesta = chain.proceed(chain.request())
                val transitorio = respuesta.code == 429 || respuesta.code >= 500
                if (!transitorio || intento >= maxReintentos) return respuesta
                respuesta.close()
            } catch (e: UnknownHostException) {
                throw e
            } catch (e: IOException) {
                if (intento >= maxReintentos || chain.call().isCanceled()) throw e
            }
            intento++
            Thread.sleep(500L * intento)
        }
    }
}

data class RoboflowResponse(
    val predictions: List<Prediction>
)

data class Prediction(
    val x: Double,
    val y: Double,
    val width: Double,
    val height: Double,
    val confidence: Double,
    @com.google.gson.annotations.SerializedName("class") val className: String,
    val image_path: String? = null
)
