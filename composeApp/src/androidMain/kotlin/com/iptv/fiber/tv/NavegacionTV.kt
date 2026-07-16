package com.iptv.fiber.tv

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.focusGroup
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.graphics.Color
import com.iptv.fiber.datos.repositorio.RepositorioAutenticacion
import com.iptv.fiber.interfaz.modelovista.ModeloVistaAutenticacion
import com.iptv.fiber.interfaz.modelovista.ModeloVistaContenido
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.focusProperties
import com.iptv.fiber.tv.componentes.MenuLateralTV
import com.iptv.fiber.tv.pantallas.InicioTV
import com.iptv.fiber.tv.pantallas.TvEnVivoTV
import com.iptv.fiber.tv.pantallas.FavoritosTV
import com.iptv.fiber.tv.pantallas.HistorialTV
import com.iptv.fiber.tv.pantallas.AjustesTV

import androidx.compose.ui.draw.clipToBounds
import com.iptv.fiber.tv.componentes.DialogoConfirmacionTV
import com.iptv.fiber.tv.dialogos.DialogoPinTV
import androidx.compose.runtime.collectAsState
import androidx.lifecycle.compose.collectAsStateWithLifecycle

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject

/**
 * EL ESQUELETO DE LA TV (Navegación TV).
 * Similar a NavegacionPrincipal para móviles, pero diseñado para el control remoto.
 * Aquí manejamos el "Menú Lateral" (la barra a la izquierda que se abre y se cierra) y
 * el área principal de contenido a la derecha.
 *
 * TRUCO DE FOCO:
 * Le decimos a la app: "Si estás en el contenido y presionas IZQUIERDA en tu control,
 * manda el foco al Menú Lateral".
 */
