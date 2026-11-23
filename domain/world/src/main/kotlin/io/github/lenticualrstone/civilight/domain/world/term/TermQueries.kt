package io.github.lenticualrstone.civilight.domain.world.term

import io.github.lenticualrstone.civilight.domain.world.common.Copy
import io.github.lenticualrstone.civilight.domain.world.common.Josa
import io.github.lenticualrstone.civilight.domain.world.common.StoryCodes
import io.github.lenticualrstone.civilight.model.Key
import io.github.lenticualrstone.civilight.model.change.ChangeRecord
import io.github.lenticualrstone.civilight.model.checks.AckKind
import io.github.lenticualrstone.civilight.model.checks.CheckType
import io.github.lenticualrstone.civilight.model.doc.ProjectDoc
import io.github.lenticualrstone.civilight.model.game.GameTools
import io.github.lenticualrstone.civilight.model.position.LensPoint
import io.github.lenticualrstone.civilight.model.position.Rank
import io.github.lenticualrstone.civilight.model.text.docText
import io.github.lenticualrstone.civilight.model.world.ItemRefKind
import io.github.lenticualrstone.civilight.model.world.Term

/**
 * Rule-21 readings of one project state (WLD-17 … WLD-28): links, usage, `▲`, candidates after
 * `아님`, the term card, the term list and the check rows. Usage and `▲` come from links only and
 * never wait for [index]; candidate numbers are pending while the index is incomplete.
 */
