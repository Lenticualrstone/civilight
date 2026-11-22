package io.github.lenticualrstone.civilight.feature.story

import io.github.lenticualrstone.civilight.domain.trash.ItemId
import io.github.lenticualrstone.civilight.domain.trash.TrashOps
import io.github.lenticualrstone.civilight.feature.story.Fx.unit
import io.github.lenticualrstone.civilight.feature.story.where.CastModel
import io.github.lenticualrstone.civilight.feature.story.where.MemberLook
import io.github.lenticualrstone.civilight.feature.story.where.PlaceFix
import io.github.lenticualrstone.civilight.feature.story.where.WhereModel
import io.github.lenticualrstone.civilight.model.checks.CheckType
import io.github.lenticualrstone.civilight.model.doc.ProjectDoc
import io.github.lenticualrstone.civilight.model.sample.ExampleKeys as K
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

/** STO-26 등장 row and the STO-11 place / member states of the `장소와 때` sheet. */
class CastAndPlaceRowTest {
    private val doc = Fx.doc()
    private val s9 = K.SCENE_1_7_3

    private fun ProjectDoc.names(scene: String, locked: Boolean = true) = CastModel.row(this, scene, locked).let { r ->
        r.speakers.map { it.name } to r.silent.map { it.name }
    }

    @Test
    fun sto26_speakersFollowTheBlocksAndSilentMembersAreEdited() {
        val (speakers, silent) = doc.names(s9)
        assertEquals(listOf("마르타"), silent)
        assertTrue(speakers.containsAll(listOf("리엔", "하르트")))
        val added = CastModel.add(doc, s9, K.TEO, Fx.ctx())
        assertEquals(listOf(K.MARTHA, K.TEO), added.unit(s9).silentCast)
        assertTrue(K.TEO in CastModel.row(added, s9, true).inScene)
        // A speaker is a fact of the blocks: adding one changes nothing.
        assertSame(doc, CastModel.add(doc, s9, K.RIEN, Fx.ctx()))
        val removed = CastModel.remove(added, s9, K.MARTHA, Fx.ctx())
        assertEquals(listOf(K.TEO), removed.unit(s9).silentCast)
    }

    @Test
    fun aDeletedSilentMemberIsDottedWithItsThreeButtons_R20() {
        val scene = K.SCENE_1_2_2
        val noah = CastModel.row(doc, scene, true).silent.single()
        assertEquals("노아", noah.name)
        assertEquals(MemberLook.DELETED, noah.look)
        assertTrue(noah.dotted)
        assertFalse("not offered as in the scene", K.NOAH in CastModel.row(doc, scene, true).inScene)
        // 이대로 두기: the acknowledgement of this one reference; the name stays dotted, the buttons go.
        val kept = CastModel.keep(doc, scene, K.NOAH, Fx.ctx())
        assertTrue(kept.checks.acknowledgements.any { it.checkType == CheckType.DELETED_ITEM && it.subjectKeys == listOf(K.NOAH, scene) })
        assertEquals(MemberLook.DELETED_KEPT, CastModel.row(kept, scene, true).silent.single().look)
        assertSame(kept, CastModel.keep(kept, scene, K.NOAH, Fx.ctx()))
        // 빼기.
        assertEquals(emptyList<String>(), CastModel.remove(doc, scene, K.NOAH, Fx.ctx()).unit(scene).silentCast)
        // 되살리기 through CharacterActions: 노아 is live again, with the cast toast.
        assertFalse(CastModel.needsAuth(doc, K.NOAH))
        val step = CastModel.restoreStep(doc, K.NOAH, Fx.ctx(), authenticated = false)!!
        assertNull(step.doc.index.character(K.NOAH)!!.deletedAt)
        assertTrue(step.toast!!.message.startsWith("노아 되살림"))
        assertEquals(MemberLook.LIVE, CastModel.row(step.doc, scene, true).silent.single().look)
        assertEquals(doc.cast, step.change.revert(step.doc).cast)
    }

    @Test
    fun aLockedHiddenMemberNeverShowsItsName_R19() {
        val hidden = doc.copy(cast = doc.cast.copy(characters = doc.cast.characters.map { if (it.key == K.MARTHA) it.copy(hidden = true) else it }))
        assertEquals(listOf("숨긴 인물"), hidden.names(s9, locked = true).second)
        assertEquals(listOf("마르타"), hidden.names(s9, locked = false).second)
        // Hiding wins over deletion.
        val hiddenNoah = doc.copy(cast = doc.cast.copy(characters = doc.cast.characters.map { if (it.key == K.NOAH) it.copy(hidden = true) else it }))
        assertEquals(MemberLook.HIDDEN, CastModel.row(hiddenNoah, K.SCENE_1_2_2, true).silent.single().look)
    }

    @Test
    fun aDeletedPlaceOffersPickRestoreAndKeep_STO11() {
        val place = doc.unit(s9).place!!.key
        val deleted = TrashOps.delete(doc, ItemId.place(place), Fx.ctx().trash).doc
        val ui = WhereModel.build(deleted, s9, readOnly = false)!!
        assertTrue(ui.sheet.placeDeleted)
        assertEquals("지운 장소 · 결사의 문", ui.sheet.place)
        assertTrue(ui.placeState.restorable)
        assertTrue(ui.placeState.keepable)
        val kept = PlaceFix.keep(deleted, s9, Fx.ctx())
        assertFalse(PlaceFix.state(kept, s9, locked = true).keepable)
        val step = PlaceFix.restoreStep(deleted, s9, Fx.ctx(), authenticated = false)!!
        assertTrue(step.doc.liveness.isPlaceLive(place))
        assertNotNull(step.toast)
        assertFalse(WhereModel.build(step.doc, s9, false)!!.sheet.placeDeleted)
        // A live place: nothing to restore or keep.
        assertNull(PlaceFix.restoreStep(doc, s9, Fx.ctx(), authenticated = false))
        assertSame(doc, PlaceFix.keep(doc, s9, Fx.ctx()))
    }

    @Test
    fun aLockedHiddenPlaceIsMasked_R19() {
        val place = doc.unit(s9).place!!.key
        val hidden = doc.copy(world = doc.world.copy(places = doc.world.places.map { if (it.key == place) it.copy(hidden = true) else it }))
        assertTrue(WhereModel.build(hidden, s9, false, locked = true)!!.placeState.masked)
        assertFalse(WhereModel.build(hidden, s9, false, locked = false)!!.placeState.masked)
    }
}
