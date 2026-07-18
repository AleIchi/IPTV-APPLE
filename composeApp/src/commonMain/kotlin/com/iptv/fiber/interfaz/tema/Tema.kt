package com.iptv.fiber.interfaz.tema

// removed android import: import android.app.Activity
// removed android import: import android.os.Build
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import kotlinx.coroutines.delay
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.iptv.fiber.datos.utilidades.MonitorRed

private val EsquemaColorOscuroPremium = darkColorScheme(
    primary = AcentoPremium,
    secondary = SecundarioPremium,
    tertiary = SuperficiePremiumClaro,
    background = FondoPremium,
    surface = SuperficiePremium,
    onPrimary = FondoPremium,
    onSecondary = Color.White,
    onTertiary = Color.White,
    onBackground = TextoPrimarioPremium,
    onSurface = TextoPrimarioPremium,
    error = ErrorPremium
)

/** Aplica el tema visual de la app (colores, tipografía, barra de estado) y muestra [AdvertenciaConexionRed] si no hay red. */
@Composable
fun TemaIPTVFiber(
    temaPreferido: String = "clasico",
    esTemaOscuro: Boolean = isSystemInDarkTheme(),
    colorDinamico: Boolean = false,
    contenido: @Composable () -> Unit
) {
    val temaSeleccionado = remember(temaPreferido) {
        TemaApp.entries.firstOrNull { it.name.lowercase() == temaPreferido.lowercase() } ?: TemaApp.CLASICO
    }

    val esquemaColor = when {
        temaSeleccionado != TemaApp.CLASICO -> obtenerEsquemaColorTema(temaSeleccionado)
        esTemaOscuro -> EsquemaColorOscuroPremium
        else -> EsquemaColorOscuroPremium
    }

    val monitorRed = remember { MonitorRed() }
    val estaConectado by monitorRed.estaConectado.collectAsState(initial = true)

    MaterialTheme(
        colorScheme = esquemaColor,
        typography = Tipografia,
        content = {
            Box(modifier = Modifier.fillMaxSize()) {
                contenido()

                // Aviso elegante de red en la parte superior central
                AdvertenciaConexionRed(estaConectado = estaConectado)
            }
        }
    )
}

/** Banner animado que aparece en la parte superior cuando [estaConectado] es `false`.
 *  Se oculta en cuanto vuelve la conexión, o automáticamente a los 10 segundos. */
@Composable
fun AdvertenciaConexionRed(estaConectado: Boolean) {
    var mostrar by remember { mutableStateOf(false) }

    LaunchedEffect(estaConectado) {
        if (!estaConectado) {
            mostrar = true
            delay(10_000)
            mostrar = false
        } else {
            mostrar = false
        }
    }

    AnimatedVisibility(
        visible = mostrar,
        enter = slideInVertically(initialOffsetY = { -it }) + fadeIn(),
        exit = slideOutVertically(targetOffsetY = { -it }) + fadeOut(),
        modifier = Modifier.fillMaxWidth()
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp, start = 16.dp, end = 16.dp),
            contentAlignment = Alignment.TopCenter
        ) {
            Row(
                modifier = Modifier
                    .wrapContentWidth()
                    .clip(RoundedCornerShape(24.dp))
                    .background(
                        Brush.linearGradient(
                            colors = listOf(Color(0xFFE53935), Color(0xFFC62828)) // Rojo intenso premium
                        )
                    )
                    .border(1.dp, Color(0x33FFFFFF), RoundedCornerShape(24.dp))
                    .padding(horizontal = 20.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = "Sin conexión",
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    text = "Sin conexión a Internet",
                    color = Color.White,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
