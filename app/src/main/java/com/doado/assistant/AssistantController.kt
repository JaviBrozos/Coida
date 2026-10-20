package com.doado.assistant

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioManager
import android.media.ToneGenerator
import androidx.annotation.StringRes
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import com.doado.R
import com.doado.assistant.audio.Speaker
import com.doado.assistant.audio.VoiceRecorder
import com.doado.assistant.cloud.ChatTurn
import com.doado.assistant.cloud.IntentInterpreter
import com.doado.assistant.cloud.SpeechToText
import com.doado.data.AssistantMode
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicBoolean

/**
 * El asistente de voz de principio a fin: escuchar, transcribir, interpretar y,
 * según el modo, preguntar antes de actuar o explicar los pasos.
 *
 * Es una **conversación** (D-31): cuando el móvil acaba de hablar, vuelve a abrir
 * el micrófono solo, sin quitar de la pantalla lo que acaba de decir, para que se
 * le pueda contestar hablando. «Sí», «no», «siguiente» o «gracias» se entienden
 * aquí mismo ([VoiceReplies]); lo demás va al modelo con lo hablado antes, que se
 * olvida al cerrar. Si no oye nada, apaga el micrófono y espera, sin error.
 *
 * Es una clase normal con el `lifecycleScope` de la Activity, no un ViewModel
 * (D-29): salir de Doado cancela todo con [cancel], y no hay recreaciones de las
 * que sobrevivir. Su estado es de Compose para que la pantalla se pinte sola.
 *
 * No ejecuta nada en el móvil: cuando hay un «Sí», pulsado o dicho, avisa con
 * [onExecute] y es la Activity quien lanza el Intent, como con cualquier toque.
 *
 * @param onExecute la persona mayor ha confirmado la acción.
 * @param onFinish se ha cerrado el asistente; [highlightId] es lo que hay que
 *   marcar en el inicio, si se estaba guiando hacia algo.
 */
