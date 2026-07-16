package com.iptv.fiber.interfaz.reproductor

// ─── LIBRERÍAS DEL SISTEMA Y COMPOSE ──────────────────────────────────────────
// 'android.content.res' nos sirve para detectar la rotación de pantalla o si estamos en TV.
// removed android import: import android.content.res.Configuration

// 'foundation' y 'material3' son las piezas de Lego de Jetpack Compose.
// Nos dan cajas (Box, Column), listas (LazyColumn), iconos y colores.
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.border
import androidx.compose.ui.draw.clip
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.*

// 'runtime' es el corazón de Compose. Tiene las variables que "recuerdan" el estado (remember).
import androidx.compose.runtime.*

// Herramientas de dibujo (Colores, modificadores visuales, tamaños).
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// ─── LIBRERÍAS DE CONTROL REMOTO (TV) Y GESTOS ───────────────────────────────
// 'focus' y 'key' se usan para atrapar los "clics" de las flechas del control remoto de la TV.
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type

// 'pointerInput' es para atrapar cuando deslizas el dedo por la pantalla (Celular).
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration

// ─── EL MOTOR DE VIDEO (ExoPlayer) ───────────────────────────────────────────
// 'AndroidView' es un puente. Como ExoPlayer (el reproductor oficial de Google) 
// usa código antiguo de Android, necesitamos 'AndroidView' para meterlo dentro de Jetpack Compose.
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.Player
import androidx.media3.common.VideoSize
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView

// ─── LIBRERÍAS PROPIAS DE NUESTRA APP (IPTV) ─────────────────────────────────
// Nuestros datos (Canales), nuestra base de datos (Repositorio) y nuestros colores (Tema).
import com.iptv.fiber.datos.modelo.Canal
import com.iptv.fiber.datos.repositorio.RepositorioContenido
import com.iptv.fiber.interfaz.tema.*

// Componentes exclusivos que dibujamos cuando la app corre en un Smart TV.
import com.iptv.fiber.tv.componentes.BarraInfoCanalTV
import com.iptv.fiber.tv.componentes.PanelCanalesTV

