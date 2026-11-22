package io.github.lenticualrstone.civilight.domain.cast

import io.github.lenticualrstone.civilight.domain.cast.field.FieldDefinitions
import io.github.lenticualrstone.civilight.domain.cast.field.FieldText
import io.github.lenticualrstone.civilight.domain.cast.field.FieldValueEdits
import io.github.lenticualrstone.civilight.domain.cast.field.NewField
import io.github.lenticualrstone.civilight.domain.cast.list.CastList
import io.github.lenticualrstone.civilight.domain.cast.map.RelationMap
import io.github.lenticualrstone.civilight.domain.cast.refs.DeleteImpact
import io.github.lenticualrstone.civilight.domain.cast.refs.DeletedItems
import io.github.lenticualrstone.civilight.domain.cast.refs.RefKind
import io.github.lenticualrstone.civilight.domain.cast.refs.References
import io.github.lenticualrstone.civilight.domain.cast.relation.RelationViews
import io.github.lenticualrstone.civilight.model.Key
import io.github.lenticualrstone.civilight.model.cast.EntityKind
import io.github.lenticualrstone.civilight.model.cast.FieldLinkTargetKind
import io.github.lenticualrstone.civilight.model.cast.FieldPayload
import io.github.lenticualrstone.civilight.model.cast.FieldType
import io.github.lenticualrstone.civilight.model.cast.FieldValue
import io.github.lenticualrstone.civilight.model.doc.ProjectDoc
import io.github.lenticualrstone.civilight.model.doc.recordAt
import io.github.lenticualrstone.civilight.model.position.LensPoint
import io.github.lenticualrstone.civilight.model.sample.ExampleClock
import io.github.lenticualrstone.civilight.model.text.plainText
import io.github.lenticualrstone.civilight.model.trash.NameTagKind
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Ignore
import org.junit.Test
import java.time.LocalDate
import io.github.lenticualrstone.civilight.model.sample.ExampleKeys as K

/**
 * Rule 20 · 지웠을 때 (30-cast.md §4.4), cast's side: impact counts, labels of references to deleted
 * items, the relation tab's dotted rows, check and trash rows, bulk speaker change, restore report,
 * purge with a name tag, same-name notice. Marking and clearing trash rows is `domain:trash`'s job;
 * here a mark is set directly. Merge cases are in [Rule20MergeTest].
 */
class Rule20CastTest {
    private val doc = Fx.doc()
    private val live = Fx.noahLive(doc)
    private val links = Fx.recorderLinks()
    private val today: LocalDate = ExampleClock.localDate(ExampleClock.NOW)
    private val ctx = Fx.ctx()

    private fun withSceneMark(d: ProjectDoc, sceneKey: Key, deletedAt: Long?): ProjectDoc =
        d.copy(story = d.story.copy(units = d.story.units.map { if (it.key == sceneKey) it.copy(deletedAt = deletedAt) else it }))

    @Test
    fun `TC-20-01 impact counts`() {
        val impact = DeleteImpact.of(live, EntityKind.CHARACTER, K.NOAH, links)
        assertEquals(listOf("오웬"), impact.relationPartners)
        assertEquals(2, impact.records)
        assertEquals(0, impact.gameTexts)
        assertEquals(14, impact.speakerLines)
        assertEquals(2, impact.appearances)
        assertEquals(3, impact.links)
        assertEquals(1, impact.recorderChips)
        assertEquals(20, impact.referenceCount)
        assertTrue(impact.needsSheet)
        val sheet = impact.sheet(live)
        assertEquals("‘노아’ 지우기", sheet.title)
        assertEquals("인물 · 참조 20", sheet.sub)
        assertEquals("같이 휴지통으로", sheet.ownedTitle)
        assertEquals(listOf("관계 1", "◆ 변화 기록 2"), sheet.owned.map { it.text })
        assertEquals("오웬", sheet.owned.first().sub)
        assertEquals("제자리에 남음 · 지운 인물로 보임", sheet.staysTitle)
        assertEquals(listOf("대사 화자 14", "씬 등장 2", "@연결 3", "레코더 칩 1"), sheet.stays.map { it.text })
        assertEquals("1-6 씬 2 · 씬 4", sheet.stays.first().sub)
        assertEquals("점검 센터" to "◇ 1줄로 모임 · 한꺼번에 바꾸기", sheet.checkRow)
        assertEquals(Triple("중복이면", "지우지 않고 합치기", "다른 인물과 합치기 ›"), sheet.mergeRow)
        assertEquals("휴지통 · 30일 · 되살리면 다시 이어짐", sheet.footnote)
        assertEquals("취소" to "휴지통으로", sheet.cancelLabel to sheet.confirmLabel)
        // Lines inside the trashed scene 1-6 씬 5 exist but are not counted.
        val inTrashedScene = References.scan(live, EntityKind.CHARACTER, K.NOAH).filter { it.sceneKey == K.SCENE_1_6_5 && it.kind == RefKind.SPEAKER }
        assertTrue(inTrashedScene.isNotEmpty())
        assertTrue(inTrashedScene.none { it.live })
    }

