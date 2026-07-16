package com.iptv.fiber.interfaz.reproductor

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.iptv.fiber.interfaz.tema.*
import com.iptv.fiber.interfaz.componentes.IconoTvPorDefecto

/** Superposición de controles de reproducción (play/pausa, canal anterior/siguiente, favorito, ajustes). */
@Composable
fun SuperposicionControlesReproductor(
    esVisible: Boolean,
    nombreCanal: String,
    logotipoCanal: String?,
    estaReproduciendo: Boolean,
    estaCargando: Boolean,
    esFavorito: Boolean,
    alReproducirPausar: () -> Unit,
    alSiguiente: () -> Unit,
    alAnterior: () -> Unit,
    alAbrirExterno: () -> Unit,
    alMostrarAjustes: () -> Unit,
    alAlternarFavorito: () -> Unit
) {
    val contexto = androidx.compose.ui.platform.LocalContext.current as android.app.Activity
    val configuracion = LocalConfiguration.current
    val esVertical = configuracion.orientation == android.content.res.Configuration.ORIENTATION_PORTRAIT

    Box(modifier = Modifier.fillMaxSize()) {
        AnimatedVisibility(
            visible = esVisible, 
            enter = fadeIn(), 
            exit = fadeOut(),
            modifier = Modifier.fillMaxSize()
        ) {
            Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.5f))) {
                
                // Barra superior: volver, imagen en imagen, favorito y ajustes
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = { contexto.finish() }) {
                        Icon(Icons.Default.ChevronLeft, "Volver", tint = Color.White, modifier = Modifier.size(28.dp))
                    }
                    
                    Spacer(modifier = Modifier.weight(1f))
                    
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        IconButton(onClick = {
                            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                                contexto.enterPictureInPictureMode(android.app.PictureInPictureParams.Builder().build())
                            }
                        }) {
                            Icon(Icons.Default.PictureInPictureAlt, "Mini reproductor", tint = Color.White, modifier = Modifier.size(22.dp))
                        }
                        IconButton(onClick = { alAlternarFavorito() }) {
                            Icon(
                                imageVector = if (esFavorito) Icons.Default.Favorite else Icons.Outlined.FavoriteBorder,
                                contentDescription = "Favorito",
                                tint = if (esFavorito) AcentoPremium else Color.White,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        IconButton(onClick = alMostrarAjustes) {
                            Icon(Icons.Default.MoreVert, "Ajustes", tint = Color.White, modifier = Modifier.size(22.dp))
                        }
                    }
                }

                // Botones centrales de canal anterior, reproducir/pausar y canal siguiente
                Row(
                    modifier = Modifier.align(Alignment.Center),
                    horizontalArrangement = Arrangement.spacedBy(48.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = alAnterior, modifier = Modifier.size(40.dp)) {
                        Icon(Icons.Default.SkipPrevious, "Canal Anterior", tint = Color.White, modifier = Modifier.fillMaxSize())
                    }
                    
                    if (!estaCargando) {
                        IconButton(onClick = alReproducirPausar, modifier = Modifier.size(56.dp)) {
                            Icon(
                                imageVector = if (estaReproduciendo) Icons.Default.Pause else Icons.Default.PlayArrow,
                                contentDescription = "Reproducir/Pausar",
                                tint = Color.White,
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    } else {
                        Spacer(modifier = Modifier.size(56.dp))
                    }

                    IconButton(onClick = alSiguiente, modifier = Modifier.size(40.dp)) {
                        Icon(Icons.Default.SkipNext, "Canal Siguiente", tint = Color.White, modifier = Modifier.fillMaxSize())
                    }
                }

                // Barra inferior con logotipo, nombre y pantalla completa
                Row(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.Bottom,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    // Información del canal con logotipo y nombre
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f, fill = false)
                    ) {
                        if (logotipoCanal != null) {
                            AsyncImage(
                                model = logotipoCanal,
                                contentDescription = null,
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(RoundedCornerShape(8.dp)),
                                contentScale = ContentScale.Fit
                            )
                        } else {
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color.White.copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                IconoTvPorDefecto(modificador = Modifier.scale(0.7f))
                            }
                        }
                        Spacer(modifier = Modifier.width(14.dp))
                        Text(
                            text = nombreCanal,
                            fontSize = 19.sp,
                            color = Color.White,
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = 0.5.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    // Botón de pantalla completa
                    IconButton(
                        onClick = {
                            contexto.requestedOrientation = if (esVertical) {
                                android.content.pm.ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
                            } else {
                                android.content.pm.ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
                            }
                        },
                        modifier = Modifier.size(36.dp).background(Color.Black.copy(alpha = 0.6f), CircleShape)
                    ) {
                        Icon(
                            imageVector = if (esVertical) Icons.Default.Fullscreen else Icons.Default.FullscreenExit,
                            contentDescription = "Pantalla completa",
                            tint = Color.White,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
            }
        }

    }
}
