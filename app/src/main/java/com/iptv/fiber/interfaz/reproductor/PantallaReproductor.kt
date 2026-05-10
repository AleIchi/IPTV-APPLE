package com.iptv.fiber.interfaz.reproductor

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import com.iptv.fiber.datos.modelo.Canal
import com.iptv.fiber.datos.repositorio.RepositorioContenido
import com.iptv.fiber.interfaz.tema.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.cancel
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.foundation.focusable
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import com.iptv.fiber.tv.componentes.BarraInfoCanalTV
import com.iptv.fiber.tv.componentes.PanelCanalesTV

/**
 * Pantalla principal del reproductor que organiza el video, los controles y la lista de canales.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PantallaReproductor(
    reproductorExo: ExoPlayer?,
    nombreCanal: String,
    logoCanal: String?,
    idTransmision: Int,
    favoritos: List<com.iptv.fiber.datos.local.base_datos.Favorito>,
    enPiP: Boolean = false,
    alSiguiente: () -> Unit,
    alAnterior: () -> Unit,
    alAbrirExterno: () -> Unit,
    alCambiarCanal: (Canal) -> Unit = {},
    alAlternarFavorito: (Canal) -> Unit = {},
    alCambiarVelocidad: (Float) -> Unit = {},
    alCambiarEscalado: (Int) -> Unit = {}
) {
    var mostrarControles by remember { mutableStateOf(true) }
    var estaReproduciendo by remember { mutableStateOf(true) }
    var estaCargando by remember { mutableStateOf(true) }
    var mostrarObturador by remember { mutableStateOf(true) }
    var mostrarAjustes by remember { mutableStateOf(false) }
    var velocidadActual by remember { mutableStateOf(1.0f) }
    var modoEscalado by remember { mutableStateOf(0) }
    val estadoHojaAjustes = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    // Estados específicos para TV
    var mostrarPanelCanales by remember { mutableStateOf(false) }
    var mostrarInfoCanal by remember { mutableStateOf(false) }

    val configuracion = LocalConfiguration.current
    val esVertical = configuracion.orientation == Configuration.ORIENTATION_PORTRAIT
    val uiMode = configuracion.uiMode
    val esTV = (uiMode and Configuration.UI_MODE_TYPE_MASK) == Configuration.UI_MODE_TYPE_TELEVISION

    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) { focusRequester.requestFocus() }

    // Datos del canal actual para la info bar TV
    val canalCompleto = remember(idTransmision) {
        RepositorioContenido.obtenerCanalCompleto(idTransmision, Canal(nombre = nombreCanal, id_transmision = idTransmision))
    }
    val canalesCategoria = remember(idTransmision) {
        RepositorioContenido.obtenerCanalesRecomendados(idTransmision)
    }
    val nombreCategoria = remember(idTransmision) {
        RepositorioContenido.obtenerNombreCategoria(canalCompleto.id_categoria)
    }
    val totalCategoria = remember(idTransmision) {
        RepositorioContenido.obtenerConteoCanalesCategoria(canalCompleto.id_categoria)
    }

    // Cada vez que cambia el canal: silenciar de inmediato y cerrar el panel de canales.
    // El obturador y estaCargando los gestiona directamente el listener de ExoPlayer.
    LaunchedEffect(idTransmision) {
        mostrarControles = false
        mostrarPanelCanales = false
        reproductorExo?.volume = 0f

        // En TV, mostrar la barra de info del canal brevemente
        if (esTV) {
            mostrarInfoCanal = true
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
        }
    }

    // Monitorear carga del video
    DisposableEffect(reproductorExo) {
        val alcanceExo = kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Main)
        var trabajoRevelar: kotlinx.coroutines.Job? = null
        val listener = object : androidx.media3.common.Player.Listener {
            override fun onPlaybackStateChanged(state: Int) {
                estaCargando = state == androidx.media3.common.Player.STATE_BUFFERING
                if (state == androidx.media3.common.Player.STATE_BUFFERING) {
                    // Solo mostrar obturador si ha pasado más de 400ms desde que comenzó el cambio.
                    // Esto evita que un rebuffering breve congele la imagen innecesariamente.
                    trabajoRevelar?.cancel()
                    trabajoRevelar = alcanceExo.launch {
                        kotlinx.coroutines.delay(400)
                        mostrarObturador = true
                        reproductorExo?.volume = 0f
                    }
                } else if (state == androidx.media3.common.Player.STATE_READY) {
                    // Si el player está listo, ocultar obturador inmediatamente.
                    trabajoRevelar?.cancel()
                    mostrarObturador = false
                    reproductorExo?.volume = 1f
                }
            }
            override fun onIsPlayingChanged(playing: Boolean) {
                estaReproduciendo = playing
            }
            override fun onRenderedFirstFrame() {
                // El primer frame ya está en pantalla: ocultar obturador de inmediato.
                // No hay razón para esperar — onRenderedFirstFrame garantiza imagen visible.
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
        reproductorExo?.addListener(listener)
        onDispose { 
            trabajoRevelar?.cancel()
            alcanceExo.cancel()
            reproductorExo?.removeListener(listener) 
        }
    }

    val vistaVideo = @Composable {
        val contexto = androidx.compose.ui.platform.LocalContext.current as android.app.Activity
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
                .then(
                    if (!mostrarPanelCanales) {
                        Modifier
                            .focusRequester(focusRequester)
                            .focusable()
                            .onKeyEvent { event ->
                                if (event.type == KeyEventType.KeyUp) {
                                    when (event.key) {
                                        Key.DirectionUp -> {
                                            if (esTV) {
                                                alSiguiente()
                                                mostrarInfoCanal = true
                                            } else alSiguiente()
                                            true
                                        }
                                        Key.DirectionDown -> {
                                            if (esTV) {
                                                alAnterior()
                                                mostrarInfoCanal = true
                                            } else alAnterior()
                                            true
                                        }
                                        Key.DirectionCenter, Key.Enter, Key.NumPadEnter -> {
                                            if (esTV) {
                                                // OK en TV: Mostrar la barra de información. Si ya está abierta, no hacer nada para no cerrarla por accidente.
                                                if (!mostrarInfoCanal) {
                                                    mostrarInfoCanal = true
                                                    mostrarPanelCanales = false
                                                }
                                            } else {
                                                mostrarAjustes = true
                                            }
                                            true
                                        }
                                        Key.DirectionLeft -> {
                                            if (esTV) {
                                                if (mostrarInfoCanal) {
                                                    false // No consumir evento: permitir navegar por los iconos
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
                                                    false // No consumir evento: permitir navegar por los iconos
                                                } else {
                                                    mostrarInfoCanal = true
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
                    detectTapGestures(onTap = {
                        if (esTV) {
                            mostrarInfoCanal = !mostrarInfoCanal
                        } else {
                            mostrarControles = !mostrarControles
                        }
                    })
                }
                .pointerInput(Unit) {
                    detectHorizontalDragGestures { _, dragAmount ->
                        if (dragAmount > 50) alAnterior()
                        else if (dragAmount < -50) alSiguiente()
                    }
                }
        ) {
            if (reproductorExo != null) {
                AndroidView(
                    factory = { ctx ->
                        PlayerView(ctx).apply {
                            this.player = reproductorExo
                            useController = false
                            controllerAutoShow = false
                            setKeepContentOnPlayerReset(false)
                            setShutterBackgroundColor(android.graphics.Color.BLACK)
                        }
                    },
                    modifier = Modifier.fillMaxSize(),
                    update = { playerView ->
                        playerView.resizeMode = when(modoEscalado) {
                            1 -> androidx.media3.ui.AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                            2 -> androidx.media3.ui.AspectRatioFrameLayout.RESIZE_MODE_FILL
                            else -> androidx.media3.ui.AspectRatioFrameLayout.RESIZE_MODE_FIT
                        }
                    }
                )
            }

            // Obturador: oculta el glitch del primer frame con un revelado mínimo (casi instantáneo)
            androidx.compose.animation.AnimatedVisibility(
                visible = mostrarObturador || estaCargando,
                enter = androidx.compose.animation.fadeIn(animationSpec = androidx.compose.animation.core.tween(0)),
                exit = androidx.compose.animation.fadeOut(animationSpec = androidx.compose.animation.core.tween(80))
            ) {
                Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.75f)), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(color = AcentoPremium, modifier = Modifier.size(64.dp), strokeWidth = 4.dp)
                        Spacer(modifier = Modifier.height(24.dp))
                        Text("Sintonizando...", color = TextoSecundarioPremium, fontWeight = FontWeight.Medium)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(nombreCanal, color = Color.White, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleLarge)
                    }
                }
            }

            if (!enPiP) {
                if (esTV) {
                    // ═══════ INTERFAZ TV ═══════

                    // Barra de información del canal (abajo) con botones interactivos
                    Box(modifier = Modifier.align(Alignment.BottomCenter)) {
                        BarraInfoCanalTV(
                            esVisible = (mostrarInfoCanal || mostrarControles) && !estaCargando && !mostrarObturador,
                            nombreCanal = nombreCanal,
                            logoCanal = logoCanal,
                            nombreCategoria = nombreCategoria,
                            numeroCanalActual = canalesCategoria.indexOfFirst { it.id_transmision == idTransmision } + 1,
                            totalCanales = totalCategoria,
                            esFavorito = favoritos.any { it.idTransmision == idTransmision },
                            modoEscalado = modoEscalado,
                            reproductorExo = reproductorExo,
                            alAlternarFavorito = {
                                val canalActual = RepositorioContenido.obtenerCanalCompleto(idTransmision, Canal(nombre = nombreCanal, id_transmision = idTransmision))
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
                        alSeleccionarCanal = { canal ->
                            alCambiarCanal(canal)
                        },
                        alCerrar = {
                            mostrarPanelCanales = false
                            // Devolver foco al reproductor
                            try { focusRequester.requestFocus() } catch (_: Exception) {}
                        }
                    )
                } else {
                    // ═══════ INTERFAZ CELULAR ═══════
                    SuperposicionControlesReproductor(
                        esVisible = mostrarControles && !estaCargando && !mostrarObturador,
                        nombreCanal = nombreCanal,
                        logoCanal = logoCanal,
                        estaReproduciendo = estaReproduciendo,
                        estaCargando = estaCargando,
                        esFavorito = favoritos.any { it.idTransmision == idTransmision },
                        alReproducirPausar = {
                            reproductorExo?.let { if (it.isPlaying) it.pause() else it.play() }
                        },
                        alSiguiente = alSiguiente,
                        alAnterior = alAnterior,
                        alAbrirExterno = alAbrirExterno,
                        alMostrarAjustes = { mostrarAjustes = true },
                        alAlternarFavorito = { 
                            val canalActual = RepositorioContenido.obtenerCanalCompleto(idTransmision, Canal(nombre = nombreCanal, id_transmision = idTransmision))
                            alAlternarFavorito(canalActual)
                        }
                    )
                }
            }
        }
    }

    Surface(modifier = Modifier.fillMaxSize(), color = FondoPremium) {
        if (esVertical && !enPiP) {
            Column(modifier = Modifier.fillMaxSize()) {
                Box(modifier = Modifier.fillMaxWidth().aspectRatio(16f / 9f)) {
                    vistaVideo()
                }

                val canalActual = remember(idTransmision) { RepositorioContenido.obtenerCanalCompleto(idTransmision, Canal(nombre = nombreCanal, id_transmision = idTransmision)) }
                val recomendados = remember(idTransmision) { RepositorioContenido.obtenerCanalesRecomendados(idTransmision) }
                val nombreCat = remember(idTransmision) { RepositorioContenido.obtenerNombreCategoria(canalActual.id_categoria) }
                val totalCat = remember(idTransmision) { RepositorioContenido.obtenerConteoCanalesCategoria(canalActual.id_categoria) }
                val contexto = androidx.compose.ui.platform.LocalContext.current as android.app.Activity

                Column(modifier = Modifier.fillMaxSize()) {
                    // Cabecera funcional
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFF1A1A1A))
                            .clickable { contexto.finish() }
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.ChevronLeft, "Salir", tint = Color.White, modifier = Modifier.size(24.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = nombreCat as String, color = Color.White, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.width(8.dp))
                        Surface(color = Color(0xFF333333), shape = RoundedCornerShape(12.dp)) {
                            Text(text = totalCat.toString(), color = Color.Gray, fontSize = 11.sp, modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp))
                        }
                    }

                    LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 16.dp)) {
                        items(items = recomendados) { canal: Canal ->
                            ItemCanalListaReproductor(
                                canal = canal,
                                esActivo = canal.id_transmision == idTransmision,
                                esFavorito = favoritos.any { it.idTransmision == canal.id_transmision },
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
