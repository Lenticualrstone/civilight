package io.github.lenticualrstone.civilight.feature.story

import io.github.lenticualrstone.civilight.feature.story.home.HomeModel
import io.github.lenticualrstone.civilight.model.asset.ImageOwnerKind
import io.github.lenticualrstone.civilight.model.asset.ImageSlot
import io.github.lenticualrstone.civilight.model.asset.ImageUse
import io.github.lenticualrstone.civilight.model.doc.ProjectDoc
import io.github.lenticualrstone.civilight.model.open
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** U-08 LIB-83: the cover on the 프로젝트 홈 head (60 §7.3 CV-30). */
class HomeCoverTest {
    private val doc = Fx.doc()

    private fun home(d: ProjectDoc = doc, readOnly: Boolean = false, fileExists: (String) -> Boolean = { true }) =
        HomeModel.build(d, readOnly, locked = true, today = Fx.today, zone = Fx.zone, fileExists = fileExists)

    private fun withCover(d: ProjectDoc, source: String?): ProjectDoc {
        val others = d.assets.imageUses.filterNot { it.slot.known == ImageSlot.PROJECT_COVER }
        val uses = if (source == null) others else others + ImageUse("cover", ImageOwnerKind.PROJECT.open(), d.key, ImageSlot.PROJECT_COVER.open(), source)
        return d.copy(assets = d.assets.copy(imageUses = uses))
    }

    @Test
    fun `CV-30 the cover with its badge and label, the example without`() {
        val ui = home()
        assertEquals("the example's cover is the preset 잿빛 새벽", "ash-dawn", ui.cover.presetId)
        assertTrue(ui.coverEditable)
        assertEquals("표지 이미지 · 바꾸기", ui.coverLabel)

        val none = home(withCover(doc, null))
        assertNull(none.cover.presetId)
        assertNull(none.cover.hash)
        assertEquals("잿", none.cover.initial)
        assertEquals("표지 이미지 · 고르기", none.coverLabel)

        val example = home(readOnly = true)
        assertFalse("read-only example: no badge, not tappable", example.coverEditable)
        assertEquals("ash-dawn", example.cover.presetId)
    }

    @Test
    fun `a picked original shows its 3 to 4 crop, a missing one the initial-letter card`() {
        val withPhoto = withCover(doc, "ab".repeat(32))
        assertEquals("ab".repeat(32), home(withPhoto).cover.hash)
        val gone = home(withPhoto, fileExists = { false })
        assertNull(gone.cover.hash)
        assertEquals("표지 이미지 · 고르기", gone.coverLabel)
    }
}
