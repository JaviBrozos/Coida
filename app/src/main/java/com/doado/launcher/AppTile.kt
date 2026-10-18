package com.doado.launcher

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.style.Hyphens
import androidx.compose.ui.text.style.LineBreak
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.doado.R
import com.doado.core.ui.DoadoCard
import com.doado.core.ui.DoadoDimens
import com.doado.core.ui.DoadoTheme
import com.doado.data.AppIcon
import com.doado.data.FavoriteApp

/**
 * Una casilla del grid de inicio: icono grande y nombre grande.
 *
 * Con dos columnas va en vertical (icono arriba, nombre debajo); con una columna
 * pasa a horizontal, que en un móvil estrecho aprovecha mejor el ancho.
 *
 * El icono va directamente sobre la casilla, sin plato (D-35): solo su silueta
 * dentro del contorno exterior. El cian se lee a 6,34:1 sobre la casilla y a
 * 5,27:1 sobre el fondo de pulsado, por encima del 3:1 que piden los gráficos.
 *
 * Decisiones de accesibilidad:
 *  · Toda la casilla es el área pulsable, muy por encima del mínimo de 48dp.
 *  · Se separa del fondo con un contorno en vez de con una sombra: una sombra
 *    tenue desaparece para quien tiene poca sensibilidad al contraste.
 *  · El icono es decorativo (contentDescription = null): quien use TalkBack ya oye
 *    el texto de la casilla, y anunciarlo dos veces solo estorba.
 *  · Si la app no está, la casilla se queda en su sitio, apagada y avisando.
 *  · [highlighted] la señala el asistente al guiar: rellena de acento y con borde
 *    grueso, sin moverla ni agrandarla, y TalkBack lo dice. El icono pasa al
 *    color que va encima del acento.
 */
@Composable
fun AppTile(
    app: FavoriteApp,
    horizontal: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    highlighted: Boolean = false,
) {
    val colors = MaterialTheme.colorScheme
    // Señalada: rellena de acento. Así se distingue también en gris y en el tema
    // claro, donde el contorno normal ya es del mismo marino que el acento.
    val contentColor = when {
        !app.isAvailable -> colors.outlineVariant
        highlighted -> colors.onPrimary
        else -> colors.onSurface
    }
    val iconTint = when {
        !app.isAvailable -> colors.outlineVariant
        highlighted -> colors.onPrimary
        else -> colors.primary
    }

    val openLabel = stringResource(R.string.cd_open_app, app.label)
    val unavailableLabel = stringResource(R.string.cd_app_unavailable, app.label)
    val highlightedLabel = stringResource(R.string.cd_highlighted)

    DoadoCard(
        onClick = onClick,
        enabled = app.isAvailable,
        highlighted = highlighted,
        color = if (highlighted) colors.primary else colors.surfaceContainer,
        borderWidth = if (highlighted) DoadoDimens.HighlightBorderWidth else DoadoDimens.BorderWidth,
        modifier = modifier
            .fillMaxWidth()
            .semantics {
                if (app.isAvailable) {
                    onClick(label = openLabel) { onClick(); true }
                } else {
                    contentDescription = unavailableLabel
                }
                if (highlighted) stateDescription = highlightedLabel
            },
        shape = RoundedCornerShape(DoadoDimens.TileCornerRadius),
    ) {
        if (horizontal) {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(20.dp),
            ) {
                TileIcon(app, iconTint, DoadoDimens.TileIconSizeCompact)
                TileLabels(app, contentColor, TextAlign.Start, Modifier.weight(1f))
            }
        } else {
            Column(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp, Alignment.CenterVertically),
            ) {
                TileIcon(app, iconTint, DoadoDimens.TileIconSize)
                TileLabels(app, contentColor, TextAlign.Center, Modifier.fillMaxWidth())
            }
        }
    }
}

@Composable
private fun TileIcon(app: FavoriteApp, tint: Color, iconSize: Dp) {
    when (val icon = app.icon) {
        is AppIcon.Vector -> Icon(
            painter = painterResource(icon.res),
            contentDescription = null,
            modifier = Modifier.size(iconSize),
            tint = tint,
        )

        is AppIcon.Bitmap -> Image(
            bitmap = icon.image,
            contentDescription = null,
            modifier = Modifier.size(iconSize),
            alpha = if (app.isAvailable) 1f else 0.35f,
        )
    }
}

@Composable
private fun TileLabels(
    app: FavoriteApp,
    color: Color,
    align: TextAlign,
    modifier: Modifier,
) {
    Column(
        modifier = modifier,
        horizontalAlignment = if (align == TextAlign.Center) Alignment.CenterHorizontally else Alignment.Start,
    ) {
        // Con la letra al máximo «Mensajes» no cabe en media pantalla y se partía
        // en «Mensaje/s»: con guion por sílabas sale «Men-sajes», que se lee.
        Text(
            text = app.label,
            style = MaterialTheme.typography.labelLarge.copy(
                hyphens = Hyphens.Auto,
                lineBreak = LineBreak.Paragraph,
            ),
            color = color,
            textAlign = align,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        if (!app.isAvailable) {
            Text(
                text = stringResource(R.string.app_unavailable),
                style = MaterialTheme.typography.labelSmall,
                color = color,
                textAlign = align,
                modifier = Modifier.padding(top = 4.dp),
            )
        }
    }
}

@Preview(showBackground = true, widthDp = 200, heightDp = 240)
@Composable
private fun AppTilePreview() {
    DoadoTheme(darkTheme = true) {
        AppTile(
            app = FavoriteApp("phone", "Llamar", AppIcon.Vector(R.drawable.ic_call), "com.android.dialer"),
            horizontal = false,
            onClick = {},
            modifier = Modifier.width(200.dp),
        )
    }
}

@Preview(showBackground = true, widthDp = 200, heightDp = 240)
@Composable
private fun AppTileUnavailablePreview() {
    DoadoTheme(darkTheme = true) {
        AppTile(
            app = FavoriteApp("camera", "Cámara", AppIcon.Vector(R.drawable.ic_camera), null),
            horizontal = false,
            onClick = {},
        )
    }
}
