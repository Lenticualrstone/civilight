package io.github.lenticualrstone.civilight.feature.canvas.gallery

import io.github.lenticualrstone.civilight.domain.story.time.SceneTimes
import io.github.lenticualrstone.civilight.domain.story.time.TimeLabels
import io.github.lenticualrstone.civilight.feature.canvas.image.PresetImages
import io.github.lenticualrstone.civilight.model.Key
import io.github.lenticualrstone.civilight.model.doc.ProjectDoc
import java.time.Instant
import java.time.ZoneId
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

// ---------------------------------------------------------------- slots

/** What the plans need of one place (pure, JVM-tested without a document). */
data class SlotFacts(
    /** Width / height of the picture; the lock bundle uses [GalleryStylePlans.LOCK_RATIO]. */
    val ratio: Float,
    val lock: Boolean = false,
    /** The first live tag's name in project tag order (태그 줄), null = untagged. */
    val tagGroup: String? = null,
    /** That tag's place in the project tag order (groups sort by it). */
    val tagOrder: Int? = null,
    /** Manuscript position of a live, visible linked scene (씬 순서); null = 씬 없음. */
    val scenePos: Int? = null,
    /** The lock bundle's hidden-image count (0 = the hidden characters' bundle, no count). */
    val lockCount: Int = 0,
)

/** The information an r3 view and its pane draw about one image (only what the masked view may show). */
data class SlotInfo(
    val title: String,
    val untitled: Boolean,
    val ratio: Float,
    val tags: List<String>,
    val tagGroup: String?,
    val tagOrder: Int?,
    /** `1-3 씬 2` of a live visible scene only; a hidden or trashed scene never gives a code here. */
    val sceneCode: String?,
    val sceneTime: String?,
    val scenePos: Int?,
    /** The panel's scene line (`숨긴 씬`, `지운 씬 · …` included) for the pane's `씬` row. */
    val sceneText: String?,
    val holo: Boolean,
    val caption: List<CaptionSpan>?,
    val size: String?,
    val ratioText: String?,
    val added: String?,
    val uses: String?,
    /** `삽화 · 1-3 씬 2 · 1일째 저녁 · 홀로` (tiles and the film caption). */
    val meta: String,
)

/** One place of an r3 view: a visible image, or the lock bundle (hidden images or characters) with the lock only. */
sealed interface StyleSlot {
    val id: String
    val facts: SlotFacts

    data class Item(val entry: GalleryEntry, val info: SlotInfo) : StyleSlot {
        override val id: String get() = entry.key
        override val facts: SlotFacts get() = SlotFacts(info.ratio, false, info.tagGroup, info.tagOrder, info.scenePos)
    }

    /** Never carries a key: picking it is screen state only, never `pickedKey` (GAL-19). */
    data class Locked(val count: Int, val owners: Boolean) : StyleSlot {
        override val id: String get() = if (owners) LOCK_OWNERS_ID else LOCK_ID
        override val facts: SlotFacts get() = SlotFacts(GalleryStylePlans.LOCK_RATIO, lock = true, lockCount = if (owners) 0 else count)

        /** `숨긴 이미지` or, for the hidden characters' bundle, `숨긴 인물`. */
        val label: String get() = if (owners) GalleryStyleCopy.HIDDEN_OWNERS else GalleryStyleCopy.HIDDEN_IMAGE
    }

    companion object {
        const val LOCK_ID = "#lock"
        const val LOCK_OWNERS_ID = "#lock-owners"
    }
}

/** The masked gallery as the r3 views see it: visible images in the view's order, then the lock bundles. */
class StyleModel(val view: GalleryView, val slots: List<StyleSlot>, val ownerName: String) {
    val facts: List<SlotFacts> = slots.map { it.facts }
    val itemCount: Int = slots.count { it is StyleSlot.Item }
    val hiddenLocked: Int get() = view.hiddenLocked

    fun indexOf(key: Key?): Int = if (key == null) -1 else slots.indexOfFirst { (it as? StyleSlot.Item)?.entry?.key == key }

    val lockIndex: Int get() = slots.indexOfFirst { it is StyleSlot.Locked }

