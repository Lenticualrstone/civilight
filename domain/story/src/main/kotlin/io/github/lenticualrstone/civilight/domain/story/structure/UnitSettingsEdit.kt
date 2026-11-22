package io.github.lenticualrstone.civilight.domain.story.structure

import io.github.lenticualrstone.civilight.domain.story.scene.EditContext
import io.github.lenticualrstone.civilight.domain.story.time.minutesAfterDayStart
import io.github.lenticualrstone.civilight.model.ClockTime
import io.github.lenticualrstone.civilight.model.Key
import io.github.lenticualrstone.civilight.model.doc.ProjectDoc
import io.github.lenticualrstone.civilight.model.open
import io.github.lenticualrstone.civilight.model.project.Band
import io.github.lenticualrstone.civilight.model.project.BandStarts
import io.github.lenticualrstone.civilight.model.project.DayBoundary
import io.github.lenticualrstone.civilight.model.project.SceneNotation

/** Small project and unit settings owned by the story area (STO-06 `완료`, STO-17 `때 표기`). */
object UnitSettingsEdit {
    /**
     * The author-set `완료` flag of a live unit (STO-06, Q3). Labels and statuses are derived from it;
     * nothing else changes. Unknown or trashed units leave the doc unchanged.
     */
    fun setDone(doc: ProjectDoc, unitKey: Key, done: Boolean, ctx: EditContext): ProjectDoc {
        val unit = doc.index.unit(unitKey) ?: return doc
        if (!doc.liveness.isUnitLive(unitKey) || unit.doneMarked == done) return doc
        val changed = unit.copy(doneMarked = done, updatedAt = ctx.now, updatedBy = ctx.device)
        return doc.copy(story = doc.story.copy(units = doc.story.units.map { if (it.key == unitKey) changed else it }))
    }

    /**
     * `때 표기` (STO-17): `에피소드 기준` or `이야기 시작부터`. Only labels change; stored 때 stays
     * byte-identical (R11 §6).
     */
    fun setNotation(doc: ProjectDoc, notation: SceneNotation, ctx: EditContext): ProjectDoc {
        val settings = doc.project.sceneTime
        if (settings.notation.known == notation) return doc
        val project = doc.project.copy(
            sceneTime = settings.copy(notation = notation.open()),
            updatedAt = ctx.now,
            updatedBy = ctx.device,
        )
        return doc.copy(project = project)
    }

    /**
     * `하루는 새벽 4시부터` · `자정` (STO-19). Only labels change: stored clocks keep the 04:00-day form,
     * so every stored 때 stays byte-identical.
     */
    fun setDayBoundary(doc: ProjectDoc, boundary: DayBoundary, ctx: EditContext): ProjectDoc {
        val settings = doc.project.sceneTime
        if (settings.dayBoundary.known == boundary) return doc
        return doc.copy(project = doc.project.copy(sceneTime = settings.copy(dayBoundary = boundary.open()), updatedAt = ctx.now, updatedBy = ctx.device))
    }

    /** Bands whose start may move (STO-19): every boundary but the 새벽 start, which is the 04:00 day start. */
    val movableBands: List<Band> = listOf(Band.MORNING, Band.DAY, Band.EVENING, Band.NIGHT)

    /**
     * Moves the start of [band] (STO-19): bands stay contiguous and cover 24 h, so the previous band
     * now ends at [start]. Refused (doc unchanged) for 새벽, whose start is the stored day start
     * (`ED` Q15: clocks and band ranges are measured from 04:00), and when [start] is not strictly
     * between the previous and the next band's start in the 04:00 day.
     */
    fun setBandStart(doc: ProjectDoc, band: Band, start: ClockTime, ctx: EditContext): ProjectDoc {
        if (!canSetBandStart(doc.project.sceneTime.bandStarts, band, start)) return doc
        val starts = doc.project.sceneTime.bandStarts
        if (starts.startOf(band) == start) return doc
        val changed = when (band) {
            Band.DAWN -> return doc
            Band.MORNING -> starts.copy(morning = start)
            Band.DAY -> starts.copy(day = start)
            Band.EVENING -> starts.copy(evening = start)
            Band.NIGHT -> starts.copy(night = start)
        }
        val settings = doc.project.sceneTime.copy(bandStarts = changed)
        return doc.copy(project = doc.project.copy(sceneTime = settings, updatedAt = ctx.now, updatedBy = ctx.device))
    }

    /** Whether [start] is a valid new start of [band]: strictly between its neighbours' starts. */
    fun canSetBandStart(starts: BandStarts, band: Band, start: ClockTime): Boolean {
        if (band == Band.DAWN) return false
        val order = Band.entries
        val i = order.indexOf(band)
        val previous = starts.startOf(order[i - 1]).minutesAfterDayStart()
        val next = if (i + 1 < order.size) starts.startOf(order[i + 1]).minutesAfterDayStart() else MINUTES_PER_DAY
        val at = start.minutesAfterDayStart()
        return at in (previous + 1) until next
    }

    /**
     * Hides or shows a live unit (`씬도 숨기기`, R19 §6, from 점검 센터 row 19). The hidden-item lock
     * and masking follow the unit's `hidden` flag; nothing else changes.
     */
    fun setHidden(doc: ProjectDoc, unitKey: Key, hidden: Boolean, ctx: EditContext): ProjectDoc {
        val unit = doc.index.unit(unitKey) ?: return doc
        if (!doc.liveness.isUnitLive(unitKey) || unit.hidden == hidden) return doc
        val changed = unit.copy(hidden = hidden, updatedAt = ctx.now, updatedBy = ctx.device)
        return doc.copy(story = doc.story.copy(units = doc.story.units.map { if (it.key == unitKey) changed else it }))
    }

    private const val MINUTES_PER_DAY = 24 * 60
}
