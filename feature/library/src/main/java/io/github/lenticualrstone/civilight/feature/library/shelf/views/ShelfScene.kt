package io.github.lenticualrstone.civilight.feature.library.shelf.views

import io.github.lenticualrstone.civilight.feature.library.shelf.ShelfCardItem
import io.github.lenticualrstone.civilight.feature.library.shelf.ShelfCopy
import io.github.lenticualrstone.civilight.designsystem.theme.CivilightMotion
import io.github.lenticualrstone.civilight.model.app.LibraryStyle
import io.github.lenticualrstone.civilight.model.project.ProjectFormat
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sign
import kotlin.math.sin

/*
 * The pure parts of the six 서재 3D views (LIB-93, U-16): which items are drawn, where each one sits for a
 * scroll position, and the picked item's buttons. Everything is in dp and degrees so the JVM tests can check
 * the frame rule (DS-74: nothing leaves the view's frame) without a device. The composables only read these.
 */

/** One item's place for a frame (dp from the frame centre to the item centre; degrees). Reused per item: no per-frame allocation. */
class Pose {
    var tx = 0f
    var ty = 0f
    var rotX = 0f
    var rotY = 0f
    var rotZ = 0f
    var scale = 1f
    var alpha = 1f

    /** Draw order: higher is drawn later (in front). */
    var z = 0f

    /** Transform origin, as a fraction of the item's height (0 top … 1 bottom). */
    var pivotY = 0.5f

    /** `3D 책장`: 0 = the spine faces the viewer, 1 = the cover does. Other views draw the face (1). */
    var turn = 1f

    fun reset() {
        tx = 0f; ty = 0f; rotX = 0f; rotY = 0f; rotZ = 0f
        scale = 1f; alpha = 1f; z = 0f; pivotY = 0.5f; turn = 1f
    }
}

/** The view's frame and the item size of a style, in dp. */
data class SceneFrame(val width: Float, val height: Float, val itemW: Float, val itemH: Float)

/**
 * How many items a style composes behind and ahead of the item in front (`behind + 1 + ahead` = the cap). An item
 * enters or leaves the window at |rel| = reach + 0.5 (when `front` flips), so every style is fully faded there.
 */
data class SceneReach(val behind: Int, val ahead: Int)

/** A style's item size and frame height, cover screen and inner screen (`ED`: no U-16 board in the repository). */
data class SceneSize(val itemW: Float, val itemH: Float, val frameH: Float)

/** The picked item's own pair of text buttons (DS-70 c-class, LIB-93 `열기` and the view's pair). */
data class PickPair(val pick: String, val back: String)

/** Constellation clusters, in 형식 order; [OTHER] holds the lock item and `새 프로젝트`. */
enum class StarGroup { SERIAL, SHORT, SETTING_ONLY, OTHER }

/** One star: its cluster ([group] is an index into the drawn clusters), its slot on the orbit and the orbit's slot count. */
data class StarSlot(val group: Int, val slot: Int, val slotCount: Int)

/** The drawn clusters (empty ones dropped) and one slot per item, in item order (computed over all items, never the window). */
data class StarLayout(val groups: List<StarGroup>, val slots: List<StarSlot>)

object ShelfScene {
    /** Items composed at once (`ED`, 60 fps): 7 on the cover screen, 9 on the inner one. */
    const val COVER_CAP = 7
    const val WIDE_CAP = 9

    /** Perspective: Compose `cameraDistance` in dp units (multiplied by density in the layer). */
    const val CAMERA_DISTANCE_DP = 12f * 72f

    /** A drag of this share of the item step moves one item; a fling adds velocity × this many seconds. */
    const val FLING_SECONDS = 0.12f

    /** Constellation: one orbit takes this long (DS token; slow idle motion, stopped under reduced motion). */
    const val ORBIT_PERIOD_MS: Long = CivilightMotion.starOrbitPeriodMs

    fun cap(wide: Boolean): Int = if (wide) WIDE_CAP else COVER_CAP

    /** The styles that draw a 3D view (every [LibraryStyle] but `목록`). */
    val styles: List<LibraryStyle> = LibraryStyle.entries.filterNot { it.isList }

