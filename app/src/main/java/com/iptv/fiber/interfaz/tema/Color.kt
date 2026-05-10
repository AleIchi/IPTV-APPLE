package com.iptv.fiber.interfaz.tema

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

// Colores Base
val Purpura80 = Color(0xFFD0BCFF)
val PurpuraGris80 = Color(0xFFCCC2DC)
val Rosa80 = Color(0xFFEFB8C8)

val Purpura40 = Color(0xFF6650a4)
val PurpuraGris40 = Color(0xFF625b71)
val Rosa40 = Color(0xFF7D5260)

// Paleta de Tema Ultra-Premium (Concepto Carbon & Blue)
val FondoPremium = Color(0xFF000000) // Negro Absoluto (OLED)
val SuperficiePremium = Color(0xFF121212)    // Grafito oscuro
val SuperficiePremiumClaro = Color(0xFF1E1E1E) // Gris carbón
val AcentoPremium = Color(0xFF4B5EB9)     // Azul Corporativo Fiber Z (Protagonista)
val SecundarioPremium = Color(0xFF6C5DD3)  // Púrpura elegante
val DetallePremium = Color(0xFFBDBE22)     // Amarillo corporativo suavizado (Mate)
val ErrorPremium = Color(0xFFFF4757)
val ExitoPremium = Color(0xFF2ED573)
val TextoPrimarioPremium = Color(0xFFFFFFFF)
val TextoSecundarioPremium = Color(0xFF94A3B8) // Slate suave para lectura cómoda

// Degradados Profesionales
val DegradadoPrimarioPremium = Brush.linearGradient(
    colors = listOf(Color(0xFF4B5EB9), Color(0xFF2D3A8C)) // Azul Corporativo
)

val DegradadoSecundarioPremium = Brush.linearGradient(
    colors = listOf(Color(0xFF1E1E1E), Color(0xFF121212)) // Degradado Carbono
)

val DegradadoOscuroPremium = Brush.verticalGradient(
    colors = listOf(Color(0x00000000), Color(0xFF000000))
)

// Colores por Categoría
val ColorTvEnVivo = Color(0xFF00E5FF)
val ColorPeliculas = Color(0xFFFF4757)
val ColorSerie    = Color(0xFFFFA502)
val ColorEpg      = Color(0xFF2ED573)
val ColorAjustes  = Color(0xFF747D8C)
val ColorCuenta   = Color(0xFFA4B0BE)

// Temas Heredados (Tratados aquí para cumplimiento de idioma)
val ClásicoPrimario        = Color(0xFF6200EE)
val ClásicoSecundario      = Color(0xFF03DAC6)
val GrisOscuroFondo        = Color(0xFF121212)
val GrisOscuroSuperficie   = Color(0xFF1E1E1E)
val GrisOscuroPrimario     = Color(0xFFBB86FC)
val AzulOscuroFondo        = Color(0xFF0D1B2A)
val AzulOscuroSuperficie   = Color(0xFF1B263B)
val AzulOscuroPrimario     = Color(0xFF415A77)
val OneUIPrimario          = Color(0xFF0066FF)
val OneUISecundario        = Color(0xFF00D9FF)
val VibeUIPrimario         = Color(0xFFFF6B6B)
val VibeUISecundario       = Color(0xFFFFE66D)
val GlossyPrimario         = Color(0xFFFF0080)
val GlossySecundario       = Color(0xFFFF4081)
val PanteraNegraFondo      = Color(0xFF000000)
val PanteraNegraPrimario   = Color(0xFFFFD700)
val CineUIFondo            = Color(0xFF1A1A1A)
val CineUIPrimario          = Color(0xFFFF5722)
val VUIFondo               = Color(0xFF0F0F23)
val VUIPrimario            = Color(0xFF00E5FF)

