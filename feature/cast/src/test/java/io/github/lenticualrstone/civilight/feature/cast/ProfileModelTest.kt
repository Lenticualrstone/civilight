package io.github.lenticualrstone.civilight.feature.cast

import io.github.lenticualrstone.civilight.domain.cast.field.FieldDefinitions
import io.github.lenticualrstone.civilight.feature.cast.CastFx.atLens
import io.github.lenticualrstone.civilight.feature.cast.common.toAction
import io.github.lenticualrstone.civilight.feature.cast.profile.FixedField
import io.github.lenticualrstone.civilight.feature.cast.profile.ProfileModel
import io.github.lenticualrstone.civilight.feature.cast.profile.ProfileUi
import io.github.lenticualrstone.civilight.model.cast.EntityKind
import io.github.lenticualrstone.civilight.model.doc.ProjectDoc
import io.github.lenticualrstone.civilight.model.position.LensPoint
import io.github.lenticualrstone.civilight.navigation.ProfileTab
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import io.github.lenticualrstone.civilight.model.sample.ExampleKeys as K

/** The character record (CST-07, CST-08, CST-11, CST-12, CST-28) on the example project. */
class ProfileModelTest {
    private val doc = CastFx.doc()

    private fun build(
        d: ProjectDoc = doc,
        key: String = K.RIEN,
        ui: ProfileUi = ProfileUi(),
        cover: Boolean = true,
        unlocked: Boolean = false,
    ) = ProfileModel.build(d, key, ui, cover, unlocked, readOnly = false)

    @Test
    fun `CST-07 record head reads number, lens, name and one-liner`() {
        val state = build()!!
        assertEquals("인물 기록 007 · 최신 1-7", state.header.title)
        assertEquals("리엔", state.name)
        assertNotNull(state.oneLiner)
        assertEquals(ProfileTab.OVERVIEW, ProfileUi().tab)
    }

    @Test
    fun `TC-11-01 header row of 소속 at 최신 carries the start, the mark and the planned row`() {
        val state = build()!!
        val row = state.header.rows.single { it.label == "소속" }
        assertEquals("등불 결사", row.value)
        assertEquals("1-3부터", row.since)
        assertTrue(row.hasHistory)
        // Fixture: R3 holds the whole value (domain-cast.md spec conflict TC-11-01).
        assertEquals("예정 · 2-5" to "등불 결사 · 등불지기장", row.planned)
        assertEquals("예정 · 집필 선(1-7 씬 3) 뒤 기록", state.header.plannedFootnote)
    }

    @Test
    fun `TC-11-01 at a past lens the record reads that lens`() {
        val state = build(doc.atLens(LensPoint.UnitEnd(K.EPISODE_1_5)))!!
        assertEquals("인물 기록 007 · 시점 1-5", state.header.title)
        assertEquals("등불 결사", state.header.rows.single { it.label == "소속" }.value)
    }

    @Test
    fun `TC-F-18 the cover shows six header rows and 항목 더 보기, inner and expanded show all`() {
        var d = doc
        val ctx = CastFx.ctx()
        for (name in listOf("성별", "코드네임", "출신지", "특기")) {
            val def = d.project.fieldDefs.first { it.name == name && it.entityKind.known == EntityKind.CHARACTER }
            d = FieldDefinitions.setPlaces(d, def.key, inHeader = true, inList = def.inList, inFilter = def.inFilter, ctx = ctx)
        }
        val cover = build(d)!!
        assertEquals(6, cover.header.rows.size)
        assertEquals("항목 더 보기", cover.header.moreLabel)
        val expanded = build(d, ui = ProfileUi(headerExpanded = true))!!
        assertTrue(expanded.header.rows.size > 6)
        assertNull(expanded.header.moreLabel)
        assertEquals(expanded.header.rows, build(d, cover = false)!!.header.rows)
    }

    @Test
    fun `TC-F-13 empty fields hide behind 빈 항목 N · 보기 and 보기 shows every fixed field`() {
        var d = doc
        for (key in listOf(K.FIELD_CODENAME, K.FIELD_ORIGIN, K.FIELD_SPECIALTY)) {
            d = d.copy(cast = d.cast.copy(characters = d.cast.characters.map { if (it.key == K.RIEN) it.copy(fields = it.fields - key) else it }))
        }
        val view = build(d)!!
        assertEquals("빈 항목 3 · 보기", view.emptyLabel)
        assertTrue(view.overview.items.none { it.label in setOf("코드네임", "출신지", "특기") })
        assertEquals(listOf(FixedField.PORTRAIT, FixedField.TAGS), view.fixedRows.map { it.field })

        val shown = build(d, ui = ProfileUi(showEmpty = true))!!
        assertTrue(shown.overview.items.map { it.label }.containsAll(listOf("코드네임", "출신지", "특기")))
        assertEquals(
            listOf(FixedField.NAME, FixedField.DISPLAY_NAME, FixedField.ONE_LINER, FixedField.PORTRAIT, FixedField.BACKGROUND, FixedField.TAGS),
            shown.fixedRows.map { it.field },
        )
        assertEquals("이미지 고르기", shown.fixedRows.single { it.field == FixedField.PORTRAIT }.value)
        assertEquals("리엔", shown.fixedRows.first().value)
        assertEquals("", shown.fixedRows.single { it.field == FixedField.DISPLAY_NAME }.value)
    }

