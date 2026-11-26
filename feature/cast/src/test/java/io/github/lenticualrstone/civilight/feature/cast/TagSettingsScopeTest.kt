package io.github.lenticualrstone.civilight.feature.cast

import io.github.lenticualrstone.civilight.designsystem.text.CopyLint
import io.github.lenticualrstone.civilight.domain.actions.TagActions
import io.github.lenticualrstone.civilight.domain.actions.TagCreate
import io.github.lenticualrstone.civilight.domain.actions.TagWiden
import io.github.lenticualrstone.civilight.domain.cast.CharacterEdits
import io.github.lenticualrstone.civilight.domain.world.term.TermTags
import io.github.lenticualrstone.civilight.feature.cast.profile.FixedFieldEdits
import io.github.lenticualrstone.civilight.feature.cast.profile.TagPick
import io.github.lenticualrstone.civilight.feature.cast.settings.TagDeleteModel
import io.github.lenticualrstone.civilight.feature.cast.settings.TagNewScope
import io.github.lenticualrstone.civilight.feature.cast.settings.TagSettingsCopy
import io.github.lenticualrstone.civilight.feature.cast.settings.TagSettingsModel
import io.github.lenticualrstone.civilight.model.cast.TagScope
import io.github.lenticualrstone.civilight.model.doc.ProjectDoc
import io.github.lenticualrstone.civilight.model.sample.ExampleKeys as K
import io.github.lenticualrstone.civilight.ui.tags.TagNameCheck
import io.github.lenticualrstone.civilight.ui.tags.TagSheetCopy
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

/** (2026-11-26) Tag settings over every scope: labels, counts, widen confirm, same-name feedback. */
class TagSettingsScopeTest {
    private val ctx = CastFx.action()

    /** `광물` (용어 전용, on one term), `1장 복선` (모든 항목, on a term and 리엔). */
    private fun withTags(): Triple<ProjectDoc, String, String> {
        val a = TagActions.createFor(CastFx.doc(), "광물", null, TagScope.TERM, ctx) as TagCreate.Created
        val b = TagActions.createFor(a.doc, "1장 복선", null, TagScope.TERM, ctx, everywhere = true) as TagCreate.Created
        val wctx = io.github.lenticualrstone.civilight.domain.world.common.EditContext(newKey = ctx.newKey, now = { ctx.now }, device = ctx.device)
        var doc = TermTags.attach(b.doc, K.TERM_ORIGINIUM_LANTERN, a.tag.key, wctx)
        doc = TermTags.attach(doc, K.TERM_ORIGINIUM_LANTERN, b.tag.key, wctx)
        doc = CharacterEdits.setTags(doc, K.RIEN, doc.index.character(K.RIEN)!!.tagKeys + b.tag.key, CastFx.ctx())
        return Triple(doc, a.tag.key, b.tag.key)
    }

    @Test
    fun `rows carry scope labels and per-kind counts`() {
        val (doc, mineral, all) = withTags()
        val rows = TagSettingsModel.rows(doc, hiddenUnlocked = true)
        val m = rows.single { it.key == mineral }
        assertEquals("용어", m.scopeLabel)
        assertFalse(m.everywhere)
        assertEquals("용어 1", m.line)
        val a = rows.single { it.key == all }
        assertEquals("전체", a.scopeLabel)
        assertTrue(a.everywhere)
        assertEquals("인물 1 · 용어 1", a.line)
        val lead = rows.single { it.key == K.TAG_LEAD }
        assertEquals("인물", lead.scopeLabel)
        assertTrue(lead.line!!.startsWith("인물 "))
    }

    @Test
    fun `widen is offered only on a kind's own scope and confirms one way`() {
        val (doc, mineral, all) = withTags()
        val rows = TagSettingsModel.rows(doc).associateBy { it.key }
        assertTrue(rows.getValue(mineral).canWiden)
        assertTrue(rows.getValue(K.TAG_LEAD).canWiden)
        assertFalse(rows.getValue(all).canWiden)
        val confirm = TagSettingsModel.widenConfirm(doc, mineral)!!
        assertEquals("‘광물’ 모든 항목으로 넓히기", confirm.title)
        assertEquals(listOf("용어 전용 → 모든 항목", "줄일 수 없음"), confirm.lines)
        assertNull("모든 항목 has no widen", TagSettingsModel.widenConfirm(doc, all))
        val widened = TagActions.widen(doc, mineral, TagScope.ALL, ctx)
        assertNull(TagSettingsModel.widenNotice(widened))
        val after = (widened as TagWiden.Widened).doc
        assertEquals("전체", TagSettingsModel.rows(after).single { it.key == mineral }.scopeLabel)
        assertNull(TagSettingsModel.widenConfirm(after, mineral))
        assertEquals(TagSettingsCopy.WIDEN_REFUSED, TagSettingsModel.widenNotice(TagWiden.Refused(TagScope.ALL, TagScope.TERM)))
        assertEquals(TagSettingsCopy.GONE, TagSettingsModel.widenNotice(TagWiden.NotFound))
    }

