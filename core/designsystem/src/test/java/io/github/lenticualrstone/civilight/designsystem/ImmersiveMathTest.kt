package io.github.lenticualrstone.civilight.designsystem

import io.github.lenticualrstone.civilight.designsystem.component.ImmersiveDetailCopy
import io.github.lenticualrstone.civilight.designsystem.component.ImmersiveFrame
import io.github.lenticualrstone.civilight.designsystem.component.ImmersiveMath
import io.github.lenticualrstone.civilight.designsystem.icon.IconButtonLint
import io.github.lenticualrstone.civilight.designsystem.text.LintIssue
import io.github.lenticualrstone.civilight.designsystem.text.lintString
import io.github.lenticualrstone.civilight.designsystem.theme.MotionLint
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/** `Immersive.dc.html` numbers on the cover board (475 × 751, status inset 28, bar present). */
class ImmersiveMathTest {
    private val h = 751f
    private val d = 375.5f
    private val eps = 1e-4f

    private fun scrollAt(p: Float) = p * d
    private fun frame(p: Float) = ImmersiveFrame.at(scrollAt(p), h)

    @Test
    fun titleHitBoxReaches44AndOnlyWhileResting() {
        assertEquals(0f, ImmersiveMath.hitPadDp(42f + 2f), eps)
        assertEquals(2f, ImmersiveMath.hitPadDp(40f), eps)
        assertEquals(0f, ImmersiveMath.hitPadDp(120f), eps)
        assertTrue(ImmersiveMath.titleTappable(hasClick = true, docked = false))
        assertFalse(ImmersiveMath.titleTappable(hasClick = true, docked = true))
        assertFalse(ImmersiveMath.titleTappable(hasClick = false, docked = false))
    }

    @Test
    fun progressIsScrollOverHalfTheViewport() {
        assertEquals(d, ImmersiveMath.distance(h), eps)
        assertEquals(0f, ImmersiveMath.progress(0f, h), eps)
        assertEquals(0.25f, ImmersiveMath.progress(scrollAt(0.25f), h), eps)
        assertEquals(1f, ImmersiveMath.progress(2000f, h), eps)
        assertEquals(0f, ImmersiveMath.progress(-50f, h), eps)
        assertEquals(0f, ImmersiveMath.progress(100f, 0f), eps)
    }

    @Test
    fun easeIsSmoothstep() {
        assertEquals(0f, ImmersiveMath.ease(0f), eps)
        assertEquals(0.15625f, ImmersiveMath.ease(0.25f), eps)
        assertEquals(0.5f, ImmersiveMath.ease(0.5f), eps)
        assertEquals(0.972f, ImmersiveMath.ease(0.9f), eps)
        assertEquals(1f, ImmersiveMath.ease(1f), eps)
    }

    @Test
    fun headerRampsOverP05To09() {
        assertEquals(0f, ImmersiveMath.headerAlpha(0f), eps)
        assertEquals(0f, ImmersiveMath.headerAlpha(0.25f), eps)
        assertEquals(0f, ImmersiveMath.headerAlpha(0.5f), eps)
        assertEquals(1f, ImmersiveMath.headerAlpha(0.9f), eps)
        assertEquals(1f, ImmersiveMath.headerAlpha(1f), eps)
        assertEquals(0.5f, ImmersiveMath.headerAlpha(0.7f), eps)
        assertEquals(84f, ImmersiveMath.headerHeight(28f), eps)
    }

