package com.iptv.fiber.tv.componentes

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.composed
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.focusable
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.ui.graphics.Color
import androidx.compose.runtime.remember
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** Aplica el color de fondo oscuro estándar de la TV al [contenido] anidado. */
@Composable
fun FondoPantallaTV(
    modifier: Modifier = Modifier,
    contenido: @Composable () -> Unit
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(TemaTV.FondoPrincipal)
    ) {
        contenido()
    }
}

/** Cabecera estándar de pantalla TV con [titulo], [subtitulo], [icono] y una [etiqueta] opcional tipo chip. */
@Composable
fun EncabezadoPantallaTV(
    titulo: String,
    subtitulo: String,
    icono: ImageVector = Icons.Default.Tv,
    etiqueta: String? = null,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = TemaTV.MargenPantalla, vertical = 22.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = titulo,
                    color = TemaTV.TextoPrincipal,
                    fontSize = TemaTV.TituloPantalla,
                    fontWeight = FontWeight.ExtraBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = subtitulo,
                    color = TemaTV.TextoSecundario,
                    fontSize = TemaTV.Subtitulo,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            if (etiqueta != null) {
                Surface(
                    shape = RoundedCornerShape(999.dp),
                    color = TemaTV.Superficie.copy(alpha = 0.9f),
                    border = BorderStroke(1.dp, TemaTV.Linea)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 9.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(9.dp)
                    ) {
                        Icon(
                            imageVector = icono,
                            contentDescription = null,
                            tint = TemaTV.AcentoClaro,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = etiqueta,
                            color = TemaTV.TextoPrincipal,
                            fontSize = TemaTV.TextoControl,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(18.dp))
        Divider(color = TemaTV.Linea)
    }
}

/** Contenedor con esquinas redondeadas y borde para secciones de contenido en TV. */
@Composable
fun PanelTV(
    modifier: Modifier = Modifier,
    contenido: @Composable () -> Unit
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(22.dp),
        color = TemaTV.FondoElevado.copy(alpha = 0.94f),
        border = BorderStroke(1.dp, TemaTV.Linea),
        tonalElevation = 8.dp
    ) {
        contenido()
    }
}

/** Pantalla de estado vacío centrada con [icono], [titulo] y [subtitulo] para cuando no hay datos que mostrar. */
@Composable
fun EstadoVacioTV(
    icono: ImageVector,
    titulo: String,
    subtitulo: String,
    modifier: Modifier = Modifier
) {
    val fuenteInteraccion = remember { MutableInteractionSource() }
    val estaEnfocado by fuenteInteraccion.collectIsFocusedAsState()

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            modifier = modifier
                .width(430.dp)
                .focusable(interactionSource = fuenteInteraccion)
                .border(
                    width = if (estaEnfocado) 2.dp else 1.dp,
                    color = if (estaEnfocado) TemaTV.AcentoClaro else TemaTV.Linea,
                    shape = RoundedCornerShape(22.dp)
                ),
            shape = RoundedCornerShape(22.dp),
            color = TemaTV.FondoElevado.copy(alpha = 0.94f),
            tonalElevation = 8.dp
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.padding(horizontal = 32.dp, vertical = 30.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(999.dp),
                    color = if (estaEnfocado) TemaTV.Acento.copy(alpha = 0.3f) else TemaTV.Acento.copy(alpha = 0.16f)
                ) {
                    Icon(
                        imageVector = icono,
                        contentDescription = null,
                        tint = if (estaEnfocado) Color.White else TemaTV.AcentoClaro,
                        modifier = Modifier
                            .padding(14.dp)
                            .size(30.dp)
                    )
                }
                Text(
                    text = titulo,
                    color = TemaTV.TextoPrincipal,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = subtitulo,
                    color = TemaTV.TextoSecundario,
                    fontSize = TemaTV.Subtitulo,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

/**
 * Modificador unificado para TV que soporta:
 * - Click normal (D-Pad Center/Enter, ratón, toque)
 * - Long-press nativo de control remoto (D-Pad Center/Enter con repeat > 0)
 * - Long-press táctil (pointerInput + detectTapGestures)
 *
 * Resuelve el problema donde `.pointerInput { detectTapGestures(onLongPress) }` solo funcionaba
 * con pantallas táctiles pero no con controles remotos físicos de TV.
 */
fun Modifier.tvClickableWithLongClick(
    interactionSource: MutableInteractionSource,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)? = null
): Modifier = composed {
    // Bandera para saber si el long-press ya se disparó (evitar doble click al soltar)
    var longPressDisparado by remember { mutableStateOf(false) }

    this
        // onKeyEvent PRIMERO: interceptar mantener pulsado para long-click.
        // Este handler se ejecuta ANTES de que clickable procese la tecla.
        .then(
            if (onLongClick != null) {
                Modifier.onKeyEvent { event ->
                    val esTeclaOk = event.key == Key.DirectionCenter ||
                        event.key == Key.Enter ||
                        event.key == Key.NumPadEnter

                    when {
                        // Mantener pulsado → long-click
                        event.type == KeyEventType.KeyDown && esTeclaOk &&
                            event.nativeKeyEvent.repeatCount > 0 && !longPressDisparado -> {
                            longPressDisparado = true
                            onLongClick()
                            true // consumir para que clickable no lo vea
                        }
                        // Soltar tras long-press → consumir para NO disparar click
                        event.type == KeyEventType.KeyUp && esTeclaOk && longPressDisparado -> {
                            longPressDisparado = false
                            true // consumir
                        }
                        // Primera pulsación: resetear bandera, NO consumir
                        event.type == KeyEventType.KeyDown && esTeclaOk -> {
                            longPressDisparado = false
                            false // dejar pasar a clickable
                        }
                        else -> false
                    }
                }
            } else Modifier
        )
        // clickable() gestiona: nodo de foco nativo, state visual (isFocused),
        // D-Pad Center/Enter → onClick, y toque táctil → onClick.
        .clickable(
            interactionSource = interactionSource,
            indication = null,
            onClick = onClick
        )
        // pointerInput: long-press táctil (emulador / pantalla táctil híbrida)
        .then(
            if (onLongClick != null) {
                Modifier.pointerInput(onLongClick) {
                    detectTapGestures(onLongPress = { onLongClick() })
                }
            } else Modifier
        )
}

