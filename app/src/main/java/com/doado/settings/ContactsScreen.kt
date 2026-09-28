package com.doado.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.doado.R
import com.doado.core.ui.ChoiceGroup
import com.doado.core.ui.DoadoCard
import com.doado.core.ui.DoadoSwitchVisual
import com.doado.core.ui.IconBadge
import com.doado.core.ui.NoteCard
import com.doado.core.ui.OutlineButton
import com.doado.core.ui.PrimaryButton
import com.doado.core.ui.ScreenHeader
import com.doado.core.ui.SettingsColumn
import com.doado.data.ContactAction
import com.doado.data.ContactShortcut
import com.doado.data.ContactShortcutsStore

/**
 * Ajustes → Contactos: los accesos directos que salen arriba del inicio.
 * Por cada persona se elige qué hace la pulsación y si sale o no en el inicio.
 */
@Composable
fun ContactsScreen(
    contacts: List<ContactShortcut>,
    whatsappInstalled: Boolean,
    onAdd: () -> Unit,
    onUpdate: (ContactShortcut) -> Unit,
    onRemove: (ContactShortcut) -> Unit,
    onBack: () -> Unit,
) {
    val onHomeCount = contacts.count { it.onHome }

    SettingsColumn(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
    ) {
        ScreenHeader(
            title = stringResource(R.string.settings_contacts),
            subtitle = stringResource(R.string.contacts_subtitle, ContactShortcutsStore.MAX_ON_HOME),
            backLabel = stringResource(R.string.cd_back_settings),
            onBack = onBack,
        )

        if (contacts.isEmpty()) {
            NoteCard(text = stringResource(R.string.contacts_empty))
        }

        contacts.forEach { contact ->
            ContactCard(
                contact = contact,
                // Se puede quitar siempre; poner solo si queda hueco.
                canPutOnHome = contact.onHome || onHomeCount < ContactShortcutsStore.MAX_ON_HOME,
                onUpdate = onUpdate,
                onRemove = { onRemove(contact) },
            )
        }

        if (contacts.any { it.action == ContactAction.WHATSAPP } && !whatsappInstalled) {
            NoteCard(text = stringResource(R.string.contacts_whatsapp_missing))
        }

        PrimaryButton(
            text = stringResource(R.string.contacts_add),
            icon = R.drawable.ic_add,
            onClick = onAdd,
            modifier = Modifier.padding(top = 8.dp),
        )
    }
}

@Composable
private fun ContactCard(
    contact: ContactShortcut,
    canPutOnHome: Boolean,
    onUpdate: (ContactShortcut) -> Unit,
    onRemove: () -> Unit,
) {
    DoadoCard(onClick = null, modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                IconBadge(icon = R.drawable.ic_contacts, size = 56.dp)
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = contact.name,
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        text = contact.number,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 56.dp)
                    .toggleable(
                        value = contact.onHome,
                        enabled = canPutOnHome,
                        role = Role.Switch,
                        onValueChange = { onUpdate(contact.copy(onHome = it)) },
                    ),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(
                        if (canPutOnHome) R.string.contacts_on_home else R.string.contacts_home_full,
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (canPutOnHome) MaterialTheme.colorScheme.onSurface
                    else MaterialTheme.colorScheme.outlineVariant,
                    modifier = Modifier.weight(1f),
                )
                DoadoSwitchVisual(checked = contact.onHome)
            }

            ChoiceGroup(
                options = listOf(
                    stringResource(R.string.contact_action_call),
                    stringResource(R.string.contact_action_whatsapp),
                ),
                selectedIndex = contact.action.ordinal,
                onSelect = { onUpdate(contact.copy(action = ContactAction.entries[it])) },
            )

            OutlineButton(
                text = stringResource(R.string.contacts_remove),
                icon = R.drawable.ic_delete,
                onClick = onRemove,
            )
        }
    }
}
