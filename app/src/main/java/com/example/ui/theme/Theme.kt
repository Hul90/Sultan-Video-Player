package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

enum class AppThemePalette(val label: String, val primaryColor: Color) {
    CYBER_VIOLET("Cyber Violet", Color(0xFFD0BCFF)),
    NEON_CYAN("Neon Cyan", Color(0xFF00E5FF)),
    EMERALD_GLOW("Emerald Glow", Color(0xFF00E676)),
    AMBER_GOLD("Amber Gold", Color(0xFFFFD54F)),
    FIRE_RED("Fire Red", Color(0xFFFF5252));

    val displayName: String get() = label
}

fun getColorSchemeForPalette(palette: AppThemePalette): androidx.compose.material3.ColorScheme {
    val primary = palette.primaryColor
    val primaryContainer = when (palette) {
        AppThemePalette.CYBER_VIOLET -> Color(0xFF4F378B)
        AppThemePalette.NEON_CYAN -> Color(0xFF004D5A)
        AppThemePalette.EMERALD_GLOW -> Color(0xFF004D27)
        AppThemePalette.AMBER_GOLD -> Color(0xFF5A4500)
        AppThemePalette.FIRE_RED -> Color(0xFF6B1414)
    }

    return darkColorScheme(
        primary = primary,
        onPrimary = Color(0xFF000000),
        primaryContainer = primaryContainer,
        onPrimaryContainer = Color.White,
        secondary = HighDensitySecondary,
        onSecondary = Color(0xFF1E1E24),
        secondaryContainer = HighDensitySubtle,
        onSecondaryContainer = Color(0xFFE8DEF8),
        tertiary = AccentGreen,
        background = HighDensityBg,
        onBackground = HighDensityTextPrimary,
        surface = HighDensitySurface,
        onSurface = HighDensityTextPrimary,
        surfaceVariant = HighDensityCard,
        onSurfaceVariant = HighDensityTextSecondary,
        outline = HighDensityBorder,
        outlineVariant = Color(0x4045464F),
        error = AccentRed
    )
}

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    selectedPalette: AppThemePalette = AppThemePalette.CYBER_VIOLET,
    content: @Composable () -> Unit,
) {
    val context = LocalContext.current
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        else -> getColorSchemeForPalette(selectedPalette)
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
