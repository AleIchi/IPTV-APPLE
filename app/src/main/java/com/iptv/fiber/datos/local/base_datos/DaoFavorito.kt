package com.iptv.fiber.datos.local.base_datos

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

/** Objeto de acceso a datos para la tabla de favoritos. */
@Dao
interface DaoFavorito {

    @Query("SELECT * FROM favorites WHERE idServidor = :idServidor")
    fun obtenerFavoritos(idServidor: String): Flow<List<Favorito>>

    @Query("SELECT * FROM favorites WHERE id = :id")
    suspend fun obtenerFavoritoPorId(id: String): Favorito?

    @Query("SELECT * FROM favorites WHERE idServidor = :idServidor AND tipo = :tipo")
    fun obtenerFavoritosPorTipo(idServidor: String, tipo: String): Flow<List<Favorito>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertarFavorito(favorito: Favorito)

    @Delete
    suspend fun eliminarFavorito(favorito: Favorito)

    @Query("DELETE FROM favorites WHERE id = :id")
    suspend fun eliminarFavoritoPorId(id: String)

    @Query("SELECT EXISTS(SELECT 1 FROM favorites WHERE id = :id)")
    suspend fun esFavorito(id: String): Boolean
}
