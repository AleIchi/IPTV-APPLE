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
 * LA SALA DE CINE (El Reproductor).
 * Esta es la pantalla donde el usuario ve la televisión o las películas.
 * Oculta la barra de notificaciones del celular para que el video ocupe toda la pantalla.
 * La parte que "dibuja" los botones de pausa/play está separada en 'PlayerUI.kt'.
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
    private var reproductorExoState = androidx.compose.runtime.mutableStateOf<androidx.media3.exoplayer.ExoPlayer?>(null)
    private var reproductorExo: androidx.media3.exoplayer.ExoPlayer?
        get() = reproductorExoState.value
        set(value) { reproductorExoState.value = value }

    private var reproducirAlEstarListo = true
    private var ventanaActual = 0
    private var posicionReproduccion = 0L
    private var urlTransmision: String? = null
    private var estadoIdTransmision = androidx.compose.runtime.mutableIntStateOf(-1)
    private var tipoTransmision: String = "live"

    private var estadoNombreCanal = androidx.compose.runtime.mutableStateOf("Emisión")
    private var estadoLogoCanal = androidx.compose.runtime.mutableStateOf<String?>(null)

    // Datos del servidor para reconstrucción de URLs (Xtream)
    private var servidorUrl: String? = null
    private var usuario: String? = null
    private var contrasena: String? = null

    private lateinit var repositorioContenido: RepositorioContenido
    private val favoritosState = androidx.compose.runtime.mutableStateOf<List<com.iptv.fiber.datos.local.base_datos.Favorito>>(emptyList())
    private val mensajeFavorito = androidx.compose.runtime.mutableStateOf<String?>(null)
    private var esImagenEnImagen = androidx.compose.runtime.mutableStateOf(false)
    private var trabajoZapping: kotlinx.coroutines.Job? = null

    // ─── Ciclo de vida ──────────────────────────────────────────────────────

    /**
     * Esta función se ejecuta cuando se abre la pantalla del reproductor.
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.setBackgroundDrawableResource(android.R.color.black)

        // Avisar a la pantalla principal que vamos a usar el reproductor, para que no lo pause
        com.iptv.fiber.tv.componentes.GestorReproductorCompartido.reproductorEnUsoPorPantallaCompleta = true

        // --- MODO CINE ---
        // Ocultar botones de navegación (atrás, inicio) y barra superior (batería, hora)
        // También evita que la pantalla se apague sola mientras el usuario ve televisión.
        @Suppress("DEPRECATION")
        window.decorView.systemUiVisibility =
                (View.SYSTEM_UI_FLAG_FULLSCREEN or
                        View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
                        View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        urlTransmision = intent.getStringExtra(ClavesReproductor.URL_TRANSMISION)
        estadoIdTransmision.intValue = intent.getIntExtra(ClavesReproductor.ID_TRANSMISION, -1)
        com.iptv.fiber.tv.componentes.GestorReproductorCompartido.ultimoCanalReproducidoId = estadoIdTransmision.intValue
        tipoTransmision = intent.getStringExtra(ClavesReproductor.TIPO_TRANSMISION) ?: "live"
        estadoNombreCanal.value = intent.getStringExtra(ClavesReproductor.NOMBRE_CANAL) ?: "Emisión"
        estadoLogoCanal.value = intent.getStringExtra(ClavesReproductor.LOGOTIPO_CANAL) ?: intent.getStringExtra(ClavesReproductor.LOGOTIPO_CANAL_ANTERIOR)

        servidorUrl = intent.getStringExtra(ClavesReproductor.SERVIDOR_URL)
        usuario = intent.getStringExtra(ClavesReproductor.USUARIO)
        contrasena = intent.getStringExtra(ClavesReproductor.CONTRASENA)

        if (urlTransmision == null) {
            finish()
            return
        }

        val baseDatos = com.iptv.fiber.datos.local.base_datos.BaseDatosIPTV.obtenerBaseDatos()
        val clienteApi = com.iptv.fiber.datos.api.ClienteApi()
        val gestorPreferencias = com.iptv.fiber.datos.local.GestorPreferencias(this)
        val repositorioAuth = com.iptv.fiber.datos.repositorio.RepositorioAutenticacion(clienteApi, gestorPreferencias)
        
        if (!servidorUrl.isNullOrBlank() && !usuario.isNullOrBlank()) {
            repositorioAuth.establecerSesionActiva(servidorUrl!!, usuario!!, contrasena ?: "")
        }

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
                            logotipoCanal = estadoLogoCanal.value,
                            idTransmision = estadoIdTransmision.intValue,
                            favoritos = favoritosState.value,
                            alSiguiente = { cargarSiguienteCanal() },
                            alAnterior = { cargarCanalAnterior() },
                            alAbrirExterno = { abrirEnReproductorExt() },
                            alCambiarCanal = { canal -> cambiarCanal(canal) },
                            enImagenEnImagen = esImagenEnImagen.value,
                            alAlternarFavorito = { canal ->
                                lifecycleScope.launch {
                                    val esFav = repositorioContenido.alternarFavorito(canal)
                                    mensajeFavorito.value = if (esFav) "Añadido a Favoritos" else "Eliminado de Favoritos"
                                }
                            },
                            mensajeFavorito = mensajeFavorito.value,
                            alLimpiarMensajeFavorito = { mensajeFavorito.value = null }
                    )
                }
            }
        }
    }

    /**
     * Toma POSESIÓN del ExoPlayer para la pantalla completa.
     * Si el mini reproductor del Inicio ya está reproduciendo la misma URL, le quitamos
     * la instancia (tomarPosesion) para que el singleton quede vacío y NADIE más controle
     * el mismo player a la vez. Esto evita el congelamiento de imagen y el crash al abrir
     * un segundo canal. Si la URL es distinta, obtenemos/creamos la instancia y luego la
     * tomamos en posesión para vaciar el singleton igualmente.
     */
    private fun inicializarExo() {
        if (reproductorExo != null) return

        urlTransmision?.let { url ->
            val gestor = com.iptv.fiber.tv.componentes.GestorReproductorCompartido
            // 1. Asegurar que la instancia compartida exista y apunte a nuestra URL.
            gestor.obtenerOInicializar(this, url)
            // 2. Tomar posesión: el singleton queda en null y esta actividad es la única dueña.
            reproductorExo = gestor.tomarPosesion()
                    ?: gestor.obtenerOInicializar(this, url).also { gestor.tomarPosesion() }
            reproductorExo?.volume = 1.0f
        }
    }

    /** Destruye el ExoPlayer del que esta actividad tomó posesión y suelta la referencia local. */
    private fun liberarExo() {
        reproductorExo?.release()
        reproductorExo = null
    }

    /** Solo desvincula nuestra referencia sin destruir el reproductor. */
    private fun desconectarExo() {
        reproductorExo = null
    }

    /**
     * Reanuda los recursos necesarios cuando la actividad entra en primer plano.
     */
    override fun onStart() {
        super.onStart()
        inicializarReproductor()
    }
    /**
     * Restaura la reproduccion y el estado visual al volver a la actividad.
     */
    override fun onResume() {
        super.onResume()
        reproductorExo?.play()
    }
    /**
     * IMAGEN EN IMAGEN (PiP)
     * Esta función se ejecuta automáticamente cuando el usuario presiona el botón "Home" del celular
     * para salir de la aplicación mientras ve un canal.
     * En lugar de cerrar el video, la app se encoge a un cuadrito flotante para que
     * el usuario siga viendo la TV mientras usa WhatsApp u otras apps.
     */
    override fun onUserLeaveHint() {
        super.onUserLeaveHint()
        val preferencias = getSharedPreferences("iptv_preferencias", android.content.Context.MODE_PRIVATE)
        val pipActivo = preferencias.getBoolean("pip_activo", true)
        
        if (pipActivo && android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            // Le decimos a Android que queremos una ventanita rectangular (16:9)
            val params = android.app.PictureInPictureParams.Builder()
                .setAspectRatio(android.util.Rational(16, 9))
                .build()
            enterPictureInPictureMode(params)
        }
    }

    /**
     * Actualiza el estado de interfaz al entrar o salir del modo imagen en imagen.
     */
    override fun onPictureInPictureModeChanged(isInPictureInPictureMode: Boolean, newConfig: Configuration) {
        @Suppress("UNUSED_PARAMETER")
        super.onPictureInPictureModeChanged(isInPictureInPictureMode, newConfig)
        esImagenEnImagen.value = isInPictureInPictureMode
    }

    /**
     * Pausa o conserva recursos segun el estado visible de la actividad.
     */
    override fun onPause() {
        super.onPause()

        if (isFinishing) {
            reproductorExo?.pause()
            // Devolver el player al singleton ANTES de que ActividadTV.onResume() dispare la
            // reactivación del mini player. Si lo destruyéramos aquí, el mini player crearía
            // una instancia nueva y recargaría el stream desde cero (pantalla negra + buffering).
            urlTransmision?.let { url ->
                reproductorExo?.let { player ->
                    com.iptv.fiber.tv.componentes.GestorReproductorCompartido.recibirPlayerDevuelto(player, url)
                }
            }
            // Liberar la bandera aquí (no en onDestroy) porque onPause() se ejecuta ANTES de
            // ActividadTV.onResume(). Si se hiciera en onDestroy(), el update{} del mini player
            // vería la bandera = true y haría playerView.player = null, causando pantalla negra.
            com.iptv.fiber.tv.componentes.GestorReproductorCompartido.reproductorEnUsoPorPantallaCompleta = false
            desconectarExo()
            return
        }

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N && isInPictureInPictureMode) {
            return
        }

        reproductorExo?.pause()
    }
    
    /**
     * Gestiona recursos cuando la actividad deja de estar visible.
     */
    override fun onStop() {
        super.onStop()
        if (isFinishing) {
            return
        }
        reproductorExo?.pause()
    }
    /**
     * Libera recursos asociados al ciclo de vida cuando la actividad se destruye.
     */
    override fun onDestroy() {
        super.onDestroy()
        trabajoZapping?.cancel()
        // La bandera y el player se gestionaron en onPause(isFinishing=true).
        // Como net de seguridad para rutas de ciclo de vida inusuales (crash, etc.):
        com.iptv.fiber.tv.componentes.GestorReproductorCompartido.reproductorEnUsoPorPantallaCompleta = false
        // liberarExo() es no-op aquí (reproductorExo == null tras desconectarExo en onPause).
        // Solo actúa si onPause(isFinishing) no se ejecutó correctamente.
        liberarExo()
    }

    // ─── Inicialización / liberación ────────────────────────────────────────

    /** Reinicializa el reproductor si fue desconectado (ej. al volver de background sin modo PiP activo). */
    private fun inicializarReproductor() {
        if (reproductorExo != null) return
        inicializarExo()
    }

    /** Libera el reproductor compartido cuando ya no se necesita (delegado de [liberarExo]). */
    private fun liberarReproductor() {
        liberarExo()
    }

    // ─── Cambio de canal ────────────────────────────────────────────────────

    /** Solicita el canal siguiente en el contexto de reproducción activo y llama a [cambiarCanal]. */
    private fun cargarSiguienteCanal() =
            RepositorioContenido.obtenerSiguienteCanal(estadoIdTransmision.intValue)?.let {
                cambiarCanal(it)
            }
    /** Solicita el canal anterior con navegación circular y llama a [cambiarCanal]. */
    private fun cargarCanalAnterior() =
            RepositorioContenido.obtenerCanalAnterior(estadoIdTransmision.intValue)?.let { cambiarCanal(it) }

    /**
     * ZAPPING ULTRA RÁPIDO (Cambio de Canal)
     * Imagina que el usuario presiona el botón "Siguiente canal" 10 veces en 1 segundo.
     * Si la app intentara cargar los 10 videos de golpe, el celular explotaría (se congelaría).
     * Por eso usamos un "Debounce":
     * 1. Pausa el video al instante.
     * 2. Espera 250 milisegundos.
     * 3. Si el usuario ya no presionó nada más en ese tiempo, recién ahí carga el nuevo canal.
     */
    private fun cambiarCanal(canal: com.iptv.fiber.datos.modelo.Canal) {
        val nuevaUrl =
                if (!canal.fuenteDirecta.isNullOrEmpty()) {
                    canal.fuenteDirecta
                } else if (servidorUrl != null && usuario != null && contrasena != null) {
                    com.iptv.fiber.datos.repositorio.RepositorioAutenticacion.construirUrlTransmisionXtream(
                            urlServidor = servidorUrl!!,
                            usuario = usuario!!,
                            contrasena = contrasena!!,
                            tipo = "live",
                            id = canal.id_transmision
                    )
                } else {
                    urlTransmision?.replace(
                            "/${estadoIdTransmision.intValue}.ts",
                            "/${canal.id_transmision}.ts"
                    )
                }

        if (nuevaUrl != null && nuevaUrl != urlTransmision) {
            // 1. Cancelar cualquier carga de zapping pendiente si el usuario sigue presionando "siguiente"
            trabajoZapping?.cancel()

            // 2. Actualizar los textos y logos de la pantalla de inmediato
            urlTransmision = nuevaUrl
            estadoIdTransmision.intValue = canal.id_transmision
            estadoNombreCanal.value = canal.nombre
            estadoLogoCanal.value = canal.icono_transmision
            com.iptv.fiber.tv.componentes.GestorReproductorCompartido.ultimoCanalReproducidoId = canal.id_transmision
            
            // 3. Crear una alarma (Delay de 250ms) antes de forzar al celular a descargar el video
            trabajoZapping = lifecycleScope.launch {
                kotlinx.coroutines.delay(250)

                // 4. Guardar en historial solo si el usuario se quedó en este canal (pasó el debounce)
                repositorioContenido.agregarAlHistorial(canal)

                // 5. ¡Tiempo cumplido! Cambiar el canal SOBRE EL PLAYER QUE YA POSEEMOS.
                // No volvemos a pasar por el singleton (quedó vacío al tomar posesión);
                // así evitamos crear un player duplicado durante el zapping.
                reproductorExo?.let { exo ->
                    val mediaItem = androidx.media3.common.MediaItem.Builder()
                            .setUri(nuevaUrl)
                            .setLiveConfiguration(
                                    androidx.media3.common.MediaItem.LiveConfiguration.Builder()
                                            .setMaxPlaybackSpeed(1.02f)
                                            .setTargetOffsetMs(3000)
                                            .setMinOffsetMs(1000)
                                            .build()
                            )
                            .build()
                    exo.setMediaItem(mediaItem)
                    exo.prepare()
                    exo.playWhenReady = true
                    exo.volume = 1f
                }
            }
        }
    }

    // ─── Reproducción externa ───────────────────────────────────────────────

    /** Lanza la URL de transmisión en un reproductor externo (preferentemente VLC) usando un Intent de sistema. */
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

    /**
     * Procesa un nuevo intent recibido por una actividad ya existente.
     */
    override fun onNewIntent(intent: android.content.Intent?) {
        super.onNewIntent(intent)
        setIntent(intent)
        intent?.let {
            val nuevaUrl = it.getStringExtra(ClavesReproductor.URL_TRANSMISION) ?: return@let
            val nuevoId = it.getIntExtra(ClavesReproductor.ID_TRANSMISION, -1)
            val nuevoNombre = it.getStringExtra(ClavesReproductor.NOMBRE_CANAL) ?: "Emisión"
            val nuevoLogo = it.getStringExtra(ClavesReproductor.LOGOTIPO_CANAL) ?: it.getStringExtra(ClavesReproductor.LOGOTIPO_CANAL_ANTERIOR)
            urlTransmision = nuevaUrl
            estadoNombreCanal.value = nuevoNombre
            estadoLogoCanal.value = nuevoLogo
            if (nuevoId != -1) estadoIdTransmision.intValue = nuevoId

            // Actualizar datos del servidor si vienen en el nuevo intent
            it.getStringExtra(ClavesReproductor.SERVIDOR_URL)?.let { servidorUrl = it }
            it.getStringExtra(ClavesReproductor.USUARIO)?.let { usuario = it }
            it.getStringExtra(ClavesReproductor.CONTRASENA)?.let { contrasena = it }

            // Guardar en historial
            if (nuevoId != -1) {
                val canal = com.iptv.fiber.datos.modelo.Canal(
                    id_transmision = nuevoId,
                    nombre = nuevoNombre,
                    icono_transmision = nuevoLogo,
                    id_categoria = ""
                )
                lifecycleScope.launch { repositorioContenido.agregarAlHistorial(canal) }
            }

            reproductorExo?.let { exo ->
                exo.setMediaItem(androidx.media3.common.MediaItem.fromUri(Uri.parse(nuevaUrl)))
                exo.prepare()
                exo.play()
            }
        }
    }

    /**
     * Actualiza el estado de interfaz al entrar o salir del modo imagen en imagen.
     */
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
