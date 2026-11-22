package io.github.lenticualrstone.civilight.domain.library

import io.github.lenticualrstone.civilight.domain.library.masking.HiddenAccess
import io.github.lenticualrstone.civilight.domain.library.masking.ProjectProfile
import io.github.lenticualrstone.civilight.domain.library.masking.ProjectSurfaces
import io.github.lenticualrstone.civilight.domain.library.masking.RecentTarget
import io.github.lenticualrstone.civilight.domain.library.masking.Recents
import io.github.lenticualrstone.civilight.domain.library.newproject.StartBlock
import io.github.lenticualrstone.civilight.domain.library.rating.NewProjectProtections
import io.github.lenticualrstone.civilight.domain.library.rating.ProjectProtectionSettings
import io.github.lenticualrstone.civilight.domain.library.rating.Protection
import io.github.lenticualrstone.civilight.domain.library.rating.ProtectionSwitch
import io.github.lenticualrstone.civilight.domain.library.rating.R18DefaultsBlock
import io.github.lenticualrstone.civilight.domain.library.rating.RatedProject
import io.github.lenticualrstone.civilight.domain.library.rating.RatingChangeOutcome
import io.github.lenticualrstone.civilight.domain.library.rating.RatingDefaultsChanges
import io.github.lenticualrstone.civilight.domain.library.rating.RatingDefaultsTable
import io.github.lenticualrstone.civilight.domain.library.rating.RatingDirection
import io.github.lenticualrstone.civilight.domain.library.rating.RatingSheets
import io.github.lenticualrstone.civilight.model.app.RatingDefaults
import io.github.lenticualrstone.civilight.model.app.RecentItem
import io.github.lenticualrstone.civilight.model.app.RecentKind
import io.github.lenticualrstone.civilight.model.project.ProjectFormat
import io.github.lenticualrstone.civilight.model.project.Protections
import io.github.lenticualrstone.civilight.model.project.Rating
import io.github.lenticualrstone.civilight.model.recorder.RecorderLibrary
import io.github.lenticualrstone.civilight.model.sample.ExampleKeys
import io.github.lenticualrstone.civilight.model.sample.ExampleProject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** §4.1 rating change sheet (R19-01 … R19-15), LIB-23…25. */
class RatingSheetTest {
    private val defaults = RatingDefaults()
    private val allOn = Protections(hidden = true, captureBlock = true, nameMask = true, recorderLinkMask = true)

    /** 잿빛 with only the record ‘하르트가 등불을 싫어하는 이유’ (R1) linked: the Given's one linked record. */
    private fun oneLinkedRecord(): RecorderLibrary {
        val lib = Fixtures.recorder()
        return RecorderLibrary(
            lib.records.filter { it.key == ExampleKeys.RECORDER_R1 },
            lib.links.filter { it.recordKey == ExampleKeys.RECORDER_R1 },
            lib.placements.filter { it.recordKey == ExampleKeys.RECORDER_R1 },
        )
    }

    private fun r1901() = RatingSheets.build(
        ExampleProject.NAME, Rating.R15, Rating.R18, Protections(), defaults,
        recoverySet = false, unencryptedBackups = 3,
        linkedRecords = oneLinkedRecord().recordsLinkedTo(ExampleKeys.PROJECT).size,
    )!!

    @Test
    fun `R19-01 raising 잿빛 to R-18 pre-checks the R-18 defaults`() {
        val sheet = r1901()
        assertEquals(RatingDirection.RAISE, sheet.direction)
        assertEquals(Protection.entries, sheet.rows.map { it.protection })
        assertEquals(Protection.entries.toSet(), sheet.initialChecks)
        assertTrue(sheet.rows.all { it.rightLabel == null })
        assertEquals("연결에 등급만 · 기록 1 바로 적용", sheet.rows.single { it.protection == Protection.RECORDER_LINK_MASK }.sub)
        assertEquals(listOf("복구 암호 없음", "이전 백업 3 · 암호 없음"), sheet.extraRows().map { it.title })
        assertEquals("바꾸면 ‘숨긴 프로젝트’ 묶음으로 · 지금은 열린 채", sheet.note())
        assertEquals("체크 · R-18 기본값 · 설정 › 수위 등급", sheet.smallLine)
        assertEquals("잿빛 새벽의 등불 · 수위 등급", sheet.header)
        assertEquals("R-15 → R-18", sheet.title)
        assertEquals(listOf("취소", "바꾸기"), sheet.buttons)
    }

