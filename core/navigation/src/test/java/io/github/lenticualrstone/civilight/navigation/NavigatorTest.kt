package io.github.lenticualrstone.civilight.navigation

import androidx.navigation3.runtime.NavKey
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NavigatorTest {
    private val stack = mutableListOf<NavKey>(Library)
    private var lastPops = 0
    private val nav = BackStackNavigator(stack) { lastPops++ }

    @Test
    fun `push adds on top and ignores the route already on top`() {
        nav.push(ProjectHome("P"))
        nav.push(ProjectHome("P"))
        assertEquals(listOf(Library, ProjectHome("P")), stack)
    }

    @Test
    fun `pushing a route that is below returns to it`() {
        nav.push(ProjectHome("P"))
        nav.push(CastList("P"))
        nav.push(CharacterProfile("P", "C"))
        nav.push(ProjectHome("P"))
        assertEquals(listOf(Library, ProjectHome("P")), stack)
    }

    @Test
    fun `pop never empties the stack`() {
        nav.push(Settings)
        assertTrue(nav.pop())
        assertFalse(nav.pop())
        assertEquals(listOf<NavKey>(Library), stack)
        assertEquals(1, lastPops)
    }

    @Test
    fun `replace swaps the top`() {
        nav.push(Onboarding(OnboardingPage.WELCOME))
        nav.replace(Onboarding(OnboardingPage.LOOK))
        assertEquals(listOf(Library, Onboarding(OnboardingPage.LOOK)), stack)
        nav.replace(Library)
        assertEquals(listOf<NavKey>(Library), stack)
    }

    @Test
    fun `popTo stops at the match and keeps at least one route`() {
        nav.push(ProjectHome("P"))
        nav.push(CastList("P"))
        nav.push(CharacterProfile("P", "C"))
        assertTrue(nav.popToType<ProjectHome>())
        assertEquals(listOf(Library, ProjectHome("P")), stack)
        assertTrue(nav.popTo(ProjectHome("P"), inclusive = true))
        assertEquals(listOf<NavKey>(Library), stack)
        assertFalse(nav.popTo(Library, inclusive = true))
        assertFalse(nav.popTo(Settings))
        assertEquals(listOf<NavKey>(Library), stack)
    }

    @Test
    fun `resetTo keeps the common bottom and never passes through empty`() {
        val watched = NonEmptyList(mutableListOf(Library, ProjectHome("P"), Script("P")))
        val navigator = BackStackNavigator(watched)
        navigator.resetTo(Library, ProjectHome("Q"))
        assertEquals(listOf(Library, ProjectHome("Q")), watched.toList())
        navigator.resetTo(Settings)
        assertEquals(listOf<NavKey>(Settings), watched.toList())
        navigator.resetTo(Settings, Trash())
        assertEquals(listOf(Settings, Trash()), watched.toList())
    }

    /** Fails the test if a change ever leaves the list empty (NavDisplay rejects an empty stack). */
    private class NonEmptyList(private val inner: MutableList<NavKey>) : java.util.AbstractList<NavKey>() {
        override val size: Int get() = inner.size
        override fun get(index: Int): NavKey = inner[index]
        override fun set(index: Int, element: NavKey): NavKey = inner.set(index, element)
        override fun add(index: Int, element: NavKey) = inner.add(index, element)
        override fun removeAt(index: Int): NavKey = inner.removeAt(index).also { check(inner.isNotEmpty()) { "stack became empty" } }
    }
}
