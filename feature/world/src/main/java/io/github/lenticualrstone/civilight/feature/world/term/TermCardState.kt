package io.github.lenticualrstone.civilight.feature.world.term

import io.github.lenticualrstone.civilight.domain.world.item.WorldKind
import io.github.lenticualrstone.civilight.domain.world.term.CandidateIndex
import io.github.lenticualrstone.civilight.domain.world.term.CandidateRow
import io.github.lenticualrstone.civilight.domain.world.term.OutsideSpot
import io.github.lenticualrstone.civilight.domain.world.term.ScriptSpot
import io.github.lenticualrstone.civilight.domain.world.term.Snippet
import io.github.lenticualrstone.civilight.domain.world.term.TermAccess
import io.github.lenticualrstone.civilight.domain.world.term.TermCandidate
import io.github.lenticualrstone.civilight.domain.world.term.TermChip
import io.github.lenticualrstone.civilight.domain.world.term.TermQueries
import io.github.lenticualrstone.civilight.domain.world.term.TextSpot
import io.github.lenticualrstone.civilight.domain.world.term.UsageRow
import io.github.lenticualrstone.civilight.feature.world.WorldCopy
import io.github.lenticualrstone.civilight.feature.world.common.CardAccess
import io.github.lenticualrstone.civilight.feature.world.common.CardHeadUi
import io.github.lenticualrstone.civilight.feature.world.common.CardText
import io.github.lenticualrstone.civilight.feature.world.common.FieldRows
import io.github.lenticualrstone.civilight.feature.world.common.FieldSectionUi
import io.github.lenticualrstone.civilight.feature.world.common.FixedRowUi
import io.github.lenticualrstone.civilight.feature.world.common.ItemRefsText
import io.github.lenticualrstone.civilight.feature.world.common.ItemLifecycle
import io.github.lenticualrstone.civilight.feature.world.common.WorldTargets
import io.github.lenticualrstone.civilight.model.Key
import io.github.lenticualrstone.civilight.model.cast.EntityKind
import io.github.lenticualrstone.civilight.model.doc.ProjectDoc
import io.github.lenticualrstone.civilight.navigation.Route
import io.github.lenticualrstone.civilight.ui.lens.LensRules

/**
 * A marker line under a usage row: `▲ 공개(1-7) 전 사용` ([warn]; tapping it opens the check center,
 * WLD-20), `◇ 첫 공개`, `◇ 근거 지워짐` ([dotted], rule 12: the reveal's block was deleted),
 * `◇ 후보 · 끊김 · 잇기 · 아님`.
 */
data class MarkerUi(val text: String, val warn: Boolean, val dotted: Boolean = false) {
    companion object {
        const val ORPHANED = "◇ 근거 지워짐"

        fun of(text: String) = MarkerUi(text, warn = text.startsWith("▲"), dotted = text == ORPHANED)
    }
}

/**
 * A `쓰인 곳` row (WLD-19): the location (mono small, [location]) and the whole sentence with the
 * linked or matched span boxed inline ([boxStart] until [boxEnd] of [text]; no box when masked).
 * Dialogue is shown in quotes (board `Term`). Rows outside the manuscript leave the mono column empty
 * and carry their place as [placeLine] under the sentence (`페이지 · 하층 구역의 빛`, WLD-08).
 * [dimmed] rows lie after the lens point (numbers stay, rule 21 §5). A candidate row has
 * [candidate] set and draws `잇기 · 아님` after its marker.
 */
data class UsageRowUi(
    val id: String,
    val location: String,
    val text: String,
    val boxStart: Int,
    val boxEnd: Int,
    val markers: List<MarkerUi>,
    val tags: List<String>,
    val dimmed: Boolean,
    val target: Route?,
    val placeLine: String? = null,
    val candidate: TermCandidate? = null,
    /** The link of a `쓰인 곳` row (null for candidates). */
    val linkKey: Key? = null,
    /** A `▲ 공개 전 사용` row not yet marked: `맞음` is offered next to the marker (WLD-24). */
    val canMarkIntended: Boolean = false,
) {
    val boxed: Boolean get() = boxEnd > boxStart
}

/** `쓰인 곳 · 2`, `후보 · 1 · 확인 안 함`, `원고 밖 · 메모 · 1`. */
data class UsageGroupUi(val id: String, val title: String, val rows: List<UsageRowUi>)

