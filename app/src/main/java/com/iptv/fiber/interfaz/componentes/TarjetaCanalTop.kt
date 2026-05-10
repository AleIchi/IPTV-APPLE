package com.iptv.fiber.interfaz.componentes

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.SubcomposeAsyncImage
import com.iptv.fiber.datos.modelo.Canal
import com.iptv.fiber.interfaz.tema.FondoPremium
import com.iptv.fiber.interfaz.tema.SuperficiePremium
import com.iptv.fiber.interfaz.tema.TextoPrimarioPremium
import com.iptv.fiber.interfaz.tema.TextoSecundarioPremium
import com.iptv.fiber.interfaz.tema.AcentoPremium

import com.iptv.fiber.datos.modelo.EPG

@Composable
fun TarjetaCanalTop(
    canal: Canal,
    posicion: Int,
    alHacerClick: () -> Unit,
    programaActual: EPG? = null,
    modificador: Modifier = Modifier
) {
    Box(
        modifier = modificador
            .width(175.dp)
            .height(240.dp)
            .clickable { alHacerClick() }
    ) {
        Card(
            modifier = Modifier
                .fillMaxSize()
                .padding(start = 34.dp, bottom = 55.dp),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = SuperficiePremium)
        ) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                if (!canal.icono_transmision.isNullOrBlank()) {
                    SubcomposeAsyncImage(
                        model = canal.icono_transmision,
                        contentDescription = canal.nombre,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(20.dp),
                        contentScale = ContentScale.Fit,
                        loading = { IconoTvPorDefecto() },
                        error = { IconoTvPorDefecto() }
                    )
                } else {
                    IconoTvPorDefecto()
                }
            }
        }

        // Número ranking premium
        Text(
            text = posicion.toString(),
            style = TextStyle(
                fontSize = 120.sp,
                fontWeight = FontWeight.Black,
                fontStyle = FontStyle.Italic,
                brush = Brush.linearGradient(
                    colors = listOf(Color.White, AcentoPremium)
                ),
                shadow = androidx.compose.ui.graphics.Shadow(
                    color = Color.Black.copy(alpha = 0.5f),
                    offset = androidx.compose.ui.geometry.Offset(4f, 6f),
                    blurRadius = 15f
                )
            ),
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(bottom = 20.dp)
                .offset(x = (-6).dp)
        )

        // Info EPG debajo
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(start = 35.dp, bottom = 4.dp)
                .fillMaxWidth()
        ) {
            Text(
                text = canal.nombre,
                style = MaterialTheme.typography.titleSmall,
                color = Color.White,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            
            if (programaActual != null) {
                Text(
                    text = programaActual.titulo,
                    style = MaterialTheme.typography.labelSmall,
                    color = AcentoPremium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                
                // Pequeña barra de progreso sutil
                val progreso = calcularProgresoEpg(programaActual)
                if (progreso > 0f) {
                    LinearProgressIndicator(
                        progress = progreso,
                        modifier = Modifier.fillMaxWidth(0.6f).padding(top = 4.dp).height(2.dp).clip(RoundedCornerShape(1.dp)),
                        color = AcentoPremium,
                        trackColor = Color.White.copy(alpha = 0.1f)
                    )
                }
            } else {
                Text(
                    text = "TV en Vivo",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextoSecundarioPremium
                )
            }
        }
    }
}

@Composable
fun TarjetaCanalEstandar(
    canal: Canal,
    alHacerClick: () -> Unit,
    programaActual: EPG? = null,
    modificador: Modifier = Modifier
) {
    Column(
        modifier = modificador
            .width(155.dp)
            .clickable { alHacerClick() }
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .height(210.dp),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = SuperficiePremium)
        ) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                if (!canal.icono_transmision.isNullOrBlank()) {
                    SubcomposeAsyncImage(
                        model = canal.icono_transmision,
                        contentDescription = canal.nombre,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(22.dp),
                        contentScale = ContentScale.Fit,
                        loading = { IconoTvPorDefecto() },
                        error = { IconoTvPorDefecto() }
                    )
                } else {
                    IconoTvPorDefecto()
                }
            }
        }
        
        Spacer(modifier = Modifier.height(10.dp))
        
        Text(
            text = canal.nombre,
            style = MaterialTheme.typography.titleMedium,
            color = Color.White,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        
        if (programaActual != null) {
            Text(
                text = programaActual.titulo,
                style = MaterialTheme.typography.bodySmall,
                color = AcentoPremium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            
            val progreso = calcularProgresoEpg(programaActual)
            if (progreso > 0f) {
                LinearProgressIndicator(
                    progress = progreso,
                    modifier = Modifier.fillMaxWidth().padding(top = 6.dp).height(3.dp).clip(RoundedCornerShape(2.dp)),
                    color = AcentoPremium,
                    trackColor = Color.White.copy(alpha = 0.1f)
                )
            }
        } else {
            Text(
                text = "TV en Vivo",
                style = MaterialTheme.typography.bodySmall,
                color = TextoSecundarioPremium,
                maxLines = 1
            )
        }
    }
}
