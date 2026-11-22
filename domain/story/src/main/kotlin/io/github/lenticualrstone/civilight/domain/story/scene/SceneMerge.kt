package io.github.lenticualrstone.civilight.domain.story.scene

import io.github.lenticualrstone.civilight.domain.story.time.SceneTimes
import io.github.lenticualrstone.civilight.domain.story.time.TimeLabels
import io.github.lenticualrstone.civilight.domain.story.time.keepAbsoluteOn
import io.github.lenticualrstone.civilight.domain.story.units.PlaceNames
import io.github.lenticualrstone.civilight.domain.story.units.UnitCodes
import io.github.lenticualrstone.civilight.model.Key
import io.github.lenticualrstone.civilight.model.OrderKeys
import io.github.lenticualrstone.civilight.model.app.LensState
import io.github.lenticualrstone.civilight.model.change.Anchor
import io.github.lenticualrstone.civilight.model.doc.ProjectDoc
import io.github.lenticualrstone.civilight.model.file.EpisodeFile
import io.github.lenticualrstone.civilight.model.open
import io.github.lenticualrstone.civilight.model.position.LensPoint
import io.github.lenticualrstone.civilight.model.project.TransitionMode
import io.github.lenticualrstone.civilight.model.script.Block
import io.github.lenticualrstone.civilight.model.script.BlockKind
import io.github.lenticualrstone.civilight.model.story.SceneTime
import io.github.lenticualrstone.civilight.model.story.StoryUnit
import io.github.lenticualrstone.civilight.model.story.TimePrecision
import io.github.lenticualrstone.civilight.model.text.isBlankText

/** Which scene's value a merge keeps (`씬 3 것` / `씬 4 것`). */
enum class MergeSide { FIRST, SECOND }

/** B's direct transition text: `시스템 텍스트로 남기기` (default) or `지우기`. */
enum class TransitionOnMerge { KEEP_AS_SYSTEM_TEXT, DISCARD }

data class MergeOptions(
    val place: MergeSide = MergeSide.FIRST,
    val time: MergeSide = MergeSide.FIRST,
    val transition: TransitionOnMerge = TransitionOnMerge.KEEP_AS_SYSTEM_TEXT,
)

/** Result of asking for `앞 씬과 합치기` on scene B. */
sealed interface MergePlan {
    /** No previous scene in the same episode (first scene of an episode): the item is not offered. */
    data object NotOffered : MergePlan

    /** `회상이 섞임 · 먼저 회상 끄기` / `숨긴 씬이 섞임 · 먼저 숨김 풀기`; [action] is the short form. */
    data class Blocked(val reason: String, val action: String) : MergePlan

    data class Ready(val preview: MergePreview) : MergePlan
}

/**
 * The `씬 합치기` sheet content (STO-09 values for SCR-51) and its result.
 *
 * @property placeChoices `씬 3 것 · <place>` / `씬 4 것 · <place>` when both differ; else [placeLine].
 * @property timeChoices `씬 3 것 · 2일째 새벽 05:40` / `씬 4 것 · 2일째 아침 06:10` when both differ.
 */
data class MergePreview(
    val firstKey: Key,
    val secondKey: Key,
    val title: String,
    val sub: String,
    val placeLine: String?,
    val placeChoices: List<String>,
    val timeLine: String?,
    val timeChoices: List<String>,
    val timeNote: String?,
    val fixedPointNote: String?,
    val transitionLine: String?,
    val transitionText: String?,
    val changeRows: List<String>,
    /** Lines appended to A's memo (`합치기 전 · 씬 4 · 결사의 문 · 2일째 아침 06:10`). */
    val memoLines: List<String>,
    val result: ProjectDoc,
)

