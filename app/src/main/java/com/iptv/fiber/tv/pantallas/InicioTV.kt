package com.iptv.fiber.tv.pantallas

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.iptv.fiber.R
import coil.compose.AsyncImage
import coil.compose.SubcomposeAsyncImage
import com.iptv.fiber.datos.modelo.Canal
import com.iptv.fiber.datos.modelo.Categoria
import com.iptv.fiber.datos.repositorio.RepositorioAutenticacion
import com.iptv.fiber.interfaz.componentes.IconoTvPorDefecto
import com.iptv.fiber.interfaz.modelovista.ModeloVistaContenido
import com.iptv.fiber.interfaz.reproductor.ActividadReproductor
import com.iptv.fiber.interfaz.tema.AcentoPremium
import com.iptv.fiber.tv.componentes.TarjetaTV
import kotlinx.coroutines.launch

/**
 * Pantalla principal del Dashboard de TV.
 * Estilo inspirado en TV360: Hero banner arriba, filas horizontales de canales abajo.
 */
@Composable
fun InicioTV(
    modeloVista: ModeloVistaContenido,
    repositorioAuth: RepositorioAutenticacion? = null
) {
    var datosCargados by remember { mutableStateOf(false) }
    val contexto = LocalContext.current
    val alcance = rememberCoroutineScope()

    val listaBanners = remember {
        listOf(
            R.drawable.banner_camara,
            R.drawable.banner_fiber_gamer,
            R.drawable.banner_fibra_optica,
            R.drawable.banner_deportes,
            R.drawable.banner_peliculas
        )
    }
    var bannerActualIndex by remember { mutableStateOf(0) }

    LaunchedEffect(bannerActualIndex) {
        kotlinx.coroutines.delay(6000L) // Cambiar automáticamente cada 6 segundos
        bannerActualIndex = (bannerActualIndex + 1) % listaBanners.size
    }

    LaunchedEffect(Unit) {
        if (!datosCargados) {
            modeloVista.cargarCategoriasEnVivo()
            modeloVista.cargarCanalesEnVivo()
            modeloVista.iniciarObservacionDatosUsuario()
            modeloVista.cargarHistorial()
            datosCargados = true
        }
    }

    val canalesTop by modeloVista.canalesTop.collectAsState()
    val categoriasPanelPrincipal by modeloVista.categoriasPanelPrincipal.collectAsState()
    val historial by modeloVista.historialReciente.collectAsState()
    val todosCanales by modeloVista.canalesEnVivo.collectAsState()
    val favoritos by modeloVista.favoritos.collectAsState()

    val alternarFavorito: (Canal) -> Unit = { canal ->
        modeloVista.alternarFavorito(canal)
        val yaEsFavorito = favoritos.any { it.idTransmision == canal.id_transmision }
        android.widget.Toast.makeText(
            contexto,
            if (yaEsFavorito) "Quitado de favoritos" else "Agregado a favoritos",
            android.widget.Toast.LENGTH_SHORT
        ).show()
    }

    // Lambda para reproducir un canal (mismos extras que la app móvil)
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
                modeloVista.establecerContextoReproduccion(todosCanales)
                modeloVista.agregarAlHistorial(canal)
            }
            contexto.startActivity(intent)
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0A0A14)),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // ═══════════ HERO BANNER & BANNERS PROMOCIONALES ═══════════
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(360.dp) // Reducido de 420dp para mejor proporción general
            ) {
                // ── Imagen Principal del Hero Banner ──
                androidx.compose.foundation.Image(
                    painter = painterResource(id = listaBanners[bannerActualIndex]), // Banner dinámico
                    contentDescription = "Banner Principal",
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(320.dp), // Reducido de 360dp
                    contentScale = ContentScale.Crop
                )
                
                // Viñeta oscura en la parte inferior para que la fila superpuesta resalte
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(60.dp)
                        .align(Alignment.BottomCenter)
                        .offset(y = (-40).dp) // Ajustado al nuevo límite de la imagen
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(Color.Transparent, Color(0xFF0A0A14).copy(alpha = 0.85f))
                            )
                        )
                )

                // ── Fila de Banners Promocionales Estáticos (solapan el fondo) ──
                LazyRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomStart), // Alineado al fondo del Box de 360dp, creando solapamiento 50/50 de forma natural
                    horizontalArrangement = Arrangement.spacedBy(12.dp), // Espaciado más compacto estilo Bitel
                    contentPadding = PaddingValues(horizontal = 48.dp)
                ) {
                    itemsIndexed(items = listaBanners) { index, idImagen ->
                        BannerPromocionalTV(
                            imagenResId = idImagen,
                            alHacerClick = { /* Acción al hacer click en el banner */ },
                            alEnfocar = {
                                bannerActualIndex = index // Cambiar el fondo al enfocar
                            }
                        )
                    }
                }
            }
        }
        




        // ═══════════ CANALES DESTACADOS ═══════════
        if (canalesTop.isNotEmpty()) {
            item {
                SeccionTituloTV(
                    titulo = "Canales Recomendados",
                    paddingSuperior = 4.dp // Súper pegado a los banners estilo Bitel 360
                )
            }
            item {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    contentPadding = PaddingValues(horizontal = 48.dp)
                ) {
                    items(items = canalesTop) { canal: Canal ->
                        val esFav = favoritos.any { it.idTransmision == canal.id_transmision }
                        TarjetaCanalGrandeTV(
                            canal = canal,
                            esFavorito = esFav,
                            alHacerClick = { reproducirCanal(canal) },
                            alHacerLongClick = { alternarFavorito(canal) }
                        )
                    }
                }
            }
        }

        // ═══════════ FILAS POR CATEGORÍA ═══════════
        items(items = categoriasPanelPrincipal.take(8)) { (categoria, canalesCat) ->
            if (canalesCat.isNotEmpty()) {
                Column {
                    SeccionTituloTV(categoria.nombre_categoria)
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        contentPadding = PaddingValues(horizontal = 48.dp)
                    ) {
                        items(items = canalesCat) { canal: Canal ->
                            val esFav = favoritos.any { it.idTransmision == canal.id_transmision }
                            TarjetaCanalMedianaTV(
                                canal = canal,
                                esFavorito = esFav,
                                alHacerClick = { reproducirCanal(canal) },
                                alHacerLongClick = { alternarFavorito(canal) }
                            )
                        }
                    }
                }
            }
        }

        // Espacio final
        item { Spacer(modifier = Modifier.height(40.dp)) }
    }
}

