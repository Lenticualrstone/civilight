package io.github.lenticualrstone.civilight.designsystem

import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.lenticualrstone.civilight.designsystem.component.BadgeSurface
import io.github.lenticualrstone.civilight.designsystem.component.ButtonVariant
import io.github.lenticualrstone.civilight.designsystem.component.LensChipState
import io.github.lenticualrstone.civilight.designsystem.component.Rating
import io.github.lenticualrstone.civilight.designsystem.component.TabSets
import io.github.lenticualrstone.civilight.designsystem.component.avatarGlyph
import io.github.lenticualrstone.civilight.designsystem.component.avatarGlyphSp
import io.github.lenticualrstone.civilight.designsystem.component.badgeStyle
import io.github.lenticualrstone.civilight.designsystem.component.buttonStyle
import io.github.lenticualrstone.civilight.designsystem.component.checkboxStyle
import io.github.lenticualrstone.civilight.designsystem.component.confirmButtonVariant
import io.github.lenticualrstone.civilight.designsystem.component.hasAtMostOnePrimary
import io.github.lenticualrstone.civilight.designsystem.component.lensChipStyle
import io.github.lenticualrstone.civilight.designsystem.component.radioStyle
import io.github.lenticualrstone.civilight.designsystem.component.segmentStyle
import io.github.lenticualrstone.civilight.designsystem.component.selectionStyle
import io.github.lenticualrstone.civilight.designsystem.component.switchStyle
import io.github.lenticualrstone.civilight.designsystem.component.tabStyle
import io.github.lenticualrstone.civilight.designsystem.component.toastStyle
import io.github.lenticualrstone.civilight.designsystem.component.travelBarFraction
import io.github.lenticualrstone.civilight.designsystem.foundation.HitSize
import io.github.lenticualrstone.civilight.designsystem.foundation.HitTarget
import io.github.lenticualrstone.civilight.designsystem.foundation.hitArea
import io.github.lenticualrstone.civilight.designsystem.glass.GlassMask
import io.github.lenticualrstone.civilight.designsystem.glass.GlassMode
import io.github.lenticualrstone.civilight.designsystem.glass.GlassSurface
import io.github.lenticualrstone.civilight.designsystem.glass.glassStyle
import io.github.lenticualrstone.civilight.designsystem.icon.IconId
import io.github.lenticualrstone.civilight.designsystem.icon.iconStrokeDp
import io.github.lenticualrstone.civilight.designsystem.theme.CivilightColors
import io.github.lenticualrstone.civilight.designsystem.theme.CivilightType
import io.github.lenticualrstone.civilight.designsystem.theme.FixedColors
import io.github.lenticualrstone.civilight.designsystem.theme.TextScale
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ComponentStylesTest {

    private val themes = listOf(CivilightColors.dark, CivilightColors.light)

    @Test
    fun `T-DS-15 rating badge`() {
        themes.forEach { c ->
            val all = badgeStyle(Rating.ALL, BadgeSurface.PLAIN, c)
            assertEquals(c.ink3, all.outline)
            assertEquals(c.ink2, all.text)
            val r15 = badgeStyle(Rating.R15, BadgeSurface.PLAIN, c)
            assertEquals(c.ink3, r15.outline)
            assertEquals(c.ink2, r15.text)
            val r18 = badgeStyle(Rating.R18, BadgeSurface.PLAIN, c)
            assertEquals(c.r18, r18.outline)
            assertEquals(c.r18, r18.text)
            assertEquals(FixedColors.onImageBadge, badgeStyle(Rating.R15, BadgeSurface.ON_IMAGE, c).outline)
            assertEquals(FixedColors.onImageBadge, badgeStyle(Rating.R15, BadgeSurface.ON_IMAGE, c).text)
            assertEquals(FixedColors.onImageR18, badgeStyle(Rating.R18, BadgeSurface.ON_IMAGE, c).outline)
            assertEquals(FixedColors.onImageR18, badgeStyle(Rating.R18, BadgeSurface.ON_IMAGE, c).text)
        }
        assertEquals(listOf("ALL", "R-15", "R-18"), Rating.entries.map { it.label })
    }

    @Test
    fun `T-DS-16 lens chip`() {
        themes.forEach { c ->
            val latest = lensChipStyle(LensChipState.LATEST, c)
            assertEquals(c.line, latest.outline)
            assertEquals(c.ink2, latest.text)
            val past = lensChipStyle(LensChipState.PAST, c)
            assertEquals(c.acc, past.outline)
            assertEquals(c.acc, past.text)
            val script = lensChipStyle(LensChipState.SCRIPT_POSITION, c)
            assertNull(script.outline)
            assertEquals(0.dp, script.endPadding)
            assertEquals(c.ink2, script.text)
        }
    }

    @Test
    fun `T-DS-17 hit areas`() {
        assertEquals(HitSize(60f, 44f), hitArea(HitTarget.CHIP, 52f, 30f))
        assertEquals(HitSize(44f, 44f), hitArea(HitTarget.CHIP, 30f, 30f))
        assertEquals(HitSize(93f, 44f), hitArea(HitTarget.LENS_CHIP, 85f, 32f))
        assertEquals(HitSize(54f, 44f), hitArea(HitTarget.SWITCH, 46f, 28f))
        assertEquals(HitSize(44f, 44f), hitArea(HitTarget.SMALL, 32f, 32f))
        assertEquals(HitSize(44f, 44f), hitArea(HitTarget.RADIO, 22f, 22f))
        assertEquals(HitSize(44f, 44f), hitArea(HitTarget.VERTEX, 14f, 14f))
        assertEquals(HitSize(44f, 44f), hitArea(HitTarget.LOCK_TOGGLE, 36f, 36f))
        assertEquals(HitSize(44f, 44f), hitArea(HitTarget.ICON_BUTTON, 44f, 44f))
    }

    @Test
    fun `T-DS-18 default text size`() {
        val t = CivilightType.create(TextScale.Default)
        assertEquals(18.sp, t.line.fontSize)
        assertEquals(31.sp, t.line.lineHeight)
        assertEquals(16.sp, t.body.fontSize)
        assertEquals(28.sp, t.body.lineHeight)
        assertEquals(14.sp, t.paren.fontSize)
        assertEquals(1.72f, t.line.lineHeight.value / t.line.fontSize.value, 0.005f)
        assertEquals(18f, TextScale.forStep(TextScale.DEFAULT_STEP).manuscriptBaseSp, 0f)
    }

    @Test
    fun `DS-07 scaling formulas`() {
        val s20 = TextScale(20f)
        assertEquals(34f, s20.line.lineHeightSp, 0f)
        assertEquals(18f, s20.body.sizeSp, 0f)
        assertEquals(31f, s20.body.lineHeightSp, 0f)
        assertEquals(16f, s20.paren.sizeSp, 0f)
        val s16 = TextScale(16f)
        assertEquals(28f, s16.line.lineHeightSp, 0f)
        assertEquals(14f, s16.body.sizeSp, 0f)
        assertEquals(25f, s16.body.lineHeightSp, 0f)
        val t = CivilightType.create(TextScale(22f))
        assertEquals(22.sp, t.line.fontSize)
        assertEquals(13.sp, t.label.fontSize)
        assertEquals("tnum", t.number.fontFeatureSettings)
        assertNull(t.line.fontFeatureSettings)
    }

    @Test
    fun `T-DS-21 travel bar`() {
        assertEquals(0.2436f, travelBarFraction(available = 760, required = 3120), 0.0001f)
        assertEquals(1f, travelBarFraction(available = 3120, required = 3120), 0f)
        assertEquals(1f, travelBarFraction(available = 5000, required = 3120), 0f)
    }

    @Test
    fun `T-DS-22 avatar glyph`() {
        assertEquals("리", avatarGlyph("리엔"))
        assertEquals(12, avatarGlyphSp(32))
        assertEquals(13, avatarGlyphSp(36))
        assertEquals(14, avatarGlyphSp(38))
        assertEquals("", avatarGlyph(""))
    }

    @Test
    fun `T-DS-23 icon stroke scales`() {
        assertEquals(1.6f, iconStrokeDp(24f), 1e-6f)
        assertEquals(1.3333334f, iconStrokeDp(20f), 1e-5f)
        assertEquals(1.0666667f, iconStrokeDp(16f), 1e-5f)
        assertEquals("M15 5l-7 7 7 7", IconId.Back.pathData)
    }

    @Test
    fun `T-DS-29 button variants`() {
        val confirm = confirmButtonVariant(hardToUndo = true)
        assertEquals(ButtonVariant.DANGER, confirm)
        themes.forEach { c ->
            assertEquals(c.r18, buttonStyle(ButtonVariant.DANGER, c).fill)
            assertEquals(c.onr18, buttonStyle(ButtonVariant.DANGER, c).content)
            assertEquals(c.tint, buttonStyle(ButtonVariant.GHOST, c).fill)
            assertEquals(c.ink, buttonStyle(ButtonVariant.GHOST, c).content)
            assertEquals(c.pri, buttonStyle(ButtonVariant.PRIMARY, c).fill)
            assertEquals(c.onpri, buttonStyle(ButtonVariant.PRIMARY, c).content)
            assertEquals(44.dp, buttonStyle(ButtonVariant.PRIMARY, c).height)
        }
        assertEquals(ButtonVariant.PRIMARY, confirmButtonVariant(hardToUndo = false))
        assertTrue(hasAtMostOnePrimary(listOf(ButtonVariant.GHOST, ButtonVariant.PRIMARY)))
        assertFalse(hasAtMostOnePrimary(listOf(ButtonVariant.PRIMARY, ButtonVariant.PRIMARY)))
    }

    @Test
    fun `T-DS-30 controls`() {
        themes.forEach { c ->
            val off = switchStyle(false, c)
            assertEquals(c.tint, off.track)
            assertEquals(c.line, off.trackOutline)
            assertEquals(3.dp, off.thumbX)
            assertEquals(c.ink3, off.thumb)
            val on = switchStyle(true, c)
            assertEquals(c.acc, on.track)
            assertNull(on.trackOutline)
            assertEquals(21.dp, on.thumbX)
            assertEquals(c.bg, on.thumb)
            val radioOn = radioStyle(true, c)
            assertEquals(c.acc, radioOn.ring)
            assertEquals(1.5.dp, radioOn.ringWidth)
            assertEquals(c.acc, radioOn.dot)
            assertEquals(c.ink3, radioStyle(false, c).ring)
            assertNull(radioStyle(false, c).dot)
            val checkOn = checkboxStyle(true, c)
            assertEquals(c.acc, checkOn.fill)
            assertEquals(c.bg, checkOn.tick)
            val checkOff = checkboxStyle(false, c)
            assertEquals(c.ink3, checkOff.ring)
            assertEquals(1.5.dp, checkOff.ringWidth)
            assertNull(checkOff.fill)
        }
    }

    @Test
    fun `T-DS-31 segmented and tabs`() {
        themes.forEach { c ->
            assertEquals(c.pri, segmentStyle(true, outlined = false, c).fill)
            assertEquals(c.onpri, segmentStyle(true, outlined = true, c).text)
            assertEquals(c.ink2, segmentStyle(false, outlined = false, c).text)
            assertEquals(c.ink3, segmentStyle(false, outlined = true, c).text)
            assertEquals(c.ink, tabStyle(true, c).text)
            assertEquals(c.acc, tabStyle(true, c).underline)
            assertEquals(c.ink3, tabStyle(false, c).text)
            assertNull(tabStyle(false, c).underline)
        }
        assertEquals(listOf("개요", "말투", "관계", "게임 텍스트", "비밀"), TabSets.characterRecord)
        assertEquals(listOf("시간", "참고", "속성", "모듈"), TabSets.scriptPanel)
    }

    @Test
    fun `T-DS-32 toast`() {
        themes.forEach { c ->
            val t = toastStyle(c)
            assertEquals(c.ink, t.fill)
            assertEquals(c.bg, t.message)
            assertEquals(c.bg, t.actionFill)
            assertEquals(c.ink, t.actionText)
            assertEquals(c.bg, buttonStyle(ButtonVariant.ON_TOAST, c).fill)
        }
    }

    @Test
    fun `T-DS-34 glass rendering by mode`() {
        themes.forEach { c ->
            val opaqueTop = glassStyle(GlassMode.OPAQUE, GlassSurface.TOP_BAR, c)
            assertEquals(c.raise, opaqueTop.fill)
            assertEquals(0.dp, opaqueTop.blur)
            assertNull(opaqueTop.mask)
            assertEquals(c.line, opaqueTop.bottomBorder)
            val opaqueBottom = glassStyle(GlassMode.OPAQUE, GlassSurface.BOTTOM_BAR, c)
            assertEquals(c.line, opaqueBottom.topBorder)
            assertNull(opaqueBottom.mask)
            val blurTop = glassStyle(GlassMode.BLUR, GlassSurface.TOP_BAR, c)
            assertEquals(c.glass, blurTop.fill)
            assertEquals(36.dp, blurTop.blur)
            assertEquals(1.4f, blurTop.saturation, 0f)
            assertEquals(GlassMask(0.72f, anchoredAtTop = true), blurTop.mask)
            assertNull(blurTop.bottomBorder)
            assertEquals(GlassMask(0.70f, anchoredAtTop = false), glassStyle(GlassMode.BLUR, GlassSurface.BOTTOM_BAR, c).mask)
            assertEquals(12.dp, glassStyle(GlassMode.BLUR, GlassSurface.CHIP, c).blur)
            assertEquals(0xEB, (glassStyle(GlassMode.SCRIM, GlassSurface.SHEET, c).fill.alpha * 255 + 0.5f).toInt())
        }
        assertEquals(GlassMode.SCRIM, GlassMode.resolve(30, false))
        assertEquals(GlassMode.BLUR_COSTLY, GlassMode.resolve(31, false))
        assertEquals(GlassMode.BLUR_COSTLY, GlassMode.resolve(32, false))
        assertEquals(GlassMode.BLUR, GlassMode.resolve(33, false))
        assertEquals(GlassMode.OPAQUE, GlassMode.resolve(35, true))
        assertEquals(GlassMode.OPAQUE, GlassMode.resolve(29, true))
    }

    @Test
    fun `T-DS-35 selection fill`() {
        themes.forEach { c ->
            val s = selectionStyle(c)
            assertEquals(c.sel, s.fill)
            assertEquals(c.acc, s.bar)
            assertEquals(2.dp, s.barWidth)
        }
    }
}
