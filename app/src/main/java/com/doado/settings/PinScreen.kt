package com.doado.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.doado.R
import com.doado.core.ui.DoadoCard
import com.doado.core.ui.DoadoDimens
import com.doado.core.ui.DoadoTextField
import com.doado.core.ui.NoteCard
import com.doado.core.ui.OutlineButton
import com.doado.core.ui.PrimaryButton
import com.doado.core.ui.ScreenHeader
import com.doado.core.ui.SettingsColumn

private const val PIN_LENGTH = 4

/** Los pasos por los que puede pasar la pantalla de PIN. */
private enum class Paso {
    COMPROBAR, ACTUAL, NUEVO, REPETIR,
    /** Configurar la pregunta de recuperación tras crear o cambiar el PIN. */
    PREGUNTA,
    /** Configurar la respuesta de recuperación. */
    RESPUESTA,
    /** Recuperar el acceso respondiendo la pregunta cuando se olvidó el PIN. */
    RECUPERAR,
}

/**
 * Puerta de Ajustes.
 *
 * Tres recorridos normales, según con qué se entre:
 *  · La primera vez no hay PIN: se crea escribiéndolo dos veces para cazar erratas.
 *  · Después se pide y ya está.
 *  · Con [changing] (el "Cambiar el PIN" de dentro de Ajustes) se pide primero
 *    el que hay ahora. Haber pasado la puerta hace un rato no basta: si alguien
 *    se encuentra el móvil abierto, no debería poder dejar fuera al familiar
 *    cambiándole el PIN (CU-07).
 *
 * Recorrido de recuperación (CON-05):
 *  · Desde COMPROBAR, si hay pregunta guardada, aparece "¿Olvidaste el PIN?".
 *  · El familiar responde la pregunta y puede poner un PIN nuevo sin perder la
 *    configuración. La pregunta de recuperación se mantiene sin cambios.
 *
 * Tras crear o cambiar el PIN siempre se ofrece establecer (o actualizar) la
 * pregunta de recuperación; se puede saltar.
 */
@Composable
fun PinGate(
    hasPin: Boolean,
    checkPin: (String) -> Boolean,
    savePin: (String) -> Unit,
    onUnlocked: () -> Unit,
    onBack: () -> Unit,
    changing: Boolean = false,
    hasRecoveryQuestion: Boolean = false,
    recoveryQuestion: String = "",
    checkRecoveryAnswer: (String) -> Boolean = { false },
    saveRecoveryQuestion: (question: String, answer: String) -> Unit = { _, _ -> },
) {
    val primerPaso = when {
        changing && hasPin -> Paso.ACTUAL
        hasPin -> Paso.COMPROBAR
        else -> Paso.NUEVO
    }
    var paso by remember { mutableStateOf(primerPaso) }
    var nuevo by remember { mutableStateOf<String?>(null) }
    var preguntaGuardada by remember { mutableStateOf("") }
    var errorRes by remember { mutableStateOf<Int?>(null) }
    var attempt by remember { mutableIntStateOf(0) }
    // Distingue si se llegó a NUEVO/REPETIR desde una recuperación (sin volver a pedir pregunta).
    var isRecovering by remember { mutableStateOf(false) }

    when (paso) {
        Paso.PREGUNTA -> {
            RecoveryQuestionScreen(
                onNext = { question ->
                    preguntaGuardada = question
                    paso = Paso.RESPUESTA
                },
                onSkip = onUnlocked,
                onBack = onUnlocked,
            )
            return
        }

        Paso.RESPUESTA -> {
            RecoveryAnswerScreen(
                question = preguntaGuardada,
                onSave = { answer ->
                    saveRecoveryQuestion(preguntaGuardada, answer)
                    onUnlocked()
                },
                onBack = { paso = Paso.PREGUNTA },
            )
            return
        }

        Paso.RECUPERAR -> {
            RecoveryAccessScreen(
                question = recoveryQuestion,
                onAnswer = { answer ->
                    if (checkRecoveryAnswer(answer)) {
                        isRecovering = true
                        paso = Paso.NUEVO
                        errorRes = null
                    } else {
                        errorRes = R.string.recovery_access_wrong
                    }
                },
                error = errorRes?.let { stringResource(it) },
                onBack = {
                    errorRes = null
                    paso = Paso.COMPROBAR
                },
            )
            return
        }

        else -> Unit
    }

    val title = when (paso) {
        Paso.COMPROBAR -> R.string.pin_title
        Paso.ACTUAL -> R.string.pin_current_title
        Paso.NUEVO -> if (changing || isRecovering) R.string.pin_new_title else R.string.pin_create_title
        Paso.REPETIR -> R.string.pin_repeat_title
        else -> R.string.pin_title
    }
    val subtitle = when (paso) {
        Paso.COMPROBAR -> R.string.pin_subtitle
        Paso.ACTUAL -> R.string.pin_current_subtitle
        Paso.NUEVO -> if (changing || isRecovering) R.string.pin_new_subtitle else R.string.pin_create_subtitle
        Paso.REPETIR -> R.string.pin_repeat_subtitle
        else -> R.string.pin_subtitle
    }

    PinScreen(
        title = stringResource(title),
        subtitle = stringResource(subtitle),
        error = errorRes?.let { stringResource(it) },
        resetKey = attempt,
        showForgotPin = paso == Paso.COMPROBAR && hasRecoveryQuestion,
        onForgotPin = { paso = Paso.RECUPERAR },
        onBack = onBack,
        onComplete = { pin ->
            when (paso) {
                Paso.COMPROBAR ->
                    if (checkPin(pin)) onUnlocked() else { errorRes = R.string.pin_wrong; attempt++ }

                Paso.ACTUAL ->
                    if (checkPin(pin)) {
                        paso = Paso.NUEVO
                        errorRes = null
                        attempt++
                    } else {
                        errorRes = R.string.pin_wrong
                        attempt++
                    }

                Paso.NUEVO -> {
                    nuevo = pin
                    paso = Paso.REPETIR
                    errorRes = null
                    attempt++  // resetea digits en PinScreen (keyed a attempt)
                }

                Paso.REPETIR ->
                    if (pin == nuevo) {
                        savePin(pin)
                        if (isRecovering) {
                            onUnlocked()
                        } else {
                            nuevo = null
                            paso = Paso.PREGUNTA
                            errorRes = null
                        }
                    } else {
                        nuevo = null
                        paso = Paso.NUEVO
                        errorRes = R.string.pin_mismatch
                        attempt++
                    }

                else -> Unit
            }
        },
    )
}

