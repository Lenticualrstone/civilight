package io.github.lenticualrstone.civilight.feature.script

import io.github.lenticualrstone.civilight.designsystem.component.ComposerTab
import io.github.lenticualrstone.civilight.domain.actions.ActionContext
import io.github.lenticualrstone.civilight.domain.script.WriteDraft
import io.github.lenticualrstone.civilight.feature.script.input.Draft
import io.github.lenticualrstone.civilight.feature.script.input.Drafts
import io.github.lenticualrstone.civilight.feature.script.input.Drafts.SendIntent
import io.github.lenticualrstone.civilight.feature.script.input.ReadAloudNav
import io.github.lenticualrstone.civilight.model.script.BlockKind
import io.github.lenticualrstone.civilight.model.text.TextRun
import io.github.lenticualrstone.civilight.model.text.plainText
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import io.github.lenticualrstone.civilight.model.sample.ExampleKeys as K

/** (0.7.0) Pure JVM tests for 3-mode composer shorthands, multi-speaker, IF branches, and TTS rehearsal helpers. */
class Script070FeatureTest {
    private val doc get() = Fx.example

    private fun typed(d: Draft, text: String): Draft = Drafts.textChanged(d, text, text.length, text.length).first

    @Test
    fun `inline shorthand parses comment memo and triple-dash scene break`() {
        val memo = Drafts.sendIntent(typed(Draft(kind = BlockKind.NARRATION), "// 나중에 다듬기"))
        assertEquals(SendIntent.Memo(listOf(TextRun.Plain("나중에 다듬기"))), memo)

        val blankScene = Drafts.sendIntent(typed(Draft(), "---"))
        assertEquals(SendIntent.NewScene(null), blankScene)

        val titledScene = Drafts.sendIntent(typed(Draft(), "--- 밤의 광장"))
        assertEquals(SendIntent.NewScene("밤의 광장"), titledScene)

        val sceneTab = Drafts.sendIntent(typed(Drafts.selectComposerTab(Draft(), ComposerTab.SCENE), "해질녘 골목"))
        assertEquals(SendIntent.NewScene("해질녘 골목"), sceneTab)
    }

    @Test
    fun `multi-speaker toggle adds and removes co-speakers cleanly`() {
        val other = doc.characters.first { it.key != K.RIEN }.key
        val base = Draft(kind = BlockKind.DIALOGUE, speakerKey = K.RIEN)
        val withCo = Drafts.toggleCoSpeaker(base, other)
        assertEquals(K.RIEN, withCo.speakerKey)
        assertEquals(listOf(other), withCo.coSpeakerKeys)

        val removedCo = Drafts.toggleCoSpeaker(withCo, other)
        assertEquals(K.RIEN, removedCo.speakerKey)
        assertTrue(removedCo.coSpeakerKeys.isEmpty())

        // Removing primary promotes first co-speaker
        val promoted = Drafts.toggleCoSpeaker(withCo, K.RIEN)
        assertEquals(other, promoted.speakerKey)
        assertTrue(promoted.coSpeakerKeys.isEmpty())
    }

    @Test
    fun `episode branch creates, appends, shows context, and adopts with version backup`() {
        val beforeVersions = doc.versions[K.EPISODE_1_7]?.versions?.size ?: 0
        val (d1, branch) = BranchOps.create(doc, K.EPISODE_1_7, K.SCENE_1_7_3, K.BLOCK_044, now = 1000L)
        assertEquals(1, BranchOps.branchesOf(d1, K.EPISODE_1_7).size)
        assertTrue(BranchOps.contextBlocks(d1, branch).any { it.key == K.BLOCK_044 })

        val appended = BranchOps.appendBlock(
            d1,
            K.EPISODE_1_7,
            branch.key,
            WriteDraft(kind = BlockKind.DIALOGUE, runs = listOf(TextRun.Plain("다른 전개의 대사")), speakerKey = K.RIEN),
            now = 2000L,
            device = "test",
        )
        assertNotNull(appended)
        val d2 = appended!!.first
        assertEquals(1, BranchOps.branchOf(d2, K.EPISODE_1_7, branch.key)!!.blocks.size)

        val adopted = BranchOps.adoptToMain(d2, K.EPISODE_1_7, branch.key, Fx.ctx(), ActionContext(now = 3000L, device = "test"))
        assertNotNull(adopted)
        // Branch removed after adoption
        assertNull(BranchOps.branchOf(adopted!!, K.EPISODE_1_7, branch.key))
        // Previous main route backed up to EpisodeVersions
        val afterVersions = adopted.versions[K.EPISODE_1_7]?.versions?.size ?: 0
        assertEquals(beforeVersions + 1, afterVersions)
        // Adopted dialogue is now live in the main scene
        assertTrue(adopted.manuscript.liveBlocksOf(K.SCENE_1_7_3).any { it.runs.plainText == "다른 전개의 대사" })
    }

    @Test
    fun `rehearsal speed cycles and pitch casting is deterministic within range`() {
        assertEquals(1.5f, ReadAloudNav.nextSpeed(1.0f), 0.01f)
        assertEquals(2.0f, ReadAloudNav.nextSpeed(1.5f), 0.01f)
        assertEquals(0.5f, ReadAloudNav.nextSpeed(2.0f), 0.01f)
        assertEquals("1.5×", ReadAloudNav.speedLabel(1.5f))

        val p1 = ReadAloudNav.speakerPitch("리엔")
        val p2 = ReadAloudNav.speakerPitch("리엔")
        assertEquals(p1, p2, 0.0001f)
        assertTrue(p1 in 0.85f..1.20f)
    }
}
