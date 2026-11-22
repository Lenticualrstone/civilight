package io.github.lenticualrstone.civilight.domain.cast.field

import io.github.lenticualrstone.civilight.domain.cast.EditContext
import io.github.lenticualrstone.civilight.domain.cast.timed.TimedValues
import io.github.lenticualrstone.civilight.model.Key
import io.github.lenticualrstone.civilight.model.cast.EntityKind
import io.github.lenticualrstone.civilight.model.cast.FieldDef
import io.github.lenticualrstone.civilight.model.cast.FieldPayload
import io.github.lenticualrstone.civilight.model.cast.FieldValue
import io.github.lenticualrstone.civilight.model.change.ChangeRecord
import io.github.lenticualrstone.civilight.model.doc.ProjectDoc
import io.github.lenticualrstone.civilight.model.position.LensPoint
import io.github.lenticualrstone.civilight.model.text.isBlankText
import kotlinx.serialization.json.JsonElement

/** Writing field values (CST-09, CST-10). Empty is never stored: a null value removes the entry. */
object FieldValueEdits {
    /** The value, or null when it holds nothing (blank texts, no options, no targets). */
    fun normalized(value: FieldValue?): FieldValue? {
        if (value == null) return null
        if (value.valueState != ValueState.VALUE) return value
        val empty = when (val p = value.payload) {
            is FieldPayload.Texts -> p.values.all { it.isBlank() }
            is FieldPayload.Rich -> p.doc.all { it.runs.isBlankText() }
            is FieldPayload.Number -> p.value.isBlank()
            is FieldPayload.Choice -> p.optionKeys.isEmpty() && p.freeText.isNullOrBlank()
            is FieldPayload.Link -> p.targetKeys.isEmpty()
            else -> false
        }
        return if (empty) null else value
    }

    /** JSON of a field value for a change record (null = empty from there on). */
    fun encode(value: FieldValue?): JsonElement? = normalized(value)?.let { ChangeRecord.encodeValue(FieldValue.serializer(), it) }

    /** Rewrites the `fields` map of one entity. */
    fun updateFields(
        doc: ProjectDoc,
        kind: EntityKind,
        entityKey: Key,
        ctx: EditContext?,
        transform: (Map<Key, FieldValue>) -> Map<Key, FieldValue>,
    ): ProjectDoc {
        val at = ctx?.now
        val by = ctx?.device
        return when (kind) {
            EntityKind.CHARACTER -> doc.copy(cast = doc.cast.copy(characters = doc.cast.characters.map { c ->
                if (c.key != entityKey) c else c.copy(fields = transform(c.fields), updatedAt = at ?: c.updatedAt, updatedBy = by ?: c.updatedBy)
            }))
            EntityKind.PLACE -> doc.copy(world = doc.world.copy(places = doc.world.places.map { p ->
                if (p.key != entityKey) p else p.copy(fields = transform(p.fields), updatedAt = at ?: p.updatedAt, updatedBy = by ?: p.updatedBy)
            }))
            EntityKind.TERM -> doc.copy(world = doc.world.copy(terms = doc.world.terms.map { t ->
                if (t.key != entityKey) t else t.copy(fields = transform(t.fields), updatedAt = at ?: t.updatedAt, updatedBy = by ?: t.updatedBy)
            }))
            EntityKind.FACTION -> doc.copy(world = doc.world.copy(factions = doc.world.factions.map { f ->
                if (f.key != entityKey) f else f.copy(fields = transform(f.fields), updatedAt = at ?: f.updatedAt, updatedBy = by ?: f.updatedBy)
            }))
        }
    }

    /** Rewrites the `fields` map of every entity of [kind] (live and trashed). */
    fun updateAllFields(doc: ProjectDoc, kind: EntityKind, transform: (Map<Key, FieldValue>) -> Map<Key, FieldValue>): ProjectDoc =
        when (kind) {
            EntityKind.CHARACTER -> doc.copy(cast = doc.cast.copy(characters = doc.cast.characters.map { it.copy(fields = transform(it.fields)) }))
            EntityKind.PLACE -> doc.copy(world = doc.world.copy(places = doc.world.places.map { it.copy(fields = transform(it.fields)) }))
            EntityKind.TERM -> doc.copy(world = doc.world.copy(terms = doc.world.terms.map { it.copy(fields = transform(it.fields)) }))
            EntityKind.FACTION -> doc.copy(world = doc.world.copy(factions = doc.world.factions.map { it.copy(fields = transform(it.fields)) }))
        }

