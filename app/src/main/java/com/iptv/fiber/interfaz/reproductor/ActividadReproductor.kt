package com.iptv.fiber.interfaz.reproductor

import android.net.Uri
import android.os.Bundle
import android.content.res.Configuration
import android.view.View
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.iptv.fiber.datos.repositorio.RepositorioContenido
import com.iptv.fiber.interfaz.tema.TemaIPTVFiber
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.collect

/**
 * Activity que gestiona la reproducción de vídeo. La UI (PlayerScreen, PlayerControlsOverlay) está
 * en PlayerUI.kt.
 */
class ActividadReproductor : ComponentActivity() {

    // ─── Detección de emulador ──────────────────────────────────────────────

    private val esEmulador =
            android.os.Build.FINGERPRINT.let {
                it.startsWith("generic") || it.startsWith("unknown")
            } ||
                    android.os.Build.MODEL.let {
                        it.contains("google_sdk") ||
                                it.contains("Emulator") ||
                                it.contains("Android SDK built for x86")
                    } ||
                    android.os.Build.MANUFACTURER.contains("Genymotion") ||
                    (android.os.Build.BRAND.startsWith("generic") &&
                            android.os.Build.DEVICE.startsWith("generic")) ||
                    "google_sdk" == android.os.Build.PRODUCT

    // ─── Estado del reproductor ─────────────────────────────────────────────
    private var reproductorExo: androidx.media3.exoplayer.ExoPlayer? = null

    private var reproducirAlEstarListo = true
    private var ventanaActual = 0
    private var posicionReproduccion = 0L
    private var urlTransmision: String? = null
    private var idTransmisionActual: Int = -1
    private var tipoTransmision: String = "live"
    private var reproductorInicializado = false

    private var estadoNombreCanal = androidx.compose.runtime.mutableStateOf("Emisión")
    private var estadoLogoCanal = androidx.compose.runtime.mutableStateOf<String?>(null)

    // Datos del servidor para reconstrucción de URLs (Xtream)
    private var servidorUrl: String? = null
    private var usuario: String? = null
    private var contrasena: String? = null

    private lateinit var repositorioContenido: RepositorioContenido
    private val favoritosState = androidx.compose.runtime.mutableStateOf<List<com.iptv.fiber.datos.local.base_datos.Favorito>>(emptyList())
    private var esPiP = androidx.compose.runtime.mutableStateOf(false)

    // ─── Ciclo de vida ──────────────────────────────────────────────────────

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        @Suppress("DEPRECATION")
        window.decorView.systemUiVisibility =
                (View.SYSTEM_UI_FLAG_FULLSCREEN or
                        View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
                        View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        urlTransmision = intent.getStringExtra("url_transmision")
        idTransmisionActual = intent.getIntExtra("id_transmision", -1)
        tipoTransmision = intent.getStringExtra("tipo_transmision") ?: "live"
        estadoNombreCanal.value = intent.getStringExtra("nombre_canal") ?: "Emisión"
        estadoLogoCanal.value = intent.getStringExtra("logo_canal")

        servidorUrl = intent.getStringExtra("servidor_url")
        usuario = intent.getStringExtra("usuario")
        contrasena = intent.getStringExtra("contrasena")

        if (urlTransmision == null) {
            finish()
            return
        }

        val baseDatos = com.iptv.fiber.datos.local.base_datos.BaseDatosIPTV.obtenerBaseDatos()
        val clienteApi = com.iptv.fiber.datos.api.ClienteApi()
        val gestorPreferencias = com.iptv.fiber.datos.local.GestorPreferencias(this)
        val repositorioAuth = com.iptv.fiber.datos.repositorio.RepositorioAutenticacion(clienteApi, gestorPreferencias)
        repositorioContenido = RepositorioContenido(
            clienteApi,
            repositorioAuth,
            baseDatos.daoFavorito(),
            baseDatos.daoSeguirViendo()
        )

        // Observar favoritos
        lifecycleScope.launch {
            repositorioContenido.obtenerFavoritos().collect {
                favoritosState.value = it
            }
        }

        inicializarExo()

        setContent {
            TemaIPTVFiber {
                Surface(modifier = Modifier.fillMaxSize(), color = Color.Black) {
                    PantallaReproductor(
                            reproductorExo = reproductorExo,
                            nombreCanal = estadoNombreCanal.value,
                            logoCanal = estadoLogoCanal.value,
                            idTransmision = idTransmisionActual,
                            favoritos = favoritosState.value,
                            alSiguiente = { cargarSiguienteCanal() },
                            alAnterior = { cargarCanalAnterior() },
                            alAbrirExterno = { abrirEnReproductorExt() },
                            alCambiarCanal = { canal -> cambiarCanal(canal) },
                            enPiP = esPiP.value,
                            alAlternarFavorito = { canal ->
                                lifecycleScope.launch {
                                    val esFav = repositorioContenido.alternarFavorito(canal)
                                    val msj = if (esFav) "Añadido a Favoritos" else "Eliminado de Favoritos"
                                    android.widget.Toast.makeText(this@ActividadReproductor, msj, android.widget.Toast.LENGTH_SHORT).show()
                                }
                            }
                    )
                }
            }
        }
    }

