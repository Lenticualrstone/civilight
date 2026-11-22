package io.github.lenticualrstone.civilight.domain.checks

import io.github.lenticualrstone.civilight.domain.story.checks.CheckCenter
import io.github.lenticualrstone.civilight.domain.story.checks.CheckFilter
import io.github.lenticualrstone.civilight.domain.story.checks.Marker
import io.github.lenticualrstone.civilight.model.checks.AckKind
import io.github.lenticualrstone.civilight.model.checks.CheckType
import io.github.lenticualrstone.civilight.model.doc.ProjectDoc

/** The area that computes a check type (STO-40 "Emitted by"). */
enum class CheckArea { WORLD, SCRIPT, CAST, TRASH, STORY, HIDING }

/**
 * One row of the STO-40 registry: id, marker (`▲` warn · `◇` info), filter chip, how it is
 * acknowledged and the buttons its rows carry. Marker and filter come from `domain:story`'s
 * [CheckCenter], which owns the registry semantics.
 */
data class CheckDefinition(
    val number: Int,
    val checkType: String,
    /** Kind title with its marker, e.g. `▲ 공개 전 사용`; rows with a variable title start with it. */
    val title: String,
    val buttons: List<String>,
    val area: CheckArea,
    /** Switched off by `이동 점검` (rows 10–12, 14) or `장소 없는 씬 알림` (row 13) (STO-42). */
    val setting: CheckSetting? = null,
) {
    val marker: Marker get() = CheckCenter.markerOf(checkType)
    val filter: CheckFilter get() = CheckCenter.filterOf(checkType)

    /** `맞음` (match) or `이대로 두기` (keep); null when the item cannot be acknowledged. */
    val ackKind: AckKind? get() = CheckType.ACKABLE_AS[checkType]
    val ackable: Boolean get() = ackKind != null

    /** The check function of this row: its items in [doc] (open and acknowledged). */
    fun compute(doc: ProjectDoc, inputs: CheckInputs): List<CheckResult> = ProjectChecks.check(doc, inputs, checkType)
}

/** App settings rows of STO-42 that switch registry rows off. */
enum class CheckSetting { TRAVEL, NO_PLACE }

