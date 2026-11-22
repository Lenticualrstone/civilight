package io.github.lenticualrstone.civilight.domain.script

import io.github.lenticualrstone.civilight.model.Key
import io.github.lenticualrstone.civilight.model.Timestamp
import io.github.lenticualrstone.civilight.model.doc.ProjectDoc
import io.github.lenticualrstone.civilight.model.open
import io.github.lenticualrstone.civilight.model.project.VoiceIdFormat
import io.github.lenticualrstone.civilight.model.project.VoiceIdFormatMode
import io.github.lenticualrstone.civilight.model.script.Block
import io.github.lenticualrstone.civilight.model.script.BlockKind
import io.github.lenticualrstone.civilight.model.script.ChoiceOption
import io.github.lenticualrstone.civilight.model.script.DiscardReason
import io.github.lenticualrstone.civilight.model.script.DiscardedVoiceId
import io.github.lenticualrstone.civilight.model.script.VoiceExport
import io.github.lenticualrstone.civilight.model.script.VoiceMode
import io.github.lenticualrstone.civilight.model.script.VoiceState
import io.github.lenticualrstone.civilight.model.text.plainText
import java.security.MessageDigest
import java.time.ZoneId

/**
 * Voice IDs (SCR-23/24, rule 12 §6). `자동` IDs are computed from the format every time; `고정` IDs
 * are stored at the first export and never change by themselves; discarded IDs are never reissued.
 *
 * Pattern tokens: `{code}`, `{chapter}`, `{episode}`, `{number}`, each with an optional pad width
 * (`{chapter2}`, `{number3}`). Choice options append `_{index}`.
 */
object VoiceIds {
    private val TOKEN = Regex("""\{(code|chapter|episode|number)(\d*)\}""")

    /** Chapter and episode numbers of a numbering unit for the episode part (Q-06 default, see status). */
    data class EpisodeParts(val chapter: Int, val episode: Int)

    /** The speaker part: the character's voice code, from its row or its name tag. */
    fun codeOf(doc: ProjectDoc, characterKey: Key): String? =
        doc.index.character(characterKey)?.voiceCode ?: doc.index.nameTag(characterKey)?.voiceCode

    /**
     * Episode part of a numbering unit: `episode` = its ordinal among live siblings, `chapter` = its
     * parent's ordinal (0 without a parent). A 1-level project (no numbering unit) gives 0 and 0.
     */
    fun episodePartsOf(doc: ProjectDoc, numberingUnitKey: Key): EpisodeParts {
        if (numberingUnitKey == doc.project.key) return EpisodeParts(0, 0)
        val path = Labels.ordinalPath(doc, numberingUnitKey)
        return EpisodeParts(chapter = path.getOrNull(path.size - 2) ?: 0, episode = path.lastOrNull() ?: 0)
    }

    /** Applies a pattern. */
    fun format(pattern: String, code: String, parts: EpisodeParts, number: Int, optionIndex: Int? = null): String {
        val base = TOKEN.replace(pattern) { m ->
            val width = m.groupValues[2].toIntOrNull() ?: 0
            when (m.groupValues[1]) {
                "code" -> code
                "chapter" -> parts.chapter.toString().padStart(width, '0')
                "episode" -> parts.episode.toString().padStart(width, '0')
                else -> number.toString().padStart(width, '0')
            }
        }
        return if (optionIndex != null) "${base}_$optionIndex" else base
    }

    private fun formatOf(doc: ProjectDoc): VoiceIdFormat = doc.project.script.voiceIdFormat
    private fun isManual(doc: ProjectDoc): Boolean = formatOf(doc).mode.known == VoiceIdFormatMode.MANUAL

    /** 1-based index of an option in its block (display `#045-1`). */
    fun optionIndex(block: Block, optionKey: Key): Int? =
        block.options.sortedWith(compareBy<ChoiceOption> { it.order }.thenBy { it.key }).indexOfFirst { it.key == optionKey }
            .takeIf { it >= 0 }?.plus(1)

    /** The automatic ID of a dialogue block, or null (no speaker, no voice code, manual format, not dialogue). */
    fun autoId(doc: ProjectDoc, block: Block, format: VoiceIdFormat = formatOf(doc)): String? {
        if (block.kind.known != BlockKind.DIALOGUE || format.mode.known == VoiceIdFormatMode.MANUAL) return null
        val code = block.speakerKey?.let { codeOf(doc, it) } ?: return null
        return format(format.pattern, code, episodePartsOf(doc, doc.numberingUnitOf(block)), block.number)
    }

    /** The automatic ID of a spoken option (`HRT_0107_045_1`), or null when the option has no speaker. */
    fun autoIdOfOption(doc: ProjectDoc, block: Block, option: ChoiceOption, format: VoiceIdFormat = formatOf(doc)): String? {
        if (format.mode.known == VoiceIdFormatMode.MANUAL) return null
        val code = option.speakerKey?.let { codeOf(doc, it) } ?: return null
        val index = optionIndex(block, option.key) ?: return null
        return format(format.pattern, code, episodePartsOf(doc, doc.numberingUnitOf(block)), block.number, index)
    }

