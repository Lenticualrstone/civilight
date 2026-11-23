package io.github.lenticualrstone.civilight.feature.library

import androidx.compose.ui.graphics.Color
import io.github.lenticualrstone.civilight.designsystem.theme.CivilightColors
import io.github.lenticualrstone.civilight.feature.library.newproject.NewProjectForm
import io.github.lenticualrstone.civilight.feature.library.newproject.NewProjectRules
import io.github.lenticualrstone.civilight.feature.library.rating.ratingCellPaint
import io.github.lenticualrstone.civilight.feature.library.rating.ratingCellStyle
import io.github.lenticualrstone.civilight.model.project.Rating
import io.github.lenticualrstone.civilight.model.project.StructurePreset
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

/** Motion round 1 (emulator): the rating cells cross over, and the step-2 `미리 보기` swaps instead of cutting. */
class NewProjectMotionTest {
    private val themes = listOf(CivilightColors.light, CivilightColors.dark)

    private fun Color.opaque() = copy(alpha = 1f)

    @Test
    fun `rating cell paint is never null and matches the board style when drawn`() {
        themes.forEach { c ->
            Rating.entries.forEach { r ->
                listOf(true, false).forEach { sel ->
                    val style = ratingCellStyle(r, sel, c)
                    val paint = ratingCellPaint(r, sel, c)
                    assertEquals(style.text, paint.text)
                    style.fill?.let { assertEquals(it, paint.fill) } ?: assertEquals(0f, paint.fill.alpha)
                    style.outline?.let { assertEquals(it, paint.outline) } ?: assertEquals(0f, paint.outline.alpha)
                }
            }
        }
    }

    @Test
    fun `a hidden rating fill or outline fades from its own hue, not from transparent black`() {
        themes.forEach { c ->
            Rating.entries.forEach { r ->
                val on = ratingCellPaint(r, true, c)
                val off = ratingCellPaint(r, false, c)
                // Unselected fill = the selected fill at alpha 0; selected outline = the unselected outline at alpha 0.
                assertEquals(on.fill.opaque(), off.fill.opaque())
                assertEquals(off.outline.opaque(), on.outline.opaque())
                // The selection really changes what is drawn, so the colour animation has something to move.
                assertNotEquals(on.fill, off.fill)
                assertNotEquals(on.text, off.text)
            }
        }
    }

    @Test
    fun `another structure swaps the preview`() {
        val chapter = NewProjectForm()
        val act = NewProjectRules.withPreset(chapter, StructurePreset.ACT_SCENE)
        val custom = NewProjectRules.withPreset(chapter, StructurePreset.CUSTOM)
        assertNotEquals(NewProjectRules.previewSwapKey(chapter), NewProjectRules.previewSwapKey(act))
        // Same default levels as 장 › 에피소드 › 씬, but another pick: still a swap.
        assertNotEquals(NewProjectRules.previewSwapKey(chapter), NewProjectRules.previewSwapKey(custom))
    }

    @Test
    fun `renaming a custom level updates in place, adding or removing one swaps`() {
        val custom = NewProjectRules.withPreset(NewProjectForm(), StructurePreset.CUSTOM)
        val renamed = NewProjectRules.setLevel(custom, 0, "부")
        assertNotEquals(NewProjectRules.preview(custom), NewProjectRules.preview(renamed))
        assertEquals(NewProjectRules.previewSwapKey(custom), NewProjectRules.previewSwapKey(renamed))

        val removed = NewProjectRules.removeLevel(custom, 2)
        assertNotEquals(NewProjectRules.previewSwapKey(custom), NewProjectRules.previewSwapKey(removed))

        val added = NewProjectRules.setLevel(NewProjectRules.addLevel(custom), 3, "컷")
        assertNotEquals(NewProjectRules.previewSwapKey(custom), NewProjectRules.previewSwapKey(added))
    }

    @Test
    fun `the default swap key follows the tree shape`() {
        val lines = listOf(0 to "장", 1 to "에피소드")
        assertEquals(NewProjectRules.previewSwapKey(null, lines), NewProjectRules.previewSwapKey(null, listOf(0 to "막", 1 to "장면")))
        assertNotEquals(NewProjectRules.previewSwapKey(null, lines), NewProjectRules.previewSwapKey(null, lines.take(1)))
    }
}
