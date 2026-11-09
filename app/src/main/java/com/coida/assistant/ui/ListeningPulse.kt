package com.coida.assistant.ui

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.MotionDurationScale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.coida.R
import com.coida.assistant.VoiceTuning
import com.coida.core.ui.CoidaDimens
import com.coida.core.ui.CoidaTheme
import kotlin.coroutines.coroutineContext

/**
 * El micrófono mientras escucha: un círculo que crece con la voz y suelta anillos
 * que se abren hacia fuera, como ondas en el agua. Le dice a la persona que el móvil
 * la está oyendo, y cuánto. En silencio respira despacio, con un anillo tenue de vez
 * en cuando: así se ve que sigue escuchando.
 *
 * Mide lo mismo que el círculo del asistente en el inicio (96 dp), para que se vea
 * que esto sale de ahí. Va en un degradado de la marca, de `primary` a `secondary`:
 * en claro del marino al cian profundo, en oscuro del cian al azul claro. El
 * micrófono, en `onPrimary`, queda por encima de 6,7:1 sobre cualquier punto del
 * degradado en los dos temas. Los anillos son de `primary` y se desvanecen.
 *
 * Sin rebotes: crece y se encoge en línea recta, y los anillos se frenan al abrirse.
 * Con «Quitar animaciones» de Android no salen anillos: el círculo solo cambia de
 * tamaño con la voz. Para TalkBack no existe: el texto de debajo ya dice «Te escucho».
 *
 * @param level volumen de lo que oye, de 0 a 1 (escala logarítmica de `VoiceRecorder`).
 */
@Composable
internal fun ListeningPulse(level: Float, modifier: Modifier = Modifier) {
    val colors = MaterialTheme.colorScheme
    val current by rememberUpdatedState(level)
    val smooth by animateFloatAsState(
        targetValue = level,
        animationSpec = tween(VoiceTuning.FRAME_MS, easing = LinearEasing),
        label = "volumen",
    )
    var rings by remember { mutableStateOf(emptyList<Ring>()) }
    var now by remember { mutableLongStateOf(0L) }

    LaunchedEffect(Unit) {
        // Con las animaciones quitadas en Android, sin anillos.
        if ((coroutineContext[MotionDurationScale]?.scaleFactor ?: 1f) == 0f) return@LaunchedEffect
        var lastRing = 0L
        while (true) {
            val frame = withFrameNanos { it }
            now = frame
            val speaking = current >= SPEAKING_LEVEL
            val every = if (speaking) RING_EVERY_SPEAKING_NS else RING_EVERY_QUIET_NS
            val alive = rings.filter { frame - it.born < RING_LIFE_NS }
            rings = if (frame - lastRing >= every) {
                lastRing = frame
                alive + Ring(born = frame, strength = if (speaking) SPEAKING_FLOOR + (1f - SPEAKING_FLOOR) * current else QUIET_STRENGTH)
            } else {
                alive
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(CoidaDimens.ListeningPulseHeight)
            .clearAndSetSemantics { },
        contentAlignment = Alignment.Center,
    ) {
        val discScale = 1f + DISC_GROWTH * smooth
        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = Offset(size.width / 2, size.height / 2)
            val disc = CoidaDimens.AssistantButtonSize.toPx() / 2
            val farthest = size.height / 2
            rings.forEach { ring ->
                val progress = ((now - ring.born).toFloat() / RING_LIFE_NS).coerceIn(0f, 1f)
                val eased = FastOutSlowInEasing.transform(progress)
                val radius = disc + (farthest - disc) * eased
                val stroke = RING_STROKE.toPx() * (1f - progress * 0.6f)
                drawCircle(
                    color = colors.primary.copy(alpha = RING_ALPHA * ring.strength * (1f - progress)),
                    radius = radius - stroke / 2,
                    center = center,
                    style = Stroke(width = stroke),
                )
            }
            val radius = disc * discScale
            drawCircle(
                brush = Brush.linearGradient(
                    colors = listOf(colors.primary, colors.secondary),
                    start = Offset(center.x - radius, center.y - radius),
                    end = Offset(center.x + radius, center.y + radius),
                ),
                radius = radius,
                center = center,
            )
        }
        Icon(
            painter = painterResource(R.drawable.ic_assistant),
            contentDescription = null,
            tint = colors.onPrimary,
            modifier = Modifier
                .size(ICON_SIZE)
                .graphicsLayer {
                    scaleX = discScale
                    scaleY = discScale
                },
        )
    }
}

/** Un anillo que se abre: cuándo salió y con qué fuerza (el volumen de entonces). */
private class Ring(val born: Long, val strength: Float)

/** Por encima de este volumen se considera que habla: salen anillos seguidos y fuertes. */
private const val SPEAKING_LEVEL = 0.35f

/** Lo que crece el círculo con la voz a tope: un 22 %. */
private const val DISC_GROWTH = 0.22f

/** Lo que tarda un anillo en abrirse del todo y apagarse. */
private const val RING_LIFE_NS = 1_400_000_000L

/** Cada cuánto sale un anillo: hablando, seguido; en silencio, uno de vez en cuando. */
private const val RING_EVERY_SPEAKING_NS = 300_000_000L
private const val RING_EVERY_QUIET_NS = 1_600_000_000L

/** La fuerza de los anillos en silencio: apenas se ven. */
private const val QUIET_STRENGTH = 0.45f

/** Hablando, los anillos salen como poco con esta fuerza: con una voz floja casi no se veían. */
private const val SPEAKING_FLOOR = 0.5f

/** Lo opaco que sale un anillo a fuerza completa. */
private const val RING_ALPHA = 0.85f

private val RING_STROKE = 5.dp
private val ICON_SIZE = 44.dp

@Preview(showBackground = true, widthDp = 360, heightDp = 240)
@Composable
private fun ListeningPulsePreview() {
    CoidaTheme(darkTheme = true) { ListeningPulse(level = 0.7f) }
}
