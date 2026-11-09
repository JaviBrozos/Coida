package com.coida.data

import android.content.Intent
import android.net.Uri
import android.provider.MediaStore
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import com.coida.R

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
    /** Si sale en el inicio de un móvil recién estrenado, o solo si la pone el familiar. */
    val onHomeByDefault: Boolean = true,
)

/** Id de la casilla «Llamar»: el asistente la señala al guiar una llamada. */
const val PHONE_FAVORITE_ID = "phone"

/** Id de la casilla «Emergencias 112» (D-51). */
const val EMERGENCY_FAVORITE_ID = "emergency"

/**
 * Los favoritos de fábrica del MVP.
 *
 * Cada acción declarada aquí tiene que aparecer también en el bloque <queries>
 * del AndroidManifest, o en Android 11+ no se podrá resolver.
 */
internal val DefaultFavorites: List<FavoriteTemplate> = listOf(
    FavoriteTemplate(
        id = PHONE_FAVORITE_ID,
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
    // ACTION_DIAL y no ACTION_CALL: Android no deja a una app llamar sola a un
    // número de emergencias, y así un toque sin querer no llama al 112. Abre el
    // teléfono con el número puesto y la llamada la hace ella con el botón verde.
    // Apagada de fábrica: la pone el familiar si le viene bien (D-51).
    FavoriteTemplate(
        id = EMERGENCY_FAVORITE_ID,
        labelRes = R.string.app_emergency,
        iconRes = R.drawable.ic_emergency,
        intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:112")),
        onHomeByDefault = false,
    ),
)
