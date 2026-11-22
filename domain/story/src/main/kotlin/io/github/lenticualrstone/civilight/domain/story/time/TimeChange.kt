package io.github.lenticualrstone.civilight.domain.story.time

import io.github.lenticualrstone.civilight.domain.story.scene.EditContext
import io.github.lenticualrstone.civilight.domain.story.units.UnitCodes
import io.github.lenticualrstone.civilight.domain.story.withTimes
import io.github.lenticualrstone.civilight.model.Key
import io.github.lenticualrstone.civilight.model.doc.ProjectDoc
import io.github.lenticualrstone.civilight.model.open
import io.github.lenticualrstone.civilight.model.story.SceneTime
import io.github.lenticualrstone.civilight.model.story.TimePrecision

/** The two radio choices of the cascade preview (STO-23). */
enum class CascadeMode {
    /** `뒤도 같이` (default): dependents keep base + interval and shift with the scene. */
    WITH_FOLLOWING,

    /** `이 씬만`: direct dependents get their interval rewritten so their absolute time stays. */
    THIS_ONLY,
}

/** One `before → after` row of a dependent scene: `1-7 씬 2 | 약 12일째 저녁 | → | 약 14일째 저녁`. */
data class CascadeRow(val sceneKey: Key, val sceneRef: String, val before: String, val after: String)

/**
 * Preview of a change to one scene's 때 (`RTCascade`). Nothing is stored until the caller takes
 * [withFollowing] or [thisOnly] (each is one undo step: the previous doc).
 *
 * @property needsPreview false when no scene depends on the changed scene or its story day does not
 *   move (`ED`: a time-of-day change moves no dependent): the value is stored directly ([withFollowing]).
 */
data class TimeChangePreview(
    val sceneKey: Key,
    val needsPreview: Boolean,
    /** `씬 1 · 때 바꾸기`. */
    val title: String,
    /** `1-7 씬 1 · 첫 번째 깜박임 · 1-6 씬 1 기준`. */
    val sceneLine: String,
    /** `약 12일째 새벽`. */
    val before: String?,
    /** `약 14일째 새벽`. */
    val after: String?,
    /** `+2일 · 이야기 날`. */
    val deltaLine: String?,
    /** `+2일` (`+약 2일`, `+30분` for a time-of-day change). */
    val delta: String?,
    val dependents: List<Key>,
    /** `뒤 씬 4개 +2일 · 1-9 씬 2(직접)에서 멈춤`. */
    val summary: String?,
    /** At most four rows. */
    val rows: List<CascadeRow>,
    /** `외 7개 · 1-8 씬 2부터 1-9 씬 1까지 · 모두 +2일`. */
    val restLine: String?,
    /** `1-9 씬 2 | 직접 · 그대로 | 고정점`. */
    val fixedPointRow: String?,
    /** `뒤도 같이 · 기본 · 뒤 씬 4개 +2일`. */
    val withFollowingLine: String,
    /** `이 씬만 · 뒤 씬 때 그대로 · ◇ 순서와 때가 다름 1`. */
    val thisOnlyLine: String,
    /** New `순서와 때가 다름` notices if `이 씬만` is chosen. */
    val thisOnlyNotices: List<OrderNotice>,
    val withFollowing: ProjectDoc,
    val thisOnly: ProjectDoc,
) {
    fun result(mode: CascadeMode): ProjectDoc = if (mode == CascadeMode.WITH_FOLLOWING) withFollowing else thisOnly
}

