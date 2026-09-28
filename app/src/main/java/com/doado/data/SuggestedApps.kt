package com.doado.data

import androidx.annotation.StringRes
import com.doado.R

/**
 * Apps que Doado propone instalar desde Ajustes → Aplicaciones.
 *
 * A diferencia de los favoritos de fábrica, aquí sí se fija el paquete: no hay
 * una "acción de Android" genérica que signifique "WhatsApp". Cualquier otra app
 * instalada se puede añadir igualmente desde la lista de apps del móvil.
 */
data class SuggestedApp(
    val packageName: String,
    val label: String,
    @param:StringRes val descriptionRes: Int,
)

val SuggestedApps: List<SuggestedApp> = listOf(
    SuggestedApp(
        packageName = WHATSAPP_PACKAGE,
        label = "WhatsApp",
        descriptionRes = R.string.suggested_whatsapp_description,
    ),
    SuggestedApp(
        packageName = "com.sofascore.results",
        label = "Sofascore",
        descriptionRes = R.string.suggested_sofascore_description,
    ),
)

const val WHATSAPP_PACKAGE = "com.whatsapp"
