package io.github.lenticualrstone.civilight.domain.library

import io.github.lenticualrstone.civilight.domain.library.masking.CrossSearchScope
import io.github.lenticualrstone.civilight.domain.library.masking.HiddenAccess
import io.github.lenticualrstone.civilight.domain.library.masking.LibraryListing
import io.github.lenticualrstone.civilight.domain.library.masking.Notifications
import io.github.lenticualrstone.civilight.domain.library.masking.Presentation
import io.github.lenticualrstone.civilight.domain.library.masking.ProjectProfile
import io.github.lenticualrstone.civilight.domain.library.masking.ProjectSurfaces
import io.github.lenticualrstone.civilight.domain.library.masking.Surface
import io.github.lenticualrstone.civilight.domain.library.masking.VisibleContent
import io.github.lenticualrstone.civilight.domain.library.masking.WindowGuards
import io.github.lenticualrstone.civilight.model.app.AppSettings
import io.github.lenticualrstone.civilight.model.project.ProjectFormat
import io.github.lenticualrstone.civilight.model.project.Protections
import io.github.lenticualrstone.civilight.model.project.Rating
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** §4.2 masking surfaces (S-01 … S-12) and notification text (N-01, N-02), LIB-26…30. */
class MaskingTest {
    private val allOn = Protections(hidden = true, captureBlock = true, nameMask = true, recorderLinkMask = true)

    /** H = 검은 서약 (hidden, R-18). */
    private val h = ProjectProfile.of(Fixtures.projectWith("검은 서약 (가제)", Rating.R18, allOn, ProjectFormat.SETTING_ONLY, characters = 5))

    /** 외전 · 오웬의 시장 (R-15, hidden). */
    private val side = ProjectProfile.of(Fixtures.projectWith("외전 · 오웬의 시장", Rating.R15, Protections(hidden = true), scenes = 2))

    /** M = 잿빛 after R19-02 with hidden unchecked. */
    private val m = ProjectProfile.of(
        Fixtures.withProtections(Fixtures.example(), Rating.R18, allOn.copy(hidden = false)),
    )

    /** N = 유리 정원. */
    private val n = ProjectProfile.of(Fixtures.projectWith("유리 정원", Rating.ALL, format = ProjectFormat.SHORT, scenes = 6, characters = 4))

    private fun at(surface: Surface, p: ProjectProfile, access: HiddenAccess = HiddenAccess.LOCKED, settings: AppSettings = AppSettings()) =
        ProjectSurfaces.present(surface, p, access, settings)

    @Test
    fun `S-01 library row`() {
        assertEquals(Presentation.CountedHidden, at(Surface.LIBRARY_ROW, h))
        val listing = LibraryListing.of(listOf(h, side, m, n), HiddenAccess.LOCKED)
        assertEquals("숨긴 프로젝트 2", listing.lockedGroup!!.title)
        assertEquals("이름 가림 · 검색 · 위젯 · 알림 제외", listing.lockedGroup!!.sub)
        assertEquals("프로젝트 · 2", listing.sectionLabel)
        assertEquals(listOf(m, n), listing.rows)
        assertEquals(Presentation.Shown("잿빛 새벽의 등불", "R-18", redBadge = true), at(Surface.LIBRARY_ROW, m))
        assertEquals(Presentation.Shown("유리 정원", "ALL", redBadge = false), at(Surface.LIBRARY_ROW, n))
    }

    @Test
    fun `S-02 recents and widget lists`() {
        for (surface in listOf(Surface.RECENTS_LIST, Surface.WIDGET_LIST)) {
            assertEquals(Presentation.Omitted, at(surface, h))
            assertEquals(Presentation.Label("R-18 프로젝트"), at(surface, m))
            assertEquals("유리 정원", (at(surface, n) as Presentation.Shown).name)
        }
    }

    @Test
    fun `S-03 cross-project search`() {
        assertEquals(Presentation.Omitted, at(Surface.CROSS_SEARCH, h))
        val scope = CrossSearchScope.of(listOf(h, m, n), HiddenAccess.LOCKED)
        assertEquals("숨긴 프로젝트 제외", scope.footer)
        assertEquals(listOf(m.key, n.key), scope.projectKeys)
        assertEquals(Presentation.Shown("잿빛 새벽의 등불", "R-18", redBadge = true), at(Surface.CROSS_SEARCH, m))
        assertTrue(at(Surface.CROSS_SEARCH, n) is Presentation.Shown)
        assertEquals("‘노아’ · 숨긴 프로젝트 제외", CrossSearchScope.emptySub("노아", scope))
    }

    @Test
    fun `S-04 notifications`() {
        assertEquals(Presentation.CountedHidden, at(Surface.NOTIFICATION, h))
        assertTrue(Notifications.backupDone(listOf(h)).endsWith("숨긴 프로젝트 1"))
        assertEquals(Presentation.Label("R-18 프로젝트"), at(Surface.NOTIFICATION, m))
        assertEquals("유리 정원", (at(Surface.NOTIFICATION, n) as Presentation.Shown).name)
    }

    @Test
    fun `S-05 system recents preview`() {
        assertEquals(true, (at(Surface.SYSTEM_RECENTS_PREVIEW, h) as Presentation.Preview).masked)
        // Hiding alone masks the recents preview only; screenshots follow the capture setting.
        val hNoCapture = h.copy(protections = h.protections.copy(captureBlock = false))
        assertEquals(Presentation.Preview(masked = true, recentsOnly = true), at(Surface.SYSTEM_RECENTS_PREVIEW, hNoCapture))
        assertEquals(Presentation.Preview(masked = true, recentsOnly = false), at(Surface.SYSTEM_RECENTS_PREVIEW, m))
        assertEquals(Presentation.Preview(masked = false), at(Surface.SYSTEM_RECENTS_PREVIEW, n))
    }

