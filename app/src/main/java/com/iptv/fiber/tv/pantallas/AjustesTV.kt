package com.iptv.fiber.tv.pantallas

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.iptv.fiber.datos.repositorio.RepositorioAutenticacion
import com.iptv.fiber.interfaz.modelovista.ModeloVistaAutenticacion
import com.iptv.fiber.interfaz.modelovista.ModeloVistaContenido
import com.iptv.fiber.interfaz.tema.AcentoPremium
import com.iptv.fiber.datos.local.GestorPreferencias
import com.iptv.fiber.interfaz.tema.TemaApp
import com.iptv.fiber.tv.componentes.BotonDialogoTV
import com.iptv.fiber.tv.componentes.DialogoConfirmacionTV
import kotlinx.coroutines.launch

@Composable
fun AjustesTV(
    modeloVista: ModeloVistaContenido,
    modeloVistaAuth: ModeloVistaAutenticacion,
    repositorioAuth: RepositorioAutenticacion
) {
    val servidorActual by modeloVistaAuth.servidorActual.collectAsState()
    val contexto = LocalContext.current
    val scope = rememberCoroutineScope()

    val preferencias = remember {
        contexto.getSharedPreferences("iptv_preferencias", Context.MODE_PRIVATE)
    }
    val gestorPreferencias = remember { GestorPreferencias(contexto) }

    val fechaExpiracion by gestorPreferencias.fechaExpiracion.collectAsState(initial = "")
    val maxConexiones by gestorPreferencias.maxConexiones.collectAsState(initial = "")
    val estadoCuenta by gestorPreferencias.estadoCuenta.collectAsState(initial = "")
    val temaActual by gestorPreferencias.tema.collectAsState(initial = "clasico")
    val controlParentalActivo by gestorPreferencias.controlParentalActivo.collectAsState(initial = false)
    val pinParental by gestorPreferencias.pinParental.collectAsState(initial = "")

    var autoplayActivo by remember { mutableStateOf(preferencias.getBoolean("autoplay_inicio", false)) }
    var modoBufferEstable by remember { mutableStateOf(preferencias.getBoolean("modo_buffer_estable", false)) }
    var audioFondoActivo by remember { mutableStateOf(preferencias.getBoolean("fondo_activo", false)) }
    var mostrarConfirmacionSalir by remember { mutableStateOf(false) }

    // Dialog states
    var mostrarDialogoHistorial by remember { mutableStateOf(false) }
    var mostrarDialogoCache by remember { mutableStateOf(false) }
    var mostrarSelectorTema by remember { mutableStateOf(false) }
    var mostrarPinSetup by remember { mutableStateOf(false) }
    var mostrarPinVerify by remember { mutableStateOf(false) }
    var mostrarConfirmacionCP by remember { mutableStateOf(false) }
    var pinPendiente by remember { mutableStateOf("") }
    var accionParentalPendiente by remember { mutableStateOf<String?>(null) }

    // Legal dialogs
    var mostrarTerminos by remember { mutableStateOf(false) }
    var mostrarPrivacidad by remember { mutableStateOf(false) }
    var mostrarReclamos by remember { mutableStateOf(false) }

    // Calcular días restantes de suscripción
    val diasRestantes = remember(fechaExpiracion) {
        try {
            if (fechaExpiracion.isNotEmpty() && fechaExpiracion != "null") {
                val expMs = fechaExpiracion.toLong() * 1000
                val ahora = System.currentTimeMillis()
                val diff = expMs - ahora
                if (diff > 0) (diff / (1000 * 60 * 60 * 24)).toInt() else 0
            } else -1
        } catch (_: Exception) { -1 }
    }

    val fechaFormateada = remember(fechaExpiracion) {
        try {
            if (fechaExpiracion.isNotEmpty() && fechaExpiracion != "null") {
                val fecha = java.util.Date(fechaExpiracion.toLong() * 1000)
                val formato = java.text.SimpleDateFormat("dd 'de' MMMM, yyyy", java.util.Locale("es", "PE"))
                formato.format(fecha)
            } else ""
        } catch (_: Exception) { "" }
    }

    // ─── Diálogo: Borrar Historial ───
    if (mostrarDialogoHistorial) {
        DialogoConfirmacionTV(
            titulo = "Borrar Historial",
            descripcion = "¿Deseas eliminar todo tu historial de canales vistos?",
            textoConfirmar = "Sí, borrar",
            textoCancelar = "Cancelar",
            alConfirmar = {
                modeloVista.limpiarHistorial()
                mostrarDialogoHistorial = false
            },
            alCancelar = { mostrarDialogoHistorial = false }
        )
    }

    // ─── Diálogo: Limpiar Caché ───
    if (mostrarDialogoCache) {
        DialogoConfirmacionTV(
            titulo = "Limpiar Caché",
            descripcion = "Esto recargará la lista de canales desde el servidor.",
            textoConfirmar = "Sí, limpiar",
            textoCancelar = "Cancelar",
            alConfirmar = {
                modeloVista.limpiarCache()
                mostrarDialogoCache = false
            },
            alCancelar = { mostrarDialogoCache = false }
        )
    }

    // ─── Diálogo: PIN Setup (primera vez) ───
    if (mostrarPinSetup) {
        DialogoPinTV(
            titulo = "Establecer PIN",
            descripcion = "Ingresa un PIN de 4 dígitos para el control parental",
            alConfirmar = { pin ->
                scope.launch {
                    gestorPreferencias.establecerPinParental(pin)
                    gestorPreferencias.establecerControlParental(true)
                }
                mostrarPinSetup = false
            },
            alCancelar = {
                mostrarPinSetup = false
            }
        )
    }

    // ─── Diálogo: PIN Verify (para desactivar) ───
    if (mostrarPinVerify) {
        DialogoPinTV(
            titulo = "Ingresar PIN",
            descripcion = "Ingresa tu PIN de seguridad para continuar",
            pinCorrecto = pinParental,
            alConfirmar = { pin ->
                if (pin == pinParental) {
                    scope.launch { gestorPreferencias.establecerControlParental(false) }
                }
                mostrarPinVerify = false
            },
            alCancelar = {
                mostrarPinVerify = false
            }
        )
    }

    // ─── Diálogo: Selección de Tema ───
    if (mostrarSelectorTema) {
        DialogoTemasTV(
            temaActual = temaActual,
            alSeleccionar = { nuevoTema ->
                scope.launch { gestorPreferencias.establecerTema(nuevoTema.name.lowercase()) }
                mostrarSelectorTema = false
            },
            alCerrar = { mostrarSelectorTema = false }
        )
    }

    // ─── Diálogos Legales ───
    if (mostrarTerminos) DialogoLegalTV("Términos y Condiciones",
        "Bienvenido a Fiber Z TV+. El uso de esta aplicación está sujeto a los lineamientos de Fiber Z Telecom (RUC 20604599459). El contenido audiovisual se provee exclusivamente para uso doméstico a través de nuestra red de fibra óptica en Huancayo. Queda prohibida la redistribución de la señal. Al utilizar la aplicación, aceptas nuestros términos de servicio.",
        alCerrar = { mostrarTerminos = false }
    )
    if (mostrarPrivacidad) DialogoLegalTV("Política de Privacidad",
        "En Fiber Z Telecom respetamos su privacidad. Los datos recopilados por esta aplicación (historial de reproducción, canales favoritos) se almacenan localmente en su dispositivo y no se venden a terceros. Las credenciales de acceso solo se utilizan para validar su suscripción activa.",
        alCerrar = { mostrarPrivacidad = false }
    )
    if (mostrarReclamos) DialogoLegalTV("Libro de Reclamaciones",
        "Conforme a lo establecido en el Código de Protección y Defensa del Consumidor, Fiber Z Telecom cuenta con un Libro de Reclamaciones físico en nuestras oficinas de Huancayo, y virtual a través de nuestro sitio web fiberztelecom.com. Puede asentar cualquier reclamo llamando a nuestra central.",
        alCerrar = { mostrarReclamos = false }
    )

    // ─── Confirmación Cerrar Sesión ───
    if (mostrarConfirmacionSalir) {
        DialogoConfirmacionTV(
            alConfirmar = {
                mostrarConfirmacionSalir = false
                modeloVistaAuth.cerrarSesion()
            },
            alCancelar = { mostrarConfirmacionSalir = false }
        )
    }

    // ─── UI Principal ───
    Box(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF0A0A14))
                .padding(horizontal = 40.dp, vertical = 32.dp),
            horizontalArrangement = Arrangement.spacedBy(28.dp)
        ) {
            // ═══════════ PANEL IZQUIERDO: PERFIL Y SUSCRIPCIÓN ═══════════
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(24.dp))
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Color(0xFF1A1A2E), Color(0xFF12121F))
                        )
                    )
                    .padding(28.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.linearGradient(
                                colors = listOf(AcentoPremium, AcentoPremium.copy(alpha = 0.5f))
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(40.dp)
                    )
                }

                Text(
                    text = servidorActual?.usuario ?: "Usuario",
                    color = Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )

                Surface(
                    color = if (estadoCuenta.equals("Active", ignoreCase = true))
                        Color(0xFF2ECC71).copy(alpha = 0.15f)
                    else
                        Color(0xFFFF6B6B).copy(alpha = 0.15f),
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Text(
                        text = if (estadoCuenta.equals("Active", ignoreCase = true)) "● Suscripción Activa" else "● Verificando...",
                        color = if (estadoCuenta.equals("Active", ignoreCase = true)) Color(0xFF2ECC71) else Color(0xFFFF6B6B),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color.White.copy(alpha = 0.04f))
                        .padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = "Tu suscripción",
                        color = Color.White.copy(alpha = 0.5f),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )

                    if (diasRestantes >= 0) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.Bottom,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = "$diasRestantes",
                                color = if (diasRestantes > 15) AcentoPremium else Color(0xFFFF6B6B),
                                fontSize = 48.sp,
                                fontWeight = FontWeight.ExtraBold,
                                lineHeight = 48.sp
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "días\nrestantes",
                                color = Color.White.copy(alpha = 0.6f),
                                fontSize = 14.sp,
                                lineHeight = 18.sp
                            )
                        }

                        if (diasRestantes in 0..365) {
                            val progreso = (diasRestantes.toFloat() / 30f).coerceIn(0f, 1f)
                            LinearProgressIndicator(
                                progress = progreso,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(6.dp)
                                    .clip(RoundedCornerShape(3.dp)),
                                color = if (diasRestantes > 15) AcentoPremium else Color(0xFFFF6B6B),
                                trackColor = Color.White.copy(alpha = 0.08f)
                            )
                        }
                    }

                    Divider(color = Color.White.copy(alpha = 0.06f))

                    if (fechaFormateada.isNotEmpty()) {
                        FilaInfoSuscripcion(
                            icono = Icons.Default.Event,
                            etiqueta = "Vence el",
                            valor = fechaFormateada
                        )
                    }

                    if (maxConexiones.isNotEmpty() && maxConexiones != "null") {
                        FilaInfoSuscripcion(
                            icono = Icons.Default.Devices,
                            etiqueta = "Dispositivos permitidos",
                            valor = "$maxConexiones simultáneos"
                        )
                    }
                }

                Spacer(modifier = Modifier.weight(1f))

                Button(
                    onClick = { mostrarConfirmacionSalir = true },
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFFF4757).copy(alpha = 0.12f)
                    ),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Icon(Icons.Default.ExitToApp, contentDescription = null, tint = Color(0xFFFF4757), modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(10.dp))
                    Text("Cerrar Sesión", color = Color(0xFFFF4757), fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }
            }

            // ═══════════ PANEL DERECHO: CONFIGURACIÓN ═══════════
            Column(
                modifier = Modifier
                    .weight(1.2f)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(24.dp))
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Color(0xFF1A1A2E), Color(0xFF12121F))
                        )
                    )
                    .padding(28.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                Text(
                    text = "Configuración",
                    color = Color.White,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold
                )

                // ── Sección: Reproducción ──
                SeccionAjustesTV("Reproducción", Icons.Default.PlayCircle) {
                    ItemSwitchTV(
                        icono = Icons.Default.SmartDisplay,
                        titulo = "Autoplay al Inicio",
                        descripcion = "Reproducir el último canal al abrir",
                        activo = autoplayActivo,
                        alCambiar = {
                            autoplayActivo = it
                            preferencias.edit().putBoolean("autoplay_inicio", it).apply()
                        }
                    )
                    Divider(color = Color.White.copy(alpha = 0.04f))
                    ItemSwitchTV(
                        icono = Icons.Default.VolumeUp,
                        titulo = "Audio en Segundo Plano",
                        descripcion = "Continuar el audio al navegar el menú",
                        activo = audioFondoActivo,
                        alCambiar = {
                            audioFondoActivo = it
                            preferencias.edit().putBoolean("fondo_activo", it).apply()
                        }
                    )
                    Divider(color = Color.White.copy(alpha = 0.04f))
                    ItemSwitchTV(
                        icono = Icons.Default.Speed,
                        titulo = "Modo Buffer Estable",
                        descripcion = "Mayor pre-carga para conexiones lentas",
                        activo = modoBufferEstable,
                        alCambiar = {
                            modoBufferEstable = it
                            preferencias.edit().putBoolean("modo_buffer_estable", it).apply()
                        }
                    )
                }

                // ── Sección: Seguridad ──
                SeccionAjustesTV("Seguridad", Icons.Default.Security) {
                    ItemSwitchTV(
                        icono = Icons.Default.Lock,
                        titulo = "Control Parental",
                        descripcion = if (controlParentalActivo) "Activado" else "Restringir contenido para adultos",
                        activo = controlParentalActivo,
                        alCambiar = { activar ->
                            if (activar) {
                                if (pinParental.isEmpty()) {
                                    mostrarPinSetup = true
                                } else {
                                    scope.launch { gestorPreferencias.establecerControlParental(true) }
                                }
                            } else {
                                if (pinParental.isNotEmpty()) {
                                    mostrarPinVerify = true
                                } else {
                                    scope.launch { gestorPreferencias.establecerControlParental(false) }
                                }
                            }
                        }
                    )
                    if (pinParental.isNotEmpty()) {
                        Divider(color = Color.White.copy(alpha = 0.04f))
                        ItemAccionTV(
                            icono = Icons.Default.LockReset,
                            titulo = "Cambiar PIN",
                            descripcion = "Actualizar el PIN de control parental",
                            alHacerClick = { mostrarPinSetup = true }
                        )
                    }
                    Divider(color = Color.White.copy(alpha = 0.04f))
                    ItemAccionTV(
                        icono = Icons.Default.History,
                        titulo = "Borrar Historial de Canales",
                        descripcion = "Eliminar todo el historial de reproducción",
                        alHacerClick = { mostrarDialogoHistorial = true }
                    )
                    Divider(color = Color.White.copy(alpha = 0.04f))
                    ItemAccionTV(
                        icono = Icons.Default.CleaningServices,
                        titulo = "Limpiar Caché de la App",
                        descripcion = "Recargar lista de canales desde el servidor",
                        alHacerClick = { mostrarDialogoCache = true }
                    )
                }

                // ── Sección: Apariencia ──
                SeccionAjustesTV("Apariencia", Icons.Default.Palette) {
                    ItemAccionTV(
                        icono = Icons.Default.ColorLens,
                        titulo = "Tema de la App",
                        descripcion = temaActual.replaceFirstChar { it.uppercase() },
                        alHacerClick = { mostrarSelectorTema = true }
                    )
                }

                // ── Sección: Control Remoto ──
                SeccionAjustesTV("Control Remoto", Icons.Default.Gamepad) {
                    GuiaControlTV(
                        icono = Icons.Default.KeyboardArrowUp,
                        accion = "▲ / ▼",
                        descripcion = "Cambiar de canal rápido"
                    )
                    Divider(color = Color.White.copy(alpha = 0.04f))
                    GuiaControlTV(
                        icono = Icons.Default.KeyboardArrowLeft,
                        accion = "◀",
                        descripcion = "Abrir lista de canales"
                    )
                    Divider(color = Color.White.copy(alpha = 0.04f))
                    GuiaControlTV(
                        icono = Icons.Default.RadioButtonChecked,
                        accion = "OK",
                        descripcion = "Cambiar ajuste de pantalla"
                    )
                    Divider(color = Color.White.copy(alpha = 0.04f))
                    GuiaControlTV(
                        icono = Icons.Default.ArrowBack,
                        accion = "BACK",
                        descripcion = "Salir del reproductor"
                    )
                }

                // ── Sección: Soporte ──
                SeccionAjustesTV("Soporte", Icons.Default.HeadsetMic) {
                    ItemAccionTV(
                        icono = Icons.Default.Description,
                        titulo = "Términos y Condiciones",
                        descripcion = "Lee los términos de servicio",
                        alHacerClick = { mostrarTerminos = true }
                    )
                    Divider(color = Color.White.copy(alpha = 0.04f))
                    ItemAccionTV(
                        icono = Icons.Default.PrivacyTip,
                        titulo = "Política de Privacidad",
                        descripcion = "Conoce cómo manejamos tus datos",
                        alHacerClick = { mostrarPrivacidad = true }
                    )
                    Divider(color = Color.White.copy(alpha = 0.04f))
                    ItemAccionTV(
                        icono = Icons.Default.Build,
                        titulo = "Libro de Reclamaciones",
                        descripcion = "Libro de Reclamaciones virtual",
                        alHacerClick = { mostrarReclamos = true }
                    )
                    Divider(color = Color.White.copy(alpha = 0.04f))
                    ItemAccionTV(
                        icono = Icons.Default.Phone,
                        titulo = "Soporte por WhatsApp",
                        descripcion = "Contáctanos para recibir ayuda",
                        alHacerClick = {
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://api.whatsapp.com/send?phone=51982497650&text=Hola%2C%20necesito%20soporte%20con%20la%20app%20IPTV%20Fiber%20Z"))
                            contexto.startActivity(intent)
                        }
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "Fiber Z TV+",
                            color = AcentoPremium.copy(alpha = 0.6f),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Versión 1.0.0",
                            color = Color.White.copy(alpha = 0.2f),
                            fontSize = 11.sp
                        )
                    }
                }
            }
        }
    }
}

