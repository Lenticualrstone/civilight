package io.github.lenticualrstone.civilight.navigation

import androidx.navigation3.runtime.NavKey
import io.github.lenticualrstone.civilight.model.Key
import io.github.lenticualrstone.civilight.model.asset.ImageOwnerKind
import io.github.lenticualrstone.civilight.model.asset.ImageSlot
import io.github.lenticualrstone.civilight.model.cast.EntityKind
import io.github.lenticualrstone.civilight.model.project.Rating
import kotlinx.serialization.Serializable

/**
 * A sheet (cover bottom sheet, inner centred sheet or popover) shown over the current screen. Sheets
 * are screen-local state, not back-stack entries: the screen's sheet host keeps a saveable stack of
 * [SheetKey]s, so an open sheet survives process death with its screen (CNV-35).
 *
 * A feature declares its own sheets as `@Serializable` classes implementing [SheetKey] (any package)
 * and draws them itself. Sheets that one area opens over another area's screen are the
 * [SharedSheet]s below; their content is registered once by the owning feature.
 */
interface SheetKey : NavKey

/** Sheets opened across areas; the content is registered by [owner] (see `SheetRegistry`). */
@Serializable
sealed interface SharedSheet : SheetKey {
    val owner: FeatureModule
}

/** Sheets inside one project. */
sealed interface ProjectSheet : SharedSheet {
    val projectKey: Key
}

/** 시점 렌즈 sheet (STO-13), drawn by the shell (core:ui) for every lens screen. */
@Serializable
data class LensSheet(override val projectKey: Key) : ProjectSheet {
    override val owner: FeatureModule get() = FeatureModule.SHELL
}

/** 구조 메뉴 ⋯ of an episode or scene (STO-07), also the scene-head ⋯ (SCR-47). */
@Serializable
data class UnitMenuSheet(override val projectKey: Key, val unitKey: Key) : ProjectSheet {
    override val owner: FeatureModule get() = FeatureModule.STORY
}

/** 단위 만들기 · 다음 씬 만들기 · 앞에 새 씬 (STO-05, SCR-49, SCR-53). */
@Serializable
data class NewUnitSheet(
    override val projectKey: Key,
    val parentKey: Key? = null,
    val afterKey: Key? = null,
    val beforeKey: Key? = null,
) : ProjectSheet {
    override val owner: FeatureModule get() = FeatureModule.STORY
}

/** 씬 옮기기 미리 보기 (STO-08, content SCR-54). */
@Serializable
data class SceneMoveSheet(
    override val projectKey: Key,
    val sceneKey: Key,
    val targetParentKey: Key,
    val afterKey: Key? = null,
) : ProjectSheet {
    override val owner: FeatureModule get() = FeatureModule.STORY
}

/** 씬 · 장소와 때 (STO-20 … STO-27), opened from the script's `시간` tab and the structure. */
@Serializable
data class SceneWhereSheet(override val projectKey: Key, val sceneKey: Key) : ProjectSheet {
    override val owner: FeatureModule get() = FeatureModule.STORY
}

/** 씬 나누기 (SCR-50) at [blockKey]. */
@Serializable
data class SceneSplitSheet(override val projectKey: Key, val blockKey: Key) : ProjectSheet {
    override val owner: FeatureModule get() = FeatureModule.SCRIPT
}

/** 씬 합치기 with the previous scene (SCR-51). */
@Serializable
data class SceneMergeSheet(override val projectKey: Key, val sceneKey: Key) : ProjectSheet {
    override val owner: FeatureModule get() = FeatureModule.SCRIPT
}

/** 항목 변화 기록 (CST S9), for any item with custom fields. */
@Serializable
data class FieldHistorySheet(
    override val projectKey: Key,
    val kind: EntityKind,
    val entityKey: Key,
    val fieldDefKey: Key,
) : ProjectSheet {
    override val owner: FeatureModule get() = FeatureModule.CAST
}

/** 값 고치기 with the two buttons (STO-15, CST S10). */
@Serializable
data class FieldValueEditSheet(
    override val projectKey: Key,
    val kind: EntityKind,
    val entityKey: Key,
    val fieldDefKey: Key,
) : ProjectSheet {
    override val owner: FeatureModule get() = FeatureModule.CAST
}

/** + 항목 (CST-21/22; S12, S14); [entityKey] null = from 프로젝트 설정. */
@Serializable
data class AddFieldSheet(override val projectKey: Key, val kind: EntityKind, val entityKey: Key? = null) : ProjectSheet {
    override val owner: FeatureModule get() = FeatureModule.CAST
}

/** 관계 만들기 · 고치기 (CST-29; S16), also from canvas `관계로 만들기` (CNV-08). */
@Serializable
data class RelationEditSheet(
    override val projectKey: Key,
    val relationKey: Key? = null,
    val endAKey: Key? = null,
    val endBKey: Key? = null,
) : ProjectSheet {
    override val owner: FeatureModule get() = FeatureModule.CAST
}

/** 지우기 · 영향 (CST-40, CST-47, WLD-14; S21) for a character, place, term or faction. */
@Serializable
data class DeleteImpactSheet(override val projectKey: Key, val kind: EntityKind, val itemKey: Key) : ProjectSheet {
    override val owner: FeatureModule get() = FeatureModule.CAST
}

/** 합치기 · 비교 (CST-45, WLD-15; S22). */
@Serializable
data class MergeItemsSheet(
    override val projectKey: Key,
    val kind: EntityKind,
    val keepKey: Key,
    val otherKey: Key,
) : ProjectSheet {
    override val owner: FeatureModule get() = FeatureModule.CAST
}

/** 후보 모두 잇기 (WLD-23, `RTLinkAll`). */
@Serializable
data class LinkCandidatesSheet(override val projectKey: Key, val termKey: Key) : ProjectSheet {
    override val owner: FeatureModule get() = FeatureModule.WORLD
}

/** 사건 만들기 (WLD-32). */
@Serializable
data class EventCreateSheet(override val projectKey: Key) : ProjectSheet {
    override val owner: FeatureModule get() = FeatureModule.WORLD
}

/** 선 라벨 (CNV-08; CST S18) of a canvas or relationship-map line. */
@Serializable
data class LineLabelSheet(override val projectKey: Key, val canvasKey: Key?, val edgeKey: Key) : ProjectSheet {
    override val owner: FeatureModule get() = FeatureModule.CANVAS
}

/** 이미지 고르기 (CNV-27) for an owner's image slot. */
@Serializable
data class ImagePickSheet(
    override val projectKey: Key,
    val ownerKind: ImageOwnerKind,
    val ownerKey: Key,
    val slot: ImageSlot,
) : ProjectSheet {
    override val owner: FeatureModule get() = FeatureModule.CANVAS
}

/** 내보내기 (LIB-41 … LIB-47) of a project. */
@Serializable
data class ExportSheet(override val projectKey: Key) : ProjectSheet {
    override val owner: FeatureModule get() = FeatureModule.LIBRARY
}

/** 등급 바꾸기 · `바뀌는 것` (LIB-24) towards [to]. */
@Serializable
data class RatingChangeSheet(override val projectKey: Key, val to: Rating) : ProjectSheet {
    override val owner: FeatureModule get() = FeatureModule.LIBRARY
}

/** 글 메모 (LRC-15), from the command bar `글로` or the inbox; [projectKey] = where it started. */
@Serializable
data class TextMemoSheet(val projectKey: Key? = null) : SharedSheet {
    override val owner: FeatureModule get() = FeatureModule.RECORDER
}
