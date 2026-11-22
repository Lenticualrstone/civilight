package io.github.lenticualrstone.civilight.store

import io.github.lenticualrstone.civilight.model.Keys
import io.github.lenticualrstone.civilight.store.io.AtomicFiles
import java.io.File

/**
 * The random id of this installation (§1.3 `updatedBy`, FMT §7): made once, kept in [file], never
 * changed. The id is a ULID and carries nothing about the device or the user.
 */
class DeviceId private constructor(private val file: File?, private val fixed: String?) {
    /** The id of this installation. Reads or creates [file] on first use. */
    val value: String by lazy { fixed ?: readOrCreate(requireNotNull(file)) }

    override fun toString(): String = value

    companion object {
        /** The id stored in [file] (created when missing or unreadable). */
        operator fun invoke(file: File): DeviceId = DeviceId(file, null)

        /** A fixed id (tests, previews). */
        fun fixed(id: String): DeviceId = DeviceId(null, id)

        private fun readOrCreate(file: File): String {
            val existing = runCatching { file.readText().trim() }.getOrNull()
            if (existing != null && existing.isNotEmpty()) return existing
            val id = Keys.newKey()
            AtomicFiles.write(file, id.toByteArray(Charsets.UTF_8))
            return id
        }
    }
}
