package com.doado.core.ui

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.Hyphens
import androidx.compose.ui.text.style.LineBreak
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Density

/*
 * Texto que se encoge para caber entero, sin partir nunca una palabra (D-54).
 *
 * En la prueba con dos personas mayores molestaba mucho ver «Mensaje/s» o
 * «Contac/tos»: con casillas cuadradas y hasta cuatro por fila, el sitio es el que
 * es, así que la letra baja de tamaño hasta que cada palabra cabe en una línea y el
 * texto entero en su hueco. No vale el `TextAutoSize` de Compose: solo mira si el
 * texto se sale, y una palabra partida por letras no se sale.
 *
 * Se parte del estilo de `MaterialTheme.typography` que se le pase, ya con la escala
 * de letra de la persona, y se multiplica por un factor: nunca hay un tamaño suelto.
 */

/** Un texto que se ajusta junto con otros: comparten el mismo factor de tamaño. */
class FitLine(val text: String, val style: TextStyle, val maxLines: Int)

/**
 * El resultado de ajustar: el factor de tamaño y si con él cabe todo. Si no cabe ni
 * al mínimo, [fits] es false: entonces se pinta en una sola línea, con «…» al final,
 * para que Android no parta la palabra por letras. Es el último recurso, para un
 * nombre muy largo en una casilla muy pequeña.
 */
class FitResult(val scale: Float, val fits: Boolean)

/**
 * El mayor factor, de 1 hacia abajo, con el que todas las [lines] caben en
 * [maxWidthPx] por [maxHeightPx] (sin límite de alto si es [Constraints.Infinity])
 * con [gapPx] entre una y otra, y ninguna palabra partida. No baja de lo que deja la
 * línea más pequeña en [MIN_FIT_TEXT_DP] ([minFitScale]).
 */
@Composable
fun rememberFitScale(lines: List<FitLine>, maxWidthPx: Int, maxHeightPx: Int, gapPx: Int = 0): FitResult {
    val measurer = rememberTextMeasurer()
    val density = LocalDensity.current
    return remember(lines.map { Triple(it.text, it.style, it.maxLines) }, maxWidthPx, maxHeightPx, gapPx, density) {
        fitScale(measurer, density, lines, maxWidthPx.coerceAtLeast(1), maxHeightPx, gapPx)
    }
}

/**
 * El mayor factor con el que caben **todos** los [groups] a la vez, cada uno en su
 * hueco de [maxWidthPx] por [maxHeightPx]: el de la que más lo necesita. Lo usa la
 * rejilla del inicio para que todas las casillas lleven la letra igual de grande;
 * si cada una eligiera la suya, «Llamar» saldría enorme al lado de «Mensajes».
 */
@Composable
fun rememberCommonFitScale(groups: List<List<FitLine>>, maxWidthPx: Int, maxHeightPx: Int, gapPx: Int = 0): Float {
    val measurer = rememberTextMeasurer()
    val density = LocalDensity.current
    val keys = groups.map { group -> group.map { Triple(it.text, it.style, it.maxLines) } }
    return remember(keys, maxWidthPx, maxHeightPx, gapPx, density) {
        groups.minOfOrNull { fitScale(measurer, density, it, maxWidthPx.coerceAtLeast(1), maxHeightPx, gapPx).scale }
            ?: 1f
    }
}

/**
 * Lo menos que puede encogerse [style]: hasta [MIN_FIT_TEXT_DP] en pantalla. El
 * suelo es un tamaño de verdad y no un porcentaje: con la letra del sistema al
 * 200 %, un 40 % aún era letra grande y «Encarnación» no cabía; con cuatro por fila
 * en «Normal», un 40 % ya era casi ilegible.
 */
fun minFitScale(style: TextStyle, density: Density): Float {
    // Un estilo sin tamaño (no debería haberlo) cuenta como uno de 16 sp.
    val size = (style.fontSize.value.takeIf { it > 0f } ?: 16f) * density.fontScale
    return (MIN_FIT_TEXT_DP / size).coerceIn(0.05f, 1f)
}

