package io.github.lenticualrstone.civilight.domain.script

import io.github.lenticualrstone.civilight.model.Key
import io.github.lenticualrstone.civilight.model.OrderKeys
import io.github.lenticualrstone.civilight.model.doc.ProjectDoc
import io.github.lenticualrstone.civilight.model.open
import io.github.lenticualrstone.civilight.model.project.TransitionMode
import io.github.lenticualrstone.civilight.model.script.BlockKind
import io.github.lenticualrstone.civilight.model.story.SceneTime
import io.github.lenticualrstone.civilight.model.story.StoryUnit
import io.github.lenticualrstone.civilight.model.text.TextRun
import io.github.lenticualrstone.civilight.model.text.plainText

/** `씬 합치기` (SCR-51): B merges into the previous scene A of the same episode. */
object SceneMerge {
    sealed interface Check {
        /** `앞 씬과 합치기` is not offered (first scene of its episode). */
        data object NotOffered : Check

        /** The sheet shows the reason and no merge button. */
        data class Blocked(val reason: String) : Check

        data class Allowed(val aKey: Key, val bKey: Key) : Check
    }

    /** The previous scene of [bKey] in its numbering unit, or why merging is not possible. */
    fun check(doc: ProjectDoc, bKey: Key): Check {
        val b = doc.index.unit(bKey)?.takeIf { doc.manuscript.isLiveUnit(it.key) } ?: return Check.NotOffered
        val unit = doc.numberingUnitOfScene(bKey)
        val scenes = doc.manuscript.liveScenes.filter { doc.numberingUnitOfScene(it.key) == unit }
        val i = scenes.indexOfFirst { it.key == bKey }
        val a = scenes.getOrNull(i - 1) ?: return Check.NotOffered
        if ((a.time is SceneTime.Flashback) != (b.time is SceneTime.Flashback)) return Check.Blocked("회상이 섞임 · 먼저 회상 끄기")
        if (a.hidden != b.hidden) return Check.Blocked("숨긴 씬이 섞임 · 먼저 숨김 풀기")
        return Check.Allowed(a.key, b.key)
    }

    /** A two-way choice row; [first] (A) is preselected. */
    data class Choice(val label: String, val first: String, val second: String)

    data class Preview(
        val title: String,
        val sub: String,
        /** `장소 · 결사의 문 · 씬 3 · 씬 4 같음` / `장소 · {place}` when only one side has one. */
        val placeRow: String,
        /** Asked only when both scenes have different places. */
        val placeChoice: Choice?,
        /** Asked only when both scenes have different 때. */
        val timeChoice: Choice?,
        val timeNote: String?,
        /** `씬 4 고정점 사라짐 · 씬 4 것을 고르면 직접 유지` when B is a fixed point. */
        val fixedPointNote: String?,
        /** `전환 문구 · 씬 4 직접` + `‘결사의 문 앞 · 06:10’`, when B has direct text. */
        val transitionRow: Pair<String, String>?,
        val changes: List<String>,
    ) {
        val title2: String get() = title
        val transitionChoices: List<String> get() = listOf("시스템 텍스트로 남기기", "지우기")
        val primary: String get() = "합치기"
    }

    private fun sameTime(a: SceneTime, b: SceneTime): Boolean = when {
        a is SceneTime.Relative && b is SceneTime.Relative -> a.copy(ext = null, rebasedFromKey = null) == b.copy(ext = null, rebasedFromKey = null)
        a is SceneTime.Direct && b is SceneTime.Direct -> a.copy(ext = null) == b.copy(ext = null)
        else -> a == b
    }

