package io.github.lenticualrstone.civilight.feature.script.panel

import io.github.lenticualrstone.civilight.domain.cast.DisplayNames
import io.github.lenticualrstone.civilight.domain.cast.ItemRefs
import io.github.lenticualrstone.civilight.domain.cast.ResolvedItem
import io.github.lenticualrstone.civilight.domain.cast.relation.RelationPanel
import io.github.lenticualrstone.civilight.domain.cast.relation.RelationReader
import io.github.lenticualrstone.civilight.domain.cast.relation.RelationViews
import io.github.lenticualrstone.civilight.domain.cast.timed.ReadPoint
import io.github.lenticualrstone.civilight.domain.script.Labels
import io.github.lenticualrstone.civilight.domain.script.Manuscript
import io.github.lenticualrstone.civilight.domain.script.Runs
import io.github.lenticualrstone.civilight.feature.script.ScriptCopy
import io.github.lenticualrstone.civilight.model.Key
import io.github.lenticualrstone.civilight.model.doc.ProjectDoc
import io.github.lenticualrstone.civilight.model.doc.recordsHere
import io.github.lenticualrstone.civilight.model.script.BlockKind
import io.github.lenticualrstone.civilight.model.text.DocBlockKind
import io.github.lenticualrstone.civilight.model.text.RichDoc
import io.github.lenticualrstone.civilight.model.text.ordered
import io.github.lenticualrstone.civilight.model.text.plainText

/** A two-column row of the `참고` tab (board `CSheetRef` `.row`); [sub] is the partner's side when it changes here. */
data class RefRow(val left: String, val right: String, val sub: String? = null, val dotted: Boolean = false)

/** `“{line}”` + `1-7 씬 2` (SCR-33 `최근 대사`). */
data class RecentLine(val text: String, val location: String)

/** The `참고` tab (SCR-33): the speaker as of just before the selected block (rule 11 §2). */
data class RefTabModel(
    /** `화자 · 하르트 · 이 블록 기준`; null when [message] is shown instead. */
    val title: String?,
    /** `고른 블록 없음`, `숨긴 인물`, or the Q-15 line for a block without a speaker. */
    val message: String?,
    /** `말투`, one `리엔을 · ‘꼬마’` row per 호칭, `표시 이름`. */
    val rows: List<RefRow>,
    val recent: List<RecentLine>,
    val relations: List<RefRow>,
    /** `숨긴 인물과의 관계 1` while the hidden items are locked. */
    val hiddenRelations: String?,
) {
    companion object {
        fun message(text: String) = RefTabModel(null, text, emptyList(), emptyList(), emptyList(), null)
    }
}

object RefTab {
    /** `최근 대사` shows this many lines ([PROPOSED] 2). */
    const val RECENT = 2

    fun of(doc: ProjectDoc, selected: Key?, unlocked: Boolean = false): RefTabModel {
        val block = selected?.let(doc.index::block)?.takeIf { doc.manuscript.isLiveBlock(it.key) }
            ?: return RefTabModel.message(ScriptCopy.NO_SELECTED_BLOCK)
        val speakerKey = block.speakerKey?.takeIf { block.kind.known == BlockKind.DIALOGUE }
            ?: return RefTabModel.message(ScriptCopy.REF_NOT_DIALOGUE)
        val character = doc.index.character(speakerKey) ?: return RefTabModel.message(ScriptCopy.REF_NOT_DIALOGUE)
        if (character.hidden && !unlocked) return RefTabModel.message(Manuscript.SpeakerLabel.Hidden.TEXT)
        val rows = mutableListOf<RefRow>()
        speechSummary(character.speechBody)?.let { rows += RefRow(ScriptCopy.SPEECH_ROW, it) }
        rows += addressRows(doc, speakerKey, block.key, unlocked)
        rows += RefRow(ScriptCopy.DISPLAY_NAME, DisplayNames.referenceValue(doc, speakerKey, ReadPoint.BeforeBlock(block.key)))
        val (relations, hidden) = relationRows(doc, speakerKey, block.key, unlocked)
        return RefTabModel(
            title = ScriptCopy.refHeader(character.name),
            message = null,
            rows = rows,
            recent = recentLines(doc, speakerKey, block.key, unlocked),
            relations = relations,
            hiddenRelations = hidden.takeIf { it > 0 }?.let(ScriptCopy::hiddenRelations),
        )
    }

    /**
     * `말투` (`ED`): the first non-empty paragraph of the cast 말투 tab (its own `말투` heading and
     * `⌁` memo paragraphs are skipped).
     */
    fun speechSummary(body: RichDoc): String? = body.ordered().asSequence()
        .filter { it.kind.known == DocBlockKind.PARAGRAPH }
        .flatMap { it.runs.plainText.lineSequence() }
        .map { it.trim() }
        .firstOrNull { it.isNotEmpty() }

