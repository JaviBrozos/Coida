package com.doado.settings

import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.doado.R
import com.doado.core.ui.ChoiceGroup
import com.doado.core.ui.DoadoCard
import com.doado.core.ui.DoadoDimens
import androidx.compose.foundation.shape.RoundedCornerShape
import com.doado.core.ui.NoteCard
import com.doado.core.ui.OutlineButton
import com.doado.core.ui.ScreenHeader
import com.doado.core.ui.SectionLabel
import com.doado.core.ui.SettingsColumn
import com.doado.core.ui.SwitchRow
import com.doado.data.AppIcon
import com.doado.data.ContactsLayout
import com.doado.data.FavoriteApp
import com.doado.launcher.AppTile
import com.doado.launcher.gridSpacing
import com.doado.data.InterfaceSettings
import com.doado.data.TextSize
import com.doado.data.ThemeMode

/**
 * Ajustes → Interfaz. Cada cambio se guarda y se aplica en el acto, a toda la
 * app: quien configura ve el resultado mientras toca, sin botón de "guardar".
 *
 * @param wallpaper la foto de fondo del inicio, si hay (D-47).
 * @param wallpaperFailed la última foto elegida no se pudo usar: se avisa hasta
 *   que se elija otra o se salga.
 */
@Composable
fun InterfaceScreen(
    settings: InterfaceSettings,
    onChange: (InterfaceSettings) -> Unit,
    onBack: () -> Unit,
    wallpaper: ImageBitmap? = null,
    wallpaperFailed: Boolean = false,
    onPickWallpaper: () -> Unit = {},
    onRemoveWallpaper: () -> Unit = {},
) {
    SettingsColumn(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
    ) {
        ScreenHeader(
            title = stringResource(R.string.settings_interface),
            subtitle = stringResource(R.string.interface_subtitle),
            backLabel = stringResource(R.string.cd_back_settings),
            onBack = onBack,
        )

        SectionLabel(stringResource(R.string.interface_text_size))
        ChoiceGroup(
            options = listOf(
                stringResource(R.string.interface_text_normal),
                stringResource(R.string.interface_text_large),
                stringResource(R.string.interface_text_extra_large),
                stringResource(R.string.interface_text_huge),
            ),
            selectedIndex = settings.textSize.ordinal,
            onSelect = { onChange(settings.copy(textSize = TextSize.entries[it])) },
        )

        // Las cuatro opciones se ofrecen siempre, mida lo que mida la pantalla
        // (D-54): antes, en un móvil de menos de 360 dp desaparecía la de tres, y
        // en el Samsung de la prueba solo se podían poner dos por fila.
        SectionLabel(stringResource(R.string.interface_columns))
        ChoiceGroup(
            options = listOf(
                stringResource(R.string.interface_columns_one),
                stringResource(R.string.interface_columns_two),
                stringResource(R.string.interface_columns_three),
                stringResource(R.string.interface_columns_four),
            ),
            selectedIndex = settings.columns - 1,
            onSelect = { onChange(settings.copy(columns = it + 1)) },
        )
        ColumnsPreview(columns = settings.columns)

        SectionLabel(stringResource(R.string.interface_contacts))
        ChoiceGroup(
            options = listOf(
                stringResource(R.string.interface_contacts_grid),
                stringResource(R.string.interface_contacts_page),
            ),
            selectedIndex = settings.contactsLayout.ordinal,
            onSelect = { onChange(settings.copy(contactsLayout = ContactsLayout.entries[it])) },
        )
        if (settings.contactsLayout == ContactsLayout.PAGE) {
            NoteCard(text = stringResource(R.string.interface_contacts_page_note))
        }

        SectionLabel(stringResource(R.string.interface_wallpaper))
        WallpaperSection(
            wallpaper = wallpaper,
            failed = wallpaperFailed,
            onPick = onPickWallpaper,
            onRemove = onRemoveWallpaper,
        )

        SectionLabel(stringResource(R.string.interface_theme))
        ChoiceGroup(
            options = listOf(
                stringResource(R.string.interface_theme_dark),
                stringResource(R.string.interface_theme_light),
            ),
            selectedIndex = settings.theme.ordinal,
            onSelect = { onChange(settings.copy(theme = ThemeMode.entries[it])) },
        )

        SwitchRow(
            title = stringResource(R.string.interface_clock),
            subtitle = stringResource(R.string.interface_clock_subtitle),
            checked = settings.showClock,
            onCheckedChange = { onChange(settings.copy(showClock = it)) },
            modifier = Modifier.padding(top = 8.dp),
        )

        NoteCard(text = stringResource(R.string.interface_note))
    }
}