    @Test
    fun `TC-20-02 after delete`() {
        assertEquals("인물 노아 지움 · 참조 20 · 점검 센터 · 되돌리기", DeleteImpact.of(live, EntityKind.CHARACTER, K.NOAH, links).deletedToast)
        // The fixture holds the state after the delete.
        val list = CastList.build(doc, Fx.LATEST)
        assertEquals("전체 11", list.chips.first().label)
        assertTrue(list.rows.none { it.key == K.NOAH })
        assertTrue(RelationMap.build(doc, Fx.LATEST).header.startsWith("인물 · 11명 중"))
        val row = RelationViews.tab(doc, K.OWEN, Fx.LATEST).deletedRows.single()
        assertEquals("노아", row.name)
        assertEquals("◇ 지운 인물 · 관계 1", row.label)
        assertEquals(listOf("되살리기", "이대로 두기"), row.actions)
        // The 14 lines keep their speaker, drawn dotted (never `화자 없음`).
        val lines = Fx.linesOf(doc, K.NOAH)
        assertEquals(14, lines.size)
        val speaker = ItemRefs.character(doc, K.NOAH)
        assertTrue(speaker is ResolvedItem.Trashed)
        assertTrue(speaker.dotted)
        assertEquals("노아", speaker.narrowLabel)
        assertEquals("지운 인물 · 노아", speaker.wideLabel)
    }

    @Test
    fun `TC-20-03 zero references`() {
        val loner = Fx.key("CHAR", 21)
        val d = Fx.addCharacter(doc, loner, "유령", 13)
        val impact = DeleteImpact.of(d, EntityKind.CHARACTER, loner, links)
        assertFalse(impact.needsSheet)
        assertEquals("인물 유령 지움 · 되돌리기", impact.deletedToast)
    }

    @Test
    fun `TC-20-04 check row and bulk change`() {
        val row = requireNotNull(DeletedItems.checkRow(doc, EntityKind.CHARACTER, K.NOAH, today, links))
        assertEquals("◇ 지운 인물 · 노아", row.label)
        assertEquals("휴지통", row.location)
        assertEquals("3일 남음", row.daysLeft)
        assertEquals("대사 14 · 씬 등장 2 · @ 3 · 레코더 1", row.counts)
        assertEquals("화자 한꺼번에 바꾸기", row.actions.first())
        assertTrue(row.actions.containsAll(listOf("되살리기", "이대로 두기")))

        assertEquals("대사 14 · 노아 → 오웬", DeletedItems.speakerPreview(doc, K.NOAH, K.OWEN))
        val (changed, change) = DeletedItems.changeSpeaker(doc, K.NOAH, K.OWEN, ctx)
        assertEquals(14, change.count)
        assertTrue(Fx.linesOf(changed, K.NOAH).isEmpty())
        val undone = DeletedItems.undoSpeakerChange(changed, change)
        assertEquals(Fx.linesOf(doc, K.NOAH).map { it.key }, Fx.linesOf(undone, K.NOAH).map { it.key })

        val eleven = Fx.linesOf(doc, K.NOAH).take(11).map { it.key }.toSet()
        val (partly, _) = DeletedItems.changeSpeaker(doc, K.NOAH, K.OWEN, ctx, onlyBlocks = eleven)
        assertTrue(DeletedItems.checkRow(partly, EntityKind.CHARACTER, K.NOAH, today, links)!!.counts.startsWith("대사 3 · "))
    }

