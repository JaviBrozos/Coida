package com.doado.assistant

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.SystemClock
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
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import java.util.concurrent.atomic.AtomicBoolean

/**
 * El asistente de voz de principio a fin: escuchar, transcribir, interpretar y,
 * según el modo, hacerlo o explicar los pasos.
 *
 * **Una orden, una escucha** (D-56). En la prueba con dos personas mayores, la
 * pregunta de confirmación y el micrófono que se volvía a abrir solo las liaban:
 * contestaban a destiempo y se colaba el ruido de la casa. Ahora:
 *  · En «Lo hace», dice qué va a hacer («Llamo a María») y lo hace al acabar la
 *    frase. Mientras lo dice, «Cancelar» lo para.
 *  · El micrófono solo se vuelve a abrir solo cuando no ha entendido u oído la
 *    orden, para que se la repitan, y una sola vez. El resto se hace con botones.
 *  · Lo hablado se recuerda mientras está abierto: al repetir, el modelo sabe qué
 *    se había dicho antes. Se olvida al cerrar.
 *
 * Es una clase normal con el `lifecycleScope` de la Activity, no un ViewModel
 * (D-29): salir de Doado cancela todo con [cancel], y no hay recreaciones de las
 * que sobrevivir. Su estado es de Compose para que la pantalla se pinte sola.
 *
 * No ejecuta nada en el móvil: avisa con [onExecute] y es la Activity quien lanza
 * el Intent, como con cualquier toque. El número sale siempre del almacén.
 *
 * @param onExecute toca hacer la acción.
 * @param onFinish se ha cerrado el asistente; [highlightId] es lo que hay que
 *   marcar en el inicio, si se estaba guiando hacia algo.
 * @param isInForeground Doado está a la vista. Si un diálogo del sistema lo tapa
 *   justo al ir a actuar, no se actúa: no se llama a nadie sin que se vea.
 */
