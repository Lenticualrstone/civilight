package io.github.lenticualrstone.civilight.domain.trash

import io.github.lenticualrstone.civilight.model.OrderKeys
import io.github.lenticualrstone.civilight.model.cast.FieldDef
import io.github.lenticualrstone.civilight.model.cast.FieldLinkTargetKind
import io.github.lenticualrstone.civilight.model.cast.FieldPayload
import io.github.lenticualrstone.civilight.model.cast.FieldType
import io.github.lenticualrstone.civilight.model.cast.FieldValue
import io.github.lenticualrstone.civilight.model.cast.EntityKind
import io.github.lenticualrstone.civilight.model.checks.CheckType
import io.github.lenticualrstone.civilight.model.doc.ProjectFileSet
import io.github.lenticualrstone.civilight.model.doc.isLinkBeforeReveal
import io.github.lenticualrstone.civilight.model.doc.liveCharacters
import io.github.lenticualrstone.civilight.model.open
import io.github.lenticualrstone.civilight.model.sample.ExampleKeys as K
import io.github.lenticualrstone.civilight.model.script.VoiceMode
import io.github.lenticualrstone.civilight.model.text.TextRun
import io.github.lenticualrstone.civilight.model.text.docText
import io.github.lenticualrstone.civilight.model.trash.NameTagKind
import io.github.lenticualrstone.civilight.model.trash.NameTagState
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Ignore
import org.junit.Test

/** 30-cast §4.4 rule 20 cases (TC-20-01 … TC-20-16, TC-20-20 … TC-20-25, TC-20-28) on the example project. */
class Rule20CastTest {
    private val noah = ItemId.character(K.NOAH)

    @Test
    fun `TC-20-01 impact counts (노아)`() {
        val doc = Fx.noahLive()
        val impact = Impact.of(doc, noah, Fx.recorder())
        assertEquals(1, impact.owned(OwnedKind.RELATION))
        assertEquals(listOf("오웬"), impact.relationPartners)
        assertEquals(2, impact.owned(OwnedKind.CHANGE_RECORD))
        assertEquals(0, impact.owned(OwnedKind.GAME_TEXT))
        assertEquals(14, impact.count(RefKind.SPEAKER))
        assertEquals(2, impact.count(RefKind.SILENT_CAST))
        assertEquals(3, impact.count(RefKind.LINK))
        assertEquals(1, impact.count(RefKind.RECORDER_LINK))
        assertEquals(20, impact.referenceTotal)
        assertEquals("1-6 씬 2 · 씬 4", impact.speakerLocations)
        val sheet = Messages.impactSheet(impact)
        assertEquals("‘노아’ 지우기", sheet.title)
        assertEquals("인물 · 참조 20", sheet.sub)
        assertEquals("같이 휴지통으로", sheet.ownedTitle)
        assertEquals("관계 1 · ◆ 변화 기록 2", sheet.ownedLine)
        assertEquals("오웬", sheet.ownedRows.first().sub)
        assertEquals("제자리에 남음 · 지운 인물로 보임", sheet.stayingTitle)
        assertEquals("대사 화자 14 · 씬 등장 2 · @연결 3 · 레코더 칩 1", sheet.stayingLine)
        assertEquals("1-6 씬 2 · 씬 4", sheet.stayingRows.first().sub)
        assertEquals("다른 인물과 합치기 ›", sheet.mergeAction)
        assertEquals("휴지통 · 30일 · 되살리면 다시 이어짐", sheet.footnote)
        assertEquals(listOf("취소", "휴지통으로"), sheet.buttons)
        // More 노아 lines sit in the trashed 1-6 씬 5; they are not counted.
        val scene5 = Fx.blocksOfScene(doc, K.SCENE_1_6_5)
        assertTrue(scene5.any { it.speakerKey == K.NOAH })
        assertTrue(impact.references.none { site -> scene5.any { it.key == site.ownerKey } })
        assertTrue(impact.needsSheet)
    }

