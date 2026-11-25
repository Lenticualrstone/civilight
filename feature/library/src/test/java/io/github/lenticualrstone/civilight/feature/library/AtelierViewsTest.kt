package io.github.lenticualrstone.civilight.feature.library

import io.github.lenticualrstone.civilight.designsystem.text.CopyLint
import io.github.lenticualrstone.civilight.feature.library.shelf.ShelfViewRenderers
import io.github.lenticualrstone.civilight.feature.library.shelf.atelier.AtelierCopy
import io.github.lenticualrstone.civilight.feature.library.shelf.atelier.AtelierRegistry
import io.github.lenticualrstone.civilight.feature.library.shelf.atelier.DetentPhysics
import io.github.lenticualrstone.civilight.feature.library.shelf.flat.PickGroup
import io.github.lenticualrstone.civilight.feature.library.shelf.flat.ViewPick
import io.github.lenticualrstone.civilight.model.app.AppSettings
import io.github.lenticualrstone.civilight.model.app.DragResistance
import io.github.lenticualrstone.civilight.model.app.LibraryStyle
import io.github.lenticualrstone.civilight.model.json.CivilightJson
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AtelierViewsTest {
    @Test
    fun `all 6 Atelier views are registered and grouped 1 physical 3D + 5 kinetic`() {
        assertEquals(6, LibraryStyle.entries.size)
        assertEquals(1, AtelierRegistry.spatialStyles.size)
        assertEquals(5, AtelierRegistry.kineticStyles.size)
        assertEquals(LibraryStyle.entries.toSet(), ShelfViewRenderers.available)

        val entries = ViewPick.entries()
        assertEquals(6, entries.size)
        assertEquals(1, entries.count { it.group == PickGroup.SPATIAL })
        assertEquals(5, entries.count { it.group == PickGroup.KINETIC })
        entries.forEach { e ->
            assertTrue("glyph present for ${e.style}", e.glyph.isNotBlank())
            assertTrue("label present for ${e.style}", e.label.isNotBlank())
            assertTrue("frag present for ${e.style}", e.frag.isNotBlank())
        }
    }

    @Test
    fun `AtelierCopy passes CopyLint with zero violations`() {
        AtelierCopy.all.forEach { text ->
            val issues = CopyLint.lint(text)
            assertTrue("CopyLint issues on '$text': $issues", issues.isEmpty())
        }
    }

    @Test
    fun `legacy stored LibraryStyle values including floppyCrt safely migrate to CD_JEWEL_CASE`() {
        for (legacy in listOf("floppyCrt", "bookshelf", "glassPrism", "archiveVault", "timeline", "table")) {
            val decoded = CivilightJson.decode(AppSettings.serializer(), """{"libraryStyle":"$legacy"}""")
            assertEquals("legacy '$legacy' falls back to CD_JEWEL_CASE", LibraryStyle.CD_JEWEL_CASE, decoded.libraryStyle)
        }
    }

    @Test
    fun `mechanical 1-step detent physics resists before breakaway and locks to 1 step`() {
        val firm = DragResistance.FIRM
        val pre = DetentPhysics.applyResistance(0.30f, firm)
        assertTrue("pre-breakaway travel should be damped ($pre < 0.20)", pre < 0.20f)
        // Below threshold with low velocity snaps back to anchor
        assertEquals(2, DetentPhysics.settleTarget(2, 0.25f, 0.5f, 6, firm))
        // Past threshold locks to exactly +1 step
        assertEquals(3, DetentPhysics.settleTarget(2, 0.55f, 3.0f, 6, firm))
        // Only extreme flick allows +2 steps
        assertEquals(4, DetentPhysics.settleTarget(2, 0.55f, 8.0f, 6, firm))
    }
}
