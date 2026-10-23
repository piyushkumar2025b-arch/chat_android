package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
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

private data class AppPalette(
    val lightPrimary: Color,
    val lightOnPrimary: Color,
    val lightPrimaryContainer: Color,
    val lightOnPrimaryContainer: Color,
    val lightSecondary: Color,
    val lightSecondaryContainer: Color,
    val lightOnSecondaryContainer: Color,
    val lightBackground: Color,
    val lightSurface: Color,
    val darkPrimary: Color,
    val darkOnPrimary: Color,
    val darkPrimaryContainer: Color,
    val darkOnPrimaryContainer: Color,
    val darkSecondary: Color,
    val darkSecondaryContainer: Color,
    val darkOnSecondaryContainer: Color,
    val darkBackground: Color,
    val darkSurface: Color
)

private val DefaultPalette = AppPalette(
    lightPrimary = Color(0xFF4F46E5),
    lightOnPrimary = Color.White,
    lightPrimaryContainer = Color(0xFFEEF2FF),
    lightOnPrimaryContainer = Color(0xFF312E81),
    lightSecondary = Color(0xFF7C3AED),
    lightSecondaryContainer = Color(0xFFF3E8FF),
    lightOnSecondaryContainer = Color(0xFF4C1D95),
    lightBackground = Color(0xFFF8FAFC),
    lightSurface = Color.White,
    darkPrimary = Color(0xFF818CF8),
    darkOnPrimary = Color(0xFF1E1B4B),
    darkPrimaryContainer = Color(0xFF312E81),
    darkOnPrimaryContainer = Color(0xFFE0E7FF),
    darkSecondary = Color(0xFFA78BFA),
    darkSecondaryContainer = Color(0xFF3B2A63),
    darkOnSecondaryContainer = Color(0xFFEDE9FE),
    darkBackground = Color(0xFF0F172A),
    darkSurface = Color(0xFF1E293B)
)

private val CyberpunkPalette = AppPalette(
    lightPrimary = Color(0xFF7C3AED),
    lightOnPrimary = Color.White,
    lightPrimaryContainer = Color(0xFFEDE9FE),
    lightOnPrimaryContainer = Color(0xFF2E1065),
    lightSecondary = Color(0xFFD97706),
    lightSecondaryContainer = Color(0xFFFEF3C7),
    lightOnSecondaryContainer = Color(0xFF78350F),
    lightBackground = Color(0xFFF5F3FF),
    lightSurface = Color.White,
    darkPrimary = Color(0xFFA78BFA),
    darkOnPrimary = Color(0xFF1E1033),
    darkPrimaryContainer = Color(0xFF4C1D95),
    darkOnPrimaryContainer = Color(0xFFF5F3FF),
    darkSecondary = Color(0xFFFBBF24),
    darkSecondaryContainer = Color(0xFF4A3608),
    darkOnSecondaryContainer = Color(0xFFFEF3C7),
    darkBackground = Color(0xFF0B0A1A),
    darkSurface = Color(0xFF17132E)
)

private val EmeraldPalette = AppPalette(
    lightPrimary = Color(0xFF059669),
    lightOnPrimary = Color.White,
    lightPrimaryContainer = Color(0xFFD1FAE5),
    lightOnPrimaryContainer = Color(0xFF064E3B),
    lightSecondary = Color(0xFF0D9488),
    lightSecondaryContainer = Color(0xFFCCFBF1),
    lightOnSecondaryContainer = Color(0xFF134E4A),
    lightBackground = Color(0xFFECFDF5),
    lightSurface = Color.White,
    darkPrimary = Color(0xFF34D399),
    darkOnPrimary = Color(0xFF022C22),
    darkPrimaryContainer = Color(0xFF065F46),
    darkOnPrimaryContainer = Color(0xFFD1FAE5),
    darkSecondary = Color(0xFF2DD4BF),
    darkSecondaryContainer = Color(0xFF0F3D36),
    darkOnSecondaryContainer = Color(0xFFCCFBF1),
    darkBackground = Color(0xFF052E2B),
    darkSurface = Color(0xFF0C3B35)
)

private val SunsetPalette = AppPalette(
    lightPrimary = Color(0xFFEA580C),
    lightOnPrimary = Color.White,
    lightPrimaryContainer = Color(0xFFFFEDD5),
    lightOnPrimaryContainer = Color(0xFF7C2D12),
    lightSecondary = Color(0xFFDC2626),
    lightSecondaryContainer = Color(0xFFFEE2E2),
    lightOnSecondaryContainer = Color(0xFF7F1D1D),
    lightBackground = Color(0xFFFFF7ED),
    lightSurface = Color.White,
    darkPrimary = Color(0xFFFB923C),
    darkOnPrimary = Color(0xFF431407),
    darkPrimaryContainer = Color(0xFF9A3412),
    darkOnPrimaryContainer = Color(0xFFFFEDD5),
    darkSecondary = Color(0xFFF87171),
    darkSecondaryContainer = Color(0xFF4C1919),
    darkOnSecondaryContainer = Color(0xFFFEE2E2),
    darkBackground = Color(0xFF1C1917),
    darkSurface = Color(0xFF292524)
)

