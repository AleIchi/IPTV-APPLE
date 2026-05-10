package com.iptv.fiber.tv.componentes

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LiveTv
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.ExitToApp
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.LiveTv
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.iptv.fiber.interfaz.tema.AcentoPremium

/**
 * Representa una opción en el menú lateral.
 */
data class OpcionMenuTV(
    val ruta: String,
    val titulo: String,
    val icono: ImageVector,
    val iconoSeleccionado: ImageVector
)

val opcionesMenuTV = listOf(
    OpcionMenuTV("inicio", "Inicio", Icons.Outlined.Home, Icons.Filled.Home),
    OpcionMenuTV("tv_vivo", "En Vivo", Icons.Outlined.LiveTv, Icons.Filled.LiveTv),
    OpcionMenuTV("favoritos", "Favoritos", Icons.Outlined.FavoriteBorder, Icons.Filled.Favorite),
    OpcionMenuTV("historial", "Historial", Icons.Outlined.History, Icons.Filled.History),
    OpcionMenuTV("ajustes", "Cuenta", Icons.Outlined.Settings, Icons.Filled.Settings),
    OpcionMenuTV("salir", "Salir", Icons.Outlined.ExitToApp, Icons.Filled.ExitToApp)
)

/**
 * Menú lateral estilo TV360: se expande al recibir el foco mostrando íconos y texto.
 * Colapsado solo muestra íconos. Fondo oscuro semi-transparente con gradiente.
 */
@Composable
fun MenuLateralTV(
    rutaActual: String,
    alNavegar: (String) -> Unit
) {
    var estaExpandido by remember { mutableStateOf(false) }

    val anchoMenu by animateDpAsState(
        targetValue = if (estaExpandido) 220.dp else 72.dp,
        animationSpec = tween(250),
        label = "ancho_menu"
    )

    Column(
        modifier = Modifier
            .width(anchoMenu)
            .fillMaxHeight()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF1A1A2E),
                        Color(0xFF0F0F1A)
                    )
                )
            )
            .padding(vertical = 24.dp)
            .onFocusChanged { state: androidx.compose.ui.focus.FocusState ->
                estaExpandido = state.hasFocus
            },
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Logo / Marca
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            contentAlignment = if (estaExpandido) Alignment.CenterStart else Alignment.Center
        ) {
            if (estaExpandido) {
                Text(
                    text = "Fiber Z",
                    color = AcentoPremium,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 1.sp,
                    modifier = Modifier.padding(start = 12.dp)
                )
            } else {
                Text(
                    text = "FZ",
                    color = AcentoPremium,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.ExtraBold
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Separador sutil
        Divider(
            color = Color.White.copy(alpha = 0.08f),
            thickness = 1.dp,
            modifier = Modifier.padding(horizontal = 16.dp)
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Opciones del Menú
        opcionesMenuTV.forEach { opcion ->
            ItemMenuTV(
                opcion = opcion,
                esExpandido = estaExpandido,
                esSeleccionado = rutaActual == opcion.ruta,
                alHacerClick = { alNavegar(opcion.ruta) }
            )
            Spacer(modifier = Modifier.height(4.dp))
        }
    }
}

@Composable
fun ItemMenuTV(
    opcion: OpcionMenuTV,
    esExpandido: Boolean,
    esSeleccionado: Boolean,
    alHacerClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()

    val colorFondo = when {
        isFocused -> AcentoPremium
        esSeleccionado -> AcentoPremium.copy(alpha = 0.15f)
        else -> Color.Transparent
    }

    val colorIcono = when {
        isFocused -> Color.White
        esSeleccionado -> AcentoPremium
        else -> Color.White.copy(alpha = 0.5f)
    }

    val icono = if (esSeleccionado || isFocused) opcion.iconoSeleccionado else opcion.icono

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp)
            .height(52.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(colorFondo)
            .focusable(interactionSource = interactionSource)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = alHacerClick
            )
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = if (esExpandido) Arrangement.Start else Arrangement.Center
    ) {
        Icon(
            imageVector = icono,
            contentDescription = opcion.titulo,
            tint = colorIcono,
            modifier = Modifier.size(26.dp)
        )

        AnimatedVisibility(
            visible = esExpandido,
            enter = fadeIn(animationSpec = tween(200)),
            exit = fadeOut(animationSpec = tween(100))
        ) {
            Text(
                text = opcion.titulo,
                color = if (isFocused) Color.White else if (esSeleccionado) Color.White else Color.White.copy(alpha = 0.7f),
                fontSize = 15.sp,
                fontWeight = if (esSeleccionado || isFocused) FontWeight.Bold else FontWeight.Normal,
                modifier = Modifier.padding(start = 14.dp),
                maxLines = 1
            )
        }
    }
}
