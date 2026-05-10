package com.iptv.fiber.tv.pantallas

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.iptv.fiber.datos.modelo.Canal
import com.iptv.fiber.datos.repositorio.RepositorioAutenticacion
import com.iptv.fiber.interfaz.modelovista.ModeloVistaContenido
import com.iptv.fiber.interfaz.reproductor.ActividadReproductor
import com.iptv.fiber.interfaz.tema.AcentoPremium
import kotlinx.coroutines.launch

@Composable
fun FavoritosTV(
    modeloVista: ModeloVistaContenido,
    repositorioAuth: RepositorioAutenticacion
) {
    val favoritos by modeloVista.favoritos.collectAsState()
    val contexto = LocalContext.current
    val alcance = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        modeloVista.iniciarObservacionDatosUsuario()
        modeloVista.cargarFavoritos()
    }

    val reproducirCanal: (Canal) -> Unit = { canal ->
        val servidor = repositorioAuth.servidorActual.value
        if (servidor != null) {
            val urlStream = if (!canal.fuenteDirecta.isNullOrEmpty()) canal.fuenteDirecta
            else repositorioAuth.construirUrlStream(servidor.urlServidor, servidor.usuario, servidor.contrasena, "live", canal.id_transmision)
            
            val intent = Intent(contexto, ActividadReproductor::class.java).apply {
                putExtra("url_transmision", urlStream)
                putExtra("id_transmision", canal.id_transmision)
                putExtra("tipo_transmision", "live")
                putExtra("nombre_canal", canal.nombre)
                putExtra("logo_canal", canal.icono_transmision)
                putExtra("servidor_url", servidor.urlServidor)
                putExtra("usuario", servidor.usuario)
                putExtra("contrasena", servidor.contrasena)
            }
            alcance.launch {
                val favoritosMapeados = favoritos.map { fav ->
                    Canal(id_transmision = fav.idTransmision, nombre = fav.nombre, icono_transmision = fav.icono, id_categoria = "")
                }
                modeloVista.establecerContextoReproduccion(favoritosMapeados)
                modeloVista.agregarAlHistorial(canal)
            }
            contexto.startActivity(intent)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0A0A14))
            .padding(horizontal = 32.dp, vertical = 28.dp)
    ) {
        Text(
            text = "Mis Favoritos",
            color = Color.White,
            fontSize = 26.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = if (favoritos.isEmpty()) "Agrega canales a favoritos para acceder rápido" else "${favoritos.size} canales guardados",
            color = Color.White.copy(alpha = 0.35f),
            fontSize = 13.sp
        )
        Spacer(modifier = Modifier.height(12.dp))
        Divider(color = Color.White.copy(alpha = 0.04f), thickness = 1.dp)
        Spacer(modifier = Modifier.height(20.dp))

        if (favoritos.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Default.FavoriteBorder,
                        contentDescription = null,
                        tint = Color.White.copy(alpha = 0.15f),
                        modifier = Modifier.size(64.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Sin favoritos aún",
                        color = Color.White.copy(alpha = 0.4f),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Usa el ícono ♥ en el reproductor para guardar canales",
                        color = Color.White.copy(alpha = 0.2f),
                        fontSize = 13.sp
                    )
                }
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 180.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp),
                horizontalArrangement = Arrangement.spacedBy(20.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(items = favoritos) { fav ->
                    val canal = Canal(
                        id_transmision = fav.idTransmision,
                        nombre = fav.nombre,
                        icono_transmision = fav.icono,
                        id_categoria = ""
                    )
                    TarjetaCanalGrandeTV(
                        canal = canal,
                        alHacerClick = { reproducirCanal(canal) }
                    )
                }
            }
        }
    }
}
