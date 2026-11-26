package io.github.lenticualrstone.civilight.domain.actions

import io.github.lenticualrstone.civilight.domain.cast.CharacterEdits
import io.github.lenticualrstone.civilight.domain.cast.list.CastList
import io.github.lenticualrstone.civilight.domain.trash.TrashOps
import io.github.lenticualrstone.civilight.domain.trash.ItemId
import io.github.lenticualrstone.civilight.domain.trash.ItemKind
import io.github.lenticualrstone.civilight.domain.trash.MergeOps
import io.github.lenticualrstone.civilight.domain.trash.PurgeOps
import io.github.lenticualrstone.civilight.domain.trash.RestoreOps
import io.github.lenticualrstone.civilight.domain.trash.RestoreOutcome
import io.github.lenticualrstone.civilight.domain.trash.TagRestore
import io.github.lenticualrstone.civilight.domain.world.common.EditContext
import io.github.lenticualrstone.civilight.domain.world.term.TermTags
import io.github.lenticualrstone.civilight.model.cast.Tag
import io.github.lenticualrstone.civilight.model.cast.TagScope
import io.github.lenticualrstone.civilight.model.cast.TagScopes
import io.github.lenticualrstone.civilight.model.doc.ProjectDoc
import io.github.lenticualrstone.civilight.model.sample.ExampleKeys as K
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

/** (2026-11-26) Term tags: scope, uniqueness across scopes, widen only, term card reads, delete impact, trash, merge. */
class TermTagsTest {
    private val ctx = Fx.ctx()
    private val wctx = EditContext(newKey = ctx.newKey, now = { ctx.now }, device = ctx.device)
    private val t1 = K.TERM_ORIGINIUM_LANTERN
    private val t2 = K.TERM_BLACKOUT

    private fun created(r: TagCreate): Pair<ProjectDoc, Tag> = (r as TagCreate.Created).let { it.doc to it.tag }

    /** Fixture (with its gallery) + `광물` (용어 전용) + `1장 복선` (모든 항목). */
    private fun withTermTags(): Triple<ProjectDoc, Tag, Tag> {
        val (a, mineral) = created(TagActions.createFor(io.github.lenticualrstone.civilight.model.sample.ExampleGallery.build(), "광물", null, TagScope.TERM, ctx))
        val (b, foreshadow) = created(TagActions.createFor(a, "1장 복선", null, TagScope.TERM, ctx, everywhere = true))
        return Triple(b, mineral, foreshadow)
    }

    // ---- 1. create with scope ----

    @Test
    fun `a tag made from a term card is term-only, all only when chosen`() {
        val (doc, mineral, foreshadow) = withTermTags()
        assertTrue(mineral.scope isOf TagScope.TERM)
        assertTrue(foreshadow.scope isOf TagScope.ALL)
        assertTrue(doc.project.tags.last().key == foreshadow.key)
        // The character create card keeps making `인물` tags.
        val (_, lead) = TagActions.create(doc, "조연", null, ctx)!!
        assertTrue(lead.scope isOf TagScope.CHARACTER)
    }

    @Test
    fun `names are unique across scopes and the duplicate names the existing tag`() {
        val (doc, mineral, _) = withTermTags()
        val legacy = doc.project.tags.first { it.scope isOf TagScope.CHARACTER }
        // A term card typing a character tag's name: taken, and a term may not carry it.
        assertEquals(TagCreate.NameTaken(legacy, fits = false), TagActions.createFor(doc, " ${legacy.name} ", null, TagScope.TERM, ctx))
        // A character card typing `광물`: taken by the term-only tag.
        assertEquals(TagCreate.NameTaken(mineral, fits = false), TagActions.createFor(doc, "광물", null, TagScope.CHARACTER, ctx))
        assertNull("the old create refuses too", TagActions.create(doc, "광물", null, ctx))
        assertEquals(TagCreate.NameTaken(mineral, fits = true), TagActions.createFor(doc, "광물", null, TagScope.TERM, ctx))
        assertEquals(TagCreate.Blank, TagActions.createFor(doc, "  ", null, TagScope.TERM, ctx))
        assertFalse(TagActions.canName(doc, "광물"))
        // Renaming a character tag to a term tag's name is refused as well.
        assertNull(CharacterEdits.renameTag(doc, legacy.key, "광물", ctx.edit))
        // NFC: a decomposed spelling is the same name.
        val decomposed = java.text.Normalizer.normalize("광물", java.text.Normalizer.Form.NFD)
        assertTrue(TagActions.createFor(doc, decomposed, null, TagScope.TERM, ctx) is TagCreate.NameTaken)
    }

    @Test
    fun `character addTag returns a term-only tag of that name without making a second one`() {
        val (doc, mineral, _) = withTermTags()
        val (same, tag) = CharacterEdits.addTag(doc, "광물", ctx.edit)
        assertSame(doc, same)
        assertEquals(mineral.key, tag.key)
        // ...and setTags will not put it on a character.
        val next = CharacterEdits.setTags(doc, K.RIEN, doc.index.character(K.RIEN)!!.tagKeys + mineral.key, ctx.edit)
        assertFalse(mineral.key in next.index.character(K.RIEN)!!.tagKeys)
    }

