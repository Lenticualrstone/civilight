package io.github.lenticualrstone.civilight.feature.world.term

import io.github.lenticualrstone.civilight.domain.world.item.CreateResult
import io.github.lenticualrstone.civilight.domain.world.item.WorldItems
import io.github.lenticualrstone.civilight.domain.world.term.RevealTarget
import io.github.lenticualrstone.civilight.domain.world.term.TermEdits
import io.github.lenticualrstone.civilight.feature.world.WorldTestFixture
import io.github.lenticualrstone.civilight.feature.world.WorldTestFixture.K
import io.github.lenticualrstone.civilight.feature.world.WorldTestFixture.PROJECT
import io.github.lenticualrstone.civilight.feature.world.WorldTestFixture.T1
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** `공개 시점 옮기기` (WLD-18): choices and what choosing does. */
class RevealOptionsTest {
    @Test
    fun `WLD-18 the link rows list the term's manuscript links in order, the reveal block current`() {
        val rows = RevealOptions.links(WorldTestFixture.stateA(), T1, hiddenOpen = false)
        assertEquals(2, rows.size)
        assertTrue(rows[0].label.startsWith("1-4 씬 2 · 세라 · “"))
        assertEquals(RevealTarget.Block(K.BLOCK_B42), rows[0].target)
        assertEquals(false, rows[0].current)
        assertTrue(rows[1].label.startsWith("1-7 씬 3"))
        assertEquals(true, rows[1].current)
    }

    @Test
    fun `WLD-18 block rows of a scene are its game-output blocks with their numbers`() {
        val scene = WorldTestFixture.stateA().index.block(K.BLOCK_044)!!.sceneKey
        val rows = RevealOptions.blocks(WorldTestFixture.stateA(), T1, scene)
        assertTrue(rows.isNotEmpty())
        val current = rows.single { it.current }
        assertEquals(RevealTarget.Block(K.BLOCK_044), current.target)
        assertTrue(current.label.startsWith("#044"))
    }

    @Test
    fun `WLD-T35 choosing the suggested block sets the first reveal with its toast`() {
        val ctx = WorldTestFixture.ctx()
        val created = WorldItems.createTerm(WorldTestFixture.stateA(), "검은 등잔", ctx) as CreateResult.Created
        val doc = WorldTestFixture.appendRun(created.doc, K.BLOCK_B12, WorldTestFixture.link(ctx.newKey(), created.key, "검은 등잔"))
        val applied = RevealOptions.apply(doc, created.key, RevealTarget.Block(K.BLOCK_B12), ctx) as RevealApply.Applied
        assertEquals("◇ 첫 공개 1-2", applied.toast)
        val ui = TermCardState.build(applied.doc, PROJECT, created.key, WorldTestFixture.index(applied.doc), false, false)
        assertTrue(ui.reveal!!.value.startsWith("1-2 씬 1 · #"))
        assertTrue(ui.groups[0].rows.single().markers.any { it.text == "◇ 첫 공개" })
    }

    @Test
    fun `WLD-T38 an option line is refused with the reason`() {
        val doc = WorldTestFixture.stateA()
        val choice = doc.index.block(K.BLOCK_1_5_CHOICE)!!
        val option = choice.options.first()
        val refused = RevealOptions.apply(doc, T1, RevealTarget.OptionLine(choice.key, option.key), WorldTestFixture.ctx())
        assertEquals(RevealApply.Refused(TermEdits.OPTION_REVEAL_REFUSED), refused)
        assertEquals("선택지 보기 줄에는 첫 공개를 걸 수 없음", TermEdits.OPTION_REVEAL_REFUSED)
    }

    @Test
    fun `WLD-18 moving the reveal to a later point names the move and the new warnings (WLD-T39 toast pattern)`() {
        val doc = WorldTestFixture.stateA()
        val applied = RevealOptions.apply(doc, T1, RevealTarget.Unit(K.episode(2, 5)), WorldTestFixture.ctx()) as RevealApply.Applied
        assertEquals("◇ 첫 공개 1-7 → 2-5 · ▲ 새로 1", applied.toast)
    }

    @Test
    fun `point levels reach down to episodes, scene picker shows scenes`() {
        val doc = WorldTestFixture.stateA()
        val structure = doc.project.structure
        assertEquals((0..structure.episodeLevel!!).toSet(), RevealOptions.pointLevels(doc))
        assertEquals(setOf(structure.sceneLevel), RevealOptions.sceneLevels(doc))
    }
}