/** Everything the term card draws (WLD-08, WLD-16 row, WLD-18 row and chip, WLD-19 groups). */
data class TermCardUi(
    val termKey: Key,
    val access: CardAccess,
    val head: CardHeadUi?,
    /** `첫 공개 | 1-7 씬 3 · #044` (tap → `공개 시점 옮기기`). */
    val reveal: FixedRowUi?,
    /** `별칭 | 오리지늄 · 빛돌`, only when the term has aliases. */
    val aliases: FixedRowUi?,
    /** `관련 | 등불 결사 · 대정전` (tap → `관련`). */
    val related: FixedRowUi?,
    val fields: FieldSectionUi,
    /** `1음절 · 글자 찾기 안 함`, `첫 공개 없음 · 쓰인 곳 3`. */
    val notices: List<String>,
    /** `글자 찾기 꺼짐 · 켜기` is shown (WLD-25). */
    val searchOff: Boolean,
    /** `1-2 씬 1을 첫 공개로`, `글자 찾기 끄기`; nothing changes until tapped. */
    val chips: List<TermChip>,
    /** `쓰인 곳 · 2 · 후보 1`. */
    val header: String,
    /** Candidate numbers are not final yet (first scan running). */
    val candidatesPending: Boolean,
    val groups: List<UsageGroupUi>,
    /** Text of the description holds links to hidden items while locked (its editor needs `지문으로 보기`). */
    val descriptionLocked: Boolean,
    /** `◇ 같은 이름 2 · 오리지늄 등불` (WLD-15 entry), null when the name is unique. */
    val sameName: String? = null,
    /** `모두 잇기 · 모두 아님` over the `후보` group (WLD-23), as two buttons; empty without candidates. */
    val bulkActions: List<String> = emptyList(),
) {
    val title: String
        get() = when (access) {
            CardAccess.Open -> head?.name.orEmpty()
            is CardAccess.Locked -> WorldCopy.HIDDEN_ITEM
            is CardAccess.Gone -> access.text
        }
}

/** Builds the term card at the lens from the domain's rule-21 readings. */
object TermCardState {
    /**
     * [aliasFound] = the count of `후보 N 새로 찾음` after the last alias change on this card (WLD-16),
     * shown next to `별칭`.
     */
    fun build(
        doc: ProjectDoc,
        projectKey: Key,
        termKey: Key,
        index: CandidateIndex,
        hiddenOpen: Boolean,
        readOnly: Boolean,
        aliasFound: Int? = null,
    ): TermCardUi {
        val access = CardAccess.of(doc, WorldKind.TERM, termKey, hiddenOpen)
        val term = doc.index.term(termKey)
        if (access != CardAccess.Open || term == null) return closed(termKey, access)
        val lens = LensRules.effective(doc, doc.app.view.lens.point)
        val queries = TermQueries(doc, index, TermAccess(hiddenOpen))
        val card = queries.card(termKey, lens) ?: return closed(termKey, access)
        val editable = !readOnly
        val groups = buildList {
            add(UsageGroupUi("usage", card.usage.title, card.usage.rows.map { usageRow(doc, projectKey, it, hiddenOpen) }))
            if (card.candidates.rows.isNotEmpty()) {
                add(UsageGroupUi("candidates", card.candidates.title, card.candidates.rows.map { candidateRow(doc, projectKey, it, hiddenOpen) }))
            }
            if (card.outside.rows.isNotEmpty()) {
                add(UsageGroupUi("outside", card.outside.title, card.outside.rows.map { usageRow(doc, projectKey, it, hiddenOpen) }))
            }
        }
        return TermCardUi(
            termKey = termKey,
            access = access,
            head = CardHeadUi(WorldCopy.EYEBROW_TERM, card.name, CardText.paragraphs(doc, term.description, hiddenOpen)),
            reveal = FixedRowUi(WorldCopy.ROW_REVEAL, card.revealValue, editable),
            // WLD-16: editable projects always show the row (`—` when empty), so aliases can be added.
            aliases = if (card.aliasValue.isEmpty() && !editable) null else FixedRowUi(
                WorldCopy.ROW_ALIASES,
                card.aliasValue.ifEmpty { WorldCopy.EMPTY_VALUE },
                editable,
                note = aliasFound?.takeIf { it > 0 }?.let(WorldCopy::newlyFound),
            ),
            related = FixedRowUi(
                WorldCopy.ROW_RELATED,
                ItemRefsText.of(doc, term.related, hiddenOpen).ifEmpty { WorldCopy.EMPTY_VALUE },
                editable,
            ),
            fields = FieldRows.build(doc, EntityKind.TERM, termKey, term.fields, lens),
            notices = card.notices.filterNot { it.startsWith(WorldCopy.NOTICE_SEARCH_OFF) },
            searchOff = !term.textSearch,
            chips = card.chips,
            header = card.header,
            candidatesPending = card.candidatesPending,
            groups = groups,
            descriptionLocked = CardText.hasLockedLinks(doc, term.description, hiddenOpen),
            sameName = ItemLifecycle.sameName(doc, WorldKind.TERM, termKey),
            bulkActions = if (card.candidates.rows.isEmpty() || !index.complete) emptyList() else card.candidates.action.split(WorldCopy.SEP),
        )
    }

