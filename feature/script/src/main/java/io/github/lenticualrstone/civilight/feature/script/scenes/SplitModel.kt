package io.github.lenticualrstone.civilight.feature.script.scenes

import io.github.lenticualrstone.civilight.domain.actions.ActionContext
import io.github.lenticualrstone.civilight.domain.actions.SceneActions
import io.github.lenticualrstone.civilight.domain.script.Labels
import io.github.lenticualrstone.civilight.domain.script.Records
import io.github.lenticualrstone.civilight.domain.story.scene.SilentCastChoice
import io.github.lenticualrstone.civilight.domain.story.scene.SplitPreview
import io.github.lenticualrstone.civilight.domain.story.time.CascadeMode
import io.github.lenticualrstone.civilight.domain.story.time.TimeChange
import io.github.lenticualrstone.civilight.domain.story.units.PlaceNames
import io.github.lenticualrstone.civilight.model.Key
import io.github.lenticualrstone.civilight.model.change.Anchor
import io.github.lenticualrstone.civilight.model.change.ChangeRecord
import io.github.lenticualrstone.civilight.model.change.ChangeTarget
import io.github.lenticualrstone.civilight.model.doc.ProjectDoc
import io.github.lenticualrstone.civilight.model.script.Block
import io.github.lenticualrstone.civilight.model.script.BlockKind
import io.github.lenticualrstone.civilight.model.story.PlaceRef
import io.github.lenticualrstone.civilight.model.story.PlaceRefKind
import io.github.lenticualrstone.civilight.model.story.SceneTime
import kotlinx.serialization.Serializable
import io.github.lenticualrstone.civilight.domain.script.SceneSplit as ScriptExcerpts

/** What the 장소 row of the new scene holds: A's place (prefilled), nothing, or a picked place. */
enum class PlaceMode { SAME, CLEARED, PICKED }

/**
 * The author's choices in `씬 나누기` (SCR-50), saveable with the sheet. Defaults = the preview's
 * prefills: A's place, `씬 n 기준 · 같은 때`, no title, every silent-cast member in `둘 다`, the
 * `회상` / `숨김` rows on.
 */
@Serializable
data class SplitChoices(
    val place: PlaceMode = PlaceMode.SAME,
    val pickedKind: PlaceRefKind? = null,
    val pickedKey: Key? = null,
    val sameTime: Boolean = true,
    val title: String = "",
    val silentCast: Map<Key, SilentCastChoice> = emptyMap(),
    val flashback: Boolean = true,
    val hidden: Boolean = true,
) {
    val picked: PlaceRef? get() = if (place == PlaceMode.PICKED && pickedKind != null && pickedKey != null) PlaceRef(pickedKind, pickedKey) else null

    fun pick(ref: PlaceRef): SplitChoices = copy(place = PlaceMode.PICKED, pickedKind = ref.kind.known, pickedKey = ref.key)
}

/** One part card: `씬 3` · `블록 9개` · `‘세라 · 불은 꺼졌어…’ ~ ‘…한 번 깜박였다.’`; [isNew] = the `acc` bar. */
data class PartCard(val title: String, val count: String, val range: String, val isNew: Boolean)

/** A `장소` / `때` / `제목` row: [value] + `small` [note] (`· 씬 3과 같음`). */
data class FieldLine(val value: String, val note: String? = null)

/** `바뀌는 것` row as the boards draw it: the label column (`순번`, `◆ 관계`) and the value. */
data class ChangeLine(val label: String, val value: String) {
    companion object {
        /** Splits a domain line (`순번 · 씬 4 · 미정 → 씬 5`) at its first ` · `. */
        fun of(line: String): ChangeLine {
            val i = line.indexOf(SEP)
            return if (i < 0) ChangeLine(line, "") else ChangeLine(line.substring(0, i), line.substring(i + SEP.length))
        }

        private const val SEP = " · "
    }
}

