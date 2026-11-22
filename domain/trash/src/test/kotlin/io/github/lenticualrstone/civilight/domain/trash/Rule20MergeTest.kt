package io.github.lenticualrstone.civilight.domain.trash

import io.github.lenticualrstone.civilight.model.OrderKeys
import io.github.lenticualrstone.civilight.model.cast.EntityRef
import io.github.lenticualrstone.civilight.model.cast.FieldPayload
import io.github.lenticualrstone.civilight.model.cast.FieldValue
import io.github.lenticualrstone.civilight.model.cast.Relation
import io.github.lenticualrstone.civilight.model.cast.RelationEndKind
import io.github.lenticualrstone.civilight.model.cast.SlotValue
import io.github.lenticualrstone.civilight.model.change.Anchor
import io.github.lenticualrstone.civilight.model.change.ChangeRecord
import io.github.lenticualrstone.civilight.model.change.ChangeTarget
import io.github.lenticualrstone.civilight.model.change.RelationSlotName
import io.github.lenticualrstone.civilight.model.doc.ProjectDoc
import io.github.lenticualrstone.civilight.model.doc.isLinkBeforeReveal
import io.github.lenticualrstone.civilight.model.map.Pin
import io.github.lenticualrstone.civilight.model.map.Route
import io.github.lenticualrstone.civilight.model.map.RoutePoint
import io.github.lenticualrstone.civilight.model.sample.ExampleKeys as K
import io.github.lenticualrstone.civilight.model.text.LinkTarget
import io.github.lenticualrstone.civilight.model.text.LinkTargetKind
import io.github.lenticualrstone.civilight.model.text.TextRun
import io.github.lenticualrstone.civilight.model.trash.TrashKind
import io.github.lenticualrstone.civilight.model.trash.TrashReason
import io.github.lenticualrstone.civilight.model.world.Place
import io.github.lenticualrstone.civilight.model.world.Term
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Ignore
import org.junit.Test

/** 30-cast §4.4 merge cases (TC-20-17 … TC-20-19, TC-20-26, TC-20-27) and LIB T-11. */
class Rule20MergeTest {
    private val kid = Fx.key("CHAR", 31)
    private val relKidOwen = Fx.key("RSHP", 31)
    private val relKidNoah = Fx.key("RSHP", 32)
    private val kidRecord = Fx.key("CHGR", 31)
    private val kidLink = Fx.key("HREF", 31)

    /**
     * TC-20-17's Given on the example: 시장 꼬마 has 3 lines (taken from 노아's 1-6 씬 4 lines, two of
     * them with fixed voice IDs), one `@` on a page, one scene appearance, 나이 `??`, a relation
     * with 오웬 (`거래 상대`, with one `◆`), 출생 and 성격 filled; 노아 has 나이 11 and a relation
     * with 오웬 whose 사이 is `보호`.
     */
    private fun scenario(mutual: Boolean = false): ProjectDoc {
        var d = Fx.addCharacter(Fx.noahLive(), kid, "시장 꼬마", 13, voiceCode = "KID")
        d = d.copy(
            cast = d.cast.copy(
                characters = d.cast.characters.map {
                    if (it.key == kid) {
                        it.copy(
                            fields = mapOf(
                                K.FIELD_AGE to FieldValue.UNKNOWN,
                                K.FIELD_BIRTH to FieldValue.of(FieldPayload.Date(1189)),
                                K.FIELD_PERSONALITY to FieldValue.of(FieldPayload.Texts(listOf("눈치가 빠름"))),
                            ),
                        )
                    } else {
                        it
                    }
                },
                relations = d.cast.relations.map { if (it.key == K.REL_OWEN_NOAH) it.copy(between = SlotValue(description = "보호")) else it } +
                    Relation(relKidOwen, EntityRef(RelationEndKind.CHARACTER, K.OWEN), EntityRef(RelationEndKind.CHARACTER, kid), between = SlotValue(kindKey = K.KIND_TRADE_PARTNER)) +
                    listOfNotNull(
                        if (mutual) Relation(relKidNoah, EntityRef(RelationEndKind.CHARACTER, kid), EntityRef(RelationEndKind.CHARACTER, K.NOAH)) else null,
                    ),
            ),
        )
        val three = Fx.linesOf(d, K.NOAH).filter { it.sceneKey == K.SCENE_1_6_4 }.take(3)
        three.forEach { b -> d = Fx.editBlock(d, b.key) { it.copy(speakerKey = kid) } }
        d = Fx.fixVoice(d, three[0].key, "KID_0106_${three[0].number.toString().padStart(3, '0')}")
        d = Fx.fixVoice(d, three[1].key, "KID_0106_${three[1].number.toString().padStart(3, '0')}")
        val page = d.pages.getValue(K.PAGE_SOCIETY_RULES)
        val paragraph = page.blocks.last()
        d = d.copy(
            pages = d.pages + (
                page.key to page.copy(
                    blocks = page.blocks.map {
                        if (it.key == paragraph.key) it.copy(runs = it.runs + TextRun.Link(kidLink, LinkTarget(LinkTargetKind.CHARACTER, kid), "시장 꼬마")) else it
                    },
                )
                ),
            story = d.story.copy(units = d.story.units.map { if (it.key == K.SCENE_1_6_3) it.copy(silentCast = it.silentCast + kid) else it }),
            changes = d.changes.copy(
                records = d.changes.records + ChangeRecord(
                    key = kidRecord, target = ChangeTarget.RelationSlot(relKidOwen, RelationSlotName.MIND_A),
                    value = ChangeRecord.encodeValue(SlotValue.serializer(), SlotValue(kindKey = K.KIND_PROTECTION)),
                    anchor = Anchor.AtBlock(three[2].key),
                ),
            ),
        )
        return d
    }

