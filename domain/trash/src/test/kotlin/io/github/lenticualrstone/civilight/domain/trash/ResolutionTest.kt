package io.github.lenticualrstone.civilight.domain.trash

import io.github.lenticualrstone.civilight.model.doc.ProjectDoc
import io.github.lenticualrstone.civilight.model.open
import io.github.lenticualrstone.civilight.model.sample.ExampleKeys as K
import io.github.lenticualrstone.civilight.model.text.LinkTarget
import io.github.lenticualrstone.civilight.model.text.LinkTargetKind
import io.github.lenticualrstone.civilight.model.trash.NameTag
import io.github.lenticualrstone.civilight.model.trash.NameTagKind
import io.github.lenticualrstone.civilight.model.trash.NameTagState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** 00-domain-model §3: the five cases in order, hiding first. */
class ResolutionTest {
    private fun hideNoah(doc: ProjectDoc) =
        doc.copy(cast = doc.cast.copy(characters = doc.cast.characters.map { if (it.key == K.NOAH) it.copy(hidden = true) else it }))

    @Test
    fun `live reference`() {
        val ref = Resolution.resolve(Fx.doc(), RefTargetKind.CHARACTER, K.RIEN)
        assertEquals(RefState.LIVE, ref.state)
        assertEquals("리엔", ref.label)
        assertFalse(ref.dotted)
        assertFalse(ref.canRestore)
        assertEquals("RIE", ref.voiceCode)
    }

    @Test
    fun `trashed reference offers restore`() {
        val ref = Resolution.resolve(Fx.doc(), RefTargetKind.CHARACTER, K.NOAH)
        assertEquals(RefState.TRASHED, ref.state)
        assertEquals("지운 인물 · 노아", ref.label)
        assertTrue(ref.canRestore)
        val scene = Resolution.resolve(Fx.doc(), RefTargetKind.UNIT, K.SCENE_1_6_5)
        assertEquals(RefState.TRASHED, scene.state)
        assertEquals("지운 씬 · 1-6 씬 5 · 시장의 밤", scene.label)
    }

    @Test
    fun `hiding wins over deletion while locked`() {
        val doc = hideNoah(Fx.doc())
        val locked = Resolution.resolve(doc, RefTargetKind.CHARACTER, K.NOAH)
        assertEquals(RefState.HIDDEN_LOCKED, locked.state)
        assertEquals("숨긴 인물", locked.label)
        assertNull(locked.name)
        assertFalse(locked.canRestore)
        // Exports still print the name (rule 19 §5, rule 20 §2).
        assertEquals("노아", locked.exportName)
        val open = Resolution.resolve(doc, RefTargetKind.CHARACTER, K.NOAH, HidingLocks(itemsLocked = false))
        assertEquals(RefState.TRASHED, open.state)
        assertEquals("지운 인물 · 노아", open.label)
    }

    @Test
    fun `hidden world item reads 숨긴 항목`() {
        val base = Fx.doc()
        val doc = base.copy(world = base.world.copy(places = base.world.places.map { if (it.key == K.PLACE_ASH_MARKET) it.copy(hidden = true) else it }))
        val ref = Resolution.resolve(doc, LinkTarget(LinkTargetKind.PLACE.open(), K.PLACE_ASH_MARKET))!!
        assertEquals(RefState.HIDDEN_LOCKED, ref.state)
        assertEquals("숨긴 항목", ref.label)
    }

    @Test
    fun `hidden project locked reads 숨긴 프로젝트`() {
        val base = Fx.doc()
        val doc = base.copy(project = base.project.copy(protections = base.project.protections.copy(hidden = true)))
        val ref = Resolution.resolve(doc, RefTargetKind.CHARACTER, K.RIEN, HidingLocks(itemsLocked = true, projectLocked = true))
        assertEquals(RefState.HIDDEN_LOCKED, ref.state)
        assertEquals("숨긴 프로젝트", ref.label)
        assertNull(ref.name)
    }

    @Test
    fun `purged and not-exported name tags`() {
        val base = Fx.doc()
        val ghost = Fx.key("CHAR", 51)
        val doc = base.copy(
            tombstones = base.tombstones.copy(
                nameTags = listOf(
                    NameTag(ghost, NameTagKind.CHARACTER.open(), "유령", "GHO", state = NameTagState.PURGED.open(), purgedAt = 1L),
                    NameTag(Fx.key("PAGE", 51), NameTagKind.PAGE.open(), "옛 페이지", state = NameTagState.NOT_EXPORTED.open()),
                ),
            ),
        )
        val purged = Resolution.resolve(doc, RefTargetKind.CHARACTER, ghost)
        assertEquals(RefState.TAGGED, purged.state)
        assertEquals("영구 삭제 · 유령", purged.label)
        assertEquals("GHO", purged.voiceCode)
        assertEquals("GHO", Resolution.voiceCodeOf(doc, ghost))
        assertFalse(purged.canRestore)
        val page = Resolution.resolve(doc, RefTargetKind.PAGE, Fx.key("PAGE", 51))
        assertEquals("지운 페이지 · 옛 페이지", page.label)
        assertFalse(page.canRestore)
    }

    @Test
    fun `a hidden purged tag never prints the name`() {
        val base = Fx.doc()
        val ghost = Fx.key("CHAR", 52)
        val doc = base.copy(
            tombstones = base.tombstones.copy(
                nameTags = listOf(NameTag(ghost, NameTagKind.CHARACTER.open(), "유령", hidden = true, state = NameTagState.PURGED.open())),
            ),
        )
        val locked = Resolution.resolve(doc, RefTargetKind.CHARACTER, ghost)
        assertEquals(RefState.HIDDEN_LOCKED, locked.state)
        assertEquals("영구 삭제 · 숨긴 항목", locked.label)
        assertNull(locked.name)
        val open = Resolution.resolve(doc, RefTargetKind.CHARACTER, ghost, HidingLocks(itemsLocked = false))
        assertEquals("영구 삭제 · 유령", open.label)
    }

    @Test
    fun `missing target never crashes and is not 화자 없음`() {
        val key = Fx.key("CHAR", 99)
        val ref = Resolution.resolve(Fx.doc(), RefTargetKind.CHARACTER, key)
        assertEquals(RefState.MISSING, ref.state)
        assertNull(ref.label)
        assertNull(ref.name)
        assertTrue(ref.dotted)
        assertFalse(ref.canRestore)
        assertEquals("TODO_COPY_DM_01", TrashCopy.MISSING_REFERENCE_COPY_ID)
    }

    @Test
    fun `a project link is not resolved here`() {
        assertNull(Resolution.resolve(Fx.doc(), LinkTarget(LinkTargetKind.PROJECT.open(), K.PROJECT, K.PROJECT)))
    }

    @Test
    fun `same name is exact NFC equality`() {
        val doc = Fx.doc()
        val decomposed = java.text.Normalizer.normalize("리엔", java.text.Normalizer.Form.NFD)
        val twin = Fx.addCharacter(doc, Fx.key("CHAR", 61), decomposed, 13)
        assertEquals("◇ 같은 이름 2 · 리엔", Fixes.sameNameNotice(twin, ItemId.character(K.RIEN)))
        val similar = Fx.addCharacter(doc, Fx.key("CHAR", 62), "리엔 ", 13)
        assertNull(Fixes.sameNameNotice(similar, ItemId.character(K.RIEN)))
    }
}
