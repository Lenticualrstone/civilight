package io.github.lenticualrstone.civilight.feature.script.ui

import androidx.compose.animation.core.Animatable
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
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.key
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
import io.github.lenticualrstone.civilight.designsystem.theme.rememberUnhide
import io.github.lenticualrstone.civilight.designsystem.theme.unhide
import io.github.lenticualrstone.civilight.feature.script.ScriptCopy
import io.github.lenticualrstone.civilight.feature.script.ScriptTourSteps
import io.github.lenticualrstone.civilight.ui.tour.TourStepAbsent
import io.github.lenticualrstone.civilight.ui.tour.tourRunning
import io.github.lenticualrstone.civilight.ui.tour.tourTarget
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
import io.github.lenticualrstone.civilight.feature.script.state.ScriptRowKeys
import io.github.lenticualrstone.civilight.feature.script.state.SpeakerModel
import io.github.lenticualrstone.civilight.model.Key
import io.github.lenticualrstone.civilight.model.script.BlockKind

/** Block gap of the manuscript (`space-7`, SCR-01). */
val BlockGap: Dp = 26.dp


/** Padding above and below the selected block (SCR-14, board `Blocks`). */
private val SELECTED_PAD: Dp = 12.dp

/**
 * A row that appears after the manuscript was shown (a hidden scene the gate just opened) unblurs
 * and brightens in (CNV-43); a row that comes back from the scroll is clear at once.
 */
@Composable
private fun Modifier.revealOnAppear(key: String, revealed: MutableSet<String>): Modifier {
    var shown by remember { mutableStateOf(key in revealed) }
    LaunchedEffect(Unit) { shown = true }
    val unhide = rememberUnhide(shown)
    LaunchedEffect(shown, unhide.isRunning) { if (shown && !unhide.isRunning) revealed.add(key) }
    return unhide(unhide)
}

private val HIDDEN_PREFIX = ScriptRowKeys.hidden("")

