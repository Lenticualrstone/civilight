package io.github.lenticualrstone.civilight.feature.story

import io.github.lenticualrstone.civilight.designsystem.text.CopyLint
import io.github.lenticualrstone.civilight.feature.story.bulk.BulkCopy
import io.github.lenticualrstone.civilight.feature.story.common.StoryCopy
import io.github.lenticualrstone.civilight.feature.story.search.SearchCopy
import io.github.lenticualrstone.civilight.feature.story.home.HomeCopy
import io.github.lenticualrstone.civilight.feature.story.menu.MenuCopy
import io.github.lenticualrstone.civilight.feature.story.newunit.NewUnitCopy
import io.github.lenticualrstone.civilight.feature.story.settings.NotationCopy
import io.github.lenticualrstone.civilight.feature.story.structure.StructureCopy
import io.github.lenticualrstone.civilight.feature.story.where.WhereCopy
import io.github.lenticualrstone.civilight.feature.story.move.MoveCopy
import io.github.lenticualrstone.civilight.feature.story.settings.DayBandCopy
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.lang.reflect.Modifier

/**
 * DECISIONS §3: every value of every `…Copy` object of this module passes `core:designsystem`
 * `CopyLint` (terse fragments, ` · `, no em dash, DS-63 vocabulary). Add new copy objects to [copyObjects].
 */
class StoryCopyLintTest {
    private val copyObjects: List<Any> = listOf(StoryCopy, HomeCopy, StructureCopy, MenuCopy, NewUnitCopy, NotationCopy, WhereCopy, MoveCopy, DayBandCopy, BulkCopy, SearchCopy)

    /** Button labels (the lint also rejects a lone `확인` on buttons). */
    private val buttons = setOf("NEW_SCENE", "CREATE", "UNDO", "CLEAR", "CHANGE", "WRITE", "RENUMBER", "CANCEL", "DISMISS", "APPLY", "MATCH", "REBASE_TO_PREVIOUS", "MOVE", "RESTORE", "KEEP", "REMOVE", "CHANGE_LINK", "SET_PLACE", "SET_TIME", "SUGGEST", "PUT", "FIX", "SKIP", "SELECT_ALL")

    private fun values(obj: Any): Map<String, String> = obj.javaClass.declaredFields
        .filter { Modifier.isStatic(it.modifiers) && it.type == String::class.java }
        .associate { f ->
            f.isAccessible = true
            f.name to f.get(null) as String
        }

    @Test
    fun everyCopyValuePassesTheLint() {
        val findings = copyObjects.flatMap { obj ->
            values(obj).mapNotNull { (name, value) ->
                CopyLint.lint(value, isButton = name in buttons).takeIf { it.isNotEmpty() }?.let { "${obj.javaClass.simpleName}.$name = '$value': $it" }
            }
        }
        assertEquals(emptyList<String>(), findings)
    }

    @Test
    fun copyObjectsAreNotEmptyAndTodoCopyIsNamedAsSuch() {
        copyObjects.forEach { assertTrue("${it.javaClass.simpleName} has values", values(it).isNotEmpty()) }
        values(StoryCopy).filterKeys { it.startsWith("TODO_COPY_") }.forEach { (name, value) -> assertEquals(name, value) }
    }

    @Test
    fun noHaeyoEndingsInCopy() {
        val haeyo = Regex("""(해요|세요|어요|아요|예요|이에요|습니다)[.!?]?$""")
        val hits = copyObjects.flatMap { values(it).values }.filter { haeyo.containsMatchIn(it) }
        assertEquals(emptyList<String>(), hits)
    }
}
