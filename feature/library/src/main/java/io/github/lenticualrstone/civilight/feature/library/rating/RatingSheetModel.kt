package io.github.lenticualrstone.civilight.feature.library.rating

import io.github.lenticualrstone.civilight.domain.library.rating.Protection
import io.github.lenticualrstone.civilight.domain.library.rating.RatingLabels
import io.github.lenticualrstone.civilight.domain.library.rating.RatingSheet
import io.github.lenticualrstone.civilight.domain.library.rating.RatingSheets
import io.github.lenticualrstone.civilight.domain.recorder.hooks.RecorderHooks
import io.github.lenticualrstone.civilight.feature.library.common.ProtectionFacts
import io.github.lenticualrstone.civilight.feature.library.protection.ProtectionOutcome
import io.github.lenticualrstone.civilight.model.project.Protections
import io.github.lenticualrstone.civilight.model.app.RatingDefaults
import io.github.lenticualrstone.civilight.model.doc.ProjectDoc
import io.github.lenticualrstone.civilight.model.project.Rating
import io.github.lenticualrstone.civilight.model.recorder.RecorderLibrary

/** Inputs of the `바뀌는 것` sheet (LIB-24) gathered from the project, settings, recorder and backups. */
object RatingSheetModel {
    /**
     * The sheet for changing [doc]'s rating to [to], built fresh from the current values (R19-07), or
     * null when the rating does not change. `기록 <n>` counts recorder records linked to the project.
     */
    fun build(doc: ProjectDoc, to: Rating, defaults: RatingDefaults, facts: ProtectionFacts, recorder: RecorderLibrary): RatingSheet? =
        RatingSheets.build(
            projectName = doc.project.name,
            from = RatingLabels.effective(doc.project.rating),
            to = to,
            current = doc.project.protections,
            defaults = defaults,
            recoverySet = facts.recoverySet(),
            unencryptedBackups = facts.unencryptedBackups(doc.key),
            linkedRecords = RecorderHooks.linkedRecordCount(recorder, doc.key),
        )

    /** The sheet as built when it opened; [sheet] null = the rating would not change (R19-09). */
    class Built(val sheet: RatingSheet?)

    /** A project the hidden group hides while it is locked: its sheet may not stay on screen. */
    fun lockedAway(protections: Protections, hiddenGroupOpen: Boolean): Boolean = protections.hidden && !hiddenGroupOpen

    /**
     * After `바꾸기`: applied or refused as read-only closes; a cancelled prompt keeps it open (LIB-24),
     * and so does a refusal for want of a device lock (nothing changed, §6 Q19).
     */
    fun closesAfter(outcome: ProtectionOutcome): Boolean = outcome != ProtectionOutcome.CANCELLED && outcome != ProtectionOutcome.NO_DEVICE_LOCK

    /** Closes a sheet at most once, whichever path (button, write, re-lock) gets there first. */
    class CloseOnce(private val onClose: () -> Unit) {
        var closed = false
            private set

        fun close() {
            if (closed) return
            closed = true
            onClose()
        }
    }

    /** A row's checkbox flipped. */
    fun toggle(checked: Set<Protection>, protection: Protection): Set<Protection> =
        if (protection in checked) checked - protection else checked + protection
}