// ══════════════════════════════════════════════
// COMPONENTES ADICIONALES
// ══════════════════════════════════════════════

@Composable
fun ItemAccionTV(
    icono: ImageVector,
    titulo: String,
    descripcion: String,
    alHacerClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(interactionSource = interactionSource, indication = null, onClick = alHacerClick)
            .focusable(interactionSource = interactionSource)
            .background(
                if (isFocused) AcentoPremium.copy(alpha = 0.12f) else Color.Transparent,
                RoundedCornerShape(12.dp)
            )
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icono,
            contentDescription = null,
            tint = if (isFocused) AcentoPremium else Color.White.copy(alpha = 0.6f),
            modifier = Modifier.size(22.dp)
        )
        Spacer(modifier = Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = titulo,
                color = if (isFocused) Color.White else Color.White.copy(alpha = 0.9f),
                fontSize = 15.sp,
                fontWeight = if (isFocused) FontWeight.Bold else FontWeight.Medium
            )
            Text(
                text = descripcion,
                color = Color.White.copy(alpha = 0.4f),
                fontSize = 12.sp
            )
        }
        Icon(
            imageVector = Icons.Default.ChevronRight,
            contentDescription = null,
            tint = Color.White.copy(alpha = 0.3f),
            modifier = Modifier.size(20.dp)
        )
    }
}

