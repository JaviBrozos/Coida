package com.doado.core.ui

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.intl.LocaleList
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
 * La letra es Inter (D-50), con su juego `ss04` (la I con remates y la l con
 * cola), para que «Isabel» no se lea «lsabel». Va dentro del APK, así que se ve
 * igual en todas las marcas y no depende de ninguna descarga.
 *
 * Todo va en sp, así que además respeta el tamaño de fuente que el usuario haya
 * configurado en los ajustes de accesibilidad del sistema.
 */

/**
 * Los dos grosores que usa la escala, los únicos que trae el APK (cada uno pesa
 * unos 400 KB). Uno que no esté, Android lo fingiría engordando otro, así que
 * cualquier grosor nuevo de la escala tiene que tener aquí su fichero.
 */
private val Inter = FontFamily(
    Font(R.font.inter_semibold, FontWeight.SemiBold),
    Font(R.font.inter_bold, FontWeight.Bold),
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
    fontFamily = Inter,
    fontWeight = weight,
    fontSize = size.sp,
    lineHeight = lineHeight.sp,
    letterSpacing = 0.sp,
    lineHeightStyle = Legible,
    // La I con remates y la l con cola: en Inter son dos palos casi iguales.
    fontFeatureSettings = LetterForms,
    // Para partir por sílabas con las reglas del español aunque el móvil esté en
    // otro idioma (en gallego no hay reglas y «disponible» se partía sin guion):
    // el texto de Doado siempre está en español.
    localeList = LocaleList("es-ES"),
)

val DoadoTypography = Typography(
    // Cifras y rótulos muy grandes (reservado para pantallas tipo reloj).
    displayLarge = doadoStyle(64, 84, FontWeight.Bold),
    displayMedium = doadoStyle(52, 68, FontWeight.Bold),
    displaySmall = doadoStyle(44, 58, FontWeight.Bold),

    // Titulares de pantalla.
    headlineLarge = doadoStyle(40, 52, FontWeight.Bold),
    headlineMedium = doadoStyle(34, 46, FontWeight.Bold),
    headlineSmall = doadoStyle(30, 40, FontWeight.SemiBold),

    // Títulos dentro de la pantalla.
    titleLarge = doadoStyle(30, 40, FontWeight.Bold),
    titleMedium = doadoStyle(26, 34, FontWeight.SemiBold),
    titleSmall = doadoStyle(22, 32, FontWeight.SemiBold),

    // Cuerpo de texto.
    bodyLarge = doadoStyle(24, 34, FontWeight.SemiBold),
    bodyMedium = doadoStyle(22, 32, FontWeight.SemiBold),
    bodySmall = doadoStyle(20, 28, FontWeight.SemiBold),

    // Etiquetas de botones y de las casillas del grid.
    labelLarge = doadoStyle(24, 32, FontWeight.SemiBold),
    labelMedium = doadoStyle(20, 28, FontWeight.SemiBold),
    labelSmall = doadoStyle(18, 26, FontWeight.SemiBold),
)

/**
 * Cifras de ancho fijo, para lo que cambia mientras se mira (el reloj y las
 * cuentas atrás) y para las teclas del PIN: con cifras de anchos distintos, la
 * hora baila de lado a cada minuto. Se usa con
 * `style.copy(fontFeatureSettings = TabularFigures)`, que sustituye los rasgos de
 * la escala en vez de sumarlos: por eso repite los de [LetterForms].
 */
const val TabularFigures = "ss04, tnum"

/** Las formas de letra que se distinguen mejor, en toda la escala. */
private const val LetterForms = "ss04"
