package com.doado.assistant.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.paneTitle
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.Hyphens
import androidx.compose.ui.text.style.LineBreak
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.doado.R
import com.doado.assistant.AssistantAction
import com.doado.assistant.AssistantFailure
import com.doado.assistant.AssistantState
import com.doado.assistant.ClarifyOption
import com.doado.assistant.messageRes
import com.doado.core.ui.DoadoCard
import com.doado.core.ui.DoadoDimens
import com.doado.core.ui.DoadoTheme
import com.doado.core.ui.IconBadge
import com.doado.core.ui.NoteCard
import com.doado.core.ui.OutlineButton
import com.doado.core.ui.PrimaryButton
import com.doado.data.ContactAction
import com.doado.data.ContactShortcut

/**
 * Pantalla del asistente (CU-04, CU-05). La lee y la oye la persona mayor.
 *
 * Una sola idea cada vez, en letra grande, y como mucho un botón principal. Todo
 * lo que el móvil dice en voz alta está también escrito, con «Repetir».
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
    followUpListening: Boolean,
    onSpeakAgain: () -> Unit,
    onDoneSpeaking: () -> Unit,
    onConfirm: () -> Unit,
    onDecline: () -> Unit,
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
        is AssistantState.Confirm -> state.question
        is AssistantState.Guide -> state.steps[state.index]
        is AssistantState.Clarify -> state.question
        is AssistantState.Answer -> state.text
        is AssistantState.Failed -> stringResource(state.failure.messageRes)
    }
    val counter = (state as? AssistantState.Guide)
        ?.takeIf { it.steps.size > 1 }
        ?.let { stringResource(R.string.assistant_step_counter, it.index + 1, it.steps.size) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .semantics { paneTitle = title }
            .verticalScroll(rememberScrollState())
            .padding(DoadoDimens.ScreenPadding),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        IconBadge(icon = R.drawable.ic_assistant, size = DoadoDimens.CornerButtonSize)

        // Lo que va encima de la frase principal.
        when (state) {
            is AssistantState.Thinking -> state.heard?.let { Heard(it) }
            is AssistantState.Confirm -> if (state.heard.isNotBlank()) Heard(state.heard)
            is AssistantState.Guide -> {
                // El contador va también dentro de lo que anuncia la frase.
                if (counter != null) StepCounter(counter)
                if (state.index == 0 && state.intro != null) Detail(state.intro)
            }
            else -> Unit
        }

        Message(text = message, announced = counter?.let { "$it. $message" } ?: message)

        // La conversación (D-31): tras hablar, el micrófono se reabre solo con la
        // pregunta a la vista. Si se ha cerrado sin oír nada, se puede reabrir.
        val conversational = state is AssistantState.Confirm || state is AssistantState.Guide ||
            state is AssistantState.Clarify || state is AssistantState.Answer ||
            (state is AssistantState.Failed && state.failure == AssistantFailure.NOT_UNDERSTOOD)
        if (conversational) {
            if (followUpListening) {
                FollowUpStrip(level, onDoneSpeaking)
            } else {
                OutlineButton(
                    text = stringResource(R.string.assistant_speak_again),
                    icon = R.drawable.ic_assistant,
                    onClick = onSpeakAgain,
                )
            }
        }

        when (state) {
            AssistantState.Idle -> Unit

            AssistantState.Listening -> {
                Detail(stringResource(R.string.assistant_listening_body))
                LevelBar(level)
                PrimaryButton(text = stringResource(R.string.assistant_done_speaking), onClick = onDoneSpeaking)
                OutlineButton(text = stringResource(R.string.assistant_cancel), onClick = onClose)
            }

            is AssistantState.Thinking ->
                OutlineButton(text = stringResource(R.string.assistant_cancel), onClick = onClose)

            is AssistantState.Confirm -> {
                PrimaryButton(text = state.yesLabel, onClick = onConfirm)
                OutlineButton(text = stringResource(R.string.assistant_no), onClick = onDecline)
                RepeatButton(onRepeat)
            }

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
                RepeatButton(onRepeat)
            }

            is AssistantState.Failed -> {
                // Si falta la clave o el micrófono, reintentar no arregla nada: lo
                // tiene que mirar el familiar.
                val canRetry = state.failure != AssistantFailure.UNAVAILABLE &&
                    state.failure != AssistantFailure.MICROPHONE
                if (canRetry) {
                    PrimaryButton(text = stringResource(R.string.assistant_try_again), onClick = onRetry)
                    OutlineButton(text = stringResource(R.string.assistant_finish), onClick = onFinish)
                } else {
                    PrimaryButton(text = stringResource(R.string.assistant_finish), onClick = onFinish)
                }
            }
        }
    }
}

/**
 * La frase principal: grande y anunciada por TalkBack cuando cambia.
 *
 * Con la letra al 200 % un nombre largo («¿Llamo a Encarnación?») no cabe en una
 * línea: se parte con guion por sílabas en vez de a mitad de palabra.
 *
 * @param announced lo que dice TalkBack, si no es igual que lo escrito.
 */
