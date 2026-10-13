package com.doado.assistant

import java.text.Normalizer
import java.util.Locale

/**
 * Respuestas cortas de la conversación que se entienden en el propio móvil, sin
 * pasar por la IA: «sí», «no», «siguiente», «repite», «gracias» y el nombre de una
 * opción al aclarar.
 *
 * La regla que manda (D-31): una respuesta solo cuenta si **todas** sus palabras
 * son de esa respuesta o de relleno. «Sí» o «vale, llámala» confirman; «sí, llama a
 * Luis» no confirma la llamada a María: no es una respuesta corta, es otra orden,
 * y va al modelo, que la convierte en una pregunta nueva. Ante la duda, nunca se
 * actúa.
 */
object VoiceReplies {

    enum class Reply { YES, NO, NEXT, REPEAT, DONE }

    /**
     * Respuesta a «¿Llamo a María?».
     *
     * @param targetName nombre del contacto o la casilla de la pregunta: «sí, a
     *   María» también confirma.
     */
    fun confirmation(heard: String, targetName: String): Reply? {
        val words = words(heard)
        if (words.isEmpty()) return null
        val nameWords = words(targetName).toSet()
        val saysYes = words.any { it in YES } || containsPhrase(words, YES_PHRASES)
        val saysNo = words.any { it in NO }
        return when {
            saysYes && !saysNo && words.all { it in YES || it in FILLER || it in nameWords || it in YES_PHRASE_WORDS } ->
                Reply.YES

            saysNo && !saysYes && words.all { it in NO || it in FILLER } -> Reply.NO
            else -> null
        }
    }

    /** Respuesta mientras se dan pasos. En el último, «siguiente» o «vale» terminan. */
    fun guide(heard: String, isLast: Boolean): Reply? {
        val words = words(heard)
        if (words.isEmpty()) return null
        return when {
            isClosing(heard) -> Reply.DONE
            onlyFrom(words, REPEAT) && words.any { it in REPEAT } -> Reply.REPEAT
            onlyFrom(words, NEXT) && words.any { it in NEXT } -> if (isLast) Reply.DONE else Reply.NEXT
            else -> null
        }
    }

    /** «Gracias», «ya está», «nada más»: la conversación ha acabado. */
    fun isClosing(heard: String): Boolean {
        val words = words(heard)
        return words.isNotEmpty() && words.all { it in DONE || it in DONE_PHRASE_WORDS || it in FILLER } &&
            (words.any { it in DONE } || containsPhrase(words, DONE_PHRASES))
    }

    /**
     * La opción que ha nombrado al aclarar, si se reconoce sin dudas. Si dice
     * «María» y hay «María» y «María José», gana la que coincide entera y más larga;
     * si aun así hay empate, no se elige ninguna.
     */
    fun option(heard: String, options: List<ClarifyOption>): ClarifyOption? {
        val spoken = " " + words(heard).joinToString(" ") + " "
        val matches = options
            .map { it to words(it.label).joinToString(" ") }
            .filter { (_, label) -> label.isNotEmpty() && spoken.contains(" $label ") }
        val longest = matches.maxOfOrNull { it.second.length } ?: return null
        return matches.filter { it.second.length == longest }.singleOrNull()?.first
    }

    /** Minúsculas, sin tildes ni signos: «¡Sí, llámala!» → [si, llamala]. */
    private fun words(text: String): List<String> =
        Normalizer.normalize(text.lowercase(Locale.ROOT), Normalizer.Form.NFD)
            .replace(Regex("\\p{Mn}+"), "")
            .replace(Regex("[^a-z0-9ñ]+"), " ")
            .trim()
            .split(' ')
            .filter { it.isNotEmpty() }

    private fun onlyFrom(words: List<String>, vocabulary: Set<String>) =
        words.all { it in vocabulary || it in FILLER }

    private fun containsPhrase(words: List<String>, phrases: List<String>): Boolean {
        val joined = " " + words.joinToString(" ") + " "
        return phrases.any { joined.contains(" $it ") }
    }

    private val YES = setOf(
        "si", "vale", "venga", "claro", "hazlo", "adelante", "correcto", "exacto", "perfecto",
        "dale", "ok", "okay", "llama", "llamala", "llamale", "llamalo", "abre", "abrela", "abrelo",
        "abrir", "llamar", "porfa",
    )
    private val YES_PHRASES = listOf("de acuerdo", "eso es", "por favor")
    private val YES_PHRASE_WORDS = setOf("de", "acuerdo", "eso", "es", "por", "favor")

    private val NO = setOf(
        "no", "cancela", "cancelar", "dejalo", "deja", "espera", "nada", "mejor", "para", "quieto",
        "todavia", "aun", "ahora",
    )

    private val NEXT = setOf(
        "siguiente", "sigue", "seguir", "vale", "venga", "ya", "hecho", "listo", "luego", "despues",
        "ahora", "y", "otro", "paso", "ok", "bueno", "adelante", "continua",
    )
    private val REPEAT = setOf("repite", "repitelo", "repetir", "repiteme", "otra", "vez", "como", "que", "perdona", "perdon")
    /** Palabras que por sí solas cierran: «gracias», «adiós», «entendido». */
    private val DONE = setOf("termina", "terminar", "terminado", "entendido", "gracias", "adios", "fin", "salir")

    /** Las que solo cierran juntas: «ya está», «nada más». Sueltas, «ya» es «siguiente». */
    private val DONE_PHRASES = listOf("ya esta", "nada mas", "eso es todo", "muchas gracias")
    private val DONE_PHRASE_WORDS = setOf("ya", "esta", "nada", "mas", "eso", "es", "todo", "muchas")

    /** Lo que se dice sin decir nada: muletillas, tratamientos y cortesía. */
    private val FILLER = setOf(
        "eh", "ah", "oh", "pues", "bueno", "hombre", "mujer", "hija", "hijo", "a", "la", "el", "lo",
        "le", "me", "te", "se", "que", "mira", "oye", "vamos", "muy", "bien", "si", "ya", "gracias",
    )
}