    @Test
    fun titleMovesAndScalesCover() {
        val lh = ImmersiveMath.titleLineHeight(ImmersiveMath.titleStartSp(false))
        assertEquals(42f, lh, eps)
        val y0 = ImmersiveMath.titleStartY(h, lh)
        val y1 = ImmersiveMath.titleDockedY(28f, lh, wide = false)
        assertEquals(601f, y0, eps)
        assertEquals(45.5f, y1, eps)
        // p 0
        assertEquals(24f, ImmersiveMath.titleX(frame(0f).e, false), eps)
        assertEquals(601f, ImmersiveMath.titleY(frame(0f).e, y0, y1), eps)
        assertEquals(1f, ImmersiveMath.titleScale(frame(0f).e, false), eps)
        // p 0.25 (e 0.15625)
        assertEquals(24f + 40f * 0.15625f, ImmersiveMath.titleX(frame(0.25f).e, false), eps)
        assertEquals(601f + (45.5f - 601f) * 0.15625f, ImmersiveMath.titleY(frame(0.25f).e, y0, y1), 1e-3f)
        // p 0.5
        assertEquals(44f, ImmersiveMath.titleX(frame(0.5f).e, false), eps)
        assertEquals(1f + (17f / 34f - 1f) * 0.5f, ImmersiveMath.titleScale(frame(0.5f).e, false), eps)
        // p 0.9 (e 0.972)
        assertEquals(24f + 40f * 0.972f, ImmersiveMath.titleX(frame(0.9f).e, false), 1e-3f)
        // p 1
        assertEquals(64f, ImmersiveMath.titleX(frame(1f).e, false), eps)
        assertEquals(45.5f, ImmersiveMath.titleY(frame(1f).e, y0, y1), eps)
        assertEquals(0.5f, ImmersiveMath.titleScale(frame(1f).e, false), eps)
    }

    @Test
    fun titleWideClass() {
        assertFalse(ImmersiveMath.isWide(475f))
        assertTrue(ImmersiveMath.isWide(704f))
        assertEquals(32f, ImmersiveMath.gutterDp(true), eps)
        assertEquals(40f, ImmersiveMath.titleStartSp(true), eps)
        assertEquals(50f, ImmersiveMath.titleLineHeight(40f), eps)
        assertEquals(17f / 40f, ImmersiveMath.titleScale(1f, true), eps)
        assertEquals(32f, ImmersiveMath.titleX(0f, true), eps)
        assertEquals(64f, ImmersiveMath.titleX(1f, true), eps)
        // y1 = 28 + 28 − 50 × 0.425 / 2
        assertEquals(56f - 50f * 0.425f / 2f, ImmersiveMath.titleDockedY(28f, 50f, true), eps)
    }

    @Test
    fun titleStartFollowsInsetsBarAndSubtitle() {
        assertEquals(601f - 30f, ImmersiveMath.titleStartY(h, 42f, bottomInset = 30f), eps)
        assertEquals(601f + 64f, ImmersiveMath.titleStartY(h, 42f, hasBar = false), eps)
        assertEquals(601f + 24f, ImmersiveMath.titleStartY(h, 42f, hasSubtitle = false), eps)
    }

    @Test
    fun withoutBackTheDockedTitleStaysAtTheGutter() {
        assertEquals(64f, ImmersiveMath.titleDockedX(false), eps)
        assertEquals(24f, ImmersiveMath.titleDockedX(false, hasBack = false), eps)
        assertEquals(32f, ImmersiveMath.titleDockedX(true, hasBack = false), eps)
        for (e in listOf(0f, 0.3f, 1f)) {
            assertEquals(24f, ImmersiveMath.titleX(e, false, hasBack = false), eps)
            assertEquals(32f, ImmersiveMath.titleX(e, true, hasBack = false), eps)
        }
        // Default keeps the back-button path.
        assertEquals(ImmersiveMath.titleX(0.4f, false), ImmersiveMath.titleX(0.4f, false, hasBack = true), eps)
    }

    @Test
    fun bottomInsetOverrideIsNotAddedTwice() {
        assertEquals(30f, ImmersiveMath.effectiveBottomInset(30f, null), eps)
        assertEquals(0f, ImmersiveMath.effectiveBottomInset(30f, 0f), eps)
        assertEquals(12f, ImmersiveMath.effectiveBottomInset(30f, 12f), eps)
        assertEquals(0f, ImmersiveMath.effectiveBottomInset(30f, -5f), eps)
        // A pane already padded by the frame bar: the title block sits at the pane's bottom, no inset on top.
        val inset = ImmersiveMath.effectiveBottomInset(30f, 0f)
        assertEquals(601f + 64f, ImmersiveMath.titleStartY(h, 42f, bottomInset = inset, hasBar = false), eps)
        assertEquals(32f, ImmersiveMath.panelBottomPadding(inset, hasBar = false), eps)
    }

