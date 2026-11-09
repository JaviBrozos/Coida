package com.coida.core.ui

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
 * Convención de roles que usan las pantallas de Coida (en los dos esquemas):
 *  · primary          -> iconos de acento, botón principal, estado activo.
 *  · surfaceContainer -> fondo de casillas y filas.
 *  · surfaceContainerHigh -> casilla pulsada y fondo de las notas.
 *  · surfaceContainerLowest -> plato hundido detrás de una inicial y del micrófono
 *    del asistente. Los iconos de casillas y filas van sin plato (D-35).
 *  · outline          -> contorno de casillas y botones.
 *  · onSurfaceVariant -> texto secundario.
 *  · outlineVariant   -> contenido apagado (app no disponible, acción bloqueada).
 */
private val CoidaLightColors = lightColorScheme(
    primary = CoidaNavy,
    onPrimary = CoidaWhite,
    primaryContainer = CoidaMist,
    onPrimaryContainer = CoidaNavy,

    secondary = CoidaCyanDeep,
    onSecondary = CoidaWhite,
    secondaryContainer = CoidaMistDim,
    onSecondaryContainer = CoidaNavy,

    tertiary = CoidaCyanDeep,
    onTertiary = CoidaWhite,
    tertiaryContainer = CoidaMist,
    onTertiaryContainer = CoidaNavy,

    error = CoidaError,
    onError = CoidaWhite,
    errorContainer = Color(0xFFF9DEDC),
    onErrorContainer = Color(0xFF410E0B),

    background = CoidaWhite,
    onBackground = CoidaNavy,
    surface = CoidaWhite,
    onSurface = CoidaNavy,
    surfaceVariant = CoidaMist,
    // Cian oscurecido: 6,71:1 sobre blanco, cumple AA aun siendo secundario.
    onSurfaceVariant = CoidaCyanDeep,

    // El plato de iniciales y del asistente: marino sobre Mist, 15,18:1.
    surfaceContainerLowest = CoidaMist,
    surfaceContainerLow = CoidaWhite,
    surfaceContainer = CoidaWhite,
    surfaceContainerHigh = CoidaMist,
    surfaceContainerHighest = CoidaMistDim,

    inverseSurface = CoidaNavy,
    inverseOnSurface = CoidaWhite,
    inversePrimary = CoidaCyanLight,

    outline = CoidaNavy,
    // Gris apagado propio del claro: 5,71:1 sobre blanco. Ver [CoidaSteelDeep].
    outlineVariant = CoidaSteelDeep,
    scrim = CoidaBlack,
    // Sin tinte por elevación: el diseño es plano y separa por borde, no por sombra.
    surfaceTint = Color.Transparent,
)

/**
 * Esquema oscuro, el de fábrica: fondo tinta, casillas elevadas con contorno
 * fino y el cian de la marca como acento. Todo el texto pasa de 9:1.
 */
private val CoidaDarkColors = darkColorScheme(
    primary = CoidaCyan,
    onPrimary = CoidaInk,
    primaryContainer = CoidaSurfaceHigh,
    onPrimaryContainer = CoidaText,

    secondary = CoidaTextMuted,
    onSecondary = CoidaInk,
    secondaryContainer = CoidaSurfaceHigh,
    onSecondaryContainer = CoidaText,

    tertiary = CoidaCyan,
    onTertiary = CoidaInk,
    tertiaryContainer = CoidaSurfaceHigh,
    onTertiaryContainer = CoidaText,

    error = CoidaErrorLight,
    onError = CoidaOnErrorLight,
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6),

    background = CoidaInk,
    onBackground = CoidaText,
    surface = CoidaInk,
    onSurface = CoidaText,
    surfaceVariant = CoidaSurfaceHigh,
    onSurfaceVariant = CoidaTextMuted,

    // El plato de iniciales y del asistente: cian sobre tinta, 7,86:1.
    surfaceContainerLowest = CoidaInk,
    surfaceContainerLow = CoidaInk,
    surfaceContainer = CoidaSurface,
    surfaceContainerHigh = CoidaSurfaceHigh,
    surfaceContainerHighest = CoidaSurfaceHigh,

    inverseSurface = CoidaText,
    inverseOnSurface = CoidaInk,
    inversePrimary = CoidaNavy,

    outline = CoidaLine,
    // 7,17:1 sobre la casilla, que es el fondo real del texto apagado. El gris
    // anterior daba 4,59:1 ahí: pasaba el mínimo, no el objetivo del proyecto.
    outlineVariant = CoidaSteelLight,
    scrim = CoidaBlack,
    surfaceTint = Color.Transparent,
)

/**
 * Tema raíz de Coida.
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
fun CoidaTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    CompositionLocalProvider(LocalOverscrollFactory provides null) {
        MaterialTheme(
            colorScheme = if (darkTheme) CoidaDarkColors else CoidaLightColors,
            typography = CoidaTypography,
            content = content,
        )
    }
}
