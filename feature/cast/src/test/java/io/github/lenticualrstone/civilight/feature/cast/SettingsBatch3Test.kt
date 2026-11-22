package io.github.lenticualrstone.civilight.feature.cast

import io.github.lenticualrstone.civilight.domain.cast.field.FieldDefinitions
import io.github.lenticualrstone.civilight.domain.cast.relation.RelationKinds
import io.github.lenticualrstone.civilight.domain.cast.relation.RelationReader
import io.github.lenticualrstone.civilight.domain.cast.relation.RelationViews
import io.github.lenticualrstone.civilight.feature.cast.kinds.KindShape
import io.github.lenticualrstone.civilight.feature.cast.kinds.RelationKindModel
import io.github.lenticualrstone.civilight.feature.cast.settings.BundleModel
import io.github.lenticualrstone.civilight.model.app.LibraryEntry
import io.github.lenticualrstone.civilight.model.cast.EntityKind
import io.github.lenticualrstone.civilight.model.cast.RelationCategory
import io.github.lenticualrstone.civilight.model.cast.RelationSymmetry
import io.github.lenticualrstone.civilight.model.doc.ProjectDoc
import io.github.lenticualrstone.civilight.model.position.LensPoint
import io.github.lenticualrstone.civilight.model.project.FieldBundle
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import io.github.lenticualrstone.civilight.model.sample.ExampleKeys as K

/** Start bundles (CST-24) and relation kinds (CST-32) through the feature models. */
class SettingsBatch3Test {
    private val doc = CastFx.doc()
    private val ctx = CastFx.ctx()

    /** A project on `기본` (the example's 게임 프로필 extras removed). */
    private fun basicOnly(): ProjectDoc {
        val extras = setOf(K.FIELD_CODENAME, K.FIELD_GENDER, K.FIELD_SPECIES, K.FIELD_ORIGIN, K.FIELD_HEIGHT, K.FIELD_SPECIALTY)
        return doc.copy(project = doc.project.copy(fieldDefs = doc.project.fieldDefs.filterNot { it.key in extras }))
    }

    @Test
    fun `CST-24 the picker lists the four bundles with their fields`() {
        val rows = BundleModel.rows(EntityKind.CHARACTER)
        assertEquals(listOf("기본", "게임 프로필", "비어 있음", "다른 프로젝트에서"), rows.map { it.title })
        assertEquals("출생 · 나이 · 생일 · 소속 · 성격", rows[0].sub)
        assertNull(rows[2].sub)
        // Other kinds have no built-in fields ([PROPOSED] they start empty).
        assertTrue(BundleModel.rows(EntityKind.PLACE).all { it.sub == null })
    }

    @Test
    fun `TC-F-15 항목 더하기 adds only the missing fields`() {
        val basic = basicOnly()
        val added = BundleModel.preview(basic, FieldBundle.GAME_PROFILE, EntityKind.CHARACTER, ctx)
        assertTrue(added.containsAll(listOf("코드네임", "성별", "종족", "출신지", "신장", "특기")))
        assertFalse(added.contains("출생"))
        val applied = BundleModel.apply(basic, FieldBundle.GAME_PROFILE, EntityKind.CHARACTER, ctx).doc
        // Existing fields and values stay.
        for (def in basic.project.fieldDefs) assertEquals(def, applied.index.fieldDef(def.key))
        assertEquals(basic.cast.characters.map { it.fields }, applied.cast.characters.map { it.fields })
        // Nothing is missing any more.
        assertEquals(emptyList<String>(), BundleModel.preview(applied, FieldBundle.GAME_PROFILE, EntityKind.CHARACTER, ctx))
    }

    @Test
    fun `TC-F-16 another project is offered only while unlocked and gives definitions only`() {
        val glass = LibraryEntry(key = "OTHER", name = "유리 정원", hidden = true)
        val open = LibraryEntry(key = "OPEN", name = "열린 책")
        val self = LibraryEntry(key = doc.project.key, name = "잿빛 새벽의 등불")
        assertEquals(listOf("열린 책"), BundleModel.sources(listOf(glass, open, self), doc.project.key, hiddenUnlocked = false).map { it.name })
        assertEquals(listOf("열린 책", "유리 정원"), BundleModel.sources(listOf(glass, open, self), doc.project.key, hiddenUnlocked = true).map { it.name })
        val empty = doc.copy(project = doc.project.copy(fieldDefs = emptyList()))
        val names = BundleModel.previewFrom(empty, doc, EntityKind.CHARACTER, ctx)
        assertTrue(names.contains("신장"))
        val copied = BundleModel.applyFrom(empty, doc, EntityKind.CHARACTER, ctx).doc
        assertTrue(copied.cast.characters.none { c -> copied.project.fieldDefs.any { it.key in c.fields } })
    }

