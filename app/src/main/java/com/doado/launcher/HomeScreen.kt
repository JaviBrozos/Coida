package com.doado.launcher

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.doado.R
import com.doado.core.ui.DoadoDimens
import com.doado.core.ui.DoadoTheme
import com.doado.data.FavoriteApp

/**
 * Pantalla de inicio de Doado.
 *
 * Es deliberadamente tonta: recibe la lista ya resuelta y avisa de los toques
 * hacia arriba. Así se puede previsualizar y probar sin PackageManager, y toda la
 * fontanería de Android se queda en [LauncherActivity].
 *
 * Dos columnas y no tres: con tres, cada casilla se queda por debajo del tamaño
 * al que un icono se distingue con la vista cansada.
 */
@Composable
fun HomeScreen(
    favorites: List<FavoriteApp>,
    onAppClick: (FavoriteApp) -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
    ) { innerPadding ->
        LazyVerticalGrid(
            columns = GridCells.Fixed(COLUMNS),
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(DoadoDimens.ScreenPadding),
            horizontalArrangement = Arrangement.spacedBy(DoadoDimens.GridSpacing),
            verticalArrangement = Arrangement.spacedBy(DoadoDimens.GridSpacing),
        ) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                HomeHeader()
            }

            items(items = favorites, key = { it.id }) { app ->
                AppTile(
                    app = app,
                    onClick = { onAppClick(app) },
                )
            }
        }
    }
}

/** Cabecera con el nombre de la app y su lema. */
@Composable
private fun HomeHeader(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(bottom = 12.dp),
    ) {
        Text(
            text = stringResource(R.string.app_name),
            style = MaterialTheme.typography.displaySmall,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.semantics { heading() },
        )
        Text(
            text = stringResource(R.string.app_tagline),
            style = MaterialTheme.typography.bodyMedium,
            // Cian oscurecido: ~4.6:1 sobre blanco, cumple AA aun siendo secundario.
            color = MaterialTheme.colorScheme.secondary,
            modifier = Modifier.padding(top = 4.dp),
        )
    }
}

private const val COLUMNS = 2

@Preview(showBackground = true, widthDp = 411, heightDp = 891)
@Composable
private fun HomeScreenPreview() {
    DoadoTheme {
        HomeScreen(
            favorites = listOf(
                FavoriteApp("phone", "Llamar", R.drawable.ic_call, "com.android.dialer"),
                FavoriteApp("messages", "Mensajes", R.drawable.ic_messages, "com.android.mms"),
                FavoriteApp("camera", "Cámara", R.drawable.ic_camera, "com.android.camera"),
                FavoriteApp("contacts", "Contactos", R.drawable.ic_contacts, null),
            ),
            onAppClick = {},
        )
    }
}
