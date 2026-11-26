package io.github.lenticualrstone.civilight.feature.cast

import io.github.lenticualrstone.civilight.domain.actions.TagActions
import io.github.lenticualrstone.civilight.domain.actions.TagCreate
import io.github.lenticualrstone.civilight.domain.cast.CharacterEdits
import io.github.lenticualrstone.civilight.domain.cast.list.CastList
import io.github.lenticualrstone.civilight.domain.world.term.TermTags
import io.github.lenticualrstone.civilight.feature.cast.profile.FixedFieldEdits
import io.github.lenticualrstone.civilight.feature.cast.settings.TagDeleteModel
import io.github.lenticualrstone.civilight.feature.cast.settings.TagSettingsModel
import io.github.lenticualrstone.civilight.model.cast.TagScope
import io.github.lenticualrstone.civilight.model.doc.ProjectDoc
import io.github.lenticualrstone.civilight.model.sample.ExampleKeys as K
import io.github.lenticualrstone.civilight.ui.picker.PickerCreateCards
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** (2026-11-26) Character surfaces with term-only and all-scope tags present; legacy data unchanged. */
class TagScopeCastTest {
    private val ctx = CastFx.action()

    private fun withTags(): Triple<ProjectDoc, String, String> {
        val a = TagActions.createFor(CastFx.doc(), "광물", null, TagScope.TERM, ctx) as TagCreate.Created
        val b = TagActions.createFor(a.doc, "1장 복선", null, TagScope.TERM, ctx, everywhere = true) as TagCreate.Created
        return Triple(b.doc, a.tag.key, b.tag.key)
    }

    @Test
    fun `legacy data shows the same tags as before on every character surface`() {
        val doc = CastFx.doc()
        val live = doc.project.tags.filter { it.isLive }.sortedBy { it.order }.map { it.key }
        assertEquals(live, CharacterEdits.tags(doc).map { it.key })
        assertEquals(live, PickerCreateCards.liveTags(doc).map { it.key })
        assertEquals(live, TagSettingsModel.rows(doc).map { it.key })
    }

    @Test
    fun `term-only tags stay out of character lists, pickers and chips, but tag settings lists every scope`() {
        val (doc, mineral, all) = withTags()
        assertFalse(mineral in PickerCreateCards.liveTags(doc).map { it.key })
        assertTrue(all in PickerCreateCards.liveTags(doc).map { it.key })
        assertFalse(CharacterEdits.tags(doc).any { it.key == mineral })
        assertFalse(CastList.chips(doc, null).any { it.tagKey == mineral })
        // (2026-11-26) Tag settings is the one place that manages every live tag.
        assertEquals(io.github.lenticualrstone.civilight.model.cast.TagScopes.live(doc).map { it.key }, TagSettingsModel.rows(doc).map { it.key })
        assertTrue(TagSettingsModel.rows(doc).any { it.key == mineral })
        // Name checks see every scope.
        assertFalse(TagSettingsModel.canName(doc, "광물"))
    }

    @Test
    fun `the character tag sheet never picks or creates a term-only name`() {
        val (doc, mineral, all) = withTags()
        assertEquals(emptyList<Any>(), FixedFieldEdits.addName(doc, emptyList(), "광물"))
        assertEquals(mineral, FixedFieldEdits.takenElsewhere(doc, "광물")?.key)
        assertNull(FixedFieldEdits.takenElsewhere(doc, "1장 복선"))
        val picks = FixedFieldEdits.addName(doc, emptyList(), "1장 복선")
        assertEquals(listOf(all), picks.map { it.key })
        val saved = FixedFieldEdits.apply(doc, K.RIEN, picks + io.github.lenticualrstone.civilight.feature.cast.profile.TagPick(null, "광물"), CastFx.ctx())
        val tags = saved.index.character(K.RIEN)!!.tagKeys
        assertTrue(all in tags)
        assertFalse(mineral in tags)
        assertEquals("no second 광물", 1, saved.project.tags.count { it.name == "광물" })
    }

    @Test
    fun `delete confirm counts terms of an all-scope tag`() {
        val (base, _, all) = withTags()
        val wctx = io.github.lenticualrstone.civilight.domain.world.common.EditContext(newKey = ctx.newKey, now = { ctx.now }, device = ctx.device)
        var doc = TermTags.attach(base, K.TERM_ORIGINIUM_LANTERN, all, wctx)
        doc = CharacterEdits.setTags(doc, K.RIEN, doc.index.character(K.RIEN)!!.tagKeys + all, CastFx.ctx())
        val confirm = TagDeleteModel.confirm(doc, all, hiddenUnlocked = true)!!
        assertEquals(listOf("쓰는 인물 1", "용어 1"), confirm.lines)
    }

    @Test
    fun `a held tag of an unknown scope (newer file) is never detached, nor newly attached`() {
        val base = CastFx.doc()
        val future = io.github.lenticualrstone.civilight.model.OpenEnum.unknown<TagScope>("somethingNew")
        val held = base.index.character(K.RIEN)!!.tagKeys.first()
        val doc = base.copy(project = base.project.copy(tags = base.project.tags.map { if (it.key == held) it.copy(scope = future) else it }))
        // Saving the tag sheet without it (pickers never offer it) keeps it.
        val saved = FixedFieldEdits.apply(doc, K.RIEN, emptyList(), CastFx.ctx())
        assertTrue(held in saved.index.character(K.RIEN)!!.tagKeys)
        assertTrue(held in CharacterEdits.setTags(doc, K.RIEN, emptyList(), CastFx.ctx()).index.character(K.RIEN)!!.tagKeys)
        // Another character not holding it does not get it.
        val other = doc.cast.characters.first { it.key != K.RIEN && held !in it.tagKeys }.key
        assertFalse(held in CharacterEdits.setTags(doc, other, listOf(held), CastFx.ctx()).index.character(other)!!.tagKeys)
    }
}
