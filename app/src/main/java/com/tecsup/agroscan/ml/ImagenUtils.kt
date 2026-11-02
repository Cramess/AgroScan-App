package com.tecsup.agroscan.ml

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.util.Base64
import java.io.ByteArrayOutputStream

private const val LADO_MAXIMO = 1280

/**
 * Decodifica el JPEG de la cámara reduciéndolo a un tamaño manejable y lo endereza
 * según la rotación que informa CameraX (imageInfo.rotationDegrees).
 */
fun decodificarConRotacion(jpeg: ByteArray, rotacion: Int): Bitmap {
    val limites = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    BitmapFactory.decodeByteArray(jpeg, 0, jpeg.size, limites)

    var muestreo = 1
    while (maxOf(limites.outWidth, limites.outHeight) / (muestreo * 2) >= LADO_MAXIMO) {
        muestreo *= 2
    }

    val opciones = BitmapFactory.Options().apply { inSampleSize = muestreo }
    val original = BitmapFactory.decodeByteArray(jpeg, 0, jpeg.size, opciones)
        ?: throw IllegalArgumentException("No se pudo decodificar la imagen")

    if (rotacion == 0) return original
    val matriz = Matrix().apply { postRotate(rotacion.toFloat()) }
    return Bitmap.createBitmap(original, 0, 0, original.width, original.height, matriz, true)
}

fun bitmapABase64(bitmap: Bitmap): String {
    val stream = ByteArrayOutputStream()
    bitmap.compress(Bitmap.CompressFormat.JPEG, 70, stream)
    return Base64.encodeToString(stream.toByteArray(), Base64.DEFAULT)
}
