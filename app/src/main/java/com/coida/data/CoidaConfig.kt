package com.coida.data

import org.json.JSONArray
import org.json.JSONObject

/**
 * Lo que el familiar configura, de una pieza: la interfaz, el asistente, los
 * contactos y el orden de las casillas. Es lo que algún día podría copiarse a
 * otro móvil o sincronizarse; hoy solo se lee y se guarda en este.
 *
 * Se convierte a JSON y vuelve de él con `org.json`, que ya viene con Android:
 * así no hace falta ninguna librería de serialización. [schemaVersion] dice con
 * qué forma se escribió, para poder leer versiones antiguas si la forma cambia.
 *
 * Quedan fuera a propósito el PIN y su pregunta (secretos que no deben salir del
 * móvil, RNF-SEG-01), la foto de fondo (un fichero, D-47) y el estado del propio
 * móvil, como el modo administrador o los avisos ya cerrados.
 *
 * @property homeAppIds ids de las casillas del inicio en su orden, como los
 *   guarda [FavoritesStore].
 */
data class CoidaConfig(
    val schemaVersion: Int = CURRENT_SCHEMA_VERSION,
    val interfaceSettings: InterfaceSettings = InterfaceSettings(),
    val assistant: AssistantSettings = AssistantSettings(),
    val contacts: List<ContactShortcut> = emptyList(),
    val homeAppIds: List<String> = emptyList(),
) {

    fun toJson(): JSONObject = JSONObject()
        .put("schemaVersion", schemaVersion)
        .put(
            "interface",
            JSONObject()
                .put("theme", interfaceSettings.theme.name)
                .put("textSize", interfaceSettings.textSize.name)
                .put("columns", interfaceSettings.columns)
                .put("showClock", interfaceSettings.showClock)
                .put("contactsLayout", interfaceSettings.contactsLayout.name),
        )
        .put(
            "assistant",
            JSONObject()
                .put("enabled", assistant.enabled)
                .put("mode", assistant.mode.name),
        )
        .put(
            "contacts",
            JSONArray().apply {
                contacts.forEach {
                    put(
                        JSONObject()
                            .put("id", it.id)
                            .put("name", it.name)
                            .put("number", it.number)
                            .put("action", it.action.name)
                            .put("onHome", it.onHome),
                    )
                }
            },
        )
        .put("homeAppIds", JSONArray(homeAppIds))

    companion object {
        const val CURRENT_SCHEMA_VERSION = 1

        /**
         * Lee un JSON escrito por [toJson]. Como las preferencias, nunca falla: lo
         * que falte o no se entienda toma el valor de fábrica, y un contacto roto
         * se salta sin llevarse a los demás.
         */
        fun fromJson(json: JSONObject): CoidaConfig {
            val defaults = InterfaceSettings()
            val ui = json.optJSONObject("interface") ?: JSONObject()
            val assistant = json.optJSONObject("assistant") ?: JSONObject()
            val contacts = json.optJSONArray("contacts") ?: JSONArray()
            val homeAppIds = json.optJSONArray("homeAppIds") ?: JSONArray()
            return CoidaConfig(
                schemaVersion = json.optInt("schemaVersion", CURRENT_SCHEMA_VERSION),
                interfaceSettings = InterfaceSettings(
                    theme = enumOrDefault(ui.optString("theme"), defaults.theme),
                    textSize = enumOrDefault(ui.optString("textSize"), defaults.textSize),
                    columns = ui.optInt("columns", defaults.columns).coerceIn(1, 4),
                    showClock = ui.optBoolean("showClock", defaults.showClock),
                    contactsLayout = enumOrDefault(ui.optString("contactsLayout"), defaults.contactsLayout),
                ),
                assistant = AssistantSettings(
                    enabled = assistant.optBoolean("enabled", false),
                    mode = enumOrDefault(assistant.optString("mode"), AssistantMode.ACT),
                ),
                contacts = (0 until contacts.length()).mapNotNull { index ->
                    val item = contacts.optJSONObject(index) ?: return@mapNotNull null
                    runCatching {
                        ContactShortcut(
                            id = item.getString("id"),
                            name = item.getString("name"),
                            number = item.getString("number"),
                            action = enumOrDefault(item.optString("action"), ContactAction.CALL),
                            onHome = item.optBoolean("onHome", true),
                        )
                    }.getOrNull()
                },
                homeAppIds = (0 until homeAppIds.length())
                    .map { homeAppIds.optString(it) }
                    .filter { it.isNotBlank() },
            )
        }

        private inline fun <reified T : Enum<T>> enumOrDefault(name: String?, default: T): T =
            enumValues<T>().firstOrNull { it.name == name } ?: default
    }
}
