// El aviso de lint salta en el import: ver rotateAsTaken.
@file:SuppressLint("ExifInterface")

package com.doado.data

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import java.io.File

/**
 * La foto de fondo del inicio que elige el familiar en Ajustes → Interfaz (D-47).
 *
 * Se guarda una copia propia en `files/`, reducida a lo que mide la pantalla: la
 * original puede pesar decenas de megas, y si el familiar la borra de la galería
 * el inicio no debe quedarse en blanco. No hay clave en las preferencias: si el
 * fichero está, hay foto.
 *
 * Fuera de la copia de seguridad (las reglas de `res/xml` son una lista blanca y
 * no la nombran): es una foto privada y pesa. En un móvil nuevo se vuelve a elegir.
 *
 * Todo lo de aquí lee o escribe disco: se llama fuera del hilo principal.
 */
class WallpaperStore(context: Context) {

    private val appContext = context.applicationContext
    private val file = File(appContext.filesDir, FILE_NAME)

    /** La foto guardada, o null si no hay o no se puede leer (el inicio sale sin foto). */
    fun load(): ImageBitmap? =
        runCatching { BitmapFactory.decodeFile(file.path)?.asImageBitmap() }.getOrNull()

    /**
     * Copia la foto de [uri] reducida para cubrir [width] × [height] píxeles, ya
     * girada según la guardó la cámara. Primero en un fichero aparte y luego se
     * cambia por el bueno, para que un fallo a medias no deje una foto rota.
     *
     * @return false si no se pudo leer o guardar; la foto anterior se queda.
     */
    fun save(uri: Uri, width: Int, height: Int): Boolean = runCatching {
        val resolver = appContext.contentResolver
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return false

        // La mayor reducción por potencias de dos que siga cubriendo la pantalla
        // (las fotos de móvil vienen tumbadas o de pie: se compara con el lado mayor).
        val target = maxOf(width, height)
        var sample = 1
        while (minOf(bounds.outWidth, bounds.outHeight) / (sample * 2) >= minOf(width, height) &&
            maxOf(bounds.outWidth, bounds.outHeight) / (sample * 2) >= target
        ) {
            sample *= 2
        }
        val decoded = resolver.openInputStream(uri)?.use {
            BitmapFactory.decodeStream(it, null, BitmapFactory.Options().apply { inSampleSize = sample })
        } ?: return false
        val upright = rotateAsTaken(decoded, uri)

        val temp = File(appContext.filesDir, "$FILE_NAME.tmp")
        temp.outputStream().use { upright.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, it) }
        temp.renameTo(file)
    }.getOrDefault(false)

    /** Si hay foto guardada. Solo mira si el fichero está: vale para el primer fotograma. */
    fun exists(): Boolean = file.exists()

    fun clear() {
        file.delete()
    }

    /**
     * Las cámaras guardan la foto tumbada y apuntan aparte cómo se giraba. Con el
     * ExifInterface de Android y no el de androidx, que sería una dependencia nueva:
     * para leer solo la orientación, el de Android basta desde la API 24.
     */
    private fun rotateAsTaken(bitmap: Bitmap, uri: Uri): Bitmap {
        val degrees = runCatching {
            appContext.contentResolver.openInputStream(uri)?.use {
                when (ExifInterface(it).getAttributeInt(ExifInterface.TAG_ORIENTATION, 0)) {
                    ExifInterface.ORIENTATION_ROTATE_90 -> 90f
                    ExifInterface.ORIENTATION_ROTATE_180 -> 180f
                    ExifInterface.ORIENTATION_ROTATE_270 -> 270f
                    else -> 0f
                }
            }
        }.getOrNull() ?: 0f
        if (degrees == 0f) return bitmap
        val matrix = Matrix().apply { postRotate(degrees) }
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    }

    private companion object {
        const val FILE_NAME = "home_wallpaper.jpg"
        const val JPEG_QUALITY = 85
    }
}
