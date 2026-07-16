package com.iptv.fiber.tv.componentes

// removed android import: import android.content.Context
import androidx.annotation.OptIn
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.okhttp.OkHttpDataSource
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.extractor.DefaultExtractorsFactory
import androidx.media3.extractor.ts.DefaultTsPayloadReaderFactory
import okhttp3.ConnectionPool
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit
import com.iptv.fiber.datos.api.ConfiguracionRed
import java.security.cert.X509Certificate
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager

@OptIn(UnstableApi::class)
object GestorReproductorCompartido {
    private var reproductorExo: ExoPlayer? = null
    var urlActual: String? = null
        private set

    // Almacena el último canal que el usuario vio, útil para sincronizar la lista de la TV al volver
    var ultimoCanalReproducidoId: Int? = null

    // Bandera para evitar que MiniReproductorTV pause el video si se está abriendo ActividadReproductor
    var reproductorEnUsoPorPantallaCompleta: Boolean = false

    // Referencia al listener interno para poder removerlo en liberar() y evitar acumulación de listeners
    private var listenerInterno: androidx.media3.common.Player.Listener? = null
    // Contador de reintentos para evitar bucle infinito de reconexión cuando el servidor cae
    private var reintentosConsecutivos: Int = 0
    private const val MAX_REINTENTOS = 5