    fun preview(doc: ProjectDoc, bKey: Key, time: SceneTimeSource): Preview? {
        val check = check(doc, bKey) as? Check.Allowed ?: return null
        val a = doc.index.unit(check.aKey)!!
        val b = doc.index.unit(bKey)!!
        val na = Labels.sceneOrdinal(doc, a.key)!!
        val nb = Labels.sceneOrdinal(doc, b.key)!!
        val code = Labels.episodeCodeOfScene(doc, a.key)
        val pa = SceneText.placeName(doc, a.place)
        val pb = SceneText.placeName(doc, b.place)
        val bothPlaces = a.place != null && b.place != null
        val samePlace = bothPlaces && a.place!!.sameAs(b.place)
        val placeRow = when {
            samePlace -> "장소 · $pa · 씬 $na · 씬 $nb 같음"
            a.place == null && b.place == null -> "장소 · 장소 없음"
            else -> "장소 · ${pa ?: pb}"
        }
        val bothTimed = TimeChains.isTimed(a.time) && TimeChains.isTimed(b.time)
        val timeChoice = if (bothTimed && !sameTime(a.time, b.time)) {
            Choice("때 · 하나 고르기", "씬 $na 것 · ${SceneText.headTime(time.timeOf(a.key))}", "씬 $nb 것 · ${SceneText.headTime(time.timeOf(b.key))}")
        } else null
        val changes = mutableListOf<String>()
        val unit = doc.numberingUnitOfScene(b.key)
        for (s in dependentsOf(doc, b.key)) {
            val name = if (doc.numberingUnitOfScene(s.key) == unit) "씬 ${Labels.sceneOrdinal(doc, s.key)}" else Labels.sceneLocation(doc, s.key)
            changes += "$name 때 · 기준 씬 $nb → 씬 $na · 간격 다시 적음 · 절대 때 그대로"
        }
        changes += if (b.title.isNullOrBlank()) "제목 · ${Labels.titleOrUndecided(a)} · 씬 $nb 제목 없음"
        else "제목 · ${Labels.titleOrUndecided(a)} · 씬 $nb 제목 ${b.title} → 씬 메모"
        changes += "기록 · 블록에 그대로 · 위치 이름만 씬 $nb → 씬 $na"
        val directText = b.transitionRuns?.plainText?.takeIf { b.transitionModeOrNone == TransitionMode.DIRECT && it.isNotBlank() }
        return Preview(
            title = "씬 합치기",
            sub = "$code 씬 $na + 씬 $nb → 씬 $na".trim(),
            placeRow = placeRow,
            placeChoice = if (bothPlaces && !samePlace) Choice("장소", "씬 $na 것", "씬 $nb 것") else null,
            timeChoice = timeChoice,
            timeNote = timeChoice?.let { "고르지 않은 때 → 씬 메모 한 줄" },
            fixedPointNote = if (b.time is SceneTime.Direct) "씬 $nb 고정점 사라짐 · 씬 $nb 것을 고르면 직접 유지" else null,
            transitionRow = directText?.let { "전환 문구 · 씬 $nb 직접" to "‘$it’" },
            changes = changes,
        )
    }

    /** Live scenes whose stored base is [sceneKey], in manuscript order. */
    fun dependentsOf(doc: ProjectDoc, sceneKey: Key): List<StoryUnit> =
        doc.manuscript.liveScenes.filter { (it.time as? SceneTime.Relative)?.baseKey == sceneKey }

    data class Choices(
        val placeFromB: Boolean = false,
        val timeFromB: Boolean = false,
        /** `시스템 텍스트로 남기기` (default) or `지우기` for B's direct transition text. */
        val keepTransitionAsSystemText: Boolean = true,
    )

    data class Result(val doc: ProjectDoc, val systemBlockKey: Key?)

