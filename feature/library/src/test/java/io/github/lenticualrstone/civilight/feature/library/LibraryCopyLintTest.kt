package io.github.lenticualrstone.civilight.feature.library

import io.github.lenticualrstone.civilight.designsystem.text.CopyLint
import io.github.lenticualrstone.civilight.designsystem.theme.MotionLint
import io.github.lenticualrstone.civilight.domain.library.onboarding.ModuleTours
import io.github.lenticualrstone.civilight.domain.library.onboarding.TourModule
import io.github.lenticualrstone.civilight.feature.library.backup.BackupCopy
import io.github.lenticualrstone.civilight.feature.library.backup.RecoveryCopy
import io.github.lenticualrstone.civilight.feature.library.backup.RestoreCopy
import io.github.lenticualrstone.civilight.domain.library.backup.BackupFailure
import io.github.lenticualrstone.civilight.domain.library.export.ExportRow
import io.github.lenticualrstone.civilight.domain.library.export.ExportSheets
import io.github.lenticualrstone.civilight.domain.library.newproject.StartBlock
import io.github.lenticualrstone.civilight.domain.library.rating.R18DefaultsBlock
import io.github.lenticualrstone.civilight.feature.library.export.ExportCopy
import io.github.lenticualrstone.civilight.feature.library.export.ExportGalleryLine
import io.github.lenticualrstone.civilight.feature.library.notices.LibraryNotices
import io.github.lenticualrstone.civilight.feature.library.help.HelpCopy
import io.github.lenticualrstone.civilight.feature.library.onboarding.OnboardingCopy
import io.github.lenticualrstone.civilight.feature.library.newproject.NewProjectCopy
import io.github.lenticualrstone.civilight.feature.library.newproject.NewProjectRules
import io.github.lenticualrstone.civilight.feature.library.openfile.OpenFileCopy
import io.github.lenticualrstone.civilight.feature.library.projectsettings.ProjectSettingsCopy
import io.github.lenticualrstone.civilight.feature.library.protection.ProtectionCopy
import io.github.lenticualrstone.civilight.feature.library.rating.RatingCopy
import io.github.lenticualrstone.civilight.feature.library.rating.RatingSettingsState
import io.github.lenticualrstone.civilight.feature.library.settings.LayoutSettingsCopy
import io.github.lenticualrstone.civilight.feature.library.settings.MoreSettingsCopy
import io.github.lenticualrstone.civilight.feature.library.settings.MotionSettingsCopy
import io.github.lenticualrstone.civilight.feature.library.settings.SettingsCopy
import io.github.lenticualrstone.civilight.feature.library.shelf.ShelfCopy
import io.github.lenticualrstone.civilight.feature.library.trash.TrashScreenCopy
import io.github.lenticualrstone.civilight.model.project.Rating
import io.github.lenticualrstone.civilight.navigation.NewProjectStep
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.lang.reflect.Modifier

/** DECISIONS §3: every value of this module's copy objects passes the design-system copy lint. */
class LibraryCopyLintTest {
    private val objects: List<Any> = listOf(ShelfCopy, NewProjectCopy, RatingCopy, ProtectionCopy, ProjectSettingsCopy, TrashScreenCopy, ExportCopy, SettingsCopy, OpenFileCopy, OnboardingCopy, HelpCopy, BackupCopy, RecoveryCopy, RestoreCopy, LayoutSettingsCopy, MotionSettingsCopy, MoreSettingsCopy, io.github.lenticualrstone.civilight.domain.library.newproject.CoverSummary)

    /** `Object.FIELD` → string value, for every constant of every copy object. */
    private fun values(): Map<String, String> = objects.flatMap { obj ->
        obj::class.java.declaredFields
            .filter { Modifier.isStatic(it.modifiers) && it.type == String::class.java }
            .map { field ->
                field.isAccessible = true
                "${obj::class.java.simpleName}.${field.name}" to field.get(null) as String
            }
    }.toMap()

