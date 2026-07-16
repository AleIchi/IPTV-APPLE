package com.iptv.fiber.tv.pantallas

import com.iptv.fiber.tv.dialogos.DialogoPinTV
// removed android import: import android.content.Intent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.iptv.fiber.datos.modelo.Canal
import com.iptv.fiber.datos.modelo.Categoria
import com.iptv.fiber.datos.local.GestorPreferencias
import com.iptv.fiber.datos.repositorio.RepositorioAutenticacion
import com.iptv.fiber.interfaz.modelovista.ModeloVistaContenido
import com.iptv.fiber.interfaz.reproductor.ActividadReproductor
import com.iptv.fiber.interfaz.reproductor.ClavesReproductor
import com.iptv.fiber.tv.componentes.esContenidoAdultoTV
import com.iptv.fiber.tv.componentes.TemaTV
import com.iptv.fiber.tv.componentes.EncabezadoTvEnVivo
import com.iptv.fiber.tv.componentes.ChipCategoriaTV
import com.iptv.fiber.tv.componentes.EstadoPantallaTV
import com.iptv.fiber.tv.componentes.FilaCanalTV
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.coroutines.launch

import androidx.compose.runtime.DisposableEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.iptv.fiber.tv.componentes.GestorReproductorCompartido
import com.iptv.fiber.tv.componentes.tvClickableWithLongClick
import androidx.compose.foundation.focusGroup
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.type
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf

