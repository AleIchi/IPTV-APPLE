package com.iptv.fiber.datos.modelo

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

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

