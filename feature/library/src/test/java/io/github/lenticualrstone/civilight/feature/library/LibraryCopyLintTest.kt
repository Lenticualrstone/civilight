package io.github.lenticualrstone.civilight.feature.library

import io.github.lenticualrstone.civilight.designsystem.text.CopyLint
import io.github.lenticualrstone.civilight.feature.library.newproject.NewProjectCopy
import io.github.lenticualrstone.civilight.feature.library.newproject.NewProjectRules
import io.github.lenticualrstone.civilight.feature.library.projectsettings.ProjectSettingsCopy
import io.github.lenticualrstone.civilight.feature.library.protection.ProtectionCopy
import io.github.lenticualrstone.civilight.feature.library.rating.RatingCopy
import io.github.lenticualrstone.civilight.feature.library.rating.RatingSettingsState
import io.github.lenticualrstone.civilight.feature.library.shelf.ShelfCopy
import io.github.lenticualrstone.civilight.model.project.Rating
import io.github.lenticualrstone.civilight.navigation.NewProjectStep
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.lang.reflect.Modifier

/** DECISIONS §3: every value of this module's copy objects passes the design-system copy lint. */
class LibraryCopyLintTest {
    private val objects: List<Any> = listOf(ShelfCopy, NewProjectCopy, RatingCopy, ProtectionCopy, ProjectSettingsCopy)

    /** `Object.FIELD` → string value, for every constant of every copy object. */
    private fun values(): Map<String, String> = objects.flatMap { obj ->
        obj::class.java.declaredFields
            .filter { Modifier.isStatic(it.modifiers) && it.type == String::class.java }
            .map { field ->
                field.isAccessible = true
                "${obj::class.java.simpleName}.${field.name}" to field.get(null) as String
            }
    }.toMap()

    /** Copy built from parts, with sample values from the boards. */
    private fun formatted(): List<String> = buildList {
        NewProjectStep.entries.forEach {
            add(NewProjectRules.smallLine(it))
            add(NewProjectRules.title(it))
        }
        Rating.entries.forEach { add(RatingSettingsState.applyTitle(it, 2)) }
    }

    @Test
    fun `every copy value passes the lint`() {
        val found = values()
        assertTrue("every copy object exposes its constants", objects.all { obj -> found.keys.any { it.startsWith(obj::class.java.simpleName + ".") } })
        val failures = found.filterValues { !it.startsWith("TODO_COPY_") }.mapNotNull { (name, text) ->
            val issues = CopyLint.lint(text)
            if (issues.isEmpty()) null else "$name = $text: $issues"
        } + formatted().mapNotNull { text -> CopyLint.lint(text).takeIf { it.isNotEmpty() }?.let { "$text: $it" } }
        assertEquals(emptyList<String>(), failures)
    }

    @Test
    fun `button labels are verbs, never a lone confirm`() {
        val buttons = listOf(
            NewProjectCopy.CANCEL, NewProjectCopy.NEXT, NewProjectCopy.PREVIOUS, NewProjectCopy.CREATE,
            ShelfCopy.RENAME_CONFIRM, ShelfCopy.CANCEL, ShelfCopy.RELOCK, RatingCopy.CANCEL, RatingCopy.APPLY_TOGETHER,
        )
        val failures = buttons.filter { CopyLint.lint(it, isButton = true).isNotEmpty() }
        assertEquals(emptyList<String>(), failures)
    }

    @Test
    fun `missing copy is marked as such`() {
        values().filterKeys { it.substringAfter('.').startsWith("TODO_COPY_") }.forEach { (name, text) ->
            assertEquals(name.substringAfter('.'), text)
        }
    }

    @Test
    fun `formatted copy reads as the boards`() {
        assertEquals("새 프로젝트 · 4/4", NewProjectRules.smallLine(NewProjectStep.START))
        assertEquals("시작 재료", NewProjectRules.title(NewProjectStep.START))
        assertEquals("이미 있는 R-18 프로젝트 2", RatingSettingsState.applyTitle(Rating.R18, 2))
    }
}