    @Test
    fun `widen is one way - kept undo steps are widened too, older steps without the tag stay as is`() {
        val (doc, mineral, _) = withTags()
        // An undo step from before the tag was made: nothing to widen there.
        val older = CastFx.doc()
        assertSame(older, TagSettingsModel.widenOnStep(older, mineral, ctx))
        // A kept step where the tag is still 용어 전용: undoing to it must not narrow the tag again.
        val step = TagSettingsModel.widenOnStep(doc, mineral, ctx)
        assertEquals(TagScope.ALL, step.index.tag(mineral)!!.scope.known)
        // Already 모든 항목: unchanged.
        assertSame(step, TagSettingsModel.widenOnStep(step, mineral, ctx))
        // A kept step where the tag is in the trash: widened too, so restoring it later never brings it back narrow.
        val trashed = doc.copy(project = doc.project.copy(tags = doc.project.tags.map { if (it.key == mineral) it.copy(deletedAt = 1L) else it }))
        val trashedStep = TagSettingsModel.widenOnStep(trashed, mineral, ctx).index.tag(mineral)!!
        assertEquals(TagScope.ALL, trashedStep.scope.known)
        assertFalse(trashedStep.isLive)
    }

    @Test
    fun `new tag scope radios default to 인물 전용 and write the picked scope`() {
        assertEquals(TagNewScope.CHARACTER, TagNewScope.DEFAULT)
        assertEquals(listOf("인물 전용", "용어 전용", "모든 항목"), TagNewScope.entries.map { it.label })
        assertEquals(TagNewScope.CHARACTER, TagNewScope.of(null))
        val doc = CastFx.doc()
        val term = TagSettingsModel.create(doc, "광물", null, TagNewScope.TERM, ctx) as TagCreate.Created
        assertEquals(TagScope.TERM, term.tag.scope.known)
        val all = TagSettingsModel.create(doc, "비밀", null, TagNewScope.ALL, ctx) as TagCreate.Created
        assertEquals(TagScope.ALL, all.tag.scope.known)
        val own = TagSettingsModel.create(doc, "조연", null, TagNewScope.CHARACTER, ctx) as TagCreate.Created
        assertEquals(TagScope.CHARACTER, own.tag.scope.known)
        // A taken name of any scope is never made twice.
        assertTrue(TagSettingsModel.create(term.doc, "광물", null, TagNewScope.CHARACTER, ctx) is TagCreate.NameTaken)
    }

    @Test
    fun `rename works on any scope and a taken name shows the inline error`() {
        val (doc, mineral, all) = withTags()
        val renamed = TagSettingsModel.rename(doc, mineral, "광석", CastFx.ctx())!!
        assertEquals("광석", renamed.index.tag(mineral)!!.name)
        assertNull(TagSettingsModel.rename(doc, mineral, "1장 복선", CastFx.ctx()))
        assertEquals("이미 있음 · 1장 복선", TagSettingsModel.nameError(doc, "1장 복선", except = mineral))
        assertNull(TagSettingsModel.nameError(doc, "광물", except = mineral))
        assertEquals("이미 있음 · 광물", TagSettingsModel.nameError(doc, " 광물 ", except = all))
    }

    @Test
    fun `delete confirm lists one line per kind the tag may be on`() {
        val (doc, mineral, all) = withTags()
        assertEquals(listOf("용어 1"), TagDeleteModel.confirm(doc, mineral, hiddenUnlocked = true)!!.lines)
        assertEquals(listOf("쓰는 인물 1", "용어 1"), TagDeleteModel.confirm(doc, all, hiddenUnlocked = true)!!.lines)
        assertTrue(TagDeleteModel.confirm(doc, K.TAG_LEAD, hiddenUnlocked = true)!!.lines.first().startsWith("쓰는 인물 "))
    }

    @Test
    fun `same-name feedback in the character picker offers 이 태그 쓰기 only for a fitting tag`() {
        val (doc, mineral, all) = withTags()
        val termOnly = TagNameCheck.taken(doc, "광물", TagScope.CHARACTER)!!
        assertEquals(mineral, termOnly.key)
        assertFalse(termOnly.fits)
        assertNull("a 용어 전용 name: the line alone", TagNameCheck.action(termOnly))
        assertEquals("이미 있음 · 광물", TagSheetCopy.taken(termOnly.name))
        val fits = TagNameCheck.taken(doc, "1장 복선", TagScope.CHARACTER)!!
        assertTrue(fits.fits)
        assertEquals(TagSheetCopy.USE_EXISTING, TagNameCheck.action(fits))
        assertNull(TagNameCheck.taken(doc, "새 이름", TagScope.CHARACTER))
        assertNull(TagNameCheck.taken(doc, "  ", TagScope.CHARACTER))
        // `이 태그 쓰기` checks the existing tag once.
        val picks = FixedFieldEdits.useExisting(emptyList(), fits.key, fits.name)
        assertEquals(listOf(TagPick(all, "1장 복선")), picks)
        assertEquals(picks, FixedFieldEdits.useExisting(picks, fits.key, fits.name))
    }

    @Test
    fun `settings copy passes the lint`() {
        val lines = TagSettingsCopy.all + listOf(TagSettingsCopy.widenTitle("광물"), TagSettingsCopy.widenFromTo("용어 전용")) + TagNewScope.entries.map { it.label }
        lines.forEach { assertEquals(it, emptyList<Any>(), CopyLint.lint(it)) }
        assertNotNull(TagSettingsCopy.WIDEN)
    }
}
