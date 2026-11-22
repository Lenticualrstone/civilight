package io.github.lenticualrstone.civilight.feature.script.state

import io.github.lenticualrstone.civilight.domain.script.BlockOps
import io.github.lenticualrstone.civilight.domain.script.BlockPosition
import io.github.lenticualrstone.civilight.domain.script.Labels
import io.github.lenticualrstone.civilight.domain.script.Links
import io.github.lenticualrstone.civilight.domain.script.Manuscript
import io.github.lenticualrstone.civilight.domain.script.Runs
import io.github.lenticualrstone.civilight.domain.script.SceneTimeSource
import io.github.lenticualrstone.civilight.feature.script.editor.LinkLook
import io.github.lenticualrstone.civilight.feature.script.editor.SpanKind
import io.github.lenticualrstone.civilight.model.Key
import io.github.lenticualrstone.civilight.model.doc.ProjectDoc
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

/** One option line of a choice block: `1` + text. */
data class OptionLine(val index: String, val text: String)

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
) : ScriptRow {
    override val key: String get() = ScriptRowKeys.block(blockKey)

    /** Text kinds are typed into; direction and choice blocks are only selected in this batch. */
    val isText: Boolean get() = kind in BlockOps.TEXT_KINDS
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
    ): List<ScriptRow> {
        val out = mutableListOf<ScriptRow>()
        val rows = Manuscript.rows(doc, unitKey, time, layers, preview, unlocked)
        for (row in rows) {
            when (row) {
                is Manuscript.Row.Head -> {
                    out += HeadRow(row.sceneKey, row.text, row.transitionLine, row.hiddenTag, Labels.sceneOrdinal(doc, row.sceneKey) ?: 0)
                    val empty = doc.manuscript.liveBlocksOf(row.sceneKey).isEmpty()
                    if (empty && !readOnly && preview == ScriptPreview.WRITE) {
                        out += EmptySceneRow(row.sceneKey, inheritedSpeaker(doc, row.sceneKey))
                    }
                }
                is Manuscript.Row.HiddenScene -> out += HiddenSceneRow(row.sceneKey, row.text)
                is Manuscript.Row.BlockRow -> out += blockRow(doc, row, layers, unlocked, zone)
            }
        }
        return out
    }

    /** The speaker a first line of [sceneKey] inherits (SCR-18), as a label. */
    fun inheritedSpeaker(doc: ProjectDoc, sceneKey: Key): SpeakerModel? {
        val key = BlockOps.inheritedSpeaker(doc, BlockPosition.SceneStart(sceneKey)) ?: return null
        val name = Labels.speakerName(doc, key) ?: return null
        return SpeakerModel(name, null, SpeakerLook.NAMED, key)
    }

    private fun blockRow(doc: ProjectDoc, row: Manuscript.Row.BlockRow, layers: ScriptLayers, unlocked: Boolean, zone: ZoneId): BlockRow {
        val block = row.block
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
                OptionLine("${i + 1}", if (layers.paren) Runs.displayText(o.runs) else Runs.exportText(o.runs, includeParens = false))
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
            text = row.text,
            memoPrefix = if (kind == BlockKind.MEMO) Manuscript.memoPrefix(block, zone) else null,
            options = options,
            speakerMarks = speakerMarks,
            textMarks = textMarks,
            addressHints = row.addressHints,
        )
    }

    /** Domain label → model; an empty dialogue block without a speaker reads `화자 없음` neutrally. */
    fun speakerOf(label: Manuscript.SpeakerLabel?, block: Block): SpeakerModel = when (label) {
        is Manuscript.SpeakerLabel.Named ->
            SpeakerModel(label.displayName ?: label.name, label.name.takeIf { label.displayName != null }, SpeakerLook.NAMED, block.speakerKey)
        Manuscript.SpeakerLabel.Missing -> SpeakerModel(Manuscript.SpeakerLabel.Missing.TEXT, null, SpeakerLook.MISSING, null)
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