    fun size(style: LibraryStyle, wide: Boolean): SceneSize = when (style) {
        LibraryStyle.BOOKSHELF -> if (wide) SceneSize(156f, 208f, 340f) else SceneSize(132f, 176f, 300f)
        LibraryStyle.COVER_SPIN -> if (wide) SceneSize(170f, 227f, 330f) else SceneSize(140f, 187f, 290f)
        LibraryStyle.MANUSCRIPT_STACK -> if (wide) SceneSize(180f, 240f, 350f) else SceneSize(150f, 200f, 300f)
        LibraryStyle.ARCHIVE_HALL -> if (wide) SceneSize(150f, 200f, 330f) else SceneSize(120f, 160f, 290f)
        LibraryStyle.INDEX_DRAWER -> if (wide) SceneSize(280f, 168f, 330f) else SceneSize(220f, 132f, 280f)
        LibraryStyle.CONSTELLATION -> if (wide) SceneSize(56f, 75f, 330f) else SceneSize(44f, 59f, 290f)
        LibraryStyle.LIST -> SceneSize(0f, 0f, 0f)
    }

    /** `3D 책장` spine width (the item box stays the cover's size; the spine is drawn in its middle). */
    fun spineWidth(wide: Boolean): Float = if (wide) 40f else 34f

    /** Spine pitch on the shelf: spine plus a 4 dp gap. */
    fun spineStep(wide: Boolean): Float = spineWidth(wide) + 4f

    /**
     * The view's own pair for the picked item, or null when the view has none (a tap only brings the item
     * forward). Words from the DS-70 c-class list (`꺼내기` · `제자리` · `앞으로` · `돌려놓기`).
     */
    fun pickPair(style: LibraryStyle): PickPair? = when (style) {
        LibraryStyle.BOOKSHELF -> PickPair(ShelfViewCopy.PULL_OUT, ShelfViewCopy.IN_PLACE)
        LibraryStyle.MANUSCRIPT_STACK, LibraryStyle.INDEX_DRAWER -> PickPair(ShelfViewCopy.PULL_OUT, ShelfViewCopy.PUT_BACK)
        LibraryStyle.CONSTELLATION -> PickPair(ShelfViewCopy.FORWARD, ShelfViewCopy.IN_PLACE)
        LibraryStyle.COVER_SPIN, LibraryStyle.ARCHIVE_HALL, LibraryStyle.LIST -> null
    }

    /**
     * Behind and ahead of the item in front. The pile, the corridor and the drawer barely show what is behind
     * (one sheet, one card), so they spend their slots ahead; the shelf, the spin and the constellation are even.
     */
    fun reach(style: LibraryStyle, wide: Boolean): SceneReach = SceneReach(behind(style, wide), ahead(style, wide))

    /** [reach] without the allocation, for per-frame reads. */
    fun behind(style: LibraryStyle, wide: Boolean): Int = if (aheadOnly(style)) 1 else cap(wide) / 2

    fun ahead(style: LibraryStyle, wide: Boolean): Int = cap(wide) - 1 - behind(style, wide)

    private fun aheadOnly(style: LibraryStyle): Boolean =
        style == LibraryStyle.MANUSCRIPT_STACK || style == LibraryStyle.ARCHIVE_HALL || style == LibraryStyle.INDEX_DRAWER

    /** The item's name for accessibility: its title, the group's words for the lock item (never a hidden name). */
    fun itemName(item: ShelfCardItem): String = when (item) {
        is ShelfCardItem.Project -> item.card.row.title
        is ShelfCardItem.Lock -> item.group.title
        ShelfCardItem.NewProject -> ShelfCopy.NEW_PROJECT
    }

    /**
     * What a tap on the item does, for accessibility: in front, the view's pick word ([pair], `제자리` and the
     * like once [picked]) or `열기`, the lock's unlock words, `새 프로젝트`. Null away from the front (a tap
     * brings the item forward; no DS-70 word for that, so the default action is read).
     */
    fun tapLabel(item: ShelfCardItem, inFront: Boolean, pair: PickPair?, picked: Boolean): String? = when {
        !inFront -> null
        item is ShelfCardItem.Project -> when {
            pair == null -> ShelfViewCopy.OPEN
            picked -> pair.back
            else -> pair.pick
        }
        item is ShelfCardItem.Lock -> item.group.button
        else -> ShelfCopy.NEW_PROJECT
    }

