package io.github.lenticualrstone.civilight.feature.world.time

import io.github.lenticualrstone.civilight.domain.world.time.TimelineView
import io.github.lenticualrstone.civilight.feature.world.WorldTestFixture
import io.github.lenticualrstone.civilight.model.open
import io.github.lenticualrstone.civilight.model.project.SceneNotation
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** WLD-34 default view, WLD-35 ages on rows. */
class TimelineViewTest {
    @Test
    fun `WLD-34 the 연표 opens in the project's 때 표기`() {
        val doc = WorldTestFixture.stateB()
        assertEquals(TimelineView.EPISODE, TimelineState.defaultView(doc))
        val storyStart = doc.copy(project = doc.project.copy(sceneTime = doc.project.sceneTime.copy(notation = SceneNotation.STORY_START.open())))
        assertEquals(TimelineView.STORY_START, TimelineState.defaultView(storyStart))
    }

    @Test
    fun `WLD-T48 rows carry the ages at their time`() {
        val ui = TimelineState.build(WorldTestFixture.stateB(), TimelineView.EPISODE, null, hiddenOpen = false)
        assertTrue(ui.rows.toString(), ui.rows.any { it.sub.contains("리엔 12세") })
        assertTrue(ui.rows.toString(), ui.rows.any { it.sub.contains("리엔 17세") && it.sub.contains("하르트 ??세") })
    }
}
