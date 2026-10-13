package com.doado.assistant.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.doado.R
import com.doado.core.ui.DoadoDimens
import com.doado.core.ui.DoadoTheme

/**
 * El círculo del asistente en el inicio (D-28).
 *
 * Relleno de acento con el icono encima: tinta sobre cian (7,86:1) en oscuro y
 * blanco sobre marino (17,08:1) en claro, y el círculo contra el fondo igual. Sin
 * sombra ni elevación (D-04). Mientras se pulsa pasa al fondo de pulsado, como el
 * resto de piezas, con el icono en el color del texto (11,35:1 y 15,18:1; en cian
 * se quedaba en 5,27:1) y el contorno de acento.
 *
 * El icono es provisional y vive en `ic_assistant.xml`: cambiarlo es sustituir
 * ese fichero.
 */
@Composable
fun AssistantButton(onClick: () -> Unit, modifier: Modifier = Modifier) {
    val colors = MaterialTheme.colorScheme
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val label = stringResource(R.string.cd_assistant_button)

    Surface(
        onClick = onClick,
        modifier = modifier
            .size(DoadoDimens.AssistantButtonSize)
            .semantics { contentDescription = label },
        shape = CircleShape,
        color = if (pressed) colors.surfaceContainerHigh else colors.primary,
        border = BorderStroke(DoadoDimens.BorderWidth, colors.primary),
        interactionSource = interaction,
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                painter = painterResource(R.drawable.ic_assistant),
                contentDescription = null,
                modifier = Modifier.size(52.dp),
                tint = if (pressed) colors.onSurface else colors.onPrimary,
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun AssistantButtonPreview() {
    DoadoTheme(darkTheme = true) {
        AssistantButton(onClick = {})
    }
}
