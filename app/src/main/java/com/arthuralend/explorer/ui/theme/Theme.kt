package com.arthuralend.explorer.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

/** Палитра Claude: терракота, кремовый, «пергамент», графит. */
object Claude {
    val Terracotta = Color(0xFFD97757)
    val TerracottaLight = Color(0xFFE38B6C)
    val Cream = Color(0xFFFAF9F5)
    val Ivory = Color(0xFFF0EEE6)
    val Oat = Color(0xFFE8E6DC)
    val Slate = Color(0xFF141413)
    val Charcoal = Color(0xFF1F1E1D)
    val Blue = Color(0xFF6A9BCC)
    val Olive = Color(0xFF788C5D)
}

private val Light = lightColorScheme(
    primary = Claude.Terracotta,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFF7E1D7),
    onPrimaryContainer = Color(0xFF5C2A17),
    secondary = Claude.Blue,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFE3EDF7),
    onSecondaryContainer = Color(0xFF1F3A57),
    tertiary = Claude.Olive,
    onTertiary = Color.White,
    background = Claude.Cream,
    onBackground = Claude.Slate,
    surface = Claude.Cream,
    onSurface = Claude.Slate,
    surfaceVariant = Claude.Ivory,
    onSurfaceVariant = Color(0xFF5E5D59),
    outline = Color(0xFFCFCBBE),
    outlineVariant = Claude.Oat,
    surfaceContainerLowest = Color.White,
    surfaceContainerLow = Color(0xFFF6F5F0),
    surfaceContainer = Color(0xFFF3F1EA),
    surfaceContainerHigh = Color(0xFFEDEBE3),
    surfaceContainerHighest = Claude.Oat,
    error = Color(0xFFB3261E),
    onError = Color.White,
)

private val Dark = darkColorScheme(
    primary = Claude.TerracottaLight,
    onPrimary = Color(0xFF2B140B),
    primaryContainer = Color(0xFF5A3020),
    onPrimaryContainer = Color(0xFFF7E1D7),
    secondary = Claude.Blue,
    onSecondary = Color(0xFF0F2135),
    secondaryContainer = Color(0xFF26394D),
    onSecondaryContainer = Color(0xFFDCE8F5),
    tertiary = Color(0xFF9DB07F),
    background = Claude.Charcoal,
    onBackground = Claude.Ivory,
    surface = Claude.Charcoal,
    onSurface = Claude.Ivory,
    surfaceVariant = Color(0xFF2C2B29),
    onSurfaceVariant = Color(0xFFB8B5AA),
    outline = Color(0xFF4A4843),
    outlineVariant = Color(0xFF35342F),
    surfaceContainerLowest = Color(0xFF262624),
    surfaceContainerLow = Color(0xFF232220),
    surfaceContainer = Color(0xFF2A2927),
    surfaceContainerHigh = Color(0xFF30302E),
    surfaceContainerHighest = Color(0xFF383734),
    error = Color(0xFFF2B8B5),
    onError = Color(0xFF601410),
)

@Composable
fun ExplorerTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (isSystemInDarkTheme()) Dark else Light,
        content = content,
    )
}
