package com.iptv.fiber

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.iptv.fiber.datos.api.ClienteApi
import com.iptv.fiber.datos.local.GestorPreferencias
import com.iptv.fiber.datos.local.base_datos.getDatabaseBuilder
import com.iptv.fiber.datos.repositorio.RepositorioAutenticacion
import com.iptv.fiber.datos.repositorio.RepositorioContenido
import com.iptv.fiber.interfaz.inicio_sesion.PantallaInicioSesion
import com.iptv.fiber.interfaz.modelovista.ModeloVistaAutenticacion
import com.iptv.fiber.interfaz.modelovista.ModeloVistaContenido
import com.iptv.fiber.interfaz.principal.PantallaPrincipal
import com.iptv.fiber.interfaz.reproductor.VideoPlayerScreen
import com.iptv.fiber.interfaz.tema.TemaIPTVFiber
import kotlinx.coroutines.flow.first

/**
 * EL CEREBRO MULTIPLATAFORMA (Reemplazo de ActividadPrincipal).
 * Esta función es el punto de entrada para iOS y Android.
 * Aquí configuramos el sistema de navegación (NavHost) en lugar de usar Intents.
 */
@Composable
fun App() {
    // Instanciar dependencias manualmente para KMP (sin usar Koin u Hilt para mantener simplicidad)
    val gestorPreferencias = remember { GestorPreferencias() }
    val clienteApi = remember { ClienteApi() }
    val repositorioAuth = remember { RepositorioAutenticacion(clienteApi, gestorPreferencias) }
    
    val daoFavorito = remember { getDatabaseBuilder().build().daoFavorito() }
    val daoSeguirViendo = remember { getDatabaseBuilder().build().daoSeguirViendo() }
    val repositorioContenido = remember { RepositorioContenido(clienteApi, repositorioAuth, daoFavorito, daoSeguirViendo) }

    val modeloVistaAuth = remember { ModeloVistaAutenticacion(repositorioAuth) }
    val modeloVistaContenido = remember { ModeloVistaContenido(repositorioContenido) }

    TemaIPTVFiber(temaPreferido = "clasico") {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            val navController = rememberNavController()

            // SISTEMA DE NAVEGACIÓN (Rutas)
            NavHost(navController = navController, startDestination = "splash") {
                
                composable("splash") {
                    LaunchedEffect(Unit) {
                        val sessionGuardada = gestorPreferencias.urlServidor.first().isNotEmpty()
                        if (sessionGuardada) {
                            navController.navigate("principal") {
                                popUpTo("splash") { inclusive = true }
                            }
                        } else {
                            navController.navigate("login") {
                                popUpTo("splash") { inclusive = true }
                            }
                        }
                    }
                }

                composable("login") {
                    PantallaInicioSesion(
                        modeloVista = modeloVistaAuth, 
                        alIniciarSesionExitosamente = { 
                            navController.navigate("principal") {
                                popUpTo("login") { inclusive = true }
                            }
                        }
                    )
                }

                composable("principal") {
                    PantallaPrincipal(
                        modeloVista = modeloVistaContenido,
                        modeloVistaAuth = modeloVistaAuth,
                        repositorioAuth = repositorioAuth,
                        alNavegarReproductor = { streamId -> 
                            navController.navigate("reproductor/$streamId") 
                        }
                    )
                }
                
                composable("reproductor/{streamId}") { backStackEntry ->
                    val streamId = backStackEntry.arguments?.getString("streamId")
                    VideoPlayerScreen(streamId = streamId)
                }
            }
        }
    }
}
