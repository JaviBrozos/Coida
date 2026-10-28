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
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.doado.R
import com.doado.core.ui.DoadoCard
import com.doado.core.ui.DoadoSwitchVisual
import com.doado.core.ui.ContactAvatar
import com.doado.core.ui.DoadoDimens
import com.doado.core.ui.NoteCard
import com.doado.core.ui.OutlineButton
import com.doado.core.ui.PrimaryButton
import com.doado.core.ui.ScreenHeader
import com.doado.core.ui.SettingsColumn
import com.doado.core.ui.rememberClickFeedback
import com.doado.data.ContactAction
import com.doado.data.ContactShortcut

/**
 * Ajustes → Contactos: los accesos directos que salen en el inicio, sin límite (D-38).
 *
 * Una tarjeta por persona con dos interruptores, uno por botón: llamar y WhatsApp.
 * Se pueden encender los dos, y entonces la persona tiene dos casillas (D-40).
 * Por dentro cada casilla es su propio [ContactShortcut]; aquí se juntan los de un
 * mismo número. Encender uno nuevo lo añade al final, para no mover las casillas
 * que ya hay. Con los dos apagados la persona sigue guardada, para el asistente.
 *
 * @param photos la foto de la agenda de cada número que la tiene (D-57).
 * @param photosAllowed hay permiso para leerlas; si no, una nota con un botón que
 *   lo pide, con el familiar delante.
 */
@Composable
fun ContactsScreen(
    contacts: List<ContactShortcut>,
    whatsappInstalled: Boolean,
    onAdd: () -> Unit,
    onSetButton: (person: ContactShortcut, action: ContactAction, onHome: Boolean) -> Unit,
    onRemove: (person: ContactShortcut) -> Unit,
    onBack: () -> Unit,
    notice: String? = null,
    photos: Map<String, ImageBitmap> = emptyMap(),
    photosAllowed: Boolean = true,
    onAllowPhotos: () -> Unit = {},
) {
    SettingsColumn(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
    ) {
        ScreenHeader(
            title = stringResource(R.string.settings_contacts),
            subtitle = stringResource(R.string.contacts_subtitle),
            backLabel = stringResource(R.string.cd_back_settings),
            onBack = onBack,
        )

        // Aviso fijo, no un mensaje que se va solo: se quita al tocar otra cosa.
        notice?.let { NoteCard(text = it) }

        if (contacts.isEmpty()) {
            NoteCard(text = stringResource(R.string.contacts_empty))
        } else if (!photosAllowed) {
            NoteCard(text = stringResource(R.string.contacts_photos_note))
            OutlineButton(
                text = stringResource(R.string.contacts_photos_allow),
                icon = R.drawable.ic_contacts,
                onClick = onAllowPhotos,
            )
        }

        groupByPerson(contacts).forEach { buttons ->
            val person = buttons.first()
            PersonCard(
                person = person,
                photo = photos[person.number],
                callOn = buttons.any { it.action == ContactAction.CALL && it.onHome },
                whatsappOn = buttons.any { it.action == ContactAction.WHATSAPP && it.onHome },
                whatsappInstalled = whatsappInstalled,
                onSetButton = { action, on -> onSetButton(person, action, on) },
                onRemove = { onRemove(person) },
            )
        }

        if (contacts.any { it.action == ContactAction.WHATSAPP && it.onHome } && !whatsappInstalled) {
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

/** Junta los accesos directos de una misma persona, en el orden en que aparece cada una. */
private fun groupByPerson(contacts: List<ContactShortcut>): List<List<ContactShortcut>> {
    val people = mutableListOf<MutableList<ContactShortcut>>()
    contacts.forEach { contact ->
        val group = people.firstOrNull { it.first().isSamePersonAs(contact) }
        if (group != null) group += contact else people += mutableListOf(contact)
    }
    return people
}

@Composable
private fun PersonCard(
    person: ContactShortcut,
    photo: ImageBitmap?,
    callOn: Boolean,
    whatsappOn: Boolean,
    whatsappInstalled: Boolean,
    onSetButton: (ContactAction, Boolean) -> Unit,
    onRemove: () -> Unit,
) {
    // Con varias tarjetas, «Botón para llamar» o «Quitar» a secas no dicen de
    // quién son: TalkBack los lee con el nombre.
    val removeLabel = stringResource(R.string.cd_contacts_remove, person.name)
    val whatsappMissing = whatsappOn && !whatsappInstalled
    DoadoCard(onClick = null, modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Row(
                // Encabezado: con TalkBack se salta de una persona a la siguiente.
                modifier = Modifier.semantics(mergeDescendants = true) { heading() },
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                // La misma cara que en el inicio, foto o inicial: el familiar reconoce
                // el acceso directo.
                ContactAvatar(name = person.name, photo = photo, size = DoadoDimens.AvatarSize)
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = person.name,
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        text = person.number,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            ButtonSwitch(
                text = stringResource(R.string.contacts_button_call),
                label = stringResource(R.string.cd_contacts_button_call, person.name),
                checked = callOn,
                onCheckedChange = { onSetButton(ContactAction.CALL, it) },
            )
            ButtonSwitch(
                text = stringResource(R.string.contacts_button_whatsapp),
                label = stringResource(R.string.cd_contacts_button_whatsapp, person.name),
                // En la propia fila, no solo en la nota del final: con varias
                // personas, la nota quedaba lejos del interruptor al que se refiere.
                detail = if (whatsappMissing) stringResource(R.string.contacts_button_whatsapp_missing) else null,
                checked = whatsappOn,
                onCheckedChange = { onSetButton(ContactAction.WHATSAPP, it) },
            )

            OutlineButton(
                text = stringResource(R.string.contacts_remove),
                icon = R.drawable.ic_delete,
                onClick = onRemove,
                modifier = Modifier.semantics { contentDescription = removeLabel },
            )
        }
    }
}

/**
 * Fila entera pulsable con su interruptor, como [com.doado.core.ui.SwitchRow] pero
 * dentro de la tarjeta. [label] es lo que lee TalkBack, con el nombre de la persona;
 * [detail], una línea debajo que también lee.
 */
@Composable
private fun ButtonSwitch(
    text: String,
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    detail: String? = null,
) {
    val feedback = rememberClickFeedback()
    val description = if (detail == null) label else "$label. $detail"
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = DoadoDimens.ComfortableTouchTarget)
            .semantics { contentDescription = description }
            .toggleable(
                value = checked,
                role = Role.Switch,
                onValueChange = { feedback(); onCheckedChange(it) },
            ),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = text,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            if (detail != null) {
                Text(
                    text = detail,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        DoadoSwitchVisual(checked = checked)
    }
}