    @Test
    fun titleColourAndShadow() {
        assertEquals(0f, ImmersiveMath.titleInkMix(frame(0.5f).hp), eps)
        assertEquals(1f, ImmersiveMath.titleInkMix(frame(0.9f).hp), eps)
        assertEquals(0.38f, ImmersiveMath.titleShadowAlpha(0f), eps)
        assertEquals(0f, ImmersiveMath.titleShadowAlpha(1f), eps)
    }

    @Test
    fun subtitleFadesOverP0To032AndRises12() {
        assertEquals(1f, ImmersiveMath.subtitleAlpha(0f), eps)
        assertEquals(1f - 0.25f / 0.32f, ImmersiveMath.subtitleAlpha(0.25f), eps)
        assertEquals(0f, ImmersiveMath.subtitleAlpha(0.32f), eps)
        assertEquals(0f, ImmersiveMath.subtitleAlpha(0.5f), eps)
        assertEquals(0f, ImmersiveMath.subtitleShiftDp(0f), eps)
        assertEquals(-3f, ImmersiveMath.subtitleShiftDp(0.25f), eps)
        assertEquals(-12f, ImmersiveMath.subtitleShiftDp(1f), eps)
    }

    @Test
    fun buttonsCircleFadesWithHeader() {
        assertEquals(0.34f, ImmersiveMath.buttonCircleAlpha(frame(0f).hp), eps)
        assertEquals(0.34f, ImmersiveMath.buttonCircleAlpha(frame(0.5f).hp), eps)
        assertEquals(0f, ImmersiveMath.buttonCircleAlpha(frame(0.9f).hp), eps)
    }

    @Test
    fun heroScaleBlurToneShade() {
        val at = listOf(0f, 0.25f, 0.5f, 0.75f, 1f)
        val scale = listOf(1f, 1f + 0.08f * 0.15625f, 1.04f, 1f + 0.08f * 0.84375f, 1.08f)
        val blur = listOf(0f, 22f * 0.15625f, 11f, 22f * 0.84375f, 22f)
        val tone = listOf(0f, 0.3f * 0.15625f, 0.15f, 0.3f * 0.84375f, 0.30f)
        val shade = listOf(1f, 2f / 3f, 1f / 3f, 0f, 0f)
        at.forEachIndexed { i, p ->
            val f = frame(p)
            assertEquals("scale $p", scale[i], ImmersiveMath.heroScale(f.e), 1e-4f)
            assertEquals("blur $p", blur[i], ImmersiveMath.heroBlurDp(f.e, f.q), 1e-3f)
            assertEquals("tone $p", tone[i], ImmersiveMath.toneAlpha(f.e), 1e-4f)
            assertEquals("shade $p", shade[i], ImmersiveMath.shadeAlpha(f.p), 1e-4f)
        }
    }

    @Test
    fun heroBlurKeepsGoingTo48By16D() {
        val atD = ImmersiveFrame.at(d, h)
        assertEquals(0f, atD.q, eps)
        assertEquals(22f, ImmersiveMath.heroBlurDp(atD.e, atD.q), eps)
        val mid = ImmersiveFrame.at(1.3f * d, h)
        assertEquals(35f, ImmersiveMath.heroBlurDp(mid.e, mid.q), 1e-3f)
        val end = ImmersiveFrame.at(1.6f * d, h)
        assertEquals(1f, end.q, eps)
        assertEquals(48f, ImmersiveMath.heroBlurDp(end.e, end.q), eps)
        val past = ImmersiveFrame.at(3f * d, h)
        assertEquals(48f, ImmersiveMath.heroBlurDp(past.e, past.q), eps)
        // Shade fades completely to 0 so no dark smear stays behind the reading panel.
        assertEquals(0.30f, ImmersiveMath.toneAlpha(end.e), eps)
        assertEquals(0f, ImmersiveMath.shadeAlpha(end.p), eps)
    }

