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

    /** Retorna un Flow con los últimos 50 elementos del historial, ordenados del más reciente al más antiguo. */
    @Query("SELECT * FROM seguir_viendo WHERE idServidor = :idServidor ORDER BY actualizadoEn DESC LIMIT 50")
    fun obtenerHistorial(idServidor: String): Flow<List<SeguirViendo>>

    /** Busca una entrada del historial por su [id] único, o null si no existe. */
    @Query("SELECT * FROM seguir_viendo WHERE id = :id")
    suspend fun obtenerHistorialPorId(id: String): SeguirViendo?

    /** Inserta o actualiza una entrada en el historial. Si existe el mismo [id], lo reemplaza. */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertarEnHistorial(entrada: SeguirViendo)

    /** Elimina la entrada exacta pasada como parámetro del historial. */
    @Delete
    suspend fun eliminarDeHistorial(entrada: SeguirViendo)

    /** Elimina la entrada del historial con el [id] indicado. */
    @Query("DELETE FROM seguir_viendo WHERE id = :id")
    suspend fun eliminarDeHistorialPorId(id: String)

    /**
     * Mantiene solo los [limite] elementos más recientes del historial para el servidor indicado.
     * Elimina las entradas más antiguas que superen ese límite.
     */
    @Query(
        """
        DELETE FROM seguir_viendo
        WHERE idServidor = :idServidor
        AND id NOT IN (
            SELECT id FROM seguir_viendo
            WHERE idServidor = :idServidor
            ORDER BY actualizadoEn DESC
            LIMIT :limite
        )
        """
    )
    suspend fun recortarHistorial(idServidor: String, limite: Int)

    /** Elimina todo el historial de reproducción del servidor indicado. */
    @Query("DELETE FROM seguir_viendo WHERE idServidor = :idServidor")
    suspend fun limpiarHistorial(idServidor: String)
}