/** `씬 합치기`: neighbours A (earlier) + B → A, same episode only. No other scene's absolute 때 changes. */
object SceneMerge {
    fun plan(doc: ProjectDoc, secondKey: Key, ctx: EditContext, options: MergeOptions = MergeOptions()): MergePlan {
        val b = requireNotNull(doc.index.unit(secondKey)) { "unknown scene $secondKey" }
        val siblings = doc.index.children(b.parentKey).filter { doc.liveness.isUnitLive(it.key) && it.level == b.level }
        val a = siblings.getOrNull(siblings.indexOfFirst { it.key == secondKey } - 1) ?: return MergePlan.NotOffered
        val aFlash = a.time is SceneTime.Flashback
        val bFlash = b.time is SceneTime.Flashback
        if (aFlash != bFlash) return MergePlan.Blocked("회상이 섞임 · 먼저 회상 끄기", "먼저 회상 끄기")
        if (a.hidden != b.hidden) return MergePlan.Blocked("숨긴 씬이 섞임 · 먼저 숨김 풀기", "먼저 숨김 풀기")
        return MergePlan.Ready(preview(doc, a, b, ctx, options))
    }

    private fun hasTime(t: SceneTime) = t is SceneTime.Relative || t is SceneTime.Direct || t is SceneTime.Flashback

