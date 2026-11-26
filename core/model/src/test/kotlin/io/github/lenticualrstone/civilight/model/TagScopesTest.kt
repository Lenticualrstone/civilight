package io.github.lenticualrstone.civilight.model

import io.github.lenticualrstone.civilight.model.cast.Tag
import io.github.lenticualrstone.civilight.model.cast.TagScope
import io.github.lenticualrstone.civilight.model.cast.TagScopes
import io.github.lenticualrstone.civilight.model.cast.fits
import io.github.lenticualrstone.civilight.model.sample.ExampleProject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** (2026-11-26) `Tag.fits` and `TagScopes` reads. */
class TagScopesTest {
    @Test
    fun legacyTagFitsCharactersOnly() {
        val tag = Tag(key = "T", name = "주연", order = "V")
        assertTrue(tag.fits(TagScope.CHARACTER))
        assertFalse(tag.fits(TagScope.TERM))
    }

    @Test
    fun allFitsEveryKindAndUnknownFitsNone() {
        val all = Tag(key = "A", name = "복선", order = "V", scope = TagScope.ALL.open())
        for (k in listOf(TagScope.CHARACTER, TagScope.TERM, TagScope.PLACE, TagScope.FACTION)) assertTrue(all.fits(k))
        val unknown = Tag(key = "U", name = "미래", order = "V", scope = OpenEnum.unknown("somethingNew"))
        for (k in TagScope.entries) assertFalse(unknown.fits(k))
    }

    @Test
    fun tagsForOnLegacyDataIsEveryLiveTag() {
        val doc = ExampleProject.build()
        assertEquals(TagScopes.live(doc), TagScopes.tagsFor(doc, TagScope.CHARACTER))
        assertTrue(TagScopes.tagsFor(doc, TagScope.TERM).isEmpty())
    }

    @Test
    fun liveNamedIgnoresScopeAndTrashed() {
        val doc = ExampleProject.build()
        val tag = TagScopes.live(doc).first()
        assertEquals(tag, TagScopes.liveNamed(doc, " ${tag.name} "))
        assertNull(TagScopes.liveNamed(doc, tag.name, except = tag.key))
        assertNull(TagScopes.liveNamed(doc, "  "))
    }
}
