# Plan de Implementación: Robustez y Persistencia para AgroScan

Este plan detalla las mejoras para convertir AgroScan en una aplicación persistente y funcional, integrando una base de datos local y servicios de clima reales, manteniendo la estructura actual lo más intacta posible.

## User Review Required

> [!IMPORTANT]
> Se añadirán nuevas dependencias al proyecto (Room y KSP). Esto requiere una sincronización de Gradle que puede tardar unos minutos.
> Se utilizarán valores "mock" para las API Keys de clima y Roboflow; deberás reemplazarlas por llaves reales en `local.properties` después de la implementación.

## Proposed Changes

### Dependencias y Configuración

#### [MODIFY] [libs.versions.toml](file:///C:/AgroScan/AgroScan/gradle/libs.versions.toml)
Añadir versiones y librerías para Room y el plugin KSP.

#### [MODIFY] [build.gradle.kts (App)](file:///C:/AgroScan/AgroScan/app/build.gradle.kts)
Aplicar el plugin KSP y añadir las dependencias de Room.

---

### Persistencia de Datos (Room)

#### [NEW] [AppDatabase.kt](file:///C:/AgroScan/AgroScan/app/src/main/java/com/tecsup/agroscan/data/local/AppDatabase.kt)
Definir la base de datos de Room y los DAOs necesarios.

#### [NEW] [Entities.kt](file:///C:/AgroScan/AgroScan/app/src/main/java/com/tecsup/agroscan/data/local/Entities.kt)
Crear las entidades de Room para `ZonaEntity` y `HistorialEntity` (mapeando las clases de datos actuales).

---

### Servicios y Red

#### [MODIFY] [WeatherApiService.kt](file:///C:/AgroScan/AgroScan/app/src/main/java/com/tecsup/agroscan/Services/WeatherApiService.kt)
Implementar la interfaz de Retrofit para obtener datos reales de clima (ej. OpenWeather).

#### [MODIFY] [AgroScanApiService.kt](file:///C:/AgroScan/AgroScan/app/src/main/java/com/tecsup/agroscan/Services/AgroScanApiService.kt)
Asegurar que el servicio de Roboflow esté correctamente configurado.

---

### Lógica de Negocio y UI

#### [MODIFY] [MainViewModel.kt](file:///C:/AgroScan/AgroScan/app/src/main/java/com/tecsup/agroscan/viewmodel/MainViewModel.kt)
- Inicializar la base de datos de Room.
- Cargar datos desde Room al iniciar.
- Actualizar los métodos `agregarZona`, `eliminarZona` y `analizarImagenPlanta` para que guarden los cambios en la base de datos local.
- Añadir lógica para obtener el clima real basado en la ubicación.

## Verification Plan

### Automated Tests
- Ejecutar `gradlew assembleDebug` para verificar que la configuración de Room y KSP es correcta.
- Pruebas unitarias básicas para los DAOs de Room (si se solicitan).

### Manual Verification
1.  **Persistencia:** Crear una zona, cerrar la app y volver a abrirla para verificar que la zona sigue ahí.
2.  **Clima:** Verificar que la temperatura y humedad se actualicen (con la API Key configurada).
3.  **Historial:** Realizar un análisis y verificar que se guarde permanentemente.
