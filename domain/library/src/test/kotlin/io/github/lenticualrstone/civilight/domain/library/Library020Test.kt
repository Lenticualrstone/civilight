package io.github.lenticualrstone.civilight.domain.library

import io.github.lenticualrstone.civilight.domain.library.export.ExportDefaults
import io.github.lenticualrstone.civilight.domain.library.export.ExportRow
import io.github.lenticualrstone.civilight.domain.library.export.ExportSheets
import io.github.lenticualrstone.civilight.domain.library.masking.HiddenAccess
import io.github.lenticualrstone.civilight.domain.library.masking.Notifications
import io.github.lenticualrstone.civilight.domain.library.masking.Presentation
import io.github.lenticualrstone.civilight.domain.library.masking.ProjectProfile
import io.github.lenticualrstone.civilight.domain.library.masking.ProjectSurfaces
import io.github.lenticualrstone.civilight.domain.library.masking.Surface
import io.github.lenticualrstone.civilight.domain.library.newproject.ProjectMenuAction
import io.github.lenticualrstone.civilight.domain.library.newproject.ProjectMenus
import io.github.lenticualrstone.civilight.domain.library.rating.Protection
import io.github.lenticualrstone.civilight.domain.library.rating.ProjectProtectionSettings
import io.github.lenticualrstone.civilight.domain.library.rating.RatingLabels
import io.github.lenticualrstone.civilight.model.app.AppSettings
import io.github.lenticualrstone.civilight.model.app.ExportFormat
import io.github.lenticualrstone.civilight.model.doc.ProjectDoc
import io.github.lenticualrstone.civilight.model.project.Protections
import io.github.lenticualrstone.civilight.model.project.Rating
import io.github.lenticualrstone.civilight.model.version.EpisodeVersion
import io.github.lenticualrstone.civilight.model.version.EpisodeVersionsFile
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** 0.2.0 library rules of spec 60 §8.3 that live in `domain:library` (LIB-91, LIB-92, LIB-97). */
class Library020Test {
    private val allOn = Protections(hidden = true, captureBlock = true, nameMask = true, recorderLinkMask = true)
    private val oath = Fixtures.projectWith("검은 서약 (가제)", Rating.R18, allOn, scenes = 1)
    private val glass = Fixtures.projectWith("유리 정원", Rating.ALL, scenes = 2)

    private fun withGameTools(doc: ProjectDoc, on: Boolean?) = doc.copy(project = doc.project.copy(gameTools = on))

    @Test
    fun `R-01 수위 등급 row restored in project menu`() {
        for (doc in listOf(glass, oath, Fixtures.withProtections(glass, Rating.R15, Protections()))) {
            val menu = ProjectMenus.of(doc)
            assertTrue(menu.rows.any { it.action == ProjectMenuAction.RATING })
        }
        assertEquals(
            listOf(
                ProjectMenuAction.OPEN, ProjectMenuAction.RENAME, ProjectMenuAction.COVER,
                ProjectMenuAction.RATING, ProjectMenuAction.UNHIDE, ProjectMenuAction.BACKUP_EXPORT, ProjectMenuAction.DELETE,
            ),
            ProjectMenus.of(oath).rows.map { it.action },
        )
    }

    @Test
    fun `R-02 protection rows keep their values, carry no rating marker, section 숨기기`() {
        val settings = ProjectProtectionSettings.withoutRating(Rating.R18, allOn)
        assertEquals("숨기기", ProjectProtectionSettings.SECTION_020)
        assertEquals(Protection.entries, settings.rows.map { it.protection })
        assertTrue(settings.rows.all { it.on })
        assertTrue(settings.rows.all { it.differsMarker == null })
        assertTrue(settings.rows.none { it.fullSub.contains("R-18") || it.fullSub.contains("등급") || it.fullSub.contains("기본과 다름") })
        assertEquals(ProjectProtectionSettings.TODO_COPY_LIB_RECORDER_MASK_SUB, settings.rows.single { it.protection == Protection.RECORDER_LINK_MASK }.sub)
        assertEquals(ProjectProtectionSettings.TODO_COPY_LIB_NAME_MASK_SUB, settings.rows.single { it.protection == Protection.NAME_MASK }.sub)
        // The stored rating is kept in the model (D34), only never drawn.
        assertEquals(Rating.R18, settings.rating)
        // A 0.1.0 R-15 project with every switch off: still nothing about ratings.
        val r15 = ProjectProtectionSettings.withoutRating(Rating.R15, Protections())
        assertTrue(r15.rows.none { it.on || it.differsMarker != null })
    }

    @Test
    fun `R-03 a masked name never names a rating`() {
        val profile = ProjectProfile.of(Fixtures.projectWith("잿빛 새벽의 등불", Rating.R18, Protections(nameMask = true), scenes = 1))
        for (surface in listOf(Surface.RECENTS_LIST, Surface.NOTIFICATION, Surface.SYSTEM_RECENTS_LABEL)) {
            val shown = ProjectSurfaces.present(surface, profile, HiddenAccess.LOCKED, AppSettings())
            assertEquals(Presentation.Label(RatingLabels.TODO_COPY_LIB_MASKED_NAME), shown)
        }
        assertEquals("이름 가림", RatingLabels.MASKED_PROJECT)
        assertFalse(RatingLabels.MASKED_PROJECT.contains("R-18"))
        assertFalse(Notifications.backupDone(listOf(profile)).contains(RatingLabels.MASKED_PROJECT_010))
    }

