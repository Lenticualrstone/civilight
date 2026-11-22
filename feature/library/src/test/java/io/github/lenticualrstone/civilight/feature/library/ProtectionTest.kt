package io.github.lenticualrstone.civilight.feature.library

import io.github.lenticualrstone.civilight.domain.library.rating.Protection
import io.github.lenticualrstone.civilight.domain.library.rating.RatedProject
import io.github.lenticualrstone.civilight.domain.library.rating.RatingDefaultsTable
import io.github.lenticualrstone.civilight.feature.library.common.NoBackupYet
import io.github.lenticualrstone.civilight.feature.library.projectsettings.ProjectSettingsState
import io.github.lenticualrstone.civilight.feature.library.protection.ProtectionActions
import io.github.lenticualrstone.civilight.feature.library.protection.ProtectionOutcome
import io.github.lenticualrstone.civilight.feature.library.protection.ProtectionPorts
import io.github.lenticualrstone.civilight.feature.library.rating.RatingSettingsState
import io.github.lenticualrstone.civilight.feature.library.rating.RatingSheetModel
import io.github.lenticualrstone.civilight.model.Key
import io.github.lenticualrstone.civilight.model.app.RatingDefaults
import io.github.lenticualrstone.civilight.model.open
import io.github.lenticualrstone.civilight.model.project.Project
import io.github.lenticualrstone.civilight.model.project.Protections
import io.github.lenticualrstone.civilight.model.project.Rating
import io.github.lenticualrstone.civilight.model.recorder.RecorderLibrary
import io.github.lenticualrstone.civilight.ui.project.EditResult
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * One write path for protections ([ProtectionActions], LIB-24, LIB-25, LIB-31) and the rating screens'
 * pure state (LIB-23 … LIB-25), spec §4.1 R19 cases.
 */
class ProtectionTest {
    private val all = Protections(hidden = true, captureBlock = true, nameMask = true, recorderLinkMask = true)

    private class FakePorts(projects: Map<Key, Project>, var authResult: Boolean = true, val readOnly: Set<Key> = emptySet()) : ProtectionPorts {
        val projects = projects.toMutableMap()
        val labels = mutableListOf<String>()
        val forgotten = mutableListOf<Key>()
        var prompts = 0

        override suspend fun write(projectKey: Key, label: String, transform: (Project) -> Project): EditResult {
            if (projectKey in readOnly) return EditResult.READ_ONLY
            labels += label
            projects[projectKey] = transform(projects.getValue(projectKey))
            return EditResult.APPLIED
        }

        override suspend fun authenticate(): Boolean {
            prompts++
            return authResult
        }

        override suspend fun forgetRecents(projectKey: Key) {
            forgotten += projectKey
        }
    }

    private val ashen = LibFx.ashen().project
    private val oath = LibFx.oath().project.copy(protections = all)

    private fun ports(authResult: Boolean = true) = FakePorts(mapOf(ashen.key to ashen, oath.key to oath), authResult)

    private fun sheet(project: Project, to: Rating) =
        RatingSheetModel.build(LibFx.ashen().copy(project = project), to, RatingDefaults(), NoBackupYet, RecorderLibrary())!!

    @Test
    fun `R19-02 raising 잿빛 to R-18 with every row checked`() = runTest {
        val p = ports()
        val change = sheet(ashen, Rating.R18).apply()
        assertFalse(change.requiresAuth)
        assertEquals(ProtectionOutcome.APPLIED, ProtectionActions(p).changeRating(ashen.key, change))
        val after = p.projects.getValue(ashen.key)
        assertEquals(Rating.R18.open(), after.rating)
        assertEquals(all, after.protections)
        assertEquals(0, p.prompts)
        assertEquals("joining the hidden group drops its recent items", listOf(ashen.key), p.forgotten)
        assertEquals(listOf("수위 등급"), p.labels)
    }

    @Test
    fun `R19-05 lowering with nothing unchecked keeps every protection`() = runTest {
        val p = ports()
        val change = sheet(oath, Rating.R15).apply()
        assertEquals(ProtectionOutcome.APPLIED, ProtectionActions(p).changeRating(oath.key, change))
        assertEquals(Rating.R15.open(), p.projects.getValue(oath.key).rating)
        assertEquals(all, p.projects.getValue(oath.key).protections)
        assertEquals(0, p.prompts)
    }