    /**
     * Applies the merge (one undo step = the caller's previous doc): B's blocks follow A's; A keeps
     * key, title and flags; unchosen differing values become one memo line `합치기 전 · 씬 {b} · …`;
     * B's title becomes the memo's first line and B's memo is appended; B's direct transition text
     * becomes a system-text block at the start of B's former blocks (new number) or is dropped with
     * `지우기`; scenes based on B are re-based on A with their absolute 때 kept; B's row is removed.
     */
    fun apply(doc: ProjectDoc, bKey: Key, choices: Choices, time: SceneTimeSource, ctx: EditContext): Result? {
        val check = check(doc, bKey) as? Check.Allowed ?: return null
        val a = doc.index.unit(check.aKey)!!
        val b = doc.index.unit(bKey)!!
        val na = Labels.sceneOrdinal(doc, a.key)!!
        val nb = Labels.sceneOrdinal(doc, b.key)!!

        // Blocks: B's after A's last block (live or trashed), same relative order.
        val aBlocks = doc.index.blocksOfScene(a.key)
        val bBlocks = doc.index.blocksOfScene(b.key)
        val directText = b.transitionRuns?.takeIf { b.transitionModeOrNone == TransitionMode.DIRECT && !it.plainText.isBlank() }
        val systemFirst = directText != null && choices.keepTransitionAsSystemText
        val orders = OrderKeys.between(aBlocks.lastOrNull()?.order, null, bBlocks.size + if (systemFirst) 1 else 0)
        var out = doc.replaceBlocks(
            bBlocks.mapIndexed { i, blk -> blk.copy(sceneKey = a.key, order = orders[i + if (systemFirst) 1 else 0], updatedAt = ctx.now, updatedBy = ctx.device) }
                .associateBy { it.key },
        )
        var systemKey: Key? = null
        if (systemFirst) {
            val (numbered, numbers) = Numbering.issue(out, out.numberingUnitOfScene(a.key), 1)
            val block = io.github.lenticualrstone.civilight.model.script.Block(
                key = ctx.newKey(), sceneKey = a.key, order = orders[0], number = numbers.single(),
                kind = BlockKind.SYSTEM.open(), runs = Runs.normalize(directText), createdAt = ctx.now,
                updatedAt = ctx.now, updatedBy = ctx.device,
            )
            out = numbered.addBlocks(listOf(block))
            systemKey = block.key
        }

        // Place, time, memo.
        val place = when {
            a.place == null -> b.place
            b.place == null -> a.place
            choices.placeFromB -> b.place
            else -> a.place
        }
        val bothTimed = TimeChains.isTimed(a.time) && TimeChains.isTimed(b.time)
        val timeDiffers = bothTimed && !sameTime(a.time, b.time)
        val takeB = !TimeChains.isTimed(a.time) || (timeDiffers && choices.timeFromB)
        val placeDiffers = a.place != null && b.place != null && !a.place!!.sameAs(b.place)
        val unchosen: String? = when {
            !timeDiffers && !placeDiffers -> null
            takeB && timeDiffers -> memoLine(doc, a, na, time)
            else -> memoLine(doc, b, nb, time)
        }
        val memo = (listOfNotNull(b.title?.takeIf { it.isNotBlank() }) +
            listOfNotNull(a.memo?.takeIf { it.isNotEmpty() }, b.memo?.takeIf { it.isNotEmpty() }, unchosen))
            .joinToString("\n").ifEmpty { null }
        val newATime = if (takeB && TimeChains.isTimed(b.time)) timeTaking(doc, a, b) else a.time
        val shiftOfA = if (takeB && TimeChains.isTimed(a.time)) TimeChains.offset(doc, a.key, b.key) else null

        val units = mutableMapOf<Key, StoryUnit>()
        units[a.key] = a.copy(
            place = place, time = newATime, memo = memo, silentCast = (a.silentCast + b.silentCast).distinct(),
            updatedAt = ctx.now, updatedBy = ctx.device,
        )
        // Scenes based on A keep their absolute time when A's time moved.
        if (shiftOfA != null) for (s in doc.story.units.filter { (it.time as? SceneTime.Relative)?.baseKey == a.key }) {
            if (s.key == b.key || s.key == a.key) continue
            val t = s.time as SceneTime.Relative
            units[s.key] = s.copy(time = TimeChains.rebased(t, a.key, TimeChains.Offset.ZERO - shiftOfA, null), updatedAt = ctx.now, updatedBy = ctx.device)
        }
        // Scenes based on B (live or in the trash) switch to A: absolute time kept.
        val bOffset = if (takeB) TimeChains.Offset.ZERO else TimeChains.offset(doc, a.key, b.key)
        for (s in doc.story.units.filter { it.key != b.key && (it.time as? SceneTime.Relative)?.baseKey == b.key }) {
            val t = s.time as SceneTime.Relative
            val shift = bOffset ?: TimeChains.Offset(0, 0, true)
            units[s.key] = s.copy(time = TimeChains.rebased(t, a.key, shift, null), updatedAt = ctx.now, updatedBy = ctx.device)
        }
        out = out.updateUnits(units)
        out = out.copy(story = out.story.copy(units = out.story.units.filterNot { it.key == b.key }))
        // Everything else that named B now names A (anchors, reveal points, lens, events, checks).
        val firstB = bBlocks.firstOrNull { doc.manuscript.isLiveBlock(it.key) }
        out = SceneRefs.retarget(out, b.key, a.key, firstB?.key)
        return Result(out, systemKey)
    }

    /** `합치기 전 · 씬 4 · 결사의 문 · 2일째 아침 06:10`. */
    private fun memoLine(doc: ProjectDoc, scene: StoryUnit, ordinal: Int, time: SceneTimeSource): String =
        listOfNotNull("합치기 전 · 씬 $ordinal", SceneText.placeName(doc, scene.place), SceneText.headTime(time.timeOf(scene.key)).takeIf { TimeChains.isTimed(scene.time) })
            .joinToString(" · ")

    /**
     * A's new time when B's is chosen: B's fixed point stays `직접`; B relative to A shifts A by the
     * interval; otherwise A takes B's stored time (same base).
     */
    private fun timeTaking(doc: ProjectDoc, a: StoryUnit, b: StoryUnit): SceneTime {
        val bt = b.time
        if (bt !is SceneTime.Relative || bt.baseKey != a.key) return bt
        return when (val at = a.time) {
            is SceneTime.Relative -> TimeChains.rebased(at, at.baseKey, TimeChains.offsetOf(bt), at.rebasedFromKey).copy(band = bt.band, clock = bt.clock)
            is SceneTime.Direct -> at.copy(storyDay = at.storyDay?.plus(bt.dayMin), band = bt.band, clock = bt.clock)
            else -> bt
        }
    }

    /** Text runs for a transition-as-system block (helper for callers building previews). */
    fun transitionRuns(scene: StoryUnit): List<TextRun> = scene.transitionRuns.orEmpty()
}
