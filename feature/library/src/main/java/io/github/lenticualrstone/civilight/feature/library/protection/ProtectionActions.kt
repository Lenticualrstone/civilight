package io.github.lenticualrstone.civilight.feature.library.protection

import io.github.lenticualrstone.civilight.domain.library.rating.Protection
import io.github.lenticualrstone.civilight.domain.library.rating.ProtectionSwitch
import io.github.lenticualrstone.civilight.domain.library.rating.RatingChange
import io.github.lenticualrstone.civilight.domain.library.rating.RatingChangeOutcome
import io.github.lenticualrstone.civilight.domain.library.rating.with
import io.github.lenticualrstone.civilight.model.Key
import io.github.lenticualrstone.civilight.model.open
import io.github.lenticualrstone.civilight.model.project.Project
import io.github.lenticualrstone.civilight.model.project.Protections
import io.github.lenticualrstone.civilight.ui.project.EditResult

/** What became of a protection change the author asked for. */
enum class ProtectionOutcome {
    APPLIED,

    /** Authentication was cancelled or failed: nothing changed and no toast (LIB-37). */
    CANCELLED,

    /** The project is read-only (the example, LIB-17). */
    READ_ONLY,

    /**
     * Hiding was asked for on a device without a secure lock screen: nothing changed. A hidden
     * project could never be opened again there (§6 Q19 `ED`: hiding needs a device credential).
     */
    NO_DEVICE_LOCK,
}

/** The outside world a protection change touches: one project write, one prompt, the recents list. */
interface ProtectionPorts {
    /** One undo step named [label] on [projectKey]'s `project.json`. */
    suspend fun write(projectKey: Key, label: String, transform: (Project) -> Project): EditResult

    /** The LIB-32 prompt (`숨긴 프로젝트 보기`); true only on success. */
    suspend fun authenticate(): Boolean

    /** 최근 연 파일 of a project that became hidden go at once (LIB-05). */
    suspend fun forgetRecents(projectKey: Key)

    /** Whether the device has a secure lock screen (PIN, pattern, password), LIB-37 / §6 Q19. */
    fun deviceSecure(): Boolean = true

    /** "지금은 열린 채" (R19 §2): a project that just became hidden stays open until the next re-lock. */
    fun keepOpen(projectKey: Key) = Unit
}

/**
 * Every way a project's protections change (LIB-24, LIB-25, LIB-31) goes through here, so the
 * authentication rule is in one place: turning `프로젝트 숨기기` off needs a fresh prompt (except
 * `숨김 풀기` inside the already open group); turning it on never does.
 */
class ProtectionActions(private val ports: ProtectionPorts) {
    /** ⋯ `프로젝트 숨기기` (LIB-31): immediate, no prompt; refused without a device lock (§6 Q19). */
    suspend fun hide(projectKey: Key): ProtectionOutcome {
        if (!ports.deviceSecure()) return ProtectionOutcome.NO_DEVICE_LOCK
        return write(projectKey, ProtectionCopy.HIDE, becameHidden = true) {
            it.copy(protections = it.protections.copy(hidden = true))
        }
    }

    /** `숨김 풀기` on a row of the open group (LIB-31): the group was opened with a prompt already. */
    suspend fun unhideInOpenGroup(projectKey: Key): ProtectionOutcome = write(projectKey, ProtectionCopy.UNHIDE, becameHidden = false) {
        it.copy(protections = it.protections.copy(hidden = false))
    }

    /** One switch of 프로젝트 설정 › 등급 · 숨기기 (LIB-25); never changes the rating. */
    suspend fun switch(projectKey: Key, before: Protections, protection: Protection, on: Boolean): ProtectionOutcome {
        val change = ProtectionSwitch(protection, before, on)
        if (change.after == before) return ProtectionOutcome.APPLIED
        if (!before.hidden && change.after.hidden && !ports.deviceSecure()) return ProtectionOutcome.NO_DEVICE_LOCK
        if (change.requiresAuth && !ports.authenticate()) return ProtectionOutcome.CANCELLED
        return write(projectKey, labelOf(protection), becameHidden = !before.hidden && change.after.hidden) {
            it.copy(protections = it.protections.with(protection, on))
        }
    }

    /**
     * `바꾸기` of the `바뀌는 것` sheet (LIB-24): rating := to, protections := checked rows. When hiding
     * goes off the prompt runs first; cancel leaves everything as it was and the sheet open.
     */
    suspend fun changeRating(projectKey: Key, change: RatingChange): ProtectionOutcome {
        if (change.joinsHiddenGroup && !ports.deviceSecure()) return ProtectionOutcome.NO_DEVICE_LOCK
        val authenticated = if (change.requiresAuth) ports.authenticate() else false
        return when (val outcome = change.complete(authenticated)) {
            is RatingChangeOutcome.Unchanged -> ProtectionOutcome.CANCELLED
            is RatingChangeOutcome.Applied -> write(projectKey, ProtectionCopy.RATING, becameHidden = change.joinsHiddenGroup) {
                it.copy(rating = outcome.rating.open(), protections = outcome.protections.copy(ext = it.protections.ext))
            }
        }
    }

    /**
     * `같이 바꾸기` (LIB-23): each (project, before, after). Turning hiding off anywhere asks once
     * first, as every other path does; a cancelled prompt changes no project.
     */
    suspend fun applyDefaultsAll(changes: List<Triple<Key, Protections, Protections>>): ProtectionOutcome {
        val real = changes.filter { (_, before, after) -> before != after }
        if (real.any { (_, before, after) -> !before.hidden && after.hidden } && !ports.deviceSecure()) return ProtectionOutcome.NO_DEVICE_LOCK
        if (real.any { (_, before, after) -> before.hidden && !after.hidden } && !ports.authenticate()) return ProtectionOutcome.CANCELLED
        var outcome = ProtectionOutcome.APPLIED
        for ((key, before, after) in real) {
            val one = write(key, ProtectionCopy.RATING, becameHidden = !before.hidden && after.hidden) {
                it.copy(protections = after.copy(ext = it.protections.ext))
            }
            if (one == ProtectionOutcome.READ_ONLY) outcome = one
        }
        return outcome
    }

    private suspend fun write(projectKey: Key, label: String, becameHidden: Boolean, transform: (Project) -> Project): ProtectionOutcome {
        val result = ports.write(projectKey, label, transform)
        if (result == EditResult.READ_ONLY) return ProtectionOutcome.READ_ONLY
        if (becameHidden) {
            ports.keepOpen(projectKey)
            ports.forgetRecents(projectKey)
        }
        return ProtectionOutcome.APPLIED
    }

    companion object {
        fun labelOf(protection: Protection): String = when (protection) {
            Protection.HIDDEN -> ProtectionCopy.HIDE_SWITCH
            Protection.CAPTURE_BLOCK -> ProtectionCopy.CAPTURE_BLOCK
            Protection.NAME_MASK -> ProtectionCopy.NAME_MASK
            Protection.RECORDER_LINK_MASK -> ProtectionCopy.RECORDER_LINK_MASK
        }
    }
}
