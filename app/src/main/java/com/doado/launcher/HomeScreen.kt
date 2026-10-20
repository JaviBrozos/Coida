package com.doado.launcher

import android.text.format.DateFormat
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.selection.selectableGroup
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.Hyphens
import androidx.compose.ui.text.style.LineBreak
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import com.doado.R
import com.doado.assistant.ui.AssistantButton
import com.doado.core.ui.DoadoCard
import com.doado.core.ui.TabularFigures
import com.doado.core.ui.DoadoDimens
import com.doado.core.ui.DoadoTheme
import com.doado.core.ui.InitialBadge
import com.doado.core.ui.NarrowTextScale
import com.doado.core.ui.NotHomeNotice
import com.doado.core.ui.NotHomeNoticeStage
import com.doado.data.AppIcon
import com.doado.data.ContactAction
import com.doado.data.ContactShortcut
import com.doado.data.ContactsLayout
import com.doado.data.FavoriteApp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import kotlin.math.ceil
import kotlin.math.floor

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
 * Arriba, fijos, el reloj (o el nombre, si se ha quitado el reloj) con el botón de
 * Ajustes en la esquina. Debajo, una sola rejilla que se desplaza entera: las
 * casillas de apps y, tras el rótulo «Tus contactos», los accesos directos, con
 * la misma forma y tamaño que una casilla (D-38). El rótulo es lo que le dice a
 * la persona mayor, al desplazar, que ahí empiezan las personas.
 *
 * El círculo del asistente flota abajo en el centro, encima de la rejilla, que se
 * ve pasar por sus lados (D-38). La rejilla deja al final un margen de su alto:
 * cualquier casilla se puede subir hasta dejarla entera a la vista.
 *
 * @param highlightedId casilla o contacto que el asistente acaba de señalar al
 *   guiar: se pinta con borde grueso, sin moverlo, y la rejilla se desplaza hasta
 *   dejarlo a la vista.
 * @param scrollResetKey cambia al pulsar Inicio: la rejilla vuelve arriba, para que
 *   el inicio sea siempre el mismo al volver de una llamada.
 * @param contactsLayout contactos en la rejilla (de fábrica) o en una segunda
 *   pantalla a la que se pasa deslizando o con las pestañas (D-46).
 * @param wallpaper foto de fondo que ha elegido el familiar, o null (D-47).
 * @param hasWallpaper hay foto, aunque aún no se haya leído: las franjas lisas salen
 *   desde el primer fotograma.
 * @param notHomeNotice cómo va el aviso de que Doado aún no es la pantalla de
 *   inicio (D-48); null si lo es o si el familiar ya lo cerró.
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
    scrollResetKey: Int = 0,
    contactsLayout: ContactsLayout = ContactsLayout.GRID,
    wallpaper: ImageBitmap? = null,
    hasWallpaper: Boolean = wallpaper != null,
    notHomeNotice: NotHomeNoticeStage? = null,
    onMakeHome: () -> Unit = {},
    onCloseNotHome: () -> Unit = {},
) {
    // Las franjas lisas se deciden con saber que hay foto, sin esperar a leerla:
    // si aparecieran después, toda la rejilla bajaría bajo el dedo.
    val onPhoto = hasWallpaper
    val paged = contactsLayout == ContactsLayout.PAGE && contacts.isNotEmpty()
    val pagerState = rememberPagerState { if (paged) 2 else 1 }
    val scope = rememberCoroutineScope()

    // Pulsar Inicio vuelve a la primera pantalla, y al guiar se pasa a la que
    // tiene lo señalado: lo que dice el asistente cuadra con lo que se ve.
    LaunchedEffect(scrollResetKey) {
        if (scrollResetKey != 0 && pagerState.currentPage != 0) pagerState.scrollToPage(0)
    }
    LaunchedEffect(highlightedId, paged) {
        if (!paged || highlightedId == null) return@LaunchedEffect
        val page = when {
            contacts.any { it.shortcut.id == highlightedId } -> CONTACTS_PAGE
            apps.any { it.id == highlightedId } -> APPS_PAGE
            else -> return@LaunchedEffect
        }
        pagerState.animateScrollToPage(page)
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
    ) { innerPadding ->
        val now = rememberCurrentMinute()
        val context = LocalContext.current
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            // Dentro del margen seguro: la barra de estado sigue sobre fondo liso y
            // sus iconos se leen. Decorativa: TalkBack no tiene nada que decir de ella.
            if (wallpaper != null) {
                Image(
                    bitmap = wallpaper,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
            }
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(
                        start = DoadoDimens.ScreenPadding,
                        top = DoadoDimens.ScreenPadding,
                        end = DoadoDimens.ScreenPadding,
                    ),
                verticalArrangement = Arrangement.spacedBy(DoadoDimens.GridSpacing),
            ) {
                PhotoBand(onPhoto) {
                    HomeHeader(now = now, showClock = showClock, onSettingsClick = onSettingsClick)
                }

                if (adminUnlockedUntil > now) {
                    val until = remember(adminUnlockedUntil) {
                        DateFormat.getTimeFormat(context).format(Date(adminUnlockedUntil))
                    }
                    AdminBanner(untilLabel = until, onClick = onSettingsClick)
                }

                if (notHomeNotice != null) {
                    NotHomeNotice(
                        stage = notHomeNotice,
                        message = stringResource(R.string.home_not_default),
                        onMakeHome = onMakeHome,
                        onClose = onCloseNotHome,
                    )
                }

                if (paged) {
                    HomePageTabs(
                        selected = pagerState.currentPage,
                        onSelect = { page -> scope.launch { pagerState.animateScrollToPage(page) } },
                    )
                    HorizontalPager(
                        state = pagerState,
                        pageSpacing = DoadoDimens.ScreenPadding,
                        verticalAlignment = Alignment.Top,
                        modifier = Modifier.weight(1f),
                    ) { page ->
                        HomeGrid(
                            apps = if (page == APPS_PAGE) apps else emptyList(),
                            contacts = if (page == CONTACTS_PAGE) contacts else emptyList(),
                            columns = columns,
                            showAssistant = showAssistant,
                            highlightedId = highlightedId,
                            scrollResetKey = scrollResetKey,
                            showContactsLabel = false,
                            onPhoto = onPhoto,
                            onAppClick = onAppClick,
                            onContactClick = onContactClick,
                            modifier = Modifier.fillMaxSize(),
                        )
                    }
                } else {
                    HomeGrid(
                        apps = apps,
                        contacts = contacts,
                        columns = columns,
                        showAssistant = showAssistant,
                        highlightedId = highlightedId,
                        scrollResetKey = scrollResetKey,
                        showContactsLabel = true,
                        onPhoto = onPhoto,
                        onAppClick = onAppClick,
                        onContactClick = onContactClick,
                        modifier = Modifier.weight(1f),
                    )
                }
            }

            if (showAssistant) {
                AssistantButton(
                    onClick = onAssistantClick,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = DoadoDimens.ScreenPadding - DoadoDimens.AssistantRingWidth),
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

/**
 * La hora va en `headlineLarge` y no en un `display`: con el tamaño de reloj de
 * pared se llevaba un cuarto de la pantalla, que es sitio que pierden las casillas.
 */
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
                    // En español el día sale en minúscula («domingo, 18 de
                    // octubre»); suelta, en su línea, se lee como un titular.
                    SimpleDateFormat(pattern, locale).format(Date(now))
                        .replaceFirstChar { it.titlecase(locale) }
                }
                Text(
                    text = time,
                    style = MaterialTheme.typography.headlineLarge.copy(fontFeatureSettings = TabularFigures),
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.semantics { heading() },
                )
                Text(
                    text = date,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onBackground,
                )
            } else {
                Text(
                    text = stringResource(R.string.app_name),
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.semantics { heading() },
                )
                Text(
                    text = stringResource(R.string.app_tagline),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onBackground,
                )
            }
        }
        SettingsCornerButton(onClick = onSettingsClick)
    }
}