    @Test
    fun `TC-20-02 after delete`() {
        val doc = Fx.noahLive()
        val impact = Impact.of(doc, noah, Fx.recorder())
        val result = TrashOps.delete(doc, noah, Fx.ctx())
        assertEquals("인물 노아 지움 · 참조 20 · 점검 센터 · 되돌리기", Messages.deleteToast(impact).text)
        val after = result.doc
        assertEquals(11, after.liveCharacters.size)
        assertTrue(after.liveCharacters.none { it.key == K.NOAH })
        // 오웬's relation tab: the relation is not live, 노아 shows as a dotted deleted character.
        assertFalse(after.liveness.isRelationLive(K.REL_OWEN_NOAH))
        val partner = Resolution.resolve(after, RefTargetKind.CHARACTER, K.NOAH)
        assertEquals(RefState.TRASHED, partner.state)
        assertEquals("지운 인물 · 노아", partner.label)
        assertTrue(partner.dotted)
        // The 14 lines keep 노아 (never 화자 없음); exports still print the name.
        val lines = Fx.linesOf(after, K.NOAH)
        assertEquals(14, lines.size)
        assertTrue(lines.all { it.speakerKey == K.NOAH })
        assertEquals("노아", partner.exportName)
        assertEquals("노아", partner.name)
    }

    @Test
    fun `TC-20-03 zero references`() {
        val key = Fx.key("CHAR", 1)
        val doc = Fx.addCharacter(Fx.doc(), key, "하린", 13)
        val item = ItemId.character(key)
        val impact = Impact.of(doc, item, Fx.recorder())
        assertFalse(impact.needsSheet)
        val result = TrashOps.delete(doc, item, Fx.ctx())
        assertEquals("인물 하린 지움 · 되돌리기", Messages.deleteToast(impact).text)
        assertFalse(result.doc.liveness.isCharacterLive(key))
    }

    @Test
    fun `TC-20-04 check row and bulk change`() {
        val doc = Fx.doc() // 노아 in the trash since 2026-10-26 (TC-20-02 state)
        val ctx = Fx.ctx()
        val rows = DeletedItems.rows(doc, ctx, Fx.recorder())
        assertEquals(1, rows.size)
        assertEquals("◇ 지운 인물 · 노아 · 3일 남음 · 대사 14 · 씬 등장 2 · @ 3 · 레코더 1", rows.single().line)
        assertEquals("휴지통", rows.single().location)
        assertEquals(listOf("화자 한꺼번에 바꾸기", "@ 한꺼번에 바꾸기", "되살리기", "이대로 두기"), rows.single().actions)

        val all = Fixes.replaceSpeaker(doc, K.NOAH, K.OWEN, ctx)
        assertEquals(14, all.count)
        assertEquals("대사 14 · 노아 → 오웬", Fixes.replaceSpeakerPreview(all.count, "노아", "오웬"))
        assertTrue(Fx.linesOf(all.doc, K.NOAH).isEmpty())
        // One undo restores them.
        assertEquals(doc, all.change.revert(all.doc))

        val eleven = Fx.linesOf(doc, K.NOAH).take(11).map { it.key }.toSet()
        val some = Fixes.replaceSpeaker(doc, K.NOAH, K.OWEN, ctx, eleven)
        assertEquals(11, some.count)
        val row = DeletedItems.rows(some.doc, ctx, Fx.recorder()).single()
        assertEquals(3, row.counts[RefKind.SPEAKER])
        assertTrue(row.line.contains("대사 3"))
    }

    @Test
    fun `TC-20-05 restore keeps user changes`() {
        val ctx = Fx.ctx()
        val deleted = TrashOps.delete(Fx.noahLive(), noah, ctx)
        val three = Fx.linesOf(deleted.doc, K.NOAH).take(3).map { it.key }.toSet()
        val changed = Fixes.replaceSpeaker(deleted.doc, K.NOAH, K.OWEN, ctx, three)
        val entry = deleted.entries.single()
        val restored = Fx.restored(RestoreOps.restore(changed.doc, entry.key, ctx))
        assertEquals(11, Fx.linesOf(restored.doc, K.NOAH).size)
        assertTrue(three.all { restored.doc.index.block(it)!!.speakerKey == K.OWEN })
        assertEquals(
            "노아 되살림 · 다시 이어짐 · 대사 11 · @ 3 · 씬 등장 2 · 그대로 둔 것 · 대사 3",
            Messages.restoreToast(restored.report).text,
        )
        assertTrue(restored.doc.liveness.isRelationLive(K.REL_OWEN_NOAH))
    }

