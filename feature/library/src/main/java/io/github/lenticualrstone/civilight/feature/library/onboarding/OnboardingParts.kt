package io.github.lenticualrstone.civilight.feature.library.onboarding

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import io.github.lenticualrstone.civilight.designsystem.component.BottomGlassBar
import io.github.lenticualrstone.civilight.designsystem.component.ButtonRow
import io.github.lenticualrstone.civilight.designsystem.component.ButtonVariant
import io.github.lenticualrstone.civilight.designsystem.component.CivilightButton
import io.github.lenticualrstone.civilight.designsystem.component.CivilightText
import io.github.lenticualrstone.civilight.designsystem.component.GlassScaffold
import io.github.lenticualrstone.civilight.designsystem.component.StepBars
import io.github.lenticualrstone.civilight.designsystem.component.TextAction
import io.github.lenticualrstone.civilight.designsystem.foundation.readingMeasure
import io.github.lenticualrstone.civilight.designsystem.theme.CivilightTheme
import io.github.lenticualrstone.civilight.ui.scaffold.ToastOverlay

/**
 * The frame of the onboarding pages (boards `OnbLook` … `OnbVoice`, `Walk1` … `Walk3`): a top row
 * ([top]: step bars and `건너뛰기`), a scrolling body and a glass bottom bar holding [buttons]. On
 * inner screens everything sits in the centred 560 column (`ED`, spec §5 "centered 560 column").
 */
@Composable
internal fun OnbFrame(
    top: (@Composable () -> Unit)?,
    buttons: @Composable RowScope.() -> Unit,
    content: @Composable ColumnScope.() -> Unit,
) {
    val colors = CivilightTheme.colors
    GlassScaffold(
        topBar = {
            if (top != null) {
                Box(Modifier.fillMaxWidth().background(colors.bg).statusBarsPadding(), contentAlignment = Alignment.TopCenter) {
                    Box(Modifier.readingMeasure().fillMaxWidth().padding(start = 24.dp, end = 24.dp, top = 20.dp, bottom = 12.dp)) { top() }
                }
            } else {
                Box(Modifier.statusBarsPadding())
            }
        },
        bottomBar = {
            BottomGlassBar(barHeight = 120.dp) {
                Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    ButtonRow(Modifier.readingMeasure().fillMaxWidth(), content = buttons)
                }
            }
        },
        // Toasts (`연결`, the widget and app-lock refusals) sit above the bottom bar, as in `CivilightScreen`.
        overlay = { padding -> ToastOverlay(bottom = padding.calculateBottomPadding()) },
    ) { padding ->
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
            Column(
                Modifier
                    .readingMeasure()
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(start = 24.dp, end = 24.dp, top = padding.calculateTopPadding() + 18.dp, bottom = padding.calculateBottomPadding() + 16.dp),
                content = content,
            )
        }
    }
}

/** 4 step bars with `건너뛰기` at the right (`OnbLook`, `OnbSafe`, `OnbVoice`). */
@Composable
internal fun SetupTopRow(current: Int, onSkip: () -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
        StepBars(OnboardingFlow.SETUP_STEPS, current, Modifier.weight(1f))
        TextAction(OnboardingCopy.SKIP, onSkip)
    }
}

/** `처음 설정 · k/4` and the page title (800 28 / 38). */
@Composable
internal fun SetupHeading(step: Int, title: String) {
    val type = CivilightTheme.type
    val colors = CivilightTheme.colors
    CivilightText(OnboardingCopy.setupLine(step), type.sec, color = colors.ink3)
    CivilightText(title, type.pageTitle, Modifier.padding(top = 8.dp), color = colors.ink)
}

/** `나중에` (ghost) · `다음` (large primary), each half the row. */
@Composable
internal fun RowScope.LaterNextButtons(onLater: () -> Unit, onNext: () -> Unit) {
    CivilightButton(OnboardingCopy.LATER, onLater, Modifier.weight(1f), variant = ButtonVariant.GHOST)
    CivilightButton(OnboardingCopy.NEXT, onNext, Modifier.weight(1f), variant = ButtonVariant.PRIMARY_LARGE)
}

/** The 3-dot walkthrough progress: 22 × 3 bars, current `acc` (`Walk1` … `Walk3`). */
@Composable
internal fun WalkDots(page: Int) {
    val colors = CivilightTheme.colors
    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        repeat(OnboardingFlow.WALK_PAGES) { i ->
            Box(Modifier.size(22.dp, 3.dp).clip(RoundedCornerShape(2.dp)).background(if (i == page - 1) colors.acc else colors.line))
        }
    }
}