    private fun inicializarExo() {
        if (reproductorExo != null) return
        
        val audioAttributes = androidx.media3.common.AudioAttributes.Builder()
            .setUsage(androidx.media3.common.C.USAGE_MEDIA)
            .setContentType(androidx.media3.common.C.AUDIO_CONTENT_TYPE_MOVIE)
            .build()

        val renderersFactory = androidx.media3.exoplayer.DefaultRenderersFactory(this)
            .setEnableDecoderFallback(true)
            .setExtensionRendererMode(androidx.media3.exoplayer.DefaultRenderersFactory.EXTENSION_RENDERER_MODE_PREFER)

        val extractorsFactory = androidx.media3.extractor.DefaultExtractorsFactory()
            .setTsExtractorFlags(
                androidx.media3.extractor.ts.DefaultTsPayloadReaderFactory.FLAG_ENABLE_HDMV_DTS_AUDIO_STREAMS or
                androidx.media3.extractor.ts.DefaultTsPayloadReaderFactory.FLAG_IGNORE_SPLICE_INFO_STREAM or
                // EL SECRETO DEL ZAPPING INSTANTÁNEO: 
                // No esperar a un frame IDR (Keyframe) completo para empezar a mostrar imagen.
                androidx.media3.extractor.ts.DefaultTsPayloadReaderFactory.FLAG_ALLOW_NON_IDR_KEYFRAMES
            )

        val clientOkHttp = okhttp3.OkHttpClient.Builder()
            .addInterceptor { chain ->
                val original = chain.request()
                val request = original.newBuilder()
                    .header("User-Agent", "FiberZapp")
                    .method(original.method, original.body)
                    .build()
                chain.proceed(request)
            }
            // Pool de conexiones más agresivo (10 conexiones abiertas)
            .connectionPool(okhttp3.ConnectionPool(10, 5, java.util.concurrent.TimeUnit.MINUTES))
            .connectTimeout(2, java.util.concurrent.TimeUnit.SECONDS)
            .readTimeout(5, java.util.concurrent.TimeUnit.SECONDS)
            .retryOnConnectionFailure(true)
            .build()
            
        val dataSourceFactory = androidx.media3.datasource.okhttp.OkHttpDataSource.Factory(clientOkHttp)
        val mediaSourceFactory = androidx.media3.exoplayer.source.DefaultMediaSourceFactory(dataSourceFactory, extractorsFactory)

        val preferencias = getSharedPreferences("iptv_preferencias", android.content.Context.MODE_PRIVATE)
        val modoEstable = preferencias.getBoolean("modo_buffer_estable", false)

        val controlCarga = androidx.media3.exoplayer.DefaultLoadControl.Builder()
            .setBufferDurationsMs(
                if (modoEstable) 15000 else 2500,  // minBufferMs (Mantener buffer amplio para evitar cortes)
                if (modoEstable) 50000 else 15000, // maxBufferMs (Límite máximo de memoria caché de video)
                if (modoEstable) 1500 else 350,    // bufferForPlaybackMs (Súper rápido para el inicio instantáneo)
                if (modoEstable) 2500 else 1000    // bufferForPlaybackAfterRebufferMs (Tiempo de recuperación tras un corte)
            )
            .setPrioritizeTimeOverSizeThresholds(true)
            .build()

        reproductorExo = androidx.media3.exoplayer.ExoPlayer.Builder(this, renderersFactory)
            .setMediaSourceFactory(mediaSourceFactory)
            .setLoadControl(controlCarga)
            .setAudioAttributes(audioAttributes, true)
            .setHandleAudioBecomingNoisy(true)
            .build()
            .apply {
                volume = 1.0f
                addListener(object : androidx.media3.common.Player.Listener {
                    override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
                        val causa = error.cause
                        val esErrorAudio = causa?.message?.lowercase()?.contains("audio") ?: false
                        val esErrorCodec = error.errorCode == androidx.media3.common.PlaybackException.ERROR_CODE_DECODER_INIT_FAILED
                        
                        if (esErrorAudio || esErrorCodec) {
                            android.util.Log.w("Reproductor", "Error de codec en ExoPlayer: ${error.message}")
                            // Intentar forzar recuperación de software
                            prepare()
                            play()
                        } else {
                            if (error.errorCode == androidx.media3.common.PlaybackException.ERROR_CODE_BEHIND_LIVE_WINDOW) {
                                seekToDefaultPosition()
                            }
                            prepare()
                            play()
                        }
                    }
                })
            }
    }

    private fun liberarExo() {
        reproductorExo?.release()
        reproductorExo = null
    }

    override fun onStart() {
        super.onStart()
        inicializarReproductor()
    }
    override fun onResume() {
        super.onResume()
        reproductorExo?.play()
    }
    override fun onUserLeaveHint() {
        super.onUserLeaveHint()
        val preferencias = getSharedPreferences("iptv_preferencias", android.content.Context.MODE_PRIVATE)
        val pipActivo = preferencias.getBoolean("pip_activo", true)
        
        if (pipActivo && android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            val params = android.app.PictureInPictureParams.Builder()
                .setAspectRatio(android.util.Rational(16, 9))
                .build()
            enterPictureInPictureMode(params)
        }
    }

    override fun onPictureInPictureModeChanged(isInPictureInPictureMode: Boolean, newConfig: Configuration) {
        @Suppress("UNUSED_PARAMETER")
        super.onPictureInPictureModeChanged(isInPictureInPictureMode, newConfig)
        esPiP.value = isInPictureInPictureMode
    }

    override fun onPause() {
        super.onPause()
        val preferencias = getSharedPreferences("iptv_preferencias", android.content.Context.MODE_PRIVATE)
        val fondoActivo = preferencias.getBoolean("fondo_activo", false)
        
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N && isInPictureInPictureMode) {
            // Si estamos en PiP, NO pausamos
            return
        }
        
        if (!fondoActivo) {
            reproductorExo?.pause()
        }
    }
    
    override fun onStop() {
        super.onStop()
        val preferencias = getSharedPreferences("iptv_preferencias", android.content.Context.MODE_PRIVATE)
        val fondoActivo = preferencias.getBoolean("fondo_activo", false)
        
        if (!fondoActivo) {
            reproductorExo?.pause()
        }
    }
    override fun onDestroy() {
        super.onDestroy()
        liberarReproductor()
    }

    // ─── Inicialización / liberación ────────────────────────────────────────

    private fun inicializarReproductor() {
        if (reproductorInicializado) return
        
        inicializarExo()
        reproductorExo?.let { exo ->
            urlTransmision?.let { url ->
                val mediaItem = androidx.media3.common.MediaItem.Builder()
                    .setUri(android.net.Uri.parse(url))
                    .setLiveConfiguration(
                        androidx.media3.common.MediaItem.LiveConfiguration.Builder()
                            .setMaxPlaybackSpeed(1.02f) // Permite acelerar ligeramente para alcanzar el en vivo
                            .setTargetOffsetMs(3000)    // 3 segundos de retraso seguro del en vivo real
                            .setMinOffsetMs(1000)       // Evitar pegarse demasiado al en vivo y generar cortes
                            .build()
                    )
                    .build()
                exo.setMediaItem(mediaItem)
                exo.playWhenReady = true
                exo.seekTo(ventanaActual, posicionReproduccion)
                exo.prepare()
                reproductorInicializado = true
            }
        }
    }

    private fun liberarReproductor() {
        liberarExo()
        reproductorInicializado = false
    }

    // ─── Cambio de canal ────────────────────────────────────────────────────

    private fun cargarSiguienteCanal() =
            RepositorioContenido.obtenerSiguienteCanal(idTransmisionActual)?.let {
                cambiarCanal(it)
            }
    private fun cargarCanalAnterior() =
            RepositorioContenido.obtenerCanalAnterior(idTransmisionActual)?.let { cambiarCanal(it) }

    private fun cambiarCanal(canal: com.iptv.fiber.datos.modelo.Canal) {
        val nuevaUrl =
                if (!canal.fuenteDirecta.isNullOrEmpty()) {
                    canal.fuenteDirecta
                } else if (servidorUrl != null && usuario != null && contrasena != null) {
                    "$servidorUrl/live/$usuario/$contrasena/${canal.id_transmision}.ts"
                } else {
                    urlTransmision?.replace(
                            "/$idTransmisionActual.ts",
                            "/${canal.id_transmision}.ts"
                    )
                }

        if (nuevaUrl != null && nuevaUrl != urlTransmision) {
            urlTransmision = nuevaUrl
            idTransmisionActual = canal.id_transmision
            estadoNombreCanal.value = canal.nombre
            estadoLogoCanal.value = canal.icono_transmision
            val uri = Uri.parse(nuevaUrl)
            
            reproductorExo?.let { exo ->
                // Aplicar la misma configuración de Zapping ultra-rápido (LiveConfiguration)
                val mediaItem = androidx.media3.common.MediaItem.Builder()
                    .setUri(uri)
                    .setLiveConfiguration(
                        androidx.media3.common.MediaItem.LiveConfiguration.Builder()
                            .setMaxPlaybackSpeed(1.02f)
                            .setTargetOffsetMs(3000)
                            .setMinOffsetMs(1000)
                            .build()
                    )
                    .build()
                
                // Detener buffers anteriores inmediatamente para liberar recursos al instante
                exo.stop()
                exo.clearMediaItems()
                exo.setMediaItem(mediaItem)
                exo.volume = 1f
                exo.playWhenReady = true
                exo.prepare()
            }
        }
    }

    // ─── Reproducción externa ───────────────────────────────────────────────

    private fun abrirEnReproductorExt() {
        if (urlTransmision == null) return
        try {
            val intent =
                    android.content.Intent(android.content.Intent.ACTION_VIEW).apply {
                        setDataAndType(Uri.parse(urlTransmision), "video/*")
                        addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                        try {
                            packageManager.getPackageInfo("org.videolan.vlc", 0)
                            setPackage("org.videolan.vlc")
                        } catch (_: Exception) {
                            /* usar selector del sistema */
                        }
                    }
            startActivity(intent)
            reproductorExo?.pause()
        } catch (e: Exception) {
            android.widget.Toast.makeText(
                            this,
                            "No se encontró un reproductor externo",
                            android.widget.Toast.LENGTH_LONG
                    )
                    .show()
        }
    }

    // ─── PiP ────────────────────────────────────────────────────────────────

    override fun onNewIntent(intent: android.content.Intent?) {
        super.onNewIntent(intent)
        setIntent(intent)
        intent?.let {
            val nuevaUrl = it.getStringExtra("url_transmision") ?: return@let
            val nuevoNombre = it.getStringExtra("nombre_canal") ?: "Emisión"
            val nuevoLogo = it.getStringExtra("logo_canal")
            urlTransmision = nuevaUrl
            estadoNombreCanal.value = nuevoNombre
            estadoLogoCanal.value = nuevoLogo

            // Actualizar datos del servidor si vienen en el nuevo intent
            it.getStringExtra("servidor_url")?.let { servidorUrl = it }
            it.getStringExtra("usuario")?.let { usuario = it }
            it.getStringExtra("contrasena")?.let { contrasena = it }
            
            reproductorExo?.let { exo ->
                exo.setMediaItem(androidx.media3.common.MediaItem.fromUri(Uri.parse(nuevaUrl)))
                exo.prepare()
                exo.play()
            }
        }
    }

    override fun onPictureInPictureModeChanged(isInPictureInPictureMode: Boolean) {
        super.onPictureInPictureModeChanged(isInPictureInPictureMode)
        @Suppress("DEPRECATION")
        window.decorView.systemUiVisibility =
                if (isInPictureInPictureMode) {
                    View.SYSTEM_UI_FLAG_FULLSCREEN
                } else {
                    View.SYSTEM_UI_FLAG_FULLSCREEN or
                            View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
                            View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                }
    }

}