class AssistantController(
    context: Context,
    private val scope: CoroutineScope,
    private val speechToText: SpeechToText,
    private val interpreter: IntentInterpreter,
    private val onExecute: (AssistantAction) -> Unit,
    private val onFinish: (highlightId: String?) -> Unit,
    private val isInForeground: () -> Boolean = { true },
) {
    private val appContext = context.applicationContext
    private val res get() = appContext.resources
    private val recorder = VoiceRecorder()
    private val speaker = Speaker(appContext)

    /** El que hace el pitido de «habla ahora». Se crea la primera vez que suena. */
    private var tone: ToneGenerator? = null

    var state: AssistantState by mutableStateOf(AssistantState.Idle)
        private set

    /** Volumen de lo que está oyendo, de 0 a 1, para la barra de la pantalla. */
    var level by mutableFloatStateOf(0f)
        private set

    private var job: Job? = null
    private val stopRequested = AtomicBoolean(false)
    private var mode = AssistantMode.ACT
    private var catalog: ActionCatalog? = null

    /** Lo hablado en esta conversación. Solo en memoria; se borra al cerrar. */
    private val history = ArrayDeque<ChatTurn>()

    /** Lo último que dijo, para calcular cuánto tarda TalkBack en leerlo. */
    private var lastSaid = ""

    /**
     * Veces seguidas que no ha entendido u oído la orden. La primera pide que se la
     * repitan y vuelve a escuchar; a partir de [MAX_MISUNDERSTANDINGS] se calla y
     * espera a que pulse, para no quedarse oyendo la tele sin fin.
     */
    private var misunderstandings = 0

    /** Empieza una conversación nueva escuchando la primera orden. */
    fun start(mode: AssistantMode, catalog: ActionCatalog) {
        cancel()
        this.mode = mode
        this.catalog = catalog
        misunderstandings = 0
        listen()
    }

    /**
     * «Probar otra vez» o «Hablar otra vez»: escucha otra orden sin olvidar lo
     * hablado, y vuelve a dejar que pida una repetición.
     */
    fun retry() {
        misunderstandings = 0
        listen()
    }

    /** «Ya he terminado»: corta la grabación y sigue con lo grabado. */
    fun finishSpeaking() = stopRequested.set(true)

    /** «Cancelar» mientras dice lo que va a hacer: no se hace nada. */
    fun cancelAction() {
        if (state !is AssistantState.Acting) return
        job?.cancel()
        job = null
        speaker.stop()
        answer(res.getString(R.string.assistant_declined))
    }

    fun choose(option: ClarifyOption) {
        if (state !is AssistantState.Clarify) return
        act(option.action, heard = null)
    }

    fun nextStep() {
        val guide = state as? AssistantState.Guide ?: return
        if (guide.isLast) return
        show(guide.copy(index = guide.index + 1))
    }

    /** «Repetir» habla siempre, también con TalkBack: es una petición expresa. */
    fun repeat() {
        val text = when (val current = state) {
            is AssistantState.Guide -> spokenStep(current)
            is AssistantState.Clarify -> current.question
            is AssistantState.Answer -> current.text
            is AssistantState.Failed -> res.getString(current.messageRes)
            else -> return
        }
        lastSaid = text
        speaker.speak(text, force = true)
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
        history.clear()
        state = AssistantState.Idle
    }

    /** Cuando la Activity se destruye: suelta el motor de voz y el del pitido. */
    fun release() {
        cancel()
        speaker.shutdown()
        tone?.release()
        tone = null
    }

    // --- Escuchar -------------------------------------------------------------

    private fun listen() {
        if (catalog == null) return
        job?.cancel()
        // Callado mientras escucha: su propia voz se grabaría con la orden.
        speaker.stop()
        level = 0f
        state = AssistantState.Listening

        job = scope.launch {
            guard {
                val wav = recordOrder()
                level = 0f
                if (wav == null) fail(AssistantFailure.NOT_HEARD) else understand(wav)
            }
        }
    }

    /** Cualquier fallo acaba en una frase llana; nunca en una pantalla colgada. */
    private suspend fun guard(block: suspend () -> Unit) {
        try {
            block()
        } catch (e: CancellationException) {
            throw e
        } catch (e: AssistantError) {
            fail(e.failure)
        } catch (_: Exception) {
            fail(AssistantFailure.UNAVAILABLE)
        }
    }

    /** De la voz a lo que toca hacer. */
    private suspend fun understand(wav: ByteArray) {
        val catalog = catalog ?: return
        state = AssistantState.Thinking(heard = null)
        val heard = speechToText.transcribe(wav, catalog.vocabulary).trim()
        if (heard.length < MIN_TRANSCRIPT_CHARS) {
            // Ruido en vez de palabras: como si no hubiera oído nada.
            fail(AssistantFailure.NOT_HEARD)
            return
        }
        state = AssistantState.Thinking(heard)
        val raw = interpreter.interpret(Prompts.system(catalog), history.toList(), heard)
        remember(ChatTurn(fromUser = true, text = heard))
        handle(catalog.parse(raw), heard)
    }

    /** Graba con un pitido delante, y solo si hay permiso. */
    private suspend fun recordOrder(): ByteArray? {
        if (ContextCompat.checkSelfPermission(appContext, Manifest.permission.RECORD_AUDIO) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            // El permiso se pide en los ajustes, con el familiar delante: aquí solo se dice.
            throw AssistantError(AssistantFailure.MICROPHONE)
        }
        if (speaker.screenReaderOn) delay(screenReaderGrace())
        // El pitido suena con el micrófono ya abriéndose, y no antes: esperar a que
        // acabara y luego arrancar el micrófono se notaba lento. Lo que coincide con
        // el pitido se tira al grabar.
        readyBeep()
        return try {
            recorder.record(
                stopRequested.also { it.set(false) },
                ignoreFirstMs = VoiceTuning.READY_BEEP_MS + VoiceTuning.READY_BEEP_TAIL_MS,
            ) { value -> scope.launch { level = value } }
        } catch (e: SecurityException) {
            throw AssistantError(AssistantFailure.MICROPHONE, e)
        }
    }

    /** Con TalkBack: lo que tarda en leer lo último que se dijo, para no grabarlo. */
    private fun screenReaderGrace(): Long =
        (lastSaid.length * VoiceTuning.SCREEN_READER_MS_PER_CHAR)
            .coerceIn(VoiceTuning.SCREEN_READER_GRACE_MS, VoiceTuning.SCREEN_READER_MAX_GRACE_MS)

    /**
     * Pitido corto de «habla ahora», sin esperar a que acabe. Al repetir, es lo que
     * avisa de que el micrófono se ha vuelto a abrir. El generador se crea una vez y
     * se guarda: crearlo en cada escucha también retrasaba el micrófono.
     */
    private fun readyBeep() {
        val generator = tone
            ?: runCatching { ToneGenerator(AudioManager.STREAM_MUSIC, READY_BEEP_VOLUME) }.getOrNull()
                .also { tone = it }
        generator?.startTone(ToneGenerator.TONE_PROP_BEEP, VoiceTuning.READY_BEEP_MS)
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

            // Entre dos personas o dos casillas no se adivina: un botón por cada una.
            is Interpretation.Clarify -> {
                val allContacts = result.options.none { it is AssistantAction.OpenApp }
                val question = res.getString(
                    if (allContacts) R.string.assistant_clarify_contact else R.string.assistant_clarify_app,
                )
                state = AssistantState.Clarify(question, result.options.map { ClarifyOption(labelOf(it), it) }, heard)
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

    /** Según el modo: decirlo y hacerlo (D-56) o explicar cómo (D-27). */
    private fun act(action: AssistantAction, heard: String?) {
        if (mode == AssistantMode.GUIDE) {
            val plan = GuideSteps.forAction(res, action, catalog ?: return)
            show(AssistantState.Guide(intro = null, steps = plan.steps, index = 0, highlightId = plan.highlightId))
            return
        }
        val text = when (action) {
            is AssistantAction.CallContact -> res.getString(R.string.assistant_acting_call, action.contact.name)
            is AssistantAction.WhatsAppContact ->
                res.getString(R.string.assistant_acting_whatsapp, action.contact.name)
            is AssistantAction.OpenApp -> res.getString(R.string.assistant_acting_open, action.app.label)
        }
        val lastHeard = heard
            ?: (state as? AssistantState.Thinking)?.heard
            ?: (state as? AssistantState.Clarify)?.heard.orEmpty()
        val acting = AssistantState.Acting(action, text, lastHeard)
        state = acting
        lastSaid = text
        // En la conversación, para que tras «Cancelar» el modelo sepa qué se canceló
        // y entienda «no, a la otra».
        remember(ChatTurn(fromUser = false, text = text))
        job?.cancel()
        job = scope.launch {
            // Se hace al acabar la frase, para que dé tiempo a oír a quién llama y a
            // pulsar «Cancelar» si no es. Si el motor de voz se cuelga, no más del tope.
            val started = SystemClock.uptimeMillis()
            val spoken = CompletableDeferred<Unit>()
            speaker.speak(text) { spoken.complete(Unit) }
            withTimeoutOrNull(VoiceTuning.ACT_MAX_WAIT_MS) { spoken.await() }
            // Callado (TalkBack, o sin motor de voz), la frase acaba al instante:
            // nunca se actúa antes de un mínimo, y con TalkBack, además, lo que tarda
            // en leerla y en llegar a «Cancelar».
            val window = if (speaker.screenReaderOn) {
                screenReaderGrace() + VoiceTuning.ACT_SCREEN_READER_EXTRA_MS
            } else {
                VoiceTuning.ACT_MIN_WINDOW_MS
            }
            val waited = SystemClock.uptimeMillis() - started
            delay(maxOf(VoiceTuning.ACT_AFTER_SPEECH_MS, window - waited))
            if (state !== acting) return@launch
            if (!isInForeground()) {
                cancelAction()
                return@launch
            }
            state = AssistantState.Idle
            history.clear()
            onExecute(action)
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

    /**
     * Si no ha entendido u oído la orden, pide que se la repitan y vuelve a escuchar,
     * una vez. Si falta red, clave o micrófono, reabrir el micrófono no arregla nada.
     */
    private fun fail(failure: AssistantFailure) {
        level = 0f
        val canRepeat = failure == AssistantFailure.NOT_UNDERSTOOD || failure == AssistantFailure.NOT_HEARD
        val listensAgain = canRepeat && ++misunderstandings < MAX_MISUNDERSTANDINGS
        val failed = AssistantState.Failed(failure, listensAgain)
        state = failed
        val text = res.getString(failed.messageRes)
        if (!listensAgain) {
            say(text)
            return
        }
        lastSaid = text
        remember(ChatTurn(fromUser = false, text = text))
        job?.cancel()
        job = scope.launch {
            // Como al actuar: si el motor de voz no avisa de que ha acabado, no más
            // del tope, para no quedarse pidiendo «repítemelo» con el micrófono
            // cerrado. Callado, la frase acaba al instante y no llegaba ni a verse:
            // antes de volver a escuchar, el tiempo de leerla.
            val spoken = CompletableDeferred<Unit>()
            speaker.speak(text) { spoken.complete(Unit) }
            withTimeoutOrNull(VoiceTuning.ACT_MAX_WAIT_MS) { spoken.await() }
            if (speaker.isSilent) delay(screenReaderGrace())
            if (state === failed) listen()
        }
    }

    /**
     * Lo dice en voz alta y lo apunta en la conversación. [then], al acabar de
     * hablar y solo si la pantalla sigue siendo la misma.
     */
    private fun say(text: String, then: (() -> Unit)? = null) {
        lastSaid = text
        remember(ChatTurn(fromUser = false, text = text))
        val shown = state
        speaker.speak(text) { if (state === shown) then?.invoke() }
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

    private companion object {
        /** Una transcripción más corta que esto es ruido, no una orden. */
        const val MIN_TRANSCRIPT_CHARS = 2

        /** Volumen del pitido de «habla ahora», sobre 100. */
        const val READY_BEEP_VOLUME = 80

        /** Turnos de la conversación que se le recuerdan al modelo (cada uno, pregunta y respuesta). */
        const val MAX_HISTORY_TURNS = 6

        /** Fallos seguidos tras los que deja de reabrir el micrófono solo: con 2, repite una vez. */
        const val MAX_MISUNDERSTANDINGS = 2
    }
}

/**
 * La frase llana de un fallo: dice qué hacer ahora, no qué ha pasado por dentro.
 * Si va a volver a escuchar, pide que se lo repitan; si no, dice qué pulsar.
 */
@get:StringRes
val AssistantState.Failed.messageRes: Int
    get() = when (failure) {
        AssistantFailure.NOT_HEARD ->
            if (listensAgain) R.string.assistant_failure_not_heard else R.string.assistant_failure_not_heard_again
        AssistantFailure.NOT_UNDERSTOOD ->
            if (listensAgain) R.string.assistant_failure_not_understood
            else R.string.assistant_failure_not_understood_again
        AssistantFailure.NO_INTERNET -> R.string.assistant_failure_no_internet
        AssistantFailure.BUSY -> R.string.assistant_failure_busy
        AssistantFailure.UNAVAILABLE -> R.string.assistant_failure_unavailable
        AssistantFailure.MICROPHONE -> R.string.assistant_failure_microphone
    }
