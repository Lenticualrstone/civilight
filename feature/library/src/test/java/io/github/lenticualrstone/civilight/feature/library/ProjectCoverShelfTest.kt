package io.github.lenticualrstone.civilight.feature.library

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import io.github.lenticualrstone.civilight.domain.library.masking.HiddenAccess
import io.github.lenticualrstone.civilight.feature.library.LibFx.NOW
import io.github.lenticualrstone.civilight.feature.library.LibFx.ZONE
import io.github.lenticualrstone.civilight.feature.library.LibFx.at
import io.github.lenticualrstone.civilight.feature.library.LibFx.shelf
import io.github.lenticualrstone.civilight.feature.library.export.CoverPreview
import io.github.lenticualrstone.civilight.feature.library.projectsettings.ProjectSettingsCopy
import io.github.lenticualrstone.civilight.feature.library.projectsettings.ProjectSettingsState
import io.github.lenticualrstone.civilight.feature.library.shelf.CoverPickerStores
import io.github.lenticualrstone.civilight.feature.library.shelf.ShelfCards
import io.github.lenticualrstone.civilight.feature.library.shelf.ShelfMenuSheet
import io.github.lenticualrstone.civilight.feature.library.shelf.ShelfProject
import io.github.lenticualrstone.civilight.feature.library.shelf.ShelfState
import io.github.lenticualrstone.civilight.feature.library.shelf.ShelfUi
import io.github.lenticualrstone.civilight.model.app.OnboardingState
import io.github.lenticualrstone.civilight.model.app.RatingDefaults
import io.github.lenticualrstone.civilight.model.app.RelockPolicy
import io.github.lenticualrstone.civilight.model.app.UnlockSession
import io.github.lenticualrstone.civilight.model.asset.Crop
import io.github.lenticualrstone.civilight.model.asset.ImageOwnerKind
import io.github.lenticualrstone.civilight.model.asset.ImageSlot
import io.github.lenticualrstone.civilight.model.asset.ImageUse
import io.github.lenticualrstone.civilight.model.asset.Ratio
import io.github.lenticualrstone.civilight.model.doc.ProjectDoc
import io.github.lenticualrstone.civilight.model.open
import io.github.lenticualrstone.civilight.model.project.Protections
import io.github.lenticualrstone.civilight.model.project.Rating
import io.github.lenticualrstone.civilight.model.recorder.RecorderLibrary
import io.github.lenticualrstone.civilight.navigation.ImagePickSheet
import io.github.lenticualrstone.civilight.ui.image.ProjectCovers
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** U-08 프로젝트 표지 on the 서재 (60 §7.3 CV-19 … CV-26, CV-31): missing originals, masking, capture, settings. */
class ProjectCoverShelfTest {
    private val ashen = LibFx.ashen()
    private val glass = LibFx.glass()
    private val oath = LibFx.oath()
    private val locked = HiddenAccess()
    private val open = HiddenAccess(UnlockSession(hiddenGroupUnlockedAt = NOW))

    private fun withCover(doc: ProjectDoc, source: String, holo: Boolean = false): ProjectDoc = doc.copy(
        assets = doc.assets.copy(
            imageUses = doc.assets.imageUses.filterNot { it.slot.known == ImageSlot.PROJECT_COVER } +
                ImageUse("cover-${doc.key}", ImageOwnerKind.PROJECT.open(), doc.key, ImageSlot.PROJECT_COVER.open(), source, holo = holo)
                    .withCrop(Ratio.R3X4, Crop(0, 0, 300, 400))
                    .withCrop(Ratio.R1X1, Crop(0, 50, 300, 300)),
        ),
    )

    private fun protect(doc: ProjectDoc, rating: Rating, p: Protections): ProjectDoc =
        doc.copy(project = doc.project.copy(rating = rating.open(), protections = p))

