package com.coida.data

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.provider.CallLog
import androidx.core.content.ContextCompat
import com.coida.BuildConfig

/**
 * Cuántas llamadas perdidas hay sin ver, para la bola roja de la casilla
 * «Llamadas» (D-66).
 *
 * Leer el registro de llamadas pide `READ_CALL_LOG`, que Google Play no deja usar a
 * un launcher: por eso el permiso solo se declara en las variantes de depuración y
 * «familia», y en la release [isAvailable] es falso y todo esto no hace nada.
 *
 * «Sin ver» es lo que la propia app de teléfono marca como nuevo (`NEW = 1`): al
 * abrir su historial las da por vistas, y la bola se va sola.
 */
class MissedCallsRepository(context: Context) {

    private val appContext = context.applicationContext

    /** Esta variante trae el permiso en su manifiesto. */
    val isAvailable: Boolean get() = BuildConfig.MISSED_CALLS_ENABLED

    fun hasPermission(): Boolean =
        ContextCompat.checkSelfPermission(appContext, Manifest.permission.READ_CALL_LOG) ==
            PackageManager.PERMISSION_GRANTED

    /**
     * Las perdidas sin ver, o 0 si no se puede saber. Hace una consulta al proveedor
     * del registro: llamar fuera del hilo principal. Nunca lanza: un móvil que no deja
     * leer el registro se queda sin bola, no sin inicio.
     */
    fun missedCount(): Int {
        if (!isAvailable || !hasPermission()) return 0
        return runCatching {
            appContext.contentResolver.query(
                CallLog.Calls.CONTENT_URI,
                arrayOf(CallLog.Calls._ID),
                "${CallLog.Calls.TYPE} = ? AND ${CallLog.Calls.NEW} = 1",
                arrayOf(CallLog.Calls.MISSED_TYPE.toString()),
                null,
            )?.use { it.count } ?: 0
        }.getOrDefault(0)
    }
}