    /**
     * Devuelve el ExoPlayer compartido (creándolo si no existe) listo para reproducir [url].
     * Configura SSL bypass, buffers adaptados a 1GB RAM y reconexión automática en errores.
     * Si el reproductor ya existe y la URL cambió, solo reemplaza el MediaItem sin recrearlo.
     */
    fun obtenerOInicializar(contexto: Context, url: String): ExoPlayer {
        val appContext = contexto.applicationContext
        
        if (reproductorExo == null) {
            val renderersFactory = DefaultRenderersFactory(appContext)
                .setEnableDecoderFallback(true)
                .setExtensionRendererMode(DefaultRenderersFactory.EXTENSION_RENDERER_MODE_ON)

            val extractorsFactory = DefaultExtractorsFactory()
                .setTsExtractorFlags(
                    DefaultTsPayloadReaderFactory.FLAG_ENABLE_HDMV_DTS_AUDIO_STREAMS or
                    DefaultTsPayloadReaderFactory.FLAG_IGNORE_SPLICE_INFO_STREAM or
                    DefaultTsPayloadReaderFactory.FLAG_ALLOW_NON_IDR_KEYFRAMES
                )

            val trustAllCerts = arrayOf<TrustManager>(
                object : X509TrustManager {
                    override fun checkClientTrusted(chain: Array<out X509Certificate>?, authType: String?) {}
                    override fun checkServerTrusted(chain: Array<out X509Certificate>?, authType: String?) {}
                    override fun getAcceptedIssuers(): Array<X509Certificate> = arrayOf()
                }
            )

            val sslContext = SSLContext.getInstance("SSL").apply {
                init(null, trustAllCerts, java.security.SecureRandom())
            }

            val clienteOkHttp = OkHttpClient.Builder()
                .sslSocketFactory(sslContext.socketFactory, trustAllCerts[0] as X509TrustManager)
                .hostnameVerifier { _, _ -> true }
                .addInterceptor { chain ->
                    val original = chain.request()
                    val request = original.newBuilder()
                        .header("User-Agent", ConfiguracionRed.USER_AGENT)
                        .method(original.method, original.body)
                        .build()
                    chain.proceed(request)
                }
                .connectionPool(ConnectionPool(
                    ConfiguracionRed.MAX_CONEXIONES_REUTILIZABLES_VIDEO,
                    ConfiguracionRed.TIEMPO_VIDA_CONEXION_VIDEO_MINUTOS,
                    TimeUnit.MINUTES
                ))
                .connectTimeout(ConfiguracionRed.TIMEOUT_CONEXION_VIDEO, ConfiguracionRed.UNIDAD_TIEMPO_VIDEO)
                .readTimeout(ConfiguracionRed.TIMEOUT_LECTURA_VIDEO, ConfiguracionRed.UNIDAD_TIEMPO_VIDEO)
                .retryOnConnectionFailure(true)
                .build()
                
            val fabricaFuenteDatos = OkHttpDataSource.Factory(clienteOkHttp)
            val fabricaFuenteMedios = DefaultMediaSourceFactory(fabricaFuenteDatos, extractorsFactory)

            val preferencias = appContext.getSharedPreferences("iptv_preferencias", Context.MODE_PRIVATE)
            val modoEstable = preferencias.getBoolean("modo_buffer_estable", false)

            // Modo estable: máximo 20s de buffer para no agotar la RAM en TV Box de 1GB.
            // Un stream de 4Mbps a 20s ocupa ~10MB; con 50s era ~25MB innecesarios.
            val controlCarga = if (modoEstable) {
                DefaultLoadControl.Builder()
                    .setBufferDurationsMs(
                        5000,   // minBufferMs (5s) — mínimo para recuperar cortes
                        20000,  // maxBufferMs (20s)
                        2500,   // bufferForPlaybackMs — espera inicial
                        5000    // bufferForPlaybackAfterRebufferMs
                    )
                    .setPrioritizeTimeOverSizeThresholds(true)
                    .build()
            } else {
                // Buffer aumentado para streams IPTV live: microcortes de red de 200-500 ms
                // son comunes y con 500 ms de buffer mínimo el reproductor entraba en BUFFERING
                // constantemente, congelando el video. 3 s mínimo absorbe la mayoría de cortes.
                // 15 s máximo es razonable para TV Box de 1 GB (un stream de 4 Mbps usa ~7.5 MB).
                DefaultLoadControl.Builder()
                    .setBufferDurationsMs(
                        3000,   // minBufferMs (3s) — absorbe microcortes de red
                        15000,  // maxBufferMs (15s) — cómodo para 1 GB RAM
                        300,    // bufferForPlaybackMs (300ms) — arranque rápido pero estable
                        1000    // bufferForPlaybackAfterRebufferMs (1s) — recuperación sólida
                    )
                    .setPrioritizeTimeOverSizeThresholds(true)
                    .build()
            }

            val audioAttributes = AudioAttributes.Builder()
                .setUsage(C.USAGE_MEDIA)
                .setContentType(C.AUDIO_CONTENT_TYPE_MOVIE)
                .build()

            val trackSelector = androidx.media3.exoplayer.trackselection.DefaultTrackSelector(appContext).apply {
                // Leemos la calidad directamente desde SharedPreferences (sin runBlocking)
                // para evitar bloquear el hilo de UI durante la inicialización del reproductor
                val calidadFinal = preferencias.getString("calidad_video", "Auto") ?: "Auto"

                val builder = buildUponParameters().setPreferredAudioLanguage("es")
                when (calidadFinal) {
                    "1080p" -> builder.setMaxVideoSize(1920, 1080)
                    "720p" -> builder.setMaxVideoSize(1280, 720)
                    "480p" -> builder.setMaxVideoSize(854, 480)
                }
                setParameters(builder)
            }

            val nuevoListener = object : androidx.media3.common.Player.Listener {
                /** Fuerza la reconexión cuando una emisión en vivo "termina" (corte del servidor). */
                override fun onPlaybackStateChanged(playbackState: Int) {
                    if (playbackState == androidx.media3.common.Player.STATE_READY) {
                        // El stream está reproduciendo: resetear el contador de reintentos
                        reintentosConsecutivos = 0
                    }
                    if (playbackState == androidx.media3.common.Player.STATE_ENDED) {
                        if (reintentosConsecutivos < MAX_REINTENTOS) {
                            reintentosConsecutivos++
                            reproductorExo?.let { exo -> exo.seekToDefaultPosition(); exo.prepare(); exo.play() }
                        } else {
                            android.util.Log.w("GestorReproductor", "Máximo de reintentos alcanzado — stream caído")
                        }
                    }
                }

                /** Reintenta con backoff; limita reintentos para no agotar memoria si el servidor está caído. */
                override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
                    if (reintentosConsecutivos >= MAX_REINTENTOS) {
                        android.util.Log.w("GestorReproductor", "Error tras $MAX_REINTENTOS reintentos, deteniendo: ${error.message}")
                        return
                    }
                    reintentosConsecutivos++
                    val causa = error.cause
                    val esErrorAudio = causa?.message?.lowercase()?.contains("audio") ?: false
                    val esErrorDecodificador = error.errorCode == androidx.media3.common.PlaybackException.ERROR_CODE_DECODER_INIT_FAILED

                    reproductorExo?.let { exo ->
                        if (esErrorAudio || esErrorDecodificador) {
                            android.util.Log.w("GestorReproductor", "Error de códec (intento $reintentosConsecutivos): ${error.message}")
                            exo.prepare()
                            exo.play()
                        } else {
                            exo.seekToDefaultPosition()
                            exo.prepare()
                            exo.play()
                        }
                    }
                }
            }
            listenerInterno = nuevoListener
            reintentosConsecutivos = 0

            reproductorExo = ExoPlayer.Builder(appContext, renderersFactory)
                .setMediaSourceFactory(fabricaFuenteMedios)
                .setTrackSelector(trackSelector)
                .setLoadControl(controlCarga)
                .setAudioAttributes(audioAttributes, true)
                .setHandleAudioBecomingNoisy(true)
                .build()
                .apply {
                    addListener(nuevoListener)
                }
        }

