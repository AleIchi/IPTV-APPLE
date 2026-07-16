package com.iptv.fiber

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.iptv.fiber.interfaz.tema.TemaIPTVFiber

/**
 * EL CEREBRO MULTIPLATAFORMA (Reemplazo de ActividadPrincipal).
 * Esta función es el punto de entrada para iOS y Android.
 * Aquí configuramos el sistema de navegación (NavHost) en lugar de usar Intents.
 */
@Composable
fun App() {
    // Nota: El ViewModel o la lectura de estado real debería pasarse aquí
    // Para simplificar la migración estructural, iniciamos el tema por defecto.
    TemaIPTVFiber(temaPreferido = "clasico") {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            val navController = rememberNavController()

            // SISTEMA DE NAVEGACIÓN (Rutas)
            NavHost(navController = navController, startDestination = "splash") {
                
                composable("splash") {
                    // Aquí iría la lógica de verificar si el usuario tiene sesión guardada
                    // simulamos que verificó y manda a login:
                    // LaunchedEffect(Unit) { navController.navigate("login") }
                }

                composable("login") {
                    // PantallaInicioSesion(
                    //     modeloVista = ..., 
                    //     alIniciarSesionExitosamente = { navController.navigate("principal") }
                    // )
                }

                composable("principal") {
                    // PantallaPrincipal(
                    //     modeloVista = ...,
                    //     modeloVistaAuth = ...,
                    //     repositorioAuth = ...,
                    //     navController = navController // Pasamos el controlador para que pueda navegar al reproductor
                    // )
                }
                
                composable("reproductor/{streamId}") { backStackEntry ->
                    val streamId = backStackEntry.arguments?.getString("streamId")
                    com.iptv.fiber.interfaz.reproductor.VideoPlayerScreen(streamId = streamId)
                }
            }
        }
    }
}
