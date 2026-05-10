package com.iptv.fiber.tv

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.iptv.fiber.datos.repositorio.RepositorioAutenticacion
import com.iptv.fiber.interfaz.modelovista.ModeloVistaAutenticacion
import com.iptv.fiber.interfaz.modelovista.ModeloVistaContenido
import com.iptv.fiber.interfaz.tema.AcentoPremium
import com.iptv.fiber.tv.componentes.MenuLateralTV
import com.iptv.fiber.tv.pantallas.InicioTV
import com.iptv.fiber.tv.pantallas.TvEnVivoTV
import com.iptv.fiber.tv.pantallas.FavoritosTV
import com.iptv.fiber.tv.pantallas.HistorialTV
import com.iptv.fiber.tv.pantallas.AjustesTV

import com.iptv.fiber.tv.componentes.DialogoConfirmacionTV

/**
 * Gestiona la estructura principal de la interfaz de TV:
 * Un menú lateral siempre presente (colapsable) y un área de contenido a la derecha.
 */
@Composable
fun NavegacionTV(
    modeloVista: ModeloVistaContenido,
    modeloVistaAuth: ModeloVistaAutenticacion,
    repositorioAuth: RepositorioAutenticacion
) {
    var rutaActual by remember { mutableStateOf("inicio") }
    var mostrarConfirmacionSalir by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF0A0A14))
        ) {
            // Menú Lateral
            MenuLateralTV(
                rutaActual = rutaActual,
                alNavegar = { nuevaRuta ->
                    if (nuevaRuta == "salir") {
                        mostrarConfirmacionSalir = true
                    } else {
                        rutaActual = nuevaRuta
                    }
                }
            )

            // Área de Contenido Principal
            Box(
                modifier = Modifier.fillMaxSize()
            ) {
                when (rutaActual) {
                    "inicio" -> InicioTV(
                        modeloVista = modeloVista,
                        repositorioAuth = repositorioAuth
                    )
                    "tv_vivo" -> TvEnVivoTV(
                        modeloVista = modeloVista,
                        repositorioAuth = repositorioAuth
                    )
                    "favoritos" -> FavoritosTV(
                        modeloVista = modeloVista,
                        repositorioAuth = repositorioAuth
                    )
                    "historial" -> HistorialTV(
                        modeloVista = modeloVista,
                        repositorioAuth = repositorioAuth
                    )
                    "ajustes" -> AjustesTV(
                        modeloVista = modeloVista,
                        modeloVistaAuth = modeloVistaAuth,
                        repositorioAuth = repositorioAuth
                    )
                    else -> Text(
                        "Pantalla no encontrada",
                        color = Color.White,
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
            }
        }

        // Overlay de Diálogo de Confirmación de Salida
        if (mostrarConfirmacionSalir) {
            DialogoConfirmacionTV(
                alConfirmar = {
                    mostrarConfirmacionSalir = false
                    modeloVistaAuth.cerrarSesion()
                },
                alCancelar = {
                    mostrarConfirmacionSalir = false
                }
            )
        }
    }
}
