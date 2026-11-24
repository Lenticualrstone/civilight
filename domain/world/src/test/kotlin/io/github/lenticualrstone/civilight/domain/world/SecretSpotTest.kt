package io.github.lenticualrstone.civilight.domain.world

import io.github.lenticualrstone.civilight.domain.world.WorldFixture.T1
import io.github.lenticualrstone.civilight.domain.world.item.WorldCards
import io.github.lenticualrstone.civilight.domain.world.term.OutsideSpot
import io.github.lenticualrstone.civilight.model.OrderKeys
import io.github.lenticualrstone.civilight.model.cast.EntityKind
import io.github.lenticualrstone.civilight.model.cast.FieldDef
import io.github.lenticualrstone.civilight.model.cast.FieldLinkTargetKind
import io.github.lenticualrstone.civilight.model.cast.FieldPayload
import io.github.lenticualrstone.civilight.model.cast.FieldType
import io.github.lenticualrstone.civilight.model.cast.FieldValue
import io.github.lenticualrstone.civilight.model.open
import io.github.lenticualrstone.civilight.model.text.DocBlock
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** (0.2.0 CST-73) A character's secret text is left out of term readings while `이 탭 잠그기` is on. */
class SecretSpotTest {
    @Test
    fun `secret body links are listed only while the tab is not locked`() {
        val base = WorldFixture.stateB()
        val c = base.cast.characters.first { it.deletedAt == null }
        val body = listOf(DocBlock(key = WorldFixture.key(901), order = "a", runs = listOf(WorldFixture.link(WorldFixture.key(902), T1, "빛돌"))))
        fun withChar(locked: Boolean) = base.copy(
            cast = base.cast.copy(characters = base.cast.characters.map { if (it.key == c.key) it.copy(secretBody = body, secretTabLocked = locked) else it }),
        )
        fun secretListed(locked: Boolean) = WorldFixture.queries(withChar(locked)).outside(T1)
            .any { (it.spot as? OutsideSpot.CharacterBody)?.let { s -> s.secret && s.characterKey == c.key } == true }
        assertTrue(secretListed(locked = false))
        assertFalse(secretListed(locked = true))
    }

    @Test
    fun `secret link fields of a locked character are left out of reverse link lists`() {
        val base = WorldFixture.stateB()
        val c = base.cast.characters.first { it.deletedAt == null && !it.hidden }
        val faction = base.world.factions.first { it.deletedAt == null && !it.hidden }
        val defKey = WorldFixture.key(911)
        val def = FieldDef(
            key = defKey,
            entityKind = EntityKind.CHARACTER.open(),
            name = "내통",
            type = FieldType.LINK.open(),
            order = OrderKeys.after(base.project.fieldDefs.maxOf { it.order }),
            isSecret = true,
            linkTargetKind = FieldLinkTargetKind.FACTION.open(),
        )
        fun doc(locked: Boolean, secret: Boolean = true) = base.copy(
            project = base.project.copy(fieldDefs = base.project.fieldDefs + def.copy(isSecret = secret)),
            cast = base.cast.copy(
                characters = base.cast.characters.map {
                    if (it.key == c.key) {
                        it.copy(secretTabLocked = locked, fields = it.fields + (defKey to FieldValue.of(FieldPayload.Link(listOf(faction.key)))))
                    } else {
                        it
                    }
                },
            ),
        )
        fun linked(d: io.github.lenticualrstone.civilight.model.doc.ProjectDoc) =
            WorldCards(d).linkedCharacters(faction.key).filter { it.label.endsWith("내통") }
        fun count(d: io.github.lenticualrstone.civilight.model.doc.ProjectDoc) =
            WorldCards(d).factionList().sections.flatMap { it.rows }.first { it.key == faction.key }.right

        assertEquals(1, linked(doc(locked = false)).size)
        assertTrue(linked(doc(locked = true)).isEmpty())
        assertTrue(WorldCards(doc(locked = true)).factionCard(faction.key)!!.linkedCharacters.none { it.label.endsWith("내통") })
        // A plain (not secret) link field stays listed while the tab is locked.
        assertEquals(1, linked(doc(locked = true, secret = false)).size)
        // The faction row count drops the hidden link.
        assertEquals(
            WorldCards(base).factionList().sections.flatMap { it.rows }.first { it.key == faction.key }.right,
            count(doc(locked = true)),
        )
    }
}
