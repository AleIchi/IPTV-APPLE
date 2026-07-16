package com.iptv.fiber.tv.componentes

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.iptv.fiber.interfaz.tema.AcentoPremium

/** Título de sección con barra neon de acento y divisor de desvanecimiento gradual. */
@Composable
fun SeccionTituloTV(titulo: String, paddingSuperior: androidx.compose.ui.unit.Dp = 16.dp) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = TemaTV.MargenPantalla, top = paddingSuperior, end = TemaTV.MargenPantalla, bottom = 4.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Barra vertical neon con degradado brillante
            Box(
                modifier = Modifier
                    .size(4.dp, 19.dp)
                    .background(
                        brush = Brush.verticalGradient(
                            colors = listOf(TemaTV.AcentoClaro, TemaTV.Acento)
                        ),
                        shape = RoundedCornerShape(2.dp)
                    )
            )

            Text(
                text = titulo,
                color = TemaTV.TextoPrincipal,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.4.sp
            )
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        // Divisor con desvanecimiento horizontal suave
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(
                    brush = Brush.horizontalGradient(
                        colors = listOf(
                            TemaTV.Linea.copy(alpha = 0.28f),
                            TemaTV.Linea.copy(alpha = 0.05f),
                            Color.Transparent
                        )
                    )
                )
        )
    }
}

/** Miniatura 16:9 (120×68 dp) con filtro atenuante y borde neon al recibir foco. */
@Composable
fun ImagenPromocionalTV(
    imagenResId: Int,
    estaSeleccionada: Boolean,
    alHacerClick: () -> Unit,
    alEnfocar: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var estaEnfocado by remember { mutableStateOf(false) }
    
    // Filtro oscuro atenuante cuando no tiene foco para dar contraste visual
    val opacidadFiltro = when {
        estaEnfocado -> 0f
        estaSeleccionada -> 0.15f
        else -> 0.45f
    }

    TarjetaTV(
        modifier = modifier
            .size(120.dp, 68.dp) // Reducido a un formato miniatura súper compacto y elegante de 16:9
            .onFocusChanged { siTieneFoco ->
                estaEnfocado = siTieneFoco.isFocused
                if (siTieneFoco.isFocused) {
                    alEnfocar()
                }
            },
        alHacerClick = alHacerClick
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            coil.compose.AsyncImage(
                model = imagenResId,
                contentDescription = "Imagen promocional",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
            
            if (opacidadFiltro > 0f) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = opacidadFiltro))
                )
            }
            
            // Barra neon decorativa en la base cuando tiene foco
            if (estaEnfocado) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(3.dp)
                        .align(Alignment.BottomCenter)
                        .background(
                            Brush.horizontalGradient(
                                colors = listOf(TemaTV.Acento, TemaTV.AcentoClaro)
                            )
                        )
                )
            }
        }
    }
}

/** Estadística resaltada con [valor] en acento grande y [etiqueta] tenue debajo. */
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
