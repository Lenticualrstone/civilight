package io.github.lenticualrstone.civilight.feature.cast

import io.github.lenticualrstone.civilight.domain.cast.CharacterEdits
import io.github.lenticualrstone.civilight.feature.cast.settings.TagSettingsModel
import io.github.lenticualrstone.civilight.model.sample.ExampleGallery
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
        assertEquals(live, first.count)
        // 55 §3.4: the row reads `인물 n · 이미지 m`; no gallery yet means no images.
        assertEquals("인물 $live · 이미지 0", first.line)
    }

    @Test
    fun `55 3_4 rows count gallery images, hidden ones only while hidden items are open`() {
        val g0 = ExampleGallery.build()
        fun line(tag: String, unlocked: Boolean) = TagSettingsModel.rows(g0, unlocked).single { it.key == tag }.line.substringAfter(" · ")
        assertEquals("이미지 4", line(ExampleGallery.TAG_ILLUSTRATION, unlocked = false))
        assertEquals("이미지 5", line(ExampleGallery.TAG_ILLUSTRATION, unlocked = true))
        assertEquals("이미지 2", line(ExampleGallery.TAG_SETTING, unlocked = false))
        assertEquals("이미지 1", line(ExampleGallery.TAG_PROP, unlocked = false))
        // Unlocked, a row is exactly the domain's usage line.
        for (row in TagSettingsModel.rows(g0, hiddenUnlocked = true)) {
            assertEquals(CharacterEdits.tagUsage(g0, row.key), row.usage)
        }
    }

    @Test
    fun `55 3_4 rename keeps gallery images on the tag by key`() {
        val g0 = ExampleGallery.build()
        val renamed = TagSettingsModel.rename(g0, ExampleGallery.TAG_PROP, "소지품", CastFx.ctx())!!
        assertEquals("소지품", renamed.project.tags.single { it.key == ExampleGallery.TAG_PROP }.name)
        assertEquals(g0.gallery, renamed.gallery)
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
