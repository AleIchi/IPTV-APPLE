package com.iptv.fiber.interfaz.principal

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.iptv.fiber.datos.modelo.Categoria
import com.iptv.fiber.datos.repositorio.RepositorioAutenticacion
import com.iptv.fiber.interfaz.componentes.TarjetaCanal
import com.iptv.fiber.interfaz.modelovista.ModeloVistaContenido
import com.iptv.fiber.interfaz.tema.*

@Composable
fun PantallaTvEnVivo(
    modeloVista: ModeloVistaContenido,
    repositorioAuth: RepositorioAutenticacion,
    alHacerBack: () -> Unit = {}
) {
    val contexto = LocalContext.current
    val canalesFiltrados by modeloVista.canalesFiltrados.collectAsState()
    val idCategoriaSeleccionada by modeloVista.idCategoriaSeleccionada.collectAsState()
    val categorias by modeloVista.categoriasEnVivo.collectAsState()
    val estaCargando by modeloVista.estaCargando.collectAsState()
    val error by modeloVista.error.collectAsState()
    val consultaBusqueda by modeloVista.consultaBusqueda.collectAsState()
    val favoritos by modeloVista.favoritos.collectAsState()
    val epg by modeloVista.epg.collectAsState()
    
    var datosInicialesCargados by remember { mutableStateOf(false) }
    val solicitanteFoco = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        if (!datosInicialesCargados) {
            modeloVista.cargarCategoriasEnVivo()
            modeloVista.cargarCanalesEnVivo()
            modeloVista.cargarEPG() // Cargar la guía para todos los canales
            modeloVista.iniciarObservacionDatosUsuario()
            datosInicialesCargados = true
        }
    }

    val alcance = rememberCoroutineScope()
    val reproducirCanal = construirLambdaReproduccionCanal(contexto, repositorioAuth, modeloVista, alcance, canalesFiltrados)

    Box(modifier = Modifier.fillMaxSize().background(FondoPremium)) {
        Column(modifier = Modifier.fillMaxSize().padding(horizontal = 8.dp, vertical = 16.dp)) {
            // Cabecera con botón de atrás
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(bottom = 20.dp)) {
                IconButton(
                    onClick = { alHacerBack() },
                    modifier = Modifier.size(48.dp).background(SuperficiePremium, RoundedCornerShape(16.dp))
                ) {
                    Icon(Icons.Default.ArrowBack, "Volver", tint = Color.White)
                }
                Spacer(modifier = Modifier.width(16.dp))
                Text("TV en Vivo", style = MaterialTheme.typography.headlineMedium, color = TextoPrimarioPremium, fontWeight = FontWeight.ExtraBold)
            }

            // Buscador con auto-enfoque
            OutlinedTextField(
                value = consultaBusqueda,
                onValueChange = { modeloVista.buscar(it) },
                modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp).focusRequester(solicitanteFoco),
                placeholder = { Text("Buscar canal...", color = TextoSecundarioPremium) },
                leadingIcon = { Icon(Icons.Default.Search, "Buscar", tint = TextoSecundarioPremium) },
                trailingIcon = if (consultaBusqueda.isNotEmpty()) {
                    {
                        IconButton(onClick = { modeloVista.buscar("") }) {
                            Icon(Icons.Default.Close, "Limpiar", tint = TextoSecundarioPremium)
                        }
                    }
                } else null,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = AcentoPremium,
                    unfocusedBorderColor = SuperficiePremiumClaro,
                    focusedTextColor = TextoPrimarioPremium,
                    unfocusedTextColor = TextoPrimarioPremium,
                    cursorColor = AcentoPremium,
                    focusedLabelColor = AcentoPremium,
                    unfocusedLabelColor = TextoSecundarioPremium,
                    focusedContainerColor = SuperficiePremium,
                    unfocusedContainerColor = SuperficiePremium
                ),
                shape = RoundedCornerShape(16.dp),
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search)
            )

            // Mensaje de error
            error?.let { msg ->
                Card(modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)) {
                    Row(modifier = Modifier.fillMaxWidth().padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically) {
                        Text(msg, color = MaterialTheme.colorScheme.onErrorContainer, modifier = Modifier.weight(1f))
                        IconButton(onClick = { modeloVista.limpiarError() }) {
                            Icon(Icons.Default.Close, "Cerrar")
                        }
                    }
                }
            }

            // Chips de categorías
            if (categorias.isNotEmpty()) {
                CategoriasDesplazables(
                    categorias = categorias,
                    idCategoriaSeleccionada = idCategoriaSeleccionada,
                    alSeleccionarCategoria = {
                        modeloVista.seleccionarCategoria(if (idCategoriaSeleccionada == it) null else it)
                        modeloVista.buscar("")
                    }
                )
                Spacer(modifier = Modifier.height(12.dp))
            }

            // Título de sección actual
            Text(
                text = if (idCategoriaSeleccionada != null) 
                    (categorias.find { it.id_categoria == idCategoriaSeleccionada }?.nombre_categoria ?: "Canales")
                    else "Todos los Canales",
                style = MaterialTheme.typography.titleLarge,
                color = TextoPrimarioPremium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            // Contenido principal
            when {
                estaCargando && canalesFiltrados.isEmpty() ->
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = AcentoPremium)
                    }
                canalesFiltrados.isEmpty() ->
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.LiveTv, null, modifier = Modifier.size(64.dp), tint = TextoSecundarioPremium)
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                if (consultaBusqueda.isNotBlank()) "Sin resultados para \"$consultaBusqueda\"" else "No hay canales disponibles",
                                style = MaterialTheme.typography.bodyLarge, color = TextoSecundarioPremium
                            )
                        }
                    }
                else -> {
                    val idFavoritos = remember(favoritos) { modeloVista.favoritos.value.map { it.idTransmision }.toSet() }
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxSize()) {
                        items(
                            items = canalesFiltrados,
                            key = { canal: com.iptv.fiber.datos.modelo.Canal -> canal.id_transmision }
                        ) { canal: com.iptv.fiber.datos.modelo.Canal ->
                            // Buscar programa actual para este canal
                            val programaActual = epg[canal.id_transmision.toString()]?.firstOrNull()
                            
                            TarjetaCanal(
                                canal = canal, 
                                programaActual = programaActual,
                                alHacerClick = { reproducirCanal(canal) },
                                esFavorito = idFavoritos.contains(canal.id_transmision),
                                alHacerTapFavorito = { modeloVista.alternarFavorito(canal) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CategoriasDesplazables(
    categorias: List<Categoria>,
    idCategoriaSeleccionada: String?,
    alSeleccionarCategoria: (String?) -> Unit
) {
    val todas = listOf(Categoria(id_categoria = "", nombre_categoria = "Todos", id_padre = "0")) + categorias
    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
        items(todas) { cat ->
            val seleccionado = idCategoriaSeleccionada == cat.id_categoria || (idCategoriaSeleccionada == null && cat.id_categoria == "")
            FilterChip(
                selected = seleccionado,
                onClick = { alSeleccionarCategoria(if (cat.id_categoria == "") null else cat.id_categoria) },
                label = { Text(cat.nombre_categoria) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = AcentoPremium, selectedLabelColor = FondoPremium,
                    containerColor = SuperficiePremium, labelColor = TextoSecundarioPremium
                ),
                border = FilterChipDefaults.filterChipBorder(
                    borderColor = Color.Transparent,
                    selectedBorderColor = Color.Transparent
                )
            )
        }
    }
}

