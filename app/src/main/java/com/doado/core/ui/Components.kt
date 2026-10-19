package com.doado.core.ui

import androidx.annotation.DrawableRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.inset
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.paneTitle
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/*
 * Piezas comunes de las pantallas de Doado.
 *
 * Todas siguen la misma receta: superficie plana con contorno de 2dp (nunca
 * sombra), el contorno pasa al color de acento mientras se pulsa, y el área
 * pulsable es la pieza entera, nunca un botoncito dentro.
 *
 * Al pulsar, además, el móvil da un toque de vibración cuando el toque cuenta:
 * quien tiene el pulso o la vista cansados nota que ha acertado sin tener que
 * mirar. Va en el toque y no al apoyar el dedo, que en una lista que se desplaza
 * vibraría también al arrastrar, confirmando algo que no ha pasado. Es la
 * vibración de teclado de Android: si la persona la tiene apagada en el sistema,
 * aquí tampoco suena.
 */

/**
 * Superficie pulsable con contorno. Es la base de casillas, filas y botones.
 *
 * @param highlighted contorno de acento aunque no se esté pulsando: para marcar
 *   lo importante (el modo administrador activo, por ejemplo).
 * @param borderWidth grosor del contorno. Solo cambia para señalar algo
 *   ([DoadoDimens.HighlightBorderWidth], cuando guía el asistente).
 */
@Composable
fun DoadoCard(
    onClick: (() -> Unit)?,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    highlighted: Boolean = false,
    shape: Shape = RoundedCornerShape(DoadoDimens.CardCornerRadius),
    color: Color = MaterialTheme.colorScheme.surfaceContainer,
    borderWidth: Dp = DoadoDimens.BorderWidth,
    content: @Composable () -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val feedback = rememberClickFeedback()

    val borderColor = when {
        !enabled -> colors.outlineVariant
        pressed || highlighted -> colors.primary
        else -> colors.outline
    }
    val border = BorderStroke(borderWidth, borderColor)
    val background = if (pressed) colors.surfaceContainerHigh else color

    if (onClick == null) {
        Surface(modifier = modifier, shape = shape, color = background, border = border) {
            content()
        }
    } else {
        Surface(
            onClick = { feedback(); onClick() },
            // El Surface pulsable de Material no dice que es un botón; sin esto
            // TalkBack decía «botón» en unas piezas y en otras no.
            modifier = modifier.semantics { role = Role.Button },
            enabled = enabled,
            shape = shape,
            color = background,
            border = border,
            interactionSource = interaction,
        ) {
            content()
        }
    }
}

/** El toque de vibración de un toque que cuenta. Ver la cabecera del fichero. */
@Composable
internal fun rememberClickFeedback(): () -> Unit {
    val haptic = LocalHapticFeedback.current
    return remember(haptic) { { haptic.performHapticFeedback(HapticFeedbackType.VirtualKey) } }
}

/**
 * Icono de acento a la izquierda de filas y tarjetas. Por defecto va sin plato,
 * solo la silueta (D-35): cian sobre la casilla 6,34:1, marino sobre blanco
 * 17,08:1. [plate] queda para el círculo del asistente, que sí lo lleva.
 */
@Composable
fun IconBadge(
    @DrawableRes icon: Int,
    modifier: Modifier = Modifier,
    size: Dp = 48.dp,
    tint: Color = MaterialTheme.colorScheme.primary,
    plate: Color = Color.Transparent,
    shape: Shape = RoundedCornerShape(DoadoDimens.ControlCornerRadius),
) {
    Box(
        modifier = modifier
            .size(size)
            .background(plate, shape),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            painter = painterResource(icon),
            contentDescription = null,
            modifier = Modifier.size(size * 0.58f),
            tint = tint,
        )
    }
}

/**
 * La inicial de un nombre sobre el mismo plato que [IconBadge]: el contacto del
 * inicio y las apps de la lista de ajustes. Es decoración: el nombre ya va
 * escrito al lado, y TalkBack no la lee: dentro de una fila que es un solo nodo
 * pulsable, la arrastraría al principio y leería «W, WhatsApp, interruptor».
 * Tampoco imita el logotipo de ninguna app.
 */
