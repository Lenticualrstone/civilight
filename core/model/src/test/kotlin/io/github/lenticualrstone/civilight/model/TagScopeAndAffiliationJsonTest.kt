package io.github.lenticualrstone.civilight.model

import io.github.lenticualrstone.civilight.model.cast.Tag
import io.github.lenticualrstone.civilight.model.cast.TagScope
import io.github.lenticualrstone.civilight.model.json.CivilightJson
import io.github.lenticualrstone.civilight.model.project.AffiliationBasis
import io.github.lenticualrstone.civilight.model.project.CastSettings
import io.github.lenticualrstone.civilight.model.project.Project
import io.github.lenticualrstone.civilight.model.world.Faction
import io.github.lenticualrstone.civilight.model.world.Place
import io.github.lenticualrstone.civilight.model.world.Term
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** (2026-11-26) Additive fields: `Tag.scope`, `Term.tagKeys`, faction 거점 and orders, place orders, `묶음 기준`. */
class TagScopeAndAffiliationJsonTest {

    // ---- Tag.scope ----

    @Test
    fun legacyTagReadsAsCharacterScopeAndIsWrittenUnchanged() {
        val text = CivilightJson.encode(Tag.serializer(), Tag(key = "TAG1", name = "주연", order = "V"))
        assertFalse(text.contains("scope"))
        val tag = CivilightJson.decode(Tag.serializer(), text)
        assertTrue(tag.scope isOf TagScope.CHARACTER)
        assertEquals(text, CivilightJson.encode(Tag.serializer(), tag))
    }

    @Test
    fun explicitCharacterScopeIsNotWritten() {
        val tag = Tag(key = "TAG1", name = "주연", order = "V", scope = TagScope.CHARACTER.open())
        assertFalse("scope" in CivilightJson.encodeElement(Tag.serializer(), tag).jsonObject)
    }

    @Test
    fun termAndAllScopesRoundTrip() {
        for (scope in listOf(TagScope.TERM, TagScope.PLACE, TagScope.FACTION, TagScope.ALL)) {
            val tag = Tag(key = "TAG2", name = "광물", order = "V", scope = scope.open())
            val element = CivilightJson.encodeElement(Tag.serializer(), tag).jsonObject
            assertEquals(JsonPrimitive(scope.serialName), element["scope"])
            assertEquals(tag, CivilightJson.decode(Tag.serializer(), CivilightJson.encode(Tag.serializer(), tag)))
        }
        val all = Tag(key = "TAG3", name = "1장 복선", order = "W", scope = TagScope.ALL.open())
        assertTrue(CivilightJson.encode(Tag.serializer(), all).contains("\"scope\":\"all\""))
    }

    @Test
    fun unknownScopeSurvives() {
        val text = CivilightJson.encode(Tag.serializer(), Tag(key = "TAG4", name = "미래", order = "V"))
            .removeSuffix("}") + ""","scope":"somethingNew"}"""
        val tag = CivilightJson.decode(Tag.serializer(), text)
        assertTrue(tag.scope.isUnknown)
        assertEquals("somethingNew", tag.scope.raw)
        assertEquals(text, CivilightJson.encode(Tag.serializer(), tag))
    }

    // ---- Term.tagKeys ----

    @Test
    fun termTagKeysRoundTrip() {
        val term = Term(key = "T1", order = "V", name = "오리지늄 등불", tagKeys = listOf("TAG2", "TAG3"))
        val back = CivilightJson.decode(Term.serializer(), CivilightJson.encode(Term.serializer(), term))
        assertEquals(listOf("TAG2", "TAG3"), back.tagKeys)
        assertEquals(term, back)
    }

    @Test
    fun oldTermLoadsWithNoTagsAndIsWrittenUnchanged() {
        val old = CivilightJson.encode(Term.serializer(), Term(key = "T1", order = "V", name = "오리지늄 등불"))
        assertFalse(old.contains("tagKeys"))
        val read = CivilightJson.decode(Term.serializer(), old)
        assertEquals(emptyList<Key>(), read.tagKeys)
        assertEquals(old, CivilightJson.encode(Term.serializer(), read))
    }

    // ---- Faction.baseKey / memberOrder ----

    @Test
    fun factionBaseAndMemberOrderRoundTrip() {
        val faction = Faction(key = "F1", order = "V", name = "등불 결사", baseKey = "PL1", memberOrder = listOf("C2", "C1"))
        val element = CivilightJson.encodeElement(Faction.serializer(), faction).jsonObject
        assertEquals(JsonPrimitive("PL1"), element["baseKey"])
        assertEquals(faction, CivilightJson.decode(Faction.serializer(), CivilightJson.encode(Faction.serializer(), faction)))
    }