    /** The composed index range: [cap] items around [focus], shifted to stay inside 0 until [count]. Empty for 0 items. */
    fun window(focus: Int, count: Int, cap: Int): IntRange = window(focus, count, SceneReach(cap / 2, cap - 1 - cap / 2))

    /** The composed index range for [reach], shifted to stay inside 0 until [count]. Empty for 0 items. */
    fun window(focus: Int, count: Int, reach: SceneReach): IntRange {
        val cap = reach.behind + 1 + reach.ahead
        if (count <= 0 || cap <= 0) return IntRange.EMPTY
        if (count <= cap) return 0 until count
        val f = focus.coerceIn(0, count - 1)
        val start = (f - reach.behind).coerceIn(0, count - cap)
        return start until start + cap
    }

    fun clampIndex(index: Int, count: Int): Int = if (count <= 0) 0 else index.coerceIn(0, count - 1)

    /** The scroll position rounded to the item in front. */
    fun nearest(position: Float, count: Int): Int = clampIndex(position.roundToInt(), count)

    /** Where a drag released at [position] with [velocityItems] (items per second) settles. */
    fun settle(position: Float, velocityItems: Float, count: Int): Int = nearest(position + velocityItems * FLING_SECONDS, count)

    /** The drag range: the ends give a little (a third of an item) before the settle brings them back. */
    fun dragRange(count: Int): ClosedFloatingPointRange<Float> = -0.35f..(max(count - 1, 0) + 0.35f)

    /** The index of [key] in [keys], or [fallback] clamped when it is not there (a re-lock, a removed project). */
    fun indexOf(keys: List<Any>, key: Any?, fallback: Int): Int {
        val i = if (key == null) -1 else keys.indexOf(key)
        return if (i >= 0) i else clampIndex(fallback, keys.size)
    }

    fun starGroup(item: ShelfCardItem): StarGroup = when (item) {
        // A project whose document is still loading sits with the model default (연재 장편).
        is ShelfCardItem.Project -> when (item.card.row.format ?: ProjectFormat.SERIAL) {
            ProjectFormat.SERIAL -> StarGroup.SERIAL
            ProjectFormat.SHORT -> StarGroup.SHORT
            ProjectFormat.SETTING_ONLY -> StarGroup.SETTING_ONLY
        }
        is ShelfCardItem.Lock, ShelfCardItem.NewProject -> StarGroup.OTHER
    }

    /** 별자리: clusters in [StarGroup] order (empty ones dropped) and each item's slot, in the given order. */
    fun stars(groups: List<StarGroup>): StarLayout {
        val drawn = StarGroup.entries.filter { it in groups }
        val counts = IntArray(StarGroup.entries.size)
        groups.forEach { counts[it.ordinal]++ }
        val next = IntArray(StarGroup.entries.size)
        val slots = groups.map { g -> StarSlot(drawn.indexOf(g), next[g.ordinal]++, counts[g.ordinal]) }
        return StarLayout(drawn, slots)
    }

    /** 별자리 cluster label: the 형식 word, none for [StarGroup.OTHER]. */
    fun groupLabel(group: StarGroup): String? = when (group) {
        StarGroup.SERIAL -> ShelfViewCopy.FORMAT_SERIAL
        StarGroup.SHORT -> ShelfViewCopy.FORMAT_SHORT
        StarGroup.SETTING_ONLY -> ShelfViewCopy.FORMAT_SETTING
        StarGroup.OTHER -> null
    }
}

/** Where every item of a view sits (DS-74 frame rule: [ShelfSceneTest] checks the extents at cover and 560 width). */
object ScenePoses {
    private fun fade(a: Float, from: Float, to: Float): Float = (1f - (a - from) / (to - from)).coerceIn(0f, 1f)

    private fun lerp(a: Float, b: Float, t: Float): Float = a + (b - a) * t

    /**
     * The window edge: 1 up to the last composed item at rest (|rel| = reach), 0 half an item further, where an
     * item enters or leaves the window as `front` flips. Multiplied into every style's alpha (stars too), so
     * nothing appears or vanishes in one frame.
     */
    fun edgeAlpha(style: LibraryStyle, rel: Float, wide: Boolean): Float {
        val r = (if (rel >= 0f) ShelfScene.ahead(style, wide) else ShelfScene.behind(style, wide)).toFloat()
        return fade(abs(rel), r, r + 0.5f)
    }

