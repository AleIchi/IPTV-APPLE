package com.iptv.fiber.interfaz.ajustes

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import com.iptv.fiber.datos.local.GestorPreferencias
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.iptv.fiber.interfaz.modelovista.ModeloVistaAutenticacion
import com.iptv.fiber.interfaz.modelovista.ModeloVistaContenido

// Colores oficiales de Fiber Z (Carbon & Blue)
private val ColorFondoApp = Color(0xFF000000)
private val ColorTarjeta = Color(0xFF121212)
private val ColorAcentoBlue = Color(0xFF4B5EB9) // Azul Corporativo Fiber Z
private val ColorAmarilloBrand = Color(0xFFBDBE22) // Amarillo Corporativo
private val ColorError = Color(0xFFFF4757)
private val ColorDivisor = Color(0xFF1E1E1E)
private val ColorTextoGris = Color(0xFF94A3B8)

@Composable
private fun FilaAjustePremium(
    icono: ImageVector,
    titulo: String,
    descripcion: String? = null,
    alHacerClick: (() -> Unit)? = null,
    mostrarFlecha: Boolean = true,
    componenteFinal: @Composable (() -> Unit)? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = alHacerClick != null) { alHacerClick?.invoke() }
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icono,
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier.size(22.dp)
        )
        
        Spacer(modifier = Modifier.width(16.dp))
        
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = titulo,
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.Normal
            )
            if (descripcion != null) {
                Text(
                    text = descripcion,
                    color = ColorTextoGris,
                    fontSize = 12.sp
                )
            }
        }

        if (componenteFinal != null) {
            componenteFinal()
        } else if (mostrarFlecha) {
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = ColorTextoGris,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PantallaAjustes(
    modeloVista: ModeloVistaContenido,
    modeloVistaAuth: ModeloVistaAutenticacion,
    alCerrarSesion: () -> Unit
) {
    val contexto = LocalContext.current
    val preferencias = remember {
        contexto.getSharedPreferences("iptv_preferencias", Context.MODE_PRIVATE)
    }
    val gestorPreferencias = remember { GestorPreferencias(contexto) }

    var mostrarDialogoHistorial by remember { mutableStateOf(false) }
    var mostrarDialogoCache by remember { mutableStateOf(false) }
    var mensajeSnackbar by remember { mutableStateOf<String?>(null) }
    val estadoHostSnackbar = remember { SnackbarHostState() }

    // Estados para diálogos legales
    var mostrarTerminos by remember { mutableStateOf(false) }
    var mostrarPrivacidad by remember { mutableStateOf(false) }
    var mostrarReclamos by remember { mutableStateOf(false) }
    var mostrarConfirmacionCerrarSesion by remember { mutableStateOf(false) }

    LaunchedEffect(mensajeSnackbar) {
        mensajeSnackbar?.let {
            estadoHostSnackbar.showSnackbar(it)
            mensajeSnackbar = null
        }
    }

    // --- DIÁLOGOS ---
    if (mostrarDialogoHistorial) {
        AlertDialog(
            onDismissRequest = { mostrarDialogoHistorial = false },
            containerColor = ColorTarjeta,
            titleContentColor = Color.White,
            textContentColor = ColorTextoGris,
            title = { Text("Borrar Historial") },
            text = { Text("¿Deseas eliminar tu historial de canales vistos?") },
            confirmButton = {
                TextButton(onClick = {
                    modeloVista.limpiarHistorial()
                    mostrarDialogoHistorial = false
                    mensajeSnackbar = "Historial borrado"
                }) { Text("Borrar", color = ColorError) }
            },
            dismissButton = {
                TextButton(onClick = { mostrarDialogoHistorial = false }) { Text("Cancelar", color = Color.White) }
            }
        )
    }

    if (mostrarConfirmacionCerrarSesion) {
        AlertDialog(
            onDismissRequest = { mostrarConfirmacionCerrarSesion = false },
            containerColor = ColorTarjeta,
            titleContentColor = Color.White,
            textContentColor = ColorTextoGris,
            title = { Text("Cerrar Sesión") },
            text = { Text("¿Estás seguro de que deseas cerrar sesión? Deberás ingresar tus credenciales de nuevo.") },
            confirmButton = {
                TextButton(onClick = {
                    mostrarConfirmacionCerrarSesion = false
                    alCerrarSesion()
                }) { Text("Sí, cerrar sesión", color = ColorError) }
            },
            dismissButton = {
                TextButton(onClick = { mostrarConfirmacionCerrarSesion = false }) { Text("No, cancelar", color = Color.White) }
            }
        )
    }

    if (mostrarDialogoCache) {
        AlertDialog(
            onDismissRequest = { mostrarDialogoCache = false },
            containerColor = ColorTarjeta,
            titleContentColor = Color.White,
            textContentColor = ColorTextoGris,
            title = { Text("Limpiar Caché") },
            text = { Text("Esto recargará la lista de canales desde el servidor.") },
            confirmButton = {
                TextButton(onClick = {
                    modeloVista.limpiarCache()
                    mostrarDialogoCache = false
                    mensajeSnackbar = "Caché limpiada"
                }) { Text("Limpiar", color = Color.White) }
            },
            dismissButton = {
                TextButton(onClick = { mostrarDialogoCache = false }) { Text("Cancelar", color = Color.White) }
            }
        )
    }

    if (mostrarTerminos) {
        AlertDialog(
            onDismissRequest = { mostrarTerminos = false },
            containerColor = ColorTarjeta,
            titleContentColor = Color.White,
            textContentColor = ColorTextoGris,
            title = { Text("Términos y Condiciones") },
            text = { Text("Bienvenido a Fiber Z TV+. El uso de esta aplicación está sujeto a los lineamientos de Fiber Z Telecom (RUC 20604599459). El contenido audiovisual se provee exclusivamente para uso doméstico a través de nuestra red de fibra óptica en Huancayo. Queda prohibida la redistribución de la señal. Al utilizar la aplicación, aceptas nuestros términos de servicio.") },
            confirmButton = { TextButton(onClick = { mostrarTerminos = false }) { Text("Aceptar", color = ColorAcentoBlue) } }
        )
    }

    if (mostrarPrivacidad) {
        AlertDialog(
            onDismissRequest = { mostrarPrivacidad = false },
            containerColor = ColorTarjeta,
            titleContentColor = Color.White,
            textContentColor = ColorTextoGris,
            title = { Text("Política de Privacidad") },
            text = { Text("En Fiber Z Telecom respetamos su privacidad. Los datos recopilados por esta aplicación (historial de reproducción, canales favoritos) se almacenan localmente en su dispositivo y no se venden a terceros. Las credenciales de acceso solo se utilizan para validar su suscripción activa.") },
            confirmButton = { TextButton(onClick = { mostrarPrivacidad = false }) { Text("Entendido", color = ColorAcentoBlue) } }
        )
    }

    if (mostrarReclamos) {
        AlertDialog(
            onDismissRequest = { mostrarReclamos = false },
            containerColor = ColorTarjeta,
            titleContentColor = Color.White,
            textContentColor = ColorTextoGris,
            title = { Text("Libro de Reclamaciones") },
            text = { Text("Conforme a lo establecido en el Código de Protección y Defensa del Consumidor, Fiber Z Telecom cuenta con un Libro de Reclamaciones físico en nuestras oficinas de Huancayo, y virtual a través de nuestro sitio web fiberztelecom.com. Puede asentar cualquier reclamo llamando a nuestra central.") },
            confirmButton = { TextButton(onClick = { mostrarReclamos = false }) { Text("Cerrar", color = Color.White) } }
        )
    }

    Scaffold(
        containerColor = ColorFondoApp,
        snackbarHost = { SnackbarHost(hostState = estadoHostSnackbar) },
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Cuenta", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 20.sp) },
                navigationIcon = {
                    IconButton(onClick = { /* Acción atrás */ }) {
                        Icon(Icons.Default.ArrowBackIosNew, contentDescription = "Atrás", tint = Color.White, modifier = Modifier.size(20.dp))
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = ColorFondoApp)
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            // --- SECCIÓN REPRODUCCIÓN ---
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = ColorTarjeta)
            ) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.PlayCircle, contentDescription = null, tint = ColorAcentoBlue, modifier = Modifier.size(26.dp))
                        Spacer(modifier = Modifier.width(12.dp))
                        Text("Reproducción", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                    }
                    
                    Divider(color = ColorDivisor, thickness = 0.5.dp)

                    FilaAjustePremium(
                        icono = Icons.Default.SmartDisplay,
                        titulo = "Autoplay al Inicio",
                        descripcion = "Reproducir el último canal al abrir la app",
                        mostrarFlecha = false,
                        componenteFinal = {
                            var activo by remember { mutableStateOf(preferencias.getBoolean("autoplay_inicio", false)) }
                            Switch(
                                checked = activo,
                                onCheckedChange = { 
                                    activo = it
                                    preferencias.edit().putBoolean("autoplay_inicio", it).apply()
                                },
                                colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = ColorAcentoBlue, uncheckedTrackColor = ColorDivisor)
                            )
                        }
                    )
                    Divider(color = ColorDivisor, thickness = 0.5.dp, modifier = Modifier.padding(horizontal = 16.dp))

                    FilaAjustePremium(
                        icono = Icons.Default.VolumeUp,
                        titulo = "Audio en Segundo Plano",
                        descripcion = "Continuar el audio al minimizar la app",
                        mostrarFlecha = false,
                        componenteFinal = {
                            var activo by remember { mutableStateOf(preferencias.getBoolean("fondo_activo", false)) }
                            Switch(
                                checked = activo,
                                onCheckedChange = { 
                                    activo = it
                                    preferencias.edit().putBoolean("fondo_activo", it).apply()
                                },
                                colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = ColorAcentoBlue, uncheckedTrackColor = ColorDivisor)
                            )
                        }
                    )
                    Divider(color = ColorDivisor, thickness = 0.5.dp, modifier = Modifier.padding(horizontal = 16.dp))

                    FilaAjustePremium(
                        icono = Icons.Default.PictureInPictureAlt,
                        titulo = "Modo Ventana Flotante (PiP)",
                        descripcion = "Minimizar video al salir de la app",
                        mostrarFlecha = false,
                        componenteFinal = {
                            var activo by remember { mutableStateOf(preferencias.getBoolean("pip_activo", true)) }
                            Switch(
                                checked = activo,
                                onCheckedChange = { 
                                    activo = it
                                    preferencias.edit().putBoolean("pip_activo", it).apply()
                                },
                                colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = ColorAcentoBlue, uncheckedTrackColor = ColorDivisor)
                            )
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // --- SECCIÓN SEGURIDAD Y DATOS ---
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = ColorTarjeta)
            ) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Security, contentDescription = null, tint = ColorAcentoBlue, modifier = Modifier.size(26.dp))
                        Spacer(modifier = Modifier.width(12.dp))
                        Text("Seguridad y Datos", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                    }
                    
                    Divider(color = ColorDivisor, thickness = 0.5.dp)

                    FilaAjustePremium(
                        icono = Icons.Default.Lock,
                        titulo = "Control Parental",
                        descripcion = "Ocultar contenido para adultos",
                        mostrarFlecha = false,
                        componenteFinal = {
                            var activo by remember { mutableStateOf(preferencias.getBoolean("control_parental", false)) }
                            Switch(
                                checked = activo,
                                onCheckedChange = { 
                                    activo = it
                                    preferencias.edit().putBoolean("control_parental", it).apply()
                                },
                                colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = ColorAcentoBlue, uncheckedTrackColor = ColorDivisor)
                            )
                        }
                    )
                    Divider(color = ColorDivisor, thickness = 0.5.dp, modifier = Modifier.padding(horizontal = 16.dp))
                    
                    FilaAjustePremium(Icons.Default.History, "Borrar Historial de Canales", alHacerClick = { mostrarDialogoHistorial = true })
                    Divider(color = ColorDivisor, thickness = 0.5.dp, modifier = Modifier.padding(horizontal = 16.dp))
                    
                    FilaAjustePremium(Icons.Default.CleaningServices, "Limpiar Caché de la App", alHacerClick = { mostrarDialogoCache = true })
                }
            }

            Spacer(modifier = Modifier.height(20.dp))
            
            // --- SECCIÓN CUENTA ---
            var mostrarInfoCuenta by remember { mutableStateOf(false) }
            
            // Datos de cuenta desde DataStore
            val fechaExpiracion by gestorPreferencias.fechaExpiracion.collectAsState(initial = "")
            val maxConexiones by gestorPreferencias.maxConexiones.collectAsState(initial = "")
            val estadoCuenta by gestorPreferencias.estadoCuenta.collectAsState(initial = "")
            
            if (mostrarInfoCuenta) {
                // El servidor XUI suele enviar la fecha en formato Timestamp (Unix Unix time en segundos) o en texto
                val fechaFormateada = remember(fechaExpiracion) {
                    if (fechaExpiracion.isEmpty() || fechaExpiracion == "null") {
                        "Ilimitado / No disponible"
                    } else {
                        try {
                            // Intentar parsear como timestamp (XUI UI envía segundos)
                            val timestampSegundos = fechaExpiracion.toLong()
                            val fecha = java.util.Date(timestampSegundos * 1000)
                            val formato = java.text.SimpleDateFormat("dd/MM/yyyy HH:mm", java.util.Locale.getDefault())
                            formato.format(fecha)
                        } catch (e: Exception) {
                            // Si no es un número, mostrar el texto tal cual
                            fechaExpiracion
                        }
                    }
                }
                
                val estadoFormateado = if (estadoCuenta.equals("Active", ignoreCase = true)) "Activa" else estadoCuenta

                AlertDialog(
                    onDismissRequest = { mostrarInfoCuenta = false },
                    containerColor = ColorTarjeta,
                    titleContentColor = Color.White,
                    textContentColor = ColorTextoGris,
                    title = { Text("Información de Suscripción") },
                    text = { 
                        Column {
                            Text("Estado de Suscripción:", color = ColorTextoGris, fontSize = 14.sp)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(estadoFormateado, color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                            
                            Spacer(modifier = Modifier.height(12.dp))
                            
                            Text("Fecha de Vencimiento:", color = ColorTextoGris, fontSize = 14.sp)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(fechaFormateada, color = ColorAmarilloBrand, fontSize = 18.sp, fontWeight = FontWeight.Bold)

                            if (maxConexiones.isNotEmpty() && maxConexiones != "null") {
                                Spacer(modifier = Modifier.height(12.dp))
                                Text("Dispositivos Permitidos:", color = ColorTextoGris, fontSize = 14.sp)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text("$maxConexiones Dispositivo(s)", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    },
                    confirmButton = { TextButton(onClick = { mostrarInfoCuenta = false }) { Text("Cerrar", color = ColorAcentoBlue) } }
                )
            }

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = ColorTarjeta)
            ) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.AccountCircle, contentDescription = null, tint = ColorAcentoBlue, modifier = Modifier.size(26.dp))
                        Spacer(modifier = Modifier.width(12.dp))
                        Text("Cuenta", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                    }
                    
                    Divider(color = ColorDivisor, thickness = 0.5.dp)

                    FilaAjustePremium(Icons.Default.Info, "Información de Suscripción", alHacerClick = { mostrarInfoCuenta = true })
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // --- SECCIÓN SOPORTE ---
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = ColorTarjeta)
            ) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Info, contentDescription = null, tint = ColorAmarilloBrand, modifier = Modifier.size(26.dp))
                        Spacer(modifier = Modifier.width(12.dp))
                        Text("Soporte", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                        Icon(Icons.Default.KeyboardArrowUp, contentDescription = null, tint = ColorTextoGris)
                    }
                    
                    Divider(color = ColorDivisor, thickness = 0.5.dp)

                    FilaAjustePremium(Icons.Default.Description, "Términos y condiciones", alHacerClick = { mostrarTerminos = true })
                    Divider(color = ColorDivisor, thickness = 0.5.dp, modifier = Modifier.padding(horizontal = 16.dp))
                    
                    FilaAjustePremium(Icons.Default.PrivacyTip, "Política de privacidad", alHacerClick = { mostrarPrivacidad = true })
                    Divider(color = ColorDivisor, thickness = 0.5.dp, modifier = Modifier.padding(horizontal = 16.dp))
                    
                    FilaAjustePremium(Icons.Default.Build, "Libro de Reclamaciones", alHacerClick = { mostrarReclamos = true })
                    Divider(color = ColorDivisor, thickness = 0.5.dp, modifier = Modifier.padding(horizontal = 16.dp))
                    
                    FilaAjustePremium(
                        icono = Icons.Default.Phone,
                        titulo = "Soporte por WhatsApp",
                        alHacerClick = {
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://api.whatsapp.com/send?phone=51982497650&text=Hola%2C%20necesito%20soporte%20con%20la%20app%20IPTV%20Fiber%20Z"))
                            contexto.startActivity(intent)
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // --- BOTÓN CERRAR SESIÓN ---
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { mostrarConfirmacionCerrarSesion = true }
                    .padding(bottom = 40.dp),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = ColorTarjeta)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.ExitToApp, contentDescription = null, tint = ColorError)
                    Spacer(modifier = Modifier.width(12.dp))
                    Text("Cerrar sesión", color = ColorError, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}
