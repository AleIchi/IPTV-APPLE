package com.iptv.fiber.interfaz.inicio_sesion

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.iptv.fiber.interfaz.tema.*
import com.iptv.fiber.interfaz.modelovista.ModeloVistaAutenticacion

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PantallaInicioSesion(
    modeloVista: ModeloVistaAutenticacion,
    alIniciarSesionExitosamente: () -> Unit
) {
    var urlServidor by remember { mutableStateOf("") }
    var usuario by remember { mutableStateOf("") }
    var contrasena by remember { mutableStateOf("") }
    var contrasenaVisible by remember { mutableStateOf(false) }
    var usarIdDispositivo by remember { mutableStateOf(false) }
    var recordarme by remember { mutableStateOf(true) }
    var esModoM3U by remember { mutableStateOf(false) }
    
    val estadoInterfaz by modeloVista.estadoInterfaz.collectAsStateWithLifecycle()
    
    // Estados de error locales
    var errorUrl by remember { mutableStateOf(false) }
    var errorUsuario by remember { mutableStateOf(false) }
    var errorContrasena by remember { mutableStateOf(false) }

    LaunchedEffect(estadoInterfaz) {
        when (estadoInterfaz) {
            is ModeloVistaAutenticacion.EstadoAutenticacion.Exito -> {
                alIniciarSesionExitosamente()
            }
            is ModeloVistaAutenticacion.EstadoAutenticacion.Error -> {
                // Si hay un error de credenciales, marcamos los campos
                val msg = (estadoInterfaz as ModeloVistaAutenticacion.EstadoAutenticacion.Error).mensaje.lowercase()
                if (msg.contains("usuario") || msg.contains("contraseña") || msg.contains("credenciales")) {
                    errorUsuario = true
                    errorContrasena = true
                } else if (msg.contains("servidor") || msg.contains("url")) {
                    errorUrl = true
                }
            }
            else -> {}
        }
    }
    
    fun validarYEntrar() {
        errorUrl = urlServidor.isBlank()
        // En modo M3U no necesitamos usuario ni contraseña
        errorUsuario = !esModoM3U && usuario.isBlank()
        errorContrasena = !esModoM3U && !usarIdDispositivo && contrasena.isBlank()

        if (!errorUrl && (esModoM3U || (!errorUsuario && !errorContrasena))) {
            if (esModoM3U) {
                // Si es modo M3U, usamos la URL del servidor como el link de la lista
                modeloVista.iniciarSesionConM3U(urlServidor)
            }
            else if (usarIdDispositivo) modeloVista.iniciarSesionConIdDispositivo(urlServidor, usuario)
            else modeloVista.iniciarSesion(urlServidor, usuario, contrasena)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        // Círculos de luz sutiles
        Box(
            modifier = Modifier
                .size(400.dp)
                .offset(x = (-150).dp, y = (-150).dp)
                .background(AcentoPremium.copy(alpha = 0.05f), RoundedCornerShape(200.dp))
                .blur(100.dp)
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Título de Marca Premium
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(bottom = 40.dp)) {
                Text(
                    text = "Fiber Z TV+",
                    style = TextStyle(
                        brush = Brush.linearGradient(listOf(Color.White, AcentoPremium)),
                        fontSize = 42.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                )
                Text(
                    text = "Entretenimiento de Alta Fibra",
                    color = AcentoPremium.copy(alpha = 0.7f),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    letterSpacing = 2.sp
                )
            }
            
            // Tarjeta de Login Glassmorphism
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = SuperficiePremium.copy(alpha = 0.7f),
                shape = RoundedCornerShape(32.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.1f))
            ) {
                Column(
                    modifier = Modifier.padding(28.dp),
                    verticalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    // Mensaje de Error con estilo Alerta
                    if (estadoInterfaz is ModeloVistaAutenticacion.EstadoAutenticacion.Error) {
                        Surface(
                            color = ErrorPremium.copy(alpha = 0.15f),
                            shape = RoundedCornerShape(12.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, ErrorPremium.copy(alpha = 0.3f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Outlined.ErrorOutline, null, tint = ErrorPremium, modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    text = (estadoInterfaz as ModeloVistaAutenticacion.EstadoAutenticacion.Error).mensaje,
                                    color = Color.White,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }

                    // Selector de Modo
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp)
                            .background(Color.Black.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                            .padding(4.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        listOf(false to "Xtream API", true to "Lista M3U").forEach { (modo, label) ->
                            Button(
                                onClick = { 
                                    esModoM3U = modo
                                    // Limpiar errores al cambiar modo
                                    errorUrl = false; errorUsuario = false; errorContrasena = false
                                },
                                modifier = Modifier.weight(1f).fillMaxHeight(),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (esModoM3U == modo) AcentoPremium else Color.Transparent,
                                    contentColor = if (esModoM3U == modo) Color.White else TextoSecundarioPremium
                                ),
                                shape = RoundedCornerShape(10.dp),
                                contentPadding = PaddingValues(0.dp),
                                elevation = null
                            ) {
                                Text(label, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    // Campos de Entrada
                    if (esModoM3U) {
                        CampoTextoPremium(
                            valor = urlServidor,
                            alCambiar = { urlServidor = it; errorUrl = false },
                            etiqueta = "URL de Lista M3U",
                            placeholder = "http://fiberz.com/lista.m3u",
                            esError = errorUrl,
                            mensajeError = "La URL es obligatoria"
                        )
                    } else {
                        CampoTextoPremium(
                            valor = urlServidor,
                            alCambiar = { urlServidor = it; errorUrl = false },
                            etiqueta = "URL del Servidor",
                            placeholder = "http://midominio.com:8080",
                            esError = errorUrl,
                            mensajeError = "Ingresa una URL válida"
                        )
                        
                        if (!usarIdDispositivo) {
                            CampoTextoPremium(
                                valor = usuario,
                                alCambiar = { usuario = it; errorUsuario = false },
                                etiqueta = "Usuario",
                                esError = errorUsuario,
                                mensajeError = "El usuario es obligatorio"
                            )
                            
                            CampoTextoPremium(
                                valor = contrasena,
                                alCambiar = { contrasena = it; errorContrasena = false },
                                etiqueta = "Contraseña",
                                esPassword = true,
                                passwordVisible = contrasenaVisible,
                                alAlternarPassword = { contrasenaVisible = !contrasenaVisible },
                                esError = errorContrasena,
                                mensajeError = "La contraseña es obligatoria"
                            )
                        } else {
                            CampoTextoPremium(
                                valor = usuario,
                                alCambiar = { usuario = it; errorUsuario = false },
                                etiqueta = "ID Dispositivo / MAC",
                                esError = errorUsuario,
                                mensajeError = "El ID es obligatorio"
                            )
                        }
                    }
                    
                    // Opciones Secundarias
                    if (!esModoM3U) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Switch(
                                    checked = recordarme,
                                    onCheckedChange = { recordarme = it },
                                    colors = SwitchDefaults.colors(
                                        checkedThumbColor = Color.White,
                                        checkedTrackColor = AcentoPremium,
                                        uncheckedThumbColor = TextoSecundarioPremium,
                                        uncheckedTrackColor = SuperficiePremiumClaro
                                    ),
                                    modifier = Modifier.graphicsLayer(scaleX = 0.7f, scaleY = 0.7f)
                                )
                                Text("Recordarme", color = TextoSecundarioPremium, fontSize = 13.sp)
                            }
                            
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Checkbox(
                                    checked = usarIdDispositivo,
                                    onCheckedChange = { usarIdDispositivo = it },
                                    colors = CheckboxDefaults.colors(
                                        checkedColor = AcentoPremium,
                                        uncheckedColor = TextoSecundarioPremium
                                    )
                                )
                                Text("Usar ID", color = TextoSecundarioPremium, fontSize = 13.sp)
                            }
                        }
                    }
                    
                    // Botón de Entrada Corporativo
                    Button(
                        onClick = { validarYEntrar() },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                        contentPadding = PaddingValues(),
                        enabled = estadoInterfaz !is ModeloVistaAutenticacion.EstadoAutenticacion.Cargando
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(DegradadoPrimarioPremium)
                                .shadow(12.dp, RoundedCornerShape(16.dp), ambientColor = AcentoPremium, spotColor = AcentoPremium),
                            contentAlignment = Alignment.Center
                        ) {
                            if (estadoInterfaz is ModeloVistaAutenticacion.EstadoAutenticacion.Cargando) {
                                CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                            } else {
                                Text("INICIAR SESIÓN", fontWeight = FontWeight.ExtraBold, letterSpacing = 1.sp, color = Color.White)
                            }
                        }
                    }
                }
            }
            
            // Footer sutil
            Text(
                text = "V 2.0 - Fiber Z Telecom",
                color = TextoSecundarioPremium.copy(alpha = 0.5f),
                modifier = Modifier.padding(top = 32.dp),
                fontSize = 12.sp,
                letterSpacing = 1.sp
            )
        }
    }
}

