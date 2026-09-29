package com.doado.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.dp
import com.doado.R
import com.doado.core.ui.DoadoCard
import com.doado.core.ui.DoadoDimens
import com.doado.core.ui.IconBadge
import com.doado.core.ui.NoteCard
import com.doado.core.ui.OutlineButton
import com.doado.core.ui.PrimaryButton
import com.doado.core.ui.ScreenHeader
import com.doado.core.ui.SectionLabel
import com.doado.core.ui.SwitchRow
import com.doado.data.InstalledApp
import com.doado.data.SuggestedApp

/** Una de las casillas básicas de fábrica, con su interruptor. */
data class BasicAppRow(val id: String, val label: String, val icon: Int, val onHome: Boolean)

/** Una app recomendada y en qué punto está en este móvil. */
data class SuggestedAppRow(val app: SuggestedApp, val installed: Boolean, val onHome: Boolean)

/** Cualquier otra app instalada que se puede añadir al inicio. */
data class OtherAppRow(val app: InstalledApp, val onHome: Boolean)

/**
 * Ajustes → Aplicaciones: qué sale en el inicio y qué se puede instalar.
 *
 * Tres bloques: las básicas de fábrica, las recomendadas (con botón de instalar)
 * y el resto de apps del móvil. Cualquier app que tenga icono en Android se puede
 * poner en el inicio; así una app que se instale por otra vía también cabe.
 */
@Composable
fun AppsScreen(
    basics: List<BasicAppRow>,
    suggested: List<SuggestedAppRow>,
    others: List<OtherAppRow>,
    adminActive: Boolean,
    onToggle: (id: String, onHome: Boolean) -> Unit,
    onInstall: (SuggestedApp) -> Unit,
    onBack: () -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(DoadoDimens.ScreenPadding),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item {
            ScreenHeader(
                title = stringResource(R.string.settings_apps),
                subtitle = stringResource(R.string.apps_subtitle),
                backLabel = stringResource(R.string.cd_back_settings),
                onBack = onBack,
            )
        }

        item { SectionLabel(stringResource(R.string.apps_section_basic)) }
        items(basics, key = { "basic-" + it.id }) { row ->
            SwitchRow(
                title = row.label,
                checked = row.onHome,
                onCheckedChange = { onToggle(row.id, it) },
                leading = { IconBadge(row.icon) },
            )
        }

        item { SectionLabel(stringResource(R.string.apps_section_suggested)) }
        items(suggested, key = { "suggested-" + it.app.packageName }) { row ->
            if (row.installed) {
                SwitchRow(
                    title = row.app.label,
                    subtitle = stringResource(row.app.descriptionRes),
                    checked = row.onHome,
                    onCheckedChange = { onToggle(com.doado.data.FavoritesStore.packageId(row.app.packageName), it) },
                    leading = { LetterBadge(row.app.label) },
                )
            } else {
                SuggestedToInstall(row.app, adminActive, onInstall = { onInstall(row.app) })
            }
        }

        if (others.isNotEmpty()) {
            item { SectionLabel(stringResource(R.string.apps_section_others)) }
            items(others, key = { "other-" + it.app.packageName }) { row ->
                SwitchRow(
                    title = row.app.label,
                    checked = row.onHome,
                    onCheckedChange = { onToggle(com.doado.data.FavoritesStore.packageId(row.app.packageName), it) },
                    leading = { LetterBadge(row.app.label) },
                )
            }
        }

        item {
            NoteCard(
                text = stringResource(R.string.apps_note),
                icon = R.drawable.ic_shield,
            )
        }
    }
}

@Composable
private fun SuggestedToInstall(app: SuggestedApp, adminActive: Boolean, onInstall: () -> Unit) {
    DoadoCard(onClick = null, modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                LetterBadge(app.label)
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = app.label,
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        text = stringResource(app.descriptionRes),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            // Sin modo administrador no se puede instalar nada, así que el botón
            // no aparenta que sí: cambia de aspecto y dice a dónde lleva de verdad.
            if (adminActive) {
                PrimaryButton(
                    text = stringResource(R.string.apps_install),
                    icon = R.drawable.ic_store,
                    onClick = onInstall,
                )
            } else {
                OutlineButton(
                    text = stringResource(R.string.apps_install_needs_admin),
                    icon = R.drawable.ic_shield,
                    onClick = onInstall,
                )
            }
        }
    }
}

/**
 * Inicial de la app en un cuadrito. Evita cargar decenas de iconos en la lista
 * y no imita el logotipo de nadie.
 */
@Composable
private fun LetterBadge(label: String) {
    Box(
        modifier = Modifier
            // La inicial es decoración. Sin esto, como la fila entera es un solo
            // nodo pulsable, TalkBack la arrastra al principio y lee
            // "W, WhatsApp, Mensajes y videollamadas, interruptor".
            .clearAndSetSemantics {}
            .size(48.dp)
            .background(
                MaterialTheme.colorScheme.surfaceContainerHigh,
                RoundedCornerShape(DoadoDimens.ControlCornerRadius),
            ),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label.take(1).uppercase(),
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.primary,
        )
    }
}