class TermQueries(
    val doc: ProjectDoc,
    val index: CandidateIndex,
    val access: TermAccess = TermAccess(),
) {
    val codes = StoryCodes(doc)
    val describer = SpotDescriber(doc, codes)

    private val scriptTexts: List<SpotText> by lazy { TextSpots.script(doc) }
    /** (0.2.0 CST-73) Secret text of a character whose `이 탭 잠그기` is on is never listed or counted here. */
    private val outsideTexts: List<SpotText> by lazy { TextSpots.outside(doc).filterNot { secretLocked(doc, it.spot) } }
    private val spotOrder: Map<TextSpot, Int> by lazy {
        (scriptTexts + outsideTexts).withIndex().associate { (i, s) -> s.spot to i }
    }
    private val runsBySpot: Map<TextSpot, SpotText> by lazy { (scriptTexts + outsideTexts).associateBy { it.spot } }

    /** Every live term link of the project: manuscript game output first (in order), then the rest. */
    val allLinks: List<TermLink> by lazy { (scriptTexts + outsideTexts).flatMap(TermLinks::inSpot) }

    private val linksByTerm: Map<Key, List<TermLink>> by lazy { allLinks.groupBy { it.termKey } }
    private val reveals = HashMap<Key, RevealInfo?>()

    fun term(termKey: Key): Term? = doc.index.term(termKey)

    /**
     * (0.2.0, WLD-72) The term's **effective** reveal, or null: the stored (hand-set) record when there is
     * one, else the automatic reveal at its first usage link in manuscript order ([RevealInfo.isAuto]).
     * Every reader (list, card, `▲`, candidates, check rows, the script) goes through this.
     */
    fun reveal(termKey: Key): RevealInfo? = reveals.getOrPut(termKey) {
        TermReveals.of(doc, termKey, codes)
            ?: usage(termKey).firstOrNull()?.takeIf { term(termKey)?.deletedAt == null }?.let { link ->
                (link.spot as? ScriptSpot)?.let { TermReveals.autoInfo(doc, termKey, TermReveals.anchorOf(it), codes) }
            }
    }

    /** (0.2.0) The stored (hand-set) reveal only, or null. */
    fun storedReveal(termKey: Key): RevealInfo? = reveal(termKey)?.takeUnless { it.isAuto }

    /** (0.2.0, WLD-72) True when the effective reveal is the automatic one. */
    fun isAutoReveal(termKey: Key): Boolean = reveal(termKey)?.isAuto == true

    /** (0.2.0, WLD-73) The project setting `용어 후보`: one switch for every candidate surface. */
    val candidatesOn: Boolean get() = doc.project.termCandidates

    /**
     * (0.2.0, WLD-26, WLD-73) True while candidate numbers wait for the scan. Never while `용어 후보` is off: the
     * index is not built then, so nothing waits for it.
     */
    val scanPending: Boolean get() = candidatesOn && !index.complete

    /** All live links to a term. */
    fun links(termKey: Key): List<TermLink> = linksByTerm[termKey].orEmpty()

    /** `쓰인 곳`: links in game-output manuscript text, in manuscript order. */
    fun usage(termKey: Key): List<TermLink> = links(termKey).filter { it.isUsage }

    /**
     * `원고 밖 · 메모`. (0.2.0, WLD-19, WLD-76) Links in character game text (voice lines, archive documents)
     * are listed and counted only with `게임 제작 도구` on; the links themselves are kept.
     */
    fun outside(termKey: Key): List<TermLink> = links(termKey).filterNot { it.isUsage }
        .filter { GameTools.on(doc) || (it.spot !is OutsideSpot.VoiceLineText && it.spot !is OutsideSpot.ArchiveText) }

    /** `▲ 공개 전 사용` (WLD-20): a usage link before the reveal; block granularity, story time never consulted. */
    fun isBeforeReveal(link: TermLink): Boolean {
        if (!link.isUsage) return false
        val reveal = reveal(link.termKey) ?: return false
        val rank = link.rank ?: return false
        return rank < reveal.rank
    }

    /** True when the author marked the link `맞음` (an `Acknowledgement(term.beforeReveal, [linkKey])`). */
    fun isIntended(link: TermLink): Boolean = doc.checks.acknowledgements.any {
        it.checkType == CheckType.TERM_BEFORE_REVEAL && it.kind.known == AckKind.MATCH &&
            it.subjectKeys == listOf(link.linkKey) && it.fingerprint == TermFingerprints.beforeReveal(link.termKey, link.linkKey)
    }

    /** `▲` links of a term, `맞음` excluded (Q-07 default). */
    fun activeWarnings(termKey: Key): List<TermLink> = usage(termKey).filter { isBeforeReveal(it) && !isIntended(it) }

    /** True when candidates of this term are searched and shown now. */
    fun showsCandidates(term: Term): Boolean = candidatesOn && access.canSearch(term)

    /** Current candidates of a term after `아님`, in manuscript order (empty when not searchable). */
    fun candidates(termKey: Key): List<TermCandidate> {
        val term = term(termKey) ?: return emptyList()
        if (!showsCandidates(term)) return emptyList()
        return index.of(termKey)
            .filter { TextSpots.isLive(doc, it.spot) && isCurrent(it) && !TermRejections.isRejected(term, it) }
            .sortedWith(compareBy<TermCandidate> { spotOrder[it.spot] ?: Int.MAX_VALUE }.thenBy { it.start })
    }

    /** False for a candidate the index still holds but the text no longer has as plain text (e.g. just linked). */
    private fun isCurrent(candidate: TermCandidate): Boolean {
        val runs = runsBySpot[candidate.spot]?.runs ?: return false
        return TextSpots.segments(runs).any { s ->
            candidate.start >= s.offset && candidate.end <= s.offset + s.text.length &&
                java.text.Normalizer.normalize(
                    s.text.substring(candidate.start - s.offset, candidate.end - s.offset), java.text.Normalizer.Form.NFC,
                ) == candidate.matchedText
        }
    }

    /** Rank of a candidate's spot. */
    fun rankOf(candidate: TermCandidate): Rank? = TextSpots.rankOf(doc, candidate.spot)

    /** A candidate before the term's reveal (`◇ 공개 전 후보`). */
    fun isBeforeReveal(candidate: TermCandidate): Boolean {
        val reveal = reveal(candidate.termKey) ?: return false
        val rank = rankOf(candidate) ?: return false
        return rank < reveal.rank
    }

    fun preRevealCandidates(termKey: Key): List<TermCandidate> = candidates(termKey).filter(::isBeforeReveal)

    /** The sentence of a link. */
    fun snippetOf(link: TermLink): Snippet =
        describer.snippet(link.spot, runsBySpot[link.spot]?.runs.orEmpty(), link.start, link.end)

    /** The sentence of a candidate. */
    fun snippetOf(candidate: TermCandidate): Snippet =
        describer.snippet(candidate.spot, TextSpots.runsOf(doc, candidate.spot).orEmpty(), candidate.start, candidate.end)

    /** `끊김` or `별칭 빛돌` (empty for a match of the name). */
    fun candidateTag(candidate: TermCandidate): String? = when {
        candidate.kind == CandidateKind.CUT_OFF -> "끊김"
        candidate.alias != null -> "별칭 ${candidate.alias}"
        else -> null
    }

    /** First manuscript link and the `<location>을 첫 공개로` chip when the term has no reveal (WLD-18). */
    fun revealSuggestion(termKey: Key): TermChip.SuggestReveal? {
        if (reveal(termKey) != null) return null
        val first = usage(termKey).firstOrNull() ?: return null
        val target = when (val spot = first.spot) {
            is ScriptSpot.BlockText -> RevealTarget.Block(spot.blockKey)
            is ScriptSpot.OptionText -> RevealTarget.Block(spot.blockKey)
            is ScriptSpot.TransitionText -> RevealTarget.Unit(spot.sceneKey)
            else -> return null
        }
        val location = describer.location(first.spot)
        return TermChip.SuggestReveal(RevealSuggestion(target, location), "${Josa.withObject(location)} 첫 공개로")
    }

    // --- Card -----------------------------------------------------------------------------------

    /** The term card at [lens] (WLD-08). */
    fun card(termKey: Key, lens: LensPoint = LensPoint.Latest): TermCard? {
        val term = term(termKey) ?: return null
        val reveal = reveal(termKey)
        val lensRank = doc.manuscript.rankOf(lens)
        val usage = usage(termKey)
        val outside = outside(termKey)
        val shows = showsCandidates(term)
        val candidates = candidates(termKey)
        val usageRows = usage.map { usageRow(it, reveal, lensRank) }
        val outsideRows = outside.map { usageRow(it, reveal, lensRank) }
        val candidateRows = candidates.map { c ->
            val tag = if (c.kind == CandidateKind.CUT_OFF) "끊김${Copy.SEP}" else ""
            CandidateRow(
                candidate = c,
                location = describer.location(c.spot),
                snippet = snippetOf(c),
                marker = "◇ 후보${Copy.SEP}${tag}잇기${Copy.SEP}아님",
                tags = if (c.spot is ScriptSpot.OptionText) listOf("선택지") else emptyList(),
                preReveal = isBeforeReveal(c),
                dimmed = (rankOf(c) ?: Rank.UNPLACED) > lensRank,
            )
        }
        val header = buildString {
            append("쓰인 곳${Copy.SEP}${usage.size}")
            if (shows && candidates.isNotEmpty()) append("${Copy.SEP}후보 ${candidates.size}")
        }
        val notices = ArrayList<String>()
        // (0.2.0, WLD-73, WLD-25) The `글자 찾기` states belong to the candidate check: hidden while it is off.
        if (candidatesOn && TermPattern.isSingleSyllable(term)) notices += "1음절${Copy.SEP}글자 찾기 안 함"
        if (candidatesOn && !term.textSearch) notices += "글자 찾기 꺼짐${Copy.SEP}켜기"
        // (0.2.0, WLD-08) With the automatic reveal a term with usage always has a reveal; the state is gone.
        if (reveal == null && usage.isNotEmpty()) notices += "첫 공개 없음${Copy.SEP}쓰인 곳 ${usage.size}"
        val chips = ArrayList<TermChip>()
        if (candidatesOn && term.textSearch && term.rejections.size > REJECTION_CHIP_THRESHOLD) chips += TermChip.TurnOffSearch("글자 찾기 끄기")
        revealSuggestion(termKey)?.let { chips += it }
        return TermCard(
            termKey = termKey,
            name = term.name,
            revealValue = reveal?.let { revealCardValue(it) } ?: "첫 공개 없음",
            aliasValue = term.aliases.joinToString(Copy.SEP),
            relatedValue = term.related.mapNotNull { ref ->
                when (ref.kind.known) {
                    ItemRefKind.TERM -> doc.index.term(ref.key)?.name
                    ItemRefKind.PLACE -> doc.index.place(ref.key)?.name
                    ItemRefKind.FACTION -> doc.index.faction(ref.key)?.name
                    null -> null
                }
            }.joinToString(Copy.SEP),
            header = header,
            usageCount = usage.size,
            candidateCount = if (shows) candidates.size else null,
            usage = UsageGroup("쓰인 곳${Copy.SEP}${usage.size}", usageRows),
            candidates = CandidateGroup(
                "후보${Copy.SEP}${candidates.size}${Copy.SEP}확인 안 함",
                "모두 잇기${Copy.SEP}모두 아님",
                candidateRows,
            ),
            outside = UsageGroup("원고 밖${Copy.SEP}메모${Copy.SEP}${outside.size}", outsideRows),
            notices = notices,
            chips = chips,
            rejections = term.rejections,
            candidatesPending = scanPending,
        )
    }

    private fun usageRow(link: TermLink, reveal: RevealInfo?, lensRank: Rank): UsageRow {
        val pre = isBeforeReveal(link)
        val intended = pre && isIntended(link)
        val markers = ArrayList<String>()
        val tags = ArrayList<String>()
        if (pre) {
            markers += "▲ 공개(${reveal?.episodeLabel.orEmpty()}) 전 사용"
            if (reveal?.orphaned == true) markers += "◇ 근거 지워짐"
        }
        val blockKey = when (val spot = link.spot) {
            is ScriptSpot.BlockText -> spot.blockKey
            is ScriptSpot.OptionText -> spot.blockKey
            else -> null
        }
        if (blockKey != null && reveal?.blockKey == blockKey && reveal.record.anchor is io.github.lenticualrstone.civilight.model.change.Anchor.AtBlock) {
            markers += "◇ 첫 공개"
        }
        if (link.spot is ScriptSpot.OptionText) tags += "선택지"
        if (link.spot is OutsideSpot.VoiceLineText || link.spot is OutsideSpot.ArchiveText) tags += "게임 텍스트"
        if (intended) tags += "맞음"
        return UsageRow(
            link = link,
            location = describer.location(link.spot),
            snippet = snippetOf(link),
            markers = markers,
            tags = tags,
            preReveal = pre,
            intended = intended,
            dimmed = link.rank != null && link.rank > lensRank,
        )
    }

    /**
     * (0.2.0, WLD-72) The card's `첫 공개` value. While the hidden lock is closed and the reveal's block is in a
     * hidden scene, it reads `<episode> · 숨긴 씬` (the scene and block are not named).
     */
    fun revealCardValue(reveal: RevealInfo): String {
        if (access.hiddenUnlocked) return reveal.cardValue
        val block = reveal.blockKey?.let { doc.index.block(it) } ?: return reveal.cardValue
        val scene = doc.index.unit(block.sceneKey) ?: return reveal.cardValue
        val hidden = scene.hidden || doc.index.ancestors(scene).any { it.hidden }
        return if (hidden) "${reveal.episodeLabel.orEmpty()}${Copy.SEP}숨긴 씬" else reveal.cardValue
    }

    /**
     * (0.2.0, WLD-73) The last section of the 용어 tab while `용어 후보` is on: `용어 후보 · N`, one row per term
     * with at least one candidate (after `아님`), in the term list's order at [lens]; null when there is none (or the
     * setting is off). Locked hidden terms are left out (they are not searched).
     */
    fun candidateSection(lens: LensPoint = LensPoint.Latest): CandidateSection? {
        if (!candidatesOn) return null
        val listed = list(lens).let { l -> l.revealed + l.pending }.map { it.termKey }
        val rows = listed.mapNotNull(::term)
            .filter { showsCandidates(it) }
            .mapNotNull { term ->
                val all = candidates(term.key)
                if (all.isEmpty()) return@mapNotNull null
                val pre = all.count(::isBeforeReveal)
                CandidateTermRow(
                    termKey = term.key,
                    name = term.name,
                    fragment = "후보 ${all.size}",
                    right = if (pre > 0) "◇ 공개 전 후보 $pre" else null,
                )
            }
        if (rows.isEmpty()) return null
        return CandidateSection("용어 후보${Copy.SEP}${rows.size}", rows, pending = scanPending)
    }

    /**
     * (0.2.0, WLD-27, WLD-73) The candidate spans of one manuscript block for the script, in text order
     * (option lines by option order): the faint dotted underline and the `참고` rows
     * `용어 후보 · <matchedText> · 잇기 · 아님`. Empty while `용어 후보` is off.
     */
    fun candidateSpans(blockKey: Key): List<CandidateSpan> {
        if (!candidatesOn) return emptyList()
        val block = doc.index.block(blockKey) ?: return emptyList()
        val spots = TextSpots.scriptSpotsOf(doc, block).mapNotNull { it.spot as? ScriptSpot }
        return spots.withIndex().flatMap { (optionIndex, spot) ->
            index.at(spot).map { it.termKey }.distinct()
                .flatMap { key -> candidates(key).filter { it.spot == spot } }
                .sortedBy { it.start }
                .map { c ->
                    CandidateSpan(
                        candidate = c,
                        termKey = c.termKey,
                        termName = term(c.termKey)?.name.orEmpty(),
                        matchedText = c.matchedText,
                        kind = c.kind,
                        start = c.start,
                        end = c.end,
                        optionIndex = if (spot is ScriptSpot.OptionText) optionIndex else null,
                    )
                }
        }
    }

    /** (0.2.0, WLD-73) The `참고` row text of a candidate: `용어 후보 · 등불`. */
    fun referenceLine(span: CandidateSpan): String = "용어 후보${Copy.SEP}${span.matchedText}"

    // --- List -----------------------------------------------------------------------------------

    /** The term list at [lens] (WLD-02). */
    fun list(lens: LensPoint = LensPoint.Latest): TermList {
        val lensRank = doc.manuscript.rankOf(lens)
        val live = doc.world.terms.filter { it.deletedAt == null }.sortedWith(compareBy({ it.order }, { it.key }))
        val locked = live.filter { it.hidden && !access.hiddenUnlocked }
        val visible = live - locked.toSet()
        val revealed = ArrayList<Pair<Rank, TermListRow>>()
        val planned = ArrayList<Pair<Rank, TermListRow>>()
        val none = ArrayList<TermListRow>()
        for (term in visible) {
            val reveal = reveal(term.key)
            val row = listRow(term, reveal)
            when {
                reveal == null -> none += row
                reveal.rank <= lensRank -> revealed += reveal.rank to row
                else -> planned += reveal.rank to row
            }
        }
        return TermList(
            revealedTitle = "공개됨${Copy.SEP}${codes.lensLabel(lens)}",
            revealed = revealed.sortedBy { it.first }.map { it.second },
            pendingTitle = "아직 공개 전",
            pending = planned.sortedBy { it.first }.map { it.second } + none,
            lockRow = if (locked.isEmpty()) null else "숨긴 용어 ${locked.size}",
            emptyLine = if (live.isEmpty()) "용어 없음" else null,
        )
    }

    private fun listRow(term: Term, reveal: RevealInfo?): TermListRow {
        val line1 = buildString {
            append(reveal?.listLabel ?: "첫 공개 없음")
            if (showsCandidates(term)) {
                val pre = preRevealCandidates(term.key).size
                if (pre > 0) append("${Copy.SEP}◇ 후보 $pre")
            }
        }
        val warnings = activeWarnings(term.key).size
        return TermListRow(
            termKey = term.key,
            name = term.name,
            fragment = term.description.docText.lineSequence().firstOrNull().orEmpty(),
            line1 = line1,
            line2 = if (warnings > 0) "▲ 앞에서 ${warnings}회 사용" else null,
        )
    }

    // --- Check center ---------------------------------------------------------------------------

    /** Rows of the check-center filter `용어 공개` (WLD-28). */
    fun checks(): TermCheckReport {
        val live = doc.world.terms.filter { it.deletedAt == null }.sortedWith(compareBy({ it.order }, { it.key }))
        val warningRows = ArrayList<Pair<Int, TermCheckRow>>()
        val intended = ArrayList<TermCheckRow.BeforeReveal>()
        val candidateRows = ArrayList<Pair<Int, TermCheckRow.CandidateBeforeReveal>>()
        val noReveal = ArrayList<TermCheckRow.NoReveal>()
        var hiddenCount = 0
        var badge = 0
        var total = 0
        for (term in live) {
            val locked = term.hidden && !access.hiddenUnlocked
            val reveal = reveal(term.key)
            val pre = usage(term.key).filter(::isBeforeReveal)
            val (acked, active) = pre.partition(::isIntended)
            badge += active.size
            if (locked) {
                hiddenCount += active.size + (if (reveal == null && usage(term.key).isNotEmpty()) 1 else 0)
                continue
            }
            intended += acked.map { beforeRevealRow(term, it, reveal) }
            if (active.isNotEmpty()) {
                val rows = active.map { beforeRevealRow(term, it, reveal) }
                val position = spotOrder[active.first().spot] ?: Int.MAX_VALUE
                warningRows += position to if (reveal?.planned == true) {
                    TermCheckRow.BeforeRevealGroup(term.key, "${term.name}${Copy.SEP}▲ ${rows.size}", rows)
                } else {
                    // Individual rows sort by their own position.
                    rows.first()
                }
                if (reveal?.planned != true) {
                    rows.drop(1).forEach { warningRows += (spotOrder[it.link.spot] ?: Int.MAX_VALUE) to it }
                }
                total += rows.size
            }
            if (reveal != null) {
                for (c in preRevealCandidates(term.key)) {
                    val snippet = snippetOf(c)
                    val tag = candidateTag(c)?.let { " $it" }.orEmpty()
                    val text = "◇ 공개 전 후보${Copy.SEP}${describer.location(c.spot)}${Copy.SEP}${snippet.render()}" +
                        "${Copy.SEP}‘${term.name}’$tag${Copy.SEP}${reveal.listLabel}"
                    candidateRows += (spotOrder[c.spot] ?: Int.MAX_VALUE) to TermCheckRow.CandidateBeforeReveal(
                        candidate = c, text = text, snippet = snippet, buttons = listOf("원고로 가기", "잇기", "아님"),
                        pending = scanPending,
                    )
                }
            } else if (usage(term.key).isNotEmpty()) {
                noReveal += TermCheckRow.NoReveal(
                    termKey = term.key,
                    text = "◇ 첫 공개 없음${Copy.SEP}${term.name}${Copy.SEP}쓰인 곳 ${usage(term.key).size}",
                    chip = revealSuggestion(term.key),
                    buttons = listOf("원고로 가기"),
                )
            }
        }
        val rows = ArrayList<TermCheckRow>()
        rows += warningRows.sortedBy { it.first }.map { it.second }
        if (hiddenCount > 0) rows += TermCheckRow.HiddenLocked(hiddenCount, "▲ 숨긴 항목 관련 $hiddenCount${Copy.SEP}지문으로 보기")
        rows += candidateRows.sortedBy { it.first }.map { it.second }
        rows += noReveal
        total += candidateRows.size + noReveal.size + hiddenCount
        return TermCheckReport(
            rows = rows,
            intended = intended,
            deleted = deletedTermRows(),
            scanning = scanPending,
            badgeCount = badge,
            totalCount = total,
        )
    }

    private fun beforeRevealRow(term: Term, link: TermLink, reveal: RevealInfo?): TermCheckRow.BeforeReveal {
        val location = describer.location(link.spot)
        val useEpisode = when (val spot = link.spot) {
            is ScriptSpot.BlockText -> codes.episodeOfBlock(spot.blockKey)
            is ScriptSpot.OptionText -> codes.episodeOfBlock(spot.blockKey)
            is ScriptSpot.TransitionText -> codes.episodeCode(spot.sceneKey)
            else -> null
        }.orEmpty()
        return TermCheckRow.BeforeReveal(
            link = link,
            title = "▲ 공개 전 사용${Copy.SEP}$location",
            snippet = snippetOf(link),
            detail = "‘${term.name}’ ${reveal?.listLabel.orEmpty()}${Copy.SEP}사용 $useEpisode",
            marker = if (reveal?.orphaned == true) "◇ 근거 지워짐" else null,
            buttons = listOf("원고로 가기", "공개 시점 옮기기", "맞음"),
        )
    }

    /**
     * Trashed terms that live text still links to: `◇ 지운 용어 · <이름>` with the `▲` that are held
     * (computed as if the term were live; restored with it, WLD-14).
     */
    private fun deletedTermRows(): List<TermCheckRow.DeletedTerm> =
        doc.world.terms.filter { it.deletedAt != null && links(it.key).isNotEmpty() }.map { term ->
            val held = heldWarnings(term.key)
            TermCheckRow.DeletedTerm(
                termKey = term.key,
                text = "◇ 지운 용어${Copy.SEP}${term.name}",
                held = held,
                heldText = if (held > 0) "▲ $held 보류" else null,
            )
        }

    /** `▲` of a term counted with its own reveal record even when the term is in the trash (`▲ N 보류`). */
    fun heldWarnings(termKey: Key): Int {
        val record: ChangeRecord = TermReveals.record(doc, termKey) { it.deletedAt == null } ?: return 0
        val revealRank = doc.manuscript.rankOf(record.anchor)
        return usage(termKey).count { link -> link.rank != null && link.rank < revealRank && !isIntended(link) }
    }

    companion object {
        /** More than this many `아님` shows the chip `글자 찾기 끄기` (WLD-25). */
        const val REJECTION_CHIP_THRESHOLD = 10
    }
}

