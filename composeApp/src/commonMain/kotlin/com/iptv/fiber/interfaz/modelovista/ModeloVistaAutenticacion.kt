package com.iptv.fiber.interfaz.modelovista

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.iptv.fiber.datos.modelo.InfoUsuario
import com.iptv.fiber.datos.repositorio.RepositorioAutenticacion
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/** 
 * EL CEREBRO DE LA AUTENTICACIÓN.
 * ModeloVistaAutenticacion es la capa intermedia entre la Pantalla (interfaz) y la base de datos o internet (Repositorio).
 * La pantalla le avisa a este ViewModel cuando el usuario da clic en "Iniciar Sesión",
 * y este ViewModel se encarga de hablar con el servidor usando el repositorio.
 */
class ModeloVistaAutenticacion(private val repositorioAuth: RepositorioAutenticacion) : ViewModel() {

    // _estadoInterfaz guarda el estado actual (ej: Cargando, Error, Éxito).
    // Es privado para que la pantalla no lo modifique a la fuerza.
    private val _estadoInterfaz = MutableStateFlow<EstadoAutenticacion>(EstadoAutenticacion.Inactivo)
    // estadoInterfaz es público y de solo lectura. La pantalla "observa" esto para saber qué dibujar.
    val estadoInterfaz: StateFlow<EstadoAutenticacion> = _estadoInterfaz.asStateFlow()

    // Observamos si el usuario ya está autenticado desde antes (si cerró y abrió la app)
    val estaAutenticado = repositorioAuth.estaAutenticado
    val servidorActual = repositorioAuth.servidorActual

    // Bandera para mostrar una pantalla de carga blanca al arrancar mientras vemos si hay sesión guardada
    private val _verificandoSesion = MutableStateFlow(true)
    val verificandoSesion: StateFlow<Boolean> = _verificandoSesion.asStateFlow()

    init {
        // Esto se ejecuta apenas se crea este ViewModel (al abrir la app).
        // Revisamos en la memoria interna si el usuario ya tenía una sesión activa.
        viewModelScope.launch {
            repositorioAuth.intentarAutoInicioSesion()
            _verificandoSesion.value = false // Terminamos de verificar
        }
    }

    /** 
     * Esta función evalúa qué respondió el servidor. 
     * Si fue un éxito, cambia el estado a 'Exito', sino a 'Error'. 
     * La pantalla reaccionará automáticamente a este cambio.
     */
    private fun manejarResultado(resultado: Result<InfoUsuario>, mensajeError: String = "Error al iniciar sesión") {
        _estadoInterfaz.value = if (resultado.isSuccess) {
            EstadoAutenticacion.Exito(resultado.getOrNull()!!)
        } else {
            EstadoAutenticacion.Error(resultado.exceptionOrNull()?.message ?: mensajeError)
        }
    }

    /** 
     * Inicia sesión con credenciales Xtream Codes (Usuario y contraseña). 
     * Se usa en la Pantalla de Login móvil y TV.
     */
    fun iniciarSesion(urlServidor: String, usuario: String, contrasena: String) {
        viewModelScope.launch { // Se ejecuta en segundo plano para no congelar la pantalla
            _estadoInterfaz.value = EstadoAutenticacion.Cargando // Le decimos a la pantalla que muestre la rueda de carga
            // Mandamos a llamar a la API y manejamos el resultado
            manejarResultado(repositorioAuth.autenticar(urlServidor, usuario, contrasena))
        }
    }

    /** Inicia sesión usando el ID de dispositivo (MAC) como credencial en lugar de usuario/contraseña. */
    fun iniciarSesionConIdDispositivo(urlServidor: String, idDispositivo: String) {
        viewModelScope.launch {
            _estadoInterfaz.value = EstadoAutenticacion.Cargando
            manejarResultado(
                repositorioAuth.autenticar(urlServidor, idDispositivo, ""),
                "Error al iniciar sesión con ID de dispositivo"
            )
        }
    }

    /** Inicia sesión importando una lista M3U desde una URL o archivo. */
    fun iniciarSesionConM3U(urlLista: String) {
        viewModelScope.launch {
            _estadoInterfaz.value = EstadoAutenticacion.Cargando
            manejarResultado(repositorioAuth.autenticarM3U(urlLista), "Error al cargar la lista")
        }
    }

    /**
     * Cierra sesión: Borra los datos del repositorio local y apaga la app.
     */
    fun cerrarSesion() {
        repositorioAuth.cerrarSesion()
        _estadoInterfaz.value = EstadoAutenticacion.Inactivo
    }

    /**
     * Limpia el mensaje de error (ej: el usuario se equivocó de clave, vio el error, 
     * y vuelve a intentar. Hay que borrar el mensaje viejo).
     */
    fun limpiarError() {
        if (_estadoInterfaz.value is EstadoAutenticacion.Error) {
            _estadoInterfaz.value = EstadoAutenticacion.Inactivo
        }
    }

    /** 
     * Esto es un "Enum" con superpoderes. Define todos los estados posibles en los que puede estar la pantalla de login.
     * La pantalla usa un `when (estado)` para decidir si dibuja cajas de texto, una ruedita de carga o un texto rojo de error.
     */
    sealed class EstadoAutenticacion {
        object Inactivo  : EstadoAutenticacion() // Viendo el formulario de usuario/clave normal
        object Cargando  : EstadoAutenticacion() // Viendo la rueda girando
        data class Exito(val infoUsuario: InfoUsuario) : EstadoAutenticacion() // Entró bien!
        data class Error(val mensaje: String)       : EstadoAutenticacion() // Falló, muestra este mensaje
    }
}