    /** The index of the lock bundle with [id] ([StyleSlot.LOCK_ID] or [StyleSlot.LOCK_OWNERS_ID]), or -1. */
    fun lockIndexOf(id: String?): Int = if (id == null) -1 else slots.indexOfFirst { it is StyleSlot.Locked && it.id == id }

    fun item(i: Int): StyleSlot.Item? = slots.getOrNull(i) as? StyleSlot.Item
}

/** Builds [StyleModel] from the masked [GalleryView] (never from the raw document lists). */
object GalleryStyleModels {
    fun build(doc: ProjectDoc, view: GalleryView, locked: Boolean, zone: ZoneId): StyleModel {
        val labels = EntryLabels(doc, locked)
        val panels = GalleryPanels(doc, locked)
        val times by lazy { TimeLabels(SceneTimes(doc)) }
        val liveTags = doc.project.tags.filter { it.isLive }.sortedBy { it.order }
        val tagRank = liveTags.withIndex().associate { it.value.key to it.index }
        val tagNames = liveTags.associate { it.key to it.name }
        val blobs = doc.assets.blobs.associateBy { it.hash }
        val items = view.entries.map { e ->
            val item = e.item
            val panel = panels.panel(item, e.position, view.count, e.hiddenTag)
            val live = labels.sceneState(item.sceneKey) == EntryLabels.SceneState.LIVE
            val sceneKey = item.sceneKey
            val code = if (live && sceneKey != null) labels.sceneCode(sceneKey) else null
            val time = if (live && sceneKey != null) runCatching { times.short(sceneKey) }.getOrNull()?.takeIf { it.isNotBlank() } else null
            val tags = item.tagKeys.distinct().mapNotNull { k -> tagNames[k]?.let { k to it } }.sortedBy { tagRank[it.first] ?: Int.MAX_VALUE }
            val blob = item.blobHash?.let(blobs::get)
            val w = blob?.width ?: 0
            val h = blob?.height ?: 0
            val ratio = GalleryStylePlans.ratioOf(w, h, preset = item.isPreset)
            val uses = labels.chip(item.copy(holo = false))
            val meta = GalleryStyleCopy.join(
                listOf(tags.firstOrNull()?.second ?: GalleryStyleCopy.NO_TAG) + listOfNotNull(code, time, GalleryStyleCopy.HOLO.takeIf { item.holo }),
            )
            StyleSlot.Item(
                e,
                SlotInfo(
                    title = panel.title,
                    untitled = panel.untitled,
                    ratio = ratio,
                    tags = tags.map { it.second },
                    tagGroup = tags.firstOrNull()?.second,
                    tagOrder = tags.firstOrNull()?.first?.let(tagRank::get),
                    sceneCode = code,
                    sceneTime = time,
                    scenePos = if (live) labels.scenePosition(sceneKey) else null,
                    sceneText = panel.scene?.text,
                    holo = item.holo,
                    caption = panel.caption,
                    size = when {
                        item.isPreset -> GalleryStyleCopy.PRESET
                        w > 0 && h > 0 -> GalleryStyleCopy.pixels(w, h)
                        else -> null
                    },
                    ratioText = if (w > 0 && h > 0) GalleryStylePlans.ratioText(w, h) else null,
                    added = if (item.createdAt > 0L) Instant.ofEpochMilli(item.createdAt).atZone(zone).toLocalDate().let { GalleryStyleCopy.monthDay(it.monthValue, it.dayOfMonth) } else null,
                    uses = uses,
                    meta = meta,
                ),
            )
        }
        val locks = listOfNotNull(
            view.lockedRow?.let { StyleSlot.Locked(it.count, owners = false) },
            view.hiddenCharacters?.let { StyleSlot.Locked(0, owners = true) },
        )
        return StyleModel(view, items + locks, view.ownerName)
    }
}

// ---------------------------------------------------------------- plans

/** A view's pages (lists of slot indices); [order] = ◀ ▶ order = the pages read in turn. */
data class StylePages(val pages: List<List<Int>>) {
    val order: List<Int> = pages.flatten()

    fun pageOf(sel: Int): Int = pages.indexOfFirst { sel in it }.coerceAtLeast(0)
}

/** A row of 태그 줄: [label] null = a continuation row of the group above. */
data class TagRow(val label: String?, val slots: List<Int>)

