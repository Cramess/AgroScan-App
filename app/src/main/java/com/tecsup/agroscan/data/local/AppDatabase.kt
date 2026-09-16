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

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertarZona(zona: ZonaEntity)

    @Delete
    suspend fun eliminarZona(zona: ZonaEntity)

    @Query("SELECT * FROM historial ORDER BY id DESC")
    suspend fun obtenerTodoElHistorial(): List<HistorialEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertarHistorial(historial: HistorialEntity)
    
    @Query("DELETE FROM historial WHERE nombreZona = :nombreZona")
    suspend fun eliminarHistorialPorZona(nombreZona: String)
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
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
