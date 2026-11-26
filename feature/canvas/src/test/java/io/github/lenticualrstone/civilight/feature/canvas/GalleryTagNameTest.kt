package io.github.lenticualrstone.civilight.feature.canvas

import io.github.lenticualrstone.civilight.feature.canvas.gallery.GalleryTagName
import io.github.lenticualrstone.civilight.model.OpenEnum
import io.github.lenticualrstone.civilight.model.cast.TagScope
import io.github.lenticualrstone.civilight.model.doc.ProjectDoc
import io.github.lenticualrstone.civilight.ui.tags.TagNameCheck
import io.github.lenticualrstone.civilight.ui.tags.TagSheetCopy
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** (2026-11-26) Gallery `+ 태그`: a taken name never silently does nothing. */
class GalleryTagNameTest {
    private fun doc(): Pair<ProjectDoc, String> {
        val d = CanvasFx.s0()
        val first = d.project.tags.first { it.isLive }
        val termOnly = first.copy(scope = OpenEnum.of(TagScope.TERM))
        return d.copy(project = d.project.copy(tags = d.project.tags.map { if (it.key == first.key) termOnly else it })) to first.name
    }

    @Test
    fun `a term-only name shows the line alone and blocks 넣기`() {
        val (d, name) = doc()
        val taken = GalleryTagName.taken(d, name)!!
        assertFalse(taken.fits)
        assertNull(TagNameCheck.action(taken))
        assertEquals("이미 있음 · $name", TagSheetCopy.taken(taken.name))
        assertFalse(GalleryTagName.canAdd(d, name))
    }

    @Test
    fun `a fitting name offers 이 태그 쓰기 and a free name can be added`() {
        val (d, _) = doc()
        val other = d.project.tags.filter { it.isLive }[1]
        val taken = GalleryTagName.taken(d, other.name)!!
        assertTrue(taken.fits)
        assertEquals(TagSheetCopy.USE_EXISTING, TagNameCheck.action(taken))
        assertFalse(GalleryTagName.canAdd(d, other.name))
        assertTrue(GalleryTagName.canAdd(d, "새 태그 이름"))
        assertFalse(GalleryTagName.canAdd(d, "  "))
        assertEquals(listOf("a", other.key), GalleryTagName.withTag(listOf("a"), other.key))
        assertEquals(listOf(other.key, "a"), GalleryTagName.withTag(listOf(other.key, "a"), other.key))
    }
}