    @Test
    fun `R19-06 unchecking hiding asks first, cancel changes nothing`() = runTest {
        val model = sheet(oath, Rating.R15)
        val change = model.apply(model.initialChecks - Protection.HIDDEN)
        val cancelled = ports(authResult = false)
        assertEquals(ProtectionOutcome.CANCELLED, ProtectionActions(cancelled).changeRating(oath.key, change))
        assertEquals(oath, cancelled.projects.getValue(oath.key))
        assertEquals(1, cancelled.prompts)
        assertTrue(cancelled.labels.isEmpty())
        assertFalse(RatingSheetModel.closesAfter(ProtectionOutcome.CANCELLED))

        val ok = ports(authResult = true)
        assertEquals(ProtectionOutcome.APPLIED, ProtectionActions(ok).changeRating(oath.key, change))
        assertEquals(all.copy(hidden = false), ok.projects.getValue(oath.key).protections)
        assertEquals(Rating.R15.open(), ok.projects.getValue(oath.key).rating)
        assertTrue(RatingSheetModel.closesAfter(ProtectionOutcome.APPLIED))
    }

    @Test
    fun `R19-09 the same rating opens no sheet`() {
        assertNull(RatingSheetModel.build(LibFx.ashen(), Rating.R15, RatingDefaults(), NoBackupYet, RecorderLibrary()))
    }

    @Test
    fun `LIB-25 one switch - hiding off asks, on never does, the rating never changes`() = runTest {
        val p = ports(authResult = false)
        val actions = ProtectionActions(p)
        assertEquals(ProtectionOutcome.CANCELLED, actions.switch(oath.key, oath.protections, Protection.HIDDEN, on = false))
        assertTrue(p.projects.getValue(oath.key).protections.hidden)
        assertEquals(ProtectionOutcome.APPLIED, actions.switch(ashen.key, ashen.protections, Protection.CAPTURE_BLOCK, on = true))
        assertEquals(1, p.prompts)
        val after = p.projects.getValue(ashen.key)
        assertTrue(after.protections.captureBlock)
        assertEquals(ashen.rating, after.rating)
        assertEquals(listOf("화면 캡처 가리기"), p.labels)
        assertEquals(ProtectionOutcome.APPLIED, actions.switch(ashen.key, after.protections, Protection.HIDDEN, on = true))
        assertEquals(1, p.prompts)
        assertEquals(listOf(ashen.key), p.forgotten)
    }

    @Test
    fun `LIB-31 hide from the menu is immediate, 숨김 풀기 in the open group needs no second prompt`() = runTest {
        val p = ports(authResult = false)
        val actions = ProtectionActions(p)
        assertEquals(ProtectionOutcome.APPLIED, actions.hide(ashen.key))
        assertTrue(p.projects.getValue(ashen.key).protections.hidden)
        assertEquals(ProtectionOutcome.APPLIED, actions.unhideInOpenGroup(oath.key))
        assertFalse(p.projects.getValue(oath.key).protections.hidden)
        assertEquals(0, p.prompts)
        assertEquals(listOf("프로젝트 숨기기", "숨김 풀기"), p.labels)
    }

    @Test
    fun `the read-only example reports READ_ONLY and is not written`() = runTest {
        val p = FakePorts(mapOf(ashen.key to ashen), readOnly = setOf(ashen.key))
        assertEquals(ProtectionOutcome.READ_ONLY, ProtectionActions(p).hide(ashen.key))
        assertEquals(ashen, p.projects.getValue(ashen.key))
        assertTrue(p.forgotten.isEmpty())
    }