/** The registry of every check type (STO-40, 00-domain-model §2.9), in registry order #1 … #20. */
object CheckRegistry {
    val all: List<CheckDefinition> = listOf(
        CheckDefinition(1, CheckType.TERM_BEFORE_REVEAL, CheckCopy.TERM_BEFORE_REVEAL, listOf(CheckCopy.GO_TO_SCRIPT, CheckCopy.MOVE_REVEAL, CheckCopy.MATCH), CheckArea.WORLD),
        CheckDefinition(2, CheckType.TERM_CANDIDATE_BEFORE_REVEAL, CheckCopy.TERM_CANDIDATE, listOf(CheckCopy.GO_TO_SCRIPT, CheckCopy.LINK, CheckCopy.REJECT), CheckArea.WORLD),
        CheckDefinition(3, CheckType.TERM_NO_REVEAL, CheckCopy.TERM_NO_REVEAL, listOf(CheckCopy.GO_TO_SCRIPT), CheckArea.WORLD),
        CheckDefinition(4, CheckType.SPEAKER_MISSING, CheckCopy.SPEAKER_MISSING, listOf(CheckCopy.PICK_SPEAKER, CheckCopy.TO_NARRATION), CheckArea.SCRIPT),
        CheckDefinition(5, CheckType.VOICE_EMPTY, CheckCopy.VOICE_EMPTY, listOf(CheckCopy.WRITE_VOICE), CheckArea.CAST),
        CheckDefinition(6, CheckType.VOICE_FORMAT_DIFFERS, CheckCopy.VOICE_FORMAT, listOf(CheckCopy.NEW_VOICE_ID, CheckCopy.AS_IS), CheckArea.SCRIPT),
        CheckDefinition(7, CheckType.VOICE_DUPLICATE, "▲ 보이스 ID 겹침", listOf(CheckCopy.GO_TO_SCRIPT), CheckArea.SCRIPT),
        CheckDefinition(8, CheckType.CHANGE_ANCHOR_DELETED, CheckCopy.ANCHOR_DELETED, listOf(CheckCopy.RELINK_RECORD, CheckCopy.KEEP, CheckCopy.TRASH_RECORD), CheckArea.SCRIPT),
        CheckDefinition(9, CheckType.DELETED_ITEM, "◇ 지운", listOf(CheckCopy.RESTORE, CheckCopy.KEEP), CheckArea.TRASH),
        CheckDefinition(10, CheckType.TRAVEL_SHORTAGE, "▲ 이동 시간 부족", listOf(CheckCopy.MATCH), CheckArea.STORY, CheckSetting.TRAVEL),
        CheckDefinition(11, CheckType.TRAVEL_SAME_TIME_TWO_PLACES, "▲ 같은 때 두 장소", listOf(CheckCopy.MATCH), CheckArea.STORY, CheckSetting.TRAVEL),
        CheckDefinition(12, CheckType.TRAVEL_SIMULTANEOUS, "▲ 동시에 두 장소", listOf(CheckCopy.MATCH), CheckArea.STORY, CheckSetting.TRAVEL),
        CheckDefinition(13, CheckType.TRAVEL_NO_PLACE, "◇ 장소 없음", listOf(CheckCopy.OPEN_BULK), CheckArea.STORY, CheckSetting.NO_PLACE),
        CheckDefinition(14, CheckType.TRAVEL_NO_SCALE, CheckCopy.NO_SCALE, listOf(CheckCopy.FIX_SCALE), CheckArea.STORY, CheckSetting.TRAVEL),
        CheckDefinition(15, CheckType.TIME_CLOCK_BAND_MISMATCH, CheckCopy.CLOCK_BAND, listOf(CheckCopy.MATCH), CheckArea.STORY),
        CheckDefinition(16, CheckType.TIME_ORDER, CheckCopy.ORDER, listOf(CheckCopy.GO_TO_SCRIPT, CheckCopy.MATCH), CheckArea.STORY),
        CheckDefinition(17, CheckType.TRANSITION_STALE, "◇ 전환 문구 · 쓴 뒤 때 바뀜", listOf(CheckCopy.TO_AUTO, CheckCopy.EDIT_TRANSITION, CheckCopy.MATCH), CheckArea.STORY),
        CheckDefinition(18, CheckType.TRANSITION_PLACE_BEFORE_REVEAL, "◇ 공개 전 장소 · 전환 문구", listOf(CheckCopy.WRITE_DIRECT, CheckCopy.MATCH), CheckArea.STORY),
        CheckDefinition(19, CheckType.HIDDEN_CHARACTER_IN_VISIBLE_SCENE, CheckCopy.HIDDEN_CHARACTER, listOf(CheckCopy.HIDE_SCENE, CheckCopy.AS_IS), CheckArea.HIDING),
        CheckDefinition(20, CheckType.HIDDEN_LOCKED_GROUP, "▲ 숨긴 항목 관련", listOf(CheckCopy.UNLOCK), CheckArea.HIDING),
    )

    private val byType: Map<String, CheckDefinition> = all.associateBy { it.checkType }

    fun of(checkType: String): CheckDefinition =
        byType[checkType] ?: throw IllegalArgumentException("Unknown check type $checkType")

    /**
     * The acknowledgement button of a type: `이대로 두기` for keep types, `그대로` for the voice-ID
     * and hidden-character rows, `맞음` otherwise; null when not ackable.
     */
    fun ackLabel(checkType: String): String? = when (CheckType.ACKABLE_AS[checkType]) {
        null -> null
        AckKind.KEEP -> CheckCopy.KEEP
        AckKind.MATCH -> if (checkType == CheckType.VOICE_FORMAT_DIFFERS || checkType == CheckType.HIDDEN_CHARACTER_IN_VISIBLE_SCENE) {
            CheckCopy.AS_IS
        } else {
            CheckCopy.MATCH
        }
    }
}
