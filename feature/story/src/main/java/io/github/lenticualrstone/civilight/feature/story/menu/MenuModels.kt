package io.github.lenticualrstone.civilight.feature.story.menu

import io.github.lenticualrstone.civilight.domain.story.scene.SceneHeads
import io.github.lenticualrstone.civilight.domain.story.structure.StoryOverview
import io.github.lenticualrstone.civilight.feature.story.common.StoryCopy
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

    /** `씬 지우기` (STO-10, SCR-52): no question, toast with `되돌리기`. */
    DELETE_SCENE,

    /** → `NewUnitSheet` before the scene (SCR-53). */
    NEW_SCENE_BEFORE,

    /** Episode ⋯ → `번호 다시 매기기 · 1-7` preview sheet (SCR-22, [RenumberSheet]). */
    RENUMBER,

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
data class MenuModel(val title: String, val rows: List<MenuRow>)

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
     * 여기서 나누기 · 씬 지우기`, then `완료` (copy not drawn).
     */
    private fun sceneMenu(doc: ProjectDoc, scene: StoryUnit, surface: MenuSurface, readOnly: Boolean): MenuModel? {
        val head = SceneHeads(doc).menu(scene.key, readOnly) ?: return null
        val mapped = head.rows.mapNotNull { row -> headActions[row.label]?.let { MenuRow(it, row.label, row.value, row.enabled) } }
        if (surface == MenuSurface.SCENE_HEAD) return MenuModel(head.title, mapped)
        fun from(action: MenuAction) = mapped.firstOrNull { it.action == action }
        val rows = buildList {
            from(MenuAction.PLACE_TIME)?.let(::add)
            from(MenuAction.TRANSITION)?.let(::add)
            add(from(MenuAction.MERGE_PREVIOUS) ?: MenuRow(MenuAction.MERGE_PREVIOUS, MenuCopy.MERGE_PREVIOUS, null, enabled = false))
            add(mutating(MenuAction.SPLIT_HERE, MenuCopy.SPLIT_HERE, null, readOnly))
            from(MenuAction.DELETE_SCENE)?.let(::add)
            add(doneRow(doc, scene, readOnly))
        }
        return MenuModel(head.title, rows)
    }

    /**
     * Episode ⋯: `번호 다시 매기기 · 1-7` (the drawn item, STO-07 → SCR-22), then `새 에피소드` and
     * `완료` (copy not drawn, Q34). The ⋯ of units above the episode level: `새 장` (copy not drawn).
     */
    private fun unitMenu(doc: ProjectDoc, unit: StoryUnit, readOnly: Boolean): MenuModel {
        val view = StoryOverview(doc)
        val isEpisode = unit.level == doc.project.structure.episodeLevel
        val title = if (isEpisode) "${view.codes.episodeCode(unit)} · ${unit.title ?: StoryCopy.UNTITLED}" else view.codes.chapterHeader(unit)
        val newLabel = if (isEpisode) StoryCopy.TODO_COPY_STO_NEW_EPISODE else StoryCopy.TODO_COPY_STO_NEW_CHAPTER
        val rows = buildList {
            if (isEpisode) add(mutating(MenuAction.RENUMBER, "${MenuCopy.RENUMBER} · ${view.codes.episodeCode(unit)}", null, readOnly))
            add(mutating(MenuAction.NEW_UNIT_AFTER, newLabel, null, readOnly))
            if (isEpisode) add(doneRow(doc, unit, readOnly))
        }
        return MenuModel(title, rows)
    }

    /** `완료` toggle; the value line is the current status word (`쓰는 중`, `완료`, `미정`). */
    private fun doneRow(doc: ProjectDoc, unit: StoryUnit, readOnly: Boolean): MenuRow {
        val view = StoryOverview(doc)
        val isScene = unit.level == doc.project.structure.sceneLevel
        val status = if (isScene) view.status(unit).word else view.episodeStatus(unit)
        return if (unit.doneMarked) {
            mutating(MenuAction.UNMARK_DONE, StoryCopy.TODO_COPY_STO_UNMARK_DONE, status, readOnly)
        } else {
            mutating(MenuAction.MARK_DONE, StoryCopy.TODO_COPY_STO_MARK_DONE, status, readOnly)
        }
    }

    internal fun mutating(action: MenuAction, label: String, value: String?, readOnly: Boolean): MenuRow =
        if (readOnly) MenuRow(action, label, StoryCopy.READ_ONLY, enabled = false) else MenuRow(action, label, value)
}

/**
 * The structure ⋯ on the 스토리 head (STO-07): `장소 · 때 한꺼번에`, `단계 이름 | 장 › 에피소드 › 씬`
 * (board `ProjSettings` row), and `새 장` at the end (copy not drawn; `새 에피소드` at 2 levels,
 * absent at 1 level).
 */
object StructureMenuModel {
    fun build(doc: ProjectDoc, readOnly: Boolean): MenuModel {
        val levels = doc.project.structure.levels
        val rows = buildList {
            add(MenuRow(MenuAction.BULK, StructureCopy.BULK, null))
            add(MenuRow(MenuAction.LEVEL_NAMES, StructureCopy.LEVEL_NAMES, levels.joinToString(" › ")))
            when {
                levels.size >= 3 -> add(UnitMenuModel.mutating(MenuAction.NEW_TOP_UNIT, StoryCopy.TODO_COPY_STO_NEW_CHAPTER, null, readOnly))
                levels.size == 2 -> add(UnitMenuModel.mutating(MenuAction.NEW_TOP_UNIT, StoryCopy.TODO_COPY_STO_NEW_EPISODE, null, readOnly))
            }
        }
        return MenuModel(StructureCopy.TITLE, rows)
    }
}
