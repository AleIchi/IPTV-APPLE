package com.iptv.fiber.interfaz.reproductor

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color

@Composable
actual fun VideoPlayerScreen(streamId: String?) {
    // Implementación temporal para iOS. 
    // Aquí se debe integrar AVPlayer usando UIKitView o compose-multiplatform-media
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text("Reproductor de Video para iOS no implementado aún", color = Color.White)
    }
}
