package com.iptv.fiber.interfaz.principal

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.iptv.fiber.datos.api.ClienteApi
import com.iptv.fiber.datos.local.GestorPreferencias
import com.iptv.fiber.datos.local.base_datos.BaseDatosIPTV
import com.iptv.fiber.datos.repositorio.RepositorioAutenticacion
import com.iptv.fiber.datos.repositorio.RepositorioContenido
import com.iptv.fiber.interfaz.inicio_sesion.PantallaInicioSesion
import com.iptv.fiber.interfaz.modelovista.ModeloVistaAutenticacion
import com.iptv.fiber.interfaz.modelovista.ModeloVistaContenido
import com.iptv.fiber.interfaz.tema.TemaIPTVFiber

/** Actividad principal: punto de entrada de la aplicación. */
class ActividadPrincipal : ComponentActivity() {

    private lateinit var repositorioAuth: RepositorioAutenticacion
    private lateinit var repositorioContenido: RepositorioContenido

    override fun onCreate(estadoGuardado: Bundle?) {
        super.onCreate(estadoGuardado)

        // Redirección inteligente: Si detectamos que es una TV, lanzamos la ActividadTV
        val uiModeManager = getSystemService(UI_MODE_SERVICE) as android.app.UiModeManager
        if (uiModeManager.currentModeType == android.content.res.Configuration.UI_MODE_TYPE_TELEVISION) {
            startActivity(android.content.Intent(this, com.iptv.fiber.tv.ActividadTV::class.java))
            finish()
            return
        }

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
                                PantallaPrincipal(
                                        modeloVista =
                                                viewModel {
                                                    ModeloVistaContenido(repositorioContenido)
                                                },
                                        modeloVistaAuth = modeloVistaAuth,
                                        repositorioAuth = repositorioAuth
                                )
                        else ->
                                PantallaInicioSesion(
                                        modeloVista = modeloVistaAuth,
                                        alIniciarSesionExitosamente = {
                                            // La navegación se gestiona automáticamente por el
                                            // estado
                                        }
                                )
                    }
                }
            }
        }
    }
}
