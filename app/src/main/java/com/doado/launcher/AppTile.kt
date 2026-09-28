package com.doado.launcher

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.doado.R
import com.doado.core.ui.DoadoCard
import com.doado.core.ui.DoadoTheme
import com.doado.data.AppIcon
import com.doado.data.FavoriteApp

/**
 * Una casilla del grid de inicio: icono grande y nombre grande.
 *
 * Con dos columnas va en vertical (icono arriba, nombre debajo); con una columna
 * pasa a horizontal, que en un móvil estrecho aprovecha mejor el ancho.
 *
 * Decisiones de accesibilidad:
 *  · Toda la casilla es el área pulsable, muy por encima del mínimo de 48dp.
 *  · Se separa del fondo con un contorno en vez de con una sombra: una sombra
 *    tenue desaparece para quien tiene poca sensibilidad al contraste.
 *  · El icono es decorativo (contentDescription = null): quien use TalkBack ya oye
 *    el texto de la casilla, y anunciarlo dos veces solo estorba.
 *  · Si la app no está, la casilla se queda en su sitio, apagada y avisando.
 */
@Composable
fun AppTile(
    app: FavoriteApp,
    horizontal: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val contentColor =
        if (app.isAvailable) MaterialTheme.colorScheme.onSurface
        else MaterialTheme.colorScheme.outlineVariant
    val iconTint =
        if (app.isAvailable) MaterialTheme.colorScheme.primary
        else MaterialTheme.colorScheme.outlineVariant

    val openLabel = stringResource(R.string.cd_open_app, app.label)
    val unavailableLabel = stringResource(R.string.cd_app_unavailable, app.label)

    DoadoCard(
        onClick = onClick,
        enabled = app.isAvailable,
        modifier = modifier
            .fillMaxWidth()
            .semantics {
                if (app.isAvailable) {
                    onClick(label = openLabel) { onClick(); true }
                } else {
                    contentDescription = unavailableLabel
                }
            },
        shape = androidx.compose.foundation.shape.RoundedCornerShape(
            com.doado.core.ui.DoadoDimens.TileCornerRadius,
        ),
    ) {
        if (horizontal) {
            Row(
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(20.dp),
            ) {
                TileIcon(app, iconTint, 60.dp)
                TileLabels(app, contentColor, TextAlign.Start, Modifier.weight(1f))
            }
        } else {
            Column(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp, Alignment.CenterVertically),
            ) {
                TileIcon(app, iconTint, 80.dp)
                TileLabels(app, contentColor, TextAlign.Center, Modifier.fillMaxWidth())
            }
        }
    }
}

@Composable
private fun TileIcon(app: FavoriteApp, tint: Color, size: Dp) {
    when (val icon = app.icon) {
        is AppIcon.Vector -> Icon(
            painter = painterResource(icon.res),
            contentDescription = null,
            modifier = Modifier.size(size),
            tint = tint,
        )

        // El icono real de la app, algo más pequeño: ya trae su propio margen.
        is AppIcon.Bitmap -> Image(
            bitmap = icon.image,
            contentDescription = null,
            modifier = Modifier.size(size * 0.9f),
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
        Text(
            text = app.label,
            style = MaterialTheme.typography.labelLarge,
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
