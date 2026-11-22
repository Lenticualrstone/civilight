package io.github.lenticualrstone.civilight.feature.story.settings

import io.github.lenticualrstone.civilight.domain.story.structure.UnitSettingsEdit
import io.github.lenticualrstone.civilight.domain.story.time.BandTable
import io.github.lenticualrstone.civilight.domain.story.time.word
import io.github.lenticualrstone.civilight.model.ClockTime
import io.github.lenticualrstone.civilight.model.doc.ProjectDoc
import io.github.lenticualrstone.civilight.model.project.Band
import io.github.lenticualrstone.civilight.model.project.DayBoundary

/** One band row: `저녁` · `17–20` · start `17:00` (editable except 새벽, the 04:00 day start). */
data class BandRow(val band: Band, val word: String, val hours: String, val start: ClockTime, val editable: Boolean)

/** 프로젝트 설정 › 하루 · 시간대 (STO-19). */
data class DayBandUi(
    val projectName: String,
    val boundary: DayBoundary,
    val bands: List<BandRow>,
    /** `하루는 새벽 4시부터 · 저녁 17–20` (the settings row value). */
    val summary: String,
    val readOnly: Boolean,
)

object DayBandModel {
    fun build(doc: ProjectDoc, readOnly: Boolean): DayBandUi {
        val settings = doc.project.sceneTime
        val table = BandTable(settings.bandStarts)
        val boundary = settings.dayBoundary.knownOr(DayBoundary.DAWN_0400)
        return DayBandUi(
            projectName = doc.project.name,
            boundary = boundary,
            bands = Band.entries.map { b ->
                BandRow(b, b.word, table.hoursLabel(b), table.start(b), editable = !readOnly && b in UnitSettingsEdit.movableBands)
            },
            summary = summary(doc),
            readOnly = readOnly,
        )
    }

    /** `하루는 새벽 4시부터 · 저녁 17–20`; under `자정` the first part reads `자정`. */
    fun summary(doc: ProjectDoc): String {
        val settings = doc.project.sceneTime
        val day = if (settings.dayBoundary.knownOr(DayBoundary.DAWN_0400) == DayBoundary.MIDNIGHT) DayBandCopy.MIDNIGHT else DayBandCopy.DAWN_DAY
        val evening = "${Band.EVENING.word} ${BandTable(settings.bandStarts).hoursLabel(Band.EVENING)}"
        return "$day · $evening"
    }

    /** A complete, valid start for [band] (a draft that is not yet one changes nothing). */
    fun accepts(doc: ProjectDoc, band: Band, start: ClockTime): Boolean =
        UnitSettingsEdit.canSetBandStart(doc.project.sceneTime.bandStarts, band, start)
}
