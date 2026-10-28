package com.doado.settings

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import com.doado.R
import com.doado.core.ui.GroupPosition
import com.doado.core.ui.NotHomeNotice
import com.doado.core.ui.NotHomeNoticeStage
import com.doado.core.ui.SectionLabel
import com.doado.core.ui.SettingsColumn
import com.doado.core.ui.SettingsGroup
import com.doado.core.ui.SettingsRow
import com.doado.core.ui.ScreenHeader
import com.doado.data.AssistantMode
import com.doado.data.AssistantSettings

/**
 * Índice de Ajustes. Está escrito para el familiar o cuidador, no para quien usa
 * el móvil a diario: cada apartado lleva debajo una frase que dice qué hay dentro.
 *
 * @param notHomeNotice cómo va el aviso de que Doado no es la pantalla de inicio
 *   (D-48); null si lo es o si ya se cerró en esta visita.
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
    onArrange: () -> Unit,
    onAssistant: () -> Unit,
    onSoundAndDisplay: () -> Unit,
    onAdmin: () -> Unit,
    onChangePin: () -> Unit,
    notHomeNotice: NotHomeNoticeStage? = null,
    onMakeHome: () -> Unit = {},
    onCloseNotHome: () -> Unit = {},
) {
    SettingsColumn(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
    ) {
        ScreenHeader(
            title = stringResource(R.string.settings_title),
            backLabel = stringResource(R.string.cd_back_home),
            onBack = onBack,
        )

        // Sin ser la pantalla de inicio, al pulsar Inicio sale otra y nada de esto
        // sirve: va lo primero (D-48). El botón lleva al diálogo de Android que
        // ofrece Doado, que no saca a ninguna otra parte del móvil. Vuelve a salir
        // cada vez que se entra, aunque se cerrara en el inicio.
        if (notHomeNotice != null) {
            NotHomeNotice(
                stage = notHomeNotice,
                message = stringResource(R.string.settings_not_home_note),
                onMakeHome = onMakeHome,
                onClose = onCloseNotHome,
            )
        }

        SectionLabel(stringResource(R.string.settings_section_personalize))
        SettingsGroup {
            SettingsRow(
                icon = R.drawable.ic_sliders,
                title = stringResource(R.string.settings_interface),
                subtitle = stringResource(R.string.settings_interface_subtitle),
                onClick = onInterface,
                position = GroupPosition.First,
            )
            SettingsRow(
                icon = R.drawable.ic_contacts,
                title = stringResource(R.string.settings_contacts),
                subtitle = pluralStringResource(R.plurals.settings_contacts_subtitle, contactsOnHome, contactsOnHome),
                onClick = onContacts,
                position = GroupPosition.Middle,
            )
            SettingsRow(
                icon = R.drawable.ic_apps,
                title = stringResource(R.string.settings_apps),
                subtitle = stringResource(R.string.settings_apps_subtitle),
                onClick = onApps,
                position = GroupPosition.Middle,
            )
            SettingsRow(
                icon = R.drawable.ic_home,
                title = stringResource(R.string.settings_arrange),
                subtitle = stringResource(R.string.settings_arrange_subtitle),
                onClick = onArrange,
                position = GroupPosition.Middle,
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
                position = GroupPosition.Middle,
            )
            SettingsRow(
                icon = R.drawable.ic_volume,
                title = stringResource(R.string.settings_sound),
                subtitle = stringResource(R.string.settings_sound_subtitle),
                onClick = onSoundAndDisplay,
                position = GroupPosition.Last,
            )
        }

        SectionLabel(stringResource(R.string.settings_section_advanced))
        SettingsGroup {
            SettingsRow(
                icon = R.drawable.ic_shield,
                title = stringResource(R.string.settings_admin),
                subtitle = stringResource(
                    if (adminActive) R.string.settings_admin_subtitle_active
                    else R.string.settings_admin_subtitle,
                ),
                onClick = onAdmin,
                highlighted = adminActive,
                position = GroupPosition.First,
            )
            SettingsRow(
                icon = R.drawable.ic_lock,
                title = stringResource(R.string.settings_pin),
                subtitle = stringResource(R.string.settings_pin_subtitle),
                onClick = onChangePin,
                position = GroupPosition.Last,
            )
        }
    }
}