@Composable
private fun PinScreen(
    title: String,
    subtitle: String,
    error: String?,
    resetKey: Int,
    showForgotPin: Boolean,
    onForgotPin: () -> Unit,
    onComplete: (String) -> Unit,
    onBack: () -> Unit,
) {
    var digits by remember(resetKey) { mutableStateOf("") }

    fun press(digit: Char) {
        if (digits.length >= PIN_LENGTH) return
        digits += digit
        if (digits.length == PIN_LENGTH) onComplete(digits)
    }

    SettingsColumn(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
    ) {
        ScreenHeader(
            title = title,
            subtitle = subtitle,
            backLabel = stringResource(R.string.cd_back_home),
            onBack = onBack,
        )

        val progress = stringResource(R.string.cd_pin_progress, digits.length, PIN_LENGTH)
        Row(
            modifier = Modifier
                .padding(vertical = 12.dp)
                .semantics(mergeDescendants = true) {
                    contentDescription = progress
                    liveRegion = LiveRegionMode.Polite
                },
            horizontalArrangement = Arrangement.spacedBy(20.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            repeat(PIN_LENGTH) { index ->
                val filled = index < digits.length
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .background(if (filled) MaterialTheme.colorScheme.primary else Color.Transparent, CircleShape)
                        .border(DoadoDimens.BorderWidth, MaterialTheme.colorScheme.outline, CircleShape),
                )
            }
        }

        if (error != null) {
            Text(
                text = error,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.semantics { liveRegion = LiveRegionMode.Assertive },
            )
        }

        val keys = listOf("123", "456", "789")
        keys.forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                row.forEach { digit ->
                    PinKey(onClick = { press(digit) }, modifier = Modifier.weight(1f)) {
                        KeyText(digit.toString())
                    }
                }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            PinKey(onClick = { digits = "" }, modifier = Modifier.weight(1f), outlined = true) {
                Text(
                    text = stringResource(R.string.pin_clear),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            PinKey(onClick = { press('0') }, modifier = Modifier.weight(1f)) {
                KeyText("0")
            }
            PinKey(onClick = { digits = digits.dropLast(1) }, modifier = Modifier.weight(1f), outlined = true) {
                Icon(
                    painter = painterResource(R.drawable.ic_backspace),
                    contentDescription = stringResource(R.string.cd_pin_delete),
                    modifier = Modifier.size(32.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        NoteCard(
            text = stringResource(R.string.pin_note),
            icon = R.drawable.ic_lock,
            modifier = Modifier.padding(top = 8.dp),
        )

        if (showForgotPin) {
            OutlineButton(
                text = stringResource(R.string.pin_forgot),
                onClick = onForgotPin,
            )
        }
    }
}

@Composable
private fun RecoveryQuestionScreen(
    onNext: (String) -> Unit,
    onSkip: () -> Unit,
    onBack: () -> Unit,
) {
    var text by remember { mutableStateOf("") }

    SettingsColumn(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
    ) {
        ScreenHeader(
            title = stringResource(R.string.recovery_question_title),
            subtitle = stringResource(R.string.recovery_question_subtitle),
            backLabel = stringResource(R.string.cd_back_settings),
            onBack = onBack,
        )

        DoadoTextField(
            value = text,
            onValueChange = { text = it },
            placeholder = stringResource(R.string.recovery_question_hint),
            singleLine = false,
            minLines = 2,
            onDone = { if (text.isNotBlank()) onNext(text) },
        )

        NoteCard(
            text = stringResource(R.string.recovery_question_note),
            icon = R.drawable.ic_info,
        )

        PrimaryButton(
            text = stringResource(R.string.recovery_question_next),
            onClick = { onNext(text) },
            enabled = text.isNotBlank(),
        )

        OutlineButton(
            text = stringResource(R.string.recovery_question_skip),
            onClick = onSkip,
        )
    }
}

@Composable
private fun RecoveryAnswerScreen(
    question: String,
    onSave: (String) -> Unit,
    onBack: () -> Unit,
) {
    var text by remember { mutableStateOf("") }

    SettingsColumn(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
    ) {
        ScreenHeader(
            title = stringResource(R.string.recovery_answer_title),
            subtitle = stringResource(R.string.recovery_answer_subtitle),
            backLabel = stringResource(R.string.cd_back_settings),
            onBack = onBack,
        )

        NoteCard(text = question, icon = R.drawable.ic_lock)

        DoadoTextField(
            value = text,
            onValueChange = { text = it },
            placeholder = stringResource(R.string.recovery_answer_hint),
            onDone = { if (text.isNotBlank()) onSave(text) },
        )

        PrimaryButton(
            text = stringResource(R.string.recovery_answer_save),
            onClick = { onSave(text) },
            enabled = text.isNotBlank(),
        )
    }
}

@Composable
private fun RecoveryAccessScreen(
    question: String,
    onAnswer: (String) -> Unit,
    error: String?,
    onBack: () -> Unit,
) {
    var text by remember { mutableStateOf("") }

    SettingsColumn(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
    ) {
        ScreenHeader(
            title = stringResource(R.string.recovery_access_title),
            subtitle = stringResource(R.string.recovery_access_subtitle),
            backLabel = stringResource(R.string.cd_back_home),
            onBack = onBack,
        )

        NoteCard(text = question, icon = R.drawable.ic_lock)

        DoadoTextField(
            value = text,
            onValueChange = { text = it },
            placeholder = stringResource(R.string.recovery_answer_hint),
            isError = error != null,
            onDone = { if (text.isNotBlank()) onAnswer(text) },
        )

        if (error != null) {
            Text(
                text = error,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.semantics { liveRegion = LiveRegionMode.Assertive },
            )
        }

        PrimaryButton(
            text = stringResource(R.string.recovery_access_confirm),
            onClick = { onAnswer(text) },
            enabled = text.isNotBlank(),
        )
    }
}

@Composable
private fun PinKey(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    outlined: Boolean = false,
    content: @Composable () -> Unit,
) {
    DoadoCard(
        onClick = onClick,
        modifier = modifier.heightIn(min = 80.dp),
        shape = RoundedCornerShape(18.dp),
        color = if (outlined) Color.Transparent else MaterialTheme.colorScheme.surfaceContainer,
    ) {
        Box(contentAlignment = Alignment.Center) { content() }
    }
}

@Composable
private fun KeyText(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.headlineSmall,
        color = MaterialTheme.colorScheme.onSurface,
    )
}
