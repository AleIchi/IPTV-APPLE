package com.iptv.fiber.tv.componentes

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.SubcomposeAsyncImage
import com.iptv.fiber.datos.modelo.Canal
import com.iptv.fiber.interfaz.componentes.IconoTvPorDefecto

/**
 * Contenedor de tarjeta premium para Android TV.
 * Se escala con animación, muestra sombra de color y borde brillante al recibir foco del D-Pad.
 */
@Composable
fun TarjetaTV(
    modifier: Modifier = Modifier,
    alHacerClick: () -> Unit,
    alHacerLongClick: (() -> Unit)? = null,
    fuenteInteraccion: MutableInteractionSource = remember { MutableInteractionSource() },
    contenido: @Composable BoxScope.() -> Unit
) {
    val estaEnfocado by fuenteInteraccion.collectIsFocusedAsState()

    val escala by animateFloatAsState(
        targetValue = if (estaEnfocado) 1.045f else 1f,
        animationSpec = tween(durationMillis = 65),
        label = "escala_tarjeta"
    )

    val colorBorde = if (estaEnfocado) TemaTV.AcentoClaro else TemaTV.Linea

    Card(
        modifier = modifier
            .graphicsLayer {
                scaleX = escala
                scaleY = escala
                shape = RoundedCornerShape(12.dp)
                clip = true
            }
            .tvClickableWithLongClick(
                interactionSource = fuenteInteraccion,
                onClick = alHacerClick,
                onLongClick = alHacerLongClick
            ),
        shape = RoundedCornerShape(TemaTV.RedondeoTarjeta),
        colors = CardDefaults.cardColors(containerColor = TemaTV.Superficie),
        border = if (estaEnfocado) BorderStroke(2.dp, colorBorde) else null,
        elevation = CardDefaults.cardElevation(
            defaultElevation = 0.dp
        )
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            content = contenido
        )
    }
}

/** Placeholder estilizado con degradado dinámico e iniciales del canal cuando no hay logotipo. */
@Composable
fun PlaceholderCanalTV(nombre: String, modifier: Modifier = Modifier) {
    val iniciales = remember(nombre) {
        nombre.split(" ")
            .filter { it.isNotBlank() }
            .take(2)
            .mapNotNull { it.firstOrNull()?.toString() }
            .joinToString("")
            .uppercase()
    }
    
    // Paletas de degradados dinámicos con efecto de vidrio esmerilado translúcido (Glassmorphism de alta gama)
    val gradiente = remember(nombre) {
        val hash = nombre.hashCode()
        val paletas = listOf(
            // 1. Púrpura translúcido suave a transparente
            listOf(Color(0xFF9C27B0).copy(alpha = 0.18f), Color.Transparent),
            
            // 2. Azul Royal translúcido suave a transparente
            listOf(Color(0xFF3F51B5).copy(alpha = 0.20f), Color.Transparent),
            
            // 3. Turquesa/Teal translúcido suave a transparente
            listOf(Color(0xFF00E5FF).copy(alpha = 0.16f), Color.Transparent),
            
            // 4. Amatista translúcido suave a transparente
            listOf(Color(0xFF7E57C2).copy(alpha = 0.18f), Color.Transparent),
            
            // 5. Verde Esmeralda translúcido suave a transparente
            listOf(Color(0xFF00E676).copy(alpha = 0.14f), Color.Transparent),
            
            // 6. Azul Eléctrico translúcido suave a transparente
            listOf(Color(0xFF2196F3).copy(alpha = 0.20f), Color.Transparent),
            
            // 7. Acento de la marca translúcido suave a transparente
            listOf(Color(0xFF7286FF).copy(alpha = 0.18f), Color.Transparent)
        )
        val index = Math.abs(hash) % paletas.size
        Brush.verticalGradient(paletas[index])
    }
    
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(brush = gradiente),
        contentAlignment = Alignment.Center
    ) {
        // Iniciales gigantes semitransparentes de fondo con mejor proporción
        if (iniciales.isNotEmpty()) {
            Text(
                text = iniciales,
                color = Color.White.copy(alpha = 0.04f),
                fontSize = 46.sp,
                fontWeight = FontWeight.Black,
                maxLines = 1
            )
        }
        
        // Columna de contenido centrada y con padding inferior para no pisar el nombre del canal
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(bottom = 12.dp)
        ) {
            IconoTvPorDefecto(
                modificador = Modifier
                    .size(28.dp)
            )
            if (iniciales.isNotEmpty()) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = iniciales,
                    color = Color.White.copy(alpha = 0.65f),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
            }
        }
    }
}

