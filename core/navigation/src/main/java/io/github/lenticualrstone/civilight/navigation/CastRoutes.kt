package io.github.lenticualrstone.civilight.navigation

import io.github.lenticualrstone.civilight.model.Key
import io.github.lenticualrstone.civilight.model.cast.EntityKind
import kotlinx.serialization.Serializable

/** 인물 목록 (CST-01 … CST-06; S1–S3), list or card view. */
@Serializable
data class CastList(override val projectKey: Key) : CastRoute, ProjectRoute, LensScreen, ListPaneRoute

/** 인물 기록 (CST-07 … CST-18; S5–S8) opened on [tab]. */
@Serializable
data class CharacterProfile(
    override val projectKey: Key,
    val characterKey: Key,
    val tab: ProfileTab = ProfileTab.OVERVIEW,
) : CastRoute, ProjectRoute, LensScreen, DetailPaneRoute

/** The five record tabs (CST-08). */
@Serializable
enum class ProfileTab { OVERVIEW, SPEECH, RELATIONS, GAME_TEXT, SECRET }

/** 전체 관계도 (CST-33; S4). */
@Serializable
data class RelationMap(override val projectKey: Key, val focusKey: Key? = null) : CastRoute, ProjectRoute, LensScreen

/** 프로젝트 설정 › 인물 항목 (CST-19; S13) for [kind] (장소 · 용어 · 세력 cards share the engine). */
@Serializable
data class FieldSettings(override val projectKey: Key, val kind: EntityKind = EntityKind.CHARACTER) : CastRoute, ProjectRoute

/** Edit one field definition (CST-23): options, type change preview, hide, delete. */
@Serializable
data class FieldDefEdit(override val projectKey: Key, val fieldDefKey: Key) : CastRoute, ProjectRoute

/** 프로젝트 설정 › 관계 종류 (CST-32; S15). */
@Serializable
data class RelationKindSettings(override val projectKey: Key) : CastRoute, ProjectRoute

/** Tag editing (CST-12). */
@Serializable
data class TagSettings(override val projectKey: Key) : CastRoute, ProjectRoute

/** 아카이브 문서 editor (CST-17); [docKey] null = new document. */
@Serializable
data class ArchiveDocEdit(override val projectKey: Key, val characterKey: Key, val docKey: Key? = null) : CastRoute, ProjectRoute

/** 설정 › 인물 템플릿 (CST-58; S32): app-level template list, `지운 템플릿` when [trash]. */
@Serializable
data class TemplateList(val trash: Boolean = false) : CastRoute

/** 템플릿 고치기 (CST-59; S34) for one app-level template. */
@Serializable
data class TemplateEdit(val templateKey: Key) : CastRoute

/** 템플릿 항목 고치기 (CST-59; S35): one field of a template. */
@Serializable
data class TemplateFieldEdit(val templateKey: Key, val fieldKey: Key) : CastRoute
