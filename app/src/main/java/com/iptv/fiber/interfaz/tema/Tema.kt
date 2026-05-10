package com.iptv.fiber.interfaz.tema

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

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

// Priorizamos el tema oscuro para este tipo de aplicación
private val EsquemaColorClaroPremium = EsquemaColorOscuroPremium

@Composable
fun TemaIPTVFiber(
    esTemaOscuro: Boolean = isSystemInDarkTheme(),
    // El color dinámico está disponible en Android 12+
    colorDinamico: Boolean = false, // Desactivado para forzar el Tema Premium
    contenido: @Composable () -> Unit
) {
    val esquemaColor = when {
        colorDinamico && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val contexto = LocalContext.current
            if (esTemaOscuro) dynamicDarkColorScheme(contexto) else dynamicLightColorScheme(contexto)
        }
        esTemaOscuro -> EsquemaColorOscuroPremium
        else -> EsquemaColorOscuroPremium // Forzar Tema Oscuro por defecto para look Premium
    }
    val vista = LocalView.current
    if (!vista.isInEditMode) {
        SideEffect {
            val ventana = (vista.context as Activity).window
            ventana.statusBarColor = esquemaColor.background.toArgb()
            WindowCompat.getInsetsController(ventana, vista).isAppearanceLightStatusBars = false
        }
    }

    MaterialTheme(
        colorScheme = esquemaColor,
        typography = Tipografia,
        content = contenido
    )
}