    @Test
    fun `TC-20-06 permanent delete`() {
        val doc = Fx.doc()
        assertEquals("NOA_0106_007", doc.index.block(K.BLOCK_1_6_007)!!.voice!!.fixedId)
        val result = PurgeOps.purge(doc, K.TRASH_NOAH, Fx.ctx())
        val after = result.doc
        val tag = after.index.nameTag(K.NOAH)!!
        assertEquals("노아", tag.name)
        assertEquals(NameTagKind.CHARACTER, tag.kind.known)
        assertEquals("NOA", tag.voiceCode)
        assertEquals(NameTagState.PURGED, tag.state.known)
        val ref = Resolution.resolve(after, RefTargetKind.CHARACTER, K.NOAH)
        assertEquals(RefState.TAGGED, ref.state)
        assertEquals("영구 삭제 · 노아", ref.label)
        assertFalse(ref.canRestore)
        // Lines keep the speaker (never 화자 없음) and the fixed voice ID is unchanged.
        val line = after.index.block(K.BLOCK_1_6_007)!!
        assertEquals(K.NOAH, line.speakerKey)
        assertEquals("NOA_0106_007", line.voice!!.fixedId)
        assertEquals(VoiceMode.FIXED, line.voice!!.mode.known)
        // The relation with 오웬 is gone.
        assertNull(after.index.relation(K.REL_OWEN_NOAH))
        // The check row reads 영구 삭제 · 노아 without 되살리기.
        val row = DeletedItems.rows(after, Fx.ctx(), Fx.recorder()).single()
        assertTrue(row.line.startsWith("영구 삭제 · 노아"))
        assertFalse(TrashCopy.RESTORE in row.actions)
    }

    @Test
    fun `TC-20-07 same name on restore`() {
        val newNoah = Fx.key("CHAR", 2)
        val doc = Fx.addCharacter(Fx.doc(), newNoah, "노아", 13)
        val restored = Fx.restored(RestoreOps.restore(doc, K.TRASH_NOAH, Fx.ctx()))
        assertTrue(restored.doc.liveness.isCharacterLive(K.NOAH))
        assertTrue(restored.doc.liveness.isCharacterLive(newNoah))
        assertEquals("◇ 같은 이름 2 · 노아", Fixes.sameNameNotice(restored.doc, noah))
        assertNull(Fixes.sameNameNotice(doc, ItemId.character(newNoah)))
    }

    @Test
    fun `TC-20-08 both partners in the trash`() {
        val ctx = Fx.ctx()
        val a = TrashOps.delete(Fx.noahLive(), noah, ctx)
        val b = TrashOps.delete(a.doc, ItemId.character(K.OWEN), ctx)
        val noahBack = Fx.restored(RestoreOps.restore(b.doc, a.entries.single().key, ctx)).doc
        assertFalse(noahBack.liveness.isRelationLive(K.REL_OWEN_NOAH))
        assertEquals(RefState.TRASHED, Resolution.resolve(noahBack, RefTargetKind.CHARACTER, K.OWEN).state)
        val both = Fx.restored(RestoreOps.restore(noahBack, b.entries.single().key, ctx)).doc
        assertTrue(both.liveness.isRelationLive(K.REL_OWEN_NOAH))
    }

    @Test
    fun `TC-20-09 restore before the scene`() {
        val ctx = Fx.ctx()
        val doc = Fx.doc() // 노아 and 1-6 씬 5 both in the trash
        val sceneLines = Fx.blocksOfScene(doc, K.SCENE_1_6_5).filter { it.speakerKey == K.NOAH }
        assertTrue(sceneLines.isNotEmpty())
        val noahBack = Fx.restored(RestoreOps.restore(doc, K.TRASH_NOAH, ctx)).doc
        assertTrue(sceneLines.none { noahBack.liveness.isBlockLive(it.key) })
        val sceneBack = Fx.restored(RestoreOps.restore(noahBack, K.TRASH_SCENE_1_6_5, ctx)).doc
        assertTrue(sceneLines.all { sceneBack.liveness.isBlockLive(it.key) })
        assertTrue(sceneLines.all { Resolution.resolve(sceneBack, RefTargetKind.CHARACTER, it.speakerKey!!).state == RefState.LIVE })
    }

