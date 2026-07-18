package com.iptv.fiber.datos.modelo

import kotlinx.serialization.Serializable

/** Datos de conexión a un servidor IPTV guardados localmente (URL, credenciales, estado activo). */
@Serializable
data class ConfiguracionServidor(
    val id: String,
    val urlServidor: String,
    val usuario: String,
    val contrasena: String,
    val estaActivo: Boolean = false,
    val nombreServidor: String = "",
    val creadoEn: Long = kotlinx.datetime.Clock.System.now().toEpochMilliseconds()
)