    @Test
    fun `S-06 system recents label`() {
        assertEquals(Presentation.Label("Civilight"), at(Surface.SYSTEM_RECENTS_LABEL, h))
        assertEquals(Presentation.Label("R-18 프로젝트"), at(Surface.SYSTEM_RECENTS_LABEL, m))
        assertEquals(Presentation.Label("Civilight"), at(Surface.SYSTEM_RECENTS_LABEL, n))
        assertEquals("Civilight", WindowGuards.of(listOf(VisibleContent(h))).taskLabel)
        assertEquals("R-18 프로젝트", WindowGuards.of(listOf(VisibleContent(m))).taskLabel)
        assertNull(WindowGuards.of(listOf(VisibleContent(n))).taskLabel)
    }

    @Test
    fun `S-07 screenshots`() {
        assertEquals(Presentation.Screenshot(blocked = h.protections.captureBlock), at(Surface.SCREENSHOT, h))
        assertEquals(Presentation.Screenshot(blocked = false), at(Surface.SCREENSHOT, h.copy(protections = h.protections.copy(captureBlock = false))))
        assertEquals(Presentation.Screenshot(blocked = true), at(Surface.SCREENSHOT, m))
        assertEquals(Presentation.Screenshot(blocked = false), at(Surface.SCREENSHOT, n))
    }

    @Test
    fun `S-08 trash and recorder locations`() {
        for (surface in listOf(Surface.TRASH_LOCATION, Surface.RECORDER_LOCATION)) {
            assertEquals(Presentation.Label("숨긴 프로젝트", needsAuth = true), at(surface, h))
            assertEquals("잿빛 새벽의 등불", (at(surface, m) as Presentation.Shown).name)
            assertEquals("유리 정원", (at(surface, n) as Presentation.Shown).name)
        }
    }

    @Test
    fun `S-09 open hidden group is searched while the setting is on`() {
        val open = HiddenAccess.LOCKED.openGroup(Fixtures.NOW)
        assertEquals("검은 서약 (가제)", (at(Surface.CROSS_SEARCH, h, open) as Presentation.Shown).name)
        assertEquals(Presentation.Omitted, at(Surface.CROSS_SEARCH, h, open, AppSettings(hiddenInSearchWhileOpen = false)))
        assertNull(CrossSearchScope.of(listOf(h, m), open).footer)
    }

    @Test
    fun `S-10 open hidden group row of a capture-blocked project sets FLAG_SECURE`() {
        assertTrue(WindowGuards.captureGuard(listOf(VisibleContent(h))))
    }

    @Test
    fun `S-11 a 기록함 row linked to M sets FLAG_SECURE`() {
        assertTrue(WindowGuards.captureGuard(listOf(VisibleContent(n), VisibleContent(m))))
    }

    @Test
    fun `S-12 only the masked label of M does not set FLAG_SECURE`() {
        assertFalse(WindowGuards.captureGuard(listOf(VisibleContent(m, maskedOnly = true))))
        assertFalse(WindowGuards.of(listOf(VisibleContent(m, maskedOnly = true))).flagSecure)
    }

    @Test
    fun `N-01 a run with a hidden project counts`() {
        val others = listOf(m.copy(protections = Protections()), n, side.copy(protections = Protections(), name = "세 번째"))
        assertEquals("백업 끝 · 프로젝트 3 · 숨긴 프로젝트 1", Notifications.backupDone(others + h))
    }

    @Test
    fun `N-02 a run without hidden projects names them with masking`() {
        val ash = ProjectProfile.of(Fixtures.example())
        val oath = h.copy(protections = Protections(nameMask = true))
        assertEquals("백업 끝 · 잿빛 새벽의 등불 · 유리 정원 · R-18 프로젝트", Notifications.backupDone(listOf(ash, n, oath)))
    }

    @Test
    fun `recorder link label`() {
        assertEquals("숨긴 프로젝트", ProjectSurfaces.recorderLinkLabel(h))
        assertEquals("R-18", ProjectSurfaces.recorderLinkLabel(h, HiddenAccess.LOCKED.openGroup(Fixtures.NOW)))
        assertEquals("R-18", ProjectSurfaces.recorderLinkLabel(m))
        assertNull(ProjectSurfaces.recorderLinkLabel(n))
        // Lowered with the mask kept on: the rating shown is the current one (§6 Q8 keeps only the name label literal).
        assertEquals("R-15", ProjectSurfaces.recorderLinkLabel(m.copy(rating = Rating.R15)))
    }

    @Test
    fun `card section counts hidden projects and the open group replaces the plus button`() {
        val locked = LibraryListing.of(listOf(h, side, m, n), HiddenAccess.LOCKED)
        assertEquals("프로젝트 · 2 · 숨김 2", locked.cardSectionLabel)
        assertEquals("+", locked.headerAction)
        val open = LibraryListing.of(listOf(h, side, m, n), HiddenAccess.LOCKED.openGroup(Fixtures.NOW))
        assertNull(open.lockedGroup)
        assertEquals(listOf(h, side), open.openGroup)
        assertEquals("다시 잠그기", open.headerAction)
    }
}
