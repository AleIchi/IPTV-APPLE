package com.iptv.fiber.datos.local.base_datos

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

/** Objeto de acceso a datos para el historial de reproducción. */
@Dao
interface DaoSeguirViendo {

    @Query("SELECT * FROM seguir_viendo WHERE idServidor = :idServidor ORDER BY actualizadoEn DESC LIMIT 15")
    fun obtenerHistorial(idServidor: String): Flow<List<SeguirViendo>>

    @Query("SELECT * FROM seguir_viendo WHERE id = :id")
    suspend fun obtenerHistorialPorId(id: String): SeguirViendo?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertarEnHistorial(entrada: SeguirViendo)

    @Delete
    suspend fun eliminarDeHistorial(entrada: SeguirViendo)

    @Query("DELETE FROM seguir_viendo WHERE id = :id")
    suspend fun eliminarDeHistorialPorId(id: String)

    @Query("DELETE FROM seguir_viendo")
    suspend fun limpiarTodoElHistorial()
}