@Composable
fun DialogoLegalTV(
    titulo: String,
    texto: String,
    alCerrar: () -> Unit
) {
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.85f))
            .clickable(enabled = true, onClick = alCerrar),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .width(500.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color(0xFF1E1E30), Color(0xFF121222))
                    )
                )
                .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(24.dp))
                .padding(32.dp)
                .clickable(enabled = false) {},
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = titulo,
                color = Color.White,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = texto,
                color = Color.White.copy(alpha = 0.7f),
                fontSize = 14.sp,
                lineHeight = 22.sp,
                textAlign = TextAlign.Start
            )
            Spacer(modifier = Modifier.height(24.dp))
            Box(
                modifier = Modifier
                    .focusRequester(focusRequester)
                    .width(200.dp)
            ) {
                BotonDialogoTV(
                    texto = "Cerrar",
                    colorBase = Color.White.copy(alpha = 0.06f),
                    colorEnfocado = AcentoPremium,
                    colorTextoBase = Color.White.copy(alpha = 0.8f),
                    colorTextoEnfocado = Color.White,
                    alHacerClick = alCerrar
                )
            }
        }
    }
}

@Composable
fun DialogoPinTV(
    titulo: String,
    descripcion: String,
    pinCorrecto: String? = null,
    alConfirmar: (String) -> Unit,
    alCancelar: () -> Unit
) {
    var pinIngresado by remember { mutableStateOf("") }
    var errorMsg by remember { mutableStateOf<String?>(null) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.85f))
            .clickable(enabled = true, onClick = alCancelar),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .width(380.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color(0xFF1E1E30), Color(0xFF121222))
                    )
                )
                .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(24.dp))
                .padding(32.dp)
                .clickable(enabled = false) {},
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Default.Lock,
                contentDescription = null,
                tint = AcentoPremium,
                modifier = Modifier.size(48.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = titulo,
                color = Color.White,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = descripcion,
                color = Color.White.copy(alpha = 0.6f),
                fontSize = 14.sp,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(24.dp))

            // Display de PIN
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                for (i in 0 until 4) {
                    val relleno = i < pinIngresado.length
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                if (relleno) AcentoPremium.copy(alpha = 0.3f)
                                else Color.White.copy(alpha = 0.06f)
                            )
                            .border(
                                1.dp,
                                if (i == pinIngresado.length) AcentoPremium
                                else Color.White.copy(alpha = 0.1f),
                                RoundedCornerShape(12.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        if (relleno) {
                            Box(
                                modifier = Modifier
                                    .size(14.dp)
                                    .clip(CircleShape)
                                    .background(Color.White)
                            )
                        }
                    }
                }
            }

            if (errorMsg != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = errorMsg!!,
                    color = Color(0xFFFF4757),
                    fontSize = 13.sp
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Teclado numérico
            val teclas = listOf(
                listOf("1", "2", "3"),
                listOf("4", "5", "6"),
                listOf("7", "8", "9"),
                listOf("⌫", "0", "✓")
            )

            teclas.forEach { fila ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    fila.forEach { tecla ->
                        val w = when (tecla) {
                            "⌫", "✓" -> Modifier.weight(1f)
                            else -> Modifier.weight(1f)
                        }
                        Box(
                            modifier = w.height(52.dp)
                        ) {
                            TeclaPinTV(
                                texto = tecla,
                                esAccion = tecla == "⌫" || tecla == "✓",
                                alHacerClick = {
                                    when (tecla) {
                                        "⌫" -> {
                                            if (pinIngresado.isNotEmpty()) {
                                                pinIngresado = pinIngresado.dropLast(1)
                                                errorMsg = null
                                            }
                                        }
                                        "✓" -> {
                                            when {
                                                pinIngresado.length < 4 -> errorMsg = "Debe tener 4 dígitos"
                                                pinCorrecto != null && pinIngresado != pinCorrecto -> {
                                                    errorMsg = "PIN incorrecto"
                                                    pinIngresado = ""
                                                }
                                                else -> {
                                                    errorMsg = null
                                                    alConfirmar(pinIngresado)
                                                }
                                            }
                                        }
                                        else -> {
                                            if (pinIngresado.length < 4) {
                                                pinIngresado += tecla
                                                errorMsg = null
                                            }
                                        }
                                    }
                                }
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
            }

            // Botón cancelar
            Box(modifier = Modifier.width(200.dp)) {
                BotonDialogoTV(
                    texto = "Cancelar",
                    colorBase = Color.White.copy(alpha = 0.06f),
                    colorEnfocado = Color(0xFFFF4757).copy(alpha = 0.3f),
                    colorTextoBase = Color(0xFFFF4757),
                    colorTextoEnfocado = Color.White,
                    alHacerClick = alCancelar
                )
            }
        }
    }
}

@Composable
fun TeclaPinTV(
    texto: String,
    esAccion: Boolean,
    alHacerClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()

    val colorFondo = when {
        isFocused && esAccion && texto == "✓" -> Color(0xFF2ECC71)
        isFocused && esAccion -> Color(0xFFFF4757)
        isFocused -> AcentoPremium
        else -> Color.White.copy(alpha = 0.06f)
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(colorFondo)
            .border(
                1.dp,
                if (isFocused) Color.Transparent else Color.White.copy(alpha = 0.08f),
                RoundedCornerShape(14.dp)
            )
            .focusable(interactionSource = interactionSource)
            .clickable(interactionSource = interactionSource, indication = null, onClick = alHacerClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = texto,
            color = Color.White,
            fontSize = 22.sp,
            fontWeight = if (esAccion) FontWeight.Bold else FontWeight.Normal
        )
    }
}

@Composable
fun DialogoTemasTV(
    temaActual: String,
    alSeleccionar: (TemaApp) -> Unit,
    alCerrar: () -> Unit
) {
    val focusRequesterCerrar = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        focusRequesterCerrar.requestFocus()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.85f))
            .clickable(enabled = true, onClick = alCerrar),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .width(480.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color(0xFF1E1E30), Color(0xFF121222))
                    )
                )
                .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(24.dp))
                .padding(32.dp)
                .clickable(enabled = false) {},
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Default.ColorLens,
                contentDescription = null,
                tint = AcentoPremium,
                modifier = Modifier.size(40.dp)
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "Seleccionar Tema",
                color = Color.White,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(20.dp))

            val temas = TemaApp.entries.toList()
            val filas = temas.chunked(2)

            filas.forEach { fila ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    fila.forEach { tema ->
                        val seleccionado = tema.name.lowercase() == temaActual.lowercase()
                        ItemTemaTV(
                            tema = tema,
                            seleccionado = seleccionado,
                            modifier = Modifier.weight(1f),
                            alHacerClick = { alSeleccionar(tema) }
                        )
                    }
                    if (fila.size < 2) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
            }

            Spacer(modifier = Modifier.height(8.dp))
            Box(modifier = Modifier.width(200.dp)) {
                BotonDialogoTV(
                    texto = "Cerrar",
                    colorBase = Color.White.copy(alpha = 0.06f),
                    colorEnfocado = AcentoPremium,
                    colorTextoBase = Color.White.copy(alpha = 0.8f),
                    colorTextoEnfocado = Color.White,
                    alHacerClick = alCerrar
                )
            }
        }
    }
}