    /** The ID in use: fixed, else manual (format `직접`), else automatic. */
    fun currentId(doc: ProjectDoc, block: Block): String? = currentOf(doc, block.voice) { autoId(doc, block) }

    fun currentIdOfOption(doc: ProjectDoc, block: Block, option: ChoiceOption): String? =
        currentOf(doc, option.voice) { autoIdOfOption(doc, block, option) }

    private inline fun currentOf(doc: ProjectDoc, voice: VoiceState?, auto: () -> String?): String? = when {
        voice?.mode?.known == VoiceMode.FIXED && voice.fixedId != null -> voice.fixedId
        isManual(doc) -> voice?.manualId
        else -> auto()
    }

    fun isFixed(voice: VoiceState?): Boolean = voice?.mode?.known == VoiceMode.FIXED && voice.fixedId != null

    /** 속성 tab text: `HRT_0107_043 · 자동` or `HRT_0107_043 · 고정 · 11월 20일 내보냄`; null without an ID. */
    fun stateLabel(doc: ProjectDoc, block: Block, zone: ZoneId): String? {
        val id = currentId(doc, block) ?: return null
        val voice = block.voice
        return if (isFixed(voice)) {
            val at = voice?.fixedAt
            if (at != null) "$id · 고정 · ${Labels.monthDay(at, zone)} 내보냄" else "$id · 고정"
        } else {
            "$id · 자동"
        }
    }

    // --- ◇ 형식과 다른 ID ---

    /**
     * True when a fixed ID's speaker part or episode part differs from what the format gives now;
     * the number part (and the option index) is ignored, so renumbering never raises it.
     */
    fun differsFromFormat(doc: ProjectDoc, block: Block, option: ChoiceOption? = null): Boolean {
        val voice = option?.voice ?: block.voice
        if (!isFixed(voice) || isManual(doc)) return false
        val speaker = option?.speakerKey ?: block.speakerKey
        val code = speaker?.let { codeOf(doc, it) } ?: return true
        val parts = episodePartsOf(doc, doc.numberingUnitOf(block))
        val pattern = formatOf(doc).pattern
        val regex = buildString {
            var last = 0
            for (m in TOKEN.findAll(pattern)) {
                append(Regex.escape(pattern.substring(last, m.range.first)))
                val width = m.groupValues[2].toIntOrNull() ?: 0
                when (m.groupValues[1]) {
                    "code" -> append(Regex.escape(code))
                    "chapter" -> append(Regex.escape(parts.chapter.toString().padStart(width, '0')))
                    "episode" -> append(Regex.escape(parts.episode.toString().padStart(width, '0')))
                    else -> append("\\d+")
                }
                last = m.range.last + 1
            }
            append(Regex.escape(pattern.substring(last)))
            if (option != null) append("_\\d+")
        }
        return !Regex(regex).matches(voice!!.fixedId!!)
    }

    // --- Fixing, discarding, replacing ---

    /** `sha256-<hex>` of the exported text (`VoiceExport.textHash`). */
    fun textHash(text: String): String {
        val digest = MessageDigest.getInstance("SHA-256").digest(text.toByteArray(Charsets.UTF_8))
        return "sha256-" + digest.joinToString("") { "%02x".format(it) }
    }

    /**
     * The voice state after a block or option was written to a CSV or JSON export at [at]: the
     * first export fixes [id]; every export records `lastExport` (inputs of the CSV `바뀜` state).
     */
    fun exported(voice: VoiceState?, id: String, text: String, speakerKey: Key?, at: Timestamp): VoiceState {
        val base = voice ?: VoiceState()
        val fixed = if (isFixed(base)) base else base.copy(mode = VoiceMode.FIXED.open(), fixedId = id, fixedAt = at)
        return fixed.copy(lastExport = VoiceExport(at = at, textHash = textHash(text), speakerKey = speakerKey))
    }

    /** A discarded-ID row for a block's fixed ID (and its spoken options' fixed IDs). */
    fun discardedOf(block: Block, reason: DiscardReason, at: Timestamp): List<DiscardedVoiceId> {
        val out = mutableListOf<DiscardedVoiceId>()
        block.voice?.takeIf(::isFixed)?.let { out += DiscardedVoiceId(it.fixedId!!, block.key, null, at, reason.open()) }
        for (o in block.options) {
            o.voice?.takeIf(::isFixed)?.let { out += DiscardedVoiceId(it.fixedId!!, block.key, o.key, at, reason.open()) }
        }
        return out
    }

