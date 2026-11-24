package com.coida.data

import android.content.Context
import androidx.core.content.edit

/**
 * Ajustes de Coida que decide quien configura el móvil: aspecto de la interfaz,
 * el asistente de voz y hasta cuándo dura el modo administrador.
 *
 * El PIN **no** está aquí: vive en [PinStore], en otro fichero, porque ese es el
 * único que se queda fuera de la copia de seguridad.
 */
class SettingsStore(context: Context) {

    private val appContext = context.applicationContext

    private val prefs =
        appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun interfaceSettings(): InterfaceSettings = InterfaceSettings(
        theme = enumOrDefault(prefs.getString(KEY_THEME, null), ThemeMode.DARK),
        textSize = textSize(),
        columns = prefs.getInt(KEY_COLUMNS, 2).coerceIn(1, MAX_COLUMNS),
        showClock = prefs.getBoolean(KEY_SHOW_CLOCK, true),
        contactsLayout = enumOrDefault(prefs.getString(KEY_CONTACTS_LAYOUT, null), ContactsLayout.GRID),
    )

    fun saveInterfaceSettings(settings: InterfaceSettings) {
        prefs.edit {
            putString(KEY_THEME, settings.theme.name)
            putString(KEY_TEXT_SIZE_V2, settings.textSize.name)
            putInt(KEY_COLUMNS, settings.columns)
            putBoolean(KEY_SHOW_CLOCK, settings.showClock)
            putString(KEY_CONTACTS_LAYOUT, settings.contactsLayout.name)
        }
    }

    fun assistantSettings(): AssistantSettings = AssistantSettings(
        enabled = prefs.getBoolean(KEY_ASSISTANT_ENABLED, false),
        mode = enumOrDefault(prefs.getString(KEY_ASSISTANT_MODE, null), AssistantMode.ACT),
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

    /**
     * El familiar cerró con la equis el aviso del inicio de que Coida no es la
     * pantalla de inicio (D-48): solo estaba probando. El de Ajustes sigue saliendo.
     */
    fun homeNoticeDismissed(): Boolean = prefs.getBoolean(KEY_HOME_NOTICE_DISMISSED, false)

    fun saveHomeNoticeDismissed() {
        prefs.edit { putBoolean(KEY_HOME_NOTICE_DISMISSED, true) }
    }

    /**
     * El familiar encendió la bola roja de llamadas perdidas (D-66). Solo cuenta en
     * las variantes que traen el permiso, y si Android lo tiene concedido.
     */
    fun missedCallsBadge(): Boolean = prefs.getBoolean(KEY_MISSED_CALLS_BADGE, false)

    fun saveMissedCallsBadge(enabled: Boolean) {
        prefs.edit { putBoolean(KEY_MISSED_CALLS_BADGE, enabled) }
    }

    /**
     * La letra se guarda en una clave nueva desde que hay un nivel por debajo del
     * antiguo «Normal» (D-55). Si solo está la vieja, se traduce un escalón hacia
     * arriba: quien ya usaba Coida conserva exactamente el tamaño que tenía.
     */
    private fun textSize(): TextSize {
        prefs.getString(KEY_TEXT_SIZE_V2, null)?.let { return enumOrDefault(it, TextSize.NORMAL) }
        val migrated = when (prefs.getString(KEY_TEXT_SIZE_LEGACY, null)) {
            "LARGE" -> TextSize.EXTRA_LARGE
            "EXTRA_LARGE" -> TextSize.HUGE
            "NORMAL" -> TextSize.LARGE
            // Sin ninguna de las dos: quien actualiza sin haber tocado nunca la letra
            // veía el «Normal» de antes, que ahora es «Grande». Solo una instalación
            // nueva arranca en el «Normal» nuevo.
            else -> if (isUpdate()) TextSize.LARGE else TextSize.NORMAL
        }
        // Se apunta ya: así se decide una vez, y no cambia con la próxima actualización.
        prefs.edit { putString(KEY_TEXT_SIZE_V2, migrated.name) }
        return migrated
    }

    /** Coida ya estaba instalado antes de esta versión, aunque no se hubiera configurado. */
    private fun isUpdate(): Boolean = runCatching {
        val info = appContext.packageManager.getPackageInfo(appContext.packageName, 0)
        info.lastUpdateTime > info.firstInstallTime
    }.getOrDefault(false)

    private inline fun <reified T : Enum<T>> enumOrDefault(name: String?, default: T): T =
        enumValues<T>().firstOrNull { it.name == name } ?: default

    private companion object {
        // Con cuatro, la casilla queda en unos 70 dp en un móvil de 360 dp: el nombre
        // se encoge para caber entero (D-54).
        const val MAX_COLUMNS = 4

        // Coincide con el fichero declarado en res/xml/backup_rules.xml. Conserva el
        // nombre de Doado a propósito: cambiarlo borraría lo guardado (D-62).
        const val PREFS_NAME = "doado_settings"
        const val KEY_THEME = "theme"
        const val KEY_TEXT_SIZE_LEGACY = "text_size"
        const val KEY_TEXT_SIZE_V2 = "text_size_v2"
        const val KEY_COLUMNS = "columns"
        const val KEY_SHOW_CLOCK = "show_clock"
        const val KEY_CONTACTS_LAYOUT = "contacts_layout"
        const val KEY_ADMIN_UNTIL = "admin_unlocked_until"
        const val KEY_ASSISTANT_ENABLED = "assistant_enabled"
        const val KEY_ASSISTANT_MODE = "assistant_mode"
        const val KEY_HOME_NOTICE_DISMISSED = "home_notice_dismissed"
        const val KEY_MISSED_CALLS_BADGE = "missed_calls_badge"
    }
}
