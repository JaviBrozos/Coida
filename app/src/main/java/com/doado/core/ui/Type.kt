package com.doado.core.ui

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import com.doado.R
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.unit.sp

/**
 * Escala tipográfica de Doado: aproximadamente 1.5x la de Material 3.
 *
 * Material usa 16sp para el cuerpo de texto; aquí el cuerpo arranca en 24sp.
 * Además subimos el grosor (SemiBold en cuerpo y etiquetas, Bold en titulares)
 * porque a tamaños grandes un trazo fino pierde legibilidad para quien tiene
 * poca visión, y dejamos el interletrado a 0: el espaciado negativo de Material
 * apelmaza el texto cuando se amplía.
 *
 * La letra es Atkinson Hyperlegible Next (D-44), hecha para lectores con poca
 * vista: no confunde I, l y 1, ni 0 y O. Va dentro del APK, así que se ve igual en
 * todas las marcas y no depende de ninguna descarga.
 *
 * Todo va en sp, así que además respeta el tamaño de fuente que el usuario haya
 * configurado en los ajustes de accesibilidad del sistema.
 */

/**
 * Los cuatro grosores que trae el APK. Los que no estén declarados, Android los
 * fingiría engordando otro, así que cualquier grosor nuevo de la escala tiene
 * que tener aquí su fichero.
 */
private val Atkinson = FontFamily(
    Font(R.font.atkinson_next_regular, FontWeight.Normal),
    Font(R.font.atkinson_next_medium, FontWeight.Medium),
    Font(R.font.atkinson_next_semibold, FontWeight.SemiBold),
    Font(R.font.atkinson_next_bold, FontWeight.Bold),
)

private val Legible = LineHeightStyle(
    alignment = LineHeightStyle.Alignment.Center,
    trim = LineHeightStyle.Trim.None,
)

private fun doadoStyle(
    size: Int,
    lineHeight: Int,
    weight: FontWeight,
) = TextStyle(
    fontFamily = Atkinson,
    fontWeight = weight,
    fontSize = size.sp,
    lineHeight = lineHeight.sp,
    letterSpacing = 0.sp,
    lineHeightStyle = Legible,
)

val DoadoTypography = Typography(
    // Cifras y rótulos muy grandes (reservado para pantallas tipo reloj).
    displayLarge = doadoStyle(64, 72, FontWeight.Bold),
    displayMedium = doadoStyle(52, 60, FontWeight.Bold),
    displaySmall = doadoStyle(44, 52, FontWeight.Bold),

    // Titulares de pantalla.
    headlineLarge = doadoStyle(40, 48, FontWeight.Bold),
    headlineMedium = doadoStyle(34, 42, FontWeight.Bold),
    headlineSmall = doadoStyle(30, 38, FontWeight.SemiBold),

    // Títulos dentro de la pantalla.
    titleLarge = doadoStyle(30, 38, FontWeight.Bold),
    titleMedium = doadoStyle(26, 34, FontWeight.SemiBold),
    titleSmall = doadoStyle(22, 30, FontWeight.SemiBold),

    // Cuerpo de texto.
    bodyLarge = doadoStyle(24, 34, FontWeight.SemiBold),
    bodyMedium = doadoStyle(22, 30, FontWeight.SemiBold),
    bodySmall = doadoStyle(20, 28, FontWeight.SemiBold),

    // Etiquetas de botones y de las casillas del grid.
    labelLarge = doadoStyle(24, 32, FontWeight.SemiBold),
    labelMedium = doadoStyle(20, 28, FontWeight.SemiBold),
    labelSmall = doadoStyle(18, 26, FontWeight.SemiBold),
)
