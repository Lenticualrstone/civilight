package io.github.lenticualrstone.civilight.feature.cast

import io.github.lenticualrstone.civilight.domain.cast.export.CharacterExport
import io.github.lenticualrstone.civilight.domain.cast.field.FieldDefinitions
import io.github.lenticualrstone.civilight.domain.cast.field.FieldScope
import io.github.lenticualrstone.civilight.domain.cast.field.FieldText
import io.github.lenticualrstone.civilight.domain.cast.field.FieldValueEdits
import io.github.lenticualrstone.civilight.domain.cast.field.NewField
import io.github.lenticualrstone.civilight.domain.cast.field.Profile
import io.github.lenticualrstone.civilight.feature.cast.common.toAction
import io.github.lenticualrstone.civilight.feature.cast.field.TimedEditModel
import io.github.lenticualrstone.civilight.feature.cast.field.TimedSubject
import io.github.lenticualrstone.civilight.feature.cast.profile.ProfileModel
import io.github.lenticualrstone.civilight.feature.cast.profile.ProfileUi
import io.github.lenticualrstone.civilight.feature.cast.settings.FieldDefEditModel
import io.github.lenticualrstone.civilight.feature.cast.trash.ItemTrash
import io.github.lenticualrstone.civilight.model.cast.EntityKind
import io.github.lenticualrstone.civilight.model.cast.FieldLinkTargetKind
import io.github.lenticualrstone.civilight.model.cast.FieldPayload
import io.github.lenticualrstone.civilight.model.cast.FieldType
import io.github.lenticualrstone.civilight.model.cast.FieldValue
import io.github.lenticualrstone.civilight.model.position.LensPoint
import io.github.lenticualrstone.civilight.navigation.ProfileTab
import kotlinx.serialization.json.jsonObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import io.github.lenticualrstone.civilight.feature.cast.CastFx.atLens
import io.github.lenticualrstone.civilight.model.sample.ExampleKeys as K

/** Field definitions (CST-22, CST-23, CST-25, CST-26) and the secret tab (CST-18) through the feature models. */
class FieldDefEditTest {
    private val doc = CastFx.doc()
    private val ctx = CastFx.ctx()

    private fun text(v: String) = FieldValue.of(FieldPayload.Texts(listOf(v)))

    @Test
    fun `CST-23 the edit view lists the field's state`() {
        val v = FieldDefEditModel.view(doc, K.FIELD_HEIGHT)!!
        assertEquals("신장", v.name)
        assertEquals("height", v.exportName)
        assertEquals("cm", v.unit)
        assertEquals(listOf(FieldType.SHORT_TEXT), v.offered)
        assertFalse(v.filterEnabled)
        assertTrue(v.deleteSummary.startsWith("값이 있는 인물 "))
    }

    @Test
    fun `TC-F-07 long text offers no other type`() {
        assertEquals(emptyList<FieldType>(), FieldDefEditModel.view(doc, K.FIELD_PERSONALITY)!!.offered)
    }

    @Test
    fun `TC-F-05 the type change preview counts what cannot convert before anything changes`() {
        val (withDef, def) = FieldDefinitions.add(doc, NewField("키", FieldType.SHORT_TEXT), ctx)
        var d = FieldValueEdits.setBase(withDef, EntityKind.CHARACTER, K.RIEN, def.key, text("163cm"), ctx)
        d = FieldValueEdits.setBase(d, EntityKind.CHARACTER, K.SERA, def.key, text("154"), ctx)
        d = FieldValueEdits.setBase(d, EntityKind.CHARACTER, K.OWEN, def.key, text("키 작음"), ctx)
        val preview = FieldDefEditModel.preview(d, def.key, FieldType.NUMBER, null, ctx)!!
        assertEquals("◇ 바꾸지 못한 값 1", preview.notice)
        // The preview wrote nothing.
        assertEquals(FieldType.SHORT_TEXT, d.index.fieldDef(def.key)!!.type.known)
        val converted = FieldDefEditModel.convert(d, def.key, FieldType.NUMBER, null, ctx)
        assertEquals(FieldPayload.Number("163"), converted.index.character(K.RIEN)!!.fields[def.key]!!.payload)
        // Not offered for long text: nothing changes.
        assertEquals(doc, FieldDefEditModel.convert(doc, K.FIELD_PERSONALITY, FieldType.SHORT_TEXT, null, ctx))
    }