    @Test
    fun `TC-20-05 restore keeps user changes`() {
        val snapshot = DeletedItems.snapshot(live, K.NOAH)
        val three = Fx.linesOf(doc, K.NOAH).take(3).map { it.key }.toSet()
        val (changed, _) = DeletedItems.changeSpeaker(doc, K.NOAH, K.OWEN, ctx, onlyBlocks = three)
        val restored = Fx.restore(changed, K.NOAH)
        val report = DeletedItems.restoreReport(restored, K.NOAH, snapshot)
        assertEquals("노아 되살림 · 다시 이어짐 · 대사 11 · @ 3 · 씬 등장 2 · 그대로 둔 것 · 대사 3", report.toast)
        assertTrue(restored.liveness.isRelationLive(K.REL_OWEN_NOAH))
        assertEquals(3, Fx.linesOf(restored, K.OWEN).count { it.key in three })
    }

    @Test
    fun `TC-20-06 permanent delete`() {
        val purged = DeletedItems.purgeCharacter(doc, K.NOAH, ctx)
        val tag = requireNotNull(purged.index.nameTag(K.NOAH))
        assertEquals("노아", tag.name)
        assertEquals(NameTagKind.CHARACTER, tag.kind.known)
        assertEquals("NOA", tag.voiceCode)
        val ref = ItemRefs.character(purged, K.NOAH)
        assertEquals("영구 삭제 · 노아", ref.narrowLabel)
        assertTrue(ref.dotted)
        // Lines keep the speaker key and the fixed voice ID.
        assertEquals(K.NOAH, purged.index.block(K.BLOCK_1_6_007)!!.speakerKey)
        assertEquals("NOA_0106_007", purged.index.block(K.BLOCK_1_6_007)!!.voice?.fixedId)
        assertNull(purged.index.relation(K.REL_OWEN_NOAH))
        assertNull(purged.index.record(K.N1))
        assertNull(purged.index.record(K.N2))
        val row = requireNotNull(DeletedItems.checkRow(purged, EntityKind.CHARACTER, K.NOAH, today, links))
        assertEquals("영구 삭제 · 노아", row.label)
        assertFalse("되살리기" in row.actions)
        assertNull(row.daysLeft)
    }

    @Test
    fun `TC-20-07 same name on restore`() {
        val newNoah = Fx.key("CHAR", 22)
        val d = Fx.addCharacter(doc, newNoah, "노아", 13)
        assertTrue(DeletedItems.sameNames(d).isEmpty())
        val restored = Fx.restore(d, K.NOAH)
        val notice = DeletedItems.sameNames(restored).single()
        assertEquals("◇ 같은 이름 2 · 노아", notice.label)
        assertEquals("합치기", notice.action)
        assertEquals(setOf(K.NOAH, newNoah), notice.keys.toSet())
        assertEquals(2, CastList.build(restored, Fx.LATEST).rows.count { it.name == "노아" })
    }

    @Test
    fun `TC-20-08 both partners in the trash`() {
        val both = Fx.trash(doc, K.OWEN)
        val onlyNoah = Fx.restore(both, K.NOAH)
        assertFalse(onlyNoah.liveness.isRelationLive(K.REL_OWEN_NOAH))
        val row = RelationViews.tab(onlyNoah, K.NOAH, Fx.LATEST).deletedRows.single()
        assertEquals("오웬", row.name)
        assertEquals("◇ 지운 인물 · 관계 1", row.label)
        val bothBack = Fx.restore(onlyNoah, K.OWEN)
        assertTrue(bothBack.liveness.isRelationLive(K.REL_OWEN_NOAH))
        assertTrue(RelationViews.tab(bothBack, K.NOAH, Fx.LATEST).blocks.any { it.partnerKey == K.OWEN })
    }

    @Test
    fun `TC-20-09 restore before the scene`() {
        val noahBack = Fx.restore(doc, K.NOAH)
        val inScene = References.scan(noahBack, EntityKind.CHARACTER, K.NOAH).filter { it.sceneKey == K.SCENE_1_6_5 }
        assertTrue(inScene.isNotEmpty())
        assertTrue(inScene.none { it.live })
        val sceneBack = withSceneMark(noahBack, K.SCENE_1_6_5, null)
        val after = References.scan(sceneBack, EntityKind.CHARACTER, K.NOAH).filter { it.sceneKey == K.SCENE_1_6_5 }
        assertTrue(after.isNotEmpty())
        assertTrue(after.all { it.live })
        assertTrue(ItemRefs.character(sceneBack, K.NOAH) is ResolvedItem.Live)
    }

