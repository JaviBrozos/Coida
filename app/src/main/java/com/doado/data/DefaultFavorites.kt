package com.doado.data

import android.content.Intent
import android.provider.MediaStore
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import com.doado.R

/**
 * Plantilla de una app favorita: qué se enseña y qué intención abre.
 *
 * Se define por *acción* y no por nombre de paquete a propósito. "Llamar" debe
 * abrir la app de teléfono que tenga este móvil, sea la de Google, la de Samsung
 * o la que sea; fijar un paquete concreto rompería la casilla en cuanto se
 * cambiara de marca de teléfono.
 */
internal data class FavoriteTemplate(
    val id: String,
    @param:StringRes val labelRes: Int,
    @param:DrawableRes val iconRes: Int,
    val intent: Intent,
)

/**
 * Los favoritos de fábrica del MVP.
 *
 * Cada acción declarada aquí tiene que aparecer también en el bloque <queries>
 * del AndroidManifest, o en Android 11+ no se podrá resolver.
 */
internal val DefaultFavorites: List<FavoriteTemplate> = listOf(
    FavoriteTemplate(
        id = "phone",
        labelRes = R.string.app_phone,
        iconRes = R.drawable.ic_call,
        intent = Intent(Intent.ACTION_DIAL),
    ),
    FavoriteTemplate(
        id = "messages",
        labelRes = R.string.app_messages,
        iconRes = R.drawable.ic_messages,
        intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_APP_MESSAGING),
    ),
    FavoriteTemplate(
        id = "camera",
        labelRes = R.string.app_camera,
        iconRes = R.drawable.ic_camera,
        intent = Intent(MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA),
    ),
    FavoriteTemplate(
        id = "contacts",
        labelRes = R.string.app_contacts,
        iconRes = R.drawable.ic_contacts,
        intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_APP_CONTACTS),
    ),
)
