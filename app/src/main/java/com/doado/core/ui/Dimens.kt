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

    /**
     * Con dos o más por fila la casilla es cuadrada, de lado lo que dé el ancho
     * (D-54): el icono, los márgenes y la letra van en proporción a ese lado. El
     * icono ocupa [TileIconRatio] del lado, entre [TileIconMin] y [TileIconMax].
     */
    const val TileIconRatio = 0.4f
    val TileIconMin = 24.dp
    val TileIconMax = 96.dp

    /** Margen interior de la casilla cuadrada: [TilePaddingRatio] del lado, entre estos dos. */
    const val TilePaddingRatio = 0.08f
    val TilePaddingMin = 4.dp
    val TilePaddingMax = 16.dp

    /** Alto de la casilla de una por fila, que va en horizontal. Fijo: no se estira. */
    val TileRowHeight = 112.dp

    /** Icono de la casilla de una por fila. */
    val TileIconSizeCompact = 54.dp

    /** Hueco entre casillas con cuatro por fila: con el de siempre, cada una perdía 6 dp. */
    val GridSpacingDense = 10.dp

    /**
     * Radio de casilla y de contacto del inicio: [TileCornerRatio] del lado, como
     * mucho [TileCornerRadius]. Fijo, en una casilla pequeña era casi un círculo.
     */
    val TileCornerRadius = 28.dp
    const val TileCornerRatio = 0.18f

    /** Foto o inicial de un contacto en Ajustes → Contactos. */
    val AvatarSize = 52.dp

    /**
     * Separador entre las filas de un grupo de ajustes. Del mismo grosor que un
     * contorno: separa dos pulsables distintos, y a 1dp, en oscuro (3,18:1), era
     * la frontera más débil de la app. No sale doble raya: solo la pinta la fila
     * de abajo. (Un número y no [BorderWidth], que se declara más abajo y aquí
     * aún valdría cero.)
     */
    val DividerWidth = 2.dp

    /** Alto del botón «Volver» de las cabeceras de ajustes. */
    val BackButtonHeight = 60.dp

    /** Alto mínimo de una tecla del PIN: crece con la letra, nunca baja de aquí. */
    val PinKeyHeight = 64.dp

    /** Ancho máximo del teclado del PIN, centrado: más ancho, las teclas eran losas. */
    val PinPadMaxWidth = 320.dp

    /** Círculo con el micrófono arriba de la pantalla del asistente. No es pulsable. */
    val AssistantBadgeSize = 64.dp

    /** El sitio del micrófono que late mientras escucha: el círculo y sus anillos. */
    val ListeningPulseHeight = 200.dp

    /**
     * Escala de letra (la del sistema por la de Doado) a partir de la que las
     * opciones de un [ChoiceGroup] pasan a columna. Es la de «Enorme» con Android
     * en normal. No es una medida, pero vive con ellas porque decide el reparto.
     */
    const val LargeTextScale = 1.3f

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
     * Aro del color del fondo alrededor del círculo, que también se puede pulsar:
     * lo separa de las casillas que pasan por debajo sin recurrir a una sombra.
     */
    val AssistantRingWidth = 8.dp

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
