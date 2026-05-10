package com.iptv.fiber.datos.local.base_datos

import androidx.room.Entity
import androidx.room.PrimaryKey

/** Entidad que registra el historial de reproducción del usuario. */
@Entity(tableName = "seguir_viendo")
data class SeguirViendo(
    @PrimaryKey
    val id: String,
    val tipo: String,               // "pelicula", "episodio"
    val nombre: String,
    val idTransmision: Int,
    val posicion: Long,             // Posición en milisegundos
    val duracion: Long,             // Duración total en milisegundos
    val icono: String? = null,
    val idServidor: String,
    val idEpisodio: Int? = null,    // Solo para episodios
    val numeroTemporada: Int? = null, // Solo para episodios
    val numeroEpisodio: Int? = null,  // Solo para episodios
    val actualizadoEn: Long = System.currentTimeMillis()
)
