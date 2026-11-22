package io.github.lenticualrstone.civilight.model.text

import io.github.lenticualrstone.civilight.model.HasExt
import io.github.lenticualrstone.civilight.model.Key
import io.github.lenticualrstone.civilight.model.OpenEnum
import io.github.lenticualrstone.civilight.model.json.TaggedUnionSerializer
import io.github.lenticualrstone.civilight.model.open
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull

/**
 * One run of stored text (§1.4). JSON: `{t}` (plain, `k` omitted), `{k:"paren", t}`,
 * `{k:"link", id, to, t}`, `{k:"break"}`. A run of an unknown kind is kept verbatim and shown as its
 * `t` text.
 */
@Serializable(with = TextRunSerializer::class)
sealed interface TextRun {
    /** The text this run shows (empty for a break). */
    val shownText: String

    /** Ordinary text. */
    @Serializable
    data class Plain(
        @SerialName("t") val text: String,
        override val ext: JsonObject? = null,
    ) : TextRun, HasExt {
        override val shownText: String get() = text
    }

    /** `괄호 지문`, stored without the parentheses and drawn `(text)` (SCR-07). */
    @Serializable
    data class Paren(
        @SerialName("t") val text: String,
        override val ext: JsonObject? = null,
    ) : TextRun, HasExt {
        override val shownText: String get() = text
    }

    /**
     * An `@` link (LinkRun). [linkKey] survives edits, split, merge, move and cut-paste and is
     * re-minted on any copy (§1.4); [text] is exactly the text shown.
     */
    @Serializable
    data class Link(
        @SerialName("id") val linkKey: Key,
        @SerialName("to") val target: LinkTarget,
        @SerialName("t") val text: String,
        override val ext: JsonObject? = null,
    ) : TextRun, HasExt {
        override val shownText: String get() = text
    }

    /** Soft line break (`줄바꿈`). */
    @Serializable
    data object Break : TextRun {
        override val shownText: String get() = "\n"
    }

    /** A run kind from a newer file; written back unchanged, shown as its `t`. */
    data class Unknown(val raw: JsonObject) : TextRun {
        val kind: String? get() = (raw["k"] as? JsonPrimitive)?.contentOrNull
        override val shownText: String get() = (raw["t"] as? JsonPrimitive)?.contentOrNull.orEmpty()
    }
}

/** Target of a link run: `{ kind, key, projectKey? }`; `projectKey` only in recorder text (§1.4). */
@Serializable
data class LinkTarget(
    val kind: OpenEnum<LinkTargetKind>,
    val key: Key,
    val projectKey: Key? = null,
    override val ext: JsonObject? = null,
) : HasExt {
    constructor(kind: LinkTargetKind, key: Key, projectKey: Key? = null) : this(kind.open(), key, projectKey)
}

enum class LinkTargetKind {
    CHARACTER, TERM, PLACE, FACTION, PAGE, UNIT, PROJECT;

    companion object {
        /** Allowed in script blocks, choice options and transition runs (SCR-55). */
        val SCRIPT: Set<LinkTargetKind> = setOf(CHARACTER, TERM, PLACE)

        /** Allowed in character bodies, field long text, voice lines, archive docs, world text, pages (CNV-13, WLD-17). */
        val DOCUMENT: Set<LinkTargetKind> = setOf(CHARACTER, TERM, PLACE, FACTION, PAGE)

        /** Allowed in recorder record text (LRC-17). */
        val RECORDER: Set<LinkTargetKind> = entries.toSet()
    }
}

object TextRunSerializer : TaggedUnionSerializer<TextRun>(
    serialName = "TextRun",
    tagKey = "k",
    defaultTag = "plain",
    omitDefaultTag = true,
) {
    override val variants: List<Variant<out TextRun>> = listOf(
        Variant("plain", TextRun.Plain::class, TextRun.Plain.serializer()),
        Variant("paren", TextRun.Paren::class, TextRun.Paren.serializer()),
        Variant("link", TextRun.Link::class, TextRun.Link.serializer()),
        Variant("break", TextRun.Break::class, TextRun.Break.serializer()),
    )

    override fun unknown(raw: JsonObject): TextRun = TextRun.Unknown(raw)
    override fun rawOf(value: TextRun): JsonObject? = (value as? TextRun.Unknown)?.raw
}

/** The concatenated shown text of these runs. */
val List<TextRun>.plainText: String get() = joinToString("") { it.shownText }

/** True when the runs show nothing but whitespace and breaks. */
fun List<TextRun>.isBlankText(): Boolean = all { it is TextRun.Break || it.shownText.isBlank() }
