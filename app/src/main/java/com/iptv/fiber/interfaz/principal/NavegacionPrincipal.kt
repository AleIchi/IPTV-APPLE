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

/** Composable raíz que contiene el sistema de navegación y la barra lateral. */
@Composable
fun PantallaPrincipal(
    modeloVista: ModeloVistaContenido,
    modeloVistaAuth: ModeloVistaAutenticacion,
    repositorioAuth: RepositorioAutenticacion
) {
    val controladorNavegacion = rememberNavController()
    var tabSeleccionado by rememberSaveable { mutableIntStateOf(0) }

    val elementosBarra = remember {
        listOf(
            Triple("Inicio",    Icons.Default.Home,     "inicio"),
            Triple("TV en Vivo", Icons.Default.LiveTv,  "envivo"),
            Triple("Favoritos", Icons.Default.Favorite, "favoritos"),
            Triple("Historial", Icons.Default.History,  "historial"),
            Triple("Ajustes",   Icons.Default.Settings, "ajustes")
        )
    }

    Column(modifier = Modifier.fillMaxSize().background(FondoPremium)) {

        Box(modifier = Modifier.weight(1f).fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
            NavHost(navController = controladorNavegacion, startDestination = "inicio") {
                composable("inicio") {
                    InicioDashboard(
                        alNavegar = { ruta ->
                            val indice = elementosBarra.indexOfFirst { it.third == ruta }
                            if (indice != -1) { tabSeleccionado = indice; controladorNavegacion.navigate(ruta) }
                        },
                        modeloVistaAuth = modeloVistaAuth,
                        modeloVistaContenido = modeloVista,
                        repositorioAuth = repositorioAuth
                    )
                }
                composable("envivo") {
                    PantallaTvEnVivo(
                        modeloVista = modeloVista,
                        repositorioAuth = repositorioAuth,
                        alHacerBack = { controladorNavegacion.popBackStack() }
                    )
                }
                composable("favoritos") {
                    PantallaFavoritos(modeloVista = modeloVista, repositorioAuth = repositorioAuth)
                }
                composable("historial") {
                    PantallaHistorial(modeloVista = modeloVista, repositorioAuth = repositorioAuth)
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
                tabSeleccionado = indice
                controladorNavegacion.navigate(elementosBarra[indice].third) {
                    popUpTo(controladorNavegacion.graph.startDestinationId)
                    launchSingleTop = true
                }
            },
            elementos = elementosBarra,
            modificador = Modifier.fillMaxWidth()
        )
    }
}
