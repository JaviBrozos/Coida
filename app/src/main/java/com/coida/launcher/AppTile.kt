package com.coida.launcher

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.SemanticsPropertyReceiver
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.coida.R
import com.coida.core.ui.ContactAvatar
import com.coida.core.ui.CoidaCard
import com.coida.core.ui.CoidaDimens
import com.coida.core.ui.CoidaTheme
import com.coida.core.ui.FitLine
import com.coida.core.ui.minFitScale
import com.coida.core.ui.rememberCommonFitScale
import com.coida.core.ui.rememberFitScale
import com.coida.core.ui.scaledBy
import com.coida.data.AppIcon
import com.coida.data.ContactAction
import com.coida.data.ContactShortcut
import com.coida.data.EMERGENCY_FAVORITE_ID
import com.coida.data.FavoriteApp

/**
 * Una casilla del inicio: icono grande y nombre debajo.
 *
 * Con dos o más por fila es **cuadrada** y no se estira nunca (D-54): en la prueba
 * con dos personas mayores, las casillas altas de antes parecían carteles y
 * cambiaban de forma según cuántas hubiera. El icono, los márgenes y la letra van
 * en proporción al lado, y el nombre se encoge hasta caber entero sin partir una
 * palabra ([TileLabels]). Con una por fila pasa a horizontal y de alto fijo, que
 * en un móvil estrecho aprovecha mejor el ancho; el icono sigue cuadrado.
 *
 * El icono va directamente sobre la casilla, sin plato (D-35): solo su silueta
 * dentro del contorno exterior. El cian se lee a 6,34:1 sobre la casilla y a
 * 5,27:1 sobre el fondo de pulsado, por encima del 3:1 que piden los gráficos.
 *
 * Decisiones de accesibilidad:
 *  · Toda la casilla es el área pulsable.
 *  · Se separa del fondo con un contorno en vez de con una sombra: una sombra
 *    tenue desaparece para quien tiene poca sensibilidad al contraste.
 *  · El icono es decorativo (contentDescription = null): quien use TalkBack ya oye
 *    el texto de la casilla, y anunciarlo dos veces solo estorba.
 *  · Si la app no está, la casilla se queda en su sitio, apagada y avisando.
 *  · [highlighted] la señala el asistente al guiar: rellena de acento y con borde
 *    grueso, sin moverla ni agrandarla, y TalkBack lo dice. El icono pasa al
 *    color que va encima del acento.
 *
 * @param onClick null para pintarla sin que se pueda pulsar: la maqueta de
 *   Interfaz y «Colocar las casillas», donde el toque lo lleva quien la envuelve.
 * @param labelScale el tamaño de letra común de su rejilla ([rememberTileLabelScale]);
 *   null para que lo calcule ella sola.
 */
@Composable
fun AppTile(
    app: FavoriteApp,
    horizontal: Boolean,
    onClick: (() -> Unit)?,
    modifier: Modifier = Modifier,
    highlighted: Boolean = false,
    labelScale: Float? = null,
) {
    val colors = MaterialTheme.colorScheme
    // Señalada: rellena de acento. Así se distingue también en gris y en el tema
    // claro, donde el contorno normal ya es del mismo marino que el acento.
    val contentColor = when {
        // Señalada o cogida va antes que apagada: el gris sobre el acento se
        // quedaba a 1,13:1. El «No disponible» sigue escrito.
        highlighted -> colors.onPrimary
        !app.isAvailable -> colors.outlineVariant
        else -> colors.onSurface
    }
    val iconTint = when {
        // Señalada o cogida va antes que apagada: el gris sobre el acento se
        // quedaba a 1,13:1. El «No disponible» sigue escrito.
        highlighted -> colors.onPrimary
        !app.isAvailable -> colors.outlineVariant
        // Emergencias, con el icono en rojo: 6,54:1 en claro y 8,67:1 en oscuro sobre
        // la casilla. El nombre va en el color de siempre: se reconoce por el icono
        // y por el texto, no solo por el color (D-51).
        app.id == EMERGENCY_FAVORITE_ID -> colors.error
        else -> colors.primary
    }

    val openLabel = stringResource(R.string.cd_open_app, app.label)
    val unavailableLabel = stringResource(R.string.cd_app_unavailable, app.label)
    val highlightedLabel = stringResource(R.string.cd_highlighted)
    val detail = appTileDetail(app)

    TileFrame(
        onClick = onClick,
        enabled = app.isAvailable,
        highlighted = highlighted,
        horizontal = horizontal,
        modifier = modifier,
        semantics = {
            if (app.isAvailable) {
                if (onClick != null) onClick(label = openLabel) { onClick(); true }
            } else {
                contentDescription = unavailableLabel
            }
            if (highlighted) stateDescription = highlightedLabel
        },
        icon = { size -> TileIcon(app, iconTint, size) },
        labels = { align, labelModifier ->
            TileLabels(app.label, detail, contentColor, contentColor, align, labelModifier, labelScale)
        },
    )
}

