package com.iptv.fiber.tv.componentes

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
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
import androidx.compose.foundation.Image
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.foundation.focusGroup
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

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

/** Menú lateral de navegación TV que se expande al recibir foco y colapsa al perderlo, mostrando icono + texto. */
@kotlin.OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun MenuLateralTV(
    rutaActual: String,
    requeridorFocoContenido: FocusRequester,
    requeridorFocoMenu: FocusRequester,
    alNavegar: (String) -> Unit
) {
    var estaExpandido by remember { mutableStateOf(false) }
    val anchoMenu by animateDpAsState(
        targetValue = if (estaExpandido) 232.dp else 76.dp,
        animationSpec = tween(230),
        label = "ancho_menu"
    )

    // Un FocusRequester por cada ítem del menú
    val requeridoresFoco = remember { List(opcionesMenuTV.size) { FocusRequester() } }

    Column(
        modifier = Modifier
            .width(anchoMenu)
            .fillMaxHeight()
            .focusGroup()
            .background(TemaTV.FondoMenu)
            .padding(vertical = 26.dp)
            .onFocusChanged { focusState ->
                estaExpandido = focusState.hasFocus
            },
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            if (estaExpandido) {
                Image(
                    painter = painterResource(id = com.iptv.fiber.R.drawable.logotipo_fiber_z),
                    contentDescription = "Fiber Z TV+",
                    modifier = Modifier
                        .width(152.dp)
                        .height(50.dp),
                    contentScale = ContentScale.Fit
                )
            } else {
                Image(
                    painter = painterResource(id = com.iptv.fiber.R.drawable.logotipo_reducido),
                    contentDescription = "Logotipo",
                    modifier = Modifier.size(38.dp),
                    contentScale = ContentScale.Fit
                )
            }
        }

        AnimatedVisibility(
            visible = estaExpandido,
            enter = fadeIn(animationSpec = tween(160)),
            exit = fadeOut(animationSpec = tween(100))
        ) {
            Text(
                text = "TV en vivo",
                color = TemaTV.TextoTenue,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 28.dp)
            )
        }

        Spacer(modifier = Modifier.height(12.dp))
        Divider(
            color = TemaTV.Linea,
            thickness = 1.dp,
            modifier = Modifier.padding(horizontal = 16.dp)
        )
        Spacer(modifier = Modifier.height(16.dp))

        opcionesMenuTV.forEachIndexed { indice, opcion ->
            val esActivo = rutaActual == opcion.ruta
            ItemMenuTV(
                opcion = opcion,
                esExpandido = estaExpandido,
                esSeleccionado = esActivo,
                alHacerClick = { alNavegar(opcion.ruta) },
                requeridorFoco = if (esActivo) requeridorFocoMenu else requeridoresFoco[indice],
                requeridorFocoContenido = requeridorFocoContenido
            )
            Spacer(modifier = Modifier.height(6.dp))
        }
    }
}

/** Ítem del menú lateral TV. Activo muestra fondo de acento; enfocado muestra borde y texto resaltado. */
@Composable
fun ItemMenuTV(
    opcion: OpcionMenuTV,
    esExpandido: Boolean,
    esSeleccionado: Boolean,
    alHacerClick: () -> Unit,
    requeridorFoco: FocusRequester,
    requeridorFocoContenido: FocusRequester
) {
    val fuenteInteraccion = remember { MutableInteractionSource() }
    val estaEnfocado by fuenteInteraccion.collectIsFocusedAsState()
    val colorFondo = when {
        esSeleccionado -> TemaTV.Acento
        estaEnfocado -> TemaTV.SuperficieSuave.copy(alpha = 0.9f)
        else -> Color.Transparent
    }
    val colorIcono = when {
        esSeleccionado -> Color.White
        estaEnfocado -> TemaTV.AcentoClaro
        else -> TemaTV.TextoTenue
    }
    val colorTexto = when {
        esSeleccionado -> TemaTV.TextoPrincipal
        estaEnfocado -> TemaTV.TextoPrincipal
        else -> TemaTV.TextoSecundario
    }
    val icono = if (esSeleccionado || estaEnfocado) opcion.iconoSeleccionado else opcion.icono

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        if (esSeleccionado) {
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .height(30.dp)
                    .background(
                        brush = Brush.verticalGradient(
                            colors = listOf(TemaTV.AcentoClaro, TemaTV.Acento)
                        ),
                        shape = RoundedCornerShape(2.dp)
                    )
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = if (esSeleccionado) 8.dp else 0.dp)
                .height(52.dp)
                .clip(RoundedCornerShape(TemaTV.RedondeoControl))
                .background(colorFondo)
                .border(
                    width = if (estaEnfocado && !esSeleccionado) 1.dp else 0.dp,
                    color = if (estaEnfocado && !esSeleccionado) TemaTV.AcentoClaro.copy(alpha = 0.65f) else Color.Transparent,
                    shape = RoundedCornerShape(TemaTV.RedondeoControl)
                )
                .focusRequester(requeridorFoco)
                .focusProperties { right = requeridorFocoContenido }
                .tvClickableWithLongClick(
                    interactionSource = fuenteInteraccion,
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
                modifier = Modifier.size(24.dp)
            )

            AnimatedVisibility(
                visible = esExpandido,
                enter = fadeIn(animationSpec = tween(160)),
                exit = fadeOut(animationSpec = tween(100))
            ) {
                Text(
                    text = opcion.titulo,
                    color = colorTexto,
                    fontSize = 14.sp,
                    fontWeight = if (esSeleccionado || estaEnfocado) FontWeight.Bold else FontWeight.Medium,
                    modifier = Modifier
                        .padding(start = 14.dp)
                        .widthIn(max = 130.dp),
                    maxLines = 1
                )
            }
        }
    }
}
