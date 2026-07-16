package com.iptv.fiber.interfaz.ajustes

// removed android import: import android.content.Context
// removed android import: import android.content.Intent
// removed android import: import android.net.Uri

import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.iptv.fiber.datos.local.GestorPreferencias
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.iptv.fiber.interfaz.modelovista.ModeloVistaAutenticacion
import com.iptv.fiber.interfaz.modelovista.ModeloVistaContenido
import kotlinx.coroutines.launch

// ── Pantalla principal ───────────────────────────────────────────────────────
/**
 * Muestra la pantalla de ajustes con secciones de perfil, reproducción, seguridad y legal.
 * Gestiona los diálogos de PIN de control parental, limpieza de historial y cierre de sesión.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PantallaAjustes(
    modeloVista: ModeloVistaContenido,
    modeloVistaAuth: ModeloVistaAutenticacion,
    alCerrarSesion: () -> Unit
) {
    val manejadorUris = androidx.compose.ui.platform.LocalUriHandler.current
    val gestorPreferencias = remember { com.iptv.fiber.datos.local.GestorPreferencias() }
    val alcance = rememberCoroutineScope()

    // ── Estado de preferencias ──
    val controlParentalActivo by gestorPreferencias.controlParentalActivo.collectAsStateWithLifecycle(initialValue = false)
    val pinParental by gestorPreferencias.pinParental.collectAsStateWithLifecycle(initialValue = "")
    val bloquearCaptura by gestorPreferencias.bloquearCaptura.collectAsStateWithLifecycle(initialValue = false)
    val calidadVideo by gestorPreferencias.calidadVideo.collectAsStateWithLifecycle(initialValue = "Automático")

    // ── Datos de cuenta ──
    val servidorActual by modeloVistaAuth.servidorActual.collectAsStateWithLifecycle()
    val fechaExpiracion by gestorPreferencias.fechaExpiracion.collectAsStateWithLifecycle(initialValue = "")
    val maxConexiones by gestorPreferencias.maxConexiones.collectAsStateWithLifecycle(initialValue = "")
    val estadoCuenta by gestorPreferencias.estadoCuenta.collectAsStateWithLifecycle(initialValue = "")

    // ── Estado de diálogos ──
    var mostrarDialogoHistorial by remember { mutableStateOf(false) }
    var mostrarDialogoCache by remember { mutableStateOf(false) }
    var mostrarConfirmacionCerrarSesion by remember { mutableStateOf(false) }
    var mostrarTerminos by remember { mutableStateOf(false) }
    var mostrarPrivacidad by remember { mutableStateOf(false) }
    var mostrarPinSetup by remember { mutableStateOf(false) }
    var mostrarPinVerify by remember { mutableStateOf(false) }
    var accionParentalPendiente by remember { mutableStateOf<String?>(null) }
    var mostrarSelectorCalidad by remember { mutableStateOf(false) }

    // ── Bloqueo de captura de pantalla ──
    com.iptv.fiber.EfectoBloqueoCaptura(bloquear = bloquearCaptura)

    // ── Calcular suscripción ──
    val diasRestantes = remember(fechaExpiracion) {
        try {
            if (fechaExpiracion.isNotEmpty() && fechaExpiracion != "null" && fechaExpiracion != "0") {
                val diferencia = (fechaExpiracion.toLong() * 1000) - System.currentTimeMillis()
                if (diferencia > 0) (diferencia / (1000 * 60 * 60 * 24)).toInt() else 0
            } else -1
        } catch (_: Exception) { -1 }
    }
    val fechaFormateada = remember(fechaExpiracion) {
        try {
            if (fechaExpiracion.isNotEmpty() && fechaExpiracion != "null" && fechaExpiracion != "0") {
                val fecha = java.util.Date(fechaExpiracion.toLong() * 1000)
                java.text.SimpleDateFormat("dd 'de' MMMM, yyyy", java.util.Locale("es", "PE")).format(fecha)
            } else "Indefinida"
        } catch (_: Exception) { "Indefinida" }
    }

    // ── Diálogos ────────────────────────────────────────────────────────────

    if (mostrarPinSetup) {
        DialogoPin(
            titulo = "Crear PIN",
            descripcion = "Establece un PIN de 4 dígitos para proteger los Ajustes",
            alConfirmar = { pin ->
                alcance.launch {
                    gestorPreferencias.establecerPinParental(pin)
                    gestorPreferencias.establecerControlParental(true)
                }
                mostrarPinSetup = false
            },
            alCancelar = { mostrarPinSetup = false }
        )
    }

    if (mostrarPinVerify) {
        DialogoPin(
            titulo = "Ingresar PIN",
            descripcion = "Ingresa tu PIN para continuar",
            pinCorrecto = pinParental,
            alConfirmar = { pin ->
                if (pin == pinParental) {
                    when (accionParentalPendiente) {
                        "cambiar_pin" -> { mostrarPinVerify = false; mostrarPinSetup = true; accionParentalPendiente = null; return@DialogoPin }
                        "desactivar" -> alcance.launch { gestorPreferencias.establecerControlParental(false) }
                    }
                }
                accionParentalPendiente = null
                mostrarPinVerify = false
            },
            alCancelar = { accionParentalPendiente = null; mostrarPinVerify = false }
        )
    }

    if (mostrarDialogoHistorial) {
        AlertDialog(
            onDismissRequest = { mostrarDialogoHistorial = false },
            containerColor = ColorTarjetaAlta,
            titleContentColor = Color.White,
            textContentColor = ColorTextoGris,
            title = { Text("Borrar historial") },
            text = { Text("¿Deseas eliminar tu historial de canales vistos? Esta acción no se puede deshacer.") },
            confirmButton = {
                TextButton(onClick = { modeloVista.limpiarHistorial(); mostrarDialogoHistorial = false }) {
                    Text("Borrar", color = ColorError, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { mostrarDialogoHistorial = false }) { Text("Cancelar", color = Color.White) }
            }
        )
    }

    if (mostrarConfirmacionCerrarSesion) {
        AlertDialog(
            onDismissRequest = { mostrarConfirmacionCerrarSesion = false },
            containerColor = ColorTarjetaAlta,
            titleContentColor = Color.White,
            textContentColor = ColorTextoGris,
            title = { Text("Cerrar Sesión") },
            text = { Text("¿Estás seguro? Deberás ingresar tus credenciales de nuevo para acceder.") },
            confirmButton = {
                TextButton(onClick = { mostrarConfirmacionCerrarSesion = false; alCerrarSesion() }) {
                    Text("Sí, cerrar sesión", color = ColorError, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { mostrarConfirmacionCerrarSesion = false }) { Text("Cancelar", color = Color.White) }
            }
        )
    }

    if (mostrarDialogoCache) {
        AlertDialog(
            onDismissRequest = { mostrarDialogoCache = false },
            containerColor = ColorTarjetaAlta,
            titleContentColor = Color.White,
            textContentColor = ColorTextoGris,
            title = { Text("Limpiar Caché") },
            text = { Text("Esto recargará la lista de canales desde el servidor. Puede tardar unos segundos.") },
            confirmButton = {
                TextButton(onClick = { modeloVista.limpiarCache(); modeloVista.cargarCategoriasEnVivo(); modeloVista.cargarCanalesEnVivo(); mostrarDialogoCache = false }) {
                    Text("Limpiar", color = ColorAcentoAzul, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { mostrarDialogoCache = false }) { Text("Cancelar", color = Color.White) }
            }
        )
    }

    if (mostrarTerminos) {
        AlertDialog(
            onDismissRequest = { mostrarTerminos = false },
            containerColor = ColorTarjetaAlta, titleContentColor = Color.White, textContentColor = ColorTextoGris,
            title = { Text("Términos y Condiciones") },
            text = { Text("El uso de esta aplicación está sujeto a los lineamientos de Fiber Z Telecom (RUC 20604599459). El contenido audiovisual se provee exclusivamente para uso doméstico a través de nuestra red de fibra óptica. Queda prohibida la redistribución de la señal.") },
            confirmButton = { TextButton(onClick = { mostrarTerminos = false }) { Text("Aceptar", color = ColorAcentoAzul) } }
        )
    }

    if (mostrarPrivacidad) {
        AlertDialog(
            onDismissRequest = { mostrarPrivacidad = false },
            containerColor = ColorTarjetaAlta, titleContentColor = Color.White, textContentColor = ColorTextoGris,
            title = { Text("Política de Privacidad") },
            text = { Text("En Fiber Z Telecom respetamos su privacidad. Los datos recopilados (historial de reproducción, favoritos) se almacenan localmente en su dispositivo y no se comparten con terceros. Las credenciales solo se usan para validar su suscripción.") },
            confirmButton = { TextButton(onClick = { mostrarPrivacidad = false }) { Text("Entendido", color = ColorAcentoAzul) } }
        )
    }


    if (mostrarSelectorCalidad) {
        AlertDialog(
            onDismissRequest = { mostrarSelectorCalidad = false },
            containerColor = ColorTarjetaAlta, titleContentColor = Color.White, textContentColor = ColorTextoGris,
            title = { Text("Calidad de video") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    listOf(
                        "Automático" to "El servidor elige la mejor disponible",
                        "1080p" to "Full HD — requiere buena conexión",
                        "720p" to "HD — equilibrio entre calidad y datos",
                        "480p" to "SD — para conexiones lentas"
                    ).forEach { (opcion, desc) ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (calidadVideo == opcion) ColorAcentoAzul.copy(alpha = 0.15f) else Color.Transparent)
                                .clickable { alcance.launch { gestorPreferencias.establecerCalidadVideo(opcion) }; mostrarSelectorCalidad = false }
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(opcion, color = Color.White, fontSize = 15.sp, fontWeight = if (calidadVideo == opcion) FontWeight.Bold else FontWeight.Normal)
                                Text(desc, color = ColorTextoGris, fontSize = 12.sp)
                            }
                            if (calidadVideo == opcion) {
                                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = ColorAcentoAzul, modifier = Modifier.size(20.dp))
                            }
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { mostrarSelectorCalidad = false }) { Text("Cerrar", color = ColorTextoGris) } }
        )
    }

    // ── Scaffold principal ───────────────────────────────────────────────────
    Scaffold(
        containerColor = ColorFondoApp,
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Cuenta", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 20.sp) },
                navigationIcon = {
                    IconButton(onClick = { despachadorAtras?.onBackPressed() }) {
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
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            Spacer(modifier = Modifier.height(4.dp))

            // ═══════════════════════════════════════
            // SECCIÓN 1: PERFIL Y SUSCRIPCIÓN
            // ═══════════════════════════════════════
            EncabezadoSeccion("Perfil", Icons.Default.AccountCircle)

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = ColorTarjeta)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    // Avatar + nombre
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .clip(CircleShape)
                                .background(Brush.linearGradient(listOf(ColorAcentoAzul, ColorAcentoAzul.copy(alpha = 0.5f)))),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Person, contentDescription = null, tint = Color.White, modifier = Modifier.size(30.dp))
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Column {
                            Text(
                                text = servidorActual?.usuario ?: "Usuario",
                                color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold
                            )
                            Surface(
                                color = if (estadoCuenta.equals("Active", ignoreCase = true)) ColorExito.copy(alpha = 0.15f) else ColorError.copy(alpha = 0.15f),
                                shape = RoundedCornerShape(20.dp),
                                modifier = Modifier.padding(top = 4.dp)
                            ) {
                                Text(
                                    text = if (estadoCuenta.equals("Active", ignoreCase = true)) "● Suscripción activa" else "● Verificando...",
                                    color = if (estadoCuenta.equals("Active", ignoreCase = true)) ColorExito else ColorError,
                                    fontSize = 11.sp, fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    Divider(color = ColorDivisor, thickness = 0.5.dp)
                    Spacer(modifier = Modifier.height(16.dp))

                    // Datos de suscripción visibles
                    if (diasRestantes >= 0) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text("Días restantes", color = ColorTextoGris, fontSize = 12.sp)
                                Text(
                                    "$diasRestantes días",
                                    color = if (diasRestantes > 15) ColorExito else ColorError,
                                    fontSize = 24.sp,
                                    fontWeight = FontWeight.ExtraBold
                                )
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text("Vence el", color = ColorTextoGris, fontSize = 12.sp)
                                Text(fechaFormateada, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Medium, textAlign = TextAlign.End)
                            }
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        val progreso = (diasRestantes.toFloat() / 30f).coerceIn(0f, 1f)
                        LinearProgressIndicator(
                            progress = progreso,
                            modifier = Modifier.fillMaxWidth().height(5.dp).clip(RoundedCornerShape(3.dp)),
                            color = if (diasRestantes > 15) ColorExito else ColorError,
                            trackColor = ColorDivisor
                        )
                    } else {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.AllInclusive, contentDescription = null, tint = ColorAcentoAzul, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Suscripción indefinida", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                        }
                    }

                    if (maxConexiones.isNotEmpty() && maxConexiones != "null") {
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(Icons.Default.Devices, contentDescription = null, tint = ColorTextoGris, modifier = Modifier.size(16.dp))
                            Text("$maxConexiones dispositivos simultáneos permitidos", color = ColorTextoGris, fontSize = 12.sp)
                        }
                    }
                }
            }

            // ═══════════════════════════════════════
            // SECCIÓN 2: REPRODUCCIÓN
            // ═══════════════════════════════════════
            EncabezadoSeccion("Reproducción", Icons.Default.PlayCircle)

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = ColorTarjeta)
            ) {
                Column {
                    var reproduccionAlIniciar by remember { mutableStateOf(preferencias.getBoolean("reproduccion_automatica_inicio", false)) }
                    var pip by remember { mutableStateOf(preferencias.getBoolean("pip_activo", true)) }
                    var bufferEstable by remember { mutableStateOf(preferencias.getBoolean("modo_buffer_estable", false)) }

                    FilaAjuste(
                        icono = Icons.Default.SmartDisplay,
                        titulo = "Reproducir al iniciar",
                        descripcion = "Ir directo al último canal al abrir la app",
                        colorIcono = ColorAcentoAzul,
                        mostrarFlecha = false,
                        componenteFinal = {
                            Switch(
                                checked = reproduccionAlIniciar,
                                onCheckedChange = { reproduccionAlIniciar = it; preferencias.edit().putBoolean("reproduccion_automatica_inicio", it).apply() },
                                colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = ColorAcentoAzul, uncheckedTrackColor = ColorTextoTenue)
                            )
                        }
                    )
                    Divider(color = ColorDivisor, thickness = 0.5.dp, modifier = Modifier.padding(horizontal = 16.dp))
                    Divider(color = ColorDivisor, thickness = 0.5.dp, modifier = Modifier.padding(horizontal = 16.dp))
                    FilaAjuste(
                        icono = Icons.Default.PictureInPictureAlt,
                        titulo = "Ventana flotante (PiP)",
                        descripcion = "Minimizar el video al salir de la app",
                        colorIcono = ColorAcentoAzul,
                        mostrarFlecha = false,
                        componenteFinal = {
                            Switch(
                                checked = pip,
                                onCheckedChange = { pip = it; preferencias.edit().putBoolean("pip_activo", it).apply() },
                                colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = ColorAcentoAzul, uncheckedTrackColor = ColorTextoTenue)
                            )
                        }
                    )
                    Divider(color = ColorDivisor, thickness = 0.5.dp, modifier = Modifier.padding(horizontal = 16.dp))
                    FilaAjuste(
                        icono = Icons.Default.Speed,
                        titulo = "Optimizar para conexión lenta",
                        descripcion = "Aumenta el búfer para reducir cortes en red inestable",
                        colorIcono = ColorAcentoAzul,
                        mostrarFlecha = false,
                        componenteFinal = {
                            Switch(
                                checked = bufferEstable,
                                onCheckedChange = { bufferEstable = it; preferencias.edit().putBoolean("modo_buffer_estable", it).apply() },
                                colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = ColorAcentoAzul, uncheckedTrackColor = ColorTextoTenue)
                            )
                        }
                    )
                    Divider(color = ColorDivisor, thickness = 0.5.dp, modifier = Modifier.padding(horizontal = 16.dp))
                    FilaAjuste(
                        icono = Icons.Default.HighQuality,
                        titulo = "Calidad de video",
                        descripcion = "Preferencia actual: $calidadVideo",
                        colorIcono = ColorAcentoAzul,
                        alHacerClick = { mostrarSelectorCalidad = true }
                    )
                }
            }

            // ═══════════════════════════════════════
            // SECCIÓN 3: SEGURIDAD
            // ═══════════════════════════════════════
            EncabezadoSeccion("Seguridad", Icons.Default.Security, colorIcono = Color(0xFF7B68EE))

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = ColorTarjeta)
            ) {
                Column {
                    FilaAjuste(
                        icono = Icons.Default.Lock,
                        titulo = "Bloquear Ajustes con PIN",
                        descripcion = if (controlParentalActivo) "Activado — se requiere PIN para entrar a Ajustes" else "Proteger sección de Ajustes/Cuenta con un PIN",
                        colorIcono = Color(0xFF7B68EE),
                        mostrarFlecha = false,
                        componenteFinal = {
                            Switch(
                                checked = controlParentalActivo,
                                onCheckedChange = { activar ->
                                    if (activar) {
                                        if (pinParental.isEmpty()) mostrarPinSetup = true
                                        else alcance.launch { gestorPreferencias.establecerControlParental(true) }
                                    } else {
                                        if (pinParental.isNotEmpty()) { accionParentalPendiente = "desactivar"; mostrarPinVerify = true }
                                        else alcance.launch { gestorPreferencias.establecerControlParental(false) }
                                    }
                                },
                                colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = Color(0xFF7B68EE), uncheckedTrackColor = ColorTextoTenue)
                            )
                        }
                    )
                    if (pinParental.isNotEmpty()) {
                        Divider(color = ColorDivisor, thickness = 0.5.dp, modifier = Modifier.padding(horizontal = 16.dp))
                        FilaAjuste(
                            icono = Icons.Default.LockReset,
                            titulo = "Cambiar PIN",
                            descripcion = "Actualizar el PIN de acceso a los Ajustes",
                            colorIcono = Color(0xFF7B68EE),
                            alHacerClick = { accionParentalPendiente = "cambiar_pin"; mostrarPinVerify = true }
                        )
                    }
                    Divider(color = ColorDivisor, thickness = 0.5.dp, modifier = Modifier.padding(horizontal = 16.dp))
                    FilaAjuste(
                        icono = Icons.Default.Screenshot,
                        titulo = "Bloquear capturas de pantalla",
                        descripcion = "Evita que se grabe o capturen pantallas de la app",
                        colorIcono = Color(0xFF7B68EE),
                        mostrarFlecha = false,
                        componenteFinal = {
                            Switch(
                                checked = bloquearCaptura,
                                onCheckedChange = { alcance.launch { gestorPreferencias.establecerBloqueoCaptura(it) } },
                                colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = Color(0xFF7B68EE), uncheckedTrackColor = ColorTextoTenue)
                            )
                        }
                    )
                }
            }

            // ═══════════════════════════════════════
            // SECCIÓN 4: DATOS Y ALMACENAMIENTO
            // ═══════════════════════════════════════
            EncabezadoSeccion("Datos y Almacenamiento", Icons.Default.Storage, colorIcono = ColorAmarilloMarca)

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = ColorTarjeta)
            ) {
                Column {
                    FilaAjuste(
                        icono = Icons.Default.History,
                        titulo = "Borrar historial de canales",
                        descripcion = "Eliminar el historial de reproducción local",
                        colorIcono = ColorAmarilloMarca,
                        alHacerClick = { mostrarDialogoHistorial = true }
                    )
                    Divider(color = ColorDivisor, thickness = 0.5.dp, modifier = Modifier.padding(horizontal = 16.dp))
                    FilaAjuste(
                        icono = Icons.Default.CleaningServices,
                        titulo = "Limpiar caché de canales",
                        descripcion = "Recargar la lista completa desde el servidor",
                        colorIcono = ColorAmarilloMarca,
                        alHacerClick = { mostrarDialogoCache = true }
                    )
                }
            }

            // ═══════════════════════════════════════
            // SECCIÓN 5: SOPORTE
            // ═══════════════════════════════════════
            EncabezadoSeccion("Soporte", Icons.Default.HeadsetMic, colorIcono = ColorExito)

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = ColorTarjeta)
            ) {
                Column {
                    FilaAjuste(
                        icono = Icons.Default.Phone,
                        titulo = "Soporte por WhatsApp",
                        descripcion = "+51 982 497 670 — Soporte técnico",
                        colorIcono = ColorExito,
                        alHacerClick = {
                                manejadorUris.openUri("https://api.whatsapp.com/send?phone=51982497670&text=Hola%2C%20necesito%20soporte%20con%20la%20aplicaci%C3%B3n%20IPTV%20Fiber%20Z")
                        }
                    )
                    Divider(color = ColorDivisor, thickness = 0.5.dp, modifier = Modifier.padding(horizontal = 16.dp))
                    FilaAjuste(icono = Icons.Default.Description, titulo = "Términos y Condiciones", colorIcono = ColorExito, alHacerClick = { mostrarTerminos = true })
                    Divider(color = ColorDivisor, thickness = 0.5.dp, modifier = Modifier.padding(horizontal = 16.dp))
                    FilaAjuste(icono = Icons.Default.PrivacyTip, titulo = "Política de Privacidad", colorIcono = ColorExito, alHacerClick = { mostrarPrivacidad = true })
                }
            }

            // ═══════════════════════════════════════
            // SECCIÓN 6: ACERCA DE
            // ═══════════════════════════════════════
            EncabezadoSeccion("Acerca de", Icons.Default.Info, colorIcono = ColorTextoGris)

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = ColorTarjeta)
            ) {
                Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Box(
                            modifier = Modifier.size(40.dp).clip(RoundedCornerShape(10.dp))
                                .background(Brush.linearGradient(listOf(ColorAcentoAzul, ColorAcentoAzul.copy(alpha = 0.4f)))),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Tv, contentDescription = null, tint = Color.White, modifier = Modifier.size(22.dp))
                        }
                        Column {
                            Text("Fiber Z TV+", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                            Text("Versión 1.0.0", color = ColorTextoGris, fontSize = 12.sp)
                        }
                    }
                    Divider(color = ColorDivisor, thickness = 0.5.dp)
                    Text("Fiber Z Telecom S.A.C.", color = ColorTextoGris, fontSize = 13.sp)
                    Text("RUC: 20604599459", color = ColorTextoTenue, fontSize = 12.sp)
                    Text("Calle La Florida 775, Huancayo — Junín, Perú", color = ColorTextoTenue, fontSize = 12.sp)
                    Text("soporte@fiberztelecom.com", color = ColorAcentoAzul, fontSize = 12.sp)
                }
            }

            // ═══════════════════════════════════════
            // BOTÓN CERRAR SESIÓN
            // ═══════════════════════════════════════
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { mostrarConfirmacionCerrarSesion = true },
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = ColorError.copy(alpha = 0.08f)),
                border = androidx.compose.foundation.BorderStroke(1.dp, ColorError.copy(alpha = 0.25f))
            ) {
                Row(
                    modifier = Modifier.padding(16.dp).fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(Icons.Default.ExitToApp, contentDescription = null, tint = ColorError, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(10.dp))
                    Text("Cerrar sesión", color = ColorError, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}
