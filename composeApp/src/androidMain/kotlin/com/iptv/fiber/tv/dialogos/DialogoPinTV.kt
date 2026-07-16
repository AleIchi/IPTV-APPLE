package com.iptv.fiber.tv.dialogos

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.iptv.fiber.interfaz.tema.AcentoPremium
import com.iptv.fiber.tv.componentes.BotonDialogoTV
import com.iptv.fiber.tv.componentes.tvClickableWithLongClick

@Composable
/**
 * Muestra el dialogo dialogo pin tv y comunica las acciones del usuario.
 */
fun DialogoPinTV(
    titulo: String,
    descripcion: String,
    pinCorrecto: String? = null,
    alConfirmar: (String) -> Unit,
    alCancelar: () -> Unit
) {
    var pinIngresado by remember { mutableStateOf("") }
    var mensajeError by remember { mutableStateOf<String?>(null) }
    val focusRequesterCancelar = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        focusRequesterCancelar.requestFocus()
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
                .background(Color.Black.copy(alpha = 0.85f))
                .pointerInput(Unit) { detectTapGestures { alCancelar() } },
            contentAlignment = Alignment.Center
        ) {
            Row(
                modifier = Modifier
                    .width(620.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Color(0xFF1E1E30), Color(0xFF121222))
                        )
                    )
                    .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(24.dp))
                    .padding(24.dp)
                    .pointerInput(Unit) {},
                horizontalArrangement = Arrangement.spacedBy(28.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Columna Izquierda: Icono + Título + Descripción + Cancelar
                Column(
                    modifier = Modifier.weight(1.1f),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = null,
                        tint = AcentoPremium,
                        modifier = Modifier.size(44.dp)
                    )
                    Text(
                        text = titulo,
                        color = Color.White,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = descripcion,
                        color = Color.White.copy(alpha = 0.6f),
                        fontSize = 13.sp,
                        textAlign = TextAlign.Center,
                        lineHeight = 18.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    BotonDialogoTV(
                        texto = "Cancelar",
                        colorBase = Color.White.copy(alpha = 0.06f),
                        colorEnfocado = Color(0xFFFF4757).copy(alpha = 0.3f),
                        colorTextoBase = Color(0xFFFF4757),
                        colorTextoEnfocado = Color.White,
                        alHacerClick = alCancelar,
                        modifier = Modifier
                            .focusRequester(focusRequesterCancelar)
                            .fillMaxWidth()
                    )
                }

                // Columna Derecha: Visor de PIN + Teclado
                Column(
                    modifier = Modifier.weight(1.3f),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Visor de PIN
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        for (i in 0 until 4) {
                            val relleno = i < pinIngresado.length
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .aspectRatio(1f)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(
                                        if (relleno) AcentoPremium.copy(alpha = 0.3f)
                                        else Color.White.copy(alpha = 0.06f)
                                    )
                                    .border(
                                        1.dp,
                                        if (i == pinIngresado.length) AcentoPremium
                                        else Color.White.copy(alpha = 0.1f),
                                        RoundedCornerShape(10.dp)
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                if (relleno) {
                                    Box(
                                        modifier = Modifier
                                            .size(10.dp)
                                            .clip(CircleShape)
                                            .background(Color.White)
                                    )
                                }
                            }
                        }
                    }

                    if (mensajeError != null) {
                        Text(
                            text = mensajeError!!,
                            color = Color(0xFFFF4757),
                            fontSize = 12.sp,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                    }

                    // Teclado numérico
                    val teclas = listOf(
                        listOf("1", "2", "3"),
                        listOf("4", "5", "6"),
                        listOf("7", "8", "9"),
                        listOf("⌫", "0", "✓")
                    )

                    Column(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        teclas.forEach { fila ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                fila.forEach { tecla ->
                                    TeclaPinTV(
                                        texto = tecla,
                                        esAccion = tecla == "⌫" || tecla == "✓",
                                        modifier = Modifier.weight(1f).height(40.dp),
                                        alHacerClick = {
                                            when (tecla) {
                                                "⌫" -> {
                                                    if (pinIngresado.isNotEmpty()) {
                                                        pinIngresado = pinIngresado.dropLast(1)
                                                        mensajeError = null
                                                    }
                                                }
                                                "✓" -> {
                                                    when {
                                                        pinIngresado.length < 4 -> mensajeError = "Debe tener 4 dígitos"
                                                        pinCorrecto != null && pinIngresado != pinCorrecto -> {
                                                            mensajeError = "PIN incorrecto"
                                                            pinIngresado = ""
                                                        }
                                                        else -> {
                                                            mensajeError = null
                                                            alConfirmar(pinIngresado)
                                                        }
                                                    }
                                                }
                                                else -> {
                                                    if (pinIngresado.length < 4) {
                                                        pinIngresado += tecla
                                                        mensajeError = null
                                                    }
                                                }
                                            }
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

/** Tecla del teclado numérico de PIN para TV; verde para confirmar, rojo para borrar, azul para dígitos. */
@Composable
fun TeclaPinTV(
    texto: String,
    esAccion: Boolean,
    modifier: Modifier = Modifier,
    alHacerClick: () -> Unit
) {
    val fuenteInteraccion = remember { MutableInteractionSource() }
    val estaEnfocado by fuenteInteraccion.collectIsFocusedAsState()

    val colorFondo = when {
        estaEnfocado && esAccion && texto == "✓" -> Color(0xFF2ECC71)
        estaEnfocado && esAccion -> Color(0xFFFF4757)
        estaEnfocado -> AcentoPremium
        else -> Color.White.copy(alpha = 0.06f)
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(colorFondo)
            .border(
                1.dp,
                if (estaEnfocado) Color.Transparent else Color.White.copy(alpha = 0.08f),
                RoundedCornerShape(12.dp)
            )
            .tvClickableWithLongClick(interactionSource = fuenteInteraccion, onClick = alHacerClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = texto,
            color = Color.White,
            fontSize = 18.sp,
            fontWeight = if (esAccion) FontWeight.Bold else FontWeight.Normal
        )
    }
}
