package com.iptv.fiber.interfaz.modelovista

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.iptv.fiber.datos.local.base_datos.Favorito
import com.iptv.fiber.datos.local.base_datos.SeguirViendo
import com.iptv.fiber.datos.modelo.Canal
import com.iptv.fiber.datos.modelo.Categoria
import com.iptv.fiber.datos.modelo.EPG
import com.iptv.fiber.datos.repositorio.RepositorioContenido
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** 
 * EL CEREBRO DEL CONTENIDO (Canales, Películas, Historial, Favoritos).
 * Este ViewModel se encarga de pedirle los datos al RepositorioContenido 
 * y exponerlos en variables "StateFlow" que la pantalla puede observar.
 * Cuando el StateFlow cambia, la pantalla se redibuja automáticamente.
 */
class ModeloVistaContenido(private val repositorioContenido: RepositorioContenido) : ViewModel() {

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

    private val _canalesPrincipales = MutableStateFlow<List<Canal>>(emptyList())
    val canalesPrincipales: StateFlow<List<Canal>> = _canalesPrincipales.asStateFlow()

    private val _categoriasPanelPrincipal =
            MutableStateFlow<List<Pair<Categoria, List<Canal>>>>(emptyList())
    val categoriasPanelPrincipal: StateFlow<List<Pair<Categoria, List<Canal>>>> =
            _categoriasPanelPrincipal.asStateFlow()

    private val _historialReciente = MutableStateFlow<List<SeguirViendo>>(emptyList())
    val historialReciente: StateFlow<List<SeguirViendo>> = _historialReciente.asStateFlow()

    private val _guiaProgramacion = MutableStateFlow<Map<String, List<EPG>>>(emptyMap())
    val epg: StateFlow<Map<String, List<EPG>>> = _guiaProgramacion.asStateFlow()

    private val _cargando = MutableStateFlow(false)
    val estaCargando: StateFlow<Boolean> = _cargando.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private val _favoritos = MutableStateFlow<List<Favorito>>(emptyList())
    val favoritos: StateFlow<List<Favorito>> = _favoritos.asStateFlow()

    private val _historial = MutableStateFlow<List<SeguirViendo>>(emptyList())
    val historial: StateFlow<List<SeguirViendo>> = _historial.asStateFlow()

