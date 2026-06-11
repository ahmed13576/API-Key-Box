package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val SophisticatedColorScheme = darkColorScheme(
    primary = SophisticatedPrimary,
    onPrimary = SophisticatedOnPrimary,
    primaryContainer = SophisticatedPrimaryContainer,
    onPrimaryContainer = SophisticatedOnPrimaryContainer,
    secondary = SophisticatedSecondary,
    onSecondary = SophisticatedOnSecondary,
    tertiary = SophisticatedTertiary,
    onTertiary = SophisticatedOnTertiary,
    background = SophisticatedBg,
    onBackground = SophisticatedOnBg,
    surface = SophisticatedSurface,
    onSurface = SophisticatedOnSurface,
    surfaceVariant = SophisticatedSurfaceVariant,
    onSurfaceVariant = SophisticatedOnSurfaceVariant,
    outline = SophisticatedOutline
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = true, // Force sophisticated dark atmosphere
    dynamicColor: Boolean = false, // Keep it true to theme intent
    content: @Composable () -> Unit,
) {
    // We strictly use the premium customized Sophisticated Dark Color Scheme matching the requested design directions.
    MaterialTheme(
        colorScheme = SophisticatedColorScheme,
        typography = Typography,
        content = content
    )
}
