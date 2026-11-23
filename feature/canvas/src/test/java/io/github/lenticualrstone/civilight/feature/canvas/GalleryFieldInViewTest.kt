package io.github.lenticualrstone.civilight.feature.canvas

import io.github.lenticualrstone.civilight.feature.canvas.gallery.FieldInView
import io.github.lenticualrstone.civilight.feature.canvas.gallery.FieldInView.State
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** `이미지 정보` fields keep their label in view above the keyboard (inner screen: `제목` label cut at the top). */
class GalleryFieldInViewTest {
    @Test
    fun gainingFocusBringsTheBlockIn() {
        assertTrue(FieldInView.bring(State(focused = false, keyboard = 0), State(focused = true, keyboard = 0)))
        // Focus moving in while the keyboard is already up (설명 → 제목).
        assertTrue(FieldInView.bring(State(focused = false, keyboard = 900), State(focused = true, keyboard = 900)))
    }

    @Test
    fun aRisingKeyboardBringsTheFocusedBlockInAgain() {
        assertTrue(FieldInView.bring(State(true, 0), State(true, 120)))
        assertTrue(FieldInView.bring(State(true, 120), State(true, 900)))
    }

    @Test
    fun nothingWhenUnfocusedSettledOrTheKeyboardGoes() {
        // Another field's keyboard: this block does not move the sheet.
        assertFalse(FieldInView.bring(State(false, 0), State(false, 900)))
        // Same frame again, keyboard settled.
        assertFalse(FieldInView.bring(State(true, 900), State(true, 900)))
        // Keyboard going down: the sheet only gets taller.
        assertFalse(FieldInView.bring(State(true, 900), State(true, 300)))
        // Focus leaving.
        assertFalse(FieldInView.bring(State(true, 900), State(false, 900)))
    }

    @Test
    fun theFieldAboveComesInFirstAndTheFocusedBlockLast() {
        // 설명 쓰기: 제목 is brought in first, then 설명 (which has the last word when both do not fit).
        assertEquals(listOf("제목", "설명"), FieldInView.order(focused = "설명", above = "제목"))
        // 제목 (and other single blocks): only itself.
        assertEquals(listOf("제목"), FieldInView.order(focused = "제목", above = null))
    }

    @Test
    fun titleComesFirstOnlyWhenBothBlocksFit() {
        // 제목 70 + gap 16 + 설명 180 = 266 px.
        assertTrue(FieldInView.fits(viewport = 400, above = 70, gap = 16, focused = 180))
        assertTrue(FieldInView.fits(viewport = 266, above = 70, gap = 16, focused = 180))
        // 1 px short (a long 설명, or a low sheet): only 설명, as before.
        assertFalse(FieldInView.fits(viewport = 265, above = 70, gap = 16, focused = 180))
        // Not measured yet.
        assertFalse(FieldInView.fits(viewport = 0, above = 70, gap = 16, focused = 180))
        assertFalse(FieldInView.fits(viewport = 400, above = 0, gap = 16, focused = 180))
        assertFalse(FieldInView.fits(viewport = 400, above = 70, gap = 16, focused = 0))
    }
}
