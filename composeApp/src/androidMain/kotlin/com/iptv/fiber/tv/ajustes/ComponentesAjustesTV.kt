package com.iptv.fiber.tv.ajustes

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.Icon
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.iptv.fiber.tv.componentes.TemaTV
import com.iptv.fiber.tv.componentes.tvClickableWithLongClick

/** Fila de acción TV con icono, título, descripción y flecha derecha; se escala y resalta al recibir foco. */
@Composable
fun ItemAccionTV(
    icono: ImageVector,
    titulo: String,
    descripcion: String,
    alHacerClick: () -> Unit
) {
    val fuenteInteraccion = remember { MutableInteractionSource() }
    val estaEnfocado by fuenteInteraccion.collectIsFocusedAsState()

    val escala by animateFloatAsState(
        targetValue = if (estaEnfocado) 1.02f else 1f,
        animationSpec = tween(durationMillis = 65),
        label = "escala_item_accion"
    )

    // graphicsLayer: escala animada en RenderThread, sin recomponer el árbol de Row+textos+iconos
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer {
                scaleX = escala
                scaleY = escala
            }
            .tvClickableWithLongClick(interactionSource = fuenteInteraccion, onClick = alHacerClick)
            .background(
                if (estaEnfocado) TemaTV.Acento.copy(alpha = 0.16f) else Color.Transparent,
                RoundedCornerShape(12.dp)
            )
            .border(
                width = if (estaEnfocado) 1.5.dp else 0.dp,
                color = if (estaEnfocado) TemaTV.AcentoClaro else Color.Transparent,
                shape = RoundedCornerShape(12.dp)
            )
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icono,
            contentDescription = null,
            tint = if (estaEnfocado) TemaTV.AcentoClaro else TemaTV.TextoSecundario,
            modifier = Modifier.size(22.dp)
        )
        Spacer(modifier = Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = titulo,
                color = if (estaEnfocado) TemaTV.TextoPrincipal else TemaTV.TextoSecundario,
                fontSize = 15.sp,
                fontWeight = if (estaEnfocado) FontWeight.Bold else FontWeight.Medium
            )
            Text(
                text = descripcion,
                color = TemaTV.TextoTenue,
                fontSize = 12.sp
            )
        }
        Icon(
            imageVector = Icons.Default.ChevronRight,
            contentDescription = null,
            tint = TemaTV.TextoTenue,
            modifier = Modifier.size(20.dp)
        )
    }
}

/** Dibuja un QR decorativo estático (bitmap codificado manualmente) como marcador de posición visual. */
@Composable
fun CodigoQRPremium(
    modifier: Modifier = Modifier,
    size: Dp = 120.dp
) {
    val matriz = remember {
        listOf(
            listOf(1,1,1,1,1,1,1, 0, 1,0,1, 0, 1,1,1,1,1,1,1),
            listOf(1,0,0,0,0,0,1, 0, 0,1,0, 0, 1,0,0,0,0,0,1),
            listOf(1,0,1,1,1,0,1, 0, 1,1,1, 0, 1,0,1,1,1,0,1),
            listOf(1,0,1,1,1,0,1, 0, 0,0,1, 0, 1,0,1,1,1,0,1),
            listOf(1,0,1,1,1,0,1, 0, 1,0,1, 0, 1,0,1,1,1,0,1),
            listOf(1,0,0,0,0,0,1, 0, 1,1,0, 0, 1,0,0,0,0,0,1),
            listOf(1,1,1,1,1,1,1, 0, 1,0,1, 0, 1,1,1,1,1,1,1),
            listOf(0,0,0,0,0,0,0, 0, 0,1,1, 0, 0,0,0,0,0,0,0),
            listOf(1,1,0,1,0,1,0, 1, 1,0,0, 1, 0,1,1,0,1,0,1),
            listOf(0,1,1,0,1,0,1, 0, 0,1,1, 0, 1,0,1,0,0,1,1),
            listOf(1,0,1,1,0,1,0, 1, 1,0,1, 1, 0,1,1,0,1,0,1),
            listOf(0,0,0,0,0,0,0, 0, 1,1,0, 0, 0,0,0,0,0,0,0),
            listOf(1,1,1,1,1,1,1, 0, 0,1,1, 1, 0,1,0,1,1,0,1),
            listOf(1,0,0,0,0,0,1, 0, 1,0,1, 0, 1,1,0,1,0,1,0),
            listOf(1,0,1,1,1,0,1, 0, 0,0,1, 1, 0,0,1,1,1,0,1),
            listOf(1,0,1,1,1,0,1, 0, 1,1,0, 0, 1,0,1,1,1,1,1),
            listOf(1,0,1,1,1,0,1, 0, 0,1,1, 1, 0,1,0,1,0,0,0),
            listOf(1,0,0,0,0,0,1, 0, 1,0,0, 1, 1,1,0,0,1,1,1),
            listOf(1,1,1,1,1,1,1, 0, 1,1,0, 0, 0,1,1,0,1,0,1)
        )
    }

    Box(
        modifier = modifier
            .size(size)
            .background(Color.White, RoundedCornerShape(12.dp))
            .padding(8.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val cantidad = matriz.size
            val tamanoCelda = this.size.width / cantidad
            for (fila in 0 until cantidad) {
                for (columna in 0 until cantidad) {
                    if (matriz[fila][columna] == 1) {
                        drawRect(
                            color = Color(0xFF0F0F1A),
                            topLeft = androidx.compose.ui.geometry.Offset((columna * tamanoCelda), (fila * tamanoCelda)),
                            size = androidx.compose.ui.geometry.Size(tamanoCelda + 0.5f, tamanoCelda + 0.5f)
                        )
                    }
                }
            }
        }
    }
}

