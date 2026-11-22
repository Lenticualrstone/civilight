package io.github.lenticualrstone.civilight.feature.cast

import io.github.lenticualrstone.civilight.domain.cast.CharacterEdits
import io.github.lenticualrstone.civilight.feature.cast.settings.TagSettingsModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** `TagSettings` (CST-12): project tags with their counts, rename and add with unique names. */
class TagSettingsTest {
    private val doc = CastFx.doc()

    @Test
    fun `CST-12 rows follow the tag order and count live characters`() {
        val rows = TagSettingsModel.rows(doc)
        assertEquals(CharacterEdits.tags(doc).map { it.name }, rows.map { it.name })
        val first = rows.first()
        val live = doc.cast.characters.count { it.deletedAt == null && first.key in it.tagKeys }
        assertEquals("${first.name} · $live", first.title)
    }

    @Test
    fun `CST-12 rename keeps the key on every character, refuses a taken or blank name`() {
        val rows = TagSettingsModel.rows(doc)
        val (a, b) = rows[0] to rows[1]
        assertNull(TagSettingsModel.rename(doc, a.key, b.name, CastFx.ctx()))
        assertNull(TagSettingsModel.rename(doc, a.key, "  ", CastFx.ctx()))
        assertNull(TagSettingsModel.rename(doc, a.key, a.name, CastFx.ctx()))
        val renamed = TagSettingsModel.rename(doc, a.key, "주역", CastFx.ctx())!!
        assertEquals("주역", renamed.project.tags.single { it.key == a.key }.name)
        assertEquals(doc.cast.characters.map { it.tagKeys }, renamed.cast.characters.map { it.tagKeys })
    }

    @Test
    fun `CST-12 add puts a new tag at the end, never a second one with the same name`() {
        val added = TagSettingsModel.add(doc, "조연", CastFx.ctx())!!
        assertEquals("조연", CharacterEdits.tags(added).last().name)
        assertFalse(TagSettingsModel.canName(added, "조연"))
        assertNull(TagSettingsModel.add(added, "조연", CastFx.ctx()))
        assertTrue(TagSettingsModel.canName(doc, "조연"))
    }
}
