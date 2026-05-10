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
import com.iptv.fiber.datos.modelo.Canal
import com.iptv.fiber.datos.modelo.Categoria
import com.iptv.fiber.datos.modelo.Pelicula
import com.iptv.fiber.interfaz.modelovista.ModeloVistaContenido
import com.iptv.fiber.tv.componentes.TarjetaTV
import coil.compose.AsyncImage
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale

/**
 * Pantalla de Películas VOD para TV.
 * Chips de categorías arriba, grilla de películas abajo.
 */
@Composable
fun PeliculasTV(modeloVista: ModeloVistaContenido) {
    val categorias by modeloVista.categoriasPeliculas.collectAsState()
    val peliculas by modeloVista.peliculas.collectAsState()
    val categoriaSeleccionada by modeloVista.idCategoriaSeleccionada.collectAsState()

    // Cargar películas al seleccionar categoría
    LaunchedEffect(categoriaSeleccionada) {
        modeloVista.cargarPeliculas(categoriaSeleccionada)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0A0A14))
    ) {
        // Título
        Text(
            text = "Películas",
            color = Color.White,
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(start = 24.dp, top = 20.dp, bottom = 8.dp)
        )

        // Chips de Categorías
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

        // Grilla de Películas
        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = 140.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            contentPadding = PaddingValues(horizontal = 24.dp, vertical = 8.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            items(items = peliculas) { pelicula: Pelicula ->
                TarjetaPeliculaTV(
                    pelicula = pelicula,
                    alHacerClick = { /* Próximamente al reproductor */ }
                )
            }
        }
    }
}

@Composable
fun TarjetaPeliculaTV(pelicula: Pelicula, alHacerClick: () -> Unit) {
    TarjetaTV(
        modifier = Modifier.size(140.dp, 200.dp),
        alHacerClick = alHacerClick
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            if (!pelicula.icono_transmision.isNullOrBlank()) {
                AsyncImage(
                    model = pelicula.icono_transmision,
                    contentDescription = pelicula.nombre,
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(14.dp)),
                    contentScale = ContentScale.Crop
                )
                // Gradiente inferior
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
                        text = pelicula.nombre,
                        color = Color.White.copy(alpha = 0.7f),
                        fontSize = 12.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }

            // Nombre en la parte inferior
            if (!pelicula.icono_transmision.isNullOrBlank()) {
                Text(
                    text = pelicula.nombre,
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
