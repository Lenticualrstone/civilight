package io.github.lenticualrstone.civilight.domain.cast.field

import io.github.lenticualrstone.civilight.domain.cast.timed.ReadPoint
import io.github.lenticualrstone.civilight.domain.cast.timed.recordAt
import io.github.lenticualrstone.civilight.model.Key
import io.github.lenticualrstone.civilight.model.cast.EntityKind
import io.github.lenticualrstone.civilight.model.cast.FieldDef
import io.github.lenticualrstone.civilight.model.cast.FieldPayload
import io.github.lenticualrstone.civilight.model.cast.FieldType
import io.github.lenticualrstone.civilight.model.cast.FieldValue
import io.github.lenticualrstone.civilight.model.change.ChangeRecord
import io.github.lenticualrstone.civilight.model.change.ChangeTarget
import io.github.lenticualrstone.civilight.model.doc.ProjectDoc
import io.github.lenticualrstone.civilight.model.doc.history
import io.github.lenticualrstone.civilight.model.nfc
import io.github.lenticualrstone.civilight.model.open
import io.github.lenticualrstone.civilight.model.project.WorldTimeMode

/** How a value was made when it was not typed. */
enum class Derivation {
    /** `나이` from `출생` at the read point (CST-25). */
    AGE_FROM_BIRTH,

    /** `생일` from `출생` month and day (CST-25, Q7 default: derived, read-only). */
    BIRTHDAY_FROM_BIRTH,
}

/** A field's value at a read point. */
data class FieldReading(
    val def: FieldDef,
    /** The value in effect; null = empty. */
    val value: FieldValue?,
    /** The record in effect; null = base value (or derived). */
    val record: ChangeRecord?,
    /** The field has live `◆` records (never for a computed age). */
    val hasHistory: Boolean,
    val derivation: Derivation? = null,
) {
    val state: ValueState get() = value.valueState
    val isEmpty: Boolean get() = state == ValueState.EMPTY

    /** Read-only (computed) values cannot be typed. */
    val readOnly: Boolean get() = derivation != null
}

/**
 * Reads field values over time (CST-09, CST-10, CST-25): base value, `◆` records at the read point,
 * the age calculation and the derived birthday.
 */
object FieldReader {
    /** Bundle ids used to find the birth and birthday fields (CST-24). */
    const val ORIGIN_BIRTH = "basic.birth"
    const val ORIGIN_AGE = "basic.age"
    const val ORIGIN_BIRTHDAY = "basic.birthday"

    fun target(kind: EntityKind, entityKey: Key, def: FieldDef): ChangeTarget.Field =
        ChangeTarget.Field(kind.open(), entityKey, def.key)

    /** Base value (before any record), or null. */
    fun baseValue(doc: ProjectDoc, kind: EntityKind, entityKey: Key, defKey: Key): FieldValue? =
        doc.index.fieldsOf(kind, entityKey)?.get(defKey)

    /** The stored (typed) value at [point]: record in effect, else base. */
    fun storedAt(doc: ProjectDoc, kind: EntityKind, entityKey: Key, def: FieldDef, point: ReadPoint): Pair<FieldValue?, ChangeRecord?> {
        val record = doc.recordAt(target(kind, entityKey, def), point)
        return if (record != null) record.valueAs(FieldValue.serializer()) to record else baseValue(doc, kind, entityKey, def.key) to null
    }

    fun read(
        doc: ProjectDoc,
        kind: EntityKind,
        entityKey: Key,
        def: FieldDef,
        point: ReadPoint,
        dates: StoryDateSource = StoryDates.Default,
    ): FieldReading {
        val type = def.type.known
        if (type == FieldType.CALC) return readAge(doc, kind, entityKey, def, point, dates)
        if (type == FieldType.ANNIVERSARY && isBirthday(def)) {
            birthOf(doc, def.entityKind.known ?: kind)?.let { birthDef ->
                val (birth, _) = storedAt(doc, kind, entityKey, birthDef, point)
                val date = birth?.payload as? FieldPayload.Date
                if (date?.month != null && date.day != null) {
                    return FieldReading(def, FieldValue.of(FieldPayload.Anniversary(date.month!!, date.day!!)), null, false, Derivation.BIRTHDAY_FROM_BIRTH)
                }
            }
        }
        val (value, record) = storedAt(doc, kind, entityKey, def, point)
        val hasHistory = doc.history(target(kind, entityKey, def)).isNotEmpty()
        return FieldReading(def, value, record, hasHistory)
    }

