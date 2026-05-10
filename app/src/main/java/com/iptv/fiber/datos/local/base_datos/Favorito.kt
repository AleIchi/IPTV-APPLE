package com.iptv.fiber.datos.local.base_datos

import androidx.room.Entity
import androidx.room.PrimaryKey

/** Entidad que representa un canal o contenido marcado como favorito. */
@Entity(tableName = "favorites")
data class Favorito(
    @PrimaryKey
    val id: String,
    val tipo: String,           // "canal", "pelicula", "series"
    val nombre: String,
    val idTransmision: Int,
    val icono: String? = null,
    val idServidor: String,
    val agregadoEn: Long = System.currentTimeMillis()
)
