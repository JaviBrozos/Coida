package com.doado.launcher

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.doado.R
import com.doado.core.ui.DoadoDimens
import com.doado.core.ui.DoadoTheme
import com.doado.data.FavoriteApp

/**
 * Una casilla del grid de inicio: icono grande arriba, nombre grande debajo.
 *
 * Decisiones de accesibilidad:
 *  · Toda la casilla es el área pulsable (>= 168dp de alto), muy por encima del
 *    mínimo de 48dp, para que no haga falta puntería.
 *  · Se separa del fondo con un borde macizo en vez de con una sombra: una sombra
 *    tenue desaparece para quien tiene poca sensibilidad al contraste.
 *  · El icono es decorativo (contentDescription = null): quien use TalkBack ya oye
 *    el texto de la casilla, y anunciarlo dos veces solo estorba.
 */
@Composable
fun AppTile(
    app: FavoriteApp,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(DoadoDimens.TileCornerRadius)
    val contentColor =
        if (app.isAvailable) MaterialTheme.colorScheme.onSurface
        else MaterialTheme.colorScheme.outlineVariant

    val openLabel = stringResource(R.string.cd_open_app, app.label)
    val unavailableLabel = stringResource(R.string.cd_app_unavailable, app.label)

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = DoadoDimens.TileMinHeight)
            .clip(shape)
            .clickable(
                enabled = app.isAvailable,
                onClickLabel = openLabel,
                role = Role.Button,
                onClick = onClick,
            )
            .semantics {
                if (!app.isAvailable) contentDescription = unavailableLabel
            },
        shape = shape,
        color = MaterialTheme.colorScheme.surface,
        contentColor = contentColor,
        border = BorderStroke(DoadoDimens.TileBorderWidth, contentColor),
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Icon(
                painter = painterResource(app.iconRes),
                contentDescription = null,
                modifier = Modifier.size(DoadoDimens.TileIconSize),
                tint = contentColor,
            )
            Text(
                text = app.label,
                modifier = Modifier.padding(top = 16.dp),
                style = MaterialTheme.typography.labelLarge,
                textAlign = TextAlign.Center,
                maxLines = 2,
            )
            if (!app.isAvailable) {
                Text(
                    text = stringResource(R.string.app_unavailable),
                    modifier = Modifier.padding(top = 4.dp),
                    style = MaterialTheme.typography.labelSmall,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}

@Preview(showBackground = true, widthDp = 220)
@Composable
private fun AppTilePreview() {
    DoadoTheme {
        AppTile(
            app = FavoriteApp("phone", "Llamar", R.drawable.ic_call, packageName = "com.android.dialer"),
            onClick = {},
        )
    }
}

@Preview(showBackground = true, widthDp = 220)
@Composable
private fun AppTileUnavailablePreview() {
    DoadoTheme {
        AppTile(
            app = FavoriteApp("camera", "Cámara", R.drawable.ic_camera, packageName = null),
            onClick = {},
        )
    }
}
