package io.github.lenticualrstone.civilight.feature.world.term

import io.github.lenticualrstone.civilight.domain.actions.ActionContext
import io.github.lenticualrstone.civilight.domain.actions.TagActions
import io.github.lenticualrstone.civilight.domain.actions.TagCreate
import io.github.lenticualrstone.civilight.domain.world.term.TermTags
import io.github.lenticualrstone.civilight.feature.world.WorldTestFixture
import io.github.lenticualrstone.civilight.feature.world.WorldTestFixture.PROJECT
import io.github.lenticualrstone.civilight.feature.world.WorldTestFixture.T1
import io.github.lenticualrstone.civilight.model.cast.TagScope
import io.github.lenticualrstone.civilight.model.doc.ProjectDoc
import io.github.lenticualrstone.civilight.model.sample.ExampleClock
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** (2026-11-26) The term card's tag row: live, scope-valid tags in tag order, and whether `+ 태그` is offered. */
class TermCardTagsTest {
    private val actx = ActionContext(now = ExampleClock.NOW, device = "test-device", newKey = WorldTestFixture.ctx(3).newKey)

    private fun card(doc: ProjectDoc, readOnly: Boolean = false) =
        TermCardState.build(doc, PROJECT, T1, WorldTestFixture.index(doc), hiddenOpen = false, readOnly = readOnly)

    @Test
    fun `card shows term and all tags, never character tags, and offers plus tag when editable`() {
        var doc = WorldTestFixture.stateA()
        assertTrue(card(doc).tags.isEmpty())
        assertTrue(card(doc).canAddTag)
        assertFalse(card(doc, readOnly = true).canAddTag)

        val all = (TagActions.createFor(doc, "1장 복선", "amber", TagScope.TERM, actx, everywhere = true) as TagCreate.Created)
        val term = (TagActions.createFor(all.doc, "광물", null, TagScope.TERM, actx) as TagCreate.Created)
        doc = term.doc
        val ctx = WorldTestFixture.ctx()
        doc = TermTags.attach(doc, T1, all.tag.key, ctx)
        doc = TermTags.attach(doc, T1, term.tag.key, ctx)
        val legacy = doc.project.tags.first { it.scope isOf TagScope.CHARACTER }
        // A character tag key slipped in by an old or foreign file is not shown.
        doc = doc.copy(world = doc.world.copy(terms = doc.world.terms.map { if (it.key == T1) it.copy(tagKeys = it.tagKeys + legacy.key) else it }))

        val ui = card(doc)
        assertEquals(listOf(TermTagUi(all.tag.key, "1장 복선", "amber"), TermTagUi(term.tag.key, "광물", null)), ui.tags)

        val (trashed, _) = TagActions.trash(doc, term.tag.key, actx)!!
        assertEquals(listOf("1장 복선"), card(trashed).tags.map { it.name })
    }
}
