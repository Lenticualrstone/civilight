package io.github.lenticualrstone.civilight.ui.window

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.github.lenticualrstone.civilight.designsystem.theme.CivilightTheme

/**
 * `가운데 한 칸` (CNV-33): on an inner screen with that setting, [content] is one column at reading
 * width 560 centred on `bg`, with 1 dp `line` at both sides and the margins filled with `raise` at
 * 50 %. Elsewhere [content] fills the width.
 */
@Composable
fun CenterColumn(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    val layout = LocalWindowLayout.current
    if (!layout.centerColumn) {
        Box(modifier.fillMaxSize()) { content() }
        return
    }
    val colors = CivilightTheme.colors
    BoxWithConstraints(modifier.fillMaxSize().background(colors.raise.copy(alpha = colors.raise.alpha * 0.5f))) {
        val columnWidth = minOf(WindowLayout.MEASURE_DP.dp, maxWidth - 2.dp)
        Row(Modifier.align(Alignment.Center).fillMaxHeight()) {
            Spacer(Modifier.width(1.dp).fillMaxHeight().background(colors.line))
            Box(Modifier.width(columnWidth).fillMaxHeight().background(colors.bg)) { content() }
            Spacer(Modifier.width(1.dp).fillMaxHeight().background(colors.line))
        }
    }
}

/**
 * `목록 + 상세` (CNV-33): [list] at 390 dp (or 50 % on narrow Medium windows, Q-LY-01), a 1 dp
 * `line` divider, [detail] in the rest. [detail] null leaves the right pane empty on `bg`
 * (Q-LY-02: nothing selected yet). With a [module] id (`PaneSplit.Modules`) the frame carries the CNV-47
 * handle and uses that module's stored ratio ([SplitPanes]); without one it keeps the fixed split.
 */
@Composable
fun ListDetailPanes(
    list: @Composable () -> Unit,
    detail: (@Composable () -> Unit)?,
    modifier: Modifier = Modifier,
    module: String? = null,
) {
    if (module != null) {
        SplitPanes(module, left = list, right = { detail?.invoke() }, modifier = modifier)
        return
    }
    val colors = CivilightTheme.colors
    BoxWithConstraints(modifier.fillMaxSize().background(colors.bg)) {
        val listWidth = PaneSplitRules.listDetailDefaultLeft(maxWidth.value).dp
        Row(Modifier.fillMaxSize()) {
            Box(Modifier.width(listWidth).fillMaxHeight()) { list() }
            Spacer(Modifier.width(1.dp).fillMaxHeight().background(colors.line))
            Box(Modifier.weight(1f).fillMaxHeight(), contentAlignment = Alignment.TopStart) { detail?.invoke() }
        }
    }
}
