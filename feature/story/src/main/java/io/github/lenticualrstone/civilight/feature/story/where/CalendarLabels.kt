package io.github.lenticualrstone.civilight.feature.story.where

import io.github.lenticualrstone.civilight.domain.story.time.SceneTimes
import io.github.lenticualrstone.civilight.domain.story.time.TimeLabels
import io.github.lenticualrstone.civilight.model.Key
import io.github.lenticualrstone.civilight.model.doc.ProjectDoc

/**
 * 세계 달력으로 보는 때 (STO-18) in this lane's surfaces. With the world calendar on, the sheet's result
 * line shows the calendar date in place of `N일째` (`1-7 · 서리달 26일 · 새벽`; `대략` gives a date range)
 * and keeps `N일째` as the secondary label (`2일째 · 이야기 약 13일째 · 연표에서`). Stored 때 never
 * changes with the calendar (labels only). `domain:story` [TimeLabels.calendar] gives the date (Q14 `ED`).
 */
object CalendarLabels {
    /**
     * [resultLine] (`<full> | <story> · 연표에서`, `SceneWhereSheet.resultLine`) with the calendar date,
     * or [resultLine] itself when the calendar is off, the scene has no date or is a 회상.
     */
    fun resultLine(doc: ProjectDoc, sceneKey: Key, resultLine: String?): String? {
        resultLine ?: return null
        val labels = TimeLabels(SceneTimes(doc))
        val date = labels.calendar(sceneKey) ?: return resultLine
        val day = labels.dayPart(sceneKey) ?: return resultLine
        val parts = resultLine.split(" | ", limit = 2)
        val main = parts[0]
        if (!main.contains(day)) return resultLine
        val sub = parts.getOrNull(1)?.let { "$day · $it" } ?: day
        return "${main.replaceFirst(day, date)} | $sub"
    }
}
