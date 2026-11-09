package com.coida.data

import android.Manifest
import android.content.ContentUris
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.net.Uri
import android.provider.ContactsContract
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.content.ContextCompat

/**
 * Las fotos de la agenda de las personas guardadas en Coida (D-57).
 *
 * Con `READ_CONTACTS` se podría leer la agenda entera; aquí solo se pregunta por
 * los números que el familiar ya eligió, uno a uno. Las fotos viven en memoria:
 * no se copian a disco ni salen del móvil, y al asistente solo le llegan nombres.
 * Se vuelven a leer cada vez que se vuelve al inicio, así que si cambia la foto en
 * la agenda, cambia aquí.
 *
 * Todo lo de aquí hace IPC con la agenda: se llama fuera del hilo principal.
 */
class ContactPhotos(context: Context) {

    private val appContext = context.applicationContext

    /**
     * Las fotos ya leídas, por número, con el id de foto de la agenda. Si el id no ha
     * cambiado se devuelve la misma: no se decodifica en cada vuelta al inicio, y la
     * pantalla no ve una foto «nueva» que en realidad es la misma.
     */
    private val cache = mutableMapOf<String, Pair<Long, ImageBitmap>>()

    fun hasPermission(): Boolean =
        ContextCompat.checkSelfPermission(appContext, Manifest.permission.READ_CONTACTS) ==
            PackageManager.PERMISSION_GRANTED

    /**
     * La foto de cada número de [numbers] que la tenga, por número tal y como está
     * guardado. Sin permiso, o si la agenda falla, un mapa vacío: salen las iniciales.
     * Sincronizado: dos vueltas seguidas al inicio pueden solaparse, y comparten la caché.
     */
    @Synchronized
    fun load(numbers: Collection<String>): Map<String, ImageBitmap> {
        if (!hasPermission()) return emptyMap()
        return numbers.distinct().mapNotNull { number ->
            runCatching { photoFor(number) }.getOrNull()?.let { number to it }
        }.toMap()
    }

    private fun photoFor(number: String): ImageBitmap? {
        val resolver = appContext.contentResolver
        val lookup = Uri.withAppendedPath(
            ContactsContract.PhoneLookup.CONTENT_FILTER_URI,
            Uri.encode(number),
        )
        val (contactId, photoId) = resolver.query(
            lookup,
            arrayOf(ContactsContract.PhoneLookup._ID, ContactsContract.PhoneLookup.PHOTO_ID),
            null,
            null,
            null,
        )?.use { if (it.moveToFirst()) it.getLong(0) to it.getLong(1) else null } ?: return null
        // Sin foto en la agenda (PHOTO_ID vacío sale como 0).
        if (photoId == 0L) return null.also { cache.remove(number) }
        cache[number]?.let { (cachedId, bitmap) -> if (cachedId == photoId) return bitmap }
        val contactUri = ContentUris.withAppendedId(ContactsContract.Contacts.CONTENT_URI, contactId)

        // La foto grande, si la hay, porque la miniatura (96 px) se ve borrosa en una
        // casilla. Se reduce por potencias de dos hasta unos PHOTO_PX de lado.
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        ContactsContract.Contacts.openContactPhotoInputStream(resolver, contactUri, true)
            ?.use { BitmapFactory.decodeStream(it, null, bounds) } ?: return null
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null
        var sample = 1
        while (minOf(bounds.outWidth, bounds.outHeight) / (sample * 2) >= PHOTO_PX) sample *= 2
        val photo = ContactsContract.Contacts.openContactPhotoInputStream(resolver, contactUri, true)?.use {
            BitmapFactory.decodeStream(it, null, BitmapFactory.Options().apply { inSampleSize = sample })
        }?.asImageBitmap() ?: return null
        cache[number] = photoId to photo
        return photo
    }

    private companion object {
        /** Lado al que se reduce la foto: de sobra para la casilla más grande. */
        const val PHOTO_PX = 256
    }
}
