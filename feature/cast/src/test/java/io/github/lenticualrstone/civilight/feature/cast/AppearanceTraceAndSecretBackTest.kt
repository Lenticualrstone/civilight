package io.github.lenticualrstone.civilight.feature.cast

import io.github.lenticualrstone.civilight.domain.cast.Appearances
import io.github.lenticualrstone.civilight.feature.cast.profile.AppearanceSceneGroup
import io.github.lenticualrstone.civilight.feature.cast.profile.AppearanceTrace
import io.github.lenticualrstone.civilight.feature.cast.profile.ProfileTabs
import io.github.lenticualrstone.civilight.feature.cast.profile.ScriptAppearanceEntry
import io.github.lenticualrstone.civilight.feature.cast.profile.SecretGate
import io.github.lenticualrstone.civilight.feature.cast.profile.SecretTabGate
import io.github.lenticualrstone.civilight.model.Key
import io.github.lenticualrstone.civilight.model.app.ProfileTabKind
import io.github.lenticualrstone.civilight.model.doc.ProjectDoc
import io.github.lenticualrstone.civilight.model.script.BlockKind
import io.github.lenticualrstone.civilight.model.text.LinkTarget
import io.github.lenticualrstone.civilight.model.text.LinkTargetKind
import io.github.lenticualrstone.civilight.model.text.TextRun
import io.github.lenticualrstone.civilight.navigation.ProfileTab
import io.github.lenticualrstone.civilight.ui.security.AuthResult
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** 0.5 cross-review: `작중 행적` follows the appearance rule and rule 19; `← 돌아가기` relocks the 비밀 view (N-43). */
class AppearanceTraceAndSecretBackTest {
    private val doc = CastFx.doc()

    private fun ProjectDoc.hideUnit(key: Key) =
        copy(story = story.copy(units = story.units.map { if (it.key == key) it.copy(hidden = true) else it }))

    private fun ProjectDoc.hideCharacter(key: Key) =
        copy(cast = cast.copy(characters = cast.characters.map { if (it.key == key) it.copy(hidden = true) else it }))

    @Test
    fun `the trace uses the appearance scenes and drops scenes under a hidden episode while locked`() {
        val (character, scenes) = Appearances.scenesByCharacter(doc, hiddenUnlocked = false).entries.first { it.value.isNotEmpty() }
        val entries = AppearanceTrace.scan(doc, character, hiddenUnlocked = false)
        assertTrue(entries.isNotEmpty())
        // Same scenes, same manuscript order as the bars.
        val sceneOrder = scenes.map { it.key }
        val traced = entries.map { it.sceneKey }.distinct()
        assertEquals(sceneOrder.filter { it in traced }, traced)

        val first = scenes.first()
        val episode = doc.index.ancestors(first).first()
        val hidden = doc.hideUnit(episode.key)
        val underEpisode = doc.manuscript.liveScenes.filter { s -> hidden.index.ancestors(s).any { it.key == episode.key } }.map { it.key }.toSet()
        assertTrue(AppearanceTrace.scan(hidden, character, hiddenUnlocked = false).none { it.sceneKey in underEpisode })
        assertTrue(AppearanceTrace.scan(hidden, character, hiddenUnlocked = true).any { it.sceneKey == first.key })
    }

    @Test
    fun `an @ link to a locked hidden character reads 숨긴 인물 in the excerpt`() {
        val block = doc.allBlocks.first { b ->
            b.deletedAt == null && b.kind.known == BlockKind.DIALOGUE && b.speakerKey != null &&
                doc.manuscript.liveScenes.any { it.key == b.sceneKey }
        }
        val speaker = block.speakerKey!!
        val other = doc.cast.characters.first { it.deletedAt == null && it.key != speaker && !it.hidden }
        val link = TextRun.Link("01JZ0000LINK00000000000001", LinkTarget(LinkTargetKind.CHARACTER, other.key), other.name)
        val withLink = doc.copy(
            episodes = doc.episodes.mapValues { (_, f) ->
                f.copy(blocks = f.blocks.map { if (it.key == block.key) it.copy(runs = listOf(TextRun.Plain("앞 "), link) + it.runs) else it })
            },
        ).hideCharacter(other.key)

        val locked = AppearanceTrace.scan(withLink, speaker, hiddenUnlocked = false).first { it.blockKey == block.key }
        assertTrue(locked.excerpt.contains("숨긴 인물"))
        assertFalse(locked.excerpt.contains(other.name))
        val open = AppearanceTrace.scan(withLink, speaker, hiddenUnlocked = true).first { it.blockKey == block.key }
        assertTrue(open.excerpt.contains(other.name))
    }

    @Test
    fun `a scene with more than 8 rows shows a 더 보기 count instead of cutting silently`() {
        val rows = (1..11).map { ScriptAppearanceEntry("S", "씬", "B$it", isDialogue = true, excerpt = "줄 $it") }
        val group = AppearanceTrace.group(rows).single()
        assertEquals(8, group.visible(expanded = false).size)
        assertEquals(3, group.more(expanded = false))
        assertEquals(11, group.visible(expanded = true).size)
        assertEquals(0, group.more(expanded = true))
        assertEquals(0, AppearanceSceneGroup("S", "씬", rows.take(8)).more(expanded = false))
    }

    @Test
    fun `back from the 비밀 view relocks it, so the next opening asks again`() {
        val start = SecretTabGate.open(ProfileTab.OVERVIEW, gameTools = true, locked = true)
        val opened = SecretTabGate.answered(SecretTabGate.select(start, ProfileTab.SECRET, locked = true), AuthResult.SUCCESS)
        assertEquals(SecretGate.OPEN, opened.gate)

        val back = SecretTabGate.leave(opened, ProfileTabs.gateTab(ProfileTabKind.TRIVIA), locked = true)
        assertEquals(ProfileTab.OVERVIEW, back.tab)
        assertFalse(SecretTabGate.secretSelected(back))
        assertFalse(SecretTabGate.composesBody(ProfileTab.SECRET, back, locked = true))

        val again = SecretTabGate.select(back, ProfileTab.SECRET, locked = true)
        assertEquals(SecretGate.PROMPTING, again.gate)
        assertTrue(again.prompting(locked = true))
    }

    @Test
    fun `a cancelled prompt drops the 비밀 selection and shows the previous tab`() {
        val start = SecretTabGate.select(SecretTabGate.open(ProfileTab.RELATIONS, gameTools = true, locked = true), ProfileTab.SECRET, locked = true)
        assertTrue(SecretTabGate.secretSelected(start))
        val cancelled = SecretTabGate.answered(start, AuthResult.CANCELLED)
        assertFalse(SecretTabGate.secretSelected(cancelled))
        assertEquals(ProfileTab.RELATIONS, cancelled.shown(locked = true))
        assertEquals(ProfileTab.SECRET, ProfileTabs.gateTab(ProfileTabKind.SECRET))
        assertEquals(ProfileTab.OVERVIEW, ProfileTabs.gateTab(ProfileTabKind.APPEARANCES))
    }
}