/** 장면 순서: rows linked to a live scene (in manuscript order, paged) and the `씬 없음` lane (unlinked, then locks). */
data class ScenePlan(val placed: StylePages, val lane: List<Int>) {
    val order: List<Int> = placed.order + lane
}

/** 색 묶음 groups (board `TONES`). */
enum class GalleryTone(val label: String) {
    WARM(GalleryStyleCopy.WARM),
    COOL(GalleryStyleCopy.COOL),
    GREY(GalleryStyleCopy.GREY),
    LOCK(GalleryStyleCopy.LOCKED),
}

/** A rectangle in fractions of a view area, with a tilt (degrees) and a draw rank. */
data class FracRect(val x: Float, val y: Float, val w: Float, val h: Float, val rot: Float = 0f, val z: Int = 0)

/** Pure layout rules of the r3 gallery views (board `scr_v_gal.py`), sized from the measured area, never the board's px. */
object GalleryStylePlans {
    const val LOCK_RATIO = 0.7f
    private const val MIN_RATIO = 0.3f
    private const val MAX_RATIO = 3.5f
    const val MOOD_PAGE = 9
    const val LINE_PAGE = 9
    const val LINES = 3
    const val EXHIBIT_WALL = 3
    const val TONE_PAGE = 6
    const val TAG_ROW_MAX = 3
    const val TAG_ROWS_PAGE = 4
    const val SCENE_ROWS_PAGE = 4

    fun ratioOf(w: Int, h: Int, preset: Boolean): Float = when {
        preset -> 0.75f
        w > 0 && h > 0 -> (w.toFloat() / h).coerceIn(MIN_RATIO, MAX_RATIO)
        else -> 0.75f
    }

    private val NAMED = listOf(1 to 1, 4 to 5, 3 to 4, 2 to 3, 9 to 16, 5 to 4, 4 to 3, 3 to 2, 16 to 10, 16 to 9, 1 to 2, 2 to 1)

    /** `3:4`, `16:10`; an odd ratio reads `1.42:1`. */
    fun ratioText(w: Int, h: Int): String {
        if (w <= 0 || h <= 0) return GalleryStyleCopy.NONE
        val r = w.toDouble() / h
        NAMED.firstOrNull { (a, b) -> abs(r - a.toDouble() / b) / r < 0.015 }?.let { (a, b) -> return "$a:$b" }
        return if (r >= 1) "%.2f:1".format(java.util.Locale.ROOT, r) else "1:%.2f".format(java.util.Locale.ROOT, 1 / r)
    }

    /** The pick: the picked lock bundle ([pickedLockIndex]), else [pickedIndex], else the first slot; -1 when empty. */
    fun selection(count: Int, pickedIndex: Int, pickedLockIndex: Int): Int = when {
        count <= 0 -> -1
        pickedLockIndex in 0 until count -> pickedLockIndex
        pickedIndex in 0 until count -> pickedIndex
        else -> 0
    }

    /** A remembered page index kept inside 0..[lastIndex] (0 when there are no pages). */
    fun clampPage(page: Int, lastIndex: Int): Int = page.coerceIn(0, maxOf(0, lastIndex))

    /**
     * True when [selection] lands on a place the writer picked (a key or a lock bundle in this view), false when it
     * only fell back to the first slot: a first tap there picks it instead of opening it (GAL: picking is explicit).
     */
    fun isExplicit(count: Int, pickedIndex: Int, pickedLockIndex: Int): Boolean =
        pickedLockIndex in 0 until count || pickedIndex in 0 until count

    /** ◀ ▶ through [order]: the neighbour of [sel] ([d] = −1 or +1), or [sel] at an end. */
    fun step(order: List<Int>, sel: Int, d: Int): Int {
        if (order.isEmpty()) return sel
        val at = order.indexOf(sel)
        if (at < 0) return order.first()
        return order[(at + d).coerceIn(0, order.lastIndex)]
    }

    fun hasPrev(order: List<Int>, sel: Int): Boolean = order.indexOf(sel) > 0

    fun hasNext(order: List<Int>, sel: Int): Boolean = order.indexOf(sel).let { it >= 0 && it < order.lastIndex }

