package com.iptv.fiber.tv.componentes

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.iptv.fiber.datos.modelo.Canal

/** Cabecera de la pantalla TV en Vivo con el nombre de la categoría activa y el conteo de canales. */
@Composable
fun EncabezadoTvEnVivo(
    categoria: String,
    totalCanales: Int
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(TemaTV.AltoHeader)
            .padding(horizontal = TemaTV.MargenPantalla),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(
                text = "TV en vivo",
                color = TemaTV.TextoPrincipal,
                fontSize = TemaTV.TituloPantalla,
                fontWeight = FontWeight.ExtraBold
            )
            Text(
                text = categoria,
                color = TemaTV.TextoSecundario,
                fontSize = TemaTV.Subtitulo,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        Surface(
            shape = RoundedCornerShape(999.dp),
            color = TemaTV.Superficie.copy(alpha = 0.86f),
            border = BorderStroke(1.dp, TemaTV.Linea)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 9.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(9.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Tv,
                    contentDescription = null,
                    tint = TemaTV.AcentoClaro,
                    modifier = Modifier.size(18.dp)
                )
                Text(
                    text = "$totalCanales canales",
                    color = TemaTV.TextoPrincipal,
                    fontSize = TemaTV.TextoControl,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

/** Chip de categoría seleccionable en TV; resalta con fondo de acento al estar enfocado o seleccionado. */
@Composable
fun ChipCategoriaTV(
    nombre: String,
    esSeleccionado: Boolean,
    alSeleccionar: () -> Unit,
    focusRequester: FocusRequester? = null,
    modifier: Modifier = Modifier
) {
    val fuenteInteraccion = remember { MutableInteractionSource() }
    val estaEnfocado by fuenteInteraccion.collectIsFocusedAsState()

    val colorFondo = when {
        estaEnfocado -> TemaTV.Acento
        esSeleccionado -> TemaTV.Acento.copy(alpha = 0.72f)
        else -> TemaTV.Superficie.copy(alpha = 0.78f)
    }
    val colorBorde = when {
        estaEnfocado -> TemaTV.AcentoClaro
        esSeleccionado -> TemaTV.Acento.copy(alpha = 0.95f)
        else -> TemaTV.Linea
    }
    val colorTexto = if (estaEnfocado || esSeleccionado) {
        TemaTV.TextoPrincipal
    } else {
        TemaTV.TextoSecundario
    }

    Surface(
        modifier = modifier
            .height(42.dp)
            .then(if (focusRequester != null) Modifier.focusRequester(focusRequester) else Modifier)
            .clip(RoundedCornerShape(999.dp))
            .tvClickableWithLongClick(
                interactionSource = fuenteInteraccion,
                onClick = alSeleccionar
            ),
        shape = RoundedCornerShape(999.dp),
        color = colorFondo,
        border = BorderStroke(if (estaEnfocado) 2.dp else 1.dp, colorBorde),
        tonalElevation = if (estaEnfocado) 6.dp else 0.dp
    ) {
        Box(
            modifier = Modifier.padding(horizontal = 22.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = nombre,
                color = colorTexto,
                fontSize = TemaTV.TextoControl,
                fontWeight = if (esSeleccionado || estaEnfocado) FontWeight.Bold else FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

/** Pantalla de estado (cargando/error/vacío) centrada con [titulo], [subtitulo] y botón de [textoAccion] opcional. */
@Composable
fun EstadoPantallaTV(
    titulo: String,
    subtitulo: String,
    mostrarAccion: Boolean = false,
    textoAccion: String = "",
    alAccion: () -> Unit = {},
    requeridorFocoContenido: FocusRequester? = null
) {
    val fuenteInteraccion = remember { MutableInteractionSource() }
    val estaEnfocado by fuenteInteraccion.collectIsFocusedAsState()

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            modifier = Modifier
                .width(430.dp)
                .then(
                    if (!mostrarAccion && requeridorFocoContenido != null) {
                        Modifier
                            .focusRequester(requeridorFocoContenido)
                            .focusable(interactionSource = fuenteInteraccion)
                    } else Modifier
                )
                .border(
                    width = if (!mostrarAccion && estaEnfocado) 2.dp else 1.dp,
                    color = if (!mostrarAccion && estaEnfocado) TemaTV.AcentoClaro else TemaTV.Linea,
                    shape = RoundedCornerShape(22.dp)
                ),
            shape = RoundedCornerShape(22.dp),
            color = TemaTV.FondoElevado.copy(alpha = 0.94f),
            tonalElevation = 8.dp
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.padding(horizontal = 32.dp, vertical = 28.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(999.dp),
                    color = if (!mostrarAccion && estaEnfocado) TemaTV.Acento.copy(alpha = 0.3f) else TemaTV.Acento.copy(alpha = 0.16f)
                ) {
                    Icon(
                        imageVector = Icons.Default.Tv,
                        contentDescription = null,
                        tint = if (!mostrarAccion && estaEnfocado) Color.White else TemaTV.AcentoClaro,
                        modifier = Modifier
                            .padding(14.dp)
                            .size(28.dp)
                    )
                }

                Text(
                    text = titulo,
                    color = TemaTV.TextoPrincipal,
                    fontSize = 19.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = subtitulo,
                    color = TemaTV.TextoSecundario,
                    fontSize = TemaTV.Subtitulo
                )
                if (mostrarAccion) {
                    Spacer(modifier = Modifier.height(4.dp))
                    val fuenteInteraccionBoton = remember { MutableInteractionSource() }
                    val botonEnFoco by fuenteInteraccionBoton.collectIsFocusedAsState()
                    val requeridorFocoBoton = requeridorFocoContenido ?: remember { FocusRequester() }

                    LaunchedEffect(requeridorFocoBoton) {
                        try { requeridorFocoBoton.requestFocus() } catch (_: Exception) {}
                    }

                    Surface(
                        shape = RoundedCornerShape(TemaTV.RedondeoControl),
                        color = if (botonEnFoco) TemaTV.AcentoClaro else TemaTV.Acento,
                        border = if (botonEnFoco) BorderStroke(2.dp, Color.White) else null,
                        modifier = Modifier
                            .focusRequester(requeridorFocoBoton)
                            .tvClickableWithLongClick(
                                interactionSource = fuenteInteraccionBoton,
                                onClick = alAccion
                            )
                    ) {
                        Text(
                            text = textoAccion,
                            color = Color.White,
                            fontSize = TemaTV.TextoControl,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp)
                        )
                    }
                }
            }
        }
    }
}

/** Fila compacta de canal para la lista de TV en Vivo; resalta en azul al recibir foco y soporta long-click. */
@Composable
fun FilaCanalTV(
    canal: Canal,
    esFavorito: Boolean,
    esSeleccionado: Boolean,
    modifier: Modifier = Modifier,
    alHacerClick: () -> Unit,
    alHacerLongClick: (() -> Unit)? = null
) {
    val fuenteInteraccion = remember { MutableInteractionSource() }
    val estaEnfocado by fuenteInteraccion.collectIsFocusedAsState()

    val colorFondo = when {
        estaEnfocado -> TemaTV.Acento
        esSeleccionado -> Color.White.copy(alpha = 0.15f)
        else -> Color.White.copy(alpha = 0.04f)
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(52.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(colorFondo)
            .tvClickableWithLongClick(
                interactionSource = fuenteInteraccion,
                onClick = alHacerClick,
                onLongClick = alHacerLongClick
            )
            .padding(horizontal = 12.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            // Logotipo pequeño o icono
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .padding(2.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color.Black.copy(alpha = 0.3f)),
                contentAlignment = Alignment.Center
            ) {
                if (!canal.icono_transmision.isNullOrBlank()) {
                    val contexto = androidx.compose.ui.platform.LocalContext.current
                    // allowHardware=true (por defecto): el bitmap vive en VRAM y se dibuja
                    // directamente por la GPU, reduciendo carga de CPU y uso de RAM de heap.
                    val request = remember(canal.icono_transmision) {
                        coil.request.ImageRequest.Builder(contexto)
                            .data(canal.icono_transmision)
                            .crossfade(false)
                            .size(96)
                            .memoryCachePolicy(coil.request.CachePolicy.ENABLED)
                            .diskCachePolicy(coil.request.CachePolicy.ENABLED)
                            .build()
                    }

                    coil.compose.AsyncImage(
                        model = request,
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize().padding(4.dp),
                        contentScale = androidx.compose.ui.layout.ContentScale.Fit
                    )
                } else {
                    Icon(Icons.Default.Tv, contentDescription = null, tint = Color.White.copy(alpha = 0.5f), modifier = Modifier.size(20.dp))
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Nombre del canal
            Text(
                text = canal.nombre,
                color = if (estaEnfocado || esSeleccionado) Color.White else Color.White.copy(alpha = 0.8f),
                fontSize = 14.sp,
                fontWeight = if (estaEnfocado || esSeleccionado) FontWeight.Bold else FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )

            // Favorito
            if (esFavorito) {
                Icon(
                    imageVector = Icons.Default.Favorite,
                    contentDescription = "Favorito",
                    tint = if (estaEnfocado) Color.White else Color(0xFFFF2D55),
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}
