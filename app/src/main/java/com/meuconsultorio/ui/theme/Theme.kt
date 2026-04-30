package com.meuconsultorio.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

private val LightColorScheme = lightColorScheme(
    primary = DentalBlue,
    onPrimary = androidx.compose.ui.graphics.Color.White,
    primaryContainer = DentalBlueLight,
    secondary = DentalTeal,
    onSecondary = androidx.compose.ui.graphics.Color.White,
    tertiary = DentalGreen,
    background = BackgroundLight,
    surface = SurfaceWhite,
    error = DentalRed,
)

private val DarkColorScheme = darkColorScheme(
    primary = DentalBlueLight,
    secondary = DentalTeal,
    tertiary = DentalGreenLight,
    error = DentalRed,
)

@Composable
fun MeuConsultorioTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
