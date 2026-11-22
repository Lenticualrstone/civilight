package io.github.lenticualrstone.civilight.feature.story.check

import io.github.lenticualrstone.civilight.domain.checks.CheckResult
import io.github.lenticualrstone.civilight.domain.story.checks.CheckFilter
import io.github.lenticualrstone.civilight.domain.story.checks.Marker
import io.github.lenticualrstone.civilight.domain.story.travel.TravelVerdict
import io.github.lenticualrstone.civilight.domain.story.travel.TravelWarning
import io.github.lenticualrstone.civilight.model.checks.CheckType

/** How one 점검 센터 row is drawn (CNV-17 row anatomy and its `TravelCheck` card variant). */
sealed interface TravelLook {
    /** Not a travel row: the generic item row. */
    data object Plain : TravelLook

    /**
     * The `tint` card (`TravelCheck` first row): `▲ 이동 시간 부족 | 하르트`, the route line, the mini map,
     * the `필요` / `있음` bars, the detail and the fix chips.
     */
    data class Card(val warning: TravelWarning, val detail: String) : TravelLook

    /**
     * The compact list form: marker glyph, bold `<kind> · <person>` and one small line
     * (`같은 때 두 장소 · 세라 | 1-7 씬 2 기록실 · 씬 4 잿더미 시장 · 둘 다 2일째 아침`,
     * `대략인데도 모자람 · 오웬 | … · 가장 길게 잡아도 부족`, `장소 없음 · 씬 3개 | … · 동선 · 점검에서 빠짐`).
     * [trailingAction] = the one button drawn at the right (`한꺼번에 ›`); the others sit under the line.
     */
    data class Compact(val marker: Marker, val title: String, val line: String, val trailingAction: Boolean) : TravelLook
}

/** Picks the card or the compact form for a row (pure, tested in `TravelLookTest`). */
object TravelLooks {
    /**
     * A non-range `이동 시간 부족` is the card in the open filters; under `맞다고 표시한 것` every travel row
     * is compact with its `되돌리기`. `같은 때 두 장소` / `동시에 두 장소` and a range-driven shortage are
     * compact rows with their chips; `◇ 장소 없음` is compact with `한꺼번에 ›` at the right.
     */
    fun of(result: CheckResult, filter: CheckFilter): TravelLook {
        val warning = result.travel
        if (warning != null) {
            val title = when {
                warning.verdict == TravelVerdict.SHORTAGE && warning.rangeDriven -> warning.compactLine.split(SEP).take(2).joinToString(SEP)
                else -> "${warning.verdict.title}$SEP${warning.characterName}"
            }
            val line = when {
                warning.verdict == TravelVerdict.SHORTAGE && warning.rangeDriven -> warning.compactLine.split(SEP).drop(2).joinToString(SEP)
                warning.verdict == TravelVerdict.SHORTAGE -> warning.routeLine
                else -> warning.compactLine.substringAfter(" | ")
            }
            val card = warning.verdict == TravelVerdict.SHORTAGE && !warning.rangeDriven && filter != CheckFilter.ACKNOWLEDGED
            return if (card) TravelLook.Card(warning, result.detail ?: warning.detail) else TravelLook.Compact(result.marker, title, line, trailingAction = false)
        }
        if (result.checkType == CheckType.TRAVEL_NO_PLACE) {
            return TravelLook.Compact(
                marker = result.marker,
                title = result.title.removePrefix("${result.marker.glyph} "),
                line = result.body.orEmpty(),
                trailingAction = filter != CheckFilter.ACKNOWLEDGED,
            )
        }
        return TravelLook.Plain
    }

    private const val SEP = " · "
}
