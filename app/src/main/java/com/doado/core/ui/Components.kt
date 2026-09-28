package com.doado.core.ui

import androidx.annotation.DrawableRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/*
 * Piezas comunes de las pantallas de Doado.
 *
 * Todas siguen la misma receta: superficie plana con contorno de 2dp (nunca
 * sombra), el contorno pasa al color de acento mientras se pulsa, y el área
 * pulsable es la pieza entera, nunca un botoncito dentro.
 */

/**
 * Superficie pulsable con contorno. Es la base de casillas, filas y botones.
 *
 * @param highlighted contorno de acento aunque no se esté pulsando: para marcar
 *   lo importante (el modo administrador activo, por ejemplo).
 */
@Composable
fun DoadoCard(
    onClick: (() -> Unit)?,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    highlighted: Boolean = false,
    shape: RoundedCornerShape = RoundedCornerShape(DoadoDimens.CardCornerRadius),
    color: Color = MaterialTheme.colorScheme.surfaceContainer,
    content: @Composable () -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()

    val borderColor = when {
        !enabled -> colors.outlineVariant
        pressed || highlighted -> colors.primary
        else -> colors.outline
    }
    val border = BorderStroke(DoadoDimens.BorderWidth, borderColor)
    val background = if (pressed) colors.surfaceContainerHigh else color

    if (onClick == null) {
        Surface(modifier = modifier, shape = shape, color = background, border = border) {
            content()
        }
    } else {
        Surface(
            onClick = onClick,
            modifier = modifier,
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

/** Cuadrito con un icono de acento, a la izquierda de filas y tarjetas. */
@Composable
fun IconBadge(
    @DrawableRes icon: Int,
    modifier: Modifier = Modifier,
    size: Dp = 48.dp,
    tint: Color = MaterialTheme.colorScheme.primary,
) {
    Box(
        modifier = modifier
            .size(size)
            .background(
                MaterialTheme.colorScheme.surfaceContainerHigh,
                RoundedCornerShape(DoadoDimens.ControlCornerRadius),
            ),
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

/** Cabecera de las pantallas de ajustes: volver, título y una frase de contexto. */
@Composable
fun ScreenHeader(
    title: String,
    backLabel: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        DoadoCard(
            onClick = onBack,
            modifier = Modifier
                .size(DoadoDimens.ComfortableTouchTarget - 12.dp),
            shape = RoundedCornerShape(16.dp),
            color = Color.Transparent,
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    painter = painterResource(com.doado.R.drawable.ic_back),
                    contentDescription = backLabel,
                    modifier = Modifier.size(28.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        Column(modifier = Modifier.weight(1f)) {
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
            .padding(top = 8.dp)
            .semantics { heading() },
    )
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
) {
    DoadoCard(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = DoadoDimens.RowHeight),
        highlighted = highlighted,
    ) {
        RowContent {
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
) {
    val colors = MaterialTheme.colorScheme
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = DoadoDimens.ComfortableTouchTarget)
            .toggleable(value = checked, role = Role.Switch, onValueChange = onCheckedChange),
        shape = RoundedCornerShape(DoadoDimens.CardCornerRadius),
        color = colors.surfaceContainer,
        border = BorderStroke(DoadoDimens.BorderWidth, colors.outline),
    ) {
        RowContent {
            leading?.invoke()
            TitleAndSubtitle(title, subtitle, Modifier.weight(1f))
            DoadoSwitchVisual(checked)
        }
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

/** Grupo de opciones excluyentes en fila (tamaño de letra, tema...). */
@Composable
fun ChoiceGroup(
    options: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme
    Row(
        modifier = modifier
            .fillMaxWidth()
            .selectableGroup(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        options.forEachIndexed { index, label ->
            val selected = index == selectedIndex
            val shape = RoundedCornerShape(DoadoDimens.ControlCornerRadius)
            Box(
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = 64.dp)
                    .background(if (selected) colors.primary else colors.surfaceContainer, shape)
                    .border(DoadoDimens.BorderWidth, if (selected) colors.primary else colors.outline, shape)
                    .selectable(selected = selected, role = Role.RadioButton, onClick = { onSelect(index) })
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
    }
}

/** Botón principal: relleno de acento. Uno por pantalla como mucho. */
@Composable
fun PrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    @DrawableRes icon: Int? = null,
) {
    val colors = MaterialTheme.colorScheme
    DoadoCard(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = DoadoDimens.ComfortableTouchTarget + 4.dp),
        color = colors.primary,
        highlighted = true,
    ) {
        ButtonContent(text, icon, colors.onPrimary)
    }
}

/** Botón secundario: solo contorno. */
@Composable
fun OutlineButton(
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
        color = Color.Transparent,
    ) {
        ButtonContent(text, icon, if (enabled) colors.onSurface else colors.outlineVariant)
    }
}

/** Nota explicativa: icono de información y una o dos frases. No es pulsable. */
@Composable
fun NoteCard(
    text: String,
    modifier: Modifier = Modifier,
    @DrawableRes icon: Int = com.doado.R.drawable.ic_info,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .border(
                DoadoDimens.BorderWidth,
                MaterialTheme.colorScheme.outline,
                RoundedCornerShape(DoadoDimens.CardCornerRadius),
            )
            .padding(16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Icon(
            painter = painterResource(icon),
            contentDescription = null,
            modifier = Modifier
                .padding(top = 2.dp)
                .size(26.dp),
            tint = MaterialTheme.colorScheme.primary,
        )
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
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
