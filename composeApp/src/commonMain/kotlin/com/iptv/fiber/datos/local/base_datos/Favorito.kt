package com.iptv.fiber.datos.local.base_datos

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.compose.runtime.Immutable

/** Entidad que representa un canal o contenido marcado como favorito. */
@Immutable
@Entity(tableName = "favorites")
data class Favorito(
    @PrimaryKey
    val id: String,
    val tipo: String,           // "canal"
    val nombre: String,
    val idTransmision: Int,
    val icono: String? = null,
    val idServidor: String,
    val agregadoEn: Long = kotlinx.datetime.Clock.System.now().toEpochMilliseconds()
)
