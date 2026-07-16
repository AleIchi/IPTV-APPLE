package com.iptv.fiber.tv

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.iptv.fiber.datos.api.ClienteApi
import com.iptv.fiber.datos.local.GestorPreferencias
import com.iptv.fiber.datos.local.base_datos.BaseDatosIPTV
import com.iptv.fiber.datos.repositorio.RepositorioAutenticacion
import com.iptv.fiber.datos.repositorio.RepositorioContenido
import com.iptv.fiber.interfaz.modelovista.ModeloVistaAutenticacion
import com.iptv.fiber.interfaz.modelovista.ModeloVistaContenido
import com.iptv.fiber.interfaz.tema.TemaIPTVFiber
import com.iptv.fiber.tv.pantallas.PantallaActivacionQR
import com.iptv.fiber.tv.pantallas.PantallaInicioSesionTV

/**
 * LA PUERTA DE ENTRADA (Televisores).
 * Al igual que ActividadPrincipal, esta es la primera pantalla que se lanza.
 * La diferencia es que el sistema Android TV (el launcher de la tele) sabe que tiene que abrir
 * ESTE archivo y no el otro porque en el AndroidManifest.xml tiene la categoría "LEANBACK_LAUNCHER".
 */
class ActividadTV : ComponentActivity() {

    // Los repositorios se declaran aquí porque esta Actividad los reparte a toda la app de TV
    private lateinit var repositorioAuth: RepositorioAutenticacion
    private lateinit var repositorioContenido: RepositorioContenido

    /**
     * Esta función se ejecuta al crearse la pantalla en la tele.
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // --- PREPARANDO EL MOTOR ---
        // Construimos las dependencias necesarias.
        val clienteApi = ClienteApi.obtener()
        val baseDatos = BaseDatosIPTV.obtenerBaseDatos()
        val gestorPreferencias = GestorPreferencias(this)
        
        repositorioAuth = RepositorioAutenticacion(clienteApi, gestorPreferencias)
        repositorioContenido =
                RepositorioContenido(
                        clienteApi,
                        repositorioAuth,
                        baseDatos.daoFavorito(),
                        baseDatos.daoSeguirViendo()
                )

        // --- JETPACK COMPOSE (Dibujando la UI de TV) ---
        setContent {
            val temaSeleccionado by gestorPreferencias.tema.collectAsStateWithLifecycle(initialValue = "clasico")

            TemaIPTVFiber(temaPreferido = temaSeleccionado) {
                Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = MaterialTheme.colorScheme.background
                ) {
                    val modeloVistaAuth: ModeloVistaAutenticacion = viewModel {
                        ModeloVistaAutenticacion(repositorioAuth)
                    }

                    // Observamos si hay sesión guardada
                    val autenticado by
                            modeloVistaAuth.estaAutenticado.collectAsStateWithLifecycle()
                    val verificandoSesion by modeloVistaAuth.verificandoSesion.collectAsStateWithLifecycle()

                    // Estado extra para la TV: ¿Mostrar código QR en lugar del formulario normal?
                    var mostrarPantallaQR by remember { mutableStateOf(false) }

                    // Decidimos qué pantalla mostrar
                    when {
                        // 1. Buscando clave guardada...
                        verificandoSesion ->
                                Box(
                                        modifier = Modifier.fillMaxSize(),
                                        contentAlignment = Alignment.Center
                                ) {
                                    CircularProgressIndicator(
                                            color = MaterialTheme.colorScheme.primary
                                    )
                                }
                        // 2. ¡Tiene sesión! Mostramos el menú principal de la tele (NavegacionTV)
                        autenticado ->
                                NavegacionTV(
                                        modeloVista =
                                                viewModel {
                                                    ModeloVistaContenido(repositorioContenido)
                                                },
                                        modeloVistaAuth = modeloVistaAuth,
                                        repositorioAuth = repositorioAuth
                                )
                        // 3. Si el usuario apretó "Iniciar sesión con QR"
                        mostrarPantallaQR ->
                                PantallaActivacionQR(
                                        modeloVista = modeloVistaAuth,
                                        gestorPreferencias = gestorPreferencias,
                                        alVolver = { mostrarPantallaQR = false } // Si cancela, vuelve a false
                                )
                        // 4. Si no hay sesión: Formulario de Login adaptado para control remoto
                        else ->
                                PantallaInicioSesionTV(
                                        modeloVista = modeloVistaAuth,
                                        alCambiarAPantallaQR = { mostrarPantallaQR = true }, // Cambia al paso 3
                                        alIniciarSesionExitosamente = {
                                            // La navegación se gestiona automáticamente
                                        }
                                )
                    }
                }
            }
        }
    }

    /**
     * Esta función se llama cuando el usuario sale completamente de la app (destruye la actividad).
     * Liberamos el "GestorReproductorCompartido" para que la tele no siga usando memoria en videos fantasmas.
     */
    override fun onDestroy() {
        super.onDestroy()
        com.iptv.fiber.tv.componentes.GestorReproductorCompartido.liberar()
    }
}