// ─── RUTINAS EN SEGUNDO PLANO (Corrutinas) ───────────────────────────────────
// Nos permiten ocultar los botones después de 5 segundos sin congelar la app (delay).
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * LA INTERFAZ DEL REPRODUCTOR (Los Botones sobre el Video).
 * Mientras que 'ActividadReproductor' es la "Sala de Cine", esta clase es el "Control Remoto".
 * Aquí dibujamos los botones de Pausa, Siguiente, y la rueda de carga.
 * Tiene lógica especial para saber si estás en Celular o en TV.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PantallaReproductor(
        reproductorExo: ExoPlayer?,
        nombreCanal: String,
        logotipoCanal: String?,
        idTransmision: Int,
        favoritos: List<com.iptv.fiber.datos.local.base_datos.Favorito>,
        enImagenEnImagen: Boolean = false,
        alSiguiente: () -> Unit,
        alAnterior: () -> Unit,
        alAbrirExterno: () -> Unit,
        alCambiarCanal: (Canal) -> Unit = {},
        alAlternarFavorito: (Canal) -> Unit = {},
        alCambiarVelocidad: (Float) -> Unit = {},
        alCambiarEscalado: (Int) -> Unit = {},
        mensajeFavorito: String? = null,
        alLimpiarMensajeFavorito: () -> Unit = {}
) {
    var mostrarControles by remember { mutableStateOf(true) }
    var estaReproduciendo by remember { mutableStateOf(true) }
    var estaCargando by remember {
        mutableStateOf(reproductorExo?.playbackState == androidx.media3.common.Player.STATE_BUFFERING)
    }
    var mostrarObturador by remember {
        mutableStateOf(reproductorExo != null && reproductorExo.playbackState != androidx.media3.common.Player.STATE_READY)
    }
    var mostrarAjustes by remember { mutableStateOf(false) }
    var velocidadActual by remember { mutableStateOf(1.0f) }
    var modoEscalado by remember { mutableStateOf(0) }
    val estadoHojaAjustes = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    // Estados específicos para TV
    var mostrarPanelCanales by remember { mutableStateOf(false) }
    var mostrarInfoCanal by remember { mutableStateOf(false) }
    var enfocarBarra by remember { mutableStateOf(false) }
    var primeraCarga by remember { mutableStateOf(true) }

    val configuracion = LocalConfiguration.current
    val esVertical = configuracion.orientation == Configuration.ORIENTATION_PORTRAIT
    val modoUi = configuracion.uiMode
    // 1. ¿DÓNDE ESTAMOS EJECUTANDO LA APP?
    // Detectamos si estamos en un Celular/Tablet o en un Smart TV.
    // Esto es crucial porque en TV necesitamos menús laterales y usar el control remoto.
    val esTV = (modoUi and Configuration.UI_MODE_TYPE_MASK) == Configuration.UI_MODE_TYPE_TELEVISION

    // 2. EL CONTROL REMOTO (Foco)
    // En TV no tenemos un dedo para tocar la pantalla. El 'FocusRequester' le dice a Android:
    // "Oye, apenas se abra esta pantalla, manda la selección (el foco) aquí para que 
    // el usuario pueda usar las flechas de su control remoto".
    val requeridorFoco = remember { FocusRequester() }
    LaunchedEffect(Unit) { requeridorFoco.requestFocus() }

    // ========================================================================
    // 3. RECUPERANDO LOS DATOS DEL CANAL (Memoria Caché)
    // Cuando el usuario elige un canal, a esta pantalla solo le llega un número (Ej: ID 1500).
    // Usamos 'remember' para que estos datos se busquen solo una vez por canal,
    // y no cada milisegundo (lo que trabaría el dispositivo).
    // ========================================================================
    
    // Traemos TODA la información del canal (Logo, categoría, servidor, etc.)
    val canalCompleto =
            remember(idTransmision) {
                RepositorioContenido.obtenerCanalCompleto(
                        idTransmision,
                        Canal(nombre = nombreCanal, id_transmision = idTransmision)
                )
            }
            
    // Traemos a los "Hermanos" de este canal. 
    // Si estamos viendo "ESPN", traemos todos los canales de la categoría "Deportes".
    // Esto sirve para mostrar la barra lateral de zapping rápido en la TV.
    val canalesCategoria =
            remember(idTransmision) {
                RepositorioContenido.obtenerCanalesRecomendados(idTransmision)
            }
            
    // Traducimos el código de la categoría (Ej: "cat_45") a un nombre real (Ej: "Noticias")
    val nombreCategoria =
            remember(idTransmision) {
                RepositorioContenido.obtenerNombreCategoria(canalCompleto.id_categoria)
            }
            
    // ¿Cuántos canales hay en total en esta categoría?
    val totalCategoria =
            remember(idTransmision) {
                RepositorioContenido.obtenerConteoCanalesCategoria(canalCompleto.id_categoria)
            }
            
    // ¿Qué número de canal estamos viendo dentro de la lista?
    // Calcula la posición. Por ejemplo: Canal 5 de 120. (Le sumamos 1 porque las listas empiezan en 0).
    val numeroCanalActual =
            remember(canalesCategoria, idTransmision) {
                canalesCategoria.indexOfFirst { it.id_transmision == idTransmision } + 1
            }

    // ========================================================================
    // 4. EL REINICIO AUTOMÁTICO (Efecto de Cambio de Canal)
    // ========================================================================
    // 'LaunchedEffect' vigila la variable 'idTransmision'. 
    // Cada vez que cambiamos de canal (el ID cambia), este bloque de código se ejecuta.
    // DELEGAMOS 100% el estado de carga y el volumen al motor ExoPlayer 
    // para evitar bloqueos y pantallas mudas.
    LaunchedEffect(idTransmision) {
        mostrarControles = false
        mostrarPanelCanales = false

        // Si es un cambio de canal por zapping (no es la primera carga), 
        // siempre forzamos el spinner de sintonización de inmediato.
        val yaListo = primeraCarga && (reproductorExo?.playbackState == androidx.media3.common.Player.STATE_READY)
        if (!yaListo) {
            mostrarObturador = true
            estaCargando = true
        } else {
            mostrarObturador = false
            estaCargando = false
        }
        primeraCarga = false

        // En TV, mostrar la barra de info del canal brevemente
        if (esTV) {
            mostrarInfoCanal = true
            enfocarBarra = false
        }
    }

    // Auto-ocultar controles / info bar
    LaunchedEffect(mostrarControles, estaReproduciendo) {
        if (mostrarControles && estaReproduciendo) {
            delay(5000)
            mostrarControles = false
        }
    }

    LaunchedEffect(mostrarInfoCanal) {
        if (mostrarInfoCanal) {
            delay(6000)
            mostrarInfoCanal = false
            enfocarBarra = false
        }
    }

    // Safety-net: si el video no arranca en 8s, forzar reconexión
    LaunchedEffect(idTransmision) {
        delay(8000)
        if (mostrarObturador || estaCargando) {
            reproductorExo?.let { exo ->
                exo.stop()
                exo.prepare()
                exo.playWhenReady = true
            }
        }
    }

    // EL VIGILANTE DEL VIDEO (Listener):
    // El reproductor "ExoPlayer" no es de Compose, es una herramienta clásica de Android.
    // Este "oyente" está pegado al reproductor avisándonos de todo lo que pasa:
    // ¿Se congeló por mal internet? -> `STATE_BUFFERING` (Mostramos la ruedita de carga).
    // ¿Ya cargó el video? -> `STATE_READY` (Ocultamos la ruedita).
    DisposableEffect(reproductorExo) {
        val alcanceExo = kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Main)
        var trabajoRevelar: kotlinx.coroutines.Job? = null
        val oyente =
                object : androidx.media3.common.Player.Listener {
                    override fun onPlaybackStateChanged(estado: Int) {
                        estaCargando = estado == androidx.media3.common.Player.STATE_BUFFERING
                        if (estado == androidx.media3.common.Player.STATE_BUFFERING) {
                            // Solo mostrar obturador si ha pasado más de 400ms.
                            // NO silenciamos el audio — el obturador visual ya cubre la imagen.
                            trabajoRevelar?.cancel()
                            trabajoRevelar =
                                    alcanceExo.launch {
                                        kotlinx.coroutines.delay(400)
                                        mostrarObturador = true
                                    }
                        } else if (estado == androidx.media3.common.Player.STATE_READY) {
                            // Canal listo: garantizar audio activo.
                            // Esperamos a onRenderedFirstFrame para ocultar el obturador y evitar el destello negro.
                            trabajoRevelar?.cancel()
                            reproductorExo?.volume = 1f  // Garantizar siempre audio activo al volver
                            // Fallback infalible: si el canal ya está listo, quitamos el obturador de carga
                            // por si onRenderedFirstFrame no se dispara o se retrasa.
                            mostrarObturador = false
                            estaCargando = false
                        }
                    }
                    override fun onIsPlayingChanged(reproduciendo: Boolean) {
                        estaReproduciendo = reproduciendo
                        if (reproduciendo) {
                            estaCargando = false
                            mostrarObturador = false
                            trabajoRevelar?.cancel()
                        }
                    }
                    override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
                        estaCargando = false
                        mostrarObturador = false
                        trabajoRevelar?.cancel()
                    }
                    override fun onRenderedFirstFrame() {
                        // El primer frame ya está en pantalla: ocultar obturador de inmediato.
                        // No hay razón para esperar — onRenderedFirstFrame garantiza imagen
                        // visible.
                        trabajoRevelar?.cancel()
                        mostrarObturador = false
                        estaCargando = false
                        reproductorExo?.volume = 1f
                        if (esTV) {
                            mostrarInfoCanal = true
                        } else {
                            mostrarControles = true
                        }
                    }
                }
        reproductorExo?.addListener(oyente)
        onDispose {
            trabajoRevelar?.cancel()
            alcanceExo.cancel()
            reproductorExo?.removeListener(oyente)
        }
    }

    val vistaVideo =
            @Composable
            {
                val contexto =
                        androidx.compose.ui.platform.LocalContext.current as android.app.Activity
                Box(
                        modifier =
                                Modifier.fillMaxSize()
                                        .background(Color.Black)
                                        .then(
                                                if (!mostrarPanelCanales) {
                                                    Modifier.focusRequester(requeridorFoco)
                                                            .focusable()
                                                            .onKeyEvent { evento ->
                                                                if (evento.type == KeyEventType.KeyUp
                                                                ) {
                                                                    when (evento.key) {
                                                                        Key.DirectionUp -> {
                                                                            if (esTV) {
                                                                                alAnterior()
                                                                                mostrarInfoCanal = true
                                                                                enfocarBarra = false
                                                                            } else alAnterior()
                                                                            true
                                                                        }
                                                                        Key.DirectionDown -> {
                                                                            if (esTV) {
                                                                                alSiguiente()
                                                                                mostrarInfoCanal = true
                                                                                enfocarBarra = false
                                                                            } else alSiguiente()
                                                                            true
                                                                        }
                                                                        Key.DirectionCenter,
                                                                        Key.Enter,
                                                                        Key.NumPadEnter -> {
                                                                            if (esTV) {
                                                                                mostrarInfoCanal = true
                                                                                enfocarBarra = true
                                                                                mostrarPanelCanales = false
                                                                            } else {
                                                                                mostrarAjustes = true
                                                                            }
                                                                            true
                                                                        }
                                                                        Key.DirectionLeft -> {
                                                                            if (esTV) {
                                                                                if (mostrarInfoCanal) {
                                                                                    if (!enfocarBarra) {
                                                                                        enfocarBarra = true
                                                                                        true
                                                                                    } else {
                                                                                        false // No consumir evento: permitir navegar por los iconos
                                                                                    }
                                                                                } else {
                                                                                    mostrarPanelCanales = true
                                                                                    true
                                                                                }
                                                                            } else {
                                                                                contexto.finish()
                                                                                true
                                                                            }
                                                                        }
                                                                        Key.DirectionRight -> {
                                                                            if (esTV) {
                                                                                if (mostrarInfoCanal) {
                                                                                    if (!enfocarBarra) {
                                                                                        enfocarBarra = true
                                                                                        true
                                                                                    } else {
                                                                                        false // No consumir evento: permitir navegar por los iconos
                                                                                    }
                                                                                } else {
                                                                                    mostrarInfoCanal = true
                                                                                    enfocarBarra = true
                                                                                    true
                                                                                }
                                                                            } else {
                                                                                mostrarControles = true
                                                                                true
                                                                            }
                                                                        }
                                                                        Key.Back -> {
                                                                            contexto.finish()
                                                                            true
                                                                        }
                                                                        else -> false
                                                                    }
                                                                } else {
                                                                    false
                                                                }
                                                            }
                                                } else Modifier
                                        )
                                        .pointerInput(Unit) {
                                            detectTapGestures(
                                                    onTap = {
                                                        if (esTV) {
                                                            mostrarInfoCanal = !mostrarInfoCanal
                                                            if (mostrarInfoCanal) enfocarBarra = true
                                                        } else {
                                                            mostrarControles = !mostrarControles
                                                        }
                                                    }
                                            )
                                        }
                                        .pointerInput(Unit) {
                                            detectHorizontalDragGestures { _, cantidadArrastre ->
                                                if (cantidadArrastre > 50) alAnterior()
                                                else if (cantidadArrastre < -50) alSiguiente()
                                            }
                                        }
                ) {
                    if (reproductorExo != null) {
                        // Referencia a la vista para poder llamar requestLayout() desde el listener
                        val refVista = remember { mutableStateOf<PlayerView?>(null) }

                        // Cuando el nuevo canal reporta sus dimensiones reales, forzamos re-medición
                        // de la vista. Esto corrige el bug donde canales con resolución no estándar
                        // (ej. 576i) "contagiaban" las dimensiones a los canales siguientes.
                        DisposableEffect(reproductorExo) {
                            val escucha = object : Player.Listener {
                                override fun onVideoSizeChanged(videoSize: VideoSize) {
                                    refVista.value?.requestLayout()
                                }
                            }
                            reproductorExo.addListener(escucha)
                            onDispose { reproductorExo.removeListener(escucha) }
                        }

                        // LA PANTALLA DONDE SE PROYECTA (AndroidView):
                        // "PlayerView" es un componente antiguo de Android. Jetpack Compose no sabe
                        // cómo dibujar videos directamente todavía. "AndroidView" es como un "agujero"
                        // que hacemos en Compose para meter el reproductor clásico adentro.
                        AndroidView(
                                factory = { contextoView ->
                                    PlayerView(contextoView).apply {
                                        this.player = reproductorExo
                                        // Inicializar tag en el factory para que la primera
                                        // ejecución de update{} vea el mismo idTransmision y
                                        // no haga player=null→player=reproductorExo, que causa
                                        // un destello negro al abrir fullscreen desde el mini player.
                                        tag = idTransmision
                                        useController = false
                                        controllerAutoShow = false
                                        setKeepContentOnPlayerReset(true)
                                        setShutterBackgroundColor(android.graphics.Color.BLACK)
                                    }.also { refVista.value = it }
                                },
                                modifier = Modifier.fillMaxSize(),
                                update = { vistaReproductor ->
                                    // Para que SurfaceView actualice su tamaño al cambiar de canal en pantalla completa,
                                    // verificamos si el idTransmision cambió usando el tag.
                                    val ultimoId = vistaReproductor.tag as? Int
                                    if (ultimoId != idTransmision || vistaReproductor.player != reproductorExo) {
                                        vistaReproductor.tag = idTransmision
                                        vistaReproductor.player = null
                                        vistaReproductor.player = reproductorExo
                                        vistaReproductor.requestLayout()
                                    }

                                    vistaReproductor.resizeMode =
                                            when (modoEscalado) {
                                                1 -> androidx.media3.ui.AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                                                2 -> androidx.media3.ui.AspectRatioFrameLayout.RESIZE_MODE_FILL
                                                else -> androidx.media3.ui.AspectRatioFrameLayout.RESIZE_MODE_FIT
                                            }
                                },
                                onRelease = { vistaReproductor ->
                                    vistaReproductor.player = null
                                }
                        )
                    }

                    // Obturador: oculta el glitch del primer frame con un revelado mínimo (casi
                    // instantáneo)
                    androidx.compose.animation.AnimatedVisibility(
                            visible = mostrarObturador || estaCargando,
                            enter =
                                    androidx.compose.animation.fadeIn(
                                            animationSpec = androidx.compose.animation.core.tween(0)
                                    ),
                            exit =
                                    androidx.compose.animation.fadeOut(
                                            animationSpec =
                                                    androidx.compose.animation.core.tween(350)
                                    )
                    ) {
                        Box(
                                modifier =
                                        Modifier.fillMaxSize()
                                                .background(Color.Black.copy(alpha = 0.6f)),
                                contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                CircularProgressIndicator(
                                        color = AcentoPremium,
                                        modifier = Modifier.size(64.dp),
                                        strokeWidth = 4.dp
                                )
                                Spacer(modifier = Modifier.height(24.dp))
                                Text(
                                        "Sintonizando...",
                                        color = TextoSecundarioPremium,
                                        fontWeight = FontWeight.Medium
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                        nombreCanal,
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold,
                                        style = MaterialTheme.typography.titleLarge
                                )
                            }
                        }
                    }

                    if (!enImagenEnImagen) {
                        if (esTV) {
                            // ═══════ INTERFAZ TV ═══════

                            // Barra de información del canal (abajo) con botones interactivos
                            Box(modifier = Modifier.align(Alignment.BottomCenter)) {
                                BarraInfoCanalTV(
                                        esVisible =
                                                (mostrarInfoCanal || mostrarControles) &&
                                                        !estaCargando &&
                                                        !mostrarObturador,
                                        solicitarFoco = enfocarBarra,
                                        nombreCanal = nombreCanal,
                                        logotipoCanal = logotipoCanal,
                                        nombreCategoria = nombreCategoria,
                                        numeroCanalActual = numeroCanalActual,
                                        totalCanales = totalCategoria,
                                        esFavorito =
                                                favoritos.any { it.idTransmision == idTransmision },
                                        modoEscalado = modoEscalado,
                                        reproductorExo = reproductorExo,
                                        alAlternarFavorito = {
                                            val canalActual =
                                                    RepositorioContenido.obtenerCanalCompleto(
                                                            idTransmision,
                                                            Canal(
                                                                    nombre = nombreCanal,
                                                                    id_transmision = idTransmision
                                                            )
                                                    )
                                            alAlternarFavorito(canalActual)
                                        },
                                        alCambiarEscalado = { nuevoModo ->
                                            modoEscalado = nuevoModo
                                            alCambiarEscalado(nuevoModo)
                                        },
                                        alAbrirListaCanales = {
                                            mostrarInfoCanal = false
                                            mostrarPanelCanales = true
                                        }
                                )
                            }

                            // Panel lateral de canales (izquierda)
                            PanelCanalesTV(
                                    esVisible = mostrarPanelCanales,
                                    canales = canalesCategoria,
                                    idCanalActual = idTransmision,
                                    nombreCategoria = nombreCategoria,
                                    alSeleccionarCanal = { canal -> alCambiarCanal(canal) },
                                    alCerrar = {
                                        mostrarPanelCanales = false
                                        // Devolver foco al reproductor
                                        try {
                                            requeridorFoco.requestFocus()
                                        } catch (_: Exception) {}
                                    }
                            )
                        } else {
                            // ═══════ INTERFAZ CELULAR ═══════
                            SuperposicionControlesReproductor(
                                    esVisible =
                                            mostrarControles && !estaCargando && !mostrarObturador,
                                    nombreCanal = nombreCanal,
                                    logotipoCanal = logotipoCanal,
                                    estaReproduciendo = estaReproduciendo,
                                    estaCargando = estaCargando,
                                    esFavorito =
                                            favoritos.any { it.idTransmision == idTransmision },
                                    alReproducirPausar = {
                                        reproductorExo?.let {
                                            if (it.isPlaying) it.pause() else it.play()
                                        }
                                    },
                                    alSiguiente = alSiguiente,
                                    alAnterior = alAnterior,
                                    alAbrirExterno = alAbrirExterno,
                                    alMostrarAjustes = { mostrarAjustes = true },
                                    alAlternarFavorito = {
                                        val canalActual =
                                                RepositorioContenido.obtenerCanalCompleto(
                                                        idTransmision,
                                                        Canal(
                                                                nombre = nombreCanal,
                                                                id_transmision = idTransmision
                                                        )
                                                )
                                        alAlternarFavorito(canalActual)
                                    }
                            )
                        }
                    }

                    // Notificación premium al alternar favorito
                    if (!enImagenEnImagen) {
                        NotificacionFavorito(
                                mensaje = mensajeFavorito,
                                alLimpiar = alLimpiarMensajeFavorito,
                                modifier = Modifier
                                        .align(Alignment.BottomCenter)
                                        .padding(bottom = 96.dp)
                        )
                    }
                }
            }

    Surface(modifier = Modifier.fillMaxSize(), color = FondoPremium) {
        if (esVertical && !enImagenEnImagen) {
            Column(modifier = Modifier.fillMaxSize()) {
                Box(modifier = Modifier.fillMaxWidth().aspectRatio(16f / 9f)) { vistaVideo() }

                val canalActual =
                        remember(idTransmision) {
                            RepositorioContenido.obtenerCanalCompleto(
                                    idTransmision,
                                    Canal(nombre = nombreCanal, id_transmision = idTransmision)
                            )
                        }
                val recomendados =
                        remember(idTransmision) {
                            RepositorioContenido.obtenerCanalesRecomendados(idTransmision)
                        }
                val nombreCat =
                        remember(idTransmision) {
                            RepositorioContenido.obtenerNombreCategoria(canalActual.id_categoria)
                        }
                val totalCat =
                        remember(idTransmision) {
                            RepositorioContenido.obtenerConteoCanalesCategoria(
                                    canalActual.id_categoria
                            )
                        }
                val contexto =
                        androidx.compose.ui.platform.LocalContext.current as android.app.Activity

                Column(modifier = Modifier.fillMaxSize()) {
                    // Cabecera funcional
                    Row(
                            modifier =
                                    Modifier.fillMaxWidth()
                                            .background(Color(0xFF1A1A1A))
                                            .clickable { contexto.finish() }
                                            .padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                                Icons.Default.ChevronLeft,
                                "Salir",
                                tint = Color.White,
                                modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                                text = nombreCat as String,
                                color = Color.White,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Surface(color = Color(0xFF333333), shape = RoundedCornerShape(12.dp)) {
                            Text(
                                    text = totalCat.toString(),
                                    color = Color.Gray,
                                    fontSize = 11.sp,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                            )
                        }
                    }

                    LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(bottom = 16.dp)
                    ) {
                        items(items = recomendados, key = { it.id_transmision }) { canal: Canal ->
                            ItemCanalListaReproductor(
                                    canal = canal,
                                    esActivo = canal.id_transmision == idTransmision,
                                    esFavorito =
                                            favoritos.any {
                                                it.idTransmision == canal.id_transmision
                                            },
                                    alHacerClick = { alCambiarCanal(canal) },
                                    alAlternarFavorito = { alAlternarFavorito(canal) }
                            )
                        }
                    }
                }
            }
        } else {
            vistaVideo()
        }

        // Hoja de ajustes modularizada (solo para celulares)
        if (!esTV) {
            HojaAjustesReproductor(
                    mostrar = mostrarAjustes,
                    alCerrar = { mostrarAjustes = false },
                    estadoHoja = estadoHojaAjustes,
                    velocidadActual = velocidadActual,
                    modoEscalado = modoEscalado,
                    nombreCanal = nombreCanal,
                    alCambiarVelocidad = { nuevaVel ->
                        velocidadActual = nuevaVel
                        alCambiarVelocidad(nuevaVel)
                    },
                    alCambiarEscalado = { nuevoModo ->
                        modoEscalado = nuevoModo
                        alCambiarEscalado(nuevoModo)
                    }
            )
        }
    }
}