// ═══════════ COMPONENTES REUTILIZABLES ═══════════

@Composable
fun SeccionTituloTV(titulo: String, paddingSuperior: androidx.compose.ui.unit.Dp = 16.dp) {
    Text(
        text = titulo,
        color = Color.White,
        fontSize = 20.sp,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(start = 48.dp, top = paddingSuperior, bottom = 10.dp)
    )
}

/**
 * Banner promocional ancho para la sección superior (solapa con el hero banner).
 * Utiliza imágenes estáticas locales.
 */
@Composable
fun BannerPromocionalTV(imagenResId: Int, alHacerClick: () -> Unit, alEnfocar: () -> Unit = {}) {
    TarjetaTV(
        modifier = Modifier
            .size(150.dp, 85.dp) // Proporción súper compacta y panorámica idéntica a Bitel 360
            .onFocusChanged { siTieneFoco ->
                if (siTieneFoco.isFocused) {
                    alEnfocar()
                }
            },
        alHacerClick = alHacerClick
    ) {
        androidx.compose.foundation.Image(
            painter = painterResource(id = imagenResId),
            contentDescription = "Banner Promocional",
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )
    }
}

@Composable
fun EstadisticaRapida(valor: String, etiqueta: String) {
    Column {
        Text(
            text = valor,
            color = AcentoPremium,
            fontSize = 28.sp,
            fontWeight = FontWeight.ExtraBold
        )
        Text(
            text = etiqueta,
            color = Color.White.copy(alpha = 0.5f),
            fontSize = 12.sp
        )
    }
}

