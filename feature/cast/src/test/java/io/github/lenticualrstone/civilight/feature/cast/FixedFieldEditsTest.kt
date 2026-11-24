package io.github.lenticualrstone.civilight.feature.cast

import io.github.lenticualrstone.civilight.feature.cast.profile.FixedFieldEdits
import io.github.lenticualrstone.civilight.feature.cast.profile.TagPick
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import io.github.lenticualrstone.civilight.model.sample.ExampleKeys as K

/** Fixed fields without history: `이름`, `한 줄 소개`, `태그` (CST-07, CST-12). */
class FixedFieldEditsTest {
    private val doc = CastFx.doc()
    private val ctx = CastFx.ctx()

    @Test
    fun `CST-07 rename needs a different, non-blank name`() {
        assertFalse(FixedFieldEdits.canRename("  ", "리엔"))
        assertFalse(FixedFieldEdits.canRename(" 리엔 ", "리엔"))
        assertTrue(FixedFieldEdits.canRename("리엔 하트", "리엔"))
        assertEquals("리아", FixedFieldEdits.rename(doc, K.RIEN, "리아", ctx).index.character(K.RIEN)!!.name)
        assertSame(doc, FixedFieldEdits.rename(doc, K.RIEN, " ", ctx))
    }

    @Test
    fun `CST-07 한 줄 소개 is saved as typed`() {
        val next = FixedFieldEdits.setOneLiner(doc, K.RIEN, "막내 등불지기", ctx)
        assertEquals("막내 등불지기", next.index.character(K.RIEN)!!.oneLiner)
    }

    @Test
    fun `CST-12 chips toggle tags and a typed name picks an existing tag or queues a new one`() {
        val picks = FixedFieldEdits.picks(doc, K.RIEN)
        assertEquals(K.TAG_LEAD, picks.first().key)
        val off = FixedFieldEdits.toggle(picks, K.TAG_LEAD, "주연")
        assertFalse(off.any { it.key == K.TAG_LEAD })
        assertEquals(picks.toSet(), FixedFieldEdits.toggle(off, K.TAG_LEAD, "주연").toSet())

        val existing = FixedFieldEdits.addName(doc, emptyList(), " 상층 ")
        assertEquals(listOf(TagPick(K.TAG_UPPER, "상층")), existing)
        val queued = FixedFieldEdits.addName(doc, existing, "회상 인물")
        assertEquals(TagPick(null, "회상 인물"), queued.last())
        assertEquals(queued, FixedFieldEdits.addName(doc, queued, "회상 인물"))
        assertEquals(queued, FixedFieldEdits.addName(doc, queued, "  "))
    }

    @Test
    fun `CST-12 saving makes new names project tags and keeps the pick order`() {
        val picks = listOf(TagPick(K.TAG_UPPER, "상층"), TagPick(null, "회상 인물"))
        val next = FixedFieldEdits.apply(doc, K.RIEN, picks, ctx)
        val tags = next.index.character(K.RIEN)!!.tagKeys
        assertEquals(2, tags.size)
        assertEquals(K.TAG_UPPER, tags[0])
        assertEquals("회상 인물", next.index.tag(tags[1])!!.name)
        assertEquals(doc.project.tags.size + 1, next.project.tags.size)
    }

    @Test
    fun `a tag made from the sheet joins the save baseline, so untapping it enables save`() {
        val initial = FixedFieldEdits.picks(doc, K.RIEN)
        // `＋ 새 태그` wrote the tag and checked it on the character; picks and baseline both gain it.
        val picks = FixedFieldEdits.created(initial, "NEWTAG", "조연")
        val baseline = FixedFieldEdits.created(initial, "NEWTAG", "조연")
        assertEquals(picks, baseline)
        assertEquals(picks, FixedFieldEdits.created(picks, "NEWTAG", "조연"))
        val untapped = FixedFieldEdits.toggle(picks, "NEWTAG", "조연")
        assertTrue(untapped != baseline)
    }
}
