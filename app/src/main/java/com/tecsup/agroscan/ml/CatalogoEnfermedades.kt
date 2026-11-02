package com.tecsup.agroscan.ml

import java.text.Normalizer

data class Enfermedad(
    val cultivo: String,   // Vacío si la etiqueta no menciona un cultivo conocido
    val condicion: String,
    val sana: Boolean
) {
    val nombre: String get() = if (cultivo.isEmpty()) condicion else "$cultivo – $condicion"
}

/**
 * Catálogo común para que el modelo de Roboflow (nube) y el YOLO11 local hablen el mismo idioma.
 * Cada etiqueta cruda se traduce a (cultivo, condición) en español mediante palabras clave en inglés
 * o español. Si una etiqueta concreta se resuelve mal, añádela en [alias] con su forma normalizada.
 */
object CatalogoEnfermedades {

    // Etiquetas exactas (normalizadas: minúsculas, sin tildes, "_" y "-" como espacios)
    private val alias: Map<String, Enfermedad> = mapOf(
        // Clase 0 de plant-disease-3: etiqueta residual del dataset, sin significado agronómico
        "12" to Enfermedad("", "Sin identificar", sana = false),
        "bell pepper leaf spot" to Enfermedad("Pimiento", "Mancha bacteriana", sana = false)
    )

    // El orden importa: los términos más específicos van primero
    private val cultivos: List<Pair<List<String>, String>> = listOf(
        listOf("tomato", "tomate", "jitomate") to "Tomate",
        listOf("potato", "papa", "patata") to "Papa",
        listOf("corn", "maize", "maiz") to "Maíz",
        listOf("apple", "manzana") to "Manzana",
        listOf("grape", "uva", "vid") to "Uva",
        listOf("bell pepper", "pepper", "pimiento", "aji") to "Pimiento",
        listOf("soyabean", "soybean", "soya", "soja") to "Soya",
        listOf("squash", "zapallo", "calabaza") to "Zapallo",
        listOf("strawberry", "fresa", "frutilla") to "Fresa",
        listOf("peach", "durazno", "melocoton") to "Durazno",
        listOf("cherry", "cereza") to "Cereza",
        listOf("blueberry", "arandano") to "Arándano",
        listOf("raspberry", "frambuesa") to "Frambuesa"
    )

    private val condiciones: List<Pair<List<String>, String>> = listOf(
        listOf("late blight", "tizon tardio", "rancha") to "Tizón tardío",
        listOf("early blight", "tizon temprano", "alternaria") to "Tizón temprano",
        listOf("gray leaf spot", "grey leaf spot", "mancha gris", "cercospora") to "Mancha gris",
        listOf("bacterial spot", "mancha bacteriana") to "Mancha bacteriana",
        listOf("septoria") to "Septoriosis",
        listOf("mosaic", "mosaico") to "Virus del mosaico",
        listOf("yellow", "curl", "rizado", "amarill") to "Virus del rizado amarillo",
        listOf("spider mite", "mite", "aranita", "arana", "acaro") to "Arañita roja",
        listOf("black rot", "pudricion negra") to "Pudrición negra",
        listOf("downy", "mildiu") to "Mildiu",
        listOf("powdery", "mildew", "oidio") to "Oídio",
        listOf("mold", "mould", "moho") to "Moho de la hoja",
        listOf("rust", "roya") to "Roya",
        listOf("scab", "sarna") to "Sarna",
        listOf("blight", "tizon") to "Tizón de la hoja",
        listOf("spot", "mancha") to "Mancha foliar"
    )

    private val terminosSana = listOf("healthy", "sana", "sano", "saludable")
    private val terminosHoja = listOf("leaf", "leaves", "hoja")

    fun clasificar(etiqueta: String): Enfermedad {
        val texto = normalizar(etiqueta)
        alias[texto]?.let { return it }

        val cultivo = buscar(texto, cultivos) ?: ""
        val condicion = buscar(texto, condiciones)

        return when {
            condicion != null -> Enfermedad(cultivo, condicion, sana = false)
            // En PlantDoc una etiqueta como "Tomato leaf" (cultivo + hoja, sin enfermedad) significa hoja sana
            contiene(texto, terminosSana) || (cultivo.isNotEmpty() && contiene(texto, terminosHoja)) ->
                Enfermedad(cultivo, "Hoja sana", sana = true)
            else -> Enfermedad(cultivo, etiqueta.trim(), sana = false)
        }
    }

    private fun buscar(texto: String, tabla: List<Pair<List<String>, String>>): String? =
        tabla.firstOrNull { (claves, _) -> contiene(texto, claves) }?.second

    private fun contiene(texto: String, claves: List<String>): Boolean =
        claves.any { Regex("\\b" + Regex.escape(it)).containsMatchIn(texto) }

    private fun normalizar(etiqueta: String): String =
        Normalizer.normalize(etiqueta, Normalizer.Form.NFD)
            .replace(Regex("\\p{M}+"), "")
            .lowercase()
            .replace(Regex("[_\\-]+"), " ")
            .replace(Regex("\\s+"), " ")
            .trim()
}
