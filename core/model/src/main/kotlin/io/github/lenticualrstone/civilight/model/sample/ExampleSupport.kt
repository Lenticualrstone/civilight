package io.github.lenticualrstone.civilight.model.sample

import io.github.lenticualrstone.civilight.model.Key
import io.github.lenticualrstone.civilight.model.OrderKey
import io.github.lenticualrstone.civilight.model.OrderKeys
import io.github.lenticualrstone.civilight.model.open
import io.github.lenticualrstone.civilight.model.text.DocBlock
import io.github.lenticualrstone.civilight.model.text.DocBlockKind
import io.github.lenticualrstone.civilight.model.text.LinkTarget
import io.github.lenticualrstone.civilight.model.text.LinkTargetKind
import io.github.lenticualrstone.civilight.model.text.RecorderSource
import io.github.lenticualrstone.civilight.model.text.RichDoc
import io.github.lenticualrstone.civilight.model.text.TextRun
import java.security.MessageDigest
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/** Fixed clock values of the example project (all deterministic). */
object ExampleClock {
    /** The fixtures' "now": 2026-11-22 09:00 Asia/Seoul. */
    val NOW: Long = at("2026-11-22T00:00:00Z")

    /** Zone used for local dates (trash days, block groups). */
    val ZONE: ZoneId = ZoneId.of("Asia/Seoul")

    /** Device id written to every `updatedBy`. */
    const val DEVICE = "example-device"

    val PROJECT_CREATED: Long = at("2026-03-01T00:00:00Z")

    /** Last edit: 2026-11-22 08:12 KST (`오늘 08:12`). */
    val LAST_EDIT: Long = at("2026-11-21T23:12:00Z")

    /** CSV export of 1-1 … 1-3 that fixed their voice IDs. */
    val VOICE_EXPORT: Long = at("2026-11-10T03:00:00Z")

    fun at(iso: String): Long = Instant.parse(iso).toEpochMilli()

    fun localDate(epochMillis: Long): LocalDate = Instant.ofEpochMilli(epochMillis).atZone(ZONE).toLocalDate()
}

/** Small builders for runs and documents used by the example content. */
internal object ExampleText {
    fun t(text: String): TextRun = TextRun.Plain(text)
    fun paren(text: String): TextRun = TextRun.Paren(text)

    fun link(linkKey: Key, kind: LinkTargetKind, target: Key, text: String, projectKey: Key? = null): TextRun =
        TextRun.Link(linkKey, LinkTarget(kind, target, projectKey), text)

    /** A one-run plain paragraph list. */
    fun plain(text: String): List<TextRun> = listOf(t(text))

    /** `sha256-<hex>` of a text, as `VoiceExport.textHash` stores it. */
    fun sha256(text: String): String {
        val digest = MessageDigest.getInstance("SHA-256").digest(text.toByteArray(Charsets.UTF_8))
        return "sha256-" + digest.joinToString("") { "%02x".format(it) }
    }
}

/**
 * Builds a [RichDoc] whose paragraph keys come from [ExampleKeys.docBlock] starting at [firstKey].
 * Each entry is a paragraph kind plus its runs.
 */
internal class DocBuilder(private val firstKey: Int) {
    private val entries = mutableListOf<Triple<DocBlockKind, List<TextRun>, RecorderSource?>>()

    fun paragraph(vararg runs: TextRun) = apply { entries += Triple(DocBlockKind.PARAGRAPH, runs.toList(), null) }
    fun paragraph(text: String) = paragraph(TextRun.Plain(text))
    fun heading(text: String) = apply { entries += Triple(DocBlockKind.HEADING, listOf(TextRun.Plain(text)), null) }
    fun memo(text: String, source: RecorderSource? = null) =
        apply { entries += Triple(DocBlockKind.MEMO, listOf(TextRun.Plain(text)), source) }

    fun build(): RichDoc {
        val orders: List<OrderKey> = OrderKeys.evenlySpaced(entries.size)
        return entries.mapIndexed { i, (kind, runs, source) ->
            DocBlock(key = ExampleKeys.docBlock(firstKey + i), order = orders[i], kind = kind.open(), runs = runs, recorderSource = source)
        }
    }

    /** Number of paragraph keys this document uses. */
    val size: Int get() = entries.size
}

/** A document starting at doc-block key number [firstKey]. */
internal fun doc(firstKey: Int, build: DocBuilder.() -> Unit): RichDoc = DocBuilder(firstKey).apply(build).build()
