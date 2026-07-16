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

// Paleta de tema ultra premium (concepto carbono y azul)
val FondoPremium = Color(0xFF000000) // Negro Absoluto (OLED)
val SuperficiePremium = Color(0xFF121212)    // Grafito oscuro
val SuperficiePremiumClaro = Color(0xFF1E1E1E) // Gris carbón
val AcentoPremium = Color(0xFF4B5EB9)     // Azul Corporativo Fiber Z (Protagonista)
val AcentoPremiumClaro = Color(0xFF6D7FE1) // Foco de alto contraste
val AcentoPremiumOscuro = Color(0xFF3545A0) // Estado presionado
val SecundarioPremium = Color(0xFF6C5DD3)  // Púrpura elegante
val DetallePremium = Color(0xFFBDBE22)     // Amarillo corporativo suavizado
val ErrorPremium = Color(0xFFFF4757)
val ExitoPremium = Color(0xFF2ED573)
val TextoPrimarioPremium = Color(0xFFFFFFFF)
val TextoSecundarioPremium = Color(0xFF94A3B8) // Gris azulado suave para lectura cómoda
val SuperficieVidrio = Color(0x1FFFFFFF) // Superficie translúcida premium
val BordeVidrio = Color(0x26FFFFFF)
val SombraPremium = Color(0x804B5EB9)

// Colores de carga
val BaseCarga = Color(0xFF1A1E2B)
val ResaltadoCarga = Color(0xFF263148)

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

val DegradadoVidrioPremium = Brush.linearGradient(
    colors = listOf(Color(0x26FFFFFF), Color(0x08FFFFFF))
)

// Colores por Categoría
val ColorTvEnVivo = Color(0xFF00E5FF)
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
val ClaroModernoPrimario          = Color(0xFF0066FF)
val ClaroModernoSecundario        = Color(0xFF00D9FF)
val VibrantePrimario         = Color(0xFFFF6B6B)
val VibranteSecundario       = Color(0xFFFFE66D)
val BRILLANTEPrimario         = Color(0xFFFF0080)
val BRILLANTESecundario       = Color(0xFFFF4081)
val PanteraNegraFondo      = Color(0xFF000000)
val PanteraNegraPrimario   = Color(0xFFFFD700)
val CineFondo            = Color(0xFF1A1A1A)
val CinePrimario          = Color(0xFFFF5722)
val VidrioFondo               = Color(0xFF0F0F23)
val VidrioPrimario            = Color(0xFF00E5FF)