/** A silent-cast row: `말 없는 등장 · 세라` with chips `씬 3 | 새 씬 4 | 둘 다`. */
data class SilentRow(val characterKey: Key, val label: String, val chips: List<String>, val selected: Int)

/** Everything `RSSplit` draws for a split below one block. */
data class SplitSheetModel(
    val title: String,
    val sub: String,
    val parts: List<PartCard>,
    val place: FieldLine,
    val placeClearable: Boolean,
    val time: FieldLine,
    /** A has a 때 that B can share: the row offers `같은 때 · 때 없음`. */
    val timeChoice: Boolean,
    val titleLine: FieldLine,
    val silentRows: List<SilentRow>,
    val flashbackRow: Boolean,
    val hiddenRow: Boolean,
    val changes: List<ChangeLine>,
    val canApply: Boolean,
)

/**
 * `씬 나누기` (SCR-50) as plain data. The write path is `domain:actions` `SceneActions.split`
 * (DECISIONS §1); the part-card excerpts reuse `domain:script` `SceneSplit`'s display functions only.
 * Nothing changes before [apply], which recomputes on the document it writes (a stale sheet writes nothing).
 */
object SplitModel {
    /** The sheet for a split below [blockKey]; null when the block is gone or is the last of its scene. */
    fun of(doc: ProjectDoc, blockKey: Key, choices: SplitChoices, unlocked: Boolean, readOnly: Boolean, ctx: ActionContext): SplitSheetModel? {
        val (block, blocks) = splitPoint(doc, blockKey) ?: return null
        val preview = plan(doc, block, choices, ctx) ?: return null
        val cut = blocks.indexOfFirst { it.key == blockKey } + 1
        val stay = blocks.subList(0, cut)
        val move = blocks.subList(cut, blocks.size)
        val (aTitle, aCount) = halves(preview.firstPart)
        val (bTitle, bCount) = halves(preview.newPart)
        val placeLine = preview.placeLine?.removePrefix("${SceneCopy.FIELD_PLACE} · ")
        val place = when (choices.place) {
            PlaceMode.SAME -> placeLine?.let(::valueAndNote) ?: FieldLine(SceneCopy.NO_PLACE)
            PlaceMode.CLEARED -> FieldLine(SceneCopy.NO_PLACE)
            PlaceMode.PICKED -> FieldLine(PlaceNames(doc).nameOrNone(choices.picked))
        }
        val placeSet = when (choices.place) {
            PlaceMode.SAME -> preview.placeLine != null
            PlaceMode.CLEARED -> false
            PlaceMode.PICKED -> choices.picked != null
        }
        val timeLine = preview.timeLine?.removePrefix("${SceneCopy.FIELD_TIME} · ")
        val timeChoice = timeLine != null && doc.index.unit(block.sceneKey)?.time !is SceneTime.Flashback
        val time = if (timeLine == null || (timeChoice && !choices.sameTime)) FieldLine(SceneCopy.NO_TIME) else FieldLine(timeLine)
        val n = Labels.sceneOrdinal(doc, block.sceneKey) ?: 0
        val sceneWord = aTitle.substringBeforeLast(' ')
        val chips = listOf(aTitle, bTitle, BOTH)
        val silentRows = preview.silentCast.map { key ->
            val selected = when (choices.silentCast[key] ?: SilentCastChoice.BOTH) {
                SilentCastChoice.FIRST -> 0
                SilentCastChoice.NEW -> 1
                SilentCastChoice.BOTH -> 2
            }
            SilentRow(key, "$SILENT · ${characterName(doc, key, unlocked)}", chips, selected)
        }
        val order = preview.changeRows.filter { it.startsWith(ORDER) }
        val dependents = preview.changeRows.filterNot { it.startsWith(ORDER) }
        val records = recordRows(doc, stay, move, Labels.episodeCodeOfScene(doc, block.sceneKey), n, sceneWord, unlocked)
        return SplitSheetModel(
            title = preview.title,
            sub = preview.sub,
            parts = listOf(
                PartCard(aTitle, aCount, range(doc, stay, unlocked), isNew = false),
                PartCard(bTitle, bCount, range(doc, move, unlocked), isNew = true),
            ),
            place = place,
            placeClearable = placeSet,
            time = time,
            timeChoice = timeChoice,
            titleLine = FieldLine(choices.title.trim().ifEmpty { SceneCopy.UNTITLED }),
            silentRows = silentRows,
            flashbackRow = preview.flashbackRow,
            hiddenRow = preview.hiddenRow,
            changes = (order + records + dependents).map(ChangeLine::of),
            canApply = !readOnly,
        )
    }