private val MidnightPalette = AppPalette(
    lightPrimary = Color(0xFF2563EB),
    lightOnPrimary = Color.White,
    lightPrimaryContainer = Color(0xFFDBEAFE),
    lightOnPrimaryContainer = Color(0xFF1E3A8A),
    lightSecondary = Color(0xFF0EA5E9),
    lightSecondaryContainer = Color(0xFFE0F2FE),
    lightOnSecondaryContainer = Color(0xFF0C4A6E),
    lightBackground = Color(0xFFF8FAFC),
    lightSurface = Color.White,
    darkPrimary = Color(0xFF60A5FA),
    darkOnPrimary = Color(0xFF0B1220),
    darkPrimaryContainer = Color(0xFF1E3A8A),
    darkOnPrimaryContainer = Color(0xFFDBEAFE),
    darkSecondary = Color(0xFF38BDF8),
    darkSecondaryContainer = Color(0xFF0C4A6E),
    darkOnSecondaryContainer = Color(0xFFE0F2FE),
    darkBackground = Color(0xFF020617),
    darkSurface = Color(0xFF0F172A)
)

private fun paletteFor(themeId: String): AppPalette = when (themeId) {
    "cyberpunk" -> CyberpunkPalette
    "emerald" -> EmeraldPalette
    "sunset" -> SunsetPalette
    "midnight" -> MidnightPalette
    else -> DefaultPalette
}

private fun AppPalette.lightScheme(): ColorScheme = lightColorScheme(
    primary = lightPrimary,
    onPrimary = lightOnPrimary,
    primaryContainer = lightPrimaryContainer,
    onPrimaryContainer = lightOnPrimaryContainer,
    secondary = lightSecondary,
    onSecondary = Color.White,
    secondaryContainer = lightSecondaryContainer,
    onSecondaryContainer = lightOnSecondaryContainer,
    background = lightBackground,
    onBackground = Color(0xFF1C1B1F),
    surface = lightSurface,
    onSurface = Color(0xFF1C1B1F),
    surfaceVariant = lightPrimaryContainer,
    onSurfaceVariant = Color(0xFF49454F)
)

private fun AppPalette.darkScheme(): ColorScheme = darkColorScheme(
    primary = darkPrimary,
    onPrimary = darkOnPrimary,
    primaryContainer = darkPrimaryContainer,
    onPrimaryContainer = darkOnPrimaryContainer,
    secondary = darkSecondary,
    onSecondary = Color(0xFF1C1033),
    secondaryContainer = darkSecondaryContainer,
    onSecondaryContainer = darkOnSecondaryContainer,
    background = darkBackground,
    onBackground = Color(0xFFE5E7EB),
    surface = darkSurface,
    onSurface = Color(0xFFE5E7EB),
    surfaceVariant = darkSurface,
    onSurfaceVariant = Color(0xFFCBD5E1)
)

private fun AppPalette.amoledScheme(): ColorScheme = darkColorScheme(
    primary = darkPrimary,
    onPrimary = darkOnPrimary,
    primaryContainer = darkPrimaryContainer,
    onPrimaryContainer = darkOnPrimaryContainer,
    secondary = darkSecondary,
    onSecondary = Color(0xFF0B0B0B),
    secondaryContainer = darkSecondaryContainer,
    onSecondaryContainer = darkOnSecondaryContainer,
    background = Color.Black,
    onBackground = Color(0xFFE5E7EB),
    surface = Color.Black,
    onSurface = Color(0xFFE5E7EB),
    surfaceVariant = Color(0xFF121212),
    onSurfaceVariant = Color(0xFFBFC7D2),
    surfaceContainerLowest = Color.Black,
    surfaceContainerLow = Color(0xFF070707),
    surfaceContainer = Color(0xFF0C0C0C),
    surfaceContainerHigh = Color(0xFF141414),
    surfaceContainerHighest = Color(0xFF1B1B1B)
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    themeId: String = "default",
    themeMode: String = "system",
    content: @Composable () -> Unit
) {
    val systemDark = darkTheme
    val isDark = when (themeMode) {
        "light" -> false
        "dark", "amoled" -> true
        else -> systemDark
    }
    val isAmoled = themeMode == "amoled"
    val palette = paletteFor(themeId)

    val colorScheme = when {
        isAmoled -> palette.amoledScheme()
        dynamicColor && themeId == "default" && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (isDark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        isDark -> palette.darkScheme()
        else -> palette.lightScheme()
    }

    MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
}