    @Test
    fun `TC-20-17 merge`() {
        val doc = scenario()
        val ctx = Fx.ctx()
        val preview = MergeOps.preview(doc, ItemKind.CHARACTER, kid, K.NOAH)
        assertEquals(5, preview.referenceCount)
        assertEquals(listOf(K.FIELD_AGE), preview.fieldConflicts.map { it.fieldDefKey })
        assertEquals(listOf(K.FIELD_BIRTH, K.FIELD_PERSONALITY), preview.filled)
        assertEquals("빈 칸 채움 · 2", Messages.filledLine(preview))
        assertEquals(2, preview.fixedVoiceIds)
        assertEquals(listOf(K.REL_OWEN_NOAH), preview.relationOverlaps.map { it.targetRelationKey })
        assertEquals("인물 합치기" to "시장 꼬마 → 노아 · 기본은 노아 값", Messages.mergeSheetHead(preview))
        val lines = Fx.linesOf(doc, kid).map { it.key }

        val result = MergeOps.merge(doc, ItemKind.CHARACTER, kid, K.NOAH, ctx)
        val after = result.doc
        // 옮길 참조 5 move to 노아.
        assertTrue(lines.all { after.index.block(it)!!.speakerKey == K.NOAH })
        assertTrue(K.NOAH in after.index.unit(K.SCENE_1_6_3)!!.silentCast && kid !in after.index.unit(K.SCENE_1_6_3)!!.silentCast)
        val link = after.pages.getValue(K.PAGE_SOCIETY_RULES).blocks.flatMap { it.runs }.filterIsInstance<TextRun.Link>().single { it.linkKey == kidLink }
        assertEquals(K.NOAH, link.target.key)
        // 나이 stays 11세; 출생 and 성격 filled without asking.
        val noah = after.index.character(K.NOAH)!!
        assertEquals(FieldValue.of(FieldPayload.Number("11")), noah.fields[K.FIELD_AGE])
        assertEquals(FieldValue.of(FieldPayload.Date(1189)), noah.fields[K.FIELD_BIRTH])
        assertEquals(FieldValue.of(FieldPayload.Texts(listOf("눈치가 빠름"))), noah.fields[K.FIELD_PERSONALITY])
        // The 오웬 relation keeps 노아's slot values; both sets of ◆ are kept.
        assertNull(after.index.relation(relKidOwen))
        assertEquals(SlotValue(description = "보호"), after.index.relation(K.REL_OWEN_NOAH)!!.between)
        val moved = after.index.record(kidRecord)!!.target as ChangeTarget.RelationSlot
        assertEquals(K.REL_OWEN_NOAH, moved.relationKey)
        assertEquals(RelationSlotName.MIND_A, moved.slot.known)
        assertTrue(after.liveness.isLive(after.index.record(K.N2)!!))
        // Fixed voice IDs unchanged.
        assertEquals(2, lines.mapNotNull { after.index.block(it)!!.voice?.fixedId }.count { it.startsWith("KID_") })
        // 시장 꼬마 goes to the trash as 합침 → 노아.
        assertEquals(TrashReason.MERGED, result.entry.reason.known)
        assertEquals(K.NOAH, result.entry.mergedIntoKey)
        assertFalse(after.liveness.isCharacterLive(kid))
        assertEquals("시장 꼬마 → 노아 합침 · 참조 5 · 되돌리기", Messages.mergeToast(preview).text)
        // 되돌리기 undoes every change above.
        assertEquals(doc, MergeOps.undo(after, result))
    }

