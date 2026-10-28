package com.doado.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridItemInfo
import androidx.compose.foundation.lazy.grid.LazyGridItemScope
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.toOffset
import androidx.compose.ui.zIndex
import com.doado.R
import com.doado.assistant.GuideSteps
import com.doado.core.ui.DoadoDimens
import com.doado.core.ui.NoteCard
import com.doado.core.ui.OutlineButton
import com.doado.core.ui.PrimaryButton
import com.doado.core.ui.ScreenHeader
import com.doado.core.ui.rememberClickFeedback
import com.doado.core.ui.SectionLabel
import com.doado.data.FavoriteApp
import com.doado.launcher.AppTile
import com.doado.launcher.ContactTile
import com.doado.launcher.HomeContact
import com.doado.launcher.contactTileDetail
import com.doado.launcher.gridSpacing
import com.doado.launcher.rememberTileLabelScale
import com.doado.launcher.tileHeight
import kotlinx.coroutines.flow.collectLatest

/**
 * Ajustes → Colocar las casillas (D-59): el familiar decide dónde va cada casilla
 * del inicio, en vez de que salgan en el orden en que se pusieron.
 *
 * Es una réplica del inicio, con las mismas columnas y las mismas casillas. Se
 * arrastra: se mantiene pulsada una casilla, se levanta (rellena de acento, como
 * la que señala el asistente) y las demás se apartan para hacerle hueco. Arrastrar
 * es la excepción a «nada de gestos» (`ui-compose.md`): esta pantalla solo la usa
 * el familiar, y así va más rápido. Para quien no puede arrastrar (TalkBack, poco
 * pulso) queda tocar y colocar: se toca una, se toca el sitio, o se mueve con
 * «Mover antes» y «Mover después».
 *
 * Las apps y los contactos se colocan cada uno en su apartado: una persona nunca
 * acaba entre las apps. Cada movimiento se guarda al soltar o al pulsar, como en
 * Interfaz. Nada más se mueve: el inicio no cambia de orden por sí solo.
 *
 * @param onAppsOrder el orden nuevo de las casillas de apps, por id.
 * @param onContactsOrder el de los contactos del inicio, por id de acceso directo.
 */
