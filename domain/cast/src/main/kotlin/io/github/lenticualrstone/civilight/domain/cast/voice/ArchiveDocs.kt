package io.github.lenticualrstone.civilight.domain.cast.voice

import io.github.lenticualrstone.civilight.domain.cast.EditContext
import io.github.lenticualrstone.civilight.domain.cast.StoryLabels
import io.github.lenticualrstone.civilight.model.Key
import io.github.lenticualrstone.civilight.model.OrderKeys
import io.github.lenticualrstone.civilight.model.cast.ArchiveDoc
import io.github.lenticualrstone.civilight.model.doc.ProjectDoc
import io.github.lenticualrstone.civilight.model.position.LensPoint
import io.github.lenticualrstone.civilight.model.position.PointRef
import io.github.lenticualrstone.civilight.model.text.RichDoc
import io.github.lenticualrstone.civilight.model.text.ordered
import io.github.lenticualrstone.civilight.model.text.plainText
import java.text.NumberFormat
import java.util.Locale

/** One row of `아카이브 문서` (CST-17). */
data class ArchiveRow(
    val key: Key,
    /** `기록 1 · 등불지기의 일지`. */
    val title: String,
    /** `1,120자 · 1-7 이후 열림` or `비어 있음 · 2장 이후 열림`. */
    val sub: String,
    val charCount: Int,
)

/** Archive documents of a character (CST-17). */
object ArchiveDocs {
    fun docs(doc: ProjectDoc, characterKey: Key): List<ArchiveDoc> = doc.index.archiveDocs(characterKey)

    /** `아카이브 문서 · 2`. */
    fun sectionTitle(doc: ProjectDoc, characterKey: Key): String = "아카이브 문서 · ${docs(doc, characterKey).size}"

    /** Characters of the body except line breaks ([PROPOSED] count rule). */
    fun charCount(body: RichDoc): Int = body.ordered().sumOf { block -> block.runs.plainText.count { it != '\n' && it != '\r' } }

    /** `1,120`. */
    fun formatCount(count: Int): String = NumberFormat.getIntegerInstance(Locale.US).format(count)

    /** `1-7 이후 열림`, or null without an unlock point. */
    fun unlockLabel(doc: ProjectDoc, point: PointRef?): String? = point?.let { "${StoryLabels.pointRef(doc, it)} 이후 열림" }

    fun rows(doc: ProjectDoc, characterKey: Key): List<ArchiveRow> = docs(doc, characterKey).mapIndexed { i, d ->
        val count = charCount(d.body)
        val size = if (count == 0) "비어 있음" else "${formatCount(count)}자"
        ArchiveRow(
            key = d.key,
            title = "기록 ${i + 1} · ${d.title}",
            sub = listOfNotNull(size, unlockLabel(doc, d.unlockPoint)).joinToString(" · "),
            charCount = count,
        )
    }

    /** Unlocked at the lens (`pos(point) ≤ pos(P)`, §4.6); no point = always open. */
    fun isOpenAt(doc: ProjectDoc, archive: ArchiveDoc, lens: LensPoint): Boolean = doc.manuscript.isVisible(archive.unlockPoint, lens)

    // --- Edits ---

    fun add(doc: ProjectDoc, characterKey: Key, title: String, unlockPoint: PointRef?, ctx: EditContext): Pair<ProjectDoc, ArchiveDoc> {
        val archive = ArchiveDoc(
            key = ctx.newKey(), characterKey = characterKey,
            order = OrderKeys.after(docs(doc, characterKey).lastOrNull()?.order),
            title = title.trim(), unlockPoint = unlockPoint, updatedAt = ctx.now, updatedBy = ctx.by,
        )
        return doc.copy(cast = doc.cast.copy(archiveDocs = doc.cast.archiveDocs + archive)) to archive
    }

    fun setBody(doc: ProjectDoc, key: Key, body: RichDoc, ctx: EditContext): ProjectDoc = update(doc, key, ctx) { it.copy(body = body) }

    fun setTitle(doc: ProjectDoc, key: Key, title: String, ctx: EditContext): ProjectDoc = update(doc, key, ctx) { it.copy(title = title.trim()) }

    /** The unlock point: an episode or chapter end (`X 이후 열림` = `unitEnd(X)`). */
    fun setUnlock(doc: ProjectDoc, key: Key, unitKey: Key?, ctx: EditContext): ProjectDoc =
        update(doc, key, ctx) { it.copy(unlockPoint = unitKey?.let(PointRef::unitEnd)) }

    fun remove(doc: ProjectDoc, key: Key): ProjectDoc = doc.copy(cast = doc.cast.copy(archiveDocs = doc.cast.archiveDocs.filterNot { it.key == key }))

    private fun update(doc: ProjectDoc, key: Key, ctx: EditContext, change: (ArchiveDoc) -> ArchiveDoc): ProjectDoc = doc.copy(
        cast = doc.cast.copy(archiveDocs = doc.cast.archiveDocs.map { if (it.key == key) change(it).copy(updatedAt = ctx.now, updatedBy = ctx.by) else it }),
    )
}
