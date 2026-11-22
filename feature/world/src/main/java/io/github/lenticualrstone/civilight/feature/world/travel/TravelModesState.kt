package io.github.lenticualrstone.civilight.feature.world.travel

import io.github.lenticualrstone.civilight.domain.world.common.EditContext
import io.github.lenticualrstone.civilight.domain.world.map.Travel
import io.github.lenticualrstone.civilight.domain.world.map.TravelModeEdit
import io.github.lenticualrstone.civilight.domain.world.map.TravelModeEdits
import io.github.lenticualrstone.civilight.domain.world.map.TravelModeRefusal
import io.github.lenticualrstone.civilight.feature.world.WorldCopy
import io.github.lenticualrstone.civilight.model.doc.ProjectDoc
import kotlinx.serialization.Serializable

/** One mode row: `도보 15 km · 8시간` (WLD-47, board `ProjSettings` value pattern). */
data class TravelModeRowUi(val index: Int, val name: String, val line: String)

/** `프로젝트 설정 › 이동 수단` (WLD-47): the modes in order; the defaults until one is changed. */
data class TravelModesUi(val rows: List<TravelModeRowUi>, val readOnly: Boolean)

/** The mode form, saveable: `이름`, daily distance in km, daily moving hours (as typed). */
@Serializable
data class TravelModeDraft(val name: String = "", val km: String = "", val hours: String = "")

/** What `저장` did. */
sealed interface TravelModeSave {
    data class Saved(val doc: ProjectDoc) : TravelModeSave
    data class Refused(val line: String) : TravelModeSave
}

/**
 * The editor of travel modes (WLD-47 `[PROPOSED editor]`): each mode has a name, a daily distance
 * (km) and daily moving hours; `직접` adds one. Writes go through `domain:world`
 * [TravelModeEdits] (the defaults 도보 15/8 · 말 40/8 · 배 30/24 are stored on the first change,
 * so keys that maps and scenes keep stay valid).
 */
object TravelModesState {
    fun build(doc: ProjectDoc, readOnly: Boolean): TravelModesUi {
        val rows = Travel.modes(doc).mapIndexed { i, mode ->
            TravelModeRowUi(i, mode.name, Travel.modeLabel(mode).removePrefix(mode.name).trim())
        }
        return TravelModesUi(rows, readOnly)
    }

    /** The form of an existing mode (by its row index). */
    fun draftOf(doc: ProjectDoc, index: Int): TravelModeDraft {
        val mode = Travel.modes(doc).getOrNull(index) ?: return TravelModeDraft()
        return TravelModeDraft(mode.name, number(mode.kmPerDay), number(mode.movingHoursPerDay))
    }

    private fun number(value: Double): String =
        if (value == Math.floor(value)) value.toLong().toString() else value.toString()

    /** `저장`: [index] null = a new mode (`직접`), else the mode at that row. */
    fun save(doc: ProjectDoc, index: Int?, draft: TravelModeDraft, ctx: EditContext): TravelModeSave {
        val km = TravelModeEdits.parseNumber(draft.km) ?: return TravelModeSave.Refused(line(TravelModeRefusal.BAD_DISTANCE))
        val hours = TravelModeEdits.parseNumber(draft.hours) ?: return TravelModeSave.Refused(line(TravelModeRefusal.BAD_HOURS))
        val edit = if (index == null) {
            TravelModeEdits.add(doc, draft.name, km, hours, ctx)
        } else {
            val stored = TravelModeEdits.ensureStored(doc, ctx)
            val mode = TravelModeEdits.storedAt(stored, index) ?: return TravelModeSave.Refused(line(TravelModeRefusal.NO_MODE))
            TravelModeEdits.update(stored, mode.key, draft.name, km, hours, ctx)
        }
        return when (edit) {
            is TravelModeEdit.Applied -> TravelModeSave.Saved(edit.doc)
            is TravelModeEdit.Refused -> TravelModeSave.Refused(line(edit.reason))
        }
    }

    fun line(reason: TravelModeRefusal): String = when (reason) {
        TravelModeRefusal.EMPTY_NAME -> WorldCopy.TODO_COPY_WORLD_TRAVEL_NAME_EMPTY
        TravelModeRefusal.BAD_DISTANCE -> WorldCopy.TODO_COPY_WORLD_TRAVEL_DISTANCE_BAD
        TravelModeRefusal.BAD_HOURS -> WorldCopy.TODO_COPY_WORLD_TRAVEL_HOURS_BAD
        TravelModeRefusal.DUPLICATE_NAME -> WorldCopy.TODO_COPY_WORLD_TRAVEL_NAME_TAKEN
        TravelModeRefusal.NO_MODE -> WorldCopy.TODO_COPY_WORLD_TRAVEL_NAME_EMPTY
    }
}
