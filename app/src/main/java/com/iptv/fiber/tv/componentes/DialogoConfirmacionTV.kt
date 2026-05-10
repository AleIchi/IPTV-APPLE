package com.iptv.fiber.tv.componentes

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.iptv.fiber.interfaz.tema.AcentoPremium

@Composable
fun DialogoConfirmacionTV(
    titulo: String = "¿Cerrar Sesión?",
    descripcion: String = "¿Estás seguro de que deseas salir? Deberás ingresar tus credenciales de nuevo.",
    textoConfirmar: String = "Sí, cerrar sesión",
    textoCancelar: String = "No, cancelar",
    alConfirmar: () -> Unit,
    alCancelar: () -> Unit
) {
    val focusRequesterCancelar = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        focusRequesterCancelar.requestFocus()
    }

    // Overlay de pantalla completa
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.85f))
            .clickable(enabled = true, onClick = alCancelar), // Cerrar si hace click fuera
        contentAlignment = Alignment.Center
    ) {
        // Tarjeta del diálogo
        Column(
            modifier = Modifier
                .width(420.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color(0xFF1E1E30), Color(0xFF121222))
                    )
                )
                .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(24.dp))
                .padding(32.dp)
                .clickable(enabled = false) {}, // Evitar clicks hacia atrás
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Icono de Advertencia
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color(0xFFFF4757).copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = null,
                    tint = Color(0xFFFF4757),
                    modifier = Modifier.size(36.dp)
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Título
            Text(
                text = titulo,
                color = Color.White,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Descripción
            Text(
                text = descripcion,
                color = Color.White.copy(alpha = 0.6f),
                fontSize = 14.sp,
                lineHeight = 20.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 12.dp)
            )

            Spacer(modifier = Modifier.height(28.dp))

            // Botones horizontales
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Botón Cancelar (Enfocado por defecto, opción segura)
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .focusRequester(focusRequesterCancelar)
                ) {
                    BotonDialogoTV(
                        texto = textoCancelar,
                        colorBase = Color.White.copy(alpha = 0.06f),
                        colorEnfocado = AcentoPremium,
                        colorTextoBase = Color.White.copy(alpha = 0.8f),
                        colorTextoEnfocado = Color.White,
                        alHacerClick = alCancelar
                    )
                }

                // Botón Confirmar (Opción peligrosa / cerrar sesión)
                Box(
                    modifier = Modifier.weight(1f)
                ) {
                    BotonDialogoTV(
                        texto = textoConfirmar,
                        colorBase = Color.White.copy(alpha = 0.06f),
                        colorEnfocado = Color(0xFFFF4757),
                        colorTextoBase = Color(0xFFFF4757),
                        colorTextoEnfocado = Color.White,
                        alHacerClick = alConfirmar
                    )
                }
            }
        }
    }
}

@Composable
fun BotonDialogoTV(
    texto: String,
    colorBase: Color,
    colorEnfocado: Color,
    colorTextoBase: Color,
    colorTextoEnfocado: Color,
    alHacerClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()

    val colorFondo = if (isFocused) colorEnfocado else colorBase
    val colorTexto = if (isFocused) colorTextoEnfocado else colorTextoBase

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(colorFondo)
            .border(
                width = if (isFocused) 0.dp else 1.dp,
                color = if (isFocused) Color.Transparent else Color.White.copy(alpha = 0.1f),
                shape = RoundedCornerShape(12.dp)
            )
            .focusable(interactionSource = interactionSource)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = alHacerClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = texto,
            color = colorTexto,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1
        )
    }
}
