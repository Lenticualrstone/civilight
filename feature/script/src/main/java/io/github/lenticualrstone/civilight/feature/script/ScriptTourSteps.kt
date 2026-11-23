package io.github.lenticualrstone.civilight.feature.script

/**
 * The script tour's targets (LIB-19, board `Tour1`; copy in `ModuleTours`): which element each step's
 * hole goes around. The tour ends at `씬 머리`: the script has no lens (STO-16), so there is no
 * `시점 렌즈` step. The block and head steps point at rows on screen, never at the first rows of the
 * episode: `이어 쓰기` opens the script at the last place written; a step whose row is not on screen
 * leaves the tour ([missing]), so no card floats with nothing marked.
 */
object ScriptTourSteps {
    /** `명령 칸`: the command row at the bottom. */
    const val COMMAND: Int = 0

    /** `블록을 왼쪽으로`: a block row. */
    const val BLOCK: Int = 1

    /** `씬 머리`: a scene head. */
    const val HEAD: Int = 2

    /**
     * Steps this script has no element for: the read-only example draws `예시 홈으로 · 온보딩 끝내기`
     * where the command field would be, so `명령 칸` is left out of its tour (emulator QA round 2).
     */
    fun absent(readOnly: Boolean): Set<Int> = if (readOnly) setOf(COMMAND) else emptySet()

    /** How many steps the script tour has (`명령 칸`, `블록을 왼쪽으로`, `씬 머리`). */
    const val COUNT: Int = 3

    /**
     * The row steps with no row on screen ([blockKey] / [headKey] null, from [firstVisible]): they leave
     * the tour. Never every step at once: a tour with nothing left would stop showing, the targets would
     * be forgotten, and it would come back (the read-only example always has rows, so this is a guard).
     */
    fun missing(blockKey: Any?, headKey: Any?, readOnly: Boolean): Set<Int> {
        val gone = buildSet {
            if (blockKey == null) add(BLOCK)
            if (headKey == null) add(HEAD)
        }
        return if ((gone + absent(readOnly)).size >= COUNT) emptySet() else gone
    }

    /** One list item as the list lays it out: [top] and [bottom] in the list's pixels. */
    data class Item(val key: Any?, val top: Int, val bottom: Int)

    /**
     * The key of the first item in [items] (in list order) that [isKind] accepts and that lies whole
     * between [windowTop] and [windowBottom] (the part not under the bars); else the first such item
     * partly in it; else null.
     */
    fun firstVisible(items: List<Item>, windowTop: Int, windowBottom: Int, isKind: (Any?) -> Boolean): Any? {
        val candidates = items.filter { isKind(it.key) }
        return candidates.firstOrNull { it.top >= windowTop && it.bottom <= windowBottom }?.key
            ?: candidates.firstOrNull { it.bottom > windowTop && it.top < windowBottom }?.key
    }
}