    @Test
    fun panelGlassStaysLegible() {
        assertEquals(0.56f, ImmersiveMath.panelGlassAlpha(frame(0f).e, dark = true), eps)
        assertEquals(0.62f, ImmersiveMath.panelGlassAlpha(frame(0f).e, dark = false), eps)
        assertEquals(0.60f, ImmersiveMath.panelGlassAlpha(frame(0.5f).e, dark = true), eps)
        assertEquals(0.66f, ImmersiveMath.panelGlassAlpha(frame(0.5f).e, dark = false), eps)
        assertEquals(0.64f, ImmersiveMath.panelGlassAlpha(frame(1f).e, dark = true), eps)
        assertEquals(0.70f, ImmersiveMath.panelGlassAlpha(frame(1f).e, dark = false), eps)
        assertEquals(96f + 20f, ImmersiveMath.panelBottomPadding(20f, hasBar = true), eps)
        assertEquals(88f + 20f, ImmersiveMath.barHeight(20f), eps)
    }

    @Test
    fun dockDistanceAndSpringDetentSnap() {
        val dock = ImmersiveMath.dockDistance(viewportHeight = 800f, statusInset = 28f, bottomInset = 24f, hasBar = true)
        assertEquals(800f - 24f - 64f - 84f, dock, eps)
        // Pull up past 22% commits to docked; small nudge falls back to hero.
        assertEquals(dock, ImmersiveMath.snapTarget(dock * 0.25f, dock, startScrollPx = 0f), eps)
        assertEquals(0f, ImmersiveMath.snapTarget(dock * 0.15f, dock, startScrollPx = 0f), eps)
        // Pull down past 78% returns to hero; flicks follow velocity.
        assertEquals(0f, ImmersiveMath.snapTarget(dock * 0.75f, dock, startScrollPx = dock), eps)
        assertEquals(dock, ImmersiveMath.snapTarget(dock * 0.85f, dock, startScrollPx = dock), eps)
        assertEquals(dock, ImmersiveMath.snapTarget(dock * 0.1f, dock, startScrollPx = 0f, velocityPxPerSec = 500f), eps)
        assertEquals(0f, ImmersiveMath.snapTarget(dock * 0.9f, dock, startScrollPx = dock, velocityPxPerSec = -500f), eps)
    }

    @Test
    fun noBlurFallbackIsStrongerToneNeverOpaque() {
        val f = ImmersiveFrame.at(1.6f * d, h)
        assertEquals(0f, ImmersiveMath.heroBlurDp(f.e, f.q, canBlur = false), eps)
        assertEquals(0.62f, ImmersiveMath.toneAlpha(f.e, canBlur = false), eps)
        assertTrue(ImmersiveMath.toneAlpha(f.e, canBlur = false) < 1f)
        assertEquals(0.64f, ImmersiveMath.panelGlassAlpha(f.e, dark = true, canBlur = false), eps)
    }

    @Test
    fun reducedMotionSnapsAtHalf() {
        val before = ImmersiveFrame.at(scrollAt(0.49f), h, reduced = true)
        assertEquals(0f, before.e, eps)
        assertEquals(0f, before.hp, eps)
        val after = ImmersiveFrame.at(scrollAt(0.5f), h, reduced = true)
        assertEquals(1f, after.e, eps)
        assertEquals(1f, after.hp, eps)
        assertEquals(64f, ImmersiveMath.titleX(after.e, false), eps)
        val far = ImmersiveFrame.at(1.6f * d, h, reduced = true)
        assertEquals(ImmersiveMath.HERO_BLUR_REDUCED_DP, ImmersiveMath.heroBlurDp(far.e, far.q, reduced = true), eps)
        assertEquals(ImmersiveMath.HERO_BLUR_REDUCED_DP, ImmersiveMath.heroBlurDp(0f, 0f, reduced = true), eps)
    }