/** (0.2.0, WLD-73) A row of the `용어 후보 · N` section: name, `후보 3`, and `◇ 공개 전 후보 1` when some come before the reveal. */
data class CandidateTermRow(val termKey: Key, val name: String, val fragment: String, val right: String?)

/** (0.2.0, WLD-73) The `용어 후보 · N` section of the 용어 tab; [pending] while the scan is not done. */
data class CandidateSection(val title: String, val rows: List<CandidateTermRow>, val pending: Boolean)

/**
 * (0.2.0, WLD-27) What the script draws for one candidate: `{termKey, termName, matchedText, kind, start,
 * end, optionIndex?}` ([optionIndex] = index of the option line inside a choice block), plus the candidate
 * itself for `잇기` ([TermEdits.link]) and `아님` ([TermEdits.reject]).
 */
data class CandidateSpan(
    val candidate: TermCandidate,
    val termKey: Key,
    val termName: String,
    val matchedText: String,
    val kind: CandidateKind,
    val start: Int,
    val end: Int,
    val optionIndex: Int?,
)

/**
 * (0.2.0, CST-73) True for a spot that shows the 비밀 tab of a character whose `이 탭 잠그기` is on: the secret
 * body and the values of secret fields. Readers outside the record (world lists, `원고 밖 · 메모`, link counts)
 * leave such spots out; the record asks for the device lock itself. The text is kept (scope §8).
 */
fun secretLocked(doc: ProjectDoc, spot: TextSpot): Boolean = when (spot) {
    is OutsideSpot.CharacterBody -> spot.secret && doc.index.character(spot.characterKey)?.secretTabLocked == true
    is OutsideSpot.FieldText -> spot.entityKind == io.github.lenticualrstone.civilight.model.cast.EntityKind.CHARACTER &&
        doc.index.character(spot.entityKey)?.secretTabLocked == true &&
        doc.index.fieldDef(spot.fieldDefKey)?.isSecret == true
    else -> false
}
