package com.doado.assistant

import com.doado.data.ContactAction
import com.doado.data.ContactShortcut
import com.doado.data.ContactsLayout
import com.doado.data.EMERGENCY_FAVORITE_ID
import com.doado.data.FavoriteApp
import org.json.JSONObject

/** Lo que ha querido decir, ya validado contra la lista blanca. */
sealed interface Interpretation {
    data class Act(val action: AssistantAction) : Interpretation
    data class Explain(val speech: String?, val steps: List<String>) : Interpretation
    data class Clarify(val options: List<AssistantAction>) : Interpretation
    data class Refuse(val speech: String?) : Interpretation

    /** Ha pedido una casilla que no está instalada, o WhatsApp si [app] es nulo. */
    data class Missing(val app: FavoriteApp?) : Interpretation
    data object NotUnderstood : Interpretation
}

/**
 * La lista blanca del asistente: los contactos guardados en Doado y las casillas
 * del inicio, con un id corto para cada uno (`c1`, `a1`).
 *
 * Al modelo solo le llegan esos ids y los nombres, nunca un número de teléfono.
 * Y lo que conteste se valida aquí: un id que no está en la lista, o una acción
 * que no existe, es «no te he entendido». Nunca se ejecuta algo dudoso.
 *
 * @param contacts todos los contactos guardados, estén o no en el inicio.
 * @param homeApps las casillas del inicio, en su orden.
 * @param contactsLayout cómo van los contactos en el inicio, para guiar hasta ellos.
 */
