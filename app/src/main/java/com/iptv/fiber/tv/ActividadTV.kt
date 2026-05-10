package com.iptv.fiber.tv

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.viewmodel.compose.viewModel
import com.iptv.fiber.datos.api.ClienteApi
import com.iptv.fiber.datos.local.GestorPreferencias
import com.iptv.fiber.datos.local.base_datos.BaseDatosIPTV
import com.iptv.fiber.datos.repositorio.RepositorioAutenticacion
import com.iptv.fiber.datos.repositorio.RepositorioContenido
import com.iptv.fiber.interfaz.inicio_sesion.PantallaInicioSesion
import com.iptv.fiber.tv.pantallas.PantallaInicioSesionTV
import com.iptv.fiber.interfaz.modelovista.ModeloVistaAutenticacion
import com.iptv.fiber.interfaz.modelovista.ModeloVistaContenido
import com.iptv.fiber.interfaz.tema.TemaIPTVFiber

/**
 * Punto de entrada principal para dispositivos Android TV / Google TV.
 * Esta actividad se lanza automáticamente gracias a la categoría LEANBACK_LAUNCHER.
 */
class ActividadTV : ComponentActivity() {

    private lateinit var repositorioAuth: RepositorioAutenticacion
    private lateinit var repositorioContenido: RepositorioContenido

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val clienteApi = ClienteApi()
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

        setContent {
            TemaIPTVFiber {
                Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = MaterialTheme.colorScheme.background
                ) {
                    val modeloVistaAuth: ModeloVistaAutenticacion = viewModel {
                        ModeloVistaAutenticacion(repositorioAuth)
                    }

                    val autenticado by
                            modeloVistaAuth.estaAutenticado.collectAsState(initial = false)
                    val verificandoSesion by modeloVistaAuth.verificandoSesion.collectAsState()

                    when {
                        verificandoSesion ->
                                Box(
                                        modifier = Modifier.fillMaxSize(),
                                        contentAlignment = Alignment.Center
                                ) {
                                    CircularProgressIndicator(
                                            color = MaterialTheme.colorScheme.primary
                                    )
                                }
                        autenticado ->
                                NavegacionTV(
                                        modeloVista =
                                                viewModel {
                                                    ModeloVistaContenido(repositorioContenido)
                                                },
                                        modeloVistaAuth = modeloVistaAuth,
                                        repositorioAuth = repositorioAuth
                                )
                        else ->
                                PantallaInicioSesionTV(
                                        modeloVista = modeloVistaAuth,
                                        alIniciarSesionExitosamente = {
                                            // La navegación se gestiona automáticamente por el estado
                                        }
                                )
                    }
                }
            }
        }
    }
}
