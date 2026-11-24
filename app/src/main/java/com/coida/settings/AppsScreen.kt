package com.coida.settings

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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.dp
import com.coida.R
import com.coida.core.ui.CoidaCard
import com.coida.core.ui.CoidaDimens
import com.coida.core.ui.GroupPosition
import com.coida.core.ui.IconBadge
import com.coida.core.ui.InitialBadge
import com.coida.core.ui.NoteCard
import com.coida.core.ui.OutlineButton
import com.coida.core.ui.PrimaryButton
import com.coida.core.ui.ScreenHeader
import com.coida.core.ui.SectionLabel
import com.coida.core.ui.SwitchRow
import com.coida.data.InstalledApp

/** Una de las casillas básicas de fábrica, con su interruptor. */
data class BasicAppRow(
    val id: String,
    val label: String,
    val icon: Int,
    val onHome: Boolean,
    /** Qué hace, cuando no es obvio por el nombre (Emergencias, D-51). */
    val note: String? = null,
)

/**
 * El interruptor de la bola roja de llamadas perdidas (D-66).
 *
 * @param enabled encendido y con el permiso concedido.
 * @param denied el familiar denegó el permiso: debajo se explica qué hacer.
 */
data class MissedCallsRow(
    val enabled: Boolean,
    val denied: Boolean,
    val onToggle: (Boolean) -> Unit,
)

/** Cualquier otra app instalada que se puede añadir al inicio. */
data class OtherAppRow(val app: InstalledApp, val onHome: Boolean)

/**
 * Ajustes → Aplicaciones: qué sale en el inicio.
 *
 * Dos bloques: las básicas de fábrica y el resto de apps del móvil. Cualquier app
 * que tenga icono en Android se puede poner en el inicio. Coida no propone apps
 * para instalar (D-33): se instalan desde la tienda, en el modo administrador.
 */
@Composable
fun AppsScreen(
    basics: List<BasicAppRow>,
    others: List<OtherAppRow>,
    onToggle: (id: String, onHome: Boolean) -> Unit,
    onBack: () -> Unit,
    onArrange: () -> Unit = {},
    missedCalls: MissedCallsRow? = null,
) {
    // Sin hueco general entre elementos: las filas de un mismo apartado van
    // pegadas en un grupo, y el aire se pone a mano entre apartados.
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(CoidaDimens.ScreenPadding),
    ) {
        item {
            ScreenHeader(
                title = stringResource(R.string.settings_apps),
                subtitle = stringResource(R.string.apps_subtitle),
                backLabel = stringResource(R.string.cd_back_settings),
                onBack = onBack,
            )
        }

        // Las que se encienden van al final del inicio: justo aquí se pueden colocar
        // en su sitio (D-59).
        item {
            OutlineButton(
                text = stringResource(R.string.apps_arrange),
                icon = R.drawable.ic_home,
                onClick = onArrange,
                modifier = Modifier.padding(top = 14.dp),
            )
        }

        item { SectionLabel(stringResource(R.string.apps_section_basic), Modifier.padding(top = 14.dp, bottom = 10.dp)) }
        itemsIndexed(basics, key = { _, row -> "basic-" + row.id }) { index, row ->
            SwitchRow(
                title = row.label,
                subtitle = row.note,
                checked = row.onHome,
                onCheckedChange = { onToggle(row.id, it) },
                leading = { IconBadge(row.icon) },
                position = GroupPosition.of(index, basics.size),
            )
        }

        // Solo en las variantes que traen el permiso (D-66). El permiso se pide al
        // encenderlo, aquí, con el familiar delante: la persona mayor nunca lo ve.
        if (missedCalls != null) {
            item { SectionLabel(stringResource(R.string.apps_section_missed), Modifier.padding(top = 14.dp, bottom = 10.dp)) }
            item(key = "missed-calls") {
                SwitchRow(
                    title = stringResource(R.string.apps_missed_title),
                    subtitle = stringResource(
                        if (missedCalls.denied) R.string.apps_missed_denied else R.string.apps_missed_note,
                    ),
                    checked = missedCalls.enabled,
                    onCheckedChange = missedCalls.onToggle,
                    leading = { IconBadge(R.drawable.ic_call) },
                    position = GroupPosition.of(0, 1),
                )
            }
        }

        if (others.isNotEmpty()) {
            item { SectionLabel(stringResource(R.string.apps_section_others), Modifier.padding(top = 14.dp, bottom = 10.dp)) }
            itemsIndexed(others, key = { _, row -> "other-" + row.app.packageName }) { index, row ->
                SwitchRow(
                    title = row.app.label,
                    checked = row.onHome,
                    onCheckedChange = { onToggle(com.coida.data.FavoritesStore.packageId(row.app.packageName), it) },
                    leading = { InitialBadge(row.app.label) },
                    position = GroupPosition.of(index, others.size),
                )
            }
        }

        item {
            NoteCard(
                text = stringResource(R.string.apps_note),
                icon = R.drawable.ic_shield,
                modifier = Modifier.padding(top = 14.dp),
            )
        }
    }
}

