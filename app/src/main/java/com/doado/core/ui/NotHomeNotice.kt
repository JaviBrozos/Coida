package com.doado.core.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.doado.R

/** Las dos caras del aviso de que Doado no es la pantalla de inicio (D-48). */
enum class NotHomeNoticeStage {
    /** Qué pasa y un botón para ponerlo. */
    OFFER,

    /** Tras cerrarlo o decir que no: cómo ponerlo más adelante. */
    EXPLAIN,
}

/**
 * Aviso de que Doado aún no es la pantalla de inicio (D-48), arriba del inicio y
 * de Ajustes. En [NotHomeNoticeStage.OFFER] dice qué pasa y lleva el botón para
 * ponerlo. La equis es para quien solo está probando Doado: al cerrarlo, el aviso
 * pasa a [NotHomeNoticeStage.EXPLAIN] y dice dónde está la opción para más
 * adelante, con su propia equis. Nada desaparece solo.
 *
 * @param message lo que pasa, dicho para quien lo va a leer en esa pantalla.
 */
@Composable
fun NotHomeNotice(
    stage: NotHomeNoticeStage,
    message: String,
    onMakeHome: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    DoadoCard(onClick = null, highlighted = true, modifier = modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(start = 16.dp, top = 8.dp, end = 8.dp, bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(verticalAlignment = Alignment.Top) {
                Text(
                    text = if (stage == NotHomeNoticeStage.OFFER) message else stringResource(R.string.not_home_later),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier
                        .weight(1f)
                        .padding(top = 8.dp, end = 8.dp),
                )
                CloseButton(onClick = onClose)
            }
            if (stage == NotHomeNoticeStage.OFFER) {
                PrimaryButton(
                    text = stringResource(R.string.settings_make_home),
                    icon = R.drawable.ic_home,
                    onClick = onMakeHome,
                    modifier = Modifier.padding(end = 8.dp),
                )
            }
        }
    }
}

/** La equis del aviso: redonda y del tamaño del botón de Ajustes, no un iconito. */
@Composable
private fun CloseButton(onClick: () -> Unit) {
    val label = stringResource(R.string.cd_close_notice)
    DoadoCard(
        onClick = onClick,
        color = Color.Transparent,
        shape = CircleShape,
        modifier = Modifier
            .size(DoadoDimens.CornerButtonSize)
            .semantics { contentDescription = label },
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                painter = painterResource(R.drawable.ic_close),
                contentDescription = null,
                modifier = Modifier.size(28.dp),
                tint = MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}
