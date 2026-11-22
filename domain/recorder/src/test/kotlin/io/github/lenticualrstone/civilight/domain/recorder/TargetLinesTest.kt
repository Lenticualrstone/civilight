package io.github.lenticualrstone.civilight.domain.recorder

import io.github.lenticualrstone.civilight.domain.recorder.Fx.withRecord
import io.github.lenticualrstone.civilight.domain.recorder.hooks.RecorderHooks
import io.github.lenticualrstone.civilight.domain.recorder.hooks.TargetLines
import io.github.lenticualrstone.civilight.model.open
import io.github.lenticualrstone.civilight.model.recorder.Placement
import io.github.lenticualrstone.civilight.model.recorder.PlacementKind
import io.github.lenticualrstone.civilight.model.recorder.RecorderTargetKind
import io.github.lenticualrstone.civilight.model.sample.ExampleKeys
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** LRC-24 from inside one open project (the scene memo card, the profile header). */
class TargetLinesTest {
    private val doc = Fx.p1Doc()

    @Test
    fun LRC_T15_targetLineFromOneProjectMatchesTheWholeLibraryCount() {
        val library = Fx.openLibrary()
        val env = Fx.env()
        for (target in listOf(Fx.sceneS, Fx.hart, Fx.sera, Fx.target(RecorderTargetKind.CHARACTER, Fx.K.RIEN))) {
            assertEquals(
                RecorderHooks.unsortedLinkedCount(library, target, env),
                TargetLines.unsortedLinkedCount(library, doc, target, recordsOpen = false),
            )
        }
        assertEquals("레코더 · 정리 안 됨 1 ›", TargetLines.line(library, doc, Fx.sceneS, recordsOpen = false))
        assertNull(TargetLines.line(library, doc, Fx.target(RecorderTargetKind.CHARACTER, Fx.K.RIEN), recordsOpen = false))
    }

    @Test
    fun LRC_24_lockedHiddenRecordsAreNotCountedUntilOpen() {
        val hidden = Fx.hiddenLibrary()
        assertNull(TargetLines.line(hidden, doc, Fx.sceneS, recordsOpen = false))
        assertEquals("레코더 · 정리 안 됨 1 ›", TargetLines.line(hidden, doc, Fx.sceneS, recordsOpen = true))
    }

    @Test
    fun LRC_24_aCopyInAnotherProjectStillCountsAsPut() {
        // R1 linked to S, its only copy a page of P2 (not part of this project's document).
        val placed = Fx.openLibrary().let { lib ->
            lib.copy(
                placements = lib.placements + Placement(
                    key = ExampleKeys.placement(900), recordKey = Fx.R1, projectKey = Fx.P2, kind = PlacementKind.PAGE.open(),
                    targetKey = Fx.p2Page(1), copiedRevision = 1, createdAt = Fx.NOW, lastLabel = "정원 메모 1",
                ),
            )
        }
        assertEquals(0, TargetLines.unsortedLinkedCount(placed, doc, Fx.sceneS, recordsOpen = false))
        // Marked purged: the copy is gone, the record is `정리 안 됨` again.
        val purged = placed.copy(placements = placed.placements.map { if (it.key == ExampleKeys.placement(900)) it.copy(purgedAt = Fx.NOW) else it })
        assertEquals(1, TargetLines.unsortedLinkedCount(purged, doc, Fx.sceneS, recordsOpen = false))
        // A 끝냄 record is not counted.
        val done = Fx.openLibrary().withRecord(Fx.R1) { it.copy(doneMarked = true) }
        assertEquals(0, TargetLines.unsortedLinkedCount(done, doc, Fx.sceneS, recordsOpen = false))
    }
}