@kotlin.OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class, androidx.compose.ui.ExperimentalComposeUiApi::class)
@Composable
fun NavegacionTV(
    modeloVista: ModeloVistaContenido,
    modeloVistaAuth: ModeloVistaAutenticacion,
    repositorioAuth: RepositorioAutenticacion
) {
    val contexto = androidx.compose.ui.platform.LocalContext.current
    val arrancarEnVivo = remember {
        val prefs = contexto.getSharedPreferences("iptv_preferencias", android.content.Context.MODE_PRIVATE)
        prefs.getBoolean("reproduccion_automatica_inicio", false)
    }

    var rutaActual by remember { mutableStateOf(if (arrancarEnVivo) "tv_vivo" else "inicio") }
    var mostrarConfirmacionSalir by remember { mutableStateOf(false) }
    val requeridorFocoContenido = remember { FocusRequester() }
    val requeridorFocoMenu = remember { FocusRequester() }

    val gestorPreferencias = remember { com.iptv.fiber.datos.local.GestorPreferencias(contexto) }
    val controlParentalActivo by gestorPreferencias.controlParentalActivo.collectAsStateWithLifecycle(initialValue = false)
    val pinParental by gestorPreferencias.pinParental.collectAsStateWithLifecycle(initialValue = "")
    var mostrarPinAjustes by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        modeloVista.iniciarObservacionDatosUsuario()
    }

    // Al cambiar de sección, dar foco al contenido después de un pequeño delay
    // para que el menú pueda procesar el click antes de perder el foco
    LaunchedEffect(rutaActual) {
        kotlinx.coroutines.delay(50) // Reducido de 150ms a 50ms para respuesta inmediata
        try {
            requeridorFocoContenido.requestFocus()
        } catch (_: Exception) {}
    }

    // Polling background para detectar cierre de sesión remoto
    var macAddress by remember { mutableStateOf("") }
    LaunchedEffect(Unit) {
        macAddress = gestorPreferencias.obtenerOGenerarMacVirtual()
    }
    // Cliente HTTP mínimo para el polling de Firestore: 1 hilo, 1 conexión persistente.
    // OkHttpClient() por defecto crea un pool de 64 hilos innecesario para un solo endpoint.
    val clienteFirestore = remember {
        OkHttpClient.Builder()
            .dispatcher(okhttp3.Dispatcher().apply { maxRequests = 1; maxRequestsPerHost = 1 })
            .connectionPool(okhttp3.ConnectionPool(1, 60, java.util.concurrent.TimeUnit.SECONDS))
            .connectTimeout(10, java.util.concurrent.TimeUnit.SECONDS)
            .readTimeout(15, java.util.concurrent.TimeUnit.SECONDS)
            .build()
    }
    // VIGILANTE DE SEGURIDAD (Polling de Firestore)
    // El administrador puede cerrar tu sesión desde internet. Como la app de TV no usa notificaciones push complejas,
    // usamos un "Polling": cada 30 segundos le preguntamos al servidor "¿Me borraste?".
    // Si la respuesta es sí ("LOGOUT"), se cierra la app automáticamente.
    LaunchedEffect(macAddress) {
        if (macAddress.isEmpty() || macAddress == "Cargando...") return@LaunchedEffect
        val urlRest = "https://firestore.googleapis.com/v1/projects/web-iptv-50785/databases/(default)/documents/devices/$macAddress"
        while (true) {
            try {
                val peticion = Request.Builder().url(urlRest).build()
                withContext(Dispatchers.IO) {
                    clienteFirestore.newCall(peticion).execute().use { respuesta ->
                        if (respuesta.isSuccessful) {
                            val cuerpo = respuesta.body?.string()
                            if (!cuerpo.isNullOrEmpty()) {
                                val json = JSONObject(cuerpo)
                                val fields = json.optJSONObject("fields")
                                val mode = fields?.optJSONObject("mode")?.optString("stringValue")

                                if (mode == "LOGOUT") {
                                    try {
                                        val peticionBorrado = Request.Builder().url(urlRest).delete().build()
                                        clienteFirestore.newCall(peticionBorrado).execute().close()
                                    } catch (e: Exception) {}

                                    withContext(Dispatchers.Main) {
                                        // Limpiar canales antes de cerrar sesión para que no aparezca
                                        // la lista anterior al iniciar con una nueva cuenta
                                        modeloVista.reiniciarEstado()
                                        modeloVistaAuth.cerrarSesion()
                                    }
                                }
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
            kotlinx.coroutines.delay(30000) // Esperar 30 segundos antes del siguiente chequeo
        }
    }

    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF0A0A14))
        ) {
            // Área de Contenido Principal
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(start = 76.dp)
                    .clipToBounds()
                    .focusGroup()
                    .focusProperties {
                        exit = { direction ->
                            if (direction == FocusDirection.Left) {
                                requeridorFocoMenu
                            } else {
                                FocusRequester.Default
                            }
                        }
                    }
                    .focusRequester(requeridorFocoContenido)
            ) {
                when (rutaActual) {
                    "inicio" -> InicioTV(
                        modeloVista = modeloVista,
                        repositorioAuth = repositorioAuth,
                        requeridorFocoContenido = requeridorFocoContenido
                    )
                    "tv_vivo" -> TvEnVivoTV(
                        modeloVista = modeloVista,
                        repositorioAuth = repositorioAuth,
                        requeridorFocoContenido = requeridorFocoContenido
                    )
                    "favoritos" -> FavoritosTV(
                        modeloVista = modeloVista,
                        repositorioAuth = repositorioAuth,
                        requeridorFocoContenido = requeridorFocoContenido
                    )
                    "historial" -> HistorialTV(
                        modeloVista = modeloVista,
                        repositorioAuth = repositorioAuth,
                        requeridorFocoContenido = requeridorFocoContenido
                    )
                    "ajustes" -> AjustesTV(
                        modeloVista = modeloVista,
                        modeloVistaAuth = modeloVistaAuth,
                        requeridorFocoContenido = requeridorFocoContenido
                    )
                    else -> Text(
                        "Pantalla no encontrada",
                        color = Color.White,
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
            }

            // EL MENÚ LATERAL (Overlay):
            // Dibuja el menú de navegación encima del contenido. Al estar separado,
            // evita tener que recalcular el tamaño de toda la pantalla cuando el menú se expande.
            MenuLateralTV(
                rutaActual = rutaActual,
                requeridorFocoContenido = requeridorFocoContenido,
                requeridorFocoMenu = requeridorFocoMenu,
                alNavegar = { nuevaRuta ->
                    if (nuevaRuta == "salir") {
                        mostrarConfirmacionSalir = true
                    } else if (nuevaRuta == "ajustes" && controlParentalActivo && pinParental.isNotEmpty()) {
                        mostrarPinAjustes = true
                    } else {
                        rutaActual = nuevaRuta
                    }
                }
            )
        }

        // Overlay de Diálogo de Confirmación de Salida
        if (mostrarConfirmacionSalir) {
            DialogoConfirmacionTV(
                alConfirmar = {
                    mostrarConfirmacionSalir = false
                    // Limpiar canales antes de cerrar sesión para que no aparezca
                    // la lista anterior al iniciar con una nueva cuenta
                    modeloVista.reiniciarEstado()
                    modeloVistaAuth.cerrarSesion()
                },
                alCancelar = {
                    mostrarConfirmacionSalir = false
                }
            )
        }

        // Overlay de Diálogo de PIN para Ajustes
        if (mostrarPinAjustes) {
            DialogoPinTV(
                titulo = "Acceso Protegido",
                descripcion = "Ingresa el PIN de seguridad para acceder a Ajustes",
                pinCorrecto = pinParental,
                alConfirmar = { pin ->
                    if (pin == pinParental) {
                        rutaActual = "ajustes"
                    }
                    mostrarPinAjustes = false
                },
                alCancelar = {
                    mostrarPinAjustes = false
                }
            )
        }
    }
}
