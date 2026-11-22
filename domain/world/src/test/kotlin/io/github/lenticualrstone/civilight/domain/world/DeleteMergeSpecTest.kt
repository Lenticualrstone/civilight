package io.github.lenticualrstone.civilight.domain.world

import io.github.lenticualrstone.civilight.domain.world.WorldFixture.K
import io.github.lenticualrstone.civilight.domain.world.WorldFixture.T1
import io.github.lenticualrstone.civilight.domain.world.WorldFixture.ctx
import io.github.lenticualrstone.civilight.domain.world.WorldFixture.key
import io.github.lenticualrstone.civilight.domain.world.WorldFixture.queries
import io.github.lenticualrstone.civilight.domain.world.WorldFixture.stateA
import io.github.lenticualrstone.civilight.domain.world.common.DocRuns
import io.github.lenticualrstone.civilight.domain.world.item.CreateResult
import io.github.lenticualrstone.civilight.domain.world.item.MergeRows
import io.github.lenticualrstone.civilight.domain.world.item.RefLabel
import io.github.lenticualrstone.civilight.domain.world.item.WorldCards
import io.github.lenticualrstone.civilight.domain.world.item.WorldItems
import io.github.lenticualrstone.civilight.domain.world.item.WorldRefs
import io.github.lenticualrstone.civilight.domain.world.map.DistanceProblem
import io.github.lenticualrstone.civilight.domain.world.map.Distances
import io.github.lenticualrstone.civilight.domain.world.map.MapEdit
import io.github.lenticualrstone.civilight.domain.world.map.MapEdits
import io.github.lenticualrstone.civilight.domain.world.map.MapViews
import io.github.lenticualrstone.civilight.domain.world.map.PinStatus
import io.github.lenticualrstone.civilight.domain.world.map.Routes
import io.github.lenticualrstone.civilight.domain.world.term.RunEdits
import io.github.lenticualrstone.civilight.domain.world.term.TermCheckRow
import io.github.lenticualrstone.civilight.domain.world.time.StoryEngine
import io.github.lenticualrstone.civilight.domain.world.track.CharacterTracks
import io.github.lenticualrstone.civilight.domain.world.track.SkipReason
import io.github.lenticualrstone.civilight.model.Key
import io.github.lenticualrstone.civilight.model.Point
import io.github.lenticualrstone.civilight.model.change.Anchor
import io.github.lenticualrstone.civilight.model.change.ChangeRecord
import io.github.lenticualrstone.civilight.model.change.ChangeTarget
import io.github.lenticualrstone.civilight.model.doc.ProjectDoc
import io.github.lenticualrstone.civilight.model.map.RoutePoint
import io.github.lenticualrstone.civilight.model.open
import io.github.lenticualrstone.civilight.model.sample.ExampleClock
import io.github.lenticualrstone.civilight.model.sample.ExampleProject
import io.github.lenticualrstone.civilight.model.story.PlaceRef
import io.github.lenticualrstone.civilight.model.story.PlaceRefKind
import io.github.lenticualrstone.civilight.model.text.LinkTarget
import io.github.lenticualrstone.civilight.model.text.LinkTargetKind
import io.github.lenticualrstone.civilight.model.text.TextRun
import io.github.lenticualrstone.civilight.model.trash.NameTag
import io.github.lenticualrstone.civilight.model.trash.NameTagKind
import io.github.lenticualrstone.civilight.model.trash.NameTagState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * WLD §4.5 and §4.8 rule-20 cases (WLD-T43, T44, T69 … T71). Trash, restore, purge and merge are
 * `domain:trash` operations the world module cannot call: each case builds their post-state by hand
 * (marks, a name tag, retargeted links) and asserts the world-side readings.
 */
class DeleteMergeSpecTest {
    private val now = ExampleClock.NOW

    private fun linkRuns(doc: ProjectDoc, vararg linkKeys: Key): List<TextRun.Link> {
        val all = doc.allBlocks.flatMap { it.runs } + doc.pages.values.flatMap { p -> p.blocks.flatMap { it.runs } }
        return linkKeys.map { k -> all.filterIsInstance<TextRun.Link>().single { it.linkKey == k } }
    }

