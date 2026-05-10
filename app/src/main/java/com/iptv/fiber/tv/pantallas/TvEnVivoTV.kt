package com.iptv.fiber.tv.pantallas

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material3.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.iptv.fiber.datos.modelo.Canal
import com.iptv.fiber.datos.modelo.Categoria
import com.iptv.fiber.datos.repositorio.RepositorioAutenticacion
import com.iptv.fiber.interfaz.modelovista.ModeloVistaContenido
import com.iptv.fiber.interfaz.reproductor.ActividadReproductor
import com.iptv.fiber.interfaz.tema.AcentoPremium
import com.iptv.fiber.tv.componentes.TarjetaTV
import kotlinx.coroutines.launch

/**
 * Pantalla de TV en Vivo estilo TV360:
 * - Barra de búsqueda premium
 * - Fila horizontal de categorías (chips/tabs)
 * - Grilla de canales con más respiración visual
 */
@Composable
fun TvEnVivoTV(
    modeloVista: ModeloVistaContenido,
    repositorioAuth: RepositorioAutenticacion? = null
) {
    val categorias by modeloVista.categoriasEnVivo.collectAsState()
    val canales by modeloVista.canalesFiltrados.collectAsState()
    val categoriaSeleccionada by modeloVista.idCategoriaSeleccionada.collectAsState()
    val consultaBusqueda by modeloVista.consultaBusqueda.collectAsState()
    val favoritos by modeloVista.favoritos.collectAsState()
    val contexto = LocalContext.current
    val alcance = rememberCoroutineScope()
    var canalEnVistaPrevia by remember { mutableStateOf<String?>(null) }

    val alternarFavorito: (Canal) -> Unit = { canal ->
        modeloVista.alternarFavorito(canal)
        val yaEsFavorito = favoritos.any { it.idTransmision == canal.id_transmision }
        android.widget.Toast.makeText(
            contexto,
            if (yaEsFavorito) "Quitado de favoritos" else "Agregado a favoritos",
            android.widget.Toast.LENGTH_SHORT
        ).show()
    }

    val reproducirCanal: (Canal) -> Unit = { canal ->
        val servidor = repositorioAuth?.servidorActual?.value
        if (servidor != null) {
            val urlStream = if (!canal.fuenteDirecta.isNullOrEmpty()) canal.fuenteDirecta
            else repositorioAuth.construirUrlStream(servidor.urlServidor, servidor.usuario, servidor.contrasena, "live", canal.id_transmision)
            
            val intent = Intent(contexto, ActividadReproductor::class.java).apply {
                putExtra("url_transmision", urlStream)
                putExtra("id_transmision", canal.id_transmision)
                putExtra("tipo_transmision", "live")
                putExtra("nombre_canal", canal.nombre)
                putExtra("logo_canal", canal.icono_transmision)
                putExtra("servidor_url", servidor.urlServidor)
                putExtra("usuario", servidor.usuario)
                putExtra("contrasena", servidor.contrasena)
            }
            alcance.launch {
                modeloVista.establecerContextoReproduccion(canales)
                modeloVista.agregarAlHistorial(canal)
            }
            contexto.startActivity(intent)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0A0A14))
    ) {
        // ═══════════ BUSCADOR DE CANALES PREMIUM ═══════════
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 48.dp, vertical = 20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = consultaBusqueda,
                onValueChange = { modeloVista.buscar(it) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                placeholder = { 
                    Text(
                        "Buscar canal...", 
                        color = Color.White.copy(alpha = 0.3f),
                        fontSize = 15.sp
                    ) 
                },
                leadingIcon = { 
                    Icon(
                        imageVector = Icons.Default.Search, 
                        contentDescription = "Buscar",
                        tint = AcentoPremium,
                        modifier = Modifier.size(22.dp)
                    ) 
                },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = Color.White.copy(alpha = 0.06f),
                    unfocusedContainerColor = Color.White.copy(alpha = 0.03f),
                    focusedBorderColor = AcentoPremium,
                    unfocusedBorderColor = Color.White.copy(alpha = 0.08f),
                    cursorColor = AcentoPremium,
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White.copy(alpha = 0.8f)
                )
            )
        }

        // ═══════════ BARRA DE CATEGORÍAS HORIZONTAL ═══════════
        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            contentPadding = PaddingValues(horizontal = 48.dp)
        ) {
            // Chip "Todas"
            item {
                ChipCategoriaTV(
                    nombre = "Todas",
                    esSeleccionado = categoriaSeleccionada == null,
                    alSeleccionar = { modeloVista.seleccionarCategoria(null) }
                )
            }
            // Chips de categorías
            items(items = categorias) { categoria: Categoria ->
                ChipCategoriaTV(
                    nombre = categoria.nombre_categoria,
                    esSeleccionado = categoria.id_categoria == categoriaSeleccionada,
                    alSeleccionar = { modeloVista.seleccionarCategoria(categoria.id_categoria) }
                )
            }
        }

        // ═══════════ SEPARADOR SUTIL ═══════════
        Divider(
            color = Color.White.copy(alpha = 0.04f),
            thickness = 1.dp,
            modifier = Modifier.padding(horizontal = 48.dp)
        )

        Spacer(modifier = Modifier.height(16.dp))

        // ═══════════ CONTADOR DE CANALES ═══════════
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 48.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "${canales.size} canales",
                color = Color.White.copy(alpha = 0.35f),
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium
            )
        }

        // ═══════════ PANTALLA DIVIDIDA (Lista a la Izq, Reproductor a la Der) ═══════════
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 48.dp, vertical = 8.dp)
        ) {
            // -- LADO IZQUIERDO: LISTA VERTICAL DE CANALES --
            Column(modifier = Modifier.weight(0.4f).fillMaxHeight()) {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(bottom = 24.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(items = canales) { canal: Canal ->
                        val esFav = favoritos.any { it.idTransmision == canal.id_transmision }
                        val estaEnVistaPrevia = canalEnVistaPrevia == canal.id_transmision.toString()
                        
                        FilaCanalTV(
                            canal = canal,
                            esFavorito = esFav,
                            esSeleccionado = estaEnVistaPrevia,
                            alHacerClick = { 
                                if (estaEnVistaPrevia) {
                                    reproducirCanal(canal) // Doble click -> Pantalla Completa
                                } else {
                                    canalEnVistaPrevia = canal.id_transmision.toString() // Primer click -> Vista Previa
                                }
                            },
                            alHacerLongClick = { alternarFavorito(canal) }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(32.dp))

            // -- LADO DERECHO: MINI-REPRODUCTOR Y DETALLES --
            Column(modifier = Modifier.weight(0.6f).fillMaxHeight()) {
                val canalActual = canales.find { it.id_transmision.toString() == canalEnVistaPrevia }

                // Contenedor del Reproductor
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(16f / 9f)
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color(0xFF16162A))
                ) {
                    if (canalActual != null) {
                        val servidor = repositorioAuth?.servidorActual?.value
                        val urlStream = if (servidor != null) {
                            if (!canalActual.fuenteDirecta.isNullOrEmpty()) canalActual.fuenteDirecta
                            else repositorioAuth.construirUrlStream(servidor.urlServidor, servidor.usuario, servidor.contrasena, "live", canalActual.id_transmision)
                        } else null

                        if (urlStream != null) {
                            com.iptv.fiber.tv.componentes.MiniReproductorTV(
                                urlStream = urlStream,
                                modifier = Modifier.fillMaxSize()
                            )
                        } else {
                            Text("Error al cargar URL", color = Color.White, modifier = Modifier.align(Alignment.Center))
                        }
                    } else {
                        // Estado vacío
                        Column(
                            modifier = Modifier.align(Alignment.Center),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Default.Tv,
                                contentDescription = null,
                                modifier = Modifier.size(64.dp),
                                tint = Color.White.copy(alpha = 0.2f)
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                "Selecciona un canal para previsualizar",
                                color = Color.White.copy(alpha = 0.4f),
                                fontSize = 16.sp
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Detalles del Canal en Vista Previa
                if (canalActual != null) {
                    Text(
                        text = canalActual.nombre,
                        color = Color.White,
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "TV en Vivo", // Aquí se podría conectar el EPG si estuviera disponible
                        color = AcentoPremium,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}

/**
 * Chip/Tab de categoría estilo premium (redondeados, con efecto de foco).
 */
@Composable
fun ChipCategoriaTV(
    nombre: String,
    esSeleccionado: Boolean,
    alSeleccionar: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()

    val colorFondo = when {
        isFocused -> AcentoPremium
        esSeleccionado -> AcentoPremium.copy(alpha = 0.8f)
        else -> Color.White.copy(alpha = 0.06f)
    }

    val colorTexto = when {
        isFocused || esSeleccionado -> Color.White
        else -> Color.White.copy(alpha = 0.55f)
    }

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(colorFondo)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = alSeleccionar
            )
            .padding(horizontal = 22.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = nombre,
            color = colorTexto,
            fontSize = 14.sp,
            fontWeight = if (esSeleccionado || isFocused) FontWeight.Bold else FontWeight.Normal,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

/**
 * Componente de lista optimizado para el rediseño Split-Screen.
 */
@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun FilaCanalTV(
    canal: Canal,
    esFavorito: Boolean,
    esSeleccionado: Boolean,
    alHacerClick: () -> Unit,
    alHacerLongClick: (() -> Unit)? = null
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()

    val colorFondo = when {
        isFocused -> AcentoPremium
        esSeleccionado -> Color.White.copy(alpha = 0.15f)
        else -> Color.White.copy(alpha = 0.04f)
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(60.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(colorFondo)
            .then(
                if (alHacerLongClick != null) {
                    Modifier.combinedClickable(
                        interactionSource = interactionSource,
                        indication = null,
                        onClick = alHacerClick,
                        onLongClick = alHacerLongClick
                    )
                } else {
                    Modifier.clickable(
                        interactionSource = interactionSource,
                        indication = null,
                        onClick = alHacerClick
                    )
                }
            )
            .padding(horizontal = 16.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            // Mini logo o icono
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color.Black.copy(alpha = 0.3f)),
                contentAlignment = Alignment.Center
            ) {
                if (!canal.icono_transmision.isNullOrBlank()) {
                    coil.compose.AsyncImage(
                        model = canal.icono_transmision,
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize().padding(4.dp),
                        contentScale = androidx.compose.ui.layout.ContentScale.Fit
                    )
                } else {
                    Icon(Icons.Default.Tv, contentDescription = null, tint = Color.White.copy(alpha = 0.5f), modifier = Modifier.size(20.dp))
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            // Nombre del canal
            Text(
                text = canal.nombre,
                color = if (isFocused || esSeleccionado) Color.White else Color.White.copy(alpha = 0.8f),
                fontSize = 16.sp,
                fontWeight = if (isFocused || esSeleccionado) FontWeight.Bold else FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )

            // Favorito
            if (esFavorito) {
                Icon(
                    imageVector = Icons.Default.Favorite,
                    contentDescription = "Favorito",
                    tint = if (isFocused) Color.White else Color(0xFFFF2D55),
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}