    private fun build(list: List<ShelfProject>, access: HiddenAccess, fileExists: (String) -> Boolean = { true }): ShelfUi =
        ShelfState.build(list, access, RelockPolicy.ON_LEAVE, RecorderLibrary(), OnboardingState(firstRunCompleted = true), NOW, ZONE, showCards = true, fileExists = fileExists)

    private fun hashesIn(ui: ShelfUi): Set<String?> =
        (ui.rows.map { it.thumb.hash } + ui.cards.map { it.cover.hash } + ui.openGroup?.rows.orEmpty().map { it.thumb.hash }).toSet()

    @Test
    fun `CV-19 a cover whose original is gone draws the initial-letter card and makes no preview`() {
        val doc = withCover(glass, "h9", holo = true)
        val gone: (String) -> Boolean = { it != "h9" }
        val cover = ShelfCards.cover(doc, "유리 정원", gone)
        assertNull(cover.hash)
        assertNull(cover.presetId)
        assertEquals("유", cover.initial)
        assertFalse("no foil over the initial-letter card", cover.holo)
        assertNull(ShelfCards.thumb(doc, "유리 정원", gone).hash)
        assertNull(CoverPreview.source(doc, gone))
        assertEquals("h9", CoverPreview.source(doc)!!.blobHash)
        // Through the 서재 build too.
        val ui = build(listOf(shelf(doc, at("2026-11-19T10:00"))), locked, gone)
        assertNull(ui.rows.single().thumb.hash)
        assertNull(ui.cards.single().cover.hash)
        assertEquals("h9", build(listOf(shelf(doc, at("2026-11-19T10:00"))), locked).cards.single().cover.hash)
    }

    @Test
    fun `presets draw their own picture, an unknown preset the initial-letter card`() {
        assertEquals("lantern", ShelfCards.cover(withCover(glass, "preset:lantern"), "유리 정원").presetId)
        val unknown = ShelfCards.cover(withCover(glass, "preset:gone"), "유리 정원")
        assertNull(unknown.presetId)
        assertEquals("유", unknown.initial)
        assertEquals("the first grapheme, not half of one", "👩‍🎨", ProjectCovers.initialOf(" 👩‍🎨 작업실"))
        assertEquals("", ProjectCovers.initialOf("  "))
    }

    @Test
    fun `CV-20 a locked hidden project's cover is never read, CV-21 the open group shows it`() {
        val secret = withCover(oath, "h3")
        assertTrue(secret.project.protections.hidden)
        val list = listOf(shelf(glass, at("2026-11-19T10:00")), shelf(secret, at("2026-11-02T10:00")))
        val lockedUi = build(list, locked)
        assertFalse("h3" in hashesIn(lockedUi))
        assertTrue(lockedUi.cards.none { it.row.key == secret.key })
        assertEquals("이름 · 표지 가림", lockedUi.lockedGroup?.cardSub)

        val openUi = build(list, open)
        assertEquals("h3", openUi.openGroup!!.rows.single().thumb.hash)
        val card = openUi.cards.single { it.row.key == secret.key }
        assertEquals("h3", card.cover.hash)
        assertTrue(card.hiddenOpen)
    }

    @Test
    fun `CV-22 the cover picker of a hidden project closes on re-lock`() {
        val secret = withCover(oath, "h3")
        val list = listOf(shelf(glass, at("2026-11-19T10:00")), shelf(secret, at("2026-11-02T10:00")))
        val picker = ImagePickSheet(secret.key, ImageOwnerKind.PROJECT, secret.key, ImageSlot.PROJECT_COVER)
        assertEquals(0, ShelfState.sheetsToClose(build(list, open), listOf(picker)))
        assertEquals(1, ShelfState.sheetsToClose(build(list, locked), listOf(picker)))
        assertEquals(1, ShelfState.sheetsToClose(build(list, locked), listOf(ShelfMenuSheet(glass.key), picker)))
        // A visible project's picker stays.
        assertEquals(0, ShelfState.sheetsToClose(build(list, locked), listOf(ImagePickSheet(glass.key, ImageOwnerKind.PROJECT, glass.key, ImageSlot.PROJECT_COVER))))
    }