    private fun withTerms(doc: ProjectDoc, f: (io.github.lenticualrstone.civilight.model.world.Term) -> io.github.lenticualrstone.civilight.model.world.Term) =
        doc.copy(world = doc.world.copy(terms = doc.world.terms.map(f)))

    @Test
    fun `WLD-T43 deleting, restoring and purging a term`() {
        val base = stateA()
        val trashed = withTerms(base) { if (it.key == T1) it.copy(deletedAt = now, trashEntryKey = key(430)) else it }
        for (run in linkRuns(trashed, K.LINK_L42, K.LINK_L44, K.LINK_LPG)) {
            val label = WorldRefs.link(trashed, run)
            assertTrue(label is RefLabel.Deleted)
            assertEquals("지운 용어 · 오리지늄 등불", label!!.text)
            assertTrue(label.dotted)
            assertEquals(listOf("다른 항목에 잇기", "글로 풀기", "되살리기", "이대로 두기"), (label as RefLabel.Deleted).actions)
        }
        assertFalse(trashed.liveness.isLive(trashed.index.record(K.R9)!!))
        val q = queries(trashed)
        assertNull(q.reveal(T1))
        val deleted = q.checks().deleted.single()
        assertEquals("◇ 지운 용어 · 오리지늄 등불", deleted.text)
        assertEquals("▲ 1 보류", deleted.heldText)
        assertTrue(q.checks().rows.none { it is TermCheckRow.BeforeReveal })

        // Restored: same key, everything re-attaches.
        val restored = withTerms(trashed) { if (it.key == T1) it.copy(deletedAt = null, trashEntryKey = null) else it }
        val back = queries(restored)
        assertNotNull(back.reveal(T1))
        assertEquals(listOf(K.LINK_L42), back.activeWarnings(T1).map { it.linkKey })
        assertEquals(RefLabel.Live("오리지늄"), WorldRefs.link(restored, linkRuns(restored, K.LINK_L42).single()))

        // Purged instead: the row and its reveal record are gone, a name tag remains.
        val purged = trashed.copy(
            world = trashed.world.copy(terms = trashed.world.terms.filterNot { it.key == T1 }),
            changes = trashed.changes.copy(records = trashed.changes.records.filterNot { it.target == ChangeTarget.TermReveal(T1) }),
            tombstones = trashed.tombstones.copy(nameTags = trashed.tombstones.nameTags + NameTag(T1, NameTagKind.TERM.open(), "오리지늄 등불", state = NameTagState.PURGED.open(), purgedAt = now)),
        )
        for (run in linkRuns(purged, K.LINK_L42, K.LINK_L44, K.LINK_LPG)) {
            val label = WorldRefs.link(purged, run)!!
            assertEquals("영구 삭제 · 오리지늄 등불", label.text)
            assertFalse((label as RefLabel.Purged).actions.contains("되살리기"))
        }
        val qp = queries(purged)
        assertEquals(0, qp.heldWarnings(T1))
        assertTrue(qp.checks().deleted.isEmpty())
        assertEquals("오리지늄 등불", purged.index.nameTag(T1)!!.name)
    }

