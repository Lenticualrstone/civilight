package io.github.lenticualrstone.civilight.domain.library.crypto

import io.github.lenticualrstone.civilight.domain.library.format.PackageZipException
import io.github.lenticualrstone.civilight.domain.library.format.ZipCodec
import io.github.lenticualrstone.civilight.model.file.EncryptionHeader
import io.github.lenticualrstone.civilight.model.file.PackageLockType
import io.github.lenticualrstone.civilight.model.file.PackagePaths
import io.github.lenticualrstone.civilight.model.json.CivilightJson
import io.github.lenticualrstone.civilight.model.open
import java.security.GeneralSecurityException
import java.security.SecureRandom
import java.util.Base64
import javax.crypto.AEADBadTagException
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

/** Outcome of decrypting a container. */
sealed interface DecryptResult {
    /** The inner `.civilight` package bytes. */
    class Ok(val inner: ByteArray, val header: EncryptionHeader) : DecryptResult

    /** Wrong password, or the payload or header was altered (GCM tag mismatch). */
    data object WrongPassword : DecryptResult

    /** A KDF or cipher this app does not implement (recorded by a newer app). */
    data class Unsupported(val kdf: String, val cipher: String) : DecryptResult

    /** Not an encrypted container, or its parts are unreadable. */
    data class NotEncrypted(val reason: String) : DecryptResult
}

/**
 * Whole-file encryption of a package (LIB-51 `ED`, §7.12): outer ZIP = `mimetype` (stored) +
 * `encryption.json` + `payload.bin`, where `payload.bin` is the complete inner package encrypted
 * with AES-256-GCM. The exact `encryption.json` bytes are the GCM associated data, so the header
 * (lock type, salt, iterations, nonce) cannot be altered without failing decryption.
 *
 * - `export` lock: content key = PBKDF2-HMAC-SHA256(password, salt, iterations).
 * - `recovery` lock (LIB-58): content key = a random backup data key; the header's
 *   `wrappedDataKey` holds it wrapped (AES-256-GCM, its own nonce prepended) with
 *   PBKDF2-HMAC-SHA256(복구 암호, salt, iterations), so daily backups need no typing (the data key
 *   is also kept by the Android Keystore, outside this module) and any device opens them with the
 *   password.
 *
 * The outer `mimetype` is the one of the inner package (`.civilight` or `.civilight-recorder`), so
 * a file manager still routes the file to the right reader.
 *
 * Passwords are `CharArray` so callers can wipe them; this object never stores them.
 */
object PackageCrypto {
    const val KDF = "PBKDF2-HMAC-SHA256"
    const val CIPHER = "AES-256-GCM"
    const val DEFAULT_ITERATIONS = 600_000
    const val SALT_BYTES = 16
    const val NONCE_BYTES = 12
    const val KEY_BYTES = 32
    private const val TAG_BITS = 128
    private val WRAP_AAD = "civilight-recovery-key".toByteArray(Charsets.US_ASCII)

    /** Encrypts [inner] with a password (`암호` of an export, lock type `export`). */
    fun encryptWithPassword(
        inner: ByteArray,
        password: CharArray,
        iterations: Int = DEFAULT_ITERATIONS,
        random: SecureRandom = SecureRandom(),
        time: Long = 0L,
        mimetype: String = PackagePaths.MIME_PROJECT,
    ): ByteArray {
        val salt = random.bytes(SALT_BYTES)
        val nonce = random.bytes(NONCE_BYTES)
        val header = EncryptionHeader(
            lockType = PackageLockType.EXPORT.open(), kdf = KDF, iterations = iterations,
            salt = b64(salt), cipher = CIPHER, nonce = b64(nonce),
        )
        val key = deriveKey(password, salt, iterations)
        return container(header, key, nonce, inner, time, mimetype)
    }

    /** Encrypts [inner] with the backup data key of [recovery] (lock type `recovery`). */
    fun encryptWithRecoveryKey(
        inner: ByteArray,
        recovery: RecoveryKey,
        random: SecureRandom = SecureRandom(),
        time: Long = 0L,
        mimetype: String = PackagePaths.MIME_PROJECT,
    ): ByteArray {
        val nonce = random.bytes(NONCE_BYTES)
        val header = EncryptionHeader(
            lockType = PackageLockType.RECOVERY.open(), kdf = KDF, iterations = recovery.iterations,
            salt = b64(recovery.salt), cipher = CIPHER, nonce = b64(nonce), wrappedDataKey = recovery.wrappedDataKey,
        )
        return container(header, recovery.dataKey, nonce, inner, time, mimetype)
    }

    /** The header of an encrypted container, or null when [outer] is not one. */
    fun headerOf(outer: ByteArray): EncryptionHeader? = try {
        ZipCodec.read(outer)[PackagePaths.ENCRYPTION]?.let { parseHeader(it) }
    } catch (e: PackageZipException) {
        null
    } catch (e: IllegalArgumentException) {
        null
    }