    /** `3 / 8` (position among the images in this view's order), `숨김 2` on the images' bundle, `숨김` on the characters'. */
    fun count(facts: List<SlotFacts>, order: List<Int>, sel: Int): String {
        val f = facts.getOrNull(sel) ?: return ""
        if (f.lock) return GalleryStyleCopy.hiddenCount(f.lockCount)
        val items = order.filter { facts.getOrNull(it)?.lock == false }
        return GalleryStyleCopy.count(items.indexOf(sel) + 1, items.size)
    }

    private fun all(facts: List<SlotFacts>): List<Int> = facts.indices.toList()

    fun chunked(indices: List<Int>, size: Int): StylePages = StylePages(if (indices.isEmpty()) emptyList() else indices.chunked(max(1, size)))

    fun film(facts: List<SlotFacts>): StylePages = StylePages(if (facts.isEmpty()) emptyList() else listOf(all(facts)))

    fun mood(facts: List<SlotFacts>): StylePages = chunked(all(facts), MOOD_PAGE)

    fun clothesline(facts: List<SlotFacts>): StylePages = chunked(all(facts), LINE_PAGE)

    /** Cover: one frame a page; inner: a wall of three. */
    fun exhibit(facts: List<SlotFacts>, inner: Boolean): StylePages = chunked(all(facts), if (inner) EXHIBIT_WALL else 1)

    // ---------------------------------------------------------------- 색 묶음

    /** Groups in tab order; each group's slot indices (in view order). */
    fun toneGroups(tones: List<GalleryTone>): Map<GalleryTone, List<Int>> =
        GalleryTone.entries.associateWith { t -> tones.indices.filter { tones[it] == t } }

    /** Pages: each group cut to [TONE_PAGE]; [toneOfPage] gives the tab of every page. */
    fun tone(tones: List<GalleryTone>): Pair<StylePages, List<GalleryTone>> {
        val pages = ArrayList<List<Int>>()
        val of = ArrayList<GalleryTone>()
        toneGroups(tones).forEach { (t, ids) ->
            ids.chunked(TONE_PAGE).forEach { pages += it; of += t }
        }
        return StylePages(pages) to of
    }

    /** Rows of one 색 묶음 page: an odd count puts one wide tile first; two stack (board `n2`); else pairs. */
    fun toneRows(n: Int): List<Int> = when {
        n <= 0 -> emptyList()
        n == 1 -> listOf(1)
        n == 2 -> listOf(1, 1)
        n % 2 == 1 -> listOf(1) + List((n - 1) / 2) { 2 }
        else -> List(n / 2) { 2 }
    }

    /** Main colour → group: low saturation or very dark = 잿빛; reds to yellows (and magentas) warm; the rest cool. */
    fun classify(r: Int, g: Int, b: Int): GalleryTone {
        val hsv = FloatArray(3)
        rgbToHsv(r, g, b, hsv)
        return when {
            hsv[1] < 0.16f || hsv[2] < 0.1f -> GalleryTone.GREY
            hsv[0] < 70f || hsv[0] >= 300f -> GalleryTone.WARM
            else -> GalleryTone.COOL
        }
    }

    /** A preset's group from the mean of its five colours. */
    fun presetTone(id: String?): GalleryTone {
        val p = PresetImages.byId(id) ?: return GalleryTone.GREY
        val cs = listOf(p.c0, p.c1, p.s1, p.s2, p.s3)
        val r = cs.sumOf { ((it shr 16) and 0xFF).toInt() } / cs.size
        val g = cs.sumOf { ((it shr 8) and 0xFF).toInt() } / cs.size
        val b = cs.sumOf { (it and 0xFF).toInt() } / cs.size
        return classify(r, g, b)
    }

    fun rgbToHsv(r: Int, g: Int, b: Int, out: FloatArray) {
        val rf = r / 255f
        val gf = g / 255f
        val bf = b / 255f
        val mx = max(rf, max(gf, bf))
        val mn = min(rf, min(gf, bf))
        val d = mx - mn
        val h = when {
            d == 0f -> 0f
            mx == rf -> 60f * (((gf - bf) / d) % 6f)
            mx == gf -> 60f * ((bf - rf) / d + 2f)
            else -> 60f * ((rf - gf) / d + 4f)
        }
        out[0] = if (h < 0f) h + 360f else h
        out[1] = if (mx == 0f) 0f else d / mx
        out[2] = mx
    }

    // ---------------------------------------------------------------- 태그 줄

