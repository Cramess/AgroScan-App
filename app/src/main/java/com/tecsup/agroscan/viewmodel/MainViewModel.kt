package com.tecsup.agroscan.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import android.app.Application
import com.tecsup.agroscan.data.local.AppDatabase
import com.tecsup.agroscan.data.local.toDomain
import com.tecsup.agroscan.data.local.toEntity
import com.tecsup.agroscan.Services.WeatherApiService
import com.tecsup.agroscan.Services.WeatherAlertEngine
import com.tecsup.agroscan.Services.AlertaAgroclimatica
import com.tecsup.agroscan.network.ExternalWebApiService
import com.tecsup.agroscan.network.ZonaSyncPayload
import com.tecsup.agroscan.network.AnalisisSyncPayload
import androidx.lifecycle.viewModelScope
import com.google.android.gms.maps.model.LatLng
import com.tecsup.agroscan.data.ResultadoAnalisis
import com.tecsup.agroscan.data.DatosClima
import com.tecsup.agroscan.data.InformacionZona
import com.tecsup.agroscan.data.Usuario
import com.tecsup.agroscan.network.RoboflowApiService
import com.tecsup.agroscan.BuildConfig
import com.tecsup.agroscan.widget.AgroScanWidget
import com.tecsup.agroscan.widget.PREFS_WIDGET
import com.tecsup.agroscan.widget.PREF_CLIMA_ALERTA
import com.tecsup.agroscan.widget.PREF_CLIMA_HUMEDAD
import com.tecsup.agroscan.widget.PREF_CLIMA_TEMPERATURA
import androidx.glance.appwidget.updateAll
import android.content.Context
import com.tecsup.agroscan.data.ValidacionCuenta
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.tecsup.agroscan.ml.CatalogoEnfermedades
import com.tecsup.agroscan.ml.DetectorLocal
import com.tecsup.agroscan.ml.Enfermedad
import com.tecsup.agroscan.ml.bitmapABase64
import com.tecsup.agroscan.ml.decodificarConRotacion
import android.graphics.Bitmap
import com.google.maps.android.PolyUtil
import com.google.maps.android.SphericalUtil
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.roundToInt

enum class ModoDeteccion(val etiqueta: String) {
    AUTOMATICO("Automático"), // Roboflow y, si falla (sin internet), modelo local
    NUBE("Nube"),
    LOCAL("Local")
}

private data class Diagnostico(val clase: String, val confianza: Float, val origen: String)

/** Cuenta guardada en el teléfono; la contraseña solo se conserva como hash SHA-256 con sal. */
private data class CuentaLocal(
    val nombre: String,
    val correo: String,
    val rol: String,
    val sal: String,
    val hash: String,
    val correoAdministrador: String? = null   // a quién escribir consultas u observaciones
)

const val ROL_TECNICO = "TÉCNICO AGRÓNOMO"

private const val PREF_ANIMACIONES = "animaciones"
private const val PREF_MODO_OSCURO = "modo_oscuro"
private const val PREF_VIDRIO = "efecto_vidrio"

private const val SESION_ACTIVA = "sesion_activa"
private const val SESION_CUENTAS = "cuentas"
private const val SESION_CORREO_ACTIVO = "correo_activo"
// Claves de la versión con una sola cuenta (solo se leen para migrar)
private const val SESION_NOMBRE = "nombre"
private const val SESION_CORREO = "correo"
private const val SESION_ROL = "rol"
private const val SESION_SAL = "sal"
private const val SESION_HASH = "hash_contrasena"

/** Resultado de un escaneo listo para mostrar; [enfermedad] es null cuando solo hay un mensaje (sin detección o error). */
data class ResultadoEscaneo(
    val enfermedad: Enfermedad?,
    val porcentaje: Int = 0,
    val etiquetaModelo: String = "",
    val origen: String = "",
    val mensaje: String? = null
) {
    companion object {
        fun mensaje(texto: String) = ResultadoEscaneo(enfermedad = null, mensaje = texto)
    }
}

