package io.github.lenticualrstone.civilight.domain.world.term

import io.github.lenticualrstone.civilight.model.Key
import io.github.lenticualrstone.civilight.model.world.TermRejection

/** A `쓰인 곳` or `원고 밖` row of the term card (WLD-08, WLD-19). */
data class UsageRow(
    val link: TermLink,
    /** `1-4 씬 2`, `페이지 · 하층 구역의 빛`. */
    val location: String,
    val snippet: Snippet,
    /** `▲ 공개(1-7) 전 사용`, `◇ 첫 공개`, `◇ 근거 지워짐`. */
    val markers: List<String>,
    /** `선택지`, `게임 텍스트`, `맞음`. */
    val tags: List<String>,
    val preReveal: Boolean,
    val intended: Boolean,
    /** After the lens point (rows dim, numbers stay, rule 21 §5). */
    val dimmed: Boolean,
)

/** A `후보` row of the term card. */
data class CandidateRow(
    val candidate: TermCandidate,
    val location: String,
    val snippet: Snippet,
    /** `◇ 후보 · 끊김 · 잇기 · 아님` or `◇ 후보 · 잇기 · 아님`. */
    val marker: String,
    /** `선택지` for an option line. */
    val tags: List<String>,
    val preReveal: Boolean,
    val dimmed: Boolean,
)

/** A titled group of rows (`쓰인 곳 · 2`, `원고 밖 · 메모 · 1`). */
data class UsageGroup(val title: String, val rows: List<UsageRow>)

/** `후보 · N · 확인 안 함` with the header action `모두 잇기 · 모두 아님`. */
data class CandidateGroup(val title: String, val action: String, val rows: List<CandidateRow>)

/** Chips on the term card; nothing changes until tapped. */
sealed interface TermChip {
    val label: String

    /** `글자 찾기 끄기` (more than 10 `아님`, WLD-25). */
    data class TurnOffSearch(override val label: String) : TermChip

    /** `1-2 씬 1을 첫 공개로` (WLD-18). */
    data class SuggestReveal(val suggestion: RevealSuggestion, override val label: String) : TermChip
}

/** Where a suggested `첫 공개` would go: the block (or, for transition text, the scene start) of the first link. */
data class RevealSuggestion(val target: RevealTarget, val location: String)

/** The term card (WLD-08). */
data class TermCard(
    val termKey: Key,
    val name: String,
    /** `첫 공개` row value: `1-7 씬 3 · #044`, `공개 예정 2-5` or `첫 공개 없음`. */
    val revealValue: String,
    /** `오리지늄 · 빛돌`. */
    val aliasValue: String,
    /** `등불 결사 · 대정전`. */
    val relatedValue: String,
    /** `쓰인 곳 · 2 · 후보 1`. */
    val header: String,
    val usageCount: Int,
    /** Null when candidates are not shown (hidden term while locked). */
    val candidateCount: Int?,
    val usage: UsageGroup,
    val candidates: CandidateGroup,
    val outside: UsageGroup,
    /** `1음절 · 글자 찾기 안 함`, `글자 찾기 꺼짐 · 켜기`, `첫 공개 없음 · 쓰인 곳 3`. */
    val notices: List<String>,
    val chips: List<TermChip>,
    /** `⋯ › 아님으로 둔 것`. */
    val rejections: List<TermRejection>,
    /** Candidate numbers are dimmed while the first full scan runs (WLD-T72). */
    val candidatesPending: Boolean,
)

/** A term list row (WLD-02). */
data class TermListRow(
    val termKey: Key,
    val name: String,
    /** First line of the description. */
    val fragment: String,
    /** `첫 공개 1-7 · ◇ 후보 1`, `공개 예정 2-5`, `첫 공개 없음`. */
    val line1: String,
    /** `▲ 앞에서 1회 사용`, only when N ≥ 1. */
    val line2: String?,
)

/** The term list (WLD-02): `공개됨 · <lens>` then `아직 공개 전`. */
data class TermList(
    val revealedTitle: String,
    val revealed: List<TermListRow>,
    val pendingTitle: String,
    val pending: List<TermListRow>,
    /** `숨긴 용어 N` while locked. */
    val lockRow: String?,
    /** `용어 없음` when the project has no terms (button `용어 만들기`). */
    val emptyLine: String?,
)

/** Rows of the check-center filter `용어 공개` (WLD-28). */
sealed interface TermCheckRow {
    /** `▲ 공개 전 사용 · 1-4 씬 2` with `원고로 가기 · 공개 시점 옮기기 · 맞음`. */
    data class BeforeReveal(
        val link: TermLink,
        val title: String,
        val snippet: Snippet,
        /** `‘오리지늄 등불’ 첫 공개 1-7 · 사용 1-4`. */
        val detail: String,
        /** `◇ 근거 지워짐` when the reveal's block was deleted. */
        val marker: String?,
        val buttons: List<String>,
    ) : TermCheckRow

    /** `잿불의 서약 · ▲ 3` (term revealed beyond the 집필 선), expanding to its link rows. */
    data class BeforeRevealGroup(val termKey: Key, val title: String, val rows: List<BeforeReveal>) : TermCheckRow

    /** `◇ 공개 전 후보 · 1-2 씬 1 · …` with `원고로 가기 · 잇기 · 아님`. */
    data class CandidateBeforeReveal(
        val candidate: TermCandidate,
        val text: String,
        val snippet: Snippet,
        val buttons: List<String>,
        /** Dimmed while the full scan runs. */
        val pending: Boolean,
    ) : TermCheckRow

    /** `◇ 첫 공개 없음 · <용어> · 쓰인 곳 N` with the suggestion chip and `원고로 가기`. */
    data class NoReveal(val termKey: Key, val text: String, val chip: TermChip.SuggestReveal?, val buttons: List<String>) : TermCheckRow

    /** `▲ 숨긴 항목 관련 N · 지문으로 보기`. */
    data class HiddenLocked(val count: Int, val text: String) : TermCheckRow

    /** `◇ 지운 용어 · <이름>` with `▲ N 보류` (WLD-14). */
    data class DeletedTerm(val termKey: Key, val text: String, val held: Int, val heldText: String?) : TermCheckRow
}

/** Everything the check center needs from the term area. */
data class TermCheckReport(
    /** Active rows in order: `▲` rows, then `◇ 공개 전 후보`, then `◇ 첫 공개 없음`. */
    val rows: List<TermCheckRow>,
    /** `맞다고 표시한 것`. */
    val intended: List<TermCheckRow.BeforeReveal>,
    /** Rows of the `지운 항목` filter supplied by the term area. */
    val deleted: List<TermCheckRow.DeletedTerm>,
    /** `점검 중`: the full candidate scan has not finished. */
    val scanning: Boolean,
    /** Script module badge: active `▲` only. */
    val badgeCount: Int,
    /** Contribution to the check center `전체` (includes `◇` rows). */
    val totalCount: Int,
)
