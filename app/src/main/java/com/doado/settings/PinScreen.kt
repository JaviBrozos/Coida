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
import com.doado.core.ui.NoteCard
import com.doado.core.ui.ScreenHeader
import com.doado.core.ui.SettingsColumn

private const val PIN_LENGTH = 4

/**
 * Puerta de Ajustes. Si todavía no hay PIN, lo crea (dos veces, para evitar
 * erratas); si ya lo hay, lo pide. [changing] fuerza el modo de crear, para
 * "Cambiar el PIN" desde dentro de Ajustes.
 */
@Composable
fun PinGate(
    hasPin: Boolean,
    checkPin: (String) -> Boolean,
    savePin: (String) -> Unit,
    onUnlocked: () -> Unit,
    onBack: () -> Unit,
    changing: Boolean = false,
) {
    val creating = changing || !hasPin
    var firstEntry by remember { mutableStateOf<String?>(null) }
    var errorRes by remember { mutableStateOf<Int?>(null) }
    // Cambia en cada intento para vaciar los puntos.
    var attempt by remember { mutableIntStateOf(0) }

    val title = when {
        !creating -> R.string.pin_title
        firstEntry == null -> R.string.pin_create_title
        else -> R.string.pin_repeat_title
    }
    val subtitle = when {
        !creating -> R.string.pin_subtitle
        firstEntry == null -> R.string.pin_create_subtitle
        else -> R.string.pin_repeat_subtitle
    }

    PinScreen(
        title = stringResource(title),
        subtitle = stringResource(subtitle),
        error = errorRes?.let { stringResource(it) },
        resetKey = attempt,
        onBack = onBack,
        onComplete = { pin ->
            when {
                !creating && checkPin(pin) -> onUnlocked()
                !creating -> errorRes = R.string.pin_wrong
                firstEntry == null -> firstEntry = pin.also { errorRes = null }
                firstEntry == pin -> {
                    savePin(pin)
                    onUnlocked()
                }

                else -> {
                    firstEntry = null
                    errorRes = R.string.pin_mismatch
                }
            }
            attempt++
        },
    )
}

@Composable
private fun PinScreen(
    title: String,
    subtitle: String,
    error: String?,
    resetKey: Int,
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
                .semantics(mergeDescendants = true) { contentDescription = progress },
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
