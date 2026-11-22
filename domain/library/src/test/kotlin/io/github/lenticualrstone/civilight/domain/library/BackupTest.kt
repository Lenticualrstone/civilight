package io.github.lenticualrstone.civilight.domain.library

import io.github.lenticualrstone.civilight.domain.library.backup.BackupAction
import io.github.lenticualrstone.civilight.domain.library.backup.BackupCandidate
import io.github.lenticualrstone.civilight.domain.library.backup.BackupFailureBanner
import io.github.lenticualrstone.civilight.domain.library.backup.BackupPlanner
import io.github.lenticualrstone.civilight.domain.library.backup.RestoreProgress
import io.github.lenticualrstone.civilight.domain.library.backup.RestoreRunRow
import io.github.lenticualrstone.civilight.domain.library.crypto.EncryptedFiles
import io.github.lenticualrstone.civilight.domain.library.crypto.EncryptedOpen
import io.github.lenticualrstone.civilight.domain.library.crypto.PackageCrypto
import io.github.lenticualrstone.civilight.domain.library.crypto.RecoveryKeys
import io.github.lenticualrstone.civilight.domain.library.export.ProjectFileExport
import io.github.lenticualrstone.civilight.domain.library.format.BlobReader
import io.github.lenticualrstone.civilight.domain.library.format.PackageOpen
import io.github.lenticualrstone.civilight.domain.library.format.ProjectPackageReader
import io.github.lenticualrstone.civilight.domain.library.masking.Notifications
import io.github.lenticualrstone.civilight.domain.library.masking.ProjectProfile
import io.github.lenticualrstone.civilight.model.app.AppSettings
import io.github.lenticualrstone.civilight.model.app.BackupEntry
import io.github.lenticualrstone.civilight.model.app.BackupKind
import io.github.lenticualrstone.civilight.model.app.BackupRun
import io.github.lenticualrstone.civilight.model.app.BackupState
import io.github.lenticualrstone.civilight.model.app.LockKind
import io.github.lenticualrstone.civilight.model.doc.ProjectDoc
import io.github.lenticualrstone.civilight.model.file.PackageLockType
import io.github.lenticualrstone.civilight.model.project.ProjectFormat
import io.github.lenticualrstone.civilight.model.project.Protections
import io.github.lenticualrstone.civilight.model.project.Rating
import io.github.lenticualrstone.civilight.model.sample.ExampleKeys
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.security.SecureRandom

/** 60-library §4.7 backup cases B-01 … B-10. */
class BackupTest {
    private val ash = Fixtures.example()
    private val glass = Fixtures.projectWith("유리 정원", Rating.ALL, format = ProjectFormat.SHORT, scenes = 6, characters = 4)
    private val black = Fixtures.projectWith(
        "검은 서약 (가제)", Rating.R18, Protections(true, true, true, true), format = ProjectFormat.SETTING_ONLY, characters = 5,
    )
    private val side = Fixtures.projectWith("외전 · 오웬의 시장", Rating.R15, Protections(hidden = true), scenes = 2)
    private val hiddenTwo = listOf(black, side)

    private fun candidates(vararg docs: ProjectDoc) = docs.map { BackupCandidate.of(it) }

    /** 잿빛 with three hidden items: 노아, 1-7 씬 4 and the term 대정전. */
    private fun ashWithThreeHiddenItems(): ProjectDoc {
        val doc = Fixtures.withSceneHidden(Fixtures.withNoahHidden(), ExampleKeys.SCENE_1_7_4)
        return doc.copy(world = doc.world.copy(terms = doc.world.terms.map { if (it.key == ExampleKeys.TERM_BLACKOUT) it.copy(hidden = true) else it }))
    }

    @Test
    fun `B-01 hidden projects without a recovery password are left out`() {
        val plan = BackupPlanner.plan(candidates(*hiddenTwo.toTypedArray()), AppSettings(), recoverySet = false)
        assertEquals(listOf(BackupAction.EXCLUDED_HIDDEN, BackupAction.EXCLUDED_HIDDEN), plan.items.map { it.action })
        assertTrue(plan.uploads.isEmpty())
        assertEquals(2, plan.hiddenExcluded)
        assertEquals(listOf("숨긴 프로젝트 2 · 백업 안 됨"), plan.warnings)
    }