    @Test
    fun oldFactionLoadsWithDefaultsAndIsWrittenUnchanged() {
        val old = CivilightJson.encode(Faction.serializer(), Faction(key = "F1", order = "V", name = "등불 결사"))
        assertFalse(old.contains("baseKey") || old.contains("memberOrder"))
        val read = CivilightJson.decode(Faction.serializer(), old)
        assertNull(read.baseKey)
        assertEquals(emptyList<Key>(), read.memberOrder)
        assertEquals(old, CivilightJson.encode(Faction.serializer(), read))
    }

    // ---- Place.factionOrder / looseOrder ----

    @Test
    fun placeOrdersRoundTrip() {
        val place = Place(key = "PL1", order = "V", name = "결사의 문", factionOrder = listOf("F2", "F1"), looseOrder = listOf("C3"))
        val back = CivilightJson.decode(Place.serializer(), CivilightJson.encode(Place.serializer(), place))
        assertEquals(listOf("F2", "F1"), back.factionOrder)
        assertEquals(listOf("C3"), back.looseOrder)
        assertEquals(place, back)
    }

    @Test
    fun oldPlaceLoadsWithEmptyOrdersAndIsWrittenUnchanged() {
        val old = CivilightJson.encode(Place.serializer(), Place(key = "PL1", order = "V", name = "결사의 문"))
        assertFalse(old.contains("factionOrder") || old.contains("looseOrder"))
        val read = CivilightJson.decode(Place.serializer(), old)
        assertEquals(emptyList<Key>(), read.factionOrder)
        assertEquals(emptyList<Key>(), read.looseOrder)
        assertEquals(old, CivilightJson.encode(Place.serializer(), read))
    }

    // ---- CastSettings.affiliationBasis ----

    @Test
    fun affiliationBasisRoundTrip() {
        val basis = AffiliationBasis(affiliationFieldKey = "FD1", residenceFieldKey = "FD2", nationalityFieldKey = null)
        val project = Project(key = "P1", name = "등불", cast = CastSettings(affiliationBasis = basis))
        val back = CivilightJson.decode(Project.serializer(), CivilightJson.encode(Project.serializer(), project))
        assertEquals(basis, back.cast.affiliationBasis)
        assertEquals(project, back)
        val written = CivilightJson.encodeElement(Project.serializer(), project).jsonObject
            .getValue("cast").jsonObject.getValue("affiliationBasis").jsonObject
        assertFalse("nationalityFieldKey" in written)
    }

    @Test
    fun oldProjectLoadsWithUnusedBasisAndIsWrittenUnchanged() {
        val old = CivilightJson.encode(Project.serializer(), Project(key = "P1", name = "등불"))
        assertFalse(old.contains("affiliationBasis"))
        val read = CivilightJson.decode(Project.serializer(), old)
        assertEquals(AffiliationBasis(), read.cast.affiliationBasis)
        assertNull(read.cast.affiliationBasis.affiliationFieldKey)
        assertNull(read.cast.affiliationBasis.residenceFieldKey)
        assertNull(read.cast.affiliationBasis.nationalityFieldKey)
        assertEquals(old, CivilightJson.encode(Project.serializer(), read))
    }

    @Test
    fun unknownScopeKeysHeldStayUntouchedOnEdits() {
        val base = io.github.lenticualrstone.civilight.model.sample.ExampleProject.build()
        val key = base.project.tags.first().key
        val future = OpenEnum.unknown<TagScope>("somethingNew")
        val doc = base.copy(project = base.project.copy(tags = base.project.tags.map { if (it.key == key) it.copy(scope = future) else it }))
        val tags = io.github.lenticualrstone.civilight.model.cast.TagScopes
        assertTrue(tags.keepsHeld(doc.index.tag(key)!!))
        assertEquals(listOf(key), tags.settle(doc, TagScope.TERM, emptyList(), listOf(key)))
        assertEquals(emptyList<Key>(), tags.settle(doc, TagScope.TERM, listOf(key), emptyList()))
        assertEquals(listOf(key), tags.settleInPickOrder(doc, TagScope.CHARACTER, emptyList(), listOf(key), keepMissing = true))
        assertEquals(emptyList<Key>(), tags.settleInPickOrder(doc, TagScope.CHARACTER, listOf(key), emptyList(), keepMissing = true))
    }
}
