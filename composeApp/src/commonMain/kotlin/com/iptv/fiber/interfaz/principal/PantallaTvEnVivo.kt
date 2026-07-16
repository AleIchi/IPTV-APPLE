package com.iptv.fiber.interfaz.principal

// removed android import: import android.content.Intent
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
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

/**
 * LA PANTALLA DE CANALES (TV en vivo).
 * Aquí se muestran todos los canales. La pantalla es tan inteligente que:
 * 1. Muestra una barra de búsqueda para filtrar canales.
 * 2. Muestra botones (chips) para filtrar por categoría (Ej: "Deportes").
 * 3. Usa "LazyColumn", que es un componente que recicla la memoria. Si tienes 5,000 canales,
 *    solo dibuja los 10 que caben en la pantalla y borra los demás, para que el celular no explote.
 */
@Composable
fun PantallaTvEnVivo(
    modeloVista: ModeloVistaContenido,
    repositorioAuth: RepositorioAutenticacion,
    alHacerBack: () -> Unit = {}
) {
    val contexto = LocalContext.current
    val canalesFiltrados by modeloVista.canalesFiltrados.collectAsStateWithLifecycle()
    val idCategoriaSeleccionada by modeloVista.idCategoriaSeleccionada.collectAsStateWithLifecycle()
    val categorias by modeloVista.categoriasEnVivo.collectAsStateWithLifecycle()
    val estaCargando by modeloVista.estaCargando.collectAsStateWithLifecycle()
    val error by modeloVista.error.collectAsStateWithLifecycle()
    val consultaBusqueda by modeloVista.consultaBusqueda.collectAsStateWithLifecycle()
    val favoritos by modeloVista.favoritos.collectAsStateWithLifecycle()
    val epg by modeloVista.epg.collectAsStateWithLifecycle()
    
    var datosInicialesCargados by remember { mutableStateOf(false) }
    val solicitanteFoco = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        if (!datosInicialesCargados) {
            modeloVista.iniciarCargaCompleta()
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
                Text("TV en vivo", style = MaterialTheme.typography.headlineMedium, color = TextoPrimarioPremium, fontWeight = FontWeight.ExtraBold)
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
                    val idFavoritos = remember(favoritos) { favoritos.map { it.idTransmision }.toSet() }
                    
                    // LAZYCOLUMN = RECICLAJE EFICIENTE
                    // Crea una lista vertical infinita. A medida que el usuario hace scroll hacia abajo,
                    // va destruyendo las tarjetas de arriba y creando las de abajo.
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxSize()) {
                        items(
                            items = canalesFiltrados,
                            key = { it.id_transmision },
                            contentType = { "canal" }
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

/** 
 * FILA DE CATEGORÍAS (LazyRow)
 * En lugar de dibujar una lista hacia abajo (como los canales), "LazyRow" dibuja los botones
 * hacia la derecha, permitiendo deslizar el dedo horizontalmente.
 */
@Composable
fun CategoriasDesplazables(
    categorias: List<Categoria>,
    idCategoriaSeleccionada: String?,
    alSeleccionarCategoria: (String?) -> Unit
) {
    val todas = listOf(Categoria(id_categoria = "", nombre_categoria = "Todos", id_padre = "0")) + categorias
    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
        items(todas, key = { it.id_categoria }) { cat ->
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