    @Test
    fun `TC-20-10 hidden item delete`() {
        val ctx = Fx.ctx()
        val base = Fx.noahLive()
        val doc = base.copy(cast = base.cast.copy(characters = base.cast.characters.map { if (it.key == K.NOAH) it.copy(hidden = true) else it }))
        val impact = Impact.of(doc, noah, Fx.recorder())
        assertTrue(impact.hidden)
        assertNull(impact.speakerLocations)
        assertNull(Messages.impactSheet(impact).stayingRows.first().sub)
        val result = TrashOps.delete(doc, noah, ctx)
        assertTrue(result.entries.single().hiddenAtDelete)
        val row = TrashListing.projectItemRows(result.doc, ctx).single { it.entryKey == result.entries.single().key }
        assertEquals("숨긴 인물", row.title)
        assertEquals("인물 · 잿빛 새벽의 등불 · 되살리기는 지문 확인", row.sub)
        assertTrue(row.restoreNeedsAuth)
        assertEquals(RestoreOutcome.NeedsAuth, RestoreOps.restore(result.doc, result.entries.single().key, ctx))
        val purged = PurgeOps.purge(result.doc, result.entries.single().key, ctx).doc
        assertEquals("영구 삭제 · 숨긴 항목", Resolution.resolve(purged, RefTargetKind.CHARACTER, K.NOAH).label)
    }

    @Test
    fun `TC-20-11 several at once`() {
        val doc = Fx.noahLive()
        val items = listOf(noah, ItemId.character(K.OWEN), ItemId.character(K.GORDON))
        val impact = Impact.of(doc, items, Fx.recorder())
        val lines = items.sumOf { Impact.of(doc, it).count(RefKind.SPEAKER) }
        val sheet = Messages.impactSheet(impact)
        // The board's `대사 40` is sample data; the fixture's three characters speak [lines] lines.
        assertTrue(sheet.sub, sheet.sub.startsWith("인물 3 · 대사 $lines"))
        val result = TrashOps.delete(doc, items, Fx.ctx())
        assertEquals(3, result.entries.size)
        assertTrue(items.none { result.doc.liveness.isCharacterLive(it.key) })
    }

    @Test
    fun `TC-20-12 plain text untouched`() {
        val doc = Fx.noahLive()
        val page = doc.pages.getValue(K.PAGE_LOWER_LIGHT)
        assertTrue(page.blocks.docText.contains("노아 시점으로?"))
        val after = TrashOps.delete(doc, noah, Fx.ctx()).doc
        assertEquals(page.blocks, after.pages.getValue(K.PAGE_LOWER_LIGHT).blocks)
        // The page counts once, for its @노아 link; the memo text is not a reference.
        val pageRefs = Impact.of(after, noah).references.filter { it.containerKey == K.PAGE_LOWER_LIGHT }
        assertEquals(1, pageRefs.size)
        assertEquals(RefKind.LINK, pageRefs.single().kind)
    }

    @Ignore("spec conflict: fixture.md #10 makes 소속 a `짧은 글` field, so no character links 상층 경비대 as 소속; see the synthetic variant")
    @Test
    fun `TC-20-13 faction delete`() {
        val doc = Fx.doc()
        val faction = ItemId.faction(K.FACTION_UPPER_GUARD)
        val impact = Impact.of(doc, faction)
        val affiliation = impact.references.filter { it.kind == RefKind.FIELD_LINK && it.slot == K.FIELD_AFFILIATION }
        assertEquals(2, affiliation.size)
    }

    @Test
    fun `TC-20-13 faction delete (synthetic 연결 field)`() {
        val defKey = Fx.key("FDEF", 1)
        var doc = Fx.doc()
        val def = FieldDef(
            key = defKey, entityKind = EntityKind.CHARACTER.open(), name = "소속 세력", type = FieldType.LINK.open(),
            order = OrderKeys.after(doc.project.fieldDefs.maxOf { it.order }), linkTargetKind = FieldLinkTargetKind.FACTION.open(),
        )
        val value = FieldValue.of(FieldPayload.Link(listOf(K.FACTION_UPPER_GUARD)))
        doc = doc.copy(
            project = doc.project.copy(fieldDefs = doc.project.fieldDefs + def),
            cast = doc.cast.copy(characters = doc.cast.characters.map { if (it.key == K.KAIL || it.key == K.IRENA) it.copy(fields = it.fields + (defKey to value)) else it }),
        )
        val faction = ItemId.faction(K.FACTION_UPPER_GUARD)
        val after = TrashOps.delete(doc, faction, Fx.ctx()).doc
        val refs = Impact.of(after, faction).references.filter { it.kind == RefKind.FIELD_LINK && it.slot == defKey }
        assertEquals(setOf(K.KAIL, K.IRENA), refs.map { it.ownerKey }.toSet())
        val resolved = Resolution.resolve(after, RefTargetKind.FACTION, K.FACTION_UPPER_GUARD)
        assertEquals("지운 세력 · 상층 경비대", resolved.label)
        assertTrue(resolved.dotted)
        assertTrue(resolved.canRestore)
        val row = DeletedItems.rows(after, Fx.ctx()).single { it.item == faction }
        assertTrue(row.actions.containsAll(listOf("연결 한꺼번에 바꾸기", "되살리기", "이대로 두기")))
    }

