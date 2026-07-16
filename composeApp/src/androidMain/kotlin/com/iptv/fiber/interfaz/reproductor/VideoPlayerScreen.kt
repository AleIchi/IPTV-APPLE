package com.iptv.fiber.interfaz.reproductor

import androidx.compose.runtime.Composable

@Composable
actual fun VideoPlayerScreen(streamId: String?) {
    // Delegamos al reproductor nativo de Android (ExoPlayer)
    PantallaReproductor(streamId = streamId)
}
