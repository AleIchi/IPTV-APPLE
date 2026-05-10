package com.iptv.fiber.tv.componentes

import android.net.Uri
import androidx.annotation.OptIn
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import com.iptv.fiber.interfaz.tema.AcentoPremium
import androidx.compose.ui.unit.dp

@OptIn(UnstableApi::class)
@Composable
fun MiniReproductorTV(
    urlStream: String,
    modifier: Modifier = Modifier
) {
    val contexto = LocalContext.current
    var estaCargando by remember { mutableStateOf(true) }

    val exoPlayer = remember {
        val controlCarga = DefaultLoadControl.Builder()
            .setBufferDurationsMs(
                2000,   // minBufferMs
                10000,  // maxBufferMs
                500,    // bufferForPlaybackMs
                1000    // bufferForPlaybackAfterRebufferMs
            )
            .setPrioritizeTimeOverSizeThresholds(true)
            .build()

        ExoPlayer.Builder(contexto)
            .setLoadControl(controlCarga)
            .build()
            .apply {
                volume = 0f // Vista previa sin sonido para no molestar
                playWhenReady = true
                
                addListener(object : Player.Listener {
                    override fun onPlaybackStateChanged(playbackState: Int) {
                        if (playbackState == Player.STATE_READY) {
                            estaCargando = false
                        }
                    }
                })
            }
    }

    DisposableEffect(urlStream) {
        val mediaItem = MediaItem.Builder()
            .setUri(Uri.parse(urlStream))
            .setLiveConfiguration(
                MediaItem.LiveConfiguration.Builder()
                    .setMaxPlaybackSpeed(1.02f)
                    .setTargetOffsetMs(3000)
                    .setMinOffsetMs(1000)
                    .build()
            )
            .build()
            
        exoPlayer.setMediaItem(mediaItem)
        exoPlayer.prepare()

        onDispose {
            exoPlayer.release()
        }
    }

    Box(modifier = modifier.fillMaxSize().background(Color.Black)) {
        AndroidView(
            factory = { context ->
                PlayerView(context).apply {
                    player = exoPlayer
                    useController = false
                    resizeMode = AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                    setBackgroundColor(android.graphics.Color.BLACK)
                }
            },
            modifier = Modifier.fillMaxSize()
        )
        
        if (estaCargando) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.5f)),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(
                    color = AcentoPremium,
                    modifier = Modifier.fillMaxSize(0.3f),
                    strokeWidth = 2.dp
                )
            }
        }
    }
}
