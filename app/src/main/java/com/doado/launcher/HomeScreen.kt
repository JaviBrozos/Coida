package com.doado.launcher

import android.text.format.DateFormat
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import com.doado.R
import com.doado.assistant.ui.AssistantButton
import com.doado.core.ui.DoadoCard
import com.doado.core.ui.DoadoDimens
import com.doado.core.ui.DoadoTheme
import com.doado.data.AppIcon
import com.doado.data.ContactAction
import com.doado.data.ContactShortcut
import com.doado.data.FavoriteApp
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import kotlin.math.ceil

/** Un contacto listo para pintar en el inicio. */
data class HomeContact(
    val shortcut: ContactShortcut,
    /** false si la acción no se puede hacer aquí (WhatsApp sin instalar). */
    val isAvailable: Boolean,
)

/**
 * Pantalla de inicio de Doado.
 *
 * Es deliberadamente tonta: recibe todo ya resuelto y avisa de los toques hacia
 * arriba. Así se puede previsualizar y probar sin PackageManager, y toda la
 * fontanería de Android se queda en [LauncherActivity].
 *
 * De arriba abajo: reloj (o el nombre, si se ha quitado el reloj) con el botón de
 * Ajustes en la esquina, contactos fijados, las casillas de apps y, si el familiar
 * lo ha encendido, el círculo del asistente flotando abajo en el centro (D-28).
 *
 * El círculo no se desplaza con la rejilla, y la columna le reserva su franja al
 * final: así nunca tapa una casilla, ni en reposo ni al desplazar.
 *
 * @param highlightedId casilla o contacto que el asistente acaba de señalar al
 *   guiar: se pinta con borde grueso, sin moverlo.
 */
@Composable
fun HomeScreen(
    apps: List<FavoriteApp>,
    contacts: List<HomeContact>,
    columns: Int,
    showClock: Boolean,
    adminUnlockedUntil: Long,
    showAssistant: Boolean,
    highlightedId: String?,
    onAppClick: (FavoriteApp) -> Unit,
    onContactClick: (ContactShortcut) -> Unit,
    onSettingsClick: () -> Unit,
    onAssistantClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
    ) { innerPadding ->
        val now = rememberCurrentMinute()
        val context = LocalContext.current
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(DoadoDimens.ScreenPadding),
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(DoadoDimens.GridSpacing),
            ) {
                HomeHeader(now = now, showClock = showClock, onSettingsClick = onSettingsClick)

                if (adminUnlockedUntil > now) {
                    val until = remember(adminUnlockedUntil) {
                        DateFormat.getTimeFormat(context).format(Date(adminUnlockedUntil))
                    }
                    AdminBanner(untilLabel = until, onClick = onSettingsClick)
                }

                if (contacts.isNotEmpty()) {
                    ContactsBlock(
                        contacts = contacts,
                        highlightedId = highlightedId,
                        onContactClick = onContactClick,
                    )
                }

                AppsGrid(
                    apps = apps,
                    columns = columns,
                    highlightedId = highlightedId,
                    onAppClick = onAppClick,
                    modifier = Modifier.weight(1f),
                )

                // La franja del círculo: la rejilla acaba encima y nada queda debajo.
                if (showAssistant) Spacer(Modifier.height(DoadoDimens.AssistantButtonSize))
            }

            if (showAssistant) {
                AssistantButton(
                    onClick = onAssistantClick,
                    modifier = Modifier.align(Alignment.BottomCenter),
                )
            }
        }
    }
}

/**
 * Con techo de escala propio, más bajo que el del resto de la app: la hora y la
 * fecha crecían con la letra igual que todo lo demás y a 200 % se comían más de
 * media pantalla, dejando la rejilla en una franja. Se reconocen aunque no sean
 * enormes; lo que la persona mayor tiene que poder leer y pulsar son las casillas.
 */
@Composable
private fun HomeHeader(now: Long, showClock: Boolean, onSettingsClick: () -> Unit) {
    val density = LocalDensity.current
    val capped = Density(density.density, density.fontScale.coerceAtMost(HEADER_MAX_TEXT_SCALE))
    CompositionLocalProvider(LocalDensity provides capped) {
        HomeHeaderContent(now = now, showClock = showClock, onSettingsClick = onSettingsClick)
    }
}