    @Test
    fun `CST-08 long text fields become numbered sections`() {
        val state = build()!!
        assertEquals("Ⅰ", state.overview.sections.first().numeral)
        assertEquals("성격", state.overview.sections.first().title)
    }

    @Test
    fun `CST-12 the 태그 row lists the character's tags in order`() {
        val tags = build()!!.fixedRows.single { it.field == FixedField.TAGS }
        assertEquals("태그", tags.label)
        assertTrue(tags.value, tags.value.startsWith("주연"))
    }

    @Test
    fun `TC-11-05 하르트's 표시 이름 row reads ??? with the 1-9 reveal as 예정`() {
        val row = build(key = K.HART)!!.fixedRows.single { it.field == FixedField.DISPLAY_NAME }
        assertEquals("???", row.value)
        assertTrue(row.hasHistory)
        assertEquals("예정 · 1-9" to "하르트", row.planned)
        val revealed = build(doc.atLens(LensPoint.UnitEnd(K.EPISODE_1_9)), key = K.HART)!!
        assertEquals("", revealed.fixedRows.single { it.field == FixedField.DISPLAY_NAME }.value)
    }

    @Test
    fun `TC-13-01 TC-13-02 TC-13-03 TC-13-05 relation blocks read from 리엔's side`() {
        val tab = build()!!.relations
        assertEquals("관계 · 최신 1-7", tab.header)
        assertEquals("관계도로 보기", tab.mapLink)
        val hart = tab.blocks.single { it.partnerKey == K.HART }
        assertEquals("하르트", hart.title)
        assertEquals("→ 경계 · ← 보호", hart.sub)
        assertTrue(hart.hasRecords)
        assertEquals("세라 · 선배", tab.blocks.single { it.partnerKey == K.SERA }.title)
        val owen = tab.blocks.single { it.partnerKey == K.OWEN }
        assertEquals("오웬 · 거래 상대", owen.title)
        assertEquals("마음 적지 않음", owen.sub)
        assertEquals("→ 보호 · ← 경계", build(key = K.HART)!!.relations.blocks.single { it.partnerKey == K.RIEN }.sub)
    }

    @Test
    fun `the record of a character in the trash or gone does not open`() {
        assertNull(build(key = K.NOAH))
        assertNull(build(key = "no-such-character"))
    }

    @Test
    fun `a hidden character opens only while unlocked`() {
        val hidden = doc.copy(cast = doc.cast.copy(characters = doc.cast.characters.map { if (it.key == K.OWEN) it.copy(hidden = true) else it }))
        assertNull(build(hidden, key = K.OWEN))
        assertNotNull(build(hidden, key = K.OWEN, unlocked = true))
        assertFalse(build(hidden, key = K.OWEN, unlocked = true)!!.readOnly)
    }

    @Test
    fun `CST-41 오웬's dotted 노아 row knows its relation, and 이대로 두기 hides only its buttons`() {
        val owen = build(key = K.OWEN)!!
        assertEquals(listOf(K.NOAH), owen.relations.deletedRows.map { it.partnerKey })
        assertEquals(mapOf(K.NOAH to K.REL_OWEN_NOAH), owen.deletedPartnerRelations)
        assertTrue(owen.keptPartners.isEmpty())
        val kept = io.github.lenticualrstone.civilight.feature.cast.trash.ItemTrash.keep(
            doc, K.NOAH, K.REL_OWEN_NOAH, CastFx.ctx().toAction(),
        )
        val after = build(kept, key = K.OWEN)!!
        assertEquals(setOf(K.NOAH), after.keptPartners)
        assertEquals(listOf(K.NOAH), after.relations.deletedRows.map { it.partnerKey })
    }

    /** `sub` keeps its elements in the order of `all`, and holds nothing else. */
    private fun <T> assertSubsequence(message: String, sub: List<T>, all: List<T>) {
        val it = all.iterator()
        for (item in sub) {
            var found = false
            while (it.hasNext()) if (it.next() == item) { found = true; break }
            assertTrue("$message: $item is not in $all in the same order", found)
        }
    }

    @Test
    fun `row 6 every row the record shows is also in the expanded record, in the same order`() {
        // The record folds rows in and out by drawing the expanded record's rows: a base row it lacks would vanish.
        val unlockedDoc = doc
        val characters = unlockedDoc.cast.characters.filter { it.deletedAt == null }.map { it.key }
        for (key in characters) for (cover in listOf(true, false)) for (unlocked in listOf(false, true)) {
            val full = build(key = key, ui = ProfileUi(showEmpty = true, headerExpanded = true), cover = cover, unlocked = unlocked) ?: continue
            for (showEmpty in listOf(false, true)) for (expanded in listOf(false, true)) {
                val base = build(key = key, ui = ProfileUi(showEmpty = showEmpty, headerExpanded = expanded), cover = cover, unlocked = unlocked)!!
                val at = "$key cover=$cover unlocked=$unlocked showEmpty=$showEmpty expanded=$expanded"
                assertSubsequence("$at header", base.header.rows.map { it.label }, full.header.rows.map { it.label })
                assertSubsequence("$at items", base.overview.items.map { it.defKey }, full.overview.items.map { it.defKey })
                assertSubsequence("$at fixed", base.fixedRows.map { it.field }, full.fixedRows.map { it.field })
                // The empty long rows only exist with `보기`, and then they are the expanded record's too.
                assertSubsequence("$at empty long", base.emptyLongRows.map { it.defKey }, full.emptyLongRows.map { it.defKey })
            }
        }
    }
}