    @Test
    fun `TC-F-22 short text to link needs the target kind`() {
        val (withDef, def) = FieldDefinitions.add(doc, NewField("소속2", FieldType.SHORT_TEXT), ctx)
        var d = FieldValueEdits.setBase(withDef, EntityKind.CHARACTER, K.RIEN, def.key, text("등불 결사"), ctx)
        d = FieldValueEdits.setBase(d, EntityKind.CHARACTER, K.SERA, def.key, text("결사"), ctx)
        assertTrue(FieldDefEditModel.needsTarget(FieldType.LINK))
        val preview = FieldDefEditModel.preview(d, def.key, FieldType.LINK, FieldLinkTargetKind.FACTION, ctx)!!
        assertEquals("◇ 바꾸지 못한 값 1", preview.notice)
    }

    @Test
    fun `TC-F-08 and TC-F-09 options list their holders, rename and delete`() {
        val v = FieldDefEditModel.view(doc, K.FIELD_GENDER)!!
        val female = v.options.single { it.key == K.OPTION_GENDER_FEMALE }
        assertTrue(female.holders.isNotEmpty())
        val renamed = FieldDefEditModel.renameOption(doc, K.FIELD_GENDER, K.OPTION_GENDER_FEMALE, "여성", ctx)
        assertEquals("여성", FieldDefEditModel.view(renamed, K.FIELD_GENDER)!!.options.single { it.key == K.OPTION_GENDER_FEMALE }.label)
        // Blank or unchanged labels change nothing.
        assertEquals(doc, FieldDefEditModel.renameOption(doc, K.FIELD_GENDER, K.OPTION_GENDER_FEMALE, " ", ctx))
        val removed = FieldDefEditModel.removeOption(doc, K.FIELD_GENDER, K.OPTION_GENDER_FEMALE, ctx)
        val holder = doc.cast.characters.first { (it.fields[K.FIELD_GENDER]?.payload as? FieldPayload.Choice)?.optionKeys?.contains(K.OPTION_GENDER_FEMALE) == true }
        val kept = removed.index.character(holder.key)!!.fields[K.FIELD_GENDER]!!
        assertEquals(FieldText.OPTION_REMOVED, FieldText.format(removed, removed.index.fieldDef(K.FIELD_GENDER)!!, kept).notice)
        assertEquals(FieldDefEditModel.addOption(doc, K.FIELD_GENDER, "", ctx), doc)
    }

    @Test
    fun `TC-F-10 TC-F-11 unit and rename keep numbers and the export name (CST-26)`() {
        val unit = FieldDefEditModel.setUnit(doc, K.FIELD_HEIGHT, "척", ctx)
        assertEquals("척", unit.index.fieldDef(K.FIELD_HEIGHT)!!.unit)
        assertEquals(doc.index.character(K.RIEN)!!.fields[K.FIELD_HEIGHT], unit.index.character(K.RIEN)!!.fields[K.FIELD_HEIGHT])
        val renamed = FieldDefEditModel.rename(doc, K.FIELD_HEIGHT, "키", ctx)
        val def = renamed.index.fieldDef(K.FIELD_HEIGHT)!!
        assertEquals("키", def.name)
        assertEquals("height", CharacterExport.exportKey(def))
        assertTrue(CharacterExport.fields(renamed, K.RIEN).jsonObject.containsKey("height"))
        // An empty export name exports under the field name.
        val cleared = FieldDefEditModel.setExportName(renamed, K.FIELD_HEIGHT, "", ctx)
        assertEquals("키", CharacterExport.exportKey(cleared.index.fieldDef(K.FIELD_HEIGHT)!!))
        assertEquals(doc, FieldDefEditModel.rename(doc, K.FIELD_HEIGHT, "  ", ctx))
    }

