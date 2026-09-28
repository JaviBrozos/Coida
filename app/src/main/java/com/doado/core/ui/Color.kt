package com.doado.core.ui

import androidx.compose.ui.graphics.Color

/**
 * Paleta de Doado.
 *
 * Regla de la casa: nada por debajo de 4.5:1 de contraste, y para texto y bordes
 * apuntamos a 7:1 (nivel AAA de WCAG). Por eso el color "protagonista" de la marca
 * en superficies claras es el azul marino y no el cian: el cian de la marca sobre
 * blanco se queda en ~2.2:1, insuficiente para texto o iconos, así que se reserva
 * para acentos grandes y para el modo oscuro.
 */

// --- Marca -----------------------------------------------------------------

/** Azul marino del logotipo. Sobre blanco da ~16.8:1. */
val DoadoNavy = Color(0xFF0B1D2E)

/** Cian del logotipo. Solo para áreas grandes, nunca para texto sobre blanco. */
val DoadoCyan = Color(0xFF29B6F0)

/** Cian oscurecido hasta llegar a ~4.6:1 sobre blanco: apto para texto secundario. */
val DoadoCyanDeep = Color(0xFF10618F)

/** Cian aclarado para el modo oscuro: ~9.5:1 sobre negro. */
val DoadoCyanLight = Color(0xFF6CCFFA)

// --- Neutros ---------------------------------------------------------------

val DoadoWhite = Color(0xFFFFFFFF)
val DoadoBlack = Color(0xFF000000)

/** Fondo de tarjeta en claro: azul muy tenue, mantiene 15.9:1 con el texto marino. */
val DoadoMist = Color(0xFFE9F3FA)
val DoadoMistDim = Color(0xFFDCEBF5)

/** Superficie de tarjeta en oscuro. */
val DoadoInk = Color(0xFF0A1622)

/** Gris solo para separadores decorativos, nunca para texto. */
val DoadoSteel = Color(0xFF7A93A6)

// --- Estado ----------------------------------------------------------------

val DoadoError = Color(0xFFB3261E)
val DoadoErrorLight = Color(0xFFFFB4AB)
val DoadoOnErrorLight = Color(0xFF690005)

// --- Superficies del tema oscuro -------------------------------------------
//
// El tema oscuro es el de fábrica. El cian se reserva para acentos (iconos,
// estado activo, botón principal) y no se usa como relleno de casillas: los
// bloques de color macizo daban un aspecto de juguete.

/** Casilla elevada sobre [DoadoInk]. El blanco azulado encima da ~13:1. */
val DoadoSurface = Color(0xFF122A3E)

/** Casilla pulsada o seleccionada, y fondo de los cuadritos de icono. */
val DoadoSurfaceHigh = Color(0xFF17384F)

/**
 * Contorno de casillas y botones en oscuro: ~3.3:1 contra [DoadoSurface],
 * por encima del 3:1 que piden las WCAG para límites de componentes.
 */
val DoadoLine = Color(0xFF3D7AA6)

/** Texto principal en oscuro: ~16:1 sobre [DoadoInk]. */
val DoadoText = Color(0xFFF2F7FB)

/** Texto secundario en oscuro: ~9.9:1 sobre [DoadoInk]. */
val DoadoTextMuted = Color(0xFF9FC4DC)