    /**
     * 호칭 rows (`리엔을` · `‘꼬마’`): the speaker's 호칭 for each partner just before the block; one
     * set by this block reads `‘꼬마’ ◆ 여기서` (`만나기 전` stands for no earlier 호칭).
     */
    fun addressRows(doc: ProjectDoc, speakerKey: Key, blockKey: Key, unlocked: Boolean): List<RefRow> {
        val here = doc.recordsHere(blockKey)
        return RelationPanel.rows(doc, speakerKey, blockKey, unlocked).mapNotNull { row ->
            val relation = doc.index.relation(row.relationKey) ?: return@mapNotNull null
            val side = RelationReader.sideOf(relation, speakerKey) ?: return@mapNotNull null
            val target = RelationReader.target(relation.key, RelationReader.addressSlot(side))
            val changed = here.lastOrNull { it.target.sameAs(target) }?.let(RelationReader::addressOf)
            val before = row.address?.let { "‘$it’" }
            val value = when {
                changed != null -> "${before ?: ScriptCopy.NOT_MET} → ‘$changed’ ${ScriptCopy.HERE}"
                before != null -> before
                else -> return@mapNotNull null
            }
            RefRow(row.partnerName + RelationPanel.objectParticle(row.partnerName), value)
        }
    }

    /**
     * `관계 · 지금`: one row per live partner from the speaker's side, `→ 보호 · ← 경계`, `사이 동료`,
     * `만나기 전`; a mind set by this block reads `만나기 전 → 보호 ◆ 여기서` (the partner's side on the
     * small line). Hidden partners are counted, deleted ones end the list as one dotted row.
     */
    fun relationRows(doc: ProjectDoc, speakerKey: Key, blockKey: Key, unlocked: Boolean): Pair<List<RefRow>, Int> {
        val rows = RelationPanel.rows(doc, speakerKey, blockKey, unlocked).map { row ->
            val main = when {
                row.own.here != null -> "→ ${row.own.text}"
                row.notMetBefore -> ScriptCopy.NOT_MET
                else -> listOfNotNull(
                    row.between,
                    row.own.before.takeIf { it != ScriptCopy.NOT_MET && row.own.here == null && it != NOT_WRITTEN }?.let { "→ $it" },
                    row.other.before.takeIf { row.other.here == null && it != ScriptCopy.NOT_MET && it != NOT_WRITTEN }?.let { "← $it" },
                ).joinToString(" · ").ifEmpty { row.between ?: ScriptCopy.NOT_MET }
            }
            val sub = row.other.here?.let { "← ${row.other.text}" }
                ?: row.other.before.takeIf { row.own.here != null && it != ScriptCopy.NOT_MET && it != NOT_WRITTEN }?.let { "← $it" }
            RefRow(row.partnerName, main, sub)
        }
        var hidden = 0
        val deleted = mutableListOf<String>()
        for (relation in RelationViews.relationsOf(doc, speakerKey)) {
            val side = RelationReader.sideOf(relation, speakerKey) ?: continue
            val partner = RelationReader.endKey(relation, side.other)
            when (val r = ItemRefs.character(doc, partner, unlocked)) {
                is ResolvedItem.HiddenLocked -> hidden++
                is ResolvedItem.Trashed -> deleted += r.name
                is ResolvedItem.Tagged -> deleted += r.name
                else -> Unit
            }
        }
        val tail = deleted.distinct().takeIf { it.isNotEmpty() }?.let { listOf(RefRow(ScriptCopy.deletedPartners(it.joinToString(" · ")), "", dotted = true)) }.orEmpty()
        return rows + tail to hidden
    }

    /** `최근 대사`: the speaker's lines before the block, newest first, skipping locked hidden scenes. */
    fun recentLines(doc: ProjectDoc, speakerKey: Key, blockKey: Key, unlocked: Boolean): List<RecentLine> {
        val blocks = doc.manuscript.liveBlocks
        val at = blocks.indexOfFirst { it.key == blockKey }.takeIf { it >= 0 } ?: return emptyList()
        return blocks.subList(0, at).asReversed().asSequence()
            .filter { it.kind.known == BlockKind.DIALOGUE && it.speakerKey == speakerKey }
            .filter { unlocked || doc.index.unit(it.sceneKey)?.hidden != true }
            .map { RecentLine("“${Runs.exportText(it.runs, includeParens = false).trim()}”", Labels.sceneLocation(doc, it.sceneKey)) }
            .filter { it.text.length > 2 }
            .take(RECENT)
            .toList()
    }

    /** Cast's word for a relation without a mind (`마음 적지 않음`), left out of the short row. */
    private val NOT_WRITTEN: String = io.github.lenticualrstone.civilight.domain.cast.CastCopy.NOT_WRITTEN
}
