package io.github.lenticualrstone.civilight.feature.cast

import io.github.lenticualrstone.civilight.domain.cast.merge.MergePick
import io.github.lenticualrstone.civilight.domain.cast.merge.MergeSlot
import io.github.lenticualrstone.civilight.domain.trash.Fixes
import io.github.lenticualrstone.civilight.domain.trash.ItemId
import io.github.lenticualrstone.civilight.feature.cast.merge.MergeModel
import io.github.lenticualrstone.civilight.model.cast.EntityKind
import io.github.lenticualrstone.civilight.model.cast.FieldPayload
import io.github.lenticualrstone.civilight.model.cast.FieldValue
import io.github.lenticualrstone.civilight.model.doc.ProjectDoc
import io.github.lenticualrstone.civilight.model.sample.ExampleRecorder
import io.github.lenticualrstone.civilight.model.trash.TrashReason
import io.github.lenticualrstone.civilight.feature.cast.profile.ProfileModel
import io.github.lenticualrstone.civilight.feature.cast.profile.ProfileUi
import io.github.lenticualrstone.civilight.model.Key
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import io.github.lenticualrstone.civilight.model.sample.ExampleKeys as K

/** 합치기 (CST-45, CST-46, WLD-15): the comparison sheet and the write through `domain:actions`. */
class MergeModelTest {
    private fun profileOf(doc: ProjectDoc, key: Key) = ProfileModel.build(doc, key, ProfileUi(), cover = true, hiddenUnlocked = false, readOnly = false)

    private val kid = CastFx.key("CHAR", 14)

    /** `시장 꼬마` (TC-20-17): 3 of 노아's lines, 나이 `??`, 출생 1189, 성격, its own one-liner; target 노아. */
    private val given: ProjectDoc = run {
        var d = CastFx.addCharacter(
            CastFx.noahLive(), kid, "시장 꼬마", 14,
            fields = mapOf(
                K.FIELD_AGE to FieldValue.UNKNOWN,
                K.FIELD_BIRTH to FieldValue.of(FieldPayload.Date(1189)),
                K.FIELD_PERSONALITY to FieldValue.of(FieldPayload.Texts(listOf("눈치가 빠르고 셈이 정확하다."))),
            ),
            oneLiner = "시장 골목의 아이",
        )
        val lines = d.manuscript.liveBlocks.filter { it.speakerKey == K.NOAH }.take(3).map { it.key }.toSet()
        d.copy(episodes = d.episodes.mapValues { (_, f) -> f.copy(blocks = f.blocks.map { if (it.key in lines) it.copy(speakerKey = kid) else it }) })
    }

    @Test
    fun `TC-20-17 the sheet reads as RCMerge with the target's values by default`() {
        val view = MergeModel.view(given, EntityKind.CHARACTER, kid, K.NOAH, emptySet())!!
        assertEquals("인물 합치기", view.title)
        assertEquals("시장 꼬마 → 노아 · 기본은 노아 값", view.sub)
        assertTrue(view.movedTitle.startsWith("옮길 참조 · "))
        assertTrue(view.movedLine.startsWith("대사 3"))
        assertEquals("시장 꼬마 · 합쳐짐" to "노아 · 남음", view.columns)
        val age = view.cells.single { it.label == "나이" }
        assertFalse(age.fromMerged)
        assertTrue(view.cells.none { it.fromMerged })
        assertEquals("빈 칸 채움" to "출생 · 성격 · 2", view.infoRows.first())
        assertEquals("시장 꼬마 · ‘합침 → 노아’로 휴지통", view.footnote)
        assertEquals("취소", view.cancelLabel)
        assertEquals("합치기", view.confirmLabel)
    }

    @Test
    fun `a picked cell becomes the merged value and maps onto the cast choices`() {
        val view = MergeModel.view(given, EntityKind.CHARACTER, kid, K.NOAH, emptySet())!!
        val age = view.cells.single { it.label == "나이" }
        val picks = MergeModel.pick(emptySet(), age.id, fromMerged = true)
        assertTrue(MergeModel.view(given, EntityKind.CHARACTER, kid, K.NOAH, picks)!!.cells.single { it.id == age.id }.fromMerged)
        assertEquals(MergePick.MERGED, MergeModel.characterChoices(picks).pick(MergeSlot.Field(K.FIELD_AGE)))
        assertEquals(emptySet<String>(), MergeModel.pick(picks, age.id, fromMerged = false))
    }

