package io.github.lenticualrstone.civilight.domain.story.checks

import io.github.lenticualrstone.civilight.model.Key
import io.github.lenticualrstone.civilight.model.checks.CheckType
import io.github.lenticualrstone.civilight.model.doc.ProjectDoc

/** `▲` warning (`warn`) or `◇` notice (`ink3`). */
enum class Marker(val glyph: String) { WARNING("▲"), NOTICE("◇") }

/** Filter chips of 점검 센터 in order (STO-39); `맞다고 표시한 것` lists acknowledged items. */
enum class CheckFilter(val word: String) {
    ALL("전체"), TERM("용어 공개"), SPEAKER("화자"), TRAVEL("이동"), VOICE("보이스"), CHANGE("변화"), DELETED("지운 항목"), ACKNOWLEDGED("맞다고 표시한 것"),
}

/**
 * One computed check item from any area (results are never stored). [storyOrder] sorts rows within a
 * marker (null last); [lockedHidden] = it concerns an item hidden while locked (collapsed, R19 §6).
 */
data class CheckEntry(
    override val checkType: String,
    override val subjectKeys: List<Key>,
    override val fingerprint: String,
    val title: String,
    val location: String,
    val storyOrder: Long? = null,
    val lockedHidden: Boolean = false,
) : AckSubject {
    val marker: Marker get() = CheckCenter.markerOf(checkType)
    val filter: CheckFilter get() = CheckCenter.filterOf(checkType)
}

/** A row of the list: an item, or the collapsed `▲ 숨긴 항목 관련 2 · 지문으로 보기`. */
sealed interface CheckRow {
    data class Item(val entry: CheckEntry) : CheckRow
    data class HiddenGroup(val count: Int) : CheckRow {
        val text: String get() = "▲ 숨긴 항목 관련 $count · 지문으로 보기"
        val action: String get() = "지문으로 보기"
    }
}

/** `어긋난 곳 없음` (`EmptyCheck`). */
data class EmptyCheck(val lastChecked: String, val range: String?, val acknowledgedCount: Int) {
    val title: String get() = "어긋난 곳 없음"

    /** `마지막 확인 · 방금 · 1-1 ~ 1-7`. */
    val line: String get() = listOfNotNull("마지막 확인", lastChecked, range).joinToString(" · ")
    val buttons: List<String> get() = listOf("원고로", "맞다고 표시한 것 $acknowledgedCount")
}

/**
 * Registry semantics of 점검 센터 (STO-39 … STO-41): marker and filter per type, open vs acknowledged,
 * counts, order (▲ before ◇, then story order) and the locked hidden collapse. The screen shell is
 * CNV-17's.
 */
object CheckCenter {
    fun markerOf(checkType: String): Marker = when (checkType) {
        CheckType.TERM_BEFORE_REVEAL, CheckType.SPEAKER_MISSING, CheckType.VOICE_EMPTY, CheckType.VOICE_DUPLICATE,
        CheckType.TRAVEL_SHORTAGE, CheckType.TRAVEL_SAME_TIME_TWO_PLACES, CheckType.TRAVEL_SIMULTANEOUS,
        CheckType.HIDDEN_LOCKED_GROUP -> Marker.WARNING
        else -> Marker.NOTICE
    }

    /** Filter per registry row; time and transition rows go to `이동` (`ED`, Q25). */
    fun filterOf(checkType: String): CheckFilter = when (checkType) {
        CheckType.TERM_BEFORE_REVEAL, CheckType.TERM_CANDIDATE_BEFORE_REVEAL, CheckType.TERM_NO_REVEAL -> CheckFilter.TERM
        CheckType.SPEAKER_MISSING, CheckType.HIDDEN_CHARACTER_IN_VISIBLE_SCENE -> CheckFilter.SPEAKER
        CheckType.VOICE_EMPTY, CheckType.VOICE_FORMAT_DIFFERS, CheckType.VOICE_DUPLICATE -> CheckFilter.VOICE
        CheckType.CHANGE_ANCHOR_DELETED -> CheckFilter.CHANGE
        CheckType.DELETED_ITEM -> CheckFilter.DELETED
        CheckType.HIDDEN_LOCKED_GROUP -> CheckFilter.ALL
        else -> CheckFilter.TRAVEL
    }

    fun open(doc: ProjectDoc, entries: List<CheckEntry>): List<CheckEntry> = Acks.open(doc, entries)

    fun count(doc: ProjectDoc, entries: List<CheckEntry>, filter: CheckFilter): Int = when (filter) {
        CheckFilter.ALL -> open(doc, entries).size
        CheckFilter.ACKNOWLEDGED -> Acks.acknowledged(doc, entries).size
        else -> open(doc, entries).count { it.filter == filter }
    }

    /** `전체 5` · `용어 공개 2` · `이동` …: 전체 always shows its count, other chips only a non-zero one (`ED`). */
    fun chips(doc: ProjectDoc, entries: List<CheckEntry>): List<String> = CheckFilter.entries.map { f ->
        val n = count(doc, entries, f)
        if (f == CheckFilter.ALL || n > 0) "${f.word} $n" else f.word
    }

    /** Rows for a filter: ▲ before ◇, then story order; locked hidden items collapse into one row at the top. */
    fun rows(doc: ProjectDoc, entries: List<CheckEntry>, filter: CheckFilter = CheckFilter.ALL): List<CheckRow> {
        val pool = when (filter) {
            CheckFilter.ACKNOWLEDGED -> Acks.acknowledged(doc, entries)
            CheckFilter.ALL -> open(doc, entries)
            else -> open(doc, entries).filter { it.filter == filter }
        }
        val (hidden, shown) = pool.partition { it.lockedHidden }
        val ordered = shown.sortedWith(compareBy<CheckEntry> { it.marker.ordinal }.thenBy { it.storyOrder ?: Long.MAX_VALUE })
        return listOfNotNull(hidden.takeIf { it.isNotEmpty() }?.let { CheckRow.HiddenGroup(it.size) }) + ordered.map(CheckRow::Item)
    }

    /** The empty state when nothing is open, else null. */
    fun empty(doc: ProjectDoc, entries: List<CheckEntry>, writtenRange: String?, lastChecked: String = "방금"): EmptyCheck? =
        if (open(doc, entries).isNotEmpty()) null else EmptyCheck(lastChecked, writtenRange, doc.checks.acknowledgements.size)
}