    /** Sets (or with null, empties) the base value. */
    fun setBase(doc: ProjectDoc, kind: EntityKind, entityKey: Key, defKey: Key, value: FieldValue?, ctx: EditContext): ProjectDoc {
        val v = normalized(value)
        return updateFields(doc, kind, entityKey, ctx) { fields -> if (v == null) fields - defKey else fields + (defKey to v) }
    }

    /** `알 수 없음` (`??`). */
    fun setUnknown(doc: ProjectDoc, kind: EntityKind, entityKey: Key, defKey: Key, ctx: EditContext): ProjectDoc =
        setBase(doc, kind, entityKey, defKey, FieldValue.UNKNOWN, ctx)

    /** `이 값 고치기` (CST-10): the record in effect at [lens], else the base value. */
    fun correctInEffect(
        doc: ProjectDoc,
        kind: EntityKind,
        entityKey: Key,
        def: FieldDef,
        value: FieldValue?,
        lens: LensPoint,
        ctx: EditContext,
    ): ProjectDoc = TimedValues.correctInEffect(doc, FieldReader.target(kind, entityKey, def), encode(value), lens, ctx) {
        setBase(it, kind, entityKey, def.key, value, ctx)
    }

    /** `여기서부터 바뀜` (CST-10): a new `◆` after the 집필 선 at `최신`, from the lens unit's start at a past lens. */
    fun changeFromHere(
        doc: ProjectDoc,
        kind: EntityKind,
        entityKey: Key,
        def: FieldDef,
        value: FieldValue?,
        lens: LensPoint,
        ctx: EditContext,
        note: String? = null,
    ): ProjectDoc = TimedValues.changeFromHere(doc, FieldReader.target(kind, entityKey, def), encode(value), lens, ctx, { d ->
        setBase(d, kind, entityKey, def.key, value, ctx)
    }, note)

    /** `시점 추가`: a point record from the start of [unitKey]. */
    fun addPointRecord(
        doc: ProjectDoc,
        kind: EntityKind,
        entityKey: Key,
        def: FieldDef,
        value: FieldValue?,
        unitKey: Key,
        ctx: EditContext,
        note: String? = null,
    ): Pair<ProjectDoc, ChangeRecord> = TimedValues.addRecord(
        doc, FieldReader.target(kind, entityKey, def), encode(value),
        io.github.lenticualrstone.civilight.model.change.Anchor.UnitStart(unitKey), ctx, note,
    )

    /** `다른 항목 고르기` on a dotted `연결` value: replaces one target key in the base value. */
    fun replaceLinkTarget(doc: ProjectDoc, kind: EntityKind, entityKey: Key, defKey: Key, from: Key, to: Key, ctx: EditContext): ProjectDoc =
        updateFields(doc, kind, entityKey, ctx) { fields ->
            val v = fields[defKey]
            val p = v?.payload as? FieldPayload.Link ?: return@updateFields fields
            fields + (defKey to v.copy(payload = p.copy(targetKeys = p.targetKeys.map { if (it == from) to else it }.distinct())))
        }

    /** `비우기` on a dotted `연결` value: removes one target key (the value becomes empty if none is left). */
    fun removeLinkTarget(doc: ProjectDoc, kind: EntityKind, entityKey: Key, defKey: Key, target: Key, ctx: EditContext): ProjectDoc =
        updateFields(doc, kind, entityKey, ctx) { fields ->
            val v = fields[defKey]
            val p = v?.payload as? FieldPayload.Link ?: return@updateFields fields
            val next = normalized(v.copy(payload = p.copy(targetKeys = p.targetKeys - target)))
            if (next == null) fields - defKey else fields + (defKey to next)
        }
}