    @Test
    fun `TC-20-18 mutual relation on merge`() {
        val doc = scenario(mutual = true)
        val preview = MergeOps.preview(doc, ItemKind.CHARACTER, kid, K.NOAH)
        assertEquals(relKidNoah, preview.mutualRelationKey)
        assertEquals("서로 관계 1 · 지워짐", Messages.mutualRelationLine(preview))
        val after = MergeOps.merge(doc, ItemKind.CHARACTER, kid, K.NOAH, Fx.ctx()).doc
        assertNull(after.index.relation(relKidNoah))
        assertEquals(relKidNoah, after.tombstones.mergeRecords.single().deletedMutualRelationKey)
    }

    @Ignore("spec conflict: TC-20-19 reads `칸 3` but TC-20-17 takes only 출생 and 성격 (`빈 칸 채움 · 2`, 나이 stays 노아's); see the variant")
    @Test
    fun `TC-20-19 restore the merged item`() {
        val merged = MergeOps.merge(scenario(), ItemKind.CHARACTER, kid, K.NOAH, Fx.ctx())
        assertEquals("옮긴 참조 5 · 관계 1 · 칸 3 · 노아에 그대로", Messages.mergedRestoreLine(merged.record, "노아"))
    }

    @Test
    fun `TC-20-19 restore the merged item (fixture counts)`() {
        val ctx = Fx.ctx()
        val doc = scenario()
        val original = doc.index.character(kid)!!
        val merged = MergeOps.merge(doc, ItemKind.CHARACTER, kid, K.NOAH, ctx)
        assertEquals("옮긴 참조 5 · 관계 1 · 칸 2 · 노아에 그대로", Messages.mergedRestoreLine(merged.record, "노아"))
        val back = Fx.restored(RestoreOps.restore(merged.doc, merged.entry.key, ctx)).doc
        val restored = back.index.character(kid)!!
        // A separate character with its original values and no relations.
        assertTrue(back.liveness.isCharacterLive(kid))
        assertEquals(original.fields, restored.fields)
        assertEquals(original.name, restored.name)
        assertTrue(back.index.relationsOf(EntityRef(RelationEndKind.CHARACTER, kid)).isEmpty())
        // The 5 references, the relation and the taken values stay with 노아.
        assertTrue(Fx.linesOf(doc, kid).all { back.index.block(it.key)!!.speakerKey == K.NOAH })
        assertEquals(0, Impact.of(back, ItemId.character(kid)).referenceTotal)
        assertEquals(K.REL_OWEN_NOAH, (back.index.record(kidRecord)!!.target as ChangeTarget.RelationSlot).relationKey)
        assertEquals(FieldValue.of(FieldPayload.Date(1189)), back.index.character(K.NOAH)!!.fields[K.FIELD_BIRTH])
    }