/** Tarjeta grande de canal para TV con badge "EN VIVO", logotipo y nombre superpuesto al fondo. */
@Composable
fun TarjetaCanalGrandeTV(
    canal: Canal,
    esFavorito: Boolean = false,
    alHacerClick: () -> Unit,
    alHacerLongClick: (() -> Unit)? = null,
    modificadorExtra: Modifier = Modifier
) {
    val fuenteInteraccion = remember { MutableInteractionSource() }
    val estaEnfocado by fuenteInteraccion.collectIsFocusedAsState()

    TarjetaTV(
        modifier = Modifier
            .size(TemaTV.AnchoTarjetaCanal, TemaTV.AltoTarjetaCanal)
            .then(modificadorExtra),
        alHacerClick = alHacerClick,
        alHacerLongClick = alHacerLongClick,
        fuenteInteraccion = fuenteInteraccion
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            // Fondo oscuro neutro como lienzo base
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(TemaTV.FondoPanel)
            )

            // SubcomposeAsyncImage gestiona los estados (Loading/Success/Error) internamente
            // sin causar recomposiciones de la tarjeta completa; el placeholder se muestra
            // sólo en los slots error/loading sin afectar al resto de la composición.
            if (!canal.icono_transmision.isNullOrBlank()) {
                val contexto = androidx.compose.ui.platform.LocalContext.current
                val solicitudImagen = remember(canal.icono_transmision) {
                    coil.request.ImageRequest.Builder(contexto)
                        .data(canal.icono_transmision)
                        .crossfade(false)
                        .size(256)
                        .memoryCachePolicy(coil.request.CachePolicy.ENABLED)
                        .diskCachePolicy(coil.request.CachePolicy.ENABLED)
                        .build()
                }
                SubcomposeAsyncImage(
                    model = solicitudImagen,
                    contentDescription = canal.nombre,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 24.dp, vertical = 20.dp),
                    contentScale = ContentScale.Fit,
                    loading = { PlaceholderCanalTV(nombre = canal.nombre) },
                    error = { PlaceholderCanalTV(nombre = canal.nombre) }
                )
            } else {
                PlaceholderCanalTV(nombre = canal.nombre)
            }

            // Badge premium de "EN VIVO" en la esquina superior izquierda
            if (estaEnfocado) {
                BadgeEnVivoTV(
                    esGrande = true,
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(8.dp)
                )
            }

            // Corazón flotante rojo vibrante si es favorito
            if (esFavorito) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp)
                        .background(Color.Black.copy(alpha = 0.55f), RoundedCornerShape(50))
                        .padding(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Favorite,
                        contentDescription = "Favorito",
                        tint = TemaTV.Favorito,
                        modifier = Modifier.size(15.dp)
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
                            colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.88f))
                        )
                    )
                    .padding(horizontal = 12.dp, vertical = 9.dp)
            ) {
                Text(
                    text = canal.nombre,
                    color = TemaTV.TextoPrincipal,
                    fontSize = TemaTV.TextoTarjeta,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

/** Tarjeta mediana de canal (156×104 dp) para listas compactas en TV. */
@Composable
fun TarjetaCanalMedianaTV(
    canal: Canal,
    esFavorito: Boolean = false,
    alHacerClick: () -> Unit,
    alHacerLongClick: (() -> Unit)? = null,
    modificadorExtra: Modifier = Modifier
) {
    val fuenteInteraccion = remember { MutableInteractionSource() }
    val estaEnfocado by fuenteInteraccion.collectIsFocusedAsState()

    TarjetaTV(
        modifier = Modifier
            .size(156.dp, 104.dp)
            .then(modificadorExtra),
        alHacerClick = alHacerClick,
        alHacerLongClick = alHacerLongClick,
        fuenteInteraccion = fuenteInteraccion
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(TemaTV.FondoPanel)
            )

            if (!canal.icono_transmision.isNullOrBlank()) {
                val contexto = androidx.compose.ui.platform.LocalContext.current
                val solicitudImagen = remember(canal.icono_transmision) {
                    coil.request.ImageRequest.Builder(contexto)
                        .data(canal.icono_transmision)
                        .crossfade(false)
                        .size(180)
                        .memoryCachePolicy(coil.request.CachePolicy.ENABLED)
                        .diskCachePolicy(coil.request.CachePolicy.ENABLED)
                        .build()
                }
                SubcomposeAsyncImage(
                    model = solicitudImagen,
                    contentDescription = canal.nombre,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    contentScale = ContentScale.Fit,
                    loading = { PlaceholderCanalTV(nombre = canal.nombre) },
                    error = { PlaceholderCanalTV(nombre = canal.nombre) }
                )
            } else {
                PlaceholderCanalTV(nombre = canal.nombre)
            }

            // Badge premium de "EN VIVO" en la esquina superior izquierda
            if (estaEnfocado) {
                BadgeEnVivoTV(
                    esGrande = false,
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(6.dp)
                )
            }

            // Corazón flotante rojo vibrante si es favorito
            if (esFavorito) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(6.dp)
                        .background(Color.Black.copy(alpha = 0.55f), RoundedCornerShape(50))
                        .padding(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Favorite,
                        contentDescription = "Favorito",
                        tint = TemaTV.Favorito,
                        modifier = Modifier.size(13.dp)
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
                            colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.85f))
                        )
                    )
                    .padding(horizontal = 8.dp, vertical = 6.dp)
            ) {
                Text(
                    text = canal.nombre,
                    color = TemaTV.TextoPrincipal,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

/** Badge "EN VIVO" con pastilla de cristal y punto rojo pulsante estilo radar. [esGrande] controla el tamaño. */
@Composable
fun BadgeEnVivoTV(
    esGrande: Boolean,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse_live")
    
    // Animación de pulso concéntrico para el efecto radar
    val scaleAnim by infiniteTransition.animateFloat(
        initialValue = 0.9f,
        targetValue = 1.9f,
        animationSpec = infiniteRepeatable(
            animation = tween(1400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "scale_live"
    )
    val alphaAnim by infiniteTransition.animateFloat(
        initialValue = 0.7f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "alpha_live"
    )

    val paddingHorizontal = if (esGrande) 8.dp else 6.dp
    val paddingVertical = if (esGrande) 3.5.dp else 2.5.dp
    val roundedCorner = if (esGrande) 6.dp else 5.dp
    val fontSize = if (esGrande) 8.5.sp else 7.5.sp
    val dotSize = if (esGrande) 6.dp else 5.dp
    val spacing = if (esGrande) 5.dp else 4.dp

    // Se despliega la hermosa pastilla completa con efecto de cristal y texto
    Row(
        modifier = modifier
            .background(
                brush = Brush.horizontalGradient(
                    colors = listOf(
                        Color(0xFF0F1123).copy(alpha = 0.85f),
                        Color(0xFF070812).copy(alpha = 0.70f)
                    )
                ),
                shape = RoundedCornerShape(roundedCorner)
            )
            .border(
                width = 1.dp,
                brush = Brush.linearGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.18f),
                        Color.White.copy(alpha = 0.04f)
                    )
                ),
                shape = RoundedCornerShape(roundedCorner)
            )
            .padding(horizontal = paddingHorizontal, vertical = paddingVertical),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(spacing)
    ) {
        // Punto rojo parpadeante estilo radar
        Box(
            modifier = Modifier.size(dotSize * 2),
            contentAlignment = Alignment.Center
        ) {
            // Halo de expansión (Radar)
            // graphicsLayer: la transformación corre en RenderThread → cero recomposiciones
            // a pesar de ser una animación infinita a 60fps.
            Box(
                modifier = Modifier
                    .size(dotSize)
                    .graphicsLayer {
                        scaleX = scaleAnim
                        scaleY = scaleAnim
                        alpha = alphaAnim
                    }
                    .background(Color(0xFFFF2D55), RoundedCornerShape(50))
            )
            // Punto central sólido brillante
            Box(
                modifier = Modifier
                    .size(dotSize)
                    .background(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                Color(0xFFFF5E7E),
                                Color(0xFFFF2D55)
                            )
                        ),
                        shape = RoundedCornerShape(50)
                    )
            )
        }
        Text(
            text = "EN VIVO",
            color = Color.White,
            fontSize = fontSize,
            fontWeight = FontWeight.ExtraBold,
            letterSpacing = 0.8.sp
        )
    }
}
