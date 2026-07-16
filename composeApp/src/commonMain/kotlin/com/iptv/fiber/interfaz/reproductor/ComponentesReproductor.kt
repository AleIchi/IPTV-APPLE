package com.iptv.fiber.interfaz.reproductor

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.HighQuality
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.iptv.fiber.datos.modelo.Canal
import com.iptv.fiber.interfaz.componentes.IconoTvPorDefecto
import com.iptv.fiber.interfaz.tema.*

/** Ítem de la lista lateral de canales recomendados en el reproductor; resalta el canal activo y permite alternar favorito. */
@Composable
fun ItemCanalListaReproductor(
    canal: Canal,
    esActivo: Boolean,
    esFavorito: Boolean,
    alHacerClick: () -> Unit,
    alAlternarFavorito: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { alHacerClick() }
            .padding(horizontal = 16.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Logotipo del canal
        Box(
            modifier = Modifier
                .size(60.dp, 50.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(Color(0xFF222222)),
            contentAlignment = Alignment.Center
        ) {
            if (!canal.icono_transmision.isNullOrBlank()) {
                AsyncImage(
                    model = canal.icono_transmision,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize().padding(6.dp),
                    contentScale = ContentScale.Fit
                )
            } else {
                IconoTvPorDefecto(modificador = Modifier.scale(0.6f))
            }
        }

        Spacer(modifier = Modifier.width(14.dp))

        // Nombre del canal
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = canal.nombre,
                color = if (esActivo) AcentoPremium else Color.White,
                fontWeight = if (esActivo) FontWeight.Bold else FontWeight.Normal,
                fontSize = 15.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        // Botón de Favorito
        IconButton(
            onClick = { alAlternarFavorito() },
            modifier = Modifier.size(36.dp)
        ) {
            Icon(
                imageVector = if (esFavorito) Icons.Default.Favorite else Icons.Outlined.FavoriteBorder,
                contentDescription = "Favorito",
                tint = if (esFavorito) AcentoPremium else Color.Gray,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}
