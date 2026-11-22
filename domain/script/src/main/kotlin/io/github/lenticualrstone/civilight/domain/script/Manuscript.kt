package io.github.lenticualrstone.civilight.domain.script

import io.github.lenticualrstone.civilight.model.Key
import io.github.lenticualrstone.civilight.model.cast.EntityRef
import io.github.lenticualrstone.civilight.model.cast.RelationEndKind
import io.github.lenticualrstone.civilight.model.change.ChangeRecord
import io.github.lenticualrstone.civilight.model.change.ChangeTarget
import io.github.lenticualrstone.civilight.model.change.RelationSlotName
import io.github.lenticualrstone.civilight.model.doc.ProjectDoc
import io.github.lenticualrstone.civilight.model.doc.recordBeforeBlock
import io.github.lenticualrstone.civilight.model.doc.recordsHere
import io.github.lenticualrstone.civilight.model.open
import io.github.lenticualrstone.civilight.model.script.Block
import io.github.lenticualrstone.civilight.model.script.BlockKind
import io.github.lenticualrstone.civilight.model.script.CueKind
import io.github.lenticualrstone.civilight.model.script.ScriptLayers
import io.github.lenticualrstone.civilight.model.script.ScriptPreview
import io.github.lenticualrstone.civilight.model.text.TextRun
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull

/**
 * What the manuscript draws (SCR-01, 04–11, 20, 26–29, 46, 57, 58): rows of scene heads and blocks
 * filtered by the `표시` layers and the preview mode. Layers only change what is drawn.
 */
object Manuscript {
    /** How a speaker label reads (SCR-20/57/58, §3 resolution order: hidden first). */
    sealed interface SpeakerLabel {
        /** `하르트`, or `??? 하르트` when the display name in effect differs from the name. */
        data class Named(val name: String, val displayName: String?) : SpeakerLabel {
            val text: String get() = if (displayName != null) "$displayName $name" else name
        }

        /** `화자 없음` in `warn` with `▲` (a non-empty dialogue block without a speaker). */
        data object Missing : SpeakerLabel {
            const val TEXT = "화자 없음"
        }

        /** `숨긴 인물` (locked hidden speaker). */
        data object Hidden : SpeakerLabel {
            const val TEXT = "숨긴 인물"
        }

        /** Trashed or purged speaker: dotted underline + `◇`; purged reads `영구 삭제 · 노아`. */
        data class Deleted(val name: String, val purged: Boolean) : SpeakerLabel {
            val text: String get() = if (purged) "영구 삭제 · $name" else name
        }
    }

    /** The display name in effect at a block, inclusive (a `◆` on the block applies from it). */
    fun displayNameAt(doc: ProjectDoc, characterKey: Key, blockKey: Key): String? {
        val character = doc.index.character(characterKey) ?: return null
        val target = ChangeTarget.DisplayName(characterKey)
        val record = doc.recordsHere(blockKey).lastOrNull { it.target.sameAs(target) } ?: doc.recordBeforeBlock(target, blockKey)
        val value = if (record != null) stringValue(record) else character.displayNameBase
        return value ?: character.name
    }

    private fun stringValue(record: ChangeRecord): String? = record.valueAs(String.serializer())

    /** The label of a dialogue block's speaker; [unlocked] = the hidden gate is open. */
    fun speakerLabel(doc: ProjectDoc, block: Block, unlocked: Boolean = false): SpeakerLabel? {
        if (block.kind.known != BlockKind.DIALOGUE) return null
        val key = block.speakerKey ?: return SpeakerLabel.Missing.takeIf { block.hasContent() }
        val character = doc.index.character(key)
        val tag = doc.index.nameTag(key)
        if ((character?.hidden == true || tag?.hidden == true) && !unlocked) return SpeakerLabel.Hidden
        if (character == null) return SpeakerLabel.Deleted(tag?.name ?: "?", purged = true)
        if (character.deletedAt != null) return SpeakerLabel.Deleted(character.name, purged = false)
        val display = displayNameAt(doc, key, block.key)
        return SpeakerLabel.Named(character.name, display?.takeIf { it != character.name })
    }

