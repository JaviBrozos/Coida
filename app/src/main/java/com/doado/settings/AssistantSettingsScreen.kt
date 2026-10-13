package com.doado.settings

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.doado.R
import com.doado.core.ui.ChoiceGroup
import com.doado.core.ui.DoadoTheme
import com.doado.core.ui.NoteCard
import com.doado.core.ui.OutlineButton
import com.doado.core.ui.ScreenHeader
import com.doado.core.ui.SectionLabel
import com.doado.core.ui.SettingsColumn
import com.doado.core.ui.SwitchRow
import com.doado.data.AssistantMode
import com.doado.data.AssistantSettings

/**
 * Ajustes → Asistente de voz (CU-15). Lo lee el familiar.
 *
 * Antes del interruptor va la nota de a dónde va la voz: encenderlo después de
 * leerla es aceptarlo (RNF-PRI-02). El permiso del micrófono no se pide aquí sino
 * en la Activity, al encender, para que la pantalla no conozca Android.
 *
 * @param configured false si esta copia de la app no tiene clave del servicio:
 *   entonces no hay nada que encender y solo se explica por qué.
 * @param micDenied el familiar acaba de negar el micrófono.
 */
@Composable
fun AssistantSettingsScreen(
    settings: AssistantSettings,
    configured: Boolean,
    micDenied: Boolean,
    onEnabledChange: (Boolean) -> Unit,
    onModeChange: (AssistantMode) -> Unit,
    onTry: () -> Unit,
    onBack: () -> Unit,
) {
    SettingsColumn(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
    ) {
        ScreenHeader(
            title = stringResource(R.string.settings_assistant),
            subtitle = stringResource(R.string.assistant_settings_subtitle),
            backLabel = stringResource(R.string.cd_back_settings),
            onBack = onBack,
        )

        if (configured) {
            AssistantControls(settings, micDenied, onEnabledChange, onModeChange, onTry)
        } else {
            NoteCard(text = stringResource(R.string.assistant_settings_not_configured))
        }
    }
}

@Composable
private fun AssistantControls(
    settings: AssistantSettings,
    micDenied: Boolean,
    onEnabledChange: (Boolean) -> Unit,
    onModeChange: (AssistantMode) -> Unit,
    onTry: () -> Unit,
) {
    NoteCard(text = stringResource(R.string.assistant_settings_privacy))
    SwitchRow(
        title = stringResource(R.string.assistant_settings_switch),
        subtitle = stringResource(R.string.assistant_settings_switch_subtitle),
        checked = settings.enabled,
        onCheckedChange = onEnabledChange,
    )
    if (micDenied && !settings.enabled) {
        NoteCard(text = stringResource(R.string.assistant_settings_mic_denied))
    }

    SectionLabel(stringResource(R.string.assistant_settings_mode))
    ChoiceGroup(
        options = listOf(
            stringResource(R.string.assistant_mode_act),
            stringResource(R.string.assistant_mode_guide),
        ),
        selectedIndex = settings.mode.ordinal,
        onSelect = { onModeChange(AssistantMode.entries[it]) },
    )
    NoteCard(
        text = stringResource(
            when (settings.mode) {
                AssistantMode.ACT -> R.string.assistant_settings_act_note
                AssistantMode.GUIDE -> R.string.assistant_settings_guide_note
            },
        ),
    )

    OutlineButton(
        text = stringResource(R.string.assistant_settings_try),
        icon = R.drawable.ic_assistant,
        onClick = onTry,
        enabled = settings.enabled,
        disabledReason = stringResource(R.string.cd_assistant_try_disabled),
    )
    // El gris solo no explica nada a quien mira: el motivo, también escrito.
    if (!settings.enabled) NoteCard(text = stringResource(R.string.assistant_settings_try_disabled))
}

@Preview(showBackground = true, widthDp = 411, heightDp = 891)
@Composable
private fun AssistantSettingsPreview() {
    DoadoTheme(darkTheme = true) {
        AssistantSettingsScreen(
            settings = AssistantSettings(enabled = true, mode = AssistantMode.GUIDE),
            configured = true,
            micDenied = false,
            onEnabledChange = {},
            onModeChange = {},
            onTry = {},
            onBack = {},
        )
    }
}
