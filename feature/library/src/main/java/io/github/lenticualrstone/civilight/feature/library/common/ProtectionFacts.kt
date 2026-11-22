package io.github.lenticualrstone.civilight.feature.library.common

import io.github.lenticualrstone.civilight.model.Key

/**
 * Facts the rating sheet (LIB-24) and step 1 (LIB-09) need from parts of this lane built in later
 * batches: whether a 복구 암호 exists (LIB-58) and how many unencrypted Drive versions of a project
 * exist (LIB-57). Swap [NoBackupYet] for the real source when those land.
 */
interface ProtectionFacts {
    fun recoverySet(): Boolean

    fun unencryptedBackups(projectKey: Key): Int
}

/** No 복구 암호 can be set and no Drive backup exists yet. */
object NoBackupYet : ProtectionFacts {
    override fun recoverySet(): Boolean = false

    override fun unencryptedBackups(projectKey: Key): Int = 0
}