/**
 * Entrada a Ajustes: pequeña, en tono apagado y lejos del pulgar, porque es del
 * familiar y no de la persona mayor (D-28). Ocupa el sitio que tenía el isotipo.
 * Solo icono, un engranaje: es el dibujo de «ajustes» que cualquiera ya conoce de
 * su móvil, y los deslizadores de antes había que aprenderlos (D-39). La etiqueta
 * la lleva para TalkBack.
 */
@Composable
private fun SettingsCornerButton(onClick: () -> Unit) {
    val label = stringResource(R.string.cd_open_settings)
    // Redondo, a juego con el círculo del asistente: las dos piezas que flotan
    // fuera de la rejilla comparten forma, y ninguna se confunde con una casilla.
    DoadoCard(
        onClick = onClick,
        color = Color.Transparent,
        shape = CircleShape,
        modifier = Modifier
            .size(DoadoDimens.CornerButtonSize)
            .semantics { contentDescription = label },
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                painter = painterResource(R.drawable.ic_gear),
                contentDescription = null,
                modifier = Modifier.size(36.dp),
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

/**
 * Apps y contactos en una sola rejilla que ocupa todo el alto que queda. Con
 * pocas casillas crecen (hasta un tope, para que no parezcan carteles) y con
 * muchas bajan hasta el mínimo y la rejilla se desplaza.
 *
 * Los contactos van después de las apps y tras su rótulo, que ocupa la fila
 * entera: así empiezan siempre en una fila nueva y una casilla de app nunca queda
 * mezclada con una persona.
 *
 * Con tres por fila (D-45) las casillas no se estiran: son casi cuadradas, con el
 * alto que pida la que más texto lleva, para que todas las de una rejilla midan
 * lo mismo y ningún nombre se corte.
 *
 * @param showContactsLabel false en la pantalla de contactos (D-46), donde ya lo
 *   dice la pestaña de arriba.
 */
@Composable
private fun HomeGrid(
    apps: List<FavoriteApp>,
    contacts: List<HomeContact>,
    columns: Int,
    showAssistant: Boolean,
    highlightedId: String?,
    scrollResetKey: Int,
    showContactsLabel: Boolean,
    onPhoto: Boolean,
    onAppClick: (FavoriteApp) -> Unit,
    onContactClick: (ContactShortcut) -> Unit,
    modifier: Modifier = Modifier,
) {
    val gridState = rememberLazyGridState()
    val density = LocalDensity.current

    // Pulsar Inicio deja la rejilla como estaba al principio: el inicio es siempre
    // el mismo, también al volver de una llamada hecha desde un contacto de abajo.
    LaunchedEffect(scrollResetKey) {
        if (scrollResetKey != 0) gridState.scrollToItem(0)
    }

    // Margen final: el del borde de la pantalla y, con el círculo, su alto con el
    // aro, para que la última fila pueda quedar entera por encima de él.
    val bottomPadding = if (showAssistant) {
        DoadoDimens.ScreenPadding + DoadoDimens.AssistantButtonSize + DoadoDimens.AssistantRingWidth * 2
    } else {
        DoadoDimens.ScreenPadding
    }
    // El rótulo crece con la letra: con 56 dp fijos, al 200 % la cuenta se quedaba
    // corta y la última fila se metía bajo el círculo. Con foto, la franja lisa
    // que lo rodea suma por fuera.
    val labelHeight = with(density) {
        maxOf(CONTACTS_LABEL_HEIGHT, MaterialTheme.typography.titleMedium.lineHeight.toDp() + 8.dp)
    }
    val hasLabel = showContactsLabel && contacts.isNotEmpty()
    val sectionHeight = if (hasLabel) {
        labelHeight + DoadoDimens.GridSpacing + if (onPhoto) PHOTO_BAND_EXTRA else 0.dp
    } else {
        0.dp
    }

    BoxWithConstraints(modifier = modifier.fillMaxWidth()) {
        val gap = DoadoDimens.GridSpacing
        val appRows = ceil(apps.size / columns.toFloat()).toInt()
        val contactRows = ceil(contacts.size / columns.toFloat()).toInt()
        val rows = (appRows + contactRows).coerceAtLeast(1)
        // Lo que queda por encima del círculo, que es lo que se ve sin taparse.
        val visible = maxHeight - bottomPadding

        val appTileHeight: Dp
        val contactTileHeight: Dp
        if (columns >= 3) {
            val side = (maxWidth - gap * (columns - 1)) / columns
            val needed = narrowTileHeights(apps, contacts, side)
            appTileHeight = maxOf(side, needed.first)
            contactTileHeight = maxOf(side, needed.second)
        } else {
            val minHeight = if (columns == 1) ONE_COLUMN_MIN else DoadoDimens.TileMinHeight
            val maxTileHeight = if (columns == 1) ONE_COLUMN_MAX else TWO_COLUMN_MAX
            val fitAll = (visible - sectionHeight - gap * (rows - 1)) / rows
            val tileHeight = if (fitAll >= minHeight) {
                fitAll.coerceAtMost(maxTileHeight)
            } else {
                // No cabe todo: filas enteras por encima del círculo al empezar, y la
                // siguiente solo asoma por sus lados como pista de que hay más. Así,
                // en reposo, ninguna casilla queda a medias bajo el círculo. Si el
                // rótulo de los contactos cae en esa primera vista, también cabe entero.
                var space = visible
                var fullRows = floor((space + gap) / (minHeight + gap)).toInt().coerceAtLeast(1)
                if (hasLabel && appRows <= fullRows) {
                    space = visible - sectionHeight
                    fullRows = floor((space + gap) / (minHeight + gap)).toInt().coerceAtLeast(1)
                }
                ((space - gap * (fullRows - 1)) / fullRows).coerceIn(minHeight, maxTileHeight)
            }
            appTileHeight = tileHeight
            contactTileHeight = tileHeight
        }

        // Al guiar, la casilla señalada puede estar fuera de la vista. Solo se
        // desplaza si no se ve entera por encima del círculo, y un contacto se sube
        // hasta su rótulo si así cabe: lo que se le dice («en Tus contactos, fila
        // 2») tiene que cuadrar con lo que ve. La casilla no cambia de sitio.
        LaunchedEffect(highlightedId) {
            val id = highlightedId ?: return@LaunchedEffect
            val appIndex = apps.indexOfFirst { it.id == id }
            val contactIndex = contacts.indexOfFirst { it.shortcut.id == id }
            val index = when {
                appIndex >= 0 -> appIndex
                // El rótulo de los contactos, si lo hay, va justo detrás de las apps.
                contactIndex >= 0 -> apps.size + (if (hasLabel) 1 else 0) + contactIndex
                else -> return@LaunchedEffect
            }
            val layout = gridState.layoutInfo
            val limit = layout.viewportEndOffset - with(density) { bottomPadding.roundToPx() }
            val shown = layout.visibleItemsInfo.firstOrNull { it.index == index }
            if (shown != null && shown.offset.y >= 0 && shown.offset.y + shown.size.height <= limit) {
                return@LaunchedEffect
            }
            val fromLabel = if (contactIndex >= 0 && hasLabel) {
                sectionHeight + (contactTileHeight + gap) * (contactIndex / columns) + contactTileHeight
            } else {
                null
            }
            val target = if (fromLabel != null && fromLabel <= visible) apps.size else index
            gridState.animateScrollToItem(target)
        }

        LazyVerticalGrid(
            columns = GridCells.Fixed(columns),
            state = gridState,
            contentPadding = PaddingValues(bottom = bottomPadding),
            horizontalArrangement = Arrangement.spacedBy(gap),
            verticalArrangement = Arrangement.spacedBy(gap),
        ) {
            items(items = apps, key = { it.id }) { app ->
                NarrowTextScale(enabled = columns >= 3) {
                    AppTile(
                        app = app,
                        horizontal = columns == 1,
                        narrow = columns >= 3,
                        highlighted = app.id == highlightedId,
                        onClick = { onAppClick(app) },
                        // Mínimo, no alto fijo: con la letra muy grande el aviso
                        // "No disponible" se recortaba y la app apagada se quedaba
                        // distinguida solo por el color. La rejilla se desplaza.
                        modifier = Modifier.heightIn(min = appTileHeight),
                    )
                }
            }

            if (contacts.isNotEmpty()) {
                if (hasLabel) {
                    item(key = CONTACTS_LABEL_KEY, span = { GridItemSpan(maxLineSpan) }) {
                        PhotoBand(onPhoto) { ContactsLabel(minHeight = labelHeight) }
                    }
                }
                items(items = contacts, key = { "contact:" + it.shortcut.id }) { contact ->
                    NarrowTextScale(enabled = columns >= 3) {
                        ContactTile(
                            contact = contact,
                            horizontal = columns == 1,
                            narrow = columns >= 3,
                            highlighted = contact.shortcut.id == highlightedId,
                            onClick = { onContactClick(contact.shortcut) },
                            modifier = Modifier.heightIn(min = contactTileHeight),
                        )
                    }
                }
            }
        }
    }
}

/**
 * Lo que necesitan de alto las casillas de tres por fila, apps y contactos por
 * separado: el de la que más texto lleva. Se mide el texto de verdad, con la
 * misma letra, el mismo ancho y el mismo techo de escala que tendrá la casilla.
 */
@Composable
private fun narrowTileHeights(apps: List<FavoriteApp>, contacts: List<HomeContact>, side: Dp): Pair<Dp, Dp> {
    val measurer = rememberTextMeasurer()
    val density = LocalDensity.current
    val capped = Density(density.density, density.fontScale.coerceAtMost(DoadoDimens.NarrowMaxTextScale))
    val labelStyle = tileLabelStyle(narrow = true)
    val smallStyle = tileSecondaryStyle()
    val unavailable = stringResource(R.string.app_unavailable)
    val call = stringResource(R.string.contact_action_call)
    val whatsApp = stringResource(R.string.contact_action_whatsapp)
    val whatsAppMissing = stringResource(R.string.contact_whatsapp_missing_short)
    val textWidth = with(density) { (side - NarrowTilePaddingH * 2).roundToPx() }.coerceAtLeast(1)

    // La línea de debajo del nombre no crece con la letra (SecondaryLineScale).
    val unscaled = Density(density.density, 1f)
    fun height(text: String, style: TextStyle, at: Density = capped): Dp = with(density) {
        measurer.measure(
            text = text,
            style = style,
            constraints = Constraints(maxWidth = textWidth),
            density = at,
        ).size.height.toDp()
    }

    val fixed = NarrowTilePaddingV * 2 + DoadoDimens.TileIconSizeNarrow + NarrowTileSpacing
    val app = apps.maxOfOrNull {
        height(it.label, labelStyle) +
            if (it.isAvailable) 0.dp else TileSecondaryGap + height(unavailable, smallStyle, unscaled)
    } ?: 0.dp
    val contact = contacts.maxOfOrNull {
        val detail = when {
            !it.isAvailable -> whatsAppMissing
            it.shortcut.action == ContactAction.CALL -> call
            else -> whatsApp
        }
        height(it.shortcut.name, labelStyle) + TileSecondaryGap + height(detail, smallStyle, unscaled)
    } ?: 0.dp
    return (fixed + app) to (fixed + contact)
}

/**
 * Con foto de fondo, lo que no es casilla (el reloj, el rótulo de los contactos,
 * las pestañas) va sobre una franja lisa del color del fondo y con contorno
 * (D-47): así el contraste se mide contra ella y no contra la foto, que puede ser
 * de cualquier color. Sin foto, no añade nada.
 */
@Composable
private fun PhotoBand(onPhoto: Boolean, content: @Composable () -> Unit) {
    if (!onPhoto) return content()
    DoadoCard(
        onClick = null,
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.background,
    ) {
        Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) { content() }
    }
}

/**
 * Las dos pantallas del inicio cuando los contactos van aparte (D-46): las apps y
 * «Tus contactos». Se pasa de una a otra deslizando, pero estas dos pestañas
 * grandes siempre están a la vista: deslizar es un atajo, no la única forma, y
 * así se sabe que hay otra pantalla y en cuál se está.
 */
@Composable
private fun HomePageTabs(selected: Int, onSelect: (Int) -> Unit) {
    // Con el mismo techo de letra que la cabecera: al 200 % las pestañas se comían
    // media pantalla en tres líneas.
    val density = LocalDensity.current
    val capped = Density(density.density, density.fontScale.coerceAtMost(HEADER_MAX_TEXT_SCALE))
    CompositionLocalProvider(LocalDensity provides capped) { HomePageTabsContent(selected, onSelect) }
}

@Composable
private fun HomePageTabsContent(selected: Int, onSelect: (Int) -> Unit) {
    val colors = MaterialTheme.colorScheme
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min)
            .selectableGroup(),
        horizontalArrangement = Arrangement.spacedBy(DoadoDimens.GridSpacing),
    ) {
        listOf(R.string.home_page_apps, R.string.home_page_contacts).forEachIndexed { page, label ->
            val isSelected = page == selected
            DoadoCard(
                onClick = { onSelect(page) },
                color = if (isSelected) colors.primary else colors.background,
                highlighted = isSelected,
                shape = RoundedCornerShape(DoadoDimens.CardCornerRadius),
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .heightIn(min = DoadoDimens.ComfortableTouchTarget)
                    .semantics {
                        role = Role.Tab
                        this.selected = isSelected
                    },
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                ) {
                    Text(
                        text = stringResource(label),
                        style = tileLabelStyle(narrow = true),
                        color = if (isSelected) colors.onPrimary else colors.onSurface,
                        textAlign = TextAlign.Center,
                    )
                }
            }
        }
    }
}

