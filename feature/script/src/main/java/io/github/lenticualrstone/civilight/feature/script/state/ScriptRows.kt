package io.github.lenticualrstone.civilight.feature.script.state

import io.github.lenticualrstone.civilight.feature.script.terms.BlockTerms
import io.github.lenticualrstone.civilight.feature.script.terms.CandidateSpan
import io.github.lenticualrstone.civilight.feature.script.terms.TermMarksModel
import io.github.lenticualrstone.civilight.domain.script.BlockOps
import io.github.lenticualrstone.civilight.domain.script.BlockPosition
import io.github.lenticualrstone.civilight.domain.script.DirectionCues
import io.github.lenticualrstone.civilight.domain.script.Labels
import io.github.lenticualrstone.civilight.domain.script.Links
import io.github.lenticualrstone.civilight.domain.script.Manuscript
import io.github.lenticualrstone.civilight.domain.script.Records
import io.github.lenticualrstone.civilight.domain.script.Runs
import io.github.lenticualrstone.civilight.domain.script.SceneTimeSource
import io.github.lenticualrstone.civilight.feature.script.ScriptCopy
import io.github.lenticualrstone.civilight.feature.script.editor.LinkLook
import io.github.lenticualrstone.civilight.feature.script.editor.SpanKind
import io.github.lenticualrstone.civilight.model.Key
import io.github.lenticualrstone.civilight.model.doc.ProjectDoc
import io.github.lenticualrstone.civilight.model.game.GameTools
import io.github.lenticualrstone.civilight.domain.script.ReadAloud
import io.github.lenticualrstone.civilight.model.script.Block
import io.github.lenticualrstone.civilight.model.script.BlockKind
import io.github.lenticualrstone.civilight.model.script.ScriptLayers
import io.github.lenticualrstone.civilight.model.script.ScriptPreview
import io.github.lenticualrstone.civilight.model.text.TextRun
import java.time.ZoneId

/** Stable list keys of manuscript rows. */
object ScriptRowKeys {
    fun head(sceneKey: Key): String = "head:$sceneKey"
    fun hidden(sceneKey: Key): String = "hidden:$sceneKey"
    fun empty(sceneKey: Key): String = "empty:$sceneKey"
    fun block(blockKey: Key): String = "block:$blockKey"

    /** The block key of a block row's key, else null (heads and scene rows). */
    fun blockKeyOf(rowKey: String): Key? = rowKey.removePrefix("block:").takeIf { it != rowKey }
}

/** One row of the manuscript list (SCR-01): scene heads and blocks of one episode, in order. */
sealed interface ScriptRow {
    /** Stable list key. */
    val key: String
    val sceneKey: Key
}

/** Scene head (SCR-46): head text, transition line (null while the `시스템 텍스트` layer is off). */
data class HeadRow(
    override val sceneKey: Key,
    val title: String,
    val transitionLine: String?,
    val hiddenTag: Boolean,
    val ordinal: Int,
) : ScriptRow {
    override val key: String get() = ScriptRowKeys.head(sceneKey)
}

/** A locked hidden scene: `숨긴 씬 · 블록 12 · 지문으로 보기` (rule 19). */
data class HiddenSceneRow(override val sceneKey: Key, val text: String) : ScriptRow {
    override val key: String get() = ScriptRowKeys.hidden(sceneKey)
}

/**
 * The empty dialogue line of a scene without blocks (SCR-01 `[PROPOSED]`). It stays virtual until
 * the first character, so opening a scene burns no number and adds no undo step.
 */
data class EmptySceneRow(override val sceneKey: Key, val speaker: SpeakerModel?) : ScriptRow {
    override val key: String get() = ScriptRowKeys.empty(sceneKey)
}

/** How a speaker label reads (SCR-20, rules 19 and 20). */
enum class SpeakerLook {
    /** A live character; [SpeakerModel.realName] is set when the display name differs. */
    NAMED,

    /** `화자 없음` in `warn` with `▲` (a non-empty dialogue block without a speaker). */
    MISSING,