private fun fitScale(
    measurer: TextMeasurer,
    density: Density,
    lines: List<FitLine>,
    maxWidthPx: Int,
    maxHeightPx: Int,
    gapPx: Int,
): FitResult {
    val floor = lines.maxOf { minFitScale(it.style, density) }

    // Se empieza donde cabe la palabra más ancha, que el tamaño del texto escala casi
    // en línea recta: así no se mide en cada paso lo que de antemano no cabe.
    val widest = lines.maxOf { line ->
        line.text.split(' ', '\n').filter { it.isNotBlank() }.maxOfOrNull { word ->
            measurer.measure(word, line.style.scaledBy(1f), softWrap = false, maxLines = 1, density = density)
                .size.width
        } ?: 0
    }
    var scale = if (widest > maxWidthPx) (maxWidthPx.toFloat() / widest).coerceIn(floor, 1f) else 1f
    while (scale > floor) {
        if (fits(measurer, density, lines, scale, maxWidthPx, maxHeightPx, gapPx)) return FitResult(scale, true)
        scale -= FIT_STEP
    }
    // El suelo se prueba aparte, exacto: restando a un Float se podía quedar fuera.
    return FitResult(floor, fits(measurer, density, lines, floor, maxWidthPx, maxHeightPx, gapPx))
}

private fun fits(
    measurer: TextMeasurer,
    density: Density,
    lines: List<FitLine>,
    scale: Float,
    maxWidthPx: Int,
    maxHeightPx: Int,
    gapPx: Int,
): Boolean {
    var height = 0
    lines.forEachIndexed { index, line ->
        val style = line.style.scaledBy(scale)
        val wordTooWide = line.text.split(' ', '\n').filter { it.isNotBlank() }.any { word ->
            measurer.measure(word, style, softWrap = false, maxLines = 1, density = density).size.width > maxWidthPx
        }
        if (wordTooWide) return false
        val layout = measurer.measure(
            text = line.text,
            style = style,
            maxLines = line.maxLines,
            constraints = Constraints(maxWidth = maxWidthPx),
            density = density,
        )
        if (layout.hasVisualOverflow) return false
        height += layout.size.height + if (index > 0) gapPx else 0
    }
    return maxHeightPx == Constraints.Infinity || height <= maxHeightPx
}

/** [this] con la letra y el interlineado multiplicados por [scale], y sin partir palabras. */
fun TextStyle.scaledBy(scale: Float): TextStyle = copy(
    fontSize = fontSize * scale,
    lineHeight = lineHeight * scale,
    hyphens = Hyphens.None,
    lineBreak = LineBreak.Heading,
)

/**
 * Un texto suelto que se encoge para caber en el sitio que le dé su padre, sin
 * partir palabras. Para varios textos que deben bajar a la vez (el nombre y la
 * línea de debajo de una casilla), [rememberFitScale] con todos ellos.
 */
@Composable
fun FitText(
    text: String,
    style: TextStyle,
    color: Color,
    modifier: Modifier = Modifier,
    textAlign: TextAlign? = null,
    maxLines: Int = 2,
) {
    BoxWithConstraints(modifier = modifier) {
        val fit = rememberFitScale(
            lines = listOf(FitLine(text, style, maxLines)),
            maxWidthPx = constraints.maxWidth,
            maxHeightPx = constraints.maxHeight,
        )
        Text(
            text = text,
            style = style.scaledBy(fit.scale),
            color = color,
            textAlign = textAlign,
            maxLines = if (fit.fits) maxLines else 1,
            softWrap = fit.fits,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

/**
 * Lo más pequeña que se pinta una línea al encogerse, en dp de pantalla (ya con la
 * escala de letra). Con cuatro por fila en un móvil de 320 dp, «Emergencias» cabe
 * justo a este tamaño; más pequeña no se lee.
 */
private const val MIN_FIT_TEXT_DP = 8f

/** Lo que baja el factor en cada intento. */
private const val FIT_STEP = 0.05f
