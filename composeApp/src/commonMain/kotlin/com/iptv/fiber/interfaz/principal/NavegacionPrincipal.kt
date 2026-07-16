package com.iptv.fiber.interfaz.principal

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.iptv.fiber.datos.repositorio.RepositorioAutenticacion
import com.iptv.fiber.interfaz.ajustes.PantallaAjustes
import com.iptv.fiber.interfaz.componentes.BarraLateralModerna
import com.iptv.fiber.interfaz.modelovista.ModeloVistaAutenticacion
import com.iptv.fiber.interfaz.modelovista.ModeloVistaContenido
import com.iptv.fiber.interfaz.tema.FondoPremium

/**
 * EL ESQUELETO DE LA APP MÓVIL (Navegación Principal).
 * Imagina que esta pantalla es el marco de un cuadro. La barra inferior siempre se dibuja,
 * pero el "lienzo" central cambia dependiendo del botón que toques (Inicio, TV, Ajustes).
 * El encargado de cambiar el lienzo se llama "NavHost".
 */
@Composable
fun PantallaPrincipal(
    modeloVista: ModeloVistaContenido,
    modeloVistaAuth: ModeloVistaAutenticacion,
    repositorioAuth: RepositorioAutenticacion,
    alNavegarReproductor: (String) -> Unit
) {
    val controladorNavegacion = rememberNavController()
    var tabSeleccionado by rememberSaveable { mutableIntStateOf(0) }

    val gestorPrefs = remember { com.iptv.fiber.datos.local.GestorPreferencias() }
    val bloqueoActivo by gestorPrefs.controlParentalActivo.collectAsState(initial = false)
    val pinCorrecto by gestorPrefs.pinParental.collectAsState(initial = "")
    var mostrarPinAjustes by remember { mutableStateOf(false) }
    var accionAlVerificar by remember { mutableStateOf<(() -> Unit)?>(null) }

    /**
     * GUARDIÁN DE PANTALLAS (Control Parental):
     * Si el usuario toca el botón de "Ajustes", esta función verifica si el bloqueo está activo.
     * Si está activo, frena el cambio de pantalla y levanta el diálogo para pedir el PIN.
     * Si el PIN es correcto, recién ahí lo deja pasar ('accion()').
     */
    fun protegerConPin(esAjustes: Boolean, accion: () -> Unit) {
        if (esAjustes && bloqueoActivo && pinCorrecto.isNotEmpty()) {
            accionAlVerificar = accion
            mostrarPinAjustes = true
        } else {
            accion()
        }
    }

    val elementosBarra = remember {
        listOf(
            Triple("Inicio",    Icons.Default.Home,     "inicio"),
            Triple("TV en vivo", Icons.Default.LiveTv,  "envivo"),
            Triple("Favoritos", Icons.Default.Favorite, "favoritos"),
            Triple("Historial", Icons.Default.History,  "historial"),
            Triple("Ajustes",   Icons.Default.Settings, "ajustes")
        )
    }

    Column(modifier = Modifier.fillMaxSize().background(FondoPremium)) {

        Box(modifier = Modifier.weight(1f).fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
            // EL MOTOR DE NAVEGACIÓN:
            // "NavHost" sabe qué pantalla mostrar basándose en una palabra clave o "ruta" ("inicio", "envivo", etc).
            NavHost(navController = controladorNavegacion, startDestination = "inicio") {
                composable("inicio") {
                    InicioPanelPrincipal(
                        alNavegar = { ruta ->
                            val indice = elementosBarra.indexOfFirst { it.third == ruta }
                            if (indice != -1) {
                                protegerConPin(ruta == "ajustes") {
                                    tabSeleccionado = indice
                                    controladorNavegacion.navigate(ruta)
                                }
                            }
                        },
                        modeloVistaAuth = modeloVistaAuth,
                        modeloVistaContenido = modeloVista,
                        repositorioAuth = repositorioAuth,
                        alNavegarReproductor = alNavegarReproductor
                    )
                }
                composable("envivo") {
                    PantallaTvEnVivo(
                        modeloVista = modeloVista,
                        repositorioAuth = repositorioAuth,
                        alHacerBack = { controladorNavegacion.popBackStack() },
                        alNavegarReproductor = alNavegarReproductor
                    )
                }
                composable("favoritos") {
                    PantallaFavoritos(
                        modeloVista = modeloVista, 
                        repositorioAuth = repositorioAuth,
                        alNavegarReproductor = alNavegarReproductor
                    )
                }
                composable("historial") {
                    PantallaHistorial(
                        modeloVista = modeloVista,
                        alNavegarReproductor = alNavegarReproductor
                    )
                }
                composable("ajustes") {
                    PantallaAjustes(
                        modeloVista = modeloVista,
                        modeloVistaAuth = modeloVistaAuth,
                        alCerrarSesion = { modeloVistaAuth.cerrarSesion() }
                    )
                }
            }
        }

        BarraLateralModerna(
            tabSeleccionado = tabSeleccionado,
            alSeleccionarTab = { indice ->
                protegerConPin(elementosBarra[indice].third == "ajustes") {
                    tabSeleccionado = indice
                    controladorNavegacion.navigate(elementosBarra[indice].third) {
                        popUpTo(controladorNavegacion.graph.startDestinationId)
                        launchSingleTop = true
                    }
                }
            },
            elementos = elementosBarra,
            modificador = Modifier.fillMaxWidth()
        )

        if (mostrarPinAjustes) {
            com.iptv.fiber.interfaz.ajustes.DialogoPin(
                titulo = "Acceso Protegido",
                descripcion = "Ingresa el PIN de seguridad para acceder a los Ajustes",
                pinCorrecto = pinCorrecto,
                alConfirmar = { pin ->
                    if (pin == pinCorrecto) {
                        accionAlVerificar?.invoke()
                    }
                    mostrarPinAjustes = false
                    accionAlVerificar = null
                },
                alCancelar = {
                    mostrarPinAjustes = false
                    accionAlVerificar = null
                }
            )
        }
    }
}
