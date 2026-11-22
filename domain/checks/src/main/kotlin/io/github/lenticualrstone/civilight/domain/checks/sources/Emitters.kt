package io.github.lenticualrstone.civilight.domain.checks.sources

import io.github.lenticualrstone.civilight.domain.checks.CheckResult
import io.github.lenticualrstone.civilight.model.checks.CheckType

/** The pure function of each registry row: `ProjectDoc` (in a [CheckScope]) to its items. */
internal object Emitters {
    private val table: Map<String, (CheckScope) -> List<CheckResult>> = mapOf(
        CheckType.TERM_BEFORE_REVEAL to { s -> s.terms.ofType(CheckType.TERM_BEFORE_REVEAL) },
        CheckType.TERM_CANDIDATE_BEFORE_REVEAL to { s -> s.terms.ofType(CheckType.TERM_CANDIDATE_BEFORE_REVEAL) },
        CheckType.TERM_NO_REVEAL to { s -> s.terms.ofType(CheckType.TERM_NO_REVEAL) },
        CheckType.SPEAKER_MISSING to ScriptSource::speakerMissing,
        CheckType.VOICE_EMPTY to CastSource::voiceEmpty,
        CheckType.VOICE_FORMAT_DIFFERS to ScriptSource::voiceFormatDiffers,
        CheckType.VOICE_DUPLICATE to ScriptSource::voiceDuplicates,
        CheckType.CHANGE_ANCHOR_DELETED to ScriptSource::anchorDeleted,
        CheckType.DELETED_ITEM to TrashSource::deletedItems,
        CheckType.TRAVEL_SHORTAGE to { s -> TravelSource.warnings(s, CheckType.TRAVEL_SHORTAGE) },
        CheckType.TRAVEL_SAME_TIME_TWO_PLACES to { s -> TravelSource.warnings(s, CheckType.TRAVEL_SAME_TIME_TWO_PLACES) },
        CheckType.TRAVEL_SIMULTANEOUS to { s -> TravelSource.warnings(s, CheckType.TRAVEL_SIMULTANEOUS) },
        CheckType.TRAVEL_NO_PLACE to TravelSource::noPlace,
        CheckType.TRAVEL_NO_SCALE to TravelSource::noScale,
        CheckType.TIME_CLOCK_BAND_MISMATCH to TimeSource::clockBand,
        CheckType.TIME_ORDER to TimeSource::order,
        CheckType.TRANSITION_STALE to TimeSource::stale,
        CheckType.TRANSITION_PLACE_BEFORE_REVEAL to TimeSource::placesBeforeReveal,
        CheckType.HIDDEN_CHARACTER_IN_VISIBLE_SCENE to CastSource::hiddenInVisibleScenes,
        // The locked aggregate is drawn by the list from `lockedHidden` items; it has no items of its own.
        CheckType.HIDDEN_LOCKED_GROUP to { _ -> emptyList() },
    )

    fun emit(scope: CheckScope, checkType: String): List<CheckResult> =
        table[checkType]?.invoke(scope) ?: throw IllegalArgumentException("Unknown check type $checkType")
}
