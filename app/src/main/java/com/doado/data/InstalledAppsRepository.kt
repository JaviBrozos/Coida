package com.doado.data

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.os.Build

/**
 * Única puerta de entrada al PackageManager.
 *
 * Ojo: los métodos de consulta hacen trabajo de disco (IPC con el system_server),
 * así que no conviene llamarlos en cada recomposición. En el MVP se resuelven las
 * cuatro casillas una sola vez al crear la Activity.
 */
class InstalledAppsRepository(context: Context) {

    private val appContext = context.applicationContext
    private val packageManager: PackageManager = appContext.packageManager
    private val favoritesStore = FavoritesStore(appContext)

    /**
     * Favoritos listos para pintar, en el orden guardado por el usuario y ya
     * resueltos contra las apps que realmente hay en este móvil.
     */
    fun favorites(): List<FavoriteApp> {
        val templatesById = DefaultFavorites.associateBy { it.id }
        return favoritesStore.favoriteIds()
            .mapNotNull { templatesById[it] }
            .map { template ->
                FavoriteApp(
                    id = template.id,
                    label = appContext.getString(template.labelRes),
                    iconRes = template.iconRes,
                    packageName = resolve(template.intent)?.activityInfo?.packageName,
                )
            }
    }

    /**
     * Intención con la que abrir un favorito, o null si en este móvil no hay
     * ninguna app que sepa atenderlo.
     */
    fun launchIntentFor(favorite: FavoriteApp): Intent? {
        val template = DefaultFavorites.firstOrNull { it.id == favorite.id } ?: return null
        if (resolve(template.intent) == null) return null

        // Copia defensiva: la plantilla es compartida y no debe salir de aquí con
        // flags añadidos. NEW_TASK porque el lanzador abre la app en su propia tarea.
        return Intent(template.intent).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }

    /**
     * Todas las apps del móvil que se pueden abrir. Todavía no se usa en la
     * pantalla de inicio: es la base de la futura pantalla de elegir favoritos.
     */
    fun installedApps(): List<InstalledApp> {
        val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        return queryAll(intent)
            .map { info ->
                InstalledApp(
                    packageName = info.activityInfo.packageName,
                    activityName = info.activityInfo.name,
                    label = info.loadLabel(packageManager).toString(),
                )
            }
            .filter { it.packageName != appContext.packageName }
            .sortedBy { it.label.lowercase() }
    }

    // El PackageManager cambió de firma en Android 13; envolvemos la diferencia
    // aquí para no repartir @Suppress("DEPRECATION") por todo el repositorio.
    private fun resolve(intent: Intent): ResolveInfo? =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            packageManager.resolveActivity(intent, PackageManager.ResolveInfoFlags.of(0L))
        } else {
            @Suppress("DEPRECATION")
            packageManager.resolveActivity(intent, 0)
        }

    private fun queryAll(intent: Intent): List<ResolveInfo> =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            packageManager.queryIntentActivities(intent, PackageManager.ResolveInfoFlags.of(0L))
        } else {
            @Suppress("DEPRECATION")
            packageManager.queryIntentActivities(intent, 0)
        }
}