    @Test
    fun `CST-32 new kinds - 마음, 대칭 and a pair that needs both names`() {
        assertFalse(RelationKindModel.canAdd(KindShape(RelationCategory.BETWEEN, RelationSymmetry.PAIR, "보호자", "")))
        val pair = KindShape(RelationCategory.BETWEEN, RelationSymmetry.PAIR, "보호자", "피보호자")
        val added = RelationKindModel.add(doc, pair, ctx)
        val kind = added.project.relationKinds.single { it.name == "보호자" }
        assertEquals("피보호자", kind.pairName)
        assertEquals("보호자 ↔ 피보호자", RelationKindModel.edit(added, kind.key)!!.title)
        val mind = RelationKindModel.add(doc, KindShape(RelationCategory.MIND, name = "존경"), ctx)
        assertEquals(RelationCategory.MIND, mind.project.relationKinds.single { it.name == "존경" }.category.known)
        assertEquals(doc, RelationKindModel.add(doc, KindShape(name = " "), ctx))
    }

    @Test
    fun `TC-13-11 and TC-13-13 rename and pair rename`() {
        val wary = RelationKindModel.edit(doc, K.KIND_WARINESS)!!
        val renamed = RelationKindModel.rename(doc, K.KIND_WARINESS, wary.shape.copy(name = "경계심"), ctx)
        assertTrue(RelationViews.view(renamed, K.RIEN, K.HART, LensPoint.Latest)!!.summary.contains("→ 경계심"))
        val senior = RelationKindModel.edit(doc, K.KIND_SENIOR_JUNIOR)!!
        assertTrue(RelationKindModel.namesChanged(senior.shape, senior.shape.copy(pairName = "막내")))
        val pair = RelationKindModel.rename(doc, K.KIND_SENIOR_JUNIOR, senior.shape.copy(pairName = "막내"), ctx)
        assertEquals("리엔 · 막내", RelationViews.view(pair, K.SERA, K.RIEN, LensPoint.Latest)!!.title("리엔"))
        assertEquals("세라 · 선배", RelationViews.view(pair, K.RIEN, K.SERA, LensPoint.Latest)!!.title("세라"))
    }

    @Test
    fun `TC-13-20 a switch shows the affected relations before it applies`() {
        val trade = RelationKindModel.edit(doc, K.KIND_TRADE_PARTNER)!!
        val toMind = trade.shape.copy(category = RelationCategory.MIND)
        assertTrue(RelationKindModel.shapeChanged(trade.shape, toMind))
        val preview = RelationKindModel.switchPreview(doc, K.KIND_TRADE_PARTNER)
        assertEquals("바뀌는 관계 ${preview.relationKeys.size}", preview.countLabel)
        assertTrue(preview.pairs.contains("리엔 · 오웬"))
        // Cancelled: nothing changed.
        assertEquals(RelationCategory.BETWEEN, doc.index.relationKind(K.KIND_TRADE_PARTNER)!!.category.known)
        val applied = RelationKindModel.applySwitch(doc, K.KIND_TRADE_PARTNER, toMind, ctx)
        assertEquals(RelationCategory.MIND, applied.index.relationKind(K.KIND_TRADE_PARTNER)!!.category.known)
        // A switch to 짝 needs the other side's name.
        assertFalse(RelationKindModel.canSwitch(trade.shape, trade.shape.copy(symmetry = RelationSymmetry.PAIR)))
    }

    @Test
    fun `TC-13-12 a deleted kind leaves its text`() {
        val deleted = RelationKindModel.delete(doc, K.KIND_PROTECTION, ctx)
        assertNull(deleted.index.relationKind(K.KIND_PROTECTION))
        assertTrue(RelationKinds.settings(deleted).minds.none { it.key == K.KIND_PROTECTION })
        // 하르트's mind for 리엔 (a `◆` record) keeps the word `보호` with `◇`.
        val hart = RelationViews.view(deleted, K.HART, K.RIEN, LensPoint.Latest)!!.ownMind!!
        assertEquals("보호", hart.word)
        assertTrue(hart.fallback)
        assertTrue(hart.full.endsWith("◇"))
        assertEquals(FieldDefinitions.live(doc, EntityKind.CHARACTER).size, FieldDefinitions.live(deleted, EntityKind.CHARACTER).size)
    }
}
