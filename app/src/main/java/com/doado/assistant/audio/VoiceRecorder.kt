package com.doado.assistant.audio

import android.Manifest
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.os.SystemClock
import android.util.Log
import androidx.annotation.RequiresPermission
import com.doado.BuildConfig
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
import kotlin.math.ln
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

/**
 * Graba una orden hablada y decide cuándo ha terminado.
 *
 * Grabamos nosotros en vez de usar el reconocedor de Android porque así mandamos
 * en el corte (D-25): la frase acaba tras [VoiceTuning.END_OF_SPEECH_SILENCE_MS]
 * de silencio, al llegar al tope o cuando se pulsa «Ya he terminado».
 *
 * Distingue voz de silencio por volumen, comparando con el ruido de fondo que va
 * midiendo: con la tele puesta el umbral sube solo. Cuesta más empezar a contar
 * voz que seguir contándola, para que el final flojo de una frase no cuente como
 * silencio. Y para seguir contándola hay que sonar cerca de como sonaba quien da
 * la orden (D-58): la tele o alguien hablando al fondo, más flojos, no alargan la
 * frase. Al servicio solo va el tramo con voz y un margen: los segundos de
 * silencio y de ruido son lo que hace a Whisper inventarse frases.
 *
 * Sin supresor de ruido ni ganancia automática (D-37): la fuente de
 * reconocimiento viene sin ellos a propósito, y los dos estropeaban lo que
 * oye Whisper y el cálculo de qué es voz. El audio vive en memoria y nunca toca
 * el disco.
 */
class VoiceRecorder {

    /**
     * Graba hasta que acaba la frase. Devuelve un WAV listo para enviar, o null si
     * no ha llegado a hablar.
     *
     * @param stopRequested se pone a true con «Ya he terminado».
     * @param ignoreFirstMs lo que se tira del principio: el pitido de «habla ahora»,
     *   que suena ya con el micrófono abierto para no hacer esperar. No cuenta ni
     *   para medir el ruido ni para lo que se envía.
     * @param onLevel volumen entre 0 y 1, cada trozo, para las barras de la pantalla.
     */
    @RequiresPermission(Manifest.permission.RECORD_AUDIO)
    suspend fun record(
        stopRequested: AtomicBoolean,
        ignoreFirstMs: Long = 0L,
        onLevel: (Float) -> Unit,
    ): ByteArray? =
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
            val pcm = ByteArrayOutputStream()
            val frame = ShortArray(frameSamples)
            var elapsedMs = 0L
            var speechMs = 0L
            var silenceMs = 0L
            var noiseFloor = -1.0
            var speechLevel = -1.0
            var inSpeech = false
            var firstSpeechByte = -1
            var lastSpeechByte = 0
            var peakRms = 0.0
            // Cuándo empezó a hablar, en ms de grabación; -1 mientras no.
            var speechStartMs = -1L
            val startedAt = SystemClock.elapsedRealtime()

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
                    if (read == 0) {
                        // Sin audio, el tiempo de grabación no avanza: se mira el reloj
                        // para que «Ya he terminado» y el tope sigan cortando.
                        val wallMs = SystemClock.elapsedRealtime() - startedAt
                        if (stopRequested.get() || wallMs >= HARD_LIMIT_MS) break
                        continue
                    }

                    val frameMs = read * 1000L / rate
                    elapsedMs += frameMs
                    // Lo que coincide con el pitido no se mira ni se guarda.
                    if (elapsedMs <= ignoreFirstMs) {
                        onLevel(0f)
                        continue
                    }
                    val rms = rms(frame, read)
                    peakRms = max(peakRms, rms)

                    // Todo ceros: el micrófono está bloqueado en los ajustes rápidos de
                    // Android o de Samsung. Mejor decirlo ya que esperar diez segundos
                    // para «no te he oído».
                    if (peakRms == 0.0 && elapsedMs >= MUTED_AFTER_MS) {
                        throw AssistantError(AssistantFailure.MICROPHONE)
                    }

                    // El ruido de fondo baja deprisa y sube despacio. Si empieza a
                    // hablar nada más sonar el pitido, el primer trozo ya es voz: el
                    // primer hueco entre palabras corrige la medida. Antes, ese
                    // arranque dejaba el umbral por encima de su voz y no se la oía.
                    // Los trozos mudos del arranque del micrófono no cuentan.
                    if (rms > 0 && (noiseFloor < 0 || rms < noiseFloor)) {
                        noiseFloor = if (noiseFloor < 0) rms else (noiseFloor + rms) / 2
                    }
                    val threshold = if (inSpeech) {
                        maxOf(
                            VoiceTuning.MIN_SPEECH_RMS,
                            noiseFloor * VoiceTuning.SPEECH_CONTINUE_FACTOR,
                            speechLevel * VoiceTuning.SPEECH_DROP_RATIO,
                        )
                    } else {
                        max(VoiceTuning.MIN_SPEECH_RMS, noiseFloor * VoiceTuning.SPEECH_START_FACTOR)
                    }
                    inSpeech = noiseFloor >= 0 && rms > threshold
                    if (inSpeech) {
                        speechLevel = if (speechLevel < 0) {
                            // Si el primer trozo es un golpe, no puede dejar la medida
                            // tan alta que la voz de verdad deje de contar.
                            rms.coerceAtMost(threshold * VoiceTuning.SPEECH_LEVEL_MAX_JUMP)
                        } else {
                            val sample = rms.coerceAtMost(speechLevel * VoiceTuning.SPEECH_LEVEL_MAX_JUMP)
                            speechLevel + (sample - speechLevel) * VoiceTuning.SPEECH_LEVEL_RISE
                        }
                    }

