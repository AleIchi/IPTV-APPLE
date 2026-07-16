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

/** 
 * LA PUERTA DE ENTRADA (Móvil).
 * ActividadPrincipal es la primera pantalla que se lanza en celulares.
 * Su único trabajo es decidir si mostrar la pantalla de Login o la lista de canales
 * dependiendo de si el usuario ya guardó sus datos antes.
 */
class ActividadPrincipal : ComponentActivity() {

    // Los repositorios se declaran aquí porque esta Actividad los reparte a toda la app
    private lateinit var repositorioAuth: RepositorioAutenticacion
    private lateinit var repositorioContenido: RepositorioContenido

    /**
     * Esta función se ejecuta al crearse la pantalla.
     */
    override fun onCreate(estadoGuardado: Bundle?) {
        super.onCreate(estadoGuardado)

        // --- DETECCIÓN DE TV ---
        // Si el usuario instaló la app en un Android TV, cerramos esta pantalla inmediatamente
        // y abrimos 'ActividadTV'. Así evitamos que la interfaz móvil se vea deformada en la tele.
        val uiModeManager = getSystemService(UI_MODE_SERVICE) as android.app.UiModeManager
        if (uiModeManager.currentModeType == android.content.res.Configuration.UI_MODE_TYPE_TELEVISION) {
            startActivity(android.content.Intent(this, com.iptv.fiber.tv.ActividadTV::class.java))
            @Suppress("DEPRECATION")
            overridePendingTransition(0, 0)
            finish() // Cierra la ActividadPrincipal (móvil)
            return
        }

        // --- PREPARANDO EL MOTOR ---
        // Construimos las piezas necesarias para que la app funcione: base de datos, API y repositorios.
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

        // --- JETPACK COMPOSE (Dibujando la UI) ---
        // Aquí empieza el código que "pinta" las cosas en pantalla.
        setContent {
            // Obtenemos qué tema quiere el usuario (clásico, oscuro, etc.) y reaccionamos en vivo.
            val temaSeleccionado by gestorPreferencias.tema.collectAsState(initial = "clasico")

            TemaIPTVFiber(temaPreferido = temaSeleccionado) {
                Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = MaterialTheme.colorScheme.background
                ) {
                    // Creamos el cerebro del login
                    val modeloVistaAuth: ModeloVistaAutenticacion = viewModel {
                        ModeloVistaAutenticacion(repositorioAuth)
                    }

                    // Observamos (collectAsState) si el usuario ya tiene sesión y si aún estamos buscando en memoria
                    val autenticado by
                            modeloVistaAuth.estaAutenticado.collectAsState(initial = false)
                    val verificandoSesion by modeloVistaAuth.verificandoSesion.collectAsState(initial = true)

                    // ¿Qué mostramos en pantalla?
                    when {
                        // 1. Si apenas abrimos la app y estamos buscando la clave guardada: Rueda girando.
                        verificandoSesion ->
                                Box(
                                        modifier = Modifier.fillMaxSize(),
                                        contentAlignment = Alignment.Center
                                ) {
                                    CircularProgressIndicator(
                                            color = MaterialTheme.colorScheme.primary
                                    )
                                }
                        // 2. Si encontramos la clave y es correcta: Mostramos el panel de canales.
                        autenticado ->
                                PantallaPrincipal(
                                        modeloVista =
                                                viewModel { // Creamos el cerebro de los canales aquí
                                                    ModeloVistaContenido(repositorioContenido)
                                                },
                                        modeloVistaAuth = modeloVistaAuth,
                                        repositorioAuth = repositorioAuth
                                )
                        // 3. Si no hay sesión o la clave está mal: Pantalla de escribir usuario y contraseña.
                        else ->
                                PantallaInicioSesion(
                                        modeloVista = modeloVistaAuth,
                                        alIniciarSesionExitosamente = {
                                            // ¡Magia! Si el login es exitoso, 'autenticado' cambia a true
                                            // y Compose automáticamente redibuja la pantalla saltando al paso 2.
                                        }
                                )
                    }
                }
            }
        }
    }
}
