package io.github.lenticualrstone.civilight.ui

import io.github.lenticualrstone.civilight.ui.image.ExifOrientation
import io.github.lenticualrstone.civilight.ui.image.UprightTurn
import io.github.lenticualrstone.civilight.ui.project.ProjectOpenBeat
import org.junit.Assert.assertEquals
import org.junit.Test

/** Opening beat of `ProjectEntryFrame` and the EXIF orientation the shared image decoder applies. */
class ProjectOpenAndExifTest {
    @Test
    fun beatOnlyOnFirstOpen() {
        assertEquals(260L, ProjectOpenBeat.remainingMs(wasOpen = false, reduced = false, beatMs = 260, elapsedMs = 0))
        assertEquals(60L, ProjectOpenBeat.remainingMs(wasOpen = false, reduced = false, beatMs = 260, elapsedMs = 200))
        assertEquals(0L, ProjectOpenBeat.remainingMs(wasOpen = false, reduced = false, beatMs = 260, elapsedMs = 400))
    }

    @Test
    fun noBeatWhenAlreadyOpenOrReduced() {
        assertEquals(0L, ProjectOpenBeat.remainingMs(wasOpen = true, reduced = false, beatMs = 260, elapsedMs = 0))
        assertEquals(0L, ProjectOpenBeat.remainingMs(wasOpen = false, reduced = true, beatMs = 260, elapsedMs = 0))
    }

    @Test
    fun exifOrientationTurns() {
        assertEquals(UprightTurn(0, false), ExifOrientation.transform(ExifOrientation.NORMAL))
        assertEquals(UprightTurn(0, true), ExifOrientation.transform(ExifOrientation.FLIP_HORIZONTAL))
        assertEquals(UprightTurn(180, false), ExifOrientation.transform(ExifOrientation.ROTATE_180))
        assertEquals(UprightTurn(180, true), ExifOrientation.transform(ExifOrientation.FLIP_VERTICAL))
        assertEquals(UprightTurn(90, true), ExifOrientation.transform(ExifOrientation.TRANSPOSE))
        assertEquals(UprightTurn(90, false), ExifOrientation.transform(ExifOrientation.ROTATE_90))
        assertEquals(UprightTurn(270, true), ExifOrientation.transform(ExifOrientation.TRANSVERSE))
        assertEquals(UprightTurn(270, false), ExifOrientation.transform(ExifOrientation.ROTATE_270))
        // Undefined (0) and out-of-range values draw the pixels as stored.
        assertEquals(UprightTurn(0, false), ExifOrientation.transform(0))
        assertEquals(UprightTurn(0, false), ExifOrientation.transform(9))
    }

    @Test
    fun uprightSizeSwapsForQuarterTurns() {
        // A portrait phone photo stored landscape (4000×3000, ROTATE_90) is cropped in 3000×4000 space.
        assertEquals(3000 to 4000, ExifOrientation.uprightSize(4000, 3000, ExifOrientation.ROTATE_90))
        assertEquals(3000 to 4000, ExifOrientation.uprightSize(4000, 3000, ExifOrientation.ROTATE_270))
        assertEquals(3000 to 4000, ExifOrientation.uprightSize(4000, 3000, ExifOrientation.TRANSPOSE))
        assertEquals(3000 to 4000, ExifOrientation.uprightSize(4000, 3000, ExifOrientation.TRANSVERSE))
        assertEquals(4000 to 3000, ExifOrientation.uprightSize(4000, 3000, ExifOrientation.ROTATE_180))
        assertEquals(4000 to 3000, ExifOrientation.uprightSize(4000, 3000, ExifOrientation.FLIP_VERTICAL))
        assertEquals(4000 to 3000, ExifOrientation.uprightSize(4000, 3000, ExifOrientation.NORMAL))
    }
}
