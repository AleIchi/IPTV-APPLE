package com.iptv.fiber.tv.componentes

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.iptv.fiber.interfaz.tema.FondoPremium
import androidx.compose.foundation.Canvas

@Composable
fun GuiaNavegacionTV(
    esVisible: Boolean
) {
    AnimatedVisibility(
        visible = esVisible,
        enter = fadeIn(),
        exit = fadeOut(),
        modifier = Modifier.fillMaxSize()
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.65f)),
            contentAlignment = Alignment.Center
        ) {
            // Contenedor principal para la guía
            Box(
                modifier = Modifier
                    .width(600.dp)
                    .height(400.dp)
            ) {
                // 1. D-Pad Gráfico en el Centro
                Box(
                    modifier = Modifier
                        .size(160.dp)
                        .align(Alignment.Center)
                        .clip(CircleShape)
                        .background(Color.DarkGray.copy(alpha = 0.8f)),
                    contentAlignment = Alignment.Center
                ) {
                    // Círculo central OK
                    Box(
                        modifier = Modifier
                            .size(60.dp)
                            .clip(CircleShape)
                            .background(Color.White),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "OK",
                            color = Color.Black,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                    }

                    // Flecha Arriba
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowUp,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .padding(8.dp)
                            .size(32.dp)
                    )

                    // Flecha Abajo
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowDown,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(8.dp)
                            .size(32.dp)
                    )

                    // Flecha Izquierda
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowLeft,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier
                            .align(Alignment.CenterStart)
                            .padding(8.dp)
                            .size(32.dp)
                    )

                    // Flecha Derecha
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowRight,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier
                            .align(Alignment.CenterEnd)
                            .padding(8.dp)
                            .size(32.dp)
                    )
                }

                // 2. Líneas punteadas y Textos Explicativos
                
                // Texto Arriba (Cambiar de canal)
                CajaTextoGuia(
                    texto = "Presiona ^ o v para\nCambiar rápidamente de canal",
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .offset(y = (-20).dp)
                )
                // Línea para arriba
                LineaPunteada(
                    inicio = Offset(300f, 60f), // desde texto arriba
                    fin = Offset(300f, 130f)    // hasta D-Pad arriba
                )

                // Texto Izquierda (Lista de canales)
                CajaTextoGuia(
                    texto = "Presiona < para\nVer la lista de canales",
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                        .offset(x = 10.dp)
                )
                // Línea para izquierda
                LineaPunteada(
                    inicio = Offset(200f, 200f), // desde texto izquierda
                    fin = Offset(230f, 200f)     // hasta D-Pad izquierda
                )

                // Texto Derecha / Centro (Opciones)
                CajaTextoGuia(
                    texto = "Presiona OK para\nVer más opciones",
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .offset(x = (-10).dp, y = 40.dp)
                )
                // Línea para centro
                LineaPunteada(
                    inicio = Offset(400f, 240f), // desde texto derecha
                    fin = Offset(330f, 200f)     // hasta D-Pad OK
                )
            }
        }
    }
}

@Composable
private fun CajaTextoGuia(texto: String, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(Color.Black.copy(alpha = 0.5f))
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Text(
            text = texto,
            color = Color.White,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
    }
}

@Composable
private fun LineaPunteada(inicio: Offset, fin: Offset) {
    Canvas(modifier = Modifier.fillMaxSize()) {
        drawLine(
            color = Color.Red.copy(alpha = 0.8f),
            start = inicio,
            end = fin,
            strokeWidth = 4f,
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
        )
        // Flecha al final de la línea
        drawCircle(
            color = Color.Red.copy(alpha = 0.8f),
            radius = 6f,
            center = fin
        )
    }
}