@Composable
fun InitialBadge(
    name: String,
    modifier: Modifier = Modifier,
    size: Dp = 48.dp,
    color: Color = MaterialTheme.colorScheme.primary,
    plate: Color = MaterialTheme.colorScheme.surfaceContainerLowest,
    shape: Shape = RoundedCornerShape(DoadoDimens.ControlCornerRadius),
    textStyle: TextStyle = MaterialTheme.typography.titleSmall,
) {
    Box(
        modifier = modifier
            .clearAndSetSemantics {}
            .size(size)
            .background(plate, shape),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = name.trim().take(1).uppercase(),
            style = textStyle,
            color = color,
        )
    }
}

/**
 * Cabecera de las pantallas de ajustes: «Volver» arriba, y debajo el título y
 * una frase de contexto a todo lo ancho.
 *
 * El título va solo en su línea para que a letra grande no tenga que compartirla
 * con el botón y se parta en tres. El botón lleva la palabra escrita, no solo la
 * flecha: el familiar no tiene por qué saber qué significa un icono. Para
 * TalkBack dice adónde vuelve («Volver a ajustes»), que empieza por lo que se ve.
 */
@Composable
fun ScreenHeader(
    title: String,
    backLabel: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
) {
    // Título de panel: al llegar a la pantalla, TalkBack dice dónde está. Sin él,
    // con la pantalla anterior ya fuera de su árbol, no anunciaba nada.
    Column(
        modifier = modifier
            .fillMaxWidth()
            .semantics { paneTitle = title },
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        DoadoCard(
            onClick = onBack,
            modifier = Modifier
                .heightIn(min = DoadoDimens.BackButtonHeight)
                .semantics { contentDescription = backLabel },
            shape = CircleShape,
            color = Color.Transparent,
        ) {
            Row(
                modifier = Modifier.padding(start = 14.dp, end = 22.dp, top = 8.dp, bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Icon(
                    painter = painterResource(com.doado.R.drawable.ic_back),
                    contentDescription = null,
                    modifier = Modifier.size(28.dp),
                    tint = MaterialTheme.colorScheme.onSurface,
                )
                // Fuera del árbol de TalkBack: si no, leería la etiqueta y luego
                // otra vez «Volver».
                Text(
                    text = stringResource(com.doado.R.string.back),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.clearAndSetSemantics {},
                )
            }
        }
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.semantics { heading() },
            )
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

/** Rótulo de un grupo de filas. */
@Composable
fun SectionLabel(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier
            .padding(top = 12.dp, start = 4.dp)
            .semantics { heading() },
    )
}

/**
 * Dónde cae una fila dentro de un grupo de ajustes.
 *
 * Las filas de un mismo apartado van juntas en un solo bloque con contorno, y
 * entre ellas una raya fina; no cada una en su caja. Es el mismo contenido con
 * la mitad de líneas, y se ve de un vistazo qué filas son del mismo apartado.
 */
enum class GroupPosition {
    Single, First, Middle, Last;

    internal val roundsTop get() = this == Single || this == First
    internal val roundsBottom get() = this == Single || this == Last

    companion object {
        /** La posición de la fila [index] de un grupo de [count] filas. */
        fun of(index: Int, count: Int): GroupPosition = when {
            count <= 1 -> Single
            index == 0 -> First
            index == count - 1 -> Last
            else -> Middle
        }
    }
}

/** Grupo de filas que van pegadas, sin el hueco de [SettingsColumn] entre ellas. */
@Composable
fun SettingsGroup(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Column(modifier = modifier.fillMaxWidth(), content = content)
}

/** Fila de ajustes que lleva a otra pantalla. */
@Composable
fun SettingsRow(
    @DrawableRes icon: Int,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    highlighted: Boolean = false,
    position: GroupPosition = GroupPosition.Single,
) {
    val interaction = remember { MutableInteractionSource() }
    val feedback = rememberClickFeedback()
    GroupedRow(
        position = position,
        interaction = interaction,
        highlighted = highlighted,
        minHeight = DoadoDimens.RowHeight,
        modifier = modifier,
        interactive = Modifier.clickable(
            interactionSource = interaction,
            indication = ripple(),
            role = Role.Button,
            onClick = { feedback(); onClick() },
        ),
    ) {
        IconBadge(icon)
        TitleAndSubtitle(title, subtitle, Modifier.weight(1f))
        Icon(
            painter = painterResource(com.doado.R.drawable.ic_chevron_right),
            contentDescription = null,
            modifier = Modifier.size(24.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/**
 * Fila con interruptor. Toda la fila es el interruptor: así TalkBack la anuncia
 * como un único control y no hace falta atinar con el dedo en la bolita.
 */
@Composable
fun SwitchRow(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    leading: (@Composable () -> Unit)? = null,
    position: GroupPosition = GroupPosition.Single,
) {
    val interaction = remember { MutableInteractionSource() }
    val feedback = rememberClickFeedback()
    GroupedRow(
        position = position,
        interaction = interaction,
        highlighted = false,
        minHeight = DoadoDimens.ComfortableTouchTarget,
        modifier = modifier,
        interactive = Modifier.toggleable(
            value = checked,
            interactionSource = interaction,
            indication = ripple(),
            role = Role.Switch,
            onValueChange = { feedback(); onCheckedChange(it) },
        ),
    ) {
        leading?.invoke()
        TitleAndSubtitle(title, subtitle, Modifier.weight(1f))
        DoadoSwitchVisual(checked)
    }
}

/**
 * Lo común a las filas de un grupo: fondo, contorno compartido y estado pulsado.
 *
 * El contorno no es el de un `Surface`, porque entonces entre dos filas saldría
 * una línea doble. Cada fila pinta solo su parte: los lados, la curva si es la
 * primera o la última, y arriba la raya fina que la separa de la anterior.
 * Pulsada o resaltada, se recorta entera en acento, como cualquier otra pieza.
 */
@Composable
private fun GroupedRow(
    position: GroupPosition,
    interaction: MutableInteractionSource,
    highlighted: Boolean,
    minHeight: Dp,
    modifier: Modifier,
    interactive: Modifier,
    content: @Composable RowScope.() -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    val pressed by interaction.collectIsPressedAsState()
    val radius = DoadoDimens.CardCornerRadius
    val shape = RoundedCornerShape(
        topStart = if (position.roundsTop) radius else 0.dp,
        topEnd = if (position.roundsTop) radius else 0.dp,
        bottomStart = if (position.roundsBottom) radius else 0.dp,
        bottomEnd = if (position.roundsBottom) radius else 0.dp,
    )
    val outline = colors.outline
    val accent = colors.primary
    val showAccent = pressed || highlighted

    Box(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = minHeight)
            .drawWithContent {
                drawContent()
                val stroke = DoadoDimens.BorderWidth.toPx()
                val r = radius.toPx()
                // Donde sigue otra fila, el contorno se alarga por fuera y se
                // recorta: así de ese lado solo quedan los laterales, rectos.
                val overflow = r + stroke
                val top = if (position.roundsTop) stroke / 2 else -overflow
                val bottom = if (position.roundsBottom) size.height - stroke / 2 else size.height + overflow
                clipRect {
                    drawRoundRect(
                        color = outline,
                        topLeft = Offset(stroke / 2, top),
                        size = Size(size.width - stroke, bottom - top),
                        cornerRadius = CornerRadius(r - stroke / 2),
                        style = Stroke(stroke),
                    )
                }
                if (!position.roundsTop) {
                    val divider = DoadoDimens.DividerWidth.toPx()
                    drawLine(
                        color = outline,
                        start = Offset(stroke, divider / 2),
                        end = Offset(size.width - stroke, divider / 2),
                        strokeWidth = divider,
                    )
                }
                if (showAccent) {
                    inset(stroke / 2) {
                        drawOutline(
                            outline = shape.createOutline(this.size, layoutDirection, this),
                            color = accent,
                            style = Stroke(stroke),
                        )
                    }
                }
            }
            .clip(shape)
            .background(if (pressed) colors.surfaceContainerHigh else colors.surfaceContainer)
            .then(interactive),
        contentAlignment = Alignment.CenterStart,
    ) {
        RowContent(content)
    }
}

/** Solo el dibujo del interruptor; la semántica la pone la fila que lo contiene. */
@Composable
fun DoadoSwitchVisual(checked: Boolean, modifier: Modifier = Modifier) {
    val colors = MaterialTheme.colorScheme
    Box(
        modifier = modifier
            .size(width = 72.dp, height = 40.dp)
            .background(if (checked) colors.primary else colors.background, CircleShape)
            .border(DoadoDimens.BorderWidth, if (checked) colors.primary else colors.outline, CircleShape)
            .padding(6.dp),
        contentAlignment = if (checked) Alignment.CenterEnd else Alignment.CenterStart,
    ) {
        Box(
            modifier = Modifier
                .size(26.dp)
                .background(if (checked) colors.onPrimary else colors.onSurfaceVariant, CircleShape),
        )
    }
}

/**
 * Grupo de opciones excluyentes (tamaño de letra, tema...), como un selector
 * segmentado: un solo marco y la opción elegida rellena de acento.
 *
 * En fila solo caben dos opciones con letra normal. Con tres, o con la letra
 * grande, cada opción va en su línea a todo lo ancho: en fila, «Muy grande» se
 * partía en «gran/de» justo en el selector del tamaño de letra.
 */
@Composable
fun ChoiceGroup(
    options: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val outer = RoundedCornerShape(DoadoDimens.ControlCornerRadius + SEGMENT_GAP)
    val stacked = options.size > 2 || LocalDensity.current.fontScale >= DoadoDimens.LargeTextScale
    val frame = modifier
        .fillMaxWidth()
        .border(DoadoDimens.BorderWidth, MaterialTheme.colorScheme.outline, outer)
        .padding(SEGMENT_GAP)
        .selectableGroup()

    if (stacked) {
        Column(modifier = frame, verticalArrangement = Arrangement.spacedBy(SEGMENT_GAP)) {
            options.forEachIndexed { index, label ->
                ChoiceSegment(label, index == selectedIndex, { onSelect(index) }, Modifier.fillMaxWidth())
            }
        }
    } else {
        // Todas miden lo mismo de alto aunque una se parta en dos líneas.
        Row(
            modifier = frame.height(IntrinsicSize.Min),
            horizontalArrangement = Arrangement.spacedBy(SEGMENT_GAP),
        ) {
            options.forEachIndexed { index, label ->
                ChoiceSegment(
                    label,
                    index == selectedIndex,
                    { onSelect(index) },
                    Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                )
            }
        }
    }
}

@Composable
private fun ChoiceSegment(label: String, selected: Boolean, onClick: () -> Unit, modifier: Modifier) {
    val colors = MaterialTheme.colorScheme
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val feedback = rememberClickFeedback()
    Box(
        modifier = modifier
            .heightIn(min = 56.dp)
            .clip(RoundedCornerShape(DoadoDimens.ControlCornerRadius))
            .background(
                when {
                    selected -> colors.primary
                    pressed -> colors.surfaceContainerHigh
                    else -> Color.Transparent
                },
            )
            .selectable(
                selected = selected,
                interactionSource = interaction,
                indication = ripple(),
                role = Role.RadioButton,
                onClick = { feedback(); onClick() },
            )
            .padding(horizontal = 6.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = if (selected) colors.onPrimary else colors.onSurface,
            textAlign = TextAlign.Center,
        )
    }
}


/** Botón principal: relleno de acento. Uno por pantalla como mucho. */
@Composable
fun PrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    @DrawableRes icon: Int? = null,
    enabled: Boolean = true,
) {
    val colors = MaterialTheme.colorScheme
    DoadoCard(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = DoadoDimens.ComfortableTouchTarget + 4.dp),
        enabled = enabled,
        color = if (enabled) colors.primary else colors.surfaceContainer,
        highlighted = enabled,
    ) {
        ButtonContent(text, icon, if (enabled) colors.onPrimary else colors.outlineVariant)
    }
}

/**
 * Botón secundario: solo contorno.
 *
 * @param disabledReason qué decir cuando está apagado. Sin esto, TalkBack lee
 *   "Abrir la tienda de apps, desactivado" y se acabó: quien no ve el gris no
 *   tiene forma de saber que falta desbloquear el modo administrador.
 */
@Composable
fun OutlineButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    @DrawableRes icon: Int? = null,
    enabled: Boolean = true,
    disabledReason: String? = null,
) {
    val colors = MaterialTheme.colorScheme
    DoadoCard(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = DoadoDimens.ComfortableTouchTarget + 4.dp)
            .semantics {
                if (!enabled && disabledReason != null) contentDescription = disabledReason
            },
        enabled = enabled,
        color = Color.Transparent,
    ) {
        ButtonContent(text, icon, if (enabled) colors.onSurface else colors.outlineVariant)
    }
}

