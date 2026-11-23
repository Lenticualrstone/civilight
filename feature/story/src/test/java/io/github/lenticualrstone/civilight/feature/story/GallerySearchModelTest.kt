package io.github.lenticualrstone.civilight.feature.story

import io.github.lenticualrstone.civilight.domain.story.search.GalleryMatchField
import io.github.lenticualrstone.civilight.domain.story.search.ProjectSearch
import io.github.lenticualrstone.civilight.domain.story.search.SearchContext
import io.github.lenticualrstone.civilight.feature.story.search.GalleryScope
import io.github.lenticualrstone.civilight.feature.story.search.SearchCopy
import io.github.lenticualrstone.civilight.feature.story.search.SearchModel
import io.github.lenticualrstone.civilight.feature.story.search.SearchTap
import io.github.lenticualrstone.civilight.model.asset.ImageUse
import io.github.lenticualrstone.civilight.model.gallery.GalleryOwnerKind
import io.github.lenticualrstone.civilight.model.sample.ExampleGallery
import io.github.lenticualrstone.civilight.navigation.Gallery
import io.github.lenticualrstone.civilight.navigation.ProjectHome
import io.github.lenticualrstone.civilight.ui.gallery.PresetPictures
import io.github.lenticualrstone.civilight.ui.gallery.ThumbSource
import io.github.lenticualrstone.civilight.model.sample.ExampleKeys as K
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Gallery images in the command-bar search (55 §3.5, GAL-04, T-GAL-19). Emulator round 1: a description word
 * found nothing from the project command bar, and the gallery had no command bar of its own.
 */
class GallerySearchModelTest {
    private val g0 = ExampleGallery.build()
    private val p = g0.project.key

    @Test
    fun projectSearchListsTheGallerySection() {
        val result = ProjectSearch(g0, SearchContext(locked = true)).search("그을음", includeGallery = true)
        assertNull("a description hit is a result, not `결과 없음`", result.empty)
        val sections = SearchModel.gallerySections(result, scoped = false)
        assertEquals(listOf(SearchCopy.GALLERY), sections.map { it.header })
        val hit = sections.single().hits.single()
        assertEquals(ExampleGallery.ITEM_3, hit.itemKey)
        assertEquals(ExampleGallery.SOOT_SENTENCE, hit.match.text)
        assertEquals(SearchTap.Open(Gallery(p, GalleryOwnerKind.CHARACTER, K.RIEN, pickKey = ExampleGallery.ITEM_3)), SearchModel.tap(p, hit, scope = null))
        assertEquals("gallery:${ExampleGallery.ITEM_3}", SearchModel.galleryKey(hit))
        val two = ProjectSearch(g0, SearchContext(locked = true)).search("등불", includeGallery = true)
        assertEquals("${SearchCopy.GALLERY} · 2", SearchModel.gallerySections(two, scoped = false).single().header)
    }

    @Test
    fun scopeComesFromTheGalleryUnderTheOverlay() {
        assertNull(SearchModel.galleryScope(ProjectHome(p)))
        val rien = SearchModel.galleryScope(Gallery.ofCharacter(p, K.RIEN))
        assertEquals(GalleryScope(GalleryOwnerKind.CHARACTER, K.RIEN), rien)
        assertEquals(SearchCopy.GALLERY_FIND, rien!!.hint)
        assertEquals(K.RIEN, rien.onlyOwnerKey)
        val all = SearchModel.galleryScope(Gallery.ofProject(p))!!
        assertEquals(SearchCopy.ALL_GALLERIES_FIND, all.hint)
        assertNull(all.onlyOwnerKey)
    }

    @Test
    fun tGal19_scopedResultBySectionAndOpensThatGalleryPicked() {
        val scope = GalleryScope(GalleryOwnerKind.CHARACTER, K.RIEN)
        val result = SearchModel.galleryResult(g0, "그을음", locked = true, scope = scope)
        assertTrue(result.sections.isEmpty())
        val sections = SearchModel.gallerySections(result, scoped = true)
        assertEquals(listOf("${GalleryMatchField.DESCRIPTION.word} · 1"), sections.map { it.header })
        val hit = sections.single().hits.single()
        assertEquals(SearchTap.ReplaceBelow(Gallery(p, GalleryOwnerKind.CHARACTER, K.RIEN, pickKey = ExampleGallery.ITEM_3)), SearchModel.tap(p, hit, scope))
        val fromProject = SearchModel.tap(p, hit, GalleryScope(GalleryOwnerKind.PROJECT, p))
        assertEquals(SearchTap.ReplaceBelow(Gallery.ofProject(p, pickKey = ExampleGallery.ITEM_3)), fromProject)
    }

    @Test
    fun scopedSearchStaysInItsGalleryAndKeepsHiddenImagesOutWhileLocked() {
        val other = SearchModel.galleryResult(g0, "그을음", locked = true, scope = GalleryScope(GalleryOwnerKind.CHARACTER, K.SERA))
        assertTrue(other.gallery.isEmpty())
        assertNotNull(other.empty)
        val empty = other.empty!!
        assertTrue("no `비슷한 이름` in a gallery search", empty.similar.isEmpty())

        val rien = GalleryScope(GalleryOwnerKind.CHARACTER, K.RIEN)
        val locked = SearchModel.galleryResult(g0, "숨긴", locked = true, scope = rien)
        assertTrue(locked.gallery.isEmpty())
        assertTrue(locked.hiddenExcluded)
        assertEquals("‘숨긴’ · 숨긴 항목 제외", locked.empty!!.line)
        val open = SearchModel.galleryResult(g0, "숨긴", locked = false, scope = rien)
        assertEquals(listOf(ExampleGallery.ITEM_H1, ExampleGallery.ITEM_H2), open.gallery.map { it.itemKey })
    }

    /**
     * Emulator round 2: a preset gallery image (`심해`) showed an empty grey 44 thumbnail in the search. The hit
     * keeps its `preset:<id>` source and the row draws that preset; a blob hit still decodes its blob.
     */
    @Test
    fun presetHitDrawsItsPresetThumbnail_GAL04() {
        val items = g0.gallery.getValue(K.RIEN).items.map {
            if (it.key == ExampleGallery.ITEM_2) it.copy(source = ImageUse.PRESET_PREFIX + "deep-sea", title = "Harbor gate") else it
        }
        val doc = g0.copy(gallery = g0.gallery + (K.RIEN to g0.gallery.getValue(K.RIEN).copy(items = items)))
        val result = ProjectSearch(doc, SearchContext(locked = true)).search("Harbor", includeGallery = true)
        val hit = SearchModel.gallerySections(result, scoped = false).single().hits.single()
        assertEquals(ThumbSource.Preset(PresetPictures.IDS.indexOf("deep-sea")), ThumbSource.of(hit.source))
        val scoped = SearchModel.galleryResult(doc, "Harbor", locked = true, scope = GalleryScope(GalleryOwnerKind.PROJECT, p))
        assertEquals(ThumbSource.Preset(2), ThumbSource.of(scoped.gallery.single().source))
        val blobHit = ProjectSearch(doc, SearchContext(locked = true)).search("그을음", includeGallery = true).gallery.single()
        assertEquals(ThumbSource.Blob(ExampleGallery.hashOf(3)), ThumbSource.of(blobHit.source))
    }
}
