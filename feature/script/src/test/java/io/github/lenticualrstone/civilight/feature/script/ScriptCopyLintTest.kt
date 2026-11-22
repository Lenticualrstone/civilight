package io.github.lenticualrstone.civilight.feature.script

import io.github.lenticualrstone.civilight.designsystem.text.CopyLint
import io.github.lenticualrstone.civilight.domain.script.ScriptCommands
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** DECISIONS §3: every UI string of the script screen passes the DS copy lint. */
class ScriptCopyLintTest {
    private fun lintAll(values: List<String>, button: Boolean = false): Map<String, Any> =
        values.filterNot { it.startsWith(TODO) }
            .associateWith { CopyLint.lint(it, isButton = button) }
            .filterValues { it.isNotEmpty() }

    @Test
    fun `script copy passes the lint`() {
        assertEquals(emptyMap<String, Any>(), lintAll(ScriptCopy.all))
        assertEquals(emptyMap<String, Any>(), lintAll(ScriptCopy.buttons, button = true))
    }

    @Test
    fun `domain strings the screen draws pass the lint`() {
        val drawn = ScriptCommands.ADD_ROWS.flatMap { listOf(it.name, it.description) } +
            listOf(ScriptCommands.SHEET_TITLE, ScriptCommands.READ_ONLY, ScriptCommands.HINT)
        assertEquals(emptyMap<String, Any>(), lintAll(drawn))
    }

    @Test
    fun `missing copy is a TODO_COPY constant, never invented prose`() {
        val todos = ScriptCopy.all.filter { it.startsWith(TODO) }
        assertTrue(todos.all { it.matches(Regex("TODO_COPY_SCRIPT_[A-Z_]+")) })
        assertTrue(ScriptCopy.all.none { it.endsWith("요") || it.endsWith("요.") })
    }

    private companion object {
        const val TODO = "TODO_COPY_"
    }
}
