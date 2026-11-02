package com.tecsup.agroscan.data

/**
 * Reglas para crear cuentas: solo se aceptan correos corporativos (dominio de una empresa o institución),
 * no los de proveedores personales como Gmail u Outlook.
 */
object ValidacionCuenta {

    // Proveedores personales: se reconocen por la primera parte del dominio (gmail.com, hotmail.com.pe, yahoo.es...)
    private val PROVEEDORES_PERSONALES = setOf(
        "gmail", "googlemail", "hotmail", "outlook", "live", "msn", "yahoo", "ymail",
        "icloud", "aol", "protonmail", "proton", "gmx", "zoho", "yandex"
    )

    // Dominios personales cuya primera parte también podría ser parte de un dominio corporativo
    private val DOMINIOS_PERSONALES = setOf("me.com", "mac.com", "mail.com")

    private val FORMATO = Regex("^[a-z0-9._%+-]+@[a-z0-9-]+(\\.[a-z0-9-]+)*\\.[a-z]{2,}$")

    fun normalizar(correo: String): String = correo.trim().lowercase()

    fun esCorreoValido(correo: String): Boolean = FORMATO.matches(normalizar(correo))

    fun esCorreoCorporativo(correo: String): Boolean {
        val limpio = normalizar(correo)
        if (!esCorreoValido(limpio)) return false
        val dominio = limpio.substringAfter("@")
        if (dominio in DOMINIOS_PERSONALES) return false
        val partes = dominio.split(".")
        // "hotmail.com.pe" es personal; "mail.tecsup.edu.pe" (4 partes) es institucional
        return !(partes.first() in PROVEEDORES_PERSONALES && partes.size <= 3)
    }

    /** Devuelve el primer error del formulario de registro, o null si todo es correcto. */
    fun validarRegistro(nombre: String, correo: String, contrasena: String, confirmacion: String): String? = when {
        nombre.isBlank() -> "Ingrese su nombre completo"
        !esCorreoValido(correo) -> "Correo electrónico no válido"
        !esCorreoCorporativo(correo) -> "Use un correo corporativo (no Gmail, Hotmail, Outlook u otro personal)"
        contrasena.length < 6 -> "La contraseña debe tener al menos 6 caracteres"
        contrasena != confirmacion -> "Las contraseñas no coinciden"
        else -> null
    }
}
