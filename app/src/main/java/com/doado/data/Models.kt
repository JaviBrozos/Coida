package com.doado.data

import androidx.annotation.DrawableRes

/**
 * Una app instalada en el móvil que el sistema considera "lanzable".
 * Todavía no se pinta en el MVP: es la materia prima de la futura pantalla
 * de "elegir favoritos".
 */
data class InstalledApp(
    val packageName: String,
    val activityName: String,
    val label: String,
)

/**
 * Una casilla del grid de inicio.
 *
 * [packageName] es nulo cuando en este móvil no hay ninguna app capaz de atender
 * la acción (por ejemplo, un emulador sin app de cámara). En ese caso la casilla
 * se sigue dibujando, pero desactivada y avisando de por qué, en lugar de
 * desaparecer: para alguien que ya se ha aprendido dónde está cada botón, que un
 * icono cambie de sitio es peor que un icono apagado.
 */
data class FavoriteApp(
    val id: String,
    val label: String,
    @param:DrawableRes val iconRes: Int,
    val packageName: String?,
) {
    val isAvailable: Boolean get() = packageName != null
}