/**
 * Campo para escribir texto, con el mismo contorno de 2 dp que el resto de piezas.
 *
 * El contorno propio de Material mide 1 dp y sobre el fondo oscuro apenas se
 * veía: el texto de ejemplo parecía una frase más de la pantalla y no se sabía
 * que ahí se escribe. Por eso se apaga y se dibuja el de Doado encima, que pasa
 * al color de acento mientras se escribe y al de error si la respuesta falla.
 *
 * @param onDone qué hacer al pulsar «Hecho» en el teclado.
 */
@Composable
fun DoadoTextField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    isError: Boolean = false,
    singleLine: Boolean = true,
    minLines: Int = 1,
    onDone: () -> Unit = {},
) {
    val colors = MaterialTheme.colorScheme
    val shape = RoundedCornerShape(DoadoDimens.CardCornerRadius)
    val interaction = remember { MutableInteractionSource() }
    val focused by interaction.collectIsFocusedAsState()

    val borderColor = when {
        isError -> colors.error
        focused -> colors.primary
        else -> colors.outline
    }

    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier
            .fillMaxWidth()
            .border(DoadoDimens.BorderWidth, borderColor, shape),
        enabled = enabled,
        placeholder = {
            Text(
                text = placeholder,
                style = MaterialTheme.typography.bodyMedium,
                color = colors.onSurfaceVariant,
            )
        },
        textStyle = MaterialTheme.typography.bodyMedium,
        isError = isError,
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
        keyboardActions = KeyboardActions(onDone = { onDone() }),
        singleLine = singleLine,
        minLines = if (singleLine) 1 else minLines,
        shape = shape,
        interactionSource = interaction,
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = Color.Transparent,
            unfocusedBorderColor = Color.Transparent,
            errorBorderColor = Color.Transparent,
            disabledBorderColor = Color.Transparent,
            focusedContainerColor = colors.surfaceContainer,
            unfocusedContainerColor = colors.surfaceContainer,
            errorContainerColor = colors.surfaceContainer,
            disabledContainerColor = colors.surfaceContainer,
        ),
    )
}

