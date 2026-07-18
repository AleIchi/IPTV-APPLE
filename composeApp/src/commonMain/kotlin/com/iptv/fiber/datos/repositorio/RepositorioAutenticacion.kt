package com.iptv.fiber.datos.repositorio

import com.iptv.fiber.datos.api.ClienteApi
import io.ktor.http.isSuccess
import io.ktor.client.statement.bodyAsText
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.decodeFromJsonElement
import com.iptv.fiber.datos.modelo.RespuestaApi
import com.iptv.fiber.datos.modelo.ConfiguracionServidor
import com.iptv.fiber.datos.modelo.InfoServidor
import com.iptv.fiber.datos.modelo.InfoUsuario
import com.iptv.fiber.datos.local.GestorPreferencias
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch


/** 
 * EL PORTERO DEL SERVIDOR.
 * Esta clase se encarga de hablar directamente con los servidores IPTV (Xtream Codes o listas M3U)
 * para validar usuarios, contraseñas y obtener la información de la cuenta (fecha de expiración, límite de pantallas, etc.).
 */
class RepositorioAutenticacion(
    private val clienteApi: ClienteApi,
    private val gestorPreferencias: GestorPreferencias
) {
    private val alcanceRepositorio = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    companion object {
        const val USUARIO_LISTA_M3U = "LISTA_M3U"
        private const val USUARIO_LISTA_M3U_ANTERIOR = "M3U_PLAYLIST"

        /** Devuelve true si [usuario] es el marcador especial que indica modo lista M3U (sin servidor Xtream). */
        fun esUsuarioListaM3U(usuario: String): Boolean =
            usuario == USUARIO_LISTA_M3U || usuario == USUARIO_LISTA_M3U_ANTERIOR

        /**
         * LA FÓRMULA MÁGICA DE XTREAM CODES:
         * Convierte los datos del usuario en un enlace directo de video (URL) que el reproductor pueda leer.
         * Formato universal: http://servidor.com/tipo/usuario/contrasena/idcanal
         * Ejemplo: http://miiptv.com/live/pepito/1234/500.ts
         * Nota importante: Los canales en vivo ("live") SIEMPRE deben terminar en ".ts".
         * Películas o series NO llevan ".ts".
         */
        fun construirUrlTransmisionXtream(
            urlServidor: String,
            usuario: String,
            contrasena: String,
            tipo: String,
            id: Int
        ): String {
            val urlLimpia = normalizarUrlBase(urlServidor) // Quita barras extras al final
            val tipoLimpio = tipo.ifBlank { "live" }.trim('/')
            // Si es un canal en vivo, agregamos ".ts", si es película dejamos solo el ID
            val idConExtension = if (tipoLimpio == "live") "$id.ts" else "$id"
            return "$urlLimpia/$tipoLimpio/$usuario/$contrasena/$idConExtension"
        }

        /**
         * Agrega el esquema http/https si falta, elimina espacios y la barra final
         * para que la URL pueda usarse directamente en todas las peticiones.
         */
        private fun normalizarUrlBase(url: String): String {
            var urlLimpia = url.trim()
            if (urlLimpia.isEmpty()) return ""

            urlLimpia = urlLimpia.replace(" ", "")

            if (!urlLimpia.startsWith("http://") && !urlLimpia.startsWith("https://")) {
                urlLimpia =
                    if (
                        urlLimpia.contains(":") ||
                            urlLimpia.contains("192.168.") ||
                            urlLimpia.contains("10.0.") ||
                            urlLimpia.contains("localhost")
                    ) {
                        "http://$urlLimpia"
                    } else {
                        "https://$urlLimpia"
                    }
            }
            if (urlLimpia.endsWith("/")) urlLimpia = urlLimpia.dropLast(1)
            return urlLimpia
        }
    }

    // Gson removido
    private val _servidorActual = MutableStateFlow<ConfiguracionServidor?>(null)
    val servidorActual: StateFlow<ConfiguracionServidor?> = _servidorActual.asStateFlow()

    private val _estaAutenticado = MutableStateFlow(false)
    val estaAutenticado: StateFlow<Boolean> = _estaAutenticado.asStateFlow()

    // ─── Autenticación Xtream ────────────────────────────────────────────────

    /**
         * INICIAR SESIÓN EN XTREAM CODES
         * Este bloque de código pide los datos al servidor y trata de leer su respuesta (JSON).
         * ¿Por qué el código de lectura es tan largo y parece complejo?
         * Porque muchos servidores piratas/baratos de IPTV devuelven el JSON roto o mal formado.
         * Este código actúa como un "traductor extremo", intentando leer el archivo de 5 formas distintas
         * hasta que logra rescatar la fecha de vencimiento y el límite de pantallas del usuario.
         */
    suspend fun autenticar(
        urlServidor: String,
        usuario: String,
        contrasena: String
    ): Result<InfoUsuario> {
        return try {
            val urlLimpia = normalizarUrl(urlServidor)
            if (urlLimpia.isBlank()) {
                return Result.failure(Exception("La URL no puede estar vacía."))
            }
            val urlAuth   = construirUrlAutenticacion(urlLimpia)
            val api       = clienteApi.crearApiParaServidor(urlLimpia)

            val respuesta = api.autenticar(urlAuth, usuario, contrasena)

            if (respuesta.status.isSuccess()) {
                val jsonTexto = respuesta.bodyAsText()
                if (jsonTexto.isNotBlank()) {
                    try {
                        var infoUsuario: InfoUsuario? = null
                        var nombreServidor = urlLimpia

                        try {
                            val objetoJson = Json.parseToJsonElement(jsonTexto).jsonObject
                            if (objetoJson.containsKey("user_info")) {
                                val jsonInfoUsuario = objetoJson["user_info"]!!
                                infoUsuario = Json { ignoreUnknownKeys = true }.decodeFromJsonElement<InfoUsuario>(jsonInfoUsuario)
                                if (objetoJson.containsKey("server_info")) {
                                    try {
                                        val jsonInfoServidor = objetoJson["server_info"]!!
                                        val infoServidor = Json { ignoreUnknownKeys = true }.decodeFromJsonElement<InfoServidor>(jsonInfoServidor)
                                        if (infoServidor.url.isNotEmpty()) nombreServidor = infoServidor.url
                                    } catch (_: Exception) { /* JSON de servidor incompleto */ }
                                }
                            }
                        } catch (_: Exception) {
                            try {
                                infoUsuario = Json { ignoreUnknownKeys = true }.decodeFromString<InfoUsuario>(jsonTexto)
                            } catch (_: Exception) {}
                        }

                        if (infoUsuario != null) {
                            if (infoUsuario.auth == 1) {
                                _estaAutenticado.value = true
                                _servidorActual.value = ConfiguracionServidor(
                                    id          = generarIdServidor(urlLimpia, usuario),
                                    urlServidor = urlLimpia,
                                    usuario     = usuario,
                                    contrasena  = contrasena,
                                    estaActivo  = true,
                                    nombreServidor = nombreServidor
                                )
                                gestorPreferencias.guardarCredenciales(urlLimpia, usuario, contrasena)
                                
                                val expDate = infoUsuario.fechaExpiracion ?: ""
                                val maxCon = infoUsuario.conexionesMaximas ?: ""
                                val estado = infoUsuario.estado ?: ""
                                gestorPreferencias.guardarDetallesCuenta(expDate, maxCon, estado)
                                
                                Result.success(infoUsuario)
                            } else {
                                val mensajeOriginal = infoUsuario.mensaje ?: ""
                                val mensajeError = traducirError(mensajeOriginal).ifEmpty { "Usuario o contraseña incorrectos. Por favor, verifica tus datos." }
                                Result.failure(Exception(mensajeError))
                            }
                        } else {
                            Result.failure(Exception("No se pudo leer la respuesta del servidor. Verifica tus datos o intenta de nuevo."))
                        }
                    } catch (e: Exception) {
                        val errorTraducido = traducirError(e.message ?: "Error al leer la respuesta")
                        Result.failure(Exception("Error al leer la respuesta. $errorTraducido"))
                    }
                } else {
                    Result.failure(Exception("Respuesta vacía del servidor"))
                }
            } else {
                val codigoError = respuesta.status.value
                val mensaje = when (codigoError) {
                    401 -> "Usuario o contraseña incorrectos. Verifica tus credenciales."
                    403 -> "Acceso denegado. Es posible que tu cuenta haya expirado o esté bloqueada."
                    404 -> "Servidor no encontrado. Revisa que la URL sea correcta."
                    500 -> "Error interno del servidor. Inténtalo de nuevo más tarde."
                    else -> "Error del servidor ($codigoError). Contacta a soporte."
                }
                Result.failure(Exception(mensaje))
            }
        } catch (e: Exception) {
            val msj = e.message ?: ""
            if (msj.contains("timeout", true)) {
                Result.failure(Exception("Tiempo de espera agotado. El servidor está tardando demasiado en responder."))
            } else if (msj.contains("host", true) || msj.contains("resolve", true)) {
                Result.failure(Exception("No se pudo encontrar el servidor. Revisa tu conexión a internet o la URL."))
            } else {
                val errorTraducido = traducirError(e.message ?: "Fallo de conexión")
                Result.failure(Exception(errorTraducido))
            }
        }
    }

    /** Traduce mensajes de error comunes del servidor o del sistema al español. */
    private fun traducirError(mensaje: String): String {
        val m = mensaje.lowercase()
        return when {
            m.isBlank() -> "Error de conexión desconocido."
            m.contains("expected url scheme") ||
                m.contains("no protocol") ||
                m.contains("malformed url") ||
                m.contains("unknown protocol") ||
                m.contains("no scheme") ||
                m.contains("url is empty") ||
                m.contains("empty url") -> "Ingresa una URL válida. Debe comenzar con http:// o https://."
            m.contains("unexpected end of stream") -> "Error de conexión: El servidor cortó la comunicación inesperadamente."
            m.contains("max_connections") || m.contains("max connections") || m.contains("reached max") -> 
                "Límite de dispositivos excedido. Por favor, cierra sesión en otro equipo."
            m.contains("expired") -> "Tu cuenta ha expirado. Contacta con tu proveedor."
            m.contains("invalid") || m.contains("wrong") -> "Usuario o contraseña incorrectos."
            m.contains("banned") || m.contains("blocked") -> "Tu cuenta ha sido bloqueada temporalmente."
            m.contains("timeout") -> "Tiempo de espera agotado. El servidor no responde."
            m.contains("connection refused") -> "Conexión rechazada. El servidor podría estar caído."
            m.contains("404") || m.contains("not found") -> "Servidor no encontrado. Revisa la URL."
            m.contains("500") || m.contains("internal server error") -> "Error interno del servidor. Inténtalo más tarde."
            else -> "No se pudo completar la operación. Revisa tus datos e intenta de nuevo."
        }
    }

    // ─── Autenticación M3U ───────────────────────────────────────────────────

    /**
     * DESCARGAR LISTA M3U (Texto plano):
     * Se conecta a la URL de la lista M3U y la descarga.
     * Si la descarga falla porque el certificado SSL (el candadito del navegador) de la lista
     * está vencido, no se rinde: intenta de nuevo usando un cliente "Tolerante" que ignora los
     * problemas de seguridad, garantizando que el usuario pueda ver sus canales.
     */
    private suspend fun descargarM3UConOkHttp(url: String): String {
        return com.iptv.fiber.descargarM3U(url)
    }

    /**
     * Descarga y parsea la lista M3U desde [urlLista], extrae los canales y establece
     * una sesión local sin credenciales de servidor Xtream.
     */
    suspend fun autenticarM3U(urlLista: String): Result<InfoUsuario> {
        return try {
            val urlNormalizada = normalizarUrl(urlLista)
            if (urlNormalizada.isBlank()) {
                return Result.failure(Exception("La URL no puede estar vacía."))
            }
            val analizador = com.iptv.fiber.datos.utilidades.AnalizadorM3U()
            val contenido = descargarM3UConOkHttp(urlNormalizada)
            val datos = analizador.procesar(contenido)
            if (datos.isEmpty()) {
                return Result.failure(Exception("La lista no contiene canales válidos o no tiene el formato M3U correcto."))
            }
            RepositorioContenido.establecerDatosM3U(datos)

            _estaAutenticado.value = true
            val servidorM3U = ConfiguracionServidor(
                id          = urlNormalizada.hashCode().toString(),
                urlServidor = urlNormalizada,
                usuario     = USUARIO_LISTA_M3U,
                contrasena  = "",
                estaActivo  = true,
                nombreServidor = "Lista M3U"
            )
            _servidorActual.value = servidorM3U
            gestorPreferencias.guardarCredenciales(urlNormalizada, USUARIO_LISTA_M3U, "")

            Result.success(InfoUsuario(usuario = "Modo M3U", estado = "Activo", auth = 1, mensaje = "Lista cargada"))
        } catch (e: Exception) {
            val msj = e.message ?: ""
            if (msj.contains("ssl", true) || msj.contains("handshake", true)) {
                Result.failure(Exception("No se pudo cargar la lista. Error de seguridad SSL. Verifica que la fecha y hora de tu dispositivo sean correctas."))
            } else if (msj.contains("timeout", true)) {
                Result.failure(Exception("No se pudo cargar la lista. Tiempo de espera agotado al descargar el archivo."))
            } else if (msj.contains("host", true) || msj.contains("resolve", true)) {
                Result.failure(Exception("No se pudo cargar la lista. No se encuentra el servidor. Revisa tu conexión a internet o la URL."))
            } else {
                val errorTraducido = traducirError(e.message ?: "Error al cargar la lista")
                Result.failure(Exception("No se pudo cargar la lista. $errorTraducido"))
            }
        }
    }
    /** Intenta recuperar una sesión guardada. */
    suspend fun intentarAutoInicioSesion(): Boolean {
        return try {
            // Leemos las tres credenciales EN PARALELO (async) en vez de tres .first() en serie,
            // para que el auto-login no espere tres lecturas de disco encadenadas al arrancar.
            val credenciales = coroutineScope {
                val url = async { gestorPreferencias.urlServidor.first() }
                val usuario = async { gestorPreferencias.usuario.first() }
                val contrasena = async { gestorPreferencias.contrasena.first() }
                listOf(url.await(), usuario.await(), contrasena.await())
            }
            val urlGuardada = credenciales[0]
            val usuarioGuardado = credenciales[1]
            val contrasenaGuardada = credenciales[2]

            println("InicioAutomatico URL: '$urlGuardada', Usuario: '$usuarioGuardado'")

            val modoM3U = esUsuarioListaM3U(usuarioGuardado)

            if (urlGuardada.isNotEmpty() && usuarioGuardado.isNotEmpty() && (contrasenaGuardada.isNotEmpty() || modoM3U)) {
                _servidorActual.value = ConfiguracionServidor(
                    id          = if (modoM3U) urlGuardada.hashCode().toString() else generarIdServidor(urlGuardada, usuarioGuardado),
                    urlServidor = urlGuardada,
                    usuario     = if (modoM3U) USUARIO_LISTA_M3U else usuarioGuardado,
                    contrasena  = contrasenaGuardada,
                    estaActivo  = true,
                    nombreServidor = if (modoM3U) "Lista M3U" else urlGuardada
                )
                if (modoM3U) {
                    try {
                        val urlNormalizada = normalizarUrl(urlGuardada)
                        val analizador = com.iptv.fiber.datos.utilidades.AnalizadorM3U()
                        val contenido = descargarM3UConOkHttp(urlNormalizada)
                        val datos = analizador.procesar(contenido)
                        com.iptv.fiber.datos.repositorio.RepositorioContenido.establecerDatosM3U(datos)
                    } catch (e: Exception) {
                        println("InicioAutomatico Error al recargar lista M3U: $e")
                    }
                }
                _estaAutenticado.value = true
                true
            } else {
                false
            }
        } catch (e: Exception) {
            println("InicioAutomatico Error durante inicio de sesión automático: $e")
            false
        }
    }

    /** Cierra la sesión y borra los datos guardados. */
    fun cerrarSesion() {
        _estaAutenticado.value = false
        _servidorActual.value = null
        alcanceRepositorio.launch { gestorPreferencias.limpiarCredenciales() }
    }

    /** Restaura una sesion activa cuando una pantalla recibe las credenciales por Intent. */
    fun establecerSesionActiva(
        urlServidor: String,
        usuario: String,
        contrasena: String,
        nombreServidor: String? = null
    ) {
        val urlLimpia = normalizarUrl(urlServidor)
        if (urlLimpia.isBlank() || usuario.isBlank()) return

        val modoM3U = esUsuarioListaM3U(usuario)
        _servidorActual.value = ConfiguracionServidor(
            id = if (modoM3U) urlLimpia.hashCode().toString() else generarIdServidor(urlLimpia, usuario),
            urlServidor = urlLimpia,
            usuario = if (modoM3U) USUARIO_LISTA_M3U else usuario,
            contrasena = contrasena,
            estaActivo = true,
            nombreServidor = nombreServidor?.takeIf { it.isNotBlank() }
                ?: if (modoM3U) "Lista M3U" else urlLimpia
        )
        _estaAutenticado.value = true
    }

    // ─── Utilidades ──────────────────────────────────────────────────────────

    /** Normaliza [url] antes de usarla en peticiones de red (delegado de [normalizarUrlBase]). */
    private fun normalizarUrl(url: String): String {
        return normalizarUrlBase(url)
    }

    /** Construye la URL del endpoint de autenticación Xtream Codes para [urlBase]. */
    private fun construirUrlAutenticacion(urlBase: String): String = "$urlBase/player_api.php"

    /** Genera un ID único para el par [url]+[usuario], usado para identificar el servidor en Room. */
    private fun generarIdServidor(url: String, usuario: String): String = "${url}_$usuario".hashCode().toString()

    /** Punto de entrada público para construir la URL de transmisión; delega en [construirUrlTransmisionXtream]. */
    fun construirUrlTransmision(urlServidor: String, usuario: String, contrasena: String, tipo: String, id: Int): String {
        return construirUrlTransmisionXtream(urlServidor, usuario, contrasena, tipo, id)
    }
}