    /** Solicita las categorías de TV en vivo al repositorio y las publica en [categoriasEnVivo]. Omite la petición si ya hay datos y [forzarRecarga] es false. */
    fun cargarCategoriasEnVivo(forzarRecarga: Boolean = false) {
        if (!forzarRecarga && _categoriasEnVivo.value.isNotEmpty()) return
        
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

    /**
     * INICIO SÚPER RÁPIDO:
     * Inicia la carga completa de categorías y canales en PARALELO.
     * En lugar de pedir las categorías, esperar, y luego pedir los canales,
     * lanza las dos peticiones de red al mismo tiempo (usando 'async').
     * Esto reduce el tiempo de arranque a la mitad.
     */
    fun iniciarCargaCompleta(forzarRecarga: Boolean = false) {
        if (!forzarRecarga && _todosCanalesEnVivo.isNotEmpty() && _categoriasEnVivo.value.isNotEmpty()) return

        viewModelScope.launch { // Corrutina: se ejecuta en segundo plano
            _cargando.value = true // Le avisa a la UI que muestre la rueda de carga
            _error.value = null
            try {
                coroutineScope {
                    // Las dos peticiones de red corren en paralelo
                    val jobCategorias = async(Dispatchers.Default) {
                        repositorioContenido.obtenerCategoriasEnVivo()
                    }
                    val jobCanales = async(Dispatchers.Default) {
                        repositorioContenido.obtenerCanalesEnVivo()
                    }

                    // Esperar a ambas a la vez
                    val resultCategorias = jobCategorias.await()
                    val resultCanales = jobCanales.await()

                    resultCategorias.onSuccess { _categoriasEnVivo.value = it }
                        .onFailure { _error.value = it.message ?: "Error al cargar categorías" }

                    resultCanales.onSuccess { canales ->
                        _todosCanalesEnVivo = canales
                        _canalesEnVivo.value = canales
                        establecerContextoReproduccion(canales)
                        actualizarFiltrado()
                    }.onFailure { _error.value = it.message ?: "Error al cargar canales" }

                    // Armar el panel solo si ambas descargas fueron exitosas
                    if (_categoriasEnVivo.value.isNotEmpty() && _todosCanalesEnVivo.isNotEmpty()) {
                        generarDatosPanelPrincipal()
                    }
                }
            } finally {
                _cargando.value = false
            }
        }
    }

    /** Carga los canales en vivo, opcionalmente filtrados por [idCategoria]. Reutiliza los datos en memoria si ya existen y [forzarRecarga] es false. */
    fun cargarCanalesEnVivo(idCategoria: String? = null, forzarRecarga: Boolean = false) {
        if (!forzarRecarga && _todosCanalesEnVivo.isNotEmpty() && idCategoria == null) {
            _canalesEnVivo.value = _todosCanalesEnVivo
            return
        }

        viewModelScope.launch {
            _cargando.value = true
            _error.value = null
            _busqueda.value = ""
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

    private var jobBusqueda: kotlinx.coroutines.Job? = null

    /**
     * MOTOR DE BÚSQUEDA CON "DEBOUNCE":
     * Filtra la lista de canales. El "debounce" de 300ms significa que si el usuario
     * escribe "H", "B", "O" muy rápido, no se filtra 3 veces (lo cual trabaría el celular).
     * El sistema espera 300 milisegundos de pausa antes de hacer el filtrado real.
     */
    fun buscar(consulta: String) {
        _busqueda.value = consulta // Actualiza el texto en la caja de búsqueda
        jobBusqueda?.cancel() // Si había una búsqueda pendiente (por la letra anterior), la cancela
        jobBusqueda = viewModelScope.launch {
            kotlinx.coroutines.delay(300) // Pausa mágica de 300ms
            actualizarFiltrado() // Aplica el filtro real
        }
    }

    /**
     * Selecciona una categoría y aplica el filtrado en Dispatchers.Default
     * para no bloquear el hilo principal.
     */
    fun seleccionarCategoria(id: String?) {
        _idCategoriaSeleccionada.value = id
        viewModelScope.launch {
            actualizarFiltrado()
        }
    }

    /** Aplica el filtro actual (categoría + texto de búsqueda) sobre la caché del repositorio en el hilo Default. */
    private suspend fun actualizarFiltrado() = withContext(Dispatchers.Default) {
        val consulta = _busqueda.value
        val idCat = _idCategoriaSeleccionada.value
        val filtrados = RepositorioContenido.filtrarCanales(idCat, consulta)
        _canalesFiltrados.value = filtrados
        _canalesEnVivo.value = filtrados
    }

    private var panelGeneradoPorPrimeraVez = false
    private var jobPanelPrincipal: kotlinx.coroutines.Job? = null

    /**
     * EL ALGORITMO DEL DASHBOARD INICIAL:
     * Construye la lista de canales recomendados y el panel de categorías para la pantalla principal.
     * Prioriza mostrar:
     * 1. Canales Favoritos
     * 2. Canales del Historial (que no sean favoritos)
     * 3. Rellena el resto con canales al azar hasta tener 20 recomendaciones.
     */
    private fun generarDatosPanelPrincipal() {
        val debeOmitirDebounce = !panelGeneradoPorPrimeraVez
        jobPanelPrincipal?.cancel()
        jobPanelPrincipal = viewModelScope.launch(Dispatchers.Default) {
            // Debounce en recargas subsecuentes (ej. cambio de favoritos).
            // 350 ms: si el usuario agrega/quita varios favoritos seguidos, solo se
            // regenera el panel UNA vez al final, evitando recomposiciones masivas.
            if (!debeOmitirDebounce) {
                kotlinx.coroutines.delay(350)
            }
            panelGeneradoPorPrimeraVez = true
            
            // Obtenemos los IDs de los canales favoritos e historial
            val favIds = _favoritos.value.map { it.idTransmision }.toSet()
            val histIds = _historial.value.map { it.idTransmision }.toSet()

            // Optimización: Usar secuencia para no iterar toda la lista si ya encontramos los 20 requeridos.
            val recomendados = mutableListOf<Canal>()

            // 1. Agregamos Favoritos (hasta 20)
            recomendados.addAll(_todosCanalesEnVivo.asSequence().filter { it.id_transmision in favIds }.take(20))

            // 2. Si faltan para llegar a 20, agregamos del Historial
            if (recomendados.size < 20) {
                recomendados.addAll(_todosCanalesEnVivo.asSequence().filter { it.id_transmision in histIds && it.id_transmision !in favIds }.take(20 - recomendados.size))
            }

            // 3. Si aún faltan para llegar a 20, agregamos cualquier canal normal
            if (recomendados.size < 20) {
                recomendados.addAll(_todosCanalesEnVivo.asSequence().filter { it.id_transmision !in favIds && it.id_transmision !in histIds }.take(20 - recomendados.size))
            }

            _canalesPrincipales.value = recomendados

            val categoriasPanel = mutableListOf<Pair<Categoria, List<Canal>>>()
            // Hasta 20 categorías × 20 canales. Las filas (LazyColumn) y los canales (LazyRow)
            // están virtualizados: solo se renderiza lo visible, así que más datos no aumentan la RAM.
            // La TV recorta a 15 y el celular muestra hasta 20 (tiene más capacidad).
            val mapaCategoriasRepo = RepositorioContenido.obtenerCanalesPorCategoria()
            _categoriasEnVivo.value.take(20).forEach { categoria ->
                val canalesDeCategoria =
                        mapaCategoriasRepo[categoria.id_categoria]?.take(20).orEmpty()
                if (canalesDeCategoria.isNotEmpty()) {
                    categoriasPanel.add(categoria to canalesDeCategoria)
                }
            }
            _categoriasPanelPrincipal.value = categoriasPanel
        }
    }

    /** Solicita la guía de programación (EPG) para [idTransmision] o para todos los canales si es null, y la publica en [epg]. */
    fun cargarEPG(idTransmision: Int? = null) {
        viewModelScope.launch {
            repositorioContenido.obtenerGuiaProgramacion(idTransmision).collect { resultado ->
                resultado.onSuccess { _guiaProgramacion.value = it }
            }
        }
    }

    /** Observa los favoritos del servidor activo desde Room y los publica en [favoritos], filtrado por [tipo] si se indica. */
    fun cargarFavoritos(tipo: String? = null) {
        viewModelScope.launch {
            repositorioContenido.obtenerFavoritos(tipo).collect { _favoritos.value = it }
        }
    }

    /** Agrega o quita [canal] de favoritos en Room; la UI se actualiza automáticamente via el Flow de favoritos. */
    fun alternarFavorito(canal: Canal) {
        viewModelScope.launch { repositorioContenido.alternarFavorito(canal) }
    }

    /** Observa el historial de reproducción desde Room y lo publica en [historial].
     *  Si [iniciarObservacionDatosUsuario] ya está activo, delega en él para evitar colectores duplicados. */
    fun cargarHistorial() {
        if (observacionIniciada) return
        viewModelScope.launch {
            repositorioContenido.obtenerHistorial().collect { _historial.value = it }
        }
    }

    /** Registra [canal] en el historial de reproducción (llamado al iniciar reproducción). */
    fun agregarAlHistorial(canal: Canal) {
        viewModelScope.launch { repositorioContenido.agregarAlHistorial(canal) }
    }

    private var observacionIniciada = false

    /**
     * Inicia la observación continua de favoritos e historial desde Room.
     * Solo se ejecuta una vez por ciclo de vida del ViewModel; cuando cambian los datos,
     * regenera automáticamente los canales recomendados del panel principal.
     */
    fun iniciarObservacionDatosUsuario() {
        if (observacionIniciada) return
        observacionIniciada = true
        
        viewModelScope.launch {
            launch {
                repositorioContenido.obtenerFavoritos().collect {
                    _favoritos.value = it
                    // Actualizar dinámicamente los recomendados si los canales ya se cargaron
                    if (_todosCanalesEnVivo.isNotEmpty()) {
                        generarDatosPanelPrincipal()
                    }
                }
            }
            launch {
                repositorioContenido.obtenerHistorial().collect {
                    _historial.value = it
                    _historialReciente.value = it.take(10)
                    // Actualizar dinámicamente los recomendados si los canales ya se cargaron
                    if (_todosCanalesEnVivo.isNotEmpty()) {
                        generarDatosPanelPrincipal()
                    }
                }
            }
        }
    }

    /** Borra el mensaje de error actual para que la UI deje de mostrarlo. */
    fun limpiarError() {
        _error.value = null
    }

    /** Informa al repositorio qué lista de [canales] está activa en el reproductor para habilitar navegación prev/next. */
    fun establecerContextoReproduccion(canales: List<Canal>) {
        RepositorioContenido.establecerContextoReproduccion(canales)
    }

    /** Busca el canal enriquecido (con todos sus campos) por [idTransmision]; devuelve [canalBase] como respaldo si no está en caché. */
    fun obtenerCanalCompleto(idTransmision: Int, canalBase: Canal): Canal =
            RepositorioContenido.obtenerCanalCompleto(idTransmision, canalBase)

    /** Elimina todo el historial del servidor activo en Room y limpia el StateFlow [historial]. */
    fun limpiarHistorial() {
        viewModelScope.launch {
            repositorioContenido.limpiarHistorial()
            _historial.value = emptyList()
        }
    }

    /** Vacía la caché del repositorio y resetea las listas de canales en el ViewModel. */
    fun limpiarCache() {
        RepositorioContenido.limpiarCache()
        _todosCanalesEnVivo = emptyList()
        _canalesEnVivo.value = emptyList()
    }

    /**
     * Reinicia completamente el estado del ViewModel al cambiar de servidor o lista.
     * Limpia tanto el caché de canales como todos los estados observables de la UI
     * para que no se muestren datos de la sesión anterior.
     */
    fun reiniciarEstado() {
        // Cancelar cualquier trabajo pendiente
        jobBusqueda?.cancel()
        jobPanelPrincipal?.cancel()

        // Limpiar caché en el Repositorio (singleton)
        RepositorioContenido.limpiarCache()

        // Limpiar estados internos del ViewModel
        _todosCanalesEnVivo = emptyList()

        // Limpiar todos los StateFlows observables para que la UI quede en blanco
        _canalesEnVivo.value = emptyList()
        _canalesFiltrados.value = emptyList()
        _categoriasEnVivo.value = emptyList()
        _canalesPrincipales.value = emptyList()
        _categoriasPanelPrincipal.value = emptyList()
        _historialReciente.value = emptyList()
        _historial.value = emptyList()
        _favoritos.value = emptyList()
        _guiaProgramacion.value = emptyMap()
        _busqueda.value = ""
        _idCategoriaSeleccionada.value = null
        _error.value = null

        // Resetear banderas internas para permitir recarga completa
        panelGeneradoPorPrimeraVez = false
        observacionIniciada = false
    }
}
