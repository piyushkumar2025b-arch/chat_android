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

data class AppThemeOption(
    val id: String,
    val name: String,
    val description: String,
    val previewColor: Color,
    val secondaryPreview: Color
)

object AvailableThemes {
    val themes = listOf(
        AppThemeOption("default", "Modern Indigo", "Dynamic Indigo & Slate Palette", Color(0xFF6366F1), Color(0xFF818CF8)),
        AppThemeOption("cyberpunk", "Cyberpunk Neon", "High-contrast Electric Violet & Yellow", Color(0xFF8B5CF6), Color(0xFFFBBF24)),
        AppThemeOption("emerald", "Emerald Forest", "Refined Nature-inspired Green & Teal", Color(0xFF10B981), Color(0xFF14B8A6)),
        AppThemeOption("sunset", "Sunset Amber", "Warm Crimson and Amber Radiance", Color(0xFFF97316), Color(0xFFEF4444)),
        AppThemeOption("midnight", "Midnight OLED", "Pure deep dark theme for battery saving", Color(0xFF3B82F6), Color(0xFF1E293B))
    )
}

private val DarkColorScheme =
    darkColorScheme(
        primary = Color(0xFF818CF8),
        onPrimary = Color(0xFF1E1B4B),
        primaryContainer = Color(0xFF312E81),
        onPrimaryContainer = Color(0xFFE0E7FF),
        secondary = Color(0xFFA78BFA),
        background = Color(0xFF0F172A),
        surface = Color(0xFF1E293B)
    )

private val LightColorScheme =
    lightColorScheme(
        primary = Color(0xFF4F46E5),
        onPrimary = Color.White,
        primaryContainer = Color(0xFFEEF2FF),
        onPrimaryContainer = Color(0xFF312E81),
        secondary = Color(0xFF7C3AED),
        background = Color(0xFFF8FAFC),
        surface = Color.White
    )

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme =
        when {
            dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
                val context = LocalContext.current
                if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
            }
            darkTheme -> DarkColorScheme
            else -> LightColorScheme
        }

    MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
}
