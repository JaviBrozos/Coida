package com.doado.data

import android.content.Context
import androidx.core.content.edit

/**
 * Ajustes de Doado que decide quien configura el móvil: aspecto de la interfaz,
 * el asistente de voz y hasta cuándo dura el modo administrador.
 *
 * El PIN **no** está aquí: vive en [PinStore], en otro fichero, porque ese es el
 * único que se queda fuera de la copia de seguridad.
 */
class SettingsStore(context: Context) {

    private val prefs =
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun interfaceSettings(): InterfaceSettings = InterfaceSettings(
        theme = enumOrDefault(prefs.getString(KEY_THEME, null), ThemeMode.DARK),
        textSize = enumOrDefault(prefs.getString(KEY_TEXT_SIZE, null), TextSize.NORMAL),
        columns = prefs.getInt(KEY_COLUMNS, 2).coerceIn(1, 2),
        showClock = prefs.getBoolean(KEY_SHOW_CLOCK, true),
    )

    fun saveInterfaceSettings(settings: InterfaceSettings) {
        prefs.edit {
            putString(KEY_THEME, settings.theme.name)
            putString(KEY_TEXT_SIZE, settings.textSize.name)
            putInt(KEY_COLUMNS, settings.columns)
            putBoolean(KEY_SHOW_CLOCK, settings.showClock)
        }
    }

    fun assistantSettings(): AssistantSettings = AssistantSettings(
        enabled = prefs.getBoolean(KEY_ASSISTANT_ENABLED, false),
        mode = enumOrDefault(prefs.getString(KEY_ASSISTANT_MODE, null), AssistantMode.GUIDE),
    )

    fun saveAssistantSettings(settings: AssistantSettings) {
        prefs.edit {
            putBoolean(KEY_ASSISTANT_ENABLED, settings.enabled)
            putString(KEY_ASSISTANT_MODE, settings.mode.name)
        }
    }

    /**
     * Momento (reloj del sistema, en ms) en que se vuelve a bloquear el modo
     * administrador. Un valor pasado equivale a bloqueado.
     */
    fun adminUnlockedUntil(): Long = prefs.getLong(KEY_ADMIN_UNTIL, 0L)

    fun saveAdminUnlockedUntil(epochMillis: Long) {
        prefs.edit { putLong(KEY_ADMIN_UNTIL, epochMillis) }
    }

    private inline fun <reified T : Enum<T>> enumOrDefault(name: String?, default: T): T =
        enumValues<T>().firstOrNull { it.name == name } ?: default

    private companion object {
        // Coincide con el fichero declarado en res/xml/backup_rules.xml.
        const val PREFS_NAME = "doado_settings"
        const val KEY_THEME = "theme"
        const val KEY_TEXT_SIZE = "text_size"
        const val KEY_COLUMNS = "columns"
        const val KEY_SHOW_CLOCK = "show_clock"
        const val KEY_ADMIN_UNTIL = "admin_unlocked_until"
        const val KEY_ASSISTANT_ENABLED = "assistant_enabled"
        const val KEY_ASSISTANT_MODE = "assistant_mode"
    }
}
