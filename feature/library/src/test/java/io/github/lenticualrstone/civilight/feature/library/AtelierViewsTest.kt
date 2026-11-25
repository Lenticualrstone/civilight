package io.github.lenticualrstone.civilight.feature.library

import io.github.lenticualrstone.civilight.designsystem.text.CopyLint
import io.github.lenticualrstone.civilight.feature.library.shelf.ShelfViewRenderers
import io.github.lenticualrstone.civilight.feature.library.shelf.atelier.AtelierCopy
import io.github.lenticualrstone.civilight.feature.library.shelf.atelier.AtelierRegistry
import io.github.lenticualrstone.civilight.feature.library.shelf.flat.PickGroup
import io.github.lenticualrstone.civilight.feature.library.shelf.flat.ViewPick
import io.github.lenticualrstone.civilight.model.app.AppSettings
import io.github.lenticualrstone.civilight.model.app.LibraryStyle
import io.github.lenticualrstone.civilight.model.json.CivilightJson
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AtelierViewsTest {
    @Test
    fun `all 7 Atelier views are registered and grouped 2 physical 3D + 5 kinetic`() {
        assertEquals(7, LibraryStyle.entries.size)
        assertEquals(2, AtelierRegistry.spatialStyles.size)
        assertEquals(5, AtelierRegistry.kineticStyles.size)
        assertEquals(LibraryStyle.entries.toSet(), ShelfViewRenderers.available)

        val entries = ViewPick.entries()
        assertEquals(7, entries.size)
        assertEquals(2, entries.count { it.group == PickGroup.SPATIAL })
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
    fun `legacy stored LibraryStyle values safely migrate to CD_JEWEL_CASE`() {
        for (legacy in listOf("bookshelf", "glassPrism", "archiveVault", "timeline", "table")) {
            val decoded = CivilightJson.decode(AppSettings.serializer(), """{"libraryStyle":"$legacy"}""")
            assertEquals("legacy '$legacy' falls back to CD_JEWEL_CASE", LibraryStyle.CD_JEWEL_CASE, decoded.libraryStyle)
        }
    }
}