@Composable
private fun CampoTextoPremium(
    valor: String,
    alCambiar: (String) -> Unit,
    etiqueta: String,
    placeholder: String = "",
    esPassword: Boolean = false,
    passwordVisible: Boolean = false,
    alAlternarPassword: () -> Unit = {},
    esError: Boolean = false,
    mensajeError: String = ""
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(etiqueta, color = if (esError) ErrorPremium else TextoSecundarioPremium, fontSize = 13.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(start = 4.dp))
            if (esError) {
                Text(mensajeError, color = ErrorPremium, fontSize = 11.sp, fontWeight = FontWeight.Normal)
            }
        }
        OutlinedTextField(
            value = valor,
            onValueChange = alCambiar,
            placeholder = { Text(placeholder, color = Color.DarkGray, fontSize = 14.sp) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            isError = esError,
            visualTransformation = if (esPassword && !passwordVisible) PasswordVisualTransformation() else VisualTransformation.None,
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = Color.Black.copy(alpha = 0.2f),
                unfocusedContainerColor = Color.Black.copy(alpha = 0.2f),
                focusedBorderColor = AcentoPremium,
                unfocusedBorderColor = Color.White.copy(alpha = 0.05f),
                errorBorderColor = ErrorPremium,
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
                cursorColor = AcentoPremium
            ),
            shape = RoundedCornerShape(14.dp),
            trailingIcon = if (esPassword) {
                {
                    IconButton(onClick = alAlternarPassword) {
                        Icon(
                            imageVector = if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                            contentDescription = null,
                            tint = if (esError) ErrorPremium else TextoSecundarioPremium,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            } else if (esError) {
                { Icon(Icons.Default.Error, null, tint = ErrorPremium, modifier = Modifier.size(20.dp)) }
            } else null
        )
    }
}
