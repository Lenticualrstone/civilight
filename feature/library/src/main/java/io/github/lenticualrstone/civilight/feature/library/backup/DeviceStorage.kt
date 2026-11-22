package io.github.lenticualrstone.civilight.feature.library.backup

import io.github.lenticualrstone.civilight.domain.library.common.TimeLabels
import io.github.lenticualrstone.civilight.store.StoreContainer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/** Free space on the app's volume and what the projects take (LIB-62 `기기 저장 공간`). */
data class StorageFacts(val freeBytes: Long, val projectBytes: Long) {
    /** `남은 공간 1.2 GB · 프로젝트 340 MB` (sizes as `domain:library` [TimeLabels.size] writes them). */
    val line: String
        get() = "${BackupCopy.STORAGE_FREE} ${TimeLabels.size(freeBytes)} · ${BackupCopy.STORAGE_PROJECTS} ${TimeLabels.size(projectBytes)}"
}

/** Reads [StorageFacts]; the app reads the files directory, tests a fixed value. */
fun interface DeviceStorage {
    suspend fun read(): StorageFacts
}

/**
 * [DeviceStorage] over the stores (`ED`): free = usable bytes of the volume that holds `files/`;
 * projects = every file of the live and the trashed projects' directories (with their history)
 * plus the blob store (images and recorded audio the projects use).
 */
internal class StoreDeviceStorage(private val stores: StoreContainer) : DeviceStorage {
    override suspend fun read(): StorageFacts = withContext(Dispatchers.IO) {
        StorageFacts(
            freeBytes = stores.filesDir.usableSpace,
            projectBytes = sizeOf(stores.library.projectsDir) + sizeOf(stores.library.trashedProjectsDir) + sizeOf(stores.blobs.dir),
        )
    }

    private fun sizeOf(dir: File): Long = dir.walkTopDown().filter { it.isFile }.sumOf { it.length() }
}