@Composable
private fun HomeHeaderContent(now: Long, showClock: Boolean, onSettingsClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            if (showClock) {
                val context = LocalContext.current
                val locale = LocalConfiguration.current.locales[0]
                val time = remember(now) { DateFormat.getTimeFormat(context).format(Date(now)) }
                val date = remember(now, locale) {
                    val pattern = DateFormat.getBestDateTimePattern(locale, "EEEEdMMMM")
                    SimpleDateFormat(pattern, locale).format(Date(now))
                }
                Text(
                    text = time,
                    style = MaterialTheme.typography.displayMedium,
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.semantics { heading() },
                )
                Text(
                    text = date,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                Text(
                    text = stringResource(R.string.app_name),
                    style = MaterialTheme.typography.displaySmall,
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.semantics { heading() },
                )
                Text(
                    text = stringResource(R.string.app_tagline),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        SettingsCornerButton(onClick = onSettingsClick)
    }
}

/**
 * Entrada a Ajustes: pequeña, en tono apagado y lejos del pulgar, porque es del
 * familiar y no de la persona mayor (D-28). Ocupa el sitio que tenía el isotipo.
 * Solo icono: la etiqueta la lleva para TalkBack.
 */
@Composable
private fun SettingsCornerButton(onClick: () -> Unit) {
    val label = stringResource(R.string.cd_open_settings)
    DoadoCard(
        onClick = onClick,
        color = Color.Transparent,
        shape = RoundedCornerShape(DoadoDimens.ControlCornerRadius),
        modifier = Modifier
            .size(DoadoDimens.CornerButtonSize)
            .semantics { contentDescription = label },
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                painter = painterResource(R.drawable.ic_settings),
                contentDescription = null,
                modifier = Modifier.size(30.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/**
 * Hora actual redondeada al minuto, que se refresca sola en el cambio de minuto.
 * Solo corre con la pantalla a la vista: al volver de estar bloqueado, se
 * recalcula en el acto en vez de enseñar la hora de cuando se apagó.
 */
@Composable
private fun rememberCurrentMinute(): Long {
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    LaunchedEffect(lifecycle) {
        lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            while (true) {
                val current = System.currentTimeMillis()
                now = current
                delay(MINUTE_MS - current % MINUTE_MS + 50)
            }
        }
    }
    return now
}

@Composable
private fun AdminBanner(untilLabel: String, onClick: () -> Unit) {
    val clickLabel = stringResource(R.string.cd_admin_banner)
    DoadoCard(
        onClick = onClick,
        highlighted = true,
        modifier = Modifier
            .fillMaxWidth()
            .semantics { onClick(label = clickLabel) { onClick(); true } },
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 18.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_shield),
                contentDescription = null,
                modifier = Modifier.size(28.dp),
                tint = MaterialTheme.colorScheme.primary,
            )
            Text(
                text = stringResource(R.string.home_admin_banner, untilLabel),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}

/** Contactos fijados, de dos en dos. */
@Composable
private fun ContactsBlock(
    contacts: List<HomeContact>,
    highlightedId: String?,
    onContactClick: (ContactShortcut) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        contacts.chunked(2).forEach { pair ->
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                pair.forEach { contact ->
                    ContactTile(
                        contact = contact,
                        highlighted = contact.shortcut.id == highlightedId,
                        onClick = { onContactClick(contact.shortcut) },
                        modifier = Modifier.weight(1f),
                    )
                }
                // Con un número impar, el último no se estira: mantiene su tamaño.
                if (pair.size == 1) Spacer(Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun ContactTile(
    contact: HomeContact,
    highlighted: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val shortcut = contact.shortcut
    val colors = MaterialTheme.colorScheme
    val actionText = stringResource(
        when (shortcut.action) {
            ContactAction.CALL -> R.string.contact_action_call
            ContactAction.WHATSAPP -> R.string.contact_action_whatsapp
        },
    )
    val clickLabel = stringResource(R.string.cd_contact_action, actionText, shortcut.name)
    val unavailableLabel = stringResource(R.string.cd_contact_unavailable, shortcut.name)
    val highlightedLabel = stringResource(R.string.cd_highlighted)
    // Señalado por el asistente: relleno de acento, como las casillas (AppTile).
    val contentColor = when {
        !contact.isAvailable -> colors.outlineVariant
        highlighted -> colors.onPrimary
        else -> colors.onSurface
    }
    val secondaryColor = when {
        !contact.isAvailable -> colors.outlineVariant
        highlighted -> colors.onPrimary
        else -> colors.onSurfaceVariant
    }

    DoadoCard(
        onClick = onClick,
        enabled = contact.isAvailable,
        highlighted = highlighted,
        color = if (highlighted) colors.primary else colors.surfaceContainer,
        borderWidth = if (highlighted) DoadoDimens.HighlightBorderWidth else DoadoDimens.BorderWidth,
        modifier = modifier
            .heightIn(min = 104.dp)
            .semantics {
                if (contact.isAvailable) {
                    onClick(label = clickLabel) { onClick(); true }
                } else {
                    contentDescription = unavailableLabel
                }
                if (highlighted) stateDescription = highlightedLabel
            },
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .background(colors.surfaceContainerHigh, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    painter = painterResource(
                        if (shortcut.action == ContactAction.WHATSAPP) R.drawable.ic_messages
                        else R.drawable.ic_call,
                    ),
                    contentDescription = null,
                    modifier = Modifier.size(28.dp),
                    tint = if (contact.isAvailable) colors.primary else colors.outlineVariant,
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = shortcut.name,
                    style = MaterialTheme.typography.titleSmall,
                    color = contentColor,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = if (contact.isAvailable) actionText else stringResource(R.string.app_unavailable),
                    style = MaterialTheme.typography.labelSmall,
                    color = secondaryColor,
                )
            }
        }
    }
}

/**
 * La rejilla de apps ocupa todo el alto que queda: con pocas apps las casillas
 * crecen (hasta un tope, para que no parezcan carteles) y con muchas bajan hasta
 * el mínimo y la rejilla pasa a desplazarse.
 */
@Composable
private fun AppsGrid(
    apps: List<FavoriteApp>,
    columns: Int,
    highlightedId: String?,
    onAppClick: (FavoriteApp) -> Unit,
    modifier: Modifier = Modifier,
) {
    BoxWithConstraints(modifier = modifier.fillMaxWidth()) {
        val gap = DoadoDimens.GridSpacing
        val rows = ceil(apps.size / columns.toFloat()).toInt().coerceAtLeast(1)
        val fitHeight = (maxHeight - gap * (rows - 1)) / rows
        val tileHeight = if (columns == 1) {
            fitHeight.coerceIn(ONE_COLUMN_MIN, ONE_COLUMN_MAX)
        } else {
            fitHeight.coerceIn(DoadoDimens.TileMinHeight, TWO_COLUMN_MAX)
        }

        LazyVerticalGrid(
            columns = GridCells.Fixed(columns),
            horizontalArrangement = Arrangement.spacedBy(gap),
            verticalArrangement = Arrangement.spacedBy(gap),
        ) {
            items(items = apps, key = { it.id }) { app ->
                AppTile(
                    app = app,
                    horizontal = columns == 1,
                    highlighted = app.id == highlightedId,
                    onClick = { onAppClick(app) },
                    // Mínimo, no alto fijo: con la letra muy grande el aviso
                    // "No disponible" se recortaba y la app apagada se quedaba
                    // distinguida solo por el color. La rejilla se desplaza.
                    modifier = Modifier.heightIn(min = tileHeight),
                )
            }
        }
    }
}

private const val MINUTE_MS = 60_000L
private const val HEADER_MAX_TEXT_SCALE = 1.3f
private val ONE_COLUMN_MIN = 112.dp
private val ONE_COLUMN_MAX = 150.dp
private val TWO_COLUMN_MAX = 300.dp

@Preview(showBackground = true, widthDp = 411, heightDp = 891)
@Composable
private fun HomeScreenPreview() {
    DoadoTheme(darkTheme = true) {
        HomeScreen(
            apps = listOf(
                FavoriteApp("phone", "Llamar", AppIcon.Vector(R.drawable.ic_call), "com.android.dialer"),
                FavoriteApp("messages", "Mensajes", AppIcon.Vector(R.drawable.ic_messages), "com.android.mms"),
                FavoriteApp("camera", "Cámara", AppIcon.Vector(R.drawable.ic_camera), "com.android.camera"),
                FavoriteApp("contacts", "Contactos", AppIcon.Vector(R.drawable.ic_contacts), null),
            ),
            contacts = listOf(
                HomeContact(ContactShortcut("1", "María", "600000000", ContactAction.CALL, true), true),
                HomeContact(ContactShortcut("2", "Luis", "600000001", ContactAction.WHATSAPP, true), false),
            ),
            columns = 2,
            showClock = true,
            adminUnlockedUntil = 0L,
            showAssistant = true,
            highlightedId = "camera",
            onAppClick = {},
            onContactClick = {},
            onSettingsClick = {},
            onAssistantClick = {},
        )
    }
}
