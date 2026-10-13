package com.doado.assistant.audio

import android.Manifest
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.media.audiofx.AudioEffect
import android.media.audiofx.AutomaticGainControl
import android.media.audiofx.NoiseSuppressor
import androidx.annotation.RequiresPermission
import com.doado.assistant.AssistantError
import com.doado.assistant.AssistantFailure
import com.doado.assistant.VoiceTuning
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.math.max
import kotlin.math.sqrt

/**
 * Graba una orden hablada y decide cuándo ha terminado.
 *
 * Grabamos nosotros en vez de usar el reconocedor de Android porque así mandamos
 * en el corte (D-25): la frase acaba tras [VoiceTuning.END_OF_SPEECH_SILENCE_MS]
 * de silencio, al llegar al tope o cuando se pulsa «Ya he terminado».
 *
 * Distingue voz de silencio por volumen, comparando con el ruido de fondo que va
 * midiendo: con la tele puesta el umbral sube solo. El audio vive en memoria y
 * nunca toca el disco.
 */
class VoiceRecorder {

    /**
     * Graba hasta que acaba la frase. Devuelve un WAV listo para enviar, o null si
     * no ha llegado a hablar.
     *
     * @param stopRequested se pone a true con «Ya he terminado».
     * @param onLevel volumen entre 0 y 1, cada trozo, para la barra de la pantalla.
     */
    @RequiresPermission(Manifest.permission.RECORD_AUDIO)
    suspend fun record(stopRequested: AtomicBoolean, onLevel: (Float) -> Unit): ByteArray? =
        withContext(Dispatchers.IO) {
            val rate = VoiceTuning.SAMPLE_RATE
            val frameSamples = rate * VoiceTuning.FRAME_MS / 1000
            val minBuffer = AudioRecord.getMinBufferSize(
                rate,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT,
            )
            if (minBuffer <= 0) throw AssistantError(AssistantFailure.MICROPHONE)

            val recorder = try {
                AudioRecord(
                    // Pensada para reconocimiento: sin el procesado de llamadas, que
                    // recorta las voces flojas.
                    MediaRecorder.AudioSource.VOICE_RECOGNITION,
                    rate,
                    AudioFormat.CHANNEL_IN_MONO,
                    AudioFormat.ENCODING_PCM_16BIT,
                    max(minBuffer, frameSamples * 2 * 4),
                )
            } catch (e: IllegalArgumentException) {
                throw AssistantError(AssistantFailure.MICROPHONE, e)
            }
            if (recorder.state != AudioRecord.STATE_INITIALIZED) {
                recorder.release()
                throw AssistantError(AssistantFailure.MICROPHONE)
            }
            val effects = enableVoiceEffects(recorder.audioSessionId)

            val pcm = ByteArrayOutputStream()
            val frame = ShortArray(frameSamples)
            var elapsedMs = 0L
            var speechMs = 0L
            var silenceMs = 0L
            var noiseFloor = -1.0

            try {
                recorder.startRecording()
                // Otra app (una llamada, una grabadora) puede tener el micrófono.
                if (recorder.recordingState != AudioRecord.RECORDSTATE_RECORDING) {
                    throw AssistantError(AssistantFailure.MICROPHONE)
                }
                while (true) {
                    ensureActive()
                    val read = recorder.read(frame, 0, frameSamples)
                    if (read < 0) throw AssistantError(AssistantFailure.MICROPHONE)
                    if (read == 0) continue

                    val frameMs = read * 1000L / rate
                    elapsedMs += frameMs
                    val rms = rms(frame, read)

                    // El primer trozo llega antes de que dé tiempo a hablar: es ruido.
                    if (noiseFloor < 0) noiseFloor = rms
                    val threshold = max(VoiceTuning.MIN_SPEECH_RMS, noiseFloor * VoiceTuning.NOISE_FACTOR)
                    if (rms > threshold) {
                        speechMs += frameMs
                        silenceMs = 0
                    } else {
                        silenceMs += frameMs
                        noiseFloor = noiseFloor * 0.9 + rms * 0.1
                    }

                    writeLittleEndian(pcm, frame, read)
                    onLevel((rms / VoiceTuning.LEVEL_FULL_SCALE).coerceIn(0.0, 1.0).toFloat())

                    val hasSpoken = speechMs >= VoiceTuning.MIN_SPEECH_MS
                    val done = stopRequested.get() ||
                        elapsedMs >= VoiceTuning.MAX_RECORDING_MS ||
                        (!hasSpoken && elapsedMs >= VoiceTuning.WAIT_FOR_SPEECH_MS) ||
                        (hasSpoken && silenceMs >= VoiceTuning.END_OF_SPEECH_SILENCE_MS)
                    if (done) break
                }
            } finally {
                runCatching { recorder.stop() }
                recorder.release()
                effects.forEach { it.release() }
            }

            if (speechMs < VoiceTuning.MIN_SPEECH_MS) null else wav(pcm.toByteArray(), rate)
        }

    /**
     * Supresor de ruido y ganancia automática, si el móvil los tiene: ayudan con
     * las voces flojas o temblorosas. Si fallan, se graba igual.
     */
    private fun enableVoiceEffects(sessionId: Int): List<AudioEffect> = buildList {
        if (NoiseSuppressor.isAvailable()) {
            runCatching { NoiseSuppressor.create(sessionId) }.getOrNull()?.let {
                it.enabled = true
                add(it)
            }
        }
        if (AutomaticGainControl.isAvailable()) {
            runCatching { AutomaticGainControl.create(sessionId) }.getOrNull()?.let {
                it.enabled = true
                add(it)
            }
        }
    }

    private fun rms(samples: ShortArray, count: Int): Double {
        var sum = 0.0
        for (i in 0 until count) {
            val s = samples[i].toDouble()
            sum += s * s
        }
        return sqrt(sum / count)
    }

    private fun writeLittleEndian(out: ByteArrayOutputStream, samples: ShortArray, count: Int) {
        for (i in 0 until count) {
            val s = samples[i].toInt()
            out.write(s and 0xFF)
            out.write((s shr 8) and 0xFF)
        }
    }

    /** Cabecera WAV de 44 bytes: PCM de 16 bits, mono. */
    private fun wav(pcm: ByteArray, rate: Int): ByteArray {
        val header = ByteBuffer.allocate(WAV_HEADER_BYTES).order(ByteOrder.LITTLE_ENDIAN).apply {
            put("RIFF".toByteArray(Charsets.US_ASCII))
            putInt(36 + pcm.size)
            put("WAVE".toByteArray(Charsets.US_ASCII))
            put("fmt ".toByteArray(Charsets.US_ASCII))
            putInt(16)
            putShort(1) // PCM
            putShort(1) // mono
            putInt(rate)
            putInt(rate * 2) // bytes por segundo
            putShort(2) // bytes por muestra
            putShort(16) // bits por muestra
            put("data".toByteArray(Charsets.US_ASCII))
            putInt(pcm.size)
        }
        return header.array() + pcm
    }

    private companion object {
        const val WAV_HEADER_BYTES = 44
    }
}