@Composable
fun ArrangeScreen(
    apps: List<FavoriteApp>,
    contacts: List<HomeContact>,
    columns: Int,
    onAppsOrder: (List<String>) -> Unit,
    onContactsOrder: (List<String>) -> Unit,
    onBack: () -> Unit,
) {
    val gridState = rememberLazyGridState()
    val haptic = LocalHapticFeedback.current
    val drag = remember(gridState) { DragState(gridState) }

    // El orden mientras se coloca. Si llega otro de fuera (al guardar, o porque se
    // han vuelto a leer las apps o las fotos), manda ese, salvo con una casilla
    // cogida: entonces se pisaría el arrastre a medias.
    var appOrder by remember { mutableStateOf(apps) }
    var contactOrder by remember { mutableStateOf(contacts) }
    LaunchedEffect(apps) { if (drag.key == null) appOrder = apps }
    LaunchedEffect(contacts) { if (drag.key == null) contactOrder = contacts }
    var selected by remember { mutableStateOf<String?>(null) }
    val saveApps by rememberUpdatedState(onAppsOrder)
    val saveContacts by rememberUpdatedState(onContactsOrder)

    fun move(fromKey: String, toKey: String) {
        if (fromKey.startsWith(APP_KEY) && toKey.startsWith(APP_KEY)) {
            appOrder = appOrder.moved(appOrder.indexOfFirst { appKey(it) == fromKey }, appOrder.indexOfFirst { appKey(it) == toKey })
        } else if (fromKey.startsWith(CONTACT_KEY) && toKey.startsWith(CONTACT_KEY)) {
            contactOrder = contactOrder.moved(
                contactOrder.indexOfFirst { contactKey(it) == fromKey },
                contactOrder.indexOfFirst { contactKey(it) == toKey },
            )
        }
    }

    fun save(key: String) {
        if (key.startsWith(APP_KEY)) saveApps(appOrder.map { it.id }) else saveContacts(contactOrder.map { it.shortcut.id })
    }

    fun sectionKeys(key: String) =
        if (key.startsWith(APP_KEY)) appOrder.map(::appKey) else contactOrder.map(::contactKey)

    fun nameOf(key: String) = appOrder.firstOrNull { appKey(it) == key }?.label
        ?: contactOrder.firstOrNull { contactKey(it) == key }?.shortcut?.name.orEmpty()

    /** Un puesto antes o después, dentro de su apartado. */
    fun step(key: String, by: Int) {
        val keys = sectionKeys(key)
        val target = keys.getOrNull(keys.indexOf(key) + by) ?: return
        move(key, target)
        save(key)
    }

    /**
     * Tocar y colocar: sin ninguna elegida, la elige; si es la elegida, la suelta; si
     * hay otra elegida de su mismo apartado, lleva aquella a este sitio.
     */
    fun tap(key: String) {
        val current = selected
        selected = when {
            current == null -> key
            current == key -> null
            sameSection(current, key) -> {
                move(current, key)
                save(current)
                null
            }
            else -> key
        }
    }

    // Con la casilla cogida contra el borde de arriba o de abajo, la rejilla se
    // desplaza sola, fotograma a fotograma, y la casilla va cambiando de sitio con
    // lo que pasa por debajo.
    LaunchedEffect(drag) {
        snapshotFlow { drag.edgeSpeed }.collectLatest { speed ->
            while (speed != 0f && drag.key != null) {
                gridState.scrollBy(speed)
                drag.drag(Offset.Zero, ::move)
                withFrameNanos { }
            }
        }
    }

    // Al elegir una, la barra de botones de abajo le quita alto a la rejilla: si la
    // elegida queda tapada, se sube hasta ella. Y tras moverla con los botones, se la
    // sigue.
    LaunchedEffect(selected, appOrder, contactOrder) {
        val key = selected ?: return@LaunchedEffect
        withFrameNanos { }
        val layout = gridState.layoutInfo
        val shown = layout.visibleItemsInfo.firstOrNull { it.key == key }
        if (shown != null && shown.offset.y >= 0 && shown.offset.y + shown.size.height <= layout.viewportEndOffset) {
            return@LaunchedEffect
        }
        gridState.animateScrollToItem(gridIndexOf(key, appOrder.map(::appKey), contactOrder.map(::contactKey)))
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // El margen va por fuera y no como relleno de la rejilla: así lo que mide el
        // dedo y lo que mide la rejilla de cada casilla salen en las mismas coordenadas.
        BoxWithConstraints(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = DoadoDimens.ScreenPadding),
        ) {
            // La misma letra en todas, como en el inicio.
            val tileWidth = if (columns == 1) maxWidth else tileHeight(maxWidth, columns)
            val appScale = rememberTileLabelScale(appOrder.map { it.label to null }, tileWidth, columns == 1)
            val contactScale = rememberTileLabelScale(
                contactOrder.map { it.shortcut.name to contactTileDetail(it) },
                tileWidth,
                columns == 1,
            )
            LazyVerticalGrid(
                columns = GridCells.Fixed(columns),
                state = gridState,
                horizontalArrangement = Arrangement.spacedBy(gridSpacing(columns)),
                verticalArrangement = Arrangement.spacedBy(gridSpacing(columns)),
                // Con una casilla cogida, el dedo la mueve a ella y no a la rejilla: si
                // no, un tirón rápido desplazaba la rejilla y la casilla se soltaba.
                // Cerca de los bordes se desplaza sola ([DragState.scrollRequests]).
                userScrollEnabled = drag.key == null,
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(drag) {
                        // Pase lo que pase con el gesto, la casilla cogida se suelta y su
                        // sitio se guarda: si no, se quedaba levantada y la rejilla quieta.
                        try {
                            detectDragGesturesAfterLongPress(
                                onDragStart = { offset ->
                                    if (drag.start(offset)) {
                                        selected = null
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    }
                                },
                                onDrag = { change, amount ->
                                    // Pulsación larga fuera de una casilla: no se coge nada, y
                                    // el movimiento sigue siendo de la rejilla.
                                    if (drag.key != null) {
                                        change.consume()
                                        drag.drag(amount, ::move)
                                    }
                                },
                                onDragEnd = { drag.end()?.let(::save) },
                                onDragCancel = { drag.end()?.let(::save) },
                            )
                        } finally {
                            drag.end()?.let(::save)
                        }
                    },
            ) {
                item(key = "header", span = { GridItemSpan(maxLineSpan) }) {
                    Column(
                        modifier = Modifier.padding(top = DoadoDimens.ScreenPadding),
                        verticalArrangement = Arrangement.spacedBy(14.dp),
                    ) {
                        ScreenHeader(
                            title = stringResource(R.string.settings_arrange),
                            subtitle = stringResource(R.string.arrange_subtitle),
                            backLabel = stringResource(R.string.cd_back_settings),
                            onBack = onBack,
                        )
                        if (appOrder.isEmpty() && contactOrder.isEmpty()) {
                            NoteCard(text = stringResource(R.string.arrange_empty))
                        }
                    }
                }

                if (appOrder.isNotEmpty()) {
                    item(key = "apps-label", span = { GridItemSpan(maxLineSpan) }) {
                        SectionLabel(stringResource(R.string.arrange_section_apps), Modifier.padding(top = 8.dp))
                    }
                    items(appOrder, key = ::appKey) { app ->
                        val key = appKey(app)
                        ArrangeTile(
                            key = key,
                            label = app.label,
                            index = appOrder.indexOf(app),
                            count = appOrder.size,
                            columns = columns,
                            selected = selected,
                            selectedName = selected?.let(::nameOf),
                            drag = drag,
                            onTap = { tap(key) },
                            onStep = { by -> step(key, by) },
                        ) { highlighted ->
                            AppTile(
                                app = app,
                                horizontal = columns == 1,
                                onClick = null,
                                highlighted = highlighted,
                                labelScale = appScale,
                            )
                        }
                    }
                }

                if (contactOrder.isNotEmpty()) {
                    item(key = "contacts-label", span = { GridItemSpan(maxLineSpan) }) {
                        SectionLabel(stringResource(R.string.home_contacts_section), Modifier.padding(top = 8.dp))
                    }
                    items(contactOrder, key = ::contactKey) { contact ->
                        val key = contactKey(contact)
                        ArrangeTile(
                            key = key,
                            label = contact.shortcut.name,
                            index = contactOrder.indexOf(contact),
                            count = contactOrder.size,
                            columns = columns,
                            selected = selected,
                            selectedName = selected?.let(::nameOf),
                            drag = drag,
                            onTap = { tap(key) },
                            onStep = { by -> step(key, by) },
                        ) { highlighted ->
                            ContactTile(
                                contact = contact,
                                horizontal = columns == 1,
                                highlighted = highlighted,
                                onClick = null,
                                labelScale = contactScale,
                            )
                        }
                    }
                }

                item(key = "end", span = { GridItemSpan(maxLineSpan) }) {
                    Spacer(modifier = Modifier.height(DoadoDimens.ScreenPadding))
                }
            }
        }

        // Con una casilla elegida, los botones para moverla sin arrastrar. Fuera de
        // la rejilla, para que no se vayan al desplazarla.
        selected?.let { key ->
            val keys = sectionKeys(key)
            val position = keys.indexOf(key)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = DoadoDimens.ScreenPadding, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                // Qué se está moviendo, escrito: con la rejilla desplazada puede no
                // verse, y TalkBack lo dice al cambiar.
                Text(
                    text = stringResource(R.string.arrange_moving, nameOf(key)),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
                )
                val before: @Composable (Modifier) -> Unit = { modifier ->
                    OutlineButton(
                        text = stringResource(R.string.arrange_move_before),
                        onClick = { step(key, -1) },
                        enabled = position > 0,
                        modifier = modifier,
                    )
                }
                val after: @Composable (Modifier) -> Unit = { modifier ->
                    OutlineButton(
                        text = stringResource(R.string.arrange_move_after),
                        onClick = { step(key, 1) },
                        enabled = position < keys.lastIndex,
                        modifier = modifier,
                    )
                }
                // Con la letra grande, uno debajo de otro: en media pantalla «después»
                // no cabía y se partía.
                if (LocalDensity.current.fontScale >= DoadoDimens.LargeTextScale) {
                    before(Modifier)
                    after(Modifier)
                } else {
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        before(Modifier.weight(1f))
                        after(Modifier.weight(1f))
                    }
                }
                PrimaryButton(text = stringResource(R.string.arrange_done), onClick = { selected = null })
            }
        }
    }
}

