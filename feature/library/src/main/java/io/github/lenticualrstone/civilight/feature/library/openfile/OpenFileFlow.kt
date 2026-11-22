package io.github.lenticualrstone.civilight.feature.library.openfile

import io.github.lenticualrstone.civilight.domain.library.crypto.EncryptedFileSheet
import io.github.lenticualrstone.civilight.domain.library.crypto.EncryptedFiles
import io.github.lenticualrstone.civilight.domain.library.crypto.EncryptedOpen
import io.github.lenticualrstone.civilight.domain.library.format.ImportedPackage
import io.github.lenticualrstone.civilight.domain.library.format.NewerFileSheet
import io.github.lenticualrstone.civilight.domain.library.format.PackageOpen
import io.github.lenticualrstone.civilight.domain.library.format.ProjectPackageReader
import io.github.lenticualrstone.civilight.domain.library.format.RecorderPackage
import io.github.lenticualrstone.civilight.domain.library.format.RecorderPackageContent
import io.github.lenticualrstone.civilight.domain.library.format.UpgradeChain
import io.github.lenticualrstone.civilight.domain.library.importing.OpenDecision
import io.github.lenticualrstone.civilight.domain.library.importing.OpenDecisions
import io.github.lenticualrstone.civilight.domain.library.importing.OpenFileSheet
import io.github.lenticualrstone.civilight.domain.library.importing.OpenFileSheets
import io.github.lenticualrstone.civilight.domain.library.importing.SameProjectChoice
import io.github.lenticualrstone.civilight.model.Timestamp
import io.github.lenticualrstone.civilight.model.app.LibraryEntry
import io.github.lenticualrstone.civilight.model.file.Features
import io.github.lenticualrstone.civilight.model.project.Protections
import java.time.ZoneId

/** Where the 파일에서 열기 flow stands (LIB-52 … LIB-55). */
sealed interface OpenStage {
    /** Reading the file (or decrypting it). */
    data object Reading : OpenStage

    /** Nothing can be imported; [message] is the one-line reason (`OpenFileCopy`). */
    data class Failed(val message: String) : OpenStage

    /** `RHEncryptedFile`: the password sheet; [error] after a wrong password, [busy] while decrypting. */
    data class Password(val sheet: EncryptedFileSheet, val error: String? = null, val busy: Boolean = false) : OpenStage

    /**
     * A Life Recorder library file `.civilight-recorder` (LIB-48): its records merge into the
     * library by key (R23 §6). `ED`: no board draws this sheet; it reuses the 서재 words
     * `라이프 레코더` · `기록 <n>` and `취소` · `열기`.
     */
    data class Recorder(val content: RecorderPackageContent, val busy: Boolean = false) : OpenStage {
        val records: Int get() = content.library.records.size
    }

    /** `RHNewerFile`: refused, nothing imported (LIB-53). */
    data class Newer(val sheet: NewerFileSheet, val summary: String) : OpenStage

    /**
     * `RHOpenFile`: what the file holds; with the same project in the 서재, the `새 프로젝트로` ·
     * `덮어쓰기` choice ([choice], default `새 프로젝트로`).
     */
    class Ask(
        val sheet: OpenFileSheet,
        val pkg: ImportedPackage,
        val decision: OpenDecision,
        val choice: SameProjectChoice?,
        /** False when the same-key project is the read-only example: only `새 프로젝트로`. */
        val overwriteAllowed: Boolean,
        /** The same-key project is hidden: the sheet shows it only while it is open (LIB-27). */
        val existingHidden: Boolean,
        /** The file's protections, for `ProtectWindow` while its name and summary show (LIB-26). */
        val protections: Protections,
        val busy: Boolean = false,
    ) : OpenStage {
        fun choose(next: SameProjectChoice): Ask =
            if (next == SameProjectChoice.OVERWRITE && !overwriteAllowed) this else copy(choice = next)

        fun copy(choice: SameProjectChoice? = this.choice, busy: Boolean = this.busy): Ask =
            Ask(sheet, pkg, decision, choice, overwriteAllowed, existingHidden, protections, busy)
    }
}

