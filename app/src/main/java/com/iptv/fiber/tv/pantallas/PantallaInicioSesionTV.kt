package com.iptv.fiber.tv.pantallas

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Error
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.iptv.fiber.interfaz.modelovista.ModeloVistaAutenticacion
import com.iptv.fiber.interfaz.tema.AcentoPremium
import androidx.lifecycle.compose.collectAsStateWithLifecycle

/**
 * Pantalla de inicio de sesión PREMIUM para Android TV.
 * Sincronizada con las funciones de la app móvil: Xtream API / M3U.
 */
@Composable
fun PantallaInicioSesionTV(
    modeloVista: ModeloVistaAutenticacion,
    alIniciarSesionExitosamente: () -> Unit
) {
    var urlServidor by remember { mutableStateOf("") }
    var usuario by remember { mutableStateOf("") }
    var contrasena by remember { mutableStateOf("") }
    var esModoM3U by remember { mutableStateOf(false) }

    val estadoInterfaz by modeloVista.estadoInterfaz.collectAsStateWithLifecycle()
    val scrollState = rememberScrollState()

    LaunchedEffect(estadoInterfaz) {
        if (estadoInterfaz is ModeloVistaAutenticacion.EstadoAutenticacion.Exito) {
            alIniciarSesionExitosamente()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF05050A))
    ) {
        // Fondo con gradiente y luces sutiles para TV
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.radialGradient(
                        colors = listOf(AcentoPremium.copy(alpha = 0.15f), Color.Transparent),
                        radius = 1000f
                    )
                )
        )

        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 48.dp)
                .imePadding(),
            verticalAlignment = Alignment.Top
        ) {
            // Detectar si el teclado está abierto (si el imePadding está actuando)
            // En TV, si el teclado está abierto, solemos ocultar el branding para dar espacio
            val keyboardVisible = WindowInsets.ime.getBottom(androidx.compose.ui.platform.LocalDensity.current) > 0

            // ═══════════ PANEL IZQUIERDO: BRANDING (ESTILO TV360) ═══════════
            if (!keyboardVisible) {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .padding(top = 64.dp, start = 32.dp, end = 32.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Fiber Z",
                        color = Color.White,
                        fontSize = 64.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 2.sp
                    )
                    Text(
                        text = "EXPERIENCIA TV+",
                        color = AcentoPremium,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 8.sp
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    Divider(color = Color.White.copy(alpha = 0.1f), thickness = 1.dp, modifier = Modifier.width(200.dp))
                    Spacer(modifier = Modifier.height(24.dp))
                    Text(
                        text = "La mejor televisión por fibra óptica ahora en tu pantalla grande.",
                        color = Color.White.copy(alpha = 0.4f),
                        fontSize = 16.sp,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        lineHeight = 24.sp
                    )
                }
            }

            // ═══════════ PANEL DERECHO: LOGIN FORM ═══════════
            Box(
                modifier = Modifier
                    .weight(if (keyboardVisible) 2.2f else 1.2f)
                    .fillMaxHeight()
                    .padding(top = 40.dp, end = 64.dp),
                contentAlignment = Alignment.TopCenter
            ) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .wrapContentHeight()
                        .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(24.dp)),
                    color = Color.White.copy(alpha = 0.03f),
                    shape = RoundedCornerShape(24.dp),
                    tonalElevation = 8.dp
                ) {
                    Column(
                        modifier = Modifier
                            .padding(40.dp)
                            .verticalScroll(scrollState),
                        verticalArrangement = Arrangement.spacedBy(20.dp)
                    ) {
                        Text(
                            text = "Acceso a la Cuenta",
                            color = Color.White,
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold
                        )

                        // Selector de Modo (Xtream / M3U) - D-Pad optimizado
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .background(Color.White.copy(alpha = 0.05f), RoundedCornerShape(12.dp))
                                .padding(4.dp)
                        ) {
                            listOf(false to "Xtream API", true to "Lista M3U").forEach { (modo, label) ->
                                Button(
                                    onClick = { esModoM3U = modo },
                                    modifier = Modifier.weight(1f).fillMaxHeight(),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (esModoM3U == modo) AcentoPremium else Color.Transparent,
                                        contentColor = if (esModoM3U == modo) Color.White else Color.White.copy(alpha = 0.4f)
                                    ),
                                    shape = RoundedCornerShape(8.dp),
                                    contentPadding = PaddingValues(0.dp)
                                ) {
                                    Text(label, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }

                        // Errores
                        if (estadoInterfaz is ModeloVistaAutenticacion.EstadoAutenticacion.Error) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(Color(0xFFFF4757).copy(alpha = 0.1f), RoundedCornerShape(8.dp))
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Error, null, tint = Color(0xFFFF4757), modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = (estadoInterfaz as ModeloVistaAutenticacion.EstadoAutenticacion.Error).mensaje,
                                    color = Color(0xFFFF4757),
                                    fontSize = 12.sp
                                )
                            }
                        }

                        // Campos dinámicos
                        CampoTextoPremiumTV(
                            valor = urlServidor,
                            alCambiar = { urlServidor = it },
                            etiqueta = if (esModoM3U) "URL de la Lista" else "URL del Servidor",
                            placeholder = if (esModoM3U) "http://example.com/lista.m3u" else "http://server.com:8080"
                        )

                        if (!esModoM3U) {
                            CampoTextoPremiumTV(
                                valor = usuario,
                                alCambiar = { usuario = it },
                                etiqueta = "Nombre de Usuario"
                            )
                            CampoTextoPremiumTV(
                                valor = contrasena,
                                alCambiar = { contrasena = it },
                                etiqueta = "Contraseña",
                                esPassword = true
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Botón de Acción
                        Button(
                            onClick = {
                                val urlLimpia = urlServidor.trim()
                                val userLimpia = usuario.trim()
                                val passLimpia = contrasena.trim()
                                
                                if (esModoM3U) modeloVista.iniciarSesionConM3U(urlLimpia)
                                else modeloVista.iniciarSesion(urlLimpia, userLimpia, passLimpia)
                            },
                            modifier = Modifier.fillMaxWidth().height(56.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = AcentoPremium),
                            shape = RoundedCornerShape(12.dp),
                            enabled = estadoInterfaz !is ModeloVistaAutenticacion.EstadoAutenticacion.Cargando
                        ) {
                            if (estadoInterfaz is ModeloVistaAutenticacion.EstadoAutenticacion.Cargando) {
                                CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                            } else {
                                Text("CONECTAR AHORA", fontSize = 16.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CampoTextoPremiumTV(
    valor: String,
    alCambiar: (String) -> Unit,
    etiqueta: String,
    placeholder: String = "",
    esPassword: Boolean = false
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(etiqueta, color = Color.White.copy(alpha = 0.5f), fontSize = 14.sp, fontWeight = FontWeight.Medium)
        OutlinedTextField(
            value = valor,
            onValueChange = alCambiar,
            placeholder = { Text(placeholder, color = Color.White.copy(alpha = 0.2f)) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            visualTransformation = if (esPassword) PasswordVisualTransformation() else androidx.compose.ui.text.input.VisualTransformation.None,
            textStyle = TextStyle(color = Color.White, fontSize = 16.sp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = Color.White.copy(alpha = 0.05f),
                unfocusedContainerColor = Color.White.copy(alpha = 0.03f),
                focusedBorderColor = AcentoPremium,
                unfocusedBorderColor = Color.White.copy(alpha = 0.1f),
                cursorColor = AcentoPremium
            ),
            shape = RoundedCornerShape(12.dp)
        )
    }
}