@Composable
fun TarjetaCanalGrandeTV(
    canal: Canal,
    esFavorito: Boolean = false,
    alHacerClick: () -> Unit,
    alHacerLongClick: (() -> Unit)? = null
) {
    TarjetaTV(
        modifier = Modifier.size(190.dp, 140.dp),
        alHacerClick = alHacerClick,
        alHacerLongClick = alHacerLongClick
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            // ── Fondo oscuro neutro ──
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFF16162A))
            )

            if (!canal.icono_transmision.isNullOrBlank()) {
                // Logo cargado desde internet con margen interno generoso
                SubcomposeAsyncImage(
                    model = canal.icono_transmision,
                    contentDescription = canal.nombre,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 24.dp, vertical = 20.dp),
                    contentScale = ContentScale.Fit,
                    loading = {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            IconoTvPorDefecto(Modifier.scale(0.8f))
                        }
                    },
                    error = {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            IconoTvPorDefecto(Modifier.scale(0.8f))
                        }
                    }
                )
            } else {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    IconoTvPorDefecto(modificador = Modifier.scale(0.85f))
                }
            }

            // Corazón flotante rojo vibrante si es favorito
            if (esFavorito) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp)
                        .background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(50))
                        .padding(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Favorite,
                        contentDescription = "Favorito",
                        tint = Color(0xFFFF2D55),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            // ── Pastilla oscura inferior para el nombre del canal ──
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.85f))
                        )
                    )
                    .padding(horizontal = 12.dp, vertical = 10.dp)
            ) {
                Text(
                    text = canal.nombre,
                    color = Color.White,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}


/**
 * Tarjeta mediana para filas de categorías.
 */
@Composable
fun TarjetaCanalMedianaTV(
    canal: Canal,
    esFavorito: Boolean = false,
    alHacerClick: () -> Unit,
    alHacerLongClick: (() -> Unit)? = null
) {
    TarjetaTV(
        modifier = Modifier.size(150.dp, 100.dp),
        alHacerClick = alHacerClick,
        alHacerLongClick = alHacerLongClick
    ) {
        val colorHash = canal.nombre.hashCode()
        val coloresFondo = listOf(
            Color(0xFFE74C3C), Color(0xFF3498DB), Color(0xFF2ECC71),
            Color(0xFFF39C12), Color(0xFF9B59B6), Color(0xFF1ABC9C)
        )
        val colorBase = coloresFondo[Math.abs(colorHash) % coloresFondo.size]

        Box(modifier = Modifier.fillMaxSize()) {
            // Fondo oscuro neutro como lienzo
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFF16162A))
            )

            if (!canal.icono_transmision.isNullOrBlank()) {
                SubcomposeAsyncImage(
                    model = canal.icono_transmision,
                    contentDescription = canal.nombre,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    contentScale = ContentScale.Fit,
                    loading = {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            IconoTvPorDefecto(Modifier.scale(0.55f))
                        }
                    },
                    error = {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            IconoTvPorDefecto(Modifier.scale(0.55f))
                        }
                    }
                )
            } else {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    IconoTvPorDefecto(modificador = Modifier.scale(0.6f))
                }
            }

            // Corazón flotante rojo vibrante si es favorito
            if (esFavorito) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(6.dp)
                        .background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(50))
                        .padding(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Favorite,
                        contentDescription = "Favorito",
                        tint = Color(0xFFFF2D55),
                        modifier = Modifier.size(14.dp)
                    )
                }
            }

            // Nombre del canal abajo
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.8f))
                        )
                    )
                    .padding(horizontal = 8.dp, vertical = 6.dp)
            ) {
                Text(
                    text = canal.nombre,
                    color = Color.White,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}
