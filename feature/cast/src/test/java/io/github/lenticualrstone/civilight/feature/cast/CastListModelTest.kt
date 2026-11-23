package io.github.lenticualrstone.civilight.feature.cast

import io.github.lenticualrstone.civilight.domain.cast.CharacterEdits
import io.github.lenticualrstone.civilight.feature.cast.CastFx.atLens
import io.github.lenticualrstone.civilight.feature.cast.list.CastListModel
import io.github.lenticualrstone.civilight.feature.cast.list.CastListUi
import io.github.lenticualrstone.civilight.feature.cast.list.NewCharacter
import io.github.lenticualrstone.civilight.model.position.LensPoint
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import io.github.lenticualrstone.civilight.model.sample.ExampleGallery as G
import io.github.lenticualrstone.civilight.model.sample.ExampleKeys as K

/** The list screen state (CST-01, CST-02, CST-05) on the example project. */
class CastListModelTest {
    private val doc = CastFx.doc()

    @Test
    fun `CST-01 chips count live characters and groups follow tag order`() {
        val state = CastListModel.build(doc, CastListUi(), hiddenUnlocked = false, readOnly = false)
        val chips = state.view.chips.map { it.label }
        assertEquals(listOf("전체 11", "주연", "등불 결사", "하층 구역", "상층"), chips)
        assertTrue(state.view.chips.first().selected)
        assertEquals("주연", state.view.groups.first().title)
        assertFalse("노아 is in the trash", state.view.rows.any { it.key == K.NOAH })
        assertEquals("인물", state.view.title)
        assertEquals("잿빛 새벽의 등불", state.view.projectName)
    }

    @Test
    fun `TC-F-17 list row of 리엔 at 최신`() {
        val state = CastListModel.build(doc, CastListUi(), hiddenUnlocked = false, readOnly = false)
        val rien = state.view.rows.single { it.key == K.RIEN }
        assertEquals("17세 · 등불 결사 · 막내 등불지기", rien.subtitle)
        assertEquals("1-2", rien.firstAppearance)
        assertEquals("리", rien.initial)
    }

    @Test
    fun `TC-13-17 list rows carry no relation text`() {
        val rows = CastListModel.build(doc, CastListUi(), hiddenUnlocked = false, readOnly = false).view.rows
        for (word in listOf("경계", "보호", "선배", "후배", "거래 상대")) {
            assertFalse(word, rows.single { it.key == K.RIEN }.subtitle.contains(word))
        }
    }

    @Test
    fun `CST-11 하르트 row carries the display-name note`() {
        val state = CastListModel.build(doc, CastListUi(), hiddenUnlocked = false, readOnly = false)
        val hart = state.view.rows.single { it.key == K.HART }
        assertTrue(hart.subtitle, hart.subtitle.contains("표시 이름 ??? (1-8까지)"))
    }

    @Test
    fun `CST-02 a tag chip shows only that tag and a removed tag falls back to 전체`() {
        val lantern = CastListModel.build(doc, CastListModel.selectTag(CastListUi(), K.TAG_LANTERN_SOCIETY), false, false)
        assertEquals(K.TAG_LANTERN_SOCIETY, lantern.selectedTag)
        assertEquals(1, lantern.view.groups.size)
        assertEquals(setOf(K.RIEN, K.SERA, K.MARTHA, K.TEO), lantern.view.rows.map { it.key }.toSet())
        val gone = CastListModel.build(doc, CastListUi(selectedTag = "no-such-tag"), false, false)
        assertNull(gone.selectedTag)
        assertTrue(gone.view.chips.first().selected)
    }

    @Test
    fun `CST-02 a tag used only on a hidden gallery image gets no chip, locked or unlocked`() {
        val g0 = G.build()
        val (tagged, harbor) = CharacterEdits.addTag(g0, "harbor", CastFx.ctx())
        val withImage = tagged.copy(
            gallery = tagged.gallery.mapValues { (_, file) ->
                file.copy(items = file.items.map { if (it.key == G.ITEM_H1) it.copy(tagKeys = it.tagKeys + harbor.key) else it })
            },
        )
        for (unlocked in listOf(false, true)) {
            val state = CastListModel.build(withImage, CastListUi(), hiddenUnlocked = unlocked, readOnly = false)
            assertFalse("unlocked=$unlocked", state.view.chips.any { it.tagKey == harbor.key })
            // A chip remembered from before falls back to `전체` instead of an empty list.
            val picked = CastListModel.build(withImage, CastListUi(selectedTag = harbor.key), hiddenUnlocked = unlocked, readOnly = false)
            assertNull(picked.selectedTag)
            assertTrue(picked.view.chips.first().selected)
            assertTrue(picked.view.rows.isNotEmpty())
        }
    }

    @Test
    fun `CST-02 a tag held only by a locked hidden character has no chip until unlocked`() {
        val ctx = CastFx.ctx()
        val (tagged, harbor) = CharacterEdits.addTag(doc, "harbor", ctx)
        val owenHidden = CharacterEdits.setHidden(CharacterEdits.setTags(tagged, K.OWEN, listOf(harbor.key), ctx), K.OWEN, true, ctx)
        val locked = CastListModel.build(owenHidden, CastListUi(selectedTag = harbor.key), hiddenUnlocked = false, readOnly = false)
        assertFalse(locked.view.chips.any { it.tagKey == harbor.key })
        assertNull(locked.selectedTag)
        val open = CastListModel.build(owenHidden, CastListUi(selectedTag = harbor.key), hiddenUnlocked = true, readOnly = false)
        assertTrue(open.view.chips.single { it.tagKey == harbor.key }.selected)
        assertEquals(listOf(K.OWEN), open.view.rows.map { it.key })
    }

    @Test
    fun `CST-02 typing filters by 초성 and ends with the typing state`() {
        val typing = CastListUi(query = "ㄹㅇ", searching = true)
        val hits = CastListModel.build(doc, typing, false, false)
        // 초성 match anywhere in the name (domain:cast default): 리엔 and 엘리오 (ㅇㄹㅇ).
        assertEquals(setOf(K.RIEN, K.ELIO), hits.view.rows.map { it.key }.toSet())
        val none = CastListModel.build(doc, typing.copy(query = "없는이름"), false, false)
        assertTrue(none.view.noResults)
        val closed = CastListModel.stopSearch(typing)
        assertEquals("", closed.query)
        assertEquals(11, CastListModel.build(doc, closed, false, false).view.rows.size)
    }

    @Test
    fun `a query left over outside the typing state does not filter`() {
        val stale = CastListUi(query = "ㄹㅇ", searching = false)
        assertEquals(11, CastListModel.build(doc, stale, false, false).view.rows.size)
    }

    @Test
    fun `values follow the stored lens`() {
        val past = doc.atLens(LensPoint.UnitEnd(K.EPISODE_1_2))
        val rien = CastListModel.build(past, CastListUi(), false, false).view.rows.single { it.key == K.RIEN }
        assertFalse("소속 is 없음 before 1-3", rien.subtitle.contains("등불 결사"))
    }

    @Test
    fun `CST-05 a new character needs only a name and opens with the next record number`() {
        assertFalse(NewCharacter.canCreate("  "))
        assertNull(NewCharacter.create(doc, " ", CastFx.ctx()))
        val (next, key) = NewCharacter.create(doc, "엘라", CastFx.ctx())!!
        val created = next.index.character(key)!!
        assertEquals("엘라", created.name)
        assertEquals(13, created.recordNo)
        assertTrue(created.fields.isEmpty())
        assertEquals("전체 12", CastListModel.build(next, CastListUi(), false, false).view.chips.first().label)
    }
}
