package com.iptv.fiber.interfaz.ajustes

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

// ── Paleta oficial Fiber Z para Móvil/Tablet ────────────────────────────────
internal val ColorFondoApp      = Color(0xFF000000)
internal val ColorTarjeta       = Color(0xFF0F0F0F)
internal val ColorTarjetaAlta   = Color(0xFF181820)
internal val ColorAcentoAzul    = Color(0xFF4B5EB9)
internal val ColorAmarilloMarca = Color(0xFFBDBE22)
internal val ColorError         = Color(0xFFFF4757)
internal val ColorExito         = Color(0xFF2ECC71)
internal val ColorDivisor       = Color(0xFF1E1E2E)
internal val ColorTextoGris     = Color(0xFF94A3B8)
internal val ColorTextoTenue    = Color(0xFF4A4A6A)

// ── Componente de fila reutilizable ─────────────────────────────────────────
/**
 * Fila de ajuste reutilizable con [icono], [titulo] y [descripcion] opcional.
 * Acepta un [componenteFinal] arbitrario (ej. Switch, Badge); si es null y [mostrarFlecha] es true,
 * muestra una flecha de navegación cuando existe [alHacerClick].
 */
@Composable
fun FilaAjuste(
    icono: ImageVector,
    titulo: String,
    descripcion: String? = null,
    colorIcono: Color = Color.White,
    alHacerClick: (() -> Unit)? = null,
    mostrarFlecha: Boolean = true,
    componenteFinal: @Composable (() -> Unit)? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = alHacerClick != null) { alHacerClick?.invoke() }
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(34.dp)
                .clip(RoundedCornerShape(9.dp))
                .background(colorIcono.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icono,
                contentDescription = null,
                tint = colorIcono,
                modifier = Modifier.size(18.dp)
            )
        }
        Spacer(modifier = Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(titulo, color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Medium)
            if (descripcion != null) {
                Text(descripcion, color = ColorTextoGris, fontSize = 12.sp, lineHeight = 16.sp)
            }
        }
        if (componenteFinal != null) {
            componenteFinal()
        } else if (mostrarFlecha && alHacerClick != null) {
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = ColorTextoTenue,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

// ── Componente encabezado de sección ────────────────────────────────────────
/** Encabezado de sección de ajustes con [icono] y [titulo] en mayúsculas con color [colorIcono]. */
@Composable
fun EncabezadoSeccion(titulo: String, icono: ImageVector, colorIcono: Color = ColorAcentoAzul) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icono, contentDescription = null, tint = colorIcono, modifier = Modifier.size(16.dp))
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = titulo.uppercase(),
            color = colorIcono,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp
        )
    }
}

// ── Diálogo de PIN ───────────────────────────────────────────────────────────
/**
 * Diálogo de teclado numérico para ingresar un PIN de 4 dígitos.
 * Si se proporciona [pinCorrecto], verifica la coincidencia antes de llamar a [alConfirmar].
 * Se adapta automáticamente entre orientación retrato y paisaje.
 */
