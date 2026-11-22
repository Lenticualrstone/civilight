package io.github.lenticualrstone.civilight.ui.nav

import io.github.lenticualrstone.civilight.navigation.Navigator
import io.github.lenticualrstone.civilight.navigation.Route

/**
 * Opens [detail] from a list module (CNV-33). When the top route is already a detail shown next
 * to its list and [detail] pairs with the same list, the detail is replaced (choosing another row
 * swaps the right pane); otherwise it is pushed (cover screens, or the first selection).
 */
fun Navigator.openDetail(detail: Route) {
    val stack = backStack
    val top = stack.lastOrNull()
    val below = stack.getOrNull(stack.lastIndex - 1)
    if (top != detail && below != null && ListDetailPairs.isPair(below, top) && ListDetailPairs.isPair(below, detail)) {
        replace(detail)
    } else {
        push(detail)
    }
}
