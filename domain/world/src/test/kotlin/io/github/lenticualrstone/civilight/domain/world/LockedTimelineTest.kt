package io.github.lenticualrstone.civilight.domain.world

import io.github.lenticualrstone.civilight.domain.world.WorldFixture.K
import io.github.lenticualrstone.civilight.domain.world.time.EventDetails
import io.github.lenticualrstone.civilight.domain.world.time.EventMasks
import io.github.lenticualrstone.civilight.domain.world.time.StoryEngine
import io.github.lenticualrstone.civilight.domain.world.time.TimelineBuilder
import io.github.lenticualrstone.civilight.domain.world.time.TimelineRow
import io.github.lenticualrstone.civilight.domain.world.time.TimelineView
import io.github.lenticualrstone.civilight.domain.world.time.WorldTimeEdits
import io.github.lenticualrstone.civilight.model.doc.ProjectDoc
import io.github.lenticualrstone.civilight.model.position.LensPoint
import io.github.lenticualrstone.civilight.model.project.CalendarMonth
import io.github.lenticualrstone.civilight.model.project.WorldCalendar
import io.github.lenticualrstone.civilight.model.project.WorldTimeMode
import io.github.lenticualrstone.civilight.model.sample.ExampleProject
import io.github.lenticualrstone.civilight.model.story.PlaceRef
import io.github.lenticualrstone.civilight.model.story.PlaceRefKind
import io.github.lenticualrstone.civilight.model.world.WorldDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** WLD-13 on the 연표 and the event detail: hidden items are masked while locked (additive, batch 2 of feature:world). */
class LockedTimelineTest {
    private val base = ExampleProject.build()

    /** 대정전 at the place 하층 거리, which is hidden; 리엔 hidden too. */
    private fun hiddenDoc(): ProjectDoc {
        val d = base.copy(world = base.world.copy(
            places = base.world.places.map { if (it.key == K.PLACE_LOWER_STREET) it.copy(hidden = true) else it },
            events = base.world.events.map {
                if (it.key == K.EVENT_BLACKOUT) it.copy(place = PlaceRef(PlaceRefKind.PLACE, K.PLACE_LOWER_STREET)) else it
            },
        ))
        return d.copy(cast = d.cast.copy(characters = d.cast.characters.map { if (it.key == K.RIEN) it.copy(hidden = true) else it }))
    }

    private fun blackoutRow(doc: ProjectDoc, unlocked: Boolean): TimelineRow.EventRow =
        TimelineBuilder(doc, StoryEngine(doc)).build(LensPoint.Latest, TimelineView.EPISODE, null, hiddenUnlocked = unlocked)
            .rows.filterIsInstance<TimelineRow.EventRow>().first { it.event.key == K.EVENT_BLACKOUT }

    @Test
    fun `the default build is the open build`() {
        val doc = hiddenDoc()
        val builder = TimelineBuilder(doc, StoryEngine(doc))
        assertEquals(builder.build(), builder.build(LensPoint.Latest, TimelineView.EPISODE, null, hiddenUnlocked = true))
    }

    @Test
    fun `locked timeline masks a hidden place and leaves out a hidden character's age`() {
        val doc = hiddenDoc()
        val open = blackoutRow(doc, unlocked = true)
        val locked = blackoutRow(doc, unlocked = false)
        assertTrue(open.sub, open.sub.contains("하층 거리"))
        assertFalse(locked.sub, locked.sub.contains("하층 거리"))
        assertTrue(locked.sub, locked.sub.contains(EventMasks.HIDDEN_ITEM))
        assertFalse(locked.sub, locked.sub.contains("리엔"))
        assertEquals(open.title, locked.title)
    }