    @Test
    fun `TC-20-14 place delete`() {
        val ctx = Fx.ctx()
        val doc = Fx.doc()
        val place = ItemId.place(K.PLACE_ASH_MARKET)
        val pinBefore = doc.index.pin(K.PIN_ASH_MARKET)!!.second
        val result = TrashOps.delete(doc, place, ctx)
        val after = result.doc
        // The scene shows 지운 장소 · 잿더미 시장 and leaves travel checks (its place is not live).
        val scenePlace = after.index.unit(K.SCENE_1_6_1)!!.place!!
        val resolved = Resolution.resolve(after, scenePlace)!!
        assertEquals("지운 장소 · 잿더미 시장", resolved.label)
        assertEquals(RefState.TRASHED, resolved.state)
        // The pin is in the trash with the place.
        assertFalse(after.liveness.isOverlayLive(K.MAP_LOWER_DISTRICT, pinBefore))
        // The route stays, with a dotted end on that pin.
        val route = after.maps.getValue(K.MAP_LOWER_DISTRICT).routes.single { it.key == K.ROUTE_GATE_TO_MARKET }
        assertTrue(route.points.any { it.pinKey == K.PIN_ASH_MARKET })
        // The sub-map stays; its breadcrumb names the deleted place.
        val subMap = after.maps.getValue(K.MAP_ASH_MARKET)
        assertTrue(after.liveness.isMapLive(subMap.key))
        val parentPlace = after.index.pin(subMap.parent!!.pinKey)!!.second.placeKey
        assertEquals("지운 장소 · 잿더미 시장", Resolution.resolve(after, RefTargetKind.PLACE, parentPlace).label)
        val impact = Impact.of(after, place)
        assertEquals(1, impact.count(RefKind.ROUTE_END))
        assertEquals(1, impact.count(RefKind.SUB_MAP))
        assertEquals(1, impact.owned(OwnedKind.PIN))
        // Restore: the pin returns at the same 0–1 coordinates.
        val back = Fx.restored(RestoreOps.restore(after, result.entries.single().key, ctx)).doc
        val pin = back.index.pin(K.PIN_ASH_MARKET)!!.second
        assertTrue(back.liveness.isOverlayLive(K.MAP_LOWER_DISTRICT, pin))
        assertEquals(pinBefore.x, pin.x)
        assertEquals(pinBefore.y, pin.y)
        assertEquals(RefState.LIVE, Resolution.resolve(back, scenePlace)!!.state)
    }

    @Test
    fun `TC-20-15 term delete holds the warning`() {
        val ctx = Fx.ctx()
        val doc = Fx.doc()
        val term = ItemId.term(K.TERM_ORIGINIUM_LANTERN)
        assertTrue(doc.isLinkBeforeReveal(K.TERM_ORIGINIUM_LANTERN, K.BLOCK_B42))
        val impact = Impact.of(doc, term)
        assertEquals(1, impact.heldWarnings)
        assertEquals(1, impact.owned(OwnedKind.FIRST_REVEAL))
        assertTrue(Messages.deleteToast(impact).text.contains("▲ 공개 전 사용 1"))
        val result = TrashOps.delete(doc, term, ctx)
        val after = result.doc
        // The ◇ record is kept with the term (no mark of its own) and #044 shows a dotted 대상 지워짐.
        val reveal = after.index.record(K.R9)!!
        assertNull(reveal.deletedAt)
        assertFalse(after.liveness.isLive(reveal))
        assertEquals(listOf(K.R9), DerivedMarks.targetDeleted(after, K.BLOCK_044).map { it.key })
        assertEquals(listOf("되살리기", "이대로 두기", "기록 지우기"), DerivedMarks.targetDeletedActions)
        // The check row holds the ▲.
        val row = DeletedItems.rows(after, ctx).single { it.item == term }
        assertEquals(1, row.heldWarnings)
        assertTrue(row.body.contains("▲ 1 보류"))
        // Restored: the ▲ returns to 1-4 씬 2.
        val back = Fx.restored(RestoreOps.restore(after, result.entries.single().key, ctx)).doc
        assertTrue(back.isLinkBeforeReveal(K.TERM_ORIGINIUM_LANTERN, K.BLOCK_B42))
        assertTrue(DerivedMarks.targetDeleted(back, K.BLOCK_044).isEmpty())
    }

