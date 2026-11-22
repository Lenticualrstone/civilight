package io.github.lenticualrstone.civilight.domain.cast.voice

import io.github.lenticualrstone.civilight.domain.cast.CastCopy
import io.github.lenticualrstone.civilight.domain.cast.EditContext
import io.github.lenticualrstone.civilight.model.Key
import io.github.lenticualrstone.civilight.model.OrderKeys
import io.github.lenticualrstone.civilight.model.cast.VoiceLine
import io.github.lenticualrstone.civilight.model.cast.VoiceLineStatus
import io.github.lenticualrstone.civilight.model.doc.ProjectDoc
import io.github.lenticualrstone.civilight.model.open
import io.github.lenticualrstone.civilight.model.text.RichDoc
import io.github.lenticualrstone.civilight.model.text.docText
import io.github.lenticualrstone.civilight.model.text.isBlankText

/** State label of a voice line (CST-16). */
enum class VoiceLineState(val label: String) {
    DONE("녹음 대본 완료"),
    WRITING("쓰는 중"),
    EMPTY("비어 있음"),
}

/** `보이스 진행` (CST-14): filled = lines that are not `비어 있음` (CST Q5 default). */
data class VoiceProgress(val filled: Int, val total: Int) {
    val empty: Int get() = total - filled

    /** `18 / 24` (record header). */
    val text: String get() = "$filled / $total"

    /** `보이스 · 18 / 24` (`게임 텍스트` tab header). */
    val sectionTitle: String get() = "보이스 · $text"

    /** Width of the flat progress bar. */
    val fraction: Float get() = if (total == 0) 0f else filled.toFloat() / total
}

/** One line of the `게임 텍스트` tab. */
data class VoiceLineRow(
    val key: Key,
    /** `01`. */
    val number: String,
    val title: String,
    val state: VoiceLineState,
    /** The text, or `—` when empty. */
    val text: String,
)

/** Check-center row `▲ 보이스 비어 있음` (CST-14), rendered by the check center. */
data class VoiceEmptyCheck(
    val label: String,
    val location: String,
    val body: String,
    val detail: String,
    val action: String,
    /** The line `보이스 쓰기` opens. */
    val firstEmptyLineKey: Key,
)

/** In-game voice lines of a character (CST-16). Profile voice lines have no voice ID in v1 (CST-52). */
object VoiceLines {
    const val EMPTY_TEXT = "—"
    const val ORDER_LABEL = "게임 순서"

    fun isEmpty(text: RichDoc): Boolean = text.all { it.runs.isBlankText() }

    /** `비어 있음` whenever the text is empty; otherwise the stored status. */
    fun state(line: VoiceLine): VoiceLineState = when {
        isEmpty(line.text) -> VoiceLineState.EMPTY
        line.status.known == VoiceLineStatus.DONE -> VoiceLineState.DONE
        else -> VoiceLineState.WRITING
    }

    fun lines(doc: ProjectDoc, characterKey: Key): List<VoiceLine> = doc.index.voiceLines(characterKey)

    fun progress(doc: ProjectDoc, characterKey: Key): VoiceProgress {
        val lines = lines(doc, characterKey)
        return VoiceProgress(lines.count { state(it) != VoiceLineState.EMPTY }, lines.size)
    }

    fun rows(doc: ProjectDoc, characterKey: Key): List<VoiceLineRow> = lines(doc, characterKey).mapIndexed { i, line ->
        VoiceLineRow(
            key = line.key,
            number = (i + 1).toString().padStart(2, '0'),
            title = line.title,
            state = state(line),
            text = if (isEmpty(line.text)) EMPTY_TEXT else line.text.docText,
        )
    }

    /** The check row when any line is empty, else null. */
    fun emptyCheck(doc: ProjectDoc, characterKey: Key): VoiceEmptyCheck? {
        val c = doc.index.character(characterKey) ?: return null
        val lines = lines(doc, characterKey)
        val first = lines.firstOrNull { state(it) == VoiceLineState.EMPTY } ?: return null
        val p = progress(doc, characterKey)
        return VoiceEmptyCheck(
            label = "${CastCopy.WARN_MARK} 보이스 비어 있음",
            location = "인물 · ${c.name}",
            body = "${c.name} · ${first.title}",
            detail = "보이스 ${p.empty}/${p.total} 비어 있음",
            action = "보이스 쓰기",
            firstEmptyLineKey = first.key,
        )
    }

    // --- Edits ---

    /** `+`: a new empty line at the end. */
    fun add(doc: ProjectDoc, characterKey: Key, title: String, ctx: EditContext): Pair<ProjectDoc, VoiceLine> {
        val line = VoiceLine(
            key = ctx.newKey(), characterKey = characterKey,
            order = OrderKeys.after(lines(doc, characterKey).lastOrNull()?.order),
            title = title.trim(), updatedAt = ctx.now, updatedBy = ctx.by,
        )
        return doc.copy(cast = doc.cast.copy(voiceLines = doc.cast.voiceLines + line)) to line
    }

    fun setText(doc: ProjectDoc, lineKey: Key, text: RichDoc, ctx: EditContext): ProjectDoc = update(doc, lineKey, ctx) { it.copy(text = text) }

    fun setTitle(doc: ProjectDoc, lineKey: Key, title: String, ctx: EditContext): ProjectDoc = update(doc, lineKey, ctx) { it.copy(title = title.trim()) }

    /** Tapping the state label switches `쓰는 중` ↔ `녹음 대본 완료`; an empty line stays `비어 있음`. */
    fun toggleStatus(doc: ProjectDoc, lineKey: Key, ctx: EditContext): ProjectDoc = update(doc, lineKey, ctx) { line ->
        if (isEmpty(line.text)) line else line.copy(
            status = (if (line.status.known == VoiceLineStatus.DONE) VoiceLineStatus.WRITING else VoiceLineStatus.DONE).open(),
        )
    }

    /** Moves a line between two neighbours (game order). */
    fun move(doc: ProjectDoc, lineKey: Key, afterKey: Key?, beforeKey: Key?, ctx: EditContext): ProjectDoc {
        val all = doc.cast.voiceLines.associateBy { it.key }
        return update(doc, lineKey, ctx) { it.copy(order = OrderKeys.between(afterKey?.let(all::get)?.order, beforeKey?.let(all::get)?.order)) }
    }

    fun remove(doc: ProjectDoc, lineKey: Key): ProjectDoc = doc.copy(cast = doc.cast.copy(voiceLines = doc.cast.voiceLines.filterNot { it.key == lineKey }))

    private fun update(doc: ProjectDoc, lineKey: Key, ctx: EditContext, change: (VoiceLine) -> VoiceLine): ProjectDoc = doc.copy(
        cast = doc.cast.copy(voiceLines = doc.cast.voiceLines.map {
            if (it.key == lineKey) change(it).copy(updatedAt = ctx.now, updatedBy = ctx.by) else it
        }),
    )
}
