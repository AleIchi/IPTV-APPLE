package com.iptv.fiber.interfaz.componentes

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.iptv.fiber.interfaz.tema.AcentoPremium

import com.iptv.fiber.datos.modelo.EPG

/** Icono de televisión estilizado usado como marcador de posición (placeholder). */
@Composable
fun IconoTvPorDefecto(modificador: Modifier = Modifier) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = modificador
    ) {
        // Antenas simplificadas y centradas
        Icon(
            imageVector = Icons.Default.KeyboardArrowDown,
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier
                .size(24.dp) // Reducido para mejor balance
        )
        // Cuerpo de la TV
        Box(
            modifier = Modifier
                .size(50.dp, 34.dp) // Proporción más compacta
                .clip(RoundedCornerShape(6.dp))
                .background(Color.White),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "TV",
                color = Color.Black,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 18.sp
            )
        }
    }
}

/** Calcula el progreso actual del programa (0.0 a 1.0) para componentes compartidos. */
fun calcularProgresoEpg(epg: EPG): Float {
    try {
        val inicio = epg.marca_tiempo_inicio?.toLongOrNull() ?: return 0f
        val fin = epg.marca_tiempo_fin?.toLongOrNull() ?: return 0f
        val ahora = System.currentTimeMillis() / 1000
        
        if (ahora < inicio) return 0f
        if (ahora > fin) return 1f
        
        val duracionTotal = fin - inicio
        val tiempoTranscurrido = ahora - inicio
        
        return (tiempoTranscurrido.toFloat() / duracionTotal.toFloat()).coerceIn(0f, 1f)
    } catch (e: Exception) {
        return 0f
    }
}