    /**
     * Justified rows grouped by first tag (project tag order, untagged last with the lock bundles). A group is cut
     * into balanced rows of at most [TAG_ROW_MAX]; neighbouring groups of one or two images share a row
     * (`설정화 · 소품`) while it fits. Only the first row of a group carries the label.
     */
    fun tagRows(facts: List<SlotFacts>, perRow: Int = TAG_ROW_MAX): List<TagRow> {
        if (facts.isEmpty()) return emptyList()
        data class G(val label: String, val tagged: Boolean, val order: Int, val ids: List<Int>)
        val tagged = facts.indices.filter { !facts[it].lock && facts[it].tagGroup != null }
            .groupBy { facts[it].tagGroup!! }
            .map { (name, ids) -> G(name, true, facts[ids.first()].tagOrder ?: Int.MAX_VALUE, ids) }
            .sortedWith(compareBy<G> { it.order }.thenBy { it.ids.first() })
        val untagged = facts.indices.filter { !facts[it].lock && facts[it].tagGroup == null }
        val locks = facts.indices.filter { facts[it].lock }
        val rows = ArrayList<TagRow>()
        var pending: MutableList<G> = ArrayList()
        fun flushSmall() {
            if (pending.isEmpty()) return
            rows += TagRow(pending.joinToString(GalleryStyleCopy.SEP) { it.label }, pending.flatMap { it.ids })
            pending = ArrayList()
        }
        for (g in tagged) {
            if (g.ids.size <= 2) {
                if (pending.sumOf { it.ids.size } + g.ids.size > perRow) flushSmall()
                pending += g
                continue
            }
            flushSmall()
            balanced(g.ids, perRow).forEachIndexed { i, ids -> rows += TagRow(if (i == 0) GalleryStyleCopy.tagRow(g.label, g.ids.size) else null, ids) }
        }
        // A lone small group keeps its count (`삽화 2`) like a full one.
        if (pending.size == 1) {
            val g = pending.single()
            rows += TagRow(GalleryStyleCopy.tagRow(g.label, g.ids.size), g.ids)
            pending = ArrayList()
        }
        flushSmall()
        val tail = untagged + locks
        if (tail.isNotEmpty()) {
            val label = listOfNotNull(GalleryStyleCopy.NO_TAG.takeIf { untagged.isNotEmpty() }, GalleryStyleCopy.HIDDEN.takeIf { locks.isNotEmpty() }).joinToString(GalleryStyleCopy.SEP)
            balanced(tail, perRow).forEachIndexed { i, ids -> rows += TagRow(if (i == 0) label else null, ids) }
        }
        return rows
    }

    /** [ids] cut into ceil(n / max) rows whose sizes differ by at most one (4 → 2 + 2, 5 → 3 + 2). */
    fun balanced(ids: List<Int>, perRow: Int): List<List<Int>> {
        if (ids.isEmpty()) return emptyList()
        val rows = (ids.size + perRow - 1) / perRow
        val base = ids.size / rows
        val extra = ids.size % rows
        val out = ArrayList<List<Int>>()
        var at = 0
        for (r in 0 until rows) {
            val n = base + if (r < extra) 1 else 0
            out += ids.subList(at, at + n)
            at += n
        }
        return out
    }

    fun tagPages(rows: List<TagRow>): List<List<TagRow>> = rows.chunked(TAG_ROWS_PAGE)

    fun tagStylePages(rows: List<TagRow>): StylePages = StylePages(tagPages(rows).map { page -> page.flatMap { it.slots } })

    /** Row heights of a justified page as weights (∝ 1 / Σ ratios, the board's `fr` rows). */
    fun rowWeights(rows: List<TagRow>, facts: List<SlotFacts>): List<Float> =
        rows.map { row -> 1f / row.slots.sumOf { (facts.getOrNull(it)?.ratio ?: 1f).toDouble() }.toFloat().coerceAtLeast(0.2f) }

    // ---------------------------------------------------------------- 장면 순서

    fun scene(facts: List<SlotFacts>): ScenePlan {
        val placed = facts.indices.filter { !facts[it].lock && facts[it].scenePos != null }
            .sortedWith(compareBy<Int> { facts[it].scenePos }.thenBy { it })
        val loose = facts.indices.filter { !facts[it].lock && facts[it].scenePos == null }
        val locks = facts.indices.filter { facts[it].lock }
        return ScenePlan(chunked(placed, SCENE_ROWS_PAGE), loose + locks)
    }

