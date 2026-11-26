package io.github.lenticualrstone.civilight.feature.script

import io.github.lenticualrstone.civilight.feature.script.synergy.MentionTap
import io.github.lenticualrstone.civilight.feature.script.synergy.SpeakerFocus
import io.github.lenticualrstone.civilight.model.Key
import io.github.lenticualrstone.civilight.model.doc.ProjectDoc
import io.github.lenticualrstone.civilight.model.text.LinkTarget
import io.github.lenticualrstone.civilight.model.text.LinkTargetKind
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** 0.5 cross-review: `@` preview and speaker focus respect rules 19 (hiding) and 20 (deleted items). */
class HiddenMentionAndFocusTest {
    private val doc = Fx.example

    private fun ProjectDoc.hideCharacter(key: Key) =
        copy(cast = cast.copy(characters = cast.characters.map { if (it.key == key) it.copy(hidden = true) else it }))

    private fun ProjectDoc.hideUnit(key: Key) =
        copy(story = story.copy(units = story.units.map { if (it.key == key) it.copy(hidden = true) else it }))

    @Test
    fun `a mention previews only a live target, asks to unlock a hidden one, and cards a deleted or missing one`() {
        val live = doc.cast.characters.first { it.deletedAt == null && !it.hidden }
        val target = LinkTarget(LinkTargetKind.CHARACTER, live.key)
        assertEquals(MentionTap.Action.Preview(LinkTargetKind.CHARACTER), MentionTap.decide(doc, target, unlocked = false))

        val hidden = doc.hideCharacter(live.key)
        assertEquals(MentionTap.Action.Unlock, MentionTap.decide(hidden, target, unlocked = false))
        assertFalse(MentionTap.previewable(hidden, target, unlocked = false))
        assertEquals(MentionTap.Action.Preview(LinkTargetKind.CHARACTER), MentionTap.decide(hidden, target, unlocked = true))

        // Rule 20: a trashed target gets the card with `되살리기`, never the dead-end sheet.
        val trashed = doc.cast.characters.first { it.deletedAt != null }
        assertEquals(MentionTap.Action.Card, MentionTap.decide(doc, LinkTarget(LinkTargetKind.CHARACTER, trashed.key), unlocked = true))
        // Neither row nor tag.
        assertEquals(MentionTap.Action.Card, MentionTap.decide(doc, LinkTarget(LinkTargetKind.CHARACTER, "01JZ0000NOPE00000000000000"), unlocked = true))
    }

    @Test
    fun `live factions still preview (Links state alone would read them as purged)`() {
        val faction = doc.world.factions.firstOrNull { it.deletedAt == null && !it.hidden } ?: return
        assertEquals(MentionTap.Action.Preview(LinkTargetKind.FACTION), MentionTap.decide(doc, LinkTarget(LinkTargetKind.FACTION, faction.key), unlocked = false))
    }

    @Test
    fun `speaker focus leaves out hidden speakers and hidden scenes while locked, and shares add up without them`() {
        val parent = doc.manuscript.liveScenes.mapNotNull { it.parentKey }.distinct()
            .first { SpeakerFocus.stats(doc, it, hiddenUnlocked = false).size >= 2 }
        val all = SpeakerFocus.stats(doc, parent, hiddenUnlocked = false)
        val top = all.first().character

        val hidden = doc.hideCharacter(top.key)
        val locked = SpeakerFocus.stats(hidden, parent, hiddenUnlocked = false)
        assertTrue(locked.none { it.character.key == top.key })
        // The share is taken over what is shown: the percentages are not short by the hidden speaker's part.
        val shownLines = locked.sumOf { it.lines }
        locked.forEach { assertEquals((it.lines * 100f / shownLines).toInt(), it.percent) }
        assertEquals(all.map { it.character.key }.toSet(), SpeakerFocus.stats(hidden, parent, hiddenUnlocked = true).map { it.character.key }.toSet())

        // A hidden episode (the scenes' ancestor) hides every count under it while locked.
        val hiddenParent = doc.hideUnit(parent)
        assertTrue(SpeakerFocus.stats(hiddenParent, parent, hiddenUnlocked = false).isEmpty())
        assertEquals(all.size, SpeakerFocus.stats(hiddenParent, parent, hiddenUnlocked = true).size)
    }
}
