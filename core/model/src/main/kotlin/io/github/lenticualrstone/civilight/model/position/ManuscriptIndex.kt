package io.github.lenticualrstone.civilight.model.position

import io.github.lenticualrstone.civilight.model.Key
import io.github.lenticualrstone.civilight.model.change.Anchor
import io.github.lenticualrstone.civilight.model.project.StoryStructure
import io.github.lenticualrstone.civilight.model.script.Block
import io.github.lenticualrstone.civilight.model.story.StoryUnit
import io.github.lenticualrstone.civilight.model.world.TimelineEvent

/**
 * Manuscript order and story positions of one project (§4.2–§4.6), derived and cached per
 * project state.
 *
 * Ranks come from an Euler tour (§4.3) over **all** units and blocks, live and trashed, in order.
 * Among live positions this order is exactly the order of the live-only tour of §4.3; keeping the
 * non-live ones lets a reference to a trashed unit or block resolve to where it would be:
 * - an [Anchor] whose referent is not live resolves like its §5.5 relocation (the first live block
 *   after the old position, else unplaced), so reads agree before and after relocation;
 * - a [PointRef] whose unit is not live resolves to the start of the next live unit, else +∞ (§4.6);
 * - a referent that is not in the project at all resolves to unplaced (+∞); a missing world event
 *   sorts before every event (§5.5 `ED`).
 */
