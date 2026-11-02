package com.tecsup.agroscan.data

import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot

/**
 * Geometría para dibujar parcelas: decide dónde va cada punto nuevo para que el polígono
 * no se cruce consigo mismo, aunque el usuario marque las esquinas en cualquier orden.
 * Trabaja con latitud/longitud en un plano local (suficiente para parcelas de pocos km).
 */
object GeometriaParcela {

    data class Punto(val lat: Double, val lng: Double)

    private data class Plano(val x: Double, val y: Double)

    private fun aPlano(puntos: List<Punto>): List<Plano> {
        if (puntos.isEmpty()) return emptyList()
        val escala = cos(Math.toRadians(puntos.map { it.lat }.average()))
        return puntos.map { Plano(it.lng * escala, it.lat) }
    }

    private fun distancia(a: Plano, b: Plano) = hypot(a.x - b.x, a.y - b.y)

    private fun orientacion(a: Plano, b: Plano, c: Plano): Double =
        (b.x - a.x) * (c.y - a.y) - (b.y - a.y) * (c.x - a.x)

    /** true si los segmentos ab y cd se cruzan en un punto interior de ambos. */
    private fun seCruzan(a: Plano, b: Plano, c: Plano, d: Plano): Boolean {
        val o1 = orientacion(a, b, c)
        val o2 = orientacion(a, b, d)
        val o3 = orientacion(c, d, a)
        val o4 = orientacion(c, d, b)
        return (o1 > 0) != (o2 > 0) && (o3 > 0) != (o4 > 0) &&
                o1 != 0.0 && o2 != 0.0 && o3 != 0.0 && o4 != 0.0
    }

    private fun esSimplePlano(p: List<Plano>): Boolean {
        val n = p.size
        if (n < 4) return true
        for (i in 0 until n) {
            for (j in i + 1 until n) {
                // Bordes vecinos comparten un vértice: no cuentan como cruce
                if (j == i + 1 || (i == 0 && j == n - 1)) continue
                if (seCruzan(p[i], p[(i + 1) % n], p[j], p[(j + 1) % n])) return false
            }
        }
        return true
    }

    /** true si ningún borde del polígono cruza a otro. */
    fun esSimple(puntos: List<Punto>): Boolean = esSimplePlano(aPlano(puntos))

    /**
     * Posición en la que conviene insertar [nuevo]: el borde donde menos aumenta el perímetro
     * sin provocar cruces. Con menos de 3 puntos se añade al final.
     */
    fun indiceDeInsercion(puntos: List<Punto>, nuevo: Punto): Int {
        val n = puntos.size
        if (n < 3) return n
        val plano = aPlano(puntos + nuevo)
        val q = plano.last()
        val base = plano.dropLast(1)

        var mejorSimple: Pair<Int, Double>? = null
        var mejorCualquiera: Pair<Int, Double>? = null
        for (i in 0 until n) {
            val a = base[i]
            val b = base[(i + 1) % n]
            val costo = distancia(a, q) + distancia(q, b) - distancia(a, b)
            val indice = i + 1
            if (mejorCualquiera == null || costo < mejorCualquiera.second) mejorCualquiera = indice to costo
            val candidato = base.toMutableList().apply { add(indice, q) }
            if (esSimplePlano(candidato) && (mejorSimple == null || costo < mejorSimple.second)) {
                mejorSimple = indice to costo
            }
        }
        return (mejorSimple ?: mejorCualquiera!!).first
    }

    /** Reordena los puntos por ángulo alrededor de su centro: deshace cruces en figuras convexas o casi convexas. */
    fun ordenarPorAngulo(puntos: List<Punto>): List<Punto> {
        if (puntos.size < 3) return puntos
        val plano = aPlano(puntos)
        val cx = plano.map { it.x }.average()
        val cy = plano.map { it.y }.average()
        return puntos.indices
            .sortedBy { atan2(plano[it].y - cy, plano[it].x - cx) }
            .map { puntos[it] }
    }
}
