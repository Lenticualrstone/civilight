package io.github.lenticualrstone.civilight.feature.cast

import io.github.lenticualrstone.civilight.designsystem.text.CopyLint
import io.github.lenticualrstone.civilight.designsystem.theme.MotionLint
import io.github.lenticualrstone.civilight.model.cast.EntityKind
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.lang.reflect.Modifier

/** DECISIONS §3: every value of the cast copy object passes the design-system copy lint. */
class CastCopyLintTest {
    /** Constant and list values of [CastUiCopy] by field name. */
    private fun values(): Map<String, List<String>> = CastUiCopy::class.java.declaredFields
        .filter { Modifier.isStatic(it.modifiers) && it.name != "INSTANCE" }
        .associate { field ->
            field.isAccessible = true
            val value = field.get(null)
            field.name to when (value) {
                is String -> listOf(value)
                is List<*> -> value.filterIsInstance<String>()
                else -> emptyList()
            }
        }

    /** Formatted copy, with sample arguments from the example project. */
    private fun formatted(): List<String> = buildList {
        add(CastUiCopy.emptyRow(3))
        add(CastUiCopy.stepStatus("지금 시점보다 뒤"))
        EntityKind.entries.forEach {
            add(CastUiCopy.pickTitle(it))
            add(CastUiCopy.addFieldSub(it))
        }
        add(CastUiCopy.recordSub("리엔", 7))
        add(CastUiCopy.onlyFor("리엔"))
        add(CastUiCopy.fromSource("출생"))
        add(CastUiCopy.since("1-3"))
        add(CastUiCopy.monthNumber(3))
        add(CastUiCopy.menuLabel("리엔"))
        add(CastUiCopy.range("150", "170"))
        add(CastUiCopy.range("150", ""))
        add(CastUiCopy.chosenType("짧은 글"))
        add(CastUiCopy.change("짧은 글", "숫자"))
        add(CastUiCopy.usage(1))
        add(CastUiCopy.movedTitle(5))
        add(CastUiCopy.differingTitle(3))
        add(CastUiCopy.mergedColumn("시장 꼬마"))
        add(CastUiCopy.keptColumn("노아"))
        add(CastUiCopy.mergedFootnote("시장 꼬마", "노아"))
        add(CastUiCopy.linkChange(3, "노아", "오웬"))
        add(CastUiCopy.gallerySection(8))
        add(CastUiCopy.galleryChip(8))
    }

    @Test
    fun `every copy value passes the lint`() {
        val found = values()
        assertTrue("the copy object exposes its constants", found.size > 20)
        val failures = found.flatMap { (name, texts) ->
            texts.filterNot { it.startsWith("TODO_COPY_") }.mapNotNull { text ->
                val issues = CopyLint.lint(text, isButton = name.startsWith("BTN_"))
                if (issues.isEmpty()) null else "$name = $text: $issues"
            }
        } + formatted().mapNotNull { text -> CopyLint.lint(text).takeIf { it.isNotEmpty() }?.let { "$text: $it" } }
        assertEquals(emptyList<String>(), failures)
    }

    @Test
    fun `motion lint`() {
        val issues = MotionLint.lintModule(File("."))
        assertEquals(MotionLint.report(issues), 0, issues.size)
    }

    @Test
    fun `no TODO copy can reach the screen`() {
        assertTrue(values().keys.none { it.startsWith("TODO_COPY_") })
    }

    @Test
    fun `missing copy is marked as such`() {
        val todo = values().filterKeys { it.startsWith("TODO_COPY_") }
        todo.forEach { (name, texts) -> assertEquals(listOf(name), texts) }
    }

    @Test
    fun `formatted copy reads as the boards`() {
        assertEquals("빈 항목 3 · 보기", CastUiCopy.emptyRow(3))
        assertEquals("인물 · 모든 인물에", CastUiCopy.addFieldSub(EntityKind.CHARACTER))
        assertEquals("리엔 · 인물 기록 007", CastUiCopy.recordSub("리엔", 7))
        assertEquals("리엔에게만", CastUiCopy.onlyFor("리엔"))
        assertEquals("세력 고르기", CastUiCopy.pickTitle(EntityKind.FACTION))
        assertEquals("◆ 지금 시점보다 뒤", CastUiCopy.stepStatus("지금 시점보다 뒤"))
        assertEquals("1-3부터", CastUiCopy.since("1-3"))
        assertEquals("3월", CastUiCopy.monthNumber(3))
        assertEquals(listOf("목록", "관계도"), CastUiCopy.VIEW_MODES)
    }
}
