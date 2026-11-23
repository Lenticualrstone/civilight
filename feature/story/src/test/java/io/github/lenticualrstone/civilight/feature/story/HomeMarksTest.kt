package io.github.lenticualrstone.civilight.feature.story

import io.github.lenticualrstone.civilight.domain.actions.BlockFieldActions
import io.github.lenticualrstone.civilight.domain.actions.MarkActions
import io.github.lenticualrstone.civilight.feature.story.Fx.withUnit
import io.github.lenticualrstone.civilight.feature.story.home.HomeMarks
import io.github.lenticualrstone.civilight.model.Key
import io.github.lenticualrstone.civilight.model.doc.ProjectDoc
import io.github.lenticualrstone.civilight.model.marks.MarkState
import io.github.lenticualrstone.civilight.model.open
import io.github.lenticualrstone.civilight.model.script.BlockKind
import io.github.lenticualrstone.civilight.model.text.TextRun
import io.github.lenticualrstone.civilight.model.sample.ExampleKeys as K
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** (0.2.0) N-41: home `작업` › `할 일 · 복선`: filter counts, rows, the block a row opens, hidden scenes. */
class HomeMarksTest {
    private val ctx = Fx.ctx()

    /** [this] with [blockKey] turned into a memo reading [text] (fixture setup, not a user edit). */
    private fun ProjectDoc.asMemo(blockKey: Key, text: String): ProjectDoc = copy(
        episodes = episodes.mapValues { (_, f) ->
            f.copy(blocks = f.blocks.map { if (it.key == blockKey) it.copy(kind = BlockKind.MEMO.open(), speakerKey = null, runs = listOf(TextRun.Plain(text))) else it })
        },
    )

    /** Two foreshadowings (one paid off) and two memo to-dos (one done). */
    private fun marked(): Triple<ProjectDoc, Key, Key> {
        var doc = Fx.doc()
        val (d1, lamp) = MarkActions.plant(doc, K.BLOCK_044, "꺼진 등불", K.EPISODE_2_5, ctx)!!
        val (d2, seal) = MarkActions.plant(d1, K.BLOCK_043, "결사의 문장", null, ctx)!!
        doc = MarkActions.payoff(d2, seal.key, K.BLOCK_045, ctx)
        doc = doc.asMemo(K.BLOCK_040, "\n하르트 동기 다시 보기\n둘째 줄").asMemo(K.BLOCK_041, "지도 축척")
        doc = BlockFieldActions.setTodo(doc, K.BLOCK_040, true, ctx)
        doc = BlockFieldActions.setTodo(doc, K.BLOCK_041, true, ctx)
        doc = BlockFieldActions.setTodoDone(doc, K.BLOCK_041, true, ctx)
        return Triple(doc, lamp.key, seal.key)
    }

    @Test
    fun filterCountsAndChipLabels() {
        val (doc) = marked()
        val ui = HomeMarks.build(doc, MarkState.OPEN, locked = false)
        assertEquals(listOf("열림 2", "거둠 1", "끝냄 1"), ui.chips.map { it.label })
        assertEquals(listOf(2, 1, 1), ui.chips.map { it.count })
    }

    @Test
    fun openRowsAreThePlantThenTheTodoAndOpenTheirBlocks() {
        val (doc, lamp) = marked()
        val rows = HomeMarks.build(doc, MarkState.OPEN, locked = false).rows
        assertEquals(listOf(lamp, K.BLOCK_040), rows.map { it.key })
        val plant = rows[0]
        assertEquals("복선", plant.kind)
        assertEquals("꺼진 등불", plant.title)
        assertEquals("1-7 씬 3 · 거둘 곳 · 2-5", plant.sub)
        assertEquals(K.BLOCK_044, plant.blockKey)
        val todo = rows[1]
        assertEquals("할 일", todo.kind)
        assertEquals("하르트 동기 다시 보기", todo.title)
        assertEquals(K.BLOCK_040, todo.blockKey)
        assertTrue(todo.sub.startsWith("1-7 씬"))
    }

    @Test
    fun paidOffAndDoneFilters() {
        val (doc, _, seal) = marked()
        assertEquals(listOf(seal), HomeMarks.build(doc, MarkState.PAID_OFF, locked = false).rows.map { it.key })
        val done = HomeMarks.build(doc, MarkState.DONE, locked = false).rows.single()
        assertEquals(K.BLOCK_041, done.key)
        assertEquals("지도 축척", done.title)
    }

    @Test
    fun itemsInAHiddenSceneLeaveTheListAndTheCountsWhileLocked() {
        val (doc) = marked()
        val sceneOf044 = doc.index.block(K.BLOCK_044)!!.sceneKey
        val hidden = doc.withUnit(sceneOf044) { it.copy(hidden = true) }
        val locked = HomeMarks.build(hidden, MarkState.OPEN, locked = true)
        assertTrue(locked.rows.none { it.blockKey == K.BLOCK_044 })
        val open = HomeMarks.build(hidden, MarkState.OPEN, locked = false)
        assertTrue(open.rows.any { it.blockKey == K.BLOCK_044 })
        assertTrue(locked.chips.first().count < open.chips.first().count)
    }

    /** Rule 19: while locked, a visible plant whose `거둘 곳` is a hidden unit drops that part (not even its code). */
    @Test
    fun aPayoffInAHiddenUnitIsNotNamedWhileLocked() {
        val (doc, lamp) = marked()
        val hidden = doc.withUnit(K.EPISODE_2_5) { it.copy(hidden = true) }
        val locked = HomeMarks.build(hidden, MarkState.OPEN, locked = true).rows.single { it.key == lamp }
        assertEquals("1-7 씬 3", locked.sub)
        val open = HomeMarks.build(hidden, MarkState.OPEN, locked = false).rows.single { it.key == lamp }
        assertEquals("1-7 씬 3 · 거둘 곳 · 2-5", open.sub)
    }

    @Test
    fun aProjectWithoutMarksShowsZeroCounts() {
        val ui = HomeMarks.build(Fx.doc(), MarkState.OPEN, locked = true)
        assertEquals(listOf("열림 0", "거둠 0", "끝냄 0"), ui.chips.map { it.label })
        assertTrue(ui.rows.isEmpty())
    }
}
