package com.iptv.fiber.tv.dialogos

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.iptv.fiber.interfaz.tema.AcentoPremium
import com.iptv.fiber.tv.ajustes.CodigoQRPremium
import com.iptv.fiber.tv.componentes.BotonDialogoTV

@Composable
/**
 * Muestra el dialogo dialogo legal tv y comunica las acciones del usuario.
 */
fun DialogoLegalTV(
    titulo: String,
    texto: String,
    enlace: String? = null,
    alCerrar: () -> Unit
) {
    val solicitanteFoco = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        solicitanteFoco.requestFocus()
    }

    Dialog(
        onDismissRequest = alCerrar,
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
                .pointerInput(Unit) { detectTapGestures { alCerrar() } },
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier
                    .width(if (enlace != null) 640.dp else 500.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Color(0xFF1E1E30), Color(0xFF121222))
                        )
                    )
                    .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(24.dp))
                    .padding(32.dp)
                    .pointerInput(Unit) {},
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = titulo,
                    color = Color.White,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(20.dp))

                if (enlace != null) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(24.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = texto,
                                color = Color.White.copy(alpha = 0.7f),
                                fontSize = 14.sp,
                                lineHeight = 22.sp
                            )
                        }
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier
                                .width(160.dp)
                                .background(Color.White.copy(alpha = 0.03f), RoundedCornerShape(16.dp))
                                .border(1.dp, Color.White.copy(alpha = 0.05f), RoundedCornerShape(16.dp))
                                .padding(12.dp)
                        ) {
                            CodigoQRPremium(size = 110.dp)
                            Text(
                                text = "Escanea para ir al enlace",
                                color = Color.White.copy(alpha = 0.5f),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Medium,
                                textAlign = TextAlign.Center,
                                lineHeight = 12.sp
                            )
                            Text(
                                text = enlace.replace("https://", ""),
                                color = AcentoPremium,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                } else {
                    Text(
                        text = texto,
                        color = Color.White.copy(alpha = 0.7f),
                        fontSize = 14.sp,
                        lineHeight = 22.sp,
                        textAlign = TextAlign.Start
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))
                BotonDialogoTV(
                    texto = "Cerrar",
                    colorBase = Color.White.copy(alpha = 0.06f),
                    colorEnfocado = AcentoPremium,
                    colorTextoBase = Color.White.copy(alpha = 0.8f),
                    colorTextoEnfocado = Color.White,
                    alHacerClick = alCerrar,
                    modifier = Modifier
                        .focusRequester(solicitanteFoco)
                        .width(200.dp)
                )
            }
        }
    }
}
