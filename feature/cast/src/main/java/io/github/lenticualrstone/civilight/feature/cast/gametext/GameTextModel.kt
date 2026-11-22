package io.github.lenticualrstone.civilight.feature.cast.gametext

import io.github.lenticualrstone.civilight.domain.cast.EditContext
import io.github.lenticualrstone.civilight.domain.cast.voice.ArchiveDocs
import io.github.lenticualrstone.civilight.domain.cast.voice.VoiceLines
import io.github.lenticualrstone.civilight.feature.cast.CastUiCopy
import io.github.lenticualrstone.civilight.model.Key
import io.github.lenticualrstone.civilight.model.cast.ArchiveDoc
import io.github.lenticualrstone.civilight.model.cast.VoiceLine
import io.github.lenticualrstone.civilight.model.doc.ProjectDoc
import io.github.lenticualrstone.civilight.model.nfc
import io.github.lenticualrstone.civilight.model.text.RichDoc

/** The archive editor at one moment (CST-17): what the screen shows around the text. */
data class ArchiveView(
    val key: Key?,
    val title: String,
    /** `1,120자`, or `비어 있음` for an empty body. */
    val count: String,
    /** `1-7 이후 열림`, or null without an unlock point. */
    val unlock: String?,
    val unlockUnitKey: Key?,
    val body: RichDoc,
)

/**
 * Pure model of the `게임 텍스트` tab's edits: voice lines (CST-16) and archive documents (CST-17)
 * over `domain:cast` [VoiceLines] and [ArchiveDocs].
 */
object GameTextModel {
    /** A voice line needs its slot name (`임명 · 대화 1 · 신뢰 상승 후`). */
    fun canSaveLine(title: String): Boolean = title.isNotBlank()

    fun line(doc: ProjectDoc, characterKey: Key, lineKey: Key): VoiceLine? =
        VoiceLines.lines(doc, characterKey).firstOrNull { it.key == lineKey }

    /** `+`: a line at the end of the game order with its slot name and text. */
    fun addLine(doc: ProjectDoc, characterKey: Key, title: String, text: RichDoc, ctx: EditContext): Pair<ProjectDoc, Key>? {
        if (!canSaveLine(title) || doc.index.character(characterKey) == null) return null
        val (added, line) = VoiceLines.add(doc, characterKey, title.nfc(), ctx)
        val withText = if (text.isEmpty()) added else VoiceLines.setText(added, line.key, text, ctx)
        return withText to line.key
    }

    /** Saves a line's slot name and text; the state stays (an emptied line reads `비어 있음`). */
    fun saveLine(doc: ProjectDoc, lineKey: Key, title: String, text: RichDoc, ctx: EditContext): ProjectDoc {
        if (!canSaveLine(title)) return doc
        return VoiceLines.setText(VoiceLines.setTitle(doc, lineKey, title.nfc(), ctx), lineKey, text, ctx)
    }

    fun archive(doc: ProjectDoc, characterKey: Key, docKey: Key?): ArchiveDoc? =
        docKey?.let { k -> ArchiveDocs.docs(doc, characterKey).firstOrNull { it.key == k } }

    fun archiveView(doc: ProjectDoc, characterKey: Key, docKey: Key?, body: RichDoc? = null): ArchiveView {
        val archive = archive(doc, characterKey, docKey)
        val text = body ?: archive?.body.orEmpty()
        val count = ArchiveDocs.charCount(text)
        return ArchiveView(
            key = archive?.key,
            title = archive?.title.orEmpty(),
            count = if (count == 0) CastUiCopy.EMPTY_VALUE else "${ArchiveDocs.formatCount(count)}자",
            unlock = ArchiveDocs.unlockLabel(doc, archive?.unlockPoint),
            unlockUnitKey = archive?.unlockPoint?.unitKey,
            body = archive?.body.orEmpty(),
        )
    }

    /**
     * Saves an archive document. [docKey] null creates it (on the first save, never on open), so
     * leaving an untouched new editor leaves nothing behind. Returns the document and its key.
     */
    fun saveArchive(
        doc: ProjectDoc,
        characterKey: Key,
        docKey: Key?,
        title: String,
        body: RichDoc,
        ctx: EditContext,
    ): Pair<ProjectDoc, Key>? {
        if (doc.index.character(characterKey) == null) return null
        val existing = archive(doc, characterKey, docKey)
        if (existing == null) {
            if (title.isBlank() && body.isEmpty()) return null
            val (added, archive) = ArchiveDocs.add(doc, characterKey, title.nfc(), null, ctx)
            return ArchiveDocs.setBody(added, archive.key, body, ctx) to archive.key
        }
        var next = doc
        if (existing.title != title.trim().nfc()) next = ArchiveDocs.setTitle(next, existing.key, title.nfc(), ctx)
        if (existing.body != body) next = ArchiveDocs.setBody(next, existing.key, body, ctx)
        return next to existing.key
    }

    /** The unlock point: an episode or chapter end (`X 이후 열림`); null clears it. */
    fun setUnlock(doc: ProjectDoc, characterKey: Key, docKey: Key, unitKey: Key?, ctx: EditContext): ProjectDoc =
        if (archive(doc, characterKey, docKey) == null) doc else ArchiveDocs.setUnlock(doc, docKey, unitKey, ctx)
}
