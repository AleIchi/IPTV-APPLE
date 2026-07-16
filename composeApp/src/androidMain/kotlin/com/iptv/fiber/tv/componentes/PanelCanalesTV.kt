package com.iptv.fiber.tv.componentes

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
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
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import com.iptv.fiber.interfaz.tema.AcentoPremium

/**
 * Panel lateral de canales para TV que se desliza desde la izquierda.
 * Muestra la lista de [canales] de la categoría actual y permite navegar con el D-Pad y seleccionar con OK.
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
        enter = slideInHorizontally(animationSpec = tween(200)) { -it } + fadeIn(tween(180)),
        exit = slideOutHorizontally(animationSpec = tween(200)) { -it } + fadeOut(tween(160))
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
                var ultimaNavegacion by remember { mutableLongStateOf(0L) }
                val indiceActual = remember(idCanalActual, canales) {
                    canales.indexOfFirst { it.id_transmision == idCanalActual }.coerceAtLeast(0)
                }

                // Scroll al canal actual centrado en la lista visible.
                LaunchedEffect(indiceActual) {
                    val visibles = estadoLista.layoutInfo.visibleItemsInfo.size.takeIf { it > 0 } ?: 6
                    estadoLista.scrollToItem((indiceActual - visibles / 2).coerceAtLeast(0))
                }

                LazyColumn(
                    state = estadoLista,
                    modifier = Modifier
                        .fillMaxSize()
                        .onKeyEvent { event ->
                            when {
                                (event.key == Key.DirectionDown || event.key == Key.DirectionUp)
                                    && event.type == KeyEventType.KeyDown -> {
                                    val ahora = System.currentTimeMillis()
                                    if (ahora - ultimaNavegacion < 150L) return@onKeyEvent true
                                    ultimaNavegacion = ahora
                                    false
                                }
                                event.type == KeyEventType.KeyUp && event.key == Key.DirectionRight -> {
                                    alCerrar(); true
                                }
                                else -> false
                            }
                        },
                    contentPadding = PaddingValues(bottom = 40.dp)
                ) {
                    itemsIndexed(
                        items = canales,
                        key = { _, canal -> canal.id_transmision },
                        contentType = { _, _ -> "ItemCanalTV" }
                    ) { index, canal ->
                        ItemCanalTV(
                            canal = canal,
                            esActivo = canal.id_transmision == idCanalActual,
                            numero = index + 1,
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

/** Cada ítem de canal en la lista lateral de TV. Se resalta con borde azul al recibir foco del D-Pad. */
@Composable
private fun ItemCanalTV(
    canal: Canal,
    esActivo: Boolean,
    numero: Int,
    alSeleccionar: () -> Unit
) {
    val requeridorFoco = remember { FocusRequester() }
    val fuenteInteraccion = remember { MutableInteractionSource() }
    val tieneFoco by fuenteInteraccion.collectIsFocusedAsState()

    // Si es el activo, pedir foco automáticamente
    LaunchedEffect(esActivo) {
        if (esActivo) {
            try { requeridorFoco.requestFocus() } catch (_: Exception) {}
        }
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .focusRequester(requeridorFoco)
            .tvClickableWithLongClick(
                interactionSource = fuenteInteraccion,
                onClick = alSeleccionar
            )
            .background(
                if (esActivo) AcentoPremium.copy(alpha = 0.15f)
                else if (tieneFoco) Color.White.copy(alpha = 0.08f)
                else Color.Transparent
            )
            .border(
                width = if (tieneFoco) 2.dp else 0.dp,
                color = if (tieneFoco) AcentoPremium else Color.Transparent,
                shape = RoundedCornerShape(8.dp)
            )
            .padding(horizontal = 16.dp, vertical = 8.dp),
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

        // Logotipo del canal
        Box(
            modifier = Modifier
                .size(46.dp, 36.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(Color(0xFF1A1A2E)),
            contentAlignment = Alignment.Center
        ) {
            if (!canal.icono_transmision.isNullOrBlank()) {
                val ctx = androidx.compose.ui.platform.LocalContext.current
                val req = remember(canal.icono_transmision) {
                    coil.request.ImageRequest.Builder(ctx)
                        .data(canal.icono_transmision)
                        .size(96)
                        .crossfade(false)
                        .memoryCachePolicy(coil.request.CachePolicy.ENABLED)
                        .build()
                }
                AsyncImage(
                    model = req,
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
