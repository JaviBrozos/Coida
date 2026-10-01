package com.doado.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.doado.R
import com.doado.core.ui.DoadoCard
import com.doado.core.ui.DoadoDimens
import com.doado.core.ui.NoteCard
import com.doado.core.ui.OutlineButton
import com.doado.core.ui.PrimaryButton
import com.doado.core.ui.ScreenHeader
import com.doado.core.ui.SettingsColumn
import com.doado.data.PinLock
import com.doado.data.PinStore
import kotlinx.coroutines.delay

private const val PIN_LENGTH = 4
private const val ONE_SECOND_MS = 1_000L

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
 *
 * Límite de intentos (RNF-SEG-03, D-24): [checkPin] y [checkRecoveryAnswer]
 * cuentan los fallos y [lockState] dice cómo va el bloqueo. A partir del cuarto
 * fallo de la misma visita aparece arriba una nota grande para la persona mayor, que puede haber
 * llegado aquí trasteando, con un botón que la devuelve al inicio
 * ([onExitToHome]). Del quinto en adelante el teclado se bloquea un tiempo.
 * Solo esta puerta: el inicio sigue funcionando.
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
    lockState: () -> PinLock = { PinLock(failures = 0, remainingMs = 0L) },
    onExitToHome: () -> Unit = onBack,
) {
    var lock by remember { mutableStateOf(PinLock(failures = 0, remainingMs = 0L)) }
    // lockState() puede escribir en disco (al detectar un reinicio o al cumplirse
    // el bloqueo), así que no se llama desde la composición sino aquí. Si en el
    // primer fotograma el teclado saliera activo, da igual: checkPin rechaza
    // cualquier intento mientras dure el bloqueo.
    LaunchedEffect(Unit) { lock = lockState() }
    // Mientras dura el bloqueo se relee cada segundo para la cuenta atrás; al
    // acabar, el teclado se desbloquea solo. Sin bloqueo no corre nada.
    LaunchedEffect(lock.locked) {
        while (lock.locked) {
            delay(ONE_SECOND_MS)
            lock = lockState()
        }
    }

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
    // Fallos de esta visita, solo para la nota de volver al inicio. No es el
    // contador guardado del bloqueo: ese no caduca, y si la nota dependiera de él
    // saldría de primeras al día siguiente de que el familiar se equivocara,
    // empujando el teclado hacia abajo. Vuelve a cero cada vez que se abre la puerta.
    var visitFailures by remember { mutableIntStateOf(0) }
    val showExitNote = visitFailures >= PinStore.FAILURES_BEFORE_EXIT_NOTE
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
                        visitFailures++
                    }
                    lock = lockState()
                },
                error = errorRes?.takeUnless { lock.locked }?.let { stringResource(it) },
                lock = lock,
                showExitNote = showExitNote,
                onExitToHome = onExitToHome,
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

    // El límite solo cuenta donde se comprueba algo; crear o repetir un PIN nuevo
    // no es un intento de entrar.
    val checksPin = paso == Paso.COMPROBAR || paso == Paso.ACTUAL

    PinScreen(
        title = stringResource(title),
        subtitle = stringResource(subtitle),
        error = errorRes?.takeUnless { checksPin && lock.locked }?.let { stringResource(it) },
        resetKey = attempt,
        lock = if (checksPin) lock else PinLock(failures = 0, remainingMs = 0L),
        showExitNote = checksPin && showExitNote,
        onExitToHome = onExitToHome,
        showForgotPin = paso == Paso.COMPROBAR && hasRecoveryQuestion && !lock.locked,
        onForgotPin = { paso = Paso.RECUPERAR },
        onBack = onBack,
        onComplete = { pin ->
            when (paso) {
                Paso.COMPROBAR -> {
                    if (checkPin(pin)) {
                        onUnlocked()
                    } else {
                        errorRes = R.string.pin_wrong
                        visitFailures++
                    }
                    lock = lockState()
                    attempt++
                }

                Paso.ACTUAL -> {
                    if (checkPin(pin)) {
                        paso = Paso.NUEVO
                        errorRes = null
                    } else {
                        errorRes = R.string.pin_wrong
                        visitFailures++
                    }
                    lock = lockState()
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
    lock: PinLock,
    showExitNote: Boolean,
    onExitToHome: () -> Unit,
    showForgotPin: Boolean,
    onForgotPin: () -> Unit,
    onComplete: (String) -> Unit,
    onBack: () -> Unit,
) {
    // Al bloquearse, los puntos a medio escribir se vacían.
    var digits by remember(resetKey, lock.locked) { mutableStateOf("") }
    val keysEnabled = !lock.locked

    fun press(digit: Char) {
        if (!keysEnabled || digits.length >= PIN_LENGTH) return
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

        if (showExitNote) {
            ExitToHomeNote(onExitToHome = onExitToHome)
        }
        if (lock.locked) {
            LockNotice(remainingMs = lock.remainingMs)
        }

        val progress = stringResource(R.string.cd_pin_progress, digits.length, PIN_LENGTH)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp)
                .semantics(mergeDescendants = true) {
                    contentDescription = progress
                    liveRegion = LiveRegionMode.Polite
                },
            // Centrados sobre el teclado, que ocupa todo el ancho.
            horizontalArrangement = Arrangement.spacedBy(20.dp, Alignment.CenterHorizontally),
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
                    PinKey(onClick = { press(digit) }, modifier = Modifier.weight(1f), enabled = keysEnabled) {
                        KeyText(digit.toString(), enabled = keysEnabled)
                    }
                }
            }
        }
        // Como en el teclado del teléfono: hueco, 0 y borrar. Con cuatro cifras no
        // hace falta un «empezar de nuevo»: se borra de una en una. La tecla de
        // borrar lleva la palabra debajo del icono, que para una persona mayor dice
        // más que el icono solo. Con la letra grande crece, y el 0 crece con ella
        // para que la fila no quede desigual.
        val keyColor = if (keysEnabled) MaterialTheme.colorScheme.onSurfaceVariant
        else MaterialTheme.colorScheme.outlineVariant
        val deleteLabel = stringResource(R.string.cd_pin_delete)
        Row(
            modifier = Modifier.height(IntrinsicSize.Min),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Spacer(modifier = Modifier.weight(1f))
            PinKey(
                onClick = { press('0') },
                modifier = Modifier.weight(1f).fillMaxHeight(),
                enabled = keysEnabled,
            ) {
                KeyText("0", enabled = keysEnabled)
            }
            PinKey(
                onClick = { digits = digits.dropLast(1) },
                modifier = Modifier.weight(1f).fillMaxHeight(),
                outlined = true,
                enabled = keysEnabled,
            ) {
                Column(
                    modifier = Modifier.padding(vertical = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_backspace),
                        contentDescription = null,
                        modifier = Modifier.size(28.dp),
                        tint = keyColor,
                    )
                    // TalkBack dice «Borrar la última cifra», más claro que «Borrar».
                    Text(
                        text = stringResource(R.string.pin_delete),
                        style = MaterialTheme.typography.labelSmall,
                        color = keyColor,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.semantics { contentDescription = deleteLabel },
                    )
                }
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

        OutlinedTextField(
            value = text,
            onValueChange = { text = it },
            modifier = Modifier.fillMaxWidth(),
            placeholder = {
                Text(
                    text = stringResource(R.string.recovery_question_hint),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            },
            textStyle = MaterialTheme.typography.bodyMedium,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { if (text.isNotBlank()) onNext(text) }),
            minLines = 2,
            shape = RoundedCornerShape(DoadoDimens.CardCornerRadius),
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

        OutlinedTextField(
            value = text,
            onValueChange = { text = it },
            modifier = Modifier.fillMaxWidth(),
            placeholder = {
                Text(
                    text = stringResource(R.string.recovery_answer_hint),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            },
            textStyle = MaterialTheme.typography.bodyMedium,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { if (text.isNotBlank()) onSave(text) }),
            singleLine = true,
            shape = RoundedCornerShape(DoadoDimens.CardCornerRadius),
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
    lock: PinLock,
    showExitNote: Boolean,
    onExitToHome: () -> Unit,
    onBack: () -> Unit,
) {
    var text by remember { mutableStateOf("") }
    val canAnswer = !lock.locked

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

        if (showExitNote) {
            ExitToHomeNote(onExitToHome = onExitToHome)
        }
        if (lock.locked) {
            LockNotice(remainingMs = lock.remainingMs)
        }

        NoteCard(text = question, icon = R.drawable.ic_lock)

        OutlinedTextField(
            value = text,
            onValueChange = { text = it },
            enabled = canAnswer,
            modifier = Modifier.fillMaxWidth(),
            placeholder = {
                Text(
                    text = stringResource(R.string.recovery_answer_hint),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            },
            textStyle = MaterialTheme.typography.bodyMedium,
            isError = error != null,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { if (canAnswer && text.isNotBlank()) onAnswer(text) }),
            singleLine = true,
            shape = RoundedCornerShape(DoadoDimens.CardCornerRadius),
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
            enabled = canAnswer && text.isNotBlank(),
        )
    }
}

/**
 * Nota para la persona mayor, que puede haber llegado a la puerta de ajustes
 * trasteando y no saber salir. Va arriba, en grande y con un botón que la lleva
 * al inicio: no depende de que encuentre el botón atrás de Android, que con la
 * navegación por gestos ni siquiera existe.
 */
@Composable
private fun ExitToHomeNote(onExitToHome: () -> Unit) {
    DoadoCard(onClick = null, highlighted = true, modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Text(
                text = stringResource(R.string.pin_exit_note_title),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = stringResource(R.string.pin_exit_note_body),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            PrimaryButton(
                text = stringResource(R.string.pin_exit_note_button),
                icon = R.drawable.ic_home,
                onClick = onExitToHome,
            )
        }
    }
}

/**
 * Aviso de bloqueo con la cuenta atrás. La cuenta va sola en su línea y en
 * grande, centrada como el teclado, para que no se parta a media frase.
 *
 * TalkBack anuncia el título una vez al aparecer; la cuenta atrás no es región
 * viva, para no leer un número cada segundo.
 */
@Composable
private fun LockNotice(remainingMs: Long) {
    val totalSeconds = (remainingMs + 999) / 1000
    val countdown = "%d:%02d".format(totalSeconds / 60, totalSeconds % 60)
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(
            text = stringResource(R.string.pin_locked_title),
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.error,
            textAlign = TextAlign.Center,
            modifier = Modifier.semantics { liveRegion = LiveRegionMode.Assertive },
        )
        Text(
            text = stringResource(R.string.pin_locked_countdown),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
        )
        Text(
            text = countdown,
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

@Composable
private fun PinKey(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    outlined: Boolean = false,
    enabled: Boolean = true,
    content: @Composable () -> Unit,
) {
    DoadoCard(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.heightIn(min = 80.dp),
        shape = RoundedCornerShape(18.dp),
        color = if (outlined) Color.Transparent else MaterialTheme.colorScheme.surfaceContainer,
    ) {
        Box(contentAlignment = Alignment.Center) { content() }
    }
}

@Composable
private fun KeyText(text: String, enabled: Boolean = true) {
    Text(
        text = text,
        style = MaterialTheme.typography.headlineSmall,
        color = if (enabled) MaterialTheme.colorScheme.onSurface
        else MaterialTheme.colorScheme.outlineVariant,
    )
}
