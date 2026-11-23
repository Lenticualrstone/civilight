package io.github.lenticualrstone.civilight.domain.cast.map

import io.github.lenticualrstone.civilight.domain.cast.CastCopy
import io.github.lenticualrstone.civilight.domain.cast.CharacterEdits
import io.github.lenticualrstone.civilight.domain.cast.FirstAppearance
import io.github.lenticualrstone.civilight.domain.cast.ItemRefs
import io.github.lenticualrstone.civilight.domain.cast.ResolvedItem
import io.github.lenticualrstone.civilight.domain.cast.relation.RelationReader
import io.github.lenticualrstone.civilight.domain.cast.relation.RelationViews
import io.github.lenticualrstone.civilight.domain.cast.relation.Side
import io.github.lenticualrstone.civilight.domain.cast.timed.ReadPoint
import io.github.lenticualrstone.civilight.model.Key
import io.github.lenticualrstone.civilight.model.cast.RelationEndKind
import io.github.lenticualrstone.civilight.model.cast.RelationSymmetry
import io.github.lenticualrstone.civilight.model.cast.SlotValue
import io.github.lenticualrstone.civilight.model.doc.ProjectDoc
import io.github.lenticualrstone.civilight.model.position.LensPoint

/** `사이 · 마음 · 둘 다` (default `둘 다`). */
enum class MapSlotFilter(val label: String) {
    BETWEEN("사이"),
    MIND("마음"),
    BOTH("둘 다"),
}

/** Filters of the full relationship map (CST-33). */
data class MapFilter(
    val slots: MapSlotFilter = MapSlotFilter.BOTH,
    /** Kind chips; empty = all kinds. */
    val kindKeys: Set<Key> = emptySet(),
    /** Group chip by tag; null = `전체`. Both ends must have the tag ([PROPOSED]). */
    val tagKey: Key? = null,
)

/** Line style: solid when the pair has a `사이`, dashed when only minds are shown. Never coloured by kind. */
enum class LineStyle { SOLID, DASHED }

/**
 * The middle label of a `사이` line. Symmetric: the kind name. Pair kinds: both role names, each
 * toward its holder (`후배 ↔ 선배` read from A to B; CST Q15 default).
 */
data class MiddleLabel(val roleOfA: String, val roleOfB: String, val symmetric: Boolean) {
    val text: String get() = if (symmetric) roleOfA else "$roleOfA ↔ $roleOfB"
}

/**
 * A mind label near its holder's end, with an arrow toward the partner. [holderIsLeft] decides
 * the arrow: `경계 →` when the holder is drawn left of the partner, `← 보호` when right.
 */
data class MindLabel(val holderKey: Key, val kind: String) {
    fun text(holderIsLeft: Boolean): String = if (holderIsLeft) "$kind →" else "← $kind"
}

data class MapLine(
    val relationKey: Key,
    val endA: Key,
    val endB: Key,
    val style: LineStyle,
    val middle: MiddleLabel?,
    val mindA: MindLabel?,
    val mindB: MindLabel?,
    /** The selected relation is drawn in `acc` 2.2 px; others `ink3` 1.2 px. */
    val selected: Boolean,
) {
    /** Label texts with A drawn left of B (the default reading). */
    val mindTextA: String? get() = mindA?.text(holderIsLeft = true)
    val mindTextB: String? get() = mindB?.text(holderIsLeft = false)
}

/** A rectangular node card (no circles): name + subtitle. */
data class MapNode(val key: Key, val name: String, val subtitle: String)

data class RelationMapView(
    /** `인물 · 11명 중 4명`, `인물 · 12명 중 5명 · 숨김 1`. */
    val header: String,
    val total: Int,
    val shown: Int,
    val hidden: Int,
    val nodes: List<MapNode>,
    val lines: List<MapLine>,
    val slotFilters: List<MapSlotFilter> = MapSlotFilter.entries,
)

/** `고른 관계` card / sheet. */
data class SelectedRelationCard(
    /** `리엔 · 하르트`. */
    val title: String,
    /** `1-7` to `리엔 → 경계 · 하르트 → 보호`. */
    val steps: List<Pair<String, String>>,
    val recordLabel: String,
    val editLabel: String,
    val closeLabel: String,
)

/**
 * The full relationship map (CST-33): lines at the lens, filters, node set and header counts.
 * Deleted characters are absent at every lens; locked hidden characters draw no node and no line.
 */
object RelationMap {
    const val TITLE_COVER = "관계도"
    const val TITLE_INNER = "전체 관계도"

