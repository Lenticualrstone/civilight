package io.github.lenticualrstone.civilight.feature.library.openfile

import io.github.lenticualrstone.civilight.domain.library.crypto.DecryptResult
import io.github.lenticualrstone.civilight.domain.library.crypto.EncryptedFileSheet
import io.github.lenticualrstone.civilight.domain.library.crypto.PackageCrypto
import io.github.lenticualrstone.civilight.domain.library.format.PackageZipException
import io.github.lenticualrstone.civilight.domain.library.format.RecorderPackage
import io.github.lenticualrstone.civilight.domain.library.format.RecorderPackageContent
import io.github.lenticualrstone.civilight.domain.library.format.RecorderPackageOpen
import io.github.lenticualrstone.civilight.domain.library.format.ZipCodec
import io.github.lenticualrstone.civilight.domain.library.importing.RecorderMerge
import io.github.lenticualrstone.civilight.model.Key
import io.github.lenticualrstone.civilight.model.file.PackagePaths

/**
 * Opening a `.civilight-recorder` file (LIB-48): the same container and version rules as a project
 * file (`domain:library` [RecorderPackage]); a recovery-locked file asks for its password with the
 * LIB-54 sheet; `열기` merges the records by permanent key, so a record already here only gains links
 * (R23 §6), and stores the audio it carries.
 */
object RecorderFiles {
    /** The first entry names the recorder library (an encrypted container keeps the inner mimetype). */
    fun isRecorderFile(bytes: ByteArray): Boolean = try {
        ZipCodec.read(bytes).items.firstOrNull()?.let { it.path == PackagePaths.MIMETYPE && it.bytes.toString(Charsets.US_ASCII).trim() == PackagePaths.MIME_RECORDER } == true
    } catch (_: PackageZipException) {
        false
    }

    fun stage(open: RecorderPackageOpen, fileName: String): OpenStage = when (open) {
        is RecorderPackageOpen.Ready -> OpenStage.Recorder(open.content)
        is RecorderPackageOpen.Encrypted -> OpenStage.Password(EncryptedFileSheet.of(open.header, fileName))
        is RecorderPackageOpen.Newer -> OpenStage.Failed(OpenFileCopy.NEWER)
        is RecorderPackageOpen.Damaged -> OpenStage.Failed(OpenFileCopy.DAMAGED)
        is RecorderPackageOpen.NotAPackage -> OpenStage.Failed(OpenFileCopy.NOT_A_PROJECT)
    }

    fun unlock(outer: ByteArray, password: CharArray, sheet: EncryptedFileSheet): OpenStage = when (val d = PackageCrypto.decrypt(outer, password)) {
        is DecryptResult.Ok -> stage(RecorderPackage.read(d.inner), sheet.fileName)
        DecryptResult.WrongPassword -> OpenStage.Password(sheet, error = OpenFileCopy.WRONG_PASSWORD)
        is DecryptResult.Unsupported -> OpenStage.Failed(OpenFileCopy.UNSUPPORTED)
        is DecryptResult.NotEncrypted -> OpenStage.Failed(OpenFileCopy.NOT_A_PROJECT)
    }

    /** `열기`: audio into the blob store, then the records merged into the library as it is now. */
    suspend fun import(content: RecorderPackageContent, ports: ImportPorts, newKey: () -> Key) {
        for ((hash, bytes) in content.audio) {
            val audio = content.library.records.firstNotNullOfOrNull { r -> r.audio?.takeIf { it.hash == hash } } ?: continue
            ports.importBlob(hash, bytes, audio.mime, hash)
        }
        ports.updateRecorder { RecorderMerge.merge(it, content.library, newKey) }
    }
}
