package com.iptv.fiber.interfaz.componentes

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.iptv.fiber.datos.modelo.Serie

@Composable
fun TarjetaSerie(
    serie: Serie,
    alHacerClick: () -> Unit,
    modificador: Modifier = Modifier
) {
    Card(
        modifier = modificador
            .width(150.dp)
            .height(220.dp)
            .clickable { alHacerClick() },
        shape = RoundedCornerShape(8.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            AsyncImage(
                model = serie.portada,
                contentDescription = serie.nombre,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp),
                contentScale = ContentScale.Crop
            )
            Text(
                text = serie.nombre,
                modifier = Modifier.padding(8.dp),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            serie.fechaLanzamiento?.let {
                Text(
                    text = it,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    style = androidx.compose.material3.MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}