@Composable
fun ItemTemaTV(
    tema: TemaApp,
    seleccionado: Boolean,
    modifier: Modifier = Modifier,
    alHacerClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()

    val colorBorde = when {
        seleccionado -> AcentoPremium
        isFocused -> Color.White.copy(alpha = 0.4f)
        else -> Color.White.copy(alpha = 0.08f)
    }

    Column(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(
                if (isFocused) AcentoPremium.copy(alpha = 0.15f)
                else Color.White.copy(alpha = 0.04f)
            )
            .border(1.dp, colorBorde, RoundedCornerShape(14.dp))
            .focusable(interactionSource = interactionSource)
            .clickable(interactionSource = interactionSource, indication = null, onClick = alHacerClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(
                    if (seleccionado) AcentoPremium else Color.White.copy(alpha = 0.1f)
                ),
            contentAlignment = Alignment.Center
        ) {
            if (seleccionado) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = tema.nombreAMostrar,
            color = if (isFocused || seleccionado) Color.White else Color.White.copy(alpha = 0.6f),
            fontSize = 13.sp,
            fontWeight = if (seleccionado) FontWeight.Bold else FontWeight.Normal,
            textAlign = TextAlign.Center
        )
    }
}

// ══════════════════════════════════════════════
// COMPONENTES EXISTENTES (REUTILIZADOS)
// ══════════════════════════════════════════════

