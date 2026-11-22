package io.github.lenticualrstone.civilight.domain.script

import io.github.lenticualrstone.civilight.model.sample.ExampleProject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Test
import io.github.lenticualrstone.civilight.model.sample.ExampleKeys as K

/** SCR-35 `씬 메모` card: the scene memo edited in place. */
class SceneMemoTest {
    private val doc = ExampleProject.build()
    private val ctx = testCtx()

    @Test
    fun `SCR-35 the card reads the scene memo and writes it back`() {
        assertEquals("등불이 두 번째로 깜박이는 타이밍", SceneMemo.of(doc, K.SCENE_1_7_3))
        val edited = SceneMemo.set(doc, K.SCENE_1_7_3, "등불이 두 번째로 깜박이는 타이밍\n하르트가 먼저 본다\n\n", ctx)
        assertEquals("등불이 두 번째로 깜박이는 타이밍\n하르트가 먼저 본다", edited.index.unit(K.SCENE_1_7_3)?.memo)
        assertEquals(ctx.now, edited.index.unit(K.SCENE_1_7_3)?.updatedAt)
    }

    @Test
    fun `SCR-35 blank clears the memo, unchanged text writes nothing, units above scenes are refused`() {
        assertNull(SceneMemo.of(SceneMemo.set(doc, K.SCENE_1_7_3, "  \n", ctx), K.SCENE_1_7_3))
        assertSame(doc, SceneMemo.set(doc, K.SCENE_1_7_3, "등불이 두 번째로 깜박이는 타이밍", ctx))
        assertSame(doc, SceneMemo.set(doc, K.EPISODE_1_7, "메모", ctx))
    }
}
