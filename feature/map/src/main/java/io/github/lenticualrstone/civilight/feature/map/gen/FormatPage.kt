package io.github.lenticualrstone.civilight.feature.map.gen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.github.lenticualrstone.civilight.designsystem.component.BottomGlassBar
import io.github.lenticualrstone.civilight.designsystem.component.ButtonRow
import io.github.lenticualrstone.civilight.designsystem.component.ButtonVariant
import io.github.lenticualrstone.civilight.designsystem.component.CivilightButton
import io.github.lenticualrstone.civilight.designsystem.component.CivilightText
import io.github.lenticualrstone.civilight.designsystem.component.FormatTile
import io.github.lenticualrstone.civilight.designsystem.component.IconAction
import io.github.lenticualrstone.civilight.designsystem.icon.CivilightIcon
import io.github.lenticualrstone.civilight.designsystem.icon.IconId
import io.github.lenticualrstone.civilight.designsystem.theme.CivilightTheme
import io.github.lenticualrstone.civilight.feature.map.MapCopy
import io.github.lenticualrstone.civilight.model.map.MapFormat
import io.github.lenticualrstone.civilight.ui.scaffold.CivilightScreen

/**
 * `양식` (WLD-54 1/2, board `MapFormat`): the note `같은 세계 · 시드 4827 · 양식만 다름 · 시작 전까지
 * 바꾸기 가능`, a 2-column grid of format cards (preview of the same world, title, small line) with
 * `글로 설명` (WLD-61), and `이전 · 단계별로 만들기`. `노드` has no steps: its button makes the node map.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun FormatPage(
    state: GenFlowState,
    vm: MapGenerateViewModel,
    back: IconAction,
    readOnly: Boolean,
    onPick: (MapFormat) -> Unit,
    onText: () -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
) {
    val ui = GenFlow.ui(state)
    val input = state.input()
    CivilightScreen(
        title = MapCopy.FORMAT_TITLE,
        smallLine = MapCopy.FORMAT_SMALL,
        back = back,
        bottomBar = {
            BottomGlassBar(barHeight = 110.dp) {
                ButtonRow(Modifier.fillMaxWidth()) {
                    CivilightButton(MapCopy.BTN_PREVIOUS, onPrevious, Modifier.weight(1f), variant = ButtonVariant.GHOST)
                    CivilightButton(MapCopy.BTN_BY_STEPS, onNext, Modifier.weight(2f), enabled = !readOnly || state.format != MapFormat.NODE)
                }
            }
        },
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(top = padding.calculateTopPadding(), bottom = padding.calculateBottomPadding() + 16.dp, start = 24.dp, end = 24.dp),
        ) {
            CivilightText(ui.formatNote, CivilightTheme.type.small, color = CivilightTheme.colors.ink3)
            BoxWithConstraints(Modifier.padding(top = 14.dp).fillMaxWidth()) {
                // Fixed-width tiles spread across the width: the leftover goes between the columns (QA 7).
                val tile = 166f
                val columns = GridFill.columns(maxWidth.value, tile)
                val gap = GridFill.gap(maxWidth.value, tile, columns).dp
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    GridFill.rows(FormatCard.shown, columns).forEach { row ->
                        Row(horizontalArrangement = Arrangement.spacedBy(gap)) {
                            row.forEach { card ->
                                val format = card.format
                                Column(Modifier.width(166.dp)) {
                                    FormatTile(
                                        selected = format != null && format == state.format,
                                        onClick = { if (format == null) onText() else onPick(format) },
                                    ) {
                                        when (format) {
                                            null -> TextCardPreview()
                                            MapFormat.NODE -> NodeCardPreview()
                                            else -> WorldPreview(vm, input, format)
                                        }
                                    }
                                    CivilightText(card.title, CivilightTheme.type.itemTitle, Modifier.padding(top = 7.dp), maxLines = 1)
                                    CivilightText(card.sub, CivilightTheme.type.small, color = CivilightTheme.colors.ink3, maxLines = 1)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TextCardPreview() {
    Column(
        Modifier.fillMaxSize().background(CivilightTheme.colors.tint).padding(horizontal = 14.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterVertically),
    ) {
        CivilightIcon(IconId.Text, null, size = 18.dp, tint = CivilightTheme.colors.acc)
        CivilightText(MapCopy.FORMAT_TEXT_SAMPLE, CivilightTheme.type.body, color = CivilightTheme.colors.ink2, maxLines = 3)
    }
}

/**
 * The `노드` card's preview (board `MapFormat`, `scr_mapgen.node_mini`): five node boxes joined by lines on `bg`,
 * drawn at the board's numbers and centred in the tile.
 */
@Composable
private fun NodeCardPreview() {
    val colors = CivilightTheme.colors
    androidx.compose.foundation.Canvas(Modifier.fillMaxSize().background(colors.bg)) {
        val u = density
        val ox = (size.width - NodeMini.WIDTH * u) / 2f
        val oy = (size.height - NodeMini.HEIGHT * u) / 2f
        fun at(x: Float, y: Float) = androidx.compose.ui.geometry.Offset(ox + x * u, oy + y * u)
        NodeMini.LINKS.forEach { (a, b) -> drawLine(colors.ink3, at(a.first, a.second), at(b.first, b.second), strokeWidth = u) }
        val box = androidx.compose.ui.geometry.Size(NodeMini.BOX_W * u, NodeMini.BOX_H * u)
        val r = androidx.compose.ui.geometry.CornerRadius(5f * u)
        NodeMini.BOXES.forEach { (x, y) ->
            drawRoundRect(colors.raise, at(x, y), box, r)
            drawRoundRect(colors.ink3, at(x, y), box, r, style = androidx.compose.ui.graphics.drawscope.Stroke(u))
        }
    }
}

/** The board's mini node graph, in dp (`node_mini`: boxes 34 × 18, radius 5). */
internal object NodeMini {
    const val BOX_W = 34f
    const val BOX_H = 18f
    const val WIDTH = 145f
    const val HEIGHT = 102f
    val BOXES: List<Pair<Float, Float>> = listOf(23f to 9f, 23f to 48f, 7f to 84f, 95f to 84f, 111f to 66f)
    val LINKS: List<Pair<Pair<Float, Float>, Pair<Float, Float>>> = listOf(
        (40f to 27f) to (40f to 48f),
        (40f to 66f) to (24f to 84f),
        (40f to 66f) to (112f to 84f),
        (112f to 102f) to (128f to 84f),
    )
}
