package io.github.lenticualrstone.civilight.feature.story

import io.github.lenticualrstone.civilight.domain.library.newproject.ChecklistItem
import io.github.lenticualrstone.civilight.feature.story.Fx.node
import io.github.lenticualrstone.civilight.feature.story.home.HomeModel
import io.github.lenticualrstone.civilight.feature.story.home.HomeStartModel
import io.github.lenticualrstone.civilight.model.recorder.RecorderLibrary
import io.github.lenticualrstone.civilight.navigation.CastList
import io.github.lenticualrstone.civilight.navigation.Script
import io.github.lenticualrstone.civilight.navigation.World
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** The 만든 직후 block hosted on home (STO-01 + LIB-13, board `NewReady`). */
class HomeStartModelTest {
    private val fresh = Fx.mini(listOf("장", "에피소드", "씬"), node("C1", null, null, node("E1", null, null, node("S1"), node("S2"))))

    @Test
    fun newProjectShowsTheBlockUntilClosed() {
        val start = HomeStartModel.build(fresh, RecorderLibrary(), readOnly = false)!!
        assertEquals("시작해 볼 것 · 0/4", start.header)
        assertEquals("1장 · 에피소드 1-1 · 씬 1", start.quickWrite)
        assertEquals(Script(fresh.project.key, sceneKey = "S1"), start.quickWriteRoute)
        val byId = start.items.associateBy { it.id }
        assertEquals(CastList(fresh.project.key), byId.getValue(ChecklistItem.Id.PROTAGONIST).route)
        assertEquals(World(fresh.project.key, io.github.lenticualrstone.civilight.navigation.WorldTab.TERMS, createTerm = true), byId.getValue(ChecklistItem.Id.TERM).route)
        assertEquals("no character yet: domain:library now draws the sub line (it was TODO_COPY_LIB13_PROTAGONIST_SUB)", "인물", byId.getValue(ChecklistItem.Id.PROTAGONIST).sub)
        assertTrue(start.items.none { it.sub.startsWith("TODO_COPY_") || it.title.startsWith("TODO_COPY_") })
        assertEquals(start.quickWriteRoute, byId.getValue(ChecklistItem.Id.FIRST_LINE).route)
        assertTrue("no linked records → that row is left out", ChecklistItem.Id.LINKED_RECORDS !in byId)

        val closed = fresh.copy(app = HomeStartModel.dismiss(fresh.app))
        assertTrue(closed.app.activity.startChecklist.dismissed)
        assertNull(HomeStartModel.build(closed, RecorderLibrary(), readOnly = false))
    }

    @Test
    fun theReadOnlyExampleNeverShowsIt() {
        assertNull(HomeStartModel.build(Fx.doc(), RecorderLibrary(), readOnly = true))
    }

    @Test
    fun itemsCompleteByFact() {
        val start = HomeStartModel.build(Fx.doc(), RecorderLibrary(), readOnly = false)!!
        val byId = start.items.associateBy { it.id }
        assertTrue(byId.getValue(ChecklistItem.Id.PROTAGONIST).done)
        assertTrue(byId.getValue(ChecklistItem.Id.FIRST_LINE).done)
    }

    @Test
    fun firstSceneFollowsTheFirstUnitOfEveryLevel() {
        assertEquals("S1", HomeStartModel.firstScene(fresh))
        assertNull(HomeStartModel.firstScene(Fx.mini(listOf("장", "에피소드", "씬"), node("C1"))))
        assertEquals("A1", HomeStartModel.firstScene(Fx.mini(listOf("씬"), node("A1"), node("A2"))))
    }

    @Test
    fun newReadyOnlyUntilThereIsSomethingToResume() {
        fun ui(doc: io.github.lenticualrstone.civilight.model.doc.ProjectDoc) =
            HomeModel.build(doc, readOnly = false, locked = false, today = Fx.today, zone = Fx.zone)
                .copy(start = HomeStartModel.build(doc, RecorderLibrary(), readOnly = false))
        val fresh = ui(fresh)
        assertNull(fresh.resume)
        assertTrue(fresh.newReady)
        // The example as a writable project: the block stays on top, but home is `Home` again.
        val written = ui(Fx.doc())
        assertTrue(written.start != null && written.resume != null)
        assertFalse(written.newReady)
        assertFalse(ui(Fx.doc().copy(app = HomeStartModel.dismiss(Fx.doc().app))).newReady)
    }
}
