package com.doado.core.ui

import androidx.compose.foundation.LocalOverscrollFactory
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color

/**
 * Esquema claro. Texto marino sobre blanco (~16.8:1) y bordes del mismo marino,
 * para que cada casilla tenga un límite visible sin depender de sombras suaves.
 *
 * Convención de roles que usan las pantallas de Doado (en los dos esquemas):
 *  · primary          -> iconos de acento, botón principal, estado activo.
 *  · surfaceContainer -> fondo de casillas y filas.
 *  · surfaceContainerHigh -> casilla pulsada y cuadrito detrás de un icono.
 *  · outline          -> contorno de casillas y botones.
 *  · onSurfaceVariant -> texto secundario.
 *  · outlineVariant   -> contenido apagado (app no disponible, acción bloqueada).
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
    // Cian oscurecido: ~4.6:1 sobre blanco, cumple AA aun siendo secundario.
    onSurfaceVariant = DoadoCyanDeep,

    surfaceContainerLowest = DoadoWhite,
    surfaceContainerLow = DoadoWhite,
    surfaceContainer = DoadoWhite,
    surfaceContainerHigh = DoadoMist,
    surfaceContainerHighest = DoadoMistDim,

    inverseSurface = DoadoNavy,
    inverseOnSurface = DoadoWhite,
    inversePrimary = DoadoCyanLight,

    outline = DoadoNavy,
    // Gris apagado propio del claro: 5,71:1 sobre blanco. Ver [DoadoSteelDeep].
    outlineVariant = DoadoSteelDeep,
    scrim = DoadoBlack,
    // Sin tinte por elevación: el diseño es plano y separa por borde, no por sombra.
    surfaceTint = Color.Transparent,
)

/**
 * Esquema oscuro, el de fábrica: fondo tinta, casillas elevadas con contorno
 * fino y el cian de la marca como acento. Todo el texto pasa de 9:1.
 */
private val DoadoDarkColors = darkColorScheme(
    primary = DoadoCyan,
    onPrimary = DoadoInk,
    primaryContainer = DoadoSurfaceHigh,
    onPrimaryContainer = DoadoText,

    secondary = DoadoTextMuted,
    onSecondary = DoadoInk,
    secondaryContainer = DoadoSurfaceHigh,
    onSecondaryContainer = DoadoText,

    tertiary = DoadoCyan,
    onTertiary = DoadoInk,
    tertiaryContainer = DoadoSurfaceHigh,
    onTertiaryContainer = DoadoText,

    error = DoadoErrorLight,
    onError = DoadoOnErrorLight,
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6),

    background = DoadoInk,
    onBackground = DoadoText,
    surface = DoadoInk,
    onSurface = DoadoText,
    surfaceVariant = DoadoSurfaceHigh,
    onSurfaceVariant = DoadoTextMuted,

    surfaceContainerLowest = DoadoInk,
    surfaceContainerLow = DoadoInk,
    surfaceContainer = DoadoSurface,
    surfaceContainerHigh = DoadoSurfaceHigh,
    surfaceContainerHighest = DoadoSurfaceHigh,

    inverseSurface = DoadoText,
    inverseOnSurface = DoadoInk,
    inversePrimary = DoadoNavy,

    outline = DoadoLine,
    // 7,17:1 sobre la casilla, que es el fondo real del texto apagado. El gris
    // anterior daba 4,59:1 ahí: pasaba el mínimo, no el objetivo del proyecto.
    outlineVariant = DoadoSteelLight,
    scrim = DoadoBlack,
    surfaceTint = Color.Transparent,
)

/**
 * Tema raíz de Doado.
 *
 * A propósito **no** usamos color dinámico (Material You): tomar los colores del
 * fondo de pantalla del usuario haría imposible garantizar el contraste, que es
 * justo lo que esta app no se puede permitir perder.
 *
 * El claro/oscuro lo decide quien configura el móvil desde Ajustes → Interfaz;
 * el valor por defecto del parámetro solo sirve para las previews.
 *
 * Aquí se apaga también el estirado elástico que Compose pone por defecto al
 * llegar al final de una lista. En una app normal es un guiño simpático; en un
 * lanzador para una persona mayor estorba por dos razones. La rejilla del inicio
 * se deja arrastrar aunque las casillas quepan enteras, así que la pantalla se
 * menea sin tener nada que desplazar y parece que algo se ha movido de sitio,
 * justo lo que promete que no pasa. Y el rebote es movimiento que nadie ha
 * pedido: quien tiene mareo o problemas de vista lo agradece quieto.
 */
@Composable
fun DoadoTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    CompositionLocalProvider(LocalOverscrollFactory provides null) {
        MaterialTheme(
            colorScheme = if (darkTheme) DoadoDarkColors else DoadoLightColors,
            typography = DoadoTypography,
            content = content,
        )
    }
}
