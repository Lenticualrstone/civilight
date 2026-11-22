package io.github.lenticualrstone.civilight.domain.cast.export

import io.github.lenticualrstone.civilight.domain.cast.field.FieldDefinitions
import io.github.lenticualrstone.civilight.domain.cast.field.FieldReader
import io.github.lenticualrstone.civilight.domain.cast.field.FieldText
import io.github.lenticualrstone.civilight.domain.cast.field.FieldValueEdits
import io.github.lenticualrstone.civilight.domain.cast.field.ValueState
import io.github.lenticualrstone.civilight.domain.cast.field.valueState
import io.github.lenticualrstone.civilight.domain.cast.relation.RelationReader
import io.github.lenticualrstone.civilight.domain.cast.relation.RelationViews
import io.github.lenticualrstone.civilight.domain.story.lens.LensView
import io.github.lenticualrstone.civilight.model.Key
import io.github.lenticualrstone.civilight.model.cast.Character
import io.github.lenticualrstone.civilight.model.cast.EntityKind
import io.github.lenticualrstone.civilight.model.cast.FieldDef
import io.github.lenticualrstone.civilight.model.cast.FieldPayload
import io.github.lenticualrstone.civilight.model.cast.FieldType
import io.github.lenticualrstone.civilight.model.cast.FieldValue
import io.github.lenticualrstone.civilight.model.change.ChangeRecord
import io.github.lenticualrstone.civilight.model.doc.ProjectDoc
import io.github.lenticualrstone.civilight.model.doc.history
import io.github.lenticualrstone.civilight.model.text.docText
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

/** Display-layer switches of the export sheet that touch cast data. */
data class CharacterExportOptions(
    /** `비밀 항목 포함` (default off, CST-18, CST-26). */
    val includeSecret: Boolean = false,
    /** The JSON option to export 호칭 only (default off, rule 13 §9, CST-35). */
    val addresses: Boolean = false,
)

/**
 * Cast part of the game-engine JSON (`characters[]`, LIB-45, CST-26, CST-35):
 * `{ "id", "name", "displayName", "fields": { "<exportName>": value | [ {from, value} ] } }`.
 * Fields are keyed by their export name (default: the field name), so renaming a field keeps the
 * key. A field with `◆` history exports a per-point list. Hidden characters give only id · name ·
 * display name (rule 19 §5). Relations are not exported, except 호칭 when the option is on
 * (`addresses`, keyed by partner id; the key and placement are `ED`, CST Q29).
 */
object CharacterExport {
    const val ADDRESSES = "addresses"

    fun exportKey(def: FieldDef): String = def.exportName?.takeIf { it.isNotBlank() } ?: def.name

    fun characters(doc: ProjectDoc, keys: List<Key>, options: CharacterExportOptions = CharacterExportOptions()): JsonArray =
        buildJsonArray { for (k in keys) doc.index.character(k)?.let { add(entry(doc, it, options)) } }

    fun entry(doc: ProjectDoc, character: Character, options: CharacterExportOptions = CharacterExportOptions()): JsonObject = buildJsonObject {
        put("id", character.key)
        put("name", character.name)
        put("displayName", character.displayNameBase ?: character.name)
        if (character.hidden) return@buildJsonObject
        put("fields", fields(doc, character.key, options.includeSecret))
        if (options.addresses) addresses(doc, character.key)?.let { put(ADDRESSES, it) }
    }

    /** The `fields` object: live, not hidden definitions that apply to the character; secret ones only on request. */
    fun fields(doc: ProjectDoc, characterKey: Key, includeSecret: Boolean = false): JsonObject = buildJsonObject {
        for (def in FieldDefinitions.visibleFor(doc, EntityKind.CHARACTER, characterKey)) {
            if (def.isSecret && !includeSecret) continue
            field(doc, characterKey, def)?.let { put(exportKey(def), it) }
        }
    }

    /** One field: its value, a per-point list when it has `◆` records, or null when empty (left out). */
    fun field(doc: ProjectDoc, characterKey: Key, def: FieldDef): JsonElement? {
        val target = FieldReader.target(EntityKind.CHARACTER, characterKey, def)
        val base = FieldReader.baseValue(doc, EntityKind.CHARACTER, characterKey, def.key)
        val history = if (def.type.known == FieldType.CALC && base == null) emptyList() else doc.history(target)
        if (history.isEmpty()) return value(doc, def, base)
        return buildJsonArray {
            if (FieldValueEdits.normalized(base) != null) add(point(null, value(doc, def, base)))
            for (r in history) add(point(from(doc, r), value(doc, def, r.valueAs(FieldValue.serializer()))))
        }
    }

