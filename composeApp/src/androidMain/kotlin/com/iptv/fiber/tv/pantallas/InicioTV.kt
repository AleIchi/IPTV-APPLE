package com.iptv.fiber.tv.pantallas

import com.iptv.fiber.tv.dialogos.DialogoPinTV
// removed android import: import android.content.Intent
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.focusable
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.iptv.fiber.R
import com.iptv.fiber.datos.modelo.Canal
import com.iptv.fiber.datos.modelo.Categoria
import com.iptv.fiber.datos.local.GestorPreferencias
import com.iptv.fiber.datos.repositorio.RepositorioAutenticacion
import com.iptv.fiber.interfaz.componentes.IconoTvPorDefecto
import com.iptv.fiber.interfaz.modelovista.ModeloVistaContenido
import com.iptv.fiber.interfaz.reproductor.ActividadReproductor
import com.iptv.fiber.interfaz.reproductor.ClavesReproductor
import com.iptv.fiber.interfaz.tema.AcentoPremium
import com.iptv.fiber.tv.componentes.esContenidoAdultoTV
import com.iptv.fiber.tv.componentes.TarjetaTV
import com.iptv.fiber.tv.componentes.TemaTV
import com.iptv.fiber.tv.componentes.PlaceholderCanalTV
import com.iptv.fiber.tv.componentes.TarjetaCanalGrandeTV
import com.iptv.fiber.tv.componentes.TarjetaCanalMedianaTV
import com.iptv.fiber.tv.componentes.SeccionTituloTV
import com.iptv.fiber.tv.componentes.ImagenPromocionalTV
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

/**
 * Modelo de datos local para enriquecer los banners promocionales del Hero.
 */
data class InfoBannerTV(
    val idImagen: Int,
    val titulo: String,
    val categoria: String,
    val descripcion: String
)

/** 
 * LA PANTALLA TIPO "NETFLIX" (Inicio de TV).
 * Esta es la pantalla principal que ves al entrar. Tiene:
 * 1. Un "Hero Banner" arriba (imágenes grandes que van pasando).
 * 2. Filas horizontales de canales separados por categoría (Ej: "Deportes", "Noticias").
 * Usa "LazyColumn" para reciclar memoria hacia abajo, y "LazyRow" para reciclar memoria hacia los lados.
 */
