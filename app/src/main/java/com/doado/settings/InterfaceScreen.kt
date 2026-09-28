package com.doado.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.doado.R
import com.doado.core.ui.ChoiceGroup
import com.doado.core.ui.DoadoCard
import com.doado.core.ui.NoteCard
import com.doado.core.ui.ScreenHeader
import com.doado.core.ui.SectionLabel
import com.doado.core.ui.SettingsColumn
import com.doado.core.ui.SwitchRow
import com.doado.data.InterfaceSettings
import com.doado.data.TextSize
import com.doado.data.ThemeMode

/**
 * Ajustes → Interfaz. Cada cambio se guarda y se aplica en el acto, a toda la
 * app: quien configura ve el resultado mientras toca, sin botón de "guardar".
 */
@Composable
fun InterfaceScreen(
    settings: InterfaceSettings,
    onChange: (InterfaceSettings) -> Unit,
    onBack: () -> Unit,
) {
    SettingsColumn(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
    ) {
        ScreenHeader(
            title = stringResource(R.string.settings_interface),
            subtitle = stringResource(R.string.interface_subtitle),
            backLabel = stringResource(R.string.cd_back_settings),
            onBack = onBack,
        )

        SectionLabel(stringResource(R.string.interface_text_size))
        ChoiceGroup(
            options = listOf(
                stringResource(R.string.interface_text_normal),
                stringResource(R.string.interface_text_large),
                stringResource(R.string.interface_text_extra_large),
            ),
            selectedIndex = settings.textSize.ordinal,
            onSelect = { onChange(settings.copy(textSize = TextSize.entries[it])) },
        )

        SectionLabel(stringResource(R.string.interface_columns))
        ChoiceGroup(
            options = listOf(
                stringResource(R.string.interface_columns_one),
                stringResource(R.string.interface_columns_two),
            ),
            selectedIndex = settings.columns - 1,
            onSelect = { onChange(settings.copy(columns = it + 1)) },
        )
        ColumnsPreview(columns = settings.columns)

        SectionLabel(stringResource(R.string.interface_theme))
        ChoiceGroup(
            options = listOf(
                stringResource(R.string.interface_theme_dark),
                stringResource(R.string.interface_theme_light),
            ),
            selectedIndex = settings.theme.ordinal,
            onSelect = { onChange(settings.copy(theme = ThemeMode.entries[it])) },
        )

        SwitchRow(
            title = stringResource(R.string.interface_clock),
            subtitle = stringResource(R.string.interface_clock_subtitle),
            checked = settings.showClock,
            onCheckedChange = { onChange(settings.copy(showClock = it)) },
            modifier = Modifier.padding(top = 8.dp),
        )

        NoteCard(text = stringResource(R.string.interface_note))
    }
}

/**
 * Muestra en pequeño cómo quedan las casillas. El tamaño de letra y el tema no
 * necesitan maqueta: se aplican ya a esta misma pantalla.
 */
@Composable
private fun ColumnsPreview(columns: Int) {
    val samples = listOf(
        R.drawable.ic_call to R.string.app_phone,
        R.drawable.ic_messages to R.string.app_messages,
    )
    DoadoCard(onClick = null, modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            if (columns == 1) {
                samples.forEach { (icon, label) -> MiniTile(icon, label, horizontal = true, Modifier.fillMaxWidth()) }
            } else {
                androidx.compose.foundation.layout.Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    samples.forEach { (icon, label) ->
                        MiniTile(icon, label, horizontal = false, Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

@Composable
private fun MiniTile(icon: Int, label: Int, horizontal: Boolean, modifier: Modifier) {
    DoadoCard(
        onClick = null,
        modifier = modifier.heightIn(min = 72.dp),
        color = MaterialTheme.colorScheme.background,
    ) {
        val content: @Composable () -> Unit = {
            Icon(
                painter = painterResource(icon),
                contentDescription = null,
                modifier = Modifier.size(32.dp),
                tint = MaterialTheme.colorScheme.primary,
            )
            Text(
                text = stringResource(label),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        if (horizontal) {
            androidx.compose.foundation.layout.Row(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp),
            ) { content() }
        } else {
            Column(
                modifier = Modifier.padding(10.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterVertically),
            ) { content() }
        }
    }
}