/** The row keys the manuscript drew last (read in composition, written after it). */
private class KeysHolder {
    var keys: Set<String>? = null
}

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
    entered: Set<Key>,
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
    val motion = CivilightTheme.motion
    val column = if (layout.wide) Modifier.padding(start = layout.columnStart.dp).width(layout.columnWidth.dp) else Modifier.fillMaxWidth()
    if (rows.isEmpty()) {
        // LIB-19: no block and no scene head to point at, so those steps leave the tour.
        if (tourRunning()) ScriptTourSteps.missing(null, null, readOnly).forEach { step -> key(step) { TourStepAbsent(step) } }
        Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
            EmptyBlock(
                title = ScriptCopy.NO_SCENES,
                primaryLabel = ScriptCopy.NEW_SCENE.takeIf { actions.onNewScene != null },
                onPrimary = actions.onNewScene,
            )
        }
        return
    }
    // The row keys of the last composition: a change is an insert, a remove or a reorder (not a typed letter).
    val keys = remember(rows) { rows.mapTo(HashSet()) { it.key } }
    val revealedRows = remember { HashSet<String>() }
    val lastKeys = remember { KeysHolder() }
    // SCR-58 / CNV-43: the rows that came with the gate opening (a `hidden:` placeholder left this composition and
    // its scene's rows joined); never a block the writer makes later in that scene.
    val revealKeys = remember { HashSet<String>() }
    lastKeys.keys?.let { before ->
        val unlockedScenes = rows.filterIsInstance<HeadRow>().filter { it.hiddenTag }.mapTo(HashSet()) { it.sceneKey }
        val opened = before.filter { it.startsWith(HIDDEN_PREFIX) }.map { it.removePrefix(HIDDEN_PREFIX) }.filter { it in unlockedScenes }.toSet()
        if (opened.isNotEmpty()) rows.filter { it.sceneKey in opened && it.key !in before }.forEach { revealKeys.add(it.key) }
    }
    val structural = ScriptMotion.structural(lastKeys.keys, keys)
    // LIB-19: while the script tour shows, a block and a scene head on screen are its holes.
    val tourKeys = if (tourRunning()) {
        val blockKeys = remember(rows) { rows.filterIsInstance<BlockRow>().mapTo(HashSet()) { it.key } }
        val headKeys = remember(rows) { rows.filterIsInstance<HeadRow>().mapTo(HashSet()) { it.key } }
        val picked by remember(listState, blockKeys, headKeys) {
            derivedStateOf {
                val info = listState.layoutInfo
                val items = info.visibleItemsInfo.map { ScriptTourSteps.Item(it.key, it.offset, it.offset + it.size) }
                val bottom = info.viewportEndOffset - info.afterContentPadding
                ScriptTourSteps.firstVisible(items, 0, bottom) { it in blockKeys } to
                    ScriptTourSteps.firstVisible(items, 0, bottom) { it in headKeys }
            }
        }
        picked
    } else null
    // A step whose row is not on screen leaves the tour (its card would float with no hole).
    tourKeys?.let { (block, head) -> ScriptTourSteps.missing(block, head, readOnly).forEach { step -> key(step) { TourStepAbsent(step) } } }
    SideEffect { lastKeys.keys = keys }
    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxHeight().then(column),
        // The scene head brings its own 8 dp top margin (DS-43): no second gap under the bar (QA #13).
        contentPadding = PaddingValues(top = padding.calculateTopPadding(), bottom = padding.calculateBottomPadding() + BlockGap),
        verticalArrangement = Arrangement.spacedBy(BlockGap),
    ) {
        items(rows, key = { it.key }, contentType = { it::class }) { row ->
            val isEntering = row is BlockRow && row.blockKey in entered
            val enter = if (row is BlockRow && isEntering) {
                Modifier.enterFromBelow(row.key) { actions.onEntered(row.blockKey) }
            } else Modifier
            // SCR-67 / CNV-39: rows around a new block slide with it; a removed row closes its gap, also
            // while typing, whenever the row set changed. The row being typed never moves, and a pure
            // edit moves nothing.
            val placement = ScriptMotion.placement(isCaretRow = row.key == editor.caretRowKey, entering = entered.isNotEmpty(), structural = structural)
            val place = Modifier.animateItem(
                // A block made by the writer enters with its own rise; every other inserted row fades in.
                fadeInSpec = if (isEntering) null else motion.spec(CivilightMotion.enter),
                placementSpec = placement?.let { motion.placement(ScriptMotion.token(it)) },
                fadeOutSpec = motion.spec(CivilightMotion.exit),
            )
            // SCR-58 / CNV-43: the rows of a hidden scene that appear because the gate opened unblur in.
            val reveal = if (row.key in revealKeys) Modifier.revealOnAppear(row.key, revealedRows) else Modifier
            val tourMark = when (row.key) {
                tourKeys?.first -> Modifier.tourTarget(ScriptTourSteps.BLOCK)
                tourKeys?.second -> Modifier.tourTarget(ScriptTourSteps.HEAD)
                else -> Modifier
            }
            Box(place.then(reveal).then(tourMark)) {
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
                    is BlockRow -> {
                        // SCR-67: the selection's 12 dp padding grows and shrinks with the row, typing or not.
                        val pad = animateDpAsState(if (editor.selected == row.blockKey) SELECTED_PAD else 0.dp, motion.move(CivilightMotion.standard), label = "blockPad")
                        CompositionLocalProvider(LocalBlockPad provides pad) {
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
    val motion = CivilightTheme.motion
    val shift = with(LocalDensity.current) { ScriptMotion.ENTER_SHIFT_DP.dp.toPx() }
    val progress = remember(key) { Animatable(0f) }
    LaunchedEffect(key) {
        progress.animateTo(1f, motion.spec(CivilightMotion.enter))
        onDone()
    }
    return graphicsLayer {
        alpha = progress.value
        translationY = if (motion.reduced) 0f else (1f - progress.value) * shift
    }
}
