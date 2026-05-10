package com.iptv.fiber.interfaz.principal

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
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
import com.iptv.fiber.interfaz.tema.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

// ─── Favoritos ────────────────────────────────────────────────────────────────

@Composable
fun PantallaFavoritos(modeloVista: ModeloVistaContenido, repositorioAuth: RepositorioAutenticacion) {
    val favoritos by modeloVista.favoritos.collectAsState()
    val contexto = LocalContext.current
    val alcance = rememberCoroutineScope()

    LaunchedEffect(Unit) { modeloVista.iniciarObservacionDatosUsuario() }

    val reproducirCanal = construirLambdaReproduccion(contexto, alcance, repositorioAuth, modeloVista) {
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

@Composable
fun PantallaHistorial(modeloVista: ModeloVistaContenido, repositorioAuth: RepositorioAutenticacion) {
    val historial by modeloVista.historial.collectAsState()
    val favoritos by modeloVista.favoritos.collectAsState()
    val contexto = LocalContext.current
    val alcance = rememberCoroutineScope()

    LaunchedEffect(Unit) { modeloVista.iniciarObservacionDatosUsuario() }

    val reproducirCanal = construirLambdaReproduccion(contexto, alcance, repositorioAuth, modeloVista) {
        historial.map { h ->
            val basico = Canal(id_transmision = h.idTransmision, nombre = h.nombre, icono_transmision = h.icono, id_categoria = "")
            modeloVista.obtenerCanalCompleto(h.idTransmision, basico)
        }
    }

    PantallaListaCanales(
        titulo = "Historial de Vistos",
        mensajeVacio = "No has visto ningún canal recientemente.",
        estaVacio = historial.isEmpty()
    ) {
        items(historial) { hist ->
            val canal = Canal(id_transmision = hist.idTransmision, nombre = hist.nombre, icono_transmision = hist.icono, id_categoria = "")
            TarjetaCanal(canal = canal, alHacerClick = { reproducirCanal(canal) },
                esFavorito = favoritos.any { it.idTransmision == canal.id_transmision },
                alHacerTapFavorito = { modeloVista.alternarFavorito(canal) })
        }
    }
}

// ─── Helpers ──────────────────────────────────────────────────────────────────

private fun construirLambdaReproduccion(
    contexto: android.content.Context,
    alcance: kotlinx.coroutines.CoroutineScope,
    repositorioAuth: RepositorioAutenticacion,
    modeloVista: ModeloVistaContenido,
    proveedorContexto: suspend () -> List<Canal>
): (Canal) -> Unit = { canal ->
    alcance.launch {
        modeloVista.establecerContextoReproduccion(proveedorContexto())
        val servidor = repositorioAuth.servidorActual.first() ?: return@launch
        val completo = modeloVista.obtenerCanalCompleto(canal.id_transmision, canal)
        val url = if (!completo.fuenteDirecta.isNullOrEmpty()) completo.fuenteDirecta
        else repositorioAuth.construirUrlStream(servidor.urlServidor, servidor.usuario, servidor.contrasena, "live", completo.id_transmision)
        contexto.startActivity(Intent(contexto, ActividadReproductor::class.java).apply {
            putExtra("url_transmision", url)
            putExtra("id_transmision", canal.id_transmision)
            putExtra("tipo_transmision", "live")
            putExtra("nombre_canal", canal.nombre)
            putExtra("logo_canal", canal.icono_transmision)
            putExtra("servidor_url", servidor.urlServidor)
            putExtra("usuario", servidor.usuario)
            putExtra("contrasena", servidor.contrasena)
        })
        modeloVista.agregarAlHistorial(canal)
    }
}

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