@Composable
fun DialogoPin(
    titulo: String,
    descripcion: String,
    pinCorrecto: String? = null,
    alConfirmar: (String) -> Unit,
    alCancelar: () -> Unit
) {
    var pinIngresado by remember { mutableStateOf("") }
    var mensajeError by remember { mutableStateOf<String?>(null) }
    val teclas = listOf(
        listOf("1","2","3"),
        listOf("4","5","6"),
        listOf("7","8","9"),
        listOf("⌫","0","✓")
    )

    val configuracion = androidx.compose.ui.platform.LocalConfiguration.current
    val esHorizontal = configuracion.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE

    Dialog(
        onDismissRequest = alCancelar,
        properties = DialogProperties(usePlatformDefaultWidth = false, dismissOnBackPress = true, dismissOnClickOutside = true)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.80f))
                .clickable { alCancelar() },
            contentAlignment = Alignment.Center
        ) {
            if (esHorizontal) {
                // Diseño premium horizontal para landscape (más proporcional)
                Row(
                    modifier = Modifier
                        .width(560.dp)
                        .clip(RoundedCornerShape(24.dp))
                        .background(Brush.verticalGradient(listOf(Color(0xFF1A1A2E), Color(0xFF0F0F1A))))
                        .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(24.dp))
                        .padding(horizontal = 24.dp, vertical = 20.dp)
                        .clickable(enabled = false) {},
                    horizontalArrangement = Arrangement.spacedBy(28.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Columna Izquierda: Icono, Título, Descripción y Cancelar
                    Column(
                        modifier = Modifier.weight(1f),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            Icons.Default.Lock,
                            contentDescription = null,
                            tint = ColorAcentoAzul,
                            modifier = Modifier.size(40.dp)
                        )
                        Text(
                            text = titulo,
                            color = Color.White,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center
                        )
                        Text(
                            text = descripcion,
                            color = ColorTextoGris,
                            fontSize = 12.sp,
                            textAlign = TextAlign.Center,
                            lineHeight = 16.sp
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(40.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color.White.copy(alpha = 0.05f))
                                .border(1.dp, Color.White.copy(alpha = 0.06f), RoundedCornerShape(12.dp))
                                .clickable { alCancelar() },
                            contentAlignment = Alignment.Center
                        ) {
                            Text("Cancelar", color = ColorError, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    // Columna Derecha: Visor de PIN y Teclado Numérico
                    Column(
                        modifier = Modifier.weight(1.2f),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Visor de PIN
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            for (i in 0 until 4) {
                                val relleno = i < pinIngresado.length
                                Box(
                                    modifier = Modifier
                                        .size(38.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(if (relleno) ColorAcentoAzul.copy(alpha = 0.25f) else Color.White.copy(alpha = 0.05f))
                                        .border(1.dp, if (i == pinIngresado.length) ColorAcentoAzul else Color.White.copy(alpha = 0.1f), RoundedCornerShape(10.dp)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (relleno) {
                                        Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(Color.White))
                                    }
                                }
                            }
                        }

                        if (mensajeError != null) {
                            Text(mensajeError!!, color = ColorError, fontSize = 12.sp, textAlign = TextAlign.Center)
                        }

                        // Teclado numérico compacto
                        Column(
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            teclas.forEach { fila ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    fila.forEach { tecla ->
                                        val esAccion = tecla == "⌫" || tecla == "✓"
                                        val fondoTecla = when {
                                            esAccion && tecla == "✓" -> ColorExito.copy(alpha = 0.15f)
                                            esAccion -> ColorError.copy(alpha = 0.12f)
                                            else -> Color.White.copy(alpha = 0.05f)
                                        }
                                        val colorTecla = when {
                                            esAccion && tecla == "✓" -> ColorExito
                                            esAccion -> ColorError
                                            else -> Color.White
                                        }
                                        Box(
                                            modifier = Modifier
                                                .weight(1f)
                                                .height(38.dp)
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(fondoTecla)
                                                .border(1.dp, Color.White.copy(alpha = 0.06f), RoundedCornerShape(8.dp))
                                                .clickable {
                                                    when (tecla) {
                                                        "⌫" -> { if (pinIngresado.isNotEmpty()) { pinIngresado = pinIngresado.dropLast(1); mensajeError = null } }
                                                        "✓" -> {
                                                            when {
                                                                pinIngresado.length < 4 -> mensajeError = "Debe tener 4 dígitos"
                                                                pinCorrecto != null && pinIngresado != pinCorrecto -> { mensajeError = "PIN incorrecto"; pinIngresado = "" }
                                                                else -> { mensajeError = null; alConfirmar(pinIngresado) }
                                                            }
                                                        }
                                                        else -> { if (pinIngresado.length < 4) { pinIngresado += tecla; mensajeError = null } }
                                                    }
                                                },
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(tecla, color = colorTecla, fontSize = 16.sp, fontWeight = if (esAccion) FontWeight.Bold else FontWeight.Normal)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            } else {
                // Diseño original vertical para retrato (portrait)
                Column(
                    modifier = Modifier
                        .width(320.dp)
                        .clip(RoundedCornerShape(24.dp))
                        .background(Brush.verticalGradient(listOf(Color(0xFF1A1A2E), Color(0xFF0F0F1A))))
                        .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(24.dp))
                        .padding(24.dp)
                        .clickable(enabled = false) {},
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(Icons.Default.Lock, contentDescription = null, tint = ColorAcentoAzul, modifier = Modifier.size(40.dp))
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(titulo, color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(descripcion, color = ColorTextoGris, fontSize = 13.sp, textAlign = TextAlign.Center)
                    Spacer(modifier = Modifier.height(20.dp))

                    // Visor de PIN
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        for (i in 0 until 4) {
                            val relleno = i < pinIngresado.length
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(if (relleno) ColorAcentoAzul.copy(alpha = 0.25f) else Color.White.copy(alpha = 0.05f))
                                    .border(1.dp, if (i == pinIngresado.length) ColorAcentoAzul else Color.White.copy(alpha = 0.1f), RoundedCornerShape(12.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                if (relleno) {
                                    Box(modifier = Modifier.size(12.dp).clip(CircleShape).background(Color.White))
                                }
                            }
                        }
                    }

                    if (mensajeError != null) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(mensajeError!!, color = ColorError, fontSize = 12.sp)
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Teclado numérico
                    teclas.forEach { fila ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            fila.forEach { tecla ->
                                val esAccion = tecla == "⌫" || tecla == "✓"
                                val fondoTecla = when {
                                    esAccion && tecla == "✓" -> ColorExito.copy(alpha = 0.15f)
                                    esAccion -> ColorError.copy(alpha = 0.12f)
                                    else -> Color.White.copy(alpha = 0.05f)
                                }
                                val colorTecla = when {
                                    esAccion && tecla == "✓" -> ColorExito
                                    esAccion -> ColorError
                                    else -> Color.White
                                }
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(52.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(fondoTecla)
                                        .border(1.dp, Color.White.copy(alpha = 0.06f), RoundedCornerShape(12.dp))
                                        .clickable {
                                            when (tecla) {
                                                "⌫" -> { if (pinIngresado.isNotEmpty()) { pinIngresado = pinIngresado.dropLast(1); mensajeError = null } }
                                                "✓" -> {
                                                    when {
                                                        pinIngresado.length < 4 -> mensajeError = "Debe tener 4 dígitos"
                                                        pinCorrecto != null && pinIngresado != pinCorrecto -> { mensajeError = "PIN incorrecto"; pinIngresado = "" }
                                                        else -> { mensajeError = null; alConfirmar(pinIngresado) }
                                                    }
                                                }
                                                else -> { if (pinIngresado.length < 4) { pinIngresado += tecla; mensajeError = null } }
                                            }
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(tecla, color = colorTecla, fontSize = 20.sp, fontWeight = if (esAccion) FontWeight.Bold else FontWeight.Normal)
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                    }

                    TextButton(onClick = alCancelar) {
                        Text("Cancelar", color = ColorTextoGris, fontSize = 14.sp)
                    }
                }
            }
        }
    }
}
