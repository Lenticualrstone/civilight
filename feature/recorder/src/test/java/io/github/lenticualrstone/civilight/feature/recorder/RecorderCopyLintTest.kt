package io.github.lenticualrstone.civilight.feature.recorder

import io.github.lenticualrstone.civilight.designsystem.text.CopyLint
import io.github.lenticualrstone.civilight.designsystem.theme.MotionLint
import io.github.lenticualrstone.civilight.domain.recorder.RecorderCopy
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.lang.reflect.Modifier

/** DECISIONS §3: every value of this feature's copy objects passes the DS-62 / DS-63 copy lint. */
class RecorderCopyLintTest {
    private fun constants(type: Class<*>): Map<String, String> = type.declaredFields
        .filter { Modifier.isStatic(it.modifiers) && it.type == String::class.java }
        .associate { field ->
            field.isAccessible = true
            field.name to field.get(null) as String
        }

    private val buttons = setOf(
        "OPEN_SYSTEM_SETTINGS", "MIC_ALLOW", "STOP_AND_SAVE", "TO_TEXT", "CONTINUE_SPEAKING", "KEEP_IN_INBOX", "SAVE", "CLOSE", "CANCEL",
        "RECORD_BY_VOICE", "BY_TEXT_SHORT", "UNDO", "GO_SEE",
    )

    private fun lint(type: Class<*>): List<String> = constants(type).flatMap { (name, value) ->
        CopyLint.lint(value, isButton = name in buttons).map { "${type.simpleName}.$name = \"$value\": $it" }
    }

    @Test
    fun featureCopyPassesTheLint() {
        val values = constants(RecorderUiCopy::class.java)
        assertTrue(values.isNotEmpty())
        assertEquals(emptyList<String>(), lint(RecorderUiCopy::class.java))
    }

    @Test
    fun domainCopyShownByTheseScreensPassesTheLint() {
        assertEquals(emptyList<String>(), lint(RecorderCopy::class.java))
    }

    /** Motion audit §3.2: no literal durations, default crossfades, default `animateItem` or snap flings in this module. */
    @Test
    fun `motion lint`() {
        val issues = MotionLint.lintModule(File("."))
        assertEquals(MotionLint.report(issues), 0, issues.size)
    }

    @Test
    fun missingCopyStaysATodoId() {
        val todos = constants(RecorderCopy::class.java).filterKeys { it.startsWith("TODO_") }.values
        assertTrue(todos.all { it.matches(Regex("TODO_COPY_LRC_\\d{2}")) })
    }
}