    private fun point(from: String?, value: JsonElement?): JsonObject = buildJsonObject {
        put("from", from?.let(::JsonPrimitive) ?: JsonNull)
        put("value", value ?: JsonNull)
    }

    /** `from` of a record: `1-3`, `1-7 #043`, `1-7 씬 3 뒤`, or the world event's title. */
    fun from(doc: ProjectDoc, record: ChangeRecord): String = LensView(doc).anchorLabel(record.anchor)

    /**
     * JSON of one value: text, number, option label(s), `{year, month?, day?}`, `{month, day}`,
     * linked `[{id, name}]`; `??` for unknown; fallback text as text; null when empty.
     */
    fun value(doc: ProjectDoc, def: FieldDef, value: FieldValue?): JsonElement? = when (value.valueState) {
        ValueState.EMPTY -> null
        ValueState.UNKNOWN -> JsonPrimitive("??")
        ValueState.FALLBACK -> JsonPrimitive(value?.fallbackText.orEmpty())
        ValueState.VALUE -> when (val p = value!!.payload!!) {
            is FieldPayload.Texts -> {
                val texts = p.values.filter { it.isNotBlank() }
                if (def.shortTextMultiple == true) JsonArray(texts.map(::JsonPrimitive)) else JsonPrimitive(texts.joinToString(" · "))
            }
            is FieldPayload.Rich -> JsonPrimitive(p.doc.docText)
            is FieldPayload.Number -> p.value.trim().toBigDecimalOrNull()?.let(::JsonPrimitive) ?: JsonPrimitive(p.value)
            is FieldPayload.Choice -> {
                val labels = p.optionKeys.mapNotNull { k -> def.options.orEmpty().firstOrNull { it.key == k }?.label } +
                    listOfNotNull(p.freeText?.takeIf { it.isNotBlank() })
                if (def.type.known == FieldType.MULTI_CHOICE) JsonArray(labels.map(::JsonPrimitive)) else JsonPrimitive(labels.joinToString(" · "))
            }
            is FieldPayload.Date -> buildJsonObject {
                put("year", p.year)
                p.month?.let { put("month", it) }
                p.day?.let { put("day", it) }
            }
            is FieldPayload.Anniversary -> buildJsonObject {
                put("month", p.month)
                put("day", p.day)
            }
            is FieldPayload.Link -> {
                val refs = FieldText.format(doc, def, value, hiddenUnlocked = true).refs
                buildJsonArray {
                    p.targetKeys.forEachIndexed { i, k ->
                        add(buildJsonObject {
                            put("id", k)
                            put("name", refs.getOrNull(i)?.narrowLabel.orEmpty())
                        })
                    }
                }
            }
            is FieldPayload.Unknown -> p.raw
        }
    }

    /**
     * 호칭 only (rule 13 §9): what this character calls each live partner, keyed by the partner's id;
     * a per-point list when the slot has `◆` records. Null when the character calls no one anything.
     */
    fun addresses(doc: ProjectDoc, characterKey: Key): JsonObject? {
        val out = LinkedHashMap<String, JsonElement>()
        for (r in RelationViews.relationsOf(doc, characterKey)) {
            if (!doc.liveness.isLive(r)) continue
            val side = RelationReader.sideOf(r, characterKey) ?: continue
            val slot = RelationReader.addressSlot(side)
            val base = RelationReader.cleanAddress(r.baseAddress(slot))
            val records = doc.history(RelationReader.target(r.key, slot))
            val partner = RelationReader.endKey(r, side.other)
            when {
                records.isEmpty() -> base?.let { out[partner] = JsonPrimitive(it) }
                else -> out[partner] = buildJsonArray {
                    base?.let { add(point(null, JsonPrimitive(it))) }
                    for (rec in records) add(point(from(doc, rec), RelationReader.addressOf(rec)?.let(::JsonPrimitive)))
                }
            }
        }
        return if (out.isEmpty()) null else JsonObject(out)
    }
}
