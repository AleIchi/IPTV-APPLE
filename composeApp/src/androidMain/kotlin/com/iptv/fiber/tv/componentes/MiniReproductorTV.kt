package com.iptv.fiber.tv.componentes

// removed android import: import android.net.Uri
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
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.extractor.DefaultExtractorsFactory
import androidx.media3.extractor.ts.DefaultTsPayloadReaderFactory
import androidx.media3.datasource.okhttp.OkHttpDataSource
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import okhttp3.OkHttpClient

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.iptv.fiber.interfaz.tema.TextoSecundarioPremium

/** 
 * EL REPRODUCTOR CHIQUITO (Mini Reproductor).
 * Este componente es el cuadrito de video que ves en la pantalla de Inicio de TV.
 * Usa un truco muy avanzado: "GestorReproductorCompartido". Si haces clic en el cuadrito
 * para verlo en pantalla completa, NO vuelve a cargar el video desde cero. Le pasa la 
 * "pelota" (el ExoPlayer) a la pantalla grande al instante, sin interrupciones ni pantallas negras.
 */
@OptIn(UnstableApi::class)
@Composable
fun MiniReproductorTV(
    urlTransmision: String,
    modifier: Modifier = Modifier,
    nombreCanal: String? = null
) {
    val contexto = LocalContext.current
    val lifecycleOwner = androidx.compose.ui.platform.LocalLifecycleOwner.current
    var seDebeReactivar by remember { mutableStateOf(0) }
    val ultimaReactivacionAplicada = remember { intArrayOf(-1) }

    val exoPlayer = remember(urlTransmision, seDebeReactivar) {
        GestorReproductorCompartido.obtenerOInicializar(contexto, urlTransmision).apply {
            volume = 1f // Vista previa con sonido habilitado
        }
    }

    var estaCargando by remember(urlTransmision) {
        mutableStateOf(true)
    }

    var estaActivo by remember { mutableStateOf(true) }

    // exoPlayer es key porque al volver de pantalla completa se crea una instancia nueva
    // (GestorReproductorCompartido.tomarPosesion() la libera) y el efecto debe re-registrar
    // el listener en el nuevo player; sin este key el listener queda huérfano.
    DisposableEffect(urlTransmision, lifecycleOwner, exoPlayer) {
        estaCargando = true

        val oyente = object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                if (playbackState == Player.STATE_BUFFERING) estaCargando = true
                else if (playbackState == Player.STATE_READY) estaCargando = false
            }
            override fun onIsPlayingChanged(isPlaying: Boolean) { if (isPlaying) estaCargando = false }
            override fun onPlayerError(error: androidx.media3.common.PlaybackException) { estaCargando = false }
            override fun onRenderedFirstFrame() { estaCargando = false }
        }
        exoPlayer.addListener(oyente)

        // EL OBSERVADOR DEL SISTEMA (Lifecycle):
        // Si el usuario presiona "Home" en su control remoto para salir de la app, el sistema 
        // dispara "ON_STOP". Si eso pasa, pausamos el video para que no se siga escuchando de fondo,
        // a menos que esté pasando a pantalla completa.
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME) {
                estaActivo = true
                seDebeReactivar++
            } else if (event == androidx.lifecycle.Lifecycle.Event.ON_STOP) {
                estaActivo = false
                if (!GestorReproductorCompartido.reproductorEnUsoPorPantallaCompleta) {
                    exoPlayer.pause()
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)

        onDispose {
            exoPlayer.removeListener(oyente)
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    Box(modifier = modifier.fillMaxSize().background(Color.Black)) {
        AndroidView(
            factory = { context ->
                PlayerView(context).apply {
                    layoutParams = android.widget.FrameLayout.LayoutParams(
                        android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                        android.view.ViewGroup.LayoutParams.MATCH_PARENT
                    )
                    useController = false
                    resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FILL
                    setBackgroundColor(android.graphics.Color.BLACK)
                    setKeepContentOnPlayerReset(false)
                    setShutterBackgroundColor(android.graphics.Color.BLACK)
                }
            },
            update = { playerView ->
                seDebeReactivar

                // Si la pantalla completa tomó posesión del player, el mini NO debe tocarlo
                // (ni attach de surface ni play): evita competir por la misma instancia y
                // el congelamiento de imagen durante la transición.
                if (!estaActivo || GestorReproductorCompartido.reproductorEnUsoPorPantallaCompleta) {
                    playerView.player = null
                    return@AndroidView
                }

                val ultimaUrl = playerView.tag as? String
                if (ultimaUrl != urlTransmision || ultimaReactivacionAplicada[0] != seDebeReactivar) {
                    ultimaReactivacionAplicada[0] = seDebeReactivar
                    playerView.tag = urlTransmision
                    
                    playerView.player = null
                    playerView.player = exoPlayer
                    playerView.requestLayout()
                }

                exoPlayer.volume = 1f
                if (!exoPlayer.isPlaying) {
                    exoPlayer.play()
                }
            },
            onRelease = { playerView ->
                playerView.player = null
            },
            modifier = Modifier.fillMaxSize()
        )

        if (estaCargando) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.6f)),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(
                        color = AcentoPremium,
                        modifier = Modifier.size(48.dp),
                        strokeWidth = 3.dp
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Sincronizando...",
                        color = TextoSecundarioPremium,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium
                    )
                    if (nombreCanal != null) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = nombreCanal,
                            color = Color.White,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}
