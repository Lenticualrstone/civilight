package io.github.lenticualrstone.civilight.domain.cast.field

import io.github.lenticualrstone.civilight.domain.cast.EditContext
import io.github.lenticualrstone.civilight.domain.cast.entityKind
import io.github.lenticualrstone.civilight.model.Key
import io.github.lenticualrstone.civilight.model.cast.FallbackReason
import io.github.lenticualrstone.civilight.model.cast.FieldDef
import io.github.lenticualrstone.civilight.model.cast.FieldLinkTargetKind
import io.github.lenticualrstone.civilight.model.cast.FieldPayload
import io.github.lenticualrstone.civilight.model.cast.FieldType
import io.github.lenticualrstone.civilight.model.cast.FieldValue
import io.github.lenticualrstone.civilight.model.cast.EntityKind
import io.github.lenticualrstone.civilight.model.change.ChangeTarget
import io.github.lenticualrstone.civilight.model.doc.ProjectDoc
import io.github.lenticualrstone.civilight.model.nfc
import io.github.lenticualrstone.civilight.model.open

/** Result of a type change: the project and the number of values kept as fallback text. */
data class ConversionResult(val doc: ProjectDoc, val failedCount: Int) {
    /** `◇ 바꾸지 못한 값 3`, or null when every value converted. */
    val notice: String? get() = if (failedCount > 0) "${FieldText.CONVERSION_FAILED} $failedCount" else null
}

/** Field type changes (CST-23): only the pairs of the table; values that cannot convert keep their text with `◇`. */
object FieldConversion {
    private val NUMBER = Regex("""^\s*(-?\d+(?:\.\d+)?)\s*[^\d\s.]*\s*$""")

    /** The number a text reads as (`163cm` → `163`), or null (`키 작음`). */
    fun readNumber(text: String): String? = NUMBER.matchEntire(text.trim())?.groupValues?.get(1)

    /** Changes [defKey] to [to]. [linkTargetKind] is required for `연결`. Throws for a pair not in the table. */
    fun convert(doc: ProjectDoc, defKey: Key, to: FieldType, ctx: EditContext, linkTargetKind: FieldLinkTargetKind? = null): ConversionResult {
        val def = requireNotNull(doc.index.fieldDef(defKey)) { "no field $defKey" }
        val from = requireNotNull(def.type.known) { "unknown field type ${def.type.raw}" }
        require(to in FieldTypes.conversionTargets(from)) { "conversion ${from.name} → ${to.name} is not offered" }
        val kind = requireNotNull(def.entityKind.known)

        // Options built from existing values (short text → choice), in first-use order.
        val builtOptions = if (from == FieldType.SHORT_TEXT && FieldTypes.hasOptions(to)) {
            FieldDefinitions.newOptions(allTexts(doc, kind, defKey), ctx)
        } else {
            null
        }
        val target = linkTargetKind ?: FieldLinkTargetKind.FACTION
        val newDef = def.copy(
            type = to.open(),
            options = when {
                builtOptions != null -> builtOptions
                FieldTypes.hasOptions(to) -> def.options
                else -> null
            },
            allowFreeText = if (to == FieldType.CHOICE) def.allowFreeText ?: false else null,
            shortTextMultiple = if (to == FieldType.SHORT_TEXT) false else null,
            unit = if (to == FieldType.NUMBER) def.unit else null,
            datePrecision = null,
            linkTargetKind = if (to == FieldType.LINK) target.open() else null,
            inFilter = def.inFilter && FieldTypes.canFilter(to),
        )

        var failed = 0
        val convert: (FieldValue) -> FieldValue? = { v ->
            when (v.valueState) {
                ValueState.EMPTY -> null
                ValueState.UNKNOWN -> v
                ValueState.FALLBACK ->
                    if (to == FieldType.SHORT_TEXT) FieldValue.of(FieldPayload.Texts(listOf(v.fallbackText.orEmpty()))) else v
                ValueState.VALUE -> convertValue(doc, def, newDef, v, from, to, target.entityKind) ?: run {
                    failed++
                    FieldValue.fallback(FieldText.payloadText(doc, def, v.payload!!), FallbackReason.CONVERSION_FAILED)
                }
            }
        }
        val converted = FieldDefinitions.mapValues(doc, kind, defKey, convert)
        val withDef = FieldDefinitions.update(converted, defKey, ctx) { newDef }
        return ConversionResult(withDef, failed)
    }

    /** Converted value, or null when it cannot convert. */
    private fun convertValue(
        doc: ProjectDoc,
        old: FieldDef,
        new: FieldDef,
        value: FieldValue,
        from: FieldType,
        to: FieldType,
        linkKind: EntityKind,
    ): FieldValue? {
        val payload = value.payload ?: return null
        return when (to) {
            FieldType.SHORT_TEXT -> FieldValue.of(FieldPayload.Texts(listOf(FieldText.payloadText(doc, old, payload))))
            FieldType.MULTI_CHOICE, FieldType.CHOICE -> when {
                from == FieldType.CHOICE && payload is FieldPayload.Choice -> FieldValue.of(payload)
                payload is FieldPayload.Texts -> {
                    val texts = payload.values.map { it.trim().nfc() }.filter { it.isNotEmpty() }
                    if (to == FieldType.CHOICE && texts.size > 1) return null
                    val keys = texts.mapNotNull { t -> new.options.orEmpty().firstOrNull { it.label == t }?.key }
                    if (keys.size != texts.size) null else FieldValue.of(FieldPayload.Choice(keys))
                }
                else -> null
            }
            FieldType.NUMBER -> (payload as? FieldPayload.Texts)?.values?.singleOrNull()?.let(::readNumber)
                ?.let { FieldValue.of(FieldPayload.Number(it)) }
            FieldType.LINK -> {
                val texts = (payload as? FieldPayload.Texts)?.values?.map { it.trim().nfc() }?.filter { it.isNotEmpty() } ?: return null
                val keys = texts.map { t -> liveNamed(doc, linkKind, t) ?: return null }
                FieldValue.of(FieldPayload.Link(keys))
            }
            else -> null
        }
    }

    /** Key of the live record of [kind] named exactly [name] (NFC), or null. */
    private fun liveNamed(doc: ProjectDoc, kind: EntityKind, name: String): Key? = when (kind) {
        EntityKind.FACTION -> doc.world.factions.firstOrNull { it.deletedAt == null && it.name.nfc() == name }?.key
        EntityKind.PLACE -> doc.world.places.firstOrNull { it.deletedAt == null && it.name.nfc() == name }?.key
        EntityKind.TERM -> doc.world.terms.firstOrNull { it.deletedAt == null && it.name.nfc() == name }?.key
        EntityKind.CHARACTER -> null
    }

    /** Distinct short-text values of the field in entity order, then record order. */
    private fun allTexts(doc: ProjectDoc, kind: EntityKind, defKey: Key): List<String> {
        val base = FieldDefinitions.liveEntities(doc, kind).flatMap { (_, _, fields) ->
            (fields[defKey]?.payload as? FieldPayload.Texts)?.values.orEmpty()
        }
        val fromRecords = doc.changes.records.filter { (it.target as? ChangeTarget.Field)?.fieldDefKey == defKey }
            .flatMap { (it.valueAs(FieldValue.serializer())?.payload as? FieldPayload.Texts)?.values.orEmpty() }
        return (base + fromRecords).map { it.trim().nfc() }.filter { it.isNotEmpty() }.distinct()
    }

    /** Types offered for [def] (empty for long text, TC-F-07). */
    fun offered(def: FieldDef): List<FieldType> = def.type.known?.let(FieldTypes::conversionTargets).orEmpty()
}