    @Test
    fun `R19-02 applying with all checked hides 잿빛 but keeps it open until re-lock`() {
        val change = r1901().apply()
        assertFalse(change.requiresAuth)
        val outcome = change.complete() as RatingChangeOutcome.Applied
        assertEquals(Rating.R18, outcome.rating)
        assertEquals(allOn, outcome.protections)
        assertTrue(outcome.keepOpenUntilRelock)

        val doc = Fixtures.withProtections(Fixtures.example(), outcome.rating, outcome.protections)
        val access = HiddenAccess.LOCKED.keepOpen(doc.key)
        assertFalse(access.groupOpen)
        assertTrue(access.isProjectOpen(doc.key))
        val profile = ProjectProfile.of(doc)
        assertEquals("R-18", ProjectSurfaces.recorderLinkLabel(profile, access))
        assertTrue(oneLinkedRecord().records.single().runs.joinToString("") { it.shownText }.startsWith("하르트가 등불을 싫어하는 이유"))

        val recent = RecentItem(Fixtures.keys()(), doc.key, RecentKind.SCENE, ExampleKeys.SCENE_1_7_3, Fixtures.NOW)
        val target = { _: RecentItem -> RecentTarget("꺼진 등불", hidden = false, trashed = false) }
        val before = ProjectProfile.of(Fixtures.example())
        assertEquals(1, Recents.cards(listOf(recent), mapOf(doc.key to before), target).size)
        assertEquals(0, Recents.cards(listOf(recent), mapOf(doc.key to profile), target).size)
    }

    @Test
    fun `R19-03 no extra rows with a recovery password and no unencrypted backups`() {
        val sheet = RatingSheets.build(ExampleProject.NAME, Rating.R15, Rating.R18, Protections(), defaults, true, 0, 1)!!
        assertEquals(emptyList<Any>(), sheet.extraRows())
    }

    private fun r1904() = RatingSheets.build("검은 서약 (가제)", Rating.R18, Rating.R15, allOn, defaults, true, 0, 0)!!

    @Test
    fun `R19-04 lowering 검은 서약 keeps every protection checked`() {
        val sheet = r1904()
        assertEquals(RatingDirection.LOWER, sheet.direction)
        assertEquals(Protection.entries.toSet(), sheet.initialChecks)
        assertTrue(sheet.rows.all { it.rightLabel == "R-15 기본 · 끔" })
        assertEquals("풀면 바꿀 때 지문 확인", sheet.rows.first { it.protection == Protection.HIDDEN }.sub)
        assertEquals("내려도 저절로 풀리는 것 없음", sheet.note())
        assertEquals("체크 · 지금 켜진 보호 · 체크를 풀어야 꺼짐", sheet.smallLine)
    }

    @Test
    fun `R19-05 lowering without unchecking changes only the rating`() {
        val change = r1904().apply()
        assertFalse(change.requiresAuth)
        assertEquals(RatingChangeOutcome.Applied(Rating.R15, allOn, keepOpenUntilRelock = false), change.complete())
    }

    @Test
    fun `R19-06 unchecking hide needs authentication and cancel changes nothing`() {
        val change = r1904().apply(Protection.entries.toSet() - Protection.HIDDEN)
        assertTrue(change.requiresAuth)
        val ok = change.complete(authenticated = true) as RatingChangeOutcome.Applied
        assertEquals(allOn.copy(hidden = false), ok.protections)
        assertEquals(Rating.R15, ok.rating)
        assertEquals(RatingChangeOutcome.Unchanged(Rating.R18, allOn), change.complete(authenticated = false))
    }

    @Test
    fun `R19-07 every sheet is built fresh from current values`() {
        val current = Protections(nameMask = true)
        val first = RatingSheets.build(ExampleProject.NAME, Rating.R15, Rating.R18, current, defaults, false, 0, 1)!!
        val second = RatingSheets.build(ExampleProject.NAME, Rating.R15, Rating.R18, current, defaults, false, 0, 1)!!
        assertEquals(Protection.entries.toSet(), first.initialChecks)
        assertEquals(first.initialChecks, second.initialChecks)
        assertEquals(first.rows, second.rows)
    }

    @Test
    fun `R19-08 raising keeps a protection that is on even when the new default is off`() {
        val sheet = RatingSheets.build("유리 정원", Rating.ALL, Rating.R15, Protections(captureBlock = true), defaults, false, 0, 0)!!
        assertEquals(setOf(Protection.CAPTURE_BLOCK), sheet.initialChecks)
    }

    @Test
    fun `R19-09 same rating gives no sheet`() {
        for (r in Rating.entries) assertNull(RatingSheets.build("x", r, r, Protections(), defaults, false, 0, 0))
    }

