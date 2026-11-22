package io.github.lenticualrstone.civilight.designsystem

import androidx.compose.animation.core.SnapSpec
import androidx.compose.animation.core.TweenSpec
import androidx.compose.ui.unit.dp
import io.github.lenticualrstone.civilight.designsystem.card.CardKind
import io.github.lenticualrstone.civilight.designsystem.card.CardMetrics
import io.github.lenticualrstone.civilight.designsystem.card.CardParallax
import io.github.lenticualrstone.civilight.designsystem.card.CardSnap
import io.github.lenticualrstone.civilight.designsystem.card.CardViewMotion
import io.github.lenticualrstone.civilight.designsystem.card.SnapTicker
import io.github.lenticualrstone.civilight.designsystem.component.GlowAmounts
import io.github.lenticualrstone.civilight.designsystem.component.GlowMode
import io.github.lenticualrstone.civilight.designsystem.component.glowAmounts
import io.github.lenticualrstone.civilight.designsystem.component.glowSweep
import io.github.lenticualrstone.civilight.designsystem.component.sheetOpenAmount
import io.github.lenticualrstone.civilight.designsystem.component.vertexPopScale
import io.github.lenticualrstone.civilight.designsystem.text.BoxReveal
import io.github.lenticualrstone.civilight.designsystem.text.searchBoxReveal
import io.github.lenticualrstone.civilight.designsystem.text.searchBoxRevealTotalMs
import io.github.lenticualrstone.civilight.designsystem.theme.Bezier
import io.github.lenticualrstone.civilight.designsystem.theme.CivilightMotion
import io.github.lenticualrstone.civilight.designsystem.theme.Motion
import io.github.lenticualrstone.civilight.designsystem.theme.MotionLint
import io.github.lenticualrstone.civilight.designsystem.theme.MotionRule
import io.github.lenticualrstone.civilight.designsystem.theme.MotionRules
import io.github.lenticualrstone.civilight.designsystem.theme.UnhideLook
import io.github.lenticualrstone.civilight.designsystem.theme.UnhideMotion
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/** Motion helpers (motion audit §3.1 / §3.2), D1–D3 tokens, MotionLint, promoted card rules. */
class MotionHelpersTest {

    @Test
    fun `reduce rule - setting or animator scale 0`() {
        assertFalse(MotionRules.effectiveReduced(setting = false, animatorScale = 1f))
        assertTrue(MotionRules.effectiveReduced(setting = true, animatorScale = 1f))
        assertTrue("animations off in developer options", MotionRules.effectiveReduced(setting = false, animatorScale = 0f))
        assertFalse(MotionRules.effectiveReduced(setting = false, animatorScale = 0.5f))
    }

    @Test
    fun `decorative loops stop when reduced or the animator scale is 0`() {
        assertTrue(MotionRules.loopsRun(reduced = false, animatorScale = 1f))
        assertFalse(MotionRules.loopsRun(reduced = true, animatorScale = 1f))
        assertFalse(MotionRules.loopsRun(reduced = false, animatorScale = 0f))
        assertTrue(Motion(reduced = false).loops)
        assertFalse(Motion(reduced = true).loops)
    }

    @Test
    fun `hand-rolled waits follow the animator scale`() {
        assertEquals(320L, MotionRules.scaledMs(320, 1f))
        assertEquals(640L, MotionRules.scaledMs(320, 2f))
        assertEquals(160L, MotionRules.scaledMs(320, 0.5f))
        assertEquals(0L, MotionRules.scaledMs(320, 0f))
        assertEquals(0L, MotionRules.scaledMs(-5, 1f))
        assertEquals(80L, Motion(reduced = false, animatorScale = 0.5f).delayMs(160))
    }

    @Test
    fun `spec - token in full motion, 120 ms linear when reduced, delay kept`() {
        val full = Motion(reduced = false).spec<Float>(CivilightMotion.enter) as TweenSpec<Float>
        assertEquals(320, full.durationMillis)
        assertEquals(0, full.delay)
        val delayed = Motion(reduced = false).spec<Float>(CivilightMotion.standard, delayMs = 160) as TweenSpec<Float>
        assertEquals(200, delayed.durationMillis)
        assertEquals(160, delayed.delay)
        val reduced = Motion(reduced = true).spec<Float>(CivilightMotion.enter) as TweenSpec<Float>
        assertEquals(CivilightMotion.reducedFadeMs, reduced.durationMillis)
    }

