package com.coida.assistant.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.paneTitle
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.coida.R
import com.coida.assistant.AssistantAction
import com.coida.assistant.AssistantFailure
import com.coida.assistant.AssistantState
import com.coida.assistant.ClarifyOption
import com.coida.assistant.messageRes
import com.coida.core.ui.CoidaDimens
import com.coida.core.ui.CoidaTheme
import com.coida.core.ui.FitText
import com.coida.core.ui.IconBadge
import com.coida.core.ui.NoteCard
import com.coida.core.ui.OutlineButton
import com.coida.core.ui.PrimaryButton
import com.coida.data.ContactAction
import com.coida.data.ContactShortcut

/**
 * Pantalla del asistente (CU-04, CU-05). La lee y la oye la persona mayor.
 *
 * Una sola idea cada vez, en letra grande, y como mucho un botón principal. Todo
 * lo que el móvil dice en voz alta está también escrito, con «Repetir».
 *
 * Solo escucha cuando lo pone (barra de volumen y «Ya he terminado»): al acabar de
 * hablar no vuelve a abrir el micrófono por su cuenta, salvo para pedir que le
 * repitan una orden que no ha entendido (D-56). Para decirle otra cosa está
 * «Hablar otra vez».
 *
 * Para TalkBack, que es quien habla cuando está encendido: la frase principal es
 * **una única** región viva que siempre está en el mismo sitio y solo cambia de
 * texto. Si cada estado pintara la suya, el nodo nacería nuevo y TalkBack no
 * siempre lo anunciaría. La columna lleva título de panel para que el foco no se
 * pierda al llegar desde el inicio.
 *
 * No conoce Android ni el controlador: recibe el estado y avisa de los toques.
 */
@Composable
fun AssistantScreen(
    state: AssistantState,
    level: Float,
    onSpeakAgain: () -> Unit,
    onDoneSpeaking: () -> Unit,
    onCancelAction: () -> Unit,
    onChoose: (ClarifyOption) -> Unit,
    onNext: () -> Unit,
    onRepeat: () -> Unit,
    onRetry: () -> Unit,
    onFinish: () -> Unit,
    onClose: () -> Unit,
) {
    val title = stringResource(R.string.settings_assistant)
    val message = when (state) {
        AssistantState.Idle -> ""
        AssistantState.Listening -> stringResource(R.string.assistant_listening_title)
        is AssistantState.Thinking -> stringResource(R.string.assistant_thinking)
        is AssistantState.Acting -> state.text
        is AssistantState.Guide -> state.steps[state.index]
        is AssistantState.Clarify -> state.question
        is AssistantState.Answer -> state.text
        is AssistantState.Failed -> stringResource(state.messageRes)
    }
    val counter = (state as? AssistantState.Guide)
        ?.takeIf { it.steps.size > 1 }
        ?.let { stringResource(R.string.assistant_step_counter, it.index + 1, it.steps.size) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .semantics { paneTitle = title }
            .verticalScroll(rememberScrollState())
            .padding(CoidaDimens.ScreenPadding),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        if (state == AssistantState.Listening) {
            // Escuchando, el micrófono crece y late con la voz, en grande y en el
            // centro: es lo que dice que le está oyendo.
            ListeningPulse(level)
        } else {
            // Redondo como el círculo del inicio, para que se vea que esto sale de
            // ahí, pero hundido y no relleno de acento: relleno parecía un botón más,
            // y no se puede pulsar.
            IconBadge(
                icon = R.drawable.ic_assistant,
                size = CoidaDimens.AssistantBadgeSize,
                plate = MaterialTheme.colorScheme.surfaceContainerLowest,
                shape = CircleShape,
            )
        }

        // Lo que va encima de la frase principal.
        when (state) {
            is AssistantState.Thinking -> state.heard?.let { Heard(it) }
            is AssistantState.Acting -> if (state.heard.isNotBlank()) Heard(state.heard)
            is AssistantState.Guide -> {
                // El contador va también dentro de lo que anuncia la frase.
                if (counter != null) StepCounter(counter)
                if (state.index == 0 && state.intro != null) Detail(state.intro)
            }
            else -> Unit
        }

        Message(text = message, announced = counter?.let { "$it. $message" } ?: message)

        when (state) {
            AssistantState.Idle -> Unit

            AssistantState.Listening -> {
                Detail(stringResource(R.string.assistant_listening_body))
                PrimaryButton(text = stringResource(R.string.assistant_done_speaking), onClick = onDoneSpeaking)
                OutlineButton(text = stringResource(R.string.assistant_cancel), onClick = onClose)
            }

            is AssistantState.Thinking ->
                OutlineButton(text = stringResource(R.string.assistant_cancel), onClick = onClose)

            // Va a hacerlo en cuanto acabe la frase: lo único que hay es pararlo.
            is AssistantState.Acting ->
                PrimaryButton(text = stringResource(R.string.assistant_cancel), onClick = onCancelAction)

            is AssistantState.Guide -> if (state.isLast) {
                if (state.highlightId != null) NoteCard(text = stringResource(R.string.assistant_highlight_hint))
                PrimaryButton(text = stringResource(R.string.assistant_understood), onClick = onFinish)
                RepeatButton(onRepeat)
            } else {
                PrimaryButton(text = stringResource(R.string.assistant_next), onClick = onNext)
                RepeatButton(onRepeat)
                OutlineButton(text = stringResource(R.string.assistant_finish), onClick = onFinish)
            }

            is AssistantState.Clarify -> {
                state.options.forEach { option ->
                    OutlineButton(
                        text = option.label,
                        icon = iconFor(option.action),
                        onClick = { onChoose(option) },
                    )
                }
                OutlineButton(text = stringResource(R.string.assistant_clarify_none), onClick = onClose)
            }

            is AssistantState.Answer -> {
                PrimaryButton(text = stringResource(R.string.assistant_finish), onClick = onFinish)
                SpeakAgainButton(onSpeakAgain)
                RepeatButton(onRepeat)
            }

            is AssistantState.Failed -> when {
                // Dice «repítemelo» y vuelve a escuchar solo: no hay nada que pulsar.
                state.listensAgain ->
                    OutlineButton(text = stringResource(R.string.assistant_cancel), onClick = onClose)

                // Si falta la clave o el micrófono, reintentar no arregla nada: lo
                // tiene que mirar el familiar.
                state.failure == AssistantFailure.UNAVAILABLE || state.failure == AssistantFailure.MICROPHONE ->
                    PrimaryButton(text = stringResource(R.string.assistant_finish), onClick = onFinish)

                else -> {
                    PrimaryButton(text = stringResource(R.string.assistant_try_again), onClick = onRetry)
                    OutlineButton(text = stringResource(R.string.assistant_finish), onClick = onFinish)
                }
            }
        }

        // En una guía o al aclarar se puede pedir otra cosa sin cerrar.
        if (state is AssistantState.Guide || state is AssistantState.Clarify) SpeakAgainButton(onSpeakAgain)
    }
}

