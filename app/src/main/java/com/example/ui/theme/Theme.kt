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
        AppThemeOption("default", "Crisp Indigo", "Clean indigo on neutral slate", Color(0xFF4F46E5), Color(0xFF64748B)),
        AppThemeOption("cyberpunk", "Royal Violet", "Sharp violet with warm gold", Color(0xFF7C3AED), Color(0xFFD97706)),
        AppThemeOption("emerald", "Forest Green", "Calm emerald and cool slate", Color(0xFF059669), Color(0xFF0D9488)),
        AppThemeOption("sunset", "Warm Coral", "Clear coral and warm stone", Color(0xFFEA580C), Color(0xFFB45309)),
        AppThemeOption("midnight", "Ocean Blue", "Deep blue on clean charcoal", Color(0xFF2563EB), Color(0xFF0284C7))
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
    val darkPrimary: Color,
    val darkOnPrimary: Color,
    val darkPrimaryContainer: Color,
    val darkOnPrimaryContainer: Color,
    val darkSecondary: Color,
    val darkSecondaryContainer: Color,
    val darkOnSecondaryContainer: Color
)

private val DefaultPalette = AppPalette(
    lightPrimary = Color(0xFF4338CA),
    lightOnPrimary = Color.White,
    lightPrimaryContainer = Color(0xFFE8EAFF),
    lightOnPrimaryContainer = Color(0xFF1E1B4B),
    lightSecondary = Color(0xFF475569),
    lightSecondaryContainer = Color(0xFFE2E8F0),
    lightOnSecondaryContainer = Color(0xFF1E293B),
    darkPrimary = Color(0xFF818CF8),
    darkOnPrimary = Color(0xFF111827),
    darkPrimaryContainer = Color(0xFF282B59),
    darkOnPrimaryContainer = Color(0xFFE0E7FF),
    darkSecondary = Color(0xFF94A3B8),
    darkSecondaryContainer = Color(0xFF242B38),
    darkOnSecondaryContainer = Color(0xFFE2E8F0)
)

private val CyberpunkPalette = AppPalette(
    lightPrimary = Color(0xFF6D28D9),
    lightOnPrimary = Color.White,
    lightPrimaryContainer = Color(0xFFEDE9FE),
    lightOnPrimaryContainer = Color(0xFF2E1065),
    lightSecondary = Color(0xFFB45309),
    lightSecondaryContainer = Color(0xFFFEF3C7),
    lightOnSecondaryContainer = Color(0xFF78350F),
    darkPrimary = Color(0xFFA78BFA),
    darkOnPrimary = Color(0xFF170B2B),
    darkPrimaryContainer = Color(0xFF3B1E6D),
    darkOnPrimaryContainer = Color(0xFFF3E8FF),
    darkSecondary = Color(0xFFFBBF24),
    darkSecondaryContainer = Color(0xFF3A2D12),
    darkOnSecondaryContainer = Color(0xFFFEF3C7)
)

private val EmeraldPalette = AppPalette(
    lightPrimary = Color(0xFF047857),
    lightOnPrimary = Color.White,
    lightPrimaryContainer = Color(0xFFD1FAE5),
    lightOnPrimaryContainer = Color(0xFF064E3B),
    lightSecondary = Color(0xFF0F766E),
    lightSecondaryContainer = Color(0xFFCCFBF1),
    lightOnSecondaryContainer = Color(0xFF134E4A),
    darkPrimary = Color(0xFF34D399),
    darkOnPrimary = Color(0xFF03231B),
    darkPrimaryContainer = Color(0xFF0C4A38),
    darkOnPrimaryContainer = Color(0xFFD1FAE5),
    darkSecondary = Color(0xFF2DD4BF),
    darkSecondaryContainer = Color(0xFF113834),
    darkOnSecondaryContainer = Color(0xFFCCFBF1)
)

private val SunsetPalette = AppPalette(
    lightPrimary = Color(0xFFC2410C),
    lightOnPrimary = Color.White,
    lightPrimaryContainer = Color(0xFFFFEDD5),
    lightOnPrimaryContainer = Color(0xFF7C2D12),
    lightSecondary = Color(0xFF78716C),
    lightSecondaryContainer = Color(0xFFF5F5F4),
    lightOnSecondaryContainer = Color(0xFF292524),
    darkPrimary = Color(0xFFFB923C),
    darkOnPrimary = Color(0xFF2B1105),
    darkPrimaryContainer = Color(0xFF5C230B),
    darkOnPrimaryContainer = Color(0xFFFFEDD5),
    darkSecondary = Color(0xFFA8A29E),
    darkSecondaryContainer = Color(0xFF2C2927),
    darkOnSecondaryContainer = Color(0xFFE7E5E4)
)