    @Test
    fun `move snaps when reduced, placement is null when reduced`() {
        assertTrue(Motion(reduced = true).move<Float>(CivilightMotion.standard) is SnapSpec<Float>)
        val full = Motion(reduced = false).move<Float>(CivilightMotion.foldOpen) as TweenSpec<Float>
        assertEquals(360, full.durationMillis)
        assertNull(Motion(reduced = true).placement())
        assertTrue(Motion(reduced = false).placement() != null)
    }

    @Test
    fun `D1 to D3 tokens`() {
        assertEquals(400, CivilightMotion.mapReroll.durationMs)
        assertEquals(250, CivilightMotion.rowIn.durationMs)
        assertEquals(1200, CivilightMotion.mapGenerate.durationMs)
        assertEquals(240, CivilightMotion.stampDrop.durationMs)
        listOf(CivilightMotion.mapReroll, CivilightMotion.rowIn, CivilightMotion.mapGenerate, CivilightMotion.stampDrop)
            .forEach { assertEquals(CivilightMotion.standardCurve, it.curve) }
        assertEquals(0.08f, CivilightMotion.cardParallax, 0f)
        assertEquals(1.1f, CivilightMotion.cardImageScale, 0f)
        assertEquals(2400, CivilightMotion.recorderGlowSweep.durationMs)
        assertEquals(Bezier(0.42f, 0f, 0.58f, 1f), CivilightMotion.recorderGlowSweep.curve)
        assertEquals(1600, CivilightMotion.recorderLevelPeriodMs)
        assertEquals(7.dp, CivilightMotion.unhideBlur)
    }

    @Test
    fun `unhide looks and values (CNV-43)`() {
        assertEquals(UnhideLook.BLUR, UnhideMotion.look(reduced = false, sdk = 34))
        assertEquals(UnhideLook.FADE, UnhideMotion.look(reduced = false, sdk = 29))
        assertEquals(UnhideLook.QUICK_FADE, UnhideMotion.look(reduced = true, sdk = 34))
        assertEquals(320, UnhideMotion.durationMs(UnhideLook.BLUR))
        assertEquals(120, UnhideMotion.durationMs(UnhideLook.QUICK_FADE))
        assertEquals(7f, UnhideMotion.blurDp(UnhideLook.BLUR, 0f), 0f)
        assertEquals(0f, UnhideMotion.blurDp(UnhideLook.BLUR, 1f), 0f)
        assertEquals(0f, UnhideMotion.blurDp(UnhideLook.FADE, 0f), 0f)
        assertEquals(0.55f, UnhideMotion.alpha(UnhideLook.BLUR, 0f), 0.0001f)
        assertEquals(1f, UnhideMotion.alpha(UnhideLook.BLUR, 1f), 0f)
        assertEquals(0f, UnhideMotion.alpha(UnhideLook.QUICK_FADE, 0f), 0f)
        assertEquals(0.5f, UnhideMotion.alpha(UnhideLook.FADE, 0.5f), 0f)
    }

    @Test
    fun `card rules copied from canvas`() {
        val pitch = CardMetrics.pitch(CardKind.PROJECT, inner = false)
        assertEquals(236f, pitch)
        assertEquals(252f, CardMetrics.pitch(CardKind.PROJECT, inner = true))
        assertEquals(236f, CardSnap.target(300f, pitch))
        assertEquals("never past the last card", 236f, CardSnap.target(900f, pitch, count = 2))
        assertEquals(CardSnap.DECELERATE_MS, CardSnap.MOTION.durationMs)
        val ticker = SnapTicker()
        assertEquals("one tick per snap", 2, listOf(true, true, false, false, true, false).count { ticker.onScrolling(it) })
        assertEquals(18.88f, CardParallax.imageShift(-236f, reduced = false), 0.001f)
        assertEquals(0f, CardParallax.imageShift(-236f, reduced = true), 0f)
        assertEquals(1.1f, CardParallax.imageScale(reduced = false), 0f)
        assertEquals(1f, CardParallax.imageScale(reduced = true), 0f)
        assertNull(CardViewMotion.plan(reduced = true).growMs)
        assertEquals(360, CardViewMotion.plan(reduced = false).growMs)
        assertEquals(160, CardViewMotion.plan(reduced = false).textInDelayMs)
    }