    /** `화자 없음` on an empty dialogue block (raises nothing, Q-34). */
    UNSET,

    /** `숨긴 인물`. */
    HIDDEN,

    /** A trashed or purged speaker: dotted underline and `◇`. */
    DELETED,
}

data class SpeakerModel(val text: String, val realName: String?, val look: SpeakerLook, val characterKey: Key?)

/** A `◆` / `◇` / `▲` mark; [dotted] when its anchor or target was deleted. */
data class MarkModel(val glyph: Char, val dotted: Boolean = false)

/** One option line of a choice block: `1` + text; [number] `#045-1` is shown while the block is selected (SCR-21). */
data class OptionLine(val index: String, val text: String, val number: String = index, val optionKey: Key? = null)

/**
 * The speaker-line slot of a block that has no speaker (boards `Blocks`, `RSSelect`): `선택지 ·
 * 다음 블록에서 합류`, `시스템 · 화면 문구`, and on a selected block its kind label with `· #043`
 * (SCR-21: the number shows on the selected block only).
 */
data class SlotModel(val label: String, val sub: String?, val number: String?)

/** A block as the manuscript draws it (SCR-04 … SCR-11, SCR-20). */
data class BlockRow(
    override val sceneKey: Key,
    val blockKey: Key,
    /** Null for a kind from a newer file (shown read-only as text). */
    val kind: BlockKind?,
    /** `#043`. */
    val number: String,
    val speaker: SpeakerModel?,
    /** Text runs of dialogue, narration, system text and memos. */
    val runs: List<TextRun>,
    /** How each link run is drawn, by link key. */
    val linkLooks: Map<Key, LinkLook>,
    /** Drawn text of a direction block or an unknown kind. */
    val text: String,
    /** `⌁ 작가 메모` (with its recorder source) for a memo. */
    val memoPrefix: String?,
    val options: List<OptionLine>,
    /** Marks at the right end of the speaker line (dialogue `◆`, `▲ 화자 없음`). */
    val speakerMarks: List<MarkModel>,
    /** Marks at the end of the text line (`◇`, and `◆` of other kinds). */
    val textMarks: List<MarkModel>,
    /** `호칭 힌트` lines (layer on). */
    val addressHints: List<String>,
    /** The block carries a `◆` (whatever the layers): the swipe row's `◆ 시간` is `acc` (SCR-30). */
    val hasChange: Boolean = false,
    /** `용어 후보` spans in raw offsets (SCR-56; empty while the layer is off). */
    val termCandidates: List<CandidateSpan> = emptyList(),
    /** A link here sits before its term's first reveal: the amber `▲` in the margin (SCR-56, always drawn). */
    val preReveal: Boolean = false,
    /** The memo was put in by the Life Recorder: its source record (SCR-59). */
    val recorderRecordKey: Key? = null,
    /** (0.2.0) `고치기` is not offered: it would show hidden text in the field ([ScriptRows.editLocked]). */
    val editLocked: Boolean = false,
) : ScriptRow {
    override val key: String get() = ScriptRowKeys.block(blockKey)

    /** Text kinds are typed into; direction blocks open their cue sheet; choice options are typed into one by one. */
    val isText: Boolean get() = kind in BlockOps.TEXT_KINDS

    /** The slot line drawn above the text ([selected] adds the number), or null (dialogue, unselected narration and memo). */
    fun slot(selected: Boolean): SlotModel? = ScriptRows.slotOf(kind, number, selected, empty = kind == BlockKind.DIRECTION && text.isBlank())
}