@Composable
private fun Message(text: String, announced: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleLarge.copy(
            hyphens = Hyphens.Auto,
            lineBreak = LineBreak.Paragraph,
        ),
        color = MaterialTheme.colorScheme.onBackground,
        modifier = Modifier.semantics {
            liveRegion = LiveRegionMode.Polite
            if (announced != text) contentDescription = announced
        },
    )
}

/**
 * El micrófono se ha vuelto a abrir para contestar. Va en un recuadro con su
 * barra, para que se vea que escucha sin tapar la pregunta ni sus botones.
 */
@Composable
private fun FollowUpStrip(level: Float, onDoneSpeaking: () -> Unit) {
    DoadoCard(onClick = null, highlighted = true, modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = stringResource(R.string.assistant_follow_up_listening),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            LevelBar(level)
            OutlineButton(text = stringResource(R.string.assistant_done_speaking), onClick = onDoneSpeaking)
        }
    }
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

/** Lo que ha entendido, para que pueda ver si se ha equivocado antes de decir «Sí». */
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

/**
 * Barra que se mueve con la voz: le dice que el móvil la está oyendo. Es solo una
 * ayuda visual; para TalkBack no existe, el texto de arriba ya lo dice.
 */
@Composable
private fun LevelBar(level: Float) {
    val colors = MaterialTheme.colorScheme
    val shape = RoundedCornerShape(DoadoDimens.ControlCornerRadius)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(24.dp)
            .border(DoadoDimens.BorderWidth, colors.outline, shape)
            .padding(4.dp)
            .clearAndSetSemantics { },
        contentAlignment = Alignment.CenterStart,
    ) {
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .fillMaxWidth(level.coerceIn(0.02f, 1f))
                .background(colors.primary, shape),
        )
    }
}

private fun iconFor(action: AssistantAction): Int = when (action) {
    is AssistantAction.CallContact -> R.drawable.ic_call
    is AssistantAction.WhatsAppContact -> R.drawable.ic_messages
    is AssistantAction.OpenApp -> R.drawable.ic_apps
}

@Preview(showBackground = true, widthDp = 411, heightDp = 891)
@Composable
private fun AssistantConfirmPreview() {
    DoadoTheme(darkTheme = true) {
        AssistantScreen(
            state = AssistantState.Confirm(
                action = AssistantAction.CallContact(
                    ContactShortcut("1", "María", "600000000", ContactAction.CALL, true),
                ),
                question = "¿Llamo a María?",
                yesLabel = "Sí, llama",
                heard = "Llama a mi hija María",
            ),
            level = 0.4f,
            followUpListening = true,
            onSpeakAgain = {},
            onDoneSpeaking = {},
            onConfirm = {},
            onDecline = {},
            onChoose = {},
            onNext = {},
            onRepeat = {},
            onRetry = {},
            onFinish = {},
            onClose = {},
        )
    }
}
