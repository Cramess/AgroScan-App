package com.tecsup.agroscan.data.local

import android.content.Context
import androidx.room.*
import com.google.android.gms.maps.model.LatLng
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

@Dao
interface AgroScanDao {
    @Query("SELECT * FROM zonas")
    suspend fun obtenerTodasLasZonas(): List<ZonaEntity>

    /** Inserta o reemplaza (si el id ya existe) y devuelve el id de la fila. */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertarZona(zona: ZonaEntity): Long

    @Query("DELETE FROM zonas WHERE id = :id")
    suspend fun eliminarZonaPorId(id: Long)

    @Query("SELECT * FROM historial ORDER BY id DESC")
    suspend fun obtenerTodoElHistorial(): List<HistorialEntity>

    /** Inserta o reemplaza (si el id ya existe) y devuelve el id de la fila. */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertarHistorial(historial: HistorialEntity): Long

    @Query("DELETE FROM historial WHERE nombreZona = :nombreZona")
    suspend fun eliminarHistorialPorZona(nombreZona: String)

    // El historial se agrupa por nombre de zona: al renombrarla hay que arrastrar sus registros
    @Query("UPDATE historial SET nombreZona = :nombreNuevo WHERE nombreZona = :nombreAnterior")
    suspend fun renombrarZonaEnHistorial(nombreAnterior: String, nombreNuevo: String)
}

class Converters {
    @TypeConverter
    fun fromLatLngList(value: String): List<LatLng> {
        val listType = object : TypeToken<List<LatLng>>() {}.type
        return Gson().fromJson(value, listType)
    }

    @TypeConverter
    fun toLatLngList(list: List<LatLng>): String {
        return Gson().toJson(list)
    }
}

@Database(entities = [ZonaEntity::class, HistorialEntity::class], version = 2)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun agroScanDao(): AgroScanDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "agroscan_database"
                )
                // Solo se descarta la base de la versión 1 (prototipo). Cualquier cambio de esquema
                // futuro debe subir `version` y añadir una Migration con .addMigrations(...) para no
                // borrar las zonas e historial del usuario.
                .fallbackToDestructiveMigrationFrom(1)
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
