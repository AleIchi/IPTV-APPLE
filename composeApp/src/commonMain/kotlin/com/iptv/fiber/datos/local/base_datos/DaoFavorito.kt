package com.iptv.fiber.datos.local.base_datos

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

/** 
 * EL MENSAJERO DE LA BASE DE DATOS (DAO - Data Access Object).
 * Las bases de datos hablan en un lenguaje llamado "SQL" ("SELECT * FROM...").
 * La app habla en Kotlin. Esta clase es el traductor.
 * Nosotros llamamos a las funciones en Kotlin, y Room automáticamente escribe
 * y ejecuta el código SQL para guardar o buscar los favoritos.
 */
@Dao
interface DaoFavorito {

    /** 
     * Retorna todos los favoritos de un servidor.
     * Al usar "Flow", la lista es REACTIVA. Es decir, si agregas un favorito nuevo
     * en otra parte de la app, la pantalla de favoritos se actualiza sola al instante
     * sin que tengas que volver a pedirlos.
     */
    @Query("SELECT * FROM favorites WHERE idServidor = :idServidor")
    fun obtenerFavoritos(idServidor: String): Flow<List<Favorito>>

    /** Busca un favorito por su [id] único, o null si no existe. */
    @Query("SELECT * FROM favorites WHERE id = :id")
    suspend fun obtenerFavoritoPorId(id: String): Favorito?

    /** Retorna un Flow de favoritos filtrado por [tipo] (ej. "canal") y servidor. */
    @Query("SELECT * FROM favorites WHERE idServidor = :idServidor AND tipo = :tipo")
    fun obtenerFavoritosPorTipo(idServidor: String, tipo: String): Flow<List<Favorito>>

    /** 
     * Guarda un favorito en el celular.
     * Si el canal ya estaba guardado (mismo ID), lo reemplaza silenciosamente en lugar de crashear
     * gracias al "REPLACE".
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertarFavorito(favorito: Favorito)

    /** Elimina el favorito exacto pasado como parámetro. */
    @Delete
    suspend fun eliminarFavorito(favorito: Favorito)

    /** Elimina el favorito con el [id] indicado sin necesidad de tener el objeto. */
    @Query("DELETE FROM favorites WHERE id = :id")
    suspend fun eliminarFavoritoPorId(id: String)

    /** Devuelve true si existe un favorito con el [id] indicado en la base de datos. */
    @Query("SELECT EXISTS(SELECT 1 FROM favorites WHERE id = :id)")
    suspend fun esFavorito(id: String): Boolean
}
