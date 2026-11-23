package io.github.lenticualrstone.civilight.domain.cast

import io.github.lenticualrstone.civilight.domain.cast.field.FieldSettings
import io.github.lenticualrstone.civilight.domain.cast.field.Profile
import io.github.lenticualrstone.civilight.domain.cast.list.CastList
import io.github.lenticualrstone.civilight.model.Key
import io.github.lenticualrstone.civilight.model.cast.EntityKind
import io.github.lenticualrstone.civilight.model.doc.ProjectDoc
import io.github.lenticualrstone.civilight.model.open
import io.github.lenticualrstone.civilight.model.position.LensPoint
import io.github.lenticualrstone.civilight.model.script.BlockKind
import io.github.lenticualrstone.civilight.model.text.LinkTarget
import io.github.lenticualrstone.civilight.model.text.LinkTargetKind
import io.github.lenticualrstone.civilight.model.text.TextRun
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import io.github.lenticualrstone.civilight.model.sample.ExampleKeys as K

/** 0.2.0 cast rules in `domain:cast`: N-44 (tags), N-47 (appearances, sort), U-15 create, G-2 cast surfaces. */
class Cast020Test {
    private val doc = Fx.doc()
    private val ctx = Fx.ctx()

    private fun trashTag(doc: ProjectDoc, tagKey: Key): ProjectDoc =
        doc.copy(project = doc.project.copy(tags = doc.project.tags.map { if (it.key == tagKey) it.copy(deletedAt = 1L) else it }))

    private fun withTags(doc: ProjectDoc, characterKey: Key, tags: List<Key>): ProjectDoc =
        doc.copy(cast = doc.cast.copy(characters = doc.cast.characters.map { if (it.key == characterKey) it.copy(tagKeys = tags) else it }))

    private fun gameTools(doc: ProjectDoc, on: Boolean): ProjectDoc = doc.copy(project = doc.project.copy(gameTools = on))

    // --- N-44 · CST-68 several tags, every group ---

    @Test
    fun `TC-N44-01 a character with two tags is listed in both sections and counted once`() {
        val two = withTags(doc, K.RIEN, listOf(K.TAG_LEAD, K.TAG_LANTERN_SOCIETY))
        val view = CastList.build(two, LensPoint.Latest)
        val sections = view.groups.filter { g -> g.rows.any { it.key == K.RIEN } }.map { it.tagKey }
        assertEquals(listOf(K.TAG_LEAD, K.TAG_LANTERN_SOCIETY), sections)
        val shown = CastList.visible(two, false).first.size
        assertEquals("전체 $shown", view.chips.first().label)
        assertEquals(view.rows.map { it.key }.distinct().size, shown)
    }

    @Test
    fun `TC-N44-02 a trashed tag makes no chip, no section and no search hit, the characters keep the key`() {
        val two = withTags(doc, K.RIEN, listOf(K.TAG_LEAD, K.TAG_LANTERN_SOCIETY))
        val name = two.index.tag(K.TAG_LEAD)!!.name
        val trashed = trashTag(two, K.TAG_LEAD)
        val view = CastList.build(trashed, LensPoint.Latest)
        assertTrue(view.chips.none { it.tagKey == K.TAG_LEAD })
        assertTrue(view.groups.none { it.tagKey == K.TAG_LEAD })
        assertTrue(view.groups.single { it.tagKey == K.TAG_LANTERN_SOCIETY }.rows.any { it.key == K.RIEN })
        assertTrue(K.TAG_LEAD in trashed.index.character(K.RIEN)!!.tagKeys)
        assertFalse(CastList.matches(trashed, trashed.index.character(K.RIEN)!!.copy(tagKeys = listOf(K.TAG_LEAD)), name))
        assertTrue(CharacterEdits.tags(trashed).none { it.key == K.TAG_LEAD })
    }

    @Test
    fun `TC-N44-03 a character whose tags are all trashed is in the untagged section`() {
        val one = withTags(doc, K.RIEN, listOf(K.TAG_LEAD))
        val trashed = trashTag(one, K.TAG_LEAD)
        val untagged = CastList.build(trashed, LensPoint.Latest).groups.single { it.tagKey == null }
        assertEquals(CastList.UNTAGGED, untagged.title)
        assertTrue(untagged.rows.any { it.key == K.RIEN })
    }

