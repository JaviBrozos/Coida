package com.coida.data

import android.content.Context
import androidx.core.content.edit
import java.util.UUID

/**
 * Un identificador aleatorio de esta instalación, que se crea la primera vez.
 *
 * Hoy no se usa ni sale del móvil: queda preparado por si un día hay copia o
 * sincronización y hace falta distinguir un móvil de otro. No identifica a nadie,
 * no es el del dispositivo ni el de publicidad y cambia al reinstalar.
 *
 * Vive en su propio fichero, fuera de la copia de seguridad (no está en la lista
 * blanca de res/xml/backup_rules.xml): si se restaurara en otro móvil, dos
 * instalaciones tendrían el mismo.
 */
class InstallationId(context: Context) {

    private val prefs =
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun get(): String = prefs.getString(KEY_ID, null) ?: UUID.randomUUID().toString().also {
        prefs.edit { putString(KEY_ID, it) }
    }

    private companion object {
        const val PREFS_NAME = "coida_install"
        const val KEY_ID = "installation_id"
    }
}