    @Test
    fun `R19-10 a changed default affects only new projects`() {
        val changed = RatingDefaultsTable.change(defaults, Rating.R18, Protection.NAME_MASK, on = false)
        val r18 = RatingDefaultsTable.defaultsFor(Rating.R18, defaults).copy(hidden = false)
        val existing = listOf(
            RatedProject(ExampleKeys.PROJECT, ExampleProject.NAME, Rating.R18, r18),
            RatedProject(Fixtures.keys(3)(), "붉은 장부", Rating.R18, r18),
        )
        val listed = RatingDefaultsChanges.existing(Rating.R18, existing, hiddenGroupOpen = false)
        assertTrue(listed.all { it.protections.nameMask })
        assertEquals("이미 있는 R-18 프로젝트 2 · 같이 바꾸기", RatingDefaultsChanges.tag(Rating.R18, changed, listed))
        assertFalse(NewProjectProtections.of(Rating.R18, changed, null).protections.nameMask)
        // Applying to the checked ones is an explicit step.
        val applied = RatingDefaultsChanges.applyToChecked(Rating.R18, Protection.NAME_MASK, changed, listed.take(1))
        assertFalse(applied.updated.single().protections.nameMask)
    }

    @Test
    fun `R19-11 R-18 with step-4 hide off`() {
        val p = NewProjectProtections.of(Rating.R18, defaults, step4Hide = false)
        assertEquals(Protections(hidden = false, captureBlock = true, nameMask = true, recorderLinkMask = true), p.protections)
        val doc = Fixtures.projectWith("검은 서약 (가제)", Rating.R18, p.protections, ProjectFormat.SETTING_ONLY)
        assertTrue(StartBlock.of(doc, RecorderLibrary(), p).summary.contains("숨김 · 끔"))
    }

    @Test
    fun `R19-12 R-15 project gets no protection`() {
        assertEquals(Protections(), NewProjectProtections.of(Rating.R15, defaults, null).protections)
    }

    @Test
    fun `R19-13 R-18 without the hide default`() {
        val p = NewProjectProtections.of(Rating.R18, defaults.copy(r18Hide = false), null)
        assertFalse(p.protections.hidden)
        assertFalse(p.step4ToggleInitial)
        assertNull(p.summaryMarker)
    }

    @Test
    fun `R19-14 a value that differs from the rating default is marked`() {
        val settings = ProjectProtectionSettings.of(Rating.R15, Protections(captureBlock = true), defaults)
        val capture = settings.rows.single { it.protection == Protection.CAPTURE_BLOCK }
        assertTrue(capture.fullSub.endsWith("R-15 기본과 다름"))
        assertTrue(settings.rows.filter { it.protection != Protection.CAPTURE_BLOCK }.all { it.differsMarker == null })
        assertEquals("R-15 ›", settings.ratingRowValue)
        // A single switch never changes the rating.
        assertEquals(Protections(captureBlock = true), ProtectionSwitch(Protection.CAPTURE_BLOCK, Protections(), true).after)
    }

    @Test
    fun `R19-15 defaults equal to the rating show no marker`() {
        assertTrue(ProjectProtectionSettings.of(Rating.R18, allOn, defaults).rows.all { it.differsMarker == null })
    }

    @Test
    fun `turning hiding off in project settings needs authentication, on does not`() {
        assertTrue(ProtectionSwitch(Protection.HIDDEN, allOn, false).requiresAuth)
        assertFalse(ProtectionSwitch(Protection.HIDDEN, Protections(), true).requiresAuth)
    }

    @Test
    fun `fixed defaults cannot be changed`() {
        assertFalse(RatingDefaultsTable.isEditable(Rating.R15, Protection.HIDDEN))
        assertFalse(RatingDefaultsTable.isEditable(Rating.ALL, Protection.CAPTURE_BLOCK))
        assertTrue(RatingDefaultsTable.isEditable(Rating.R15, Protection.CAPTURE_BLOCK))
    }

    @Test
    fun `R-18 step-1 block lists the on defaults and the backup warning`() {
        val block = R18DefaultsBlock.of(defaults, recoverySet = false)
        assertEquals("R-18 기본 동작 · 설정에서 바꾸기", block.header)
        assertEquals(listOf("프로젝트 숨기기 켬", "화면 캡처 · 최근 앱 미리 보기 가림", "위젯 · 알림에 이름 대신 ‘R-18 프로젝트’"), block.lines.take(3))
        assertEquals("자동 백업에서 빠짐", block.warning)
        assertNull(R18DefaultsBlock.of(defaults, recoverySet = true).warning)
    }

    @Test
    fun `lowering to ALL labels the ALL default`() {
        val sheet = RatingSheets.build("x", Rating.R15, Rating.ALL, Protections(captureBlock = true), defaults, false, 0, 0)!!
        assertTrue(sheet.rows.all { it.rightLabel == "ALL 기본 · 끔" })
        assertEquals(setOf(Protection.CAPTURE_BLOCK), sheet.initialChecks)
    }
}
