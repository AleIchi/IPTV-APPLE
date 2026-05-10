package com.iptv.fiber.interfaz.modelovista

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.iptv.fiber.datos.local.base_datos.Favorito
import com.iptv.fiber.datos.local.base_datos.SeguirViendo
import com.iptv.fiber.datos.modelo.Canal
import com.iptv.fiber.datos.modelo.Categoria
import com.iptv.fiber.datos.modelo.EPG
import com.iptv.fiber.datos.modelo.Pelicula
import com.iptv.fiber.datos.modelo.Radio
import com.iptv.fiber.datos.modelo.Serie
import com.iptv.fiber.datos.repositorio.RepositorioContenido
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel que expone el estado de contenido (canales, favoritos, historial, etc.) a la interfaz.
 */
class ModeloVistaContenido(private val repositorioContenido: RepositorioContenido) : ViewModel() {

    // ─── TV en Vivo ──────────────────────────────────────────────────────────
    private val _categoriasEnVivo = MutableStateFlow<List<Categoria>>(emptyList())
    val categoriasEnVivo: StateFlow<List<Categoria>> = _categoriasEnVivo.asStateFlow()

    private var _todosCanalesEnVivo: List<Canal> = emptyList()
    private val _canalesEnVivo = MutableStateFlow<List<Canal>>(emptyList())
    val canalesEnVivo: StateFlow<List<Canal>> = _canalesEnVivo.asStateFlow()

    private val _busqueda = MutableStateFlow("")
    val consultaBusqueda: StateFlow<String> = _busqueda.asStateFlow()

    private val _idCategoriaSeleccionada = MutableStateFlow<String?>(null)
    val idCategoriaSeleccionada: StateFlow<String?> = _idCategoriaSeleccionada.asStateFlow()

    private val _canalesFiltrados = MutableStateFlow<List<Canal>>(emptyList())
    val canalesFiltrados: StateFlow<List<Canal>> = _canalesFiltrados.asStateFlow()

    // Datos del Panel Principal
    private val _canalesTop = MutableStateFlow<List<Canal>>(emptyList())
    val canalesTop: StateFlow<List<Canal>> = _canalesTop.asStateFlow()

    private val _categoriasPanelPrincipal = MutableStateFlow<List<Pair<Categoria, List<Canal>>>>(emptyList())
    val categoriasPanelPrincipal: StateFlow<List<Pair<Categoria, List<Canal>>>> = _categoriasPanelPrincipal.asStateFlow()

    private val _historialReciente = MutableStateFlow<List<SeguirViendo>>(emptyList())
    val historialReciente: StateFlow<List<SeguirViendo>> = _historialReciente.asStateFlow()

    // ─── Películas ───────────────────────────────────────────────────────────

    private val _categoriasPeliculas = MutableStateFlow<List<Categoria>>(emptyList())
    val categoriasPeliculas: StateFlow<List<Categoria>> = _categoriasPeliculas.asStateFlow()

    private val _peliculas = MutableStateFlow<List<Pelicula>>(emptyList())
    val peliculas: StateFlow<List<Pelicula>> = _peliculas.asStateFlow()

    // ─── Series ──────────────────────────────────────────────────────────────

    private val _categoriasSeries = MutableStateFlow<List<Categoria>>(emptyList())
    val categoriasSeries: StateFlow<List<Categoria>> = _categoriasSeries.asStateFlow()

    private val _series = MutableStateFlow<List<Serie>>(emptyList())
    val series: StateFlow<List<Serie>> = _series.asStateFlow()

    // ─── Radio ───────────────────────────────────────────────────────────────

    private val _categoriasRadio = MutableStateFlow<List<Categoria>>(emptyList())
    val categoriasRadio: StateFlow<List<Categoria>> = _categoriasRadio.asStateFlow()

    private val _canalesRadio = MutableStateFlow<List<Radio>>(emptyList())
    val estacionesRadio: StateFlow<List<Radio>> = _canalesRadio.asStateFlow()

    // ─── Guía de Programación ────────────────────────────────────────────────

    private val _guiaProgramacion = MutableStateFlow<Map<String, List<EPG>>>(emptyMap())
    val epg: StateFlow<Map<String, List<EPG>>> = _guiaProgramacion.asStateFlow()

