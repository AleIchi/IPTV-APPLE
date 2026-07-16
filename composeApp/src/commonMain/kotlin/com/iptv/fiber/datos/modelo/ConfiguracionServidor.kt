package com.iptv.fiber.datos.modelo

// removed android import: import android.os.Parcelable
import kotlinx.parcelize.Parcelize

/** Datos de conexión a un servidor IPTV guardados localmente (URL, credenciales, estado activo). */
@Parcelize
data class ConfiguracionServidor(
    val id: String,
    val urlServidor: String,
    val usuario: String,
    val contrasena: String,
    val estaActivo: Boolean = false,
    val nombreServidor: String = "",
    val creadoEn: Long = System.currentTimeMillis()
) : Parcelable

