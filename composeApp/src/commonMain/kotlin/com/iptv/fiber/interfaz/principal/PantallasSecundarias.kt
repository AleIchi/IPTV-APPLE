package com.iptv.fiber.interfaz.principal

// removed android import: import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.iptv.fiber.datos.modelo.Canal
import com.iptv.fiber.datos.repositorio.RepositorioAutenticacion
import com.iptv.fiber.interfaz.componentes.TarjetaCanal
import com.iptv.fiber.interfaz.modelovista.ModeloVistaContenido
import com.iptv.fiber.interfaz.reproductor.ActividadReproductor
import com.iptv.fiber.interfaz.reproductor.ClavesReproductor
import com.iptv.fiber.interfaz.tema.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

// ─── Favoritos ────────────────────────────────────────────────────────────────

/** Muestra la lista de canales marcados como favoritos con opción de reproducir o quitar el marcador. */
@Composable
fun PantallaFavoritos(
    modeloVista: ModeloVistaContenido, 
    repositorioAuth: RepositorioAutenticacion,
    alNavegarReproductor: (String) -> Unit
) {
    val favoritos by modeloVista.favoritos.collectAsStateWithLifecycle()
    val alcance = rememberCoroutineScope()

    LaunchedEffect(Unit) { modeloVista.iniciarObservacionDatosUsuario() }

    val reproducirCanal = construirLambdaReproduccion(alcance, modeloVista, alNavegarReproductor) {
        favoritos.map { fav ->
            val basico = Canal(id_transmision = fav.idTransmision, nombre = fav.nombre, icono_transmision = fav.icono, id_categoria = "")
            modeloVista.obtenerCanalCompleto(fav.idTransmision, basico)
        }
    }

    PantallaListaCanales(
        titulo = "Tus Favoritos",
        mensajeVacio = "No tienes canales favoritos aún.",
        estaVacio = favoritos.isEmpty()
    ) {
        items(favoritos) { fav ->
            val canal = Canal(id_transmision = fav.idTransmision, nombre = fav.nombre, icono_transmision = fav.icono, id_categoria = "")
            TarjetaCanal(canal = canal, alHacerClick = { reproducirCanal(canal) },
                esFavorito = true, alHacerTapFavorito = { modeloVista.alternarFavorito(canal) })
        }
    }
}

// ─── Historial ────────────────────────────────────────────────────────────────

/** Muestra los canales del historial de reproducción con opción de volver a reproducir o marcar como favorito. */
@Composable
fun PantallaHistorial(
    modeloVista: ModeloVistaContenido,
    alNavegarReproductor: (String) -> Unit
) {
    val historial by modeloVista.historial.collectAsStateWithLifecycle()
    val favoritos by modeloVista.favoritos.collectAsStateWithLifecycle()
    val alcance = rememberCoroutineScope()

    LaunchedEffect(Unit) { modeloVista.iniciarObservacionDatosUsuario() }

    val reproducirCanal = construirLambdaReproduccion(alcance, modeloVista, alNavegarReproductor) {
        historial.map { h ->
            val basico = Canal(id_transmision = h.idTransmision, nombre = h.nombre, icono_transmision = h.icono, id_categoria = "")
            modeloVista.obtenerCanalCompleto(h.idTransmision, basico)
        }
    }

    val idFavoritos = remember(favoritos) { favoritos.map { it.idTransmision }.toSet() }

    PantallaListaCanales(
        titulo = "Historial de Vistos",
        mensajeVacio = "No has visto ningún canal recientemente.",
        estaVacio = historial.isEmpty()
    ) {
        items(historial) { hist ->
            val canal = Canal(id_transmision = hist.idTransmision, nombre = hist.nombre, icono_transmision = hist.icono, id_categoria = "")
            TarjetaCanal(canal = canal, alHacerClick = { reproducirCanal(canal) },
                esFavorito = idFavoritos.contains(canal.id_transmision),
                alHacerTapFavorito = { modeloVista.alternarFavorito(canal) })
        }
    }
}

// ─── Ayudantes ────────────────────────────────────────────────────────────────

/**
 * Construye la lambda de reproducción para Favoritos e Historial.
 * [proveedorContexto] es una función suspendida que devuelve la lista completa para la navegación prev/next.
 */
private fun construirLambdaReproduccion(
    alcance: kotlinx.coroutines.CoroutineScope,
    modeloVista: ModeloVistaContenido,
    alNavegarReproductor: (String) -> Unit,
    proveedorContexto: suspend () -> List<Canal>
): (Canal) -> Unit = { canal ->
    alcance.launch {
        modeloVista.establecerContextoReproduccion(proveedorContexto())
        val completo = modeloVista.obtenerCanalCompleto(canal.id_transmision, canal)
        alNavegarReproductor(completo.id_transmision.toString())
        modeloVista.agregarAlHistorial(canal)
    }
}

/** Estructura reutilizable de pantalla con [titulo], mensaje vacío cuando no hay datos, y un [contenido] de lista. */
@Composable
fun PantallaListaCanales(
    titulo: String,
    mensajeVacio: String,
    estaVacio: Boolean,
    contenido: androidx.compose.foundation.lazy.LazyListScope.() -> Unit
) {
    Column(modifier = Modifier.fillMaxSize().background(FondoPremium).padding(16.dp)) {
        Text(titulo, style = MaterialTheme.typography.headlineMedium, color = TextoPrimarioPremium, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(16.dp))
        if (estaVacio) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(mensajeVacio, color = TextoSecundarioPremium)
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxSize(), content = contenido)
        }
    }
}
