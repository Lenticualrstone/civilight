@file:OptIn(ExperimentalSerializationApi::class)

package io.github.lenticualrstone.civilight.model.app

import io.github.lenticualrstone.civilight.model.Key
import io.github.lenticualrstone.civilight.model.OpenEnum
import io.github.lenticualrstone.civilight.model.OrderKey
import io.github.lenticualrstone.civilight.model.Timestamp
import io.github.lenticualrstone.civilight.model.cast.DatePrecision
import io.github.lenticualrstone.civilight.model.cast.EntityKind
import io.github.lenticualrstone.civilight.model.cast.FieldLinkTargetKind
import io.github.lenticualrstone.civilight.model.cast.FieldType
import kotlinx.serialization.EncodeDefault
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject

/**
 * `files/app/fieldTemplates.json` (CST-63): the user's 인물 템플릿 (app level, never in `.civilight`).
 * A missing or unreadable file reads as empty.
 */
@Serializable
data class FieldTemplatesFile(
    @EncodeDefault val formatVersion: Int = FORMAT_VERSION,
    val templates: List<FieldTemplate> = emptyList(),
    val ext: JsonObject? = null,
) {
    companion object {
        const val FORMAT_VERSION = 1
    }
}

/**
 * A named, app-level set of field definitions of any of the four kinds (CST-56). It never holds
 * values, characters, change records, project names or project keys.
 */
@Serializable
data class FieldTemplate(
    val key: Key,
    /** NFC, trimmed, non-empty; not unique. */
    val name: String,
    /** `숨긴 템플릿` (CST-62): shown only while the hidden group is open. */
    @EncodeDefault val hidden: Boolean = false,
    val fields: List<TemplateField> = emptyList(),
    /** List order (creation order in v1). */
    val order: OrderKey,
    @EncodeDefault val createdAt: Timestamp = 0L,
    /** Decides the restore merge (newer wins). */
    @EncodeDefault val updatedAt: Timestamp = 0L,
    /** `지운 템플릿` (30 days). */
    val deletedAt: Timestamp? = null,
    val ext: JsonObject? = null,
) {
    companion object {
        /** Days a deleted template stays in `지운 템플릿`. */
        const val TRASH_DAYS = 30
        const val TRASH_MS: Long = TRASH_DAYS * 24L * 60 * 60 * 1000
    }
}

/** One field definition inside a template (CST-56, §7.3). */
@Serializable
data class TemplateField(
    /** Template-local key. */
    val key: Key,
    /** `basic.birth`, `gameProfile.height`, or `tpl:<key>`; kept from the first save on. */
    val origin: String,
    val entityKind: OpenEnum<EntityKind>,
    val name: String,
    val exportName: String? = null,
    val type: OpenEnum<FieldType>,
    /** Order inside its kind. */
    val order: OrderKey,
    @EncodeDefault val inHeader: Boolean = false,
    @EncodeDefault val inList: Boolean = false,
    /** Only for 고르기 · 여러 개 고르기 · 연결. */
    @EncodeDefault val inFilter: Boolean = false,
    @EncodeDefault val isSecret: Boolean = false,
    val shortTextMultiple: Boolean? = null,
    val unit: String? = null,
    val allowFreeText: Boolean? = null,
    /** Option labels in order; new option keys are made on apply. */
    val options: List<String> = emptyList(),
    val datePrecision: OpenEnum<DatePrecision>? = null,
    val linkTargetKind: OpenEnum<FieldLinkTargetKind>? = null,
    /** Another [TemplateField.key] (a `날짜` field); null = typed age. */
    val calcSourceKey: Key? = null,
    /** Keys of the project definitions this field was saved from (opaque; matching only). */
    val sourceDefKeys: List<Key> = emptyList(),
    val ext: JsonObject? = null,
)