    // ---- 2. widen / narrow ----

    @Test
    fun `scope widens to all and never narrows`() {
        val (doc, mineral, foreshadow) = withTermTags()
        val widened = TagActions.widen(doc, mineral.key, TagScope.ALL, ctx) as TagWiden.Widened
        assertTrue(widened.tag.scope isOf TagScope.ALL)
        assertTrue(mineral.key in CharacterEdits.tags(widened.doc).map { it.key })

        assertEquals(TagWiden.Refused(TagScope.ALL, TagScope.TERM), TagActions.widen(doc, foreshadow.key, TagScope.TERM, ctx))
        assertEquals(TagWiden.Refused(TagScope.TERM, TagScope.CHARACTER), TagActions.widen(doc, mineral.key, TagScope.CHARACTER, ctx))
        assertEquals(TagWiden.Unchanged(foreshadow), TagActions.widen(doc, foreshadow.key, TagScope.ALL, ctx))
        assertEquals(TagWiden.NotFound, TagActions.widen(doc, "nope", TagScope.ALL, ctx))
        assertTrue(TagActions.canWiden(doc, mineral.key))
        assertFalse(TagActions.canWiden(doc, foreshadow.key))
        assertFalse(TagActions.canWiden(doc, foreshadow.key, TagScope.TERM))
    }

    // ---- 3. pickers by kind ----

    @Test
    fun `tagsFor shows own scope and all in tag order, legacy stays character`() {
        val legacyDoc = io.github.lenticualrstone.civilight.model.sample.ExampleGallery.build()
        val legacy = TagScopes.live(legacyDoc).map { it.key }
        assertEquals("legacy data: every tag is a character tag", legacy, TagActions.tagsFor(legacyDoc, TagScope.CHARACTER).map { it.key })
        assertEquals(legacy, CharacterEdits.tags(legacyDoc).map { it.key })
        assertTrue(TagActions.tagsFor(legacyDoc, TagScope.TERM).isEmpty())

        val (doc, mineral, foreshadow) = withTermTags()
        assertEquals(listOf(mineral.key, foreshadow.key), TagActions.tagsFor(doc, TagScope.TERM).map { it.key })
        assertEquals(legacy + foreshadow.key, TagActions.tagsFor(doc, TagScope.CHARACTER).map { it.key })
        assertEquals(listOf(foreshadow.key), TagActions.tagsFor(doc, TagScope.PLACE).map { it.key })
        // Character list sections and chips never show the term-only tag.
        assertFalse(mineral.key in CharacterEdits.tags(doc).map { it.key })
        assertFalse(CastList.chips(doc, null).any { it.tagKey == mineral.key })
    }

    @Test
    fun `gallery images take character and all tags only`() {
        val (doc, mineral, foreshadow) = withTermTags()
        val item = doc.galleryItems.first { it.deletedAt == null }
        val edited = GalleryActions.setTags(doc, item.key, listOf(mineral.key, foreshadow.key), ctx)!!.doc
        assertEquals(listOf(foreshadow.key), edited.index.galleryItem(item.key)!!.tagKeys)
    }

    // ---- 4. attach / detach on a term, card state ----

    @Test
    fun `attach and detach on a term follow tag order and the scope`() {
        val (doc, mineral, foreshadow) = withTermTags()
        val legacy = doc.project.tags.first { it.scope isOf TagScope.CHARACTER }
        var d = TermTags.attach(doc, t1, foreshadow.key, wctx)
        d = TermTags.attach(d, t1, mineral.key, wctx)
        assertEquals("tag order, not tap order", listOf(mineral.key, foreshadow.key), d.index.term(t1)!!.tagKeys)
        assertSame("a character tag is never put on a term", d, TermTags.attach(d, t1, legacy.key, wctx))
        assertEquals(listOf("광물", "1장 복선"), TermTags.tags(d, t1).map { it.name })
        assertTrue(TermTags.canAdd(d, t1, editable = true))
        assertFalse(TermTags.canAdd(d, t1, editable = false))

        val off = TermTags.toggle(d, t1, mineral.key, wctx)
        assertEquals(listOf(foreshadow.key), off.index.term(t1)!!.tagKeys)
        val set = TermTags.set(d, t1, listOf(legacy.key, foreshadow.key), wctx)
        assertEquals(listOf(foreshadow.key), set.index.term(t1)!!.tagKeys)
        // One edit = one doc: undo is the doc before.
        assertEquals(doc.world.terms.first { it.key == t1 }, doc.index.term(t1))
    }

    // ---- 5. delete impact, trash, restore, purge ----

