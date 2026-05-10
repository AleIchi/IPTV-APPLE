package com.iptv.fiber.tv.pantallas

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.iptv.fiber.datos.modelo.Categoria
import com.iptv.fiber.datos.modelo.Serie
import com.iptv.fiber.interfaz.modelovista.ModeloVistaContenido
import com.iptv.fiber.tv.componentes.TarjetaTV
import coil.compose.AsyncImage
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale

/**
 * Pantalla de Series para TV.
 * Chips de categorías arriba, grilla de series abajo (poster vertical).
 */
@Composable
fun SeriesTV(modeloVista: ModeloVistaContenido) {
    val categorias by modeloVista.categoriasSeries.collectAsState()
    val series by modeloVista.series.collectAsState()
    val categoriaSeleccionada by modeloVista.idCategoriaSeleccionada.collectAsState()

    LaunchedEffect(categoriaSeleccionada) {
        modeloVista.cargarSeries(categoriaSeleccionada)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0A0A14))
    ) {
        Text(
            text = "Series",
            color = Color.White,
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(start = 24.dp, top = 20.dp, bottom = 8.dp)
        )

        LazyRow(
            modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            contentPadding = PaddingValues(horizontal = 24.dp)
        ) {
            item {
                ChipCategoriaTV(
                    nombre = "Todas",
                    esSeleccionado = categoriaSeleccionada == null,
                    alSeleccionar = { modeloVista.seleccionarCategoria(null) }
                )
            }
            items(items = categorias) { categoria: Categoria ->
                ChipCategoriaTV(
                    nombre = categoria.nombre_categoria,
                    esSeleccionado = categoria.id_categoria == categoriaSeleccionada,
                    alSeleccionar = { modeloVista.seleccionarCategoria(categoria.id_categoria) }
                )
            }
        }

        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = 140.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            contentPadding = PaddingValues(horizontal = 24.dp, vertical = 8.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            items(items = series) { serie: Serie ->
                TarjetaSerieTV(
                    serie = serie,
                    alHacerClick = { /* Próximamente al reproductor */ }
                )
            }
        }
    }
}

@Composable
fun TarjetaSerieTV(serie: Serie, alHacerClick: () -> Unit) {
    TarjetaTV(
        modifier = Modifier.size(140.dp, 200.dp),
        alHacerClick = alHacerClick
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            if (!serie.portada.isNullOrBlank()) {
                AsyncImage(
                    model = serie.portada,
                    contentDescription = serie.nombre,
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(14.dp)),
                    contentScale = ContentScale.Crop
                )
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(60.dp)
                        .align(Alignment.BottomCenter)
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.85f))
                            )
                        )
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color(0xFF16162A)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = serie.nombre,
                        color = Color.White.copy(alpha = 0.7f),
                        fontSize = 12.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }

            if (!serie.portada.isNullOrBlank()) {
                Text(
                    text = serie.nombre,
                    color = Color.White,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(10.dp)
                )
            }
        }
    }
}
