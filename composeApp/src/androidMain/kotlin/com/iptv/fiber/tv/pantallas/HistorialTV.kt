package com.iptv.fiber.tv.pantallas

// removed android import: import android.content.Intent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.iptv.fiber.datos.local.GestorPreferencias
import com.iptv.fiber.datos.modelo.Canal
import com.iptv.fiber.datos.repositorio.RepositorioAutenticacion
import com.iptv.fiber.interfaz.modelovista.ModeloVistaContenido
import com.iptv.fiber.interfaz.reproductor.ActividadReproductor
import com.iptv.fiber.interfaz.reproductor.ClavesReproductor
import com.iptv.fiber.tv.componentes.EncabezadoPantallaTV
import com.iptv.fiber.tv.componentes.EstadoVacioTV
import com.iptv.fiber.tv.componentes.FondoPantallaTV
import com.iptv.fiber.tv.componentes.TemaTV
import com.iptv.fiber.tv.componentes.esContenidoAdultoTV
import com.iptv.fiber.tv.componentes.TarjetaCanalGrandeTV
import com.iptv.fiber.tv.dialogos.DialogoPinTV
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.launch

/** Pantalla de historial en TV: cuadrícula de los últimos canales vistos con soporte de control parental. */
@Composable
fun HistorialTV(
    modeloVista: ModeloVistaContenido,
    repositorioAuth: RepositorioAutenticacion,
    requeridorFocoContenido: FocusRequester = remember { FocusRequester() }
) {
    val historial by modeloVista.historial.collectAsStateWithLifecycle()
    val favoritos by modeloVista.favoritos.collectAsStateWithLifecycle()
    val favoritosIds = remember(favoritos) { favoritos.map { it.idTransmision }.toSet() }
    val categorias by modeloVista.categoriasEnVivo.collectAsStateWithLifecycle()
    val contexto = LocalContext.current
    val alcance = rememberCoroutineScope()
    val gestorPreferencias = remember { GestorPreferencias(contexto) }
    val controlParentalActivo by gestorPreferencias.controlParentalActivo.collectAsStateWithLifecycle(initialValue = false)
    val pinParental by gestorPreferencias.pinParental.collectAsStateWithLifecycle(initialValue = "")
    var canalPendientePin by remember { mutableStateOf<Canal?>(null) }

    LaunchedEffect(Unit) {
        modeloVista.iniciarObservacionDatosUsuario()
        modeloVista.cargarHistorial()
    }

    LaunchedEffect(historial) {
        kotlinx.coroutines.delay(100)
        try {
            requeridorFocoContenido.requestFocus()
        } catch (_: Exception) {}
    }

    val abrirCanal: (Canal) -> Unit = { canal ->
        val servidor = repositorioAuth.servidorActual.value
        if (servidor != null) {
            alcance.launch {
                // Construir la URL directamente usando el servidor y el id del canal
                // No dependemos del caché (que puede estar vacío si el usuario no visitó TV en Vivo primero)
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

                if (urlTransmision.isNullOrEmpty()) return@launch

// TODO(KMP):                 val intent = Intent(contexto, ActividadReproductor::class.java).apply {
                    putExtra(ClavesReproductor.URL_TRANSMISION, urlTransmision)
                    putExtra(ClavesReproductor.ID_TRANSMISION, canal.id_transmision)
                    putExtra(ClavesReproductor.TIPO_TRANSMISION, "live")
                    putExtra(ClavesReproductor.NOMBRE_CANAL, canal.nombre)
                    putExtra(ClavesReproductor.LOGOTIPO_CANAL, canal.icono_transmision)
                    putExtra(ClavesReproductor.SERVIDOR_URL, servidor.urlServidor)
                    putExtra(ClavesReproductor.USUARIO, servidor.usuario)
                    putExtra(ClavesReproductor.CONTRASENA, servidor.contrasena)
                }

                modeloVista.agregarAlHistorial(canal)
// TODO(KMP):                 contexto.startActivity(intent)
            }
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

    FondoPantallaTV {
        Column(modifier = Modifier.fillMaxSize()) {
            EncabezadoPantallaTV(
                titulo = "Historial",
                subtitulo = if (historial.isEmpty()) {
                    "Aquí aparecerán los canales vistos recientemente"
                } else {
                    "Ultimos ${historial.size} de 50 canales vistos"
                },
                icono = Icons.Default.History,
                etiqueta = "${historial.size} vistos"
            )

            if (historial.isEmpty()) {
                EstadoVacioTV(
                    icono = Icons.Default.History,
                    titulo = "Sin historial aún",
                    subtitulo = "Los canales que reproduzcas aparecerán en esta pantalla.",
                    modifier = Modifier.focusRequester(requeridorFocoContenido)
                )
            } else {

                LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = TemaTV.AnchoTarjetaCanal),
                    verticalArrangement = Arrangement.spacedBy(TemaTV.EspacioGrid),
                    horizontalArrangement = Arrangement.spacedBy(TemaTV.EspacioGrid),
                    contentPadding = PaddingValues(horizontal = TemaTV.MargenPantalla, vertical = 10.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    itemsIndexed(
                        items = historial,
                        key = { _, item -> item.idTransmision },
                        contentType = { _, _ -> "CanalGrandeTV" }
                    ) { index, item ->
                        val esPrimero = index == 0
                        val canal = remember(item.idTransmision) {
                            // canalBase construido dentro de remember para no crear objetos
                            // en cada recomposición; la búsqueda es O(1) en el mapa en RAM.
                            val canalBase = Canal(
                                id_transmision = item.idTransmision,
                                nombre = item.nombre,
                                icono_transmision = item.icono,
                                id_categoria = ""
                            )
                            modeloVista.obtenerCanalCompleto(item.idTransmision, canalBase)
                        }
                        val modFocus = if (esPrimero) Modifier.focusRequester(requeridorFocoContenido) else Modifier
                        val esFav = canal.id_transmision in favoritosIds
                        TarjetaCanalGrandeTV(
                            canal = canal,
                            esFavorito = esFav,
                            alHacerClick = { reproducirCanal(canal) },
                            alHacerLongClick = {
                                modeloVista.alternarFavorito(canal)
                                val msg = if (esFav) "Quitado de favoritos" else "Agregado a favoritos"
// TODO(KMP):                                 android.widget.Toast.makeText(contexto, msg, android.widget.Toast.LENGTH_SHORT).show()
                            },
                            modificadorExtra = modFocus
                        )
                    }
                }
            }
        }
    }
}
