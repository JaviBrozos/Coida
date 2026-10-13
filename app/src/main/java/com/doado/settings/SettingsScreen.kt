package com.doado.settings

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import com.doado.R
import com.doado.core.ui.SectionLabel
import com.doado.core.ui.SettingsColumn
import com.doado.core.ui.SettingsRow
import com.doado.core.ui.ScreenHeader
import com.doado.data.AssistantMode
import com.doado.data.AssistantSettings

/**
 * Índice de Ajustes. Está escrito para el familiar o cuidador, no para quien usa
 * el móvil a diario: cada apartado lleva debajo una frase que dice qué hay dentro.
 */
@Composable
fun SettingsScreen(
    contactsOnHome: Int,
    adminActive: Boolean,
    assistant: AssistantSettings,
    onBack: () -> Unit,
    onInterface: () -> Unit,
    onContacts: () -> Unit,
    onApps: () -> Unit,
    onAssistant: () -> Unit,
    onSoundAndDisplay: () -> Unit,
    onAdmin: () -> Unit,
    onChangePin: () -> Unit,
) {
    SettingsColumn(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
    ) {
        ScreenHeader(
            title = stringResource(R.string.settings_title),
            subtitle = stringResource(R.string.settings_subtitle),
            backLabel = stringResource(R.string.cd_back_home),
            onBack = onBack,
        )

        SectionLabel(stringResource(R.string.settings_section_personalize))
        SettingsRow(
            icon = R.drawable.ic_settings,
            title = stringResource(R.string.settings_interface),
            subtitle = stringResource(R.string.settings_interface_subtitle),
            onClick = onInterface,
        )
        SettingsRow(
            icon = R.drawable.ic_contacts,
            title = stringResource(R.string.settings_contacts),
            subtitle = pluralStringResource(R.plurals.settings_contacts_subtitle, contactsOnHome, contactsOnHome),
            onClick = onContacts,
        )
        SettingsRow(
            icon = R.drawable.ic_apps,
            title = stringResource(R.string.settings_apps),
            subtitle = stringResource(R.string.settings_apps_subtitle),
            onClick = onApps,
        )
        SettingsRow(
            icon = R.drawable.ic_assistant,
            title = stringResource(R.string.settings_assistant),
            subtitle = stringResource(
                when {
                    !assistant.enabled -> R.string.settings_assistant_subtitle_off
                    assistant.mode == AssistantMode.ACT -> R.string.settings_assistant_subtitle_act
                    else -> R.string.settings_assistant_subtitle_guide
                },
            ),
            onClick = onAssistant,
        )
        SettingsRow(
            icon = R.drawable.ic_volume,
            title = stringResource(R.string.settings_sound),
            subtitle = stringResource(R.string.settings_sound_subtitle),
            onClick = onSoundAndDisplay,
        )

        SectionLabel(stringResource(R.string.settings_section_advanced))
        SettingsRow(
            icon = R.drawable.ic_shield,
            title = stringResource(R.string.settings_admin),
            subtitle = stringResource(
                if (adminActive) R.string.settings_admin_subtitle_active
                else R.string.settings_admin_subtitle,
            ),
            onClick = onAdmin,
            highlighted = adminActive,
        )
        SettingsRow(
            icon = R.drawable.ic_lock,
            title = stringResource(R.string.settings_pin),
            subtitle = stringResource(R.string.settings_pin_subtitle),
            onClick = onChangePin,
        )
    }
}
