package io.github.lenticualrstone.civilight.feature.canvas

import io.github.lenticualrstone.civilight.designsystem.text.CopyLint
import io.github.lenticualrstone.civilight.feature.canvas.board.CanvasTool
import io.github.lenticualrstone.civilight.model.canvas.CanvasNodeKind
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.lang.reflect.Modifier

/** DECISIONS §3: every value of the canvas copy object passes the design-system copy lint. */
class CanvasCopyLintTest {
    private fun values(): Map<String, List<String>> = CanvasCopy::class.java.declaredFields
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
        add(CanvasCopy.zoomPercent(0.82f))
        CanvasNodeKind.entries.forEach { add(CanvasCopy.kindWord(it)) }
        CanvasCopy.NODE_TOOL_KINDS.forEach { add(CanvasCopy.pickTitle(it)) }
        add(CanvasCopy.deleted(CanvasCopy.KIND_CHARACTER, "노아"))
        add(CanvasCopy.mindOf("리엔"))
        add(CanvasCopy.pair("리엔", "하르트"))
        CanvasTool.entries.forEach {
            add(it.label(cover = true))
            add(it.label(cover = false))
        }
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
    fun `missing copy is marked as such`() {
        val todo = values().filterKeys { it.startsWith("TODO_COPY_") }
        assertTrue(todo.isNotEmpty())
        todo.forEach { (name, texts) -> assertEquals(listOf(name), texts) }
    }

    @Test
    fun `formatted copy reads as the boards`() {
        assertEquals("82%", CanvasCopy.zoomPercent(0.82f))
        assertEquals("100%", CanvasCopy.zoomPercent(1f))
        assertEquals("리엔의 마음", CanvasCopy.mindOf("리엔"))
        assertEquals("리엔 · 하르트", CanvasCopy.pair("리엔", "하르트"))
        assertEquals("지운 인물 · 노아", CanvasCopy.deleted("인물", "노아"))
        assertEquals("세력 고르기", CanvasCopy.pickTitle(CanvasCopy.KIND_FACTION))
        assertEquals(listOf("노드", "인물", "메모", "선", "묶음"), CanvasTool.entries.map { it.label(cover = true) })
        assertEquals(listOf("노드", "인물 카드", "메모", "선", "묶음"), CanvasTool.entries.map { it.label(cover = false) })
        assertEquals("두 손가락 이동 · 확대 · 노드는 끌어서 이동", CanvasCopy.HINT)
    }
}