    /** Speaker line with the number (selected block, panels): `??? 하르트 · #043`. */
    fun speakerLineWithNumber(doc: ProjectDoc, block: Block): String {
        val label = when (val l = speakerLabel(doc, block, unlocked = true)) {
            is SpeakerLabel.Named -> l.text
            is SpeakerLabel.Deleted -> l.text
            SpeakerLabel.Hidden -> SpeakerLabel.Hidden.TEXT
            SpeakerLabel.Missing -> SpeakerLabel.Missing.TEXT
            null -> Labels.kindLabel(block.kind.known)
        }
        return "$label · ${Labels.number(block.number)}"
    }

    /** Direction cues joined with [separator]: `▸ 배경 하층 거리(새벽) ♪ 잔불` (select mode: ` · `). */
    fun cueText(doc: ProjectDoc, block: Block, separator: String = " "): String = block.cues.joinToString(separator) { cue ->
        val name = cue.assetKey?.let { doc.index.scriptAsset(it)?.name } ?: cue.effect.orEmpty()
        when (cue.kind.known) {
            CueKind.BACKGROUND -> "▸ 배경 $name"
            CueKind.BGM -> "♪ $name"
            CueKind.SFX -> "효과음 $name"
            CueKind.CG -> "CG $name"
            CueKind.SCREEN_EFFECT -> "화면 효과 $name"
            null -> name
        }
    }

    /** Memo line: `⌁ 작가 메모  {text}`, or `⌁ 작가 메모 · 레코더 11월 22일 08:41  {text}` with a recorder source. */
    fun memoPrefix(block: Block, zone: java.time.ZoneId): String =
        block.recorderSource?.let { "⌁ 작가 메모 · 레코더 ${Labels.monthDayTime(it.recordedAt, zone)}" } ?: "⌁ 작가 메모"

    sealed interface Row {
        /** Scene head; [transitionLine] is null while the `시스템 텍스트` layer is off. */
        data class Head(val sceneKey: Key, val text: String, val transitionLine: String?, val hiddenTag: Boolean) : Row

        /** A locked hidden scene: `숨긴 씬 · 블록 12 · 지문으로 보기`. */
        data class HiddenScene(val sceneKey: Key, val text: String) : Row

        data class BlockRow(
            val block: Block,
            val speaker: SpeakerLabel?,
            /** Drawn text (parens dropped when the layer is off). */
            val text: String,
            val marks: List<Records.Mark>,
            /** `호칭 힌트` lines (layer on): `리엔을 ‘꼬마’`. */
            val addressHints: List<String>,
        ) : Row
    }

    /** Whether a block kind is drawn under the layers. */
    fun isShown(kind: BlockKind?, layers: ScriptLayers, preview: ScriptPreview = ScriptPreview.WRITE): Boolean {
        if (preview == ScriptPreview.DIALOGUE_ONLY) return kind == BlockKind.DIALOGUE
        return when (kind) {
            BlockKind.DIRECTION -> layers.direction
            BlockKind.MEMO -> layers.memo
            BlockKind.SYSTEM -> layers.system
            else -> true
        }
    }

    /** `[PROPOSED]`: inserting a block of a hidden kind turns that layer on. */
    fun layersAfterInsert(layers: ScriptLayers, kind: BlockKind): ScriptLayers = when (kind) {
        BlockKind.DIRECTION -> layers.copy(direction = true)
        BlockKind.MEMO -> layers.copy(memo = true)
        BlockKind.SYSTEM -> layers.copy(system = true)
        else -> layers
    }

    /** Text of a block as drawn. */
    fun blockText(doc: ProjectDoc, block: Block, layers: ScriptLayers): String = when (block.kind.known) {
        BlockKind.DIRECTION -> cueText(doc, block)
        BlockKind.CHOICE -> BlockOps.orderedOptions(block).mapIndexed { i, o -> "${i + 1} ${render(o.runs, layers)}" }.joinToString("\n")
        else -> render(block.runs, layers)
    }