    private fun preview(doc: ProjectDoc, a: StoryUnit, b: StoryUnit, ctx: EditContext, options: MergeOptions): MergePreview {
        val times = SceneTimes(doc)
        val codes = UnitCodes(doc)
        val labels = TimeLabels(times, codes)
        val places = PlaceNames(doc)
        val na = codes.ordinal(a)
        val nb = codes.ordinal(b)
        val word = codes.sceneLabel(a).substringBeforeLast(' ')

        // Place.
        val placeBoth = a.place != null && b.place != null
        val placeDiffers = placeBoth && !a.place!!.sameAs(b.place)
        val mergedPlace = when {
            a.place == null -> b.place
            b.place == null -> a.place
            placeDiffers && options.place == MergeSide.SECOND -> b.place
            else -> a.place
        }
        // Time.
        val timeBoth = hasTime(a.time) && hasTime(b.time)
        val timeDiffers = timeBoth && (labels.head(a.key) != labels.head(b.key) || times.timed(a.key)?.absolute != times.timed(b.key)?.absolute)
        val takeSecondTime = (!hasTime(a.time) && hasTime(b.time)) || (timeDiffers && options.time == MergeSide.SECOND)
        val mergedTime = if (takeSecondTime) timeFromSecond(doc, a, b) else a.time

        // Unchosen values become memo lines (one per scene whose value was not kept).
        val unchosen = buildList {
            val placeLoser = if (placeDiffers) (if (options.place == MergeSide.SECOND) a else b) else null
            val timeLoser = if (timeDiffers) (if (options.time == MergeSide.SECOND) a else b) else null
            for (s in listOf(a, b)) if (s == placeLoser || s == timeLoser) add(s)
        }
        val memoLines = unchosen.map { s -> "합치기 전 · ${codes.sceneLabel(s)} · ${places.nameOrNone(s.place)} · ${labels.head(s.key)}" }
        val memo = listOfNotNull(b.title, a.memo?.takeIf { it.isNotBlank() }, b.memo?.takeIf { it.isNotBlank() }) + memoLines
        val newA = a.copy(
            place = mergedPlace,
            time = mergedTime,
            memo = memo.joinToString("\n").ifEmpty { null },
            silentCast = (a.silentCast + b.silentCast).distinct(),
            updatedAt = ctx.now,
            updatedBy = ctx.device,
        )

        // Dependents of A (other than B) and of B keep their absolute time.
        val proxy = if (takeSecondTime) b.key else a.key
        val timeChanges = LinkedHashMap<Key, SceneTime>()
        val affected = (if (takeSecondTime) times.directDependents(a.key) else emptyList()) + times.directDependents(b.key)
        for (d in affected) {
            if (d.key == b.key || d.key == a.key) continue
            val onProxy = if (!hasTime(mergedTime)) keepAbsoluteOn(times, d.key, times.previousTimed(a.key)?.key) else keepAbsoluteOn(times, d.key, proxy)
            val rewritten = (onProxy as? SceneTime.Relative)?.let { if (it.baseKey == proxy) it.copy(baseKey = a.key) else it } ?: onProxy
            if (rewritten != null) timeChanges[d.key] = rewritten
        }

        // Blocks: B's follow A's; B's direct text may become a system-text block.
        val aBlocks = doc.index.blocksOfScene(a.key)
        val bBlocks = doc.index.blocksOfScene(b.key)
        val directText = b.transitionRuns?.takeIf { b.transitionModeOrNone == TransitionMode.DIRECT && !it.isBlankText() }
        val numberingKey = doc.index.numberingUnitKeyOfScene(a.key)
        val counterOwner = numberingKey?.let(doc.index::unit)
        var nextNumber = counterOwner?.nextBlockNumber ?: doc.project.counters.nextBlockNumber ?: 1
        val systemBlock = if (directText != null && options.transition == TransitionOnMerge.KEEP_AS_SYSTEM_TEXT) {
            Block(
                key = ctx.newKey(), sceneKey = a.key, order = "", number = nextNumber++, kind = BlockKind.SYSTEM.open(),
                runs = directText, createdAt = ctx.now, updatedAt = ctx.now, updatedBy = ctx.device,
            )
        } else {
            null
        }
        val appended = listOfNotNull(systemBlock) + bBlocks
        val orders = OrderKeys.between(aBlocks.maxOfOrNull { it.order }, null, appended.size)
        val reordered = appended.withIndex().associate { (i, blk) ->
            blk.key to blk.copy(sceneKey = a.key, order = orders[i], updatedAt = ctx.now, updatedBy = ctx.device)
        }
        val targetFile = numberingKey ?: doc.project.key
        val episodes = doc.episodes
            .mapValues { (_, f) -> f.copy(blocks = f.blocks.map { reordered[it.key] ?: it }) }
            .let { files ->
                if (systemBlock == null) {
                    files
                } else {
                    val file = files[targetFile] ?: EpisodeFile(unitKey = targetFile)
                    files + (targetFile to file.copy(blocks = file.blocks + reordered.getValue(systemBlock.key)))
                }
            }

        // Units: A rewritten, B removed (not trashed, SCR-51), counter grown, dependents rewritten.
        val units = doc.story.units.mapNotNull { u ->
            when {
                u.key == b.key -> null
                u.key == a.key -> newA
                u.key == counterOwner?.key && systemBlock != null -> u.copy(nextBlockNumber = nextNumber, updatedAt = ctx.now, updatedBy = ctx.device)
                u.key in timeChanges -> u.copy(time = timeChanges.getValue(u.key), updatedAt = ctx.now, updatedBy = ctx.device)
                else -> u
            }
        }
        var result = doc.copy(story = doc.story.copy(units = units), episodes = episodes)
        if (systemBlock != null && counterOwner == null) {
            result = result.copy(project = result.project.copy(counters = result.project.counters.copy(nextBlockNumber = nextNumber)))
        }
        result = retargetReferences(result, b.key, a.key, bBlocks.firstOrNull { it.deletedAt == null }?.key)

        val bFixed = b.time is SceneTime.Direct
        return MergePreview(
            firstKey = a.key,
            secondKey = b.key,
            title = "씬 합치기",
            sub = "${codes.sceneRef(a)} + ${codes.sceneLabel(b)} → ${codes.sceneLabel(a)}",
            placeLine = when {
                placeDiffers -> null
                placeBoth -> "장소 · ${places.nameOrNone(a.place)} · ${codes.sceneLabel(a)} · ${codes.sceneLabel(b)} 같음"
                mergedPlace != null -> "장소 · ${places.nameOrNone(mergedPlace)}"
                else -> null
            },
            placeChoices = if (placeDiffers) listOf(
                "$word $na 것 · ${places.nameOrNone(a.place)}",
                "$word $nb 것 · ${places.nameOrNone(b.place)}",
            ) else emptyList(),
            timeLine = when {
                timeDiffers -> "때 · 하나 고르기"
                hasTime(mergedTime) -> "때 · ${if (takeSecondTime) labels.head(b.key) else labels.head(a.key)}"
                else -> null
            },
            timeChoices = if (timeDiffers) listOf("$word $na 것 · ${labels.head(a.key)}", "$word $nb 것 · ${labels.head(b.key)}") else emptyList(),
            timeNote = if (timeDiffers) "고르지 않은 때 → 씬 메모 한 줄" else null,
            fixedPointNote = if (bFixed) "$word $nb 고정점 사라짐 · $word $nb 것을 고르면 직접 유지" else null,
            transitionLine = directText?.let { "전환 문구 · $word $nb 직접" },
            transitionText = directText?.let { runs -> "‘${runs.joinToString("") { it.shownText }}’" },
            changeRows = buildList {
                for (d in times.directDependents(b.key)) {
                    if (d.key == a.key) continue
                    add("${codes.sceneRefRelativeTo(d, b)} 때 · 기준 $word $nb → $word $na · 간격 다시 적음 · 절대 때 그대로")
                }
                add(
                    if (b.title == null) "제목 · ${a.title ?: "미정"} · $word $nb 제목 없음"
                    else "제목 · ${a.title ?: "미정"} · $word $nb 제목 ${b.title} → 씬 메모",
                )
                add("기록 · 블록에 그대로 · 위치 이름만 $word $nb → $word $na")
            },
            memoLines = memoLines,
            result = result,
        )
    }

