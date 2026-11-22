package io.github.lenticualrstone.civilight.feature.library.onboarding

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.InfiniteRepeatableSpec
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import io.github.lenticualrstone.civilight.designsystem.component.CivilightText
import io.github.lenticualrstone.civilight.designsystem.component.GlowMode
import io.github.lenticualrstone.civilight.designsystem.component.RecorderGlowBand
import io.github.lenticualrstone.civilight.designsystem.foundation.insetOutline
import io.github.lenticualrstone.civilight.designsystem.foundation.startHairline
import io.github.lenticualrstone.civilight.designsystem.foundation.topHairline
import io.github.lenticualrstone.civilight.designsystem.icon.CivilightIcon
import io.github.lenticualrstone.civilight.designsystem.icon.IconId
import io.github.lenticualrstone.civilight.designsystem.theme.CivilightMotion
import io.github.lenticualrstone.civilight.designsystem.theme.CivilightTheme
import kotlinx.coroutines.delay

/**
 * The looping demos of the walkthrough (LIB-16, boards `Walk1` … `Walk3`), drawn from the example's
 * words. Decorative: the page's title and sub say the same thing to a screen reader. With reduced
 * motion each demo shows its end state, still (`ED`).
 */
@Composable
internal fun WalkDemo(page: Int, modifier: Modifier = Modifier) {
    val colors = CivilightTheme.colors
    val shape = RoundedCornerShape(24.dp)
    Box(
        modifier
            .fillMaxWidth()
            .height(DEMO_HEIGHT)
            .clip(shape)
            .background(colors.raise)
            .insetOutline(colors.line, 1.dp, shape)
            .clearAndSetSemantics { },
    ) {
        when (page) {
            1 -> SwipeDemo()
            2 -> LensDemo()
            else -> VoiceDemo()
        }
    }
}

private val DEMO_HEIGHT = 420.dp

/** One round of the swipe demo: hold, slide out on `swipe`, hold, slide back; the loop length is the demo's own. */
private const val SWIPE_LOOP_MS = 4400
private const val SWIPE_HOLD_MS = 900
private const val SWIPE_OPEN_MS = 2900
private const val REVEAL_DP = 192f

/**
 * `Walk1`: 하르트's block slides left and `◆ 시간` · `참고` · `속성` come in behind it. The slide takes `swipeMs`
 * on the `standard` curve and each tab follows [CivilightMotion.swipeStaggerMs] after the one before (CNV-37).
 * Without loops (reduced motion, animator scale 0) it is the still end state: no clock runs.
 */
@Composable
private fun BoxScope.SwipeDemo() {
    if (CivilightTheme.motion.loops) {
        val transition = rememberInfiniteTransition(label = "walk1")
        val standard = CivilightMotion.standard.easing
        val shift by transition.animateFloat(
            0f, 0f,
            infiniteRepeatable(
                keyframes {
                    durationMillis = SWIPE_LOOP_MS
                    0f at SWIPE_HOLD_MS using standard
                    -REVEAL_DP at SWIPE_HOLD_MS + CivilightMotion.swipeMs
                    -REVEAL_DP at SWIPE_OPEN_MS using standard
                    0f at SWIPE_OPEN_MS + CivilightMotion.swipeMs
                },
            ),
            label = "swipe",
        )
        val tabs = List(3) { i -> tabProgress(transition, i * CivilightMotion.swipeStaggerMs) }
        SwipeFrame(shift = { shift }, tab = { tabs[it] })
    } else {
        SwipeFrame(shift = { -REVEAL_DP }, tab = { 1f })
    }
}

@Composable
private fun BoxScope.SwipeFrame(shift: () -> Float, tab: (Int) -> Float) {
    val colors = CivilightTheme.colors
    val type = CivilightTheme.type
    val density = LocalDensity.current
    Box(Modifier.align(Alignment.TopStart).padding(top = 150.dp).fillMaxWidth().height(92.dp)) {
        Row(Modifier.align(Alignment.CenterEnd).width(192.dp).fillMaxHeight().background(colors.bg)) {
            listOf("◆ " + OnboardingCopy.TAB_TIME, OnboardingCopy.TAB_REFERENCE, OnboardingCopy.TAB_PROPERTIES).forEachIndexed { i, label ->
                Box(
                    Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .graphicsLayer {
                            val p = tab(i)
                            alpha = p
                            translationX = (1f - p) * with(density) { 16.dp.toPx() }
                        }
                        .startHairline(colors.line),
                    contentAlignment = Alignment.Center,
                ) {
                    CivilightText(label, type.label, color = colors.ink)
                }
            }
        }
        Column(
            Modifier
                .width(350.dp)
                .fillMaxHeight()
                .graphicsLayer { translationX = with(density) { shift().dp.toPx() } }
                .background(colors.raise)
                .padding(horizontal = 24.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                CivilightText(OnboardingSample.HART_UNKNOWN, type.speakerName, color = colors.ink2)
                CivilightText(OnboardingSample.HART, type.speaker, color = colors.ink3)
            }
            CivilightText(OnboardingSample.HART_LINE, type.line, color = colors.ink)
        }
    }
}

