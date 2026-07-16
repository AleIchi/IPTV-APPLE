package com.iptv.fiber.tv.componentes

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.iptv.fiber.interfaz.componentes.IconoTvPorDefecto
import com.iptv.fiber.interfaz.tema.AcentoPremium

/**
 * Barra inferior de información del canal para TV con botones interactivos.
 * Muestra el logo, nombre del canal y opciones de acción (Favoritos, escalado, Lista, Info EPG).
 */
@Composable
fun BarraInfoCanalTV(
    esVisible: Boolean,
    solicitarFoco: Boolean = true,
    nombreCanal: String,
    logotipoCanal: String?,
    nombreCategoria: String,
    numeroCanalActual: Int,
    totalCanales: Int,
    esFavorito: Boolean = false,
    modoEscalado: Int = 0,
    reproductorExo: androidx.media3.exoplayer.ExoPlayer? = null,
    alAlternarFavorito: () -> Unit = {},
    alCambiarEscalado: (Int) -> Unit = {},
    alAbrirListaCanales: () -> Unit = {}
) {
    var mostrarInfoTransmision by remember { mutableStateOf(false) }

    // Ocultar la información del stream si se oculta la barra
    LaunchedEffect(esVisible) {
        if (!esVisible) mostrarInfoTransmision = false
    }

    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.BottomCenter) {

        // Ventana de información de transmisión (aparece justo encima de la barra)
        AnimatedVisibility(
            visible = mostrarInfoTransmision && esVisible,
            enter = slideInVertically(tween(300)) { it } + fadeIn(tween(300)),
            exit = slideOutVertically(tween(300)) { it } + fadeOut(tween(300)),
            modifier = Modifier.padding(bottom = 120.dp) // Deja espacio para la barra inferior
        ) {
            InfoTransmisionTV(reproductorExo = reproductorExo)
        }

        // Barra Inferior Principal
        AnimatedVisibility(
            visible = esVisible,
            enter = slideInVertically(tween(300)) { it } + fadeIn(tween(300)),
            exit = slideOutVertically(tween(500)) { it } + fadeOut(tween(500))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Transparent,
                                Color.Black.copy(alpha = 0.8f),
                                Color.Black.copy(alpha = 0.95f)
                            )
                        )
                    )
                    .padding(horizontal = 40.dp, vertical = 20.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Logotipo del canal
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFF1A1A2E)),
                        contentAlignment = Alignment.Center
                    ) {
                        if (!logotipoCanal.isNullOrBlank()) {
                            val ctx = androidx.compose.ui.platform.LocalContext.current
                            AsyncImage(
                                model = coil.request.ImageRequest.Builder(ctx)
                                    .data(logotipoCanal)
                                    .size(128, 128)
                                    .build(),
                                contentDescription = null,
                                modifier = Modifier.fillMaxSize().padding(8.dp),
                                contentScale = ContentScale.Fit
                            )
                        } else {
                            IconoTvPorDefecto(modificador = Modifier.size(32.dp))
                        }
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    // Info del canal
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = nombreCanal,
                            color = Color.White,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = nombreCategoria,
                                color = Color.White.copy(alpha = 0.6f),
                                fontSize = 14.sp
                            )
                            if (totalCanales > 0) {
                                Text(
                                    text = "  •  Canal $numeroCanalActual de $totalCanales",
                                    color = Color.White.copy(alpha = 0.4f),
                                    fontSize = 14.sp
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.width(24.dp))

                    // Guía rápida de controles D-Pad
                    Column(
                        horizontalAlignment = Alignment.End,
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        // Fila Arriba/Abajo
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.KeyboardArrowUp,
                                contentDescription = null,
                                tint = Color.White.copy(alpha = 0.5f),
                                modifier = Modifier.size(16.dp)
                            )
                            Icon(
                                imageVector = Icons.Default.KeyboardArrowDown,
                                contentDescription = null,
                                tint = Color.White.copy(alpha = 0.5f),
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "Cambiar canal",
                                color = Color.White.copy(alpha = 0.4f),
                                fontSize = 11.sp
                            )
                        }

                        // Fila Izquierda
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.KeyboardArrowLeft,
                                contentDescription = null,
                                tint = Color.White.copy(alpha = 0.5f),
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "Lista de canales",
                                color = Color.White.copy(alpha = 0.4f),
                                fontSize = 11.sp
                            )
                        }

                        // Fila de selección.
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(16.dp)
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(Color.White.copy(alpha = 0.2f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "✓",
                                    color = Color.White.copy(alpha = 0.6f),
                                    fontSize = 6.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Text(
                                text = "Opciones / Seleccionar",
                                color = Color.White.copy(alpha = 0.4f),
                                fontSize = 11.sp
                            )
                        }

                        // Fila Atrás
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.ArrowBack,
                                contentDescription = null,
                                tint = Color.White.copy(alpha = 0.5f),
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "Cerrar menú / Salir",
                                color = Color.White.copy(alpha = 0.4f),
                                fontSize = 11.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(32.dp))

                    // Fila de botones interactivos
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val focusRequester = remember { FocusRequester() }

                        // Si la barra se hace visible y solicita foco, pedir foco en el primer botón automáticamente
                        LaunchedEffect(esVisible, solicitarFoco) {
                            if (esVisible && solicitarFoco) {
                                kotlinx.coroutines.delay(100)
                                try { focusRequester.requestFocus() } catch (_: Exception) {}
                            }
                        }

                        // 1) Favorito
                        BotonIconoTV(
                            icono = if (esFavorito) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                            colorTint = if (esFavorito) Color(0xFFFF2D55) else Color.White,
                            alSeleccionar = { alAlternarFavorito() },
                            modifier = Modifier.focusRequester(focusRequester),
                            alIzquierda = { alAbrirListaCanales() }
                        )

                        // 2) Ajuste de Pantalla
                        BotonIconoTV(
                            icono = Icons.Default.AspectRatio,
                            colorTint = Color.White,
                            alSeleccionar = {
                                val nuevoModo = (modoEscalado + 1) % 3
                                alCambiarEscalado(nuevoModo)
                            }
                        )

                        // 3) Lista de Canales
                        BotonIconoTV(
                            icono = Icons.Default.LiveTv,
                            colorTint = Color.White,
                            alSeleccionar = { alAbrirListaCanales() }
                        )

                        // 4) Información de la transmisión
                        BotonIconoTV(
                            icono = Icons.Default.Info,
                            colorTint = Color.White,
                            alSeleccionar = { mostrarInfoTransmision = !mostrarInfoTransmision }
                        )
                    }
                }
            }
        }
    }
}