    @Test
    fun `TC-20-10 hidden item delete`() {
        val hiddenLive = Fx.hide(live, K.NOAH)
        val sheet = DeleteImpact.of(hiddenLive, EntityKind.CHARACTER, K.NOAH, links).sheet(hiddenLive)
        assertTrue(sheet.stays.all { it.sub == null })
        assertTrue(sheet.owned.all { it.sub == null })
        assertEquals("대사 화자 14", sheet.stays.first().text)

        val hiddenTrashed = Fx.hide(doc, K.NOAH)
        val trashRow = requireNotNull(DeletedItems.trashRow(hiddenTrashed, K.NOAH, today))
        assertEquals("숨긴 인물", trashRow.name)
        assertEquals("인물 · 잿빛 새벽의 등불 · 되살리기는 지문 확인", trashRow.sub)
        val purged = DeletedItems.purgeCharacter(hiddenTrashed, K.NOAH, ctx)
        assertTrue(purged.index.nameTag(K.NOAH)!!.hidden)
        assertEquals("영구 삭제 · 숨긴 항목", ItemRefs.character(purged, K.NOAH).narrowLabel)
        assertEquals("영구 삭제 · 노아", ItemRefs.character(purged, K.NOAH, hiddenUnlocked = true).narrowLabel)
    }

    @Test
    fun `TC-20-10 trash row of a visible character`() {
        val row = requireNotNull(DeletedItems.trashRow(doc, K.NOAH, today))
        assertEquals("노아", row.name)
        assertEquals("3일 남음", row.daysLeft)
        assertEquals("인물 · 잿빛 새벽의 등불 · 대사 14 · 관계 1", row.sub)
        assertEquals(listOf("되살리기", "영구 삭제"), row.chips)
    }

    @Test
    fun `TC-20-11 several at once`() {
        val impacts = listOf(K.NOAH, K.OWEN, K.GORDON).map { DeleteImpact.of(live, EntityKind.CHARACTER, it, links) }
        val lines = impacts.sumOf { it.speakerLines }
        assertTrue(DeleteImpact.combinedSummary(impacts).startsWith("인물 3 · 대사 $lines · "))
    }

    @Test
    fun `TC-20-12 plain text untouched`() {
        val page = live.pages.getValue(K.PAGE_LOWER_LIGHT)
        assertTrue(page.blocks.any { "노아 시점으로?" in it.runs.plainText })
        val onPage = References.scan(live, EntityKind.CHARACTER, K.NOAH).filter { it.ownerKey == K.PAGE_LOWER_LIGHT }
        assertEquals(1, onPage.size)
        assertEquals(RefKind.LINK, onPage.single().kind)
    }

    @Ignore("spec conflict: TC-20-13 needs 소속 as `연결 · 세력`; the fixture's 소속 is `짧은 글` (fixture.md conflict #10)")
    @Test
    fun `TC-20-13 faction delete as stated`() {
        val affiliation = Fx.def(doc, "소속")
        assertEquals(FieldType.LINK, affiliation.type.known)
    }

