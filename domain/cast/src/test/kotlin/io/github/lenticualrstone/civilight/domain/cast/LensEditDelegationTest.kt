package io.github.lenticualrstone.civilight.domain.cast

import io.github.lenticualrstone.civilight.domain.cast.field.FieldReader
import io.github.lenticualrstone.civilight.domain.cast.field.FieldValueEdits
import io.github.lenticualrstone.civilight.domain.cast.timed.TimedValues
import io.github.lenticualrstone.civilight.domain.story.lens.EditChoice
import io.github.lenticualrstone.civilight.domain.story.lens.LensEdit
import io.github.lenticualrstone.civilight.model.cast.EntityKind
import io.github.lenticualrstone.civilight.model.change.Anchor
import io.github.lenticualrstone.civilight.model.doc.ProjectDoc
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import io.github.lenticualrstone.civilight.model.sample.ExampleKeys as K

/**
 * `여기서부터 바뀜` on cast values takes its anchor from `domain:story` `LensEdit` (DECISIONS §1),
 * including the case with nothing written, where cast used to fall back to `이 값 고치기`.
 */
class LensEditDelegationTest {
    private val doc = Fx.doc()
    private val affiliation = Fx.def(doc, "소속")
    private val target = FieldReader.target(EntityKind.CHARACTER, K.RIEN, affiliation)

    /** The example with every block removed: no 집필 선. */
    private val nothingWritten: ProjectDoc = doc.copy(episodes = doc.episodes.mapValues { (_, f) -> f.copy(blocks = emptyList()) })

    /** No story units at all. */
    private val noUnits: ProjectDoc = nothingWritten.copy(story = doc.story.copy(units = emptyList()), episodes = emptyMap())

    @Test
    fun anchorIsLensEditsNewAnchor() {
        val missingUnit = Fx.lens(Fx.key("SCNE", 999))
        val cases = listOf(
            doc to Fx.LATEST, doc to Fx.L1_5, nothingWritten to Fx.LATEST, nothingWritten to Fx.L1_5,
            noUnits to Fx.LATEST, doc to missingUnit,
        )
        for ((d, lens) in cases) assertEquals("$lens", LensEdit.newAnchor(d, lens), TimedValues.changeFromHereAnchor(d, lens))
        assertNull(nothingWritten.manuscript.writingLineSceneKey)
        assertEquals(Anchor.UnitStart(K.CHAPTER_1), TimedValues.changeFromHereAnchor(nothingWritten, Fx.LATEST))
    }

    @Test
    fun changeFromHereWithNothingWrittenAddsARecordAtTheFirstUnit() {
        val before = nothingWritten
        val edited = FieldValueEdits.changeFromHere(before, EntityKind.CHARACTER, K.RIEN, affiliation, Fx.text("등불 결사 · 견습"), Fx.LATEST, Fx.ctx())
        val added = edited.changes.records.single { r -> before.changes.records.none { it.key == r.key } }
        assertEquals(Anchor.UnitStart(K.CHAPTER_1), added.anchor)
        // No fallback to `이 값 고치기`: the base value and the existing records stay.
        assertEquals(before.index.character(K.RIEN)!!.fields, edited.index.character(K.RIEN)!!.fields)
        assertEquals(before.changes.records, edited.changes.records - added)
        // The same record LensEdit writes.
        assertEquals(LensEdit.changeFromHere(before, target, Fx.LATEST, added.value, Fx.ctx()).changes.records.last().anchor, added.anchor)
    }

    @Test
    fun sheetWithNothingWrittenOffersTheSecondButtonAsLensEditDoes() {
        val sheet = TimedValues.editSheet(nothingWritten, target, Fx.LATEST, "등불 결사") { "" }
        val fromHere = (LensEdit.choice(nothingWritten, target, Fx.LATEST) as EditChoice.TwoButtons).fromHere
        assertEquals("여기서부터 바뀜 · 전체", sheet.changeLabel)
        assertEquals(fromHere.label, sheet.changeLabel)
        assertEquals("새 ◆", sheet.changeSub)
        assertEquals(fromHere.sub, sheet.changeSub)
    }
}