    @Test
    fun `TC-20-26 merge two places with pins`() {
        val placeB = Fx.key("P1CE", 31)
        val pinB = Fx.key("P1NS", 31)
        var doc = Fx.doc()
        val lower = doc.maps.getValue(K.MAP_LOWER_DISTRICT)
        doc = doc.copy(
            world = doc.world.copy(
                places = doc.world.places + Place(placeB, OrderKeys.after(doc.world.places.maxOf { it.order }), "시장 골목"),
            ),
            maps = doc.maps + (
                lower.key to lower.copy(
                    pins = lower.pins + Pin(pinB, placeB, 0.5f, 0.5f),
                    routes = lower.routes +
                        Route(Fx.key("RTES", 31), listOf(RoutePoint.pin(K.PIN_SOCIETY_GATE), RoutePoint.pin(pinB))) +
                        Route(Fx.key("RTES", 32), listOf(RoutePoint.pin(K.PIN_LOWER_STREET), RoutePoint.pin(pinB))),
                )
                ),
        )
        val targetPin = doc.index.pin(K.PIN_ASH_MARKET)!!.second
        val preview = MergeOps.preview(doc, ItemKind.PLACE, placeB, K.PLACE_ASH_MARKET)
        assertEquals(2, preview.routeEnds)
        val after = MergeOps.merge(doc, ItemKind.PLACE, placeB, K.PLACE_ASH_MARKET, Fx.ctx()).doc
        val map = after.maps.getValue(K.MAP_LOWER_DISTRICT)
        // A's pin stays at its coordinates; B's pin goes to the trash with B.
        assertEquals(targetPin, map.pins.single { it.key == K.PIN_ASH_MARKET })
        assertFalse(after.liveness.isOverlayLive(map.key, map.pins.single { it.key == pinB }))
        // Both route ends move to A's pin.
        val ends = map.routes.filter { it.key == Fx.key("RTES", 31) || it.key == Fx.key("RTES", 32) }.map { it.points.last().pinKey }
        assertEquals(listOf(K.PIN_ASH_MARKET, K.PIN_ASH_MARKET), ends)
    }

    @Test
    fun `TC-20-27 merge two terms with first reveals`() {
        val termB = Fx.key("TERM", 31)
        val revealB = Fx.key("CHGR", 32)
        val linkB = Fx.key("HREF", 32)
        val useBlock = K.block(1, 4, 6) // 1-4 씬 2
        var doc = Fx.doc()
        doc = doc.copy(
            world = doc.world.copy(terms = doc.world.terms + Term(termB, OrderKeys.after(doc.world.terms.maxOf { it.order }), "빛의 돌")),
            changes = doc.changes.copy(
                records = doc.changes.records + ChangeRecord(revealB, ChangeTarget.TermReveal(termB), anchor = Anchor.UnitStart(K.EPISODE_1_3)),
            ),
        )
        doc = Fx.editBlock(doc, useBlock) { it.copy(runs = it.runs + TextRun.Link(linkB, LinkTarget(LinkTargetKind.TERM, termB), "빛의 돌")) }
        assertFalse(doc.isLinkBeforeReveal(termB, useBlock))
        assertTrue(MergeOps.preview(doc, ItemKind.TERM, termB, K.TERM_ORIGINIUM_LANTERN).revealConflict)

        // Default: A's reveal 1-7 #044 is kept; the use in 1-4 씬 2 becomes ▲.
        val byDefault = MergeOps.merge(doc, ItemKind.TERM, termB, K.TERM_ORIGINIUM_LANTERN, Fx.ctx()).doc
        assertEquals(Anchor.AtBlock(K.BLOCK_044), MergeOps.liveReveal(byDefault, K.TERM_ORIGINIUM_LANTERN)!!.anchor)
        assertTrue(byDefault.isLinkBeforeReveal(K.TERM_ORIGINIUM_LANTERN, useBlock))

        // B's value chosen: the first reveal is B's 1-3 record and that ▲ is not raised.
        val fromB = MergeOps.merge(doc, ItemKind.TERM, termB, K.TERM_ORIGINIUM_LANTERN, Fx.ctx(), MergeChoices(revealFromMerged = true)).doc
        assertEquals(revealB, fromB.changes.records.single { fromB.liveness.isLive(it) && (it.target as? ChangeTarget.TermReveal)?.termKey == K.TERM_ORIGINIUM_LANTERN }.key)
        assertFalse(fromB.isLinkBeforeReveal(K.TERM_ORIGINIUM_LANTERN, useBlock))
        assertTrue("빛의 돌" in fromB.index.term(K.TERM_ORIGINIUM_LANTERN)!!.aliases)
    }

    @Test
    fun `T-11 merged item trash row`() {
        val ctx = Fx.ctx()
        val result = MergeOps.merge(scenario(), ItemKind.CHARACTER, kid, K.NOAH, ctx)
        assertEquals(TrashKind.CHARACTER, result.entry.kind.known)
        assertEquals(TrashReason.MERGED, result.entry.reason.known)
        val row = TrashListing.projectItemRows(result.doc, ctx).single { it.entryKey == result.entry.key }
        assertTrue(row.sub, row.sub.contains("합침 → 노아"))
        assertEquals("시장 꼬마", row.title)
    }
}