@kotlin.OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun InicioTV(
    modeloVista: ModeloVistaContenido,
    repositorioAuth: RepositorioAutenticacion? = null,
    requeridorFocoContenido: FocusRequester = remember { FocusRequester() }
) {
    val contexto = LocalContext.current
    val alcance = rememberCoroutineScope()
    val gestorPreferencias = remember { GestorPreferencias(contexto) }
    val controlParentalActivo by gestorPreferencias.controlParentalActivo.collectAsStateWithLifecycle(initialValue = false)
    val pinParental by gestorPreferencias.pinParental.collectAsStateWithLifecycle(initialValue = "")
    var canalPendientePin by remember { mutableStateOf<Canal?>(null) }
    var contextoReproduccionPendiente by remember { mutableStateOf<List<Canal>>(emptyList()) }

    // Enriquecimiento de banners promocionales locales
    val infoBanners = remember {
        listOf(
            InfoBannerTV(
                idImagen = R.drawable.promocion_camara,
                titulo = "Cámaras de Seguridad",
                categoria = "Monitoreo en Vivo",
                descripcion = "Visualiza tus cámaras de seguridad y transmisiones privadas en tiempo real con latencia ultra baja y en alta definición."
            ),
            InfoBannerTV(
                idImagen = R.drawable.promocion_fibra_juegos,
                titulo = "Z-Fiber Gamer",
                categoria = "Gaming & Esports",
                descripcion = "La conexión simétrica definitiva. Vive la adrenalina de los torneos de eSports, transmisiones en 4K y gameplays con ping mínimo."
            ),
            InfoBannerTV(
                idImagen = R.drawable.promocion_fibra_optica,
                titulo = "Fibra Óptica Simétrica",
                categoria = "Conectividad Z",
                descripcion = "Navega y comparte a la velocidad de la luz sin interrupciones. Tecnología FTTH para conectar todos tus dispositivos del hogar."
            ),
            InfoBannerTV(
                idImagen = R.drawable.promocion_tv_en_vivo,
                titulo = "Televisión Digital",
                categoria = "Entretenimiento",
                descripcion = "Accede a cientos de canales nacionales e internacionales en vivo. Deportes, películas, series y noticias en una sola app."
            ),
            InfoBannerTV(
                idImagen = R.drawable.promocion_tv_estable,
                titulo = "Señal de Alta Estabilidad",
                categoria = "Tecnología Z",
                descripcion = "Nuestra red de fibra dedicada garantiza transmisiones estables y sin congelamientos incluso durante las horas pico de mayor tráfico."
            ),
            InfoBannerTV(
                idImagen = R.drawable.promocion_deportes_en_vivo,
                titulo = "Deportes en Vivo",
                categoria = "Pasión Deportiva",
                descripcion = "El mejor asiento del estadio está en tu hogar. Sigue en directo la Liga 1, Champions League, Copa Libertadores y mucho más."
            )
        )
    }

    val listaImagenesPromocionales = remember {
        infoBanners.map { it.idImagen }
    }
    var indiceImagenActual by remember { mutableStateOf(0) }

    // El reloj digital se ha movido al componente independiente RelojDigitalTV para evitar recomposiciones de toda la pantalla

    LaunchedEffect(indiceImagenActual) {
        // Dar prioridad al inicio de la carga de datos antes de arrancar el carrusel
        if (indiceImagenActual == 0) kotlinx.coroutines.delay(300L)
        kotlinx.coroutines.delay(6000L) // Cambiar automáticamente cada 6 segundos
        indiceImagenActual = (indiceImagenActual + 1) % listaImagenesPromocionales.size
    }

    LaunchedEffect(Unit) {
        // Carga PARALELA de categorías y canales — reduce el tiempo de red a la mitad
        modeloVista.iniciarCargaCompleta()
        modeloVista.iniciarObservacionDatosUsuario()
    }

    val canalesPrincipales by modeloVista.canalesPrincipales.collectAsStateWithLifecycle()
    val categorias by modeloVista.categoriasEnVivo.collectAsStateWithLifecycle()
    val categoriasPanelPrincipal by modeloVista.categoriasPanelPrincipal.collectAsStateWithLifecycle()
    val favoritos by modeloVista.favoritos.collectAsStateWithLifecycle()
    val favoritosIds = androidx.compose.runtime.remember(favoritos) {
        favoritos.map { it.idTransmision }.toSet()
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

    // Lambda para reproducir un canal (mismos extras que la app móvil)
    val abrirCanal: (Canal, List<Canal>) -> Unit = { canal, contextoCanales ->
        val servidor = repositorioAuth?.servidorActual?.value
        if (servidor != null) {
            val urlTransmision = if (!canal.fuenteDirecta.isNullOrEmpty()) canal.fuenteDirecta
            else repositorioAuth.construirUrlTransmision(servidor.urlServidor, servidor.usuario, servidor.contrasena, "live", canal.id_transmision)

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
                modeloVista.establecerContextoReproduccion(contextoCanales)
                modeloVista.agregarAlHistorial(canal)
            }
// TODO(KMP):             contexto.startActivity(intent)
        }
    }

    val reproducirCanal: (Canal, List<Canal>) -> Unit = { canal, contextoCanales ->
        if (controlParentalActivo && pinParental.isNotEmpty() && esContenidoAdultoTV(canal, categorias)) {
            canalPendientePin = canal
            contextoReproduccionPendiente = contextoCanales
        } else {
            abrirCanal(canal, contextoCanales)
        }
    }

    if (canalPendientePin != null) {
        DialogoPinTV(
            titulo = "Control parental",
            descripcion = "Ingresa tu PIN para reproducir este canal",
            pinCorrecto = pinParental,
            alConfirmar = {
                canalPendientePin?.let { abrirCanal(it, contextoReproduccionPendiente) }
                canalPendientePin = null
            },
            alCancelar = { canalPendientePin = null }
        )
    }

    val estadoListaPerezosa = androidx.compose.foundation.lazy.rememberLazyListState()

    LaunchedEffect(canalesPrincipales) {
        if (canalesPrincipales.isNotEmpty()) {
            try { requeridorFocoContenido.requestFocus() } catch (_: Exception) {}
        }
    }

    // Categorías visibles (sin vacías) precalculadas: garantiza índices contiguos en la LazyColumn
    // para que el centrado vertical apunte a la fila correcta.
    val categoriasVisibles = remember(categoriasPanelPrincipal) {
        categoriasPanelPrincipal.take(15).filter { it.second.isNotEmpty() }
    }
    val hayRecomendados = canalesPrincipales.isNotEmpty()
    // Índice base de la primera fila de categoría: item 0 = hero/promos, item 1 = recomendados (si hay)
    val baseCategorias = if (hayRecomendados) 2 else 1

    var ultimaNavegacion by remember { mutableLongStateOf(0L) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(TemaTV.FondoPrincipal)
            .onKeyEvent { event ->
                if ((event.key == Key.DirectionDown || event.key == Key.DirectionUp || event.key == Key.DirectionLeft || event.key == Key.DirectionRight)
                    && event.type == KeyEventType.KeyDown) {
                    val ahora = System.currentTimeMillis()
                    // Si pasaron menos de 180ms desde la última tecla, ignorar (evita el bug de scroll ultra rápido)
                    if (ahora - ultimaNavegacion < 180L) return@onKeyEvent true
                    ultimaNavegacion = ahora
                    false
                } else false
            }
    ) {
        LazyColumn(
            state = estadoListaPerezosa,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(top = 82.dp, bottom = 40.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
            // ═══════════ IMAGEN PRINCIPAL Y PROMOCIONES ═══════════
            @kotlin.OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
            val requeridorTraerAVista = remember { androidx.compose.foundation.relocation.BringIntoViewRequester() }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .bringIntoViewRequester(requeridorTraerAVista)
                    .padding(horizontal = TemaTV.MargenPantalla)
            ) {
                // Banner principal flotante premium con bordes redondeados y marco sutil de cristal
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(230.dp) // Reducido para dar máxima visibilidad y espacio a los canales recomendados
                        .clip(RoundedCornerShape(16.dp))
                        .border(
                            width = 1.dp,
                            brush = Brush.linearGradient(
                                colors = listOf(
                                    Color.White.copy(alpha = 0.14f),
                                    Color.White.copy(alpha = 0.02f)
                                )
                            ),
                            shape = RoundedCornerShape(16.dp)
                        )
                ) {
                    Crossfade(
                        targetState = indiceImagenActual,
                        modifier = Modifier.fillMaxSize(),
                        label = "CrossfadeHero"
                    ) { index ->
                        coil.compose.AsyncImage(
                            model = listaImagenesPromocionales[index],
                            contentDescription = "Imagen principal",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    }

                    // Degradado oscuro inferior integrado en el banner para transicionar con el fondo
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(
                                        Color.Transparent,
                                        Color.Black.copy(alpha = 0.5f)
                                    )
                                )
                            )
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Fila de miniaturas promocionales compactas y ordenadas
                @kotlin.OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
                LazyRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusGroup()
                        .onFocusChanged { state ->
                            if (state.hasFocus) {
                                alcance.launch {
                                    // Añadimos un pequeño retraso de 50ms. Esto es un truco necesario en Android TV
                                    // porque el sistema de foco nativo intenta hacer su propio scroll al mismo tiempo
                                    // y cancela nuestra animación fluida. Al esperar 50ms, nuestra animación toma 
                                    // el control y se ejecuta de manera suave y perfecta cada vez.
                                    kotlinx.coroutines.delay(50)
                                    estadoListaPerezosa.animateScrollToItem(0)
                                }
                            }
                        },
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(vertical = 4.dp)
                ) {
                    itemsIndexed(items = listaImagenesPromocionales, key = { index, _ -> index }) { index, idImagen ->
                        val modFocus = if (index == 0) {
                            Modifier.focusRequester(requeridorFocoContenido)
                        } else Modifier

                        ImagenPromocionalTV(
                            imagenResId = idImagen,
                            estaSeleccionada = index == indiceImagenActual,
                            alHacerClick = { /* Acción al hacer click en la imagen */ },
                            alEnfocar = {
                                indiceImagenActual = index // Cambiar el fondo al enfocar
                            },
                            modifier = modFocus
                        )
                    }
                }
            }
            }

            // ═══════════ CANALES DESTACADOS ═══════════
            if (canalesPrincipales.isNotEmpty()) {
                item {
                    Column(
                        modifier = Modifier.onFocusChanged { estado ->
                            if (estado.hasFocus) {
                                alcance.launch {
                                    kotlinx.coroutines.delay(50)
                                    centrarFilaEnLista(estadoListaPerezosa, 1)
                                }
                            }
                        }
                    ) {
                        SeccionTituloTV(
                            titulo = "Canales recomendados",
                            paddingSuperior = 4.dp
                        )

                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                    contentPadding = PaddingValues(end = TemaTV.MargenPantalla),
                    modifier = Modifier
                        .padding(start = TemaTV.MargenPantalla)
                        .fillMaxWidth()
                        .focusGroup()
                ) {
                    items(
                        items = canalesPrincipales,
                        key = { it.id_transmision },
                        contentType = { "CanalDestacado" }
                    ) { canal: Canal ->
                        val esFav = favoritosIds.contains(canal.id_transmision)
                        val clickCanal = remember(canal.id_transmision) { { reproducirCanal(canal, canalesPrincipales) } }
                        val longClickCanal = remember(canal) { { alternarFavorito(canal) } }
                        TarjetaCanalGrandeTV(
                            canal = canal,
                            esFavorito = esFav,
                            alHacerClick = clickCanal,
                            alHacerLongClick = longClickCanal,
                            modificadorExtra = Modifier
                        )
                    }
                }
                    }
                }
            }

            // ── FILAS POR CATEGORÍA (MÁXIMO 20 PARA MEJOR EXPERIENCIA) ──
            categoriasVisibles.forEachIndexed { posicion, par ->
                val (categoria, canalesCat) = par
                    item {
                        val indiceEnLista = baseCategorias + posicion
                        Column(
                            modifier = Modifier.onFocusChanged { estado ->
                                if (estado.hasFocus) {
                                    alcance.launch {
                                        kotlinx.coroutines.delay(50)
                                        centrarFilaEnLista(estadoListaPerezosa, indiceEnLista)
                                    }
                                }
                            }
                        ) {
                            SeccionTituloTV(categoria.nombre_categoria)
                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(16.dp),
                            contentPadding = PaddingValues(end = TemaTV.MargenPantalla),
                            modifier = Modifier
                                .padding(start = TemaTV.MargenPantalla)
                                .fillMaxWidth()
                                .focusGroup()
                        ) {
                            items(
                                items = canalesCat,
                                key = { it.id_transmision },
                                contentType = { "CanalCategoria" }
                            ) { canal: Canal ->
                                val esFav = favoritosIds.contains(canal.id_transmision)
                                val clickCanal = remember(canal) { { reproducirCanal(canal, canalesCat) } }
                                val longClickCanal = remember(canal) { { alternarFavorito(canal) } }
                                TarjetaCanalMedianaTV(
                                    canal = canal,
                                    esFavorito = esFav,
                                    alHacerClick = clickCanal,
                                    alHacerLongClick = longClickCanal
                                )
                            }
                        }
                    }
                    }
                }
            // Espacio final ya está manejado por contentPadding
        }

        // 2. Cabecera Fija Flotante (Siempre visible y elegante)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(82.dp)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFF070812).copy(alpha = 0.96f),
                            Color(0xFF070812).copy(alpha = 0.75f),
                            Color.Transparent
                        )
                    )
                )
                .padding(horizontal = TemaTV.MargenPantalla),
            contentAlignment = Alignment.Center
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Logo de la App
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.logotipo_reducido),
                        contentDescription = null,
                        tint = TemaTV.AcentoClaro,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = "FIBER Z TV",
                        color = TemaTV.TextoPrincipal,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 1.sp
                    )
                }

                // Reloj Digital
                RelojDigitalTV()
            }
        }
    }
}