    private fun closed(termKey: Key, access: CardAccess) = TermCardUi(
        termKey = termKey, access = access, head = null, reveal = null, aliases = null, related = null,
        fields = FieldSectionUi(emptyList(), emptyList()), notices = emptyList(), searchOff = false, chips = emptyList(),
        header = "", candidatesPending = false, groups = emptyList(), descriptionLocked = false,
    )

    private fun usageRow(doc: ProjectDoc, projectKey: Key, row: UsageRow, hiddenOpen: Boolean): UsageRowUi {
        val spot = row.link.spot
        val (text, start, end) = shown(doc, spot, row.snippet, hiddenOpen)
        val locked = WorldTargets.isLocked(doc, spot, hiddenOpen)
        val outside = spot is OutsideSpot && spot !is OutsideSpot.MemoBlock
        val place = if (locked && spot is OutsideSpot) WorldCopy.HIDDEN_ITEM else row.location
        return UsageRowUi(
            id = "link:${row.link.linkKey}",
            location = if (outside) "" else place,
            placeLine = if (outside) place else null,
            text = text,
            boxStart = start,
            boxEnd = end,
            // A `맞음` row keeps its `▲` line in `ink3` with the `맞음` tag (Q-07); it leaves every count.
            markers = row.markers.map { if (row.intended && it.startsWith("▲")) MarkerUi(it, warn = false) else MarkerUi.of(it) },
            tags = row.tags,
            dimmed = row.dimmed,
            target = if (locked) null else WorldTargets.of(projectKey, spot),
            linkKey = row.link.linkKey,
            canMarkIntended = row.preReveal && !row.intended && !locked,
        )
    }

    private fun candidateRow(doc: ProjectDoc, projectKey: Key, row: CandidateRow, hiddenOpen: Boolean): UsageRowUi {
        val c = row.candidate
        val (text, start, end) = shown(doc, c.spot, row.snippet, hiddenOpen)
        val locked = WorldTargets.isLocked(doc, c.spot, hiddenOpen)
        return UsageRowUi(
            id = LinkCandidatesState.idOf(c),
            location = row.location,
            text = text,
            boxStart = start,
            boxEnd = end,
            markers = listOf(MarkerUi(candidateMarker(row.marker), warn = false)),
            tags = row.tags,
            dimmed = row.dimmed,
            target = if (locked) null else WorldTargets.of(projectKey, c.spot),
            candidate = c,
        )
    }

    /**
     * `◇ 후보 · 끊김` of the domain's `◇ 후보 · 끊김 · 잇기 · 아님`: the two action words are drawn
     * as buttons after it.
     */
    fun candidateMarker(marker: String): String =
        marker.removeSuffix("${WorldCopy.SEP}${WorldCopy.BTN_LINK}${WorldCopy.SEP}${WorldCopy.BTN_REJECT}")

    /**
     * The sentence as the card draws it: dialogue and option lines in quotes (box shifted by the
     * opening quote), other text as is; text the lock hides reads `숨긴 씬` / `숨긴 항목` unboxed.
     */
    fun shown(doc: ProjectDoc, spot: TextSpot, snippet: Snippet, hiddenOpen: Boolean): Triple<String, Int, Int> {
        if (WorldTargets.isLocked(doc, spot, hiddenOpen)) {
            val word = if (spot is ScriptSpot || spot is OutsideSpot.MemoBlock) WorldCopy.HIDDEN_SCENE else WorldCopy.HIDDEN_ITEM
            return Triple(word, 0, 0)
        }
        return if (snippet.speaker != null) {
            Triple("“${snippet.text}”", snippet.boxStart + 1, snippet.boxEnd + 1)
        } else {
            Triple(snippet.text, snippet.boxStart, snippet.boxEnd)
        }
    }
}
