package io.github.lenticualrstone.civilight.feature.world.list

import androidx.compose.animation.ContentTransform
import io.github.lenticualrstone.civilight.designsystem.card.CardViewMotion
import io.github.lenticualrstone.civilight.designsystem.theme.Motion
import io.github.lenticualrstone.civilight.navigation.WorldTab

/** What the 세계 list body shows: a tab in `목록` or in `카드`. */
data class WorldView(val tab: WorldTab, val cards: Boolean)

/**
 * The change of the list body (WLD-05, WLD-68, CNV-45). Pure, so the decisions are checked on the JVM:
 * a flip of `목록 | 카드` on the same tab uses the shared card-view motion (text leaves on `exit`, cards come
 * in after it and grow in place); a change of tab is a plain crossfade; nothing else animates as a swap.
 */
object WorldViewSwitch {
    enum class Kind { NONE, VIEW, TAB }

    /** Start scale of a card row growing in place from its top-left corner. */
    const val GROW_FROM = 0.92f

    fun kind(from: WorldView, to: WorldView): Kind = when {
        from.tab != to.tab -> Kind.TAB
        from.cards != to.cards -> Kind.VIEW
        else -> Kind.NONE
    }

    /** True while the cards of [to] grow in place: only `목록 → 카드` on one tab, and not when motion is reduced. */
    fun growsIn(from: WorldView, to: WorldView, reduced: Boolean): Boolean =
        kind(from, to) == Kind.VIEW && !from.cards && to.cards && CardViewMotion.plan(reduced).growMs != null

    /** The transition between two body states. */
    fun transition(from: WorldView, to: WorldView, motion: Motion): ContentTransform =
        if (kind(from, to) == Kind.VIEW) CardViewMotion.transform(motion) else motion.swap()

    /**
     * Lazy keys for [ids] in order: the first use of an id keeps it, a repeat gets `#2`, `#3` …, so a list never
     * crashes on a duplicate key and a row keeps its key when rows before it come and go.
     */
    fun uniqueKeys(ids: List<String>): List<String> {
        val seen = HashMap<String, Int>()
        return ids.map { id ->
            val n = (seen[id] ?: 0) + 1
            seen[id] = n
            if (n == 1) id else "$id#$n"
        }
    }
}
