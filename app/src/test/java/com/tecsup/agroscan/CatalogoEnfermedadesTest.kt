package com.tecsup.agroscan

import com.tecsup.agroscan.ml.CatalogoEnfermedades
import org.junit.Assert.assertEquals
import org.junit.Test

class CatalogoEnfermedadesTest {

    private fun nombre(etiqueta: String) = CatalogoEnfermedades.clasificar(etiqueta).nombre

    @Test
    fun etiquetasDelModeloLocal() {
        val esperado = mapOf(
            "12" to "Sin identificar",
            "Apple Scab Leaf" to "Manzana – Sarna",
            "Apple leaf" to "Manzana – Hoja sana",
            "Apple rust leaf" to "Manzana – Roya",
            "Bell_pepper leaf" to "Pimiento – Hoja sana",
            "Bell_pepper leaf spot" to "Pimiento – Mancha bacteriana",
            "Blueberry leaf" to "Arándano – Hoja sana",
            "Cherry leaf" to "Cereza – Hoja sana",
            "Corn Gray leaf spot" to "Maíz – Mancha gris",
            "Corn leaf blight" to "Maíz – Tizón de la hoja",
            "Corn rust leaf" to "Maíz – Roya",
            "Peach leaf" to "Durazno – Hoja sana",
            "Potato leaf" to "Papa – Hoja sana",
            "Potato leaf early blight" to "Papa – Tizón temprano",
            "Potato leaf late blight" to "Papa – Tizón tardío",
            "Raspberry leaf" to "Frambuesa – Hoja sana",
            "Soyabean leaf" to "Soya – Hoja sana",
            "Soybean leaf" to "Soya – Hoja sana",
            "Squash Powdery mildew leaf" to "Zapallo – Oídio",
            "Strawberry leaf" to "Fresa – Hoja sana",
            "Tomato Early blight leaf" to "Tomate – Tizón temprano",
            "Tomato Septoria leaf spot" to "Tomate – Septoriosis",
            "Tomato leaf" to "Tomate – Hoja sana",
            "Tomato leaf bacterial spot" to "Tomate – Mancha bacteriana",
            "Tomato leaf late blight" to "Tomate – Tizón tardío",
            "Tomato leaf mosaic virus" to "Tomate – Virus del mosaico",
            "Tomato leaf yellow virus" to "Tomate – Virus del rizado amarillo",
            "Tomato mold leaf" to "Tomate – Moho de la hoja",
            "Tomato two spotted spider mites leaf" to "Tomate – Arañita roja",
            "grape leaf" to "Uva – Hoja sana",
            "grape leaf black rot" to "Uva – Pudrición negra"
        )
        esperado.forEach { (etiqueta, nombreEsperado) ->
            assertEquals("Etiqueta: $etiqueta", nombreEsperado, nombre(etiqueta))
        }
    }

    @Test
    fun formatosHabitualesDeRoboflow() {
        assertEquals("Tomate – Tizón tardío", nombre("Tomato___Late_blight"))
        assertEquals("Papa – Hoja sana", nombre("Potato___healthy"))
        assertEquals("Tomate – Tizón tardío", nombre("tomate-tizon-tardío"))
        assertEquals("Maíz – Roya", nombre("Roya del maíz"))
    }

    @Test
    fun etiquetaDesconocidaSeConserva() {
        assertEquals("Clorosis", nombre("Clorosis"))
    }
}
