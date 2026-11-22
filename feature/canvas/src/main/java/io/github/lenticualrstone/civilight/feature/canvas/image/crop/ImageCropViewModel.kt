package io.github.lenticualrstone.civilight.feature.canvas.image.crop

import io.github.lenticualrstone.civilight.domain.cast.EditContext
import io.github.lenticualrstone.civilight.feature.canvas.common.CanvasViewModel
import io.github.lenticualrstone.civilight.feature.canvas.image.CropSession
import io.github.lenticualrstone.civilight.feature.canvas.image.ImageCopy
import io.github.lenticualrstone.civilight.feature.canvas.image.ImageEdits
import io.github.lenticualrstone.civilight.feature.canvas.image.ImageOwner
import io.github.lenticualrstone.civilight.model.Key
import io.github.lenticualrstone.civilight.ui.project.EditResult
import io.github.lenticualrstone.civilight.ui.project.ProjectSessions

/**
 * The crop editor's view model (CNV-28): nothing is written while editing; `적용` places the original in
 * the slot with the crops of the edited ratios and the holo flag, as one undo step named `적용`. The
 * original file is never touched (CNV-29).
 */
class ImageCropViewModel(
    projectKey: Key,
    sessions: ProjectSessions,
    contextOf: () -> EditContext,
) : CanvasViewModel(projectKey, sessions, contextOf) {
    fun apply(owner: ImageOwner, blobHash: String, session: CropSession, then: (EditResult) -> Unit) =
        act(ImageCopy.BTN_APPLY, then = then) { doc, ctx ->
            ImageEdits.placeBlob(doc, owner, blobHash, session.crops(), session.holo, ctx)
        }
}