/** Builds the manuscript rows of one numbering unit from the domain rows (`Manuscript.rows`). */
object ScriptRows {
    fun build(
        doc: ProjectDoc,
        unitKey: Key,
        time: SceneTimeSource,
        layers: ScriptLayers,
        preview: ScriptPreview,
        unlocked: Boolean,
        readOnly: Boolean,
        zone: ZoneId,
        terms: TermMarksModel.Source? = null,
    ): List<ScriptRow> {
        val out = mutableListOf<ScriptRow>()
        val rows = Manuscript.rows(doc, unitKey, time, layers, preview, unlocked)
        for (row in rows) {
            when (row) {
                is Manuscript.Row.Head -> {
                    out += HeadRow(row.sceneKey, HeadTitles.of(doc, row.sceneKey, row.text), row.transitionLine, row.hiddenTag, Labels.sceneOrdinal(doc, row.sceneKey) ?: 0)
                    // (0.2.0, SCR-01) An empty scene draws only its head; a send writes its first block.
                }
                is Manuscript.Row.HiddenScene -> out += HiddenSceneRow(row.sceneKey, row.text)
                is Manuscript.Row.BlockRow -> out += blockRow(doc, row, layers, unlocked, zone, terms)
            }
        }
        return out
    }

    /**
     * Slot line by kind (SCR-21, SCR-09, SCR-10): choice and system text always carry theirs, the
     * number only while selected; narration, direction and memo show their kind label with the
     * number only while selected; dialogue uses its speaker line instead. An [empty] direction block
     * (no cue yet) keeps its `연출` label unselected too, so the numbered row can still be found.
     */
    fun slotOf(kind: BlockKind?, number: String, selected: Boolean, empty: Boolean = false): SlotModel? {
        val n = number.takeIf { selected }
        return when (kind) {
            BlockKind.DIALOGUE -> null
            BlockKind.CHOICE -> SlotModel(ScriptCopy.CHOICE_LABEL, ScriptCopy.CHOICE_REJOIN, n)
            BlockKind.SYSTEM -> SlotModel(ScriptCopy.SYSTEM_LABEL, ScriptCopy.SYSTEM_SUB, n)
            BlockKind.NARRATION, BlockKind.DIRECTION, BlockKind.MEMO, null ->
                if (selected || empty) SlotModel(Labels.kindLabel(kind), null, n) else null
        }
    }

    /** The speaker a first line of [sceneKey] inherits (SCR-18), as a label. */
    fun inheritedSpeaker(doc: ProjectDoc, sceneKey: Key): SpeakerModel? {
        val key = BlockOps.inheritedSpeaker(doc, BlockPosition.SceneStart(sceneKey)) ?: return null
        val name = Labels.speakerName(doc, key) ?: return null
        return SpeakerModel(name, null, SpeakerLook.NAMED, key)
    }

    private fun blockRow(doc: ProjectDoc, row: Manuscript.Row.BlockRow, layers: ScriptLayers, unlocked: Boolean, zone: ZoneId, terms: TermMarksModel.Source?): BlockRow {
        val block = row.block
        val termMarks = terms?.of(block) ?: BlockTerms.NONE
        val kind = block.kind.known
        val speaker = if (kind == BlockKind.DIALOGUE) speakerOf(row.speaker, block) else null
        val marks = row.marks.map { MarkModel(it.glyph.first(), it.dotted) }
        val speakerMarks = mutableListOf<MarkModel>()
        val textMarks = mutableListOf<MarkModel>()
        for (mark in marks) {
            if (kind == BlockKind.DIALOGUE && mark.glyph == '◆') speakerMarks += mark else textMarks += mark
        }
        if (speaker?.look == SpeakerLook.MISSING) speakerMarks += MarkModel('▲')
        val options = if (kind == BlockKind.CHOICE) {
            BlockOps.orderedOptions(block).mapIndexed { i, o ->
                OptionLine(
                    index = "${i + 1}",
                    // SCR-55: a link to a locked hidden item reads `숨긴 인물` / `숨긴 항목`.
                    text = Runs.shownText(doc, o.runs, unlocked, includeParens = layers.paren),
                    number = Labels.optionNumber(block.number, i + 1),
                    optionKey = o.key,
                )
            }
        } else emptyList()
        return BlockRow(
            sceneKey = block.sceneKey,
            blockKey = block.key,
            kind = kind,
            number = Labels.number(block.number),
            speaker = speaker,
            runs = block.runs,
            linkLooks = linkLooks(doc, block, unlocked),
            // SCR-08: cues read with Korean effect names (`화면 효과 페이드`).
            // (0.2.0, SCR-08) The 연출 text line; the cue summary with `게임 제작 도구` on, or alone on a cue-only block.
            text = if (kind == BlockKind.DIRECTION) "▸ " + ReadAloud.directionText(doc, block, GameTools.on(doc), unlocked) else row.text,
            memoPrefix = if (kind == BlockKind.MEMO) Manuscript.memoPrefix(block, zone) else null,
            options = options,
            speakerMarks = speakerMarks,
            textMarks = textMarks,
            addressHints = row.addressHints,
            hasChange = Records.marksOn(doc, block.key).any { it.glyph == "◆" },
            termCandidates = termMarks.candidates,
            preReveal = termMarks.preReveal,
            recorderRecordKey = block.recorderSource?.recordKey.takeIf { kind == BlockKind.MEMO },
            editLocked = editLocked(doc, block, unlocked),
        )
    }

