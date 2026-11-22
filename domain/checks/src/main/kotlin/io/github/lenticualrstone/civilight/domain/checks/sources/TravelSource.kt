package io.github.lenticualrstone.civilight.domain.checks.sources

import io.github.lenticualrstone.civilight.domain.checks.CheckCopy
import io.github.lenticualrstone.civilight.domain.checks.CheckRegistry
import io.github.lenticualrstone.civilight.domain.checks.CheckResult
import io.github.lenticualrstone.civilight.domain.checks.FixAction
import io.github.lenticualrstone.civilight.domain.checks.FixKind
import io.github.lenticualrstone.civilight.domain.story.checks.Fingerprints
import io.github.lenticualrstone.civilight.domain.story.travel.TravelVerdict
import io.github.lenticualrstone.civilight.domain.story.travel.TravelWarning
import io.github.lenticualrstone.civilight.model.checks.CheckType

/** Rows 10–14 (`이동`) from the story area's travel check (STO-29/30, R18). */
internal object TravelSource {
    /** Rows 10–12 of one verdict. */
    fun warnings(scope: CheckScope, checkType: String): List<CheckResult> = scope.travelWarnings.filter { it.checkType == checkType }

    /**
     * `▲ 이동 시간 부족` (card: route line, bars, detail) or the compact rows `▲ 같은 때 두 장소 · 세라 |
     * 1-7 씬 2 기록실 · 씬 4 잿더미 시장 · 둘 다 2일째 아침` (STO-30), with the minimal-fix chips, then
     * `맞음`. A range-driven shortage carries `대략인데도 모자람 · … · 가장 길게 잡아도 부족` as its detail.
     */
    fun warning(scope: CheckScope, w: TravelWarning): CheckResult {
        val chips = listOf(FixAction(w.timeChip.label, FixKind.DELAY_SCENE, w.timeChip.sceneKey, days = w.timeChip.days)) +
            w.modeChips.map { FixAction(it.label, FixKind.USE_MODE, w.characterKey, modeKey = it.modeKey) }
        val shortage = w.verdict == TravelVerdict.SHORTAGE
        val body = if (shortage) w.routeLine else w.compactLine.substringAfter(" | ")
        val detail = when {
            shortage && w.rangeDriven -> w.compactLine
            w.straightNote != null -> CheckCopy.join(w.detail, w.straightNote)
            else -> w.detail
        }
        return CheckResult(
            checkType = w.checkType,
            subjectKeys = w.subjectKeys,
            fingerprint = w.fingerprint,
            title = w.headline,
            tag = w.characterName,
            location = scope.sceneRef(w.fromSceneKey),
            body = body,
            detail = detail,
            actions = chips + FixAction(CheckRegistry.ackLabel(w.checkType)!!, FixKind.ACKNOWLEDGE, w.characterKey),
            storyOrder = scope.sceneOrder(w.toSceneKey),
            lockedHidden = scope.hiding.character(w.characterKey) || scope.hiding.scene(w.fromSceneKey) ||
                scope.hiding.scene(w.toSceneKey) || placeHidden(scope, w),
            travel = w,
        )
    }

    private fun placeHidden(scope: CheckScope, w: TravelWarning): Boolean =
        scope.hiding.place(scope.doc.index.unit(w.fromSceneKey)?.place) || scope.hiding.place(scope.doc.index.unit(w.toSceneKey)?.place)

    /**
     * `◇ 축척 없음 · 점검 못 함` once per scene pair (`ED`: the engine gives one per character on the
     * pair; the names go in [CheckResult.tag]).
     */
    fun noScale(scope: CheckScope): List<CheckResult> =
        scope.travel.noScale.groupBy { it.fromSceneKey to it.toSceneKey }.map { (pair, group) ->
            val (from, to) = pair
            val subjects = listOf(from, to)
            val names = group.mapNotNull { scope.doc.index.character(it.characterKey)?.name }.distinct()
            CheckResult(
                checkType = CheckType.TRAVEL_NO_SCALE,
                subjectKeys = subjects,
                fingerprint = Fingerprints.of(CheckType.TRAVEL_NO_SCALE, subjects, group.first().mapKey),
                title = CheckCopy.NO_SCALE,
                tag = names.joinToString(CheckCopy.SEP).ifEmpty { null },
                location = scope.sceneRef(from),
                body = group.first().detail,
                actions = listOf(FixAction(CheckCopy.FIX_SCALE, FixKind.FIX_SCALE, group.first().mapKey)),
                storyOrder = scope.sceneOrder(to),
                lockedHidden = group.all { scope.hiding.character(it.characterKey) } || scope.hiding.scene(from) || scope.hiding.scene(to),
                cannotCheck = true,
            )
        }

    /** `◇ 장소 없음 · 씬 n개` (one row for every timed scene without a place). */
    fun noPlace(scope: CheckScope): List<CheckResult> {
        val item = scope.travel.noPlace ?: return emptyList()
        return listOf(
            CheckResult(
                checkType = CheckType.TRAVEL_NO_PLACE,
                subjectKeys = item.sceneKeys,
                fingerprint = Fingerprints.of(CheckType.TRAVEL_NO_PLACE, item.sceneKeys),
                title = item.title,
                body = item.detail,
                actions = listOf(FixAction(item.action, FixKind.OPEN_BULK, item.sceneKeys.first())),
                storyOrder = scope.sceneOrder(item.sceneKeys.first()),
                lockedHidden = item.sceneKeys.all(scope.hiding::scene),
            ),
        )
    }
}
