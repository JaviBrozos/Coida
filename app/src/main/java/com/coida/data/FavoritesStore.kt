package com.coida.data

import android.content.Context
import androidx.core.content.edit

/**
 * Guarda qué casillas salen en la pantalla de inicio y en qué orden.
 *
 * Cada casilla se guarda con un id: los favoritos de fábrica usan el suyo
 * ("phone", "camera"...) y las apps añadidas después usan "pkg:" + su paquete
 * ("pkg:com.whatsapp").
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
            ?: return DefaultFavorites.filter { it.onHomeByDefault }.map { it.id }
        return stored.split(SEPARATOR).filter { it.isNotBlank() }
    }

    fun saveFavoriteIds(ids: List<String>) {
        prefs.edit { putString(KEY_FAVORITES, ids.distinct().joinToString(SEPARATOR)) }
    }

    /** Vuelve a los favoritos de fábrica. */
    fun reset() {
        prefs.edit { remove(KEY_FAVORITES) }
    }

    companion object {
        /** Prefijo de las casillas que apuntan a una app concreta por su paquete. */
        const val PACKAGE_PREFIX = "pkg:"

        fun packageId(packageName: String) = PACKAGE_PREFIX + packageName

        // Coincide con el fichero declarado en res/xml/backup_rules.xml.
        private const val PREFS_NAME = "doado_favorites"
        private const val KEY_FAVORITES = "favorite_ids"
        private const val SEPARATOR = ","
    }
}
