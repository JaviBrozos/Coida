package com.doado.assistant.cloud

/**
 * Interpreta lo que ha dicho la persona mayor con un modelo de lenguaje.
 *
 * Devuelve el texto tal cual, que debería ser un objeto JSON con la forma que pide
 * [com.doado.assistant.Prompts]. Fiarse de él no es asunto de esta interfaz: lo
 * valida [com.doado.assistant.ActionCatalog].
 */
interface IntentInterpreter {
    /**
     * @param history lo hablado antes en esta misma conversación, de lo más antiguo
     *   a lo más reciente, para entender «no, mejor a Luis». Vive solo en memoria.
     * @throws com.doado.assistant.AssistantError si falla la red o el servicio.
     */
    suspend fun interpret(systemPrompt: String, history: List<ChatTurn>, userMessage: String): String
}

/** Una frase de la conversación: de la persona mayor, o lo que el móvil le dijo en voz alta. */
data class ChatTurn(val fromUser: Boolean, val text: String)
