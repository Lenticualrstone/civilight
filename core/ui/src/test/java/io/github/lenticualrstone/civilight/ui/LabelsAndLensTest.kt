package io.github.lenticualrstone.civilight.ui

import io.github.lenticualrstone.civilight.designsystem.component.LensChipState
import io.github.lenticualrstone.civilight.model.app.LensState
import io.github.lenticualrstone.civilight.model.position.LensPoint
import io.github.lenticualrstone.civilight.model.sample.ExampleKeys
import io.github.lenticualrstone.civilight.model.sample.ExampleProject
import io.github.lenticualrstone.civilight.ui.labels.UnitLabels
import io.github.lenticualrstone.civilight.ui.lens.LensCopy
import io.github.lenticualrstone.civilight.ui.lens.LensRows
import io.github.lenticualrstone.civilight.ui.lens.LensRules
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class LabelsAndLensTest {
    private val doc = ExampleProject.build()
    private val labels = UnitLabels(doc)

    @Test
    fun `unit codes follow STO-03`() {
        val scene = doc.index.unit(ExampleKeys.SCENE_1_7_3)!!
        assertEquals("씬 3", labels.code(scene))
        assertEquals("1-7 씬 3", labels.reference(scene))
        assertEquals("1-7 씬 3 · 꺼진 등불", labels.referenceWithTitle(scene))
        assertEquals("1-7", labels.code(ExampleKeys.EPISODE_1_7))
        assertEquals("1장", labels.code(ExampleKeys.CHAPTER_1))
        assertEquals("Ⅰ 1장 · 등불지기", labels.header(doc.index.unit(ExampleKeys.CHAPTER_1)!!))
        assertEquals(ExampleKeys.EPISODE_1_7, labels.episodeOf(scene)?.key)
    }

    @Test
    fun `trashed units have no code and do not count`() {
        assertNull(labels.code(ExampleKeys.SCENE_1_6_5))
        val sixth = doc.index.children(ExampleKeys.EPISODE_1_6).filter { doc.liveness.isLive(it) }
        assertEquals(sixth.indices.map { "씬 ${it + 1}" }, sixth.map { labels.code(it) })
    }

    @Test
    fun `roman numerals`() {
        assertEquals("Ⅱ", UnitLabels.roman(2))
        assertEquals("Ⅻ", UnitLabels.roman(12))
        assertEquals("XIV", UnitLabels.roman(14))
    }

    @Test
    fun `lens chip reads 최신 · 1-7 at the writing line and 시점 at a point`() {
        val latest = LensRules.chipLabel(doc, LensPoint.Latest)
        assertEquals("최신 · 1-7", latest.text)
        assertEquals(LensChipState.LATEST, latest.state)
        assertEquals("시점 · 1-5", LensRules.chipLabel(doc, LensPoint.UnitEnd(ExampleKeys.EPISODE_1_5)).text)
        val scenePoint = LensRules.chipLabel(doc, LensPoint.UnitEnd(ExampleKeys.scene(1, 7, 2)))
        assertEquals("시점 · 1-7 씬 2", scenePoint.text)
        assertEquals(LensChipState.PAST, scenePoint.state)
        // A trashed or unknown unit reads as 최신.
        assertEquals("최신 · 1-7", LensRules.chipLabel(doc, LensPoint.UnitEnd(ExampleKeys.SCENE_1_6_5)).text)
        assertEquals("최신 · 1-7", LensRules.chipLabel(doc, LensPoint.UnitEnd("gone")).text)
    }

    @Test
    fun `the script has no lens - STO-16`() {
        val past = LensState(point = LensPoint.UnitEnd(ExampleKeys.EPISODE_1_5))
        val entered = LensRules.enterScript(past.copy(savedForScript = LensPoint.Latest))
        assertEquals(past, entered)
        // Leaving without 렌즈를 이 씬으로 맞추기 keeps the stored lens.
        assertEquals(past, LensRules.leaveScript(entered))
        // With it, the scene point applies on leaving.
        val aligned = LensRules.setLensToScene(entered, ExampleKeys.SCENE_1_7_3)
        assertEquals(past.point, aligned.point)
        assertEquals(LensState(point = LensPoint.UnitEnd(ExampleKeys.SCENE_1_7_3)), LensRules.leaveScript(aligned))
    }

    @Test
    fun `lens sheet rows start with 최신 and show the first unwritten episode`() {
        val model = LensRows(doc, labels)
        val rows = model.rows(LensPoint.Latest)
        assertEquals(LensCopy.LATEST, rows.first().code)
        assertEquals(LensCopy.LATEST_SUB, rows.first().sub)
        val eighth = rows.single { it.code == "1-8" }
        assertTrue(eighth.notWritten)
        assertEquals(LensCopy.NOT_WRITTEN, eighth.sub)
        val written = rows.filter { !it.notWritten && it.point != LensPoint.Latest }
        assertTrue(written.isNotEmpty())
        assertTrue(written.all { it.sub != LensCopy.NOT_WRITTEN })
        assertFalse(model.isNotWritten(doc.index.unit(ExampleKeys.EPISODE_1_7)!!))
        // Rows are in manuscript order.
        val order = model.episodes.map { it.key }
        val episodeRows = rows.drop(1).map { (it.point as LensPoint.UnitEnd).unitKey }
        assertEquals(episodeRows, episodeRows.sortedBy { order.indexOf(it) })
    }

    @Test
    fun `a viewed scene point gets its own row and neighbours surround an episode`() {
        val model = LensRows(doc, labels)
        val viewed = LensPoint.UnitEnd(ExampleKeys.scene(1, 7, 2))
        val rows = model.rows(viewed)
        assertTrue(rows.any { it.point == viewed && it.code == "1-7 씬 2" })
        assertTrue(rows.any { it.code == "1-7" })
        assertEquals(listOf("1-6", "1-7", "1-8"), model.neighbours(ExampleKeys.EPISODE_1_7).map { labels.code(it) })
        assertEquals(listOf("1-7 씬 1", "1-7 씬 2", "1-7 씬 3", "1-7 씬 4"), model.sceneRows(ExampleKeys.EPISODE_1_7).map { it.code })
    }
}