    @Test
    fun `TC-F-12 a field definition goes to the trash with its values and comes back`() {
        val deleted = ItemTrash.deleteFieldDef(doc, K.FIELD_HEIGHT, ctx.toAction())!!
        assertNull(FieldDefEditModel.view(deleted.doc, K.FIELD_HEIGHT))
        assertTrue(deleted.toast.message.startsWith("항목 신장 지움"))
        assertTrue(Profile.header(deleted.doc, K.RIEN, LensPoint.Latest, cover = false).rows.none { it.label == "신장" })
        assertNull(ItemTrash.deleteFieldDef(deleted.doc, K.FIELD_HEIGHT, ctx.toAction()))
        val back = deleted.change.revert(deleted.doc)
        assertNotNull(FieldDefEditModel.view(back, K.FIELD_HEIGHT))
        assertEquals(doc.index.character(K.RIEN)!!.fields[K.FIELD_HEIGHT], back.index.character(K.RIEN)!!.fields[K.FIELD_HEIGHT])
    }

    @Test
    fun `TC-F-14 a record field for one character, promoted later, keeps its value`() {
        val (d, def) = FieldDefinitions.addFromRecord(doc, K.RIEN, "좋아하는 것", FieldType.SHORT_TEXT, FieldScope.THIS_CHARACTER, ctx)
        val withValue = FieldValueEdits.setBase(d, EntityKind.CHARACTER, K.RIEN, def.key, text("등불"), ctx)
        assertTrue(FieldDefEditModel.view(withValue, def.key)!!.characterOnly)
        val rien = ProfileModel.build(withValue, K.RIEN, ProfileUi(), cover = false, hiddenUnlocked = false, readOnly = false)!!
        assertTrue(rien.overview.items.single { it.defKey == def.key }.characterOnly)
        val promoted = FieldDefinitions.promoteToAll(withValue, def.key, ctx)
        assertFalse(FieldDefEditModel.view(promoted, def.key)!!.characterOnly)
        assertEquals(text("등불"), promoted.index.character(K.RIEN)!!.fields[def.key])
    }

    @Test
    fun `CST-18 a secret field shows only on the secret tab`() {
        val secret = FieldDefinitions.setSecret(doc, K.FIELD_HEIGHT, true, ctx)
        val state = ProfileModel.build(secret, K.RIEN, ProfileUi(ProfileTab.SECRET), cover = false, hiddenUnlocked = false, readOnly = false)!!
        assertTrue(state.header.rows.none { it.label == "신장" })
        assertTrue(state.overview.items.none { it.label == "신장" })
        assertTrue(Profile.secretTab(secret, K.RIEN, LensPoint.Latest).rows.any { it.label == "신장" })
        assertTrue(FieldDefEditModel.view(secret, K.FIELD_HEIGHT)!!.secret)
    }

    @Test
    fun `CST-25 a computed age is read-only and a direct age is typed`() {
        val at14 = doc.atLens(LensPoint.UnitEnd(K.EPISODE_1_4))
        val age = TimedEditModel.fieldView(at14, TimedSubject.Field(EntityKind.CHARACTER, K.RIEN, K.FIELD_AGE), at14.app.view.lens.point)!!
        assertNotNull(age.derived)
        assertEquals("17세", age.currentText)
        // Without 출생, 나이 is typed (TC-F-04).
        val noBirth = doc.copy(cast = doc.cast.copy(characters = doc.cast.characters.map { if (it.key == K.RIEN) it.copy(fields = it.fields - K.FIELD_BIRTH) else it }))
        assertNull(TimedEditModel.fieldView(noBirth, TimedSubject.Field(EntityKind.CHARACTER, K.RIEN, K.FIELD_AGE), LensPoint.Latest)!!.derived)
    }
}