/** STO-23: change one scene's stored 때 with the cascade preview. */
object TimeChange {
    fun preview(doc: ProjectDoc, sceneKey: Key, newTime: SceneTime, ctx: EditContext? = null): TimeChangePreview {
        val before = SceneTimes(doc)
        val codes = UnitCodes(doc)
        val labels = TimeLabels(before, codes)
        val scene = requireNotNull(doc.index.unit(sceneKey)) { "unknown scene $sceneKey" }
        val oldTimed = before.timed(sceneKey)
        val dependents = before.dependents(sceneKey)

        val stored = doc.withTimes(mapOf(sceneKey to newTime), ctx)
        val storedTimes = SceneTimes(stored)
        val newTimed = storedTimes.timed(sceneKey)

        // The scene leaves the chain: dependents rebase onto the previous timed scene, absolute kept (`ED`).
        val leaving = oldTimed != null && newTimed == null
        val withFollowing = if (leaving) rebaseDirectDependents(stored, before, sceneKey, ctx) else stored
        val after = SceneTimes(withFollowing)

        val dayDelta = if (oldTimed != null && newTimed != null) (newTimed.day - oldTimed.day).range else null
        val minuteDelta = if (oldTimed != null && newTimed != null) minutesBetween(oldTimed, newTimed) else null
        val dayMoves = dayDelta != null && dayDelta != Span.point(0)
        val needsPreview = dependents.isNotEmpty() && dayMoves && !leaving

        val thisOnly = if (needsPreview) rewriteDirectDependents(withFollowing, before, sceneKey, dayDelta!!, ctx) else withFollowing
        val thisOnlyNotices = if (needsPreview) OrderChecks.newNotices(before, SceneTimes(thisOnly)) else emptyList()

        val delta = when {
            minuteDelta == null -> null
            dayMoves -> Durations.signedDays(dayDelta!!)
            minuteDelta.isPoint -> signedDuration(minuteDelta.min)
            else -> "${if (minuteDelta.midpoint < 0) "−" else "+"}${Durations.approx(kotlin.math.abs(minuteDelta.midpoint))}"
        }
        val afterLabels = TimeLabels(after, UnitCodes(withFollowing))
        val rows = dependents.take(4).map { d ->
            CascadeRow(d.key, codes.sceneRef(d), labels.storyShort(d.key) ?: "때 없음", afterLabels.storyShort(d.key) ?: "때 없음")
        }
        val rest = dependents.drop(4)
        val restLine = if (rest.isEmpty() || delta == null) null else
            "외 ${rest.size}개 · ${codes.sceneRef(rest.first())}부터 ${codes.sceneRef(rest.last())}까지 · 모두 $delta"
        val fixedPoint = dependents.lastOrNull()?.let { last ->
            val from = before.position(last.key) ?: return@let null
            before.liveScenes.firstOrNull { s -> (before.position(s.key) ?: -1) > from && s.time is SceneTime.Direct && s.key != sceneKey }
        }
        val count = "뒤 씬 ${dependents.size}개 ${delta ?: ""}".trimEnd()
        val summary = if (!needsPreview) null else
            count + (fixedPoint?.let { " · ${codes.sceneRef(it)}(직접)에서 멈춤" } ?: "")

        val baseText = (scene.time as? SceneTime.Relative)?.let { before.timed(sceneKey)?.baseKey ?: it.baseKey }
            ?.let { codes.sceneRef(it)?.let { ref -> "$ref 기준" } }
        return TimeChangePreview(
            sceneKey = sceneKey,
            needsPreview = needsPreview,
            title = "${codes.sceneLabel(scene)} · 때 바꾸기",
            sceneLine = listOfNotNull(codes.sceneRef(scene), scene.title, baseText).joinToString(" · "),
            before = labels.storyShort(sceneKey),
            after = afterLabels.storyShort(sceneKey),
            deltaLine = delta?.let { "$it · 이야기 날" },
            delta = delta,
            dependents = dependents.map { it.key },
            summary = summary,
            rows = if (needsPreview) rows else emptyList(),
            restLine = if (needsPreview) restLine else null,
            fixedPointRow = if (needsPreview) fixedPoint?.let { "${codes.sceneRef(it)} | 직접 · 그대로 | 고정점" } else null,
            withFollowingLine = "뒤도 같이 · 기본 · $count",
            thisOnlyLine = "이 씬만 · 뒤 씬 때 그대로" + if (thisOnlyNotices.isEmpty()) "" else " · ◇ 순서와 때가 다름 ${thisOnlyNotices.size}",
            thisOnlyNotices = thisOnlyNotices,
            withFollowing = withFollowing,
            thisOnly = thisOnly,
        )
    }

    /** Applies a change directly with [mode] (the preview's result). */
    fun apply(doc: ProjectDoc, sceneKey: Key, newTime: SceneTime, mode: CascadeMode, ctx: EditContext? = null): ProjectDoc =
        preview(doc, sceneKey, newTime, ctx).result(mode)

    private fun signedDuration(minutes: Int): String =
        if (minutes < 0) "−" + Durations.gap(-minutes) else "+" + Durations.gap(minutes)

    /**
     * `이 씬만`: each direct dependent's interval absorbs the base's day delta (it may become
     * negative). An inexact delta widens the interval and makes it `대략` (`ED`).
     */
    private fun rewriteDirectDependents(doc: ProjectDoc, before: SceneTimes, sceneKey: Key, dayDelta: Span, ctx: EditContext?): ProjectDoc {
        val changes = before.directDependents(sceneKey).associate { d ->
            val r = d.time as SceneTime.Relative
            d.key to if (dayDelta.isPoint) {
                r.copy(dayMin = r.dayMin - dayDelta.min, dayMax = r.dayMax - dayDelta.min)
            } else {
                r.copy(dayMin = r.dayMin - dayDelta.max, dayMax = r.dayMax - dayDelta.min, precision = TimePrecision.APPROX.open())
            }
        }
        return doc.withTimes(changes, ctx)
    }

    /** Direct dependents of [sceneKey] move onto its previous timed scene, absolute kept. */
    internal fun rebaseDirectDependents(doc: ProjectDoc, before: SceneTimes, sceneKey: Key, ctx: EditContext?, rebasedFrom: Key? = null): ProjectDoc {
        val target = (before.doc.index.unit(sceneKey)?.time as? SceneTime.Relative)?.baseKey
            ?.takeIf { before.timed(it) != null && it != sceneKey && before.isLive(it) }
            ?: before.previousTimed(sceneKey)?.key
        val changes = before.directDependents(sceneKey).mapNotNull { d ->
            keepAbsoluteOn(before, d.key, target, rebasedFrom)?.let { d.key to it }
        }.toMap()
        return doc.withTimes(changes, ctx)
    }
}
