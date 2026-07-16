package com.iptv.fiber.interfaz.reproductor

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.iptv.fiber.interfaz.tema.*

/** Hoja inferior de ajustes del reproductor: velocidad de reproducción y modo de escalado de pantalla. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HojaAjustesReproductor(
    mostrar: Boolean,
    alCerrar: () -> Unit,
    estadoHoja: SheetState,
    velocidadActual: Float,
    modoEscalado: Int,
    nombreCanal: String,
    alCambiarVelocidad: (Float) -> Unit,
    alCambiarEscalado: (Int) -> Unit
) {
    val manejadorUris = androidx.compose.ui.platform.LocalUriHandler.current

    if (mostrar) {
        ModalBottomSheet(
            onDismissRequest = alCerrar,
            sheetState = estadoHoja,
            containerColor = SuperficiePremium
        ) {
            Column(modifier = Modifier.fillMaxWidth().padding(16.dp).padding(bottom = 32.dp)) {
                Text(
                    text = "Ajustes",
                    style = MaterialTheme.typography.titleLarge,
                    color = TextoPrimarioPremium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
                
                // Ajuste de Pantalla
                ListItem(
                    headlineContent = { Text("Ajuste de pantalla", color = TextoPrimarioPremium) },
                    trailingContent = { 
                        Text(
                            text = when(modoEscalado) {
                                1 -> "Rellenar"
                                2 -> "Estirar"
                                else -> "Ajustar"
                            }, 
                            color = TextoSecundarioPremium
                        ) 
                    },
                    leadingContent = { Icon(Icons.Default.Fullscreen, null, tint = TextoSecundarioPremium) },
                    modifier = Modifier.clickable { 
                        val nuevoModo = (modoEscalado + 1) % 3
                        alCambiarEscalado(nuevoModo)
                        com.iptv.fiber.mostrarMensaje("Modo: " + when(nuevoModo) {
                            1 -> "Rellenar"
                            2 -> "Estirar"
                            else -> "Ajustar"
                        })
                    },
                    colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                )
                Divider(color = Color.White.copy(alpha = 0.1f))
                
                // Velocidad
                ListItem(
                    headlineContent = { Text("Velocidad", color = TextoPrimarioPremium) },
                    trailingContent = { Text(if (velocidadActual == 1.0f) "Estándar" else "${velocidadActual}x", color = TextoSecundarioPremium) },
                    leadingContent = { Icon(Icons.Default.Speed, null, tint = TextoSecundarioPremium) },
                    modifier = Modifier.clickable { 
                        val nuevaVelocidad = when (velocidadActual) {
                            1.0f -> 1.5f
                            1.5f -> 2.0f
                            2.0f -> 0.5f
                            else -> 1.0f
                        }
                        alCambiarVelocidad(nuevaVelocidad)
                        com.iptv.fiber.mostrarMensaje("Velocidad: ${if (nuevaVelocidad == 1.0f) "Estándar" else "${nuevaVelocidad}x"}")
                    },
                    colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                )
                Divider(color = Color.White.copy(alpha = 0.1f))
                
                // Reportar
                ListItem(
                    headlineContent = { Text("Reportar canal", color = TextoPrimarioPremium) },
                    leadingContent = { Icon(Icons.Default.Flag, null, tint = TextoSecundarioPremium) },
                    modifier = Modifier.clickable { 
                        try {
                            val mensaje = "Hola, quiero reportar una falla en el canal $nombreCanal en la aplicación Fiber Z."
                            val url = "https://wa.me/51982497670?text=${mensaje.replace(" ", "%20")}"
                            manejadorUris.openUri(url)
                        } catch (e: Exception) {
                            com.iptv.fiber.mostrarMensaje("No se pudo abrir WhatsApp")
                        }
                        alCerrar()
                    },
                    colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                )
            }
        }
    }
}