/**
 * Rótulo «Tus contactos» sobre los accesos directos. No dice solo «Contactos»
 * porque así se llama también la casilla de la agenda, y se confundirían.
 */
@Composable
private fun ContactsLabel(minHeight: Dp) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = minHeight)
            .padding(start = 4.dp, top = 8.dp)
            .semantics(mergeDescendants = true) { heading() },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_contacts),
            contentDescription = null,
            modifier = Modifier.size(28.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = stringResource(R.string.home_contacts_section),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onBackground,
        )
    }
}

/**
 * Acceso directo a una persona, con la forma de una casilla: la inicial ocupa el
 * sitio del icono y debajo van el nombre y lo que hace (llamar o WhatsApp). La
 * inicial va en su círculo (D-35), que es lo que la distingue de una app.
 */
@Composable
private fun ContactTile(
    contact: HomeContact,
    horizontal: Boolean,
    highlighted: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    narrow: Boolean = false,
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
    // Solo se apaga el de WhatsApp sin WhatsApp. Dice qué falta, y no un «No
    // disponible» a secas: con la misma persona dos veces, así se sabe cuál es.
    val detail = if (contact.isAvailable) actionText else stringResource(R.string.contact_whatsapp_missing_short)
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
        shape = RoundedCornerShape(DoadoDimens.TileCornerRadius),
        borderWidth = if (highlighted) DoadoDimens.HighlightBorderWidth else DoadoDimens.BorderWidth,
        modifier = modifier
            .fillMaxWidth()
            .semantics {
                if (contact.isAvailable) {
                    onClick(label = clickLabel) { onClick(); true }
                } else {
                    contentDescription = unavailableLabel
                }
                if (highlighted) stateDescription = highlightedLabel
            },
    ) {
        val initial: @Composable (Dp) -> Unit = { size ->
            InitialBadge(
                name = shortcut.name,
                size = size,
                shape = CircleShape,
                color = if (contact.isAvailable) colors.primary else colors.outlineVariant,
                plate = when {
                    !contact.isAvailable -> colors.surfaceContainer
                    highlighted -> colors.onPrimary
                    else -> colors.surfaceContainerLowest
                },
                // En el círculo pequeño de una columna, una letra menor: con la grande
                // al 200 % se salía del círculo.
                textStyle = if (size < DoadoDimens.TileIconSize) {
                    MaterialTheme.typography.labelLarge
                } else {
                    MaterialTheme.typography.titleMedium
                },
            )
        }
        if (horizontal) {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(20.dp),
            ) {
                initial(DoadoDimens.TileIconSizeCompact)
                ContactLabels(
                    name = shortcut.name,
                    detail = detail,
                    color = contentColor,
                    detailColor = secondaryColor,
                    align = TextAlign.Start,
                    modifier = Modifier.weight(1f),
                )
            }
        } else {
            Column(
                modifier = if (narrow) {
                    Modifier.padding(horizontal = NarrowTilePaddingH, vertical = NarrowTilePaddingV)
                } else {
                    Modifier.padding(horizontal = 12.dp, vertical = 16.dp)
                },
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(
                    if (narrow) NarrowTileSpacing else 14.dp,
                    Alignment.CenterVertically,
                ),
            ) {
                initial(if (narrow) DoadoDimens.TileIconSizeNarrow else DoadoDimens.TileIconSize)
                ContactLabels(
                    name = shortcut.name,
                    detail = detail,
                    color = contentColor,
                    detailColor = secondaryColor,
                    align = TextAlign.Center,
                    narrow = narrow,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

@Composable
private fun ContactLabels(
    name: String,
    detail: String,
    color: Color,
    detailColor: Color,
    align: TextAlign,
    modifier: Modifier,
    narrow: Boolean = false,
) {
    Column(
        modifier = modifier,
        horizontalAlignment = if (align == TextAlign.Center) Alignment.CenterHorizontally else Alignment.Start,
    ) {
        // Como en las casillas: si el nombre no cabe, se parte por sílabas, y sin
        // tope de líneas: al 200 % «Tía Encarnación» pide cinco, y un nombre cortado
        // no se reconoce. La casilla crece.
        Text(
            text = name,
            style = tileLabelStyle(narrow),
            color = color,
            textAlign = align,
        )
        SecondaryLineScale(narrow) {
            Text(
                text = detail,
                style = tileSecondaryStyle(),
                color = detailColor,
                textAlign = align,
                modifier = Modifier.padding(top = TileSecondaryGap),
            )
        }
    }
}

private const val MINUTE_MS = 60_000L
private const val HEADER_MAX_TEXT_SCALE = 1.5f
private const val CONTACTS_LABEL_KEY = "contacts-label"
private val CONTACTS_LABEL_HEIGHT = 56.dp
private val ONE_COLUMN_MIN = 112.dp
private val ONE_COLUMN_MAX = 150.dp
private val TWO_COLUMN_MAX = 300.dp

// Las dos pantallas del inicio cuando los contactos van aparte (D-46).
private const val APPS_PAGE = 0
private const val CONTACTS_PAGE = 1

// Lo que la franja lisa sobre la foto añade al alto: su margen y su contorno.
private val PHOTO_BAND_EXTRA = 20.dp

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
