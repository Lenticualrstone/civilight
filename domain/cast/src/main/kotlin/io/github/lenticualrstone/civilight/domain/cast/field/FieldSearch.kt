package io.github.lenticualrstone.civilight.domain.cast.field

import io.github.lenticualrstone.civilight.domain.cast.CastCopy
import io.github.lenticualrstone.civilight.domain.cast.CharacterEdits
import io.github.lenticualrstone.civilight.domain.cast.ItemRefs
import io.github.lenticualrstone.civilight.domain.cast.ResolvedItem
import io.github.lenticualrstone.civilight.domain.cast.timed.ReadPoint
import io.github.lenticualrstone.civilight.model.Key
import io.github.lenticualrstone.civilight.model.cast.EntityKind
import io.github.lenticualrstone.civilight.model.cast.FieldDef
import io.github.lenticualrstone.civilight.model.cast.FieldPayload
import io.github.lenticualrstone.civilight.model.cast.FieldType
import io.github.lenticualrstone.civilight.model.doc.ProjectDoc
import io.github.lenticualrstone.civilight.model.nfc
import io.github.lenticualrstone.civilight.model.position.LensPoint
import io.github.lenticualrstone.civilight.model.text.plainText

/**
 * A field-value hit of the command-bar search (CST-27). Short values name the field and the value
 * (`특기 · 성직자`); long text gives the whole sentence with the match range inside it, so the result
 * can box the match inline.
 */
data class FieldHit(
    val characterKey: Key,
    val characterName: String,
    val defKey: Key,
    /** `종족 · 인간`, `특기 · 성직자`; for long text `<항목> · <sentence>`. */
    val label: String,
    /** The matched value or sentence. */
    val text: String,
    /** Match range inside [text]. */
    val match: IntRange,
)

/** Search over character field values (CST-27). */
object FieldSearch {
    /** Index of [query] in [text], ignoring case, both NFC; −1 when absent. */
    fun indexOf(text: String, query: String): Int = text.nfc().lowercase().indexOf(query.nfc().lowercase())

    /** Sentences of a text (split after `. ? ! 。 …` or a line break), whitespace trimmed. */
    fun sentences(text: String): List<String> =
        Regex("""[^.?!。…\n]+[.?!。…]*""").findAll(text).map { it.value.trim() }.filter { it.isNotEmpty() }.toList()

    /**
     * Hits of [query] in field values of live, visible characters at [lens]. Searched: values of
     * `짧은 글 · 고르기 · 여러 개 고르기 · 연결` as whole values, `긴 글` like body text (sentence
     * hits). Hidden and secret definitions are not searched; locked hidden characters never match.
     */
    fun search(
        doc: ProjectDoc,
        query: String,
        lens: LensPoint = LensPoint.Latest,
        hiddenUnlocked: Boolean = false,
        dates: StoryDateSource = StoryDates.Default,
    ): List<FieldHit> {
        val q = query.trim()
        if (q.isEmpty()) return emptyList()
        val out = mutableListOf<FieldHit>()
        for (c in CharacterEdits.live(doc)) {
            if (ItemRefs.character(doc, c.key, hiddenUnlocked) !is ResolvedItem.Live) continue
            for (def in FieldDefinitions.visibleFor(doc, EntityKind.CHARACTER, c.key).filterNot { it.isSecret }) {
                val type = def.type.known ?: continue
                val value = FieldReader.read(doc, EntityKind.CHARACTER, c.key, def, ReadPoint.AtLens(lens), dates).value
                if (type == FieldType.LONG_TEXT) {
                    val rich = value?.payload as? FieldPayload.Rich ?: continue
                    for (block in rich.doc) for (sentence in sentences(block.runs.plainText)) {
                        val i = indexOf(sentence, q)
                        if (i >= 0) out += FieldHit(c.key, c.name, def.key, "${def.name}${CastCopy.SEP}$sentence", sentence, i until i + q.nfc().length)
                    }
                    continue
                }
                if (!FieldTypes.isValueSearchable(type)) continue
                for (part in parts(doc, def, value?.payload, value?.fallbackText)) {
                    val i = indexOf(part, q)
                    if (i >= 0) out += FieldHit(c.key, c.name, def.key, "${def.name}${CastCopy.SEP}$part", part, i until i + q.nfc().length)
                }
            }
        }
        return out
    }

    /** The searchable pieces of a value: each text, option label, free text or linked name. */
    private fun parts(doc: ProjectDoc, def: FieldDef, payload: FieldPayload?, fallback: String?): List<String> = when (payload) {
        is FieldPayload.Texts -> payload.values.filter { it.isNotBlank() }
        is FieldPayload.Choice -> payload.optionKeys.mapNotNull { k -> def.options.orEmpty().firstOrNull { it.key == k }?.label } +
            listOfNotNull(payload.freeText?.takeIf { it.isNotBlank() })
        is FieldPayload.Link -> FieldText.format(doc, def, io.github.lenticualrstone.civilight.model.cast.FieldValue.of(payload)).refs
            .filterIsInstance<ResolvedItem.Live>().map { it.name }
        null -> listOfNotNull(fallback?.takeIf { it.isNotBlank() })
        else -> emptyList()
    }
}
