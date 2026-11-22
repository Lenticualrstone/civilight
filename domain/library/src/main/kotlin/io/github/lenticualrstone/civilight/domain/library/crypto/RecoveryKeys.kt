package io.github.lenticualrstone.civilight.domain.library.crypto

import io.github.lenticualrstone.civilight.domain.library.crypto.PackageCrypto.b64
import io.github.lenticualrstone.civilight.domain.library.crypto.PackageCrypto.bytes
import io.github.lenticualrstone.civilight.domain.library.crypto.PackageCrypto.unb64
import io.github.lenticualrstone.civilight.model.Timestamp
import io.github.lenticualrstone.civilight.model.app.RecoveryKeyMaterial
import java.security.SecureRandom

/**
 * The 복구 암호 key in use (LIB-58 `ED`): the random backup [dataKey] plus what travels in each
 * recovery-locked header (salt, iterations and the data key wrapped by the password). The password
 * itself is never kept.
 */
class RecoveryKey(
    val dataKey: ByteArray,
    val salt: ByteArray,
    val iterations: Int,
    /** base64 of `nonce || AES-GCM(kek, dataKey)`. */
    val wrappedDataKey: String,
) {
    /** The stored material; the Android layer supplies the Keystore-wrapped copy of [dataKey]. */
    fun toMaterial(dataKeyWrappedByKeystore: String, keystoreAlias: String, setAt: Timestamp) = RecoveryKeyMaterial(
        kdf = PackageCrypto.KDF,
        iterations = iterations,
        salt = b64(salt),
        dataKeyWrappedByPassword = wrappedDataKey,
        dataKeyWrappedByKeystore = dataKeyWrappedByKeystore,
        keystoreAlias = keystoreAlias,
        setAt = setAt,
    )
}

/** Creating, checking and restoring the 복구 암호 key (LIB-58). */
object RecoveryKeys {
    /** Minimum length for `정하기` (`ED`). */
    const val MIN_LENGTH = 8

    /** A new backup data key locked by [password]. Changing the password makes a new key (§6 Q24). */
    fun create(
        password: CharArray,
        iterations: Int = PackageCrypto.DEFAULT_ITERATIONS,
        random: SecureRandom = SecureRandom(),
    ): RecoveryKey {
        val dataKey = random.bytes(PackageCrypto.KEY_BYTES)
        val salt = random.bytes(PackageCrypto.SALT_BYTES)
        val kek = PackageCrypto.deriveKey(password, salt, iterations)
        return RecoveryKey(dataKey, salt, iterations, PackageCrypto.wrapDataKey(dataKey, kek, random))
    }

    /** Rebuilds the key from stored [material] and the data key unwrapped by the Keystore. */
    fun fromMaterial(material: RecoveryKeyMaterial, dataKey: ByteArray): RecoveryKey =
        RecoveryKey(dataKey, unb64(material.salt), material.iterations, material.dataKeyWrappedByPassword)

    /** The data key when [password] is right, else null (e.g. to confirm the password before `바꾸기`). */
    fun unlock(material: RecoveryKeyMaterial, password: CharArray): ByteArray? {
        val kek = PackageCrypto.deriveKey(password, unb64(material.salt), material.iterations)
        return PackageCrypto.unwrapDataKey(material.dataKeyWrappedByPassword, kek)
    }

    /** `정하기` is enabled when both fields match and the password is long enough (`ED` minimum 8). */
    fun canSet(password: CharArray, repeat: CharArray): Boolean =
        password.size >= MIN_LENGTH && password.contentEquals(repeat)
}