    /**
     * A's new 때 when B's is chosen: A's absolute becomes B's old absolute. B based on A → A's own
     * interval absorbs B's; otherwise A takes B's stored value (B's fixed point stays `직접`).
     */
    private fun timeFromSecond(doc: ProjectDoc, a: StoryUnit, b: StoryUnit): SceneTime {
        val bt = b.time
        if (bt !is SceneTime.Relative || bt.baseKey != a.key) return bt
        return when (val at = a.time) {
            is SceneTime.Relative -> at.copy(
                dayMin = at.dayMin + bt.dayMin,
                dayMax = at.dayMax + bt.dayMax,
                precision = if (at.dayMin + bt.dayMin == at.dayMax + bt.dayMax && at.precision.known != TimePrecision.APPROX) at.precision
                else TimePrecision.APPROX.open(),
                band = bt.band,
                clock = bt.clock,
                rebasedFromKey = null,
            )
            is SceneTime.Direct -> at.copy(storyDay = at.storyDay?.plus(if (bt.dayMin == bt.dayMax) bt.dayMin else (bt.dayMin + bt.dayMax + 1) / 2), band = bt.band, clock = bt.clock)
            else -> keepAbsoluteOn(SceneTimes(doc), b.key, SceneTimes(doc).previousTimed(a.key)?.key) ?: bt
        }
    }

    /**
     * References to the removed scene B move to A (`ED`): unit anchors (`UnitStart(B)` → B's first
     * block, `UnitEnd(B)` → `UnitEnd(A)`), the lens, travel overrides (dropped when both ends become
     * A), acknowledgements about B (dropped: re-evaluated) and story events.
     */
    private fun retargetReferences(doc: ProjectDoc, bKey: Key, aKey: Key, bFirstBlock: Key?): ProjectDoc {
        val records = doc.changes.records.map { r ->
            when (val anchor = r.anchor) {
                is Anchor.UnitStart -> if (anchor.unitKey == bKey) r.copy(anchor = bFirstBlock?.let { Anchor.AtBlock(it) } ?: Anchor.UnitEnd(aKey)) else r
                is Anchor.UnitEnd -> if (anchor.unitKey == bKey) r.copy(anchor = Anchor.UnitEnd(aKey)) else r
                else -> r
            }
        }
        fun point(p: LensPoint?): LensPoint? = if (p is LensPoint.UnitEnd && p.unitKey == bKey) LensPoint.UnitEnd(aKey) else p
        val lens = doc.app.view.lens
        val newLens = LensState(point(lens.point) ?: LensPoint.Latest, point(lens.savedForScript))
        val overrides = doc.checks.travelOverrides.map { o ->
            o.copy(
                fromSceneKey = if (o.fromSceneKey == bKey) aKey else o.fromSceneKey,
                toSceneKey = if (o.toSceneKey == bKey) aKey else o.toSceneKey,
            )
        }.filter { it.fromSceneKey != it.toSceneKey }
        val acks = doc.checks.acknowledgements.filterNot { bKey in it.subjectKeys }
        val events = doc.world.events.map { e -> if (e.sceneKey == bKey) e.copy(sceneKey = aKey) else e }
        return doc.copy(
            changes = doc.changes.copy(records = records),
            app = doc.app.copy(view = doc.app.view.copy(lens = newLens)),
            checks = doc.checks.copy(acknowledgements = acks, travelOverrides = overrides),
            world = doc.world.copy(events = events),
        )
    }
}