    @Test
    fun `TC-20-17 합치기 moves the lines, trashes the merged item as 합침 and 되돌리기 brings it all back`() {
        val result = MergeModel.merge(given, EntityKind.CHARACTER, kid, K.NOAH, emptySet(), CastFx.action(), ExampleRecorder.library())!!
        val d = result.doc
        assertTrue(d.manuscript.liveBlocks.none { it.speakerKey == kid })
        assertEquals(TrashReason.MERGED, result.entry.reason.known)
        assertEquals(K.NOAH, result.entry.mergedIntoKey)
        assertTrue(result.toast.message.startsWith("시장 꼬마 → 노아 합침 · 참조 "))
        assertEquals(FieldValue.of(FieldPayload.Date(1189)), d.index.character(K.NOAH)!!.fields[K.FIELD_BIRTH])
        val undone = result.change.revert(d)
        assertEquals(given.cast.characters, undone.cast.characters)
        assertEquals(given.episodes, undone.episodes)
    }

    @Test
    fun `TC-20-18 a relation between the two shows 서로 관계 1 지워짐`() {
        val doc = CastFx.doc()
        val view = MergeModel.view(doc, EntityKind.CHARACTER, K.OWEN, K.RIEN, emptySet())!!
        assertTrue(view.infoRows.contains("서로 관계 1" to "지워짐"))
    }

    @Test
    fun `merging needs two live items of one kind, never the same one`() {
        val doc = CastFx.doc()
        assertFalse(MergeModel.canMerge(doc, EntityKind.CHARACTER, K.RIEN, K.RIEN))
        // 노아 is in the fixture's trash.
        assertFalse(MergeModel.canMerge(doc, EntityKind.CHARACTER, K.NOAH, K.RIEN))
        assertNull(MergeModel.view(doc, EntityKind.CHARACTER, K.NOAH, K.RIEN, emptySet()))
        assertNull(MergeModel.merge(doc, EntityKind.CHARACTER, K.NOAH, K.RIEN, emptySet(), CastFx.action(), null))
        assertFalse(MergeModel.canMerge(doc, EntityKind.PLACE, K.RIEN, K.OWEN))
        assertEquals("인물 합치기", MergeModel.title(EntityKind.CHARACTER))
    }

    @Test
    fun `WLD-15 two places compare through the engine with the place sheet's words`() {
        val doc = CastFx.doc()
        val view = MergeModel.view(doc, EntityKind.PLACE, K.PLACE_UPPER_GATE, K.PLACE_SOCIETY_GATE, emptySet())
        assertNotNull(view)
        assertEquals("장소 합치기", view!!.title)
        assertTrue(view.sub.endsWith("기본은 결사의 문 값") || view.sub.contains(" → "))
        assertTrue(view.footnote.contains("‘합침 → "))
        val merged = MergeModel.merge(doc, EntityKind.PLACE, K.PLACE_UPPER_GATE, K.PLACE_SOCIETY_GATE, emptySet(), CastFx.action(), null)!!
        assertFalse(merged.doc.liveness.isPlaceLive(K.PLACE_UPPER_GATE))
    }

    @Test
    fun `CST-46 the same-name notice and its record row`() {
        val twin = CastFx.key("CHAR", 15)
        val doc = CastFx.addCharacter(CastFx.doc(), twin, "리엔", 15)
        assertEquals("◇ 같은 이름 2 · 리엔", Fixes.sameNameNotice(doc, ItemId.character(K.RIEN)))
        assertEquals("◇ 같은 이름 2 · 리엔", profileOf(doc, K.RIEN)!!.sameName)
        assertNull(profileOf(CastFx.doc(), K.RIEN)!!.sameName)
        assertTrue(MergeModel.canMerge(doc, EntityKind.CHARACTER, twin, K.RIEN))
    }
}