    @Test
    fun `B-02 with a recovery password hidden projects go in locked`() {
        val plan = BackupPlanner.plan(candidates(*hiddenTwo.toTypedArray()), AppSettings(backupHidden = true), recoverySet = true)
        assertEquals(listOf(BackupAction.UPLOAD, BackupAction.UPLOAD), plan.items.map { it.action })
        assertEquals(listOf(LockKind.RECOVERY, LockKind.RECOVERY), plan.items.map { it.lock })
        assertTrue(plan.warnings.isEmpty())

        // The uploaded file is recovery-locked and opens on any device with the password.
        val key = RecoveryKeys.create("등불지기의 복구 암호".toCharArray(), iterations = 1_000)
        val file = ProjectFileExport.backup(black, Fixtures.recorder(), BlobReader.NONE, key, "Civilight test", Fixtures.NOW, SecureRandom())
        assertTrue(file.encrypted)
        val header = (ProjectPackageReader.open(file.bytes) as PackageOpen.Encrypted).header
        assertEquals(PackageLockType.RECOVERY, header.lockType.known)
        val opened = EncryptedFiles.open(file.bytes, "등불지기의 복구 암호".toCharArray()) as EncryptedOpen.Opened
        val pkg = (opened.result as PackageOpen.Ready).pkg
        assertEquals("검은 서약 (가제)", pkg.doc.project.name)
        assertTrue(pkg.doc.project.protections.hidden)
        // Same device, no typing: the backup data key opens it too.
        assertTrue(PackageCrypto.decryptWithDataKey(file.bytes, key.dataKey) is io.github.lenticualrstone.civilight.domain.library.crypto.DecryptResult.Ok)
    }

    @Test
    fun `B-03 숨긴 것도 백업 off leaves hidden projects out`() {
        val plan = BackupPlanner.plan(candidates(*hiddenTwo.toTypedArray()), AppSettings(backupHidden = false), recoverySet = true)
        assertEquals(listOf(BackupAction.EXCLUDED_HIDDEN, BackupAction.EXCLUDED_HIDDEN), plan.items.map { it.action })
        assertEquals(listOf("숨긴 프로젝트 2 · 백업 안 됨"), plan.warnings)
    }

    @Test
    fun `B-04 hidden items without a recovery password go in unencrypted with a warning`() {
        val doc = ashWithThreeHiddenItems()
        val candidate = BackupCandidate.of(doc)
        assertEquals(3, candidate.hiddenItemCount)
        val plan = BackupPlanner.plan(listOf(candidate), AppSettings(), recoverySet = false)
        val item = plan.of(ExampleKeys.PROJECT)!!
        assertEquals(BackupAction.UPLOAD, item.action)
        assertNull(item.lock)
        assertEquals(listOf("숨긴 항목 3 · 암호 없이 백업 · 복구 암호 정하기 ›"), plan.warnings)
        val file = ProjectFileExport.backup(doc, Fixtures.recorder(), BlobReader.NONE, null, "Civilight test", Fixtures.NOW)
        assertFalse(file.encrypted)
        assertTrue(ProjectPackageReader.open(file.bytes) is PackageOpen.Ready)
    }

    @Test
    fun `B-05 hidden items with a recovery password lock the whole file`() {
        val plan = BackupPlanner.plan(listOf(BackupCandidate.of(ashWithThreeHiddenItems())), AppSettings(), recoverySet = true)
        val item = plan.of(ExampleKeys.PROJECT)!!
        assertEquals(BackupAction.UPLOAD, item.action)
        assertEquals(LockKind.RECOVERY, item.lock)
        assertTrue(plan.warnings.isEmpty())

        // Restoring it with the 복구 암호 gives the project back as it was: visible, items hidden.
        val pw = "등불지기의 복구 암호".toCharArray()
        val key = RecoveryKeys.create(pw, iterations = 1_000)
        val file = ProjectFileExport.backup(ashWithThreeHiddenItems(), Fixtures.recorder(), BlobReader.NONE, key, "Civilight test", Fixtures.NOW)
        val pkg = ((EncryptedFiles.open(file.bytes, pw) as EncryptedOpen.Opened).result as PackageOpen.Ready).pkg
        assertEquals(PackageLockType.RECOVERY, pkg.lockType!!.known)
        val restored = io.github.lenticualrstone.civilight.domain.library.importing.ProjectImports.direct(pkg, io.github.lenticualrstone.civilight.model.recorder.RecorderLibrary(), Fixtures.NOW, Fixtures.keys(55))
        assertFalse(restored.doc.project.protections.hidden)
        assertFalse(restored.entry.hidden)
        assertEquals(3, io.github.lenticualrstone.civilight.domain.library.hiding.HiddenItems(restored.doc).count)
    }

