package io.github.lenticualrstone.civilight.feature.library.backup

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import io.github.lenticualrstone.civilight.domain.library.crypto.PackageCrypto
import io.github.lenticualrstone.civilight.domain.library.crypto.RecoveryKey
import io.github.lenticualrstone.civilight.domain.library.crypto.RecoveryKeys
import io.github.lenticualrstone.civilight.model.Timestamp
import io.github.lenticualrstone.civilight.model.app.RecoveryKeyMaterial
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.security.KeyStore
import java.util.Base64
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * Wraps the backup data key with a key that never leaves this device, so the daily backup needs no
 * typing (LIB-58 `ED`). Tests use an in-memory wrap.
 */
interface DeviceKeyWrap {
    val alias: String

    /** base64 of the wrapped [dataKey]. */
    fun wrap(dataKey: ByteArray): String

    /** The data key, or null when the device key is gone (reinstall, cleared credentials). */
    fun unwrap(wrapped: String): ByteArray?
}

/** [DeviceKeyWrap] over an AES-256-GCM key in the Android Keystore (`nonce || ciphertext`). */
class AndroidKeystoreWrap(override val alias: String = ALIAS) : DeviceKeyWrap {
    private fun key(): SecretKey {
        val store = KeyStore.getInstance(PROVIDER).apply { load(null) }
        (store.getKey(alias, null) as? SecretKey)?.let { return it }
        val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, PROVIDER)
        generator.init(
            KeyGenParameterSpec.Builder(alias, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .build(),
        )
        return generator.generateKey()
    }

    override fun wrap(dataKey: ByteArray): String {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, key())
        return Base64.getEncoder().encodeToString(cipher.iv + cipher.doFinal(dataKey))
    }

    override fun unwrap(wrapped: String): ByteArray? = runCatching {
        val bytes = Base64.getDecoder().decode(wrapped)
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.DECRYPT_MODE, key(), GCMParameterSpec(TAG_BITS, bytes.copyOfRange(0, NONCE_BYTES)))
        cipher.doFinal(bytes.copyOfRange(NONCE_BYTES, bytes.size))
    }.getOrNull()

    private companion object {
        const val PROVIDER = "AndroidKeyStore"
        const val ALIAS = "civilight.recovery"
        const val TRANSFORMATION = "AES/GCM/NoPadding"
        const val NONCE_BYTES = 12
        const val TAG_BITS = 128
    }
}

/**
 * The 복구 암호 on this device (LIB-58): set or change it (a new data key each time, §6 Q24), and give
 * the backup its key without typing. The password is a [CharArray] the caller zeroes; PBKDF2 runs
 * off the main thread.
 */
class RecoveryKeyring(
    private val book: BackupBooks,
    private val wrap: DeviceKeyWrap,
    private val iterations: Int = PackageCrypto.DEFAULT_ITERATIONS,
) {
    /** Creates the key, stores the material (never the password) and writes the book at once. */
    suspend fun set(password: CharArray, now: Timestamp): RecoveryKeyMaterial {
        val key = withContext(Dispatchers.Default) { RecoveryKeys.create(password, iterations) }
        val material = key.toMaterial(wrap.wrap(key.dataKey), wrap.alias, now)
        key.dataKey.fill(0)
        book.update { it.copy(recovery = material) }
        book.flush()
        return material
    }

    /** The key for today's backup, or null when none is set or the device key is gone. */
    suspend fun current(): RecoveryKey? {
        book.ensureLoaded()
        val material = book.state.value.recovery ?: return null
        val dataKey = wrap.unwrap(material.dataKeyWrappedByKeystore) ?: return null
        return RecoveryKeys.fromMaterial(material, dataKey)
    }
}