                    val frameStart = pcm.size()
                    writeLittleEndian(pcm, frame, read)
                    if (inSpeech) {
                        speechMs += frameMs
                        silenceMs = 0
                        if (firstSpeechByte < 0) {
                            firstSpeechByte = frameStart
                            speechStartMs = elapsedMs - frameMs
                        }
                        lastSpeechByte = pcm.size()
                    } else {
                        silenceMs += frameMs
                    }
                    // Sube también durante la voz, más despacio: si no, una tele que
                    // ya sonaba al empezar contaría como voz hasta el tope.
                    if (noiseFloor >= 0 && rms > noiseFloor) {
                        val rise = if (inSpeech) VoiceTuning.NOISE_RISE_IN_SPEECH else VoiceTuning.NOISE_RISE
                        noiseFloor += (rms - noiseFloor) * rise
                    }
                    onLevel(level(rms))

                    // El tope de la orden cuenta desde que empieza a hablar: quien tarda
                    // ocho segundos en arrancar no se queda con dos para decirla.
                    val hasSpoken = speechMs >= VoiceTuning.MIN_SPEECH_MS
                    val started = speechStartMs >= 0
                    val done = stopRequested.get() ||
                        (started && elapsedMs - speechStartMs >= VoiceTuning.MAX_RECORDING_MS) ||
                        elapsedMs >= HARD_LIMIT_MS ||
                        (!started && elapsedMs >= VoiceTuning.WAIT_FOR_SPEECH_MS) ||
                        // Un ruido corto que no llega a voz: con su silencio detrás,
                        // pasado el plazo de espera, se acaba.
                        (started && !hasSpoken && elapsedMs >= VoiceTuning.WAIT_FOR_SPEECH_MS &&
                            silenceMs >= VoiceTuning.END_OF_SPEECH_SILENCE_MS) ||
                        (hasSpoken && silenceMs >= VoiceTuning.END_OF_SPEECH_SILENCE_MS)
                    if (done) break
                }
            } finally {
                runCatching { recorder.stop() }
                recorder.release()
            }

            val audio = pcm.toByteArray()
            val bytesPerMs = rate * 2 / 1000
            val from = max(0, firstSpeechByte - (VoiceTuning.PADDING_BEFORE_SPEECH_MS * bytesPerMs).toInt())
            val to = min(audio.size, lastSpeechByte + (VoiceTuning.PADDING_AFTER_SPEECH_MS * bytesPerMs).toInt())
            if (BuildConfig.DEBUG) {
                // Solo cifras, para ajustar VoiceTuning con voces reales: ni audio ni texto.
                Log.d(
                    LOG_TAG,
                    "Grabado ${elapsedMs} ms, voz ${speechMs} ms, ruido ${noiseFloor.toInt()}, " +
                        "voz media ${speechLevel.toInt()}, pico ${peakRms.toInt()}, se envían ${(to - from).coerceAtLeast(0) / bytesPerMs} ms",
                )
            }
            if (speechMs < VoiceTuning.MIN_SPEECH_MS) null else wav(audio.copyOfRange(from, to), rate)
        }

    /**
     * Volumen para la barra, en escala logarítmica, como lo oye el oído: con una
     * escala lineal, una voz floja apenas la movía y parecía que no se la oía.
     */
    private fun level(rms: Double): Float {
        if (rms <= VoiceTuning.LEVEL_EMPTY_RMS) return 0f
        val span = ln(VoiceTuning.LEVEL_FULL_RMS / VoiceTuning.LEVEL_EMPTY_RMS)
        return (ln(rms / VoiceTuning.LEVEL_EMPTY_RMS) / span).coerceIn(0.0, 1.0).toFloat()
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
        const val LOG_TAG = "DoadoVoz"

        /** Tope absoluto de una grabación: la espera a que hable más la orden entera. */
        const val HARD_LIMIT_MS = VoiceTuning.WAIT_FOR_SPEECH_MS + VoiceTuning.MAX_RECORDING_MS

        /** Tras esto oyendo solo ceros, el micrófono está bloqueado. */
        const val MUTED_AFTER_MS = 1_500L
    }
}
