package com.tecsup.agroscan.ml

import android.content.Context
import android.graphics.Bitmap
import android.graphics.RectF
import org.tensorflow.lite.Interpreter
import java.io.Closeable
import java.io.FileInputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.channels.FileChannel

data class Deteccion(
    val clase: String,
    val confianza: Float,
    val caja: RectF // Coordenadas normalizadas (0..1) sobre la imagen
)

/**
 * Detector de enfermedades sin conexión: modelo YOLO11 exportado a TFLite y ejecutado con LiteRT.
 * Entrada RGB float 0..1 en [1, 3, tam, tam] (exportación directa desde PyTorch) o [1, tam, tam, 3];
 * salida [1, 4 + clases, cajas] o su transpuesta, con (cx, cy, w, h, puntajes...) por caja.
 */
class DetectorLocal(context: Context) : Closeable {
    private val interprete: Interpreter
    private val etiquetas: List<String>
    private val tamEntrada: Int
    private val canalesPrimero: Boolean
    private val salidaTranspuesta: Boolean
    private val numClases: Int
    private val numCajas: Int

    init {
        val fd = context.assets.openFd(NOMBRE_MODELO)
        val modelo = FileInputStream(fd.fileDescriptor).channel
            .map(FileChannel.MapMode.READ_ONLY, fd.startOffset, fd.declaredLength)
        interprete = Interpreter(modelo, Interpreter.Options().setNumThreads(4))
        etiquetas = context.assets.open(NOMBRE_ETIQUETAS).bufferedReader()
            .readLines()
            .filter { it.isNotBlank() }

        val formaEntrada = interprete.getInputTensor(0).shape()
        canalesPrimero = formaEntrada[1] == 3
        tamEntrada = if (canalesPrimero) formaEntrada[2] else formaEntrada[1]

        // Siempre hay muchas más cajas (8400) que atributos (4 + clases)
        val formaSalida = interprete.getOutputTensor(0).shape()
        salidaTranspuesta = formaSalida[1] > formaSalida[2]
        numCajas = if (salidaTranspuesta) formaSalida[1] else formaSalida[2]
        numClases = (if (salidaTranspuesta) formaSalida[2] else formaSalida[1]) - 4

        android.util.Log.i(
            "DetectorLocal",
            "Entrada ${formaEntrada.contentToString()}, salida ${formaSalida.contentToString()}"
        )
        if (etiquetas.size != numClases) {
            android.util.Log.w(
                "DetectorLocal",
                "$NOMBRE_ETIQUETAS tiene ${etiquetas.size} etiquetas pero el modelo predice $numClases clases"
            )
        }
    }

    @Synchronized
    fun detectar(bitmap: Bitmap, umbral: Float = 0.25f): List<Deteccion> {
        val escalado = Bitmap.createScaledBitmap(bitmap, tamEntrada, tamEntrada, true)
        val pixeles = IntArray(tamEntrada * tamEntrada)
        escalado.getPixels(pixeles, 0, tamEntrada, 0, 0, tamEntrada, tamEntrada)

        val area = pixeles.size
        val entrada = ByteBuffer.allocateDirect(4 * area * 3).order(ByteOrder.nativeOrder())
        val flotantes = entrada.asFloatBuffer()
        for (i in 0 until area) {
            val p = pixeles[i]
            val r = (p shr 16 and 0xFF) / 255f
            val g = (p shr 8 and 0xFF) / 255f
            val b = (p and 0xFF) / 255f
            if (canalesPrimero) {
                // Planos separados: todo R, luego todo G, luego todo B
                flotantes.put(i, r)
                flotantes.put(area + i, g)
                flotantes.put(2 * area + i, b)
            } else {
                flotantes.put(i * 3, r)
                flotantes.put(i * 3 + 1, g)
                flotantes.put(i * 3 + 2, b)
            }
        }

        val bufferSalida = ByteBuffer.allocateDirect(interprete.getOutputTensor(0).numBytes())
            .order(ByteOrder.nativeOrder())
        interprete.run(entrada, bufferSalida)
        bufferSalida.rewind()
        val s = bufferSalida.asFloatBuffer()
        val atributos = 4 + numClases
        fun valor(atributo: Int, caja: Int): Float =
            if (salidaTranspuesta) s.get(caja * atributos + atributo) else s.get(atributo * numCajas + caja)

        // Ultralytics suele normalizar las cajas al exportar a TFLite; si vinieran en píxeles, se normalizan aquí
        val escala = if ((0 until numCajas).any { valor(2, it) > 2f }) tamEntrada.toFloat() else 1f

        val candidatos = mutableListOf<Deteccion>()
        for (i in 0 until numCajas) {
            var mejor = 0
            var conf = 0f
            for (c in 0 until numClases) {
                val v = valor(4 + c, i)
                if (v > conf) {
                    conf = v
                    mejor = c
                }
            }
            if (conf < umbral) continue

            val cx = valor(0, i) / escala
            val cy = valor(1, i) / escala
            val w = valor(2, i) / escala
            val h = valor(3, i) / escala
            candidatos += Deteccion(
                clase = etiquetas.getOrElse(mejor) { "Clase $mejor" },
                confianza = conf,
                caja = RectF(cx - w / 2, cy - h / 2, cx + w / 2, cy + h / 2)
            )
        }
        return nms(candidatos)
    }

    private fun nms(detecciones: List<Deteccion>, iouMax: Float = 0.45f): List<Deteccion> {
        val pendientes = detecciones.sortedByDescending { it.confianza }.toMutableList()
        val resultado = mutableListOf<Deteccion>()
        while (pendientes.isNotEmpty()) {
            val mejor = pendientes.removeAt(0)
            resultado += mejor
            pendientes.removeAll { it.clase == mejor.clase && iou(it.caja, mejor.caja) > iouMax }
        }
        return resultado
    }

    private fun iou(a: RectF, b: RectF): Float {
        val interseccion = maxOf(0f, minOf(a.right, b.right) - maxOf(a.left, b.left)) *
                maxOf(0f, minOf(a.bottom, b.bottom) - maxOf(a.top, b.top))
        val union = a.width() * a.height() + b.width() * b.height() - interseccion
        return if (union <= 0f) 0f else interseccion / union
    }

    override fun close() = interprete.close()

    companion object {
        const val NOMBRE_MODELO = "modelo_plantas.tflite"
        const val NOMBRE_ETIQUETAS = "labels.txt"
    }
}
