package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val SolarColorScheme = darkColorScheme(
    primary = SolarAmber,
    secondary = EmeraldGreen,
    tertiary = IndigoAccent,
    background = DarkSlate,
    surface = CardSlate,
    onPrimary = DarkSlate,
    onSecondary = DarkSlate,
    onBackground = TextLight,
    onSurface = TextLight,
    outline = BorderSlate
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = true, // Force dark theme for the premium Bloomberg financial dashboard experience
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    // We always use the SolarColorScheme to maintain a high-end, cohesive, professional brand identity.
    MaterialTheme(
        colorScheme = SolarColorScheme,
        typography = Typography,
        content = content
    )
}
