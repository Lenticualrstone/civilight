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
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.github.lenticualrstone.civilight.designsystem.component.CivilightText
import io.github.lenticualrstone.civilight.designsystem.glass.LocalGlassSource
import io.github.lenticualrstone.civilight.designsystem.glass.glassSource
import io.github.lenticualrstone.civilight.designsystem.glass.rememberGlassSource
import io.github.lenticualrstone.civilight.designsystem.theme.CivilightTheme

/**
 * (0.2.3, r3) `가운데 한 칸` is retired: there is no centred-column layout any more, and [content] always fills the
 * frame. Kept as a pass-through so screens that still wrap themselves in it compile unchanged; remove the call
 * sites at leisure. On the inner screen a two-pane screen closes its side panel by the seam button instead.
 */
@Composable
fun CenterColumn(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Box(modifier.fillMaxSize()) { content() }
}

/** Copy of the `목록 + 상세` frame (CNV-33). */
object ListDetailCopy {
    /**
     * The right pane before anything is selected (Q-LY-02 leaves it open). Follows `고른 블록 없음` and the
     * setting line `왼쪽 목록 · 오른쪽 고른 항목`; listed under "Copy to confirm" in `docs/status/shell.md`.
     */
    const val NO_SELECTION = "고른 항목 없음"

    val all: List<String> = listOf(NO_SELECTION)
}

/** The right pane of `목록 + 상세` before a selection: one centred `ink3` line on `bg` (Q-LY-02). */
@Composable
fun EmptyDetailPane(modifier: Modifier = Modifier, text: String = ListDetailCopy.NO_SELECTION) {
    Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CivilightText(text, CivilightTheme.type.label, color = CivilightTheme.colors.ink3, maxLines = 1)
    }
}

/**
 * `목록 + 상세` (CNV-33, r3): [list] at 300 dp upright or 320 on its side (at most half of the frame, Q-LY-01), a
 * 1 dp `line` divider, [detail] in the rest. [detail] null shows [emptyDetail] centred in the right pane
 * ([EmptyDetailPane], Q-LY-02: nothing selected yet; null leaves it blank on `bg`). With a [module] id
 * (`PaneSplit.Modules`) the frame is [SplitPanes]: the module's CNV-47 ratio and the r3 seam button that closes
 * and reopens the list ([onListClosedChange] reports it); without one it keeps the fixed split. [bottomBar] is the
 * frame's one bottom bar (DS-65), drawn over both panes with the list pane's width.
 */
@Composable
fun ListDetailPanes(
    list: @Composable () -> Unit,
    detail: (@Composable () -> Unit)?,
    modifier: Modifier = Modifier,
    module: String? = null,
    emptyDetail: String? = ListDetailCopy.NO_SELECTION,
    bottomBar: (@Composable (leftDp: () -> Float) -> Unit)? = null,
    onListClosedChange: ((closed: Boolean) -> Unit)? = null,
    hasDetail: Boolean = detail != null,
) {
    val right: @Composable () -> Unit = {
        when {
            detail != null -> detail()
            emptyDetail != null -> EmptyDetailPane(text = emptyDetail)
        }
    }
    if (module != null) {
        SplitPanes(
            module,
            left = list,
            right = right,
            modifier = modifier,
            bottomBar = bottomBar,
            onPanelClosedChange = onListClosedChange,
            hasDetail = hasDetail,
        )
        return
    }
    val colors = CivilightTheme.colors
    val landscape = LocalWindowLayout.current.isLandscape
    BoxWithConstraints(modifier.fillMaxSize().background(colors.bg)) {
        val listWidth = PaneSplitRules.listDetailDefaultLeft(maxWidth.value, landscape).dp
        val source = rememberGlassSource()
        Row(Modifier.fillMaxSize().then(if (bottomBar != null) Modifier.glassSource(source) else Modifier)) {
            Box(Modifier.width(listWidth).fillMaxHeight()) { list() }
            Spacer(Modifier.width(1.dp).fillMaxHeight().background(colors.line))
            Box(Modifier.weight(1f).fillMaxHeight(), contentAlignment = Alignment.TopStart) { right() }
        }
        if (bottomBar != null) {
            Box(Modifier.align(Alignment.BottomCenter)) {
                CompositionLocalProvider(LocalGlassSource provides source) { bottomBar { listWidth.value } }
            }
        }
    }
}
