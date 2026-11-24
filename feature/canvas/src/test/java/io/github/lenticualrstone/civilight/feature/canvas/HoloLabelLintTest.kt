package io.github.lenticualrstone.civilight.feature.canvas

import io.github.lenticualrstone.civilight.designsystem.card.CardFit
import io.github.lenticualrstone.civilight.designsystem.card.CardMetrics
import io.github.lenticualrstone.civilight.feature.canvas.gallery.GalleryCardFit
import io.github.lenticualrstone.civilight.ui.gallery.GalleryCardRules
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * 0.2.1 holo and gallery-card checks: `홀로` never shows without the effect (every source drawing an image chip
 * also puts a holo surface on its image), and the gallery shelf keeps the board's 222 × 296 card on real phones
 * (scr_gallery `shelf`: 222 × 296, pitch 236; small text below 190 dp wide).
 */
class HoloLabelLintTest {
    private val surface = Regex("""\b(HoloSurface|holoImage|holoSurface|GalleryImage)\s*\(""")

    @Test
    fun everyImageChipComesWithTheFoil() {
        val offenders = File("src/main").walkTopDown()
            .filter { it.isFile && it.extension == "kt" }
            .filter { f -> f.readText().let { it.contains("ImageStatusChip(") && !surface.containsMatchIn(it) } }
            .map { it.name }
            .toList()
        assertEquals(emptyList<String>(), offenders)
    }

    /** Viewport (dp) and the bars' paddings of common phones, with the measured chips + order line. */
    private data class Phone(val w: Float, val h: Float, val top: Float, val bottom: Float)

    @Test
    fun shelfCardsStayAtTheBoardSizeOnRealPhones() {
        val spec = GalleryCardRules.size(inner = false)
        assertEquals(222f, spec.width)
        assertEquals(296f, spec.height)
        val phones = listOf(
            Phone(360f, 740f, 104f, 112f),
            Phone(384f, 832f, 104f, 112f),
            Phone(390f, 844f, 110f, 120f),
            Phone(412f, 892f, 110f, 120f),
            Phone(412f, 915f, 118f, 126f),
        )
        for (p in phones) {
            val room = CardFit.room(p.h, p.top + GalleryCardFit.TOP_GAP_DP, p.bottom, GalleryCardFit.HEAD_ESTIMATE_DP, GalleryCardFit.ROW_EXTRA_DP)
            val size = CardFit.fit(spec, room, p.w, CardMetrics.inset(false), CardMetrics.gap(false))
            assertEquals("$p", spec, size)
        }
    }

    @Test
    fun smallTextOnlyBelowTheBoardsBigCard() {
        assertFalse(GalleryCardRules.small(222f))
        assertFalse(GalleryCardRules.small(196f))
        assertTrue(GalleryCardRules.small(162f))
        assertTrue(GalleryCardRules.small(132f))
    }
}

/** The DS holo surface reads the shared DS-74 tilt; its sign must match the CNV-30 reading and the board. */
class HoloTiltSignTest {
    @Test
    fun dsTiltAfterTheHoloFlipLeansLikeHoloRules() {
        val tm = io.github.lenticualrstone.civilight.designsystem.card.TiltMath
        val ds = io.github.lenticualrstone.civilight.designsystem.component.HoloMath
        val rules = io.github.lenticualrstone.civilight.feature.canvas.image.HoloRules
        // Upright base, then the right edge down (gravity row x < 0) and the top towards the viewer.
        val base = rules.anglesFromGravity(0f, 0.7071f, 0.7071f)
        for ((gx, gy, gz) in listOf(Triple(-0.1f, 0.7f, 0.7f), Triple(0.1f, 0.7f, 0.7f), Triple(0f, 0.6f, 0.8f), Triple(0f, 0.8f, 0.6f))) {
            val holo = rules.fromAngles(rules.anglesFromGravity(gx, gy, gz), base)
            val leanDs = ds.fromDeviceTilt(tm.normalized(tm.lean(tm.screenX(gx, gy, 0)), tm.lean(tm.screenX(0f, 0.7071f, 0))))
            val pitchDs = ds.fromDeviceTilt(
                tm.normalized(tm.pitch(tm.screenY(gx, gy, 0), gz), tm.pitch(tm.screenY(0f, 0.7071f, 0), 0.7071f)),
            )
            if (holo.x != 0f || leanDs != 0f) assertEquals("lean $gx $gy $gz", Math.signum(holo.x), Math.signum(leanDs))
            if (holo.y != 0f || pitchDs != 0f) assertEquals("pitch $gx $gy $gz", Math.signum(holo.y), Math.signum(pitchDs))
        }
    }
}
