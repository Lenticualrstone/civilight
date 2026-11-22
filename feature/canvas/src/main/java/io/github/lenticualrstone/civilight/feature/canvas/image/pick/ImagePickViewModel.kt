package io.github.lenticualrstone.civilight.feature.canvas.image.pick

import android.content.ContentResolver
import android.net.Uri
import androidx.lifecycle.viewModelScope
import io.github.lenticualrstone.civilight.domain.cast.EditContext
import io.github.lenticualrstone.civilight.feature.canvas.common.CanvasViewModel
import io.github.lenticualrstone.civilight.feature.canvas.image.ImageCopy
import io.github.lenticualrstone.civilight.feature.canvas.image.ImageEdits
import io.github.lenticualrstone.civilight.feature.canvas.image.ImageOwner
import io.github.lenticualrstone.civilight.model.Key
import io.github.lenticualrstone.civilight.model.asset.BlobSource
import io.github.lenticualrstone.civilight.store.blob.BlobStore
import io.github.lenticualrstone.civilight.store.blob.import
import io.github.lenticualrstone.civilight.ui.project.EditResult
import io.github.lenticualrstone.civilight.ui.project.ProjectSessions
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

/** Result of `사진` / `파일`. */
sealed interface ImportResult {
    data class Imported(val blobHash: String) : ImportResult

    data object ReadOnly : ImportResult

    /** Unreadable or not an image (copy missing, Q-EM-01). */
    data object Failed : ImportResult
}

/**
 * The picker's view model (CNV-27), one per owner × slot in the hosting entry (cast's profile opens two
 * slots from one screen). Writes: a preset into the slot (`고르기`), or an imported original into
 * `내 이미지` (`사진` / `파일`); the crop editor places an original.
 */
class ImagePickViewModel(
    projectKey: Key,
    sessions: ProjectSessions,
    contextOf: () -> EditContext,
    private val blobs: BlobStore,
) : CanvasViewModel(projectKey, sessions, contextOf) {
    fun placePreset(owner: ImageOwner, presetId: String, holo: Boolean, then: (EditResult) -> Unit) =
        act(ImageCopy.BTN_PICK, then = then) { doc, ctx -> ImageEdits.placePreset(doc, owner, presetId, holo, ctx) }

    /**
     * Copies the picked image into the blob store (stored once by content hash) and lists it under
     * `내 이미지`, as one undo step named by the button. A read-only project imports nothing.
     */
    fun import(resolver: ContentResolver, uri: Uri, source: BlobSource, then: (ImportResult) -> Unit) {
        viewModelScope.launch {
            if (isReadOnly) return@launch then(ImportResult.ReadOnly)
            val blob = try {
                blobs.import(resolver, uri, source)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                null
            }
            if (blob == null || !blob.mime.startsWith("image/")) return@launch then(ImportResult.Failed)
            val label = if (source == BlobSource.FILE) ImageCopy.BTN_FILE else ImageCopy.BTN_PHOTO
            when (apply(label) { doc, _ -> ImageEdits.addBlob(doc, blob) }) {
                EditResult.APPLIED -> then(ImportResult.Imported(blob.hash))
                EditResult.READ_ONLY -> then(ImportResult.ReadOnly)
            }
        }
    }

    companion object {
        fun key(owner: ImageOwner): String = "image-pick-${owner.kind}-${owner.key}-${owner.slot}"
    }
}