/**
 * Acceso directo a una persona, con la forma de una casilla: su foto de la agenda
 * (D-57) o, si no tiene, la inicial, en un círculo (D-35) que es lo que la distingue
 * de una app. Debajo van el nombre y lo que hace (llamar o WhatsApp).
 */
@Composable
internal fun ContactTile(
    contact: HomeContact,
    horizontal: Boolean,
    highlighted: Boolean,
    onClick: (() -> Unit)?,
    modifier: Modifier = Modifier,
    labelScale: Float? = null,
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
    val detail = contactTileDetail(contact)
    val highlightedLabel = stringResource(R.string.cd_highlighted)
    // Señalado por el asistente: relleno de acento, como las casillas.
    val contentColor = when {
        highlighted -> colors.onPrimary
        !contact.isAvailable -> colors.outlineVariant
        else -> colors.onSurface
    }
    val secondaryColor = when {
        highlighted -> colors.onPrimary
        !contact.isAvailable -> colors.outlineVariant
        else -> colors.onSurfaceVariant
    }

    TileFrame(
        onClick = onClick,
        enabled = contact.isAvailable,
        highlighted = highlighted,
        horizontal = horizontal,
        modifier = modifier,
        semantics = {
            if (contact.isAvailable) {
                if (onClick != null) onClick(label = clickLabel) { onClick(); true }
            } else {
                contentDescription = unavailableLabel
            }
            if (highlighted) stateDescription = highlightedLabel
        },
        icon = { size ->
            ContactAvatar(
                name = shortcut.name,
                photo = contact.photo,
                size = size,
                enabled = contact.isAvailable,
                onAccent = highlighted,
            )
        },
        labels = { align, labelModifier ->
            TileLabels(shortcut.name, detail, contentColor, secondaryColor, align, labelModifier, labelScale)
        },
    )
}

/**
 * El marco común de casillas y contactos: el contorno, el relleno de señalado y el
 * reparto del icono y los textos, cuadrado o en fila.
 *
 * El radio de las esquinas también va en proporción al lado: con 28 dp fijos, una
 * casilla de 62 dp (cuatro por fila en 320 dp) parecía un círculo y las esquinas
 * se comían el nombre.
 *
 * @param modifier el sitio de la casilla en su padre (p. ej. un `weight`).
 * @param semantics lo que dice TalkBack, sobre la pieza pulsable.
 */
@Composable
private fun TileFrame(
    onClick: (() -> Unit)?,
    enabled: Boolean,
    highlighted: Boolean,
    horizontal: Boolean,
    modifier: Modifier,
    semantics: SemanticsPropertyReceiver.() -> Unit,
    icon: @Composable (Dp) -> Unit,
    labels: @Composable (TextAlign, Modifier) -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    BoxWithConstraints(
        // Con una por fila, el alto es un mínimo: con la letra muy grande, la fila
        // crece en vez de dejar el nombre diminuto. Cuadrada no crece nunca (D-54).
        modifier = if (horizontal) {
            modifier.fillMaxWidth().heightIn(min = CoidaDimens.TileRowHeight)
        } else {
            modifier.fillMaxWidth().aspectRatio(1f)
        },
        propagateMinConstraints = true,
    ) {
        val side = minOf(maxWidth, maxHeight)
        CoidaCard(
            onClick = onClick,
            enabled = enabled,
            highlighted = highlighted,
            // Apagada, con el contorno a trazos: con cuatro por fila el «No
            // disponible» es pequeño, y en gris el color solo no la distingue.
            dashed = !enabled,
            color = if (highlighted) colors.primary else colors.surfaceContainer,
            borderWidth = if (highlighted) CoidaDimens.HighlightBorderWidth else CoidaDimens.BorderWidth,
            modifier = Modifier
                .fillMaxSize()
                .semantics(properties = semantics),
            shape = RoundedCornerShape(
                (side * CoidaDimens.TileCornerRatio).coerceAtMost(CoidaDimens.TileCornerRadius),
            ),
        ) {
            if (horizontal) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = ROW_PADDING_H, vertical = ROW_PADDING_V),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(ROW_SPACING),
                ) {
                    icon(CoidaDimens.TileIconSizeCompact)
                    labels(TextAlign.Start, Modifier.weight(1f))
                }
            } else {
                val geometry = SquareTileGeometry(side)
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(geometry.padding),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(geometry.spacing, Alignment.CenterVertically),
                ) {
                    icon(geometry.iconSize)
                    labels(TextAlign.Center, Modifier.fillMaxWidth().weight(1f, fill = false))
                }
            }
        }
    }
}

