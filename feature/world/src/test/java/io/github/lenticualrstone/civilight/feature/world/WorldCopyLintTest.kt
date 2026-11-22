package io.github.lenticualrstone.civilight.feature.world

import io.github.lenticualrstone.civilight.designsystem.text.CopyLint
import io.github.lenticualrstone.civilight.domain.world.item.WorldKind
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.lang.reflect.Modifier

/** DECISIONS §3: every value of the world copy object passes the design-system copy lint. */
class WorldCopyLintTest {
    /** Constant values of [WorldCopy] by field name. */
    private fun values(): Map<String, List<String>> = WorldCopy::class.java.declaredFields
        .filter { Modifier.isStatic(it.modifiers) && it.name != "INSTANCE" }
        .associate { field ->
            field.isAccessible = true
            field.name to when (val value = field.get(null)) {
                is String -> listOf(value)
                is List<*> -> value.filterIsInstance<String>()
                else -> emptyList()
            }
        }

    /** Formatted copy, with arguments from the example project. */
    private fun formatted(): List<String> = buildList {
        WorldKind.entries.forEach {
            add(WorldCopy.createLabel(it))
            add(WorldCopy.smallLine(it))
            add(WorldCopy.hiddenRow(it, 2))
        }
        add(WorldCopy.menuLabel("오리지늄 등불"))
        add(WorldCopy.revealSet("1-2"))
        add(WorldCopy.emptyFields(3))
        add(WorldCopy.lensEditTitle("1-5"))
        add(WorldCopy.lensEditSub("1-5"))
    }

    @Test
    fun `every copy value passes the lint`() {
        val found = values()
        assertTrue("the copy object exposes its constants", found.size > 30)
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
    fun `formatted copy reads as the specs and boards`() {
        assertEquals("용어 만들기", WorldCopy.createLabel(WorldKind.TERM))
        assertEquals("세계 · 장소", WorldCopy.smallLine(WorldKind.PLACE))
        assertEquals("숨긴 세력 2", WorldCopy.hiddenRow(WorldKind.FACTION, 2))
        assertEquals("빈 항목 3 · 보기", WorldCopy.emptyFields(3))
        assertEquals("1-5 시점 값 고치는 중", WorldCopy.lensEditTitle("1-5"))
        assertEquals("고치면 1-5부터 적용", WorldCopy.lensEditSub("1-5"))
        assertEquals("◇ 첫 공개 1-2", WorldCopy.revealSet("1-2"))
    }
}
