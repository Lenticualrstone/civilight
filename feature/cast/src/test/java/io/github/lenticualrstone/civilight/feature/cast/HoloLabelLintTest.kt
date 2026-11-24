package io.github.lenticualrstone.civilight.feature.cast

import org.junit.Assert.assertEquals
import org.junit.Test
import java.io.File

/**
 * `홀로` never shows without the effect (0.2.1, user feedback on 0.2.0): every source that draws an image chip
 * or the `홀로` label must also put the DS holo surface on the image.
 */
class HoloLabelLintTest {
    private val surface = Regex("""\b(HoloSurface|holoImage|holoSurface|GalleryImage)\s*\(""")

    @Test
    fun everyHoloLabelComesWithTheFoil() {
        val offenders = File("src/main").walkTopDown()
            .filter { it.isFile && it.extension == "kt" }
            .filter { f ->
                val text = f.readText()
                (text.contains("ImageStatusChip(") || text.contains("CharacterImages.HOLO")) && !surface.containsMatchIn(text)
            }
            .map { it.name }
            .toList()
        assertEquals(emptyList<String>(), offenders)
    }
}
