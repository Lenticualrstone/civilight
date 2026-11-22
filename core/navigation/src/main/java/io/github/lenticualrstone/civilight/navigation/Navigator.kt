package io.github.lenticualrstone.civilight.navigation

import androidx.navigation3.runtime.NavKey

/**
 * Moves through the app's single back stack. Features never touch the stack directly.
 *
 * Every route appears at most once in the stack (equal routes would share a content key): pushing a
 * route that is already below the top returns to it and drops what was above it.
 */
interface Navigator {
    /** The current stack, bottom first. */
    val backStack: List<NavKey>

    /** The route on top. */
    val current: NavKey get() = backStack.last()

    /** Opens [route] on top (or returns to it if it is already in the stack). */
    fun push(route: Route)

    /** Replaces the top route with [route] (e.g. moving between onboarding pages or tabs). */
    fun replace(route: Route)

    /**
     * Leaves the top route. Returns false when it is the last one; the stack is never emptied (the
     * shell then lets the system handle back, which leaves the app).
     */
    fun pop(): Boolean

    /**
     * Pops until the top matches [predicate] (also popping that one when [inclusive]). Returns false
     * and changes nothing when no route matches or the stack would become empty.
     */
    fun popTo(inclusive: Boolean = false, predicate: (NavKey) -> Boolean): Boolean

    /** Replaces the whole stack (`서재로`, finishing onboarding, opening a project from a widget). */
    fun resetTo(routes: List<Route>)
}

/** [Navigator.popTo] the nearest route equal to [route]. */
fun Navigator.popTo(route: Route, inclusive: Boolean = false): Boolean = popTo(inclusive) { it == route }

/** [Navigator.popTo] the nearest route of type [R]. */
inline fun <reified R : Route> Navigator.popToType(inclusive: Boolean = false): Boolean = popTo(inclusive) { it is R }

/** [Navigator.resetTo] with routes given inline. */
fun Navigator.resetTo(vararg routes: Route) = resetTo(routes.toList())

/**
 * [Navigator] over a mutable list (the Navigation3 `NavBackStack` in the app, a plain list in tests).
 * [onLastPop] runs when [pop] is called on the last route (the app finishes the activity).
 */
class BackStackNavigator(
    private val stack: MutableList<NavKey>,
    private val onLastPop: () -> Unit = {},
) : Navigator {
    override val backStack: List<NavKey> get() = stack

    override fun push(route: Route) {
        val existing = stack.indexOf(route)
        when {
            existing == stack.lastIndex -> Unit
            existing >= 0 -> removeAbove(existing)
            else -> stack.add(route)
        }
    }

    override fun replace(route: Route) {
        if (stack.isEmpty()) {
            stack.add(route)
            return
        }
        if (stack.last() == route) return
        val existing = stack.indexOf(route)
        if (existing >= 0) {
            removeAbove(existing)
            return
        }
        stack[stack.lastIndex] = route
    }

    override fun pop(): Boolean {
        if (stack.size <= 1) {
            onLastPop()
            return false
        }
        stack.removeAt(stack.lastIndex)
        return true
    }

    override fun popTo(inclusive: Boolean, predicate: (NavKey) -> Boolean): Boolean {
        val index = stack.indexOfLast(predicate)
        if (index < 0) return false
        val keep = if (inclusive) index else index + 1
        if (keep <= 0) return false
        removeFrom(keep)
        return true
    }

    override fun resetTo(routes: List<Route>) {
        require(routes.isNotEmpty()) { "the back stack cannot be empty" }
        val unique = routes.distinct()
        // Keep the common bottom so retained entries keep their state; add before removing so the
        // stack is never empty in between.
        var common = 0
        while (common < stack.size && common < unique.size && stack[common] == unique[common]) common++
        val oldSize = stack.size
        stack.addAll(unique.drop(common))
        repeat(oldSize - common) { stack.removeAt(common) }
    }

    private fun removeAbove(index: Int) = removeFrom(index + 1)

    private fun removeFrom(index: Int) {
        while (stack.size > index) stack.removeAt(stack.lastIndex)
    }
}