@Composable
private fun NotificacionFavorito(
    mensaje: String?,
    alLimpiar: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (mensaje != null) {
        LaunchedEffect(mensaje) {
            kotlinx.coroutines.delay(2500)
            alLimpiar()
        }
    }
    val esFavorito = mensaje?.contains("Añadido") == true
    AnimatedVisibility(
        visible = mensaje != null,
        enter = fadeIn(tween(180)) + slideInVertically(initialOffsetY = { it / 2 }, animationSpec = tween(180)),
        exit = fadeOut(tween(280)),
        modifier = modifier
    ) {
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(50.dp))
                .background(Color(0xF0101020))
                .border(
                    1.dp,
                    if (esFavorito) com.iptv.fiber.interfaz.tema.AcentoPremium.copy(alpha = 0.4f)
                    else Color.White.copy(alpha = 0.15f),
                    RoundedCornerShape(50.dp)
                )
                .padding(horizontal = 24.dp, vertical = 13.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                imageVector = if (esFavorito) Icons.Default.Favorite else Icons.Outlined.FavoriteBorder,
                contentDescription = null,
                tint = if (esFavorito) com.iptv.fiber.interfaz.tema.AcentoPremium else Color.White.copy(alpha = 0.7f),
                modifier = Modifier.size(22.dp)
            )
            Text(
                text = mensaje ?: "",
                color = Color.White,
                fontWeight = FontWeight.SemiBold,
                fontSize = 15.sp,
                letterSpacing = 0.3.sp
            )
        }
    }
}
