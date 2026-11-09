package com.coida.data

import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.net.Uri
import android.os.Build
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.graphics.drawable.toBitmap
import com.coida.R

/**
 * Única puerta de entrada al PackageManager.
 *
 * Ojo: los métodos de consulta hacen trabajo de disco (IPC con el system_server),
 * así que no conviene llamarlos en cada recomposición. La Activity los resuelve
 * al arrancar y cada vez que se vuelve a la pantalla de inicio.
 */
class InstalledAppsRepository(context: Context) {

    private val appContext = context.applicationContext
    private val packageManager: PackageManager = appContext.packageManager
    private val favoritesStore = FavoritesStore(appContext)

    /**
     * Casillas listas para pintar, en el orden guardado y ya resueltas contra las
     * apps que realmente hay en este móvil.
     */
    fun homeApps(): List<FavoriteApp> {
        val templatesById = DefaultFavorites.associateBy { it.id }
        return favoritesStore.favoriteIds().mapNotNull { id ->
            val template = templatesById[id]
            when {
                template != null -> FavoriteApp(
                    id = template.id,
                    label = appContext.getString(template.labelRes),
                    icon = AppIcon.Vector(template.iconRes),
                    packageName = resolve(template.intent)?.activityInfo?.packageName,
                )

                id.startsWith(FavoritesStore.PACKAGE_PREFIX) ->
                    packageApp(id.removePrefix(FavoritesStore.PACKAGE_PREFIX))

                else -> null
            }
        }
    }

    fun homeIds(): List<String> = favoritesStore.favoriteIds()

    /** Pone o quita una casilla. Las nuevas van al final: nada cambia de sitio. */
    fun setOnHome(id: String, onHome: Boolean) {
        val current = favoritesStore.favoriteIds()
        val updated = if (onHome) current + id else current - id
        favoritesStore.saveFavoriteIds(updated)
    }

    /**
     * Guarda el orden en que el familiar ha colocado las casillas (D-59). Es lo
     * único que las mueve: nada cambia de sitio por sí solo. Un id guardado que no
     * venga en [orderedIds] se queda detrás, en el orden que tenía.
     */
    fun saveHomeOrder(orderedIds: List<String>) {
        val current = favoritesStore.favoriteIds()
        val placed = orderedIds.filter { it in current }
        favoritesStore.saveFavoriteIds(placed + current.filterNot { it in placed })
    }

    /**
     * Intención con la que abrir una casilla, o null si en este móvil no hay
     * ninguna app que sepa atenderla.
     */
    fun launchIntentFor(app: FavoriteApp): Intent? {
        val template = DefaultFavorites.firstOrNull { it.id == app.id }
        val intent = when {
            template != null -> template.intent.takeIf { resolve(it) != null }?.let(::Intent)
            app.id.startsWith(FavoritesStore.PACKAGE_PREFIX) ->
                packageManager.getLaunchIntentForPackage(
                    app.id.removePrefix(FavoritesStore.PACKAGE_PREFIX),
                )

            else -> null
        }
        // Copia defensiva arriba: la plantilla es compartida y no debe salir de aquí
        // con flags añadidos. NEW_TASK porque el lanzador abre la app en su tarea.
        return intent?.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }

    /** Paquetes que atienden las casillas de fábrica en este móvil (el marcador, la cámara...). */
    fun defaultFavoritePackages(): Set<String> =
        DefaultFavorites.mapNotNull { resolve(it.intent)?.activityInfo?.packageName }.toSet()

    fun isInstalled(packageName: String): Boolean =
        packageManager.getLaunchIntentForPackage(packageName) != null

    /**
     * Ficha de la app en Google Play. Si el móvil no tiene Play Store (pasa en
     * algunos emuladores), la misma ficha en el navegador.
     */
    fun storeIntents(packageName: String): List<Intent> = listOf(
        Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=$packageName")),
        Intent(
            Intent.ACTION_VIEW,
            Uri.parse("https://play.google.com/store/apps/details?id=$packageName"),
        ),
    ).map { it.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) }

    /** Todas las apps del móvil que se pueden abrir, sin contar Coida. */
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
            .distinctBy { it.packageName }
            .sortedBy { it.label.lowercase() }
    }

    private fun packageApp(packageName: String): FavoriteApp {
        val id = FavoritesStore.packageId(packageName)
        val info = applicationInfo(packageName)
        if (info == null || !isInstalled(packageName)) {
            // Desinstalada: la casilla se queda en su sitio, apagada. Android ya no
            // sabe su nombre; si Coida tampoco, un texto llano y no «com.algo.app».
            val label = KnownAppLabels[packageName]
                ?: appContext.getString(R.string.app_removed_label)
            return FavoriteApp(id, label, AppIcon.Vector(R.drawable.ic_apps), null)
        }
        // Algún tema de iconos de fabricante da un dibujo que no se deja pasar a
        // mapa de bits; antes eso cerraba Coida. Mejor el icono genérico.
        val icon = runCatching {
            AppIcon.Bitmap(info.loadIcon(packageManager).toBitmap(ICON_PX, ICON_PX).asImageBitmap())
        }.getOrElse { AppIcon.Vector(R.drawable.ic_apps) }
        return FavoriteApp(
            id = id,
            label = info.loadLabel(packageManager).toString(),
            icon = icon,
            packageName = packageName,
        )
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

    private fun applicationInfo(packageName: String): ApplicationInfo? = try {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            packageManager.getApplicationInfo(
                packageName,
                PackageManager.ApplicationInfoFlags.of(0L),
            )
        } else {
            @Suppress("DEPRECATION")
            packageManager.getApplicationInfo(packageName, 0)
        }
    } catch (_: PackageManager.NameNotFoundException) {
        null
    }

    private companion object {
        /**
         * Lado del icono real de una app. El más grande de una casilla mide 96 dp
         * (`CoidaDimens.TileIconMax`), que en un móvil xxhdpi son 288 px: con menos
         * se estiraba y se veía borroso.
         */
        const val ICON_PX = 288
    }
}
