package io.github.lenticualrstone.civilight.feature.recorder

import io.github.lenticualrstone.civilight.domain.recorder.ProjectSnapshot
import io.github.lenticualrstone.civilight.domain.recorder.RecorderEnv
import io.github.lenticualrstone.civilight.domain.recorder.link.Chips
import io.github.lenticualrstone.civilight.domain.recorder.link.EntryLinks
import io.github.lenticualrstone.civilight.domain.recorder.RecorderEditContext
import io.github.lenticualrstone.civilight.feature.recorder.capture.EntryRoutes
import io.github.lenticualrstone.civilight.feature.recorder.env.RecorderEnvs
import io.github.lenticualrstone.civilight.feature.recorder.link.LinkPickerRows
import io.github.lenticualrstone.civilight.model.open
import io.github.lenticualrstone.civilight.model.project.Rating
import io.github.lenticualrstone.civilight.model.recorder.EntrySurface
import io.github.lenticualrstone.civilight.model.recorder.RecorderSettings
import io.github.lenticualrstone.civilight.model.recorder.RecorderTargetKind
import io.github.lenticualrstone.civilight.model.sample.ExampleKeys
import io.github.lenticualrstone.civilight.model.sample.ExampleRecorder
import io.github.lenticualrstone.civilight.navigation.Recording
import io.github.lenticualrstone.civilight.ui.security.UnlockState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class LinksAndRoutesTest {
    private val env = Fx.env()
    private val ctx = RecorderEditContext(newKey = Fx.keys(), now = Fx.NOW, device = Fx.DEVICE, zone = Fx.ZONE)

    /** The chip row a capture opened by [route] starts with. */
    private fun chipsOf(route: Recording, settings: RecorderSettings = RecorderSettings()): List<String> {
        val links = EntryLinks.initialLinks("", EntryRoutes.entryOf(route), settings, ctx)
        return Chips.captureRow(links, emptyList(), env).map { it.text }
    }

    @Test
    fun LRC_T01_prefilledLinkByEntry() {
        assertEquals(
            listOf("연결 · 1-7 씬 3"),
            chipsOf(Recording(surface = "script", projectKey = Fx.P1, targetKind = "scene", targetKey = Fx.S, caretBlockKey = ExampleKeys.BLOCK_044)),
        )
        assertEquals(listOf("연결 · 하르트"), chipsOf(Recording(surface = "character", projectKey = Fx.P1, targetKind = "character", targetKey = ExampleKeys.HART)))
        val page = ExampleKeys.PAGE_LOWER_LIGHT
        assertEquals(listOf("연결 · ${Fx.p1().pages.getValue(page).name}"), chipsOf(Recording(surface = "page", projectKey = Fx.P1, targetKind = "page", targetKey = page)))
        for (surface in listOf("projectHome", "story", "timeline", "map")) {
            assertEquals(surface, listOf("연결 · 잿빛 새벽의 등불"), chipsOf(Recording(surface = surface, projectKey = Fx.P1)))
        }
        for (surface in listOf("library", "widget", "tile", "lockScreen", "coverWidget")) {
            assertEquals(surface, emptyList<String>(), chipsOf(Recording(surface = surface)))
        }
        assertEquals(listOf("연결 · 잿빛 새벽의 등불"), chipsOf(Recording(surface = "inbox", projectKey = Fx.P1)))
        assertEquals(emptyList<String>(), chipsOf(Recording(surface = "inbox")))
        // `보던 곳 연결` off: no pre-filled chip from any entry.
        val off = RecorderSettings(presetLink = false)
        assertEquals(emptyList<String>(), chipsOf(Recording(surface = "script", projectKey = Fx.P1, targetKind = "scene", targetKey = Fx.S), off))
        assertEquals(emptyList<String>(), chipsOf(Recording(surface = "projectHome", projectKey = Fx.P1), off))
    }

    @Test
    fun theRouteParserInfersTheSurfaceFromTheDefaultCommandBar() {
        val script = EntryRoutes.entryOf(Recording(projectKey = Fx.P1, targetKind = "scene", targetKey = Fx.S, caretBlockKey = ExampleKeys.BLOCK_044))
        assertEquals(EntrySurface.SCRIPT, script.surface)
        assertEquals(RecorderTargetKind.UNIT, script.targetKind)
        assertEquals(ExampleKeys.BLOCK_044, script.caretBlockKey)
        assertEquals(EntrySurface.CHARACTER, EntryRoutes.entryOf(Recording(projectKey = Fx.P1, targetKind = "character", targetKey = ExampleKeys.HART)).surface)
        assertEquals(EntrySurface.OTHER_IN_PROJECT, EntryRoutes.entryOf(Recording(projectKey = Fx.P1)).surface)
        assertEquals(EntrySurface.LIBRARY, EntryRoutes.entryOf(Recording()).surface)
        // Names in any case; a caret outside the script is dropped; unknown kinds pre-fill the project.
        assertEquals(EntrySurface.PROJECT_HOME, EntryRoutes.entryOf(Recording(surface = "PROJECT_HOME", projectKey = Fx.P1)).surface)
        assertNull(EntryRoutes.entryOf(Recording(surface = "projectHome", projectKey = Fx.P1, caretBlockKey = ExampleKeys.BLOCK_044)).caretBlockKey)
        val unknownKind = EntryRoutes.entryOf(Recording(surface = "world", projectKey = Fx.P1, targetKind = "event", targetKey = "X"))
        assertNull(unknownKind.targetKind)
        assertNull(unknownKind.targetKey)
        // A surface that needs a project but has none falls back to no pre-fill.
        assertEquals(EntrySurface.LIBRARY, EntryRoutes.entryOf(Recording(surface = "script")).surface)
        for (kind in listOf("scene", "episode", "chapter", "unit")) assertEquals(RecorderTargetKind.UNIT, EntryRoutes.targetKind(kind))
    }

    @Test
    fun theRouteRoundTripsAnEntry() {
        val route = Recording(surface = "script", projectKey = Fx.P1, targetKind = "scene", targetKey = Fx.S, caretBlockKey = ExampleKeys.BLOCK_044)
        val entry = EntryRoutes.entryOf(route)
        assertEquals(entry, EntryRoutes.entryOf(EntryRoutes.routeOf(entry)))
        val typed = EntryRoutes.routeOf(entry, startTyped = true)
        assertTrue(typed.startTyped)
        assertEquals("script", typed.surface)
        assertEquals(EntryRoutes.entryOf(Recording(surface = "inbox")), EntryRoutes.entryOf(EntryRoutes.routeOf(EntryRoutes.entryOf(Recording(surface = "inbox")))))
    }

    @Test
    fun LRC_T07_fixture_pickerRowsStartWithNamesInTheText() {
        val library = ExampleRecorder.library()
        val r1 = library.record(ExampleKeys.RECORDER_R1)!!
        val sections = LinkPickerRows.sections(env, "", library.linksOf(r1.key), io.github.lenticualrstone.civilight.domain.recorder.text.Runs.text(r1.runs))
        val inText = sections.first()
        assertEquals("글에 나온 이름", inText.title)
        // Order of first occurrence; 대정전 is a term in the example fixture (domain twin LRC_T07_fixture).
        assertEquals(listOf("하르트", "대정전", "세라"), inText.rows.map { it.title })
        assertEquals("잿빛 새벽의 등불 · 인물", inText.rows[0].sub)
        assertEquals("잿빛 새벽의 등불 · 용어", inText.rows[1].sub)
        assertTrue("하르트 is linked already", inText.rows[0].linked)
        assertFalse(inText.rows[2].linked)
        val rest = sections[1]
        assertNull(rest.title)
        val project = rest.rows.first { it.candidate.target.kind == RecorderTargetKind.PROJECT }
        assertEquals("잿빛 새벽의 등불", project.title)
        assertEquals("프로젝트", project.sub)
        val scene = rest.rows.first { it.candidate.target.key == Fx.S }
        assertEquals("1-7 씬 3", scene.title)
        assertEquals("잿빛 새벽의 등불", scene.sub)
    }

    @Test
    fun LRC_22_maskedProjectRowsPrintTheRating() {
        val p1 = Fx.p1()
        val masked = p1.copy(
            project = p1.project.copy(rating = Rating.R18.open(), protections = p1.project.protections.copy(recorderLinkMask = true)),
        )
        val rows = LinkPickerRows.sections(Fx.env(masked), "잿빛", emptyList(), null).flatMap { it.rows }
        assertEquals("이름 가림", rows.first { it.candidate.target.kind == RecorderTargetKind.PROJECT }.title)
    }

    @Test
    fun theEnvListsLiveProjectsByLastEditThenTrashedOnes() {
        val p1 = Fx.p1()
        val older = p1.copy(project = p1.project.copy(key = "01JZ000000PRJT000000000002", name = "유리 정원"))
        val gone = p1.copy(project = p1.project.copy(key = "01JZ000000PRJT000000000003", name = "버린 것"))
        val entries = listOf(
            Fx.entry(older, updatedAt = 10, lastOpenedAt = 50),
            Fx.entry(gone, updatedAt = 30, deletedAt = 40),
            Fx.entry(p1, updatedAt = 20, lastOpenedAt = 5),
        )
        val unlock = UnlockState(hiddenGroupUnlockedAt = 7, recordsUnlockedAt = 9, itemsUnlockedAt = mapOf(Fx.P1 to 8L))
        val built: RecorderEnv = RecorderEnvs.build(entries, listOf(p1, older, gone).associateBy { it.key }, unlock, Fx.ZONE)
        assertEquals(listOf(Fx.P1, older.key, gone.key), built.projects.map(ProjectSnapshot::key))
        assertEquals(older.key, built.recentProjectKey)
        assertTrue(built.recordsOpen)
        assertTrue(built.hiddenGroupOpen)
        assertTrue(built.itemsOpen(Fx.P1))
        assertTrue(built.project(gone.key)!!.trashed)
        // A row whose document could not be read is left out.
        assertEquals(2, RecorderEnvs.build(entries, mapOf(p1.key to p1, older.key to older), UnlockState(), Fx.ZONE).projects.size)
    }
}
