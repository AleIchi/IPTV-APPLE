package com.iptv.fiber.datos.local.base_datos

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.compose.runtime.Immutable

/** Entidad que registra el historial de reproducción del usuario. */
@Immutable
@Entity(tableName = "seguir_viendo")
data class SeguirViendo(
    @PrimaryKey
    val id: String,
    val tipo: String,               // "canal"
    val nombre: String,
    val idTransmision: Int,
    val posicion: Long,             // Posición en milisegundos
    val duracion: Long,             // Duración total en milisegundos
    val icono: String? = null,
    val idServidor: String,
    val actualizadoEn: Long = kotlinx.datetime.Clock.System.now().toEpochMilliseconds()
)