@Composable
private fun TileIcon(app: FavoriteApp, tint: Color, iconSize: Dp) {
    when (val icon = app.icon) {
        is AppIcon.Vector -> Icon(
            painter = painterResource(icon.res),
            contentDescription = null,
            modifier = Modifier.size(iconSize),
            tint = tint,
        )

        is AppIcon.Bitmap -> Image(
            bitmap = icon.image,
            contentDescription = null,
            modifier = Modifier.size(iconSize),
            alpha = if (app.isAvailable) 1f else 0.35f,
        )
    }
}

/**
 * El nombre y, si hay, lo de debajo («No disponible», «Llamada»), en dos líneas
 * como mucho cada uno. Los dos bajan de tamaño a la vez hasta caber en el hueco que
 * deja el icono, sin partir nunca una palabra (D-54): «Mensaje/s» no se reconoce y
 * molestaba. Solo si ni al mínimo cabe, se recorta con «…».
 */
@Composable
private fun TileLabels(
    name: String,
    detail: String?,
    color: Color,
    detailColor: Color,
    align: TextAlign,
    modifier: Modifier,
    labelScale: Float?,
) {
    val nameStyle = MaterialTheme.typography.labelLarge
    val detailStyle = MaterialTheme.typography.labelSmall
    BoxWithConstraints(modifier = modifier) {
        val gap = with(LocalDensity.current) { TileSecondaryGap.roundToPx() }
        // La letra común de la rejilla, salvo que esta casilla necesite menos: la
        // común de las apps no cuenta el «No disponible», para que desinstalar una
        // app no encoja todo el inicio.
        val own = rememberFitScale(tileLines(name, detail), constraints.maxWidth, constraints.maxHeight, gap)
        val scale = labelScale?.let { minOf(it, own.scale) } ?: own.scale
        // Lo de debajo, nunca por debajo de su mínimo legible: la letra común de las
        // apps se calcula sin él, y lo dejaba en 6 dp. Si no cabe, «…».
        val detailScale = maxOf(scale, minFitScale(detailStyle, LocalDensity.current))
        // A todo lo ancho: si no, el texto se queda pegado a la izquierda aunque pida
        // ir centrado.
        Column(modifier = Modifier.fillMaxWidth()) {
            // Si ni al mínimo cabe, en una línea con «…»: así Android no parte la
            // palabra por letras, que es lo que molestaba.
            Text(
                text = name,
                style = nameStyle.scaledBy(scale),
                color = color,
                textAlign = align,
                maxLines = if (own.fits) 2 else 1,
                softWrap = own.fits,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.fillMaxWidth(),
            )
            if (detail != null) {
                Text(
                    text = detail,
                    style = detailStyle.scaledBy(detailScale),
                    color = detailColor,
                    textAlign = align,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = TileSecondaryGap),
                )
            }
        }
    }
}

/** Los textos de una casilla tal y como se ajustan: nombre y, si hay, lo de debajo. */
@Composable
private fun tileLines(name: String, detail: String?): List<FitLine> {
    val nameStyle = MaterialTheme.typography.labelLarge
    val detailStyle = MaterialTheme.typography.labelSmall
    return buildList {
        add(FitLine(name, nameStyle, maxLines = 2))
        if (detail != null) add(FitLine(detail, detailStyle, maxLines = 2))
    }
}

/** Lo que va debajo del nombre de una casilla de app: nada, o «No disponible». */
@Composable
internal fun appTileDetail(app: FavoriteApp): String? =
    if (app.isAvailable) null else stringResource(R.string.app_unavailable)

/**
 * Lo que va debajo del nombre de un contacto: lo que hace, o qué falta. Solo se
 * apaga el de WhatsApp sin WhatsApp, y dice eso y no un «No disponible» a secas:
 * con la misma persona dos veces, así se sabe cuál es.
 */
