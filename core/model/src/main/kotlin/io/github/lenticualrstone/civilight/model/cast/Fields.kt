@file:OptIn(ExperimentalSerializationApi::class)

package io.github.lenticualrstone.civilight.model.cast

import io.github.lenticualrstone.civilight.model.Entity
import io.github.lenticualrstone.civilight.model.HasExt
import io.github.lenticualrstone.civilight.model.Key
import io.github.lenticualrstone.civilight.model.OpenEnum
import io.github.lenticualrstone.civilight.model.OrderKey
import io.github.lenticualrstone.civilight.model.Ordered
import io.github.lenticualrstone.civilight.model.Timestamp
import io.github.lenticualrstone.civilight.model.Trashable
import io.github.lenticualrstone.civilight.model.open
import kotlinx.serialization.EncodeDefault
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject

/** A custom field definition (`project.json › fieldDefs`, §2.5), shared by cast and world cards. */
@Serializable
data class FieldDef(
    override val key: Key,
    val entityKind: OpenEnum<EntityKind>,
    val name: String,
    /** JSON export key; null = [name]. */
    val exportName: String? = null,
    val type: OpenEnum<FieldType>,
    override val order: OrderKey,
    @EncodeDefault val inHeader: Boolean = false,
    @EncodeDefault val inList: Boolean = false,
    /** Only for choice · multiChoice · link. */
    @EncodeDefault val inFilter: Boolean = false,
    @EncodeDefault val isSecret: Boolean = false,
    /** Definition hidden (values kept); not rule-19 item hiding. */
    @EncodeDefault val definitionHidden: Boolean = false,
    /** Set for `이 인물만` fields. */
    val scopeCharacterKey: Key? = null,
    val shortTextMultiple: Boolean? = null,
    /** Number unit text (`cm`); never converted. */
    val unit: String? = null,
    val options: List<FieldOption>? = null,
    val allowFreeText: Boolean? = null,
    val datePrecision: OpenEnum<DatePrecision>? = null,
    val linkTargetKind: OpenEnum<FieldLinkTargetKind>? = null,
    val calc: CalcSpec? = null,
    /** Bundle field id this came from (`basic.birth`). */
    val bundleOrigin: String? = null,
    override val deletedAt: Timestamp? = null,
    override val trashEntryKey: Key? = null,
    @EncodeDefault override val updatedAt: Timestamp = 0L,
    @EncodeDefault override val updatedBy: String = "",
    override val ext: JsonObject? = null,
) : Entity, Ordered, Trashable

/** `짧은 글 · 긴 글 · 숫자 · 고르기 · 여러 개 고르기 · 날짜 · 기념일 · 연결 · 계산`. */
enum class FieldType { SHORT_TEXT, LONG_TEXT, NUMBER, CHOICE, MULTI_CHOICE, DATE, ANNIVERSARY, LINK, CALC }

enum class DatePrecision { YEAR, YMD }

/** Targets of a `연결` field (never characters). */
enum class FieldLinkTargetKind { FACTION, PLACE, TERM }

@Serializable
data class FieldOption(
    val key: Key,
    val label: String,
    override val order: OrderKey,
    override val ext: JsonObject? = null,
) : HasExt, Ordered

/** A calculated field: `나이` from `출생`. */
@Serializable
data class CalcSpec(
    @EncodeDefault val formula: OpenEnum<CalcFormula> = CalcFormula.AGE.open(),
    val sourceFieldDefKey: Key,
    override val ext: JsonObject? = null,
) : HasExt

enum class CalcFormula { AGE }

/**
 * A field value (§2.5), embedded in its entity's `fields` map and used as the `value` of `field`
 * change records. Empty = no entry (never stored).
 */
@Serializable
data class FieldValue(
    val state: OpenEnum<FieldValueState>,
    /** Present iff [state] is value. */
    val payload: FieldPayload? = null,
    /** Present iff [state] is fallbackText. */
    val fallbackText: String? = null,
    val fallbackReason: OpenEnum<FallbackReason>? = null,
    override val ext: JsonObject? = null,
) : HasExt {
    companion object {
        val UNKNOWN = FieldValue(FieldValueState.UNKNOWN.open())

        fun of(payload: FieldPayload) = FieldValue(FieldValueState.VALUE.open(), payload)

        fun fallback(text: String, reason: FallbackReason) =
            FieldValue(FieldValueState.FALLBACK_TEXT.open(), fallbackText = text, fallbackReason = reason.open())
    }
}

/** `unknown` = `??`; `value`; `fallbackText` = kept text after an option removal or failed conversion. */
enum class FieldValueState { UNKNOWN, VALUE, FALLBACK_TEXT }

/** `◇ 보기에 없는 값` / `◇ 바꾸지 못한 값`. */
enum class FallbackReason { OPTION_REMOVED, CONVERSION_FAILED }
