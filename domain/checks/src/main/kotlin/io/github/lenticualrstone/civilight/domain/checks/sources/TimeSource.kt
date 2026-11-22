package io.github.lenticualrstone.civilight.domain.checks.sources

import io.github.lenticualrstone.civilight.domain.checks.CheckCopy
import io.github.lenticualrstone.civilight.domain.checks.CheckResult
import io.github.lenticualrstone.civilight.domain.checks.FixAction
import io.github.lenticualrstone.civilight.domain.checks.FixKind
import io.github.lenticualrstone.civilight.domain.story.time.OrderChecks

/**
 * Rows 15–18 from the story time engine: `◇ 시각과 시간대가 다름`, `◇ 순서와 때가 다름` and the two
 * transition rows. The transition rows use `domain:story`'s `TransitionChecks` (STO-40 wins over
 * SCR-62's own filter `씬`, 00-domain-model C-24).
 */
internal object TimeSource {
    /** `<scene> · 새벽 06:20` with `맞음` (`ED` row copy). */
    fun clockBand(scope: CheckScope): List<CheckResult> = OrderChecks.clockBandNotices(scope.times, scope.codes).map { n ->
        CheckResult(
            checkType = n.checkType,
            subjectKeys = n.subjectKeys,
            fingerprint = n.fingerprint,
            title = CheckCopy.CLOCK_BAND,
            location = scope.sceneRef(n.sceneKey),
            body = n.line,
            actions = listOf(FixAction(CheckCopy.MATCH, FixKind.ACKNOWLEDGE, n.sceneKey)),
            storyOrder = scope.sceneOrder(n.sceneKey),
            lockedHidden = scope.hiding.scene(n.sceneKey),
        )
    }

    /** `<scene> · 직전 씬보다 앞 · 약 9시간 전` (`ED` row copy), never ▲. */
    fun order(scope: CheckScope): List<CheckResult> = OrderChecks.notices(scope.times).map { n ->
        val ref = scope.sceneRef(n.sceneKey)
        CheckResult(
            checkType = n.checkType,
            subjectKeys = n.subjectKeys,
            fingerprint = n.fingerprint,
            title = CheckCopy.ORDER,
            location = ref,
            body = CheckCopy.join(ref, n.line),
            actions = listOf(
                FixAction(CheckCopy.GO_TO_SCRIPT, FixKind.GO_TO_SCRIPT, n.sceneKey),
                FixAction(CheckCopy.MATCH, FixKind.ACKNOWLEDGE, n.sceneKey),
            ),
            storyOrder = scope.sceneOrder(n.sceneKey),
            lockedHidden = scope.hiding.scene(n.sceneKey) || scope.hiding.scene(n.previousKey),
        )
    }

    /** `◇ 전환 문구 · 쓴 뒤 때 바뀜` (or `장소 바뀜`) with `1-7 씬 4 · 06:10 → 07:00`. */
    fun stale(scope: CheckScope): List<CheckResult> = scope.transitionChecks.stale().map { s ->
        CheckResult(
            checkType = s.checkType,
            subjectKeys = s.subjectKeys,
            fingerprint = s.fingerprint,
            title = s.title,
            location = scope.sceneRef(s.sceneKey),
            body = s.detail,
            actions = listOf(
                FixAction(CheckCopy.TO_AUTO, FixKind.TO_AUTO, s.sceneKey),
                FixAction(CheckCopy.EDIT_TRANSITION, FixKind.EDIT_TRANSITION, s.sceneKey),
                FixAction(CheckCopy.MATCH, FixKind.ACKNOWLEDGE, s.sceneKey),
            ),
            storyOrder = scope.sceneOrder(s.sceneKey),
            lockedHidden = scope.hiding.scene(s.sceneKey),
        )
    }

    /** `◇ 공개 전 장소 · 전환 문구` with `1-3 씬 2 · 상층 관문`. */
    fun placesBeforeReveal(scope: CheckScope): List<CheckResult> = scope.transitionChecks.placesBeforeReveal().map { p ->
        CheckResult(
            checkType = p.checkType,
            subjectKeys = p.subjectKeys,
            fingerprint = p.fingerprint,
            title = p.title,
            location = scope.sceneRef(p.sceneKey),
            body = p.detail,
            actions = listOf(
                FixAction(CheckCopy.WRITE_DIRECT, FixKind.WRITE_DIRECT, p.sceneKey),
                FixAction(CheckCopy.MATCH, FixKind.ACKNOWLEDGE, p.sceneKey),
            ),
            storyOrder = scope.sceneOrder(p.sceneKey),
            lockedHidden = scope.hiding.scene(p.sceneKey) || (scope.hiding.locked && scope.doc.index.place(p.placeKey)?.hidden == true),
        )
    }
}