/** Sección de ajustes TV con título+icono de cabecera y [contenido] en tarjeta redondeada. */
@Composable
fun SeccionAjustesTV(titulo: String, icono: ImageVector, contenido: @Composable () -> Unit) {
    Column {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(bottom = 12.dp)
        ) {
            Icon(icono, contentDescription = null, tint = TemaTV.AcentoClaro, modifier = Modifier.size(22.dp))
            Spacer(modifier = Modifier.width(10.dp))
            Text(titulo, color = TemaTV.TextoPrincipal, fontSize = 16.sp, fontWeight = FontWeight.Bold)
        }
        Column(
            modifier = Modifier
                .clip(RoundedCornerShape(14.dp))
                .background(TemaTV.Superficie.copy(alpha = 0.58f))
        ) {
            contenido()
        }
    }
}

/** Fila de ajuste con interruptor Toggle; el D-Pad Center lo activa/desactiva. Se resalta al recibir foco. */
@Composable
fun ItemSwitchTV(
    icono: ImageVector,
    titulo: String,
    descripcion: String,
    activo: Boolean,
    alCambiar: (Boolean) -> Unit,
    focusRequester: FocusRequester? = null
) {
    val fuenteInteraccion = remember { MutableInteractionSource() }
    val estaEnfocado by fuenteInteraccion.collectIsFocusedAsState()

    val escala by animateFloatAsState(
        targetValue = if (estaEnfocado) 1.02f else 1f,
        animationSpec = tween(durationMillis = 65),
        label = "escala_item_switch"
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (focusRequester != null) Modifier.focusRequester(focusRequester) else Modifier)
            .graphicsLayer {
                scaleX = escala
                scaleY = escala
            }
            .tvClickableWithLongClick(interactionSource = fuenteInteraccion, onClick = { alCambiar(!activo) })
            .background(
                if (estaEnfocado) TemaTV.Acento.copy(alpha = 0.16f) else Color.Transparent,
                RoundedCornerShape(12.dp)
            )
            .border(
                width = if (estaEnfocado) 1.5.dp else 0.dp,
                color = if (estaEnfocado) TemaTV.AcentoClaro else Color.Transparent,
                shape = RoundedCornerShape(12.dp)
            )
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icono,
            contentDescription = null,
            tint = if (estaEnfocado) TemaTV.AcentoClaro else TemaTV.TextoSecundario,
            modifier = Modifier.size(22.dp)
        )
        Spacer(modifier = Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = titulo,
                color = if (estaEnfocado) TemaTV.TextoPrincipal else TemaTV.TextoSecundario,
                fontSize = 15.sp,
                fontWeight = if (estaEnfocado) FontWeight.Bold else FontWeight.Medium
            )
            Text(
                text = descripcion,
                color = TemaTV.TextoTenue,
                fontSize = 12.sp
            )
        }
        Switch(
            checked = activo,
            onCheckedChange = null,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = TemaTV.Acento,
                uncheckedTrackColor = TemaTV.Linea
            )
        )
    }
}

/** Fila de guía de control remoto con [icono], [accion] y [descripcion] de lo que hace esa tecla. */
@Composable
fun GuiaControlTV(
    modifier: Modifier = Modifier,
    icono: ImageVector,
    accion: String,
    descripcion: String
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icono, contentDescription = null, tint = TemaTV.AcentoClaro.copy(alpha = 0.86f), modifier = Modifier.size(22.dp))
        Spacer(modifier = Modifier.width(14.dp))
        Text(
            text = accion,
            color = TemaTV.TextoPrincipal,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.width(60.dp)
        )
        Text(
            text = descripcion,
            color = TemaTV.TextoSecundario,
            fontSize = 13.sp
        )
    }
}

/** Fila de información de suscripción con [icono], [etiqueta] tenue y [valor] resaltado. */
@Composable
fun FilaInfoSuscripcion(
    icono: ImageVector,
    etiqueta: String,
    valor: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Icon(
            imageVector = icono,
            contentDescription = null,
            tint = TemaTV.AcentoClaro.copy(alpha = 0.86f),
            modifier = Modifier.size(20.dp)
        )
        Column {
            Text(
                text = etiqueta,
                color = TemaTV.TextoTenue,
                fontSize = 11.sp
            )
            Text(
                text = valor,
                color = TemaTV.TextoPrincipal,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}