    @Test
    fun `TC-20-13 faction delete`() {
        val (withDef, def) = FieldDefinitions.add(doc, NewField("소속", FieldType.LINK, linkTargetKind = FieldLinkTargetKind.FACTION), ctx)
        val guard = FieldValue.of(FieldPayload.Link(listOf(K.FACTION_UPPER_GUARD)))
        var d = FieldValueEdits.setBase(withDef, EntityKind.CHARACTER, K.KAIL, def.key, guard, ctx)
        d = FieldValueEdits.setBase(d, EntityKind.CHARACTER, K.IRENA, def.key, guard, ctx)
        d = d.copy(world = d.world.copy(factions = d.world.factions.map { if (it.key == K.FACTION_UPPER_GUARD) it.copy(deletedAt = ExampleClock.NOW) else it }))
        for (who in listOf(K.KAIL, K.IRENA)) {
            val display = FieldText.format(d, def, d.index.character(who)!!.fields[def.key])
            assertEquals("지운 세력 · 상층 경비대", display.text)
            assertTrue(display.dotted)
            assertEquals(listOf("다른 항목 고르기", "비우기", "되살리기", "이대로 두기"), FieldText.linkFixPaths(display.refs.single()))
        }
        assertEquals(2, References.scan(d, EntityKind.FACTION, K.FACTION_UPPER_GUARD).count { it.kind == RefKind.FIELD_LINK })
        // `비우기` empties the value; `다른 항목 고르기` re-points it.
        val cleared = FieldValueEdits.removeLinkTarget(d, EntityKind.CHARACTER, K.KAIL, def.key, K.FACTION_UPPER_GUARD, ctx)
        assertNull(cleared.index.character(K.KAIL)!!.fields[def.key])
        val moved = FieldValueEdits.replaceLinkTarget(d, EntityKind.CHARACTER, K.IRENA, def.key, K.FACTION_UPPER_GUARD, K.FACTION_LANTERN_SOCIETY, ctx)
        assertEquals("등불 결사", FieldText.format(moved, def, moved.index.character(K.IRENA)!!.fields[def.key]).text)
    }

    @Test
    fun `TC-20-14 place delete, cast side`() {
        val d = doc.copy(world = doc.world.copy(places = doc.world.places.map { if (it.key == K.PLACE_ASH_MARKET) it.copy(deletedAt = ExampleClock.NOW) else it }))
        val ref = ItemRefs.resolve(d, EntityKind.PLACE, K.PLACE_ASH_MARKET)
        assertEquals("지운 장소 · 잿더미 시장", ref.wideLabel)
        assertTrue(ref.dotted)
        assertTrue(References.scan(d, EntityKind.PLACE, K.PLACE_ASH_MARKET).any { it.kind == RefKind.SCENE_PLACE && it.live })
        assertTrue(ItemRefs.resolve(doc, EntityKind.PLACE, K.PLACE_ASH_MARKET) is ResolvedItem.Live)
    }

    @Test
    fun `TC-20-15 term delete holds the warning`() {
        val impact = DeleteImpact.of(doc, EntityKind.TERM, K.TERM_ORIGINIUM_LANTERN, links)
        assertEquals(1, impact.heldWarnings)
        assertTrue("▲ 공개 전 사용 1" in impact.deletedToast)
        assertTrue(impact.deletedToast.startsWith("용어 오리지늄 등불 지움 · 참조 "))
        assertEquals(1, impact.records)
    }

    @Test
    fun `TC-20-16 deleted item invisible at any lens`() {
        for (lens in listOf(Fx.L1_6, Fx.LATEST)) {
            assertTrue(CastList.build(doc, lens).rows.none { it.key == K.NOAH })
            assertTrue(RelationMap.build(doc, lens).nodes.none { it.key == K.NOAH })
            assertTrue(CastList.search(doc, "노아", lens).rows.isEmpty())
        }
        // Dotted references do not depend on the lens.
        assertTrue(ItemRefs.character(doc, K.NOAH).dotted)
    }

    @Test
    fun `TC-20-21 voice code kept while deleted`() {
        assertEquals("NOA", doc.index.character(K.NOAH)!!.voiceCode)
        assertEquals("NOA", DeletedItems.purgeCharacter(doc, K.NOAH, ctx).index.nameTag(K.NOAH)!!.voiceCode)
    }

    @Test
    fun `TC-20-23 record whose target is deleted`() {
        val target = doc.index.record(K.N1)!!.target
        // In the trash: the record is kept with 노아 and not live.
        assertNotNull(doc.index.record(K.N1))
        assertFalse(doc.liveness.isLive(doc.index.record(K.N1)!!))
        assertNull(doc.recordAt(target, Fx.LATEST))
        // Restored: solid again.
        val restored = Fx.restore(doc, K.NOAH)
        assertTrue(restored.liveness.isLive(restored.index.record(K.N1)!!))
        assertEquals(K.N1, restored.recordAt(target, LensPoint.Latest)?.key)
        // Permanently deleted: deleted with it.
        assertNull(DeletedItems.purgeCharacter(doc, K.NOAH, ctx).index.record(K.N1))
    }
}