/**
 * COMPONENTE AISLADO: RELOJ DIGITAL.
 * ¿Por qué el reloj tiene su propia función en lugar de estar dentro de "InicioTV"?
 * Porque Jetpack Compose redibuja la pantalla cada vez que un dato cambia.
 * Si pusiéramos el reloj en la pantalla principal, TODA la pantalla (los cientos de canales)
 * se redibujaría cada minuto. Al sacarlo, solo el texto del reloj se redibuja, ahorrando muchísima batería.
 */
@Composable
fun RelojDigitalTV() {
    var horaFechaTexto by remember { mutableStateOf("") }
    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(500L)
        val format = SimpleDateFormat("hh:mm a  \u2022  EEEE, d 'de' MMMM", java.util.Locale("es", "ES"))
        while (true) {
            val cal = java.util.Calendar.getInstance().time
            horaFechaTexto = format.format(cal)
                .replaceFirstChar { if (it.isLowerCase()) it.titlecase(java.util.Locale("es", "ES")) else it.toString() }
            kotlinx.coroutines.delay(15000L) // Actualizar cada 15 segundos
        }
    }

    if (horaFechaTexto.isNotEmpty()) {
        Text(
            text = horaFechaTexto,
            color = TemaTV.TextoSecundario,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            letterSpacing = 0.5.sp
        )
    }
}

/**
 * Desplaza la lista vertical para que la fila [indice] quede CENTRADA en pantalla.
 * Calcula el offset usando el alto real del viewport y el alto de la fila, de modo que
 * el bloque enfocado no se quede pegado abajo al navegar con el control remoto.
 */
private suspend fun centrarFilaEnLista(
    estado: androidx.compose.foundation.lazy.LazyListState,
    indice: Int
) {
    val info = estado.layoutInfo
    val alturaViewport = info.viewportEndOffset - info.viewportStartOffset
    if (alturaViewport <= 0) {
        // En lugar de animateScroll, usar scrollToItem cuando falla el cálculo de viewports, para no encolar animaciones
        estado.scrollToItem(indice)
        return
    }
    val alturaItem = info.visibleItemsInfo.firstOrNull { it.index == indice }?.size ?: 0
    val desplazamiento = -((alturaViewport - alturaItem) / 2).coerceAtLeast(0)
    estado.animateScrollToItem(indice, desplazamiento)
}