/**
 * Nota explicativa: icono de información y una o dos frases. No es pulsable.
 *
 * Lleva relleno, que la distingue de botones y filas, y además contorno: solo
 * el relleno se quedaba en 1,13:1 contra el fondo en claro (1,49:1 en oscuro),
 * que es separar por elevación, lo que D-04 descarta.
 */
@Composable
fun NoteCard(
    text: String,
    modifier: Modifier = Modifier,
    @DrawableRes icon: Int = com.doado.R.drawable.ic_info,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(
                MaterialTheme.colorScheme.surfaceContainerHigh,
                RoundedCornerShape(DoadoDimens.CardCornerRadius),
            )
            .border(
                DoadoDimens.BorderWidth,
                MaterialTheme.colorScheme.outline,
                RoundedCornerShape(DoadoDimens.CardCornerRadius),
            )
            .padding(horizontal = 18.dp, vertical = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Icon(
            painter = painterResource(icon),
            contentDescription = null,
            modifier = Modifier
                .padding(top = 2.dp)
                .size(26.dp),
            tint = MaterialTheme.colorScheme.primary,
        )
        // Texto principal y no secundario: sobre el relleno de la nota el
        // secundario bajaba a 6,65:1 en oscuro y 5,97:1 en claro.
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

/** Columna de pantalla de ajustes con el hueco estándar entre piezas. */
@Composable
fun SettingsColumn(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier = modifier.padding(PaddingValues(DoadoDimens.ScreenPadding)),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        content = content,
    )
}

@Composable
private fun RowContent(content: @Composable RowScope.() -> Unit) {
    Row(
        modifier = Modifier.padding(horizontal = 18.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        content = content,
    )
}

@Composable
private fun TitleAndSubtitle(title: String, subtitle: String?, modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurface,
        )
        if (subtitle != null) {
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun ButtonContent(text: String, @DrawableRes icon: Int?, contentColor: Color) {
    Row(
        modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally),
    ) {
        if (icon != null) {
            Icon(
                painter = painterResource(icon),
                contentDescription = null,
                modifier = Modifier.size(26.dp),
                tint = contentColor,
            )
        }
        Text(
            text = text,
            style = MaterialTheme.typography.titleSmall,
            color = contentColor,
            textAlign = TextAlign.Center,
        )
    }
}

/** Hueco entre el marco de un [ChoiceGroup] y sus opciones. */
private val SEGMENT_GAP = 6.dp