@Composable
fun SeccionAjustesTV(titulo: String, icono: ImageVector, contenido: @Composable () -> Unit) {
    Column {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(bottom = 12.dp)
        ) {
            Icon(icono, contentDescription = null, tint = AcentoPremium, modifier = Modifier.size(22.dp))
            Spacer(modifier = Modifier.width(10.dp))
            Text(titulo, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
        }
        Column(
            modifier = Modifier
                .clip(RoundedCornerShape(14.dp))
                .background(Color.White.copy(alpha = 0.04f))
        ) {
            contenido()
        }
    }
}

@Composable
fun ItemSwitchTV(
    icono: ImageVector,
    titulo: String,
    descripcion: String,
    activo: Boolean,
    alCambiar: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { alCambiar(!activo) }
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icono, contentDescription = null, tint = Color.White.copy(alpha = 0.6f), modifier = Modifier.size(22.dp))
        Spacer(modifier = Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(titulo, color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Medium)
            Text(descripcion, color = Color.White.copy(alpha = 0.4f), fontSize = 12.sp)
        }
        Switch(
            checked = activo,
            onCheckedChange = alCambiar,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = AcentoPremium,
                uncheckedTrackColor = Color.White.copy(alpha = 0.15f)
            )
        )
    }
}

@Composable
fun GuiaControlTV(
    icono: ImageVector,
    accion: String,
    descripcion: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icono, contentDescription = null, tint = AcentoPremium.copy(alpha = 0.7f), modifier = Modifier.size(22.dp))
        Spacer(modifier = Modifier.width(14.dp))
        Text(
            text = accion,
            color = Color.White,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.width(60.dp)
        )
        Text(
            text = descripcion,
            color = Color.White.copy(alpha = 0.5f),
            fontSize = 13.sp
        )
    }
}

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
            tint = AcentoPremium.copy(alpha = 0.7f),
            modifier = Modifier.size(20.dp)
        )
        Column {
            Text(
                text = etiqueta,
                color = Color.White.copy(alpha = 0.4f),
                fontSize = 11.sp
            )
            Text(
                text = valor,
                color = Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}


