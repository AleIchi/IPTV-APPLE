package com.iptv.fiber.tv.componentes

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.Divider
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
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
import com.iptv.fiber.datos.modelo.Canal
import com.iptv.fiber.interfaz.componentes.IconoTvPorDefecto
import androidx.compose.foundation.focusable
import com.iptv.fiber.interfaz.tema.AcentoPremium

/**
 * Panel lateral de canales para TV que se desliza desde la izquierda.
 * Muestra la lista de canales de la categoría actual, permite navegar
 * con el D-Pad y seleccionar un canal con OK.
 */
@Composable
fun PanelCanalesTV(
    esVisible: Boolean,
    canales: List<Canal>,
    idCanalActual: Int,
    nombreCategoria: String,
    alSeleccionarCanal: (Canal) -> Unit,
    alCerrar: () -> Unit
) {
    AnimatedVisibility(
        visible = esVisible,
        enter = slideInHorizontally(animationSpec = tween(300)) { -it } + fadeIn(tween(300)),
        exit = slideOutHorizontally(animationSpec = tween(300)) { -it } + fadeOut(tween(300))
    ) {
        Row(modifier = Modifier.fillMaxSize()) {
            // Panel lateral oscuro con la lista de canales
            Column(
                modifier = Modifier
                    .width(380.dp)
                    .fillMaxHeight()
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(
                                Color(0xF0101020),
                                Color(0xE0101020),
                                Color(0x80101020)
                            )
                        )
                    )
                    .padding(top = 24.dp)
            ) {
                // Cabecera con nombre de categoría
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.LiveTv,
                        contentDescription = null,
                        tint = AcentoPremium,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = nombreCategoria,
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    Surface(
                        color = Color.White.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = "${canales.size}",
                            color = Color.White.copy(alpha = 0.7f),
                            fontSize = 12.sp,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                }

                Divider(
                    color = Color.White.copy(alpha = 0.08f),
                    thickness = 1.dp,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Lista de canales
                val estadoLista = rememberLazyListState()
                val indiceActual = remember(idCanalActual, canales) {
                    canales.indexOfFirst { it.id_transmision == idCanalActual }.coerceAtLeast(0)
                }

                // Scroll automático al canal actual
                LaunchedEffect(indiceActual) {
                    estadoLista.animateScrollToItem(
                        index = (indiceActual - 2).coerceAtLeast(0)
                    )
                }

                LazyColumn(
                    state = estadoLista,
                    modifier = Modifier
                        .fillMaxSize()
                        .onKeyEvent { event ->
                            if (event.type == KeyEventType.KeyUp && event.key == Key.DirectionRight) {
                                alCerrar()
                                true
                            } else {
                                false
                            }
                        },
                    contentPadding = PaddingValues(bottom = 40.dp)
                ) {
                    items(items = canales) { canal ->
                        ItemCanalTV(
                            canal = canal,
                            esActivo = canal.id_transmision == idCanalActual,
                            numero = canales.indexOf(canal) + 1,
                            alSeleccionar = {
                                alSeleccionarCanal(canal)
                                alCerrar()
                            }
                        )
                    }
                }
            }

            // Zona derecha clickeable para cerrar
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Transparent)
            )
        }
    }
}

/**
 * Cada ítem de canal en la lista lateral de TV.
 * Se resalta con borde azul al recibir foco del D-Pad.
 */
@Composable
private fun ItemCanalTV(
    canal: Canal,
    esActivo: Boolean,
    numero: Int,
    alSeleccionar: () -> Unit
) {
    val focusRequester = remember { FocusRequester() }
    var tieneFoco by remember { mutableStateOf(false) }

    // Si es el activo, pedir foco automáticamente
    LaunchedEffect(esActivo) {
        if (esActivo) {
            try { focusRequester.requestFocus() } catch (_: Exception) {}
        }
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .focusRequester(focusRequester)
            .onFocusChanged { tieneFoco = it.isFocused }
            .onKeyEvent { event ->
                if (event.type == KeyEventType.KeyUp &&
                    (event.key == Key.DirectionCenter || event.key == Key.Enter || event.key == Key.NumPadEnter)
                ) {
                    alSeleccionar()
                    true
                } else false
            }
            .then(
                if (esActivo) Modifier.background(AcentoPremium.copy(alpha = 0.15f))
                else if (tieneFoco) Modifier.background(Color.White.copy(alpha = 0.08f))
                else Modifier
            )
            .then(
                if (tieneFoco) Modifier.border(
                    width = 2.dp,
                    color = AcentoPremium,
                    shape = RoundedCornerShape(8.dp)
                ) else Modifier
            )
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .focusable(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Número de canal
        Text(
            text = numero.toString().padStart(3, ' '),
            color = if (esActivo) AcentoPremium else Color.White.copy(alpha = 0.4f),
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.width(32.dp)
        )

        Spacer(modifier = Modifier.width(10.dp))

        // Logo del canal
        Box(
            modifier = Modifier
                .size(46.dp, 36.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(Color(0xFF1A1A2E)),
            contentAlignment = Alignment.Center
        ) {
            if (!canal.icono_transmision.isNullOrBlank()) {
                AsyncImage(
                    model = canal.icono_transmision,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize().padding(4.dp),
                    contentScale = ContentScale.Fit
                )
            } else {
                IconoTvPorDefecto(
                    modificador = Modifier.size(22.dp)
                )
            }
        }

        Spacer(modifier = Modifier.width(12.dp))

        // Nombre del canal
        Text(
            text = canal.nombre,
            color = if (esActivo) AcentoPremium else Color.White,
            fontWeight = if (esActivo) FontWeight.Bold else FontWeight.Normal,
            fontSize = 14.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )

        // Indicador de canal actual
        if (esActivo) {
            Spacer(modifier = Modifier.width(8.dp))
            Icon(
                imageVector = Icons.Default.PlayCircle,
                contentDescription = null,
                tint = AcentoPremium,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