class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val database = AppDatabase.getDatabase(application)
    private val agroScanDao = database.agroScanDao()
    private val weatherService = WeatherApiService.create()
    private val webApiService = ExternalWebApiService.create(BuildConfig.WEB_API_URL)

    var permisoUbicacionConcedido by mutableStateOf(false)
    var permisoCamaraConcedido by mutableStateOf(false)
    var ciudadUsuario by mutableStateOf("Lima, Perú")

    var puntosEdicion = mutableStateListOf<LatLng>()
    
    val alertasClimaticasActivas = mutableStateListOf<AlertaAgroclimatica>()
    
    var estaSincronizandoWeb by mutableStateOf(false)
    var mensajeEstadoSincronizacion by mutableStateOf<String?>(null)

    fun calcularHectareas(puntos: List<LatLng>): Double {
        if (puntos.size < 3) return 0.0
        val areaMetros = SphericalUtil.computeArea(puntos)
        return areaMetros / 10000.0
    }

    fun detectarZonaPorUbicacion(ubicacion: LatLng): InformacionZona? {
        return zonas.find { zona ->
            zona.vertices.size >= 3 && PolyUtil.containsLocation(ubicacion, zona.vertices, true)
        }
    }

    fun actualizarEstadoCamara(concedido: Boolean) {
        permisoCamaraConcedido = concedido
    }

    fun actualizarEstadoUbicacion(concedido: Boolean) {
        permisoUbicacionConcedido = concedido
    }
    
    // Perfil de Usuario: cuenta local guardada en el teléfono (no hay servidor de autenticación)
    private val prefsSesion = application.getSharedPreferences("agroscan_sesion", Context.MODE_PRIVATE)

    var usuarioActual by mutableStateOf(run {
        migrarCuentaAntigua()
        cargarSesion()
    })

    private fun leerCuentas(): List<CuentaLocal> {
        val json = prefsSesion.getString(SESION_CUENTAS, null) ?: return emptyList()
        val tipo = object : TypeToken<List<CuentaLocal>>() {}.type
        return Gson().fromJson<List<CuentaLocal>>(json, tipo) ?: emptyList()
    }

    private fun guardarCuentas(cuentas: List<CuentaLocal>) {
        prefsSesion.edit().putString(SESION_CUENTAS, Gson().toJson(cuentas)).apply()
    }

    // Versiones anteriores guardaban una sola cuenta en claves sueltas; se pasa a la lista de cuentas
    private fun migrarCuentaAntigua() {
        if (prefsSesion.contains(SESION_CUENTAS)) return
        val correo = prefsSesion.getString(SESION_CORREO, null) ?: return
        val cuenta = CuentaLocal(
            nombre = prefsSesion.getString(SESION_NOMBRE, "").orEmpty(),
            correo = correo,
            rol = prefsSesion.getString(SESION_ROL, "ADMINISTRADOR").orEmpty(),
            sal = prefsSesion.getString(SESION_SAL, "").orEmpty(),
            hash = prefsSesion.getString(SESION_HASH, "").orEmpty()
        )
        guardarCuentas(listOf(cuenta))
        prefsSesion.edit()
            .putString(SESION_CORREO_ACTIVO, correo)
            .remove(SESION_NOMBRE).remove(SESION_CORREO).remove(SESION_ROL).remove(SESION_SAL).remove(SESION_HASH)
            .apply()
    }

    private fun cargarSesion(): Usuario? {
        if (!prefsSesion.getBoolean(SESION_ACTIVA, false)) return null
        val correo = prefsSesion.getString(SESION_CORREO_ACTIVO, null) ?: return null
        val cuenta = leerCuentas().find { it.correo == correo } ?: return null
        return Usuario(nombre = cuenta.nombre, email = cuenta.correo, rol = cuenta.rol)
    }

    private fun abrirSesion(correo: String) {
        prefsSesion.edit().putString(SESION_CORREO_ACTIVO, correo).putBoolean(SESION_ACTIVA, true).apply()
        usuarioActual = cargarSesion()
    }

    /**
     * Crea una cuenta de técnico agrónomo (el usuario principal de la app) con correo corporativo y abre su sesión.
     * Devuelve un mensaje de error o null si todo fue bien.
     */
    fun registrarCuenta(nombre: String, correo: String, contrasena: String, confirmacion: String): String? {
        ValidacionCuenta.validarRegistro(nombre, correo, contrasena, confirmacion)?.let { return it }
        val correoLimpio = ValidacionCuenta.normalizar(correo)
        val cuentas = leerCuentas()
        if (cuentas.any { it.correo == correoLimpio }) return "Ya existe una cuenta con ese correo"

        val sal = ByteArray(16).also { java.security.SecureRandom().nextBytes(it) }
        val nueva = CuentaLocal(
            nombre = nombre.trim(),
            correo = correoLimpio,
            rol = ROL_TECNICO,
            sal = android.util.Base64.encodeToString(sal, android.util.Base64.NO_WRAP),
            hash = hashContrasena(contrasena, sal)
        )
        guardarCuentas(cuentas + nueva)
        abrirSesion(correoLimpio)
        return null
    }

    /** Valida las credenciales contra las cuentas del teléfono. Devuelve un mensaje de error o null si todo fue bien. */
    fun iniciarSesion(correo: String, contrasena: String): String? {
        val cuenta = leerCuentas().find { it.correo == ValidacionCuenta.normalizar(correo) }
            ?: return "No hay una cuenta con ese correo. Toque «Crear cuenta»."
        val sal = android.util.Base64.decode(cuenta.sal, android.util.Base64.NO_WRAP)
        if (hashContrasena(contrasena, sal) != cuenta.hash) return "Correo o contraseña incorrectos"

        abrirSesion(cuenta.correo)
        return null
    }

    private fun cuentaActiva(): CuentaLocal? {
        val correo = prefsSesion.getString(SESION_CORREO_ACTIVO, null) ?: return null
        return leerCuentas().find { it.correo == correo }
    }

    private fun reemplazarCuentaActiva(nueva: CuentaLocal) {
        val actual = cuentaActiva() ?: return
        guardarCuentas(leerCuentas().map { if (it.correo == actual.correo) nueva else it })
        abrirSesion(nueva.correo)
    }

    /** Cambia nombre y correo de la cuenta activa. Devuelve un mensaje de error o null si todo fue bien. */
    fun actualizarDatos(nombre: String, correo: String): String? {
        val actual = cuentaActiva() ?: return "No hay una sesión activa"
        val correoLimpio = ValidacionCuenta.normalizar(correo)
        when {
            nombre.isBlank() -> return "Ingrese su nombre completo"
            !ValidacionCuenta.esCorreoValido(correoLimpio) -> return "Correo electrónico no válido"
        }
        if (correoLimpio != actual.correo) {
            if (!ValidacionCuenta.esCorreoCorporativo(correoLimpio)) {
                return "Use un correo corporativo (no Gmail, Hotmail, Outlook u otro personal)"
            }
            if (leerCuentas().any { it.correo == correoLimpio }) return "Ya existe una cuenta con ese correo"
        }
        reemplazarCuentaActiva(actual.copy(nombre = nombre.trim(), correo = correoLimpio))
        return null
    }

    /** Cambia la contraseña verificando la actual. Devuelve un mensaje de error o null si todo fue bien. */
    fun cambiarContrasena(actual: String, nueva: String, confirmacion: String): String? {
        val cuenta = cuentaActiva() ?: return "No hay una sesión activa"
        val salActual = android.util.Base64.decode(cuenta.sal, android.util.Base64.NO_WRAP)
        when {
            hashContrasena(actual, salActual) != cuenta.hash -> return "La contraseña actual no es correcta"
            nueva.length < 6 -> return "La nueva contraseña debe tener al menos 6 caracteres"
            nueva != confirmacion -> return "Las contraseñas no coinciden"
            nueva == actual -> return "La nueva contraseña debe ser distinta de la actual"
        }
        val sal = ByteArray(16).also { java.security.SecureRandom().nextBytes(it) }
        reemplazarCuentaActiva(
            cuenta.copy(
                sal = android.util.Base64.encodeToString(sal, android.util.Base64.NO_WRAP),
                hash = hashContrasena(nueva, sal)
            )
        )
        return null
    }

    /** Correo guardado del administrador o, si aún no hay, una sugerencia con el dominio de la empresa. */
    fun correoAdministrador(): String {
        val cuenta = cuentaActiva() ?: return ""
        cuenta.correoAdministrador?.let { return it }
        val dominio = cuenta.correo.substringAfter("@", "")
        return if (ValidacionCuenta.esCorreoCorporativo(cuenta.correo)) "administrador@$dominio" else ""
    }

    fun guardarCorreoAdministrador(correo: String): String? {
        val cuenta = cuentaActiva() ?: return "No hay una sesión activa"
        val limpio = ValidacionCuenta.normalizar(correo)
        if (!ValidacionCuenta.esCorreoValido(limpio)) return "Correo del administrador no válido"
        reemplazarCuentaActiva(cuenta.copy(correoAdministrador = limpio))
        return null
    }

    private fun hashContrasena(contrasena: String, sal: ByteArray): String {
        val digest = java.security.MessageDigest.getInstance("SHA-256")
        digest.update(sal)
        val hash = digest.digest(contrasena.toByteArray(Charsets.UTF_8))
        return android.util.Base64.encodeToString(hash, android.util.Base64.NO_WRAP)
    }
    
    val zonas = mutableStateListOf<InformacionZona>()

    var datosClimaReales by mutableStateOf<DatosClima?>(null)

    val historialAnalisis = mutableStateListOf<ResultadoAnalisis>()

    init {
        cargarDatosDesdeRoom()
    }

    private fun cargarDatosDesdeRoom() {
        viewModelScope.launch {
            val zonasLocal = agroScanDao.obtenerTodasLasZonas()
            zonas.clear()
            zonas.addAll(zonasLocal.map { it.toDomain() })

            val historialLocal = agroScanDao.obtenerTodoElHistorial()
            historialAnalisis.clear()
            historialAnalisis.addAll(historialLocal.map { it.toDomain() })
        }
    }

    fun obtenerClimaActual(lat: Double, lon: Double) {
        android.util.Log.d("MainViewModel", "Obteniendo clima para: $lat, $lon")
        viewModelScope.launch {
            try {
                val response = weatherService.getClima(lat, lon, BuildConfig.OPENWEATHER_API_KEY)
                android.util.Log.d("MainViewModel", "Respuesta clima: ${response.name}, ${response.main.temp}")
                datosClimaReales = DatosClima(
                    ubicacion = response.name,
                    temperatura = "${response.main.temp.toInt()}°C",
                    humedad = "${response.main.humidity}%",
                    // OpenWeather (units=metric) da el viento en m/s: 1 m/s = 3,6 km/h
                    viento = "${(response.wind.speed * 3.6).roundToInt()} km/h",
                    // El endpoint /weather no incluye índice UV; se marca como no disponible en vez de inventarlo
                    uv = "UV N/D",
                    lluvia = String.format(Locale.US, "%.1fmm", response.rain?.ultimaHora ?: 0.0)
                )
                ciudadUsuario = response.name

                val nuevasAlertas = WeatherAlertEngine.evaluarRiesgo(
                    temperatura = response.main.temp,
                    humedad = response.main.humidity
                )
                alertasClimaticasActivas.clear()
                alertasClimaticasActivas.addAll(nuevasAlertas)

                // El widget no consulta la API: se le deja el último clima conocido
                getApplication<Application>().getSharedPreferences(PREFS_WIDGET, Context.MODE_PRIVATE).edit()
                    .putString(PREF_CLIMA_TEMPERATURA, datosClimaReales?.temperatura)
                    .putString(PREF_CLIMA_HUMEDAD, datosClimaReales?.humedad)
                    .putString(PREF_CLIMA_ALERTA, nuevasAlertas.firstOrNull()?.titulo)
                    .apply()
                refrescarWidget()

            } catch (e: Exception) {
                android.util.Log.e("MainViewModel", "Error al obtener clima: ${e.message}")
            }
        }
    }

    private val apiService = RoboflowApiService.create()

    // Detección de enfermedades: Roboflow (nube) y YOLO11 con LiteRT (local)
    var modoDeteccion by mutableStateOf(ModoDeteccion.AUTOMATICO)

    private val detectorLocalPerezoso = lazy<DetectorLocal?> {
        try {
            DetectorLocal(getApplication())
        } catch (e: Exception) {
            android.util.Log.e("MainViewModel", "No se pudo cargar el modelo local: ${e.message}")
            null
        }
    }
    private val detectorLocal by detectorLocalPerezoso

    private fun refrescarWidget() {
        viewModelScope.launch {
            try {
                AgroScanWidget().updateAll(getApplication())
            } catch (e: Exception) {
                android.util.Log.w("MainViewModel", "No se pudo actualizar el widget: ${e.message}")
            }
        }
    }

    fun alternarModoDeteccion() {
        val modos = ModoDeteccion.entries
        modoDeteccion = modos[(modoDeteccion.ordinal + 1) % modos.size]
    }

    override fun onCleared() {
        if (detectorLocalPerezoso.isInitialized()) detectorLocalPerezoso.value?.close()
        super.onCleared()
    }

    /**
     * El historial se agrupa y se borra por nombre de zona, así que el nombre debe ser único.
     * Devuelve un mensaje de error o null si el nombre es válido.
     */
    private fun validarNombreZona(nombre: String, idPropio: Long?): String? {
        val limpio = nombre.trim()
        if (limpio.isEmpty()) return "Ingrese un nombre para la zona"
        val repetida = zonas.any { it.id != idPropio && it.nombre.trim().equals(limpio, ignoreCase = true) }
        return if (repetida) "Ya existe una zona llamada «$limpio»" else null
    }

    /** Guarda una zona nueva. Devuelve un mensaje de error o null si se aceptó. */
    fun agregarZona(zonaNueva: InformacionZona): String? {
        validarNombreZona(zonaNueva.nombre, idPropio = null)?.let { return it }
        val zonaSinId = zonaNueva.copy(nombre = zonaNueva.nombre.trim(), id = 0)
        viewModelScope.launch {
            val id = agroScanDao.insertarZona(zonaSinId.toEntity())
            val zona = zonaSinId.copy(id = id)
            zonas.add(zona)

            // Cada zona nueva deja un registro en el historial para que aparezca agrupada ahí
            val sdfFecha = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
            val sdfHora = SimpleDateFormat("HH:mm", Locale.getDefault())
            val ahora = Date()
            
            val resultado = ResultadoAnalisis(
                titulo = "Registro de Zona",
                nombrePlanta = zona.cultivo,
                nombreZona = zona.nombre,
                descripcion = "Zona recién añadida",
                valorColor = 0xFF9E9E9E,
                fecha = sdfFecha.format(ahora),
                hora = sdfHora.format(ahora),
                ubicacion = ciudadUsuario,
                temperatura = datosClimaReales?.temperatura ?: "--",
                humedad = datosClimaReales?.humedad ?: "--",
                radiacionUV = datosClimaReales?.uv ?: "--",
                resumen = "Sin cambios detectados. La zona ha sido registrada exitosamente en el sistema."
            )
            
            val idHistorial = agroScanDao.insertarHistorial(resultado.toEntity())
            historialAnalisis.add(0, resultado.copy(id = idHistorial))
            refrescarWidget()
        }
        return null
    }

    fun eliminarZona(zona: InformacionZona) {
        viewModelScope.launch {
            agroScanDao.eliminarZonaPorId(zona.id)
            agroScanDao.eliminarHistorialPorZona(zona.nombre)
            zonas.removeAll { it.id == zona.id }
            historialAnalisis.removeAll { it.nombreZona == zona.nombre }
            refrescarWidget()
        }
    }

    /** Guarda los cambios de una zona existente. Devuelve un mensaje de error o null si se aceptó. */
    fun actualizarZona(zonaAntigua: InformacionZona, zonaEditada: InformacionZona): String? {
        validarNombreZona(zonaEditada.nombre, idPropio = zonaAntigua.id)?.let { return it }
        // Se conserva el id para que Room reemplace la fila en lugar de crear otra
        val zonaNueva = zonaEditada.copy(nombre = zonaEditada.nombre.trim(), id = zonaAntigua.id)
        viewModelScope.launch {
            agroScanDao.insertarZona(zonaNueva.toEntity())

            if (zonaNueva.nombre != zonaAntigua.nombre) {
                agroScanDao.renombrarZonaEnHistorial(zonaAntigua.nombre, zonaNueva.nombre)
                for (i in historialAnalisis.indices) {
                    val registro = historialAnalisis[i]
                    if (registro.nombreZona == zonaAntigua.nombre) {
                        historialAnalisis[i] = registro.copy(nombreZona = zonaNueva.nombre)
                    }
                }
            }

            val indice = zonas.indexOfFirst { it.id == zonaAntigua.id }
            if (indice != -1) zonas[indice] = zonaNueva else zonas.add(zonaNueva)
            refrescarWidget()
        }
        return null
    }

    fun actualizarValidacionAnalisis(
        resultadoOriginal: ResultadoAnalisis,
        nuevoEstado: String,
        observacionTecnica: String,
        esDeficienciaNutricional: Boolean
    ) {
        viewModelScope.launch {
            val resultadoActualizado = resultadoOriginal.copy(
                estadoValidacion = nuevoEstado,
                observacionTecnico = observacionTecnica,
                esDeficienciaNutricional = esDeficienciaNutricional
            )

            // El id identifica el registro exacto (antes se buscaba por fecha + hora + zona y
            // dos escaneos en el mismo minuto se confundían)
            if (resultadoOriginal.id != 0L) {
                agroScanDao.insertarHistorial(resultadoActualizado.toEntity())
            }

            val index = historialAnalisis.indexOfFirst { it.id == resultadoOriginal.id }
            if (index != -1) {
                historialAnalisis[index] = resultadoActualizado
            }
            refrescarWidget()
        }
    }

    fun sincronizarConPlataformaWeb(onComplete: (Boolean, String) -> Unit) {
        // Hoy solo se comprueba que el servidor responda (GET /health): el envío de zonas y diagnósticos
        // aún no existe, así que nunca se informa como "sincronizado".
        val servicio = webApiService
        if (servicio == null) {
            val msg = "Plataforma web no configurada: defina WEB_API_URL en local.properties."
            mensajeEstadoSincronizacion = msg
            onComplete(false, msg)
            return
        }
        viewModelScope.launch {
            estaSincronizandoWeb = true
            mensajeEstadoSincronizacion = "Conectando con la plataforma web..."
            val (exito, msg) = try {
                val response = servicio.verificarConexion()
                if (response.isSuccessful) {
                    val msgServer = response.body()?.get("mensaje")?.toString() ?: "OK"
                    true to "Servidor disponible ($msgServer). El envío de datos aún no está implementado."
                } else {
                    false to "El servidor respondió con error ${response.code()}."
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                false to "No se pudo conectar con la plataforma web: ${e.message ?: "sin conexión"}."
            }
            estaSincronizandoWeb = false
            mensajeEstadoSincronizacion = msg
            onComplete(exito, msg)
        }
    }

    // Preferencias de la interfaz (tema y animaciones): se recuerdan entre sesiones
    private val prefsApp = application.getSharedPreferences("agroscan_preferencias", Context.MODE_PRIVATE)

    // Declarado después de prefsApp: Kotlin inicializa las propiedades en orden
    var modoOscuroHabilitado by mutableStateOf(prefsApp.getBoolean(PREF_MODO_OSCURO, false))
        private set

    fun alternarTema(habilitado: Boolean) {
        modoOscuroHabilitado = habilitado
        prefsApp.edit().putBoolean(PREF_MODO_OSCURO, habilitado).apply()
    }

    var animacionesHabilitadas by mutableStateOf(prefsApp.getBoolean(PREF_ANIMACIONES, true))
        private set

    fun alternarAnimaciones(activas: Boolean) {
        animacionesHabilitadas = activas
        prefsApp.edit().putBoolean(PREF_ANIMACIONES, activas).apply()
    }

    // Vidrio esmerilado en vivo (desenfoque por GPU, Android 12+); se puede apagar en equipos modestos
    var vidrioHabilitado by mutableStateOf(prefsApp.getBoolean(PREF_VIDRIO, true))
        private set

    fun alternarVidrio(activo: Boolean) {
        vidrioHabilitado = activo
        prefsApp.edit().putBoolean(PREF_VIDRIO, activo).apply()
    }

    fun cerrarSesion() {
        // La cuenta se conserva; solo se cierra la sesión para pedir credenciales la próxima vez
        prefsSesion.edit().putBoolean(SESION_ACTIVA, false).apply()
        usuarioActual = null
    }

    /**
     * Analiza la foto y devuelve el diagnóstico sin guardarlo: el usuario elige después
     * en qué campo registrarlo con [guardarDiagnostico].
     */
    fun analizarImagenPlanta(
        jpeg: ByteArray,
        rotacion: Int,
        onResult: (ResultadoEscaneo) -> Unit
    ) {
        viewModelScope.launch {
            try {
                val bitmap = withContext(Dispatchers.Default) { decodificarConRotacion(jpeg, rotacion) }

                val diagnostico = when (modoDeteccion) {
                    ModoDeteccion.NUBE -> detectarEnNube(bitmap)
                    ModoDeteccion.LOCAL -> detectarEnLocal(bitmap)
                    ModoDeteccion.AUTOMATICO -> try {
                        detectarEnNube(bitmap)
                    } catch (e: CancellationException) {
                        throw e
                    } catch (e: Exception) {
                        android.util.Log.w("MainViewModel", "Roboflow no disponible, usando modelo local: ${e.message}")
                        detectarEnLocal(bitmap)
                    }
                }

                if (diagnostico != null) {
                    // Ambos modelos se traducen al mismo catálogo para que el historial sea comparable
                    onResult(
                        ResultadoEscaneo(
                            enfermedad = CatalogoEnfermedades.clasificar(diagnostico.clase),
                            porcentaje = (diagnostico.confianza * 100).toInt(),
                            etiquetaModelo = diagnostico.clase,
                            origen = diagnostico.origen
                        )
                    )
                } else {
                    onResult(ResultadoEscaneo.mensaje("No se detectaron hojas o síntomas en la imagen."))
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                onResult(ResultadoEscaneo.mensaje("Error: ${e.message}"))
            }
        }
    }

    /** Registra el diagnóstico en el historial del campo elegido (o como escaneo rápido si no hay campo). */
    fun guardarDiagnostico(escaneo: ResultadoEscaneo, zona: InformacionZona?) {
        val enfermedad = escaneo.enfermedad ?: return
        viewModelScope.launch {
            val ahora = Date()
            val resultado = ResultadoAnalisis(
                titulo = "Análisis IA",
                nombrePlanta = zona?.cultivo ?: enfermedad.cultivo.ifEmpty { "Planta Detectada" },
                nombreZona = zona?.nombre ?: "Escaneo Rápido",
                descripcion = enfermedad.nombre,
                valorColor = if (enfermedad.sana) 0xFF2ECC71 else 0xFFE67E22,
                fecha = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(ahora),
                hora = SimpleDateFormat("HH:mm", Locale.getDefault()).format(ahora),
                ubicacion = ciudadUsuario,
                temperatura = datosClimaReales?.temperatura ?: "--",
                humedad = datosClimaReales?.humedad ?: "--",
                radiacionUV = datosClimaReales?.uv ?: "--",
                resumen = "Análisis automático realizado vía cámara (${escaneo.origen}, ${escaneo.porcentaje}% de confianza, " +
                        "etiqueta del modelo: ${escaneo.etiquetaModelo}). Viento: ${datosClimaReales?.viento ?: "--"}."
            )

            val id = agroScanDao.insertarHistorial(resultado.toEntity())
            historialAnalisis.add(0, resultado.copy(id = id))
            refrescarWidget()
        }
    }

    private suspend fun detectarEnNube(bitmap: Bitmap): Diagnostico? {
        check(BuildConfig.ROBOFLOW_API_KEY.isNotBlank()) { "Falta ROBOFLOW_API_KEY en local.properties" }
        val base64 = withContext(Dispatchers.Default) { bitmapABase64(bitmap) }
        val response = apiService.detectDisease(
            // Modelo RF-DETR Small entrenado en Roboflow (workspace cristians-workspace-prtfd)
            project = "plant-disease-w0ogb-fl4eq",
            version = "1",
            authHeader = "Bearer ${BuildConfig.ROBOFLOW_API_KEY}",
            imageBase64 = base64.toRequestBody("text/plain".toMediaTypeOrNull())
        )
        val mejor = response.predictions.maxByOrNull { it.confidence } ?: return null
        return Diagnostico(mejor.className, mejor.confidence.toFloat(), "Nube (Roboflow)")
    }

    private suspend fun detectarEnLocal(bitmap: Bitmap): Diagnostico? {
        val mejor = withContext(Dispatchers.Default) {
            val detector = detectorLocal
                ?: throw IllegalStateException("Modelo local no disponible (falta assets/${DetectorLocal.NOMBRE_MODELO})")
            detector.detectar(bitmap).maxByOrNull { it.confianza }
        } ?: return null
        return Diagnostico(mejor.clase, mejor.confianza, "Local (YOLO11)")
    }
}
