package com.doado.data

import android.content.Context
import androidx.core.content.edit

/**
 * Guarda qué favoritos ha elegido el usuario y en qué orden.
 *
 * El orden se guarda como una cadena separada por comas y no como un StringSet
 * de SharedPreferences: un Set no conserva el orden, y en un lanzador para
 * personas mayores la posición de cada casilla es memoria muscular. Que los
 * iconos bailen de sitio entre arranques sería un fallo de accesibilidad, no una
 * cuestión estética.
 */
class FavoritesStore(context: Context) {

    private val prefs =
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    /** Ids guardados; si no hay nada guardado todavía, los favoritos de fábrica. */
    fun favoriteIds(): List<String> {
        val stored = prefs.getString(KEY_FAVORITES, null)
            ?.split(SEPARATOR)
            ?.filter { it.isNotBlank() }

        return if (stored.isNullOrEmpty()) DefaultFavorites.map { it.id } else stored
    }

    fun saveFavoriteIds(ids: List<String>) {
        prefs.edit { putString(KEY_FAVORITES, ids.joinToString(SEPARATOR)) }
    }

    /** Vuelve a los favoritos de fábrica. */
    fun reset() {
        prefs.edit { remove(KEY_FAVORITES) }
    }

    private companion object {
        // Coincide con el fichero declarado en res/xml/backup_rules.xml.
        const val PREFS_NAME = "doado_favorites"
        const val KEY_FAVORITES = "favorite_ids"
        const val SEPARATOR = ","
    }
}