/** Pantalla TV en Vivo con vista dividida: lista de categorías/canales a la izquierda y mini-reproductor a la derecha. */
@kotlin.OptIn(
    androidx.compose.foundation.ExperimentalFoundationApi::class,
    androidx.compose.ui.ExperimentalComposeUiApi::class
)
@Composable
fun TvEnVivoTV(
    modeloVista: ModeloVistaContenido,
    repositorioAuth: RepositorioAutenticacion? = null,
    requeridorFocoContenido: FocusRequester = remember { FocusRequester() }
) {
    val categorias by modeloVista.categoriasEnVivo.collectAsStateWithLifecycle()
    val canales by modeloVista.canalesFiltrados.collectAsStateWithLifecycle()
    val cargando by modeloVista.estaCargando.collectAsStateWithLifecycle()
    val error by modeloVista.error.collectAsStateWithLifecycle()
    val categoriaSeleccionada by modeloVista.idCategoriaSeleccionada.collectAsStateWithLifecycle()
    val consultaBusqueda by modeloVista.consultaBusqueda.collectAsStateWithLifecycle()
    val favoritos by modeloVista.favoritos.collectAsStateWithLifecycle()
    val favoritosIds = remember(favoritos) { favoritos.map { it.idTransmision }.toSet() }
    val contexto = LocalContext.current
    val fuenteInteraccionBusqueda = remember { MutableInteractionSource() }
    val busquedaEnFoco by fuenteInteraccionBusqueda.collectIsFocusedAsState()
    val alcance = rememberCoroutineScope()
    val gestorPreferencias = remember { GestorPreferencias(contexto) }
    val controlParentalActivo by gestorPreferencias.controlParentalActivo.collectAsStateWithLifecycle(initialValue = false)
    val pinParental by gestorPreferencias.pinParental.collectAsStateWithLifecycle(initialValue = "")
    var canalEnVistaPrevia by remember { mutableStateOf<String?>(null) }
    var canalActivoVistaPrevia by remember { mutableStateOf<String?>(null) }
    // Objeto Canal completo del canal activo — sobrevive al cambio de filtro/búsqueda
    // para que el player no cambie al primer resultado cuando el usuario busca.
    var canalActivoObjeto by remember { mutableStateOf<Canal?>(null) }
    var canalPendientePin by remember { mutableStateOf<Canal?>(null) }
    val canalesPorId = remember(canales) { canales.associateBy { it.id_transmision.toString() } }
    val nombreCategoriaSeleccionada = remember(categorias, categoriaSeleccionada) {
        categorias.firstOrNull { it.id_categoria == categoriaSeleccionada }?.nombre_categoria
    }
    
    val estadoLista = rememberLazyListState()
    var ultimaNavegacion by remember { mutableLongStateOf(0L) }
    val idAEnfocar = remember { mutableStateOf<String?>(null) }
    // FocusRequester compartido que siempre apunta al chip de categoría seleccionado
    val requeridorCategoriaSeleccionada = remember { FocusRequester() }
    var categoriaFilaEnFoco by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        modeloVista.iniciarCargaCompleta()
        modeloVista.iniciarObservacionDatosUsuario()
        
        // Delay de robustez para permitir que el LazyColumn se dibuje
        kotlinx.coroutines.delay(200)
        try { requeridorFocoContenido.requestFocus() } catch (_: Exception) {}
    }

    LaunchedEffect(canales.firstOrNull()?.id_transmision) {
        val primerCanal = canales.firstOrNull()?.id_transmision?.toString()
        if (canalEnVistaPrevia == null && canalActivoVistaPrevia == null) {
            canalActivoVistaPrevia = primerCanal
            canalActivoObjeto = canales.firstOrNull()
        }
    }

    LaunchedEffect(canalEnVistaPrevia) {
        val idSeleccionado = canalEnVistaPrevia ?: return@LaunchedEffect
        kotlinx.coroutines.delay(450)
        canalActivoVistaPrevia = idSeleccionado
    }

    val propietarioCicloVida = androidx.compose.ui.platform.LocalLifecycleOwner.current
    DisposableEffect(propietarioCicloVida) {
        val observador = androidx.lifecycle.LifecycleEventObserver { _, event ->
            if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME) {
                    GestorReproductorCompartido.ultimoCanalReproducidoId?.let { id ->
                        canalEnVistaPrevia = id.toString()
                        canalActivoVistaPrevia = id.toString()
                        canalActivoObjeto = canales.firstOrNull { it.id_transmision == id }
                        val index = canales.indexOfFirst { it.id_transmision == id }
                        if (index >= 0) {
                        alcance.launch {
                            val visibles = estadoLista.layoutInfo.visibleItemsInfo.size.takeIf { it > 0 } ?: 6
                            estadoLista.scrollToItem((index - visibles / 2).coerceAtLeast(0))
                            idAEnfocar.value = id.toString()
                        }
                    }
                    GestorReproductorCompartido.ultimoCanalReproducidoId = null
                }
            }
        }
        propietarioCicloVida.lifecycle.addObserver(observador)
        
        onDispose {
            propietarioCicloVida.lifecycle.removeObserver(observador)
            // NO llamar liberar() aquí: el player compartido podría estar siendo usado por
            // MiniReproductorTV o por ActividadReproductor. La liberación se gestiona desde
            // ActividadTV.onDestroy() o al cerrar sesión, no desde el ciclo de vida del composable.
        }
    }

    val alternarFavorito: (Canal) -> Unit = { canal ->
        modeloVista.alternarFavorito(canal)
        val yaEsFavorito = favoritos.any { it.idTransmision == canal.id_transmision }
// TODO(KMP):         android.widget.Toast.makeText(
            contexto,
            if (yaEsFavorito) "Quitado de favoritos" else "Agregado a favoritos",
            android.widget.Toast.LENGTH_SHORT
        ).show()
    }

    val abrirCanal: (Canal) -> Unit = { canal ->
        val servidor = repositorioAuth?.servidorActual?.value
        if (servidor != null) {
            val urlTransmision = if (!canal.fuenteDirecta.isNullOrEmpty()) {
                canal.fuenteDirecta
            } else {
                repositorioAuth.construirUrlTransmision(
                    servidor.urlServidor,
                    servidor.usuario,
                    servidor.contrasena,
                    "live",
                    canal.id_transmision
                )
            }

// TODO(KMP):             val intent = Intent(contexto, ActividadReproductor::class.java).apply {
                putExtra(ClavesReproductor.URL_TRANSMISION, urlTransmision)
                putExtra(ClavesReproductor.ID_TRANSMISION, canal.id_transmision)
                putExtra(ClavesReproductor.TIPO_TRANSMISION, "live")
                putExtra(ClavesReproductor.NOMBRE_CANAL, canal.nombre)
                putExtra(ClavesReproductor.LOGOTIPO_CANAL, canal.icono_transmision)
                putExtra(ClavesReproductor.SERVIDOR_URL, servidor.urlServidor)
                putExtra(ClavesReproductor.USUARIO, servidor.usuario)
                putExtra(ClavesReproductor.CONTRASENA, servidor.contrasena)
            }
            alcance.launch {
                modeloVista.establecerContextoReproduccion(canales)
                modeloVista.agregarAlHistorial(canal)
            }
// TODO(KMP):             contexto.startActivity(intent)
        }
    }

    val reproducirCanal: (Canal) -> Unit = { canal ->
        if (controlParentalActivo && pinParental.isNotEmpty() && esContenidoAdultoTV(canal, categorias)) {
            canalPendientePin = canal
        } else {
            abrirCanal(canal)
        }
    }

    if (canalPendientePin != null) {
        DialogoPinTV(
            titulo = "Control parental",
            descripcion = "Ingresa tu PIN para reproducir este canal",
            pinCorrecto = pinParental,
            alConfirmar = {
                canalPendientePin?.let(abrirCanal)
                canalPendientePin = null
            },
            alCancelar = { canalPendientePin = null }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(TemaTV.FondoPrincipal)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = TemaTV.MargenPantalla, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f).padding(end = 16.dp)) {
                Text(
                    text = "TV en vivo",
                    color = TemaTV.TextoPrincipal,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.ExtraBold
                )
                Text(
                    text = nombreCategoriaSeleccionada ?: "Todas las categorías",
                    color = TemaTV.TextoSecundario,
                    fontSize = 14.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            var modoBusqueda by remember { mutableStateOf(false) }
            var yaTuvoFoco by remember { mutableStateOf(false) }
            val keyboardController = androidx.compose.ui.platform.LocalSoftwareKeyboardController.current
            val focusRequesterBusqueda = remember { FocusRequester() }

            Surface(
                modifier = Modifier
                    .weight(1.5f)
                    .height(44.dp)
                    .then(
                        if (!modoBusqueda) {
                            Modifier.tvClickableWithLongClick(
                                interactionSource = fuenteInteraccionBusqueda,
                                onClick = {
                                    modoBusqueda = true
                                }
                            )
                        } else {
                            Modifier
                        }
                    ),
                shape = RoundedCornerShape(TemaTV.RedondeoControl),
                color = if (busquedaEnFoco) TemaTV.Superficie.copy(alpha = 0.92f) else TemaTV.Superficie.copy(alpha = 0.62f),
                border = BorderStroke(if (busquedaEnFoco) 2.dp else 1.dp, if (busquedaEnFoco) TemaTV.AcentoClaro else TemaTV.Linea)
            ) {
                if (!modoBusqueda) {
                    Row(
                        modifier = Modifier.fillMaxSize().padding(horizontal = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Search, "Buscar", tint = TemaTV.AcentoClaro, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (consultaBusqueda.isEmpty()) "Buscar canal... (Presiona OK)" else consultaBusqueda,
                            color = if (consultaBusqueda.isEmpty()) Color.White.copy(alpha = 0.5f) else TemaTV.TextoPrincipal,
                            fontSize = TemaTV.TextoControl,
                            maxLines = 1
                        )
                    }
                } else {
                    LaunchedEffect(Unit) {
                        try { focusRequesterBusqueda.requestFocus() } catch (_: Exception) {}
                        keyboardController?.show()
                    }
                    androidx.compose.foundation.text.BasicTextField(
                        value = consultaBusqueda,
                        onValueChange = { modeloVista.buscar(it) },
                        textStyle = androidx.compose.ui.text.TextStyle(color = TemaTV.TextoPrincipal, fontSize = TemaTV.TextoControl),
                        singleLine = true,
                        cursorBrush = androidx.compose.ui.graphics.SolidColor(TemaTV.AcentoClaro),
                        modifier = Modifier
                            .fillMaxSize()
                            .focusRequester(focusRequesterBusqueda)
                            .onKeyEvent { evento ->
                                if (evento.nativeKeyEvent.keyCode == android.view.KeyEvent.KEYCODE_BACK) {
                                    modoBusqueda = false
                                    yaTuvoFoco = false
                                    keyboardController?.hide()
                                    true
                                } else if (evento.nativeKeyEvent.keyCode == android.view.KeyEvent.KEYCODE_DPAD_DOWN) {
                                    modoBusqueda = false
                                    yaTuvoFoco = false
                                    keyboardController?.hide()
                                    false
                                } else {
                                    false
                                }
                            }
                            .onFocusChanged { estadoFoco ->
                                if (estadoFoco.isFocused) {
                                    yaTuvoFoco = true
                                } else if (yaTuvoFoco) {
                                    modoBusqueda = false
                                    yaTuvoFoco = false
                                    keyboardController?.hide()
                                }
                            },
                        decorationBox = { innerTextField ->
                            Row(
                                modifier = Modifier.fillMaxSize().padding(horizontal = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Search, "Buscar", tint = TemaTV.AcentoClaro, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
                                    innerTextField()
                                }
                            }
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            Surface(
                shape = RoundedCornerShape(999.dp),
                color = TemaTV.Superficie.copy(alpha = 0.86f),
                border = BorderStroke(1.dp, TemaTV.Linea)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Default.Tv, null, tint = TemaTV.AcentoClaro, modifier = Modifier.size(16.dp))
                    Text(
                        text = "${canales.size} canales",
                        color = TemaTV.TextoPrincipal,
                        fontSize = TemaTV.TextoControl,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }

        LazyRow(
            modifier = Modifier
                .padding(start = TemaTV.MargenPantalla)
                .fillMaxWidth()
                .padding(bottom = 10.dp)
                .onFocusChanged { focusState ->
                    if (focusState.hasFocus && !categoriaFilaEnFoco) {
                        // Foco entra al row desde fuera: redirigir al chip seleccionado
                        categoriaFilaEnFoco = true
                        alcance.launch {
                            try { requeridorCategoriaSeleccionada.requestFocus() } catch (_: Exception) {}
                        }
                    } else if (!focusState.hasFocus) {
                        categoriaFilaEnFoco = false
                    }
                }
                .focusGroup(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            contentPadding = PaddingValues(end = TemaTV.MargenPantalla)
        ) {
            item {
                ChipCategoriaTV(
                    nombre = "Todas",
                    esSeleccionado = categoriaSeleccionada == null,
                    alSeleccionar = { modeloVista.seleccionarCategoria(null) },
                    focusRequester = if (categoriaSeleccionada == null) requeridorCategoriaSeleccionada else null
                )
            }
            items(items = categorias, key = { it.id_categoria }) { categoria: Categoria ->
                ChipCategoriaTV(
                    nombre = categoria.nombre_categoria,
                    esSeleccionado = categoria.id_categoria == categoriaSeleccionada,
                    alSeleccionar = { modeloVista.seleccionarCategoria(categoria.id_categoria) },
                    focusRequester = if (categoria.id_categoria == categoriaSeleccionada) requeridorCategoriaSeleccionada else null
                )
            }
        }

        when {
            cargando && canales.isEmpty() -> {
                EstadoPantallaTV(
                    titulo = "Sincronizando...",
                    subtitulo = "Preparando contenido en vivo",
                    requeridorFocoContenido = requeridorFocoContenido
                )
            }
            error != null && canales.isEmpty() -> {
                EstadoPantallaTV(
                    titulo = "No se pudo cargar",
                    subtitulo = error ?: "Error de red",
                    mostrarAccion = true,
                    textoAccion = "Reintentar",
                    alAccion = {
                        modeloVista.cargarCategoriasEnVivo()
                        modeloVista.cargarCanalesEnVivo()
                    },
                    requeridorFocoContenido = requeridorFocoContenido
                )
            }
            canales.isEmpty() -> {
                EstadoPantallaTV(
                    titulo = "Sin resultados",
                    subtitulo = "Prueba otra búsqueda o cambia la categoría",
                    requeridorFocoContenido = requeridorFocoContenido
                )
            }
            else -> {
                // ═══════════ PANTALLA DIVIDIDA (Lista a la Izq, Reproductor a la Der) ═══════════
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = TemaTV.MargenPantalla, vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(22.dp)
                ) {
                    // -- LADO IZQUIERDO: LISTA VERTICAL DE CANALES --
                    Column(
                        modifier = Modifier
                            .weight(0.35f)
                            .fillMaxHeight()
                            .clip(RoundedCornerShape(18.dp))
                            .background(Color(0xFF141523)) // Color sólido optimizado para GPU sin alpha blend
                            .padding(10.dp)
                    ) {
                        LaunchedEffect(canales) {
                            // Solo solicitar foco si el usuario NO está escribiendo en el buscador
                            // para evitar saltos de foco inesperados al teclear
                            if (canales.isNotEmpty() && !busquedaEnFoco && consultaBusqueda.isEmpty()) {
                                try { requeridorFocoContenido.requestFocus() } catch (_: Exception) {}
                            }
                        }
                        var indiceFocusado by remember { mutableIntStateOf(-1) }
                        LaunchedEffect(indiceFocusado) {
                            if (indiceFocusado >= 0) {
                                val visibles = estadoLista.layoutInfo.visibleItemsInfo.size.coerceAtLeast(6)
                                estadoLista.scrollToItem((indiceFocusado - visibles / 2).coerceAtLeast(0))
                            }
                        }
                        LazyColumn(
                            state = estadoLista,
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                            contentPadding = PaddingValues(bottom = 24.dp),
                            modifier = Modifier
                                .fillMaxSize()
                                .onKeyEvent { event ->
                                    if ((event.key == Key.DirectionDown || event.key == Key.DirectionUp)
                                        && event.type == KeyEventType.KeyDown) {
                                        val ahora = System.currentTimeMillis()
                                        if (ahora - ultimaNavegacion < 150L) return@onKeyEvent true
                                        ultimaNavegacion = ahora
                                        false
                                    } else false
                                }
                                .focusGroup()
                                .focusProperties {
                                exit = { direction: FocusDirection ->
                                    if (direction == FocusDirection.Up) {
                                        // Obtener información exacta de si el primer canal (índice 0) está renderizado
                                        val infoPrimerItem = estadoLista.layoutInfo.visibleItemsInfo.firstOrNull { it.index == 0 }
                                        
                                        // Si el canal 0 no está visible, o está visible pero cortado (offset negativo),
                                        // estamos en medio de un scroll y bloqueamos la salida del foco
                                        if (infoPrimerItem == null || infoPrimerItem.offset < 0) {
                                            FocusRequester.Cancel
                                        } else {
                                            FocusRequester.Default
                                        }
                                    } else if (direction == FocusDirection.Down) {
                                        val ultimoVisible = estadoLista.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
                                        if (ultimoVisible < canales.size - 1) {
                                            FocusRequester.Cancel
                                        } else {
                                            FocusRequester.Default
                                        }
                                    } else {
                                        FocusRequester.Default
                                    }
                                }
                            }
                        ) {
                            itemsIndexed(
                                items = canales,
                                key = { _, canal -> canal.id_transmision },
                                contentType = { _, _ -> "ItemCanalTV" }
                            ) { index, canal ->
                                val esFav = canal.id_transmision in favoritosIds
                                val estaEnVistaPrevia = canalEnVistaPrevia == canal.id_transmision.toString()
                                val esElCanalAEnfocar = idAEnfocar.value == canal.id_transmision.toString()
                                val localFocusRequester = remember { FocusRequester() }

                                LaunchedEffect(esElCanalAEnfocar) {
                                    if (esElCanalAEnfocar) {
                                        try { localFocusRequester.requestFocus() } catch (_: Exception) {}
                                        // NO hacemos idAEnfocar.value = null aquí para evitar
                                        // recomposición masiva e instantánea de toda la lista.
                                    }
                                }

                                // Determinar si este canal debe usar el FocusRequester principal de contenido
                                // (Se asocia al canal seleccionado en vista previa, o al canal 0 si no hay selección)
                                val debeUsarFocusRequesterContenido = estaEnVistaPrevia || (canalEnVistaPrevia == null && index == 0)

                                val clickCanal = remember(canal, estaEnVistaPrevia) {
                                    {
                                        if (estaEnVistaPrevia) {
                                            reproducirCanal(canal) // Doble toque: pantalla completa
                                        } else {
                                            canalEnVistaPrevia = canal.id_transmision.toString() // Primer toque: vista previa
                                            canalActivoObjeto = canal // Guardar el objeto antes de cualquier filtro
                                        }
                                    }
                                }
                                val longClickCanal = remember(canal) {
                                    {
                                        alternarFavorito(canal)
                                    }
                                }
                                
                                val modifierFoco = remember(index, esElCanalAEnfocar, debeUsarFocusRequesterContenido) {
                                    Modifier
                                        .onFocusChanged { focusState ->
                                            if (focusState.isFocused) {
                                                indiceFocusado = index
                                            }
                                        }
                                        .focusRequester(
                                            if (debeUsarFocusRequesterContenido && idAEnfocar.value == null) {
                                                requeridorFocoContenido
                                            } else {
                                                localFocusRequester
                                            }
                                        )
                                }

                                FilaCanalTV(
                                    canal = canal,
                                    esFavorito = esFav,
                                    esSeleccionado = estaEnVistaPrevia,
                                    modifier = modifierFoco,
                                    alHacerClick = clickCanal,
                                    alHacerLongClick = longClickCanal
                                )
                            }
                        }
                    }

                    // -- LADO DERECHO: MINI-REPRODUCTOR Y DETALLES --
                    Column(modifier = Modifier.weight(0.65f).fillMaxHeight()) {
                        val canalSeleccionado =
                            canalEnVistaPrevia?.let { canalesPorId[it] } ?: canales.firstOrNull()
                        // canalActivoObjeto tiene el objeto Canal guardado al momento de la selección,
                        // por lo que sobrevive a cambios de filtro o búsqueda sin cambiar el player.
                        val canalActual = canalActivoObjeto
                            ?: canalActivoVistaPrevia?.let { canalesPorId[it] }
                            ?: canalSeleccionado

                        // Contenedor del Reproductor
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .aspectRatio(16f / 9f)
                                .clip(RoundedCornerShape(22.dp))
                                .background(TemaTV.FondoElevado)
                                .border(1.dp, TemaTV.Linea, RoundedCornerShape(22.dp))
                        ) {
                            if (canalActual != null) {
                                val servidor = repositorioAuth?.servidorActual?.value
                                val urlTransmision = if (servidor != null) {
                                    if (!canalActual.fuenteDirecta.isNullOrEmpty()) canalActual.fuenteDirecta
                                    else repositorioAuth.construirUrlTransmision(servidor.urlServidor, servidor.usuario, servidor.contrasena, "live", canalActual.id_transmision)
                                } else null

                                val bloqueadoPorControlParental = controlParentalActivo &&
                                    pinParental.isNotEmpty() &&
                                    esContenidoAdultoTV(canalActual, categorias)

                                if (bloqueadoPorControlParental) {
                                    Column(
                                        modifier = Modifier.align(Alignment.Center),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Lock,
                                            contentDescription = null,
                                            tint = TemaTV.AcentoClaro,
                                            modifier = Modifier.size(42.dp)
                                        )
                                        Spacer(modifier = Modifier.height(10.dp))
                                        Text(
                                            text = "Canal protegido",
                                            color = TemaTV.TextoPrincipal,
                                            fontSize = 18.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = "Presiona Aceptar dos veces e ingresa tu PIN para reproducirlo",
                                            color = TemaTV.TextoSecundario,
                                            fontSize = 13.sp
                                        )
                                    }
                                } else if (urlTransmision != null) {
                                    com.iptv.fiber.tv.componentes.MiniReproductorTV(
                                        urlTransmision = urlTransmision,
                                        modifier = Modifier.fillMaxSize(),
                                        nombreCanal = canalActual.nombre
                                    )
                                } else {
                                    Text("Error al cargar la dirección", color = TemaTV.TextoPrincipal, modifier = Modifier.align(Alignment.Center))
                                }
                            } else {
                                // Estado vacío
                                Column(
                                    modifier = Modifier.align(Alignment.Center),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Tv,
                                        contentDescription = null,
                                        modifier = Modifier.size(64.dp),
                                        tint = TemaTV.TextoTenue
                                    )
                                    Spacer(modifier = Modifier.height(16.dp))
                                    Text(
                                        "Selecciona un canal para previsualizar",
                                        color = TemaTV.TextoSecundario,
                                        fontSize = 16.sp
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Detalles del Canal en Vista Previa
                        if (canalSeleccionado != null) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                if (!canalSeleccionado.icono_transmision.isNullOrBlank()) {
                                    val contextoLocal = androidx.compose.ui.platform.LocalContext.current
                                    coil.compose.AsyncImage(
                                        // size(96,96) limita la resolución que Coil descarga y guarda en caché.
                                        // Sin este límite, un logo de 4000×4000 px se cachearía entero
                                        // aunque se muestre en 48dp, agotando la RAM en TV Box de 1 GB.
                                        model = coil.request.ImageRequest.Builder(contextoLocal)
                                            .data(canalSeleccionado.icono_transmision)
                                            .size(96, 96)
                                            .build(),
                                        contentDescription = "Logotipo",
                                        modifier = Modifier
                                            .size(48.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(Color.Black.copy(alpha = 0.3f))
                                            .padding(4.dp)
                                    )
                                    Spacer(modifier = Modifier.width(16.dp))
                                }
                                Column {
                                    Text(
                                        text = canalSeleccionado.nombre,
                                        color = TemaTV.TextoPrincipal,
                                        fontSize = 22.sp,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "Vista previa - presiona Aceptar otra vez para pantalla completa",
                                        color = TemaTV.TextoTenue,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
