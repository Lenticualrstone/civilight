package io.github.lenticualrstone.civilight.domain.script

import io.github.lenticualrstone.civilight.model.sample.ExampleProject
import io.github.lenticualrstone.civilight.model.script.CueKind
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import io.github.lenticualrstone.civilight.model.sample.ExampleKeys as K

/** SCR-08 cue editing of a `연출 지시` block (`[PROPOSED]` editor, Q-11 effects default). */
class DirectionCuesTest {
    private val doc = ExampleProject.build()
    private val ctx = testCtx()

    @Test
    fun `SCR-08 the manuscript line names cues and effects in Korean`() {
        assertEquals("▸ 배경 하층 거리(새벽) ♪ 잔불", DirectionCues.blockLine(doc, doc.block(K.BLOCK_041).cues))
        assertEquals("▸ 배경 하층 거리(새벽) · ♪ 잔불", DirectionCues.blockLine(doc, doc.block(K.BLOCK_041).cues, " · "))
        assertEquals("화면 효과 페이드 효과음 바람", DirectionCues.blockLine(doc, doc.block(K.block(1, 7, 1)).cues))
        assertEquals("shake", DirectionCues.effectWord("shake"))
    }

    @Test
    fun `SCR-08 add, change and take out cues`() {
        val added = DirectionCues.add(doc, K.BLOCK_041, CueKind.BACKGROUND, K.ASSET_BG_SOCIETY_GATE_MORNING, null, ctx)
        val cues = added.block(K.BLOCK_041).cues
        assertEquals(3, cues.size)
        assertEquals(K.ASSET_BG_SOCIETY_GATE_MORNING, cues.last().assetKey)

        val effect = DirectionCues.add(added, K.BLOCK_041, CueKind.SCREEN_EFFECT, null, "blackout", ctx)
        assertTrue(DirectionCues.blockLine(effect, effect.block(K.BLOCK_041).cues).endsWith("화면 효과 암전"))

        val first = cues.first()
        val changed = DirectionCues.replace(added, K.BLOCK_041, first.key, K.ASSET_BG_ASH_MARKET_NIGHT, null, ctx)
        val after = changed.block(K.BLOCK_041).cues.first()
        assertEquals(first.key, after.key)
        assertEquals(K.ASSET_BG_ASH_MARKET_NIGHT, after.assetKey)

        val removed = DirectionCues.remove(changed, K.BLOCK_041, first.key, ctx)
        assertEquals(2, removed.block(K.BLOCK_041).cues.size)
    }

    @Test
    fun `a cue that does not fit its kind or block changes nothing`() {
        // A BGM asset is not a background.
        assertSame(doc, DirectionCues.add(doc, K.BLOCK_041, CueKind.BACKGROUND, K.ASSET_BGM_EMBERS, null, ctx))
        // A screen effect needs a name; other kinds need an asset.
        assertSame(doc, DirectionCues.add(doc, K.BLOCK_041, CueKind.SCREEN_EFFECT, null, " ", ctx))
        assertSame(doc, DirectionCues.add(doc, K.BLOCK_041, CueKind.CG, null, null, ctx))
        // Not a direction block.
        assertSame(doc, DirectionCues.add(doc, K.BLOCK_043, CueKind.BACKGROUND, K.ASSET_BG_SOCIETY_GATE_MORNING, null, ctx))
        assertSame(doc, DirectionCues.remove(doc, K.BLOCK_041, "nope", ctx))
    }
}
