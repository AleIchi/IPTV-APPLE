package com.iptv.fiber.interfaz.modelovista

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.iptv.fiber.datos.modelo.InfoUsuario
import com.iptv.fiber.datos.repositorio.RepositorioAutenticacion
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/** ViewModel que gestiona el estado de autenticación del usuario. */
class ModeloVistaAutenticacion(private val repositorioAuth: RepositorioAutenticacion) : ViewModel() {

    private val _estadoInterfaz = MutableStateFlow<EstadoAutenticacion>(EstadoAutenticacion.Inactivo)
    val estadoInterfaz: StateFlow<EstadoAutenticacion> = _estadoInterfaz.asStateFlow()

    val estaAutenticado = repositorioAuth.estaAutenticado
    val servidorActual = repositorioAuth.servidorActual

    private val _verificandoSesion = MutableStateFlow(true)
    val verificandoSesion: StateFlow<Boolean> = _verificandoSesion.asStateFlow()

    init {
        viewModelScope.launch {
            repositorioAuth.intentarAutoInicioSesion()
            _verificandoSesion.value = false
        }
    }

    fun iniciarSesion(urlServidor: String, usuario: String, contrasena: String) {
        viewModelScope.launch {
            _estadoInterfaz.value = EstadoAutenticacion.Cargando
            val resultado = repositorioAuth.autenticar(urlServidor, usuario, contrasena)
            _estadoInterfaz.value = if (resultado.isSuccess) {
                EstadoAutenticacion.Exito(resultado.getOrNull()!!)
            } else {
                EstadoAutenticacion.Error(resultado.exceptionOrNull()?.message ?: "Error al iniciar sesión")
            }
        }
    }

    fun iniciarSesionConIdDispositivo(urlServidor: String, idDispositivo: String) {
        viewModelScope.launch {
            _estadoInterfaz.value = EstadoAutenticacion.Cargando
            val resultado = repositorioAuth.autenticar(urlServidor, idDispositivo, "")
            _estadoInterfaz.value = if (resultado.isSuccess) {
                EstadoAutenticacion.Exito(resultado.getOrNull()!!)
            } else {
                EstadoAutenticacion.Error(resultado.exceptionOrNull()?.message ?: "Error al iniciar sesión con ID de dispositivo")
            }
        }
    }

    fun iniciarSesionConM3U(urlLista: String) {
        viewModelScope.launch {
            _estadoInterfaz.value = EstadoAutenticacion.Cargando
            val resultado = repositorioAuth.autenticarM3U(urlLista)
            _estadoInterfaz.value = if (resultado.isSuccess) {
                EstadoAutenticacion.Exito(resultado.getOrNull()!!)
            } else {
                EstadoAutenticacion.Error(resultado.exceptionOrNull()?.message ?: "Error al cargar la lista")
            }
        }
    }

    fun cerrarSesion() {
        repositorioAuth.cerrarSesion()
        _estadoInterfaz.value = EstadoAutenticacion.Inactivo
    }

    /** Estados posibles de la autenticación. */
    sealed class EstadoAutenticacion {
        object Inactivo  : EstadoAutenticacion()
        object Cargando  : EstadoAutenticacion()
        data class Exito(val infoUsuario: InfoUsuario) : EstadoAutenticacion()
        data class Error(val mensaje: String)       : EstadoAutenticacion()
    }
}