    @Test
    fun `B-06 an unchanged project reuses its last version`() {
        val previous = BackupEntry(BackupKind.PROJECT, glass.key, "drive-glass", "v1", 1_000, encrypted = false)
        val lastUpload = Fixtures.at("2026-11-21T00:00:00Z")
        val plan = BackupPlanner.plan(
            listOf(BackupCandidate.of(glass, updatedAt = Fixtures.at("2026-11-19T00:00:00Z")), BackupCandidate.of(ash, updatedAt = Fixtures.NOW)),
            AppSettings(), recoverySet = false,
            lastUploads = mapOf(glass.key to (lastUpload to previous), ash.key to (lastUpload to previous.copy(projectKey = ash.key, driveFileId = "drive-ash"))),
        )
        val g = plan.of(glass.key)!!
        assertEquals(BackupAction.REUSE, g.action)
        assertEquals(previous, g.previous)
        assertEquals(BackupAction.UPLOAD, plan.of(ash.key)!!.action)
        assertEquals(listOf(ash.key), plan.uploads.map { it.projectKey })
    }

    @Test
    fun `B-07 versions no run of the last 30 days refers to are pruned`() {
        fun entry(project: String, version: String) = BackupEntry(BackupKind.PROJECT, project, "drive-$project", version, 10, encrypted = false)
        val old = BackupRun("r1", Fixtures.at("2026-10-10T00:00:00Z"), ok = true, entries = listOf(entry("glass", "v1"), entry("ash", "v1"), entry("side", "v1")))
        val older = BackupRun("r0", Fixtures.at("2026-10-01T00:00:00Z"), ok = true, entries = listOf(entry("ash", "v0")))
        val recent = BackupRun("r2", Fixtures.at("2026-11-20T00:00:00Z"), ok = true, entries = listOf(entry("glass", "v1"), entry("ash", "v2")))
        val pruned = BackupPlanner.prune(listOf(older, old, recent), Fixtures.NOW)
        assertEquals(setOf("ash" to "v0", "ash" to "v1", "side" to "v1"), pruned.map { it.projectKey to it.versionId }.toSet())
        // glass v1 is still referenced by the recent run.
        assertTrue(pruned.none { it.projectKey == "glass" })
    }

    @Test
    fun `B-08 restore list row and button`() {
        val mb = 1024L * 1024
        val run = BackupRun(
            "r", Fixtures.at("2026-11-19T23:12:00Z"), ok = true, totalBytes = 340 * mb,
            entries = listOf(
                BackupEntry(BackupKind.PROJECT, "ash", "d1", "v", 100 * mb, encrypted = false),
                BackupEntry(BackupKind.PROJECT, "black", "d2", "v", 100 * mb, encrypted = true, lockType = LockKind.RECOVERY, hidden = true),
                BackupEntry(BackupKind.PROJECT, "side", "d3", "v", 100 * mb, encrypted = true, lockType = LockKind.RECOVERY, hidden = true),
                BackupEntry(BackupKind.RECORDER, null, "d4", "v", 30 * mb, encrypted = false),
                BackupEntry(BackupKind.SETTINGS, null, "d5", "v", 10 * mb, encrypted = false),
            ),
        )
        val row = RestoreRunRow.of(run, Fixtures.ZONE)
        assertEquals("11월 20일 08:12", row.title)
        assertEquals("프로젝트 3 · 숨김 2 포함 · 340 MB", row.sub)
        assertEquals("11월 20일 백업 되살리기", row.button)
        val noHidden = RestoreRunRow.of(run.copy(entries = run.entries.filterNot { it.hidden }), Fixtures.ZONE)
        assertEquals("프로젝트 1 · 340 MB", noHidden.sub)
    }

