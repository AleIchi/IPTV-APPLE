package com.iptv.fiber.tv.componentes

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import com.iptv.fiber.tv.componentes.tvClickableWithLongClick
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

@Composable
/**
 * Muestra el dialogo dialogo confirmacion tv y comunica las acciones del usuario.
 */
fun DialogoConfirmacionTV(
    titulo: String = "Cerrar sesion?",
    descripcion: String = "Seguro que deseas salir? Deberas ingresar tus credenciales de nuevo.",
    textoConfirmar: String = "Si, cerrar",
    textoCancelar: String = "Cancelar",
    alConfirmar: () -> Unit,
    alCancelar: () -> Unit
) {
    val requeridorFocoCancelar = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        try {
            requeridorFocoCancelar.requestFocus()
        } catch (_: Exception) {}
    }

    Dialog(
        onDismissRequest = alCancelar,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = true,
            dismissOnClickOutside = true
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.82f))
                .pointerInput(Unit) { detectTapGestures { alCancelar() } },
            contentAlignment = Alignment.Center
        ) {
            PanelTV(
                modifier = Modifier
                    .width(430.dp)
                    .pointerInput(Unit) {}
            ) {
                Column(
                    modifier = Modifier.padding(30.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(62.dp)
                            .clip(RoundedCornerShape(18.dp))
                            .background(TemaTV.Peligro.copy(alpha = 0.14f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = null,
                            tint = TemaTV.Peligro,
                            modifier = Modifier.size(34.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(18.dp))
                    Text(
                        text = titulo,
                        color = TemaTV.TextoPrincipal,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = descripcion,
                        color = TemaTV.TextoSecundario,
                        fontSize = 14.sp,
                        lineHeight = 20.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 12.dp)
                    )
                    Spacer(modifier = Modifier.height(28.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        BotonDialogoTV(
                            texto = textoCancelar,
                            colorBase = TemaTV.Superficie.copy(alpha = 0.78f),
                            colorEnfocado = TemaTV.Acento,
                            colorTextoBase = TemaTV.TextoSecundario,
                            colorTextoEnfocado = TemaTV.TextoPrincipal,
                            alHacerClick = alCancelar,
                            modifier = Modifier
                                .weight(1f)
                                .focusRequester(requeridorFocoCancelar)
                        )
                        BotonDialogoTV(
                            texto = textoConfirmar,
                            colorBase = TemaTV.Peligro.copy(alpha = 0.12f),
                            colorEnfocado = TemaTV.Peligro,
                            colorTextoBase = TemaTV.Peligro,
                            colorTextoEnfocado = Color.White,
                            alHacerClick = alConfirmar,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
/**
 * Renderiza el boton boton dialogo tv y ejecuta su accion al seleccionarlo.
 */
fun BotonDialogoTV(
    texto: String,
    colorBase: Color,
    colorEnfocado: Color,
    colorTextoBase: Color,
    colorTextoEnfocado: Color,
    alHacerClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val fuenteInteraccion = remember { MutableInteractionSource() }
    val estaEnfocado by fuenteInteraccion.collectIsFocusedAsState()
    val colorFondo = if (estaEnfocado) colorEnfocado else colorBase
    val colorTexto = if (estaEnfocado) colorTextoEnfocado else colorTextoBase

    val escala by animateFloatAsState(
        targetValue = if (estaEnfocado) 1.05f else 1f,
        animationSpec = tween(durationMillis = 65),
        label = "escala_boton_dialogo"
    )

    Box(
        modifier = modifier
            .graphicsLayer {
                scaleX = escala
                scaleY = escala
            }
            .fillMaxWidth()
            .height(48.dp)
            .clip(RoundedCornerShape(TemaTV.RedondeoControl))
            .background(colorFondo)
            .border(
                width = if (estaEnfocado) 1.5.dp else 0.dp,
                color = if (estaEnfocado) colorEnfocado.copy(alpha = 0.5f) else Color.Transparent,
                shape = RoundedCornerShape(TemaTV.RedondeoControl)
            )
            .tvClickableWithLongClick(
                interactionSource = fuenteInteraccion,
                onClick = alHacerClick
            )
            .padding(horizontal = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = texto,
            color = colorTexto,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}
