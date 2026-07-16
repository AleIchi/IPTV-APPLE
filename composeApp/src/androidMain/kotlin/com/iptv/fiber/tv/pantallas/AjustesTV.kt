package com.iptv.fiber.tv.pantallas

// removed android import: import android.content.Context
// removed android import: import android.content.ActivityNotFoundException
// removed android import: import android.content.Intent
// removed android import: import android.net.Uri
// removed android import: import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.input.pointer.pointerInput
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.Canvas
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.iptv.fiber.datos.repositorio.RepositorioAutenticacion
import com.iptv.fiber.interfaz.modelovista.ModeloVistaAutenticacion
import com.iptv.fiber.interfaz.modelovista.ModeloVistaContenido
import com.iptv.fiber.interfaz.tema.AcentoPremium
import com.iptv.fiber.datos.local.GestorPreferencias
import com.iptv.fiber.tv.componentes.BotonDialogoTV
import com.iptv.fiber.tv.componentes.DialogoConfirmacionTV
import com.iptv.fiber.tv.componentes.TemaTV
import com.iptv.fiber.tv.componentes.tvClickableWithLongClick
import com.iptv.fiber.tv.ajustes.*
import com.iptv.fiber.tv.dialogos.*
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.launch

/** Pantalla de ajustes de cuenta TV: muestra info de suscripción, opciones de configuración y cierre de sesión. */
@Composable
fun AjustesTV(
    modeloVista: ModeloVistaContenido,
    modeloVistaAuth: ModeloVistaAutenticacion,
    requeridorFocoContenido: FocusRequester = remember { FocusRequester() }
) {
    val servidorActual by modeloVistaAuth.servidorActual.collectAsStateWithLifecycle()
    val contexto = LocalContext.current
    val scope = rememberCoroutineScope()

    val preferencias = remember {
        contexto.getSharedPreferences("iptv_preferencias", Context.MODE_PRIVATE)
    }
    val gestorPreferencias = remember { GestorPreferencias(contexto) }
    var direccionMac by remember { mutableStateOf("Cargando...") }

    LaunchedEffect(Unit) {
        direccionMac = gestorPreferencias.obtenerOGenerarMacVirtual()
    }

    val fechaExpiracion by gestorPreferencias.fechaExpiracion.collectAsStateWithLifecycle(initialValue = "")
    val maxConexiones by gestorPreferencias.maxConexiones.collectAsStateWithLifecycle(initialValue = "")
    val estadoCuenta by gestorPreferencias.estadoCuenta.collectAsStateWithLifecycle(initialValue = "")
    val controlParentalActivo by gestorPreferencias.controlParentalActivo.collectAsStateWithLifecycle(initialValue = false)
    val pinParental by gestorPreferencias.pinParental.collectAsStateWithLifecycle(initialValue = "")
    val calidadVideo by gestorPreferencias.calidadVideo.collectAsStateWithLifecycle(initialValue = "Automático")

    var reproduccionAutomaticaActiva by remember { mutableStateOf(preferencias.getBoolean("reproduccion_automatica_inicio", false)) }
    var modoBufferEstable by remember { mutableStateOf(preferencias.getBoolean("modo_buffer_estable", false)) }
    var mostrarConfirmacionSalir by remember { mutableStateOf(false) }

    // Estados de diálogos
    var mostrarDialogoHistorial by remember { mutableStateOf(false) }
    var mostrarDialogoCache by remember { mutableStateOf(false) }
    var mostrarPinSetup by remember { mutableStateOf(false) }
    var mostrarPinVerify by remember { mutableStateOf(false) }
    var accionParentalPendiente by remember { mutableStateOf<String?>(null) }

    // Diálogos legales
    var mostrarTerminos by remember { mutableStateOf(false) }
    var mostrarPrivacidad by remember { mutableStateOf(false) }
    var mostrarSoporte by remember { mutableStateOf(false) }
    var mostrarAcercaDe by remember { mutableStateOf(false) }

    // Calcular días restantes de suscripción
    val diasRestantes = remember(fechaExpiracion) {
        try {
            if (fechaExpiracion.isNotEmpty() && fechaExpiracion != "null" && fechaExpiracion != "0") {
                val expMs = fechaExpiracion.toLong() * 1000
                val ahora = System.currentTimeMillis()
                val diff = expMs - ahora
                if (diff > 0) (diff / (1000 * 60 * 60 * 24)).toInt() else 0
            } else -1
        } catch (_: Exception) { -1 }
    }

    val fechaFormateada = remember(fechaExpiracion) {
        try {
            if (fechaExpiracion.isNotEmpty() && fechaExpiracion != "null" && fechaExpiracion != "0") {
                val fecha = java.util.Date(fechaExpiracion.toLong() * 1000)
                val formato = java.text.SimpleDateFormat("dd 'de' MMMM, yyyy", java.util.Locale("es", "PE"))
                formato.format(fecha)
            } else "Indefinida"
        } catch (_: Exception) { "Indefinida" }
    }

    // ─── Interfaz principal ───
    LaunchedEffect(Unit) {
        try { requeridorFocoContenido.requestFocus() } catch (_: Exception) {}
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .background(TemaTV.FondoPrincipal)
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
                    .padding(horizontal = 20.dp, vertical = 18.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(52.dp)
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
                        modifier = Modifier.size(28.dp)
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
                        TemaTV.Exito.copy(alpha = 0.15f)
                    else
                        TemaTV.Peligro.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text(
                        text = if (estadoCuenta.equals("Active", ignoreCase = true)) "● Suscripción activa" else "● Verificando...",
                        color = if (estadoCuenta.equals("Active", ignoreCase = true)) Color(0xFF2ECC71) else Color(0xFFFF6B6B),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 5.dp)
                    )
                }

                val interaccionSuscripcion = remember { MutableInteractionSource() }
                val suscripcionEnFoco by interaccionSuscripcion.collectIsFocusedAsState()
                val escalaSuscripcion by animateFloatAsState(targetValue = if (suscripcionEnFoco) 1.02f else 1f, label = "escala_suscripcion")

                // graphicsLayer: evita re-medición del Column y su subárbol de textos al animarse
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .graphicsLayer {
                            scaleX = escalaSuscripcion
                            scaleY = escalaSuscripcion
                        }
                        .clip(RoundedCornerShape(16.dp))
                        .background(if (suscripcionEnFoco) Color.White.copy(alpha = 0.08f) else Color.White.copy(alpha = 0.04f))
                        .border(
                            width = if (suscripcionEnFoco) 1.5.dp else 0.dp,
                            color = if (suscripcionEnFoco) TemaTV.AcentoClaro.copy(alpha = 0.5f) else Color.Transparent,
                            shape = RoundedCornerShape(16.dp)
                        )
                        .focusable(interactionSource = interaccionSuscripcion)
                        .padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
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
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Start
                        ) {
                            Text(
                                text = "$diasRestantes",
                                color = if (diasRestantes > 15) TemaTV.AcentoClaro else TemaTV.Peligro,
                                fontSize = 44.sp,
                                fontWeight = FontWeight.ExtraBold
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "días\nrestantes",
                                color = Color.White.copy(alpha = 0.6f),
                                fontSize = 13.sp,
                                lineHeight = 15.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }

                        if (diasRestantes in 0..365) {
                            val progreso = (diasRestantes.toFloat() / 30f).coerceIn(0f, 1f)
                            LinearProgressIndicator(
                                progress = progreso,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(5.dp)
                                    .clip(RoundedCornerShape(2.5.dp)),
                                color = if (diasRestantes > 15) TemaTV.AcentoClaro else TemaTV.Peligro,
                                trackColor = TemaTV.Linea
                            )
                        } else {
                            Divider(color = Color.White.copy(alpha = 0.06f))
                        }
                    } else {
                        Divider(color = Color.White.copy(alpha = 0.06f))
                    }

                    FilaInfoSuscripcion(
                        icono = Icons.Default.Tv,
                        etiqueta = "ID del Dispositivo (MAC)",
                        valor = direccionMac
                    )

                    FilaInfoSuscripcion(
                        icono = Icons.Default.Event,
                        etiqueta = if (fechaFormateada == "Indefinida") "Vencimiento" else "Vence el",
                        valor = fechaFormateada
                    )

                    if (maxConexiones.isNotEmpty() && maxConexiones != "null") {
                        val sufijoConexiones = if (maxConexiones == "1") "simultáneo" else "simultáneos"
                        FilaInfoSuscripcion(
                            icono = Icons.Default.Devices,
                            etiqueta = "Dispositivos permitidos",
                            valor = "$maxConexiones $sufijoConexiones"
                        )
                    }
                }

                Spacer(modifier = Modifier.weight(1f))

                val fuenteInteraccionCerrar = remember { MutableInteractionSource() }
                val cerrarEnFoco by fuenteInteraccionCerrar.collectIsFocusedAsState()

                val cerrarEscala by animateFloatAsState(
                    targetValue = if (cerrarEnFoco) 1.04f else 1f,
                    animationSpec = tween(durationMillis = 65),
                    label = "escala_cerrar_sesion"
                )

                Box(
                    modifier = Modifier
                        .width(220.dp)
                        .graphicsLayer {
                            scaleX = cerrarEscala
                            scaleY = cerrarEscala
                        }
                        .height(44.dp)
                        .clip(RoundedCornerShape(22.dp))
                        .background(if (cerrarEnFoco) TemaTV.Peligro else TemaTV.Peligro.copy(alpha = 0.12f))
                        .border(
                            width = if (cerrarEnFoco) 1.5.dp else 0.dp,
                            color = if (cerrarEnFoco) TemaTV.Peligro.copy(alpha = 0.5f) else Color.Transparent,
                            shape = RoundedCornerShape(22.dp)
                        )
                        .tvClickableWithLongClick(
                            interactionSource = fuenteInteraccionCerrar,
                            onClick = { mostrarConfirmacionSalir = true }
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.ExitToApp,
                            contentDescription = null,
                            tint = if (cerrarEnFoco) Color.White else Color(0xFFFF4757),
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Cerrar sesión",
                            color = if (cerrarEnFoco) Color.White else Color(0xFFFF4757),
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }
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
                        titulo = "Abrir TV en vivo al iniciar",
                        descripcion = "Entrar directo a la sección en vivo",
                        activo = reproduccionAutomaticaActiva,
                        alCambiar = {
                            reproduccionAutomaticaActiva = it
                            preferencias.edit().putBoolean("reproduccion_automatica_inicio", it).apply()
                        },
                        focusRequester = requeridorFocoContenido
                    )
                    Divider(color = Color.White.copy(alpha = 0.04f))
                    ItemSwitchTV(
                        icono = Icons.Default.Speed,
                        titulo = "Optimizar para conexión lenta",
                        descripcion = "Aumenta el búfer para reducir cortes en red inestable",
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
                        titulo = "Bloquear Ajustes con PIN",
                        descripcion = if (controlParentalActivo) "Activado — se requiere PIN para entrar a Ajustes" else "Proteger sección de Ajustes/Cuenta con un PIN",
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
                                    accionParentalPendiente = "desactivar"
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
                            descripcion = "Actualizar el PIN de acceso a los Ajustes",
                            alHacerClick = {
                                accionParentalPendiente = "cambiar_pin"
                                mostrarPinVerify = true
                            }
                        )
                    }
                }

                // ── Sección: Datos y Almacenamiento ──
                SeccionAjustesTV("Datos y Almacenamiento", Icons.Default.Storage) {
                    ItemAccionTV(
                        icono = Icons.Default.History,
                        titulo = "Borrar historial de canales",
                        descripcion = "Eliminar todo el historial de reproducción",
                        alHacerClick = { mostrarDialogoHistorial = true }
                    )
                    Divider(color = Color.White.copy(alpha = 0.04f))
                    ItemAccionTV(
                        icono = Icons.Default.CleaningServices,
                        titulo = "Limpiar caché de la aplicación",
                        descripcion = "Recargar la lista de canales desde el servidor",
                        alHacerClick = { mostrarDialogoCache = true }
                    )
                }

                // ── Sección: Calidad de Video ──
                SeccionAjustesTV("Calidad de Video", Icons.Default.HighQuality) {
                    val opcionesCalidad = listOf("Automático", "1080p", "720p", "480p")
                    opcionesCalidad.forEachIndexed { index, opcion ->
                        val fuenteInteraccion = remember { MutableInteractionSource() }
                        val estaEnfocado by fuenteInteraccion.collectIsFocusedAsState()
                        val escala by animateFloatAsState(
                            targetValue = if (estaEnfocado) 1.02f else 1f,
                            animationSpec = tween(durationMillis = 65),
                            label = "escala_calidad_$opcion"
                        )
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .graphicsLayer {
                                    scaleX = escala
                                    scaleY = escala
                                }
                                .tvClickableWithLongClick(
                                    interactionSource = fuenteInteraccion,
                                    onClick = { scope.launch { gestorPreferencias.establecerCalidadVideo(opcion) } }
                                )
                                .background(
                                    if (estaEnfocado) TemaTV.Acento.copy(alpha = 0.16f) else Color.Transparent,
                                    RoundedCornerShape(12.dp)
                                )
                                .border(
                                    width = if (estaEnfocado) 1.5.dp else 0.dp,
                                    color = if (estaEnfocado) TemaTV.AcentoClaro else Color.Transparent,
                                    shape = RoundedCornerShape(12.dp)
                                )
                                .padding(horizontal = 16.dp, vertical = 14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = when (opcion) {
                                    "Automático" -> Icons.Default.AutoAwesome
                                    "1080p" -> Icons.Default.Hd
                                    "720p" -> Icons.Default.Hd
                                    else -> Icons.Default.Sd
                                },
                                contentDescription = null,
                                tint = if (calidadVideo == opcion) TemaTV.AcentoClaro else TemaTV.TextoSecundario,
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.width(14.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = opcion,
                                    color = if (estaEnfocado || calidadVideo == opcion) TemaTV.TextoPrincipal else TemaTV.TextoSecundario,
                                    fontSize = 15.sp,
                                    fontWeight = if (calidadVideo == opcion) FontWeight.Bold else FontWeight.Medium
                                )
                                Text(
                                    text = when (opcion) {
                                        "Automático" -> "El servidor elige la mejor calidad disponible"
                                        "1080p" -> "Full HD — requiere buena conexión"
                                        "720p" -> "HD — equilibrio entre calidad y datos"
                                        else -> "SD — para conexiones lentas"
                                    },
                                    color = TemaTV.TextoTenue,
                                    fontSize = 12.sp
                                )
                            }
                            if (calidadVideo == opcion) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = TemaTV.AcentoClaro,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                        if (index < opcionesCalidad.lastIndex) {
                            Divider(color = Color.White.copy(alpha = 0.04f))
                        }
                    }
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
                        icono = Icons.Default.Phone,
                        titulo = "Soporte Técnico",
                        descripcion = "Contacto y asistencia en línea",
                        alHacerClick = { mostrarSoporte = true }
                    )
                    Divider(color = Color.White.copy(alpha = 0.04f))
                    ItemAccionTV(
                        icono = Icons.Default.Info,
                        titulo = "Acerca de Fiber Z Telecom",
                        descripcion = "Versión de la app, dirección y datos oficiales",
                        alHacerClick = { mostrarAcercaDe = true }
                    )
                    Divider(color = Color.White.copy(alpha = 0.04f))

                    // Tarjeta de contacto directo
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color.White.copy(alpha = 0.03f))
                            .border(1.dp, Color.White.copy(alpha = 0.05f), RoundedCornerShape(16.dp))
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF2ECC71).copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Phone,
                                    contentDescription = null,
                                    tint = Color(0xFF2ECC71),
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Column {
                                Text(
                                    text = "Contacto Oficial",
                                    color = TemaTV.TextoPrincipal,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Fiber Z Telecom — soporte@fiberztelecom.com",
                                    color = TemaTV.AcentoClaro,
                                    fontSize = 11.sp
                                )
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Column {
                                    Text(
                                        text = "Soporte Técnico (WhatsApp)",
                                        color = TemaTV.TextoSecundario,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                    Text(
                                        text = "+51 982 497 670",
                                        color = TemaTV.TextoPrincipal,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                Column {
                                    Text(
                                        text = "Atención al Cliente / Ventas",
                                        color = TemaTV.TextoSecundario,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                    Text(
                                        text = "+51 982 497 650",
                                        color = TemaTV.TextoPrincipal,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }

                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                CodigoQRPremium(
                                    size = 76.dp,
                                    modifier = Modifier
                                        .border(1.dp, Color(0xFF2ECC71).copy(alpha = 0.3f), RoundedCornerShape(10.dp))
                                        .padding(4.dp)
                                )
                                Text(
                                    text = "Escanea para chatear",
                                    color = TemaTV.TextoTenue,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // ── Pie de página de la empresa ──
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color.White.copy(alpha = 0.03f))
                        .padding(horizontal = 20.dp, vertical = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Image(
                        painter = painterResource(id = com.iptv.fiber.R.drawable.logotipo_fiber_z),
                        contentDescription = "Fiber Z TV+",
                        modifier = Modifier
                            .height(28.dp)
                            .widthIn(max = 160.dp),
                        contentScale = ContentScale.Fit
                    )
                    Text(
                        text = "Versión 1.0.0",
                        color = Color.White.copy(alpha = 0.35f),
                        fontSize = 11.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Fiber Z Telecom S.A.C. · RUC 20604599459",
                        color = Color.White.copy(alpha = 0.22f),
                        fontSize = 10.sp
                    )
                    Text(
                        text = "Calle La Florida 775, Huancayo · Junín, Perú",
                        color = Color.White.copy(alpha = 0.18f),
                        fontSize = 10.sp
                    )
                }
            }
        }

        // ─── Diálogos e Interacciones (Renderizados arriba del Box principal) ───

        // ─── Diálogo: Borrar Historial ───
        if (mostrarDialogoHistorial) {
            DialogoConfirmacionTV(
                titulo = "Borrar historial",
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
                titulo = "Limpiar caché",
                descripcion = "Esto recargará la lista de canales desde el servidor.",
                textoConfirmar = "Sí, limpiar",
                textoCancelar = "Cancelar",
                alConfirmar = {
                    modeloVista.limpiarCache()
                    modeloVista.cargarCategoriasEnVivo()
                    modeloVista.cargarCanalesEnVivo()
                    mostrarDialogoCache = false
                },
                alCancelar = { mostrarDialogoCache = false }
            )
        }

        // ─── Diálogo: configuración de PIN (primera vez) ───
        if (mostrarPinSetup) {
            DialogoPinTV(
                titulo = "Establecer PIN",
                descripcion = "Ingresa un PIN de 4 dígitos para proteger los Ajustes",
                alConfirmar = { pin ->
                    scope.launch {
                        gestorPreferencias.establecerPinParental(pin)
                        gestorPreferencias.establecerControlParental(true)
                    }
                    accionParentalPendiente = null
                    mostrarPinSetup = false
                },
                alCancelar = {
                    mostrarPinSetup = false
                }
            )
        }

        // ─── Diálogo: verificación de PIN (para desactivar) ───
        if (mostrarPinVerify) {
            DialogoPinTV(
                titulo = "Ingresar PIN",
                descripcion = "Ingresa tu PIN de seguridad para continuar",
                pinCorrecto = pinParental,
                alConfirmar = { pin ->
                    if (pin == pinParental) {
                        when (accionParentalPendiente) {
                            "cambiar_pin" -> mostrarPinSetup = true
                            else -> scope.launch { gestorPreferencias.establecerControlParental(false) }
                        }
                    }
                    accionParentalPendiente = null
                    mostrarPinVerify = false
                },
                alCancelar = {
                    accionParentalPendiente = null
                    mostrarPinVerify = false
                }
            )
        }



        // ─── Diálogos Legales ───
        if (mostrarTerminos) {
            DialogoLegalTV(
                titulo = "Términos y Condiciones",
                texto = "Bienvenido a Fiber Z TV+. El uso de esta aplicación está sujeto a los lineamientos de Fiber Z Telecom (RUC 20604599459). El contenido audiovisual se provee exclusivamente para uso doméstico a través de nuestra red de fibra óptica de alta velocidad. Queda prohibida la redistribución de la señal. Al utilizar la aplicación, aceptas nuestros términos de servicio.",
                enlace = "https://fiberztelecom.com/terminos",
                alCerrar = { mostrarTerminos = false }
            )
        }
        if (mostrarPrivacidad) {
            DialogoLegalTV(
                titulo = "Política de Privacidad",
                texto = "En Fiber Z Telecom respetamos su privacidad. Los datos recopilados por esta aplicación (como su historial de reproducción local y lista de canales favoritos) se procesan localmente en su dispositivo de televisión y no se comparten con terceros. Las credenciales de acceso solo se utilizan de forma segura para validar su suscripción activa.",
                enlace = "https://fiberztelecom.com/privacidad",
                alCerrar = { mostrarPrivacidad = false }
            )
        }
        if (mostrarSoporte) {
            DialogoLegalTV(
                titulo = "Soporte Técnico",
                texto = "En Fiber Z Telecom nuestro compromiso es brindarte la mejor señal de televisión digital por fibra óptica. Si tienes alguna duda, necesitas ayuda para configurar tu cuenta o experimentas algún inconveniente, nuestro equipo de soporte está listo para asistirte de inmediato.\n\nCorreo: soporte@fiberztelecom.com\nTeléfono/WhatsApp: +51 982 497 670",
                enlace = "https://wa.me/51982497670",
                alCerrar = { mostrarSoporte = false }
            )
        }
        if (mostrarAcercaDe) {
            DialogoLegalTV(
                titulo = "Acerca de Fiber Z Telecom",
                texto = "Fiber Z TV+ es la plataforma oficial de entretenimiento digital de Fiber Z Telecom, diseñada para ofrecer la máxima calidad en transmisión de TV sobre nuestra red de fibra óptica.\n\nEmpresa: Fiber Z Telecom S.A.C.\nRUC: 20604599459\nDirección: Calle La Florida 775, Huancayo (a espaldas del Cuartel 9 de Diciembre), Junín, Perú\nTeléfono Soporte: +51 982 497 670\nAtención Comercial: +51 982 497 650\nWeb: fiberztelecom.com\nCorreo: soporte@fiberztelecom.com\n\nVersión: 1.0.0\n© 2026 Fiber Z Telecom. Todos los derechos reservados.",
                enlace = "https://fiberztelecom.com",
                alCerrar = { mostrarAcercaDe = false }
            )
        }

        // ─── Confirmación Cerrar Sesión ───
        if (mostrarConfirmacionSalir) {
            DialogoConfirmacionTV(
                alConfirmar = {
                    mostrarConfirmacionSalir = false
                    // Limpiar canales antes de cerrar sesión para que no aparezca
                    // la lista anterior al iniciar con una nueva cuenta o lista
                    modeloVista.reiniciarEstado()
                    modeloVistaAuth.cerrarSesion()
                },
                alCancelar = { mostrarConfirmacionSalir = false }
            )
        }
    }
}