    /**
     * `나누기`: [SceneActions.split] on [doc], then the new scene's 장소 · 제목 from the sheet and, with
     * `때 없음`, its 때 cleared (`TimeChange`, `이 씬만`; B has no dependents yet). One result = one undo
     * step. Returns [doc] unchanged when the split no longer applies.
     */
    fun apply(doc: ProjectDoc, blockKey: Key, choices: SplitChoices, ctx: ActionContext): ProjectDoc {
        val (block, _) = splitPoint(doc, blockKey) ?: return doc
        val preview = plan(doc, block, choices, ctx) ?: return doc
        val key = preview.newSceneKey
        val title = choices.title.trim().ifEmpty { null }
        var result = preview.result
        if (choices.place != PlaceMode.SAME || title != null) {
            val place = when (choices.place) {
                PlaceMode.SAME -> null
                PlaceMode.CLEARED -> null
                PlaceMode.PICKED -> choices.picked
            }
            result = result.copy(
                story = result.story.copy(
                    units = result.story.units.map { u ->
                        if (u.key != key) u
                        else u.copy(place = if (choices.place == PlaceMode.SAME) u.place else place, title = title ?: u.title)
                    },
                ),
            )
        }
        val bTime = result.index.unit(key)?.time
        if (!choices.sameTime && bTime != null && bTime !is SceneTime.None && bTime !is SceneTime.Flashback) {
            result = TimeChange.apply(result, key, SceneTime.None, CascadeMode.THIS_ONLY, ctx.edit)
        }
        return result
    }

    /** The block and its scene's live blocks, when a split below it leaves blocks for a new scene. */
    private fun splitPoint(doc: ProjectDoc, blockKey: Key): Pair<Block, List<Block>>? {
        val block = doc.index.block(blockKey)?.takeIf { doc.manuscript.isLiveBlock(it.key) } ?: return null
        val blocks = doc.manuscript.liveBlocksOf(block.sceneKey)
        if (blocks.lastOrNull()?.key == blockKey) return null
        return block to blocks
    }

    private fun plan(doc: ProjectDoc, block: Block, choices: SplitChoices, ctx: ActionContext): SplitPreview? = runCatching {
        SceneActions.split(doc, block.sceneKey, block.key, ctx, choices.silentCast, choices.flashback, choices.hidden)
    }.getOrNull()

    /** `씬 3 · 블록 9개` → (`씬 3`, `블록 9개`). */
    private fun halves(text: String): Pair<String, String> {
        val i = text.lastIndexOf(" · ")
        return if (i < 0) text to "" else text.substring(0, i) to text.substring(i + 3)
    }

    /** `결사의 문 · 씬 3과 같음` → value `결사의 문`, note `· 씬 3과 같음`. */
    private fun valueAndNote(line: String): FieldLine {
        val i = line.lastIndexOf(" · ")
        return if (i < 0) FieldLine(line) else FieldLine(line.substring(0, i), line.substring(i + 1))
    }

    /** `‘세라 · 불은 꺼졌어…’ ~ ‘…한 번 깜박였다.’`, with a locked hidden speaker masked (rule 19). */
    fun range(doc: ProjectDoc, blocks: List<Block>, unlocked: Boolean): String {
        if (blocks.isEmpty()) return ""
        return "‘${leading(doc, blocks.first(), unlocked)}’ ~ ‘${ScriptExcerpts.trailingExcerpt(doc, blocks.last())}’"
    }

