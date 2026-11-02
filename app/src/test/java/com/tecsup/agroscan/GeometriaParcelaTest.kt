package com.tecsup.agroscan

import com.tecsup.agroscan.data.GeometriaParcela
import com.tecsup.agroscan.data.GeometriaParcela.Punto
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class GeometriaParcelaTest {

    // Esquinas de un rectángulo pequeño en Ica (unos 100 m x 60 m)
    private val so = Punto(-14.0670, -75.7290)
    private val se = Punto(-14.0670, -75.7280)
    private val ne = Punto(-14.0665, -75.7280)
    private val no = Punto(-14.0665, -75.7290)

    /** Simula los toques del usuario agregando cada punto donde indica el algoritmo. */
    private fun dibujar(vararg toques: Punto): List<Punto> {
        val puntos = mutableListOf<Punto>()
        toques.forEach { puntos.add(GeometriaParcela.indiceDeInsercion(puntos, it), it) }
        return puntos
    }

    @Test
    fun ordenCorrectoSiguePareciendoUnRectangulo() {
        assertTrue(GeometriaParcela.esSimple(dibujar(so, se, ne, no)))
    }

    @Test
    fun ordenEnZigzagNoDeformaLaParcela() {
        // Marcar en diagonal (SO, NE, SE, NO) uniría las esquinas en forma de moño
        assertFalse(GeometriaParcela.esSimple(listOf(so, ne, se, no)))
        assertTrue(GeometriaParcela.esSimple(dibujar(so, ne, se, no)))
    }

    @Test
    fun formaEnLConservaSuConcavidad() {
        // Una L marcada siguiendo el borde no debe convertirse en su envolvente convexa
        val l = listOf(
            Punto(0.0, 0.0), Punto(0.0, 0.003), Punto(0.001, 0.003),
            Punto(0.001, 0.001), Punto(0.003, 0.001), Punto(0.003, 0.0)
        )
        val dibujada = dibujar(*l.toTypedArray())
        assertTrue(GeometriaParcela.esSimple(dibujada))
        assertEquals(6, dibujada.size)
    }

    @Test
    fun ordenarPorAnguloDeshaceUnMono() {
        val mono = listOf(so, ne, se, no)
        assertTrue(GeometriaParcela.esSimple(GeometriaParcela.ordenarPorAngulo(mono)))
    }

    @Test
    fun conPocosPuntosSeAgreganAlFinal() {
        assertEquals(0, GeometriaParcela.indiceDeInsercion(emptyList(), so))
        assertEquals(2, GeometriaParcela.indiceDeInsercion(listOf(so, se), ne))
    }
}