/**
 * Una casilla de la réplica. La envuelve un toque propio (la casilla de dentro no
 * se pulsa) y, para TalkBack, una sola etiqueta en vez de lo que diría en el inicio
 * («Abrir Cámara»): el nombre, su sitio como estado (que TalkBack vuelve a leer al
 * moverla) y las acciones de moverla.
 *
 * @param selected la casilla elegida para tocar y colocar, si hay; [selectedName], su nombre.
 * @param count cuántas hay en su apartado: la primera no tiene «Mover antes».
 */
@Composable
private fun LazyGridItemScope.ArrangeTile(
    key: String,
    label: String,
    index: Int,
    count: Int,
    columns: Int,
    selected: String?,
    selectedName: String?,
    drag: DragState,
    onTap: () -> Unit,
    onStep: (Int) -> Unit,
    tile: @Composable (highlighted: Boolean) -> Unit,
) {
    val resources = LocalResources.current
    val place = if (columns == 1) {
        stringResource(R.string.cd_arrange_place_one, index + 1)
    } else {
        stringResource(R.string.cd_arrange_place, index / columns + 1, GuideSteps.side(resources, index, columns))
    }
    val isSelected = key == selected
    val inSameSection = selected != null && sameSection(selected, key)
    // Lo que hará el toque, dicho tal cual: elegirla, soltarla o traer aquí la elegida.
    val tapLabel = when {
        isSelected -> stringResource(R.string.cd_arrange_release)
        inSameSection && selectedName != null -> stringResource(R.string.cd_arrange_put_here, selectedName)
        else -> stringResource(R.string.cd_arrange_select)
    }
    val state = if (isSelected) stringResource(R.string.cd_arrange_selected, place) else place
    val before = stringResource(R.string.arrange_move_before)
    val after = stringResource(R.string.arrange_move_after)
    val dragging = key == drag.key
    val feedback = rememberClickFeedback()

    Box(
        modifier = Modifier
            // La que se arrastra va encima de las demás y sigue al dedo; las demás
            // se apartan con su animación.
            .then(
                if (dragging) {
                    Modifier
                        .zIndex(1f)
                        .graphicsLayer {
                            translationX = drag.offset.x
                            translationY = drag.offset.y
                        }
                } else {
                    Modifier.animateItem()
                },
            )
            .clearAndSetSemantics {
                contentDescription = label
                stateDescription = state
                role = Role.Button
                this.selected = isSelected
                onClick(label = tapLabel) { onTap(); true }
                customActions = listOfNotNull(
                    CustomAccessibilityAction(before) { onStep(-1); true }.takeIf { index > 0 },
                    CustomAccessibilityAction(after) { onStep(1); true }.takeIf { index < count - 1 },
                )
            }
            // Sin onda: la respuesta es que se rellena de acento, y la vibración.
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = { feedback(); onTap() },
            ),
    ) {
        tile(isSelected || dragging)
    }
}

