package io.github.lenticualrstone.civilight.domain.script

import io.github.lenticualrstone.civilight.model.sample.ExampleProject
import org.junit.Assert.assertEquals
import org.junit.Test
import io.github.lenticualrstone.civilight.model.sample.ExampleKeys as K

/** SCR-58 (rule 19): `이 씬 분량` never names a locked hidden speaker. */
class SceneSpeakersHiddenTest {
    private val doc = ExampleProject.build()

    @Test
    fun `T-19-01 locked hidden speakers are counted together as 숨긴 인물`() {
        val hidden = doc.copy(cast = doc.cast.copy(characters = doc.cast.characters.map { if (it.key == K.HART) it.copy(hidden = true) else it }))
        val open = WritingCounts.sceneSpeakers(hidden, K.SCENE_1_7_3)
        assertEquals(open, WritingCounts.sceneSpeakers(hidden, K.SCENE_1_7_3, unlocked = true))
        val locked = WritingCounts.sceneSpeakers(hidden, K.SCENE_1_7_3, unlocked = false)
        assertEquals(open.replace("하르트", "숨긴 인물").split(" · ").toSet(), locked.split(" · ").toSet())
        // Nothing hidden: the same line either way.
        assertEquals(WritingCounts.sceneSpeakers(doc, K.SCENE_1_7_3), WritingCounts.sceneSpeakers(doc, K.SCENE_1_7_3, unlocked = false))
    }
}