    private fun leading(doc: ProjectDoc, block: Block, unlocked: Boolean): String {
        val speaker = block.speakerKey?.takeIf { block.kind.known == BlockKind.DIALOGUE }
        if (speaker == null || !isLockedHidden(doc, speaker, unlocked)) return ScriptExcerpts.leadingExcerpt(doc, block)
        return "${SceneCopy.HIDDEN_SPEAKER} · ${ScriptExcerpts.leadingExcerpt(doc, block.copy(speakerKey = null))}"
    }

    private fun characterName(doc: ProjectDoc, key: Key, unlocked: Boolean): String =
        if (isLockedHidden(doc, key, unlocked)) SceneCopy.HIDDEN_SPEAKER else Labels.speakerName(doc, key) ?: "?"

    private fun isLockedHidden(doc: ProjectDoc, characterKey: Key, unlocked: Boolean): Boolean =
        !unlocked && doc.index.character(characterKey)?.hidden == true

    /**
     * Records on the split scene's blocks (T-22-09, T-22-10): first reveals, then `◆` records, each in
     * block order. A record on a block that stays reads `첫 공개 · 오리지늄 등불 · 씬 3 #044 그대로` /
     * `◆ 관계 · 리엔 → 하르트 · #043 그대로`; one on a block that moves reads
     * `◆ 관계 · 1-7 씬 3 #043 → 1-7 씬 4 #043`. Subjects naming a locked hidden item are masked.
     */
    fun recordRows(doc: ProjectDoc, stay: List<Block>, move: List<Block>, code: String, n: Int, sceneWord: String, unlocked: Boolean): List<String> {
        val blocks = stay + move
        val moved = move.mapTo(HashSet()) { it.key }
        val byBlock = blocks.associate { b ->
            b.key to doc.changes.records.filter { r ->
                r.deletedAt == null && (r.anchor as? Anchor.AtBlock)?.blockKey == b.key && doc.liveness.isLive(r)
            }
        }
        fun rows(reveals: Boolean): List<String> = blocks.flatMap { b ->
            byBlock.getValue(b.key).filter { (it.target is ChangeTarget.TermReveal) == reveals }.map { r ->
                row(doc, r, b, code, n, sceneWord, b.key in moved, unlocked)
            }
        }
        return rows(reveals = true) + rows(reveals = false)
    }

    private fun row(doc: ProjectDoc, r: ChangeRecord, block: Block, code: String, n: Int, word: String, moves: Boolean, unlocked: Boolean): String {
        val number = Labels.number(block.number)
        val reveal = r.target is ChangeTarget.TermReveal
        val head = if (reveal) REVEAL else "◆ ${Records.kindLabel(doc, r)}"
        val subject = if (involvesLockedHidden(doc, r, unlocked)) SceneCopy.HIDDEN_ITEM else Records.subject(doc, r)
        return when {
            moves -> "$head · $code $word $n $number → $code $word ${n + 1} $number"
            reveal -> "$head · $subject · $word $n $number 그대로"
            else -> "$head · $subject · $number 그대로"
        }
    }

    private fun involvesLockedHidden(doc: ProjectDoc, r: ChangeRecord, unlocked: Boolean): Boolean {
        if (unlocked) return false
        val keys = when (val t = r.target) {
            is ChangeTarget.RelationSlot -> doc.index.relation(t.relationKey)?.let { listOf(it.endA.key, it.endB.key) }.orEmpty()
            is ChangeTarget.Field -> listOf(t.entityKey)
            is ChangeTarget.DisplayName -> listOf(t.characterKey)
            is ChangeTarget.TermReveal -> listOf(t.termKey)
            else -> emptyList()
        }
        return keys.any { k -> doc.index.character(k)?.hidden == true || doc.index.term(k)?.hidden == true }
    }

    private const val ORDER = "순번 · "
    private const val REVEAL = "첫 공개"
    private const val SILENT = "말 없는 등장"
    private const val BOTH = "둘 다"
}
