package com.doado.core.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

/**
 * Esquema claro. Texto marino sobre blanco (~16.8:1) y bordes del mismo marino,
 * para que cada casilla tenga un límite visible sin depender de sombras suaves.
 */
private val DoadoLightColors = lightColorScheme(
    primary = DoadoNavy,
    onPrimary = DoadoWhite,
    primaryContainer = DoadoMist,
    onPrimaryContainer = DoadoNavy,

    secondary = DoadoCyanDeep,
    onSecondary = DoadoWhite,
    secondaryContainer = DoadoMistDim,
    onSecondaryContainer = DoadoNavy,

    tertiary = DoadoCyanDeep,
    onTertiary = DoadoWhite,
    tertiaryContainer = DoadoMist,
    onTertiaryContainer = DoadoNavy,

    error = DoadoError,
    onError = DoadoWhite,
    errorContainer = Color(0xFFF9DEDC),
    onErrorContainer = Color(0xFF410E0B),

    background = DoadoWhite,
    onBackground = DoadoNavy,
    surface = DoadoWhite,
    onSurface = DoadoNavy,
    surfaceVariant = DoadoMist,
    onSurfaceVariant = DoadoNavy,

    surfaceContainerLowest = DoadoWhite,
    surfaceContainerLow = DoadoWhite,
    surfaceContainer = DoadoMist,
    surfaceContainerHigh = DoadoMist,
    surfaceContainerHighest = DoadoMistDim,

    inverseSurface = DoadoNavy,
    inverseOnSurface = DoadoWhite,
    inversePrimary = DoadoCyanLight,

    outline = DoadoNavy,
    outlineVariant = DoadoSteel,
    scrim = DoadoBlack,
    // Sin tinte por elevación: el diseño es plano y separa por borde, no por sombra.
    surfaceTint = Color.Transparent,
)

/** Esquema oscuro: negro puro de fondo y blanco de texto, ~21:1. */
private val DoadoDarkColors = darkColorScheme(
    primary = DoadoCyanLight,
    onPrimary = Color(0xFF00243A),
    primaryContainer = DoadoNavy,
    onPrimaryContainer = Color(0xFFDCF3FF),

    secondary = DoadoCyanLight,
    onSecondary = Color(0xFF00243A),
    secondaryContainer = DoadoNavy,
    onSecondaryContainer = Color(0xFFDCF3FF),

    tertiary = DoadoCyanLight,
    onTertiary = Color(0xFF00243A),
    tertiaryContainer = DoadoNavy,
    onTertiaryContainer = Color(0xFFDCF3FF),

    error = DoadoErrorLight,
    onError = DoadoOnErrorLight,
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6),

    background = DoadoBlack,
    onBackground = DoadoWhite,
    surface = DoadoInk,
    onSurface = DoadoWhite,
    surfaceVariant = DoadoNavy,
    onSurfaceVariant = DoadoWhite,

    surfaceContainerLowest = DoadoBlack,
    surfaceContainerLow = DoadoInk,
    surfaceContainer = DoadoInk,
    surfaceContainerHigh = DoadoNavy,
    surfaceContainerHighest = DoadoNavy,

    inverseSurface = DoadoWhite,
    inverseOnSurface = DoadoNavy,
    inversePrimary = DoadoNavy,

    outline = DoadoWhite,
    outlineVariant = DoadoSteel,
    scrim = DoadoBlack,
    surfaceTint = Color.Transparent,
)

/**
 * Tema raíz de Doado.
 *
 * A propósito **no** usamos color dinámico (Material You): tomar los colores del
 * fondo de pantalla del usuario haría imposible garantizar el contraste, que es
 * justo lo que esta app no se puede permitir perder.
 */
@Composable
fun DoadoTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DoadoDarkColors else DoadoLightColors,
        typography = DoadoTypography,
        content = content,
    )
}
