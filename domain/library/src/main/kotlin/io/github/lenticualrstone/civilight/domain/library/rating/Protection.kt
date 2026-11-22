package io.github.lenticualrstone.civilight.domain.library.rating

import io.github.lenticualrstone.civilight.model.app.RatingDefaults
import io.github.lenticualrstone.civilight.model.project.Protections
import io.github.lenticualrstone.civilight.model.project.Rating

/**
 * The four per-project protections (LIB-23). [HIDDEN] is a state that changes only by an explicit
 * choice; the other three are presentation values read live.
 */
enum class Protection { HIDDEN, CAPTURE_BLOCK, NAME_MASK, RECORDER_LINK_MASK }

fun Protections.isOn(p: Protection): Boolean = when (p) {
    Protection.HIDDEN -> hidden
    Protection.CAPTURE_BLOCK -> captureBlock
    Protection.NAME_MASK -> nameMask
    Protection.RECORDER_LINK_MASK -> recorderLinkMask
}

fun Protections.with(p: Protection, on: Boolean): Protections = when (p) {
    Protection.HIDDEN -> copy(hidden = on)
    Protection.CAPTURE_BLOCK -> copy(captureBlock = on)
    Protection.NAME_MASK -> copy(nameMask = on)
    Protection.RECORDER_LINK_MASK -> copy(recorderLinkMask = on)
}

/** The protections that are on. */
val Protections.onSet: Set<Protection> get() = Protection.entries.filter { isOn(it) }.toSet()

/** These protections with exactly [on] switched on (the `ext` slot is kept). */
fun Protections.withExactly(on: Set<Protection>): Protections = Protection.entries.fold(this) { acc, p -> acc.with(p, p in on) }

/**
 * Rating defaults table (LIB-23, R19 §2). R-18 values come from 설정 · 수위 등급; R-15 has only an
 * editable capture default; ALL is all off. The rating itself changes no behaviour (LIB-22).
 */
object RatingDefaultsTable {
    fun defaultsFor(rating: Rating, defaults: RatingDefaults): Protections = when (rating) {
        Rating.R18 -> Protections(
            hidden = defaults.r18Hide,
            captureBlock = defaults.r18Capture,
            nameMask = defaults.r18NameMask,
            recorderLinkMask = defaults.r18RecorderMask,
        )
        Rating.R15 -> Protections(captureBlock = defaults.r15Capture)
        Rating.ALL -> Protections()
    }

    /** Whether [p] can be changed in 설정 · 수위 등급 for [rating] (fixed: R-15 hide/name/recorder, ALL all). */
    fun isEditable(rating: Rating, p: Protection): Boolean = when (rating) {
        Rating.R18 -> true
        Rating.R15 -> p == Protection.CAPTURE_BLOCK
        Rating.ALL -> false
    }

    /** [defaults] with [p] of [rating] set to [on]; fixed defaults cannot change. */
    fun change(defaults: RatingDefaults, rating: Rating, p: Protection, on: Boolean): RatingDefaults {
        require(isEditable(rating, p)) { "fixed default: $rating $p" }
        val changed = when (rating) {
            Rating.R18 -> when (p) {
                Protection.HIDDEN -> defaults.copy(r18Hide = on)
                Protection.CAPTURE_BLOCK -> defaults.copy(r18Capture = on)
                Protection.NAME_MASK -> defaults.copy(r18NameMask = on)
                Protection.RECORDER_LINK_MASK -> defaults.copy(r18RecorderMask = on)
            }
            Rating.R15 -> defaults.copy(r15Capture = on)
            Rating.ALL -> defaults
        }
        return if (changed == defaults) defaults else changed.copy(changedGroups = changed.changedGroups + serial(rating))
    }

    private fun serial(rating: Rating): String = io.github.lenticualrstone.civilight.model.OpenEnum.serialNameOf(rating.name)
}
