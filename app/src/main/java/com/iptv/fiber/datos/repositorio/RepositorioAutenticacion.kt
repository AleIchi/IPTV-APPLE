package com.iptv.fiber.datos.repositorio

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.JsonParser
import com.iptv.fiber.datos.api.ClienteApi
import com.iptv.fiber.datos.modelo.RespuestaApi
import com.iptv.fiber.datos.modelo.ConfiguracionServidor
import com.iptv.fiber.datos.modelo.InfoServidor
import com.iptv.fiber.datos.modelo.InfoUsuario
import com.iptv.fiber.datos.local.GestorPreferencias
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

/** Gestiona la autenticación del usuario contra el servidor Xtream o lista M3U. */
class RepositorioAutenticacion(
    private val clienteApi: ClienteApi,
    private val gestorPreferencias: GestorPreferencias
) {
    private val gson: Gson = GsonBuilder().setLenient().create()

    private val _servidorActual = MutableStateFlow<ConfiguracionServidor?>(null)
    val servidorActual: StateFlow<ConfiguracionServidor?> = _servidorActual.asStateFlow()

    private val _estaAutenticado = MutableStateFlow(false)
    val estaAutenticado: StateFlow<Boolean> = _estaAutenticado.asStateFlow()

    // ─── Autenticación Xtream ────────────────────────────────────────────────

    suspend fun autenticar(
        urlServidor: String,
        usuario: String,
        contrasena: String
    ): Result<InfoUsuario> {
        return try {
            val urlLimpia = normalizarUrl(urlServidor)
            val urlAuth   = construirUrlAutenticacion(urlLimpia)
            val api       = clienteApi.crearApiParaServidor(urlLimpia)

            val respuesta = api.autenticar(urlAuth, usuario, contrasena)

            if (respuesta.isSuccessful) {
                val cuerpo = respuesta.body()
                if (cuerpo != null) {
                    try {
                        val jsonTexto = cuerpo.string()
                        var infoUsuario: InfoUsuario? = null
                        var nombreServidor = urlLimpia

                        try {
                            val objetoJson = JsonParser.parseString(jsonTexto).asJsonObject
                            if (objetoJson.has("user_info")) {
                                val jsonInfoUsuario = objetoJson.getAsJsonObject("user_info")
                                infoUsuario = gson.fromJson(jsonInfoUsuario, InfoUsuario::class.java)
                                if (objetoJson.has("server_info")) {
                                    try {
                                        val jsonInfoServidor = objetoJson.getAsJsonObject("server_info")
                                        val infoServidor = gson.fromJson(jsonInfoServidor, InfoServidor::class.java)
                                        if (infoServidor.url.isNotEmpty()) nombreServidor = infoServidor.url
                                    } catch (_: Exception) { /* JSON de servidor incompleto, se usa valor por defecto */ }
                                }
                            }
                        } catch (_: Exception) {
                            // El JSON no se puede parsear completamente, intentar extraer user_info manualmente
                            try {
                                val inicioUserInfo = jsonTexto.indexOf("\"user_info\"")
                                if (inicioUserInfo >= 0) {
                                    val inicioObjeto = jsonTexto.indexOf('{', inicioUserInfo)
                                    if (inicioObjeto >= 0) {
                                        var contadorLlaves = 0
                                        var i = inicioObjeto
                                        var finObjeto = -1
                                        while (i < jsonTexto.length) {
                                            when (jsonTexto[i]) {
                                                '{' -> contadorLlaves++
                                                '}' -> { contadorLlaves--; if (contadorLlaves == 0) { finObjeto = i + 1; break } }
                                            }
                                            i++
                                        }
                                        if (finObjeto > inicioObjeto) {
                                            val jsonUsuario = jsonTexto.substring(inicioObjeto, finObjeto)
                                            infoUsuario = gson.fromJson(jsonUsuario, InfoUsuario::class.java)
                                        }
                                    }
                                }
                            } catch (_: Exception) { /* Extracción manual fallida */ }

                            if (infoUsuario == null) {
                                try { infoUsuario = gson.fromJson(jsonTexto, InfoUsuario::class.java) }
                                catch (_: Exception) { /* Todos los intentos de parseo fallaron */ }
                            }
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
                                
                                // Guardar detalles de cuenta adicionales (Expiración, conexiones, etc)
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
                            Result.failure(Exception("No se pudo leer la respuesta del servidor. Vista previa: ${jsonTexto.take(500)}"))
                        }
                    } catch (e: Exception) {
                        Result.failure(Exception("Error al leer la respuesta: ${e.message}"))
                    }
                } else {
                    Result.failure(Exception("Respuesta vacía del servidor"))
                }
            } else {
                val codigoError = respuesta.code()
                val mensaje = when (codigoError) {
                    401 -> "Usuario o contraseña incorrectos. Verifica tus credenciales."
                    403 -> "Acceso denegado. Es posible que tu cuenta haya expirado o esté bloqueada."
                    404 -> "Servidor no encontrado. Revisa que la URL sea correcta."
                    500 -> "Error interno del servidor. Inténtalo de nuevo más tarde."
                    else -> "Error del servidor ($codigoError). Contacta a soporte."
                }
                Result.failure(Exception(mensaje))
            }
        } catch (e: java.net.UnknownHostException) {
            Result.failure(Exception("No se pudo encontrar el servidor. Revisa tu conexión a internet o la URL."))
        } catch (e: java.net.ConnectException) {
            Result.failure(Exception("No se pudo conectar al servidor. Asegúrate de que el servidor esté activo."))
        } catch (e: java.net.SocketTimeoutException) {
            Result.failure(Exception("Tiempo de espera agotado. El servidor está tardando demasiado en responder."))
        } catch (e: Exception) {
            val errorTraducido = traducirError(e.localizedMessage ?: "Fallo de conexión")
            Result.failure(Exception(errorTraducido))
        }
    }

    /** Traduce mensajes de error comunes del servidor o del sistema al español. */
    private fun traducirError(mensaje: String): String {
        val m = mensaje.lowercase()
        return when {
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
            else -> if (mensaje.length > 5) mensaje else "Error de conexión desconocido."
        }
    }

    // ─── Autenticación M3U ───────────────────────────────────────────────────

    suspend fun autenticarM3U(urlLista: String): Result<InfoUsuario> {
        return try {
            val urlNormalizada = normalizarUrl(urlLista)
            val analizador = com.iptv.fiber.datos.utilidades.AnalizadorM3U()
            val contenido = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                java.net.URL(urlNormalizada).readText()
            }
            val datos = analizador.procesar(contenido)
            RepositorioContenido.establecerDatosM3U(datos)

            _estaAutenticado.value = true
            val servidorM3U = ConfiguracionServidor(
                id          = urlLista.hashCode().toString(),
                urlServidor = urlLista,
                usuario     = "M3U_PLAYLIST",
                contrasena  = "",
                estaActivo  = true,
                nombreServidor = "Lista M3U"
            )
            _servidorActual.value = servidorM3U
            gestorPreferencias.guardarCredenciales(urlNormalizada, "M3U_PLAYLIST", "")

            Result.success(InfoUsuario(usuario = "Modo M3U", estado = "Activo", auth = 1, mensaje = "Lista cargada"))
        } catch (e: Exception) {
            Result.failure(Exception("Error al cargar la lista: ${e.message}"))
        }
    }

    /** Intenta recuperar una sesión guardada. */
    suspend fun intentarAutoInicioSesion(): Boolean {
        return try {
            val urlGuardada  = gestorPreferencias.urlServidor.first()
            val usuarioGuardado  = gestorPreferencias.usuario.first()
            val contrasenaGuardada = gestorPreferencias.contrasena.first()

            android.util.Log.d("AutoLogin", "URL: '$urlGuardada', Usuario: '$usuarioGuardado'")

            val modoM3U = usuarioGuardado == "M3U_PLAYLIST"

            if (urlGuardada.isNotEmpty() && usuarioGuardado.isNotEmpty() && (contrasenaGuardada.isNotEmpty() || modoM3U)) {
                _servidorActual.value = ConfiguracionServidor(
                    id          = if (modoM3U) urlGuardada.hashCode().toString() else generarIdServidor(urlGuardada, usuarioGuardado),
                    urlServidor = urlGuardada,
                    usuario     = usuarioGuardado,
                    contrasena  = contrasenaGuardada,
                    estaActivo  = true,
                    nombreServidor = if (modoM3U) "Lista M3U" else urlGuardada
                )
                if (modoM3U) {
                    try {
                        val urlNormalizada = normalizarUrl(urlGuardada)
                        val analizador = com.iptv.fiber.datos.utilidades.AnalizadorM3U()
                        val contenido = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                            java.net.URL(urlNormalizada).readText()
                        }
                        val datos = analizador.procesar(contenido)
                        com.iptv.fiber.datos.repositorio.RepositorioContenido.establecerDatosM3U(datos)
                    } catch (e: Exception) {
                        android.util.Log.e("AutoLogin", "Error al recargar lista M3U", e)
                    }
                }
                _estaAutenticado.value = true
                true
            } else {
                false
            }
        } catch (e: Exception) {
            android.util.Log.e("AutoLogin", "Error durante inicio de sesión automático", e)
            false
        }
    }

    /** Cierra la sesión y borra los datos guardados. */
    fun cerrarSesion() {
        _estaAutenticado.value = false
        _servidorActual.value = null
        GlobalScope.launch { gestorPreferencias.limpiarCredenciales() }
    }

    // ─── Utilidades ──────────────────────────────────────────────────────────

    private fun normalizarUrl(url: String): String {
        var urlLimpia = url.trim()
        if (urlLimpia.isEmpty()) return ""
        
        // Eliminar espacios internos si los hay por error de teclado
        urlLimpia = urlLimpia.replace(" ", "")
        
        if (!urlLimpia.startsWith("http://") && !urlLimpia.startsWith("https://")) {
            // Por defecto intentar HTTP para IPs con puerto (común en IPTV)
            urlLimpia = if (urlLimpia.contains(":") || urlLimpia.contains("192.168.") || urlLimpia.contains("10.0.") || urlLimpia.contains("localhost")) {
                "http://$urlLimpia"
            } else {
                "https://$urlLimpia"
            }
        }
        if (urlLimpia.endsWith("/")) urlLimpia = urlLimpia.dropLast(1)
        return urlLimpia
    }

    private fun construirUrlAutenticacion(urlBase: String): String = "$urlBase/player_api.php"

    private fun generarIdServidor(url: String, usuario: String): String = "${url}_$usuario".hashCode().toString()

    fun construirUrlStream(urlServidor: String, usuario: String, contrasena: String, tipo: String, id: Int): String {
        val urlLimpia = normalizarUrl(urlServidor)
        return "$urlLimpia/$tipo/$usuario/$contrasena/$id"
    }
}