    @Test
    fun `MotionLint flags literals, default specs and curves`() {
        val src = """
            val a = tween(320)
            val b = tween(durationMillis = 400, easing = LinearEasing)
            val c = keyframes { durationMillis = 900 }
            val d = tween<Float>(CivilightMotion.enter.durationMs)
            Crossfade(bitmap) { }
            Crossfade(bitmap, animationSpec = motion.spec()) { }
            Modifier.animateItem()
            val f = rememberSnapFlingBehavior(state)
            val e = CubicBezierEasing(0.2f, 0f, 0f, 1f)
            val g = tween(0)
            val h = tween<Float>(320, easing = LinearEasing)
        """.trimIndent()
        val issues = MotionLint.lint("X.kt", src)
        assertEquals(
            listOf(
                1 to MotionRule.TWEEN_LITERAL,
                2 to MotionRule.TWEEN_LITERAL,
                3 to MotionRule.DURATION_LITERAL,
                5 to MotionRule.CROSSFADE_WITHOUT_SPEC,
                7 to MotionRule.ANIMATE_ITEM_DEFAULT,
                8 to MotionRule.DEFAULT_SNAP_FLING,
                9 to MotionRule.CUBIC_BEZIER_OUTSIDE_DS,
                11 to MotionRule.TWEEN_LITERAL,
            ),
            issues.map { it.line to it.rule },
        )
        assertTrue(MotionLint.lint("X.kt", "val e = CubicBezierEasing(.1f, 0f, 0f, 1f)", allowEasingDefinitions = true).isEmpty())
    }

    @Test(expected = IllegalStateException::class)
    fun `MotionLint - a module without sources fails loudly`() {
        MotionLint.lintModule(File("does-not-exist"))
    }

    @Test
    fun `MotionLint ignores comments and strings, honours the allow marker`() {
        val src = """
            /** Fades in 320 ms: tween(320) in the board. */
            // Crossfade(x) { } would be wrong
            val s = "tween(320)"
            val ok = tween(160) // motion-lint: allow (spec value outside the tokens)
            // motion-lint: allow
            val ok2 = tween(90)
        """.trimIndent()
        assertEquals(MotionLint.report(MotionLint.lint("Y.kt", src)), 0, MotionLint.lint("Y.kt", src).size)
    }

    @Test
    fun `S12 search boxes grow from the left 60 ms apart, fade when reduced`() {
        assertEquals(BoxReveal(0f, 1f), searchBoxReveal(0, 0f, reduced = false))
        assertEquals(1f, searchBoxReveal(0, 200f, reduced = false).widthFraction, 0f)
        val mid = searchBoxReveal(1, 160f, reduced = false)
        assertTrue("box 1 starts at 60 ms and is partly drawn at 160", mid.widthFraction in 0.01f..0.99f)
        assertEquals(0f, searchBoxReveal(2, 100f, reduced = false).widthFraction, 0f)
        assertEquals(BoxReveal(1f, 0.5f), searchBoxReveal(0, 60f, reduced = true))
        assertEquals(320, searchBoxRevealTotalMs(3, reduced = false))
        assertEquals(240, searchBoxRevealTotalMs(3, reduced = true))
    }

    @Test
    fun `S13 glow - no level or sweep when still or loops off, sweep alternates`() {
        assertEquals(GlowAmounts(1f, 0f), glowAmounts(GlowMode.RECORDING, loops = true))
        assertEquals(GlowAmounts(0f, 1f), glowAmounts(GlowMode.PROCESSING, loops = true))
        assertEquals(GlowAmounts(0f, 0f), glowAmounts(GlowMode.STILL, loops = true))
        assertEquals(GlowAmounts(0f, 0f), glowAmounts(GlowMode.RECORDING, loops = false))
        assertEquals(-1f, glowSweep(0f), 1e-4f)
        assertEquals(1f, glowSweep(2.4f), 1e-4f)
        assertEquals(-1f, glowSweep(4.8f), 1e-4f)
    }

    @Test
    fun `S5 scrim and back scale follow the drag`() {
        assertEquals(1f, sheetOpenAmount(1f, 0f, 400f), 0f)
        assertEquals(0.5f, sheetOpenAmount(1f, 200f, 400f), 1e-4f)
        assertEquals(0.25f, sheetOpenAmount(0.5f, 200f, 400f), 1e-4f)
        assertEquals("not measured yet", 1f, sheetOpenAmount(1f, 50f, 0f), 0f)
    }

    @Test
    fun `S18g vertex pops past 1 and settles`() {
        assertEquals(0.6f, vertexPopScale(0f), 1e-4f)
        assertEquals(1.3f, vertexPopScale(0.4f), 1e-4f)
        assertEquals(1f, vertexPopScale(1f), 1e-4f)
    }

    @Test
    fun `MotionLint - the design system itself is clean`() {
        assertTrue("the scan reads the DS sources", MotionLint.sourceFiles(File("src/main")).size > 50)
        val issues = MotionLint.lintModule(File("."), allowEasingDefinitions = true)
        assertEquals(MotionLint.report(issues), 0, issues.size)
    }
}