    /** `1-3 씬 2` → `1-3` · `씬 2` (the spine's two lines); a code without a scene part stays whole. */
    fun splitCode(code: String): Pair<String, String> {
        val at = code.lastIndexOf(" ${GalleryUiCopy.SCENE} ")
        return if (at <= 0) code to "" else code.substring(0, at) to code.substring(at + 1)
    }

    // ---------------------------------------------------------------- 무드보드 · 빨랫줄 · 전시실 geometry

    /** Board `MOOD_C` (388 × 507) in fractions, in slot order; z = the board's draw rank. */
    val MOOD_COVER: List<FracRect> = moodFracs(
        388f, 507f,
        listOf(
            0 to floatArrayOf(12f, 14f, 110f, 146f, -3f), 1 to floatArrayOf(272f, 12f, 104f, 138f, 4f),
            2 to floatArrayOf(132f, 18f, 128f, 160f, 2f), 3 to floatArrayOf(12f, 176f, 150f, 100f, 2f),
            4 to floatArrayOf(174f, 196f, 120f, 90f, -2f), 5 to floatArrayOf(304f, 166f, 74f, 74f, -5f),
            6 to floatArrayOf(14f, 290f, 162f, 106f, -2f), 7 to floatArrayOf(292f, 254f, 86f, 236f, 3f),
            8 to floatArrayOf(186f, 302f, 96f, 96f, -3f),
        ),
        drawOrder = listOf(0, 2, 1, 3, 4, 5, 7, 6, 8),
    )
    val MOOD_COVER_NOTE = FracRect(10f / 388f, 412f / 507f, 262f / 388f, 84f / 507f, -1.5f)

    /** Board `MOOD_I` (426 × 770). */
    val MOOD_INNER: List<FracRect> = moodFracs(
        426f, 770f,
        listOf(
            0 to floatArrayOf(12f, 16f, 130f, 220f, -3f), 1 to floatArrayOf(322f, 18f, 94f, 140f, 4f),
            2 to floatArrayOf(152f, 10f, 160f, 232f, 2f), 3 to floatArrayOf(12f, 254f, 198f, 168f, 2f),
            4 to floatArrayOf(222f, 274f, 192f, 146f, -2f), 5 to floatArrayOf(326f, 172f, 88f, 88f, -4f),
            6 to floatArrayOf(12f, 440f, 198f, 318f, -2f), 7 to floatArrayOf(224f, 442f, 104f, 220f, 3f),
            8 to floatArrayOf(340f, 446f, 76f, 214f, 2f),
        ),
        drawOrder = listOf(0, 2, 1, 5, 3, 4, 6, 7, 8),
    )
    val MOOD_INNER_NOTE = FracRect(222f / 426f, 678f / 770f, 196f / 426f, 82f / 770f, -1.5f)

    private fun moodFracs(w: Float, h: Float, rects: List<Pair<Int, FloatArray>>, drawOrder: List<Int>): List<FracRect> =
        rects.sortedBy { it.first }.map { (i, r) -> FracRect(r[0] / w, r[1] / h, r[2] / w, r[3] / h, r[4], drawOrder.indexOf(i) + 1) }

    /** Board `ROT` of 빨랫줄, by place on the page. */
    val LINE_TILT: List<Float> = listOf(-3f, 2f, -2f, 3f, -1f, 2f, 2f, -3f, 1f)

    /** The polaroid picture height (dp) on a line [lineH] high: rope sag + clip on top, caption strip below. */
    fun polaroidPictureH(lineH: Float, pad: Float, cap: Float): Float = (lineH - 30f - cap - pad - 10f).coerceAtLeast(24f)

    /** A frame fitted in [boxW] × [boxH] at [ratio] (w / h): (w, h). */
    fun fit(boxW: Float, boxH: Float, ratio: Float): Pair<Float, Float> {
        if (boxW <= 0f || boxH <= 0f) return 0f to 0f
        val r = ratio.coerceIn(MIN_RATIO, MAX_RATIO)
        return if (boxW / boxH > r) boxH * r to boxH else boxW to boxW / r
    }
}
