package com.iptv.fiber

import platform.Foundation.NSUUID

@androidx.compose.runtime.Composable
actual fun EfectoBloqueoCaptura(bloquear: Boolean) {
    // En iOS se maneja de forma diferente, por ahora vacío
}

actual fun generarUUID(): String {
    return NSUUID().UUIDString
}

actual suspend fun descargarM3U(url: String): String {
    // TODO: Implementar descarga HTTP para iOS usando Ktor o NSURLSession
    return ""
}
