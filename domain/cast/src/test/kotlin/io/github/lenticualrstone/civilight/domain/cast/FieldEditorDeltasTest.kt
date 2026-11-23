package io.github.lenticualrstone.civilight.domain.cast

import io.github.lenticualrstone.civilight.domain.cast.field.FieldDefinitions
import io.github.lenticualrstone.civilight.domain.cast.field.FieldReader
import io.github.lenticualrstone.civilight.domain.cast.field.FieldSettings
import io.github.lenticualrstone.civilight.domain.cast.field.FieldText
import io.github.lenticualrstone.civilight.domain.cast.field.FieldValueEdits
import io.github.lenticualrstone.civilight.domain.cast.field.NewField
import io.github.lenticualrstone.civilight.domain.cast.field.Profile
import io.github.lenticualrstone.civilight.domain.cast.timed.ReadPoint
import io.github.lenticualrstone.civilight.model.cast.EntityKind
import io.github.lenticualrstone.civilight.model.cast.FieldPayload
import io.github.lenticualrstone.civilight.model.cast.FieldType
import io.github.lenticualrstone.civilight.model.cast.FieldValue
import io.github.lenticualrstone.civilight.model.doc.ProjectDoc
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import io.github.lenticualrstone.civilight.model.sample.ExampleKeys as K

/** U-09 editor deltas (30-cast.md §7: CST-54, CST-55). */
class FieldEditorDeltasTest {
    private val doc = Fx.noahLive()
    private val ctx = Fx.ctx()

    private fun age(d: ProjectDoc): String? {
        val def = Fx.def(d, "나이")
        val value = FieldReader.read(d, EntityKind.CHARACTER, K.RIEN, def, ReadPoint.AtLens(Fx.L1_4)).value
        return value?.let { FieldText.format(d, def, it).text }
    }

    @Test
    fun `TC-F-30 delete confirm lines per kind`() {
        // 세력 `규모` with values on 3 factions, no records, not a filter.
        val (withDef, scale) = FieldDefinitions.add(doc, NewField("규모", FieldType.SHORT_TEXT, entityKind = EntityKind.FACTION), ctx)
        var d = withDef
        val factions = d.world.factions.filter { it.deletedAt == null }.take(3)
        assertTrue(factions.size >= 2)
        for (f in factions) {
            d = FieldValueEdits.setBase(d, EntityKind.FACTION, f.key, scale.key, Fx.text("작음"), ctx)
        }
        val n = factions.size
        assertEquals(listOf("값이 있는 세력 ${n}개"), FieldDefinitions.deleteImpact(d, scale.key).lines())
        assertEquals("값이 있는 세력 ${n}개", FieldDefinitions.deleteSummary(d, scale.key))
        assertEquals("값이 있는 세력 3개", FieldDefinitions.deleteHolders(EntityKind.FACTION, 3))
        assertEquals("값이 있는 장소 4개", FieldDefinitions.deleteHolders(EntityKind.PLACE, 4))

        // 인물 `출생`: holders, live records, `나이` calculating from it.
        val birth = Fx.def(doc, "출생")
        var c = doc
        c = FieldValueEdits.addPointRecord(c, EntityKind.CHARACTER, K.RIEN, birth, FieldValue.of(FieldPayload.Date(1183)), K.EPISODE_1_7, ctx).first
        c = FieldValueEdits.addPointRecord(c, EntityKind.CHARACTER, K.HART, birth, FieldValue.of(FieldPayload.Date(1150)), K.EPISODE_1_6, ctx).first
        val impact = FieldDefinitions.deleteImpact(c, birth.key)
        val holders = FieldDefinitions.valueHolderCount(c, birth.key)
        val records = FieldDefinitions.liveRecordCount(c, birth.key)
        assertTrue(records >= 2)
        assertEquals(
            listOf("값이 있는 인물 ${holders}명", "◆ 변화 기록 $records", "계산 항목 나이 · 직접 넣기로"),
            impact.lines(),
        )
        // A filter field adds `필터에서 빠짐`.
        val species = Fx.def(doc, "종족")
        assertTrue(FieldDefinitions.deleteImpact(doc, species.key).lines().last() == "필터에서 빠짐")
    }

    @Test
    fun `TC-F-31 a calc whose source is in the trash reads as empty, and comes back on restore`() {
        val typedAge = Fx.def(doc, "나이")
        val d = Fx.setField(doc, K.RIEN, typedAge.key, null)
        assertEquals("17세", age(d))
        val birth = Fx.def(d, "출생")

        val deleted = FieldDefinitions.delete(d, birth.key, ctx)
        assertNull(age(deleted))
        assertNull(FieldReader.read(deleted, EntityKind.CHARACTER, K.RIEN, Fx.def(deleted, "나이"), ReadPoint.LATEST).derivation)
        // No value is written while the source is in the trash, and the row no longer names it.
        assertNull(deleted.index.character(K.RIEN)!!.fields[typedAge.key])
        val ageRow = FieldSettings.screen(deleted).rows.single { it.name == "나이" }
        assertTrue(!ageRow.sub.contains("출생에서"))
        // 나이 is an empty field in the overview (`빈 항목 N`).
        assertTrue(Profile.overview(deleted, K.RIEN, Fx.LATEST).emptyCount > Profile.overview(d, K.RIEN, Fx.LATEST).emptyCount)

        val restored = FieldDefinitions.restore(deleted, birth.key, ctx)
        assertEquals("17세", age(restored))
    }

    @Test
    fun `CST-54 order copy`() {
        val screen = FieldSettings.screen(doc)
        assertEquals("순서 바꾸기", screen.reorderAction)
        assertEquals("끌기 · 앞으로 · 뒤로", screen.orderModeNote)
    }
}