    @Test
    fun `B-09 a three-day failure streak`() {
        val state = BackupState(
            lastSuccessAt = Fixtures.at("2026-11-17T23:12:00Z"),
            failureStreakStartedAt = Fixtures.at("2026-11-19T23:00:00Z"),
            lastFailureReason = "loginExpired",
        )
        val banner = BackupFailureBanner.of(state, Fixtures.NOW, Fixtures.ZONE)!!
        assertEquals("백업 실패 · 3일째", banner.title)
        assertEquals("Google 로그인 만료 · 원고는 기기에 안전", banner.sub)
        assertEquals(listOf("다시 로그인", "지금 다시 시도"), banner.buttons)
        assertEquals("마지막 성공 11월 18일 08:12", banner.driveRow)
    }

    @Test
    fun `B-10 a failure today is day 1`() {
        val state = BackupState(failureStreakStartedAt = Fixtures.at("2026-11-21T22:00:00Z"), lastFailureReason = "loginExpired")
        assertEquals("백업 실패 · 1일째", BackupFailureBanner.of(state, Fixtures.NOW, Fixtures.ZONE)!!.title)
        assertNull(BackupFailureBanner.of(BackupState(lastSuccessAt = Fixtures.NOW), Fixtures.NOW, Fixtures.ZONE))
    }

    @Test
    fun `R-18 not hidden goes unencrypted, example and trashed projects are skipped`() {
        val r18 = Fixtures.projectWith("R-18 공개", Rating.R18, Protections(false, true, true, true), characters = 1)
        val plan = BackupPlanner.plan(
            listOf(BackupCandidate.of(r18), BackupCandidate.of(ash, isExample = true), BackupCandidate.of(glass, trashed = true)),
            AppSettings(), recoverySet = true,
        )
        assertEquals(BackupAction.UPLOAD, plan.of(r18.key)!!.action)
        assertNull(plan.of(r18.key)!!.lock)
        assertEquals(BackupAction.SKIPPED, plan.of(ash.key)!!.action)
        assertEquals(BackupAction.SKIPPED, plan.of(glass.key)!!.action)
    }

    @Test
    fun `a version locked differently is uploaded again`() {
        // Hidden items appeared and a recovery password was set: the old unencrypted version is not reused.
        val previous = BackupEntry(BackupKind.PROJECT, ash.key, "drive-ash", "v1", 10, encrypted = false)
        val plan = BackupPlanner.plan(
            listOf(BackupCandidate.of(ashWithThreeHiddenItems(), updatedAt = Fixtures.at("2026-11-01T00:00:00Z"))),
            AppSettings(), recoverySet = true, lastUploads = mapOf(ash.key to (Fixtures.NOW to previous)),
        )
        assertEquals(BackupAction.UPLOAD, plan.of(ash.key)!!.action)
    }

    @Test
    fun `backup notification counts hidden projects`() {
        val text = Notifications.backupDone(listOf(ash, glass, Fixtures.otherProject("세 번째", Rating.ALL), black).map { ProjectProfile.of(it) })
        assertEquals("백업 끝 · 프로젝트 3 · 숨긴 프로젝트 1", text)
    }

    @Test
    fun `restoring progress lines`() {
        val mb = 1024L * 1024
        val p = RestoreProgress(doneBytes = 120 * mb, totalBytes = 340 * mb, projects = 3, images = 42, audio = 7, hiddenProjects = 2)
        assertEquals("되살리는 중", p.title)
        assertEquals("120 / 340 MB", p.bytesLine)
        assertEquals(
            listOf("원고 · 인물 · 세계" to "3개 프로젝트", "에셋 · 음성 기록" to "이미지 42 · 음성 7", "숨긴 프로젝트 2" to "지문 확인 후 풀림"),
            p.steps,
        )
        assertEquals("앱을 닫아도 계속 · 끝나면 알림", p.note)
    }
}
