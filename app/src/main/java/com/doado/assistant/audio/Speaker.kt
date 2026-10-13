package com.doado.assistant.audio

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.view.accessibility.AccessibilityManager
import com.doado.assistant.VoiceTuning
import java.util.Locale

/**
 * La voz del asistente: el motor de voz del propio móvil, en español y algo más
 * pausado que lo normal. No sale nada a internet.
 *
 * Con TalkBack encendido no habla: TalkBack ya lee la pantalla, y dos voces a la
 * vez no las entiende nadie.
 *
 * Avisa cuando termina de hablar ([speak] con `onDone`): la conversación vuelve a
 * abrir el micrófono justo entonces, nunca antes, para no grabar su propia voz.
 */
class Speaker(context: Context) : TextToSpeech.OnInitListener {

    private val appContext = context.applicationContext
    private val main = Handler(Looper.getMainLooper())
    private val tts = TextToSpeech(appContext, this)
    private var ready = false

    /** El motor no ha arrancado: no se habla, pero la conversación no se queda esperando. */
    private var broken = false

    /** Lo que se pidió decir antes de que el motor estuviera listo. */
    private var pending: Pair<String, (() -> Unit)?>? = null

    private var utteranceCount = 0
    private var currentUtterance: String? = null
    private var currentOnDone: (() -> Unit)? = null

    override fun onInit(status: Int) {
        if (status != TextToSpeech.SUCCESS) {
            broken = true
            pending?.second?.invoke()
            pending = null
            return
        }
        val spain = tts.setLanguage(Locale.forLanguageTag("es-ES"))
        if (spain == TextToSpeech.LANG_MISSING_DATA || spain == TextToSpeech.LANG_NOT_SUPPORTED) {
            tts.setLanguage(Locale.forLanguageTag("es"))
        }
        tts.setSpeechRate(VoiceTuning.SPEECH_RATE)
        tts.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) = Unit
            override fun onDone(utteranceId: String?) = finished(utteranceId)

            @Deprecated("Solo para versiones antiguas de Android")
            override fun onError(utteranceId: String?) = finished(utteranceId)
            override fun onError(utteranceId: String?, errorCode: Int) = finished(utteranceId)
        })
        ready = true
        pending?.let { (text, onDone) -> speak(text, onDone = onDone) }
        pending = null
    }

    /**
     * Dice [text] cortando lo que estuviera diciendo.
     *
     * @param force hablar aunque TalkBack esté encendido. Solo para «Repetir»: es
     *   una petición expresa y, tras la doble pulsación, TalkBack ya ha callado.
     * @param onDone al acabar de hablar, en el hilo principal. Si no se habla (por
     *   TalkBack o porque no hay motor de voz), enseguida. Si se corta con [stop] o
     *   con otra frase, nunca.
     */
    fun speak(text: String, force: Boolean = false, onDone: (() -> Unit)? = null) {
        currentOnDone = null
        if ((!force && screenReaderOn) || broken) {
            onDone?.let { main.post(it) }
            return
        }
        if (!ready) {
            pending = text to onDone
            return
        }
        val id = "doado-asistente-${++utteranceCount}"
        currentUtterance = id
        currentOnDone = onDone
        tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, id)
    }

    fun stop() {
        pending = null
        currentOnDone = null
        currentUtterance = null
        if (ready) tts.stop()
    }

    fun shutdown() {
        stop()
        tts.shutdown()
    }

    /** TalkBack (o cualquier lector con exploración táctil) está encendido. */
    val screenReaderOn: Boolean
        get() {
            val manager = appContext.getSystemService(AccessibilityManager::class.java) ?: return false
            return manager.isEnabled && manager.isTouchExplorationEnabled
        }

    /** Llega en un hilo del motor de voz: se pasa al principal y solo cuenta la última frase. */
    private fun finished(utteranceId: String?) {
        main.post {
            if (utteranceId != currentUtterance) return@post
            val onDone = currentOnDone
            currentOnDone = null
            currentUtterance = null
            onDone?.invoke()
        }
    }
}
