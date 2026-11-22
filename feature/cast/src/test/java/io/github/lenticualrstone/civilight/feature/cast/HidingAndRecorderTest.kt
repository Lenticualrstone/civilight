package io.github.lenticualrstone.civilight.feature.cast

import io.github.lenticualrstone.civilight.domain.cast.CharacterEdits
import io.github.lenticualrstone.civilight.domain.cast.relation.RelationViews
import io.github.lenticualrstone.civilight.feature.cast.list.CastListModel
import io.github.lenticualrstone.civilight.feature.cast.list.CastListUi
import io.github.lenticualrstone.civilight.feature.cast.profile.ProfileModel
import io.github.lenticualrstone.civilight.feature.cast.profile.ProfileUi
import io.github.lenticualrstone.civilight.feature.cast.profile.RecorderLinkModel
import io.github.lenticualrstone.civilight.feature.cast.merge.MergeModel
import io.github.lenticualrstone.civilight.feature.cast.settings.TagSettingsModel
import io.github.lenticualrstone.civilight.model.cast.EntityKind
import io.github.lenticualrstone.civilight.model.doc.ProjectDoc
import io.github.lenticualrstone.civilight.model.sample.ExampleClock
import io.github.lenticualrstone.civilight.model.sample.ExampleProject
import io.github.lenticualrstone.civilight.model.sample.ExampleRecorder
import io.github.lenticualrstone.civilight.ui.security.UnlockState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import io.github.lenticualrstone.civilight.model.sample.ExampleKeys as K

/** 숨기기 from the record (CST-39, rule 19 §6 on cast screens) and `레코더 · 정리 안 됨 n ›` (CST-49). */
class HidingAndRecorderTest {
    private val doc = CastFx.doc()
    private val hidden: ProjectDoc = CharacterEdits.setHidden(doc, K.OWEN, true, CastFx.ctx())

    private fun profile(d: ProjectDoc, unlocked: Boolean) = ProfileModel.build(d, K.OWEN, ProfileUi(), cover = true, hiddenUnlocked = unlocked, readOnly = false)

    @Test
    fun `TC-19-01 hiding 오웬 collapses him into the list's lock row and closes his locked record`() {
        val before = CastListModel.build(doc, CastListUi(), hiddenUnlocked = false, readOnly = false)
        val after = CastListModel.build(hidden, CastListUi(), hiddenUnlocked = false, readOnly = false)
        assertNull(before.view.lockRow)
        assertEquals(1, after.view.lockRow!!.count)
        assertEquals("숨긴 인물 1 · 지문 · PIN으로 열림 · 지문으로 보기", after.view.lockRow!!.text)
        assertNull(profile(hidden, unlocked = false))
    }

    @Test
    fun `TC-19-06 unlocked, the record is in place with the 숨김 tag and the menu offers 숨김 풀기`() {
        val state = profile(hidden, unlocked = true)!!
        assertTrue(state.hidden)
        assertFalse(profile(doc, unlocked = false)!!.hidden)
        val unhidden = CharacterEdits.setHidden(hidden, K.OWEN, false, CastFx.ctx())
        assertNotNull(profile(unhidden, unlocked = false))
    }

    @Test
    fun `TC-19-02 other characters' relations show one 숨긴 인물과의 관계 row`() {
        assertEquals(1, RelationViews.hiddenPartnerCount(hidden, K.RIEN, hiddenUnlocked = false))
        assertEquals(0, RelationViews.hiddenPartnerCount(hidden, K.RIEN, hiddenUnlocked = true))
    }

    @Test
    fun `rule 19 batch-4 surfaces never reveal a locked hidden character`() {
        // A visible twin of hidden 오웬: the same-name row counts only visible characters while locked.
        val twin = CastFx.key("CHAR", 31)
        val withTwin = CastFx.addCharacter(hidden, twin, "오웬", 31)
        val twinState = { unlocked: Boolean -> ProfileModel.build(withTwin, twin, ProfileUi(), cover = true, hiddenUnlocked = unlocked, readOnly = false)!! }
        assertNull(twinState(false).sameName)
        assertEquals("◇ 같은 이름 2 · 오웬", twinState(true).sameName)

        // A merge overlap with a hidden partner reads `숨긴 인물` while locked.
        val mergeView = { unlocked: Boolean -> MergeModel.view(hidden, EntityKind.CHARACTER, K.HART, K.RIEN, emptySet(), hiddenUnlocked = unlocked)!! }
        assertTrue(mergeView(false).overlaps.none { it.label == "오웬" })
        assertEquals(mergeView(true).overlaps.size, mergeView(false).overlaps.size)

        // Tag counts follow the list chips.
        val owenTags = doc.index.character(K.OWEN)!!.tagKeys
        if (owenTags.isNotEmpty()) {
            val tag = owenTags.first()
            val locked = TagSettingsModel.rows(hidden).single { it.key == tag }.count
            val open = TagSettingsModel.rows(hidden, hiddenUnlocked = true).single { it.key == tag }.count
            assertEquals(open - 1, locked)
        }
    }

    @Test
    fun `CST-49 the 개요 link counts unsorted records linked to the character`() {
        val library = ExampleRecorder.library()
        val env = RecorderLinkModel.env(listOf(ExampleProject.libraryEntry()), mapOf(K.PROJECT to doc), UnlockState(), ExampleClock.ZONE)
        val line = RecorderLinkModel.line(library, env, K.PROJECT, K.HART)
        assertTrue(line, line!!.startsWith("레코더 · 정리 안 됨 ") && line.endsWith(" ›"))
        assertNull(RecorderLinkModel.line(library, env, K.PROJECT, CastFx.key("CHAR", 30)))
        assertTrue(RecorderLinkModel.projectsNeeded(library, K.PROJECT, K.HART).contains(K.PROJECT))
    }

    @Test
    fun `CST-49 locked hidden records are not counted`() {
        val library = ExampleRecorder.library()
        val env = RecorderLinkModel.env(listOf(ExampleProject.libraryEntry()), mapOf(K.PROJECT to doc), UnlockState(), ExampleClock.ZONE)
        val hartRecords = library.links.filter { it.targetKey == K.HART }.map { it.recordKey }.toSet()
        val hiddenLib = library.copy(records = library.records.map { if (it.key in hartRecords) it.copy(hidden = true) else it })
        assertNull(RecorderLinkModel.line(hiddenLib, env, K.PROJECT, K.HART))
        val open = RecorderLinkModel.env(listOf(ExampleProject.libraryEntry()), mapOf(K.PROJECT to doc), UnlockState(recordsUnlockedAt = 1L), ExampleClock.ZONE)
        assertEquals(RecorderLinkModel.line(library, env, K.PROJECT, K.HART), RecorderLinkModel.line(hiddenLib, open, K.PROJECT, K.HART))
    }
}
