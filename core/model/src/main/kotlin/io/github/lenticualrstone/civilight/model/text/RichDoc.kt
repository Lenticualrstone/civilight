@file:OptIn(ExperimentalSerializationApi::class)

package io.github.lenticualrstone.civilight.model.text

import io.github.lenticualrstone.civilight.model.HasExt
import io.github.lenticualrstone.civilight.model.Key
import io.github.lenticualrstone.civilight.model.OpenEnum
import io.github.lenticualrstone.civilight.model.OrderKey
import io.github.lenticualrstone.civilight.model.Ordered
import io.github.lenticualrstone.civilight.model.Timestamp
import io.github.lenticualrstone.civilight.model.open
import kotlinx.serialization.EncodeDefault
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject

/**
 * Long text with keyed paragraphs (§1.4): every long text the recorder can append to or that has
 * headings. A JSON array of [DocBlock]s; their order comes from [DocBlock.order], never the array.
 */
typealias RichDoc = List<DocBlock>

/** One paragraph of a [RichDoc]. */
@Serializable
data class DocBlock(
    /** Paragraph identity; target of a recorder `cardParagraph` placement. */
    val key: Key,
    override val order: OrderKey,
    @EncodeDefault val kind: OpenEnum<DocBlockKind> = DocBlockKind.PARAGRAPH.open(),
    @EncodeDefault val runs: List<TextRun> = emptyList(),
    /** Set on a `⌁` paragraph made by `카드에 넣기` / `인물로 만들기` / `페이지로 만들기`. */
    val recorderSource: RecorderSource? = null,
    override val ext: JsonObject? = null,
) : HasExt, Ordered

/** `본문 · 소제목 · ⌁ 작가 메모`; an open list (Q-PG-01). */
enum class DocBlockKind { PARAGRAPH, HEADING, MEMO }

/** Where recorder-made content came from (§1.4); shown `레코더 11월 22일 08:41`. */
@Serializable
data class RecorderSource(
    val recordKey: Key,
    val recordedAt: Timestamp,
    override val ext: JsonObject? = null,
) : HasExt

/** Paragraphs in document order (order key, then key). */
fun RichDoc.ordered(): RichDoc = sortedWith(compareBy<DocBlock> { it.order }.thenBy { it.key })

/** Plain text of the document, paragraphs joined by newlines. */
val RichDoc.docText: String get() = ordered().joinToString("\n") { it.runs.plainText }

/** Every link run in the document. */
val RichDoc.docLinks: List<TextRun.Link> get() = flatMap { block -> block.runs.filterIsInstance<TextRun.Link>() }