    @Test
    fun `TC-20-16 deleted item invisible at any lens`() {
        val doc = Fx.doc() // 노아 in the trash
        assertTrue(doc.liveCharacters.none { it.key == K.NOAH })
        // References resolve without a lens: dotted at every lens.
        assertEquals(RefState.TRASHED, Resolution.resolve(doc, RefTargetKind.CHARACTER, K.NOAH).state)
        // The picker lists no 노아 except the top row when the current block's speaker is 노아.
        assertEquals("지운 인물 · 노아 · 되살리기", Fixes.deletedSpeakerPickerRow(doc, K.BLOCK_1_6_007))
        assertNull(Fixes.deletedSpeakerPickerRow(doc, K.BLOCK_043))
    }

    @Test
    fun `TC-20-20 trash filter`() {
        val ctx = Fx.ctx()
        val after = TrashOps.delete(Fx.doc(), ItemId.place(K.PLACE_ASH_MARKET), ctx).doc
        val rows = TrashListing.projectItemRows(after, ctx, recorder = Fx.recorder())
        val chips = TrashListing.chips(rows).map { it.substringBefore(" ") }
        assertEquals(listOf("전체", "프로젝트", "씬", "블록", "인물", "세계", "변화", "레코더"), chips)
        assertEquals("전체 ${rows.size}", TrashListing.chips(rows).first())
        assertTrue(TrashListing.list(rows, TrashFilter.WORLD).any { it.title == "잿더미 시장" })
        assertTrue(TrashListing.list(rows, TrashFilter.CHARACTER).none { it.title == "잿더미 시장" })
    }

    @Test
    fun `TC-20-21 automatic voice ID while deleted`() {
        val doc = Fx.doc()
        val line = Fx.linesOf(doc, K.NOAH).first { it.voice == null && doc.index.numberingUnitKeyOfScene(it.sceneKey) == K.EPISODE_1_6 }
        val code = Resolution.voiceCodeOf(doc, K.NOAH)
        assertEquals("NOA", code)
        val id = "${code}_0106_${line.number.toString().padStart(3, '0')}"
        assertTrue(id.startsWith("NOA_0106_"))
        // Still NOA after the purge (from the name tag); never cleared or swapped.
        val purged = PurgeOps.purge(doc, K.TRASH_NOAH, Fx.ctx()).doc
        assertEquals("NOA", Resolution.voiceCodeOf(purged, K.NOAH))
        assertEquals("NOA", Resolution.resolve(purged, RefTargetKind.CHARACTER, K.NOAH).voiceCode)
    }

    @Test
    fun `TC-20-22 글로 풀기`() {
        val ctx = Fx.ctx()
        val doc = Fx.doc()
        assertEquals(3, Impact.of(doc, noah).count(RefKind.LINK))
        val unlinked = Fixes.unlink(doc, K.link(7), ctx).doc
        val page = unlinked.pages.getValue(K.PAGE_LOWER_LIGHT)
        assertTrue(page.blocks.docText.contains("시장 아이들 중 노아만 빛돌 시세를 외운다."))
        assertTrue(page.blocks.flatMap { it.runs }.none { it is TextRun.Link && it.linkKey == K.link(7) })
        assertEquals(2, Impact.of(unlinked, noah).count(RefKind.LINK))
        val back = Fx.restored(RestoreOps.restore(unlinked, K.TRASH_NOAH, ctx)).doc
        assertTrue(back.pages.getValue(K.PAGE_LOWER_LIGHT).blocks.flatMap { it.runs }.none { it is TextRun.Link && it.target.key == K.NOAH })
    }