    @Test
    fun `WLD-T44 deleting and restoring a place`() {
        val base = ExampleProject.build()
        val trashed = base.copy(world = base.world.copy(places = base.world.places.map {
            if (it.key == K.PLACE_ASH_MARKET) it.copy(deletedAt = now, trashEntryKey = key(440)) else it
        }))
        val lower = trashed.maps.getValue(K.MAP_LOWER_DISTRICT)
        val pin = lower.pins.single { it.key == K.PIN_ASH_MARKET }
        assertFalse(trashed.liveness.isOverlayLive(lower.key, pin))
        val route = lower.routes.single { it.key == K.ROUTE_GATE_TO_MARKET }
        assertEquals(listOf(true, false), Routes.ends(trashed, lower, route).map { it.live })
        val scene = trashed.index.unit(K.SCENE_1_6_1)!!
        val label = WorldRefs.scenePlace(trashed, scene.place!!)
        assertEquals("지운 장소 · 잿더미 시장", label.text)
        assertEquals(listOf("장소 고르기", "되살리기", "이대로 두기"), (label as RefLabel.Deleted).actions)
        val distance = Distances(trashed).distanceKm(PlaceRef(PlaceRefKind.PLACE, K.PLACE_SOCIETY_GATE), PlaceRef(PlaceRefKind.PLACE, K.PLACE_ASH_MARKET))
        assertEquals(DistanceProblem.NO_LOCATION, distance.unavailable)
        val engine = StoryEngine(trashed)
        assertEquals(SkipReason.DELETED_PLACE, CharacterTracks(trashed, engine, engine).track(K.RIEN)!!.skipped[K.SCENE_1_6_1])
        assertEquals("세계 지도 › 하층 구역 지도 › 지운 장소 · 잿더미 시장", MapViews.breadcrumbText(trashed, trashed.maps.getValue(K.MAP_ASH_MARKET)))

        // Restored: the same key comes back, so the pin and the route end re-attach.
        val restored = trashed.copy(world = trashed.world.copy(places = trashed.world.places.map {
            if (it.key == K.PLACE_ASH_MARKET) it.copy(deletedAt = null, trashEntryKey = null) else it
        }))
        assertTrue(restored.liveness.isOverlayLive(K.MAP_LOWER_DISTRICT, pin))
        assertEquals(RefLabel.Live("잿더미 시장"), WorldRefs.scenePlace(restored, scene.place!!))
        val pinBack = restored.maps.getValue(K.MAP_LOWER_DISTRICT).pins.single { it.key == K.PIN_ASH_MARKET }
        assertEquals(Point(0.45f, 0.40f), Point(pinBack.x, pinBack.y))
        assertTrue(Routes.ends(restored, restored.maps.getValue(K.MAP_LOWER_DISTRICT), route).all { it.live })
    }

    @Test
    fun `WLD-T69 merging places with pins`() {
        val c = ctx()
        val base = ExampleProject.build()
        val alley = (WorldItems.createPlace(base, "시장 골목", c) as CreateResult.Created)
        val pinned = MapEdits.addPin(alley.doc, K.MAP_LOWER_DISTRICT, alley.key, Point(0.47f, 0.45f), c) as MapEdit.Applied
        val alleyPin = pinned.value.key
        var given = MapEdits.addRoute(pinned.doc, K.MAP_LOWER_DISTRICT, listOf(RoutePoint.pin(K.PIN_SOCIETY_GATE), RoutePoint.pin(alleyPin)), c).let { (it as MapEdit.Applied).doc }
        given = MapEdits.addRoute(given, K.MAP_LOWER_DISTRICT, listOf(RoutePoint.pin(K.PIN_LOWER_STREET), RoutePoint.pin(alleyPin)), c).let { (it as MapEdit.Applied).doc }
        assertEquals("경로 끝 2 · 잿더미 시장 핀으로", MergeRows.routeEndRow(given, alley.key, K.PLACE_ASH_MARKET))

        // The merge (domain:trash): the merged place goes to the trash, its route ends move to the target pin.
        var merged = given.copy(world = given.world.copy(places = given.world.places.map {
            if (it.key == alley.key) it.copy(deletedAt = now, trashEntryKey = key(690)) else it
        }))
        merged = MapEdits.reattachRouteEnds(merged, K.MAP_LOWER_DISTRICT, alleyPin, K.PIN_ASH_MARKET, c)!!
        val lower = merged.maps.getValue(K.MAP_LOWER_DISTRICT)
        val target = lower.pins.single { it.key == K.PIN_ASH_MARKET }
        assertEquals(Point(0.45f, 0.40f), Point(target.x, target.y))
        assertFalse(merged.liveness.isOverlayLive(lower.key, lower.pins.single { it.key == alleyPin }))
        val moved = lower.routes.filter { r -> r.points.last().pinKey == K.PIN_ASH_MARKET }
        assertEquals(3, moved.size) // the fixture's route plus the two re-attached ones
        assertTrue(lower.routes.none { r -> r.points.any { it.pinKey == alleyPin } })
        assertTrue(moved.all { r -> Routes.ends(merged, lower, r).all { it.live } })
        assertNull(MergeRows.routeEndRow(merged, alley.key, K.PLACE_ASH_MARKET))
    }

