package io.github.lenticualrstone.civilight.domain.story

import io.github.lenticualrstone.civilight.domain.story.Fx.S5
import io.github.lenticualrstone.civilight.domain.story.Fx.S9
import io.github.lenticualrstone.civilight.domain.story.scene.Appearance
import io.github.lenticualrstone.civilight.domain.story.scene.AppearanceEdit
import io.github.lenticualrstone.civilight.domain.story.time.FlashbackLines
import io.github.lenticualrstone.civilight.model.open
import io.github.lenticualrstone.civilight.model.sample.ExampleKeys as K
import io.github.lenticualrstone.civilight.model.story.SceneTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Test

/** STO-26 silent-cast edits and the STO-24 `회상 건너뜀` line (additions for feature:story). */
class AppearanceEditTest {
    private val doc = Fx.doc()
    private val ctx = Fx.ctx()

    @Test
    fun addAndRemoveASilentMember_STO26() {
        val added = AppearanceEdit.addSilent(doc, S9, K.TEO, ctx)
        assertEquals(listOf(K.MARTHA, K.TEO), added.index.unit(S9)!!.silentCast)
        assertEquals(listOf(K.MARTHA, K.TEO), Appearance.of(added, S9).silent)
        val removed = AppearanceEdit.removeSilent(added, S9, K.TEO, ctx)
        assertEquals(listOf(K.MARTHA), removed.index.unit(S9)!!.silentCast)
    }

    @Test
    fun speakersDuplicatesTrashedCharactersAndGoneScenesAreNoOps() {
        val speaker = Appearance.of(doc, S9).speakers.first()
        assertSame(doc, AppearanceEdit.addSilent(doc, S9, speaker, ctx))
        assertSame(doc, AppearanceEdit.addSilent(doc, S9, K.MARTHA, ctx))
        assertSame(doc, AppearanceEdit.addSilent(doc, S9, K.NOAH, ctx))
        assertSame(doc, AppearanceEdit.addSilent(doc, K.SCENE_1_6_5, K.TEO, ctx))
        assertSame(doc, AppearanceEdit.addSilent(doc, K.EPISODE_1_7, K.TEO, ctx))
        assertSame(doc, AppearanceEdit.removeSilent(doc, S9, K.TEO, ctx))
    }

    @Test
    fun aDeletedSilentMemberCanBeRemoved_R20() {
        // 노아 (in the trash) is in 1-2 씬 2's silent cast; `빼기` takes him out.
        val removed = AppearanceEdit.removeSilent(doc, K.SCENE_1_2_2, K.NOAH, ctx)
        assertEquals(emptyList<String>(), removed.index.unit(K.SCENE_1_2_2)!!.silentCast)
    }

    @Test
    fun aDirectFlashbackDoesNotNameItsKeptEvent_STO24() {
        val t = doc.index.unit(S5)!!.time as SceneTime.Flashback
        val direct = doc.copy(
            story = doc.story.copy(
                units = doc.story.units.map {
                    if (it.key == S5) it.copy(time = t.copy(mode = io.github.lenticualrstone.civilight.model.story.FlashbackMode.DIRECT.open(), year = 1190)) else it
                },
            ),
        )
        assertEquals("연도 1190" to "이야기 시작 10년 전", Fx.labels(direct).flashbackResult(S5))
        assertEquals("연도 1195 · 대정전 그날" to "이야기 시작 5년 전", Fx.labels(doc).flashbackResult(S5))
    }

    @Test
    fun skipLineNamesTheNextScenesBase_STO24() {
        assertEquals("씬 3 때 기준" to "1-3 씬 2 · 회상 건너뜀", FlashbackLines.skipLine(doc, S5))
        assertNull(FlashbackLines.skipLine(doc, S9))
        // The next scene untimed: no line.
        val untimed = doc.copy(story = doc.story.copy(units = doc.story.units.map { if (it.key == K.SCENE_1_4_3) it.copy(time = SceneTime.None) else it }))
        assertNull(FlashbackLines.skipLine(untimed, S5))
    }
}