    /**
     * `새 ID로` (SCR-23): the old fixed string becomes a `버린 ID` (replaced) and the block returns to
     * `자동` until its next export (`[PROPOSED]`).
     */
    fun replaceWithNew(doc: ProjectDoc, blockKey: Key, optionKey: Key? = null, ctx: EditContext): ProjectDoc {
        val block = doc.index.block(blockKey) ?: return doc
        val unit = doc.numberingUnitOf(block)
        return if (optionKey == null) {
            val voice = block.voice?.takeIf(::isFixed) ?: return doc
            doc.replaceBlock(block.copy(voice = null, updatedAt = ctx.now, updatedBy = ctx.device))
                .addDiscarded(unit, listOf(DiscardedVoiceId(voice.fixedId!!, blockKey, null, ctx.now, DiscardReason.REPLACED.open())))
        } else {
            val option = block.options.firstOrNull { it.key == optionKey } ?: return doc
            val voice = option.voice?.takeIf(::isFixed) ?: return doc
            doc.replaceBlock(
                block.copy(
                    options = block.options.map { if (it.key == optionKey) it.copy(voice = null) else it },
                    updatedAt = ctx.now, updatedBy = ctx.device,
                ),
            ).addDiscarded(unit, listOf(DiscardedVoiceId(voice.fixedId!!, blockKey, optionKey, ctx.now, DiscardReason.REPLACED.open())))
        }
    }

    /**
     * Format `직접` (SCR-23): the ID the author typed for a dialogue block in the `속성` tab. A blank
     * [id] clears it. A fixed ID is never touched (the field is read-only then).
     */
    fun setManualId(doc: ProjectDoc, blockKey: Key, id: String?, ctx: EditContext): ProjectDoc {
        val block = doc.index.block(blockKey)?.takeIf { it.kind.known == BlockKind.DIALOGUE } ?: return doc
        if (isFixed(block.voice)) return doc
        val typed = id?.trim()?.takeIf { it.isNotEmpty() }
        if (block.voice?.manualId == typed) return doc
        val base = block.voice ?: VoiceState()
        val voice = base.copy(manualId = typed).takeIf { it != VoiceState() }
        return doc.replaceBlock(block.copy(voice = voice, updatedAt = ctx.now, updatedBy = ctx.device))
    }

    /** Fixed IDs of live blocks and options (`고정 ID {n}개는 그대로`). */
    fun fixedCount(doc: ProjectDoc): Int = doc.manuscript.liveBlocks.sumOf { b ->
        (if (isFixed(b.voice)) 1 else 0) + b.options.count { isFixed(it.voice) }
    }

    /** Settings lines after a format change: `고정 ID 214개는 그대로` and the button `고정 ID도 새 형식으로`. */
    fun formatChangeNotice(doc: ProjectDoc): List<String> =
        listOf("고정 ID ${fixedCount(doc)}개는 그대로", "고정 ID도 새 형식으로")

    /**
     * `고정 ID도 새 형식으로` (`[PROPOSED]`): every fixed ID of a live block is re-fixed in [format];
     * each old string becomes a `버린 ID` (formatRefixed). Applies [format] to the project too.
     */
    fun refixAll(doc: ProjectDoc, format: VoiceIdFormat, ctx: EditContext): ProjectDoc {
        var out = doc.copy(project = doc.project.copy(script = doc.project.script.copy(voiceIdFormat = format)))
        val discarded = mutableMapOf<Key, MutableList<DiscardedVoiceId>>()
        val updates = mutableMapOf<Key, Block>()
        for (b in doc.manuscript.liveBlocks) {
            var changed = false
            var voice = b.voice
            if (isFixed(voice)) {
                autoId(out, b, format)?.takeIf { it != voice!!.fixedId }?.let { id ->
                    discarded.getOrPut(out.numberingUnitOf(b)) { mutableListOf() } +=
                        DiscardedVoiceId(voice!!.fixedId!!, b.key, null, ctx.now, DiscardReason.FORMAT_REFIXED.open())
                    voice = voice!!.copy(fixedId = id, fixedAt = ctx.now)
                    changed = true
                }
            }
            val options = b.options.map { o ->
                val ov = o.voice
                if (!isFixed(ov)) return@map o
                val id = autoIdOfOption(out, b, o, format)?.takeIf { it != ov!!.fixedId } ?: return@map o
                discarded.getOrPut(out.numberingUnitOf(b)) { mutableListOf() } +=
                    DiscardedVoiceId(ov!!.fixedId!!, b.key, o.key, ctx.now, DiscardReason.FORMAT_REFIXED.open())
                changed = true
                o.copy(voice = ov.copy(fixedId = id, fixedAt = ctx.now))
            }
            if (changed) updates[b.key] = b.copy(voice = voice, options = options, updatedAt = ctx.now, updatedBy = ctx.device)
        }
        out = out.replaceBlocks(updates)
        for ((unit, ids) in discarded) out = out.addDiscarded(unit, ids)
        return out
    }

    /** All discarded IDs of the project. */
    fun discarded(doc: ProjectDoc): List<DiscardedVoiceId> = doc.index.allDiscardedVoiceIds

    /** Text of a block or option as the CSV writes it (`lastExport.textHash` input). */
    fun spokenText(block: Block, option: ChoiceOption? = null): String =
        (option?.runs ?: block.runs).plainText
}