class ActionCatalog(
    contacts: List<ContactShortcut>,
    val homeApps: List<FavoriteApp>,
    val columns: Int,
    val whatsappInstalled: Boolean,
    val contactsLayout: ContactsLayout = ContactsLayout.GRID,
) {
    /**
     * Una persona puede tener dos accesos directos, llamada y WhatsApp (D-40). Al
     * modelo se le cuenta una vez, con sus botones: si no, preguntaría «¿a María
     * o a María?».
     */
    private val people: List<List<ContactShortcut>> = buildList<MutableList<ContactShortcut>> {
        contacts.forEach { contact ->
            val group = firstOrNull { it.first().isSamePersonAs(contact) }
            if (group != null) group += contact else add(mutableListOf(contact))
        }
    }
    private val contactsById: Map<String, ContactShortcut> =
        people.mapIndexed { i, group -> "c${i + 1}" to group.first() }.toMap()
    private val buttonsById: Map<String, List<ContactAction>> =
        people.mapIndexed { i, group ->
            "c${i + 1}" to group.filter { it.onHome }.map { it.action }.distinct()
        }.toMap()
    private val appsById: Map<String, FavoriteApp> =
        homeApps.mapIndexed { i, app -> "a${i + 1}" to app }.toMap()

    /** Los accesos directos del inicio, en el orden en que se pintan. */
    val homeContacts: List<ContactShortcut> = contacts.filter { it.onHome }

    /**
     * El acceso directo del inicio con el que [contact] hace [action], si lo tiene:
     * puede ser otro registro de la misma persona.
     */
    fun homeButton(contact: ContactShortcut, action: ContactAction): ContactShortcut? =
        homeContacts.firstOrNull {
            (it.id == contact.id || it.isSamePersonAs(contact)) && it.action == action
        }

    /** Nombres que probablemente salgan en la orden, para afinar la transcripción. */
    val vocabulary: List<String> =
        (contacts.map { it.name } + homeApps.map { it.label }).distinct()

    /** La lista tal como se le cuenta al modelo en el prompt. */
    fun describe(): String = buildString {
        appendLine("Contactos guardados:")
        if (contactsById.isEmpty()) appendLine("- (ninguno)")
        contactsById.forEach { (id, contact) ->
            val buttons = buttonsById[id].orEmpty().map {
                when (it) {
                    ContactAction.CALL -> "llamada"
                    ContactAction.WHATSAPP -> "WhatsApp"
                }
            }
            val button = if (buttons.isEmpty()) "" else " (botones en el inicio: ${buttons.joinToString(" y ")})"
            appendLine("- $id: ${JSONObject.quote(contact.name)}$button")
        }
        appendLine("Casillas del inicio:")
        if (appsById.isEmpty()) appendLine("- (ninguna)")
        appsById.forEach { (id, app) ->
            val missing = if (app.isAvailable) "" else " (no está instalada)"
            val emergency = if (app.id == EMERGENCY_FAVORITE_ID) " (emergencias: abre el teléfono con el 112 marcado)" else ""
            appendLine("- $id: ${JSONObject.quote(app.label)}$emergency$missing")
        }
        append("WhatsApp: ").append(if (whatsappInstalled) "instalado" else "no instalado")
    }

    /** Valida la respuesta del modelo. Cualquier cosa rara acaba en [Interpretation.NotUnderstood]. */
    fun parse(raw: String): Interpretation {
        val json = extractJson(raw) ?: return Interpretation.NotUnderstood
        val speech = json.text(Prompts.FIELD_SAY)
        return when (val kind = json.text(Prompts.FIELD_ACTION)) {
            Prompts.ACTION_CALL, Prompts.ACTION_WHATSAPP, Prompts.ACTION_OPEN_APP ->
                resolve(kind, json.text(Prompts.FIELD_ID))

            Prompts.ACTION_EXPLAIN -> {
                val steps = json.optJSONArray(Prompts.FIELD_STEPS)?.let { array ->
                    (0 until array.length())
                        .mapNotNull { array.optString(it).trim().takeIf(String::isNotEmpty) }
                        .take(MAX_STEPS)
                }.orEmpty()
                if (speech == null && steps.isEmpty()) Interpretation.NotUnderstood
                else Interpretation.Explain(speech, steps)
            }

            Prompts.ACTION_CLARIFY -> {
                val target = json.text(Prompts.FIELD_FOR)
                val ids = json.optJSONArray(Prompts.FIELD_OPTIONS)
                val resolved = (0 until (ids?.length() ?: 0))
                    .mapNotNull { ids?.optString(it)?.trim() }
                    .distinct()
                    .map { resolve(target, it) }
                val options = resolved.mapNotNull { (it as? Interpretation.Act)?.action }.take(MAX_OPTIONS)
                when {
                    // Aunque solo quede una, se pregunta igual: el modelo dudaba, y
                    // desde D-56 lo que sale de aquí se haría sin preguntar.
                    options.isNotEmpty() -> Interpretation.Clarify(options)
                    // Todas eran de WhatsApp sin WhatsApp, o casillas apagadas: se dice qué falta.
                    resolved.isNotEmpty() && resolved.all { it is Interpretation.Missing } -> resolved.first()
                    else -> Interpretation.NotUnderstood
                }
            }

            Prompts.ACTION_REFUSE -> Interpretation.Refuse(speech)
            else -> Interpretation.NotUnderstood
        }
    }

    /** Convierte una acción con su id en algo ejecutable, si de verdad lo es. */
    private fun resolve(kind: String?, id: String?): Interpretation {
        if (id == null) return Interpretation.NotUnderstood
        return when (kind) {
            Prompts.ACTION_CALL -> contactsById[id]
                ?.let { Interpretation.Act(AssistantAction.CallContact(it)) }
                ?: Interpretation.NotUnderstood

            Prompts.ACTION_WHATSAPP -> {
                val contact = contactsById[id] ?: return Interpretation.NotUnderstood
                if (!whatsappInstalled) Interpretation.Missing(app = null)
                else Interpretation.Act(AssistantAction.WhatsAppContact(contact))
            }

            Prompts.ACTION_OPEN_APP -> {
                val app = appsById[id] ?: return Interpretation.NotUnderstood
                if (!app.isAvailable) Interpretation.Missing(app)
                else Interpretation.Act(AssistantAction.OpenApp(app))
            }

            else -> Interpretation.NotUnderstood
        }
    }

    /** Los modelos a veces envuelven el JSON en texto o en bloques de código. */
    private fun extractJson(raw: String): JSONObject? {
        val start = raw.indexOf('{')
        val end = raw.lastIndexOf('}')
        if (start < 0 || end <= start) return null
        return runCatching { JSONObject(raw.substring(start, end + 1)) }.getOrNull()
    }

    /** `optString` devuelve "null" para un null de JSON; aquí eso es que no hay nada. */
    private fun JSONObject.text(key: String): String? =
        optString(key).trim().takeUnless { it.isEmpty() || it == "null" }

    private companion object {
        // Los mismos que pide el prompt (regla 5): más pasos no se siguen de oído.
        const val MAX_STEPS = 4
        const val MAX_OPTIONS = 4
    }
}