    @Test
    fun `TC-N44-04 addTag and renameTag compare with live tags only`() {
        val name = doc.index.tag(K.TAG_LEAD)!!.name
        val trashed = trashTag(doc, K.TAG_LEAD)
        val (added, tag) = CharacterEdits.addTag(trashed, name, ctx)
        assertTrue("a trashed tag is not reused", tag.key != K.TAG_LEAD)
        assertEquals(2, added.project.tags.count { it.name == name })
        val renamed = CharacterEdits.renameTag(trashed, K.TAG_UPPER, name, ctx)
        assertEquals(name, renamed!!.index.tag(K.TAG_UPPER)!!.name)
    }

    // --- U-15 · CST-66 create ---

    @Test
    fun `TC-U15-01 create keeps display name and live tags only, as one new character`() {
        val trashed = trashTag(doc, K.TAG_UPPER)
        val (next, c) = CharacterEdits.create(
            trashed, " 세라 ", ctx, oneLiner = " 한 줄 ", displayNameBase = " 붉은 망토 ",
            tagKeys = listOf(K.TAG_LEAD, K.TAG_UPPER, K.TAG_LEAD),
        )
        assertEquals("세라", c.name)
        assertEquals("붉은 망토", c.displayNameBase)
        assertEquals("한 줄", c.oneLiner)
        assertEquals(listOf(K.TAG_LEAD), c.tagKeys)
        assertEquals(doc.cast.characters.size + 1, next.cast.characters.size)
        assertEquals(doc.project.counters.nextCharacterRecordNo, c.recordNo)
        assertNull(CharacterEdits.create(doc, "노아", ctx, displayNameBase = "  ").second.displayNameBase)
    }

    // --- N-47 · CST-71 appearances, CST-69 sort ---

    private val x = Fx.key("CHAR", 900)

    private fun fixtureP(): ProjectDoc {
        var d = Fx.addCharacter(doc, x, "테스트", 900)
        d = Fx.addBlock(d, Fx.key("BLCK", 901), K.SCENE_1_2_1, 901, "대사", BlockKind.DIALOGUE, speaker = x)
        d = Fx.addBlock(d, Fx.key("BLCK", 902), K.SCENE_1_2_2, 902, "또 대사", BlockKind.DIALOGUE, speaker = x)
        d = Fx.addBlock(d, Fx.key("BLCK", 903), K.SCENE_1_4_1, 903, "")
        d = Fx.editBlock(d, Fx.key("BLCK", 903)) {
            it.copy(runs = listOf(TextRun.Plain("서술 "), TextRun.Link("lnk1", LinkTarget(LinkTargetKind.CHARACTER, x), "테스트")))
        }
        // A memo link and a direction link do not count.
        d = Fx.addBlock(d, Fx.key("BLCK", 904), K.SCENE_1_5_1, 904, "", BlockKind.MEMO)
        d = Fx.editBlock(d, Fx.key("BLCK", 904)) { it.copy(runs = listOf(TextRun.Link("lnk2", LinkTarget(LinkTargetKind.CHARACTER, x), "테스트"))) }
        d = Fx.addBlock(d, Fx.key("BLCK", 905), K.SCENE_1_5_2, 905, "", BlockKind.DIRECTION)
        d = Fx.editBlock(d, Fx.key("BLCK", 905)) { it.copy(runs = listOf(TextRun.Link("lnk3", LinkTarget(LinkTargetKind.CHARACTER, x), "테스트"))) }
        return d
    }

    @Test
    fun `TC-N47-01 bars count scenes per episode from speaking and game-text links, memo and direction do not count`() {
        val d = fixtureP()
        val a = Appearances.of(d, x, hiddenUnlocked = false)
        assertEquals(Appearances.episodes(d).size, a.bars.size)
        val byEpisode = a.bars.associate { it.episodeKey to it.scenes }
        assertEquals(2, byEpisode[K.EPISODE_1_2])
        assertEquals(1, byEpisode[K.EPISODE_1_4])
        assertEquals(0, byEpisode[K.EPISODE_1_5])
        assertEquals(3, a.bars.sumOf { it.scenes })
        assertEquals(2, a.max)
        assertEquals(StoryLabels.episodeCodeOf(d, K.EPISODE_1_4), a.last!!.episodeLabel)
        assertEquals(K.SCENE_1_4_1, a.last!!.sceneKey)
    }

    @Test
    fun `TC-N47-02 a character who never appeared has empty bars and no last appearance`() {
        val d = Fx.addCharacter(doc, x, "테스트", 900)
        val a = Appearances.of(d, x, false)
        assertTrue(a.bars.all { it.scenes == 0 })
        assertEquals(0f, a.share(a.bars.first()))
        assertNull(a.last)
    }