    /** Decrypts with the password typed for the header's lock type (`암호` or `복구 암호`). */
    fun decrypt(outer: ByteArray, password: CharArray): DecryptResult {
        val parts = parts(outer) ?: return DecryptResult.NotEncrypted("no encryption.json or payload.bin")
        val (headerBytes, payload) = parts
        val header = try {
            parseHeader(headerBytes)
        } catch (e: IllegalArgumentException) {
            return DecryptResult.NotEncrypted("encryption.json: ${e.message}")
        }
        if (header.kdf != KDF || header.cipher != CIPHER) return DecryptResult.Unsupported(header.kdf, header.cipher)
        return try {
            val salt = unb64(header.salt)
            val kek = deriveKey(password, salt, header.iterations)
            val contentKey = when (header.lockType.known) {
                PackageLockType.RECOVERY -> unwrapDataKey(header.wrappedDataKey ?: return DecryptResult.NotEncrypted("no wrappedDataKey"), kek)
                    ?: return DecryptResult.WrongPassword
                else -> kek
            }
            DecryptResult.Ok(gcm(Cipher.DECRYPT_MODE, contentKey, unb64(header.nonce), headerBytes, payload), header)
        } catch (e: AEADBadTagException) {
            DecryptResult.WrongPassword
        } catch (e: IllegalArgumentException) {
            DecryptResult.NotEncrypted(e.message ?: "bad base64")
        } catch (e: GeneralSecurityException) {
            DecryptResult.NotEncrypted(e.message ?: "cipher error")
        }
    }

    /** Decrypts a recovery-locked container with the backup data key (no password; same device). */
    fun decryptWithDataKey(outer: ByteArray, dataKey: ByteArray): DecryptResult {
        val (headerBytes, payload) = parts(outer) ?: return DecryptResult.NotEncrypted("no encryption.json or payload.bin")
        val header = try {
            parseHeader(headerBytes)
        } catch (e: IllegalArgumentException) {
            return DecryptResult.NotEncrypted("encryption.json: ${e.message}")
        }
        if (header.cipher != CIPHER) return DecryptResult.Unsupported(header.kdf, header.cipher)
        return try {
            DecryptResult.Ok(gcm(Cipher.DECRYPT_MODE, dataKey, unb64(header.nonce), headerBytes, payload), header)
        } catch (e: AEADBadTagException) {
            DecryptResult.WrongPassword
        } catch (e: GeneralSecurityException) {
            DecryptResult.NotEncrypted(e.message ?: "cipher error")
        }
    }

    /** PBKDF2-HMAC-SHA256 → 256-bit key. */
    fun deriveKey(password: CharArray, salt: ByteArray, iterations: Int): ByteArray {
        require(iterations > 0) { "iterations must be positive" }
        val spec = PBEKeySpec(password, salt, iterations, KEY_BYTES * 8)
        try {
            return SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).encoded
        } finally {
            spec.clearPassword()
        }
    }

    /** `nonce || ciphertext` of [dataKey] under [kek], base64. */
    internal fun wrapDataKey(dataKey: ByteArray, kek: ByteArray, random: SecureRandom): String {
        val nonce = random.bytes(NONCE_BYTES)
        return b64(nonce + gcm(Cipher.ENCRYPT_MODE, kek, nonce, WRAP_AAD, dataKey))
    }

    /** The data key, or null when [kek] is wrong. */
    internal fun unwrapDataKey(wrapped: String, kek: ByteArray): ByteArray? {
        val bytes = unb64(wrapped)
        if (bytes.size <= NONCE_BYTES) return null
        return try {
            gcm(Cipher.DECRYPT_MODE, kek, bytes.copyOfRange(0, NONCE_BYTES), WRAP_AAD, bytes.copyOfRange(NONCE_BYTES, bytes.size))
        } catch (e: AEADBadTagException) {
            null
        }
    }

    private fun container(header: EncryptionHeader, key: ByteArray, nonce: ByteArray, inner: ByteArray, time: Long, mimetype: String): ByteArray {
        val headerBytes = CivilightJson.encode(EncryptionHeader.serializer(), header).toByteArray(Charsets.UTF_8)
        val payload = gcm(Cipher.ENCRYPT_MODE, key, nonce, headerBytes, inner)
        return ZipCodec.write(
            mimetype,
            linkedMapOf(PackagePaths.ENCRYPTION to headerBytes, PackagePaths.PAYLOAD to payload),
            time,
        )
    }

    private fun parts(outer: ByteArray): Pair<ByteArray, ByteArray>? = try {
        val zip = ZipCodec.read(outer)
        val header = zip[PackagePaths.ENCRYPTION]
        val payload = zip[PackagePaths.PAYLOAD]
        if (header == null || payload == null) null else header to payload
    } catch (e: PackageZipException) {
        null
    }

    private fun parseHeader(bytes: ByteArray): EncryptionHeader =
        CivilightJson.decode(EncryptionHeader.serializer(), bytes.toString(Charsets.UTF_8))

    private fun gcm(mode: Int, key: ByteArray, nonce: ByteArray, aad: ByteArray, data: ByteArray): ByteArray {
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(mode, SecretKeySpec(key, "AES"), GCMParameterSpec(TAG_BITS, nonce))
        cipher.updateAAD(aad)
        return cipher.doFinal(data)
    }

    internal fun SecureRandom.bytes(n: Int): ByteArray = ByteArray(n).also { nextBytes(it) }

    internal fun b64(bytes: ByteArray): String = Base64.getEncoder().encodeToString(bytes)

    internal fun unb64(text: String): ByteArray = Base64.getDecoder().decode(text)
}
