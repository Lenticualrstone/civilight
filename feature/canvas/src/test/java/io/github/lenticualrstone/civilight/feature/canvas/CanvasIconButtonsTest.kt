package io.github.lenticualrstone.civilight.feature.canvas

import io.github.lenticualrstone.civilight.designsystem.icon.IconButtonLint
import io.github.lenticualrstone.civilight.designsystem.icon.IconButtonSpec
import io.github.lenticualrstone.civilight.designsystem.icon.IconClass
import io.github.lenticualrstone.civilight.feature.canvas.gallery.GalleryUiCopy
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/** ICON (DS-70) in this module: every icon-only button has a name, and the gallery rows follow the list. */
class CanvasIconButtonsTest {
    @Test
    fun `every icon-only button in feature canvas has a non-blank accessibility name`() {
        val root = listOf(File("src/main"), File("feature/canvas/src/main")).first { it.exists() }
        assertEquals("", IconButtonLint.report(IconButtonLint.lintDir(root)))
    }

    @Test
    fun `gallery rows of the icon list`() {
        // a · icon only, with the old text as the name.
        for (spec in listOf(IconButtonSpec.VIEWER_PREV, IconButtonSpec.VIEWER_NEXT, IconButtonSpec.CAPTION_EDIT, IconButtonSpec.CARD_VIEW, IconButtonSpec.CARD_CAPTION_EDIT, IconButtonSpec.CLOSE)) {
            assertEquals(IconClass.A, spec.cls)
            assertNotNull(spec.icon)
            assertTrue(spec.accessibilityName.isNotBlank())
        }
        assertEquals(GalleryUiCopy.BTN_PREV, IconButtonSpec.VIEWER_PREV.accessibilityName)
        assertEquals(GalleryUiCopy.BTN_NEXT, IconButtonSpec.VIEWER_NEXT.accessibilityName)
        assertEquals(GalleryUiCopy.BTN_EDIT, IconButtonSpec.CARD_CAPTION_EDIT.accessibilityName)
        assertEquals(GalleryUiCopy.BTN_VIEW, IconButtonSpec.CARD_VIEW.accessibilityName)
        assertEquals(GalleryUiCopy.BTN_CLOSE, IconButtonSpec.CLOSE.accessibilityName)
        // b · icon + text; a shortened label still reads the full old text.
        assertEquals(IconClass.B, IconButtonSpec.CAPTION_TOGGLE.cls)
        assertTrue(IconButtonSpec.CAPTION_TOGGLE.toggle)
        assertEquals(GalleryUiCopy.BTN_TEXT, IconButtonSpec.CAPTION_TOGGLE.accessibilityName)
        assertEquals(GalleryUiCopy.BTN_SELECT_MANY, IconButtonSpec.MULTI_SELECT.accessibilityName)
        assertEquals(GalleryUiCopy.BTN_RELOCK, IconButtonSpec.RELOCK.accessibilityName)
        assertEquals(GalleryUiCopy.BTN_FIRST, IconButtonSpec.TO_TOP.text)
        assertEquals(GalleryUiCopy.BTN_LAST, IconButtonSpec.TO_BOTTOM.text)
        for (spec in listOf(IconButtonSpec.CAPTION_TOGGLE, IconButtonSpec.MULTI_SELECT, IconButtonSpec.RELOCK, IconButtonSpec.TO_TOP, IconButtonSpec.TO_BOTTOM, IconButtonSpec.SHOW_WITH_FINGERPRINT)) {
            assertEquals(IconClass.B, spec.cls)
            assertNotNull(spec.icon)
        }
        // Zoom and back names (icon list, "표에 넣지 않은 것").
        assertEquals("확대", CanvasCopy.ZOOM_IN)
        assertEquals("축소", CanvasCopy.ZOOM_OUT)
        assertEquals("뒤로", CanvasCopy.BACK)
        assertEquals("메뉴", CanvasCopy.ROW_MENU)
    }
}