    @Test
    fun `TC-20-23 record whose target is deleted`() {
        val ctx = Fx.ctx()
        val deleted = TrashOps.delete(Fx.noahLive(), noah, ctx)
        val n1 = deleted.doc.index.record(K.N1)!!
        assertNull(n1.deletedAt)
        assertFalse(deleted.doc.liveness.isLive(n1))
        assertTrue(DerivedMarks.targetDeleted(deleted.doc, K.BLOCK_1_6_007).any { it.key == K.N1 })
        val back = Fx.restored(RestoreOps.restore(deleted.doc, deleted.entries.single().key, ctx)).doc
        assertTrue(back.liveness.isLive(back.index.record(K.N1)!!))
        assertTrue(DerivedMarks.targetDeleted(back, K.BLOCK_1_6_007).isEmpty())
        val purged = PurgeOps.purge(deleted.doc, deleted.entries.single().key, ctx).doc
        assertNull(purged.index.record(K.N1))
    }

    @Test
    fun `TC-20-24 restore a place whose map is gone`() {
        val ctx = Fx.ctx()
        val place = TrashOps.delete(Fx.doc(), ItemId.place(K.PLACE_ASH_MARKET), ctx)
        val map = TrashOps.delete(place.doc, ItemId.map(K.MAP_LOWER_DISTRICT), ctx)
        val back = Fx.restored(RestoreOps.restore(map.doc, place.entries.single().key, ctx)).doc
        assertTrue(back.liveness.isPlaceLive(K.PLACE_ASH_MARKET))
        val status = DerivedMarks.pinsOf(back, K.PLACE_ASH_MARKET).single { it.mapKey == K.MAP_LOWER_DISTRICT }
        assertFalse(status.mapLive)
        assertEquals("◇ 지도 없음", status.mark)
        assertEquals(listOf("지도 되살리기", "핀 다시 놓기"), status.actions)
        val mapBack = Fx.restored(RestoreOps.restore(back, map.entries.single().key, ctx)).doc
        val pin = mapBack.index.pin(K.PIN_ASH_MARKET)!!.second
        assertTrue(mapBack.liveness.isOverlayLive(K.MAP_LOWER_DISTRICT, pin))
        assertEquals(Fx.doc().index.pin(K.PIN_ASH_MARKET)!!.second, pin)
    }

    @Test
    fun `TC-20-25 sub-map after permanent delete`() {
        val ctx = Fx.ctx()
        val deleted = TrashOps.delete(Fx.doc(), ItemId.place(K.PLACE_ASH_MARKET), ctx)
        assertFalse(DerivedMarks.isTopLevelMap(deleted.doc, K.MAP_ASH_MARKET))
        val purged = PurgeOps.purge(deleted.doc, deleted.entries.single().key, ctx).doc
        assertNotNull(purged.maps[K.MAP_ASH_MARKET])
        assertTrue(purged.liveness.isMapLive(K.MAP_ASH_MARKET))
        assertNull(purged.index.pin(K.PIN_ASH_MARKET))
        assertTrue(DerivedMarks.isTopLevelMap(purged, K.MAP_ASH_MARKET))
    }

    @Test
    fun `TC-20-28 이대로 두기`() {
        val ctx = Fx.ctx()
        val doc = Fx.doc()
        val kept = Fixes.keep(doc, K.NOAH, ctx).doc
        val row = DeletedItems.rows(kept, ctx, Fx.recorder()).single()
        assertTrue(row.kept)
        // The 14 lines still show the dotted 노아 ◇.
        assertTrue(Fx.linesOf(kept, K.NOAH).size == 14)
        assertTrue(Resolution.resolve(kept, RefTargetKind.CHARACTER, K.NOAH).dotted)
        // The ack is stored and survives a .civilight round trip.
        val ack = kept.checks.acknowledgements.single { it.checkType == CheckType.DELETED_ITEM }
        val text: Map<String, String> = ProjectFileSet.split(kept).encode().mapValues { it.value.toString() }
        val parsed: Map<String, JsonElement> = text.mapValues { Json.parseToJsonElement(it.value) }
        val decoded = ProjectFileSet.decode(parsed)
        assertTrue(decoded.failedPaths.isEmpty())
        val reread = decoded.files.toDoc(app = kept.app)
        assertEquals(ack, reread.checks.acknowledgements.single { it.checkType == CheckType.DELETED_ITEM })
        assertTrue(Fixes.isKept(reread, K.NOAH))
    }
}