class ManuscriptIndex private constructor(
    private val startRank: Map<Key, Long>,
    private val endRank: Map<Key, Long>,
    private val transitionRank: Map<Key, Long>,
    private val blockRank: Map<Key, Long>,
    private val liveUnitKeys: Set<Key>,
    private val liveBlockKeys: Set<Key>,
    private val eventRank: Map<Key, Long>,
    private val eventCount: Int,
    /** Live scenes in manuscript order. */
    val liveScenes: List<StoryUnit>,
    /** Live blocks in manuscript order. */
    val liveBlocks: List<Block>,
    private val liveBlockRanks: LongArray,
    private val liveUnitStartRanks: LongArray,
    /** Scene whose end is the 집필 선, or null when nothing is written (§4.4). */
    val writingLineSceneKey: Key?,
) {
    private val liveBlocksByScene: Map<Key, List<Block>> by lazy { liveBlocks.groupBy { it.sceneKey } }
    private val liveSceneIndex: Map<Key, Int> by lazy { liveScenes.withIndex().associate { (i, s) -> s.key to i } }

    // --- Liveness as seen by the index ---

    fun isLiveUnit(key: Key): Boolean = key in liveUnitKeys
    fun isLiveBlock(key: Key): Boolean = key in liveBlockKeys

    /** Live blocks of a scene by order. */
    fun liveBlocksOf(sceneKey: Key): List<Block> = liveBlocksByScene[sceneKey].orEmpty()

    /** 0-based index of a live scene in manuscript order. */
    fun sceneIndex(sceneKey: Key): Int? = liveSceneIndex[sceneKey]

    // --- Raw ranks ---

    /**
     * The tour rank of a position, live or not; null when its referent is not in the project.
     * [Position.Unplaced] is +∞.
     */
    fun rankOf(position: Position): Rank? = when (position) {
        is Position.Start -> startRank[position.unitKey]?.let(::Rank)
        is Position.Transition -> transitionRank[position.sceneKey]?.let(::Rank)
        is Position.AtBlock -> blockRank[position.blockKey]?.let(::Rank)
        is Position.End -> endRank[position.unitKey]?.let(::Rank)
        is Position.Event -> eventRank[position.eventKey]?.let(::Rank)
        Position.Unplaced -> Rank.UNPLACED
    }

    /** Rank of a block (live or not), or null. A link inside a block or a choice option has this position. */
    fun blockRankOf(blockKey: Key): Rank? = blockRank[blockKey]?.let(::Rank)

    /** Rank of a scene's transition text (positions of transition runs). */
    fun transitionRankOf(sceneKey: Key): Rank? = transitionRank[sceneKey]?.let(::Rank)

    /** Total order of two positions; positions not in the project sort as unplaced. */
    fun compare(a: Position, b: Position): Int = (rankOf(a) ?: Rank.UNPLACED).compareTo(rankOf(b) ?: Rank.UNPLACED)

    // --- Anchors, points, lens ---

    /** `pos(anchor)` (§4.3); see the class doc for non-live and missing referents. */
    fun rankOf(anchor: Anchor): Rank = when (anchor) {
        is Anchor.AtBlock -> blockRank[anchor.blockKey]?.let { r ->
            if (anchor.blockKey in liveBlockKeys) Rank(r) else firstLiveBlockRankAfter(r)
        } ?: Rank.UNPLACED
        is Anchor.UnitStart -> unitAnchorRank(anchor.unitKey, startRank)
        is Anchor.UnitEnd -> unitAnchorRank(anchor.unitKey, endRank)
        is Anchor.WorldEvent -> Rank(eventRank[anchor.eventKey] ?: (-1L - eventCount))
        Anchor.Unplaced, is Anchor.Unknown -> Rank.UNPLACED
    }

    private fun unitAnchorRank(unitKey: Key, ranks: Map<Key, Long>): Rank {
        val r = ranks[unitKey] ?: return Rank.UNPLACED
        return if (unitKey in liveUnitKeys) Rank(r) else firstLiveBlockRankAfter(r)
    }

    /** `pos(point)` (§4.6). */
    fun rankOf(point: PointRef): Rank {
        val ranks = when (point.kind.known) {
            PointRefKind.UNIT_START -> startRank
            PointRefKind.UNIT_END -> endRank
            null -> return Rank.UNPLACED
        }
        val r = ranks[point.unitKey] ?: return Rank.UNPLACED
        if (point.unitKey in liveUnitKeys) return Rank(r)
        val start = startRank.getValue(point.unitKey)
        val next = firstGreater(liveUnitStartRanks, start) ?: return Rank.UNPLACED
        return Rank(next)
    }

    /** The rank of the 집필 선 (`end(S*)`), or null when nothing is written. */
    val writingLineRank: Rank? get() = writingLineSceneKey?.let { endRank[it] }?.let(::Rank)

    /** `pos(P)` of a lens point (§4.4): `최신` = the 집필 선 or +∞; an unknown unit reads as `최신`. */
    fun rankOf(lens: LensPoint): Rank = when (lens) {
        LensPoint.Latest -> writingLineRank ?: Rank.UNPLACED
        is LensPoint.UnitEnd -> endRank[lens.unitKey]?.let(::Rank) ?: rankOf(LensPoint.Latest)
    }

    /** A live record with this anchor applies at [lens] iff `pos(anchor) ≤ pos(P)`. */
    fun applies(anchor: Anchor, lens: LensPoint): Boolean = rankOf(anchor) <= rankOf(lens)

    /** `예정`: a 집필 선 exists and the anchor is after it (§4.4). */
    fun isPlanned(anchor: Anchor): Boolean = writingLineRank?.let { rankOf(anchor) > it } ?: false

    /** An item with a reveal point is visible at [lens] iff `pos(point) ≤ pos(P)`; no point = always. */
    fun isVisible(point: PointRef?, lens: LensPoint): Boolean = point == null || rankOf(point) <= rankOf(lens)

    // --- Relocation support (§5.5) ---

    /** The first live block strictly after [rank] in manuscript order, or null. */
    fun firstLiveBlockAfter(rank: Rank): Block? {
        val i = firstGreaterIndex(liveBlockRanks, rank.value)
        return if (i < 0) null else liveBlocks[i]
    }

    /**
     * Where an anchor goes when its referent stops being live (§5.5): the first live block after
     * its old position, or unplaced. Event and unplaced anchors do not move.
     */
    fun relocationTarget(anchor: Anchor): Anchor {
        val old = when (anchor) {
            is Anchor.AtBlock -> blockRank[anchor.blockKey]
            is Anchor.UnitStart -> startRank[anchor.unitKey]
            is Anchor.UnitEnd -> endRank[anchor.unitKey]
            else -> return anchor
        } ?: return Anchor.Unplaced
        return firstLiveBlockAfter(Rank(old))?.let { Anchor.AtBlock(it.key) } ?: Anchor.Unplaced
    }

    private fun firstLiveBlockRankAfter(rank: Long): Rank =
        firstGreater(liveBlockRanks, rank)?.let(::Rank) ?: Rank.UNPLACED

    companion object {
        /**
         * Builds the index from the structure, all units, all blocks and all timeline events
         * (live and trashed). Children and blocks are ordered by order key, then key.
         */
        fun build(
            structure: StoryStructure,
            units: List<StoryUnit>,
            blocks: List<Block>,
            events: List<TimelineEvent>,
        ): ManuscriptIndex {
            val sceneLevel = structure.sceneLevel
            val unitsByKey = units.associateBy { it.key }
            val children = units
                .groupBy { u -> u.parentKey?.takeIf { it in unitsByKey } }
                .mapValues { (_, list) -> list.sortedWith(compareBy<StoryUnit> { it.order }.thenBy { it.key }) }
            val blocksByScene = blocks.groupBy { it.sceneKey }
                .mapValues { (_, list) -> list.sortedWith(compareBy<Block> { it.order }.thenBy { it.key }) }

            val start = HashMap<Key, Long>()
            val end = HashMap<Key, Long>()
            val transition = HashMap<Key, Long>()
            val blockRank = HashMap<Key, Long>()
            val liveUnits = HashSet<Key>()
            val liveBlockKeys = HashSet<Key>()
            val liveScenes = ArrayList<StoryUnit>()
            val liveBlocks = ArrayList<Block>()
            val liveBlockRanks = ArrayList<Long>()
            val liveUnitStarts = ArrayList<Long>()
            var writingLine: Key? = null
            var rank = 0L

            fun visit(u: StoryUnit, parentLive: Boolean) {
                if (u.key in start) return
                val live = parentLive && u.deletedAt == null
                if (live) liveUnits += u.key
                start[u.key] = rank
                if (live) liveUnitStarts += rank
                rank++
                val sceneBlocks = blocksByScene[u.key]
                if (u.level == sceneLevel || sceneBlocks != null) {
                    if (live) liveScenes += u
                    transition[u.key] = rank++
                    var written = false
                    for (b in sceneBlocks.orEmpty()) {
                        blockRank[b.key] = rank
                        if (live && b.deletedAt == null) {
                            liveBlockKeys += b.key
                            liveBlocks += b
                            liveBlockRanks += rank
                            if (b.hasContent()) written = true
                        }
                        rank++
                    }
                    if (written) writingLine = u.key
                }
                for (c in children[u.key].orEmpty()) visit(c, live)
                end[u.key] = rank++
            }
            for (top in children[null].orEmpty()) visit(top, parentLive = true)

            val eventOrder = events.sortedWith(
                compareByDescending<TimelineEvent> { it.worldDate?.year ?: Int.MIN_VALUE }
                    .thenByDescending { it.worldDate?.month ?: 0 }
                    .thenByDescending { it.worldDate?.day ?: 0 }
                    .thenByDescending { it.order }
                    .thenByDescending { it.key },
            )
            val eventRank = eventOrder.withIndex().associate { (i, e) -> e.key to (-1L - i) }

            return ManuscriptIndex(
                startRank = start,
                endRank = end,
                transitionRank = transition,
                blockRank = blockRank,
                liveUnitKeys = liveUnits,
                liveBlockKeys = liveBlockKeys,
                eventRank = eventRank,
                eventCount = events.size,
                liveScenes = liveScenes,
                liveBlocks = liveBlocks,
                liveBlockRanks = liveBlockRanks.toLongArray(),
                liveUnitStartRanks = liveUnitStarts.toLongArray(),
                writingLineSceneKey = writingLine,
            )
        }

        /** Index of the first element > [value] in an ascending array, or −1. */
        private fun firstGreaterIndex(sorted: LongArray, value: Long): Int {
            var lo = 0
            var hi = sorted.size
            while (lo < hi) {
                val mid = (lo + hi) ushr 1
                if (sorted[mid] <= value) lo = mid + 1 else hi = mid
            }
            return if (lo < sorted.size) lo else -1
        }

        private fun firstGreater(sorted: LongArray, value: Long): Long? =
            firstGreaterIndex(sorted, value).takeIf { it >= 0 }?.let { sorted[it] }
    }
}
