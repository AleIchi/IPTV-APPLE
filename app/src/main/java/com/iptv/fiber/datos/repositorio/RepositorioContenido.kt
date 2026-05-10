package com.iptv.fiber.datos.repositorio

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.JsonParser
import com.iptv.fiber.datos.api.ClienteApi
import com.iptv.fiber.datos.local.base_datos.DaoFavorito
import com.iptv.fiber.datos.local.base_datos.DaoSeguirViendo
import com.iptv.fiber.datos.local.base_datos.Favorito
import com.iptv.fiber.datos.local.base_datos.SeguirViendo
import com.iptv.fiber.datos.modelo.Canal
import com.iptv.fiber.datos.modelo.Categoria
import com.iptv.fiber.datos.modelo.ConfiguracionServidor
import com.iptv.fiber.datos.modelo.EPG
import com.iptv.fiber.datos.modelo.Pelicula
import com.iptv.fiber.datos.modelo.Radio
import com.iptv.fiber.datos.modelo.Serie
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow

/** Repositorio principal de contenido: canales, películas, series, radio, favoritos e historial. */
class RepositorioContenido(
        private val clienteApi: ClienteApi,
        private val repositorioAuth: RepositorioAutenticacion,
        private val daoFavorito: DaoFavorito,
        private val daoSeguirViendo: DaoSeguirViendo
) {
    private val gson: Gson = GsonBuilder().setLenient().create()

    companion object {
        private var canalesEnCache: List<Canal> = emptyList()
        private var mapaCanales: Map<Int, Canal> = emptyMap()
        private var contextoReproduccion: List<Canal> = emptyList()
        private var datosM3U: Map<String, List<Canal>> = emptyMap()

        private fun actualizarCache(canales: List<Canal>) {
            canalesEnCache = canales
            mapaCanales = canales.associateBy { it.id_transmision }
        }

        fun establecerDatosM3U(datos: Map<String, List<Canal>>) {
            datosM3U = datos
            actualizarCache(datos.values.flatten())
        }

        fun obtenerSiguienteCanal(idStreamActual: Int): Canal? {
            val lista =
                    if (contextoReproduccion.isNotEmpty()) contextoReproduccion else canalesEnCache
            if (lista.isEmpty()) return null
            val indice = lista.indexOfFirst { it.id_transmision == idStreamActual }
            if (indice == -1) return null
            return lista[(indice + 1) % lista.size]
        }

        fun obtenerCanalAnterior(idStreamActual: Int): Canal? {
            val lista =
                    if (contextoReproduccion.isNotEmpty()) contextoReproduccion else canalesEnCache
            if (lista.isEmpty()) return null
            val indice = lista.indexOfFirst { it.id_transmision == idStreamActual }
            if (indice == -1) return null
            return lista[if (indice - 1 < 0) lista.size - 1 else indice - 1]
        }

        fun establecerContextoReproduccion(canales: List<Canal>) {
            contextoReproduccion = canales
        }

        fun obtenerCanalCompleto(idStream: Int, fallback: Canal): Canal =
                mapaCanales[idStream] ?: fallback

        private var categoriasCache: List<Categoria> = emptyList()

        fun establecerCategoriasCache(categorias: List<Categoria>) {
            categoriasCache = categorias
        }

        fun obtenerCanalesRecomendados(idStreamActual: Int): List<Canal> {
            val lista = if (contextoReproduccion.isNotEmpty()) contextoReproduccion else canalesEnCache
            if (lista.isEmpty()) return emptyList()
            val indice = lista.indexOfFirst { it.id_transmision == idStreamActual }
            if (indice == -1) return lista.take(10)
            
            val recomendados = mutableListOf<Canal>()
            for (i in 1..10) {
                recomendados.add(lista[(indice + i) % lista.size])
            }
            return recomendados
        }

        fun obtenerNombreCategoria(idCategoria: String): String {
            return categoriasCache.find { it.id_categoria == idCategoria }?.nombre_categoria ?: "Canales"
        }

        fun obtenerConteoCanalesCategoria(idCategoria: String): Int {
            return canalesEnCache.count { it.id_categoria == idCategoria }
        }

        fun limpiarCache() {
            canalesEnCache = emptyList()
            mapaCanales = emptyMap()
            contextoReproduccion = emptyList()
            datosM3U = emptyMap()
            categoriasCache = emptyList()
        }
    }

    // ─── Utilidades privadas ─────────────────────────────────────────────────

    private suspend fun obtenerServidor(): ConfiguracionServidor? =
            repositorioAuth.servidorActual.first()

    private fun construirUrlApi(servidor: ConfiguracionServidor): String =
            "${servidor.urlServidor}/player_api.php"

    private suspend fun procesarLogos(canales: List<Canal>): List<Canal> {
        val servidor = obtenerServidor() ?: return canales
        val urlBase = servidor.urlServidor.trimEnd('/')
        return canales.map { canal ->
            val iconoCorregido = normalizarUrlImagen(canal.icono_transmision, urlBase)
            if (iconoCorregido != canal.icono_transmision) canal.copy(icono_transmision = iconoCorregido)
            else canal
        }
    }

    private suspend fun procesarPortadasPeliculas(peliculas: List<Pelicula>): List<Pelicula> {
        val servidor = obtenerServidor() ?: return peliculas
        val urlBase = servidor.urlServidor.trimEnd('/')
        return peliculas.map { pelicula ->
            val iconoCorregido = normalizarUrlImagen(pelicula.icono_transmision, urlBase)
            if (iconoCorregido != pelicula.icono_transmision) pelicula.copy(icono_transmision = iconoCorregido)
            else pelicula
        }
    }

    private suspend fun procesarPortadasSeries(series: List<Serie>): List<Serie> {
        val servidor = obtenerServidor() ?: return series
        val urlBase = servidor.urlServidor.trimEnd('/')
        return series.map { serie ->
            val portadaCorregida = normalizarUrlImagen(serie.portada, urlBase)
            if (portadaCorregida != serie.portada) serie.copy(portada = portadaCorregida)
            else serie
        }
    }

    /**
     * Normaliza una URL de imagen: reemplaza IPs privadas del servidor por la URL pública
     * y convierte rutas relativas en absolutas. Esto soluciona el problema de servidores XUI
     * que almacenan los logos apuntando a su IP de red local (ej: 192.168.x.x).
     */
    private fun normalizarUrlImagen(url: String?, urlPublica: String): String? {
        if (url.isNullOrBlank()) return url
        // Caso 1: URL relativa -> convertir a absoluta
        if (!url.startsWith("http")) {
            val ruta = if (url.startsWith("/")) url else "/$url"
            return "$urlPublica$ruta"
        }
        // Caso 2: URL con IP privada -> reemplazar con IP pública
        // Detecta rangos privados: 192.168.x.x, 10.x.x.x, 172.16-31.x.x
        val patronIpPrivada = Regex("https?://(192\\.168\\.[0-9]+\\.[0-9]+|10\\.[0-9]+\\.[0-9]+\\.[0-9]+|172\\.(1[6-9]|2[0-9]|3[01])\\.[0-9]+\\.[0-9]+)(:[0-9]+)?")
        val coincidencia = patronIpPrivada.find(url)
        if (coincidencia != null) {
            return url.replace(coincidencia.value, urlPublica)
        }
        return url
    }

    /**
     * Analiza un array JSON que puede estar incompleto o truncado. Devuelve solo los objetos
     * completos que se pudieron parsear.
     */
    private inline fun <reified T> parsearJsonParcial(jsonTexto: String): List<T> {
        val lista = mutableListOf<T>()
        val textoLimpio = jsonTexto.trim()
        val contenido =
                when {
                    textoLimpio.startsWith('[') && textoLimpio.endsWith(']') ->
                            textoLimpio.substring(1, textoLimpio.length - 1).trim()
                    textoLimpio.startsWith('[') -> textoLimpio.substring(1).trim()
                    else -> textoLimpio
                }
        try {
            val arrayJson = JsonParser.parseString(jsonTexto).asJsonArray
            arrayJson.forEach { elemento ->
                try {
                    lista.add(gson.fromJson(elemento, T::class.java))
                } catch (_: Exception) {
                    /* Omitir objetos mal formados */
                }
            }
            return lista
        } catch (_: Exception) {
            // Si el parseo completo falla, extraer objetos uno a uno
            try {
                var posActual = 0
                while (posActual < contenido.length) {
                    while (posActual < contenido.length &&
                            (contenido[posActual].isWhitespace() ||
                                    contenido[posActual] == ',')) posActual++
                    if (posActual >= contenido.length) break

                    val inicioObjeto = contenido.indexOf('{', posActual)
                    if (inicioObjeto == -1) break

                    var contadorLlaves = 0
                    var i = inicioObjeto
                    var finObjeto = -1
                    var dentroDeTexto = false
                    var escapar = false

                    while (i < contenido.length) {
                        val char = contenido[i]
                        if (escapar) {
                            escapar = false
                            i++
                            continue
                        }
                        when (char) {
                            '\\' -> escapar = true
                            '"' -> dentroDeTexto = !dentroDeTexto
                            '{' -> if (!dentroDeTexto) contadorLlaves++
                            '}' ->
                                    if (!dentroDeTexto) {
                                        contadorLlaves--
                                        if (contadorLlaves == 0) {
                                            finObjeto = i + 1
                                            break
                                        }
                                    }
                        }
                        i++
                    }

                    if (finObjeto > inicioObjeto) {
                        try {
                            val jsonObjeto = contenido.substring(inicioObjeto, finObjeto)
                            lista.add(gson.fromJson(jsonObjeto, T::class.java))
                            posActual = finObjeto
                        } catch (_: Exception) {
                            posActual = inicioObjeto + 1
                        }
                    } else break
                }
            } catch (_: Exception) {
                /* Todos los intentos de parseo fallaron */
            }
        }
        return lista
    }

    // ─── TV en Vivo ──────────────────────────────────────────────────────────

    suspend fun obtenerCategoriasEnVivo(): Result<List<Categoria>> {
        return try {
            val servidor = obtenerServidor() ?: return Result.failure(Exception("No autenticado"))
            if (servidor.usuario == "M3U_PLAYLIST") {
                val cats = datosM3U.keys.map { Categoria(id_categoria = it, nombre_categoria = it) }
                establecerCategoriasCache(cats)
                return Result.success(cats)
            }
            val api = clienteApi.crearApiParaServidor(servidor.urlServidor)
            val respuesta =
                    api.obtenerCategoriasEnVivo(
                            construirUrlApi(servidor),
                            servidor.usuario,
                            servidor.contrasena
                    )
            if (respuesta.isSuccessful) {
                val cuerpo = respuesta.body()?.string()
                if (cuerpo.isNullOrBlank()) Result.success(emptyList())
                else {
                    val categorias = parsearJsonParcial<Categoria>(cuerpo)
                    establecerCategoriasCache(categorias)
                    Result.success(categorias)
                }
            } else {
                Result.failure(
                        Exception(
                                "Error al cargar categorías: ${respuesta.errorBody()?.string() ?: "Error desconocido"}"
                        )
                )
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun obtenerCanalesEnVivo(idCategoria: String? = null): Result<List<Canal>> {
        return try {
            val servidor = obtenerServidor() ?: return Result.failure(Exception("No autenticado"))
            if (servidor.usuario == "M3U_PLAYLIST") {
                val canales =
                        if (idCategoria != null) datosM3U[idCategoria] ?: emptyList()
                        else datosM3U.values.flatten()
                actualizarCache(datosM3U.values.flatten())
                return Result.success(canales)
            }
            val api = clienteApi.crearApiParaServidor(servidor.urlServidor)
            val respuesta =
                    api.obtenerCanalesEnVivo(
                            construirUrlApi(servidor),
                            servidor.usuario,
                            servidor.contrasena,
                            idCategoria = idCategoria
                    )
            if (respuesta.isSuccessful) {
                val cuerpo = respuesta.body()
                if (cuerpo != null) {
                    val jsonTexto = cuerpo.string()
                    android.util.Log.d(
                            "RepositorioContenido",
                            "Longitud de respuesta: ${jsonTexto.length}"
                    )
                    if (jsonTexto.length < 10)
                            return Result.failure(
                                    Exception("El servidor devolvió una respuesta vacía")
                            )

                    val canalesRaw = parsearJsonParcial<Canal>(jsonTexto)
                    android.util.Log.d("RepositorioContenido", "Canales parseados: ${canalesRaw.size}")

                    if (canalesRaw.isNotEmpty()) {
                        val canales = procesarLogos(canalesRaw)
                        actualizarCache(canales)
                        val canalesFiltrados =
                                if (idCategoria != null) {
                                    val idCategInt = idCategoria.toIntOrNull()
                                    canales.filter { c ->
                                        c.id_categoria == idCategoria ||
                                                (idCategInt != null &&
                                                        c.ids_categorias?.contains(idCategInt) ==
                                                                true)
                                    }
                                } else canales
                        android.util.Log.d(
                                "RepositorioContenido",
                                "Filtrados por categoría '$idCategoria': ${canalesFiltrados.size} de ${canales.size}"
                        )
                        Result.success(canalesFiltrados)
                    } else {
                        // Intento alternativo con parseo estándar
                        try {
                            val arrayJson = JsonParser.parseString(jsonTexto).asJsonArray
                            val parseadosRaw =
                                    arrayJson.mapNotNull {
                                        try {
                                            gson.fromJson(it, Canal::class.java)
                                        } catch (_: Exception) {
                                            null
                                        }
                                    }
                            val parseados = procesarLogos(parseadosRaw)
                            actualizarCache(parseados)
                            val filtrados =
                                    if (idCategoria != null) {
                                        val idCategInt = idCategoria.toIntOrNull()
                                        parseados.filter { c ->
                                            c.id_categoria == idCategoria ||
                                                    (idCategInt != null &&
                                                            c.ids_categorias?.contains(
                                                                    idCategInt
                                                            ) == true)
                                        }
                                    } else parseados
                            Result.success(filtrados)
                        } catch (e: Exception) {
                            android.util.Log.e(
                                    "RepositorioContenido",
                                    "Parseo fallido: ${e.message}"
                            )
                            Result.failure(
                                    Exception("Error al cargar canales. Revisa tu conexión.")
                            )
                        }
                    }
                } else Result.failure(Exception("Respuesta vacía del servidor"))
            } else {
                Result.failure(
                        Exception(
                                "Error al cargar canales (${respuesta.code()}): ${respuesta.errorBody()?.string() ?: "Error desconocido"}"
                        )
                )
            }
        } catch (e: Exception) {
            Result.failure(Exception("Error al cargar canales: ${e.message}"))
        }
    }

    // ─── Películas ───────────────────────────────────────────────────────────

    suspend fun obtenerCategoriasPeliculas(): Flow<Result<List<Categoria>>> = flow {
        try {
            val servidor =
                    obtenerServidor()
                            ?: run {
                                emit(Result.failure(Exception("No autenticado")))
                                return@flow
                            }
            val api = clienteApi.crearApiParaServidor(servidor.urlServidor)
            val r =
                    api.obtenerCategoriasPeliculas(
                            construirUrlApi(servidor),
                            servidor.usuario,
                            servidor.contrasena
                    )
            if (r.isSuccessful) emit(Result.success(r.body() ?: emptyList()))
            else emit(Result.failure(Exception("Error al cargar categorías de películas")))
        } catch (e: Exception) {
            emit(Result.failure(e))
        }
    }

    suspend fun obtenerPeliculas(idCategoria: String? = null): Flow<Result<List<Pelicula>>> = flow {
        try {
            val servidor =
                    obtenerServidor()
                            ?: run {
                                emit(Result.failure(Exception("No autenticado")))
                                return@flow
                            }
            val api = clienteApi.crearApiParaServidor(servidor.urlServidor)
            val r =
                    api.obtenerPeliculas(
                            construirUrlApi(servidor),
                            servidor.usuario,
                            servidor.contrasena,
                            idCategoria = idCategoria
                    )
            if (r.isSuccessful) {
                val lista = r.body() ?: emptyList()
                emit(Result.success(procesarPortadasPeliculas(lista)))
            } else {
                emit(Result.failure(Exception("Error al cargar películas")))
            }
        } catch (e: Exception) {
            emit(Result.failure(e))
        }
    }

    // ─── Series ──────────────────────────────────────────────────────────────

    suspend fun obtenerCategoriasSeries(): Flow<Result<List<Categoria>>> = flow {
        try {
            val servidor =
                    obtenerServidor()
                            ?: run {
                                emit(Result.failure(Exception("No autenticado")))
                                return@flow
                            }
            val api = clienteApi.crearApiParaServidor(servidor.urlServidor)
            val r =
                    api.obtenerCategoriasSeries(
                            construirUrlApi(servidor),
                            servidor.usuario,
                            servidor.contrasena
                    )
            if (r.isSuccessful) emit(Result.success(r.body() ?: emptyList()))
            else emit(Result.failure(Exception("Error al cargar categorías de series")))
        } catch (e: Exception) {
            emit(Result.failure(e))
        }
    }

    suspend fun obtenerSeries(idCategoria: String? = null): Flow<Result<List<Serie>>> = flow {
        try {
            val servidor =
                    obtenerServidor()
                            ?: run {
                                emit(Result.failure(Exception("No autenticado")))
                                return@flow
                            }
            val api = clienteApi.crearApiParaServidor(servidor.urlServidor)
            val r =
                    api.obtenerSeries(
                            construirUrlApi(servidor),
                            servidor.usuario,
                            servidor.contrasena,
                            idCategoria = idCategoria
                    )
            if (r.isSuccessful) {
                val lista = r.body() ?: emptyList()
                emit(Result.success(procesarPortadasSeries(lista)))
            } else {
                emit(Result.failure(Exception("Error al cargar series")))
            }
        } catch (e: Exception) {
            emit(Result.failure(e))
        }
    }

    suspend fun obtenerInfoSerie(idSerie: Int): Flow<Result<Serie>> = flow {
        try {
            val servidor =
                    obtenerServidor()
                            ?: run {
                                emit(Result.failure(Exception("No autenticado")))
                                return@flow
                            }
            val api = clienteApi.crearApiParaServidor(servidor.urlServidor)
            val r =
                    api.obtenerInfoSerie(
                            construirUrlApi(servidor),
                            servidor.usuario,
                            servidor.contrasena,
                            idSerie = idSerie
                    )
            if (r.isSuccessful && r.body() != null) emit(Result.success(r.body()!!))
            else emit(Result.failure(Exception("Error al cargar información de la serie")))
        } catch (e: Exception) {
            emit(Result.failure(e))
        }
    }

    // ─── Radio ───────────────────────────────────────────────────────────────

    suspend fun obtenerCategoriasRadio(): Flow<Result<List<Categoria>>> = flow {
        try {
            val servidor =
                    obtenerServidor()
                            ?: run {
                                emit(Result.failure(Exception("No autenticado")))
                                return@flow
                            }
            val api = clienteApi.crearApiParaServidor(servidor.urlServidor)
            val r =
                    api.obtenerCategoriasRadio(
                            construirUrlApi(servidor),
                            servidor.usuario,
                            servidor.contrasena
                    )
            if (r.isSuccessful) emit(Result.success(r.body() ?: emptyList()))
            else emit(Result.failure(Exception("Error al cargar categorías de radio")))
        } catch (e: Exception) {
            emit(Result.failure(e))
        }
    }

    suspend fun obtenerCanalesRadio(idCategoria: String? = null): Flow<Result<List<Radio>>> = flow {
        try {
            val servidor =
                    obtenerServidor()
                            ?: run {
                                emit(Result.failure(Exception("No autenticado")))
                                return@flow
                            }
            val api = clienteApi.crearApiParaServidor(servidor.urlServidor)
            val r =
                    api.obtenerCanalesRadio(
                            construirUrlApi(servidor),
                            servidor.usuario,
                            servidor.contrasena,
                            idCategoria = idCategoria
                    )
            if (r.isSuccessful) emit(Result.success(r.body() ?: emptyList()))
            else emit(Result.failure(Exception("Error al cargar estaciones de radio")))
        } catch (e: Exception) {
            emit(Result.failure(e))
        }
    }

    // ─── Guía de Programación (EPG) ──────────────────────────────────────────

    suspend fun obtenerGuiaProgramacion(
            idStream: Int? = null
    ): Flow<Result<Map<String, List<EPG>>>> = flow {
        try {
            val servidor =
                    obtenerServidor()
                            ?: run {
                                emit(Result.failure(Exception("No autenticado")))
                                return@flow
                            }
            val api = clienteApi.crearApiParaServidor(servidor.urlServidor)
            val r =
                    api.obtenerGuiaProgramacion(
                            construirUrlApi(servidor),
                            servidor.usuario,
                            servidor.contrasena,
                            idStream = idStream
                    )
            if (r.isSuccessful) emit(Result.success(r.body() ?: emptyMap()))
            else emit(Result.failure(Exception("Error al cargar la guía de programación")))
        } catch (e: Exception) {
            emit(Result.failure(e))
        }
    }

    // ─── Favoritos ───────────────────────────────────────────────────────────

    suspend fun obtenerFavoritos(tipo: String? = null): Flow<List<Favorito>> {
        val servidor = obtenerServidor() ?: return flow { emit(emptyList()) }
        return if (tipo == null) daoFavorito.obtenerFavoritos(servidor.urlServidor)
        else daoFavorito.obtenerFavoritosPorTipo(servidor.urlServidor, tipo)
    }

    suspend fun esFavorito(id: String): Boolean = daoFavorito.esFavorito(id)

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

    // ─── Historial ───────────────────────────────────────────────────────────

    suspend fun obtenerHistorial(): Flow<List<SeguirViendo>> {
        val servidor = obtenerServidor() ?: return flow { emit(emptyList()) }
        return daoSeguirViendo.obtenerHistorial(servidor.urlServidor)
    }

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
    }

    suspend fun limpiarHistorial() {
        daoSeguirViendo.limpiarTodoElHistorial()
    }
}