/**
 * La foto de fondo del inicio: una muestra, elegirla o cambiarla, y quitarla. La
 * elige el familiar con el selector de fotos de Android, que no pide permisos.
 */
@Composable
private fun WallpaperSection(
    wallpaper: ImageBitmap?,
    failed: Boolean,
    onPick: () -> Unit,
    onRemove: () -> Unit,
) {
    if (wallpaper != null) {
        // La muestra es decorativa: los botones de debajo ya dicen qué hay.
        Image(
            bitmap = wallpaper,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxWidth()
                .height(WALLPAPER_PREVIEW_HEIGHT)
                .clip(RoundedCornerShape(DoadoDimens.CardCornerRadius))
                .border(
                    DoadoDimens.BorderWidth,
                    MaterialTheme.colorScheme.outline,
                    RoundedCornerShape(DoadoDimens.CardCornerRadius),
                ),
        )
    }
    if (failed) NoteCard(text = stringResource(R.string.interface_wallpaper_failed))
    OutlineButton(
        text = stringResource(
            if (wallpaper == null) R.string.interface_wallpaper_pick else R.string.interface_wallpaper_change,
        ),
        icon = R.drawable.ic_camera,
        onClick = onPick,
    )
    if (wallpaper != null) {
        OutlineButton(
            text = stringResource(R.string.interface_wallpaper_remove),
            icon = R.drawable.ic_delete,
            onClick = onRemove,
        )
        NoteCard(text = stringResource(R.string.interface_wallpaper_note))
    }
}

private val WALLPAPER_PREVIEW_HEIGHT = 160.dp

/**
 * Muestra cómo quedan las casillas, con las casillas de verdad del inicio en
 * pequeño: con una por fila, dos filas; con más, una fila entera. El tamaño de
 * letra y el tema no necesitan maqueta: se aplican ya a esta misma pantalla.
 */
@Composable
private fun ColumnsPreview(columns: Int) {
    val samples = listOf(
        PreviewApp("phone", R.drawable.ic_call, stringResource(R.string.app_phone)),
        PreviewApp("messages", R.drawable.ic_messages, stringResource(R.string.app_messages)),
        PreviewApp("camera", R.drawable.ic_camera, stringResource(R.string.app_camera)),
        PreviewApp("contacts", R.drawable.ic_contacts, stringResource(R.string.app_contacts)),
    ).map { FavoriteApp(it.id, it.label, AppIcon.Vector(it.icon), PREVIEW_PACKAGE) }
    // Como en el inicio: casillas sobre el fondo de pantalla. Decorativa: TalkBack
    // ya ha leído las opciones de encima.
    DoadoCard(
        onClick = null,
        modifier = Modifier
            .fillMaxWidth()
            .clearAndSetSemantics {},
        color = MaterialTheme.colorScheme.background,
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(gridSpacing(columns)),
        ) {
            if (columns == 1) {
                samples.take(2).forEach { AppTile(app = it, horizontal = true, onClick = null) }
            } else {
                Row(horizontalArrangement = Arrangement.spacedBy(gridSpacing(columns))) {
                    samples.take(columns).forEach {
                        AppTile(app = it, horizontal = false, onClick = null, modifier = Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

private class PreviewApp(val id: String, val icon: Int, val label: String)

/** Paquete de mentira: la maqueta pinta las casillas encendidas, sin ir a Android. */
private const val PREVIEW_PACKAGE = "preview"