    /** Copy built from parts, with sample values from the boards. */
    private fun formatted(): List<String> = buildList {
        NewProjectStep.entries.forEach {
            add(NewProjectRules.smallLine(it))
            add(NewProjectRules.title(it))
        }
        Rating.entries.forEach { add(RatingSettingsState.applyTitle(it, 2)) }
        add(TrashScreenCopy.restored("유리 정원"))
        // U-08: the cover words (LIB-81 `지금 등불`, LIB-82, CV-32).
        io.github.lenticualrstone.civilight.model.asset.Presets.NAMES.forEach { add("${io.github.lenticualrstone.civilight.domain.library.newproject.CoverSummary.NOW} $it") }
        (1..3).forEach { add(OnboardingCopy.setupLine(it)) }
        add(OnboardingCopy.exploreLine(OnboardingCopy.EXPLORE_SCRIPT, "1-7 씬 3"))
        add(HelpCopy.stepsLine(3, seen = true))
        add(HelpCopy.stepsLine(4, seen = false))
        // LIB-19 tour copy (domain:library: drawn steps and the 2026-11-23 words), and the card words.
        TourModule.entries.forEach { m ->
            m.label?.let(::add)
            add(m.rowTitle)
            ModuleTours.steps(m).forEach { add(it.title); add(it.sub) }
        }
        addAll(listOf(ModuleTours.SKIP, ModuleTours.PREVIOUS, ModuleTours.NEXT, ModuleTours.FINISH))
        // 0.1.0 motion wave: copy that lives outside the copy objects (domain:library constants, notices, the export gallery row).
        addAll(listOf(ExportGalleryLine(12, 148L * 1024 * 1024).text, ExportGalleryLine(8, 3L * 1024 * 1024).label))
        addAll(listOf(ExportSheets.CREATE_CSV, ExportSheets.CREATE_JSON, ExportSheets.CREATE_FILE, ExportRow.PASSWORD_WARNING))
        io.github.lenticualrstone.civilight.model.app.RelockPolicy.entries.forEach { add(io.github.lenticualrstone.civilight.domain.library.lock.Relock.openGroupNote(it)) }
        BackupFailure.entries.forEach { add(it.label) }
        add(R18DefaultsBlock.RECORDER_LINE)
        add(StartBlock.PROTAGONIST_SUB)
        add(LibraryNotices.RESTORE_DONE)
        add(LibraryNotices.EXPORT_DONE)
        // 0.2.0 copy that lives in domain:library (LIB-92 masked name and name-mask sub, LIB-97 versions row, format subs).
        add(io.github.lenticualrstone.civilight.domain.library.rating.RatingLabels.TODO_COPY_LIB_MASKED_NAME)
        add(io.github.lenticualrstone.civilight.domain.library.rating.ProjectProtectionSettings.TODO_COPY_LIB_NAME_MASK_SUB)
        add(io.github.lenticualrstone.civilight.domain.library.rating.ProjectProtectionSettings.SECTION_020)
        add(ExportRow.TODO_COPY_LIB_EXPORT_VERSIONS)
        io.github.lenticualrstone.civilight.domain.library.export.ExportDefaults.FORMATS.forEach {
            val (title, sub) = io.github.lenticualrstone.civilight.domain.library.export.ExportDefaults.formatCard(it)
            add(title); add(sub)
        }
        // Batch 5: 복구 암호 strength line and the 되살리는 중 lines.
        add(io.github.lenticualrstone.civilight.feature.library.backup.RecoveryPasswordState.strength("등불지기의새벽1잿빛하층구역".toCharArray())!!.line)
        add("${RestoreCopy.DRIVE} · 11월 20일 ${RestoreCopy.BACKUP}")
        add("${RestoreCopy.ABOUT} 40${RestoreCopy.SECONDS}")
        // Batch 6: the LIB-62 storage line and the LIB-61 banner (domain:library `BackupFailureBanner`).
        add(io.github.lenticualrstone.civilight.feature.library.backup.StorageFacts(1_288_490_188L, 356_515_840L).line)
        io.github.lenticualrstone.civilight.domain.library.backup.BackupFailureBanner.of(
            io.github.lenticualrstone.civilight.model.app.BackupState(lastSuccessAt = LibFx.NOW - 4 * 86_400_000L, failureStreakStartedAt = LibFx.NOW - 2 * 86_400_000L, lastFailureReason = "loginExpired"),
            LibFx.NOW,
            LibFx.ZONE,
        )!!.let { addAll(listOf(it.title, it.sub, it.driveRow!!) + it.buttons) }
    }