    /**
     * `나이` (CST-25): from `출생` when it has a value and the project has world time; otherwise the
     * typed age with its own history; `??` when 출생 is `??` and nothing is typed.
     */
    private fun readAge(
        doc: ProjectDoc,
        kind: EntityKind,
        entityKey: Key,
        def: FieldDef,
        point: ReadPoint,
        dates: StoryDateSource,
    ): FieldReading {
        val mode = doc.project.worldTime.mode.known ?: WorldTimeMode.YEAR_ONLY
        // A source in the trash (or purged, or missing) reads as empty (CST-55, TC-F-31).
        val sourceDef = liveCalcSource(doc, def)
        val source = sourceDef?.let { storedAt(doc, kind, entityKey, it, point).first }
        val birth = source?.payload as? FieldPayload.Date
        if (birth != null && mode != WorldTimeMode.NONE && source.valueState == ValueState.VALUE) {
            val at = dates.dateAt(doc, point)
            if (at != null) {
                val years = age(birth, at, mode == WorldTimeMode.CALENDAR)
                val value = if (years >= 0) FieldValue.of(FieldPayload.Number(years.toString())) else null
                return FieldReading(def, value, null, hasHistory = false, derivation = Derivation.AGE_FROM_BIRTH)
            }
        }
        val (typed, record) = storedAt(doc, kind, entityKey, def, point)
        val hasHistory = doc.history(target(kind, entityKey, def)).isNotEmpty()
        if (typed != null) return FieldReading(def, typed, record, hasHistory)
        if (source.valueState == ValueState.UNKNOWN) return FieldReading(def, FieldValue.UNKNOWN, null, hasHistory, Derivation.AGE_FROM_BIRTH)
        return FieldReading(def, null, null, hasHistory)
    }

    /** The live source definition of a `계산` field; null when none, trashed or purged (CST-55). */
    fun liveCalcSource(doc: ProjectDoc, def: FieldDef): FieldDef? =
        def.calc?.sourceFieldDefKey?.let { doc.index.fieldDef(it) }?.takeIf { it.deletedAt == null }

    /**
     * Age in years: `연도만` subtracts years; `세계 달력` gives 만 나이 when both dates have month and
     * day, else falls back to year subtraction (TC-F-25 [PROPOSED] fallback).
     */
    fun age(birth: FieldPayload.Date, at: StoryDate, calendar: Boolean): Int {
        var years = at.year - birth.year
        val bm = birth.month
        val bd = birth.day
        val am = at.month
        val ad = at.day
        if (calendar && bm != null && bd != null && am != null && ad != null) {
            if (am < bm || (am == bm && ad < bd)) years -= 1
        }
        return years
    }

    /** The anniversary field fed by `출생` (bundle id `basic.birthday`, else the name `생일`). */
    fun isBirthday(def: FieldDef): Boolean =
        def.type.known == FieldType.ANNIVERSARY && (def.bundleOrigin == ORIGIN_BIRTHDAY || (def.bundleOrigin == null && def.name.nfc() == "생일"))

    /** The live `출생` date field of a kind (bundle id `basic.birth`, else the name `출생`). */
    fun birthOf(doc: ProjectDoc, kind: EntityKind): FieldDef? = doc.index.fieldDefs(kind)
        .filter { it.deletedAt == null && it.type.known == FieldType.DATE }
        .let { defs -> defs.firstOrNull { it.bundleOrigin == ORIGIN_BIRTH } ?: defs.firstOrNull { it.bundleOrigin == null && it.name.nfc() == "출생" } }
}