    fun build(
        doc: ProjectDoc,
        lens: LensPoint,
        filter: MapFilter = MapFilter(),
        hiddenUnlocked: Boolean = false,
        selectedRelationKey: Key? = null,
    ): RelationMapView {
        val point = ReadPoint.AtLens(lens)
        val live = CharacterEdits.live(doc)
        val visible = live.filter { ItemRefs.character(doc, it.key, hiddenUnlocked) is ResolvedItem.Live }
        val visibleKeys = visible.mapTo(HashSet()) { it.key }
        val lines = doc.cast.relations.mapNotNull { r ->
            if (r.endA.kind.known != RelationEndKind.CHARACTER || r.endB.kind.known != RelationEndKind.CHARACTER) return@mapNotNull null
            if (r.endA.key !in visibleKeys || r.endB.key !in visibleKeys || !doc.liveness.isLive(r)) return@mapNotNull null
            if (filter.tagKey != null && !listOf(r.endA.key, r.endB.key).all { k -> doc.index.character(k)?.tagKeys?.contains(filter.tagKey) == true }) {
                return@mapNotNull null
            }
            val state = RelationReader.state(doc, r, point)
            fun kindOk(v: SlotValue?) = v != null && (filter.kindKeys.isEmpty() || v.kindKey in filter.kindKeys)
            val between = state.between?.takeIf { filter.slots != MapSlotFilter.MIND && kindOk(it) }
            val mindA = state.mindA?.takeIf { filter.slots != MapSlotFilter.BETWEEN && kindOk(it) }
            val mindB = state.mindB?.takeIf { filter.slots != MapSlotFilter.BETWEEN && kindOk(it) }
            if (between == null && mindA == null && mindB == null) return@mapNotNull null
            MapLine(
                relationKey = r.key,
                endA = r.endA.key,
                endB = r.endB.key,
                style = if (between != null) LineStyle.SOLID else LineStyle.DASHED,
                middle = between?.let { b ->
                    val k = RelationReader.kind(doc, b)
                    MiddleLabel(
                        roleOfA = RelationReader.roleOf(doc, b, Side.A).orEmpty(),
                        roleOfB = RelationReader.roleOf(doc, b, Side.B).orEmpty(),
                        symmetric = k?.symmetry?.known != RelationSymmetry.PAIR || b.pairFirstHolder == null,
                    )
                },
                mindA = mindA?.let { MindLabel(r.endA.key, RelationReader.mindText(doc, it)!!.word) },
                mindB = mindB?.let { MindLabel(r.endB.key, RelationReader.mindText(doc, it)!!.word) },
                selected = r.key == selectedRelationKey,
            )
        }
        val drawn = lines.flatMap { listOf(it.endA, it.endB) }.toSet()
        val nodes = visible.filter { it.key in drawn }.map { c ->
            val tag = c.tagKeys.firstNotNullOfOrNull { io.github.lenticualrstone.civilight.domain.cast.CharacterEdits.liveTag(doc, it)?.name }
            val notYet = if (FirstAppearance.isAfterLens(doc, c.key, lens)) FirstAppearance.label(doc, c.key) else null
            MapNode(c.key, c.name, CastCopy.join(tag ?: c.oneLiner, notYet))
        }
        val hidden = live.size - visible.size
        return RelationMapView(
            header = header(live.size, nodes.size, hidden),
            total = live.size,
            shown = nodes.size,
            hidden = hidden,
            nodes = nodes,
            lines = lines,
        )
    }

    /** `인물 · 11명 중 4명`, with locked hidden characters `인물 · 12명 중 5명 · 숨김 1`. */
    fun header(total: Int, shown: Int, hidden: Int): String =
        "인물 · ${total}명 중 ${shown}명" + if (hidden > 0) " · 숨김 $hidden" else ""

    /** The `고른 관계` card of a line (CST-33). */
    fun selected(doc: ProjectDoc, relationKey: Key, lens: LensPoint): SelectedRelationCard? {
        val r = doc.index.relation(relationKey) ?: return null
        val a = doc.index.character(r.endA.key)?.name.orEmpty()
        val b = doc.index.character(r.endB.key)?.name.orEmpty()
        return SelectedRelationCard(
            title = "$a · $b",
            steps = RelationViews.neutralSteps(doc, r, lens),
            recordLabel = "인물 기록",
            editLabel = "관계 고치기",
            closeLabel = "닫기",
        )
    }
}
