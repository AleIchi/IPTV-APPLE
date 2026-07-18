package com.iptv.fiber.interfaz.componentes

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.iptv.fiber.datos.modelo.Canal
import com.iptv.fiber.datos.modelo.EPG
import com.iptv.fiber.interfaz.tema.*

/** Tarjeta compacta que muestra un canal en listas con información de EPG. */
@Composable
fun TarjetaCanal(
    canal: Canal,
    alHacerClick: () -> Unit,
    programaActual: EPG? = null,
    esFavorito: Boolean = false,
    alHacerTapFavorito: () -> Unit = {},
    modificador: Modifier = Modifier
) {
    Card(
        modifier = modificador
            .fillMaxWidth()
            .height(115.dp)
            .padding(horizontal = 4.dp)
            .clickable { alHacerClick() },
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = SuperficiePremium)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Ícono del canal con marco premium
            Box(
                modifier = Modifier
                    .size(85.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color.White.copy(alpha = 0.05f))
                    .border(1.dp, Color.White.copy(alpha = 0.05f), RoundedCornerShape(14.dp)),
                contentAlignment = Alignment.Center
            ) {
                ImagenCanal(
                    url = canal.icono_transmision,
                    nombreCanal = canal.nombre,
                    tamano = 160,
                    modificador = Modifier.fillMaxSize().padding(10.dp)
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            // Información del Canal y EPG
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = canal.nombre,
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(4.dp))

                if (programaActual != null) {
                    Text(
                        text = programaActual.titulo,
                        style = MaterialTheme.typography.bodyMedium,
                        color = AcentoPremium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        fontWeight = FontWeight.Medium
                    )

                    // remember evita recalcular kotlinx.datetime.Clock.System.now().toEpochMilliseconds() en cada recomposición
                    val progreso = remember(programaActual) { calcularProgresoEpg(programaActual) }
                    if (progreso > 0f) {
                        Spacer(modifier = Modifier.height(8.dp))
                        LinearProgressIndicator(
                            progress = progreso,
                            modifier = Modifier
                                .fillMaxWidth(0.8f)
                                .height(3.dp)
                                .clip(RoundedCornerShape(2.dp)),
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

            // Botón de favorito lateral
            IconButton(
                onClick = alHacerTapFavorito,
                modifier = Modifier.size(40.dp)
            ) {
                Icon(
                    imageVector = if (esFavorito) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                    contentDescription = null,
                    tint = if (esFavorito) AcentoPremium else Color.Gray.copy(alpha = 0.5f),
                    modifier = Modifier.size(22.dp)
                )
            }
        }
    }
}
