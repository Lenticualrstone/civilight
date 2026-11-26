package io.github.lenticualrstone.civilight.feature.story.menu

import io.github.lenticualrstone.civilight.domain.story.scene.SceneHeads
import io.github.lenticualrstone.civilight.domain.story.structure.StoryOverview
import io.github.lenticualrstone.civilight.feature.story.common.StoryCopy
import io.github.lenticualrstone.civilight.feature.story.common.isDrawnCopy
import io.github.lenticualrstone.civilight.feature.story.move.MoveModel
import io.github.lenticualrstone.civilight.feature.story.structure.StructureCopy
import io.github.lenticualrstone.civilight.model.Key
import io.github.lenticualrstone.civilight.model.doc.ProjectDoc
import io.github.lenticualrstone.civilight.model.story.StoryUnit

/** What a ⋯ row does. */
enum class MenuAction {
    /** → `SceneWhereSheet` (STO-20). */
    PLACE_TIME,

    /** → `SceneWhereSheet`, whose `전환 문구` row edits it (STO-27, SCR-48). */
    TRANSITION,

    /** → `SceneMergeSheet` (SCR-51). */
    MERGE_PREVIOUS,

    /** Opens the scene in the script; the split happens there below a block (STO-07 Q5 `ED`). */
    SPLIT_HERE,

    /** `옮기기` (STO-08): destination picker, then the `SceneMoveSheet` preview. */
    MOVE,

    /** `씬 지우기` (STO-10, SCR-52): no question, toast with `되돌리기`. */
    DELETE_SCENE,

    /** → `NewUnitSheet` before the scene (SCR-53). */
    NEW_SCENE_BEFORE,

    /** Episode ⋯ → `번호 다시 매기기 · 1-7` preview sheet (SCR-22, [RenumberSheet]). */
    RENUMBER,

    /** (0.2.0, N-29) Episode ⋯ `판 남기기` → `EpisodeVersionsSheet(keepOnly)` (SCR-76). */
    KEEP_VERSION,

    /** (0.2.0, N-29) Episode ⋯ `지난 판` → `EpisodeVersionsSheet` (SCR-76). */
    VERSIONS,

    /** A new unit of the same level after this one (`새 에피소드` / `새 장`, STO-05). */
    NEW_UNIT_AFTER,

    /** `완료` on / off (STO-06). */
    MARK_DONE,
    UNMARK_DONE,

    /** Structure ⋯: → `BulkPlaceTime` (STO-31). */
    BULK,

    /** Structure ⋯: → `StructureLevels` (STO-02, editor owned by LIB-69). */
    LEVEL_NAMES,

    /** Structure ⋯: a new top-level unit at the end (`새 장`). */
    NEW_TOP_UNIT,
}

/** One ⋯ row: label, value line (`결사의 문 · 2일째 아침 06:10`, `휴지통 · 30일`), usable or not. */
data class MenuRow(val action: MenuAction, val label: String, val value: String?, val enabled: Boolean = true)

/** A ⋯ sheet: title (`씬 4 · 1-7`) and rows. */
data class MenuModel(val title: String, val rows: List<MenuRow>) {
    /**
     * Without the rows whose label is not drawn yet (`TODO_COPY_…`, 20-story-time §6 Q34): they come
     * back by themselves once the copy constant gets its words.
     */
    fun drawnOnly(): MenuModel = copy(rows = rows.filter { isDrawnCopy(it.label) })
}

/** Where a unit ⋯ was opened: the 스토리 list (STO-07) or a scene head in the script (SCR-47). */
enum class MenuSurface { STORY_LIST, SCENE_HEAD }

/**
 * The ⋯ menus of STO-07 as data. Read-only projects (the example, LIB-17) get every mutating row
 * disabled with the value `읽기 전용`.
 */
object UnitMenuModel {
    private val headActions = mapOf(
        MenuCopy.PLACE_TIME to MenuAction.PLACE_TIME,
        MenuCopy.TRANSITION to MenuAction.TRANSITION,
        MenuCopy.MERGE_PREVIOUS to MenuAction.MERGE_PREVIOUS,
        MenuCopy.DELETE_SCENE to MenuAction.DELETE_SCENE,
        MenuCopy.NEW_SCENE_BEFORE to MenuAction.NEW_SCENE_BEFORE,
    )

    /** The ⋯ of [unitKey] (scene, episode or a unit above), or null for a unit that is not live. */
    fun build(doc: ProjectDoc, unitKey: Key, surface: MenuSurface, readOnly: Boolean): MenuModel? {
        val unit = doc.index.unit(unitKey)?.takeIf { doc.liveness.isUnitLive(it.key) } ?: return null
        val structure = doc.project.structure
        return when (unit.level) {
            structure.sceneLevel -> sceneMenu(doc, unit, surface, readOnly)
            else -> unitMenu(doc, unit, readOnly)
        }
    }