    private fun render(runs: List<TextRun>, layers: ScriptLayers): String =
        if (layers.paren) Runs.displayText(runs) else Runs.exportText(runs, includeParens = false)

    /**
     * Rows of one numbering unit (an episode) in manuscript order. Scene heads are always drawn; a
     * locked hidden scene becomes one row; `대사만` keeps only dialogue rows and heads (`낭독` is
     * disabled, Q-08, and draws like `쓰기`).
     */
    fun rows(
        doc: ProjectDoc,
        numberingUnitKey: Key,
        time: SceneTimeSource,
        layers: ScriptLayers = ScriptLayers(),
        preview: ScriptPreview = ScriptPreview.WRITE,
        unlocked: Boolean = false,
    ): List<Row> {
        val out = mutableListOf<Row>()
        for (scene in doc.manuscript.liveScenes.filter { doc.numberingUnitOfScene(it.key) == numberingUnitKey }) {
            val blocks = doc.manuscript.liveBlocksOf(scene.key)
            if (scene.hidden && !unlocked) {
                out += Row.HiddenScene(scene.key, "숨긴 씬 · 블록 ${blocks.size} · 지문으로 보기")
                continue
            }
            val t = time.timeOf(scene.key)
            out += Row.Head(
                scene.key, SceneText.headText(doc, scene.key, t),
                SceneText.transitionLine(doc, scene, t).takeIf { layers.system && preview != ScriptPreview.DIALOGUE_ONLY },
                hiddenTag = scene.hidden,
            )
            for (b in blocks) {
                if (!isShown(b.kind.known, layers, preview)) continue
                out += Row.BlockRow(
                    b, speakerLabel(doc, b, unlocked), blockText(doc, b, layers), Records.marksOn(doc, b.key, layers),
                    if (layers.addressHint) addressHints(doc, b) else emptyList(),
                )
            }
        }
        return out
    }

    // --- 호칭 힌트 (SCR-29, Q-09 `[PROPOSED]`) ---

    /**
     * How the speaker addresses the other characters of the scene (speakers and silent cast) at the
     * block, from the speaker's side of each relation: `리엔을 ‘꼬마’`.
     */
    fun addressHints(doc: ProjectDoc, block: Block): List<String> {
        val speaker = block.speakerKey?.takeIf { block.kind.known == BlockKind.DIALOGUE } ?: return emptyList()
        val scene = doc.index.unit(block.sceneKey) ?: return emptyList()
        val others = (doc.manuscript.liveBlocksOf(scene.key).mapNotNull { it.speakerKey } + scene.silentCast).distinct() - speaker
        val me = EntityRef(RelationEndKind.CHARACTER.open(), speaker)
        return others.mapNotNull { other ->
            val relation = doc.index.relationBetween(me, EntityRef(RelationEndKind.CHARACTER.open(), other)) ?: return@mapNotNull null
            if (!doc.liveness.isLive(relation)) return@mapNotNull null
            val slot = if (relation.endA.key == speaker) RelationSlotName.ADDRESS_A else RelationSlotName.ADDRESS_B
            val target = ChangeTarget.RelationSlot(relation.key, slot)
            val here = doc.recordsHere(block.key).lastOrNull { it.target.sameAs(target) }
            val record = here ?: doc.recordBeforeBlock(target, block.key)
            val value = if (record != null) (record.value as? JsonPrimitive)?.contentOrNull
            else if (slot == RelationSlotName.ADDRESS_A) relation.addressA else relation.addressB
            val name = Labels.speakerName(doc, other) ?: return@mapNotNull null
            value?.takeIf { it.isNotBlank() }?.let { "$name${objectParticle(name)} ‘$it’" }
        }
    }

    /** `을` after a final consonant, else `를` (Hangul syllables; others get `을`). */
    fun objectParticle(word: String): String {
        val c = word.lastOrNull() ?: return "을"
        if (c !in '가'..'힣') return "을"
        return if ((c - '가') % 28 == 0) "를" else "을"
    }
}