    /** A deterministic −1 … 1 jitter per item, so a pile looks hand-stacked but never moves on its own. */
    fun jitter(index: Int): Float = (((index * 37 + 11) % 7) - 3) / 3f

    /**
     * Places item [index] at [rel] = index − scroll position. [lift] (0 … 1) is the picked item's pull, used
     * only on the item in front. Under reduced motion the views do not call this (a flat face with a fade).
     */
    fun place(style: LibraryStyle, rel: Float, index: Int, lift: Float, wide: Boolean, frame: SceneFrame, out: Pose) {
        out.reset()
        val a = abs(rel)
        val s = sign(rel)
        val front = max(0f, 1f - a)
        val p = lift.coerceIn(0f, 1f) * front
        when (style) {
            LibraryStyle.BOOKSHELF -> {
                // Spines side by side; the pulled book comes forward, turns to its cover, the neighbours step aside.
                val step = ShelfScene.spineStep(wide)
                val spine = ShelfScene.spineWidth(wide)
                val pulledLift = lift.coerceIn(0f, 1f)
                val aside = (frame.itemW * 1.1f - spine) / 2f + 6f
                out.tx = rel * step + s * min(a, 1f) * pulledLift * aside
                out.ty = frame.height * 0.02f - front * 6f - p * 10f
                out.scale = 1f + p * 0.1f
                out.turn = p
                out.alpha = fade(a, 3.2f, 4.2f)
                out.z = -a + p * 4f
            }
            LibraryStyle.COVER_SPIN -> {
                val near = min(a, 1f)
                out.tx = s * (near * frame.itemW * 0.55f + max(a - 1f, 0f) * frame.itemW * 0.17f)
                out.rotY = -s * near * 58f
                out.scale = 1f - near * 0.18f
                out.alpha = fade(a, 2.9f, 3.6f)
                out.z = -a
            }
            LibraryStyle.MANUSCRIPT_STACK -> {
                // A pile on the desk: sheets under the top one peek out to the right; read ones fan away to the left.
                out.pivotY = 1f
                out.rotX = 16f * (1f - p)
                if (rel >= 0f) {
                    out.tx = rel * 12f + jitter(index) * 4f * min(a, 1f)
                    out.ty = rel * 4f
                    out.rotZ = rel * 2.5f + jitter(index) * 2f * min(a, 1f)
                    out.scale = 1f - min(a, 4f) * 0.025f
                    out.alpha = fade(a, 4.5f, 5.5f)
                } else {
                    out.tx = rel * frame.itemW * 0.3f
                    out.rotZ = rel * 9f
                    out.ty = a * 6f
                    out.alpha = fade(a, 1.2f, 2f)
                }
                out.ty -= p * 22f
                out.scale += p * 0.05f
                // Fanned sheets lie under the front one too (only the pile's order goes by depth).
                out.z = -a + p * 4f
            }
            LibraryStyle.ARCHIVE_HALL -> {
                // A corridor: items stand on both walls and converge on the vanishing point; the one in front turns to face you.
                val side = if (index % 2 == 0) -1f else 1f
                val depth = max(rel, 0f)
                val persp = 1f / (1f + depth * 0.55f)
                val wall = frame.width * 0.5f - frame.itemW * 0.5f
                out.tx = side * wall * persp * (1f - front * 1f)
                out.ty = -(1f - persp) * frame.height * 0.16f
                out.rotY = -side * 62f * (1f - front)
                out.scale = persp * (if (rel < 0f) 1f + a * 0.25f else 1f)
                out.alpha = if (rel < 0f) fade(a, 0.2f, 0.9f) else fade(depth, 3.6f, 4.6f)
                out.z = -a
            }
            LibraryStyle.INDEX_DRAWER -> {
                // A rolodex: cards hinge at their foot; those behind lean back, the one before the front falls forward.
                out.pivotY = 1f
                if (rel >= 0f) {
                    out.rotX = -min(rel, 5f) * 11f
                    out.ty = -rel * 9f
                    out.scale = 1f - min(rel, 5f) * 0.035f
                    out.alpha = fade(rel, 5f, 6f)
                } else {
                    out.rotX = min(a, 1f) * 92f
                    out.alpha = fade(a, 0.35f, 0.95f)
                }
                out.rotX *= 1f - p
                out.ty -= p * frame.itemH * 0.3f
                out.z = -rel + p * 4f
            }
            LibraryStyle.CONSTELLATION, LibraryStyle.LIST -> Unit
        }
        out.alpha *= edgeAlpha(style, rel, wide)
    }

