package io.github.lenticualrstone.civilight.feature.script

import io.github.lenticualrstone.civilight.domain.world.common.DocRuns
import io.github.lenticualrstone.civilight.feature.script.state.HeadRow
import io.github.lenticualrstone.civilight.feature.script.state.HeadTitles
import io.github.lenticualrstone.civilight.feature.script.state.ScriptRows
import io.github.lenticualrstone.civilight.domain.script.StoryTimes
import io.github.lenticualrstone.civilight.model.doc.ProjectDoc
import io.github.lenticualrstone.civilight.model.script.ScriptLayers
import io.github.lenticualrstone.civilight.model.script.ScriptPreview
import org.junit.Assert.assertEquals
import org.junit.Test
import io.github.lenticualrstone.civilight.model.sample.ExampleKeys as K

/** QA 2026-11-23 #6: the scene head names the scene like 스토리 and 홈 do (`씬 1 · The Gate`). */
class HeadTitlesTest {
    private fun titled(title: String?): ProjectDoc =
        DocRuns.updateUnit(Fx.example, K.SCENE_1_7_1) { it.copy(title = title) }!!

    @Test
    fun `a titled scene without place or time reads the ordinal and the title only`() {
        assertEquals("씬 1 · The Gate", HeadTitles.of(titled("The Gate"), K.SCENE_1_7_1, "씬 1 · 장소 없음 · 때 없음"))
    }

    @Test
    fun `a titled scene keeps the place and the time it has`() {
        val doc = titled("The Gate")
        assertEquals("씬 1 · The Gate · 결사의 문", HeadTitles.of(doc, K.SCENE_1_7_1, "씬 1 · 결사의 문 · 때 없음"))
        assertEquals("씬 1 · The Gate · 결사의 문 · 2일째 아침 06:10", HeadTitles.of(doc, K.SCENE_1_7_1, "씬 1 · 결사의 문 · 2일째 아침 06:10"))
        assertEquals("씬 1 · The Gate · 2일째 새벽", HeadTitles.of(doc, K.SCENE_1_7_1, "씬 1 · 장소 없음 · 2일째 새벽"))
    }

    @Test
    fun `a flashback keeps its words in the domain's order`() {
        assertEquals("씬 1 · The Gate · 회상 · 하층 구역 · 1195", HeadTitles.of(titled("The Gate"), K.SCENE_1_7_1, "씬 1 · 회상 · 하층 구역 · 1195"))
    }

    @Test
    fun `an untitled or blank-titled scene keeps SCR-46's table`() {
        assertEquals("씬 1 · 장소 없음 · 때 없음", HeadTitles.of(titled(null), K.SCENE_1_7_1, "씬 1 · 장소 없음 · 때 없음"))
        assertEquals("씬 1 · 장소 없음 · 때 없음", HeadTitles.of(titled("  "), K.SCENE_1_7_1, "씬 1 · 장소 없음 · 때 없음"))
    }

    @Test
    fun `a text that is not a head of this scene is left alone`() {
        assertEquals("씬 9 · 어딘가", HeadTitles.of(titled("The Gate"), K.SCENE_1_7_1, "씬 9 · 어딘가"))
    }

    @Test
    fun `the manuscript's head row carries the title`() {
        val doc = titled("The Gate")
        val rows = ScriptRows.build(
            doc, K.EPISODE_1_7, StoryTimes.source(doc), ScriptLayers(), ScriptPreview.WRITE,
            unlocked = false, readOnly = false, zone = Fx.zone,
        )
        val head = rows.filterIsInstance<HeadRow>().first { it.sceneKey == K.SCENE_1_7_1 }
        assertEquals(true, head.title.startsWith("씬 1 · The Gate"))
    }
}
