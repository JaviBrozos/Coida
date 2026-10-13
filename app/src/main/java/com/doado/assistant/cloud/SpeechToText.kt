package com.doado.assistant.cloud

/**
 * Pasa la voz a texto. Es la mitad del contrato con un proveedor: para cambiar a
 * uno que no hable el formato de OpenAI basta con implementar esta interfaz y
 * [IntentInterpreter].
 */
interface SpeechToText {
    /**
     * @param wav la orden grabada, en WAV de 16 kHz.
     * @param vocabulary nombres que probablemente salgan (contactos, casillas), para
     *   que acierte con «Maripili» o «Sofascore».
     * @return lo que ha entendido; vacío si no ha entendido nada.
     * @throws com.doado.assistant.AssistantError si falla la red o el servicio.
     */
    suspend fun transcribe(wav: ByteArray, vocabulary: List<String>): String
}
