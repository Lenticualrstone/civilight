package io.github.lenticualrstone.civilight.store.io

import java.io.File
import java.io.OutputStream

/**
 * Test seam of the write path: lets a test stop a write at a chosen point, exactly as a process
 * kill would. Production code uses [None].
 */
open class WriteFaults {
    /** Writes the content of a temp file for [target]. */
    open fun write(target: File, out: OutputStream, bytes: ByteArray) = out.write(bytes)

    /** Called after every temp file of a transaction is written and synced, before the journal. */
    open fun beforeJournal() = Unit

    /** Called right before the temp of [target] is renamed onto it. */
    open fun beforeRename(target: File) = Unit

    /** Called after a [target] was written or deleted (write log for tests). */
    open fun committed(target: File, deleted: Boolean) = Unit

    object None : WriteFaults()
}

/** Thrown by a [WriteFaults] to stand for a process kill: nothing is cleaned up. */
class SimulatedCrash(message: String) : RuntimeException(message)