/** One tab's appearance (0 hidden … 1 shown), [delayMs] after the first tab: in on `enter`, out on `standard`. */
@Composable
private fun tabProgress(transition: androidx.compose.animation.core.InfiniteTransition, delayMs: Int): Float {
    val enter = CivilightMotion.enter.easing
    val standard = CivilightMotion.standard.easing
    val spec: InfiniteRepeatableSpec<Float> = infiniteRepeatable(
        keyframes {
            durationMillis = SWIPE_LOOP_MS
            0f at SWIPE_HOLD_MS + delayMs using enter
            1f at SWIPE_HOLD_MS + CivilightMotion.swipeMs + delayMs
            1f at SWIPE_OPEN_MS + delayMs using standard
            0f at SWIPE_OPEN_MS + CivilightMotion.swipeMs + delayMs
        },
        RepeatMode.Restart,
    )
    val value by transition.animateFloat(0f, 0f, spec, label = "tab$delayMs")
    return value
}

/** `Walk2`: the lens chip flips `시점 · 1-7` ↔ `시점 · 1-2` and 리엔's values follow. */
@Composable
private fun BoxScope.LensDemo() {
    val colors = CivilightTheme.colors
    val type = CivilightTheme.type
    val motion = CivilightTheme.motion
    // Without loops the chip rests on the earlier point (`ED`: the still end state); with them it flips every 2.5 s.
    var earlier by remember { mutableStateOf(!motion.loops) }
    LaunchedEffect(motion.loops) {
        if (!motion.loops) {
            earlier = true
            return@LaunchedEffect
        }
        while (true) {
            delay(motion.delayMs(LENS_HOLD_MS.toInt()))
            earlier = !earlier
        }
    }
    Column(Modifier.align(Alignment.TopStart).fillMaxWidth().padding(start = 24.dp, end = 24.dp, top = 40.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            CivilightText(OnboardingSample.RIEN, type.stateTitle, Modifier.weight(1f), color = colors.ink)
            Box(
                Modifier.heightIn(min = 32.dp).clip(RoundedCornerShape(16.dp)).insetOutline(colors.line, 1.dp, RoundedCornerShape(16.dp)).padding(horizontal = 12.dp),
                contentAlignment = Alignment.Center,
            ) {
                Swap(earlier, OnboardingCopy.LENS_LATER, OnboardingCopy.LENS_EARLIER) { CivilightText(it, type.label, color = colors.ink) }
            }
        }
        Column(Modifier.padding(top = 20.dp)) {
            LensRow(OnboardingCopy.ROW_AGE, earlier, OnboardingSample.AGE, OnboardingSample.AGE)
            LensRow(OnboardingCopy.ROW_FACTION, earlier, OnboardingSample.FACTION_LATER, OnboardingSample.FACTION_EARLIER)
            LensRow(OnboardingCopy.ROW_WITH_HART, earlier, OnboardingSample.HART_LATER, OnboardingSample.HART_EARLIER)
        }
    }
}

private const val LENS_HOLD_MS = 2500L

@Composable
private fun LensRow(label: String, earlier: Boolean, later: String, before: String) {
    val colors = CivilightTheme.colors
    val type = CivilightTheme.type
    Row(Modifier.fillMaxWidth().topHairline(colors.line).padding(vertical = 11.dp)) {
        CivilightText(label, type.command, Modifier.weight(1f), color = colors.ink3)
        Swap(earlier, later, before) { CivilightText(it, type.command, color = colors.ink, textAlign = TextAlign.End) }
    }
}

/** The value swap of `mValA` / `mValB` is the shared one (CNV-42): the old text leaves up 8 dp, the new one comes from below. */
@Composable
private fun Swap(earlier: Boolean, later: String, before: String, content: @Composable (String) -> Unit) {
    val text = if (earlier) before else later
    val motion = CivilightTheme.motion
    AnimatedContent(
        targetState = text,
        transitionSpec = { motion.valueSwap() },
        label = "lensValue",
    ) { content(it) }
}

/** `Walk3`: the recorder glow, `듣는 중`, the spoken line with the caret, the mic button. */
@Composable
private fun BoxScope.VoiceDemo() {
    val colors = CivilightTheme.colors
    val type = CivilightTheme.type
    val motion = CivilightTheme.motion
    RecorderGlowBand(if (motion.loops) GlowMode.RECORDING else GlowMode.STILL, Modifier.align(Alignment.BottomCenter).fillMaxWidth(), height = 260.dp)
    Column(Modifier.align(Alignment.TopStart).fillMaxWidth().padding(start = 22.dp, end = 22.dp, top = 60.dp)) {
        CivilightText(OnboardingCopy.LISTENING, type.sec, color = colors.ink3)
        Row(Modifier.padding(top = 10.dp), verticalAlignment = Alignment.Bottom) {
            CivilightText(OnboardingSample.SPOKEN, type.recorderLive, Modifier.weight(1f, fill = false), color = colors.ink)
            // The caret blinks as the script's does (linear alpha, `caretPeriodMs`); solid without loops.
            val blink = if (motion.loops) {
                val caret by rememberInfiniteTransition(label = "caret").animateFloat(1f, 0f, infiniteRepeatable(tween(CivilightMotion.caretPeriodMs, easing = LinearEasing), RepeatMode.Reverse), label = "caretAlpha")
                caret
            } else {
                1f
            }
            Box(Modifier.padding(start = 2.dp, bottom = 6.dp).size(2.dp, 20.dp).graphicsLayer { alpha = blink }.background(colors.acc))
        }
    }
    Box(
        Modifier.align(Alignment.BottomCenter).offset(y = (-24).dp).size(56.dp).clip(RoundedCornerShape(18.dp)).background(colors.pri),
        contentAlignment = Alignment.Center,
    ) {
        CivilightIcon(IconId.Mic, contentDescription = null, size = 22.dp, tint = colors.onpri)
    }
}
