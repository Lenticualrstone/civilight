package io.github.lenticualrstone.civilight.domain.checks

import io.github.lenticualrstone.civilight.domain.story.checks.AckSubject
import io.github.lenticualrstone.civilight.domain.story.checks.CheckEntry
import io.github.lenticualrstone.civilight.domain.story.checks.CheckFilter
import io.github.lenticualrstone.civilight.domain.story.checks.Marker
import io.github.lenticualrstone.civilight.domain.story.travel.TravelWarning
import io.github.lenticualrstone.civilight.model.Key

/** What a row button does; the screen routes it (the label is the copy). */
enum class FixKind {
    GO_TO_SCRIPT, MOVE_REVEAL, ACKNOWLEDGE, LINK_CANDIDATE, REJECT_CANDIDATE, SET_REVEAL,
    PICK_SPEAKER, TO_NARRATION, WRITE_VOICE, NEW_VOICE_ID,
    RELINK_RECORD, TRASH_RECORD, TRASH_ALL_RECORDS,
    BULK_REPLACE, RESTORE,
    DELAY_SCENE, USE_MODE, OPEN_BULK, FIX_SCALE,
    TO_AUTO, EDIT_TRANSITION, WRITE_DIRECT,
    HIDE_SCENE, UNLOCK, REVERT,
}

/**
 * One button of a row. [target] is the key the action works on (block, scene, record, term,
 * character, voice line, map or acknowledgement); [option] a choice option; [days] the delay of a
 * travel time chip; [modeKey] the travel mode of a mode chip.
 */
data class FixAction(
    val label: String,
    val kind: FixKind,
    val target: Key? = null,
    val option: Key? = null,
    val days: Int? = null,
    val modeKey: Key? = null,
)

/** The boxed span of [CheckResult.body] (search-style highlight of the matched words). */
data class Highlight(val start: Int, val end: Int)

/**
 * Rows of one type that the list folds into one row titled `<prefix> <n>` (`◇ 근거 지워진 변화 2`,
 * `◇ 형식과 다른 ID 3`, `잿불의 서약 · ▲ 3`), expanding to the member rows.
 */
data class CheckGroup(val id: String, val prefix: String) {
    fun title(count: Int): String = "$prefix $count"
}

/**
 * One computed check item (never stored). Subject and fingerprint follow the owning area's scheme,
 * so an [io.github.lenticualrstone.civilight.model.checks.Acknowledgement] written anywhere in the
 * app matches it.
 *
 * @property title marker and kind, e.g. `▲ 공개 전 사용`.
 * @property tag bold text after the kind (`하르트` on a travel row).
 * @property location right-aligned place (`1-4 씬 2`, `인물 · 리엔`, `휴지통`).
 * @property body the evidence line (quoted sentence, route, values).
 * @property detail the small line under it.
 * @property flag a dotted extra mark on the row (`◇ 근거 지워짐`).
 * @property storyOrder manuscript rank of the location; null sorts last.
 * @property lockedHidden concerns an item hidden while the project's hidden items are locked.
 * @property nestKey / [nestUnder] a `◇ 공개 전 후보` sub-row hangs under the `▲` row of its term.
 * @property cannotCheck `점검 못 함`: the pair could not be measured (`◇ 축척 없음`).
 * @property travel the card data of a travel `▲` (STO-30 `TravelCheck` card).
 */
data class CheckResult(
    override val checkType: String,
    override val subjectKeys: List<Key>,
    override val fingerprint: String,
    val title: String,
    val location: String? = null,
    val tag: String? = null,
    val body: String? = null,
    val highlight: Highlight? = null,
    val detail: String? = null,
    val flag: String? = null,
    val actions: List<FixAction> = emptyList(),
    val storyOrder: Long? = null,
    val lockedHidden: Boolean = false,
    val group: CheckGroup? = null,
    val nestKey: String? = null,
    val nestUnder: String? = null,
    val cannotCheck: Boolean = false,
    val travel: TravelWarning? = null,
) : AckSubject {
    val definition: CheckDefinition get() = CheckRegistry.of(checkType)
    val marker: Marker get() = definition.marker
    val filter: CheckFilter get() = definition.filter
    val ackable: Boolean get() = definition.ackable

    /** Stable identity of the item within one evaluation. */
    val id: String get() = "$checkType:${subjectKeys.joinToString(",")}:$fingerprint"

    /** The registry-level entry `domain:story`'s `CheckCenter` counts, orders and collapses. */
    fun entry(): CheckEntry = CheckEntry(
        checkType = checkType,
        subjectKeys = subjectKeys,
        fingerprint = fingerprint,
        title = title,
        location = location.orEmpty(),
        storyOrder = storyOrder,
        lockedHidden = lockedHidden,
    )

    /** The row without its location as one line: `◇ 지운 인물 · 노아 · 3일 남음 · 대사 14 · …` (tests, accessibility text). */
    val line: String get() = CheckCopy.join(title, tag, body, detail)
}
