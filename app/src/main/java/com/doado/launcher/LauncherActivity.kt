package com.doado.launcher

import android.content.ActivityNotFoundException
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.mutableStateOf
import com.doado.R
import com.doado.core.ui.DoadoTheme
import com.doado.data.FavoriteApp
import com.doado.data.InstalledAppsRepository

/**
 * Pantalla de inicio de Doado.
 *
 * Está declarada en el manifiesto con las categorías HOME + DEFAULT, así que el
 * sistema la ofrece como lanzador. Aquí solo vive lo que es puramente de Android
 * (PackageManager, Intents, Toasts); todo lo visual está en [HomeScreen].
 */
class LauncherActivity : ComponentActivity() {

    private lateinit var repository: InstalledAppsRepository
    private val favorites = mutableStateOf<List<FavoriteApp>>(emptyList())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // A partir de Android 15 el dibujado a pantalla completa es obligatorio;
        // el Scaffold de HomeScreen ya aparta el contenido de las barras.
        enableEdgeToEdge()

        repository = InstalledAppsRepository(this)
        favorites.value = repository.favorites()

        setContent {
            DoadoTheme {
                HomeScreen(
                    favorites = favorites.value,
                    onAppClick = ::openApp,
                )
            }
        }
    }

    /**
     * Se relee al volver a la pantalla de inicio: si mientras tanto se ha
     * instalado o desinstalado alguna app, las casillas se actualizan solas.
     */
    override fun onResume() {
        super.onResume()
        favorites.value = repository.favorites()
    }

    private fun openApp(app: FavoriteApp) {
        val intent = repository.launchIntentFor(app)
        if (intent == null) {
            showCannotOpen(app)
            return
        }
        try {
            startActivity(intent)
        } catch (_: ActivityNotFoundException) {
            // La app pudo desinstalarse entre que se pintó la casilla y el toque.
            showCannotOpen(app)
        }
    }

    private fun showCannotOpen(app: FavoriteApp) {
        Toast.makeText(
            this,
            getString(R.string.error_cannot_open, app.label),
            Toast.LENGTH_LONG,
        ).show()
    }
}
