package com.iptv.fiber.tv.componentes

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.iptv.fiber.interfaz.tema.AcentoPremium

object TemaTV {
    val Fondo = Color(0xFF070812)
    val FondoElevado = Color(0xFF111426)
    val Superficie = Color(0xFF171A2D)
    val SuperficieSuave = Color(0xFF20243A)
    val Linea = Color.White.copy(alpha = 0.08f)
    val TextoPrincipal = Color.White
    val TextoSecundario = Color.White.copy(alpha = 0.64f)
    val TextoTenue = Color.White.copy(alpha = 0.38f)
    val Acento = AcentoPremium
    val AcentoClaro = Color(0xFF7286FF)
    val Favorito = Color(0xFFFF2D55)
    val Advertencia = Color(0xFFFFB547)
    val Exito = Color(0xFF2ED573)
    val Peligro = Color(0xFFFF4757)

    val RedondeoTarjeta = 12.dp
    val RedondeoControl = 14.dp
    val RedondeoPanel = 22.dp
    val MargenPantalla = 48.dp
    val AltoHeader = 88.dp
    val AltoBuscador = 48.dp
    val AnchoTarjetaCanal = 184.dp
    val AltoTarjetaCanal = 126.dp
    val EspacioGrid = 18.dp

    val TituloPantalla = 28.sp
    val Subtitulo = 13.sp
    val TextoControl = 14.sp
    val TextoTarjeta = 12.sp

    val FondoPrincipal = Brush.verticalGradient(
        colors = listOf(Color(0xFF0D1020), Fondo)
    )

    val FondoPanel = Brush.verticalGradient(
        colors = listOf(FondoElevado, Color(0xFF0D0F1C))
    )

    val FondoMenu = Brush.verticalGradient(
        colors = listOf(Color(0xFF151936), Color(0xFF090B16))
    )

    val FondoHero = Brush.verticalGradient(
        colors = listOf(Color(0x00070812), Fondo)
    )
}
