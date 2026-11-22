package io.github.lenticualrstone.civilight.domain.library.crypto

import io.github.lenticualrstone.civilight.domain.library.format.PackageOpen
import io.github.lenticualrstone.civilight.domain.library.format.ProjectPackageReader
import io.github.lenticualrstone.civilight.domain.library.format.UpgradeChain
import io.github.lenticualrstone.civilight.model.file.EncryptionHeader
import io.github.lenticualrstone.civilight.model.file.Features
import io.github.lenticualrstone.civilight.model.file.PackageLockType

/** The password field of an encrypted file; its label follows the lock type (LIB-54). */
data class PasswordField(val label: String, val hint: String?) {
    companion object {
        val EXPORT = PasswordField("암호", "내보낼 때 정한 암호")
        val RECOVERY = PasswordField("복구 암호", null)

        /** An unknown lock type asks with the export wording (`ED`). */
        fun of(header: EncryptionHeader): PasswordField =
            if (header.lockType.known == PackageLockType.RECOVERY) RECOVERY else EXPORT
    }
}

/** The `RHEncryptedFile` sheet (LIB-54). Name, rating and content stay unknown until opened. */
data class EncryptedFileSheet(val fileName: String, val field: PasswordField) {
    val title: String get() = TITLE
    val note: String get() = NOTE
    val afterNote: String get() = AFTER_NOTE
    val buttons: List<String> get() = listOf(CANCEL, OPEN)

    companion object {
        const val TITLE = "암호 파일"
        const val NOTE = "이름 · 등급 · 내용 · 열면 보임"
        const val AFTER_NOTE = "열면 ‘숨긴 프로젝트’ 묶음으로 · 지문으로 다시 잠금"
        const val CANCEL = "취소"
        const val OPEN = "열기"

        fun of(header: EncryptionHeader, fileName: String) = EncryptedFileSheet(fileName, PasswordField.of(header))
    }
}

/** Outcome of `열기` on the encrypted-file sheet. */
sealed interface EncryptedOpen {
    /** Wrong password: field error, retry allowed, no lockout (`ED`). */
    data object WrongPassword : EncryptedOpen

    data class Unsupported(val kdf: String, val cipher: String) : EncryptedOpen

    data class NotEncrypted(val reason: String) : EncryptedOpen

    /**
     * The inner package, then the same checks as an unencrypted file (LIB-52, LIB-53). A [PackageOpen.Ready]
     * result is marked `decrypted`, so the import lands hidden on this device.
     */
    data class Opened(val result: PackageOpen) : EncryptedOpen
}

/** Decrypt-then-open (LIB-54). */
object EncryptedFiles {
    fun open(
        outer: ByteArray,
        password: CharArray,
        chain: UpgradeChain = UpgradeChain.CURRENT,
        knownFeatures: Set<String> = Features.KNOWN,
    ): EncryptedOpen = when (val d = PackageCrypto.decrypt(outer, password)) {
        is DecryptResult.Ok -> EncryptedOpen.Opened(markDecrypted(ProjectPackageReader.open(d.inner, chain, knownFeatures)))
        DecryptResult.WrongPassword -> EncryptedOpen.WrongPassword
        is DecryptResult.Unsupported -> EncryptedOpen.Unsupported(d.kdf, d.cipher)
        is DecryptResult.NotEncrypted -> EncryptedOpen.NotEncrypted(d.reason)
    }

    private fun markDecrypted(result: PackageOpen): PackageOpen =
        if (result is PackageOpen.Ready) PackageOpen.Ready(result.pkg.copyDecrypted()) else result
}