    @Test
    fun `every copy value passes the lint`() {
        val found = values()
        assertTrue("every copy object exposes its constants", objects.all { obj -> found.keys.any { it.startsWith(obj::class.java.simpleName + ".") } })
        val failures = found.mapNotNull { (name, text) ->
            val issues = CopyLint.lint(text)
            if (issues.isEmpty()) null else "$name = $text: $issues"
        } + formatted().mapNotNull { text -> CopyLint.lint(text).takeIf { it.isNotEmpty() }?.let { "$text: $it" } }
        assertEquals(emptyList<String>(), failures)
    }

    @Test
    fun `button labels are verbs, never a lone confirm`() {
        val buttons = listOf(
            NewProjectCopy.CANCEL, NewProjectCopy.NEXT, NewProjectCopy.PREVIOUS, NewProjectCopy.CREATE,
            ShelfCopy.RENAME_CONFIRM, ShelfCopy.CANCEL, ShelfCopy.RELOCK, RatingCopy.CANCEL, RatingCopy.APPLY_TOGETHER,
            TrashScreenCopy.RESTORE, TrashScreenCopy.PURGE, TrashScreenCopy.EMPTY_TRASH, TrashScreenCopy.CANCEL,
            OnboardingCopy.START, OnboardingCopy.RESTORE_FROM_BACKUP, OnboardingCopy.NEXT, OnboardingCopy.LATER, OnboardingCopy.PREVIOUS,
            OnboardingCopy.CONNECT, OnboardingCopy.SEE_EXAMPLE, OnboardingCopy.FINISH_REPLAY, OnboardingCopy.FINISH_ONBOARDING,
            HelpCopy.REPLAY, HelpCopy.RESET_ALL,
            RecoveryCopy.SET, RestoreCopy.CONNECT, RestoreCopy.CANCEL, RestoreCopy.OPEN, ExportCopy.CANCEL,
        )
        val failures = buttons.filter { CopyLint.lint(it, isButton = true).isNotEmpty() }
        assertEquals(emptyList<String>(), failures)
    }

    /** QA 2026-11-23 #1, #4: no placeholder reaches a screen, a toast or a content description. */
    @Test
    fun `no placeholder copy is left in this module`() {
        val left = values().filterValues { it.startsWith("TODO_COPY_") }.keys
        assertEquals(emptyList<String>(), left.toList())
        val sources = File("src/main").walkTopDown().filter { it.extension == "kt" }
            .filter { file -> file.readLines().any { line -> Regex("\"TODO_COPY_|\\bTODO_COPY_\\w+\\s*=").containsMatchIn(line) } }
            .map { it.path }.toList()
        assertEquals(emptyList<String>(), sources)
    }

    @Test
    fun `formatted copy reads as the boards`() {
        assertEquals("새 프로젝트 · 4/4", NewProjectRules.smallLine(NewProjectStep.START))
        assertEquals("시작 재료", NewProjectRules.title(NewProjectStep.START))
        assertEquals("이미 있는 R-18 프로젝트 2", RatingSettingsState.applyTitle(Rating.R18, 2))
        assertEquals("처음 설정 · 2/4", OnboardingCopy.setupLine(2))
        assertEquals("3단계 · 아직 안 봄", HelpCopy.stepsLine(3, seen = false))
    }

    /** DS-70: every icon-only button of this module carries a non-blank accessibility name. */
    @Test
    fun `icon button lint`() {
        val issues = io.github.lenticualrstone.civilight.designsystem.icon.IconButtonLint.lintDir(File("src/main"))
        assertEquals(io.github.lenticualrstone.civilight.designsystem.icon.IconButtonLint.report(issues), 0, issues.size)
    }

    /** Motion audit §4: no literal durations or curves, no default `animateItem()` or snap fling in this module. */
    @Test
    fun `motion lint`() {
        val issues = MotionLint.lintModule(File("."))
        assertEquals(MotionLint.report(issues), 0, issues.size)
    }
}