/** Botón circular interactivo para la barra de información; resalta en azul al recibir foco del D-Pad. */
@Composable
private fun BotonIconoTV(
    icono: ImageVector,
    colorTint: Color,
    alSeleccionar: () -> Unit,
    modifier: Modifier = Modifier,
    alIzquierda: (() -> Unit)? = null
) {
    val interactionSource = remember { MutableInteractionSource() }
    val tieneFoco by interactionSource.collectIsFocusedAsState()

    Box(
        modifier = modifier
            .size(46.dp)
            .clip(RoundedCornerShape(23.dp))
            .onKeyEvent { event ->
                if (event.type == KeyEventType.KeyDown && event.key == Key.DirectionLeft && alIzquierda != null) {
                    alIzquierda()
                    true
                } else false
            }
            .background(
                if (tieneFoco) AcentoPremium else Color.White.copy(alpha = 0.1f)
            )
            .tvClickableWithLongClick(
                interactionSource = interactionSource,
                onClick = alSeleccionar
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icono,
            contentDescription = null,
            tint = if (tieneFoco) Color.White else colorTint.copy(alpha = 0.8f),
            modifier = Modifier.size(24.dp)
        )
    }
}

/** Tarjeta superpuesta con información técnica de la transmisión (resolución, códec, tasa de bits, FPS). */
@Composable
private fun InfoTransmisionTV(reproductorExo: androidx.media3.exoplayer.ExoPlayer?) {
    Column(
        modifier = Modifier
            .width(260.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFF16162A).copy(alpha = 0.95f))
            .padding(16.dp)
    ) {
        val videoFormat = reproductorExo?.videoFormat
        val audioFormat = reproductorExo?.audioFormat

        val resolucion = if (videoFormat != null) "${videoFormat.width}x${videoFormat.height}" else "—"
        val formatoVideo = videoFormat?.sampleMimeType?.replace("video/", "")?.uppercase() ?: "—"
        val formatoAudio = audioFormat?.sampleMimeType?.replace("audio/", "")?.uppercase() ?: "—"
        val tasaBits = reproductorExo?.videoFormat?.bitrate?.let {
            if (it > 0) "${it / 1000} kbps" else "Variable"
        } ?: "—"
        val fps = videoFormat?.frameRate?.let {
            if (it > 0) "${"%.1f".format(it)} fps" else "—"
        } ?: "—"

        Text(
            text = "DATOS TÉCNICOS",
            color = AcentoPremium,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp
        )

        Spacer(modifier = Modifier.height(10.dp))

        FilaInfoTransmision("Resolución", resolucion)
        FilaInfoTransmision("Vídeo", formatoVideo)
        FilaInfoTransmision("Audio", formatoAudio)
        FilaInfoTransmision("Tasa", tasaBits)
        FilaInfoTransmision("Cuadros/s", fps)
    }
}

/** Fila etiqueta-valor para mostrar un dato técnico de la transmisión. */
@Composable
private fun FilaInfoTransmision(etiqueta: String, valor: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = etiqueta,
            color = Color.White.copy(alpha = 0.5f),
            fontSize = 13.sp
        )
        Text(
            text = valor,
            color = Color.White,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium
        )
    }
}
