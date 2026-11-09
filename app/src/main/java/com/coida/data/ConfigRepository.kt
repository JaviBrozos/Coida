package com.coida.data

import android.content.Context

/**
 * Por donde la interfaz lee y guarda lo que configura el familiar ([CoidaConfig]).
 *
 * Hoy solo existe [LocalConfigRepository], en el propio móvil. Está detrás de una
 * interfaz para que, si un día hay copia o sincronización, la interfaz no tenga
 * que enterarse. Coida no tiene servidor propio: eso sería una decisión aparte.
 */
interface ConfigRepository {

    fun load(): CoidaConfig

    fun saveInterface(settings: InterfaceSettings)

    fun saveAssistant(settings: AssistantSettings)

    fun saveContacts(contacts: List<ContactShortcut>)

    /** Reordena los accesos directos del inicio (D-59) y devuelve la lista ya guardada. */
    fun saveContactOrder(orderedIds: List<String>): List<ContactShortcut>
}

/**
 * La configuración en las mismas SharedPreferences de siempre. Solo delega: no
 * cambia ningún fichero ni ninguna clave, así que lo ya guardado se sigue leyendo
 * igual (una clave publicada no se renombra).
 *
 * Las casillas se leen aquí, pero se encienden, se apagan y se ordenan desde
 * [InstalledAppsRepository], que es quien sabe qué apps hay instaladas.
 */
class LocalConfigRepository(context: Context) : ConfigRepository {

    private val settings = SettingsStore(context)
    private val contacts = ContactShortcutsStore(context)
    private val favorites = FavoritesStore(context)

    override fun load() = CoidaConfig(
        interfaceSettings = settings.interfaceSettings(),
        assistant = settings.assistantSettings(),
        contacts = contacts.all(),
        homeAppIds = favorites.favoriteIds(),
    )

    override fun saveInterface(settings: InterfaceSettings) = this.settings.saveInterfaceSettings(settings)

    override fun saveAssistant(settings: AssistantSettings) = this.settings.saveAssistantSettings(settings)

    override fun saveContacts(contacts: List<ContactShortcut>) = this.contacts.save(contacts)

    override fun saveContactOrder(orderedIds: List<String>): List<ContactShortcut> {
        contacts.saveHomeOrder(orderedIds)
        return contacts.all()
    }
}