    @Test
    fun `CV-23 a hidden project makes no preview`() {
        assertNull(CoverPreview.source(withCover(oath, "h3")))
    }

    @Test
    fun `CV-24 raising to R-18 with hiding takes the cover off the 서재 at once`() {
        val before = withCover(ashen, "h1")
        val after = protect(before, Rating.R18, Protections(hidden = true, captureBlock = true, nameMask = true))
        assertTrue("h1" in hashesIn(build(listOf(shelf(before, at("2026-11-22T08:12"))), locked)))
        val ui = build(listOf(shelf(after, at("2026-11-22T08:12"))), locked)
        assertFalse("h1" in hashesIn(ui))
        assertEquals("숨긴 프로젝트 1", ui.lockedGroup?.title)
    }

    @Test
    fun `CV-25 a picked cover of a capture-blocked project protects the window, CV-26 a preset does not`() {
        val r18 = protect(withCover(ashen, "h1"), Rating.R18, Protections(captureBlock = true, nameMask = true))
        val card = build(listOf(shelf(r18, at("2026-11-22T08:12"))), locked).cards.single()
        assertEquals("h1", card.cover.hash)
        assertEquals("잿빛 새벽의 등불", card.row.title)
        assertEquals(Rating.R18, card.row.rating)
        assertTrue(ShelfState.coverProtects(card.row.protections, card.cover))
        assertTrue(ShelfState.coverProtects(card.row.protections, build(listOf(shelf(r18, at("2026-11-22T08:12"))), locked).rows.single().thumb))

        val preset = protect(withCover(ashen, "preset:lantern"), Rating.R18, Protections(captureBlock = true, nameMask = true))
        assertFalse(ShelfState.coverProtects(preset.project.protections, ShelfCards.cover(preset, "잿빛 새벽의 등불")))
        val plain = withCover(ashen, "h1")
        assertFalse(ShelfState.coverProtects(plain.project.protections, ShelfCards.cover(plain, "잿빛 새벽의 등불")))
    }

    @Test
    fun `CV-31 프로젝트 설정 opens with the 표지 row`() {
        val ui = ProjectSettingsState.build(glass, readOnly = false, defaults = RatingDefaults())
        assertEquals("표지", ProjectSettingsCopy.SECTION_COVER)
        assertEquals("표지 이미지", ui.cover.title)
        assertEquals("서재 카드 · 목록 썸네일 · 프로젝트 홈", ui.cover.sub)
        assertEquals("없음", ui.cover.value)
        assertEquals("등불", ProjectSettingsState.build(withCover(glass, "preset:lantern"), false, RatingDefaults()).cover.value)
        assertEquals("없음", ProjectSettingsState.build(withCover(glass, "h9"), false, RatingDefaults(), fileExists = { false }).cover.value)
        assertTrue("the example disables it with every row", ProjectSettingsState.build(ashen, readOnly = true, defaults = RatingDefaults()).readOnly)
    }

    private class Probe : ViewModel() {
        var cleared = false
        override fun onCleared() {
            cleared = true
        }
    }

    private fun probeOf(stores: CoverPickerStores, key: String): Probe =
        ViewModelProvider(
            stores.ownerFor(key),
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T = Probe() as T
            },
        )[Probe::class.java]

    @Test
    fun `the 서재's picker lets a project's view model go once the project leaves the 서재`() {
        val stores = CoverPickerStores()
        val a = probeOf(stores, "A")
        val b = probeOf(stores, "B")
        assertTrue("the same project keeps its view model", a === probeOf(stores, "A"))
        stores.retain(setOf("A"))
        assertTrue(b.cleared)
        assertFalse(a.cleared)
        assertEquals(setOf("A"), stores.held)
        stores.clearAll()
        assertTrue(a.cleared)
        assertTrue(stores.held.isEmpty())
    }
}
