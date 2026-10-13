package com.doado.assistant.cloud

import com.doado.assistant.AssistantError
import com.doado.assistant.AssistantFailure
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runInterruptible
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.util.Locale
import java.util.UUID

/**
 * Cliente para cualquier proveedor que hable el formato de OpenAI: Groq, que es
 * con el que se prueba, pero también OpenAI, Mistral, OpenRouter o Gemini de pago
 * (D-25). Usa dos rutas: `/audio/transcriptions` y `/chat/completions`.
 *
 * Sin dependencias (D-30): `HttpURLConnection` y `org.json`, que trae Android. El
 * envío del audio se monta a mano en multipart.
 *
 * Lo que sale del móvil es el audio, la transcripción y lo que diga el prompt
 * (nombres e ids cortos). Nunca números de teléfono.
 */
class OpenAiCompatibleClient(private val config: AssistantConfig) : SpeechToText, IntentInterpreter {

    override suspend fun transcribe(wav: ByteArray, vocabulary: List<String>): String =
        runInterruptible(Dispatchers.IO) {
            val boundary = "doado-" + UUID.randomUUID()
            val body = ByteArrayOutputStream().apply {
                field(boundary, "model", config.speechModel)
                field(boundary, "language", "es")
                field(boundary, "response_format", "json")
                field(boundary, "temperature", "0")
                // Whisper usa el prompt como «lo que se dijo antes»: con los nombres
                // delante, acierta con los que no están en el diccionario.
                vocabularyPrompt(vocabulary)?.let { field(boundary, "prompt", it) }
                file(boundary, "file", "orden.wav", "audio/wav", wav)
                write("--$boundary--\r\n".toByteArray())
            }.toByteArray()

            val response = post(
                path = "/audio/transcriptions",
                contentType = "multipart/form-data; boundary=$boundary",
                body = body,
            )
            val text = parse { JSONObject(response).optString("text") }.trim()
            if (isSilenceHallucination(text)) "" else text
        }

    override suspend fun interpret(systemPrompt: String, history: List<ChatTurn>, userMessage: String): String =
        runInterruptible(Dispatchers.IO) {
            val messages = JSONArray().put(JSONObject().put("role", "system").put("content", systemPrompt))
            history.forEach { turn ->
                messages.put(
                    JSONObject()
                        .put("role", if (turn.fromUser) "user" else "assistant")
                        .put("content", turn.text),
                )
            }
            messages.put(JSONObject().put("role", "user").put("content", userMessage))

            val request = JSONObject()
                .put("model", config.languageModel)
                .put("messages", messages)
                .put("temperature", 0.2)
                .put("response_format", JSONObject().put("type", "json_object"))
            if (config.reasoningEffort.isNotBlank()) {
                request.put("reasoning_effort", config.reasoningEffort)
            }

            val response = post(
                path = "/chat/completions",
                contentType = "application/json; charset=utf-8",
                body = request.toString().toByteArray(Charsets.UTF_8),
            )
            val content = parse {
                JSONObject(response)
                    .getJSONArray("choices")
                    .getJSONObject(0)
                    .getJSONObject("message")
                    .optString("content")
            }
            if (content.isBlank()) throw AssistantError(AssistantFailure.NOT_UNDERSTOOD)
            content
        }

    /** Envía y devuelve el cuerpo de la respuesta, o traduce el fallo a un motivo llano. */
    private fun post(path: String, contentType: String, body: ByteArray): String {
        val connection = try {
            (URL(config.baseUrl + path).openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                connectTimeout = CONNECT_TIMEOUT_MS
                readTimeout = READ_TIMEOUT_MS
                doOutput = true
                setFixedLengthStreamingMode(body.size)
                setRequestProperty("Authorization", "Bearer ${config.apiKey}")
                setRequestProperty("Content-Type", contentType)
                setRequestProperty("Accept", "application/json")
            }
        } catch (e: IOException) {
            throw AssistantError(AssistantFailure.NO_INTERNET, e)
        }

        try {
            connection.outputStream.use { it.write(body) }
            val code = connection.responseCode
            if (code !in 200..299) {
                // Se vacía para liberar la conexión; el contenido no se registra
                // porque puede repetir lo que se envió.
                runCatching { connection.errorStream?.use { it.readBytes() } }
                throw AssistantError(failureFor(code))
            }
            return connection.inputStream.use { it.readBytes().toString(Charsets.UTF_8) }
        } catch (e: IOException) {
            // Sin red, DNS que no resuelve, tiempo agotado o conexión cortada.
            throw AssistantError(AssistantFailure.NO_INTERNET, e)
        } finally {
            connection.disconnect()
        }
    }

    private fun failureFor(code: Int): AssistantFailure = when (code) {
        // Muchas peticiones seguidas, o el servicio saturado: se arregla esperando.
        429, 500, 502, 503, 504 -> AssistantFailure.BUSY
        // Clave mala, modelo retirado o petición que el servicio no acepta: lo
        // tiene que mirar el familiar, no la persona mayor.
        else -> AssistantFailure.UNAVAILABLE
    }

    private inline fun <T> parse(block: () -> T): T = try {
        block()
    } catch (e: JSONException) {
        throw AssistantError(AssistantFailure.UNAVAILABLE, e)
    }

    private fun vocabularyPrompt(vocabulary: List<String>): String? {
        val names = vocabulary.filter { it.isNotBlank() }.distinct()
        if (names.isEmpty()) return null
        return names.joinToString(", ", prefix = "Nombres: ", postfix = ".").take(MAX_PROMPT_CHARS)
    }

    /**
     * Whisper, ante audio casi mudo, a veces «oye» las frases de despedida de los
     * vídeos con los que se entrenó. Si la transcripción es eso, no ha dicho nada.
     */
    private fun isSilenceHallucination(text: String): Boolean {
        val normalized = text.lowercase(Locale.ROOT).trim(' ', '.', '!', '¡', '?', '¿')
        return SILENCE_HALLUCINATIONS.any { normalized.contains(it) }
    }

    private fun ByteArrayOutputStream.field(boundary: String, name: String, value: String) {
        write(
            ("--$boundary\r\n" +
                "Content-Disposition: form-data; name=\"$name\"\r\n\r\n" +
                "$value\r\n").toByteArray(Charsets.UTF_8),
        )
    }

    private fun ByteArrayOutputStream.file(
        boundary: String,
        name: String,
        fileName: String,
        mimeType: String,
        bytes: ByteArray,
    ) {
        write(
            ("--$boundary\r\n" +
                "Content-Disposition: form-data; name=\"$name\"; filename=\"$fileName\"\r\n" +
                "Content-Type: $mimeType\r\n\r\n").toByteArray(Charsets.UTF_8),
        )
        write(bytes)
        write("\r\n".toByteArray())
    }

    private companion object {
        const val CONNECT_TIMEOUT_MS = 10_000
        const val READ_TIMEOUT_MS = 20_000

        /** Whisper solo mira los últimos 224 tokens del prompt; esto se queda corto a propósito. */
        const val MAX_PROMPT_CHARS = 600

        val SILENCE_HALLUCINATIONS = listOf(
            "amara.org",
            "gracias por ver",
            "suscríbete",
            "subtítulos realizados por",
        )
    }
}
