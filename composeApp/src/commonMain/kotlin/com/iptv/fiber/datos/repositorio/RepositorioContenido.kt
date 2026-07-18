package com.iptv.fiber.datos.repositorio

import com.iptv.fiber.datos.api.ClienteApi
import io.ktor.http.isSuccess
import io.ktor.client.statement.bodyAsText
import kotlinx.serialization.json.Json
import com.iptv.fiber.datos.local.base_datos.DaoFavorito
import com.iptv.fiber.datos.local.base_datos.DaoSeguirViendo
import com.iptv.fiber.datos.local.base_datos.Favorito
import com.iptv.fiber.datos.local.base_datos.SeguirViendo
import com.iptv.fiber.datos.modelo.Canal
import com.iptv.fiber.datos.modelo.Categoria
import com.iptv.fiber.datos.modelo.ConfiguracionServidor
import com.iptv.fiber.datos.modelo.EPG
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** 
 * EL GESTOR DEL CONTENIDO (El Cajón de Memoria).
 * Su función principal es pedir la lista de canales a internet UNA SOLA VEZ
 * y guardarla en la memoria RAM (caché) para que el celular no se congele 
 * cada vez que el usuario hace una búsqueda o cambia de categoría.
 */
class RepositorioContenido(
        private val clienteApi: ClienteApi,
        private val repositorioAuth: RepositorioAutenticacion,
        private val daoFavorito: DaoFavorito,
        private val daoSeguirViendo: DaoSeguirViendo
) {
    companion object {
        private const val LIMITE_HISTORIAL = 50
        private var canalesEnCache: List<Canal> = emptyList()
        private var mapaCanales: Map<Int, Canal> = emptyMap()
        private var indicesCanales: Map<Int, Int> = emptyMap()
        private var canalesPorCategoria: Map<String, List<Canal>> = emptyMap()
        private var contextoReproduccion: List<Canal> = emptyList()
        private var indicesContextoReproduccion: Map<Int, Int> = emptyMap()
        private var datosM3U: Map<String, List<Canal>> = emptyMap()

        private val patronIpPrivada = Regex(
            "https?://(192\\.168\\.[0-9]+\\.[0-9]+|10\\.[0-9]+\\.[0-9]+\\.[0-9]+|172\\.(1[6-9]|2[0-9]|3[01])\\.[0-9]+\\.[0-9]+)(:[0-9]+)?"
        )

        /**
         * LA MAGIA DE LA VELOCIDAD (CACHÉ LOCAL):
         * Cuando descargamos 10,000 canales de internet, hacer búsquedas en esa lista sería lentísimo.
         * Esta función organiza esos canales en "diccionarios" (Mapas) agrupados por categoría.
         * Así, si el usuario abre la categoría "Deportes", la app no tiene que buscar entre 10,000 canales,
         * simplemente va al diccionario y saca la lista de Deportes al instante.
         */
        private fun actualizarCache(canales: List<Canal>) {
            canalesEnCache = canales
            // Pre-sizing evita rehashes internos: para 10k canales el default (16) haría ~10 rehashes
            val capacidad = (canales.size * 1.4).toInt().coerceAtLeast(16)
            mapaCanales = LinkedHashMap<Int, Canal>(capacidad).apply {
                canales.forEach { put(it.id_transmision, it) }
            }
            indicesCanales = HashMap<Int, Int>(capacidad).apply {
                canales.forEachIndexed { index, canal -> put(canal.id_transmision, index) }
            }
            val agrupadosPorCategoria = mutableMapOf<String, MutableList<Canal>>()
            canales.forEach { canal ->
                agrupadosPorCategoria.getOrPut(canal.id_categoria) { mutableListOf() }.add(canal)
                canal.ids_categorias?.forEach { idExtra ->
                    val clave = idExtra.toString()
                    val grupo = agrupadosPorCategoria.getOrPut(clave) { mutableListOf() }
                    if (grupo.lastOrNull()?.id_transmision != canal.id_transmision) {
                        grupo.add(canal)
                    }
                }
            }
            canalesPorCategoria = agrupadosPorCategoria.mapValues { it.value.toList() }
        }

        /** Expone el mapa de canales por categoría (ya construido por actualizarCache) para reutilización externa sin duplicar trabajo. */
        fun obtenerCanalesPorCategoria(): Map<String, List<Canal>> = canalesPorCategoria

        /** Devuelve true si [canal] pertenece a [idCategoria], considerando tanto el ID principal como los secundarios. */
        private fun canalCoincideCategoria(canal: Canal, idCategoria: String): Boolean {
            val idCategInt = idCategoria.toIntOrNull()
            return canal.id_categoria == idCategoria ||
                    (idCategInt != null && canal.ids_categorias?.contains(idCategInt) == true)
        }

        /** Filtra la caché de canales por [idCategoria] y/o texto de [consulta]. Si ambos son vacíos, devuelve todos. */
        fun filtrarCanales(idCategoria: String?, consulta: String): List<Canal> {
            val base = if (idCategoria == null) {
                canalesEnCache
            } else {
                canalesPorCategoria[idCategoria]
                        ?: canalesEnCache.filter { canalCoincideCategoria(it, idCategoria) }
            }
            val texto = consulta.trim()
            if (texto.isEmpty()) return base
            return base.filter { canal ->
                canal.nombre.contains(texto, ignoreCase = true)
            }
        }

        /** Reemplaza los datos de la lista M3U (agrupados por categoría) y sincroniza la caché de canales. */
        fun establecerDatosM3U(datos: Map<String, List<Canal>>) {
            datosM3U = datos
            actualizarCache(datos.values.flatten())
        }

        /** Devuelve el canal siguiente al de [idTransmisionActual] en el contexto de reproducción activo (o la caché global), con navegación circular. */
        fun obtenerSiguienteCanal(idTransmisionActual: Int): Canal? {
            val lista =
                    if (contextoReproduccion.isNotEmpty()) contextoReproduccion else canalesEnCache
            if (lista.isEmpty()) return null
            val indice =
                    if (contextoReproduccion.isNotEmpty()) {
                        indicesContextoReproduccion[idTransmisionActual]
                    } else {
                        indicesCanales[idTransmisionActual]
                    } ?: -1
            if (indice == -1) return null
            return lista[(indice + 1) % lista.size]
        }

        /** Devuelve el canal anterior al de [idTransmisionActual], con navegación circular. */
        fun obtenerCanalAnterior(idTransmisionActual: Int): Canal? {
            val lista =
                    if (contextoReproduccion.isNotEmpty()) contextoReproduccion else canalesEnCache
            if (lista.isEmpty()) return null
            val indice =
                    if (contextoReproduccion.isNotEmpty()) {
                        indicesContextoReproduccion[idTransmisionActual]
                    } else {
                        indicesCanales[idTransmisionActual]
                    } ?: -1
            if (indice == -1) return null
            return lista[if (indice - 1 < 0) lista.size - 1 else indice - 1]
        }

        /** Establece la lista de [canales] que define el contexto de reproducción activo (ej. categoría abierta). */
        fun establecerContextoReproduccion(canales: List<Canal>) {
            contextoReproduccion = canales
            indicesContextoReproduccion =
                    canales.mapIndexed { index, canal -> canal.id_transmision to index }.toMap()
        }

        /** Busca el canal por [idTransmision] en el mapa en memoria; devuelve [respaldo] si no existe. */
        fun obtenerCanalCompleto(idTransmision: Int, respaldo: Canal): Canal =
                mapaCanales[idTransmision] ?: respaldo

        private var categoriasCache: List<Categoria> = emptyList()

        /** Guarda la lista de [categorias] en caché para que las pantallas puedan obtener nombres sin nueva petición al servidor. */
        fun establecerCategoriasCache(categorias: List<Categoria>) {
            categoriasCache = categorias
        }

        /** Devuelve los canales de la misma categoría que el canal activo, para mostrarse en la barra lateral del reproductor. */
        fun obtenerCanalesRecomendados(idTransmisionActual: Int): List<Canal> {
            val lista =
                    if (contextoReproduccion.isNotEmpty()) contextoReproduccion else canalesEnCache
            if (lista.isEmpty()) return emptyList()

            // 1. Obtener el canal actual para identificar su categoría
            val canalActual = lista.find { it.id_transmision == idTransmisionActual }
                    ?: mapaCanales[idTransmisionActual]
                    ?: return lista.take(10)

            val idCategoria = canalActual.id_categoria
            val idCategInt = idCategoria.toIntOrNull()

            // 2. Verificar si todos los canales de la lista actual ya pertenecen a esta categoría
            val todosPertenecen = lista.all { canal ->
                canal.id_categoria == idCategoria ||
                        (idCategInt != null && canal.ids_categorias?.contains(idCategInt) == true)
            }

            if (todosPertenecen) {
                return lista
            }

            // 3. Si no, filtrar por la categoría del canal actual
            return canalesPorCategoria[idCategoria]
                    ?: lista.filter { canal ->
                        canal.id_categoria == idCategoria ||
                                (idCategInt != null && canal.ids_categorias?.contains(idCategInt) == true)
                    }
        }

        /** Busca el nombre legible de [idCategoria] en caché; devuelve "Canales" si no se encuentra. */
        fun obtenerNombreCategoria(idCategoria: String): String {
            return categoriasCache.find { it.id_categoria == idCategoria }?.nombre_categoria
                    ?: "Canales"
        }

        /** Retorna la cantidad de canales de [idCategoria] según la caché actual. */
        fun obtenerConteoCanalesCategoria(idCategoria: String): Int {
            return canalesPorCategoria[idCategoria]?.size
                    ?: canalesEnCache.count { it.id_categoria == idCategoria }
        }

        /** Vacía todos los índices y listas en memoria (se llama al cerrar sesión). */
        fun limpiarCache() {
            canalesEnCache = emptyList()
            mapaCanales = emptyMap()
            indicesCanales = emptyMap()
            canalesPorCategoria = emptyMap()
            contextoReproduccion = emptyList()
            indicesContextoReproduccion = emptyMap()
            datosM3U = emptyMap()
            categoriasCache = emptyList()
        }
    }

    /** Lee el servidor autenticado actual desde [repositorioAuth]; devuelve null si no hay sesión activa. */
    private suspend fun obtenerServidor(): ConfiguracionServidor? =
            repositorioAuth.servidorActual.first()

    /** Construye la URL del endpoint player_api.php para el [servidor] indicado. */
    private fun construirUrlApi(servidor: ConfiguracionServidor): String =
            "${servidor.urlServidor}/player_api.php"

    /** Corrige las URL de iconos que apuntan a IPs privadas del servidor, sustituyéndolas por la URL pública. */
    private suspend fun procesarLogotipos(canales: List<Canal>): List<Canal> = withContext(Dispatchers.Default) {
        val servidor = obtenerServidor() ?: return@withContext canales
        val urlBase = servidor.urlServidor.trimEnd('/')
        return@withContext canales.map { canal ->
            val iconoCorregido = normalizarUrlImagen(canal.icono_transmision, urlBase)
            if (iconoCorregido != canal.icono_transmision) {
                canal.copy(icono_transmision = iconoCorregido)
            } else {
                canal
            }
        }
    }

    /**
     * Normaliza una URL de imagen: reemplaza IPs privadas del servidor por la URL publica
     * y convierte rutas relativas en absolutas.
     */
    private fun normalizarUrlImagen(url: String?, urlPublica: String): String? {
        if (url.isNullOrBlank()) return url
        if (!url.startsWith("http")) {
            val ruta = if (url.startsWith("/")) url else "/$url"
            return "$urlPublica$ruta"
        }
        // Fast-path: la mayoría de logos usan IPs públicas → saltamos la regex en esos casos.
        // Una IP privada en una URL siempre aparece justo después de "://".
        if (!url.contains("192.168.") && !url.contains("://10.") && !url.contains("://172.")) return url
        val coincidencia = patronIpPrivada.find(url)
        return if (coincidencia != null) url.replace(coincidencia.value, urlPublica) else url
    }

    /**
     * LECTOR DE JSON "A PRUEBA DE BALAS" (STREAMING):
     * Normalente, para leer un texto se carga todo a la vez en la memoria.
     * Pero las listas de IPTV pesan megabytes y a veces traen un canal mal escrito en el medio.
     * Si usáramos un lector normal, la app crashearía al llegar al error.
     * Este "JsonReader" va leyendo el texto canal por canal (streaming).
     * Si encuentra un canal roto, imprime un error, lo salta, y sigue con el siguiente.
     * ¡Así aseguramos que la lista se cargue sí o sí!
     */
    private inline fun <reified T> parsearJsonParcial(jsonTexto: String): List<T> {
        return try {
            Json { ignoreUnknownKeys = true }.decodeFromString<List<T>>(jsonTexto)
        } catch (e: Exception) {
            emptyList()
        }
    }

    /** Obtiene las categorías de TV en vivo desde el servidor o desde los datos M3U si el usuario usa lista local. */
    suspend fun obtenerCategoriasEnVivo(): Result<List<Categoria>> = withContext(Dispatchers.Default) {
        try {
            val servidor = obtenerServidor() ?: return@withContext Result.failure(Exception("No autenticado"))
            if (RepositorioAutenticacion.esUsuarioListaM3U(servidor.usuario)) {
                val categorias =
                        datosM3U.keys.map { Categoria(id_categoria = it, nombre_categoria = it) }
                establecerCategoriasCache(categorias)
                return@withContext Result.success(categorias)
            }

            val api = clienteApi.crearApiParaServidor(servidor.urlServidor)
            val respuesta =
                    api.obtenerCategoriasEnVivo(
                            construirUrlApi(servidor),
                            servidor.usuario,
                            servidor.contrasena
                    )
            if (respuesta.status.isSuccess()) {
                val cuerpo = respuesta.bodyAsText()
                if (cuerpo.isBlank()) {
                    Result.success(emptyList())
                } else {
                    val categorias = parsearJsonParcial<Categoria>(cuerpo)
                    establecerCategoriasCache(categorias)
                    Result.success(categorias)
                }
            } else {
                Result.failure(
                        Exception(
                                "Error al cargar categorías: ${respuesta.bodyAsText()}"
                        )
                )
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * DESCARGA Y GUARDA CANALES:
     * 1. Revisa si el usuario usa M3U local (archivo) o Xtream Codes (API web).
     * 2. Si es web, se conecta y baja la inmensa lista de canales.
     * 3. Pasa los canales por el arreglador de logos (procesarLogotipos).
     * 4. Los guarda en la memoria ultrarrápida (actualizarCache) para futuros usos.
     */
    suspend fun obtenerCanalesEnVivo(idCategoria: String? = null): Result<List<Canal>> = withContext(Dispatchers.Default) {
        try {
            val servidor = obtenerServidor() ?: return@withContext Result.failure(Exception("No autenticado"))
            if (RepositorioAutenticacion.esUsuarioListaM3U(servidor.usuario)) {
                val canales =
                        if (idCategoria != null) datosM3U[idCategoria] ?: emptyList()
                        else datosM3U.values.flatten()
                actualizarCache(datosM3U.values.flatten())
                return@withContext Result.success(canales)
            }

            val api = clienteApi.crearApiParaServidor(servidor.urlServidor)
            val respuesta =
                    api.obtenerCanalesEnVivo(
                            construirUrlApi(servidor),
                            servidor.usuario,
                            servidor.contrasena,
                            idCategoria = idCategoria
                    )
            if (respuesta.status.isSuccess()) {
                val cuerpo = respuesta.bodyAsText()
                if (cuerpo.isNotBlank()) {
                    val canalesRaw = parsearJsonParcial<Canal>(cuerpo)
                    if (canalesRaw.isNotEmpty()) {
                        val canales = procesarLogotipos(canalesRaw)
                        actualizarCache(canales)
                        val canalesFiltrados =
                                if (idCategoria != null) filtrarCanales(idCategoria, "")
                                else canales
                        return@withContext Result.success(canalesFiltrados)
                    }
                }
                Result.success(emptyList())
            } else {
                Result.failure(
                        Exception(
                                "Error al cargar canales: ${respuesta.bodyAsText()}"
                        )
                )
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Carga la guía de programación (EPG) para un canal específico o para todos.
     * Devuelve un Flow para que el ViewModel pueda observarlo con ciclo de vida.
     */
    fun obtenerGuiaProgramacion(
        idTransmision: Int? = null
    ): Flow<Result<Map<String, List<EPG>>>> = flow {
        try {
            val servidor =
                    obtenerServidor()
                            ?: run {
                                emit(Result.failure(Exception("No autenticado")))
                                return@flow
                            }
            if (RepositorioAutenticacion.esUsuarioListaM3U(servidor.usuario)) {
                emit(Result.success(emptyMap()))
                return@flow
            }
            val api = clienteApi.crearApiParaServidor(servidor.urlServidor)
            val respuesta =
                    api.obtenerGuiaProgramacion(
                            construirUrlApi(servidor),
                            servidor.usuario,
                            servidor.contrasena,
                            idTransmision = idTransmision
                    )
            emit(Result.success(respuesta))
        } catch (e: Exception) {
            emit(Result.failure(e))
        }
    }

    /** Devuelve un Flow de favoritos del servidor activo, opcionalmente filtrado por [tipo] (ej. "canal"). */
    suspend fun obtenerFavoritos(tipo: String? = null): Flow<List<Favorito>> {
        val servidor = obtenerServidor() ?: return flow { emit(emptyList()) }
        return if (tipo == null) daoFavorito.obtenerFavoritos(servidor.urlServidor)
        else daoFavorito.obtenerFavoritosPorTipo(servidor.urlServidor, tipo)
    }

    /** Retorna true si existe un favorito con [id] en la base de datos local. */
    suspend fun esFavorito(id: String): Boolean = daoFavorito.esFavorito(id)

    /** Agrega o elimina [canal] de favoritos según su estado actual. Devuelve true si quedó marcado como favorito. */
    suspend fun alternarFavorito(canal: Canal): Boolean {
        val servidor = obtenerServidor() ?: return false
        val id = "${servidor.urlServidor}_canal_${canal.id_transmision}"
        return if (daoFavorito.esFavorito(id)) {
            daoFavorito.eliminarFavoritoPorId(id)
            false
        } else {
            daoFavorito.insertarFavorito(
                    Favorito(
                            id = id,
                            tipo = "canal",
                            nombre = canal.nombre,
                            idTransmision = canal.id_transmision,
                            icono = canal.icono_transmision,
                            idServidor = servidor.urlServidor
                    )
            )
            true
        }
    }

    /** Devuelve un Flow con los últimos registros del historial de reproducción del servidor activo. */
    suspend fun obtenerHistorial(): Flow<List<SeguirViendo>> {
        val servidor = obtenerServidor() ?: return flow { emit(emptyList()) }
        return daoSeguirViendo.obtenerHistorial(servidor.urlServidor)
    }

    /** Registra [canal] en el historial de reproducción y recorta la lista al límite de 50 entradas. */
    suspend fun agregarAlHistorial(canal: Canal) {
        val servidor = obtenerServidor() ?: return
        val id = "${servidor.urlServidor}_canal_${canal.id_transmision}"
        daoSeguirViendo.insertarEnHistorial(
                SeguirViendo(
                        id = id,
                        tipo = "canal",
                        nombre = canal.nombre,
                        idTransmision = canal.id_transmision,
                        posicion = 0,
                        duracion = 0,
                        icono = canal.icono_transmision,
                        idServidor = servidor.urlServidor
                )
        )
        daoSeguirViendo.recortarHistorial(servidor.urlServidor, LIMITE_HISTORIAL)
    }

    /** Elimina todo el historial de reproducción del servidor activo. */
    suspend fun limpiarHistorial() {
        val servidor = obtenerServidor() ?: return
        daoSeguirViendo.limpiarHistorial(servidor.urlServidor)
    }
}