/**
 * Dónde está [key] en la rejilla: primero la cabecera; luego, si hay apps, su rótulo
 * y ellas; luego el rótulo de los contactos y ellos.
 */
private fun gridIndexOf(key: String, appKeys: List<String>, contactKeys: List<String>): Int {
    val app = appKeys.indexOf(key)
    if (app >= 0) return 2 + app
    val contactsStart = if (appKeys.isEmpty()) 2 else 3 + appKeys.size
    return contactsStart + contactKeys.indexOf(key).coerceAtLeast(0)
}

/**
 * Lo que dura un arrastre: qué casilla va cogida, cuánto se ha movido el dedo y
 * dónde estaba al empezar. Se trabaja por claves y no por posiciones, porque en la
 * rejilla también están la cabecera y los rótulos.
 */
private class DragState(private val grid: LazyGridState) {
    var key by mutableStateOf<String?>(null)
        private set
    private var delta by mutableStateOf(Offset.Zero)
    private var startOffset by mutableStateOf(Offset.Zero)

    /**
     * Con la casilla pegada al borde de arriba o de abajo, lo que se desplaza la
     * rejilla sola en cada fotograma (negativo, hacia arriba). Sigue aunque el dedo
     * se quede quieto ahí; lo atiende un efecto de la pantalla.
     */
    var edgeSpeed by mutableFloatStateOf(0f)
        private set

