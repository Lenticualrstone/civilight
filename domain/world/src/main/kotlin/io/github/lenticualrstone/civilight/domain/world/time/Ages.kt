package io.github.lenticualrstone.civilight.domain.world.time

import io.github.lenticualrstone.civilight.model.Key
import io.github.lenticualrstone.civilight.model.cast.CalcFormula
import io.github.lenticualrstone.civilight.model.cast.Character
import io.github.lenticualrstone.civilight.model.cast.EntityKind
import io.github.lenticualrstone.civilight.model.cast.FieldPayload
import io.github.lenticualrstone.civilight.model.cast.FieldType
import io.github.lenticualrstone.civilight.model.cast.FieldValueState
import io.github.lenticualrstone.civilight.model.doc.ProjectDoc
import io.github.lenticualrstone.civilight.model.project.WorldTimeMode

/** A character's `출생` as the age calculation reads it. */
sealed interface Birth {
    data class Known(val year: Int, val month: Int? = null, val day: Int? = null) : Birth

    /** `알 수 없음` (`??`). */
    data object Unknown : Birth
}

/** An age at some time: years, or `??`. */
sealed interface Age {
    data class Years(val years: Int) : Age
    data object Unknown : Age

    fun label(): String = when (this) {
        is Years -> "${years}세"
        Unknown -> "??세"
    }
}

/** `그때 나이` (WLD-35): ages at a world time from the `출생` field. */
class Ages(private val doc: ProjectDoc) {
    private val clock = WorldClock(doc.project.worldTime)

    /** The `출생` field: the source of the `나이` calculation, else a date field from the `basic.birth` bundle. */
    val birthFieldKey: Key? by lazy {
        val defs = doc.index.fieldDefs(EntityKind.CHARACTER).filter { it.deletedAt == null }
        defs.firstNotNullOfOrNull { d -> d.calc?.takeIf { it.formula.known == CalcFormula.AGE }?.sourceFieldDefKey }
            ?: defs.firstOrNull { it.type.known == FieldType.DATE && it.bundleOrigin == "basic.birth" }?.key
            ?: defs.firstOrNull { it.type.known == FieldType.DATE && it.name == "출생" }?.key
    }

    private val birthdayFieldKey: Key? by lazy {
        doc.index.fieldDefs(EntityKind.CHARACTER).firstOrNull { it.deletedAt == null && it.type.known == FieldType.ANNIVERSARY && it.bundleOrigin == "basic.birthday" }?.key
    }

    /** The character's birth, or null when `출생` is empty. */
    fun birthOf(character: Character): Birth? {
        val key = birthFieldKey ?: return null
        val value = character.fields[key] ?: return null
        return when (value.state.known) {
            FieldValueState.UNKNOWN -> Birth.Unknown
            FieldValueState.VALUE -> {
                val date = value.payload as? FieldPayload.Date ?: return null
                val anniversary = birthdayFieldKey?.let { character.fields[it]?.payload as? FieldPayload.Anniversary }
                Birth.Known(date.year, date.month ?: anniversary?.month, date.day ?: anniversary?.day)
            }
            else -> null
        }
    }

    /**
     * Age at [year] (and, in `세계 달력`, [month]/[day]): `연도만` = year difference; calendar =
     * 만 나이 when both dates have month and day. Null when there is no `출생` or the character is not
     * born yet.
     */
    fun ageAt(character: Character, year: Int, month: Int? = null, day: Int? = null): Age? = when (val birth = birthOf(character)) {
        null -> null
        Birth.Unknown -> Age.Unknown
        is Birth.Known -> {
            var years = year - birth.year
            val calendar = clock.mode == WorldTimeMode.CALENDAR
            if (calendar && month != null && day != null && birth.month != null && birth.day != null) {
                if (month < birth.month || (month == birth.month && day < birth.day)) years -= 1
            }
            if (years < 0) null else Age.Years(years)
        }
    }

    /** `리엔 12세`, `하르트 ??세`, or null when omitted. */
    fun label(character: Character, year: Int, month: Int? = null, day: Int? = null): String? =
        ageAt(character, year, month, day)?.let { "${character.name} ${it.label()}" }

    /** Labels of [characters] that have an age at that time, joined by ` · `. */
    fun line(characters: List<Character>, year: Int, month: Int? = null, day: Int? = null): String =
        characters.mapNotNull { label(it, year, month, day) }.joinToString(" · ")
}