@Composable
internal fun contactTileDetail(contact: HomeContact): String = stringResource(
    when {
        !contact.isAvailable -> R.string.contact_whatsapp_missing_short
        contact.shortcut.action == ContactAction.CALL -> R.string.contact_action_call
        else -> R.string.contact_action_whatsapp
    },
)

/** Margen, icono y hueco del texto de una casilla cuadrada de lado [side]. */
private class SquareTileGeometry(side: Dp) {
    val padding = (side * CoidaDimens.TilePaddingRatio).coerceIn(CoidaDimens.TilePaddingMin, CoidaDimens.TilePaddingMax)
    val iconSize = (side * CoidaDimens.TileIconRatio).coerceIn(CoidaDimens.TileIconMin, CoidaDimens.TileIconMax)

    // Entre icono y nombre, medio margen: en una casilla pequeña cada dp que no va
    // al nombre es letra más pequeña.
    val spacing = padding / 2
    val textWidth = side - padding * 2
    val textHeight = side - padding * 2 - iconSize - spacing
}

/**
 * El tamaño de letra común a todas las casillas de una rejilla de [width] de ancho
 * por casilla: el que necesita el nombre que menos cabe. Así todas llevan la letra
 * igual de grande, y la rejilla se lee pareja.
 *
 * @param texts nombre y lo de debajo de cada casilla. Lo de debajo puede ir a null
 *   aunque la casilla lo lleve: entonces no cuenta para la letra común, y la casilla
 *   que lo lleva baja ella sola lo que le haga falta.
 */
@Composable
internal fun rememberTileLabelScale(texts: List<Pair<String, String?>>, width: Dp, horizontal: Boolean): Float {
    val density = LocalDensity.current
    val textWidth = if (horizontal) {
        width - ROW_PADDING_H * 2 - CoidaDimens.TileIconSizeCompact - ROW_SPACING
    } else {
        SquareTileGeometry(width).textWidth
    }
    // Con una por fila, la fila crece con el texto: solo cuenta el ancho.
    val textHeightPx = if (horizontal) {
        Constraints.Infinity
    } else {
        with(density) { SquareTileGeometry(width).textHeight.roundToPx() }
    }
    // El orden no cambia el resultado: ordenados, al recolocar casillas no se vuelve
    // a medir todo en cada cambio de sitio.
    val sorted = texts.sortedWith(compareBy({ it.first }, { it.second }))
    return rememberCommonFitScale(
        groups = sorted.map { (name, detail) -> tileLines(name, detail) },
        maxWidthPx = with(density) { textWidth.roundToPx() },
        maxHeightPx = textHeightPx,
        gapPx = with(density) { TileSecondaryGap.roundToPx() },
    )
}

/** Lo que separa el nombre de la línea de debajo («No disponible», «Llamada»). */
private val TileSecondaryGap = 4.dp

/** Márgenes y hueco de la casilla de una por fila, que va en horizontal. */
private val ROW_PADDING_H = 16.dp
private val ROW_PADDING_V = 12.dp
private val ROW_SPACING = 20.dp

@Preview(showBackground = true, widthDp = 200, heightDp = 240)
@Composable
private fun AppTilePreview() {
    CoidaTheme(darkTheme = true) {
        AppTile(
            app = FavoriteApp("phone", "Llamar", AppIcon.Vector(R.drawable.ic_call), "com.android.dialer"),
            horizontal = false,
            onClick = {},
            modifier = Modifier.width(200.dp),
        )
    }
}

@Preview(showBackground = true, widthDp = 80, heightDp = 100)
@Composable
private fun AppTileFourPerRowPreview() {
    CoidaTheme(darkTheme = true) {
        AppTile(
            app = FavoriteApp("emergency", "Emergencias 112", AppIcon.Vector(R.drawable.ic_emergency), "preview"),
            horizontal = false,
            onClick = {},
            modifier = Modifier.width(72.dp),
        )
    }
}

@Preview(showBackground = true, widthDp = 200, heightDp = 240)
@Composable
private fun ContactTilePreview() {
    CoidaTheme(darkTheme = true) {
        ContactTile(
            contact = HomeContact(ContactShortcut("1", "Tía Encarnación", "600000000", ContactAction.CALL, true), true),
            horizontal = false,
            highlighted = false,
            onClick = {},
            modifier = Modifier.width(200.dp),
        )
    }
}
