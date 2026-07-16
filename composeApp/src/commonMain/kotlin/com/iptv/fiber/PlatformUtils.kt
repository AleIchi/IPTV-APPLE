package com.iptv.fiber

expect fun generarUUID(): String

@androidx.compose.runtime.Composable
expect fun EfectoBloqueoCaptura(bloquear: Boolean)

expect suspend fun descargarM3U(url: String): String
