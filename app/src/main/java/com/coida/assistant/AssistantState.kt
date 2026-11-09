package com.coida.assistant

import com.coida.data.ContactShortcut
import com.coida.data.FavoriteApp

/**
 * Lo único que el asistente puede hacer en el móvil (lista blanca).
 *
 * Cada acción lleva el contacto o la casilla **guardados en Coida**, nunca algo
 * que haya dicho el modelo: el número de teléfono sale siempre del almacén.
 */
sealed interface AssistantAction {
    data class CallContact(val contact: ContactShortcut) : AssistantAction
    data class WhatsAppContact(val contact: ContactShortcut) : AssistantAction
    data class OpenApp(val app: FavoriteApp) : AssistantAction
}

/** Por qué no ha salido bien. Cada motivo tiene su frase llana en strings.xml. */
enum class AssistantFailure {
    /** No ha habido voz, o no se ha entendido ninguna palabra. */
    NOT_HEARD,

    /** Se ha oído, pero no sabemos qué quiere. */
    NOT_UNDERSTOOD,

    /** Sin conexión, o tan lenta que no llega. */
    NO_INTERNET,

    /** El servicio ha dicho que esperemos (límite de peticiones). */
    BUSY,

    /** El servicio no está configurado o la clave no vale: lo arregla el familiar. */
    UNAVAILABLE,

    /** El micrófono no se puede usar: sin permiso o lo tiene otra app. */
    MICROPHONE,
}

/** Fallo con su motivo, para que el controlador sepa qué frase enseñar. */
class AssistantError(val failure: AssistantFailure, cause: Throwable? = null) :
    Exception(failure.name, cause)

/** Lo que la pantalla del asistente tiene que enseñar en cada momento. */
sealed interface AssistantState {

    data object Idle : AssistantState

    /** Grabando. No se habla mientras: la voz del móvil se grabaría con la orden. */
    data object Listening : AssistantState

    /** Transcribiendo o interpretando. [heard] es lo que ha entendido, si ya se sabe. */
    data class Thinking(val heard: String?) : AssistantState

    /**
     * Modo «Lo hace»: dice qué va a hacer («Llamo a María») y, al acabar la frase,
     * lo hace, sin preguntar (D-56). Hasta entonces se puede cancelar.
     */
    data class Acting(
        val action: AssistantAction,
        val text: String,
        val heard: String,
    ) : AssistantState

    /**
     * Pasos de uno en uno. [highlightId] es la casilla o el contacto que se marca en
     * el inicio al terminar; nulo cuando los pasos no son sobre algo de Coida.
     */
    data class Guide(
        val intro: String?,
        val steps: List<String>,
        val index: Int,
        val highlightId: String?,
    ) : AssistantState {
        val isLast: Boolean get() = index >= steps.lastIndex
    }

    /** Hay varias opciones posibles: un botón grande por cada una. */
    data class Clarify(
        val question: String,
        val options: List<ClarifyOption>,
        /** Lo que dijo, para seguir enseñándolo tras elegir una opción. */
        val heard: String = "",
    ) : AssistantState

    /** Una frase y nada más: una negativa, una respuesta corta, un «vale, no hago nada». */
    data class Answer(val text: String) : AssistantState

    /**
     * No ha salido bien. Con [listensAgain], el móvil pide que se lo repita y vuelve
     * a escuchar solo; si no, se queda esperando a que se pulse.
     */
    data class Failed(val failure: AssistantFailure, val listensAgain: Boolean = false) : AssistantState
}

/** Una de las opciones al aclarar, con el texto de su botón. */
data class ClarifyOption(val label: String, val action: AssistantAction)