    /**
     * Scene ⋯. On a scene head it is `domain:story` `SceneHeads.menu` as is (SCR-47). In the 스토리
     * list (STO-07): `장소와 때 · 전환 문구 · 앞 씬과 합치기` (disabled for an episode's first scene) `·
     * 여기서 나누기 · 옮기기`, `완료 표시` (`완료 표시 빼기` when set), then (0.3.2) `씬 지우기` last.
     */
    private fun sceneMenu(doc: ProjectDoc, scene: StoryUnit, surface: MenuSurface, readOnly: Boolean): MenuModel? {
        val head = SceneHeads(doc).menu(scene.key, readOnly) ?: return null
        val mapped = head.rows.mapNotNull { row -> headActions[row.label]?.let { MenuRow(it, row.label, row.value, row.enabled) } }
        if (surface == MenuSurface.SCENE_HEAD) return MenuModel(head.title, mapped).drawnOnly()
        fun from(action: MenuAction) = mapped.firstOrNull { it.action == action }
        val rows = buildList {
            from(MenuAction.PLACE_TIME)?.let(::add)
            from(MenuAction.TRANSITION)?.let(::add)
            add(from(MenuAction.MERGE_PREVIOUS) ?: MenuRow(MenuAction.MERGE_PREVIOUS, MenuCopy.MERGE_PREVIOUS, null, enabled = false))
            add(mutating(MenuAction.SPLIT_HERE, MenuCopy.SPLIT_HERE, null, readOnly))
            add(mutating(MenuAction.MOVE, MenuCopy.MOVE, null, readOnly).let { if (MoveModel.movable(doc, scene.key)) it else it.copy(enabled = false) })
            add(doneRow(doc, scene, readOnly))
            // (0.3.2) The destructive row last, under its own divider (`MenuRowView`).
            from(MenuAction.DELETE_SCENE)?.let(::add)
        }
        return MenuModel(head.title, rows).drawnOnly()
    }

    /**
     * Episode ⋯: `번호 다시 매기기 · 1-7` (the drawn item, STO-07 → SCR-22), (0.2.0) `판 남기기 · 지난 판` (N-29), `옮기기` (STO-08), then
     * `새 에피소드` and `완료 표시` / `완료 표시 빼기`. Units above the episode level: `옮기기` and `새 장`.
     */
    private fun unitMenu(doc: ProjectDoc, unit: StoryUnit, readOnly: Boolean): MenuModel {
        val view = StoryOverview(doc)
        val isEpisode = unit.level == doc.project.structure.episodeLevel
        val title = if (isEpisode) "${view.codes.episodeCode(unit)} · ${unit.title ?: StoryCopy.UNTITLED}" else view.codes.chapterHeader(unit)
        val newLabel = if (isEpisode) StoryCopy.NEW_EPISODE else StoryCopy.NEW_CHAPTER
        val rows = buildList {
            if (isEpisode) add(mutating(MenuAction.RENUMBER, "${MenuCopy.RENUMBER} · ${view.codes.episodeCode(unit)}", null, readOnly))
            // (0.2.0, N-29) versions per episode; `지난 판` stays readable in a read-only project.
            if (isEpisode) add(mutating(MenuAction.KEEP_VERSION, MenuCopy.KEEP_VERSION, null, readOnly))
            if (isEpisode) add(MenuRow(MenuAction.VERSIONS, MenuCopy.VERSIONS, null))
            // `옮기기` (STO-08): the explicit path for reordering episodes and chapters (no drag needed).
            add(mutating(MenuAction.MOVE, MenuCopy.MOVE, null, readOnly))
            add(mutating(MenuAction.NEW_UNIT_AFTER, newLabel, null, readOnly))
            if (isEpisode) add(doneRow(doc, unit, readOnly))
        }
        return MenuModel(title, rows).drawnOnly()
    }

    /** `완료` toggle; the value line is the current status word (`쓰는 중`, `완료`, `미정`). */
    private fun doneRow(doc: ProjectDoc, unit: StoryUnit, readOnly: Boolean): MenuRow {
        val view = StoryOverview(doc)
        val isScene = unit.level == doc.project.structure.sceneLevel
        val status = if (isScene) view.status(unit).word else view.episodeStatus(unit)
        return if (unit.doneMarked) {
            mutating(MenuAction.UNMARK_DONE, StoryCopy.UNMARK_DONE, status, readOnly)
        } else {
            mutating(MenuAction.MARK_DONE, StoryCopy.MARK_DONE, status, readOnly)
        }
    }

    internal fun mutating(action: MenuAction, label: String, value: String?, readOnly: Boolean): MenuRow =
        if (readOnly) MenuRow(action, label, StoryCopy.READ_ONLY, enabled = false) else MenuRow(action, label, value)
}

/**
 * The structure ⋯ on the 스토리 head (STO-07): `장소 · 때 한꺼번에`, `단계 이름 | 장 › 에피소드 › 씬`
 * (board `ProjSettings` row), and `새 장` at the end (`새 에피소드` at 2 levels, absent at 1 level).
 */
object StructureMenuModel {
    fun build(doc: ProjectDoc, readOnly: Boolean): MenuModel {
        val levels = doc.project.structure.levels
        val rows = buildList {
            add(MenuRow(MenuAction.BULK, StructureCopy.BULK, null))
            add(MenuRow(MenuAction.LEVEL_NAMES, StructureCopy.LEVEL_NAMES, levels.joinToString(" › ")))
            when {
                levels.size >= 3 -> add(UnitMenuModel.mutating(MenuAction.NEW_TOP_UNIT, StoryCopy.NEW_CHAPTER, null, readOnly))
                levels.size == 2 -> add(UnitMenuModel.mutating(MenuAction.NEW_TOP_UNIT, StoryCopy.NEW_EPISODE, null, readOnly))
            }
        }
        return MenuModel(StructureCopy.TITLE, rows).drawnOnly()
    }
}
