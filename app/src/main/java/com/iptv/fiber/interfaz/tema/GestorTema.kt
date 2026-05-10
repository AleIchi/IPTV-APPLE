package com.iptv.fiber.interfaz.tema

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

enum class TemaApp(
    val nombreAMostrar: String,
    val esOscuro: Boolean = true
) {
    CLASICO("Clásico", false),
    GRIS_OSCURO("Gris Oscuro", true),
    AZUL_OSCURO("Azul Oscuro", true),
    OSCURO("Oscuro", true),
    ONE_UI("OneUI", true),
    VIBE_UI("VibeUI", true),
    GLOSSY("Glossy", true),
    BLACK_PANTHER("Pantera Negra", true),
    MOVIE_UI("CineUI", true),
    VUI("VUI", true);
}

@Composable
fun obtenerEsquemaColorTema(tema: TemaApp): ColorScheme {
    return when (tema) {
        TemaApp.CLASICO -> lightColorScheme(
            primary = ClásicoPrimario,
            secondary = ClásicoSecundario
        )
        TemaApp.GRIS_OSCURO -> darkColorScheme(
            primary = GrisOscuroPrimario,
            background = GrisOscuroFondo,
            surface = GrisOscuroSuperficie
        )
        TemaApp.AZUL_OSCURO -> darkColorScheme(
            primary = AzulOscuroPrimario,
            background = AzulOscuroFondo,
            surface = AzulOscuroSuperficie
        )
        TemaApp.OSCURO -> darkColorScheme(
            primary = Color.White,
            background = Color.Black,
            surface = Color(0xFF1C1C1C)
        )
        TemaApp.ONE_UI -> darkColorScheme(
            primary = OneUIPrimario,
            secondary = OneUISecundario
        )
        TemaApp.VIBE_UI -> darkColorScheme(
            primary = VibeUIPrimario,
            secondary = VibeUISecundario
        )
        TemaApp.GLOSSY -> darkColorScheme(
            primary = GlossyPrimario,
            secondary = GlossySecundario
        )
        TemaApp.BLACK_PANTHER -> darkColorScheme(
            primary = PanteraNegraPrimario,
            background = PanteraNegraFondo
        )
        TemaApp.MOVIE_UI -> darkColorScheme(
            primary = CineUIPrimario,
            background = CineUIFondo
        )
        TemaApp.VUI -> darkColorScheme(
            primary = VUIPrimario,
            background = VUIFondo
        )
    }
}

@Composable
fun TemaIPTVFiberBase(
    tema: TemaApp = TemaApp.CLASICO,
    contenido: @Composable () -> Unit
) {
    val esquemaColor = obtenerEsquemaColorTema(tema)

    MaterialTheme(
        colorScheme = esquemaColor,
        typography = Tipografia,
        content = contenido
    )
}

