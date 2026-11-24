package io.github.lenticualrstone.civilight.feature.cast

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Matrix
import io.github.lenticualrstone.civilight.designsystem.icon.IconButtonCopy
import io.github.lenticualrstone.civilight.domain.cast.list.CastCard
import io.github.lenticualrstone.civilight.domain.cast.list.LockRow
import io.github.lenticualrstone.civilight.feature.cast.list.cards3d.CardPose
import io.github.lenticualrstone.civilight.feature.cast.list.cards3d.CardPoses
import io.github.lenticualrstone.civilight.feature.cast.list.cards3d.CardXform
import io.github.lenticualrstone.civilight.feature.cast.list.cards3d.Cards3dCopy
import io.github.lenticualrstone.civilight.feature.cast.list.cards3d.DeckItem
import io.github.lenticualrstone.civilight.feature.cast.list.cards3d.LongPressGate
import io.github.lenticualrstone.civilight.feature.cast.list.cards3d.LongPressGate.Verdict
import io.github.lenticualrstone.civilight.feature.cast.list.cards3d.Projector
import io.github.lenticualrstone.civilight.feature.cast.list.cards3d.Scene
import io.github.lenticualrstone.civilight.feature.cast.list.cards3d.TurnOrder
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs

/** 0.2.2: the turned fan card's `열기` can be tapped, and a long press on any 3D card opens it. */
class Cards022Test {
    private val slop = 18f

    @Test
    fun aStillPressWaitsAndMovingPastTheSlopCancels() {
        assertEquals(Verdict.WAIT, LongPressGate.verdict(0f, 0f, slop, lifted = false, takenByOther = false, pointers = 1))
        assertEquals(Verdict.WAIT, LongPressGate.verdict(10f, -12f, slop, lifted = false, takenByOther = false, pointers = 1))
        // Exactly on the slop circle still counts as still; past it in any direction cancels.
        assertEquals(Verdict.WAIT, LongPressGate.verdict(slop, 0f, slop, lifted = false, takenByOther = false, pointers = 1))
        assertEquals(Verdict.CANCEL, LongPressGate.verdict(slop + 0.5f, 0f, slop, lifted = false, takenByOther = false, pointers = 1))
        assertEquals(Verdict.CANCEL, LongPressGate.verdict(0f, -(slop + 0.5f), slop, lifted = false, takenByOther = false, pointers = 1))
        assertEquals(Verdict.CANCEL, LongPressGate.verdict(13f, 13f, slop, lifted = false, takenByOther = false, pointers = 1))
    }

    @Test
    fun liftingADragTakingTheFingerOrASecondFingerCancels() {
        assertEquals(Verdict.CANCEL, LongPressGate.verdict(0f, 0f, slop, lifted = true, takenByOther = false, pointers = 0))
        assertEquals(Verdict.CANCEL, LongPressGate.verdict(2f, 0f, slop, lifted = false, takenByOther = true, pointers = 1))
        assertEquals(Verdict.CANCEL, LongPressGate.verdict(0f, 0f, slop, lifted = false, takenByOther = false, pointers = 2))
    }

    @Test
    fun theLongPressReadsOpenForACharacterAndTheUnlockPathForTheLockCard() {
        assertEquals(IconButtonCopy.OPEN, Cards3dCopy.LONG_PRESS_OPEN)
        assertEquals(IconButtonCopy.SHOW_WITH_FINGERPRINT, Cards3dCopy.LONG_PRESS_UNLOCK)
        val card = CastCard(CastFx.key("CHR", 1), "리엔", "", null, holo = false, initial = "리")
        assertEquals(Cards3dCopy.LONG_PRESS_OPEN, LongPressGate.label(DeckItem.Character(card)))
        assertEquals(Cards3dCopy.LONG_PRESS_UNLOCK, LongPressGate.label(DeckItem.Lock(LockRow(2))))
    }

    /**
     * Compose hit-tests a layer with `Matrix.resetToPivotedTransform`. The turned fan card solves to
     * rotationX ≈ 180 · rotationZ ≈ 180; with the card's move on a pure translation layer of its own, the hit
     * matrix puts the back's `열기` (its bottom centre) where it is drawn: the pivot turn, then the move.
     */
    @Test
    fun theTurnedFanCardIsHitWhereItIsDrawn() {
        val scene = Scene(1000f, 195f, 230f, 195f, 250f)
        val xf = CardXform()
        val p = CardPoses.fan(0f, 1f, CardPose())!!
        Projector().solve(xf, scene, 0f, 0f, 195f, 250f, p.x, p.y, p.z, p.rotX, p.rotY, p.rotZ, TurnOrder.ZY, flipY = 180f, scale = p.scale)
        assertTrue(xf.backFacing)
        assertEquals(180f, abs(xf.rotationX), 1e-3f)
        assertTrue("the turned card moves up", xf.translationY < -20f)

        val w = CardPoses.DECK_W
        val h = CardPoses.DECK_H
        val pivot = Offset(w / 2, h / 2)
        // The back's `열기` sits at the bottom centre of the face; the face is drawn mirrored back, x only.
        val button = Offset(w / 2 + 40f, h - 30f)
        // Drawn: turned about the pivot (x mirrored, y kept), scaled, then moved.
        val drawn = Offset(
            pivot.x - (button.x - pivot.x) * xf.scale + xf.translationX,
            pivot.y + (button.y - pivot.y) * xf.scale + xf.translationY,
        )
        val move = Matrix().apply { resetToPivotedTransform(translationX = xf.translationX, translationY = xf.translationY) }
        val turn = Matrix().apply {
            resetToPivotedTransform(
                pivotX = pivot.x, pivotY = pivot.y,
                rotationX = xf.rotationX, rotationY = xf.rotationY, rotationZ = xf.rotationZ,
                scaleX = xf.scale, scaleY = xf.scale,
            )
        }
        val hit = move.map(turn.map(button))
        assertEquals(drawn.x, hit.x, 0.5f)
        assertEquals(drawn.y, hit.y, 0.5f)
    }
}
