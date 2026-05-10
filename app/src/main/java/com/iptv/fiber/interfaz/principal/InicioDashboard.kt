@file:OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
package com.iptv.fiber.interfaz.principal

import android.content.Intent
import android.net.Uri
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
import com.iptv.fiber.interfaz.componentes.TarjetaCanalTop
import com.iptv.fiber.interfaz.modelovista.ModeloVistaAutenticacion
import com.iptv.fiber.interfaz.modelovista.ModeloVistaContenido
import com.iptv.fiber.interfaz.reproductor.ActividadReproductor
import com.iptv.fiber.interfaz.tema.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

@Composable
fun InicioDashboard(
    alNavegar: (String) -> Unit,
    modeloVistaAuth: ModeloVistaAutenticacion,
    modeloVistaContenido: ModeloVistaContenido,
    repositorioAuth: RepositorioAutenticacion
) {
    ContenidoInicioDashboard(
        alNavegar = alNavegar,
        alCerrarSesion = { modeloVistaAuth.cerrarSesion() },
        modeloVista = modeloVistaContenido,
        repositorioAuth = repositorioAuth
    )
}

@Composable
fun ContenidoInicioDashboard(
    alNavegar: (String) -> Unit,
    alCerrarSesion: () -> Unit,
    modeloVista: ModeloVistaContenido,
    repositorioAuth: RepositorioAutenticacion
) {
    val contexto = LocalContext.current
    val todosCanales by modeloVista.canalesEnVivo.collectAsState()
    var datosInicialesCargados by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        if (!datosInicialesCargados) {
            modeloVista.cargarCategoriasEnVivo()
            modeloVista.cargarCanalesEnVivo()
            modeloVista.iniciarObservacionDatosUsuario()
            datosInicialesCargados = true
        }
    }

    val alcance = rememberCoroutineScope()
    val reproducirCanal = construirLambdaReproduccionCanal(contexto, repositorioAuth, modeloVista, alcance, todosCanales)

    val canalesTop by modeloVista.canalesTop.collectAsState()
    val categoriasConCanales by modeloVista.categoriasPanelPrincipal.collectAsState()
    val recientes by modeloVista.historialReciente.collectAsState()
    val epg by modeloVista.epg.collectAsState()

    // ─── Lógica de Autoplay al Inicio ───
    val preferencias = contexto.getSharedPreferences("iptv_preferencias", android.content.Context.MODE_PRIVATE)
    val autoplayActivo = preferencias.getBoolean("autoplay_inicio", false)
    var autoplayEjecutado by androidx.compose.runtime.saveable.rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(recientes) {
        if (autoplayActivo && !autoplayEjecutado && recientes.isNotEmpty()) {
            autoplayEjecutado = true // Marcar antes de lanzar para evitar bucles
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
                Text("Fiber Z TV+", style = MaterialTheme.typography.headlineMedium,
                    color = Color.White, fontWeight = FontWeight.Bold)
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

        // Banner carrusel
        item {
            val anuncios = listOf(
                com.iptv.fiber.R.drawable.banner_camara,
                com.iptv.fiber.R.drawable.banner_fiber_gamer,
                com.iptv.fiber.R.drawable.banner_fibra_optica
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
                    .aspectRatio(2.2f) // Proporción responsiva tipo banner (aprox 21:9)
                    .padding(bottom = 24.dp), // Más espacio bajo el banner
                shape = RoundedCornerShape(16.dp), // Esquinas más suaves
                elevation = CardDefaults.cardElevation(defaultElevation = 12.dp)
            ) {
                HorizontalPager(state = estadoPaginador, modifier = Modifier.fillMaxSize()) { pagina ->
                    Image(
                        painter = painterResource(id = anuncios[pagina]),
                        contentDescription = "Banner Promocional",
                        modifier = Modifier.fillMaxSize().clickable {
                            contexto.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://fiberztelecom.com/")))
                        },
                        contentScale = ContentScale.FillBounds // Rellena el espacio del banner responsivo
                    )
                }
            }
        }

        // Top 10 canales
        if (canalesTop.isNotEmpty()) {
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
                    itemsIndexed(items = canalesTop) { indice: Int, canal: com.iptv.fiber.datos.modelo.Canal ->
                        TarjetaCanalTop(
                            canal = canal, 
                            posicion = indice + 1, 
                            programaActual = epg[canal.id_transmision.toString()]?.firstOrNull(),
                            alHacerClick = { reproducirCanal(canal) }
                        )
                    }
                }
            }
        }

        categoriasConCanales.forEach { (categoria, canales) ->
            item {
                Text(text = categoria.nombre_categoria as String, style = MaterialTheme.typography.headlineSmall, color = Color.White,
                    fontWeight = FontWeight.ExtraBold, modifier = Modifier.padding(bottom = 12.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.padding(bottom = 32.dp)) {
                    items(items = canales) { canal: com.iptv.fiber.datos.modelo.Canal ->
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
                    items(items = recientes) { registro: com.iptv.fiber.datos.local.base_datos.SeguirViendo ->
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

/** Cabecera de sección reutilizable */
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
            val urlStream = if (!canal.fuenteDirecta.isNullOrEmpty()) canal.fuenteDirecta
            else repositorioAuth.construirUrlStream(servidor.urlServidor, servidor.usuario, servidor.contrasena, "live", canal.id_transmision)
            val intento = Intent(contexto, ActividadReproductor::class.java).apply {
                putExtra("url_transmision", urlStream)
                putExtra("id_transmision", canal.id_transmision)
                putExtra("tipo_transmision", "live")
                putExtra("nombre_canal", canal.nombre)
                putExtra("logo_canal", canal.icono_transmision)
                putExtra("servidor_url", servidor.urlServidor)
                putExtra("usuario", servidor.usuario)
                putExtra("contrasena", servidor.contrasena)
            }
            // Historial y contexto en segundo plano, no bloquean el lanzamiento
            alcance.launch {
                modeloVista.establecerContextoReproduccion(listaContexto)
                modeloVista.agregarAlHistorial(canal)
            }
            contexto.startActivity(intento)
        }
    }
}
