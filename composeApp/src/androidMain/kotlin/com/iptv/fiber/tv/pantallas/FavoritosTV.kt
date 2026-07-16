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
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
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
import kotlinx.coroutines.launch

/** Pantalla de favoritos en TV: cuadrícula de canales marcados como favoritos con soporte de control parental. */
@Composable
fun FavoritosTV(
    modeloVista: ModeloVistaContenido,
    repositorioAuth: RepositorioAutenticacion,
    requeridorFocoContenido: FocusRequester = remember { FocusRequester() }
) {
    val favoritos by modeloVista.favoritos.collectAsStateWithLifecycle()
    val categorias by modeloVista.categoriasEnVivo.collectAsStateWithLifecycle()
    val contexto = LocalContext.current
    val alcance = rememberCoroutineScope()
    val gestorPreferencias = remember { GestorPreferencias(contexto) }
    val controlParentalActivo by gestorPreferencias.controlParentalActivo.collectAsStateWithLifecycle(initialValue = false)
    val pinParental by gestorPreferencias.pinParental.collectAsStateWithLifecycle(initialValue = "")
    var canalPendientePin by remember { mutableStateOf<Canal?>(null) }

    LaunchedEffect(Unit) {
        modeloVista.iniciarObservacionDatosUsuario()
        modeloVista.cargarFavoritos()
    }

    LaunchedEffect(favoritos) {
        kotlinx.coroutines.delay(100)
        try {
            requeridorFocoContenido.requestFocus()
        } catch (_: Exception) {}
    }

    val abrirCanal: (Canal) -> Unit = { canal ->
        val servidor = repositorioAuth.servidorActual.value
        if (servidor != null) {
            alcance.launch {
                val completo = modeloVista.obtenerCanalCompleto(canal.id_transmision, canal)
                val urlTransmision = if (!completo.fuenteDirecta.isNullOrEmpty()) {
                    completo.fuenteDirecta
                } else {
                    repositorioAuth.construirUrlTransmision(
                        servidor.urlServidor,
                        servidor.usuario,
                        servidor.contrasena,
                        "live",
                        completo.id_transmision
                    )
                }

// TODO(KMP):                 val intent = Intent(contexto, ActividadReproductor::class.java).apply {
                    putExtra(ClavesReproductor.URL_TRANSMISION, urlTransmision)
                    putExtra(ClavesReproductor.ID_TRANSMISION, completo.id_transmision)
                    putExtra(ClavesReproductor.TIPO_TRANSMISION, "live")
                    putExtra(ClavesReproductor.NOMBRE_CANAL, completo.nombre)
                    putExtra(ClavesReproductor.LOGOTIPO_CANAL, completo.icono_transmision)
                    putExtra(ClavesReproductor.SERVIDOR_URL, servidor.urlServidor)
                    putExtra(ClavesReproductor.USUARIO, servidor.usuario)
                    putExtra(ClavesReproductor.CONTRASENA, servidor.contrasena)
                }

                val favoritosMapeados = favoritos.map { fav ->
                    val basico = Canal(
                        id_transmision = fav.idTransmision,
                        nombre = fav.nombre,
                        icono_transmision = fav.icono,
                        id_categoria = ""
                    )
                    modeloVista.obtenerCanalCompleto(fav.idTransmision, basico)
                }
                modeloVista.establecerContextoReproduccion(favoritosMapeados)
                modeloVista.agregarAlHistorial(completo)

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
                titulo = "Mis favoritos",
                subtitulo = if (favoritos.isEmpty()) {
                    "Guarda tus canales mas usados para abrirlos rapido"
                } else {
                    "${favoritos.size} canales guardados"
                },
                icono = Icons.Default.FavoriteBorder,
                etiqueta = "${favoritos.size} canales"
            )

            if (favoritos.isEmpty()) {
                EstadoVacioTV(
                    icono = Icons.Default.FavoriteBorder,
                    titulo = "Sin favoritos aún",
                    subtitulo = "Mantén presionado Aceptar sobre un canal para guardarlo aquí.",
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
                        items = favoritos,
                        key = { _, fav -> fav.idTransmision },
                        contentType = { _, _ -> "CanalGrandeTV" }
                    ) { index, fav ->
                        val esPrimero = index == 0
                        val canal = remember(fav.idTransmision) {
                            val canalBase = Canal(
                                id_transmision = fav.idTransmision,
                                nombre = fav.nombre,
                                icono_transmision = fav.icono,
                                id_categoria = ""
                            )
                            modeloVista.obtenerCanalCompleto(fav.idTransmision, canalBase)
                        }
                        val modFocus = if (esPrimero) Modifier.focusRequester(requeridorFocoContenido) else Modifier
                        TarjetaCanalGrandeTV(
                            canal = canal,
                            esFavorito = true,
                            alHacerClick = { reproducirCanal(canal) },
                            alHacerLongClick = {
                                modeloVista.alternarFavorito(canal)
// TODO(KMP):                                 android.widget.Toast.makeText(
                                    contexto,
                                    "Quitado de favoritos",
                                    android.widget.Toast.LENGTH_SHORT
                                ).show()
                            },
                            modificadorExtra = modFocus
                        )
                    }
                }
            }
        }
    }
}