/**
 * La frase principal: grande y anunciada por TalkBack cuando cambia. Si una palabra
 * no cabe en la línea («Encarnación» al 200 %), la letra baja en vez de partirla.
 *
 * @param announced lo que dice TalkBack, si no es igual que lo escrito.
 */
@Composable
private fun Message(text: String, announced: String) {
    FitText(
        text = text,
        style = MaterialTheme.typography.titleLarge,
        color = MaterialTheme.colorScheme.onBackground,
        maxLines = Int.MAX_VALUE,
        modifier = Modifier
            .fillMaxWidth()
            // Un solo nodo con el texto de dentro: si no, la región viva no tendría
            // qué anunciar.
            .semantics(mergeDescendants = true) {
                liveRegion = LiveRegionMode.Polite
                if (announced != text) contentDescription = announced
            },
    )
}

/** «Paso 2 de 3». Para TalkBack ya va dentro de la frase principal. */
@Composable
private fun StepCounter(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.clearAndSetSemantics { },
    )
}

@Composable
private fun Detail(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

/** Lo que ha entendido, para que vea a tiempo si se ha equivocado y pueda cancelar. */
@Composable
private fun Heard(text: String) {
    Text(
        text = stringResource(R.string.assistant_heard, text),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@Composable
private fun RepeatButton(onRepeat: () -> Unit) {
    OutlineButton(
        text = stringResource(R.string.assistant_repeat),
        icon = R.drawable.ic_volume,
        onClick = onRepeat,
    )
}

@Composable
private fun SpeakAgainButton(onSpeakAgain: () -> Unit) {
    OutlineButton(
        text = stringResource(R.string.assistant_speak_again),
        icon = R.drawable.ic_assistant,
        onClick = onSpeakAgain,
    )
}

private fun iconFor(action: AssistantAction): Int = when (action) {
    is AssistantAction.CallContact -> R.drawable.ic_call
    is AssistantAction.WhatsAppContact -> R.drawable.ic_messages
    is AssistantAction.OpenApp -> R.drawable.ic_apps
}

@Preview(showBackground = true, widthDp = 411, heightDp = 891)
@Composable
private fun AssistantActingPreview() {
    CoidaTheme(darkTheme = true) {
        AssistantScreen(
            state = AssistantState.Acting(
                action = AssistantAction.CallContact(
                    ContactShortcut("1", "María", "600000000", ContactAction.CALL, true),
                ),
                text = "Llamo a María.",
                heard = "Llama a mi hija María",
            ),
            level = 0f,
            onSpeakAgain = {},
            onDoneSpeaking = {},
            onCancelAction = {},
            onChoose = {},
            onNext = {},
            onRepeat = {},
            onRetry = {},
            onFinish = {},
            onClose = {},
        )
    }
}
