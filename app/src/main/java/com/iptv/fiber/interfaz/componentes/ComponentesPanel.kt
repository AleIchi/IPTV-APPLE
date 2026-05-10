package com.iptv.fiber.interfaz.componentes

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.iptv.fiber.interfaz.tema.*

/** Tarjeta de acceso rápido para el dashboard principal. */
@Composable
fun TarjetaDashboard(
    titulo: String,
    icono: ImageVector,
    color: Color,
    alHacerTap: () -> Unit,
    modificador: Modifier = Modifier,
    subtitulo: String? = null
) {
    Card(
        modifier = modificador
            .height(160.dp)
            .clickable(onClick = alHacerTap),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = SuperficiePremium),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            // Fondo degradado de fondo
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                color.copy(alpha = 0.1f),
                                color.copy(alpha = 0.05f)
                            )
                        )
                    )
            )

            // Contenido
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Contenedor del ícono
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(color.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icono,
                        contentDescription = null,
                        tint = color,
                        modifier = Modifier.size(28.dp)
                    )
                }

                // Texto
                Column {
                    Text(
                        text = titulo,
                        style = MaterialTheme.typography.titleLarge,
                        color = TextoPrimarioPremium,
                        fontWeight = FontWeight.Bold
                    )
                    if (subtitulo != null) {
                        Text(
                            text = subtitulo,
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextoSecundarioPremium
                        )
                    }
                }
            }

            // Línea de acento inferior
            Box(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .fillMaxWidth()
                    .height(4.dp)
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(color, color.copy(alpha = 0.3f))
                        )
                    )
            )
        }
    }
}

/** Barra de navegación inferior con estilo moderno. */
@Composable
fun BarraLateralModerna(
    tabSeleccionado: Int,
    alSeleccionarTab: (Int) -> Unit,
    elementos: List<Triple<String, ImageVector, String>>,
    modificador: Modifier = Modifier
) {
    // Línea divisora superior muy ligera
    Box(modifier = Modifier.fillMaxWidth().height(0.5.dp).background(Color.White.copy(alpha = 0.05f)))

    Row(
        modifier = modificador
            .background(Color(0xFF0F1014).copy(alpha = 0.95f)) // Fondo oscuro, ligeramente transparente
            .padding(vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        elementos.forEachIndexed { indice, elemento ->
            val estaSeleccionado = tabSeleccionado == indice

            val colorPildora by animateColorAsState(
                targetValue = if (estaSeleccionado) Color.White.copy(alpha = 0.15f) else Color.Transparent,
                animationSpec = spring(stiffness = Spring.StiffnessMedium),
                label = "colorPildora_$indice"
            )
            val tinteIcono by animateColorAsState(
                targetValue = if (estaSeleccionado) AcentoPremium else Color.White,
                animationSpec = spring(stiffness = Spring.StiffnessMedium),
                label = "tinteIcono_$indice"
            )

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(12.dp))
                    .clickable { alSeleccionarTab(indice) }
                    .padding(vertical = 4.dp)
            ) {
                // Píldora con ícono centrado
                Box(
                    modifier = Modifier
                        .width(60.dp)
                        .height(32.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(colorPildora),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = elemento.second,
                        contentDescription = elemento.first,
                        tint = tinteIcono,
                        modifier = Modifier.size(24.dp)
                    )
                }
                // El texto solo aparece si está seleccionado
                if (estaSeleccionado) {
                    Spacer(modifier = Modifier.height(3.dp))
                    Text(
                        text = elemento.first,
                        style = MaterialTheme.typography.labelSmall,
                        color = AcentoPremium,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

/** Cabecera de sección con título y subtítulo. */
@Composable
fun CabeceraDashboard(
    titulo: String,
    subtitulo: String,
    modificador: Modifier = Modifier
) {
    Column(modifier = modificador) {
        Text(
            text = titulo,
            style = MaterialTheme.typography.headlineMedium,
            color = TextoPrimarioPremium
        )
        Text(
            text = subtitulo,
            style = MaterialTheme.typography.bodyLarge,
            color = TextoSecundarioPremium
        )
    }
}
