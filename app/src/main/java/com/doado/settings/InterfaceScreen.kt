package com.doado.settings

import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.Hyphens
import androidx.compose.ui.text.style.LineBreak
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.doado.R
import com.doado.core.ui.ChoiceGroup
import com.doado.core.ui.DoadoCard
import com.doado.core.ui.DoadoDimens
import com.doado.core.ui.NarrowTextScale
import com.doado.core.ui.IconBadge
import androidx.compose.foundation.shape.RoundedCornerShape
import com.doado.core.ui.NoteCard
import com.doado.core.ui.OutlineButton
import com.doado.core.ui.ScreenHeader
import com.doado.core.ui.SectionLabel
import com.doado.core.ui.SettingsColumn
import com.doado.core.ui.SwitchRow
import com.doado.data.ContactsLayout
import com.doado.data.InterfaceSettings
import com.doado.data.TextSize
import com.doado.data.ThemeMode

/** Ancho mínimo de una casilla para ofrecer tres por fila (D-45, RNF-ACC-02). */
private val NARROW_TILE_MIN_WIDTH = 96.dp

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
            ),
            selectedIndex = settings.textSize.ordinal,
            onSelect = { onChange(settings.copy(textSize = TextSize.entries[it])) },
        )

        // Tres por fila solo si cada casilla queda en 96 dp o más (D-45). Si ya
        // estaba puesto (una copia de un móvil más ancho), se sigue enseñando.
        val screenWidth = with(LocalDensity.current) { LocalWindowInfo.current.containerSize.width.toDp() }
        val threeFit = (screenWidth - DoadoDimens.ScreenPadding * 2 - DoadoDimens.GridSpacing * 2) / 3 >=
            NARROW_TILE_MIN_WIDTH
        val offerThree = threeFit || settings.columns == 3
        val bigText = LocalDensity.current.fontScale > DoadoDimens.NarrowMaxTextScale

        SectionLabel(stringResource(R.string.interface_columns))
        ChoiceGroup(
            options = listOfNotNull(
                stringResource(R.string.interface_columns_one),
                stringResource(R.string.interface_columns_two),
                if (offerThree) stringResource(R.string.interface_columns_three) else null,
            ),
            selectedIndex = settings.columns - 1,
            onSelect = { onChange(settings.copy(columns = it + 1)) },
        )
        ColumnsPreview(columns = settings.columns)
        when {
            !threeFit -> NoteCard(text = stringResource(R.string.interface_columns_three_narrow))
            settings.columns == 3 && bigText ->
                NoteCard(text = stringResource(R.string.interface_columns_three_big_text))
        }

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
 * Muestra en pequeño cómo quedan las casillas. El tamaño de letra y el tema no
 * necesitan maqueta: se aplican ya a esta misma pantalla.
 */
@Composable
private fun ColumnsPreview(columns: Int) {
    val samples = listOf(
        R.drawable.ic_call to R.string.app_phone,
        R.drawable.ic_messages to R.string.app_messages,
        R.drawable.ic_camera to R.string.app_camera,
    ).take(maxOf(columns, 2))
    // Como en el inicio: casillas sobre el fondo de pantalla.
    DoadoCard(onClick = null, modifier = Modifier.fillMaxWidth(), color = MaterialTheme.colorScheme.background) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            if (columns == 1) {
                samples.forEach { (icon, label) -> MiniTile(icon, label, horizontal = true, Modifier.fillMaxWidth()) }
            } else {
                // Con tres, el mismo tope de letra que en el inicio (D-45): la maqueta
                // enseña lo que se va a ver.
                NarrowTextScale(enabled = columns >= 3) {
                    androidx.compose.foundation.layout.Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        samples.forEach { (icon, label) ->
                            MiniTile(icon, label, horizontal = false, Modifier.weight(1f))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MiniTile(icon: Int, label: Int, horizontal: Boolean, modifier: Modifier) {
    DoadoCard(
        onClick = null,
        modifier = modifier.heightIn(min = 72.dp),
        shape = RoundedCornerShape(DoadoDimens.ControlCornerRadius),
    ) {
        val content: @Composable () -> Unit = {
            IconBadge(icon = icon, size = 44.dp)
            // Dos líneas y con guion: con tres por fila y la letra grande, una sola
            // cortaba «Mensajes» en «Mens…».
            Text(
                text = stringResource(label),
                style = MaterialTheme.typography.labelSmall.copy(
                    hyphens = Hyphens.Auto,
                    lineBreak = LineBreak.Paragraph,
                ),
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
        if (horizontal) {
            androidx.compose.foundation.layout.Row(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp),
            ) { content() }
        } else {
            Column(
                modifier = Modifier.padding(10.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterVertically),
            ) { content() }
        }
    }
}
