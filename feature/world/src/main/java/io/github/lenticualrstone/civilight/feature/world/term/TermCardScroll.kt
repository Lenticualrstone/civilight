package io.github.lenticualrstone.civilight.feature.world.term

/**
 * (0.2.0, WLD-73) A tap on a `용어 후보 · N` row opens the term card scrolled to its `후보` group.
 * The `TermCard` route (core:navigation, frozen) has no scroll target yet, so the list leaves a
 * one-shot request here and the card takes it when it opens (feature-world.md, Requests: a route
 * flag). Requests live in this process only; a card restored later opens at its top.
 */
object TermCardScroll {
    /** Id of the `후보` group (`TermCardState`). */
    const val CANDIDATES: String = "candidates"

    /** At most one request: a newer tap replaces an older one that no card took. */
    private var pending: String? = null

    private fun id(projectKey: String, termKey: String) = "$projectKey/$termKey"

    @Synchronized
    fun request(projectKey: String, termKey: String) {
        pending = id(projectKey, termKey)
    }

    /** True once for a request of this card (and removes it). */
    @Synchronized
    fun take(projectKey: String, termKey: String): Boolean = (pending == id(projectKey, termKey)).also { if (it) pending = null }

    /**
     * The lazy-list index of the `후보` group in the card body (`termBody`: `art`, `head`, `fixed`, the
     * optional `notices`, `header`, then each group). With one group the `header` line names it, so the
     * target is `header`; with several, the group's own title item. Null without a `후보` group.
     */
    fun candidatesIndex(hasNotices: Boolean, groups: List<UsageGroupUi>): Int? {
        val header = 3 + (if (hasNotices) 1 else 0)
        if (groups.size <= 1) return header.takeIf { groups.firstOrNull()?.id == CANDIDATES }
        var index = header + 1
        groups.forEach { group ->
            if (group.id == CANDIDATES) return index
            index += 1 + group.rows.size
        }
        return null
    }

    /** [candidatesIndex] for a loaded open card. */
    fun candidatesIndex(card: TermCardUi): Int? =
        candidatesIndex(card.notices.isNotEmpty() || card.searchOff || card.chips.isNotEmpty(), card.groups)
}