    @Test
    fun `WLD-T70 merging terms with two first reveals`() {
        val c = ctx()
        val t9 = key(700)
        var doc = WorldFixture.addTerm(stateA(), t9, "빛돌")
        fun linkSpan(d: ProjectDoc, block: Key, linkKey: Key, word: String): ProjectDoc = WorldFixture.withBlock(d, block) { b ->
            val text = b.runs.joinToString("") { it.shownText }
            val at = text.indexOf(word)
            b.copy(runs = RunEdits.wrap(b.runs, at, at + word.length, TextRun.Link(linkKey, LinkTarget(LinkTargetKind.TERM, t9), word))!!)
        }
        doc = linkSpan(doc, K.BLOCK_B32, key(701), "빛돌")
        doc = linkSpan(doc, K.BLOCK_B61, key(702), "빛돌")
        val t9Reveal = ChangeRecord(key(703), ChangeTarget.TermReveal(t9), anchor = Anchor.AtBlock(K.BLOCK_B32), createdAt = now)
        doc = doc.copy(changes = doc.changes.copy(records = doc.changes.records + t9Reveal))
        assertTrue(queries(doc).activeWarnings(t9).isEmpty())

        // Merge with the default choice (the target's reveal): links retarget, T9 and its reveal go to the trash.
        fun merge(d: ProjectDoc, keepMergedReveal: Boolean): ProjectDoc {
            var m = DocRuns.mapAll(d) { runs -> RunEdits.mapLinks(runs) { l -> if (l.target.key == t9) l.copy(target = l.target.copy(key = T1)) else l } }
            m = withTerms(m) {
                when (it.key) {
                    t9 -> it.copy(deletedAt = now, trashEntryKey = key(704))
                    T1 -> it.copy(aliases = it.aliases + "빛돌")
                    else -> it
                }
            }
            if (keepMergedReveal) {
                m = m.copy(changes = m.changes.copy(records = m.changes.records.map { r ->
                    when (r.target) {
                        ChangeTarget.TermReveal(T1) -> r.copy(target = ChangeTarget.TermReveal(t9))
                        ChangeTarget.TermReveal(t9) -> r.copy(target = ChangeTarget.TermReveal(T1))
                        else -> r
                    }
                }))
            }
            return m
        }
        val byTarget = queries(merge(doc, keepMergedReveal = false))
        assertEquals(listOf(K.LINK_L42, key(701), key(702)).toSet(), byTarget.activeWarnings(T1).map { it.linkKey }.toSet())
        assertEquals(3, byTarget.activeWarnings(T1).size)
        assertTrue(byTarget.usage(T1).filter { it.linkKey in setOf(key(701), key(702)) }.all { it.termKey == T1 })

        val byMerged = queries(merge(doc, keepMergedReveal = true))
        assertEquals(K.BLOCK_B32, byMerged.reveal(T1)!!.blockKey)
        assertTrue(byMerged.activeWarnings(T1).isEmpty())
        assertTrue(c.newKey().isNotEmpty())
    }

    @Test
    fun `WLD-T71 restoring a place whose map was deleted`() {
        val base = ExampleProject.build()
        // 기록실 to trash, then 하층 구역 지도 to trash, then 기록실 restored.
        val mapTrashed = base.copy(maps = base.maps + (K.MAP_LOWER_DISTRICT to base.maps.getValue(K.MAP_LOWER_DISTRICT).copy(deletedAt = now, trashEntryKey = key(711))))
        val status = MapViews.pinsOf(mapTrashed, K.PLACE_ARCHIVE_ROOM).single()
        assertTrue(status is PinStatus.MapMissing)
        assertEquals("◇ 지도 없음", (status as PinStatus.MapMissing).text)
        assertEquals(listOf("지도 되살리기", "핀 다시 놓기"), status.actions)
        assertTrue(mapTrashed.liveness.isPlaceLive(K.PLACE_ARCHIVE_ROOM))
        assertEquals(listOf(status), WorldCards(mapTrashed).placeCard(K.PLACE_ARCHIVE_ROOM)!!.pins)
    }
}
