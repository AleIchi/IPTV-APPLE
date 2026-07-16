package com.iptv.fiber.datos.api



/**
 * Objeto centralizado para la configuración de red de la aplicación.
 * Permite cambiar los tiempos de espera de la API y de los reproductores de video,
 * el agente de usuario y el conjunto de conexiones en un solo lugar.
 */
object ConfiguracionRed {
    // ─── Tiempos de espera de la API (Retrofit / Ktor) ──────────────────────────────
    const val TIMEOUT_CONEXION_API_MS = 8000L
    const val TIMEOUT_LECTURA_API_MS = 10000L  // Reducido de 15s a 10s: si el servidor no responde en 10s, es un problema del servidor
    const val TIMEOUT_ESCRITURA_API_MS = 10000L

    // ─── Tiempos de espera de video (transmisiones de ExoPlayer) ─────────────
    const val TIMEOUT_CONEXION_VIDEO_MS = 4000L // Subido de 2s a 4s para mayor tolerancia
    const val TIMEOUT_LECTURA_VIDEO_MS = 8000L   // Subido de 5s a 8s para evitar cortes en servidores lentos

    // ─── Agente de usuario ───────────────────────────────────────────────────
    const val USER_AGENT = "FiberZapp"

    // ─── Conjunto de conexiones TCP ──────────────────────────────────────────
    const val MAX_CONEXIONES_REUTILIZABLES_API = 5
    const val TIEMPO_VIDA_CONEXION_API_SEGUNDOS = 30L

    const val MAX_CONEXIONES_REUTILIZABLES_VIDEO = 3   // 1 stream activo + 2 reconexiones en cola
    const val TIEMPO_VIDA_CONEXION_VIDEO_MINUTOS = 3L  // 3 min suficiente para live streams
}