/**
 * The pure steps of 파일에서 열기: bytes → stage, password → stage, package → `RHOpenFile`. The
 * reading itself (`ContentResolver`, a background thread) and the import are the view model's.
 */
object OpenFileFlow {
    /** The file's bytes (or null when it could not be read) → the first stage. */
    fun open(
        bytes: ByteArray?,
        fileName: String,
        library: List<LibraryEntry>,
        existingSceneCount: (LibraryEntry) -> Int,
        now: Timestamp,
        zone: ZoneId,
        chain: UpgradeChain = UpgradeChain.CURRENT,
        knownFeatures: Set<String> = Features.KNOWN,
    ): OpenStage {
        if (bytes == null) return OpenStage.Failed(OpenFileCopy.NOT_A_PROJECT)
        if (RecorderFiles.isRecorderFile(bytes)) return RecorderFiles.stage(RecorderPackage.read(bytes), fileName)
        return after(ProjectPackageReader.open(bytes, chain, knownFeatures), bytes.size.toLong(), fileName, library, existingSceneCount, now, zone)
    }

    /** `열기` on the password sheet: wrong password keeps the sheet with an error (retry, no lockout). */
    fun unlock(
        outer: ByteArray,
        password: CharArray,
        sheet: EncryptedFileSheet,
        library: List<LibraryEntry>,
        existingSceneCount: (LibraryEntry) -> Int,
        now: Timestamp,
        zone: ZoneId,
        chain: UpgradeChain = UpgradeChain.CURRENT,
        knownFeatures: Set<String> = Features.KNOWN,
    ): OpenStage = if (RecorderFiles.isRecorderFile(outer)) RecorderFiles.unlock(outer, password, sheet) else when (val r = EncryptedFiles.open(outer, password, chain, knownFeatures)) {
        EncryptedOpen.WrongPassword -> OpenStage.Password(sheet, error = OpenFileCopy.WRONG_PASSWORD)
        is EncryptedOpen.Unsupported -> OpenStage.Failed(OpenFileCopy.UNSUPPORTED)
        is EncryptedOpen.NotEncrypted -> OpenStage.Failed(OpenFileCopy.NOT_A_PROJECT)
        // The size on the sheet is the file's own (the encrypted container).
        is EncryptedOpen.Opened -> after(r.result, outer.size.toLong(), sheet.fileName, library, existingSceneCount, now, zone)
    }

    private fun after(
        result: PackageOpen,
        sizeBytes: Long,
        fileName: String,
        library: List<LibraryEntry>,
        existingSceneCount: (LibraryEntry) -> Int,
        now: Timestamp,
        zone: ZoneId,
    ): OpenStage = when (result) {
        is PackageOpen.NotAPackage -> OpenStage.Failed(OpenFileCopy.NOT_A_PROJECT)
        is PackageOpen.Damaged -> OpenStage.Failed(OpenFileCopy.DAMAGED)
        is PackageOpen.Encrypted -> OpenStage.Password(EncryptedFileSheet.of(result.header, fileName))
        is PackageOpen.Newer -> OpenStage.Newer(result.sheet, result.sheet.summary(sizeBytes, zone))
        is PackageOpen.Ready -> ask(result.pkg, sizeBytes, library, existingSceneCount, now, zone)
    }

    /** `RHOpenFile` for a readable package; same project = same project key (never the name). */
    fun ask(
        pkg: ImportedPackage,
        sizeBytes: Long,
        library: List<LibraryEntry>,
        existingSceneCount: (LibraryEntry) -> Int,
        now: Timestamp,
        zone: ZoneId,
    ): OpenStage.Ask {
        val decision = OpenDecisions.decide(pkg, library)
        val existing = (decision as? OpenDecision.AskSameProject)?.existing
        val sheet = OpenFileSheets.of(pkg, decision, existing?.let(existingSceneCount) ?: 0, now, zone, sizeBytes)
        return OpenStage.Ask(
            sheet = sheet,
            pkg = pkg,
            decision = decision,
            choice = (decision as? OpenDecision.AskSameProject)?.choices?.default,
            overwriteAllowed = existing != null && !existing.isExample && !existing.readOnly,
            existingHidden = existing?.hidden == true,
            protections = pkg.doc.project.protections,
        )
    }
}
