package com.iptv.fiber.interfaz.componentes

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.SubcomposeAsyncImage
import com.iptv.fiber.interfaz.tema.AcentoPremium

import com.iptv.fiber.datos.modelo.EPG

import androidx.compose.material.icons.filled.Tv

/** Icono de televisión estilizado usado como marcador de posición cuando no hay logotipo disponible. */
@Composable
fun IconoTvPorDefecto(modificador: Modifier = Modifier) {
    BoxWithConstraints(
        modifier = modificador,
        contentAlignment = Alignment.Center
    ) {
        val totalHeight = maxHeight
        val size = if (totalHeight != androidx.compose.ui.unit.Dp.Unspecified && totalHeight > 0.dp) {
            if (totalHeight < 35.dp) {
                totalHeight // Respetar tamaños muy pequeños (ej. 22.dp en el panel lateral)
            } else if (totalHeight < 100.dp) {
                36.dp // Tamaño fijo ideal para listas compactas (evita tamaños disparejos)
            } else {
                64.dp // Tamaño fijo ideal para tarjetas grandes y carruseles
            }
        } else {
            40.dp
        }

        val antennaSize = size * 0.35f
        val tvHeight = size * 0.65f
        val tvWidth = tvHeight * 1.4f
        val fontSize = (tvHeight.value * 0.5f).sp
        val roundedCorner = (tvHeight.value * 0.18f).dp

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Antenas simplificadas y centradas
            Icon(
                imageVector = Icons.Default.KeyboardArrowDown,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(antennaSize)
            )
            // Cuerpo de la TV
            Box(
                modifier = Modifier
                    .size(tvWidth, tvHeight)
                    .clip(RoundedCornerShape(roundedCorner))
                    .background(Color.White),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "TV",
                    color = Color.Black,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = fontSize
                )
            }
        }
    }
}

/**
 * Carga la imagen del logo de un canal con caché en memoria y disco.
 * Muestra [IconoTvPorDefecto] mientras carga o si hay error.
 * @param url URL del logo del canal.
 * @param nombreCanal Descripción accesible de la imagen.
 * @param tamano Tamaño en píxeles al que se decodifica la imagen (reduce uso de RAM).
 */
@Composable
fun ImagenCanal(url: String?, nombreCanal: String, tamano: Int, modificador: Modifier = Modifier) {
    SubcomposeAsyncImage(
        model = url,
        contentDescription = nombreCanal,
        modifier = modificador,
        contentScale = ContentScale.Fit,
        loading = { IconoTvPorDefecto() },
        error = { IconoTvPorDefecto() }
    )
}

/** Calcula el progreso actual del programa (0.0 a 1.0) para componentes compartidos. */
fun calcularProgresoEpg(epg: EPG): Float {
    try {
        val inicio = epg.marca_tiempo_inicio?.toLongOrNull() ?: return 0f
        val fin = epg.marca_tiempo_fin?.toLongOrNull() ?: return 0f
        val ahora = kotlinx.datetime.Clock.System.now().toEpochMilliseconds() / 1000
        
        if (ahora < inicio) return 0f
        if (ahora > fin) return 1f
        
        val duracionTotal = fin - inicio
        val tiempoTranscurrido = ahora - inicio
        
        return (tiempoTranscurrido.toFloat() / duracionTotal.toFloat()).coerceIn(0f, 1f)
    } catch (e: Exception) {
        return 0f
    }
}
