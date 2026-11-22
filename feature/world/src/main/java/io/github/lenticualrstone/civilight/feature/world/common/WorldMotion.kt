package io.github.lenticualrstone.civilight.feature.world.common

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import io.github.lenticualrstone.civilight.designsystem.theme.CivilightTheme
import io.github.lenticualrstone.civilight.designsystem.theme.ContentSwap
import io.github.lenticualrstone.civilight.designsystem.theme.animatedAlpha
import io.github.lenticualrstone.civilight.designsystem.theme.rememberUnhide
import io.github.lenticualrstone.civilight.designsystem.theme.unhide

/** Alpha of a row after the lens point (`쓰인 곳` rows, timeline and place-time rows). */
const val DIMMED_ALPHA = 0.5f

/** Opacity of a row that is dimmed because it lies after the lens point; changing it fades (`standard`). */
@Composable
fun Modifier.dimmed(on: Boolean): Modifier {
    val alpha by animatedAlpha(if (on) DIMMED_ALPHA else 1f, label = "dimmed")
    return graphicsLayer { this.alpha = alpha }
}

/** What a card screen shows: nothing yet, the lock line, the dotted label of a gone item, or the card. */
enum class BodyStage { LOADING, LOCKED, GONE, OPEN }

/**
 * The body of a card screen (P3, P5): moving between loading, locked, gone and open crossfades the body (a
 * change inside one stage does not, its rows animate themselves). Unlocking plays the shared unhide (CNV-43,
 * blur and opacity) on the open body; a card that opens already unlocked, or loads, appears clear. [card] is
 * kept per stage, so a leaving body keeps drawing the card it had.
 */
@Composable
fun <C : Any> CardBodySwap(card: C?, stage: (C?) -> BodyStage, content: @Composable (C?) -> Unit) {
    // Hoisted above the swap: first composed unlocked (or loading) it starts clear, so only a real unlock plays.
    val unhide = rememberUnhide(revealed = stage(card) != BodyStage.LOCKED)
    ContentSwap(state = card, modifier = Modifier.fillMaxSize(), contentKey = { stage(it) }, label = "cardBody") { shown ->
        Box(Modifier.fillMaxSize().then(if (stage(shown) == BodyStage.OPEN) Modifier.unhide(unhide) else Modifier)) { content(shown) }
    }
}

/** [CardAccess] as the body stage of a card screen. */
fun stageOf(access: CardAccess?): BodyStage = when (access) {
    null -> BodyStage.LOADING
    is CardAccess.Locked -> BodyStage.LOCKED
    is CardAccess.Gone -> BodyStage.GONE
    CardAccess.Open -> BodyStage.OPEN
}

/**
 * The steps of a sheet that replaces its body step by step (P3): a step forward pushes in an eighth of the width
 * on `enter`, back reverses (`motion.push`), reduced motion crossfades. [forward] says whether `to` lies ahead of
 * `from`. The step content is a `Column`, so it keeps the sheet's `ColumnScope` pieces (title row, scroll body).
 */
@Composable
fun <S> SheetSteps(step: S, forward: (from: S, to: S) -> Boolean, content: @Composable ColumnScope.(S) -> Unit) {
    val motion = CivilightTheme.motion
    AnimatedContent(
        targetState = step,
        transitionSpec = { motion.push(forward(initialState, targetState)) },
        label = "sheetStep",
    ) { shown ->
        Column(Modifier.fillMaxWidth()) { content(shown) }
    }
}