    @Test
    fun `TC-N47-03 hidden scenes count only while hidden items are open, trashed blocks never`() {
        val d = fixtureP()
        val hidden = d.copy(story = d.story.copy(units = d.story.units.map { if (it.key == K.SCENE_1_4_1) it.copy(hidden = true) else it }))
        assertEquals(StoryLabels.episodeCodeOf(d, K.EPISODE_1_2), Appearances.of(hidden, x, false).last!!.episodeLabel)
        assertEquals(StoryLabels.episodeCodeOf(d, K.EPISODE_1_4), Appearances.of(hidden, x, true).last!!.episodeLabel)
        val trashed = Fx.editBlock(d, Fx.key("BLCK", 903)) { it.copy(deletedAt = 5L) }
        assertEquals(0, Appearances.of(trashed, x, false).bars.single { it.episodeKey == K.EPISODE_1_4 }.scenes)
    }

    @Test
    fun `TC-N47-04 오래 안 나옴 puts the longest absent first, ties keep list order, never-seen last`() {
        val last = mapOf("a" to 5, "b" to 1, "c" to 5)
        assertEquals(listOf("b", "a", "c", "d", "e"), Appearances.sortAbsentKeys(listOf("a", "b", "c", "d", "e"), last))
        val d = fixtureP()
        val positions = Appearances.lastPositions(d, false)
        assertEquals(d.manuscript.sceneIndex(K.SCENE_1_4_1), positions[x])
    }

    // --- G-2 · CST-74 ---

    @Test
    fun `TC-G-01 보이스 진행 and the 보이스 header row follow the game tools switch`() {
        assertEquals(6, FieldSettings.screen(gameTools(doc, false), EntityKind.CHARACTER).fixedFields.size)
        assertTrue(FieldSettings.VOICE_PROGRESS in FieldSettings.screen(gameTools(doc, true), EntityKind.CHARACTER).fixedFields)
        val withVoices = doc.cast.voiceLines.map { it.characterKey }.firstOrNull()
        if (withVoices != null) {
            val on = Profile.header(gameTools(doc, true), withVoices, LensPoint.Latest, cover = false).rows.map { it.label }
            val off = Profile.header(gameTools(doc, false), withVoices, LensPoint.Latest, cover = false).rows.map { it.label }
            assertTrue("보이스" in on)
            assertFalse("보이스" in off)
        }
    }
}

/** CST-76: the relation-kind create card's write. */
class RelationKindCard020Test {
    private val doc = Fx.doc()
    private val ctx = Fx.ctx()

    @Test
    fun `TC-U15-08 a filled other-side name makes a pair kind the partner holds, empty makes a symmetric one, 마음 ignores it`() {
        val (pairDoc, pair) = io.github.lenticualrstone.civilight.domain.cast.relation.RelationDrafts.addKindFromCard(
            doc, io.github.lenticualrstone.civilight.domain.cast.relation.SheetSlot.BETWEEN, "사형", " 사제 ", ctx,
        )!!
        val kind = pairDoc.project.relationKinds.single { it.key == pair.kindKey }
        assertEquals("사제", kind.pairName)
        assertEquals(io.github.lenticualrstone.civilight.model.cast.RelationSymmetry.PAIR, kind.symmetry?.known)
        assertEquals(true, pair.partnerHoldsFirst)
        val (symDoc, sym) = io.github.lenticualrstone.civilight.domain.cast.relation.RelationDrafts.addKindFromCard(
            doc, io.github.lenticualrstone.civilight.domain.cast.relation.SheetSlot.BETWEEN, "동문", "", ctx,
        )!!
        assertNull(symDoc.project.relationKinds.single { it.key == sym.kindKey }.pairName)
        val (mindDoc, mind) = io.github.lenticualrstone.civilight.domain.cast.relation.RelationDrafts.addKindFromCard(
            doc, io.github.lenticualrstone.civilight.domain.cast.relation.SheetSlot.VIEWER_MIND, "동경", "무시됨", ctx,
        )!!
        assertNull(mindDoc.project.relationKinds.single { it.key == mind.kindKey }.pairName)
    }
}

/** U-09 in 0.2.0 (§7.9): a template has no game tools switch and holds no voice data. */
class Template020Test {
    @Test
    fun `the template editor's fixed box reads 고정 항목 6 without 보이스 진행`() {
        val template = io.github.lenticualrstone.civilight.model.app.FieldTemplate(key = "t1", name = "내 틀", order = "a")
        val scratch = io.github.lenticualrstone.civilight.domain.cast.field.FieldTemplates.toScratchDoc(template)
        val screen = FieldSettings.screen(scratch, EntityKind.CHARACTER)
        assertEquals("고정 항목 · 6", screen.fixedTitle)
        assertFalse(FieldSettings.VOICE_PROGRESS in screen.fixedFields)
    }
}
