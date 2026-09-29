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

data class ThemeInfo(
    val id: String,
    val name: String,
    val previewColor: Color,
    val secondaryPreview: Color,
    val description: String
)

object AvailableThemes {
    val themes = listOf(
        ThemeInfo(
            id = "indigo",
            name = "Electric Indigo",
            previewColor = IndigoPrimaryLight,
            secondaryPreview = CyanSecondaryLight,
            description = "Default modern tech palette"
        ),
        ThemeInfo(
            id = "emerald",
            name = "Emerald Mint",
            previewColor = EmeraldPrimaryLight,
            secondaryPreview = MintSecondaryLight,
            description = "Fresh natural green accents"
        ),
        ThemeInfo(
            id = "cyberpunk",
            name = "Neon Cyber",
            previewColor = CyberPrimaryLight,
            secondaryPreview = NeonPinkSecondaryLight,
            description = "Vibrant synthwave purple & neon pink"
        ),
        ThemeInfo(
            id = "sunset",
            name = "Warm Sunset",
            previewColor = SunsetPrimaryLight,
            secondaryPreview = AmberSecondaryLight,
            description = "Cozy terracotta, orange & amber"
        ),
        ThemeInfo(
            id = "ocean",
            name = "Deep Ocean",
            previewColor = OceanPrimaryLight,
            secondaryPreview = BlueSecondaryLight,
            description = "Calm azure & aquatic blues"
        ),
        ThemeInfo(
            id = "slate",
            name = "Monochrome Slate",
            previewColor = SlatePrimaryLight,
            secondaryPreview = CharcoalSecondaryLight,
            description = "Clean minimalist charcoal & paper"
        )
    )
}

@Composable
fun MyApplicationTheme(
    selectedTheme: String = "indigo",
    themeMode: String = "system",
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    val isSystemDark = isSystemInDarkTheme()
    val isDark = when (themeMode) {
        "dark" -> true
        "amoled" -> true
        "light" -> false
        else -> isSystemDark
    }

    val isAmoled = themeMode == "amoled"

    val baseColorScheme: ColorScheme = when (selectedTheme) {
        "emerald" -> if (isDark) {
            darkColorScheme(
                primary = EmeraldPrimaryDark,
                onPrimary = OnEmeraldPrimaryDark,
                primaryContainer = EmeraldPrimaryContainerDark,
                onPrimaryContainer = OnEmeraldPrimaryContainerDark,
                secondary = MintSecondaryDark
            )
        } else {
            lightColorScheme(
                primary = EmeraldPrimaryLight,
                onPrimary = OnEmeraldPrimaryLight,
                primaryContainer = EmeraldPrimaryContainerLight,
                onPrimaryContainer = OnEmeraldPrimaryContainerLight,
                secondary = MintSecondaryLight
            )
        }

        "cyberpunk" -> if (isDark) {
            darkColorScheme(
                primary = CyberPrimaryDark,
                onPrimary = OnCyberPrimaryDark,
                primaryContainer = CyberPrimaryContainerDark,
                onPrimaryContainer = OnCyberPrimaryContainerDark,
                secondary = NeonPinkSecondaryDark
            )
        } else {
            lightColorScheme(
                primary = CyberPrimaryLight,
                onPrimary = OnCyberPrimaryLight,
                primaryContainer = CyberPrimaryContainerLight,
                onPrimaryContainer = OnCyberPrimaryContainerLight,
                secondary = NeonPinkSecondaryLight
            )
        }

        "sunset" -> if (isDark) {
            darkColorScheme(
                primary = SunsetPrimaryDark,
                onPrimary = OnSunsetPrimaryDark,
                primaryContainer = SunsetPrimaryContainerDark,
                onPrimaryContainer = OnSunsetPrimaryContainerDark,
                secondary = AmberSecondaryDark
            )
        } else {
            lightColorScheme(
                primary = SunsetPrimaryLight,
                onPrimary = OnSunsetPrimaryLight,
                primaryContainer = SunsetPrimaryContainerLight,
                onPrimaryContainer = OnSunsetPrimaryContainerLight,
                secondary = AmberSecondaryLight
            )
        }

        "ocean" -> if (isDark) {
            darkColorScheme(
                primary = OceanPrimaryDark,
                onPrimary = OnOceanPrimaryDark,
                primaryContainer = OceanPrimaryContainerDark,
                onPrimaryContainer = OnOceanPrimaryContainerDark,
                secondary = BlueSecondaryDark
            )
        } else {
            lightColorScheme(
                primary = OceanPrimaryLight,
                onPrimary = OnOceanPrimaryLight,
                primaryContainer = OceanPrimaryContainerLight,
                onPrimaryContainer = OnOceanPrimaryContainerLight,
                secondary = BlueSecondaryLight
            )
        }

        "slate" -> if (isDark) {
            darkColorScheme(
                primary = SlatePrimaryDark,
                onPrimary = OnSlatePrimaryDark,
                primaryContainer = SlatePrimaryContainerDark,
                onPrimaryContainer = OnSlatePrimaryContainerDark,
                secondary = CharcoalSecondaryDark
            )
        } else {
            lightColorScheme(
                primary = SlatePrimaryLight,
                onPrimary = OnSlatePrimaryLight,
                primaryContainer = SlatePrimaryContainerLight,
                onPrimaryContainer = OnSlatePrimaryContainerLight,
                secondary = CharcoalSecondaryLight
            )
        }

        else -> if (isDark) {
            darkColorScheme(
                primary = IndigoPrimaryDark,
                onPrimary = OnIndigoPrimaryDark,
                primaryContainer = IndigoPrimaryContainerDark,
                onPrimaryContainer = OnIndigoPrimaryContainerDark,
                secondary = CyanSecondaryDark,
                onSecondary = OnCyanSecondaryDark,
                secondaryContainer = CyanSecondaryContainerDark,
                onSecondaryContainer = OnCyanSecondaryContainerDark
            )
        } else {
            lightColorScheme(
                primary = IndigoPrimaryLight,
                onPrimary = OnIndigoPrimaryLight,
                primaryContainer = IndigoPrimaryContainerLight,
                onPrimaryContainer = OnIndigoPrimaryContainerLight,
                secondary = CyanSecondaryLight,
                onSecondary = OnCyanSecondaryLight,
                secondaryContainer = CyanSecondaryContainerLight,
                onSecondaryContainer = OnCyanSecondaryContainerLight
            )
        }
    }

    val finalColorScheme = if (isAmoled) {
        baseColorScheme.copy(
            background = Color.Black,
            surface = Color.Black,
            surfaceVariant = Color(0xFF121212),
            surfaceContainer = Color(0xFF1A1A1A),
            surfaceContainerHigh = Color(0xFF222222)
        )
    } else {
        baseColorScheme
    }

    MaterialTheme(colorScheme = finalColorScheme, typography = Typography, content = content)
}
