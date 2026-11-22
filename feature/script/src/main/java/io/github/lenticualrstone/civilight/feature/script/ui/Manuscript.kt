package io.github.lenticualrstone.civilight.feature.script.ui

import androidx.compose.animation.core.Animatable
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
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.github.lenticualrstone.civilight.designsystem.component.EmptyBlock
import io.github.lenticualrstone.civilight.designsystem.theme.CivilightMotion
import io.github.lenticualrstone.civilight.designsystem.theme.CivilightTheme
import io.github.lenticualrstone.civilight.feature.script.ScriptCopy
import io.github.lenticualrstone.civilight.feature.script.state.BlockRow
import io.github.lenticualrstone.civilight.feature.script.state.EditorState
import io.github.lenticualrstone.civilight.feature.script.state.EmptySceneRow
import io.github.lenticualrstone.civilight.feature.script.state.HeadRow
import io.github.lenticualrstone.civilight.feature.script.state.HiddenSceneRow
import io.github.lenticualrstone.civilight.feature.script.state.ScriptRow
import io.github.lenticualrstone.civilight.feature.script.state.SpeakerModel
import io.github.lenticualrstone.civilight.model.Key

/** Block gap of the manuscript (`space-7`, SCR-01). */
val BlockGap: Dp = 26.dp

/** Reading column on the inner screen (SCR-03): 560 dp wide, 40 dp from the left. */
val InnerColumnWidth: Dp = 560.dp
val InnerColumnStart: Dp = 40.dp

/** What the manuscript calls back into. */
class ManuscriptActions(
    val onTapBlock: (row: BlockRow, rawOffset: Int?) -> Unit,
    val onSpeaker: (row: BlockRow) -> Unit,
    val onTapEmptyScene: (sceneKey: Key) -> Unit,
    val onOpenHead: (sceneKey: Key) -> Unit,
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
)

/**
 * The manuscript of one episode (SCR-01): scene heads and blocks typeset on `bg`, 26 dp apart, one
 * scroll for the whole episode. On the inner screen the column is 560 dp wide from x 40 (SCR-03).
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
    wide: Boolean,
    actions: ManuscriptActions,
) {
    val column = if (wide) Modifier.padding(start = InnerColumnStart).width(InnerColumnWidth) else Modifier.fillMaxWidth()
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
            when (row) {
                is HeadRow -> HeadView(row, onOpen = { actions.onOpenHead(row.sceneKey) }, onMenu = null)
                is HiddenSceneRow -> HiddenSceneView(row, actions.onUnlockHidden)
                is EmptySceneRow ->
                    if (editor.editing && editor.virtualScene == row.sceneKey) {
                        actions.editableEmpty(row, Modifier)
                    } else {
                        EmptySceneView(row, actions.emptySpeaker(row)) { actions.onTapEmptyScene(row.sceneKey) }
                    }
                is BlockRow ->
                    if (!readOnly && row.isText && editor.caretBlock == row.blockKey) {
                        actions.editableBlock(row, enter)
                    } else {
                        BlockView(
                            row = row,
                            selected = editor.selected == row.blockKey,
                            showParens = showParens,
                            inheritedHint = editor.inheritedHint == row.blockKey,
                            onTapText = { offset -> actions.onTapBlock(row, offset) },
                            onSpeaker = if (readOnly) null else ({ actions.onSpeaker(row) }),
                            modifier = enter,
                        )
                    }
            }
        }
    }
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
