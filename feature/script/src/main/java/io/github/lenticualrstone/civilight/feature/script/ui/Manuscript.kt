package io.github.lenticualrstone.civilight.feature.script.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.github.lenticualrstone.civilight.designsystem.component.EmptyBlock
import io.github.lenticualrstone.civilight.designsystem.theme.CivilightMotion
import io.github.lenticualrstone.civilight.designsystem.theme.CivilightTheme
import io.github.lenticualrstone.civilight.feature.script.ScriptCopy
import io.github.lenticualrstone.civilight.feature.script.layout.ScriptLayout
import io.github.lenticualrstone.civilight.feature.script.motion.ScriptMotion
import io.github.lenticualrstone.civilight.feature.script.motion.StaggeredSwipeRow
import io.github.lenticualrstone.civilight.feature.script.panel.PanelTab
import io.github.lenticualrstone.civilight.feature.script.state.BlockRow
import io.github.lenticualrstone.civilight.feature.script.state.EditorState
import io.github.lenticualrstone.civilight.feature.script.state.EmptySceneRow
import io.github.lenticualrstone.civilight.feature.script.state.HeadRow
import io.github.lenticualrstone.civilight.feature.script.state.HiddenSceneRow
import io.github.lenticualrstone.civilight.feature.script.links.BlockTap
import io.github.lenticualrstone.civilight.domain.world.term.TermCandidate
import io.github.lenticualrstone.civilight.feature.script.state.OptionLine
import io.github.lenticualrstone.civilight.feature.script.state.ScriptRow
import io.github.lenticualrstone.civilight.feature.script.state.SpeakerModel
import io.github.lenticualrstone.civilight.model.Key
import io.github.lenticualrstone.civilight.model.script.BlockKind

/** Block gap of the manuscript (`space-7`, SCR-01). */
val BlockGap: Dp = 26.dp


/** What the manuscript calls back into. */
class ManuscriptActions(
    val onTapBlock: (row: BlockRow, rawOffset: Int?) -> Unit,
    /** A tap on an option line of a choice block (SCR-09). */
    val onTapOption: (row: BlockRow, option: OptionLine) -> Unit,
    val onSpeaker: (row: BlockRow) -> Unit,
    val onTapEmptyScene: (sceneKey: Key) -> Unit,
    val onOpenHead: (sceneKey: Key) -> Unit,
    /** The scene head's `⋯` (SCR-46 → the shared `UnitMenuSheet`, SCR-47). */
    val onHeadMenu: (sceneKey: Key) -> Unit,
    val onUnlockHidden: () -> Unit,
    val onNewScene: (() -> Unit)?,
    /** Speaker shown on an empty scene's line: a held pick, else the inherited one. */
    val emptySpeaker: (EmptySceneRow) -> SpeakerModel?,
    /** The enter motion of a new block finished (it must not replay when the row comes back). */
    val onEntered: (Key) -> Unit,
    /** The block with the caret: drawn by the screen (it owns the text field). */
    val editableBlock: @Composable (row: BlockRow, modifier: Modifier) -> Unit,
    /** The virtual first line of an empty scene while it holds the caret. */
    val editableEmpty: @Composable (row: EmptySceneRow, modifier: Modifier) -> Unit,
    /** A swipe button `시간 · 참고 · 속성` of a block (SCR-30). */
    val onPanel: (row: BlockRow, tab: PanelTab) -> Unit = { _, _ -> },
    /** The row whose swipe buttons are open changes (one at a time; null closes). */
    val onReveal: (rowKey: String?) -> Unit = {},
    /** A tap on a link, a `용어 후보` span or a recorder source outside typing (SCR-55, 56, 59); true = handled. */
    val onTapTarget: (row: BlockRow, tap: BlockTap) -> Boolean = { _, _ -> false },
    /** The candidate whose small window is open in [row], if any (SCR-56). */
    val openCandidate: (row: BlockRow) -> TermCandidate? = { null },
    /** The small window of [row]'s open candidate. */
    val candidateWindow: @Composable (row: BlockRow) -> Unit = {},
    /** The margin `▲` (SCR-56). */
    val onMarginWarning: (() -> Unit)? = null,
)

/**
 * The manuscript of one episode (SCR-01): scene heads and blocks typeset on `bg`, 26 dp apart, one
 * scroll for the whole episode. On the inner screen the column is up to 560 dp wide from x 40 (SCR-03, [ScriptLayout]).
 */