private val MidnightPalette = AppPalette(
    lightPrimary = Color(0xFF1D4ED8),
    lightOnPrimary = Color.White,
    lightPrimaryContainer = Color(0xFFDBEAFE),
    lightOnPrimaryContainer = Color(0xFF1E3A8A),
    lightSecondary = Color(0xFF0369A1),
    lightSecondaryContainer = Color(0xFFE0F2FE),
    lightOnSecondaryContainer = Color(0xFF0C4A6E),
    darkPrimary = Color(0xFF60A5FA),
    darkOnPrimary = Color(0xFF0A1426),
    darkPrimaryContainer = Color(0xFF17315F),
    darkOnPrimaryContainer = Color(0xFFDBEAFE),
    darkSecondary = Color(0xFF38BDF8),
    darkSecondaryContainer = Color(0xFF132F44),
    darkOnSecondaryContainer = Color(0xFFE0F2FE)
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
    tertiary = lightPrimary,
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFF1F5F9),
    onTertiaryContainer = Color(0xFF1E293B),
    background = Color(0xFFF7F8FA),
    onBackground = Color(0xFF0F172A),
    surface = Color.White,
    onSurface = Color(0xFF0F172A),
    surfaceVariant = Color(0xFFF1F5F9),
    onSurfaceVariant = Color(0xFF475569),
    surfaceContainerLowest = Color.White,
    surfaceContainerLow = Color(0xFFFAFBFD),
    surfaceContainer = Color(0xFFF3F5F8),
    surfaceContainerHigh = Color(0xFFECEFF4),
    surfaceContainerHighest = Color(0xFFE2E8F0),
    outline = Color(0xFFCBD5E1),
    outlineVariant = Color(0xFFE2E8F0),
    error = Color(0xFFDC2626),
    onError = Color.White,
    errorContainer = Color(0xFFFEE2E2),
    onErrorContainer = Color(0xFF7F1D1D)
)

private fun AppPalette.darkScheme(): ColorScheme = darkColorScheme(
    primary = darkPrimary,
    onPrimary = darkOnPrimary,
    primaryContainer = darkPrimaryContainer,
    onPrimaryContainer = darkOnPrimaryContainer,
    secondary = darkSecondary,
    onSecondary = Color(0xFF0F172A),
    secondaryContainer = darkSecondaryContainer,
    onSecondaryContainer = darkOnSecondaryContainer,
    tertiary = darkPrimary,
    onTertiary = darkOnPrimary,
    tertiaryContainer = Color(0xFF1E2636),
    onTertiaryContainer = Color(0xFFE2E8F0),
    background = Color(0xFF0B0E14),
    onBackground = Color(0xFFF1F5F9),
    surface = Color(0xFF111622),
    onSurface = Color(0xFFF1F5F9),
    surfaceVariant = Color(0xFF1A2232),
    onSurfaceVariant = Color(0xFF94A3B8),
    surfaceContainerLowest = Color(0xFF080B10),
    surfaceContainerLow = Color(0xFF131926),
    surfaceContainer = Color(0xFF182030),
    surfaceContainerHigh = Color(0xFF202A3D),
    surfaceContainerHighest = Color(0xFF28344B),
    outline = Color(0xFF3E4C66),
    outlineVariant = Color(0xFF273145),
    error = Color(0xFFF87171),
    onError = Color(0xFF450A0A),
    errorContainer = Color(0xFF451A1A),
    onErrorContainer = Color(0xFFFECACA)
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
    tertiary = darkPrimary,
    onTertiary = darkOnPrimary,
    tertiaryContainer = Color(0xFF14171F),
    onTertiaryContainer = Color(0xFFE2E8F0),
    background = Color.Black,
    onBackground = Color(0xFFF3F4F6),
    surface = Color.Black,
    onSurface = Color(0xFFF3F4F6),
    surfaceVariant = Color(0xFF12141A),
    onSurfaceVariant = Color(0xFF9CA3AF),
    surfaceContainerLowest = Color.Black,
    surfaceContainerLow = Color(0xFF08090C),
    surfaceContainer = Color(0xFF101218),
    surfaceContainerHigh = Color(0xFF171A22),
    surfaceContainerHighest = Color(0xFF1F242E),
    outline = Color(0xFF323846),
    outlineVariant = Color(0xFF20242E),
    error = Color(0xFFF87171),
    onError = Color(0xFF450A0A),
    errorContainer = Color(0xFF3B1214),
    onErrorContainer = Color(0xFFFECACA)
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
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