    @Test
    fun `delete impact counts per kind and restore reattaches on characters and terms`() {
        val (base, _, foreshadow) = withTermTags()
        var doc = TermTags.attach(base, t1, foreshadow.key, wctx)
        doc = TermTags.attach(doc, t2, foreshadow.key, wctx)
        doc = CharacterEdits.setTags(doc, K.RIEN, doc.index.character(K.RIEN)!!.tagKeys + foreshadow.key, ctx.edit)

        val impact = TagActions.impact(doc, foreshadow.key)!!
        assertEquals("인물 1 · 용어 2", impact.line)
        assertEquals(2, TagActions.usage(doc, foreshadow.key).terms)
        assertEquals("locked hidden terms are not counted", 1, TagActions.usage(doc, foreshadow.key, countedTerm = { it.key != t2 }).terms)

        val (trashed, entry) = TagActions.trash(doc, foreshadow.key, ctx)!!
        assertTrue("detached: shown nowhere", TermTags.tags(trashed, t1).isEmpty())
        assertFalse(foreshadow.key in CharacterEdits.tags(trashed).map { it.key })
        assertEquals("the key stays on the term", doc.index.term(t1)!!.tagKeys, trashed.index.term(t1)!!.tagKeys)
        // Editing the term's tags meanwhile keeps the trashed key.
        val edited = TermTags.set(trashed, t1, emptyList(), wctx)
        assertEquals(listOf(foreshadow.key), edited.index.term(t1)!!.tagKeys)

        val restored = (TagActions.restore(edited, entry.key, ctx) as TagRestore.Restored).doc
        assertEquals(listOf("1장 복선"), TermTags.tags(restored, t1).map { it.name })
        assertEquals(listOf("1장 복선"), TermTags.tags(restored, t2).map { it.name })
        assertTrue(foreshadow.key in CastList.groupTags(restored, restored.index.character(K.RIEN)!!).map { it.key })

        // 영구 삭제: the key leaves terms and characters for good.
        val purged = TagActions.purge(trashed, entry.key, ctx).doc
        assertFalse(purged.world.terms.any { foreshadow.key in it.tagKeys })
        assertFalse(purged.cast.characters.any { foreshadow.key in it.tagKeys })
    }

    @Test
    fun `impact of a term-only tag reads 용어 n, of a character tag 인물 n`() {
        val (base, mineral, _) = withTermTags()
        val doc = TermTags.attach(base, t1, mineral.key, wctx)
        assertEquals("용어 1", TagActions.impact(doc, mineral.key)!!.line)
        val legacy = doc.project.tags.first { t -> t.scope isOf TagScope.CHARACTER && doc.cast.characters.any { t.key in it.tagKeys && it.deletedAt == null } }
        val n = doc.cast.characters.count { it.deletedAt == null && legacy.key in it.tagKeys }
        assertTrue(TagActions.impact(doc, legacy.key)!!.line.startsWith("인물 $n"))
        assertNull(TagActions.impact(doc, "nope"))
    }

    // ---- 6. term trash / restore / merge / purge ----

    @Test
    fun `term tags follow the term through trash and restore`() {
        val (base, mineral, _) = withTermTags()
        val doc = TermTags.attach(base, t1, mineral.key, wctx)
        val deleted = TrashOps.delete(doc, ItemId(ItemKind.TERM, t1), ctx.trash)
        assertEquals(listOf(mineral.key), deleted.doc.index.term(t1)!!.tagKeys)
        val entry = deleted.entries.single { t1 in it.targetKeys }
        val back = (RestoreOps.restore(deleted.doc, entry.key, ctx.trash, authenticated = true) as RestoreOutcome.Restored).doc
        assertEquals(listOf("광물"), TermTags.tags(back, t1).map { it.name })
        assertTrue("+ 태그 is not offered on a trashed term", !TermTags.canAdd(deleted.doc, t1, editable = true))

        // Permanent delete: the row (with its tags) goes; a name tag stays as today.
        val purged = PurgeOps.purge(deleted.doc, entry.key, ctx.trash).doc
        assertNull(purged.index.term(t1))
        assertNotNull(purged.index.nameTag(t1))
        assertTrue("the tag itself stays live", mineral.key in TagScopes.live(purged).map { it.key })
    }

    @Test
    fun `merging terms unions their tags in tag order, trashed tags included`() {
        val (base, mineral, foreshadow) = withTermTags()
        val (withThird, third) = created(TagActions.createFor(base, "제3", null, TagScope.TERM, ctx))
        var doc = TermTags.attach(withThird, t1, foreshadow.key, wctx)
        doc = TermTags.attach(doc, t1, third.key, wctx)
        doc = TermTags.attach(doc, t2, mineral.key, wctx)
        doc = TermTags.attach(doc, t2, foreshadow.key, wctx)
        val (trashedThird, _) = TagActions.trash(doc, third.key, ctx)!!

        val merged = MergeOps.merge(trashedThird, ItemKind.TERM, t1, t2, ctx.trash)
        assertEquals(listOf(mineral.key, foreshadow.key, third.key), merged.doc.index.term(t2)!!.tagKeys)
        assertEquals(listOf("광물", "1장 복선"), TermTags.tags(merged.doc, t2).map { it.name })

        // Restoring the merged term brings it back with its own tags (from the merge snapshot).
        val back = (RestoreOps.restore(merged.doc, merged.entry.key, ctx.trash, authenticated = true) as RestoreOutcome.Restored).doc
        assertEquals(listOf(foreshadow.key, third.key), back.index.term(t1)!!.tagKeys)
    }
}
