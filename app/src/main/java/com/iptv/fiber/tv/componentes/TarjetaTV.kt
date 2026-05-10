package com.iptv.fiber.tv.componentes

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.iptv.fiber.interfaz.tema.AcentoPremium

/**
 * Tarjeta premium para Android TV.
 * Se escala, muestra sombra de color y borde brillante al recibir foco del D-Pad.
 */
@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun TarjetaTV(
    modifier: Modifier = Modifier,
    alHacerClick: () -> Unit,
    alHacerLongClick: (() -> Unit)? = null,
    contenido: @Composable BoxScope.() -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()

    val escala by animateFloatAsState(
        targetValue = if (isFocused) 1.08f else 1f,
        animationSpec = tween(durationMillis = 150),
        label = "escala_tarjeta"
    )

    val colorBorde = if (isFocused) AcentoPremium else Color.Transparent

    Card(
        modifier = modifier
            .scale(escala)
            .then(
                if (isFocused) Modifier.shadow(
                    elevation = 16.dp,
                    shape = RoundedCornerShape(14.dp),
                    ambientColor = AcentoPremium.copy(alpha = 0.4f),
                    spotColor = AcentoPremium.copy(alpha = 0.4f)
                ) else Modifier
            )
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
            ),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E2E)),
        border = BorderStroke(if (isFocused) 2.5.dp else 0.dp, colorBorde),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (isFocused) 12.dp else 2.dp
        )
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            content = contenido
        )
    }
}