    private val info: LazyGridItemInfo?
        get() = grid.layoutInfo.visibleItemsInfo.firstOrNull { it.key == key }

    /** Dónde se pinta la casilla cogida respecto a su sitio en la rejilla. */
    val offset: Offset
        get() = info?.let { startOffset + delta - it.offset.toOffset() } ?: Offset.Zero

    /** Coge la casilla que hay bajo el dedo, si es una casilla. */
    fun start(at: Offset): Boolean {
        val item = itemAt(at) ?: return false
        key = item.key as String
        startOffset = item.offset.toOffset()
        delta = Offset.Zero
        return true
    }

    /** Mueve la cogida y, si su centro cae sobre otra de su apartado, le cambia el sitio. */
    fun drag(amount: Offset, move: (from: String, to: String) -> Unit) {
        val current = key ?: return
        delta += amount
        val item = info ?: return
        val topLeft = item.offset.toOffset() + offset
        val center = topLeft + Offset(item.size.width / 2f, item.size.height / 2f)
        val target = itemAt(center)?.takeIf { it.key != current && sameSection(it.key as String, current) }
        if (target != null) {
            // Si la primera casilla visible es una de las dos, la rejilla saltaría
            // para seguirla: se le pide que se quede donde está.
            if (item.index == grid.firstVisibleItemIndex || target.index == grid.firstVisibleItemIndex) {
                grid.requestScrollToItem(grid.firstVisibleItemIndex, grid.firstVisibleItemScrollOffset)
            }
            move(current, target.key as String)
        }
        // Cuánto se sale por arriba o por abajo, y hacia dónde va el dedo ahora.
        val layout = grid.layoutInfo
        val below = (topLeft.y + item.size.height - layout.viewportEndOffset).coerceAtLeast(0f)
        val above = (topLeft.y - layout.viewportStartOffset).coerceAtMost(0f)
        edgeSpeed = when {
            below > 0f && amount.y >= 0f -> (below * EDGE_SPEED_RATIO).coerceAtMost(EDGE_SPEED_MAX)
            above < 0f && amount.y <= 0f -> (above * EDGE_SPEED_RATIO).coerceAtLeast(-EDGE_SPEED_MAX)
            else -> 0f
        }
    }

    /** Suelta la casilla. Devuelve su clave, para guardar el orden nuevo. */
    fun end(): String? {
        val released = key
        key = null
        delta = Offset.Zero
        startOffset = Offset.Zero
        edgeSpeed = 0f
        return released
    }

    private fun itemAt(at: Offset): LazyGridItemInfo? =
        grid.layoutInfo.visibleItemsInfo.firstOrNull { item ->
            val itemKey = item.key as? String ?: return@firstOrNull false
            (itemKey.startsWith(APP_KEY) || itemKey.startsWith(CONTACT_KEY)) &&
                at.x >= item.offset.x && at.x <= item.offset.x + item.size.width &&
                at.y >= item.offset.y && at.y <= item.offset.y + item.size.height
        }
}

/** Las dos claves son del mismo apartado: apps con apps, contactos con contactos. */
private fun sameSection(a: String, b: String) =
    (a.startsWith(APP_KEY) && b.startsWith(APP_KEY)) || (a.startsWith(CONTACT_KEY) && b.startsWith(CONTACT_KEY))

private const val APP_KEY = "app:"
private const val CONTACT_KEY = "contact:"

/** El desplazamiento solo: una parte de lo que se sale, y como mucho tanto por fotograma (px). */
private const val EDGE_SPEED_RATIO = 0.2f
private const val EDGE_SPEED_MAX = 40f

private fun appKey(app: FavoriteApp) = APP_KEY + app.id
private fun contactKey(contact: HomeContact) = CONTACT_KEY + contact.shortcut.id

/** La lista con el elemento de [from] llevado a [to]; los de en medio corren un puesto. */
private fun <T> List<T>.moved(from: Int, to: Int): List<T> {
    if (from !in indices || to !in indices || from == to) return this
    return toMutableList().apply { add(to, removeAt(from)) }
}
