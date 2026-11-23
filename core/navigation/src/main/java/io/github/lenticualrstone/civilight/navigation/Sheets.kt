package io.github.lenticualrstone.civilight.navigation

import androidx.navigation3.runtime.NavKey
import io.github.lenticualrstone.civilight.model.Key
import io.github.lenticualrstone.civilight.model.asset.ImageOwnerKind
import io.github.lenticualrstone.civilight.model.asset.ImageSlot
import io.github.lenticualrstone.civilight.model.cast.EntityKind
import io.github.lenticualrstone.civilight.model.gallery.GalleryOwnerKind
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

/**
 * (0.2.0, N-29, SCR-76) `지난 판` of [episodeKey]: `판 남기기` with its default name on top, then the
 * versions, newest first. [keepOnly] = the episode ⋯ `판 남기기` row (only the name and the button).
 * Opened from the story episode ⋯ and the script's `찾기` command.
 */
@Serializable
data class EpisodeVersionsSheet(override val projectKey: Key, val episodeKey: Key, val keepOnly: Boolean = false) : ProjectSheet {
    override val owner: FeatureModule get() = FeatureModule.SCRIPT
}

/** (0.2.0, N-29, SCR-76) The compare screen of one version of [episodeKey] (`이 판 글로` per block). */
@Serializable
data class VersionCompareSheet(override val projectKey: Key, val episodeKey: Key, val versionKey: Key) : ProjectSheet {
    override val owner: FeatureModule get() = FeatureModule.SCRIPT
}

/**
 * (0.2.0, N-28, SCR-75) Manuscript `바꾸기` with `찾을 말` = [query]: the cast and world name fields'
 * `원고 속 n곳 · 바꾸기` line. Never offered in a read-only project.
 */
@Serializable
data class ManuscriptReplaceSheet(override val projectKey: Key, val query: String) : ProjectSheet {
    override val owner: FeatureModule get() = FeatureModule.SCRIPT
}

/** 씬 나누기 (SCR-50) at [blockKey]. */
@Serializable
data class SceneSplitSheet(override val projectKey: Key, val blockKey: Key) : ProjectSheet {
    override val owner: FeatureModule get() = FeatureModule.SCRIPT
}

/** 번호 다시 매기기 (SCR-22) of [episodeKey]'s numbering unit; opened from the structure ⋯ and the script. */
@Serializable
data class RenumberSheet(override val projectKey: Key, val episodeKey: Key) : ProjectSheet {
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

/**
 * 선 라벨 (CNV-08; CST S18) of a canvas or relationship-map line. A canvas line names its
 * [canvasKey] and [edgeKey]. A relationship-map line (`canvasKey == null`) names the two
 * characters in [fromCharacterKey] / [toCharacterKey]; use [forCharacters], which also fills
 * [edgeKey] with the pair id (`Relation.pairIdOf` order).
 */
@Serializable
data class LineLabelSheet(
    override val projectKey: Key,
    val canvasKey: Key?,
    val edgeKey: Key,
    val fromCharacterKey: Key? = null,
    val toCharacterKey: Key? = null,
) : ProjectSheet {
    override val owner: FeatureModule get() = FeatureModule.CANVAS

    companion object {
        /** The relationship-map line between characters [a] and [b]. */
        fun forCharacters(projectKey: Key, a: Key, b: Key): LineLabelSheet =
            LineLabelSheet(projectKey, canvasKey = null, edgeKey = if (a <= b) "$a|$b" else "$b|$a", fromCharacterKey = a, toCharacterKey = b)
    }
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

/**
 * 이미지 넣기 (GAL-11) into the gallery of [ownerKind] / [ownerKey]: the image picker with several
 * choices, `사진` · `파일`, `이미 있음` marks. Opened from the gallery header `+`, the profile section
 * `넣기` (GAL-01) and the empty gallery (GAL-25). Drawn by canvas.
 */
@Serializable
data class GalleryAddSheet(
    override val projectKey: Key,
    val ownerKind: GalleryOwnerKind,
    val ownerKey: Key,
) : ProjectSheet {
    override val owner: FeatureModule get() = FeatureModule.CANVAS
}

/**
 * 이미지 정보 (GAL-13) of one gallery item: title, description, tags, scene link. [focusDescription]
 * opens with the cursor in `설명` (`설명 쓰기`, GAL-08). Drawn by canvas.
 */
@Serializable
data class GalleryInfoSheet(
    override val projectKey: Key,
    val itemKey: Key,
    val focusDescription: Boolean = false,
) : ProjectSheet {
    override val owner: FeatureModule get() = FeatureModule.CANVAS
}

/** The `⋯` menu (GAL-14) of one gallery item: 초상으로 · 배경으로 · 표지로, order, holo, hide, delete. Drawn by canvas. */
@Serializable
data class GalleryMenuSheet(
    override val projectKey: Key,
    val itemKey: Key,
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

/**
 * 글 메모 (LRC-15), from the command bar `글로` or the inbox; [projectKey] = where it started.
 * [targetKind] / [targetKey] pre-fill the link to a card or scene (LRC-19), with the same strings as
 * [Recording.targetKind].
 */
@Serializable
data class TextMemoSheet(
    val projectKey: Key? = null,
    val targetKind: String? = null,
    val targetKey: Key? = null,
) : SharedSheet {
    override val owner: FeatureModule get() = FeatureModule.RECORDER
}

/**
 * The recorder's link picker (LRC-20), stacked over [TextMemoSheet] on another area's screen. What
 * it edits is held by the recorder's capture hub; [forMemo] = the memo's links (else the record's).
 */
@Serializable
data class RecorderLinkPickerSheet(val forMemo: Boolean = true) : SharedSheet {
    override val owner: FeatureModule get() = FeatureModule.RECORDER
}

/**
 * The `@` link picker of the shared rich text editor (core:ui `RichTextEditor`), drawn by the shell.
 * The pick goes back through `NavResults` under [requestKey] as a core:ui `LinkPick`. [kinds] are
 * `LinkTargetKind` names offered (empty = 인물 · 용어 · 장소); [title] is the caller's copy.
 */
@Serializable
data class RichLinkPickerSheet(
    override val projectKey: Key,
    val requestKey: String,
    val title: String,
    val kinds: List<String> = emptyList(),
) : ProjectSheet {
    override val owner: FeatureModule get() = FeatureModule.SHELL
}
