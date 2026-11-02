package com.tecsup.agroscan

import com.tecsup.agroscan.data.ValidacionCuenta
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ValidacionCuentaTest {

    @Test
    fun aceptaCorreosDeEmpresas() {
        listOf(
            "cristian@agroscan.pe",
            "Aldo.Alva@AgroExport.com.pe",
            "tecnico@tecsup.edu.pe",
            "ana@mail.tecsup.edu.pe",
            "gerente@camposica.com"
        ).forEach { assertTrue(it, ValidacionCuenta.esCorreoCorporativo(it)) }
    }

    @Test
    fun rechazaCorreosPersonales() {
        listOf(
            "cristianrufino@gmail.com",
            "alguien@hotmail.com",
            "alguien@hotmail.com.pe",
            "alguien@outlook.es",
            "alguien@yahoo.es",
            "alguien@icloud.com",
            "alguien@me.com",
            "alguien@mail.com",
            "alguien@proton.me"
        ).forEach { assertFalse(it, ValidacionCuenta.esCorreoCorporativo(it)) }
    }

    @Test
    fun rechazaFormatosInvalidos() {
        listOf("", "sin-arroba.com", "a@b", "a@empresa.", "a b@empresa.com").forEach {
            assertFalse(it, ValidacionCuenta.esCorreoValido(it))
        }
    }

    @Test
    fun validarRegistroDevuelveElPrimerError() {
        assertEquals("Ingrese su nombre completo",
            ValidacionCuenta.validarRegistro(" ", "a@empresa.pe", "secreto1", "secreto1"))
        assertEquals("Use un correo corporativo (no Gmail, Hotmail, Outlook u otro personal)",
            ValidacionCuenta.validarRegistro("Ana", "ana@gmail.com", "secreto1", "secreto1"))
        assertEquals("La contraseña debe tener al menos 6 caracteres",
            ValidacionCuenta.validarRegistro("Ana", "ana@empresa.pe", "123", "123"))
        assertEquals("Las contraseñas no coinciden",
            ValidacionCuenta.validarRegistro("Ana", "ana@empresa.pe", "secreto1", "secreto2"))
        assertNull(ValidacionCuenta.validarRegistro("Ana", "ana@empresa.pe", "secreto1", "secreto1"))
    }
}