        val player = reproductorExo!!

        if (urlActual != url) {
            urlActual = url
            val mediaItem = MediaItem.Builder()
                .setUri(url)
                .setLiveConfiguration(
                    MediaItem.LiveConfiguration.Builder()
                        .setMaxPlaybackSpeed(1.02f)
                        .setTargetOffsetMs(3000)
                        .setMinOffsetMs(1000)
                        .build()
                )
                .build()
            
            // Permitir a ExoPlayer gestionar la transición internamente y reutilizar recursos
            // NO forzar la detención (stop), ya que bloquea el decodificador de hardware
            // y causa que la pantalla se congele en el último fotograma en dispositivos Android TV.
            player.setMediaItem(mediaItem)
            player.prepare()
            player.playWhenReady = true
        }

        return player
    }

    /**
     * Devuelve el reproductor actual si ya está reproduciendo el URL solicitado.
     */
    fun cambiarUrlSiEsNecesario(url: String): ExoPlayer? {
        val player = reproductorExo ?: return null
        if (urlActual == url) return player

        urlActual = url
        val mediaItem = MediaItem.Builder()
            .setUri(url)
            .setLiveConfiguration(
                MediaItem.LiveConfiguration.Builder()
                    .setMaxPlaybackSpeed(1.02f)
                    .setTargetOffsetMs(3000)
                    .setMinOffsetMs(1000)
                    .build()
            )
            .build()

        player.setMediaItem(mediaItem)
        player.prepare()
        player.playWhenReady = true
        return player
    }

    /** Devuelve el reproductor si ya está reproduciendo exactamente [url]; null en caso contrario. */
    fun obtenerSiEsMismaUrl(url: String): ExoPlayer? {
        return if (urlActual == url) reproductorExo else null
    }

    /**
     * Permite a la ActividadReproductor tomar posesión de este reproductor.
     * Desvincula la referencia interna para que no se libere prematuramente.
     */
    fun tomarPosesion(): ExoPlayer? {
        val p = reproductorExo
        reproductorExo = null
        urlActual = null
        return p
    }

    /**
     * Recibe de vuelta el player que fue tomado con [tomarPosesion].
     * Llamado por [ActividadReproductor.onPause] al finalizar, para que
     * [MiniReproductorTV] reutilice la misma instancia sin recargar el stream.
     */
    fun recibirPlayerDevuelto(player: ExoPlayer, url: String) {
        reproductorExo = player
        urlActual = url
    }

    /** Destruye el ExoPlayer y libera todos los recursos de audio/video del sistema. */
    fun liberar() {
        // Remover el listener interno ANTES de release() para que no reciba callbacks
        // de estados finales (STATE_ENDED) y no intente reconectar sobre un player ya destruido.
        listenerInterno?.let { reproductorExo?.removeListener(it) }
        listenerInterno = null
        reintentosConsecutivos = 0
        reproductorExo?.release()
        reproductorExo = null
        urlActual = null
    }
}
