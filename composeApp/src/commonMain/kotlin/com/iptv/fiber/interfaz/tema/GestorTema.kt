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
    GRIS_OSCURO("Gris oscuro", true),
    AZUL_OSCURO("Azul oscuro", true),
    OSCURO("Oscuro", true),
    CLARO_MODERNO("Claro moderno", true),
    VIBRANTE("Vibrante", true),
    BRILLANTE("Brillante", true),
    PANTERA_NEGRA("Pantera negra", true),
    CINE("Cine", true),
    VIDRIO("Vidrio", true);
}

/** Devuelve el [ColorScheme] de Material3 correspondiente al [tema] seleccionado. */
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
        TemaApp.CLARO_MODERNO -> darkColorScheme(
            primary = ClaroModernoPrimario,
            secondary = ClaroModernoSecundario
        )
        TemaApp.VIBRANTE -> darkColorScheme(
            primary = VibrantePrimario,
            secondary = VibranteSecundario
        )
        TemaApp.BRILLANTE -> darkColorScheme(
            primary = BRILLANTEPrimario,
            secondary = BRILLANTESecundario
        )
        TemaApp.PANTERA_NEGRA -> darkColorScheme(
            primary = PanteraNegraPrimario,
            background = PanteraNegraFondo
        )
        TemaApp.CINE -> darkColorScheme(
            primary = CinePrimario,
            background = CineFondo
        )
        TemaApp.VIDRIO -> darkColorScheme(
            primary = VidrioPrimario,
            background = VidrioFondo
        )
    }
}

/** Aplica el [ColorScheme] y la tipografía de [tema] al árbol de composables en [contenido]. */
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