    /**
     * `고치기` would put hidden text into the field: the block links a hidden item, or its speaker is hidden,
     * while the gate is locked (SCR-55, rule 19). The swipe drops its `고치기` button then.
     */
    fun editLocked(doc: ProjectDoc, block: Block, unlocked: Boolean): Boolean {
        if (unlocked) return false
        if (block.speakerKey?.let { doc.index.character(it) }?.hidden == true) return true
        return Runs.blockHidesLink(doc, block, unlocked)
    }

    /** Domain label → model; an empty dialogue block without a speaker reads `화자 없음` neutrally. */
    fun speakerOf(label: Manuscript.SpeakerLabel?, block: Block): SpeakerModel = when (label) {
        // (0.2.0, N-43, SCR-20) A per-block display name wins: `경비병 (하르트)`.
        is Manuscript.SpeakerLabel.Named -> block.displayNameOverride?.takeIf { it.isNotBlank() }?.let { own ->
            SpeakerModel(own, "(${label.name})", SpeakerLook.NAMED, block.speakerKey)
        } ?: SpeakerModel(label.displayName ?: label.name, label.name.takeIf { label.displayName != null }, SpeakerLook.NAMED, block.speakerKey)
        Manuscript.SpeakerLabel.Missing -> block.displayNameOverride?.takeIf { it.isNotBlank() }?.let { own ->
            SpeakerModel(own, "(${Manuscript.SpeakerLabel.Missing.TEXT})", SpeakerLook.MISSING, null)
        } ?: SpeakerModel(Manuscript.SpeakerLabel.Missing.TEXT, null, SpeakerLook.MISSING, null)
        Manuscript.SpeakerLabel.Hidden -> SpeakerModel(Manuscript.SpeakerLabel.Hidden.TEXT, null, SpeakerLook.HIDDEN, block.speakerKey)
        is Manuscript.SpeakerLabel.Deleted -> SpeakerModel(label.text, null, SpeakerLook.DELETED, block.speakerKey)
        null -> SpeakerModel(Manuscript.SpeakerLabel.Missing.TEXT, null, SpeakerLook.UNSET, null)
    }

    /** Link looks of a block's runs and options (live, masked hidden, deleted). */
    fun linkLooks(doc: ProjectDoc, block: Block, unlocked: Boolean): Map<Key, LinkLook> {
        val links = (block.runs + block.options.flatMap { it.runs }).filterIsInstance<TextRun.Link>()
        if (links.isEmpty()) return emptyMap()
        return links.associate { link ->
            link.linkKey to when (val state = Links.state(doc, link, unlocked)) {
                Links.State.Live -> LinkLook(SpanKind.LINK)
                is Links.State.Hidden -> LinkLook(SpanKind.HIDDEN_LINK, state.text)
                is Links.State.Deleted, is Links.State.Purged -> LinkLook(SpanKind.DELETED_LINK)
            }
        }
    }
}
