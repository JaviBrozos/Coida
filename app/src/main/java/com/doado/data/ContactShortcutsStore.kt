package com.doado.data

import android.content.Context
import androidx.core.content.edit
import org.json.JSONArray
import org.json.JSONObject

/**
 * Contactos fijados como accesos directos, en el orden en que se añadieron.
 *
 * Se guardan como JSON en SharedPreferences: son pocos (caben cuatro en el
 * inicio) y así no hace falta montar una base de datos para esto.
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

    companion object {
        /** Los que caben arriba del inicio sin empujar las casillas fuera de pantalla. */
        const val MAX_ON_HOME = 4

        // Coincide con el fichero declarado en res/xml/backup_rules.xml.
        private const val PREFS_NAME = "doado_contacts"
        private const val KEY_CONTACTS = "contacts"
    }
}