    /**
     * 별자리 window fade: stars do not fade with distance, so they fade against the window's own bounds. The
     * window start follows [position] continuously (`position − behind`, clamped like [ShelfScene.window]); an
     * item inside it is 1, an item half an item outside (where it enters or leaves as `front` flips) is 0.
     * All 1 when every item fits.
     */
    fun windowAlpha(index: Int, position: Float, count: Int, behind: Int, cap: Int): Float {
        if (count <= cap) return 1f
        val start = (position - behind).coerceIn(0f, (count - cap).toFloat())
        val outside = max(start - index, index - (start + cap - 1))
        return fade(outside, 0f, 0.5f)
    }

    /**
     * 별자리: star [slot] on its cluster's orbit at [orbit] (0 … 1, the idle turn; fixed under reduced motion).
     * [focus] (0 … 1) enlarges the star in front; [lift] (0 … 1) brings it to the centre (`앞으로`).
     */
    fun star(slot: StarSlot, groupCount: Int, orbit: Float, focus: Float, lift: Float, frame: SceneFrame, out: Pose) {
        out.reset()
        val g = max(groupCount, 1)
        val usable = frame.width - frame.itemW * 1.6f
        val cell = usable / g
        val cx = -usable / 2f + cell * (slot.group + 0.5f)
        val cy = -frame.height * 0.08f
        val rx = min(cell * 0.26f, frame.width * 0.2f)
        val ry = rx * 0.42f
        val angle = 2f * PI.toFloat() * (slot.slot.toFloat() / max(slot.slotCount, 1) + orbit + slot.group * 0.17f)
        val depth = sin(angle)
        val f = focus.coerceIn(0f, 1f)
        val p = lift.coerceIn(0f, 1f)
        out.tx = lerp(cx + cos(angle) * rx, 0f, p)
        out.ty = lerp(cy + depth * ry, -frame.height * 0.06f, p)
        out.scale = lerp((0.8f + 0.14f * depth) * (1f + 0.22f * f), 2f, p)
        out.alpha = lerp(0.7f + 0.3f * (depth + 1f) / 2f, 1f, max(f, p))
        out.rotY = -cos(angle) * 18f * (1f - p)
        out.z = depth + f * 2f + p * 10f
    }

    /** 별자리 cluster centre (dp from the frame centre), for its label and its lines. */
    fun clusterCentre(group: Int, groupCount: Int, frame: SceneFrame): Float {
        val usable = frame.width - frame.itemW * 1.6f
        val cell = usable / max(groupCount, 1)
        return -usable / 2f + cell * (group + 0.5f)
    }

    /**
     * Whether item [index] is drawn while the view rests on [front]: an invisible item (passed, faded) takes no
     * tap and no accessibility focus, so a tap never lands on something the user cannot see.
     */
    fun visibleAtRest(style: LibraryStyle, index: Int, front: Int, wide: Boolean, frame: SceneFrame, scratch: Pose): Boolean {
        if (style == LibraryStyle.CONSTELLATION) return true
        place(style, (index - front).toFloat(), index, 0f, wide, frame, scratch)
        return scratch.alpha > 0.02f
    }

    /** The tap target's width: what is visible of the item (a spine on the shelf, the cover once pulled). */
    fun hitWidth(style: LibraryStyle, wide: Boolean, frame: SceneFrame, pulled: Boolean): Float =
        if (style == LibraryStyle.BOOKSHELF && !pulled) ShelfScene.spineWidth(wide) else frame.itemW

    /** 3D 책장 face turn: the spine turns away over the first half, the cover turns in over the second. */
    fun spineRotY(turn: Float): Float = min(turn.coerceIn(0f, 1f), 0.5f) * 180f

    fun coverRotY(turn: Float): Float = (max(turn.coerceIn(0f, 1f), 0.5f) - 1f) * 180f

    fun spineShown(turn: Float): Boolean = turn < 0.5f
}