    /** Every listed value at p 0, 0.25, 0.5, 0.9, 1 and at scroll 1.6 D (cover, dark / light). */
    @Test
    fun fullMatrixAtListedPoints() {
        val y0 = 601f
        val y1 = 45.5f
        val scrolls = listOf(0f, 0.25f * d, 0.5f * d, 0.9f * d, d, 1.6f * d)
        val es = listOf(0f, 0.15625f, 0.5f, 0.972f, 1f, 1f)
        val hps = listOf(0f, 0f, 0f, 1f, 1f, 1f)
        val ps = listOf(0f, 0.25f, 0.5f, 0.9f, 1f, 1f)
        val qs = listOf(0f, 0f, 0f, 0f, 0f, 1f)
        scrolls.forEachIndexed { i, s ->
            val f = ImmersiveFrame.at(s, h)
            val e = es[i]
            assertEquals("p@$i", ps[i], f.p, 1e-4f)
            assertEquals("e@$i", e, f.e, 1e-4f)
            assertEquals("hp@$i", hps[i], f.hp, 1e-4f)
            assertEquals("q@$i", qs[i], f.q, 1e-4f)
            assertEquals("x@$i", 24f + 40f * e, ImmersiveMath.titleX(f.e, false), 1e-3f)
            assertEquals("y@$i", y0 + (y1 - y0) * e, ImmersiveMath.titleY(f.e, y0, y1), 1e-3f)
            assertEquals("scale@$i", 1f - 0.5f * e, ImmersiveMath.titleScale(f.e, false), 1e-4f)
            assertEquals("sub@$i", (1f - ps[i] / 0.32f).coerceIn(0f, 1f), ImmersiveMath.subtitleAlpha(f.p), 1e-4f)
            assertEquals("subY@$i", -12f * ps[i], ImmersiveMath.subtitleShiftDp(f.p), 1e-4f)
            assertEquals("btn@$i", 0.34f * (1f - hps[i]), ImmersiveMath.buttonCircleAlpha(f.hp), 1e-4f)
            assertEquals("heroS@$i", 1f + 0.08f * e, ImmersiveMath.heroScale(f.e), 1e-4f)
            assertEquals("blur@$i", 22f * e + 26f * qs[i], ImmersiveMath.heroBlurDp(f.e, f.q), 1e-3f)
            assertEquals("tone@$i", 0.30f * e, ImmersiveMath.toneAlpha(f.e), 1e-4f)
            assertEquals("shade@$i", (1f - ps[i] / 0.75f).coerceIn(0f, 1f), ImmersiveMath.shadeAlpha(f.p), 1e-4f)
            assertEquals("dark@$i", 0.56f + 0.08f * e, ImmersiveMath.panelGlassAlpha(f.e, dark = true), 1e-4f)
            assertEquals("light@$i", 0.62f + 0.08f * e, ImmersiveMath.panelGlassAlpha(f.e, dark = false), 1e-4f)
        }
    }

    @Test
    fun copyIsTerseAndLintClean() {
        ImmersiveDetailCopy.all.forEach { assertEquals("'$it'", emptyList<LintIssue>(), lintString(it)) }
    }

    @Test
    fun sourcesPassMotionAndIconLints() {
        val dir = File("src/main/java/io/github/lenticualrstone/civilight/designsystem/component")
        listOf("ImmersiveDetail.kt", "ImmersiveMath.kt").forEach { name ->
            val src = File(dir, name).readText()
            assertEquals("", MotionLint.report(MotionLint.lint(name, src, allowEasingDefinitions = true)))
            assertEquals(emptyList<Any>(), IconButtonLint.lint(name, src))
        }
    }
}
