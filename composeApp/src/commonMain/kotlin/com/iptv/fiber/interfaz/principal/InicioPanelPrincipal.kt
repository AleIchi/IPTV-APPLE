@file:OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
package com.iptv.fiber.interfaz.principal

// removed android import: import android.content.Intent
// removed android import: import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.iptv.fiber.datos.repositorio.RepositorioAutenticacion
import com.iptv.fiber.interfaz.componentes.TarjetaCanalEstandar
import com.iptv.fiber.interfaz.componentes.TarjetaCanalPrincipal
import com.iptv.fiber.interfaz.modelovista.ModeloVistaAutenticacion
import com.iptv.fiber.interfaz.modelovista.ModeloVistaContenido
import com.iptv.fiber.interfaz.reproductor.ActividadReproductor
import com.iptv.fiber.interfaz.reproductor.ClavesReproductor
import com.iptv.fiber.interfaz.tema.*
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * Puerta de entrada al panel principal: delega en [ContenidoInicioPanelPrincipal]
 * pasando las dependencias necesarias.
 */
@Composable
fun InicioPanelPrincipal(
    alNavegar: (String) -> Unit,
    modeloVistaAuth: ModeloVistaAutenticacion,
    modeloVistaContenido: ModeloVistaContenido,
    repositorioAuth: RepositorioAutenticacion
) {
    ContenidoInicioPanelPrincipal(
        alNavegar = alNavegar,
        alCerrarSesion = { modeloVistaAuth.cerrarSesion() },
        modeloVista = modeloVistaContenido,
        repositorioAuth = repositorioAuth
    )
}

/**
 * Muestra el panel principal: carrusel de banners promocionales, canales de tendencia,
 * primeras 4 categorías con sus canales y los 10 canales del historial reciente.
 */
@Composable
fun ContenidoInicioPanelPrincipal(
    alNavegar: (String) -> Unit,
    alCerrarSesion: () -> Unit,
    modeloVista: ModeloVistaContenido,
    repositorioAuth: RepositorioAutenticacion
) {
    val contexto = LocalContext.current
    val todosCanales by modeloVista.canalesEnVivo.collectAsStateWithLifecycle()
    var datosInicialesCargados by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        if (!datosInicialesCargados) {
            modeloVista.iniciarCargaCompleta()
            modeloVista.iniciarObservacionDatosUsuario()
            datosInicialesCargados = true
        }
    }

    val alcance = rememberCoroutineScope()
    val reproducirCanal = construirLambdaReproduccionCanal(contexto, repositorioAuth, modeloVista, alcance, todosCanales)

    val canalesPrincipales by modeloVista.canalesPrincipales.collectAsStateWithLifecycle()
    val categoriasConCanales by modeloVista.categoriasPanelPrincipal.collectAsStateWithLifecycle()
    val recientes by modeloVista.historialReciente.collectAsStateWithLifecycle()
    val epg by modeloVista.epg.collectAsStateWithLifecycle()

    // ─── Lógica de reproducción automática al inicio ───
    val preferencias = contexto.getSharedPreferences("iptv_preferencias", android.content.Context.MODE_PRIVATE)
    val reproduccionAutomaticaActiva = preferencias.getBoolean("reproduccion_automatica_inicio", false)
    var reproduccionAutomaticaEjecutada by androidx.compose.runtime.saveable.rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(recientes) {
        if (reproduccionAutomaticaActiva && !reproduccionAutomaticaEjecutada && recientes.isNotEmpty()) {
            reproduccionAutomaticaEjecutada = true // Marcar antes de lanzar para evitar bucles
            val ultimo = recientes.first()
            val canal = com.iptv.fiber.datos.modelo.Canal(
                id_transmision = ultimo.idTransmision,
                nombre = ultimo.nombre,
                icono_transmision = ultimo.icono
            )
            reproducirCanal(canal)
        }
    }

    LazyColumn(modifier = Modifier.fillMaxSize()) {
        // Cabecera
        item {
            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Image(
                    painter = painterResource(id = com.iptv.fiber.R.drawable.logotipo_fiber_z),
                    contentDescription = "Fiber Z TV+",
                    modifier = Modifier
                        .height(28.dp)
                        .widthIn(max = 150.dp),
                    contentScale = ContentScale.Fit
                )
                Row {
                    IconButton(onClick = { alNavegar("envivo") }) {
                        Icon(Icons.Default.Search, "Buscar", tint = Color.White)
                    }
                    IconButton(onClick = { alNavegar("ajustes") }) {
                        Icon(Icons.Default.Settings, "Ajustes", tint = Color.White)
                    }
                    IconButton(onClick = alCerrarSesion) {
                        Icon(Icons.Default.Logout, "Cerrar Sesión", tint = TextoSecundarioPremium)
                    }
                }
            }
        }

        // Carrusel de promociones
        item {
            val anuncios = listOf(
                com.iptv.fiber.R.drawable.promocion_camara,
                com.iptv.fiber.R.drawable.promocion_fibra_juegos,
                com.iptv.fiber.R.drawable.promocion_fibra_optica,
                com.iptv.fiber.R.drawable.promocion_tv_en_vivo,
                com.iptv.fiber.R.drawable.promocion_tv_estable,
                com.iptv.fiber.R.drawable.promocion_deportes_en_vivo
            )
            val estadoPaginador = rememberPagerState(pageCount = { anuncios.size })
            LaunchedEffect(Unit) {
                while (true) {
                    delay(5000)
                    estadoPaginador.animateScrollToPage((estadoPaginador.currentPage + 1) % anuncios.size)
                }
            }
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1.77f) // Proporción perfecta 16:9 para banners de TV
                    .padding(bottom = 24.dp),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 12.dp)
            ) {
                HorizontalPager(state = estadoPaginador, modifier = Modifier.fillMaxSize()) { pagina ->
                    coil.compose.AsyncImage(
                        model = anuncios[pagina],
                        contentDescription = "Imagen promocional",
                        modifier = Modifier
                            .fillMaxSize()
                            .clickable {
// TODO(KMP):                                 contexto.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://fiberztelecom.com/")))
                            },
                        contentScale = ContentScale.Crop // Mantiene proporción perfecta sin estirar la imagen
                    )
                }
            }
        }

        // 10 canales principales
        if (canalesPrincipales.isNotEmpty()) {
            item {
                CabeceraSeccion(titulo = "Canales de tendencia") {
                    Card(colors = CardDefaults.cardColors(containerColor = DetallePremium),
                        shape = RoundedCornerShape(6.dp)) {
                        Text("MÁS VISTO", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.ExtraBold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp), color = Color.Black)
                    }
                }
                LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp), // Más espacio entre tarjetas
                    modifier = Modifier.padding(bottom = 32.dp)) {
                    itemsIndexed(items = canalesPrincipales, key = { _, canal -> canal.id_transmision }) { indice: Int, canal: com.iptv.fiber.datos.modelo.Canal ->
                        TarjetaCanalPrincipal(
                            canal = canal, 
                            posicion = indice + 1, 
                            programaActual = epg[canal.id_transmision.toString()]?.firstOrNull(),
                            alHacerClick = { reproducirCanal(canal) }
                        )
                    }
                }
            }
        }

        categoriasConCanales.take(20).forEach { (categoria, canales) ->
            item {
                Text(text = categoria.nombre_categoria as String, style = MaterialTheme.typography.headlineSmall, color = Color.White,
                    fontWeight = FontWeight.ExtraBold, modifier = Modifier.padding(bottom = 12.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.padding(bottom = 32.dp)) {
                    items(items = canales, key = { it.id_transmision }, contentType = { "canalCategoria" }) { canal: com.iptv.fiber.datos.modelo.Canal ->
                        TarjetaCanalEstandar(
                            canal = canal, 
                            programaActual = epg[canal.id_transmision.toString()]?.firstOrNull(),
                            alHacerClick = { reproducirCanal(canal) }
                        )
                    }
                }
            }
        }

        // Recientes del historial
        if (recientes.isNotEmpty()) {
            item {
                Text("10 canales recientes", style = MaterialTheme.typography.titleLarge, color = Color.White,
                    fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 8.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(bottom = 24.dp)) {
                    items(items = recientes, key = { it.idTransmision }) { registro: com.iptv.fiber.datos.local.base_datos.SeguirViendo ->
                        val canal = com.iptv.fiber.datos.modelo.Canal(
                            id_transmision = registro.idTransmision,
                            nombre = registro.nombre,
                            icono_transmision = registro.icono
                        )
                        TarjetaCanalEstandar(
                            canal = canal, 
                            programaActual = epg[canal.id_transmision.toString()]?.firstOrNull(),
                            alHacerClick = { reproducirCanal(canal) }
                        )
                    }
                }
            }
        }
    }
}