    // ─── Estado general ──────────────────────────────────────────────────────

    private val _cargando = MutableStateFlow(false)
    val estaCargando: StateFlow<Boolean> = _cargando.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    // ─── Favoritos e Historial ───────────────────────────────────────────────

    private val _favoritos = MutableStateFlow<List<Favorito>>(emptyList())
    val favoritos: StateFlow<List<Favorito>> = _favoritos.asStateFlow()

    private val _historial = MutableStateFlow<List<SeguirViendo>>(emptyList())
    val historial: StateFlow<List<SeguirViendo>> = _historial.asStateFlow()

    // ─── Funciones de carga ──────────────────────────────────────────────────

    fun cargarCategoriasEnVivo() {
        viewModelScope.launch {
            _cargando.value = true
            _error.value = null
            repositorioContenido
                    .obtenerCategoriasEnVivo()
                    .onSuccess {
                        _categoriasEnVivo.value = it
                        if (_todosCanalesEnVivo.isNotEmpty()) {
                            generarDatosPanelPrincipal()
                        }
                        _cargando.value = false
                    }
                    .onFailure {
                        _error.value = it.message ?: "Error al cargar categorías"
                        _cargando.value = false
                    }
        }
    }

    fun cargarCanalesEnVivo(idCategoria: String? = null) {
        viewModelScope.launch {
            _cargando.value = true
            _error.value = null
            _busqueda.value = "" // Reiniciar búsqueda al cargar nueva categoría
            repositorioContenido
                    .obtenerCanalesEnVivo(idCategoria)
                    .onSuccess { canales ->
                        _todosCanalesEnVivo = canales
                        _canalesEnVivo.value = canales
                        establecerContextoReproduccion(canales)
                        actualizarFiltrado()
                        if (_categoriasEnVivo.value.isNotEmpty()) {
                            generarDatosPanelPrincipal()
                        }
                        _cargando.value = false
                    }
                    .onFailure {
                        _error.value = it.message ?: "Error al cargar canales"
                        _cargando.value = false
                    }
        }
    }

    fun buscar(consulta: String) {
        _busqueda.value = consulta
        actualizarFiltrado()
    }

    fun seleccionarCategoria(id: String?) {
        _idCategoriaSeleccionada.value = id
        actualizarFiltrado()
    }

    private fun actualizarFiltrado() {
        val consulta = _busqueda.value.lowercase()
        val idCat = _idCategoriaSeleccionada.value
        
        val filtrados = _todosCanalesEnVivo.filter { canal ->
            val coincideBusqueda = consulta.isEmpty() || canal.nombre.lowercase().contains(consulta)
            val coincideCategoria = idCat == null || canal.id_categoria == idCat
            coincideBusqueda && coincideCategoria
        }
        _canalesFiltrados.value = filtrados
        _canalesEnVivo.value = filtrados
    }

    private fun generarDatosPanelPrincipal() {
        viewModelScope.launch {
            _canalesTop.value = _todosCanalesEnVivo.take(15)
            val categoriasPanel = mutableListOf<Pair<Categoria, List<Canal>>>()
            _categoriasEnVivo.value.take(8).forEach { cat ->
                val canalesDeCat = _todosCanalesEnVivo.filter { it.id_categoria == cat.id_categoria }.take(10)
                if (canalesDeCat.isNotEmpty()) categoriasPanel.add(cat to canalesDeCat)
            }
            _categoriasPanelPrincipal.value = categoriasPanel
        }
    }

    fun cargarCategoriasPeliculas() {
        viewModelScope.launch {
            _cargando.value = true
            repositorioContenido.obtenerCategoriasPeliculas().collect { resultado ->
                resultado
                        .onSuccess {
                            _categoriasPeliculas.value = it
                            _cargando.value = false
                        }
                        .onFailure {
                            _error.value = it.message
                            _cargando.value = false
                        }
            }
        }
    }

    fun cargarPeliculas(idCategoria: String? = null) {
        viewModelScope.launch {
            _cargando.value = true
            repositorioContenido.obtenerPeliculas(idCategoria).collect { resultado ->
                resultado
                        .onSuccess {
                            _peliculas.value = it
                            _cargando.value = false
                        }
                        .onFailure {
                            _error.value = it.message
                            _cargando.value = false
                        }
            }
        }
    }