    @Test
    fun `R19-10 a changed default leaves projects alone and offers 같이 바꾸기`() = runTest {
        val r18a = RatedProject("A", "검은 서약 (가제)", Rating.R18, all.copy(hidden = false))
        val r18b = RatedProject("B", "붉은 문", Rating.R18, all.copy(hidden = false))
        val changed = RatingDefaultsTable.change(RatingDefaults(), Rating.R18, Protection.NAME_MASK, false)
        val ui = RatingSettingsState.build(changed, listOf(r18a, r18b), hiddenGroupOpen = false)
        val r18 = ui.groups.first { it.rating == Rating.R18 }
        assertEquals("이미 있는 R-18 프로젝트 2 · 같이 바꾸기", r18.tag)
        assertFalse(r18.switches.single { it.protection == Protection.NAME_MASK }.on)
        assertNull(ui.groups.first { it.rating == Rating.R15 }.tag)
        assertTrue("ALL is 라벨만", ui.groups.first { it.rating == Rating.ALL }.switches.isEmpty())
        assertEquals(listOf(Protection.CAPTURE_BLOCK), ui.groups.first { it.rating == Rating.R15 }.switches.map { it.protection })

        val rows = RatingSettingsState.applyRows(Rating.R18, changed, listOf(r18a, r18b), hiddenGroupOpen = false)
        assertEquals(listOf("이름 가리기", "이름 가리기"), rows.map { it.changes })
        assertFalse(rows.first().after.nameMask)

        val p = FakePorts(mapOf("A" to oath.copy(key = "A", protections = r18a.protections), "B" to oath.copy(key = "B", protections = r18b.protections)))
        assertEquals(ProtectionOutcome.APPLIED, ProtectionActions(p).applyDefaultsAll(rows.map { Triple(it.project.key, it.project.protections, it.after) }))
        assertFalse(p.projects.getValue("A").protections.nameMask)
        assertEquals(0, p.prompts)
    }

    @Test
    fun `같이 바꾸기 turning hiding off asks once for all, cancel changes none`() = runTest {
        val hiddenA = RatedProject("A", "A", Rating.R18, all)
        val hiddenB = RatedProject("B", "B", Rating.R18, all)
        val changed = RatingDefaultsTable.change(RatingDefaults(), Rating.R18, Protection.HIDDEN, false)
        assertNull("hidden projects are listed only while the group is open", RatingSettingsState.build(changed, listOf(hiddenA, hiddenB), false).groups.first().tag)
        val rows = RatingSettingsState.applyRows(Rating.R18, changed, listOf(hiddenA, hiddenB), hiddenGroupOpen = true)
        val triples = rows.map { Triple(it.project.key, it.project.protections, it.after) }
        val start = mapOf("A" to oath.copy(key = "A"), "B" to oath.copy(key = "B"))

        val cancelled = FakePorts(start, authResult = false)
        assertEquals(ProtectionOutcome.CANCELLED, ProtectionActions(cancelled).applyDefaultsAll(triples))
        assertTrue(cancelled.projects.values.all { it.protections.hidden })

        val ok = FakePorts(start, authResult = true)
        assertEquals(ProtectionOutcome.APPLIED, ProtectionActions(ok).applyDefaultsAll(triples))
        assertEquals(1, ok.prompts)
        assertTrue(ok.projects.values.none { it.protections.hidden })
    }

    @Test
    fun `R19-14 and R19-15 프로젝트 설정 marks values that differ from the rating's default`() {
        val r15 = LibFx.ashen().let { it.copy(project = it.project.copy(protections = Protections(captureBlock = true))) }
        val rows = ProjectSettingsState.build(r15, readOnly = false, RatingDefaults()).protection.rows
        assertEquals("R-15 기본과 다름", rows.single { it.protection == Protection.CAPTURE_BLOCK }.differsMarker)
        assertTrue(rows.filter { it.protection != Protection.CAPTURE_BLOCK }.all { it.differsMarker == null })

        val r18 = LibFx.ashen().let { it.copy(project = it.project.copy(rating = Rating.R18.open(), protections = all)) }
        val settings = ProjectSettingsState.build(r18, readOnly = true, RatingDefaults())
        assertTrue(settings.protection.rows.all { it.differsMarker == null })
        assertEquals("R-18 ›", settings.protection.ratingRowValue)
        assertTrue(settings.readOnly)
    }

    @Test
    fun `the rating sheet closes once and leaves with a locked hidden project`() {
        var closes = 0
        val closer = RatingSheetModel.CloseOnce { closes++ }
        closer.close()
        closer.close()
        assertEquals(1, closes)
        assertTrue(closer.closed)
        assertTrue(RatingSheetModel.lockedAway(all, hiddenGroupOpen = false))
        assertFalse(RatingSheetModel.lockedAway(all, hiddenGroupOpen = true))
        assertFalse(RatingSheetModel.lockedAway(Protections(), hiddenGroupOpen = false))
    }
}