    @Test
    fun `locked event detail masks place, ages and related terms`() {
        val doc = hiddenDoc().let { d ->
            d.copy(world = d.world.copy(terms = d.world.terms.map { if (it.key == K.TERM_ORIGINIUM_LANTERN) it.copy(hidden = true) else it }))
        }
        val details = EventDetails(doc, StoryEngine(doc))
        val open = details.detail(K.EVENT_BLACKOUT)!!
        val locked = details.detail(K.EVENT_BLACKOUT, hiddenUnlocked = false)!!
        assertEquals("하층 거리", open.rows.toMap()["장소"])
        assertEquals(EventMasks.HIDDEN_ITEM, locked.rows.toMap()["장소"])
        assertFalse(locked.rows.toMap()["그때 나이"].orEmpty().contains("리엔"))
        val openTerms = open.rows.toMap()["관련 용어"]
        if (openTerms != null) {
            assertTrue(openTerms.contains("오리지늄 등불"))
            assertFalse(locked.rows.toMap()["관련 용어"]!!.contains("오리지늄 등불"))
        }
        locked.changes.filter { it.label == EventMasks.HIDDEN_ITEM }.forEach { assertEquals("", it.value) }
        assertFalse(locked.changes.any { it.label.startsWith("리엔") })
    }

    @Test
    fun `WLD-29 changing the mode keeps dates, calendar and start year`() {
        val calendar = WorldCalendar(months = listOf(CalendarMonth("가달", 30), CalendarMonth("나달", 31)))
        val dated = base.copy(
            project = base.project.copy(worldTime = base.project.worldTime.copy(calendar = calendar)),
            world = base.world.copy(events = base.world.events.map { if (it.key == K.EVENT_BLACKOUT) it.copy(worldDate = WorldDate(1195, 2, 3)) else it }),
        )
        val none = WorldTimeEdits.setMode(dated, WorldTimeMode.NONE)
        assertEquals(WorldTimeMode.NONE, none.project.worldTime.mode.known)
        assertEquals(calendar, none.project.worldTime.calendar)
        assertEquals(dated.project.worldTime.storyStartYear, none.project.worldTime.storyStartYear)
        assertEquals(WorldDate(1195, 2, 3), none.index.event(K.EVENT_BLACKOUT)!!.worldDate)
        val back = WorldTimeEdits.setMode(none, WorldTimeMode.CALENDAR)
        assertEquals(calendar, back.project.worldTime.calendar)
        assertEquals(WorldDate(1195, 2, 3), back.index.event(K.EVENT_BLACKOUT)!!.worldDate)
        assertTrue(WorldTimeEdits.setMode(back, WorldTimeMode.CALENDAR) === back)
    }

    @Test
    fun `WLD-29 start year parses digits only`() {
        assertEquals(1200, WorldTimeEdits.parseYear(" 1200 "))
        assertEquals(-40, WorldTimeEdits.parseYear("-40"))
        assertNull(WorldTimeEdits.parseYear("1200년"))
        assertNull(WorldTimeEdits.parseYear(""))
        assertEquals(1300, WorldTimeEdits.setStartYear(base, 1300).project.worldTime.storyStartYear)
    }
}

/** Hidden units on the locked 연표 (rule 19): a hidden chapter keeps its code only, a story event on a hidden scene reads `숨긴 씬`. */
class LockedTimelineUnitsTest {
    @Test
    fun `hidden chapter and scene are masked while locked`() {
        val base = ExampleProject.build()
        val event = base.index.event(K.EVENT_JOINS_SOCIETY)!!
        val sceneKey = event.sceneKey!!
        val chapter = base.index.ancestors(base.index.unit(sceneKey)!!).last()
        val hidden = base.copy(story = base.story.copy(units = base.story.units.map { if (it.key == chapter.key) it.copy(hidden = true) else it }))
        fun rows(unlocked: Boolean) = TimelineBuilder(hidden, StoryEngine(hidden)).build(LensPoint.Latest, TimelineView.EPISODE, null, unlocked).rows
        val open = rows(true)
        val locked = rows(false)
        val openChapter = open.filterIsInstance<TimelineRow.ChapterRow>().first { it.chapterKey == chapter.key }
        val lockedChapter = locked.filterIsInstance<TimelineRow.ChapterRow>().first { it.chapterKey == chapter.key }
        assertTrue(openChapter.title.contains(chapter.title!!))
        assertFalse(lockedChapter.title.contains(chapter.title!!))
        val lockedEvent = locked.filterIsInstance<TimelineRow.EventRow>().first { it.event.key == K.EVENT_JOINS_SOCIETY }
        assertEquals(EventMasks.HIDDEN_SCENE, lockedEvent.title)
        assertTrue(EventMasks.sceneHidden(hidden, event))
        assertFalse(EventMasks.sceneHidden(base, event))
    }
}