    fun cargarCategoriasSeries() {
        viewModelScope.launch {
            _cargando.value = true
            repositorioContenido.obtenerCategoriasSeries().collect { resultado ->
                resultado
                        .onSuccess {
                            _categoriasSeries.value = it
                            _cargando.value = false
                        }
                        .onFailure {
                            _error.value = it.message
                            _cargando.value = false
                        }
            }
        }
    }

    fun cargarSeries(idCategoria: String? = null) {
        viewModelScope.launch {
            _cargando.value = true
            repositorioContenido.obtenerSeries(idCategoria).collect { resultado ->
                resultado
                        .onSuccess {
                            _series.value = it
                            _cargando.value = false
                        }
                        .onFailure {
                            _error.value = it.message
                            _cargando.value = false
                        }
            }
        }
    }

    fun cargarCategoriasRadio() {
        viewModelScope.launch {
            _cargando.value = true
            repositorioContenido.obtenerCategoriasRadio().collect { resultado ->
                resultado
                        .onSuccess {
                            _categoriasRadio.value = it
                            _cargando.value = false
                        }
                        .onFailure {
                            _error.value = it.message
                            _cargando.value = false
                        }
            }
        }
    }

    fun cargarEstacionesRadio(idCategoria: String? = null) {
        viewModelScope.launch {
            _cargando.value = true
            repositorioContenido.obtenerCanalesRadio(idCategoria).collect { resultado ->
                resultado
                        .onSuccess {
                            _canalesRadio.value = it
                            _cargando.value = false
                        }
                        .onFailure {
                            _error.value = it.message
                            _cargando.value = false
                        }
            }
        }
    }

    fun cargarEPG(idStream: Int? = null) {
        viewModelScope.launch {
            repositorioContenido.obtenerGuiaProgramacion(idStream).collect { resultado ->
                resultado.onSuccess { _guiaProgramacion.value = it }
                // El fallo de la guía no es crítico, se ignora silenciosamente
            }
        }
    }

    // ─── Favoritos e Historial ───────────────────────────────────────────────

    fun cargarFavoritos(tipo: String? = null) {
        viewModelScope.launch {
            repositorioContenido.obtenerFavoritos(tipo).collect { _favoritos.value = it }
        }
    }

    fun alternarFavorito(canal: Canal) {
        viewModelScope.launch { repositorioContenido.alternarFavorito(canal) }
    }

    fun cargarHistorial() {
        viewModelScope.launch {
            repositorioContenido.obtenerHistorial().collect { _historial.value = it }
        }
    }

    fun agregarAlHistorial(canal: Canal) {
        viewModelScope.launch { repositorioContenido.agregarAlHistorial(canal) }
    }

    /** Inicia la observación continua de favoritos e historial desde la base de datos. */
    fun iniciarObservacionDatosUsuario() {
        viewModelScope.launch {
            launch { 
                repositorioContenido.obtenerFavoritos().collect { 
                    _favoritos.value = it 
                } 
            }
            launch { 
                repositorioContenido.obtenerHistorial().collect { 
                    _historial.value = it
                    _historialReciente.value = it.take(10)
                } 
            }
        }
    }

    // ─── Utilidades ──────────────────────────────────────────────────────────

    fun limpiarError() {
        _error.value = null
    }

    fun establecerContextoReproduccion(canales: List<Canal>) {
        RepositorioContenido.establecerContextoReproduccion(canales)
    }

    fun obtenerCanalCompleto(idStream: Int, canalBase: Canal): Canal =
            RepositorioContenido.obtenerCanalCompleto(idStream, canalBase)

    fun limpiarHistorial() {
        viewModelScope.launch {
            repositorioContenido.limpiarHistorial()
            _historial.value = emptyList()
        }
    }

    fun limpiarCache() {
        RepositorioContenido.limpiarCache()
        _todosCanalesEnVivo = emptyList()
        _canalesEnVivo.value = emptyList()
        _peliculas.value = emptyList()
        _series.value = emptyList()
        _canalesRadio.value = emptyList()
    }
}
