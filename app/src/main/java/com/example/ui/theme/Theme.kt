package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = NeonRose,
    onPrimary = Color.White,
    primaryContainer = CardSurface,
    onPrimaryContainer = SoftRose,
    secondary = BrightViolet,
    onSecondary = Color.White,
    secondaryContainer = CardBorder,
    onSecondaryContainer = SoftLavender,
    tertiary = ElectricAmber,
    onTertiary = Color.Black,
    background = DeepPlum,
    onBackground = DarkTextPrimary,
    surface = DarkPurpleSurface,
    onSurface = DarkTextPrimary,
    surfaceVariant = CardSurface,
    onSurfaceVariant = DarkTextSecondary,
    outline = CardBorder
)

private val LightColorScheme = lightColorScheme(
    primary = BrightRose,
    onPrimary = Color.White,
    primaryContainer = LightSurfaceVariant,
    onPrimaryContainer = LightTextPrimary,
    secondary = ElectricViolet,
    onSecondary = Color.White,
    secondaryContainer = LightSurfaceVariant,
    onSecondaryContainer = LightTextSecondary,
    tertiary = ElectricAmber,
    onTertiary = Color.White,
    background = LightBackground,
    onBackground = LightTextPrimary,
    surface = LightSurface,
    onSurface = LightTextPrimary,
    surfaceVariant = LightSurfaceVariant,
    onSurfaceVariant = LightTextSecondary,
    outline = CardBorder
)

@Composable
fun MahiTheme(
    darkTheme: Boolean = true, // Default to dark for glowing neon AI aesthetic
    dynamicColor: Boolean = false, // Keep Mahi's signature vibrant identity
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
