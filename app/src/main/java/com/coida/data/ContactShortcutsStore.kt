package com.coida.data

import android.content.Context
import androidx.core.content.edit
import org.json.JSONArray
import org.json.JSONObject

/**
 * Contactos fijados como accesos directos, en el orden en que salen en el inicio:
 * el de llegada, salvo que el familiar los haya colocado a mano (D-59).
 *
 * Se guardan como JSON en SharedPreferences: son pocos y así no hace falta montar
 * una base de datos para esto.
 */
class ContactShortcutsStore(context: Context) {

    private val prefs =
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun all(): List<ContactShortcut> {
        val raw = prefs.getString(KEY_CONTACTS, null) ?: return emptyList()
        return runCatching {
            val array = JSONArray(raw)
            (0 until array.length()).map { index ->
                val item = array.getJSONObject(index)
                ContactShortcut(
                    id = item.getString("id"),
                    name = item.getString("name"),
                    number = item.getString("number"),
                    action = ContactAction.entries
                        .firstOrNull { it.name == item.optString("action") }
                        ?: ContactAction.CALL,
                    onHome = item.optBoolean("onHome", true),
                )
            }
        }.getOrDefault(emptyList())
    }

    fun save(contacts: List<ContactShortcut>) {
        val array = JSONArray()
        contacts.forEach { contact ->
            array.put(
                JSONObject()
                    .put("id", contact.id)
                    .put("name", contact.name)
                    .put("number", contact.number)
                    .put("action", contact.action.name)
                    .put("onHome", contact.onHome),
            )
        }
        prefs.edit { putString(KEY_CONTACTS, array.toString()) }
    }

    /**
     * Reordena los accesos directos del inicio según [orderedIds] (D-59). Los que
     * no están en el inicio no se ven, así que conservan su hueco en la lista: solo
     * se reparten de nuevo los huecos de los que sí están.
     */
    fun saveHomeOrder(orderedIds: List<String>) {
        val contacts = all()
        val byId = contacts.associateBy { it.id }
        // Sin repetidos: un id dos veces se comía a otro, y la rejilla no admite claves iguales.
        val placed = orderedIds.distinct().mapNotNull { byId[it] }.filter { it.onHome }
        val rest = contacts.filter { it.onHome && it !in placed }
        val queue = ArrayDeque(placed + rest)
        save(contacts.map { if (it.onHome) queue.removeFirst() else it })
    }

    companion object {
        // Coincide con el fichero declarado en res/xml/backup_rules.xml.
        private const val PREFS_NAME = "doado_contacts"
        private const val KEY_CONTACTS = "contacts"
    }
}
