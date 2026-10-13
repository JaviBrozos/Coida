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

    val TileCornerRadius = 22.dp

    /**
     * Grosor de contorno de casillas, filas y botones. Con el contorno en un tono
     * que ya da 3:1 contra la casilla, 2dp se ve de sobra sin parecer un dibujo
     * infantil, que es lo que pasaba con 3dp en marino puro.
     */
    val BorderWidth = 2.dp

    /**
     * Borde de la casilla o el contacto que señala el asistente al guiar. El doble
     * del normal para que se note también en gris, sin mover ni agrandar nada.
     */
    val HighlightBorderWidth = 4.dp

    /** Círculo del asistente en el inicio: el pulsable más grande después de las casillas. */
    val AssistantButtonSize = 96.dp

    /**
     * Botón de Ajustes en la esquina del inicio. Pequeño a propósito, porque es del
     * familiar, pero muy por encima del mínimo de 48dp.
     */
    val CornerButtonSize = 64.dp

    /** Alto de una fila de ajustes y de los botones grandes. */
    val RowHeight = 88.dp

    /** Radio de filas, botones y tarjetas de ajustes. */
    val CardCornerRadius = 20.dp

    /** Radio de controles pequeños: opciones, cuadrito de icono. */
    val ControlCornerRadius = 14.dp

    val ScreenPadding = 20.dp
    val GridSpacing = 16.dp
}