@Composable
fun Manuscript(
    rows: List<ScriptRow>,
    editor: EditorState,
    showParens: Boolean,
    readOnly: Boolean,
    entered: Key?,
    listState: LazyListState,
    padding: PaddingValues,
    layout: ScriptLayout,
    actions: ManuscriptActions,
    /** The scene whose `⋯` sheet is open: its head has the `sel` background (SCR-46). */
    headMenuOpen: Key? = null,
    /** `대사만` (SCR-27): a read-only view; taps go back to `쓰기` (through [ManuscriptActions.onTapBlock]). */
    previewOnly: Boolean = false,
    /** The block row whose swipe buttons are open (SCR-30). */
    revealedRow: String? = null,
) {
    val reduced = CivilightTheme.reducedMotion
    val column = if (layout.wide) Modifier.padding(start = layout.columnStart.dp).width(layout.columnWidth.dp) else Modifier.fillMaxWidth()
    if (rows.isEmpty()) {
        Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
            EmptyBlock(
                title = ScriptCopy.NO_SCENES,
                primaryLabel = ScriptCopy.NEW_SCENE.takeIf { actions.onNewScene != null },
                onPrimary = actions.onNewScene,
            )
        }
        return
    }
    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxHeight().then(column),
        contentPadding = PaddingValues(top = padding.calculateTopPadding() + 8.dp, bottom = padding.calculateBottomPadding() + BlockGap),
        verticalArrangement = Arrangement.spacedBy(BlockGap),
    ) {
        items(rows, key = { it.key }, contentType = { it::class }) { row ->
            val enter = if (row is BlockRow && row.blockKey == entered) {
                Modifier.enterFromBelow(row.key) { actions.onEntered(row.blockKey) }
            } else Modifier
            // SCR-67 / CNV-39: rows around a new block slide with it; a removed row closes its gap.
            // The row being typed never moves.
            val placement = ScriptMotion.placement(isCaretRow = row.key == editor.caretRowKey, entering = entered != null, reduced = reduced, editing = editor.editing)
            val place = Modifier.animateItem(
                fadeInSpec = null,
                placementSpec = placement?.let { ScriptMotion.token(it) }?.let { tween(it.durationMs, easing = it.easing) },
                fadeOutSpec = tween(ScriptMotion.removeMs(reduced), easing = if (reduced) LinearEasing else CivilightMotion.exit.easing),
            )
            Box(place) {
                when (row) {
                    is HeadRow -> HeadView(
                        row,
                        selected = headMenuOpen == row.sceneKey,
                        onOpen = { actions.onOpenHead(row.sceneKey) },
                        onMenu = { actions.onHeadMenu(row.sceneKey) },
                    )
                    is HiddenSceneRow -> HiddenSceneView(row, actions.onUnlockHidden)
                    is EmptySceneRow ->
                        if (editor.editing && editor.virtualScene == row.sceneKey) {
                            actions.editableEmpty(row, Modifier)
                        } else {
                            EmptySceneView(row, actions.emptySpeaker(row)) { actions.onTapEmptyScene(row.sceneKey) }
                        }
                    is BlockRow ->
                        if (!readOnly && !previewOnly && editor.caretBlock == row.blockKey && (row.isText || row.kind == BlockKind.CHOICE && editor.caretOption != null)) {
                            actions.editableBlock(row, enter)
                        } else {
                            // CNV-40: no swipe while a block is being typed.
                            SwipeBlock(row, revealedRow == row.key, actions, enter, enabled = !editor.editing) {
                                BlockView(
                                    row = row,
                                    selected = editor.selected == row.blockKey,
                                    showParens = showParens,
                                    inheritedHint = editor.inheritedHint == row.blockKey,
                                    onTapText = { offset ->
                                        actions.onReveal(null)
                                        actions.onTapBlock(row, offset)
                                    },
                                    onSpeaker = if (readOnly || previewOnly) null else ({ actions.onSpeaker(row) }),
                                    onTapOption = { option ->
                                        actions.onReveal(null)
                                        actions.onTapOption(row, option)
                                    },
                                    onTapTarget = if (previewOnly) null else ({ tap ->
                                        actions.onReveal(null)
                                        actions.onTapTarget(row, tap)
                                    }),
                                    openCandidate = actions.openCandidate(row),
                                    popover = { actions.candidateWindow(row) },
                                    onMarginWarning = actions.onMarginWarning,
                                )
                            }
                        }
                }
            }
        }
    }
}

/**
 * A block row with its swipe-left buttons (SCR-30, DS SwipeTabs): `◆ 시간` (`acc` when the block
 * carries a `◆`), `참고`, `속성`. The swipe only reveals them; a button selects the block and opens
 * the panel at that tab. TalkBack gets the same three as custom actions (no gesture needed).
 */
@Composable
private fun SwipeBlock(
    row: BlockRow,
    revealed: Boolean,
    actions: ManuscriptActions,
    modifier: Modifier,
    enabled: Boolean,
    content: @Composable () -> Unit,
) {
    fun open(tab: PanelTab) {
        actions.onReveal(null)
        actions.onPanel(row, tab)
    }
    StaggeredSwipeRow(
        revealed = revealed,
        onRevealedChange = { actions.onReveal(if (it) row.key else null) },
        onTime = { open(PanelTab.TIME) },
        onReference = { open(PanelTab.REF) },
        onProperties = { open(PanelTab.PROPS) },
        timeHasChange = row.hasChange,
        labels = listOf(PanelTab.TIME.label, PanelTab.REF.label, PanelTab.PROPS.label),
        enabled = enabled,
        modifier = modifier.semantics {
            customActions = listOf(PanelTab.TIME, PanelTab.REF, PanelTab.PROPS).map { tab ->
                CustomAccessibilityAction(tab.label) { open(tab); true }
            }
        },
        content = content,
    )
}

/**
 * A block created by the writer enters from 8 dp below over 320 ms, decelerating (SCR-12, SCR-67);
 * with reduced motion it fades in over 120 ms.
 */
@Composable
private fun Modifier.enterFromBelow(key: String, onDone: () -> Unit): Modifier {
    val reduced = CivilightTheme.reducedMotion
    val shift = with(LocalDensity.current) { 8.dp.toPx() }
    val progress = remember(key) { Animatable(0f) }
    LaunchedEffect(key) {
        val token = CivilightMotion.enter
        progress.animateTo(1f, if (reduced) tween(CivilightMotion.reducedFadeMs) else tween(token.durationMs, easing = token.easing))
        onDone()
    }
    return graphicsLayer {
        alpha = progress.value
        translationY = if (reduced) 0f else (1f - progress.value) * shift
    }
}