/** Fila de cabecera de sección con [titulo] a la izquierda y un componente [coda] opcional a la derecha (ej. un badge). */
@Composable
private fun CabeceraSeccion(titulo: String, coda: @Composable () -> Unit = {}) {
    Row(modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically) {
        Text(titulo, style = MaterialTheme.typography.titleLarge, color = Color.White, fontWeight = FontWeight.Bold)
        coda()
    }
}

/** Construye la lambda de reproducción de canal para evitar código duplicado */
fun construirLambdaReproduccionCanal(
    contexto: android.content.Context,
    repositorioAuth: RepositorioAutenticacion,
    modeloVista: ModeloVistaContenido,
    alcance: kotlinx.coroutines.CoroutineScope,
    listaContexto: List<com.iptv.fiber.datos.modelo.Canal>
): (com.iptv.fiber.datos.modelo.Canal) -> Unit {
    // Leemos el servidor UNA vez al construir la lambda, no en cada click.
    // servidorActual es un StateFlow, .value es inmediato y no suspende.
    val servidor = repositorioAuth.servidorActual.value
    return { canal ->
        if (servidor != null) {
            // Construcción sincrónica de la URL — sin coroutine, sin delay
            val urlTransmision = if (!canal.fuenteDirecta.isNullOrEmpty()) canal.fuenteDirecta
            else repositorioAuth.construirUrlTransmision(servidor.urlServidor, servidor.usuario, servidor.contrasena, "live", canal.id_transmision)
// TODO(KMP):             val intento = Intent(contexto, ActividadReproductor::class.java).apply {
                putExtra(ClavesReproductor.URL_TRANSMISION, urlTransmision)
                putExtra(ClavesReproductor.ID_TRANSMISION, canal.id_transmision)
                putExtra(ClavesReproductor.TIPO_TRANSMISION, "live")
                putExtra(ClavesReproductor.NOMBRE_CANAL, canal.nombre)
                putExtra(ClavesReproductor.LOGOTIPO_CANAL, canal.icono_transmision)
                putExtra(ClavesReproductor.SERVIDOR_URL, servidor.urlServidor)
                putExtra(ClavesReproductor.USUARIO, servidor.usuario)
                putExtra(ClavesReproductor.CONTRASENA, servidor.contrasena)
            }
            // Historial y contexto en segundo plano, no bloquean el lanzamiento
            alcance.launch {
                modeloVista.establecerContextoReproduccion(listaContexto)
                modeloVista.agregarAlHistorial(canal)
            }
// TODO(KMP):             contexto.startActivity(intento)
        }
    }
}