class AssistantController(
    context: Context,
    private val scope: CoroutineScope,
    private val speechToText: SpeechToText,
    private val interpreter: IntentInterpreter,
    private val onExecute: (AssistantAction) -> Unit,
    private val onFinish: (highlightId: String?) -> Unit,
) {
    private val appContext = context.applicationContext
    private val res get() = appContext.resources
    private val recorder = VoiceRecorder()
    private val speaker = Speaker(appContext)

    var state: AssistantState by mutableStateOf(AssistantState.Idle)
        private set

    /** Volumen de lo que está oyendo, de 0 a 1, para la barra de la pantalla. */
    var level by mutableFloatStateOf(0f)
        private set

    /**
     * Escuchando la respuesta con la pregunta todavía en pantalla. La primera orden
     * no cuenta: esa tiene su propio estado, [AssistantState.Listening].
     */
    var followUpListening by mutableStateOf(false)
        private set

    private var job: Job? = null
    private val stopRequested = AtomicBoolean(false)
    private var mode = AssistantMode.GUIDE
    private var catalog: ActionCatalog? = null

    /** Lo hablado en esta conversación. Solo en memoria; se borra al cerrar. */
    private val history = ArrayDeque<ChatTurn>()

    /** Lo último que dijo, para calcular cuánto tarda TalkBack en leerlo. */
    private var lastSaid = ""

    /**
     * «No te he entendido» seguidos. Con la tele de fondo, el ruido se transcribe,
     * no se entiende y el micrófono se reabriría sin fin; a partir de
     * [MAX_MISUNDERSTANDINGS] se calla y espera a que pulse.
     */
    private var misunderstandings = 0

    /** Empieza una conversación nueva escuchando la primera orden. */
    fun start(mode: AssistantMode, catalog: ActionCatalog) {
        cancel()
        this.mode = mode
        this.catalog = catalog
        misunderstandings = 0
        listenFirst()
    }

    /** «Probar otra vez» tras un fallo: como la primera orden, sin olvidar lo hablado. */
    fun retry() = listenFirst()

    /** «Hablar otra vez»: reabre el micrófono sin cambiar de pantalla. */
    fun speakAgain() = listenFollowUp()

    /** «Ya he terminado»: corta la grabación y sigue con lo grabado. */
    fun finishSpeaking() = stopRequested.set(true)

    fun confirm() {
        val confirm = state as? AssistantState.Confirm ?: return
        stopFollowUp()
        speaker.stop()
        state = AssistantState.Idle
        history.clear()
        onExecute(confirm.action)
    }

    fun decline() {
        if (state !is AssistantState.Confirm) return
        stopFollowUp()
        answer(res.getString(R.string.assistant_declined))
    }

    fun choose(option: ClarifyOption) {
        if (state !is AssistantState.Clarify) return
        stopFollowUp()
        act(option.action, heard = null)
    }

    fun nextStep() {
        val guide = state as? AssistantState.Guide ?: return
        if (guide.isLast) return
        stopFollowUp()
        show(guide.copy(index = guide.index + 1))
    }

    /** «Repetir» habla siempre, también con TalkBack: es una petición expresa. */
    fun repeat() {
        val text = when (val current = state) {
            is AssistantState.Guide -> spokenStep(current)
            is AssistantState.Confirm -> current.question
            is AssistantState.Clarify -> current.question
            is AssistantState.Answer -> current.text
            is AssistantState.Failed -> res.getString(current.failure.messageRes)
            else -> return
        }
        stopFollowUp()
        val shown = state
        lastSaid = text
        speaker.speak(text, force = true) { if (state === shown) listenFollowUp() }
    }

    /** «Terminar» o «Entendido»: se cierra el asistente y, si guiaba, se marca el botón. */
    fun finish() {
        val highlightId = (state as? AssistantState.Guide)?.highlightId
        cancel()
        onFinish(highlightId)
    }

    /**
     * Deja de escuchar, de pensar y de hablar, y olvida la conversación. Al salir
     * de Doado, al pulsar Cancelar o ATRÁS.
     */
    fun cancel() {
        job?.cancel()
        job = null
        speaker.stop()
        level = 0f
        followUpListening = false
        history.clear()
        state = AssistantState.Idle
    }

    /** Cuando la Activity se destruye: suelta el motor de voz. */
    fun release() {
        cancel()
        speaker.shutdown()
    }

    // --- Escuchar -------------------------------------------------------------

    private fun listenFirst() {
        if (catalog == null) return
        job?.cancel()
        // Callado mientras escucha: su propia voz se grabaría con la orden.
        speaker.stop()
        followUpListening = false
        level = 0f
        state = AssistantState.Listening

        job = scope.launch {
            guard {
                val wav = recordOrder()
                level = 0f
                if (wav == null) fail(AssistantFailure.NOT_HEARD) else understand(wav, previous = null)
            }
        }
    }

    /**
     * Reabre el micrófono con la pantalla como está. Si no oye nada, lo apaga y no
     * pasa nada más: la pregunta sigue ahí, con sus botones y «Hablar otra vez».
     */
    private fun listenFollowUp() {
        if (catalog == null || job?.isActive == true) return
        speaker.stop()
        val previous = state
        followUpListening = true
        level = 0f

        job = scope.launch {
            guard {
                val wav = recordOrder(VoiceTuning.MIN_REPLY_SPEECH_MS)
                followUpListening = false
                level = 0f
                if (wav != null) understand(wav, previous)
            }
        }
    }

    private fun stopFollowUp() {
        if (!followUpListening) return
        job?.cancel()
        job = null
        followUpListening = false
        level = 0f
    }

    /** Cualquier fallo acaba en una frase llana; nunca en una pantalla colgada. */
    private suspend fun guard(block: suspend () -> Unit) {
        try {
            block()
        } catch (e: CancellationException) {
            throw e
        } catch (e: AssistantError) {
            followUpListening = false
            fail(e.failure)
        } catch (_: Exception) {
            followUpListening = false
            fail(AssistantFailure.UNAVAILABLE)
        }
    }

    /**
     * De la voz a lo que toca hacer. [previous] es la pantalla a la que se contesta
     * (la pregunta, el paso...), o null si es la primera orden.
     */
    private suspend fun understand(wav: ByteArray, previous: AssistantState?) {
        val catalog = catalog ?: return
        // En una respuesta, la pregunta sigue en pantalla mientras se transcribe: un
        // «sí» se resuelve aquí y no tiene sentido hacerla parpadear.
        if (previous == null) state = AssistantState.Thinking(heard = null)
        val heard = speechToText.transcribe(wav, catalog.vocabulary).trim()
        if (heard.length < MIN_TRANSCRIPT_CHARS) {
            // Ruido en vez de palabras: en una respuesta se vuelve a la pregunta sin
            // más; en la primera orden, se dice.
            if (previous != null) state = previous else fail(AssistantFailure.NOT_HEARD)
            return
        }
        if (previous != null && answeredHere(previous, heard)) return

        state = AssistantState.Thinking(heard)
        val raw = interpreter.interpret(Prompts.system(catalog), history.toList(), heard)
        remember(ChatTurn(fromUser = true, text = heard))
        handle(catalog.parse(raw), heard)
    }

    /**
     * Respuestas cortas que se entienden sin la IA. Devuelve true si la ha
     * resuelto. Antes de actuar se repone la pantalla a la que contestaba, porque
     * las acciones comprueban en qué estado están.
     */
    private fun answeredHere(previous: AssistantState, heard: String): Boolean {
        val handled: () -> Unit = when (previous) {
            is AssistantState.Confirm -> when (VoiceReplies.confirmation(heard, targetName(previous.action))) {
                VoiceReplies.Reply.YES -> ::confirm
                VoiceReplies.Reply.NO -> ::decline
                else -> return false
            }

            is AssistantState.Guide -> when (VoiceReplies.guide(heard, previous.isLast)) {
                VoiceReplies.Reply.NEXT -> ::nextStep
                VoiceReplies.Reply.REPEAT -> ::repeat
                VoiceReplies.Reply.DONE -> ::finish
                else -> return false
            }

            is AssistantState.Clarify -> {
                val option = VoiceReplies.option(heard, previous.options) ?: return false
                ({ choose(option) })
            }

            is AssistantState.Answer, is AssistantState.Failed ->
                if (VoiceReplies.isClosing(heard)) ::finish else return false

            else -> return false
        }
        state = previous
        misunderstandings = 0
        remember(ChatTurn(fromUser = true, text = heard))
        handled()
        return true
    }

    /**
     * Graba con un pitido delante, y solo si hay permiso. [minSpeechMs] baja al
     * contestar a una pregunta, para que un «no» seco cuente (D-42).
     */
    private suspend fun recordOrder(minSpeechMs: Long = VoiceTuning.MIN_SPEECH_MS): ByteArray? {
        if (ContextCompat.checkSelfPermission(appContext, Manifest.permission.RECORD_AUDIO) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            // El permiso se pide en los ajustes, con el familiar delante: aquí solo se dice.
            throw AssistantError(AssistantFailure.MICROPHONE)
        }
        if (speaker.screenReaderOn) delay(screenReaderGrace())
        readyBeep()
        return try {
            recorder.record(stopRequested.also { it.set(false) }, minSpeechMs) { value -> scope.launch { level = value } }
        } catch (e: SecurityException) {
            throw AssistantError(AssistantFailure.MICROPHONE, e)
        }
    }

    /** Con TalkBack: lo que tarda en leer lo último que se dijo, para no grabarlo. */
    private fun screenReaderGrace(): Long =
        (lastSaid.length * VoiceTuning.SCREEN_READER_MS_PER_CHAR)
            .coerceIn(VoiceTuning.SCREEN_READER_GRACE_MS, VoiceTuning.SCREEN_READER_MAX_GRACE_MS)

    /**
     * Pitido corto de «habla ahora». En una conversación es lo que avisa de que el
     * micrófono se ha vuelto a abrir. Tampoco debe grabarse: es lo primero que
     * mediría el ruido de fondo.
     */
    private suspend fun readyBeep() {
        val tone = runCatching { ToneGenerator(AudioManager.STREAM_MUSIC, READY_BEEP_VOLUME) }.getOrNull()
        try {
            tone?.startTone(ToneGenerator.TONE_PROP_BEEP, VoiceTuning.READY_BEEP_MS)
            delay(VoiceTuning.READY_BEEP_MS + 150L)
        } finally {
            tone?.release()
        }
    }

    // --- Contestar ------------------------------------------------------------

    private fun handle(result: Interpretation, heard: String) {
        if (result != Interpretation.NotUnderstood) misunderstandings = 0
        when (result) {
            is Interpretation.Act -> act(result.action, heard)

            is Interpretation.Explain ->
                if (result.steps.isEmpty()) {
                    answer(result.speech ?: res.getString(R.string.assistant_refuse_default))
                } else {
                    show(AssistantState.Guide(result.speech, result.steps, index = 0, highlightId = null))
                }

            is Interpretation.Clarify -> {
                val allContacts = result.options.none { it is AssistantAction.OpenApp }
                val question = res.getString(
                    if (allContacts) R.string.assistant_clarify_contact else R.string.assistant_clarify_app,
                )
                state = AssistantState.Clarify(question, result.options.map { ClarifyOption(labelOf(it), it) })
                say(question)
            }

            is Interpretation.Refuse ->
                answer(result.speech ?: res.getString(R.string.assistant_refuse_default))

            is Interpretation.Missing -> answer(
                res.getString(
                    R.string.assistant_missing_app,
                    result.app?.label ?: res.getString(R.string.contact_action_whatsapp),
                ),
            )

            Interpretation.NotUnderstood -> fail(AssistantFailure.NOT_UNDERSTOOD)
        }
    }

    /** Según el modo: preguntar antes de hacerlo (D-26) o explicar cómo (D-27). */
    private fun act(action: AssistantAction, heard: String?) {
        if (mode == AssistantMode.ACT) {
            val (question, yes) = when (action) {
                is AssistantAction.CallContact ->
                    res.getString(R.string.assistant_confirm_call, action.contact.name) to
                        res.getString(R.string.assistant_yes_call)

                is AssistantAction.WhatsAppContact ->
                    res.getString(R.string.assistant_confirm_whatsapp, action.contact.name) to
                        res.getString(R.string.assistant_yes_open)

                is AssistantAction.OpenApp ->
                    res.getString(R.string.assistant_confirm_open, action.app.label) to
                        res.getString(R.string.assistant_yes_open)
            }
            val lastHeard = heard ?: (state as? AssistantState.Thinking)?.heard.orEmpty()
            state = AssistantState.Confirm(action, question, yes, lastHeard)
            say(question)
        } else {
            val plan = GuideSteps.forAction(res, action, catalog ?: return)
            show(AssistantState.Guide(intro = null, steps = plan.steps, index = 0, highlightId = plan.highlightId))
        }
    }

    private fun show(guide: AssistantState.Guide) {
        state = guide
        say(spokenStep(guide))
    }

    private fun spokenStep(guide: AssistantState.Guide): String {
        val step = guide.steps[guide.index]
        val numbered = if (guide.steps.size == 1) step
        else res.getString(R.string.assistant_step_spoken, guide.index + 1, guide.steps.size, step)
        return if (guide.index == 0 && guide.intro != null) "${guide.intro} $numbered" else numbered
    }

    private fun answer(text: String) {
        state = AssistantState.Answer(text)
        say(text)
    }

    private fun fail(failure: AssistantFailure) {
        level = 0f
        state = AssistantState.Failed(failure)
        // Si no la ha entendido, se le deja contestar, pero no sin fin; si falta
        // red, clave o micrófono, reabrir el micrófono no arregla nada.
        val listenAgain = failure == AssistantFailure.NOT_UNDERSTOOD &&
            ++misunderstandings < MAX_MISUNDERSTANDINGS
        say(res.getString(failure.messageRes), listenAfter = listenAgain)
    }

    /**
     * Lo dice en voz alta, lo apunta en la conversación y, al acabar de hablar,
     * reabre el micrófono si la pantalla sigue siendo la misma.
     */
    private fun say(text: String, listenAfter: Boolean = true) {
        lastSaid = text
        remember(ChatTurn(fromUser = false, text = text))
        val shown = state
        speaker.speak(text) { if (listenAfter && state === shown) listenFollowUp() }
    }

    private fun remember(turn: ChatTurn) {
        history.addLast(turn)
        while (history.size > MAX_HISTORY_TURNS * 2) history.removeFirst()
    }

    private fun labelOf(action: AssistantAction): String = when (action) {
        is AssistantAction.CallContact -> action.contact.name
        is AssistantAction.WhatsAppContact -> action.contact.name
        is AssistantAction.OpenApp -> action.app.label
    }

    private fun targetName(action: AssistantAction): String = labelOf(action)

    private companion object {
        /** Una transcripción más corta que esto es ruido, no una orden. */
        const val MIN_TRANSCRIPT_CHARS = 2

        /** Volumen del pitido de «habla ahora», sobre 100. */
        const val READY_BEEP_VOLUME = 80

        /** Turnos de la conversación que se le recuerdan al modelo (cada uno, pregunta y respuesta). */
        const val MAX_HISTORY_TURNS = 6

        /** Malentendidos seguidos tras los que deja de reabrir el micrófono solo. */
        const val MAX_MISUNDERSTANDINGS = 2
    }
}

/** La frase llana de cada fallo: dice qué hacer ahora, no qué ha pasado por dentro. */
@get:StringRes
val AssistantFailure.messageRes: Int
    get() = when (this) {
        AssistantFailure.NOT_HEARD -> R.string.assistant_failure_not_heard
        AssistantFailure.NOT_UNDERSTOOD -> R.string.assistant_failure_not_understood
        AssistantFailure.NO_INTERNET -> R.string.assistant_failure_no_internet
        AssistantFailure.BUSY -> R.string.assistant_failure_busy
        AssistantFailure.UNAVAILABLE -> R.string.assistant_failure_unavailable
        AssistantFailure.MICROPHONE -> R.string.assistant_failure_microphone
    }