    @Test
    fun `R-04 export sheet sub and format cards carry no rating`() {
        val sheet = ExportSheets.build(oath, ExportDefaults.initial(oath, ExportFormat.PDF, LibFxNow, java.time.ZoneOffset.UTC))
        assertEquals("검은 서약 (가제)", sheet.sub)
        assertEquals("읽기용", ExportDefaults.formatCard(ExportFormat.PDF).second)
        assertEquals("옮기기 · 보관 · 숨김 · 숨긴 항목 있으면 암호", ExportDefaults.formatCard(ExportFormat.PROJECT_FILE).second)
        ExportDefaults.FORMATS.forEach { assertFalse(ExportDefaults.formatCard(it).second.contains("R-18") || ExportDefaults.formatCard(it).second.contains("등급")) }
    }

    @Test
    fun `R-05 a stored R-18 still drives the export defaults (D34)`() {
        val options = ExportDefaults.initial(oath, ExportFormat.PDF, LibFxNow, java.time.ZoneOffset.UTC)
        assertTrue(options.fileName.startsWith("civilight-내보내기-"))
        assertTrue(ExportDefaults.pdfPasswordDefault(oath))
        assertTrue(ExportDefaults.encryptDefault(oath))
        assertFalse(ExportDefaults.pdfPasswordDefault(glass))
    }

    @Test
    fun `G-06 export formats follow 게임 제작 도구`() {
        assertEquals(listOf(ExportFormat.PDF, ExportFormat.PROJECT_FILE), ExportDefaults.exportFormats(withGameTools(glass, false)))
        assertEquals(ExportDefaults.FORMATS, ExportDefaults.exportFormats(withGameTools(glass, true)))
        // Absent reads off (every load path resolves it before the UI sees it).
        assertEquals(listOf(ExportFormat.PDF, ExportFormat.PROJECT_FILE), ExportDefaults.exportFormats(withGameTools(glass, null)))
        // A hidden choice falls back to the first visible one; a visible one stays.
        assertEquals(ExportFormat.PDF, ExportDefaults.visibleFormat(withGameTools(glass, false), ExportFormat.CSV))
        assertEquals(ExportFormat.PROJECT_FILE, ExportDefaults.visibleFormat(withGameTools(glass, false), ExportFormat.PROJECT_FILE))
        assertEquals(ExportFormat.ENGINE_JSON, ExportDefaults.visibleFormat(withGameTools(glass, true), ExportFormat.ENGINE_JSON))
    }

    @Test
    fun `E-01 the versions row shows only with a version, bound to includeVersions`() {
        val file = ExportDefaults.initial(glass, ExportFormat.PROJECT_FILE, LibFxNow, java.time.ZoneOffset.UTC)
        assertNull(ExportSheets.build(glass, file).row<ExportRow.IncludeVersions>())
        val unit = glass.story.units.first { it.level == 1 }.key
        val versioned = glass.copy(
            versions = mapOf(unit to EpisodeVersionsFile(unitKey = unit, versions = listOf(EpisodeVersion(key = Fixtures.keys(3)(), name = "11월 23일 · 초고", createdAt = LibFxNow)))),
        )
        assertTrue(ExportSheets.hasVersions(versioned))
        assertEquals(ExportRow.IncludeVersions(true), ExportSheets.build(versioned, file).row<ExportRow.IncludeVersions>())
        assertEquals(ExportRow.IncludeVersions(false), ExportSheets.build(versioned, file.copy(includeVersions = false)).row<ExportRow.IncludeVersions>())
        assertEquals("지난 판도 포함", ExportRow.IncludeVersions(true).label)
        // Reading formats never show it.
        assertNull(ExportSheets.build(versioned, ExportDefaults.initial(versioned, ExportFormat.PDF, LibFxNow, java.time.ZoneOffset.UTC)).row<ExportRow.IncludeVersions>())
    }

    @Test
    fun `E-01 versions of a trashed unit show the row only with 휴지통도 포함`() {
        val file = ExportDefaults.initial(glass, ExportFormat.PROJECT_FILE, LibFxNow, java.time.ZoneOffset.UTC)
        val unit = glass.story.units.first { it.level == 1 }.key
        val trashed = glass.copy(
            story = glass.story.copy(units = glass.story.units.map { if (it.key == unit) it.copy(deletedAt = LibFxNow) else it }),
            versions = mapOf(unit to EpisodeVersionsFile(unitKey = unit, versions = listOf(EpisodeVersion(key = Fixtures.keys(3)(), name = "11월 23일 · 초고", createdAt = LibFxNow)))),
        )
        assertFalse(ExportSheets.hasVersions(trashed, includeTrash = false))
        assertTrue(ExportSheets.hasVersions(trashed, includeTrash = true))
        assertNull("the file would carry no version", ExportSheets.build(trashed, file.copy(includeTrash = false)).row<ExportRow.IncludeVersions>())
        assertEquals(ExportRow.IncludeVersions(true), ExportSheets.build(trashed, file.copy(includeTrash = true)).row<ExportRow.IncludeVersions>())
    }

    private companion object {
        val LibFxNow: Long = Fixtures.at("2026-11-24T10:00:00Z")
    }
}
