package com.doado.core.ui

import androidx.compose.ui.unit.dp

/**
 * Medidas compartidas de Doado.
 *
 * [MinTouchTarget] es el suelo innegociable: 48dp es el mínimo que piden tanto
 * Material como las WCAG. En la práctica Doado usa objetivos bastante más
 * grandes, porque el temblor de manos y la vista cansada hacen que 48dp siga
 * siendo un blanco difícil.
 */
object DoadoDimens {
    /** Mínimo absoluto para cualquier elemento pulsable. Nunca bajar de aquí. */
    val MinTouchTarget = 48.dp

    /** Objetivo táctil recomendado en Doado para botones sueltos. */
    val ComfortableTouchTarget = 72.dp

    /** Alto mínimo de una casilla del grid de favoritos. */
    val TileMinHeight = 168.dp

    /** Tamaño del icono dentro de la casilla. */
    val TileIconSize = 72.dp

    val TileCornerRadius = 24.dp
    val TileBorderWidth = 3.dp

    val ScreenPadding = 20.dp
    val GridSpacing = 16.dp
}
