package io.github.lenticualrstone.civilight.ui

import io.github.lenticualrstone.civilight.ui.gallery.PresetPictures
import io.github.lenticualrstone.civilight.ui.gallery.ThumbSource
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** Stored image sources → what a small thumbnail draws (CNV-26 presets, GAL-04 search thumbnails). */
class PresetPictureTest {
    @Test
    fun presetSourcesResolveToTheirIndex() {
        assertEquals(8, PresetPictures.IDS.size)
        assertEquals(0, PresetPictures.indexOfSource("preset:ash-dawn"))
        assertEquals(2, PresetPictures.indexOfSource("preset:deep-sea"))
        assertEquals(7, PresetPictures.indexOfSource("preset:snowfield"))
        assertNull(PresetPictures.indexOfSource("preset:unknown"))
        assertNull(PresetPictures.indexOfSource("deep-sea"))
        assertNull(PresetPictures.indexOfSource(null))
    }

    @Test
    fun thumbSourceIsPresetBlobOrNone() {
        assertEquals(ThumbSource.Preset(2), ThumbSource.of("preset:deep-sea"))
        assertEquals(ThumbSource.Blob("ab12"), ThumbSource.of("ab12"))
        assertEquals(ThumbSource.None, ThumbSource.of(""))
        assertEquals(ThumbSource.None, ThumbSource.of(null))
        assertEquals(ThumbSource.None, ThumbSource.of("preset:unknown"))
    }
}
